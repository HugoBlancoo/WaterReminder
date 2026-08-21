package com.example.waterreminder.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waterreminder.R
import com.example.waterreminder.data.RangoTiempo
import com.example.waterreminder.ui.components.TarjetaEstadistica
import com.example.waterreminder.viewmodel.WaterViewModel
import java.util.Locale

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
        Text(stringResource(R.string.tu_evolucion), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

        Text(stringResource(R.string.estadisticas_del_periodo), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaEstadistica(
                titulo = stringResource(R.string.dias_logrados),
                valor = "${viewModel.diasMetaLograda}",
                icono = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
            TarjetaEstadistica(
                titulo = stringResource(R.string.promedio),
                valor = stringResource(R.string.formato_ml, viewModel.promedioDiario),
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
                titulo = stringResource(R.string.consumo_total),
                valor = stringResource(R.string.formato_litros, totalLitros),
                icono = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            TarjetaEstadistica(
                titulo = stringResource(R.string.objetivo),
                valor = stringResource(R.string.formato_ml, viewModel.metaDiaria),
                icono = Icons.Default.Info,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
