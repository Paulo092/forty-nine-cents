package com.fortyninecents.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AliasResolverTest {

    private fun tx(title: String) = Transaction(
        title = title, amountCents = 100, type = TransactionType.EXPENSE,
        category = Category.OUTROS, occurredAt = 0,
    )

    @Test
    fun `igualdade ignora maiusculas acentos e espacos extras`() {
        val resolver = AliasResolver(listOf(Alias(pattern = "PAG*JOSÉ  DA SILVA", nickname = "Feira do Zé")))
        assertEquals("Feira do Zé", resolver.nicknameFor("pag*jose da silva"))
        assertNull(resolver.nicknameFor("PAG*JOSE DA SILVA 2"))
    }

    @Test
    fun `contem casa variacoes do identificador`() {
        val resolver = AliasResolver(listOf(Alias(pattern = "UBER *TRIP", nickname = "Uber", matchType = AliasMatch.CONTAINS)))
        assertEquals("Uber", resolver.nicknameFor("UBER *TRIP 1234"))
        assertEquals("Uber", resolver.nicknameFor("Uber *Trip HELP.UBER.COM"))
        assertNull(resolver.nicknameFor("UBER EATS"))
    }

    @Test
    fun `exato tem prioridade e entre contem vence o mais especifico`() {
        val resolver = AliasResolver(
            listOf(
                Alias(pattern = "IFOOD", nickname = "iFood", matchType = AliasMatch.CONTAINS),
                Alias(pattern = "IFOOD *PIZZARIA", nickname = "Pizza da sexta", matchType = AliasMatch.CONTAINS),
                Alias(pattern = "IFOOD *MERCADO", nickname = "Mercado pelo iFood"),
            ),
        )
        assertEquals("Mercado pelo iFood", resolver.nicknameFor("IFOOD *MERCADO"))
        assertEquals("Pizza da sexta", resolver.nicknameFor("IFOOD *PIZZARIA BELLA"))
        assertEquals("iFood", resolver.nicknameFor("IFOOD *LANCHES"))
    }

    @Test
    fun `aplica no lancamento sem alterar o identificador original`() {
        val resolver = AliasResolver(listOf(Alias(pattern = "SUPERMERCADO JULLIUS", nickname = "Mercado")))
        val result = resolver.apply(tx("SUPERMERCADO JULLIUS"))
        assertEquals("SUPERMERCADO JULLIUS", result.title)
        assertEquals("Mercado", result.displayTitle)
        assertEquals("OUTRA LOJA", resolver.apply(tx("OUTRA LOJA")).displayTitle)
    }

    @Test
    fun `padroes em branco sao ignorados`() {
        val resolver = AliasResolver(listOf(Alias(pattern = "  ", nickname = "x", matchType = AliasMatch.CONTAINS)))
        assertNull(resolver.nicknameFor("QUALQUER COISA"))
        assertNull(AliasResolver.EMPTY.nicknameFor(""))
    }

    @Test
    fun `editar regra contem como exato cria apelido novo e preserva a regra geral`() {
        val geral = Alias(id = 7, pattern = "UBER *TRIP", nickname = "Uber", matchType = AliasMatch.CONTAINS)

        val especifico = aliasFromEdit(geral, "UBER *TRIP 1234", "Uber do trabalho", AliasMatch.EXACT)
        assertEquals(0L, especifico.id)

        // continua sendo a regra geral: edita no lugar
        assertEquals(7L, aliasFromEdit(geral, "UBER *TRIP", "Corridas", AliasMatch.CONTAINS).id)
        // sem apelido prévio: novo
        assertEquals(0L, aliasFromEdit(null, "PADARIA", "Padaria", AliasMatch.EXACT).id)

        val resolver = AliasResolver(listOf(geral, especifico.copy(id = 8)))
        assertEquals("Uber do trabalho", resolver.nicknameFor("UBER *TRIP 1234"))
        assertEquals("Uber", resolver.nicknameFor("UBER *TRIP 9999"))
    }
}
