package com.fortyninecents.notification

/**
 * Apps cujas notificações são analisadas. Notificações de outros apps são ignoradas
 * logo na entrada do serviço (privacidade + desempenho).
 *
 * O package de quem postou a notificação é preenchido pelo próprio Android, então
 * outro app não consegue se passar por um banco desta lista.
 *
 * Para suportar um banco novo basta adicionar o package aqui (veja em
 * "Configurações > Apps > <app>" ou na URL da Play Store: ...?id=<package>).
 */
object BankApps {

    private val banks: Map<String, String> = mapOf(
        "com.nu.production" to "Nubank",
        "com.itau" to "Itaú",
        "br.com.intermedium" to "Inter",
        "com.c6bank.app" to "C6 Bank",
        "com.bradesco" to "Bradesco",
        "br.com.bradesco.next" to "Next",
        "com.santander.app" to "Santander",
        "br.com.bb.android" to "Banco do Brasil",
        "br.com.gabba.Caixa" to "Caixa",
        "br.gov.caixa.tem" to "Caixa Tem",
        "com.picpay" to "PicPay",
        "com.mercadopago.wallet" to "Mercado Pago",
        "br.com.uol.ps.myaccount" to "PagBank",
        "com.neon" to "Neon",
        "br.com.original.bank" to "Original",
        "com.btg.pactual.banking" to "BTG",
        "br.com.xp.carteira" to "XP",
        "com.willbank" to "Will Bank",
    )

    /**
     * Apps de SMS. Diferente dos apps de banco, aqui QUALQUER pessoa pode mandar uma mensagem,
     * então a leitura é opcional (desligada por padrão) e passa pelo [SmsSenderFilter].
     */
    private val smsApps: Set<String> = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
    )

    fun isBank(packageName: String): Boolean = packageName in banks

    fun isSms(packageName: String): Boolean = packageName in smsApps

    fun displayName(packageName: String): String = when {
        packageName in banks -> banks.getValue(packageName)
        packageName in smsApps -> "SMS"
        else -> packageName
    }
}
