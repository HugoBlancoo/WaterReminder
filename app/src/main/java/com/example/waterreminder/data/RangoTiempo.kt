package com.example.waterreminder.data

enum class RangoTiempo(val dias: Int, val etiqueta: String) {
    SEMANA(7, "1 Sem"),
    DOS_SEMANAS(14, "2 Sem"),
    MES(30, "1 Mes"),
    SEIS_MESES(180, "6 Mes"),
    ANO(365, "1 Año")
}
