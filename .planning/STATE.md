---
gsd_state_version: "1.0"
current_phase: 1
current_phase_name: Gestión del inventario por el admin
status: executing
stopped_at: Phase 2 context gathered
last_updated: "2026-10-03T07:55:29.469Z"
last_activity: 2026-10-02
last_activity_desc: Phase 1 execution started
state_head: 2d44033d9bfda3b5bac632c462976db53248cd9a
progress:
  total_phases: 6
  completed_phases: 0
  total_plans: 7
  completed_plans: 7
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-02)

**Core value:** Un usuario registrado y verificado puede encontrar un auto y hablar con la agencia para comprarlo, o cotizar el suyo, todo dentro de la web, y el admin sabe al 100 % con quién está hablando.
**Current focus:** Phase 1 — Gestión del inventario por el admin

## Current Position

Phase: 1 (Gestión del inventario por el admin) — EXECUTING
Plan: 7 of 7
Status: Ready to execute
Last activity: 2026-10-02 — Phase 1 execution started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 1 P01 | 5 min | 3 tasks | 15 files |
| Phase 1 P02 | 3 min | 2 tasks | 7 files |
| Phase 1 P03 | 7 min | 2 tasks | 7 files |
| Phase 1 P04 | 6 min | 2 tasks | 3 files |
| Phase 01 P05 | 14 min | 2 tasks | 9 files |
| Phase 1 P06 | 25 min | 3 tasks | 12 files |
| Phase 1 P07 | 17 min | 3 tasks | 10 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Proyecto en modo MVP vertical: cada fase cubre back (`danteautomotores-back`) + front (`danteautomotores-front`) de punta a punta
- [Roadmap]: El admin va antes que el catálogo público (el panel admin ya usa la API real; los mocks están solo en las páginas públicas)
- [Roadmap]: El deploy a producción (PROD-04) entra en la Fase 2; desde ahí el esquema usa migraciones versionadas y las fases siguientes se verifican en producción
- [Roadmap]: El modelo de conversaciones (Fase 4) contempla el tipo "cotización" desde el inicio, para la Fase 5
- [Phase 1]: [01-01] listarParaAdmin() es un metodo aparte; buscar() conserva el default DISPONIBLE (lo cambia la Fase 2)
- [Phase 1]: [01-01] Filtro JWT: try/catch solo sobre la autenticacion; 401/403 JSON los emiten RestAuthenticationEntryPoint/RestAccessDeniedHandler con cuerpo fijo
- [Phase 1]: [01-01] Front: sin Bearer hacia /auth/*; el interceptor 401 ignora /auth/* y solo actua si hay token (una sola redireccion)
- [Phase 1]: [01-02] El seed no sincroniza ni promueve: con ADMIN existente no toca nada y un ADMIN_EMAIL ya registrado falla (prod) o avisa
- [Phase 1]: [01-02] DataSeeder siembra la agencia inicial solo si no hay ninguna (multiples agencias soportadas); Rol.ADMIN solo aparece en DataSeeder
- [Phase 1]: 01-03: una agencia con autos no se puede eliminar (400 con mensaje accionable via existsByAgenciaId); AgenciaService.listar() ordena por id
- [Phase 1]: [01-04] Formato de error uniforme: {error} y en validacion {error:'Datos invalidos', campos:{...}}; un unico advice que extiende ResponseEntityExceptionHandler, sin @ExceptionHandler propio para excepciones de Spring MVC
- [Phase 1]: [01-04] ServicioExternoException -> 502 con su mensaje; DataIntegrityViolation -> 409 fijo; 500 generico sin detalle interno (solo en log)
- [Phase 01]: 01-05: destacado es independiente del estado (VENDIDO no lo desmarca, editar no lo toca); Fase 2 filtra destacado && estado != VENDIDO. Sin tope de destacados.
- [Phase 1]: 01-06: fotos validadas por magic bytes (JPEG/PNG/WebP), tope 10 MB y 10 por auto; reorden por flechas + Hacer portada (orden 0 = portada) via PUT /fotos/orden con lista completa de ids
- [Phase 1]: [01-07] Borrado de publicacion en cascada (favoritos+consultas, una transaccion) con aviso de conteo via GET /api/admin/publicaciones/{id}/impacto-eliminacion; assets de Cloudinary solo en afterCommit; ConfirmDialog nativo

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 5]: Proveedor de precios sin elegir; la investigación del proyecto se omitió, así que se investiga al inicio de la Fase 5 (InfoAuto, ACARA, API de Mercado Libre, etc.; costo y acceso pueden condicionar la elección)
- [Phase 3]: Login con Google requiere credenciales OAuth y URIs de redirect de producción; DNI y teléfono quedan alcanzados por la Ley 25.326
- [General]: Sin tests automatizados (QA-V2-01 está diferido a v2); la verificación de cada fase es manual/UAT

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-10-03T07:55:29.414Z
Stopped at: Phase 2 context gathered
Resume file: .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-CONTEXT.md
