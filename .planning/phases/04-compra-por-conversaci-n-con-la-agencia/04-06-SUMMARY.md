---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 06
subsystem: api
tags: [spring, jpa, postgres, react, admin, hilo, cerrar, reabrir, polling, humo]
status: complete

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: RegistroDeMensajes, ConversacionAdminService.listar, AdminConversacionController, ConversacionMapper (toResumenParaAdmin), MensajeRepository (marcarLeidos y conteos), HiloDeMensajes, useSondeo, NoLeidosContext, AdminMensajesPage (04-01 a 04-05)
provides:
  - GET /api/admin/conversaciones/{id} (hilo con usuario y noLeidos del lado de la agencia)
  - POST /api/admin/conversaciones/{id}/mensajes (respuesta de la agencia, autor AGENCIA con la cuenta admin)
  - POST /api/admin/conversaciones/{id}/leida, /cerrar y /reabrir
  - AdminConversacionPage en /admin/mensajes/:id con responder, cerrar y reabrir
  - Redireccion del admin que abre /mensajes/:id al mismo hilo en su panel
  - Recorrido completo comprador y agencia en el humo
affects: [04-07, 04-08, 04-10]

# Los commits estan repartidos en dos repos: back (6cb2144) y front (dfd07b1).
plan_head_before: 4a3ff4849da8afcafdbadcbc4d73658897d7aef8
plan_head_after: 6cb21441dc13807d0e16014a946202bd30520f3e

actuals:
  tokens: 18000   # chars/4 sobre el diff de los dos commits (back + front)
  tasks: 1
  commits: 1   # medido en el back (1); el front suma 1 commit propio (dfd07b1), 2 en total

tech-stack:
  added: []
  patterns:
    - "responder registra por el unico punto de registro (RegistroDeMensajes.agregar): el mensaje y el movimiento de la conversacion van juntos, y queda la cuenta admin que escribio (D-05)"
    - "reabrir verifica con existsBy...IdNot antes de tocar nada; el indice unico parcial de V6 sigue siendo la ultima defensa"
    - "toDetalleParaAdmin calcula los no leidos del mismo hilo ya cargado (mensajes del usuario sin leer), espejo de toDetalle del comprador"

key-files:
  created:
    - ../danteautomotores-front/src/pages/admin/AdminConversacionPage.jsx
  modified:
    - src/main/java/com/danteautomotores/repository/ConversacionRepository.java
    - src/main/java/com/danteautomotores/mapper/ConversacionMapper.java
    - src/main/java/com/danteautomotores/service/ConversacionAdminService.java
    - src/main/java/com/danteautomotores/controller/AdminConversacionController.java
    - src/test/java/com/danteautomotores/service/ConversacionAdminServiceTest.java
    - src/test/java/com/danteautomotores/controller/AdminConversacionSeguridadTest.java
    - src/test/java/com/danteautomotores/service/ConversacionAdminPostgresTest.java
    - scripts/verify/mensajes-humo.js
    - ../danteautomotores-front/src/routes/AppRouter.jsx
    - ../danteautomotores-front/src/pages/ConversacionPage.jsx

key-decisions:
  - "Cerrar y reabrir son idempotentes: cerrar una cerrada o reabrir una abierta devuelve el resumen sin cambiar nada (ni el instante cerradaEn)"
  - "Reabrir solo mira otras abiertas cuando la conversacion es COMPRA con auto; una cotizacion se reabre siempre"
  - "marcarLeida de la agencia devuelve los conteos de toda la bandeja (noLeidos y conversaciones), igual que el contador del Navbar del admin"
  - "El front mezcla el resumen que devuelven cerrar y reabrir sobre la conversacion cargada, sin volver a pedir el hilo"

patterns-established:
  - "Hilo del admin como espejo de ConversacionPage del comprador: mismas piezas (HiloDeMensajes, useSondeo, NoLeidosContext), cambia miAutor, la etiqueta del otro y el endpoint"

requirements-completed: [MSG-04, MSG-05, MSG-08]

duration: 40min
completed: 2026-10-07
---

# Phase 4 Plan 06: El admin atiende una conversacion Summary

**El admin abre una conversacion desde la bandeja, responde con un mensaje firmado por su cuenta (el usuario lo ve como "Dante Automotores"), marca como leidos los mensajes del usuario al abrirla y puede cerrarla y reabrirla; el comprador ve subir su contador con la respuesta y bajar al abrir el hilo, una cerrada no recibe mensajes de nadie y reabrir no puede dejar dos compras abiertas del mismo usuario por el mismo auto.**

## Performance

- **Duration:** ~40 min
- **Completed:** 2026-10-07
- **Tasks:** 1 de 1
- **Files modified:** 11 entre los dos repos (8 en el back, 3 en el front, mas el SUMMARY)

## Accomplishments

- Back: `ConversacionAdminService` suma `obtener` (`readOnly`, 404 "No existe la conversacion", con `usuario` y los no leidos de la agencia), `responder` (busca la cuenta admin por mail, rechaza una CERRADA con 400 "Esta conversacion esta cerrada. Reabrila para responder." y registra el texto recortado con `registroDeMensajes.agregar(..., AutorMensaje.AGENCIA, ...)`), `marcarLeida` (solo los mensajes de autor USUARIO de esa conversacion, con el instante UTC del `Clock`, y devuelve los conteos de la agencia), `cerrar` (ABIERTA a CERRADA con `cerradaEn` UTC, idempotente) y `reabrir` (CERRADA a ABIERTA con `cerradaEn` nulo; una COMPRA con auto se rechaza con 400 "El usuario ya tiene otra conversacion abierta por este auto." si existe otra abierta). `ConversacionRepository.existsByUsuarioIdAndPublicacionIdAndTipoAndEstadoAndIdNot`; `ConversacionMapper.toDetalleParaAdmin`; `AdminConversacionController` con `GET /{id}`, `POST /{id}/mensajes` (`@Valid`, el autor sale del token), `/leida`, `/cerrar` y `/reabrir`. `SecurityConfig` no cambio.
- Front: `AdminConversacionPage` (`/admin/mensajes/:id`, `ProtectedRoute soloAdmin`) con "Cargando...", 404 con link a la bandeja, error con "Reintentar", encabezado con nombre, apellido, mail, auto (portada, "marca modelo anio", precio, estado, link a la ficha), tipo y estado; `HiloDeMensajes` con `miAutor` AGENCIA y la etiqueta del usuario; aviso "Conversacion cerrada. Reabrila para responder." en lugar de la caja de texto; sondeo cada 10 s; marca como leidos los mensajes del usuario con la pestana visible y actualiza el contador del contexto; boton "Cerrar conversacion" o "Reabrir conversacion" que muestra el error del back. El admin que abre `/mensajes/:id` llega a `/admin/mensajes/:id`.
- Humo (`mensajes-humo.js`, 38 chequeos, 9 nuevos): 401 y 403 en los cinco endpoints del hilo de la agencia, el admin abre el hilo (usuario, 2 sin leer, sin dni ni telefono, 404 en inexistente), responde (vacio y 2001 caracteres dan 400, autor AGENCIA, texto recortado), marca leida (su contador baja en 2), el comprador ve el contador, la fila de la lista y el mensaje sin leer y lo deja en cero, el contador del admin sube cuando el comprador vuelve a escribir, cerrar (el comprador recibe 400 "Esta conversacion esta cerrada." y el admin 400), reabrir (el comprador vuelve a escribir) y el conflicto de reapertura tras un segundo "Lo quiero".

## Verification

| Comando del plan | Resultado |
|---|---|
| `mvn -B -o -Djava.version=17 test -Dtest=ConversacionAdminServiceTest,AdminConversacionSeguridadTest,ConversacionAdminPostgresTest,ConversacionServiceTest,ConversacionPostgresTest -Ddante.pg.required=true` | BUILD SUCCESS, 93 tests (19 servicio, 14 seguridad, 15 Postgres de la agencia, 29 y 16 del comprador), 0 fallas, 0 skipped |
| `mvn -B -o -Djava.version=17 test -Ddante.pg.required=true` (suite completa, extra) | BUILD SUCCESS, 746 tests, 0 fallas, 0 skipped |
| `npm --prefix ../danteautomotores-front run build && npm ... test` | build OK; 58 tests, 0 fallas |
| `con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js` | `humo: 38 ok, 0 fallas, 0 skip` |

Criterios de aceptacion: `registroDeMensajes.agregar` en `ConversacionAdminService` (1), `AdminConversacionPage` en `AppRouter.jsx` (2: import y ruta), `HiloDeMensajes` en `AdminConversacionPage.jsx` (2), `dangerouslySetInnerHTML` no aparece en `src`, el humo termina con "0 fallas" y cubre responder, cerrar, reabrir y el conflicto de reapertura. La base `dante_humo_mensajes` se borro al terminar. Presupuesto del limite de envio de la cuenta principal del humo: 15 de 20.

## Task Commits

1. **Tarea 1 (back):** `6cb2144` feat(04-06): la agencia responde una conversacion, la cierra y la reabre, y el comprador lo ve con su contador
2. **Tarea 1 (front):** `dfd07b1` feat(04-06): el admin abre una conversacion desde la bandeja, responde, la cierra y la reabre

## Deviations from Plan

None - plan executed exactly as written. Detalle de alcance: los tests de Postgres de la agencia importan tambien `ConversacionService` (con `VerificacionCuenta` y `LimitadorDeIntentos`) para probar de punta a punta el lado del comprador (contador, marcar leida y escribir en una cerrada) sobre la misma base.

## Auth Gates

None.

## Known Stubs

Ninguno.

## Threat Flags

Ninguno. T-04-22 mitigado y probado (el servicio verifica con `existsBy...IdNot` antes de reabrir y el indice unico parcial de V6 queda como defensa final; test de Postgres y del humo), T-04-23 (cada mensaje guarda `autor_id` de la cuenta admin que lo escribio; el test de Postgres lo verifica y el JSON del comprador no trae `autorId`), T-04-24 (`responder` rechaza una CERRADA con 400; el comprador tambien; tests de servicio, de Postgres y del humo). `/api/admin/**` sigue siendo solo ADMIN: 401 sin token y 403 al comprador en el test de seguridad y en el humo. T-04-SC aceptado: sin paquetes nuevos.

## Limitaciones conocidas

- ESLint no tiene `eslint.config.*` en el front (pre-existente): `npm run lint` no corre.
- Cerrar y reabrir no mandan mail (los avisos por mail son 04-07) y la ficha del usuario es 04-08.

## Pendiente de verificacion humana

Human-check de la tarea (no automatizable aca, sin navegador): con el back local y el front de prueba en el 5174, como admin abrir una conversacion desde la bandeja, responder, cerrarla y reabrirla; como comprador (otra ventana) ver llegar la respuesta y el contador en el Navbar (hasta 30 segundos), ver que baja al abrir el hilo y comprobar que en una conversacion cerrada la caja de texto no esta. La pasada completa por navegador es 04-10.

## Self-Check: PASSED

- Archivos creados y modificados verificados en disco (`AdminConversacionPage.jsx`, `ConversacionAdminService.java`, `AdminConversacionController.java` y los tres tests).
- Commits `6cb2144` (back) y `dfd07b1` (front) existen en sus repos; `commits:` medido desde el ledger en disco (1 en el back).
