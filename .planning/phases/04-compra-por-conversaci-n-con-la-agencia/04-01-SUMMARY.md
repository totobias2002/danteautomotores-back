---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 01
subsystem: api
tags: [spring-boot, flyway, postgres, jpa, react, conversaciones, mensajes, tracer]
status: complete

requires:
  - phase: 03-cuentas-verificadas
    provides: VerificacionCuenta.exigir (gate de cuenta verificada), useExigirCuenta y RutaVerificada del front, SecurityUtils, GlobalExceptionHandler con CUENTA_NO_VERIFICADA
provides:
  - Migracion V6 con las tablas conversaciones y mensajes, sus restricciones y el indice unico parcial uk_conversaciones_compra_abierta
  - Modelo Conversacion y Mensaje (enums TipoConversacion, EstadoConversacion, AutorMensaje) y RegistroDeMensajes como unico punto de registro de un mensaje
  - POST /api/conversaciones (Lo quiero) y GET /api/conversaciones (Mis mensajes), solo COMPRADOR
  - Front: Lo quiero conectado, MisMensajesPage con la lista y utils/mensajes.js
  - scripts/verify/humo-comun.js y scripts/verify/mensajes-humo.js
affects: [04-02, 04-03, 04-04, 04-05, 04-06, 04-07, 04-08, 04-09, 04-10, 04-11]

# Los commits del back estan en danteautomotores-back; el del front en danteautomotores-front (428f58d).
plan_head_before: bc8345bf126e21c06b4b71d953a59f7c5338f2c3
plan_head_after: 72b7af8aa217e24b2d3eced3cfdeafe49c725a53

actuals:
  tokens: 15500
  tasks: 1
  commits: 1   # medido en el back (1); el front suma 1 commit propio (428f58d), 2 en total

tech-stack:
  added: []
  patterns:
    - "Toda fecha de conversaciones y mensajes se fija con el Clock en UTC (LocalDateTime.now(clock.withZone(UTC))) y la API la devuelve como Instant (ISO con Z)"
    - "Un mensaje se registra solo con RegistroDeMensajes.agregar (propagation MANDATORY); los avisos por mail de 04-07 se enganchan ahi"
    - "Los humos comparten helpers en scripts/verify/humo-comun.js (CommonJS) y terminan con la linea 'humo: N ok, N fallas, N skip'"

key-files:
  created:
    - src/main/resources/db/migration/V6__fase4_conversaciones.sql
    - src/main/java/com/danteautomotores/entity/Conversacion.java
    - src/main/java/com/danteautomotores/entity/Mensaje.java
    - src/main/java/com/danteautomotores/service/ConversacionService.java
    - src/main/java/com/danteautomotores/service/RegistroDeMensajes.java
    - src/main/java/com/danteautomotores/controller/ConversacionController.java
    - scripts/verify/humo-comun.js
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/utils/mensajes.js
    - ../danteautomotores-front/src/utils/mensajes.test.js
  modified:
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java
    - ../danteautomotores-front/src/pages/MisMensajesPage.jsx
    - ../danteautomotores-front/src/pages/PublicacionDetallePage.jsx

key-decisions:
  - "Se respetan D-01 a D-04, D-11, D-12 y D-13 del plan: V6 aditiva, tipo y estado desde el inicio, una sola conversacion abierta de compra por usuario y auto, cuenta exigida antes de buscar el auto, fechas en UTC"
  - "Repetir Lo quiero sobre el mismo auto devuelve la misma conversacion; solo agrega un mensaje si el usuario escribio uno propio"
  - "El borrado de un auto con conversaciones queda cubierto por el ON DELETE CASCADE de la base (probado en el test de V6 y en el humo); el aviso previo con conteo (D-16) sigue en 04-09"

requirements-completed: [MSG-01, MSG-03]

duration: 40min
completed: 2026-10-07
---

# Phase 4 Plan 01: Tracer de Lo quiero a Mis mensajes Summary

**Un comprador verificado toca "Lo quiero" y queda creada una conversacion COMPRA ABIERTA atada al auto (migracion V6 con indice unico parcial, gate de cuenta reutilizado, fechas en UTC) que ve en Mis mensajes con foto, auto, estado y extracto.**

## Performance

- **Duration:** ~40 min
- **Completed:** 2026-10-07
- **Tasks:** 1 de 1 (tracer)
- **Files modified:** 24 entre los dos repos

## Accomplishments

- V6 crea `conversaciones` y `mensajes` con los CHECK de tipo, estado, autor y largo de texto (1 a 2000), el CHECK `conversaciones_compra_publicacion_check` (una COMPRA siempre tiene auto), las FK con `ON DELETE CASCADE` y los seis indices, entre ellos el unico parcial `uk_conversaciones_compra_abierta`. No toca V1 a V5 ni ninguna fila existente; Hibernate la valida con `ddl-auto: validate` (el back arranca contra la base vacia migrada).
- `ConversacionService.iniciarCompra` busca la cuenta, llama a `verificacionCuenta.exigir` antes de buscar el auto, rechaza VENDIDO (400) y deja pasar RESERVADO, reutiliza la conversacion abierta o crea una con su primer mensaje (el texto del usuario o "Hola, me interesa este auto: marca modelo anio."). `listarMias` resuelve el ultimo mensaje de todas las conversaciones con una sola consulta.
- `/api/conversaciones/**` queda restringido a COMPRADOR en `SecurityConfig`; el controller nunca recibe un id de usuario.
- Front: "Lo quiero" pasa por `exigir`, hace `api.post('/conversaciones', ...)`, queda deshabilitado con "Abriendo conversacion..." y navega a `/mensajes`; `MisMensajesPage` lista las conversaciones con portada, "marca modelo anio", estado del auto, etiqueta del tipo, extracto y fecha (cargando, error con Reintentar y estado vacio con link al catalogo; el admin se redirige a `/admin`). `utils/mensajes.js` tiene `etiquetaTipo`, `extracto` y `fechaDeMensaje` con 8 tests.
- Humo de punta a punta con helpers reutilizables (`humo-comun.js`) y `mensajes-humo.js`.

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=MigracionesPostgresTest -Ddante.pg.required=true` | 9 tests, 0 fallas (incluye `v6CreaConversacionesYMensajesConSusRestricciones`) |
| `npm --prefix ../danteautomotores-front run build && npm --prefix ../danteautomotores-front test` | build OK; 43 tests, 0 fallas (8 nuevos de `mensajes.test.js`) |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` | `humo: 5 ok, 0 fallas, 0 skip` |
| Suite completa del back (`mvn test -Ddante.pg.required=true`) | 629 tests, 0 fallas |

Criterios de aceptacion: `uk_conversaciones_compra_abierta` aparece 1 vez y `ON DELETE CASCADE` 2 veces fuera de comentarios; `git status` de `db/migration` solo listaba V6; `verificacionCuenta.exigir` aparece 1 vez en `ConversacionService`; `api.post('/conversaciones'` aparece 1 vez en la ficha.

## Task Commits

1. **Tarea 1 (tracer), back:** `72b7af8` feat(04-01): Lo quiero abre una conversacion de compra (V6, modelo, API y humo)
2. **Tarea 1 (tracer), front:** `428f58d` feat(04-01): Lo quiero abre la conversacion y Mis mensajes lista las conversaciones

## Deviations from Plan

None - plan executed exactly as written. El borrado del auto de prueba al final del humo (`DELETE /publicaciones/{id}`) es un agregado menor del propio script que ademas prueba el cascade de la base de punta a punta.

## Auth Gates

None.

## Known Stubs

Ninguno nuevo. Siguen vivos a proposito (fuera de este plan): el aviso temporal de "Simula tu financiamiento" y "Cotizar" (los conecta la Fase 5) y el bloque "Consultar por este auto", que sigue usando `/consultas` hasta 04-02.

## Threat Flags

Ninguno: la unica superficie nueva es `/api/conversaciones`, ya cubierta por T-04-01 a T-04-07. Las respuestas no llevan DNI, telefono ni hash (el humo recorre todo el JSON). Queda para 04-02 el test con dos cuentas (IDOR), con campos de mas y la matriz 401/403/404 de seguridad, tal como indica el modelo de amenazas.

## Pendiente de verificacion humana

El plan indica un human-check junto con el recorrido final de 04-10: como comprador verificado, tocar "Lo quiero" en una ficha y ver la conversacion en Mis mensajes con la foto y el estado del auto. No bloquea la ejecucion.

## Self-Check: PASSED

- Archivos creados verificados en disco (V6, entidades, services, controller, humos, `mensajes.js` y su test).
- Commits `72b7af8` (back) y `428f58d` (front) existen en sus repos.
