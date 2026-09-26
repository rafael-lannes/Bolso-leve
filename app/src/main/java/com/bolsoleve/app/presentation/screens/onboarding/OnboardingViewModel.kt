package com.bolsoleve.app.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolsoleve.app.core.utils.WeightUtils
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.domain.model.PastMedicationInput
import com.bolsoleve.app.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isStartingFromScratch: Boolean = true,
    val initialWeightText: String = "",
    val currentWeightText: String = "",
    val targetWeightText: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val selectedDayOfWeek: Int = 1, // 1 = Segunda
    val medicationName: String = "Mounjaro",
    val initialDoseMg: Double = 2.5,
    val pastMedications: List<PastMedicationInput> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onTreatmentTypeChanged(isFromScratch: Boolean) {
        _uiState.update { it.copy(isStartingFromScratch = isFromScratch, errorMessage = null) }
    }

    fun onInitialWeightChanged(text: String) {
        val cleaned = WeightUtils.cleanWeightInput(text)
        _uiState.update { it.copy(initialWeightText = cleaned, errorMessage = null) }
    }

    fun onCurrentWeightChanged(text: String) {
        val cleaned = WeightUtils.cleanWeightInput(text)
        _uiState.update { it.copy(currentWeightText = cleaned, errorMessage = null) }
    }

    fun onTargetWeightChanged(text: String) {
        val cleaned = WeightUtils.cleanWeightInput(text)
        _uiState.update { it.copy(targetWeightText = cleaned, errorMessage = null) }
    }

    fun onStartDateChanged(millis: Long) {
        _uiState.update { it.copy(startDateMillis = millis) }
    }

    fun onDayOfWeekChanged(day: Int) {
        _uiState.update { it.copy(selectedDayOfWeek = day) }
    }

    fun onMedicationSelected(name: String, defaultDose: Double) {
        _uiState.update { it.copy(medicationName = name, initialDoseMg = defaultDose) }
    }

    fun onInitialDoseChanged(dose: Double) {
        _uiState.update { it.copy(initialDoseMg = dose) }
    }

    fun addPastMedication(
        name: String,
        doseMg: Double,
        boxesCount: Int,
        weeksCount: Int,
        pricePerBox: Double,
        startDateMillis: Long
    ) {
        val newEntry = PastMedicationInput(
            medicationName = name,
            doseMg = doseMg,
            boxesCount = boxesCount,
            weeksCount = weeksCount,
            pricePerBox = pricePerBox,
            startDateMillis = startDateMillis
        )
        _uiState.update {
            it.copy(pastMedications = it.pastMedications + newEntry)
        }
    }

    fun removePastMedication(id: String) {
        _uiState.update {
            it.copy(pastMedications = it.pastMedications.filterNot { item -> item.id == id })
        }
    }

    fun submitOnboarding(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val initialWeight = WeightUtils.parseWeight(currentState.initialWeightText)
        val targetWeight = WeightUtils.parseWeight(currentState.targetWeightText)
        val currentWeight = if (!currentState.isStartingFromScratch) {
            WeightUtils.parseWeight(currentState.currentWeightText)
        } else null

        if (initialWeight == null) {
            _uiState.update { it.copy(errorMessage = "Por favor informe um peso inicial de partida válido (ex: 95.0)") }
            return
        }

        if (!currentState.isStartingFromScratch && currentWeight == null) {
            _uiState.update { it.copy(errorMessage = "Por favor informe seu peso atual hoje (ex: 88.0)") }
            return
        }

        if (targetWeight == null) {
            _uiState.update { it.copy(errorMessage = "Por favor informe sua meta de peso válida (ex: 75.0)") }
            return
        }

        if (targetWeight >= initialWeight) {
            _uiState.update { it.copy(errorMessage = "O peso meta deve ser menor que o peso inicial.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                completeOnboardingUseCase(
                    isStartingFromScratch = currentState.isStartingFromScratch,
                    initialWeight = initialWeight,
                    currentWeight = currentWeight,
                    targetWeight = targetWeight,
                    startDateMillis = currentState.startDateMillis,
                    applicationDayOfWeek = currentState.selectedDayOfWeek,
                    medicationName = currentState.medicationName,
                    currentDoseMg = currentState.initialDoseMg,
                    pastMedications = currentState.pastMedications
                )
                _uiState.update { it.copy(isSaving = false, isCompleted = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Erro ao salvar: ${e.localizedMessage}") }
            }
        }
    }
}
