package com.fortyninecents.ui.aliases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortyninecents.data.repository.AliasRepository
import com.fortyninecents.domain.Alias
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Usado pela tela de apelidos e também pelo Extrato/Revisão (diálogo de apelido ao tocar
 * num lançamento). `null` enquanto carrega.
 */
class AliasesViewModel(private val repository: AliasRepository) : ViewModel() {

    val aliases: StateFlow<List<Alias>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(alias: Alias) {
        viewModelScope.launch { repository.save(alias) }
    }

    fun delete(alias: Alias) {
        viewModelScope.launch { repository.delete(alias) }
    }

    /** Desfaz uma exclusão recriando o apelido. */
    fun restore(alias: Alias) = save(alias.copy(id = 0))
}
