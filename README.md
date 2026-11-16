# Robot HMI — TP DAI (UADE)

App Android de operación y supervisión para un robot de servicio Unitree (Go2 / G1) **simulado**.
Trabajo Práctico grupal de Desarrollo de Aplicaciones I.

> Estado: **repo base**. La arquitectura, el simulador, los comandos y el historial ya funcionan.
> El caso de uso de ejemplo (inspección de planta con Go2) se cambia en un solo archivo.

## Cómo correrlo

1. Clonar el repo y abrir la carpeta en **Android Studio** (*File → Open*).
2. Esperar el *Gradle Sync* y correr en un emulador o un celular (minSdk 24).
3. No necesita robot, servidor ni internet.

**Si el sync falla por KSP:** en `gradle/libs.versions.toml`, cambiar `ksp = "..."` por la versión que
sugiera Android Studio y volver a sincronizar. Las versiones de Kotlin, KSP, Room y AGP tienen que ser compatibles entre sí.

## Stack

Kotlin · Jetpack Compose (Material 3) · MVVM · ViewModel + StateFlow · Repository · Room · Coroutines.

## Estructura

```
app/src/main/java/ar/edu/uade/robothmi/
├── domain/
│   ├── model/Models.kt          # Telemetry, MissionProgress, CommandRecord, RobotAlert, ...
│   └── CommandRules.kt          # Condiciones de habilitación de cada comando (función pura, testeada)
├── data/
│   ├── RobotRepository.kt       # Interfaz que usa el ViewModel + SimulationControls (demo)
│   ├── local/                   # Room: EventLogEntity, EventLogDao, AppDatabase
│   └── sim/
│       ├── SimulatedRobotRepository.kt   # Robot simulado: tick, misiones, comandos, fallas
│       └── ServiceProfile.kt             # ⇐ TODO lo específico del caso de uso
├── ui/
│   ├── RobotViewModel.kt        # RobotUiState (StateFlow) + eventos
│   ├── components/              # Componentes reutilizables (StatusPill, MetricTile, ...)
│   ├── screens/                 # Selección, Panel, Alertas, Historial, Simulación
│   └── theme/
├── RobotHmiApplication.kt       # AppContainer (inyección manual)
└── MainActivity.kt
```

Más detalle en [`docs/arquitectura.md`](docs/arquitectura.md).

## Adaptarlo a nuestro caso de uso

Todo lo específico del servicio está en `data/sim/ServiceProfile.kt`:

- `base`: punto seguro / base de carga
- `missions`: recorridos (waypoints con zona y coordenadas)
- `metrics()`: **las 3 variables del servicio** que pide la consigna
- `onWaypointReached()`: qué pasa en cada punto (lecturas, hallazgos)
- `customScenario()`: el **escenario propio** (escenario 5)

Para cambiar de caso: crear otra clase que implemente `ServiceProfile` y pasarla en `AppContainer`.

## Qué cubre de la consigna

| Requisito | Dónde |
|---|---|
| Telemetría: conectividad, batería, estado, misión, ubicación, movimiento, alertas, última actualización + 3 variables | `PanelScreen` / `Telemetry` |
| Comandos: iniciar, pausar, reanudar, cancelar, volver a punto seguro, detención, reconocer alertas | `CommandRules` + `SimulatedRobotRepository.execute` |
| Estado de cada orden: pendiente / confirmada / rechazada / sin respuesta | `CommandRecord`, "Últimas órdenes" en el Panel |
| UI observa estado y envía eventos, sin tocar el simulador | `RobotViewModel` + `RobotRepository` |
| Persistencia local del historial | Room (`event_log`), pestaña Historial |
| Forzar desconexiones y rechazos | Pestaña Simulación |
| Contrato JSON | [`docs/contrato-json.md`](docs/contrato-json.md) |
| 5 escenarios | [`docs/escenarios.md`](docs/escenarios.md) |
| Decisiones y limitaciones | [`docs/decisiones.md`](docs/decisiones.md) |

## Pendiente (TODO del grupo)

- [ ] Definir el caso de uso final y adaptar `ServiceProfile`
- [ ] Aplicar el diseño del Figma (colores, tipografía, componentes) en `ui/theme` y `ui/components`
- [ ] Vista de mapa / esquema del recorrido (opcional: Canvas con los waypoints)
- [ ] Diagrama de arquitectura final (exportar el de `docs/arquitectura.md`)
- [ ] Capturas/video de los 5 escenarios
- [ ] Completar `docs/decisiones.md`

## Cómo trabajamos en el repo

- `main` siempre compila. Cada uno trabaja en una rama: `feature/<algo>` (ej. `feature/mapa`).
- Pull Request a `main`; lo revisa al menos otro integrante.
- No commitear `local.properties`, `build/` ni `.idea/` (ya están en `.gitignore`).
- Tests de reglas: `./gradlew test` (o botón ▶ sobre `CommandRulesTest`).
