---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 03
subsystem: api
tags: [spring, mockito, webmvctest, postgres, react, conversaciones, rate-limit, polling, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: Conversacion, Mensaje, RegistroDeMensajes, ConversacionService/Controller/Mapper, MisMensajesPage, utils/mensajes.js, humo-comun.js (04-01 y 04-02)
provides:
  - GET /api/conversaciones/{id} y POST /api/conversaciones/{id}/mensajes (hilo del comprador, solo con la cuenta del token)
  - Limite de envio de 20 mensajes cada 10 minutos por cuenta (cuenta tambien el Lo quiero), 429 con Retry-After 600
  - ConversacionPage en /mensajes/:id, HiloDeMensajes reutilizable por el admin (04-06) y useSondeo
  - Mis mensajes enlaza cada fila al hilo; Lo quiero y Consultar por este auto llevan directo al hilo
affects: [04-04, 04-06, 04-09, 04-10]

# Los commits estan repartidos en dos repos: back (3a087b7) y front (37d8614).
plan_head_before: 088108805da9db99ca7b5cc00e81a5f5f6ba033d
plan_head_after: 3a087b7c595ae3d892a613737be383df8fd5e378

actuals:
  tokens: 36000
  tasks: 1
  commits: 1   # medido en el back (1); el front suma 1 commit propio (37d8614), 2 en total

tech-stack:
  added: []
  patterns:
    - "Toda busqueda del comprador va por id y dueno (findByIdAndUsuarioId): ajena e inexistente dan el mismo 404"
    - "El limite de envio vive en ConversacionService y se aplica despues de exigir la cuenta, antes de buscar la conversacion"
    - "useSondeo guarda el callback en una referencia y solo consulta con document.visibilityState visible"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/conversacion/MensajeRequest.java
    - src/main/java/com/danteautomotores/dto/conversacion/MensajeResponse.java
    - src/main/java/com/danteautomotores/dto/conversacion/ConversacionDetalleResponse.java
    - ../danteautomotores-front/src/pages/ConversacionPage.jsx
    - ../danteautomotores-front/src/components/HiloDeMensajes.jsx
    - ../danteautomotores-front/src/hooks/useSondeo.js
  modified:
    - src/main/java/com/danteautomotores/service/ConversacionService.java
    - src/main/java/com/danteautomotores/controller/ConversacionController.java
    - src/main/java/com/danteautomotores/mapper/ConversacionMapper.java
    - src/main/java/com/danteautomotores/repository/ConversacionRepository.java
    - src/main/java/com/danteautomotores/repository/MensajeRepository.java
    - src/test/java/com/danteautomotores/service/ConversacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/ConversacionSeguridadTest.java
    - src/test/java/com/danteautomotores/service/ConversacionPostgresTest.java
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/routes/AppRouter.jsx
    - ../danteautomotores-front/src/pages/MisMensajesPage.jsx
    - ../danteautomotores-front/src/pages/PublicacionDetallePage.jsx
    - ../danteautomotores-front/src/utils/mensajes.js
    - ../danteautomotores-front/src/utils/mensajes.test.js

key-decisions:
  - "El limite de envio se aplica tambien en iniciarCompra (cada Lo quiero cuenta): cierra T-04-04 de 04-01 y evita abrir conversaciones sin freno"
  - "Los DTOs del hilo no exponen datos de la cuenta que escribio: el lado de la agencia se identifica solo por el autor AGENCIA y el front lo rotula Dante Automotores (D-05)"
  - "useSondeo se exporta con nombre y por defecto; la consulta periodica de ConversacionPage es silenciosa (un fallo de red no tapa el hilo ya cargado)"
  - "El humo respeta el presupuesto de envios: la cuenta principal usa 11 de 20 por corrida y solo la cuenta descartable llega al 429"

requirements-completed: [MSG-03, MSG-04]

duration: 45min
completed: 2026-10-07
---

# Phase 4 Plan 03: El comprador conversa en su hilo Summary

**El comprador abre su conversacion desde Mis mensajes o directo desde la ficha, lee el hilo completo, escribe mensajes de 1 a 2000 caracteres que se guardan por el unico punto de registro, y el back protege las conversaciones ajenas, las cerradas, las cuentas incompletas y el exceso de envios (20 cada 10 minutos, 429 con Retry-After).**

## Performance

- **Duration:** ~45 min
- **Completed:** 2026-10-07
- **Tasks:** 1 de 1
- **Files modified:** 23 entre los dos repos

## Accomplishments

- Back: `obtenerMia` (solo lectura, 404 igual para ajena e inexistente) y `enviarMensaje` (exige cuenta, aplica el limite, 404 propio, 400 `Esta conversación está cerrada.` y registra con `registroDeMensajes.agregar` el texto recortado). `findByIdAndUsuarioId` carga el auto y la agencia con `@EntityGraph`; `findByConversacionIdOrderByIdAsc` da el hilo en orden. `MensajeRequest` valida `@NotBlank` y `@Size(max = 2000)`.
- Limite de envio (D-10): clave `msg:<id de la cuenta>`, 20 en 10 minutos, `LimiteDeIntentosException` con 600 segundos; se aplica en `enviarMensaje` y en `iniciarCompra`.
- Tests: `ConversacionServiceTest` 21 (con `LimitadorDeIntentos` real: el mensaje 21 lanza el limite y no guarda; el limite es por cuenta y comparte el cupo con Lo quiero), `ConversacionSeguridadTest` 18 (401, 403 admin, 200 con el mail del token, 404, 400 con `campos.texto` para vacio, blanco, sin texto y 2001, 429 con `Retry-After: 600`, 400 de cerrada y 403 de cuenta incompleta), `ConversacionPostgresTest` 11 contra Postgres real (ajena no se encuentra, orden por id sin mezclar conversaciones, el envio real mueve `ultimo_mensaje_en` a UTC, ajena 404 en lectura y escritura, cerrada rechazada sin guardar).
- Front: `HiloDeMensajes` (propios a la derecha en navy, los del otro con etiqueta, `whitespace-pre-line break-words` sobre texto de React, `maxLength` 2000 con contador desde 1800, Ctrl+Enter, error con `mensajeDeError`, desplazamiento al ultimo mensaje), `ConversacionPage` (admin a `/admin`, Cargando, 404 como "No encontramos esta conversación", Reintentar, auto asociado, aviso de cerrada con link a la ficha, sondeo cada 10 s), `useSondeo`, ruta `/mensajes/:id` con `RutaVerificada`, filas de Mis mensajes como `Link`, ficha que navega a `/mensajes/${data.id}` y `horaYFecha` con 5 tests.
- Humo (`mensajes-humo.js`, 22 chequeos): hilo con el primer mensaje, fechas con Z y sin datos personales; envio con HTML literal y orden; 400 con `campos.texto` para vacio, blanco, 2001 y sin cuerpo; 401 y 403 en hilo y envio; segunda cuenta con 404 al leer y escribir, e inexistente con la misma respuesta que ajena; cuenta descartable con 429 y `Retry-After` en el envio 21, el mensaje rechazado fuera del hilo y un Lo quiero tambien frenado.

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=ConversacionServiceTest,ConversacionSeguridadTest,ConversacionPostgresTest,TransaccionesServiceTest -Ddante.pg.required=true` | BUILD SUCCESS, 53 tests en la ultima corrida, 0 fallas, 0 errores, 0 skipped (Servicio 21, Postgres 11, Transacciones 3 y Seguridad) |
| `npm --prefix ../danteautomotores-front run build && npm ... test` | build OK; 48 tests, 0 fallas |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` | `humo: 22 ok, 0 fallas, 0 skip` |

Criterios de aceptacion: `grep -c "registroDeMensajes.agregar"` en `ConversacionService` imprime 3; `grep -c "LimiteDeIntentosException"` imprime 2; `grep -c 'path="/mensajes/:id"'` en `AppRouter.jsx` imprime 1; `grep -c "whitespace-pre-line"` en `HiloDeMensajes.jsx` imprime 1; `grep -rn "dangerouslySetInnerHTML" ../danteautomotores-front/src` no encuentra nada; el humo termina con "0 fallas".

## Task Commits

1. **Tarea 1 (front):** `37d8614` feat(04-03): el comprador abre el hilo de su conversacion y escribe mensajes
2. **Tarea 1 (back):** `3a087b7` feat(04-03): hilo de la conversacion, envio de mensajes y limite de 20 cada 10 minutos

## Deviations from Plan

None - plan executed exactly as written. Un detalle de alcance: se agregaron al `ConversacionSeguridadTest` y al humo chequeos extra (400 de conversacion cerrada y 403 de cuenta incompleta en el envio; respuesta identica entre ajena e inexistente) que el plan no pedia de forma explicita.

## Auth Gates

None.

## Known Stubs

Ninguno nuevo. Siguen vivos a proposito el aviso temporal de "Simula tu financiamiento" y "Cotizar" (Fase 5). `ConsultaServiceTest`, `ConsultaSeguridadTest` y el endpoint `/api/consultas` siguen en el repo hasta 04-09.

## Threat Flags

Ninguno. T-04-10 a T-04-14 quedan mitigados y probados: IDOR por `findByIdAndUsuarioId` (servicio, seguridad, Postgres y humo con dos cuentas), inundacion por el limite de 20 cada 10 minutos (servicio y humo), conversacion cerrada con 400, cuenta incompleta con 403 y faltantes, y texto sin HTML crudo (`grep` sobre todo `src` sin `dangerouslySetInnerHTML`).

## Limitaciones conocidas

- El limite de envio vive en memoria de una sola instancia (igual que el resto de los limitadores): un reinicio lo resetea.
- ESLint no tiene `eslint.config.*` en el front (pre-existente), asi que `npm run lint` no corre; no se agrego configuracion en este plan.

## Pendiente de verificacion humana

Human-check de la tarea (no automatizable aca): con el back local y el front de prueba en el 5174, como comprador verificado tocar "Lo quiero" y ver que abre el hilo con el primer mensaje; escribir dos mensajes (uno con etiquetas HTML que se ve literal); volver a Mis mensajes y abrir la fila; pegar el id de una conversacion ajena en la URL y ver "No encontramos esta conversación". La pasada completa por navegador es 04-10.

## Self-Check: PASSED

- Archivos creados verificados en disco (tres DTOs del back; `ConversacionPage.jsx`, `HiloDeMensajes.jsx` y `useSondeo.js` del front).
- Commits `3a087b7` (back) y `37d8614` (front) existen en sus repos; `commits:` medido desde el ledger en disco (1 en el back).
