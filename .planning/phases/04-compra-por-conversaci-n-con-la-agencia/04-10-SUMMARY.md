---
phase: 04-compra-por-conversaci-n-con-la-agencia
plan: 10
subsystem: docs
tags: [documentacion, readme, verificacion, humo, flyway, mensajeria]
status: partial

requires:
  - phase: 04-compra-por-conversaci-n-con-la-agencia
    provides: V6 y V7, la API de conversaciones, los avisos por mail, la ficha del usuario, el borrado en cascada y los humos (04-01 a 04-09)
provides:
  - README del back con el modelo de conversaciones, V6 y V7, la seccion "Mensajeria: conversaciones con la agencia (Fase 4)", la privacidad de la ficha y los tests de la fase
  - README del front con las paginas, el contexto NoLeidosContext, los hooks y utils de mensajeria y como se prueba
  - README de la demo coherente (borrar autos borra conversaciones, no consultas)
  - Verificacion automatica final de la fase en verde (suite del back, build y tests del front, dos humos)
affects: [04-11]

# Commits en dos repos: back (fd1c9f8 mas el de docs de cierre) y front (5dabfe3).
plan_head_before: 40d60f17faa526843d61d8604471d23007cf0406
plan_head_after: fd1c9f8166fe079f0c3aab8a0b580ef357dbaa62

actuals:
  tokens: 6000   # chars/4 sobre el diff de los README (back y front)
  tasks: 1   # la Tarea 1 esta completa; la Tarea 2 queda parcial (automatizado verde, recorrido en navegador pendiente)
  commits: 1   # medido en el back antes del commit de docs de cierre (git rev-list --count plan_head_before..plan_head_after); el front suma 1 commit propio (5dabfe3)

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - README.md
    - scripts/demo/README.md
    - ../danteautomotores-front/README.md

key-decisions:
  - "El plan no queda como completo: el recorrido en navegador (human-check de la Tarea 2) no se hizo porque no hay herramientas de Chrome en esta sesion; se deja como pendiente del usuario y no se da por hecho"
  - "Los requisitos MSG ya figuraban como Complete (marcados por 04-01 a 04-09), asi que este plan no los toca"

requirements-completed: []

duration: una sesion
completed: 2026-10-07
---

# Phase 4 Plan 10: README de los dos repos y verificacion final Summary

**La mensajeria, V6 y V7, la API, los limites asumidos y el humo quedan documentados en los dos repos, y toda la verificacion automatica de la fase pasa (772 tests del back con Postgres real, build y 58 tests del front, y los dos humos con 0 fallas); falta el recorrido en navegador, que queda pendiente del usuario.**

## Accomplishments

- `README.md` del back: **Conversacion** y **Mensaje** reemplazan a la consulta en "Modelo de datos" y el circuito pasa a "Lo quiero abre una conversacion con la agencia". "Base de datos y migraciones" describe V6 y V7 (aditivas, `consultas` queda sin uso ni entidad, la vuelta atras de un deploy es redeployar el back anterior) y manda las proximas migraciones a V8. Nueva seccion "Mensajeria: conversaciones con la agencia (Fase 4)" antes de "Produccion": flujo, tabla de endpoints del comprador y del admin, no leidos, consulta periodica (30 s el contador, 10 s el hilo), cerrar y reabrir, avisos por mail (asunto fijo, sin texto, 1 cada 10 minutos por conversacion y destinatario, tope diario compartido de 250), limite de 20 mensajes cada 10 minutos con 429, privacidad, borrado de autos con aviso, limitaciones conocidas y el comando del humo (con marcador en lugar de contrasena). "Datos personales" dice que la ficha del admin es el unico lugar donde salen DNI y telefono, y "Tests" menciona V1 a V7 y los dos tests de conversaciones.
- `scripts/demo/README.md`: las dos menciones de "consultas" pasan a "conversaciones".
- `README.md` del front: estructura con `MisMensajesPage`, `ConversacionPage`, `admin/AdminMensajesPage`, `admin/AdminConversacionPage`, `admin/AdminUsuarioPage`, `HiloDeMensajes`, `BadgeNoLeidos`, `NoLeidosContext`, `hooks/useSondeo` y `utils`, mas la linea de `npm test` y `npm run build`.
- Se verifico contra el codigo que `GET /api/conversaciones/no-leidas` responde a los dos roles (al admin le cuenta la bandeja) y que los endpoints de la tabla son los reales de los tres controladores.

## Task Commits

1. **Tarea 1: documentacion** - back `fd1c9f8`, front `5dabfe3` (docs)
2. **Tarea 2: verificacion final** - sin commits (no modifica archivos); resultados abajo

## Verificacion

Resultados sin datos personales ni secretos.

| Paso | Resultado |
|------|-----------|
| Tarea 1, comando de docs | imprime `docs actualizadas` |
| Tarea 1, secretos en README | `grep` de variables con valor asignado en los README del back y del front: sin coincidencias |
| Suite completa del back, `mvn -B -o -Djava.version=17 test -Ddante.pg.required=true` | 772 tests, 0 fallas, 0 errores, 0 salteados, BUILD SUCCESS |
| Front, `npm run build` | build correcto |
| Front, `npm test` | 58 tests, 58 ok, 0 fallas |
| Humo de cuentas (`--vacia dante_humo_cuentas`) | `humo: 28 ok, 0 fallas, 0 skip`, 0 lineas FALLA |
| Humo de mensajeria (`--vacia dante_humo_mensajes`, admin de prueba con contrasena aleatoria) | `humo: 49 ok, 0 fallas, 0 skip`, 0 lineas FALLA |

Los dos humos usan bases vacias y descartables que `con-back-local.sh` crea y borra; la base `danteautomotores` no se toco.

## Recorrido en navegador: PENDIENTE (lo tiene que hacer el usuario)

**No se hizo.** En esta sesion no habia herramientas de Chrome ni de navegador, y el recorrido no se simulo ni se da por hecho. Los dos humos cubren por API lo mismo que los criterios (apertura y reuso de la conversacion, hilo, no leidos, avisos de mail en el log, filtros, cerrar y reabrir, ficha, borrado en cascada), pero no la interfaz. Hasta que se haga, la Tarea 2 queda parcial y no se pasa a 04-11.

Como prepararlo:

1. Back local contra una copia de la base de desarrollo, con el origen del front de prueba, y con un admin de prueba por si la copia no trae uno: `ADMIN_EMAIL=<mail de prueba> ADMIN_PASSWORD=<contrasena de prueba> ADMIN_NOMBRE=<nombre> APP_CORS_ALLOWED_ORIGINS="http://localhost:5174" APP_FRONTEND_URL=http://localhost:5174 bash scripts/verify/con-back-local.sh --copia-de danteautomotores <base_descartable> sleep 7200`
2. Front de prueba, en `danteautomotores-front`: `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174` (el `.env` apunta a produccion y no se usa).
3. Dos ventanas (un comprador con cuenta verificada y el admin) y el log del back en `$TMP/dante-back-<base_descartable>.log` para ver los avisos de mail.

Puntos a recorrer y anotar (resultado de cada uno, sin datos personales):

- [ ] **1 (criterio 1).** Visitante sin sesion en la ficha de un auto disponible: "Lo quiero" lleva al login y vuelve a la ficha; con cuenta incompleta lleva a Completa tus datos y vuelve; con cuenta verificada abre el hilo con un primer mensaje que nombra el auto; tocarlo otra vez sobre el mismo auto vuelve a la misma conversacion; "Consultar por este auto" con texto propio tambien abre el hilo.
- [ ] **2 (criterio 2).** Mis mensajes lista la conversacion con foto, auto y estado; escribir dos mensajes, uno con etiquetas HTML (por ejemplo una negrita) que se ve literal.
- [ ] **3 (criterio 3).** El admin ve el contador en el Navbar y en la tarjeta Mensajes del panel; el log del back muestra un aviso de mail para el admin y, tras responder, uno para el comprador, ninguno con el texto del mensaje; el contador del comprador sube en el Navbar (hasta 30 segundos) y baja al abrir el hilo.
- [ ] **4 (criterio 4).** Bandeja del admin: filtros de tipo, estado y solo no leidas, y el paginador si hay mas de 20; abrir la conversacion, responder, cerrarla (el comprador ve el hilo sin caja de texto) y reabrirla.
- [ ] **5 (criterio 5).** "Ver ficha del usuario" desde la conversacion: nombre, telefono, DNI y mail correctos y el historial con links a cada conversacion.
- [ ] **6 (bordes).** Un admin que entra a /mensajes termina en su bandeja; en pantalla angosta el Navbar muestra los iconos de mensajes y perfil con contador; eliminar un auto con conversaciones muestra el aviso con la cantidad; el comprador no abre el hilo de otro (pegar el id de una ajena muestra "No encontramos esta conversacion").

Si algun punto falla, no se sigue a 04-11: se registra el error sin datos personales y se corrige con un plan de cierre de brechas.

## Deviations from Plan

None - la Tarea 1 se ejecuto como estaba escrita. La unica diferencia es de alcance: el `<human-check>` de la Tarea 2 no se pudo ejecutar por falta de herramientas de navegador y queda listado arriba como pendiente (no es una falla del producto).

## Known Stubs

None.

## Threat Flags

None. No se agrego superficie nueva: solo documentacion. T-04-39: los README llevan un marcador en lugar de contrasena y este SUMMARY registra solo resultados. T-04-40: las verificaciones usaron bases descartables creadas y borradas por `con-back-local.sh`.

## Self-Check: PASSED

- README.md, scripts/demo/README.md (back) y README.md (front): modificados y commiteados.
- Commits `fd1c9f8` (back) y `5dabfe3` (front) existen.
