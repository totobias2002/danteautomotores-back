---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 02
subsystem: testing
tags: [mockito, webmvctest, postgres, flyway, react, conversaciones, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: ConversacionService, ConversacionController, RegistroDeMensajes, V6 con uk_conversaciones_compra_abierta, humo-comun.js y mensajes-humo.js (04-01)
provides:
  - ConversacionServiceTest, ConversacionSeguridadTest y ConversacionPostgresTest con el contrato del tracer
  - TransaccionesServiceTest con ConversacionService en la lista transaccional
  - mensajes-humo.js con casos negativos (401, 403, 400, 404, campos de mas, aislamiento entre cuentas) y consulta con texto propio
  - Ficha: "Consultar por este auto" abre la conversacion de compra y ya no usa el endpoint viejo de consultas
affects: [04-03, 04-09]

# Los commits del back estan en danteautomotores-back; el de la ficha en danteautomotores-front (f5f259f).
plan_head_before: 0274c202dcb8c25333d4f0fab4c2cb942624e85c
plan_head_after: b483ffab075fdd58adec2f712f77e04959edb41c

actuals:
  tokens: 21000
  tasks: 2
  commits: 2   # medido en el back (2); el front suma 1 commit propio (f5f259f), 3 en total

tech-stack:
  added: []
  patterns:
    - "El service de conversaciones se prueba con Mockito armando el constructor a mano (Clock real, RegistroDeMensajes real sobre un MensajeRepository mock): @InjectMocks dejaria el Clock en null"
    - "Los tests de Postgres leen las fechas con to_char para no depender de la zona de la JVM"

key-files:
  created:
    - src/test/java/com/danteautomotores/service/ConversacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/ConversacionSeguridadTest.java
    - src/test/java/com/danteautomotores/service/ConversacionPostgresTest.java
  modified:
    - src/test/java/com/danteautomotores/service/TransaccionesServiceTest.java
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/pages/PublicacionDetallePage.jsx

key-decisions:
  - "Sin cambios de codigo de produccion en el back: ningun test demostro algo roto, el contrato de 04-01 se sostiene"
  - "La ficha navega a /mensajes tras enviar (04-03 lo cambia al hilo) y conserva el boton deshabilitado durante el envio porque el componente se desmonta al navegar"
  - "Un 401 en Consultar por este auto no muestra error: el interceptor de api.js ya cierra la sesion, igual que en Lo quiero y favoritos"

requirements-completed: [MSG-01, MSG-03]

duration: 35min
completed: 2026-10-07
---

# Phase 4 Plan 02: Contrato del tracer y Consultar por este auto Summary

**El camino de "Lo quiero" queda cubierto por tests de servicio, seguridad HTTP y Postgres real (indice unico parcial, ultimo mensaje en una consulta, fechas en UTC), y "Consultar por este auto" abre la misma conversacion con el texto escrito sin tocar el endpoint viejo de consultas.**

## Performance

- **Duration:** ~35 min
- **Completed:** 2026-10-07
- **Tasks:** 2 de 2
- **Files modified:** 6 entre los dos repos

## Accomplishments

- `ConversacionServiceTest` (12 tests, Mockito con la `VerificacionCuenta` real como `@Spy` y un `Clock.fixed` en otra zona): crea la conversacion COMPRA ABIERTA con `creadaEn` y `ultimoMensajeEn` iguales al instante fijo en UTC; texto propio recortado y texto automatico (marca, modelo y anio) para el blanco; un segundo pedido devuelve la existente sin guardar nada, o suma solo el mensaje propio; la cuenta incompleta lanza `CuentaNoVerificadaException` con los faltantes sin buscar la publicacion; VENDIDO rechaza con "Este auto ya se vendió", RESERVADO se acepta; 404 por auto o cuenta inexistentes; `listarMias` con extracto de 120 caracteres y fechas `Instant`.
- `ConversacionSeguridadTest` (9 tests, `@WebMvcTest` sobre `SeguridadWebMvcTestBase`): sin token 401 en POST y GET, admin 403 en ambos, comprador 200 con el mail del token, 403 `CUENTA_NO_VERIFICADA` con `faltantes`, cuerpo con `tipo`/`estado`/`usuarioId` de mas ignorado (reflexion sobre `ConversacionRequest`: solo `publicacionId` y `mensaje`), 400 con `campos.publicacionId` y con `campos.mensaje` (2001 caracteres; 2000 se acepta).
- `ConversacionPostgresTest` (7 tests contra Postgres real con V6 validada por Hibernate): dos ABIERTAS de compra del mismo usuario y auto lanzan `DataIntegrityViolationException`; una ABIERTA mas CERRADAS conviven, y las de otro usuario u otro auto no chocan; `iniciarCompra` dos veces deja una sola fila con el mismo id; las fechas guardadas son las del reloj fijo en UTC; `findUltimosPorConversaciones` devuelve el mensaje de mayor id y respeta las conversaciones pedidas; `listarMias` no mezcla cuentas.
- `TransaccionesServiceTest` suma `ConversacionService.class` a los services transaccionales.
- Humo (`mensajes-humo.js`, 16 chequeos): 401 sin token, 403 del admin, 403 `CUENTA_NO_VERIFICADA` con `EMAIL_SIN_CONFIRMAR` para el mail sin confirmar (sin crear nada), 400 "Este auto ya se vendió" con auto VENDIDO (y RESERVADO aceptado), 404 con auto inexistente, campos de mas ignorados, 400 con `campos.mensaje`, lista de una segunda cuenta sin la conversacion de la primera, mas la consulta con texto propio (crea, reutiliza y actualiza `ultimoMensaje`; el blanco usa el texto automatico). Limpia todos los autos que crea.
- Front: `handleConsultaSubmit` pasa a `api.post('/conversaciones', { publicacionId: Number(id), mensaje })` y navega a `/mensajes`; se elimina el estado `enviado` y su texto; el boton dice "Enviar mensaje"; se conservan `enviando`, `errorConsulta` (con `mensajeDeError`), `maxLength` 2000, el aviso de reservado y el acceso para quien no puede consultar. La ficha ya no llama a `/consultas`.

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=ConversacionServiceTest,ConversacionSeguridadTest,ConversacionPostgresTest,TransaccionesServiceTest,MigracionesPostgresTest,SeguridadErroresTest -Ddante.pg.required=true` | 55 tests, 0 fallas, 0 errores, 0 skipped (Servicio 12, Seguridad 9, Postgres 7, Transacciones 3, Migraciones 9, SeguridadErrores 15) |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` (tras la tarea 1) | `humo: 13 ok, 0 fallas, 0 skip` |
| `npm --prefix ../danteautomotores-front run build && npm ... test` | build OK; 43 tests, 0 fallas |
| `con-back-local.sh ... mensajes-humo.js` (tras la tarea 2) | `humo: 16 ok, 0 fallas, 0 skip` |

Criterios de aceptacion: `grep -c "ConversacionService.class"` en `TransaccionesServiceTest` imprime 1; `grep -c "api.post('/conversaciones'"` en la ficha imprime 2; `grep -rn "'/consultas'" ../danteautomotores-front/src` no encuentra nada; el humo termina con "0 fallas" e incluye chequeos 401, 403, 400 y 404. El mensaje de Maven no informa "Skipped" para `ConversacionPostgresTest` ni `MigracionesPostgresTest`.

## Task Commits

1. **Tarea 1 (back):** `246b9af` test(04-02): contrato del tracer de conversaciones (servicio, seguridad HTTP, Postgres real y humo negativo)
2. **Tarea 2 (front):** `f5f259f` feat(04-02): Consultar por este auto abre la conversacion con el texto del usuario
3. **Tarea 2 (back):** `b483ffa` test(04-02): el humo cubre Consultar por este auto con texto propio

## Deviations from Plan

None - plan executed exactly as written. Ningun test encontro un defecto en el codigo de produccion de 04-01.

## Auth Gates

None.

## Known Stubs

Ninguno nuevo. Siguen vivos a proposito: el aviso temporal de "Simula tu financiamiento" y "Cotizar" (Fase 5). `ConsultaServiceTest`, `ConsultaSeguridadTest` y el endpoint `/api/consultas` siguen en el repo hasta 04-09, pero el front ya no los usa.

## Threat Flags

Ninguno. T-04-08 (asignacion masiva) queda mitigado y probado por reflexion sobre `ConversacionRequest`, por `ConversacionSeguridadTest` y por el humo; T-04-09 (datos de contacto) se cumple porque la consulta de la ficha solo manda publicacion y texto.

## Pendiente de verificacion humana

Ninguno propio de este plan. El recorrido visual de la ficha (escribir en "Consultar por este auto", enviar y llegar a Mis mensajes) se revisa junto con el human-check de 04-10.

## Self-Check: PASSED

- Archivos creados verificados en disco (los tres tests nuevos y la ficha modificada).
- Commits `246b9af`, `b483ffa` (back) y `f5f259f` (front) existen en sus repos.
