# danteautomotores-back

API REST del marketplace de reventa de autos usados DanteAutomotores. Java 21 + Spring Boot + PostgreSQL + JWT.

Repo hermano: [danteautomotores-front](https://github.com/totobias2002/danteautomotores-front)

## Modelo de datos

- **Usuario**: cuenta con rol ADMIN o COMPRADOR.
- **Agencia**: perfil de la agencia (página estandarizada dentro del marketplace), gestionado por el admin.
- **Publicacion**: auto en venta, vinculado a una agencia y al admin que lo cargó.
- **FotoPublicacion**: fotos de cada publicación (se suben a Cloudinary, se guarda la URL).
- **Conversacion**: hilo entre un usuario y la agencia atado a una publicación. Es de tipo COMPRA o COTIZACION y está ABIERTA o CERRADA. Las fechas se guardan en UTC.
- **Mensaje**: texto de 1 a 2000 caracteres dentro de una conversación, de autor USUARIO o AGENCIA, con `leido_en` (nulo mientras el otro lado no lo leyó).
- **Favorito**: publicaciones que un usuario guardó.

No hay flujo de pago dentro de la plataforma: "Lo quiero" abre una conversación con la agencia, la venta se cierra fuera del sistema y el admin actualiza el estado de la publicación (DISPONIBLE / RESERVADO / VENDIDO). Solo el ADMIN puede crear/editar/eliminar agencias y publicaciones; el COMPRADOR navega, conversa con la agencia, consulta y guarda favoritos.

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
| `BREVO_API_KEY` | **Obligatoria fuera del modo desarrollo.** API key de Brevo (API REST transaccional, no SMTP: Railway bloquea SMTP en los planes bajos). Sin ella los mails no se mandan: solo se escriben en el log. Por eso, fuera del modo desarrollo el backend no arranca si falta. Nunca se commitea ni se pega en un chat. |
| `MAIL_REMITENTE_EMAIL` | **Obligatoria fuera del modo desarrollo.** Dirección desde la que salen los mails; debe estar creada y validada como remitente en Brevo. |
| `MAIL_REMITENTE_NOMBRE` | Nombre del remitente (opcional, por defecto `Dante Automotores`). |
| `MAIL_RESPONDER_A` | Dirección de respuesta (`replyTo`) de los mails (opcional). |
| `APP_FRONTEND_URL` | **Obligatoria fuera del modo desarrollo.** Origen exacto del front, con `https://` y sin barra final (por ejemplo `https://mi-app.vercel.app`); debe coincidir con uno de `APP_CORS_ALLOWED_ORIGINS`. Con ella se arman los links de los mails. El backend no arranca si no es https o apunta a localhost. En desarrollo vale `http://localhost:5173`. |
| `GOOGLE_CLIENT_ID` | **Obligatoria fuera del modo desarrollo.** Client ID de OAuth de Google (no es un secreto, pero sin él no se puede entrar con Google). |
| `BREVO_API_URL` | URL base de la API de Brevo (opcional, por defecto `https://api.brevo.com`). Solo se cambia en tests, para apuntar a un servidor local. |
| `ADMIN_EMAIL` | Email de la cuenta admin (se recortan los espacios y se pasa a minúsculas; el log dice con qué email hay que ingresar). Solo se usa para crear la cuenta si no existe ningún admin; no puede ser el de una cuenta ya registrada. |
| `ADMIN_PASSWORD` | Contraseña de la cuenta admin (mínimo 8 caracteres). Solo se usa para crear la cuenta si no existe ningún admin; cambiarla después no modifica la cuenta. |
| `ADMIN_NOMBRE` | Nombre visible del admin (por ejemplo, Dante). Mismo uso que `ADMIN_EMAIL`. |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring. El `Dockerfile` lo deja en `prod` por defecto. `SecretosGuard` y `DataSeeder` comparten el criterio de "modo desarrollo" (clase `EntornoDeDesarrollo`) y solo son permisivos (avisan en el log y arrancan) cuando **no hay ningún perfil activo** (desarrollo local con `mvn spring-boot:run` o el IDE, sin configurar nada; un `SPRING_PROFILES_DEFAULT` que no sea de desarrollo cuenta como producción) o cuando todos los perfiles activos son `dev`, `local` o `test`. Con cualquier otro perfil (`prod`, `production`, `railway`, `staging`...) o una mezcla como `prod,dev`, el backend no arranca si `APP_JWT_SECRET` falta, es el valor de ejemplo o tiene menos de 32 bytes, ni si `SPRING_DATASOURCE_PASSWORD` es la contraseña de desarrollo del repo. Si desplegás **sin el Dockerfile** (por ejemplo el build nativo de Railway), definí `SPRING_PROFILES_ACTIVE=prod`: sin perfil el guard queda en modo desarrollo. Además, fuera del modo desarrollo el backend tampoco arranca si faltan (o son inválidas) las variables del admin y todavía no existe ninguno. |

El repo incluye un `Dockerfile` (build multi-stage con Maven + JDK 21) listo para deployar en Railway, Render o cualquier hosting que soporte contenedores.

En desarrollo y en los tests (sin `BREVO_API_KEY`) los mails no se mandan: el back escribe una línea en el log con el destinatario, el asunto y el texto, que incluye el link de confirmación o de recuperación, para probar el flujo a mano.

Al arrancar, si no hay ninguna agencia se crea "Dante Automotores" (el resto de sus datos se completa desde el panel). Las cuentas admin no se pueden crear desde la web: el registro público siempre crea compradores.

## Base de datos y migraciones

El esquema lo crea y versiona Flyway desde `src/main/resources/db/migration`:

- `V1` es el esquema previo a la Fase 1; `V2` y `V3` suman los cambios de las fases 1 y 2, y `V4` es la de las solicitudes de venta. Una base que ya existía sin historial de Flyway (por ejemplo la de producción, creada antes por Hibernate) se marca como V1 (baseline) y recibe solo las migraciones siguientes.
- `V5` es la migración de identidad de la Fase 3 (apellido, DNI único, mail confirmado, Google, tokens de cuenta y cierre de sesiones). Es aditiva: no borra ni reescribe datos de las cuentas existentes (solo da por confirmado el mail de los admins), que conviven con DNI y apellido vacíos hasta que completan sus datos. Como toda migración aplicada, no se edita. No tiene vuelta atrás de datos: no hay un script que deshaga el esquema, y una base ya migrada no se "des-migra". La vuelta atrás de un deploy es redeployar la versión anterior del back, que sigue funcionando contra el esquema nuevo porque ignora las columnas y tablas agregadas.
- `V6` (Fase 4) crea las tablas `conversaciones` y `mensajes`, con un índice único parcial que impide más de una conversación abierta de compra por usuario y auto, y las fechas en UTC. `V7` copia las consultas viejas con cuenta de comprador a conversaciones y mensajes (sin leer) y cambia la clave foránea de `consultas` hacia `publicaciones` a cascada; no borra nada. Ambas son aditivas: la tabla `consultas` queda sin uso y sin entidad (su eliminación definitiva, junto con la revisión de la agencia por la Ley 25.326, es una limpieza posterior), y la vuelta atrás de un deploy sigue siendo redeployar el back anterior, que funciona contra el esquema nuevo porque ignora lo agregado.
- **Nunca se edita una migración ya aplicada.** Todo cambio de entidad va en una migración nueva (`V8__...` en adelante).
- Hibernate está en `ddl-auto: validate`: si una entidad y el esquema no coinciden, el arranque se frena con el error. No crea ni modifica tablas.
- El SQL se puede loguear en local con `SPRING_JPA_SHOW_SQL=true` (por defecto está apagado).
- Las migraciones y las consultas del catálogo se prueban contra un Postgres real. La sección "Tests" explica cuándo esos tests se saltean y cuándo son obligatorios.

## Probar el catálogo en local

El `.env` del front puede apuntar al backend de producción, así que para probar se levanta un front aparte en otro puerto sin tocarlo:

1. `docker compose up -d` (Postgres en el 5433).
2. Backend con el origen del front de prueba permitido (la lista admite espacios después de las comas):
   `APP_CORS_ALLOWED_ORIGINS="http://localhost:5173, http://localhost:5174" mvn spring-boot:run`
3. Front de prueba, en el repo `danteautomotores-front`: `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174`.

Para verificar el catálogo sin tocar la base de desarrollo: `bash scripts/verify/con-back-local.sh --copia-de <base> <base_descartable> node scripts/verify/catalogo-humo.js` levanta el back contra una copia (o con `--vacia`, contra una base vacía), corre el humo y lo apaga. Con `PUERTO_BACK=8081` se evita chocar con otro back en el 8080.

## Cuentas, Google y mails

**Flujo (Fase 3).** Para consultar un auto o cotizar el propio hace falta una cuenta verificada: nombre, apellido, mail confirmado, teléfono y DNI.

- **Registro** con email y contraseña: pide además apellido, teléfono y DNI. El teléfono se guarda normalizado como celular argentino (`+549` más diez dígitos) y el DNI sin puntos (7 u 8 dígitos). Un mismo DNI no puede tener dos cuentas, y una vez cargado no se modifica desde la web.
- **Confirmación del mail:** al registrarse sale un mail con un link que vale 24 horas y se usa una sola vez. Mientras no se confirme, la cuenta no puede consultar ni cotizar (el back responde 403 con `codigo: CUENTA_NO_VERIFICADA` y la lista de datos que faltan).
- **Recuperación de contraseña:** link por mail que vale 1 hora y se usa una sola vez. Pedirlo responde siempre lo mismo, exista o no la cuenta. Restablecer no inicia sesión (el usuario va al login) y cierra todas las sesiones anteriores de la cuenta.
- **Ingreso con Google:** un ID token de Google Identity Services. Si ya existe una cuenta con el mismo mail y Google lo informa como verificado, se une automáticamente a esa cuenta. Si esa cuenta tenía el mail sin confirmar, pierde su contraseña al unirse (quien la creó pudo no ser el dueño de la casilla) y sus sesiones anteriores se cierran. Una cuenta que entra con Google completa después teléfono y DNI.

**Cómo se prueba en local.** El `.env` del front apunta a producción: se usa otra instancia del front en el puerto 5174 contra un back local.

1. Back (con una base descartable copiada de la de desarrollo, que se borra al terminar): `APP_FRONTEND_URL=http://localhost:5174 bash scripts/verify/con-back-local.sh --copia-de <base> <base_descartable> sleep 7200`
2. Front, en el repo `danteautomotores-front`: `VITE_API_URL=http://localhost:8080/api npm run dev -- --port 5174`
3. Sin `BREVO_API_KEY` el back está en modo desarrollo: no manda mails, escribe cada uno en su log (`$TMP/dante-back-<base_descartable>.log`, con la línea `[MAIL SOLO LOG, no se envió]`) y ahí está el link de confirmación o de recuperación.
4. El humo de punta a punta de la fase (registro, confirmación, gate, recuperación, límites y que el log no tenga datos personales) corre contra un back vacío con `bash scripts/verify/con-back-local.sh --vacia <base_descartable> node scripts/verify/cuentas-humo.js`; el script exporta `LOG_BACK` para que el humo lea los links de los mails.

**Variables de Google y de Brevo.** Para probar con mails reales o con Google, en la terminal donde se levanta el back: `GOOGLE_CLIENT_ID`, `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL` y `APP_FRONTEND_URL`; en el `.env` del front (ignorado por git), `VITE_GOOGLE_CLIENT_ID`. La API key de Brevo es un secreto: **nunca se pega en un chat, un issue ni un commit**; se carga directo en la terminal o en el panel de Railway. Si se expuso, se revoca en Brevo y se genera otra. En Brevo hay que desactivar "Block unknown IP addresses" (Railway rota las IPs de salida y Brevo respondería 401).

**Google en estado "Testing" (pendiente conocido).** Hoy no hay dominio propio, y Google exige que la página de inicio y la política de privacidad de la app estén en un dominio autorizado y propio (un `*.vercel.app` se rechaza). Por eso la app de Google Cloud sigue en estado Testing: el ingreso con Google solo funciona para los usuarios de prueba cargados en la consola (hasta 100). El registro con email y contraseña no se ve afectado. Para cerrarlo: comprar un dominio, agregarlo como dominio autorizado en Google Auth Platform, cargar las URLs de inicio y de privacidad de ese dominio y publicar la app (pasar de Testing a In production).

### Pasar a un dominio propio para los mails (D-13)

Con una casilla de Gmail como remitente, Brevo no puede autenticar el dominio y los mails salen desde `@brevosend.com`: pueden caer en spam o mostrar "vía brevosend.com". Es lo que permite arrancar sin dominio, y la entregabilidad se mide en el primer deploy (D-18). Cuando haya un dominio propio, sin cambiar código:

1. Comprar el dominio.
2. En Brevo: Domains → Add a domain.
3. En el DNS del dominio, crear los registros que Brevo indica: el TXT "Brevo code", el DKIM y el DMARC (TXT en `_dmarc`).
4. Tocar "Authenticate" en Brevo y esperar a que valide los registros.
5. Cambiar `MAIL_REMITENTE_EMAIL` en Railway a una casilla del dominio (por ejemplo `no-responder@<dominio>`) y redeployar.

### Deuda de seguridad conocida

- El registro responde "Ya existe una cuenta con ese email", lo que revela que esa cuenta existe (D-17: elección de UX frente a un mensaje genérico). Se mitiga con un límite de registros por IP y con la confirmación del mail.
- Los límites de intentos (login, registro, recuperación, reenvío de mails y tope diario de mails) viven en la memoria de una sola instancia y se reinician con cada deploy. Si el back se escala a más de una instancia, hay que moverlos a la base o a Redis.
- La IP detrás del proxy de Railway es de mejor esfuerzo (puede ser compartida o falseada): el límite por mail es la defensa real y el de IP suma una capa.
- El JWT se guarda en `localStorage` del front, expuesto a un XSS. Cambiar la contraseña cierra las sesiones anteriores, pero no hay cierre de sesión remoto desde el servidor.

### Datos personales (Ley 25.326)

- El DNI y el teléfono de una cuenta solo los devuelve `/api/usuarios/me` a su dueño y, desde la Fase 4, `GET /api/admin/usuarios/{id}` al admin: la ficha del usuario (`/admin/usuarios/{id}` en el front) es el único lugar donde salen hacia otra persona. Las respuestas de login y registro nunca los incluyen.
- No se loguean: ni el DNI, ni el teléfono, ni las contraseñas, ni los links de un solo uso en producción (en desarrollo el mail se escribe en el log a propósito, y `SecretosGuard` impide ese modo fuera de él). El humo de cuentas verifica que el log de una corrida completa no contenga esos datos.
- La página `/privacidad` del front es un borrador. La revisión legal y la inscripción de la base de datos ante la AAIP (Agencia de Acceso a la Información Pública) son pendientes de la agencia, fuera del código (D-20).

## Mensajería: conversaciones con la agencia (Fase 4)

**Flujo.** "Lo quiero" y "Consultar por este auto" (`POST /api/conversaciones`) crean la conversación de compra del usuario sobre ese auto, o reutilizan la que ya tiene abierta. Un auto vendido se rechaza y la cuenta tiene que estar verificada (si no, 403 `CUENTA_NO_VERIFICADA`). El comprador sigue la charla en Mis mensajes; el admin atiende todas desde su bandeja.

| Endpoint | Rol | Qué hace |
|---|---|---|
| `POST /api/conversaciones` | comprador | Lo quiero / Consultar: crea o reutiliza la conversación abierta de compra del auto |
| `GET /api/conversaciones` | comprador | Lista sus conversaciones |
| `GET /api/conversaciones/{id}` | comprador | Hilo de una conversación propia |
| `POST /api/conversaciones/{id}/mensajes` | comprador | Envía un mensaje |
| `POST /api/conversaciones/{id}/leida` | comprador | Marca como leídos los mensajes de la agencia |
| `GET /api/conversaciones/no-leidas` | comprador y admin | Contador de mensajes sin leer (el del admin cuenta la bandeja) |
| `GET /api/admin/conversaciones` | admin | Bandeja con filtros (tipo, estado, solo no leídas) y paginación |
| `GET /api/admin/conversaciones/{id}` | admin | Hilo de cualquier conversación |
| `POST /api/admin/conversaciones/{id}/mensajes` | admin | Responde como agencia |
| `POST /api/admin/conversaciones/{id}/leida` | admin | Marca como leídos los mensajes del usuario |
| `POST /api/admin/conversaciones/{id}/cerrar` | admin | Cierra la conversación |
| `POST /api/admin/conversaciones/{id}/reabrir` | admin | La reabre |
| `GET /api/admin/usuarios/{id}` | admin | Ficha del usuario con su historial de conversaciones |

- **No leídos.** Cada mensaje tiene `leido_en`; los no leídos de un lado son los que escribió el otro. El contador sale de `GET /api/conversaciones/no-leidas`, que responde según el rol: al comprador le cuenta lo suyo y al admin le cuenta la bandeja.
- **Sin tiempo real.** El front consulta el contador cada 30 segundos y el hilo abierto cada 10, solo con la pestaña visible. No hay websockets.
- **Cerrar y reabrir.** Una conversación cerrada no recibe mensajes. Reabrir se rechaza si el usuario ya tiene otra conversación abierta por el mismo auto.
- **Avisos por mail.** Cada mensaje nuevo avisa al otro lado (a los admins si escribe el usuario, al dueño si responde la agencia) con asunto fijo y sin el texto del mensaje. Sale como máximo un mail cada 10 minutos por conversación y destinatario, y cuenta contra el tope diario compartido de 250 mails. Usan Brevo sin variables de entorno nuevas; en desarrollo se escriben en el log.
- **Límite de envío.** 20 mensajes cada 10 minutos por usuario; al pasarse, 429.
- **Privacidad.** El DNI y el teléfono salen solo en `GET /api/admin/usuarios/{id}`, para el admin. Una conversación ajena o inexistente responde el mismo 404. El texto de los mensajes no se escribe en el log.
- **Borrar un auto** borra sus conversaciones y mensajes (el panel avisa cuántas conversaciones se pierden antes de confirmar).

**Limitaciones conocidas.**

- El estado de los límites de envío y de las ventanas de mail vive en la memoria de una instancia: se reinicia con cada deploy y no escala a varias instancias (mismo caso que los límites de la sección "Deuda de seguridad conocida").
- El tipo COTIZACION existe en el modelo, pero las conversaciones de cotización las crea la Fase 5, que también suma las cotizaciones a la ficha del usuario.
- La tabla `consultas` quedó sin uso (ver "Base de datos y migraciones").

**Cómo correr el humo de la fase.** Contra un back local con una base vacía y descartable:

```bash
ADMIN_EMAIL=admin.humo@dante.test ADMIN_PASSWORD=<una contraseña de prueba> ADMIN_NOMBRE=AdminHumo \
  bash scripts/verify/con-back-local.sh --vacia <base_descartable> node scripts/verify/mensajes-humo.js
```

Termina con una línea `humo:` que debe decir `0 fallas`.

## Producción

Variables del servicio en Railway (nombres exactos):

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL` (formato JDBC: `jdbc:postgresql://host:puerto/base`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET` (32 caracteres o más)
- `APP_CORS_ALLOWED_ORIGINS` (orígenes exactos, separados por coma; por ejemplo el dominio de Vercel)
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL` (y, si se quiere, `MAIL_REMITENTE_NOMBRE`, `MAIL_RESPONDER_A`)
- `APP_FRONTEND_URL` (https, sin barra final), `GOOGLE_CLIENT_ID`
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NOMBRE`: solo si la base todavía no tiene un admin.

Con `prod`, el backend no arranca si falta o es inválido el secreto JWT, la contraseña de la base, alguna credencial de Cloudinary, `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `GOOGLE_CLIENT_ID`, un `APP_FRONTEND_URL` que no sea https (o apunte a localhost) o (sin admin existente) las variables del admin.

- **Healthcheck de Railway:** `/actuator/health`. Responde 200 `{"status":"UP"}` sin token y sin detalles; el resto de `/actuator` está cerrado.
- **Front en Vercel:** `VITE_API_URL` es una variable de build; al cambiarla hay que redeployar.
- Los volcados de base (`*.dump`) nunca se commitean.
- **Antes de cada deploy:** con `docker compose up -d`, correr `mvn -B test -Ddante.pg.required=true` y exigir verde. Sin el flag, los tests de migraciones pueden saltearse en silencio.
- El paso a paso del primer deploy está en `.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-08-PLAN.md`.

## Tests

Hay dos grupos de tests:

- La mayoría (unitarios y de controladores) corren sin Docker ni base de datos.
- `MigracionesPostgresTest` (V1 a V7 contra `ddl-auto: validate`, incluida una base creada por Hibernate sin historial), `CatalogoPostgresTest` (consultas reales del catálogo), `ConversacionPostgresTest` y `ConversacionAdminPostgresTest` (conversaciones y mensajes reales) usan el Postgres del `docker-compose.yml` en localhost:5433, en bases descartables `test_xxxxxxxx` que crean y borran solas. Nunca tocan la base `danteautomotores`.
- El front (repo `danteautomotores-front`) corre sus tests con `npm test`.

Sin Postgres, esos tests se saltean: Maven los informa como "Skipped" y el build queda verde igual, así que un verde sin Postgres no prueba las migraciones.

Con `-Ddante.pg.required=true` no se saltean: sin Postgres fallan con un mensaje que explica cómo levantarlo. Es obligatorio correrlos así (con `docker compose up -d` antes) antes de cada deploy y cada vez que se agrega una migración o se cambia una entidad.

La conexión se puede cambiar con `-Ddante.pg.host`, `-Ddante.pg.port`, `-Ddante.pg.user` y `-Ddante.pg.password` (por defecto localhost, 5433 y el usuario y la contraseña de desarrollo del `docker-compose.yml`).

```bash
# Rápido: los tests de Postgres pueden saltearse
mvn -B test

# Obligatorio antes de cada deploy
docker compose up -d
mvn -B test -Ddante.pg.required=true
```

Setup local en Windows (Git Bash) con solo JDK 17; `MAVEN_HOME` es la carpeta donde está instalado Maven 3.9+:

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="$MAVEN_HOME/bin:$PATH"
mvn -B -Djava.version=17 test -Ddante.pg.required=true
```

`-Djava.version=17` es solo para equipos sin JDK 21; no se cambia el `pom.xml`. No se usa `-o` porque el modo offline solo anda con las dependencias ya descargadas.
