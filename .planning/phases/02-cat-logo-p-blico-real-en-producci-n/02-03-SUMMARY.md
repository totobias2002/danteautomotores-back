---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 03
subsystem: catalogo-publico
tags: [spring-boot, jpa, enums, validation, clock, node-script]

requires:
  - phase: 02-01
    provides: columnas de V3 (tipo_carroceria, precio_anterior, fecha_vendido, agencias.zona), PublicacionResumenResponse, con-back-local.sh, catalogo-humo.js
provides:
  - Enums TipoCarroceria (8 valores) y ZonaAgencia (5 valores)
  - Publicacion.tipoCarroceria/precioAnterior/fechaVendido y Agencia.zona mapeados contra V3 (ddl-auto=validate)
  - PublicacionMapper.esOferta (regla unica de oferta) y oferta/agenciaZona/tipoCarroceria/precioAnterior/fechaVendido en PublicacionResponse y PublicacionResumenResponse
  - PublicacionRequest con tipoCarroceria y precioAnterior validado; AgenciaRequest/Response con zona
  - ClockConfig (bean Clock) y cambiarEstado que registra y borra fechaVendido
affects: [02-04, 02-05, 02-06, 02-07, 02-08]

plan_head_before: c5f5b175acc579e6378f43e2ce48bc99bb005419
plan_head_after: 249c398ae4467a22b66b9c83115033e40885c097

actuals:
  tokens: 5400    # chars/4 sobre las lineas agregadas (21.653 chars: codigo, tests y script)
  tasks: 3
  commits: 3      # MEASURED: git rev-list --count plan_head_before..HEAD (antes del commit de metadata)

tech-stack:
  added: []
  patterns:
    - "Una unica regla de oferta en el mapper (compareTo, precioAnterior > precio) de la que dependen card, detalle y filtro; oferta nunca viaja en un request"
    - "Reloj inyectable (Clock) para reglas dependientes del tiempo; los tests con @InjectMocks lo fijan con ReflectionTestUtils"
    - "fechaVendido solo la toca cambiarEstado: VENDIDO desde otro estado la fija, seguir VENDIDO la conserva, salir de VENDIDO la borra"

key-files:
  created:
    - src/main/java/com/danteautomotores/enums/TipoCarroceria.java
    - src/main/java/com/danteautomotores/enums/ZonaAgencia.java
    - src/main/java/com/danteautomotores/config/ClockConfig.java
    - src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java
  modified:
    - src/main/java/com/danteautomotores/entity/Publicacion.java
    - src/main/java/com/danteautomotores/entity/Agencia.java
    - src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java
    - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResponse.java
    - src/main/java/com/danteautomotores/dto/publicacion/PublicacionResumenResponse.java
    - src/main/java/com/danteautomotores/dto/agencia/AgenciaRequest.java
    - src/main/java/com/danteautomotores/dto/agencia/AgenciaResponse.java
    - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
    - src/main/java/com/danteautomotores/mapper/AgenciaMapper.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/service/AgenciaService.java
    - src/test/java/com/danteautomotores/dto/PublicacionRequestValidationTest.java
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - src/test/java/com/danteautomotores/service/AgenciaServiceTest.java
    - src/test/java/com/danteautomotores/controller/AgenciaControllerTest.java
    - scripts/verify/catalogo-humo.js

key-decisions:
  - "Un precio anterior menor o igual al precio se acepta (no es oferta ni es invalido), segun RESEARCH Open Question 6"
  - "El campo se llama agenciaZona en ambos DTOs de publicacion; el parametro de filtro de la API seguira llamandose zona (plan 02-04)"
  - "Tipos de carroceria y zonas: la lista propuesta por el planner (A5); V3 no restringe valores, asi que sumar uno no requiere migracion"

patterns-established:
  - "PublicacionMapper.esOferta es el unico lugar donde se decide una oferta"

requirements-completed: []
requirements-advanced: [CAT-02, CAT-04]   # datos y contrato listos; filtros (02-04) y card/detalle (02-05) completan los requisitos

coverage:
  - id: D1
    description: "El admin guarda tipo de carroceria y precio anterior opcional; un auto sin tipo queda null; precio anterior 0, negativo o con mas de 10 enteros da 400 con el campo precioAnterior (D-01, T-02-10)"
    requirement: CAT-02
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/dto/PublicacionRequestValidationTest.java (4 tests nuevos)"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java (crear/actualizar con tipo y precio anterior, 3 tests)"
        status: pass
      - kind: integration
        ref: "CatalogoPostgresTest (contexto JPA con ddl-auto=validate contra V3 en Postgres real)"
        status: pass
    human_judgment: false
  - id: D2
    description: "oferta=true si y solo si precioAnterior > precio, calculado en el servidor con una sola regla y expuesto en detalle y resumen; un precio anterior menor o igual no es oferta (D-03, prohibicion CAT-04)"
    requirement: CAT-04
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java (10 tests: null, igual, menor, mayor, escala, sin precio, toResponse, toResumen, zona, fechaVendido)"
        status: pass
      - kind: integration
        ref: "bash scripts/verify/con-back-local.sh --copia-de dante_uat dante_copia_uat03 node scripts/verify/catalogo-humo.js (humo: 7 ok, 0 fallas, 0 skip; verifica oferta booleana coherente con precio y precioAnterior)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Al pasar a VENDIDO se registra fechaVendido con el reloj del servidor; seguir VENDIDO la conserva; salir de VENDIDO la borra; crear y actualizar no la tocan (D-04, T-02-11)"
    requirement: CAT-04
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java (7 tests nuevos con Clock fijo 2026-10-01T12:00Z)"
        status: pass
    human_judgment: false
  - id: D4
    description: "La agencia guarda y expone una zona opcional; sus autos la exponen como agenciaZona; una zona desconocida da 400 con $.error (D-02, T-02-12)"
    requirement: CAT-02
    verification:
      - kind: unit
        ref: "AgenciaServiceTest (3 tests nuevos) y AgenciaControllerTest (2 tests nuevos, incluido zona MARTE -> 400)"
        status: pass
      - kind: integration
        ref: "catalogo-humo.js contra una copia de dante_uat: GET /agencias trae zona en cada agencia; cada destacado trae tipoCarroceria, precioAnterior, oferta, agenciaZona y fechaVendido"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 03: Datos del catalogo en la API (tipo, oferta, zona, fecha de venta) Summary

**El auto guarda tipo de carroceria y precio anterior, la API publica expone `oferta` (regla unica `precioAnterior > precio` en `PublicacionMapper.esOferta`), `agenciaZona` heredada de la agencia y `fechaVendido` fijada por el servidor con un `Clock` inyectable al pasar a VENDIDO.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3 (todas TDD)
- **Files modified:** 16 modificados, 4 creados

## Accomplishments

- Enums `TipoCarroceria` (SEDAN, HATCHBACK, SUV, PICKUP, UTILITARIO, COUPE, MONOVOLUMEN, FAMILIAR) y `ZonaAgencia` (CABA, ZONA_NORTE, ZONA_SUR, ZONA_OESTE, INTERIOR); las entidades calzan con V3 y el contexto arranca con `ddl-auto=validate` en Postgres real.
- `PublicacionRequest`: `tipoCarroceria` opcional y `precioAnterior` con `@Positive` + `@Digits(10,2)` sin `@NotNull`; `oferta` y `fechaVendido` no existen en ningun request (T-02-10, T-02-11).
- `PublicacionMapper.esOferta` con `compareTo` (120.00 == 120), usado por `toResponse` y `toResumen`; ambos exponen tambien `tipoCarroceria`, `precioAnterior`, `agenciaZona` y `fechaVendido`.
- `ClockConfig` + `cambiarEstado`: DISPONIBLE/RESERVADO -> VENDIDO fija `fechaVendido`; VENDIDO -> VENDIDO la conserva; VENDIDO -> otro estado la borra. Crear y PUT no la tocan.
- Agencias con `zona` en request, response, mapper y service (el PUT reemplaza: null la borra); zona desconocida -> 400 uniforme.
- `catalogo-humo.js` suma el chequeo de las cinco claves nuevas en destacados (incluida la coherencia de `oferta` con precio y precio anterior) y de `zona` en `GET /agencias`.

## Task Commits

1. **Task 1 (TDD): tipo de carroceria, precio anterior y regla unica de oferta** - `b46af0e` (feat)
2. **Task 2 (TDD): fecha de venta con Clock inyectable** - `8955d35` (feat)
3. **Task 3 (TDD): zona de la agencia y humo de los campos nuevos** - `249c398` (feat)

**Plan metadata:** commit docs(02-03) a continuacion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Un comentario de PublicacionRequest contenia la palabra "oferta"**
- **Found during:** Task 1 (criterios de aceptacion)
- **Issue:** el criterio `grep -c "oferta" PublicacionRequest.java` debia dar 0 y mi comentario sobre D-03 decia "no es oferta".
- **Fix:** el comentario dice ahora "no hay descuento que mostrar"; commit de la tarea 1 enmendado (mio, sin push).
- **Files modified:** src/main/java/com/danteautomotores/dto/publicacion/PublicacionRequest.java
- **Commit:** b46af0e

**2. [Rule 3 - Blocking] Los tests existentes de `cambiarEstado` habrian lanzado NPE con el `Clock` en null**
- **Found during:** Task 2
- **Issue:** `@InjectMocks` deja el `Clock` en null y `pasarUnAutoDestacadoAVendidoNoLoDesmarca` (existente) pasa a VENDIDO.
- **Fix:** el reloj fijo se instala en el `@BeforeEach` de `PublicacionServiceTest` (no solo en los tests nuevos), con `ReflectionTestUtils.setField`.
- **Files modified:** src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
- **Commit:** 8955d35

### Other notes (not code deviations)

- **Pasos RED:** la tarea 2 paso por RED real (error de compilacion por `setFechaVendido`/`getFechaVendido` inexistentes antes de implementar). En las tareas 1 y 3 los tests y el codigo se escribieron en la misma pasada y no hubo corrida RED separada; las reglas se comprobaron igual (el caso de la prohibicion "no marcar oferta sin descuento real" esta cubierto por tres tests del mapper).
- **Humo sobre la copia de `danteautomotores`:** la base de desarrollo tiene 1 auto sin destacados, asi que los chequeos por item se saltean (3 ok, 0 fallas, 4 skip, como preve el plan). Para ejercitarlos se corrio tambien sobre una copia de `dante_uat` (11 autos, 6 destacados): 7 ok, 0 fallas, 0 skip. Esos autos no tienen precio anterior, asi que la oferta de punta a punta (`oferta=true`) solo esta cubierta por tests unitarios; la carga real de ofertas se ve en 02-08 con la demo.
- **Ejecucion secuencial:** no corrio en paralelo con 02-02, asi que se uso el puerto 8080 sin reintentos de `mvn`. Las bases `dante_copia_datos` y `dante_copia_uat03` se borraron al terminar; `danteautomotores` y `dante_uat*` no se tocaron; no quedan bases `test_*` ni puertos 8080/8081 abiertos.
- **Alcance del test de zona desconocida:** el 400 de `zona: "MARTE"` lo resuelve el handler base de `ResponseEntityExceptionHandler` (mensaje no legible); el test comprueba status 400 y presencia de `$.error`.

- **Requisitos no marcados como completos:** `requirements mark-complete CAT-02 CAT-04` los marco, pero este plan solo aporta los datos y el contrato (filtros en 02-04, card y detalle en 02-05); se revirtio el cambio en REQUIREMENTS.md para no declarar completo lo que aun no se ve en el sitio.

**Total deviations:** 2 auto-fixed (1 Rule 1, 1 Rule 3). **Impact:** ninguno sobre el alcance.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints ni superficie nueva fuera del threat_model: solo campos nuevos en contratos existentes (T-02-10, T-02-11, T-02-12 mitigados y cubiertos por tests).

## Verification Results

| Check | Resultado |
|-------|-----------|
| `mvn -o test -Dtest=PublicacionRequestValidationTest,PublicacionMapperTest,PublicacionServiceTest,CatalogoPostgresTest -Ddante.pg.required=true` (tarea 1) | 74 tests, 0 fallas, 0 skipped |
| Idem tarea 2 (PublicacionServiceTest, PublicacionMapperTest, CatalogoPostgresTest) | 69 tests, 0 fallas, 0 skipped |
| `mvn -o test -Dtest=AgenciaServiceTest,AgenciaControllerTest` | 18 tests, 0 fallas |
| Humo sobre copia de `danteautomotores` | humo: 3 ok, 0 fallas, 4 skip |
| Humo sobre copia de `dante_uat` | humo: 7 ok, 0 fallas, 0 skip |
| Suite completa (`-Ddante.pg.required=true`) | 230 tests, 0 fallas, 0 skipped |
| Criterios de aceptacion por grep (esOferta 1 definicion y 2 usos, precio_anterior, ZonaAgencia, agenciaZona en ambos DTOs, 0 "oferta" y 0 "fechaVendido" en PublicacionRequest, LocalDateTime.now(clock) 1, Clock.systemDefaultZone() 1, 8 usos de cambiarEstado en el test) | todos PASS |

## Next Phase Readiness

Listo para 02-04 (filtros por tipo, zona y oferta: la API ya expone los campos y la regla de oferta es `PublicacionMapper.esOferta`; el filtro por oferta en base de datos debe replicar `precio_anterior > precio`) y para 02-05 (el panel y la card pueden leer `tipoCarroceria`, `precioAnterior`, `oferta`, `agenciaZona` y `zona` de agencias). Para 02-07/02-08: la visibilidad de vendidos por 30 dias usa `fechaVendido` (backfill de V3 para los ya vendidos).

## Self-Check: PASSED

- Archivos creados verificados en disco (TipoCarroceria, ZonaAgencia, ClockConfig, PublicacionMapperTest).
- Commits verificados: `b46af0e`, `8955d35`, `249c398`.
