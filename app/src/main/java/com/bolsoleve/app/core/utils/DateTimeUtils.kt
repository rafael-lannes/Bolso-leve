package com.bolsoleve.app.core.utils

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val ptBrLocale = Locale("pt", "BR")

    val dayNames = listOf(
        "Segunda-feira",
        "Terça-feira",
        "Quarta-feira",
        "Quinta-feira",
        "Sexta-feira",
        "Sábado",
        "Domingo"
    )

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", ptBrLocale)
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", ptBrLocale)
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", ptBrLocale)
        return sdf.format(Date(millis))
    }

    /**
     * Retorna quantos dias faltam até o próximo dia da semana configurado (1 = Segunda, 7 = Domingo)
     * Se hoje for o dia, retorna 0.
     */
    fun daysUntilNextDayOfWeek(targetDayOfWeekIndex: Int): Int {
        val today = LocalDate.now()
        val currentDay = today.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val diff = (targetDayOfWeekIndex - currentDay + 7) % 7
        return diff
    }

    /**
     * Próxima data do dia da semana configurado
     */
    fun getNextDateForDayOfWeek(targetDayOfWeekIndex: Int): LocalDate {
        val daysUntil = daysUntilNextDayOfWeek(targetDayOfWeekIndex)
        return LocalDate.now().plusDays(daysUntil.toLong())
    }

    /**
     * Calcula semanas completas desde o início do tratamento
     */
    fun calculateActiveWeeks(startDateMillis: Long): Int {
        if (startDateMillis <= 0) return 0
        val startDate = Instant.ofEpochMilli(startDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(startDate, today)
        return ((days / 7) + 1).coerceAtLeast(1).toInt()
    }

    /**
     * Retorna o dia da pesagem sugerido (Geralmente 1 dia antes da aplicação ou no mesmo dia pela manhã)
     */
    fun getSuggestedWeighInDay(applicationDayIndex: Int): String {
        // Sugerimos a pesagem no mesmo dia da dose pela manhã
        val index = (applicationDayIndex - 1).coerceIn(0, 6)
        return dayNames[index]
    }

    /**
     * Converte LocalDate para epoch millis
     */
    fun localDateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Converte LocalDateTime para epoch millis
     */
    fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Converte millis para LocalDate
     */
    fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    /**
     * Estima a data de chegada na meta com base na perda média semanal (kg/semana)
     */
    fun estimateGoalArrivalDate(
        currentWeight: Double,
        targetWeight: Double,
        weeklyLossRateKg: Double
    ): String {
        if (weeklyLossRateKg <= 0.05 || currentWeight <= targetWeight) {
            return if (currentWeight <= targetWeight) "Meta atingida!" else "Em ritmo de consolidação"
        }
        val remainingKg = currentWeight - targetWeight
        val weeksNeeded = (remainingKg / weeklyLossRateKg).toLong()
        val estimatedDate = LocalDate.now().plusWeeks(weeksNeeded)
        val formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptBrLocale)
        return estimatedDate.format(formatter).replaceFirstChar { it.uppercase() }
    }
}
