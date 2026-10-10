package com.fortyninecents.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortyninecents.data.repository.TransactionRepository
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.CategoryClassifier
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionType
import com.fortyninecents.util.Money
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** Campos do formulário de novo lançamento. */
data class EntryForm(
    val amountDigits: String = "",
    val title: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.OUTROS,
    /** true quando o usuário escolheu a categoria manualmente (paramos de sugerir). */
    val categoryLocked: Boolean = false,
    val useToday: Boolean = true,
    val date: LocalDate = LocalDate.now(),
) {
    val amountCents: Long get() = Money.digitsToCents(amountDigits)
    val canSave: Boolean get() = amountCents > 0 && title.isNotBlank()
}

data class HomeUiState(
    val form: EntryForm = EntryForm(),
    val recent: List<Transaction> = emptyList(),
    val pendingCount: Int = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
) {
    val monthBalance: Long get() = monthIncome - monthExpense
}

sealed interface HomeEvent {
    data class Saved(val transaction: Transaction) : HomeEvent
}

class HomeViewModel(
    private val repository: TransactionRepository,
    private val classifier: CategoryClassifier,
) : ViewModel() {

    private val form = MutableStateFlow(EntryForm())

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        form,
        repository.observeRecent(limit = 5),
        repository.observePendingCount(),
        repository.observeMonth(YearMonth.now()),
    ) { form, recent, pending, month ->
        HomeUiState(
            form = form,
            recent = recent,
            pendingCount = pending,
            monthIncome = month.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
            monthExpense = month.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onAmountChange(raw: String) {
        val digits = raw.filter(Char::isDigit).trimStart('0').take(Money.MAX_INPUT_DIGITS)
        form.update { it.copy(amountDigits = digits) }
    }

    fun onTitleChange(title: String) {
        form.update { current ->
            val suggested = if (current.categoryLocked) current.category else classifier.classify(title, current.type)
            current.copy(title = title, category = suggested)
        }
    }

    fun onTypeChange(type: TransactionType) {
        form.update { current ->
            val category = when {
                current.categoryLocked && current.category in Category.forType(type) -> current.category
                else -> classifier.classify(current.title, type)
            }
            current.copy(type = type, category = category, categoryLocked = false)
        }
    }

    fun onCategoryChange(category: Category) {
        form.update { it.copy(category = category, categoryLocked = true) }
    }

    fun onUseTodayChange(useToday: Boolean) {
        form.update { it.copy(useToday = useToday, date = if (useToday) LocalDate.now() else it.date) }
    }

    fun onDateChange(date: LocalDate) {
        form.update { it.copy(date = date, useToday = date == LocalDate.now()) }
    }

    fun save() {
        val current = form.value
        if (!current.canSave) return

        val date = if (current.useToday) LocalDate.now() else current.date
        val transaction = Transaction(
            title = current.title.trim(),
            amountCents = current.amountCents,
            type = current.type,
            category = current.category,
            occurredAt = date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        viewModelScope.launch {
            val id = repository.add(transaction)
            // mantém tipo e data escolhidos: facilita lançar vários gastos do mesmo dia em sequência
            form.update {
                EntryForm(
                    type = it.type,
                    category = classifier.classify("", it.type),
                    useToday = it.useToday,
                    date = it.date,
                )
            }
            events.send(HomeEvent.Saved(transaction.copy(id = id)))
        }
    }

    fun undo(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction.id) }
    }
}
