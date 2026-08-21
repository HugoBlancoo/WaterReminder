package com.example.waterreminder.data

fun deserializarHistorial(texto: String): Map<Long, Int> =
    if (texto.isEmpty()) {
        emptyMap()
    } else {
        texto.split(",").associate { it.split(":")[0].toLong() to it.split(":")[1].toInt() }
    }

fun serializarHistorial(mapa: Map<Long, Int>): String =
    mapa.entries.joinToString(",") { "${it.key}:${it.value}" }
