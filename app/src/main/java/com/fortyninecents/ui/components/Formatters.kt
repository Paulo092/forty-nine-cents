package com.fortyninecents.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formatadores de data usados pela UI (sempre em pt-BR, independente do idioma do aparelho). */
object UiFormat {
    private val ptBR: Locale = Locale.forLanguageTag("pt-BR")
    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yy", ptBR)
    private val time = DateTimeFormatter.ofPattern("HH:mm", ptBR)
    private val dayHeader = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)
    private val dayMonth = DateTimeFormatter.ofPattern("dd/MM", ptBR)

    fun shortDate(date: LocalDate): String = shortDate.format(date)

    fun time(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        time.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun dayMonth(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dayMonth.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun dayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> dayHeader.format(date).replaceFirstChar { it.titlecase(ptBR) }
    }

    fun month(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, ptBR).replaceFirstChar { it.titlecase(ptBR) } + " " + month.year

    fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, ptBR)
}
