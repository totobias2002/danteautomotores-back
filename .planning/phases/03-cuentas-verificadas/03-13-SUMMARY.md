---
phase: 03-cuentas-verificadas
plan: 13
subsystem: ui
tags: [react, react-router, perfil, gate, privacidad, vercel, referrer-policy]

requires:
  - phase: 03-cuentas-verificadas
    provides: GET/PUT /usuarios/me (03-05), POST /consultas con cuenta verificada (03-08), cambio de contrasena y reenvio de confirmacion (03-10), useExigirCuenta, RutaVerificada, cuenta.js y CompletarDatosPage (03-11), rutas de acceso y /olvide-contrasena (03-12)
provides:
  - PerfilPage (/perfil) con estado de la cuenta, datos editables, cambio de contrasena con sesion nueva y alta de contrasena para cuentas de Google
  - MisMensajesPage (/mensajes, Proximamente) y PrivacidadPage (/privacidad, borrador)
  - /vender y /mensajes detras de RutaVerificada
  - Navbar con Mis mensajes y perfil, pie con Politica de privacidad, Cotiza tu usado con gate
  - Ficha con Lo quiero, gate en Lo quiero/Cotizar/Simula tu financiamiento y consulta que solo manda el mensaje
  - Referrer-Policy strict-origin-when-cross-origin en vercel.json
affects: [03-14, fase-04, fase-05]

# Los commits de este plan estan en el repo del front (danteautomotores-front); los hashes son de ese repo.
plan_head_before: 0eb0ca37c5b368ee91ff5b4c344b157c71a25d49
plan_head_after: 30fc8d02318ae262a6fa659990bf8b353f64de6a

actuals:
  tokens: 7600
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Los botones de operar llaman a exigir(accion) de useExigirCuenta; las rutas que exigen cuenta se envuelven en RutaVerificada"
    - "El bloque de consulta decide por evaluarAcceso: anonimo e incompleta muestran el acceso (destinoDeGate), desconocida refresca la cuenta y verificada muestra solo el mensaje"

key-files:
  created:
    - ../danteautomotores-front/src/pages/PerfilPage.jsx
    - ../danteautomotores-front/src/pages/MisMensajesPage.jsx
    - ../danteautomotores-front/src/pages/PrivacidadPage.jsx
  modified:
    - ../danteautomotores-front/src/routes/AppRouter.jsx
    - ../danteautomotores-front/src/components/Navbar.jsx
    - ../danteautomotores-front/src/components/Footer.jsx
    - ../danteautomotores-front/src/components/SeccionFinanciamiento.jsx
    - ../danteautomotores-front/src/pages/PublicacionDetallePage.jsx
    - ../danteautomotores-front/vercel.json

key-decisions:
  - "El perfil lee tieneContrasena y cuentaVerificada de GET /usuarios/me en el estado de la pantalla; AuthContext sigue sin guardar DNI, telefono ni esas banderas extra en localStorage"
  - "Un admin sin DNI igual edita su perfil (el aviso de completar datos solo aplica a cuentas que no son admin)"
  - "Cotiza tu usado abre WhatsApp despues de que exigir deje pasar; si hubo que refrescar la cuenta (estado desconocido) el navegador podria bloquear la ventana emergente, caso raro que se acepta hasta el cotizador de la Fase 5"

patterns-established:
  - "Mensajes de la ficha: un unico estado mensaje que se limpia al cambiar de auto"

requirements-completed: [AUTH-05, AUTH-06]

coverage:
  - id: D1
    description: "El usuario ve y edita su perfil (nombre, apellido, telefono), con DNI y mail de solo lectura, y ve si la cuenta esta verificada o que le falta"
    requirement: "AUTH-05"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build; test (35 pass); lectura de PerfilPage: PUT /usuarios/me sin dni, DNI y mail readOnly"
        status: pass
    human_judgment: true
    rationale: "No hay tests de componentes en el front; el recorrido en navegador es el human-check del plan y queda como UAT manual"
  - id: D2
    description: "Cambio de contrasena con la actual y sesion nueva via guardarSesion; las cuentas de Google reciben un mail para crear una contrasena"
    requirement: "AUTH-05"
    verification:
      - kind: other
        ref: "build; lectura de PerfilPage (POST /usuarios/me/contrasena, guardarSesion, POST /auth/olvide-contrasena)"
        status: pass
    human_judgment: true
    rationale: "Que la otra pestana pida ingresar de nuevo solo se ve en el navegador contra un back local"
  - id: D3
    description: "Un visitante sin sesion que toca Lo quiero, Cotizar, Simula tu financiamiento, Cotiza tu usado, Vender tu auto o Mis mensajes va al login y vuelve; con la cuenta incompleta va a Completa tus datos"
    requirement: "AUTH-06"
    verification:
      - kind: other
        ref: "build; grep: /vender y /mensajes envueltos en RutaVerificada; ficha y SeccionFinanciamiento usan useExigirCuenta; 18 tests de cuenta.js (03-11) cubren destinoDeGate"
        status: pass
    human_judgment: true
    rationale: "La vuelta a la pagina de origen es un flujo de navegador (human-check 1 a 3 del plan, no ejecutado)"
  - id: D4
    description: "Lo quiero reemplaza a Reservar o agendar visita, Mis mensajes aparece en el navbar de quien tiene sesion y /mensajes es una pagina Proximamente"
    requirement: "AUTH-06"
    verification:
      - kind: other
        ref: "grep: Lo quiero presente y Reservar o agendar visita ausente en PublicacionDetallePage; Navbar con links y iconos a /mensajes y /perfil"
        status: pass
    human_judgment: false
    rationale: ""
  - id: D5
    description: "La consulta de la ficha solo manda publicacionId y mensaje y muestra el acceso a quien no puede consultar"
    requirement: "AUTH-06"
    verification:
      - kind: other
        ref: "build; grep: sin nombreComprador/emailComprador en la ficha; api.post('/consultas', { publicacionId, mensaje }) con maxLength 2000"
        status: pass
    human_judgment: true
    rationale: "Que la consulta llegue al panel del admin con los datos de la cuenta es el human-check 4 del plan"
  - id: D6
    description: "/privacidad existe como borrador con la leyenda de la AAIP y vercel.json agrega Referrer-Policy"
    requirement: "AUTH-06"
    verification:
      - kind: other
        ref: "build; grep Borrador en PrivacidadPage; vercel.json valido (JSON.parse) con Referrer-Policy strict-origin-when-cross-origin y rewrites intactos"
        status: pass
    human_judgment: true
    rationale: "El texto legal lo debe revisar un profesional (D-20); la revision y la inscripcion ante la AAIP son pendientes de la agencia"

duration: 25min
completed: 2026-10-06
status: complete
---

# Phase 3 Plan 13: Perfil, gate de botones, consulta y privacidad del front Summary

**Perfil editable con cambio de contrasena (sesion nueva), gate con vuelta al origen en Lo quiero, Cotizar, Simula tu financiamiento, Cotiza tu usado, Vender tu auto y Mis mensajes, consulta de la ficha que solo manda el mensaje, politica de privacidad en borrador y Referrer-Policy en vercel.json.**

## Performance

- **Duration:** 25 min
- **Completed:** 2026-10-06
- **Tasks:** 3
- **Files:** 9 (3 creados, 6 modificados; todos en el repo del front)

## Accomplishments
- `PerfilPage` (`/perfil`, `ProtectedRoute`): estado de la cuenta (verificada o lista de faltantes con link a Completa tus datos y reenvio del mail), datos editables con `PUT /usuarios/me` sin DNI, mail y DNI de solo lectura con la nota de escribir a la agencia, aviso en lugar del formulario si la cuenta (no admin) no tiene DNI, cambio de contrasena que reemplaza la sesion con `guardarSesion(data)` y, para cuentas sin contrasena, el boton que dispara `POST /auth/olvide-contrasena` con su mail.
- `MisMensajesPage` (Proximamente) y `PrivacidadPage` (borrador pendiente de revision legal, con responsable, datos, finalidad, proveedores, plazo, derechos y la leyenda de la AAIP marcada para validar por un profesional; sin domicilio ni mail inventados).
- `AppRouter`: `/perfil`, `/mensajes` (`RutaVerificada`), `/privacidad` (publica) y `/vender` envuelta en `RutaVerificada`.
- `Navbar`: link "Mis mensajes" en el centro e iconos de mensajes y perfil con `aria-label` y `title`, visibles tambien en pantallas chicas; `Footer`: link a la politica de privacidad.
- `SeccionFinanciamiento`: "Cotiza tu usado" es un `button` que abre el link de WhatsApp solo si `exigir` deja pasar.
- `PublicacionDetallePage`: `requiereCuenta` pasa por `exigir` (conserva el aviso temporal de 4 s para cuentas verificadas), el boton principal se llama "Lo quiero", y el bloque de consulta decide por `evaluarAcceso` (acceso, "Revisando tu cuenta...", o solo el textarea con `maxLength` 2000); el pedido es `{ publicacionId, mensaje }` y el mensaje se limpia al cambiar de auto.
- `vercel.json`: bloque `headers` con `Referrer-Policy: strict-origin-when-cross-origin`; sin Cross-Origin-Opener-Policy.

## Task Commits

Todos en el repo del front (`danteautomotores-front`, rama main):

1. **Tarea 1: perfil, Mis mensajes, privacidad y rutas con gate** - `4181fbe` (feat)
2. **Tarea 2: navbar, pie y Cotiza tu usado con gate** - `5846dcf` (feat)
3. **Tarea 3: ficha con Lo quiero, consulta sin datos sueltos y Referrer-Policy** - `30fc8d0` (feat)

**Plan metadata:** commit docs en el repo del back (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Decisions Made
- Ver `key-decisions` del frontmatter.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- `npm run lint` sigue sin correr por falta de `eslint.config.*` y de `eslint` instalado (problema previo, ajeno al plan).
- Git avisa LF a CRLF en los archivos del front; sin efecto en los commits.

## Verificacion
- `npm --prefix ../danteautomotores-front run build`: compila sin errores tras cada tarea.
- `npm --prefix ../danteautomotores-front test`: 35 tests, 35 pass, 0 fail (sin tests nuevos: el plan no los pide y el front no tiene tests de componentes).
- Verificacion automatica de la Tarea 3: "Lo quiero" presente, "Reservar o agendar visita" ausente, "Referrer-Policy" presente en `vercel.json` (imprime "ficha actualizada").

## Pendiente de UAT manual
Recorrido en navegador (human-check de la Tarea 3), NO ejecutado en esta corrida: requiere back local con copia de la base y el front en el puerto 5174 (el `.env` del front apunta a produccion; no se tocó). Pasos del plan: (1) visitante en una ficha toca Lo quiero, Cotizar y Simula tu financiamiento y vuelve a la ficha tras ingresar; (2) Cotiza tu usado en la Home y Vender tu auto en el navbar llevan al login y vuelven; (3) cuenta incompleta lleva a Completa tus datos y vuelve a la ficha; (4) cuenta verificada: Lo quiero muestra el aviso temporal y la consulta llega al admin con los datos de la cuenta; (5) Mis mensajes en el navbar y /mensajes muestra Proximamente; (6) perfil: estado, edicion, DNI y mail bloqueados, cambio de contrasena que cierra la otra sesion; (7) /privacidad muestra el borrador; (8) el admin no ve el aviso de datos faltantes.

## Known Stubs
- `MisMensajesPage` es una pagina "Proximamente" intencional: la Fase 4 la llena con las conversaciones. Lo quiero, Cotizar y Simula tu financiamiento conservan el aviso temporal "Esta funcion todavia no esta conectada" para cuentas verificadas (D-16: Fase 4 conecta Lo quiero, Fase 5 el cotizador).
- `PrivacidadPage` es un borrador: faltan domicilio, canal de contacto y plazo de conservacion definitivos, a publicar con la revision legal (pendiente de la agencia, tambien la inscripcion ante la AAIP).

## Threat Flags
Ninguno: no se agregaron endpoints ni caminos de autenticacion. T-03-71 (la autoridad sigue en el back), T-03-72 (Referrer-Policy), T-03-73 (guardarSesion con el token nuevo), T-03-74 (DNI y telefono solo en el estado de la pantalla, DNI de solo lectura) y T-03-75 (maxLength 2000) quedaron mitigadas en codigo.

## Next Phase Readiness
- 03-14 (humo) debe ejercer el UAT manual de arriba y el boton de Google real con un Client ID.
- La Fase 4 llena `/mensajes` y conecta Lo quiero; la Fase 5 conecta el cotizador y Cotiza tu usado.

## Self-Check: PASSED

- Archivos creados presentes en el repo del front: PerfilPage, MisMensajesPage y PrivacidadPage.
- Commits `4181fbe`, `5846dcf` y `30fc8d0` existen en `danteautomotores-front`.
- Build verde y 35 tests pass.
