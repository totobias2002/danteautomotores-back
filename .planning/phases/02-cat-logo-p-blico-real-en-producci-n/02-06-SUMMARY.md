---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 06
subsystem: front-listados-publicos
tags: [react, vite, react-router, url-state, pagination, facets, debounce, abortcontroller, node-test]

requires:
  - phase: 02-04
    provides: GET /api/publicaciones paginado (PaginaResponse base 1, 24 por pagina) y GET /api/publicaciones/facetas
  - phase: 02-05
    provides: etiquetas.js (TRANSMISION, TIPO_CARROCERIA, ZONA, ESTADO) y PublicacionCard con estado, oferta y zona
provides:
  - src/utils/catalogoParams.js (leerFiltros, aSearchParams, conFiltro, alternarEnLista, paramsParaApi, paginasVisibles, bandasDePrecio) con 9 tests node:test
  - src/components/Paginador.jsx (paginador numerado accesible, aria-current)
  - /autos contra el backend con filtros, orden y pagina en la URL (debounce 350 ms, AbortController, facetas, orden "Menos km")
  - AgenciaPage con los autos reales paginados y las marcas del carrusel desde las facetas de la agencia
  - HomePage con sugerencias del buscador y accesos por presupuesto desde las facetas, sin datos de prueba
affects: [02-07, 02-08]

plan_head_before: 13aaae7dd8dc5a93d15de83e3c997afd6c15741e
plan_head_after: dac91a8833bb6965250a0b2c67db155a71935584

actuals:
  tokens: 9300    # chars/4 sobre las lineas agregadas del diff en el repo del front (37.309 chars)
  tasks: 3
  commits: 3      # MEASURED: git -C danteautomotores-front rev-list --count 13aaae7..HEAD (los commits de este plan viven en el repo del front)

tech-stack:
  added: []
  patterns:
    - "La URL es la unica fuente de verdad del listado: filtros = useMemo(leerFiltros(searchParams)); cambiar filtro/orden escribe con replace y vuelve a la pagina 1, cambiar de pagina hace push + scrollTo"
    - "Campos con debounce (hook useCampoConDebounce): estado local para lo que se escribe, confirmacion a la URL a los 350 ms y resincronizacion si la URL cambia desde afuera; las confirmaciones leen un ref con el ultimo estado para no pisarse entre si"
    - "Un efecto por cambio de URL con AbortController; en catch se ignora el error si controller.signal.aborted (sin importar axios)"
    - "Parametros de la API como URLSearchParams (claves repetidas marca=A&marca=B) en vez de paramsSerializer"

key-files:
  created:
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/utils/catalogoParams.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/utils/catalogoParams.test.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/components/Paginador.jsx
  modified:
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/AgenciaPage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/AutosPage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/HomePage.jsx

key-decisions:
  - "Paginador: la pagina actual es un <span aria-current=\"page\"> (no clickeable) y las demas son botones con aria-label; asi coincide con el criterio de aceptacion literal y con la practica de Mercado Libre"
  - "paginasVisibles rellena con el numero cuando el salto seria de una sola pagina (4/10 -> 1 2 3 4 5 ... 10) en vez de poner '...' para una sola pagina"
  - "Al sacar una marca en /autos se podan los modelos elegidos que ya no pertenecen a las marcas que quedan (si no, quedaria un chip de modelo sin opcion visible)"
  - "Facetas de /autos y de la Home se piden una vez por visita y sin recalcular con los filtros (supuesto marcado en el plan); las opciones no muestran cantidades para no sugerir que son por filtro"
  - "Las facetas de la Home y del carrusel de agencia fallan en silencio (son accesos rapidos / adorno); /autos las reemplaza por 'Sin opciones por ahora'"

patterns-established:
  - "Filtros del catalogo = URLSearchParams con los mismos nombres que la API (catalogoParams.js), reutilizable por el detalle (similares, 02-07) y cualquier otro listado"

requirements-completed: [CAT-02]
requirements-advanced: [CAT-01]   # Home, Autos y Agencia sin mocks; falta el Detalle (02-07). CAT-03 ya estaba completo desde 02-01

coverage:
  - id: D1
    description: "/autos muestra los autos reales paginados de a 24 (nunca el inventario completo) con paginador numerado, y el contrato front-back (claves repetidas, orden, busqueda, rango de precio, agenciaId, pagina fuera de rango, enum invalido) funciona contra un back real (D-07, CAT-02)"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_uat06 node integracion-06.mjs (usa catalogoParams.js + axios reales): 12 ok, 0 fallas"
        status: pass
      - kind: unit
        ref: "node --test src/utils/catalogoParams.test.js: 9 pass, 0 fail"
        status: pass
      - kind: render
        ref: "render del servidor del Paginador: 1/1 vacio; 5/10 con 1 aria-current y 2 puntos suspensivos; Anterior deshabilitado en la 1 y Siguiente en la ultima"
        status: pass
    human_judgment: true
  - id: D2
    description: "Pagina, filtros y orden viven en la URL; cambiar filtro/orden vuelve a la pagina 1 sin historial; cambiar de pagina agrega una entrada y lleva el scroll arriba; recargar o compartir el link da el mismo listado"
    requirement: CAT-02
    verification:
      - kind: unit
        ref: "catalogoParams.test.js: ida y vuelta, pagina invalida a 1, parametros desconocidos descartados, conFiltro y alternarEnLista vuelven a pagina 1"
        status: pass
      - kind: render
        ref: "render del servidor de AutosPage con /autos?marca=Toyota&marca=Ford&zona=ZONA_NORTE&tipo=SUV&estado=RESERVADO&precioMin=1000000&orden=km_asc&pagina=2: los 6 chips activos con etiquetas en espanol y el orden km_asc seleccionado, salidos solo de la URL"
        status: pass
    human_judgment: true
  - id: D3
    description: "Una respuesta lenta no pisa a una mas nueva: cada cambio de URL cancela el pedido anterior; texto y rangos esperan 350 ms (T-02-22)"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "integracion-06.mjs: abort() sobre un pedido real da signal.aborted y ERR_CANCELED, y el catch lo ignora"
        status: pass
      - kind: build
        ref: "grep: AbortController x2 en AutosPage, DEBOUNCE_MS 350; el debounce y el historial real se ven en el navegador (UAT pendiente)"
        status: pass
    human_judgment: true
  - id: D4
    description: "La pagina de una agencia lista sus autos reales paginados (agenciaId), con el total en el titulo y las marcas del carrusel desde las facetas de esa agencia; sin mock"
    requirement: CAT-01
    verification:
      - kind: integration
        ref: "integracion-06.mjs: agenciaId filtra por agencia (todos los autos con ese agenciaId) y las facetas de la agencia suman el total de la agencia"
        status: pass
      - kind: build
        ref: "grep: agenciaMock 0, USE_MOCK_DATA 0, <Paginador 1, /publicaciones/facetas 1 en AgenciaPage; render del servidor sin datos no rompe"
        status: pass
    human_judgment: true
  - id: D5
    description: "La Home arma sugerencias y accesos por presupuesto desde las facetas reales y no importa datos de prueba (CAT-01, CAT-03)"
    requirement: CAT-01
    verification:
      - kind: integration
        ref: "integracion-06.mjs: las bandas de bandasDePrecio sobre el rango real de /facetas son 4 consecutivas y /publicaciones?precioMin&precioMax de la primera banda devuelve solo autos dentro del rango"
        status: pass
      - kind: build
        ref: "grep: mocks 0, /publicaciones/facetas 1, bandasDePrecio( 1 en HomePage; el render sin facetas oculta 'Buscá por presupuesto'; npm run build verde"
        status: pass
    human_judgment: true
  - id: D6
    description: "Con la API caida o con un error cada pagina muestra un mensaje legible (mensajeDeError) y con 0 resultados un estado vacio con 'Limpiar filtros'; el mensaje nunca expone detalles internos (T-02-23)"
    requirement: CAT-02
    verification:
      - kind: integration
        ref: "integracion-06.mjs: tipo=NAVE da 400 con {error} string, que es lo unico que muestra mensajeDeError"
        status: pass
      - kind: build
        ref: "grep: mensajeDeError en AutosPage y AgenciaPage; render de AutosPage sin API muestra 'Cargando autos...'"
        status: pass
    human_judgment: true

duration: 35min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 06: Listados publicos contra el catalogo real Summary

**`/autos` deja de filtrar un mock en memoria y deriva todo (filtros, orden y pagina) de la URL pidiendole al backend cada pagina de 24, con facetas reales para opciones, rangos e histograma; la pagina de cada agencia lista sus autos reales paginados y la Home arma sugerencias y presupuestos desde las facetas, sin importar ningun dato de prueba.**

## Performance

- **Duration:** ~35 min
- **Tasks:** 3 (Tarea 1 con `tdd="true"`; las otras `auto`)
- **Files:** 3 creados, 3 modificados (todos en `danteautomotores-front`)

## Accomplishments

- `catalogoParams.js`: `FILTROS_LISTA`/`FILTROS_ESCALAR` con los mismos nombres que la API, `leerFiltros`/`aSearchParams` (ida y vuelta; pagina 1 no se escribe; parametros desconocidos descartados), `conFiltro`/`alternarEnLista` (vuelven a la pagina 1), `paramsParaApi` (URLSearchParams con claves repetidas y sin vacios), `paginasVisibles` y `bandasDePrecio`. 9 tests `node:test`, sin dependencias nuevas.
- `Paginador.jsx`: nav accesible (Anterior/Siguiente deshabilitados en los extremos, actual con `aria-current="page"`, "..." no clickeable, nada si hay una sola pagina).
- `AutosPage`: se eliminan los 14 `useState` de filtros, el filtrado/orden en memoria y el import del mock. La URL manda; chips y checkboxes escriben con `replace`; el cambio de pagina hace push + `scrollTo`. Texto y rangos con debounce de 350 ms (hook `useCampoConDebounce` con resincronizacion desde la URL). Un efecto por cambio de URL con `AbortController`. Opciones de Marca (con logos), Modelo (solo de las marcas elegidas), Color, Tipo, Ubicacion, Transmision (ex "Mecanica") y Disponibilidad salen de `/publicaciones/facetas`, igual que los placeholders y limites de precio, el histograma y "Ver rangos de precios". Orden "Menos km". Estados de carga, error y vacio con "Limpiar filtros".
- `AgenciaPage`: sin la rama de datos de prueba; agencia + facetas por slug, listado por pagina (`agenciaId`), titulo "Autos (N)", carrusel de marcas desde las facetas de la agencia, `Paginador` con push y scroll, redireccion a la ultima pagina si la pedida ya no existe.
- `HomePage`: sugerencias (marcas + "marca modelo") y bandas de presupuesto desde las facetas; sin facetas no hay sugerencias ni bloque de presupuesto, sin error visible.

## Task Commits (repo `danteautomotores-front`)

1. **Tarea 1 (TDD): catalogoParams + tests + Paginador + AgenciaPage** - `168f76c` (feat)
2. **Tarea 2: /autos contra el backend con el estado en la URL** - `6803106` (feat)
3. **Tarea 3: Home con sugerencias y presupuesto desde las facetas** - `dac91a8` (feat)

**Plan metadata:** commit docs(02-06) en el repo del back a continuacion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Estado de las confirmaciones con debounce podia pisarse entre campos**
- **Found during:** Tarea 2 (diseno del hook de debounce)
- **Issue:** si dos campos confirman casi juntos (por ejemplo "Ver rangos de precios" fija precio minimo y maximo) y cada uno parte de los `filtros` del ultimo render, el segundo pisa al primero porque el re-render todavia no corrio.
- **Fix:** `filtrosRef` guarda el ultimo estado (se actualiza en el render y en cada `escribir`); todas las confirmaciones, chips y el cambio de pagina parten de ahi. Las bandas de precio usan `fijarYa` (sin esperar el debounce) para ambos extremos.
- **Files modified:** src/pages/AutosPage.jsx
- **Commit:** 6803106

### Plan-compliant adjustments (no son desvios de alcance)

- **Paginador con la actual como `<span>`:** el criterio de aceptacion pide el literal `aria-current="page"` (1 vez), asi que la actual se renderiza aparte y no como boton.
- **Sin `paramsSerializer`:** el mensaje del orquestador (herencia de 02-04) sugeria `paramsSerializer: { indexes: null }`; como los params viajan como `URLSearchParams` (lo que pide el plan), axios ya serializa las claves repetidas. Verificado contra el back real (`marca=BMW&marca=Chevrolet` devolvio la suma de las dos facetas).
- **Poda de modelos al sacar una marca** (no estaba en el plan): ver key-decisions.

**Total deviations:** 1 auto-fixed (Rule 1). **Impact:** ninguno sobre el alcance.

## Pendiente de UAT humano (checkpoint visual no ejecutado)

El `<human-check>` de la Tarea 2 no se corrio: el run es autonomo y no hay runner de navegador. Se reemplazo por el contrato real contra un back local (12 chequeos), el render del servidor de las paginas y los tests de la logica de la URL. **Queda pendiente la verificacion visual humana** (back local con `bash scripts/verify/con-back-local.sh --copia-de danteautomotores dante_copia_uat sleep 900` y el front con `VITE_API_URL=http://localhost:8080/api npx vite --port 5174`; no tocar el `.env` del front): en `/autos` elegir una marca, ordenar por "Menor precio", escribir en la busqueda (un solo pedido tras ~350 ms, no uno por tecla), ir a la pagina 2 si existe, recargar, apretar "atras" y abrir la URL en otra pestana. Esperado: cada cambio pide `GET /api/publicaciones` con los parametros de la URL, marca y orden vuelven a la pagina 1 sin sumar historial, "atras" vuelve a la pagina anterior, las opciones de los filtros son las del catalogo real, "N resultados" coincide con `totalElementos` y los vendidos salen al final con su etiqueta.

## Other notes

- **Sin ESLint:** el front no tiene `eslint.config.*` (como en 02-05); el build de Vite es el chequeo de sintaxis. Se dejaron dos comentarios `eslint-disable-next-line react-hooks/exhaustive-deps` donde las dependencias se omiten a proposito (identidad de `setSearchParams`/`escribir`).
- **Mocks restantes:** `grep -rn "mocks/"` sobre `AutosPage`, `HomePage` y `AgenciaPage` da 0 resultados; solo `PublicacionDetallePage.jsx` sigue importando mocks (plan 02-07). Los archivos `src/mocks/*` siguen en el repo hasta que 02-07/02-08 los retiren.
- **Caveat conocido (RESEARCH Pitfall 5, sin cambios):** filtro y orden por precio comparan el numero crudo aunque haya autos en USD; la card ya muestra el simbolo de la moneda.
- **Higiene:** la base `dante_copia_uat06` y el back local los creo y borro `con-back-local.sh`; no quedan bases `dante_copia*` ni procesos en 8080/8081/5174. No se pusheo nada, no se toco el `.env` del front ni la base `danteautomotores`.
- **Alcance de los requisitos:** CAT-02 se marca completo (backend 02-04 + front 02-06). CAT-01 queda avanzado: falta el Detalle (02-07). CAT-03 ya estaba completo desde 02-01.

## Known Stubs

None. `PublicacionDetallePage` conserva su dato de prueba previo a este plan y pertenece a 02-07.

## Threat Flags

None. T-02-21 (solo se leen parametros conocidos, pagina invalida a 1, el backend vuelve a validar), T-02-22 (debounce de 350 ms y AbortController) y T-02-23 (solo `mensajeDeError`) mitigados.

## Verification Results

| Check | Resultado |
|-------|-----------|
| `node --test src/utils/catalogoParams.test.js` | 9 pass, 0 fail |
| `npm run build` (front) tras cada tarea | verde, "built in" presente |
| Integracion contra back local (copia de `dante_uat`, 11 autos) con `catalogoParams.js` + axios | 12 ok, 0 fallas |
| Render del servidor: Paginador, AutosPage (URL con 6 filtros + orden + pagina), HomePage y AgenciaPage sin datos | OK, sin errores de render |
| Criterios por grep (7 exports, agenciaMock 0, USE_MOCK_DATA 0, `<Paginador` 1 en Agencia y Autos, facetas 1 en las tres paginas, `aria-current="page"` 1, catalogoMock 0, `paramsParaApi(` 1, AbortController 2, km_asc 1, totalElementos 1, mocks 0 en Home, `bandasDePrecio(` 1) | todos PASS |
| Verificacion visual (historial, debounce, render real) | PENDIENTE (UAT humano) |

## Next Phase Readiness

Listo para 02-07 (detalle): `catalogoParams.js` y las facetas ya estan en el front; solo `PublicacionDetallePage` sigue con datos de prueba. 02-08 puede desplegar back y front juntos (el front ya se adapto al cambio de forma de `GET /api/publicaciones`).

## Self-Check: PASSED

- Archivos verificados en disco: catalogoParams.js, catalogoParams.test.js, Paginador.jsx (front).
- Commits verificados en el repo del front: `168f76c`, `6803106`, `dac91a8`.
