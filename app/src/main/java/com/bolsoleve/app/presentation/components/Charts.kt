package com.bolsoleve.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bolsoleve.app.core.theme.PrimaryLight
import com.bolsoleve.app.core.theme.SuccessGreen
import com.bolsoleve.app.core.theme.TrendDownGreen
import com.bolsoleve.app.core.theme.TrendUpOrange
import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.core.utils.WeightUtils
import com.bolsoleve.app.domain.model.WeeklyWeightVariation
import com.bolsoleve.app.domain.model.WeightRecord

/**
 * Mini-gráfico da Tela de Início (Dashboard)
 */
@Composable
fun MiniWeightChart(
    records: List<WeightRecord>,
    modifier: Modifier = Modifier,
    onManageEntries: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Curva Recente de Peso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onManageEntries != null) {
                    TextButton(
                        onClick = onManageEntries,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Entradas",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editar Entradas",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Text(
                        text = "Últimas semanas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (records.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registre mais pesagens para visualizar sua curva evolutiva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                val weights = records.map { it.weightKg }
                val minWeight = (weights.minOrNull() ?: 0.0) - 1.0
                val maxWeight = (weights.maxOrNull() ?: 100.0) + 1.0
                val range = (maxWeight - minWeight).coerceAtLeast(1.0)

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Máx: ${WeightUtils.formatWeight(weights.maxOrNull() ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Mín: ${WeightUtils.formatWeight(weights.minOrNull() ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val stepX = width / (records.size - 1).coerceAtLeast(1)

                        val points = records.mapIndexed { index, record ->
                            val x = index * stepX
                            val normalizedY = ((record.weightKg - minWeight) / range).toFloat()
                            val y = height - (normalizedY * height)
                            Offset(x, y)
                        }

                        // Path para preenchimento com gradiente
                        val fillPath = Path().apply {
                            moveTo(0f, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(width, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(gradientColor, Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Path da linha
                        val strokePath = Path().apply {
                            points.forEachIndexed { i, pt ->
                                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Desenha pontos
                        points.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 3.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gráfico Detalhado de Evolução do Peso com linha de Meta
 */
@Composable
fun FullEvolutionWeightChart(
    records: List<WeightRecord>,
    targetWeight: Double,
    modifier: Modifier = Modifier,
    onManageEntries: (() -> Unit)? = null
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evolução Completa e Meta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (onManageEntries != null) {
                    TextButton(
                        onClick = onManageEntries,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Entradas",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editar Entradas",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Legenda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = "Histórico de Peso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 3.dp)
                            .background(TrendDownGreen)
                    )
                    Text(
                        text = "Meta (${WeightUtils.formatWeight(targetWeight)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem registros de peso suficientes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val targetLineColor = TrendDownGreen
                val allValues = records.map { it.weightKg } + listOf(targetWeight)
                val minWeight = (allValues.minOrNull() ?: 50.0) - 2.0
                val maxWeight = (allValues.maxOrNull() ?: 100.0) + 2.0
                val range = (maxWeight - minWeight).coerceAtLeast(1.0)

                // Balão com ponto selecionado
                selectedIndex?.let { idx ->
                    if (idx in records.indices) {
                        val record = records[idx]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "${DateTimeUtils.formatDate(record.dateMillis)}: ${WeightUtils.formatWeight(record.weightKg)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 12.dp)
                        .pointerInput(records) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (records.size - 1).coerceAtLeast(1)
                                val closestIndex = (offset.x / stepX).toInt().coerceIn(0, records.size - 1)
                                selectedIndex = closestIndex
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (records.size - 1).coerceAtLeast(1)

                    // Linha da Meta (tracejada)
                    val targetNormY = ((targetWeight - minWeight) / range).toFloat()
                    val targetY = height - (targetNormY * height)
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )

                    val points = records.mapIndexed { index, record ->
                        val x = index * stepX
                        val normalizedY = ((record.weightKg - minWeight) / range).toFloat()
                        val y = height - (normalizedY * height)
                        Offset(x, y)
                    }

                    // Área sob a curva
                    val fillPath = Path().apply {
                        moveTo(0f, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Linha contínua
                    val linePath = Path().apply {
                        points.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Pontos
                    points.forEachIndexed { idx, pt ->
                        val isSelected = selectedIndex == idx
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = if (isSelected) TrendDownGreen else primaryColor,
                            radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gráfico 2: Variação de Peso Líquida por Semana (Barras positivas e negativas)
 */
@Composable
fun WeeklyVariationBarChart(
    variations: List<WeeklyWeightVariation>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Variação Semanal Líquida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(TrendDownGreen, CircleShape))
                        Text("Perda", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(TrendUpOrange, CircleShape))
                        Text("Ganho", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (variations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aguardando mais semanas de pesagem para calcular variações.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxAbsDiff = variations.maxOfOrNull { kotlin.math.abs(it.weightDiffKg) }?.coerceAtLeast(1.0) ?: 1.0

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(vertical = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val barCount = variations.size
                    val slotWidth = width / barCount
                    val barWidth = (slotWidth * 0.55f).coerceAtMost(36.dp.toPx())

                    // Linha zero no centro
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.35f),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1.dp.toPx()
                    )

                    variations.forEachIndexed { index, variation ->
                        val centerX = (index * slotWidth) + (slotWidth / 2f)
                        val diff = variation.weightDiffKg
                        // Se diff < 0 (perda), a barra desce abaixo do centro
                        // Se diff > 0 (ganho), a barra sobe acima do centro
                        val normalized = (kotlin.math.abs(diff) / maxAbsDiff).toFloat().coerceIn(0.05f, 1f)
                        val barHeight = normalized * (height * 0.42f)

                        val isLoss = diff <= 0
                        val barColor = if (isLoss) TrendDownGreen else TrendUpOrange

                        val topLeft = if (isLoss) {
                            Offset(centerX - barWidth / 2f, centerY)
                        } else {
                            Offset(centerX - barWidth / 2f, centerY - barHeight)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = topLeft,
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Legenda com rótulos de semanas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    variations.forEach { variation ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = variation.weekLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = WeightUtils.formatWeightDiff(variation.weightDiffKg),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (variation.weightDiffKg <= 0) TrendDownGreen else TrendUpOrange
                            )
                        }
                    }
                }
            }
        }
    }
}
