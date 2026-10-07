---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 05
subsystem: api
tags: [spring, jpa, specification, postgres, react, bandeja, admin, polling, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: Conversacion/Mensaje, ConversacionRepository, MensajeRepository (findUltimosPorConversaciones, contarNoLeidosPorConversacion), ConversacionMapper, NoLeidosContext, BadgeNoLeidos, useSondeo, Navbar (04-01 a 04-04)
provides:
  - GET /api/admin/conversaciones?tipo&estado&soloNoLeidas&pagina (pagina de a 20, ultimo mensaje primero)
  - ConversacionSpecification.bandeja (filtros opcionales sin parametros nulos; soloNoLeidas con EXISTS)
  - usuario (id, nombre, apellido, email) en el resumen, solo para el admin
  - AdminMensajesPage en /admin/mensajes con filtros en la URL, paginador y actualizacion cada 30 s
  - Tarjeta Mensajes del panel y link e icono de Mensajes del Navbar del admin, con contador
affects: [04-06, 04-08, 04-10]

# Los commits estan repartidos en dos repos: back (1d5439e) y front (bbe270b).
plan_head_before: b091031bf1e8b82ba3b4454e6f01c0a79565f626
plan_head_after: 1d5439e25caecd28e16fb227e7b2ad0da4963751

actuals:
  tokens: 20000   # chars/4 sobre el diff de los dos commits (back + front)
  tasks: 1
  commits: 1   # medido en el back (1); el front suma 1 commit propio (bbe270b), 2 en total

tech-stack:
  added: []
  patterns:
    - "Bandeja con Specification: cada filtro presente agrega un predicado; los ausentes no agregan nada; 'solo no leidas' es un EXISTS (no duplica filas ni el conteo de la pagina)"
    - "findAll(Specification, Pageable) sobreescrito en el repositorio con @EntityGraph (usuario, auto, agencia) para armar la pagina sin una consulta por fila"
    - "Filtros de la bandeja como funciones puras (leerFiltrosBandeja, paramsDeBandeja, paramsParaApi) con la URL como fuente de verdad"

key-files:
  created:
    - src/main/java/com/danteautomotores/repository/spec/ConversacionSpecification.java
    - src/main/java/com/danteautomotores/service/ConversacionAdminService.java
    - src/main/java/com/danteautomotores/controller/AdminConversacionController.java
    - src/main/java/com/danteautomotores/dto/conversacion/UsuarioDeConversacionResponse.java
    - src/test/java/com/danteautomotores/service/ConversacionAdminServiceTest.java
    - src/test/java/com/danteautomotores/controller/AdminConversacionSeguridadTest.java
    - src/test/java/com/danteautomotores/service/ConversacionAdminPostgresTest.java
    - ../danteautomotores-front/src/pages/admin/AdminMensajesPage.jsx
  modified:
    - src/main/java/com/danteautomotores/repository/ConversacionRepository.java
    - src/main/java/com/danteautomotores/dto/conversacion/ConversacionResumenResponse.java
    - src/main/java/com/danteautomotores/mapper/ConversacionMapper.java
    - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/routes/AppRouter.jsx
    - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
    - ../danteautomotores-front/src/components/Navbar.jsx
    - ../danteautomotores-front/src/pages/MisMensajesPage.jsx
    - ../danteautomotores-front/src/utils/mensajes.js
    - ../danteautomotores-front/src/utils/mensajes.test.js

key-decisions:
  - "Sin 'estado' el back devuelve todas las conversaciones; el front manda estado=ABIERTA por defecto y no manda estado cuando el filtro es TODAS"
  - "El campo usuario del resumen es @JsonInclude(NON_NULL): las respuestas del comprador no lo llevan (lo completa solo toResumenParaAdmin)"
  - "Una pagina que ya no existe (link viejo) vuelve a la primera con replace, sin romper el historial"
  - "Cada pedido de la bandeja lleva un numero y la respuesta de uno viejo se descarta, para que un filtro cambiado no se pise con una respuesta tardia"

patterns-established:
  - "Navbar del admin: link de texto Mensajes desde md y el icono con contador visible tambien en pantallas chicas"

requirements-completed: [MSG-07]

duration: 45min
completed: 2026-10-07
---

# Phase 4 Plan 05: La bandeja del admin Summary

**El admin ve todas las conversaciones de todos los usuarios en una bandeja de a 20 (la de mensaje mas reciente primero), la filtra por tipo, estado y "solo no leidas" con los filtros en la URL, y ve el contador de no leidos en su Navbar y en la tarjeta Mensajes del panel; cada fila dice con quien se habla, sin DNI ni telefono.**

## Performance

- **Duration:** ~45 min
- **Completed:** 2026-10-07
- **Tasks:** 1 de 1
- **Files modified:** 19 entre los dos repos (12 en el back, 7 en el front)

## Accomplishments

- Back: `ConversacionSpecification.bandeja(tipo, estado, soloNoLeidas)` con Criteria API (parametros enlazados, sin predicados para filtros ausentes; `soloNoLeidas` es un `EXISTS` sobre mensajes de autor USUARIO con `leidoEn` nulo, asi una conversacion con varios sin leer sale una sola vez y el conteo de la pagina da el total correcto). `ConversacionRepository` extiende `JpaSpecificationExecutor` y sobreescribe `findAll(Specification, Pageable)` con `@EntityGraph` (usuario, auto, agencia). `ConversacionAdminService.listar` (`readOnly`) pagina de a 20 con `max(pagina, 1) - 1`, ordena por `ultimoMensajeEn` desc e `id` desc, y resuelve el ultimo mensaje y los no leidos (autor USUARIO) de toda la pagina con dos consultas agrupadas. `AdminConversacionController` en `/api/admin/conversaciones`; `SecurityConfig` no cambio (`/api/admin/**` ya era solo ADMIN). `UsuarioDeConversacionResponse` (id, nombre, apellido, email) y `ConversacionMapper.toResumenParaAdmin`.
- Front: `AdminMensajesPage` (`/admin/mensajes`, `ProtectedRoute soloAdmin`) con selectores de tipo y estado y la casilla "Solo no leidas" escritos en la URL (replace y vuelta a la pagina 1), `Paginador` (push), actualizacion silenciosa cada 30 s con `useSondeo`, y los estados "Cargando...", error con "Reintentar" y "No hay conversaciones con estos filtros.". Tarjeta "Mensajes" del panel con `BadgeNoLeidos`; link de texto "Mensajes" (desde `md`) e icono de mensajes con contador (tambien en pantallas chicas) en el Navbar del admin; el admin que abre `/mensajes` llega a `/admin/mensajes`. `leerFiltrosBandeja`, `paramsDeBandeja` y `paramsParaApi` en `utils/mensajes.js`.
- Humo (`mensajes-humo.js`, 29 chequeos): 401 sin token y 403 al comprador en la bandeja; el admin lista la fila con nombre y apellido del usuario, `noLeidos > 0`, estructura de pagina, orden por ultimo mensaje y sin claves dni ni telefono; `estado=CERRADA` no la incluye, `ABIERTA`, `soloNoLeidas=true` y `tipo=COMPRA` si, `tipo=COTIZACION` da una pagina vacia, `tipo=NAVE` da 400, pagina 999 vacia.

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=ConversacionAdminServiceTest,AdminConversacionSeguridadTest,ConversacionAdminPostgresTest,TransaccionesServiceTest,SeguridadErroresTest -Ddante.pg.required=true` | BUILD SUCCESS, 44 tests (9 servicio, 8 seguridad, 9 Postgres, 3 transacciones, 15 errores), 0 fallas, 0 skipped |
| `mvn -B -o -Djava.version=17 test -Ddante.pg.required=true` (suite completa, extra) | BUILD SUCCESS, 724 tests, 0 fallas, 0 skipped |
| `npm --prefix ../danteautomotores-front run build && npm ... test` | build OK; 58 tests, 0 fallas (8 nuevos de los filtros de la bandeja) |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` | `humo: 29 ok, 0 fallas, 0 skip` |

Criterios de aceptacion: `ConversacionSpecification` en `ConversacionAdminService` (>= 1), `JpaSpecificationExecutor` en `ConversacionRepository` (>= 1), `path="/admin/mensajes"` en `AppRouter.jsx` (1), `admin/mensajes` en `Navbar.jsx` (>= 1), `ConversacionAdminService.class` en `TransaccionesServiceTest` (1), tests de `leerFiltrosBandeja` pasan, `dangerouslySetInnerHTML` no aparece en `src`, el humo termina con "0 fallas". La base `dante_humo_mensajes` se borro al terminar.

## Task Commits

1. **Tarea 1 (back):** `1d5439e` feat(04-05): el admin ve la bandeja de todas las conversaciones y la filtra por tipo, estado y no leidas
2. **Tarea 1 (front):** `bbe270b` feat(04-05): el admin ve la bandeja de mensajes con filtros en la URL y el contador en el Navbar y el panel

## Deviations from Plan

None - plan executed exactly as written. Detalles de alcance: el repositorio sobreescribe `findAll(Specification, Pageable)` con `@EntityGraph` para que la pagina no haga una consulta por fila de usuario, auto y agencia (las fotos de portada siguen cargandose perezosamente por fila, igual que en Mis mensajes); la bandeja vuelve sola a la pagina 1 si la URL pide una pagina que ya no existe; los pedidos de la bandeja se numeran para descartar respuestas tardias.

## Auth Gates

None.

## Known Stubs

Ninguno nuevo. La fila de la bandeja enlaza a `/admin/mensajes/${id}`, ruta que todavia no existe (el hilo del admin lo agrega 04-06, como dice el plan): hasta entonces el clic cae en la pagina "no encontrada".

## Threat Flags

Ninguno. T-04-18 mitigado y probado (`/api/admin/**` solo ADMIN: 401 sin token y 403 al comprador en el test de seguridad y en el humo), T-04-19 (el DTO del usuario solo tiene id, nombre, apellido y email; el test y el humo verifican que el JSON no trae dni ni telefono; `usuario` es NON_NULL y no viaja en las respuestas del comprador), T-04-20 (filtros enum/booleano/entero con Criteria API, sin predicados para filtros ausentes; `tipo=NAVE` da 400) y T-04-21 (el extracto se muestra como texto de React; grep de `dangerouslySetInnerHTML` sin resultados). T-04-SC aceptado: sin paquetes nuevos.

## Limitaciones conocidas

- Las filas apuntan a `/admin/mensajes/:id`, que llega con 04-06 (hilo del admin, responder, cerrar y reabrir).
- ESLint no tiene `eslint.config.*` en el front (pre-existente): `npm run lint` no corre.

## Pendiente de verificacion humana

Human-check de la tarea (no automatizable aca, sin navegador): con el back local y el front de prueba en el 5174, como admin abrir Administracion, ver la tarjeta Mensajes con el contador, entrar a la bandeja y probar los tres filtros y el paginador; comprobar que cambiar un filtro vuelve a la pagina 1, que recargar la pagina conserva los filtros y que un admin que abre `/mensajes` termina en su bandeja. La pasada completa por navegador es 04-10.

## Self-Check: PASSED

- Archivos creados verificados en disco (`ConversacionSpecification.java`, `ConversacionAdminService.java`, `AdminConversacionController.java`, `UsuarioDeConversacionResponse.java`, los tres tests y `AdminMensajesPage.jsx`).
- Commits `1d5439e` (back) y `bbe270b` (front) existen en sus repos; `commits:` medido desde el ledger en disco (1 en el back).
