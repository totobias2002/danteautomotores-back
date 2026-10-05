---
phase: 03-cuentas-verificadas
plan: 01
subsystem: auth
tags: [flyway, postgres, spring-boot, jpa, react, libphonenumber, caffeine, oauth2-jose]

requires:
  - phase: 02-catalogo-publico-real-en-produccion
    provides: Flyway V1 a V4 con baseline-version 1 y ddl-auto validate; PostgresLocalTestBase; con-back-local.sh
provides:
  - Migración V5 (apellido, dni único con formato, email_confirmado, google_sub, password_cambiada_en, password_hash nullable, índice único lower(email), tabla tokens_cuenta)
  - Entidad Usuario ampliada y regla única VerificacionCuenta (faltantes, estaVerificada) con el enum DatoFaltante
  - AuthResponse aditivo (apellido, emailConfirmado, cuentaVerificada, faltantes) sin DNI ni teléfono
  - Banner BannerCuentaIncompleta y faltantes guardados en la sesión del front
  - Dependencias de la fase en el pom (oauth2-jose, caffeine, libphonenumber 9.0.40) y búsquedas de UsuarioRepository por mail sin mayúsculas, DNI y sub de Google
affects: [03-02, 03-03, 03-08, 03-09, fase-04, fase-05]

plan_head_before: 4b3a9734d9b7f8b5ac151d573e21662d6603292c
plan_head_after: ff7684a832a58fc219756f77df6980d732445afa

actuals:
  tokens: 14000
  tasks: 2
  commits: 2

tech-stack:
  added: [spring-security-oauth2-jose 6.3.4 (BOM), caffeine 3.1.8 (BOM), libphonenumber 9.0.40]
  patterns:
    - "Regla de cuenta verificada en un solo componente (VerificacionCuenta); la obligatoriedad vive ahí y no en la base"
    - "Migración aditiva y reversible por deploy: sin UPDATE de datos de usuarios salvo el mail confirmado de los ADMIN"

key-files:
  created:
    - src/main/resources/db/migration/V5__fase3_cuentas_verificadas.sql
    - src/main/java/com/danteautomotores/enums/DatoFaltante.java
    - src/main/java/com/danteautomotores/service/VerificacionCuenta.java
    - src/test/java/com/danteautomotores/service/VerificacionCuentaTest.java
    - scripts/verify/cuentas-humo.js
    - ../danteautomotores-front/src/components/BannerCuentaIncompleta.jsx
  modified:
    - src/main/java/com/danteautomotores/entity/Usuario.java
    - src/main/java/com/danteautomotores/dto/auth/AuthResponse.java
    - src/main/java/com/danteautomotores/service/AuthService.java
    - src/main/java/com/danteautomotores/repository/UsuarioRepository.java
    - src/main/java/com/danteautomotores/config/DataSeeder.java
    - src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java
    - src/test/java/com/danteautomotores/service/AuthServiceTest.java
    - src/test/java/com/danteautomotores/config/DataSeederTest.java
    - pom.xml
    - ../danteautomotores-front/src/context/AuthContext.jsx
    - ../danteautomotores-front/src/App.jsx

key-decisions:
  - "V5 es el número de la migración (el research la llamaba V4; V4 ya es solicitudes_venta): nunca se edita V1 a V4"
  - "AuthService queda sin tocar el contrato de entrada: apellido, teléfono y DNI siguen opcionales hasta 03-09"
  - "El front no asume que faltantes existe: una sesión guardada antes de esta fase no muestra banner"

patterns-established:
  - "Humo de la fase: scripts/verify/cuentas-humo.js con la misma estructura que catalogo-humo.js; los planes siguientes le suman chequeos"
  - "Respuestas de auth y sesión del front nunca llevan DNI ni teléfono (T-03-01)"

requirements-completed: [AUTH-03, AUTH-05]

coverage:
  - id: D1
    description: "V5 se aplica sobre una producción simulada (V1 con datos, baseline y V2 a V5) sin perder filas, con UNIQUE de dni, CHECK de formato, índice único lower(email), password_hash nullable y tabla tokens_cuenta"
    requirement: "AUTH-03"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java#v5SobreProduccionSimuladaNoTocaFilasYDejaLasRestriccionesDeIdentidad"
        status: pass
    human_judgment: false
  - id: D2
    description: "Regla de cuenta verificada: apellido, teléfono, DNI y mail confirmado; un ADMIN nunca tiene faltantes"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/VerificacionCuentaTest.java"
        status: pass
    human_judgment: false
  - id: D3
    description: "Registro y login devuelven apellido, emailConfirmado, cuentaVerificada y faltantes, y no devuelven dni ni telefono"
    requirement: "AUTH-05"
    verification:
      - kind: e2e
        ref: "bash scripts/verify/con-back-local.sh --vacia dante_humo_cuentas node scripts/verify/cuentas-humo.js (humo: 2 ok, 0 fallas, 0 skip)"
        status: pass
    human_judgment: false
  - id: D4
    description: "El banner del front aparece arriba para un comprador con la cuenta incompleta, persiste al recargar, desaparece al cerrar sesión y no se ve con el admin"
    requirement: "AUTH-05"
    verification:
      - kind: other
        ref: "npm --prefix ../danteautomotores-front run build (compila)"
        status: pass
    human_judgment: true
    rationale: "No hay tests de front ni de navegador; la apariencia y el comportamiento visual se confirman a mano (ver Pendiente de UAT manual)"
  - id: D5
    description: "Las tres dependencias de la fase están en el pom, las coordenadas de libphonenumber están verificadas contra Maven Central y todo resuelve offline"
    verification:
      - kind: other
        ref: "curl maven-metadata.xml lista 9.0.40; mvn -B -o -Djava.version=17 test (336 tests, BUILD SUCCESS)"
        status: pass
    human_judgment: true
    rationale: "La revisión humana de las coordenadas en central.sonatype.com es el checkpoint de legitimidad del plan (T-03-SC)"

duration: 4 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 01: Tracer de cuentas verificadas Summary

**Migración V5 con identidad completa (DNI único, mail sin mayúsculas, tokens_cuenta), regla VerificacionCuenta, registro y login que devuelven los datos faltantes sin PII y un banner en el front, probados de punta a punta.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-10-05T18:44:50Z
- **Completed:** 2026-10-05T18:49:00Z
- **Tasks:** 2 (el tracer y las dependencias, el repositorio y el seed)
- **Files modified:** 17 (6 creados, 11 modificados; 3 de ellos en el repo del front)

## Accomplishments

- V5 aplica sobre una producción simulada sin perder ninguna fila: las cuentas viejas quedan con apellido y dni NULL, `email_confirmado` en false (true para el ADMIN), y la base rechaza un DNI repetido, un DNI como 0123456 y un mail que difiere solo en mayúsculas. El back levanta con `ddl-auto: validate` sobre la entidad ampliada.
- `VerificacionCuenta` es la regla única de cuenta verificada (D-01) y queda lista para las Fases 4 y 5; `AuthService.construirRespuesta` la usa para registro y login.
- El front guarda `faltantes` en la sesión (nunca DNI ni teléfono) y `BannerCuentaIncompleta` lo muestra a un comprador; el admin no lo ve.
- El pom tiene `spring-security-oauth2-jose` y `caffeine` (BOM) y `libphonenumber` 9.0.40 (metadata y jar confirmados en repo1.maven.org); `UsuarioRepository` suma búsquedas por mail sin mayúsculas, DNI y sub de Google; el admin sembrado nace con el mail confirmado.

## Task Commits

1. **Tarea 1 (tracer): V5, entidad, regla, respuesta de auth, banner** - back `e4acf38`, front `117f4e2` (repo `danteautomotores-front`)
2. **Tarea 2: dependencias, repositorio, seed y tests de la regla** - back `ff7684a`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit del back).

## Decisions Made

- La migración es V5 (el research la llama V4, pero V4 ya es `solicitudes_venta`).
- No se toca el contrato de entrada del registro: apellido, teléfono y DNI siguen opcionales hasta 03-09.
- El banner no asume que `faltantes` existe: una sesión anterior a la fase no lo muestra (las cuentas viejas lo ven en el próximo login).

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecución (no son desvíos de código)

- **Compuerta del tracer:** el plan trae un `<human-check>` (UAT en navegador) en el tracer. El orquestador indicó no ejecutarlo; se re-corrió todo lo automatizado (tests de Postgres, humo contra un back local sobre una base vacía y build del front) y pasó. El UAT visual queda listado abajo, no se sintetizó un checkpoint.
- **Commits en dos repos:** el campo `commits: 2` es el conteo medido en el repo del back (`rev-list 4b3a973..HEAD` antes de este SUMMARY). El tercer commit del plan está en el repo del front (`117f4e2`).
- **Lint del front:** `eslint` no corre (no hay `eslint.config.*`, ESLint 10); es una carencia previa del front, fuera del alcance. El build de Vite compila.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificación

- `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test -Dtest=MigracionesPostgresTest,AuthServiceTest`: 11 tests, 0 fallas.
- `mvn ... test -Dtest=VerificacionCuentaTest,DataSeederTest,MigracionesPostgresTest,AuthServiceTest`: 45 tests (10 + 24 + 8 + 3), 0 fallas.
- Suite completa del back: 336 tests, 0 fallas, 0 errores, BUILD SUCCESS.
- Humo (`con-back-local.sh --vacia dante_humo_cuentas node scripts/verify/cuentas-humo.js`): `humo: 2 ok, 0 fallas, 0 skip`; Flyway aplicó las 5 migraciones y el back arrancó con `ddl-auto: validate`.
- `curl` a `maven-metadata.xml` de libphonenumber: lista `9.0.40` (también `latest` y `release`); el jar responde 200.
- `npm --prefix ../danteautomotores-front run build`: compila.

## Pendiente de UAT manual

- **Banner en navegador (human-check del tracer):** levantar un back local (`bash scripts/verify/con-back-local.sh --copia-de danteautomotores dante_uat_fase3 sleep 7200`) y un front de prueba en el puerto 5174 con `VITE_API_URL=http://localhost:8080/api npm --prefix ../danteautomotores-front run dev -- --port 5174` (nunca el `.env` del front, que apunta a producción). Crear una cuenta con el formulario actual y comprobar que arriba aparece el aviso con lo que falta (apellido, teléfono, DNI y confirmar tu mail), que recargar lo mantiene, que cerrar sesión lo hace desaparecer y que con la cuenta admin no aparece.
- **Legitimidad de libphonenumber (human-check de la Tarea 2):** confirmar en central.sonatype.com que `com.googlecode.libphonenumber:libphonenumber` existe, es de Google (github.com/google/libphonenumber) y que 9.0.40 está publicada. Es la única dependencia que no gestiona el BOM.

## Known Stubs

None.

## Threat Flags

None. La tabla `tokens_cuenta` ya estaba en el plan y todavía no tiene código que la use (llega en planes siguientes).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 03-02 puede sumar la validez del teléfono a `VerificacionCuenta` y usar `libphonenumber`; `UsuarioRepository` ya expone `existsByDni`, `existsByDniAndIdNot`, `findByGoogleSub` y las búsquedas sin mayúsculas.
- Antes de desplegar V5 a producción hay que correr la consulta de colisiones de mail por mayúsculas del runbook (plan 03-14): si existieran dos mails que difieren solo en mayúsculas, V5 falla sin dejar nada a medias.

## Self-Check: PASSED

- Archivos creados presentes en disco: V5, DatoFaltante, VerificacionCuenta, VerificacionCuentaTest, cuentas-humo.js y BannerCuentaIncompleta.jsx (FOUND los seis).
- Commits presentes: `e4acf38` y `ff7684a` (back) y `117f4e2` (front).
