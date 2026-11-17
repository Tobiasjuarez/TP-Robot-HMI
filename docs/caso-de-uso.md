# Caso de uso: guía para personas con baja visión en UADE

> Borrador para la preentrega (punto 1 y 2 de la consigna). Completar con el plano real de la sede y las fuentes técnicas que investiguemos.

## Problema

Una persona ciega o con baja visión que llega a una sede UADE (alumno nuevo, ingresante, visitante en una jornada) tiene que llegar a un aula, a Bedelía o a la Biblioteca en un edificio de varios pisos que no conoce, con pasillos que se llenan en los cambios de turno.

**Cómo se resuelve hoy:** pidiendo ayuda a otra persona (personal, bedeles, compañeros), memorizando el recorrido con práctica previa, con bastón y señalética, o con una app en el celular. Cuando no hay nadie disponible (horas pico, turno noche), la persona depende de la buena voluntad de quien pase.

## Servicio propuesto

Un **Unitree Go2** con un **asa** guía a la persona desde el **Punto de encuentro** (Hall de entrada) hasta el destino, avisándole por voz giros, puertas y ascensores. Un **operador** supervisa los viajes desde la app y decide qué hacer ante incidentes.

## Actores

| Actor | Rol | Necesidad | Implicación en la interfaz |
| --- | --- | --- | --- |
| Persona guiada | Usuaria principal (HRI) | Llegar sin ayuda, saber dónde está y qué pasa | Voz clara, asa; nunca depender de una pantalla |
| Operador | Supervisa los viajes (HMI) | Saber si la persona está bien y cuándo intervenir | Estado del asa, alertas, comandos con confirmación |
| Alumnos y personal en el pasillo | Comparten el espacio (HRI) | Entender qué hace el robot y dejar paso | Pedido de paso por voz, movimiento predecible |
| Personal de apoyo | Acude cuando el operador lo pide | Saber dónde está la persona | Ubicación y aviso del operador |

## Misión (recorrido ilustrativo)

```
Punto de encuentro (Hall, PB) → Pasillo central → Ascensor A → Piso 3 → Pasillo norte → Aula 305
```

Otras misiones: Biblioteca (piso 1) y Bedelía (planta baja). **El plano de la app es ilustrativo**: reemplazarlo por el de la sede que elijamos.

## ¿Por qué un robot y por qué Go2?

| Alternativa | Qué le falta |
| --- | --- |
| Acompañante humano | Es lo mejor en calidez, pero no siempre hay alguien disponible. El robot **complementa**, no reemplaza. |
| Señalética, piso podotáctil | Ayuda a orientarse, pero no guía físicamente ni avisa obstáculos que cambian (gente, sillas). |
| App de navegación en el celular | Indica, pero no tira del asa ni frena ante un obstáculo. Requiere manejar el teléfono mientras se camina. |
| Robot con ruedas | Más barato, pero no se adapta a desniveles y escalones. |
| **G1 (humanoide)** | Más pesado y alto, menos batería en uso continuo, y su forma no se asocia a guiar. |

El Go2 se elige por **forma y altura parecidas a las de un perro guía**, por moverse en desniveles y porque se lo puede equipar con un asa.

## Capacidades: lo que trae y lo que requiere

| Disponible en el Go2 (según versión) | Requiere equipamiento o desarrollo adicional |
| --- | --- |
| Caminar en interiores, desniveles y escaleras | **Asa con sensor** para saber si la persona la sujeta |
| Sensores de percepción (LiDAR) para obstáculos | **Voz bidireccional** clara en ambientes ruidosos |
| Wi-Fi / conectividad | **Ascensores**: el Go2 no aprieta botones → integración con el edificio o personal |
| SDK para desarrollo (versión EDU) | Mapa de la sede y navegación entre pisos |

Completar con las especificaciones de la versión elegida y sus fuentes.

## Telemetría

Obligatoria: conectividad, batería, estado operativo, misión y progreso, ubicación (piso y zona), movimiento, alertas y última actualización.

Variables del servicio:

1. **Asa**: sujetada / suelta / sin persona.
2. **Distancia al destino** (m).
3. **Gente en el pasillo**: poca / media / mucha (con mucha, el robot baja la velocidad y pide paso).

Extra de HRI: **último aviso por voz**, para que el operador sepa qué escuchó la persona.

## Comandos en el lenguaje del servicio

| Comando | Qué significa acá |
| --- | --- |
| Iniciar misión | "Guiar a Aula 305" |
| Pausar / Reanudar | La persona quiere frenar un momento / seguir |
| Cancelar | Se suspende el viaje; el robot queda con la persona y se avisa al personal |
| Volver a punto seguro | Volver **juntos** al Punto de encuentro |
| Solicitar detención | Detener el movimiento (software; no reemplaza una parada física) |
| Reconocer alerta | El operador confirma que leyó la alerta antes de decidir |

## Riesgos

| # | Riesgo | Medida |
| --- | --- | --- |
| 1 | Llevar a la persona hacia un peligro (escalera, puerta de vidrio) | Avisos por voz anticipados; recorridos solo por rutas habilitadas; prueba en entorno controlado |
| 2 | La persona se separa del robot | Sensor del asa: si la suelta, el robot se detiene, le habla y alerta al operador (escenario propio) |
| 3 | Dejar sola a la persona (batería, falla) | Con batería crítica el robot **no** se vuelve solo: se detiene junto a la persona y pide ayuda |
| 4 | Multitudes en el cambio de turno | Baja la velocidad, pide paso por voz; el operador puede pausar |
| 5 | Órdenes accidentales del operador | Confirmación para cancelar y volver; órdenes deshabilitadas con su motivo |
| 6 | Pérdida de conexión (ascensor, escaleras) | Datos marcados como viejos; órdenes que quedan "sin respuesta" sin suponer que se ejecutaron |
| 7 | Privacidad: la discapacidad es un dato sensible | No se guarda quién es la persona; el historial registra viajes, no identidades; no se graba video |
