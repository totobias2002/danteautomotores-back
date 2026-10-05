---
phase: 03-cuentas-verificadas
plan: 07
subsystem: auth
tags: [google, id-token, nimbus-jwt, jwks, pre-hijacking, vinculacion-de-cuentas]

requires:
  - phase: 03-cuentas-verificadas
    provides: Usuario con googleSub, passwordCambiadaEn y passwordHash nullable, findByGoogleSub y findByEmailIgnoreCase (plan 03-01); AuthService.iniciarSesion y cuentas sin contraseña autenticables (03-03); app.google.client-id y ClockConfig (03-04)
provides:
  - GoogleIdTokenVerifier (firma contra el JWKS de Google, emisor, audiencia y vencimiento con 60 s de tolerancia) que devuelve IdentidadGoogle o lanza BadCredentialsException con mensaje fijo
  - IdentidadGoogle (record sin tipos de Spring Security)
  - GoogleAuthService.entrar(credential) con la política de vinculación D-06 y la defensa anti pre-hijacking D-15, y emisión de la sesión por AuthService.iniciarSesion
affects: [03-10, 03-12, 03-14, fase-04]

plan_head_before: 22ae66812899330b415bbae16285e0d85d458365
plan_head_after: f2f72a88c0e81e1ae9b19324b05977b8e15e8e42

actuals:
  tokens: 9600
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Verificación de ID token con NimbusJwtDecoder y DelegatingOAuth2TokenValidator (JwtTimestampValidator, emisor y audiencia propios); un constructor de tests recibe un decoder con clave pública RSA generada para probar sin red"
    - "Servicio de alta con reintento ante carrera deliberadamente sin @Transactional: saveAndFlush, captura de DataIntegrityViolationException y segunda búsqueda por la clave única"

key-files:
  created:
    - src/main/java/com/danteautomotores/security/GoogleIdTokenVerifier.java
    - src/main/java/com/danteautomotores/security/IdentidadGoogle.java
    - src/main/java/com/danteautomotores/service/GoogleAuthService.java
    - src/test/java/com/danteautomotores/security/GoogleIdTokenVerifierTest.java
    - src/test/java/com/danteautomotores/service/GoogleAuthServiceTest.java
  modified: []

key-decisions:
  - "El constructor de tests del verificador recibe un NimbusJwtDecoder (no un JwtDecoder genérico): la interfaz JwtDecoder no permite fijar el validador de emisor, audiencia y vencimiento, y con la clase concreta los tests ejercen exactamente la misma validación que producción"
  - "Un token sin given_name ni name crea la cuenta con la parte local del mail como nombre (el nombre es obligatorio en la cuenta y se corrige en Completá tus datos)"
  - "Un token con email_verified verdadero pero sin mail se trata como mail no confirmado: sin mail no hay unión ni alta"
  - "Los rechazos de la política (mail no confirmado, admin, cuenta de otro Google) son ReglaDeNegocioException (400); un token inválido es BadCredentialsException (401)"

patterns-established:
  - "Las validaciones de rechazo corren antes de mutar la entidad, así una cuenta rechazada queda idéntica y no hace falta deshacer nada"

requirements-completed: [AUTH-02]

coverage:
  - id: D1
    description: "El verificador acepta solo un ID token de Google bien firmado, con el emisor correcto (con o sin https), la audiencia igual al Client ID y vigente (60 s de tolerancia); rechaza audiencia o emisor ajenos, vencidos hace más de 60 s, malformados, vacíos, nulos, firmados con otra clave RSA y cualquier token con el Client ID en blanco"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/GoogleIdTokenVerifierTest.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "email_verified se interpreta con tolerancia (booleano true o texto true), nombre y apellido salen de given_name y family_name con respaldo en name, y la excepción lleva un mensaje fijo sin el token"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/GoogleIdTokenVerifierTest.java"
        status: pass
    human_judgment: false
  - id: D3
    description: "Un sub ya vinculado entra a su cuenta; un sub nuevo sin cuenta crea un COMPRADOR con mail en minúsculas confirmado, sin teléfono, DNI ni contraseña; un token inválido o con email_verified falso no toca la base"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/GoogleAuthServiceTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "La unión por mail conserva la contraseña de una cuenta con mail confirmado y descarta la contraseña (con passwordCambiadaEn igual al reloj) de una con mail sin confirmar (anti pre-hijacking, D-15), sin perder datos; completa el apellido solo si faltaba"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/GoogleAuthServiceTest.java#unaCuentaConMailSinConfirmarSeUneDescartaLaContrasenaYMarcaElInstante"
        status: pass
    human_judgment: false
  - id: D5
    description: "Una cuenta ADMIN, una cuenta ya unida a otro Google y un mail no confirmado se rechazan sin modificar ni guardar nada; la carrera de dos ingresos reintenta la búsqueda por sub; el servicio no es transaccional"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/GoogleAuthServiceTest.java"
        status: pass
    human_judgment: false

duration: 12 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 07: Login con Google (back) Summary

**Verificador del ID token de Google con NimbusJwtDecoder contra el JWKS oficial (iss, aud y exp) y política de vinculación por sub o por mail confirmado que descarta la contraseña de cuentas con mail sin confirmar (anti pre-hijacking) y deja afuera a los admins.**

## Performance

- **Duration:** 12 min
- **Tasks:** 2
- **Files modified:** 5 (todos creados), todos en el back

## Accomplishments

- `GoogleIdTokenVerifier` arma `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` (RS256, caché de 5 minutos, no baja nada hasta el primer uso) con tres validadores: `JwtTimestampValidator(60 s)`, emisor (`https://accounts.google.com` o `accounts.google.com`) y audiencia igual a `app.google.client-id`; con el Client ID en blanco nada pasa. Cualquier excepción del decoder, un token nulo o vacío o un fallo de red al bajar el JWKS se traduce a `BadCredentialsException("No pudimos verificar tu cuenta de Google")`, sin el token ni causa encadenada.
- `GoogleAuthService.entrar` aplica la política en el orden del plan: verificar, exigir `email_verified`, buscar por `sub`, buscar por mail (minúsculas), rechazar ADMIN y cuentas de otro Google, vincular o crear, y emitir la sesión con `AuthService.iniciarSesion` (que ya informa los faltantes para llevar a Completá tus datos).
- D-15: si la cuenta tenía el mail sin confirmar y una contraseña, la unión pone `passwordHash = null` y `passwordCambiadaEn = LocalDateTime.now(clock)`; así quien registró el mail antes que su dueño pierde el acceso y el claim `pca` corta sus sesiones. Con el mail confirmado se conserva la contraseña (los dos métodos).
- La carrera de dos ingresos simultáneos se resuelve con `saveAndFlush`, captura de `DataIntegrityViolationException` y una segunda búsqueda por sub; el servicio no lleva `@Transactional` y un test lo fija.
- Probado sin red: el test del verificador genera dos pares RSA y firma los tokens con `SignedJWT`/`RSASSASigner`. Una mutación (quitar `setPasswordHash(null)`) hace fallar el test de D-15, así que no es vacuo.

## Task Commits

1. **Tarea 1: verificador del ID token de Google** - `16c059c`
2. **Tarea 2: política de vinculación de cuentas con Google** - `f2f72a8`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- El constructor de tests recibe `NimbusJwtDecoder` y no `JwtDecoder` (ver `key-decisions`): es el único tipo sobre el que se puede fijar el validador completo, y los tests prueban la misma validación que producción.
- Sin `given_name` ni `name` el nombre sale de la parte local del mail; sin mail (aunque `email_verified` sea verdadero) se rechaza como mail no confirmado.
- La cuenta ya unida a otro Google se rechaza con "Esta cuenta ya está vinculada a otra cuenta de Google." (el plan solo pedía una `ReglaDeNegocioException`).

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written, con las dos precisiones de diseño de arriba (constructor de tests con `NimbusJwtDecoder`; nombre de respaldo).

### Notas de ejecución (no son desvíos de código)

- **TDD:** el plan marca ambas tareas como `tdd="true"`, pero la implementación se escribió junto con los tests y no hubo un commit RED separado: los tests pasaron a la primera. Como control de que no son vacuos se hizo una mutación puntual en D-15 (quitar el borrado de la contraseña) y falló exactamente `unaCuentaConMailSinConfirmarSeUneDescartaLaContrasenaYMarcaElInstante`; después se restauró el archivo. No se corrió `gsd_run check tdd-red-evidence`.
- **Tests extra (no pedidos):** token sin audiencia, token sin `sub`, Client ID en blanco, mismatch de `email_verified` con valores raros (`"yes"`, `1`), token sin ningún nombre, cuenta solo-Google que se vuelve a unir (no marca cambio de contraseña), Google sin apellido que no pisa el de la cuenta, token sin mail.
- **Archivos tocados:** coinciden con `files_modified` del plan; no hubo otros.
- **Sin endpoint todavía:** `GoogleAuthService` no está expuesto por HTTP; el `POST /api/auth/google` con su límite de intentos es de 03-10 y el botón del front es de 03-12.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificación

- `mvn -B -o -Djava.version=17 test -Dtest=GoogleIdTokenVerifierTest`: 16 tests, 0 fallas.
- `mvn ... test -Dtest=GoogleAuthServiceTest,GoogleIdTokenVerifierTest,AuthServiceTest`: 43 tests (17 + 16 + 10), 0 fallas.
- Suite completa del back (`-Ddante.pg.required=true`): 530 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS (antes 497; este plan suma 33: 16 GoogleIdTokenVerifier y 17 GoogleAuthService). El bean nuevo `GoogleIdTokenVerifier` se crea en los contextos de Spring con el Client ID en blanco sin romper ningún test.
- Nunca se llamó a Google ni se escribió ningún Client ID real, clave ni contraseña: los tests usan pares RSA generados al vuelo y un Client ID ficticio.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador. La prueba del botón de Google con una cuenta real y el Client ID creado por el usuario es del humo de 03-14.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints ni rutas de acceso nuevas (el controlador llega en 03-10). T-03-31 a T-03-36 quedaron mitigadas y probadas: token ajeno, vencido, malformado o con otra clave rechazado (T-03-31), contraseña descartada en la unión con mail sin confirmar (T-03-32), cuenta ADMIN rechazada sin cambios (T-03-33), identidad por sub y unión solo con `email_verified` (T-03-34), reintento por sub sin `@Transactional` (T-03-35), mensaje fijo y sin logs de token, sub ni mail (T-03-36).

## User Setup Required

None en este plan. El Client ID de Google (`GOOGLE_CLIENT_ID`) lo crea el usuario en 03-14; hasta entonces, con el valor en blanco, el verificador rechaza todo token (comportamiento esperado en desarrollo).

## Next Phase Readiness

- 03-10 puede exponer `POST /api/auth/google {credential}` llamando a `GoogleAuthService.entrar`, con su límite de intentos (`google-ip`). Los rechazos de la política salen como 400 con su mensaje y un token inválido como 401 con el cuerpo fijo del advice.
- Cuando la cuenta se une por mail sin confirmar, el front debe indicar al dueño real que defina una contraseña nueva con "olvidé mi contraseña" si quiere usar los dos métodos (la contraseña anterior fue descartada).
- Recordatorio: el back con `SecretosGuard` no se debe desplegar hasta cargar las variables de 03-14.

## Self-Check: PASSED

- Archivos creados presentes en disco: GoogleIdTokenVerifier, IdentidadGoogle, GoogleAuthService, GoogleIdTokenVerifierTest y GoogleAuthServiceTest.
- Commits presentes: `16c059c` y `f2f72a8`.
