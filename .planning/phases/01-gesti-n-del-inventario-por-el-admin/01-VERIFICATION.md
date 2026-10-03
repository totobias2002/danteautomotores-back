---
phase: 01-gesti-n-del-inventario-por-el-admin
verified: 2026-10-03T03:05:00Z
status: human_needed
score: 8/10 must-haves verified
covered_files:
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-01-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-01-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-02-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-02-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-03-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-03-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-04-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-04-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-05-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-05-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-06-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-06-SUMMARY.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-07-PLAN.md
  - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-07-SUMMARY.md
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
covered_digest: "v2:sha256:4302bdcbb3a916c85dc2712b5ba2ba59bab6650ebec98cb7bc713920ed83c3c5"
behavior_unverified: 1
overrides_applied: 0
re_verification: false
gaps: []
deferred:
  - truth: "El backend no arranca con el secret JWT por defecto del repo (CR-01 de 01-REVIEW.md); app.jwt.secret sigue con un fallback publico en application.yml"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC5: 'El backend no arranca si falta el secret JWT o las credenciales de Cloudinary' (PROD-02)"
  - truth: "CORS acepta la lista de origenes aunque tenga espacios (WR-05: allowedOrigins.split(',') sin trim)"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC5: 'CORS acepta la lista de origenes aunque tenga espacios' (PROD-02)"
  - truth: "Perfil de produccion sin ddl-auto: update ni show-sql (application.yml los mantiene en todos los perfiles)"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC4: 'perfil de produccion (sin ddl-auto: update ni show-sql)' y nota de Flyway (PROD-04)"
behavior_unverified_items:
  - truth: "Con un token vencido o malformado, el panel lleva al admin a /login (con aviso de sesion vencida y vuelta a la pagina de origen) en vez de romperse"
    test: "Con el stack arriba, entrar como admin a /admin, poner localStorage.token = 'basura' (o dejar vencer APP_JWT_EXPIRATION_MS=60000) y recargar /admin o tocar cualquier accion del panel"
    expected: "Se limpia la sesion, se llega a /login con el cartel 'Tu sesion vencio...', y al ingresar de nuevo se vuelve a /admin. Un login con password incorrecta muestra solo 'Email o contrasena incorrectos', sin cartel de sesion vencida ni redireccion"
    why_human: "No hay test runner en el front. api.js (interceptor 401), AuthContext (registrarManejadorSesionVencida) y LoginPage estan presentes y conectados, pero la secuencia logout() + navigate() dentro de un callback de promesa (batching de React/Router) solo se comprueba en un navegador"
human_verification:
  - test: "Arranque real con Postgres (docker-compose) y ADMIN_EMAIL/ADMIN_PASSWORD/ADMIN_NOMBRE; POST /api/auth/login con esas credenciales"
    expected: "200 con rol ADMIN; la tabla agencias tiene 'Dante Automotores'/'dante-automotores'; reiniciar con otra ADMIN_PASSWORD no cambia la clave; con SPRING_PROFILES_ACTIVE=prod, sin ADMIN_* y sin admin, el arranque aborta con IllegalStateException"
    why_human: "Los tests de DataSeeder son Mockito; nunca corrio contra una base real"
  - test: "Con ddl-auto: update sobre una base que ya tiene filas en publicaciones/fotos_publicacion, arrancar el back"
    expected: "Se agregan sin error la columna destacado (boolean default false not null) y public_id; los autos viejos quedan destacado=false"
    why_human: "El schema lo genera Hibernate en runtime; ningun test lo ejecuta contra PostgreSQL"
  - test: "Panel completo contra la API real: crear un auto, editarlo (cambiar agencia, precio), recargar; cambiar estado a RESERVADO/VENDIDO con el select; marcar y desmarcar la estrella de destacado; filtrar por estado y buscar con acentos"
    expected: "Todo persiste al recargar; un auto reservado/vendido sigue visible en el panel con su estado; el destacado no cambia al cambiar de estado ni al editar"
    why_human: "Requiere navegador + back + base; el front no tiene tests"
  - test: "Eliminar un auto que tiene consultas y favoritos reales desde el panel"
    expected: "El dialogo (ConfirmDialog) muestra solo cantidades, el boton Eliminar espera el conteo, el auto desaparece y no vuelve al recargar; no hay error de FK; los assets de Cloudinary desaparecen despues del commit"
    why_human: "Los tests de cascada y afterCommit son Mockito con la sincronizacion simulada a mano; nunca se ejercito una transaccion JPA real ni el orden de borrado (favoritos, consultas, fotos, publicacion) contra PostgreSQL"
  - test: "Con credenciales CLOUDINARY_* reales: subir una JPG valida, un PDF, una imagen de 12-15 MB, un .exe renombrado a .jpg, una tanda mixta; llegar a 10 fotos; reordenar con flechas y 'Hacer portada'; eliminar la portada; recargar"
    expected: "JPG aparece; PDF rechazado ('formato no permitido'); >10 MB rechazado con mensaje claro y sin cortar la conexion (413 de Tomcat con max-swallow-size 50MB); .exe rechazado por el backend; en la tanda cada archivo malo muestra 'nombre: motivo' y los buenos se suben; la 11a foto no se puede subir; el orden y la portada persisten al recargar; al borrar la portada la siguiente pasa a orden 0 y el asset desaparece de Cloudinary; con credenciales faltantes la API responde 502 en espanol"
    why_human: "Cloudinary esta mockeado en los tests; los limites de Tomcat no se aplican en MockMvc"
  - test: "Disparar varias requests simultaneas con token vencido (por ejemplo, el dashboard hace GET /agencias y GET /admin/publicaciones al montar)"
    expected: "Una sola limpieza de sesion y una sola redireccion a /login (el handler de api.js solo actua mientras haya token en localStorage y logout() lo borra sincronamente)"
    why_human: "Truth marcada verification: backstop (no inferible): la presencia del chequeo localStorage.getItem('token') no prueba la ausencia de doble redireccion; no hay test de concurrencia. Estado insufficient_spec"
  - test: "Revision humana de la prohibicion judgment-tier de 01-01: el aviso de sesion vencida NO debe aparecer ni forzar redireccion ante un 401 de /auth/*"
    expected: "Confirmar en navegador que un login fallido no muestra el cartel ni redirige. Veredicto del verificador (NO autoritativo, LLM-judge): api.js excluye error.config.url.startsWith('/auth/') y LoginPage.handleSubmit solo hace setError; flag unverified-prohibition - human review recommended"
    why_human: "Prohibicion de tier judgment: el protocolo exige resolucion humana explicita, nunca un pase silencioso"
---

# Phase 1: Gestion del inventario por el admin - Verification Report

**Phase Goal:** La unica cuenta admin gestiona el inventario completo de autos (datos, fotos, estado y destacados) desde el panel, contra la API real y con errores manejados de forma uniforme.
**Verified:** 2026-10-03
**Status:** human_needed
**Re-verification:** No - initial verification
**Decisiones revisadas aplicadas:** se verifico contra 01-CONTEXT.md (D-05..D-07 revisados el 2026-10-02: varias agencias gestionadas por el mismo admin), no contra el texto de agencia unica que queda en 01-RESEARCH.md / 01-PATTERNS.md.

## Resumen

El codigo del objetivo existe, esta conectado de punta a punta (panel React, axios, controllers, services, repos) y no hay stubs ni datos mock en ninguna de las rutas de la fase. La suite del back corre en verde (124 tests, ejecutada por el verificador: `mvn -B -o -Djava.version=17 test`, BUILD SUCCESS) y el build del front pasa (`npm run build`, 1981 modulos). No encontre ninguna truth FALLIDA ni ningun artefacto faltante. Lo que impide marcar `passed` es que **nada de la fase se ejecuto nunca contra PostgreSQL, Cloudinary ni un navegador**: todos los tests del back son Mockito o MockMvc de slice, y el front no tiene test runner. Las secuencias de runtime (sesion vencida en el navegador, transaccion real de borrado en cascada, creacion de columnas por `ddl-auto`, Cloudinary real) quedan como items de verificacion humana.

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | SC1: la cuenta admin existe al arrancar (config/seed) y desde la web no se puede registrar ni obtener otra cuenta admin | VERIFIED | `config/DataSeeder.java`: `ApplicationRunner`, `existsByRol(ADMIN)` antes de crear (no sincroniza si ya hay), BCrypt via `PasswordEncoder`, falla con `IllegalStateException` bajo perfil `prod` si faltan/invalidas las variables, nunca promueve un email existente, no loguea la clave (solo el email). `application.yml` lee `ADMIN_*` con default vacio. `RegistroRequest` no tiene campo `rol`; `AuthService.registrar` fuerza `Rol.COMPRADOR`; `AuthController` solo expone `/registro` y `/login`. `DataSeederTest` (13 casos) y `AuthServiceTest` (`registroConRolAdminEnElBody_siempreCreaComprador`) en verde. Arranque real: ver Human Verification 1 |
| 2  | SC2: el admin crea, edita y elimina una publicacion desde el panel y los cambios persisten | VERIFIED | `PublicacionController` POST/PUT/DELETE con `@Valid`, solo ADMIN en `SecurityConfig`; `PublicacionService.crear/actualizar` validan que la agencia exista (404 "No existe una agencia con id: X"), `eliminar` borra favoritos, consultas, fotos y publicacion en una `@Transactional`. Front: `AdminPublicacionFormPage` hace `api.post/put('/publicaciones')` con el payload y `AdminDashboardPage` hace `api.delete` tras `ConfirmDialog` (sin `window.confirm`; grep sin resultados). Sin mocks (`USE_MOCK_DATA` no aparece en `pages/admin`). Persistencia real: Human Verification 3 y 4 |
| 3  | SC3: el admin sube, reordena y elimina fotos; archivo invalido o >10 MB se rechaza con mensaje claro | VERIFIED | `ImagenValidator` (content-type permitido + magic bytes JPEG/PNG/WebP + 10 MB), invocado en `agregarFoto` antes de `cloudinaryService.subir`; tope `MAX_FOTOS=10` en el back; `orden = max+1`; `publicId` guardado; `reordenarFotos` exige el conjunto exacto de ids (400 si falta, repetido o ajeno); `eliminarFoto` resecuencia 0..n-1 y borra el asset en `afterCommit` (best-effort, omite `publicId` nulo); `CloudinaryService` convierte fallas del SDK en `ServicioExternoException` -> 502; `GlobalExceptionHandler` mapea 413 a mensaje en espanol; `application.yml` tiene `max-file-size 10MB` y `max-swallow-size 50MB`. Front: prevalidacion, errores "nombre: motivo" por archivo, flechas y "Hacer portada", `ConfirmDialog` para borrar. `ImagenValidatorTest` (15), `CloudinaryServiceTest` (10), `PublicacionServiceTest` (32) en verde. Cloudinary y Tomcat reales: Human Verification 5 |
| 4  | SC4: el admin cambia el estado y marca/desmarca destacado desde el panel | VERIFIED | `PATCH /{id}/estado` y `PATCH /{id}/destacado` (`CambiarDestacadoRequest` con `@NotNull Boolean`: body sin campo da 400); `Publicacion.destacado` `@ColumnDefault("false")` + `@Builder.Default`; `PublicacionMapper` expone `destacado`; destacado solo lo cambia `cambiarDestacado` (tests: pasar a VENDIDO y `actualizar` no lo tocan; una publicacion nueva nace sin destacar). Front: estrella con `aria-pressed` -> `api.patch('/publicaciones/${id}/destacado')`, select de estado en linea que reemplaza solo la fila (no desaparece), filtro por estado con conteos y busqueda sin acentos. `GET /api/admin/publicaciones` devuelve todos los estados (ADMIN) |
| 5  | SC5 (API): con token vencido o malformado la API responde 401 (no 500) con formato uniforme | VERIFIED | `JwtAuthenticationFilter` captura `JwtException`/`IllegalArgumentException`/`UsernameNotFoundException`, limpia el contexto y deja seguir la request; `RestAuthenticationEntryPoint` (401) y `RestAccessDeniedHandler` (403) escriben `{"error": ...}` y estan registrados en `SecurityConfig.exceptionHandling`. `SeguridadErroresTest` cubre token malformado, vacio, vencido y de usuario borrado: 401 JSON en endpoints protegidos y 200 en publicos; comprador 403 JSON |
| 6  | SC5 (panel): con un token vencido el panel lleva al admin al login en vez de romperse (aviso de sesion vencida y vuelta a la pagina de origen) | PRESENT_BEHAVIOR_UNVERIFIED | Presente y conectado: `api.js` interceptor de respuesta (401, fuera de `/auth/`, solo si hay token), `registrarManejadorSesionVencida` registrado en un `useEffect` de `AuthProvider` (logout + `navigate('/login', {state:{from, sesionVencida:true}})`), `LoginPage` muestra el cartel y vuelve a `from` solo si empieza con `/` y no con `//`. Es una transicion de estado de runtime sin test (el front no tiene runner): ver `behavior_unverified_items` |
| 7  | D-05..D-07 (revisados): el admin crea, edita y elimina agencias; cada auto pertenece a una agencia elegida en el form; no se puede eliminar una agencia con autos (400) | VERIFIED | `AgenciaService.eliminar`: 404 si no existe, `IllegalArgumentException` (-> 400 con mensaje claro) si `existsByAgenciaId`, sino `deleteById`; `listar` ordenado por id. `SecurityConfig`: POST/PUT/DELETE de `/api/agencias/**` solo ADMIN, GET publico. `DataSeeder` siembra "Dante Automotores" solo si `agenciaRepository.count()==0`. `AgenciaServiceTest` y `AgenciaControllerTest` (403 comprador, 401 anonimo, 400 con autos) en verde. Front: ABM de agencias en `AdminDashboardPage` con confirmacion inline y error del backend visible; selector `agenciaId` en el form de publicaciones |
| 8  | PROD-01: todo error de la API tiene el formato uniforme `{"error": ...}` (y `campos` en validaciones); un error inesperado da 500 generico sin stacktrace | VERIFIED | Un unico `@RestControllerAdvice` (`grep ControllerAdvice` en `src/main`: solo `GlobalExceptionHandler`) que extiende `ResponseEntityExceptionHandler`; sin handlers propios para `MethodArgumentNotValidException` ni `MaxUploadSizeExceededException`; `handleExceptionInternal` reemplaza el ProblemDetail por `{"error"}`; `handleUnexpected` devuelve mensaje fijo y loguea el detalle; `ServicioExternoException` -> 502; `AccessDeniedException` -> 403. `GlobalExceptionHandlerTest` en verde. El front lee `error`/`campos` en `utils/errores.js` y lo usa en todos los forms y acciones del panel |
| 9  | Eliminar una publicacion con favoritos/consultas no falla por FK: cascada en una transaccion, aviso previo con solo cantidades, Cloudinary solo tras commit | VERIFIED (nivel unitario) | `PublicacionService.eliminar` (`@Transactional`) y `eliminarImagenesDespuesDelCommit` (`TransactionSynchronization.afterCommit`; sin transaccion activa borra en el momento; filtra `publicId` nulo/blanco). `GET /api/admin/publicaciones/{id}/impacto-eliminacion` devuelve solo `{cantidadConsultas, cantidadFavoritos}` (ADMIN; 403/401/404 cubiertos). Tests: `eliminarBorraLosAssetsDeCloudinarySoloDespuesDelCommit...`, `siLaTransaccionNoHaceCommitCloudinaryNoSeToca`, `elImpactoDeEliminacionSoloTieneDosConteosSinDatosPersonales`. Limite: la sincronizacion se simula a mano en Mockito; la transaccion JPA real es Human Verification 4 |
| 10 | Concurrencia (edge PROD-01, `verification: backstop`): varias requests con 401 simultaneas disparan una sola limpieza de sesion y una sola redireccion | UNCERTAIN (insufficient_spec) | El guard `localStorage.getItem('token')` + `logout()` sincrono esta en `api.js`/`AuthContext.jsx`, pero para una truth no inferible la presencia y el wiring nunca alcanzan; no hay test de concurrencia ni observacion directa. Se abstiene y pasa a Human Verification 6. No cuenta para el score |

**Score:** 8/10 truths verified (1 present, behavior-unverified; 1 insufficient_spec)

### Deferred Items

Items que las revisiones de codigo senalaron y que una fase posterior del milestone cubre de forma explicita. No son gaps accionables de esta fase.

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Secret JWT con fallback publico en `application.yml` (CR-01, critico del review, anterior a esta fase desde el scaffold 7e0dd33) | Phase 2 | SC5: "El backend no arranca si falta el secret JWT o las credenciales de Cloudinary" (PROD-02) |
| 2 | CORS sin `trim()` de origenes (WR-05) | Phase 2 | SC5: "CORS acepta la lista de origenes aunque tenga espacios" |
| 3 | `ddl-auto: update` y `show-sql: true` en todos los perfiles (IN-08) | Phase 2 | SC4: perfil de produccion sin `ddl-auto: update` ni `show-sql` + migraciones Flyway |

**Aclaracion sobre CR-01 y ADM-01:** el fallback del secret es anterior a esta fase y su correccion pertenece a PROD-02, asi que no es un gap de Phase 1. Pero mientras no se cierre, la garantia "no se puede obtener otra cuenta admin" de SC1 vale solo si `APP_JWT_SECRET` esta seteado en el entorno: quien conozca el email del admin y el secret publico puede firmar un JWT valido. Setear `APP_JWT_SECRET` antes de cualquier despliegue.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `controller/AdminPublicacionController.java` | GET listado admin + impacto-eliminacion | VERIFIED | Existe, sustantivo, conectado a `PublicacionService.listarParaAdmin/obtenerImpactoEliminacion`; `/api/admin/**` exige ADMIN |
| `security/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java` | 401/403 JSON | VERIFIED | Registrados en `SecurityConfig.exceptionHandling` |
| `config/DataSeeder.java` | Seed de agencia y admin | VERIFIED | `implements ApplicationRunner`; `UsuarioRepository.existsByRol` |
| `exception/GlobalExceptionHandler.java`, `ServicioExternoException.java` | Advice unico, 502 | VERIFIED | Ver truth 8 |
| `service/ImagenValidator.java` | Tipo real, tamano, `MAX_FOTOS = 10` | VERIFIED | Ver truth 3 |
| `service/CloudinaryService.java` | `ImagenSubida(url, publicId)` + `eliminar` best-effort | VERIFIED | `record ImagenSubida`; borra con `invalidate` y nunca lanza |
| `dto/publicacion/{CambiarDestacadoRequest,ReordenarFotosRequest,ImpactoEliminacionResponse}.java` | DTOs validados | VERIFIED | `@NotNull Boolean`, `@NotEmpty List<Long>`, dos conteos |
| `entity/Publicacion.java` / `FotoPublicacion.java` | `destacado` con default, `publicId` | VERIFIED | `@ColumnDefault("false")`; `public_id` nullable |
| Front `utils/errores.js`, `services/api.js`, `components/ConfirmDialog.jsx` | Mensaje del backend, interceptor 401, dialogo nativo | VERIFIED | Existen y estan importados por Dashboard, Form, AuthContext |
| Front `pages/admin/AdminDashboardPage.jsx`, `AdminPublicacionFormPage.jsx` | Panel real | VERIFIED | Sin mocks ni stubs; rutas `/admin*` bajo `ProtectedRoute soloAdmin` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AdminDashboardPage.jsx` | `AdminPublicacionController` | `api.get('/admin/publicaciones')` | WIRED | `cargar()`; la respuesta alimenta `setPublicaciones` |
| `AdminDashboardPage.jsx` | `PublicacionController` | `api.patch('/publicaciones/${id}/destacado')` y `/estado` | WIRED | El resultado reemplaza la fila |
| `AdminDashboardPage.jsx` | `AdminPublicacionController` | `impacto-eliminacion` antes del dialogo | WIRED | Confirmar queda deshabilitado hasta tener el conteo |
| `AdminDashboardPage.jsx` | `AgenciaController` | `api.delete('/agencias/${id}')` | WIRED | Error del backend visible |
| `AdminPublicacionFormPage.jsx` | `PublicacionController` | `/fotos`, `/fotos/orden`, `/fotos/{id}`, POST/PUT publicaciones | WIRED | Todas con `mensajeDeError` |
| `SecurityConfig` | `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` | `exceptionHandling(...)` | WIRED | |
| `AuthContext.jsx` | `api.js` | `registrarManejadorSesionVencida` en `useEffect` | WIRED | Se desregistra al desmontar |
| `PublicacionService` | `ImagenValidator` / `CloudinaryService` | `validar` antes de `subir`; `eliminar` en `afterCommit` | WIRED | |
| `AgenciaService` | `PublicacionRepository` | `existsByAgenciaId` antes de `deleteById` | WIRED | |
| `DataSeeder` | `application.yml` / `UsuarioRepository` | `@Value("${app.admin.*:}")`, `existsByRol(Rol.ADMIN)` | WIRED | |
| `PublicacionMapper` | `PublicacionResponse` | `.destacado(publicacion.isDestacado())` | WIRED | |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AdminDashboardPage` listado | `publicaciones` | `GET /admin/publicaciones` -> `publicacionRepository.findAll(Sort DESC fechaPublicacion)` | Si (consulta JPA real, sin filtro de estado) | FLOWING |
| `AdminDashboardPage` agencias | `agencias` | `GET /agencias` -> `agenciaRepository.findAll(Sort id)` | Si | FLOWING |
| `AdminPublicacionFormPage` edicion | `publicacion`, `form` | `GET /publicaciones/{id}` | Si | FLOWING |
| Dialogo de eliminacion | `impacto` | `GET .../impacto-eliminacion` -> `countByPublicacionId` x2 | Si | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Suite del back | `mvn -B -o -Djava.version=17 test` (una sola corrida completa) | 124 tests, 0 fallas, 0 errores, BUILD SUCCESS (los stacktraces del log son excepciones esperadas de los tests de error) | PASS |
| Build del front | `npm run build` (en `danteautomotores-front`) | 1981 modulos, build ok | PASS |
| Arbol limpio | `git status --short` en el front; en el back solo `.gsd/`, `.planning/milestone.lock`, `.planning/state.json` sin trackear | Todo el codigo de la fase esta commiteado | PASS |

### Probe Execution

Step 7c: SKIPPED - no hay `scripts/*/tests/probe-*.sh` ni probes declarados en los PLAN/SUMMARY de la fase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| ADM-01 | 01-02 | Una unica cuenta admin por config/seed; no se puede registrar otra desde la web | SATISFIED (arranque real pendiente) | Truth 1 |
| ADM-02 | 01-03, 01-07 | Crear, editar y eliminar publicaciones desde el panel contra la API real | SATISFIED (persistencia real pendiente) | Truths 2, 7, 9 |
| ADM-03 | 01-06, 01-07 | Subir, reordenar y eliminar fotos (solo imagenes validas, con limite) | SATISFIED (Cloudinary real pendiente) | Truth 3 |
| ADM-04 | 01-01, 01-05 | Cambiar estado disponible/reservado/vendido | SATISFIED | Truth 4 |
| ADM-05 | 01-05 | Marcar o desmarcar destacados | SATISFIED | Truth 4 |
| PROD-01 | 01-01, 01-04 | Token invalido o vencido da 401; errores con formato uniforme | SATISFIED en la API; el redirect del panel esta en human verification | Truths 5, 6, 8, 10 |

Los seis IDs de ROADMAP.md y de REQUIREMENTS.md (tabla de trazabilidad) para la Fase 1 aparecen en el frontmatter `requirements` de al menos un PLAN. **Sin requisitos huerfanos** (REQUIREMENTS.md no mapea ningun otro ID a la Fase 1).

### Prohibitions

| Prohibition | Tier | Result |
|-------------|------|--------|
| 01-01: no mostrar sesion vencida ni redirigir ante un 401 de `/auth/*` | judgment | Veredicto NO autoritativo del verificador: cumple (`api.js` excluye `/auth/`; `LoginPage` solo hace `setError`). **unverified-prohibition - human review recommended** (Human Verification 7) |
| 01-02: no escribir `ADMIN_PASSWORD` ni su hash en logs/respuestas/archivos | test | Con enforcement cableado: `DataSeederTest` usa `CapturedOutput`; el seeder solo loguea el email; `application.yml` y README no traen valores reales |
| 01-02: no promover un usuario existente a ADMIN | test | Con enforcement cableado: `emailYaRegistradoConPerfilProd_lanzaYNuncaPromueve` y `...SinPerfilProd_noGuardaNiPromueve` |
| 01-05: destacado no cambia como efecto secundario | test | Con enforcement cableado: `pasarUnAutoDestacadoAVendidoNoLoDesmarca`, `actualizarUnaPublicacionDestacadaNoCambiaElDestacado` |
| 01-07: impacto-eliminacion sin datos personales | test | Con enforcement cableado: `elImpactoDeEliminacionSoloTieneDosConteosSinDatosPersonales`; el DTO tiene dos `long` |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (todo `src/` de ambos repos) | - | `TBD`/`FIXME`/`XXX` | ninguno | Los unicos matches son la palabra "TODOS" (filtro de estado) |
| front `utils/whatsapp.js:1`, `LoginPage.jsx` (Google) | - | `TODO` | Info | Anteriores a la fase y fuera de su alcance (WhatsApp y Google login: Fase 3) |
| `application.yml` | 17-18, 30 | `ddl-auto: update`, `show-sql: true`, secret JWT por defecto | Warning | Diferido a Fase 2 (ver Deferred) |

### Hallazgos de 01-REVIEW.md que afectan al objetivo (advisory)

El review marco 1 critico, 11 warnings y 8 info; **01-REVIEW-DISPOSITION.md los deja todos en `open`** (sin triar). Ninguno invalida una truth, pero estos rozan el objetivo y conviene triarlos antes de cerrar la fase:

- **CR-01** (secret JWT por defecto): diferido a Fase 2 (arriba), con la salvedad sobre ADM-01.
- **WR-03** (`PublicacionRequest` casi sin validacion de servidor: `anio`, `kilometraje` negativo, `moneda` libre, largos, `precio` sin `@Digits`): un dato fuera de rango llega a la base y vuelve como 409 "datos relacionados", mensaje enganoso para el admin. Roza ADM-02 y la promesa de errores claros.
- **WR-01 / WR-02** (tope de 10 fotos y `orden` sin lock bajo subidas concurrentes; asset huerfano en Cloudinary si falla el commit tras subir): el front sube en serie, riesgo bajo con un unico admin.
- **WR-07** (editar un auto con `transmision`/`combustible`/`condicion` nulos los pisa con MANUAL/NAFTA/BUENO) y **WR-06** (precio con decimales se corrompe al editar): solo afectan filas viejas o cargadas por API; los autos creados desde el form siempre traen valores enteros y completos.
- **WR-09** (JSON corrupto en `localStorage.usuario` rompe `AuthProvider`) y **WR-10** (`mensajeDeError` muestra nombres de propiedad Java en vez del mensaje de cada campo): degradan UX, no el flujo principal.
- **WR-11 / WR-04 / IN-***: deuda de calidad, sin impacto en las truths.

### Human Verification Required

Ver el frontmatter `human_verification` y `behavior_unverified_items` (7 items). En resumen, hace falta un pase de UAT con el stack real:

1. **Arranque con Postgres + `ADMIN_*`** y login del admin; reinicio sin sincronizar clave; aborto bajo perfil `prod` sin variables.
2. **`ddl-auto: update` sobre una base con datos** (columnas `destacado` y `public_id`).
3. **Panel completo en el navegador**: crear/editar/estado/destacado/filtro, con recarga para comprobar persistencia.
4. **Eliminar un auto con consultas y favoritos reales** (transaccion JPA real, orden de borrado, `afterCommit`).
5. **Cloudinary real y limites de Tomcat**: JPG valido, PDF, >10 MB, .exe renombrado, tanda mixta, tope de 10, reorden/portada, borrado de portada.
6. **401 concurrentes** con token vencido: una sola redireccion (truth `backstop`).
7. **Redireccion por sesion vencida** (truth 6) y resolucion humana de la prohibicion judgment-tier sobre `/auth/*`.

La lista de variables a configurar esta en `01-USER-SETUP.md` (estado "Incomplete": `ADMIN_*` y `CLOUDINARY_*` aun sin setear).

### Gaps Summary

No hay gaps bloqueantes: ninguna truth fallo, ningun artefacto falta ni es stub, ningun link esta roto. El estado `human_needed` se debe a que toda la evidencia de runtime (PostgreSQL, Cloudinary, navegador) esta pendiente, a una truth de redireccion del front sin test, a una truth `backstop` de concurrencia y a una prohibicion judgment-tier que exige resolucion humana. Los 20 hallazgos del review siguen sin triar, y el critico (CR-01) esta cubierto por la Fase 2.

---

_Verified: 2026-10-03_
_Verifier: Claude (gsd-verifier)_
