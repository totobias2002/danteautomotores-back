---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# Codebase Concerns

**Analysis Date:** 2026-10-02

## Backend Security Issues

### Hardcoded Credentials in Configuration Files

**Issue:** Database password hardcoded in two locations
- Files: `src/main/resources/application.yml` (line 10), `docker-compose.yml` (line 9)
- Password: Hardcoded as default value
- Impact: Development credentials exposed in version control; production must override via environment variables
- Fix approach: Remove default password values and require environment variable. Update CI/CD to inject credentials. Document required env vars: `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`

### Insecure Database Configuration

**Issue:** Multiple JPA/Hibernate settings unsafe for production
- File: `src/main/resources/application.yml` (lines 13-14)
- Settings:
  - `ddl-auto: update` — allows schema modifications at runtime
  - `show-sql: true` — outputs all SQL to logs, exposing data and structure
- Impact: Potential data loss on failed migrations; sensitive data visible in logs
- Fix approach: Create separate `application-prod.yml` with `ddl-auto: validate` and `show-sql: false`

### JWT Secret Default/Placeholder

**Issue:** JWT secret configuration has placeholder comment indicating default in use
- File: `src/main/resources/application.yml` (line 25)
- Value: `CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES` with comment
- Impact: If `APP_JWT_SECRET` env var not set, weak default is used; all tokens predictable
- Fix approach: Make `APP_JWT_SECRET` required (fail on startup if missing). Generate strong secret for production. Verify at deployment time.

### CORS Configuration Parsing Bug

**Issue:** CORS origins split without whitespace trimming
- File: `src/main/java/com/danteautomotores/config/SecurityConfig.java` (line 64)
- Code: `allowedOrigins.split(",")`
- Impact: If env var has spaces (e.g., `http://localhost:3000, http://localhost:5173`), the second origin fails to match due to leading space
- Fix approach: Change to `allowedOrigins.split(",\\s*")` to trim whitespace

## Backend File Upload & Validation Issues

### Missing File Type Validation

**Issue:** No validation of uploaded file types; any file accepted
- File: `src/main/java/com/danteautomotores/service/CloudinaryService.java` (line 18-26)
- Impact: Attackers could upload malicious files (executables, archives, etc.) disguised as images
- Fix approach: 
  1. Whitelist allowed MIME types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`
  2. Validate in `PublicacionService.agregarFoto()` before calling CloudinaryService
  3. Add file size validation (currently hardcoded max 10MB in `application.yml` line 20-21)

### No Frontend File Type Validation

**Issue:** Frontend uploads files without type checking
- File: `danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx` (line 159-185)
- Impact: Bad UX (upload fails server-side); no client-side protection against malicious uploads
- Fix approach: Add file type validation before FormData.append() using `file.type` check

## Backend Authorization & Access Control

### Centralized Authorization Without Method-Level Documentation

**Issue:** All authorization rules in SecurityConfig; controllers lack @Secured/@PreAuthorize annotations
- File: `src/main/java/com/danteautomotores/config/SecurityConfig.java` (lines 43-52)
- Impact: Authorization logic hidden from controller code; fragile to miss when adding new endpoints
- Safe modification: Always add explicit `@PreAuthorize` annotations to controller methods even if SecurityConfig rule exists. This makes intent clear and catches mistakes.
- Example pattern missing from `src/main/java/com/danteautomotores/controller/PublicacionController.java` (should have `@PreAuthorize("hasRole('ADMIN')")` on POST/PUT/DELETE/PATCH methods)

### JWT Token Parsing Without Exception Handling

**Issue:** JwtAuthenticationFilter can throw uncaught exceptions if token is malformed
- File: `src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java` (line 39)
- Code: `jwtService.extractUsername(jwt)` can throw `JwtException`
- Impact: Bad token crashes request with 500 instead of 401; no graceful error response
- Fix approach: Wrap token extraction in try-catch block; return 401 Unauthorized if token invalid

## Testing Gaps

### Zero Test Coverage

**Issue:** No unit, integration, or E2E tests in backend
- Files: None found in codebase; pom.xml includes test dependencies but no tests written
- Impact: Refactors risk breaking authentication, file uploads, or database operations silently
- Test coverage: [MISSING]
  - Authentication flow (login, registration, JWT generation/validation)
  - Authorization checks (ADMIN-only endpoints)
  - File upload validation
  - Database queries and Specifications

### Frontend Zero Test Coverage

**Issue:** No test dependencies or test configuration in React app
- File: `danteautomotores-front/package.json` (lines 1-24)
- Impact: No safety net for auth context changes, routing, or API interaction bugs
- Test coverage: [MISSING]
  - AuthContext login/logout flows
  - ProtectedRoute component
  - API error handling
  - localStorage persistence

### Build Skips Tests

**Issue:** Dockerfile skips tests during Maven build
- File: `Dockerfile` (line 7): `-DskipTests` flag
- Impact: Broken tests go undetected in production builds
- Fix approach: Remove `-DskipTests`. If tests are missing, write them. If they fail legitimately, fix them.

## Frontend Issues & Missing Features

### Incomplete Google OAuth Integration

**Issue:** Multiple TODOs indicating Google login not connected to backend
- Files: 
  - `danteautomotores-front/src/pages/LoginPage.jsx` (line 33)
  - `danteautomotores-front/src/pages/RegistroPage.jsx` (line 39)
- Impact: UI shows Google button but it's non-functional; users can't see this feature works
- Fix approach: Either implement Google OAuth end-to-end with backend, or hide UI elements until ready

### Mock Data Still in Production Code

**Issue:** Multiple hardcoded mock datasets loaded instead of API calls
- Files:
  - `danteautomotores-front/src/pages/HomePage.jsx` (line 12)
  - `danteautomotores-front/src/pages/AutosPage.jsx` (line 17)
  - `danteautomotores-front/src/pages/AgenciaPage.jsx` (line 12)
  - `danteautomotores-front/src/pages/PublicacionDetallePage.jsx` (line 25)
  - `danteautomotores-front/src/mocks/catalogoMock.js` (line 4)
- Impact: Real data never displayed; UI doesn't reflect actual catalog
- Fix approach: Replace mock imports with actual API calls via `api.get()`. Remove mock files once frontend fully integrated.

### WhatsApp Integration Placeholder

**Issue:** WhatsApp number hardcoded as placeholder
- File: `danteautomotores-front/src/utils/whatsapp.js` (line 1-3)
- Current value: `5491100000000` (fake number)
- Impact: Links to WhatsApp won't work until real company number configured
- Fix approach: Load from backend API endpoint or environment variable `VITE_WHATSAPP_NUMBER`

### Missing Error Handling in Auth Context

**Issue:** AuthContext login/register functions don't catch or report errors
- File: `danteautomotores-front/src/context/AuthContext.jsx` (lines 12-19)
- Impact: If API call fails (network error, invalid credentials, server error), function throws uncaught exception; no error message shown
- Safe modification: Add try-catch with return/throw of user-readable error. Update calling pages (LoginPage, RegistroPage) to handle returned errors.

### Insecure Token Storage

**Issue:** JWT token stored in localStorage, vulnerable to XSS attacks
- File: `danteautomotores-front/src/context/AuthContext.jsx` (line 23)
- Impact: Any XSS vulnerability in app allows attacker to steal JWT and impersonate user
- Mitigation in place: None visible
- Recommendations:
  1. Use httpOnly cookies instead of localStorage (requires backend to set `Set-Cookie` header)
  2. Implement Content Security Policy (CSP) headers to mitigate XSS
  3. Sanitize any user-generated content rendered in the app

## Cross-Cutting Concerns

### Environment Configuration Fragmentation

**Issue:** Secrets scattered across multiple config files with inconsistent defaults
- Files:
  - Backend: `src/main/resources/application.yml` (SPRING_DATASOURCE_PASSWORD, APP_JWT_SECRET, CLOUDINARY_*)
  - Frontend: `danteautomotores-front/src/services/api.js` (VITE_API_URL hardcoded to http://localhost:8080/api)
  - Docker: `docker-compose.yml` (POSTGRES_PASSWORD hardcoded)
- Impact: Easy to accidentally commit secrets; hard to manage across dev/staging/prod
- Fix approach: 
  1. Create `.env.example` for each project documenting required variables
  2. Update Dockerfile to fail if required secrets not provided
  3. Add pre-commit hook to prevent `.env` files from being committed

### Cloudinary Credentials Not Validated

**Issue:** Empty default values for Cloudinary config
- File: `src/main/resources/application.yml` (lines 31-33)
- Impact: If `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` not set, uploads silently fail or crash
- Fix approach: Add startup validation to fail if any Cloudinary env var is missing or empty. Log clear error message.

### Frontend-Backend Contract Not Validated

**Issue:** No shared type definitions or API schema documentation
- Impact: Frontend and backend can drift (e.g., new field added to response, frontend expects old shape); causes silent bugs
- Fix approach:
  1. Document API contract (OpenAPI/Swagger spec)
  2. Generate TypeScript types from backend (e.g., using openapi-generator)
  3. Add integration tests verifying request/response shapes

### CORS Whitelist Too Permissive Potential

**Issue:** CORS allows `*` (all headers) without nuance
- File: `src/main/java/com/danteautomotores/config/SecurityConfig.java` (line 66)
- Code: `configuration.setAllowedHeaders(List.of("*"))`
- Impact: Any header can be sent; not a critical vulnerability but overly permissive
- Fix approach: Change to specific headers: `["Authorization", "Content-Type", "Accept"]`

## Performance Considerations

### No Database Query Optimization Visible

**Issue:** PublicacionService uses broad findAll() with Specifications; no pagination
- File: `src/main/java/com/danteautomotores/service/PublicacionService.java` (lines 36-48)
- Impact: If 10,000+ publications exist, loading all in memory could cause memory exhaustion
- Scaling path: Add `Pageable` parameter to controller, use `findAll(spec, pageable)` to return Page<>

### No Caching Layer

**Issue:** Repeated queries to database (e.g., agencies list, catalog) not cached
- Impact: Performance degrades as data grows
- Improvement path: Add Spring Cache annotations (`@Cacheable`) to frequently read endpoints

## Deployment & Infrastructure

### Dockerfile Alpine JRE Missing Timezone

**Issue:** Alpine Linux Docker image lacks timezone database
- File: `Dockerfile` (line 10)
- Impact: Date/time operations may fail; timestamps could be incorrect
- Fix approach: Add `RUN apk add --no-cache tzdata` to build stage

### No Health Check in Docker

**Issue:** No HEALTHCHECK instruction in Dockerfile
- Impact: Docker can't auto-restart container if app becomes unresponsive
- Fix approach: Add `HEALTHCHECK --interval=30s --timeout=3s CMD curl --fail http://localhost:8080/actuator/health || exit 1` (requires adding actuator endpoint)

## Summary Table

| Category | Severity | Count | Blocking | Docs Needed |
|----------|----------|-------|----------|------------|
| Security | High | 6 | No (2 critical for prod) | Config guide |
| Testing | Critical | 3 | Yes | Test strategy |
| Incomplete Features | Medium | 4 | Partially | Integration plan |
| Performance | Medium | 2 | No | Scaling guide |
| Configuration | High | 3 | Yes (for prod) | Env var docs |

---

*Concerns audit: 2026-10-02*
