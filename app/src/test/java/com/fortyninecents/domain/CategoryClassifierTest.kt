package com.fortyninecents.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    private val classifier = CategoryClassifier()

    @Test
    fun `classifica despesas comuns`() {
        assertEquals(Category.MERCADO, classifier.classify("SUPER MERCADO JULLIUS", TransactionType.EXPENSE))
        assertEquals(Category.SERVICOS, classifier.classify("Lavanderia Super Clean", TransactionType.EXPENSE))
        assertEquals(Category.TRANSPORTE, classifier.classify("Uber do Brasil", TransactionType.EXPENSE))
        assertEquals(Category.SAUDE, classifier.classify("Drogaria São Paulo", TransactionType.EXPENSE))
        assertEquals(Category.ALIMENTACAO, classifier.classify("Padaria Bom Pão", TransactionType.EXPENSE))
        assertEquals(Category.COMPRAS, classifier.classify("MERCADOLIVRE*LOJA", TransactionType.EXPENSE))
    }

    @Test
    fun `sem correspondencia usa categoria padrao do tipo`() {
        assertEquals(Category.OUTROS, classifier.classify("XPTO LTDA", TransactionType.EXPENSE))
        assertEquals(Category.RECEITA, classifier.classify("Joao Pereira", TransactionType.INCOME))
    }

    @Test
    fun `salario so vale para receitas`() {
        assertEquals(Category.SALARIO, classifier.classify("Salário outubro", TransactionType.INCOME))
        assertEquals(Category.OUTROS, classifier.classify("Salário outubro", TransactionType.EXPENSE))
    }
}
