package com.fortyninecents.util

import kotlin.math.absoluteValue

/**
 * Formatação de valores em Real feita à mão (em vez de NumberFormat) para ser
 * determinística entre versões de Android/JDK, que variam o tipo de espaço usado.
 */
object Money {

    /** Máximo de dígitos aceitos no campo de valor (R$ 999.999.999,99). */
    const val MAX_INPUT_DIGITS = 11

    /** 4000 -> "R$ 40,00"; 123456 -> "R$ 1.234,56". O sinal é ignorado. */
    fun format(cents: Long): String = "R$ " + formatNumber(cents.absoluteValue)

    /** -4000 -> "-R$ 40,00"; 1000 -> "+R$ 10,00"; 0 -> "R$ 0,00". */
    fun formatSigned(cents: Long): String = when {
        cents < 0 -> "-" + format(cents)
        cents > 0 -> "+" + format(cents)
        else -> format(0)
    }

    /** 4000 -> "40,00" (sem símbolo, com separador de milhar). O sinal é ignorado. */
    fun formatNumber(cents: Long): String {
        val abs = cents.absoluteValue
        val reais = (abs / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        val centavos = (abs % 100).toString().padStart(2, '0')
        return "$reais,$centavos"
    }

    /** Valor decimal simples para CSV: -4000 -> "-40,00". */
    fun formatPlain(cents: Long): String {
        val abs = cents.absoluteValue
        val sign = if (cents < 0) "-" else ""
        return "$sign${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
    }

    /** Converte os dígitos digitados ("4000") em centavos (4000). Vazio -> 0. */
    fun digitsToCents(digits: String): Long =
        digits.filter(Char::isDigit).take(MAX_INPUT_DIGITS).toLongOrNull() ?: 0L

    /**
     * Converte texto no formato brasileiro em centavos.
     * "40,00" -> 4000, "1.234,56" -> 123456, "10" -> 1000, "5,5" -> 550. Retorna null se inválido.
     */
    fun parseBrl(text: String): Long? {
        val clean = text.trim().removePrefix("R$").trim()
        if (clean.isEmpty() || !clean.matches(Regex("""\d{1,3}(\.\d{3})*(,\d{1,2})?|\d+(,\d{1,2})?"""))) return null
        val parts = clean.replace(".", "").split(",")
        val reais = parts[0].toLongOrNull() ?: return null
        val centavos = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
        return reais * 100 + centavos
    }
}
