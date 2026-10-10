package com.fortyninecents.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Versões:
 * 1 – lançamentos
 * 2 – apelidos de identificadores (tabela nova, migração automática a partir de app/schemas/…/1.json)
 */
@Database(
    entities = [TransactionEntity::class, AliasEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun aliasDao(): AliasDao

    companion object {
        // se mudar o nome, atualize res/xml/data_extraction_rules.xml e backup_rules.xml
        private const val NAME = "forty_nine_cents.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
