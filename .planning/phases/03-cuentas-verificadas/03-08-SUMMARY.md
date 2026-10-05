---
phase: 03-cuentas-verificadas
plan: 08
subsystem: auth
tags: [spring-security, consultas, solicitudes-venta, cuenta-verificada, gate]

requires:
  - phase: 03-cuentas-verificadas
    provides: VerificacionCuenta.exigir, CuentaNoVerificadaException con mapeo 403 (plan 03-02) y UsuarioRepository.findByEmailIgnoreCase (plan 03-01)
provides:
  - POST /api/consultas exige un comprador con cuenta verificada y guarda la consulta con los datos de la cuenta (D-11)
  - POST /api/solicitudes-venta (Vender tu auto) exige un comprador con cuenta verificada (D-01)
  - Patron de gate reutilizable para "Lo quiero" y la mensajeria (Fases 4 y 5)
affects: [03-13, fase-04, fase-05]

plan_head_before: 0a4c7a1c4b93d0a651fd18a92cf3c0b4c456006c
plan_head_after: a82b6b860f6f94d526a69d23134424f8c14cd4f2

actuals:
  tokens: 8900
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Gate de dos capas: SecurityConfig decide el rol (401 sin sesion, 403 admin) y el service llama a VerificacionCuenta.exigir con el estado actual de la base antes de tocar nada mas"
    - "Los datos de contacto salen de la cuenta autenticada, no del cuerpo del pedido"

key-files:
  created:
    - src/test/java/com/danteautomotores/controller/ConsultaSeguridadTest.java
    - src/test/java/com/danteautomotores/controller/SolicitudVentaSeguridadTest.java
    - src/test/java/com/danteautomotores/service/SolicitudVentaServiceTest.java
  modified:
    - src/main/java/com/danteautomotores/dto/consulta/ConsultaRequest.java
    - src/main/java/com/danteautomotores/service/ConsultaService.java
    - src/main/java/com/danteautomotores/controller/ConsultaController.java
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/main/java/com/danteautomotores/service/SolicitudVentaService.java
    - src/main/java/com/danteautomotores/controller/SolicitudVentaController.java
    - src/test/java/com/danteautomotores/service/ConsultaServiceTest.java

key-decisions:
  - "La cuenta se busca y se exige antes de buscar la publicacion: a quien no puede consultar no se le revela si el auto existe"
  - "ConsultaService y SolicitudVentaService usan el estado actual de la base (findByEmailIgnoreCase + exigir); no hay claim de verificacion en el token"
  - "Los tests de servicio usan un @Spy de la VerificacionCuenta real, no un mock: si cambia la regla de cuenta verificada, estos tests lo notan"

patterns-established:
  - "Un endpoint de comprador que exige verificacion: regla hasRole COMPRADOR antes de las reglas ADMIN genericas, controller pasa SecurityUtils.obtenerEmailAutenticado() y el service llama a exigir"

requirements-completed: [AUTH-03, AUTH-06]

coverage:
  - id: D1
    description: "Consultar desde la ficha exige comprador con sesion: sin token 401, admin 403, cuenta incompleta 403 CUENTA_NO_VERIFICADA con faltantes"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/ConsultaSeguridadTest.java"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/ConsultaServiceTest.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "La consulta toma nombre, mail y telefono de la cuenta; un cuerpo viejo con esos campos no rompe y se ignoran"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/ConsultaSeguridadTest.java#unCuerpoViejoConNombreMailYTelefonoNoRompeYEsosValoresSeIgnoran"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/ConsultaServiceTest.java#unaCuentaVerificadaGuardaLaConsultaConLosDatosDeLaCuenta"
        status: pass
    human_judgment: false
  - id: D3
    description: "Un auto vendido sigue rechazado con 'Este auto ya se vendio'; uno reservado o disponible se consulta con cuenta verificada"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/ConsultaServiceTest.java"
        status: pass
    human_judgment: false
  - id: D4
    description: "Crear una solicitud de Vender tu auto exige comprador verificado; el listado y el cambio de estado siguen siendo solo del admin"
    requirement: "AUTH-06"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/controller/SolicitudVentaSeguridadTest.java"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/SolicitudVentaServiceTest.java"
        status: pass
    human_judgment: false
  - id: D5
    description: "El rechazo lo decide el back en cada accion con el estado actual de la base, no con el front ni un claim del token"
    requirement: "AUTH-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/ConsultaServiceTest.java#unaCuentaIncompletaLanzaConLosFaltantesYNoGuarda"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/SolicitudVentaServiceTest.java#unaCuentaIncompletaLanzaConLosFaltantesYNoGuarda"
        status: pass
    human_judgment: false

duration: 8 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 08: Gate de cuenta verificada en consultas y Vender tu auto Summary

**POST /api/consultas y POST /api/solicitudes-venta pasan de publicos a hasRole COMPRADOR con VerificacionCuenta.exigir en el service; la consulta se guarda con nombre, mail y telefono de la cuenta, no del cuerpo.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-10-05T19:21:00Z
- **Completed:** 2026-10-05T19:29:00Z
- **Tasks:** 2
- **Files modified:** 10 (3 creados, 7 modificados)

## Accomplishments

- `SecurityConfig`: las dos reglas POST publicas pasan a `hasRole("COMPRADOR")`, en el mismo lugar (antes de las reglas ADMIN genericas): sin token 401 JSON por el entry point existente y el admin recibe 403. El listado de consultas y el listado/estado de solicitudes siguen siendo del admin (probado).
- `ConsultaService.crear(request, email)`: busca la cuenta (404 si no existe), llama a `exigir` y recien despues busca la publicacion; el rechazo de vendido ("Este auto ya se vendio") y la aceptacion de reservado no cambian. La `Consulta` guarda "nombre apellido", mail y telefono normalizado de la cuenta; la tabla no cambia.
- `ConsultaRequest` queda con `publicacionId` y `mensaje` (`@NotBlank @Size(max = 2000)`). Un cuerpo viejo con nombre, mail y telefono responde 200 y esos valores no llegan al service.
- `SolicitudVentaService.crear(request, email)`: mismo gate; nombreVendedor y telefonoVendedor siguen saliendo del formulario. No hay migracion ni cambio de entidad.

## Task Commits

1. **Tarea 1: la consulta de la ficha exige cuenta verificada y usa los datos de la cuenta** - `54c433a`
2. **Tarea 2: el alta de Vender tu auto tambien exige cuenta verificada** - `a82b6b8`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- La cuenta se exige antes de buscar la publicacion, para no revelar a un no autorizado si el auto existe.
- Los tests de servicio usan un `@Spy` sobre la `VerificacionCuenta` real en lugar de un mock, para que ejerciten la regla verdadera.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Notas de ejecucion

- Un primer intento de editar `SolicitudVentaService` con un script fallo por los finales de linea CRLF; se rehizo con la herramienta de edicion, sin efecto en el resultado. En la Tarea 1 se aplico solo la regla de consultas en `SecurityConfig` y la de solicitudes se dejo para la Tarea 2, para que cada commit sea coherente por si solo.
- El valor del enum de estado de solicitud es `CONTACTADO` (no `CONTACTADA`); se corrigio en el test antes de correrlo.

**Total deviations:** 0 auto-fixed. **Impact:** ninguno.

## Issues Encountered

None.

## Verificacion

- Tarea 1: `mvn -B -o -Djava.version=17 test -Dtest=ConsultaServiceTest,ConsultaSeguridadTest,SeguridadErroresTest,CorsOrigenesTest,TransaccionesServiceTest`: 36 tests (7 + 8 + 15 + 3 + 3), 0 fallas.
- Tarea 2: `... -Dtest=SolicitudVentaServiceTest,SolicitudVentaSeguridadTest,ConsultaSeguridadTest,AdminPublicacionControllerTest,PublicacionControllerTest`: 32 tests, 0 fallas.
- Suite completa del back: 550 tests, 0 fallas, 0 errores, BUILD SUCCESS (con `-o -Djava.version=17`, sin `-Ddante.pg.required=true`).

## Pendiente de UAT manual

None - este plan es solo de back y no tiene verificaciones de navegador.

## Known Stubs

None.

## Threat Flags

None. No se agregan endpoints ni rutas de acceso: se restringen dos existentes (T-03-40 a T-03-45 mitigadas como lo planifica el threat model).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Importante para el front (plan 03-13):** el front actual de la ficha y de `/vender` llama a estos endpoints sin sesion y la consulta manda nombre, mail y telefono sueltos. Con este back, un visitante anonimo recibe 401 y una cuenta incompleta 403 CUENTA_NO_VERIFICADA con `faltantes`. Hasta que 03-13 adapte el front, esos dos formularios no funcionan contra este back. Por eso el back de la Fase 3 no debe publicarse a produccion antes que el front.
- `VerificacionCuenta.exigir` y este patron (regla `hasRole COMPRADOR`, email del token al service, exigir antes de operar) quedan listos para "Lo quiero" y la mensajeria de las Fases 4 y 5.

## Self-Check: PASSED

- Archivos creados presentes en disco: ConsultaSeguridadTest, SolicitudVentaSeguridadTest y SolicitudVentaServiceTest.
- Commits presentes: `54c433a` y `a82b6b8`.
