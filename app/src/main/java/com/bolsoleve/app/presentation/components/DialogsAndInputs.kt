package com.bolsoleve.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bolsoleve.app.core.utils.CurrencyUtils
import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.core.utils.WeightUtils
import com.bolsoleve.app.domain.model.ExpenseCategory
import com.bolsoleve.app.domain.model.WeightRecord

/**
 * Campo de texto monetário com prefixo nativo R$ do Material 3
 * e digitação decimal natural, fluida e à prova de falhas.
 */
@Composable
fun CurrencyInputField(
    valueText: String,
    onValueChange: (String) -> Unit,
    label: String = "Valor",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valueText,
        onValueChange = { input ->
            val cleaned = CurrencyUtils.cleanCurrencyInput(input)
            onValueChange(cleaned)
        },
        label = { Text(label) },
        prefix = {
            Text(
                text = "R$ ",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        placeholder = { Text("0,00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Diálogo Material 3 de Seleção de Data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3DatePickerDialog(
    initialMillis: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Diálogo Material 3 de Seleção de Horário
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3TimePickerDialog(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecione o Horário",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TimePicker(state = timePickerState)
            }
        }
    )
}

/**
 * Diálogo para cadastrar nova compra de remédio / caneta ou consulta
 */
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (description: String, category: ExpenseCategory, amount: Double, quantity: Int) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.MEDICATION) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Registrar Investimento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tipo de Despesa:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedCategory = ExpenseCategory.MEDICATION },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == ExpenseCategory.MEDICATION)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedCategory == ExpenseCategory.MEDICATION)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Medicação")
                    }

                    Button(
                        onClick = { selectedCategory = ExpenseCategory.CONSULTATION },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == ExpenseCategory.CONSULTATION)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedCategory == ExpenseCategory.CONSULTATION)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Consulta")
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / Medicamento") },
                    placeholder = { Text("Ex: Caneta Mounjaro 5mg") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                CurrencyInputField(
                    valueText = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = "Valor Total (R$)"
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantidade de Canetas / Doses") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Por favor preencha descrição e valor válidos.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.extractDoubleFromCurrency(rawAmount)
                    val qty = quantityText.toIntOrNull() ?: 1
                    if (description.isNotBlank() && amount > 0.0) {
                        onConfirm(description.trim(), selectedCategory, amount, qty)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para cadastrar compra de caixa de medicação com preço e doses retroativas
 */
@Composable
fun AddMedicationBoxDialog(
    initialMedicationName: String = "Mounjaro",
    initialDoseMg: Double = 2.5,
    onDismiss: () -> Unit,
    onConfirm: (com.bolsoleve.app.domain.model.MedicationBoxInput) -> Unit
) {
    var medicationName by remember { mutableStateOf(initialMedicationName) }
    var doseText by remember { mutableStateOf(initialDoseMg.toString()) }
    var boxPriceText by remember { mutableStateOf("") }
    var dosesCountText by remember { mutableStateOf("4") }
    var purchaseDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var registerRetroactiveDoses by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val commonMeds = listOf("Mounjaro", "Ozempic", "Wegovy")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Adicionar Caixa de Remédio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecione ou digite o medicamento:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonMeds.forEach { name ->
                        androidx.compose.material3.FilterChip(
                            selected = medicationName.equals(name, ignoreCase = true),
                            onClick = { medicationName = name },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("Nome da Medicação") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = doseText,
                        onValueChange = { doseText = it },
                        label = { Text("Dose (mg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dosesCountText,
                        onValueChange = { dosesCountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Doses / Semanas") },
                        placeholder = { Text("4") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                CurrencyInputField(
                    valueText = boxPriceText,
                    onValueChange = { boxPriceText = it },
                    label = "Preço da Caixa (R$)"
                )

                // Seletor de data da compra / primeira dose (pode ser retroativa)
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Data da Compra / Início desta caixa:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.bolsoleve.app.core.utils.DateTimeUtils.formatDate(purchaseDateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Opção para gerar doses semanais retroativas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = registerRetroactiveDoses,
                        onCheckedChange = { registerRetroactiveDoses = it }
                    )
                    Text(
                        text = "Registrar doses semanais desta caixa no histórico",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isError) {
                    Text(
                        text = "Preencha o nome, dose válida e preço da caixa.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dose = doseText.replace(',', '.').toDoubleOrNull()
                    val price = CurrencyUtils.extractDoubleFromCurrency(boxPriceText)
                    val count = dosesCountText.toIntOrNull() ?: 4
                    if (medicationName.isNotBlank() && dose != null && dose > 0 && price > 0) {
                        onConfirm(
                            com.bolsoleve.app.domain.model.MedicationBoxInput(
                                medicationName = medicationName.trim(),
                                doseMg = dose,
                                boxPrice = price,
                                dosesCount = count,
                                purchaseDateMillis = purchaseDateMillis,
                                registerRetroactiveDoses = registerRetroactiveDoses
                            )
                        )
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        Material3DatePickerDialog(
            initialMillis = purchaseDateMillis,
            onDateSelected = {
                purchaseDateMillis = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Diálogo para registrar uma aplicação individual de dose (pode ser data de hoje ou retroativa)
 */
@Composable
fun AddSingleDoseDialog(
    initialMedicationName: String,
    initialDoseMg: Double,
    onDismiss: () -> Unit,
    onConfirm: (medicationName: String, doseMg: Double, dateMillis: Long) -> Unit
) {
    var medicationName by remember { mutableStateOf(initialMedicationName) }
    var doseText by remember { mutableStateOf(initialDoseMg.toString()) }
    var appliedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Registrar Aplicação de Dose",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("Nome da Medicação") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = doseText,
                    onValueChange = { doseText = it },
                    label = { Text("Dose Aplicada (mg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Data da Aplicação (Hoje ou Retroativa):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.bolsoleve.app.core.utils.DateTimeUtils.formatDate(appliedDateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (isError) {
                    Text(
                        text = "Informe medicação e dose válidas.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dose = doseText.replace(',', '.').toDoubleOrNull()
                    if (medicationName.isNotBlank() && dose != null && dose > 0) {
                        onConfirm(medicationName.trim(), dose, appliedDateMillis)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salvar Aplicação")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        Material3DatePickerDialog(
            initialMillis = appliedDateMillis,
            onDateSelected = {
                appliedDateMillis = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Diálogo para cadastrar pesagem anterior / retroativa
 */
@Composable
fun AddPastWeightDialog(
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double, dateMillis: Long, notes: String) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)) } // 1 semana atrás por padrão
    var showDatePicker by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Inserir Pesagem Anterior",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cadastre pesagens passadas para enriquecer a curva e o histórico de evolução do peso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = com.bolsoleve.app.core.utils.WeightUtils.cleanWeightInput(it)
                        isError = false
                    },
                    label = { Text("Peso aferido (kg)") },
                    placeholder = { Text("Ex: 88.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Data da Pesagem:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.bolsoleve.app.core.utils.DateTimeUtils.formatDate(dateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notas / Observações (opcional)") },
                    placeholder = { Text("Ex: Início da 3ª semana de Mounjaro") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Informe um peso válido (20 a 450 kg).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = com.bolsoleve.app.core.utils.WeightUtils.parseWeight(weightText)
                    if (parsed != null) {
                        onConfirm(parsed, dateMillis, notesText.trim())
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salvar Pesagem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        Material3DatePickerDialog(
            initialMillis = dateMillis,
            onDateSelected = {
                dateMillis = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Diálogo "Sobre o Bolso+Leve" com créditos do criador
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Bolso+Leve",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Versão 1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Aplicativo nativo para acompanhamento de tratamento de perda de peso e controle de medicação semanal (como Mounjaro, Ozempic e Wegovy), unindo controle clínico, farmacológico e financeiro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DESENVOLVIDO POR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Rafael Lannes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "GitHub / Portfólio: rafael-lannes.github.io",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://rafael-lannes.github.io")
                                ).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Abrir rafael-lannes.github.io")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Bottom Sheet para listar todas as entradas da curva de peso com opção de editar ou excluir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWeightEntriesBottomSheet(
    records: List<WeightRecord>,
    onDismiss: () -> Unit,
    onEditRecord: (WeightRecord) -> Unit,
    onDeleteRecord: (WeightRecord) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortedRecords = remember(records) { records.sortedByDescending { it.dateMillis } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Histórico de Pesagens",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${records.size} registro(s) encontrado(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar"
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (sortedRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma pesagem registrada no momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedRecords, key = { it.id }) { record ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = DateTimeUtils.formatDate(record.dateMillis),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = WeightUtils.formatWeight(record.weightKg),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (record.notes.isNotBlank()) {
                                        Text(
                                            text = record.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Botão Editar
                                    IconButton(
                                        onClick = { onEditRecord(record) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar pesagem",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Botão Excluir
                                    IconButton(
                                        onClick = { onDeleteRecord(record) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir pesagem",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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

/**
 * Diálogo para Edição de uma Pesagem Existente
 */
@Composable
fun EditWeightEntryDialog(
    record: WeightRecord,
    onDismiss: () -> Unit,
    onConfirm: (id: Long, weightKg: Double, dateMillis: Long, notes: String) -> Unit
) {
    var weightText by remember { mutableStateOf(record.weightKg.toString().replace('.', ',')) }
    var selectedDateMillis by remember { mutableStateOf(record.dateMillis) }
    var notesText by remember { mutableStateOf(record.notes) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Registro de Peso",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Campo Peso em KG
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (sanitized.count { it == '.' } <= 1 && sanitized.all { it.isDigit() || it == '.' }) {
                            weightText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } }
                )

                // Seletor de Data
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Data da Pesagem",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateTimeUtils.formatDate(selectedDateMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Observações
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observação (opcional)") },
                    placeholder = { Text("Ex: Pesagem em jejum, nova balança...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = weightText.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed !in 20.0..450.0) {
                        errorMessage = "Informe um peso válido entre 20kg e 450kg."
                    } else {
                        onConfirm(record.id, parsed, selectedDateMillis, notesText)
                    }
                }
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        Material3DatePickerDialog(
            initialMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                selectedDateMillis = millis
                showDatePicker = false
            }
        )
    }
}

/**
 * Diálogo de Confirmação para Excluir uma Pesagem
 */
@Composable
fun ConfirmDeleteWeightDialog(
    record: WeightRecord,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Excluir Pesagem?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Tem certeza que deseja excluir o registro de ${WeightUtils.formatWeight(record.weightKg)} gravado em ${DateTimeUtils.formatDate(record.dateMillis)}?\n\nSua curva de peso e projeções serão recalculadas."
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(record.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para Registro Retroativo de Consulta Médica Realizada
 */
@Composable
fun AddPastConsultationDialog(
    initialDoctorName: String,
    initialFee: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (doctorName: String, fee: Double, dateMillis: Long, notes: String) -> Unit
) {
    var doctorName by remember { mutableStateOf(initialDoctorName) }
    var feeText by remember {
        mutableStateOf(if (initialFee > 0) CurrencyUtils.formatForEditing(initialFee) else "")
    }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var notesText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adicionar Consulta Retroativa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nome do Médico
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Nome do Médico(a)") },
                    placeholder = { Text("Ex: Dra. Ana Paula Costa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Seletor de Data
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Data em que a consulta ocorreu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateTimeUtils.formatDate(selectedDateMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Valor pago na consulta
                CurrencyInputField(
                    valueText = feeText,
                    onValueChange = { feeText = it },
                    label = "Valor Pago na Consulta (R$)"
                )

                // Anotações da consulta
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Orientações / Anotações") },
                    placeholder = { Text("Ex: Ajuste de dosagem para 5mg, exames normais.") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = CurrencyUtils.extractDoubleFromCurrency(feeText)
                    onConfirm(doctorName.trim(), fee, selectedDateMillis, notesText.trim())
                }
            ) {
                Text("Registrar Consulta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        Material3DatePickerDialog(
            initialMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                selectedDateMillis = millis
                showDatePicker = false
            }
        )
    }
}


