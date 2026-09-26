package com.bolsoleve.app.presentation.screens.medicine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bolsoleve.app.core.theme.SuccessGreen
import com.bolsoleve.app.core.utils.CurrencyUtils
import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.core.utils.IntentUtils
import com.bolsoleve.app.domain.model.DoctorProfile
import com.bolsoleve.app.presentation.components.AddPastConsultationDialog
import com.bolsoleve.app.presentation.components.CurrencyInputField
import com.bolsoleve.app.presentation.components.Material3DatePickerDialog
import com.bolsoleve.app.presentation.components.Material3TimePickerDialog
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    viewModel: MedicineViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    // Estados locais para edição dos dados médicos
    var doctorName by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.name) }
    var crm by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.crm) }
    var specialty by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.specialty) }
    var clinicAddress by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.clinicAddress) }
    var phone by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.phone) }
    var consultationFeeText by remember(uiState.doctorProfile) {
        mutableStateOf(if (uiState.doctorProfile.consultationFee > 0) CurrencyUtils.formatForEditing(uiState.doctorProfile.consultationFee) else "")
    }
    var doctorNotes by remember(uiState.doctorProfile) { mutableStateOf(uiState.doctorProfile.notes) }
    var consultationDateMillis by remember(uiState.doctorProfile) {
        mutableStateOf(uiState.doctorProfile.nextConsultationMillis)
    }

    // Modais e colapso
    var showConsultationDatePicker by remember { mutableStateOf(false) }
    var showConsultationTimePicker by remember { mutableStateOf(false) }
    var showAddBoxDialog by remember { mutableStateOf(false) }
    var showAddSingleDoseDialog by remember { mutableStateOf(false) }
    var showAddPastConsultationDialog by remember { mutableStateOf(false) }
    var isDoseHistoryExpanded by remember { mutableStateOf(false) }

    val daysOfWeek = listOf(
        Pair(1, "Seg"),
        Pair(2, "Ter"),
        Pair(3, "Qua"),
        Pair(4, "Qui"),
        Pair(5, "Sex"),
        Pair(6, "Sáb"),
        Pair(7, "Dom")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Medicina & Consultas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ==================== SESSÃO 1: MINHA MEDICAÇÃO ====================
                item {
                    Text(
                        text = "Minha Medicação",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Medication,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = uiState.medicationName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Dosagem: ${uiState.currentDoseMg} mg",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Ações de Registro de Caixa e Dose
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddBoxDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nova Caixa (R$)", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { showAddSingleDoseDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dose Retroativa", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            // Botão de confirmar dose de hoje
                            Button(
                                onClick = {
                                    viewModel.recordNewDose(
                                        uiState.medicationName,
                                        uiState.currentDoseMg
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirmar Aplicação de Hoje", fontWeight = FontWeight.Bold)
                            }

                            // Seletor de dia da semana de aplicação
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Dia de aplicação semanal configurado:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    daysOfWeek.forEach { (dayIndex, dayLabel) ->
                                        val isSelected = uiState.applicationDayOfWeek == dayIndex
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    viewModel.updateMedicationSettings(
                                                        uiState.medicationName,
                                                        uiState.currentDoseMg,
                                                        dayIndex
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== CAIXAS COMPRADAS ====================
                item {
                    Text(
                        text = "Caixas Compradas & Custo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.purchasedBoxes.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "Nenhuma caixa registrada ainda. Toque em 'Nova Caixa' para adicionar compras e preços.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(uiState.purchasedBoxes) { box ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = box.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Comprada em: ${DateTimeUtils.formatDate(box.dateMillis)} • ${box.quantity} doses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyUtils.formatCurrency(box.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    IconButton(
                                        onClick = { viewModel.deleteExpense(box.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover caixa",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Histórico de Doses Tomadas (Colapsável)
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDoseHistoryExpanded = !isDoseHistoryExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Histórico de Doses Tomadas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${uiState.doseHistory.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { isDoseHistoryExpanded = !isDoseHistoryExpanded }) {
                                    Icon(
                                        imageVector = if (isDoseHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isDoseHistoryExpanded) "Recolher" else "Expandir"
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isDoseHistoryExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))

                                    if (uiState.doseHistory.isEmpty()) {
                                        Text(
                                            text = "Nenhuma aplicação registrada ainda.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        for (dose in uiState.doseHistory) {
                                            OutlinedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = SuccessGreen
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "${dose.medicationName} (${dose.doseMg} mg)",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = DateTimeUtils.formatDateTime(dose.appliedAtMillis),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteDose(dose.id) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Remover dose",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== SESSÃO 2: MÉDICO & CLÍNICA ====================
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Médico & Clínica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = { showAddPastConsultationDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Consulta Retroativa", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Nome do médico
                            OutlinedTextField(
                                value = doctorName,
                                onValueChange = { doctorName = it },
                                label = { Text("Nome do Médico(a)") },
                                placeholder = { Text("Ex: Dra. Ana Paula Costa") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.LocalHospital, contentDescription = null)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // CRM e Especialidade
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = crm,
                                    onValueChange = { crm = it },
                                    label = { Text("CRM") },
                                    placeholder = { Text("12345/SP") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.45f)
                                )

                                OutlinedTextField(
                                    value = specialty,
                                    onValueChange = { specialty = it },
                                    label = { Text("Especialidade") },
                                    placeholder = { Text("Endocrinologista") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.55f)
                                )
                            }

                            // Endereço com botão Google Maps
                            OutlinedTextField(
                                value = clinicAddress,
                                onValueChange = { clinicAddress = it },
                                label = { Text("Endereço do Consultório") },
                                placeholder = { Text("Av. Paulista, 1000 - São Paulo") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (clinicAddress.isNotBlank()) {
                                        IconButton(onClick = { IntentUtils.openMaps(context, clinicAddress) }) {
                                            Icon(
                                                imageVector = Icons.Default.Directions,
                                                contentDescription = "Abrir no Maps",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Telefone com botão Discar
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Telefone / WhatsApp") },
                                placeholder = { Text("(11) 98765-4321") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (phone.isNotBlank()) {
                                        IconButton(onClick = { IntentUtils.openDialer(context, phone) }) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Ligar",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Seletor de Data e Hora da Próxima Consulta
                            Text(
                                text = "Próxima Consulta Agendada:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showConsultationDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = consultationDateMillis?.let { DateTimeUtils.formatDate(it) } ?: "Definir Data",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showConsultationTimePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = consultationDateMillis?.let { DateTimeUtils.formatTime(it) } ?: "Definir Hora",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Valor da Consulta (R$)
                            CurrencyInputField(
                                valueText = consultationFeeText,
                                onValueChange = { consultationFeeText = it },
                                label = "Valor da Consulta (R$)"
                            )

                            // Notas Médicas e Orientações
                            OutlinedTextField(
                                value = doctorNotes,
                                onValueChange = { doctorNotes = it },
                                label = { Text("Orientações e Notas Médicas") },
                                placeholder = { Text("Ex: Ingerir 3L de água/dia, meta de 100g de proteína.") },
                                minLines = 3,
                                leadingIcon = {
                                    Icon(Icons.Default.Notes, contentDescription = null)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Botão Salvar Dados Médicos
                            Button(
                                onClick = {
                                    val fee = CurrencyUtils.extractDoubleFromCurrency(consultationFeeText)
                                    viewModel.saveDoctorProfile(
                                        DoctorProfile(
                                            name = doctorName.trim(),
                                            crm = crm.trim(),
                                            specialty = specialty.trim(),
                                            clinicAddress = clinicAddress.trim(),
                                            phone = phone.trim(),
                                            nextConsultationMillis = consultationDateMillis,
                                            consultationFee = fee,
                                            notes = doctorNotes.trim()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Salvar Dados Médicos")
                            }
                        }
                    }
                }

                // Consultas Retroativas Realizadas
                if (uiState.pastConsultations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Histórico de Consultas Realizadas (${uiState.pastConsultations.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(uiState.pastConsultations) { consultation ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = consultation.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Realizada em: ${DateTimeUtils.formatDate(consultation.dateMillis)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (consultation.amount > 0) {
                                        Text(
                                            text = CurrencyUtils.formatCurrency(consultation.amount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteExpense(consultation.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover consulta",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal DatePicker Consulta
    if (showConsultationDatePicker) {
        Material3DatePickerDialog(
            initialMillis = consultationDateMillis,
            onDateSelected = { selectedDateMillis ->
                // Preserva o horário anterior se existir ou coloca 09:00
                val date = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val currentHour = consultationDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour
                } ?: 9
                val currentMin = consultationDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).minute
                } ?: 0
                val combined = date.atTime(currentHour, currentMin)
                consultationDateMillis = DateTimeUtils.localDateTimeToMillis(combined)
                showConsultationDatePicker = false
            },
            onDismiss = { showConsultationDatePicker = false }
        )
    }

    // Modal TimePicker Consulta
    if (showConsultationTimePicker) {
        val currentHour = consultationDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour
        } ?: 9
        val currentMin = consultationDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).minute
        } ?: 0

        Material3TimePickerDialog(
            initialHour = currentHour,
            initialMinute = currentMin,
            onTimeSelected = { hour, minute ->
                val baseDate = consultationDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: LocalDate.now()
                val combined = baseDate.atTime(hour, minute)
                consultationDateMillis = DateTimeUtils.localDateTimeToMillis(combined)
                showConsultationTimePicker = false
            },
            onDismiss = { showConsultationTimePicker = false }
        )
    }

    if (showAddBoxDialog) {
        com.bolsoleve.app.presentation.components.AddMedicationBoxDialog(
            initialMedicationName = uiState.medicationName,
            initialDoseMg = uiState.currentDoseMg,
            onDismiss = { showAddBoxDialog = false },
            onConfirm = { boxInput ->
                viewModel.addMedicationBox(boxInput)
            }
        )
    }

    if (showAddSingleDoseDialog) {
        com.bolsoleve.app.presentation.components.AddSingleDoseDialog(
            initialMedicationName = uiState.medicationName,
            initialDoseMg = uiState.currentDoseMg,
            onDismiss = { showAddSingleDoseDialog = false },
            onConfirm = { name, dose, dateMillis ->
                viewModel.recordRetroactiveDose(name, dose, dateMillis)
            }
        )
    }

    // Modal para Adicionar Consulta Retroativa
    if (showAddPastConsultationDialog) {
        AddPastConsultationDialog(
            initialDoctorName = doctorName.ifBlank { uiState.doctorProfile.name },
            initialFee = uiState.doctorProfile.consultationFee,
            onDismiss = { showAddPastConsultationDialog = false },
            onConfirm = { docName, fee, dateMillis, notes ->
                viewModel.addPastConsultation(docName, fee, dateMillis, notes)
                showAddPastConsultationDialog = false
            }
        )
    }
}
