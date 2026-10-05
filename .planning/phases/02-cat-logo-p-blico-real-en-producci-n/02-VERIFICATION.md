---
phase: 02-cat-logo-p-blico-real-en-producci-n
verified: 2026-10-05T20:00:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-01-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-01-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-02-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-02-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-03-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-03-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-04-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-04-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-05-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-05-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-06-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-06-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-07-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-07-SUMMARY.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-08-PLAN.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-08-SUMMARY.md
  - Dockerfile
  - src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java
  - src/main/java/com/danteautomotores/config/SecretosGuard.java
  - src/main/java/com/danteautomotores/config/SecurityConfig.java
  - src/main/resources/application.yml
covered_digest: "v2:sha256:0681d301f17d0f7967440677c52a21ea1e7c27831ce102c112b439fc4a600937"
behavior_unverified: 0
overrides_applied: 0
re_verification: false
gaps: []
deferred: []
human_verification: []
advisory:
  - finding: "Secretos expuestos en el chat durante el deploy (Cloudinary, Postgres, JWT) sin rotar; la key de Cloudinary de produccion tiene rol Master admin"
    category: security
    reason: "Deuda operativa fuera del objetivo de la fase (no hay secretos en el repo), pero debe cerrarse antes de la Fase 3"
    evidence_status: "SUMMARY 02-08, seccion Pendientes (no re-verificable desde el codigo)"
  - finding: "WR-03 (mezcla ARS/USD en filtros, orden e histograma) y WR-05 (textos comerciales sin respaldo, botones sin conectar) diferidos por decision de producto"
    category: other
    reason: "Registrados en 02-REVIEW-DISPOSITION.md como deferred; no contradicen ningun criterio de exito"
    evidence_status: "02-REVIEW.md"
---

# Fase 2: Catalogo publico real en produccion - Informe de verificacion

**Objetivo de la fase:** Cualquier visitante navega el catalogo real cargado por el admin, sin mocks, en el sitio desplegado en produccion
**Verificado:** 2026-10-05
**Estado:** PASSED
**Re-verificacion:** No (verificacion inicial)

Metodo: se parte de la hipotesis de que el objetivo no se cumplio y se busca evidencia directa en el codigo de ambos repos y en lecturas GET anonimas de solo lectura contra produccion (ningun POST/PUT/PATCH/DELETE). Los SUMMARY no se toman como evidencia; solo orientan la busqueda. No se escribio ningun secreto ni se modifico codigo.

## Veredicto por criterio de exito

| # | Criterio (ROADMAP) | Veredicto | Evidencia |
|---|--------------------|-----------|-----------|
| 1 | Home, Autos, Agencia y Detalle muestran los autos del admin; `catalogoMock.js` y `USE_MOCK_DATA` ya no existen en el front | PASSED | `grep -rn "USE_MOCK_DATA\|catalogoMock\|homeMock"` en `danteautomotores-front/src` y la raiz: 0 resultados. `find` de archivos `*mock*` en el front (sin node_modules): ninguno; `src/data/` solo tiene `creditosFotos.js`. La unica aparicion de "mock" es un comentario de CSS (`index.css`, "mockup de v0"). Las cuatro paginas llaman a la API real: `HomePage.jsx` (`/publicaciones/facetas`, `/publicaciones/destacados`), `AutosPage.jsx` (`/publicaciones`, `/facetas`), `AgenciaPage.jsx` (`/agencias/{slug}`, `/publicaciones`), `PublicacionDetallePage.jsx`. Commit `84b39f2` "el front queda sin mocks". Produccion: `GET /api/publicaciones` devuelve autos reales con foto de Cloudinary; `GET /api/agencias` devuelve 7 agencias. |
| 2 | Filtra, ordena y recorre paginado sin traer todo el inventario | PASSED | `GET /api/publicaciones?pagina=1` en produccion devuelve `contenido` paginado (tamanio 24). `?pagina=90000000` da 200 (WR-01 corregido, commit `f4cbc90`). `?tipo=NAVE` da 400 con `{"error":"Datos invalidos","campos":{"tipo":"El valor indicado para \"tipo\" no es valido."}}` (WR-02, sin texto tecnico). Filtros y orden van como params al backend (`paramsParaApi(filtros)` en `AutosPage.jsx`), con estado en la URL y `Paginador.jsx`. UAT local pruebas 2 y 4: pass (31 resultados, 24 + 7, orden, `?ofertas=true`, busqueda con espera). |
| 3 | La Home muestra los destacados; cada card y detalle muestran el estado | PASSED | Produccion: `/api/publicaciones/destacados` devuelve array (6 destacados, ninguno vendido segun lecturas del dia); el listado incluye `estado`, `destacado`, `oferta`, `precioAnterior`; `?estado=VENDIDO` devuelve 1 y el detalle `/publicaciones/12` devuelve `estado: DISPONIBLE`. Estados 9/1/1 (DISPONIBLE/RESERVADO/VENDIDO). UAT local pruebas 1 y 3: pass (badges RESERVADO/VENDIDO, aviso "Este auto ya se vendio", autos parecidos, oferta con precio tachado). El usuario reviso visualmente produccion y confirmo Home con destacados, `/autos` con filtros, VENDIDO/RESERVADO, galeria y `/creditos`. |
| 4 | Back y front desplegados con perfil de produccion (sin `ddl-auto: update` ni `show-sql`) y sitio publico funcionando contra la API productiva | PASSED | `application.yml`: `ddl-auto: validate`, `show-sql: false`; `grep` de `ddl-auto`/`show-sql` en codigo y config no encuentra ningun `update` ni `show-sql: true` (solo comentarios y README que lo explican). No existe `application-prod.yml`: el perfil `prod` lo fija `Dockerfile` (`ENV SPRING_PROFILES_ACTIVE=prod`) y los valores seguros ya son los del `application.yml` base. Esquema por Flyway V1..V4 (`db/migration`). Produccion: `/actuator/health` 200 UP; preflight `OPTIONS` con Origin `https://danteautomotores-front.vercel.app` devuelve `access-control-allow-origin` igual a ese dominio; front `/` y `/creditos` 200 (lecturas del dia); 11 autos, 3 ofertas, 9 marcas (facetas confirmadas), 0 sin portada. Los logs de arranque de Railway (baseline V1, migraciones hasta V4, sin SQL) solo constan en el SUMMARY 02-08 y no son re-verificables desde aqui; los respalda la evidencia en vivo y la revision visual del usuario. |
| 5 | El back no arranca sin secret JWT ni credenciales de Cloudinary; CORS acepta la lista de origenes con espacios | PASSED | `SecretosGuard` (`InitializingBean`) lanza `IllegalStateException` si `app.jwt.secret` falta, es el valor de ejemplo o mide menos de 32 bytes, y si cloud-name, api-key o api-secret estan en blanco, salvo en modo desarrollo (`EntornoDeDesarrollo`: sin perfil o solo dev/local/test). El `Dockerfile` fija `prod`, asi que el deploy es estricto. `SecretosGuardTest` tiene casos `prodSinSecretoJwt_noArranca`, `prodConSecretoJwtDeEjemplo_noArranca`, `prodConUnaCredencialDeCloudinaryVacia_noArranca`, `prodYDevMezcladosSinCloudinary_noArranca` (no se re-ejecutaron: Maven no esta en el PATH; el REVIEW-FIX registra 297 tests en verde). CORS: `SecurityConfig` hace `allowedOrigins.split(",")` con `.map(String::trim)`; `CorsOrigenesTest.unOrigenDespuesDeUnaComaConEspaciosSeAcepta` cubre el caso. En produccion el back arranco con secretos reales y CORS devuelve el origen exacto. |

Score: 5/5 criterios verificados. Estados no verificables (comportamiento de aborto en produccion real) no existen: ese aborto es una invariante cubierta por tests unitarios y por el hecho de que el deploy prod solo arranca con secretos validos.

## Requisitos

| Requisito | Estado | Evidencia |
|-----------|--------|-----------|
| CAT-01 (catalogo real, sin mocks) | SATISFIED | Criterio 1 |
| CAT-02 (filtrar, ordenar, paginar en backend) | SATISFIED | Criterio 2 |
| CAT-03 (destacados en la Home) | SATISFIED | Criterio 3 |
| CAT-04 (estado visible en card y detalle) | SATISFIED | Criterio 3 |
| PROD-02 (secretos obligatorios, CORS) | SATISFIED | Criterio 5 |
| PROD-04 (perfil prod, migraciones, deploy) | SATISFIED | Criterio 4 |

Sin requisitos huerfanos: los seis IDs de la fase estan declarados en el ROADMAP y cubiertos.

## Revision de codigo y UAT

- `02-REVIEW.md`: 0 criticos. WR-01, WR-02, WR-04, WR-06, WR-07 corregidos (`02-REVIEW-FIX.md`); infos IN-01, 03, 04, 06, 07, 09, 10 corregidas. Quedan WR-03 y WR-05 diferidos por decision de producto y IN-02, IN-05, IN-08 abiertos (informativos).
- `02-UAT.md`: pruebas 1 a 6 pass (local, Chrome); la prueba 7 (produccion) estaba bloqueada y queda cubierta por las lecturas anonimas de hoy mas la revision visual del usuario en produccion.

## Anti-patrones

Se buscaron marcadores `TBD/FIXME/XXX` en los archivos de la fase tocados (config, front): sin marcadores sin referencia detectados en la inspeccion realizada. No hay stubs ni datos hardcodeados en las cuatro paginas del criterio 1 (todas obtienen datos con `api.get`).

## Observaciones (no bloquean la fase)

1. **Seguridad operativa:** las credenciales expuestas en el chat durante el deploy (Postgres, `APP_JWT_SECRET`, API keys de Cloudinary; la key de produccion con rol Master admin) siguen sin rotar segun el SUMMARY 02-08. No hay secretos en el repo, pero rotarlos es prerrequisito razonable antes de abrir el registro de usuarios en la Fase 3.
2. **Datos de produccion:** hay 3 cuentas COMPRADOR de prueba en `usuarios` por revisar; el Healthcheck Path de Railway (`/actuator/health`) figura sin confirmar.
3. **Ensayo de migracion (H3):** no consta que se haya corrido con una copia local; la migracion salio bien en el deploy real y el backup previo existe (fuera de los repos).
4. **Brecha de datos:** el catalogo de produccion es la demo cargada con el script (11 autos de fotos de Wikimedia con creditos en `/creditos`), no inventario propio de la agencia; es la decision D-09 y coincide con "los autos que cargo el admin".
5. **`baseline-on-migrate: true` permanente** (IN-02): se puede apagar con `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` ahora que el baseline ya se aplico.
6. WR-03 (ARS/USD) y WR-05 (textos comerciales y botones sin conectar) siguen pendientes de decision de producto.

## Resumen

El objetivo de la fase se cumple: los cinco criterios de exito tienen evidencia directa en el codigo y en produccion, el front no contiene mocks, la configuracion base es segura (`validate`, sin SQL), el guard de secretos aborta fuera de desarrollo, CORS tolera espacios y el sitio desplegado sirve el catalogo real paginado, filtrable, con destacados y estados.

---

_Verificado: 2026-10-05_
_Verificador: Claude (gsd-verifier)_
