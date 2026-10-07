---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 04
subsystem: api
tags: [spring, jpa, postgres, react, no-leidos, polling, navbar, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: ConversacionService/Controller/Mapper, MensajeRepository, useSondeo, ConversacionPage, MisMensajesPage, Navbar (04-01 a 04-03)
provides:
  - GET /api/conversaciones/no-leidas (comprador cuenta los mensajes de la agencia sin leer; admin los de usuarios en toda la bandeja) y POST /api/conversaciones/{id}/leida
  - noLeidos por conversacion en el resumen (lista y hilo)
  - NoLeidosContext (consulta cada 30 s con la pestana visible), BadgeNoLeidos y textoContador
  - Navbar con contador, icono de mensajes y icono de perfil tambien en pantallas chicas
affects: [04-05, 04-06, 04-09, 04-10]

# Los commits estan repartidos en dos repos: back (df6b495) y front (b174a7a).
plan_head_before: cd731edb6ab87b844403bb13a7c159a4a380769d
plan_head_after: df6b495ea4817f62c8b6fb7bb76626623daa7a7f

actuals:
  tokens: 14000   # chars/4 sobre el diff de los dos commits (back + front)
  tasks: 1
  commits: 1   # medido en el back (1); el front suma 1 commit propio (b174a7a), 2 en total

tech-stack:
  added: []
  patterns:
    - "Conteos de no leidos por autor contrario: el comprador cuenta AGENCIA, el admin cuenta USUARIO; marcarLeidos solo toca un autor de una conversacion"
    - "Una sola consulta agrupada (proyeccion ConteoPorConversacion) para los no leidos de toda la lista, sin una por fila"
    - "El contador compartido vive en un contexto con useSondeo; el marcado al abrir el hilo corre como efecto despues de cada carga"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/conversacion/NoLeidosResponse.java
    - ../danteautomotores-front/src/context/NoLeidosContext.jsx
    - ../danteautomotores-front/src/components/BadgeNoLeidos.jsx
  modified:
    - src/main/java/com/danteautomotores/dto/conversacion/ConversacionResumenResponse.java
    - src/main/java/com/danteautomotores/mapper/ConversacionMapper.java
    - src/main/java/com/danteautomotores/repository/MensajeRepository.java
    - src/main/java/com/danteautomotores/service/ConversacionService.java
    - src/main/java/com/danteautomotores/controller/ConversacionController.java
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/test/java/com/danteautomotores/service/ConversacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/ConversacionSeguridadTest.java
    - src/test/java/com/danteautomotores/service/ConversacionPostgresTest.java
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/main.jsx
    - ../danteautomotores-front/src/components/Navbar.jsx
    - ../danteautomotores-front/src/pages/MisMensajesPage.jsx
    - ../danteautomotores-front/src/pages/ConversacionPage.jsx
    - ../danteautomotores-front/src/utils/mensajes.js
    - ../danteautomotores-front/src/utils/mensajes.test.js

key-decisions:
  - "El endpoint de no leidas contesta a cualquier sesion con una regla propia antes de /api/conversaciones/** (gana la primera que coincide); el resto de las rutas de conversaciones sigue siendo solo COMPRADOR"
  - "El detalle del hilo calcula noLeidos desde los mensajes ya cargados (sin consulta extra); la lista usa una unica consulta agrupada"
  - "El marcado al abrir el hilo es un efecto sobre el estado de mensajes: corre despues de cada carga, solo con la pestana visible, y con una guarda para no solapar llamadas"
  - "textoContador devuelve el numero del 1 al 9 y 9+ desde 10"

patterns-established:
  - "Navbar: el link de texto de Mis mensajes y los iconos de mensajes y perfil son solo del comprador; el admin recibe el suyo en 04-05"

requirements-completed: [MSG-05]

duration: 40min
completed: 2026-10-07
---

# Phase 4 Plan 04: El comprador ve cuantos mensajes no leyo Summary

**El comprador ve en el Navbar y en cada fila de Mis mensajes cuantos mensajes de la agencia no abrio: el back cuenta y marca como leidos por autor (con 404 igual para conversaciones ajenas), el endpoint de no leidas ya contesta tambien al admin, y el front consulta cada 30 segundos con la pestana visible.**

## Performance

- **Duration:** ~40 min
- **Completed:** 2026-10-07
- **Tasks:** 1 de 1
- **Files modified:** 19 entre los dos repos

## Accomplishments

- Back: `MensajeRepository` suma `marcarLeidos` (`@Modifying(clearAutomatically, flushAutomatically)`, un autor y una conversacion), `contarNoLeidosPorConversacion` (agrupado, con la proyeccion `ConteoPorConversacion`) y los conteos de mensajes y de conversaciones distintas, por usuario y de toda la bandeja. `ConversacionService.marcarLeida` busca la conversacion por id y cuenta del token (404 si no es suya, sin exigir cuenta verificada), marca solo los de autor AGENCIA con el instante del `Clock` en UTC y devuelve los conteos. `contarNoLeidos` cuenta por rol: ADMIN los de autor USUARIO en toda la bandeja, COMPRADOR los de AGENCIA en sus conversaciones. `ConversacionResumenResponse.noLeidos` lo completan `listarMias` (una consulta agrupada) y `obtenerMia` (desde el hilo cargado).
- `SecurityConfig`: `GET /api/conversaciones/no-leidas` con cualquier sesion autenticada, en la linea 58, antes de `/api/conversaciones/**` con `hasRole("COMPRADOR")` (linea 59). Sin token da 401. El controlador resuelve el literal `no-leidas` antes que `/{id}`.
- Tests: `ConversacionServiceTest` 29 (marcarLeida solo AGENCIA y UTC, 404 ajena sin tocar mensajes, sin exigir verificacion, conteo por rol, no leidos en lista y hilo), `ConversacionSeguridadTest` (no-leidas: 401, 200 comprador, 200 admin, no cae en `/{id}`; leida: 401, 403 admin, 200, 404), `ConversacionPostgresTest` 16 con la base real (3 mensajes en 2 conversaciones suman 3 y 2, marcarLeida deja en cero solo la indicada y no toca los propios, los usuarios no se mezclan, el admin cuenta los de USUARIO, la ajena da 404 y no toca nada).
- Front: `NoLeidosProvider`/`useNoLeidos` (consulta al montar y cada 30 s con `useSondeo`, vuelve a cero al cerrar sesion, ignora respuestas de otra cuenta y traga errores de red), `BadgeNoLeidos` (`aria-label` "N mensajes sin leer", nada si es cero), `textoContador`, Navbar (contador en el link de texto desde `md`, mas los iconos de mensajes y de perfil visibles tambien en pantallas chicas; el admin no ve el link de mensajes), Mis mensajes (contador por fila, extracto en negrita y actualizacion silenciosa cada 30 s) y `ConversacionPage` (marca como leidos con `POST /conversaciones/{id}/leida` tras cada carga con la pestana visible, actualiza el estado local y llama a `refrescar()`).
- Humo (`mensajes-humo.js`, 25 chequeos): no-leidas con 401 sin token, ceros al comprador, el admin cuenta el primer mensaje del comprador y la respuesta solo trae dos numeros; `leida` del dueno con 200 sin tocar sus mensajes; 401, 403 y 404 identico para ajena e inexistente.

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=ConversacionServiceTest,ConversacionSeguridadTest,ConversacionPostgresTest,TransaccionesServiceTest,SeguridadErroresTest -Ddante.pg.required=true` | BUILD SUCCESS, 87 tests, 0 fallas, 0 errores, 0 skipped |
| `npm --prefix ../danteautomotores-front run build && npm ... test` | build OK; 50 tests, 0 fallas |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` | `humo: 25 ok, 0 fallas, 0 skip` |

Criterios de aceptacion: `grep -c "marcarLeidos"` en `MensajeRepository` imprime 1; `no-leidas` en `SecurityConfig` esta en la linea 58 y `/api/conversaciones/**` en la 59; `NoLeidosProvider` en `main.jsx` imprime 3; `BadgeNoLeidos` en `Navbar.jsx` imprime 3; `to="/perfil"` en `Navbar.jsx` imprime 1; el humo termina con "0 fallas".

## Task Commits

1. **Tarea 1 (back):** `df6b495` feat(04-04): contador de mensajes no leidos, marcado al abrir el hilo y endpoint por rol
2. **Tarea 1 (front):** `b174a7a` feat(04-04): el comprador ve el contador de mensajes sin leer en el Navbar y en Mis mensajes

## Deviations from Plan

None - plan executed exactly as written. Detalles de alcance: `obtenerMia` calcula `noLeidos` desde el hilo ya cargado en vez de una consulta agrupada aparte (misma garantia, una consulta menos); `BadgeNoLeidos` usa el singular ("1 mensaje sin leer"); el `aria-label` del icono de mensajes incluye la cantidad sin leer.

## Auth Gates

None.

## Known Stubs

Ninguno nuevo. Siguen vivos a proposito el aviso temporal de "Simula tu financiamiento" y "Cotizar" (Fase 5).

## Threat Flags

Ninguno. T-04-16 mitigado y probado: `marcarLeida` busca por id y dueno (404 si no es suya) y solo toca los mensajes del autor contrario (servicio, Postgres con dos usuarios y humo). T-04-15 y T-04-17 aceptados: el endpoint devuelve solo dos numeros (el humo verifica las claves) y el front consulta cada 30 s con la pestana visible.

## Limitaciones conocidas

- Los no leidos con mensajes de la agencia reales solo se cubren con los tests de Postgres hasta que 04-06 permita responder desde el admin; el humo de 04-06 los repite de punta a punta.
- ESLint no tiene `eslint.config.*` en el front (pre-existente): `npm run lint` no corre.
- La cuenta del admin consulta el mismo endpoint pero el Navbar todavia no le muestra el contador (lo suma 04-05).

## Pendiente de verificacion humana

Human-check de la tarea (no automatizable aca, sin navegador): con el back local y el front de prueba en el 5174, como comprador verificado comprobar que el Navbar tiene el icono de mensajes y el de perfil tambien en pantalla angosta; insertar por SQL en la base de prueba un mensaje de la agencia sin leer y esperar hasta 30 segundos: aparece el contador en el Navbar y en la fila de Mis mensajes, y al abrir el hilo desaparece. La pasada completa por navegador es 04-10.

## Self-Check: PASSED

- Archivos creados verificados en disco (`NoLeidosResponse.java`, `NoLeidosContext.jsx`, `BadgeNoLeidos.jsx`).
- Commits `df6b495` (back) y `b174a7a` (front) existen en sus repos; `commits:` medido desde el ledger en disco (1 en el back).
