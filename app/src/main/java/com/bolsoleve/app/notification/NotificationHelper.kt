package com.bolsoleve.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bolsoleve.app.MainActivity
import com.bolsoleve.app.R
import java.util.concurrent.TimeUnit

object BolsoLeveNotificationHelper {
    const val CHANNEL_MEDICATION_ID = "channel_medication_reminder"
    const val CHANNEL_WEIGH_IN_ID = "channel_weigh_in_reminder"
    const val NOTIFICATION_MEDICATION_ID = 1001
    const val NOTIFICATION_WEIGH_IN_ID = 1002

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
            }

            val weighChannel = NotificationChannel(
                CHANNEL_WEIGH_IN_ID,
                "Lembretes de Pesagem",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes para a pesagem semanal recomendada"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(medChannel)
            notificationManager.createNotificationChannel(weighChannel)
        }
    }

    fun showMedicationReminder(context: Context, medicationName: String, doseMg: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICATION_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hoje é dia de dose! 💉")
            .setContentText("Não se esqueça de aplicar $medicationName ($doseMg mg) e confirmar no app.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mantenha a regularidade semanal da sua dose de $medicationName ($doseMg mg) para garantir a máxima eficácia do tratamento."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_MEDICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permissão não concedida no Android 13+
        }
    }

    fun showWeighInReminder(context: Context) {
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hora da pesagem semanal! ⚖️")
            .setContentText("Pese-se pela manhã, em jejum e após ir ao banheiro.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Dica Bolso+Leve: Para dados mais precisos, pese-se pela manhã, em jejum, descalço e com roupas leves."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_WEIGH_IN_ID, notification)
        } catch (e: SecurityException) {
            // Permissão não concedida no Android 13+
        }
    }
}

class MedicationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        BolsoLeveNotificationHelper.showMedicationReminder(
            applicationContext,
            medicationName = "sua medicação",
            doseMg = 2.5
        )
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "weekly_medication_reminder"

    fun scheduleWeeklyReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            7, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
