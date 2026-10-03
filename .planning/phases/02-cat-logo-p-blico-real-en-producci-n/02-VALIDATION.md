---
phase: "2"
slug: "cat-logo-p-blico-real-en-producci-n"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-10-03"
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Fuente: `02-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + MockMvc/`spring-security-test` (Boot 3.3.5); tests nuevos contra el Postgres del docker-compose (`localhost:5433`) con `@DataJpaTest` + Flyway; front: `node:test` (built-in) + `npm run build` |
| **Config file** | none — los tests Postgres fijan propiedades con `@DynamicPropertySource` (`PostgresLocalTestBase`, Wave 0) |
| **Quick run command** | `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -o -Djava.version=17 -f C:/Users/toto/Desktop/work/danteautomotores-back/pom.xml test -Dtest=<Clase>` |
| **Full suite command** | `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test` (Postgres del compose arriba) + `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` + `node --test` sobre `src/utils/` del front |
| **Estimated runtime** | ~120 seconds (back) + ~30 seconds (front) |

Nota: si aparece error de `class file version`, correr antes `rm -rf target/classes target/test-classes`. Flyway requiere una resolución online una vez (`mvn -B -Djava.version=17 -DskipTests test-compile` sin `-o`). Ningún comando de verificación apunta a producción.

---

## Sampling Rate

- **After every task commit:** `mvn -B -o -Djava.version=17 test -Dtest=<clase de la tarea>` (+ `npm run build` si toca el front)
- **After every plan wave:** suite completa del back con `-Ddante.pg.required=true` + build del front + `node --test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

(Requirement-level map; the planner binds task IDs in each PLAN.md `<verify>`.)

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| PROD-04 | V1+V2+V3 sobre base vacía; prod simulada + baseline ⇒ solo V2/V3, datos intactos; `validate` pasa | integración PG | `-Ddante.pg.required=true -Dtest=MigracionesPostgresTest` | ❌ W0 | ⬜ pending |
| PROD-04 | yml base sin `ddl-auto: update` ni `show-sql: true` | estático | `! grep -rnE "ddl-auto: update|show-sql: true" src/main/resources` | ✅ | ⬜ pending |
| PROD-02 | Guard falla en `prod` sin Cloudinary/JWT; avisa en dev | unit | `-Dtest=SecretosGuardTest` | ✅ ampliar | ⬜ pending |
| PROD-02 | CORS acepta orígenes con espacios | slice | `-Dtest=CorsOrigenesTest` | ✅ | ⬜ pending |
| PROD-02/04 | `/actuator/health` sin token | slice | `-Dtest=SeguridadErroresTest` | ✅ ampliar | ⬜ pending |
| CAT-02 | Filtros combinados, orden por defecto, paginado de 24 sin solapamiento | integración PG | `-Dtest=CatalogoPostgresTest` | ❌ W0 | ⬜ pending |
| CAT-02 | Clamps de parámetros, enum inválido → 400 uniforme, facetas | slice + unit | `-Dtest=PublicacionControllerCatalogoTest,CatalogoServiceTest` | ❌ W0 | ⬜ pending |
| CAT-03 | `destacados` excluye VENDIDO, límite acotado | unit + PG | `-Dtest=CatalogoServiceTest,CatalogoPostgresTest` | ❌ W0 | ⬜ pending |
| CAT-04 | `fechaVendido` al pasar a VENDIDO; vendido > 30 días fuera del listado pero visible por id; similares | unit + PG | `-Dtest=PublicacionServiceTest,CatalogoPostgresTest` | ✅ ampliar / ❌ W0 | ⬜ pending |
| D-03 | `oferta` = `precioAnterior > precio` (mapper y filtro) | unit + PG | `-Dtest=PublicacionMapperTest,CatalogoPostgresTest` | ❌ W0 | ⬜ pending |
| D-06 | Consulta sobre VENDIDA → 400 | unit | `-Dtest=ConsultaServiceTest` | ❌ W0 | ⬜ pending |
| CAT-01 | Sin mocks ni `USE_MOCK_DATA` en el front; build verde | estático + build | `! grep -rnE "USE_MOCK_DATA|mocks/" .../danteautomotores-front/src` + `npm run build` | ✅ | ⬜ pending |
| CAT-02 (front) | Parseo/serialización de filtros de la URL | unit node:test | `node --test .../danteautomotores-front/src/utils/` | ❌ W0 | ⬜ pending |
| CAT-01..04 | Humo del API local | humo Node | `API=http://localhost:8080/api node scripts/verify/catalogo-humo.js` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Dependencias Flyway (`flyway-core`, `flyway-database-postgresql`) en `pom.xml` + resolución online
- [ ] `src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java` — base descartable en `localhost:5433`, `Assumptions` salvo `-Ddante.pg.required=true`
- [ ] `MigracionesPostgresTest`, `CatalogoPostgresTest`
- [ ] `CatalogoServiceTest`, `PublicacionControllerCatalogoTest`, `PublicacionMapperTest`, `ConsultaServiceTest`
- [ ] Bean `Clock` (`ClockConfig`)
- [ ] `scripts/verify/catalogo-humo.js` (solo back local)
- [ ] Front: `src/utils/catalogoParams.test.js` (`node --test`)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Navegación visual Home/Autos/Agencia/Detalle | CAT-01..04 | No hay runner de navegador | Front de prueba en `http://localhost:5174` con `VITE_API_URL=http://localhost:8080/api` y back local con `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174` |
| Backup + migración de la base de producción | PROD-04 | Requiere acceso al dashboard de Railway | Runbook H1-H9 de `02-RESEARCH.md` |
| Deploy back (Railway) y front (Vercel), carga de demo | PROD-02, PROD-04, D-09 | Requiere credenciales del usuario | Checkpoints humanos del plan de despliegue |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
