# 💧 Water Reminder

Aplicación Android para registrar el consumo de agua diario, visualizar el histórico y recibir recordatorios periódicos para mantenerte hidratado. Incluye un widget interactivo 2x2 para añadir agua sin abrir la app.

[![Android CI](https://github.com/HugoBlancoo/WaterReminder/actions/workflows/android-ci.yml/badge.svg)](https://github.com/HugoBlancoo/WaterReminder/actions/workflows/android-ci.yml)
[![Release](https://img.shields.io/github/v/tag/HugoBlancoo/WaterReminder?label=release&sort=semver)](https://github.com/HugoBlancoo/WaterReminder/tags)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/API-24%2B-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![License: MIT](https://img.shields.io/github/license/HugoBlancoo/WaterReminder)](LICENSE)
[![Last commit](https://img.shields.io/github/last-commit/HugoBlancoo/WaterReminder)](https://github.com/HugoBlancoo/WaterReminder/commits)

## Funcionalidades

- **Hoy**: progreso circular animado hacia la meta diaria, con registro rápido de 180/250/500 ml.
- **Historial**: gráfico de barras por rango (1 semana, 2 semanas, 1 mes, 6 meses, 1 año) y estadísticas del periodo (días logrados, promedio, consumo total).
- **Ajustes**: meta diaria configurable, frecuencia de recordatorios y selector de tema (sistema / claro / oscuro).
- **Widget 2x2** (Jetpack Glance): progreso del día y botón de acceso rápido para sumar un vaso de agua, sincronizado en tiempo real con la app.
- **Recordatorios en segundo plano**: notificación periódica si no se registra agua en el intervalo configurado, reprogramada automáticamente vía WorkManager.
- **Persistencia** con DataStore Preferences y **modo claro/oscuro** dinámico según el sistema o preferencia manual.

## Arquitectura

Proyecto modularizado en capas siguiendo MVVM y separación de responsabilidades:

```
com.example.waterreminder/
├── MainActivity.kt          # Entry point: Activity + Scaffold/NavHost
├── data/                    # DataStore, claves de preferencias, modelos y constantes
├── viewmodel/                # WaterViewModel: estado y lógica de negocio
├── ui/
│   ├── screens/              # PantallaHoy, PantallaHistorial, PantallaConfig
│   ├── components/           # Componentes reutilizables (TarjetaEstadistica...)
│   └── theme/                 # Tema Material 3 (claro/oscuro)
├── worker/                   # RecordatorioWorker (WorkManager)
└── widget/                   # WaterWidget, WaterWidgetReceiver y acciones (Glance)
```

## Stack técnico

| | |
|---|---|
| Lenguaje | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Widget | Jetpack Glance |
| Persistencia | DataStore Preferences |
| Background | WorkManager |
| Navegación | Navigation Compose |
| Build | AGP 9.2.1 / Gradle 9.4.1 |
| minSdk / targetSdk | 24 / 36 |

## Requisitos

- Android Studio (Ladybug o superior)
- JDK 21
- SDK de Android con API 36 instalado

## Cómo ejecutarlo

```bash
git clone git@github.com:HugoBlancoo/WaterReminder.git
cd WaterReminder
./gradlew assembleDebug
```

O directamente desde Android Studio: `Open` → seleccionar la carpeta del proyecto → `Run`.

Para añadir el widget a la pantalla de inicio: mantener pulsado en el launcher → *Widgets* → **Water Reminder**.

## Calidad y CI

El workflow de [GitHub Actions](.github/workflows/android-ci.yml) corre en cada push/PR contra `master`:

- `lintDebug` (con [`lint-baseline.xml`](app/lint-baseline.xml) para no bloquear por deuda técnica preexistente, solo por regresiones nuevas)
- `testDebugUnitTest`
- `assembleDebug`

Para ejecutarlo en local:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Licencia

Distribuido bajo licencia [MIT](LICENSE).
