---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 07
subsystem: api
tags: [spring-boot, jpa, transaction-synchronization, cloudinary, react, native-dialog]

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "01-06: FotoPublicacion.publicId, CloudinaryService.eliminar (best-effort), orden = max+1, FotosGrid en el form"
provides:
  - "Eliminar publicacion en cascada (favoritos + consultas + fotos) en una sola transaccion"
  - "GET /api/admin/publicaciones/{id}/impacto-eliminacion con solo {cantidadConsultas, cantidadFavoritos}"
  - "Eliminar foto con resecuenciado 0..n-1 y borrado del asset en Cloudinary post-commit"
  - "ConfirmDialog.jsx (dialog nativo con showModal) usado en el panel y en el form de fotos"
affects: [fase-2-catalogo-publico, fase-4-conversaciones]

actuals:
  tokens: 42000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Efectos externos irreversibles (Cloudinary destroy) solo en TransactionSynchronization.afterCommit; sin transaccion activa se ejecutan en el momento"
    - "Aviso previo a un borrado destructivo: endpoint de conteo dedicado + boton de confirmar deshabilitado hasta tener el conteo"

key-files:
  created:
    - src/main/java/com/danteautomotores/dto/publicacion/ImpactoEliminacionResponse.java
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/components/ConfirmDialog.jsx
  modified:
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/controller/AdminPublicacionController.java
    - src/main/java/com/danteautomotores/repository/ConsultaRepository.java
    - src/main/java/com/danteautomotores/repository/FavoritoRepository.java
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/AdminPublicacionControllerTest.java
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx

key-decisions:
  - "Cascada + aviso (decision del usuario en plan-phase): favoritos y consultas se borran con la publicacion; el dialogo muestra el conteo antes de habilitar Eliminar"
  - "Los assets de Cloudinary se borran en afterCommit para que un rollback no deje filas apuntando a imagenes borradas"
  - "resecuenciarFotos usa el mismo criterio que PublicacionMapper (orden null = 0, desempate por id)"

patterns-established:
  - "ConfirmDialog: dialog nativo controlado por la prop abierto; Esc cancela via onCancel/preventDefault y el padre cierra por estado"

requirements-completed: [ADM-02, ADM-03]

coverage:
  - id: D1
    description: "Eliminar una publicacion con favoritos/consultas ya no falla por la FK: todo se borra en una transaccion (favoritos, consultas, fotos, publicacion), 404 si no existe"
    requirement: "ADM-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java#eliminarBorraFavoritosYConsultasAntesQueLaPublicacion"
        status: pass
    human_judgment: false
  - id: D2
    description: "Cloudinary solo se toca despues del commit, omite fotos sin public_id y no se toca si no hay commit"
    requirement: "ADM-02"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java#eliminarBorraLosAssetsDeCloudinarySoloDespuesDelCommitYOmiteLasFotosSinPublicId"
        status: pass
    human_judgment: false
  - id: D3
    description: "GET impacto-eliminacion devuelve solo dos conteos a ADMIN (403 comprador, 401 sin token, 404 inexistente)"
    requirement: "ADM-02"
    verification:
      - kind: integration
        ref: "src/test/java/com/danteautomotores/controller/AdminPublicacionControllerTest.java#adminConsultaElImpactoDeEliminarUnAuto"
        status: pass
    human_judgment: false
  - id: D4
    description: "Eliminar una foto deja el orden 0..n-1 sin huecos, la portada pasa a la siguiente, 400 si la foto es de otra publicacion"
    requirement: "ADM-03"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java#eliminarLaPortadaHaceQueLaSiguientePaseAOrdenCero"
        status: pass
    human_judgment: false
  - id: D5
    description: "Dialogo de confirmacion en el panel: el admin lee cuantas consultas y favoritos se pierden, Esc cancela, Eliminar se habilita con el conteo; confirmacion de foto con aviso de portada"
    requirement: "ADM-02"
    verification:
      - kind: other
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Flujo visual con dialog nativo, datos reales en Postgres y estado real en Cloudinary; el build solo prueba que compila"

duration: 17min
completed: 2026-10-02
status: complete
plan_head_before: e9531bac036fa056fd332428644fe98e3860dfd8
plan_head_after: 5f038d8555d25453c723efe49a741b51cdfe78b9
commits: 2
---

# Phase 1 Plan 07: Eliminacion segura de publicaciones y fotos Summary

**Borrado en cascada de publicaciones (favoritos y consultas en la misma transaccion) con endpoint de conteo previo, resecuenciado de fotos al borrar y borrado de assets de Cloudinary en afterCommit, mas un ConfirmDialog nativo en el panel.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-10-03T02:27Z
- **Completed:** 2026-10-03T02:44Z
- **Tasks:** 3
- **Files modified:** 10 (8 modificados, 2 creados)

## Accomplishments

- `PublicacionService.eliminar` es `@Transactional`: borra favoritos, consultas y la publicacion (fotos por cascade) y despues registra el borrado de los assets en `afterCommit`; las fotos sin `public_id` se omiten.
- `GET /api/admin/publicaciones/{id}/impacto-eliminacion` devuelve solo `cantidadConsultas` y `cantidadFavoritos` (test por reflexion de que no hay otros campos).
- `eliminarFoto` saca la foto de la coleccion, resecuencia las restantes a 0..n-1 (la portada pasa a la siguiente) y borra el asset post-commit; 400 "La foto no pertenece a esta publicacion" sin efectos.
- Front: `ConfirmDialog.jsx` (dialog nativo, `showModal`, Esc cancela); el panel pide el conteo antes de abrir y deshabilita Eliminar hasta tenerlo; el form de fotos confirma con aviso de portada. Ya no queda `window.confirm` en `pages/admin`.

## Task Commits

1. **Tarea 1: eliminar publicacion en cascada con conteo de impacto** - `2e1059f` (feat, back)
2. **Tarea 2: eliminar foto con resecuenciado y borrado post-commit** - `5f038d8` (feat, back)
3. **Tarea 3: ConfirmDialog y flujos de borrado del panel** - `52e2df1` (feat, repo danteautomotores-front)

**Plan metadata:** commit docs(01-07) en el back (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

- `src/main/java/com/danteautomotores/service/PublicacionService.java` - eliminar en cascada, obtenerImpactoEliminacion, eliminarFoto + resecuenciarFotos, eliminarImagenesDespuesDelCommit
- `src/main/java/com/danteautomotores/controller/AdminPublicacionController.java` - endpoint impacto-eliminacion
- `src/main/java/com/danteautomotores/repository/{Consulta,Favorito}Repository.java` - deleteByPublicacionId, countByPublicacionId
- `src/main/java/com/danteautomotores/dto/publicacion/ImpactoEliminacionResponse.java` - dos conteos
- `danteautomotores-front/src/components/ConfirmDialog.jsx` - dialogo reutilizable
- `danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx` - pedirEliminacion/confirmarEliminacion con conteo
- `danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx` - fotoAEliminar + dialogo unico

## Decisions Made

- Se respeto la decision de usuario "cascada + aviso" sin agregar checkpoint.
- Un rollback no toca Cloudinary: el borrado de assets se registra como `TransactionSynchronization`; sin sincronizacion activa se ejecuta de inmediato (cubierto por test).

## Deviations from Plan

None - plan executed exactly as written. Notas menores sin impacto en el comportamiento:

- Las dos tareas del back se commitearon cada una con tests + codigo en un solo commit (no hubo commit RED separado, la RED era un error de compilacion de las clases nuevas).
- En el test se uso `delete(any(Publicacion.class))` porque `delete(any())` es ambiguo entre `CrudRepository` y `JpaSpecificationExecutor`.
- Tras compilar con JDK 17 hubo que borrar `target/classes` y `target/test-classes` (problema conocido "class file version 65.0").

**Total deviations:** 0 auto-fixed. **Impact:** none.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. El unico endpoint nuevo (`impacto-eliminacion`) esta bajo `/api/admin/**` y esta cubierto por T-01-26.

## Verificacion pendiente (humana)

Con back, Postgres, Cloudinary y front corriendo: marcar un auto como favorito y dejarle una consulta como comprador; como admin eliminarlo (leer el dialogo, cancelar con Esc, volver a eliminar y confirmar, recargar); eliminar la portada de un auto con 3 fotos y recargar; revisar en Cloudinary que los assets borrados ya no estan.

## Next Phase Readiness

Fase 1 sin planes pendientes tras este (7 de 7). Queda la verificacion de fase / UAT. Suite completa del back: 124 tests en verde; build del front en verde.

## Self-Check: PASSED

- Archivos creados existen (ImpactoEliminacionResponse.java, ConfirmDialog.jsx).
- Commits 2e1059f y 5f038d8 (back) y 52e2df1 (front) existen.
- Criterios de aceptacion de las 3 tareas verificados con grep/build.
