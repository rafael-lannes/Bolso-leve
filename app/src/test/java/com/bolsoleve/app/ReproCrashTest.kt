package com.bolsoleve.app

import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.core.utils.WeightUtils
import com.bolsoleve.app.data.local.datastore.UserPreferences
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.WeightRecord
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReproCrashTest {

    @Test
    fun testDateTimeUtilsDays() {
        for (day in 0..8) {
            val days = DateTimeUtils.daysUntilNextDayOfWeek(day)
            println("Day $day -> daysUntil: $days")
            val nextDate = DateTimeUtils.getNextDateForDayOfWeek(day)
            println("Day $day -> nextDate: $nextDate")
            val suggested = DateTimeUtils.getSuggestedWeighInDay(day)
            println("Day $day -> suggested: $suggested")
        }
    }

    @Test
    fun testActiveWeeks() {
        val weeks0 = DateTimeUtils.calculateActiveWeeks(0L)
        val weeksPast = DateTimeUtils.calculateActiveWeeks(System.currentTimeMillis() - 86400000L * 10)
        val weeksFuture = DateTimeUtils.calculateActiveWeeks(System.currentTimeMillis() + 86400000L * 10)
        println("weeks: $weeks0, $weeksPast, $weeksFuture")
    }

    @Test
    fun testProgressPercentageNaN() {
        val initialWeight = 80.0
        val targetWeight = 80.0
        val currentWeight = 80.0
        val totalLostKg = (initialWeight - currentWeight).coerceAtLeast(0.0)
        val totalToLose = (initialWeight - targetWeight).coerceAtLeast(0.1)
        val progressPercentage = ((initialWeight - currentWeight) / totalToLose).coerceIn(0.0, 1.0).toFloat()
        assertFalse(progressPercentage.isNaN())
        assertTrue(progressPercentage in 0f..1f)
    }

    @Test
    fun testWeightSummaryCalculationsEdgeCases() {
        // Test case 1: Initial weight == 0.0 (DataStore default before onboarding or corrupted)
        val initial0 = 0.0
        val target0 = 0.0
        val current0 = 0.0
        val totalLost0 = (initial0 - current0).coerceAtLeast(0.0)
        val totalToLose0 = (initial0 - target0).coerceAtLeast(0.1)
        val progress0 = ((initial0 - current0) / totalToLose0).coerceIn(0.0, 1.0).toFloat()
        println("Case 0: progress = $progress0")
        assertFalse(progress0.isNaN())

        // Test case 2: Current weight > initial weight (weight gain)
        val initialGain = 80.0
        val targetGain = 70.0
        val currentGain = 85.0
        val totalLostGain = (initialGain - currentGain).coerceAtLeast(0.0)
        val totalToLoseGain = (initialGain - targetGain).coerceAtLeast(0.1)
        val progressGain = ((initialGain - currentGain) / totalToLoseGain).coerceIn(0.0, 1.0).toFloat()
        println("Case Gain: progress = $progressGain, totalLost = $totalLostGain")
        assertEquals(0.0f, progressGain, 0.001f)
        assertEquals(0.0, totalLostGain, 0.001)

        // Test case 3: Target weight > initial weight (invalid meta)
        val initialInv = 70.0
        val targetInv = 80.0
        val currentInv = 75.0
        val totalToLoseInv = (initialInv - targetInv).coerceAtLeast(0.1)
        val progressInv = ((initialInv - currentInv) / totalToLoseInv).coerceIn(0.0, 1.0).toFloat()
        println("Case Inv: progress = $progressInv, totalToLose = $totalToLoseInv")
        assertFalse(progressInv.isNaN())
    }

    @Test
    fun testWeeklyVariationsCalculation() {
        val initialWeight = 100.0
        val records = listOf(
            WeightRecord(id = 1, weightKg = 100.0, dateMillis = 1000L),
            WeightRecord(id = 2, weightKg = 98.0, dateMillis = 2000L)
        )
        val variations = mutableListOf<com.bolsoleve.app.domain.model.WeeklyWeightVariation>()
        var previousWeight = initialWeight
        records.takeLast(12).forEachIndexed { index, record ->
            val diff = record.weightKg - previousWeight
            val label = "Sem ${index + 1}"
            variations.add(
                com.bolsoleve.app.domain.model.WeeklyWeightVariation(
                    weekLabel = label,
                    weightDiffKg = diff,
                    averageWeightKg = record.weightKg,
                    dateMillis = record.dateMillis
                )
            )
            previousWeight = record.weightKg
        }
        assertEquals(2, variations.size)
        assertEquals(0.0, variations[0].weightDiffKg, 0.001)
        assertEquals(-2.0, variations[1].weightDiffKg, 0.001)
    }

    @Test
    fun testMiniChartRangeWithSingleRecord() {
        val records = listOf(
            WeightRecord(id = 1, weightKg = 85.0, dateMillis = 1000L)
        )
        val weights = records.map { it.weightKg }
        val minWeight = (weights.minOrNull() ?: 0.0) - 1.0
        val maxWeight = (weights.maxOrNull() ?: 100.0) + 1.0
        val range = (maxWeight - minWeight).coerceAtLeast(1.0)
        val stepX = 300f / (records.size - 1).coerceAtLeast(1)
        println("stepX: $stepX, range: $range")
        assertFalse(stepX.isNaN())
    }
}
