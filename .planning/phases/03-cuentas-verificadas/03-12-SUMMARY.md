---
phase: 03-cuentas-verificadas
plan: 12
subsystem: ui
tags: [react, google-identity-services, auth, registro, recuperar-contrasena, confirmar-mail, react-router]

requires:
  - phase: 03-cuentas-verificadas
    provides: POST /auth/google, /auth/confirmar-email, /auth/olvide-contrasena, /auth/restablecer-contrasena y reenviar-confirmacion con 429 (03-10); registro con identidad completa (03-09); cuenta.js, guardarSesion, refrescarUsuario y CompletarDatosPage (03-11)
provides:
  - BotonGoogle (script oficial de Google Identity Services sin npm, una sola inicializacion, callback vigente) y googleDisponible
  - AuthContext con registrar ampliado ({nombre, apellido, email, password, telefono, dni}) y loginConGoogle
  - LoginPage y RegistroPage con Google, vuelta al origen, 429, olvide mi contrasena y link a /privacidad
  - Paginas /olvide-contrasena, /restablecer-contrasena y /confirmar-email (publicas)
  - Variable de build VITE_GOOGLE_CLIENT_ID documentada en .env.example
affects: [03-13, 03-14, fase-04, fase-05]

# Los commits de este plan estan en el repo del front (danteautomotores-front); los hashes son de ese repo.
plan_head_before: 60b226730d28c023dcbadf6d261995cda590744d
plan_head_after: 0eb0ca37c5b368ee91ff5b4c344b157c71a25d49

actuals:
  tokens: 11250
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "El token de un link (confirmar mail, restablecer contrasena) se lee a una referencia en el primer render y se saca de la URL con navigate(replace); la guardia de envio unico es una referencia, no estado"
    - "El script de Google se carga con una promesa de modulo y initialize corre una sola vez; el callback lee siempre el ultimo onCredential de una variable de modulo"
    - "Tras login, registro o Google, las pantallas navegan a destinoPostLogin(usuario, from) con replace"

key-files:
  created:
    - ../danteautomotores-front/src/components/BotonGoogle.jsx
    - ../danteautomotores-front/src/pages/OlvideContrasenaPage.jsx
    - ../danteautomotores-front/src/pages/RestablecerContrasenaPage.jsx
    - ../danteautomotores-front/src/pages/ConfirmarEmailPage.jsx
  modified:
    - ../danteautomotores-front/src/context/AuthContext.jsx
    - ../danteautomotores-front/.env.example
    - ../danteautomotores-front/src/pages/LoginPage.jsx
    - ../danteautomotores-front/src/pages/RegistroPage.jsx
    - ../danteautomotores-front/src/routes/AppRouter.jsx
  deleted:
    - ../danteautomotores-front/src/components/IconoGoogle.jsx

key-decisions:
  - "Sin VITE_GOOGLE_CLIENT_ID el componente no renderiza nada y las paginas ocultan tambien el separador: el desarrollo local sigue funcionando"
  - "Un fallo de red en el login muestra el mensaje de conexion de mensajeDeError; solo el 401 conserva el texto generico de credenciales"
  - "Restablecer: un 400 del back vuelve el link inutil (se ofrece Pedir un link nuevo) en vez de dejar reintentar con el mismo token"
  - "Confirmar mail: sin cancelacion en el efecto (un flag de desmontaje descartaria la respuesta del primer montaje de StrictMode); la guardia useRef evita el segundo POST"

patterns-established:
  - "Paginas de links de mail: centradas, sin panel de video, con role=alert/status en los mensajes"

requirements-completed: [AUTH-01, AUTH-02, AUTH-04]

coverage:
  - id: D1
    description: "El registro pide nombre, apellido, mail, contrasena, telefono y DNI obligatorios con validacion liviana, muestra los mensajes del back y lleva a Completa tus datos solo si faltan datos (si no, vuelve al origen)"
    requirement: "AUTH-01"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build; npm test (35 pass); lectura de RegistroPage (registrar con los seis campos, destinoPostLogin con from)"
        status: pass
    human_judgment: true
    rationale: "No hay tests de componentes en el front; el flujo en navegador contra un back local es el human-check del plan y queda como UAT manual"
  - id: D2
    description: "El boton oficial de Google reemplaza al decorativo en login y registro, manda el credential al back y oculta boton y separador sin Client ID"
    requirement: "AUTH-02"
    verification:
      - kind: other
        ref: "build; grep: sin IconoGoogle ni avisarGoogle en src; BotonGoogle contiene accounts.google.com/gsi/client; .env.example sin valor"
        status: pass
    human_judgment: true
    rationale: "El boton real exige un Client ID de Google Cloud que carga el usuario en su terminal; no se pudo ejercer con Google en esta corrida"
  - id: D3
    description: "Login con mensaje generico en 401, mensaje de limite en 429, link a Olvide mi contrasena, aviso de contrasena cambiada y propagacion segura del origen en Creá una gratis e Ingresá"
    requirement: "AUTH-01"
    verification:
      - kind: other
        ref: "build; LoginPage usa sanitizarDestino y destinoPostLogin de cuenta.js (18 tests de 03-11)"
        status: pass
    human_judgment: true
    rationale: "Verificacion visual y de flujo pendiente en el UAT manual"
  - id: D4
    description: "Olvide mi contrasena muestra siempre el mismo mensaje; Restablecer saca el token de la URL y lleva al login con aviso; Confirmar mail envia el token una sola vez con StrictMode"
    requirement: "AUTH-04"
    verification:
      - kind: other
        ref: "build; lectura: useRef para token y para la guardia de envio, navigate(replace) del pathname en los dos efectos"
        status: pass
    human_judgment: true
    rationale: "El comportamiento de StrictMode y de la barra de direcciones solo se comprueba en el navegador (human-check del plan, no ejecutado)"

duration: 20min
completed: 2026-10-06
status: complete
---

# Phase 3 Plan 12: Pantallas de acceso del front Summary

**Registro ampliado (apellido, telefono, DNI), boton oficial de Google Identity Services sin npm en login y registro, login con vuelta al origen y limites, y las paginas de olvide mi contrasena, restablecer contrasena y confirmar mail con el token fuera de la URL y de un solo uso.**

## Performance

- **Duration:** 20 min
- **Completed:** 2026-10-06
- **Tasks:** 3
- **Files:** 10 (4 creados, 5 modificados, 1 borrado; todos en el repo del front)

## Accomplishments
- `BotonGoogle` carga `accounts.google.com/gsi/client` una sola vez, llama a `initialize` una sola vez y renderiza el boton oficial con ancho maximo de 400; sin `VITE_GOOGLE_CLIENT_ID` no renderiza nada y exporta `googleDisponible` para ocultar el separador.
- `AuthContext.registrar` recibe un unico objeto con los seis datos y `loginConGoogle(credential)` hace `POST /auth/google`; ninguno guarda DNI ni telefono en el navegador.
- `LoginPage`: destino con `sanitizarDestino` y `destinoPostLogin`, 401 con el texto generico, otros errores (429) con `mensajeDeError`, aviso `contrasenaRestablecida`, link Olvidaste tu contrasena, `Creá una gratis` con `state={{ from }}` y Politica de Privacidad como `Link` a `/privacidad`.
- `RegistroPage`: nombre y apellido separados, telefono y DNI obligatorios con ayuda y validacion liviana junto al campo, contrasena de 8 a 72, mensajes del back (mail repetido, DNI ya registrado, campos), texto de confirmacion de mail y vuelta al origen.
- `OlvideContrasenaPage`, `RestablecerContrasenaPage` y `ConfirmarEmailPage` con sus rutas publicas; el token se guarda en una referencia y se quita de la URL con `replace`.
- Se borro `IconoGoogle.jsx` y el aviso temporal con su TODO.

## Task Commits

Todos en el repo del front (`danteautomotores-front`, rama main):

1. **Tarea 1: boton oficial de Google y sesion** - `57629c2` (feat)
2. **Tarea 2: LoginPage y RegistroPage con Google, destino, limites y privacidad** - `cbf5dd0` (feat)
3. **Tarea 3: olvide mi contrasena, restablecer contrasena y confirmar mail con rutas** - `0eb0ca3` (feat)

**Plan metadata:** commit docs en el repo del back (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Decisions Made
- Ver `key-decisions` del frontmatter. La mas relevante: en `ConfirmarEmailPage` el efecto no se cancela en el cleanup, porque el flag de desmontaje descartaria la respuesta del primer montaje de StrictMode; la unica proteccion contra el doble envio es la guardia `useRef`.

## Deviations from Plan

None - plan executed exactly as written.

Notas de ejecucion (no son desvios de codigo):
- Los archivos de la Tarea 1 (`BotonGoogle.jsx`, cambios de `AuthContext.jsx` y `.env.example`) ya estaban en el working tree sin commitear al empezar (restos de una corrida anterior); se revisaron contra el plan, se verifico el build y se commitearon sin cambios.
- La Tarea 2 elimino `IconoGoogle.jsx` con `git rm`.
- Un fallo de red en el login muestra el mensaje de conexion de `mensajeDeError` en lugar del texto generico de credenciales (solo el 401 lo conserva).

## Issues Encountered
- `npm run lint` sigue sin correr por falta de `eslint.config.*` (problema previo de 03-11, ajeno al plan).
- Git avisa LF a CRLF en los archivos del front; sin efecto en los commits.

## Verificacion
- `npm --prefix ../danteautomotores-front run build`: compila sin errores tras cada tarea.
- `npm --prefix ../danteautomotores-front test`: 35 tests, 35 pass, 0 fail (sin tests nuevos: el plan no los pide y el front no tiene tests de componentes).
- `test ! -e IconoGoogle.jsx` y sin `avisarGoogle` en `src`: ok. `VITE_GOOGLE_CLIENT_ID=` vacio en `.env.example`; no hay ningun Client ID en el repo.

## Pendiente de UAT manual
Verificacion en navegador (human-check de la Tarea 3), NO ejecutada en esta corrida. Con back local (copia de la base, sin `BREVO_API_KEY`: los links salen en el log) y el front en el puerto 5174 (`VITE_API_URL=http://localhost:8080/api npm --prefix ../danteautomotores-front run dev -- --port 5174`; `APP_FRONTEND_URL=http://localhost:5174` en el back; nunca contra produccion):
1. Registrarse con apellido, telefono y DNI; el log muestra el link a /confirmar-email; abrirlo muestra "Tu mail quedo confirmado" una sola vez y abrirlo de nuevo da el error de link invalido (y que en la red se vea un unico POST bajo StrictMode).
2. Registrar un DNI repetido muestra el mensaje de DNI ya registrado.
3. Olvide mi contrasena con un mail existente y uno inexistente da el mismo texto; el link del log abre /restablecer-contrasena sin dejar el token en la barra; definir la contrasena lleva al login con el aviso; el mismo link no sirve dos veces.
4. Con cuenta incompleta el login lleva a /completar-datos; con cuenta completa vuelve a la pagina de origen.
5. Con `GOOGLE_CLIENT_ID` y `VITE_GOOGLE_CLIENT_ID` definidos (los carga el usuario en su terminal) el boton de Google aparece y entra; sin ellos no aparece ni el separador.

## Known Stubs
Ninguno. `/privacidad` (enlace de la leyenda de login y registro) todavia no tiene pagina: la agrega 03-13; hasta entonces cae en "no encontrada".

## Threat Flags
Ninguno: no se agregaron endpoints ni caminos de autenticacion; el plan consume contratos del back existentes. T-03-65 a T-03-70 quedaron mitigadas en codigo (token fuera de la URL con replace, destinos sanitizados, el credential de Google lo verifica el back, respuesta uniforme en olvide mi contrasena, guardia useRef, nada tipeado en localStorage); el header Referrer-Policy de T-03-65 llega en 03-13.

## Next Phase Readiness
- 03-13 puede enchufar navbar y ficha: el router y el contexto ya exponen `loginConGoogle`, `registrar` ampliado y las rutas de los links de mail. Falta la pagina `/privacidad` y `Referrer-Policy` en `vercel.json` (03-13).
- 03-14 (humo) debe ejercer el boton de Google real con un Client ID y el UAT manual de arriba.

## Self-Check: PASSED

- Archivos creados presentes en el repo del front: BotonGoogle, OlvideContrasenaPage, RestablecerContrasenaPage y ConfirmarEmailPage; IconoGoogle.jsx ausente.
- Commits `57629c2`, `cbf5dd0` y `0eb0ca3` existen en `danteautomotores-front`.
- Build verde y 35 tests pass.

---
*Phase: 03-cuentas-verificadas*
*Completed: 2026-10-06*
