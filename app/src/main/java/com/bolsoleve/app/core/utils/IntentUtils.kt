package com.bolsoleve.app.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast

object IntentUtils {

    /**
     * Abre endereço no Google Maps ou app de navegação padrão com geo:0,0?q=
     */
    fun openMaps(context: Context, address: String) {
        if (address.isBlank()) {
            Toast.makeText(context, "Endereço não cadastrado", Toast.LENGTH_SHORT).show()
            return
        }
        val encodedAddress = Uri.encode(address)
        val uri = Uri.parse("geo:0,0?q=$encodedAddress")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback para navegador web com Google Maps
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Abre o discador telefônico com Intent.ACTION_DIAL
     */
    fun openDialer(context: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "Telefone não cadastrado", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o discador", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Adiciona lembrete da consulta na agenda do sistema (CalendarContract)
     */
    fun addToCalendar(
        context: Context,
        doctorName: String,
        specialty: String,
        location: String,
        startTimeMillis: Long,
        notes: String = ""
    ) {
        val title = "Consulta Médica: $doctorName" + if (specialty.isNotBlank()) " ($specialty)" else ""
        val endTimeMillis = startTimeMillis + (60 * 60 * 1000) // 1 hora de duração padrão

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.Events.DESCRIPTION, "Acompanhamento de tratamento semanal Bolso+Leve. $notes")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o app de Agenda", Toast.LENGTH_SHORT).show()
        }
    }
}
