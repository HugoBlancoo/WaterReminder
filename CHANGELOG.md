# Changelog

Todas las entradas relevantes de este proyecto se documentan en este archivo, generado automáticamente por [Commitizen](https://commitizen-tools.github.io/commitizen/) a partir de [Conventional Commits](https://www.conventionalcommits.org/) cada vez que se mergea a `main`.

El historial previo a la adopción de este flujo (incluida la modularización del proyecto) no sigue Conventional Commits y no aparece aquí; puede consultarse en el log de git.

## v1.3.0 (2026-08-24)

### Feat

- allow logging water for past days from the history screen

## v1.2.0 (2026-08-21)

### Feat

- automate semantic versioning and changelog with Commitizen

### Fix

- prevent notification spam loop from invalid reminder interval
- prevent daily progress from being wiped on app reopen
- enable core library desugaring so java.time works on minSdk 24
