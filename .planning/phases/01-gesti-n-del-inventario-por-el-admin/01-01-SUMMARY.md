---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 01
subsystem: api
tags: [spring-security, jwt, webmvctest, axios, react-router, admin-panel]

requires:
  - phase: none
    provides: "Primera fase del proyecto (brownfield: SecurityConfig, filtro JWT y panel admin ya existían)"
provides:
  - "GET /api/admin/publicaciones: listado de autos en todos los estados, solo ADMIN"
  - "Filtro JWT que nunca lanza: token malformado, vacío, vencido o de usuario borrado deja la request sin autenticar"
  - "RestAuthenticationEntryPoint (401) y RestAccessDeniedHandler (403) con cuerpo JSON {error} en español"
  - "SeguridadWebMvcTestBase: base reutilizable de @WebMvcTest con SecurityConfig real, bearerPara(email, rol) y bearerVencido(email)"
  - "Front: mensajeDeError(err, fallback), interceptor axios 401, registrarManejadorSesionVencida, state de router { from, sesionVencida }"
affects: [01-02, 01-03, 01-04, 01-05, 01-06, 01-07, fase-2-catalogo-publico]

actuals:
  tokens: 7859
  tasks: 3
  commits: 3
plan_head_before: 57baf79075d03ca292e8d762e1cd0a306e6bd4db
plan_head_after: d5be501d5ccabd6c6c3753f362754c6adcf90ce4

tech-stack:
  added: []
  patterns:
    - "Tests de seguridad sin DB: @WebMvcTest + @Import(SecurityConfig, JwtAuthenticationFilter, JwtService, handlers) + @MockBean UserDetailsService"
    - "Endpoints de admin bajo /api/admin/** con matcher hasRole(ADMIN) antes de anyRequest()"
    - "Interceptor 401 de axios que actúa solo si todavía hay token (una sola redirección ante 401 simultáneos) e ignora /auth/*"
    - "Destino post-login solo desde router state y solo rutas internas (empieza con / y no con //)"

key-files:
  created:
    - src/main/java/com/danteautomotores/controller/AdminPublicacionController.java
    - src/main/java/com/danteautomotores/security/RestAuthenticationEntryPoint.java
    - src/main/java/com/danteautomotores/security/RestAccessDeniedHandler.java
    - src/test/java/com/danteautomotores/support/SeguridadWebMvcTestBase.java
    - src/test/java/com/danteautomotores/controller/AdminPublicacionControllerTest.java
    - src/test/java/com/danteautomotores/security/SeguridadErroresTest.java
    - "danteautomotores-front: src/utils/errores.js"
  modified:
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java
    - "danteautomotores-front: src/pages/admin/AdminDashboardPage.jsx"
    - "danteautomotores-front: src/services/api.js"
    - "danteautomotores-front: src/context/AuthContext.jsx"
    - "danteautomotores-front: src/components/ProtectedRoute.jsx"
    - "danteautomotores-front: src/pages/LoginPage.jsx"

key-decisions:
  - "listarParaAdmin() no filtra por estado y no toca buscar(): el default DISPONIBLE del catálogo público lo cambia la Fase 2"
  - "El try/catch del filtro JWT cubre solo la autenticación; doFilter queda afuera para no tapar errores de controllers"
  - "El 401 lo emite el entry point de Security (no el advice), por eso el cuerpo es fijo y sin detalle de la excepción"
  - "Sin Bearer hacia /auth/* y sin manejador de sesión vencida ante 401 de /auth/*: el login con credenciales malas no redirige"

patterns-established:
  - "SeguridadWebMvcTestBase: las subclases solo declaran @WebMvcTest(controllers=...) y sus @MockBean de services"
  - "Mensajes de error del backend: {\"error\": \"texto en español\"}; el front los lee con mensajeDeError"

requirements-completed: [ADM-04, PROD-01]

coverage:
  - id: D1
    description: "GET /api/admin/publicaciones devuelve todos los estados a un ADMIN, 403 a un COMPRADOR y 401 JSON sin token"
    requirement: ADM-04
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/controller/AdminPublicacionControllerTest.java (3 tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Token malformado, vacío, vencido o de usuario borrado nunca da 500: público sigue en 200, protegido da 401 JSON; rol insuficiente da 403 JSON"
    requirement: PROD-01
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/security/SeguridadErroresTest.java (10 tests)"
        status: pass
    human_judgment: false
  - id: D3
    description: "El panel /admin consume el endpoint admin y muestra autos RESERVADO/VENDIDO y el error legible del backend"
    requirement: ADM-04
    verification:
      - kind: other
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "No hay runner de tests en el front (QA-V2-01 diferido); el listado real depende de Postgres y del router/localStorage/CORS reales. Diferido al UAT de fin de fase."
  - id: D4
    description: "Sesión vencida en el front: 401 fuera de /auth/* limpia la sesión una sola vez, lleva a /login con aviso amarillo y vuelve al origen; login fallido no redirige"
    requirement: PROD-01
    verification:
      - kind: other
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Flujo de router + localStorage + CORS sin runner de tests en el front; requiere navegador. Diferido al UAT de fin de fase."

duration: 5min
completed: 2026-10-03
status: complete
---

# Phase 1 Plan 01: Tracer admin (listado completo + errores 401/403 uniformes) Summary

**GET /api/admin/publicaciones con todos los estados bajo matcher /api/admin/** (solo ADMIN), filtro JWT que nunca lanza, 401/403 JSON {"error"} en español y un front que lleva a /login con aviso de sesión vencida y vuelve a la página de origen.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-10-03T02:00:43Z
- **Completed:** 2026-10-03T02:05:00Z
- **Tasks:** 3 (1 tracer, 1 TDD, 1 auto)
- **Files modified:** 15 (9 en back: 6 creados y 3 modificados; 6 en front: 1 creado y 5 modificados)

## Accomplishments

- El panel admin ya no pierde los autos reservados o vendidos: lee `GET /api/admin/publicaciones` (todos los estados, orden por fecha desc) y muestra un error legible si falla (ADM-04).
- Ningún token malo produce 500: el filtro JWT atrapa `JwtException`, `IllegalArgumentException` y `UsernameNotFoundException`; en endpoints públicos la request sigue (200) y en protegidos responde 401 JSON. Rol insuficiente responde 403 JSON (PROD-01, mitad de seguridad).
- Base de tests reutilizable `SeguridadWebMvcTestBase` (SecurityConfig real, sin DB) con 13 tests verdes que las próximas olas pueden heredar.
- El front maneja la sesión vencida: interceptor 401 con una sola redirección ante 401 simultáneos, sin Bearer hacia `/auth/*`, aviso amarillo en login y vuelta al origen solo a rutas internas.

## Task Commits

Repo backend (`danteautomotores-back`):

1. **Task 1 (tracer), parte back** - `cc059b7` (feat)
2. **Task 2 RED** - `72dbc52` (test)
3. **Task 2 GREEN** - `d5be501` (feat)

Repo frontend (`danteautomotores-front`):

1. **Task 1 (tracer), parte front** - `ae71d97` (feat)
2. **Task 3** - `71d2e6a` (feat)

**Plan metadata:** commit docs en el repo back (ver `git log --grep "docs(01-01)"`).

_Nota: no hubo commit de refactor (TDD) porque no hizo falta._

## Files Created/Modified

- `src/main/java/com/danteautomotores/controller/AdminPublicacionController.java` - GET /api/admin/publicaciones
- `src/main/java/com/danteautomotores/service/PublicacionService.java` - `listarParaAdmin()` read-only, sin filtro de estado
- `src/main/java/com/danteautomotores/config/SecurityConfig.java` - matcher `/api/admin/**` ADMIN y `exceptionHandling` con los dos handlers
- `src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java` - try/catch solo sobre la autenticación
- `src/main/java/com/danteautomotores/security/RestAuthenticationEntryPoint.java` - 401 `{"error"}`
- `src/main/java/com/danteautomotores/security/RestAccessDeniedHandler.java` - 403 `{"error"}`
- `src/test/java/.../support/SeguridadWebMvcTestBase.java` - base de tests con `bearerPara` y `bearerVencido`
- `src/test/java/.../controller/AdminPublicacionControllerTest.java` - ADMIN 200, COMPRADOR 403, sin token 401
- `src/test/java/.../security/SeguridadErroresTest.java` - 10 casos de token malo / rol insuficiente
- `danteautomotores-front/src/utils/errores.js` - `mensajeDeError`
- `danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx` - consume el endpoint admin y muestra `errorListado`
- `danteautomotores-front/src/services/api.js` - interceptor de respuesta, `registrarManejadorSesionVencida`, sin Bearer a `/auth/*`
- `danteautomotores-front/src/context/AuthContext.jsx` - registra el manejador de sesión vencida
- `danteautomotores-front/src/components/ProtectedRoute.jsx` - guarda `from` en el state
- `danteautomotores-front/src/pages/LoginPage.jsx` - aviso de sesión vencida y vuelta al origen interno

## Decisions Made

- Se mantuvo `buscar()` intacto (default DISPONIBLE) y se creó un método aparte para el panel: el catálogo público se rediseña en la Fase 2.
- `api.js` repite `config.url?.startsWith('/auth/')` en request y response en vez de un helper, para respetar el criterio de aceptación literal del plan (2 o más ocurrencias).
- No se agregó el comentario `eslint-disable` de `react-hooks/exhaustive-deps`: el front no tiene configuración de ESLint (ver Issues Encountered).

## Deviations from Plan

None - plan executed exactly as written.

(Detalles menores sin impacto: el plan pedía `api.get('/admin/publicaciones')` en una sola línea, así que `cargar()` quedó con `api.get(...)` en la misma línea y `.then/.catch` debajo. La tarea 1 se comiteó como un commit por repo, tal como indica el plan.)

## Issues Encountered

- **Rama protegida:** HEAD está en `main` en ambos repos. El orquestador pidió explícitamente ejecutar secuencial sobre los working trees principales (`branching_strategy: none`, sin worktree), y los commits de planificación previos también están en `main`, así que se commiteó ahí.
- **ESLint sin configuración:** el front declara `"lint": "eslint ."` pero no tiene `eslint.config.*` ni ESLint instalado. Un intento de `npx eslint` ofreció descargar `eslint@10.12.0` en una caché temporal de npx (no toca `package.json` ni `node_modules` del proyecto) y falló por falta de config. No se instaló nada en el proyecto. El gate usado fue `npm run build`. Queda como deuda previa al plan (fuera de alcance).
- **Python no disponible** en el entorno; las ediciones se hicieron con la herramienta Edit.

## User Setup Required

None - no external service configuration required.

## Deferred Human Checks (UAT de fin de fase)

Docker/Postgres y credenciales de Cloudinary no están disponibles, así que quedan diferidos al UAT:

1. Con Postgres arriba (`docker compose up -d`), back con `ADMIN_*` seteadas y front en `localhost:5173`: ingresar como admin, ir a `/admin`, reemplazar `localStorage.token` por `"basura"` y recargar. Esperado: el panel no se rompe, se llega a `/login` con el aviso amarillo "Tu sesión venció...", tras ingresar se vuelve a `/admin` y el listado muestra también autos RESERVADO/VENDIDO.
2. En `/login`, intentar con una contraseña incorrecta. Esperado: "Email o contraseña incorrectos", sin aviso de sesión vencida ni redirección.
3. En Network, la respuesta 401 trae `{"error": ...}` y headers CORS.
4. Prohibición PROD-01 (aviso engañoso) y edge de concurrencia (varios 401 simultáneos, una sola redirección): verificadas por diseño en el código; confirmar en navegador.

## Known Stubs

None. (El `.catch(() => {})` del bloque de agencias en `AdminDashboardPage.jsx` es previo y lo reemplaza el plan 01-03.)

## Threat Flags

None. Las superficies nuevas (`/api/admin/**`, entry point 401, access-denied 403, destino post-login) están cubiertas por T-01-01 a T-01-06 del plan.

## Next Phase Readiness

- `SeguridadWebMvcTestBase`, el matcher `/api/admin/**` y `mensajeDeError` están listos para los planes 01-02 a 01-07.
- Pendiente para olas siguientes: `GlobalExceptionHandler` uniforme (las validaciones siguen devolviendo el mapa por campo actual), seed de admin/agencia única, destacados y fotos.
- Sin bloqueos.

## Self-Check: PASSED

- Archivos creados verificados en disco y commits verificados en `git log` de ambos repos (back: `cc059b7`, `72dbc52`, `d5be501`; front: `ae71d97`, `71d2e6a`).
- Criterios de aceptación de las tres tareas re-ejecutados: todos PASS.
- `mvn test` completo del back: 13 tests, 0 fallas. `npm run build` del front: OK.

---
*Phase: 01-gesti-n-del-inventario-por-el-admin*
*Completed: 2026-10-03*
