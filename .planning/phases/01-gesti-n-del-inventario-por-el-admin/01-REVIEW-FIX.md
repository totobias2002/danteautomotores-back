---
phase: 01-gesti-n-del-inventario-por-el-admin
fixed_at: 2026-10-03T03:00:00-03:00
review_path: .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-REVIEW.md
iteration: 2
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 1: Code Review Fix Report

**Fixed at:** 2026-10-03
**Source review:** .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-REVIEW.md
**Iteration:** 2

**Summary:**
- Findings in scope: 2 (WR-12, WR-13; Info excluded by `fix_scope: critical_warning`)
- Fixed: 2
- Skipped: 0

**Verification:** run in the main checkout (`workflow.use_worktrees=false`, no worktree created). Backend: `rm -rf target/classes target/test-classes; mvn -B -o -Djava.version=17 test` gives 172 tests, 0 failures (150 before). Two throwaway probes against the local Postgres container (port 5433) were run and deleted before committing, see WR-13. Back-only change, the front repo was not touched.

## Fixed in iteration 1 (already fixed, not redone)

CR-01, WR-01, WR-02, WR-03, WR-04, WR-05, WR-06, WR-07, WR-08, WR-09, WR-10 and WR-11 were fixed in iteration 1 and confirmed fixed by the re-review (see the git log for the `fix(01)` commits). WR-12 and WR-13 are the residuals the re-review found in CR-01 and WR-01.

## Fixed Issues

### WR-12: `SecretosGuard` is fail-open and does not enforce the 32-character minimum it advertises

**Files modified:** `src/main/java/com/danteautomotores/config/SecretosGuard.java`, `src/main/java/com/danteautomotores/security/JwtService.java`, `src/test/java/com/danteautomotores/config/SecretosGuardTest.java`, `README.md`
**Commit:** afcd4c4
**Status:** fixed: requires human verification (startup-gating logic; the deploy-without-Dockerfile case depends on how the host sets the profile)
**Applied fix:**
- Choice: the lenient mode is now the exception, not `prod` the strict one. The guard only warns when no profile is active (zero-setup local dev: `mvn spring-boot:run` or the IDE, which keeps working) or when every active profile is one of `dev`, `local`, `test`. Any other profile (`production`, `railway`, `staging`, `qa`...) and any mix such as `prod,dev` aborts startup on a missing or placeholder JWT secret or the dev DB password. A pure "default profile is strict" rule was rejected because it would break zero-setup local dev, which the orchestrator required.
- New length check: a JWT secret under 32 bytes (UTF-8, what JJWT HS256 needs) aborts startup in strict mode and warns in lenient mode, instead of failing later with a 500 on the first login.
- `JwtService.key()` now uses `StandardCharsets.UTF_8` explicitly, matching the guard's byte count.
- README: the `SPRING_PROFILES_ACTIVE`, `APP_JWT_SECRET` and `SPRING_DATASOURCE_PASSWORD` rows document the rule and say that a deploy without the Dockerfile (the Dockerfile already sets `prod`) must set `SPRING_PROFILES_ACTIVE=prod`.
- Tests (18, up from 6): short secret, exact 32 bytes, byte vs character length, `production`/`railway`/`staging`/`qa` strict, `prod,dev` strict, no profile and `dev`/`local`/`test` lenient with a warning, short secret lenient with a warning.
- Residual: with no profile set, the guard is lenient by design. A host that builds without the Dockerfile and sets no profile still boots with the placeholder secret (warning only). The README now says to set the profile.

### WR-13: Row lock and DB connection are held across the Cloudinary upload, with no lock timeout

**Files modified:** `src/main/java/com/danteautomotores/service/PublicacionService.java`, `src/main/java/com/danteautomotores/service/CloudinaryService.java`, `src/main/java/com/danteautomotores/repository/PublicacionRepository.java`, `src/main/java/com/danteautomotores/repository/FotoPublicacionRepository.java`, `src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java`, `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java`, `src/test/java/com/danteautomotores/service/CloudinaryServiceTest.java`, `src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java`
**Commit:** 188f144
**Status:** fixed: requires human verification (concurrency and transaction-boundary change; no real two-request test against the full HTTP stack, see below)
**Applied fix:**
- `agregarFoto` is now `@Transactional(propagation = NOT_SUPPORTED)` and works in three stages. (1) No lock: the publication must exist (404), the file is validated, and a `countByPublicacionId` check against the 10-photo cap runs (400, same Spanish message), so a rejected photo never reaches Cloudinary. (2) No transaction: `cloudinaryService.subir`. (3) A short `TransactionTemplate` transaction (15 s timeout) takes the lock, re-checks the cap under the lock, computes `max(orden)+1`, saves.
- If stage 3 fails for any reason (lock not obtained, cap hit by a concurrent upload, DB error, commit error), the uploaded asset is deleted best-effort and the exception is rethrown. This replaces the previous `afterCompletion` synchronization. The 502 contract (`ServicioExternoException`) is unchanged, and a failed upload opens no transaction and deletes nothing.
- Lock timeout: the first attempt, the `jakarta.persistence.lock.timeout` hint, was tested against the local Postgres and does not work. The PostgreSQL dialect ignores it and a second transaction waited the full 30 s until the first released the lock. The final fix is `PublicacionRepository.fijarTimeoutDeLock()` (`select set_config('lock_timeout', '5000', true)`, local to the transaction), called by `buscarEntidadParaEscritura` before `findByIdForUpdate`. This covers `agregarFoto`, `reordenarFotos` and `eliminarFoto`. The probe then failed after 5029 ms with `PessimisticLockingFailureException`.
- `GlobalExceptionHandler` maps `PessimisticLockingFailureException` (and so `CannotAcquireLockException`) to 409 with "Otra operación está modificando este auto. Intentá de nuevo en unos segundos.", without leaking the driver message.
- Cloudinary timeouts without new dependencies: the SDK's per-call options `connect_timeout` and `connection_request_timeout` (10 s) plus `timeout` (read, 60 s for uploads, 15 s for deletes) are passed on upload and destroy. The option names were confirmed from the `cloudinary-http44` 1.39 bytecode (`ApiUtils.setTimeouts`).
- Tests: `PublicacionServiceTest` 36 to 44 (upload happens before `getTransaction` and before the lock, the transaction has a timeout and `NOT_SUPPORTED`, cap re-check under the lock deletes the asset, cleanup on save failure, commit failure and lock failure, no cleanup on success or on upload failure, `fijarTimeoutDeLock` runs before the lock); `CloudinaryServiceTest` +1; `GlobalExceptionHandlerTest` +1 (409).
- Real-database checks (throwaway, deleted before the commit): a two-thread probe for the lock timeout (above), and a `@SpringBootTest` against Postgres with `CloudinaryService` mocked that ran `agregarFoto` then `eliminarFoto` on publication 1 through the real proxy and `TransactionTemplate` (photo count 0, 1, 0).
- Not done: a permanent concurrent test. It needs a live Postgres, which would break the offline `mvn test` run. The cap race itself is covered by the Mockito re-check test, not by two real concurrent HTTP requests.
- Residual (not in scope): `IN-11` (an upload compensated on a commit with unknown outcome) now maps to "delete the asset on any exception from the transaction", same trade-off as before and noted in a code comment. `IN-10` (`eliminar` does not take the lock) is untouched.

---

_Fixed: 2026-10-03_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 2_
