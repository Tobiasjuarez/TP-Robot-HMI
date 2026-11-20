# Plan de desarrollo de Guía UADE

Este documento cuenta cómo se construyó la app, etapa por etapa, qué decisiones tomamos y qué falta. Sirve para que cualquiera del equipo, al bajar el repo, sepa dónde estamos y cómo probar todo. El README explica cómo correr la app; acá está el recorrido completo.

**Caso de uso:** un Unitree Go2 simulado guía a personas ciegas o con baja visión por el Campus UADE Monserrat, y un operador lo supervisa desde la app ([`caso-de-uso.md`](caso-de-uso.md)).

**Equipo:** Tobías Juárez, Matías Fernández y Lucas Casas.

## Estado

| Etapa | Qué se hizo | PR | Estado |
| --- | --- | --- | --- |
| 0 | Repo base: MVVM, robot simulado, Room, pantallas y documentación | — (commit inicial) | Hecha |
| 1 | Gradle como Rodando y caso de uso Guía UADE sobre el plano del Campus Monserrat | #1 | Hecha |
| 2 | Figma: componentes, wireframes, 12 pantallas, flujo normal, flujo con falla, mapa de navegación y flujo de casos de uso | — (en Figma) | Hecha |
| 3 | Documentación con el formato de Rodando, integrantes, wireframes y flujo de casos de uso | #2, #3 y #4 | Hecha |
| 4 | APK compilado por GitHub Actions y publicado en el release v1.0; informe en PDF | #5 | Hecha |
| 5 | Guía del proyecto para asistentes de IA (`AGENTS.md`) | #6 | Hecha |
| 6 | Evidencias de los cinco escenarios (Lucas Casas) e informe con el Anexo A | #7 | Hecha |
| 7 | Video de demostración | — | Pendiente |

## Forma de trabajo

- Una rama por etapa (`feature/...`, `fix/...`, `docs/...`), commits chicos con prefijo (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Cada rama termina en un pull request con el formato de `.github/pull_request_template.md`: qué cambia, punto de la consigna, cómo se probó y uso de IA.
- Antes de mergear: compila, corre en el emulador y pasa `./gradlew test`.
- Usamos Claude (Anthropic) como asistente para escribir código y documentos; cada PR lo declara y el equipo lo revisa y prueba.

## Arquitectura en una línea

`Pantalla (Compose) → RobotViewModel (StateFlow de RobotUiState) → RobotRepository → robot simulado + Room`. La UI no conoce al simulador; todo lo propio del caso de uso está en `GuiaUadeProfile`. Inyección de dependencias manual en `AppContainer`. Diagrama en [`arquitectura.md`](arquitectura.md).

## Qué se hizo en cada etapa

### Etapa 0 · Repo base

- Modelos de dominio y `CommandRules`, que decide qué orden se habilita en cada momento, con tests unitarios.
- `RobotRepository` como interfaz y `SimulatedRobotRepository` como implementación: tick de 1 s, misiones por puntos, batería, alertas, órdenes con latencia, rechazos y vencimiento a los 6 s.
- Pantallas: selección del robot, Panel (estado, telemetría, misión, control y últimas órdenes), Alertas, Historial (Room) y Simulación.
- Documentación: arquitectura, contrato JSON, escenarios y decisiones.

### Etapa 1 · Gradle y Guía UADE (#1)

- Ajustes de Gradle iguales a Rodando para que KSP (Room) funcione con AGP 9: plugin `org.jetbrains.kotlin.android`, `android.builtInKotlin=false` y `android.newDsl=false`. Las advertencias de "deprecated" al compilar son esperables.
- `GuiaUadeProfile` sobre el plano del Mapa UADE: misiones al Aula 225 (Lima 2, 2.º piso), a UADE Labs por el patio y a la parada de autos de Av. Independencia, desde el Punto de encuentro en la entrada de Lima.
- Variables del servicio: asa, distancia al destino y gente en el pasillo, más el último aviso por voz.
- Escenario propio: la persona suelta el asa y el robot se detiene, le habla y pide decisión.
- Con batería crítica el robot no deja sola a la persona: se detiene y pide ayuda. No se puede reanudar con batería crítica.
- Esquema del recorrido ([`recorrido-campus.svg`](recorrido-campus.svg)) y caso de uso con la justificación frente a un acompañante humano, con fuentes.

### Etapa 2 · Figma

- [Archivo de Figma](https://www.figma.com/design/4jqfkUGmYIYXSamN4MWbRV) con cinco páginas: componentes con estados normal, deshabilitado, advertencia y error; wireframes de 7 pantallas en baja fidelidad con notas numeradas; prototipo con 12 pantallas y cuatro puntos de inicio (flujo normal, asa suelta, sin conexión, batería crítica); mapa de navegación; y flujo de casos de uso con carriles (persona, operador, robot) y los flujos alternativos de obstáculo, asa suelta, batería baja y sin conexión.

### Etapa 3 · Documentación (#2)

- README, `docs/README.md` y este plan con el mismo formato que Rodando. Carpeta `evidencias/` con la lista de capturas.

### Etapas 4 a 7 · Entrega final

- [x] APK en el [release v1.0](https://github.com/Tobiasjuarez/TP-Robot-HMI/releases/latest): lo compila GitHub Actions, que también corre los tests en cada PR (#5)
- [x] Guía para asistentes de IA en `AGENTS.md` (#6)
- [x] Capturas de los cinco escenarios en [`evidencias/`](evidencias/), tomadas por Lucas el 19/11 (24 imágenes)
- [x] Informe final con las evidencias en el Anexo A y las observaciones de las pruebas, exportado a [`entrega-final.pdf`](entrega-final.pdf) (#7)
- [ ] Video de demostración

## Decisiones importantes

| Decisión | Por qué |
| --- | --- |
| Repository como interfaz y robot simulado detrás | Desacoplar la UI del robot (lo pide la consigna) y poder cambiar a un robot real sin tocar la UI |
| Reglas de habilitación en `CommandRules` (Kotlin puro) | Una sola fuente de verdad para la UI, testeable sin Android |
| El robot simulado valida de nuevo cada orden | La app puede tener datos viejos: el robot es la fuente de verdad |
| Datos viejos a los 3 s, órdenes sin respuesta a los 6 s | Con 1 telemetría por segundo, 3 s sin datos indica un problema real |
| Detención sin confirmación; cancelar y volver con confirmación | Prevenir órdenes accidentales sin demorar la de seguridad |
| Reanudar exige reconocer la alerta crítica | Obliga a leer qué pasó antes de decidir |
| Con batería crítica el robot no vuelve solo si está guiando | Un servicio de guía no puede dejar sola a alguien que no ve |
| Go2 y no G1 | Forma y altura de perro guía, se le monta un asa, más autonomía |
| Plano del Mapa UADE con ascensores aproximados | Edificios y entradas reales; los interiores hay que verificarlos en la sede |

El registro completo, con las limitaciones, está en [`decisiones.md`](decisiones.md).

## Cómo probar todo

1. Run desde Android Studio → **Conectar**.
2. Misión normal: **Iniciar** "Guiar al Aula 225" → confirmar → ver la orden Pendiente → Confirmada, el progreso y los avisos por voz → esperar la llegada → pestaña Historial.
3. Batería baja: con una misión en curso, Simulación → **Batería 18 %** (advertencia) y después **Batería 8 %** (se detiene junto a la persona).
4. Desconexión: Simulación → **Cortar conexión 15 s** → en el Panel, banner de datos viejos y Pausar deshabilitado; Solicitar detención queda Sin respuesta.
5. Incidente: Simulación → **Generar obstáculo** → reconocer la alerta → Reanudar.
6. Escenario propio: Simulación → **Disparar escenario propio** (la persona suelta el asa) → reconocer → Reanudar.
7. Rechazo: Simulación → **Rechazar próxima orden** → mandar cualquier orden.
8. Tests: `./gradlew test`.

Detalle de cada escenario y qué capturar: [`escenarios.md`](escenarios.md).

## Problemas conocidos y soluciones

| Problema | Solución |
| --- | --- |
| "Cannot find a Java installation … languageVersion=25" al abrir el proyecto | *Settings → Build Tools → Gradle → Gradle JDK* = **jbr-25** y borrar `gradle/gradle-daemon-jvm.properties` (lo crea Android Studio) |
| "Gradle 9.5.0 is not the latest minor version" | Ignorarlo: usamos la misma versión que Rodando |
| No se puede iniciar una misión | Batería por debajo de 25 %: "Volver a punto seguro" y esperar a que cargue |
| Reanudar sigue deshabilitado | Hay una alerta crítica sin reconocer (pestaña Alertas) o la batería está por debajo de 10 % |

## Pendientes del equipo (fuera del código)

- Verificar en la sede dónde están los ascensores de Lima 2 y el paso interno de Lima 1 a Lima 2.
- Investigar la versión del Go2 elegida y citar sus especificaciones oficiales.
