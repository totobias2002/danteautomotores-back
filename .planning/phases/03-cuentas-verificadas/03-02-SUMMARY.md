---
phase: 03-cuentas-verificadas
plan: 02
subsystem: auth
tags: [libphonenumber, validacion, errores, spring-advice, ley-25326]

requires:
  - phase: 03-cuentas-verificadas
    provides: VerificacionCuenta, DatoFaltante, libphonenumber 9.0.40 en el pom (plan 03-01)
provides:
  - NormalizadorDeContacto (celular argentino a +549 mas diez digitos, DNI sin separadores), puro y sin Spring
  - VerificacionCuenta con TELEFONO como faltante si el telefono no es un celular valido, y exigir(Usuario)
  - CuentaNoVerificadaException (403 con codigo CUENTA_NO_VERIFICADA y faltantes) y LimiteDeIntentosException (429 con Retry-After)
  - Log de integridad sin datos personales (solo el nombre de la restriccion)
affects: [03-05, 03-08, 03-09, 03-10, fase-04, fase-05]

plan_head_before: d0fdac7c1eac729275523d08970918c3f1aa8cd5
plan_head_after: b009a43b4ba16cdc91da7a497d68a1704cf33de1

actuals:
  tokens: 6350
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Normalizadores de contacto estaticos y puros; los mensajes de rechazo son constantes publicas reutilizables por tests y front"
    - "Errores nuevos aditivos al formato uniforme: los campos extra (codigo, faltantes) no rompen a los clientes que solo leen error"
    - "El log de excepciones de base de datos nunca incluye el mensaje de la causa: solo restriccion y clase"

key-files:
  created:
    - src/main/java/com/danteautomotores/service/identidad/NormalizadorDeContacto.java
    - src/main/java/com/danteautomotores/exception/CuentaNoVerificadaException.java
    - src/main/java/com/danteautomotores/exception/LimiteDeIntentosException.java
    - src/test/java/com/danteautomotores/service/identidad/NormalizadorDeContactoTest.java
  modified:
    - src/main/java/com/danteautomotores/service/VerificacionCuenta.java
    - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java
    - src/test/java/com/danteautomotores/service/VerificacionCuentaTest.java
    - src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java

key-decisions:
  - "Se acepta cualquier numero argentino valido para libphonenumber y se guarda como celular (+549...): no distingue fijo de celular sin el 15 o el 9; D-03 pide solo codigo de area mas numero"
  - "El nombre de la restriccion se busca recorriendo la cadena de causas hasta org.hibernate.exception.ConstraintViolationException; si no esta, se loguea 'desconocida'"

patterns-established:
  - "VerificacionCuenta.exigir(Usuario) es el punto unico para cortar una accion de cuenta no verificada (lo usan 03-08, 03-10 y las Fases 4 y 5)"

requirements-completed: [AUTH-01, AUTH-03]

coverage:
  - id: D1
    description: "El telefono se valida como celular argentino y se guarda como +549 mas 10 digitos; otro pais, sin codigo de area, incompleto o vacio se rechaza con mensaje claro"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/identidad/NormalizadorDeContactoTest.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "El DNI se acepta con puntos, espacios o guiones y se guarda sin ellos (7 u 8 digitos, sin cero inicial); todo lo demas se rechaza"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/identidad/NormalizadorDeContactoTest.java"
        status: pass
    human_judgment: false
  - id: D3
    description: "Una cuenta con telefono viejo de texto libre invalido cuenta como faltante de TELEFONO; exigir lanza con los faltantes y no hace nada con una cuenta verificada"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/VerificacionCuentaTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "403 CUENTA_NO_VERIFICADA con error, codigo y faltantes; 429 con Retry-After y mensaje en espanol, en el formato de error uniforme"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "El log de una violacion de integridad contiene el nombre de la restriccion y no el DNI ni la fila"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java#elLogDeIntegridadSoloTraeLaRestriccionYNuncaElDni"
        status: pass
    human_judgment: false

duration: 5 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 02: Normalizacion de contacto y errores uniformes Summary

**Celular argentino normalizado a +549 mas diez digitos con libphonenumber, DNI sin separadores, 403 CUENTA_NO_VERIFICADA con faltantes, 429 con Retry-After y log de integridad que ya no vuelca el DNI.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-10-05T18:48:00Z
- **Completed:** 2026-10-05T18:53:00Z
- **Tasks:** 2
- **Files modified:** 8 (4 creados, 4 modificados)

## Accomplishments

- `NormalizadorDeContacto` cubre la tabla de casos completa (35 casos parametrizados): los nueve formatos de celular validos dan el mismo +549 mas diez digitos, y otro pais, sin codigo de area, un digito de menos, texto, vacio, espacios y null se rechazan con el mensaje del plan. El DNI acepta puntos, espacios y guiones.
- `VerificacionCuenta` suma la validez del telefono (una cuenta vieja con telefono de formato libre lo vuelve a cargar, D-09) y el metodo `exigir`, que lanza `CuentaNoVerificadaException` con los faltantes.
- El advice responde 403 con `{error, codigo: CUENTA_NO_VERIFICADA, faltantes}` y 429 con `Retry-After` en segundos (60 por defecto), sin tocar los handlers existentes.
- `handleDataIntegrity` ya no loguea el mensaje de la causa (en Postgres trae `Key (dni)=(...)` y la fila): loguea la restriccion y la clase de la excepcion. El cuerpo 409 no cambia.

## Task Commits

1. **Tarea 1: normalizar celular argentino y DNI y validar el telefono en la regla** - `217edbc`
2. **Tarea 2: errores uniformes 403 / 429 y log de integridad sin datos personales** - `b009a43`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- Se acepta cualquier numero argentino valido y se guarda como celular (limitacion documentada en el comentario de la clase): libphonenumber no distingue fijo de celular cuando no se escribe el 15 o el 9.
- Si la causa no es un `ConstraintViolationException` de Hibernate con nombre, el log dice `restriccion=desconocida`.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecucion

- **TDD de la Tarea 1:** el plan marca `tdd="true"`. Los tests y la implementacion se escribieron en la misma pasada y se commitearon juntos: no hubo un commit RED aparte (la clase no existia, el test no compilaba), por eso tampoco se observo una falla previa de la tabla. Los 35 casos pasan contra la implementacion final.
- **Log de integridad:** el test se escribio contra la nueva conducta y no se corrio contra el codigo viejo; el codigo viejo logueaba `getMostSpecificCause().getMessage()`, que contiene los digitos del DNI del caso.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificacion

- `mvn -B -o -Djava.version=17 test -Dtest=NormalizadorDeContactoTest,VerificacionCuentaTest`: 47 tests (35 + 12), 0 fallas, antes de la Tarea 2.
- `mvn ... test -Dtest=GlobalExceptionHandlerTest,VerificacionCuentaTest,SeguridadErroresTest`: 50 tests (21 + 14 + 15), 0 fallas.
- Suite completa del back: 379 tests, 0 fallas, 0 errores, BUILD SUCCESS.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador.

## Known Stubs

None.

## Threat Flags

None. Los errores nuevos no agregan endpoints ni rutas de acceso; solo existen como excepciones que los planes siguientes lanzan.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 03-05, 03-08, 03-09 y 03-10 ya pueden usar `NormalizadorDeContacto.normalizarCelular/normalizarDni`, `VerificacionCuenta.exigir` y las dos excepciones.
- Nadie lanza todavia `CuentaNoVerificadaException` ni `LimiteDeIntentosException`: el mapeo esta probado con un service mockeado.

## Self-Check: PASSED

- Archivos creados presentes en disco: NormalizadorDeContacto, CuentaNoVerificadaException, LimiteDeIntentosException y NormalizadorDeContactoTest.
- Commits presentes: `217edbc` y `b009a43`.
