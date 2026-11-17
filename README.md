# Guía UADE — TP DAI (UADE)

App Android para operar y supervisar un **Unitree Go2 simulado que guía a personas ciegas o con baja visión** dentro del Campus UADE Monserrat.
Trabajo Práctico grupal de Desarrollo de Aplicaciones I (Interfaz HMI/HRI para robots de servicio).

- Caso de uso, actores, justificación y riesgos: [`docs/caso-de-uso.md`](docs/caso-de-uso.md)
- Plan de desarrollo y forma de trabajo: [`docs/PLAN.md`](docs/PLAN.md)
- Diseño en Figma (prototipo navegable, flujo normal y con falla, mapa de navegación): https://www.figma.com/design/4jqfkUGmYIYXSamN4MWbRV

## Cómo correrlo

1. Clonar el repo y abrir la carpeta en **Android Studio** (*File → Open*).
2. Esperar el *Gradle Sync* y correr en un emulador o un celular (minSdk 26).
3. No necesita robot, servidor ni internet.

### Si aparece "Cannot find a Java installation … languageVersion=25"

Al abrir el proyecto, Android Studio crea `gradle/gradle-daemon-jvm.properties` pidiendo un Java 25 que Gradle no encuentra. Igual que en Rodando:

1. **Settings › Build, Execution, Deployment › Build Tools › Gradle › Gradle JDK** → elegir **jbr-25** (el Java que trae Android Studio).
2. Borrar `gradle/gradle-daemon-jvm.properties` (no se sube al repo) y volver a sincronizar.

El aviso "Gradle 9.5.0 is not the latest minor version" se puede ignorar: usamos la misma versión que Rodando.

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
│       ├── ServiceProfile.kt             # Interfaz del caso de uso
│       └── GuiaUadeProfile.kt            # ⇐ Guía UADE: recorridos, variables, escenario propio
├── ui/
│   ├── RobotViewModel.kt        # RobotUiState (StateFlow) + eventos
│   ├── components/              # Componentes reutilizables (StatusPill, MetricTile, ...)
│   ├── screens/                 # Selección, Panel, Alertas, Historial, Simulación
│   └── theme/
├── RobotHmiApplication.kt       # AppContainer (inyección manual)
└── MainActivity.kt
```

Más detalle en [`docs/arquitectura.md`](docs/arquitectura.md).

## El caso de uso en el código

Todo lo específico del servicio está en `data/sim/GuiaUadeProfile.kt`:

- `base`: el **Punto de encuentro** (entrada peatonal de Lima), adonde vuelve el robot
- `missions`: viajes al Aula 225 (Lima 2, 2.º piso), a UADE Labs y a la parada de autos de Av. Independencia, sobre el plano del Mapa UADE ([esquema](docs/recorrido-campus.svg))
- `metrics()`: **asa**, **distancia al destino** y **gente en el pasillo**, más el último aviso por voz
- `customScenario()`: **la persona suelta el asa** (escenario 5)
- `autoReturnOnCriticalBattery = false`: con batería crítica el robot no deja sola a la persona

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

## Cómo trabajamos en el repo

Igual que en Rodando (detalle en [`docs/PLAN.md`](docs/PLAN.md)):

- `main` siempre compila. Una rama por etapa: `feature/<algo>`.
- Commits chicos con prefijo: `feat:`, `fix:`, `test:`, `docs:`, `chore:`.
- Pull request con la plantilla (qué cambia, punto de la consigna, cómo se probó, uso de IA); lo revisa otro integrante.
- Tests de reglas: `./gradlew test`.
