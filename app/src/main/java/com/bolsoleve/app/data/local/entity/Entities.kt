package com.bolsoleve.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.WeightRecord

@Entity(tableName = "weight_records")
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Double,
    val dateMillis: Long,
    val notes: String = ""
) {
    fun toDomain() = WeightRecord(
        id = id,
        weightKg = weightKg,
        dateMillis = dateMillis,
        notes = notes
    )

    companion object {
        fun fromDomain(domain: WeightRecord) = WeightRecordEntity(
            id = domain.id,
            weightKg = domain.weightKg,
            dateMillis = domain.dateMillis,
            notes = domain.notes
        )
    }
}

@Entity(tableName = "medication_doses")
data class MedicationDoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationName: String,
    val doseMg: Double,
    val appliedAtMillis: Long,
    val isConfirmed: Boolean,
    val weekNumber: Int
) {
    fun toDomain() = MedicationDose(
        id = id,
        medicationName = medicationName,
        doseMg = doseMg,
        appliedAtMillis = appliedAtMillis,
        isConfirmed = isConfirmed,
        weekNumber = weekNumber
    )

    companion object {
        fun fromDomain(domain: MedicationDose) = MedicationDoseEntity(
            id = domain.id,
            medicationName = domain.medicationName,
            doseMg = domain.doseMg,
            appliedAtMillis = domain.appliedAtMillis,
            isConfirmed = domain.isConfirmed,
            weekNumber = domain.weekNumber
        )
    }
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val category: String, // CONSULTATION, MEDICATION, OTHER
    val amount: Double,
    val dateMillis: Long,
    val quantity: Int = 1
) {
    fun toDomain() = ExpenseRecord(
        id = id,
        description = description,
        category = try {
            ExpenseCategory.valueOf(category)
        } catch (e: Exception) {
            ExpenseCategory.OTHER
        },
        amount = amount,
        dateMillis = dateMillis,
        quantity = quantity
    )

    companion object {
        fun fromDomain(domain: ExpenseRecord) = ExpenseEntity(
            id = domain.id,
            description = domain.description,
            category = domain.category.name,
            amount = domain.amount,
            dateMillis = domain.dateMillis,
            quantity = domain.quantity
        )
    }
}

@Entity(tableName = "doctor_profile")
data class DoctorProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val name: String,
    val crm: String,
    val specialty: String,
    val clinicAddress: String,
    val phone: String,
    val nextConsultationMillis: Long?,
    val consultationFee: Double,
    val notes: String
)
