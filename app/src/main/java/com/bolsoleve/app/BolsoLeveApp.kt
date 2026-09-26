package com.bolsoleve.app

import android.app.Application
import android.util.Log
import com.bolsoleve.app.di.appModules
import com.bolsoleve.app.notification.BolsoLeveNotificationHelper
import com.bolsoleve.app.notification.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class BolsoLeveApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Captura global de exceções não tratadas (registra em last_crash.txt para diagnóstico)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()
                Log.e("BolsoLeveCrash", "FATAL CRASH on thread ${thread.name}: $stackTrace")
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText("Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\n\n$stackTrace")
            } catch (e: Exception) {
                Log.e("BolsoLeveCrash", "Failed to write crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        // 2. Inicialização segura do Koin (previne KoinAppAlreadyStartedException em reinicializações)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger(Level.ERROR)
                androidContext(this@BolsoLeveApp)
                modules(appModules)
            }
        }

        // 3. Canais de Notificação (protegido contra exceções de sistema)
        runCatching {
            BolsoLeveNotificationHelper.createNotificationChannels(this)
        }.onFailure { e ->
            Log.e("BolsoLeveApp", "Falha ao criar canais de notificação", e)
        }

        // 4. Agendamento periódico de lembretes no WorkManager (protegido contra falhas de inicialização)
        runCatching {
            ReminderScheduler.scheduleWeeklyReminder(this)
        }.onFailure { e ->
            Log.e("BolsoLeveApp", "Falha ao agendar lembretes no WorkManager", e)
        }
    }
}
