package com.fortyninecents.notification

import com.fortyninecents.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationParserTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val parser = NotificationParser(zone)

    /** 05/10/2026 14:30 em São Paulo */
    private val postedAt = at(2026, 10, 5, 14, 30)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun parse(title: String?, text: String?) = parser.parse(title, text, postedAt)

    @Test
    fun `nubank compra no credito`() {
        val result = parse(
            "Compra aprovada",
            "Compra de R$ 40,00 APROVADA em SUPERMERCADO JULLIUS para o cartão com final 1234",
        )
        assertNotNull(result)
        assertEquals(4000L, result!!.amountCents)
        assertEquals("SUPERMERCADO JULLIUS", result.description)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(postedAt, result.occurredAt)
    }

    @Test
    fun `itau compra com data e hora no texto`() {
        val result = parse(
            "Itaú",
            "Compra aprovada no seu cartão final 1234 de R$ 1.234,56 em 04/10 às 21:15, LAVANDERIA SUPER CLEAN.",
        )!!
        assertEquals(123456L, result.amountCents)
        assertEquals("LAVANDERIA SUPER CLEAN", result.description)
        assertEquals(at(2026, 10, 4, 21, 15), result.occurredAt)
    }

    @Test
    fun `c6 compra com valor antes do estabelecimento`() {
        val result = parse(
            "C6 Bank",
            "Compra aprovada no C6 Mastercard final 1234 no valor de R$ 89,90, dia 05/10/2026 às 13:00, em PADARIA BOM PAO.",
        )!!
        assertEquals(8990L, result.amountCents)
        assertEquals("PADARIA BOM PAO", result.description)
        assertEquals(at(2026, 10, 5, 13, 0), result.occurredAt)
    }

    @Test
    fun `bradesco sms em maiusculas`() {
        val result = parse(
            null,
            "BRADESCO CARTOES: COMPRA APROVADA NO CARTAO FINAL 1234 EM 05/10/2026 13:00. VALOR DE R$ 40,00 SUPERMERCADO JULLIUS.",
        )!!
        assertEquals(4000L, result.amountCents)
        assertEquals("SUPERMERCADO JULLIUS", result.description)
        assertEquals(at(2026, 10, 5, 13, 0), result.occurredAt)
    }

    @Test
    fun `pix enviado`() {
        val result = parse("Pix enviado", "Você enviou R$ 50,00 para Maria da Silva")!!
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(5000L, result.amountCents)
        assertEquals("Maria da Silva", result.description)
    }

    @Test
    fun `transferencia recebida e receita`() {
        val result = parse(
            "Transferência recebida",
            "Você recebeu uma transferência de R$ 10,00 de JOAO PEREIRA.",
        )!!
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(1000L, result.amountCents)
        assertEquals("JOAO PEREIRA", result.description)
    }

    @Test
    fun `pagamento com carteira digital`() {
        val result = parse("Pagamento realizado", "Você pagou R$ 25,50 para Uber do Brasil.")!!
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(2550L, result.amountCents)
        assertEquals("Uber do Brasil", result.description)
    }

    @Test
    fun `sem estabelecimento usa o titulo`() {
        val result = parse("Débito automático", "Foi debitado R$ 120,00 da sua conta.")!!
        assertEquals(12000L, result.amountCents)
        assertEquals("Débito automático", result.description)
    }

    @Test
    fun `hora sem data no futuro significa ontem`() {
        val result = parser.parse(
            "Compra aprovada",
            "Compra de R$ 15,00 aprovada às 23:50 em BAR DO ZE",
            at(2026, 10, 6, 0, 5),
        )!!
        assertEquals(at(2026, 10, 5, 23, 50), result.occurredAt)
        assertEquals("BAR DO ZE", result.description)
    }

    @Test
    fun `ignora compra recusada`() {
        assertNull(parse("Compra recusada", "Sua compra de R$ 40,00 em LOJA X foi recusada"))
    }

    @Test
    fun `ignora promocoes e fatura`() {
        assertNull(parse("Oferta", "Ganhe R$ 50,00 de cashback na sua próxima compra"))
        assertNull(parse("Fatura", "Sua fatura de R$ 500,00 vence amanhã"))
    }

    @Test
    fun `ignora notificacao sem valor ou sem palavra-chave`() {
        assertNull(parse("Nubank", "Seu cartão chegou! Desbloqueie agora."))
        assertNull(parse("Saldo", "Seu saldo é R$ 1.000,00"))
    }

    @Test
    fun `aceita espacos nao separaveis`() {
        val result = parse("Compra aprovada", "Compra de R$\u00A07,50 APROVADA em CAFE CENTRAL")!!
        assertEquals(750L, result.amountCents)
        assertEquals("CAFE CENTRAL", result.description)
    }
}
