package com.fortyninecents.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formata valores em real`() {
        assertEquals("R$ 0,00", Money.format(0))
        assertEquals("R$ 0,49", Money.format(49))
        assertEquals("R$ 40,00", Money.format(4000))
        assertEquals("R$ 1.234,56", Money.format(123456))
        assertEquals("R$ 1.000.000,00", Money.format(100_000_000))
    }

    @Test
    fun `formata com sinal`() {
        assertEquals("-R$ 40,00", Money.formatSigned(-4000))
        assertEquals("+R$ 10,00", Money.formatSigned(1000))
        assertEquals("R$ 0,00", Money.formatSigned(0))
    }

    @Test
    fun `formato simples para csv`() {
        assertEquals("-40,00", Money.formatPlain(-4000))
        assertEquals("1234,05", Money.formatPlain(123405))
    }

    @Test
    fun `converte digitos do campo de valor`() {
        assertEquals(0L, Money.digitsToCents(""))
        assertEquals(4000L, Money.digitsToCents("4000"))
        assertEquals(4000L, Money.digitsToCents("40a00"))
        assertEquals(99_999_999_999L, Money.digitsToCents("999999999999999"))
    }

    @Test
    fun `interpreta texto brasileiro`() {
        assertEquals(4000L, Money.parseBrl("40,00"))
        assertEquals(123456L, Money.parseBrl("R$ 1.234,56"))
        assertEquals(1000L, Money.parseBrl("10"))
        assertEquals(550L, Money.parseBrl("5,5"))
        assertEquals(100000L, Money.parseBrl("1.000"))
        assertNull(Money.parseBrl("abc"))
        assertNull(Money.parseBrl(""))
    }
}
