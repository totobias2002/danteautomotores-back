---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 09
subsystem: api
tags: [spring, flyway, postgres, react, migracion, consultas, conversaciones, cascada, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: tablas de V6, ConversacionRepository, MensajeRepository, ConversacionPostgresTest, mensajes-humo.js, el gate de conversaciones y el dialogo de eliminar de AdminDashboardPage (04-01 a 04-08)
provides:
  - V7__fase4_consultas_a_conversaciones.sql (aditiva) que copia las consultas con cuenta de comprador a conversaciones COMPRA abiertas y mensajes sin leer, y pasa la FK de consultas a ON DELETE CASCADE
  - PublicacionService.eliminar borra favoritos, mensajes y conversaciones en una transaccion; obtenerImpactoEliminacion devuelve cantidadConversaciones
  - MensajeRepository.deleteByPublicacionId, ConversacionRepository.deleteByPublicacionId y countByPublicacionId
  - El aviso de eliminar un auto del panel dice "N conversaciones con compradores"
  - Se retira el codigo del modelo viejo de consultas (entidad, repositorio, servicio, controlador, DTOs, mapper, tests y reglas de seguridad)
  - cuentas-humo.js prueba el gate con POST /conversaciones; mensajes-humo.js prueba el impacto y el borrado en cascada
affects: [04-10, 04-11]

# Los commits estan repartidos en dos repos: back (449db04, 2ce4f98) y front (4d45be2).
plan_head_before: 360d1256c02d45164934db4d38dd478a2f507ef1
plan_head_after: 2ce4f983d20bfa0c16ac3283393a6b8b9fb4881a

actuals:
  tokens: 21000   # chars/4 sobre el diff de los tres commits (back + front), sin contar los archivos borrados
  tasks: 2
  commits: 2   # medido en el back (git rev-list --count plan_head_before..plan_head_after); el front suma 1 commit propio (4d45be2), 3 en total

tech-stack:
  added: []
  patterns:
    - "Migracion de datos aditiva en SQL puro: INSERT ... SELECT con ON CONFLICT sobre el indice unico parcial, sin DELETE/UPDATE/DROP TABLE, y un bloque DO que busca la FK por pg_constraint porque su nombre cambia entre bases"
    - "Los mensajes migrados solo se cargan en conversaciones sin mensajes, para no mezclar el historial viejo con una conversacion que ya tenia vida"

key-files:
  created:
    - src/main/resources/db/migration/V7__fase4_consultas_a_conversaciones.sql
  modified:
    - src/main/java/com/danteautomotores/repository/MensajeRepository.java
    - src/main/java/com/danteautomotores/repository/ConversacionRepository.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/dto/publicacion/ImpactoEliminacionResponse.java
    - src/main/java/com/danteautomotores/config/SecurityConfig.java
    - src/main/java/com/danteautomotores/controller/AdminPublicacionController.java
    - src/test/java/com/danteautomotores/migration/MigracionesPostgresTest.java
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/AdminPublicacionControllerTest.java
    - src/test/java/com/danteautomotores/service/ConversacionPostgresTest.java
    - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
    - scripts/verify/cuentas-humo.js
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
  deleted:
    - src/main/java/com/danteautomotores/entity/Consulta.java
    - src/main/java/com/danteautomotores/repository/ConsultaRepository.java
    - src/main/java/com/danteautomotores/service/ConsultaService.java
    - src/main/java/com/danteautomotores/controller/ConsultaController.java
    - src/main/java/com/danteautomotores/dto/consulta/ConsultaRequest.java
    - src/main/java/com/danteautomotores/dto/consulta/ConsultaResponse.java
    - src/main/java/com/danteautomotores/mapper/ConsultaMapper.java
    - src/test/java/com/danteautomotores/service/ConsultaServiceTest.java
    - src/test/java/com/danteautomotores/controller/ConsultaSeguridadTest.java

key-decisions:
  - "V7 usa ON CONFLICT sobre el indice unico parcial de V6 y solo agrega mensajes a conversaciones sin mensajes, de modo que una base de desarrollo con conversaciones ya abiertas no mezcla fechas viejas con el hilo vivo"
  - "La tabla consultas queda en la base sin entidad: ddl-auto=validate ignora las tablas sin entidad, y el back anterior sigue funcionando contra la base migrada (vuelta atras de un deploy)"

patterns-established:
  - "Borrar un auto sigue el orden favoritos, mensajes, conversaciones, publicacion en una sola transaccion; la cascada de V6 queda como red de seguridad"

requirements-completed: [MSG-01, MSG-03]

duration: una sesion
completed: 2026-10-07
---

# Phase 4 Plan 09: Las consultas viejas pasan a la bandeja y se retira el modelo de consultas Summary

**V7 convierte las consultas viejas con cuenta de comprador en conversaciones abiertas sin leer (la tabla `consultas` queda intacta), borrar un auto avisa y borra sus conversaciones en una transaccion, y el codigo del modelo viejo de consultas desaparece.**

## Performance

- **Completed:** 2026-10-07
- **Tasks:** 2 de 2
- **Files modified:** 24 (23 en el back, 1 en el front; 9 de ellos borrados)

## Accomplishments

- `V7__fase4_consultas_a_conversaciones.sql` tiene tres pasos y ningun DELETE/UPDATE de datos: crea una conversacion COMPRA ABIERTA por cada par (cuenta COMPRADOR por mail sin distinguir mayusculas, auto) con `creada_en` en la primera consulta y `ultimo_mensaje_en` en la ultima; inserta un mensaje USUARIO por consulta en orden de id, con la fecha original y `leido_en` nulo (recortado y limitado a 2000 caracteres, o "Consulta sin mensaje" si queda vacio); y reemplaza por catalogo la FK de `consultas` hacia `publicaciones` por `fk_consultas_publicacion ... ON DELETE CASCADE`. Las consultas sin cuenta y las de cuentas admin no se migran y siguen en `consultas`.
- Borrar un auto: `PublicacionService.eliminar` borra favoritos, mensajes, conversaciones y la publicacion en la misma transaccion; el impacto devuelve exactamente `cantidadConversaciones` y `cantidadFavoritos`. El aviso del panel dice "N conversaciones con compradores" (singular "1 conversacion con compradores").
- Se borraron `Consulta`, `ConsultaRepository`, `ConsultaService`, `ConsultaController`, `ConsultaRequest`, `ConsultaResponse`, `ConsultaMapper` y sus dos tests, y las dos reglas de `/api/consultas` de `SecurityConfig`. Nada del front ni de los humos llama a `/consultas`.
- `cuentas-humo.js` conserva su cobertura del gate sobre `POST /conversaciones` (401 sin token, 403 `CUENTA_NO_VERIFICADA` con `EMAIL_SIN_CONFIRMAR` antes de buscar el auto, 404 con la cuenta verificada). `mensajes-humo.js` suma el chequeo del impacto (1 conversacion, sin la clave vieja) y que despues de borrar el auto la lista del comprador, su hilo (404), el hilo del admin (404) y la bandeja ya no la traen.

## Task Commits

1. **Tarea 1: V7, cascada de borrado, impacto y aviso del panel** - back `449db04`, front `4d45be2` (feat)
2. **Tarea 2: retiro del codigo de consultas y humo de cuentas sobre conversaciones** - back `2ce4f98` (feat)

## Verificacion

- Tarea 1: `mvn -B -o -Djava.version=17 test -Dtest=MigracionesPostgresTest,PublicacionServiceTest,AdminPublicacionControllerTest,ConversacionPostgresTest,TransaccionesServiceTest -Ddante.pg.required=true`: 92 tests, 0 fallas, 0 salteados (la prueba `v7MigraNLasConsultasConCuentaALaBandejaSinPerderNada` corre sobre la produccion simulada con consultas de mayusculas, con dos mensajes sobre un auto, sin cuenta, de la cuenta admin, solo espacios y de 2500 caracteres; confirma que `consultas` conserva todas sus filas intactas, que borrar un auto con consultas viejas ya no falla y que la FK quedo en cascada).
- Front: `npm --prefix ../danteautomotores-front run build` correcto y `npm test`: 58 tests, 0 fallas.
- Humo de mensajes (`con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js`): `humo: 49 ok, 0 fallas, 0 skip`.
- Tarea 2: suite completa del back con `-Ddante.pg.required=true`: 772 tests, 0 fallas, 0 salteados (menos que los 784 de 04-08 por los dos tests de consultas borrados).
- Humo de cuentas (`con-back-local.sh --vacia dante_humo_cuentas node scripts/verify/cuentas-humo.js`): `humo: 28 ok, 0 fallas, 0 skip`.
- Criterios de aceptacion: V1 a V6 sin cambios (solo V7 nuevo); 0 coincidencias de `DELETE FROM|DROP TABLE|TRUNCATE` fuera de comentarios; `cantidadConversaciones` 1 vez en el DTO y `cantidadConsultas` en ningun lugar de `src` ni del front; `conversacionRepository` 3 veces en `PublicacionService`; los 9 archivos del modelo viejo no existen; sin referencias a `ConsultaRepository|ConsultaService|ConsultaController`; 0 `api/consultas` en `SecurityConfig`; 0 `"/consultas"` en `cuentas-humo.js`.
- V7 se probo solo contra bases locales y descartables (la de tests y las de los humos); no se corrio contra produccion.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Datos] V7 solo carga mensajes en conversaciones sin mensajes**
- **Found during:** Tarea 1
- **Issue:** con `ON CONFLICT DO NOTHING`, en una base de desarrollo que ya tuviera una conversacion abierta por un par usuario-auto, los mensajes viejos se habrian agregado al hilo vivo con ids posteriores a los nuevos, rompiendo el orden cronologico.
- **Fix:** el paso 2 agrega `WHERE NOT EXISTS (mensajes de esa conversacion)`; en una base nueva (la de produccion) no cambia nada porque las conversaciones las crea el paso 1.
- **Files modified:** src/main/resources/db/migration/V7__fase4_consultas_a_conversaciones.sql
- **Commit:** 449db04

**2. [Rule 1 - Consistencia] Comentarios que seguian hablando de consultas**
- **Found during:** Tarea 2
- **Issue:** el comentario de `AdminPublicacionController` ("cuantas consultas y favoritos") y el de `SecurityConfig` (compartido con la regla borrada de consultas) quedaban desactualizados.
- **Fix:** se actualizaron a conversaciones y a "Cotizar exige sesion de comprador".
- **Files modified:** AdminPublicacionController.java, SecurityConfig.java
- **Commit:** 2ce4f98

Por lo demas, el plan se ejecuto como estaba escrito.

**Total deviations:** 2 (una de datos, una de consistencia de comentarios).

## Issues Encountered

Un script de edicion de Python con heredoc fallo por el entrecomillado del shell antes de modificar nada; se rehizo la edicion del test de migraciones con las herramientas de edicion, sin efectos en el repo.

## Known Stubs

Ninguno.

## Threat Flags

Ninguno fuera del plan. Mitigaciones aplicadas: T-04-34 (V7 solo inserta y cambia una FK; sin DELETE/DROP TABLE/TRUNCATE, probado sobre una produccion simulada con consultas de todos los tipos; el ensayo con una copia y el backup quedan para 04-11), T-04-36 (el panel avisa antes cuantas conversaciones y favoritos se van; todo en una transaccion), T-04-37 (V6 y V7 son aditivas: el rollback es redeployar el back anterior sin tocar la base), T-04-38 (se quitaron las dos reglas de `/api/consultas`). T-04-35 aceptada: las consultas sin cuenta quedan en la tabla vieja con sus datos personales, sin entidad ni endpoint; su eliminacion definitiva (con revision de la agencia por la Ley 25.326) queda como limpieza posterior.

## Next Phase Readiness

04-10 actualiza `scripts/demo/README.md` y el README del back (todavia mencionan consultas). 04-11 documenta el orden de los deploys, el backup y el ensayo de V7 con una copia de produccion antes de aplicarla.

## Self-Check: PASSED

- Archivos: V7 y SUMMARY presentes; los 9 archivos del modelo viejo ausentes.
- Commits: `449db04` y `2ce4f98` (back) y `4d45be2` (front) existen.
