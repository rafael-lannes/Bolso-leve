package com.bolsoleve.app.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.TreatmentSummary
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.domain.usecase.ConfirmDoseApplicationUseCase
import com.bolsoleve.app.domain.usecase.DeleteWeightRecordUseCase
import com.bolsoleve.app.domain.usecase.GetAllWeightRecordsUseCase
import com.bolsoleve.app.domain.usecase.GetDashboardDataUseCase
import com.bolsoleve.app.domain.usecase.SaveWeightRecordUseCase
import com.bolsoleve.app.domain.usecase.UpdateWeightRecordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val summary: TreatmentSummary = TreatmentSummary(),
    val recentWeights: List<WeightRecord> = emptyList(),
    val allWeights: List<WeightRecord> = emptyList(),
    val doctorProfile: DoctorProfile? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class DashboardViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val saveWeightRecordUseCase: SaveWeightRecordUseCase,
    private val updateWeightRecordUseCase: UpdateWeightRecordUseCase,
    private val deleteWeightRecordUseCase: DeleteWeightRecordUseCase,
    private val getAllWeightRecordsUseCase: GetAllWeightRecordsUseCase,
    private val confirmDoseApplicationUseCase: ConfirmDoseApplicationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDashboardDataUseCase().collect { data ->
                _uiState.update {
                    it.copy(
                        summary = data.summary,
                        recentWeights = data.recentWeights,
                        doctorProfile = data.doctorProfile,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            getAllWeightRecordsUseCase().collect { weights ->
                _uiState.update {
                    it.copy(allWeights = weights)
                }
            }
        }
    }

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            val result = saveWeightRecordUseCase(weightKg)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Peso registrado com sucesso!") }
            } else {
                _uiState.update { it.copy(userMessage = result.exceptionOrNull()?.message ?: "Erro ao registrar peso") }
            }
        }
    }

    fun savePastWeight(weightKg: Double, dateMillis: Long, notes: String = "") {
        viewModelScope.launch {
            val result = saveWeightRecordUseCase(weightKg, notes, dateMillis)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Pesagem anterior registrada com sucesso!") }
            } else {
                _uiState.update { it.copy(userMessage = result.exceptionOrNull()?.message ?: "Erro ao registrar peso") }
            }
        }
    }

    fun updateWeight(id: Long, weightKg: Double, dateMillis: Long, notes: String = "") {
        viewModelScope.launch {
            val result = updateWeightRecordUseCase(id, weightKg, notes, dateMillis)
            if (result.isSuccess) {
                _uiState.update { it.copy(userMessage = "Pesagem atualizada com sucesso!") }
            } else {
                _uiState.update { it.copy(userMessage = result.exceptionOrNull()?.message ?: "Erro ao atualizar pesagem") }
            }
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            deleteWeightRecordUseCase(id)
            _uiState.update { it.copy(userMessage = "Pesagem excluída com sucesso.") }
        }
    }

    fun confirmTodayApplication() {
        val summary = uiState.value.summary
        viewModelScope.launch {
            confirmDoseApplicationUseCase(
                medicationName = summary.medicationName,
                doseMg = summary.currentDoseMg
            )
            _uiState.update { it.copy(userMessage = "Dose semanal de hoje confirmada!") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
