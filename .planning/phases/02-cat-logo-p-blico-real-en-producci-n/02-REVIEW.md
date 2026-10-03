---
phase: 02-cat-logo-p-blico-real-en-producci-n
reviewed: 2026-10-03T00:00:00Z
depth: standard
files_reviewed: 52
files_reviewed_list:
  - README.md
  - pom.xml
  - scripts/demo/sembrar-demo.js
  - scripts/verify/catalogo-humo.js
  - scripts/verify/con-back-local.sh
  - src/main/java/com/danteautomotores/config/ClockConfig.java
  - src/main/java/com/danteautomotores/config/DataSeeder.java
  - src/main/java/com/danteautomotores/config/SecretosGuard.java
  - src/main/java/com/danteautomotores/config/SecurityConfig.java
  - src/main/java/com/danteautomotores/controller/PublicacionController.java
  - src/main/java/com/danteautomotores/dto/agencia/AgenciaRequest.java
  - src/main/java/com/danteautomotores/dto/agencia/AgenciaResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/FacetasResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/FiltrosCatalogo.java
  - src/main/java/com/danteautomotores/dto/publicacion/PaginaResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResponse.java
  - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResumenResponse.java
  - src/main/java/com/danteautomotores/entity/Agencia.java
  - src/main/java/com/danteautomotores/entity/Publicacion.java
  - src/main/java/com/danteautomotores/enums/OrdenCatalogo.java
  - src/main/java/com/danteautomotores/enums/TipoCarroceria.java
  - src/main/java/com/danteautomotores/enums/ZonaAgencia.java
  - src/main/java/com/danteautomotores/mapper/AgenciaMapper.java
  - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
  - src/main/java/com/danteautomotores/repository/PublicacionRepository.java
  - src/main/java/com/danteautomotores/repository/spec/CatalogoSpecification.java
  - src/main/java/com/danteautomotores/service/AgenciaService.java
  - src/main/java/com/danteautomotores/service/CatalogoService.java
  - src/main/java/com/danteautomotores/service/ConsultaService.java
  - src/main/java/com/danteautomotores/service/PublicacionService.java
  - src/main/resources/application.yml
  - src/main/resources/db/migration/V1__esquema_original.sql
  - src/main/resources/db/migration/V2__fase1_destacado_y_public_id.sql
  - src/main/resources/db/migration/V3__fase2_catalogo.sql
  - src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java
  - src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java
  - ../danteautomotores-front/src/components/Footer.jsx
  - ../danteautomotores-front/src/components/Paginador.jsx
  - ../danteautomotores-front/src/components/PublicacionCard.jsx
  - ../danteautomotores-front/src/data/creditosFotos.js
  - ../danteautomotores-front/src/pages/AgenciaPage.jsx
  - ../danteautomotores-front/src/pages/AutosPage.jsx
  - ../danteautomotores-front/src/pages/CreditosPage.jsx
  - ../danteautomotores-front/src/pages/HomePage.jsx
  - ../danteautomotores-front/src/pages/PublicacionDetallePage.jsx
  - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
  - ../danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx
  - ../danteautomotores-front/src/routes/AppRouter.jsx
  - ../danteautomotores-front/src/utils/catalogoParams.js
  - ../danteautomotores-front/src/utils/cloudinary.js
  - ../danteautomotores-front/src/utils/etiquetas.js
  - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java (leído para trazar errores)
  - ../danteautomotores-front/src/utils/errores.js (leído para trazar errores)
findings:
  critical: 0
  warning: 7
  info: 10
  total: 17
status: issues_found
---

# Phase 2: Code Review Report

**Reviewed:** 2026-10-03
**Depth:** standard
**Files Reviewed:** 52 (back y front)
**Status:** issues_found

## Summary

Se revisó el catálogo público paginado (filtros, facetas, similares, destacados), las migraciones Flyway V1 a V3, el endurecimiento para producción (SecretosGuard, DataSeeder, actuator, CORS) y el front conectado a la API. No se encontró ningún bloqueante: la consulta del catálogo usa Criteria API con parámetros enlazados (sin SQL concatenado), el `LIKE` escapa `%`, `_` y `\`, el tamaño de página y el orden no los elige el cliente, el resumen público no expone datos del admin, las migraciones son idempotentes y el test de migraciones cubre el caso "base de producción creada por Hibernate".

Lo que sí hay son defectos de robustez en el borde de la API pública (un parámetro de página grande produce un 500, los parámetros de enum o número ilegibles producen un 400 con el mensaje técnico de Spring que el front muestra tal cual), una limitación funcional conocida pero sin red de seguridad (precio y orden mezclan ARS y USD), contenido comercial no respaldado en páginas públicas del front y un par de riesgos en los scripts de apoyo. Ninguno de los hallazgos de abajo contradice lo que el UAT dio por aprobado: son casos que ese UAT no ejercitó.

Verificado leyendo el bytecode de las dependencias: `spring-data-jpa 3.3.5` usa `PageableUtils.getOffsetAsInteger`, que lanza `InvalidDataAccessApiUsageException("Page offset exceeds Integer.MAX_VALUE")` (ver WR-01).

## Warnings

### WR-01: `?pagina=` grande da un 500 en un endpoint público (el comentario dice que cae a la primera página)

**File:** `src/main/java/com/danteautomotores/service/CatalogoService.java:148` (también `src/main/java/com/danteautomotores/controller/PublicacionController.java:38-51`)
**Issue:** `normalizar` solo acota la página por abajo (`< 1` pasa a 1). El `PropertyEditor` del controller solo captura `NumberFormatException`, y `Integer.valueOf("99999999999")` sí falla y cae a `null`, pero `pagina=90000000` (8 dígitos, entra en `Integer`) pasa. `PageRequest.of(89999999, 24)` tiene offset 2.159.999.976, mayor que `Integer.MAX_VALUE`; Spring Data lanza `InvalidDataAccessApiUsageException`, que no tiene handler propio y cae en `GlobalExceptionHandler.handleUnexpected`: responde 500 y escribe un stack trace con `log.error` por cada pedido. Es un endpoint sin token, así que cualquiera puede llenar el log o disparar alertas con una URL. El comentario del controller ("pagina=abc, o fuera de rango, cae a la primera página en vez de dar 400") promete lo contrario y el humo solo prueba `pagina=999`.
**Fix:** acotar también por arriba en `normalizar`, por ejemplo:
```java
static final int MAX_PAGINA = 10_000; // 240.000 autos: muy por encima de cualquier inventario real
int pagina = crudos.getPagina() == null || crudos.getPagina() < 1 || crudos.getPagina() > MAX_PAGINA
        ? 1 : crudos.getPagina();
f.setPagina(pagina);
```
Sumar un caso al humo (`/publicaciones?pagina=90000000` responde 200 con `pagina` 1) y a `CatalogoServiceTest`.

### WR-02: Un filtro con enum o número ilegible rompe el listado y el usuario ve el mensaje técnico de Spring

**File:** `src/main/java/com/danteautomotores/controller/PublicacionController.java:33-36` (efecto visible en `../danteautomotores-front/src/pages/AutosPage.jsx:209-213` y `../danteautomotores-front/src/utils/catalogoParams.js:14-24`)
**Issue:** `orden` desconocido y `pagina` ilegible se toleran a propósito ("un link viejo no rompa la página"), pero `tipo`, `zona`, `estado`, `transmision`, `anioMin`, `anioMax`, `kmMax`, `precioMin`, `precioMax` y `agenciaId` fallan el binding con 400. Los enums de Spring se convierten de forma sensible a mayúsculas, así que `?transmision=automatica` o `?zona=norte` (links escritos a mano, compartidos o de una versión anterior del front) devuelven 400. `GlobalExceptionHandler.handleMethodArgumentNotValid` arma `campos` con `fieldError.getDefaultMessage()`, que para un error de conversión es el texto del `TypeMismatchException` ("Failed to convert property value of type 'java.lang.String' to required type 'java.util.List' for property 'tipo'; ... no matching constants for [NAVE]", con nombres de clases Java). `mensajeDeError` del front prioriza `campos` sobre `error`, por lo que ese texto en inglés es lo que se muestra en rojo en `AutosPage`. El resumen del plan 02-06 da por hecho que `mensajeDeError` solo muestra el string `error`; no es así cuando la respuesta trae `campos`. Además `leerFiltros` del front copia sin validar cualquier valor de la URL, y la página queda sin resultados (el usuario debe darse cuenta de apretar "Limpiar todo").
**Fix:** (a) en el back, tolerar los valores inválidos igual que `orden`: registrar en el `@InitBinder` editores que ignoren (devuelvan `null`) lo que no convierta, o ignorar el filtro en `normalizar`; o (b) como mínimo, que el handler de binding en rutas públicas devuelva un mensaje fijo en español sin el texto de la excepción. En el front, filtrar en `leerFiltros` los valores de enum que no estén en `TIPO_CARROCERIA`, `ZONA`, `TRANSMISION` y `ESTADO` y descartar los escalares no numéricos.

### WR-03: Filtro de precio, orden por precio, histograma y "ofertas" mezclan ARS y USD sin ninguna advertencia

**File:** `src/main/java/com/danteautomotores/repository/spec/CatalogoSpecification.java:77-82,107-108`; `src/main/java/com/danteautomotores/service/CatalogoService.java:121-126,260-286`; `../danteautomotores-front/src/pages/AutosPage.jsx:268-271`
**Issue:** `PublicacionRequest` admite `ARS|USD` y el formulario del admin ofrece el selector, pero `precioMin/Max`, `PRECIO_ASC/DESC`, el rango y el histograma de facetas comparan el número crudo. Un auto de US$ 20.000 queda "más barato" que uno de $ 15.000.000 y el slider/histograma resulta sin sentido en cuanto haya un solo auto en dólares. El RESEARCH (Pitfall 5, Open Question 4) lo registró como riesgo aceptado porque la demo es todo ARS, pero no quedó documentado en el README, no hay un guard ni un aviso al admin, y los chips del filtro y las bandas de la Home hardcodean `$`. En un mercado de usados en Argentina, cargar en USD es esperable. `similaresA` sí respeta la moneda, lo que deja el comportamiento inconsistente entre listado y similares.
**Fix:** corto plazo, o bien impedir `USD` en `PublicacionRequest` hasta resolverlo, o bien documentarlo en el README y en el formulario del admin ("el catálogo ordena y filtra por el número, no convierte monedas"). Definitivo: filtrar y facetar por moneda (parámetro `moneda`, histograma por moneda) o guardar un precio normalizado.

### WR-04: El detalle marca el auto como favorito aunque la llamada falle, y el corazón de la card es un botón sin efecto dentro de un `<a>`

**File:** `../danteautomotores-front/src/pages/PublicacionDetallePage.jsx:154-157`; `../danteautomotores-front/src/components/PublicacionCard.jsx:60-69`
**Issue:** `alternarFavorito` hace `.catch(() => setFavoritoOk(true))` para cualquier error ("probablemente ya estaba en favoritos"). Un 401 por token vencido, un 500 o un corte de red muestran el corazón lleno y el usuario cree que lo guardó; además la función se llama "alternar" pero nunca quita el favorito. Por otro lado, la card (envuelta por `<Link>` en Autos, Home, Agencia y Similares) trae un `<button aria-label="Guardar auto">` con `preventDefault` y sin ninguna acción: promete guardar y no guarda, y es un control interactivo anidado dentro de un `<a>` (HTML inválido y problemático para lectores de pantalla).
**Fix:** distinguir el error: tratar como éxito solo el 409/duplicado que devuelva el backend (o consultar el estado real al cargar), mostrar un error en cualquier otro caso, y quitar el favorito con `DELETE`. En la card, o conectar el botón con el mismo endpoint o eliminarlo hasta que funcione.

### WR-05: Páginas públicas con afirmaciones comerciales no respaldadas y controles que no hacen nada

**File:** `../danteautomotores-front/src/pages/PublicacionDetallePage.jsx:136-147,477-524,527-529`; `../danteautomotores-front/src/components/PublicacionCard.jsx:54-56`; `../danteautomotores-front/src/pages/HomePage.jsx:160-164`
**Issue:** el detalle muestra "Precio financiando 50% o más" con el mismo monto que el precio de contado, "Te damos hasta 3% extra por tu auto al cambiarlo" y tres botones ("Simulá tu financiamiento", "Cotizar", "Reservar o agendar visita") que, con sesión, solo muestran "Esta función todavía no está conectada", y sin sesión redirigen al login. Todos los autos disponibles llevan el badge "Verificado" y el pie "Auto verificado por DanteAutomotores", y la Home dice "100% verificados". Nada en el modelo (no hay campo ni proceso de verificación) respalda esos textos. Esta fase se llama "Catálogo público real en producción": publicar números de financiación y una promesa de 3 % extra que no existen es un riesgo comercial y de consumo, y los botones de relleno son un callejón sin salida para el usuario real.
**Fix:** ocultar el bloque de financiación, el de "cambiá tu auto" y el botón de reserva hasta que existan; mostrar "Verificado" solo si hay un dato que lo respalde (o quitarlo); retirar "100% verificados" y "Sin sorpresas" o reemplazarlos por texto que sí sea cierto.

### WR-06: `con-back-local.sh` borra sin avisar cualquier base local preexistente con el nombre pedido

**File:** `scripts/verify/con-back-local.sh:108,119` (protecciones en `:46-56`)
**Issue:** en los modos `--copia-de` y `--vacia` el script ejecuta `DROP DATABASE IF EXISTS $BASE WITH (FORCE)` antes de crear, sin confirmar que la base haya sido creada por una corrida anterior del propio script. La única protección es no tocar el nombre `danteautomotores` y el regex `^[a-z0-9_]+$`. Una corrida con `--vacia mi_base_de_pruebas` (o con `--copia-de` hacia una base con datos) destruye esa base y su contenido; la cabecera dice "Solo se borra lo que este script creó", y no es cierto en este caso. Además, la lista de bases protegidas es de una sola.
**Fix:** exigir un prefijo propio y refusar si la base existe y no lo tiene:
```bash
[[ "$BASE" == humo_* ]] || fallar "la base descartable debe llamarse humo_*"
```
y, si la base ya existe sin ese prefijo, abortar en vez de `DROP`.

### WR-07: `AgenciaRequest` no limita el largo ni valida el formato del email

**File:** `src/main/java/com/danteautomotores/dto/agencia/AgenciaRequest.java:10-22`
**Issue:** la Fase 1 agregó `@Size` y `@Digits` a `PublicacionRequest` porque un dato demasiado largo llegaba a la base y volvía como un 409 engañoso. `AgenciaRequest` quedó sin esa defensa: `nombre`, `logo`, `direccion`, `telefonoContacto` y `emailContacto` mapean a `varchar(255)` sin `@Size`, y `emailContacto` solo tiene `@NotBlank` (acepta "xx"). Un nombre de más de 255 caracteres termina en `DataIntegrityViolationException`, que `GlobalExceptionHandler` traduce a "No se pudo completar la operación porque hay datos relacionados" (409), un mensaje que no se corresponde con el problema. El email de contacto se publica en `GET /api/agencias` y en la página de la agencia.
**Fix:**
```java
@NotBlank @Size(max = 255) private String nombre;
@Size(max = 255) private String logo, direccion, telefonoContacto;
@NotBlank @Email @Size(max = 255) private String emailContacto;
```

## Info

### IN-01: DataSeeder solo aborta con el perfil exacto `prod`; SecretosGuard aborta con cualquier perfil no de desarrollo

**File:** `src/main/java/com/danteautomotores/config/DataSeeder.java:102-107` y `src/main/java/com/danteautomotores/config/SecretosGuard.java:80-90`
**Issue:** el Javadoc de `SecretosGuard` dice "mismo criterio de fondo que DataSeeder", pero con `SPRING_PROFILES_ACTIVE=production`, `railway` o `staging` el guard exige secretos reales mientras el seeder solo avisa en el log si faltan las variables del admin: el despliegue arranca sin ningún admin. El README lo describe como comportamiento de `prod`, así que no es un bug de documentación, pero sí un desvío entre dos mecanismos que se anuncian como equivalentes.
**Fix:** extraer un helper común (`EntornoDeDesarrollo.esDesarrollo(environment)`) y usarlo en ambos.

### IN-02: `baseline-on-migrate: true` queda encendido de forma permanente

**File:** `src/main/resources/application.yml:29-35`
**Issue:** el propio comentario recomienda apagarlo tras el primer deploy, pero el default sigue en `true`. Con el flag activo, apuntar por error `SPRING_DATASOURCE_URL` a otra base no vacía y sin historial la "marca" como V1 y le ejecuta V2 y V3 (el `UPDATE publicaciones` fallaría si faltan las tablas, pero en una base ajena que casualmente tenga `publicaciones` y `agencias` se alterarían sus columnas sin aviso). Es el comportamiento que la fase buscó para la primera migración, no para siempre.
**Fix:** dejar el default en `false` y que solo la primera corrida de producción lo active con `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`; o abrir una tarea explícita para apagarlo tras el primer deploy.

### IN-03: Los tests de migración se saltean sin Postgres y el README se contradice

**File:** `src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java:92-103`; `README.md:56,89,92-93`
**Issue:** sin Postgres en `localhost:5433`, `MigracionesPostgresTest` y `CatalogoPostgresTest` se saltean en silencio (solo fallan con `-Ddante.pg.required=true`), por lo que un `mvn test` común puede dar "verde" sin haber ejercitado nunca V1 a V3 contra `validate`. El README dice en la sección Tests que "los tests no necesitan Docker ni Postgres" (línea 89) y dos secciones antes explica que sí se usan (línea 56). También incluye rutas personales (`/c/Users/toto/.maven/...`) en un archivo de documentación compartido.
**Fix:** corregir la frase del README; ejecutar la verificación de producción con `-Ddante.pg.required=true` antes de cada deploy (y dejarlo escrito en `02-08-PLAN.md`); usar `$HOME` o una variable en vez de la ruta personal.

### IN-04: Las fotos no tienen orden total, así que la portada puede variar entre listado y detalle

**File:** `src/main/java/com/danteautomotores/entity/Publicacion.java:95-97`; `src/main/java/com/danteautomotores/mapper/PublicacionMapper.java:49-50,78-80`
**Issue:** `fotos` es una lista sin `@OrderBy`, y tanto `toResponse` (sorted) como `toResumen` (min) tratan `orden == null` como 0 sin desempate. Si dos fotos empatan (fotos viejas con `orden` nulo, que solo se resecuencian al borrar una), el resultado depende del orden físico en que PostgreSQL devuelve las filas, y la portada de la card podría diferir de la primera foto del detalle.
**Fix:** `@OrderBy("orden ASC, id ASC")` en la colección y `.thenComparing(FotoPublicacion::getId, nullsLast(naturalOrder()))` en los dos comparadores.

### IN-05: La búsqueda de texto distingue acentos

**File:** `src/main/java/com/danteautomotores/repository/spec/CatalogoSpecification.java:43-49`
**Issue:** `lower(marca || ' ' || modelo) LIKE %palabra%` no ignora tildes: buscar "citroen" no encuentra "Citroën" (que está en la lista de marcas del formulario del admin) y "coupe" no encuentra "Coupé". El front dice "Buscá por marca o modelo" y usa `normalizar` sin acentos en el panel del admin, pero no en el catálogo público. Además `toLowerCase(Locale.ROOT)` de Java y `lower()` de PostgreSQL dependen de la configuración regional de cada uno para caracteres no ASCII.
**Fix:** habilitar la extensión `unaccent` en una migración V4 y comparar `unaccent(lower(...))`, o guardar una columna de búsqueda normalizada.

### IN-06: Las bandas de precio redondean con `Math.round` mientras el rango usa `Math.floor`/`Math.ceil`

**File:** `../danteautomotores-front/src/utils/catalogoParams.js:80-89` y `../danteautomotores-front/src/pages/AutosPage.jsx:245`
**Issue:** la primera banda empieza en `Math.round(min)`. Si el auto más barato vale 17.200.000,50, el link de "Buscá por presupuesto" manda `precioMin=17200001` y ese auto no aparece en su propia banda. `precioTope` usa `Math.floor`/`Math.ceil` (correcto). Los precios del demo son redondos, así que no se ve hoy.
**Fix:** `corte(0)` = `Math.floor(desdeMin)` y el último corte `Math.ceil(hastaMax)`.

### IN-07: El router no tiene ruta de captura (`*`)

**File:** `../danteautomotores-front/src/routes/AppRouter.jsx:16-57`
**Issue:** una URL inexistente (por ejemplo `/auto/12` o un link roto de un buscador) renderiza solo el Navbar y el Footer con el `<main>` vacío. Ya que esta fase publica el sitio, es la primera página que verá quien llegue con un link equivocado.
**Fix:** agregar `<Route path="*" element={<NoEncontradaPage />} />` con un enlace al catálogo.

### IN-08: Las columnas enum nuevas de V3 no tienen `CHECK`

**File:** `src/main/resources/db/migration/V3__fase2_catalogo.sql:2,5`
**Issue:** en V1 las columnas enum (`estado`, `transmision`, etc.) tienen `CHECK ... IN (...)`; `tipo_carroceria` y `zona` se agregan como `varchar(255)` libre. Un valor cargado a mano (o por un script) fuera del enum haría que Hibernate falle al leer esa fila y rompería el listado completo y las facetas, no solo ese auto.
**Fix:** en una V4 agregar `CHECK (tipo_carroceria IN ('SEDAN','HATCHBACK','SUV','PICKUP','UTILITARIO','COUPE','MONOVOLUMEN','FAMILIAR'))` y el equivalente para `zona`. No se edita V3 (ya aplicada).

### IN-09: La página de créditos no indica que las fotos se modifican

**File:** `../danteautomotores-front/src/pages/CreditosPage.jsx:21-24`; `../danteautomotores-front/src/utils/cloudinary.js:4-10`
**Issue:** las licencias CC BY y CC BY-SA exigen señalar si se hicieron cambios. El front sirve las fotos recortadas, reescaladas y recomprimidas por Cloudinary (`c_fill`, `c_limit`, `q_auto`), y la página no lo aclara. El crédito aparece solo en `/creditos` (enlace en el footer), no junto a la foto.
**Fix:** agregar una frase del estilo "Las imágenes fueron recortadas y redimensionadas para su visualización" en `CreditosPage`.

### IN-10: `sembrar-demo.js` pisa los datos de una agencia existente con el mismo nombre y no se recupera de una corrida a medias

**File:** `scripts/demo/sembrar-demo.js:56-66`
**Issue:** si el backend ya tiene una agencia llamada "Premium Hub" (por ejemplo, una real cargada desde el panel), el script la reemplaza con un `PUT` completo (descripción, dirección, teléfono, email y zona de ejemplo, conservando solo el logo). La guarda `LIMPIAR` protege contra el borrado, no contra esto, y es el script que se usaría contra producción para la demo. Además, si falla a mitad de una corrida, los autos ya creados bloquean la siguiente (hay que borrarlos a mano o usar `FORZAR=1`, que duplica).
**Fix:** si la agencia existe, solo completar `zona` cuando viene vacía (o saltear la agencia) y no sobrescribir contacto; registrar los ids creados para poder deshacer una corrida parcial.

---

_Reviewed: 2026-10-03_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
