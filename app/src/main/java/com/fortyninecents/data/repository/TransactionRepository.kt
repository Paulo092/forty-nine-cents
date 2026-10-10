package com.fortyninecents.data.repository

import com.fortyninecents.data.local.TransactionDao
import com.fortyninecents.data.local.TransactionEntity
import com.fortyninecents.data.local.toDomain
import com.fortyninecents.data.local.toEntity
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import java.time.ZoneId

/**
 * Fonte única de verdade para os lançamentos. A UI só conhece o modelo de domínio,
 * já com os apelidos aplicados ([Transaction.nickname]).
 */
class TransactionRepository(
    private val dao: TransactionDao,
    private val aliases: AliasRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    fun observeRecent(limit: Int = 5): Flow<List<Transaction>> = dao.observeRecent(limit).withAliases()

    fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
        val (start, end) = month.bounds()
        return dao.observeBetween(start, end).withAliases()
    }

    fun observePending(): Flow<List<Transaction>> = dao.observePending().withAliases()

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    suspend fun getMonth(month: YearMonth): List<Transaction> {
        val (start, end) = month.bounds()
        return dao.getBetween(start, end).withAliases()
    }

    suspend fun getAll(): List<Transaction> = dao.getAllConfirmed().withAliases()

    suspend fun add(transaction: Transaction): Long = dao.insert(transaction.toEntity())

    /** Insere um lançamento capturado; retorna false se for duplicado. */
    suspend fun addCaptured(transaction: Transaction, dedupKey: String): Boolean =
        dao.insert(transaction.toEntity(dedupKey)) != -1L

    /** Reinsere um lançamento excluído (ação "desfazer"), mantendo o mesmo id. */
    suspend fun restore(transaction: Transaction) {
        dao.insert(transaction.toEntity())
    }

    suspend fun confirm(id: Long) = dao.confirm(id)

    suspend fun confirmAllPending() = dao.confirmAllPending()

    suspend fun updateCategory(id: Long, category: Category) = dao.updateCategory(id, category.name)

    suspend fun delete(id: Long) = dao.delete(id)

    private fun Flow<List<TransactionEntity>>.withAliases(): Flow<List<Transaction>> =
        combine(aliases.observeResolver()) { list, resolver -> list.map { resolver.apply(it.toDomain()) } }

    private suspend fun List<TransactionEntity>.withAliases(): List<Transaction> {
        val resolver = aliases.resolver()
        return map { resolver.apply(it.toDomain()) }
    }

    private fun YearMonth.bounds(): Pair<Long, Long> {
        val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
