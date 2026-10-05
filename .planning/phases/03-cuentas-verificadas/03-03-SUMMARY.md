---
phase: 03-cuentas-verificadas
plan: 03
subsystem: auth
tags: [jwt, spring-security, caffeine, sesiones, rate-limit]

requires:
  - phase: 03-cuentas-verificadas
    provides: VerificacionCuenta, findByEmailIgnoreCase y Caffeine en el pom (plan 03-01); LimiteDeIntentosException (plan 03-02)
provides:
  - CuentaUserDetails (UserDetails con el instante del último cambio de contraseña y hash inválido constante si no hay contraseña)
  - Claim pca en el JWT y validación que cierra las sesiones anteriores a un cambio de contraseña (D-19)
  - AuthService.iniciarSesion(Usuario) como único emisor de sesiones y login con mail normalizado que rechaza cuentas sin contraseña
  - LimitadorDeIntentos (contadores en memoria con ventana, tope de claves y reloj inyectable)
affects: [03-04, 03-05, 03-07, 03-08, 03-09, 03-10, fase-04]

plan_head_before: 9a6666b971735ced017c3a7bd2bd0938ef527e5b
plan_head_after: 7dd2e32790de8891c9e081236863fcdff32fc0e9

actuals:
  tokens: 9800
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Toda sesión se emite con AuthService.iniciarSesion y un CuentaUserDetails; el claim pca va siempre en el token (también en la variante con claims extra de JwtService)"
    - "Cuenta sin contraseña = hash inválido '!' (BCrypt devuelve false sin lanzar), nunca password nulo en User.builder()"
    - "Limitador sin excepciones: quien lo llama decide cuándo lanzar LimiteDeIntentosException; un caché Caffeine por ventana con mantenimiento en el hilo que llama"

key-files:
  created:
    - src/main/java/com/danteautomotores/security/CuentaUserDetails.java
    - src/main/java/com/danteautomotores/service/LimitadorDeIntentos.java
    - src/test/java/com/danteautomotores/security/JwtServiceTest.java
    - src/test/java/com/danteautomotores/security/CuentaSoloGoogleTest.java
    - src/test/java/com/danteautomotores/service/LimitadorDeIntentosTest.java
  modified:
    - src/main/java/com/danteautomotores/security/CustomUserDetailsService.java
    - src/main/java/com/danteautomotores/security/JwtService.java
    - src/main/java/com/danteautomotores/service/AuthService.java
    - src/test/java/com/danteautomotores/service/AuthServiceTest.java

key-decisions:
  - "El claim pca se agrega dentro de generateToken(Map, UserDetails) y no solo en la variante simple: ningún emisor puede saltearse el cierre de sesiones"
  - "pca se compara en segundos de época UTC con la misma función (CuentaUserDetails.segundosDe) al emitir y al validar; un token sin claim vale 0"
  - "Login: la cuenta sin contraseña se rechaza antes de llamar al AuthenticationManager con BadCredentialsException('Credenciales inválidas'); un mail inexistente sigue pasando por el AuthenticationManager (mismo 401 y mismo costo de BCrypt que una contraseña incorrecta)"

patterns-established:
  - "Un caché de Caffeine por ventana (Duration) indexado en un ConcurrentHashMap; la misma clave en dos ventanas cuenta por separado"

requirements-completed: [AUTH-02, AUTH-04]

coverage:
  - id: D1
    description: "Una cuenta sin contraseña (solo Google) se autentica con su JWT en el filtro real, con su rol, y no queda sin autenticar en silencio"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/CuentaSoloGoogleTest.java#elFiltroRealAutenticaALaCuentaSinContrasenaConSuRol"
        status: pass
    human_judgment: false
  - id: D2
    description: "Un token emitido antes de un cambio de contraseña deja de valer, uno posterior vale y los tokens viejos sin claim pca valen hasta el primer cambio"
    requirement: "AUTH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/JwtServiceTest.java"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/CuentaSoloGoogleTest.java#elFiltroNoAutenticaUnTokenAnteriorAlCambioDeContrasena"
        status: pass
    human_judgment: false
  - id: D3
    description: "El login normaliza el mail, rechaza con el 401 genérico a una cuenta sin contraseña sin llamar al AuthenticationManager, y todas las sesiones salen de iniciarSesion con una CuentaUserDetails"
    requirement: "AUTH-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "El limitador cuenta por clave y ventana, vence solo con un reloj falso, no suma al consultar, tiene tope de 10.000 claves y no lanza excepciones"
    requirement: "AUTH-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/LimitadorDeIntentosTest.java"
        status: pass
    human_judgment: false

duration: 6 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 03: Sesión lista para cuentas nuevas Summary

**CuentaUserDetails con claim pca en el JWT (cambiar la contraseña cierra las otras sesiones), iniciarSesion como único emisor de sesiones, login que rechaza cuentas sin contraseña con el 401 genérico y un limitador de intentos en memoria con Caffeine.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-10-05T18:53:50Z
- **Completed:** 2026-10-05T18:59:30Z
- **Tasks:** 3
- **Files modified:** 9 (5 creados, 4 modificados), todos en el back

## Accomplishments

- La trampa del password nulo queda resuelta antes de crear la primera cuenta solo-Google (03-07): `CuentaUserDetails.de(usuario)` usa la constante "!" cuando no hay hash, y un test con el `JwtAuthenticationFilter` real comprueba que la cuenta queda autenticada con su rol. El filtro no se modificó.
- D-19: `JwtService` agrega `pca` (segundos de época UTC del último cambio de contraseña) a todo token y `isTokenValid` exige `pca del token >= pca de la cuenta`. Los tokens viejos sin claim y los `User` planos de `SeguridadWebMvcTestBase` siguen valiendo mientras la cuenta no cambió su contraseña.
- `AuthService.iniciarSesion(Usuario)` reemplaza a `construirRespuesta` y es el único punto de emisión; `registrar` y `login` lo usan. `login` recorta y pasa a minúsculas el mail, busca con `findByEmailIgnoreCase` y rechaza una cuenta sin contraseña con `BadCredentialsException` sin llamar al `AuthenticationManager`.
- `LimitadorDeIntentos` (`intentar`, `bloqueado`, `registrarFallo`, `olvidar`) con un caché Caffeine por ventana, `maximumSize(10_000)`, `expireAfterWrite` y `Ticker` inyectable; las dos limitaciones aceptadas (instancia única y IP de mejor esfuerzo) están documentadas en la clase.

## Task Commits

1. **Tarea 1: sesión a prueba de cuentas sin contraseña y claim pca** - `d278f32`
2. **Tarea 2: iniciarSesion y login a prueba de mayúsculas y cuentas sin contraseña** - `10e8256`
3. **Tarea 3 (TDD): limitador de intentos**
   - RED: `cce2453` (tests con un esqueleto que compila pero no cuenta)
   - GREEN: `7dd2e32`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- `pca` se agrega en `generateToken(Map, UserDetails)` (copiando el mapa de claims extra) para que ninguna ruta de emisión lo omita.
- Un mail inexistente en el login sigue pasando por el `AuthenticationManager`: da la misma excepción y el mismo costo de BCrypt que una contraseña incorrecta, sin revelar si la cuenta existe.
- `LimitadorDeIntentos` corre el mantenimiento de Caffeine en el hilo que llama (`executor(Runnable::run)`): no hay hilos de limpieza y el tope de tamaño es determinista en los tests.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecución (no son desvíos de código)

- **TDD de la Tarea 3:** el RED se hizo con un esqueleto que compila y devuelve valores incorrectos, así que fallaron por aserción (10 de 13 tests; los otros 3 pasan vacuamente con un esqueleto: sin excepciones, clave sin historial y tope de memoria). Se commiteó como `test(...)` con el esqueleto y después `feat(...)`. No se corrió `gsd_run check tdd-red-evidence` (no hay registro JSON persistido); el fallo se observó en la salida de Maven.
- **Compuerta:** `commits: 4` es el conteo medido (`rev-list 9a6666b..HEAD` antes de este SUMMARY).
- **Archivos tocados:** coinciden con `files_modified` del plan; no hubo otros.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificación

- `mvn -B -o -Djava.version=17 test -Dtest=JwtServiceTest,CuentaSoloGoogleTest,SeguridadErroresTest,CorsOrigenesTest`: 30 tests (8 + 4 + 15 + 3), 0 fallas.
- `mvn ... test -Dtest=AuthServiceTest,JwtServiceTest,CuentaSoloGoogleTest,GlobalExceptionHandlerTest`: 43 tests (10 + 8 + 4 + 21), 0 fallas.
- `mvn ... test -Dtest=LimitadorDeIntentosTest`: RED con 10 fallas por aserción y 13 tests verdes después de la implementación.
- Suite completa del back (`-Ddante.pg.required=true`): 411 tests, 0 fallas, 0 errores, BUILD SUCCESS (antes 379; este plan suma 32: 8 JwtService, 4 CuentaSoloGoogle, 7 AuthService, 13 LimitadorDeIntentos).

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints nuevos: `LimitadorDeIntentos` todavía no lo usa ningún controlador (llega en 03-08, 03-09 y 03-10) y T-03-10 a T-03-12 y T-03-14 quedaron mitigadas y probadas.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 03-07 (Google) y 03-10 (cambio y recuperación de contraseña) pueden llamar a `AuthService.iniciarSesion`; para que un cambio de contraseña cierre las demás sesiones solo hay que fijar `passwordCambiadaEn` (con `LocalDateTime` en UTC) antes de emitir la sesión nueva.
- El `LimitadorDeIntentos` está listo como `@Component` para login, registro, recuperación y tope de mails; el login todavía no lo usa.
- Aviso para 03-10: el claim usa `passwordCambiadaEn` tal cual, leído como UTC, tanto al emitir como al validar; la comparación es consistente sin importar la zona. El `Clock` del proyecto es `systemDefaultZone()`, así que si el servidor cambiara de zona o hubiera un retroceso por horario de verano, un cambio de contraseña podría quedar con un valor menor al anterior; conviene guardar `passwordCambiadaEn` con `LocalDateTime.now(clock)` y no retroceder nunca el valor (usar el máximo entre el valor anterior y el nuevo).

## Self-Check: PASSED

- Archivos creados presentes en disco: CuentaUserDetails, LimitadorDeIntentos, JwtServiceTest, CuentaSoloGoogleTest y LimitadorDeIntentosTest.
- Commits presentes: `d278f32`, `10e8256`, `cce2453` y `7dd2e32`.
