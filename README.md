# Guía UADE

App Android para operar y supervisar un **Unitree Go2 que guía a personas ciegas o con baja visión** dentro del Campus UADE Monserrat. La persona va tomada de un asa y el robot le avisa por voz giros, puertas y ascensores; un operador sigue cada viaje desde la app y decide qué hacer ante un incidente. El robot es simulado: la app funciona sin robot, servidor ni internet.

Trabajo Práctico grupal — Desarrollo de Aplicaciones I (UADE), 2.º cuatrimestre 2026: *Interfaz HMI/HRI para robots de servicio*.

## Equipo

| Integrante | Rol principal |
| --- | --- |
| Tobías Juárez | Arquitectura, simulador e integración |
| _(completar)_ | _(completar)_ |
| _(completar)_ | _(completar)_ |

## Qué pide la consigna y cómo lo cubrimos

- **Caso de uso:** guiar a una persona con baja visión desde la entrada de Lima hasta el Aula 225 (Lima 2, 2.º piso), UADE Labs o la parada de autos de Av. Independencia. Por qué un robot, por qué Go2 y qué requiere equipamiento extra: [`docs/caso-de-uso.md`](docs/caso-de-uso.md).
- **Telemetría:** conectividad, batería, estado, misión y progreso, ubicación, movimiento, alertas y última actualización, más tres variables del servicio: **asa** (sujetada / suelta), **distancia al destino** y **gente en el pasillo**. Extra: el último aviso por voz que escuchó la persona.
- **Comandos:** iniciar, pausar, reanudar y cancelar misión, volver a punto seguro, solicitar detención y reconocer alertas, cada uno con sus condiciones de habilitación.
- **Cinco escenarios:** misión normal, batería baja, desconexión, incidente (paso bloqueado) y escenario propio (la persona suelta el asa), todos disparables desde la pestaña Simulación.

### Estados de una orden

| Estado | Significa |
| --- | --- |
| **Pendiente** | Se envió y la app espera la respuesta del robot. |
| **Confirmada** | El robot la aceptó. No significa que la tarea ya terminó. |
| **Rechazada** | El robot no la aceptó; la app muestra el motivo. |
| **Sin respuesta** | Pasaron 6 s sin respuesta. No significa que no se ejecutó: hay que verificar el estado antes de reintentar. |

La detención es un comando de software simulado: **no reemplaza una parada de emergencia física**.

## Tecnologías

Kotlin · Jetpack Compose + Material 3 · ViewModel + Coroutines + StateFlow · Room · JUnit.

## Arquitectura

MVVM con Repository. La UI observa un único estado y manda eventos; nunca toca el simulador.

```
UI (Compose) → RobotViewModel (StateFlow) → RobotRepository → Robot simulado + Room
```

```
app/src/main/java/ar/edu/uade/robothmi/
├── domain/
│   ├── model/         # Telemetry, MissionProgress, CommandRecord, RobotAlert, Waypoint…
│   └── CommandRules   # Qué orden se habilita en cada momento (Kotlin puro, con tests)
├── data/
│   ├── RobotRepository        # Interfaz que usa el ViewModel + controles de simulación
│   ├── local/                 # Room: historial de misiones y eventos (event_log)
│   └── sim/
│       ├── SimulatedRobotRepository  # Robot simulado: tick de 1 s, misiones, órdenes, fallas
│       ├── ServiceProfile            # Interfaz del caso de uso
│       └── GuiaUadeProfile           # Guía UADE: recorridos, variables, avisos por voz, escenario propio
├── ui/
│   ├── RobotViewModel         # RobotUiState + eventos
│   ├── components/            # Estado, tarjeta de telemetría, aviso de datos viejos…
│   ├── screens/               # Selección, Panel, Alertas, Historial, Simulación
│   └── theme/
├── RobotHmiApplication        # AppContainer (inyección de dependencias manual)
└── MainActivity
```

Diagrama y estados en [`docs/arquitectura.md`](docs/arquitectura.md); contrato de mensajes propuesto en [`docs/contrato-json.md`](docs/contrato-json.md).

## Cómo ejecutar

1. Abrir la carpeta del proyecto en Android Studio y esperar el *Gradle Sync*.
2. Elegir un emulador (Android 8.0 / API 26 o superior) y tocar **Run**.
3. En la app: **Conectar** → en el Panel, **Iniciar** "Guiar al Aula 225" → confirmar.
4. Tests unitarios: clic derecho en `app/src/test` → **Run Tests**, o `./gradlew test`.

**Probar las fallas:** la pestaña **Simulación** fuerza cada escenario: batería al 18 % u 8 %, cortar la conexión 15 s, generar un obstáculo, disparar el escenario propio (la persona suelta el asa) y rechazar la próxima orden. El paso a paso de cada uno está en [`docs/escenarios.md`](docs/escenarios.md).

**Si la batería queda baja:** "Volver a punto seguro" lleva al robot al Punto de encuentro, donde carga solo.

**Si Android Studio pide un Java 25:** en *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* elegir **jbr-25** y borrar `gradle/gradle-daemon-jvm.properties` (lo crea Android Studio). El aviso "Gradle 9.5.0 is not the latest minor version" se puede ignorar: es la misma versión que en Rodando.

## Forma de trabajo

- **Ramas:** `main` siempre compila. Cada tarea va en su rama: `feature/<algo>`, `fix/<problema>`, `docs/<tema>`.
- **Commits:** chicos y frecuentes, con prefijo: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- **Pull requests:** con la plantilla de `.github/pull_request_template.md` (qué cambia, punto de la consigna, cómo se probó, uso de IA). Al menos un compañero revisa antes de mergear.

## Documentación

Plan de desarrollo con todas las etapas, decisiones, cómo probar todo y pendientes: [`docs/PLAN.md`](docs/PLAN.md). Caso de uso, diseño en Figma, arquitectura, escenarios y evidencias: ver [`docs/`](docs/).
