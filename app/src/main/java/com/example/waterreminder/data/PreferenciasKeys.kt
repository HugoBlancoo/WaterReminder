package com.example.waterreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes_agua")

val CLAVE_META = intPreferencesKey("meta_diaria")
val CLAVE_HISTORIAL = stringPreferencesKey("historial_completo")
val CLAVE_TEMA = intPreferencesKey("modo_tema")
val CLAVE_INTERVALO = intPreferencesKey("intervalo_notificacion")
