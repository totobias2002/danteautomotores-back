---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 02
subsystem: auth
tags: [spring-boot, applicationrunner, seed, bcrypt, mockito, env-vars]
status: complete

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "Plan 01-01: panel admin tracer, errores 401/403 uniformes y SeguridadWebMvcTestBase"
provides:
  - "DataSeeder (ApplicationRunner): siembra la agencia inicial 'Dante Automotores' si no hay ninguna y la cuenta ADMIN desde ADMIN_EMAIL/ADMIN_PASSWORD/ADMIN_NOMBRE si no hay ningun admin"
  - "UsuarioRepository.existsByRol(Rol)"
  - "Bloque app.admin.* en application.yml con default vacio (sin credenciales en el repo)"
  - "Contrato por test: el registro web siempre crea COMPRADOR aunque el body traiga rol ADMIN"
  - "README: variables del admin, SPRING_PROFILES_ACTIVE y seccion Tests"
affects: [01-03, 01-04, fase-2-deploy]

actuals:
  tokens: 5000
  tasks: 2
  commits: 3
plan_head_before: 83b9662d3af259198db66430e9a2c50c911f1848
plan_head_after: 719d545331f50fabbcd1d81c70d7f56a5962577c

tech-stack:
  added: []
  patterns:
    - "Seed de arranque con ApplicationRunner @Transactional, valores por @Value con default vacio"
    - "fallarOAvisar: IllegalStateException con perfil prod, log.warn en el resto"
    - "Tests unitarios de componentes de arranque con Mockito + MockEnvironment + OutputCaptureExtension (sin DB)"

key-files:
  created:
    - src/main/java/com/danteautomotores/config/DataSeeder.java
    - src/test/java/com/danteautomotores/config/DataSeederTest.java
    - src/test/java/com/danteautomotores/service/AuthServiceTest.java
    - .planning/phases/01-gesti-n-del-inventario-por-el-admin/01-USER-SETUP.md
  modified:
    - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
    - src/main/resources/application.yml
    - README.md

key-decisions:
  - "El seed no sincroniza: con un ADMIN existente no toca ni contrasena ni nombre (D-02)"
  - "Si ADMIN_EMAIL ya pertenece a una cuenta existente, falla (prod) o avisa; nunca promueve (D-04)"
  - "Rol.ADMIN aparece en un unico archivo de main: DataSeeder"
  - "Agencia inicial solo con nombre y slug; soporta varias agencias (D-05..D-07 revisados), sin restriccion de agencia unica"

patterns-established:
  - "Mensajes de fallo del seed nombran la variable pero nunca incluyen la password ni su hash"

requirements-completed: [ADM-01]

coverage:
  - id: D1
    description: "El seed crea la cuenta ADMIN con password BCrypt cuando no hay admin y las variables son validas; no toca nada si ya hay admin"
    requirement: ADM-01
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/DataSeederTest.java (11 tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Con perfil prod, variables faltantes o invalidas (y email ya registrado) abortan el arranque con IllegalStateException; sin prod solo se avisa"
    requirement: ADM-01
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/DataSeederTest.java"
        status: pass
    human_judgment: true
    rationale: "El aborto real del arranque de Spring Boot contra Postgres no esta cubierto por tests (Docker apagado); se verifica manualmente"
  - id: D3
    description: "El registro web siempre crea COMPRADOR aunque el body traiga rol ADMIN; RegistroRequest no tiene campo rol"
    requirement: ADM-01
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java (3 tests)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Agencia inicial sembrada solo si no hay ninguna"
    requirement: ADM-01
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/DataSeederTest.java#sinAgencias_siembraDanteAutomotores"
        status: pass
    human_judgment: false
---

# Phase 1 Plan 02: Seed de la cuenta admin y agencia inicial Summary

**DataSeeder (ApplicationRunner) crea el admin unico desde ADMIN_EMAIL/ADMIN_PASSWORD/ADMIN_NOMBRE con BCrypt, aborta el arranque en prod si faltan, y el registro web queda fijado a COMPRADOR por test.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-10-03T02:18:12Z
- **Completed:** 2026-10-03T02:21Z
- **Tasks:** 2
- **Files modified:** 7 (incluye USER-SETUP.md)

## Accomplishments

- `DataSeeder` siembra "Dante Automotores" (slug `dante-automotores`) solo si `agencias` esta vacia, y la cuenta ADMIN solo si no existe ningun admin (D-01, D-02, D-05).
- Validaciones del seed (variables en blanco, email sin `@`, password de menos de 8 caracteres, email ya registrado) llaman a `fallarOAvisar`: `IllegalStateException` con perfil `prod`, `log.warn` sin el.
- Nunca se loguea la password ni su hash, ni se promueve una cuenta existente (D-04); lo prueban 11 tests con `OutputCaptureExtension`.
- `AuthServiceTest` fija que `{"rol":"ADMIN"}` en el body del registro se ignora y `RegistroRequest` no declara campo de rol.
- README documenta las variables del admin, `SPRING_PROFILES_ACTIVE` y como correr los tests offline con JDK 17.

## Task Commits

1. **Tarea 1 RED: tests de DataSeeder** - `6d88130` (test)
2. **Tarea 1 GREEN: DataSeeder + existsByRol + app.admin** - `2fb1753` (feat)
3. **Tarea 2: AuthServiceTest + README** - `719d545` (test)

**Plan metadata:** commit docs(01-02) a continuacion.

## Files Created/Modified

- `src/main/java/com/danteautomotores/config/DataSeeder.java` - ApplicationRunner que siembra agencia inicial y admin
- `src/main/java/com/danteautomotores/repository/UsuarioRepository.java` - `existsByRol(Rol)`
- `src/main/resources/application.yml` - bloque `app.admin.*` con defaults vacios
- `src/test/java/com/danteautomotores/config/DataSeederTest.java` - 11 tests sin DB
- `src/test/java/com/danteautomotores/service/AuthServiceTest.java` - contrato de D-04
- `README.md` - variables del admin y seccion Tests
- `.planning/phases/01-gesti-n-del-inventario-por-el-admin/01-USER-SETUP.md` - variables que debe setear el dueno

## Decisions Made

Ver `key-decisions` del frontmatter. Se respeta la revision de D-05..D-07 (varias agencias): el seed no impone agencia unica, solo crea la inicial cuando no hay ninguna.

## Deviations from Plan

None - plan executed exactly as written.

Nota de proceso: la Tarea 2 no tuvo ciclo RED porque el plan indica no modificar `AuthService` (ya fuerza COMPRADOR); `AuthServiceTest` es un test de caracterizacion que pasa desde el primer momento y fija el contrato.

**Total deviations:** 0.

## Issues Encountered

None.

## User Setup Required

Si. Ver `01-USER-SETUP.md`: el dueno debe elegir y setear `ADMIN_EMAIL`, `ADMIN_PASSWORD` (8+ caracteres) y `ADMIN_NOMBRE` antes de arrancar contra una base sin admin.

## Verification

- `mvn -B -o -Djava.version=17 test -Dtest=AuthServiceTest,DataSeederTest`: 14 tests, 0 fallas.
- Suite completa: 27 tests, 0 fallas.
- `grep -rl 'Rol.ADMIN' src/main/java` lista solo `DataSeeder.java`.
- Todos los `acceptance_criteria` de ambas tareas verificados (DataSeederTest con 11 `@Test`, `existsByRol`, `Profiles.of("prod")`, defaults vacios en el yml, README con las variables, `SPRING_PROFILES_ACTIVE` y `-Djava.version=17`).
- Pendiente (humano, requiere Postgres real): arranque completo con login del admin, reinicio con otra password y arranque en prod sin variables.

## Known Stubs

None.

## Threat Flags

None. Las amenazas T-01-07..T-01-11 del plan quedaron mitigadas segun el threat_model.

## Next Phase Readiness

Listo para 01-03 y 01-04 (no comparten archivos). En una base nueva el dueno ya puede setear `ADMIN_*` y entrar al panel.

## Self-Check: PASSED

- FOUND: DataSeeder.java, DataSeederTest.java, AuthServiceTest.java, 01-USER-SETUP.md
- FOUND commits: 6d88130, 2fb1753, 719d545
