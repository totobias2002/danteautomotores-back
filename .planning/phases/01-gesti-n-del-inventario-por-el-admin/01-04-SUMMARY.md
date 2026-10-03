---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 04
subsystem: api
tags: [spring-boot, exception-handling, webmvctest, tdd, error-contract]
status: complete

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "Plan 01-01: SeguridadWebMvcTestBase, RestAuthenticationEntryPoint/RestAccessDeniedHandler y utils/errores.js mensajeDeError"
provides:
  - "ServicioExternoException (RuntimeException) mapeada a 502 con su mensaje; la usa 01-06 para envolver errores de Cloudinary"
  - "GlobalExceptionHandler unico (@RestControllerAdvice) que extiende ResponseEntityExceptionHandler"
  - "Contrato de error de la API: {error} y en validacion {error: 'Datos invalidos', campos: {campo: mensaje}}; codigos 400/401/403/404/405/409/413/415/500/502"
  - "GlobalExceptionHandlerTest: 15 tests de contrato del formato de errores"
affects: [01-05, 01-06, 01-07]

requirements-completed: [PROD-01]

actuals:
  tokens: 9000
  tasks: 2
  commits: 2
plan_head_before: e7320c32610efc7207b042069a8d8e4a53e8de97
plan_head_after: b64618a7eb755202826c702e72eade378d077f76

tech-stack:
  added: []
  patterns:
    - "Advice unico que extiende ResponseEntityExceptionHandler: solo overrides (handleMethodArgumentNotValid, handleExceptionInternal) para excepciones de Spring MVC; @ExceptionHandler propios solo para excepciones de dominio, seguridad y genericas"
    - "Mensajes fijos por status en handleExceptionInternal: nunca se filtra el mensaje de la excepcion de framework"

key-files:
  created:
    - src/main/java/com/danteautomotores/exception/ServicioExternoException.java
    - src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java
  modified:
    - src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java

key-decisions:
  - "El formato de validacion pasa del mapa plano {campo: msg} a {error: 'Datos invalidos', campos: {...}}; ningun consumidor leia el mapa plano"
  - "No hay @ExceptionHandler propio para MethodArgumentNotValidException ni MaxUploadSizeExceededException: los resuelve la base (un handler propio deja un mapeo ambiguo)"
  - "DataIntegrityViolationException responde 409 con mensaje fijo; el detalle (constraint/SQL) solo va al log en warn"
  - "El 500 generico y el 502 registran la excepcion completa con log.error; el cuerpo nunca lleva stacktrace, clase ni mensaje interno"

patterns-established:
  - "Contrato de errores de la API: todo cuerpo de error lleva la clave 'error' en espanol"
  - "Tests @WebMvcTest sobre dos controllers con @MockBean de sus services y SeguridadWebMvcTestBase"

coverage:
  deliverables:
    - name: "Formato uniforme {error} para errores de negocio, framework y genericos"
      verification:
        - kind: test
          ref: "src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java"
          status: pass
      human_judgment: false
    - name: "Validacion con {error, campos}"
      verification:
        - kind: test
          ref: "src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java#validacionDevuelveErrorYMapaDeCampos"
          status: pass
      human_judgment: false
    - name: "500 sin filtrar detalles internos; 403 desde service; 401 en login; 409 y 502"
      verification:
        - kind: test
          ref: "src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java"
          status: pass
      human_judgment: false
---

# Phase 1 Plan 04: Formato uniforme de errores Summary

**Un unico `@RestControllerAdvice` que extiende `ResponseEntityExceptionHandler` y responde `{"error": "..."}` en espanol (con `campos` en validaciones) para errores de negocio, de Spring MVC y genericos, sin filtrar stacktraces ni mensajes internos.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-10-03T02:27Z
- **Completed:** 2026-10-03T02:35Z
- **Tasks:** 2 (RED + GREEN)
- **Files:** 3 (2 creados, 1 modificado)

## Accomplishments

- RED: `GlobalExceptionHandlerTest` con 15 casos de contrato; contra el handler viejo fallaron 12 (9 aserciones, 3 errores por excepciones sin handler).
- GREEN: `GlobalExceptionHandler` reescrito; los 15 casos pasan junto con `SeguridadErroresTest` y `AdminPublicacionControllerTest`. Suite completa del back: 58 tests verdes (43 previos + 15).
- `ServicioExternoException` queda lista para que 01-06 envuelva los errores de Cloudinary (502 con mensaje apto para el usuario).
- Mapeos: `ResourceNotFoundException` 404, `IllegalArgumentException` 400, `AuthenticationException` 401, `AccessDeniedException` 403, `DataIntegrityViolationException` 409, `ServicioExternoException` 502, `Exception` 500; y por la base: 400 (JSON mal formado, enum/tipo invalido, parte multipart faltante), 404 de ruta, 405, 413, 415.

## Task Commits

1. **Tarea 1 (RED): test de contrato y ServicioExternoException** - `08d0dd4` (test)
2. **Tarea 2 (GREEN): GlobalExceptionHandler unico** - `b64618a` (feat)

## Deviations from Plan

None - plan executed exactly as written.

Notas de ejecucion (no son desvios): al compilar el GREEN aparecieron clases JDK 21 viejas en `target/` (el IDE compila en paralelo, "class file version 65.0"); se resolvio con `rm -rf target/classes target/test-classes` y se reintento, segun la nota del orquestador.

## Threat Model

- T-01-15 (500 sin filtrar): mitigado, test con "detalle interno secreto" y "RuntimeException".
- T-01-16 (excepciones de framework e integridad): mitigado, mensajes fijos por status; tests sobre "For input string" y "fk_consulta".
- T-01-17 (AccessDenied como 500): mitigado, handler explicito 403 y test dedicado.

## Known Stubs

None.

## Threat Flags

None.

## Issues Encountered

None.

## Next Phase Readiness

Listo para 01-05. El front ya lee `data.error` y `data.campos` con `mensajeDeError` (01-01); 01-06 debe lanzar `ServicioExternoException` desde el flujo de Cloudinary para obtener el 502.

## Self-Check: PASSED

- ServicioExternoException.java, GlobalExceptionHandlerTest.java, GlobalExceptionHandler.java: FOUND
- Commits 08d0dd4 y b64618a: FOUND
- Criterios de aceptacion: `extends ResponseEntityExceptionHandler` = 1; handlers explicitos de MaxUpload/MethodArgumentNotValid = 0; un unico `@RestControllerAdvice`; `BAD_GATEWAY` = 1; `@Test` = 15; `extends RuntimeException` = 1
- `mvn test` completo: 58 tests, 0 fallas
