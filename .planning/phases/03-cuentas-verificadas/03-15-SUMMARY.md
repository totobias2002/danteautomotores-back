---
phase: 03-cuentas-verificadas
plan: 15
subsystem: infra
tags: [produccion, railway, vercel, flyway, postgres, backup, brevo, google, entregabilidad]

requires:
  - phase: 03-cuentas-verificadas
    provides: migracion V5 (03-01), registro con identidad completa, confirmacion y recuperacion por mail, ingreso con Google, SecretosGuard ampliado (03-04), humo de la fase y README con el runbook (03-14)
provides:
  - base de produccion migrada de V4 a V5 sin perdida de filas, con backup logico previo verificado fuera de los repos
  - ensayo de V5 y del humo de la fase sobre una copia restaurada del backup, con conteos antes y despues
  - back en Railway y front en Vercel desplegados con las variables nuevas (Google, Brevo, remitente, URL del front)
  - medicion real de entregabilidad con casillas de Gmail (Outlook sin medir) y prueba manual de registro, confirmacion de un solo uso, Google, recuperacion y "Lo quiero" sin sesion
affects: [verificacion de la fase 3, dominio propio, fase 4]

plan_head_before: f62132d5bbabd21e2724a22fbd497dbfe8944400
plan_head_after: f62132d5bbabd21e2724a22fbd497dbfe8944400

actuals:
  tokens: 6000
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns:
    - "Orden de salida a produccion de una migracion de un solo sentido: relevamiento de solo lectura, backup logico verificado con pg_restore --list, ensayo con una copia restaurada en un Postgres descartable de la misma version mayor, recien despues variables y deploy"
    - "Todo lo que toca produccion lo hace el usuario; el agente solo verifica despues con GET y OPTIONS anonimos de solo lectura"

key-files:
  created: []
  modified: []

key-decisions:
  - "Sin pestana de Backups en el plan de Railway (solo en Pro), el unico backup es un volcado logico de pg_dump hecho por el usuario desde su maquina con un acceso publico temporal al servicio Postgres, que se quito al terminar"
  - "La app de Google queda en Testing: publicarla exige un dominio propio; hasta entonces solo los usuarios de prueba pueden entrar con Google y el registro con mail y contrasena no se ve afectado"
  - "La entregabilidad con remitente de Gmail (via brevosend.com, y spam en una de tres) es la esperada por D-13 y D-18; mejorarla pasa por un dominio propio, no por codigo"

patterns-established:
  - "Un backup de produccion se verifica listando su contenido en un contenedor descartable sin conectarse a produccion, y nunca se guarda dentro de un repo"

requirements-completed: []

coverage:
  - id: D1
    description: "Antes de aplicar V5 en produccion existe un backup que se lista con pg_restore y un ensayo con una copia termino con V5 aplicada, Hibernate validando, los mismos conteos, cuentas previas sin DNI y el humo sin fallas"
    requirement: "PROD-03"
    verification:
      - kind: other
        ref: "pg_restore --list del volcado: 7 tablas de la aplicacion mas flyway_schema_history; ensayo local sobre copia: V4 a V5, validate OK, humo 28 ok, 0 fallas, 0 skip"
        status: pass
    human_judgment: false
  - id: D2
    description: "El back corre en Railway con V5 aplicada y las variables nuevas, el catalogo sigue sirviendo y el front de Vercel tiene el Client ID de produccion compilado"
    requirement: "PROD-03"
    verification:
      - kind: other
        ref: "Deploy Logs (informados por el usuario): 'Successfully applied 1 migration ... now at version v5', arranque de la aplicacion, 'Mails por Brevo (remitente configurado: si)', sin lineas de SQL; GET anonimos: health 200 UP, /api/usuarios/me sin token 401, catalogo con tamanio 24, preflight CORS con el origen de Vercel, /privacidad 200, header Referrer-Policy"
        status: pass
    human_judgment: false
  - id: D3
    description: "En produccion un usuario real se registra, confirma el mail con un link de un solo uso, entra con Google, recupera su contrasena y 'Lo quiero' sin sesion lleva al login y vuelve a la ficha"
    requirement: "AUTH-04"
    verification:
      - kind: manual_procedural
        ref: "Prueba H6 hecha por el usuario con casillas reales (sin direcciones registradas)"
        status: pass
    human_judgment: true
    rationale: "Requiere casillas de mail reales y una cuenta de Google; el agente no tiene acceso a ellas ni debe pedirlas"
  - id: D4
    description: "Entregabilidad medida con casillas reales de Gmail y de Outlook para decidir si hace falta un dominio propio"
    requirement: "PROD-03"
    verification:
      - kind: manual_procedural
        ref: "Gmail medido; Outlook NO medido (el usuario decidio saltearlo)"
        status: unknown
    human_judgment: true
    rationale: "La mitad de la medicion pedida por la verdad del plan (Outlook) queda pendiente; la decision de comprar dominio se toma con lo medido en Gmail y se puede revisar cuando se mida Outlook"

duration: no registrado
completed: 2026-10-06
status: complete
---

# Phase 3 Plan 15: produccion con backup, ensayo de V5, deploy y prueba con casillas reales Summary

**La base de produccion paso de V4 a V5 despues de un backup logico verificado y de un ensayo completo sobre una copia restaurada; back y front quedaron desplegados con Google y Brevo, y la prueba real con Gmail dio registro, confirmacion de un solo uso, ingreso con Google y recuperacion funcionando, con los mails de Gmail via brevosend.com y uno de tres en spam.**

## Performance

- **Duration:** no registrado (tres tareas, dos de ellas humanas, repartidas en el dia)
- **Completed:** 2026-10-06
- **Tasks:** 3 (Tarea 1 humana, Tarea 2 automatica local, Tarea 3 humana con verificacion de solo lectura del agente)
- **Files modified:** 0 en los repos; solo este SUMMARY y el seguimiento de planificacion

## Accomplishments

- **Relevamiento previo (Tarea 1, usuario, solo lectura):** Postgres de produccion version 18.6; roles ADMIN y COMPRADOR presentes; cero mails que difieran solo en mayusculas (V5 crea un indice unico sobre lower(email) y habria fallado); ultima migracion aplicada: 4.
- **Backup:** el plan de Railway no tiene la pestana Backups (solo Pro), asi que el unico backup es un volcado logico de pg_dump (`dante-prod-20261006-fase3.dump`, 25.646 bytes) hecho por el usuario desde su maquina con un Public Access temporal en el servicio Postgres, ya quitado. Se guarda fuera de los dos repos. El agente lo verifico con `pg_restore --list` en un contenedor descartable: trae las 7 tablas de la aplicacion y `flyway_schema_history`. `git status` de ambos repos: 0 archivos `.dump`.
- **Ensayo local (Tarea 2, agente):** Postgres descartable `postgres:18-alpine` restaurado desde el volcado; V5 se aplico sin errores (v4 a v5) y `ddl-auto: validate` paso; el humo de la fase dio `28 ok, 0 fallas, 0 skip`. Conteos antes y despues: agencias 7, publicaciones 11, fotos 56, consultas 0, favoritos 0 (sin cambios); usuarios 4 a 6 (el humo agrega 2 cuentas de prueba); solicitudes_venta 0 a 1 (la agrega el humo). Las 4 cuentas previas conservan `dni` y `apellido` en NULL; el mail esta confirmado solo en la ADMIN (1 ADMIN con `email_confirmado` true, 3 COMPRADOR en false). Limpieza hecha (base y contenedor borrados); no se guardo ningun dato sensible.
- **Deploy (Tarea 3):** el usuario cargo en Railway las variables nuevas (Client ID de Google, API key de Brevo, remitente, URL del front) antes de desplegar y publico el back (`47aaa00..8ea90f2`). Deploy Logs: "Successfully applied 1 migration ... now at version v5", "Started DanteAutomotoresApplication", "Mails por Brevo (remitente configurado: si)" y sin lineas de SQL. Despues publico el front (`36995b7..30fc8d0`) y el build de Vercel quedo vivo con el Client ID de Google compilado.
- **Verificacion de solo lectura del agente despues del deploy:** health 200 con estado UP; `/api/usuarios/me` sin token 401; catalogo con `tamanio` 24; el preflight de CORS devuelve el origen de Vercel; `/privacidad` 200; el header Referrer-Policy esta presente; los endpoints nuevos (olvide-contrasena, google, confirmar-email) contestan 405 a GET (antes del deploy daban 404).
- **Prueba manual H6 (usuario, casillas reales, sin direcciones registradas):**
  - Registro con Gmail: el mail de confirmacion llego, 2 veces a la bandeja y 1 a spam; el remitente muestra "via brevosend.com" (esperado con remitente de Gmail, D-13 y D-18).
  - El link de confirmacion funciona y el segundo uso del mismo link falla con el mensaje de invalido o vencido.
  - Ingreso con Google funciona.
  - Recuperacion de contrasena: el mail llega y el flujo funciona.
  - "Lo quiero" sin sesion lleva al login y, despues de entrar, vuelve a la ficha del mismo auto.
  - **Outlook NO se midio** (el usuario decidio saltearlo): queda pendiente.

## Task Commits

El plan no genero commits de codigo: cada tarea opero sobre produccion, sobre una copia local descartable o sobre verificaciones de solo lectura. Los commits que se desplegaron (back `47aaa00..8ea90f2`, front `36995b7..30fc8d0`) son de planes anteriores y los pushes los hizo el usuario. El unico commit de este plan es el de documentacion con este SUMMARY y el estado.

## Decisions Made

- Se acepta como backup unico un volcado logico por la falta de Backups en el plan de Railway; se aprovecha el acceso publico temporal solo el tiempo necesario y se quita.
- Google sigue en Testing: Google exige paginas de inicio y de privacidad en un dominio privado autorizado y `vercel.app` se rechaza. Solo los usuarios de prueba pueden entrar con Google hasta tener dominio propio y publicar la app.
- La medicion de Gmail alcanza para documentar el comportamiento esperado, pero no cierra la verdad del plan sobre Gmail y Outlook: la decision de comprar un dominio se toma sabiendo que Outlook falta medir.

## Deviations from Plan

### Auto-fixed Issues

None: el codigo no se toco. Lo que sigue son desvios operativos y de seguridad, documentados tal cual ocurrieron.

### Incidentes y desvios

**1. [Seguridad - repeticion del incidente de la Fase 2] Secretos pegados en el chat**
- **Que paso:** el usuario pego la API key de Brevo en el chat varias veces y, una vez, la URL de conexion de Postgres con la contrasena (el plan lo prohibia y se le recordo en cada checkpoint). Es la misma clase de incidente que en la Fase 2 (rotacion del 2026-10-05).
- **Remediacion:** se borraron las keys de Brevo y se cargo una nueva solo en Railway; se roto la contrasena de la base (ALTER USER, y luego se actualizaron a mano la variable de contrasena de Postgres, la de `PGPASSWORD`, `DATABASE_URL` y la contrasena del datasource del back). El back reconecto: health UP y catalogo 200.
- **Aprendizaje:** conviene que los proximos runbooks pidan resultados numericos o la palabra de confirmacion, nunca un valor copiado de un panel; el riesgo reaparece cada vez que hay que depurar una conexion.

**2. [Operativo] Public Access temporal en el servicio Postgres**
- **Que paso:** para poder hacer el pg_dump desde la maquina del usuario se habilito un acceso publico al servicio Postgres de Railway.
- **Remediacion:** se quito al terminar el volcado.

**3. [Alcance] La app de Google sigue en Testing**
- **Que paso:** publicar la app exige un dominio propio autorizado (Google rechaza `vercel.app` para las URLs de inicio y de privacidad).
- **Efecto:** hasta tener dominio, solo los usuarios de prueba pueden usar Google; el registro con mail y contrasena no cambia.

**4. [Alcance] Outlook no se midio**
- **Que paso:** la tarea pedia medir Gmail y Outlook; el usuario eligio saltear Outlook.
- **Efecto:** la verdad del plan sobre entregabilidad queda parcial; se mide mas adelante.

**5. [Quick task] 261006-ohs**
- **Que paso:** durante la prueba se corrigio en el front que el boton de la pantalla de mail confirmado lleve al home. Commit del front `d95387f`, todavia sin push (no forma parte de lo desplegado).

**6. [Pendiente opcional] Healthcheck Path**
- El valor `/actuator/health` en Railway sigue sin confirmarse (es opcional en el plan; el deploy funciono).

## Issues Encountered

- Ninguno tecnico: V5 se aplico en el ensayo y en produccion sin errores y no hubo que usar la vuelta atras.
- Aviso de Flyway durante el arranque: PostgreSQL 18.6 es mas nuevo que la version de Flyway probada. No fallo nada; queda como pendiente de seguimiento.

## Pendientes y ideas para despues (no se hicieron)

- Comprar un dominio propio: mejora el remitente (se va el "via brevosend.com" y el spam) y permite publicar la app de Google. El paso a paso esta en el README.
- Medir la entregabilidad en Outlook.
- Icono de soporte por WhatsApp (todavia no hay numero).
- Confirmar el Healthcheck Path `/actuator/health` en Railway.
- Seguimiento del aviso de Flyway sobre PostgreSQL 18.
- Pendientes menores de la Fase 2 que siguen vigentes (rol de la key de Cloudinary, apagar el baseline-on-migrate, cuentas COMPRADOR de prueba).

## Known Stubs

None.

## Threat Flags

None. Estado de las amenazas del plan: T-03-82 mitigada (backup verificado, relevamiento y ensayo antes de tocar produccion); T-03-83 se materializo parcialmente (secretos pegados en el chat, rotados; no entraron a los repos ni a este SUMMARY); T-03-84, T-03-85 y T-03-87 mitigadas (variables cargadas antes del deploy, origen del front verificado con CORS y con los links reales, ventana de deploy corta); T-03-86 aceptada y observada (spam y brevosend.com en Gmail).

## User Setup Required

Hecho por el usuario: variables en Railway y Vercel, deploy de ambos repos, backup y prueba con casillas reales. Pendiente del usuario: ver la seccion de pendientes.

## Next Phase Readiness

- La fase 3 esta desplegada en produccion; la verificacion de la fase (y de PROD-03 en particular, con la medicion de Outlook pendiente) es un paso aparte del orquestador.
- El push del quick task `d95387f` del front todavia no se hizo.

## Self-Check: PASSED

- SUMMARY sin secretos, direcciones ni datos personales (no hay variables con valor asignado).
- Los commits desplegados citados existen en los repos (los pushes los hizo el usuario).
