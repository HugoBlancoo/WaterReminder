package com.example.waterreminder.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.waterreminder.MainActivity
import com.example.waterreminder.R
import com.example.waterreminder.data.CLAVE_INTERVALO
import com.example.waterreminder.data.ID_CANAL_RECORDATORIOS
import com.example.waterreminder.data.ID_NOTIFICACION_RECORDATORIO
import com.example.waterreminder.data.NOMBRE_TRABAJO_RECORDATORIO
import com.example.waterreminder.data.dataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RecordatorioWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                ID_CANAL_RECORDATORIOS,
                context.getString(R.string.canal_recordatorios_nombre),
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, ID_CANAL_RECORDATORIOS)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(context.getString(R.string.notificacion_titulo))
            .setContentText(context.getString(R.string.notificacion_texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ID_NOTIFICACION_RECORDATORIO, notificacion)

        // --- NUEVO: REPROGRAMAR LA SIGUIENTE NOTIFICACIÓN ---
        // Leemos el intervalo guardado en los ajustes
        val prefs = context.dataStore.data.first()
        val intervaloActual = prefs[CLAVE_INTERVALO] ?: 2

        // Programamos el siguiente aviso automático
        val peticionRecordatorio = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(intervaloActual.toLong(), TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NOMBRE_TRABAJO_RECORDATORIO,
            ExistingWorkPolicy.APPEND_OR_REPLACE, // <-- EL CAMBIO CLAVE
            peticionRecordatorio
        )

        return Result.success()
    }
}
