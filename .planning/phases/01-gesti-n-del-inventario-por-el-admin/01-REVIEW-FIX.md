---
phase: 01-gesti-n-del-inventario-por-el-admin
fixed_at: 2026-10-03T02:00:00-03:00
review_path: .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-REVIEW.md
iteration: 1
findings_in_scope: 12
fixed: 12
skipped: 0
status: all_fixed
---

# Phase 1: Code Review Fix Report

**Fixed at:** 2026-10-03
**Source review:** .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 12 (1 critical, 11 warning; Info excluded by `fix_scope: critical_warning`)
- Fixed: 12
- Skipped: 0

**Verification:** run in the main checkout (`workflow.use_worktrees=false`, no worktree created). Backend: `mvn -B -o -Djava.version=17 test` gives 150 tests, 0 failures (124 before). Frontend: `npm run build` passes after each front fix. Smoke boot against the Postgres container on port 5433: the app starts without the `prod` profile (warns about the default secrets), lists publicaciones and agencias (lazy mapping inside the new transactions works, `findByIdForUpdate` JPQL validated at startup), and with `SPRING_PROFILES_ACTIVE=prod` it refuses to start (`SecretosGuard` IllegalStateException).

## Fixed Issues

### CR-01: Default JWT secret is committed and nothing blocks it from reaching production

**Files modified:** `src/main/java/com/danteautomotores/config/SecretosGuard.java` (new), `src/test/java/com/danteautomotores/config/SecretosGuardTest.java` (new), `README.md`, `Dockerfile`
**Commit:** 7265a9d
**Applied fix:** New `SecretosGuard` bean (same pattern as `DataSeeder`): under the `prod` profile it throws `IllegalStateException` when `app.jwt.secret` is blank or equals the public placeholder, or when `spring.datasource.password` equals `dante_dev_password`. Without `prod` it only logs a warning, so local dev still starts with no setup. The secret values are never logged. `application.yml` defaults are unchanged (the app must start in dev). README documents `APP_JWT_SECRET` as mandatory in production. The Dockerfile now sets `ENV SPRING_PROFILES_ACTIVE=prod` so the guard is on by default in the deployed image (can still be overridden at runtime). No real secret was committed.

### WR-01: `agregarFoto` can exceed the 10-photo cap and duplicate `orden` under concurrent uploads

**Files modified:** `src/main/java/com/danteautomotores/repository/PublicacionRepository.java`, `src/main/java/com/danteautomotores/service/PublicacionService.java`, `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java`
**Commit:** 16733c0
**Status:** fixed: requires human verification (concurrency behavior is only covered by mock tests, not by a real concurrent run)
**Applied fix:** Added `findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`) and used it in `agregarFoto`, `reordenarFotos` and `eliminarFoto`. In `eliminarFoto` the publication is now locked first, so a nonexistent publication id yields 404 before the photo lookup (previously a foreign photo gave 400 even for a nonexistent publication id).

### WR-02: Orphan Cloudinary asset when the DB write or commit fails after a successful upload

**Files modified:** `src/main/java/com/danteautomotores/service/PublicacionService.java`, `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java`
**Commit:** 3034fb0
**Applied fix:** After `cloudinaryService.subir`, a `TransactionSynchronization.afterCompletion` deletes the uploaded `public_id` if the status is not `STATUS_COMMITTED`. Tests simulate rollback and commit.

### WR-03: `PublicacionRequest` has almost no server-side validation, so bad data returns a misleading 409

**Files modified:** `src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java`, `src/main/java/com/danteautomotores/dto/publicacion/AnioDeModelo.java` (new), `src/test/java/com/danteautomotores/dto/PublicacionRequestValidationTest.java` (new)
**Commit:** b1fc94d
**Applied fix:** `anio` uses a new `@AnioDeModelo` constraint (1900 to current year + 1, computed at validation time so field key stays `anio`); `kilometraje` `@PositiveOrZero`; `moneda` `@Pattern(ARS|USD)`; `marca`/`modelo`/`color` `@Size(max=255)`; `precio` `@Digits(10,2)`; `descripcion` `@Size(min=10, max=5000)` (null still allowed, matching the front's 10-character minimum). All messages are in rioplatense Spanish, including the pre-existing `@NotNull`/`@NotBlank`. The `{"error","campos"}` format is unchanged.

### WR-04: `IllegalArgumentException` handler echoes arbitrary internal messages to the client

**Files modified:** `src/main/java/com/danteautomotores/exception/ReglaDeNegocioException.java` (new), `src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java`, `AgenciaService`, `AuthService`, `FavoritoService`, `ImagenValidator`, `PublicacionService`, and the matching tests
**Commit:** baf89cf
**Status:** fixed: requires human verification (error-handling semantics changed)
**Applied fix:** Deliberate business-rule violations now throw `ReglaDeNegocioException`, mapped to 400 with its message. The `IllegalArgumentException` handler is removed, so a stray one falls through to `handleUnexpected` (logged, generic 500). Added a test for that. Existing 400 responses keep the same messages.

### WR-05: CORS origins are not trimmed

**Files modified:** `src/main/java/com/danteautomotores/config/SecurityConfig.java`, `src/test/java/com/danteautomotores/security/CorsOrigenesTest.java` (new)
**Commit:** 9397ee5
**Applied fix:** Origins are split, trimmed and empty entries dropped. Preflight tests cover spaces around commas, an empty entry, and a rejected unlisted origin.

### WR-06: Editing a car with a decimal price corrupts the displayed price

**Files modified:** `danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx`
**Commit:** 8a7f3a8 (front repo)
**Applied fix:** `publicacionAForm` loads the price as `String(Math.round(Number(p.precio)))`, since the price field only handles whole numbers. Note: a stored price with cents is rounded to a whole number on the next save.

### WR-07: Editing a car silently overwrites unset transmission, fuel and condition with defaults

**Files modified:** `danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx`
**Commit:** a81e6d9 (front repo)
**Applied fix:** A null `transmision`/`combustible`/`condicion` loads as `''`; the three selects gained a "Sin especificar" option; the payload sends `null` for `''`. New-car defaults (Manual/Nafta/Bueno) are unchanged.

### WR-08: Dashboard search input and status select have no accessible name

**Files modified:** `danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx`
**Commit:** 6d899f3 (front repo)
**Applied fix:** `aria-label="Buscar por marca o modelo"` on the search input and `aria-label={`Estado de ${marca} ${modelo}`}` on each row's status select.

### WR-09: `AuthProvider` crashes the whole app on corrupt `localStorage`

**Files modified:** `danteautomotores-front/src/context/AuthContext.jsx`
**Commit:** 2476894 (front repo)
**Applied fix:** The `useState` initializer wraps `JSON.parse` in try/catch; on failure it removes `usuario` and `token` and returns null.

### WR-10: `mensajeDeError` discards the field messages and shows raw property names

**Files modified:** `danteautomotores-front/src/utils/errores.js`
**Commit:** 15074f2 (front repo)
**Applied fix:** Validation errors now show the backend's Spanish field messages (deduplicated, joined with ". "), falling back to `data.error` when there are none.

### WR-11: `PublicacionService` mixes transactional and non-transactional methods and relies on Open-Session-In-View

**Files modified:** `PublicacionService`, `FavoritoService`, `ConsultaService`, `src/main/resources/application.yml`, `src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java` (new)
**Commit:** af844ea
**Status:** fixed: requires human verification (runtime behavior with `open-in-view: false`)
**Applied fix:** Class-level `@Transactional` on `PublicacionService`, with `readOnly = true` on `buscar` and `obtenerPorId`. `spring.jpa.open-in-view: false` set. Beyond the cited file, `FavoritoService` and `ConsultaService` got the same treatment, because their mappers read lazy associations and would break once open-in-view is off. A structural test guards both the setting and the annotations. A smoke boot against Postgres confirmed the public listing still maps lazy data. Write paths (create/update with a real DB) were not exercised end to end.

---

_Fixed: 2026-10-03_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
