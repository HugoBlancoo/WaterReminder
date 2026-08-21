package com.example.waterreminder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.waterreminder.R
import com.example.waterreminder.viewmodel.WaterViewModel

@Composable
fun PantallaConfig(viewModel: WaterViewModel) {
    var meta by remember { mutableStateOf(viewModel.metaDiaria.toString()) }
    val opcionesTema = listOf(
        stringResource(R.string.tema_seguir_sistema),
        stringResource(R.string.tema_modo_claro),
        stringResource(R.string.tema_modo_oscuro)
    )
    var intervalo by remember { mutableStateOf(viewModel.intervaloNotificaciones.toString()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(stringResource(R.string.ajustes_titulo), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        Text(stringResource(R.string.meta_diaria), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = meta,
            onValueChange = { meta = it },
            label = { Text(stringResource(R.string.label_mililitros_dia)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = { viewModel.actualizarMeta(meta.toIntOrNull() ?: 2000) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.guardar_meta))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.recordatorios), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = intervalo,
            onValueChange = { intervalo = it },
            label = { Text(stringResource(R.string.label_frecuencia_horas)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = { viewModel.actualizarIntervalo(intervalo.toIntOrNull() ?: 2) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.guardar_frecuencia))
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))

        Text(stringResource(R.string.apariencia), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
