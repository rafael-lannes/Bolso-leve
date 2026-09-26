package com.bolsoleve.app.domain.repository

import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun getAllRecords(): Flow<List<WeightRecord>>
    fun getAllRecordsAsc(): Flow<List<WeightRecord>>
    fun getLatestRecord(): Flow<WeightRecord?>
    fun getRecentRecords(limit: Int = 8): Flow<List<WeightRecord>>
    suspend fun saveRecord(record: WeightRecord): Long
    suspend fun updateRecord(record: WeightRecord)
    suspend fun deleteRecord(id: Long)
    suspend fun deleteAllRecords()
}

interface MedicationRepository {
    fun getAllDoses(): Flow<List<MedicationDose>>
    fun getLatestDose(): Flow<MedicationDose?>
    suspend fun recordDose(dose: MedicationDose): Long
    suspend fun updateDose(dose: MedicationDose)
    suspend fun deleteDose(id: Long)
    suspend fun deleteAllDoses()
}

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseRecord>>
    suspend fun saveExpense(expense: ExpenseRecord): Long
    suspend fun deleteExpense(id: Long)
    suspend fun deleteAllExpenses()
}

interface DoctorRepository {
    fun getDoctorProfile(): Flow<DoctorProfile?>
    suspend fun saveDoctorProfile(profile: DoctorProfile)
    suspend fun deleteDoctorProfile()
}
