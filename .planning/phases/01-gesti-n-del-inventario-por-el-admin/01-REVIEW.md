---
phase: 01-gesti-n-del-inventario-por-el-admin
reviewed: 2026-10-03T00:00:00Z
depth: standard
files_reviewed: 33
files_reviewed_list:
  - Dockerfile
  - README.md
  - src/main/resources/application.yml
  - src/main/java/com/danteautomotores/config/SecretosGuard.java
  - src/main/java/com/danteautomotores/config/SecurityConfig.java
  - src/main/java/com/danteautomotores/dto/publicacion/AnioDeModelo.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java
  - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java
  - src/main/java/com/danteautomotores/exception/ReglaDeNegocioException.java
  - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
  - src/main/java/com/danteautomotores/service/AgenciaService.java
  - src/main/java/com/danteautomotores/service/AuthService.java
  - src/main/java/com/danteautomotores/service/ConsultaService.java
  - src/main/java/com/danteautomotores/service/FavoritoService.java
  - src/main/java/com/danteautomotores/service/ImagenValidator.java
  - src/main/java/com/danteautomotores/service/PublicacionService.java
  - src/main/java/com/danteautomotores/service/CloudinaryService.java
  - src/main/java/com/danteautomotores/security/JwtService.java
  - src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java
  - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
  - src/main/java/com/danteautomotores/mapper/AgenciaMapper.java
  - src/main/java/com/danteautomotores/mapper/FavoritoMapper.java
  - src/main/java/com/danteautomotores/mapper/ConsultaMapper.java
  - src/main/java/com/danteautomotores/entity/Publicacion.java
  - src/test/java/com/danteautomotores/config/SecretosGuardTest.java
  - src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java
  - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
  - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
  - ../danteautomotores-front/src/context/AuthContext.jsx
  - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
  - ../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx
  - ../danteautomotores-front/src/utils/errores.js
  - src/test/java/com/danteautomotores/ (remaining fix-touched tests skimmed: PublicacionRequestValidationTest, CorsOrigenesTest, AgenciaServiceTest, AuthServiceTest, ImagenValidatorTest, AgenciaControllerTest)
findings:
  critical: 0
  warning: 2
  info: 12
  total: 14
status: issues_found
---

# Phase 1: Code Review Report (re-review after fixes)

**Reviewed:** 2026-10-03
**Depth:** standard
**Files Reviewed:** 33 (back and front repos)
**Status:** issues_found

## Summary

I checked each prior finding (CR-01, WR-01 to WR-11) against the current source, not against the fix report. All twelve are fixed. Two have a residual that is tracked as a new finding (CR-01 gives WR-12; WR-01 gives WR-13 and IN-10). No blocker remains. The fixes introduced no functional regression in the areas the orchestrator listed.

Verification results:

| ID | Status | Evidence |
|----|--------|----------|
| CR-01 | Fixed for the Docker path. The guard is fail-open outside the exact profile `prod` (see WR-12). | `SecretosGuard.java:33-47` throws under `prod` for a blank or placeholder JWT secret and for the dev DB password. `Dockerfile:15` sets `ENV SPRING_PROFILES_ACTIVE=prod`. |
| WR-01 | Fixed for the three photo operations. Held-lock side effects are WR-13. `eliminar()` is still unlocked (IN-10). | `PublicacionRepository.java:19-21` has `@Lock(PESSIMISTIC_WRITE)` with `select p from Publicacion p`. It is used in `agregarFoto`, `reordenarFotos` and `eliminarFoto` through `buscarEntidadParaEscritura`. The lock is taken before the lazy `fotos` collection is read, so the cap and `max(orden)+1` see current state. The query has no join, so Postgres does not reject `FOR UPDATE` on a nullable side of an outer join. |
| WR-02 | Fixed. One edge case is IN-11. | `PublicacionService.java:190-192, 295-307`. The synchronization is registered right after `subir`, and nothing between the upload and the registration can throw. A `subir` failure registers nothing, which is correct. Validation and the 10-photo cap run before the upload. |
| WR-03 | Fixed. | `PublicacionRequest.java` and `AnioDeModelo.java`. Limits match the columns (`varchar(255)`, `numeric(12,2)`). Optional fields stay nullable. The year ceiling is computed at validation time. The front end already enforces at least 10 characters and ARS/USD, so no legitimate payload from the UI is rejected. |
| WR-04 | Fixed and complete. | Every `throw new IllegalArgumentException` in `src/main` became `ReglaDeNegocioException` (grep: AgenciaService, AuthService, FavoritoService, ImagenValidator, PublicacionService). The only remaining `IllegalArgumentException` reference is the catch in `JwtAuthenticationFilter:58`, which is correct. Spring-originated argument errors are still mapped to 400 by `ResponseEntityExceptionHandler`: type mismatch, unreadable body and missing part. Other `IllegalArgumentException`s now reach `handleUnexpected`, which logs them and returns a generic 500. |
| WR-05 | Fixed. | `SecurityConfig.java:74-77` trims and drops empty entries. |
| WR-06 | Fixed in the way the review suggested. A silent-rounding side effect is IN-09. | `AdminPublicacionFormPage.jsx:86`. |
| WR-07 | Fixed. | `AdminPublicacionFormPage.jsx:91-94, 158-160, 427-481`. A null value loads as `''` and the payload sends `null`. The new-car defaults are unchanged. |
| WR-08 | Fixed. | `AdminDashboardPage.jsx:392, 440`. |
| WR-09 | Fixed. | `AuthContext.jsx:9-18`. |
| WR-10 | Fixed. | `errores.js:11-18`. |
| WR-11 | Fixed. | Class-level `@Transactional` on `PublicacionService`, `FavoritoService` and `ConsultaService`. `buscar`, `obtenerPorId`, `listarParaAdmin` and `obtenerImpactoEliminacion` are `readOnly`. `open-in-view: false` is set. |

Open-in-view regression check. Every mapper that reads a lazy association runs inside a transaction:
- `PublicacionMapper` reads `agencia` and `fotos`.
- `FavoritoMapper` reads `publicacion`, then `PublicacionMapper`.
- `ConsultaMapper` reads `publicacion.getId()`, which is safe on a proxy.
- `AgenciaMapper` touches only scalar columns, so `AgenciaService` correctly needs no transaction.
- No controller, `DataSeeder`, `CustomUserDetailsService` or the JWT filter touches lazy state. A grep of `controller/` found no repository or entity access.

Cloudinary cleanup on rollback and commit paths is correct. `eliminarImagenesDespuesDelCommit` runs after commit and `eliminarImagenSiNoHayCommit` runs after a non-commit. Both only register when synchronization is active, and `CloudinaryService.eliminar` never throws.

## Warnings

### WR-12: `SecretosGuard` is fail-open and does not enforce the 32-character minimum it advertises (residual of CR-01)

**File:** `src/main/java/com/danteautomotores/config/SecretosGuard.java:33-47` (and `Dockerfile:15`, `security/JwtService.java:19-21`)
**Issue:**
1. The guard aborts only when the active profile is exactly `prod`. Any other value for `SPRING_PROFILES_ACTIVE` disables it silently: `production`, `railway`, `staging`, or an override that drops `prod`. The same applies to any deployment that does not use the Dockerfile, such as Railway's native Spring Boot build mentioned in the project docs. The only signal is one `log.warn`. In that case the app boots with the public placeholder secret, and anyone who knows the admin email can forge an ADMIN token, which is exactly the original CR-01 scenario. The original review offered the inverted logic ("only a `dev` profile is lenient") as an alternative. The fix chose opt-in strictness, so the safe default depends on deploy configuration.
2. The guard rejects a blank secret or the exact placeholder. A short secret supplied through the environment (for example `APP_JWT_SECRET=abc`) passes. The README and the message both say "at least 32 characters", but nothing enforces it. `Keys.hmacShaKeyFor` then throws `WeakKeyException` on the first `generateToken`, so every login and registration returns a generic 500 instead of failing at startup.

**Fix:** Make the lenient mode opt-in instead of the strict mode.
```java
boolean dev = environment.acceptsProfiles(Profiles.of("dev", "test", "default"));
// ...
if (!dev) throw new IllegalStateException(mensaje);
```
Alternatively, keep `prod` but also fail when no profile at all is active outside tests. Add a length check in the same method:
```java
if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
    fallarOAvisar("APP_JWT_SECRET tiene menos de 32 bytes");
}
```
Add test cases for a short secret and for a profile other than `prod`. Also use the same charset in `JwtService.key()` (`secret.getBytes()` uses the platform default).

### WR-13: Row lock and DB connection are held across the Cloudinary upload, with no lock timeout

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:180-211` (with `repository/PublicacionRepository.java:19-21` and `service/CloudinaryService.java:27-39`)
**Issue:** The WR-01 fix makes `agregarFoto` take `PESSIMISTIC_WRITE` on the publication row and then call `cloudinaryService.subir` (a network upload of up to 10 MB) while still inside the transaction. Three consequences:
- The lock and a Hikari connection are held for the whole upload.
- No lock timeout is configured (`jakarta.persistence.lock.timeout` is unset), and `CloudinaryConfig` sets no connect or read timeout on the SDK. If Cloudinary stalls, every other photo operation on that car (reorder, delete, a second upload) waits on the row lock indefinitely. Each waiter holds a pool connection and a Tomcat thread, so a few stalled uploads can exhaust the pool for the whole API, including public reads.
- The admin UI uploads files one after another, so normal use serializes, but the stall scenario is a server-side exposure.

The concurrency behavior (the cap and duplicate `orden`) is still only covered by Mockito tests, as the fix report notes. No real concurrent run was done.

**Fix:** Bound the wait and the upload.
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
@Query("select p from Publicacion p where p.id = :id")
Optional<Publicacion> findByIdForUpdate(@Param("id") Long id);
```
Map `PessimisticLockingFailureException` and `CannotAcquireLockException` to a 409 or 503 with a Spanish message in `GlobalExceptionHandler`. Set explicit Cloudinary timeouts (`"timeout"` or `"connection_timeout"` in `ObjectUtils.asMap`). Optionally, upload before opening the transaction, then take the lock only to check the cap, compute `orden` and insert, keeping the compensation on failure. Add one real concurrent test (`@DataJpaTest` with two threads) for the cap.

## Info

### IN-01: Login shows "Email o contraseña incorrectos" for every failure

**File:** `../danteautomotores-front/src/pages/LoginPage.jsx:31-33`
**Issue:** The bare `catch` shows a wrong-credentials message for a network failure, a 500 or a 502 too. Someone who types the right password during a backend outage is told it's wrong. This is more visible now that a short JWT secret in a misconfigured prod gives a 500 on every login (WR-12).
**Fix:** Show that message only when `err.response?.status === 401`. Otherwise use `mensajeDeError(err, ...)`.

### IN-02: `ConfirmDialog` can lose sync with its parent and lacks an accessible name

**File:** `../danteautomotores-front/src/components/ConfirmDialog.jsx:14-35`
**Issue:** Chrome allows a second Esc press to close a modal dialog even when `cancel` was prevented. While `cargando` is true, `handleCancel` does nothing, so the native dialog can close while the parent still has `abierto = true`. After that the user sees no dialog and the delete is still in progress. There is no `onClose` handler to resync the parent. The `<dialog>` also has no `aria-labelledby` pointing at the `<h2>`.
**Fix:** Add `onClose={() => abierto && !cargando && onCancelar()}` and `aria-labelledby` with an `id` on the heading.

### IN-03: `JwtAuthenticationFilter` swallows configuration errors silently

**File:** `src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java:58-60`
**Issue:** `WeakKeyException` (a secret under 32 bytes) is a `JwtException`, and `IllegalArgumentException` can also come from `loadUserByUsername`. Both are swallowed with no log line. A server misconfiguration then looks like "every token is invalid", with 401s for every user and no trace in the logs.
**Fix:** Add `log.debug("JWT rechazado: {}", e.getMessage())` in the catch. Use `log.error` for `io.jsonwebtoken.security.SecurityException`, which is the parent of `WeakKeyException`.

### IN-04: Cloudinary deletes run synchronously after commit with no retry or reconciliation

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:274-291`
**Issue:** `afterCommit` loops over up to 10 sequential Cloudinary calls on the request thread, so the 204 waits for all of them. A Cloudinary failure is only logged, so the asset stays orphaned and nothing records it. The design trades a possible orphan for safety, which is reasonable, but nothing lets the admin find or fix orphans later. The new rollback compensation (`eliminarImagenSiNoHayCommit`) has the same property.
**Fix:** Send the deletes through an `@Async` executor, or write the `public_id` to a `cloudinary_pendientes` table and clean it up with a scheduled job. At minimum log at `error` with the `public_id` so orphans can be recovered from the log.

### IN-05: Agency slug generation yields empty or dash-terminated slugs

**File:** `src/main/java/com/danteautomotores/service/AgenciaService.java:91-98`
**Issue:** `trim()` runs before the punctuation is removed. `"Dante !"` becomes `"dante "` and then `"dante-"`. A name made only of symbols or emoji gives `""`, and `GET /api/agencias/{slug}` can't reach that agency. Names with non-Latin characters also lose all their letters.
**Fix:** Strip the punctuation first, then `trim()`, then collapse whitespace and dashes. Use a fallback like `"agencia"` when the result is empty.

### IN-06: Photo ordering is not deterministic for equal or null `orden`

**File:** `src/main/java/com/danteautomotores/mapper/PublicacionMapper.java:34-35` (and `entity/Publicacion.java:82-84`)
**Issue:** The mapper sorts only by `orden`, and `fotos` has no `@OrderBy`. `resecuenciarFotos` breaks ties by id, but the mapper does not. For legacy photos with all-null `orden`, the cover shown before a delete can differ from the one the resequence picks. The comment in the service claims the two use the same criterion, which isn't true.
**Fix:** Add `.thenComparing(FotoPublicacion::getId, Comparator.nullsLast(Comparator.naturalOrder()))` to the mapper, and put the comparator in one shared place.

### IN-07: Documentation drift in the README

**File:** `README.md:20, 44, 53, 58`
**Issue:** The README says Postgres runs on port 5432, but `docker-compose.yml` maps `5433:5432` and the default JDBC URL uses 5433. It says "Maven + JDK 25", but the Dockerfile uses JDK 21. The test instructions point at `jdk-17` with `-Djava.version=17`. The project's baseline is Java 21. The fix commit edited the prod-variables table but did not touch these lines.
**Fix:** Update those lines so they match the compose file, the Dockerfile and `pom.xml`.

### IN-08: Smaller issues in seeding, build config and tests

**File:** `src/main/java/com/danteautomotores/config/DataSeeder.java:70-93`; `src/main/resources/application.yml:19-24`; `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java`; `../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx:121-134`
**Issue:**
- `DataSeeder` doesn't trim or lowercase `ADMIN_EMAIL`. A trailing space from a pasted env var creates an admin who can't log in. `contains("@")` is a very weak email check.
- `show-sql: true` and `ddl-auto: update` apply in every profile, including production, so the new `prod`-by-default Docker image still dumps SQL to the logs and changes the schema automatically with no migrations.
- The service tests are all Mockito-only. Nothing exercises a real transaction, so the `afterCommit` ordering, orphan removal, `orden` persistence through dirty checking, the cascade delete order and the new pessimistic lock are untested end to end. The new rollback tests simulate synchronization callbacks by hand.
- If loading a publication fails in edit mode, the form stays in edit mode with `FORM_INICIAL` defaults (Manual, Nafta, Bueno, empty text). A PUT from that blank form would overwrite the car. WR-07 fixed the null-on-load case but not this one.

**Fix:** Normalize the seeder email with `trim().toLowerCase()`. Move `show-sql` and `ddl-auto` to a dev profile. Add one `@DataJpaTest` or `@SpringBootTest` with Testcontainers or H2 for delete, reorder and the lock. Disable the edit form when the load fails.

### IN-09: Editing a car now silently rounds a stored price with cents, while the server still accepts cents

**File:** `../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx:84-86` (and `dto/publicacion/PublicacionRequest.java:39`)
**Issue:** The WR-06 fix rounds the price on load. A car stored at 12999.99 (USD, for example) shows as 13000, and the next save of any unrelated field writes 13000 without telling the admin. The server contract still allows two decimals (`@Digits(integer=10, fraction=2)`), so the two sides disagree about what a price is. The review's suggested fix said to decide that prices are whole numbers and validate that on the server too. This was not done.
**Fix:** Decide on whole-number prices, then use `@Digits(integer = 10, fraction = 0)` and round or migrate existing rows. Or support decimals in the input (keep the fraction, format with a decimal separator). At minimum, skip sending `precio` when it was not edited.

### IN-10: `eliminar` does not take the publication lock that the photo operations now use

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:154-167`
**Issue:** `eliminar` loads the car with plain `findById` and collects `public_id`s from the photos visible at that moment. A concurrent `agregarFoto` that commits in between adds a photo the delete never saw. The `DELETE` on `publicaciones` then fails on the foreign key, and `handleDataIntegrity` returns a misleading 409 "hay datos relacionados". No data is lost, because the transaction rolls back. The Cloudinary cleanup for that photo is not at risk either, but the admin gets a confusing error.
**Fix:** Use `buscarEntidadParaEscritura(id)` in `eliminar` so the delete and the photo mutations serialize.

### IN-11: The upload compensation also deletes the image when the commit outcome is `STATUS_UNKNOWN`

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:299-306`
**Issue:** `afterCompletion` deletes the image for every status other than `STATUS_COMMITTED`, which includes `STATUS_UNKNOWN` (a heuristic outcome or a connection lost during commit). If the database actually committed the row, the image is destroyed and the published car points to a dead URL. This is rare, and the alternative is an orphan, but a dead cover photo is the worse failure.
**Fix:** Delete only on `STATUS_ROLLED_BACK`. For `STATUS_UNKNOWN`, log at `error` with the `public_id` and leave the asset alone.

### IN-12: `AuthService.login` throws a 400 with the same text that the handler returns as a 401

**File:** `src/main/java/com/danteautomotores/service/AuthService.java:49-50`
**Issue:** After `authenticationManager.authenticate` succeeds, the `orElseThrow` raises `ReglaDeNegocioException("Credenciales inválidas")`, which maps to 400. The branch is effectively unreachable, but if a user is deleted between the two calls, the client sees 400 where every other credential failure is 401. The WR-04 conversion made this inconsistency explicit.
**Fix:** Throw `BadCredentialsException("Credenciales inválidas")` there.

---

_Reviewed: 2026-10-03_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
