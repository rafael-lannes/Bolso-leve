package com.bolsoleve.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.di.appModules
import com.bolsoleve.app.domain.model.MedicationDose
import com.bolsoleve.app.domain.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MedicationReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MEDICATION_REMINDER = "com.bolsoleve.app.ACTION_MEDICATION_REMINDER"
        const val ACTION_CONFIRM_DOSE = "com.bolsoleve.app.ACTION_CONFIRM_DOSE"
        private const val TAG = "MedReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action=$action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Garante que Koin esteja inicializado mesmo em cold boot
                ensureKoinStarted(context)

                val koin = GlobalContext.get()
                val preferencesRepository = koin.get<UserPreferencesRepository>()
                val medicationRepository = koin.get<MedicationRepository>()
                val prefs = preferencesRepository.userPreferencesFlow.first()

                when (action) {
                    ACTION_CONFIRM_DOSE -> {
                        // Confirmação rápida da dose diretamente pela notificação
                        medicationRepository.recordDose(
                            MedicationDose(
                                medicationName = prefs.medicationName,
                                doseMg = prefs.currentDoseMg,
                                appliedAtMillis = System.currentTimeMillis(),
                                isConfirmed = true
                            )
                        )
                        // Cancela a notificação ativa
                        NotificationManagerCompat.from(context).cancel(BolsoLeveNotificationHelper.NOTIFICATION_MEDICATION_ID)
                        // Agenda próximo lembrete semanal
                        ReminderScheduler.scheduleWeeklyReminder(context)
                    }

                    ACTION_MEDICATION_REMINDER -> {
                        if (prefs.reminderEnabled && prefs.isOnboardingCompleted) {
                            val today = LocalDate.now()
                            // Verifica se hoje é de fato o dia da semana configurado
                            val isDoseDay = today.dayOfWeek.value == prefs.applicationDayOfWeek

                            if (isDoseDay) {
                                // Verifica se a dose já foi tomada/confirmada hoje
                                val latestDose = medicationRepository.getLatestDose().first()
                                val isTodayConfirmed = latestDose?.let { dose ->
                                    val doseDate = Instant.ofEpochMilli(dose.appliedAtMillis)
                                        .atZone(ZoneId.systemDefault()).toLocalDate()
                                    doseDate == today && dose.isConfirmed
                                } ?: false

                                if (!isTodayConfirmed) {
                                    BolsoLeveNotificationHelper.showMedicationReminder(
                                        context = context,
                                        medicationName = prefs.medicationName,
                                        doseMg = prefs.currentDoseMg
                                    )
                                    preferencesRepository.updateLastNotificationDate(today.toString())
                                }
                            }
                        }

                        // Sempre reagenda o próximo lembrete semanal para garantir continuidade perpétua
                        ReminderScheduler.scheduleWeeklyReminder(context)
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                        Log.d(TAG, "Dispositivo reiniciado/atualizado. Reagendando lembretes...")
                        ReminderScheduler.scheduleWeeklyReminder(context)
                        ReminderScheduler.checkAndTriggerPendingNotification(context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar lembrete de medicação: ${e.localizedMessage}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureKoinStarted(context: Context) {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context.applicationContext)
                modules(appModules)
            }
        }
    }
}
