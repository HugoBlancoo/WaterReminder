package com.example.waterreminder.ui.screens

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterreminder.R
import com.example.waterreminder.viewmodel.WaterViewModel

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
            Text(text = stringResource(R.string.meta_cumplida), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.registrarAgua(180) }) { Text(stringResource(R.string.boton_agregar_ml, 180)) }
            Button(onClick = { viewModel.registrarAgua(250) }) { Text(stringResource(R.string.boton_agregar_ml, 250)) }
            Button(onClick = { viewModel.registrarAgua(500) }) { Text(stringResource(R.string.boton_agregar_ml, 500)) }
        }
    }
}
