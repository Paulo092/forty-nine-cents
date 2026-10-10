package com.fortyninecents.util

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
private val WHITESPACE = Regex("\\s+")

/** Minúsculas, sem acentos e com espaços normalizados — para comparar palavras-chave. */
fun String.normalizeForMatching(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .replace(WHITESPACE, " ")
        .trim()

/** Troca espaços "especiais" (NBSP etc.) por espaço comum e colapsa repetições. */
fun String.collapseWhitespace(): String =
    replace('\u00A0', ' ').replace('\u202F', ' ').replace(WHITESPACE, " ").trim()
