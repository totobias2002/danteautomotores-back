---
phase: 01-gesti-n-del-inventario-por-el-admin
verified: 2026-10-03T17:10:00Z
status: passed
score: 11/11 must-haves verified
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
  - Dockerfile
  - README.md
  - src/main/java/com/danteautomotores/config/DataSeeder.java
  - src/main/java/com/danteautomotores/config/SecretosGuard.java
  - src/main/java/com/danteautomotores/config/SecurityConfig.java
  - src/main/java/com/danteautomotores/controller/AdminPublicacionController.java
  - src/main/java/com/danteautomotores/controller/PublicacionController.java
  - src/main/java/com/danteautomotores/dto/publicacion/AnioDeModelo.java
  - src/main/java/com/danteautomotores/dto/publicacion/CambiarDestacadoRequest.java
  - src/main/java/com/danteautomotores/dto/publicacion/ImpactoEliminacionResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/ReordenarFotosRequest.java
  - src/main/java/com/danteautomotores/entity/FotoPublicacion.java
  - src/main/java/com/danteautomotores/entity/Publicacion.java
  - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java
  - src/main/java/com/danteautomotores/exception/ReglaDeNegocioException.java
  - src/main/java/com/danteautomotores/exception/ServicioExternoException.java
  - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
  - src/main/java/com/danteautomotores/repository/ConsultaRepository.java
  - src/main/java/com/danteautomotores/repository/FavoritoRepository.java
  - src/main/java/com/danteautomotores/repository/FotoPublicacionRepository.java
  - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
  - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
  - src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java
  - src/main/java/com/danteautomotores/security/JwtService.java
  - src/main/java/com/danteautomotores/security/RestAccessDeniedHandler.java
  - src/main/java/com/danteautomotores/security/RestAuthenticationEntryPoint.java
  - src/main/java/com/danteautomotores/service/AgenciaService.java
  - src/main/java/com/danteautomotores/service/AuthService.java
  - src/main/java/com/danteautomotores/service/CloudinaryService.java
  - src/main/java/com/danteautomotores/service/ConsultaService.java
  - src/main/java/com/danteautomotores/service/FavoritoService.java
  - src/main/java/com/danteautomotores/service/ImagenValidator.java
  - src/main/java/com/danteautomotores/service/PublicacionService.java
  - src/main/resources/application.yml
covered_digest: "v2:sha256:12f35782f164a00de2c125cef67e1d780eae507906ef5467036b3e49da987c32"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 8/11
  gaps_closed:
    - "SC5 (panel): sesion vencida lleva a /login con aviso y vuelta al origen (UAT 7, navegador)"
    - "Concurrencia 401 (backstop): una sola limpieza de sesion y una sola redireccion (UAT 6, observacion directa)"
    - "Hardening de fotos WR-01/WR-02/WR-11/WR-13: lock, compensacion y open-in-view apagado (UAT 3-5 contra Postgres y Cloudinary reales, mas 172 tests)"
    - "Prohibicion judgment-tier de 01-01 (401 de /auth/* sin aviso ni redireccion): resuelta por un humano en UAT 7"
  gaps_remaining: []
  regressions: []
gaps: []
deferred:
  - truth: "Residual de WR-12: con NINGUN perfil activo SecretosGuard solo avisa (a proposito, para desarrollo local sin configuracion); un deploy sin el Dockerfile debe definir SPRING_PROFILES_ACTIVE (documentado en README). Las credenciales de Cloudinary tampoco abortan el arranque"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC5: 'El backend no arranca si falta el secret JWT o las credenciales de Cloudinary' (PROD-02)"
  - truth: "Perfil de produccion sin ddl-auto: update ni show-sql (application.yml los mantiene en todos los perfiles)"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC4: 'perfil de produccion (sin ddl-auto: update ni show-sql)' y nota de Flyway (PROD-04)"
advisory:
  - finding: "Quick task 261003-6lx (miniatura de portada en el listado del panel): su human-check de la Task 2 (miniaturas, request con c_fill, placeholder, 375 px, fallback al bloquear la transformacion) no se ejecuto. El UAT 3 corrio despues del commit en el mismo panel y el listado funciono con fotos, pero no registra la miniatura visualmente"
    category: other
    reason: "Mejora visual fuera de los Success Criteria de la fase; build de Vite en verde, urlMiniatura comprobada por el verificador (solo res.cloudinary.com por igualdad estricta de host, idempotente, otras URLs intactas), componente MiniaturaPublicacion cableado con fallback transformada -> original -> placeholder. Conviene un vistazo en el navegador, no bloquea"
    evidence_status: "SUMMARY de la quick task declara el check manual pendiente"
  - finding: "IN-01..IN-12 del code review siguen abiertos (calidad; destacan IN-09 redondeo silencioso del precio al editar, IN-10 eliminar() no toma el lock, IN-11 borrado de imagen ante commit de resultado incierto)"
    category: other
    reason: "Informativos, sin impacto en ninguna truth"
    evidence_status: "01-REVIEW-DISPOSITION.md (open: 12)"
---

# Phase 1: Gestion del inventario por el admin - Verification Report

**Phase Goal:** La unica cuenta admin gestiona el inventario completo de autos (datos, fotos, estado y destacados) desde el panel, contra la API real y con errores manejados de forma uniforme.
**Verified:** 2026-10-03
**Status:** passed
**Re-verification:** Yes - el reporte anterior quedo viejo por los fixes WR-12 (afcd4c4) y WR-13 (188f144) del back y la quick task 261003-6lx (eb926c2, f586e4e) del front, y los items humanos ya se ejecutaron (01-UAT.md, 7/7 pass)

> **Aviso sobre el modo MVP.** ROADMAP.md marca la fase con `Mode: mvp` pero el goal no tiene formato de User Story. Se aplico la metodologia goal-backward estandar contra el goal y los 5 Success Criteria. Decision pendiente del equipo (no afecta este resultado): reescribir el goal como User Story (`/gsd mvp-phase 1`) o quitar `Mode: mvp`.

## Resumen

Cada Success Criteria esta respaldado por codigo conectado de punta a punta y por evidencia de runtime. En esta corrida el verificador ejecuto la suite completa del back una sola vez (`mvn -B -o -Djava.version=17 test`): **172 tests, 0 fallas, 0 errores, BUILD SUCCESS** (150 en la verificacion anterior; los 22 nuevos cubren WR-12 y WR-13). `npm run build` del front pasa (`built in 1.93s`) y ambos repos no tienen cambios de codigo sin commitear. No hay marcadores TBD/FIXME/XXX en `src/main` ni en `src` del front, ni `USE_MOCK_DATA`/`window.confirm` en `pages/admin`.

Los puntos que dejaban la fase en `human_needed` quedaron cerrados por 01-UAT.md (7/7 pass, 0 issues): tests 1-2 contra Postgres real; tests 3-7 en Chrome contra el back local (base descartable) y, para fotos, con Cloudinary real via API **sobre el codigo con WR-12/WR-13** (10 subidas, tope en la 11.a, reorden, borrar portada con resecuenciado y asset 404, dos subidas simultaneas con 9 fotos -> una 200 y una 400 con ordenes 0..9 unicos, borrar el auto elimina los 10 assets). Limite declarado por el UAT: parte de las interacciones del navegador se dispararon por JS (`click()`, `requestSubmit()`) porque la pestana estaba en segundo plano; ejercitan los mismos handlers de React pero no el puntero fisico. No lo considero un hueco: la logica bajo prueba esta en esos handlers y en la API.

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | SC1: la cuenta admin existe al arrancar (config/seed) y desde la web no se puede registrar ni obtener otra cuenta admin | VERIFIED | `DataSeeder` (`existsByRol(ADMIN)`, BCrypt, `IllegalStateException` en perfil prod sin `ADMIN_*`, nunca promueve un email existente); `AuthService.registrar` fuerza `Rol.COMPRADOR`; `AuthController` solo expone registro y login. **UAT 1 pass contra Postgres real** (login 200 ADMIN, reinicio con otra clave no la cambia, prod sin `ADMIN_*` aborta con exit 1). Con WR-12 el guard de secretos ya es estricto salvo dev/local/test o sin perfil (ver Deferred) |
| 2  | SC2: el admin crea, edita y elimina una publicacion desde el panel y persiste | VERIFIED | `PublicacionController` POST/PUT/DELETE solo ADMIN; `PublicacionService.crear/actualizar/eliminar` con validacion de agencia y `PublicacionRequest` validado. Front `AdminPublicacionFormPage`/`AdminDashboardPage` usan `api.post/put/delete`. **UAT 3 pass en navegador**: crear, editar agencia y precio, persiste al recargar y en la base. **UAT 4 pass**: eliminar con consulta y favorito reales, ConfirmDialog con solo cantidades, sin error de FK |
| 3  | SC3: sube, reordena y elimina fotos; archivo invalido o >10 MB se rechaza con mensaje claro | VERIFIED | `ImagenValidator` (content-type + magic bytes + 10 MB) antes de `cloudinaryService.subir`; `MAX_FOTOS=10`; `reordenarFotos` exige el conjunto exacto; `eliminarFoto` resecuencia y borra el asset tras el commit; `CloudinaryService` -> 502 en espanol. **UAT 5 pass**: rechazos PDF 400, .exe 400, 12/15 MB 413 en espanol; en el form "nombre: motivo" por archivo; con Cloudinary real la subida, el tope de 10, el reorden, borrar portada y borrar el asset funcionan; flechas y "Hacer portada" persisten en el navegador |
| 4  | SC4: el admin cambia el estado y marca/desmarca destacado desde el panel | VERIFIED | `PATCH /{id}/estado` y `/{id}/destacado`; solo `cambiarDestacado` toca `destacado`. **UAT 3 pass**: RESERVADO y VENDIDO siguen visibles en "Todos", la estrella marca/desmarca y el destacado no cambia al cambiar de estado ni al editar; filtros por estado y busqueda con acentos correctos |
| 5  | SC5 (API): token vencido o malformado -> 401 (no 500) con formato uniforme | VERIFIED | `JwtAuthenticationFilter` captura `JwtException`/`IllegalArgumentException`/`UsernameNotFoundException`; `RestAuthenticationEntryPoint` (401) y `RestAccessDeniedHandler` (403) devuelven `{"error": ...}`. `SeguridadErroresTest` en verde. UAT 6 y 7 observaron los 401 reales |
| 6  | SC5 (panel): con token vencido el panel lleva al admin a /login con aviso y vuelta al origen en vez de romperse | VERIFIED | `api.js` (interceptor 401, excluye `/auth/`), `AuthContext` (`registrarManejadorSesionVencida`), `LoginPage`. **UAT 7 pass en navegador**: con token vencido en `/admin/publicaciones/2/editar?tab=fotos`, guardar -> PUT 401 -> `/login` con "Tu sesion vencio..." y la sesion limpia; al loguearse vuelve a la misma ruta |
| 7  | D-05..D-07: ABM de agencias, cada auto con su agencia, baja bloqueada (400) si tiene autos | VERIFIED | `AgenciaService.eliminar` (404 / `ReglaDeNegocioException` -> 400); escritura de `/api/agencias/**` solo ADMIN; seed de "Dante Automotores"; selector `agenciaId` en el form. `AgenciaServiceTest`, `AgenciaControllerTest` verdes; UAT 3 cambio la agencia de un auto a "Sucursal Pilar" |
| 8  | PROD-01: todo error de la API tiene formato uniforme `{"error": ...}` (`campos` en validaciones); un error inesperado da 500 generico sin stacktrace | VERIFIED | Un unico `@RestControllerAdvice` (`GlobalExceptionHandler`): `ReglaDeNegocioException` 400, `ServicioExternoException` 502, `AccessDeniedException` 403, `DataIntegrityViolationException` 409 y, desde WR-13, `PessimisticLockingFailureException` 409 con mensaje fijo (sin detalle del driver); `grep IllegalArgumentException src/main` solo deja el catch del filtro JWT. `GlobalExceptionHandlerTest` verde. Front `utils/errores.js` muestra los mensajes por campo |
| 9  | Eliminar una publicacion con favoritos/consultas no falla por FK: cascada en una transaccion, aviso previo con solo cantidades, Cloudinary solo tras commit | VERIFIED | `eliminar` `@Transactional` con `eliminarImagenesDespuesDelCommit` (`afterCommit`); `impacto-eliminacion` devuelve dos conteos. **UAT 4 pass** en API y navegador (0 filas huerfanas, sin FK ni LazyInitialization) y borrado de assets confirmado con Cloudinary real (10 assets -> 404). Tests `eliminarBorraLosAssetsDeCloudinarySoloDespuesDelCommit...` y `siLaTransaccionNoHaceCommitCloudinaryNoSeToca` verdes |
| 10 | Concurrencia 401 (`verification: backstop`): varias requests con 401 simultaneas -> una sola limpieza de sesion y una sola redireccion | VERIFIED | Evidencia exogena por observacion directa, no por presencia: **UAT 6 pass**: con un JWT vencido, 4 requests protegidas simultaneas por el cliente `api` de la app (4 x 401) produjeron una sola limpieza de sesion y una sola navegacion a `/login`; el dashboard montado con token vencido (GET /agencias 200 publico, GET /admin/publicaciones 401, doble por StrictMode) termina en `/login` con el aviso |
| 11 | Hardening de fotos (WR-01/02/11/13): operaciones serializadas, imagen subida compensada si el guardado no hace commit, escrituras con `open-in-view: false`, lock con espera acotada y sin red dentro de la transaccion | VERIFIED | Codigo actual de `PublicacionService.agregarFoto` (`NOT_SUPPORTED`): valida y chequea el tope sin lock, sube a Cloudinary sin transaccion, luego `TransactionTemplate` corta (timeout 15 s) con `fijarTimeoutDeLock` (`set_config('lock_timeout','5000',true)`) + `findByIdForUpdate`, re-chequeo del tope bajo lock y, ante cualquier `RuntimeException`, `cloudinaryService.eliminar(publicId)`; `reordenarFotos` y `eliminarFoto` toman el lock primero; `CloudinaryService` con timeouts de conexion y lectura. **Tests que ejercitan la invariante** (`PublicacionServiceTest`, 44): sube antes de abrir transaccion y de tomar el lock, borra la imagen si falla el guardado, el commit o el lock, la conserva si todo sale, no abre transaccion si falla la subida. **Runtime real (UAT 3-5, con los fixes)**: dos subidas simultaneas con 9 fotos -> una 200 y una 400, quedan 10 con ordenes 0..9 unicos; crear/editar/subir/reordenar/borrar foto sin 500 por LazyInitialization con `open-in-view: false` (confirmado en application.yml). El lock timeout se probo con dos hilos contra Postgres real segun 01-REVIEW-FIX.md (falla a los ~5 s). Nota: la compensacion ante fallo del commit solo tiene cobertura Mockito (no se forzo un fallo de base en runtime) |

**Score:** 11/11 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Residual de WR-12: sin ningun perfil activo `SecretosGuard` solo avisa (por diseno, para dev local); un deploy sin Dockerfile debe fijar `SPRING_PROFILES_ACTIVE` (README). Credenciales Cloudinary no abortan el arranque | Phase 2 | SC5: "El backend no arranca si falta el secret JWT o las credenciales de Cloudinary" (PROD-02) |
| 2 | `ddl-auto: update` y `show-sql: true` en todos los perfiles (IN-08) | Phase 2 | SC4: perfil de produccion sin `ddl-auto: update` ni `show-sql` + Flyway (PROD-04) |

Cerrado en esta fase: CR-01 y WR-12 (guard estricto con cualquier perfil que no sea dev/local/test o mezcla como `prod,dev`, y chequeo de 32 bytes, 18 tests en `SecretosGuardTest`), WR-13, WR-05 (CORS con trim). Salvedad ADM-01 que se mantiene: `application.yml` conserva el fallback publico del secret JWT; solo es explotable en un despliegue sin perfil ni `APP_JWT_SECRET`, riesgo de configuracion acotado a Phase 2 (PROD-02), no un fallo de SC1.

### Advisory

| # | Finding | Category | Why Advisory |
|---|---------|----------|--------------|
| 1 | Quick task 261003-6lx: human-check visual de la miniatura de portada sin ejecutar | other | Mejora fuera de los SC de la fase; build verde, `urlMiniatura` comprobada por el verificador, componente cableado con fallback; mirar en el navegador cuando convenga |
| 2 | IN-01..IN-12 abiertos (IN-09, IN-10, IN-11 destacan) | other | Informativos |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `controller/AdminPublicacionController.java` | listado admin + impacto-eliminacion | VERIFIED | Conectado a `listarParaAdmin`/`obtenerImpactoEliminacion` |
| `security/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java` | 401/403 JSON | VERIFIED | En `SecurityConfig.exceptionHandling` |
| `config/DataSeeder.java` | seed admin y agencia | VERIFIED | UAT 1 |
| `config/SecretosGuard.java` | estricto salvo dev/local/test/sin perfil, largo minimo 32 | VERIFIED | Leido; `SecretosGuardTest` verde |
| `exception/GlobalExceptionHandler.java`, `ServicioExternoException`, `ReglaDeNegocioException` | advice unico | VERIFIED | Incluye 409 por lock |
| `service/PublicacionService.java` | CRUD, fotos en tres tramos, cascada | VERIFIED | Leido completo |
| `service/CloudinaryService.java`, `ImagenValidator.java` | subida con timeouts, validacion | VERIFIED | Tests verdes; UAT 5 con Cloudinary real |
| `repository/PublicacionRepository.java` | `findByIdForUpdate`, `fijarTimeoutDeLock` | VERIFIED | Ejecutados contra Postgres real (UAT 5, probe de WR-13) |
| Front `pages/admin/AdminDashboardPage.jsx`, `AdminPublicacionFormPage.jsx` | panel real | VERIFIED | Sin mocks; `MiniaturaPublicacion` y `utils/cloudinary.js` (quick task) cableados |
| Front `services/api.js`, `context/AuthContext.jsx`, `utils/errores.js`, `components/ConfirmDialog.jsx` | sesion vencida, errores, dialogo | VERIFIED | UAT 4, 6, 7 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AdminDashboardPage` | `AdminPublicacionController` | `api.get('/admin/publicaciones')`, `impacto-eliminacion`, `api.delete` | WIRED | UAT 3/4 |
| `AdminDashboardPage` | `PublicacionController` | PATCH `/destacado`, `/estado` | WIRED | UAT 3 |
| `AdminDashboardPage` | `AgenciaController` | post/put/delete `/agencias` | WIRED | |
| `AdminDashboardPage` | `MiniaturaPublicacion` / `urlMiniatura` | `fotoDePortada(p.fotos)`; `FotoResponse` expone `url` y `orden` | WIRED | Dato real desde `PublicacionMapper` |
| `AdminPublicacionFormPage` | `PublicacionController` | POST/PUT, `/fotos`, `/fotos/orden`, `/fotos/{id}` | WIRED | UAT 5 |
| `AuthContext` | `api.js` | `registrarManejadorSesionVencida` | WIRED | UAT 6/7 |
| `PublicacionService` | `PublicacionRepository` | `fijarTimeoutDeLock` + `findByIdForUpdate` en `buscarEntidadParaEscritura` | WIRED | |
| `PublicacionService` | `TransactionTemplate` / `CloudinaryService` | subir fuera de la transaccion, eliminar en el catch | WIRED | |
| `GlobalExceptionHandler` | `PessimisticLockingFailureException` | 409 | WIRED | |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| Listado del panel | `publicaciones` | `GET /admin/publicaciones` -> `findAll(Sort)` | Si | FLOWING |
| Miniatura de portada | `p.fotos[].url/orden` | `PublicacionMapper` -> `FotoResponse` | Si | FLOWING |
| Form de edicion | `publicacion` | `GET /publicaciones/{id}` | Si | FLOWING |
| Dialogo de eliminacion | `impacto` | dos `countBy` | Si | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Suite del back (una sola corrida) | `mvn -B -o -Djava.version=17 test` | Tests run: 172, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS | PASS |
| Build del front | `npm run build` | `built in 1.93s` | PASS |
| `urlMiniatura` | `node` sobre `src/utils/cloudinary.js` | inserta `c_fill,w_160,h_120,q_auto,f_auto` en res.cloudinary.com; `res.cloudinary.com.evil.com` y wikimedia intactas; idempotente | PASS |
| Marcadores de deuda | `grep TBD/FIXME/XXX` en `src/main` y front `src` | sin resultados | PASS |
| Runtime real | 01-UAT.md 7/7 | Postgres real, Chrome, Cloudinary real | PASS |

### Probe Execution

Step 7c: SKIPPED - no hay probes declarados ni `scripts/*/tests/probe-*.sh`.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| ADM-01 | 01-02 | Unica cuenta admin por config/seed; no se registra otra desde la web | SATISFIED | Truth 1, UAT 1 |
| ADM-02 | 01-03, 01-07 | Crear, editar y eliminar publicaciones desde el panel contra la API real | SATISFIED | Truths 2, 7, 9; UAT 3, 4 |
| ADM-03 | 01-06, 01-07 | Subir, reordenar y eliminar fotos (solo imagenes validas, con limite) | SATISFIED | Truths 3, 11; UAT 5 |
| ADM-04 | 01-01, 01-05 | Cambiar estado disponible/reservado/vendido | SATISFIED | Truth 4; UAT 3 |
| ADM-05 | 01-05 | Marcar o desmarcar destacados | SATISFIED | Truth 4; UAT 3 |
| PROD-01 | 01-01, 01-04 | Token invalido/vencido da 401; errores uniformes | SATISFIED | Truths 5, 6, 8, 10; UAT 6, 7 |

Los seis IDs de ROADMAP.md y de la tabla de trazabilidad de REQUIREMENTS.md para la Fase 1 figuran en el frontmatter `requirements` de al menos un PLAN (01-01: ADM-04, PROD-01; 01-02: ADM-01; 01-03: ADM-02; 01-04: PROD-01; 01-05: ADM-04, ADM-05; 01-06: ADM-03; 01-07: ADM-02, ADM-03). Sin requisitos huerfanos; ADM-06 corresponde a Phase 5.

### Prohibitions

| Prohibition | Tier | Result |
|-------------|------|--------|
| 01-01: no mostrar sesion vencida ni redirigir ante un 401 de `/auth/*` | judgment | Resuelta por un humano: UAT 7 (login con clave incorrecta -> 401, "Email o contrasena incorrectos", sin aviso ni redireccion). El veredicto del verificador coincide (`api.js` excluye `/auth/`) |
| 01-02: no escribir `ADMIN_PASSWORD` ni su hash en logs/respuestas/archivos | test | Enforcement cableado: `DataSeederTest` (`CapturedOutput`) |
| 01-02: no promover un usuario existente a ADMIN | test | Enforcement cableado: tests de `DataSeederTest` |
| 01-05: destacado no cambia como efecto secundario | test | Enforcement cableado + UAT 3 en navegador |
| 01-07: impacto-eliminacion sin datos personales | test | Enforcement cableado; UAT 4 mostro solo cantidades |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `application.yml` | 17-20, 33 | `ddl-auto: update`, `show-sql: true`, secret JWT con fallback | Warning | Diferido a Phase 2 |
| front `utils/whatsapp.js`, `LoginPage.jsx` (Google) | - | `TODO` | Info | Previos a la fase, alcance Phase 3 |

### Human Verification Required

Ninguno pendiente: los items de la verificacion anterior estan cubiertos por 01-UAT.md (7/7 pass). Solo queda el advisory opcional de la miniatura (quick task).

### Gaps Summary

Sin gaps. Ninguna truth fallo, ningun artefacto falta ni es stub, ningun link esta roto y los fixes WR-12/WR-13 y la quick task no introdujeron regresiones (172 tests y build del front en verde; UAT corrido sobre el codigo con los fixes). Residuales diferidos a Phase 2 (PROD-02/PROD-04) y advisories listados arriba.

---

_Verified: 2026-10-03_
_Verifier: Claude (gsd-verifier)_
