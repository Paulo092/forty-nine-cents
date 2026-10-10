package com.fortyninecents.ui.aliases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.fortyninecents.domain.Alias
import com.fortyninecents.domain.AliasMatch
import com.fortyninecents.domain.AliasResolver
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.aliasFromEdit

/**
 * Formulário de apelido.
 *
 * @param original identificador do lançamento de onde o diálogo foi aberto (só informativo).
 * @param onRemove null quando ainda não existe apelido.
 */
@Composable
fun AliasDialog(
    title: String,
    initialPattern: String,
    initialNickname: String,
    initialMatch: AliasMatch,
    original: String?,
    onSave: (pattern: String, nickname: String, match: AliasMatch) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var nickname by rememberSaveable { mutableStateOf(initialNickname) }
    var pattern by rememberSaveable { mutableStateOf(initialPattern) }
    var contains by rememberSaveable { mutableStateOf(initialMatch == AliasMatch.CONTAINS) }
    val canSave = nickname.isNotBlank() && pattern.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (original != null) {
                    Text(
                        text = original,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(MAX_LENGTH) },
                    label = { Text("Apelido") },
                    placeholder = { Text("Ex.: Feira do Zé") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it.take(MAX_LENGTH) },
                    label = { Text(if (contains) "Trecho do identificador" else "Identificador do banco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = contains, onCheckedChange = { contains = it })
                    Text(
                        "Usar para todo identificador que contenha esse texto",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (contains) {
                    Text(
                        "Dica: deixe só a parte fixa. \"UBER *TRIP\" cobre todas as corridas, " +
                            "mesmo com números diferentes no final.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(pattern.trim(), nickname.trim(), if (contains) AliasMatch.CONTAINS else AliasMatch.EXACT)
                },
                enabled = canSave,
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Remover") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

/** Abre o [AliasDialog] a partir de um lançamento, editando o apelido que já se aplica a ele (se houver). */
@Composable
fun TransactionAliasDialog(
    transaction: Transaction,
    aliases: List<Alias>,
    onSave: (Alias) -> Unit,
    onRemove: (Alias) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = remember(transaction.title, aliases) { AliasResolver(aliases).match(transaction.title) }
    AliasDialog(
        title = if (existing == null) "Dar apelido" else "Editar apelido",
        initialPattern = existing?.pattern ?: transaction.title,
        initialNickname = existing?.nickname.orEmpty(),
        initialMatch = existing?.matchType ?: AliasMatch.EXACT,
        original = transaction.title,
        onSave = { pattern, nickname, match ->
            onSave(aliasFromEdit(existing, pattern, nickname, match))
        },
        onRemove = existing?.let { alias -> { onRemove(alias) } },
        onDismiss = onDismiss,
    )
}

private const val MAX_LENGTH = 60
