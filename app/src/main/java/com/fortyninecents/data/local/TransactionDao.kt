package com.fortyninecents.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY occurred_at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE status = 'CONFIRMED' AND occurred_at >= :start AND occurred_at < :end
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE status = 'CONFIRMED' AND occurred_at >= :start AND occurred_at < :end
        ORDER BY occurred_at ASC, id ASC
        """,
    )
    suspend fun getBetween(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY occurred_at ASC, id ASC")
    suspend fun getAllConfirmed(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY occurred_at DESC, id DESC")
    fun observePending(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    /** Retorna -1 quando a linha foi ignorada (ex.: dedup_key repetida). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("UPDATE transactions SET status = 'CONFIRMED' WHERE id = :id")
    suspend fun confirm(id: Long)

    @Query("UPDATE transactions SET status = 'CONFIRMED' WHERE status = 'PENDING'")
    suspend fun confirmAllPending()

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
