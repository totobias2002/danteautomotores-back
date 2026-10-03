---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 03
subsystem: api
tags: [spring-boot, jpa, mockito, webmvctest, react, agencias, multi-agencia]
status: complete

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "Plan 01-01: SeguridadWebMvcTestBase, errores 401/403 uniformes y utils/errores.js mensajeDeError"
provides:
  - "PublicacionRepository.existsByAgenciaId(Long): boolean"
  - "AgenciaService.eliminar con baja segura (400 si la agencia tiene autos, nunca 500 por la FK) y listar() ordenado por id"
  - "PublicacionServiceTest: base Mockito que amplian 01-05, 01-06 y 01-07"
  - "AgenciaServiceTest y AgenciaControllerTest (contrato HTTP y de seguridad de agencias)"
  - "Front: panel con errores del backend en agencias y publicaciones, baja de agencia con confirmacion inline"
affects: [01-04, 01-05, 01-06, 01-07]

actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: 0bcc31184e007780e359a2a54d164544b3c6c836
plan_head_after: c9b54ac58e40972c6948dc1811d5e694306a07c8

tech-stack:
  added: []
  patterns:
    - "Tests de service con Mockito puro (@ExtendWith(MockitoExtension), @InjectMocks) y SecurityContextHolder con TestingAuthenticationToken"
    - "Tests @WebMvcTest que extienden SeguridadWebMvcTestBase y asertan solo status cuando el cuerpo lo define otro plan"
    - "Confirmacion de baja inline en la fila (estado agenciaAEliminar), sin window.confirm"

key-files:
  created:
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - src/test/java/com/danteautomotores/service/AgenciaServiceTest.java
    - src/test/java/com/danteautomotores/controller/AgenciaControllerTest.java
  modified:
    - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
    - src/main/java/com/danteautomotores/service/AgenciaService.java
    - "danteautomotores-front: src/pages/admin/AdminDashboardPage.jsx"
    - "danteautomotores-front: src/pages/admin/AdminPublicacionFormPage.jsx"

key-decisions:
  - "Se conserva el soporte de varias agencias (D-05..D-07 revisados): no se toca agenciaId, POST/DELETE ni el selector del form"
  - "Una agencia con autos no se borra: IllegalArgumentException (400) con mensaje accionable, chequeo con existsByAgenciaId antes de deleteById"
  - "AgenciaService.listar() ordena por id con Sort.by(\"id\") para orden estable en panel y selector"

patterns-established:
  - "Baja segura: chequear dependientes con existsBy* y lanzar IllegalArgumentException en vez de dejar que la FK explote"

requirements-completed: [ADM-02]

coverage:
  - id: D1
    description: "Eliminar una agencia con publicaciones da 400 y no borra; sin publicaciones se elimina (204); inexistente da 404"
    requirement: ADM-02
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AgenciaServiceTest.java (4 tests)"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AgenciaControllerTest.java#eliminarUnaAgenciaConAutosDa400"
        status: pass
    human_judgment: false
  - id: D2
    description: "POST, PUT y DELETE de /api/agencias son solo ADMIN (COMPRADOR 403, anonimo 401); GET publico"
    requirement: ADM-02
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AgenciaControllerTest.java (9 tests)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Crear/actualizar una publicacion con agenciaId valido la deja en esa agencia (y la mueve al editar); con uno inexistente da 404 sin guardar"
    requirement: ADM-02
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java (3 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "El panel y el form muestran el mensaje de error del backend y la baja de agencia pide confirmacion inline"
    requirement: ADM-02
    verification:
      - kind: command
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "El front no tiene runner de tests; el flujo real (segunda agencia, mover un auto, baja con y sin autos) requiere back + Postgres + navegador"
---

# Phase 1 Plan 03: ABM de agencias multi-agencia y errores visibles en el panel Summary

**Baja segura de agencias (400 en vez de 500 por la FK `publicaciones.agencia_id`), listado ordenado por id, tests Mockito/WebMvc de agencias y publicaciones, y panel que muestra los errores del backend con confirmacion inline de baja.**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-10-03T02:19Z
- **Completed:** 2026-10-03T02:26Z
- **Tasks:** 2 (Tarea 1 en ciclo TDD RED/GREEN)
- **Files modified:** 7 (5 en back, 2 en front)

## Accomplishments

- `AgenciaService.eliminar` lanza `IllegalArgumentException("No se puede eliminar la agencia porque tiene autos publicados. Pasalos a otra agencia o eliminalos primero.")` si `publicacionRepository.existsByAgenciaId(id)`; el handler existente la mapea a 400 (T-01-28).
- `AgenciaService.listar()` usa `findAll(Sort.by("id"))`: orden estable en el panel y el selector.
- `PublicacionServiceTest` (3 tests) fija que crear/actualizar respetan el `agenciaId` del request y que uno inexistente da `ResourceNotFoundException` sin guardar (T-01-13); queda como base para 01-05, 01-06 y 01-07.
- `AgenciaControllerTest` (9 tests) cubre GET publico, POST/PUT/DELETE como ADMIN, 403 para COMPRADOR, 401 sin token y 400 en la baja rechazada (T-01-12).
- Front: `AdminDashboardPage` usa `mensajeDeError` en alta, edicion, baja y carga de agencias, con estado `errorAgencias` visible sobre la lista y confirmacion inline "¿Eliminar? Confirmar / Cancelar" (sin `window.confirm`); `AdminPublicacionFormPage` muestra el error del backend al guardar, al cargar la publicacion y al cargar agencias. Se conserva el CRUD de agencias, el selector "¿Quien lo vende?" y `({p.agenciaNombre})` en el listado.

## Task Commits

1. **Tarea 1 RED: tests de agencias y publicaciones + `existsByAgenciaId`** - `27a93a0` (test, back)
2. **Tarea 1 GREEN: baja segura y listado ordenado** - `c9b54ac` (feat, back)
3. **Tarea 2: errores del backend y confirmacion de baja en el panel** - `7c5b994` (feat, repo `danteautomotores-front`)

**Plan metadata:** commit docs(01-03) a continuacion.

## Files Created/Modified

- `src/main/java/com/danteautomotores/repository/PublicacionRepository.java` - `existsByAgenciaId(Long)`
- `src/main/java/com/danteautomotores/service/AgenciaService.java` - baja segura, `listar()` ordenado
- `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java` - 3 tests Mockito
- `src/test/java/com/danteautomotores/service/AgenciaServiceTest.java` - 4 tests Mockito
- `src/test/java/com/danteautomotores/controller/AgenciaControllerTest.java` - 9 tests WebMvc
- `danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx` - errores del backend, confirmacion inline
- `danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx` - errores del backend

## Decisions Made

Ver `key-decisions` del frontmatter. Se respeta la revision de D-05..D-07 (varias agencias gestionadas por el unico admin).

## TDD Gate Compliance

- RED (`27a93a0`): `AgenciaServiceTest` fallo por las razones esperadas: `eliminarUnaAgenciaConAutosPublicadosLanzaErrorYNoBorraNada` ("Expecting code to raise a throwable"), `listarPidePorIdParaTenerUnOrdenEstable` (`findAll()` en vez de `findAll(Sort)`) y `eliminarUnaAgenciaSinAutosLaBorra` (stub de `existsByAgenciaId` sin usar). Para que compilara, el commit RED incluye la firma `existsByAgenciaId` en el repositorio (interfaz, sin comportamiento).
- GREEN (`c9b54ac`): suite completa verde (43 tests, 0 fallas).
- `PublicacionServiceTest` y `AgenciaControllerTest` son tests de caracterizacion: el comportamiento ya existia (el plan indica no tocar `PublicacionService`, `AgenciaController` ni `SecurityConfig`), por lo que pasaron desde el primer momento y fijan el contrato.

## Deviations from Plan

None - plan executed exactly as written.

Nota de proceso (no es desviacion del plan): `target/classes` y `target/test-classes` (ignorados por git) tenian clases compiladas con Java 21 (class version 65) que rompian el build offline con JDK 17; se borraron y se recompilo. En una segunda corrida aparecieron otra vez clases 65 en `target/test-classes` (probablemente un IDE u otro proceso compilando con JDK 21) y se borraron de nuevo. Quien corra Maven con JDK 17 debe limpiar esos directorios si ve `class file has wrong version 65.0`.

**Total deviations:** 0.

## Issues Encountered

Solo el tema de clases compiladas con otra version de Java descrito arriba.

## User Setup Required

None.

## Verification

- `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest,AgenciaServiceTest,AgenciaControllerTest`: 16 tests (3 + 4 + 9), 0 fallas (verificado en RED y GREEN).
- Suite completa: 43 tests, 0 fallas, `BUILD SUCCESS`.
- `npm --prefix danteautomotores-front run build`: verde (`built in 1.78s`).
- Acceptance criteria: `existsByAgenciaId` en repo = 1; en `AgenciaService` = 1; `agenciaId` en `PublicacionRequest` = 1; `@Post/Put/DeleteMapping` en `AgenciaController` = 3; `agenciaId` en el form = 4; `mensajeDeError(` en form = 3 y en dashboard = 5; `window.confirm` en dashboard = 0.
- Pendiente (humano, back + Postgres + navegador): crear una segunda agencia, crear una publicacion en ella, moverla de agencia al editar, recargar, e intentar eliminar una agencia con autos (debe mostrar el mensaje del backend) y otra sin autos.

## Known Stubs

None.

## Threat Flags

None. Mitigadas segun el threat_model: T-01-12 (AgenciaControllerTest 403/401), T-01-13 (PublicacionServiceTest 404 sin guardar), T-01-14 (React escapa; sin `dangerouslySetInnerHTML`), T-01-28 (chequeo `existsByAgenciaId`).

## Next Phase Readiness

Listo para 01-04 (reescribe el `GlobalExceptionHandler`; `AgenciaControllerTest` asserta solo el status del 400, asi que no se rompe) y para 01-05..01-07, que amplian `PublicacionServiceTest`.

## Self-Check: PASSED

- FOUND: PublicacionServiceTest.java, AgenciaServiceTest.java, AgenciaControllerTest.java, AgenciaService.java, PublicacionRepository.java
- FOUND commits: 27a93a0, c9b54ac (back), 7c5b994 (front)
