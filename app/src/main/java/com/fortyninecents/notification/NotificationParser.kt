package com.fortyninecents.notification

import com.fortyninecents.domain.TransactionType
import com.fortyninecents.util.Money
import com.fortyninecents.util.collapseWhitespace
import com.fortyninecents.util.normalizeForMatching
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class ParsedNotification(
    val amountCents: Long,
    val description: String,
    val type: TransactionType,
    val occurredAt: Long,
)

/**
 * Extrai valor, estabelecimento, tipo e data/hora do texto de notificações de bancos brasileiros.
 *
 * Estratégia (sem depender do layout de um banco específico):
 * 1. descarta notificações que não são movimentações (promoções, compras recusadas, fatura...);
 * 2. decide se é despesa ou receita por palavras-chave;
 * 3. pega o primeiro valor "R$ x,xx";
 * 4. tenta vários padrões de "onde/quem" (em X, às 13:00, X, para X, de X...) em ordem de confiança;
 * 5. usa data/hora do texto se houver, senão o horário em que a notificação chegou.
 *
 * Classe pura (sem Android) para ser testada na JVM.
 */
class NotificationParser(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun parse(title: String?, text: String?, postedAt: Long): ParsedNotification? {
        val cleanTitle = title.orEmpty().collapseWhitespace()
        val cleanText = text.orEmpty().collapseWhitespace()
        val full = listOf(cleanTitle, cleanText).filter { it.isNotEmpty() }.joinToString(". ")
        if (full.isEmpty()) return null

        // espaço no fim permite palavras-chave como "pago " casarem no final do texto
        val normalized = full.normalizeForMatching() + " "
        if (IGNORE_KEYWORDS.any { normalized.contains(it) }) return null

        val type = detectType(normalized) ?: return null
        val amountMatch = AMOUNT.find(full) ?: return null
        val amount = Money.parseBrl(amountMatch.groupValues[1]) ?: return null
        if (amount <= 0) return null

        val description = extractCounterpart(full, type) ?: fallbackDescription(cleanTitle, type)
        val occurredAt = extractDateTime(full, postedAt)

        return ParsedNotification(amount, description, type, occurredAt)
    }

    private fun detectType(normalized: String): TransactionType? = when {
        INCOME_KEYWORDS.any { normalized.contains(it) } -> TransactionType.INCOME
        EXPENSE_KEYWORDS.any { normalized.contains(it) } -> TransactionType.EXPENSE
        else -> null
    }

    private fun extractCounterpart(full: String, type: TransactionType): String? {
        val patterns = if (type == TransactionType.EXPENSE) EXPENSE_PATTERNS else INCOME_PATTERNS
        for (pattern in patterns) {
            for (match in pattern.findAll(full)) {
                val candidate = cleanCounterpart(match.groupValues[1])
                if (candidate != null) return candidate
            }
        }
        return null
    }

    private fun cleanCounterpart(raw: String): String? {
        val value = raw.collapseWhitespace().trim(' ', '.', '-', '*', ':').take(MAX_DESCRIPTION)
        if (value.length < 2 || value.none(Char::isLetter)) return null
        val normalized = value.normalizeForMatching()
        if (REJECTED_COUNTERPARTS.any { normalized == it || normalized.startsWith("$it ") }) return null
        return value
    }

    private fun fallbackDescription(title: String, type: TransactionType): String = when {
        title.isNotBlank() && !title.contains("R$") && title.length <= MAX_DESCRIPTION -> title
        type == TransactionType.EXPENSE -> "Despesa via notificação"
        else -> "Receita via notificação"
    }

    private fun extractDateTime(full: String, postedAt: Long): Long {
        val posted = Instant.ofEpochMilli(postedAt).atZone(zone).toLocalDateTime()
        val date = DATE.find(full)?.let { parseDate(it, posted.toLocalDate()) }
        val time = TIME.find(full)?.let { parseTime(it) }
        if (date == null && time == null) return postedAt

        var result = LocalDateTime.of(date ?: posted.toLocalDate(), time ?: posted.toLocalTime())
        // só a hora no texto e ela é "do futuro"? então foi ontem (ex.: 23:58 notificado 00:01)
        if (date == null && result.isAfter(posted.plusMinutes(5))) result = result.minusDays(1)

        val millis = result.atZone(zone).toInstant().toEpochMilli()
        // proteção contra datas absurdas (ex.: "vencimento 10/12" passou pelo filtro)
        val distance = Duration.ofMillis(kotlin.math.abs(millis - postedAt))
        return if (distance > Duration.ofDays(MAX_DAYS_FROM_POST)) postedAt else millis
    }

    private fun parseDate(match: MatchResult, postedDate: LocalDate): LocalDate? = try {
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val yearText = match.groupValues[3]
        when {
            yearText.isEmpty() -> {
                val candidate = LocalDate.of(postedDate.year, month, day)
                if (candidate.isAfter(postedDate.plusDays(1))) candidate.minusYears(1) else candidate
            }
            yearText.length == 2 -> LocalDate.of(2000 + yearText.toInt(), month, day)
            else -> LocalDate.of(yearText.toInt(), month, day)
        }
    } catch (_: DateTimeException) {
        null
    }

    private fun parseTime(match: MatchResult): LocalTime? = try {
        LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    } catch (_: DateTimeException) {
        null
    }

    private companion object {
        const val MAX_DESCRIPTION = 50
        const val MAX_DAYS_FROM_POST = 45L

        /** R$ 1.234,56 | R$ 40,00 | R$40 | R$ 1.000 */
        val AMOUNT = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+,\d{1,2}|\d+,\d{1,2}|\d{1,3}(?:\.\d{3})+|\d+)""")
        val DATE = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{4}|\d{2}))?\b""")
        val TIME = Regex("""\b([01]?\d|2[0-3])(?::|h)([0-5]\d)\b""")

        /** Onde o nome do estabelecimento/pessoa termina. */
        const val END =
            """(?=(?i:\s+(?:para\s+(?:o|seu)\s+cart|no\s+(?:seu\s+)?cart|com\s+(?:o\s+|seu\s+)?cart|cart[aã]o\s+final|""" +
                """no\s+valor|no\s+dia|dia\s+\d|em\s+\d|[aà]s\s+\d|foi\s|via\s|pelo\s|pela\s|realizad|aprovad))""" +
                """|\s*[,;:!?(]|\.(?:\s|$)|\s+-\s|$)"""
        const val BODY = """([^\n,;:!?(]+?)"""

        val EXPENSE_PATTERNS = listOf(
            Regex("""(?i)\b(?:no\s+estabelecimento|na\s+loja|no\s+com[eé]rcio)\s+$BODY$END"""),
            Regex("""(?i)\bem\s+(?!\d)$BODY$END"""),
            Regex("""(?i)(?<!\p{L})[aà]s\s+\d{1,2}[:h]\d{2}h?\s*,\s*(?:em\s+)?$BODY$END"""),
            Regex("""(?i)\bpara\s+(?!(?:o|seu|sua|a)\s|voc[eê])$BODY$END"""),
            // Ex.: "VALOR DE R$ 40,00 SUPERMERCADO JULLIUS." (nome em maiúsculas logo após o valor)
            Regex("""R\$\s*[\d.,]+\s+(?:EM\s+|NO\s+|NA\s+)?([A-Z][A-Z0-9 *&'./-]+?)$END"""),
        )

        val INCOME_PATTERNS = listOf(
            Regex("""(?i)\bde\s+(?!R\$|\d|sua\s|seu\s)$BODY$END"""),
            Regex("""(?i)\bremetente:?\s+$BODY$END"""),
        )

        val INCOME_KEYWORDS = listOf(
            "voce recebeu", "recebeu um pix", "recebeu uma transferencia", "pix recebido",
            "transferencia recebida", "deposito", "credito em conta", "creditado", "estorno",
            "estornad", "reembolso", "rendimento", "recebido", "recebida",
        )

        val EXPENSE_KEYWORDS = listOf(
            "compra", "voce pagou", "pagamento", "pagou", "pago ", "debito", "debitad", "pix enviado",
            "transferencia enviada", "voce enviou", "enviou", "transferiu", "saque", "assinatura",
        )

        /** Notificações que citam valores mas não são movimentações confirmadas. */
        val IGNORE_KEYWORDS = listOf(
            "recusad", "negad", "nao autorizad", "nao aprovad", "bloquead", "cancelad", "agendad",
            "vence ", "vencimento", "fatura fechou", "fatura fechada", "fatura atual", "pagamento da fatura",
            "pagamento de fatura", "recebemos seu pagamento", "ganhe", "oferta", "promocao", "cupom",
            "codigo de verificacao", "codigo de seguranca", "emprestimo pre-aprovado", "limite aumentado",
        )

        /** Capturas que são parte da frase, não o estabelecimento. */
        val REJECTED_COUNTERPARTS = listOf(
            "sua conta", "seu cartao", "sua", "seu", "conta", "cartao", "aprovada", "aprovado",
            "realizada", "realizado", "efetuada", "efetuado", "confirmada", "confirmado",
            "credito", "debito", "um pix", "uma transferencia", "voce",
        )
    }
}
