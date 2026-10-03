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

1. Base de datos: `docker compose up -d` (levanta Postgres en el puerto 5432).
2. Backend: `mvn spring-boot:run`, o directamente corriendo la clase `DanteAutomotoresApplication` desde tu IDE (IntelliJ, con Java 21 configurado).

## Variables de entorno

Ver `src/main/resources/application.yml`. Para producción, sobreescribir `app.jwt.secret` y las credenciales de `cloudinary` por variables de entorno reales — nunca commitear secretos.

## Variables de entorno para producción (Railway, Render, etc.)

| Variable | Descripción |
|---|---|
| `PORT` | Puerto HTTP (Railway/Render la inyectan solos). |
| `SPRING_DATASOURCE_URL` | URL JDBC de la base Postgres, ej: `jdbc:postgresql://host:5432/db`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base. |
| `APP_JWT_SECRET` | Secreto para firmar JWT — al menos 32 caracteres, distinto al de desarrollo. |
| `APP_JWT_EXPIRATION_MS` | Duración del token en ms (opcional, default 24hs). |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma, ej: `https://mi-app.vercel.app`. |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Credenciales de Cloudinary para las fotos. |
| `ADMIN_EMAIL` | Email de la cuenta admin. Solo se usa para crear la cuenta si no existe ningún admin; no puede ser el de una cuenta ya registrada. |
| `ADMIN_PASSWORD` | Contraseña de la cuenta admin (mínimo 8 caracteres). Solo se usa para crear la cuenta si no existe ningún admin; cambiarla después no modifica la cuenta. |
| `ADMIN_NOMBRE` | Nombre visible del admin (por ejemplo, Dante). Mismo uso que `ADMIN_EMAIL`. |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring. Con `prod`, el backend no arranca si faltan (o son inválidas) las variables del admin y todavía no existe ninguno. Sin `prod`, solo avisa en el log y arranca igual. |

El repo incluye un `Dockerfile` (build multi-stage con Maven + JDK 25) listo para deployar en Railway, Render o cualquier hosting que soporte contenedores.

Al arrancar, si no hay ninguna agencia se crea "Dante Automotores" (el resto de sus datos se completa desde el panel). Las cuentas admin no se pueden crear desde la web: el registro público siempre crea compradores.

## Tests

Los tests no necesitan Docker ni Postgres. Setup local en Windows (Git Bash):

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"
mvn -B -o -Djava.version=17 test
```

`-Djava.version=17` es solo para equipos sin JDK 21; no se cambia el `pom.xml`.
