package com.bolsoleve.app.presentation.screens.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.MedicationBoxInput
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.usecase.AddMedicationBoxUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicineUiState(
    val medicationName: String = "Mounjaro",
    val currentDoseMg: Double = 2.5,
    val applicationDayOfWeek: Int = 1,
    val doseHistory: List<MedicationDose> = emptyList(),
    val purchasedBoxes: List<ExpenseRecord> = emptyList(),
    val pastConsultations: List<ExpenseRecord> = emptyList(),
    val doctorProfile: DoctorProfile = DoctorProfile(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

class MedicineViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val medicationRepository: MedicationRepository,
    private val doctorRepository: DoctorRepository,
    private val expenseRepository: ExpenseRepository,
    private val addMedicationBoxUseCase: AddMedicationBoxUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineUiState(isLoading = true))
    val uiState: StateFlow<MedicineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.userPreferencesFlow,
                medicationRepository.getAllDoses(),
                doctorRepository.getDoctorProfile(),
                expenseRepository.getAllExpenses()
            ) { prefs, doses, doctor, expenses ->
                val medBoxes = expenses.filter { it.category == ExpenseCategory.MEDICATION }
                val consultations = expenses.filter { it.category == ExpenseCategory.CONSULTATION }
                MedicineUiState(
                    medicationName = prefs.medicationName,
                    currentDoseMg = prefs.currentDoseMg,
                    applicationDayOfWeek = prefs.applicationDayOfWeek,
                    doseHistory = doses,
                    purchasedBoxes = medBoxes,
                    pastConsultations = consultations,
                    doctorProfile = doctor ?: DoctorProfile(),
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateMedicationSettings(name: String, dose: Double, dayOfWeek: Int) {
        viewModelScope.launch {
            preferencesRepository.updateMedicationSettings(name, dose, dayOfWeek)
            _uiState.update { it.copy(feedbackMessage = "Configurações de medicação atualizadas!") }
        }
    }

    fun addMedicationBox(input: MedicationBoxInput) {
        viewModelScope.launch {
            addMedicationBoxUseCase(input)
            _uiState.update { it.copy(feedbackMessage = "Caixa e histórico de medicação adicionados com sucesso!") }
        }
    }

    fun recordNewDose(medicationName: String, doseMg: Double) {
        viewModelScope.launch {
            medicationRepository.recordDose(
                MedicationDose(
                    medicationName = medicationName,
                    doseMg = doseMg,
                    appliedAtMillis = System.currentTimeMillis(),
                    isConfirmed = true
                )
            )
            _uiState.update { it.copy(feedbackMessage = "Dose de hoje confirmada com sucesso!") }
        }
    }

    fun recordRetroactiveDose(medicationName: String, doseMg: Double, dateMillis: Long) {
        viewModelScope.launch {
            medicationRepository.recordDose(
                MedicationDose(
                    medicationName = medicationName,
                    doseMg = doseMg,
                    appliedAtMillis = dateMillis,
                    isConfirmed = true
                )
            )
            _uiState.update { it.copy(feedbackMessage = "Dose retroativa registrada com sucesso!") }
        }
    }

    fun deleteDose(id: Long) {
        viewModelScope.launch {
            medicationRepository.deleteDose(id)
            _uiState.update { it.copy(feedbackMessage = "Dose removida do histórico.") }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
            _uiState.update { it.copy(feedbackMessage = "Caixa/despesa removida do histórico.") }
        }
    }

    fun addPastConsultation(doctorName: String, fee: Double, dateMillis: Long, notes: String = "") {
        viewModelScope.launch {
            expenseRepository.saveExpense(
                ExpenseRecord(
                    description = "Consulta - ${doctorName.ifBlank { "Médico" }}",
                    category = ExpenseCategory.CONSULTATION,
                    amount = fee,
                    quantity = 1,
                    dateMillis = dateMillis
                )
            )
            if (notes.isNotBlank()) {
                val current = _uiState.value.doctorProfile
                val updatedNotes = if (current.notes.isBlank()) notes else "${current.notes}\n[Consulta ${com.bolsoleve.app.core.utils.DateTimeUtils.formatDate(dateMillis)}]: $notes"
                doctorRepository.saveDoctorProfile(current.copy(notes = updatedNotes))
            }
            _uiState.update { it.copy(feedbackMessage = "Consulta anterior registrada com sucesso!") }
        }
    }

    fun saveDoctorProfile(profile: DoctorProfile) {
        viewModelScope.launch {
            doctorRepository.saveDoctorProfile(profile)
            _uiState.update { it.copy(feedbackMessage = "Dados do médico e consulta salvos!") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
