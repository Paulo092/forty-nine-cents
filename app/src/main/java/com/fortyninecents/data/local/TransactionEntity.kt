package com.fortyninecents.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionStatus
import com.fortyninecents.domain.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurred_at"]),
        Index(value = ["status"]),
        // evita duplicar a mesma notificação quando o banco a "re-posta"
        Index(value = ["dedup_key"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val type: TransactionType,
    val category: Category,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val source: TransactionSource,
    val status: TransactionStatus,
    @ColumnInfo(name = "source_app") val sourceApp: String? = null,
    @ColumnInfo(name = "dedup_key") val dedupKey: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    title = title,
    amountCents = amountCents,
    type = type,
    category = category,
    occurredAt = occurredAt,
    source = source,
    status = status,
    sourceApp = sourceApp,
)

fun Transaction.toEntity(dedupKey: String? = null) = TransactionEntity(
    id = id,
    title = title,
    amountCents = amountCents,
    type = type,
    category = category,
    occurredAt = occurredAt,
    source = source,
    status = status,
    sourceApp = sourceApp,
    dedupKey = dedupKey,
)
