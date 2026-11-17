# Plan de desarrollo — Guía UADE

Este documento cuenta cómo se construye la app, etapa por etapa, qué decisiones tomamos y qué falta. El README explica cómo correr la app; acá está el recorrido completo.

**Caso de uso:** un Unitree Go2 guía a personas ciegas o con baja visión dentro de una sede UADE, supervisado desde la app ([caso-de-uso.md](caso-de-uso.md)).

**Equipo:** _(completar)_

## Estado

| Etapa | Qué se hizo | PR | Estado |
| --- | --- | --- | --- |
| 0 | Repo base: arquitectura MVVM, robot simulado, Room, docs | — (commit inicial) | Hecha |
| 1 | Configuración de Gradle como Rodando y caso de uso Guía UADE | #1 | En revisión |
| 2 | Preentrega: documento, esquema del recorrido, Figma (flujo normal y falla) | — | Pendiente |
| 3 | Aplicar el diseño del Figma a la app y plano real de la sede | — | Pendiente |
| 4 | Evidencias de los 5 escenarios, video y diagrama de arquitectura | — | Pendiente |

## Forma de trabajo

- Una rama por etapa (`feature/...`), commits chicos con prefijo (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Cada rama termina en un pull request con el formato de `.github/pull_request_template.md`: qué cambia, punto de la consigna, cómo se probó y uso de IA.
- Antes de mergear: compila, corre en el emulador y pasa `./gradlew test`.
- Usamos Claude (Anthropic) como asistente para escribir código y documentos; cada PR lo declara y el equipo lo revisa y prueba.

## Arquitectura en una línea

`Pantalla (Compose) → RobotViewModel (StateFlow de RobotUiState) → RobotRepository → robot simulado + Room`. La UI no conoce al simulador. Todo lo propio del caso de uso está en `GuiaUadeProfile`. Inyección de dependencias manual en `AppContainer`. Detalle en [arquitectura.md](arquitectura.md).

## Qué se hizo en cada etapa

### Etapa 0 · Repo base

- Modelos, `CommandRules` (habilitación de comandos, con tests), Repository, simulador con misiones, batería, alertas, latencia, rechazos y timeouts.
- Pantallas: selección del robot, Panel, Alertas, Historial (Room) y Simulación.

### Etapa 1 · Gradle y Guía UADE (#1)

- Ajustes de Gradle iguales a Rodando para que KSP (Room) funcione con AGP 9: `android.builtInKotlin=false` y `android.newDsl=false`. Las advertencias de "deprecated" al compilar son esperables.
- No se versiona `gradle/gradle-daemon-jvm.properties` (pedía Java 25).
- `GuiaUadeProfile`: viajes a Aula 305, Biblioteca y Bedelía; asa, distancia, gente en el pasillo y avisos por voz.
- Con batería crítica el robot no deja sola a la persona.

## Pendientes

- [ ] Elegir la sede y pasar el plano real a `GuiaUadeProfile`
- [ ] Investigar la versión del Go2 (sensores, autonomía, qué trae y qué no) y citar fuentes
- [ ] Figma navegable: flujo normal y flujo con falla
- [ ] Aplicar colores y componentes del Figma en `ui/theme` y `ui/components`
- [ ] Vista del recorrido (opcional: Canvas con los puntos y la posición del robot)
- [ ] Capturas/video de los 5 escenarios
