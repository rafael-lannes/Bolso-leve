package com.bolsoleve.app.domain.usecase

import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.data.local.datastore.UserPreferences
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.TreatmentFinancialStats
import com.bolsoleve.app.domain.model.TreatmentSummary
import com.bolsoleve.app.domain.model.WeeklyWeightVariation
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class GetDashboardDataUseCase(
    private val preferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val medicationRepository: MedicationRepository,
    private val doctorRepository: DoctorRepository
) {
    operator fun invoke(): Flow<DashboardData> {
        return combine(
            preferencesRepository.userPreferencesFlow,
            weightRepository.getLatestRecord(),
            weightRepository.getRecentRecords(8),
            medicationRepository.getLatestDose(),
            doctorRepository.getDoctorProfile()
        ) { prefs, latestWeight, recentWeights, latestDose, doctor ->
            val initialWeight = prefs.initialWeight
            val targetWeight = prefs.targetWeight
            val currentWeight = latestWeight?.weightKg ?: initialWeight
            val totalLostKg = (initialWeight - currentWeight).coerceAtLeast(0.0)

            val totalToLose = (initialWeight - targetWeight).coerceAtLeast(0.1)
            val rawProgress = if (totalToLose > 0.0 && !totalToLose.isNaN()) {
                ((initialWeight - currentWeight) / totalToLose).toFloat()
            } else 0f
            val progressPercentage = if (rawProgress.isNaN()) 0f else rawProgress.coerceIn(0f, 1f)

            val daysUntilDose = DateTimeUtils.daysUntilNextDayOfWeek(prefs.applicationDayOfWeek)
            val isDoseToday = daysUntilDose == 0

            // Verifica se hoje já foi registrada alguma dose
            val isTodayConfirmed = latestDose?.let { dose ->
                val doseDate = Instant.ofEpochMilli(dose.appliedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                doseDate == LocalDate.now() && dose.isConfirmed
            } ?: false

            val activeWeeks = DateTimeUtils.calculateActiveWeeks(prefs.treatmentStartDateMillis)
            val suggestedWeighInDay = DateTimeUtils.getSuggestedWeighInDay(prefs.applicationDayOfWeek)

            val summary = TreatmentSummary(
                currentWeight = currentWeight,
                initialWeight = initialWeight,
                targetWeight = targetWeight,
                totalLostKg = totalLostKg,
                progressPercentage = progressPercentage,
                daysUntilNextDose = daysUntilDose,
                nextDoseDate = DateTimeUtils.getNextDateForDayOfWeek(prefs.applicationDayOfWeek).toString(),
                medicationName = prefs.medicationName,
                currentDoseMg = prefs.currentDoseMg,
                isDoseToday = isDoseToday,
                isTodayDoseConfirmed = isTodayConfirmed,
                nextConsultationDateMillis = doctor?.nextConsultationMillis,
                doctorName = doctor?.name.orEmpty(),
                doctorSpecialty = doctor?.specialty.orEmpty(),
                clinicAddress = doctor?.clinicAddress.orEmpty(),
                suggestedWeighInDay = suggestedWeighInDay,
                activeWeeks = activeWeeks
            )

            DashboardData(
                summary = summary,
                recentWeights = recentWeights.reversed(), // Cronológico para mini-gráfico
                doctorProfile = doctor
            )
        }
    }
}

data class DashboardData(
    val summary: TreatmentSummary,
    val recentWeights: List<WeightRecord>,
    val doctorProfile: DoctorProfile?
)

class SaveWeightRecordUseCase(
    private val weightRepository: WeightRepository
) {
    suspend operator fun invoke(
        weightKg: Double,
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ): Result<Long> {
        if (weightKg !in 20.0..450.0) {
            return Result.failure(IllegalArgumentException("O peso informado deve estar entre 20kg e 450kg."))
        }
        val id = weightRepository.saveRecord(
            WeightRecord(
                weightKg = weightKg,
                dateMillis = dateMillis,
                notes = notes
            )
        )
        return Result.success(id)
    }
}

class UpdateWeightRecordUseCase(
    private val weightRepository: WeightRepository
) {
    suspend operator fun invoke(
        id: Long,
        weightKg: Double,
        notes: String = "",
        dateMillis: Long
    ): Result<Unit> {
        if (weightKg !in 20.0..450.0) {
            return Result.failure(IllegalArgumentException("O peso informado deve estar entre 20kg e 450kg."))
        }
        weightRepository.updateRecord(
            WeightRecord(
                id = id,
                weightKg = weightKg,
                dateMillis = dateMillis,
                notes = notes
            )
        )
        return Result.success(Unit)
    }
}

class DeleteWeightRecordUseCase(
    private val weightRepository: WeightRepository
) {
    suspend operator fun invoke(id: Long) {
        weightRepository.deleteRecord(id)
    }
}

class GetAllWeightRecordsUseCase(
    private val weightRepository: WeightRepository
) {
    operator fun invoke(): Flow<List<WeightRecord>> = weightRepository.getAllRecords()
}

class ConfirmDoseApplicationUseCase(
    private val medicationRepository: MedicationRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(medicationName: String, doseMg: Double): Long {
        return medicationRepository.recordDose(
            MedicationDose(
                medicationName = medicationName,
                doseMg = doseMg,
                appliedAtMillis = System.currentTimeMillis(),
                isConfirmed = true
            )
        )
    }
}

class GetTreatmentStatsUseCase(
    private val preferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(): Flow<TreatmentStatsData> {
        return combine(
            preferencesRepository.userPreferencesFlow,
            weightRepository.getAllRecordsAsc(),
            expenseRepository.getAllExpenses()
        ) { prefs, allWeightsAsc, expenses ->
            val initialWeight = prefs.initialWeight
            val targetWeight = prefs.targetWeight
            val currentWeight = allWeightsAsc.lastOrNull()?.weightKg ?: initialWeight
            val totalLostKg = (initialWeight - currentWeight).coerceAtLeast(0.0)

            val activeWeeks = DateTimeUtils.calculateActiveWeeks(prefs.treatmentStartDateMillis)
            val weeklyLossRateKg = if (activeWeeks > 0) totalLostKg / activeWeeks else 0.0

            val estimatedArrival = DateTimeUtils.estimateGoalArrivalDate(
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                weeklyLossRateKg = weeklyLossRateKg
            )

            // Despesas financeiras
            var consultationSum = 0.0
            var medicationSum = 0.0
            var otherSum = 0.0

            expenses.forEach { exp ->
                when (exp.category) {
                    ExpenseCategory.CONSULTATION -> consultationSum += exp.amount
                    ExpenseCategory.MEDICATION -> medicationSum += exp.amount
                    ExpenseCategory.OTHER -> otherSum += exp.amount
                }
            }

            val totalInvested = consultationSum + medicationSum + otherSum
            val costPerKgLost = if (totalLostKg > 0.0) totalInvested / totalLostKg else 0.0

            val financialStats = TreatmentFinancialStats(
                totalConsultationExpenses = consultationSum,
                totalMedicationExpenses = medicationSum,
                totalOtherExpenses = otherSum,
                totalInvested = totalInvested,
                costPerKgLost = costPerKgLost,
                weeklyLossRateKg = weeklyLossRateKg,
                activeWeeks = activeWeeks,
                estimatedArrivalDate = estimatedArrival
            )

            // Variação semanal (barras positivas e negativas)
            val weeklyVariations = calculateWeeklyVariations(allWeightsAsc, initialWeight)

            TreatmentStatsData(
                financialStats = financialStats,
                allWeightRecords = allWeightsAsc,
                weeklyVariations = weeklyVariations,
                initialWeight = initialWeight,
                targetWeight = targetWeight,
                currentWeight = currentWeight
            )
        }
    }

    private fun calculateWeeklyVariations(
        records: List<WeightRecord>,
        initialWeight: Double
    ): List<WeeklyWeightVariation> {
        if (records.isEmpty()) return emptyList()

        // Agrupa por semana ou calcula delta relativo a cada leitura/semana
        val variations = mutableListOf<WeeklyWeightVariation>()
        var previousWeight = initialWeight

        // Se houver registros, agrupamos por blocos de 7 dias ou por registros consecutivos
        records.takeLast(12).forEachIndexed { index, record ->
            val diff = record.weightKg - previousWeight
            val label = "Sem ${index + 1}"
            variations.add(
                WeeklyWeightVariation(
                    weekLabel = label,
                    weightDiffKg = diff,
                    averageWeightKg = record.weightKg,
                    dateMillis = record.dateMillis
                )
            )
            previousWeight = record.weightKg
        }

        return variations
    }
}

data class TreatmentStatsData(
    val financialStats: TreatmentFinancialStats,
    val allWeightRecords: List<WeightRecord>,
    val weeklyVariations: List<WeeklyWeightVariation>,
    val initialWeight: Double,
    val targetWeight: Double,
    val currentWeight: Double
)

class AddMedicationBoxUseCase(
    private val expenseRepository: ExpenseRepository,
    private val medicationRepository: MedicationRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(input: com.bolsoleve.app.domain.model.MedicationBoxInput): Long {
        // 1. Salva a despesa
        val expenseId = expenseRepository.saveExpense(
            ExpenseRecord(
                description = "Caixa ${input.medicationName} ${input.doseMg} mg",
                category = ExpenseCategory.MEDICATION,
                amount = input.boxPrice,
                dateMillis = input.purchaseDateMillis,
                quantity = input.dosesCount
            )
        )

        // 2. Se solicitado, gera as doses semanais correspondentes
        if (input.registerRetroactiveDoses) {
            val oneWeekMillis = 7L * 24 * 60 * 60 * 1000
            for (i in 0 until input.dosesCount) {
                val doseTime = input.purchaseDateMillis + (i * oneWeekMillis)
                // Se a data da dose for até hoje, marca como confirmada
                val isPastOrToday = doseTime <= System.currentTimeMillis()
                medicationRepository.recordDose(
                    MedicationDose(
                        medicationName = input.medicationName,
                        doseMg = input.doseMg,
                        appliedAtMillis = doseTime,
                        isConfirmed = isPastOrToday,
                        weekNumber = i + 1
                    )
                )
            }
        }

        // 3. Atualiza preferência de dose atual
        preferencesRepository.updateMedicationSettings(
            medicationName = input.medicationName,
            currentDoseMg = input.doseMg,
            applicationDayOfWeek = run {
                val cal = java.time.Instant.ofEpochMilli(input.purchaseDateMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .dayOfWeek.value
                cal
            }
        )

        return expenseId
    }
}

class CompleteOnboardingUseCase(
    private val preferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val expenseRepository: ExpenseRepository,
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(
        isStartingFromScratch: Boolean,
        initialWeight: Double,
        currentWeight: Double?,
        targetWeight: Double,
        startDateMillis: Long,
        applicationDayOfWeek: Int,
        medicationName: String,
        currentDoseMg: Double,
        pastMedications: List<com.bolsoleve.app.domain.model.PastMedicationInput> = emptyList()
    ) {
        preferencesRepository.saveOnboardingData(
            initialWeight = initialWeight,
            targetWeight = targetWeight,
            startDateMillis = startDateMillis,
            applicationDayOfWeek = applicationDayOfWeek,
            medicationName = medicationName,
            initialDoseMg = currentDoseMg
        )

        // 1. Salva o peso inicial no histórico
        weightRepository.saveRecord(
            WeightRecord(
                weightKg = initialWeight,
                dateMillis = startDateMillis,
                notes = "Peso inicial de partida"
            )
        )

        // 2. Se já estava em tratamento e tem um peso atual aferido hoje diferente
        if (!isStartingFromScratch && currentWeight != null && currentWeight > 0.0) {
            weightRepository.saveRecord(
                WeightRecord(
                    weightKg = currentWeight,
                    dateMillis = System.currentTimeMillis(),
                    notes = "Peso atual ao ingressar no Bolso+Leve"
                )
            )
        }

        // 3. Se já estava em tratamento, registra as caixas prévias e despesas
        if (!isStartingFromScratch && pastMedications.isNotEmpty()) {
            val oneWeekMillis = 7L * 24 * 60 * 60 * 1000
            pastMedications.forEach { past ->
                // Salva despesa de cada caixa informada
                val totalCost = past.pricePerBox * past.boxesCount
                expenseRepository.saveExpense(
                    ExpenseRecord(
                        description = "Histórico: ${past.medicationName} (${past.boxesCount} cx)",
                        category = ExpenseCategory.MEDICATION,
                        amount = totalCost,
                        dateMillis = past.startDateMillis,
                        quantity = past.boxesCount
                    )
                )

                // Gera doses semanais retroativas para cálculo preciso de semanas e consistência
                for (w in 0 until past.weeksCount) {
                    val doseTime = past.startDateMillis + (w * oneWeekMillis)
                    if (doseTime <= System.currentTimeMillis()) {
                        medicationRepository.recordDose(
                            MedicationDose(
                                medicationName = past.medicationName,
                                doseMg = past.doseMg,
                                appliedAtMillis = doseTime,
                                isConfirmed = true,
                                weekNumber = w + 1
                            )
                        )
                    }
                }
            }
        }
    }
}
