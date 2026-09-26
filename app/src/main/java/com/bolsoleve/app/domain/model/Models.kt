package com.bolsoleve.app.domain.model

data class WeightRecord(
    val id: Long = 0,
    val weightKg: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class MedicationDose(
    val id: Long = 0,
    val medicationName: String,
    val doseMg: Double,
    val appliedAtMillis: Long = System.currentTimeMillis(),
    val isConfirmed: Boolean = true,
    val weekNumber: Int = 1
)

data class PastMedicationInput(
    val id: String = java.util.UUID.randomUUID().toString(),
    val medicationName: String,
    val doseMg: Double,
    val boxesCount: Int = 1,
    val weeksCount: Int = 4,
    val pricePerBox: Double = 0.0,
    val startDateMillis: Long = System.currentTimeMillis()
)

data class MedicationBoxInput(
    val medicationName: String,
    val doseMg: Double,
    val boxPrice: Double,
    val dosesCount: Int = 4,
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val registerRetroactiveDoses: Boolean = true
)

enum class ExpenseCategory {
    MEDICATION,
    CONSULTATION,
    OTHER
}

data class ExpenseRecord(
    val id: Long = 0,
    val description: String,
    val category: ExpenseCategory,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val quantity: Int = 1
)

data class DoctorProfile(
    val id: Long = 1,
    val name: String = "",
    val crm: String = "",
    val specialty: String = "",
    val clinicAddress: String = "",
    val phone: String = "",
    val nextConsultationMillis: Long? = null,
    val consultationFee: Double = 0.0,
    val notes: String = ""
)

data class TreatmentSummary(
    val currentWeight: Double = 0.0,
    val initialWeight: Double = 0.0,
    val targetWeight: Double = 0.0,
    val totalLostKg: Double = 0.0,
    val progressPercentage: Float = 0f,
    val daysUntilNextDose: Int = 0,
    val nextDoseDate: String = "",
    val medicationName: String = "",
    val currentDoseMg: Double = 0.0,
    val isDoseToday: Boolean = false,
    val isTodayDoseConfirmed: Boolean = false,
    val nextConsultationDateMillis: Long? = null,
    val doctorName: String = "",
    val doctorSpecialty: String = "",
    val clinicAddress: String = "",
    val suggestedWeighInDay: String = "",
    val activeWeeks: Int = 1
)

data class WeeklyWeightVariation(
    val weekLabel: String,
    val weightDiffKg: Double,
    val averageWeightKg: Double,
    val dateMillis: Long
)

data class TreatmentFinancialStats(
    val totalConsultationExpenses: Double = 0.0,
    val totalMedicationExpenses: Double = 0.0,
    val totalOtherExpenses: Double = 0.0,
    val totalInvested: Double = 0.0,
    val costPerKgLost: Double = 0.0,
    val weeklyLossRateKg: Double = 0.0,
    val activeWeeks: Int = 0,
    val estimatedArrivalDate: String = ""
)
