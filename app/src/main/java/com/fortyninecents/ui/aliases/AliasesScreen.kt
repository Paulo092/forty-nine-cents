@file:OptIn(ExperimentalMaterial3Api::class)

package com.fortyninecents.ui.aliases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortyninecents.domain.Alias
import com.fortyninecents.domain.AliasMatch
import com.fortyninecents.ui.AppViewModelProvider
import com.fortyninecents.ui.components.EmptyState
import kotlinx.coroutines.launch

/** Estado do diálogo: criando um apelido novo ou editando um existente. */
private sealed interface Editing {
    data object New : Editing
    data class Existing(val alias: Alias) : Editing
}

@Composable
fun AliasesScreen(
    onBack: () -> Unit,
    viewModel: AliasesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val aliases by viewModel.aliases.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Editing?>(null) }

    fun deleteWithUndo(alias: Alias) {
        viewModel.delete(alias)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Apelido \"${alias.nickname}\" removido",
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(alias)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Apelidos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Editing.New },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo apelido") },
            )
        },
    ) { padding ->
        val list = aliases
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "Nenhum apelido ainda",
                        message = "Dê nomes fáceis para identificadores como \"PAG*JOSEDASILVA\". " +
                            "Você também pode tocar em um lançamento no extrato para criar um.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            items(list.orEmpty(), key = { it.id }) { alias ->
                AliasRow(
                    alias = alias,
                    onClick = { editing = Editing.Existing(alias) },
                    onDelete = { deleteWithUndo(alias) },
                )
            }
        }
    }

    when (val current = editing) {
        null -> Unit
        Editing.New -> AliasDialog(
            title = "Novo apelido",
            initialPattern = "",
            initialNickname = "",
            initialMatch = AliasMatch.EXACT,
            original = null,
            onSave = { pattern, nickname, match ->
                viewModel.save(Alias(pattern = pattern, nickname = nickname, matchType = match))
                editing = null
            },
            onRemove = null,
            onDismiss = { editing = null },
        )
        is Editing.Existing -> AliasDialog(
            title = "Editar apelido",
            initialPattern = current.alias.pattern,
            initialNickname = current.alias.nickname,
            initialMatch = current.alias.matchType,
            original = null,
            onSave = { pattern, nickname, match ->
                viewModel.save(current.alias.copy(pattern = pattern, nickname = nickname, matchType = match))
                editing = null
            },
            onRemove = {
                deleteWithUndo(current.alias)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun AliasRow(alias: Alias, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    alias.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (alias.matchType) {
                        AliasMatch.EXACT -> alias.pattern
                        AliasMatch.CONTAINS -> "contém \"${alias.pattern}\""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remover apelido")
            }
        }
    }
}
