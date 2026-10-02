# Phase 1: Gestión del inventario por el admin - Context

**Gathered:** 2026-10-02
**Status:** Ready for planning

<domain>
## Phase Boundary

La única cuenta admin gestiona el inventario completo de autos (datos, fotos, estado y destacados) desde el panel del front, contra la API real, con errores manejados de forma uniforme. Cubre ADM-01..ADM-05 y PROD-01.

Incluye: seed de la cuenta admin, consolidación a una sola agencia, validación y reorden de fotos, flag de destacado, listado del panel con todos los estados, manejo global de errores (`@RestControllerAdvice` + filtro JWT que devuelve 401) y manejo del 401 en el front.

No incluye: catálogo público sin mocks, paginación pública, deploy a producción y Flyway (Fase 2), recuperación de contraseña (Fase 3), métricas del admin (Fase 5).

</domain>

<decisions>
## Implementation Decisions

### Cuenta admin
- **D-01:** El admin se crea al arrancar la app a partir de variables de entorno (`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOMBRE`; los nombres exactos los define el planner). Se crea **solo si no existe ningún usuario con rol ADMIN**. La contraseña nunca queda en el repo (sin defaults con credenciales reales en `application.yml`).
- **D-02:** Si el admin ya existe, el seed **no lo modifica**: no sincroniza la contraseña ni otros datos aunque cambien las variables. Cambiar la contraseña queda para otro mecanismo (la recuperación de la Fase 3).
- **D-03:** Si faltan las variables del admin y no hay ningún ADMIN en la base, el planner decide entre avisar en el log y fallar al arrancar. Se prefiere fallar en prod y avisar en dev.
- **D-04:** No existe ningún endpoint que cree o promueva admins. `AuthService.registrar` sigue forzando `Rol.COMPRADOR`. Los usuarios ADMIN que ya estén en la base de dev se dejan como están, sin chequeo de unicidad al arrancar. El modelo `Rol` sigue admitiendo varios admins a futuro (ADM-V2-01).

### Agencia única
- **D-05:** Hay una **sola agencia fija (Dante Automotores)**, sembrada al arrancar si no existe ninguna. La tabla y la entidad `Agencia` se mantienen, así que se puede volver a varias más adelante. — **Reversibility:** reversible — la FK `publicaciones.agencia_id` se conserva.
- **D-06:** Los autos se asocian automáticamente a esa agencia: el backend la asigna en `crear`/`actualizar` y deja de exigir `agenciaId` en `PublicacionRequest`. El form de publicación del front deja de mostrar el selector de agencia.
- **D-07:** En el panel, el admin **edita** los datos de la agencia (nombre, contacto, dirección, logo, descripción) pero **no crea ni borra** agencias. Se quitan del panel las acciones "Nueva agencia" y "Eliminar", y el backend deja de exponer la creación y la eliminación (las quita o las bloquea). Los GET públicos de agencia siguen como están, porque la página "Agencia" los usa en la Fase 2.

### Claude's Discretion
El usuario eligió no discutir estas áreas. El planner define el detalle con estos defaults razonables:

- **Fotos (ADM-03):** validar en el **backend** el tipo real (JPEG/PNG/WebP, revisando content-type y, si se puede, magic bytes) y el tamaño (≤10MB, como el multipart actual). El tope de 10 fotos por auto se valida también en el backend, no solo en el front (`MAX_FOTOS = 10`). Los rechazos llevan un mensaje claro en español. El reorden se guarda en el backend con un endpoint que recibe el orden completo de ids; la UI puede usar drag & drop o flechas, lo que sea más simple y usable en mobile. La foto con orden 0 es la portada. Al eliminar una foto (o una publicación), se intenta borrar el asset en Cloudinary (guardando el `public_id`); si falla, no se bloquea la operación.
- **Destacados (ADM-05):** campo booleano `destacado` en `Publicacion`, con un endpoint dedicado tipo PATCH (como el de estado) y un toggle en el listado del panel. Sin tope obligatorio de destacados. Que un auto vendido pueda seguir destacado o no queda a criterio del planner (el catálogo público de la Fase 2 decide cómo mostrarlo).
- **Listado del panel (ADM-04):** el panel muestra **todos** los estados (hoy solo trae `DISPONIBLE`), con filtro por estado y búsqueda simple. El cambio de estado sigue en línea con el select actual. Eliminar una publicación pide confirmación dentro de la UI (no `window.confirm` si se puede evitar).
- **Errores (PROD-01):** formato uniforme `{"error": "mensaje"}`. Para validaciones se puede sumar un mapa `campos`, pero el planner decide si conserva compatibilidad con el formato actual por campo. El filtro JWT captura los tokens malformados o vencidos y deja que la request siga sin autenticar o responde 401, nunca 500. Se agregan un `AuthenticationEntryPoint` y un `AccessDeniedHandler` con el mismo formato (401/403), y un handler genérico para 500 sin stacktrace. En el front, un interceptor de respuesta de axios ante un 401 limpia la sesión y redirige a `/login`, idealmente con un aviso de "sesión vencida" y volviendo después a la página donde estaba. Los forms del panel muestran el mensaje `error` del backend en lugar de textos genéricos.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Alcance y requisitos
- `.planning/ROADMAP.md` §"Phase 1" — objetivo, criterios de éxito y notas de la fase
- `.planning/REQUIREMENTS.md` — ADM-01..ADM-05, PROD-01
- `.planning/PROJECT.md` — restricciones (una sola cuenta admin, extensible a futuro; Cloudinary)

### Estado actual del código
- `.planning/codebase/CONCERNS.md` — deuda conocida: filtro JWT con 500, uploads sin validar, etc.
- `.planning/codebase/ARCHITECTURE.md` — capas y flujo de auth
- `.planning/codebase/CONVENTIONS.md` — convenciones de naming y errores

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `exception/GlobalExceptionHandler.java`: ya existe un `@RestControllerAdvice` con 404/400/401/validación. Se extiende en lugar de crear otro.
- `service/CloudinaryService.java`: `subirImagen` sube a la carpeta `danteautomotores/publicaciones` y devuelve solo `secure_url`. Para poder borrar hay que guardar también el `public_id`.
- `entity/FotoPublicacion.java`: ya tiene el campo `orden`. Hoy `agregarFoto` asigna `orden = fotos.size()`.
- `controller/PublicacionController.java`: `PATCH /{id}/estado` sirve de patrón para el endpoint de destacado.
- Front `pages/admin/AdminPublicacionFormPage.jsx` (516 líneas): ya tiene `FotosGrid`, subida múltiple, `MAX_FOTOS = 10` y `accept="image/*"`.
- Front `pages/admin/AdminDashboardPage.jsx` (312 líneas): CRUD de agencias (que pasa a ser solo edición) y listado de publicaciones con select de estado.

### Established Patterns
- `@PrePersist` para fechas, Lombok `@Builder`, mappers estáticos y excepciones (`ResourceNotFoundException` / `IllegalArgumentException`) traducidas por el handler global.
- El esquema todavía usa `ddl-auto: update`, así que en esta fase los cambios (`destacado`, `public_id`) se aplican así. La Fase 2 crea la línea base de Flyway que los incluye.
- Front: `services/api.js` es una instancia axios con un interceptor de request (el token sale de localStorage). Ahí se suma el interceptor de response para el 401.

### Integration Points
- `security/JwtAuthenticationFilter.java`: `jwtService.extractUsername` lanza excepciones con tokens malformados o vencidos, y eso es el origen del 500.
- `config/SecurityConfig.java`: reglas de autorización (los endpoints de escritura de publicaciones y agencias son solo para ADMIN) y el lugar para el entry point o el access denied handler.
- `service/AuthService.java`: el registro fuerza COMPRADOR. El seed del admin va en un componente de arranque (`ApplicationRunner` o `CommandLineRunner`).
- `service/PublicacionService.java`: hoy `crear`/`actualizar` buscan la agencia por `request.getAgenciaId()` y pasan a usar la agencia única.
- Front `components/ProtectedRoute.jsx` y `context/AuthContext.jsx`: la redirección a login y el logout.

</code_context>

<specifics>
## Specific Ideas

- El negocio es una sola agencia (Dante Automotores). El panel no debe sugerir que hay varias.
- El admin es la única persona que carga inventario, así que la UX del panel prioriza que sea simple y no requiera configuración.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-gestion-del-inventario-por-el-admin*
*Context gathered: 2026-10-02*
