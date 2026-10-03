---
phase: quick-261003-sfp
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java
  - src/main/java/com/danteautomotores/config/SecretosGuard.java
  - src/main/java/com/danteautomotores/config/DataSeeder.java
  - src/test/java/com/danteautomotores/config/EntornoDeDesarrolloTest.java
  - src/test/java/com/danteautomotores/config/DataSeederTest.java
  - README.md
  - ../danteautomotores-front/src/utils/catalogoParams.js
  - ../danteautomotores-front/src/utils/catalogoParams.test.js
  - ../danteautomotores-front/src/pages/CreditosPage.jsx
  - scripts/demo/sembrar-demo.js
  - scripts/demo/sembrar-demo.test.js
  - scripts/demo/README.md
  - .gitignore
  - .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md
autonomous: true
requirements: [ADM-01, PROD-02, CAT-02, PROD-04]

estimate:
  tokens: 60000
  raw_tokens: 60000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "DataSeeder y SecretosGuard deciden 'modo desarrollo' con un unico helper, EntornoDeDesarrollo.esDesarrollo(environment): con perfiles activos, es desarrollo solo si todos son dev, local o test; sin perfiles activos, solo si los perfiles por defecto son el 'default' implicito de Spring o de desarrollo (IN-01)"
    - "Con cualquier perfil que no sea de desarrollo (prod, production, railway, staging, qa, una mezcla como prod,dev, o SPRING_PROFILES_DEFAULT=prod sin perfil activo), si no existe ningun admin y faltan o son invalidas ADMIN_EMAIL, ADMIN_PASSWORD o ADMIN_NOMBRE, el arranque se aborta con IllegalStateException; sin perfil o con dev/local/test solo se avisa en el log (IN-01)"
    - "La primera banda de precio empieza en Math.floor del minimo y la ultima termina en Math.ceil del maximo, asi el auto mas barato y el mas caro siempre entran en su propia banda aunque su precio tenga centavos (IN-06)"
    - "leerFiltros del front descarta los valores de tipo, zona, transmision y estado que no son claves propias de TIPO_CARROCERIA, ZONA, TRANSMISION y ESTADO (incluidas claves heredadas como constructor o __proto__), los anioMin/anioMax/kmMax que no son enteros no negativos dentro del rango de un int de Java, los precioMin/precioMax que no son numeros no negativos y un ofertas distinto de 'true'; el resto del link se conserva y paramsParaApi ya no manda lo descartado (WR-02 front)"
    - "La pagina /creditos aclara que las imagenes fueron recortadas, redimensionadas y comprimidas para su visualizacion (IN-09)"
    - "sembrar-demo.js nunca sobrescribe los datos de una agencia existente con el mismo nombre: si no tiene zona se la completa con un PUT que reenvia sus propios datos; si ya tiene zona no se le manda nada (IN-10)"
    - "Cada corrida de sembrar-demo.js registra, a medida que los crea, los ids de agencias y autos en scripts/demo/registros/<host>.json (fuera de git); DESHACER=1 borra solo esos ids, y solo si el registro es del mismo backend y los datos coinciden; todas las guardas nuevas corren antes de cualquier request (IN-10)"
    - "02-REVIEW-DISPOSITION.md: IN-01, IN-06, IN-09 e IN-10 en fixed con Source quick 261003-sfp, la fila de WR-02 dice que la parte del front se cerro en quick 261003-sfp y el contador queda en open: 3"
  artifacts:
    - path: "src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java"
      provides: "Helper unico esDesarrollo(Environment) con PERFILES_DE_DESARROLLO = Set.of(\"dev\", \"local\", \"test\")"
      contains: "esDesarrollo"
    - path: "src/test/java/com/danteautomotores/config/EntornoDeDesarrolloTest.java"
      provides: "Tests del helper: sin perfil, dev/local/test, perfiles de produccion, mezcla, perfiles por defecto"
    - path: "src/test/java/com/danteautomotores/config/DataSeederTest.java"
      provides: "Tests parametrizados: el seed aborta con production/railway/staging/qa y prod,dev y con SPRING_PROFILES_DEFAULT=prod; solo avisa con dev/local/test"
    - path: "../danteautomotores-front/src/utils/catalogoParams.js"
      provides: "leerFiltros con validacion contra etiquetas.js y escalares numericos; bandasDePrecio con floor/ceil en las puntas"
      contains: "from './etiquetas.js'"
    - path: "../danteautomotores-front/src/utils/catalogoParams.test.js"
      provides: "Tests node:test de WR-02 front e IN-06"
    - path: "../danteautomotores-front/src/pages/CreditosPage.jsx"
      provides: "Aviso de cambios en las imagenes (CC BY / BY-SA)"
      contains: "recortadas, redimensionadas y comprimidas"
    - path: "scripts/demo/sembrar-demo.js"
      provides: "cuerpoParaCompletarZona, planDeDeshacer, rutaDelRegistro exportados; main bajo require.main === module; registro incremental y modo DESHACER=1"
      contains: "require.main === module"
    - path: "scripts/demo/sembrar-demo.test.js"
      provides: "Tests node:test de las funciones puras del script"
    - path: "scripts/demo/README.md"
      provides: "Documentacion del registro de corrida, DESHACER=1, REGISTRO y del nuevo trato de agencias existentes"
      contains: "DESHACER=1"
    - path: ".gitignore"
      provides: "scripts/demo/registros/ fuera de git"
      contains: "scripts/demo/registros/"
    - path: ".planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md"
      provides: "IN-01, IN-06, IN-09, IN-10 fixed; WR-02 con nota del front; open: 3"
  key_links:
    - from: "src/main/java/com/danteautomotores/config/DataSeeder.java (fallarOAvisar)"
      to: "src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java"
      via: "EntornoDeDesarrollo.esDesarrollo(environment)"
      pattern: "EntornoDeDesarrollo\\.esDesarrollo\\(environment\\)"
    - from: "src/main/java/com/danteautomotores/config/SecretosGuard.java (fallarOAvisar)"
      to: "src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java"
      via: "EntornoDeDesarrollo.esDesarrollo(environment)"
      pattern: "EntornoDeDesarrollo\\.esDesarrollo\\(environment\\)"
    - from: "../danteautomotores-front/src/utils/catalogoParams.js (leerFiltros)"
      to: "../danteautomotores-front/src/utils/etiquetas.js"
      via: "import de TIPO_CARROCERIA, ZONA, TRANSMISION y ESTADO como listas blancas"
      pattern: "from './etiquetas.js'"
    - from: "../danteautomotores-front/src/pages/HomePage.jsx y AutosPage.jsx"
      to: "../danteautomotores-front/src/utils/catalogoParams.js (bandasDePrecio)"
      via: "links precioMin/precioMax de 'Busca por presupuesto' y 'Ver rangos de precios'"
      pattern: "bandasDePrecio"
    - from: "scripts/demo/sembrar-demo.js (main)"
      to: "scripts/demo/registros/<host>.json"
      via: "rutaDelRegistro(B) + escritura despues de cada POST exitoso"
      pattern: "rutaDelRegistro"
---

<objective>
Cerrar cinco hallazgos del code review de la Fase 2 (`.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md`, secciones IN-01, IN-06, IN-09, IN-10 y la parte del front de WR-02; la parte del back de WR-02 ya está en `02-REVIEW-FIX.md`) y registrarlos en la disposición del review.

- IN-01 (back): `DataSeeder` solo aborta con el perfil exacto `prod` mientras `SecretosGuard` aborta con cualquier perfil que no sea de desarrollo. Se extrae un helper común `EntornoDeDesarrollo.esDesarrollo(environment)` que usan los dos, con tests.
- IN-06 (front): `bandasDePrecio` redondea la primera punta con `Math.round`; pasa a `Math.floor` en la primera y `Math.ceil` en la última, con test node:test.
- WR-02 front: `leerFiltros` copia sin validar cualquier valor de la URL; pasa a descartar enums desconocidos y escalares no numéricos, con tests node:test, para que un link viejo o mal escrito no deje la página sin resultados.
- IN-09 (front): `CreditosPage` aclara que las imágenes fueron recortadas, redimensionadas y comprimidas (las licencias CC BY y BY-SA exigen indicar cambios).
- IN-10 (back, script): `scripts/demo/sembrar-demo.js` deja de sobrescribir agencias existentes (solo completa `zona` vacía) y registra los ids creados en un archivo local fuera de git, con un modo `DESHACER=1` para deshacer una corrida parcial; documentado en `scripts/demo/README.md`.
- Al final, `02-REVIEW-DISPOSITION.md`: IN-01, IN-06, IN-09 e IN-10 a `fixed` (Source `quick 261003-sfp`), nota en la fila de WR-02 y `open:` de 7 a 3.

Purpose: el sitio se publica en esta fase; un deploy con un perfil distinto de `prod` no puede quedar sin admin en silencio, un link compartido no puede dejar el catálogo vacío, los créditos tienen que cumplir la licencia y el script de demo (el que se usará contra producción) no puede pisar datos reales ni dejar basura sin forma de deshacerla.
Output: helper + tests (back), `catalogoParams.js` + tests + `CreditosPage` (front), `sembrar-demo.js` + tests + README + `.gitignore` y disposición actualizada (back).

Repos y rutas: son dos repos git. Back = `C:/Users/toto/Desktop/work/danteautomotores-back` (acá viven el plan, el README, el script de demo y la disposición). Front = `C:/Users/toto/Desktop/work/danteautomotores-front`. En el cuerpo del plan cada ruta es relativa a la raíz del repo que indica la tarea, y cada `<automated>` asume como cwd la raíz de ese repo. En el frontmatter las rutas son relativas al back (el front como `../danteautomotores-front/`). Los commits de código del front van en el repo front (`git -C C:/Users/toto/Desktop/work/danteautomotores-front commit ...`); el resto en el back. Sin push.

Cobertura de fuentes:

| Fuente | Ítem | Tarea |
|--------|------|-------|
| 02-REVIEW.md | IN-01 criterio de desarrollo distinto en DataSeeder y SecretosGuard | Task 1 (back) |
| 02-REVIEW.md | IN-06 bandas de precio con `Math.round` en la primera punta | Task 2 parte A (front) |
| 02-REVIEW.md / 02-REVIEW-FIX.md | WR-02 parte front: `leerFiltros` sin validar | Task 2 parte B (front) |
| 02-REVIEW.md | IN-09 créditos sin aviso de cambios | Task 2 parte C (front) |
| 02-REVIEW.md | IN-10 PUT que pisa la agencia + sin forma de deshacer una corrida parcial | Task 3 parte A y B (back) |
| Pedido del usuario | Disposición: 4 IN a fixed, nota en WR-02, `open: 3` | Task 3 parte C |

Fuera de alcance (no tocar): IN-02, IN-05, IN-08, WR-03, WR-05; `02-08-PLAN.md` (su comando de carga de la demo sigue funcionando igual); el `.env` del front; la base `danteautomotores`; push y producción. El script de demo NO se ejecuta contra ningún backend en esta tarea (ni local: el back del 8080 usa la base `danteautomotores` y la carga sube fotos a Cloudinary); se verifica con tests puros y con guardas que abortan antes de cualquier request, apuntando a `http://127.0.0.1:9/api`.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW.md
@.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-FIX.md
@.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md

<interfaces>
Back (verificado leyendo el código):
- `src/main/java/com/danteautomotores/config/SecretosGuard.java`: `@Component @RequiredArgsConstructor @Slf4j`, implementa `InitializingBean`, recibe `Environment environment` por constructor. Hoy declara su propio conjunto privado de perfiles de desarrollo (`Set.of("dev", "local", "test")`, línea 37) y un método privado `esEntornoDeDesarrollo()` (líneas 87-90) que hace `Arrays.stream(environment.getActiveProfiles()).allMatch(...)`; `fallarOAvisar` (líneas 79-85) lanza `IllegalStateException` fuera de desarrollo y si no hace `log.warn("{} (fuera del modo desarrollo, ...)", mensaje)`. El Javadoc dice "Mismo criterio de fondo que {@link DataSeeder}", que hoy no es cierto.
- `src/main/java/com/danteautomotores/config/DataSeeder.java`: `@Component @RequiredArgsConstructor @Slf4j`, `ApplicationRunner`; constructor `(UsuarioRepository, AgenciaRepository, PasswordEncoder, Environment)`. `fallarOAvisar` (líneas 101-107) lanza solo si el entorno acepta el perfil exacto `prod` (usa `Profiles.of("prod")`, import `org.springframework.core.env.Profiles`); si no, `log.warn(mensaje)`. Se llama desde `sembrarAdmin` cuando faltan variables, el email no tiene `@`, la contraseña es corta o el email ya existe. Si ya hay un admin, retorna antes de validar nada.
- `src/test/java/com/danteautomotores/config/DataSeederTest.java`: `@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})` (stubs estrictos: un `when` sin usar hace fallar el test), helper `seeder(boolean prod, email, password, nombre)` con `MockEnvironment` y `ReflectionTestUtils.setField` para `adminEmail`, `adminPassword`, `adminNombre`. Los tests "con perfil prod" stubbean `agenciaRepository.count()` y `usuarioRepository.existsByRol(Rol.ADMIN)`.
- `src/test/java/com/danteautomotores/config/SecretosGuardTest.java`: ya cubre `production`, `railway`, `staging`, `qa`, `prod,dev`, sin perfil y `dev/local/test` con `@ParameterizedTest @ValueSource` (junit-jupiter-params disponible) y `CapturedOutput` como parámetro. No se modifica: tiene que seguir en verde.
- `MockEnvironment` (spring-test): `setActiveProfiles(String...)`, `setDefaultProfiles(String...)`; sin llamar a `setDefaultProfiles`, `getDefaultProfiles()` devuelve `["default"]`. En Spring, `Environment.getActiveProfiles()` está vacío cuando solo rigen los perfiles por defecto. La constante `"default"` de `AbstractEnvironment` es `protected`: el helper declara la suya.
- No hay `@ActiveProfiles` ni `application-*.yml` de test: los `@SpringBootTest` arrancan sin perfil activo (modo desarrollo), así que el cambio no altera la suite.
- `README.md` línea 42: fila `SPRING_PROFILES_ACTIVE` de la tabla de variables; describe el modo permisivo de `SecretosGuard` y termina con una oración que limita el aborto del seed del admin al perfil `prod`. Línea 80 ("Con `prod`, el backend no arranca si ...") sigue siendo cierta y no se toca.
- `scripts/demo/sembrar-demo.js` (CommonJS, sin `package.json` en el back, estilo denso con comillas dobles y punto y coma): constantes `B` (API, por defecto `http://localhost:8080/api`), `AGENCIAS` (6 con `nombre`, `zona`, `direccion`, `telefonoContacto`, `emailContacto`, `descripcion`), `AUTOS` (11 filas), `esLocal`, `clave`; un IIFE async que: valida `ADMIN_EMAIL`/`ADMIN_PASSWORD` y la guarda de `LIMPIAR` remoto (antes de cualquier request, con `process.exit(1)`), hace login, `LIMPIAR`, el chequeo de idempotencia (aborta si ya hay autos de la demo salvo `FORZAR=1`, con `process.exitCode = 1; return` porque en Windows `exit()` con fetch pendientes aborta Node), y en las líneas 61-66 recorre `AGENCIAS`: si existe una con el mismo nombre le hace un `PUT` completo con los datos de ejemplo (conservando solo el logo); si no, `POST`. Después crea cada auto con `POST /publicaciones`, sube 5 fotos desde `scripts/demo/fotos/` (carpeta que hoy no existe en esta máquina), y aplica destacado y estado.
- API de agencias: `GET /api/agencias` devuelve `AgenciaResponse` (`id`, `nombre`, `slug`, `logo`, `descripcion`, `direccion`, `telefonoContacto`, `emailContacto`, `zona`, `fechaAlta`). `PUT /api/agencias/{id}` recibe `AgenciaRequest` (`nombre` y `emailContacto` obligatorios, `emailContacto` con `@Email`; `logo`, `descripcion`, `direccion`, `telefonoContacto`, `zona` opcionales) y REEMPLAZA todos esos campos (lo que no se manda queda en null). `DELETE /api/agencias/{id}` da 400 si la agencia tiene autos. `DELETE /api/publicaciones/{id}` borra en cascada fotos (Cloudinary), favoritos y consultas. `GET /api/admin/publicaciones` lista todos los autos con `id`, `marca`, `modelo`, `anio`.
- `.gitignore` del back ya ignora `scripts/demo/fotos/`.
- Node v24.14.1. `node --test <archivo>` funciona; `node --test <directorio>` falla en esta máquina (problema previo, ver 02-REVIEW-FIX.md).

Front (verificado):
- `src/utils/catalogoParams.js` (ESM puro, sin React): `FILTROS_LISTA = ['marca', 'modelo', 'color', 'transmision', 'tipo', 'zona', 'estado']`, `FILTROS_ESCALAR = ['busqueda', 'anioMin', 'anioMax', 'kmMax', 'precioMin', 'precioMax', 'ofertas', 'orden']`. `leerFiltros(searchParams)` (líneas 14-24) normaliza `pagina` y copia el resto sin validar (listas: `getAll` sin vacíos; escalares: `get ?? ''`). `bandasDePrecio(min, max)` (líneas 80-89): 4 bandas; con `min === max` devuelve una sola banda `{desde: min, hasta: max}`; `corte(i)` es `hastaMax` en la última y `Math.round(desdeMin + paso * i)` en el resto (incluida la primera).
- `src/utils/etiquetas.js` (ESM puro): exporta `TRANSMISION` (`MANUAL`, `AUTOMATICA`), `TIPO_CARROCERIA` (`SEDAN`, `HATCHBACK`, `SUV`, `PICKUP`, `UTILITARIO`, `COUPE`, `MONOVOLUMEN`, `FAMILIAR`), `ZONA` (`CABA`, `ZONA_NORTE`, `ZONA_SUR`, `ZONA_OESTE`, `INTERIOR`) y `ESTADO` (`DISPONIBLE`, `RESERVADO`, `VENDIDO`, con objetos como valor). Coinciden con los enums del back. El back convierte los enums distinguiendo mayúsculas.
- Tipos del back para los filtros (`FiltrosCatalogo`): `anioMin`, `anioMax`, `kmMax` son `Integer` (un número que no entra en un int da 400); `precioMin`, `precioMax` son `BigDecimal`; `ofertas` es `Boolean`; `orden` es `String` y el back ya tolera valores desconocidos; `busqueda`, `marca`, `modelo`, `color` son texto libre.
- Escritores de la URL: el precio del panel escribe solo dígitos (`replace(/\D/g, '')`); año y km son `<input type="number">`; el checkbox de ofertas escribe `'true'` o vacío; los links de la Home usan `/autos?precioMin=${banda.desde}&precioMax=${banda.hasta}`. `AutosPage` (línea 245) ya calcula `precioTope` con `Math.floor`/`Math.ceil` y no se toca.
- `src/utils/catalogoParams.test.js`: 9 tests `node:test` (cada uno empieza en columna 0 con `test(`), importa de `./catalogoParams.js`. El test de bandas con `bandasDePrecio(9000000, 9000000)` espera `[{ desde: 9000000, hasta: 9000000 }]` y tiene que seguir pasando.
- `src/pages/CreditosPage.jsx`: `<main className="mx-auto max-w-4xl px-4 py-16">`, un párrafo `mt-3 text-sm text-slate-500` que explica que las fotos son de Wikimedia Commons bajo licencias Creative Commons; cada foto tiene un link "Ver original".
- Estilo del front: sin punto y coma, comillas simples, 2 espacios, comentarios en español que explican el porqué.

Entorno (solo JDK 17; Maven offline). En Git Bash, antes de cada `mvn`:
export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"
y siempre `mvn -B -o -Djava.version=17 ...`. Build del front: `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build`. Tests del front: `node --test src/utils/catalogoParams.test.js` desde la raíz del front. `core.autocrlf=true`: los greps anclados con `$` o `-x` pasan antes por `tr -d '\r'`.

Mensajes de commit: convención de los fixes del review, en español sin tildes, por ejemplo `fix(02): IN-01 ...`, `fix(02): WR-02 front ...`, `docs(02): ...`.
</interfaces>
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1 (back): IN-01 un único criterio de modo desarrollo para DataSeeder y SecretosGuard</name>
  <files>src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java, src/test/java/com/danteautomotores/config/EntornoDeDesarrolloTest.java, src/main/java/com/danteautomotores/config/SecretosGuard.java, src/main/java/com/danteautomotores/config/DataSeeder.java, src/test/java/com/danteautomotores/config/DataSeederTest.java, README.md</files>
  <precondition>Docker Desktop corriendo, para que `docker compose up -d` levante el contenedor `danteautomotores-db` en localhost:5433 (lo exige el segundo automated con -Ddante.pg.required=true).</precondition>
  <behavior>
    - EntornoDeDesarrolloTest: sin perfiles activos y con el perfil por defecto de Spring (`default`) es desarrollo; `dev`, `local`, `test` y `dev,test` son desarrollo; `prod`, `production`, `railway`, `staging` y `qa` no lo son; `prod,dev` no lo es; sin perfiles activos con `setDefaultProfiles("prod")` no lo es; sin perfiles activos con `setDefaultProfiles("dev")` sí lo es; con perfil activo `dev` y por defecto `prod` es desarrollo (los perfiles activos mandan, como en Spring).
    - DataSeederTest (nuevos, parametrizados): sin admin y con variables faltantes, `production`, `railway`, `staging` y `qa` lanzan `IllegalStateException` con `ADMIN_EMAIL` en el mensaje y nunca guardan; `dev`, `local` y `test` solo avisan (el log contiene `ADMIN_EMAIL`) y no guardan; `prod,dev` lanza; sin perfil activo pero con perfil por defecto `prod` lanza; con admin existente y perfil `staging` no lanza aunque falten las variables y no guarda nada.
    - Los tests existentes de DataSeederTest y SecretosGuardTest siguen pasando sin cambios de expectativa.
  </behavior>
  <action>
RED primero: crear `src/test/java/com/danteautomotores/config/EntornoDeDesarrolloTest.java` (JUnit 5 + `MockEnvironment`, `@ParameterizedTest @ValueSource` donde aplique) con los casos de `<behavior>`, y sumar a `DataSeederTest` un helper `seederConPerfiles(String[] activos, String[] porDefecto, email, password, nombre)` (el helper existente `seeder(boolean prod, ...)` se conserva y puede delegar en el nuevo) más los tests parametrizados nuevos de `<behavior>`. Correrlos y confirmar que fallan (la clase del helper no existe y el seed hoy solo aborta con `prod`). Commit `test(02): IN-01 tests del criterio unico de modo desarrollo`.

GREEN: crear `src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java`, clase `public final` con constructor privado, con la constante `static final Set<String> PERFILES_DE_DESARROLLO = Set.of("dev", "local", "test");` (escrita exactamente así: el gate verifica que sea el único lugar de `src/main/java` con esa lista), una constante privada para el nombre del perfil por defecto de Spring (`"default"`) y el método `public static boolean esDesarrollo(Environment environment)`: si hay perfiles activos, es desarrollo solo si todos están en `PERFILES_DE_DESARROLLO`; si no hay ninguno activo, se miran `environment.getDefaultProfiles()` y es desarrollo solo si cada uno es de desarrollo o es el `default` implícito. Javadoc en español que explique el criterio, que lo comparten `SecretosGuard` y `DataSeeder`, y por qué se miran los perfiles por defecto (un `SPRING_PROFILES_DEFAULT=prod` sin perfil activo es producción; antes `DataSeeder` lo trataba así y `SecretosGuard` no).

En `SecretosGuard`: eliminar su conjunto propio de perfiles y su método privado de chequeo; `fallarOAvisar` usa `EntornoDeDesarrollo.esDesarrollo(environment)` (literal, con ese nombre de variable). Quitar imports que queden sin uso (`Arrays`, `Set`). Actualizar el Javadoc: el modo permisivo aplica sin perfil activo (y con el perfil por defecto de Spring) o con todos los perfiles en dev/local/test, y el criterio es el mismo que el de `DataSeeder` porque ambos usan `{@link EntornoDeDesarrollo}`.

En `DataSeeder`: `fallarOAvisar` lanza `IllegalStateException(mensaje)` cuando `!EntornoDeDesarrollo.esDesarrollo(environment)` (literal) en vez de la comprobación del perfil exacto `prod`; en modo desarrollo hace `log.warn` con el mensaje y un sufijo que diga que fuera del modo desarrollo el arranque se aborta (sin incluir nunca la contraseña). Quitar el import de `Profiles`. Actualizar el comentario del método (hoy dice "En prod ...") para que hable de "fuera del modo desarrollo". No cambia nada más del seed (agencia inicial, normalización del email, no promover cuentas).

`README.md`, fila `SPRING_PROFILES_ACTIVE` (línea 42): decir que `SecretosGuard` y `DataSeeder` comparten el criterio (`EntornoDeDesarrollo`, nombrado así en el texto), que son permisivos solo sin perfil activo (con el perfil por defecto de Spring; un `SPRING_PROFILES_DEFAULT` que no sea de desarrollo cuenta como producción) o con todos los perfiles en `dev`, `local` o `test`; y reemplazar la oración final que limita al perfil `prod` el aborto por las variables del admin por una que diga que fuera del modo desarrollo el backend tampoco arranca si faltan (o son inválidas) las variables del admin y todavía no existe ninguno. No tocar la línea 80.

Correr los tests (verde) y commitear en el back: `fix(02): IN-01 DataSeeder y SecretosGuard comparten el criterio de modo desarrollo`.
  </action>
  <verify>
    <automated>export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -o -q -Djava.version=17 test -Dtest='EntornoDeDesarrolloTest,DataSeederTest,SecretosGuardTest' && grep -qF 'EntornoDeDesarrollo.esDesarrollo(environment)' src/main/java/com/danteautomotores/config/DataSeeder.java && grep -qF 'EntornoDeDesarrollo.esDesarrollo(environment)' src/main/java/com/danteautomotores/config/SecretosGuard.java && ! grep -q 'acceptsProfiles' src/main/java/com/danteautomotores/config/DataSeeder.java && [ "$(grep -rlF '"dev", "local", "test"' src/main/java)" = "src/main/java/com/danteautomotores/config/EntornoDeDesarrollo.java" ] && grep -q 'EntornoDeDesarrollo' README.md && ! grep -q 'con .prod. el backend tampoco arranca' README.md && echo "Task 1 unit OK"</automated>
    <fails_when>Falla algún test de EntornoDeDesarrolloTest (un perfil de producción, una mezcla con prod o un perfil por defecto prod cuenta como desarrollo, o sin perfil no cuenta como desarrollo), de DataSeederTest (el seed no aborta con production/railway/staging/qa, prod,dev o default prod; aborta con dev/local/test; o un test viejo cambió de resultado) o de SecretosGuardTest; o DataSeeder o SecretosGuard no llaman a EntornoDeDesarrollo.esDesarrollo(environment); o DataSeeder sigue comparando con el perfil exacto prod; o la lista dev/local/test aparece en otro archivo de src/main/java además del helper (o no aparece en él); o el README no nombra EntornoDeDesarrollo o conserva la oración que limitaba el aborto del seed al perfil prod.</fails_when>
    <automated>docker compose up -d >/dev/null 2>&1; export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"; mvn -B -o -q -Djava.version=17 test -Ddante.pg.required=true && echo "Suite completa con Postgres OK"</automated>
    <fails_when>Falla cualquier test de la suite completa del back (por ejemplo un @SpringBootTest que ahora aborte en el arranque porque el helper no trata como desarrollo el caso sin perfil activo), o no hay Postgres en localhost:5433 (con dante.pg.required=true los tests de Postgres fallan en vez de saltearse).</fails_when>
  </verify>
  <done>Existe EntornoDeDesarrollo.esDesarrollo(Environment) y es la única definición de "modo desarrollo" del back; DataSeeder y SecretosGuard la usan; con cualquier perfil que no sea de desarrollo el seed aborta si faltan las variables del admin y no hay admin; tests nuevos y viejos en verde; suite completa en verde con Postgres; README coherente; commits test + fix en el back.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2 (front): IN-06 bandas con floor/ceil, WR-02 leerFiltros valida la URL, IN-09 créditos indican cambios</name>
  <files>src/utils/catalogoParams.js, src/utils/catalogoParams.test.js, src/pages/CreditosPage.jsx</files>
  <behavior>
    - leerFiltros con `tipo=SUV&tipo=NAVE&zona=norte&zona=CABA&transmision=automatica&transmision=MANUAL&estado=roto&estado=VENDIDO` da tipo ['SUV'], zona ['CABA'], transmision ['MANUAL'], estado ['VENDIDO'].
    - leerFiltros con `tipo=constructor&tipo=toString&zona=__proto__&estado=hasOwnProperty&transmision=valueOf` deja las cuatro listas vacías (no se aceptan claves heredadas del prototipo).
    - Escalares numéricos: `anioMin=abc`, `anioMax=20x0`, `kmMax=-5`, `precioMin=1e6`, `precioMax=15.000.000` quedan todos en ''; `kmMax=99999999999` (no entra en un int de Java) queda en ''; valores válidos se conservan: `anioMin=2018` da '2018', `anioMax=%202022%20` da '2022' (se recortan espacios), `kmMax=50000` da '50000', `precioMin=17200000.5` da '17200000.5', `precioMax=20000000` da '20000000'.
    - `ofertas=true` da 'true'; `ofertas=si`, `ofertas=1` y `ofertas=TRUE` dan ''.
    - El texto libre no se valida: `marca=Marca Rara`, `modelo=X`, `color=Verde agua`, `busqueda=lo que sea` y `orden=desconocido` se conservan tal cual (el back ya tolera `orden` desconocido).
    - `paramsParaApi(leerFiltros(new URLSearchParams('tipo=NAVE&anioMin=abc&ofertas=si&marca=Fiat'))).toString()` es exactamente 'marca=Fiat&pagina=1'.
    - `bandasDePrecio(17200000.5, 47500000.4)`: 4 bandas; la primera empieza en 17200000 y la última termina en 47500001; todas las puntas son enteras, consecutivas y con hasta > desde; el mínimo 17200000.5 queda dentro de la primera banda y el máximo 47500000.4 dentro de la última.
    - `bandasDePrecio(9000000.5, 9000000.5)` da [{ desde: 9000000, hasta: 9000001 }]; `bandasDePrecio(9000000, 9000000)` sigue dando [{ desde: 9000000, hasta: 9000000 }].
    - Los 9 tests existentes siguen pasando sin cambios.
  </behavior>
  <action>
Repo: front (`C:/Users/toto/Desktop/work/danteautomotores-front`). RED primero: sumar a `src/utils/catalogoParams.test.js` al menos 6 tests nuevos con los casos de `<behavior>` (enums desconocidos, claves del prototipo, escalares numéricos, ofertas, texto libre conservado + `paramsParaApi` sin lo descartado, bandas con centavos y banda única con centavos), cada uno como `test(` en columna 0 y con nombres en español sin tildes como los existentes. Correr `node --test src/utils/catalogoParams.test.js` y confirmar que fallan. Commit en el front: `test(02): WR-02 front e IN-06 tests de leerFiltros y bandasDePrecio`.

GREEN, parte A (IN-06), en `bandasDePrecio`: la primera punta pasa a `Math.floor(desdeMin)` y la última a `Math.ceil(hastaMax)` (expresiones literales, el gate las busca); los cortes intermedios siguen con `Math.round(desdeMin + paso * i)`. El caso de un solo precio devuelve una banda `{ desde: Math.floor(desdeMin), hasta: Math.ceil(hastaMax) }`. Actualizar el comentario de la función: las puntas se redondean hacia afuera para que el auto más barato y el más caro entren en su propia banda (mismo criterio que `precioTope` de `AutosPage`).

GREEN, parte B (WR-02 front), en `leerFiltros`: importar `ESTADO`, `TIPO_CARROCERIA`, `TRANSMISION` y `ZONA` desde `'./etiquetas.js'` (un solo import, con esa ruta literal) y armar una tabla de valores válidos por filtro de lista (`tipo` con TIPO_CARROCERIA, `zona` con ZONA, `transmision` con TRANSMISION, `estado` con ESTADO). Para esas cuatro claves se conservan solo los valores que son claves propias del mapa, comparando exacto y con mayúsculas como el back (usar `Object.keys(mapa).includes(valor)`, no el operador `in`, para que `constructor` o `__proto__` no pasen). `marca`, `modelo` y `color` siguen sin validar (solo se descartan vacíos). Escalares: cada valor se recorta con `trim()`; `anioMin`, `anioMax` y `kmMax` se conservan solo si son dígitos (`^\d+$`) y su valor numérico es menor o igual a 2147483647 (el `Integer` del back); `precioMin` y `precioMax` solo si son un número no negativo con decimales opcionales (`^\d+(\.\d+)?$`, el `BigDecimal` del back); `ofertas` solo si es exactamente `'true'` (lo único que escribe el checkbox); `busqueda` y `orden` quedan como hoy. Lo descartado queda como lista vacía o `''`, igual que un filtro no pedido. No cambiar `FILTROS_LISTA`, `FILTROS_ESCALAR`, `aSearchParams`, `paramsParaApi` ni la normalización de `pagina`. Actualizar el comentario de cabecera de `leerFiltros`: además de descartar parámetros desconocidos, descarta valores que el back rechazaría con 400 para que un link viejo, compartido o mal escrito no deje la página sin resultados (el back también valida y responde un 400 legible, ver 02-REVIEW-FIX.md).

Parte C (IN-09), en `src/pages/CreditosPage.jsx`: agregar, debajo del párrafo introductorio y con el mismo estilo (`mt-3 text-sm text-slate-500`), un párrafo que diga que las imágenes se muestran modificadas: fueron recortadas, redimensionadas y comprimidas para su visualización en este sitio, y que los originales sin cambios están en el enlace "Ver original" de cada foto. La frase tiene que contener literalmente "recortadas, redimensionadas y comprimidas".

Correr los tests (verde) y el build, y commitear en el front: `fix(02): IN-06 WR-02 IN-09 bandas con floor/ceil, leerFiltros valida la URL y creditos indican cambios` (o dos commits si se prefiere separar IN-09).
  </action>
  <verify>
    <automated>node --test src/utils/catalogoParams.test.js && npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build && grep -qF "from './etiquetas.js'" src/utils/catalogoParams.js && grep -qF 'Math.floor(desdeMin)' src/utils/catalogoParams.js && grep -qF 'Math.ceil(hastaMax)' src/utils/catalogoParams.js && [ "$(grep -c '^test(' src/utils/catalogoParams.test.js)" -ge 15 ] && grep -q 'NAVE' src/utils/catalogoParams.test.js && grep -q '__proto__' src/utils/catalogoParams.test.js && grep -q '17200000.5' src/utils/catalogoParams.test.js && grep -q '99999999999' src/utils/catalogoParams.test.js && grep -q 'recortadas, redimensionadas y comprimidas' src/pages/CreditosPage.jsx && echo "Task 2 OK"</automated>
    <fails_when>Falla algún test de catalogoParams.test.js (un enum desconocido o una clave del prototipo pasa el filtro, un escalar no numérico o fuera de int se conserva, un valor válido se descarta, ofertas acepta algo distinto de 'true', paramsParaApi manda lo descartado, la primera banda no empieza en el piso del mínimo o la última no termina en el techo del máximo, o se rompió un test existente); el build de Vite falla; catalogoParams.js no importa de './etiquetas.js' o no usa Math.floor(desdeMin) y Math.ceil(hastaMax); hay menos de 15 tests; faltan los casos NAVE, __proto__, 17200000.5 o 99999999999; o CreditosPage no tiene la frase de cambios.</fails_when>
  </verify>
  <done>Un link con valores de enum o números inválidos muestra el catálogo con el resto de sus filtros válidos (sin 400 y sin página vacía); las bandas de presupuesto incluyen al auto más barato y al más caro aunque tengan centavos; /creditos indica los cambios hechos a las imágenes; tests (15 o más) y build en verde; commits en el front.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3 (back): IN-10 sembrar-demo no pisa agencias y registra lo creado para poder deshacerlo; disposición del review</name>
  <files>scripts/demo/sembrar-demo.js, scripts/demo/sembrar-demo.test.js, scripts/demo/README.md, .gitignore, .planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md</files>
  <behavior>
    - cuerpoParaCompletarZona(existente, demo): si la agencia existente no tiene zona (null, undefined o '') y la de la demo sí, devuelve exactamente { nombre, logo, descripcion, direccion, telefonoContacto, emailContacto } de la EXISTENTE más zona de la demo (sin id, slug ni fechaAlta, y nunca la dirección, el teléfono, el email ni la descripción de ejemplo); si la existente ya tiene zona devuelve null; si la demo no trae zona devuelve null.
    - planDeDeshacer(registro, publicacionesActuales, agenciasActuales): un auto registrado que sigue en el backend con el mismo id, marca, modelo y anio va a borrar; uno que ya no está va a yaNoEstan; uno con el mismo id y otros datos va a noCoinciden y no se borra. Igual con agencias comparando id y nombre. Un registro sin listas (o con listas vacías) da todas las listas vacías.
    - rutaDelRegistro(api, env): con env.REGISTRO definido devuelve ese valor; si no, `scripts/demo/registros/<host>.json` junto al script, donde el host (con puerto) pasa a minúsculas y todo carácter fuera de [a-z0-9.-] se reemplaza por '_': 'http://localhost:8080/api' da 'localhost_8080.json' y 'https://Dante-Back.up.railway.app/api' da 'dante-back.up.railway.app.json'.
    - Guardas sin red (API=http://127.0.0.1:9/api): DESHACER=1 sin archivo de registro sale con código distinto de 0 y 'No hay registro'; DESHACER=1 con un registro de otro backend sale con 'es de otro backend'; una corrida normal con un registro ya existente sale con 'Ya hay un registro'; DESHACER=1 junto con LIMPIAR=1 sale con 'no se combina con LIMPIAR'; DESHACER=1 contra un backend no local sin CONFIRMAR_BORRADO_EN_PRODUCCION=SI sale nombrando esa variable. Ninguno de esos casos llega a hacer un request.
  </behavior>
  <action>
Repo: back. No ejecutar el script contra ningún backend (ni local); solo los tests y las guardas sin red del `<verify>`.

RED primero: crear `scripts/demo/sembrar-demo.test.js` (CommonJS: `require('node:test')`, `require('node:assert/strict')`, `require('path')` y `require('./sembrar-demo.js')`) con los casos de `<behavior>` para `cuerpoParaCompletarZona`, `planDeDeshacer` y `rutaDelRegistro` (para la ruta, comparar con `path.basename` y que el directorio padre se llame `registros`, así el test pasa con separadores de Windows). Correr `node --test scripts/demo/sembrar-demo.test.js` y confirmar que falla. Commit: `test(02): IN-10 tests de las funciones puras de sembrar-demo`.

GREEN, parte A (`scripts/demo/sembrar-demo.js`, manteniendo su estilo):
1. Pasar el IIFE a una función `async function main()` y ejecutarla solo con `if (require.main === module)`, con el mismo `.catch` que imprime el mensaje y sale con 1; exportar con `module.exports` `cuerpoParaCompletarZona`, `planDeDeshacer` y `rutaDelRegistro` (funciones puras, sin red ni disco salvo `path`).
2. Agencias existentes (per IN-10): si una agencia del backend tiene el mismo nombre que una de la demo, se usa su id y NO se le mandan los datos de ejemplo. Solo si `cuerpoParaCompletarZona(existente, demo)` no es null se hace `PUT /agencias/{id}` con ese cuerpo (los propios datos de la agencia + la zona); si ese PUT falla (por ejemplo, un email viejo que hoy no pasa `@Email`), se avisa por consola con el motivo y se sigue con la agencia tal como está. Loguear qué se hizo con cada agencia existente (sin cambios / zona completada / zona no se pudo completar). Las agencias nuevas siguen con `POST` como hoy.
3. Registro de la corrida: `rutaDelRegistro(B, process.env)`. El archivo es JSON con `api` (el valor de `B`), `inicio` (fecha ISO), `completa` (false hasta el final), `agencias` (lista de `{ id, nombre }` creadas por esta corrida) y `publicaciones` (lista de `{ id, marca, modelo, anio }`). Nunca guarda el email, la contraseña ni el token del admin. Se crea (con `mkdirSync` recursivo) justo antes de crear la primera agencia o auto, después del login, de `LIMPIAR` y del chequeo de idempotencia, y se reescribe completo después de cada POST exitoso: cada agencia nueva después de su `POST /agencias` y cada auto inmediatamente después de su `POST /publicaciones` (antes de subir fotos, así un corte durante las fotos igual lo deja registrado). Al terminar bien se marca `completa: true`, se guarda y se imprime la ruta del registro y el comando para deshacer la corrida con `DESHACER=1`. Las agencias existentes no se registran (no las creó el script).
4. Guardas nuevas, todas antes de cualquier request y con `process.exit(1)` como las existentes, en este orden: (a) la guarda existente de `ADMIN_EMAIL`/`ADMIN_PASSWORD`; (b) `DESHACER=1` junto con `LIMPIAR=1` se rechaza con un mensaje que contenga "DESHACER=1 no se combina con LIMPIAR=1"; (c) la guarda existente de `LIMPIAR` remoto; (d) `DESHACER=1` contra un backend no local exige `CONFIRMAR_BORRADO_EN_PRODUCCION=SI` (mensaje que nombre esa variable y aclare que solo se borra lo registrado); (e) en modo `DESHACER=1`: si el archivo de registro no existe, salir con un mensaje que empiece con "No hay registro de corrida en <ruta>"; si existe y su `api` (sin barras finales) no es igual a `B` (sin barras finales), salir con un mensaje que contenga "El registro <ruta> es de otro backend (<api del registro>)"; (f) en una corrida normal (incluida `LIMPIAR=1`), si el archivo de registro ya existe, salir con un mensaje que contenga "Ya hay un registro de una corrida anterior contra este backend en <ruta>" y explique que se deshace con `DESHACER=1` o, si se quiere conservar esa carga, se borra el archivo.
5. Modo `DESHACER=1` (después del login): leer el registro, pedir `GET /admin/publicaciones` y `GET /agencias`, calcular `planDeDeshacer`, borrar primero los autos (`DELETE /publicaciones/{id}`) y después las agencias (`DELETE /agencias/{id}`), capturando el error de cada borrado (por ejemplo, una agencia que ahora tiene otros autos da 400) para seguir con el resto. Imprimir qué se borró, qué ya no estaba, qué no coincidía y qué falló. Si no quedó nada pendiente (sin "no coinciden" ni fallidos), borrar el archivo de registro; si no, reescribirlo solo con lo pendiente y terminar con `process.exitCode = 1`. Nunca borra nada que no esté en el registro.

Parte B (IN-10, docs): en `.gitignore` agregar la línea `scripts/demo/registros/` (con un comentario: registros locales de corridas de la demo). En `scripts/demo/README.md`: reemplazar la explicación de que las agencias existentes se completan con un PUT completo por el comportamiento nuevo (no se tocan sus datos; solo se completa la zona si está vacía, reenviando sus propios datos); reemplazar el párrafo "Si una corrida se corta a la mitad" por una sección sobre el registro (`scripts/demo/registros/<host>_<puerto>.json`, fuera de git, qué contiene y que no guarda credenciales, que una corrida nueva contra el mismo backend se niega si ya hay un registro) y cómo deshacer con `DESHACER=1` (ejemplo de comando contra `http://localhost:8080/api`, qué verifica antes de borrar, qué pasa con lo pendiente, y que contra un backend no local exige `CONFIRMAR_BORRADO_EN_PRODUCCION=SI`); sumar a la tabla de variables las filas `DESHACER=1` y `REGISTRO` (ruta alternativa del archivo de registro); en "Contra producción", mencionar que el registro de la carga queda en la máquina de quien la corrió.

Correr los tests y las guardas, y commitear en el back: `fix(02): IN-10 sembrar-demo no pisa agencias existentes y registra lo creado para poder deshacerlo`.

Parte C (disposición), en `.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md`: las filas de IN-01, IN-06, IN-09 e IN-10 pasan a `| IN-0X | info | fixed | quick 261003-sfp |` (mismo formato de columnas que las de quick 261003-qde); la fila de WR-02 conserva `fixed` y su Source pasa a decir que el back se corrigió en `02-REVIEW-FIX.md` y la parte del front (`leerFiltros`) en `quick 261003-sfp`; IN-02, IN-05 e IN-08 quedan en `open`; el contador pasa a `open: 3`; actualizar `updated:` del frontmatter con la fecha y hora actual (UTC, ISO). No tocar las demás filas. Commit en el back: `docs(02): disposicion del review con IN-01 IN-06 IN-09 IN-10 y WR-02 front cerrados`.
  </action>
  <verify>
    <automated>node --check scripts/demo/sembrar-demo.js && node --test scripts/demo/sembrar-demo.test.js && grep -qF 'require.main === module' scripts/demo/sembrar-demo.js && grep -q 'module.exports' scripts/demo/sembrar-demo.js && grep -q 'rutaDelRegistro' scripts/demo/sembrar-demo.js && ! grep -qF '...a, logo' scripts/demo/sembrar-demo.js && echo "Task 3 unit OK"</automated>
    <fails_when>El script tiene un error de sintaxis; falla algún test de sembrar-demo.test.js (el cuerpo del PUT trae datos de ejemplo o campos de más, se manda PUT a una agencia que ya tiene zona, planDeDeshacer borra un auto o agencia cuyos datos no coinciden o no borra uno que sí, la ruta del registro no se sanea o ignora REGISTRO); requerir el script ejecuta la carga (falta la guarda require.main); no exporta las funciones; o sigue el PUT que mezclaba los datos de ejemplo con el logo de la agencia existente.</fails_when>
    <automated>T=$(mktemp -d); export ADMIN_EMAIL=demo@local.test ADMIN_PASSWORD=x API=http://127.0.0.1:9/api; o1=$(DESHACER=1 REGISTRO="$T/no-existe.json" node scripts/demo/sembrar-demo.js 2>&1); c1=$?; printf '{"api":"http://otro-backend.example/api","agencias":[],"publicaciones":[]}' > "$T/otro.json"; o2=$(DESHACER=1 REGISTRO="$T/otro.json" node scripts/demo/sembrar-demo.js 2>&1); c2=$?; o3=$(REGISTRO="$T/otro.json" node scripts/demo/sembrar-demo.js 2>&1); c3=$?; o4=$(DESHACER=1 LIMPIAR=1 REGISTRO="$T/otro.json" node scripts/demo/sembrar-demo.js 2>&1); c4=$?; o5=$(DESHACER=1 API=https://remoto.example/api REGISTRO="$T/no-existe.json" node scripts/demo/sembrar-demo.js 2>&1); c5=$?; rm -rf "$T"; [ $c1 -ne 0 ] && echo "$o1" | grep -q 'No hay registro' && [ $c2 -ne 0 ] && echo "$o2" | grep -q 'es de otro backend' && [ $c3 -ne 0 ] && echo "$o3" | grep -q 'Ya hay un registro' && [ $c4 -ne 0 ] && echo "$o4" | grep -q 'no se combina con LIMPIAR' && [ $c5 -ne 0 ] && echo "$o5" | grep -q 'CONFIRMAR_BORRADO_EN_PRODUCCION' && ! printf '%s\n' "$o1$o2$o3$o4$o5" | grep -qi 'fetch failed\|ECONNREFUSED\|ENOTFOUND' && echo "Task 3 guardas OK"</automated>
    <fails_when>Alguna de las cinco guardas nuevas no existe, no sale con código distinto de 0, no imprime su frase, está en otro orden (por ejemplo, DESHACER+LIMPIAR llega a la guarda del registro, o DESHACER remoto sin confirmación llega a buscar el archivo), o el script llega a hacer un request (aparece fetch failed, ECONNREFUSED o ENOTFOUND) antes de abortar.</fails_when>
    <automated>tr -d '\r' < .gitignore | grep -qxF 'scripts/demo/registros/' && git check-ignore -q scripts/demo/registros/localhost_8080.json && R=scripts/demo/README.md && grep -q 'DESHACER=1' $R && grep -q 'REGISTRO' $R && grep -q 'registros/' $R && ! grep -q 'y el resto de sus datos' $R && D=.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-REVIEW-DISPOSITION.md && for i in 01 06 09 10; do tr -d '\r' < $D | grep -qE "^\| IN-$i \| info \| fixed \| quick 261003-sfp \|$" || exit 1; done && for i in 03 04 07; do tr -d '\r' < $D | grep -qE "^\| IN-$i \| info \| fixed \| quick 261003-qde \|$" || exit 1; done && [ "$(tr -d '\r' < $D | grep -E '\| open \|' | cut -d'|' -f2 | tr -d ' ' | tr '\n' ',')" = "IN-02,IN-05,IN-08," ] && for i in 1 2 3 4 5 6 7; do tr -d '\r' < $D | grep -qE "^\| WR-0$i \| warning \| (fixed|deferred) \|" || exit 1; done && tr -d '\r' < $D | grep -E '^\| WR-02 \| warning \| fixed \|' | grep -q '261003-sfp' && tr -d '\r' < $D | grep -qx 'open: 3' && echo "Task 3 docs OK"</automated>
    <fails_when>.gitignore no ignora scripts/demo/registros/ (o git no lo considera ignorado); el README de la demo no documenta DESHACER=1, REGISTRO y la carpeta registros/, o conserva la explicación vieja de que a una agencia existente se le reemplazan todos los datos; en la disposición falta alguna de las filas IN-01, IN-06, IN-09 o IN-10 en fixed con Source quick 261003-sfp, se alteraron las filas de quick 261003-qde, las filas en open no son exactamente IN-02, IN-05 e IN-08 (en ese orden), la fila de WR-02 no menciona 261003-sfp o dejó de estar en fixed, alguna de las filas WR-01 a WR-07 dejó de estar en fixed o deferred, o el contador no dice open: 3.</fails_when>
  </verify>
  <done>sembrar-demo.js ya no pisa agencias existentes, registra fuera de git cada id que crea a medida que lo crea y puede deshacer una corrida parcial con DESHACER=1 verificando backend, ids y datos; todas las guardas corren antes de cualquier request; tests y guardas en verde sin tocar ningún backend; README de la demo y .gitignore actualizados; disposición con 4 IN nuevos en fixed, nota de WR-02 y open: 3; commits en el back.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Variables de entorno del deploy -> arranque del back | El perfil activo/por defecto decide si faltar el admin o los secretos aborta el arranque o solo avisa |
| URL del navegador -> SPA -> API pública | Cualquier link (compartido, viejo o armado a mano) llega a `leerFiltros` y de ahí a `GET /api/publicaciones` |
| Script de demo (máquina del operador) -> API admin del backend (posiblemente producción) | El script crea, modifica y (con DESHACER) borra datos con el token del admin |
| Archivo de registro local -> modo DESHACER | Un archivo en disco decide qué ids se borran |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-sfp-01 | Elevation of Privilege | `DataSeeder` / `SecretosGuard` (back) | medium | mitigate | Un único `EntornoDeDesarrollo.esDesarrollo` para ambos: con cualquier perfil que no sea dev/local/test (incluido `SPRING_PROFILES_DEFAULT` no de desarrollo) un deploy sin admin válido no arranca en vez de quedar sin admin en silencio; tests parametrizados en Task 1 |
| T-sfp-02 | Tampering | `leerFiltros` (front) | low | mitigate | Lista blanca contra las claves propias de `etiquetas.js` (`Object.keys(...).includes`, no `in`, para que `__proto__`/`constructor` no pasen) y regex estrictas para números; el back sigue validando y respondiendo 400 legible (02-REVIEW-FIX.md) |
| T-sfp-03 | Tampering | `sembrar-demo.js` (PUT de agencias) | high | mitigate | Una agencia real con el mismo nombre ya no se sobrescribe: solo se completa `zona` vacía reenviando sus propios datos; test de `cuerpoParaCompletarZona` en Task 3 |
| T-sfp-04 | Tampering | `sembrar-demo.js` modo DESHACER | high | mitigate | Solo borra ids del registro, y solo si el registro es del mismo backend (`api`) y cada id coincide en marca/modelo/año o nombre; los que no coinciden se saltean; contra un backend no local exige `CONFIRMAR_BORRADO_EN_PRODUCCION=SI`; `DESHACER` no se combina con `LIMPIAR`; guardas antes de cualquier request, verificadas sin red |
| T-sfp-05 | Information Disclosure | `scripts/demo/registros/*.json` | low | mitigate | El registro guarda solo URL de la API, fecha, ids, nombres de agencia y marca/modelo/año; nunca email, contraseña ni token del admin; la carpeta queda en `.gitignore` (verificado con `git check-ignore`) |
| T-sfp-06 | Repudiation | Créditos de fotos CC BY / BY-SA (front) | low | mitigate | `/creditos` declara que las imágenes fueron recortadas, redimensionadas y comprimidas, como exigen las licencias |
| T-sfp-07 | Denial of Service | Corrida de la demo cortada a la mitad | low | mitigate | El registro se reescribe después de cada POST exitoso (cada auto antes de subir sus fotos), así una corrida parcial siempre se puede deshacer con `DESHACER=1` |
| T-sfp-SC | Tampering | npm/Maven installs | low | accept | Esta tarea no instala paquetes: el front usa solo `node:test` (incluido en Node) y el back no suma dependencias (Maven offline) |
</threat_model>

<verification>
- Task 1: `EntornoDeDesarrolloTest`, `DataSeederTest` y `SecretosGuardTest` en verde, gates de uso del helper y del README, y la suite completa del back en verde con `-Ddante.pg.required=true`.
- Task 2: `node --test src/utils/catalogoParams.test.js` (15 tests o más) y `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` en verde, más gates grep de `etiquetas.js`, floor/ceil y la frase de créditos.
- Task 3: `node --check` y `node --test scripts/demo/sembrar-demo.test.js` en verde; las cinco guardas abortan sin red contra `http://127.0.0.1:9/api`; gates de `.gitignore`, README de la demo y disposición (`open: 3`).
- Repos: `git -C C:/Users/toto/Desktop/work/danteautomotores-back status --short` y el mismo en el front sin cambios sin commitear del alcance de esta tarea (salvo los archivos de planificación que commitea el orquestador); no existe `scripts/demo/registros/` con archivos nuevos; sin push; el `.env` del front y la base `danteautomotores` sin tocar.
</verification>

<success_criteria>
- `DataSeeder` y `SecretosGuard` usan el mismo criterio de modo desarrollo; con cualquier perfil que no sea de desarrollo, un deploy sin admin y sin las variables del admin no arranca.
- Un link del catálogo con enums o números inválidos muestra resultados con el resto de sus filtros; las bandas de presupuesto incluyen siempre al auto más barato y al más caro.
- `/creditos` indica que las imágenes fueron recortadas, redimensionadas y comprimidas.
- `sembrar-demo.js` no pisa agencias existentes, registra fuera de git lo que crea y puede deshacer una corrida parcial con `DESHACER=1`, documentado en `scripts/demo/README.md`.
- `02-REVIEW-DISPOSITION.md`: IN-01, IN-06, IN-09 e IN-10 en `fixed` (Source `quick 261003-sfp`), WR-02 con la nota del front y `open: 3`.
</success_criteria>

<output>
Create `.planning/quick/261003-sfp-fix-review-in-01-in-06-in-09-in-10-y-wr-/261003-sfp-SUMMARY.md` when done
</output>
