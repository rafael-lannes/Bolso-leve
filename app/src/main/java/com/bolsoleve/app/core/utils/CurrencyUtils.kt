package com.bolsoleve.app.core.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val decimalSymbols = DecimalFormatSymbols(ptBrLocale)
    private val editingFormat = DecimalFormat("#,##0.00", decimalSymbols)

    /**
     * Formata um valor numérico Double para padrão R$ oficial (ex: R$ 1.250,50)
     */
    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    /**
     * Formata um valor Double existente para preenchimento de campo editável (ex: 1250,00)
     */
    fun formatForEditing(amount: Double): String {
        if (amount <= 0.0) return ""
        return editingFormat.format(amount).replace(".", "")
    }

    /**
     * Limpa e valida a digitação em tempo real:
     * - Permite dígitos e no máximo UMA vírgula ou ponto
     * - Limita a 2 casas decimais após a vírgula
     */
    fun cleanCurrencyInput(input: String): String {
        val filtered = input
            .replace("R$", "")
            .trim()
            .filter { it.isDigit() || it == ',' || it == '.' }

        val normalized = filtered.replace('.', ',')
        val parts = normalized.split(',')
        return when {
            parts.size > 2 -> "${parts[0]},${parts[1].take(2)}"
            parts.size == 2 -> "${parts[0]},${parts[1].take(2)}"
            else -> normalized
        }
    }

    /**
     * Converte o texto digitado pelo usuário (com vírgula, ponto ou número puro) para Double de forma precisa e segura.
     * Exemplos aceitos:
     * - "1200" -> 1200.0
     * - "1200,50" -> 1200.5
     * - "1.200,50" -> 1200.5
     * - "1200.50" -> 1200.5
     * - "R$ 1.250,00" -> 1250.0
     */
    fun parseCurrency(input: String): Double {
        if (input.isBlank()) return 0.0

        val clean = input
            .replace("R$", "", ignoreCase = true)
            .replace(" ", "")
            .trim()

        if (clean.isEmpty()) return 0.0

        // Caso tenha ponto e vírgula ao mesmo tempo
        val normalized = if (clean.contains(".") && clean.contains(",")) {
            if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                // Padrão brasileiro: 1.250,50 -> 1250.50
                clean.replace(".", "").replace(",", ".")
            } else {
                // Padrão americano: 1,250.50 -> 1250.50
                clean.replace(",", "")
            }
        } else if (clean.contains(",")) {
            // Apenas vírgula: 1250,50 -> 1250.50
            clean.replace(",", ".")
        } else {
            clean
        }

        return normalized.toDoubleOrNull() ?: 0.0
    }

    /**
     * Alias seguro que agora utiliza parseCurrency para evitar divisões incorretas
     */
    fun extractDoubleFromCurrency(text: String): Double {
        return parseCurrency(text)
    }
}
