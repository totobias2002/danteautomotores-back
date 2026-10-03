---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 05
subsystem: api
tags: [spring-boot, jpa, react, destacado, webmvctest, tdd, admin-panel]
status: complete

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "01-03: PublicacionService/PublicacionServiceTest con agencias, AdminDashboardPage con ABM de agencias; 01-04: formato de error {error, campos} y mensajeDeError"
provides:
  - "Publicacion.destacado (boolean not null default false, seguro para ddl-auto con filas)"
  - "PATCH /api/publicaciones/{id}/destacado con CambiarDestacadoRequest (@NotNull Boolean)"
  - "PublicacionResponse.destacado (la Home de la Fase 2 filtra destacado && estado != VENDIDO)"
  - "PublicacionControllerTest: contrato HTTP de PATCH estado/destacado (01-06 lo amplia con el reorden de fotos)"
  - "Listado del panel con toggle de estrella, select de estado en linea, filtro por estado con cantidades y busqueda sin acentos"
affects: [01-06, 01-07, fase-2-home]

requirements-completed: [ADM-04, ADM-05]

actuals:
  tokens: 14000
  tasks: 2
  commits: 2
plan_head_before: 62291d2476e4854fd4d1bcd1ec4236cc47f4dd33
plan_head_after: 13ab1456c43f1bd86e64e74444578f6137c58ee1

tech-stack:
  added: []
  patterns:
    - "Flag booleano nuevo en entidad existente: @Column(nullable=false) + @ColumnDefault(\"false\") + @Builder.Default"
    - "Mutacion de fila en el front: PATCH devuelve la publicacion y se reemplaza solo ese item en el estado (actualizarEnLista) en vez de recargar todo"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/publicacion/CambiarDestacadoRequest.java
    - src/test/java/com/danteautomotores/controller/PublicacionControllerTest.java
  modified:
    - src/main/java/com/danteautomotores/entity/Publicacion.java
    - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResponse.java
    - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/controller/PublicacionController.java
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx

key-decisions:
  - "destacado es independiente del estado: pasar a VENDIDO no lo desmarca y editar no lo toca; solo cambiarDestacado lo modifica. La Fase 2 decide como mostrar un vendido destacado"
  - "No hay tope de destacados (CONTEXT)"
  - "Filtro por estado y busqueda del lado del cliente sobre el listado completo (inventario chico asumido, RESEARCH A7)"

patterns-established:
  - "PublicacionControllerTest extiende SeguridadWebMvcTestBase y cubre 200/400/401/403 de los PATCH admin"

coverage:
  - deliverable: "PATCH /api/publicaciones/{id}/destacado persiste el flag y lo devuelve; 400 con campos, 403 comprador, 401 anonimo"
    verification:
      - kind: test
        ref: "src/test/java/com/danteautomotores/controller/PublicacionControllerTest.java"
        status: pass
      - kind: test
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java"
        status: pass
    human_judgment: false
  - deliverable: "El destacado no cambia por efecto secundario de cambiar estado ni de editar"
    verification:
      - kind: test
        ref: "PublicacionServiceTest#pasarUnAutoDestacadoAVendidoNoLoDesmarca / actualizarUnaPublicacionDestacadaNoCambiaElDestacado"
        status: pass
    human_judgment: false
  - deliverable: "Listado del panel: toggle de estrella, estado en linea, filtros con cantidades y busqueda"
    verification:
      - kind: command
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Interaccion visual en el panel; el front no tiene runner de tests. Pendiente la verificacion manual descrita en el plan (estrella persiste tras recargar, vendido sigue destacado, filtros/busqueda sin acentos, error legible con el back apagado)"

duration: 14 min
completed: 2026-10-03
---

# Phase 1 Plan 05: Destacados y gestion de estado en el listado del panel Summary

**Flag `destacado` independiente del estado con PATCH admin-only y listado del panel con estrella, estado en linea, filtros por estado con cantidades y busqueda sin acentos.**

## Performance

- **Duration:** ~14 min
- **Tasks:** 2 (Tarea 1 con ciclo TDD RED -> GREEN)
- **Files modified:** 9 (7 back + 1 test nuevo + 1 front)

## Accomplishments

- Backend: `Publicacion.destacado` (`@ColumnDefault("false")` + `@Builder.Default`), `CambiarDestacadoRequest` validado, `PublicacionService.cambiarDestacado`, `PATCH /api/publicaciones/{id}/destacado`, y `destacado` en `PublicacionResponse` via mapper. La seguridad la cubre el matcher PATCH `/api/publicaciones/**` -> ADMIN ya existente.
- Tests: 6 casos nuevos de servicio (marcar, desmarcar, id inexistente, VENDIDO no desmarca, actualizar no toca, default false) y `PublicacionControllerTest` con 5 casos HTTP (200, 400 con `campos.destacado`, 403, 401, PATCH estado 200). Suite completa del back: 69 tests verdes.
- Front: estrella con `aria-pressed` y badge "Destacado", select de estado que reemplaza solo la fila (`actualizarEnLista`), barra de filtros Todos/Disponibles/Reservados/Vendidos con cantidades, buscador por marca/modelo sin mayusculas ni acentos, `errorAccion` visible y mensaje "No hay autos que coincidan con el filtro.". Build del front verde.

## Task Commits

1. **Tarea 1 RED: tests de destacado y contrato HTTP** - `bf8a432` (test, back)
2. **Tarea 1 GREEN: destacado de punta a punta** - `13ab145` (feat, back)
3. **Tarea 2: toggle, filtros y busqueda en el panel** - `103cdf9` (feat, repo danteautomotores-front)

`commits: 2` mide solo el repo back (`rev-list` desde `plan_head_before`); el commit del front es del repo hermano.

## Files Created/Modified

Ver `key-files` en el frontmatter.

## Decisions Made

Ver `key-decisions`. El flag es independiente del estado; la Home de la Fase 2 debe filtrar `destacado && estado != VENDIDO`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking/criterio de aceptacion] Forma de la definicion de `actualizarEnLista`**
- **Found during:** Tarea 2, verificacion de criterios de aceptacion
- **Issue:** el criterio pide `grep -c 'actualizarEnLista('` >= 3 (definicion y dos usos); con `const actualizarEnLista = (data) =>` la definicion no matcheaba (contaba 2)
- **Fix:** se declaro como `function actualizarEnLista(data) { ... }`; mismo comportamiento
- **Files modified:** AdminDashboardPage.jsx
- **Commit:** `103cdf9`

**2. [Rule 3 - criterio de aceptacion] Un solo "Buscar por marca o modelo"**
- **Issue:** el criterio pide exactamente 1 aparicion; habia placeholder y `aria-label` duplicados
- **Fix:** se dejo solo el placeholder (el input esta dentro de un `<label>` con el icono)
- **Commit:** `103cdf9`

**Total deviations:** 2 menores (ajustes de forma para cumplir criterios de aceptacion). **Impact:** ninguno funcional.

## Issues Encountered

- La rama de trabajo es `main` (el proyecto trabaja sin ramas por fase, `branching_strategy: none`, ejecucion secuencial indicada por el orquestador); la asercion de rama protegida se omitio por instruccion explicita del orquestador.
- La verificacion visual (`human-check` de la Tarea 2) queda pendiente para la verificacion de fase: no hay runner de tests en el front.

## Known Stubs

None.

## Threat Flags

None. T-01-18 (EoP en PATCH /destacado) y T-01-19 (body sin el campo) mitigados y cubiertos por `PublicacionControllerTest`.

## Self-Check: PASSED

- Archivos creados existen (CambiarDestacadoRequest.java, PublicacionControllerTest.java); commits `bf8a432`, `13ab145` (back) y `103cdf9` (front) presentes.
- Criterios de aceptacion de ambas tareas re-ejecutados y en verde; `mvn test` completo: 69 tests, 0 fallas; `npm run build` del front en verde.

## Next Phase Readiness

Listo para 01-06 (amplia `PublicacionControllerTest` con el reorden de fotos) y 01-07 (cambia `eliminarPublicacion` en el mismo AdminDashboardPage).
