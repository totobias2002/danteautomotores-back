# danteautomotores-back

API REST del marketplace de reventa de autos usados DanteAutomotores. Java 21 + Spring Boot + PostgreSQL + JWT.

Repo hermano: [danteautomotores-front](https://github.com/totobias2002/danteautomotores-front)

## Modelo de datos

- **Usuario**: cuenta con rol ADMIN o COMPRADOR.
- **Agencia**: perfil de la agencia (página estandarizada dentro del marketplace), gestionado por el admin.
- **Publicacion**: auto en venta, vinculado a una agencia y al admin que lo cargó.
- **FotoPublicacion**: fotos de cada publicación (se suben a Cloudinary, se guarda la URL).
- **Consulta**: mensaje de contacto de un usuario sobre una publicación.
- **Favorito**: publicaciones que un usuario guardó.

No hay flujo de compra/pago dentro de la plataforma: el circuito es consulta → gestión de la venta fuera del sistema → el admin actualiza el estado de la publicación (DISPONIBLE / RESERVADO / VENDIDO). Solo el ADMIN puede crear/editar/eliminar agencias y publicaciones; el COMPRADOR navega, consulta y guarda favoritos.

## Cómo levantar el entorno de desarrollo

1. Base de datos: `docker compose up -d` (levanta Postgres 16 en el puerto **5433** del host, para no chocar con un Postgres local en el 5432; usuario `dante`, base `danteautomotores`).
2. Backend: `mvn spring-boot:run`, o directamente corriendo la clase `DanteAutomotoresApplication` desde tu IDE (IntelliJ, con Java 21 configurado). En una máquina que solo tiene JDK 17 se compila con `-Djava.version=17` (el `pom.xml` y el `Dockerfile` usan Java 21).

## Variables de entorno

Ver `src/main/resources/application.yml`. Para producción, sobreescribir `app.jwt.secret` y las credenciales de `cloudinary` por variables de entorno reales — nunca commitear secretos.

## Variables de entorno para producción (Railway, Render, etc.)

| Variable | Descripción |
|---|---|
| `PORT` | Puerto HTTP (Railway/Render la inyectan solos). |
| `SPRING_DATASOURCE_URL` | URL JDBC de la base Postgres, ej: `jdbc:postgresql://host:5432/db`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base. Fuera del modo desarrollo (ver `SPRING_PROFILES_ACTIVE`), el backend no arranca si se deja la contraseña de desarrollo del repo. |
| `APP_JWT_SECRET` | **Obligatoria en producción.** Secreto para firmar JWT — al menos 32 caracteres y distinto del valor de ejemplo del repo. Si falta, es el valor de ejemplo (público: con él cualquiera podría falsificar tokens, incluso de admin) o tiene menos de 32 bytes, el backend no arranca (salvo en modo desarrollo, ver `SPRING_PROFILES_ACTIVE`). |
| `APP_JWT_EXPIRATION_MS` | Duración del token en ms (opcional, default 24hs). |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma, ej: `https://mi-app.vercel.app`. |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Credenciales de Cloudinary para las fotos. **Obligatorias fuera del modo desarrollo**: sin las tres el backend no arranca (solo se verifica que no estén vacías; una credencial mal escrita se detecta recién al subir una foto). |
| `ADMIN_EMAIL` | Email de la cuenta admin (se recortan los espacios y se pasa a minúsculas; el log dice con qué email hay que ingresar). Solo se usa para crear la cuenta si no existe ningún admin; no puede ser el de una cuenta ya registrada. |
| `ADMIN_PASSWORD` | Contraseña de la cuenta admin (mínimo 8 caracteres). Solo se usa para crear la cuenta si no existe ningún admin; cambiarla después no modifica la cuenta. |
| `ADMIN_NOMBRE` | Nombre visible del admin (por ejemplo, Dante). Mismo uso que `ADMIN_EMAIL`. |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring. El `Dockerfile` lo deja en `prod` por defecto. `SecretosGuard` solo es permisivo (avisa en el log y arranca) cuando **no hay ningún perfil activo** (desarrollo local con `mvn spring-boot:run` o el IDE, sin configurar nada) o cuando todos los perfiles activos son `dev`, `local` o `test`. Con cualquier otro perfil (`prod`, `production`, `railway`, `staging`...) o una mezcla como `prod,dev`, el backend no arranca si `APP_JWT_SECRET` falta, es el valor de ejemplo o tiene menos de 32 bytes, ni si `SPRING_DATASOURCE_PASSWORD` es la contraseña de desarrollo del repo. Si desplegás **sin el Dockerfile** (por ejemplo el build nativo de Railway), definí `SPRING_PROFILES_ACTIVE=prod`: sin perfil el guard queda en modo desarrollo. Además, con `prod` el backend tampoco arranca si faltan (o son inválidas) las variables del admin y todavía no existe ninguno. |

El repo incluye un `Dockerfile` (build multi-stage con Maven + JDK 21) listo para deployar en Railway, Render o cualquier hosting que soporte contenedores.

Al arrancar, si no hay ninguna agencia se crea "Dante Automotores" (el resto de sus datos se completa desde el panel). Las cuentas admin no se pueden crear desde la web: el registro público siempre crea compradores.

## Base de datos y migraciones

El esquema lo crea y versiona Flyway desde `src/main/resources/db/migration`:

- `V1` es el esquema previo a la Fase 1; `V2` y `V3` suman los cambios de las fases 1 y 2. Una base que ya existía sin historial de Flyway (por ejemplo la de producción, creada antes por Hibernate) se marca como V1 (baseline) y recibe solo las migraciones siguientes.
- **Nunca se edita una migración ya aplicada.** Todo cambio de entidad va en una migración nueva (`V4__...`, `V5__...`).
- Hibernate está en `ddl-auto: validate`: si una entidad y el esquema no coinciden, el arranque se frena con el error. No crea ni modifica tablas.
- El SQL se puede loguear en local con `SPRING_JPA_SHOW_SQL=true` (por defecto está apagado).
- Los tests contra Postgres usan bases descartables `test_*` en localhost:5433 y se saltean si no hay Postgres; para que fallen en vez de saltearse se corre con `-Ddante.pg.required=true`.

## Probar el catálogo en local

El `.env` del front puede apuntar al backend de producción, así que para probar se levanta un front aparte en otro puerto sin tocarlo:

1. `docker compose up -d` (Postgres en el 5433).
2. Backend con el origen del front de prueba permitido (la lista admite espacios después de las comas):
   `APP_CORS_ALLOWED_ORIGINS="http://localhost:5173, http://localhost:5174" mvn spring-boot:run`
3. Front de prueba, en el repo `danteautomotores-front`: `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174`.

Para verificar el catálogo sin tocar la base de desarrollo: `bash scripts/verify/con-back-local.sh --copia-de <base> <base_descartable> node scripts/verify/catalogo-humo.js` levanta el back contra una copia (o con `--vacia`, contra una base vacía), corre el humo y lo apaga. Con `PUERTO_BACK=8081` se evita chocar con otro back en el 8080.

## Producción

Variables del servicio en Railway (nombres exactos):

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL` (formato JDBC: `jdbc:postgresql://host:puerto/base`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET` (32 caracteres o más)
- `APP_CORS_ALLOWED_ORIGINS` (orígenes exactos, separados por coma; por ejemplo el dominio de Vercel)
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOMBRE`: solo si la base todavía no tiene un admin.

Con `prod`, el backend no arranca si falta o es inválido el secreto JWT, la contraseña de la base, alguna credencial de Cloudinary o (sin admin existente) las variables del admin.

- **Healthcheck de Railway:** `/actuator/health`. Responde 200 `{"status":"UP"}` sin token y sin detalles; el resto de `/actuator` está cerrado.
- **Front en Vercel:** `VITE_API_URL` es una variable de build; al cambiarla hay que redeployar.
- Los volcados de base (`*.dump`) nunca se commitean.
- El paso a paso del primer deploy está en `.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-08-PLAN.md`.

## Tests

Los tests no necesitan Docker ni Postgres. Setup local en Windows (Git Bash):

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"
mvn -B -o -Djava.version=17 test
```

`-Djava.version=17` es solo para equipos sin JDK 21; no se cambia el `pom.xml`.
