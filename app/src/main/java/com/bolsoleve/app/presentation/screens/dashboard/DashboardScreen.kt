package com.bolsoleve.app.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bolsoleve.app.core.utils.IntentUtils
import com.bolsoleve.app.domain.model.WeightRecord
import com.bolsoleve.app.notification.BolsoLeveNotificationHelper
import com.bolsoleve.app.presentation.components.AboutDialog
import com.bolsoleve.app.presentation.components.AddPastWeightDialog
import com.bolsoleve.app.presentation.components.ConfirmDeleteWeightDialog
import com.bolsoleve.app.presentation.components.EditWeightEntryDialog
import com.bolsoleve.app.presentation.components.ManageWeightEntriesBottomSheet
import com.bolsoleve.app.presentation.components.MiniWeightChart
import com.bolsoleve.app.presentation.components.NextApplicationCard
import com.bolsoleve.app.presentation.components.NextConsultationCard
import com.bolsoleve.app.presentation.components.NextWeighInCard
import com.bolsoleve.app.presentation.components.QuickWeightEntryCard
import com.bolsoleve.app.presentation.components.WeighInGuideExpandableCard
import com.bolsoleve.app.presentation.components.WeightSummaryCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddPastWeightDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showManageWeightEntries by remember { mutableStateOf(false) }
    var weightRecordToEdit by remember { mutableStateOf<WeightRecord?>(null) }
    var weightRecordToDelete by remember { mutableStateOf<WeightRecord?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bolso+Leve",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Semana ${uiState.summary.activeWeeks} de tratamento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Botão Sobre o App / Criador
                    IconButton(
                        onClick = { showAboutDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Sobre o App",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Botão Testar Lembrete
                    IconButton(
                        onClick = {
                            BolsoLeveNotificationHelper.showMedicationReminder(
                                context,
                                uiState.summary.medicationName,
                                uiState.summary.currentDoseMg
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Testar Notificação",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Resumo de Peso
                item {
                    WeightSummaryCard(summary = uiState.summary)
                }

                // 2. Próxima Aplicação (com contagem regressiva e confirmação imediata)
                item {
                    NextApplicationCard(
                        summary = uiState.summary,
                        onConfirmTodayApplication = {
                            viewModel.confirmTodayApplication()
                        }
                    )
                }

                // 3. Próxima Consulta com botão para Adicionar à Agenda
                item {
                    NextConsultationCard(
                        summary = uiState.summary,
                        onAddToCalendar = {
                            uiState.summary.nextConsultationDateMillis?.let { dateMillis ->
                                IntentUtils.addToCalendar(
                                    context = context,
                                    doctorName = uiState.summary.doctorName,
                                    specialty = uiState.summary.doctorSpecialty,
                                    location = uiState.summary.clinicAddress,
                                    startTimeMillis = dateMillis
                                )
                            }
                        }
                    )
                }

                // 4. Próxima Pesagem
                item {
                    NextWeighInCard(suggestedDay = uiState.summary.suggestedWeighInDay)
                }

                // 5. Registro de Peso (Com opção de Pesagem Anterior com Data)
                item {
                    QuickWeightEntryCard(
                        onSaveWeight = { weight ->
                            viewModel.saveWeight(weight)
                        },
                        onOpenPastWeightDialog = {
                            showAddPastWeightDialog = true
                        }
                    )
                }

                // 6. Guia Rápido de Pesagem Expansível
                item {
                    WeighInGuideExpandableCard()
                }

                // 7. Mini-gráfico recente com atalho para gerenciar/editar entradas
                item {
                    MiniWeightChart(
                        records = uiState.recentWeights,
                        onManageEntries = { showManageWeightEntries = true }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modal de Inserção de Pesagem Anterior
    if (showAddPastWeightDialog) {
        AddPastWeightDialog(
            onDismiss = { showAddPastWeightDialog = false },
            onConfirm = { weightKg, dateMillis, notes ->
                viewModel.savePastWeight(weightKg, dateMillis, notes)
            }
        )
    }

    // Modal "Sobre o Bolso+Leve" com créditos a Rafael Lannes
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Modal para listar todas as pesagens da curva e gerenciar (editar / excluir)
    if (showManageWeightEntries) {
        ManageWeightEntriesBottomSheet(
            records = uiState.allWeights,
            onDismiss = { showManageWeightEntries = false },
            onEditRecord = { record ->
                weightRecordToEdit = record
            },
            onDeleteRecord = { record ->
                weightRecordToDelete = record
            }
        )
    }

    // Diálogo para edição de registro existente
    weightRecordToEdit?.let { record ->
        EditWeightEntryDialog(
            record = record,
            onDismiss = { weightRecordToEdit = null },
            onConfirm = { id, weightKg, dateMillis, notes ->
                viewModel.updateWeight(id, weightKg, dateMillis, notes)
                weightRecordToEdit = null
            }
        )
    }

    // Diálogo para confirmação de exclusão
    weightRecordToDelete?.let { record ->
        ConfirmDeleteWeightDialog(
            record = record,
            onDismiss = { weightRecordToDelete = null },
            onConfirm = { id ->
                viewModel.deleteWeight(id)
                weightRecordToDelete = null
            }
        )
    }
}
