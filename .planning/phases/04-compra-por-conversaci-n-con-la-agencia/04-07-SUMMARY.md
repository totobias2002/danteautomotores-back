---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 07
subsystem: api
tags: [spring, mail, notificaciones, async, transacciones, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: RegistroDeMensajes (unico punto de registro), Conversacion, Mensaje, AutorMensaje, NotificacionesService, LimitadorDeIntentos, LogEmailSender, humo-comun.js y mensajes-humo.js (04-01 a 04-06)
provides:
  - PlantillasEmail.mensajeNuevoParaUsuario y mensajeNuevoParaLaAgencia (asunto fijo, datos escapados, sin el texto del mensaje)
  - NotificacionesService.enviarAvisoDeMensajeAlUsuario y enviarAvisoDeMensajeALaAgencia (@Async mailExecutor, ventana de 10 minutos por conversacion y destinatario, antes del tope diario)
  - UsuarioRepository.findByRol
  - Aviso al otro lado despues del commit, enganchado en RegistroDeMensajes.agregar
  - Chequeos de los mails en el humo (mail al admin, mail al comprador, una sola vez por ventana, sin texto de mensajes en mails ni en el log)
affects: [04-08, 04-10, 04-11]

# Todos los commits estan en el back; el front no se toca en este plan.
plan_head_before: cbcb76f59df616546a666a332541073cd9ab2208
plan_head_after: 82421096678c433ca04ac8dafd7f5ba2a1e9990f

actuals:
  tokens: 14000   # chars/4 sobre el diff de los dos commits
  tasks: 2
  commits: 2   # medido: git rev-list --count plan_head_before..plan_head_after

tech-stack:
  added: []
  patterns:
    - "El aviso se arma con valores simples dentro de la transaccion y se encola con ContrasenaCuenta.despuesDelCommit: si se revierte no sale, y ningun objeto perezoso se toca en el hilo asincrono"
    - "La ventana de 10 minutos (clave aviso-mensaje:{conversacion}:{mail en minusculas}) va antes del tope diario de 250: un aviso frenado no gasta cupo de los mails de cuenta"
    - "Los links de conversacion se arman solo con app.frontend-url (baseDelFront compartido con armarLink), nunca con el Host de la request"

key-files:
  created:
    - src/test/java/com/danteautomotores/service/RegistroDeMensajesTest.java
  modified:
    - src/main/java/com/danteautomotores/service/PlantillasEmail.java
    - src/main/java/com/danteautomotores/service/NotificacionesService.java
    - src/main/java/com/danteautomotores/service/RegistroDeMensajes.java
    - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
    - src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java
    - src/test/java/com/danteautomotores/service/ConversacionServiceTest.java
    - src/test/java/com/danteautomotores/service/ConversacionPostgresTest.java
    - src/test/java/com/danteautomotores/service/ConversacionAdminPostgresTest.java
    - scripts/verify/mensajes-humo.js

key-decisions:
  - "El enganche vive solo en RegistroDeMensajes; ConversacionService y ConversacionAdminService no cambian (verificado con git diff)"
  - "RegistroDeMensajes atrapa las fallas en tres niveles: al preparar el aviso (p. ej. findByRol), al correr la accion despues del commit y por cada destinatario, asi un admin que falla al encolar no impide avisar a los demas"
  - "ConversacionAdminServiceTest no necesito cambios: ya mockeaba RegistroDeMensajes entero"

patterns-established:
  - "Mails de aviso: metodos @Async que reciben solo valores simples (String y Long), nunca entidades"

requirements-completed: [MSG-06]

duration: 35min
completed: 2026-10-07
---

# Phase 4 Plan 07: Avisos por mail de mensaje nuevo Summary

**Cada mensaje nuevo avisa por mail al otro lado (todas las cuentas admin si escribe el usuario, el dueno de la conversacion si responde la agencia) despues de confirmado en la base, con asunto fijo, sin el texto del mensaje, como mucho un mail cada 10 minutos por conversacion y destinatario y sin poder romper el envio del mensaje.**

## Performance

- **Duration:** ~35 min
- **Completed:** 2026-10-07
- **Tasks:** 2 de 2
- **Files modified:** 10 (todos en el back)

## Accomplishments

- `PlantillasEmail` y `NotificacionesService` tienen los dos avisos nuevos. Los asuntos son constantes, el cuerpo pasa por el mismo `armar` que escapa con `HtmlUtils`, y el link sale de `app.frontend-url` con el mismo recorte de espacios y barras finales que los mails de cuenta.
- La ventana de 10 minutos usa `LimitadorDeIntentos` con la clave `aviso-mensaje:{id}:{mail}` y se consulta antes del tope diario: un aviso frenado no suma al contador de 250.
- `RegistroDeMensajes.agregar` junta los datos como valores simples, registra el envio con `despuesDelCommit` y atrapa cualquier excepcion al preparar o encolar.
- El humo verifica en el log del back: mail al admin con `/admin/mensajes/{id}`, el nombre del comprador y el auto; mail al comprador con `/mensajes/{id}`; un solo mail por ventana aunque haya varios mensajes y respuestas; cerrar y reabrir no mandan nada; ni los mails ni el log completo contienen el texto de los mensajes.

## Task Commits

1. **Tarea 1: plantillas y envio de los avisos con ventana de 10 minutos** - `b56b529` (feat, back)
2. **Tarea 2: aviso al otro lado despues del commit y chequeos del humo** - `8242109` (feat, back)

## Verificacion

- `mvn -B -o -Djava.version=17 test -Dtest=NotificacionesServiceTest,LimitadorDeIntentosTest`: 35 tests, 0 fallas.
- `mvn ... test -Dtest=RegistroDeMensajesTest,NotificacionesServiceTest,ConversacionServiceTest,ConversacionAdminServiceTest,ConversacionPostgresTest,ConversacionAdminPostgresTest,TransaccionesServiceTest -Ddante.pg.required=true`: 113 tests, 0 fallas, 0 saltados.
- Suite completa del back con `-Ddante.pg.required=true`: 767 tests, 0 fallas, 0 saltados.
- Humo (`con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js`): `humo: 42 ok, 0 fallas, 0 skip`; la base descartable quedo borrada.
- Criterios de aceptacion: `enviarAvisoDeMensajeALaAgencia`, `enviarAvisoDeMensajeAlUsuario`, `aviso-mensaje`, `despuesDelCommit` y `findByRol` presentes; `@Async("mailExecutor")` aparece 5 veces; `ConversacionService` y `ConversacionAdminService` sin cambios en este plan.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug de test] El HTML escapa las tildes**
- **Found during:** Tarea 1
- **Issue:** Mis dos primeros tests buscaban "Ver mi conversación" y "Abrir la conversación" en el HTML, pero `HtmlUtils.htmlEscape` los deja como `conversaci&oacute;n`.
- **Fix:** Los asserts comparan contra la forma escapada.
- **Files modified:** src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java
- **Commit:** b56b529

Por lo demas, el plan se ejecuto como estaba escrito. `ConversacionAdminServiceTest` figuraba en `files_modified` pero no hizo falta tocarlo (ya mockea `RegistroDeMensajes`).

**Total deviations:** 1 (autocorregida, solo de test).

## Issues Encountered

- El humo lee el log en latin1 y el asunto del usuario lleva una tilde ("Tenés"): el chequeo compara con una expresion regular tolerante a la codificacion en lugar del texto literal.

## Known Stubs

Ninguno.

## Threat Flags

Ninguno: no se agregaron endpoints, rutas de auth ni acceso a archivos. Mitigaciones del plan aplicadas: T-04-25 (ventana antes del tope), T-04-26 (sin texto en mail ni log, verificado en el humo), T-04-27 (asunto fijo y HTML escapado, con test de nombre malicioso), T-04-28 (links con `app.frontend-url`), T-04-29 (envio asincrono despues del commit, con atrapado en `enviar` y en el encolado).

## Self-Check: PASSED

- Archivos: RegistroDeMensajesTest.java, PlantillasEmail.java, NotificacionesService.java, RegistroDeMensajes.java, UsuarioRepository.java y mensajes-humo.js existen.
- Commits `b56b529` y `8242109` presentes en el historial del back.
