package com.fortyninecents.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AliasDao {

    @Query("SELECT * FROM aliases ORDER BY nickname COLLATE NOCASE, pattern COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<AliasEntity>>

    @Query("SELECT * FROM aliases")
    abstract suspend fun getAll(): List<AliasEntity>

    /** REPLACE: se já existir apelido para o mesmo identificador, o novo substitui. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: AliasEntity): Long

    @Query("DELETE FROM aliases WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Edição: apaga a versão anterior (o identificador pode ter mudado) e grava a nova. */
    @Transaction
    open suspend fun replace(previousId: Long, entity: AliasEntity) {
        if (previousId != 0L) delete(previousId)
        insert(entity.copy(id = 0))
    }
}
