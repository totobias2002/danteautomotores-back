---
phase: 02-cat-logo-p-blico-real-en-producci-n
plan: 08
subsystem: produccion-railway-vercel
tags: [railway, vercel, flyway, postgres, cloudinary, deploy, demo]

requires:
  - phase: 02-01
    provides: migraciones Flyway V1 (baseline), V2 y V3, perfil prod
  - phase: 02-02
    provides: scripts de demo (bajar-fotos, sembrar-demo), healthcheck /actuator/health, SecretosGuard
  - phase: 02-07
    provides: front sin mocks, humo completo (solo local)
provides:
  - Back desplegado en Railway con perfil prod (deploy activo c173751c) y base de produccion migrada por Flyway (baseline V1, migraciones hasta V4)
  - Front desplegado en Vercel (https://danteautomotores-front.vercel.app) contra la API productiva
  - Demo cargada en produccion con el script contra la API (11 autos, 3 ofertas, 6 destacados, 9 marcas)
  - Backup logico previo al primer arranque con Flyway, fuera de los repos
affects: [fase-3]

plan_head_before: f93454ab6941aee012e490ed18196ec379cf17fb
plan_head_after: f93454ab6941aee012e490ed18196ec379cf17fb

actuals:
  tokens: 0       # el plan no modifica codigo (files_modified vacio); el SUMMARY no cuenta como diff de codigo
  tasks: 3
  commits: 0      # MEASURED: el plan no tuvo commits de codigo (docs-only); no hay ledger de este plan, base y fin son el HEAD al escribir el SUMMARY

tech-stack:
  added: []
  patterns:
    - "Primer arranque con Flyway sobre una base existente: baseline en V1 y migraciones aditivas encima"
    - "Verificacion de produccion solo con GET/OPTIONS anonimos de solo lectura"

key-files:
  created: []
  modified: []

key-decisions:
  - "La migracion se hizo con baseline en V1 sobre la base de produccion existente (backup logico previo, fuera de los repos)"
  - "El seed de demo se resolvio asignando a la API key de Cloudinary de produccion un rol con permisos de escritura (Master admin), de forma provisoria hasta rotarla por una de rol acotado"

requirements-completed: [PROD-02, PROD-04]

coverage:
  - id: D1
    description: "Backup logico de la base de produccion fuera de los repos antes del primer arranque con Flyway (PROD-04, T-02-28, T-02-29)"
    requirement: PROD-04
    verification:
      - kind: manual
        ref: "Tarea 1: el archivo existe en C:/Users/toto/Desktop/backups-dante/ (dante-prod-20261004-0005.dump) y ultimo-backup.txt lo referencia; version de Postgres de produccion 18.6 (log de Flyway)"
        status: pass
    human_judgment: true
    rationale: "Conteos por tabla, listado con pg_restore --list y coincidencia del esquema con V1: no registrado en esta sesion"
  - id: D2
    description: "Ensayo de la migracion con una copia local del backup (PROD-04, T-02-28)"
    requirement: PROD-04
    verification:
      - kind: manual
        ref: "Ensayo local con copia: no registrado en esta sesion. En el deploy real Flyway hizo baseline en V1 y aplico las migraciones sobre la base de produccion sin errores de validacion"
        status: unknown
    human_judgment: true
    rationale: "No hay registro de que el ensayo local de la tarea 2 se haya corrido; la evidencia disponible es el resultado del deploy real"
  - id: D3
    description: "Back en Railway con perfil prod, base migrada y sin SQL en el log; front en Vercel contra la API productiva (PROD-04, PROD-02)"
    requirement: PROD-04
    verification:
      - kind: integration
        ref: "GET /actuator/health 200 {status UP}; GET /api/publicaciones?pagina=1 con tamanio 24; GET /api/publicaciones/destacados devuelve array JSON; preflight OPTIONS con el Origin del front devuelve access-control-allow-origin igual al dominio de Vercel; front / 200 y /creditos 200 (verificacion de solo lectura, 2026-10-05)"
        status: pass
      - kind: manual
        ref: "Logs de arranque en Railway: baseline V1, migraciones hasta V4 (V4 solicitudes_venta ya existia: skipping), Started DanteAutomotoresApplication, sin lineas de SQL"
        status: pass
    human_judgment: false
  - id: D4
    description: "Produccion arranca con la demo cargada por el script contra la API (D-09) y /creditos publicada antes (D-10)"
    requirement: PROD-04
    verification:
      - kind: integration
        ref: "Lecturas anonimas del 2026-10-05: 11 autos visibles, ofertas=true devuelve totalElementos 3, 9 marcas en facetas, 6 destacados, 0 autos sin foto portada, estados 9 DISPONIBLE / 1 RESERVADO / 1 VENDIDO; /creditos 200"
        status: pass
    human_judgment: false
  - id: D5
    description: "Revision visual del sitio en produccion (UAT)"
    requirement: PROD-04
    verification:
      - kind: manual
        ref: "Pendiente: la revision visual la hace el usuario"
        status: unknown
    human_judgment: true
    rationale: "Sin runner de navegador; que el sitio se vea bien con datos reales es UAT humano"

duration: no registrado en esta sesion
completed: 2026-10-05
status: complete
---

# Phase 2 Plan 08: Produccion (backup, deploy de Railway y Vercel, demo) Summary

**El back (Railway, perfil prod) y el front (Vercel) quedaron desplegados contra la base de produccion migrada por Flyway (baseline en V1, migraciones hasta V4), con la demo cargada por el script contra la API: 11 autos, 3 ofertas, 6 destacados y 9 marcas, verificados con lecturas anonimas de solo lectura.**

## Performance

- **Duration:** no registrado en esta sesion (plan no autonomo: los pasos que tocan produccion los hizo el usuario con el runbook; el orquestador solo verifico)
- **Tasks:** 3 (Tarea 1 y Tarea 3 humanas; Tarea 2 automatica, sin registro en esta sesion)
- **Files:** ninguno en los repos (el plan no modifica codigo)

## Accomplishments

- **Tarea 1 (backup, H1 y H2):** existe un backup logico de produccion fuera de los repos, en `C:/Users/toto/Desktop/backups-dante/dante-prod-20261004-0005.dump`, referenciado por `ultimo-backup.txt`. Version de Postgres de produccion: 18.6 (segun el log de Flyway). Conteos por tabla, si hay ADMIN, coincidencia del esquema con V1 y el chequeo con `pg_restore --list`: no registrado en esta sesion.
- **Tarea 2 (ensayo local con copia, H3):** no hay registro en esta sesion de que se haya corrido. Lo que si consta es que, en el deploy real, Flyway hizo baseline en V1 y aplico las migraciones sobre la base de produccion sin errores de validacion.
- **Tarea 3 (deploy y demo, H4 a H8):**
  - Back en Railway: deploy activo `c173751c` (2026-10-05 13:10 GMT-3), perfil prod. Logs de arranque: baseline V1, migraciones hasta V4 (V4 `solicitudes_venta` ya existia: skipping), "Started DanteAutomotoresApplication", sin lineas de SQL. El deploy original de 2026-10-04 aplico V2, V3 y V4 ("Successfully applied 3 migrations ... now at version v4"); el plan esperaba V3, pero el merge con 'Vender tu auto' sumo V4 antes del deploy.
  - Front en Vercel: https://danteautomotores-front.vercel.app, con `/creditos` publicada (200) antes de la carga de la demo (D-10).
  - Demo cargada con `scripts/demo/sembrar-demo.js` contra la API (D-09), no por SQL.

## Verificacion de solo lectura (2026-10-05)

Hecha por el orquestador con GET y OPTIONS anonimos; ningun POST, PUT, PATCH ni DELETE contra produccion.

| Chequeo | Resultado |
|---------|-----------|
| `GET /actuator/health` | 200, `{"status":"UP"}` |
| `GET /api/publicaciones?pagina=1` | responde con `tamanio` 24 (back nuevo activo) |
| `GET /api/publicaciones/destacados` | array JSON |
| Preflight `OPTIONS` con el `Origin` del front | `access-control-allow-origin` igual al dominio de Vercel |
| Front `/` y `/creditos` | 200 y 200 |
| Demo: autos visibles | 11 |
| `GET /api/publicaciones?ofertas=true` | `totalElementos` 3 |
| Facetas | 9 marcas (criterio: 6 o mas) |
| Destacados | 6 |
| Autos sin foto de portada | 0 |
| Estados | 9 DISPONIBLE, 1 RESERVADO, 1 VENDIDO |

## Task Commits

El plan no genero commits de codigo (`files_modified` vacio): los cambios fueron de configuracion en Railway, Vercel y Cloudinary, y datos en produccion. El commit de metadata es `docs(02-08)` en el repo del back.

## Deviations from Plan

### Incidentes durante la carga de la demo

**1. [Rule 3 - Bloqueo] El seed de demo fallo 4 veces con 502 en la subida de fotos a Cloudinary**
- **Found during:** Tarea 3 (H8)
- **Issue:** la API devolvio 502 al subir fotos. Primero por "missing permissions (actions=create)": la API key cargada en Railway no tenia permisos de escritura. Luego "Invalid api_key": las variables de key y de secret de Cloudinary tenian el mismo valor cargado. Luego otra vez por permisos: la key nueva no tenia rol asignado.
- **Fix:** se asigno el rol Master admin a la key `dante-back-prod` en Cloudinary y se cargo cada valor en su variable. Cada corrida parcial se deshizo con `DESHACER=1` del script antes de reintentar, sin tocar la base por SQL.
- **Files modified:** ninguno en los repos (configuracion en Cloudinary y Railway)
- **Commit:** n/a

**2. [Seguridad] Credenciales expuestas en el chat**
- **Found during:** Tarea 3 (H8)
- **Issue:** la key nueva de Cloudinary, su secreto, la tabla con otras 4 keys y otras claves quedaron expuestas en el chat. Contradice la prohibicion de que los secretos no pasen por el chat del agente.
- **Fix:** ninguno inmediato. Queda como pendiente de rotacion (ver abajo). Este SUMMARY no contiene ningun valor de secreto, clave ni contrasena.

### Plan-compliant adjustments

- **Version de la base desplegada:** el plan decia "V3" y "2 migraciones aplicadas"; en el deploy real quedo en V4 con 3 migraciones, porque V4 (`solicitudes_venta`, del merge con 'Vender tu auto') ya estaba en `main`.
- **Tarea 2:** sin registro del ensayo local en esta sesion (ver Accomplishments); la evidencia de que la migracion es segura es el resultado del deploy real.

**Total deviations:** 1 bloqueo resuelto, 1 incidente de seguridad abierto. **Impact:** el despliegue y la demo se completaron; queda deuda de rotacion de secretos.

## Pendientes (para el usuario)

1. **Rotar secretos expuestos:** clave de Postgres, `APP_JWT_SECRET` (cierra las sesiones abiertas) y todas las API keys de Cloudinary: borrar las 4 viejas y reemplazar `dante-back-prod` por una key con rol acotado (no Master admin).
2. **Usuarios de prueba:** hay 3 cuentas COMPRADOR de prueba en la tabla `usuarios` de produccion por revisar y, si corresponde, borrar.
3. **Healthcheck Path (H4 bis):** `/actuator/health` en Railway (Settings, Deploy): sin confirmar.
4. **UAT visual en produccion:** revision del sitio (Home, `/autos`, detalle en los tres estados, agencias, `/creditos`) por el usuario, pendiente. Se suma al UAT visual pendiente de 02-05, 02-06 y 02-07.

## Known Stubs

None en este plan. No se crearon ni modificaron archivos.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: secret-exposure | (fuera de los repos) | Credenciales de Cloudinary y otras claves expuestas en el chat; la key de produccion quedo con rol Master admin hasta rotarla (T-02-30, prohibicion de secretos en el chat) |

T-02-28 mitigado (backup previo y migraciones aditivas; el ensayo local no esta registrado), T-02-29 mitigado (el volcado vive fuera de los repos y no se copio al repo), T-02-31 mitigado (preflight devuelve el dominio exacto de Vercel), T-02-32 mitigado (la demo se cargo con credenciales tipeadas por el usuario y sin LIMPIAR contra produccion; las corridas parciales se deshicieron con DESHACER=1).

## Alcance de los requisitos

PROD-04 (desplegado con perfil prod, esquema por migraciones versionadas, sin SQL en el log) y PROD-02 en produccion (el back arranco con secreto JWT y credenciales de Cloudinary reales; CORS acepta el origen de Vercel) se marcan completos por las lecturas de solo lectura y los logs de arranque. La fase queda a la espera del verifier y del UAT visual humano; este plan no la marca como verificada.

## Next Phase Readiness

El catalogo publico real esta en produccion con la demo cargada. Antes de dar por cerrada la fase: rotar los secretos expuestos y revisar visualmente el sitio. Despues, el orquestador corre el verifier de la fase 2.

## Self-Check: PASSED

- Archivos: este SUMMARY existe en disco; no se crearon otros archivos.
- Commits: el plan no tuvo commits de codigo (0 medido, base y fin en `f93454a`); el commit docs(02-08) se registra a continuacion.
- Sin secretos: el SUMMARY no contiene valores de claves, secretos ni contrasenas.
