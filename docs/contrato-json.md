# Contrato de mensajes (propuesto)

> **Aclaración:** este es un contrato **propuesto por el grupo** para desacoplar la app del robot.
> **No es el protocolo oficial de Unitree.** En la app los mensajes son objetos Kotlin; estos JSON
> muestran cómo se serializarían si hubiera un enlace real (por ejemplo, WebSocket).

## 1. Telemetría (robot → app, 1 vez por segundo)

```json
{
  "type": "telemetry",
  "robotId": "robot-01",
  "timestamp": "2026-11-20T21:14:03.120Z",
  "connectivity": { "status": "CONECTADO", "signalPct": 82 },
  "battery": { "pct": 64, "charging": false },
  "state": "EN_MISION",
  "mission": {
    "missionId": "aula-225",
    "status": "EN_CURSO",
    "currentWaypoint": 2,
    "totalWaypoints": 5,
    "stage": "Yendo a Ascensores de Lima 2"
  },
  "position": { "x": 53.5, "y": 28.5, "zone": "Lima 2 · Planta baja" },
  "motion": { "speedMps": 0.6 },
  "serviceMetrics": [
    { "id": "asa", "label": "Asa", "value": "Sujetada", "level": "NORMAL" },
    { "id": "distancia", "label": "Distancia al destino", "value": "15", "unit": "m", "level": "NORMAL" },
    { "id": "gente", "label": "Gente en el pasillo", "value": "Mucha", "level": "ADVERTENCIA" },
    { "id": "voz", "label": "Último aviso por voz", "value": "Giramos a la izquierda para pasar a Lima 2." }
  ]
}
```

## 2. Comando (app → robot)

```json
{
  "type": "command",
  "commandId": "a1b2c3d4",
  "robotId": "robot-01",
  "command": "PAUSAR",
  "target": null,
  "sentAt": "2026-11-20T21:14:05.000Z"
}
```

Valores de `command`: `INICIAR_MISION` (target = id de misión), `PAUSAR`, `REANUDAR`, `CANCELAR`,
`VOLVER_A_BASE`, `DETENCION_OPERATIVA`, `RECONOCER_ALERTA` (target = id de alerta).

## 3. Respuesta a comando (robot → app)

Aceptado:

```json
{
  "type": "command_ack",
  "commandId": "a1b2c3d4",
  "result": "CONFIRMADA",
  "message": "Robot en pausa. Puede reanudarse.",
  "respondedAt": "2026-11-20T21:14:05.850Z"
}
```

Rechazado:

```json
{
  "type": "command_ack",
  "commandId": "e5f6a7b8",
  "result": "RECHAZADA",
  "message": "Batería crítica: no se puede seguir la misión.",
  "respondedAt": "2026-11-20T21:15:10.400Z"
}
```

`SIN_RESPUESTA` **no viaja por la red**: lo asigna la app cuando pasan 6 s sin `command_ack`.

## 4. Alerta (robot → app)

```json
{
  "type": "alert",
  "alertId": "9f1c...",
  "robotId": "robot-01",
  "alertType": "OBSTACULO",
  "severity": "CRITICA",
  "message": "Paso bloqueado camino a Ascensores de Lima 2. El robot está quieto y espera tu decisión.",
  "createdAt": "2026-11-20T21:16:42.000Z"
}
```

## 5. Evento de misión (robot → app)

```json
{
  "type": "mission_event",
  "robotId": "robot-01",
  "missionId": "aula-225",
  "event": "WAYPOINT_REACHED",
  "detail": "Hall de Lima 1",
  "at": "2026-11-20T21:13:58.000Z"
}
```

Valores de `event`: `STARTED`, `WAYPOINT_REACHED`, `PAUSED`, `RESUMED`, `COMPLETED`, `CANCELLED`, `INTERRUPTED`.
