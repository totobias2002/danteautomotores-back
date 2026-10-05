---
phase: 03-cuentas-verificadas
plan: 11
subsystem: ui
tags: [react, react-router, auth, gate, node-test, completar-datos, localstorage]

requires:
  - phase: 03-cuentas-verificadas
    provides: GET y PUT /api/usuarios/me y la lista faltantes (03-05); POST /api/usuarios/me/reenviar-confirmacion con 429 (03-10); 403 CUENTA_NO_VERIFICADA con faltantes (03-08); guardarSesion con faltantes y BannerCuentaIncompleta sin links (03-01)
provides:
  - cuenta.js con evaluarAcceso, destinoDeGate, destinoPostLogin, sanitizarDestino, normalizarDni, esDniValido, validarTelefonoBasico y etiquetaFaltante, puras y con 18 tests de node:test
  - script npm test del front (node --test "src/**/*.test.js"), 35 tests en total con los 17 previos
  - AuthContext con guardarSesion y refrescarUsuario expuestos, login que devuelve el usuario y rehidratacion desde /usuarios/me
  - interceptor de 403 CUENTA_NO_VERIFICADA que refresca la cuenta y lleva a /completar-datos
  - BannerCuentaIncompleta con link a Completa tus datos, hook useExigirCuenta y RutaVerificada
  - pagina /completar-datos con reenvio del mail de confirmacion
affects: [03-12, 03-13, fase-04, fase-05]

plan_head_before: 117f4e282b46e9302f08c21a6bf3cefd99780616
plan_head_after: 60b226730d28c023dcbadf6d261995cda590744d

actuals:
  tokens: 8580
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Una unica funcion pura (evaluarAcceso) decide el estado de acceso y destinoDeGate dice a donde llevar; el hook, la ruta y el banner la reusan"
    - "El perfil completo (DNI y telefono) vive en el estado de la pantalla que lo pide; localStorage solo guarda nombre, apellido, mail, rol, mail confirmado, cuenta verificada y faltantes"
    - "El 403 CUENTA_NO_VERIFICADA del back es la autoridad: el interceptor de api.js avisa a AuthProvider, que refresca la cuenta y navega"

key-files:
  created:
    - ../danteautomotores-front/src/utils/cuenta.js
    - ../danteautomotores-front/src/utils/cuenta.test.js
    - ../danteautomotores-front/src/hooks/useExigirCuenta.js
    - ../danteautomotores-front/src/components/RutaVerificada.jsx
    - ../danteautomotores-front/src/pages/CompletarDatosPage.jsx
  modified:
    - ../danteautomotores-front/package.json
    - ../danteautomotores-front/src/context/AuthContext.jsx
    - ../danteautomotores-front/src/services/api.js
    - ../danteautomotores-front/src/components/BannerCuentaIncompleta.jsx
    - ../danteautomotores-front/src/routes/AppRouter.jsx

key-decisions:
  - "useExigirCuenta devuelve true si la accion se ejecuto y false si redirigio o si no pudo refrescar la cuenta desconocida (sin poder confirmarla no se opera)"
  - "destinoPostLogin trata al ADMIN y a una sesion sin lista de faltantes como 'directo al origen'; solo APELLIDO, TELEFONO o DNI faltantes mandan a /completar-datos"
  - "El manejador del 403 usa un candado (useRef) para que varios 403 simultaneos produzcan una sola navegacion y un solo refresco"
  - "En Completa tus datos el DNI solo viaja en el PUT si faltaba; con DNI cargado se muestra de solo lectura (el back ignora un DNI en blanco y rechaza uno distinto)"

patterns-established:
  - "Pantallas de cuenta: leer /usuarios/me en el estado del componente y llamar a refrescarUsuario() tras guardar; navegar a state.from sanitizado con replace"

requirements-completed: [AUTH-03, AUTH-06]

coverage:
  - id: D1
    description: "Existe una unica funcion pura que decide el acceso (anonimo, desconocida, incompleta, verificada), el admin siempre es verificada y un destino externo nunca es valido"
    requirement: "AUTH-06"
    verification:
      - kind: unit
        ref: "../danteautomotores-front/src/utils/cuenta.test.js (npm test: 35 pass, 0 fail)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Sesion rehidratada desde /usuarios/me, 403 CUENTA_NO_VERIFICADA manejado por el interceptor, y DNI y telefono fuera de localStorage"
    requirement: "AUTH-03"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build (compila); grep: refrescarUsuario guarda solo los 7 campos no sensibles"
        status: pass
    human_judgment: true
    rationale: "El comportamiento en el navegador (rehidratar una sesion vieja, navegar tras un 403 real) no tiene tests de front; queda en el UAT manual"
  - id: D3
    description: "Aviso de cuenta incompleta con acceso a Completa tus datos, hook useExigirCuenta y RutaVerificada que conservan la vuelta a la pagina de origen"
    requirement: "AUTH-06"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Aun no hay botones ni rutas que los usen (03-12 y 03-13 los enchufan); la verificacion visual y de flujo queda para el UAT"
  - id: D4
    description: "Pantalla /completar-datos que precarga, pide telefono y DNI, deja el DNI de solo lectura si ya esta cargado, muestra el estado del mail con el reenvio de confirmacion y se puede saltear con Mas tarde"
    requirement: "AUTH-03"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Verificacion en navegador contra un back local (human-check del plan); no se ejecuto en esta corrida"

duration: 3min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 11: Gate del front y Completa tus datos Summary

**Logica pura del acceso con 18 tests de node:test, sesion rehidratada desde /usuarios/me, manejo del 403 CUENTA_NO_VERIFICADA, aviso, hook y ruta del gate, y la pantalla Completa tus datos con reenvio del mail de confirmacion**

## Performance

- **Duration:** 3 min
- **Started:** 2026-10-05T19:47:05Z
- **Completed:** 2026-10-05T19:50:04Z
- **Tasks:** 3
- **Files modified:** 10 (5 creados, 5 modificados; todos en el repo del front)

## Accomplishments
- `cuenta.js` concentra las reglas del gate (estado de acceso, destino de gate y de post-login, sanitizacion del destino, normalizadores de DNI y telefono) sin dependencias de React; `sanitizarDestino` rechaza `//host`, URL absolutas, `/\host`, `javascript:` y valores que no son texto (T-03-61).
- `AuthContext` expone `guardarSesion` y `refrescarUsuario`, `login` devuelve el usuario para que las pantallas decidan el destino, y una sesion guardada antes de la fase se rehidrata sola desde `/usuarios/me`. `refrescarUsuario` guarda solo nombre, apellido, mail, rol, mail confirmado, cuenta verificada y faltantes: el DNI y el telefono van unicamente en el estado de la pantalla que los pide (T-03-62).
- `api.js` suma `registrarManejadorCuentaNoVerificada`; el 403 `CUENTA_NO_VERIFICADA` refresca la cuenta y lleva a `/completar-datos` con la ruta actual como `from` (T-03-63). El manejo del 401 no se toco.
- `BannerCuentaIncompleta` usa `evaluarAcceso`, se oculta en `/completar-datos` y para el admin, y enlaza a Completa tus datos; `useExigirCuenta` y `RutaVerificada` aplican el gate a botones y rutas.
- `CompletarDatosPage` (`/completar-datos`, protegida): precarga nombre y apellido corregibles, pide telefono y DNI con validacion liviana, DNI de solo lectura si ya esta cargado, estado del mail con el boton Reenviar mail de confirmacion (deshabilitado mientras envia, mensaje del back y de limite 429, aviso de spam), leyenda de privacidad con link a `/privacidad`, y Mas tarde sin guardar.

## Task Commits

Todos en el repo del front (`danteautomotores-front`, rama main):

1. **Task 1: logica pura del acceso y normalizadores (TDD)**
   - RED: `cee2b54` (test) - cuenta.test.js y el script `test`; fallaba por no existir cuenta.js
   - GREEN: `842740c` (feat) - cuenta.js; 35 tests verdes
2. **Task 2: sesion, 403, aviso, hook y ruta del gate** - `0ff7dc2` (feat)
3. **Task 3: pantalla Completa tus datos** - `60b2267` (feat)

**Plan metadata:** commit docs en el repo del back (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified
- `src/utils/cuenta.js` - reglas puras del gate y normalizadores
- `src/utils/cuenta.test.js` - 18 tests de node:test
- `package.json` - script `test` con el glob entre comillas (sin dependencias nuevas)
- `src/context/AuthContext.jsx` - rehidratacion, `refrescarUsuario`, manejador del 403, `login` devuelve el usuario
- `src/services/api.js` - `registrarManejadorCuentaNoVerificada` y su disparo en el interceptor
- `src/components/BannerCuentaIncompleta.jsx` - aviso con link a Completa tus datos
- `src/hooks/useExigirCuenta.js` - `exigir(accion)` para botones protegidos
- `src/components/RutaVerificada.jsx` - ruta que exige cuenta verificada
- `src/pages/CompletarDatosPage.jsx` - pantalla Completa tus datos con reenvio del mail
- `src/routes/AppRouter.jsx` - ruta `/completar-datos` dentro de `ProtectedRoute`

## Decisions Made
- `useExigirCuenta` devuelve `true`/`false` segun se ejecuto o no la accion; si la cuenta es desconocida y no se puede refrescar, no se opera (el servidor sigue siendo la autoridad).
- Un candado (`useRef`) en el manejador del 403 evita navegaciones y refrescos duplicados cuando varias llamadas fallan a la vez.
- El DNI solo se manda en el PUT cuando faltaba; con DNI cargado se muestra de solo lectura, en linea con el contrato de 03-05.

## Deviations from Plan

None - plan executed exactly as written.

Observaciones menores que no son desvios: el test de `etiquetaFaltante` y `destinoPostLogin` suma casos (admin, sesion sin faltantes, from externo) por encima de las lineas del comportamiento, y `useExigirCuenta` devuelve un booleano que el plan no pedia (no cambia el contrato `exigir(accion)`).

## Issues Encountered
- `npm run lint` no corre: el repo del front no tiene `eslint.config.*` (ESLint 10 lo exige); es un problema previo y ajeno al plan, no se toco.
- Git avisa de LF a CRLF en los archivos del front (configuracion de autocrlf de Windows); sin efecto en los commits.

## Known Stubs
Ninguno. `/privacidad` (enlace de la leyenda) todavia no tiene pagina: la agrega otro plan de la fase (03-12/03-13); hasta entonces el link cae en la pagina "no encontrada".

## Pendiente de UAT manual
Verificacion en navegador (human-check de la Tarea 3), no ejecutada por este agente: con un back local (`bash scripts/verify/con-back-local.sh --copia-de danteautomotores dante_uat_fase3 sleep 7200`) y el front en el puerto 5174 (`VITE_API_URL=http://localhost:8080/api npm --prefix ../danteautomotores-front run dev -- --port 5174`; nunca contra produccion), entrar con una cuenta vieja y comprobar:
1. El aviso de datos faltantes enlaza a /completar-datos y desaparece dentro de esa pantalla.
2. La pantalla precarga el nombre y pide apellido, telefono y DNI.
3. Un telefono tipo "011 15 1234-5678" y un DNI con puntos se aceptan y quedan guardados normalizados.
4. Un DNI de otra cuenta muestra el mensaje de DNI ya registrado sin revelar de quien es.
5. "Reenviar mail de confirmacion" responde con un mensaje (el link aparece en el log del back, porque sin BREVO_API_KEY el mail solo se loguea) y el boton queda deshabilitado mientras envia.
6. "Mas tarde" vuelve a la pagina anterior sin guardar.
7. Con la cuenta admin, /completar-datos dice "Tu cuenta esta completa".
8. Una sesion guardada antes de la fase se rehidrata sola (aparece el aviso sin volver a loguearse) y localStorage no contiene DNI ni telefono.

## User Setup Required
None - no external service configuration required.

## Threat Flags
Ninguno: no se agregaron endpoints, caminos de autenticacion ni accesos a archivos nuevos; el plan solo consume contratos del back ya existentes.

## Next Phase Readiness
- Listo para 03-12 y 03-13: `destinoPostLogin`, `useExigirCuenta`, `RutaVerificada` y `refrescarUsuario` estan disponibles para enchufarlos en login, registro, navbar y ficha. `registrar` conserva su firma anterior (la cambia 03-12) y `LoginPage` todavia usa su comprobacion de destino inline (se reemplaza por `sanitizarDestino` en 03-12).
- Pendiente: el UAT manual de arriba y la pagina `/privacidad`.

## Self-Check: PASSED

- Archivos creados y modificados existen en el repo del front (10 de 10).
- Commits `cee2b54`, `842740c`, `0ff7dc2` y `60b2267` existen en `danteautomotores-front`.
- `npm --prefix ../danteautomotores-front test`: 35 tests, 35 pass, 0 fail (17 previos + 18 nuevos).
- `npm --prefix ../danteautomotores-front run build`: compila sin errores.
- `refrescarUsuario` y `guardarSesion` no escriben DNI ni telefono en localStorage (campos guardados: nombre, apellido, email, rol, emailConfirmado, cuentaVerificada, faltantes).

---
*Phase: 03-cuentas-verificadas*
*Completed: 2026-10-05*
