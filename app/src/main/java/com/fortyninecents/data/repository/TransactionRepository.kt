package com.fortyninecents.data.repository

import com.fortyninecents.data.local.TransactionDao
import com.fortyninecents.data.local.toDomain
import com.fortyninecents.data.local.toEntity
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId

/** Fonte única de verdade para os lançamentos. A UI só conhece o modelo de domínio. */
class TransactionRepository(
    private val dao: TransactionDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    fun observeRecent(limit: Int = 5): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
        val (start, end) = month.bounds()
        return dao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }
    }

    fun observePending(): Flow<List<Transaction>> =
        dao.observePending().map { list -> list.map { it.toDomain() } }

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    suspend fun getMonth(month: YearMonth): List<Transaction> {
        val (start, end) = month.bounds()
        return dao.getBetween(start, end).map { it.toDomain() }
    }

    suspend fun getAll(): List<Transaction> = dao.getAllConfirmed().map { it.toDomain() }

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

    private fun YearMonth.bounds(): Pair<Long, Long> {
        val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
