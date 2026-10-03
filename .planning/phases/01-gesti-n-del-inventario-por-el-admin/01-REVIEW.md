---
phase: 01-gesti-n-del-inventario-por-el-admin
reviewed: 2026-10-02T00:00:00Z
depth: standard
files_reviewed: 38
files_reviewed_list:
  - README.md
  - src/main/java/com/danteautomotores/config/DataSeeder.java
  - src/main/java/com/danteautomotores/config/SecurityConfig.java
  - src/main/java/com/danteautomotores/controller/AdminPublicacionController.java
  - src/main/java/com/danteautomotores/controller/PublicacionController.java
  - src/main/java/com/danteautomotores/dto/publicacion/CambiarDestacadoRequest.java
  - src/main/java/com/danteautomotores/dto/publicacion/ImpactoEliminacionResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/ReordenarFotosRequest.java
  - src/main/java/com/danteautomotores/entity/FotoPublicacion.java
  - src/main/java/com/danteautomotores/entity/Publicacion.java
  - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java
  - src/main/java/com/danteautomotores/exception/ServicioExternoException.java
  - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
  - src/main/java/com/danteautomotores/repository/ConsultaRepository.java
  - src/main/java/com/danteautomotores/repository/FavoritoRepository.java
  - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
  - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
  - src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java
  - src/main/java/com/danteautomotores/security/RestAccessDeniedHandler.java
  - src/main/java/com/danteautomotores/security/RestAuthenticationEntryPoint.java
  - src/main/java/com/danteautomotores/service/AgenciaService.java
  - src/main/java/com/danteautomotores/service/CloudinaryService.java
  - src/main/java/com/danteautomotores/service/ImagenValidator.java
  - src/main/java/com/danteautomotores/service/PublicacionService.java
  - src/main/resources/application.yml
  - src/test/java/com/danteautomotores/ (12 test files, skimmed: PublicacionServiceTest, SeguridadWebMvcTestBase, DataSeederTest in detail)
  - ../danteautomotores-front/src/components/ConfirmDialog.jsx
  - ../danteautomotores-front/src/components/ProtectedRoute.jsx
  - ../danteautomotores-front/src/context/AuthContext.jsx
  - ../danteautomotores-front/src/pages/LoginPage.jsx
  - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
  - ../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx
  - ../danteautomotores-front/src/services/api.js
  - ../danteautomotores-front/src/utils/errores.js
findings:
  critical: 1
  warning: 11
  info: 8
  total: 20
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-10-02
**Depth:** standard
**Files Reviewed:** 38 (back and front repos)
**Status:** issues_found

## Summary

The phase is generally well built. Errors share one format, the photo validator checks magic bytes, the cascade delete runs in one transaction, and the Cloudinary cleanup runs after commit. The orchestrator's specific concerns came out as follows.

- **Cloudinary cleanup in `eliminar` and `eliminarFoto`:** correct. The `afterCommit` registration happens inside an active transaction. A rollback skips the Cloudinary delete. `CloudinaryService.eliminar` swallows its own failures, so a failure there cannot undo a committed delete. The weak spots are listed below (WR-02, IN-04).
- **Open redirect via `from`:** not exploitable. `from` is built by the app from `window.location` or the router location and travels in history `state`, not in the URL. `LoginPage` also rejects values that don't start with `/` and values that start with `//`. No finding.
- **`JwtAuthenticationFilter` exception handling:** correct for the cases it targets. An invalid token is cleared and the request continues, so protected routes return 401 and public routes still work. It swallows some failures silently (IN-03).
- **`DataSeeder` secret handling:** the password is never logged, and it is hashed with BCrypt. The seeder's `prod` guard does not cover the more dangerous default JWT secret (CR-01).
- **Search input accessible name:** confirmed (WR-08).

## Critical Issues

### CR-01: Default JWT secret is committed and nothing blocks it from reaching production

**File:** `src/main/resources/application.yml:30` (also `config/DataSeeder.java:98-103`)
**Issue:** `app.jwt.secret` falls back to `CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES`. That string is 62 characters long and public in the repo, so it passes the key-length check. If `APP_JWT_SECRET` is missing on Railway or Render, the app boots normally and signs tokens with a known key. Anyone who knows the admin's email can then forge `{"sub":"<admin email>"}` and get ADMIN access.

This phase added a fail-fast guard for the missing admin variables, but only under the `prod` profile. The Dockerfile does not set that profile. The datasource password also falls back to `dante_dev_password` (`application.yml:14`).

**Fix:** Remove the default so the app fails to start when the secret is unset, or reject the placeholder explicitly.
```yaml
app:
  jwt:
    secret: ${APP_JWT_SECRET}   # no default
```
Or add a `@PostConstruct` check in `JwtService` that throws when the secret starts with `CAMBIAR_`. Put the dev default in `application-dev.yml` instead. Also set `SPRING_PROFILES_ACTIVE=prod` in the Dockerfile `ENV`, or invert the logic so that only a `dev` profile is lenient.

## Warnings

### WR-01: `agregarFoto` can exceed the 10-photo cap and duplicate `orden` under concurrent uploads

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:176-205`
**Issue:** The cap check (`getFotos().size() >= MAX_FOTOS`) and the `max(orden)+1` calculation read a snapshot, with no lock and no unique constraint on `(publicacion_id, orden)`. Two simultaneous uploads for the same car both pass the check and both get the same `orden`. The front end uploads sequentially, but nothing on the server enforces that. Duplicate orders make the cover photo ambiguous. The mapper's tie-break is unstable (see IN-06).

**Fix:** Take a pessimistic lock when loading the publication for photo mutations, for example `@Lock(PESSIMISTIC_WRITE)` on a `findByIdForUpdate` repository method used by `agregarFoto`, `reordenarFotos` and `eliminarFoto`. Or add `@Version` to `Publicacion`.

### WR-02: Orphan Cloudinary asset when the DB write or commit fails after a successful upload

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:176-205`
**Issue:** `agregarFoto` is `@Transactional` and calls `cloudinaryService.subir` inside the transaction. If `fotoPublicacionRepository.save` or the commit then fails (connection loss, constraint violation, a concurrent delete of the car), the transaction rolls back but the image stays in Cloudinary. No row points to it, so it is never cleaned up. The delete path is carefully "after commit", but the upload path has no matching compensation.

**Fix:** Register a compensating action for rollback.
```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) cloudinaryService.eliminar(subida.publicId());
    }
});
```
Alternatively, upload first outside the transaction and persist in a short transaction with `try/catch` cleanup.

### WR-03: `PublicacionRequest` has almost no server-side validation, so bad data returns a misleading 409

**File:** `src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java:12-37` (with `GlobalExceptionHandler.java:58-62`)
**Issue:** The validation gaps are:
- `anio` has only `@NotNull`, so 0, -5 or 99999 pass.
- `kilometraje` accepts negative values.
- `moneda` is any free string, although the UI only offers ARS and USD.
- `marca`, `modelo`, `color` and `descripcion` have no `@Size`.
- `precio` has no `@Digits`, although the column is `numeric(12,2)`.

A name over 255 characters or a price over 10 digits fails at the database. `handleDataIntegrity` then returns 409 "No se pudo completar la operación porque hay datos relacionados", which is wrong and unhelpful for an input problem. The front end enforces "description of at least 10 characters" (`AdminPublicacionFormPage.jsx:142`), but the server does not, so the two disagree.

**Fix:** Add `@Min(1900) @Max(<currentYear+1>)` on `anio`, `@PositiveOrZero` on `kilometraje`, `@Pattern(regexp="ARS|USD")` on `moneda`, `@Size(max=255)` on the string fields, and `@Digits(integer=10, fraction=2)` on `precio`. Decide whether `descripcion` has a minimum and apply it on both sides.

### WR-04: `IllegalArgumentException` handler echoes arbitrary internal messages to the client

**File:** `src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java:41-44`
**Issue:** Any `IllegalArgumentException` becomes a 400 with `ex.getMessage()`. Business rules throw it on purpose, but so do Spring, Hibernate, Spring Data and JDK internals (for example, an invalid entity state or an argument check). Those messages can leak class and property names, and they report a server bug as a client error. The same handler also hides real 500s from monitoring, because it logs nothing.

**Fix:** Throw a dedicated `ReglaDeNegocioException` for deliberate rule violations and map only that to 400. Let `IllegalArgumentException` fall through to `handleUnexpected`, which logs it and returns a generic 500. If the broad handler stays, add at least `log.debug`.

### WR-05: CORS origins are not trimmed

**File:** `src/main/java/com/danteautomotores/config/SecurityConfig.java:72`
**Issue:** `allowedOrigins.split(",")` keeps the whitespace. The README documents "orígenes separados por coma". A natural value like `https://a.vercel.app, https://b.com` gives the second origin a leading space, and the browser then blocks every request from it.

**Fix:**
```java
configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
        .map(String::trim).filter(s -> !s.isEmpty()).toList());
```

### WR-06: Editing a car with a decimal price corrupts the displayed price, and the next keystroke corrupts the saved value

**File:** `../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx:58-62, 84, 488-489`
**Issue:** The column is `numeric(12,2)` and the backend accepts decimals. `publicacionAForm` puts `String(p.precio)` into the form, for example `"12500.5"`. `formatearPrecio` strips every non-digit, so the input displays `125.005`, which is ten times the real price. Any edit to the field runs `e.target.value.replace(/\D/g,'')`, and the stored value becomes `125005`. A save without touching the field sends the correct raw value, which hides the bug until someone edits the price.

**Fix:** Store the price as an integer, or parse it properly.
```js
precio: p.precio != null ? String(Math.round(Number(p.precio))) : '',
```
Better, decide that prices are whole numbers. In that case validate with `@Digits(fraction=0)` on the server and round at load time.

### WR-07: Editing a car silently overwrites unset transmission, fuel and condition with defaults

**File:** `../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx:87-90, 148-154`
**Issue:** `publicacionAForm` maps a null `transmision`, `combustible` or `condicion` to `'MANUAL'`, `'NAFTA'` or `'BUENO'`. These fields are nullable in the entity, and legacy rows can have nulls. The edit form has no "unspecified" option, and the PUT always sends the whole form. Saving any edit therefore writes invented values to the database. The admin would see a plausible value and not notice the data was invented.

**Fix:** Add an empty "Sin especificar" option and keep null as `''`. Send `null` for `''` in the payload. If the fields are meant to be mandatory, mark them `@NotNull` on the server and show them as required.

### WR-08: Dashboard search input and status select have no accessible name

**File:** `../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx:388-397, 438-448`
**Issue:** The search `<label>` wraps only an icon (`Search`, no text) and the `<input>`. The only name left is the placeholder, which is a weak fallback in the accessible-name computation and disappears once the user types. Several assistive technologies don't announce it reliably. The aria-label was removed to satisfy a grep count, which is the wrong reason to drop it. The per-row status `<select>` has no label at all, so a screen reader reads "combobox, Disponible" with no context.

**Fix:**
```jsx
<input type="text" aria-label="Buscar por marca o modelo" ... />
<select aria-label={`Estado de ${p.marca} ${p.modelo}`} ... />
```

### WR-09: `AuthProvider` crashes the whole app on corrupt `localStorage`

**File:** `../danteautomotores-front/src/context/AuthContext.jsx:8-11`
**Issue:** `JSON.parse(guardado)` in the `useState` initializer has no `try/catch`. If `usuario` holds invalid JSON (an extension, a manual edit, a half-written value), the provider throws on mount. The whole tree unmounts to a blank page, and there is no error boundary, so the user can't recover without clearing storage by hand.

**Fix:**
```js
const [usuario, setUsuario] = useState(() => {
  try { return JSON.parse(localStorage.getItem('usuario') ?? 'null') }
  catch { localStorage.removeItem('usuario'); localStorage.removeItem('token'); return null }
})
```

### WR-10: `mensajeDeError` discards the field messages and shows raw property names

**File:** `../danteautomotores-front/src/utils/errores.js:8-11`
**Issue:** For validation errors it returns `Datos inválidos (agenciaId, precio)`. It joins only the keys of `campos` and drops the Spanish messages the backend sends. The user sees Java property names (`telefonoContacto`, `agenciaId`) and no reason.

**Fix:** Return `Object.values(campos).join('. ')`, or `Object.entries(campos).map(([k,v]) => `${k}: ${v}`).join('\n')`. Spanish labels per field would be better still.

### WR-11: `PublicacionService` mixes transactional and non-transactional methods and relies on Open-Session-In-View

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:54-135`
**Issue:** `buscar`, `obtenerPorId`, `crear`, `actualizar` and `cambiarEstado` have no `@Transactional`. Yet `PublicacionMapper.toResponse` reads lazy associations (`agencia`, `fotos`). `actualizar` and `cambiarEstado` load the entity, then the mapper reads the `fotos` collection after the repository call has returned. This works only because `spring.jpa.open-in-view` defaults to true. Turning it off, which Boot recommends and warns about at startup, would cause `LazyInitializationException` in these paths. `actualizar` and `cambiarEstado` also run a read, a write and a read as separate units, while `cambiarDestacado` is `@Transactional`, so the class is inconsistent.

**Fix:** Annotate the class with `@Transactional` and mark read-only methods with `@Transactional(readOnly = true)`. Set `spring.jpa.open-in-view: false` and add a test that exercises the mapper outside a session.

## Info

### IN-01: Login shows "Email o contraseña incorrectos" for every failure

**File:** `../danteautomotores-front/src/pages/LoginPage.jsx:31-33`
**Issue:** The bare `catch` shows a wrong-credentials message for a network failure, a 500 or a 502 too. Someone who types the right password during a backend outage is told it's wrong.
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

**File:** `src/main/java/com/danteautomotores/service/PublicacionService.java:266-283`
**Issue:** `afterCommit` loops over up to 10 sequential Cloudinary calls on the request thread, so the 204 waits for all of them. A Cloudinary failure is only logged, so the asset stays orphaned and nothing records it. The design trades a possible orphan for safety, which is reasonable, but nothing lets the admin find or fix orphans later.
**Fix:** Send the deletes through an `@Async` executor, or write the `public_id` to a `cloudinary_pendientes` table and clean it up with a scheduled job. At minimum log at `error` with the `public_id` so orphans can be recovered from the log.

### IN-05: Agency slug generation yields empty or dash-terminated slugs

**File:** `src/main/java/com/danteautomotores/service/AgenciaService.java:90-97`
**Issue:** `trim()` runs before the punctuation is removed. `"Dante !"` becomes `"dante "` and then `"dante-"`. A name made only of symbols or emoji gives `""`, and `GET /api/agencias/{slug}` can't reach that agency. Names with non-Latin characters also lose all their letters.
**Fix:** Strip the punctuation first, then `trim()`, then collapse whitespace and dashes. Use a fallback like `"agencia"` when the result is empty.

### IN-06: Photo ordering is not deterministic for equal or null `orden`

**File:** `src/main/java/com/danteautomotores/mapper/PublicacionMapper.java:34-35` (and `entity/Publicacion.java:82-84`)
**Issue:** The mapper sorts only by `orden`, and `fotos` has no `@OrderBy`. `resecuenciarFotos` breaks ties by id, but the mapper does not. For legacy photos with all-null `orden`, the cover shown before a delete can differ from the one the resequence picks. The comment in the service claims the two use the same criterion, which isn't true.
**Fix:** Add `.thenComparing(FotoPublicacion::getId, Comparator.nullsLast(Comparator.naturalOrder()))` to the mapper, and put the comparator in one shared place.

### IN-07: Documentation drift in the README

**File:** `README.md:20, 46, 62-65`
**Issue:** The README says Postgres runs on port 5432, but `docker-compose.yml` maps `5433:5432` and the default JDBC URL uses 5433. It says "Maven + JDK 25", but the Dockerfile uses JDK 21. The test instructions point at `jdk-17` with `-Djava.version=17`. The project's baseline is Java 21.
**Fix:** Update those lines so they match the compose file, the Dockerfile and `pom.xml`.

### IN-08: Smaller issues in seeding, build config and tests

**File:** `src/main/java/com/danteautomotores/config/DataSeeder.java:70-93`; `src/main/resources/application.yml:17-21`; `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java`
**Issue:**
- `DataSeeder` doesn't trim or lowercase `ADMIN_EMAIL`. A trailing space from a pasted env var creates an admin who can't log in. `contains("@")` is a very weak email check.
- `show-sql: true` and `ddl-auto: update` apply in every profile, including production. SQL is dumped to the logs, and the schema is changed automatically with no migrations.
- The service tests are all Mockito-only. Nothing exercises a real transaction, so the `afterCommit` ordering, orphan removal, `orden` persistence through dirty checking, and the cascade delete order are untested end to end. Only the synchronization registration is simulated by hand.
- If loading a publication fails in edit mode, the form stays in edit mode with the default empty values. A PUT from that blank form would overwrite the car.

**Fix:** Normalize the seeder email with `trim().toLowerCase()`. Move `show-sql` and `ddl-auto` to a dev profile. Add one `@DataJpaTest` or `@SpringBootTest` with Testcontainers or H2 for delete and reorder. Disable the edit form when the load fails.

---

_Reviewed: 2026-10-02_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
