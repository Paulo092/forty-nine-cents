@file:OptIn(ExperimentalMaterial3Api::class)

package com.fortyninecents.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionType
import com.fortyninecents.ui.AppViewModelProvider
import com.fortyninecents.ui.components.CurrencyVisualTransformation
import com.fortyninecents.ui.components.FieldLabel
import com.fortyninecents.ui.components.TransactionRow
import com.fortyninecents.ui.components.UiFormat
import com.fortyninecents.ui.components.WalletLogo
import com.fortyninecents.ui.components.icon
import com.fortyninecents.ui.theme.AmountTextStyle
import com.fortyninecents.ui.theme.AppTheme
import com.fortyninecents.ui.theme.FortyNineCentsTheme
import com.fortyninecents.util.Money
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun HomeScreen(
    onOpenTransactions: () -> Unit,
    onOpenReview: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HomeEvent.Saved -> {
                    focusManager.clearFocus()
                    launch {
                        val kind = if (event.transaction.type == TransactionType.EXPENSE) "Gasto" else "Receita"
                        val result = snackbarHostState.showSnackbar(
                            message = "$kind de ${Money.format(event.transaction.amountCents)} salvo",
                            actionLabel = "Desfazer",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.transaction)
                    }
                }
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = HomeActions(
            onAmountChange = viewModel::onAmountChange,
            onTitleChange = viewModel::onTitleChange,
            onTypeChange = viewModel::onTypeChange,
            onCategoryChange = viewModel::onCategoryChange,
            onUseTodayChange = viewModel::onUseTodayChange,
            onDateChange = viewModel::onDateChange,
            onSave = viewModel::save,
            onOpenTransactions = onOpenTransactions,
            onOpenReview = onOpenReview,
        ),
    )
}

/** Agrupa os callbacks para manter a assinatura do conteúdo enxuta (e fácil de usar em previews). */
data class HomeActions(
    val onAmountChange: (String) -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onTypeChange: (TransactionType) -> Unit = {},
    val onCategoryChange: (Category) -> Unit = {},
    val onUseTodayChange: (Boolean) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onSave: () -> Unit = {},
    val onOpenTransactions: () -> Unit = {},
    val onOpenReview: () -> Unit = {},
)

@Composable
fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { CheckNotificationsBar(pendingCount = state.pendingCount, onClick = actions.onOpenReview) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header") {
                Header(state = state, onClick = actions.onOpenTransactions)
            }
            item(key = "amount") {
                AmountSection(form = state.form, onAmountChange = actions.onAmountChange, onTypeChange = actions.onTypeChange)
            }
            item(key = "title") {
                TitleField(title = state.form.title, onTitleChange = actions.onTitleChange)
            }
            item(key = "category") {
                CategorySelector(form = state.form, onCategoryChange = actions.onCategoryChange)
            }
            item(key = "date") {
                DateAndSaveRow(
                    form = state.form,
                    onUseTodayChange = actions.onUseTodayChange,
                    onDateChange = actions.onDateChange,
                    onSave = actions.onSave,
                )
            }
            item(key = "recent") {
                RecentCard(recent = state.recent, onSeeDetails = actions.onOpenTransactions)
            }
        }
    }
}

@Composable
private fun Header(state: HomeUiState, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        WalletLogo(size = 64.dp)
        Text(
            text = "49 CENTAVOS",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        MonthSummary(state = state, onClick = onClick)
    }
}

@Composable
private fun MonthSummary(state: HomeUiState, onClick: () -> Unit) {
    val money = AppTheme.money
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SummaryItem("Entradas", Money.format(state.monthIncome), money.income, Modifier.weight(1f))
            SummaryItem("Saídas", Money.format(state.monthExpense), money.expense, Modifier.weight(1f))
            SummaryItem(
                label = "Saldo de ${UiFormat.monthName(YearMonth.now())}",
                value = Money.formatSigned(state.monthBalance),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.3f),
            )
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun AmountSection(
    form: EntryForm,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
) {
    val money = AppTheme.money
    val accent = if (form.type == TransactionType.EXPENSE) money.expense else money.income
    val amountColor = if (form.amountCents == 0L) money.neutral else accent

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TransactionType.entries.forEachIndexed { index, type ->
                val color = if (type == TransactionType.EXPENSE) money.expense else money.income
                SegmentedButton(
                    selected = form.type == type,
                    onClick = { onTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = color.copy(alpha = 0.14f),
                        activeContentColor = color,
                    ),
                    label = { Text(if (type == TransactionType.EXPENSE) "Despesa" else "Receita") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BasicTextField(
            value = form.amountDigits,
            onValueChange = onAmountChange,
            singleLine = true,
            textStyle = AmountTextStyle.copy(color = amountColor, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            visualTransformation = remember { CurrencyVisualTransformation() },
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 4.dp),
            thickness = 1.5.dp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun TitleField(title: String, onTitleChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column {
        FieldLabel("Etiqueta")
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Identificador do gasto") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategorySelector(form: EntryForm, onCategoryChange: (Category) -> Unit) {
    Column {
        FieldLabel("Categoria")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Category.forType(form.type), key = { it.name }) { category ->
                FilterChip(
                    selected = form.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category.label) },
                    leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DateAndSaveRow(
    form: EntryForm,
    onUseTodayChange: (Boolean) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Data")
                Surface(
                    onClick = { showPicker = true },
                    shape = MaterialTheme.shapes.large,
                    color = if (form.useToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "Escolher data",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = UiFormat.shortDate(form.date),
                            color = if (form.useToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onSave,
                enabled = form.canSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text("PRONTO", style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.useToday, onCheckedChange = onUseTodayChange)
            Text("HOJE", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = PastAndTodayDates,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Não faz sentido registrar gastos no futuro. O DatePicker trabalha em UTC. */
private object PastAndTodayDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

@Composable
private fun RecentCard(recent: List<Transaction>, onSeeDetails: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        if (recent.isEmpty()) {
            Text(
                text = "Nenhum lançamento ainda.\nRegistre seu primeiro gasto acima ou ative a captura automática.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        } else {
            recent.forEachIndexed { index, transaction ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TransactionRow(transaction, modifier = Modifier.fillMaxWidth())
            }
        }
        Surface(
            onClick = onSeeDetails,
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "VER COM DETALHES",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun CheckNotificationsBar(pendingCount: Int, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onClick,
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("VERIFICAR NOTIFICAÇÕES", style = MaterialTheme.typography.labelLarge)
                if (pendingCount > 0) {
                    Spacer(Modifier.width(10.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) { Text(pendingCount.toString()) }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomePreview() {
    val now = System.currentTimeMillis()
    FortyNineCentsTheme(darkTheme = false) {
        HomeContent(
            state = HomeUiState(
                form = EntryForm(amountDigits = "4000", title = "Super Mercado Jullius", category = Category.MERCADO),
                recent = listOf(
                    Transaction(1, "Super Mercado Jullius", 4000, TransactionType.EXPENSE, Category.MERCADO, now),
                    Transaction(2, "Depósito bancário", 1000, TransactionType.INCOME, Category.RECEITA, now),
                    Transaction(3, "Lavanderia Super Clean", 4000, TransactionType.EXPENSE, Category.SERVICOS, now),
                ),
                pendingCount = 2,
                monthIncome = 1000,
                monthExpense = 8000,
            ),
            actions = HomeActions(),
        )
    }
}
