package com.fortyninecents.export

import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionType
import com.fortyninecents.util.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Gera CSV (RFC 4180) pensado para o Google Sheets/Excel em português:
 * - separador vírgula, todos os campos entre aspas;
 * - datas dd/MM/yyyy e valores com vírgula decimal ("-40,00"), que o Sheets em pt-BR reconhece como número;
 * - "descricao" traz o apelido (se houver) e "identificador_original" o texto como veio do banco;
 * - BOM UTF-8 no início para o Excel não quebrar os acentos;
 * - textos vindos de fora (estabelecimento, nome de quem mandou o Pix) são neutralizados contra
 *   "CSV injection": se começarem com = + - @ ou tab/CR, ganham um apóstrofo na frente para a
 *   planilha tratá-los como texto e não executar uma fórmula como =IMPORTXML(...).
 */
class CsvBuilder(private val zone: ZoneId = ZoneId.systemDefault()) {

    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun build(transactions: List<Transaction>): String = buildString {
        append(BOM)
        appendRow(HEADER)
        transactions.forEach { t ->
            val dateTime = Instant.ofEpochMilli(t.occurredAt).atZone(zone)
            appendRow(
                listOf(
                    t.id.toString(),
                    dateFormat.format(dateTime),
                    timeFormat.format(dateTime),
                    safeText(t.displayTitle),
                    safeText(t.category.label),
                    if (t.type == TransactionType.EXPENSE) "Despesa" else "Receita",
                    Money.formatPlain(t.signedCents),
                    if (t.source == TransactionSource.NOTIFICATION) "Notificação" else "Manual",
                    safeText(t.title),
                ),
            )
        }
    }

    /** Só para campos de texto; a coluna de valor precisa manter o "-" para continuar numérica. */
    private fun safeText(value: String): String =
        if (value.isNotEmpty() && value.first() in FORMULA_TRIGGERS) "'$value" else value

    private fun StringBuilder.appendRow(fields: List<String>) {
        fields.joinTo(this, separator = ",") { "\"" + it.replace("\"", "\"\"") + "\"" }
        append("\r\n")
    }

    companion object {
        const val BOM = "\uFEFF"

        /** Caracteres que fazem Sheets/Excel interpretar a célula como fórmula (inclui as versões de largura total). */
        private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@', '\t', '\r', '\uFF1D', '\uFF0B', '\uFF0D', '\uFF20')
        val HEADER = listOf("id", "data", "hora", "descricao", "categoria", "tipo", "valor", "origem", "identificador_original")
    }
}
