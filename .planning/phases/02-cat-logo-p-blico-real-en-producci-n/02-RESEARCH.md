# Phase 2: Catálogo público real en producción - Research

**Researched:** 2026-10-03
**Domain:** Spring Boot 3.3.5 (Spring Data JPA Specifications + Hibernate 6.5.3, Flyway 10.10.0 sobre PostgreSQL) + React 18 / Vite / react-router 6 (catálogo público) + despliegue Railway/Vercel. Fase brownfield cross-repo (back en `danteautomotores-back`, front en `danteautomotores-front`).
**Confidence:** HIGH en migraciones y consultas (se ejecutaron contra el Postgres local de docker-compose y con un test descartable de Hibernate, ya eliminados); MEDIUM en Railway/Vercel (el estado real de producción no se puede leer desde acá: hay checkpoints humanos que lo confirman).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** El auto tiene un **tipo de carrocería** que el admin elige en el formulario (enum: Sedán, Hatchback, SUV, Pickup, Utilitario, y los que el planner considere necesarios para el mercado argentino, p. ej. Coupé/Monovolumen). El filtro "tipo de auto" de `/autos` funciona contra ese dato. Los autos existentes sin tipo quedan como "sin especificar" y no rompen nada. — **Reversibility:** costly — es una columna nueva en `publicaciones` con migración y un campo nuevo en el contrato de la API y del form.
- **D-02:** La **ubicación** sale de la **agencia**, no del auto: la agencia tiene una zona (CABA, Zona Norte, Zona Sur, Zona Oeste, Interior, u otra lista que el planner justifique) que se elige al crear/editar la agencia, y todos sus autos la heredan para el filtro "ubicación". El admin no la carga auto por auto. — **Reversibility:** costly — columna nueva en `agencias` y campo en el form de agencias.
- **D-03:** Las **ofertas** se modelan con un **precio anterior** opcional en el auto. Si `precioAnterior > precio`, el auto es oferta: la card y el detalle muestran el precio anterior tachado y entra en el filtro "solo ofertas". Si falta o no es mayor, no es oferta. — **Reversibility:** costly — columna nueva + validación + UI en card/detalle/form.
- **D-04:** Los **vendidos** se ven en el catálogo público (`/autos`, página de agencia) con la etiqueta "Vendido", **al final de la lista**, durante **30 días desde que se marcaron como vendidos**; después dejan de aparecer en el catálogo público (siguen en el panel admin). Para esto hace falta registrar la fecha en que el auto pasó a VENDIDO. La Home (destacados) nunca muestra vendidos (decisión de Fase 1).
- **D-05:** Los **reservados** se ven normalmente con la etiqueta "Reservado"; el detalle se puede ver y consultar (por si la reserva se cae).
- **D-06:** Un **link directo a un auto vendido** (p. ej. compartido por WhatsApp) abre la ficha con un aviso "Este auto ya se vendió", sin acción de consulta, y con sugerencias de autos parecidos disponibles (criterio simple: mismo tipo de carrocería o misma marca, precio cercano; el planner lo define). Aplica aunque hayan pasado los 30 días.
- **D-07:** `/autos` usa **páginas numeradas** (estilo Mercado Libre), **24 autos por página**, paginado en el backend. La página actual, los filtros y el orden viven en la URL (`?pagina=2&...`), así un link compartido o el botón "atrás" vuelven al mismo lugar.
- **D-08:** **Orden por defecto:** primero destacados, después los publicados más recientes, y los vendidos al final. El visitante puede cambiar a precio (menor/mayor), año (más nuevo) y kilómetros (menos km). Los filtros existentes de `/autos` (búsqueda, marca/modelo, año, km, precio, transmisión, color, disponibilidad, tipo, ubicación, ofertas) pasan a resolverse en el backend.
- **D-09:** Producción **arranca con la demo**: las 6 agencias y los 11 autos con sus 5 fotos cada uno, cargados con `scripts/demo/sembrar-demo.js` contra la API productiva una vez desplegada (usando el script, no SQL, para que las fotos pasen por Cloudinary y la validación). El admin borra los de demo desde el panel cuando cargue autos reales. El script debe completarse con los datos nuevos (D-01 tipo, D-02 zona de agencia, D-03 precio anterior en alguno para que haya ofertas). — **Reversibility:** reversible — se borran desde el panel.
- **D-10:** Los **créditos de las fotos** (CC BY / CC BY-SA, ver `docs/demo/CREDITOS-FOTOS.md`) se publican en una **página de créditos** enlazada desde el **pie de página** con un link discreto "Créditos de imágenes". No se muestran en cada foto.

### Claude's Discretion
- Migraciones: herramienta (Flyway sugerido en ROADMAP) y línea base que incluya el esquema de Fase 1 (incluye `destacado`, `public_id`, columnas nuevas de esta fase). Cómo se aplica la línea base sobre la base de producción existente sin perder datos.
- Diseño del endpoint paginado (contrato, nombres de parámetros, `Page` de Spring o DTO propio), consultas/índices y cómo se calculan los datos de los filtros (marcas, rangos de precio, histograma) desde el backend.
- Cómo se cierran PROD-02 (secret JWT + Cloudinary obligatorios en prod; parte ya hecha en Fase 1 con `SecretosGuard`) y PROD-04 (perfil prod sin `ddl-auto`/`show-sql`), y la configuración de Railway/Vercel.
- Estados vacíos y de carga del catálogo, y manejo de errores de la API en las páginas públicas.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CAT-01 | El visitante ve el catálogo con datos reales del backend (sin mocks) en Home, Autos, Agencia y Detalle | Inventario de mocks y campos que espera la UI vs `PublicacionResponse` (§Front); contrato nuevo de la API (§Patrón 2); eliminación de `catalogoMock/homeMock/agenciaMock` y 4 `USE_MOCK_DATA` |
| CAT-02 | El visitante puede filtrar y ordenar el catálogo, que se carga paginado | `GET /api/publicaciones` paginado con DTO propio, filtros multivalor, orden con `CASE` (verificado con Hibernate 6.5.3 + Postgres), `GET /api/publicaciones/facetas`, estado de la URL con `useSearchParams` |
| CAT-03 | El visitante ve en la Home los autos que el admin marcó como destacados | `GET /api/publicaciones/destacados` (destacado y no vendido); Home deja de pedir todo el inventario |
| CAT-04 | El visitante ve el estado de cada auto (disponible / reservado / vendido) en card y detalle | `estado` + `fechaVendido` en el contrato; regla de visibilidad de vendidos 30 días; detalle de vendido con aviso y similares |
| PROD-02 | La app no arranca sin un secret JWT y credenciales de Cloudinary válidas; CORS acepta orígenes con espacios | `SecretosGuard` ya cubre JWT/DB y CORS ya hace trim (verificado); falta exigir Cloudinary en perfiles no-dev (§PROD-02) |
| PROD-04 | Back y front desplegados (Railway/Render + Vercel) con perfil de producción (sin `ddl-auto: update` ni `show-sql`) | Flyway 10.10.0 + V1 (esquema original) / V2 (Fase 1) / V3 (Fase 2) con `baseline-on-migrate`, `ddl-auto: validate`, `show-sql: false`, runbook con checkpoints humanos (backup, ensayo con copia, variables, rollback) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Directivas accionables de `.claude/CLAUDE.md` que el plan debe respetar:

- Stack fijo: Spring Boot 3.3 / Java 21 / PostgreSQL / JPA + React 18 / Vite / Tailwind v4. No cambiar de stack.
- Idioma: mensajes al usuario, errores, comentarios, nombres de tablas/campos/enums en **español**. Entidades en singular (`Publicacion`), DTOs por feature en `dto/<feature>/`, mappers con métodos estáticos `toResponse()`/`toEntity()`.
- Servicios devuelven DTOs, nunca entidades; controllers envuelven en `ResponseEntity<T>`; DTOs de entrada con `@Valid`; `ResourceNotFoundException` → 404, `ReglaDeNegocioException` → 400 (formato `{"error"}` / `{"error","campos"}`).
- Front: componentes `.jsx` PascalCase, utilidades `.js` camelCase, handlers `handle*`, 2 espacios, comillas simples, sin punto y coma en el código nuevo del front tal como está hoy (el front actual no usa `;`: seguir el estilo del archivo que se edita). Estilos solo con utilidades Tailwind y la paleta existente (`bronze`, `navy`, `navy-dark`, `cream`).
- Back y front son repos separados; la planificación vive en el back. Imágenes siempre por Cloudinary.
- No hacer ediciones directas fuera de un flujo GSD.
- Animaciones: moderadas (la Fase 6 hace el pulido; esta fase no agrega motion nuevo salvo skeletons/estados de carga básicos).

## Summary

La fase tiene tres frentes de riesgo muy distintos. El **más riesgoso** es pasar la base de producción (creada por `ddl-auto: update` con una versión vieja del back) a migraciones versionadas sin perder datos. Se resolvió y **se probó de punta a punta** con el Postgres 16 local: la receta segura es Flyway 10.10.0 (la versión que gestiona el BOM de Boot 3.3.5) con tres migraciones: `V1` = el esquema original tal como lo dejaba Hibernate antes de la Fase 1, `V2` = los cambios de Fase 1 (`destacado`, `public_id`) con `ADD COLUMN IF NOT EXISTS`, `V3` = lo nuevo de esta fase. Con `baseline-on-migrate=true` y `baseline-version=1`, una base existente sin historial se marca como "ya en V1" y recibe solo V2 y V3; una base vacía recibe V1+V2+V3. Se verificó que el resultado de ambos caminos es **idéntico** (pg_dump comparado) y que `V1+V2` reproducen exactamente el esquema que generó Hibernate en la base de desarrollo. Todos los cambios son aditivos, así que la versión vieja del back sigue funcionando contra la base migrada: ese es el seguro de reversibilidad.

El segundo frente es el **catálogo paginado**. `GET /api/publicaciones` hoy devuelve una lista completa filtrando `DISPONIBLE` por default (`PublicacionService.java:70`). Se reemplaza por un contrato paginado con **DTO propio** (`PaginaResponse`, página base 1 para calzar con `?pagina=2`), tamaño fijo 24, filtros multivalor y orden decidido por el servidor (nunca un `Sort` que venga del cliente). Se verificó con un test descartable contra Postgres que un `Specification` con `orderBy(CASE ... )` funciona con `Pageable` sin sort, que el conteo no se rompe y que `ddl-auto: validate` pasa sobre el esquema migrado. Hacen falta tres columnas nuevas (`tipo_carroceria`, `precio_anterior`, `fecha_vendido` en `publicaciones`) y una en `agencias` (`zona`), más endpoints auxiliares: `facetas` (marcas, modelos, rango/histograma de precio, etc., que reemplazan lo que hoy calcula el mock), `destacados` y `{id}/similares`. En el front, `AutosPage` (573 líneas) pasa de filtrar en memoria a derivar todo de `useSearchParams`, y los mocks y los 4 `USE_MOCK_DATA` desaparecen.

El tercer frente es **producción**: PROD-02 está casi hecho (`SecretosGuard` ya es estricto fuera de dev/local/test y exige 32 bytes; CORS ya recorta espacios): solo falta exigir las credenciales de Cloudinary. PROD-04 se cierra moviendo `ddl-auto`/`show-sql` a valores seguros en el `application.yml` base (no depender de que haya un perfil), más `/actuator/health` público para el healthcheck de Railway (hoy `anyRequest().authenticated()` lo bloquea). Todo lo que requiere el panel de Railway/Vercel o credenciales reales (backup, variables, deploy, correr la demo) va como checkpoint humano con instrucciones exactas (§Runbook).

**Primary recommendation:** Hacer **primero y solo** la pista de migraciones (Flyway + V1/V2/V3 + `validate` + tests contra Postgres local) como tracer, porque es lo que no se puede arreglar a posteriori en producción; después las columnas/endpoints del catálogo, después el front, y dejar el deploy y la carga de la demo para el final con checkpoints humanos explícitos (backup y ensayo con copia de producción antes del primer arranque en Railway).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Esquema versionado (Flyway) y `validate` | Database / Storage (migraciones) | API / Backend (arranque ejecuta Flyway) | El esquema lo define el SQL versionado; Hibernate solo valida |
| Filtros, orden, paginación y visibilidad de vendidos | API / Backend | — | El visitante no puede traer el inventario completo; la regla de 30 días y el orden "vendidos al final" son del servidor |
| Facetas (marcas, rangos, histograma de precio) | API / Backend | Browser (dibuja slider/histograma) | Se calculan sobre el set visible; el front solo las presenta |
| `fecha_vendido` | API / Backend (servicio de estado) | Database | La fija el servicio al pasar a VENDIDO; no es un dato del cliente |
| Zona (ubicación) heredada del auto | Database (JOIN con agencia) | API (DTO la expone) | D-02: se guarda en la agencia, no en el auto |
| Estado de filtros/página/orden | Browser (URL: `useSearchParams`) | API | D-07: la URL es la fuente de verdad (links compartibles, botón atrás) |
| Cards, detalle de vendido, tachado de oferta | Browser | API (campos `oferta`, `precioAnterior`, `estado`) | Presentación; el flag `oferta` lo calcula el servidor para que filtro y UI no diverjan |
| Miniaturas/tamaños de foto | CDN (Cloudinary transformaciones por URL) | Browser | Evita bajar originales de hasta 10 MB en cada card |
| Secretos y Cloudinary obligatorios, perfil prod | API / Backend (arranque: `SecretosGuard`) | Plataforma (variables en Railway) | Falla rápido antes de aceptar tráfico |
| Healthcheck | API (`/actuator/health` público) | Plataforma (Railway) | Railway activa el deploy nuevo recién cuando responde 2xx |
| Página de créditos de fotos | Browser (ruta estática) | — | Contenido estático (D-10) |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Flyway (`flyway-core` + `flyway-database-postgresql`) | 10.10.0 (la gestiona el BOM, no se declara versión) | Migraciones versionadas | `[VERIFIED: ~/.m2/.../spring-boot-dependencies/3.3.5/spring-boot-dependencies-3.3.5.pom:57]` `<flyway.version>10.10.0</flyway.version>`; `flyway-core` en las líneas 530-537 y `flyway-database-postgresql` en 571-572 del mismo BOM. Desde Flyway 10 el soporte de PostgreSQL vive en un módulo aparte `[CITED: docs.spring.io/spring-boot/3.3/how-to/data-initialization.html]` |
| Spring Data JPA `Specification` + `Page` | 3.3.5 (Boot) | Consultas dinámicas paginadas | Ya se usa (`PublicacionSpecification`, `JpaSpecificationExecutor`) |
| Hibernate ORM | 6.5.3.Final (Boot) | `ddl-auto: validate` | `[VERIFIED: ~/.m2/repository/org/hibernate/orm/hibernate-core/6.5.3.Final]` y probado: `validate` pasó sobre el esquema migrado |
| Spring Boot Actuator | 3.3.5 (ya en pom) | `/actuator/health` para Railway | Ya declarado en `pom.xml` (`spring-boot-starter-actuator`) |
| react-router-dom | ^6.27.0 (ya instalado) | `useSearchParams` como estado del catálogo | `[VERIFIED: danteautomotores-front/package.json]` |
| axios | ^1.7.7 (ya instalado) | Cliente (con `AbortController`/`signal`) | ya instalado |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `node:test` (built-in, Node v24.14.1 en esta máquina) | — | Tests sin dependencias de la lógica pura del front (parseo/serialización de filtros de la URL) | Front sin test runner; **no** agregar vitest (QA-V2-01 está diferido a v2) |
| Cloudinary transformaciones por URL | — | `c_fill,w_640,h_420,q_auto,f_auto` para cards, otra mayor para el detalle | Reutilizar `urlMiniatura(url, transformacion)` de `src/utils/cloudinary.js` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flyway | Liquibase | Más pesado para un esquema de 6 tablas; ROADMAP ya sugiere Flyway |
| `PaginaResponse` propio | `Page` directo + `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` / `PagedModel` | Con `VIA_DTO` el JSON es `{content, page:{size,totalElements,totalPages,number}}` con `number` base 0 `[CITED: docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html]`; el front usa `?pagina=2` base 1 y nombres en español, y el DTO propio evita además aceptar un `Pageable`/`sort` del cliente. **Usar el DTO propio.** |
| Testcontainers para los tests con Postgres | Postgres del `docker-compose` (puerto 5433) con base descartable por clase | Testcontainers 1.19.8 (el que gestiona Boot 3.3.5) usa una API de Docker retirada: con Docker Engine 29 falla con "client version 1.32 is too old" `[CITED: github.com/testcontainers/testcontainers-java/issues/11210, #11235]`. Esta máquina tiene Docker Engine 29.6.2 (min API 1.40) `[VERIFIED: docker version]`. **No usar Testcontainers**; usar el Postgres local con `Assumptions` |
| H2 para tests | — | No sirve para validar Flyway/PG (checks, identity, `LOCALTIMESTAMP`); H2 2.2.224 ni siquiera está en `~/.m2` (solo 2.4.240 de otro proyecto) |
| `like '%x%'` simple | Full-text search de Postgres | Con cientos de autos alcanza el `like` con tokens; FTS es sobreingeniería ahora |

**Installation (backend):**
```xml
<!-- pom.xml, junto a las demás dependencias; sin <version>: la gestiona el BOM de Boot -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
Paso online obligatorio una vez (Flyway no estaba en `~/.m2`): `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -Djava.version=17 -DskipTests test-compile` (sin `-o`). En esta sesión se bajaron `flyway-core:10.10.0` y `flyway-database-postgresql:10.10.0` con `mvn dependency:get` y quedaron cacheados `[VERIFIED: ls ~/.m2/repository/org/flywaydb/ → flyway-core, flyway-database-postgresql, flyway-parent]`; en otra máquina hay que repetir el paso online. A partir de ahí `mvn -B -o -Djava.version=17 test` sigue funcionando.

**Installation (frontend):** ninguna (`# no new packages`).

**Version verification:** Maven Central responde 200 para `flyway-core-10.10.0.pom` y `flyway-database-postgresql-10.10.0.pom` `[VERIFIED: curl -sI repo.maven.apache.org]`. Flyway 10.10.0 es viejo (la imagen avisa "out of date, upgrade to 13.9.0") pero es la que gestiona Boot 3.3.5; funciona con el Postgres 16.15 local `[VERIFIED: salida de flyway/flyway:10.10.0 contra el contenedor]`. Si el Postgres de Railway es 17 o más nuevo, Flyway 10.10.0 solo debería **avisar** (warning "support has not been tested"), no fallar `[CITED: github.com/flyway/flyway/issues/3242; soporte de PG 17 desde Flyway 10.20.0]`; si molesta, se puede subir con `<flyway.version>10.22.0</flyway.version>` en `<properties>` (override estándar del BOM) — opcional, ver Open Question 3.

## Package Legitimacy Audit

El seam `package-legitimacy check` solo cubre `npm|pypi|crates`; las dependencias Maven de esta fase son **BOM-gestionadas por Spring Boot 3.3.5** (fuente autoritativa, leída en esta sesión: `spring-boot-dependencies-3.3.5.pom:57,530-537,571-572`) y publicadas por Redgate (`org.flywaydb`). No se agrega ningún paquete npm.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| org.flywaydb:flyway-core | Maven Central | años (Flyway 10.10.0) | — | github.com/flyway/flyway | n/a (BOM de Boot) | Approved |
| org.flywaydb:flyway-database-postgresql | Maven Central | años | — | github.com/flyway/flyway | n/a (BOM de Boot) | Approved |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
*No se agregan Testcontainers, H2, vitest ni librerías de UI: si el planner decide lo contrario, cada una pasa por `checkpoint:human-verify`.*

## Architecture Patterns

### System Architecture Diagram

```
 VISITANTE (browser, Vercel)                          BACKEND (Railway, perfil prod)                 DATOS
 ─────────────────────────                            ────────────────────────────                 ─────
 /autos?pagina=2&marca=Toyota&orden=precio_asc
   │ useSearchParams = fuente de verdad
   │ (debounce en texto/precio; replace en filtros,
   │  push en cambio de página)
   ▼
 GET /api/publicaciones?...  ───────────────────────► PublicacionController.buscar
 GET /api/publicaciones/facetas?agenciaId=             ├─ normaliza/clampa parámetros (pagina>=1, listas<=20,
 GET /api/publicaciones/destacados?limite=6            │  busqueda<=60, orden desconocido -> RELEVANCIA)
 GET /api/publicaciones/{id}  (cualquier estado)       ▼
 GET /api/publicaciones/{id}/similares                CatalogoService (@Transactional readOnly, Clock)
   ◄── PaginaResponse<PublicacionResumenResponse>      ├─ CatalogoSpecification:
        {contenido,pagina,tamanio,                     │    visible = estado<>VENDIDO OR fechaVendido>=ahora-30d
         totalElementos,totalPaginas}                  │    + filtros (IN, rangos, LIKE por tokens, ofertas)
                                                       │    + orderBy(CASE vendido, destacado desc, fecha desc, id desc)
                                                       ▼
 PublicacionCard / detalle (estado, oferta,           PublicacionRepository.findAll(spec, PageRequest.of(p-1,24))
 tachado, "Este auto ya se vendió")                    │   (batch fetch de agencia y fotos: sin N+1)
                                                       ▼
                                                      PostgreSQL ◄── Flyway V1/V2/V3 al arrancar (antes de JPA)
                                                                      ddl-auto: validate
 Arranque en Railway:
   Dockerfile (ENV SPRING_PROFILES_ACTIVE=prod) → SecretosGuard (JWT 32B, DB pass, Cloudinary) → Flyway → Hibernate validate
   → DataSeeder → /actuator/health 200 → Railway activa el deploy nuevo (si falla, queda el anterior)
```

### Recommended Project Structure
```
danteautomotores-back/
├── src/main/resources/
│   ├── application.yml                 # base SEGURA: ddl-auto validate, show-sql false, flyway on
│   ├── application-dev.yml             # (opcional) show-sql true para desarrollo
│   └── db/migration/
│       ├── V1__esquema_original.sql    # esquema previo a la Fase 1 (tal como lo dejó Hibernate)
│       ├── V2__fase1_destacado_y_public_id.sql
│       └── V3__fase2_catalogo.sql
├── src/main/java/com/danteautomotores/
│   ├── enums/TipoCarroceria.java, ZonaAgencia.java, OrdenCatalogo.java
│   ├── dto/publicacion/PaginaResponse.java (o dto/common/), PublicacionResumenResponse.java,
│   │                    FacetasResponse.java
│   ├── repository/spec/CatalogoSpecification.java
│   ├── service/CatalogoService.java    # buscar / destacados / similares / facetas
│   └── config/ClockConfig.java         # @Bean Clock (testeable)
├── src/test/java/.../support/PostgresLocalTestBase.java   # base descartable en localhost:5433
├── scripts/demo/ (sembrar-demo.js extendido), .gitattributes (*.sql text eol=lf)
danteautomotores-front/src/
├── utils/catalogoParams.js (+ .test.js con node:test), utils/etiquetas.js (labels enums), utils/cloudinary.js
├── pages/CreditosPage.jsx, data/creditosFotos.js
└── (se borra src/mocks/ y los 4 USE_MOCK_DATA)
```

### Pattern 1: Migraciones — V1 original + baseline-version=1 (VERIFICADO)

**What:** `V1` recrea el esquema anterior a la Fase 1; en una base existente Flyway la marca como "ya aplicada" sin ejecutarla; `V2`/`V3` son aditivas e idempotentes.
**When to use:** siempre; es el único camino probado para la base de producción vieja *y* para bases vacías.
**Resultado probado (Postgres 16.15 del docker-compose, Flyway 10.10.0):**
- Base "prod simulada" (copia de la de desarrollo a la que se le borraron `destacado` y `public_id`, con una fila VENDIDO): `Successfully baselined schema with version: 1` → `Successfully applied 2 migrations ... now at version v3`; la fila vendida quedó con `fecha_vendido` rellenada y no se perdió ningún dato. `[VERIFIED: ejecución local]`
- Base vacía: `Current version of schema "public": << Empty Schema >>` → aplica V1, V2, V3. `[VERIFIED]`
- `pg_dump --schema-only` de la base vacía migrada y de la "prod simulada" migrada: **idénticos**. Contra la base de desarrollo creada por Hibernate: las únicas diferencias son las columnas e índices de V3 y la tabla `flyway_schema_history`. `[VERIFIED]`
- Base ya con las columnas de Fase 1 (la de desarrollo): V2 solo emite `WARNING: column "destacado" ... already exists, skipping`; re-ejecutar da `Schema "public" is up to date`. `[VERIFIED]`
- Sin `baselineOnMigrate` sobre una base no vacía: `ERROR: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true...` `[VERIFIED]` (es la red de seguridad; por eso la opción debe estar prendida **a propósito** en el primer deploy).

**Config (application.yml base):**
```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate          # [VERIFIED: pasó contra el esquema migrado]
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true     # solo actúa si hay esquema no vacío SIN flyway_schema_history
    baseline-version: 1
    # locations: classpath:db/migration  (default)
```
`[CITED: documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html]` — "Be careful when enabling this as it removes the safety net that ensures Flyway does not migrate the wrong database in case of a configuration mistake!". Mitigación: la URL de la base en producción se verifica en el checkpoint H4 y, tras el primer deploy exitoso, se puede apagar con la variable `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` (la opción es inerte una vez que existe la tabla de historial).

**Código de las migraciones (probado; es el contenido a copiar, con comentarios en ASCII):**

`V1__esquema_original.sql`
```sql
-- Esquema tal como lo dejo Hibernate (ddl-auto: update) antes de la Fase 1.
-- En una base existente Flyway NO ejecuta este script (baseline-version=1); solo crea las tablas en bases vacias.
CREATE TABLE agencias (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    descripcion text,
    direccion varchar(255),
    email_contacto varchar(255),
    fecha_alta timestamp(6),
    logo varchar(255),
    nombre varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    telefono_contacto varchar(255),
    CONSTRAINT agencias_pkey PRIMARY KEY (id),
    CONSTRAINT ukav7uu38c6bbsvc80sy4bfsh6j UNIQUE (slug)
);

CREATE TABLE usuarios (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    email varchar(255) NOT NULL,
    fecha_registro timestamp(6),
    nombre varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    rol varchar(255) NOT NULL,
    telefono varchar(255),
    CONSTRAINT usuarios_pkey PRIMARY KEY (id),
    CONSTRAINT ukkfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email),
    CONSTRAINT usuarios_rol_check CHECK (rol in ('ADMIN', 'COMPRADOR'))
);

CREATE TABLE publicaciones (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    anio integer NOT NULL,
    color varchar(255),
    combustible varchar(255),
    condicion varchar(255),
    descripcion text,
    estado varchar(255),
    fecha_publicacion timestamp(6),
    kilometraje integer,
    marca varchar(255) NOT NULL,
    modelo varchar(255) NOT NULL,
    moneda varchar(255),
    precio numeric(12,2) NOT NULL,
    transmision varchar(255),
    admin_id bigint NOT NULL,
    agencia_id bigint NOT NULL,
    CONSTRAINT publicaciones_pkey PRIMARY KEY (id),
    CONSTRAINT publicaciones_combustible_check CHECK (combustible in ('NAFTA', 'DIESEL', 'GNC', 'HIBRIDO', 'ELECTRICO')),
    CONSTRAINT publicaciones_condicion_check CHECK (condicion in ('EXCELENTE', 'MUY_BUENO', 'BUENO', 'REGULAR')),
    CONSTRAINT publicaciones_estado_check CHECK (estado in ('DISPONIBLE', 'RESERVADO', 'VENDIDO')),
    CONSTRAINT publicaciones_transmision_check CHECK (transmision in ('MANUAL', 'AUTOMATICA')),
    CONSTRAINT fk4we40j4h5dcqgb5qcdrwiuvmd FOREIGN KEY (admin_id) REFERENCES usuarios (id),
    CONSTRAINT fkhmv7xu8ff7aa0twbu24wsbscb FOREIGN KEY (agencia_id) REFERENCES agencias (id)
);

CREATE TABLE fotos_publicacion (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    orden integer,
    url varchar(255) NOT NULL,
    publicacion_id bigint NOT NULL,
    CONSTRAINT fotos_publicacion_pkey PRIMARY KEY (id),
    CONSTRAINT fkb859gbcnh6nbr62akqoquy20m FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id)
);

CREATE TABLE consultas (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    email_comprador varchar(255) NOT NULL,
    fecha timestamp(6),
    mensaje text,
    nombre_comprador varchar(255) NOT NULL,
    telefono_comprador varchar(255),
    publicacion_id bigint NOT NULL,
    CONSTRAINT consultas_pkey PRIMARY KEY (id),
    CONSTRAINT fknycgcr2xmas00xlcodcig611m FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id)
);

CREATE TABLE favoritos (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    fecha timestamp(6),
    publicacion_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    CONSTRAINT favoritos_pkey PRIMARY KEY (id),
    CONSTRAINT uklbdxm8xrecugqmhfov0g8pvul UNIQUE (usuario_id, publicacion_id),
    CONSTRAINT fkgisq99c5dapg7un36hhysttlo FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT fkq9wif2hcqfxj8t49wo613wm0h FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
```
Los nombres de constraints (`ukav7uu...`, `fk4we4...`) son los que generó Hibernate; se conservan **idénticos** para que migraciones futuras puedan hacer `DROP CONSTRAINT` por nombre en cualquier base. Los `CHECK` se escribieron como `col in (...)` (la forma que emite Hibernate) porque así `pg_dump` los reproduce byte a byte; la forma `= ANY (ARRAY[...]::text[])` daba el mismo significado pero un dump distinto.

`V2__fase1_destacado_y_public_id.sql`
```sql
-- Cambios de la Fase 1. IF NOT EXISTS porque la base local de desarrollo ya los tiene (los agrego ddl-auto: update).
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS destacado boolean NOT NULL DEFAULT false;
ALTER TABLE fotos_publicacion ADD COLUMN IF NOT EXISTS public_id varchar(255);
```

`V3__fase2_catalogo.sql`
```sql
-- Fase 2: datos nuevos para los filtros del catalogo publico (D-01 tipo, D-02 zona, D-03 precio anterior, D-04 fecha de venta).
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS tipo_carroceria varchar(255);
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS precio_anterior numeric(12,2);
ALTER TABLE publicaciones ADD COLUMN IF NOT EXISTS fecha_vendido timestamp(6);
ALTER TABLE agencias ADD COLUMN IF NOT EXISTS zona varchar(255);

-- Los autos que ya figuraban como VENDIDO cuentan como vendidos desde esta migracion: sin fecha quedarian fuera del
-- catalogo sin haber tenido nunca sus 30 dias de visibilidad.
UPDATE publicaciones SET fecha_vendido = LOCALTIMESTAMP WHERE estado = 'VENDIDO' AND fecha_vendido IS NULL;

CREATE INDEX IF NOT EXISTS idx_publicaciones_agencia ON publicaciones (agencia_id);
CREATE INDEX IF NOT EXISTS idx_publicaciones_estado_fecha ON publicaciones (estado, fecha_publicacion DESC);
CREATE INDEX IF NOT EXISTS idx_fotos_publicacion_orden ON fotos_publicacion (publicacion_id, orden);
CREATE INDEX IF NOT EXISTS idx_favoritos_publicacion ON favoritos (publicacion_id);
CREATE INDEX IF NOT EXISTS idx_consultas_publicacion ON consultas (publicacion_id);
```
- **Sin `CHECK` en `tipo_carroceria` ni `zona`** (a diferencia de las columnas enum viejas): agregar un valor al enum después no exigirá otra migración. `ddl-auto: validate` no mira los `CHECK`. Consecuencia que el planner debe saber: las columnas enum **viejas** (`combustible`, `condicion`, `estado`, `transmision`, `rol`) sí tienen `CHECK` — sumar un valor a esos enums en el futuro requiere una migración que reemplace la constraint.
- Los índices: PostgreSQL **no** indexa las FK; los cinco cubren los joins/lookups reales (fotos de cada card, borrado en cascada de consultas/favoritos, filtro por agencia, estado+fecha). Con cientos de autos son baratos y no hay que justificar más; no se agregan índices por marca/precio (YAGNI hasta medir) `[ASSUMED: A8]`.

**Trampas al derivar scripts de `pg_dump` (si el ejecutor regenera V1 en vez de pegar la de arriba):**
1. `pg_dump` 16.x/17.x reciente emite líneas `\restrict <clave>` / `\unrestrict` (comandos de psql): Flyway las ejecuta como SQL y falla con error de sintaxis. Hay que sacarlas.
2. La cabecera `SELECT pg_catalog.set_config('search_path', '', false);` deja el `search_path` vacío para toda la sesión: las migraciones siguientes fallarían con "no schema has been selected". Hay que sacarla.
3. Agregar `.gitattributes` con `*.sql text eol=lf` y mantener los `.sql` en ASCII (sin acentos) evita sorpresas de checksum/encoding entre Windows y Railway `[ASSUMED: A9 — Flyway calcula el checksum por líneas, así que CRLF no debería importar]`. **Nunca editar una migración ya aplicada**: cambiar el contenido cambia el checksum y `validate-on-migrate` (default) frena el arranque.

### Pattern 2: Contrato del catálogo público (recomendado)

Todos `GET`, públicos (ya cubiertos por `.requestMatchers(HttpMethod.GET, "/api/publicaciones/**", "/api/agencias/**").permitAll()` — `SecurityConfig.java:51`).

| Endpoint | Respuesta | Notas |
|----------|-----------|-------|
| `GET /api/publicaciones` | `PaginaResponse<PublicacionResumenResponse>` | Reemplaza la lista sin paginar. Hoy el front viejo del admin **no** la usa (el panel usa `/admin/publicaciones`); ver Open Question 5 por el orden de deploy |
| `GET /api/publicaciones/facetas` | `FacetasResponse` | Sobre el set visible; acepta solo `agenciaId` opcional |
| `GET /api/publicaciones/destacados?limite=6` | `List<PublicacionResumenResponse>` | `destacado = true` y `estado <> VENDIDO`; `limite` clampado a 1..12 |
| `GET /api/publicaciones/{id}` | `PublicacionResponse` (existente) | Sigue devolviendo **cualquier** estado (D-06); suma `tipoCarroceria`, `precioAnterior`, `oferta`, `agenciaZona`, `fechaVendido` |
| `GET /api/publicaciones/{id}/similares?limite=4` | `List<PublicacionResumenResponse>` | Solo DISPONIBLE; ver abajo |

La ruta literal `/facetas` y `/destacados` gana a `/{id}` (Long) en Spring MVC por especificidad `[ASSUMED: A10 — comportamiento estándar de PathPattern; el test de controller lo confirma]`.

**Parámetros de `GET /api/publicaciones`** (mismo nombre en la URL del front y en la API, así el front reenvía `searchParams` casi sin traducir):

| Param | Tipo | Semántica |
|-------|------|-----------|
| `pagina` | int, base 1, default 1 | `< 1` o inválido → 1 (clamp, no error). Tamaño **fijo** 24 (D-07), no es parámetro |
| `orden` | `relevancia` (default) · `precio_asc` · `precio_desc` · `anio_desc` · `km_asc` | Valor desconocido → `relevancia` (un link viejo no debe romper la página) |
| `busqueda` | string ≤ 60 | Se parte en palabras; cada palabra debe aparecer en `marca + ' ' + modelo` (case-insensitive); `%` `_` `\` escapados |
| `marca`, `modelo`, `color` | lista (repetido `?marca=A&marca=B`), ≤ 20 valores | Igualdad case-insensitive (`lower(col) in (...)`) — son chips exactos, ya no `like` como el filtro viejo |
| `transmision`, `tipo`, `zona`, `estado` | listas de enum | `tipo` = `TipoCarroceria`; `zona` = `ZonaAgencia` de la **agencia**; `estado` = disponibilidad; enum inválido → 400 uniforme (lo resuelve `ResponseEntityExceptionHandler`) |
| `anioMin`, `anioMax`, `kmMax`, `precioMin`, `precioMax` | números | `kmMax` excluye autos sin km cargado |
| `ofertas` | boolean | `precioAnterior IS NOT NULL AND precioAnterior > precio` |
| `agenciaId` | long | Página de agencia (también muestra vendidos ≤ 30 días al final) |

**Respuesta:**
```json
{ "contenido": [ { "id": 1, "marca": "Toyota", "modelo": "Corolla XEI", "anio": 2022, "kilometraje": 42000,
                   "precio": 24500000.00, "precioAnterior": null, "oferta": false, "moneda": "ARS",
                   "estado": "DISPONIBLE", "destacado": true, "transmision": "AUTOMATICA", "tipoCarroceria": "SEDAN",
                   "color": "Blanco", "agenciaId": 1, "agenciaNombre": "Autocity Belgrano", "agenciaSlug": "autocity-belgrano",
                   "zona": "CABA", "fotoPortada": "https://res.cloudinary.com/..." } ],
  "pagina": 1, "tamanio": 24, "totalElementos": 11, "totalPaginas": 1 }
```
`PublicacionResumenResponse` NO incluye `descripcion` (hasta 5000 caracteres) ni la lista de fotos: solo `fotoPortada` (la de menor `orden`). Un `pagina` mayor al total devuelve `contenido: []` y `totalPaginas` real; el front redirige a la última página.

**Visibilidad y orden (el corazón):**
```java
// Source: probado contra Postgres 16 + Hibernate 6.5.3 con un test descartable (eliminado).
// Visible: estado distinto de VENDIDO, o VENDIDO con fecha de venta dentro de los 30 dias.
static Specification<Publicacion> visible(LocalDateTime limiteVendidos) {
    return (root, query, cb) -> cb.or(
            cb.isNull(root.get("estado")),                                   // la columna es nullable
            cb.notEqual(root.get("estado"), EstadoPublicacion.VENDIDO),
            cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaVendido"), limiteVendidos)); // NULL => false => oculto
}

// Orden: SIEMPRE vendidos al final (D-04), y desempate por id para que la paginacion sea estable.
static Specification<Publicacion> conOrden(OrdenCatalogo orden) {
    return (root, query, cb) -> {
        if (query.getResultType() != Long.class && query.getResultType() != long.class) { // no en el count
            List<jakarta.persistence.criteria.Order> o = new ArrayList<>();
            o.add(cb.asc(cb.selectCase().when(cb.equal(root.get("estado"), EstadoPublicacion.VENDIDO), 1).otherwise(0)));
            switch (orden) {
                case PRECIO_ASC  -> o.add(cb.asc(root.get("precio")));
                case PRECIO_DESC -> o.add(cb.desc(root.get("precio")));
                case ANIO_DESC   -> o.add(cb.desc(root.get("anio")));
                case KM_ASC      -> o.add(cb.asc(root.get("kilometraje")));
                default          -> { o.add(cb.desc(root.get("destacado"))); o.add(cb.desc(root.get("fechaPublicacion"))); }
            }
            o.add(cb.desc(root.get("id")));
            query.orderBy(o);
        }
        return null; // sin predicado propio
    };
}
// uso: repo.findAll(Specification.where(visible(...)).and(filtros...).and(conOrden(orden)), PageRequest.of(pagina - 1, 24));
```
**Verificado:** `findAll(spec, PageRequest.of(0,3))` con `Pageable` **sin sort** respetó el `orderBy` del `Specification` (SQL generado: `order by case when p1_0.estado=? then ? else ? end, p1_0.destacado desc, p1_0.fecha_publicacion desc, p1_0.id desc offset ? rows fetch first ? rows only`), la página 2 continuó sin repetir ni saltear, y `getTotalElements()` (query de conteo) dio 5. También compiló y corrió `cb.asc(cb.abs(cb.diff(root.<BigDecimal>get("precio"), valor)))` (orden por cercanía de precio para "similares") y la comparación columna-contra-columna `cb.greaterThan(root.<BigDecimal>get("precioAnterior"), root.<BigDecimal>get("precio"))` (ofertas). `[VERIFIED: ejecución de TmpSpecProbeTest contra localhost:5433]`.

Con `precioAnterior`, `KM_ASC` y `NULL`s: Postgres ordena `NULL` al final en `ASC` y al principio en `DESC`; para `km_asc` los autos sin km quedan al final (correcto); para `precio_*` el precio es `NOT NULL`. `[ASSUMED: A11 — comportamiento estándar de Postgres]`.

**`oferta`** lo calcula el servidor (`precioAnterior != null && precioAnterior.compareTo(precio) > 0`), así la card, el detalle y el filtro usan la misma regla (D-03). **No** rechazar con 400 un `precioAnterior <= precio` (D-03 dice "no es oferta", no "es inválido"); validar solo `@Positive` y `@Digits(integer = 10, fraction = 2)` como `precio`, y mostrar una ayuda en el form. Pendiente de decisión del usuario solo si prefiere rechazarlo (Open Question 6).

**`fecha_vendido`:** la fija `PublicacionService.cambiarEstado` (hoy en `PublicacionService.java:142-148` solo hace `setEstado`):
```java
EstadoPublicacion anterior = publicacion.getEstado();
publicacion.setEstado(request.getEstado());
if (request.getEstado() == EstadoPublicacion.VENDIDO && anterior != EstadoPublicacion.VENDIDO) {
    publicacion.setFechaVendido(LocalDateTime.now(clock));   // inyectar Clock (bean) para poder testear los 30 días
} else if (request.getEstado() != EstadoPublicacion.VENDIDO) {
    publicacion.setFechaVendido(null);                       // si se "des-vende", vuelve al catálogo normal
}
```
El límite de 30 días: `app.catalogo.dias-vendido-visible: 30` (configurable) → `LocalDateTime.now(clock).minusDays(30)`.

**Facetas (`GET /api/publicaciones/facetas`)** — calculadas sobre el set visible, *no* contextuales a los filtros activos (más simple y suficiente):
```json
{ "marcas":    [ {"valor":"Toyota","cantidad":2} ],
  "modelos":   [ {"marca":"Toyota","valor":"Corolla XEI","cantidad":1} ],
  "tipos":     [ {"valor":"SEDAN","cantidad":3} ],
  "zonas":     [ {"valor":"CABA","cantidad":5} ],
  "colores":   [ {"valor":"Blanco","cantidad":3} ],
  "transmisiones": [ {"valor":"AUTOMATICA","cantidad":8} ],
  "estados":   [ {"valor":"DISPONIBLE","cantidad":9} ],
  "anio":      {"min":2019,"max":2024},
  "kilometraje": {"max":89000},
  "precio":    {"min":15600000,"max":47500000,
                "histograma":[ {"desde":15600000,"hasta":17550000,"cantidad":2} ] } }
```
Implementación simple y testeable: una consulta con proyección de columnas sobre el set visible (`select marca, modelo, color, tipoCarroceria, transmision, estado, agencia.zona, anio, kilometraje, precio`) y agrupar/contar en Java; histograma de 16 bins (como el mock: `CANTIDAD_BINS_PRECIO = 16`) también en Java. Es aceptable mientras el inventario sea de cientos a pocos miles de filas; si crece, pasar a `GROUP BY`/`width_bucket` nativo `[ASSUMED: A12]`. Reemplaza lo que hoy calcula el mock (`PRECIO_MIN/MAX`, `HISTOGRAMA_PRECIOS`, `BANDAS_PRECIO`, listas de marcas/modelos/colores/tipos).

**Similares (D-06):** `DISPONIBLE`, `id <> X`, misma `moneda`, `precio` entre 70 % y 130 % del auto, y (`tipoCarroceria = X.tipo` **o** `marca = X.marca`; si el auto no tiene tipo, solo marca), orden por `abs(precio - precioX)` asc, `limite` 4 (clamp 1..8). Si no hay resultados el front oculta la sección.

**Rendimiento / N+1:** `PublicacionMapper.toResponse` toca `agencia` y `fotos` (lazy). Con 24 autos por página eso son ~49 queries. Mitigación: `spring.jpa.properties.hibernate.default_batch_fetch_size: 50` (probado: la página de 3 autos con acceso a agencia y fotos quedó en pocas queries agrupadas) y mapear a `PublicacionResumenResponse` dentro de `@Transactional(readOnly = true)`. **No** usar `JOIN FETCH fotos` junto con paginación (Hibernate pagina en memoria y avisa HHH90003004).

### Pattern 3: Estado del catálogo en la URL (front)

- `searchParams` es la única fuente de verdad (hoy `AutosPage` tiene 14 `useState` sueltos, `AutosPage.jsx:82-101`). Un módulo puro `src/utils/catalogoParams.js` con `leerFiltros(searchParams)` y `aSearchParams(filtros)` (testeable con `node --test`).
- Cambiar un filtro/orden ⇒ resetear `pagina` (sacar el param) y `setSearchParams(next, { replace: true })`; cambiar de página ⇒ `setSearchParams(next)` (push) para que "atrás" vuelva a la página anterior. Texto de búsqueda y precio/año/km: estado local + debounce (~350 ms) antes de escribir en la URL, si no cada tecla dispara un request.
- Cada cambio de URL dispara `api.get('/publicaciones', { params, signal })` con `AbortController` (descartar respuestas viejas; sin esto, una respuesta lenta pisa a una nueva). Los params repetidos se serializan con `URLSearchParams` (no con el `params` por defecto de axios, que serializa arrays como `marca[]=`): usar `paramsSerializer: { indexes: null }` o armar el query string a mano.
- `ScrollToTop` solo reacciona a `pathname` (`ScrollToTop.jsx:6-11`): al paginar hay que hacer `window.scrollTo({ top: 0 })` explícito en `AutosPage` cuando cambia `pagina`.
- Los links existentes siguen funcionando: `LogoMarca` y la Home generan `/autos?marca=Toyota`, `/autos?precioMin=..&precioMax=..` y `/autos?busqueda=..` (los nombres coinciden con la API).
- Etiquetas de enums en un solo módulo `src/utils/etiquetas.js` (Manual/Automática, Sedán/Hatchback/…, CABA/Zona Norte/…, Disponible/Reservado/Vendido) para no duplicarlas en card, detalle, filtros y forms.

### Anti-Patterns to Avoid
- **Aceptar `Pageable`/`sort`/`size` del cliente** en el endpoint público: permite `size=100000` (traer todo) o `sort=<propiedad interna>`. Tamaño fijo y orden mapeado a un enum del servidor.
- **`@Validated` + `@Min/@Max` en el controller** para validar `pagina`: dispara `ConstraintViolationException`/`HandlerMethodValidationException` con manejos distintos; clampar en el servicio es más simple y no cambia el formato de error uniforme.
- **Paginar en el front** (traer todo y cortar): incumple el criterio 2 del ROADMAP.
- **Editar una migración ya aplicada** o mezclar `ddl-auto: update` con Flyway: el esquema diverge entre bases. Con `validate`, un cambio de entidad sin migración rompe el arranque a propósito.
- **Mantener `show-sql`/`ddl-auto: update` "solo por ahora" en el yml base**: si el deploy de Railway no usa el Dockerfile (sin `SPRING_PROFILES_ACTIVE=prod`), esos valores quedarían activos. Los valores seguros van en el **base**, lo cómodo de desarrollo en un perfil `dev` opcional.
- **Tratar `Page` como contrato JSON** (`PageImpl` serializado): Spring Data 3.3 avisa de que no es estable `[CITED: docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html]`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Versionado/aplicación de esquema | Scripts SQL ejecutados a mano o un `schema.sql` propio | Flyway (BOM de Boot) | Historial, checksums, baseline, `validate`; probado acá |
| Escape de `LIKE` | Concatenar la búsqueda del usuario | `cb.like(expr, patron, '\\')` con `%`, `_`, `\` escapados | Evita comodines no deseados y scans caros; el valor ya viaja parametrizado |
| Paginación | Contar y cortar a mano | `JpaSpecificationExecutor.findAll(spec, PageRequest)` | Ya incluye la query de conteo (verificado) |
| Cache de tiempo para tests | `LocalDateTime.now()` suelto | Bean `Clock` inyectado | Probar la regla de los 30 días sin dormir |
| Transformación de imágenes | Redimensionar en el back/front | Transformaciones por URL de Cloudinary (`urlMiniatura`) | Ya existe `src/utils/cloudinary.js`; las fotos suben hasta 10 MB |
| Estado de filtros | Context/Redux propio | `useSearchParams` de react-router | La URL ya debe ser la fuente de verdad (D-07) |
| Tests de lógica pura del front | Instalar vitest/jest | `node --test` (built-in) | Sin dependencias nuevas; QA-V2-01 sigue diferido |
| Healthcheck | Endpoint propio | `/actuator/health` (actuator ya está en el pom) | Estándar, incluye chequeo de la base |

**Key insight:** el riesgo de esta fase no está en la complejidad de las consultas sino en los bordes: base existente sin historial, orden estable al paginar, vendidos sin fecha, `NULL`s en columnas opcionales (`estado`, `kilometraje`, `fechaVendido`) y mocks cuyos nombres de campo (`tipoAuto`, `mecanica`, `colorExterior`, `ubicacion`, `oferta`) no existen en la API real.

## Runtime State Inventory

> Aplica parcialmente: es una migración de gestor de esquema (Hibernate `update` → Flyway) y un cambio de contrato de API; no es un rename de strings.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | La base de producción (Railway Postgres) contiene datos reales en `usuarios`, `agencias`, `publicaciones`, `fotos_publicacion`, `consultas`, `favoritos`; fue creada por `ddl-auto: update` de una versión previa a la Fase 1 y **no** tiene `destacado` ni `public_id` (`git diff dedb4c0 HEAD -- entity/` solo toca `FotoPublicacion` y `Publicacion`). Estado exacto: `[ASSUMED: A1]` | **Data migration**: V2/V3 (aditivas). Checkpoint H1 confirma el esquema real y la cantidad de filas; H2 hace backup; H3 ensaya con copia |
| Live service config | Railway: variables de entorno, builder (Dockerfile vs Nixpacks), healthcheck path, dominio. Vercel: `VITE_API_URL`, dominio. Cloudinary: cuenta con los assets existentes | Configurar/confirmar en el panel (checkpoints H4, H7); nada vive en git |
| OS-registered state | None — el deploy es por contenedor en Railway; no hay tareas del SO ni servicios registrados | None — verificado por inspección del repo (Dockerfile, sin scripts de instalación) |
| Secrets/env vars | Back lee: `PORT`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_MS`, `APP_CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL/PASSWORD/NOMBRE`, `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET`, `SPRING_PROFILES_ACTIVE`. Front: `VITE_API_URL` | Code edit: ninguno renombra variables. Los nombres exactos están en `application.yml` (ver §Runbook) |
| Build artifacts / installed packages | `target/` local con clases viejas; `~/.m2` sin Flyway (ya bajado en esta sesión). Imagen Docker de Railway se reconstruye en cada deploy | `rm -rf target/classes target/test-classes` antes de testear; paso online de resolución en otra máquina |

## Common Pitfalls

### Pitfall 1: Flyway rechaza arrancar sobre la base existente
**What goes wrong:** `Found non-empty schema(s) "public" but no schema history table` al desplegar. `[VERIFIED]`
**Why it happens:** primera vez que Flyway ve la base de producción.
**How to avoid:** `baseline-on-migrate: true` + `baseline-version: 1` en el yml base (o `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` en Railway) y V1 = esquema original.
**Warning signs:** el log del arranque no muestra `Successfully baselined schema with version: 1`.

### Pitfall 2: Baselinear en una versión equivocada
**What goes wrong:** con `baseline-version: 2` (o con V1 = esquema de Fase 1) Flyway saltea V2 en producción, que no tiene `destacado`/`public_id`, y `ddl-auto: validate` falla (`missing column [destacado]`).
**How to avoid:** `baseline-version: 1` y V1 = esquema **previo** a la Fase 1; V2 con `IF NOT EXISTS` cubre también una base que ya tuviera esas columnas. Alternativa descartada (V1 = esquema completo de Fase 1 con `baseline-version: 0` y todo `IF NOT EXISTS`): funciona pero esconde divergencias y no es el patrón canónico.

### Pitfall 3: Paginación inestable
**What goes wrong:** el mismo auto aparece en dos páginas o desaparece.
**Why it happens:** orden con empates (varios destacados con la misma fecha) sin desempate.
**How to avoid:** último criterio de orden siempre `id desc`. Test: dos páginas consecutivas sin solapamiento.

### Pitfall 4: Vendidos sin fecha
**What goes wrong:** autos VENDIDO de antes (sin `fecha_vendido`) quedan para siempre ocultos (o visibles).
**How to avoid:** el `UPDATE ... SET fecha_vendido = LOCALTIMESTAMP` de V3 y el predicado "NULL ⇒ oculto" (falla seguro); el detalle directo (D-06) igual los muestra.

### Pitfall 5: Filtro de precio con dos monedas
**What goes wrong:** `PublicacionRequest` admite `ARS|USD` (`@Pattern(regexp = "ARS|USD")`), pero `precioMin/Max`, el histograma y el orden por precio comparan el número crudo: un auto de USD 20.000 cae al lado de uno de ARS 20.000.000, y la UI hardcodea `$ ` (`PublicacionCard.jsx`, `PublicacionDetallePage.jsx`).
**How to avoid:** mostrar el símbolo según `moneda` (`US$` / `$`) en card y detalle; el filtro/histograma operan sobre el valor crudo y se documenta; si el admin usa USD de verdad, abrir una fase/tarea (Open Question 4). No bloquea la demo (todo ARS).

### Pitfall 6: `ddl-auto: validate` rompe tests Mockito/WebMvc
**What goes wrong:** nada: los tests existentes (`@WebMvcTest`, Mockito) no levantan JPA ni Flyway. Pero un `@SpringBootTest`/`@DataJpaTest` nuevo sin base falla.
**How to avoid:** los tests con Postgres usan la base base descartable y se saltean (`Assumptions`) si no hay Postgres; el comando de la fase exige que corran (`-Ddante.pg.required=true`).

### Pitfall 7: Healthcheck de Railway devuelve 401
**What goes wrong:** `SecurityConfig` termina con `.anyRequest().authenticated()` (`SecurityConfig.java:58`) y no tiene regla para `/actuator/**`; `GET /actuator/health` responde 401 y el deploy nunca se activa si se configuró healthcheck path.
**How to avoid:** `.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()` antes de `anyRequest()`, `management.endpoints.web.exposure.include: health`, `management.endpoint.health.show-details: never`. Test: `get("/actuator/health")` en el slice devuelve 404 (no hay handler en `@WebMvcTest`) y **no** 401.

### Pitfall 8: Imágenes enormes en las cards
**What goes wrong:** 24 cards × foto original de hasta 10 MB (las fotos de la demo vienen de Wikimedia a resolución completa).
**How to avoid:** `urlMiniatura(url, 'c_fill,w_640,h_420,q_auto,f_auto')` en cards, `w_1280` en la foto principal del detalle, `w_160` en thumbnails; `loading="lazy"` y `width/height` para no mover el layout. `urlMiniatura` ya deja intactas las URLs que no son de `res.cloudinary.com`.

### Pitfall 9: CORS en local para la segunda instancia
**What goes wrong:** el front de prueba corre en `http://localhost:5174` pero el back por defecto solo permite `http://localhost:5173` (`application.yml`: `allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:5173}`).
**How to avoid:** al levantar el back local para probar: `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174` (con o sin espacios: ya se recortan). Y el front: `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174` — una variable ya exportada en el proceso tiene prioridad sobre `.env` `[CITED: vite.dev/guide/env-and-mode]`, así el `.env` que apunta a Railway no se usa.

### Pitfall 10: `DataSeeder` y espacios en `ADMIN_EMAIL`
**What goes wrong:** pegar la variable en Railway con un espacio/mayúsculas crea un admin que no puede loguearse (IN-08 de Fase 1, `DataSeeder.java:70-93`: no hace `trim()`/`toLowerCase()`).
**How to avoid:** incluir en esta fase el fix de una línea (`trim().toLowerCase()`) porque el deploy a producción es el primer uso real; si la base de producción ya tiene un ADMIN el seed ni se ejecuta.

### Pitfall 11: Demo duplicada en producción
**What goes wrong:** `sembrar-demo.js` crea los autos siempre de nuevo ("correrlo dos veces los duplica", `scripts/demo/README.md`) y `LIMPIAR=1` borra **todos** los autos y agencias del backend (salvo `dante-automotores`).
**How to avoid:** agregar al script (a) abortar si la API ya tiene algún auto con el mismo `marca+modelo+anio`, salvo `FORZAR=1`; (b) negarse a usar `LIMPIAR=1` cuando `API` no es `localhost` salvo una confirmación explícita (`CONFIRMAR_BORRADO_EN_PRODUCCION=SI`); (c) en agencias reutilizadas, hacer `PUT` para completar `zona`.

### Pitfall 12: Enum con `CHECK` en columnas viejas
Ver nota en V3: sumar valores a `Combustible`/`Condicion`/`Transmision`/`EstadoPublicacion`/`Rol` requiere reemplazar la constraint por migración (Hibernate `update` nunca lo hacía). No afecta a esta fase si no se tocan esos enums.

### Pitfall 13: Respuesta lenta pisa a una nueva en el front
Sin `AbortController`, tipear rápido en la búsqueda o clickear chips muestra resultados de un request anterior. Descartar con `signal`/contador de petición.

## Code Examples

### Test de migración sobre el Postgres local (patrón de base descartable)
```java
// Source: patrón verificado en esta sesión (el test descartable usó el mismo DataJpaTest contra localhost:5433)
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true", "spring.flyway.baseline-version=1"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class PostgresLocalTestBase {
    // @DynamicPropertySource: conectarse a jdbc:postgresql://localhost:5433/postgres (user dante / dante_dev_password),
    // CREATE DATABASE test_<random>; registrar spring.datasource.url apuntando a esa base; DROP DATABASE en @AfterAll
    // (FORCE). Si no se puede conectar: Assumptions.assumeTrue(false) salvo -Ddante.pg.required=true (entonces fail).
}
```
Casos de `MigracionesPostgresTest`: (1) base vacía → V1+V2+V3 y `validate` OK; (2) simular producción: ejecutar `V1__esquema_original.sql` con `ScriptUtils`, insertar usuario/agencia/publicación/foto/favorito/consulta, correr Flyway con baseline ⇒ V2+V3 aplicadas, **los datos siguen**, `fecha_vendido` rellenada para el VENDIDO insertado; (3) re-ejecutar ⇒ sin cambios; (4) Hibernate `validate` arranca contra ambas.

### `PaginaResponse`
```java
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {
    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
```

### Parseo de filtros del front (puro, testeable con `node --test`)
```js
// src/utils/catalogoParams.js
export const FILTROS_LISTA = ['marca', 'modelo', 'color', 'transmision', 'tipo', 'zona', 'estado']
export const FILTROS_ESCALAR = ['busqueda', 'anioMin', 'anioMax', 'kmMax', 'precioMin', 'precioMax', 'ofertas', 'orden']

export function leerFiltros(sp) {
  const f = { pagina: Math.max(1, Number.parseInt(sp.get('pagina') ?? '1', 10) || 1) }
  FILTROS_LISTA.forEach((k) => { f[k] = sp.getAll(k) })
  FILTROS_ESCALAR.forEach((k) => { f[k] = sp.get(k) ?? '' })
  return f
}

export function aSearchParams(f) {
  const sp = new URLSearchParams()
  FILTROS_LISTA.forEach((k) => (f[k] ?? []).forEach((v) => sp.append(k, v)))
  FILTROS_ESCALAR.forEach((k) => { if (f[k]) sp.set(k, f[k]) })
  if (f.pagina > 1) sp.set('pagina', String(f.pagina))
  return sp
}
```
```js
// src/utils/catalogoParams.test.js  ->  `node --test src/utils/`  (cwd = danteautomotores-front)
import test from 'node:test'
import assert from 'node:assert/strict'
import { leerFiltros, aSearchParams } from './catalogoParams.js'
test('ida y vuelta conserva listas repetidas y pagina', () => {
  const sp = new URLSearchParams('marca=Toyota&marca=Ford&pagina=3&orden=precio_asc')
  assert.equal(aSearchParams(leerFiltros(sp)).toString(), 'marca=Toyota&marca=Ford&orden=precio_asc&pagina=3')
})
```

### Cobertura de Cloudinary en `SecretosGuard` (PROD-02, patrón del test existente)
Agregar `@Value("${cloudinary.cloud-name:}")`, `cloudinary.api-key`, `cloudinary.api-secret` a `SecretosGuard` y, en `afterPropertiesSet()`, un `fallarOAvisar("CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY o CLOUDINARY_API_SECRET faltan: sin ellos no se pueden subir fotos")` si alguno está en blanco. El test (`SecretosGuardTest` ya arma el guard con `ReflectionTestUtils.setField`) suma: prod sin Cloudinary ⇒ `IllegalStateException`; dev/sin perfil ⇒ solo avisa; prod con todo ⇒ arranca. **No** hacer una llamada de red a Cloudinary en el arranque (frágil y acopla el deploy a un tercero); "válidas" = presentes y no vacías.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ddl-auto: update` en producción | Migraciones versionadas + `validate` | Esta fase | Esquema reproducible; un cambio de entidad sin migración falla al arrancar |
| `flyway-core` solo (Flyway ≤ 9) | `flyway-core` + `flyway-database-postgresql` | Flyway 10 | Sin el módulo: "Unsupported Database: PostgreSQL" `[CITED: docs.spring.io/spring-boot/3.3/how-to/data-initialization.html]` |
| `Page`/`PageImpl` como JSON | DTO propio o `PagedModel`/`VIA_DTO` | Spring Data 3.1/3.3 | Warning de serialización; se evita con `PaginaResponse` |
| Testcontainers 1.x en Docker ≥ 29 | Testcontainers 2.0.2+ o Postgres local | nov-2025 | TC 1.19.8 (Boot 3.3.5) no conecta con Docker Engine 29; se usa el Postgres del compose |

**Deprecated/outdated:**
- `GET /api/publicaciones` que devuelve `List` completa y `PublicacionService.buscar` con default `DISPONIBLE`: se reemplaza (la nota de STATE.md de Fase 1 ya lo anticipaba: "lo cambia la Fase 2").
- Mocks `catalogoMock.js`, `homeMock.js`, `agenciaMock.js` y las imágenes `public/images/car-*.jpg` que solo ellos usan (borrar las imágenes sin referencias es opcional).
- Campos del mock que no existen en la API: `tipoAuto` → `tipoCarroceria`, `mecanica` → `transmision` (enum `MANUAL|AUTOMATICA`), `colorExterior` → `color`, `ubicacion` → `zona` de la agencia, `oferta` → calculado (`precioAnterior > precio`).

## Inventario del front (CAT-01) — qué hay que cambiar

| Archivo | Hoy | Cambio |
|---------|-----|--------|
| `src/pages/HomePage.jsx` (280 l.) | `USE_MOCK_DATA = true` (l.15); `destacadosMock`, `catalogoMock`, `BANDAS_PRECIO`; autocompletado de `SUGERENCIAS_BASE` con el mock; `buscar()` pide `/publicaciones` con params | Destacados desde `GET /publicaciones/destacados`; sugerencias y bandas de presupuesto desde `facetas` (`marcas`/`modelos`, `precio.min/max`); sacar el botón "Ver todos" que re-busca y reemplazarlo por `<Link to="/autos">`; estados de carga/vacío/error |
| `src/pages/AutosPage.jsx` (573 l.) | Todo en memoria sobre `catalogoMock`; 14 `useState`; `ORDENES` con "Relevancia/Menor precio/Mayor precio/Año" | Estado en la URL (Pattern 3); opciones de filtro y rangos desde `facetas`; sumar orden `Menos km`; paginador numerado; chips conservan la UX; "N resultados" = `totalElementos` |
| `src/pages/AgenciaPage.jsx` (153 l.) | `USE_MOCK_DATA` (l.15) + `agenciaMock`/`publicacionesMock`; `api.get('/publicaciones', {agenciaId})` | `GET /agencias/{slug}` + `GET /publicaciones?agenciaId=&pagina=`; marcas del marquee desde `facetas?agenciaId=`; título "Autos (n)"; paginador |
| `src/pages/PublicacionDetallePage.jsx` (427 l.) | `USE_MOCK_DATA` en l.28, 78, 122, 145 (detalle, favorito y consulta también mockeados); `armarDescripcion()` inventa el texto; campos del mock (`tipoAuto`, `mecanica`, `colorExterior`, `ubicacion`) | Usar `GET /publicaciones/{id}` y `descripcion` real; ficha con `tipoCarroceria`, `transmision`, `color`, `combustible`, `condicion`, zona/agencia; favoritos y consulta contra la API real; **vendido**: aviso "Este auto ya se vendió", ocultar consulta y CTAs, sección "Autos parecidos" desde `/similares`; precio anterior tachado + badge "Oferta" |
| `src/components/PublicacionCard.jsx` (83 l.) | Usa `publicacion.mecanica`, `publicacion.ubicacion`, `publicacion.oferta`, `fotos?.[0]?.url`; badge ya maneja RESERVADO/VENDIDO | Pasar a `transmision` (etiqueta), `zona`/`agenciaNombre`, `fotoPortada` con `urlMiniatura(…, card)`; precio tachado si `oferta`; vendido atenuado |
| `src/components/Footer.jsx` (21 l.) | Sin créditos | Link discreto "Créditos de imágenes" → `/creditos` (D-10) |
| `src/routes/AppRouter.jsx` | — | `<Route path="/creditos" …/>` |
| `src/pages/admin/AdminPublicacionFormPage.jsx` (666 l.) | `FORM_INICIAL` (l.64-77) / `publicacionAForm` (l.79-98) / payload (l.150-159) | Campos `tipoCarroceria` (select "Sin especificar" + enum) y `precioAnterior` (mismo formato que `precio`, con ayuda "Si es mayor al precio, el auto aparece como oferta"); incluirlos en `publicacionAForm` y el payload (`|| null`). **Atención:** el `PUT` reemplaza todo el auto: si `publicacionAForm` no carga los campos nuevos, guardar desde el form los borraría |
| `src/pages/admin/AdminDashboardPage.jsx` (569 l.) | `AGENCIA_INICIAL` (l.9-16), form de crear y de editar agencia, `comenzarEdicionAgencia` (l.114-126) | Select `zona` ("Sin especificar" + enum) en ambos forms y en `comenzarEdicionAgencia`; mostrar la zona en la lista |
| `src/mocks/*` | 3 archivos | Borrar |
| `src/utils/cloudinary.js` | Solo `TRANSFORMACION_MINIATURA` (160×120) | Agregar constantes para card y detalle |

Backend equivalente: `PublicacionRequest` suma `tipoCarroceria` (enum, opcional) y `precioAnterior` (BigDecimal, opcional, `@Positive`, `@Digits`); `PublicacionService.crear/actualizar` los copian (hoy `actualizar` asigna campo por campo, `PublicacionService.java:117-137`); `AgenciaRequest`/`AgenciaResponse`/`AgenciaMapper`/`AgenciaService.crear/actualizar` suman `zona` (enum opcional, `ZonaAgencia`); `PublicacionResponse` + `PublicacionMapper.toResponse` suman `tipoCarroceria`, `precioAnterior`, `oferta`, `agenciaZona`, `fechaVendido`. También reforzar en `ConsultaService.crear` que una publicación VENDIDA responde 400 "Este auto ya se vendió" (D-06; defensa en profundidad, hoy `crear` acepta cualquier publicación existente).

### Valores propuestos para los enums nuevos (D-01 / D-02)
- `TipoCarroceria`: `SEDAN`, `HATCHBACK`, `SUV`, `PICKUP`, `UTILITARIO`, `COUPE`, `MONOVOLUMEN`, `FAMILIAR` (rural/station wagon). Los autos existentes quedan `NULL` = "Sin especificar" (se excluyen del filtro por tipo; no rompen nada). Etiquetas: Sedán, Hatchback, SUV, Pickup, Utilitario, Coupé, Monovolumen, Familiar. `[ASSUMED: A5 — lista pedida como "criterio del planner"]`
- `ZonaAgencia`: `CABA`, `ZONA_NORTE`, `ZONA_SUR`, `ZONA_OESTE`, `INTERIOR` (los cinco de D-02). `NULL` = "Sin especificar". Las facetas solo listan las zonas que tienen autos visibles, así que "Zona Sur"/"Interior" no aparecen hasta que haya una agencia ahí.
- Demo (D-09), sugerido: Autocity Belgrano → CABA; Norte Motors (San Isidro) → ZONA_NORTE; Premium Hub (Figueroa Alcorta) → CABA; Rivadavia Cars → CABA; Punto Auto (Vicente López) → ZONA_NORTE; Garage 21 (Ramos Mejía) → ZONA_OESTE. Tipos: Corolla SEDAN, Compass SUV, Golf HATCHBACK, Territory SUV, 320i SEDAN, 208 HATCHBACK (los dos), Onix Premier SEDAN (las fotos son del Onix Plus), Ranger PICKUP, Cronos SEDAN, Kangoo UTILITARIO. `precioAnterior` en 3 autos (p. ej. Onix 21.500.000, 208 de Garage 21 23.900.000, Ranger 44.900.000) para que el filtro "ofertas" tenga resultados; el script debe pasar `tipoCarroceria`/`precioAnterior` en el `POST /publicaciones` y `zona` en `POST /agencias` (y `PUT` si la agencia ya existía).

## PROD-02 / PROD-04 — estado real y qué falta

**Ya hecho (Fase 1), `[VERIFIED: SecretosGuard.java:35,47-57,69-71]`:**
- `PERFILES_DE_DESARROLLO = Set.of("dev", "local", "test")`; con cualquier otro perfil (o mezcla) un secreto JWT en blanco/de ejemplo (`JWT_SECRET_POR_DEFECTO = "CAMBIAR_ESTE_SECRETO_POR_UNO_PROPIO_DE_AL_MENOS_32_CARACTERES"`) o de menos de 32 bytes, o la clave de DB de desarrollo (`DB_PASSWORD_POR_DEFECTO = "dante_dev_password"`), aborta el arranque con `IllegalStateException`. Sin perfiles activos cuenta como desarrollo (solo avisa).
- CORS: `SecurityConfig.java:74-77` hace `allowedOrigins.split(",")` + `.map(String::trim)` + `.filter(origen -> !origen.isEmpty())`; `CorsOrigenesTest` lo cubre. La mitad "CORS acepta espacios" de PROD-02 está cerrada.
- `Dockerfile:15` → `ENV SPRING_PROFILES_ACTIVE=prod`.
- El WR-12 que figuraba como diferido en `01-VERIFICATION.md` (guard fail-open) quedó corregido por el commit `afcd4c4` (anterior a esa verificación inicial); el código actual es el estricto.

**Falta:**
1. Exigir `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` en perfiles no-dev (hoy `application.yml:44-46` los defaultea a vacío y `CloudinaryConfig` crea el bean igual) — ver ejemplo.
2. `ddl-auto: update` y `show-sql: true` están hoy en `application.yml:20-21` para todos los perfiles → `validate` / `false` en el base.
3. Healthcheck público (Pitfall 7).
4. Que el servicio de Railway realmente corra con perfil `prod`: el Dockerfile lo fija, pero si Railway construye con otro builder (Nixpacks/Railpack) no hay perfil y el guard queda permisivo. Defensa: definir `SPRING_PROFILES_ACTIVE=prod` también como variable del servicio (checkpoint H4).
5. Documentación: README (IN-07: puerto 5433 en `docker-compose.yml`, "JDK 25" vs Java 21 del Dockerfile/pom; sumar sección de migraciones y del runbook).

## Runbook de producción — checkpoints humanos

> Todo lo que sigue requiere acceso al panel de Railway/Vercel o credenciales reales. El ejecutor **no** debe pedir ni escribir secretos en archivos del repo; cada paso es `checkpoint:human-action` con verificación posterior. Las verificaciones que tocan producción son **de solo lectura** y están marcadas; las pruebas funcionales de desarrollo se hacen contra `localhost`.

**H1 — Relevar la base de producción (solo lectura).** Railway → proyecto → servicio **Postgres** → pestaña *Data* (o *Query*) y ejecutar:
```sql
SELECT version();
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name, ordinal_position;
SELECT 'usuarios' t, count(*) FROM usuarios UNION ALL SELECT 'agencias', count(*) FROM agencias
UNION ALL SELECT 'publicaciones', count(*) FROM publicaciones UNION ALL SELECT 'fotos_publicacion', count(*) FROM fotos_publicacion
UNION ALL SELECT 'consultas', count(*) FROM consultas UNION ALL SELECT 'favoritos', count(*) FROM favoritos;
SELECT estado, count(*) FROM publicaciones GROUP BY estado;
SELECT id, email, rol FROM usuarios WHERE rol = 'ADMIN';
SELECT to_regclass('public.flyway_schema_history');   -- debe dar NULL
```
Comparar las columnas con el `V1` de arriba: esperado = V1 sin `destacado` ni `public_id`. Si hay **columnas o tablas distintas/extra**, avisar antes de seguir (el ejecutor ajusta V2/V3, no V1). Anotar: versión de Postgres (para H2/H3 y Open Question 3) y si ya hay un ADMIN (si no hay, hay que definir `ADMIN_*`).

**H2 — Backup (obligatorio antes del primer arranque con Flyway).** (a) Si el servicio Postgres tiene la pestaña *Backups*, activar un snapshot y crear uno manual `[CITED: docs.railway.com/guides/postgres-backups-restores]`. (b) Siempre, además, un volcado lógico desde esta máquina (no hay `pg_dump` local; usar la imagen oficial con la **misma versión mayor** que `SELECT version()`; `DATABASE_PUBLIC_URL` sale de las variables del servicio Postgres):
```bash
# Git Bash, en una carpeta FUERA del repo (el volcado trae hashes de contraseña y datos personales: no se commitea)
MSYS_NO_PATHCONV=1 docker run --rm -v "$PWD:/backup" postgres:16-alpine \
  pg_dump "$DATABASE_PUBLIC_URL" --format=custom --no-owner --file=/backup/dante-prod-AAAAMMDD.dump
```
Agregar `*.dump` al `.gitignore` del back. Verificar que no está vacío (`ls -l`) y que restaura (H3).

**H3 — Ensayo con copia de producción (recomendado, es el mejor seguro).** Restaurar el dump en una base descartable del Postgres local y arrancar el back **nuevo** contra ella:
```bash
docker exec danteautomotores-db psql -U dante -d postgres -c "CREATE DATABASE prod_copia"
docker exec -i danteautomotores-db pg_restore -U dante -d prod_copia --no-owner < dante-prod-AAAAMMDD.dump
# Back local contra la copia (NO contra Railway):
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/prod_copia mvn -B -Djava.version=17 spring-boot:run
```
Esperar en el log: `Successfully baselined schema with version: 1`, `Migrating schema "public" to version "2 - ..."`, `"3 - ..."`, `Started DanteAutomotoresApplication` y ningún error de `SchemaManagementException` (validate). Después `DROP DATABASE prod_copia`. Si algo falla acá, **se arregla antes de tocar Railway**.

**H4 — Variables en Railway** (servicio del back → *Variables*). Nombres exactos `[VERIFIED: application.yml:2,12-14,33-36,39-41,44-46 y Dockerfile:15]`:
| Variable | Valor / nota |
|----------|--------------|
| `SPRING_PROFILES_ACTIVE` | `prod` (redundante con el Dockerfile; protege si el builder no es Dockerfile) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<PGHOST>:<PGPORT>/<PGDATABASE>` (formato JDBC, no el `postgresql://` de `DATABASE_URL`); usar las variables de referencia del servicio Postgres |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | las de Railway; la clave **no** puede ser `dante_dev_password` |
| `APP_JWT_SECRET` | ≥ 32 caracteres, aleatorio, distinto del valor de ejemplo (p. ej. `openssl rand -base64 48`). **Cambiarlo invalida las sesiones activas** (los tokens viejos dan 401): aceptable |
| `APP_CORS_ALLOWED_ORIGINS` | origen(es) exactos del front con esquema, sin `/` final ni path, separados por coma: `https://<proyecto>.vercel.app` (+ dominio propio si hay). Sin comodines |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | las de la cuenta (obligatorias desde esta fase) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NOMBRE` | solo hacen falta si H1 no mostró ningún ADMIN; sin espacios; contraseña ≥ 8 |
| `APP_JWT_EXPIRATION_MS`, `PORT` | opcionales / los inyecta Railway |
Además *Settings → Deploy → Healthcheck Path* = `/actuator/health` (recién después de desplegar el código que lo habilita). Verificar *Settings → Build* (builder Dockerfile).

**H5 — Deploy del back y lectura de logs.** Disparar el deploy (push a la rama que Railway vigila o *Redeploy*). Con healthcheck configurado, Railway mantiene el deploy anterior hasta que el nuevo responde 2xx (timeout por defecto 300 s) `[CITED: docs.railway.com/deployments/healthchecks]`; Flyway corre dentro de ese margen. Líneas esperadas en *Deploy Logs*: `Successfully baselined schema with version: 1` → `Successfully applied 2 migrations to schema "public", now at version v3` → `Started DanteAutomotoresApplication`. Aviso aceptable: `Flyway upgrade recommended: PostgreSQL N is newer than this version of Flyway` (solo si el PG es ≥ 17).

**H6 — Verificación posterior (solo lectura contra producción):**
```bash
curl -s -o /dev/null -w "%{http_code}\n" https://danteautomotores-back-production.up.railway.app/actuator/health      # 200
curl -s "https://danteautomotores-back-production.up.railway.app/api/publicaciones?pagina=1" | head -c 400                 # {"contenido":[...
curl -s -i -X OPTIONS -H "Origin: https://<vercel>" -H "Access-Control-Request-Method: GET" \
  https://danteautomotores-back-production.up.railway.app/api/publicaciones | grep -i access-control-allow-origin          # = el origen
```

**H7 — Vercel (front).** Proyecto → *Settings → Environment Variables* → `VITE_API_URL` = `https://danteautomotores-back-production.up.railway.app/api` para *Production* (es una variable de **build**: Vite la inlinea al compilar, hay que *redeployar* el front después de cambiarla `[CITED: vite.dev/guide/env-and-mode]`). `vercel.json` ya reescribe todo a `/index.html` (SPA). Confirmar el dominio de producción para `APP_CORS_ALLOWED_ORIGINS`. Los deploys de preview tienen otra URL y quedarán bloqueados por CORS (esperado; no usar comodín `*.vercel.app`).

**H8 — Cargar la demo (D-09), con las credenciales del admin de producción tipeadas por el usuario** (el agente no las ve):
```bash
node scripts/demo/bajar-fotos.js
API=https://danteautomotores-back-production.up.railway.app/api ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```
(**Nunca** `LIMPIAR=1` contra producción.) Controlar en el panel que haya 11 autos con 5 fotos y en `/autos` que aparezcan.

**H9 — Reversibilidad / rollback.** Las tres migraciones son **aditivas** (columnas nullable o con default, índices, tabla de historial): la versión anterior del back sigue funcionando contra la base migrada (Hibernate `update` ignora columnas desconocidas y `destacado` tiene `DEFAULT false`). Por lo tanto el rollback normal es *Railway → Deployments → deploy anterior → Rollback/Redeploy*, sin tocar la base. El rollback "duro" (restaurar el dump de H2 con `pg_restore --clean`) pierde lo escrito después del backup; solo si la base quedó inconsistente. **Decisión explícita antes de H5:** confirmar que H2 y H3 están hechos.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker Engine | Postgres local, ensayo H3, dumps | ✓ | 29.6.2 (API 1.55, min 1.40) | — |
| Postgres local (docker-compose, puerto 5433) | tests con Postgres, verificación local | ✓ | 16.15 (contenedor `danteautomotores-db` arriba) | `docker compose up -d` |
| `psql`/`pg_dump` en el host | — | ✗ | — | `docker exec danteautomotores-db psql/pg_dump …` o imagen `postgres:<N>-alpine` |
| JDK | build/tests | ⚠ solo 17 | 17 (`/c/Program Files/Java/jdk-17`) | `-Djava.version=17` (no tocar el `pom.xml`) |
| Maven | build/tests | ✓ (no está en PATH) | 3.9.16 en `/c/Users/toto/.maven/maven-3.9.16/bin` | `export PATH=...` |
| Flyway en `~/.m2` | compilar/tests | ✓ ahora (se bajó en esta sesión) | 10.10.0 | `mvn -B -Djava.version=17 -DskipTests test-compile` (online) |
| Node / npm | build del front, `node --test`, script de demo | ✓ | Node v24.14.1 | — |
| Railway CLI / Vercel CLI | deploy | ✗ | — | Paneles web (checkpoints humanos) |
| Imagen Docker `flyway/flyway:10.10.0` | ensayo manual de migraciones | ✓ (descargada en esta sesión) | 10.10.0 | no necesaria para el plan |
| Acceso a internet (Maven Central, Docker Hub, Wikimedia) | resolución online, `bajar-fotos.js` | ✓ | — | — |

**Missing dependencies with no fallback:** ninguna (el acceso al panel de Railway/Vercel es del usuario y está modelado como checkpoints humanos).
**Missing dependencies with fallback:** JDK 21 (usar `-Djava.version=17`), `pg_dump` local (usar Docker).

## Validation Architecture

> `workflow.nyquist_validation: true` en `.planning/config.json`.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + MockMvc/`spring-security-test` (`spring-boot-starter-test`, Boot 3.3.5); tests nuevos contra Postgres con `@DataJpaTest` + Flyway; front: `node:test` (built-in) + `npm run build` |
| Config file | ninguno (`src/test/resources` no existe); los tests Postgres fijan sus propiedades por `@DynamicPropertySource` |
| Quick run command | `mvn -B -o -Djava.version=17 test -Dtest=<Clase>` (con `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"` y `rm -rf target/classes target/test-classes` si hay error de `class file version`) |
| Full suite command | `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test` (con el Postgres del compose arriba) **+** `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` **+** `node --test` en `src/utils/` del front |
| Baseline | 150 tests verdes al cierre de Fase 1 `[CITED: 01-VERIFICATION.md]` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PROD-04 | V1+V2+V3 sobre base vacía; simulación de producción (V1 sin historial + datos) + baseline ⇒ V2/V3, datos intactos, `fecha_vendido` rellena; `validate` pasa | integración PG | `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test -Dtest=MigracionesPostgresTest` | ❌ Wave 0 |
| PROD-04 | El yml base no contiene `ddl-auto: update` ni `show-sql: true` | estático | `! grep -rnE "ddl-auto: update|show-sql: true" src/main/resources` | ✅ (comando) |
| PROD-02 | Guard falla en perfil `prod` sin Cloudinary; avisa en dev; arranca con todo | unit | `mvn … test -Dtest=SecretosGuardTest` | ✅ existe, ampliar |
| PROD-02 | CORS acepta orígenes con espacios | slice | `mvn … test -Dtest=CorsOrigenesTest` | ✅ existe |
| PROD-02/04 | `/actuator/health` no exige token | slice | `mvn … test -Dtest=SeguridadErroresTest` (caso nuevo) | ✅ ampliar |
| CAT-02 | Filtros combinados, multivalor, rangos, ofertas, búsqueda por tokens (con `%`/`_` escapados) | integración PG | `… test -Dtest=CatalogoPostgresTest` | ❌ Wave 0 |
| CAT-02 | Orden por defecto: vendidos al final, destacados primero, desempate por `id`; sin solapamiento entre páginas; tamaño 24 | integración PG | `… test -Dtest=CatalogoPostgresTest` | ❌ Wave 0 |
| CAT-02 | Clamps: `pagina<1`→1, `orden` desconocido→relevancia, listas > 20 recortadas, `busqueda` > 60 recortada; enum inválido → 400 uniforme; sin `Pageable` del cliente | slice + unit | `… test -Dtest=PublicacionControllerCatalogoTest,CatalogoServiceTest` | ❌ Wave 0 |
| CAT-02 | Facetas: conteos, rangos, histograma de 16 bins | unit (Mockito) + PG | `… test -Dtest=CatalogoServiceTest` | ❌ Wave 0 |
| CAT-03 | `destacados` excluye VENDIDO y respeta `limite` (clamp 1..12) | unit + PG | `… test -Dtest=CatalogoServiceTest,CatalogoPostgresTest` | ❌ Wave 0 |
| CAT-04 | `cambiarEstado` fija `fechaVendido` al pasar a VENDIDO (con `Clock` fijo), la conserva si ya era VENDIDO y la limpia al salir; vendido > 30 días no aparece en el listado pero `GET /{id}` sí lo devuelve | unit + PG | `… test -Dtest=PublicacionServiceTest,CatalogoPostgresTest` | ✅ ampliar / ❌ |
| CAT-04 | Similares: solo DISPONIBLE, excluye el propio, mismo tipo o marca, precio ±30 % | PG | `… test -Dtest=CatalogoPostgresTest` | ❌ Wave 0 |
| D-03 | `oferta` y filtro `ofertas` usan la misma regla (`precioAnterior > precio`); `precioAnterior <= precio` no es oferta | unit + PG | `… test -Dtest=PublicacionMapperTest,CatalogoPostgresTest` | ❌ Wave 0 |
| D-06 | `ConsultaService.crear` sobre VENDIDA → 400 | unit | `… test -Dtest=ConsultaServiceTest` | ❌ Wave 0 |
| CAT-01 | No quedan mocks ni `USE_MOCK_DATA` en el front | estático + build | `! grep -rnE "USE_MOCK_DATA|mocks/" C:/Users/toto/Desktop/work/danteautomotores-front/src` y `npm --prefix … run build` | ✅ (comando) |
| CAT-02 (front) | Parseo/serialización de filtros de la URL ida y vuelta | unit `node:test` | `node --test C:/Users/toto/Desktop/work/danteautomotores-front/src/utils/` | ❌ Wave 0 |
| CAT-01..04 (E2E local) | Home/Autos/Agencia/Detalle con datos reales contra el back local | humo con Node | `API=http://localhost:8080/api node scripts/verify/catalogo-humo.js` (script nuevo: GET paginado, facetas, destacados, detalle de vendido, similares) | ❌ Wave 0 |
| CAT-01..04 (navegador) | Navegación visual en `http://localhost:5174` (front de prueba) contra el back local con `APP_CORS_ALLOWED_ORIGINS` ampliado | manual (UAT, `human_verify_mode: end-of-phase`) | ver Pitfall 9 | — |

### Sampling Rate
- **Per task commit:** `mvn -B -o -Djava.version=17 test -Dtest=<clase de la tarea>` (+ `npm run build` si toca el front).
- **Per wave merge:** suite completa del back con `-Ddante.pg.required=true` + build del front + `node --test`.
- **Phase gate:** todo verde + H3 (ensayo con copia) hecho + UAT manual en el front local; después los checkpoints de despliegue.

### Wave 0 Gaps
- [ ] `src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java` — base descartable en `localhost:5433` (crea/borra `test_<random>`; `Assumptions` salvo `-Ddante.pg.required=true`).
- [ ] `MigracionesPostgresTest`, `CatalogoPostgresTest` — cubren PROD-04 y CAT-02/03/04.
- [ ] `CatalogoServiceTest`, `PublicacionControllerCatalogoTest`, `PublicacionMapperTest`, `ConsultaServiceTest` — Mockito/slice (corren offline sin Postgres).
- [ ] Bean `Clock` (`ClockConfig`) para poder fijar la hora en tests.
- [ ] `scripts/verify/catalogo-humo.js` — humo del API local (nunca contra producción).
- [ ] Front: `src/utils/catalogoParams.test.js` (con `node --test`).
- [ ] Dependencias Flyway en `pom.xml` + resolución online (`test-compile` sin `-o`) — Wave 0 de la pista de migraciones.
- [ ] `.gitattributes` (`*.sql text eol=lf`) y `*.dump` en `.gitignore`.

## Security Domain

> `security_enforcement` habilitado (ASVS nivel 1, `security_block_on: high`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (sin cambios; JWT de Fase 1) | — |
| V3 Session Management | no (stateless; ojo: rotar `APP_JWT_SECRET` invalida tokens) | — |
| V4 Access Control | sí | Endpoints de catálogo son `GET` públicos (`SecurityConfig.java:51`); el DTO público no expone `admin` ni datos de usuario; admin sigue bajo `hasRole("ADMIN")`; `/actuator` solo `health` |
| V5 Input Validation | sí | Todos los query params se normalizan: enums tipados, `pagina` clamp, tamaño de página fijo, listas ≤ 20, `busqueda` ≤ 60 con escape de `LIKE`, `orden` por lista blanca; Criteria API parametrizada (sin concatenar SQL) |
| V6 Cryptography | no | JWT/BCrypt de Fase 1; no se agrega criptografía propia |
| V7 Error Handling & Logging | sí | Mantener el formato uniforme de errores; `show-sql: false`; no loguear secretos; `management.endpoint.health.show-details: never` |
| V8 Data Protection | sí | Dumps de producción contienen hashes y datos personales: fuera del repo (`*.dump` en `.gitignore`), borrar la copia local tras el ensayo |
| V14 Configuration | sí | Secretos y Cloudinary obligatorios fuera de dev (`SecretosGuard`); CORS con orígenes exactos (nada de `*`); perfil `prod`; `ddl-auto: validate`; `baseline-on-migrate` apagable tras el primer deploy |

### Known Threat Patterns for Spring Boot público + Postgres + SPA

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Pedir toda la tabla (`size` enorme) o `sort` por propiedad interna | DoS / Info Disclosure | Tamaño fijo 24, sin `Pageable` del cliente, orden por enum del servidor |
| `LIKE` con comodines `%`/`_` y cadenas largas (scan caro) | DoS | Escape + tope de 60 caracteres + tokenización; Postgres con cientos de filas lo tolera `[ASSUMED]` |
| Listas enormes en `?marca=` repetido | DoS | Tope de 20 valores por filtro |
| Inyección SQL por filtros | Tampering | Criteria API / parámetros enlazados; nada de SQL concatenado; la única SQL nativa (`set_config`) no recibe input |
| CORS demasiado abierto | Spoofing / Info Disclosure | `APP_CORS_ALLOWED_ORIGINS` exacto; sin comodín `*.vercel.app` (con `allowCredentials(true)`) |
| Health expuesto con detalles | Info Disclosure | `show-details: never`, solo `health` |
| Secretos por defecto en prod | Elevation of Privilege | `SecretosGuard` (JWT ≥ 32 B, DB, Cloudinary) + `SPRING_PROFILES_ACTIVE=prod` explícito |
| Migración contra la base equivocada | Tampering | `baseline-on-migrate` solo para el primer deploy; verificar `SPRING_DATASOURCE_URL` en H4; backup H2 |
| Consulta sobre un auto vendido (spam / datos inconsistentes) | Tampering | Rechazo 400 en `ConsultaService.crear` (el front además oculta el form) |
| XSS por `descripcion` | Tampering | React escapa texto; `grep` confirma que el front no usa `dangerouslySetInnerHTML` ni `innerHTML` `[VERIFIED: grep en src/ sin resultados]` |
| Fotos de terceros (licencias) | Cumplimiento | Página de créditos `/creditos` enlazada desde el footer (D-10) |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | La base de producción tiene exactamente el esquema "previo a la Fase 1" (V1 menos nada) y no tiene `flyway_schema_history` | Runtime State, Pattern 1 | Si tiene columnas extra o faltantes, V2/V3 siguen siendo idempotentes pero `validate` podría fallar; lo detecta H1/H3 antes de producción |
| A2 | Railway construye el back con el `Dockerfile` (perfil `prod` por ENV) | PROD-02, H4 | Sin perfil, `SecretosGuard` queda permisivo; se mitiga fijando `SPRING_PROFILES_ACTIVE=prod` en variables |
| A3 | El Postgres de Railway es 16 o 17 (versión por confirmar en H1) | Standard Stack, H2 | Con ≥ 17, Flyway 10.10.0 avisa (warning); la imagen de `pg_dump` debe coincidir en versión mayor |
| A4 | El panel de Railway tiene la pestaña *Backups* para el servicio Postgres (depende del plan) | H2 | Si no, el `pg_dump` manual del paso (b) es la única copia: es obligatorio igual |
| A5 | Listas de `TipoCarroceria` y `ZonaAgencia` propuestas cubren el mercado argentino | Valores propuestos | Costo bajo: agregar valores (sin `CHECK`) no exige migración |
| A6 | Los autos de la demo se mapean a los tipos/zonas/ofertas sugeridos | Valores propuestos | Solo cosmético |
| A7 | El front desplegado hoy en Vercel no llama a `GET /api/publicaciones` desde el panel admin (usa `/admin/publicaciones`) | Pattern 2, Open Q 5 | Si la versión desplegada es anterior a la Fase 1, el admin viejo se rompería entre el deploy del back y el del front |
| A8 | Los cinco índices de V3 alcanzan; no hacen falta índices por marca/precio con cientos de autos | V3 | Consultas lentas a futuro; se mide y se agrega otra migración |
| A9 | El checksum de Flyway ignora diferencias de fin de línea (CRLF/LF) | Pitfalls del Pattern 1 | Un `checksum mismatch` por EOL; se previene con `.gitattributes` |
| A10 | `/facetas` y `/destacados` ganan a `/{id}` por especificidad en Spring MVC | Pattern 2 | 400 por tipo en `/{id}`; lo cubre el test de controller |
| A11 | Orden de `NULL` en Postgres (al final en ASC) coincide con lo deseado para `km_asc` | Pattern 2 | Autos sin km aparecerían al revés; el test de orden lo cubre |
| A12 | Calcular facetas/histograma en Java sobre una proyección alcanza para el volumen esperado | Facetas | Lentitud con miles de filas; migrar a `GROUP BY`/`width_bucket` |
| A13 | Cloudinary free tier absorbe las transformaciones por URL de las 50 fotos de la demo (3 variantes) | Pitfall 8 | Cuota de transformaciones; las variantes se cachean |
| A14 | Fotos del `.env`/dominio: el front de producción usa un único dominio de Vercel | H4/H7 | CORS bloquea el dominio no listado |

## Open Questions

1. **¿Cuál es el estado real de la base de producción?**
   - Sabemos: fue creada por la versión previa a la Fase 1 con `ddl-auto: update`; el diff de entidades indica solo `destacado` y `public_id` de diferencia.
   - No sabemos: versión de Postgres, filas, si ya existe un ADMIN, si algún día corrió una versión intermedia.
   - Recomendación: ejecutar H1 antes de planificar el deploy; ajustar V2/V3 (nunca V1) si aparece algo distinto.
2. **¿Railway construye con el Dockerfile y tiene healthcheck configurado?**
   - Recomendación: fijar `SPRING_PROFILES_ACTIVE=prod` como variable igual, y configurar el healthcheck path **después** de desplegar el código que expone `/actuator/health`.
3. **¿Subir Flyway por encima de 10.10.0 (p. ej. `<flyway.version>10.22.0</flyway.version>`)?**
   - Recomendación por defecto: **no**, quedarse con la versión del BOM; solo hacerlo si H1 muestra Postgres ≥ 17 y el warning molesta. El warning no es un fallo.
4. **Precios en USD vs ARS en filtros/orden.** Default recomendado: mostrar el símbolo correcto por `moneda` en card/detalle y dejar el filtro sobre el valor crudo; documentarlo. Si el admin va a cargar USD de verdad, planificarlo aparte.
5. **Orden de deploy back/front.** Default: desplegar el back y enseguida el front nuevo (misma ventana). Si el front viejo de producción usara `GET /api/publicaciones` como lista, tendría un corte breve; confirmar qué versión del front está desplegada (A7). Alternativa si no se tolera ningún corte: exponer el catálogo nuevo bajo `/api/catalogo` y retirar la lista vieja después.
6. **¿Rechazar `precioAnterior <= precio` con 400?** Default: **no** (D-03 lo trata como "no es oferta"); solo ayuda en el form. Cambiar si el usuario prefiere validación estricta.
7. **Dominio de producción del front** (`*.vercel.app` o dominio propio) para `APP_CORS_ALLOWED_ORIGINS`. Default: pedirlo en H7 y listar solo orígenes exactos.
8. **Agencia semilla "Dante Automotores" sin autos ni zona** aparecerá en `GET /agencias`. Default: dejarla; el admin la borra o la completa (no se oculta nada por código).

## Sources

### Primary (HIGH confidence)
- Código leído esta sesión (con rutas citadas arriba): `application.yml`, `pom.xml`, `Dockerfile`, `docker-compose.yml`, `SecretosGuard.java`, `SecurityConfig.java`, `DataSeeder.java`, `CloudinaryConfig.java`, entidades y enums, `PublicacionService/Controller/Specification/Mapper`, `AgenciaService/Controller`, `GlobalExceptionHandler`, tests existentes; front: `AutosPage`, `HomePage`, `AgenciaPage`, `PublicacionDetallePage`, `PublicacionCard`, `Footer`, `AppRouter`, `ScrollToTop`, `api.js`, `cloudinary.js`, `errores.js`, forms admin, mocks, `vercel.json`, `.env.example`.
- Ejecución local: Flyway 10.10.0 (imagen oficial) contra Postgres 16.15 del compose (baseline, base vacía, idempotencia, error sin baseline, `pg_dump` comparados); test descartable `@DataJpaTest` de Hibernate 6.5.3 (orden con `CASE`, conteo, `validate`, `abs(diff)`), eliminado y bases temporales borradas.
- `spring-boot-dependencies-3.3.5.pom` (versiones de Flyway) leído en `~/.m2`.

### Secondary (MEDIUM confidence)
- docs.spring.io/spring-boot/3.3/how-to/data-initialization.html — Flyway, ubicación por defecto, módulo PostgreSQL, ejecución antes de JPA.
- docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html — `VIA_DTO`/`PagedModel` y su JSON.
- documentation.red-gate.com/fd/flyway-baseline-on-migrate-setting-277578974.html — semántica y advertencia de `baselineOnMigrate`.
- docs.railway.com/guides/postgres-backups-restores — backups (volumen y `pg_dump` con `DATABASE_PUBLIC_URL`).
- docs.railway.com/deployments/healthchecks — healthcheck path, 300 s por defecto, activación del deploy nuevo.
- vite.dev/guide/env-and-mode — prioridad de variables ya exportadas, `VITE_` inlineadas en build.

### Tertiary (LOW confidence)
- github.com/flyway/flyway/issues/3242 y búsquedas sobre soporte de PG 17 en Flyway 10.20.0 (solo aviso en versiones anteriores).
- github.com/testcontainers/testcontainers-java/issues/11210, #11235 — incompatibilidad con Docker Engine 29 (no se reprodujo acá: se decidió evitar Testcontainers).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versiones leídas del BOM y resolución verificada en Maven Central.
- Migraciones/baseline: HIGH — ejecutado contra PG 16.15 en los tres escenarios; falta solo confirmar el esquema real de producción (H1).
- Consultas del catálogo: HIGH — orden/conteo/`validate` probados con Hibernate 6.5.3 + Postgres; facetas e histograma son diseño (MEDIUM).
- Front: MEDIUM-HIGH — inventario exacto del código actual; el diseño de estado en URL es estándar pero no se ejecutó.
- Railway/Vercel: MEDIUM — documentación oficial citada; el estado de los paneles del usuario no es observable desde acá.
- Pitfalls: HIGH para los reproducidos (Flyway sin baseline, healthcheck bloqueado por `anyRequest().authenticated()` por lectura del código, CORS 5174), MEDIUM para el resto.

**Research date:** 2026-10-03
**Valid until:** 2026-11-02 (30 días; Flyway/Boot estables. Revalidar H1 y la versión de Postgres de Railway antes del deploy).
