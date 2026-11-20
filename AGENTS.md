# Guía para asistentes de IA (y para el equipo)

Leé este archivo entero antes de tocar el proyecto. Resume qué es, cómo está armado, qué reglas no se pueden romper y qué falta. Vale para cualquier asistente (Claude, Copilot, Cursor, Codex, Gemini, etc.); `CLAUDE.md`, `GEMINI.md` y `.github/copilot-instructions.md` solo apuntan acá.

## Qué es

**Guía UADE**: TP grupal de *Desarrollo de Aplicaciones I* (UADE, 2.º cuatrimestre 2026, viernes noche, docentes Peña y Narducci). Integrantes: Tobías Juárez, Matías Fernández y Lucas Casas. **Entrega final: viernes 20 de noviembre de 2026.**

La consigna pide una interfaz HMI/HRI para un robot de servicio Unitree G1 o Go2: un prototipo en Figma y un APK Android con el robot **simulado** (no hay hardware). Elegimos un **Unitree Go2 con asa que guía a personas ciegas o con baja visión por el Campus UADE Monserrat** (Lima, Chile, Salta y Av. Independencia). La app es para el **operador** que supervisa los viajes. El robot nunca deja sola a la persona.

Qué pide la consigna y cómo lo cubrimos está en el `README.md` (sección "Qué pide la consigna y cómo lo cubrimos") y en `docs/entrega-final.pdf`. Antes de proponer cambios, chequeá que sigan cumpliendo la consigna (telemetría, comandos con estados, 5 escenarios, Repository desacoplado, Room, sin internet).

## Dónde está cada cosa

| Qué | Dónde |
| --- | --- |
| Plan, etapas, decisiones, cómo probar y pendientes | `docs/PLAN.md` |
| Índice de la documentación | `docs/README.md` |
| Caso de uso, actores, por qué robot y por qué Go2, riesgos | `docs/caso-de-uso.md` |
| Arquitectura (mermaid) y ciclo de una orden | `docs/arquitectura.md` |
| Contrato JSON propuesto (no es el protocolo oficial de Unitree) | `docs/contrato-json.md` |
| Paso a paso de los 5 escenarios | `docs/escenarios.md` |
| Capturas de los cinco escenarios | `docs/evidencias/` (tabla en su README) |
| Registro de decisiones y limitaciones | `docs/decisiones.md` |
| Preentrega entregada | `docs/preentrega.pdf` |
| Informe de la entrega final (con evidencias) | `docs/entrega-final.pdf` |
| Diseño | Figma: https://www.figma.com/design/4jqfkUGmYIYXSamN4MWbRV (páginas Componentes, Wireframes, Prototipo, Mapa de navegación y Flujo de casos de uso) |

## Arquitectura

```
UI (Compose) → RobotViewModel (StateFlow<RobotUiState>) → RobotRepository (interfaz) → SimulatedRobotRepository + GuiaUadeProfile
                                                                                    → Room (event_log)
```

Paquete `ar.edu.uade.robothmi`:

- `domain/model/Models.kt`: enums y data classes (estados del robot, misión, órdenes, alertas, telemetría).
- `domain/CommandRules.kt`: **única fuente de verdad** de qué orden está habilitada y por qué. Kotlin puro, con tests.
- `data/RobotRepository.kt`: interfaz `RobotRepository` + `SimulationControls` (solo para la pestaña Simulación).
- `data/sim/SimulatedRobotRepository.kt`: el robot simulado (tick de 1 s, latencia, rechazos, vencimiento a los 6 s, batería, alertas).
- `data/sim/ServiceProfile.kt` y `GuiaUadeProfile.kt`: **todo lo propio del caso de uso** (misiones con coordenadas del campus, variables asa / distancia / gente, avisos por voz, escenario propio "la persona soltó el asa").
- `data/local/`: Room (`EventLog`, `AppDatabase`).
- `ui/RobotViewModel.kt`: combina repositorio + reloj; marca datos viejos a los 3 s.
- `ui/screens/`: Selección de robot, Panel, Alertas, Historial, Simulación (`MainScaffold` arma la navegación).
- `ui/components/Components.kt`: StatusPill, MetricTile, StaleBanner, AvailabilityHint.
- `RobotHmiApplication.kt`: `AppContainer` (inyección de dependencias manual).

## Reglas que no hay que romper

1. **La UI nunca accede al simulador**: solo observa `RobotUiState` y manda eventos al ViewModel. El simulador se usa únicamente a través de `RobotRepository` / `SimulationControls`.
2. **Habilitación de órdenes solo en `CommandRules`** (no duplicar `if`s en las pantallas). Umbrales: iniciar misión con batería ≥ 25 %, advertencia < 20 %, crítica < 10 %.
3. Toda orden muestra su estado: **Pendiente, Confirmada, Rechazada o Sin respuesta** (6 s). "Confirmada" = el robot aceptó, no que terminó.
4. **Solicitar detención** siempre visible y sin confirmación; se habilita cuando el robot se mueve o espera una decisión; es un comando de software simulado, no una parada de emergencia. Sin conexión solo se permiten detención y volver a punto seguro.
5. **Reanudar** exige reconocer la alerta crítica y está bloqueado con batería crítica.
6. **El robot no deja sola a la persona**: con batería crítica en misión pasa a "Requiere intervención" y espera; no vuelve solo a la base (`autoReturnOnCriticalBattery = false`).
7. Todo estado se muestra con **texto + símbolo + color**, nunca solo color.
8. No guardar datos personales de la persona guiada.
9. La app funciona **sin robot, servidor ni internet**.

Si un cambio necesita romper alguna, primero hablarlo con el equipo y anotarlo en `docs/decisiones.md`.

## Cómo compilar y probar

- Android Studio, Gradle JDK = **jbr-25** (Settings → Build Tools → Gradle). Si aparece "Cannot find a Java installation … languageVersion=25", borrar `gradle/gradle-daemon-jvm.properties` (lo genera Android Studio y está en `.gitignore`).
- Ignorar el aviso de pasar a Gradle 9.9: usamos 9.5 a propósito (igual que Rodando).
- `./gradlew testDebugUnitTest` corre los tests de `CommandRules`; `./gradlew assembleDebug` arma el APK.
- **No tocar** `android.builtInKotlin=false` ni `android.newDsl=false` en `gradle.properties`: KSP (Room) no funciona con AGP 9 sin eso.
- GitHub Actions (`.github/workflows/apk.yml`) compila y corre los tests en cada PR. Al crear un release con tag `v*`, adjunta `GuiaUADE.apk`.
- Para probar los escenarios: pestaña **Simulación** (Batería 18 % / 8 %, Cortar conexión 15 s, Generar obstáculo, Disparar escenario propio, Rechazar próxima orden). Detalle en `docs/PLAN.md` → "Cómo probar todo".

## Forma de trabajo

- `main` siempre compila. Una rama por tarea: `feature/…`, `fix/…`, `docs/…`, `chore/…`.
- Commits chicos con prefijo: `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`. Mensajes en español.
- Todo entra por pull request con la plantilla `.github/pull_request_template.md` (qué cambia, punto de la consigna, cómo se probó, **uso de IA**). Declarar siempre qué hizo la IA.
- Código y textos de la app en español rioplatense neutro ("Tocá", "Seleccioná"). Documentación en español, directa y sin relleno.
- Si se agrega una pantalla, estado o regla nueva: actualizar Figma, `docs/PLAN.md` y, si corresponde, `docs/decisiones.md` y el informe.

## Estado y pendientes (20/11/2026)

- **Preentrega (20/11):** entregada con `docs/preentrega.pdf` y el link al Figma. El docente aceptó que ya tengamos avanzado lo de la entrega final.
- **Hecho para la entrega final:** app con simulador, Room y tests; Figma (componentes, wireframes, 12 pantallas, mapa de navegación, flujo de casos de uso); APK en el release `v1.0` (GitHub Actions); 24 capturas en `docs/evidencias/`; informe `docs/entrega-final.pdf` con el Anexo A.

Falta para la entrega final (fecha a confirmar):

- [ ] Grabar el video de demostración y poner el link en el README y en el informe.
- [ ] Regenerar `docs/entrega-final.pdf` con el link del video (el informe editable lo tiene Tobías).
- [ ] Verificar en la sede la ubicación de los ascensores de Lima 2 (hoy es aproximada).
- [ ] Opcional: que "Disparar escenario propio" solo funcione con una misión en curso (ver observaciones en `docs/decisiones.md`).

No conviene agregar funcionalidades grandes: priorizar el video y la estabilidad.
