---
phase: quick-261003-qde
plan: 01
subsystem: catalogo-publico, front-router, docs
tags: [review-fix, fotos, portada, 404, readme, postgres-tests]
requires: []
provides:
  - PublicacionMapper.ORDEN_DE_FOTOS (unico comparador total de fotos)
  - NoEncontradaPage (404) con ruta catch-all
  - README coherente sobre tests de Postgres
affects: [PublicacionMapper, Publicacion, PublicacionService, AppRouter, AdminDashboardPage, README]
tech-stack:
  added: []
  patterns: ["un unico comparador para toda regla de orden que decide una portada"]
key-files:
  created:
    - ../danteautomotores-front/src/pages/NoEncontradaPage.jsx
  modified:
    - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
    - src/main/java/com/danteautomotores/entity/Publicacion.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java
    - src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java
    - ../danteautomotores-front/src/routes/AppRouter.jsx
    - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
    - README.md
    - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md
decisions:
  - "ORDEN_DE_FOTOS: orden con null = 0 y desempate por id (null al final); lo usan toResponse, toResumen y resecuenciarFotos"
  - "El panel admin usa fotos[0] del back como miniatura en lugar de buscar orden 0"
  - "NoEncontradaPage no lee ni muestra la ruta pedida (T-qde-01)"
requirements-completed: [CAT-01, ADM-03, PROD-04]
status: complete
commits: 2
plan_head_before: 3896452aa799131e60bd55010a7d1bdb9223ee77
plan_head_after: 1fdb309e4b7020740c8912820d6cb73481b62e59
actuals:
  tokens: 4950
  tasks: 3
  commits: 4
completed: 2026-10-03
---

# Quick 261003-qde: IN-03, IN-04 e IN-07 del code review de la Fase 2

Orden total de fotos con un unico comparador (`ORDEN_DE_FOTOS`) para que la portada de la card y la del detalle coincidan siempre, pagina 404 con ruta catch-all en el front, y README sin contradiccion sobre los tests de Postgres.

Nota sobre `commits`: el frontmatter `commits: 2` / `plan_head_*` mide solo el repo back (2 commits de codigo). Sumando los 2 commits del repo front, el total de la tarea es 4 (`actuals.commits: 4`).

## Tareas

| Tarea | Repo | Commit | Descripcion |
|-------|------|--------|-------------|
| 1 (tracer, TDD) | back | 74d7f9a | `fix(02): IN-04` comparador `ORDEN_DE_FOTOS`, `@OrderBy("orden ASC, id ASC")`, tests unitarios y contra Postgres |
| 2A | front | 0d0af3d | `fix(02): IN-07` `NoEncontradaPage` + `<Route path="*">` ultima |
| 2B | front | d3a72f7 | `fix(02): IN-04` `fotoDePortada` devuelve `fotos[0]` |
| 3A | back | 1fdb309 | `docs(02): IN-03` README: seccion Tests reescrita, bullet previo al deploy, sin ruta personal |
| 3B | back | (sin commit) | `02-REVIEW-DISPOSITION.md`: IN-03/04/07 `fixed` (quick 261003-qde), `open: 7`; edicion dejada sin commitear para el commit de docs del orquestador (instruccion del orquestador) |

## Verificacion

- RED confirmado: con los tests nuevos de `PublicacionMapperTest`, fallaban 2 (portada con empate e id null) antes de tocar el codigo.
- `mvn -B -o -Djava.version=17 test -Ddante.pg.required=true`: 301 tests, 0 fallas, 0 salteados (incluye `CatalogoPostgresTest` 30, con el caso nuevo, y `MigracionesPostgresTest` 7: el `@OrderBy` no rompe `ddl-auto: validate`).
- Front: `npm run build` en verde en ambos commits; gates grep de la Task 2 y de la Task 3 (README y disposicion) en verde.
- No se hizo el human-check visual de la 404 en el front de prueba (puerto 5174); queda para la verificacion end-of-phase.

## Deviations from Plan

- **Disposicion sin commitear (instruccion del orquestador):** el plan pedia el commit `docs(02): IN-03, IN-04 e IN-07 resueltos...`; por la restriccion del orquestador, la edicion de `02-REVIEW-DISPOSITION.md` quedo en el working tree para su commit de docs. `updated:` se fijo con la hora real UTC actual (2026-10-03T22:10:49Z).
- Por lo demas, el plan se ejecuto tal como estaba escrito. No hubo fixes automaticos de reglas 1-3.

## Known Stubs

None.

## Threat Flags

None (T-qde-01..05 mitigados segun el threat model: la 404 no refleja la ruta, un comparador unico, README exige `-Ddante.pg.required=true`).

## Self-Check: PASSED

- FOUND: src/main/java/com/danteautomotores/mapper/PublicacionMapper.java (ORDEN_DE_FOTOS), ../danteautomotores-front/src/pages/NoEncontradaPage.jsx
- Commits back 74d7f9a, 1fdb309 y front 0d0af3d, d3a72f7 presentes en `git log`.
