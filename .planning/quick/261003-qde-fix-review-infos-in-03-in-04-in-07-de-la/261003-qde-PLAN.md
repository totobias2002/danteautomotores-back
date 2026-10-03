---
phase: quick-261003-qde
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/com/danteautomotores/entity/Publicacion.java
  - src/main/java/com/danteautomotores/mapper/PublicacionMapper.java
  - src/main/java/com/danteautomotores/service/PublicacionService.java
  - src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java
  - src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java
  - ../danteautomotores-front/src/pages/NoEncontradaPage.jsx
  - ../danteautomotores-front/src/routes/AppRouter.jsx
  - ../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx
  - README.md
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md
autonomous: true
requirements: [CAT-01, ADM-03, PROD-04]

estimate:
  tokens: 45000
  raw_tokens: 45000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "La portada de la card del listado (fotoPortada de toResumen) es siempre la misma foto que encabeza fotos en el detalle (toResponse), aunque dos fotos empaten en orden (incluido orden null) y sin importar el orden fisico en que PostgreSQL devuelve las filas (IN-04)"
    - "El orden de las fotos es total y lo define un unico comparador, PublicacionMapper.ORDEN_DE_FOTOS: orden ascendente con null contado como 0 y desempate por id ascendente (id null al final); lo usan toResponse, toResumen y el resecuenciado al borrar una foto (IN-04)"
    - "La coleccion fotos de Publicacion se carga con @OrderBy(\"orden ASC, id ASC\") (IN-04)"
    - "La miniatura del listado del panel admin es la primera foto que entrega el back, la misma que la card publica, el detalle y el formulario de edicion (IN-04)"
    - "Una URL inexistente del front (por ejemplo /auto/12) muestra, entre el Navbar y el Footer, una pagina 404 con el estilo del sitio y links a la Home y a /autos, sin mostrar la ruta pedida (IN-07)"
    - "El README del back ya no se contradice: dice que sin Postgres los tests de Postgres se saltean y el build queda verde, que con -Ddante.pg.required=true son obligatorios y fallan sin Postgres, que asi se corren antes de cada deploy, y no tiene rutas personales (IN-03)"
    - "02-REVIEW-DISPOSITION.md tiene IN-03, IN-04 e IN-07 en fixed con Source quick 261003-qde y el contador open en 7"
  artifacts:
    - path: "src/main/java/com/danteautomotores/mapper/PublicacionMapper.java"
      provides: "ORDEN_DE_FOTOS (Comparator<FotoPublicacion>) usado por toResponse y toResumen"
      contains: "ORDEN_DE_FOTOS"
    - path: "src/main/java/com/danteautomotores/entity/Publicacion.java"
      provides: "@OrderBy(\"orden ASC, id ASC\") en la coleccion fotos"
      contains: "@OrderBy(\"orden ASC, id ASC\")"
    - path: "src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java"
      provides: "Tests de portada estable con empates, orden explicito e id null"
    - path: "src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java"
      provides: "Test contra Postgres real: portada del listado == primera foto del detalle con empate null/0"
    - path: "../danteautomotores-front/src/pages/NoEncontradaPage.jsx"
      provides: "Pagina 404 con links a / y /autos"
    - path: "../danteautomotores-front/src/routes/AppRouter.jsx"
      provides: "Ruta catch-all path=\"*\" como ultima Route"
      contains: "path=\"*\""
    - path: "README.md"
      provides: "Seccion Tests coherente con -Ddante.pg.required=true y paso previo a cada deploy"
      contains: "dante.pg.required=true"
    - path: ".planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md"
      provides: "IN-03, IN-04, IN-07 fixed; open: 7"
  key_links:
    - from: "src/main/java/com/danteautomotores/mapper/PublicacionMapper.java (toResumen.fotoPortada)"
      to: "src/main/java/com/danteautomotores/mapper/PublicacionMapper.java (toResponse.fotos)"
      via: "ambos usan ORDEN_DE_FOTOS: min() y sorted() con el mismo comparador"
      pattern: "ORDEN_DE_FOTOS"
    - from: "src/main/java/com/danteautomotores/service/PublicacionService.java (resecuenciarFotos)"
      to: "src/main/java/com/danteautomotores/mapper/PublicacionMapper.java"
      via: "PublicacionMapper.ORDEN_DE_FOTOS reemplaza el comparador local"
      pattern: "PublicacionMapper.ORDEN_DE_FOTOS"
    - from: "../danteautomotores-front/src/routes/AppRouter.jsx"
      to: "../danteautomotores-front/src/pages/NoEncontradaPage.jsx"
      via: "<Route path=\"*\" element={<NoEncontradaPage />} />"
      pattern: "NoEncontradaPage"
    - from: "../danteautomotores-front/src/pages/admin/AdminDashboardPage.jsx (fotoDePortada)"
      to: "fotos de GET /api/admin/publicaciones (ordenadas por ORDEN_DE_FOTOS)"
      via: "fotoDePortada devuelve fotos[0]"
      pattern: "return fotos\\[0\\]"
---

<objective>
Cerrar tres hallazgos info del code review de la Fase 2 (`.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md`, secciones IN-03, IN-04 e IN-07) y registrarlos como resueltos en la disposicion del review.

- IN-04 (back, con alineacion en el panel admin del front): las fotos no tienen orden total y la portada de la card puede diferir de la primera foto del detalle. Se define un unico comparador total y se agrega `@OrderBy` a la coleccion, con tests.
- IN-07 (front): el router no tiene ruta de captura; una URL inexistente deja el `<main>` vacio. Se agrega una pagina 404 simple en el estilo del sitio.
- IN-03 (back, solo README): el README dice en una seccion que los tests no usan Postgres y en otra que si; se aclara cuando se saltean y cuando son obligatorios (`-Ddante.pg.required=true`), y se saca la ruta personal de Maven.
- Al final, `02-REVIEW-DISPOSITION.md`: IN-03, IN-04 e IN-07 pasan a `fixed` (Source `quick 261003-qde`) y `open:` baja de 10 a 7.

Purpose: el sitio queda publicado en esta fase; la portada tiene que ser la misma en todas las vistas, un link roto no puede mostrar una pagina vacia y un `mvn test` verde sin Postgres no puede pasar por una verificacion de migraciones.
Output: comparador unico de fotos + `@OrderBy` + tests (back), `NoEncontradaPage` + ruta `*` + miniatura del panel alineada (front), README corregido y disposicion actualizada (back).

Repos y rutas: son dos repos git. Back = `C:/Users/toto/Desktop/work/danteautomotores-back` (aca viven el plan, el README y la disposicion). Front = `C:/Users/toto/Desktop/work/danteautomotores-front`. En el cuerpo del plan cada ruta es relativa a la raiz del repo que indica la tarea, y cada `<automated>` asume como cwd la raiz de ese repo. En el frontmatter las rutas son relativas al back (el front como `../danteautomotores-front/`). Los commits de codigo del front van en el repo front (`git -C C:/Users/toto/Desktop/work/danteautomotores-front commit ...`); el resto en el back.

Cobertura de fuentes (las tres IN pedidas + la disposicion):

| Fuente | Item | Tarea |
|--------|------|-------|
| 02-REVIEW.md | IN-04 portada sin orden total (Publicacion.fotos + PublicacionMapper) | Task 1 (back) + Task 2 parte B (panel admin) |
| 02-REVIEW.md | IN-07 sin ruta catch-all en AppRouter | Task 2 parte A |
| 02-REVIEW.md | IN-03 README contradictorio + ruta personal | Task 3 parte A |
| Pedido del usuario | Disposicion: IN-03/04/07 fixed, open: 7 | Task 3 parte B |

Fuera de alcance (no tocar): el resto de los hallazgos IN-01, IN-02, IN-05, IN-06, IN-08, IN-09, IN-10 y los WR; `02-08-PLAN.md` (el paso previo al deploy queda documentado en el README); el `.env` del front; la base `danteautomotores`; push y produccion.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md
@.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md

<interfaces>
Back (verificado leyendo el codigo):
- `src/main/java/com/danteautomotores/entity/Publicacion.java` lineas 95-97: `@OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default private List<FotoPublicacion> fotos = new ArrayList<>();` sin `@OrderBy`. El archivo importa `jakarta.persistence.*` (incluye `jakarta.persistence.OrderBy`).
- `src/main/java/com/danteautomotores/entity/FotoPublicacion.java`: `Long id`, `String url`, `Integer orden` (puede ser null en fotos viejas), `String publicId`; tiene `@Builder`.
- `src/main/java/com/danteautomotores/mapper/PublicacionMapper.java`: clase utilitaria con constructor privado. `toResponse` (lineas 48-51) ordena con `Comparator.comparing(f -> orden null = 0)` sin desempate; `toResumen` (lineas 78-81) usa `min` con el mismo criterio sin desempate. Con empate, el resultado depende del orden de la lista (stream estable / `min` se queda con el primero).
- `src/main/java/com/danteautomotores/service/PublicacionService.java` lineas 293-303, `resecuenciarFotos`: ya ordena por orden (null = 0) y despues por id con `Comparator.nullsLast(Comparator.naturalOrder())`; su comentario dice que es "el mismo criterio de orden que PublicacionMapper", que hoy no es cierto. `java.util.Comparator` (linea 38) solo se usa ahi. `PublicacionMapper` ya esta importado (linea 16).
- `FotoResponse` (`dto/publicacion/FotoResponse.java`): `@Data @Builder` con `id`, `url`, `orden`. `PublicacionResponse.getFotos()` es `List<FotoResponse>`; `PublicacionResumenResponse.getFotoPortada()` es la URL o null.
- `src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java` extiende `PostgresLocalTestBase` (`@DataJpaTest`, transaccional) y ya tiene `fotoPortadaEsLaDeMenorOrdenYNullSiNoHayFotos` (lineas 130-146) como modelo: crea un auto con `auto(nombre, destacado, estado, fecha)`, guarda fotos con `fotoPublicacionRepository.save(FotoPublicacion.builder()...)`, hace `em.flush(); em.clear();` y lee `catalogoService.destacados(6)`.
- Los tests de Postgres usan bases descartables `test_xxxxxxxx` en localhost:5433 (contenedor `danteautomotores-db` del `docker-compose.yml`) y NUNCA tocan la base `danteautomotores`. Sin Postgres se saltean; con `-Ddante.pg.required=true` fallan.

Front (verificado):
- `src/routes/AppRouter.jsx`: `Routes` con `/`, `/autos`, `/publicaciones/:id`, `/agencias/:slug`, `/login`, `/registro`, `/creditos`, `/favoritos`, `/admin`, `/admin/publicaciones/nueva`, `/admin/publicaciones/:id/editar`; no hay ruta `*`. `App.jsx` envuelve el router con Navbar y Footer, asi que una pagina solo renderiza su `<main>`.
- Modelo de pagina simple: `src/pages/CreditosPage.jsx` (`<main className="mx-auto max-w-4xl px-4 py-16">`, eyebrow `text-xs font-bold uppercase tracking-[0.18em] text-bronze`, titulo `font-heading text-3xl text-navy-dark`, texto `text-sm text-slate-500`).
- Botones existentes en el sitio: primario `inline-flex items-center gap-2 rounded-full bg-bronze px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-bronze/20 transition hover:-translate-y-0.5 hover:bg-bronze-light`; secundario `rounded-full bg-navy px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-navy/15 transition hover:-translate-y-0.5 hover:bg-bronze`. Animacion de entrada existente: `animate-fade-in-up` (definida en `src/index.css`).
- `lucide-react` ^1.47.0 instalado y exporta `Car` (ya usado en `src/pages/admin/AdminDashboardPage.jsx`).
- `src/pages/admin/AdminDashboardPage.jsx` lineas 37-42: `fotoDePortada(fotos)` devuelve la foto con orden 0 y si no hay, `fotos[0]`; se usa en la `key` de `MiniaturaPublicacion` (linea 442) y dentro del componente (linea 548). `src/pages/admin/AdminPublicacionFormPage.jsx` ya trata la primera foto del array como portada ("La primera foto es la portada").
- Estilo del front: sin punto y coma, comillas simples, 2 espacios, comentarios en espanol que explican el porque, export default de funcion.

Entorno (solo JDK 17; Maven offline). En Git Bash, antes de cada `mvn`:
export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"
y siempre `mvn -B -o -Djava.version=17 ...`. Build del front: `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build`. Los archivos del working tree tienen CRLF: los greps anclados con `$` o `-x` pasan antes por `tr -d '\r'`.

Mensajes de commit: convencion de los fixes del review, en espanol sin tildes, por ejemplo `fix(02): IN-04 ...`, `docs(02): IN-03 ...`.
</interfaces>
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1 (repo back): orden total de las fotos con un unico comparador, @OrderBy y tests de portada estable (IN-04)</name>
  <files>src/main/java/com/danteautomotores/mapper/PublicacionMapper.java, src/main/java/com/danteautomotores/entity/Publicacion.java, src/main/java/com/danteautomotores/service/PublicacionService.java, src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java, src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java</files>
  <read_first>src/main/java/com/danteautomotores/mapper/PublicacionMapper.java (completo), src/main/java/com/danteautomotores/entity/Publicacion.java (lineas 90-98), src/main/java/com/danteautomotores/service/PublicacionService.java (lineas 285-305), src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java (completo), src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java (lineas 1-100 y 130-146)</read_first>
  <behavior>
    - Test 1 (PublicacionMapperTest): fotos en la lista en el orden [id 7 orden 0 url siete, id 3 orden null url tres, id 9 orden 1 url nueve] y tambien en el orden inverso: en ambos casos toResponse devuelve fotos con ids [3, 7, 9] y toResumen.fotoPortada es la url de la foto 3, igual a toResponse.getFotos().get(0).getUrl(). Hoy falla (stream estable y min se quedan con la 7 en el primer orden).
    - Test 2: el orden explicito gana al id: [id 1 orden 2, id 2 orden 0] da portada = foto 2 y detalle [2, 1].
    - Test 3: una foto sin id (todavia no persistida) no rompe: [id null orden 0 url nueva, id 4 orden 0 url cuatro] no lanza excepcion, la portada es la foto 4 y el detalle queda [4, null].
    - Test 4 (CatalogoPostgresTest, Postgres real): un auto destacado DISPONIBLE con tres fotos guardadas en este orden: orden 1 (url uno), orden null (url nula), orden 0 (url cero). La fotoPortada que devuelve catalogoService.destacados es la url nula, y es igual a la primera foto de PublicacionMapper.toResponse sobre la misma publicacion leida de nuevo del repositorio (despues de em.flush y em.clear). Ejercita ademas la carga de la coleccion con el @OrderBy contra PostgreSQL.
  </behavior>
  <action>
RED primero: agregar los tests 1 a 3 del bloque behavior a `src/test/java/com/danteautomotores/mapper/PublicacionMapperTest.java` (nombres en espanol, por ejemplo `laPortadaDelResumenEsLaPrimeraFotoDelDetalleAunqueEmpatenEnOrden`, `elOrdenExplicitoGanaAlId`, `unaFotoSinIdQuedaDespuesDeLasQueTienenId`), armando las fotos con `FotoPublicacion.builder().id(..).url(..).orden(..).build()` y agregandolas a `publicacion.getFotos()` del helper `auto(...)` existente (la lista es mutable por `@Builder.Default`). Correr `-Dtest=PublicacionMapperTest` y confirmar que el test 1 falla antes de tocar el codigo de produccion. Commit opcional del rojo; el commit obligatorio es el del verde.

GREEN, segun IN-04 de 02-REVIEW.md:
1) En `src/main/java/com/danteautomotores/mapper/PublicacionMapper.java` agregar la constante publica `ORDEN_DE_FOTOS` de tipo `Comparator<FotoPublicacion>`: primero por `orden` contando null como 0 (el criterio que ya existia, no se cambia) y despues por `id` con `Comparator.nullsLast(Comparator.naturalOrder())`. Comentario en espanol con el porque, al estilo del de `esOferta`: es la unica regla de orden de fotos y de portada; el desempate por id hace que la card y el detalle elijan la misma foto aunque dos empaten (fotos viejas con orden null), sin depender del orden en que la base devuelve las filas. Usarla en `toResponse` (`sorted(ORDEN_DE_FOTOS)`) y en `toResumen` (`min(ORDEN_DE_FOTOS)`), y actualizar el comentario de `toResumen` para que diga que la portada es la primera foto segun `ORDEN_DE_FOTOS`, la misma que encabeza `fotos` en el detalle.
2) En `src/main/java/com/danteautomotores/entity/Publicacion.java` agregar `@OrderBy("orden ASC, id ASC")` a la coleccion `fotos` (la anotacion de JPA, `jakarta.persistence.OrderBy`, ya cubierta por el import con comodin; no usar la de Hibernate). Comentario corto con el porque: hace determinista el orden en que se carga la coleccion para cualquier codigo que la recorra sin ordenar; PostgreSQL pone los orden null al final, por eso la regla que decide la portada es `PublicacionMapper.ORDEN_DE_FOTOS`. No hace falta migracion: no cambia el esquema (`ddl-auto: validate` sigue igual).
3) En `src/main/java/com/danteautomotores/service/PublicacionService.java`, `resecuenciarFotos` pasa a ordenar con `PublicacionMapper.ORDEN_DE_FOTOS` en lugar de su comparador local (mismo criterio, comportamiento identico); dejar el comentario diciendo que usa la misma regla que el mapper y quitar el import de `java.util.Comparator` si queda sin uso.
4) Agregar el test 4 del bloque behavior a `src/test/java/com/danteautomotores/service/CatalogoPostgresTest.java`, siguiendo el modelo de `fotoPortadaEsLaDeMenorOrdenYNullSiNoHayFotos` (por ejemplo `laPortadaDelListadoEsLaPrimeraFotoDelDetalleAunqueEmpatenEnOrden`), importando `PublicacionMapper`. No tocar los demas tests.
5) Correr los tests del automated. El contenedor `danteautomotores-db` esta corriendo; si no, `docker compose up -d` desde la raiz del back solo lo levanta (los tests crean y borran sus propias bases `test_*` y no tocan la base `danteautomotores`). Commit en el repo back: `fix(02): IN-04 orden total de las fotos para que la portada coincida entre listado y detalle`.
  </action>
  <verify>
    <automated>export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -o -q -Djava.version=17 test -Dtest='PublicacionMapperTest,PublicacionServiceTest' && grep -qF '@OrderBy("orden ASC, id ASC")' src/main/java/com/danteautomotores/entity/Publicacion.java && [ "$(grep -c 'ORDEN_DE_FOTOS' src/main/java/com/danteautomotores/mapper/PublicacionMapper.java)" -ge 3 ] && grep -qF 'PublicacionMapper.ORDEN_DE_FOTOS' src/main/java/com/danteautomotores/service/PublicacionService.java && echo "Task 1 unit OK"</automated>
    <fails_when>Falla algun test de PublicacionMapperTest (portada distinta de la primera foto del detalle, orden de ids distinto de [3, 7, 9], NullPointerException con id null) o de PublicacionServiceTest (el resecuenciado cambio de comportamiento); o falta el @OrderBy exacto en Publicacion.fotos, o ORDEN_DE_FOTOS no esta declarado y usado en toResponse y toResumen, o resecuenciarFotos no usa PublicacionMapper.ORDEN_DE_FOTOS.</fails_when>
    <automated>docker compose up -d >/dev/null 2>&1; export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -o -q -Djava.version=17 test -Ddante.pg.required=true && echo "Suite completa con Postgres OK"</automated>
    <fails_when>Falla cualquier test de la suite completa, incluido el nuevo caso de CatalogoPostgresTest (la fotoPortada del listado no es la url nula o no coincide con la primera foto del detalle), MigracionesPostgresTest (el @OrderBy rompio el arranque con ddl-auto validate) o cualquier otro; o no hay Postgres en localhost:5433 (exigirPostgres lanza AssertionError por dante.pg.required=true).</fails_when>
  </verify>
  <done>Hay un unico comparador `PublicacionMapper.ORDEN_DE_FOTOS` (orden con null = 0, desempate por id con nulls al final) usado por `toResponse`, `toResumen` y `resecuenciarFotos`; `Publicacion.fotos` tiene `@OrderBy("orden ASC, id ASC")`; los tests nuevos del mapper y de Postgres pasan y la suite completa pasa con `-Ddante.pg.required=true`; commit en el repo back.</done>
</task>

<task type="auto">
  <name>Task 2 (repo front): pagina 404 con ruta catch-all (IN-07) y miniatura del panel alineada con la portada del back (IN-04)</name>
  <files>src/pages/NoEncontradaPage.jsx, src/routes/AppRouter.jsx, src/pages/admin/AdminDashboardPage.jsx</files>
  <read_first>src/routes/AppRouter.jsx (completo), src/pages/CreditosPage.jsx (modelo de pagina simple y estilos), src/pages/admin/AdminDashboardPage.jsx (lineas 30-45 y 435-450 y 540-560)</read_first>
  <action>
Repo front (`C:/Users/toto/Desktop/work/danteautomotores-front`); rutas relativas a su raiz.

Parte A, IN-07 (pagina 404):
1) Crear `src/pages/NoEncontradaPage.jsx` con `export default function NoEncontradaPage()`, importando `Link` de `react-router-dom` y `Car` de `lucide-react`. Renderiza solo un `<main>` (Navbar y Footer los pone `App.jsx`) centrado y con la entrada animada existente: `mx-auto flex max-w-2xl flex-col items-center px-6 py-24 text-center animate-fade-in-up`. Contenido, en este orden: un circulo `flex h-14 w-14 items-center justify-center rounded-full bg-bronze/10 text-bronze` con `Car` (`className="h-7 w-7"`, `aria-hidden="true"`); el eyebrow "Error 404" con las clases del eyebrow de `CreditosPage` (`mt-6 text-xs font-bold uppercase tracking-[0.18em] text-bronze`); un `h1` "No encontramos esta página" con `mt-2 font-heading text-3xl text-navy-dark`; un parrafo `mt-3 text-sm text-slate-500` "Puede que el link esté mal escrito o que la página ya no exista."; y un contenedor `mt-8 flex flex-col gap-3 sm:flex-row` con dos `Link`: primero `to="/autos"` con el texto "Ver autos" y las clases del boton primario bronze listado en interfaces (agregar `justify-center`), despues `to="/"` con el texto "Ir al inicio" y las clases del boton secundario navy. No mostrar la ruta pedida ni leerla del router: un link armado a mano no debe poder poner texto propio en una pagina del sitio. Sin estado, sin efectos, sin llamadas a la API.
2) En `src/routes/AppRouter.jsx` importar `NoEncontradaPage` desde `'../pages/NoEncontradaPage.jsx'` (junto a los demas imports de paginas) y agregar `<Route path="*" element={<NoEncontradaPage />} />` como ULTIMA `Route` dentro de `Routes`. No cambiar ninguna otra ruta ni el `ProtectedRoute`.
3) `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` en verde y commit en el repo front: `fix(02): IN-07 pagina 404 para rutas inexistentes con links al inicio y al catalogo`.

Parte B, IN-04 en el panel (depende de la Task 1, que ya ordena las fotos en el back):
4) En `src/pages/admin/AdminDashboardPage.jsx`, `fotoDePortada(fotos)` conserva la guarda que devuelve null si `fotos` no es un array o esta vacio y pasa a devolver `fotos[0]` (quitar la busqueda por la foto de orden 0). Actualizar el comentario: el back entrega las fotos ordenadas con su unica regla de portada (orden, null como 0, desempate por id), la misma que usan la card publica, el detalle y el formulario de edicion ("La primera foto es la portada"); buscar la de orden 0 podia elegir otra foto si una vieja tiene orden null. No tocar `MiniaturaPublicacion`, la `key` de la fila ni nada mas del archivo.
5) Build en verde y commit en el repo front: `fix(02): IN-04 la miniatura del panel usa la primera foto que ordena el back`.
  </action>
  <verify>
    <automated>npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build && P=src/pages/NoEncontradaPage.jsx && R=src/routes/AppRouter.jsx && A=src/pages/admin/AdminDashboardPage.jsx && grep -qF "import NoEncontradaPage from '../pages/NoEncontradaPage.jsx'" $R && grep '<Route' $R | tail -1 | grep -qF 'path="*" element={<NoEncontradaPage />}' && [ "$(grep -c '<Route' $R)" -ge 13 ] && grep -qF 'to="/"' $P && grep -qF 'to="/autos"' $P && grep -q 'text-navy-dark' $P && grep -q 'bg-bronze' $P && grep -q 'Error 404' $P && ! grep -qE 'useLocation|pathname' $P && grep -qF 'return fotos[0]' $A && ! grep -qF 'f.orden === 0' $A && grep -qF "key={fotoDePortada(p.fotos)?.url ?? 'sin-fotos'}" $A && echo "Task 2 OK"</automated>
    <fails_when>El build de Vite falla; AppRouter no importa NoEncontradaPage o la ruta path="*" no es la ultima Route o se perdio alguna de las rutas existentes (hoy 12 lineas abren Route o Routes; tras el cambio tienen que ser al menos 13); la pagina no tiene los links a "/" y "/autos", el eyebrow "Error 404" o los colores navy/bronze; la pagina lee la ruta pedida del router; fotoDePortada sigue buscando la foto de orden 0 o no devuelve fotos[0]; o se perdio la key de MiniaturaPublicacion.</fails_when>
    <human-check>Sin tocar el `.env` del front: con el back local levantado, en el repo front `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174` y abrir http://localhost:5174/auto/12 y http://localhost:5174/esto/no/existe: se ve la pagina 404 entre Navbar y Footer, con "Ver autos" (lleva a /autos) e "Ir al inicio" (lleva a /), en desktop y a 375px de ancho sin scroll horizontal. /, /autos, /publicaciones/:id, /creditos y /admin siguen funcionando. En /admin, la miniatura de cada auto coincide con la foto de portada de su card en /autos.</human-check>
  </verify>
  <done>Una URL inexistente renderiza `NoEncontradaPage` (eyebrow "Error 404", titulo navy, links a /autos y a /), sin reflejar la ruta pedida; la ruta `*` es la ultima del router; la miniatura del panel usa `fotos[0]`; build en verde; dos commits en el repo front.</done>
</task>

<task type="auto">
  <name>Task 3 (repo back): README sin contradiccion sobre los tests de Postgres (IN-03) y disposicion del review actualizada</name>
  <files>README.md, .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md</files>
  <read_first>README.md (lineas 48-97), src/test/java/com/danteautomotores/support/PostgresLocalTestBase.java (lineas 16-24 y 60-103), .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md (completo)</read_first>
  <action>
Repo back; rutas relativas a su raiz. Usar Edit (no Write) en ambos archivos y conservar sus finales de linea.

Parte A, IN-03 en `README.md`:
1) Linea 56 (seccion "Base de datos y migraciones"): reemplazar el bullet por uno corto que diga que las migraciones y las consultas del catalogo se prueban contra Postgres real y remita a la seccion "Tests" para saber cuando esos tests se saltean y cuando son obligatorios.
2) Reescribir la seccion "Tests" (lineas 87-97) para que no se contradiga con lo anterior. Debe decir, en espanol y en este orden:
   - Hay dos grupos: la mayoria (unitarios y de controladores) corre sin Docker ni base de datos; `MigracionesPostgresTest` (V1 a V3 contra `ddl-auto: validate`, incluida una base creada por Hibernate sin historial) y `CatalogoPostgresTest` (consultas reales del catalogo) usan el Postgres del `docker-compose.yml` en localhost:5433, en bases descartables `test_xxxxxxxx` que crean y borran solas, sin tocar nunca la base `danteautomotores`. Para los unitarios usar exactamente la redaccion "corren sin Docker ni base de datos": el gate de verificacion rechaza la frase vieja de la linea 89.
   - Sin Postgres, esos dos tests se saltean: Maven los informa como "Skipped" y el build queda verde igual, asi que un verde sin Postgres no prueba las migraciones.
   - Con `-Ddante.pg.required=true` no se saltean: sin Postgres fallan con un mensaje que explica como levantarlo. Es obligatorio correrlos asi (con `docker compose up -d` antes) antes de cada deploy y cada vez que se agrega una migracion o se cambia una entidad.
   - La conexion se puede cambiar con `-Ddante.pg.host`, `-Ddante.pg.port`, `-Ddante.pg.user` y `-Ddante.pg.password` (por defecto localhost, 5433 y el usuario y la contrasena de desarrollo del `docker-compose.yml`).
   - Bloque de comandos bash: `mvn -B test` (rapido, los de Postgres pueden saltearse) y `docker compose up -d` seguido de `mvn -B test -Ddante.pg.required=true` (obligatorio antes de cada deploy).
   - Setup en Windows (Git Bash) con solo JDK 17: conservar el `export JAVA_HOME="/c/Program Files/Java/jdk-17"`, reemplazar la ruta personal de Maven de la linea 93 por `export PATH="$MAVEN_HOME/bin:$PATH"` aclarando que `MAVEN_HOME` es la carpeta donde esta instalado Maven 3.9+, y usar `mvn -B -Djava.version=17 test -Ddante.pg.required=true` sin `-o` (el modo offline solo anda con las dependencias ya descargadas). Mantener la aclaracion de que `-Djava.version=17` es solo para equipos sin JDK 21 y no se cambia el `pom.xml`.
3) En la seccion "Producción", agregar a la lista de la linea 82 en adelante un bullet: antes de cada deploy, con `docker compose up -d`, correr `mvn -B test -Ddante.pg.required=true` y exigir verde; sin el flag los tests de migraciones pueden saltearse en silencio.
4) No tocar el resto del README. Commit en el repo back: `docs(02): IN-03 el README aclara cuando se saltean los tests de Postgres y exige -Ddante.pg.required=true antes de cada deploy`.

Parte B, disposicion (`.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md`), solo despues de que las Tasks 1 y 2 y la Parte A esten commiteadas:
5) Cambiar en su lugar (sin reordenar la tabla) las filas IN-03, IN-04 e IN-07 de `| open | - |` a `| fixed | quick 261003-qde |`, dejando el resto de las filas igual.
6) Cambiar la linea final del contador de 10 a 7 (quedan abiertas IN-01, IN-02, IN-05, IN-06, IN-08, IN-09, IN-10) y actualizar `updated:` del frontmatter con la fecha y hora actuales en ISO 8601 UTC.
7) Commit en el repo back: `docs(02): IN-03, IN-04 e IN-07 resueltos en la disposicion del code review`.
  </action>
  <verify>
    <automated>! grep -q 'no necesitan Docker ni Postgres' README.md && ! grep -q '/c/Users/' README.md && [ "$(grep -c 'dante.pg.required=true' README.md)" -ge 2 ] && grep -qi 'skipped' README.md && grep -qi 'antes de cada deploy' README.md && grep -q 'MAVEN_HOME' README.md && grep -q 'corren sin Docker ni base de datos' README.md && grep -q 'dante.pg.host' README.md && D=.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md && [ "$(tr -d '\r' < $D | grep -cE '^\| IN-0(3|4|7) \| info \| fixed \| quick 261003-qde \|$')" -ge 3 ] && ! tr -d '\r' < $D | grep -qE '^\| IN-0(3|4|7) \| info \| open' && [ "$(tr -d '\r' < $D | grep -cE '^\| IN-(01|02|05|06|08|09|10) \| info \| open \| - \|$')" -ge 7 ] && [ "$(tr -d '\r' < $D | grep -cE '^\| WR-0[1-7] \| warning \| (fixed|deferred) \|')" -ge 7 ] && tr -d '\r' < $D | grep -qx 'open: 7' && echo "Task 3 OK"</automated>
    <fails_when>El README sigue con la frase vieja de la seccion Tests o con una ruta personal de Windows; menciona dante.pg.required=true menos de dos veces; no explica que sin Postgres los tests quedan como Skipped, no exige el flag antes de cada deploy, no usa MAVEN_HOME o no documenta dante.pg.host; o la disposicion no tiene exactamente IN-03, IN-04 e IN-07 en fixed con Source quick 261003-qde, no quedan exactamente 7 filas IN en open, se alteraron las 7 filas WR, o el contador no dice open: 7.</fails_when>
  </verify>
  <done>El README explica sin contradicciones cuando se saltean los tests de Postgres y cuando son obligatorios (`-Ddante.pg.required=true`, antes de cada deploy), documenta las propiedades de conexion y no tiene rutas personales; la disposicion tiene IN-03, IN-04 e IN-07 en `fixed` (Source `quick 261003-qde`), `open: 7` y `updated:` actualizado; dos commits en el repo back.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| URL del navegador -> SPA (React Router) | Cualquier ruta que escriba o comparta un tercero llega a la ruta catch-all y se renderiza con la pagina 404 |
| API publica -> navegador | `fotoPortada` (listado) y `fotos` (detalle y panel) definen que imagen ve el usuario |
| README -> quien despliega | El README es la guia de verificacion previa al deploy; un verde enganoso deja pasar migraciones rotas a produccion |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-qde-01 | Spoofing | `NoEncontradaPage` (front) | low | mitigate | La pagina no muestra ni lee la ruta pedida: un link armado a mano no puede inyectar texto propio (por ejemplo un telefono falso) en una pagina con la marca del sitio; gate negativo en el automated de la Task 2 |
| T-qde-02 | Tampering | Verificacion previa al deploy (README) | medium | mitigate | El README explica que sin Postgres los tests de migraciones se saltean con build verde y exige `-Ddante.pg.required=true` antes de cada deploy; la Task 1 corre la suite completa con ese flag |
| T-qde-03 | Information Disclosure | README.md | low | mitigate | Se reemplaza la ruta personal del equipo de desarrollo por `$MAVEN_HOME`; no se agregan credenciales nuevas (solo se mencionan los valores de desarrollo que ya estan en `docker-compose.yml`) |
| T-qde-04 | Denial of Service | Carga de `Publicacion.fotos` con `@OrderBy` | low | accept | Agrega un ORDER BY sobre como mucho 10 fotos por auto (tope de 01-06); costo despreciable y sin cambio de esquema |
| T-qde-05 | Tampering | Portada inconsistente entre vistas | low | mitigate | Un unico comparador total (`ORDEN_DE_FOTOS`) para detalle, listado y resecuenciado, y el panel usa `fotos[0]` del back; tests unitarios y contra Postgres |
| T-qde-SC | Tampering | npm/Maven installs | low | accept | Esta tarea no instala paquetes: `Car` ya esta en `lucide-react` ^1.47.0 y el back no suma dependencias (Maven offline) |
</threat_model>

<verification>
- Task 1: `PublicacionMapperTest` y `PublicacionServiceTest` en verde y la suite completa del back en verde con `-Ddante.pg.required=true` (incluye el nuevo caso de `CatalogoPostgresTest` y `MigracionesPostgresTest`).
- Task 2: `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` en verde y gates grep de la ruta `*`, la pagina 404 y `fotoDePortada`; human-check (end-of-phase) de la 404 en el front de prueba en 5174.
- Task 3: gates grep del README y de la disposicion (3 filas fixed, 7 open, contador `open: 7`, filas WR intactas).
- Repos: `git -C C:/Users/toto/Desktop/work/danteautomotores-back status --short` y el mismo en el front sin cambios sin commitear del alcance de esta tarea; sin push.
</verification>

<success_criteria>
- La card del listado y el detalle muestran siempre la misma portada, aun con fotos empatadas en orden; un unico comparador lo define y esta cubierto por tests unitarios y contra Postgres.
- Una URL inexistente del front muestra una pagina 404 con el estilo del sitio y links a la Home y a /autos.
- El README del back explica sin contradicciones cuando se saltean los tests de Postgres y exige `-Ddante.pg.required=true` antes de cada deploy, sin rutas personales.
- `02-REVIEW-DISPOSITION.md`: IN-03, IN-04 e IN-07 en `fixed` (Source `quick 261003-qde`) y `open: 7`.
</success_criteria>

<output>
Create `.planning/quick/261003-qde-fix-review-infos-in-03-in-04-in-07-de-la/261003-qde-SUMMARY.md` when done
</output>
