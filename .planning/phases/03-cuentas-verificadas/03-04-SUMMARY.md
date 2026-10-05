---
phase: 03-cuentas-verificadas
plan: 04
subsystem: mail
tags: [brevo, restclient, spring-async, secretos-guard, email]

requires:
  - phase: 03-cuentas-verificadas
    provides: ServicioExternoException y SecretosGuard de fases anteriores; dependencias de la fase en el pom (plan 03-01)
provides:
  - Interfaz propia EmailSender y record MensajeEmail (el proveedor queda detras de la interfaz)
  - BrevoEmailSender por API REST (POST /v3/smtp/email, timeouts 5 s y 10 s) y LogEmailSender para desarrollo y tests
  - MailConfig que elige la implementacion por BREVO_API_KEY y AsyncConfig con el ejecutor mailExecutor
  - SecretosGuard que fuera del modo desarrollo exige BREVO_API_KEY, MAIL_REMITENTE_EMAIL, APP_FRONTEND_URL (https, no localhost) y GOOGLE_CLIENT_ID
  - Claves nuevas de application.yml (frontend-url, mail, google, seguridad) y variables documentadas en el README
  - EmailSenderEnMemoria, helper de test para capturar mails
affects: [03-06, 03-07, 03-08, 03-10, 03-14, fase-04]

plan_head_before: f4ff0fdc18a6f53addfa8bc048f09f839b660939
plan_head_after: bbf3a6a0dd1eaa7f783bdab9504f3ea2accca025

actuals:
  tokens: 9400
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Servicios externos detras de una interfaz propia con dos implementaciones elegidas por configuracion (Brevo o log)"
    - "Clientes HTTP externos con RestClient y JdkClientHttpRequestFactory con timeouts fijados en la fabrica; toda falla se traduce a ServicioExternoException con mensaje fijo y sin contenido sensible en el log"
    - "Envios de mail en un ejecutor acotado y propio (@Async(\"mailExecutor\"))"

key-files:
  created:
    - src/main/java/com/danteautomotores/mail/EmailSender.java
    - src/main/java/com/danteautomotores/mail/MensajeEmail.java
    - src/main/java/com/danteautomotores/mail/BrevoEmailSender.java
    - src/main/java/com/danteautomotores/mail/LogEmailSender.java
    - src/main/java/com/danteautomotores/config/MailConfig.java
    - src/main/java/com/danteautomotores/config/AsyncConfig.java
    - src/test/java/com/danteautomotores/mail/BrevoEmailSenderTest.java
    - src/test/java/com/danteautomotores/config/MailConfigTest.java
    - src/test/java/com/danteautomotores/support/EmailSenderEnMemoria.java
  modified:
    - src/main/java/com/danteautomotores/config/SecretosGuard.java
    - src/main/resources/application.yml
    - README.md
    - src/test/java/com/danteautomotores/config/SecretosGuardTest.java

key-decisions:
  - "La respuesta de Brevo se lee con exchange(): un 2xx nunca se informa como falla aunque el cuerpo no se pueda leer (el mail ya salio); solo se pierde el messageId del log"
  - "La excepcion que se pasa como causa lleva solo el estado HTTP, no el cuerpo de la respuesta de Brevo, y los logs llevan solo estado o tipo de error"
  - "El ejecutor mailExecutor espera hasta 10 s a que terminen los envios en cola al apagar el back"
  - "MailConfig recibe las propiedades por parametros del metodo @Bean (no por campos) para poder probarlo con ApplicationContextRunner"

patterns-established:
  - "Tests de clientes HTTP externos con com.sun.net.httpserver.HttpServer en puerto libre, sin librerias nuevas ni red"
  - "El guard de arranque amplia fallarOAvisar con las variables de cada servicio externo nuevo"

requirements-completed: []  # PROD-03 sigue abierto: este plan es la mitad de codigo; la cuenta de Brevo y las variables en Railway son 03-14 y 03-15

coverage:
  - id: D1
    description: "BrevoEmailSender manda api-key, JSON y el cuerpo del contrato (sender, to, subject, htmlContent, textContent, replyTo solo si esta configurado) con los acentos en UTF-8"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/mail/BrevoEmailSenderTest.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "Un 4xx, un 5xx, un timeout o una conexion rechazada lanzan ServicioExternoException con mensaje fijo; los logs y la excepcion no contienen el texto del mail, el destinatario ni la API key; un 401 se loguea aparte mencionando la API key y las IPs"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/mail/BrevoEmailSenderTest.java#ningunLogNiLaExcepcionLlevanElTextoDelMailNiLaApiKey"
        status: pass
    human_judgment: false
  - id: D3
    description: "Sin BREVO_API_KEY el bean es LogEmailSender (una sola linea de log con destinatario, asunto y link) y con ella es BrevoEmailSender; la clave no se loguea; un envio @Async corre en un hilo mail-"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/MailConfigTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "Fuera del modo desarrollo el back no arranca sin BREVO_API_KEY, MAIL_REMITENTE_EMAIL, APP_FRONTEND_URL (https, no localhost ni 127.0.0.1) o GOOGLE_CLIENT_ID, y el mensaje nombra la variable exacta; en desarrollo solo avisa"
    requirement: "PROD-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/SecretosGuardTest.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "Las variables nuevas estan en application.yml y en el README sin ningun valor de credencial"
    requirement: "PROD-03"
    verification:
      - kind: other
        ref: "grep BREVO_API_KEY en README.md, application.yml y SecretosGuard.java"
        status: pass
    human_judgment: false

duration: 14 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 04: Servicio de mail Summary

**Interfaz EmailSender con Brevo por API REST (timeouts 5 y 10 s, mensajes fijos y logs sin contenido) y log para desarrollo, elegidos por BREVO_API_KEY, ejecutor asincrono propio y un guard que impide arrancar fuera de desarrollo sin Brevo, remitente, URL https del front y Client ID de Google.**

## Performance

- **Duration:** 14 min
- **Tasks:** 3
- **Files modified:** 13 (9 creados, 4 modificados), todos en el back

## Accomplishments

- `BrevoEmailSender` hace `POST /v3/smtp/email` con `RestClient` y `JdkClientHttpRequestFactory` (conexion 5 s, lectura 10 s). Cualquier 4xx, 5xx, timeout o conexion rechazada se traduce a `ServicioExternoException("No pudimos enviar el mail. Intentá de nuevo en unos minutos.")`. El log lleva el estado HTTP o el tipo de error y el `messageId` del exito; un 401 se loguea aparte sugiriendo revisar la API key y "Block unknown IP addresses".
- `LogEmailSender` escribe una sola linea con destinatario, asunto y texto plano completo (los saltos de linea se reemplazan por ` | `), y su Javadoc advierte que jamas debe estar activo en produccion.
- `MailConfig` elige la implementacion por `app.mail.brevo.api-key` y avisa en el log cuando cae en el modo log; `AsyncConfig` agrega `@EnableAsync` y `mailExecutor` (nucleo 2, maximo 4, cola 100, prefijo `mail-`); un test confirma que un `@Async("mailExecutor")` corre en un hilo `mail-`.
- `SecretosGuard` suma cuatro chequeos (T-03-15 y T-03-17): sin `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `APP_FRONTEND_URL` o `GOOGLE_CLIENT_ID`, o con una URL del front que no sea https o apunte a localhost/127.0.0.1, el back no arranca fuera del modo desarrollo; en dev, local, test y sin perfil solo avisa.
- `application.yml` y README documentan las variables nuevas y las tres cifras de seguridad (24 h, 60 min, 250 mails por dia) sin ningun valor de credencial.

## Task Commits

1. **Tarea 1: interfaz EmailSender, Brevo por API REST y envio por log** - `90a2b40`
2. **Tarea 2: seleccion por configuracion, ejecutor asincrono y captura para tests** - `da3e486`
3. **Tarea 3: guard de arranque, claves en application.yml y README** - `bbf3a6a`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- La respuesta de Brevo se procesa con `exchange()` en lugar de `retrieve()`: asi un 2xx nunca se reporta como falla si el cuerpo viene raro (el mail ya salio) y la causa de una falla lleva solo el estado, no el cuerpo de la respuesta del proveedor.
- `mailExecutor` espera hasta 10 s a la cola al apagar el back, para no perder avisos pendientes en un redeploy.
- `MailConfig` recibe las propiedades por parametros del `@Bean`, lo que permite probarlo con `ApplicationContextRunner` sin levantar la aplicacion.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecucion (no son desvios de codigo)

- **Alcance extra de tests (no pedido):** `MailConfigTest` tambien cubre que la API key no se loguea, la linea unica del `LogEmailSender`, el tamaño del ejecutor y el `@Async` real en hilo `mail-`; `BrevoEmailSenderTest` cubre ademas 403, 429, 503, conexion rechazada y respuesta 2xx con cuerpo no JSON.
- **Archivos tocados:** coinciden con `files_modified` del plan; no hubo otros.
- **PROD-03 no se marca completo:** el requisito sigue abierto porque la cuenta de Brevo, el remitente validado y las variables en Railway son de 03-14 y 03-15 (tambien lo listan 03-06, 03-14 y 03-15). Por eso `requirements-completed` queda vacio y no se llamo a `requirements.mark-complete`.
- **Efecto lateral conocido:** al declarar un `Executor` propio (`mailExecutor`), el autoconfigurado `applicationTaskExecutor` de Spring Boot deja de crearse (es `@ConditionalOnMissingBean(Executor.class)`). El proyecto no usa `@Async`, `@Scheduled` ni tareas asincronas de MVC, asi que no cambia nada hoy; si una fase futura necesita otro ejecutor, tiene que declararlo.
- **Rechazo por cola llena:** con la politica por defecto (abortar), un `@Async("mailExecutor")` con la cola llena lanza `TaskRejectedException` en quien llama. 03-06 (`NotificacionesService`) debe capturarla y loguearla para no romper la respuesta.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None. Todos los tests pasaron en la primera corrida de cada tarea.

## Verificacion

- `mvn -B -o -Djava.version=17 test -Dtest=BrevoEmailSenderTest`: 14 tests, 0 fallas.
- `mvn ... test -Dtest=MailConfigTest,BrevoEmailSenderTest`: 23 tests (9 + 14), 0 fallas.
- `mvn ... test -Dtest=SecretosGuardTest,EntornoDeDesarrolloTest,DataSeederTest,MailConfigTest`: 92 tests (45 + 14 + 24 + 9), 0 fallas.
- `grep BREVO_API_KEY` en README.md, application.yml y SecretosGuard.java: presente en los tres ("variables documentadas").
- Suite completa del back (`-Ddante.pg.required=true`): 452 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS (antes 411; este plan suma 41: 14 BrevoEmailSender, 9 MailConfig, 18 SecretosGuard).
- Nunca se llamo a la API real de Brevo ni se escribio ninguna credencial: los tests usan un servidor HTTP local en loopback y claves ficticias.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador. La prueba de envio real con casillas de Gmail y Outlook es del humo de 03-14.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints nuevos ni rutas de acceso nuevas: el unico trafico saliente nuevo es el POST a Brevo, ya cubierto por T-03-15 a T-03-19. T-03-15, T-03-16, T-03-17 y T-03-19 quedaron mitigadas y probadas; T-03-18 queda con la clave `seguridad.mails-por-dia` (250) y los timeouts y el ejecutor acotado listos, y su aplicacion es de 03-06.

## User Setup Required

External services require manual configuration in 03-14 (no en este plan): crear la cuenta de Brevo, validar el remitente, desactivar "Block unknown IP addresses" y cargar `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `APP_FRONTEND_URL` y `GOOGLE_CLIENT_ID` en Railway. Hasta que esten cargadas, un deploy a produccion con este codigo no arranca (es el comportamiento buscado del guard).

## Next Phase Readiness

- 03-06 puede inyectar `EmailSender` y llamarlo desde un metodo `@Async("mailExecutor")`, y usar `EmailSenderEnMemoria` en sus tests; las claves `app.seguridad.*` y `app.frontend-url` ya existen en `application.yml`.
- **Aviso de despliegue:** no desplegar a produccion este commit sin cargar antes las cuatro variables nuevas en Railway: el guard aborta el arranque fuera del modo desarrollo (T-03-15). El orden y el runbook estan en 03-14.

## Self-Check: PASSED

- Archivos creados presentes en disco: EmailSender, MensajeEmail, BrevoEmailSender, LogEmailSender, MailConfig, AsyncConfig, BrevoEmailSenderTest, MailConfigTest y EmailSenderEnMemoria.
- Commits presentes: `90a2b40`, `da3e486` y `bbf3a6a`.
