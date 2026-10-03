---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 01
subsystem: catalogo-publico
tags: [flyway, postgres, spring-boot, jpa, react, cloudinary]

requires:
  - phase: 01-panel-admin
    provides: campo destacado independiente del estado, public_id de fotos, API admin real
provides:
  - Esquema versionado con Flyway (V1 original, V2 Fase 1, V3 Fase 2) y Hibernate en ddl-auto validate
  - GET /api/publicaciones/destacados (publico, limite 1..12, sin vendidos) con PublicacionResumenResponse
  - CatalogoService (lectura del catalogo publico, separado de PublicacionService)
  - PostgresLocalTestBase y arnes de humo local (con-back-local.sh, catalogo-humo.js)
  - Home con destacados reales y PublicacionCard con miniaturas de Cloudinary
affects: [02-02, 02-03, 02-04, 02-05, 02-06, 02-07, 02-08]

plan_head_before: c6d99cf407d3b848970416d02a84827c18977b6c
plan_head_after: 2b02ecc2c0b8b6b58aeb08cdf75e6c3f5a8666e7

actuals:
  tokens: 12400   # chars/4 sobre las lineas agregadas (back 47.660 chars + front 1.767 chars)
  tasks: 2
  commits: 3      # MEASURED en el repo back (rev-list plan_head_before..HEAD); ademas 1 commit en el repo front (6476435)

tech-stack:
  added: [org.flywaydb:flyway-core 10.10.0 (BOM), org.flywaydb:flyway-database-postgresql 10.10.0 (BOM, runtime)]
  patterns:
    - "Esquema solo por migraciones: cada cambio de entidad necesita su Vn+1; validate aborta el arranque si falta"
    - "DTO resumido para listados publicos (sin descripcion, fotos ni admin) mapeado dentro de la transaccion readOnly"
    - "Tests contra Postgres local con bases descartables test_* (Assumptions salvo -Ddante.pg.required=true)"

key-files:
  created:
    - src/main/resources/db/migration/V1__esquema_original.sql
    - src/main/resources/db/migration/V2__fase1_destacado_y_public_id.sql
    - src/main/resources/db/migration/V3__fase2_catalogo.sql
    - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResumenResponse.java
    - src/main/java/com/danteautomotores/service/CatalogoService.java
    - src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java
    - src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java
    - src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java
    - scripts/verify/con-back-local.sh
    - scripts/verify/catalogo-humo.js
    - .gitattributes
  modified:
    - pom.xml
    - src/main/resources/application.yml
    - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
    - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
    - src/main/java/com/danteautomotores/controller/PublicacionController.java
    - src/test/java/com/danteautomotores/support/SeguridadWebMvcTestBase.java
    - .gitignore
    - "danteautomotores-front: src/pages/HomePage.jsx, src/components/PublicacionCard.jsx, src/utils/cloudinary.js"

key-decisions:
  - "Flyway V1 = esquema previo a la Fase 1 con baseline-version 1: una base creada por Hibernate se marca V1 sin ejecutarla y recibe solo V2 y V3"
  - "Una sola base descartable test_* por JVM para todas las clases que extienden PostgresLocalTestBase (borrada en un shutdown hook), porque Spring cachea el contexto entre clases"
  - "El humo corre contra copias descartables de bases locales; nunca contra la base de desarrollo ni contra produccion"

patterns-established:
  - "CatalogoService: servicio de solo lectura del catalogo publico; los planes 02-04 y 02-07 le suman listado, facetas y similares"
  - "scripts/verify/con-back-local.sh [--copia-de <origen> | --vacia] <base> <comando>: levanta el back local contra una base descartable y exporta API al comando"

requirements-completed: [CAT-03, PROD-04]

coverage:
  - id: D1
    description: "GET /api/publicaciones/destacados devuelve hasta 6 (acotado 1..12) destacados no vendidos, estado NULL visible, orden fecha desc e id desc, con fotoPortada de menor orden"
    requirement: CAT-03
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java (5 tests, Postgres real)"
        status: pass
      - kind: integration
        ref: "bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_tracer node scripts/verify/catalogo-humo.js (humo: 5 ok, 0 fallas)"
        status: pass
    human_judgment: false
  - id: D2
    description: "El DTO publico no expone admin, email, passwordHash, descripcion ni fotos"
    requirement: CAT-03
    verification:
      - kind: unit
        ref: "CatalogoPostgresTest#elResumenSerializadoNoExponeAdminNiDescripcionNiFotos + chequeo de claves del humo"
        status: pass
    human_judgment: false
  - id: D3
    description: "El back arranca con Flyway (V1, V2, V3) y ddl-auto validate; una base existente sin historial queda en baseline V1 y recibe V2 y V3 sin perder filas; idempotente y seguro con dos migrate() simultaneos"
    requirement: PROD-04
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java (7 tests, Postgres real)"
        status: pass
      - kind: integration
        ref: "con-back-local.sh --copia-de danteautomotores: baselined v1, applied 2 migrations, now at v3"
        status: pass
    human_judgment: false
  - id: D4
    description: "La Home muestra los destacados reales con miniaturas de Cloudinary (c_fill,w_640,h_420,q_auto,f_auto) y 'Ver todos' lleva a /autos"
    requirement: CAT-03
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build (ok, 1982 modules)"
        status: pass
    human_judgment: true
    rationale: "No hay runner de navegador: el render real de las cards y las URLs de Cloudinary se ven en el navegador (human-check del plan, pendiente)"

duration: 9min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 01: Destacados reales sobre un esquema versionado con Flyway Summary

**Flyway (V1/V2/V3) con Hibernate en `validate` y baseline-version 1 para la base de produccion existente, `GET /api/publicaciones/destacados` con DTO resumido sin datos de usuarios, y la Home del front consumiendo esos destacados con miniaturas de Cloudinary.**

## Performance

- **Duration:** ~9 min
- **Completed:** 2026-10-03
- **Tasks:** 2 (tracer + TDD)
- **Files modified:** 18 en el back, 3 en el front

## Accomplishments

- Tracer completo de punta a punta: esquema Flyway -> entidad validada -> repositorio -> `CatalogoService` -> endpoint publico -> `HomePage`/`PublicacionCard`. Se eliminaron `USE_MOCK_DATA` y `destacadosMock` de la Home.
- Migraciones seguras para la base de produccion: una base creada por Hibernate sin historial queda como baseline V1, recibe V2 y V3, conserva las seis tablas intactas, y los autos ya VENDIDO reciben `fecha_vendido`. Re-migrar ejecuta 0 migraciones; dos `migrate()` en paralelo aplican cada version una sola vez; sin baseline Flyway se niega ("non-empty schema").
- Arnes de verificacion local: `con-back-local.sh` (copia o base vacia descartable, jar copiado, back levantado y apagado, `API` exportada) y `catalogo-humo.js` (solo localhost).
- Yml base seguro: `ddl-auto: validate`, `show-sql: false`, `default_batch_fetch_size: 50`, actuator solo `health`, `app.catalogo.dias-vendido-visible`.
- Suite completa del back verde: 184 tests, 0 fallas, 0 skipped (con `-Ddante.pg.required=true`).

## Task Commits

1. **Task 1 (tracer): destacados reales sobre esquema Flyway** - back `b1df2d8` (feat), front `6476435` (feat)
2. **Task 2 (TDD): migraciones seguras V3, baseline, idempotencia, concurrencia** - `2d15cab` (test, RED: 4 de 7 fallaban por falta de V3), `2b02ecc` (feat, GREEN: 7/7)

**Plan metadata:** commit docs(02-01) a continuacion (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Decisions Made

- Se respeto la decision de ROADMAP de pasar a Flyway con `validate` (reversibilidad "costly" ya asumida).
- Base compartida por JVM en `PostgresLocalTestBase` (ver desvio 1).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Base descartable por JVM en vez de por clase en PostgresLocalTestBase**
- **Found during:** Task 1 (diseno de la base de tests)
- **Issue:** el plan pedia crear la base en un `@DynamicPropertySource` y borrarla en un `@AfterAll` por clase. Spring cachea el contexto entre clases con el mismo conjunto de propiedades dinamicas: la segunda clase que extienda la base (planes 02-04, 02-07) reutilizaria un contexto apuntando a una base ya borrada.
- **Fix:** una sola base `test_*` por JVM (creada de forma perezosa) que se borra en un shutdown hook; los helpers estaticos (`crearBaseDescartable`, `borrarBase`, etc.) quedan como pidio el plan y los usa `MigracionesPostgresTest`. Se verifico que no quedan bases `test_*` huerfanas.
- **Files modified:** src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java
- **Commit:** b1df2d8

**2. [Rule 3 - Blocking] Resolucion de dependencias Flyway sin `-o`**
- **Found during:** Task 1
- **Issue:** `mvn -o` no resolvia `jackson-dataformat-toml:2.17.2` (dependencia transitiva oficial de flyway-core).
- **Fix:** una corrida online de `mvn test-compile` (como preveia el plan) que bajo solo ese artefacto de Maven Central; sin cambios de versiones.
- **Commit:** n/a (cache local de ~/.m2)

**3. [Rule 1 - Bug] Comentarios de V1/V2 contenian el texto "ddl-auto: update"**
- **Found during:** Task 1 (criterios de aceptacion)
- **Issue:** el criterio `! grep -rnE "ddl-auto: update|show-sql: true" src/main/resources` fallaba por los comentarios copiados literalmente de la investigacion.
- **Fix:** los comentarios dicen ahora `ddl-auto=update` (solo comentarios, antes de que la migracion se haya aplicado en ninguna base, sin efecto en el checksum de nada existente).
- **Files modified:** V1__esquema_original.sql, V2__fase1_destacado_y_public_id.sql
- **Commit:** b1df2d8

**4. [Rule 3 - Blocking] `.gitattributes` creado en el commit de la tarea 1 (el plan lo ubica en la tarea 2)**
- **Found during:** Task 1
- **Issue:** `con-back-local.sh` y las migraciones V1/V2 se commitean en la tarea 1; sin `.gitattributes` con `eol=lf` Windows podia guardarlas con CRLF.
- **Fix:** se adelanto la creacion de `.gitattributes`; el criterio de la tarea 2 (`eol: lf`) se cumple igual.
- **Commit:** b1df2d8

**5. [Rule 1 - Bug] `catalogo-humo.js` usa `process.exitCode` en vez de `process.exit`**
- **Found during:** Task 1 (verificacion de punta a punta)
- **Issue:** en Windows `process.exit()` con conexiones `fetch` pendientes aborta Node con un assert de libuv (`UV_HANDLE_CLOSING`).
- **Fix:** `process.exitCode`. El codigo de salida sigue siendo 1 ante fallas.
- **Commit:** b1df2d8

### Other notes (not code deviations)

- **Human-check del tracer pendiente.** El `<human-check>` de la tarea 1 (abrir la Home en el navegador, ver las cards y las URLs `res.cloudinary.com/.../c_fill,w_640,h_420,q_auto,f_auto/...`) no se puede hacer en esta corrida autonoma (no hay navegador). Queda para el UAT de la fase. Cubierto en su lugar: API verificada de punta a punta, `npm run build` verde. El `<verify>` automatizado del tracer se re-corrio completo y paso (gate del tracer superado sin expandir sobre una base rota).
- **Humo sobre la copia de `danteautomotores`:** la base de desarrollo tiene 1 auto y 0 destacados, asi que los chequeos por item dieron SKIP (2 ok, 0 fallas, 3 skip, como preve el plan). Para ejercitar los chequeos por item se corrio ademas el humo sobre una copia de `dante_uat` (11 autos, 6 destacados): 5 ok, 0 fallas, 0 skip. Ambas copias son descartables (`dante_copia_tracer`, borrada al terminar); `danteautomotores`, `dante_uat*` no se modificaron (la de desarrollo sigue sin `flyway_schema_history`).
- **ESLint del front:** `npx eslint` falla porque el front no tiene `eslint.config.*` (preexistente, fuera de alcance).
- **Fix del orden del ledger:** el ledger de commits (`gsd-plan-head-before-02-01`) se creo despues del primer commit con el padre de ese commit (`c6d99cf`, el HEAD previo exacto), por lo que el conteo medido (3) es correcto.

**Total deviations:** 5 auto-fixed (3 Rule 1, 2 Rule 3). **Impact:** ninguno sobre el alcance; todos los criterios de aceptacion se cumplen.

## Known Stubs

- `src/pages/HomePage.jsx`: las sugerencias del autocompletado del hero y las bandas de precio siguen leyendo `src/mocks/catalogoMock.js`. Es intencional segun el plan (hueco de funcionalidad, no de arquitectura); lo resuelve el plan 02-06 (pasa a `facetas`). No impide el objetivo de este plan (destacados reales).

## Threat Flags

None - no hay superficie nueva fuera del threat_model del plan. (`management.endpoints.web.exposure.include: health` queda configurado; el `permitAll` del healthcheck lo agrega 02-02.)

## Verification Results

| Check | Resultado |
|-------|-----------|
| `mvn -o test -Dtest=CatalogoPostgresTest,PublicacionControllerTest,GlobalExceptionHandlerTest,SeguridadErroresTest,CorsOrigenesTest -Ddante.pg.required=true` | 43 tests, 0 fallas, 0 skipped |
| `mvn -o test -Dtest=MigracionesPostgresTest,CatalogoPostgresTest -Ddante.pg.required=true` | 12 tests, 0 fallas, 0 skipped |
| Suite completa del back (`-Ddante.pg.required=true`) | 184 tests, 0 fallas, 0 skipped |
| Humo sobre copia de `danteautomotores` | humo: 2 ok, 0 fallas, 3 skip; Flyway: baselined v1, applied 2 migrations, now at v3 |
| Humo sobre copia de `dante_uat` | humo: 5 ok, 0 fallas, 0 skip |
| `npm --prefix ../danteautomotores-front run build` | built in 1.69s |
| Criterios de aceptacion por grep (yml, pom, V1/V3, controller, repo, base de test, front, `.gitignore`, `check-attr eol`) | todos PASS |

## Next Phase Readiness

Listo para 02-02 y 02-03 (V3 ya agrego `tipo_carroceria`, `precio_anterior`, `fecha_vendido`, `agencias.zona`; las entidades las mapean 02-03). Para los planes siguientes: cada cambio de entidad necesita su migracion Vn+1 (`validate`); las pruebas contra Postgres exigen `-Ddante.pg.required=true` para no quedar salteadas; el humo local usa `PUERTO_BACK` si otro plan ocupa el 8080.

## Self-Check: PASSED

- Archivos creados verificados en disco (V1, V2, V3, DTO, CatalogoService, PostgresLocalTestBase, CatalogoPostgresTest, MigracionesPostgresTest, con-back-local.sh, catalogo-humo.js, .gitattributes).
- Commits verificados: back `b1df2d8`, `2d15cab`, `2b02ecc`; front `6476435`.
