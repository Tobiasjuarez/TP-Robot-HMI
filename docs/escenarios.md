# Escenarios de prueba (para la demo y el video)

Todos se reproducen desde la app, con la pestaña **Simulación**. Para cada uno, guardar capturas o un clip como evidencia.

## 1. Misión normal: guiar al Aula 225

1. Conectar al robot → Panel → **Iniciar** "Guiar al Aula 225" → confirmar.
2. Ver la orden **Pendiente → Confirmada** en "Últimas órdenes".
3. Ver el progreso (punto X de N, edificio y piso), la distancia al destino que baja, el asa "Sujetada" y los avisos por voz en cada punto (Hall de Lima 1, paso a Lima 2, ascensores, 2.º piso).
4. Al llegar: "Llegamos al Aula 225", estado **Disponible** y "Misión completada" en Historial.

**Evidencia:** inicio, el robot en el ascensor, la llegada y el Historial. Capturas 01 a 07 en [`evidencias/`](evidencias/).

## 2. Batería baja

1. Con un viaje en curso → Simulación → **Batería 18 %**.
2. Aparece la advertencia y el Panel recomienda volver al Punto de encuentro.
3. Variante crítica: **Batería 8 %** → el robot **se detiene junto a la persona** (no se vuelve solo), alerta crítica "Enviá a alguien a acompañarla". **Reanudar** queda deshabilitado; **Volver a punto seguro** sigue disponible (vuelven juntos).

**Evidencia:** advertencia, robot detenido con la persona, acción disponible. Capturas 08a a 09b en [`evidencias/`](evidencias/).

## 3. Desconexión

1. Con un viaje en curso → Simulación → **Cortar conexión 15 s** (por ejemplo, dentro del ascensor).
2. A los 3 s aparece "Sin datos nuevos hace N s", las tarjetas se atenúan y el indicador pasa a "Sin datos".
3. **Pausar** queda deshabilitado; **Solicitar detención** se puede enviar y queda **Pendiente** → **Sin respuesta** a los 6 s.
4. Al volver la conexión: telemetría al día, alerta de desconexión y registro en Historial.

**Evidencia:** banner de datos viejos, orden sin respuesta, recuperación. Capturas 10a a 12 en [`evidencias/`](evidencias/).

## 4. Incidente: paso bloqueado

1. Con un viaje en curso → Simulación → **Generar obstáculo** (por ejemplo, el paso de Lima 1 a Lima 2 lleno de gente en el cambio de turno).
2. Estado **Requiere intervención** y alerta crítica "Paso bloqueado…".
3. **Reanudar** está deshabilitado hasta **reconocer** la alerta.
4. El operador decide: **Reanudar** (el robot replanifica), **Volver al Punto de encuentro** o **Cancelar**.

**Evidencia:** alerta, botón bloqueado con su motivo, decisión y resultado. Capturas 13 a 14b en [`evidencias/`](evidencias/).

## 5. Escenario propio: la persona suelta el asa

1. Con un viaje en curso → Simulación → **Disparar escenario propio**.
2. El asa pasa a **Suelta** (en rojo), el robot se detiene y dice "Soltaste el asa. Me quedo quieto acá…".
3. El operador reconoce la alerta y decide: **Reanudar** cuando la persona vuelve a tomarla (el asa vuelve a "Sujetada"), mandar a alguien o **Cancelar**.

**Evidencia:** asa suelta, aviso por voz, decisión del operador. Capturas 15 y 16 en [`evidencias/`](evidencias/).

## Extra: rechazo

Simulación → **Rechazar próxima orden** → mandar cualquier orden → vuelve **Rechazada** con el motivo. Capturas 17a y 17b en [`evidencias/`](evidencias/).
