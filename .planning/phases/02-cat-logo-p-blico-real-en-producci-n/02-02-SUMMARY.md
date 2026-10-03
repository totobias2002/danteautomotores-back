---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 02
subsystem: despliegue-produccion
tags: [spring-security, actuator, cloudinary, railway, node-script, readme]

requires:
  - phase: 02-01
    provides: yml con management.* (health, show-details never), Flyway V1-V3, con-back-local.sh
provides:
  - SecretosGuard que exige las credenciales de Cloudinary fuera de desarrollo (cierra PROD-02)
  - GET /actuator/health y /actuator/health/** publicos (healthcheck de Railway), resto de actuator cerrado
  - DataSeeder con ADMIN_EMAIL normalizado (trim + minusculas)
  - README con migraciones Flyway, prueba local en 5174 y variables de produccion
  - sembrar-demo.js con tipo, zona y ofertas, y guardas contra borrado remoto y duplicados
affects: [02-03, 02-08]

plan_head_before: 29296835971db42549e9f97e9c724a3f04f34e15
plan_head_after: 72f965e8bddf6856892944cfe2b8eee802c65564

actuals:
  tokens: 14500   # chars/4 sobre las lineas agregadas (codigo, tests, README y scripts)
  tasks: 3
  commits: 3      # MEASURED: git rev-list --count plan_head_before..HEAD (antes del commit de metadata)

tech-stack:
  added: []
  patterns:
    - "Guard de arranque unico (SecretosGuard) para toda credencial obligatoria en prod: fallarOAvisar"
    - "Scripts Node que tocan datos validan entorno y host ANTES de cualquier request y usan process.exitCode en vez de exit tras fetch"

key-files:
  created: []
  modified:
    - src/main/java/com/danteautomotores/config/SecretosGuard.java
    - src/main/java/com/danteautomotores/config/DataSeeder.java
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/test/java/com/danteautomotores/config/SecretosGuardTest.java
    - src/test/java/com/danteautomotores/config/DataSeederTest.java
    - src/test/java/com/danteautomotores/security/SeguridadErroresTest.java
    - README.md
    - scripts/demo/sembrar-demo.js
    - scripts/demo/README.md

key-decisions:
  - "Credenciales de Cloudinary validas = presentes y no vacias; no se llama a Cloudinary al arrancar (no acoplar el deploy a un tercero)"
  - "Solo GET /actuator/health y /actuator/health/** son publicos; /actuator/env y cualquier otro metodo siguen protegidos"
  - "La guarda de duplicados del demo compara marca+modelo+anio contra GET /admin/publicaciones y aborta; FORZAR=1 la saltea"

patterns-established:
  - "El demo nunca borra un backend no local sin CONFIRMAR_BORRADO_EN_PRODUCCION=SI, chequeado antes del login"

requirements-completed: [PROD-02, PROD-04]

coverage:
  - id: D1
    description: "Con un perfil que no es de desarrollo el back no arranca si falta CLOUDINARY_CLOUD_NAME, API_KEY o API_SECRET (mensaje con nombres, nunca valores); en dev/sin perfil solo avisa; sin llamadas de red"
    requirement: PROD-02
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/SecretosGuardTest.java (27 casos, 20 metodos)"
        status: pass
    human_judgment: false
  - id: D2
    description: "GET /actuator/health responde 200 {\"status\":\"UP\"} sin token y sin components; /actuator/env sigue dando 401"
    requirement: PROD-04
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/security/SeguridadErroresTest.java (5 tests nuevos)"
        status: pass
      - kind: integration
        ref: "PUERTO_BACK=8081 bash scripts/verify/con-back-local.sh --vacia dante_prueba_health ... curl /actuator/health -> {\"status\":\"UP\"} 200; /actuator/env -> 401"
        status: pass
    human_judgment: false
  - id: D3
    description: "El seed del admin normaliza ADMIN_EMAIL (trim + minusculas) antes de validar, buscar, guardar y loguear; solo espacios cuenta como faltante"
    requirement: PROD-04
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/config/DataSeederTest.java (3 tests nuevos)"
        status: pass
    human_judgment: false
  - id: D4
    description: "El script de demo se niega a LIMPIAR=1 contra un host no local sin confirmacion, antes de cualquier request; aborta si la demo ya esta cargada; completa agencias existentes con PUT"
    requirement: PROD-04
    verification:
      - kind: integration
        ref: "API=https://ejemplo.invalid/api LIMPIAR=1 node scripts/demo/sembrar-demo.js -> exit 1 con CONFIRMAR_BORRADO_EN_PRODUCCION, sin ENOTFOUND (cero trafico de red)"
        status: pass
      - kind: integration
        ref: "corrida local sobre base descartable (back en 8081): login malo -> exit 1; segunda corrida -> aborta listando '#1 Toyota Corolla XEI 2022'; tras borrar el auto, el reintento hace PUT y quedan 7 agencias (sin duplicar)"
        status: pass
    human_judgment: false
  - id: D5
    description: "El script de demo carga zona, tipoCarroceria y precioAnterior en los datos correctos (valores de RESEARCH A5/A6)"
    requirement: PROD-04
    verification:
      - kind: other
        ref: "grep de criterios de aceptacion (6 zonas, 3 precios anteriores, 11 tipos) y node --check"
        status: pass
    human_judgment: true
    rationale: "La carga completa con fotos y los campos nuevos solo se puede ver con la API del plan 02-03 y Cloudinary; se verifica en la carga real del plan 02-08 (H8)"

duration: 21min
completed: 2026-10-03
status: complete
---

# Phase 2 Plan 02: Preparacion de produccion (Cloudinary, healthcheck, demo segura) Summary

**El back exige Cloudinary fuera de desarrollo (junto con JWT y DB), expone `/actuator/health` publico para Railway (200, sin detalles), normaliza el email del admin sembrado, y el script de demo carga tipo/zona/ofertas con guardas contra borrado remoto y duplicados.**

## Performance

- **Duration:** ~21 min
- **Tasks:** 3
- **Files modified:** 9 (0 creados)

## Accomplishments

- `SecretosGuard` suma las tres credenciales de Cloudinary a su validacion (`fallarOAvisar`): prod o cualquier perfil mezclado con prod aborta el arranque con un mensaje que solo nombra variables; sin perfil o con dev/local/test avisa en el log. Sin llamadas de red.
- `SecurityConfig` permite `GET /actuator/health` y `/actuator/health/**` sin token; verificado de punta a punta con el back real (8081, base vacia descartable): `{"status":"UP"}` 200, sin `components`, y `/actuator/env` sigue en 401.
- `DataSeeder` normaliza `ADMIN_EMAIL` una vez (`trim` + `toLowerCase(Locale.ROOT)`) y el log dice con que email ingresar; comprobado tambien contra el back real (el login del script funciona con el email normalizado).
- README: puerto 5433, JDK 21 (y `-Djava.version=17`), secciones "Base de datos y migraciones", "Probar el catalogo en local" (front en 5174) y "Produccion" (variables exactas, healthcheck, Vercel). Cierra IN-07 de la Fase 1.
- `sembrar-demo.js`: guardas de entorno y de host antes del login, login con error claro, zona/tipo/precio anterior, `PUT` de agencias existentes, aborto por duplicados con `FORZAR=1` como salida; `scripts/demo/README.md` documenta todo.
- Suite completa del back: 201 tests, 0 fallas, 0 skipped (`-Ddante.pg.required=true`).

## Task Commits

1. **Task 1 (TDD): Cloudinary en SecretosGuard y email normalizado** - `253088e` (feat)
2. **Task 2: healthcheck publico y README** - `9f4e17b` (feat)
3. **Task 3: demo con tipo/zona/ofertas y guardas** - `72f965e` (feat)

**Plan metadata:** commit docs(02-02) a continuacion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] README decia "JDK 25" para el Dockerfile**
- **Found during:** Task 2
- **Issue:** el README afirmaba un build con JDK 25, pero el `Dockerfile` usa JDK 21.
- **Fix:** corregido a JDK 21 junto con el resto de las correcciones de IN-07.
- **Files modified:** README.md
- **Commit:** 9f4e17b

**2. [Rule 2 - Missing critical] El login fallido del script de demo y el aborto por duplicados usan `process.exitCode` + `return`**
- **Found during:** Task 3
- **Issue:** el plan pedia "salir con 1"; tras un `fetch` previo, `process.exit()` aborta Node en Windows (mismo problema que 02-01, desvio 5).
- **Fix:** esos dos caminos usan `process.exitCode = 1; return;`. Las guardas previas a cualquier request (variables faltantes, LIMPIAR remoto) siguen con `process.exit(1)`, que es seguro sin conexiones pendientes. El codigo de salida sigue siendo 1.
- **Files modified:** scripts/demo/sembrar-demo.js
- **Commit:** 72f965e

**3. [Rule 1 - Bug] El `PUT` de una agencia existente conserva su `logo`**
- **Found during:** Task 3
- **Issue:** `AgenciaRequest` incluye `logo`; un PUT solo con los datos del script lo habria borrado en agencias que el admin ya edito.
- **Fix:** el `PUT` manda `{ ...datosDeAGENCIAS, logo: existente.logo }`.
- **Commit:** 72f965e

### Other notes (not code deviations)

- **Test de healthcheck sin paso RED separado:** los tests del slice y el matcher se escribieron en la misma pasada (el plan marca la tarea 2 sin `tdd="true"`); la prueba de que el matcher es el que hace pasar el caso esta en el humo end-to-end y en que `healthPorPostSigueProtegido` y `/actuator/env` siguen en 401. La tarea 1 (TDD) si paso por RED (26 fallas + 2 errores antes de implementar).
- **Humo del demo contra el back local:** no hay fotos (`scripts/demo/fotos/` no se versiona) ni Cloudinary en el entorno, asi que la corrida falla al leer la primera foto despues de crear el auto; eso se uso a proposito para verificar la guarda de duplicados y el `PUT`. Las zonas se ven como `undefined` en `GET /agencias` porque la API con `zona` es del plan 02-03 (Jackson ignora el campo). La carga completa se verifica en 02-08.
- **Ejecucion secuencial:** el plan preveia correr en paralelo con 02-03 (puerto 8081 para el back de humo); se uso el 8081 igual. Las bases `dante_prueba_health` y `dante_prueba_demo` se borraron al terminar; `danteautomotores` y `dante_uat*` no se tocaron; no quedan back ni puertos 8080/8081 abiertos.

**Total deviations:** 3 auto-fixed (2 Rule 1, 1 Rule 2). **Impact:** ninguno sobre el alcance.

## Known Stubs

None.

## Threat Flags

None. El unico endpoint nuevo expuesto es `GET /actuator/health` (T-02-07, ya mitigado: exposicion limitada a `health`, `show-details: never`, comprobado sin `components`).

## Verification Results

| Check | Resultado |
|-------|-----------|
| `mvn -o test -Dtest=SecretosGuardTest,DataSeederTest,CorsOrigenesTest` | 44 tests, 0 fallas |
| `mvn -o test -Dtest=SeguridadErroresTest,CorsOrigenesTest` | 18 tests, 0 fallas |
| Healthcheck de punta a punta (8081, base vacia) | `{"status":"UP"} 200`, sin `components`; `/actuator/env` 401 |
| Guarda de LIMPIAR contra `https://ejemplo.invalid/api` | exit 1, mensaje con `CONFIRMAR_BORRADO_EN_PRODUCCION`, sin ENOTFOUND |
| Faltan ADMIN_EMAIL/ADMIN_PASSWORD | exit 1 sin red |
| Suite completa (`-Ddante.pg.required=true`) | 201 tests, 0 fallas, 0 skipped |
| Criterios de aceptacion por grep (tareas 1, 2 y 3) | todos PASS (SecretosGuardTest 20 metodos, SeguridadErroresTest 15 tests, matcher health en la linea 60 antes de `anyRequest` en la 61) |

## Next Phase Readiness

Listo para 02-03 (la demo ya envia `tipoCarroceria`, `precioAnterior` y `zona`, que 02-03 agrega a la API) y para el runbook de produccion de 02-08: variables exactas en el README, healthcheck path `/actuator/health`, demo segura. Pendiente para el despliegue real: la pagina `/creditos` del front debe estar publicada antes de cargar la demo en produccion.

## Self-Check: PASSED

- Archivos modificados verificados en disco (SecretosGuard, DataSeeder, SecurityConfig, los tres tests, README, sembrar-demo.js, scripts/demo/README.md).
- Commits verificados: `253088e`, `9f4e17b`, `72f965e`.
