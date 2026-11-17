# Arquitectura

## Capas

```mermaid
flowchart LR
    UI["UI (Compose)<br/>Panel · Alertas · Historial · Simulación"]
    VM["RobotViewModel<br/>StateFlow&lt;RobotUiState&gt;"]
    RULES["CommandRules<br/>(habilitación)"]
    REPO[["RobotRepository<br/>(interfaz)"]]
    SIMC[["SimulationControls<br/>(solo demo)"]]
    SIM["SimulatedRobotRepository"]
    PROF["GuiaUadeProfile<br/>(caso de uso)"]
    DAO["EventLogDao"]
    DB[("Room<br/>event_log")]

    UI -- eventos --> VM
    VM -- estado --> UI
    VM --> RULES
    VM --> REPO
    VM --> SIMC
    SIM -. implementa .-> REPO
    SIM -. implementa .-> SIMC
    SIM --> PROF
    SIM --> DAO --> DB
```

- La **UI** solo observa `RobotUiState` y llama funciones del ViewModel. No conoce al simulador.
- El **ViewModel** combina telemetría, órdenes, alertas y un reloj (para detectar datos viejos) y calcula qué comandos están habilitados con `CommandRules`.
- El **Repository** es una interfaz. Hoy la implementa el simulador; un robot real tendría otra implementación con la misma interfaz.
- El **simulador** valida de nuevo cada orden (el robot es la fuente de verdad: la app puede tener datos viejos).

## Estados del robot

```mermaid
stateDiagram-v2
    [*] --> DISPONIBLE
    DISPONIBLE --> EN_MISION: Iniciar misión
    CARGANDO --> EN_MISION: Iniciar misión
    EN_MISION --> PAUSADO: Pausar
    PAUSADO --> EN_MISION: Reanudar
    EN_MISION --> REQUIERE_AYUDA: Obstáculo / evento del servicio
    REQUIERE_AYUDA --> EN_MISION: Reconocer alerta + Reanudar
    EN_MISION --> DETENIDO: Solicitar detención
    PAUSADO --> DETENIDO: Solicitar detención
    REQUIERE_AYUDA --> DETENIDO: Solicitar detención
    DETENIDO --> EN_MISION: Reanudar
    EN_MISION --> DISPONIBLE: Misión completada
    EN_MISION --> DISPONIBLE: Cancelar
    PAUSADO --> DISPONIBLE: Cancelar
    DETENIDO --> DISPONIBLE: Cancelar
    DISPONIBLE --> VOLVIENDO_A_BASE: Volver a punto seguro
    EN_MISION --> VOLVIENDO_A_BASE: Volver a punto seguro / batería crítica
    PAUSADO --> VOLVIENDO_A_BASE: Volver a punto seguro
    DETENIDO --> VOLVIENDO_A_BASE: Volver a punto seguro
    REQUIERE_AYUDA --> VOLVIENDO_A_BASE: Volver a punto seguro
    VOLVIENDO_A_BASE --> DETENIDO: Solicitar detención
    VOLVIENDO_A_BASE --> CARGANDO: Llega a base
    CARGANDO --> DISPONIBLE: Carga completa
```

## Ciclo de vida de una orden

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: Operador envía
    PENDIENTE --> CONFIRMADA: El robot acepta
    PENDIENTE --> RECHAZADA: El robot rechaza (estado inválido, falla forzada)
    PENDIENTE --> SIN_RESPUESTA: Timeout 6 s (p. ej. sin conexión)
```

"Confirmada" significa que el robot **aceptó** la orden, no que la tarea ya se completó.
"Sin respuesta" **no** significa que no se ejecutó: el operador tiene que verificar el estado.
