package com.bolsoleve.app.presentation.screens.options

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolsoleve.app.data.backup.BackupManager
import com.bolsoleve.app.data.local.datastore.UserPreferences
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.repository.WeightRepository
import com.bolsoleve.app.notification.BolsoLeveNotificationHelper
import com.bolsoleve.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OptionsUiState(
    val isProcessing: Boolean = false,
    val feedbackMessage: String? = null,
    val lastExportedJson: String? = null
)

class OptionsViewModel(
    private val context: Context,
    private val backupManager: BackupManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val weightRepository: WeightRepository,
    private val medicationRepository: MedicationRepository,
    private val expenseRepository: ExpenseRepository,
    private val doctorRepository: DoctorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState: StateFlow<OptionsUiState> = _uiState.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.updateReminderSettings(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleWeeklyReminder(context)
                ReminderScheduler.checkAndTriggerPendingNotification(context)
                _uiState.update { it.copy(feedbackMessage = "Lembrete semanal salvo com sucesso!") }
            } else {
                ReminderScheduler.cancelReminder(context)
                _uiState.update { it.copy(feedbackMessage = "Lembretes desativados.") }
            }
        }
    }

    fun testMedicationNotification() {
        viewModelScope.launch {
            val prefs = userPreferences.value
            BolsoLeveNotificationHelper.showMedicationReminder(
                context = context,
                medicationName = prefs.medicationName,
                doseMg = prefs.currentDoseMg
            )
            _uiState.update { it.copy(feedbackMessage = "Notificação de teste disparada!") }
        }
    }

    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val json = backupManager.exportBackupJson()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastExportedJson = json,
                        feedbackMessage = "Backup gerado com sucesso!"
                    )
                }
                onExportReady(json)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Erro ao exportar backup: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun importBackup(jsonString: String, clearExisting: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = backupManager.importBackupJson(jsonString, clearExisting)
            if (result.isSuccess) {
                val stats = result.getOrNull()!!
                ReminderScheduler.scheduleWeeklyReminder(context)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Backup restaurado! (${stats.weightsCount} pesagens, ${stats.dosesCount} doses, ${stats.expensesCount} despesas)"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Falha ao restaurar backup: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    fun importWeightCsv(csvContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = backupManager.importWeightCsv(csvContent)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "$count pesagem(ns) importada(s) do CSV com sucesso!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Erro ao importar CSV: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                weightRepository.deleteAllRecords()
                medicationRepository.deleteAllDoses()
                expenseRepository.deleteAllExpenses()
                doctorRepository.deleteDoctorProfile()
                preferencesRepository.resetAllPreferences()
                ReminderScheduler.cancelReminder(context)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Todos os dados foram resetados para o padrão inicial."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "Erro ao resetar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
