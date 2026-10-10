package com.fortyninecents.export

import com.fortyninecents.domain.Category
import com.fortyninecents.domain.Transaction
import com.fortyninecents.domain.TransactionSource
import com.fortyninecents.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CsvBuilderTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val builder = CsvBuilder(zone)

    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `gera cabecalho e linhas com sinal e aspas escapadas`() {
        val csv = builder.build(
            listOf(
                Transaction(
                    id = 1, title = "Mercado \"Jullius\"", amountCents = 4000, type = TransactionType.EXPENSE,
                    category = Category.MERCADO, occurredAt = millis(2026, 10, 5, 13, 0),
                ),
                Transaction(
                    id = 2, title = "Depósito", amountCents = 1000, type = TransactionType.INCOME,
                    category = Category.RECEITA, occurredAt = millis(2026, 10, 6, 9, 5),
                    source = TransactionSource.NOTIFICATION,
                ),
            ),
        )

        assertTrue(csv.startsWith(CsvBuilder.BOM))
        val lines = csv.removePrefix(CsvBuilder.BOM).split("\r\n")
        assertEquals("\"id\",\"data\",\"hora\",\"descricao\",\"categoria\",\"tipo\",\"valor\",\"origem\"", lines[0])
        assertEquals(
            "\"1\",\"05/10/2026\",\"13:00\",\"Mercado \"\"Jullius\"\"\",\"Mercado\",\"Despesa\",\"-40,00\",\"Manual\"",
            lines[1],
        )
        assertEquals(
            "\"2\",\"06/10/2026\",\"09:05\",\"Depósito\",\"Receita\",\"Receita\",\"10,00\",\"Notificação\"",
            lines[2],
        )
        assertEquals("", lines[3])
    }

    @Test
    fun `neutraliza formulas em campos de texto mas preserva o sinal do valor`() {
        val base = Transaction(
            id = 3, title = "", amountCents = 100, type = TransactionType.EXPENSE,
            category = Category.OUTROS, occurredAt = millis(2026, 10, 5, 13, 0),
        )
        val titles = listOf("=IMPORTXML(\"http://x\";\"//a\")", "+5511999", "-Loja", "@SUM(A1)", "\tTAB", "Loja normal")
        val csv = builder.build(titles.map { base.copy(title = it) })
        val rows = csv.removePrefix(CsvBuilder.BOM).split("\r\n").drop(1).filter { it.isNotEmpty() }

        assertTrue(rows[0].contains(",\"'=IMPORTXML(\"\"http://x\"\";\"\"//a\"\")\","))
        assertTrue(rows[1].contains(",\"'+5511999\","))
        assertTrue(rows[2].contains(",\"'-Loja\","))
        assertTrue(rows[3].contains(",\"'@SUM(A1)\","))
        assertTrue(rows[4].contains(",\"'\tTAB\","))
        assertTrue(rows[5].contains(",\"Loja normal\","))
        // valor negativo continua numérico, sem apóstrofo
        rows.forEach { assertTrue(it.contains(",\"-1,00\",")) }
    }
}
