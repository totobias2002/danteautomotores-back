# Phase 2: Catálogo público real en producción - Pattern Map

**Mapped:** 2026-10-03
**Files analyzed:** 44 (back: `src/main/java/com/danteautomotores/` = `B/`, tests `src/test/java/com/danteautomotores/` = `BT/`; front: `C:/Users/toto/Desktop/work/danteautomotores-front/src/` = `F/`)
**Analogs found:** 38 / 44 (todos los paths de back son tracked por git, verificado con `git ls-files`; los del front viven en su propio repo y son los archivos fuente de `src/`)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `src/main/resources/db/migration/V1__esquema_original.sql` (new) | migration | batch | RESEARCH Pattern 1 (verificado); sin analog en repo | none |
| `.../db/migration/V2__fase1_destacado_y_public_id.sql` (new) | migration | batch | RESEARCH Pattern 1 | none |
| `.../db/migration/V3__fase2_catalogo.sql` (new) | migration | batch | RESEARCH Pattern 1 | none |
| `pom.xml` (mod: flyway-core + flyway-database-postgresql) | config | - | dependencias existentes del propio pom | exact |
| `src/main/resources/application.yml` (mod: validate, show-sql false, flyway, batch fetch, actuator, `app.catalogo`) | config | - | si mismo | exact |
| `B/enums/TipoCarroceria.java`, `B/enums/ZonaAgencia.java`, `B/enums/OrdenCatalogo.java` (new) | model (enum) | - | `B/enums/Transmision.java` | exact |
| `B/entity/Publicacion.java` (mod: `tipoCarroceria`, `precioAnterior`, `fechaVendido`) | model | CRUD | si mismo (`transmision`, `precio`, `fechaPublicacion`) | exact |
| `B/entity/Agencia.java` (mod: `zona`) | model | CRUD | `Publicacion.transmision` (`@Enumerated`) | role-match |
| `B/dto/publicacion/PublicacionRequest.java` (mod) | model (DTO) | request-response | si mismo (`precio`, `transmision`) | exact |
| `B/dto/publicacion/PublicacionResponse.java` + `B/mapper/PublicacionMapper.java` (mod) | DTO/mapper | transform | si mismos | exact |
| `B/dto/publicacion/PublicacionResumenResponse.java` (new) | model (DTO) | transform | `B/dto/publicacion/PublicacionResponse.java` | exact |
| `B/dto/publicacion/PaginaResponse.java` (new, record) | model (DTO) | transform | RESEARCH Code Examples; el repo no usa records | none |
| `B/dto/publicacion/FacetasResponse.java` (new) | model (DTO) | transform | `PublicacionResponse.java` (`@Data @Builder`) | role-match |
| `B/dto/agencia/AgenciaRequest.java`, `AgenciaResponse.java`, `B/mapper/AgenciaMapper.java`, `B/service/AgenciaService.java` (mod: `zona`) | DTO/mapper/service | CRUD | si mismos | exact |
| `B/repository/spec/CatalogoSpecification.java` (new) | utility (Specification) | request-response | `B/repository/spec/PublicacionSpecification.java` | exact |
| `B/service/CatalogoService.java` (new) | service | request-response | `B/service/PublicacionService.java` (`buscar`, `@Transactional(readOnly = true)`) | role-match |
| `B/config/ClockConfig.java` (new) | config | - | `B/config/CloudinaryConfig.java` (`@Configuration` + `@Bean`) | role-match |
| `B/service/PublicacionService.java` (mod: `crear`/`actualizar` copian campos, `cambiarEstado` fija `fechaVendido`, se retira `buscar`) | service | CRUD | si mismo | exact |
| `B/service/ConsultaService.java` (mod: 400 si VENDIDO) | service | CRUD | `ReglaDeNegocioException` en `PublicacionService` | role-match |
| `B/controller/PublicacionController.java` (mod: `GET` paginado, `/facetas`, `/destacados`, `/{id}/similares`) | controller | request-response | si mismo (`buscar`) | exact |
| `B/config/SecurityConfig.java` (mod: `/actuator/health` público) | config | request-response | si mismo (`.requestMatchers(... permitAll())`) | exact |
| `B/config/SecretosGuard.java` (mod: Cloudinary obligatorio) | config (arranque) | event-driven | si mismo | exact |
| `B/config/DataSeeder.java` (mod: `trim().toLowerCase()` de `ADMIN_EMAIL`) | config (arranque) | batch | si mismo | exact |
| `BT/support/PostgresLocalTestBase.java` + `BT/migration/MigracionesPostgresTest.java` (new) | test | batch | `BT/support/SeguridadWebMvcTestBase.java` (base abstracta) + RESEARCH Code Examples | partial |
| `BT/repository/CatalogoSpecificationTest.java` (new, Postgres local) | test | request-response | `PostgresLocalTestBase` (nuevo) | partial |
| `BT/service/CatalogoServiceTest.java`, `PublicacionServiceTest.java` (mod) | test | request-response | `BT/service/PublicacionServiceTest.java` (Mockito) | exact |
| `BT/controller/PublicacionControllerTest.java` (mod/ext) | test | request-response | si mismo (`SeguridadWebMvcTestBase`, `mvc.perform`) | exact |
| `BT/config/SecretosGuardTest.java` (mod) | test | event-driven | si mismo (helper `guard(...)`) | exact |
| `scripts/demo/sembrar-demo.js` (mod: tipo, zona, precioAnterior, guardas) | script | batch | si mismo | exact |
| `.gitattributes`, `.gitignore` (mod: `*.dump`), `README.md` (mod, IN-07) | config/doc | - | - | none |
| `F/utils/catalogoParams.js` + `.test.js` (new) | utility | transform | `F/utils/errores.js` (utilidad pura, export nombrado) | role-match |
| `F/utils/etiquetas.js` (new) | utility | transform | `ESTADO_BADGE` en `F/components/PublicacionCard.jsx:3-6` | role-match |
| `F/utils/cloudinary.js` (mod: constantes card/detalle) | utility | transform | si mismo (`TRANSFORMACION_MINIATURA`) | exact |
| `F/components/PublicacionCard.jsx` (mod) | component | request-response | si mismo | exact |
| `F/components/Paginador.jsx` (new) | component | event-driven | `F/components/ConfirmDialog.jsx` / botones de `AutosPage.jsx` | partial |
| `F/pages/AutosPage.jsx` (mod, 573 l.) | page | request-response | si mismo + `AgenciaPage.jsx` (fetch con cargando/error) | role-match |
| `F/pages/HomePage.jsx`, `AgenciaPage.jsx`, `PublicacionDetallePage.jsx` (mod, sin mocks) | page | request-response | `AgenciaPage.jsx` (rama no-mock) | role-match |
| `F/pages/CreditosPage.jsx` + `F/data/creditosFotos.js` (new) | page/data | request-response | `F/pages/AgenciaPage.jsx` (layout `main`) | partial |
| `F/components/Footer.jsx`, `F/routes/AppRouter.jsx` (mod) | component/route | - | si mismos | exact |
| `F/pages/admin/AdminPublicacionFormPage.jsx` (mod) | page | CRUD | si mismo (`FORM_INICIAL`, `publicacionAForm`, payload) | exact |
| `F/pages/admin/AdminDashboardPage.jsx` (mod: zona de agencia) | page | CRUD | si mismo (`AGENCIA_INICIAL`, `comenzarEdicionAgencia`) | exact |
| `F/mocks/{catalogoMock,homeMock,agenciaMock}.js` (delete) | - | - | - | n/a |

## Pattern Assignments

### Migraciones `db/migration/V1..V3` (migration, batch)

**Analog:** ninguno en el repo (hoy el esquema lo crea `ddl-auto: update`). Copiar **literal** el SQL verificado de `02-RESEARCH.md` §Pattern 1 (V1 completo, V2 con `ADD COLUMN IF NOT EXISTS`, V3 con columnas + `UPDATE fecha_vendido` + 5 índices). Reglas: comentarios en ASCII, nombres de constraints idénticos a los de Hibernate, sin `CHECK` en `tipo_carroceria`/`zona`, nunca editar una migración aplicada, no regenerar V1 desde `pg_dump` (trampas `\restrict` y `set_config('search_path')`).

---

### `src/main/resources/application.yml` (config)

**Analog:** si mismo, líneas 15-24 (`ddl-auto: update` y `show-sql: true` hoy en el base).

**Cambio** (base seguro, valores de desarrollo en perfil `dev` opcional):
```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        default_batch_fetch_size: 50
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
management:
  endpoints.web.exposure.include: health
  endpoint.health.show-details: never
app:
  catalogo:
    dias-vendido-visible: 30
```
Mantener el estilo de variables de entorno `${NOMBRE:default}` de las líneas 12-14 y 33-36.

---

### `B/enums/TipoCarroceria.java`, `ZonaAgencia.java`, `OrdenCatalogo.java` (enum)

**Analog:** `B/enums/Transmision.java` (1-6)
```java
package com.danteautomotores.enums;

public enum Transmision {
    MANUAL,
    AUTOMATICA
}
```
Valores: `TipoCarroceria` = SEDAN, HATCHBACK, SUV, PICKUP, UTILITARIO, COUPE, MONOVOLUMEN, FAMILIAR; `ZonaAgencia` = CABA, ZONA_NORTE, ZONA_SUR, ZONA_OESTE, INTERIOR; `OrdenCatalogo` = RELEVANCIA, PRECIO_ASC, PRECIO_DESC, ANIO_DESC, KM_ASC (con método estático `desde(String)` que cae a RELEVANCIA).

---

### `B/entity/Publicacion.java` y `B/entity/Agencia.java` (model, CRUD)

**Analog:** `Publicacion.java` lineas 46-63 y 79-80.

**Excerpts a imitar:**
```java
@Column(nullable = false, precision = 12, scale = 2)
private BigDecimal precio;

@Enumerated(EnumType.STRING)
private Transmision transmision;

@Column(name = "fecha_publicacion")
private LocalDateTime fechaPublicacion;
```
Nuevos (nombres de columna = V3): `@Enumerated(EnumType.STRING) private TipoCarroceria tipoCarroceria;` (`tipo_carroceria` por la naming strategy de Boot), `@Column(name = "precio_anterior", precision = 12, scale = 2) private BigDecimal precioAnterior;`, `@Column(name = "fecha_vendido") private LocalDateTime fechaVendido;`. En `Agencia`: `@Enumerated(EnumType.STRING) private ZonaAgencia zona;`. Con `ddl-auto: validate` el tipo debe calzar con V3 (`varchar(255)`, `numeric(12,2)`, `timestamp(6)`). Nada de `@ColumnDefault` (las columnas nuevas son nullable).

---

### `B/dto/publicacion/PublicacionRequest.java` (DTO entrada)

**Analog:** si mismo, lineas 37-48.
```java
@NotNull(message = "El precio es obligatorio")
@Positive(message = "El precio tiene que ser mayor a cero")
@Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 dígitos enteros y 2 decimales")
private BigDecimal precio;
...
private Transmision transmision;
```
Nuevos: `private TipoCarroceria tipoCarroceria;` (opcional) y `precioAnterior` con `@Positive` + `@Digits(integer = 10, fraction = 2, ...)` **sin** `@NotNull`, mensajes en español. No rechazar `precioAnterior <= precio` (D-03). Extender `BT/dto/PublicacionRequestValidationTest.java`.

---

### `B/dto/publicacion/PublicacionResponse.java` + `B/mapper/PublicacionMapper.java` (transform)

**Analog:** si mismos. DTO (lineas 16-40: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`); mapper (14-39, builder encadenado).

**Agregar al builder** (junto a `.destacado(...)`, linea 32):
```java
.tipoCarroceria(publicacion.getTipoCarroceria())
.precioAnterior(publicacion.getPrecioAnterior())
.oferta(esOferta(publicacion))                  // precioAnterior != null && precioAnterior.compareTo(precio) > 0
.agenciaZona(publicacion.getAgencia().getZona())
.fechaVendido(publicacion.getFechaVendido())
```
El helper `esOferta` va como `public static` en el mapper (el resumen lo reutiliza); la regla es única (D-03). Mantener constructor privado y métodos estáticos (convención del proyecto).

---

### `B/dto/publicacion/PublicacionResumenResponse.java` (DTO, transform)

**Analog:** `PublicacionResponse.java` (anotaciones Lombok y estilo). Campos del JSON de RESEARCH §Pattern 2 (sin `descripcion` ni lista de fotos; `fotoPortada` = foto de menor `orden`). Mapper nuevo `toResumen` en `PublicacionMapper`, reusando el criterio de orden de fotos de la linea 35: `Comparator.comparing(f -> f.getOrden() == null ? 0 : f.getOrden())` con `.min(...)`.

---

### `B/dto/publicacion/PaginaResponse.java` (record) y `FacetasResponse.java`

**Analog:** ninguno de record en el repo; usar el código de RESEARCH "PaginaResponse" (`de(Page<T>)`, página base 1). `FacetasResponse`: `@Data @Builder @NoArgsConstructor @AllArgsConstructor` como `PublicacionResponse`, con clases internas estáticas para `Conteo`, `Rango`, `Histograma`.

---

### `B/repository/spec/CatalogoSpecification.java` (Specification, request-response)

**Analog:** `B/repository/spec/PublicacionSpecification.java` (1-49)

**Estructura a copiar** (clase utilitaria, constructor privado, lambdas `(root, query, cb)`, acumulando `Predicate`):
```java
public class PublicacionSpecification {
    private PublicacionSpecification() { }

    public static Specification<Publicacion> conFiltros(...) {
        return (root, query, cb) -> {
            Predicate predicado = cb.conjunction();
            if (anioMin != null) {
                predicado = cb.and(predicado, cb.greaterThanOrEqualTo(root.get("anio"), anioMin));
            }
            if (agenciaId != null) {
                predicado = cb.and(predicado, cb.equal(root.get("agencia").get("id"), agenciaId));
            }
            return predicado;
        };
    }
}
```
Agregar: `visible(LocalDateTime limite)` y `conOrden(OrdenCatalogo)` (código verificado en RESEARCH §Pattern 2, con guarda `query.getResultType() != Long.class` para el count), filtros multivalor `cb.lower(root.get("marca")).in(...)`, `zona` vía `root.get("agencia").get("zona")`, `ofertas` con `cb.greaterThan(root.<BigDecimal>get("precioAnterior"), root.<BigDecimal>get("precio"))`, búsqueda por tokens con `cb.like(expr, patron, '\\')` y escape de `%_\`. Decisión: la `PublicacionSpecification` vieja se elimina o queda solo si `AdminPublicacionController`/otros la usan (verificar con grep antes de borrar).

---

### `B/service/CatalogoService.java` (service, request-response)

**Analog:** `B/service/PublicacionService.java` (`buscar`, líneas 64-77, y cabecera 45-58).

**Imports/cabecera a imitar:**
```java
@Service
@Transactional
@RequiredArgsConstructor
public class CatalogoService {
    private final PublicacionRepository publicacionRepository;
    private final Clock clock;
    @Value("${app.catalogo.dias-vendido-visible:30}") private int diasVendidoVisible;
```
**Patrón de lectura** (lineas 64-76): `@Transactional(readOnly = true)`, `publicacionRepository.findAll(spec, PageRequest.of(pagina - 1, 24))`, `.map(PublicacionMapper::toResumen)` **dentro** de la transacción (`open-in-view: false`, lazy `agencia`/`fotos`). Clampar parámetros en el servicio (pagina >= 1, listas <= 20, busqueda <= 60, orden desconocido a RELEVANCIA), nunca aceptar `Pageable`/`sort`. Errores: `ResourceNotFoundException` para `similares` de id inexistente (como `buscarEntidad`, linea 328-331).

---

### `B/config/ClockConfig.java` (config)

**Analog:** `B/config/CloudinaryConfig.java` (`@Configuration` + `@Bean`; nombre `*Config.java`).
```java
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() { return Clock.systemDefaultZone(); }
}
```

---

### `B/service/PublicacionService.java` (mod)

**Analog:** si mismo.

**`crear`/`actualizar`** (lineas 99-113 y 125-136): agregar `.tipoCarroceria(request.getTipoCarroceria())` y `.precioAnterior(request.getPrecioAnterior())` al builder y los dos `setX` equivalentes en `actualizar` (asigna campo por campo; el `PUT` reemplaza todo).

**`cambiarEstado`** (lineas 142-147) pasa a:
```java
EstadoPublicacion anterior = publicacion.getEstado();
publicacion.setEstado(request.getEstado());
if (request.getEstado() == EstadoPublicacion.VENDIDO && anterior != EstadoPublicacion.VENDIDO) {
    publicacion.setFechaVendido(LocalDateTime.now(clock));
} else if (request.getEstado() != EstadoPublicacion.VENDIDO) {
    publicacion.setFechaVendido(null);
}
```
Inyectar `Clock` (el `@RequiredArgsConstructor` lo toma del campo `final`; actualizar el setup de `BT/service/PublicacionServiceTest.java` que usa `@InjectMocks`, agregando `@Mock`/`Clock.fixed`). `buscar` (64-77) se retira: lo reemplaza `CatalogoService`; `listarParaAdmin` y `obtenerPorId` (cualquier estado, D-06) no cambian.

---

### `B/service/ConsultaService.java` (mod: VENDIDO responde 400)

**Analog:** `PublicacionService.java:204` y `:261` para `throw new ReglaDeNegocioException("...")` (mapeado a 400 por `GlobalExceptionHandler`). Mensaje: "Este auto ya se vendió".

---

### `B/controller/PublicacionController.java` (controller, request-response)

**Analog:** si mismo (lineas 19-43).

**Patrón actual:**
```java
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {
    @GetMapping
    public ResponseEntity<List<PublicacionResponse>> buscar(
            @RequestParam(required = false) String marca, ...
            @RequestParam(required = false) Long agenciaId
    ) { return ResponseEntity.ok(...); }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponse> obtenerPorId(@PathVariable Long id) { ... }
```
Cambios: `buscar` devuelve `ResponseEntity<PaginaResponse<PublicacionResumenResponse>>` con `@RequestParam(required = false) List<String> marca`, `List<TransmisionEnum>`, `Integer pagina`, `String orden`, `Boolean ofertas`, etc. (enum inválido da 400 uniforme por el handler existente). Agregar `@GetMapping("/facetas")`, `@GetMapping("/destacados")` (`limite` clamp 1..12) y `@GetMapping("/{id}/similares")` (clamp 1..8), delegando a `CatalogoService`. Mantener el mismo estilo `ResponseEntity.ok(...)`. Seguridad: ya cubierto por `SecurityConfig.java:51` (`GET /api/publicaciones/**` permitAll).

---

### `B/config/SecurityConfig.java` (mod: healthcheck)

**Analog:** si mismo, lineas 48-58. Insertar antes de `anyRequest()`:
```java
.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
```
Test: extender `BT/security/SeguridadErroresTest.java` / un test con `SeguridadWebMvcTestBase` verificando que `get("/actuator/health")` no devuelve 401.

---

### `B/config/SecretosGuard.java` (mod: Cloudinary obligatorio, PROD-02)

**Analog:** si mismo (campos `@Value` líneas 39-43, `afterPropertiesSet` 46-58, `fallarOAvisar` 60-66).
```java
@Value("${app.jwt.secret:}")
private String jwtSecret;
...
if (DB_PASSWORD_POR_DEFECTO.equals(dbPassword)) {
    fallarOAvisar("SPRING_DATASOURCE_PASSWORD es la contraseña de desarrollo del repo: definí una contraseña propia");
}
```
Agregar `@Value("${cloudinary.cloud-name:}")`, `cloudinary.api-key`, `cloudinary.api-secret` y un `if` con `isBlank()` que llame `fallarOAvisar("CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET faltan: ...")`. Sin llamada de red. Actualizar el Javadoc de la clase.

**Test:** `BT/config/SecretosGuardTest.java` lineas 22-31 (helper `guard(...)` con `ReflectionTestUtils.setField`) y 33-58 (`assertThatThrownBy(...).isInstanceOf(IllegalStateException.class).hasMessageContaining(...)`). Ojo: el helper actual no setea Cloudinary, así que los tests existentes de `prod` ok deben pasar a setear los tres campos.

---

### `B/config/DataSeeder.java` (mod, Pitfall 10)

**Analog:** si mismo; `@Value("${app.admin.email:}")` en línea 35 y uso en 70-93 (`.email(adminEmail)`). Normalizar una vez: `String adminEmail = this.adminEmail.trim().toLowerCase();` antes de validar/usar. Cubrir en `BT/config/DataSeederTest.java`.

---

### `BT/support/PostgresLocalTestBase.java`, `MigracionesPostgresTest`, tests de spec (test)

**Analog parcial:** `BT/support/SeguridadWebMvcTestBase.java` (base abstracta compartida). El resto no tiene analog: copiar el esqueleto de RESEARCH "Test de migración sobre el Postgres local" (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)`, base `test_<random>` en `localhost:5433`, `Assumptions` salvo `-Ddante.pg.required=true`). No usar Testcontainers ni H2.

### `BT/controller/PublicacionControllerTest.java` (ext)

**Analog:** si mismo, líneas 26-46.
```java
@WebMvcTest(PublicacionController.class)
class PublicacionControllerTest extends SeguridadWebMvcTestBase {
    @MockBean private PublicacionService publicacionService;   // sumar @MockBean CatalogoService

    mvc.perform(patch("/api/publicaciones/1/destacado")
            .header("Authorization", bearerPara(ADMIN, "ADMIN"))
            ...)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.destacado").value(true));
```
Casos nuevos: `GET /api/publicaciones?pagina=2&marca=A&marca=B` sin token (200, forma `contenido/pagina/tamanio/totalElementos/totalPaginas`), `/facetas` y `/destacados` no se confunden con `/{id}`, enum inválido da 400 con `$.error`.

---

### `scripts/demo/sembrar-demo.js` (script, batch)

**Analog:** si mismo (lineas 11, 34-44). Constante `AGENCIAS` (línea 11) suma `zona`; el `POST /publicaciones` (línea 43) suma `tipoCarroceria`/`precioAnterior`; agencias existentes (línea 41: `existentes.find`) hacen `PUT` para completar `zona`. Guardas de Pitfall 11 junto al `LIMPIAR` de línea 35: negarse si `API` no es localhost sin `CONFIRMAR_BORRADO_EN_PRODUCCION=SI`, abortar si ya existe `marca+modelo+anio` salvo `FORZAR=1`. Datos sugeridos por agencia/auto en RESEARCH §"Valores propuestos". Estilo del archivo: comillas dobles y `;` (distinto del front).

---

## Front (repo `danteautomotores-front`)

### `F/utils/catalogoParams.js` + `catalogoParams.test.js` (utility, transform)

**Analog:** `F/utils/errores.js` (1-22): módulo de funciones puras con `export function`, comentarios en español explicando el porqué. Código base en RESEARCH "Parseo de filtros del front" (`leerFiltros`, `aSearchParams`). El test usa `node:test` + `assert/strict` (`node --test src/utils/`), sin vitest. Estilo del front: 2 espacios, comillas simples, **sin punto y coma** en archivos nuevos (como `PublicacionCard.jsx`).

### `F/utils/etiquetas.js` (utility)

**Analog:** `F/components/PublicacionCard.jsx:3-6`
```js
const ESTADO_BADGE = {
  RESERVADO: { texto: 'Reservado', clase: 'bg-amber-100 text-amber-800' },
  VENDIDO: { texto: 'Vendido', clase: 'bg-slate-200 text-slate-700' },
}
```
Mapas constantes en MAYÚSCULAS por enum (TRANSMISION, TIPO_CARROCERIA, ZONA, ESTADO) y exportarlos nombrados; card, detalle, filtros y forms los importan (ya no se duplican).

### `F/utils/cloudinary.js` (mod)

**Analog:** si mismo, línea 4 y 10.
```js
export const TRANSFORMACION_MINIATURA = 'c_fill,w_160,h_120,q_auto,f_auto'
export function urlMiniatura(url, transformacion = TRANSFORMACION_MINIATURA) {
```
Agregar `TRANSFORMACION_CARD = 'c_fill,w_640,h_420,q_auto,f_auto'` y `TRANSFORMACION_DETALLE = 'c_limit,w_1280,q_auto,f_auto'`; usar `urlMiniatura(url, TRANSFORMACION_CARD)`.

### `F/components/PublicacionCard.jsx` (mod)

**Analog:** si mismo. Cambios puntuales:
- Línea 11: `publicacion.fotos?.[0]?.url` pasa a `urlMiniatura(publicacion.fotoPortada, TRANSFORMACION_CARD)` (+ `loading="lazy"`).
- Línea 17: `publicacion.mecanica` pasa a `ETIQUETA_TRANSMISION[publicacion.transmision]`.
- Líneas 75-79: `publicacion.ubicacion` pasa a `ETIQUETA_ZONA[publicacion.zona] || publicacion.agenciaNombre`.
- Línea 72: `$ {formatoNumero(publicacion.precio)}` suma símbolo por `moneda` (`US$`/`$`) y, si `publicacion.oferta`, `<span className="line-through text-slate-400">` con `precioAnterior`.
- `ESTADO_BADGE` (3-6) ya maneja RESERVADO/VENDIDO; sumar `opacity-75` al article cuando VENDIDO.
- El botón `♡` (52-61) sigue como está (favoritos real es de otra fase).

### `F/pages/AutosPage.jsx` (page, request-response)

**Analog:** si mismo (líneas 81-112, 14 `useState` + `toggleEnLista`) para la UX de filtros/chips/histograma; y `F/pages/AgenciaPage.jsx:17-42` para el fetch con `cargando`/`error`.

**Patrón actual a reemplazar:**
```jsx
const [searchParams] = useSearchParams()
const [marcasActivas, setMarcasActivas] = useState(() => {
  const marca = searchParams.get('marca')
  return marca ? [marca] : []
})
const toggleEnLista = (setter) => (valor) =>
  setter((prev) => (prev.includes(valor) ? prev.filter((v) => v !== valor) : [...prev, valor]))
```
Nuevo: `const [searchParams, setSearchParams] = useSearchParams()`; `filtros = useMemo(() => leerFiltros(searchParams), [searchParams])`; `toggleEnLista` pasa a escribir en la URL (`setSearchParams(next, { replace: true })`, quita `pagina`); cambio de página con push. Texto/precio/año/km: estado local + debounce 350 ms. Efecto con `AbortController` y `api.get('/publicaciones', { params, signal, paramsSerializer: { indexes: null } })` (errores con `mensajeDeError`). Opciones de filtro e histograma desde `GET /publicaciones/facetas`. `window.scrollTo({ top: 0 })` al cambiar `pagina` (`ScrollToTop` solo reacciona a `pathname`). Eliminar `catalogoMock` (líneas 116-125 y análogos).

### `F/pages/HomePage.jsx`, `AgenciaPage.jsx`, `PublicacionDetallePage.jsx` (page)

**Analog:** rama real de `AgenciaPage.jsx:33-42`:
```jsx
setCargando(true)
api.get(`/agencias/${slug}`)
  .then((res) => {
    setAgencia(res.data)
    return api.get('/publicaciones', { params: { agenciaId: res.data.id } })
  })
  .then((res) => setPublicaciones(res?.data ?? []))
  .catch(() => setError('No se encontró la agencia'))
  .finally(() => setCargando(false))
```
Para todas: borrar `USE_MOCK_DATA` + import del mock + TODO (AgenciaPage líneas 10-15, 25-31). Agencia: `res.data.contenido`, título con `totalElementos` (línea 131), marquee de marcas desde `facetas?agenciaId=` (línea 44-47 hoy derivado de la lista) y `Paginador`. Home: destacados desde `/publicaciones/destacados`. Detalle: `GET /publicaciones/{id}` real, vendido muestra aviso, oculta consulta y llama `/similares`. Mantener el patrón de error de AgenciaPage (líneas 49-56: `<main>` con mensaje y `Link` de vuelta).

### `F/components/Paginador.jsx` (component)

**Analog:** parcial, botones de `AutosPage.jsx` (chips, clases `rounded-full border ... hover:bg-bronze`) y `ConfirmDialog.jsx` para el estilo de componente con props destructuradas. Props: `pagina`, `totalPaginas`, `onCambiar`. Export default.

### `F/pages/CreditosPage.jsx`, `F/data/creditosFotos.js`, `Footer.jsx`, `AppRouter.jsx`

**Footer** (`Footer.jsx:10-13`): sumar `<Link to="/creditos" className="transition hover:text-slate-300">Créditos de imágenes</Link>` en la línea de copyright (16-18, estilo discreto `text-[10px] text-slate-500`).
**AppRouter** (`AppRouter.jsx:16-19`): `import CreditosPage from '../pages/CreditosPage.jsx'` y `<Route path="/creditos" element={<CreditosPage />} />`.
**CreditosPage**: layout `<main className="mx-auto max-w-4xl px-4 py-16">` como el estado de error de `AgenciaPage.jsx:51`; datos desde `docs/demo/CREDITOS-FOTOS.md` transcriptos a `creditosFotos.js` (export nombrado).

### `F/pages/admin/AdminPublicacionFormPage.jsx` (mod)

**Analog:** si mismo, lineas 64-96 y 152-161.
```js
const FORM_INICIAL = { ..., transmision: 'MANUAL', ..., descripcion: '' }
const publicacionAForm = (p) => ({ ..., transmision: p.transmision ?? '', ... })
const payload = { ...form, ..., transmision: form.transmision || null, condicion: form.condicion || null }
```
Agregar en los **tres** lugares (si falta `publicacionAForm`, guardar borra el dato): `tipoCarroceria: ''` / `p.tipoCarroceria ?? ''` / `form.tipoCarroceria || null`; `precioAnterior: ''` / `p.precioAnterior != null ? String(Math.round(Number(p.precioAnterior))) : ''` / `form.precioAnterior ? Number(form.precioAnterior) : null`. El input de `precioAnterior` usa `formatearPrecio` (línea ~58-62) igual que `precio`, con ayuda "Si es mayor al precio, el auto aparece como oferta". El select usa los helpers `campo(...)` (línea 136) y las opciones de `etiquetas.js`.

### `F/pages/admin/AdminDashboardPage.jsx` (mod, zona)

**Analog:** si mismo, `AGENCIA_INICIAL` (9-16) y `comenzarEdicionAgencia` (114-126).
```js
const AGENCIA_INICIAL = { nombre: '', emailContacto: '', direccion: '', telefonoContacto: '', descripcion: '', logo: '' }
...
setAgenciaEditando({ id: a.id, nombre: a.nombre ?? '', ..., logo: a.logo ?? '' })
```
Sumar `zona: ''` / `zona: a.zona ?? ''` y un `<select>` "Sin especificar" + `ETIQUETA_ZONA` en ambos formularios (crear y editar); enviar `zona: form.zona || null`; mostrar la zona en la lista de agencias.

---

## Shared Patterns

### Error uniforme de API
**Source:** `B/exception/GlobalExceptionHandler.java` (+ `ReglaDeNegocioException`, `ResourceNotFoundException`)
**Apply to:** `CatalogoService`, `ConsultaService`, controller. Formato `{"error"}` / `{"error","campos"}`; reglas de negocio con `ReglaDeNegocioException` (400), inexistente con `ResourceNotFoundException` (404). Front: `mensajeDeError(err, 'texto de respaldo')` de `F/utils/errores.js:4`.

### Servicios `@Transactional` y mappers con lazy
**Source:** `B/service/PublicacionService.java:45-47, 64`
**Apply to:** `CatalogoService`. `open-in-view: false`: mapear a DTO dentro de `@Transactional(readOnly = true)`. Evitar `JOIN FETCH fotos` con paginación; usar `default_batch_fetch_size: 50`.

### Nombres y estilo
Java: 4 espacios, DTOs `@Data`/`@Builder`, mappers estáticos con constructor privado, mensajes y comentarios en español. Front: componentes default export, utilidades con export nombrado, sin `;` en archivos nuevos del front, Tailwind con paleta `bronze`/`navy`/`navy-dark`/`cream`.

### Tests back
**Source:** `BT/support/SeguridadWebMvcTestBase.java` (slice web con `bearerPara(email, rol)`), Mockito con `@ExtendWith(MockitoExtension.class)` (`PublicacionServiceTest.java:61-62`), `assertThatThrownBy(...)` AssertJ. Tests que requieren Postgres: base descartable local con `Assumptions`.

### Seguridad de rutas públicas
**Source:** `B/config/SecurityConfig.java:51` (`GET /api/publicaciones/**`, `/api/agencias/**` permitAll). Los endpoints nuevos GET de catálogo quedan cubiertos sin cambios; solo falta `/actuator/health`.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `db/migration/V1..V3__*.sql` | migration | batch | No hay migraciones en el repo; usar el SQL verificado de RESEARCH §Pattern 1 |
| `PaginaResponse` (record) | DTO | transform | El repo no usa records; usar el snippet de RESEARCH |
| `PostgresLocalTestBase` / `MigracionesPostgresTest` | test | batch | No existen tests con base real; usar RESEARCH "Test de migración" |
| `catalogoParams.test.js` | test (front) | transform | El front no tiene test runner; usar `node --test` (RESEARCH) |
| `.gitattributes` (`*.sql text eol=lf`), `.gitignore` (`*.dump`), README (IN-07) | config/doc | - | Sin analog; instrucciones en RESEARCH §Runbook y Pitfalls |
| Runbook Railway/Vercel (H1-H8) | ops | - | Solo checkpoints humanos; no es código |

## Metadata

**Analog search scope:** `src/main/java/com/danteautomotores/**`, `src/test/java/**`, `src/main/resources/`, `scripts/demo/`, `danteautomotores-front/src/{pages,components,utils,routes}`
**Files scanned:** ~35 leídos / 85 listados (`git ls-files src scripts`)
**Pattern extraction date:** 2026-10-03
