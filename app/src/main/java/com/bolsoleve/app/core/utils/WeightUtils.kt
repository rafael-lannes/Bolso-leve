package com.bolsoleve.app.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object WeightUtils {
    private val decimalFormat: DecimalFormat = run {
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        DecimalFormat("#0.0", symbols)
    }

    /**
     * Formata um valor de peso em kg com 1 casa decimal (ex: 84,5 kg)
     */
    fun formatWeight(weight: Double): String {
        return "${decimalFormat.format(weight)} kg"
    }

    /**
     * Formata diferença de peso com sinal +/- (ex: -3,2 kg ou +0,5 kg)
     */
    fun formatWeightDiff(diff: Double): String {
        val prefix = if (diff > 0) "+" else ""
        return "$prefix${decimalFormat.format(diff)} kg"
    }

    /**
     * Valida e converte entrada de peso com proteção rigorosa para evitar crashes.
     * Aceita valores entre 20.0kg e 450.0kg.
     */
    fun parseWeight(input: String): Double? {
        val clean = input
            .replace("kg", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", ".")
            .trim()
        val parsed = clean.toDoubleOrNull()
        return if (parsed != null && parsed in 20.0..450.0) parsed else null
    }

    /**
     * Normaliza entrada em tempo real para permitir digitação fluida com vírgula ou ponto
     */
    fun cleanWeightInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
        val normalized = filtered.replace(',', '.')
        val parts = normalized.split('.')
        return when {
            parts.size > 2 -> "${parts[0]}.${parts[1]}"
            parts.size == 2 && parts[1].length > 1 -> "${parts[0]}.${parts[1].take(1)}"
            else -> input
        }
    }
}
