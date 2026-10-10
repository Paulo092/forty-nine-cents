package com.fortyninecents.domain

enum class Category(val label: String, val isIncome: Boolean = false) {
    MERCADO("Mercado"),
    ALIMENTACAO("Alimentação"),
    TRANSPORTE("Transporte"),
    MORADIA("Moradia"),
    SAUDE("Saúde"),
    LAZER("Lazer"),
    COMPRAS("Compras"),
    EDUCACAO("Educação"),
    SERVICOS("Serviços"),
    TRANSFERENCIA("Transferência"),
    OUTROS("Outros"),
    SALARIO("Salário", isIncome = true),
    RECEITA("Receita", isIncome = true);

    companion object {
        /** Categorias sugeridas na UI para cada tipo de lançamento. */
        fun forType(type: TransactionType): List<Category> = when (type) {
            TransactionType.EXPENSE -> entries.filter { !it.isIncome }
            TransactionType.INCOME -> listOf(SALARIO, RECEITA, TRANSFERENCIA, OUTROS)
        }

        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: OUTROS
    }
}
