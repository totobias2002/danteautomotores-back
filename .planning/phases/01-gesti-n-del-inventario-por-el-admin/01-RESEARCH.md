# Phase 1: Gestión del inventario por el admin - Research

**Researched:** 2026-10-02
**Domain:** Spring Boot 3.3.5 REST (Spring Security 6 + JWT, JPA/Hibernate 6.5, Cloudinary) + React 18 / axios admin panel. Fase brownfield que toca los DOS repos.
**Confidence:** HIGH (el comportamiento actual y el diseño de errores/seguridad se verificaron ejecutando sondas contra el código real; Cloudinary y Tomcat se verificaron contra docs oficiales / búsqueda)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** El admin se crea al arrancar la app a partir de variables de entorno (`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOMBRE`; los nombres exactos los define el planner). Se crea **solo si no existe ningún usuario con rol ADMIN**. La contraseña nunca queda en el repo (sin defaults con credenciales reales en `application.yml`).
- **D-02:** Si el admin ya existe, el seed **no lo modifica**: no sincroniza la contraseña ni otros datos aunque cambien las variables. Cambiar la contraseña queda para otro mecanismo (la recuperación de la Fase 3).
- **D-03:** Si faltan las variables del admin y no hay ningún ADMIN en la base, el planner decide entre avisar en el log y fallar al arrancar. Se prefiere fallar en prod y avisar en dev.
- **D-04:** No existe ningún endpoint que cree o promueva admins. `AuthService.registrar` sigue forzando `Rol.COMPRADOR`. Los usuarios ADMIN que ya estén en la base de dev se dejan como están, sin chequeo de unicidad al arrancar. El modelo `Rol` sigue admitiendo varios admins a futuro (ADM-V2-01).
- **D-05:** Hay una **sola agencia fija (Dante Automotores)**, sembrada al arrancar si no existe ninguna. La tabla y la entidad `Agencia` se mantienen, así que se puede volver a varias más adelante. — **Reversibility:** reversible — la FK `publicaciones.agencia_id` se conserva.
- **D-06:** Los autos se asocian automáticamente a esa agencia: el backend la asigna en `crear`/`actualizar` y deja de exigir `agenciaId` en `PublicacionRequest`. El form de publicación del front deja de mostrar el selector de agencia.
- **D-07:** En el panel, el admin **edita** los datos de la agencia (nombre, contacto, dirección, logo, descripción) pero **no crea ni borra** agencias. Se quitan del panel las acciones "Nueva agencia" y "Eliminar", y el backend deja de exponer la creación y la eliminación (las quita o las bloquea). Los GET públicos de agencia siguen como están, porque la página "Agencia" los usa en la Fase 2.

### Claude's Discretion
El usuario eligió no discutir estas áreas. El planner define el detalle con estos defaults razonables:

- **Fotos (ADM-03):** validar en el **backend** el tipo real (JPEG/PNG/WebP, revisando content-type y, si se puede, magic bytes) y el tamaño (≤10MB, como el multipart actual). El tope de 10 fotos por auto se valida también en el backend, no solo en el front (`MAX_FOTOS = 10`). Los rechazos llevan un mensaje claro en español. El reorden se guarda en el backend con un endpoint que recibe el orden completo de ids; la UI puede usar drag & drop o flechas, lo que sea más simple y usable en mobile. La foto con orden 0 es la portada. Al eliminar una foto (o una publicación), se intenta borrar el asset en Cloudinary (guardando el `public_id`); si falla, no se bloquea la operación.
- **Destacados (ADM-05):** campo booleano `destacado` en `Publicacion`, con un endpoint dedicado tipo PATCH (como el de estado) y un toggle en el listado del panel. Sin tope obligatorio de destacados. Que un auto vendido pueda seguir destacado o no queda a criterio del planner (el catálogo público de la Fase 2 decide cómo mostrarlo).
- **Listado del panel (ADM-04):** el panel muestra **todos** los estados (hoy solo trae `DISPONIBLE`), con filtro por estado y búsqueda simple. El cambio de estado sigue en línea con el select actual. Eliminar una publicación pide confirmación dentro de la UI (no `window.confirm` si se puede evitar).
- **Errores (PROD-01):** formato uniforme `{"error": "mensaje"}`. Para validaciones se puede sumar un mapa `campos`, pero el planner decide si conserva compatibilidad con el formato actual por campo. El filtro JWT captura los tokens malformados o vencidos y deja que la request siga sin autenticar o responde 401, nunca 500. Se agregan un `AuthenticationEntryPoint` y un `AccessDeniedHandler` con el mismo formato (401/403), y un handler genérico para 500 sin stacktrace. En el front, un interceptor de respuesta de axios ante un 401 limpia la sesión y redirige a `/login`, idealmente con un aviso de "sesión vencida" y volviendo después a la página donde estaba. Los forms del panel muestran el mensaje `error` del backend en lugar de textos genéricos.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope. (Fuera de fase por el boundary: catálogo público sin mocks, paginación pública, deploy a producción y Flyway = Fase 2; recuperación de contraseña = Fase 3; métricas del admin = Fase 5.)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ADM-01 | Cuenta admin única, creada por config/seed; no se puede registrar otra desde la web | `DataSeeder` (ApplicationRunner) + env vars; `AuthService.registrar` ya fuerza COMPRADOR y `RegistroRequest` no tiene campo `rol` (verificado); test de "registro con `rol":"ADMIN"` sigue dando COMPRADOR |
| ADM-02 | CRUD de publicaciones desde el panel contra la API real | Agencia única asignada por backend (D-06); `eliminar` hoy falla con FK si hay favoritos/consultas (pitfall 5); errores uniformes visibles en los forms |
| ADM-03 | Subir, reordenar y eliminar fotos; solo imágenes válidas con límite de tamaño | `ImagenValidator` (content-type + magic bytes + 10 MB + tope 10), `public_id` guardado, endpoint `PUT /{id}/fotos/orden`, resecuenciado de `orden` al borrar (pitfall 3), `max-swallow-size` (pitfall 6) |
| ADM-04 | Cambiar estado de un auto | Endpoint `PATCH /{id}/estado` ya existe; falta que el listado admin traiga todos los estados → endpoint `GET /api/admin/publicaciones` |
| ADM-05 | Marcar/desmarcar destacados | Campo `destacado` con `@ColumnDefault("false")` (pitfall 4, DDL verificado) + `PATCH /{id}/destacado` |
| PROD-01 | Token inválido/vencido → 401; errores con formato uniforme | Filtro JWT con try/catch + `AuthenticationEntryPoint`/`AccessDeniedHandler` JSON + `@RestControllerAdvice` que extiende `ResponseEntityExceptionHandler` (todo verificado con sonda); interceptor axios 401 |
</phase_requirements>

## Summary

La fase es mayormente "completar y endurecer" código existente, no construir desde cero. Los hallazgos que más condicionan el plan se **verificaron ejecutando sondas** (tests `@WebMvcTest` descartables, ya eliminados del repo) contra el código real: (1) hoy un token malformado, vencido o vacío NO devuelve 401: la excepción del filtro (`MalformedJwtException`, `ExpiredJwtException`, `IllegalArgumentException`) se propaga y termina en 500; (2) un request sin token a un endpoint protegido devuelve **403 con cuerpo vacío**, no 401, porque no hay `AuthenticationEntryPoint` configurado, así que el `@RestControllerAdvice` por sí solo no alcanza para PROD-01; (3) extender `ResponseEntityExceptionHandler` da formato uniforme a 400/404/405/413 y a los errores de framework (JSON mal formado, enum inválido, type mismatch, parte multipart faltante), pero **declarar un `@ExceptionHandler(MaxUploadSizeExceededException)` propio rompe el arranque** (mapeo ambiguo) y un `@ExceptionHandler(Exception)` genérico debe convivir con handlers explícitos de `AccessDeniedException`.

Hay tres bugs latentes que el plan debe cubrir aunque no estén en el CONTEXT: borrar una publicación con favoritos o consultas viola una FK (hoy → 500); `agregarFoto` usa `fotos.size()` como `orden`, lo que genera órdenes duplicados después de borrar una foto del medio; y un campo `boolean destacado` primitivo con `ddl-auto: update` genera `boolean not null` sin default, lo que **falla al agregar la columna a una tabla con filas** (se verificó el DDL generado; con `@ColumnDefault("false")` genera `boolean default false not null`).

En el front, el panel ya usa la API real. Hay que: sacar el selector de agencia y el CRUD de agencias, traer todos los estados desde un endpoint admin nuevo, sumar toggle de destacado, reordenar fotos (flechas, sin dependencias), diálogo de confirmación in-UI (`<dialog>` nativo), interceptor 401 con "sesión vencida" + vuelta a la página de origen, y mostrar `error` del backend. **No se requieren paquetes nuevos** en ninguno de los dos repos.

**Primary recommendation:** Implementar en este orden: (1) manejo global de errores + filtro JWT + entry point (PROD-01, desbloquea el resto y se prueba sin DB), (2) seed admin + agencia única, (3) modelo de datos (`destacado`, `public_id`) + endpoints de fotos/destacado/listado admin, (4) front. Cubrir cada requisito con tests Mockito/`@WebMvcTest` (no requieren DB ni Docker) y usar `mvn -Djava.version=17` mientras no haya JDK 21 instalado.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Seed de admin y agencia única | API / Backend (arranque) | Database | Se ejecuta en `ApplicationRunner`; la unicidad real la garantiza el backend (sin endpoints de alta de admin) |
| Bloqueo de alta de admins desde la web | API / Backend | — | `AuthService.registrar` fuerza COMPRADOR; el front no decide roles |
| Autorización de endpoints admin | API / Backend (SecurityConfig) | Browser (ProtectedRoute, solo UX) | La barrera real es el backend; el front solo oculta rutas |
| Validación de imágenes (tipo/tamaño/tope) | API / Backend | Browser (pre-validación UX) | El front se puede saltear; el backend es la fuente de verdad. El front pre-valida para dar feedback inmediato y evitar el corte de conexión de Tomcat |
| Almacenamiento/borrado de imágenes | Servicio externo (Cloudinary) vía Backend | Database (guarda url + public_id) | El backend no guarda archivos; la DB guarda la referencia |
| Orden de fotos / portada | API / Backend + Database (`orden`) | Browser (UI de flechas) | El orden persiste; la UI solo manda la lista completa de ids |
| Destacado / estado | API / Backend + Database | Browser (toggle/select) | Flags persistidos; la Home pública (Fase 2) los consume |
| Formato uniforme de errores | API / Backend (advice + entry point + handlers) | Browser (interceptor + helper de mensaje) | Los errores del filtro no pasan por el advice: requieren handlers de Security |
| Sesión vencida → login → volver | Browser (interceptor axios + AuthContext + router) | API (devuelve 401) | La redirección es de cliente; el backend solo emite 401 uniforme |
| Listado admin con todos los estados | API / Backend (endpoint admin) | Browser (filtro/búsqueda client-side) | El listado público (`GET /api/publicaciones`) sigue filtrando DISPONIBLE hasta la Fase 2 |

## Standard Stack

### Core (todo ya presente; no se instala nada)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.3.5 | Web, Security, Data JPA, Validation | `[VERIFIED: pom.xml]` parent `spring-boot-starter-parent` 3.3.5 |
| Spring Security | 6.3.x (gestionado por Boot) | Filter chain, `AuthenticationEntryPoint`, `AccessDeniedHandler` | Mecanismo estándar para 401/403 JSON; las sondas lo confirmaron |
| Hibernate ORM | 6.5.3.Final (gestionado por Boot) | JPA, `ddl-auto: update` en esta fase | `[VERIFIED: ~/.m2/repository/org/hibernate/orm/hibernate-core/6.5.3.Final]` |
| JJWT | 0.12.6 | Parseo/validación de JWT | `[VERIFIED: pom.xml]` `<jjwt.version>0.12.6</jjwt.version>` |
| cloudinary-http44 | 1.39.0 | Upload y destroy de imágenes | `[VERIFIED: pom.xml]` y `javap` del jar: `Uploader.upload(Object, Map)` y `Uploader.destroy(String, Map)` existen |
| spring-boot-starter-test + spring-security-test | (Boot) | JUnit 5, Mockito, MockMvc, `@WithMockUser` | `[VERIFIED: pom.xml]` ya declarados con scope test |
| axios | ^1.7.7 | Cliente HTTP del front (interceptor de response) | `[VERIFIED: danteautomotores-front/package.json]` |
| React Router | ^6.27.0 | `useNavigate`, `useLocation`, `state` | `[VERIFIED: danteautomotores-front/package.json]` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Lombok | (Boot) | `@Slf4j` para el log que hoy no existe | Seeder y borrado best-effort en Cloudinary |
| `<dialog>` nativo del navegador | — | Confirmación in-UI sin dependencias | Eliminar publicación / foto |
| lucide-react | ^1.47.0 | Íconos (Star, ChevronLeft/Right, Trash2) | Ya instalado |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flechas para reordenar | Drag & drop (`@dnd-kit/*`) | Agrega dependencia y es peor en mobile; CONTEXT permite lo más simple. **Usar flechas + "Hacer portada"** |
| H2 / Testcontainers para tests de DB | — | No hacen falta: todas las reglas de la fase se prueban con Mockito + `@WebMvcTest`. H2 sumaría una dependencia con divergencias de dialecto; Testcontainers necesita el daemon de Docker (hoy apagado). **No agregar** |
| `@TransactionalEventListener` para el borrado en Cloudinary | `TransactionSynchronizationManager.registerSynchronization` | Equivalentes; el evento es más testeable. Cualquiera sirve |
| Endpoint admin nuevo `GET /api/admin/publicaciones` | `?estado=TODOS` sobre el GET público | El GET público es `permitAll`; mezclar lo admin ahí lo expone. Se prefiere endpoint separado |

**Installation:** ninguna. `# no new packages — backend ni frontend`

**Version verification:** `[VERIFIED: pom.xml / package.json]` las versiones salen de los archivos de proyecto leídos esta sesión; no se propone ningún paquete nuevo, por lo que no hay nada que contrastar contra el registro.

## Package Legitimacy Audit

No se instalan paquetes externos en esta fase (ni Maven ni npm). La auditoría de legitimidad no aplica.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | — (ninguno) |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

*Si el planner decide agregar H2, Testcontainers, vitest o una librería de drag & drop, cada una debe pasar por `package-legitimacy check` y quedar detrás de un `checkpoint:human-verify`. Este research recomienda no hacerlo.*

## Architecture Patterns

### System Architecture Diagram

```
                         ARRANQUE
 env ADMIN_EMAIL/PASSWORD/NOMBRE ──► DataSeeder (ApplicationRunner)
                                       ├─ ¿existe agencia? no ─► crea "Dante Automotores"
                                       └─ ¿existe ADMIN?  no ─► vars completas? ─ sí ─► crea ADMIN (BCrypt)
                                                                              └ no ─► prod: FALLA arranque / dev: WARN
                         REQUEST (admin panel, axios + Bearer)
 Browser ──► CorsFilter ──► JwtAuthenticationFilter ──try/catch──► (token malo: contexto vacío, sigue)
                               │                                       │
                               ▼                                       ▼
                    AuthorizationFilter (SecurityConfig)       ExceptionTranslationFilter
                      /api/admin/** , PUT/PATCH/DELETE/POST      ├─ sin auth  ─► RestAuthenticationEntryPoint ─► 401 {"error"}
                      /api/publicaciones/** = ADMIN              └─ rol malo  ─► RestAccessDeniedHandler    ─► 403 {"error"}
                               │ ok
                               ▼
        Controller (@Valid, MultipartFile) ──► Service (@Transactional)
              │                                  ├─ ImagenValidator (tipo + magic bytes + 10MB + tope 10)
              │                                  ├─ AgenciaRepository.findFirstByOrderByIdAsc() (agencia única)
              │                                  ├─ CloudinaryService.subir (guarda secure_url + public_id)
              │                                  ├─ PublicacionRepository / FotoPublicacionRepository
              │                                  └─ AFTER_COMMIT ─► CloudinaryService.eliminar(public_id) (best-effort)
              ▼
   GlobalExceptionHandler extends ResponseEntityExceptionHandler  ──► {"error": "...", "campos"?: {...}}
        404/400/405/413/415 ; AccessDenied→403 ; Exception→500 genérico (sin stacktrace)

 Browser: axios response interceptor
        401 (con token, fuera de /auth/*) ─► AuthContext.logout() + navigate('/login', {state:{from, sesionVencida}})
        LoginPage ─► aviso "sesión vencida" ─► navigate(state.from ?? '/')
```

### Recommended Project Structure
```
danteautomotores-back/src/main/java/com/danteautomotores/
├── config/
│   ├── DataSeeder.java                 # NUEVO: ApplicationRunner (agencia + admin)
│   └── SecurityConfig.java             # exceptionHandling(entryPoint, accessDenied) + /api/admin/**
├── security/
│   ├── JwtAuthenticationFilter.java    # try/catch alrededor de la autenticación
│   ├── RestAuthenticationEntryPoint.java   # NUEVO: 401 JSON
│   └── RestAccessDeniedHandler.java        # NUEVO: 403 JSON
├── exception/
│   └── GlobalExceptionHandler.java     # extiende ResponseEntityExceptionHandler
├── controller/
│   ├── AdminPublicacionController.java # NUEVO: GET /api/admin/publicaciones
│   ├── PublicacionController.java      # + PATCH /{id}/destacado, PUT /{id}/fotos/orden
│   └── AgenciaController.java          # se quitan POST y DELETE (D-07)
├── service/
│   ├── PublicacionService.java         # agencia única, tope 10, reorden, destacado, borrado completo
│   ├── CloudinaryService.java          # devuelve (url, publicId) + eliminar best-effort
│   └── ImagenValidator.java            # NUEVO (puede ser @Component o util estático)
├── dto/publicacion/
│   ├── CambiarDestacadoRequest.java    # NUEVO
│   ├── ReordenarFotosRequest.java      # NUEVO: List<Long> fotoIds
│   └── PublicacionRequest.java         # se quita agenciaId
└── entity/  Publicacion (+destacado), FotoPublicacion (+publicId)

danteautomotores-front/src/
├── services/api.js                     # + interceptor response, no Bearer en /auth/*
├── utils/errores.js                    # NUEVO: mensajeDeError(err, fallback)
├── components/ConfirmDialog.jsx        # NUEVO: <dialog> nativo
├── context/AuthContext.jsx             # registra el manejador de sesión vencida
├── components/ProtectedRoute.jsx       # <Navigate state={{from}}>
├── pages/LoginPage.jsx                 # navigate(state?.from ?? '/') + aviso
└── pages/admin/  AdminDashboardPage.jsx, AdminPublicacionFormPage.jsx
```

### Pattern 1: Filtro JWT que nunca lanza (continúa sin autenticar)
**What:** El `try/catch` va SOLO alrededor de la autenticación, no de `filterChain.doFilter`. Un token malo deja el contexto vacío; los endpoints públicos siguen funcionando y los protegidos responden 401 vía `AuthenticationEntryPoint`.
**When to use:** Siempre; es el origen del 500 (PROD-01).
**Example:**
```java
// Verificado con sonda @WebMvcTest (public GET + token basura/vencido/vacío -> 200; POST protegido -> 401 JSON)
try {
    final String jwt = authHeader.substring(7);
    final String userEmail = jwtService.extractUsername(jwt);
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        if (jwtService.isTokenValid(jwt, userDetails)) {
            // ... setAuthentication igual que hoy
        }
    }
} catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
    SecurityContextHolder.clearContext(); // token vencido/malformado/vacío o usuario borrado: seguir sin autenticar
}
filterChain.doFilter(request, response);
```
Excepciones observadas (`[VERIFIED: sonda]`): `"Bearer garbage"` → `io.jsonwebtoken.MalformedJwtException`; `"Bearer "` → `java.lang.IllegalArgumentException: CharSequence cannot be null or empty.`; vencido → `io.jsonwebtoken.ExpiredJwtException`. `UsernameNotFoundException` sale de `CustomUserDetailsService` si el usuario del token fue borrado.

### Pattern 2: EntryPoint y AccessDeniedHandler con JSON `{"error"}`
```java
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(res.getWriter(), Map.of("error", "Tu sesión venció o no es válida. Iniciá sesión de nuevo."));
    }
}
// RestAccessDeniedHandler: idéntico con 403 y "No tenés permiso para realizar esta acción."
// SecurityConfig: .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
```
`[VERIFIED: sonda]` sin token / token vencido / usuario borrado → 401 `application/json;charset=UTF-8`; COMPRADOR con token válido → 403 JSON. **Sin** estos handlers, hoy el mismo caso devuelve **403 con cuerpo vacío** `[VERIFIED: sonda sobre SecurityConfig real: "POST protected no token -> 403 body="]`.

### Pattern 3: Advice único que extiende `ResponseEntityExceptionHandler`
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)  // 404
    @ExceptionHandler(IllegalArgumentException.class)   // 400 (regla de negocio)
    @ExceptionHandler(AuthenticationException.class)    // 401 (BadCredentials -> "Credenciales inválidas")
    @ExceptionHandler(AccessDeniedException.class)     // 403 — DEBE existir si hay handler de Exception
    @ExceptionHandler(DataIntegrityViolationException.class) // 409 genérico
    @ExceptionHandler(Exception.class)                  // 500 genérico, log.error(ex), SIN stacktrace al cliente

    @Override protected ResponseEntity<Object> handleMethodArgumentNotValid(...) {
        // {"error":"Datos inválidos","campos":{"marca":"..."}}
    }
    @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // {"error": mensajeEnEspañolPorStatus(status)}  (400,404,405,413,415...)
    }
}
```
Resultados `[VERIFIED: sonda con un único advice]`: validación → `400 {"campos":{...},"error":"Datos invalidos"}`; JSON mal formado, enum inválido, type mismatch (`/publicaciones/abc`), parte multipart faltante → 400 con `{"error"}`; ruta inexistente → 404 (`NoResourceFoundException`); método no soportado → 405; `MaxUploadSizeExceededException` → **413**; `RuntimeException` → 500 `{"error":"Error interno"}`; `AccessDeniedException` lanzada desde un service → 403.

### Pattern 4: Seeder de arranque
```java
@Component @RequiredArgsConstructor @Slf4j
public class DataSeeder implements ApplicationRunner {
    @Value("${app.admin.email:}") private String adminEmail;
    @Value("${app.admin.password:}") private String adminPassword;
    @Value("${app.admin.nombre:}") private String adminNombre;
    private final UsuarioRepository usuarioRepository;
    private final AgenciaRepository agenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        sembrarAgencia();
        sembrarAdmin();
    }
    // sembrarAdmin: if (usuarioRepository.existsByRol(Rol.ADMIN)) return;  // D-02: no toca nada
    //   faltan vars -> boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
    //   prod ? throw new IllegalStateException("Falta ADMIN_EMAIL/ADMIN_PASSWORD ...") : log.warn(...)
    //   email ya registrado como COMPRADOR -> fallar/avisar con mensaje claro; NO promover (D-04)
    //   password < 8 caracteres -> mismo criterio que RegistroRequest (@Size(min = 8))
    //   NUNCA loguear la contraseña
}
```
Necesita `boolean existsByRol(Rol rol)` en `UsuarioRepository` y `Optional<Agencia> findFirstByOrderByIdAsc()` en `AgenciaRepository`. Lanzar desde `run()` aborta el arranque (comportamiento estándar de `ApplicationRunner`; `[ASSUMED]`, se prueba con un test unitario que espera la excepción).

### Pattern 5: Interceptor axios + manejador registrado por AuthProvider
```js
// api.js
let manejadorSesionVencida = null
export const registrarManejadorSesionVencida = (fn) => { manejadorSesionVencida = fn }

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  // no mandar un Bearer viejo a /auth/*: con el filtro actual rompe incluso el login
  if (token && !config.url?.startsWith('/auth/')) config.headers.Authorization = `Bearer ${token}`
  return config
})
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const esAuth = error.config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && !esAuth && localStorage.getItem('token')) manejadorSesionVencida?.()
    return Promise.reject(error)
  }
)
```
`AuthProvider` (ya dentro de `BrowserRouter` en `main.jsx`) registra en un `useEffect`: `logout()` + `navigate('/login', { replace: true, state: { from: window.location.pathname + window.location.search, sesionVencida: true } })`. Como `logout` borra el token de forma síncrona, los 401 simultáneos siguientes no vuelven a disparar el manejador. `LoginPage` hace `navigate(state?.from ?? '/')` y muestra el aviso; usar `state` del router (no un query param) evita un open redirect.

### Anti-Patterns to Avoid
- **Un `@ExceptionHandler(MaxUploadSizeExceededException.class)` propio junto a `ResponseEntityExceptionHandler`:** `[VERIFIED: sonda]` el contexto no arranca (`IllegalStateException: Ambiguous @ExceptionHandler method mapped for [class ...MaxUploadSizeExceededException]`). Personalizar vía `handleExceptionInternal`.
- **Dos advices con handlers solapados:** en la sonda, el `GlobalExceptionHandler` viejo (`IllegalArgumentException`) capturó el `NumberFormatException` causa de un type mismatch y devolvió `"For input string: \"abc\""` en inglés. Dejar UN solo advice.
- **`@ExceptionHandler(Exception.class)` sin handler de `AccessDeniedException`:** convierte un 403 en 500 si algún día se usa `@PreAuthorize`.
- **Poner el `try/catch` alrededor de `filterChain.doFilter`:** taparía errores reales de los controllers.
- **Confiar solo en `Content-Type` del upload:** lo manda el cliente; la barrera real son los magic bytes.
- **Tratar cualquier 401 del front como sesión vencida:** el login con credenciales malas también devuelve 401 (`BadCredentialsException`) y NO debe redirigir ni mostrar "sesión vencida".

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Formato de errores de framework (400/404/405/415/413) | Un `@ExceptionHandler` por cada excepción de Spring MVC | Extender `ResponseEntityExceptionHandler` y sobreescribir `handleExceptionInternal` | Cubre ~20 excepciones; la sonda confirmó el formato uniforme |
| Respuesta 401/403 de seguridad | `response.getWriter().write("{...}")` a mano en el filtro | `AuthenticationEntryPoint` + `AccessDeniedHandler` en `exceptionHandling(...)` | Se ejecutan en el `ExceptionTranslationFilter` y cubren también "sin token" |
| Hash de contraseña del admin | MD5/SHA propio o hash fijo en SQL | El bean `PasswordEncoder` (BCrypt) ya existente | Misma ruta que el login; D-01 prohíbe credenciales en el repo |
| Detección de tipo de imagen | Parsear por extensión o por `getContentType()` solo | Content-type allowlist + firma binaria (12 bytes) con `readNBytes`, más `allowed_formats` en Cloudinary como defensa en profundidad | La extensión y el content-type son controlados por el cliente |
| Borrado de assets | Reconstruir `public_id` parseando la URL | Guardar el `public_id` que devuelve la subida | El `public_id` puede o no incluir el folder según el modo de carpetas de la cuenta (ver Pitfall 7) |
| Diálogo de confirmación | `window.confirm` o librería de modales | `<dialog>` nativo (`showModal()`) | Accesible, sin dependencia, estilable con Tailwind (`backdrop:`) |
| Reorden con drag & drop | `@dnd-kit` u otra lib | Botones ← → y "Hacer portada" que mandan la lista completa de ids | Menos código, mejor en mobile, cero dependencias |
| Migración de columnas | Scripts SQL manuales | `ddl-auto: update` (esta fase) con columnas nullable/default | Flyway llega en la Fase 2 y toma el esquema de esta fase como línea base |

**Key insight:** casi todo el riesgo está en los bordes (filtro fuera del advice, límites de Tomcat, DDL sobre tablas con datos, FKs al borrar), no en la lógica de negocio. Lo difícil ya lo resuelven Spring Security / `ResponseEntityExceptionHandler`; el trabajo es cablearlo bien y probarlo.

## Runtime State Inventory

> No es una fase de rename/refactor/migración pura, pero cambia el esquema de tablas con datos de dev y retira endpoints. Se completa la parte relevante.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | Base Postgres de dev (`danteautomotores`, puerto 5433) con posibles filas en `publicaciones`, `fotos_publicacion`, `agencias`, `usuarios` ya creadas con el esquema viejo. Las fotos viejas no tienen `public_id`. Puede haber varias agencias y usuarios ADMIN de pruebas (D-04 los deja). | Columnas nuevas **nullable o con default** (`destacado` con `@ColumnDefault("false")`, `public_id` nullable). Borrado de fotos viejas sin `public_id`: omitir el destroy y loguear. Publicaciones apuntando a otra agencia: `actualizar` las reasigna a la única. **Migración de datos: ninguna obligatoria** |
| Live service config | Cloudinary: assets existentes en `danteautomotores/publicaciones` sin `public_id` en la DB | Ninguna acción; quedan como huérfanos aceptables (dev). Verificado: no hay otra config viva dependiente |
| OS-registered state | None — verificado: no hay tareas programadas ni servicios que referencien nombres de esta fase | None |
| Secrets/env vars | Nuevas: `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOMBRE` (sin defaults). Sin env vars de este grupo seteadas en la shell actual (`env \| grep` vacío; no se pudo comprobar la config del IDE) | Documentar en README (tabla de variables); el front `.env` no cambia |
| Build artifacts | `target/` (gitignored). El front no tiene artefactos instalados dependientes | None |

**API retirada (consumidores):** `POST /api/agencias` y `DELETE /api/agencias/{id}` solo los usa `AdminDashboardPage.jsx` (`api.post('/agencias')`, `api.delete('/agencias/...')`); `AgenciaPage` y `HomePage` usan solo GET `[VERIFIED: grep de api.* en src/]`. `agenciaId` en `PublicacionRequest` solo lo manda `AdminPublicacionFormPage.jsx`.

## Common Pitfalls

### Pitfall 1: Token malo → 500, y "sin token" → 403 vacío
**What goes wrong:** El filtro lanza `JwtException`/`IllegalArgumentException`/`UsernameNotFoundException` y llega al contenedor como 500; un request anónimo a un endpoint protegido devuelve 403 sin cuerpo (el front no puede distinguirlo de "no tenés permiso").
**Why it happens:** El advice no ve excepciones del filtro; sin `AuthenticationEntryPoint`, Spring Security usa el entry point por defecto que responde 403.
**How to avoid:** Patrón 1 + Patrón 2. **Consecuencia oculta:** con un token vencido en `localStorage`, hoy el propio `POST /auth/login` falla con 500 porque el interceptor de request adjunta el Bearer viejo; por eso el patrón 5 no manda Bearer a `/auth/*`.
**Warning signs:** logs con `ExpiredJwtException` y stacktrace; respuestas 403 con `content-length: 0`.

### Pitfall 2: Handler genérico que se come excepciones de framework
**What goes wrong:** Un `@ExceptionHandler(Exception.class)` suelto convierte `NoResourceFoundException` (404), `HttpMessageNotReadableException` (400) y `AccessDeniedException` (403) en 500.
**How to avoid:** Extender `ResponseEntityExceptionHandler` (cubre las de MVC) y declarar explícito `AccessDeniedException`/`AuthenticationException`. No declarar handlers para excepciones que la base ya maneja (ambigüedad, ver Anti-Patterns). Un solo advice.
**Warning signs:** 500 en rutas inexistentes o JSON inválido.

### Pitfall 3: `orden` duplicado al borrar una foto
**What goes wrong:** `agregarFoto` asigna `orden = publicacion.getFotos().size()` (`PublicacionService.java:124` usa `.orden(siguienteOrden)`). Con órdenes [0,1,2] y borrando la 0, `size()` vale 2 y la nueva foto repite `orden = 2`. La portada (orden 0) también queda sin definir si se borra la primera.
**How to avoid:** En `eliminarFoto`, quitar la foto de `publicacion.getFotos()` (hay `orphanRemoval`) y **resecuenciar** 0..n-1 por orden/id; en `agregarFoto` usar `max(orden)+1` o resecuenciar. Test unitario con [0,1,2] → borrar la del medio → órdenes [0,1].
**Warning signs:** dos fotos con el mismo `orden`; portada que cambia sola.

### Pitfall 4: `destacado` primitivo rompe `ddl-auto: update` en tablas con filas
**What goes wrong:** `private boolean destacado` genera `boolean not null` sin default; PostgreSQL no puede agregar esa columna a una tabla con filas y la app no arranca en una DB de dev poblada.
**Verified:** `[VERIFIED: sonda con SchemaGeneration de Hibernate 6.5.3 / PostgreSQLDialect]` genera
`plano boolean not null` (primitivo, **falla**), `conDefault boolean default false not null` (`@Column(nullable=false) @ColumnDefault("false")`, **OK**), `conDefinition boolean default false not null` (`columnDefinition = "boolean default false"`, OK).
**How to avoid:** `@Column(nullable = false) @ColumnDefault("false") @Builder.Default private boolean destacado = false;` y `@Column(name = "public_id")` nullable en `FotoPublicacion`. Ojo con `@Builder.Default` (el builder de Lombok ignora el inicializador sin él; el proyecto ya lo usa en `moneda`/`estado`/`fotos`).
**Warning signs:** `column "destacado" of relation "publicaciones" contains null values` al arrancar.

### Pitfall 5: Borrar una publicación con favoritos o consultas → violación de FK (500)
**What goes wrong:** `Consulta.publicacion` (`@JoinColumn(name = "publicacion_id", nullable = false)`, `Consulta.java:22`) y `Favorito.publicacion` (`Favorito.java:26`) apuntan a `publicaciones`; `PublicacionService.eliminar` hace `deleteById` sin limpiar dependientes. El ADM-02 "eliminar" falla en cuanto un comprador marcó el auto como favorito o consultó.
**How to avoid:** En `eliminar` (`@Transactional`): borrar primero `favoritos` y `consultas` de esa publicación (derived `deleteByPublicacionId`) y recién entonces la publicación; agregar un handler `DataIntegrityViolationException → 409` como red. Ver Open Question 1 (qué hacer con las consultas).
**Warning signs:** `DataIntegrityViolationException` al eliminar un auto con actividad.

### Pitfall 6: Archivo grande → el cliente ve "Network Error", no el 413 JSON
**What goes wrong:** Cuando el archivo excede el límite multipart, Tomcat intenta "tragar" el resto del body hasta `server.tomcat.max-swallow-size` (default 2 MB); si el cliente sigue enviando más, cierra la conexión y el navegador ve un reset sin respuesta, así que ni el advice ni el interceptor reciben el 413 `[CITED: github.com/spring-projects/spring-boot issues + mkyong.com/spring/spring-file-upload-and-connection-reset-issue]`. Una foto de celular de 12 MB cae exactamente en este caso.
**How to avoid:** (a) validar tamaño/tipo **en el front antes de subir** (previene el caso normal); (b) subir `server.tomcat.max-swallow-size` a un valor acotado (p. ej. `50MB`, no `-1`); (c) `max-request-size` un poco mayor que `max-file-size` (p. ej. 12 MB vs 10 MB, hoy ambos 10 MB en `application.yml:20-21`) y chequeo explícito `archivo.getSize() > 10 MB` para el mensaje claro; (d) el helper de errores del front debe tratar "sin `response`" como "No se pudo subir (¿archivo muy grande?)". Probar manualmente con un archivo de 12–15 MB (MockMvc **no** aplica los límites de Tomcat, así que no hay test automatizado fiel).
**Warning signs:** `ERR_CONNECTION_RESET` en el navegador; el handler de 413 nunca se ejecuta.

### Pitfall 7: `public_id` y modo de carpetas de Cloudinary
**What goes wrong:** El código actual sube con `ObjectUtils.asMap("folder", "danteautomotores/publicaciones")`. Según la doc, `folder` antepone el path al `public_id` solo en cuentas con *legacy fixed folder mode*; en cuentas de carpetas dinámicas `folder` no se antepone al `public_id` salvo configuración `[CITED: cloudinary.com/documentation/image_upload_api_reference_upload]`. Armar el `public_id` a mano sería un bug según la cuenta.
**How to avoid:** Guardar **literalmente** `resultado.get("public_id")` de la respuesta de subida y usar ese mismo valor en `destroy`. `Uploader.destroy(String publicId, Map options)` existe en 1.39.0 `[VERIFIED: javap del jar]`; devuelve `{"result":"ok"}` si borró `[CITED: cloudinary.com/documentation/image_upload_api_reference_destroy_by_public_id]`; tratar `"ok"` y `"not found"` como éxito (el segundo `[ASSUMED]`, valor no confirmado en la doc consultada) y cualquier otra cosa/excepción como warning sin bloquear (CONTEXT). Opciones útiles: `"invalidate", true`, `"resource_type", "image"`.
**Warning signs:** `"result":"not found"` sistemático en los logs.

### Pitfall 8: Borrado en Cloudinary dentro de la transacción
**What goes wrong:** Si se llama a Cloudinary dentro de la transacción y esta luego hace rollback, el asset ya no existe pero la fila sí (imagen rota).
**How to avoid:** Recolectar los `public_id` y ejecutar el destroy **después del commit** (`@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` o `TransactionSynchronization.afterCommit`), con try/catch + `log.warn`. `public_id == null` (fotos viejas) → omitir.

### Pitfall 9: Rutas admin bajo un prefijo `permitAll`
**What goes wrong:** Un endpoint nuevo bajo `GET /api/publicaciones/**` queda público (el matcher `permitAll` de `SecurityConfig.java:46` va antes). Y `/api/admin/**` sin matcher explícito caería en `.anyRequest().authenticated()` (`:52`), es decir, accesible a COMPRADOR.
**How to avoid:** Poner el listado admin en `/api/admin/publicaciones` y agregar `.requestMatchers("/api/admin/**").hasRole("ADMIN")` **antes** de `anyRequest()`. Test: COMPRADOR → 403, anónimo → 401, ADMIN → 200. El orden de matchers importa (el primero que coincide gana).
**Warning signs:** el listado admin responde 200 sin token.

### Pitfall 10: Subida múltiple en el front aborta todo ante un error
**What goes wrong:** `subirFotos` (`AdminPublicacionFormPage.jsx:178-196`) sube en un `for` con un único `try/catch`: el primer archivo inválido corta el resto y muestra "No se pudo subir alguna de las fotos." sin decir cuál ni por qué.
**How to avoid:** Pre-validar cada archivo (JPEG/PNG/WebP, ≤10 MB) y subir los válidos uno por uno acumulando un mensaje por archivo rechazado (`nombre: motivo`); `accept="image/jpeg,image/png,image/webp"`. HEIC de iPhone: avisar que no se admite.

### Pitfall 11: Rol "ADMIN" falsificable en el cliente (`localStorage.usuario`)
**What goes wrong:** `esAdmin` sale de `localStorage.usuario.rol`, editable por el usuario; ve las pantallas admin pero cada llamada falla.
**How to avoid:** Es solo UX: la autorización real es del backend. Con el interceptor, un 401/403 se maneja; mostrar el `error` en la UI. No requiere cambio de diseño, pero los tests de seguridad deben ser del backend.

## Code Examples

### Validación de imagen (backend)
```java
// Firmas: JPEG FF D8 FF | PNG 89 50 4E 47 0D 0A 1A 0A | WebP "RIFF"....."WEBP" (bytes 0-3 y 8-11)  [ASSUMED: tablas estándar de firmas]
@Component
public class ImagenValidator {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final int MAX_FOTOS = 10;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    public void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("Elegí una imagen para subir");
        if (archivo.getSize() > MAX_BYTES) throw new IllegalArgumentException("La imagen supera el máximo de 10 MB");
        String ct = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!TIPOS.contains(ct)) throw new IllegalArgumentException("Formato no permitido. Usá JPG, PNG o WebP");
        byte[] h;
        try (InputStream in = archivo.getInputStream()) { h = in.readNBytes(12); }
        catch (IOException e) { throw new IllegalArgumentException("No se pudo leer el archivo"); }
        if (!(esJpeg(h) || esPng(h) || esWebp(h)))
            throw new IllegalArgumentException("El archivo no es una imagen válida (JPG, PNG o WebP)");
    }
    // esJpeg: h.length>=3 && (h[0]&0xFF)==0xFF && (h[1]&0xFF)==0xD8 && (h[2]&0xFF)==0xFF  ... etc.
}
```

### Subida y borrado en Cloudinary
```java
// Subida: guardar AMBOS valores de la respuesta
Map<?, ?> r = cloudinary.uploader().upload(archivo.getBytes(), ObjectUtils.asMap(
        "folder", "danteautomotores/publicaciones",
        "resource_type", "image",
        "allowed_formats", "jpg,png,webp"));          // [CITED: allowed_formats = lista separada por comas]
String url = (String) r.get("secure_url");
String publicId = (String) r.get("public_id");
// Borrado best-effort (después del commit)
Map<?, ?> res = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
```
Si faltan credenciales (default vacío en `application.yml`), el SDK lanza una excepción propia al subir (`[ASSUMED]` probablemente `IllegalArgumentException("Must supply api_key")`, que hoy el handler convertiría en 400 con texto en inglés); envolver errores del SDK en una excepción de servicio externo → 502/500 con mensaje en español.

### Reordenar fotos
```java
public PublicacionResponse reordenarFotos(Long id, ReordenarFotosRequest req) {
    Publicacion p = buscarEntidad(id);
    Map<Long, FotoPublicacion> porId = p.getFotos().stream().collect(Collectors.toMap(FotoPublicacion::getId, f -> f));
    List<Long> ids = req.getFotoIds();
    if (ids.size() != porId.size() || new HashSet<>(ids).size() != ids.size() || !porId.keySet().containsAll(ids))
        throw new IllegalArgumentException("El orden debe incluir todas las fotos del auto, una sola vez cada una");
    for (int i = 0; i < ids.size(); i++) porId.get(ids.get(i)).setOrden(i);   // orden 0 = portada
    return PublicacionMapper.toResponse(p);
}
// Controller: @PutMapping("/{id}/fotos/orden") -> cubierto por PUT /api/publicaciones/** hasRole ADMIN (SecurityConfig.java:48)
```

### Endpoint de destacado (patrón de `PATCH /{id}/estado`)
```java
@PatchMapping("/{id}/destacado")
public ResponseEntity<PublicacionResponse> cambiarDestacado(@PathVariable Long id, @Valid @RequestBody CambiarDestacadoRequest r) {...}
// CambiarDestacadoRequest: @NotNull private Boolean destacado;   // PATCH ya está cubierto por SecurityConfig.java:50
// PublicacionResponse + PublicacionMapper: agregar `destacado`
```
Decisión recomendada para "vendido y destacado": **no acoplar** (el flag es independiente; no se limpia al marcar VENDIDO). La Home de la Fase 2 filtra `destacado && estado != VENDIDO`.

### Test de seguridad sin DB (patrón verificado)
```java
@WebMvcTest(PublicacionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
         RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})   // + GlobalExceptionHandler lo trae el slice
@TestPropertySource(properties = {"app.jwt.secret=0123456789012345678901234567890123456789",
        "app.jwt.expiration-ms=86400000", "app.cors.allowed-origins=http://localhost:5173"})
class SeguridadErroresTest {
    @Autowired MockMvc mvc;
    @MockBean PublicacionService publicacionService;
    @MockBean UserDetailsService userDetailsService;   // CustomUserDetailsService no entra en el slice
    // GET público + "Bearer basura" -> 200 ; POST sin token / vencido / usuario borrado -> 401 {"error"}; COMPRADOR -> 403 {"error"}
}
```
`[VERIFIED: sonda]` `@MockBean` funciona en Boot 3.3.5 (queda deprecado en 3.4+: usar `@MockitoBean` si se actualiza). Los tokens de prueba se arman con `Jwts.builder().subject(..).expiration(..).signWith(Keys.hmacShaKeyFor(secret.getBytes()))`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `WebSecurityConfigurerAdapter` / `antMatchers` | `SecurityFilterChain` bean + `requestMatchers` (ya usado en el proyecto) | Spring Security 5.7/6 | Sin cambios; mantener el estilo actual |
| `@MockBean` | `@MockitoBean` | Spring Boot 3.4 | El proyecto está en 3.3.5: usar `@MockBean` hoy |
| Handler 404 por `NoHandlerFoundException` | `NoResourceFoundException` (ErrorResponse) | Spring 6.1 / Boot 3.2 | La sonda confirmó 404 vía `ResponseEntityExceptionHandler` |
| Excepciones MVC sin `ErrorResponse` | `ProblemDetail` / `ErrorResponse` | Spring 6 | Por eso hay que sobreescribir `handleExceptionInternal` para mantener `{"error"}` |

**Deprecated/outdated:**
- `npm run lint` del front: el script existe pero **eslint no está instalado** (`node_modules/.bin` solo tiene `vite`; no hay config). No usarlo como gate.
- README del back dice "Postgres en el puerto 5432" pero `docker-compose.yml` mapea `5433:5432` (stale) y "JDK 25" en el Dockerfile mientras el Dockerfile usa JDK 21 (la rama de modernización no está en `main`).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Lanzar una excepción desde `ApplicationRunner.run` aborta el arranque de Spring Boot | Pattern 4 | Si no abortara, "fallar en prod" (D-03) no se cumple; se detecta con un test que espera la excepción y una corrida real sin vars con `SPRING_PROFILES_ACTIVE=prod` |
| A2 | Firmas de archivo JPEG/PNG/WebP (FF D8 FF / 89 50 4E 47 0D 0A 1A 0A / RIFF..WEBP) | Code Examples | Falsos rechazos de imágenes válidas; se mitiga con test usando imágenes reales mínimas de cada formato |
| A3 | Cloudinary `destroy` devuelve `"not found"` para un asset inexistente | Pitfall 7 | Un warning de más en logs; sin impacto funcional |
| A4 | El SDK de Cloudinary lanza `IllegalArgumentException` al subir sin credenciales | Code Examples | Mensaje en inglés / 400 en vez de 5xx cuando faltan credenciales (PROD-02 en Fase 2 lo ataca) |
| A5 | El 401 del entry point lleva headers CORS (el `CorsFilter` corre antes) y axios puede leer `error.response.status` | Pattern 5 | Si faltaran, el front vería "Network Error" en vez de 401; se detecta en la UAT manual desde `localhost:5173` |
| A6 | `ddl-auto: update` con `public_id` nullable agrega la columna sin problemas a filas existentes | Runtime State | Bajo; columna nullable es el caso trivial. No se pudo probar contra Postgres real (Docker apagado) |
| A7 | Inventario pequeño (< unos cientos de autos): listado admin sin paginar y filtro/búsqueda client-side alcanza | Pattern/Structure | Si el inventario creciera mucho, paginar en Fase 2+; bajo riesgo para una sola agencia |
| A8 | iOS Safari convierte HEIC a JPEG cuando `accept` lista tipos específicos | Pitfall 10 | Algunos usuarios iPhone verían el archivo rechazado; mensaje claro de todos modos |
| A9 | El perfil de producción de la Fase 2 se llamará `prod` | Pattern 4 | Si se llama distinto, el seeder no falla en prod; coordinar el nombre al planear la Fase 2 |

## Open Questions

1. **¿Qué pasa con las consultas (leads) al eliminar una publicación?**
   - What we know: FK `NOT NULL` desde `consultas` y `favoritos`; hoy el borrado falla con 500.
   - What's unclear: si el negocio quiere perder las consultas del auto eliminado.
   - Recommendation: borrar favoritos siempre; borrar también las consultas en la misma transacción (la `Consulta` anónima probablemente la reemplace el modelo de conversaciones de la Fase 4) y mostrar en el diálogo de confirmación cuántas se perderán. Alternativa conservadora: responder 409 "tiene consultas, marcalo como vendido". Pedir confirmación al usuario en el plan-check.

2. **Nombre del perfil de producción**
   - What we know: D-03 pide "fallar en prod, avisar en dev"; la Fase 2 crea el perfil.
   - What's unclear: el nombre exacto.
   - Recommendation: usar `prod` (`Profiles.of("prod")`) y dejarlo anotado para la Fase 2.

3. **Email de contacto de la agencia sembrada**
   - What we know: `AgenciaRequest.emailContacto` es `@NotBlank`, pero la entidad lo admite nulo; el seed no tiene un dato real.
   - Recommendation: sembrar solo `nombre` y `slug = "dante-automotores"` (derivado con `normalizarSlug`), dejar el resto vacío; el admin completa los datos al primer "Editar" (el form ya los exige).

4. **Qué hacer con agencias extra ya existentes en la DB de dev**
   - Recommendation: "la agencia" = `findFirstByOrderByIdAsc()`; no borrar las otras (D-05); el panel muestra solo esa.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 21 (pom `java.version`=21) | `mvn compile/test` | ✗ | — (solo JDK 17.0.12 en `C:\Program Files\Java\jdk-17`) | `mvn -Djava.version=17 ...` **verificado**: compila y corre tests; en Docker/CI se usa JDK 21 |
| Maven | build/tests | ✓ (no está en PATH ni hay `mvnw`) | 3.9.16 en `C:\Users\toto\.maven\maven-3.9.16\bin\mvn` | Agregar al PATH en la sesión: `export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; export JAVA_HOME="/c/Program Files/Java/jdk-17"` |
| Surefire provider (descarga) | `mvn test` | ✓ ahora en `~/.m2` | 3.2.5 | La primera corrida necesitó red; ya queda offline (`-o`) |
| Docker Desktop / daemon | Postgres de dev (`docker compose up -d`, puerto 5433) | ✗ (CLI 29.6.2 instalado, **daemon apagado**) | — | Iniciar Docker Desktop; sin DB no se puede levantar la app ni hacer UAT E2E. Los tests automatizados NO la necesitan |
| PostgreSQL 16 | Arranque real / UAT | ✗ (depende de Docker) | — | `docker compose up -d` |
| Node / npm | Front | ✓ | Node 24.14.1 / npm 11.11.0 | — |
| Credenciales Cloudinary | UAT de subida real | ? (no hay env vars en la shell; no se pudo comprobar la config del IDE) | — | UAT de fotos requiere `CLOUDINARY_*` reales; los tests automatizados mockean `CloudinaryService` |
| Python | — | ✗ | — | No hace falta |

**Missing dependencies with no fallback:**
- Para el UAT end-to-end: Docker daemon corriendo (Postgres) y credenciales de Cloudinary. Son prerequisitos humanos del checkpoint `human-verify` de fin de fase (`human_verify_mode: end-of-phase`).

**Missing dependencies with fallback:**
- JDK 21 → `-Djava.version=17` para correr tests localmente.

## Validation Architecture

> `workflow.nyquist_validation: true` en `.planning/config.json`.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + Spring MockMvc / spring-security-test (de `spring-boot-starter-test`, Boot 3.3.5) `[VERIFIED: pom.xml]` |
| Config file | ninguno — `src/test/` **no existe** (0 tests). Ver Wave 0 |
| Quick run command | `mvn -B -o -Djava.version=17 test -Dtest=NombreDeLaClase` |
| Full suite command | `mvn -B -o -Djava.version=17 test` |
| Setup de shell (Windows/Git Bash) | `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"` |
| Front gate automatizado | `cd C:/Users/toto/Desktop/work/danteautomotores-front && npm run build` (no hay test runner; `npm run lint` NO funciona: eslint no instalado) |

`[VERIFIED: ejecutado esta sesión]` Con ese setup, `mvn -B -o -Djava.version=17 clean test -Dtest=...` corrió tests `@WebMvcTest` verdes en ~3 s sin base de datos. Si cambia el `target/` entre JDKs, usar `clean` (se vio un `class file version 65.0` residual de una compilación previa con release 21). En `pom.xml`/Dockerfile **no** se debe tocar `java.version` ni quitar `-DskipTests` (QA-V2-01 está diferido).

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ADM-01 | Seed crea ADMIN solo si no hay ninguno; no modifica uno existente; prod sin vars → excepción; dev sin vars → warn; email ya usado por COMPRADOR → error claro; siembra la agencia si no hay | unit (Mockito) | `mvn -B -o -Djava.version=17 test -Dtest=DataSeederTest` | ❌ Wave 0 |
| ADM-01 | `POST /api/auth/registro` con `"rol":"ADMIN"` devuelve rol COMPRADOR; no existe endpoint de promoción | unit/slice | `mvn -B -o -Djava.version=17 test -Dtest=AuthServiceTest` | ❌ Wave 0 |
| ADM-02 | `crear`/`actualizar` asignan la agencia única sin `agenciaId`; `eliminar` borra favoritos/consultas antes y no falla | unit (Mockito) | `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest` | ❌ Wave 0 |
| ADM-02 / D-07 | `POST`/`DELETE /api/agencias` ya no existen (404/405); `PUT` y `GET` siguen | slice `@WebMvcTest(AgenciaController)` | `mvn -B -o -Djava.version=17 test -Dtest=AgenciaControllerTest` | ❌ Wave 0 |
| ADM-03 | Validador: JPEG/PNG/WebP reales OK; `.exe` con `Content-Type: image/jpeg` rechazado; >10 MB rechazado; vacío rechazado | unit | `mvn -B -o -Djava.version=17 test -Dtest=ImagenValidatorTest` | ❌ Wave 0 |
| ADM-03 | Tope de 10 fotos; reorden válido/ inválido (faltan ids, duplicados, ajenos); borrar foto resecuencia 0..n-1; `public_id` se guarda y se manda a destroy; fallo de Cloudinary no bloquea | unit (Mockito, `CloudinaryService` mock) | `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest` | ❌ Wave 0 |
| ADM-04 | Listado admin devuelve todos los estados; protegido por rol | slice | `mvn -B -o -Djava.version=17 test -Dtest=AdminPublicacionControllerTest` | ❌ Wave 0 |
| ADM-04 | `PATCH /{id}/estado` cambia el estado | unit | `... -Dtest=PublicacionServiceTest` | ❌ Wave 0 |
| ADM-05 | `PATCH /{id}/destacado` persiste y se refleja en la respuesta; body sin `destacado` → 400 | unit + slice | `... -Dtest=PublicacionServiceTest,PublicacionControllerTest` | ❌ Wave 0 |
| PROD-01 | Token basura / vencido / vacío / usuario borrado → 401 `{"error"}` en endpoint protegido y 200 en público; sin token → 401; COMPRADOR → 403 `{"error"}` | slice `@WebMvcTest` + `SecurityConfig` real | `mvn -B -o -Djava.version=17 test -Dtest=SeguridadErroresTest` | ❌ Wave 0 |
| PROD-01 | Formato uniforme: validación (`error`+`campos`), 404, JSON mal formado 400, ruta inexistente 404, 405, 413 por `MaxUploadSizeExceededException`, 500 genérico sin stacktrace | slice | `mvn -B -o -Djava.version=17 test -Dtest=GlobalExceptionHandlerTest` | ❌ Wave 0 |
| ADM-02/03/04/05 (UI) | Panel: crear/editar/eliminar con confirmación in-UI, estado, destacado, reordenar con flechas, mensajes del backend, selector de agencia ausente, sin "Nueva agencia"/"Eliminar" | **manual-only** (sin framework de tests de front; QA-V2-01 diferido) | `npm run build` como gate de compilación + UAT manual | n/a |
| PROD-01 (UI) | Token vencido → el panel va a `/login` con aviso "sesión vencida" y vuelve a la página de origen; login con credenciales malas NO redirige | **manual-only** | UAT: editar `localStorage.token` a un valor inválido o esperar el vencimiento (`APP_JWT_EXPIRATION_MS=60000`) | n/a |
| ADM-03 (límite real) | Subir un archivo de 12–15 MB muestra el mensaje (no queda colgado/Network Error) | **manual-only** (MockMvc no aplica límites de Tomcat) | UAT con archivo grande contra la app corriendo | n/a |
| ADM-01 (arranque real) | App arranca con vars → admin puede loguearse; arranque con `SPRING_PROFILES_ACTIVE=prod` sin vars falla | **manual smoke** | `mvn spring-boot:run` + `curl -X POST localhost:8080/api/auth/login ...` | n/a |

### Sampling Rate
- **Per task commit:** `mvn -B -o -Djava.version=17 test -Dtest=<clase del task>` (+ `npm run build` si el task toca el front)
- **Per wave merge:** `mvn -B -o -Djava.version=17 test` completo y `npm run build`
- **Phase gate:** suite completa verde + `npm run build` + UAT manual del front (con Docker/Postgres y Cloudinary) antes de `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/danteautomotores/config/DataSeederTest.java` — ADM-01 (Mockito: repos, `PasswordEncoder`, `MockEnvironment`)
- [ ] `src/test/java/com/danteautomotores/service/AuthServiceTest.java` — ADM-01 (forzado a COMPRADOR)
- [ ] `src/test/java/com/danteautomotores/service/ImagenValidatorTest.java` — ADM-03 (fixtures: bytes mínimos reales de JPEG/PNG/WebP + un ejecutable falso)
- [ ] `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java` — ADM-02/03/04/05
- [ ] `src/test/java/com/danteautomotores/security/SeguridadErroresTest.java` — PROD-01 (patrón de la sección Code Examples)
- [ ] `src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java` — PROD-01
- [ ] `src/test/java/com/danteautomotores/controller/{Agencia,AdminPublicacion,Publicacion}ControllerTest.java` — D-07, ADM-04/05
- [ ] Nota de ejecución en README: setup de `JAVA_HOME`/`PATH` y `-Djava.version=17` (el equipo no tiene JDK 21)
- [ ] Framework install: ninguno (todo ya está en el pom). Frontend: ninguno en esta fase (se documenta como UAT manual)

## Security Domain

> `security_enforcement` habilitado, ASVS nivel 1, `security_block_on: high`.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Admin sembrado con BCrypt (`PasswordEncoder` existente); contraseña solo por env var, mínimo 8 caracteres, nunca logueada; sin endpoint de alta/promoción de admin |
| V3 Session Management | yes | JWT stateless (JJWT 0.12.6) con expiración 24 h; token inválido → sin autenticar → 401 uniforme; el front limpia la sesión |
| V4 Access Control | yes | `/api/admin/**` y escritura de publicaciones/agencias `hasRole("ADMIN")` en `SecurityConfig` (matchers antes de `anyRequest`); tests de 401/403 |
| V5 Input Validation | yes | `@Valid` + Bean Validation en DTOs nuevos; `ImagenValidator` (allowlist de content-type + magic bytes + tamaño + tope); `ReordenarFotosRequest` validado contra el conjunto real de fotos |
| V6 Cryptography | yes (acotado) | BCrypt y firma HMAC de JJWT; no inventar cripto. (El secret JWT por defecto es deuda de la Fase 2/PROD-02) |
| V7 Error Handling & Logging | yes | Handler genérico 500 sin stacktrace ni mensajes internos; `@Slf4j` solo para diagnóstico del servidor; no loguear credenciales |
| V12 Files and Resources | yes | Validación de subida (tipo real, tamaño, cantidad), `resource_type=image` y `allowed_formats` en Cloudinary, sin almacenamiento local |
| V14 Configuration | yes | Sin credenciales en el repo (`ADMIN_*` sin defaults); documentar variables en README |

### Known Threat Patterns for Spring Boot + JWT + Cloudinary

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Escalada de rol por mass assignment en el registro | Elevation of Privilege | `RegistroRequest` sin campo `rol`; `AuthService.registrar` fija `Rol.COMPRADOR` (`AuthService.java:35`); test con `"rol":"ADMIN"` en el body |
| Subida de archivo no-imagen / content-type falso | Tampering | Magic bytes + allowlist + tamaño + `allowed_formats` de Cloudinary |
| Endpoint admin accidentalmente público por orden de matchers | Elevation of Privilege | Prefijo `/api/admin/**` con matcher explícito; test anónimo/COMPRADOR/ADMIN |
| Filtración de información en errores (stacktrace, mensajes de excepción) | Information Disclosure | Handler 500 genérico; mensajes en español controlados; ejemplo real: `NumberFormatException` filtraba `For input string: "abc"` |
| Token vencido/manipulado provoca 500 (DoS de disponibilidad por logs) | Denial of Service | Filtro con catch + 401 |
| Agotamiento por uploads gigantes | Denial of Service | Límite 10 MB + `max-swallow-size` acotado (no `-1`) + tope de 10 fotos |
| Open redirect tras el login | Spoofing/Tampering | Usar `location.state.from` (interno), no un parámetro de URL arbitrario |
| Credencial del admin en logs o repo | Information Disclosure | Solo env vars; el seeder nunca imprime la contraseña |
| XSS almacenado (descripción, nombre de agencia, URL de logo) | Tampering | React escapa por defecto; no usar `dangerouslySetInnerHTML`; el logo se usa solo en `<img src>` |

## Project Constraints (from CLAUDE.md)

Directivas accionables extraídas de `./.claude/CLAUDE.md` (misma autoridad que las decisiones bloqueadas):

- **Stack fijo:** Spring Boot 3.3 / Java 21 / PostgreSQL / JPA + React 18 / Vite / Tailwind v4. No cambiar el stack ni agregar frameworks.
- **Idioma:** mensajes al usuario, errores, comentarios, documentación, nombres de tablas/campos y valores de enum **en español** (`Ya existe una cuenta con ese email`); nombres de clases en PascalCase en español de dominio (`Publicacion`, `Agencia`).
- **Convenciones backend:** `*Controller`, `*Service`, `*Repository`, `*Mapper` (métodos estáticos `toResponse`/`toEntity`), `*Config`, `*Exception`; DTOs por feature bajo `dto/<feature>/`; servicios devuelven DTOs, no entidades; controllers devuelven `ResponseEntity<T>`; `.orElseThrow()` para fallar rápido; `@Valid` en DTOs de request; Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Data`); 4 espacios; métodos < ~30 líneas.
- **Errores backend:** `ResourceNotFoundException` → 404, `IllegalArgumentException` → 400 (regla de negocio), `BadCredentialsException` → 401 genérico, `MethodArgumentNotValidException` → 400; formato `{"error": "mensaje"}`.
- **Convenciones front:** componentes/páginas `.jsx` PascalCase (sufijo `Page`), servicios/utils `.js` camelCase, handlers `handle*`, constantes en MAYÚSCULAS, 2 espacios, punto y coma consistente con el archivo existente, Tailwind utility-first con la paleta existente (`bg-bronze`, `text-navy-dark`, `slate-*`), props desestructuradas, `export default function`.
- **Imágenes:** Cloudinary (ya integrado). **Admin:** una única cuenta por ahora, sin impedir más a futuro. **UX:** familiar (Kavak / Mercado Libre), animaciones moderadas.
- **Repos separados;** la planificación vive en el back. Esta fase edita ambos.
- **Flujo GSD:** los cambios de archivos van por comandos GSD (`/gsd-execute-phase` para trabajo planificado); no editar el repo fuera de un workflow GSD.
- **Anti-patrones declarados:** datos mock en producción, autorización solo en el frontend, mutar objetos de respuesta.
- **Skills del proyecto:** no hay (`.claude/skills/` y similares no existen).

## Sources

### Primary (HIGH confidence)
- Código real leído esta sesión (backend y front): `pom.xml`, `application.yml`, `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `GlobalExceptionHandler`, `PublicacionService/Controller`, `AgenciaService/Controller`, `AuthService`, `CloudinaryService`, entidades y DTOs, `AdminDashboardPage.jsx`, `AdminPublicacionFormPage.jsx`, `api.js`, `AuthContext.jsx`, `ProtectedRoute.jsx`, `LoginPage.jsx`.
- **Sondas ejecutadas** (JDK 17 + Maven 3.9.16, `-Djava.version=17`, tests descartables ya borrados): comportamiento actual de filtro/403 vacío/401 con handlers/advice único/ambigüedad de `MaxUploadSizeExceededException`/doble advice; DDL de Hibernate 6.5.3 para `boolean`, `@ColumnDefault`, `columnDefinition`.
- `javap` de `cloudinary-core-1.39.0.jar`: firmas `Uploader.upload(Object, Map)` y `destroy(String, Map)`.
- https://cloudinary.com/documentation/image_upload_api_reference_upload — `allowed_formats`, `folder`, campos de la respuesta (`public_id`, `secure_url`)
- https://cloudinary.com/documentation/image_upload_api_reference_destroy_by_public_id — parámetros de `destroy` (`invalidate`, `resource_type`) y `{"result":"ok"}`

### Secondary (MEDIUM confidence)
- Búsqueda web sobre `server.tomcat.max-swallow-size` (default 2 MB; reset de conexión; el advice no se ejecuta): spring-projects/spring-boot issues #6669 / #18555, https://mkyong.com/spring/spring-file-upload-and-connection-reset-issue/, https://howtodoinjava.com/spring-boot2/embedded-tomcat-configuration/
- Búsqueda web sobre Cloudinary `destroy` en el SDK Java (`invalidate`, `resource_type`, `type`): https://cloudinary.com/documentation/delete_assets

### Tertiary (LOW confidence)
- Firmas de archivo y comportamiento de HEIC en iOS (training knowledge, ver A2/A8)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — todo ya está en `pom.xml`/`package.json`; no hay paquetes nuevos.
- Architecture: HIGH — el diseño de seguridad/errores se validó con sondas ejecutadas contra el `SecurityConfig` real y contra un advice prototipo.
- Pitfalls: HIGH para 1–5 y 9 (verificados por ejecución o lectura de código); MEDIUM para 6–7 (Tomcat/Cloudinary por docs y búsqueda, sin prueba end-to-end porque Docker/credenciales no están disponibles).
- Validation: HIGH en comandos (corridos); los tests listados aún no existen.

**Research date:** 2026-10-02
**Valid until:** 2026-11-01 (stack estable; Spring Boot 3.3.x y Cloudinary 1.39.0 fijados en el pom)
