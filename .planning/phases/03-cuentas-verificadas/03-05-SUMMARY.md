---
phase: 03-cuentas-verificadas
plan: 05
subsystem: auth
tags: [perfil, dni, telefono, idor, mass-assignment, ley-25326, spring-mvc]

requires:
  - phase: 03-cuentas-verificadas
    provides: NormalizadorDeContacto, VerificacionCuenta y el log de integridad sin PII (plan 03-02); Usuario ampliada, UsuarioRepository con existsByDniAndIdNot y el humo cuentas-humo.js (plan 03-01)
provides:
  - GET y PUT /api/usuarios/me (perfil propio; sin id, la cuenta sale del token)
  - UsuarioService.obtenerPerfil y actualizarPerfil con DNI unico e inmutable y telefono normalizado
  - UsuarioResponse (unico DTO con DNI y telefono) y ActualizarPerfilRequest (sin mail ni rol)
  - Humo ampliado con el flujo de completar datos contra Postgres real
affects: [03-10, 03-11, 03-13, fase-04, fase-05]

plan_head_before: bfd1b29b78675530c912414cca58e2f100de7a3c
plan_head_after: a14731cea0f7059602eead67500b90a078c38bfa

actuals:
  tokens: 10500
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Endpoints de cuenta propia sin id en la ruta: el mail sale de SecurityUtils.obtenerEmailAutenticado()"
    - "Validar todo antes de mutar la entidad; el UNIQUE de la base resuelve la carrera y el service traduce la violacion al mensaje de negocio"
    - "DTOs con datos personales usan @ToString.Exclude en cada campo sensible"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/usuario/UsuarioResponse.java
    - src/main/java/com/danteautomotores/dto/usuario/ActualizarPerfilRequest.java
    - src/main/java/com/danteautomotores/mapper/UsuarioMapper.java
    - src/main/java/com/danteautomotores/service/UsuarioService.java
    - src/main/java/com/danteautomotores/controller/UsuarioController.java
    - src/test/java/com/danteautomotores/service/UsuarioServiceTest.java
    - src/test/java/com/danteautomotores/controller/UsuarioControllerTest.java
  modified:
    - scripts/verify/cuentas-humo.js

key-decisions:
  - "Un DNI en blanco en el pedido de una cuenta que ya tiene DNI se trata como 'no lo mando' y no cambia nada; solo un valor distinto da 400"
  - "El UNIQUE de la base se reconoce por el nombre uk_usuarios_dni en la cadena de causas; cualquier otra violacion de integridad se relanza sin traducir"
  - "La cuenta inexistente responde ResourceNotFoundException con 'No existe la cuenta.' sin repetir el mail"

patterns-established:
  - "03-10 suma POST /me/contrasena y /me/reenviar-confirmacion al mismo UsuarioController y UsuarioService"

requirements-completed: [AUTH-03, AUTH-05]

coverage:
  - id: D1
    description: "GET /api/usuarios/me devuelve el perfil del propio usuario con faltantes, cuentaVerificada, tieneContrasena y tieneGoogle, sin recibir ningun id"
    requirement: "AUTH-05"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/UsuarioControllerTest.java#getDevuelveElPerfilDelDuenoYElServiceSeInvocaConElMailDelToken"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java#obtenerPerfilDevuelveLaCuentaConFaltantesYBanderas"
        status: pass
    human_judgment: false
  - id: D2
    description: "PUT /api/usuarios/me guarda nombre, apellido, telefono normalizado y DNI normalizado; la cuenta queda solo con EMAIL_SIN_CONFIRMAR"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java#completaNombreApellidoTelefonoNormalizadoYDniNormalizado"
        status: pass
      - kind: e2e
        ref: "bash scripts/verify/con-back-local.sh --vacia dante_humo_perfil node scripts/verify/cuentas-humo.js (humo: 8 ok, 0 fallas, 0 skip)"
        status: pass
    human_judgment: false
  - id: D3
    description: "El DNI es inmutable una vez cargado (el mismo con puntos se acepta, uno distinto da 400) y obligatorio para compradores pero no para el admin"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java"
        status: pass
      - kind: e2e
        ref: "cuentas-humo.js: la primera cuenta con otro DNI da 400 'no se puede modificar' y el DNI no cambia"
        status: pass
    human_judgment: false
  - id: D4
    description: "Un DNI de otra cuenta se rechaza con el mensaje de D-04 sin revelar el mail ni el DNI, tambien cuando lo decide el UNIQUE de la base"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java#siElUniqueDeLaBaseGanaLaCarreraSeTraduceAlMismoMensaje"
        status: pass
      - kind: e2e
        ref: "cuentas-humo.js: DNI de otra cuenta da 400 sin el mail de la primera ni el DNI en el error"
        status: pass
    human_judgment: false
  - id: D5
    description: "Sin IDOR ni mass assignment: el mail sale del token, el request no declara mail ni rol y un body con campos de mas no rompe"
    requirement: "AUTH-05"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/UsuarioControllerTest.java#putConEmailYRolDeMasNoRompeYNoLlegaAlServiceComoCampos"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java#actualizarPerfilRequestNoDeclaraCampoDeEmailNiDeRol"
        status: pass
    human_judgment: false
  - id: D6
    description: "El telefono y el DNI no aparecen en el toString del request ni de la respuesta ni en el log del back durante el humo"
    requirement: "AUTH-05"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/UsuarioServiceTest.java#elToStringDelRequestNoContieneElTelefonoNiElDni"
        status: pass
      - kind: other
        ref: "grep del log del back del humo por el DNI y los mails de prueba: 0 coincidencias"
        status: pass
    human_judgment: false

duration: 4 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 05: Perfil propio (GET y PUT /api/usuarios/me) Summary

**API del perfil propio sin id en la ruta: completar o editar nombre, apellido, telefono (+549 normalizado) y DNI (unico, inmutable una vez cargado, mensaje de D-04 sin revelar de quien es), verificada de punta a punta contra Postgres real.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-10-05T19:07:19Z
- **Completed:** 2026-10-05T19:11:00Z
- **Tasks:** 2
- **Files modified:** 8 (7 creados, 1 modificado)

## Accomplishments

- `UsuarioService.actualizarPerfil` valida todo antes de mutar la cuenta: telefono normalizado, y para el DNI cuatro ramas (cuenta con DNI: el mismo con puntos se acepta, uno distinto da "no se puede modificar"; sin DNI: comprador obligado, admin exento, y duplicado con `existsByDniAndIdNot`). Si dos pedidos simultaneos empatan el chequeo previo, el UNIQUE `uk_usuarios_dni` gana y la `DataIntegrityViolationException` se traduce al mismo mensaje.
- `UsuarioController` expone `GET` y `PUT /api/usuarios/me` sin parametro de id: el mail sale de `SecurityUtils`, asi que no hay forma de pedir el perfil de otro. `SecurityConfig` no se toco (`anyRequest().authenticated()`).
- `UsuarioResponse` es el unico DTO con DNI y telefono y los excluye de su `toString`; `ActualizarPerfilRequest` no declara mail ni rol y tambien los excluye.
- El humo suma 6 chequeos al camino del tracer: 401 sin token, perfil de cuenta nueva con 4 faltantes y telefono nulo, completar datos (telefono `+5491112345678`, DNI `30123456`, solo queda `EMAIL_SIN_CONFIRMAR`), relectura, DNI duplicado de una segunda cuenta sin el mail de la primera, y cambio de DNI rechazado.

## Task Commits

1. **Tarea 1: perfil propio con las reglas de DNI y telefono (DTOs, mapper y service)** - `124549e`
2. **Tarea 2: endpoints GET y PUT /api/usuarios/me y el humo de completar datos** - `a14731c`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- Un DNI en blanco en el pedido de una cuenta que ya tiene DNI se interpreta como "no lo mando" y no cambia nada; solo un valor distinto es un 400. Es lo que permite que el front de perfil no reenvie el DNI.
- Solo la restriccion `uk_usuarios_dni` se traduce al mensaje de D-04; cualquier otra violacion de integridad se relanza para que el advice la responda como 409 sin PII.
- Un admin que se llama a este endpoint sin DNI guarda igual (no queda obligado), pero el telefono, nombre y apellido si son obligatorios por la validacion del request.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecucion

- **TDD de la Tarea 1:** el plan marca `tdd="true"`. Igual que en 03-02, los tests y la implementacion se escribieron en la misma pasada y se commitearon juntos: no hubo commit RED aparte, porque `UsuarioService` no existia y el test no compilaba. Los 17 tests pasan contra la implementacion final.
- **Tests de la Tarea 2 no corridos contra una version rota:** `UsuarioControllerTest` se escribio junto al controlador; se verifico solo en verde.
- **Orden de los commits:** el controlador y su test quedaron en el commit de la Tarea 2, como pide el plan.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificacion

- `mvn -B -o -Djava.version=17 test -Dtest=UsuarioServiceTest,NormalizadorDeContactoTest,VerificacionCuentaTest`: 66 tests (17 + 35 + 14), 0 fallas.
- `mvn ... test -Dtest=UsuarioControllerTest,UsuarioServiceTest,SeguridadErroresTest`: 39 tests (7 + 17 + 15), 0 fallas.
- Humo (`bash scripts/verify/con-back-local.sh --vacia dante_humo_perfil node scripts/verify/cuentas-humo.js`): `humo: 8 ok, 0 fallas, 0 skip`; la base temporal se borro al terminar y el log del back no contiene el DNI ni los mails de prueba.
- Suite completa del back (`-Ddante.pg.required=true`): 476 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador (las pantallas de "Completa tus datos" y del perfil llegan en 03-11 y 03-13).

## Known Stubs

None.

## Threat Flags

None. El unico endpoint nuevo (`/api/usuarios/me`) esta cubierto por T-03-20 a T-03-25 del plan y no agrega otra superficie.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 03-10 puede sumar `POST /api/usuarios/me/contrasena` y `/me/reenviar-confirmacion` a `UsuarioController` y `UsuarioService`.
- 03-11 y 03-13 (front) consumen `GET` y `PUT /api/usuarios/me` con el contrato `UsuarioResponse {id, nombre, apellido, email, telefono, dni, rol, emailConfirmado, tieneContrasena, tieneGoogle, cuentaVerificada, faltantes}`.
- El back sigue sin poder pushearse a produccion: el `SecretosGuard` de 03-04 exige las variables de Brevo y Google que todavia no existen alla.

## Self-Check: PASSED

- Archivos creados presentes en disco: UsuarioResponse, ActualizarPerfilRequest, UsuarioMapper, UsuarioService, UsuarioController, UsuarioServiceTest y UsuarioControllerTest (FOUND los siete).
- Commits presentes: `124549e` y `a14731c`.
