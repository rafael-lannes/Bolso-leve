package com.bolsoleve.app.data.repository

import com.bolsoleve.app.data.local.dao.DoctorDao
import com.bolsoleve.app.data.local.dao.ExpenseDao
import com.bolsoleve.app.data.local.dao.MedicationDao
import com.bolsoleve.app.data.local.dao.WeightDao
import com.bolsoleve.app.data.local.entity.DoctorProfileEntity
import com.bolsoleve.app.data.local.entity.ExpenseEntity
import com.bolsoleve.app.data.local.entity.MedicationDoseEntity
import com.bolsoleve.app.data.local.entity.WeightRecordEntity
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepositoryImpl(
    private val dao: WeightDao
) : WeightRepository {
    override fun getAllRecords(): Flow<List<WeightRecord>> =
        dao.getAllRecords().map { list -> list.map { it.toDomain() } }

    override fun getAllRecordsAsc(): Flow<List<WeightRecord>> =
        dao.getAllRecordsAsc().map { list -> list.map { it.toDomain() } }

    override fun getLatestRecord(): Flow<WeightRecord?> =
        dao.getLatestRecord().map { it?.toDomain() }

    override fun getRecentRecords(limit: Int): Flow<List<WeightRecord>> =
        dao.getRecentRecords(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun saveRecord(record: WeightRecord): Long =
        dao.insertRecord(WeightRecordEntity.fromDomain(record))

    override suspend fun updateRecord(record: WeightRecord) =
        dao.updateRecord(WeightRecordEntity.fromDomain(record))

    override suspend fun deleteRecord(id: Long) =
        dao.deleteRecord(id)

    override suspend fun deleteAllRecords() =
        dao.deleteAllRecords()
}

class MedicationRepositoryImpl(
    private val dao: MedicationDao
) : MedicationRepository {
    override fun getAllDoses(): Flow<List<MedicationDose>> =
        dao.getAllDoses().map { list -> list.map { it.toDomain() } }

    override fun getLatestDose(): Flow<MedicationDose?> =
        dao.getLatestDose().map { it?.toDomain() }

    override suspend fun recordDose(dose: MedicationDose): Long =
        dao.insertDose(MedicationDoseEntity.fromDomain(dose))

    override suspend fun updateDose(dose: MedicationDose) =
        dao.updateDose(MedicationDoseEntity.fromDomain(dose))

    override suspend fun deleteDose(id: Long) =
        dao.deleteDose(id)

    override suspend fun deleteAllDoses() =
        dao.deleteAllDoses()
}

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao
) : ExpenseRepository {
    override fun getAllExpenses(): Flow<List<ExpenseRecord>> =
        dao.getAllExpenses().map { list -> list.map { it.toDomain() } }

    override suspend fun saveExpense(expense: ExpenseRecord): Long =
        dao.insertExpense(ExpenseEntity.fromDomain(expense))

    override suspend fun deleteExpense(id: Long) =
        dao.deleteExpense(id)

    override suspend fun deleteAllExpenses() =
        dao.deleteAllExpenses()
}

class DoctorRepositoryImpl(
    private val dao: DoctorDao
) : DoctorRepository {
    override fun getDoctorProfile(): Flow<DoctorProfile?> =
        dao.getDoctorProfile().map { entity ->
            entity?.let {
                DoctorProfile(
                    id = it.id,
                    name = it.name,
                    crm = it.crm,
                    specialty = it.specialty,
                    clinicAddress = it.clinicAddress,
                    phone = it.phone,
                    nextConsultationMillis = it.nextConsultationMillis,
                    consultationFee = it.consultationFee,
                    notes = it.notes
                )
            }
        }

    override suspend fun saveDoctorProfile(profile: DoctorProfile) {
        dao.saveDoctorProfile(
            DoctorProfileEntity(
                id = 1,
                name = profile.name,
                crm = profile.crm,
                specialty = profile.specialty,
                clinicAddress = profile.clinicAddress,
                phone = profile.phone,
                nextConsultationMillis = profile.nextConsultationMillis,
                consultationFee = profile.consultationFee,
                notes = profile.notes
            )
        )
    }

    override suspend fun deleteDoctorProfile() {
        dao.deleteDoctorProfile()
    }
}
