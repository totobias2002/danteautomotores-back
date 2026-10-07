---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 08
subsystem: api
tags: [spring, react, admin, ficha-de-usuario, privacidad, ley-25326, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: Conversacion, ConversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc, ConversacionResumenResponse, ConversacionAdminService, VerificacionCuenta, AdminConversacionPage, humo-comun.js y mensajes-humo.js (04-01 a 04-07)
provides:
  - GET /api/admin/usuarios/{id} con la ficha del usuario (contacto, estado de la cuenta e historial de conversaciones)
  - UsuarioFichaResponse (mail, telefono y DNI fuera del toString) y UsuarioAdminService.obtenerFicha
  - ConversacionAdminService.resumir, el armado de resumenes compartido entre la bandeja y la ficha
  - AdminUsuarioPage en /admin/usuarios/:id y el link "Ver ficha del usuario" desde el hilo del admin
  - PrivacidadMensajeriaTest y el cierre de privacidad del humo (claves personales en respuestas y log limpio)
affects: [04-10, 04-11]

# Los commits estan repartidos en dos repos: back (8cfef6d, 6e34bd7) y front (c23338b).
plan_head_before: fb19eedec7c3eb583e723b0ae9e68248903b08c7
plan_head_after: 6e34bd703804da8b132709bd40dd7f24824e4825

actuals:
  tokens: 14600   # chars/4 sobre el diff de los tres commits (back + front)
  tasks: 2
  commits: 2   # medido en el back (git rev-list --count plan_head_before..plan_head_after); el front suma 1 commit propio (c23338b), 3 en total

tech-stack:
  added: []
  patterns:
    - "La ficha del usuario es la unica respuesta que lleva DNI y telefono de otra persona; cuenta admin e id inexistente responden el mismo 404 con el mismo mensaje"
    - "Los DTOs de mensajeria dejan fuera del toString el texto de los mensajes y los mails (@ToString.Exclude); el control por reflexion solo mira los nombres de campo porque Lombok no deja la anotacion en el bytecode, asi que el toString se prueba por comportamiento"
    - "El humo guarda las respuestas de /conversaciones y /admin/conversaciones y las revisa al final contra las claves dni, telefono, password, passwordHash y googleSub"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/usuario/UsuarioFichaResponse.java
    - src/main/java/com/danteautomotores/service/UsuarioAdminService.java
    - src/main/java/com/danteautomotores/controller/AdminUsuarioController.java
    - src/test/java/com/danteautomotores/service/UsuarioAdminServiceTest.java
    - src/test/java/com/danteautomotores/controller/AdminUsuarioSeguridadTest.java
    - src/test/java/com/danteautomotores/dto/PrivacidadMensajeriaTest.java
    - ../danteautomotores-front/src/pages/admin/AdminUsuarioPage.jsx
  modified:
    - src/main/java/com/danteautomotores/service/ConversacionAdminService.java
    - src/main/java/com/danteautomotores/dto/conversacion/ConversacionResumenResponse.java
    - src/main/java/com/danteautomotores/dto/conversacion/MensajeResponse.java
    - src/main/java/com/danteautomotores/dto/conversacion/UsuarioDeConversacionResponse.java
    - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
    - scripts/verify/humo-comun.js
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/pages/admin/AdminConversacionPage.jsx
    - ../danteautomotores-front/src/routes/AppRouter.jsx

key-decisions:
  - "La ficha deja tambien el historial de conversaciones fuera de su toString: las filas repiten el mail del usuario"
  - "Cotizaciones: la ficha muestra solo el historial de conversaciones; las cotizaciones se suman con la Fase 5 (D-15, ADM-06)"
  - "registrarCuentaVerificada del humo devuelve ahora tambien el DNI de la cuenta, para poder buscarlo en el log y en la ficha"

patterns-established:
  - "Un DTO con datos personales declara @ToString.Exclude en cada dato de contacto y tiene su test por toString"

requirements-completed: [MSG-09]

duration: retomado tras un corte por limite de uso (el primer ejecutor dejo la tarea 1 sin commitear)
completed: 2026-10-07
---

# Phase 4 Plan 08: Ficha del usuario y privacidad de la mensajeria Summary

**El admin abre desde cualquier conversacion la ficha del usuario (nombre, apellido, mail, telefono, DNI, estado de la cuenta, cliente desde) con el historial de todas sus conversaciones enlazadas a su hilo; es la unica respuesta de la API con DNI y telefono de otra persona, solo para la cuenta admin, y una pasada de privacidad prueba que ni los DTOs de mensajeria ni el log del back los exponen.**

## Performance

- **Completed:** 2026-10-07
- **Tasks:** 2 de 2
- **Files modified:** 16 (13 en el back, 3 en el front)

## Accomplishments

- `GET /api/admin/usuarios/{id}` devuelve `{ id, nombre, apellido, email, telefono, dni, emailConfirmado, cuentaVerificada, faltantes, fechaRegistro (fecha sin hora), conversaciones }`. `/api/admin/**` ya es solo de ADMIN: sin token 401, comprador 403. Una cuenta admin y un id inexistente dan el mismo 404 ("No existe el usuario"), y un id no numerico da 400.
- `ConversacionAdminService.resumir(List<Conversacion>)` es publico y lo usan la bandeja (`listar`) y la ficha, con el ultimo mensaje y los no leidos en consultas agrupadas.
- `AdminUsuarioPage` (`/admin/usuarios/:id`, solo admin): Cargando, 404 como "No encontramos al usuario", error con "Reintentar", tarjeta de contacto con el mail confirmado o sin confirmar, el telefono como link `tel:`, el DNI, "Cliente desde" y el estado de la cuenta (verificada o lo que falta), y debajo el historial con filas enlazadas a `/admin/mensajes/{id}`. El hilo del admin tiene el link "Ver ficha del usuario".
- Pasada de privacidad: `PrivacidadMensajeriaTest` recorre los seis DTOs de mensajeria (nombres de campo), comprueba que solo `UsuarioResponse` y `UsuarioFichaResponse` declaran dni y telefono entre las respuestas del paquete de usuarios, y que los `toString` de la ficha, del perfil propio y de los DTOs de mensajeria no imprimen contacto, textos ni mails. Se agrego `@ToString.Exclude` al texto del mensaje, al ultimo mensaje y al usuario del resumen y al mail del usuario de la conversacion.
- No hay ningun `log.` en `UsuarioAdminService`, y ninguna clase del back registra DTOs o entidades de mensajeria (revisado con grep sobre `src/main`).
- El humo suma: ficha con 401/403, datos de contacto normalizados (+549...) y DNI sin puntos, historial igual al de la cuenta y ordenado, ficha de una segunda cuenta sin las conversaciones de la primera, 404 igual para el admin y un id inexistente, 400 para un id no numerico; y el cierre: ninguna respuesta de mensajeria tiene claves personales y el log del back no tiene DNI (normalizado ni con puntos), telefono (normalizado ni como lo escribio el usuario), contrasenas de las cuentas y del admin ni el texto de los mensajes.

## Task Commits

1. **Tarea 1: ficha del usuario desde la conversacion (back, ruta, pagina y link)** - back `8cfef6d`, front `c23338b` (feat)
2. **Tarea 2: pasada de privacidad de la mensajeria** - back `6e34bd7` (feat)

## Verificacion

- `mvn -B -o -Djava.version=17 test -Dtest=UsuarioAdminServiceTest,AdminUsuarioSeguridadTest,ConversacionAdminServiceTest,AdminConversacionSeguridadTest,TransaccionesServiceTest`: 47 tests, 0 fallas.
- `npm --prefix ../danteautomotores-front run build` y `npm test`: build correcto, 58 tests, 0 fallas.
- `mvn ... test -Dtest=PrivacidadMensajeriaTest,UsuarioAdminServiceTest,AdminUsuarioSeguridadTest`: 17 tests, 0 fallas.
- Suite completa del back: 784 tests, 0 fallas.
- Humo (`con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js`): `humo: 48 ok, 0 fallas, 0 skip`; la base descartable quedo borrada.
- Criterios de aceptacion de la tarea 1: `ToString.Exclude` aparece 4 veces en `UsuarioFichaResponse`; `path="/admin/usuarios/:id"` una vez en `AppRouter.jsx`; `/admin/usuarios/` presente en `AdminConversacionPage.jsx`; `UsuarioAdminService.class` una vez en `TransaccionesServiceTest`; sin `log.` en `UsuarioAdminService`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Privacidad] toString de los DTOs de mensajeria**
- **Found during:** Tarea 2
- **Issue:** `MensajeResponse`, `ConversacionResumenResponse` y `UsuarioDeConversacionResponse` usan `@Data`, asi que su `toString` imprimia el texto del mensaje, el ultimo mensaje y el mail. Hoy nadie los registra en el log, pero una linea de log futura los filtraria (T-04-32).
- **Fix:** `@ToString.Exclude` en `MensajeResponse.texto`, `ConversacionResumenResponse.ultimoMensaje` y `.usuario`, y `UsuarioDeConversacionResponse.email`; cubierto por `PrivacidadMensajeriaTest`.
- **Files modified:** los tres DTOs de `dto/conversacion`
- **Commit:** 6e34bd7

**2. [Rule 3 - Bloqueo] El humo necesitaba el DNI de cada cuenta**
- **Found during:** Tarea 2
- **Issue:** `registrarCuentaVerificada` no devolvia el DNI que generaba, y sin el no se podia comparar con la ficha ni buscar en el log.
- **Fix:** devuelve tambien `dni` (cambio aditivo en `humo-comun.js`; `humo-comun.js` no figuraba en `files_modified`).
- **Files modified:** scripts/verify/humo-comun.js
- **Commit:** 6e34bd7

**3. [Rule 1 - Bug de test] Lombok no deja `@ToString.Exclude` en el bytecode**
- **Found during:** Tarea 2
- **Issue:** el primer borrador de `PrivacidadMensajeriaTest` verificaba la anotacion por reflexion y fallaba aunque el codigo estaba bien.
- **Fix:** el `toString` se prueba por comportamiento (valores reconocibles que no deben aparecer); la reflexion queda solo para los nombres de campo.
- **Commit:** 6e34bd7

Por lo demas, el plan se ejecuto como estaba escrito. El trabajo de la tarea 1 lo habia dejado sin commitear un ejecutor anterior cortado por limite de uso; se reviso contra el plan, se corrieron todos los comandos `<automated>` y se commiteo sin cambios.

**Total deviations:** 3 (2 autocorregidas de privacidad/bloqueo y 1 de test).

## Issues Encountered

Ninguno mas.

## Known Stubs

Ninguno. La ficha no muestra cotizaciones a proposito: llegan con la Fase 5 (D-15, ADM-06).

## Threat Flags

Ninguno fuera del plan. Mitigaciones aplicadas: T-04-30 (`/api/admin/**` solo ADMIN, 401 y 403 probados en test y humo), T-04-31 (mismo 404 y mismo mensaje para cuenta admin e id inexistente, probado en test y humo), T-04-32 (`@ToString.Exclude`, sin logging en `UsuarioAdminService`, `PrivacidadMensajeriaTest` y chequeo del log en el humo). T-04-33 aceptada: el admin ve DNI y telefono por diseno.

## Self-Check: PASSED

- Archivos: los siete archivos nuevos y los modificados existen.
- Commits `8cfef6d` y `6e34bd7` (back) y `c23338b` (front) presentes en sus historiales.
