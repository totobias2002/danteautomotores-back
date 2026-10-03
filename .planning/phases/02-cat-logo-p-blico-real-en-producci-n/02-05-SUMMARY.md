---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 05
subsystem: front-panel-y-card
tags: [react, vite, tailwind, admin-panel, creditos, etiquetas]

requires:
  - phase: 02-01
    provides: la card ya usa fotoPortada con miniaturas de Cloudinary
  - phase: 02-03
    provides: contrato de la API (tipoCarroceria, precioAnterior, oferta, agenciaZona, zona de agencias)
provides:
  - src/utils/etiquetas.js (TRANSMISION, TIPO_CARROCERIA, ZONA, ESTADO, simboloMoneda, opcionesDe), unica fuente de etiquetas en espanol
  - Form de publicacion con tipo de carroceria y precio anterior (cargan, editan y se conservan al guardar) y aviso no bloqueante de "no es oferta"
  - Panel de agencias con zona (crear, editar y ver en la lista)
  - PublicacionCard con estado, transmision en espanol, "Zona - Agencia", simbolo de moneda, precio anterior tachado y badge Oferta solo si oferta=true, vendido atenuado
  - Pagina publica /creditos con los 50 creditos de las fotos de demo y link "Creditos de imagenes" en el pie
affects: [02-06, 02-07, 02-08]

plan_head_before: 6476435
plan_head_after: 13aaae7dd8dc5a93d15de83e3c997afd6c15741e

actuals:
  tokens: 7600    # chars/4 sobre el diff realizado (30.320 chars en el repo del front)
  tasks: 3
  commits: 4      # MEASURED: git -C danteautomotores-front rev-list --count 6476435..HEAD (3 de tareas + 1 fix); los commits de este plan viven en el repo del front

tech-stack:
  added: []
  patterns:
    - "Etiquetas de enums y simbolo de moneda en un solo modulo (etiquetas.js) que importan card, forms y, en los proximos planes, detalle y filtros"
    - "La oferta en la card depende solo de publicacion.oferta === true (la regla vive en el servidor); el front no compara precios para decidirla"
    - "Datos de la demo transcriptos a un modulo JS generado desde el .md del back (un script lo parsea, no se copian a mano)"

key-files:
  created:
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/utils/etiquetas.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/data/creditosFotos.js
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/CreditosPage.jsx
  modified:
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/components/PublicacionCard.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/components/Footer.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/routes/AppRouter.jsx

key-decisions:
  - "El badge de la esquina de la card sigue siendo uno solo (estado > Oferta > Verificado, como antes); el precio anterior tachado se muestra aunque el auto este RESERVADO"
  - "Un auto DISPONIBLE no muestra badge de estado (ESTADO.DISPONIBLE existe en etiquetas.js para el detalle, filtros y admin, pero la card lo omite como antes)"
  - "Zona vacia en agencias se envia como null (el PUT reemplaza todo), igual que tipoCarroceria y precioAnterior en el auto"

patterns-established:
  - "etiquetas.js como unica fuente de etiquetas"

requirements-completed: []
requirements-advanced: [CAT-01, CAT-04]   # card con estado/oferta y creditos publicados; faltan detalle (estado) y paginas Agencia/Autos sin mocks (02-06/02-07) para completarlos

coverage:
  - id: D1
    description: "El admin carga tipo de carroceria y precio anterior en el form, al editar se cargan y guardar sin tocarlos no los borra; aviso no bloqueante cuando el precio anterior no supera al precio (D-01, D-03)"
    requirement: CAT-04
    verification:
      - kind: build
        ref: "npm run build (vite) verde; grep: tipoCarroceria x4, precioAnterior x6, 'no se muestra como oferta' x1 en AdminPublicacionFormPage.jsx"
        status: pass
      - kind: unit
        ref: "node: etiquetas.js (8 tipos, 5 zonas, simboloMoneda, opcionesDe) -> 'etiquetas OK'"
        status: pass
    human_judgment: true
  - id: D2
    description: "El admin elige la zona de cada agencia al crear y editar, y la ve en la lista (D-02)"
    requirement: CAT-04
    verification:
      - kind: build
        ref: "npm run build verde; grep -c zona AdminDashboardPage.jsx = 10"
        status: pass
    human_judgment: true
  - id: D3
    description: "La card muestra estado, transmision en espanol, zona y agencia, simbolo de moneda; precio anterior tachado y badge Oferta solo con oferta=true (prohibicion CAT-04)"
    requirement: CAT-04
    verification:
      - kind: render
        ref: "render del servidor de PublicacionCard (esbuild + react-dom/server, 6 casos): oferta=true -> tachado+badge; oferta=false con precioAnterior -> ni tachado ni badge; USD+VENDIDO -> 'US$ 100' + opacity-75; RESERVADO; 'Zona Norte - Dante' + 'Automatica'; sin zona -> solo agencia. 6/6 ok"
        status: pass
    human_judgment: true
  - id: D4
    description: "El pie de toda pagina enlaza /creditos, que lista las 50 fotos de la demo con autor, licencia (con link) y origen; links externos con noopener noreferrer (D-10, prohibicion CAT-01)"
    requirement: CAT-01
    verification:
      - kind: unit
        ref: "node: creditosFotos.js -> 'creditos OK: 50' (campos completos, fuente de commons.wikimedia.org, toda licencia con link)"
        status: pass
      - kind: render
        ref: "render del servidor de CreditosPage: 10 grupos por auto, 100 links externos (50 licencia + 50 origen) con target=_blank y rel='noopener noreferrer' en los 100"
        status: pass
      - kind: build
        ref: "grep: '/creditos' en Footer.jsx y path=\"/creditos\" en AppRouter.jsx; Footer se renderiza en App.jsx para todas las rutas"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 05: Panel y card con los datos nuevos + creditos de fotos Summary

**El admin carga tipo de carroceria, precio anterior y zona de agencia desde el panel; la card muestra estado, oferta real (precio anterior tachado solo si la API dice `oferta=true`), zona y moneda; y `/creditos` publica los 50 creditos de las fotos de demo con link en el pie.**

## Performance

- **Duration:** ~20 min
- **Tasks:** 3 (todas `type="auto"`)
- **Files:** 5 modificados, 3 creados (en `danteautomotores-front`)

## Accomplishments

- `etiquetas.js`: `TRANSMISION`, `TIPO_CARROCERIA`, `ZONA`, `ESTADO`, `simboloMoneda`, `opcionesDe`, un unico lugar para las etiquetas en espanol.
- `AdminPublicacionFormPage`: `tipoCarroceria` y `precioAnterior` en los tres lugares obligatorios (estado inicial, carga del auto al editar y payload), select "Tipo de carroceria", input "Precio anterior (opcional)" con el formato de miles del precio y aviso ambar "Con este valor el auto no se muestra como oferta." sin bloquear el guardado.
- `AdminDashboardPage`: select "Zona" ("Sin especificar" + 5 zonas) al crear y al editar agencias, `zona` vacia viaja como `null`, y la lista muestra la zona como chip junto al nombre.
- `PublicacionCard`: sin `ESTADO_BADGE` ni `mecanica` ni `ubicacion` (campos que la API no tiene); transmision con `TRANSMISION`, "Zona - Agencia", `simboloMoneda(moneda)` en vez del `$` fijo, precio anterior `line-through` y badge "Oferta" solo con `oferta === true`, vendido con `opacity-75`.
- `/creditos`: `CreditosPage` agrupa por auto los 50 creditos generados desde `docs/demo/CREDITOS-FOTOS.md` con un script que parsea la tabla (no se transcribio a mano); cada licencia enlaza a su texto oficial; link discreto "Creditos de imagenes" en el pie, visible en todas las paginas.

## Task Commits (repo `danteautomotores-front`)

1. **Task 1: form de publicacion con tipo y precio anterior + etiquetas.js** - `d695266` (feat)
2. **Task 2: zona en el panel de agencias y card con estado/oferta/zona/moneda** - `0c5256b` (feat)
3. **Fix del regex de precio anterior** - `2d8560d` (fix, ver desvios)
4. **Task 3: pagina /creditos, datos de creditos, link en el pie y ruta** - `13aaae7` (feat)

**Plan metadata:** commit docs(02-05) en el repo del back a continuacion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] El input de precio anterior descartaba la letra "D" en vez de lo que no es digito**
- **Found during:** Task 3 (al generar un archivo con un script parecido vi que la barra invertida se perdia al pasar por el shell; revise los regex escritos de la misma forma en la tarea 1)
- **Issue:** el script que inserto el input en el form dejo `/D/g` en lugar de `/\D/g`; el campo habria aceptado letras y el `Number(...)` de `sinOferta`/payload daria `NaN`. El build pasaba igual.
- **Fix:** restituida la barra invertida; las tres apariciones de `replace(/\D/g, '')` del archivo quedan iguales.
- **Files modified:** src/pages/admin/AdminPublicacionFormPage.jsx
- **Commit:** `2d8560d`

### Other notes (not code deviations)

- **Checkpoint visual no ejecutado (pendiente de UAT humano):** el `<human-check>` de la tarea 2 (admin elige zona/tipo/precio anterior, reabre el form y mira la Home con el back local) no se corrio: no hay runner de navegador y el run es autonomo. Se reemplazo por chequeos automaticos (renders del servidor de la card y de `/creditos`, modulos con node, build). **Queda pendiente la verificacion visual humana** con el flujo descrito en el plan (back local con `con-back-local.sh --copia-de ...` y front en 5174 con `VITE_API_URL=http://localhost:8080/api`).
- **Sin ESLint:** el front no tiene `eslint.config.*` (ESLint 10 no corre), asi que no hubo lint; el build de Vite es el chequeo de sintaxis.
- **Alcance de los requisitos:** `CAT-01` y `CAT-04` quedan como *advanced*, no completos: CAT-04 pide el estado tambien en el detalle y CAT-01 pide Agencia/Autos/Detalle sin mocks (02-06/02-07); REQUIREMENTS.md no se modifico.
- **Arquitectura de commits:** los commits de este plan estan en el repo del front; no se pusheo nada ni se toco el `.env` del front ni ninguna base. No se levantaron servidores, asi que no hay nada que detener.
- **Caveat conocido (plan, Edge Coverage CAT-04):** autos en USD muestran "US$" pero el filtro y el orden por precio comparan el numero crudo (RESEARCH Pitfall 5); la demo es toda en ARS.

**Total deviations:** 1 auto-fixed (Rule 1). **Impact:** ninguno sobre el alcance.

## Known Stubs

None. `AgenciaPage` y `HomePage` conservan sus flags `USE_MOCK_DATA` previos a este plan y pertenecen a 02-06/02-07, no a este.

## Threat Flags

None. T-02-18 (oferta solo desde `oferta` del servidor, con test de render), T-02-19 (`rel="noopener noreferrer"` en los 100 links de `/creditos`) y T-02-20 (cero `dangerouslySetInnerHTML` en `src`) mitigados.

## Verification Results

| Check | Resultado |
|-------|-----------|
| `npm run build` (front) tras cada tarea | verde, "built in" presente |
| node: `etiquetas.js` | `etiquetas OK` |
| node: `creditosFotos.js` | `creditos OK: 50` |
| Render del servidor de `PublicacionCard` (6 casos) | 6/6 ok |
| Render del servidor de `CreditosPage` | 100 links externos, 100 con `rel="noopener noreferrer"`, 10 grupos |
| Criterios de aceptacion por grep (tipoCarroceria 4, precioAnterior 6, aviso 1, 6 exports, zona 10, ESTADO_BADGE 0, mecanica 0, ubicacion 0, simboloMoneda 2, line-through 1, agenciaZona 1, /creditos en Footer y router, rel 2) | todos PASS |
| `grep -rn dangerouslySetInnerHTML src` | 0 |
| Verificacion visual con back local | PENDIENTE (UAT humano) |

## Next Phase Readiness

`etiquetas.js` queda listo para el detalle y los filtros (02-06/02-07), y la card ya consume el contrato de 02-03. Para 02-08: la carga de ofertas reales y la verificacion visual de este plan se pueden hacer con la demo sembrada en local.

## Self-Check: PASSED

- Archivos verificados en disco: etiquetas.js, creditosFotos.js, CreditosPage.jsx (front).
- Commits verificados en el repo del front: `d695266`, `0c5256b`, `2d8560d`, `13aaae7`.
