package com.fortyninecents.notification

import com.fortyninecents.util.normalizeForMatching

/**
 * Decide se um SMS parece vir de um banco, olhando o remetente (título da notificação
 * do app de mensagens). Bloqueia SMS de números comuns e de contatos pessoais, que é
 * onde estariam tanto as mensagens falsas ("Você recebeu um Pix de R$ 5.000...") quanto
 * as conversas privadas do usuário.
 *
 * Aceita:
 * - códigos curtos (3 a 6 dígitos), usados pelos bancos para SMS transacional, ex.: "27888";
 * - remetentes alfanuméricos com nome de banco, ex.: "NUBANK", "Itau", "BB".
 *
 * Não é infalível (um contato salvo como "Nubank" passaria), por isso a captura de SMS
 * é opcional e tudo continua indo para a revisão antes de entrar no extrato.
 */
object SmsSenderFilter {

    private val SHORT_CODE = Regex("""^\d{3,6}$""")

    private val BANK_SENDERS = listOf(
        "nubank", "itau", "bradesco", "santander", "caixa", "banco do brasil", "bancodobrasil",
        "inter", "c6", "picpay", "mercado pago", "mercadopago", "pagbank", "pagseguro", "neon",
        "next", "original", "btg", "sicoob", "sicredi", "banrisul", "banco pan", "will bank",
    )

    private val BANK_PATTERNS = BANK_SENDERS.map { bank ->
        // nome de banco só vale como palavra inteira: "inter" não pode casar com "Interfone"
        Regex("""(^|[^a-z0-9])${Regex.escape(bank)}($|[^a-z0-9])""")
    }

    /** Siglas curtas exigem igualdade exata para não casar com nomes de pessoas. */
    private val EXACT_SENDERS = setOf("bb", "cef")

    fun isTrusted(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val compact = sender.filterNot { it.isWhitespace() || it == '-' }
        if (SHORT_CODE.matches(compact)) return true

        val normalized = sender.normalizeForMatching()
        if (normalized in EXACT_SENDERS) return true
        return BANK_PATTERNS.any { it.containsMatchIn(normalized) }
    }
}
