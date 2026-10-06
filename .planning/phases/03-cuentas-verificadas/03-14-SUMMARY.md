---
phase: 03-cuentas-verificadas
plan: 14
subsystem: testing
tags: [humo, smoke, brevo, google, readme, ley-25326, flyway, postgres]

requires:
  - phase: 03-cuentas-verificadas
    provides: registro con identidad completa (03-09), confirmacion y recuperacion (03-06 a 03-08), gate de cuenta verificada (03-08), cambio de contrasena y limites (03-10), LogEmailSender y SecretosGuard (03-04)
provides:
  - cuentas-humo.js de punta a punta contra un back real (28 chequeos), con lectura del log de mails por LOG_BACK y chequeo de que el log no tiene DNI, telefono ni contrasenas
  - con-back-local.sh que exporta LOG_BACK (cygpath -m) al comando
  - README con V5, tests, flujo de cuentas, prueba en local, variables de Google y Brevo, runbook de dominio propio, deuda de seguridad y datos personales
  - cuentas externas (Google Client ID y Brevo) creadas por el usuario
affects: [03-15]

plan_head_before: 8911f628fc42cf5b2de8a29573b8edf0db46ad39
plan_head_after: 66091ce45d8293971e38ea045420e58e60dcfa5f

actuals:
  tokens: 14000
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "El humo lee los links de los mails de desarrollo del log del back (LOG_BACK) y exige lineas completas (el link va seguido de ' | ') para no tomar un token cortado"
    - "Chequeo de privacidad: al final del humo se busca en el log el DNI (con y sin puntos), el telefono (normalizado y como lo escribio el usuario) y todas las contrasenas usadas"

key-files:
  created: []
  modified:
    - scripts/verify/cuentas-humo.js
    - scripts/verify/con-back-local.sh
    - README.md

key-decisions:
  - "Los chequeos viejos del humo que registraban una cuenta sin apellido, telefono ni DNI se reescribieron al contrato actual del registro (03-09), que ya los exige: conservan la intencion (cuenta nueva sin PII en la respuesta, perfil normalizado, DNI inmutable) pero sobre una cuenta con identidad completa"
  - "La lectura del log se hace en latin1 y solo busca cadenas ASCII, porque Java en Windows puede escribir el log en una codificacion que no es UTF-8"
  - "Google queda en estado Testing hasta tener dominio propio: se documenta como pendiente conocido en el README"

patterns-established:
  - "Un humo de fase corre contra una base vacia creada y borrada por con-back-local.sh, nunca contra produccion (guarda de localhost en el script)"

requirements-completed: [AUTH-02, AUTH-04]

coverage:
  - id: D1
    description: "El humo completo recorre el flujo de la fase contra un back real sobre una base vacia sin una falla"
    requirement: "AUTH-04"
    verification:
      - kind: other
        ref: "bash scripts/verify/con-back-local.sh --vacia dante_humo_final node scripts/verify/cuentas-humo.js -> humo: 28 ok, 0 fallas, 0 skip"
        status: pass
  - id: D2
    description: "Diez logins fallidos dan 429 con Retry-After y la recuperacion del mismo mail sigue respondiendo 200 y mandando el mail"
    requirement: "AUTH-04"
    verification:
      - kind: other
        ref: "cuentas-humo.js: chequeos de limites (OK)"
        status: pass
  - id: D3
    description: "El log del back de la corrida no contiene DNI, telefono ni contrasenas (Ley 25.326)"
    requirement: "PROD-03"
    verification:
      - kind: other
        ref: "cuentas-humo.js: ultimo chequeo (OK)"
        status: pass
  - id: D4
    description: "Suites completas verdes: back con Postgres real y exigido, tests del front y build del front"
    requirement: "AUTH-02"
    verification:
      - kind: other
        ref: "mvn -B -o -Djava.version=17 -Ddante.pg.required=true test (628 tests, 0 fallas, 0 skipped); npm test del front (35 pass); npm run build del front"
        status: pass
  - id: D5
    description: "README con variables, runbook de dominio propio, deuda de seguridad, politica de datos personales y pendientes legales de la agencia"
    requirement: "PROD-03"
    verification:
      - kind: other
        ref: "grep de MAIL_REMITENTE_EMAIL, 'Deuda de seguridad' y 'Brevo code' en README.md; grep de Client ID y API key de Brevo sin resultados"
        status: pass
  - id: D6
    description: "Cuentas externas listas: Client ID de Google y cuenta de Brevo creados y cargados por el usuario sin pasar secretos por el chat"
    requirement: "PROD-03"
    verification:
      - kind: human
        ref: "Tarea 1 (checkpoint:human-action): el usuario informo H1 y H2 hechos"
        status: partial
    human_judgment: true
    rationale: "La API key de Brevo se pego en el chat, por lo que no se cumple del todo el 'sin pasar por el chat': hay que rotarla antes de cargarla en Railway (03-15). La app de Google quedo en Testing, no publicada."

duration: 40min
completed: 2026-10-06
status: complete
---

# Phase 3 Plan 14: humo de la fase, suites y README Summary

**Humo de 28 chequeos contra un back real (Flyway, Spring y Postgres reales) que recorre registro, confirmacion de mail, gate, recuperacion, perfil y limites leyendo los mails del log del back, con chequeo de que el log no tiene datos personales; suites completas verdes y README con el runbook de dominio propio, deuda de seguridad y datos personales.**

## Performance

- **Duration:** 40 min
- **Completed:** 2026-10-06
- **Tasks:** 2 (Tarea 1 humana hecha por el usuario; Tarea 2 automatica)
- **Files modified:** 3

## Accomplishments

- `cuentas-humo.js` recorre el flujo completo y termina con `humo: 28 ok, 0 fallas, 0 skip`: el registro rechaza lo que falta (400 con `campos.telefono` y `campos.dni`) y el telefono invalido, normaliza telefono y DNI, rechaza el mail repetido en mayusculas y el DNI repetido sin revelar de quien; el mail de confirmacion sale por el log y confirma una sola vez; una cuenta incompleta recibe 403 `CUENTA_NO_VERIFICADA` en consultas y solicitudes de venta (401 sin token) y una verificada pasa el gate (404 por la publicacion y 200); la recuperacion responde igual exista o no la cuenta, restablece una sola vez sin devolver sesion, cierra las sesiones anteriores y manda el aviso sin ningun link; el cambio de contrasena del perfil devuelve un token nuevo e invalida el anterior.
- Diez logins fallidos del mismo mail dan 429 con `Retry-After` y aun asi la recuperacion de ese mail responde 200 y manda el mail (D-14).
- El log de la corrida no contiene el DNI (con y sin puntos), el telefono (normalizado y como lo escribio el usuario) ni ninguna contrasena (T-03-78).
- `con-back-local.sh` exporta `LOG_BACK` convertida con `cygpath -m` para que Node nativo de Windows la lea.
- Suites completas: 628 tests del back con Postgres exigido (0 fallas, 0 salteados), 35 tests del front y build del front.
- README: V5 y su vuelta atras, tests (V1 a V5 y `npm test`), seccion "Cuentas, Google y mails" (flujo, prueba en local, variables, Google en Testing), runbook de dominio propio (D-13, D-18), "Deuda de seguridad conocida" (D-17, limites en memoria, IP de mejor esfuerzo, JWT en localStorage) y "Datos personales (Ley 25.326)" con los pendientes legales de la agencia (D-20).

## Tarea 1 (humana): confirmacion del usuario

El usuario informo que completo H1 (cliente de Google creado, con los origenes de localhost y de Vercel) y H2 (cuenta de Brevo, remitente, API key y bloqueo de IPs desactivado). No se registra ningun valor. **La API key de Brevo se pego en el chat: esta comprometida y hay que rotarla antes de cargarla en Railway (ya se le aviso al usuario).** La comprobacion de alcance de red de la tarea 1 no la corrio este agente.

## Task Commits

1. **Tarea 2 (humo y LOG_BACK):** `685b25d` (test) - `cuentas-humo.js` y `con-back-local.sh`
2. **Tarea 2 (README):** `66091ce` (docs) - `README.md`

## Decisions Made

- Los chequeos viejos del humo (registro sin apellido, telefono ni DNI) se reescribieron al contrato actual del registro, que desde 03-09 los exige.
- La app de Google sigue en Testing: sin dominio propio Google rechaza las URLs de inicio y de privacidad (`vercel.app` no sirve). El ingreso con Google solo funciona para los usuarios de prueba (hasta 100) hasta comprar un dominio, agregarlo como autorizado, cargar las URLs y publicar. Esta documentado en el README como pendiente conocido; el registro con email y contrasena no se ve afectado.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Chequeos del humo desactualizados respecto del contrato del registro**
- **Found during:** Tarea 2
- **Issue:** Los chequeos heredados de 03-01 y 03-05 registraban una cuenta sin apellido, telefono ni DNI y esperaban 200 con cuatro faltantes; desde 03-09 el registro responde 400.
- **Fix:** Se reescribieron sobre una cuenta con identidad completa (solo falta confirmar el mail) conservando el espiritu de cada chequeo. El chequeo "DNI de otra cuenta por PUT" paso a ser "DNI repetido en el registro", porque por la web ya no hay cuentas de contrasena sin DNI.
- **Files modified:** scripts/verify/cuentas-humo.js
- **Commit:** 685b25d

**2. [Rule 3 - Blocking] Docker Desktop estaba apagado**
- **Found during:** Tarea 2 (verificacion)
- **Issue:** Sin el contenedor `danteautomotores-db` no corren los tests de Postgres ni el humo.
- **Fix:** Se inicio Docker Desktop y el contenedor volvio a levantar solo.

## Issues Encountered

None en el codigo: el humo dio verde en la primera corrida real.

## Verificacion

- `bash scripts/verify/con-back-local.sh --vacia dante_humo_final node scripts/verify/cuentas-humo.js`: `humo: 28 ok, 0 fallas, 0 skip`.
- `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test`: 628 tests, 0 fallas, 0 salteados.
- `npm --prefix ../danteautomotores-front test`: 35 pass. `npm --prefix ../danteautomotores-front run build`: OK.
- README: las tres cadenas del grep estan. `grep` de Client ID y API key de Brevo en README y scripts: sin resultados.

## Pendiente de UAT manual

El human-check de la tarea 2 (boton de Google en /login y /registro con una cuenta de Google nueva y con una existente, mail real de confirmacion y de recuperacion con Brevo, y el recorrido de 03-13 con el back real) no lo hizo este agente: necesita las credenciales cargadas en la terminal del usuario, que el agente no tiene ni debe pedir. Para Google, recordar que hasta publicar la app solo entran los usuarios de prueba.

## Known Stubs

None.

## Threat Flags

None.

## Next Phase Readiness

03-15 (despliegue a produccion) necesita:
- **Rotar la API key de Brevo** (se pego en el chat) y cargar la nueva solo en Railway.
- Cargar en Railway `GOOGLE_CLIENT_ID`, `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `APP_FRONTEND_URL` (https, sin barra final) y en Vercel `VITE_GOOGLE_CLIENT_ID`, y redeployar el front.
- Decidir sobre Google: seguir en Testing (agregar como usuarios de prueba a quienes deban entrar con Google) o comprar un dominio y publicar la app.
- Medir la entregabilidad de los mails desde `@brevosend.com` con una casilla de Gmail (D-18).
- Correr el UAT manual de arriba antes o despues del deploy.

## Self-Check: PASSED
