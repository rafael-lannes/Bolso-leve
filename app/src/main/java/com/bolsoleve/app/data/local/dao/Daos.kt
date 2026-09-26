package com.bolsoleve.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bolsoleve.app.data.local.entity.DoctorProfileEntity
import com.bolsoleve.app.data.local.entity.ExpenseEntity
import com.bolsoleve.app.data.local.entity.MedicationDoseEntity
import com.bolsoleve.app.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC")
    fun getAllRecords(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY dateMillis ASC")
    fun getAllRecordsAsc(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC LIMIT 1")
    fun getLatestRecord(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_records ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<WeightRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WeightRecordEntity): Long

    @Update
    suspend fun updateRecord(record: WeightRecordEntity)

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM weight_records")
    suspend fun deleteAllRecords()
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_doses ORDER BY appliedAtMillis DESC")
    fun getAllDoses(): Flow<List<MedicationDoseEntity>>

    @Query("SELECT * FROM medication_doses ORDER BY appliedAtMillis DESC LIMIT 1")
    fun getLatestDose(): Flow<MedicationDoseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDose(dose: MedicationDoseEntity): Long

    @Update
    suspend fun updateDose(dose: MedicationDoseEntity)

    @Query("DELETE FROM medication_doses WHERE id = :id")
    suspend fun deleteDose(id: Long)

    @Query("DELETE FROM medication_doses")
    suspend fun deleteAllDoses()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY dateMillis DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctor_profile WHERE id = 1 LIMIT 1")
    fun getDoctorProfile(): Flow<DoctorProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDoctorProfile(doctor: DoctorProfileEntity)

    @Query("DELETE FROM doctor_profile")
    suspend fun deleteDoctorProfile()
}
