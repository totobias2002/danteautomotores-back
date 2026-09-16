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
