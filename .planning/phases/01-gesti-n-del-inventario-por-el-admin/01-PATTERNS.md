# Phase 1: Gestión del inventario por el admin - Pattern Map

**Mapped:** 2026-10-02
**Files analyzed:** 36 (back: `src/main/java/com/danteautomotores/` = `B/`; front: `C:/Users/toto/Desktop/work/danteautomotores-front/src/` = `F/`)
**Analogs found:** 31 / 36 (todos los paths son archivos tracked por git; verificado con `git ls-files`)

No existe `src/test` ni framework de tests de front: los tests nuevos no tienen analog (ver "No Analog Found"); usar los ejemplos de RESEARCH.md (Validation Architecture / Code Examples).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `B/security/JwtAuthenticationFilter.java` (mod) | middleware | request-response | si mismo + RESEARCH Pattern 1 | exact |
| `B/security/RestAuthenticationEntryPoint.java` (new) | middleware | request-response | `B/exception/GlobalExceptionHandler.java` (formato `{"error"}`) + RESEARCH Pattern 2 | partial |
| `B/security/RestAccessDeniedHandler.java` (new) | middleware | request-response | idem entry point | partial |
| `B/config/SecurityConfig.java` (mod) | config | request-response | si mismo | exact |
| `B/exception/GlobalExceptionHandler.java` (mod) | middleware (advice) | request-response | si mismo | exact |
| `B/config/DataSeeder.java` (new) | config (arranque) | batch | `B/service/AuthService.java` (builder de Usuario + encoder) | role-partial |
| `B/repository/UsuarioRepository.java` (mod: `existsByRol`) | repository | CRUD | si mismo (`existsByEmail`) | exact |
| `B/repository/AgenciaRepository.java` (mod: `findFirstByOrderByIdAsc`) | repository | CRUD | si mismo (`findBySlug`) | exact |
| `B/repository/FavoritoRepository.java` / `ConsultaRepository.java` (mod: `deleteByPublicacionId`) | repository | CRUD | si mismos (derived queries) | exact |
| `B/entity/Publicacion.java` (mod: `destacado`) | model | CRUD | si mismo (`@Builder.Default estado`) | exact |
| `B/entity/FotoPublicacion.java` (mod: `publicId`) | model | CRUD | si mismo | exact |
| `B/dto/publicacion/CambiarDestacadoRequest.java` (new) | model (DTO) | request-response | `B/dto/publicacion/CambiarEstadoRequest.java` | exact |
| `B/dto/publicacion/ReordenarFotosRequest.java` (new) | model (DTO) | request-response | `CambiarEstadoRequest.java` | role-match |
| `B/dto/publicacion/PublicacionRequest.java` (mod: quitar `agenciaId`) | model (DTO) | request-response | si mismo | exact |
| `B/dto/publicacion/PublicacionResponse.java` + `B/mapper/PublicacionMapper.java` (mod: `destacado`) | model/mapper | transform | `PublicacionMapper.toResponse` | exact |
| `B/service/PublicacionService.java` (mod) | service | CRUD | si mismo | exact |
| `B/service/CloudinaryService.java` (mod: public_id + `eliminar`) | service | file-I/O | si mismo | exact |
| `B/service/ImagenValidator.java` (new) | utility | transform | RESEARCH Code Examples (no analog en codebase) | none |
| `B/service/AgenciaService.java` + `B/controller/AgenciaController.java` (mod: quitar crear/eliminar) | service/controller | CRUD | si mismos | exact |
| `B/controller/PublicacionController.java` (mod: PATCH destacado, PUT fotos/orden) | controller | request-response | `PATCH /{id}/estado` en el mismo archivo | exact |
| `B/controller/AdminPublicacionController.java` (new) | controller | request-response | `B/controller/PublicacionController.java` | role-match |
| `src/main/resources/application.yml` (mod) | config | - | si mismo | exact |
| `F/services/api.js` (mod) | service | request-response | si mismo | exact |
| `F/utils/errores.js` (new) | utility | transform | `F/utils/whatsapp.js` (utility camelCase) | role-match |
| `F/components/ConfirmDialog.jsx` (new) | component | event-driven | `Seccion`/`Input` en `AdminPublicacionFormPage.jsx` (:482-516) | partial |
| `F/context/AuthContext.jsx` (mod) | provider | event-driven | si mismo | exact |
| `F/components/ProtectedRoute.jsx` (mod) | component | request-response | si mismo | exact |
| `F/pages/LoginPage.jsx` (mod) | page | request-response | si mismo | exact |
| `F/pages/admin/AdminDashboardPage.jsx` (mod) | page | CRUD | si mismo | exact |
| `F/pages/admin/AdminPublicacionFormPage.jsx` (mod) | page | CRUD/file-I/O | si mismo | exact |
| Tests (`DataSeederTest`, `ImagenValidatorTest`, `PublicacionServiceTest`, `SeguridadErroresTest`, `GlobalExceptionHandlerTest`, `*ControllerTest`, `AuthServiceTest`) | test | - | ninguno | none |

## Pattern Assignments

### `B/security/JwtAuthenticationFilter.java` (middleware)

Hoy (lineas 38-52) `extractUsername`/`loadUserByUsername` no tienen try/catch: origen del 500. Envolver SOLO lineas 38-50, dejando `filterChain.doFilter` (:52) fuera:

```java
// actual :38-42
final String jwt = authHeader.substring(7);
final String userEmail = jwtService.extractUsername(jwt);
if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
```
Aplicar el bloque `try { ... } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) { SecurityContextHolder.clearContext(); }` de RESEARCH Pattern 1. Imports nuevos: `io.jsonwebtoken.JwtException`, `org.springframework.security.core.userdetails.UsernameNotFoundException`.

### `B/security/RestAuthenticationEntryPoint.java` y `RestAccessDeniedHandler.java` (nuevos)

No hay analog de handler de Security; copiar el formato `Map.of("error", ...)` de `GlobalExceptionHandler.java:19` y estilo `@Component @RequiredArgsConstructor` de `JwtAuthenticationFilter.java:19-21`. Cuerpo completo en RESEARCH Pattern 2 (401 "Tu sesión venció o no es válida..." / 403 "No tenés permiso..."), `ObjectMapper` inyectado.

### `B/config/SecurityConfig.java` (config)

Inyectar los dos handlers (campos `private final`, junto a lineas 32-33) y editar la cadena (:40-56):

```java
// agregar ANTES de .anyRequest() (:52), despues de la linea :51
.requestMatchers("/api/admin/**").hasRole("ADMIN")
// y en la cadena, junto a .sessionManagement (:54)
.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
```
Los matchers PUT/PATCH/DELETE de `/api/publicaciones/**` (:48-50) ya cubren `PATCH /{id}/destacado` y `PUT /{id}/fotos/orden`. Los de `/api/agencias/**` POST/DELETE (:47, :49) pueden quedar (el endpoint se elimina, D-07) o recortarse a PUT/PATCH.

### `B/exception/GlobalExceptionHandler.java` (advice)

Analog: si mismo. Convenciones a conservar: `ResponseEntity<Map<String,String>>`, handlers existentes :17-30 (404, 400, BadCredentials 401).

```java
// :14-15 -> extender la base
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
// :32-39 handleValidation actual devuelve mapa por campo; pasa a override de handleMethodArgumentNotValid
// con {"error":"Datos inválidos","campos":{...}} (copiar el bucle FieldError de :35-37)
```
Agregar `AuthenticationException` (401; absorbe el handler de `BadCredentialsException` :27-30 manteniendo "Credenciales inválidas"), `AccessDeniedException` (403), `DataIntegrityViolationException` (409), `Exception` (500 + `@Slf4j`), y override de `handleExceptionInternal`. NO declarar handler para `MaxUploadSizeExceededException` (rompe el arranque; RESEARCH Anti-Patterns). Un solo advice.

### `B/config/DataSeeder.java` (config, batch)

**Analog parcial:** `B/service/AuthService.java` (:30-36 construccion de Usuario con encoder):
```java
Usuario usuario = Usuario.builder()
        .nombre(request.getNombre())
        .email(request.getEmail())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .telefono(request.getTelefono())
        .rol(Rol.COMPRADOR) // ... los admins se cargan aparte
        .build();
```
Para el seed usar `.rol(Rol.ADMIN)`. Estructura del seeder, `@Value("${app.admin.email:}")`, `Environment.acceptsProfiles(Profiles.of("prod"))`: RESEARCH Pattern 4. Agencia sembrada: `Agencia.builder().nombre("Dante Automotores").slug("dante-automotores")` (slug como `AgenciaService.normalizarSlug`, :120-127). Los `app.admin.*` van en `application.yml` bajo `app:` (:23-28) como `${ADMIN_EMAIL:}` SIN defaults con credenciales.

### `B/repository/*` (mods)

Estilo derived-query de una linea (`UsuarioRepository.java:9-10`, `FavoritoRepository.java:10-12`):
```java
boolean existsByRol(Rol rol);                                   // UsuarioRepository
Optional<Agencia> findFirstByOrderByIdAsc();                    // AgenciaRepository
void deleteByPublicacionId(Long publicacionId);                 // FavoritoRepository, ConsultaRepository (requieren @Transactional en el service)
```

### `B/entity/Publicacion.java` y `FotoPublicacion.java`

Analog: `Publicacion.java:67-69` (`@Builder.Default` + Lombok `@Builder`). Agregar:
```java
@Column(nullable = false)
@ColumnDefault("false")      // org.hibernate.annotations.ColumnDefault; obligatorio con ddl-auto: update
@Builder.Default
private boolean destacado = false;
```
`FotoPublicacion` (:23-26): agregar `@Column(name = "public_id") private String publicId;` (nullable).

### `B/dto/publicacion/CambiarDestacadoRequest.java` y `ReordenarFotosRequest.java`

Copiar `CambiarEstadoRequest.java` completo (:7-12):
```java
@Data
public class CambiarDestacadoRequest {
    @NotNull
    private Boolean destacado;
}
```
`ReordenarFotosRequest`: `@NotNull @NotEmpty private List<Long> fotoIds;`. En `PublicacionRequest` quitar `agenciaId` (y su `@NotNull`).

### `B/mapper/PublicacionMapper.java`

Agregar `.destacado(publicacion.isDestacado())` al builder (:31, junto a `.estado(...)`) y el campo en `PublicacionResponse`. Los fotos ya salen ordenadas por `orden` (:33-36): el reorden solo cambia `orden`.

### `B/service/PublicacionService.java` (service, CRUD)

Analog: si mismo. Cambios puntuales:

- `crear` (:55-56) y `actualizar` (:81-82): reemplazar `agenciaRepository.findById(request.getAgenciaId()).orElseThrow(...)` por un helper privado `obtenerAgenciaUnica()` con `findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("No hay una agencia configurada"))`. Builder de crear (:58-72): agregar nada mas para destacado (default false).
- `cambiarEstado` (:101-106) es el molde de `cambiarDestacado`:
```java
Publicacion publicacion = buscarEntidad(id);
publicacion.setEstado(request.getEstado());
publicacionRepository.save(publicacion);
return PublicacionMapper.toResponse(publicacion);
```
- `eliminar` (:108-113): hoy `existsById` + `deleteById`; agregar antes el borrado de favoritos/consultas, `@Transactional`, y destroy post-commit de los `public_id` de las fotos (RESEARCH Pitfalls 5 y 8).
- `agregarFoto` (:115-131): `int siguienteOrden = publicacion.getFotos().size();` (:119) es el bug de orden duplicado; usar `max(orden)+1`. Llamar `ImagenValidator.validar(archivo)` y tope `MAX_FOTOS` ANTES de `cloudinaryService.subirImagen` (:118), y guardar `publicId`.
- `eliminarFoto` (:133-142): conserva la validacion de pertenencia (:137-139, `IllegalArgumentException("La foto no pertenece a esta publicación")`); luego quitar de `publicacion.getFotos()`, resecuenciar 0..n-1 y destroy best-effort.
- `reordenarFotos`: cuerpo en RESEARCH Code Examples.
- `buscar` (:36-48): el default `DISPONIBLE` (:41) se mantiene para el publico; el listado admin va por un metodo nuevo que llama `findAll(PublicacionSpecification.conFiltros(..., null, null))`; verificar en `repository/spec/PublicacionSpecification.java` que `estado == null` no filtre.
- Manejo de errores: `ResourceNotFoundException` (:56, :110, :146) y `IllegalArgumentException` (:138); mensajes en español.

### `B/service/CloudinaryService.java`

Hoy (:18-26) devuelve `String` con `secure_url`:
```java
Map<?, ?> resultado = cloudinary.uploader().upload(archivo.getBytes(),
        ObjectUtils.asMap("folder", "danteautomotores/publicaciones"));
return (String) resultado.get("secure_url");
```
Cambiar el retorno a un record (`url`, `publicId`) leyendo `resultado.get("public_id")`, agregar `"resource_type","image"`, `"allowed_formats","jpg,png,webp"` (RESEARCH Code Examples) y un metodo `eliminar(String publicId)` best-effort (try/catch + `log.warn`, `@Slf4j`). El catch actual de `IOException` (:23-25) lanza `RuntimeException` en ingles: pasarlo a español.

### `B/controller/PublicacionController.java` (controller)

Molde exacto: `:53-56`
```java
@PatchMapping("/{id}/estado")
public ResponseEntity<PublicacionResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
    return ResponseEntity.ok(publicacionService.cambiarEstado(id, request));
}
```
Replicar para `@PatchMapping("/{id}/destacado")` y `@PutMapping("/{id}/fotos/orden")` (con `ReordenarFotosRequest`). Imports con `org.springframework.web.bind.annotation.*` ya cubiertos (:11).

### `B/controller/AdminPublicacionController.java` (nuevo)

Copiar estructura de clase de `PublicacionController.java:17-22` (`@RestController @RequestMapping("/api/admin/publicaciones") @RequiredArgsConstructor`, un `GetMapping` que devuelve `ResponseEntity<List<PublicacionResponse>>`, mismos imports de :3-15).

### `B/controller/AgenciaController.java` y `AgenciaService.java`

Eliminar `crear` (controller :30-33, service :72-85) y `eliminar` (controller :40-44, service :102-107) y, si quedan sin uso, `generarSlugUnico` (:109-118). Dejar `listar`, `obtenerPorSlug`, `actualizar` (:20-28, :35-38). Nota: el seeder necesita `normalizarSlug` (:120-127): extraerlo o duplicar el slug fijo "dante-automotores".

### `application.yml`

Sumar bajo `spring.servlet.multipart` (:19-21) `max-request-size: 12MB` y bajo `server:` `tomcat.max-swallow-size: 50MB` (RESEARCH Pitfall 6); bajo `app:` (:23-28) el bloque `admin:` con `${ADMIN_EMAIL:}` etc.

---

### Frontend

### `F/services/api.js`

Actual (:7-13) solo tiene el interceptor de request:
```js
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) { config.headers.Authorization = `Bearer ${token}` }
  return config
})
```
Reemplazar/extender con RESEARCH Pattern 5 (no Bearer a `/auth/*`, `registrarManejadorSesionVencida`, interceptor de response que ignora `/auth/*`). Estilo del archivo: sin punto y coma, comillas simples, 2 espacios (respetarlo; tambien en `AuthContext.jsx`).

### `F/context/AuthContext.jsx`

`logout` (:44-48) borra token y usuario sincronicamente. Agregar `useEffect` que registre el manejador (importar `registrarManejadorSesionVencida` de `../services/api.js`, :2; `useEffect` de 'react', :1) que llama `logout()` y `navigate('/login', { replace:true, state:{ from, sesionVencida:true } })`. `AuthProvider` ya queda dentro de `BrowserRouter` (por `main.jsx`), asi que `useNavigate` es valido.

### `F/components/ProtectedRoute.jsx`

`:8` `if (!usuario) return <Navigate to="/login" replace />` -> agregar `state={{ from: location.pathname + location.search }}` con `useLocation()`.

### `F/pages/LoginPage.jsx`

`:25` `navigate('/')` -> `navigate(location.state?.from ?? '/')`; `:26-27` el catch es generico ("Email o contraseña incorrectos": mantenerlo, es un 401 de credenciales). Mostrar aviso si `location.state?.sesionVencida`. Patron de estado: `useState('')` para `error` (:13).

### `F/utils/errores.js` (nuevo)

Convencion de util: camelCase `.js` como `utils/whatsapp.js`. Exporta `mensajeDeError(err, fallback)`: `err.response?.data?.error ?? (err.response ? fallback : 'No se pudo conectar o el archivo es demasiado grande')`. Reemplaza los `catch {}` genericos: `AdminDashboardPage.jsx:50-51, 82-83`, `AdminPublicacionFormPage.jsx:112, 148-149, 190-191`.

### `F/components/ConfirmDialog.jsx` (nuevo)

Convencion: `export default function`, props desestructuradas, Tailwind con `bg-bronze`, `bg-navy`, `text-navy-dark`, `rounded-xl border border-slate-200` (ver `Seccion`/`Input` en `AdminPublicacionFormPage.jsx:482-516`). Usar `<dialog>` nativo con `showModal()` (RESEARCH). Usar en `eliminarPublicacion` (hoy `onClick` en `AdminDashboardPage.jsx:~275`) y en `eliminarFoto` (`AdminPublicacionFormPage.jsx:~198`).

### `F/pages/admin/AdminDashboardPage.jsx`

- `cargar` (:30-36): cambiar `api.get('/publicaciones', { params: { estado: 'DISPONIBLE' } })` por `api.get('/admin/publicaciones')`; reemplazar `.catch(() => {})` por mostrar error.
- Agencia: quitar `crearAgencia` (:43-52), `eliminarAgencia` (:55-57), el boton "Nueva agencia" (:117) y "Eliminar agencia" (:225); conservar `agenciaEditando` + `api.put('/agencias/${id}', datos)` (:60-84) y `CampoAgencia` (:296-306). Mostrar solo la primera agencia.
- Listado (:238-292): el `<li>` con `select` de estado (:264-274) es el molde para agregar un toggle de destacado (`Star` de lucide-react, mismo patron de boton de icono que :276-291, `onClick` -> `api.patch('/publicaciones/${id}/destacado', { destacado })`), y filtro por estado + busqueda arriba de la lista (client-side, A7).

### `F/pages/admin/AdminPublicacionFormPage.jsx`

- Quitar `agenciaId` de `EMPTY` (:53), de `desdePublicacion` (:68), de `agencias` state (:85) / `api.get('/agencias')` (:98), de `agenciaId: Number(form.agenciaId)` (:134) y el `Select` (:276-288).
- `subirFotos` (:160-196): bucle `for` con un unico try/catch (:178-192). Reestructurar con try/catch POR archivo + pre-validacion (JPEG/PNG/WebP, <=10 MB) acumulando mensajes; `accept="image/jpeg,image/png,image/webp"` (:~503, hoy `image/*`).
- `FotosGrid` (:448-496): agregar botones "mover izquierda/derecha" y "hacer portada" junto al boton de eliminar (:458-465) -> `api.put('/publicaciones/${id}/fotos/orden', { fotoIds })` y `setPublicacion(data)`. Usar `ChevronLeft/ChevronRight` (lucide-react). Boton visible tambien en mobile (el actual usa `opacity-0 group-hover:opacity-100`, inviable en touch).

## Shared Patterns

### Formato de error `{"error": "mensaje"}`
**Source:** `B/exception/GlobalExceptionHandler.java:17-30`; aplicar a advice, entry point, access denied handler. En el front, `utils/errores.js` lee `err.response.data.error`.

### Excepciones de negocio
**Source:** `ResourceNotFoundException` (404) y `IllegalArgumentException` (400), p. ej. `PublicacionService.java:137-139`. Usar para `ImagenValidator`, tope de fotos, reorden invalido. Mensajes en español.

### Controller delgado + service que devuelve DTO
**Source:** `PublicacionController.java:43-56`: `@Valid @RequestBody`, `ResponseEntity.ok(service.metodo(...))`; el service devuelve `PublicacionMapper.toResponse(...)`.

### Lombok / DI
**Source:** `@RequiredArgsConstructor` con campos `private final` (`PublicacionService.java:26-34`); entidades con `@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor`.

### Autorizacion por matcher, no por anotacion
**Source:** `SecurityConfig.java:43-53`. El orden importa (primer match gana); lo admin nuevo bajo `/api/admin/**` va ANTES de `anyRequest()`.

### Front: estado local de error
**Source:** `AdminPublicacionFormPage.jsx:88, 123-127` (`useState('')`, `setError('')` al empezar). Sin error boundary global.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `B/service/ImagenValidator.java` | utility | transform | No hay validacion de uploads ni magic bytes; usar RESEARCH Code Examples |
| `B/security/Rest*Handler` (logica de escritura de response) | middleware | request-response | No hay handlers de Security previos; usar RESEARCH Pattern 2 |
| Todos los tests (`src/test` no existe) | test | - | Usar RESEARCH "Validation Architecture" (`@WebMvcTest` + `@MockBean`, Mockito); nombres y rutas en "Wave 0 Gaps". Crear tambien `src/test/java/com/danteautomotores/` |
| Front tests | test | - | Sin runner; gate = `npm run build` + UAT manual |

## Metadata

**Analog search scope:** `danteautomotores-back/src/main` (55 archivos tracked, todos leidos o listados), `danteautomotores-front/src/{services,context,components,pages,utils}`.
**Pattern extraction date:** 2026-10-02
