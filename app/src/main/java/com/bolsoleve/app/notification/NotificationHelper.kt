package com.bolsoleve.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bolsoleve.app.MainActivity
import com.bolsoleve.app.R
import com.bolsoleve.app.core.utils.DateTimeUtils
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.di.appModules
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
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object BolsoLeveNotificationHelper {
    const val CHANNEL_MEDICATION_ID = "channel_medication_reminder"
    const val CHANNEL_WEIGH_IN_ID = "channel_weigh_in_reminder"
    const val NOTIFICATION_MEDICATION_ID = 1001
    const val NOTIFICATION_WEIGH_IN_ID = 1002
    private const val TAG = "BolsoLeveNotif"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val medChannel = NotificationChannel(
                CHANNEL_MEDICATION_ID,
                "Lembretes de Aplicação Semanal",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para o dia da aplicação da sua dose"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val weighChannel = NotificationChannel(
                CHANNEL_WEIGH_IN_ID,
                "Lembretes de Pesagem",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes para a pesagem semanal recomendada"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(medChannel)
            notificationManager.createNotificationChannel(weighChannel)
        }
    }

    fun showMedicationReminder(context: Context, medicationName: String, doseMg: Double) {
        createNotificationChannels(context)

        // Verificação segura de permissão no Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Permissão POST_NOTIFICATIONS não concedida. Notificação não exibida.")
                return
            }
        }

        // 1. Intent para abrir o aplicativo ao tocar na notificação
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Ação direta: "Confirmar Aplicação" pelo próprio aviso
        val confirmIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_CONFIRM_DOSE
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            confirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedDose = if (doseMg % 1.0 == 0.0) "${doseMg.toInt()} mg" else "$doseMg mg"

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICATION_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hoje é dia de dose! 💉")
            .setContentText("Não se esqueça de aplicar $medicationName ($formattedDose) e confirmar.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Hoje é o seu dia programado para aplicação de $medicationName ($formattedDose). " +
                            "Mantenha a regularidade semanal para garantir a eficácia máxima do seu tratamento!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Confirmar Aplicação",
                confirmPendingIntent
            )
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_MEDICATION_ID, notification)
            Log.d(TAG, "Notificação de lembrete de medicação disparada com sucesso!")
        } catch (e: SecurityException) {
            Log.e(TAG, "Falha de segurança ao postar notificação: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado ao postar notificação: ${e.localizedMessage}")
        }
    }

    fun showWeighInReminder(context: Context) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WEIGH_IN_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora da pesagem semanal! ⚖️")
            .setContentText("Pese-se pela manhã, em jejum e após ir ao banheiro.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Dica Bolso+Leve: Para dados mais precisos, pese-se pela manhã, em jejum, descalço e com roupas leves."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_WEIGH_IN_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao postar lembrete de pesagem: ${e.localizedMessage}")
        }
    }
}

/**
 * Worker periódico do WorkManager como rede de proteção secundária.
 * Executa periodicamente para checar se hoje é dia de dose e se o alarme exato não disparou.
 */
class DailyReminderCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            ReminderScheduler.checkAndTriggerPendingNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyReminderWorker", "Erro na checagem diária: ${e.localizedMessage}")
            Result.retry()
        }
    }
}

/**
 * Agendador Central de Lembretes.
 * Combina AlarmManager exato (com suporte a Doze Mode) com WorkManager diário de proteção.
 */
object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val ALARM_REQUEST_CODE = 8801
    private const val WORK_NAME = "daily_medication_reminder_safety_check"

    private fun ensureKoin(context: Context) {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context.applicationContext)
                modules(appModules)
            }
        }
    }

    /**
     * Agenda o próximo alarme com AlarmManager (RTC_WAKEUP) para o dia da semana e horário configurados.
     */
    fun scheduleWeeklyReminder(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureKoin(context)
                val prefsRepo = GlobalContext.get().get<UserPreferencesRepository>()
                val prefs = prefsRepo.userPreferencesFlow.first()

                if (!prefs.reminderEnabled || !prefs.isOnboardingCompleted) {
                    cancelReminder(context)
                    return@launch
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                    action = MedicationReminderReceiver.ACTION_MEDICATION_REMINDER
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nextAlarmMillis = DateTimeUtils.calculateNextReminderTimeMillis(
                    targetDayOfWeek = prefs.applicationDayOfWeek,
                    hour = prefs.reminderHour,
                    minute = prefs.reminderMinute
                )

                Log.d(
                    TAG,
                    "Agendando alarme para ${DateTimeUtils.formatDateTime(nextAlarmMillis)} " +
                            "(Dia ${prefs.applicationDayOfWeek} - ${DateTimeUtils.getDayName(prefs.applicationDayOfWeek)} às " +
                            "${DateTimeUtils.formatReminderTime(prefs.reminderHour, prefs.reminderMinute)})"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nextAlarmMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                nextAlarmMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextAlarmMillis,
                        pendingIntent
                    )
                }

                // Agenda também o worker de proteção de 12 horas no WorkManager
                scheduleSafetyWorkManager(context)

            } catch (e: Exception) {
                Log.e(TAG, "Falha ao agendar alarme com AlarmManager: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * Agenda checagem de segurança de 12 em 12 horas no WorkManager
     */
    private fun scheduleSafetyWorkManager(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyReminderCheckWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Verifica e dispara notificação caso hoje seja o dia da dose, o horário já tenha chegado
     * e a dose ainda não tenha sido confirmada nem a notificação disparada hoje.
     */
    fun checkAndTriggerPendingNotification(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureKoin(context)
                val prefsRepo = GlobalContext.get().get<UserPreferencesRepository>()
                val medRepo = GlobalContext.get().get<MedicationRepository>()
                val prefs = prefsRepo.userPreferencesFlow.first()

                if (!prefs.reminderEnabled || !prefs.isOnboardingCompleted) return@launch

                val today = LocalDate.now()
                val isDoseDay = today.dayOfWeek.value == prefs.applicationDayOfWeek

                if (isDoseDay) {
                    val nowTime = LocalTime.now()
                    val targetTime = LocalTime.of(
                        prefs.reminderHour.coerceIn(0, 23),
                        prefs.reminderMinute.coerceIn(0, 59)
                    )

                    // Se já chegou no horário configurado hoje
                    if (!nowTime.isBefore(targetTime)) {
                        // Verifica se a dose já foi confirmada
                        val latestDose = medRepo.getLatestDose().first()
                        val isTodayConfirmed = latestDose?.let { dose ->
                            val doseDate = Instant.ofEpochMilli(dose.appliedAtMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            doseDate == today && dose.isConfirmed
                        } ?: false

                        // Verifica se a notificação já foi exibida hoje
                        val alreadyNotifiedToday = prefs.lastNotificationDate == today.toString()

                        if (!isTodayConfirmed && !alreadyNotifiedToday) {
                            Log.d(TAG, "Disparando notificação pendente de hoje!")
                            BolsoLeveNotificationHelper.showMedicationReminder(
                                context = context,
                                medicationName = prefs.medicationName,
                                doseMg = prefs.currentDoseMg
                            )
                            prefsRepo.updateLastNotificationDate(today.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na verificação de notificação pendente: ${e.localizedMessage}")
            }
        }
    }

    fun cancelReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                action = MedicationReminderReceiver.ACTION_MEDICATION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Lembretes cancelados.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cancelar lembretes: ${e.localizedMessage}")
        }
    }
}
