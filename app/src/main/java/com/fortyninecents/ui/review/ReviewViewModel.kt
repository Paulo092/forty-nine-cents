package com.fortyninecents.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortyninecents.data.repository.TransactionRepository
import com.fortyninecents.data.settings.SettingsRepository
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import com.fortyninecents.notification.NotificationProcessor
import com.fortyninecents.util.Money
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val pending: List<Transaction> = emptyList(),
    val smsCaptureEnabled: Boolean = false,
    val isLoading: Boolean = true,
)

/**
 * Tela de revisão: lançamentos capturados das notificações ficam PENDENTES até o
 * usuário confirmar (evita que uma leitura errada polua o histórico).
 */
class ReviewViewModel(
    private val repository: TransactionRepository,
    private val processor: NotificationProcessor,
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<ReviewUiState> = combine(
        repository.observePending(),
        settings.smsCaptureEnabled,
    ) { pending, smsEnabled ->
        ReviewUiState(pending = pending, smsCaptureEnabled = smsEnabled, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun setSmsCaptureEnabled(enabled: Boolean) = settings.setSmsCaptureEnabled(enabled)

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    fun confirm(transaction: Transaction) {
        viewModelScope.launch { repository.confirm(transaction.id) }
    }

    fun discard(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction.id) }
    }

    fun confirmAll() {
        viewModelScope.launch {
            val count = uiState.value.pending.size
            repository.confirmAllPending()
            messages.send(if (count == 1) "1 lançamento confirmado" else "$count lançamentos confirmados")
        }
    }

    fun changeCategory(transaction: Transaction, category: Category) {
        viewModelScope.launch { repository.updateCategory(transaction.id, category) }
    }

    /** Roda o mesmo fluxo do serviço com um texto digitado — útil para testar sem esperar o banco. */
    fun simulate(text: String) {
        viewModelScope.launch {
            val result = processor.process(
                packageName = SIMULATION_PACKAGE,
                sourceName = "Simulação",
                title = null,
                text = text,
                postedAt = System.currentTimeMillis(),
            )
            messages.send(
                when (result) {
                    is NotificationProcessor.Result.Captured ->
                        "Reconhecido: ${result.transaction.title} – ${Money.format(result.transaction.amountCents)}"
                    NotificationProcessor.Result.Duplicate -> "Notificação repetida, ignorada"
                    NotificationProcessor.Result.NotATransaction -> "Texto não reconhecido como transação"
                },
            )
        }
    }

    private companion object {
        const val SIMULATION_PACKAGE = "simulacao"
    }
}
