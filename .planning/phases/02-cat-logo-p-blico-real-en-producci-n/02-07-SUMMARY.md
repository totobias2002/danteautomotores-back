---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 07
subsystem: detalle-publico-y-cierre-de-mocks
tags: [spring-boot, jpa, specification, similares, react, vite, cloudinary, node-script]

requires:
  - phase: 02-02
    provides: healthcheck publico /actuator/health (el humo lo verifica)
  - phase: 02-04
    provides: CatalogoService, CatalogoSpecification, PostgresLocalTestBase, humo del listado
  - phase: 02-06
    provides: Home, Autos y Agencia sin mocks (solo el detalle los usaba)
provides:
  - GET /api/publicaciones/{id}/similares (publico, 4 por defecto, acotado a 1..8, 404 si el auto no existe)
  - CatalogoService.similares y CatalogoSpecification.similaresA (criterio de D-06)
  - ConsultaService.crear rechaza autos VENDIDO con 400 "Este auto ya se vendió"
  - PublicacionDetallePage real (estado, oferta, aviso de vendido, autos parecidos, consulta y favorito contra la API)
  - Front sin src/mocks ni public/images/car-*.jpg
  - scripts/verify/catalogo-humo.js completo (24 chequeos)
affects: [02-08]

plan_head_before: acb18c78090dcb8b5d77df62b39d47f33961a338
plan_head_after: 74d77901cd4d5876eac513cf5a25fe1e811bfe3d

actuals:
  tokens: 8100    # chars/4 sobre las lineas agregadas (18.979 en el back + 13.451 en el front)
  tasks: 3
  commits: 2      # MEASURED: git rev-list --count plan_head_before..plan_head_after en el back; ademas 2 commits en el front (dac91a8..84b39f2), total 4

tech-stack:
  added: []
  patterns:
    - "Similares como Specification (similaresA) con orden por cb.abs(cb.diff(precio, precioBase)) y desempate id desc; el orden se omite en la consulta de conteo"
    - "Defensa en profundidad del estado: el front oculta el formulario de un vendido y el backend igual rechaza la consulta"
    - "Efecto de carga del detalle con bandera 'vigente' para descartar respuestas de un auto al que el usuario ya no esta mirando"

key-files:
  created:
    - src/test/java/com/danteautomotores/service/ConsultaServiceTest.java
  modified:
    - src/main/java/com/danteautomotores/service/CatalogoService.java
    - src/main/java/com/danteautomotores/repository/spec/CatalogoSpecification.java
    - src/main/java/com/danteautomotores/controller/PublicacionController.java
    - src/main/java/com/danteautomotores/service/ConsultaService.java
    - src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java
    - src/test/java/com/danteautomotores/service/CatalogoServiceTest.java
    - src/test/java/com/danteautomotores/controller/PublicacionControllerCatalogoTest.java
    - scripts/verify/catalogo-humo.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/PublicacionDetallePage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/utils/etiquetas.js
  deleted:
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/mocks/catalogoMock.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/mocks/homeMock.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/mocks/agenciaMock.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-audi-q7.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-bmw-5.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-golf.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-panamera.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-porsche-911.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-volvo-xc60.jpg
    - C:/Users/toto/Desktop/work/danteautomotores-front/public/images/car-wagon.jpg

key-decisions:
  - "Los similares comparan la marca sin distinguir mayusculas (lower) y el tipo de carroceria por igualdad exacta del enum; si el auto base no tiene tipo solo cuenta la marca"
  - "El rango de precio 70-130 % se arma con cb.between sobre precioBase*0.70 y precioBase*1.30 (inclusivo en ambos extremos)"
  - "Solo el detalle de un VENDIDO pide y muestra 'Autos parecidos'; un disponible o reservado no hace el pedido (menos trafico a un endpoint publico)"
  - "El favorito del detalle sigue funcionando para un vendido (el plan solo pidio ocultar formulario y botones de compra)"
  - "Las etiquetas COMBUSTIBLE y CONDICION se agregaron a etiquetas.js (fuente unica) en vez de armarlas dentro de la pagina; el formulario del admin conserva sus propias listas"

patterns-established:
  - "CatalogoSpecification.similaresA(base): reutilizable para cualquier otra sugerencia de autos"

requirements-completed: [CAT-01, CAT-04]

coverage:
  - id: D1
    description: "GET /api/publicaciones/{id}/similares devuelve hasta 4 (1..8) autos DISPONIBLES, distintos del auto, de la misma moneda, entre 70 % y 130 % del precio, del mismo tipo o marca (solo marca si el auto no tiene tipo), por cercania de precio; sirve para un vendido de hace 90 dias; 404 si el auto no existe (CAT-04, D-06)"
    requirement: CAT-04
    verification:
      - kind: integration
        ref: "CatalogoPostgresTest#similaresFiltraPorEstadoMonedaPrecioYTipoOMarcaYOrdenaPorCercaniaDePrecio (9 candidatos: solo entran Yaris 19M y Focus 22M, en ese orden), #similaresDesempataPorIdDescendente..., #similaresSinTipoDeCarroceria..., #similaresDeUnVendidoDeHace90Dias..., #similaresRespetaElLimite..., #similaresSinCandidatos... (Postgres real)"
        status: pass
      - kind: unit
        ref: "CatalogoServiceTest: limite null=4, 0=1, 20=8, id inexistente -> ResourceNotFoundException; PublicacionControllerCatalogoTest: sin token 200 a similares(7,null), /7 sigue yendo a obtenerPorId, limite=6, 404 con error"
        status: pass
      - kind: integration
        ref: "catalogo-humo.js contra back local (copia de dante_uat): similares del primer auto 200, <=4, distintos y DISPONIBLES; ?limite=50 <=8; id 999999999 -> 404 con error"
        status: pass
    human_judgment: false
  - id: D2
    description: "Una consulta sobre un auto VENDIDO se rechaza con 400 'Este auto ya se vendio' y no se guarda; sobre RESERVADO y DISPONIBLE se guarda (T-02-24, D-05, D-06, prohibicion de ofrecer consultar un vendido)"
    requirement: CAT-04
    verification:
      - kind: unit
        ref: "ConsultaServiceTest (4 tests): VENDIDO -> ReglaDeNegocioException y nunca save; RESERVADO y DISPONIBLE guardan; id inexistente -> 404"
        status: pass
      - kind: integration
        ref: "POST /api/consultas contra un back local (copia de dante_uat): VENDIDO 22 -> 400 {error: 'Este auto ya se vendió'}, RESERVADO 18 -> 200, DISPONIBLE 19 -> 200"
        status: pass
    human_judgment: false
  - id: D3
    description: "El detalle /publicaciones/:id carga el auto real de la API: fotos con transformaciones de Cloudinary, descripcion del admin (o el texto de respaldo), ficha con año, km, transmision, combustible, color, condicion, tipo y zona/agencia con link, precio con su moneda y, si es oferta, precio anterior tachado y badge Oferta (CAT-01, D-03)"
    requirement: CAT-01
    verification:
      - kind: build
        ref: "npm run build verde; grep: USE_MOCK_DATA 0, armarDescripcion 0, tipoAuto|colorExterior|mecanica 0, TRANSFORMACION_DETALLE 2, simboloMoneda 2"
        status: pass
      - kind: integration
        ref: "humo: GET /publicaciones/{id} del primer auto trae descripcion (clave) y fotos (array) con el id pedido"
        status: pass
    human_judgment: true
    rationale: "No hay runner de navegador: que el detalle se vea bien con datos reales (ficha, tachado, galeria con w_1280) es UAT visual humano"
  - id: D4
    description: "El detalle muestra el estado: RESERVADO con etiqueta, nota y formulario visible; VENDIDO con el aviso 'Este auto ya se vendio', sin formulario, sin financiamiento, sin cotizar ni reservar, con 'Autos parecidos' cuando hay (CAT-04, D-05, D-06)"
    requirement: CAT-04
    verification:
      - kind: build
        ref: "grep: 'Este auto ya se vendió' 2, '/similares' 1; el formulario, las tarjetas de financiamiento/cotizar y el boton de reserva estan dentro de !vendido; build verde"
        status: pass
      - kind: integration
        ref: "humo: el detalle de un VENDIDO por link directo responde 200 con estado VENDIDO y sus similares 200 con un array"
        status: pass
    human_judgment: true
    rationale: "La disposicion visual de los tres estados contra datos reales es UAT humano (ver Pendiente de UAT)"
  - id: D5
    description: "En el front ya no hay ningun mock: sin src/mocks, sin USE_MOCK_DATA, sin las 7 imagenes car-*.jpg; el build pasa (CAT-01, ROADMAP SC1)"
    requirement: CAT-01
    verification:
      - kind: build
        ref: "test ! -e src/mocks; ! grep -rnE 'USE_MOCK_DATA|mocks/' src; ls public/images | grep -c '^car-' = 0; npm run build 'built in 1.64s'"
        status: pass
    human_judgment: false
  - id: D6
    description: "De punta a punta contra un back local el humo recorre destacados, listado, facetas, detalle, similares, detalle de un vendido, 404 y healthcheck sin fallas"
    requirement: CAT-01
    verification:
      - kind: integration
        ref: "bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_final node scripts/verify/catalogo-humo.js -> 'humo: 24 ok, 0 fallas, 0 skip'; con la copia de danteautomotores (sin destacados ni vendidos): 19 ok, 0 fallas, 5 skip"
        status: pass
    human_judgment: false

duration: 28min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 07: Detalle real, parecidos y front sin mocks Summary

**El detalle de un auto pasa a la API real con su estado, la oferta y el link directo a un vendido (aviso "Este auto ya se vendió" + autos parecidos por `GET /api/publicaciones/{id}/similares`), el backend rechaza con 400 las consultas sobre vendidos, el front queda sin ningún mock y el humo recorre toda la API pública (24 chequeos, 0 fallas) contra un back local.**

## Performance

- **Duration:** ~28 min
- **Tasks:** 3 (Tarea 1 con `tdd="true"`)
- **Files:** 1 creado, 9 modificados, 10 eliminados (back + front)

## Accomplishments

- `CatalogoSpecification.similaresA(base)` + `CatalogoService.similares(id, limite)` + endpoint público `GET /api/publicaciones/{id}/similares`: DISPONIBLES, distintos del auto, misma moneda, 70-130 % del precio, mismo tipo o misma marca (solo marca si no hay tipo), orden por `cb.abs(cb.diff(precio, base))` y después `id desc`; límite 4 por defecto, acotado a 1..8; 404 si el auto no existe. Funciona para un auto en cualquier estado (un vendido de hace 90 días también recibe sugerencias).
- `ConsultaService.crear` lanza `ReglaDeNegocioException("Este auto ya se vendió")` (400) para VENDIDO y no guarda; RESERVADO y DISPONIBLE siguen guardando.
- `PublicacionDetallePage` sin mocks ni `armarDescripcion`: carga `/publicaciones/{id}` con `mensajeDeError`, foto principal con `TRANSFORMACION_DETALLE` y miniaturas con `TRANSFORMACION_MINIATURA` (lazy), ficha con año, km, transmisión, combustible, color, condición, tipo ("Sin especificar") y "Zona · Agencia" con link a la agencia, precio con `simboloMoneda`, y en ofertas precio anterior tachado más badge "Oferta". RESERVADO: badge, nota y formulario visible. VENDIDO: aviso destacado, sin formulario / financiamiento / cotizar / reservar, y sección "Autos parecidos" (con `PublicacionCard` dentro de `Link`) que no aparece si la lista viene vacía o falla.
- Se borran `src/mocks/*` (3 archivos) y las 7 imágenes `public/images/car-*.jpg`; nada del front referencia ya `mocks/` ni `USE_MOCK_DATA`.
- `catalogo-humo.js` suma 7 chequeos: detalle con `descripcion` y `fotos`, similares (<=4, distintos, DISPONIBLES), `limite=50` <=8, detalle de un VENDIDO, 404 del detalle y de similares, y `/actuator/health` (UP, sin `components`).

## Task Commits

Repo del back (`danteautomotores-back`):

1. **Tarea 1 (TDD): similares + consultas sobre vendidos** - `04afb54` (feat)
2. **Tarea 3 (parte back): humo completo** - `74d7790` (test)

Repo del front (`danteautomotores-front`):

3. **Tarea 2: detalle real** - `bb34b8a` (feat)
4. **Tarea 3 (parte front): sin mocks ni imágenes de prueba** - `84b39f2` (feat)

**Plan metadata:** commit docs(02-07) en el repo del back a continuación.

## Deviations from Plan

### Plan-compliant adjustments (no son desvíos de alcance)

- **Etiquetas en `etiquetas.js`:** el plan permitía armar COMBUSTIBLE y CONDICION "en el propio archivo o en etiquetas.js"; se eligió `etiquetas.js` (fuente única de etiquetas). No se tocaron las listas del formulario del admin.
- **Tests TDD en un solo commit:** la lógica y sus tests (Postgres, unit, slice, `ConsultaServiceTest`) se escribieron juntos y se commitearon en un único `feat(02-07)`; pasaron al primer intento (61 tests de las 4 clases).
- **Humo corrido contra dos copias:** la copia de `danteautomotores` (comando del plan) da 19 ok / 0 fallas / 5 skip porque la base de desarrollo no tiene destacados ni vendidos; para ejercitar esos chequeos se corrió también contra una copia de `dante_uat` (11 autos, 1 vendido, 1 reservado): 24 ok / 0 fallas / 0 skip. Además, un script descartable confirmó el 400 / 200 / 200 de `POST /api/consultas` sobre VENDIDO / RESERVADO / DISPONIBLE contra un back real.
- **Rama `main`:** los commits del back y del front se hicieron sobre `main` (como los planes 02-01..02-06, `git.branching_strategy: none`); no se pusheó nada.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno sobre el alcance.

## Pendiente de UAT humano (checkpoint visual no ejecutado)

El `<human-check>` de la Tarea 2 y el de la Tarea 3 no se corrieron: el run es autónomo, no hay runner de navegador y no se toca el `.env` del front. Quedan como UAT manual de fin de fase (nunca contra producción):

1. Back local contra una copia: `bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_uat sleep 1800` (o, para ver tipos, zonas y ofertas reales, la variante de la Tarea 3 que migra la base de desarrollo `danteautomotores` con respaldo previo y recarga la demo local: **paso OMITIDO a propósito, pendiente para el usuario**, porque modifica `danteautomotores`).
2. Front de prueba: `VITE_API_URL=http://localhost:8080/api npx vite --port 5174` desde `danteautomotores-front`.
3. Detalles: un disponible con oferta (precio anterior tachado + "Oferta"), un reservado (etiqueta, nota y consulta enviable), un vendido (aviso "Este auto ya se vendió", sin formulario ni botones de compra, "Autos parecidos" o sin la sección si no hay; en `dante_uat` el vendido no tiene parecidos, así que conviene marcar otro como VENDIDO desde el panel); fotos con `w_1280` la principal y `w_160` las miniaturas en la pestaña de red.
4. Recorrido completo: Home (destacados reales, sin vendidos), `/autos` (filtros, orden, páginas, atrás), una agencia y `/creditos`.

## Known Stubs

None. Los botones "Simulá tu financiamiento", "Cotizar" y "Reservar o agendar visita" siguen mostrando "todavía no está conectada" para usuarios logueados: es el supuesto marcado en el plan (su flujo real es de las fases 3 a 5) y no es un dato de prueba.

## Threat Flags

None. T-02-24 (consulta sobre vendido: 400 en el backend, `ConsultaServiceTest` y prueba real), T-02-26 (`/similares` acotado a 1..8, rango de precio y estado, 404 para ids inexistentes) y T-02-27 (descripción renderizada como texto por React, sin `dangerouslySetInnerHTML`) mitigados; T-02-25 aceptado (D-06, el DTO no incluye datos de usuarios).

## Verification Results

| Check | Resultado |
|-------|-----------|
| `mvn ... -Dtest=CatalogoPostgresTest,CatalogoServiceTest,PublicacionControllerCatalogoTest,ConsultaServiceTest` | 61 tests, 0 fallas, 0 errores, 0 skipped |
| Suite completa del back (`-Ddante.pg.required=true`) | BUILD SUCCESS, 286 tests, Failures 0, Errors 0, Skipped 0 |
| `node --test src/utils/catalogoParams.test.js` (front) | 9 pass, 0 fail |
| `npm run build` (front), tras cada tarea | verde, "built in" presente |
| Humo contra copia de `danteautomotores` (comando del plan) | 19 ok, 0 fallas, 5 skip (base de desarrollo sin destacados ni vendidos) |
| Humo contra copia de `dante_uat` | 24 ok, 0 fallas, 0 skip |
| `POST /api/consultas` real: VENDIDO / RESERVADO / DISPONIBLE | 400 "Este auto ya se vendió" / 200 / 200 |
| Criterios por grep (Tarea 1: `@GetMapping("/{id}/similares")` 1, "Este auto ya se vendió" 1 en ConsultaService, `cb.abs(cb.diff(` 1 en CatalogoSpecification; Tarea 2: USE_MOCK_DATA 0, armarDescripcion 0, mensaje de vendido 2, `/similares` 1, TRANSFORMACION_DETALLE 2, simboloMoneda 2, tipoAuto/colorExterior/mecanica 0; Tarea 3: sin `src/mocks`, 0 `car-*`, 0 referencias `mocks/`, humo con similares/999999999/actuator/health) | todos PASS |
| Verificación visual de los estados del detalle | PENDIENTE (UAT humano) |

## Alcance de los requisitos

CAT-01 (Home, Autos, Agencia y Detalle con datos reales, sin mocks) y CAT-04 (estado en card y detalle; vendido con aviso y parecidos; reservado consultable) se marcan completos: el contrato y los estados están verificados contra un back real y el build; la comprobación visual queda como UAT humano de fin de fase, igual que en 02-05 y 02-06. El despliegue (SC1 "en el sitio desplegado en producción") es 02-08.

## Higiene

La base `dante_copia_final` y el back local los creó y borró `con-back-local.sh` (sin bases `dante_copia*` ni procesos en 8080/5174 al terminar). No se tocó la base `danteautomotores` (solo se la copió), ni el `.env` del front, ni producción; no se pusheó nada. Quedan sin referencias en el front `public/images/hero-car.png` e `inventory-cars.png` (no eran de los mocks ni estaban en el alcance del plan; se dejaron).

## Next Phase Readiness

Lo autónomo de la fase 2 está terminado. 02-08 (no autónomo) despliega back y front juntos con checkpoints humanos; recordar que el UAT visual de 02-05, 02-06 y 02-07 sigue pendiente de una pasada humana en local antes o durante el despliegue.

## Self-Check: PASSED

- Archivos verificados en disco: ConsultaServiceTest.java (back), PublicacionDetallePage.jsx y etiquetas.js (front); `src/mocks` y `public/images/car-*.jpg` ausentes.
- Commits verificados: back `04afb54`, `74d7790`; front `bb34b8a`, `84b39f2`.
