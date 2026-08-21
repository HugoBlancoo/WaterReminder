package com.example.waterreminder

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.waterreminder.ui.theme.WaterReminderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

// ==========================================
// 1. CONFIGURACIÓN GLOBAL Y MODELOS
// ==========================================
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes_agua")
val CLAVE_META = intPreferencesKey("meta_diaria")
val CLAVE_HISTORIAL = stringPreferencesKey("historial_completo")
val CLAVE_TEMA = intPreferencesKey("modo_tema")
val CLAVE_INTERVALO = intPreferencesKey("intervalo_notificacion")

data class DatosDiaSemana(val nombreDia: String, val mililitros: Int, val progreso: Float)

enum class RangoTiempo(val dias: Int, val etiqueta: String) {
    SEMANA(7, "1 Sem"),
    DOS_SEMANAS(14, "2 Sem"),
    MES(30, "1 Mes"),
    SEIS_MESES(180, "6 Mes"),
    ANO(365, "1 Año")
}

// ==========================================
// 2. VIEWMODEL (EL CEREBRO)
// ==========================================
@RequiresApi(Build.VERSION_CODES.O)
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
                mapaHistorialRaw = deserializar(prefs[CLAVE_HISTORIAL] ?: "")

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
                val mapa = deserializar(prefs[CLAVE_HISTORIAL] ?: "").toMutableMap()
                val hoy = LocalDate.now().toEpochDay()
                mapa[hoy] = (mapa[hoy] ?: 0) + cantidad
                prefs[CLAVE_HISTORIAL] = serializar(mapa)
            }
            sincronizarWidget() // Empujamos al widget
        }

        val peticionRecordatorio = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(intervaloNotificaciones.toLong(), TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("alarma_agua", ExistingWorkPolicy.REPLACE, peticionRecordatorio)
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

    private fun deserializar(t: String) = if (t.isEmpty()) emptyMap() else t.split(",").associate { it.split(":")[0].toLong() to it.split(":")[1].toInt() }
    private fun serializar(m: Map<Long, Int>) = m.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun refrescarDia() {
        val hoy = LocalDate.now().toEpochDay()
        if (!mapaHistorialRaw.containsKey(hoy)) {
            viewModelScope.launch {
                context.dataStore.edit { prefs ->
                    val mapa = deserializar(prefs[CLAVE_HISTORIAL] ?: "").toMutableMap()
                    mapa[hoy] = 0
                    prefs[CLAVE_HISTORIAL] = serializar(mapa)
                }
                sincronizarWidget() // Empujamos al widget
            }
        }
    }
}

// ==========================================
// 3. PANTALLAS INDIVIDUALES
// ==========================================

@Composable
fun PantallaHoy(viewModel: WaterViewModel) {
    val progresoAnimado by animateFloatAsState(
        targetValue = viewModel.progresoHoy,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "animacionProgreso"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progresoAnimado },
                modifier = Modifier.size(240.dp),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${viewModel.porcentajeHoy}%", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${viewModel.aguaBebidaHoy} / ${viewModel.metaDiaria} ml", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (viewModel.porcentajeHoy >= 100) {
            Text(text = "¡Meta Cumplida!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.registrarAgua(180) }) { Text("+ 180 ml") }
            Button(onClick = { viewModel.registrarAgua(250) }) { Text("+ 250 ml") }
            Button(onClick = { viewModel.registrarAgua(500) }) { Text("+ 500 ml") }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PantallaHistorial(viewModel: WaterViewModel) {
    val estadoDesplazamiento = rememberLazyListState()

    LaunchedEffect(viewModel.historialMostrado) {
        if (viewModel.historialMostrado.isNotEmpty()) {
            estadoDesplazamiento.scrollToItem(viewModel.historialMostrado.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tu Evolución", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RangoTiempo.values()) { rango ->
                val estaSeleccionado = viewModel.rangoSeleccionado == rango
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.cambiarRango(rango) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = rango.etiqueta,
                        color = if (estaSeleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        LazyRow(
            state = estadoDesplazamiento,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(viewModel.historialMostrado) { dia ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier.width(28.dp).height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val alturaBarra = (140 * dia.progreso).dp
                        Box(
                            modifier = Modifier.fillMaxWidth().height(if (alturaBarra > 0.dp) alturaBarra else 2.dp)
                                .background(if(dia.progreso >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dia.nombreDia, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    Text(if(dia.mililitros > 0) "${dia.mililitros}" else "-", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("Estadísticas del periodo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaEstadistica(
                titulo = "Días Logrados",
                valor = "${viewModel.diasMetaLograda}",
                icono = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
            TarjetaEstadistica(
                titulo = "Promedio",
                valor = "${viewModel.promedioDiario} ml",
                icono = Icons.Default.DateRange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val totalLitros = String.format(Locale("es"), "%.1f", viewModel.totalBebido / 1000f)
            TarjetaEstadistica(
                titulo = "Consumo Total",
                valor = "$totalLitros L",
                icono = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            TarjetaEstadistica(
                titulo = "Objetivo",
                valor = "${viewModel.metaDiaria} ml",
                icono = Icons.Default.Info,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TarjetaEstadistica(titulo: String, valor: String, icono: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Icon(
            imageVector = icono,
            contentDescription = titulo,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PantallaConfig(viewModel: WaterViewModel) {
    var meta by remember { mutableStateOf(viewModel.metaDiaria.toString()) }
    val opcionesTema = listOf("Seguir Sistema", "Modo Claro", "Modo Oscuro")
    var intervalo by remember { mutableStateOf(viewModel.intervaloNotificaciones.toString()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Meta Diaria", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = meta,
            onValueChange = { meta = it },
            label = { Text("Mililitros al día") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = { viewModel.actualizarMeta(meta.toIntOrNull() ?: 2000) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Guardar Meta")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Recordatorios", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = intervalo,
            onValueChange = { intervalo = it },
            label = { Text("Frecuencia (en horas)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = { viewModel.actualizarIntervalo(intervalo.toIntOrNull() ?: 2) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Guardar Frecuencia")
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))

        Text("Apariencia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        opcionesTema.forEachIndexed { index, etiqueta ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (index == viewModel.temaElegido),
                        onClick = { viewModel.guardarTema(index) }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (index == viewModel.temaElegido),
                    onClick = { viewModel.guardarTema(index) }
                )
                Text(text = etiqueta, modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ==========================================
// 4. MAIN ACTIVITY & NAVEGACIÓN
// ==========================================
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val viewModel = ViewModelProvider(this).get(WaterViewModel::class.java)

        setContent {
            val oscuroDelSistema = isSystemInDarkTheme()
            val usarModoOscuro = when(viewModel.temaElegido) {
                1 -> false
                2 -> true
                else -> oscuroDelSistema
            }

            WaterReminderTheme(darkTheme = usarModoOscuro) {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refrescarDia()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Water Reminder") },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "hoy",
                                onClick = { navController.navigate("hoy") },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Hoy") },
                                label = { Text("Hoy") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "hist",
                                onClick = { navController.navigate("hist") },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Historial") },
                                label = { Text("Historial") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "conf",
                                onClick = { navController.navigate("conf") },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                                label = { Text("Ajustes") }
                            )
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "hoy",
                        modifier = Modifier.padding(paddingValues)
                            .background(MaterialTheme.colorScheme.background) // Asegura que el fondo general aplique el tema
                    ) {
                        composable("hoy") { PantallaHoy(viewModel) }
                        composable("hist") { PantallaHistorial(viewModel) }
                        composable("conf") { PantallaConfig(viewModel) }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. TRABAJADOR DE NOTIFICACIONES (WORKER)
// ==========================================
class RecordatorioWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val idCanal = "recordatorios_agua"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(idCanal, "Recordatorios de Hidratación", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, idCanal)
            .setSmallIcon(R.drawable.ic_water_drop)            .setContentTitle("¡Hora de beber agua!")
            .setContentText("No has registrado agua en el último intervalo. ¡Mantente hidratado!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notificacion)

        // --- NUEVO: REPROGRAMAR LA SIGUIENTE NOTIFICACIÓN ---
        // Leemos el intervalo guardado en los ajustes
        val prefs = context.dataStore.data.first()
        val intervaloActual = prefs[CLAVE_INTERVALO] ?: 2

        // Programamos el siguiente aviso automático
        val peticionRecordatorio = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(intervaloActual.toLong(), TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "alarma_agua",
            ExistingWorkPolicy.APPEND_OR_REPLACE, // <-- EL CAMBIO CLAVE
            peticionRecordatorio
        )

        return Result.success()
    }
}