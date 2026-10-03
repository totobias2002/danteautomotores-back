---
phase: 02-cat-logo-p-blico-real-en-producci-n
fixed_at: 2026-10-03T19:10:00-03:00
review_path: .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Fase 2: Informe de correcciones del code review

**Corregido:** 2026-10-03
**Revisión de origen:** .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md
**Iteración:** 1

**Resumen:**
- Hallazgos en alcance: 5 (WR-01, WR-02, WR-04, WR-06, WR-07; alcance acordado con el orquestador)
- Corregidos: 5
- Omitidos: 0
- Fuera de alcance por decisión de producto, no tocados: WR-03 (mezcla ARS/USD en filtros de precio) y WR-05 (afirmaciones comerciales y botones sin conectar). Los IN-01 a IN-10 tampoco se tocaron.

**Verificación:** corrida en el checkout principal (`workflow.use_worktrees=false`, sin worktree). Back: `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test` con el contenedor `danteautomotores-db` en el puerto 5433 da 297 tests, 0 fallas, 0 errores, 0 salteados, BUILD SUCCESS. Front: `npm run build` correcto y `node --test src/utils/catalogoParams.test.js` da 9 de 9 (ver la nota sobre `node --test src/utils/` abajo). No se tocó producción, el `.env` del front ni las bases `danteautomotores` y `dante_uat*`; las pruebas manuales de WR-06 usaron bases descartables `humo_wr06_*`, ya borradas.

## Problemas corregidos

### WR-01: `?pagina=` grande da un 500 en un endpoint público

**Repo:** back
**Archivos modificados:** `src/main/java/com/danteautomotores/service/CatalogoService.java`, `src/test/java/com/danteautomotores/service/CatalogoServiceTest.java`, `scripts/verify/catalogo-humo.js`
**Commit:** f4cbc90
**Fix aplicado:**
- `normalizar` acota la página por arriba además de por abajo: una página fuera de `1..MAX_PAGINA` (constante nueva, 10.000, o sea 240.000 autos) cae a la primera, igual que el comentario del controller ya prometía ("fuera de rango, cae a la primera página"). Con ese tope el offset máximo es 239.976, muy lejos de `Integer.MAX_VALUE`, así que `PageRequest`/Spring Data ya no lanzan `InvalidDataAccessApiUsageException`.
- Se eligió "cae a la primera" (la propuesta del review) y no "última página vacía" para mantener un solo criterio con `pagina=abc` y `pagina=0`.
- Tests: `CatalogoServiceTest.unaPaginaMasAllaDelTopeVaALaPrimeraYNuncaRompeElOffset` (tope + 1, 90.000.000 y `Integer.MAX_VALUE` van a 1; el tope exacto se respeta) y `buscarConUnaPaginaGigantePideLaPrimeraEnVezDeUnOffsetFueraDeInt` (el `Pageable` que llega al repositorio es la página 0 y su offset cabe en un int).
- Humo: `catalogo-humo.js` suma el caso `/publicaciones?pagina=90000000` (200 y `pagina` 1).

### WR-02: filtro con enum o número ilegible muestra el mensaje técnico de Spring

**Repo:** back
**Archivos modificados:** `src/main/java/com/danteautomotores/exception/GlobalExceptionHandler.java`, `src/test/java/com/danteautomotores/controller/PublicacionControllerCatalogoTest.java`, `scripts/verify/catalogo-humo.js`
**Commit:** a5c9574
**Fix aplicado:**
- Se eligió la opción (b) del review, en el `GlobalExceptionHandler`, no tolerar el valor inválido: `handleMethodArgumentNotValid` ahora arma cada mensaje de `campos` con un helper `mensajeDeCampo`. Si el `FieldError` es un fallo de conversión (`isBindingFailure()`), el mensaje es fijo y en español: `El valor indicado para "tipo" no es válido.` (sin nombres de clases Java, sin el texto del `TypeMismatchException` y sin repetir lo que mandó el cliente). Los errores de Bean Validation conservan su mensaje propio.
- Se sigue respondiendo 400 con el formato uniforme `{"error":"Datos inválidos","campos":{campo: mensaje}}`. El front (`mensajeDeError`) ya muestra solo los mensajes de `campos`, así que el usuario ve la frase en español. Cubre `tipo`, `zona`, `estado`, `transmision`, `anioMin`, `anioMax`, `kmMax`, `precioMin`, `precioMax` y `agenciaId`.
- Se descartó la opción (a) (ignorar en silencio el filtro inválido): un `?transmision=automatica` ignorado devolvería el catálogo sin filtrar y el usuario creería que el filtro se aplicó. `orden` y `pagina` siguen tolerándose a propósito.
- Tests: `unFiltroDeEnumInvalidoDa400ConMensajeEnEspanolSinTextoTecnico` (`tipo=NAVE`, `transmision=automatica`, `zona=norte`, `estado=roto`; el cuerpo no contiene "Failed to convert", "java.", "enum" ni el valor enviado, y el servicio nunca se invoca) y `unNumeroIlegibleEnUnFiltroDa400ConMensajeEnEspanolSinTextoTecnico` (los seis filtros numéricos). Se mantiene `unTipoInvalidoDa400ConError`.
- Humo: el caso `tipo=NAVE` de `catalogo-humo.js` además verifica que el cuerpo no filtre texto técnico.
- No hecho (fuera del alcance acordado): la parte del front del review (validar en `leerFiltros` los valores de la URL contra los enums). Con el 400 ahora legible el usuario ve un mensaje claro, pero la página sigue sin resultados hasta que apriete "Limpiar todo".

### WR-04: el detalle marca el favorito aunque la llamada falle; el corazón de la card no hace nada

**Repo:** front
**Archivos modificados:** `src/pages/PublicacionDetallePage.jsx`, `src/components/PublicacionCard.jsx`
**Commit:** 82e34a0
**Estado:** corregido, con una verificación manual pendiente (no hay framework de tests de componentes en el front: solo se probó que el build compila).
**Fix aplicado:**
- Detalle: `alternarFavorito` ya no hace `.catch(() => setFavoritoOk(true))`. El corazón cambia solo cuando el backend confirma: `POST /favoritos/{id}` para guardar y `DELETE /favoritos/{id}` para quitar (antes la función se llamaba "alternar" pero nunca quitaba). Ante cualquier error el corazón queda como estaba y se muestra el motivo con `mensajeDeError` en un `role="alert"` bajo las acciones, con "No se pudo actualizar tus favoritos. Intentá de nuevo." como respaldo. En un 401 no se muestra nada ni se marca nada: se mantiene el comportamiento de sesión existente (el interceptor de `api.js` cierra la sesión y lleva a `/login`); sin sesión previa el botón sigue llevando a `/login`.
- Con sesión, el corazón arranca con el estado real (`GET /favoritos` y comparar el id), para que un auto ya guardado no dé el error "ya está en tus favoritos" al primer clic. Si ese pedido falla, el corazón queda vacío. Se agregaron `aria-pressed`, `aria-label` según el estado y `disabled` mientras la llamada está en curso (evita dobles clics).
- Card: se eligió la opción mínima, eliminar el botón `♡` de `PublicacionCard`. Conectarlo exigiría que cada pantalla que usa la card (Autos, Home, Agencia, Similares, Favoritos) cargue los favoritos y propague el estado, y además sigue siendo un control interactivo dentro de un `<a>`. Guardar y quitar favoritos queda en el detalle y en la pantalla de favoritos. Si se quiere el corazón en el listado, conviene abrirlo como tarea propia, con el botón fuera del `<a>`.
- Residual (no tocado): `FavoritosPage` sigue enlazando a `/publicaciones/{id}` (ruta que el router no define) y usa estilos viejos; no era parte de este hallazgo.

### WR-06: `con-back-local.sh` borra sin avisar cualquier base local preexistente

**Repo:** back
**Archivos modificados:** `scripts/verify/con-back-local.sh`
**Commit:** 2a443b4
**Fix aplicado:**
- En los modos `--copia-de` y `--vacia` el script consulta primero `pg_database`; si la base pedida ya existe, aborta con un mensaje en español (con el comando para borrarla a mano si es descartable) antes de ejecutar nada. Se eliminó el `DROP DATABASE IF EXISTS` previo al `CREATE`.
- `BASE_CREADA=1` se marca recién después de que el `CREATE DATABASE` tuvo éxito, así el `trap` de limpieza solo borra lo que este script creó en esta corrida (en la ruta de respaldo con `pg_dump | pg_restore`, tras el `CREATE` de la base vacía).
- Esto también cubre las bases protegidas (`postgres`, `template0/1`, `dante_uat*`, cualquier base con datos), no solo el nombre `danteautomotores`. No se impuso el prefijo `humo_*` del review porque los planes y el UAT usan `dante_copia_*` y `dante_prueba_*`; "no borrar lo que ya existe" da la misma garantía sin romper esos usos.
- Cabecera del script actualizada para decir lo que hace.
- Verificación con bases descartables: con `humo_wr06_previa` ya existente y con datos, `--vacia` y `--copia-de` abortan con código 1 y la tabla sigue intacta; `--vacia danteautomotores` y `--vacia postgres` se niegan; una corrida normal `--vacia humo_wr06_nueva` (`SALTAR_BUILD=1`, puerto 8093) crea la base, levanta el back, migra V1 a V3, corre el comando con código 0 y borra la base al terminar. Las bases de prueba quedaron borradas.

### WR-07: `AgenciaRequest` no limita el largo ni valida el formato del email

**Repo:** back
**Archivos modificados:** `src/main/java/com/danteautomotores/dto/agencia/AgenciaRequest.java`, `src/test/java/com/danteautomotores/dto/AgenciaRequestValidationTest.java` (nuevo)
**Commit:** 543cb0e
**Fix aplicado:**
- `nombre`, `logo`, `direccion`, `telefonoContacto` y `emailContacto` llevan `@Size(max = 255)` con mensaje en español (coincide con las columnas `varchar(255)`); `emailContacto` suma `@Email` y mensajes en español para `@NotBlank` y `@Email`. `descripcion` queda sin límite porque la columna es `TEXT`. Mismo estilo que `PublicacionRequest`.
- Un dato demasiado largo ahora vuelve como 400 con el campo señalado y no como el 409 engañoso de `DataIntegrityViolationException`.
- Tests nuevos (`AgenciaRequestValidationTest`, 7): request mínimo y completo válidos, nombre y email en blanco, cuatro emails sin formato, textos de 256 caracteres rechazados con mensaje, justo 255 aceptado, y un email bien formado pero de 308 caracteres rechazado por tamaño. `AgenciaControllerTest` y `AgenciaServiceTest` siguen en verde.

## Resultados de las corridas finales

| Corrida | Resultado |
|---------|-----------|
| `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test` (back, con Postgres en 5433) | 297 tests, 0 fallas, 0 errores, 0 salteados, BUILD SUCCESS |
| `npm --prefix ../danteautomotores-front run build` | correcto (vite build) |
| `node --test src/utils/catalogoParams.test.js` (front) | 9 de 9 |

Nota: `node --test src/utils/` (con el directorio) falla en esta máquina con Node 24.14.1 porque trata `src/utils` como un módulo (`Cannot find module ...\src\utils`). Es un problema del comando con esa versión de Node, anterior a estos cambios; pasando el archivo de test explícito corre y pasa. `eslint` tampoco corre en el front porque no hay un `eslint.config.*` (también anterior).

## Para confirmar a mano

- WR-04: con sesión iniciada, abrir un auto, tocar el corazón (se llena), recargar (sigue lleno), tocarlo de nuevo (se vacía y desaparece de Favoritos); con el back apagado, el corazón no debe llenarse y debe aparecer el aviso de error.
- WR-06: sin ejecutar nada destructivo, probar `bash scripts/verify/con-back-local.sh --vacia <base_que_ya_existe> true` y comprobar que aborta.

---

_Corregido: 2026-10-03_
_Corrector: Claude (gsd-code-fixer)_
_Iteración: 1_
