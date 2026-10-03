---
phase: quick-261003-6lx
plan: 01
subsystem: ui
tags: [react, cloudinary, tailwind, admin-panel, lucide-react]

requires:
  - phase: 01
    provides: "fotos [{ id, url, orden }] en GET /api/admin/publicaciones; orden 0 = portada (decision 01-06)"
provides:
  - "Miniatura de la foto de portada (o placeholder con icono Car) en cada fila del listado de autos del panel admin"
  - "utils/cloudinary.js: urlMiniatura y TRANSFORMACION_MINIATURA"
affects: [admin-panel, listado-autos]

actuals:
  tokens: 2500
  tasks: 2
  commits: 2
plan_head_before: 15074f21c6f2bece98ed369cab1c05a5bdd13ecd
plan_head_after: f586e4e80da5adab80513ced9283adf78a415f65

tech-stack:
  added: []
  patterns:
    - "Util puro de URL de Cloudinary que solo reescribe res.cloudinary.com (hostname estricto) y es idempotente"
    - "Imagen con cadena de fallback por onError (transformada -> original -> placeholder), reiniciada con key por URL"

key-files:
  created:
    - ../danteautomotores-front/src/utils/cloudinary.js
  modified:
    - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx

key-decisions:
  - "Miniatura 80x60 mostrada, 160x120 pedida a Cloudinary (densidad 2x) con q_auto,f_auto"
  - "Hook useState antes del return temprano del placeholder en MiniaturaPublicacion para respetar las reglas de hooks"

requirements-completed: [ADM-02, ADM-03]

duration: 8min
completed: 2026-10-03
status: complete
---

# Quick 261003-6lx: Miniatura de portada en el listado admin Summary

**Cada fila del listado de autos del panel admin muestra la foto de portada (orden 0) pedida a Cloudinary ya recortada a 160x120 con q_auto,f_auto, con placeholder de icono de auto si no hay fotos y fallback transformada -> original -> placeholder si una imagen no carga.**

## Performance

- **Duration:** ~8 min
- **Tasks:** 2
- **Files modified:** 2 (1 creado, 1 modificado), todos en el repo front

## Accomplishments
- `urlMiniatura` inserta `c_fill,w_160,h_120,q_auto,f_auto` despues de `/image/upload/` solo cuando el host es exactamente `res.cloudinary.com`; es idempotente y devuelve tal cual cualquier otra entrada (hosts parecidos, video, URLs invalidas, vacio, null, undefined). Verificado con asserts en node.
- `fotoDePortada(fotos)` y `MiniaturaPublicacion` en `AdminDashboardPage.jsx`: `img` lazy, `decoding="async"`, 80x60, `object-cover`, alt "Foto de portada de {marca} {modelo} {anio}"; placeholder con `Car` (`role="img"`, `aria-label="Sin fotos"`) cuando no hay portada.
- Fila responsive: miniatura + datos en un `div min-w-0` y acciones con `shrink-0`; las clases del `li` no cambiaron (en mobile acciones debajo, desde sm en una fila).
- Fallback ante `onError`: miniatura transformada, luego URL original, luego placeholder; `key` por URL de portada reinicia el estado cuando cambia la portada (por ejemplo tras `actualizarEnLista`).
- Intactos: estrella de destacado, select de estado con su aria-label, nombre de agencia, editar, eliminar, seccion de agencias, filtros y ConfirmDialog (diseño multi-agencia sin cambios).

## Task Commits

Repo front (`danteautomotores-front`, rama main):

1. **Task 1: Miniatura de portada de punta a punta (util + fila + placeholder)** - `eb926c2` (feat)
2. **Task 2: Fallback ante error de carga y reinicio por key** - `f586e4e` (feat)

**Plan metadata:** el commit de docs (SUMMARY/STATE) lo hace el orquestador en el repo back.

## Files Created/Modified
- `../danteautomotores-front/src/utils/cloudinary.js` - `urlMiniatura` y `TRANSFORMACION_MINIATURA`
- `../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx` - `fotoDePortada`, `PlaceholderSinFotos`, `MiniaturaPublicacion` y la fila del listado con miniatura

## Decisions Made
- `PlaceholderSinFotos` como componente interno separado, reutilizado por el caso "sin fotos" y por el fallback final de error.
- Candidatas deduplicadas con `new Set([urlMiniatura(url), url])`, asi las URLs que no son de Cloudinary quedan con un unico intento.

## Deviations from Plan

### Gate/estilo

**1. `fotoDePortada` como arrow function en vez de `function`**
- **Issue:** el grep gate de la Task 1 busca `function fotoDePortada`, pero el helper hermano `normalizar` del mismo archivo es `const ... = () =>` y la instruccion del plan dice "como `normalizar`".
- **Fix:** se mantuvo `const fotoDePortada = (fotos) => ...` por coherencia de estilo; ese unico grep del gate no coincide literalmente (el resto de los gates paso). Comportamiento identico a lo especificado.

Por lo demas, el plan se ejecuto tal cual.

**Total deviations:** 1 (estilo, sin impacto funcional).

## Issues Encountered
- `npx eslint` no corre en el front: no hay `eslint.config.*` (preexistente, fuera de alcance). La verificacion fue build de Vite en verde y las comprobaciones grep.
- Se confirmo en el CSS generado que Tailwind v4 emite `.h-15` (60px), usado para la miniatura.
- Tracer gate: el `<verify>` automatizado se re-ejecuto de punta a punta tras la Task 1 (asserts + build + greps) y paso antes de pasar a la Task 2.

## Pendiente de verificacion manual (human-check de la Task 2, no ejecutado)
Requiere backend levantado y navegador; no se ejecuto en este run. Pasos en el PLAN: (1) miniaturas de portada en autos con fotos, (2) Network pide `c_fill,w_160,h_120,q_auto,f_auto`, (3) placeholder en auto sin fotos, (4) 375px sin scroll horizontal, (5) estrella/estado/editar/eliminar como antes, (6) bloquear `*c_fill*` y ver las originales.

## Known Stubs
None.

## Threat Flags
None - sin nuevas superficies; T-6lx-01 (host estricto) y T-6lx-03 (lazy + transformacion) mitigados como planeado.

## User Setup Required
None - no external service configuration required.

## Self-Check: PASSED
- FOUND: ../danteautomotores-front/src/utils/cloudinary.js
- FOUND commits eb926c2 y f586e4e en danteautomotores-front
- `git rev-list --count 15074f2..HEAD` = 2

---
*Quick: 261003-6lx*
*Completed: 2026-10-03*
