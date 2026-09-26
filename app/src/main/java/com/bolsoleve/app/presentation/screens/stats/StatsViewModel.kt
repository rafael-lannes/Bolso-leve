package com.bolsoleve.app.presentation.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.ExpenseRecord
import com.bolsoleve.app.domain.model.TreatmentFinancialStats
import com.bolsoleve.app.domain.model.WeeklyWeightVariation
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.usecase.DeleteWeightRecordUseCase
import com.bolsoleve.app.domain.usecase.GetTreatmentStatsUseCase
import com.bolsoleve.app.domain.usecase.UpdateWeightRecordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val financialStats: TreatmentFinancialStats = TreatmentFinancialStats(),
    val allWeightRecords: List<WeightRecord> = emptyList(),
    val weeklyVariations: List<WeeklyWeightVariation> = emptyList(),
    val allExpenses: List<ExpenseRecord> = emptyList(),
    val initialWeight: Double = 0.0,
    val targetWeight: Double = 0.0,
    val currentWeight: Double = 0.0,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

class StatsViewModel(
    private val getTreatmentStatsUseCase: GetTreatmentStatsUseCase,
    private val expenseRepository: ExpenseRepository,
    private val updateWeightRecordUseCase: UpdateWeightRecordUseCase,
    private val deleteWeightRecordUseCase: DeleteWeightRecordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTreatmentStatsUseCase().collect { statsData ->
                _uiState.update {
                    it.copy(
                        financialStats = statsData.financialStats,
                        allWeightRecords = statsData.allWeightRecords,
                        weeklyVariations = statsData.weeklyVariations,
                        initialWeight = statsData.initialWeight,
                        targetWeight = statsData.targetWeight,
                        currentWeight = statsData.currentWeight,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            expenseRepository.getAllExpenses().collect { expenses ->
                _uiState.update { it.copy(allExpenses = expenses) }
            }
        }
    }

    fun addExpense(description: String, category: ExpenseCategory, amount: Double, quantity: Int) {
        viewModelScope.launch {
            expenseRepository.saveExpense(
                ExpenseRecord(
                    description = description,
                    category = category,
                    amount = amount,
                    quantity = quantity,
                    dateMillis = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(feedbackMessage = "Investimento adicionado!") }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
            _uiState.update { it.copy(feedbackMessage = "Registro financeiro removido.") }
        }
    }

    fun updateWeight(id: Long, weightKg: Double, dateMillis: Long, notes: String = "") {
        viewModelScope.launch {
            val result = updateWeightRecordUseCase(id, weightKg, notes, dateMillis)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Pesagem atualizada com sucesso!") }
            } else {
                _uiState.update { it.copy(feedbackMessage = result.exceptionOrNull()?.message ?: "Erro ao atualizar pesagem.") }
            }
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            deleteWeightRecordUseCase(id)
            _uiState.update { it.copy(feedbackMessage = "Pesagem excluída com sucesso.") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
