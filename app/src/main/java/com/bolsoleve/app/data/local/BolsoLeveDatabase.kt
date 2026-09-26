package com.bolsoleve.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bolsoleve.app.data.local.dao.DoctorDao
import com.bolsoleve.app.data.local.dao.ExpenseDao
import com.bolsoleve.app.data.local.dao.MedicationDao
import com.bolsoleve.app.data.local.dao.WeightDao
import com.bolsoleve.app.data.local.entity.DoctorProfileEntity
import com.bolsoleve.app.data.local.entity.ExpenseEntity
import com.bolsoleve.app.data.local.entity.MedicationDoseEntity
import com.bolsoleve.app.data.local.entity.WeightRecordEntity

@Database(
    entities = [
        WeightRecordEntity::class,
        MedicationDoseEntity::class,
        ExpenseEntity::class,
        DoctorProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BolsoLeveDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun medicationDao(): MedicationDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun doctorDao(): DoctorDao

    companion object {
        const val DATABASE_NAME = "bolso_leve_db"
    }
}
