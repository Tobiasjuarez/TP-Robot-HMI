# Registro de decisiones y limitaciones

## Decisiones

| # | Decisión | Por qué |
|---|---|---|
| 1 | `RobotRepository` como interfaz; el simulador es una implementación | Desacoplar la UI del robot (pedido por la consigna) y poder cambiar a un robot real sin tocar la UI |
| 2 | Room para el historial de eventos | Persistencia local estructurada, consultas con Flow, visto en la cursada |
| 3 | Las reglas de habilitación viven en `CommandRules` (función pura) | Una sola fuente de verdad para la UI, testeable sin Android |
| 4 | El robot simulado valida de nuevo cada orden | La app puede tener datos viejos; el robot es la fuente de verdad |
| 5 | Datos "viejos" si pasan más de 3 s sin telemetría | Con 1 telemetría/s, 3 s sin datos indica un problema real sin dar falsas alarmas |
| 6 | Timeout de órdenes: 6 s → "Sin respuesta" | Evita órdenes pendientes eternas; "sin respuesta" no implica "no ejecutada" |
| 7 | Sin conexión solo se permiten Detención y Volver a punto seguro | Son órdenes de seguridad: mejor intentarlas que bloquearlas; las demás no se podrían confirmar |
| 8 | Cancelar y Volver a punto seguro piden confirmación; la Detención no | Prevenir órdenes accidentales de alto impacto sin demorar la de seguridad |
| 9 | Reanudar tras un incidente exige reconocer la alerta crítica | Obliga a que el operador lea qué pasó antes de decidir |
| 10 | Con batería crítica el robot vuelve solo, salvo que el operador lo haya detenido o espere una decisión | No mover el robot en contra de una orden explícita del operador |
| 11 | Estados siempre con texto + símbolo + color | Accesibilidad: no depender solo del color |

## Limitaciones

- La "detención" es un **comando de software simulado**: no reemplaza una parada de emergencia física ni está validada como función de seguridad.
- El simulador no modela física, obstáculos reales ni la navegación del robot: la posición se interpola entre puntos.
- Una orden que vence por timeout no se reenvía ni se ejecuta después (en un sistema real podría llegar tarde).
- Un solo robot simulado y un solo operador; no hay roles ni permisos.
- El contrato JSON es una propuesta del grupo, **no** el protocolo oficial de Unitree.
- Sensores específicos del servicio (p. ej. cámara térmica) requerirían equipamiento adicional sobre el robot base.

_(Completar con las decisiones del grupo.)_
