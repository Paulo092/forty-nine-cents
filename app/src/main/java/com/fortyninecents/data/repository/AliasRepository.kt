package com.fortyninecents.data.repository

import com.fortyninecents.data.local.AliasDao
import com.fortyninecents.data.local.toDomain
import com.fortyninecents.data.local.toEntity
import com.fortyninecents.domain.Alias
import com.fortyninecents.domain.AliasResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AliasRepository(private val dao: AliasDao) {

    fun observeAll(): Flow<List<Alias>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Resolver sempre atualizado: qualquer mudança nos apelidos reflete nas telas na hora. */
    fun observeResolver(): Flow<AliasResolver> = observeAll().map { AliasResolver(it) }

    suspend fun resolver(): AliasResolver = AliasResolver(dao.getAll().map { it.toDomain() })

    /** Cria ou edita (quando [Alias.id] != 0). Ignora padrão/apelido em branco. */
    suspend fun save(alias: Alias) {
        if (alias.pattern.isBlank() || alias.nickname.isBlank()) return
        dao.replace(previousId = alias.id, entity = alias.toEntity())
    }

    suspend fun delete(alias: Alias) = dao.delete(alias.id)
}
