package com.fortyninecents.domain

import com.fortyninecents.util.normalizeForMatching

/**
 * Apelido para um identificador de gasto, ex.: "PAG*JOSEDASILVA 0042" -> "Feira do Zé".
 *
 * O lançamento continua guardando o identificador original; o apelido é aplicado na hora
 * de exibir/exportar. Assim, editar ou remover um apelido vale para o passado e o futuro.
 */
data class Alias(
    val id: Long = 0,
    /** Texto do identificador original (ou parte dele, quando [matchType] = CONTAINS). */
    val pattern: String,
    val nickname: String,
    val matchType: AliasMatch = AliasMatch.EXACT,
)

enum class AliasMatch {
    /** Identificador igual ao padrão (ignorando maiúsculas, acentos e espaços extras). */
    EXACT,

    /** Identificador que contém o padrão — útil para "UBER *TRIP 1234", "UBER *TRIP 5678"... */
    CONTAINS,
}

/**
 * Encontra o apelido de um identificador. Igualdade exata tem prioridade; entre os
 * "contém", vence o padrão mais longo (mais específico).
 */
class AliasResolver(aliases: List<Alias>) {

    private val exact: Map<String, Alias> = aliases
        .filter { it.matchType == AliasMatch.EXACT }
        .associateBy { key(it.pattern) }
        .filterKeys { it.isNotEmpty() }

    private val contains: List<Pair<String, Alias>> = aliases
        .filter { it.matchType == AliasMatch.CONTAINS }
        .map { key(it.pattern) to it }
        .filter { (k, _) -> k.isNotEmpty() }
        .sortedByDescending { (k, _) -> k.length }

    fun match(identifier: String): Alias? {
        val k = key(identifier)
        if (k.isEmpty()) return null
        return exact[k] ?: contains.firstOrNull { (pattern, _) -> k.contains(pattern) }?.second
    }

    fun nicknameFor(identifier: String): String? = match(identifier)?.nickname

    fun apply(transaction: Transaction): Transaction =
        nicknameFor(transaction.title)?.let { transaction.copy(nickname = it) } ?: transaction

    companion object {
        val EMPTY = AliasResolver(emptyList())

        /** Chave usada para comparar identificadores. */
        fun key(text: String): String = text.normalizeForMatching()
    }
}

/**
 * Monta o apelido a salvar quando o usuário edita, a partir de um lançamento, o apelido que já se aplica a ele.
 *
 * Se o atual é uma regra "contém" (ex.: "UBER *TRIP" -> "Uber") e o usuário trocou para identificador
 * exato, cria um apelido novo e específico em vez de sobrescrever a regra geral — que continua valendo
 * para os outros lançamentos (o exato tem prioridade no [AliasResolver]).
 */
fun aliasFromEdit(existing: Alias?, pattern: String, nickname: String, match: AliasMatch): Alias {
    val keepsGeneralRule = existing?.matchType == AliasMatch.CONTAINS && match == AliasMatch.EXACT
    val id = if (existing == null || keepsGeneralRule) 0 else existing.id
    return Alias(id = id, pattern = pattern, nickname = nickname, matchType = match)
}
