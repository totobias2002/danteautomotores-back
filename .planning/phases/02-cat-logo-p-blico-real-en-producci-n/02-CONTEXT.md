# Phase 2: Catálogo público real en producción - Context

**Gathered:** 2026-10-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Las páginas públicas (Home, `/autos`, `/agencias/:slug` y `/publicaciones/:id`) muestran los autos reales que carga el admin, sin mocks (`catalogoMock.js`, `homeMock.js`, `agenciaMock.js` y todos los `USE_MOCK_DATA` desaparecen). El catálogo se filtra, se ordena y se recorre paginado desde el backend. La Home muestra los destacados. Cada card y cada detalle muestran el estado del auto. Back (Railway) y front (Vercel) quedan desplegados con perfil de producción, migraciones versionadas en lugar de `ddl-auto: update`, sin `show-sql`, y con secretos y Cloudinary obligatorios (PROD-02, PROD-04).

Requisitos: CAT-01, CAT-02, CAT-03, CAT-04, PROD-02, PROD-04.

</domain>

<decisions>
## Implementation Decisions

### Datos nuevos que necesitan los filtros
- **D-01:** El auto tiene un **tipo de carrocería** que el admin elige en el formulario (enum: Sedán, Hatchback, SUV, Pickup, Utilitario, y los que el planner considere necesarios para el mercado argentino, p. ej. Coupé/Monovolumen). El filtro "tipo de auto" de `/autos` funciona contra ese dato. Los autos existentes sin tipo quedan como "sin especificar" y no rompen nada. — **Reversibility:** costly — es una columna nueva en `publicaciones` con migración y un campo nuevo en el contrato de la API y del form.
- **D-02:** La **ubicación** sale de la **agencia**, no del auto: la agencia tiene una zona (CABA, Zona Norte, Zona Sur, Zona Oeste, Interior, u otra lista que el planner justifique) que se elige al crear/editar la agencia, y todos sus autos la heredan para el filtro "ubicación". El admin no la carga auto por auto. — **Reversibility:** costly — columna nueva en `agencias` y campo en el form de agencias.
- **D-03:** Las **ofertas** se modelan con un **precio anterior** opcional en el auto. Si `precioAnterior > precio`, el auto es oferta: la card y el detalle muestran el precio anterior tachado y entra en el filtro "solo ofertas". Si falta o no es mayor, no es oferta. — **Reversibility:** costly — columna nueva + validación + UI en card/detalle/form.

### Vendidos y reservados en el catálogo público
- **D-04:** Los **vendidos** se ven en el catálogo público (`/autos`, página de agencia) con la etiqueta "Vendido", **al final de la lista**, durante **30 días desde que se marcaron como vendidos**; después dejan de aparecer en el catálogo público (siguen en el panel admin). Para esto hace falta registrar la fecha en que el auto pasó a VENDIDO. La Home (destacados) nunca muestra vendidos (decisión de Fase 1).
- **D-05:** Los **reservados** se ven normalmente con la etiqueta "Reservado"; el detalle se puede ver y consultar (por si la reserva se cae).
- **D-06:** Un **link directo a un auto vendido** (p. ej. compartido por WhatsApp) abre la ficha con un aviso "Este auto ya se vendió", sin acción de consulta, y con sugerencias de autos parecidos disponibles (criterio simple: mismo tipo de carrocería o misma marca, precio cercano; el planner lo define). Aplica aunque hayan pasado los 30 días.

### Recorrido del catálogo
- **D-07:** `/autos` usa **páginas numeradas** (estilo Mercado Libre), **24 autos por página**, paginado en el backend. La página actual, los filtros y el orden viven en la URL (`?pagina=2&...`), así un link compartido o el botón "atrás" vuelven al mismo lugar.
- **D-08:** **Orden por defecto:** primero destacados, después los publicados más recientes, y los vendidos al final. El visitante puede cambiar a precio (menor/mayor), año (más nuevo) y kilómetros (menos km). Los filtros existentes de `/autos` (búsqueda, marca/modelo, año, km, precio, transmisión, color, disponibilidad, tipo, ubicación, ofertas) pasan a resolverse en el backend.

### Datos de producción y créditos
- **D-09:** Producción **arranca con la demo**: las 6 agencias y los 11 autos con sus 5 fotos cada uno, cargados con `scripts/demo/sembrar-demo.js` contra la API productiva una vez desplegada (usando el script, no SQL, para que las fotos pasen por Cloudinary y la validación). El admin borra los de demo desde el panel cuando cargue autos reales. El script debe completarse con los datos nuevos (D-01 tipo, D-02 zona de agencia, D-03 precio anterior en alguno para que haya ofertas). — **Reversibility:** reversible — se borran desde el panel.
- **D-10:** Los **créditos de las fotos** (CC BY / CC BY-SA, ver `docs/demo/CREDITOS-FOTOS.md`) se publican en una **página de créditos** enlazada desde el **pie de página** con un link discreto "Créditos de imágenes". No se muestran en cada foto.

### Claude's Discretion
- Migraciones: herramienta (Flyway sugerido en ROADMAP) y línea base que incluya el esquema de Fase 1 (incluye `destacado`, `public_id`, columnas nuevas de esta fase). Cómo se aplica la línea base sobre la base de producción existente sin perder datos.
- Diseño del endpoint paginado (contrato, nombres de parámetros, `Page` de Spring o DTO propio), consultas/índices y cómo se calculan los datos de los filtros (marcas, rangos de precio, histograma) desde el backend.
- Cómo se cierran PROD-02 (secret JWT + Cloudinary obligatorios en prod; parte ya hecha en Fase 1 con `SecretosGuard`) y PROD-04 (perfil prod sin `ddl-auto`/`show-sql`), y la configuración de Railway/Vercel.
- Estados vacíos y de carga del catálogo, y manejo de errores de la API en las páginas públicas.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Alcance y requisitos
- `.planning/ROADMAP.md` §Phase 2 — goal, success criteria y nota de migraciones (Flyway con línea base que incluye Fase 1)
- `.planning/REQUIREMENTS.md` — CAT-01..CAT-04, PROD-02, PROD-04
- `.planning/PROJECT.md` — varias agencias gestionadas por un único admin; UX estilo Kavak/Mercado Libre

### Decisiones previas
- `.planning/phases/01-gesti-n-del-inventario-por-el-admin/01-CONTEXT.md` — D-05..D-07 revisados (varias agencias), destacado independiente del estado
- `.planning/phases/01-gesti-n-del-inventario-por-el-admin/01-REVIEW.md` — hallazgos info abiertos (IN-07 README, IN-08 `ddl-auto`/`show-sql`) que esta fase cierra o afecta
- `.planning/phases/01-gesti-n-del-inventario-por-el-admin/01-VERIFICATION.md` — WR-12 (guard de secretos) diferido a PROD-02

### Demo
- `scripts/demo/README.md`, `scripts/demo/sembrar-demo.js`, `scripts/demo/fotos.json` — carga de la demo (D-09)
- `docs/demo/CREDITOS-FOTOS.md` — créditos a publicar (D-10)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PublicacionController.buscar` + `PublicacionSpecification` (back): búsqueda con filtros marca/modelo/año/precio/estado/agencia; base para el endpoint paginado.
- `PublicacionResponse` ya trae `destacado`, `estado`, `agenciaNombre`, `fotos` ordenadas (portada = orden 0).
- Front `AutosPage.jsx` (573 líneas): UI completa de filtros, chips, histograma de precio, orden y estado vacío, hoy sobre `catalogoMock`; hay que conectarla al backend sin perder la UX.
- Front `src/utils/cloudinary.js` (`urlMiniatura`) — transformación de Cloudinary para miniaturas; reutilizable en cards del catálogo.
- Front `mensajeDeError` (`src/utils/errores.js`) para errores de la API.

### Established Patterns
- Error uniforme `{"error"}` / `{"error","campos"}` (Fase 1, PROD-01); `ReglaDeNegocioException` → 400.
- `open-in-view: false` y servicios `@Transactional` (Fase 1, WR-11): los mappers con relaciones lazy deben correr dentro de transacción.
- `SecretosGuard` estricto fuera de perfiles dev/local/test (Fase 1, WR-12).

### Integration Points
- Rutas públicas: `/`, `/autos`, `/publicaciones/:id`, `/agencias/:slug` en `src/routes/AppRouter.jsx`.
- Mocks a eliminar: `src/mocks/{catalogoMock,homeMock,agenciaMock}.js` y `USE_MOCK_DATA` en HomePage, AutosPage, AgenciaPage, PublicacionDetallePage.
- Formularios admin: `AdminPublicacionFormPage.jsx` (tipo de carrocería, precio anterior) y sección de agencias de `AdminDashboardPage.jsx` (zona).
- El `.env` del front local apunta `VITE_API_URL` al backend de producción en Railway; para pruebas locales se usa otra instancia en el puerto 5174 con `VITE_API_URL=http://localhost:8080/api`.

</code_context>

<specifics>
## Specific Ideas

- El usuario quiere que la demo se vea "cargada", no "pelada": en `/autos` y la Home deben aparecer los mismos autos con fotos que hoy ve en el panel.
- Referencias de UX: Mercado Libre (paginación numerada) y Kavak.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-catalogo-publico-real-en-produccion*
*Context gathered: 2026-10-03*
