package com.fortyninecents.data.local

import androidx.room.TypeConverter
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionStatus
import com.fortyninecents.domain.TransactionType

/** Enums persistidos pelo nome (legível no banco e estável a reordenações). */
class Converters {
    @TypeConverter fun fromCategory(value: Category): String = value.name
    @TypeConverter fun toCategory(value: String): Category = Category.fromName(value)

    @TypeConverter fun fromType(value: TransactionType): String = value.name
    @TypeConverter fun toType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter fun fromSource(value: TransactionSource): String = value.name
    @TypeConverter fun toSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter fun fromStatus(value: TransactionStatus): String = value.name
    @TypeConverter fun toStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)
}
