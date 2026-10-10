package com.fortyninecents.notification

import com.fortyninecents.data.repository.AliasRepository
import com.fortyninecents.data.repository.TransactionRepository
import com.fortyninecents.domain.CategoryClassifier
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionStatus
import java.security.MessageDigest

/**
 * Orquestra: texto da notificação -> parser -> categoria -> banco (como PENDENTE para revisão).
 * Usado tanto pelo NotificationListenerService quanto pela simulação na tela de revisão.
 */
class NotificationProcessor(
    private val parser: NotificationParser,
    private val classifier: CategoryClassifier,
    private val repository: TransactionRepository,
    private val aliases: AliasRepository,
) {

    sealed interface Result {
        data class Captured(val transaction: Transaction) : Result
        data object Duplicate : Result
        data object NotATransaction : Result
    }

    suspend fun process(
        packageName: String,
        sourceName: String,
        title: String?,
        text: String?,
        postedAt: Long,
    ): Result {
        val parsed = parser.parse(title, text, postedAt) ?: return Result.NotATransaction

        // o apelido costuma ser mais descritivo ("Feira do Zé" x "PAG*JOSEDASILVA"): ajuda a categorizar
        val nickname = aliases.resolver().nicknameFor(parsed.description)
        val categoryHint = listOfNotNull(nickname, parsed.description).joinToString(" ")

        val transaction = Transaction(
            title = parsed.description,
            nickname = nickname,
            amountCents = parsed.amountCents,
            type = parsed.type,
            category = classifier.classify(categoryHint, parsed.type),
            occurredAt = parsed.occurredAt,
            source = TransactionSource.NOTIFICATION,
            status = TransactionStatus.PENDING,
            sourceApp = sourceName,
        )
        // Mesmo app + mesmo texto no mesmo minuto = mesma notificação re-postada
        val key = sha256("$packageName|$title|$text|${postedAt / 60_000}")

        return if (repository.addCaptured(transaction, key)) Result.Captured(transaction) else Result.Duplicate
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
