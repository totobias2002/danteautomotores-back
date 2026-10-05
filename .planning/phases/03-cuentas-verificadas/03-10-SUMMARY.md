---
phase: 03-cuentas-verificadas
plan: 10
subsystem: auth
tags: [recuperacion-de-contrasena, google, rate-limit, jwt-pca, confirmacion-de-mail, owasp]

requires:
  - phase: 03-cuentas-verificadas
    provides: LimitadorDeIntentos y AuthService.iniciarSesion con el claim pca (03-03); perfil propio GET y PUT /api/usuarios/me (03-05); TokenCuentaService y NotificacionesService (03-06); GoogleAuthService.entrar (03-07); registro con identidad completa (03-09)
provides:
  - RecuperacionCuentaService (confirmarEmail, solicitarRestablecimiento, restablecerContrasena) con respuesta uniforme, token de un solo uso y sin sesión automática
  - POST /api/auth/confirmar-email, /olvide-contrasena, /restablecer-contrasena y /google, todos POST
  - Límites de intentos por mail e IP con 429 y Retry-After en login, registro, Google y consumo de tokens (D-17)
  - POST /api/usuarios/me/contrasena (exige la actual, devuelve sesión nueva, D-19) y /me/reenviar-confirmacion (3 por hora, D-21)
  - ClienteIp (IP de mejor esfuerzo) y ContrasenaCuenta (tope de 72 bytes, instante del cambio que nunca retrocede y envío de mails después del commit)
affects: [03-11, 03-12, 03-13, 03-14, fase-04]

plan_head_before: 747f5008a31ff4c5cffa0cfd7c2467926a515aad
plan_head_after: 6f495312edff5b5380acc2ba2a9a70bbafe037b2

actuals:
  tokens: 20300
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Los mails de cuenta se encolan después del commit: ContrasenaCuenta.despuesDelCommit registra un afterCommit si hay transacción y ejecuta directo si no"
    - "El login solo suma al contador los fallos de autenticación (bloqueado antes de intentar, registrarFallo al fallar, olvidar al entrar); la recuperación usa claves propias"
    - "passwordCambiadaEn avanza siempre al menos un segundo sobre el valor anterior, porque el claim pca se compara en segundos"
    - "Tests de límites con el LimitadorDeIntentos real y una IP y un mail propios por test (X-Forwarded-For) en un contexto compartido"

key-files:
  created:
    - src/main/java/com/danteautomotores/service/RecuperacionCuentaService.java
    - src/main/java/com/danteautomotores/service/ContrasenaCuenta.java
    - src/main/java/com/danteautomotores/security/ClienteIp.java
    - src/main/java/com/danteautomotores/dto/auth/ConfirmarEmailRequest.java
    - src/main/java/com/danteautomotores/dto/auth/OlvideContrasenaRequest.java
    - src/main/java/com/danteautomotores/dto/auth/RestablecerContrasenaRequest.java
    - src/main/java/com/danteautomotores/dto/auth/GoogleLoginRequest.java
    - src/main/java/com/danteautomotores/dto/usuario/CambiarContrasenaRequest.java
    - src/test/java/com/danteautomotores/service/RecuperacionCuentaServiceTest.java
    - src/test/java/com/danteautomotores/controller/AuthControllerTest.java
  modified:
    - src/main/java/com/danteautomotores/controller/AuthController.java
    - src/main/java/com/danteautomotores/controller/UsuarioController.java
    - src/main/java/com/danteautomotores/service/UsuarioService.java
    - src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java
    - src/test/java/com/danteautomotores/service/UsuarioServiceTest.java
    - src/test/java/com/danteautomotores/controller/UsuarioControllerTest.java

key-decisions:
  - "Restablecer y cambiar la contraseña fijan passwordCambiadaEn con LocalDateTime.now(clock) pero nunca por debajo ni dentro del mismo segundo del valor anterior (anterior + 1 s): el pca se compara en segundos y un retroceso o un empate dejaría vivas las sesiones viejas"
  - "Los mails (aviso de contraseña cambiada, confirmación reenviada) se encolan en afterCommit y no dentro de la transacción; el mail del link de recuperación sale después de que emitir() confirmó, porque solicitarRestablecimiento no es transaccional"
  - "solicitarRestablecimiento consulta siempre los dos contadores (mail e IP) sin cortocircuito, así ambos cuentan cada pedido; un límite excedido o una cuenta inexistente salen igual que un envío exitoso"
  - "El Retry-After de los 429 es la duración de la ventana (900 s o 3600 s): una cota superior de la espera real, sin calcular el resto de la ventana (el limitador no lo expone)"
  - "El cambio de contraseña desde el perfil limpia el contador de fallos de la contraseña actual al tener éxito"

patterns-established:
  - "Endpoints públicos de cuenta: todo es POST, y consumir un token nunca es un GET (escáneres de mail y prefetch)"

requirements-completed: [AUTH-01, AUTH-02, AUTH-04, AUTH-05]

coverage:
  - id: D1
    description: "Pedir el cambio de contraseña responde 200 con el mismo texto para dos mails distintos y aun después de diez logins fallidos del mismo mail; para una cuenta inexistente o un límite excedido el service no emite ni manda ni lanza"
    requirement: "AUTH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AuthControllerTest.java#olvideContrasenaResponde200ConElMismoTextoParaDosMailsDistintos"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AuthControllerTest.java#olvideContrasenaNoSeBloqueaPorLosFallosDeLogin"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/RecuperacionCuentaServiceTest.java#paraUnaCuentaInexistenteNoEmiteNiMandaNiLanza"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/RecuperacionCuentaServiceTest.java#laCuartaSolicitudDelMismoMailEnLaHoraNoMandaNadaNiLanza"
        status: pass
    human_judgment: false
  - id: D2
    description: "Restablecer con token válido cambia el hash, fija passwordCambiadaEn, confirma el mail, borra los tokens pendientes, avisa y no devuelve sesión; con token inválido da 400 con el mismo mensaje y no cambia nada; una contraseña de más de 72 bytes se rechaza sin gastar el token"
    requirement: "AUTH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/RecuperacionCuentaServiceTest.java"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AuthControllerTest.java#restablecerConTokenValidoDa200SinSesion"
        status: pass
    human_judgment: false
  - id: D3
    description: "Cambiar la contraseña desde el perfil exige la actual, devuelve la sesión de iniciarSesion con la cuenta ya actualizada (las demás caen por pca), limita 5 fallos por cuenta y manda a una cuenta solo-Google a Olvidé mi contraseña"
    requirement: "AUTH-05"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/UsuarioControllerTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "El undécimo login fallido del mismo mail, el trigésimo primero de la misma IP, el registro número 11 y el intento 31 de Google o de consumo de tokens dan 429 con Retry-After; un login correcto reinicia el contador y otro mail no está bloqueado"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AuthControllerTest.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "POST /api/auth/google devuelve la misma sesión que el login; sin credential da 400 con campos y con BadCredentialsException da 401; confirmar-email y restablecer-contrasena son POST (el GET da 405)"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/AuthControllerTest.java"
        status: pass
    human_judgment: false
  - id: D6
    description: "El reenvío de confirmación manda el mail con la cuenta sin confirmar, no hace nada ni gasta cupo con la cuenta confirmada y el cuarto reenvío en la hora lanza LimiteDeIntentosException (429)"
    requirement: "AUTH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java"
        status: pass
    human_judgment: false

duration: 25 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 10: Endpoints de cuenta, Google y límites de intentos Summary

**Confirmar mail, recuperar contraseña con respuesta uniforme y sin sesión automática, entrar con Google, cambiar la contraseña desde el perfil con sesión nueva y reenviar la confirmación, con límites de intentos por mail e IP (429 con Retry-After) y un contador de login que nunca bloquea la recuperación.**

## Performance

- **Duration:** 25 min
- **Tasks:** 3
- **Files modified:** 16 (10 creados, 6 modificados), todos en el back

## Accomplishments

- `RecuperacionCuentaService`: `confirmarEmail` (mensaje genérico para vencido, usado o inexistente), `solicitarRestablecimiento` (límites `reset:<mail>` 3/h y `reset-ip:<ip>` 10/h; nunca lanza ni distingue cuentas, y no es transaccional para que el token esté confirmado antes de que salga el mail) y `restablecerContrasena` (verifica los 72 bytes antes de gastar el token, confirma el mail, descarta tokens pendientes, avisa y no devuelve sesión).
- `AuthController` con `/confirmar-email`, `/olvide-contrasena`, `/restablecer-contrasena` y `/google`, todos POST. Login: 10 fallos por mail y 30 por IP cada 15 min (bloqueo antes de autenticar, `registrarFallo` solo ante `AuthenticationException`, `olvidar` al entrar); registro: 10 por IP por hora (D-17); Google y consumo de tokens: 30 por IP cada 15 min. Las claves de login y de recuperación son distintas, y un test lo prueba con el login ya bloqueado.
- `UsuarioService.cambiarContrasena` (actual obligatoria, 5 fallos por cuenta cada 15 min, cuenta solo-Google derivada a Olvidé mi contraseña, sesión nueva con `iniciarSesion` sobre la cuenta ya actualizada) y `reenviarConfirmacion` (3 por hora, nada si ya está confirmado); ambos con POST en `UsuarioController`.
- `ClienteIp` (primer valor de `X-Forwarded-For` o dirección remota, recortada a 64 caracteres para que un header gigante no infle las claves del limitador) y `ContrasenaCuenta` (tope de 72 bytes, instante del cambio y mails después del commit).

## Task Commits

1. **Tarea 1: confirmar mail y restablecer contraseña** - `9d5ad7d`
2. **Tarea 2: AuthController, Google y límites por mail e IP** - `865059c`
3. **Tarea 3: cambio de contraseña desde el perfil y reenvío de confirmación** - `6f49531`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- Los avisos de 03-03 y 03-06 se aplicaron como pedían: `passwordCambiadaEn` con `LocalDateTime.now(clock)` sin retroceder nunca, `descartarPendientes` al restablecer y al cambiar, y los mails encolados después del commit.
- El `Retry-After` es la duración de la ventana (900 o 3600 s), no el resto exacto: el limitador no expone el tiempo restante y la cota superior es segura para el cliente.
- `solicitarRestablecimiento` suma siempre a los dos contadores, así el IP también cuenta cuando se agotó el del mail.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] El instante del cambio de contraseña podía dejar vivas las sesiones anteriores**
- **Found during:** Tarea 1 (aviso de 03-03: "no retroceder nunca el valor")
- **Issue:** el aviso pedía usar el máximo entre el valor anterior y el nuevo, pero el claim `pca` se compara en segundos con `>=`: con el máximo, un cambio dentro del mismo segundo o con un reloj que retrocedió deja `pca` igual al de las sesiones viejas y no las cierra.
- **Fix:** `ContrasenaCuenta.marcarCambio` usa `now(clock)` y, si no supera en segundos al valor anterior, fija anterior + 1 s. Tests en `RecuperacionCuentaServiceTest` y `UsuarioServiceTest`.
- **Files modified:** `src/main/java/com/danteautomotores/service/ContrasenaCuenta.java` (archivo nuevo no listado en el plan)
- **Commit:** `9d5ad7d`

**2. [Rule 2 - Missing critical functionality] Mails fuera de la transacción**
- **Found during:** Tarea 1 y 3 (aviso de 03-06: "llamar a NotificacionesService después del commit")
- **Issue:** `restablecerContrasena`, `cambiarContrasena` y `reenviarConfirmacion` corren en una transacción; encolar el mail dentro enviaría el aviso aunque el commit fallara y, en el reenvío, el link podría llegar antes que el token.
- **Fix:** `ContrasenaCuenta.despuesDelCommit` registra un `afterCommit` si hay transacción y ejecuta directo si no (tests unitarios sin Spring).
- **Commit:** `9d5ad7d` y `6f49531`

### Notas de ejecución (no son desvíos de código)

- **Archivo nuevo no listado:** `ContrasenaCuenta.java` (los dos puntos de arriba). `AuthService` conserva su propio `verificarLargoDeContrasena` privado con el mismo texto: no se tocó para no salirse del alcance del plan; unificar es una limpieza menor pendiente.
- **`CambiarContrasenaRequest.actual`** lleva además `@Size(max = 200)` (el plan solo pedía `@NotBlank`): acota la entrada sin afectar a ninguna contraseña legítima (el tope real es 72).
- **TDD:** el plan no marca `tdd="true"`; tests e implementación se escribieron en la misma pasada y se verificaron en verde.
- **Tests de 03-03 sobre `GlobalExceptionHandlerTest`:** solo se agregaron `@Import(LimitadorDeIntentos.class)` y los dos `@MockBean`; las aserciones existentes no cambiaron.
- **Archivos tocados:** coinciden con `files_modified` del plan salvo `ContrasenaCuenta.java`.

**Total deviations:** 2 auto-fixed (Rule 2). **Impact:** las sesiones anteriores caen de verdad en cualquier cambio de contraseña y ningún mail sale antes ni sin el commit.

## Issues Encountered

- Un error de compilación del propio test (un helper `post` tapaba el import estático de `MockMvcRequestBuilders.post`); se renombró a `enviar`. Sin impacto en producción.

## Verificación

- `mvn -B -o -Djava.version=17 test -Dtest=RecuperacionCuentaServiceTest,TokenCuentaServiceTest,NotificacionesServiceTest -Ddante.pg.required=true`: 32 tests (12 + 10 + 10), 0 fallas.
- `mvn ... test -Dtest=AuthControllerTest,GlobalExceptionHandlerTest,SeguridadErroresTest,CorsOrigenesTest`: 64 tests (25 + 21 + 15 + 3), 0 fallas.
- `mvn ... test -Dtest=UsuarioServiceTest,UsuarioControllerTest,AuthControllerTest,RecuperacionCuentaServiceTest`: 79 tests (28 + 14 + 25 + 12), 0 fallas.
- Suite completa del back (`-o -Djava.version=17 -Ddante.pg.required=true`): 628 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS (antes 573; este plan suma 55: 12 RecuperacionCuentaService, 25 AuthController, 11 UsuarioService y 7 UsuarioController).
- Ningún test llamó a Brevo ni a Google; todos los mails, tokens y credenciales de los tests son ficticios y no se escribió ningún secreto.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador. Las pantallas (olvidé mi contraseña, restablecer, confirmar mail, botón de Google, cambio de contraseña en el perfil) son de 03-11, 03-12 y 03-13; la prueba real de envío y del botón de Google es del humo de 03-14.

## Known Stubs

None.

## Threat Flags

None. Los endpoints nuevos (`/api/auth/confirmar-email`, `/olvide-contrasena`, `/restablecer-contrasena`, `/google` y `/api/usuarios/me/contrasena` y `/me/reenviar-confirmacion`) están cubiertos por T-03-53 a T-03-60 del plan: T-03-53 (límites de login por mail e IP, test del undécimo intento), T-03-54 (respuesta idéntica, dos mails), T-03-55 y T-03-56 (un solo uso, POST, sin sesión automática, cierre de sesiones por pca), T-03-57 (3 y 10 por hora y 3 reenvíos), T-03-58 (5 fallos por cuenta) y T-03-60 (DTOs con `@ToString.Exclude` y sin logs de token, mail ni IP) quedaron mitigadas y probadas; T-03-59 (X-Forwarded-For falsificable) se acepta y está documentada en `ClienteIp`.

## User Setup Required

None - no external service configuration required en este plan.

## Next Phase Readiness

- 03-12 (front) puede consumir el contrato: `POST /api/auth/confirmar-email {token}`, `/olvide-contrasena {email}`, `/restablecer-contrasena {token, password}` (200 `{mensaje}`, sin sesión: el front lleva a `/login`), `POST /api/auth/google {credential}` (200 `AuthResponse` con `faltantes`), `POST /api/usuarios/me/contrasena {actual, nueva}` (200 `AuthResponse` con token nuevo: el front debe reemplazar el token guardado) y `POST /api/usuarios/me/reenviar-confirmacion` (200 `{mensaje}`). Las páginas deben leer `?token=` y hacer POST, nunca un GET con efecto.
- Los 429 traen `Retry-After` y `{error}`; el front debe mostrar el mensaje y no reintentar solo.
- El back sigue sin poder pushearse a producción: el `SecretosGuard` de 03-04 exige las variables de Brevo y Google de 03-14.
- Limitación aceptada: los límites viven en memoria de una instancia; con varias instancias hay que mover los contadores a la base.

## Self-Check: PASSED

- Archivos creados presentes en disco: RecuperacionCuentaService, ContrasenaCuenta, ClienteIp, los cinco DTOs nuevos, RecuperacionCuentaServiceTest y AuthControllerTest.
- Commits presentes: `9d5ad7d`, `865059c` y `6f49531`.
