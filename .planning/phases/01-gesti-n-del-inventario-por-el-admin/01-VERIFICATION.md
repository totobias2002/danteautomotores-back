---
phase: 01-gesti-n-del-inventario-por-el-admin
verified: 2026-10-03T05:30:00Z
status: human_needed
score: 8/11 must-haves verified
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
  - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
  - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
  - src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java
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
covered_digest: "v2:sha256:bbfdc29de78553eb3933a050fb5152c7e7aa0b0937b8e5c583261d13d8769a17"
behavior_unverified: 2
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 8/10
  gaps_closed: []
  gaps_remaining: []
  regressions: []
gaps: []
deferred:
  - truth: "El backend no arranca con el secret JWT por defecto fuera del perfil exacto 'prod' ni con un secret de menos de 32 bytes (WR-12, residual de CR-01: SecretosGuard es fail-open salvo perfil 'prod' y no valida el largo)"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC5: 'El backend no arranca si falta el secret JWT o las credenciales de Cloudinary' (PROD-02)"
  - truth: "Perfil de produccion sin ddl-auto: update ni show-sql (application.yml los mantiene en todos los perfiles)"
    addressed_in: "Phase 2"
    evidence: "Phase 2 SC4: 'perfil de produccion (sin ddl-auto: update ni show-sql)' y nota de Flyway (PROD-04)"
advisory:
  - finding: "WR-13: el lock pesimista y una conexion Hikari se retienen durante la subida a Cloudinary en agregarFoto, sin lock timeout ni timeouts del SDK"
    category: architectural
    reason: "Hardening introducido por el fix de WR-01; riesgo de agotar el pool si Cloudinary se cuelga. No invalida ninguna truth con un unico admin que sube en serie. Resolver con @QueryHints de lock.timeout, timeouts del SDK y mapeo de PessimisticLockingFailureException"
    evidence_status: "review 01-REVIEW.md; sin test que lo reproduzca"
behavior_unverified_items:
  - truth: "Con un token vencido o malformado, el panel lleva al admin a /login (con aviso de sesion vencida y vuelta a la pagina de origen) en vez de romperse"
    test: "Con el stack arriba, entrar como admin a /admin, poner localStorage.token = 'basura' (o dejar vencer APP_JWT_EXPIRATION_MS=60000) y recargar /admin o tocar cualquier accion del panel"
    expected: "Se limpia la sesion, se llega a /login con el cartel 'Tu sesion vencio...', y al ingresar de nuevo se vuelve a /admin. Un login con password incorrecta muestra solo 'Email o contrasena incorrectos', sin cartel ni redireccion"
    why_human: "No hay test runner en el front. api.js (interceptor 401), AuthContext (registrarManejadorSesionVencida) y LoginPage estan presentes y conectados, pero la secuencia logout() + navigate() dentro de un callback de promesa solo se comprueba en un navegador"
  - truth: "Las operaciones sobre fotos de una misma publicacion se serializan (tope de 10 fotos y orden sin repetidos bajo pedidos concurrentes), la imagen subida se borra si la transaccion no termina en commit, y las escrituras (crear, editar, subir, reordenar, borrar foto) siguen mapeando datos lazy con open-in-view apagado (WR-01, WR-02, WR-11)"
    test: "Contra Postgres real y con credenciales Cloudinary: (a) lanzar dos subidas simultaneas a un auto con 9 fotos; (b) forzar un fallo despues de subir (por ejemplo, tirar la base antes del commit); (c) crear y editar un auto, subir, reordenar y borrar una foto con el back arrancado con open-in-view false"
    expected: "(a) una sola foto se agrega, la otra recibe 400 'hasta 10 fotos' o espera el lock; ningun orden repetido. (b) el asset recien subido desaparece de Cloudinary. (c) ninguna respuesta da 500 por LazyInitializationException"
    why_human: "Los tests nuevos (PublicacionServiceTest, TransaccionesServiceTest) son Mockito y simulan TransactionSynchronization a mano; el smoke del fixer solo hizo lecturas. findByIdForUpdate (FOR UPDATE) solo se valido al arrancar, nunca se ejecuto, y las escrituras con open-in-view false no se corrieron de punta a punta. Los items 4 y 5 del UAT (parte API) son anteriores a estos fixes"
human_verification:
  - test: "Panel completo en el navegador contra la API real: crear un auto, editarlo (cambiar agencia, precio), recargar; cambiar estado a RESERVADO/VENDIDO con el select; marcar y desmarcar la estrella de destacado; filtrar por estado y buscar con acentos"
    expected: "Todo persiste al recargar; un auto reservado/vendido sigue visible en el panel con su estado; el destacado no cambia al cambiar de estado ni al editar. Un auto con transmision/combustible/condicion nulos se edita sin que se rellenen (WR-07) y un precio con centavos se redondea al cargar (WR-06/IN-09)"
    why_human: "Requiere navegador + back + base; el front no tiene tests (UAT 3, pendiente)"
  - test: "Eliminar un auto con consultas y favoritos desde el panel, en el navegador"
    expected: "El ConfirmDialog muestra solo cantidades, el boton Eliminar espera el conteo, el auto desaparece y no vuelve al recargar; con fotos reales los assets de Cloudinary desaparecen despues del commit"
    why_human: "La parte API (cascada con favorito y consulta reales, impacto 403/200, DELETE 204, sin huerfanos) se verifico en el UAT 4 contra el back real, pero antes de los fixes (eliminarFoto ahora toma el lock primero; servicios transaccionales). Falta el dialogo en navegador y el borrado de assets en Cloudinary (UAT 4, parcial)"
  - test: "Con credenciales CLOUDINARY_* reales: subir una JPG valida, una tanda mixta; llegar a 10 fotos; reordenar con flechas y 'Hacer portada'; eliminar la portada; recargar"
    expected: "JPG aparece; la tanda muestra 'nombre: motivo' por archivo malo y sube los buenos; la 11a foto no se puede subir; orden y portada persisten; al borrar la portada la siguiente pasa a orden 0 y el asset desaparece de Cloudinary"
    why_human: "La parte API de rechazos (PDF 400, .exe renombrado 400, 12/15 MB 413 en espanol, JPG valida sin credenciales 502) se verifico en el UAT 5 contra el back real; falta Cloudinary real y el flujo de la UI (UAT 5, parcial)"
  - test: "Disparar varias requests simultaneas con token vencido (el dashboard hace GET /agencias y GET /admin/publicaciones al montar)"
    expected: "Una sola limpieza de sesion y una sola redireccion a /login"
    why_human: "Truth marcada verification: backstop (no inferible): la presencia del chequeo localStorage.getItem('token') no prueba la ausencia de doble redireccion; no hay test de concurrencia. Estado insufficient_spec (UAT 6)"
  - test: "Revision humana de la prohibicion judgment-tier de 01-01: el aviso de sesion vencida NO debe aparecer ni forzar redireccion ante un 401 de /auth/*"
    expected: "Confirmar en navegador que un login fallido no muestra el cartel ni redirige. Veredicto del verificador (NO autoritativo, LLM-judge): api.js excluye error.config.url.startsWith('/auth/') y LoginPage.handleSubmit solo hace setError; flag unverified-prohibition - human review recommended"
    why_human: "Prohibicion de tier judgment: el protocolo exige resolucion humana explicita, nunca un pase silencioso (UAT 7)"
---

# Phase 1: Gestion del inventario por el admin - Verification Report

**Phase Goal:** La unica cuenta admin gestiona el inventario completo de autos (datos, fotos, estado y destacados) desde el panel, contra la API real y con errores manejados de forma uniforme.
**Verified:** 2026-10-03
**Status:** human_needed
**Re-verification:** Yes - tras 12 commits de fixes del code review (back 7265a9d..af844ea, front 8a7f3a8..15074f2) y con el UAT parcial como evidencia de runtime
**Decisiones revisadas aplicadas:** se verifico contra 01-CONTEXT.md (D-05..D-07 revisados el 2026-10-02: varias agencias gestionadas por el mismo admin), no contra el texto de agencia unica que queda en 01-RESEARCH.md / 01-PATTERNS.md.

> **Aviso sobre el modo MVP.** ROADMAP.md marca la fase con `Mode: mvp`, pero el goal no tiene formato de User Story (`gsd-tools query user-story.validate` devuelve `valid=false`). El protocolo de modo MVP exige no verificar contra un goal que no es User Story. Igual que la verificacion inicial, se aplico la metodologia goal-backward estandar contra el goal y los 5 Success Criteria de ROADMAP.md. Decision pendiente del equipo: reescribir el goal como User Story (`/gsd mvp-phase 1`) o quitar `Mode: mvp`. No afecta el resultado de esta verificacion.

## Resumen

El codigo del objetivo sigue existiendo, conectado de punta a punta (panel React, axios, controllers, services, repos) y sin stubs ni mocks en las rutas de la fase. Los 12 fixes del review no rompieron ninguna truth: la suite del back corre en verde (**150 tests, 0 fallas**, ejecutada por el verificador con `rm -rf target/classes target/test-classes; mvn -B -o -Djava.version=17 test`, BUILD SUCCESS) y el build del front pasa (`npm run build`, `built in 1.93s`; `git status` del front limpio). No hay marcadores `TBD`/`FIXME`/`XXX` en `src/main` ni en `src/` del front. No encontre ninguna truth FALLIDA ni artefacto faltante, y el re-review (0 criticos, 2 warnings WR-12 y WR-13) no invalida ninguna truth.

Lo que impide `passed`: (1) el UAT corrio contra Postgres real solo los tests 1 y 2 completos y la parte API de 4 y 5 (anterior a los fixes); (2) los fixes introdujeron tres invariantes de runtime (lock pesimista, compensacion de subida en rollback, `open-in-view: false`) que solo tienen tests Mockito; (3) la redireccion por sesion vencida del front y la truth `backstop` de 401 concurrentes siguen sin evidencia de navegador; (4) la prohibicion judgment-tier sigue flaggeada para revision humana.

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | SC1: la cuenta admin existe al arrancar (config/seed) y desde la web no se puede registrar ni obtener otra cuenta admin | VERIFIED | `DataSeeder` (`ApplicationRunner`, `@Transactional`): `existsByRol(ADMIN)` antes de crear (no sincroniza si ya hay), BCrypt via `PasswordEncoder`, `IllegalStateException` bajo perfil `prod` si faltan/invalidas las variables, nunca promueve un email existente (`existsByEmail`), loguea solo el email. `application.yml` lee `ADMIN_*` con default vacio. `AuthService.registrar` fuerza `Rol.COMPRADOR`; `AuthController` solo expone registro y login. `DataSeederTest`, `AuthServiceTest` en verde. **Runtime: UAT 1 pass contra Postgres real** (seed crea agencia y admin, login 200 ADMIN, reinicio con otra clave no la cambia, `prod` sin `ADMIN_*` aborta con exit 1). Salvedad ADM-01: ver "Aclaracion sobre CR-01" |
| 2  | SC2: el admin crea, edita y elimina una publicacion desde el panel y los cambios persisten | VERIFIED | `PublicacionController` POST/PUT/DELETE con `@Valid`, solo ADMIN en `SecurityConfig` (POST/PUT/DELETE/PATCH sobre `/api/publicaciones/**` y `/api/agencias/**`). `PublicacionService.crear/actualizar` validan agencia (404 "No existe una agencia con id: X"); `eliminar` borra favoritos, consultas y publicacion (fotos por cascade/orphanRemoval) en una `@Transactional`. `PublicacionRequest` ahora valida rango/largo/moneda (WR-03, `PublicacionRequestValidationTest`). Front: `AdminPublicacionFormPage` hace `api.post/put('/publicaciones')`; `AdminDashboardPage` hace `api.delete` tras `ConfirmDialog`; sin `window.confirm` ni `USE_MOCK_DATA` en `pages/admin`. Persistencia en navegador: Human Verification 1 |
| 3  | SC3: el admin sube, reordena y elimina fotos; archivo invalido o >10 MB se rechaza con mensaje claro | VERIFIED | `ImagenValidator` (content-type + magic bytes + 10 MB) invocado en `agregarFoto` antes de `cloudinaryService.subir`; `MAX_FOTOS=10` en el back; `orden = max+1`; `publicId` guardado; `reordenarFotos` exige el conjunto exacto de ids; `eliminarFoto` resecuencia y borra el asset en `afterCommit`; `CloudinaryService` convierte fallas del SDK en `ServicioExternoException` -> 502; 413 con mensaje en espanol; `max-file-size 10MB`, `max-swallow-size 50MB`. Front: prevalidacion, "nombre: motivo" por archivo, flechas, "Hacer portada", `ConfirmDialog`. **Runtime: UAT 5 (parte API) contra el back real**: PDF 400, .exe renombrado 400, 12/15 MB 413 en espanol, JPG valida sin credenciales 502. Falta Cloudinary real y UI: Human Verification 3 |
| 4  | SC4: el admin cambia el estado y marca/desmarca destacado desde el panel | VERIFIED | `PATCH /{id}/estado` y `PATCH /{id}/destacado` (`CambiarDestacadoRequest` `@NotNull Boolean`); solo `cambiarDestacado` toca `destacado` (tests: pasar a VENDIDO y `actualizar` no lo tocan). Front: estrella con `aria-pressed`/`aria-label` -> `api.patch('/publicaciones/${id}/destacado')` (linea 151) y select de estado en linea `api.patch(.../estado)` (linea 141) que reemplaza solo la fila; `GET /api/admin/publicaciones` devuelve todos los estados. Navegador: Human Verification 1 |
| 5  | SC5 (API): con token vencido o malformado la API responde 401 (no 500) con formato uniforme | VERIFIED | `JwtAuthenticationFilter` captura `JwtException`/`IllegalArgumentException`/`UsernameNotFoundException` y deja seguir la request sin autenticar; `RestAuthenticationEntryPoint` (401) y `RestAccessDeniedHandler` (403) responden `{"error": ...}`, registrados en `SecurityConfig.exceptionHandling`. `SeguridadErroresTest` en verde (malformado, vacio, vencido, usuario borrado). Sin regresion tras los fixes (el filtro no cambio de comportamiento; solo se agrego trim de CORS en `SecurityConfig`) |
| 6  | SC5 (panel): con un token vencido el panel lleva al admin al login en vez de romperse (aviso y vuelta al origen) | PRESENT_BEHAVIOR_UNVERIFIED | Presente y conectado: `api.js` interceptor de respuesta (401, fuera de `/auth/`, solo si hay token), `registrarManejadorSesionVencida` en `useEffect` de `AuthProvider` (logout + `navigate('/login', {state:{from, sesionVencida:true}})`), `LoginPage` muestra el cartel (`location.state?.sesionVencida`). `AuthContext` ahora tolera `localStorage.usuario` corrupto (WR-09). Transicion de estado de runtime sin test (el front no tiene runner): ver `behavior_unverified_items` |
| 7  | D-05..D-07 (revisados): el admin crea, edita y elimina agencias; cada auto pertenece a una agencia elegida en el form; no se puede eliminar una agencia con autos (400) | VERIFIED | `AgenciaService.eliminar`: 404 si no existe, `ReglaDeNegocioException` (-> 400 con mensaje claro) si `existsByAgenciaId`, sino `deleteById`; `listar` ordenado por id. `SecurityConfig`: escritura de `/api/agencias/**` solo ADMIN, GET publico. `DataSeeder` siembra "Dante Automotores" solo si `agenciaRepository.count()==0`. `AgenciaServiceTest` y `AgenciaControllerTest` en verde (se actualizaron al nuevo tipo de excepcion). Front: ABM de agencias en `AdminDashboardPage` (post/put/delete) y selector `agenciaId` en el form |
| 8  | PROD-01: todo error de la API tiene el formato uniforme `{"error": ...}` (y `campos` en validaciones); un error inesperado da 500 generico sin stacktrace | VERIFIED | Unico `@RestControllerAdvice` (`GlobalExceptionHandler extends ResponseEntityExceptionHandler`). Tras WR-04: `ReglaDeNegocioException` -> 400 con su mensaje; `grep IllegalArgumentException src/main` solo deja el catch del filtro JWT, asi que ningun `throw` de negocio quedo sin migrar; un `IllegalArgumentException` ajeno cae en `handleUnexpected` (log + 500 con mensaje fijo, sin detalle). `ServicioExternoException` -> 502; `AccessDeniedException` -> 403; `DataIntegrityViolationException` -> 409 sin mensaje SQL. `GlobalExceptionHandlerTest` en verde. Front: `utils/errores.js` muestra los mensajes por campo del backend (WR-10) y cae a `data.error` |
| 9  | Eliminar una publicacion con favoritos/consultas no falla por FK: cascada en una transaccion, aviso previo con solo cantidades, Cloudinary solo tras commit | VERIFIED (cascada con runtime previo a los fixes; afterCommit a nivel unitario) | `PublicacionService.eliminar` (`@Transactional`) y `eliminarImagenesDespuesDelCommit` (`afterCommit`; sin transaccion activa borra en el momento; filtra `publicId` nulo/blanco). `GET /api/admin/publicaciones/{id}/impacto-eliminacion` devuelve solo dos conteos (ADMIN). **Runtime: UAT 4 (parte API) contra el back real**: cascada con favorito y consulta reales, impacto 403/200, DELETE 204, sin huerfanos. Ese UAT es anterior a los fixes; el codigo de `eliminar` no cambio (ya era transaccional) y su rama sigue cubierta por tests. Navegador/Cloudinary: Human Verification 2 |
| 10 | Concurrencia 401 (edge PROD-01, `verification: backstop`): varias requests con 401 simultaneas disparan una sola limpieza de sesion y una sola redireccion | UNCERTAIN (insufficient_spec) | El guard `localStorage.getItem('token')` + `logout()` sincrono esta en `api.js`/`AuthContext.jsx` (revisado: sin cambios), pero para una truth no inferible la presencia y el wiring nunca alcanzan; no hay test de concurrencia ni observacion directa. Se abstiene -> Human Verification 4. No cuenta para el score |
| 11 | Hardening de fotos (WR-01/WR-02/WR-11): las operaciones sobre fotos de una publicacion se serializan, la imagen subida se borra si la transaccion no hace commit, y las escrituras siguen funcionando con `open-in-view: false` | PRESENT_BEHAVIOR_UNVERIFIED | Presente y conectado: `PublicacionRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, `select p from Publicacion p where p.id = :id`, sin join) usado por `buscarEntidadParaEscritura` en `agregarFoto`, `reordenarFotos` y `eliminarFoto` (lock antes de leer `fotos`); `eliminarImagenSiNoHayCommit` registra `afterCompletion` justo despues de `subir` y borra si `status != STATUS_COMMITTED`; `@Transactional` a nivel de clase en `PublicacionService`, `FavoritoService`, `ConsultaService`, `open-in-view: false`; `TransaccionesServiceTest` guarda la configuracion. Los mappers que leen asociaciones lazy (`PublicacionMapper`, `FavoritoMapper`) corren dentro de transaccion; `AgenciaMapper` y `ConsultaMapper` no tocan estado lazy. Es una invariante de concurrencia/cancelacion/limpieza que ningun test ejercita con una transaccion real (Mockito + sincronizacion simulada); el smoke del fixer solo hizo lecturas. Ver `behavior_unverified_items` |

**Score:** 8/11 truths verified (2 present, behavior-unverified; 1 insufficient_spec)

Efecto de los fixes sobre el runtime observado en el UAT: `eliminarFoto` ahora toma el lock de la publicacion antes de buscar la foto, de modo que una foto ajena con id de publicacion inexistente devuelve 404 en vez de 400 (los tests se actualizaron); el borrado de publicacion y las respuestas de rechazo de fotos del UAT 5 no dependen de esa rama. Los mensajes 400 existentes conservan su texto (WR-04). No se detecto ninguna regresion en las rutas que el UAT cubrio.

### Deferred Items

Items senalados por las revisiones que una fase posterior del milestone cubre de forma explicita. No son gaps accionables de esta fase.

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | WR-12: `SecretosGuard` solo aborta bajo el perfil exacto `prod` (fail-open con `production`, `railway`, despliegues sin el Dockerfile) y no valida el largo minimo de 32 bytes del secret | Phase 2 | SC5: "El backend no arranca si falta el secret JWT o las credenciales de Cloudinary" (PROD-02) |
| 2 | `ddl-auto: update` y `show-sql: true` en todos los perfiles (IN-08) | Phase 2 | SC4: perfil de produccion sin `ddl-auto: update` ni `show-sql` + migraciones Flyway (PROD-04) |

Cerrado en esta fase y por lo tanto ya no diferido: CR-01 en su camino Docker (`SecretosGuard` + `ENV SPRING_PROFILES_ACTIVE=prod`) y WR-05 (CORS con trim; `CorsOrigenesTest`). La mitad "CORS acepta espacios" de Phase 2 SC5 ya esta hecha.

**Aclaracion sobre CR-01/WR-12 y ADM-01:** el fallback publico del secret JWT es anterior a la fase. Con el fix, la imagen Docker (perfil `prod`) no arranca sin `APP_JWT_SECRET` propio ni con la clave de DB de desarrollo. Pero el guard es fail-open fuera del perfil `prod`, asi que la garantia "no se puede obtener otra cuenta admin" de SC1 vale solo si el despliegue usa `prod` o setea `APP_JWT_SECRET`: quien conozca el email del admin y el secret publico podria firmar un JWT valido. Es un riesgo de configuracion de despliegue, diferido a PROD-02, no un fallo de SC1 en el codigo de la fase.

### Advisory

| # | Finding | Category | Why Advisory |
|---|---------|----------|--------------|
| 1 | WR-13: lock + conexion Hikari retenidos durante la subida a Cloudinary (sin lock timeout ni timeouts del SDK) | architectural | Hardening del fix de WR-01; sin test que lo reproduzca; con un unico admin que sube en serie el riesgo es bajo. Decidir antes del despliegue |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `controller/AdminPublicacionController.java` | GET listado admin + impacto-eliminacion | VERIFIED | Sustantivo, conectado a `listarParaAdmin`/`obtenerImpactoEliminacion`; `/api/admin/**` exige ADMIN |
| `security/RestAuthenticationEntryPoint.java`, `RestAccessDeniedHandler.java` | 401/403 JSON | VERIFIED | Registrados en `SecurityConfig.exceptionHandling` |
| `config/DataSeeder.java` | Seed de agencia y admin | VERIFIED | `ApplicationRunner`; `UsuarioRepository.existsByRol` |
| `config/SecretosGuard.java` | Aborta con secretos por defecto bajo `prod` | VERIFIED (con WR-12) | `InitializingBean`; no loguea valores; fail-open fuera de `prod` y sin chequeo de largo (diferido) |
| `exception/GlobalExceptionHandler.java`, `ServicioExternoException.java`, `ReglaDeNegocioException.java` | Advice unico, 502, 400 de negocio | VERIFIED | Ver truth 8 |
| `service/ImagenValidator.java` | Tipo real, tamano, `MAX_FOTOS = 10` | VERIFIED | Lanza `ReglaDeNegocioException` |
| `service/CloudinaryService.java` | `ImagenSubida(url, publicId)` + `eliminar` best-effort | VERIFIED | Sin cambios funcionales |
| `repository/PublicacionRepository.java` | `findByIdForUpdate` | VERIFIED (ejecucion real sin probar) | Ver truth 11 |
| `dto/publicacion/{CambiarDestacadoRequest,ReordenarFotosRequest,ImpactoEliminacionResponse,AnioDeModelo,PublicacionRequest}.java` | DTOs validados | VERIFIED | Limites alineados con las columnas |
| `entity/Publicacion.java` / `FotoPublicacion.java` | `destacado` con default, `publicId` | VERIFIED | `@ColumnDefault("false")`; UAT 2 confirmo que `ddl-auto` agrega las columnas sobre una base poblada (segun el orquestador; ver nota sobre 01-UAT.md) |
| Front `utils/errores.js`, `services/api.js`, `components/ConfirmDialog.jsx`, `context/AuthContext.jsx` | Mensaje del backend, interceptor 401, dialogo, sesion | VERIFIED | Existen y estan importados |
| Front `pages/admin/AdminDashboardPage.jsx`, `AdminPublicacionFormPage.jsx` | Panel real | VERIFIED | Sin mocks ni stubs; llamadas reales a todos los endpoints de la fase |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AdminDashboardPage.jsx` | `AdminPublicacionController` | `api.get('/admin/publicaciones')` (l.69) | WIRED | La respuesta alimenta la lista |
| `AdminDashboardPage.jsx` | `PublicacionController` | `api.patch('/publicaciones/${id}/destacado')` (l.151) y `/estado` (l.141) | WIRED | El resultado reemplaza la fila |
| `AdminDashboardPage.jsx` | `AdminPublicacionController` | `impacto-eliminacion` (l.163) antes del dialogo, luego `api.delete` (l.186) | WIRED | Confirmar espera el conteo |
| `AdminDashboardPage.jsx` | `AgenciaController` | `api.post/put/delete('/agencias...')` | WIRED | Error del backend visible |
| `AdminPublicacionFormPage.jsx` | `PublicacionController` | POST/PUT `/publicaciones`, `/fotos`, `/fotos/orden`, `/fotos/{id}` | WIRED | Todas con `mensajeDeError` |
| `SecurityConfig` | `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` | `exceptionHandling(...)` | WIRED | |
| `AuthContext.jsx` | `api.js` | `registrarManejadorSesionVencida` en `useEffect` | WIRED | Se desregistra al desmontar |
| `PublicacionService` | `PublicacionRepository.findByIdForUpdate` | `buscarEntidadParaEscritura` en agregar/reordenar/eliminar foto | WIRED | |
| `PublicacionService` | `ImagenValidator` / `CloudinaryService` | `validar` antes de `subir`; `afterCommit` y `afterCompletion` | WIRED | |
| `AgenciaService` | `PublicacionRepository` | `existsByAgenciaId` antes de `deleteById` | WIRED | |
| `DataSeeder` / `SecretosGuard` | `application.yml` / `Environment` | `@Value` y `acceptsProfiles("prod")` | WIRED | |
| `PublicacionMapper` | `PublicacionResponse` | `destacado` | WIRED | |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AdminDashboardPage` listado | `publicaciones` | `GET /admin/publicaciones` -> `findAll(Sort DESC fechaPublicacion)` dentro de `@Transactional(readOnly)` | Si | FLOWING |
| `AdminDashboardPage` agencias | `agencias` | `GET /agencias` -> `findAll(Sort id)` | Si | FLOWING |
| `AdminPublicacionFormPage` edicion | `publicacion`, `form` | `GET /publicaciones/{id}` | Si | FLOWING |
| Dialogo de eliminacion | `impacto` | `GET .../impacto-eliminacion` -> `countBy...` x2 | Si | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Suite del back (una sola corrida completa) | `rm -rf target/classes target/test-classes; mvn -B -o -Djava.version=17 test` | `Tests run: 150, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS (`PublicacionServiceTest` 36, `TransaccionesServiceTest` 3, `ImagenValidatorTest` 15) | PASS |
| Build del front | `npm --prefix danteautomotores-front run build` | build ok, `built in 1.93s` | PASS |
| Arbol limpio | `git status --short` | Front limpio; back solo `.gsd/`, `.planning/milestone.lock`, `.planning/state.json` sin trackear | PASS |
| Marcadores de deuda | `grep TBD|FIXME|XXX` en `src/main` y front `src` | Sin resultados | PASS |
| Runtime contra Postgres real | UAT 1 y 2 (completos) y parte API de 4 y 5 | Pass segun 01-UAT.md y el orquestador; anterior a los fixes | PASS (previo a los fixes) |

### Probe Execution

Step 7c: SKIPPED - no hay `scripts/*/tests/probe-*.sh` ni probes declarados en los PLAN/SUMMARY de la fase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| ADM-01 | 01-02 | Una unica cuenta admin por config/seed; no se puede registrar otra desde la web | SATISFIED | Truth 1 (UAT 1 pass contra Postgres real). Salvedad de despliegue: WR-12 |
| ADM-02 | 01-03, 01-07 | Crear, editar y eliminar publicaciones desde el panel contra la API real | SATISFIED (persistencia en navegador pendiente) | Truths 2, 7, 9 |
| ADM-03 | 01-06, 01-07 | Subir, reordenar y eliminar fotos (solo imagenes validas, con limite) | SATISFIED (Cloudinary real pendiente) | Truths 3, 11 |
| ADM-04 | 01-01, 01-05 | Cambiar estado disponible/reservado/vendido | SATISFIED | Truth 4 |
| ADM-05 | 01-05 | Marcar o desmarcar destacados | SATISFIED | Truth 4 |
| PROD-01 | 01-01, 01-04 | Token invalido o vencido da 401; errores con formato uniforme | SATISFIED en la API; el redirect del panel esta en human verification | Truths 5, 6, 8, 10 |

Los seis IDs de ROADMAP.md y de la tabla de trazabilidad de REQUIREMENTS.md para la Fase 1 (ADM-01..ADM-05, PROD-01) aparecen en el frontmatter `requirements` de al menos un PLAN (01-01: ADM-04, PROD-01; 01-02: ADM-01; 01-03: ADM-02; 01-04: PROD-01; 01-05: ADM-04, ADM-05; 01-06: ADM-03; 01-07: ADM-02, ADM-03). **Sin requisitos huerfanos**: REQUIREMENTS.md no mapea ningun otro ID a la Fase 1 y los seis figuran como `[x]` / Complete.

### Prohibitions

| Prohibition | Tier | Result |
|-------------|------|--------|
| 01-01: no mostrar sesion vencida ni redirigir ante un 401 de `/auth/*` | judgment | Veredicto NO autoritativo del verificador: cumple (`api.js` linea 26-31 excluye `/auth/`; `LoginPage` solo hace `setError`). **unverified-prohibition - human review recommended** (Human Verification 5) |
| 01-02: no escribir `ADMIN_PASSWORD` ni su hash en logs/respuestas/archivos | test | Con enforcement cableado: `DataSeederTest` (`CapturedOutput`); `DataSeeder` solo loguea el email; `SecretosGuard` tampoco loguea valores de secretos |
| 01-02: no promover un usuario existente a ADMIN | test | Con enforcement cableado: `emailYaRegistradoConPerfilProd_lanzaYNuncaPromueve` y `...SinPerfilProd_noGuardaNiPromueve` |
| 01-05: destacado no cambia como efecto secundario | test | Con enforcement cableado: `pasarUnAutoDestacadoAVendidoNoLoDesmarca`, `actualizarUnaPublicacionDestacadaNoCambiaElDestacado` (suite en verde tras los fixes) |
| 01-07: impacto-eliminacion sin datos personales | test | Con enforcement cableado: `elImpactoDeEliminacionSoloTieneDosConteosSinDatosPersonales`; el DTO tiene dos `long` |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (todo `src/` de ambos repos) | - | `TBD`/`FIXME`/`XXX` | ninguno | Sin coincidencias |
| front `utils/whatsapp.js`, `LoginPage.jsx` (Google) | - | `TODO` | Info | Anteriores a la fase y fuera de su alcance (Fase 3) |
| `application.yml` | 17-20 | `ddl-auto: update`, `show-sql: true`, secret JWT con fallback | Warning | Diferido a Fase 2 (WR-12, IN-08) |
| `PublicacionService.agregarFoto` | 180-211 | Subida a Cloudinary dentro de la transaccion con lock | Warning (advisory WR-13) | Posible agotamiento del pool si Cloudinary se cuelga |

### Estado del re-review (01-REVIEW.md, advisory)

0 criticos, 2 warnings y 12 info. Comprobado contra el codigo actual, no contra el reporte de fixes: los 12 hallazgos corregidos (CR-01, WR-01..WR-11) estan efectivamente en `src/` (`SecretosGuard`, `findByIdForUpdate`, `eliminarImagenSiNoHayCommit`, `ReglaDeNegocioException`, trim de CORS, `@Transactional` de clase, `open-in-view: false`, `errores.js`, `AuthContext`). Pendientes sin impacto en las truths: WR-12 (diferido a Fase 2), WR-13 (advisory arriba), IN-01..IN-12 (calidad; destacan IN-09, redondeo silencioso del precio al editar, y IN-11, borrado de la imagen ante `STATUS_UNKNOWN`). Inconsistencia documental menor: `01-UAT.md` deja `result: [pending]` en el test 2 aunque su resumen cuenta 2 aprobados y el commit d579206 lo registra como pass; conviene actualizar esa linea.

### Human Verification Required

Ver el frontmatter `human_verification` y `behavior_unverified_items` (6 items). En resumen, quedan:

1. **Panel completo en el navegador** (UAT 3): crear/editar/estado/destacado/filtro con recarga.
2. **Eliminar un auto con consultas y favoritos en el navegador** (UAT 4, parte API ya verificada antes de los fixes) y borrado de assets en Cloudinary.
3. **Cloudinary real y flujo de fotos de la UI** (UAT 5, parte API ya verificada antes de los fixes).
4. **401 concurrentes** con token vencido: una sola redireccion (UAT 6, truth `backstop`).
5. **Redireccion por sesion vencida** y resolucion humana de la prohibicion judgment-tier sobre `/auth/*` (UAT 7).
6. **Re-correr contra Postgres real las escrituras posteriores a los fixes** (lock pesimista, compensacion de subida, `open-in-view: false`): crear/editar, subir/reordenar/borrar foto y dos subidas simultaneas.

La lista de variables a configurar esta en `01-USER-SETUP.md` (`ADMIN_*` y `CLOUDINARY_*`).

### Gaps Summary

No hay gaps bloqueantes: ninguna truth fallo, ningun artefacto falta ni es stub, ningun link esta roto y los 12 fixes no introdujeron regresiones detectables (150 tests y build del front en verde). El estado sigue siendo `human_needed` por evidencia de runtime que ningun test automatizado puede dar (navegador y Cloudinary real), por la truth `backstop` de concurrencia de 401, por la prohibicion judgment-tier pendiente de resolucion humana y por las invariantes de runtime nuevas de los fixes (truth 11). WR-12 queda diferido a la Fase 2 (PROD-02) y WR-13 como advisory.

---

_Verified: 2026-10-03_
_Verifier: Claude (gsd-verifier)_
