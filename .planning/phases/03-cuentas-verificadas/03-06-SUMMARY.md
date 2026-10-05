---
phase: 03-cuentas-verificadas
plan: 06
subsystem: auth
tags: [tokens, sha-256, securerandom, mail, spring-async, postgres]

requires:
  - phase: 03-cuentas-verificadas
    provides: tabla tokens_cuenta en V5 (plan 03-01); LimitadorDeIntentos (03-03); EmailSender, MensajeEmail, mailExecutor y las claves app.frontend-url y app.seguridad.* (03-04)
provides:
  - TokenCuentaService (emitir, consumir, descartarPendientes) con tokens opacos de 256 bits, guardados solo como SHA-256 y de un solo uso atomico
  - TokenCuenta, TipoTokenCuenta y TokenCuentaRepository (consumo por UPDATE condicional)
  - PlantillasEmail con los tres mails de cuenta en español (texto plano y HTML escapado)
  - NotificacionesService asincrono (enviarConfirmacionEmail, enviarRestablecerContrasena, enviarContrasenaCambiada) con tope global diario y log sin datos personales
affects: [03-07, 03-08, 03-09, 03-10, 03-12, 03-14, fase-04]

plan_head_before: 97c5ed30b5a3ce0dbf61d5260d55427b3a8e8447
plan_head_after: b2645ce38f15b47f1806e05fb33c4ecb3e8f1e85

actuals:
  tokens: 10400
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Token opaco con estado en la base: SecureRandom 32 bytes en Base64 URL, solo el SHA-256 hex se persiste; el consumo es un UPDATE condicional que devuelve filas afectadas"
    - "Servicios de mail que nunca lanzan: toda RuntimeException se atrapa y se loguea solo el tipo de mail y la clase de la excepcion"
    - "Rechazo del ejecutor asincrono resuelto en el RejectedExecutionHandler del propio ejecutor, no en el metodo @Async"

key-files:
  created:
    - src/main/java/com/danteautomotores/enums/TipoTokenCuenta.java
    - src/main/java/com/danteautomotores/entity/TokenCuenta.java
    - src/main/java/com/danteautomotores/repository/TokenCuentaRepository.java
    - src/main/java/com/danteautomotores/service/TokenCuentaService.java
    - src/main/java/com/danteautomotores/service/PlantillasEmail.java
    - src/main/java/com/danteautomotores/service/NotificacionesService.java
    - src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java
    - src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java
  modified:
    - src/main/java/com/danteautomotores/config/AsyncConfig.java
    - src/test/java/com/danteautomotores/config/MailConfigTest.java

key-decisions:
  - "consumir rechaza sin tocar la base cualquier token que no tenga la forma exacta de los emitidos (43 caracteres de [A-Za-z0-9_-]); null, vacio, con espacios o gigante dan vacio"
  - "Los DELETE del repositorio son @Modifying @Query con flush y clear automaticos (una sentencia, sin cargar entidades) en lugar de derived deletes"
  - "El aviso de contraseña cambiada no lleva ningun link (ni al login ni al restablecimiento): solo explica que hacer si no fue el usuario"
  - "El log de fallas de envio lleva el tipo de mail y la clase de la excepcion, nunca su mensaje: ServicioExternoException u otras podrian traer destinatario o link"
  - "El rechazo por cola llena se maneja en AsyncConfig (descartar y loguear) porque el proxy de @Async lanza TaskRejectedException al llamador antes de entrar al metodo"

patterns-established:
  - "Tests de concurrencia contra Postgres real: @Transactional(propagation = NOT_SUPPORTED), N hilos con CountDownLatch, cuenta de apoyo propia que se borra al final"
  - "Captura de logs en tests con un ListAppender de Logback sobre la clase bajo prueba"

requirements-completed: [AUTH-04]
# PROD-03 sigue abierto: este plan es la mitad de codigo del mail; la cuenta de Brevo y las variables en Railway son 03-14 y 03-15.

coverage:
  - id: D1
    description: "El token son 256 bits aleatorios (43 caracteres) y en la base solo se guarda el SHA-256 hex; ningun valor de la fila es igual al token"
    requirement: "AUTH-04"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#emitirGuardaElHashYNoElToken"
        status: pass
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#elTokenTiene43CaracteresYDosEmisionesDanTokensDistintos"
        status: pass
    human_judgment: false
  - id: D2
    description: "Un token se consume una sola vez aunque lleguen ocho pedidos simultaneos (exactamente un exito); vencido, usado, de otro tipo o inexistente dan el mismo vacio"
    requirement: "AUTH-04"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#dosHilosQueConsumenElMismoTokenProducenExactamenteUnExito"
        status: pass
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#unTokenInventadoVacioONuloDevuelveVacio"
        status: pass
    human_judgment: false
  - id: D3
    description: "La confirmacion de mail vence a las 24 horas (valida a las 23 h 59, invalida a las 24 h 1) y el restablecimiento de contraseña a la hora"
    requirement: "AUTH-04"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#laConfirmacionDeMailVenceALas24Horas"
        status: pass
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#elRestablecimientoDeContrasenaVenceALaHora"
        status: pass
    human_judgment: false
  - id: D4
    description: "Emitir un token nuevo borra el anterior del mismo tipo sin tocar el del otro tipo; descartarPendientes borra los dos tipos de la cuenta y solo de esa cuenta"
    requirement: "AUTH-04"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#emitirDeNuevoInvalidaElAnteriorDelMismoTipoPeroNoElDelOtro"
        status: pass
      - kind: integration
        ref: "src/test/java/com/danteautomotores/service/TokenCuentaServiceTest.java#descartarPendientesBorraLosDosTipos"
        status: pass
    human_judgment: false
  - id: D5
    description: "Los links de los tres mails se arman solo con app.frontend-url (sin duplicar la barra final) y el aviso de contraseña cambiada no lleva ningun link; el HTML escapa el nombre"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java"
        status: pass
    human_judgment: false
  - id: D6
    description: "Superado el tope diario (por defecto 250) no se envia nada y se loguea un error sin datos personales; una falla de envio no hace lanzar al metodo y el log no contiene el token ni el mail del destinatario"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java#superadoElTopeDiarioNoSeEnviaNadaYSeLogueaUnError"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/NotificacionesServiceTest.java#unEnvioQueFallaNoHaceLanzarAlMetodoYElLogNoLlevaElTokenNiElMail"
        status: pass
    human_judgment: false
  - id: D7
    description: "Con la cola del ejecutor de mail llena el envio se descarta y se loguea, sin lanzar excepcion a quien lo pidio"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/MailConfigTest.java#conLaColaLlenaElEnvioSeDescartaYSeLogueaSinLanzar"
        status: pass
    human_judgment: false

duration: 22 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 06: Tokens de cuenta y notificaciones por mail Summary

**Tokens opacos de 256 bits guardados solo como SHA-256 y consumidos por un UPDATE condicional (un único ganador entre pedidos simultáneos), más los tres mails de cuenta en español enviados por el ejecutor asíncrono con links armados solo con APP_FRONTEND_URL, tope global de 250 por día y log sin datos personales.**

## Performance

- **Duration:** 22 min
- **Tasks:** 2
- **Files modified:** 10 (8 creados, 2 modificados), todos en el back

## Accomplishments

- `TokenCuentaService.emitir` genera 32 bytes de `SecureRandom` (43 caracteres en Base64 URL), guarda solo `SHA-256(token)` en hexadecimal y borra antes el token anterior del mismo usuario y tipo (un solo link vivo). Vigencia de 24 h para confirmar el mail y 60 min para restablecer la contraseña, leídas de `app.seguridad.*`.
- `consumir` hashea, ejecuta `update ... where usadoEn is null and expiraEn > :ahora` y solo si afectó una fila devuelve el id de la cuenta; vencido, usado, de otro tipo o inexistente son indistinguibles. El test con 8 hilos contra Postgres real y transacciones propias da exactamente un éxito.
- `PlantillasEmail` arma confirmar mail, cambiar contraseña y aviso de contraseña cambiada (texto plano + HTML con botón y URL completa debajo; el nombre se escapa con `HtmlUtils`). El aviso no lleva ningún link.
- `NotificacionesService` (`@Async("mailExecutor")`) arma el link con `app.frontend-url` recortada de espacios y barras finales y `UriComponentsBuilder`, consulta el tope `mails:dia` (250 por 24 h) en el `LimitadorDeIntentos`, y atrapa toda `RuntimeException` logueando solo el tipo de mail y la clase de la excepción.

## Task Commits

1. **Tarea 1: tokens de cuenta hasheados y de un solo uso** - `75c04b1`
2. **Tarea 2: plantillas y notificaciones asíncronas con tope diario** - `b2645ce`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- `consumir` descarta sin tocar la base todo token que no tenga la forma exacta de los emitidos (43 caracteres URL-safe): cubre nulo, vacío, con espacios, con comillas y de longitud absurda.
- Los borrados del repositorio son `@Modifying @Query` con flush y clear automáticos: una sola sentencia y sin cargar entidades; el `clearAutomatically` también evita leer estado viejo después del UPDATE de consumo.
- El log de falla de envío no incluye el mensaje de la excepción (podría traer destinatario o link), y los tests comprueban además que el evento de log no lleva `throwableProxy`.
- El aviso de contraseña cambiada no tiene links: un link ahí solo serviría de señuelo para phishing.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Rechazo por cola llena en el ejecutor de mail**
- **Found during:** Tarea 2 (el SUMMARY de 03-04 pedía que `NotificacionesService` capturara `TaskRejectedException`)
- **Issue:** Con `@Async`, cuando la cola de `mailExecutor` está llena es el proxy el que lanza `TaskRejectedException` al llamador (registro o recuperación) antes de entrar al método, así que un `try/catch` dentro de `NotificacionesService` nunca la ve y el contrato "nunca lanzan" no se cumple.
- **Fix:** `AsyncConfig` instala un `RejectedExecutionHandler` que descarta el envío y loguea un error fijo sin datos personales (el usuario puede reintentar; el reenvío es explícito, D-21). Nada cambia en el tamaño del pool (2/4/100).
- **Files modified:** `src/main/java/com/danteautomotores/config/AsyncConfig.java`, `src/test/java/com/danteautomotores/config/MailConfigTest.java` (test: 104 tareas bloquean el pool, la 105 no lanza y deja el log)
- **Commit:** `b2645ce`

### Notas de ejecución (no son desvíos de código)

- **Alcance extra de tests (no pedido):** `TokenCuentaServiceTest` cubre además que un intento con el tipo equivocado no gasta el token, el formato inválido (comillas, espacios, 100.000 caracteres) y la aislación entre cuentas en `descartarPendientes`; `NotificacionesServiceTest` cubre además el link con otra URL de front configurada, fallas de tipo inesperado y una entrada nula.
- **Archivos tocados:** coinciden con `files_modified` del plan salvo `AsyncConfig.java` y `MailConfigTest.java` (desvío de arriba).
- **PROD-03 no se marca completo:** sigue abierto hasta que 03-14 y 03-15 den de alta Brevo y las variables en Railway; solo se marca AUTH-04.
- **Dónde falta cablear:** nadie llama todavía a estos servicios; los endpoints que los usan son 03-09 (registro y reenvío) y 03-10 (olvidé mi contraseña, restablecer, cambio de contraseña). Quien los llame debe emitir el token dentro de su transacción y llamar a `NotificacionesService` después del commit para que el link no llegue antes que el token.

**Total deviations:** 1 auto-fixed (Rule 2). **Impact:** el contrato de "nunca lanzan" vale ahora también con la cola llena.

## Issues Encountered

None. Los tests pasaron en la primera corrida de cada tarea.

## Verificacion

- `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test -Dtest=TokenCuentaServiceTest,MigracionesPostgresTest`: 18 tests (10 + 8), 0 fallas; Hibernate `validate` acepta la entidad contra V5.
- `mvn ... test -Dtest=NotificacionesServiceTest,LimitadorDeIntentosTest,BrevoEmailSenderTest,MailConfigTest`: 47 tests (10 + 13 + 14 + 10), 0 fallas.
- Suite completa del back (`-Ddante.pg.required=true`): 497 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS (antes 476; este plan suma 21: 10 TokenCuentaService, 10 NotificacionesService, 1 MailConfig).
- Nunca se llamó a la API de Brevo ni se escribió ninguna credencial; los tokens de los tests son ficticios.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador. La prueba de envío real con casillas de Gmail y Outlook es del humo de 03-14.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints nuevos ni rutas de acceso nuevas. T-03-26, T-03-27, T-03-28 y T-03-29 quedaron mitigadas y probadas (256 bits y un solo uso, solo hash en base, consumo atómico con 8 hilos, link con `app.frontend-url`); T-03-30 queda mitigada con el tope diario de 250 mails y el ejecutor acotado (los límites por cuenta e IP van en los endpoints de 03-10).

## User Setup Required

None - no external service configuration required en este plan.

## Next Phase Readiness

- 03-09 y 03-10 pueden inyectar `TokenCuentaService` y `NotificacionesService`; `emitir` devuelve el token en claro solo para pasárselo a `NotificacionesService`, y nunca debe loguearse ni devolverse en una respuesta HTTP.
- 03-10 debe llamar a `descartarPendientes` al restablecer o cambiar la contraseña y fijar `passwordCambiadaEn` con `LocalDateTime.now(clock)` (ver el aviso de 03-03).
- 03-12 debe crear las rutas del front `/confirmar-email` y `/restablecer-contrasena` que leen el parámetro `token` y hacen un `POST` (nunca un `GET` con efecto).
- Los tests de integración de 03-09 y 03-10 pueden reemplazar `EmailSender` por `EmailSenderEnMemoria`; como los envíos son `@Async`, esperar el mail con un poll corto o sustituir `NotificacionesService`.

## Self-Check: PASSED

- Archivos creados presentes en disco: TipoTokenCuenta, TokenCuenta, TokenCuentaRepository, TokenCuentaService, PlantillasEmail, NotificacionesService, TokenCuentaServiceTest y NotificacionesServiceTest.
- Commits presentes: `75c04b1` y `b2645ce`.
