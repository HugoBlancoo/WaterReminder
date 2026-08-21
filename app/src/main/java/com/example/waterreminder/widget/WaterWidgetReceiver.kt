package com.example.waterreminder.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WaterWidget()
}
