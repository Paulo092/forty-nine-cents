@file:OptIn(ExperimentalMaterial3Api::class)

package com.fortyninecents.ui.review

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import com.fortyninecents.notification.BankNotificationListenerService
import com.fortyninecents.ui.AppViewModelProvider
import com.fortyninecents.ui.aliases.AliasesViewModel
import com.fortyninecents.ui.aliases.TransactionAliasDialog
import com.fortyninecents.ui.components.EmptyState
import com.fortyninecents.ui.components.TransactionRow
import com.fortyninecents.ui.components.UiFormat
import com.fortyninecents.ui.components.icon
import kotlinx.coroutines.launch

private const val SAMPLE_NOTIFICATION =
    "Compra de R$ 40,00 APROVADA em SUPER MERCADO JULLIUS para o cartão com final 1234"

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = AppViewModelProvider.Factory),
    aliasesViewModel: AliasesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aliases by aliasesViewModel.aliases.collectAsStateWithLifecycle()
    var aliasTarget by remember { mutableStateOf<Transaction?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var listenerEnabled by remember { mutableStateOf(BankNotificationListenerService.isEnabled(context)) }
    var showSimulation by rememberSaveable { mutableStateOf(false) }

    // o usuário volta das Configurações: reavalia a permissão
    LifecycleResumeEffect(Unit) {
        listenerEnabled = BankNotificationListenerService.isEnabled(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.messageFlow.collect { message ->
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Verificar notificações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showSimulation = true }) {
                        Icon(Icons.Filled.Science, contentDescription = "Simular notificação")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "permission") {
                PermissionCard(enabled = listenerEnabled, onOpenSettings = { openNotificationAccessSettings(context) })
            }
            item(key = "sms") {
                SmsCaptureCard(enabled = state.smsCaptureEnabled, onToggle = viewModel::setSmsCaptureEnabled)
            }

            if (state.pending.isNotEmpty()) {
                item(key = "pending-header") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (state.pending.size == 1) "1 lançamento para revisar" else "${state.pending.size} lançamentos para revisar",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::confirmAll) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirmar todos")
                        }
                    }
                }
                items(state.pending, key = { it.id }) { transaction ->
                    PendingCard(
                        transaction = transaction,
                        onConfirm = { viewModel.confirm(transaction) },
                        onDiscard = { viewModel.discard(transaction) },
                        onCategoryChange = { viewModel.changeCategory(transaction, it) },
                        onRename = { aliasTarget = transaction },
                    )
                }
            } else if (!state.isLoading) {
                item(key = "empty") {
                    EmptyState(
                        title = "Tudo revisado",
                        message = if (listenerEnabled) {
                            "Quando chegar uma notificação de compra ou Pix, ela aparece aqui para você confirmar."
                        } else {
                            "Ative a captura automática acima para registrar seus gastos sem digitar nada."
                        },
                        modifier = Modifier.fillMaxWidth(),
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

    if (showSimulation) {
        SimulationDialog(
            onDismiss = { showSimulation = false },
            onSubmit = { text ->
                showSimulation = false
                viewModel.simulate(text)
            },
        )
    }
}

@Composable
private fun PermissionCard(enabled: Boolean, onOpenSettings: () -> Unit) {
    val colors = if (enabled) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Card(colors = colors, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (enabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                    contentDescription = null,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (enabled) "Captura automática ativa" else "Captura automática desativada",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = if (enabled) {
                    "Lemos apenas notificações de apps de bancos. Tudo é processado no seu celular."
                } else {
                    "Permita que o 49 Centavos leia as notificações dos apps de banco para registrar " +
                        "compras e Pix automaticamente. Nada sai do seu celular."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (enabled) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) { Text("Gerenciar") }
            } else {
                Button(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) { Text("Permitir acesso") }
            }
        }
    }
}

@Composable
private fun SmsCaptureCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(Icons.Filled.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Ler SMS de bancos", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Só de remetentes que parecem bancos (números curtos ou nome do banco). " +
                        "Qualquer pessoa pode enviar SMS, então revise com atenção.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PendingCard(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onCategoryChange: (Category) -> Unit,
    onRename: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        TransactionRow(
            transaction = transaction,
            subtitle = listOfNotNull(
                transaction.sourceApp,
                "${UiFormat.dayMonth(transaction.occurredAt)} às ${UiFormat.time(transaction.occurredAt)}",
            ).joinToString(" · "),
            // tocar no lançamento abre o diálogo de apelido
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Dar apelido", onClick = onRename),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(Modifier.weight(1f)) {
                AssistChip(
                    onClick = { menuOpen = true },
                    label = { Text(transaction.category.label) },
                    leadingIcon = { Icon(transaction.category.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Trocar categoria") },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Category.forType(transaction.type).forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.label) },
                            leadingIcon = { Icon(category.icon, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onCategoryChange(category)
                            },
                        )
                    }
                }
            }
            OutlinedIconButton(onClick = onDiscard) {
                Icon(Icons.Filled.Close, contentDescription = "Descartar")
            }
            Button(onClick = onConfirm) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Confirmar")
            }
        }
    }
}

@Composable
private fun SimulationDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(SAMPLE_NOTIFICATION) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Science, contentDescription = null) },
        title = { Text("Simular notificação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Cole o texto de uma notificação do seu banco para testar a leitura automática.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(text) }, enabled = text.isNotBlank()) { Text("Processar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun openNotificationAccessSettings(context: Context) {
    val fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            BankNotificationListenerService.componentName(context).flattenToString(),
        )
    } else {
        fallback
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallback)
    }
}
