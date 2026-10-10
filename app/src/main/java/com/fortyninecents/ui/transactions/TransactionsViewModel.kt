package com.fortyninecents.ui.transactions

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortyninecents.data.repository.TransactionRepository
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionType
import com.fortyninecents.export.CsvExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DayGroup(val date: LocalDate, val items: List<Transaction>) {
    val totalCents: Long get() = items.sumOf { it.signedCents }
}

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val groups: List<DayGroup> = emptyList(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val isExporting: Boolean = false,
    val isLoading: Boolean = true,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
    val canGoForward: Boolean get() = month < YearMonth.now()
    val isEmpty: Boolean get() = !isLoading && groups.isEmpty()
}

sealed interface TransactionsEvent {
    data class Share(val intent: Intent) : TransactionsEvent
    data class Deleted(val transaction: Transaction) : TransactionsEvent
    data class Message(val text: String) : TransactionsEvent
}

enum class ExportScope { MONTH, ALL }

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val exporter: CsvExporter,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val exporting = MutableStateFlow(false)

    private val events = Channel<TransactionsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        month,
        month.flatMapLatest { repository.observeMonth(it) },
        exporting,
    ) { month, items, exporting ->
        TransactionsUiState(
            month = month,
            groups = items
                .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }
                .map { (date, list) -> DayGroup(date, list) }
                .sortedByDescending { it.date },
            incomeCents = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
            expenseCents = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
            isExporting = exporting,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction.id)
            events.send(TransactionsEvent.Deleted(transaction))
        }
    }

    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.restore(transaction) }
    }

    fun export(scope: ExportScope) {
        if (exporting.value) return
        viewModelScope.launch {
            exporting.value = true
            try {
                val (items, label) = when (scope) {
                    ExportScope.MONTH -> repository.getMonth(month.value) to month.value.toString()
                    ExportScope.ALL -> repository.getAll() to "completo"
                }
                if (items.isEmpty()) {
                    events.send(TransactionsEvent.Message("Nada para exportar nesse período"))
                } else {
                    val uri = exporter.export(items, label)
                    events.send(TransactionsEvent.Share(exporter.shareIntent(uri)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                events.send(TransactionsEvent.Message("Não foi possível gerar o CSV: ${e.message}"))
            } finally {
                exporting.value = false
            }
        }
    }
}
