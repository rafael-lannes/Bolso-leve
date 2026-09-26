package com.bolsoleve.app.data.backup

import com.bolsoleve.app.data.local.datastore.UserPreferences
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BackupImportResult(
    val weightsCount: Int,
    val dosesCount: Int,
    val expensesCount: Int,
    val preferencesRestored: Boolean,
    val doctorRestored: Boolean
)

class BackupManager(
    private val preferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val medicationRepository: MedicationRepository,
    private val expenseRepository: ExpenseRepository,
    private val doctorRepository: DoctorRepository
) {

    /**
     * Exporta todos os dados da aplicação para uma String JSON estruturada
     */
    suspend fun exportBackupJson(): String {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val weights = weightRepository.getAllRecords().first()
        val doses = medicationRepository.getAllDoses().first()
        val expenses = expenseRepository.getAllExpenses().first()
        val doctor = doctorRepository.getDoctorProfile().first()

        val root = JSONObject()
        root.put("app", "Bolso+Leve")
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())

        // Preferências
        val prefsJson = JSONObject().apply {
            put("isOnboardingCompleted", prefs.isOnboardingCompleted)
            put("initialWeight", prefs.initialWeight)
            put("targetWeight", prefs.targetWeight)
            put("treatmentStartDateMillis", prefs.treatmentStartDateMillis)
            put("applicationDayOfWeek", prefs.applicationDayOfWeek)
            put("medicationName", prefs.medicationName)
            put("initialDoseMg", prefs.initialDoseMg)
            put("currentDoseMg", prefs.currentDoseMg)
            put("reminderEnabled", prefs.reminderEnabled)
            put("reminderHour", prefs.reminderHour)
            put("reminderMinute", prefs.reminderMinute)
        }
        root.put("preferences", prefsJson)

        // Perfil Médico
        doctor?.let { doc ->
            val docJson = JSONObject().apply {
                put("name", doc.name)
                put("crm", doc.crm)
                put("specialty", doc.specialty)
                put("clinicAddress", doc.clinicAddress)
                put("phone", doc.phone)
                put("nextConsultationMillis", doc.nextConsultationMillis ?: JSONObject.NULL)
                put("consultationFee", doc.consultationFee)
                put("notes", doc.notes)
            }
            root.put("doctor", docJson)
        }

        // Pesagens
        val weightsArray = JSONArray()
        weights.forEach { w ->
            weightsArray.put(JSONObject().apply {
                put("weightKg", w.weightKg)
                put("dateMillis", w.dateMillis)
                put("notes", w.notes)
            })
        }
        root.put("weights", weightsArray)

        // Doses
        val dosesArray = JSONArray()
        doses.forEach { d ->
            dosesArray.put(JSONObject().apply {
                put("medicationName", d.medicationName)
                put("doseMg", d.doseMg)
                put("appliedAtMillis", d.appliedAtMillis)
                put("isConfirmed", d.isConfirmed)
                put("weekNumber", d.weekNumber)
            })
        }
        root.put("doses", dosesArray)

        // Despesas Financeiras
        val expensesArray = JSONArray()
        expenses.forEach { exp ->
            expensesArray.put(JSONObject().apply {
                put("description", exp.description)
                put("category", exp.category.name)
                put("amount", exp.amount)
                put("dateMillis", exp.dateMillis)
                put("quantity", exp.quantity)
            })
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    /**
     * Importa e restaura o backup JSON completo no banco de dados e preferências
     */
    suspend fun importBackupJson(jsonString: String, clearExisting: Boolean = false): Result<BackupImportResult> {
        return try {
            val root = JSONObject(jsonString)

            if (clearExisting) {
                weightRepository.deleteAllRecords()
                medicationRepository.deleteAllDoses()
                expenseRepository.deleteAllExpenses()
                doctorRepository.deleteDoctorProfile()
            }

            var prefsRestored = false
            if (root.has("preferences")) {
                val p = root.getJSONObject("preferences")
                val restoredPrefs = UserPreferences(
                    isOnboardingCompleted = p.optBoolean("isOnboardingCompleted", true),
                    initialWeight = p.optDouble("initialWeight", 0.0),
                    targetWeight = p.optDouble("targetWeight", 0.0),
                    treatmentStartDateMillis = p.optLong("treatmentStartDateMillis", System.currentTimeMillis()),
                    applicationDayOfWeek = p.optInt("applicationDayOfWeek", 1),
                    medicationName = p.optString("medicationName", "Mounjaro"),
                    initialDoseMg = p.optDouble("initialDoseMg", 2.5),
                    currentDoseMg = p.optDouble("currentDoseMg", 2.5),
                    reminderEnabled = p.optBoolean("reminderEnabled", true),
                    reminderHour = p.optInt("reminderHour", 9),
                    reminderMinute = p.optInt("reminderMinute", 0)
                )
                preferencesRepository.restoreFullPreferences(restoredPrefs)
                prefsRestored = true
            }

            var docRestored = false
            if (root.has("doctor")) {
                val d = root.getJSONObject("doctor")
                val nextMillis = if (!d.isNull("nextConsultationMillis")) d.optLong("nextConsultationMillis") else null
                val doc = DoctorProfile(
                    name = d.optString("name", ""),
                    crm = d.optString("crm", ""),
                    specialty = d.optString("specialty", ""),
                    clinicAddress = d.optString("clinicAddress", ""),
                    phone = d.optString("phone", ""),
                    nextConsultationMillis = nextMillis,
                    consultationFee = d.optDouble("consultationFee", 0.0),
                    notes = d.optString("notes", "")
                )
                doctorRepository.saveDoctorProfile(doc)
                docRestored = true
            }

            var weightsCount = 0
            if (root.has("weights")) {
                val weightsArray = root.getJSONArray("weights")
                for (i in 0 until weightsArray.length()) {
                    val wObj = weightsArray.getJSONObject(i)
                    weightRepository.saveRecord(
                        WeightRecord(
                            weightKg = wObj.getDouble("weightKg"),
                            dateMillis = wObj.optLong("dateMillis", System.currentTimeMillis()),
                            notes = wObj.optString("notes", "")
                        )
                    )
                    weightsCount++
                }
            }

            var dosesCount = 0
            if (root.has("doses")) {
                val dosesArray = root.getJSONArray("doses")
                for (i in 0 until dosesArray.length()) {
                    val dObj = dosesArray.getJSONObject(i)
                    medicationRepository.recordDose(
                        MedicationDose(
                            medicationName = dObj.optString("medicationName", "Mounjaro"),
                            doseMg = dObj.optDouble("doseMg", 2.5),
                            appliedAtMillis = dObj.optLong("appliedAtMillis", System.currentTimeMillis()),
                            isConfirmed = dObj.optBoolean("isConfirmed", true),
                            weekNumber = dObj.optInt("weekNumber", 1)
                        )
                    )
                    dosesCount++
                }
            }

            var expensesCount = 0
            if (root.has("expenses")) {
                val expArray = root.getJSONArray("expenses")
                for (i in 0 until expArray.length()) {
                    val expObj = expArray.getJSONObject(i)
                    val catName = expObj.optString("category", ExpenseCategory.OTHER.name)
                    val cat = try {
                        ExpenseCategory.valueOf(catName)
                    } catch (e: Exception) {
                        ExpenseCategory.OTHER
                    }
                    expenseRepository.saveExpense(
                        ExpenseRecord(
                            description = expObj.optString("description", "Despesa importada"),
                            category = cat,
                            amount = expObj.optDouble("amount", 0.0),
                            dateMillis = expObj.optLong("dateMillis", System.currentTimeMillis()),
                            quantity = expObj.optInt("quantity", 1)
                        )
                    )
                    expensesCount++
                }
            }

            Result.success(
                BackupImportResult(
                    weightsCount = weightsCount,
                    dosesCount = dosesCount,
                    expensesCount = expensesCount,
                    preferencesRestored = prefsRestored,
                    doctorRestored = docRestored
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Importa pesagens a partir de um arquivo CSV no formato "Data,peso"
     * Exemplo: "26/06/2026,109" ou "26/06/2026,109.5"
     */
    suspend fun importWeightCsv(csvContent: String): Result<Int> {
        return try {
            val lines = csvContent.lines()
            var importedCount = 0

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue

                // Suporta separador por vírgula ou ponto e vírgula
                val delimiter = if (trimmed.contains(';')) ';' else ','
                val parts = trimmed.split(delimiter)
                if (parts.size < 2) continue

                val rawDate = parts[0].trim().replace("\"", "").replace("'", "")
                val rawWeight = parts[1].trim().replace("\"", "").replace("'", "")

                // Ignora cabeçalhos tipo "Data,peso"
                if (rawDate.lowercase().contains("data") || rawWeight.lowercase().contains("peso")) {
                    continue
                }

                val dateMillis = parseDateToMillis(rawDate) ?: continue
                val weight = rawWeight.replace(',', '.').toDoubleOrNull() ?: continue

                if (weight in 20.0..450.0) {
                    weightRepository.saveRecord(
                        WeightRecord(
                            weightKg = weight,
                            dateMillis = dateMillis,
                            notes = "Importado via CSV"
                        )
                    )
                    importedCount++
                }
            }

            if (importedCount > 0) {
                Result.success(importedCount)
            } else {
                Result.failure(IllegalArgumentException("Nenhuma linha válida no formato 'Data,peso' (ex: 26/06/2026,109) foi encontrada."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        val cleanDate = dateStr.trim()
        val patterns = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd/M/yyyy",
            "d/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yy"
        )
        for (pattern in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern)
                val localDate = LocalDate.parse(cleanDate, formatter)
                return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }
        return null
    }
}
