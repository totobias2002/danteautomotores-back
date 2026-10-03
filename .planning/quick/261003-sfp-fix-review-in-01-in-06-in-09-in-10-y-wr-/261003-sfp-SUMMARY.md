---
phase: quick-261003-sfp
plan: 01
subsystem: config, catalogo-front, scripts-demo
tags: [review-fix, seguridad-arranque, leerFiltros, bandas-precio, creditos, sembrar-demo]
requires:
  - phase: 02
    provides: 02-REVIEW.md, 02-REVIEW-FIX.md, 02-REVIEW-DISPOSITION.md
provides:
  - EntornoDeDesarrollo.esDesarrollo(Environment) como unico criterio de modo desarrollo del back
  - leerFiltros del front con lista blanca de enums y escalares numericos validados
  - bandasDePrecio con floor/ceil en las puntas
  - Aviso de cambios en las imagenes en /creditos
  - sembrar-demo.js que no pisa agencias, registra lo creado y deshace con DESHACER=1
affects: [DataSeeder, SecretosGuard, CatalogoParams, CreditosPage, scripts/demo]
tech-stack:
  added: []
  patterns:
    - "Helper estatico unico para el criterio de entorno compartido por dos componentes"
    - "Script CommonJS con main bajo require.main === module y funciones puras exportadas para node:test"
key-files:
  created:
    - src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java
    - src/test/java/com/danteautomotores/config/EntornoDeDesarrolloTest.java
    - scripts/demo/sembrar-demo.test.js
  modified:
    - src/main/java/com/danteautomotores/config/DataSeeder.java
    - src/main/java/com/danteautomotores/config/SecretosGuard.java
    - src/test/java/com/danteautomotores/config/DataSeederTest.java
    - README.md
    - scripts/demo/sembrar-demo.js
    - scripts/demo/README.md
    - .gitignore
    - ../danteautomotores-front/src/utils/catalogoParams.js
    - ../danteautomotores-front/src/utils/catalogoParams.test.js
    - ../danteautomotores-front/src/pages/CreditosPage.jsx
    - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md
key-decisions:
  - "esDesarrollo mira los perfiles por defecto cuando no hay activos: SPRING_PROFILES_DEFAULT=prod cuenta como produccion para el seed y para el guard"
  - "planDeDeshacer devuelve { publicaciones, agencias } con listas borrar / yaNoEstan / noCoinciden"
  - "El registro de la corrida se crea justo antes de la primera creacion (despues de login, LIMPIAR e idempotencia) y se reescribe tras cada POST exitoso"
patterns-established:
  - "Guardas del script de demo, todas antes de cualquier request, en orden fijo (credenciales, DESHACER+LIMPIAR, LIMPIAR remoto, DESHACER remoto, registro)"
requirements-completed: [ADM-01, PROD-02, CAT-02, PROD-04]
status: complete
duration: ~35min
completed: 2026-10-03
actuals:
  tokens: 40000
  tasks: 3
  commits: 4
plan_head_before: fb577f200f51d30faf0bdb969f0d59979ad42f79
plan_head_after: c8097e5616a1b7acf926721a7eb94f086c0abf5f
---

# Phase quick-261003-sfp Plan 01: Fix review IN-01, IN-06, IN-09, IN-10 y WR-02 (front) Summary

**Un unico `EntornoDeDesarrollo.esDesarrollo` decide si faltar el admin o los secretos aborta el arranque, el catalogo del front descarta valores de URL que el back rechazaria y `sembrar-demo.js` ya no pisa agencias reales y puede deshacer su propia corrida.**

## Commits

Back (4, `commits:` medido desde el ledger `plan_head_before`):

| Hash | Mensaje |
|------|---------|
| 0216685 | test(02): IN-01 tests del criterio unico de modo desarrollo |
| 79fe206 | fix(02): IN-01 DataSeeder y SecretosGuard comparten el criterio de modo desarrollo |
| 57a02aa | test(02): IN-10 tests de las funciones puras de sembrar-demo |
| c8097e5 | fix(02): IN-10 sembrar-demo no pisa agencias existentes y registra lo creado para poder deshacerlo |

Front (2, repo `danteautomotores-front`, no cuentan en `commits:` de arriba):

| Hash | Mensaje |
|------|---------|
| 23ae603 | test(02): WR-02 front e IN-06 tests de leerFiltros y bandasDePrecio |
| a1d4a97 | fix(02): IN-06 WR-02 IN-09 bandas con floor/ceil, leerFiltros valida la URL y creditos indican cambios |

La disposicion (`02-REVIEW-DISPOSITION.md`) queda modificada sin commitear, para el commit de docs del orquestador (el plan pedia commitearla, la instruccion de ejecucion lo reemplaza).

## Que se hizo

- **IN-01 (Task 1, tracer):** nuevo `EntornoDeDesarrollo` con `PERFILES_DE_DESARROLLO = Set.of("dev", "local", "test")` como unica definicion. Con perfiles activos, es desarrollo solo si todos lo son; sin activos, solo si los por defecto son `default` o de desarrollo. `SecretosGuard` perdio su conjunto y metodo privados; `DataSeeder` dejo de comparar con el perfil exacto `prod` y ahora aborta con `production`, `railway`, `staging`, `qa`, `prod,dev` y `SPRING_PROFILES_DEFAULT=prod`. README alineado. Verificacion del tracer: suite completa con `-Ddante.pg.required=true`, 325 tests, 0 fallos, 0 salteados.
- **IN-06 (Task 2):** `bandasDePrecio` usa `Math.floor(desdeMin)` y `Math.ceil(hastaMax)` en las puntas (tambien en la banda unica).
- **WR-02 front (Task 2):** `leerFiltros` valida `tipo`, `zona`, `transmision` y `estado` con `Object.keys(...).includes` contra `etiquetas.js` (sin claves heredadas), `anioMin`/`anioMax`/`kmMax` como enteros no negativos hasta 2147483647, `precioMin`/`precioMax` como decimales no negativos y `ofertas` solo si es `true`. El texto libre no se valida.
- **IN-09 (Task 2):** `/creditos` declara que las imagenes fueron recortadas, redimensionadas y comprimidas.
- **IN-10 (Task 3):** agencias existentes solo reciben un PUT con sus propios datos mas la zona si no tenian; registro incremental en `scripts/demo/registros/<host>.json` (ignorado por git, sin credenciales); modo `DESHACER=1` con las cinco guardas previas a cualquier request.
- **Disposicion:** IN-01, IN-06, IN-09 e IN-10 en `fixed` (quick 261003-sfp), nota de WR-02, `open: 3`.

## Verificacion

- Back: `EntornoDeDesarrolloTest` 14, `DataSeederTest` 24, `SecretosGuardTest` 27, todos en verde; suite completa con Postgres 325 tests en verde.
- Front: `node --test src/utils/catalogoParams.test.js` 17 tests en verde (8 nuevos); `npm run build` en verde.
- Script: `node --check` y `node --test scripts/demo/sembrar-demo.test.js` (10 tests) en verde; las cinco guardas abortan con codigo 1 contra `http://127.0.0.1:9/api` sin hacer requests.
- Extra no pedido: se probo el flujo completo (corrida cortada por falta de fotos, registro incremental, segunda corrida rechazada, `DESHACER=1`, PUT solo con datos propios de la agencia) contra un servidor falso en `127.0.0.1:18999` (script temporal en el scratchpad, ya detenido). No se toco ningun backend real, la base `danteautomotores` ni el `.env` del front.

## Deviations from Plan

**1. [Instruccion de ejecucion] La disposicion no se commitea**
- El plan pedia el commit `docs(02): disposicion del review ...`; la instruccion del orquestador prohibe commitear artefactos de docs (SUMMARY, STATE, PLAN, 02-REVIEW-DISPOSITION). El archivo esta editado y verificado con el gate del plan (`Task 3 docs OK`), pendiente del commit de docs.

**2. [Detalle] `updated:` de la disposicion** se fijo en `2026-10-03T23:45:00Z` (hora UTC aproximada de la edicion).

Fuera de eso, el plan se ejecuto como estaba escrito.

## Known Stubs

None.

## Threat Flags

None. Las mitigaciones T-sfp-01 a T-sfp-07 del plan quedaron implementadas; no se agrego superficie nueva.

## Self-Check: PASSED

- Archivos creados presentes: `EntornoDeDesarrollo.java`, `EntornoDeDesarrolloTest.java`, `sembrar-demo.test.js`.
- Commits presentes: 0216685, 79fe206, 57a02aa, c8097e5 (back); 23ae603, a1d4a97 (front).
