@file:OptIn(ExperimentalMaterial3Api::class)

package com.fortyninecents.ui.transactions

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortyninecents.domain.Transaction
import com.fortyninecents.ui.AppViewModelProvider
import com.fortyninecents.ui.aliases.AliasesViewModel
import com.fortyninecents.ui.aliases.TransactionAliasDialog
import com.fortyninecents.ui.components.EmptyState
import com.fortyninecents.ui.components.TransactionRow
import com.fortyninecents.ui.components.UiFormat
import com.fortyninecents.ui.theme.AppTheme
import com.fortyninecents.util.Money
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    onOpenAliases: () -> Unit,
    viewModel: TransactionsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    aliasesViewModel: AliasesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aliases by aliasesViewModel.aliases.collectAsStateWithLifecycle()
    var aliasTarget by remember { mutableStateOf<Transaction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is TransactionsEvent.Share -> try {
                    context.startActivity(event.intent)
                } catch (_: ActivityNotFoundException) {
                    snackbarHostState.showSnackbar("Nenhum app disponível para receber o arquivo")
                }

                is TransactionsEvent.Message -> launch { snackbarHostState.showSnackbar(event.text) }

                is TransactionsEvent.Deleted -> launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "\"${event.transaction.displayTitle}\" excluído",
                        actionLabel = "Desfazer",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.transaction)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Extrato") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAliases) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Apelidos")
                    }
                    if (state.isExporting) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.Share, contentDescription = "Exportar CSV")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Exportar ${UiFormat.monthName(state.month)} (CSV)") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.export(ExportScope.MONTH)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar tudo (CSV)") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.export(ExportScope.ALL)
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "month") {
                MonthSelector(state, onPrevious = viewModel::previousMonth, onNext = viewModel::nextMonth)
            }
            item(key = "summary") { Summary(state) }

            if (aliases?.isEmpty() == true && state.groups.isNotEmpty()) {
                item(key = "alias-hint") {
                    Text(
                        "Dica: toque em um lançamento para dar um apelido ao identificador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            if (state.isEmpty) {
                item(key = "empty") {
                    EmptyState(
                        title = "Nada por aqui",
                        message = "Nenhum lançamento em ${UiFormat.monthName(state.month)}.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.groups.forEach { group ->
                item(key = "header-${group.date}") { DayHeader(group) }
                items(group.items, key = { it.id }) { transaction ->
                    SwipeToDeleteRow(
                        transaction = transaction,
                        onClick = { aliasTarget = transaction },
                        onDelete = { viewModel.delete(transaction) },
                    )
                }
            }
        }
    }

    aliasTarget?.let { transaction ->
        TransactionAliasDialog(
            transaction = transaction,
            aliases = aliases.orEmpty(),
            onSave = {
                aliasesViewModel.save(it)
                aliasTarget = null
            },
            onRemove = {
                aliasesViewModel.delete(it)
                aliasTarget = null
            },
            onDismiss = { aliasTarget = null },
        )
    }
}

@Composable
private fun MonthSelector(state: TransactionsUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior") }
        Text(UiFormat.month(state.month), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = state.canGoForward) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês")
        }
    }
}

@Composable
private fun Summary(state: TransactionsUiState) {
    val money = AppTheme.money
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(16.dp)) {
            SummaryColumn("Entradas", Money.format(state.incomeCents), money.income, Modifier.weight(1f))
            SummaryColumn("Saídas", Money.format(state.expenseCents), money.expense, Modifier.weight(1f))
            SummaryColumn(
                "Saldo",
                Money.formatSigned(state.balanceCents),
                if (state.balanceCents < 0) money.expense else MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun DayHeader(group: DayGroup) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp),
    ) {
        Text(
            UiFormat.dayHeader(group.date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            Money.formatSigned(group.totalCents),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwipeToDeleteRow(transaction: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.money.expense, MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 1.dp,
        ) {
            TransactionRow(
                transaction = transaction,
                subtitle = listOfNotNull(
                    transaction.category.label,
                    UiFormat.time(transaction.occurredAt),
                    transaction.sourceApp,
                ).joinToString(" · "),
                showOriginalTitle = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
