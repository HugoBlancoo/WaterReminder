package com.example.waterreminder.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.waterreminder.data.CLAVE_HISTORIAL
import com.example.waterreminder.data.CLAVE_INTERVALO
import com.example.waterreminder.data.CLAVE_META
import com.example.waterreminder.data.CLAVE_TEMA
import com.example.waterreminder.data.DatosDiaSemana
import com.example.waterreminder.data.NOMBRE_TRABAJO_RECORDATORIO
import com.example.waterreminder.data.RangoTiempo
import com.example.waterreminder.data.dataStore
import com.example.waterreminder.data.deserializarHistorial
import com.example.waterreminder.data.serializarHistorial
import com.example.waterreminder.worker.RecordatorioWorker
import com.example.waterreminder.widget.WaterWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    var aguaBebidaHoy by mutableIntStateOf(0)
        private set
    var porcentajeHoy by mutableIntStateOf(0)
        private set
    var progresoHoy by mutableFloatStateOf(0f)
        private set
    var metaDiaria by mutableIntStateOf(2000)
        private set

    var historialMostrado by mutableStateOf<List<DatosDiaSemana>>(emptyList())
        private set
    var rangoSeleccionado by mutableStateOf(RangoTiempo.SEMANA)
        private set

    var diasMetaLograda by mutableIntStateOf(0)
        private set
    var promedioDiario by mutableIntStateOf(0)
        private set
    var totalBebido by mutableIntStateOf(0)
        private set

    var temaElegido by mutableIntStateOf(0)
        private set
    var intervaloNotificaciones by mutableIntStateOf(2)
        private set

    private var mapaHistorialRaw = mapOf<Long, Int>()

    init {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                metaDiaria = prefs[CLAVE_META] ?: 2000
                temaElegido = prefs[CLAVE_TEMA] ?: 0
                intervaloNotificaciones = prefs[CLAVE_INTERVALO] ?: 2
                mapaHistorialRaw = deserializarHistorial(prefs[CLAVE_HISTORIAL] ?: "")

                val hoy = LocalDate.now().toEpochDay()
                aguaBebidaHoy = mapaHistorialRaw[hoy] ?: 0
                progresoHoy = if (metaDiaria > 0) (aguaBebidaHoy.toFloat() / metaDiaria).coerceIn(0f, 1f) else 0f
                porcentajeHoy = (progresoHoy * 100).toInt()

                recalcularGraficoYEstadisticas()
            }
        }
    }

    private fun recalcularGraficoYEstadisticas() {
        val lista = mutableListOf<DatosDiaSemana>()
        val formatoFechaCorta = DateTimeFormatter.ofPattern("dd/MM")

        var sumaMlRango = 0
        var diasCompletados = 0

        for (i in (rangoSeleccionado.dias - 1) downTo 0) {
            val fecha = LocalDate.now().minusDays(i.toLong())
            val ml = mapaHistorialRaw[fecha.toEpochDay()] ?: 0

            sumaMlRango += ml
            val progresoDia = if (metaDiaria > 0) (ml.toFloat() / metaDiaria).coerceIn(0f, 1f) else 0f
            if (progresoDia >= 1f) diasCompletados++

            val nombreEje = if (rangoSeleccionado.dias <= 14) {
                fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")).uppercase()
            } else {
                fecha.format(formatoFechaCorta)
            }

            lista.add(DatosDiaSemana(nombreDia = nombreEje, mililitros = ml, progreso = progresoDia))
        }

        historialMostrado = lista
        totalBebido = sumaMlRango
        promedioDiario = if (rangoSeleccionado.dias > 0) sumaMlRango / rangoSeleccionado.dias else 0
        diasMetaLograda = diasCompletados
    }

    // MAGIA: Función para forzar la actualización de la memoria del widget
    private suspend fun sincronizarWidget() {
        val prefsApp = context.dataStore.data.first()
        val historial = prefsApp[CLAVE_HISTORIAL] ?: ""
        val meta = prefsApp[CLAVE_META] ?: 2000

        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(WaterWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { widgetPrefs ->
                widgetPrefs[CLAVE_HISTORIAL] = historial
                widgetPrefs[CLAVE_META] = meta
            }
            WaterWidget().update(context, glanceId)
        }
    }

    fun cambiarRango(nuevoRango: RangoTiempo) {
        rangoSeleccionado = nuevoRango
        recalcularGraficoYEstadisticas()
    }

    fun registrarAgua(cantidad: Int) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                val mapa = deserializarHistorial(prefs[CLAVE_HISTORIAL] ?: "").toMutableMap()
                val hoy = LocalDate.now().toEpochDay()
                mapa[hoy] = (mapa[hoy] ?: 0) + cantidad
                prefs[CLAVE_HISTORIAL] = serializarHistorial(mapa)
            }
            sincronizarWidget() // Empujamos al widget
        }

        val peticionRecordatorio = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(intervaloNotificaciones.toLong(), TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE_TRABAJO_RECORDATORIO, ExistingWorkPolicy.REPLACE, peticionRecordatorio)
    }

    fun actualizarMeta(nueva: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[CLAVE_META] = nueva }
            sincronizarWidget() // Empujamos al widget
        }
    }

    fun actualizarIntervalo(horas: Int) {
        viewModelScope.launch { context.dataStore.edit { it[CLAVE_INTERVALO] = horas } }
    }

    fun guardarTema(nuevoTema: Int) {
        viewModelScope.launch { context.dataStore.edit { it[CLAVE_TEMA] = nuevoTema } }
    }

    fun refrescarDia() {
        viewModelScope.launch {
            val hoy = LocalDate.now().toEpochDay()
            var diaInicializado = false
            context.dataStore.edit { prefs ->
                val mapa = deserializarHistorial(prefs[CLAVE_HISTORIAL] ?: "").toMutableMap()
                if (!mapa.containsKey(hoy)) {
                    mapa[hoy] = 0
                    prefs[CLAVE_HISTORIAL] = serializarHistorial(mapa)
                    diaInicializado = true
                }
            }
            if (diaInicializado) {
                sincronizarWidget() // Empujamos al widget
            }
        }
    }
}
