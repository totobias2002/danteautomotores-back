---
phase: 01-gesti-n-del-inventario-por-el-admin
plan: 06
subsystem: api
tags: [spring-boot, cloudinary, multipart, react, fotos, validacion, admin-panel]
status: complete

requires:
  - phase: 01-gesti-n-del-inventario-por-el-admin
    provides: "01-02: application.yml con multipart; 01-04: ServicioExternoException -> 502 y mensajeDeError; 01-05: PublicacionService/PublicacionController y sus tests"
provides:
  - "ImagenValidator (@Component): content-type permitido + magic bytes JPEG/PNG/WebP, tope de 10 MB, constantes MAX_BYTES y MAX_FOTOS=10"
  - "CloudinaryService.subir(MultipartFile) -> ImagenSubida(url, publicId) y eliminar(publicId) best-effort (01-07 lo usa al borrar fotos); se elimina subirImagen"
  - "FotoPublicacion.publicId (columna public_id, nullable: las fotos viejas siguen funcionando)"
  - "PublicacionService.agregarFoto valida y chequea el tope antes de subir, orden = max + 1"
  - "PUT /api/publicaciones/{id}/fotos/orden con ReordenarFotosRequest (lista completa de ids; la primera es la portada)"
  - "Config: multipart max-request-size 12MB y server.tomcat.max-swallow-size 50MB"
  - "Front: prevalidacion por archivo, errores 'nombre: motivo', flechas y Hacer portada visibles en mobile"
affects: [01-07, fase-2-catalogo]

requirements-completed: [ADM-03]

actuals:
  tokens: 36000
  tasks: 3
  commits: 2
plan_head_before: 5b3eb24606bac5a3b21886f09b3ddae00458f7a8
plan_head_after: fffe00ed9f08767f6a5455e6a51d4f9d998ac552

tech-stack:
  added: []
  patterns:
    - "Validacion de archivo por firma binaria (magic bytes) ademas del Content-Type, en un @Component aparte del servicio"
    - "Servicio externo envuelto: IOException | RuntimeException del SDK -> ServicioExternoException con mensaje fijo en espanol (502); el detalle queda en la causa"
    - "Reorden por lista completa de ids, validada contra el conjunto de fotos de la publicacion"

key-files:
  created:
    - src/main/java/com/danteautomotores/service/ImagenValidator.java
    - src/main/java/com/danteautomotores/dto/publicacion/ReordenarFotosRequest.java
    - src/test/java/com/danteautomotores/service/ImagenValidatorTest.java
    - src/test/java/com/danteautomotores/service/CloudinaryServiceTest.java
  modified:
    - src/main/java/com/danteautomotores/service/CloudinaryService.java
    - src/main/java/com/danteautomotores/service/PublicacionService.java
    - src/main/java/com/danteautomotores/controller/PublicacionController.java
    - src/main/java/com/danteautomotores/entity/FotoPublicacion.java
    - src/main/resources/application.yml
    - src/test/java/com/danteautomotores/service/PublicacionServiceTest.java
    - src/test/java/com/danteautomotores/controller/PublicacionControllerTest.java
    - C:/Users/toto/Desktop/work/danteautomotores-front/src/pages/admin/AdminPublicacionFormPage.jsx

key-decisions:
  - "El validador acepta cualquiera de las tres firmas con cualquiera de los tres content-types permitidos (un PNG declarado como image/jpeg pasa); lo que se bloquea es lo que no es JPEG/PNG/WebP real"
  - "Flechas + 'Hacer portada' en vez de drag & drop (sin dependencias, mejor en mobile); la foto con orden 0 es la portada"
  - "Un reorden que se cruza con una subida nueva falla con 400 (falta el id nuevo) en vez de dejar un orden corrupto"

patterns-established:
  - "FotosGrid recibe onMover/onHacerPortada/reordenando; los botones de cada foto usan opacity-100 sm:opacity-0 sm:group-hover:opacity-100 sm:focus-within:opacity-100"

coverage:
  - deliverable: "Solo se aceptan JPEG/PNG/WebP reales, hasta 10 MB; un ejecutable con Content-Type image/jpeg se rechaza"
    verification:
      - kind: test
        ref: "src/test/java/com/danteautomotores/service/ImagenValidatorTest.java"
        status: pass
    human_judgment: false
  - deliverable: "Cloudinary devuelve url y public_id; errores del SDK llegan como ServicioExternoException sin texto interno; eliminar es best-effort"
    verification:
      - kind: test
        ref: "src/test/java/com/danteautomotores/service/CloudinaryServiceTest.java"
        status: pass
    human_judgment: false
  - deliverable: "Tope de 10 fotos y validacion antes de subir, orden max+1, publicId guardado, reorden con 400 ante lista incompleta/duplicada/ajena, PUT admin-only"
    verification:
      - kind: test
        ref: "src/test/java/com/danteautomotores/service/PublicacionServiceTest.java"
        status: pass
      - kind: test
        ref: "src/test/java/com/danteautomotores/controller/PublicacionControllerTest.java"
        status: pass
    human_judgment: false
  - deliverable: "Tomcat responde 413 en vez de cortar la conexion con archivos de hasta 50 MB (max-swallow-size)"
    verification: []
    human_judgment: true
    rationale: "MockMvc no aplica los limites de Tomcat; requiere el back real corriendo (human-check del plan)"
  - deliverable: "Form de fotos: prevalidacion, errores por archivo, flechas, portada, botones visibles en mobile"
    verification:
      - kind: command
        ref: "npm --prefix danteautomotores-front run build"
        status: pass
    human_judgment: true
    rationale: "Interaccion visual/tactil con Cloudinary real; el front no tiene runner de tests. Pendiente el human-check de la Tarea 3"

duration: ~25 min
completed: 2026-10-03
---

# Phase 1 Plan 06: Fotos validadas, public_id de Cloudinary y reorden con portada Summary

**Subida de fotos con validacion de tipo real por magic bytes, tope de 10 MB y 10 fotos por auto, `public_id` de Cloudinary guardado, `PUT /fotos/orden` y un form del panel con errores por archivo, flechas y "Hacer portada".**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3 (Tareas 1 y 2 con tests primero; Tarea 3 front)
- **Files modified:** 12 (4 nuevos en el back, 7 modificados en el back, 1 en el front)

## Accomplishments

- `ImagenValidator`: vacio -> tamano -> content-type -> firma binaria (`readNBytes(12)`), con mensajes en espanol. Un `MZ...` con `image/jpeg` se rechaza con "El archivo no es una imagen valida (JPG, PNG o WebP)".
- `CloudinaryService.subir` manda `folder`, `resource_type=image` y `allowed_formats=jpg,png,webp` y devuelve `secure_url` y `public_id` literales; cualquier `IOException`/`RuntimeException` (incluido "Must supply api_key") sale como `ServicioExternoException` con mensaje fijo (502). `eliminar` nunca propaga.
- `agregarFoto`: valida y chequea el tope antes de llamar a Cloudinary, guarda `url` + `publicId` y toma `orden = max + 1` (null cuenta como -1), con lo que no se repiten ordenes tras borrar una foto del medio.
- `reordenarFotos` + `PUT /api/publicaciones/{id}/fotos/orden`: la lista debe ser exactamente el conjunto de fotos de la publicacion; si no, 400. La seguridad la cubre el matcher PUT existente (ADMIN).
- `application.yml`: `max-request-size: 12MB` y `server.tomcat.max-swallow-size: 50MB` (acotado, nunca -1).
- Front: `accept` limitado a JPG/PNG/WebP, `motivoRechazo` antes de subir, subida de a una foto con try/catch propio (una mala no corta a las demas), errores `nombre: motivo` con `whitespace-pre-line`, `guardarOrden`/`moverFoto`/`hacerPortada`, badge "Portada" y botones visibles sin hover en pantallas tactiles.
- Tests: ImagenValidatorTest (15), CloudinaryServiceTest (10), 10 casos nuevos en PublicacionServiceTest y 3 en PublicacionControllerTest. Suite completa del back: 107 tests verdes. Build del front verde.

## Task Commits

1. **Tarea 1: validador de imagenes y CloudinaryService con url y public_id** - `ea0a424` (feat, back)
2. **Tarea 2: agregarFoto valida, tope de 10, public_id y PUT de reorden** - `fffe00e` (feat, back)
3. **Tarea 3: form de fotos con prevalidacion, errores por archivo, flechas y portada** - `3e29754` (feat, repo danteautomotores-front)

`commits: 2` mide solo el repo back (`rev-list` desde `plan_head_before`, antes del commit del SUMMARY); el commit del front es del repo hermano. Los tests de cada tarea se escribieron primero y se vio el fallo (error de compilacion por clases inexistentes) pero se commitearon junto con la implementacion para no dejar el arbol sin compilar; el plan es `type: execute`, no `type: tdd`, asi que no hay commit RED separado.

## Files Created/Modified

Ver `key-files` en el frontmatter.

## Decisions Made

Ver `key-decisions`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug en mi test] Caso "PNG declarado como JPEG" descartado**
- **Found during:** Tarea 1
- **Issue:** escribi un test que esperaba rechazar un PNG real con `image/jpeg`, pero el comportamiento especificado es "firma valida de cualquiera de los tres formatos"; el PNG es una imagen valida
- **Fix:** se elimino ese test (no estaba en `<behavior>`); el validador no cambio
- **Commit:** `ea0a424`

**2. [Rule 3 - criterio de aceptacion] Forma del codigo para los grep**
- **Issue:** `grep -c 'readNBytes(12)'` pedia el literal; se usaba una constante `BYTES_DE_FIRMA`. Y `grep -c 'motivoRechazo('` pedia >= 2 y la definicion como `const motivoRechazo = (` no contaba
- **Fix:** `readNBytes(12)` inline con comentario; `function motivoRechazo(archivo)` en el front. Mismo comportamiento
- **Commit:** `ea0a424`, `3e29754`

**3. [Rule 2 - documentacion de setup] 01-USER-SETUP.md ampliado**
- El plan declara `user_setup` de Cloudinary; se agrego la seccion con las tres variables `CLOUDINARY_*` y la verificacion manual a `01-USER-SETUP.md` (ya existia por 01-02)

**Total deviations:** 3 menores. **Impact:** ninguno funcional.

## Issues Encountered

- Un IDE compilaba con JDK 21 en paralelo y produjo "class file version 65.0" al correr con JDK 17; se resolvio con `rm -rf target/classes target/test-classes` (previsto por el orquestador).
- La rama de trabajo es `main` (ejecucion secuencial, `branching_strategy: none`); la asercion de rama protegida se omitio por instruccion del orquestador.
- Pendiente el `human-check` de la Tarea 3 (necesita Cloudinary real, los limites de Tomcat y prueba tactil/responsive); queda para la verificacion de fase.

## Known Stubs

None.

## Threat Flags

None. Los cuatro threats del plan (T-01-20..23) quedan mitigados como estaban previstos; no aparecio superficie nueva.

## Authentication Gates

None.

## Self-Check: PASSED

- Archivos creados verificados en disco; commits `ea0a424`, `fffe00e` (back) y `3e29754` (front) existen.
- Criterios de aceptacion de las tres tareas re-verificados (grep y tests) y suite completa del back verde (107 tests).
