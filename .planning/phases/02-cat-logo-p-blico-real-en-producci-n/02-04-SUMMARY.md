---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 04
subsystem: catalogo-publico
tags: [spring-boot, jpa, specification, pagination, facets, postgres, node-script]

requires:
  - phase: 02-01
    provides: CatalogoService, PostgresLocalTestBase, con-back-local.sh, catalogo-humo.js, app.catalogo.dias-vendido-visible
  - phase: 02-03
    provides: tipoCarroceria, precioAnterior, fechaVendido, Agencia.zona, PublicacionMapper.esOferta, ClockConfig
provides:
  - GET /api/publicaciones paginado de a 24 (PaginaResponse base 1) con filtros, orden y vendidos al final resueltos en el servidor
  - GET /api/publicaciones/facetas (opciones con cantidad, rangos de anio y km, rango de precio con histograma de 16 tramos)
  - CatalogoSpecification (visible, conFiltros, conOrden con desempate por id), OrdenCatalogo, FiltrosCatalogo, PaginaResponse, FacetasResponse
affects: [02-05, 02-06, 02-07, 02-08]

plan_head_before: df3ad7f7017d4a14584c5f04aa6bb66145c3fac7
plan_head_after: e8c3937c83d7d96aa2acda8709064c65d4b52ae4

actuals:
  tokens: 15900   # chars/4 sobre las lineas agregadas (63.784 chars: codigo, tests y script)
  tasks: 2
  commits: 2      # MEASURED: git rev-list --count plan_head_before..HEAD (antes del commit de metadata)

tech-stack:
  added: []
  patterns:
    - "Contrato de pagina propio (record PaginaResponse, base 1, nombres en espanol) en vez de serializar Page de Spring Data"
    - "Parametros del cliente neutralizados en el servicio (CatalogoService.normalizar), no con @Min/@Max en el controller: tamano fijo 24, orden por enum, listas de hasta 20, busqueda de hasta 60"
    - "Specification de orden que no ordena la query de conteo (query.getResultType() es Long) y siempre cierra con id desc"
    - "Facetas agrupadas en Java sobre el conjunto visible (inventario de cientos a pocos miles; si crece, GROUP BY)"

key-files:
  created:
    - src/main/java/com/danteautomotores/enums/OrdenCatalogo.java
    - src/main/java/com/danteautomotores/dto/publicacion/FiltrosCatalogo.java
    - src/main/java/com/danteautomotores/dto/publicacion/PaginaResponse.java
    - src/main/java/com/danteautomotores/dto/publicacion/FacetasResponse.java
    - src/main/java/com/danteautomotores/repository/spec/CatalogoSpecification.java
    - src/test/java/com/danteautomotores/service/CatalogoServiceTest.java
    - src/test/java/com/danteautomotores/controller/PublicacionControllerCatalogoTest.java
  modified:
    - src/main/java/com/danteautomotores/service/CatalogoService.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/controller/PublicacionController.java
    - src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java
    - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
    - src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java
    - scripts/verify/catalogo-humo.js
  deleted:
    - src/main/java/com/danteautomotores/repository/spec/PublicacionSpecification.java

key-decisions:
  - "Un numero de pagina ilegible (pagina=abc) cae a 1 con un @InitBinder sobre el campo pagina del controlador; el resto de los parametros numericos o enum invalidos dan 400 uniforme"
  - "Las facetas ordenan marcas, modelos y colores alfabeticamente sin distinguir mayusculas (con la primera grafia vista) y los enums en el orden del enum"
  - "Facetas con histograma por piso exacto de (precio - min) * 16 / rango en BigDecimal, para que la suma de tramos sea siempre el total"

patterns-established:
  - "CatalogoSpecification.visible / conFiltros / conOrden: base de los planes 02-07 (similares) y de cualquier otro listado publico"

requirements-completed: []
requirements-advanced: [CAT-02, CAT-04]   # backend completo; los filtros visibles (02-06) y estado en card y detalle (02-05) completan los requisitos en el sitio

coverage:
  - id: D1
    description: "GET /api/publicaciones devuelve paginas de 24 con la forma {contenido, pagina, tamanio, totalElementos, totalPaginas} (base 1), sin repetir ni saltear autos entre paginas, y nunca el inventario completo (CAT-02, D-07)"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "CatalogoPostgresTest#paginaDe24ConLaSegundaPaginaSinRepetirNiSaltear (Postgres real, 30 autos: 24 + 6, pagina 5 vacia con totalPaginas 2)"
        status: pass
      - kind: integration
        ref: "bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_catalogo04 node scripts/verify/catalogo-humo.js (humo: 17 ok, 0 fallas, 0 skip)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Orden por defecto (no vendidos primero, destacados y mas recientes) y por precio, anio y km con los vendidos siempre al final y los autos sin km al final; desempate por id"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "CatalogoPostgresTest#relevanciaPoneLosVendidosAlFinal..., #ordenPorPrecioAnioYKmConLosVendidosSiempreAlFinal"
        status: pass
    human_judgment: false
  - id: D3
    description: "Un VENDIDO se ve solo 30 dias despues de marcado (y al final); VENDIDO sin fecha no aparece; RESERVADO y estado NULL aparecen con su estado (CAT-04, D-04, D-05, prohibicion de no esconder reservados ni mostrar vendidos como disponibles)"
    requirement: CAT-04
    verification:
      - kind: integration
        ref: "CatalogoPostgresTest#unVendidoSoloApareceSiSeVendioHace30DiasOMenos (29 dias si, 31 no, sin fecha no, RESERVADO y NULL si; Clock fijo 2026-10-01T12:00Z)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Filtros resueltos en el backend: AND entre filtros y OR dentro de cada lista; marca/modelo/color exactos sin distinguir mayusculas; zona de la agencia; ofertas = precioAnterior > precio; kmMax excluye sin km; busqueda por palabras con % y _ como texto; agenciaId"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "CatalogoPostgresTest (8 tests de filtros: marca, zona, ofertas, kmMax, busqueda, busqueda con %/_, agenciaId, tipo+precioMax, rangos)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Parametros fuera de rango neutralizados: pagina < 1 o ilegible a 1, orden desconocido a relevancia, mas de 20 valores recortados, busqueda de 60 caracteres, sin tamano ni sort del cliente, enum invalido 400 con error (T-02-13, T-02-14, T-02-15, T-02-16)"
    requirement: CAT-02
    verification:
      - kind: unit
        ref: "CatalogoServiceTest (normalizar y PageRequest 24 sin Sort) y PublicacionControllerCatalogoTest (tipo=NAVE 400, size/sort ignorados, pagina=abc)"
        status: pass
      - kind: integration
        ref: "catalogo-humo.js: pagina=999 vacio, size=1000&sort=admin.email sin efecto, tipo=NAVE 400"
        status: pass
    human_judgment: false
  - id: D6
    description: "GET /api/publicaciones/facetas (agenciaId opcional) devuelve marcas, modelos, tipos, zonas, colores, transmisiones y estados con cantidad, rango de anio y km y rango de precio con histograma de 16 tramos cuya suma es el total visible, sin contar los vendidos ocultos (T-02-17)"
    requirement: CAT-02
    verification:
      - kind: unit
        ref: "CatalogoServiceTest (histograma: 16 tramos, ultimo incluye el maximo, precios iguales 1 tramo, vacio, suma = total)"
        status: pass
      - kind: integration
        ref: "CatalogoPostgresTest (5 tests de facetas) y humo (histograma 16 o 1 tramos, suma del histograma y de las marcas igual al total del listado)"
        status: pass
    human_judgment: false

duration: 40min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 04: Catalogo publico paginado y facetas en el backend Summary

**`GET /api/publicaciones` pasa de lista completa a pagina de 24 (record `PaginaResponse`, base 1) con filtros, orden estable y la regla de vendidos (30 dias, al final) resueltos en `CatalogoSpecification`, y `GET /api/publicaciones/facetas` devuelve las opciones, los rangos y el histograma de 16 tramos que antes calculaba el mock del front.**

## Performance

- **Duration:** ~40 min
- **Tasks:** 2 (ambas TDD)
- **Files:** 7 creados, 7 modificados, 1 eliminado

## Accomplishments

- `CatalogoSpecification.visible` (estado NULL, no vendido, o vendido con `fechaVendido` dentro de la ventana; VENDIDO sin fecha queda oculto), `conFiltros` (todo en Criteria API con parametros enlazados, LIKE con escape de `\`, `%` y `_`) y `conOrden` (vendidos al final, criterio pedido, `id desc` siempre al final, sin ordenar el conteo).
- `CatalogoService.buscar` normaliza los parametros del cliente antes de armar la consulta (pagina minima 1, orden por enum, 20 valores por lista, busqueda de 60 caracteres) y pide `PageRequest.of(pagina - 1, 24)` sin Sort; mapea a `PublicacionResumenResponse` dentro de la transaccion.
- `CatalogoService.facetas` agrupa en Java sobre el mismo conjunto visible; `histograma` calcula 16 tramos con piso exacto en BigDecimal, el ultimo incluye el maximo y la suma siempre da el total.
- Se retiran `PublicacionService.buscar` y `PublicacionSpecification` (el panel admin sigue con `listarParaAdmin`; el detalle sigue devolviendo cualquier estado).
- `catalogo-humo.js` suma 10 chequeos de listado y facetas.

## Task Commits

1. **Task 1 (TDD): catalogo paginado con filtros, orden y vendidos al final** - `f4a8caa` (feat)
2. **Task 2 (TDD): facetas y humo del listado** - `e8c3937` (feat)

**Plan metadata:** commit docs(02-04) a continuacion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `pagina=abc` habria dado 400 en vez de caer a la pagina 1**
- **Found during:** Task 1 (diseno del controller; la verdad del plan dice "pagina menor a 1 o invalida -> 1")
- **Issue:** con `Integer pagina` en `FiltrosCatalogo`, un valor no numerico falla el binding de `@ModelAttribute` y da 400.
- **Fix:** un `@InitBinder("filtrosCatalogo")` en `PublicacionController` registra un editor tolerante solo para el campo `pagina` (ilegible o vacio -> null, que el servicio lleva a 1). Los demas parametros numericos siguen dando 400 uniforme.
- **Files modified:** src/main/java/com/danteautomotores/controller/PublicacionController.java
- **Verification:** `PublicacionControllerCatalogoTest#unaPaginaIlegibleCaeALaPrimeraEnVezDeRomper`
- **Commit:** f4a8caa

**2. [Rule 3 - Blocking] `TransaccionesServiceTest` buscaba `PublicacionService.buscar`**
- **Found during:** Task 1 (al retirar el metodo)
- **Issue:** `lasLecturasDePublicacionSonReadOnly` recorre una lista de nombres de metodos con `findFirst().orElseThrow()`; "buscar" desaparece con este plan y el test habria fallado.
- **Fix:** se quito "buscar" de la lista (los metodos del catalogo estan cubiertos por el `@Transactional(readOnly = true)` de clase de `CatalogoService`).
- **Files modified:** src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
- **Commit:** f4a8caa

### Other notes (not code deviations)

- **Pasos RED:** el codigo y los tests de cada tarea se escribieron en la misma pasada y se corrieron juntos (sin commit `test(...)` separado ni corrida RED previa); la cobertura de los comportamientos del plan se comprobo igual con las suites en verde.
- **Criterio `grep -c "pagina=999" catalogo-humo.js` imprime 1:** imprime 2 (el nombre del chequeo y la URL). La intencion del criterio (existe el chequeo de `pagina=999`) se cumple.
- **Humo de punta a punta:** sobre la copia de `danteautomotores` (1 auto, sin destacados) dio 13 ok, 0 fallas, 4 skip (los skips son los chequeos por item de destacados, como en planes anteriores); sobre una copia de `dante_uat` (11 autos) 17 ok, 0 fallas, 0 skip. Ambas copias descartables se borraron; no quedan bases `dante_copia*` ni `test_*` ni procesos en 8080/8081.
- **Limitacion conocida (Pitfall 5 del RESEARCH, sin cambios):** filtro y orden por precio comparan el numero crudo aunque haya autos en USD; la UI debe mostrar el simbolo de la moneda (plan 02-05).
- **Requisitos no marcados como completos:** CAT-02 y CAT-04 quedan como avanzados (backend completo); se completan en el sitio con 02-05 (estado en card y detalle) y 02-06 (filtros, orden y paginacion en `/autos`).
- **Cambio de forma de `GET /api/publicaciones`:** de lista a pagina. El unico consumidor es el front propio, que se adapta en 02-06 y se despliega junto con el back (02-08); mientras tanto `/autos` en el front local sigue sobre mocks.

**Total deviations:** 2 auto-fixed (1 Rule 1, 1 Rule 3). **Impact:** ninguno sobre el alcance.

## Known Stubs

None.

## Threat Flags

None. Los endpoints nuevos (`/api/publicaciones` paginado y `/facetas`) estan cubiertos por el threat_model del plan (T-02-13 a T-02-17, todos mitigados y con test).

## Verification Results

| Check | Resultado |
|-------|-----------|
| `mvn -o test -Dtest=CatalogoPostgresTest,CatalogoServiceTest,PublicacionControllerCatalogoTest,GlobalExceptionHandlerTest,PublicacionControllerTest,PublicacionServiceTest,TransaccionesServiceTest` (tarea 1) | 0 fallas, 0 skipped (CatalogoPostgresTest 18, CatalogoServiceTest 8, PublicacionControllerCatalogoTest 6) |
| `mvn -o test -Dtest=CatalogoServiceTest,CatalogoPostgresTest,PublicacionControllerCatalogoTest` (tarea 2) | CatalogoServiceTest 13, CatalogoPostgresTest 23, PublicacionControllerCatalogoTest 8; 0 fallas, 0 skipped |
| Humo sobre copia de `dante_uat` | humo: 17 ok, 0 fallas, 0 skip |
| Humo sobre copia de `danteautomotores` | humo: 13 ok, 0 fallas, 4 skip |
| Suite completa (`-Ddante.pg.required=true`) | 269 tests, 0 fallas, 0 skipped, BUILD SUCCESS |
| Criterios de aceptacion por grep (PublicacionSpecification eliminado, TAMANIO_PAGINA 24 = 1, Pageable en controller = 0, `cb.desc(root.get("id"))` = 1, record PaginaResponse = 1, size/sort/pageable en FiltrosCatalogo = 0, `/facetas` = 1, CANTIDAD_TRAMOS_PRECIO 16 = 1, facetas en humo >= 1) | todos PASS (pagina=999 en humo = 2, ver nota) |

## Next Phase Readiness

Listo para 02-05 (card y detalle: `estado`, `oferta`, `precioAnterior`, `agenciaZona` ya viajan en el resumen y el detalle) y 02-06 (el front puede reenviar `searchParams` casi sin traducir: mismos nombres `pagina`, `orden`, `busqueda`, `marca`, `modelo`, `color`, `transmision`, `tipo`, `zona`, `estado`, `anioMin`, `anioMax`, `kmMax`, `precioMin`, `precioMax`, `ofertas`, `agenciaId`; pagina base 1; las listas se repiten `?marca=A&marca=B`, y axios necesita `paramsSerializer: { indexes: null }`; las facetas reemplazan marcas, modelos, rangos e histograma del mock). `CatalogoSpecification` queda disponible para los similares de 02-07.

## Self-Check: PASSED

- Archivos creados verificados en disco (OrdenCatalogo, FiltrosCatalogo, PaginaResponse, FacetasResponse, CatalogoSpecification, CatalogoServiceTest, PublicacionControllerCatalogoTest) y `PublicacionSpecification` eliminado.
- Commits verificados: `f4a8caa`, `e8c3937`.
