# Evidencias de los escenarios

Capturas del emulador de Android Studio tomadas por Lucas Casas el 19/11/2026, con la app compilada desde `main`. Todos los escenarios se dispararon desde la pestaña **Simulación**. Las mismas capturas, con su explicación, están en el **Anexo A** de [`../entrega-final.pdf`](../entrega-final.pdf).

| # | Escenario | Captura | Archivo |
| --- | --- | --- | --- |
| 01 | 1 · Misión normal | Selección del robot simulado | [`01-seleccion-robot.png`](01-seleccion-robot.png) |
| 02 | 1 · Misión normal | Panel en Disponible: telemetría y variables del servicio | [`02-panel-disponible.png`](02-panel-disponible.png) |
| 02b | 1 · Misión normal | Sin misión: órdenes deshabilitadas con su motivo | [`02b-misiones-y-control.png`](02b-misiones-y-control.png) |
| 03 | 1 · Misión normal | Confirmar misión "Guiar al Aula 225" | [`03-confirmar-mision.png`](03-confirmar-mision.png) |
| 05 | 1 · Misión normal | En curso en los ascensores; orden **Confirmada** | [`05-mision-en-curso-confirmada.png`](05-mision-en-curso-confirmada.png) |
| 06 | 1 · Misión normal | Llegada: aviso por voz y misión completada | [`06-llegada-aula-225.png`](06-llegada-aula-225.png) |
| 07 | 1 · Misión normal | Historial persistido en Room | [`07-historial.png`](07-historial.png) |
| 08a | 2 · Batería baja | Advertencia de batería baja | [`08a-bateria-baja-advertencia.png`](08a-bateria-baja-advertencia.png) |
| 08b | 2 · Batería baja | Iniciar bloqueado (mínimo 25 %) y volver recomendado | [`08b-bateria-baja-accion-disponible.png`](08b-bateria-baja-accion-disponible.png) |
| 08c | 2 · Batería baja | Confirmación antes de volver a punto seguro | [`08c-confirmar-volver-punto-seguro.png`](08c-confirmar-volver-punto-seguro.png) |
| 09a | 2 · Batería baja | 8 % en viaje: Requiere intervención, asa sujetada | [`09a-bateria-critica-requiere-intervencion.png`](09a-bateria-critica-requiere-intervencion.png) |
| 09b | 2 · Batería baja | Reanudar bloqueado por batería crítica | [`09b-bateria-critica-reanudar-bloqueado.png`](09b-bateria-critica-reanudar-bloqueado.png) |
| 10a | 3 · Desconexión | Pestaña Simulación: se corta el enlace 15 s | [`10a-simulacion-cortar-conexion.png`](10a-simulacion-cortar-conexion.png) |
| 10b | 3 · Desconexión | Banner de datos viejos y datos atenuados | [`10b-desconexion-datos-viejos.png`](10b-desconexion-datos-viejos.png) |
| 11a | 3 · Desconexión | Detención **Pendiente**; resto de órdenes deshabilitadas | [`11a-orden-pendiente-sin-conexion.png`](11a-orden-pendiente-sin-conexion.png) |
| 11b | 3 · Desconexión | Detención **Sin respuesta** a los 6 s | [`11b-orden-sin-respuesta.png`](11b-orden-sin-respuesta.png) |
| 12 | 3 · Desconexión | Conexión recuperada | [`12-conexion-recuperada.png`](12-conexion-recuperada.png) |
| 13 | 4 · Incidente | Paso bloqueado: Requiere intervención | [`13-incidente-requiere-intervencion.png`](13-incidente-requiere-intervencion.png) |
| 14a | 4 · Incidente | Reanudar bloqueado hasta reconocer la alerta | [`14a-incidente-reanudar-bloqueado.png`](14a-incidente-reanudar-bloqueado.png) |
| 14b | 4 · Incidente | Alerta crítica reconocida | [`14b-incidente-alerta-reconocida.png`](14b-incidente-alerta-reconocida.png) |
| 15 | 5 · Escenario propio | La persona suelta el asa (asa en rojo, aviso por voz) | [`15-asa-suelta.png`](15-asa-suelta.png) |
| 16 | 5 · Escenario propio | Alerta crítica con Reconocer | [`16-alerta-asa-reconocer.png`](16-alerta-asa-reconocer.png) |
| 17a | Extra | Aviso de orden **Rechazada** | [`17a-orden-rechazada-aviso.png`](17a-orden-rechazada-aviso.png) |
| 17b | Extra | Orden **Rechazada** con su motivo | [`17b-orden-rechazada-motivo.png`](17b-orden-rechazada-motivo.png) |

La captura 04 (orden **Pendiente** con conexión normal) no se incluye: con buena conexión el estado dura menos de 1 s. El estado Pendiente queda documentado en la 11a, durante la desconexión.
