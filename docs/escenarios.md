# Escenarios de prueba (para la demo y el video)

Todos se reproducen desde la app, con la pestaña **Simulación**. Para cada uno, guardar capturas o un clip como evidencia.

## 1. Misión normal

1. Conectar al robot → Panel → **Iniciar** "Solo tableros eléctricos" (es la más corta) → confirmar.
2. Ver la orden **Pendiente → Confirmada** en "Últimas órdenes".
3. Ver el progreso (punto X de N, barra, etapa) y las variables del servicio actualizándose.
4. Esperar a que termine: estado **Disponible** y "Misión completada" en Historial.

**Evidencia:** inicio, progreso a mitad, finalización y el Historial.

## 2. Batería baja

1. Con una misión en curso → Simulación → **Batería 18 %**.
2. Aparece la alerta de advertencia, el Panel muestra "Batería baja: considerá volver a punto seguro" y "Volver a punto seguro" queda marcado como recomendado.
3. Ordenar **Volver a punto seguro** (pide confirmación) → el robot vuelve y empieza a cargar.
4. Variante: **Batería 8 %** → el robot inicia solo el regreso (si no estaba detenido por el operador).

**Evidencia:** alerta, acción recomendada, llegada a base cargando.

## 3. Desconexión

1. Con una misión en curso → Simulación → **Cortar conexión 15 s**.
2. A los 3 s aparece el banner "Sin datos nuevos hace N s", las tarjetas se atenúan y el indicador pasa a "Sin datos".
3. Probar **Pausar**: está deshabilitado ("Sin conexión"). Probar **Solicitar detención**: se envía y queda **Pendiente**; si pasan 6 s → **Sin respuesta**.
4. Al volver la conexión: la telemetría se actualiza, llega la alerta de desconexión y el Historial registra el corte.

**Evidencia:** banner de datos viejos, orden sin respuesta, recuperación.

## 4. Incidente (obstáculo)

1. Con una misión en curso → Simulación → **Generar obstáculo**.
2. Estado **Requiere intervención** y alerta crítica "Paso bloqueado...".
3. **Reanudar** está deshabilitado hasta **reconocer** la alerta (pestaña Alertas).
4. El operador decide: **Reanudar** (el robot replanifica), **Volver a punto seguro** o **Cancelar**.

**Evidencia:** alerta, botón bloqueado con su motivo, decisión y resultado.

## 5. Escenario propio

Depende del caso de uso (`ServiceProfile.customScenario()`). En el ejemplo de inspección:
**Temperatura crítica en un tablero** → alerta crítica, el robot se frena y el operador decide si sigue la ronda.

## Extra: rechazo

Simulación → **Rechazar próxima orden** → mandar cualquier orden → vuelve **Rechazada** con el motivo.
