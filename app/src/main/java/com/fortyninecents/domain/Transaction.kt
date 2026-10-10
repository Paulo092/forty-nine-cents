package com.fortyninecents.domain

/**
 * Movimentação financeira do usuário.
 *
 * @property amountCents valor absoluto em centavos (sempre positivo); o sinal vem de [type].
 * @property occurredAt  instante da movimentação em epoch millis.
 */
data class Transaction(
    val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val type: TransactionType,
    val category: Category,
    val occurredAt: Long,
    val source: TransactionSource = TransactionSource.MANUAL,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val sourceApp: String? = null,
) {
    /** Valor com sinal: negativo para despesas, positivo para receitas. */
    val signedCents: Long get() = if (type == TransactionType.EXPENSE) -amountCents else amountCents
}

enum class TransactionType { EXPENSE, INCOME }

/** De onde veio o lançamento. */
enum class TransactionSource { MANUAL, NOTIFICATION }

/** Lançamentos capturados de notificações começam como PENDING até o usuário revisar. */
enum class TransactionStatus { PENDING, CONFIRMED }
