package com.example.waterreminder.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.waterreminder.R
import com.example.waterreminder.data.CLAVE_HISTORIAL
import com.example.waterreminder.data.CLAVE_META
import com.example.waterreminder.data.deserializarHistorial
import java.time.LocalDate

val cantidadKey = ActionParameters.Key<Int>("cantidad_agua")

class WaterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Usamos la memoria interna del widget para que reaccione al instante
            val prefs = currentState<Preferences>()
            val textoHistorial = prefs[CLAVE_HISTORIAL] ?: ""
            val metaDiaria = prefs[CLAVE_META] ?: 2000

            val mapaHistorial = deserializarHistorial(textoHistorial)

            val hoyEpoch = LocalDate.now().toEpochDay()
            val aguaBebidaHoy = mapaHistorial[hoyEpoch] ?: 0
            val progresoFloat = if (metaDiaria > 0) (aguaBebidaHoy.toFloat() / metaDiaria).coerceIn(0f, 1f) else 0f
            val porcentajeHoy = (progresoFloat * 100).toInt()

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFE8EAF6)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_water_drop),
                        contentDescription = context.getString(R.string.widget_content_desc_agua),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = context.getString(R.string.widget_hidratacion),
                        style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.Gray))
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = "$porcentajeHoy%",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = ColorProvider(if (progresoFloat >= 1f) Color(0xFF4CAF50) else Color(0xFF3949AB))
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "$aguaBebidaHoy / $metaDiaria ml",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.DarkGray))
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                LinearProgressIndicator(
                    progress = progresoFloat,
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                    color = ColorProvider(if (progresoFloat >= 1f) Color(0xFF4CAF50) else Color(0xFF5C6BC0)),
                    backgroundColor = ColorProvider(Color(0xFFCFD8DC))
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                Box(
                    modifier = GlanceModifier
                        .size(64.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(Color(0xFFE0E0E0)))
                        .clickable(onClick = actionRunCallback<SumarAguaAction>(actionParametersOf(cantidadKey to 250)))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_vaso),
                        contentDescription = context.getString(R.string.widget_content_desc_vaso),
                        modifier = GlanceModifier.size(36.dp)
                    )
                }
            }
        }
    }
}
