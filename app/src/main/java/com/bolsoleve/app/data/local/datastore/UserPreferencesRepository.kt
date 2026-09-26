package com.bolsoleve.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bolso_leve_preferences")

data class UserPreferences(
    val isOnboardingCompleted: Boolean = false,
    val initialWeight: Double = 0.0,
    val targetWeight: Double = 0.0,
    val treatmentStartDateMillis: Long = System.currentTimeMillis(),
    val applicationDayOfWeek: Int = 1, // 1 = Segunda, 7 = Domingo
    val medicationName: String = "Mounjaro",
    val initialDoseMg: Double = 2.5,
    val currentDoseMg: Double = 2.5,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val INITIAL_WEIGHT = doublePreferencesKey("initial_weight")
        val TARGET_WEIGHT = doublePreferencesKey("target_weight")
        val START_DATE_MILLIS = longPreferencesKey("start_date_millis")
        val APPLICATION_DAY_OF_WEEK = intPreferencesKey("application_day_of_week")
        val MEDICATION_NAME = stringPreferencesKey("medication_name")
        val INITIAL_DOSE_MG = doublePreferencesKey("initial_dose_mg")
        val CURRENT_DOSE_MG = doublePreferencesKey("current_dose_mg")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
        UserPreferences(
            isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false,
            initialWeight = preferences[PreferencesKeys.INITIAL_WEIGHT] ?: 0.0,
            targetWeight = preferences[PreferencesKeys.TARGET_WEIGHT] ?: 0.0,
            treatmentStartDateMillis = preferences[PreferencesKeys.START_DATE_MILLIS] ?: System.currentTimeMillis(),
            applicationDayOfWeek = preferences[PreferencesKeys.APPLICATION_DAY_OF_WEEK] ?: 1,
            medicationName = preferences[PreferencesKeys.MEDICATION_NAME] ?: "Mounjaro",
            initialDoseMg = preferences[PreferencesKeys.INITIAL_DOSE_MG] ?: 2.5,
            currentDoseMg = preferences[PreferencesKeys.CURRENT_DOSE_MG] ?: 2.5,
            reminderEnabled = preferences[PreferencesKeys.REMINDER_ENABLED] ?: true,
            reminderHour = preferences[PreferencesKeys.REMINDER_HOUR] ?: 9,
            reminderMinute = preferences[PreferencesKeys.REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun saveOnboardingData(
        initialWeight: Double,
        targetWeight: Double,
        startDateMillis: Long,
        applicationDayOfWeek: Int,
        medicationName: String,
        initialDoseMg: Double
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INITIAL_WEIGHT] = initialWeight
            preferences[PreferencesKeys.TARGET_WEIGHT] = targetWeight
            preferences[PreferencesKeys.START_DATE_MILLIS] = startDateMillis
            preferences[PreferencesKeys.APPLICATION_DAY_OF_WEEK] = applicationDayOfWeek
            preferences[PreferencesKeys.MEDICATION_NAME] = medicationName
            preferences[PreferencesKeys.INITIAL_DOSE_MG] = initialDoseMg
            preferences[PreferencesKeys.CURRENT_DOSE_MG] = initialDoseMg
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateMedicationSettings(
        medicationName: String,
        currentDoseMg: Double,
        applicationDayOfWeek: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEDICATION_NAME] = medicationName
            preferences[PreferencesKeys.CURRENT_DOSE_MG] = currentDoseMg
            preferences[PreferencesKeys.APPLICATION_DAY_OF_WEEK] = applicationDayOfWeek
        }
    }

    suspend fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.REMINDER_HOUR] = hour
            preferences[PreferencesKeys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun restoreFullPreferences(prefs: UserPreferences) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = prefs.isOnboardingCompleted
            preferences[PreferencesKeys.INITIAL_WEIGHT] = prefs.initialWeight
            preferences[PreferencesKeys.TARGET_WEIGHT] = prefs.targetWeight
            preferences[PreferencesKeys.START_DATE_MILLIS] = prefs.treatmentStartDateMillis
            preferences[PreferencesKeys.APPLICATION_DAY_OF_WEEK] = prefs.applicationDayOfWeek
            preferences[PreferencesKeys.MEDICATION_NAME] = prefs.medicationName
            preferences[PreferencesKeys.INITIAL_DOSE_MG] = prefs.initialDoseMg
            preferences[PreferencesKeys.CURRENT_DOSE_MG] = prefs.currentDoseMg
            preferences[PreferencesKeys.REMINDER_ENABLED] = prefs.reminderEnabled
            preferences[PreferencesKeys.REMINDER_HOUR] = prefs.reminderHour
            preferences[PreferencesKeys.REMINDER_MINUTE] = prefs.reminderMinute
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
