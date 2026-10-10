package com.fortyninecents.di

import android.content.Context
import com.fortyninecents.data.local.AppDatabase
import com.fortyninecents.data.repository.TransactionRepository
import com.fortyninecents.data.settings.SettingsRepository
import com.fortyninecents.domain.CategoryClassifier
import com.fortyninecents.export.CsvExporter
import com.fortyninecents.notification.NotificationParser
import com.fortyninecents.notification.NotificationProcessor

/**
 * Injeção de dependências manual: suficiente para o tamanho atual do app e sem
 * custo de build. Se o grafo crescer, migrar para Hilt é direto (mesmas dependências).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val repository: TransactionRepository by lazy { TransactionRepository(database.transactionDao()) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val classifier: CategoryClassifier by lazy { CategoryClassifier() }

    val csvExporter: CsvExporter by lazy { CsvExporter(appContext) }

    val notificationProcessor: NotificationProcessor by lazy {
        NotificationProcessor(NotificationParser(), classifier, repository)
    }
}
