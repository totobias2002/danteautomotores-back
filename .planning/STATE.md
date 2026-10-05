---
gsd_state_version: "1.0"
current_phase: 03
current_phase_name: Cuentas verificadas
status: executing
stopped_at: Completed 03-01-PLAN.md
last_updated: "2026-10-05T18:49:36.168Z"
last_activity: 2026-10-05
last_activity_desc: Phase 03 execution started
state_head: ff7684a832a58fc219756f77df6980d732445afa
progress:
  total_phases: 6
  completed_phases: 2
  total_plans: 30
  completed_plans: 16
  percent: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-03)

**Core value:** Un usuario registrado y verificado puede encontrar un auto y hablar con la agencia para comprarlo, o cotizar el suyo, todo dentro de la web, y el admin sabe al 100 % con quién está hablando.
**Current focus:** Phase 03 — Cuentas verificadas

## Current Position

Phase: 03 (Cuentas verificadas) — EXECUTING
Plan: 2 of 15
Status: Ready to execute
Last activity: 2026-10-05 — Phase 03 execution started

Progress: [███░░░░░░░] 33%

## Performance Metrics

**Velocity:**
- Total plans completed: 15
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 7 | - | - |
| 2 | 8 | - | - |

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
| Phase 02 P01 | 9 min | 2 tasks | 21 files |
| Phase 02 P02 | 21 min | 3 tasks | 9 files |
| Phase 02 P03 | 25min | 3 tasks | 20 files |
| Phase 02 P04 | 40 min | 2 tasks | 15 files |
| Phase 02 P05 | 20min | 3 tasks | 8 files |
| Phase 02 P06 | 35min | 3 tasks | 6 files |
| Phase 02 P07 | 28 min | 3 tasks | 21 files |
| Phase 02 P08 | no registrado | 3 tasks | 0 files |
| Phase 03 P01 | 4 min | 2 tasks | 17 files |

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
- [Phase 02]: [02-01] Flyway V1/V2/V3 con baseline-version 1 y ddl-auto validate: una base de Hibernate sin historial queda como V1 y recibe solo V2 y V3; cada cambio de entidad exige su migracion
- [Phase 02]: [02-01] PostgresLocalTestBase usa una base test_* por JVM (shutdown hook) y los tests de Postgres exigen -Ddante.pg.required=true; el humo local corre contra copias descartables via con-back-local.sh
- [Phase 02]: 02-02: credenciales de Cloudinary validas = presentes y no vacias; SecretosGuard no llama a Cloudinary al arrancar
- [Phase 02]: 02-02: solo GET /actuator/health y /actuator/health/** son publicos; el demo aborta si ya hay autos (marca+modelo+anio) salvo FORZAR=1 y LIMPIAR remoto exige CONFIRMAR_BORRADO_EN_PRODUCCION=SI
- [Phase 02]: 02-03: precio anterior <= precio se acepta pero no es oferta; PublicacionMapper.esOferta es la unica regla de oferta
- [Phase 02]: 02-03: el campo se llama agenciaZona en los DTOs de publicacion; fechaVendido solo la fija cambiarEstado con Clock inyectable
- [Phase 02]: 02-04: un numero de pagina ilegible (pagina=abc) cae a 1 con un @InitBinder en PublicacionController; el resto de los parametros numericos o enum invalidos dan 400 uniforme
- [Phase 02]: 02-04: facetas agrupadas en Java sobre el conjunto visible (no dependen de los filtros activos); histograma de 16 tramos con piso exacto en BigDecimal para que la suma sea el total
- [Phase 02]: 02-05: etiquetas.js es la unica fuente de etiquetas en espanol; la card decide la oferta solo con oferta===true del servidor
- [Phase 02]: 02-06: la URL es la unica fuente de verdad del listado /autos; filtro/orden escriben con replace y vuelven a pagina 1, la pagina hace push; texto y rangos con debounce 350 ms y AbortController por cambio de URL
- [Phase 02]: 02-06: las facetas de /autos y la Home se piden una vez por visita sin recalcular con los filtros; si fallan, 'Sin opciones por ahora' (Autos) o se omiten en silencio (Home y carrusel de agencia)
- [Phase 02]: 02-07: similares = DISPONIBLES de la misma moneda, 70-130 % del precio, mismo tipo o marca (solo marca sin tipo), por cercania de precio; 4 por defecto, 1..8
- [Phase 02]: 02-07: ConsultaService rechaza con 400 las consultas sobre VENDIDO (defensa en profundidad); RESERVADO se consulta
- [Phase 02]: 02-07: solo el detalle de un VENDIDO pide y muestra Autos parecidos
- [Phase 03]: [03-01] La migracion de la fase es V5 (V4 ya es solicitudes_venta); VerificacionCuenta es la regla unica de cuenta verificada y AuthResponse suma faltantes sin DNI ni telefono

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 5]: Proveedor de precios sin elegir; la investigación del proyecto se omitió, así que se investiga al inicio de la Fase 5 (InfoAuto, ACARA, API de Mercado Libre, etc.; costo y acceso pueden condicionar la elección)
- [Phase 3]: Login con Google requiere credenciales OAuth y URIs de redirect de producción; DNI y teléfono quedan alcanzados por la Ley 25.326
- [General]: Hay 172 tests de back (Fase 1), pero no hay tests de front ni de navegador; la verificación visual de cada fase sigue siendo UAT manual
- [Phase 2]: El deploy (Railway/Vercel), el backup y la migración de la base de producción y la carga de la demo requieren credenciales del usuario: quedan como checkpoints humanos
- [Local]: Solo hay JDK 17 instalado; el back se compila con -Djava.version=17

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 261003-qde | Fix de los infos IN-03, IN-04 e IN-07 del code review de la Fase 2 | 2026-10-03 | b1ea2d6 | [261003-qde-fix-review-infos-in-03-in-04-in-07-de-la](./quick/261003-qde-fix-review-infos-in-03-in-04-in-07-de-la/) |
| 261003-sfp | Fix de IN-01, IN-06, IN-09, IN-10 y la parte del front de WR-02 del code review de la Fase 2 | 2026-10-03 | c8097e5 | [261003-sfp-fix-review-in-01-in-06-in-09-in-10-y-wr-](./quick/261003-sfp-fix-review-in-01-in-06-in-09-in-10-y-wr-/) |

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-10-05T18:49:36.093Z
Stopped at: Completed 03-01-PLAN.md
Seguridad: el 2026-10-05 se rotaron la clave de Postgres, APP_JWT_SECRET y las credenciales de Cloudinary (key dante-prod-3 con rol Master admin; el resto de las keys se borraron). Produccion verificada despues de rotar (health UP, 11 autos).
Pendientes menores de produccion: bajarle a la key de Cloudinary el rol Master admin a uno acotado si Cloudinary lo permite; revisar 3 cuentas COMPRADOR de prueba en usuarios; confirmar el Healthcheck Path /actuator/health en Railway; apagar SPRING_FLYWAY_BASELINE_ON_MIGRATE.
Fase 3: 03-CONTEXT y 03-RESEARCH listos; falta que el usuario responda 5 preguntas del research (union con Google que descarta la contrasena de cuentas sin mail confirmado, boton "Lo quiero", remitente Brevo/dominio, cierre de sesiones al cambiar clave, texto legal /privacidad) y planificar.
Integrado en esta sesion: la funcion "Vender tu auto" (solicitudes_venta) que estaba en GitHub desde el 29/09; migracion V4 creada.
Resume file: None
