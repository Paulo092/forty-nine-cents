package com.fortyninecents.domain

import com.fortyninecents.util.normalizeForMatching

/**
 * Sugere uma categoria a partir da descrição do lançamento usando palavras-chave.
 * Simples de propósito: é fácil de testar e de evoluir (ex.: aprender com correções do usuário).
 */
class CategoryClassifier {

    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.SALARIO to listOf("salario", "folha de pagamento", "proventos"),
        // marketplaces antes de MERCADO, senão "mercado livre" cairia em supermercado
        Category.COMPRAS to listOf("mercadolivre", "mercado livre"),
        Category.MERCADO to listOf(
            "supermercado", "mercado", "atacad", "hortifruti", "sacolao", "carrefour", "assai",
            "pao de acucar", "extra hiper", "dia brasil", "zaffari", "guanabara",
        ),
        Category.ALIMENTACAO to listOf(
            "restaurante", "ifood", "rappi", "lanchonete", "padaria", "burger", "pizza", "cafe",
            "mcdonald", "bk ", "subway", "outback", "acai", "sorvet", "churrasc", "bar e ", "lanche",
        ),
        Category.TRANSPORTE to listOf(
            "uber", "99app", "99 pop", "cabify", "posto", "combustivel", "gasolina", "shell", "ipiranga",
            "petrobras", "estacionamento", "metro", "onibus", "bilhete unico", "pedagio", "sem parar",
        ),
        Category.MORADIA to listOf(
            "aluguel", "condominio", "energia", "enel", "cemig", "copel", "light s", "sabesp", "agua",
            "internet", "vivo", "claro", "tim ", "oi fibra", "gas ",
        ),
        Category.SAUDE to listOf(
            "farmacia", "drogaria", "droga", "pague menos", "hospital", "clinica",
            "laboratorio", "odonto", "unimed", "amil", "academia", "smartfit", "smart fit",
        ),
        Category.LAZER to listOf(
            "cinema", "netflix", "spotify", "disney", "hbo", "prime video", "steam", "playstation",
            "xbox", "ingresso", "show", "teatro", "parque",
        ),
        Category.EDUCACAO to listOf("escola", "curso", "faculdade", "universidade", "livraria", "udemy", "alura"),
        Category.SERVICOS to listOf("lavanderia", "barbearia", "salao", "cabeleireiro", "manicure", "oficina", "conserto"),
        Category.COMPRAS to listOf(
            "amazon", "mercadolivre", "mercado livre", "shopee", "magalu", "magazine", "americanas",
            "shein", "aliexpress", "renner", "riachuelo", "c&a", "zara", "shopping", "loja",
        ),
        Category.TRANSFERENCIA to listOf("pix", "transferencia", "ted ", "doc "),
    )

    fun classify(description: String, type: TransactionType): Category {
        // espaço extra no fim permite casar chaves como "tim " no final da string
        val text = description.normalizeForMatching() + " "
        val match = rules.firstOrNull { (category, keywords) ->
            (type == TransactionType.INCOME || !category.isIncome) && keywords.any { text.contains(it) }
        }?.first
        return match ?: if (type == TransactionType.INCOME) Category.RECEITA else Category.OUTROS
    }
}
