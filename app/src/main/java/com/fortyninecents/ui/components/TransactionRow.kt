package com.fortyninecents.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fortyninecents.R
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionType
import com.fortyninecents.ui.theme.AppTheme
import com.fortyninecents.util.Money

/** Linha de lançamento: ícone da categoria, descrição (apelido, se houver) e valor colorido (vermelho/verde). */
@Composable
fun TransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    subtitle: String = defaultSubtitle(transaction),
    /** Mostra, em letra pequena, o identificador do banco quando o lançamento tem apelido. */
    showOriginalTitle: Boolean = false,
) {
    val money = AppTheme.money
    val amountColor = if (transaction.type == TransactionType.EXPENSE) money.expense else money.income

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(transaction)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.displayTitle.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showOriginalTitle && transaction.nickname != null) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.source == TransactionSource.NOTIFICATION) {
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = "Capturado de notificação",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = Money.formatSigned(transaction.signedCents),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor,
        )
    }
}

private fun defaultSubtitle(t: Transaction): String =
    listOfNotNull(t.category.label, UiFormat.dayMonth(t.occurredAt), t.sourceApp).joinToString(" · ")

@Composable
fun CategoryBadge(transaction: Transaction, size: Dp = 40.dp) {
    val money = AppTheme.money
    val tint = if (transaction.type == TransactionType.EXPENSE) money.expense else money.income
    Box(
        modifier = Modifier
            .size(size)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = transaction.category.icon,
            contentDescription = transaction.category.label,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun WalletLogo(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Image(
        painter = painterResource(R.drawable.ic_wallet),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** Rótulo pequeno em caixa alta, como "ETIQUETA" e "DATA" do protótipo. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

/** Estado vazio reutilizável. */
@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WalletLogo(size = 56.dp, modifier = Modifier.padding(bottom = 8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
