package com.example.waterreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.waterreminder.data.CODIGO_PERMISO_NOTIFICACIONES
import com.example.waterreminder.ui.screens.PantallaConfig
import com.example.waterreminder.ui.screens.PantallaHistorial
import com.example.waterreminder.ui.screens.PantallaHoy
import com.example.waterreminder.ui.theme.WaterReminderTheme
import com.example.waterreminder.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), CODIGO_PERMISO_NOTIFICACIONES)
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
                            title = { Text(stringResource(R.string.top_bar_title)) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary)
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "hoy",
                                onClick = { navController.navigate("hoy") },
                                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_hoy)) },
                                label = { Text(stringResource(R.string.nav_hoy)) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "hist",
                                onClick = { navController.navigate("hist") },
                                icon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.nav_historial)) },
                                label = { Text(stringResource(R.string.nav_historial)) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "conf",
                                onClick = { navController.navigate("conf") },
                                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_ajustes)) },
                                label = { Text(stringResource(R.string.nav_ajustes)) }
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
