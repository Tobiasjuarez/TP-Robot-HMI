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
| 12 | Con batería crítica, en un viaje, el robot se detiene junto a la persona y pide ayuda (no vuelve solo) | Un servicio de guía no puede dejar sola a una persona que no ve |
| 13 | No se puede reanudar con batería crítica; sí volver al Punto de encuentro | Evitar quedarse sin batería a mitad del recorrido |
| 14 | El operador ve el "último aviso por voz" | Saber qué escuchó la persona antes de decidir (HRI visible en la HMI) |
| 15 | El historial guarda viajes y eventos, nunca quién es la persona | La discapacidad es un dato sensible |
| 16 | Misma configuración de Gradle que Rodando (Kotlin plugin, KSP 2.3.0, Room 2.7.2) | Ya probada en nuestras máquinas; KSP no funciona con el Kotlin integrado de AGP 9 |

## Limitaciones

- La "detención" es un **comando de software simulado**: no reemplaza una parada de emergencia física ni está validada como función de seguridad.
- El simulador no modela física, obstáculos reales ni la navegación del robot: la posición se interpola entre puntos.
- Una orden que vence por timeout no se reenvía ni se ejecuta después (en un sistema real podría llegar tarde).
- Un solo robot simulado y un solo operador; no hay roles ni permisos.
- El contrato JSON es una propuesta del grupo, **no** el protocolo oficial de Unitree.
- El asa con sensor y la voz bidireccional requieren equipamiento adicional sobre el Go2.
- Un Go2 no puede usar los botones del ascensor: requiere integración con el edificio o personal.
- El plano sale del Mapa UADE, pero la ubicación de ascensores y pasillos interiores es aproximada; los avisos por voz son textos, no audio.
- Cualquier prueba con personas reales requeriría entorno controlado y supervisión permanente.

### Observaciones de las pruebas (19/11/2026)

- **Una orden "Sin respuesta" puede haberse ejecutado:** en la desconexión, la detención quedó "Sin respuesta" y al volver la conexión el robot estaba detenido. Confirma el criterio de la app: verificar el estado antes de reintentar.
- **Reanudar exige reconocer todas las alertas críticas:** si quedan críticas de un escenario anterior, Reanudar sigue bloqueado. Es intencional, pero el motivo podría indicar cuántas quedan.
- **El escenario propio se puede disparar sin misión:** la herramienta de Simulación lo acepta sin viaje en curso y queda un aviso por voz sin persona. Conviene habilitarlo solo con una misión activa.
- **"Pendiente" dura menos de 1 s con buena conexión:** por la latencia simulada (0,4 a 1,2 s); se documenta en la desconexión (captura 11a).
