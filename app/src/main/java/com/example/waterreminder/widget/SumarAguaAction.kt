package com.example.waterreminder.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.waterreminder.data.CLAVE_HISTORIAL
import com.example.waterreminder.data.CLAVE_INTERVALO
import com.example.waterreminder.data.CLAVE_META
import com.example.waterreminder.data.NOMBRE_TRABAJO_RECORDATORIO
import com.example.waterreminder.data.dataStore
import com.example.waterreminder.data.deserializarHistorial
import com.example.waterreminder.data.serializarHistorial
import com.example.waterreminder.worker.RecordatorioWorker
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SumarAguaAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val cantidad = parameters[cantidadKey] ?: 250

        context.dataStore.edit { prefs ->
            val textoHistorial = prefs[CLAVE_HISTORIAL] ?: ""
            val mapa = deserializarHistorial(textoHistorial).toMutableMap()

            val hoy = LocalDate.now().toEpochDay()
            mapa[hoy] = (mapa[hoy] ?: 0) + cantidad
            val nuevoHistorial = serializarHistorial(mapa)

            // 1. Guardamos en la app
            prefs[CLAVE_HISTORIAL] = nuevoHistorial

            // 2. Empujamos el dato al widget
            updateAppWidgetState(context, glanceId) { currentState ->
                currentState[CLAVE_HISTORIAL] = nuevoHistorial
                if (!currentState.contains(CLAVE_META)) {
                    currentState[CLAVE_META] = prefs[CLAVE_META] ?: 2000
                }
            }
        }

        WaterWidget().update(context, glanceId)

        val prefsFinales = context.dataStore.data.first()
        val intervaloActual = prefsFinales[CLAVE_INTERVALO] ?: 2

        val peticionRecordatorio = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(intervaloActual.toLong(), TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE_TRABAJO_RECORDATORIO, ExistingWorkPolicy.REPLACE, peticionRecordatorio)
    }
}
