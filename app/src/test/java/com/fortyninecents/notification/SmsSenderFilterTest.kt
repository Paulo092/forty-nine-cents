package com.fortyninecents.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSenderFilterTest {

    @Test
    fun `aceita codigos curtos e nomes de banco`() {
        listOf("27888", "29292", "4004", "NUBANK", "Itaú", "BB", "Banco do Brasil", "C6 Bank", "Inter").forEach {
            assertTrue("deveria aceitar $it", SmsSenderFilter.isTrusted(it))
        }
    }

    @Test
    fun `rejeita numeros comuns e contatos pessoais`() {
        listOf(
            "+55 11 99999-9999", "(11) 98888-7777", "11988887777", "Mãe", "João Pereira",
            "Interfone portaria", "Bbzinho", "", null,
        ).forEach {
            assertFalse("deveria rejeitar $it", SmsSenderFilter.isTrusted(it))
        }
    }
}
