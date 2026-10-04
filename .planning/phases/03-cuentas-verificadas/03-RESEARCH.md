# Phase 3: Cuentas verificadas - Research

**Researched:** 2026-10-03
**Domain:** Spring Boot 3.3.5 (Spring Security 6.3.4 stateless JWT, Flyway sobre PostgreSQL 16) + React 18 / Vite / react-router 6. Login con Google Identity Services, mails transaccionales con Brevo, tokens de un solo uso, datos personales (DNI/teléfono) bajo Ley 25.326. Fase brownfield cross-repo (back en `danteautomotores-back`, front en `danteautomotores-front`).
**Confidence:** HIGH en verificación del ID token de Google, tokens, migración V4 y normalización de teléfono (todo ejecutado en esta sesión contra la JVM 17, el Postgres local y el JWKS real de Google); MEDIUM en Brevo (docs oficiales parcialmente inaccesibles: varias afirmaciones vienen de resultados de búsqueda) y en Railway; las configuraciones de Google Cloud / Brevo / Railway / Vercel son checkpoints humanos que no se pueden observar desde acá.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Qué significa "cuenta verificada"**
- **D-01:** Una cuenta está **verificada** (puede comprar, cotizar y consultar) cuando tiene nombre, apellido, teléfono y DNI cargados **y** el mail confirmado. Sin eso puede navegar y guardar favoritos. — **Reversibility:** costly — el estado "verificada" lo consultan el back (gate de endpoints) y el front (gate de botones) de esta fase y de las Fases 4 y 5.
- **D-02:** **El mail se confirma con un link** que llega al registrarse (y que se puede reenviar). El link de confirmación dura **24 horas**. Con Google el mail ya viene confirmado.
- **D-03:** **El teléfono no se confirma con código**: solo se valida que sea un celular argentino válido (código de área + número) y se guarda normalizado.
- **D-04:** **El DNI es único** (una cuenta por DNI): si alguien registra un DNI existente se rechaza con un mensaje que sugiere recuperar la contraseña, sin revelar de quién es la cuenta. — **Reversibility:** one-way — es una restricción UNIQUE en la base (migración Flyway) y una regla del contrato del registro.
- **D-05:** **El DNI se valida solo por formato** (7 u 8 dígitos, se guarda sin puntos). Sin foto de DNI ni validación contra RENAPER: el admin confirma la identidad al cerrar la operación en persona.

**Login con Google**
- **D-06:** Si el mail de Google **ya tiene una cuenta con contraseña, se unen automáticamente**: entra a la misma cuenta (datos, favoritos) y desde ahí puede usar los dos métodos. Solo cuando Google confirma el mail (`email_verified`). — **Reversibility:** costly — define el modelo de identidad (una cuenta, varios métodos de ingreso).
- **D-07:** A quien entra con Google por primera vez (o a cualquier cuenta incompleta) se le muestra **"Completá tus datos" apenas entra** (apellido, teléfono, DNI). Puede saltearla y navegar, pero comprar, cotizar o consultar lo vuelve a llevar ahí.
- **D-08:** El nombre y apellido de Google **se precargan y se pueden corregir** (en "Completá tus datos" y en el perfil), para que coincidan con el DNI.

**Cuentas que ya existen**
- **D-09:** Los compradores ya registrados (sin apellido ni DNI, teléfono opcional) se tratan **igual que Google**: en el próximo login ven "Completá tus datos"; para operar la tienen que completar. — **Reversibility:** costly — la migración deja `apellido`/`dni` nulos para las cuentas viejas, así que las columnas no pueden ser NOT NULL a nivel base; la obligatoriedad vive en la regla de "cuenta verificada".
- **D-10:** Los mails de las cuentas existentes **no se dan por confirmados**: también tienen que confirmarlo antes de operar.
- **D-11:** La **consulta anónima actual** de la ficha ("Enviar consulta" con nombre/mail/teléfono sueltos) **pasa a exigir cuenta verificada** y usa los datos de la cuenta (el formulario solo pide el mensaje). La Fase 4 la reemplaza por la conversación "Lo quiero".

**Mails y recuperación de contraseña**
- **D-12:** El servicio de mail es **Brevo** (plan gratis), detrás de una interfaz propia para poder cambiarlo; credenciales por variables de entorno (la cuenta y la API key/SMTP las crea el usuario: checkpoint humano). En desarrollo y tests no se mandan mails reales (implementación que loguea o captura). — **Reversibility:** reversible — aislado detrás de la interfaz.
- **D-13:** **Sin dominio propio por ahora**: el remitente (dirección y nombre) es configurable por variable de entorno y se arranca con el que permita Brevo sin dominio verificado. Dejar en el runbook cómo pasar a un dominio propio (registros DNS) más adelante.
- **D-14:** El link para **cambiar la contraseña dura 1 hora y es de un solo uso**. Pedirlo no revela si el mail tiene cuenta (respuesta siempre igual).

### Claude's Discretion
- Diseño de "Completá tus datos", del perfil y de los mails (estilo del sitio; Kavak / Mercado Libre como referencia).
- Qué se puede editar en el perfil: por defecto nombre, apellido y teléfono editables; **DNI no editable** una vez cargado (si está mal, lo corrige el admin) y **mail no editable** en esta fase (cambiarlo exigiría reconfirmar; queda como mejora). El planner puede ajustar si encuentra una razón fuerte, dejándolo anotado.
- Cambio de contraseña desde el perfil (pidiendo la actual) y cómo convive con cuentas solo-Google (pueden definir una contraseña vía "olvidé mi contraseña").
- Mecanismo técnico de Google (Google Identity Services con ID token verificado en el back vs. flujo OAuth con redirect) y configuración en Google Cloud (credenciales y URIs de producción: checkpoint humano).
- Límites de intentos (login, reenvío de confirmación, pedidos de recuperación) y mensajes de error.
- Cómo se transporta el estado "verificada" al front (claims del JWT y/o endpoint `/me`) y cómo el back rechaza las acciones de cuentas no verificadas (403 con mensaje accionable).
- Tratamiento de datos personales (Ley 25.326): no loguear DNI ni teléfono, no exponerlos en respuestas públicas, aviso de privacidad mínimo en el registro.

### Deferred Ideas (OUT OF SCOPE)
- Cambiar el mail desde el perfil (con reconfirmación).
- Confirmar el teléfono con código por SMS/WhatsApp.
- Foto o validación del DNI contra RENAPER.
- Dominio propio para los mails (registros DNS) cuando la agencia lo tenga.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | Registro con nombre, apellido, mail, contraseña, teléfono y DNI (los dos últimos obligatorios) | `RegistroRequest` ampliado + normalización de teléfono (libphonenumber) y DNI (regex) §Patrón 4/5; V4 con `dni` UNIQUE §Patrón 1; mail de confirmación §Patrón 3 |
| AUTH-02 | Login con Google | GIS `renderButton` + `POST /api/auth/google` con `NimbusJwtDecoder` contra el JWKS de Google (probado) §Patrón 2; vinculación D-06 con defensa anti pre-hijacking |
| AUTH-03 | Quien entra con Google sin teléfono/DNI los completa antes de comprar o cotizar | `faltantes` + `GET/PUT /api/usuarios/me`, pantalla `/completar-datos`, gate 403 `CUENTA_NO_VERIFICADA` §Patrón 6 |
| AUTH-04 | Recuperar contraseña con link por mail | Tokens opacos hasheados de 1 h / un solo uso, respuesta uniforme, límites de intentos §Patrón 3; Brevo por API REST §Patrón 7 |
| AUTH-05 | Ver y editar el perfil | `GET/PUT /api/usuarios/me`, cambio de contraseña, reglas de edición (DNI/mail no editables) §Patrón 6 |
| AUTH-06 | "Lo quiero", cotizador y mensajería exigen sesión; tras el login se vuelve a la página de origen | `state.from` reutilizado + `evaluarAcceso()` puro testeable §Patrón 8; inventario del front |
| PROD-03 | Servicio de envío de mails configurado | `EmailSender` + `BrevoEmailSender` (RestClient) + `LogEmailSender`; guard de arranque; runbook Brevo §Runbook |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Directivas accionables de `.claude/CLAUDE.md` que el plan debe respetar:

- Stack fijo: Spring Boot 3.3 / Java 21 / PostgreSQL / JPA + React 18 / Vite / Tailwind v4. No cambiar de stack. Se compila localmente con JDK 17 (`-Djava.version=17`).
- Idioma: mensajes, errores, comentarios, tablas/campos/enums en **español**. Entidades en singular, DTOs por feature en `dto/<feature>/` (nuevo: `dto/usuario/`), mappers con métodos estáticos `toResponse()`/`toEntity()`.
- Servicios devuelven DTOs, nunca entidades; controllers envuelven en `ResponseEntity<T>`; DTOs de entrada con `@Valid`; `ResourceNotFoundException` → 404, `ReglaDeNegocioException` → 400 con `{"error"}` / `{"error","campos"}`.
- Todo cambio de esquema es una migración Flyway nueva (**V4**); nunca se editan V1–V3 (README y 02-CONTEXT).
- Front: componentes `.jsx` PascalCase (sufijo `Page` en rutas), utilidades `.js` camelCase, handlers `handle*`, 2 espacios, comillas simples, **sin punto y coma** (así está el front hoy), Tailwind con la paleta existente (`bronze`, `navy`, `navy-dark`, `cream`), `lucide-react` para íconos.
- Back y front en repos separados; la planificación vive en el back. Imágenes por Cloudinary (no aplica acá).
- Sin ediciones directas fuera de un flujo GSD. Animaciones moderadas (el pulido es la Fase 6: esta fase no agrega motion nuevo).
- No loguear secretos; el front no debe leer ni mostrar `.env` (hook de protección de secretos activo en esta máquina).

## Summary

La fase mezcla cuatro frentes con riesgo distinto. El **más delicado es la identidad**: D-06 pide unir automáticamente una cuenta de Google con una cuenta existente de contraseña. Hecho tal cual, habilita el ataque de *account pre-hijacking* (alguien registra el mail de la víctima con su propia contraseña antes de que la víctima use Google, y después de la unión sigue entrando a la cuenta de la víctima). Se resuelve **sin contradecir D-06**: se une solo si Google dice `email_verified=true`, y si la cuenta local tenía el mail **sin confirmar**, la unión descarta su contraseña (el dueño real la recupera por mail) y marca `password_cambiada_en` para cortar sesiones viejas. Además, todo el flujo de mails (reset incluido) confirma el mail al completarse con éxito, porque probó que quien lo hizo controla la casilla.

El segundo frente es **Google**. Se recomienda *Google Identity Services* con el botón oficial (`renderButton`, modo popup/callback) que entrega un ID token al SPA, y `POST /api/auth/google` que lo verifica en el back con `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` (módulo `spring-security-oauth2-jose`, ya gestionado por el BOM de Boot 3.3.5 → 6.3.4, sin dependencia de terceros nueva) más validadores de `iss`, `aud`, `exp` y la verificación manual de `email_verified`. Se probó de punta a punta en una JVM 17: el decoder baja el JWKS real de Google, rechaza `aud`/`iss` ajenos, tokens vencidos, mal formados y firmados con otra clave. El flujo OAuth con redirect se descarta (obliga a un secreto de cliente en el back, a exponer un callback y a perder el `state.from` del SPA). El front **no necesita npm nuevo**: alcanza un hook que carga `https://accounts.google.com/gsi/client`.

El tercer frente son los **mails**. Brevo se usa por **API REST** (`POST https://api.brevo.com/v3/smtp/email`) con `RestClient`, no por SMTP: Railway **bloquea SMTP en los planes Free/Trial/Hobby** `[CITED: docs.railway.com/networking/outbound-networking]`. Dos trampas conocidas de Brevo condicionan D-13: (1) un remitente `@gmail.com`/`@yahoo.com` **no se puede autenticar** y Brevo lo reemplaza por `@brevosend.com` (deliverabilidad dudosa, hay que probarlo con casillas reales) y (2) la restricción de IPs autorizadas de Brevo bloquea las IPs rotativas de Railway con 401: hay que desactivarla. El límite gratis es 300 mails/día en total y requiere activar el envío transaccional.

El cuarto frente es el **modelo de datos y el gate**. V4 agrega `apellido`, `dni` (UNIQUE: en Postgres los NULL no colisionan, verificado), `email_confirmado`, `google_sub` (UNIQUE), `password_cambiada_en`, hace `password_hash` nullable, crea `tokens_cuenta` y un índice único sobre `lower(email)` (sin mutar datos, para poder revertir a la versión anterior del back). El estado "verificada" **no va en el JWT** (cambia durante la vida del token de 24 h): el back lo calcula desde la base en cada acción y responde `403` con `codigo: CUENTA_NO_VERIFICADA` y la lista `faltantes`; el front lo conoce por `GET /api/usuarios/me` y lo refresca al cargar. Hay una trampa que rompe a las cuentas solo-Google en silencio: `User.builder().password(null)` lanza `IllegalArgumentException` (verificado) y `JwtAuthenticationFilter` captura esa excepción y deja la request sin autenticar.

**Primary recommendation:** Hacer primero la pista de datos (entidad `Usuario` + V4 + `tokens_cuenta` + normalizadores de DNI/teléfono con tests en Postgres), después identidad (registro ampliado, tokens, `EmailSender` y recuperación), después Google, después el gate y el perfil, después el front (que depende de todo lo anterior), y dejar las configuraciones externas (Google Cloud, Brevo, variables de Railway/Vercel) como checkpoints humanos explícitos **antes** del deploy, porque el `SecretosGuard` ampliado hará fallar el arranque en producción si faltan.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Verificación del ID token de Google | API / Backend | Browser (obtiene el token de GIS) | El front nunca decide identidad: el back valida firma, `iss`, `aud`, `exp`, `email_verified` |
| Identidad (cuenta única, vinculación por mail, anti pre-hijacking) | API / Backend | Database (UNIQUE) | Regla de negocio D-06; la base garantiza unicidad de `dni`, `google_sub`, `lower(email)` |
| Estado "cuenta verificada" | API / Backend (se calcula de la base en cada acción) | Browser (lo muestra y gatea botones por UX) | Es mutable y lo usan fases 4 y 5; el JWT dura 24 h y se desactualizaría; el gate del front es solo cortesía |
| Rechazo de acciones de cuentas no verificadas | API / Backend (403 + `codigo` + `faltantes`) | Browser (redirige a `/completar-datos`) | "Authorization checks only in frontend" es un anti-patrón del proyecto |
| Normalización/validación de teléfono y DNI | API / Backend (fuente de verdad) | Browser (validación liviana de UX) | Se guarda normalizado (E.164 `+549…`, DNI sin puntos) |
| Tokens de confirmación y de reset | API / Backend + Database (`tokens_cuenta`) | — | Hasheados, un solo uso, vencimiento; el front solo transporta el token de la URL |
| Envío de mails | API / Backend (`EmailSender`) | Proveedor externo (Brevo, HTTPS) | Interfaz propia (D-12); el envío es asíncrono para no bloquear ni filtrar por tiempos |
| Límite de intentos (login, registro, reset, reenvío) | API / Backend (en memoria, una instancia) | — | Sin infra nueva; reinicios lo resetean (aceptable ahora) |
| Vuelta a la página de origen tras el login | Browser (`location.state.from`) | — | Patrón ya existente de Fase 1; con GIS en modo popup el estado del SPA se conserva |
| Sesión (token + perfil) | Browser (`localStorage` + `AuthContext`) | API (`/usuarios/me` como fuente de verdad) | El perfil guardado puede estar viejo: se rehidrata desde `/me` al cargar |
| Aviso de privacidad / Ley 25.326 | Browser (página `/privacidad` + leyenda) | API (no loguear/exponer PII) | Texto estático; el back evita PII en logs y respuestas públicas |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `org.springframework.security:spring-security-oauth2-jose` | 6.3.4 (la gestiona el BOM de Boot 3.3.5, sin `<version>`) | `NimbusJwtDecoder` + `JwtValidators` para verificar el ID token de Google contra su JWKS | `[VERIFIED: ejecución local en jprobe]` resolvió a `spring-security-oauth2-jose:jar:6.3.4:compile` con `com.nimbusds:nimbus-jose-jwt:jar:9.37.3` y rechazó/aceptó tokens como se esperaba. Es la forma "librería JWT de propósito general" que Google recomienda `[CITED: developers.google.com/identity/gsi/web/guides/verify-google-id-token]`. Cachea el JWKS 5 min y rota claves solas `[CITED: docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html]`. **No** activa autoconfig de resource server (falta el módulo `oauth2-resource-server`) |
| `com.googlecode.libphonenumber:libphonenumber` | 9.0.40 (Maven Central, `lastUpdated 20260923`) | Parseo y normalización de celulares argentinos (0, 15, +54, 9, largo del código de área) | `[VERIFIED: ejecución local]` 41 entradas reales y sintéticas normalizan a `+549…` correctamente (§Code Examples). Jar de 360 KB, Apache-2.0, de Google `[CITED: github.com/google/libphonenumber]`. NO lo gestiona el BOM: llevar `<libphonenumber.version>` a `<properties>` |
| `com.github.ben-manes.caffeine:caffeine` | 3.1.8 (BOM de Boot 3.3.5, `spring-boot-dependencies-3.3.5.pom:41`) | Contadores de intentos con vencimiento y tope de tamaño (límite de intentos en memoria) | `[VERIFIED: ejecución local]` `expireAfterWrite` + `maximumSize` + `AtomicInteger` bloqueó el intento 6 y 7. Evita un `ConcurrentHashMap` sin purga |
| Spring `RestClient` (spring-web 6.1.14, ya en el classpath por `spring-boot-starter-web`) | 6.1.14 | Llamada HTTPS a la API de Brevo | `[VERIFIED: ejecución local]` posteó JSON con header `api-key` y leyó `{"messageId":...}`; sin dependencia nueva. `JdkClientHttpRequestFactory` + `setReadTimeout` para los timeouts |
| Google Identity Services (script `https://accounts.google.com/gsi/client`) | — | Botón oficial "Continuar con Google" que devuelve un ID token (JWT) al callback | `[CITED: developers.google.com/identity/gsi/web/reference/js-reference]` `initialize({client_id, callback})` + `renderButton(el, {type, theme, size, text, shape, logo_alignment, width≤400})`; `credential` = JWT |
| Flyway (ya instalado) | 10.10.0 | Migración **V4** | Fase 2 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.security.SecureRandom` + `MessageDigest("SHA-256")` + `Base64.getUrlEncoder()` (JDK) | JDK 17/21 | Generar y hashear tokens opacos | No hace falta librería: con 256 bits de entropía basta SHA-256 sin sal |
| `spring-security-test` (ya en el pom) | 6.3.4 | Tests `@WebMvcTest` del gate | Reutilizar `SeguridadWebMvcTestBase` |
| `node:test` (Node v24.14.1) | built-in | Tests de utilidades puras del front (`cuenta.js`, normalizadores) | Front sin runner; **no** agregar vitest. Ojo: en Node 24 `node --test src/utils/` (directorio) **falla** (`'test failed'`, verificado); usar `node --test "src/**/*.test.js"` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `NimbusJwtDecoder` (Spring) | `com.google.api-client:google-api-client` `GoogleIdTokenVerifier` | Es el camino "oficial" de Google pero arrastra Guava, google-http-client, opencensus y otro cliente HTTP; el cliente de Cloudinary ya trae Apache HttpClient 4. Para una verificación de 5 claims no se justifica. Mantener Nimbus |
| Botón GIS sin npm | `@react-oauth/google` 0.13.5 (`GoogleLogin`/`GoogleOAuthProvider`) | `[VERIFIED: gsd-tools package-legitimacy → OK]` (1,6 M descargas/sem, repo MomenSherif/react-oauth, peerDeps react ≥16.8). Es solo un wrapper del mismo script; agrega una dependencia sin ganancia real. Opción válida si el planner prefiere no escribir el hook |
| Callback con ID token (popup) | Flujo OAuth *authorization code* con redirect | Requiere client secret en el back, intercambio de code, callback público y `state`/CSRF propio; pierde `location.state.from` del SPA. Descartado |
| Botón propio estilizado + `initTokenClient` | `renderButton` de GIS | Los flujos `oauth2.*` devuelven access token/code, no ID token; habría que llamar a `userinfo`. El botón renderizado por Google es el que da `credential` directo y cumple la marca |
| Brevo por API REST | Brevo SMTP relay con `spring-boot-starter-mail` | `[CITED: docs.railway.com/networking/outbound-networking]` "SMTP is only available on the Pro plan and above. Free, Trial, and Hobby plans must use transactional email services with HTTPS APIs." REST funciona en cualquier plan y evita `jakarta.mail`/`angus-mail` |
| Contadores con Caffeine | Bucket4j (`bucket4j_jdk17-core`) | Bucket4j hace token-bucket "bonito" pero no purga claves por sí solo y es otra dependencia fuera del BOM; un contador de ventana fija alcanza |
| libphonenumber | Regex propia | Una regex no sabe si el `15` va después de 2, 3 o 4 dígitos de código de área ni convierte a `+549`. Es exactamente el "problema aparentemente simple" para no escribir a mano |

**Installation (backend, `pom.xml`):**
```xml
<properties>
    <libphonenumber.version>9.0.40</libphonenumber.version>
</properties>

<!-- Verificación del ID token de Google (JWKS + validadores). Versión gestionada por el BOM de Boot (6.3.4). -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
<!-- Contadores de intentos con vencimiento. Versión gestionada por el BOM (3.1.8). -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<!-- Normalización de teléfonos argentinos -->
<dependency>
    <groupId>com.googlecode.libphonenumber</groupId>
    <artifactId>libphonenumber</artifactId>
    <version>${libphonenumber.version}</version>
</dependency>
```
Los tres quedaron cacheados en `~/.m2` en esta sesión (`spring-security-oauth2-jose/6.3.4`, `nimbus-jose-jwt/9.37.3`, `caffeine/3.1.8`, `libphonenumber/9.0.40` `[VERIFIED: ls ~/.m2/repository/...]`), así que `mvn -B -o -Djava.version=17 test` sigue andando. En otra máquina hace falta una resolución online: `mvn -B -Djava.version=17 -DskipTests test-compile`.

**Installation (frontend):** ninguna (`# no new packages`). Variable nueva de build: `VITE_GOOGLE_CLIENT_ID` (agregar a `.env.example`, que hoy solo tiene `VITE_API_URL=http://localhost:8080/api`).

**Version verification:** `curl maven-metadata.xml` → libphonenumber `<release>9.0.40</release>` (`lastUpdated 20260923133008`); `spring-security-oauth2-jose` y `caffeine` según `spring-boot-dependencies-3.3.5.pom` (`<spring-security.version>6.3.4</spring-security.version>` línea 201, `<caffeine.version>3.1.8</caffeine.version>` línea 41) `[VERIFIED: grep del BOM]`.

## Package Legitimacy Audit

El seam `package-legitimacy check` solo cubre `npm|pypi|crates`. Las dependencias Maven no se pueden pasar por él; se documentan con la evidencia disponible.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| org.springframework.security:spring-security-oauth2-jose | Maven Central | años (Spring Security 6.3.4) | — | github.com/spring-projects/spring-security | n/a (BOM de Boot, versión leída del pom) | Approved |
| com.github.ben-manes.caffeine:caffeine | Maven Central | años | — | github.com/ben-manes/caffeine | n/a (BOM de Boot, línea 41) | Approved |
| com.googlecode.libphonenumber:libphonenumber | Maven Central (200 en `.pom` y `.jar`, 360 070 bytes) | años (release 9.0.40, 2026-09-23) | — | github.com/google/libphonenumber (README confirma `groupId`/`artifactId`) | n/a (fuera del seam; existe + coordenadas confirmadas en la doc oficial + ejecutado localmente) | Approved con **checkpoint:human-verify liviano**: confirmar las coordenadas en central.sonatype.com antes de agregarla (no está en el BOM) |
| @react-oauth/google (npm) | npm | publicado 2026-04-08 (0.13.5) | 1 643 312/sem | github.com/MomenSherif/react-oauth | OK (seam) | **No se recomienda** (alternativa); si el planner la elige, aprobada por el seam, `postinstall: null` |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
*No se agregan: `spring-boot-starter-mail`, `google-api-client`, Bucket4j, vitest, `@react-oauth/google`. Si el planner decide lo contrario, cada una pasa por `checkpoint:human-verify`.*

## Architecture Patterns

### System Architecture Diagram

```
 NAVEGADOR (Vercel)                                    BACKEND (Railway, perfil prod)                    EXTERNOS / DATOS
 ──────────────────                                    ──────────────────────────────                    ────────────────
 Registro / Login (mail+clave)  ───────────────────►  AuthController
 Botón GIS "Continuar con Google" ─┐                    ├ LimitadorDeIntentos (Caffeine; por mail e IP)
   (renderButton, popup, callback) │ credential (JWT)   ├ AuthService.registrar/login (BCrypt, normaliza mail,
                                   └──────────────►    │    DNI, teléfono E.164) ──► NotificacionesService (@Async)
 POST /api/auth/google {credential}                     ├ GoogleAuthService
                                                        │    └ GoogleIdTokenVerifier (NimbusJwtDecoder.withJwkSetUri) ◄── JWKS googleapis.com/oauth2/v3/certs
                                                        │         iss, aud=GOOGLE_CLIENT_ID, exp, email_verified, sub
                                                        │    └ vincular por sub → por mail (D-06 + anti pre-hijacking)
                                                        ├ TokenCuentaService (SecureRandom 32B → SHA-256 → tokens_cuenta)
                                                        │    confirmar mail 24 h / restablecer contraseña 1 h, un solo uso (UPDATE condicional)
 /confirmar-email?token=  ──POST──►                     └ EmailSender ── BrevoEmailSender (RestClient, HTTPS 443) ──► api.brevo.com/v3/smtp/email
 /olvide-contrasena  ──POST──► (200 siempre igual)                       └ LogEmailSender (dev/test: loguea el link)
 /restablecer-contrasena?token= ──POST──►

 AuthContext (usuario + token en localStorage)          UsuarioController  (/api/usuarios/me, PUT, contraseña, reenviar)
   └ al montar: GET /api/usuarios/me  ◄───────────────   └ UsuarioService → UsuarioResponse {faltantes[], cuentaVerificada}
 Botones "Lo quiero", "Cotizá tu usado",
 "Mis mensajes", "Enviar consulta"
   └ evaluarAcceso(usuario) → anónimo → /login        ConsultaController POST /api/consultas  (authenticated, COMPRADOR)
                              incompleta → /completar-datos      └ ConsultaService.crear → VerificacionCuenta.exigir(usuario)
                              verificada → acción                     └ 403 {error, codigo:"CUENTA_NO_VERIFICADA", faltantes}
   (state.from se conserva)                                                                              PostgreSQL ◄── Flyway V1..V4 (ddl-auto: validate)
                                                                                                          usuarios(+apellido, dni UQ, email_confirmado, google_sub UQ,
                                                                                                                   password_cambiada_en, password_hash NULL), tokens_cuenta
 Arranque: SecretosGuard (JWT, DB, Cloudinary + BREVO_API_KEY, MAIL_REMITENTE_EMAIL, GOOGLE_CLIENT_ID, APP_FRONTEND_URL fuera de dev)
```

### Recommended Project Structure
```
danteautomotores-back/src/main/java/com/danteautomotores/
├── controller/UsuarioController.java            # GET/PUT /api/usuarios/me, contraseña, reenviar-confirmacion
├── controller/AuthController.java               # + /google, /confirmar-email, /olvide-contrasena, /restablecer-contrasena
├── dto/usuario/                                 # UsuarioResponse, ActualizarPerfilRequest, CambiarContrasenaRequest
├── dto/auth/                                    # RegistroRequest (ampliado), GoogleLoginRequest, ConfirmarEmailRequest,
│                                                #   OlvideContrasenaRequest, RestablecerContrasenaRequest, AuthResponse (ampliado)
├── entity/Usuario.java (ampliada), entity/TokenCuenta.java, enums/TipoTokenCuenta.java, enums/DatoFaltante.java
├── repository/TokenCuentaRepository.java
├── service/AuthService.java, GoogleAuthService.java, UsuarioService.java, TokenCuentaService.java,
│        VerificacionCuenta.java, NotificacionesService.java (@Async), LimitadorDeIntentos.java
├── service/identidad/NormalizadorDeContacto.java    # DNI + teléfono (libphonenumber)
├── mail/EmailSender.java, MensajeEmail.java, BrevoEmailSender.java, LogEmailSender.java, PlantillasEmail.java
├── security/GoogleIdTokenVerifier.java
├── exception/CuentaNoVerificadaException.java, LimiteDeIntentosException.java (429)
├── config/MailConfig.java, AsyncConfig.java         # + SecretosGuard ampliado
└── resources/db/migration/V4__fase3_cuentas_verificadas.sql

danteautomotores-front/src/
├── components/BotonGoogle.jsx (+ hook useGoogleIdentity), components/BannerCuentaIncompleta.jsx (opcional)
├── pages/CompletarDatosPage.jsx, PerfilPage.jsx, OlvideContrasenaPage.jsx, RestablecerContrasenaPage.jsx,
│        ConfirmarEmailPage.jsx, PrivacidadPage.jsx, MisMensajesPage.jsx (placeholder "Próximamente")
├── utils/cuenta.js (+ cuenta.test.js)          # evaluarAcceso, destinoPostLogin, sanitizarDestino, normalizarDni
└── (se editan) context/AuthContext.jsx, services/api.js, pages/LoginPage.jsx, RegistroPage.jsx,
    pages/PublicacionDetallePage.jsx, components/Navbar.jsx, SeccionFinanciamiento.jsx, routes/AppRouter.jsx, vercel.json
```

### Pattern 1: Migración V4 (probada contra Postgres 16.15 local)

**What:** una sola migración aditiva más un índice único funcional; no muta filas existentes.
**Verificado** en una base descartable (ya borrada): las dos filas legacy quedan con `dni NULL` y el `UNIQUE (dni)` **no** las hace colisionar; un segundo `UPDATE` con el mismo DNI falla con `duplicate key value violates unique constraint "uk_usuarios_dni"`; el `CHECK` rechaza `0123456`; `password_hash` acepta NULL. `[VERIFIED: ejecución local psql contra danteautomotores-db]`

`V4__fase3_cuentas_verificadas.sql` (ASCII, sin acentos, como V1–V3):
```sql
-- Fase 3: identidad completa y cuentas verificadas (D-01, D-04, D-06, D-09, D-10).
-- Todo es aditivo: la version anterior del back sigue funcionando contra esta base (rollback sin tocar datos).
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS apellido varchar(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS dni varchar(8);
-- DEFAULT false: los mails de las cuentas existentes NO se dan por confirmados (D-10).
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email_confirmado boolean NOT NULL DEFAULT false;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS google_sub varchar(255);
-- Se actualiza al restablecer/cambiar la contrasena o al unir una cuenta con Google: los JWT emitidos antes dejan de valer.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS password_cambiada_en timestamp(6);
-- Una cuenta solo-Google no tiene contrasena.
ALTER TABLE usuarios ALTER COLUMN password_hash DROP NOT NULL;

-- En Postgres un UNIQUE permite muchos NULL: las cuentas viejas (sin DNI) no chocan entre si (D-09).
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_dni UNIQUE (dni);
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_google_sub UNIQUE (google_sub);
ALTER TABLE usuarios ADD CONSTRAINT usuarios_dni_formato CHECK (dni IS NULL OR dni ~ '^[1-9][0-9]{6,7}$');
-- El mail se compara sin distinguir mayusculas (Google devuelve minusculas; el login actual es sensible a mayusculas).
-- Sin UPDATE de filas: si ya hay dos mails que difieren solo en mayusculas esta sentencia falla (ver runbook H1).
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_email_lower ON usuarios (lower(email));

-- Los admins no compran ni cotizan: su mail se da por confirmado para que nunca los moleste el gate.
UPDATE usuarios SET email_confirmado = true WHERE rol = 'ADMIN';

CREATE TABLE tokens_cuenta (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    usuario_id bigint NOT NULL,
    tipo varchar(30) NOT NULL,
    token_hash varchar(64) NOT NULL,
    creado_en timestamp(6) NOT NULL,
    expira_en timestamp(6) NOT NULL,
    usado_en timestamp(6),
    CONSTRAINT tokens_cuenta_pkey PRIMARY KEY (id),
    CONSTRAINT uk_tokens_cuenta_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_cuenta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_tokens_cuenta_usuario_tipo ON tokens_cuenta (usuario_id, tipo);
```
Notas para el planner:
- La entidad nueva y la migración van **juntas** (`ddl-auto: validate`): `Usuario` suma `apellido`, `dni`, `@Column(name="email_confirmado", nullable=false) boolean emailConfirmado`, `googleSub`, `passwordCambiadaEn`, y `passwordHash` pasa a `nullable = true`. `TokenCuenta` mapea `tokens_cuenta`. `validate` no mira índices ni `CHECK`.
- `tipo` sin `CHECK` (igual criterio que Fase 2): sumar un tipo de token no exige otra migración.
- **Normalizar el mail a minúsculas en todo lo nuevo** (registro, login, Google) y usar búsqueda sin distinguir mayúsculas (`findByEmailIgnoreCase`) también en `CustomUserDetailsService`. No hay `UPDATE ... lower(email)` a propósito: así, volver a la versión anterior del back no deja sin acceso a nadie con mail en mayúsculas.
- `DataSeeder` debe crear el admin con `emailConfirmado(true)` (hoy el builder no lo setea → quedaría `false` en una base vacía).
- `email_confirmado` y los demás campos nuevos no rompen los tests existentes que arman `Usuario.builder()` (`CatalogoPostgresTest`, `PublicacionServiceTest`) porque son nullable / `false` por defecto.
- `MigracionesPostgresTest` (hoy espera "recibe solo V2 y V3", `MigracionesPostgresTest.java:33,167`) se amplía: versión final `4`, filas legacy con `dni NULL` y `email_confirmado=false` (COMPRADOR) / `true` (ADMIN), DNI duplicado rechazado, dos mails que difieren en mayúsculas rechazados.

### Pattern 2: Login con Google — ID token verificado en el back

**Decisión:** GIS `renderButton` (modo `popup`, default) + `POST /api/auth/google {credential}`. No se necesita client secret, ni redirect URIs, ni `g_csrf_token` (eso es del modo `login_uri`/redirect `[CITED: developers.google.com/identity/gsi/web/guides/verify-google-id-token]`).

**Backend: verificador (probado con la JVM 17; ver §Code Examples).** Validaciones exigidas por Google: firma con las claves de `https://www.googleapis.com/oauth2/v3/certs`, `iss` ∈ {`accounts.google.com`, `https://accounts.google.com`}, `aud` = tu Client ID, `exp` vigente `[CITED: developers.google.com/identity/gsi/web/guides/verify-google-id-token]`. Más: `email_verified` debe ser `true` (D-06) y el identificador estable es **`sub`**, no el mail: "Only use Google ID token sub field as identifier for the user ... don't use email address as an identifier" `[CITED: idem]`.

**Servicio `GoogleAuthService.entrar(credential)` — algoritmo:**
1. Verificar el token (excepción → 401 uniforme "No pudimos verificar tu cuenta de Google").
2. Exigir `email_verified == true`; si no → 400 "Google no confirmó tu mail: usá tu mail y contraseña." (D-06 vincula solo con mail confirmado).
3. Buscar por `google_sub`. Si existe → login (ya vinculada).
4. Si no, buscar por mail (sin mayúsculas). Si existe:
   - setear `google_sub`; `emailConfirmado = true` (Google lo confirmó);
   - **si la cuenta tenía `emailConfirmado == false` y `passwordHash != null`: poner `passwordHash = null` y `passwordCambiadaEn = ahora`** (anti pre-hijacking; el dueño real define una nueva contraseña con "olvidé mi contraseña" y eso además deja la cuenta lista para los dos métodos). Si ya tenía el mail confirmado, la contraseña se conserva (D-06: "puede usar los dos métodos").
   - Si es ADMIN → **rechazar la unión** (400): una cuenta admin no se alcanza por Google en esta fase (la regla "ningún endpoint promueve admins" de Fase 1 se extiende a "ninguna vía alternativa de login a un admin sin decisión explícita"). `[ASSUMED: A6]`
5. Si no existe → crear COMPRADOR: `nombre = given_name ?: name`, `apellido = family_name` (puede ser `null`), sin teléfono ni DNI, `passwordHash = null`, `emailConfirmado = true`, `googleSub = sub`.
6. Devolver el mismo `AuthResponse` que el login normal (token + perfil con `faltantes`). El front, si `faltantes` no está vacío, lleva a `/completar-datos` (D-07).
7. Concurrencia: dos clicks simultáneos pueden crear dos cuentas → el `UNIQUE (google_sub)` y el índice `lower(email)` hacen fallar a una; capturar `DataIntegrityViolationException` alrededor del `save` y reintentar la búsqueda una vez.

**Frontend:** componente `BotonGoogle` que (a) carga el script una sola vez (`<script src="https://accounts.google.com/gsi/client" async defer>`), (b) llama a `google.accounts.id.initialize({ client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID, callback })` una sola vez (guardar en una variable de módulo: React `StrictMode` monta dos veces), (c) `renderButton(contenedor, { type: 'standard', theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular', logo_alignment: 'center', width: Math.min(400, contenedor.offsetWidth) })` (el ancho máximo es 400 px `[CITED: js-reference]`), (d) en el callback hace `POST /auth/google` y reutiliza `destinoPostLogin`. Si `VITE_GOOGLE_CLIENT_ID` no está definido, el componente muestra un aviso discreto (o nada) en vez de romper el desarrollo local. Reemplaza al botón decorativo y su `avisarGoogle` en `LoginPage.jsx` y `RegistroPage.jsx`; `IconoGoogle.jsx` queda sin uso (se puede borrar). El botón renderizado por Google no se puede re-estilizar más allá de `theme/size/text/shape/width`; es el costo de recibir un ID token directo.

### Pattern 3: Tokens de confirmación de mail y de restablecimiento (opacos, hasheados, un solo uso)

**Decisión:** token opaco aleatorio (no JWT firmado): se puede revocar/invalidar de verdad, no necesita secreto ni expone claims en la URL, y el "un solo uso" requiere estado en base de todas formas. OWASP exige que los tokens sean CSPRNG, largos, **guardados hasheados**, de un solo uso, con vencimiento y con límite de intentos `[CITED: cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html]`.

- Generación: `SecureRandom` 32 bytes → `Base64.getUrlEncoder().withoutPadding()` (43 caracteres) para la URL; en base se guarda `SHA-256(token)` en hex (64 caracteres, `token_hash`). Con 256 bits de entropía no hace falta sal ni comparación en tiempo constante (la búsqueda es por el hash indexado).
- Vigencias: `CONFIRMAR_EMAIL` 24 h (D-02), `RESTABLECER_CONTRASENA` 60 min (D-14). Configurables (`app.seguridad.*`), con `Clock` inyectado (ya existe `ClockConfig`) para testear vencimiento.
- Emisión: antes de insertar uno nuevo se borran los anteriores del mismo `(usuario, tipo)` (`deleteByUsuarioIdAndTipo`): un solo link vivo por vez; mantiene la tabla chica sin job de limpieza.
- Consumo **atómico** (un solo uso aun con dos requests simultáneas): `@Modifying @Query("update TokenCuenta t set t.usadoEn = :ahora where t.tokenHash = :hash and t.tipo = :tipo and t.usadoEn is null and t.expiraEn > :ahora")` devuelve `1` solo para quien gana. Si devuelve `0` → mensaje genérico "El link no es válido o ya venció. Pedí uno nuevo." (no distingue vencido/usado/inexistente).
- Confirmación de mail: idempotente. `POST /api/auth/confirmar-email {token}` → marca `emailConfirmado = true`. La página del front hace el `POST` al cargar y después `navigate(..., { replace: true })` para sacar el token de la URL/historial. **Usar POST desde la página y no un `GET` con efecto**: escáneres de mail y prefetch de enlaces ejecutan GET y consumirían el token.
- Restablecimiento: `POST /api/auth/olvide-contrasena {email}` responde **siempre 200 con el mismo texto** ("Si el mail tiene una cuenta, te mandamos un link…") (D-14); el envío es asíncrono para que el tiempo de respuesta sea el mismo exista o no la cuenta (OWASP pide mensaje y tiempo consistentes). `POST /api/auth/restablecer-contrasena {token, password}`: consume el token, setea `passwordHash`, `passwordCambiadaEn = ahora`, **`emailConfirmado = true`** (el link llegó a la casilla), borra los tokens pendientes del usuario, y manda un mail de aviso "tu contraseña fue cambiada". **No** devuelve un JWT: OWASP "Do not automatically authenticate the user after password creation"; el front lleva a `/login` con un mensaje de éxito. Una cuenta solo-Google puede definir contraseña por esta vía (queda con los dos métodos).
- El link se arma **con `APP_FRONTEND_URL` configurada**, nunca con el `Host`/`Origin` de la request (OWASP: "Avoid relying on the Host header"): `${APP_FRONTEND_URL}/confirmar-email?token=...` y `${APP_FRONTEND_URL}/restablecer-contrasena?token=...`.
- Referer: el default de los navegadores (`strict-origin-when-cross-origin`) no manda path ni query a otros orígenes. Agregar explícitamente `Referrer-Policy: strict-origin-when-cross-origin` en `vercel.json` (Google también lo recomienda `[CITED: google doc get-google-api-clientid]`); no usar `no-referrer` global (OWASP lo sugiere solo para las páginas de reset; en una SPA se puede poner un `<meta name="referrer">` dinámico, opcional).

**Invalidación de sesiones (recomendado, el planner puede recortar):** OWASP pide "enforce automatic invalidation" o consultarlo. Con JWT sin estado: el token lleva un claim `pca` = epoch-segundos de `passwordCambiadaEn` (o `0`); `CustomUserDetailsService` devuelve un `UserDetails` que expone ese valor y `JwtAuthenticationFilter` rechaza el token si el claim es menor. El cambio de contraseña desde el perfil responde con un token nuevo para la sesión actual. Impacto en tests: `SeguridadWebMvcTestBase.bearerPara` arma `User` plano → el claim ausente debe equivaler a `0`.

**Límite de intentos (en memoria, una instancia de Railway):** `LimitadorDeIntentos` con `Cache<String, AtomicInteger>` de Caffeine (`expireAfterWrite(ventana)`, `maximumSize(10_000)`), `Clock` o ventana inyectable. Valores iniciales sugeridos (`[ASSUMED: A4]`, ajustables):

| Acción | Clave | Límite | Al excederlo |
|--------|-------|--------|--------------|
| Login | `login:<mail>` y `login-ip:<ip>` | 10 por mail / 30 por IP cada 15 min | 429 "Demasiados intentos. Esperá unos minutos o recuperá tu contraseña." |
| Registro | `registro-ip:<ip>` | 10 por hora | 429 |
| Olvidé mi contraseña | `reset:<mail>` y `reset-ip:<ip>` | 3 por mail / 10 por IP por hora | **200 igual** (sin mail): no filtrar nada |
| Reenviar confirmación | `confirm:<usuarioId>` | 3 por hora | 429 |
| Google | `google-ip:<ip>` | 30 por 15 min | 429 |
| **Tope global de mails** | `mails:dia` | 250 por día (el plan gratis son 300/día en total) | no envía y loguea ERROR |

El bloqueo por mail permite el DoS de "bloquearle la cuenta a alguien"; se mitiga porque la recuperación de contraseña **no** se bloquea por el contador de login y porque es un bloqueo temporal (OWASP: atar el límite a la cuenta y no solo a la IP, y dejar abierta la recuperación `[CITED: cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html]`). IP del cliente: Railway agrega la IP real al final de `X-Forwarded-For`; hay informes contradictorios sobre si el borde borra valores enviados por el cliente (una fuente dice que el primer valor es la IP real y que no se puede pisar, otra que no se limpian) `[CITED: station.railway.com threads, LOW]`. Tratar la IP como **mejor esfuerzo** (tomar el primer valor de `X-Forwarded-For`, caer a `getRemoteAddr()`), y confiar en los límites por mail/usuario como la defensa real. `[ASSUMED: A5]`

Los límites se reinician con cada deploy y no se comparten entre instancias: aceptable con una sola instancia; si se escala, mover a base o Redis (anotar como deuda).

### Pattern 4: Normalización de DNI y teléfono (D-03, D-05)

- **DNI:** quitar puntos, espacios y guiones → `^[1-9][0-9]{6,7}$` (7 u 8 dígitos, sin ceros a la izquierda; ningún DNI argentino empieza en 0 `[ASSUMED: A7]`). Se guarda sin puntos. El mismo patrón va en el `CHECK` de V4 (defensa en profundidad). Solo formato (D-05).
- **Teléfono:** libphonenumber con región `AR`. Algoritmo: `parse(raw, "AR")` → exigir `getCountryCode() == 54` y `isValidNumberForRegion(p, "AR")` → NSN (`getNationalSignificantNumber`) de 10 dígitos (fijo o móvil sin 9) u 11 dígitos que empiezan con 9 (móvil) → canónico **`+549` + 10 dígitos**. Se agrega el 9 si falta porque el campo es "celular" (D-03) y para que `https://wa.me/549…` funcione; el back puede dar a las fases 4/5 el número en E.164 sin recomputar.
- **Validado en esta sesión** (JVM 17 + libphonenumber 9.0.40 `[VERIFIED: ejecución local T.java/T2.java]`). Entradas → E.164 de libphonenumber / NSN:

| Entrada | `isValidNumberForRegion` | NSN | Canónico a guardar |
|---------|-------------------------|-----|--------------------|
| `011 15 1234-5678` | true (MOBILE) | `91112345678` | `+5491112345678` |
| `+54 9 11 1234-5678` / `+5491112345678` | true | `91112345678` | `+5491112345678` |
| `11 1234-5678` / `11-15-1234-5678` | true (FIXED_LINE / MOBILE) | `1112345678` / `91112345678` | `+5491112345678` |
| `0351 15 6123456` / `351 15 6123456` | true (MOBILE) | `93516123456` | `+5493516123456` |
| `02954 15 412345` | true | `92954412345` | `+5492954412345` |
| `+54 9 223 512 3456` | true | `92235123456` | `+5492235123456` |
| `+34 612 345 678` | false (cc=34) | — | **rechazar** |
| `15 1234 5678` (sin código de área) | false | `1512345678` | **rechazar** |
| `+54 9 11 1234-567` (le falta un dígito) | false | `9111234567` | **rechazar** |
| `abc` | `NumberParseException` | — | **rechazar** |

  Limitación honesta: un número `11 6123-4567` sin `15` figura como `FIXED_LINE` en la metadata de libphonenumber; el sistema no puede distinguir fijo de celular cuando el usuario no escribe el 15/9, así que se acepta cualquier número argentino válido y se lo guarda como celular. D-03 pide solo "celular argentino válido (código de área + número)".
- Los dos normalizadores viven en `NormalizadorDeContacto` (puros, sin Spring, testeables), el DTO valida con `@NotBlank` y el servicio normaliza y lanza `ReglaDeNegocioException` con mensaje claro ("Ingresá un celular argentino válido, con código de área. Ej: 11 2345-6789"). El front hace solo una validación liviana (10–13 dígitos tras limpiar); el back es la fuente de verdad.
- Migración de datos: el `telefono` de las cuentas viejas queda como está (texto libre, opcional); se normaliza cuando el usuario lo completa en `/completar-datos`. El gate considera "teléfono presente" solo si `telefono` parsea como válido (si el viejo es basura, `faltantes` incluye `TELEFONO`).

### Pattern 5: Registro ampliado y contrato de errores

`RegistroRequest`: `nombre`, `apellido` (`@NotBlank @Size(max=100)`), `email` (`@NotBlank @Email`), `password` (`@NotBlank @Size(min=8, max=72)`), `telefono` y `dni` **`@NotBlank`** (AUTH-01: sin ellos se rechaza), más `aceptaPrivacidad` (`@AssertTrue`, opcional pero recomendado por Ley 25.326). **`max=72`**: BCrypt considera solo los primeros 72 bytes `[VERIFIED: ejecución local — un encode de 80 caracteres "matchea" con el prefijo de 72]`; OWASP pide permitir al menos 64 `[CITED: Authentication_Cheat_Sheet]`.
- Cuidado con Lombok: `RegistroRequest` es `@Data` (`RegistroRequest.java:8`), así que `toString()` imprime contraseña, DNI y teléfono. Agregar `@ToString.Exclude` en `password`, `dni`, `telefono` (y en los DTOs nuevos que lleven esos datos), o usar `@Getter/@Setter` sin `@ToString`.
- Orden en `AuthService.registrar`: normalizar mail (trim + minúsculas) → `existsByEmailIgnoreCase` (se mantiene el mensaje actual "Ya existe una cuenta con ese email", que `AuthServiceTest` ya fija; es un compromiso de UX consciente respecto de la recomendación OWASP de mensaje genérico, anotado en Open Questions) → normalizar teléfono y DNI → `existsByDni` → mensaje D-04: **"Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña."** (400, sin decir de quién) → guardar → emitir token de confirmación y encolar el mail (asíncrono; si el mail falla el registro **no** falla: el usuario puede reenviar) → devolver token + perfil.
- Carreras: `existsBy…` + `save` no es atómico; el `UNIQUE` de la base es la garantía. `AuthService` hoy **no** es `@Transactional` (cada `repository.save` confirma por su cuenta), así que se puede capturar `DataIntegrityViolationException` alrededor del `save` y traducirla a la misma `ReglaDeNegocioException`; si se agrega `@Transactional`, usar `saveAndFlush` para que la excepción salte dentro del método.
- **No loguear PII por la vía del handler de integridad.** `GlobalExceptionHandler.java:63` hace `log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage())`. Con el nuevo `UNIQUE`, el mensaje de Postgres incluye `DETAIL: Key (dni)=(12345678) already exists.` y con el `CHECK` incluye `Failing row contains (2, b@x.com, h, B, null, 0123456, ...)` `[VERIFIED: salida de psql contra el Postgres local]`. Cambiar ese log a registrar solo la clase y el nombre de la constraint (`ConstraintViolationException#getConstraintName`), o un texto fijo, para no volcar DNI/mails al log (Ley 25.326 / CONTEXT).
- Nuevo mapeo en `GlobalExceptionHandler` (el formato uniforme se mantiene): `LimiteDeIntentosException` → 429 `{"error":"Demasiados intentos…"}` con header `Retry-After`; `CuentaNoVerificadaException` → 403 `{"error":"…","codigo":"CUENTA_NO_VERIFICADA","faltantes":["DNI","EMAIL_SIN_CONFIRMAR"]}` (campos extra aditivos: `mensajeDeError` del front solo lee `error`/`campos`).

### Pattern 6: Estado "verificada", gate del back y perfil

**Dónde viaja el estado:** NO en el JWT. `[ASSUMED: A1 — decisión de diseño; D-01 la deja a criterio]` Razones: cambia durante la vida del token (24 h: confirmar el mail, completar datos, o el admin corrigiendo un DNI) y las fases 4/5 deben decidir con el estado real. El token sigue siendo `sub=email` (`JwtService.generateToken`). El back evalúa cada vez desde la base (el filtro ya hace `loadUserByUsername` en cada request, así que no agrega costo relevante); el front lo lee de `GET /api/usuarios/me` y de la respuesta de login/registro/Google.

**`VerificacionCuenta`** (servicio chico, reutilizado por Fases 4 y 5):
```java
// faltantes(): nombre siempre está (NOT NULL). ADMIN => lista vacia (no compra ni cotiza; nunca ve "Completa tus datos").
List<DatoFaltante> faltantes(Usuario u);           // APELLIDO, TELEFONO, DNI, EMAIL_SIN_CONFIRMAR
boolean estaVerificada(Usuario u);                 // faltantes(u).isEmpty()
void exigir(Usuario u);                            // lanza CuentaNoVerificadaException(faltantes)
```
- `ConsultaService.crear(ConsultaRequest, Usuario usuarioAutenticado)`: llama `verificacion.exigir(...)`, **toma nombre/mail/teléfono de la cuenta** (`nombreComprador = nombre + " " + apellido`, `emailComprador`, `telefonoComprador`) y de la request solo `publicacionId` y `mensaje` (D-11). La entidad `Consulta` y su tabla no cambian (los tres campos siguen `NOT NULL`: se rellenan como foto de los datos al momento de la consulta; la Fase 4 la reemplaza por conversaciones). `ConsultaRequest` deja de tener `nombreComprador/emailComprador/telefonoComprador` (Jackson de Boot ignora propiedades desconocidas, así que un front viejo que los mande no rompe).
- `SecurityConfig.java:50` hoy dice `.requestMatchers(HttpMethod.POST, "/api/consultas/**").permitAll()` → cambiar a `.requestMatchers(HttpMethod.POST, "/api/consultas/**").hasRole("COMPRADOR")` (sin token → 401 JSON; admin → 403). La línea 56 `.requestMatchers("/api/consultas/**").hasRole("ADMIN")` sigue protegiendo el listado. `/api/auth/**` (línea 49) es `permitAll` entero: **no** poner `me` ahí; usar `/api/usuarios/me` (cae en `anyRequest().authenticated()`, línea 61).
- Endpoints: `GET /api/usuarios/me` → `UsuarioResponse {id, nombre, apellido, email, telefono, dni, rol, emailConfirmado, tieneContrasena, tieneGoogle, cuentaVerificada, faltantes[]}` (el DNI y el teléfono se devuelven **solo** a su dueño; nunca en respuestas públicas ni en `PublicacionResponse`). `PUT /api/usuarios/me {nombre, apellido, telefono, dni?}`: `nombre`, `apellido` y `telefono` editables; `dni` solo se acepta si hoy es `null` (completar datos); si ya está cargado y el valor difiere → 400 "El DNI no se puede modificar desde la web. Escribinos si hay un error." (CONTEXT: DNI no editable una vez cargado); el mail no se acepta. `POST /api/usuarios/me/contrasena {actual, nueva}` (exige la actual; para cuentas sin contraseña → 400 "Tu cuenta ingresa con Google: definí una contraseña desde 'Olvidé mi contraseña'."; responde con token nuevo si se implementó la invalidación). `POST /api/usuarios/me/reenviar-confirmacion` (autenticado, con límite; si ya está confirmado → 200 sin enviar).
- `AuthResponse` mantiene `token, nombre, email, rol` (el front desplegado hoy los lee en el nivel superior) y suma `usuario` (el `UsuarioResponse` sin DNI) o al menos `cuentaVerificada` y `faltantes`. Aditivo: no rompe a un front viejo durante el deploy.
- Cuentas solo-Google y `passwordHash == null` (**trampa verificada**): `CustomUserDetailsService.java:26` y `AuthService.java:59` hacen `.password(usuario.getPasswordHash())`; `User.builder().password(null)` lanza `IllegalArgumentException: password cannot be null` `[VERIFIED: ejecución local]`, y `JwtAuthenticationFilter.java:58` captura `IllegalArgumentException` y deja la request **sin autenticar** en silencio → el usuario de Google vería 401 en todo. Usar una constante inválida (`"!"`: BCrypt devuelve `false` y solo emite un warn `[VERIFIED]`) cuando el hash es `null`, y que `AuthService.login` responda el mismo 401 genérico ("Email o contraseña incorrectos") para cuentas sin contraseña. Refactorizar la generación del token para recibir el `Usuario` (hoy `construirRespuesta` arma un `UserDetails` a mano).

### Pattern 7: Mails — interfaz propia + Brevo por API REST

```java
public interface EmailSender { void enviar(MensajeEmail mensaje); }          // sincrona, lanza ServicioExternoException
public record MensajeEmail(String paraEmail, String paraNombre, String asunto, String html, String texto) {}
```
- `BrevoEmailSender` (producción): `POST {BREVO_API_URL}/v3/smtp/email`, headers `api-key`, `accept: application/json`, `content-type: application/json`; cuerpo `{sender:{name,email}, to:[{email,name}], subject, htmlContent, textContent, replyTo?}`; éxito `201` con `messageId` `[CITED: developers.brevo.com/reference/sendtransacemail]`. Cualquier 4xx/5xx/timeout → `ServicioExternoException` (ya existe; el advice la mapea a 502). Timeouts: conexión 5 s, lectura 10 s (`JdkClientHttpRequestFactory.setReadTimeout`; Boot 3.3 no tiene todavía `spring.http.client.*` `[ASSUMED: A9]`). No loguear el cuerpo ni el link; loguear `tipo` y `messageId`.
- `LogEmailSender` (dev/test, selección en `MailConfig`): loguea destinatario, asunto y **el link** (es el punto: poder probar el flujo a mano); jamás activo en producción (ver guard).
- Selección: `MailConfig` crea `BrevoEmailSender` si `app.mail.brevo.api-key` no está en blanco, y `LogEmailSender` si lo está. `SecretosGuard` (extender, mismo patrón `fallarOAvisar`): **fuera del modo desarrollo exigir** `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `APP_FRONTEND_URL` (no `localhost`) y `GOOGLE_CLIENT_ID`; así un deploy sin Brevo nunca "manda" los links de reset al log. Los tests de `SecretosGuardTest` arman el guard con `ReflectionTestUtils.setField` (`SecretosGuardTest.java:35-39`): agregar los campos nuevos ahí.
- Envío asíncrono: `NotificacionesService` con `@Async` sobre un `ThreadPoolTaskExecutor` chico (core 2, max 4, cola 100; requiere `@EnableAsync`) y **después** de confirmada la transacción que guardó el token (el servicio que emite el token no es `@Transactional` alrededor del envío, o se usa `@TransactionalEventListener(phase = AFTER_COMMIT)`), para que el link no llegue antes de que el token exista. Fallos: se loguean y no afectan la respuesta.
- Plantillas (`PlantillasEmail`, español, estilo sobrio): confirmar mail, restablecer contraseña, "tu contraseña fue cambiada". Texto plano + HTML simple. La Fase 4 reutiliza la misma interfaz para los avisos de mensaje nuevo.
- Variables: `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `MAIL_REMITENTE_NOMBRE` (default "Dante Automotores"), `MAIL_RESPONDER_A` (opcional, `replyTo`), `APP_FRONTEND_URL`, `BREVO_API_URL` (default `https://api.brevo.com`, útil para tests con un servidor local).
- Límites y sender (D-13): **el plan gratis permite 300 mails/día en total (campañas + transaccionales) y exige activar el envío transaccional la primera vez** `[CITED: help.brevo.com FAQs free plan, vía búsqueda]`; el remitente debe estar **creado y validado** en Brevo (código de 6 dígitos al mail del remitente) o el envío es rechazado `[CITED: developers.brevo.com/docs/sender-creation-and-management, vía búsqueda]`. **Un remitente `@gmail.com`/`@yahoo.com` no se puede autenticar y Brevo reemplaza el dominio por `@brevosend.com`** `[CITED: help.brevo.com domain authentication, vía búsqueda: "Brevo will auto-replace your domain with @brevosend.com"]` (no pude abrir la página oficial: 403). Consecuencia práctica: los mails salen desde el dominio compartido de Brevo (reputación compartida; pueden caer en spam y los destinatarios ven "vía brevosend.com"). Es lo que permite D-13, pero hay que **probarlo con casillas reales de Gmail y Outlook** (§Runbook H2) y dejar listo el camino a un dominio propio (Brevo: registro `Brevo code` TXT, DKIM y DMARC `[CITED: help.brevo.com/hc/en-us/articles/12163873383186]`).
- **IPs autorizadas de Brevo vs Railway:** si en Brevo está activo "Block unknown IP addresses", las llamadas desde Railway fallan con 401 por IP rotativa (hay un hilo reciente en Railway Central Station `[CITED: station.railway.com "Brevo IP authorization breaking…"]`) → en el checkpoint de Brevo hay que **desactivar el bloqueo de IPs desconocidas**.

### Pattern 8: Front — gate, vuelta a origen y "Completá tus datos"

- `utils/cuenta.js` (puro, con `node:test`): `evaluarAcceso(usuario)` → `'anonimo' | 'incompleta' | 'verificada'` (`incompleta` si `faltantes.length > 0`; un usuario guardado en `localStorage` por la versión anterior no tiene `faltantes`: tratarlo como **desconocido** y refrescar `/me` antes de decidir), `destinoDeGate(estado, desde)` → `{ ruta: '/login' | '/completar-datos' | null, state: { from } }`, `destinoPostLogin(usuario, from)` (si incompleta → `/completar-datos` conservando `from`; si no → `from` o `/`), `sanitizarDestino(origen)` (solo rutas internas: empieza con `/`, no con `//` ni `/\`), `normalizarDni(texto)`, `validarTelefonoBasico(texto)`. La lógica de `destino` hoy está inline en `LoginPage.jsx` (`origen.startsWith('/') && !origen.startsWith('//')`): extraerla.
- `AuthContext`: `registrar(datos)`, `login`, `loginConGoogle(credential)`, `refrescarUsuario()` (`GET /usuarios/me` → `setUsuario`), y **rehidratación al montar**: si hay `token` en `localStorage` y el `usuario` guardado no tiene `faltantes`, pedir `/me` (sesiones iniciadas antes del deploy). El `usuario` guardado conserva solo lo no sensible (`nombre`, `apellido`, `email`, `rol`, `cuentaVerificada`, `faltantes`, `emailConfirmado`); **no** guardar DNI ni teléfono en `localStorage` (se piden a `/me` cuando hacen falta). `esAdmin` sin cambios.
- `api.js`: sin cambios en el 401 (ya ignora `/auth/*`). Agregar un manejador análogo al de sesión vencida para `403` con `data.codigo === 'CUENTA_NO_VERIFICADA'`: `refrescarUsuario()` y navegar a `/completar-datos` con `state.from = ruta actual` (el servidor es la autoridad aunque el estado del front esté viejo).
- Gate de botones (AUTH-06): `useExigirCuenta()` devuelve `exigir(accion)`; si `anonimo` → `navigate('/login', { state: { from: pathname + search } })`; si `incompleta` → `navigate('/completar-datos', …)`; si `verificada` → ejecuta `accion`. Aplicar a: **"Lo quiero"** (hoy el botón de la ficha se llama "Reservar o agendar visita", `PublicacionDetallePage.jsx:553-556`, llama a `requiereCuenta`; renombrar a "Lo quiero" — ver Open Question 2), **"Cotizar"** de la ficha (`:544-547`), "Simulá tu financiamiento" (`:528`), **"Vender tu auto"** del `Navbar` y **"Cotizá tu usado"** de `SeccionFinanciamiento` (hoy son `<a href=wa.me>` externos → pasar a `<button>` que, tras el gate, abren el link actual), y **"Mis mensajes"** (no existe: agregar el ítem al `Navbar` para usuarios con sesión y la ruta `/mensajes` protegida con una página "Próximamente"; la Fase 4 la llena). Estos botones solo ponen el gate; sus flujos reales llegan en las Fases 4 y 5 (CONTEXT). El aviso temporal "Esta función todavía no está conectada" se conserva para el caso verificado.
- "Enviar consulta" (D-11): el formulario de la ficha deja de pedir nombre/mail/teléfono y solo muestra el textarea; si el usuario es anónimo o incompleto en lugar del form se muestra el CTA de gate ("Ingresá para consultar" / "Completá tus datos para consultar"); el `POST /consultas` manda `{ publicacionId, mensaje }`.
- Rutas nuevas en `AppRouter`: `/completar-datos` y `/perfil` y `/mensajes` (`ProtectedRoute`), `/confirmar-email`, `/olvide-contrasena`, `/restablecer-contrasena`, `/privacidad` (públicas). `ProtectedRoute` ya redirige con `state.from`.
- Vuelta a origen incluyendo Google: el botón de GIS en modo popup no navega, así que `location.state.from` sigue ahí. `RegistroPage` hoy hace `navigate('/')` ignorando `from` y el link "Creá una gratis" de `LoginPage` no lo propaga: pasar `state={{ from }}` en ambos sentidos.
- "Completá tus datos" (`/completar-datos`, D-07/D-08): se muestra apenas entra una cuenta `incompleta` (decisión en `destinoPostLogin`); precarga nombre/apellido (Google o cuenta), pide apellido (si falta), teléfono y DNI, con la leyenda de privacidad; incluye el estado del mail ("Tu mail todavía no está confirmado" + botón "Reenviar mail de confirmación" con su mensaje de límite); botón "Más tarde" que lleva a `from` o `/` (se puede saltear y navegar). Al guardar → `PUT /usuarios/me` → `refrescarUsuario()` → `from`. Un `BannerCuentaIncompleta` discreto en la app (opcional, Claude's discretion) recuerda el pendiente.
- `PerfilPage` (`/perfil`): datos editables (nombre, apellido, teléfono), DNI y mail de solo lectura, cambio de contraseña (para cuentas con contraseña) o aviso + botón "Enviarme un mail para crear una contraseña" (cuentas solo-Google), estado de la cuenta ("Verificada" / qué falta).
- Textos y leyendas: en registro y completar-datos, mostrar el aviso de privacidad con link a `/privacidad` (§Ley 25.326). Los textos de `LoginPage`/`RegistroPage` hoy dicen "aceptás nuestros Términos y Condiciones y nuestra Política de Privacidad" sin enlace ni página.

### Anti-Patterns to Avoid
- **Poner `cuentaVerificada` en el JWT** como fuente de verdad: queda vieja hasta 24 h.
- **Unión automática con Google sin tocar la contraseña de una cuenta con mail sin confirmar** (pre-hijacking).
- **`GET` que consume el token** de confirmación/reset (los escáneres de mail lo gastan).
- **Armar el link con el `Host`/`Origin` de la request**.
- **Loguear el link de reset en producción** o loguear DNI/mail/teléfono (`Lombok @Data toString`, `handleDataIntegrity`).
- **Usar el mail como identificador de Google** en lugar de `sub`.
- **Un `GET /api/auth/me`** (cae en `permitAll` de `/api/auth/**`): usar `/api/usuarios/me`.
- **Validar el gate solo en el front.**
- **Guardar DNI/teléfono en `localStorage`.**
- **Bloquear la recuperación de contraseña con el contador de login.**

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Verificar firma/`exp`/`iss` del ID token de Google y rotación de claves | Parser JWT + descarga y caché del JWKS | `NimbusJwtDecoder.withJwkSetUri(...)` + `JwtTimestampValidator` + validadores de `iss`/`aud` | Caché de 5 min, refresco ante `kid` desconocido, algoritmos acotados a RS256, tolerancia de reloj de 60 s ya resueltos |
| Normalizar celulares argentinos (0, 15, +54, 9, código de área de 2-4 dígitos) | Regex propia | libphonenumber 9.0.40 | El `15` se ubica según el código de área (metadata); la conversión a `+549` es regla del plan de numeración |
| Hash/aleatoriedad de tokens | Un esquema propio de firma/cifrado | `SecureRandom` + `MessageDigest("SHA-256")` del JDK | 256 bits de entropía; nada de crypto casera |
| Hash de contraseñas | — | `BCryptPasswordEncoder` ya configurado | Ya existe; solo respetar el límite de 72 bytes |
| Contadores con vencimiento y tope de memoria | `ConcurrentHashMap` + hilo de limpieza | Caffeine `expireAfterWrite` + `maximumSize` | Sin fugas por claves de IP |
| Cliente HTTP de Brevo | SDK de Brevo / SMTP manual | `RestClient` (spring-web) con timeouts | Un solo endpoint; evita dependencia y puerto 587 bloqueado en Railway |
| Botón de Google y su estilo | Botón propio simulando el de Google | `renderButton` de GIS | Único camino que devuelve ID token directo y respeta la marca |
| Unicidad de DNI / `google_sub` / mail | Chequeos solo en código | `UNIQUE` + índice `lower(email)` en V4 (más el pre-chequeo para el mensaje) | Las carreras solo las resuelve la base |

**Key insight:** casi todo lo "de seguridad" de esta fase tiene una pieza estándar (decoder JWT, SecureRandom+SHA-256, BCrypt, UNIQUE, libphonenumber, botón de GIS). El código propio es la **política**: qué se une con qué (D-06), qué falta para estar verificada (D-01), qué se responde y qué no se revela (D-04, D-14).

## Runtime State Inventory

> No es una fase de renombrado, pero cambia el modelo de `usuarios`, que ya tiene filas y sesiones vivas. Se documenta el estado en tiempo de ejecución afectado.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | `usuarios` de producción (y de la demo): sin `apellido`/`dni`, `telefono` opcional y de formato libre, mails posiblemente con mayúsculas, `password_hash` NOT NULL. Las filas de `consultas` guardan nombre/mail/teléfono sueltos | **Migración de esquema** V4 (aditiva, sin mutar filas). **No** hay migración de datos: D-09/D-10 se resuelven con la regla de "verificada" (`dni NULL` y `email_confirmado=false` → "Completá tus datos"). El teléfono viejo se re-normaliza cuando el usuario lo vuelve a cargar |
| Live service config | Google Cloud (Client ID, orígenes), Brevo (cuenta, remitente validado, API key, IPs autorizadas), variables de Railway y Vercel — nada de esto vive en git | Checkpoints humanos H1-H4 (§Runbook) |
| OS-registered state | Ninguno | None — verificado: la fase no registra tareas del SO ni servicios |
| Secrets/env vars | Nuevas: `GOOGLE_CLIENT_ID` (back), `VITE_GOOGLE_CLIENT_ID` (front, build), `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL`, `MAIL_REMITENTE_NOMBRE`, `APP_FRONTEND_URL`. Sin cambios: `APP_JWT_SECRET` (rotarlo invalida todas las sesiones) | Sumarlas a `SecretosGuard`, README y runbook de Railway/Vercel **antes** del deploy |
| Build artifacts / installed packages | `localStorage` de los navegadores con `{nombre,email,rol}` y un JWT 24 h válido (el `sub` sigue siendo el mail, así que las sesiones viejas siguen siendo válidas tras el deploy) | Rehidratar desde `/usuarios/me` al montar el `AuthProvider`; no asumir que `faltantes` existe |

## Common Pitfalls

### Pitfall 1: Las cuentas solo-Google quedan sin autenticar en silencio
**What goes wrong:** el usuario entra con Google, el token se emite, pero toda llamada autenticada da 401.
**Why it happens:** `CustomUserDetailsService` pasa `password_hash = null` a `User.builder().password(...)` → `IllegalArgumentException`; `JwtAuthenticationFilter` la captura y borra el contexto. `[VERIFIED: ejecución local + lectura del filtro]`
**How to avoid:** hash inválido constante (`"!"`) cuando es `null`; test que arma un `Usuario` sin contraseña, emite un token y pega a un endpoint protegido (200, no 401).
**Warning signs:** login con Google "funciona" pero el perfil no carga.

### Pitfall 2: Pre-hijacking en la unión automática (D-06)
**What goes wrong:** el atacante registra `victima@gmail.com` con su clave; cuando la víctima entra con Google se unen y el atacante conserva el acceso.
**How to avoid:** unir solo con `email_verified=true` y, si la cuenta local estaba sin confirmar, descartar su contraseña y fijar `password_cambiada_en` (§Patrón 2). Test: cuenta local sin confirmar + Google → `passwordHash == null`; cuenta local confirmada + Google → conserva la contraseña.
**Warning signs:** una cuenta con `email_confirmado=false` que además tiene `google_sub` y `password_hash`.

### Pitfall 3: Remitente de Gmail → `@brevosend.com` y entregabilidad
**What goes wrong:** el mail de reset llega a spam o con "vía brevosend.com"; con un remitente cuyo dominio publica DMARC `reject` el mail se rechaza (soft bounce) `[CITED: help.brevo.com FAQs DMARC, vía búsqueda]`.
**How to avoid:** probar con Gmail y Outlook reales antes de dar la fase por cerrada; dejar el camino a dominio propio en el runbook; mostrar en el front "Revisá también la carpeta de spam".
**Warning signs:** `201` de Brevo pero el usuario "no recibió nada".

### Pitfall 4: SMTP bloqueado en Railway
Usar la API REST (HTTPS 443). Si alguien propone `spring-boot-starter-mail` + 587, falla con timeout en planes Free/Trial/Hobby `[CITED: docs.railway.com/networking/outbound-networking]`.

### Pitfall 5: Brevo bloquea las IPs de Railway
401 "unrecognised IP" desde Railway si "Block unknown IP addresses" está activo; el egress de Railway rota por deploy. Desactivarlo en Brevo (Security → Authorised IPs). Un 401 de Brevo debe loguearse con un mensaje que lo mencione.

### Pitfall 6: Cuenta de Brevo sin activar para transaccionales / cupo
El primer envío transaccional puede requerir pedir la activación a soporte; el cupo gratis son 300/día. El tope global de la app (250/día) evita pasarse y quedar sin mails de reset.

### Pitfall 7: Google — orígenes, pantalla de consentimiento y COOP
- Los **orígenes JavaScript autorizados** deben incluir esquema + host (+ puerto): `http://localhost` **y** `http://localhost:5173` **y** `http://localhost:5174`, más el dominio de Vercel `[CITED: developers.google.com/identity/gsi/web/guides/get-google-api-clientid: "For local tests or development add both http://localhost and http://localhost:<port_number>"]`. No admite comodines: los *preview deployments* de Vercel (otra URL) no van a poder iniciar sesión con Google (esperado; mismo criterio que CORS en Fase 2). Los cambios pueden tardar en propagarse `[ASSUMED: A8]`.
- La pantalla de consentimiento en estado **Testing** limita a 100 usuarios de prueba: hay que **publicar a producción**; con solo `openid email profile` no requiere verificación de Google y no muestra advertencia `[CITED: support.google.com/cloud/answer/15549945 y developers.google.com/identity/protocols/oauth2/scopes, vía búsqueda]`.
- **COOP:** si se agrega `Cross-Origin-Opener-Policy` al front, tiene que ser `same-origin-allow-popups` (no `same-origin`): "Failing to set the proper header breaks communication between windows, leading to a blank pop-up window" `[CITED: developers.google.com/identity/gsi/web/guides/get-google-api-clientid]`. Vercel no lo agrega por defecto; `vercel.json` hoy solo tiene `rewrites`.
- GIS en `StrictMode`: `initialize` se llama una sola vez; el `renderButton` se repite al re-montar.

### Pitfall 8: `email_verified=false` y Google sin apellido
`email_verified` falso (raro, pero existe) → no unir, mensaje claro. `family_name` puede faltar (nombre único): `apellido` queda `null` y la pantalla "Completá tus datos" lo pide (D-07/D-08: precargado y corregible). Si falta `given_name`, usar `name`. `[ASSUMED: A10 — comportamiento de claims]`

### Pitfall 9: Mayúsculas en el mail
`Usuario.email` es `unique` sensible a mayúsculas (`Usuario.java:25`) y `findByEmail` es exacto: `Ana@x.com` y `ana@x.com` hoy son cuentas distintas y Google devuelve minúsculas. Normalizar a minúsculas en lo nuevo, buscar sin distinguir mayúsculas y el índice único `lower(email)` de V4. **Antes del deploy** correr la consulta de colisiones del runbook (H5): si hay duplicados por mayúsculas V4 falla (y como es transaccional no deja nada a medias).

### Pitfall 10: DNI/PII en logs y respuestas
`handleDataIntegrity` (`GlobalExceptionHandler.java:63`), `@Data` de Lombok en DTOs/entidad, `show-sql`/`format_sql` con parámetros (`show-sql: false` ya está), y los tests que imprimen objetos. No devolver DNI/teléfono en ninguna respuesta pública ni en `PublicacionResponse`; solo en `/usuarios/me` (propio) y, en la Fase 4, en la ficha del admin.

### Pitfall 11: `localStorage` desactualizado y sesiones previas
Las sesiones anteriores siguen válidas (`sub=email`). El `usuario` guardado no tiene `faltantes`: rehidratar con `/me`. Rotar `APP_JWT_SECRET` desloguea a todos (aceptable, ya documentado en Fase 2). Si se implementa `pca`, los tokens sin ese claim se tratan como `0`.

### Pitfall 12: El admin ve "Completá tus datos"
La cuenta admin no tiene apellido ni DNI. `faltantes(ADMIN)` debe ser vacío y `POST /api/consultas` exige `ROLE_COMPRADOR`. Test explícito.

### Pitfall 13: Deploy sin las variables nuevas
`SecretosGuard` ampliado aborta el arranque en `prod` si faltan `BREVO_API_KEY`/`MAIL_REMITENTE_EMAIL`/`APP_FRONTEND_URL`/`GOOGLE_CLIENT_ID`. Railway mantiene el deploy anterior si el healthcheck no responde (Fase 2), pero un reinicio loop es evitable: **cargar las variables antes** de desplegar.

### Pitfall 14: Tests existentes que se rompen
`AuthServiceTest` (registro sin teléfono/DNI; `@InjectMocks` con dependencias nuevas), `ConsultaServiceTest` (ahora exige usuario verificado), `DataSeederTest` (admin con `emailConfirmado`), `SecretosGuardTest` (campos nuevos), `MigracionesPostgresTest` (versión final 4), `SeguridadWebMvcTestBase` (`CustomUserDetailsService`/`bearerPara` con `User` plano), `PublicacionServiceTest`/`CatalogoPostgresTest` (construyen `Usuario`; siguen compilando). Baseline: **325 tests** back verdes y **17** del front `[VERIFIED: mvn -o -Ddante.pg.required=true test → "Tests run: 325, Failures: 0"; node --test "src/**/*.test.js" → pass 17]`.

### Pitfall 15: Token consumido por prefetch / doble submit
`POST` desde la página; botón deshabilitado mientras envía; mensaje de éxito idempotente para confirmar mail ya confirmado.

### Pitfall 16: Spoofing de `X-Forwarded-For`
No confiar en la IP para nada crítico; el límite por mail/usuario es la defensa real (§Patrón 3).

## Code Examples

### Verificador del ID token de Google (probado en JVM 17 con Spring Security 6.3.4)
```java
// Source: probe ejecutada en esta sesion (scratchpad/gprobe, spring-security-oauth2-jose 6.3.4) +
// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
@Component
public class GoogleIdTokenVerifier {

    private static final String JWKS_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private final JwtDecoder decoder;

    public GoogleIdTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWKS_GOOGLE).build();   // RS256 por defecto, cache 5 min
        OAuth2TokenValidator<Jwt> emisor = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return ("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "iss", null));
        };
        OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "aud", null));
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(60)), emisor, audiencia));
        this.decoder = nimbus;
    }

    /** Devuelve los claims o lanza JwtException (el servicio la traduce a 401 uniforme). */
    public Jwt verificar(String credential) { return decoder.decode(credential); }
}
// En el servicio: exigir Boolean.TRUE.equals(jwt.getClaim("email_verified")) y usar jwt.getSubject() como google_sub.
```
Salida real de la probe (clave RSA propia y, para el JWKS real, un token firmado con clave ajena):
```
valido(iss https)       -> OK sub=1234567890 email_verified=true
valido(iss sin esquema) -> OK
aud ajena               -> JwtValidationException: ... aud
iss ajeno               -> JwtValidationException: ... iss
vencido                 -> JwtValidationException: ... Jwt expired at 2026-10-04T01:30:32Z
basura                  -> BadJwtException: ... Malformed token
JWKS Google + clave ajena -> BadJwtException: Signed JWT rejected: Another algorithm expected, or no matching key(s) found
```
(El último caso confirma que el decoder alcanzó `googleapis.com` desde esta máquina y rechazó por clave, no por red.) Para tests unitarios usar `NimbusJwtDecoder.withPublicKey(...)` con un par RSA generado: el verificador debe recibir el `JwtDecoder` por constructor secundario para poder inyectarlo.

### Brevo por RestClient (probado contra un servidor HTTP local)
```java
// Source: probe B.java (RestClient + JdkClientHttpRequestFactory) y https://developers.brevo.com/reference/sendtransacemail
JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
fabrica.setReadTimeout(Duration.ofSeconds(10));
RestClient brevo = RestClient.builder().baseUrl(urlBase).requestFactory(fabrica)
        .defaultHeader("api-key", apiKey).defaultHeader("accept", "application/json").build();

Map<String, Object> cuerpo = new LinkedHashMap<>();
cuerpo.put("sender", Map.of("name", remitenteNombre, "email", remitenteEmail));
cuerpo.put("to", List.of(Map.of("email", m.paraEmail(), "name", m.paraNombre())));
cuerpo.put("subject", m.asunto());
cuerpo.put("htmlContent", m.html());
cuerpo.put("textContent", m.texto());
try {
    brevo.post().uri("/v3/smtp/email").contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().toBodilessEntity();
} catch (RestClientException e) {
    throw new ServicioExternoException("No pudimos enviar el mail. Intentá de nuevo en unos minutos.");  // sin loguear el cuerpo
}
```
Salida de la probe: el servidor local recibió `api-key=xkeysib-test`, `Content-Type: application/json` y el cuerpo con `sender/to/subject/htmlContent/textContent`; el cliente leyó `{messageId=<abc@smtp-relay.mailin.fr>}`. (Ojo con el encoding de la consola de Windows al imprimir tildes en la probe: el JSON viaja en UTF-8.)

### Normalización de teléfono (probado con libphonenumber 9.0.40)
```java
// Source: probe T.java/T2.java en esta sesion; https://github.com/google/libphonenumber
public static String normalizarCelular(String crudo) {
    PhoneNumberUtil u = PhoneNumberUtil.getInstance();
    try {
        Phonenumber.PhoneNumber p = u.parse(crudo, "AR");
        if (p.getCountryCode() != 54 || !u.isValidNumberForRegion(p, "AR")) throw new ReglaDeNegocioException(MSJ);
        String nsn = u.getNationalSignificantNumber(p);                       // "1112345678" o "91112345678"
        String diezDigitos = (nsn.length() == 11 && nsn.startsWith("9")) ? nsn.substring(1) : nsn;
        if (diezDigitos.length() != 10) throw new ReglaDeNegocioException(MSJ);
        return "+549" + diezDigitos;                                          // +5491112345678
    } catch (NumberParseException e) {
        throw new ReglaDeNegocioException(MSJ);
    }
}
public static String normalizarDni(String crudo) {
    String d = crudo == null ? "" : crudo.replaceAll("[\\s.\\-]", "");
    if (!d.matches("[1-9][0-9]{6,7}")) throw new ReglaDeNegocioException("El DNI debe tener 7 u 8 dígitos, sin puntos.");
    return d;
}
```

### Token de un solo uso (consumo atómico)
```java
// SecureRandom 32 bytes -> token de URL; en base solo el SHA-256 (hex).
String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios(32));
String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(UTF_8)));

// TokenCuentaRepository
@Modifying
@Query("update TokenCuenta t set t.usadoEn = :ahora where t.tokenHash = :hash and t.tipo = :tipo "
     + "and t.usadoEn is null and t.expiraEn > :ahora")
int consumir(@Param("hash") String hash, @Param("tipo") TipoTokenCuenta tipo, @Param("ahora") LocalDateTime ahora);
// 1 => el token era valido y ahora esta usado (solo lo gana una de dos requests simultaneas); 0 => invalido/vencido/usado.
```

### Límite de intentos con Caffeine (probado)
```java
// Ventana fija desde el primer intento; la clave se descarta sola y el mapa tiene tope.
private final Cache<String, AtomicInteger> contadores =
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15)).maximumSize(10_000).build();

public void registrarOFallar(String clave, int maximo) {
    if (contadores.get(clave, k -> new AtomicInteger()).incrementAndGet() > maximo) {
        throw new LimiteDeIntentosException("Demasiados intentos. Esperá unos minutos e intentá de nuevo.");
    }
}
// Probe: 7 intentos con maximo 5 => 2 bloqueados.
```

### Front: reglas puras del gate (`src/utils/cuenta.js`)
```js
export function evaluarAcceso(usuario) {
  if (!usuario) return 'anonimo'
  if (usuario.rol === 'ADMIN') return 'verificada'
  if (!Array.isArray(usuario.faltantes)) return 'desconocida' // sesion guardada antes de esta fase: refrescar /me
  return usuario.faltantes.length === 0 ? 'verificada' : 'incompleta'
}

export function sanitizarDestino(origen) {
  return typeof origen === 'string' && origen.startsWith('/') && !origen.startsWith('//') && !origen.startsWith('/\\')
    ? origen
    : '/'
}

export function destinoDeGate(estado, desde) {
  const from = sanitizarDestino(desde)
  if (estado === 'anonimo') return { ruta: '/login', state: { from } }
  if (estado === 'incompleta') return { ruta: '/completar-datos', state: { from } }
  return { ruta: null, state: null }
}
```
```js
// src/utils/cuenta.test.js  ->  node --test "src/**/*.test.js"  (cwd = danteautomotores-front)
import test from 'node:test'
import assert from 'node:assert/strict'
import { evaluarAcceso, destinoDeGate, sanitizarDestino } from './cuenta.js'
test('un visitante va al login y vuelve a la ficha', () => {
  assert.deepEqual(destinoDeGate(evaluarAcceso(null), '/publicaciones/7?x=1'),
    { ruta: '/login', state: { from: '/publicaciones/7?x=1' } })
})
test('no se vuelve a una URL externa', () => {
  assert.equal(sanitizarDestino('//evil.com'), '/')
  assert.equal(sanitizarDestino('https://evil.com'), '/')
})
```

### Front: botón de Google (sin npm)
```jsx
// src/components/BotonGoogle.jsx  (estilo del repo: sin punto y coma)
import { useEffect, useRef } from 'react'

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
let inicializado = false

function cargarScript() {
  return new Promise((resolve) => {
    if (window.google?.accounts?.id) return resolve()
    const s = document.createElement('script')
    s.src = 'https://accounts.google.com/gsi/client'
    s.async = true
    s.defer = true
    s.onload = () => resolve()
    document.head.appendChild(s)
  })
}

export default function BotonGoogle({ onCredential }) {
  const ref = useRef(null)
  useEffect(() => {
    if (!CLIENT_ID) return undefined
    let vigente = true
    cargarScript().then(() => {
      if (!vigente || !ref.current) return
      if (!inicializado) {
        window.google.accounts.id.initialize({ client_id: CLIENT_ID, callback: (r) => window.__dnteGoogleCb?.(r.credential) })
        inicializado = true
      }
      window.__dnteGoogleCb = onCredential
      window.google.accounts.id.renderButton(ref.current, {
        type: 'standard', theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular',
        logo_alignment: 'center', width: Math.min(400, ref.current.offsetWidth || 400),
      })
    })
    return () => { vigente = false }
  }, [onCredential])
  if (!CLIENT_ID) return null
  return <div ref={ref} className="flex w-full justify-center" />
}
```
(Esqueleto ilustrativo: el planner decide si el callback global se resuelve con un `ref` o un contexto; lo importante es que `initialize` corra una sola vez y que el callback apunte al `onCredential` vigente.)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Google Sign-In JS platform library (`gapi.auth2`) | Google Identity Services (`accounts.google.com/gsi/client`) con ID token | `gapi.auth2` retirado | Usar GIS; los docs de `sign-in/web/backend-auth` son del flujo viejo `[CITED: resultados de búsqueda]` |
| `GoogleIdTokenVerifier` (google-api-client) como única vía | Cualquier librería JWT con el JWKS de Google | vigente en docs actuales | Nimbus/Spring cumple y no suma dependencias |
| SMTP desde la app | API HTTPS del proveedor | Railway deshabilita SMTP en planes bajos | `RestClient` + Brevo `/v3/smtp/email` |
| Remitente con casilla de Gmail | Dominio propio autenticado (SPF/DKIM/DMARC) | Requisitos de Gmail/Yahoo desde feb 2024 | D-13 arranca sin dominio; deuda documentada |
| `node --test <directorio>` | `node --test "<glob>"` | Node 24 | La convención de Fase 2 (`node --test src/utils/`) hoy falla (verificado): actualizar VALIDATION |

**Deprecated/outdated:**
- El botón "Continuar con Google" decorativo y los `// TODO: sacar este aviso cuando el login con Google esté conectado` de `LoginPage.jsx`/`RegistroPage.jsx` (`avisarGoogle`, `avisoGoogle`).
- `ConsultaRequest` con nombre/mail/teléfono sueltos (D-11).
- `armarLinkWhatsapp` en el `Navbar` "Vender tu auto" y en `SeccionFinanciamiento` quedan detrás del gate hasta las Fases 4/5.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | El estado "verificada" va por `/api/usuarios/me` + 403 evaluado en el back en cada acción, no en claims del JWT | Patrón 6 | Bajo: D-01/CONTEXT lo dejan a criterio; cambiar a claims exigiría invalidar tokens al confirmar el mail |
| A2 | Un `POST` desde la página (no un `GET` con efecto) evita que escáneres de mail gasten el token | Patrón 3 | Bajo; es práctica común, no verificada con un escáner real |
| A3 | Las cuentas existentes con mail sin confirmar y contraseña pierden la contraseña si entran por Google (anti pre-hijacking) | Patrón 2, Open Q 1 | Medio: un usuario legacy legítimo que entra con Google deberá definir una contraseña nueva por mail; no pierde datos ni favoritos. El usuario debe confirmar la decisión |
| A4 | Umbrales de límite de intentos (10/30/10/3/…, tope global 250 mails/día) | Patrón 3 | Bajo: son configurables; ajustar tras uso real |
| A5 | La IP del cliente tras el proxy de Railway = primer valor de `X-Forwarded-For` (fuentes contradictorias) | Patrón 3, Pitfall 16 | Bajo: los límites por mail/usuario son la defensa real |
| A6 | Una cuenta ADMIN no se puede alcanzar por la unión automática con Google | Patrón 2 | Bajo: refuerza "ningún camino crea/promueve admins"; si el dueño quiere usar Google para el admin, se levanta la restricción |
| A7 | Ningún DNI argentino de 7-8 dígitos empieza en 0 (regex `^[1-9]\d{6,7}$` y `CHECK` en V4) | Patrón 4 | Bajo-medio: un DNI real que empiece en 0 sería rechazado; relajar a `\d{7,8}` si aparece |
| A8 | Los cambios en "Authorized JavaScript origins" pueden tardar minutos en aplicar | Pitfall 7 | Bajo |
| A9 | Boot 3.3.5 no trae aún las propiedades `spring.http.client.*`; los timeouts se fijan en la fábrica | Patrón 7 | Bajo: se prueba al implementar |
| A10 | Google puede omitir `family_name` y, raramente, devolver `email_verified=false` | Pitfall 8 | Bajo |
| A11 | Brevo reemplaza el dominio de remitentes de dominios gratuitos por `@brevosend.com` y la página oficial no se pudo abrir (403); la afirmación viene de resultados de búsqueda de `help.brevo.com` | Patrón 7, Pitfall 3 | Medio: si el comportamiento cambió, el remitente podría ser rechazado; el humo con casillas reales (H2) lo confirma |
| A12 | El plan gratis de Brevo exige activar el envío transaccional la primera vez (de búsqueda) | Pitfall 6 | Bajo: se descubre en el primer envío |
| A13 | Inscripción en el Registro Nacional de Bases de Datos de la AAIP: la reglamentación incluye en "bases destinadas a dar informes" las que exceden el uso exclusivamente personal; es probable que aplique a la base de usuarios, pero no es asesoramiento legal | Ley 25.326 | Medio (cumplimiento): consultar con un abogado/contador; no bloquea el trabajo técnico |
| A14 | Libphonenumber con `isValidNumberForRegion` no rechaza celulares reales (probado con 21 números realistas, pero la metadata puede quedar corta para rangos nuevos) | Patrón 4 | Bajo-medio: un celular real rechazado impide registrarse; mitigar con mensaje claro y, si pasa, caer a la validación estructural de 10 dígitos |
| A15 | "Reservar o agendar visita" de la ficha es el botón que la Fase 4 llama "Lo quiero" | Patrón 8, Open Q 2 | Bajo: solo cambia la etiqueta |

## Open Questions

1. **¿Confirmás que la unión con Google borra la contraseña de una cuenta con mail sin confirmar?** (A3)
   - Sabemos: D-06 une automáticamente solo con `email_verified`; D-10 deja los mails de las cuentas viejas sin confirmar; así, todo usuario legacy que entre por Google pasará por esa regla.
   - Falta: aceptar que esos usuarios deban crear contraseña de nuevo (por "olvidé mi contraseña") si quieren seguir usando mail+contraseña.
   - Recomendación: **sí**, es el costo mínimo de cerrar el pre-hijacking. Alternativa más laxa (no recomendada): conservar la contraseña y dejar la cuenta como "no confirmada".
2. **¿"Lo quiero" es el botón "Reservar o agendar visita" de la ficha?** En el front no existe un botón con ese texto ni "Mis mensajes". Recomendación: renombrar el botón de la ficha a "Lo quiero" (la acción real llega en Fase 4) y crear la entrada "Mis mensajes" en el `Navbar` con una página "Próximamente" para poder cumplir el criterio de éxito 5 hoy.
3. **Mensaje de registro con mail duplicado.** Hoy dice "Ya existe una cuenta con ese email" (revela existencia). OWASP recomienda un mensaje genérico con mail de aviso. Recomendación: mantener (UX, ya cubierto por `AuthServiceTest`) con límite por IP; anotarlo como deuda de seguridad.
4. **Remitente de Brevo (D-13):** ¿qué casilla usará la agencia (¿un Gmail propio?). Con Gmail el mail sale desde `@brevosend.com`. Decidir si se compra un dominio barato ya (autenticar con DKIM sube bastante la entregabilidad) o se arranca así y se mide. No bloquea el desarrollo.
5. **Invalidación de sesiones al cambiar/restablecer contraseña (claim `pca`).** Recomendado por OWASP; agrega un cambio en `JwtService`, `CustomUserDetailsService` y el filtro. Recomendación: incluirlo (costo bajo, ~1 plan chico); si el planner quiere acotar, dejarlo como mejora.
6. **Registro Nacional de Bases de Datos (AAIP) y texto legal de privacidad.** Fuera del código: el titular de la agencia debería revisar con un profesional el texto y la inscripción (A13).
7. **Auto-enviar el mail de confirmación a las cuentas legacy.** Recomendación: no mandar nada masivo; el mail se envía cuando el usuario toca "Reenviar mail de confirmación" en `/completar-datos` (evita gastar el cupo de 300/día).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | build/tests del back | ⚠ solo 17 | 17.0.12 (`/c/Program Files/Java/jdk-17`) | `-Djava.version=17` (no tocar el `pom.xml`) |
| Maven | build/tests | ✓ (no está en PATH) | 3.9.16 en `/c/Users/toto/.maven/maven-3.9.16/bin` | `export PATH=...` |
| Postgres local (docker-compose, 5433) | tests con Postgres, V4 | ✓ | 16.15 (`danteautomotores-db`, Up) | `docker compose up -d` |
| Node / npm | build del front y `node --test` | ✓ | Node v24.14.1 | — |
| Dependencias Maven nuevas en `~/.m2` | compilar offline | ✓ (cacheadas en esta sesión) | oauth2-jose 6.3.4, nimbus 9.37.3, caffeine 3.1.8, libphonenumber 9.0.40 | resolución online una vez |
| Salida a internet desde esta máquina (JWKS de Google, Maven Central) | probes | ✓ | — | — |
| Cuenta de Google Cloud + Client ID Web | AUTH-02 en cualquier entorno real | ✗ (la crea el usuario) | — | **Checkpoint humano H1**; sin Client ID el botón no se muestra y el back lo exige solo fuera de dev |
| Cuenta de Brevo + API key + remitente validado | PROD-03 / AUTH-04 | ✗ (la crea el usuario) | — | **Checkpoint humano H2**; en dev/test funciona `LogEmailSender` |
| Variables en Railway / Vercel | deploy | ✗ (paneles del usuario) | — | **Checkpoint humano H3/H4**; no hay Railway CLI ni Vercel CLI |
| `psql` en el host | consultas manuales | ✗ | — | `docker exec danteautomotores-db psql -U dante ...` |

**Missing dependencies with no fallback:**
- Cuenta de Google Cloud, cuenta de Brevo y acceso a los paneles de Railway/Vercel: los crea o los opera el usuario (checkpoints humanos). Todo el desarrollo y los tests corren sin ellas.

**Missing dependencies with fallback:**
- JDK 21 (usar `-Djava.version=17`), `psql` del host (usar Docker).

## Validation Architecture

> `workflow.nyquist_validation: true` en `.planning/config.json` (verificado: `"nyquist_validation": true`).

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito + MockMvc/`spring-security-test` (Boot 3.3.5); tests con Postgres reales por `PostgresLocalTestBase` (base descartable `test_<hex>` en `localhost:5433`); front: `node:test` (built-in) + `npm run build` |
| Config file | ninguno (`src/test/resources` no existe); los tests Postgres fijan propiedades por `@DynamicPropertySource` |
| Quick run command | `mvn -B -o -Djava.version=17 test -Dtest=<Clase>` (con `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"`; si hay error de `class file version`, `rm -rf target/classes target/test-classes`) |
| Full suite command | `mvn -B -o -Djava.version=17 -Ddante.pg.required=true test` **+** `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` **+** `cd C:/Users/toto/Desktop/work/danteautomotores-front && node --test "src/**/*.test.js"` |
| Baseline | **325 tests** back verdes (`Tests run: 325, Failures: 0, Errors: 0`) y **17** front (`pass 17`), medidos hoy `[VERIFIED]`. Nota: `node --test src/utils/` (con directorio) falla en Node 24; usar el glob |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Registro exige apellido, teléfono y DNI; formatos; password 8–72; `@ToString` sin PII | unit (validación) | `mvn … test -Dtest=RegistroRequestValidationTest` | ❌ Wave 0 |
| AUTH-01 | Registro crea COMPRADOR, normaliza mail/teléfono/DNI, DNI duplicado → 400 "Ese DNI ya está registrado…", mail duplicado, encola mail de confirmación; rol del body ignorado | unit (Mockito) | `mvn … test -Dtest=AuthServiceTest` | ✅ existe, ampliar |
| AUTH-01 / D-04 | `UNIQUE(dni)` con NULL permitidos; `CHECK` de formato; índice `lower(email)`; filas legacy intactas; versión final 4 | integración PG | `mvn … -Ddante.pg.required=true test -Dtest=MigracionesPostgresTest,UsuarioPostgresTest` | ✅ ampliar / ❌ Wave 0 |
| AUTH-01 / D-03,D-05 | Normalizador: tabla de teléfonos (+549…), rechazo de no-argentinos / sin área / incompletos; DNI 7-8 dígitos | unit | `mvn … test -Dtest=NormalizadorDeContactoTest` | ❌ Wave 0 |
| AUTH-02 | Verificador: acepta ambos `iss`, rechaza `aud` ajena, vencido, malformado, firma ajena (par RSA generado) | unit | `mvn … test -Dtest=GoogleIdTokenVerifierTest` | ❌ Wave 0 |
| AUTH-02 / D-06 | Flujo Google: por `sub`; alta nueva (sin apellido); unión por mail con `email_verified`; `email_verified=false` rechazado; cuenta sin confirmar pierde contraseña; confirmada la conserva; ADMIN rechazado | unit | `mvn … test -Dtest=GoogleAuthServiceTest` | ❌ Wave 0 |
| AUTH-02 | Cuenta sin contraseña autentica con el JWT (no 401) y no puede entrar por `/login` | slice + unit | `mvn … test -Dtest=CuentaSoloGoogleTest` | ❌ Wave 0 |
| AUTH-03 / D-01 | `faltantes`: apellido, teléfono, DNI, mail; ADMIN vacío; teléfono viejo inválido cuenta como faltante | unit | `mvn … test -Dtest=VerificacionCuentaTest` | ❌ Wave 0 |
| AUTH-03 / D-11 | `POST /api/consultas`: sin token 401, admin 403, comprador incompleto 403 `{codigo:CUENTA_NO_VERIFICADA,faltantes}`, verificado 200 con datos de la cuenta; body viejo con nombre/mail ignorado | slice (WebMvc) + unit | `mvn … test -Dtest=ConsultaSeguridadTest,ConsultaServiceTest` | ❌ Wave 0 / ✅ ampliar |
| AUTH-03 | `PUT /api/usuarios/me` completa datos; DNI inmutable una vez cargado; DNI duplicado 400; mail no editable | slice + PG | `mvn … test -Dtest=UsuarioControllerTest,UsuarioPostgresTest` | ❌ Wave 0 |
| AUTH-04 / D-02,D-14 | Token: expira (24 h / 1 h con `Clock` fijo), un solo uso incluso concurrente, emitir invalida el anterior, hash en base (no el token) | integración PG + unit | `mvn … -Ddante.pg.required=true test -Dtest=TokenCuentaServiceTest` | ❌ Wave 0 |
| AUTH-04 | Olvidé mi contraseña: misma respuesta y sin mail para mail inexistente; reset exitoso confirma el mail, borra tokens, notifica y **no** devuelve JWT; límites | unit + slice | `mvn … test -Dtest=RecuperacionContrasenaTest` | ❌ Wave 0 |
| AUTH-04 | Límite: ventana, tope, `Clock`; recuperación no bloqueada por el contador de login | unit | `mvn … test -Dtest=LimitadorDeIntentosTest` | ❌ Wave 0 |
| AUTH-05 | `GET /me` solo propio (sin DNI en otras respuestas), cambio de contraseña exige la actual, cuenta Google sin contraseña → mensaje | slice | `mvn … test -Dtest=UsuarioControllerTest` | ❌ Wave 0 |
| PROD-03 | `BrevoEmailSender`: cuerpo/headers correctos contra servidor HTTP local; 4xx/5xx/timeout → `ServicioExternoException` sin loguear el link | unit | `mvn … test -Dtest=BrevoEmailSenderTest` | ❌ Wave 0 |
| PROD-03 | `SecretosGuard`: prod sin `BREVO_API_KEY`/remitente/`APP_FRONTEND_URL`/`GOOGLE_CLIENT_ID` no arranca; dev solo avisa; `LogEmailSender` jamás en prod | unit | `mvn … test -Dtest=SecretosGuardTest,MailConfigTest` | ✅ ampliar / ❌ Wave 0 |
| PROD-03 | Mail real recibido en Gmail y Outlook (no spam) y link funcional | manual (UAT, `human_verify_mode: end-of-phase`) | ver Runbook H2/H6 | — |
| AUTH-06 | `evaluarAcceso`, `destinoDeGate`, `sanitizarDestino`, `destinoPostLogin`, `normalizarDni` | unit `node:test` | `cd danteautomotores-front && node --test "src/**/*.test.js"` | ❌ Wave 0 |
| AUTH-06 | Visitante toca "Lo quiero"/"Cotizá tu usado"/"Mis mensajes" → login → vuelve a la ficha (también con Google) | manual (UAT en `localhost:5174`) | ver Pitfall de front local de Fase 2 | — |
| Front (todas) | El build compila con las páginas y rutas nuevas; sin `USE_MOCK_DATA` ni `TODO` de Google | estático + build | `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` y `! grep -rn "avisarGoogle" C:/Users/toto/Desktop/work/danteautomotores-front/src` | ✅ (comando) |
| Seguridad | Sin DNI/teléfono en logs: `handleDataIntegrity` no vuelca el mensaje de Postgres | unit | `mvn … test -Dtest=GlobalExceptionHandlerTest` | ✅ ampliar |

### Sampling Rate
- **Per task commit:** `mvn -B -o -Djava.version=17 test -Dtest=<clase de la tarea>` (+ `node --test "src/**/*.test.js"` y `npm run build` si toca el front).
- **Per wave merge:** suite completa del back con `-Ddante.pg.required=true` + build del front + `node --test`.
- **Phase gate:** todo verde (≥ 325 back + los nuevos, ≥ 17 front) + UAT manual en el front local (`localhost:5174` contra el back local con `APP_CORS_ALLOWED_ORIGINS` ampliado) + checkpoints H1-H6 hechos antes de dar PROD-03 por cerrado.

### Wave 0 Gaps
- [ ] `RegistroRequestValidationTest`, `NormalizadorDeContactoTest`, `VerificacionCuentaTest`, `LimitadorDeIntentosTest`, `GoogleIdTokenVerifierTest`, `GoogleAuthServiceTest`, `BrevoEmailSenderTest`, `MailConfigTest` — unit, corren offline.
- [ ] `UsuarioPostgresTest`, `TokenCuentaServiceTest` — extienden `PostgresLocalTestBase` (base descartable).
- [ ] `UsuarioControllerTest`, `ConsultaSeguridadTest`, `CuentaSoloGoogleTest` — `@WebMvcTest` sobre `SeguridadWebMvcTestBase` (agregar `@MockBean` de los servicios nuevos; ajustar `bearerPara` si se implementa `pca`).
- [ ] Un `EmailSender` de captura para tests de integración (`EmailSenderEnMemoria`) y/o `@MockBean EmailSender`.
- [ ] Ampliar `MigracionesPostgresTest`, `AuthServiceTest`, `ConsultaServiceTest`, `DataSeederTest`, `SecretosGuardTest`, `GlobalExceptionHandlerTest`.
- [ ] Front: `src/utils/cuenta.test.js`.
- [ ] Dependencias nuevas en `pom.xml` (+ `libphonenumber.version`) y, en otra máquina, resolución online.
- [ ] Script de humo opcional `scripts/verify/cuentas-humo.js` (registro → confirmar vía `LogEmailSender` → login → `/me` → 403 en consulta → completar datos → 200) contra el back local (nunca contra producción), en el estilo de `scripts/verify/catalogo-humo.js`.

## Security Domain

> `security_enforcement` habilitado (ASVS nivel 1, `security_block_on: high`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | sí | BCrypt ya existente; contraseña 8–72 (OWASP: permitir ≥ 64); verificación del ID token de Google (firma, `iss`, `aud`, `exp`, `email_verified`); respuesta uniforme en login/recuperación; límite de intentos por cuenta e IP; recuperación por token CSPRNG hasheado, un solo uso, 1 h; no loguear al usuario tras el reset |
| V3 Session Management | sí | JWT stateless 24 h (sin cambios); invalidación opcional por `pca` al cambiar contraseña; el estado verificada NO viaja en el token |
| V4 Access Control | sí | `POST /api/consultas` solo `ROLE_COMPRADOR`; gate de cuenta verificada en el servicio (403 con `codigo`); `/api/usuarios/me` solo propio (sin parámetro de id: no hay IDOR); DNI/teléfono nunca en endpoints públicos |
| V5 Input Validation | sí | `@Valid` + `NormalizadorDeContacto` (libphonenumber, regex de DNI), `@Size` en todos los textos, token con formato fijo; `from` del front restringido a rutas internas |
| V6 Cryptography | sí | `SecureRandom` + SHA-256 del JDK para tokens; nada de crypto propia; secretos solo por variables de entorno |
| V7 Error Handling & Logging | sí | Formato uniforme `{error[, campos|codigo, faltantes]}`; sin PII en logs (`handleDataIntegrity`, Lombok `toString`); no loguear tokens ni links de reset en prod; 429 con `Retry-After` |
| V8 Data Protection | sí | Ley 25.326: finalidad, consentimiento, acceso/rectificación/supresión, deber de seguridad; no guardar PII en `localStorage`; dumps de producción fuera del repo (ya en `.gitignore`) |
| V13 API | sí | Límites de intentos; `POST` para operaciones con efecto; CORS exacto (ya) |
| V14 Configuration | sí | `SecretosGuard` exige Brevo/Google/`APP_FRONTEND_URL` fuera de dev; `APP_FRONTEND_URL` fija (no `Host`); `Referrer-Policy` en `vercel.json` |

### Known Threat Patterns for Spring Boot + SPA + Google/Brevo

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Pre-hijacking de cuenta (mail ajeno registrado antes) | Spoofing / Elevation | Unión por Google solo con `email_verified`; descartar la contraseña de cuentas sin confirmar; el reset confirma el mail |
| ID token de otra app / otro emisor / vencido | Spoofing | Validadores `aud`, `iss`, `exp`; `sub` como identificador |
| Enumeración de cuentas (login, reset, registro) | Information Disclosure | Respuesta y tiempo uniformes (envío asíncrono); mensaje genérico en login/reset; DNI duplicado sin revelar dueño (D-04); mail duplicado: compromiso documentado |
| Fuerza bruta / credential stuffing | Spoofing | Límite por mail e IP; BCrypt; 429; recuperación siempre disponible |
| Fuerza bruta del token de reset/confirmación | Tampering | 256 bits, hash en base, un solo uso, vencimiento, límite |
| Host-header injection en links de reset | Tampering | `APP_FRONTEND_URL` configurada |
| Token consumido por escáner de mails | Denial of Service | `POST` desde la página, no `GET` |
| Mail-bombing / agotar el cupo de Brevo | DoS | Límites por mail/IP + tope global diario; sin mail si cuenta inexistente |
| Open redirect vía `state.from` | Tampering | `sanitizarDestino` (solo rutas internas) |
| Fuga de DNI/teléfono por logs o respuestas | Information Disclosure | Logs sin PII; DTOs sin esos campos fuera de `/me`; `@ToString.Exclude` |
| Sesión viva tras cambio de contraseña | Elevation | Claim `pca` (opcional) |
| XSS que roba el JWT de `localStorage` | Information Disclosure | React escapa por defecto (sin `dangerouslySetInnerHTML`, verificado en Fase 2); deuda conocida del proyecto (anti-patrón ya listado en CLAUDE.md) |
| Cuenta solo-Google "bloqueada" por `password_hash NULL` | Availability | Hash inválido constante; test de integración |
| COOP mal configurado rompe el popup de Google | Availability | `same-origin-allow-popups` si se agrega COOP |

## Runbook: checkpoints humanos

> Todo esto requiere cuentas y paneles del usuario. El ejecutor **no** pide ni escribe secretos en archivos del repo. Las verificaciones que tocan producción son de solo lectura salvo la prueba de mail de H6, que la hace el usuario con su propia casilla. El `.env` del front está protegido en esta máquina: pedirle el valor al usuario, no leerlo.

**H1 — Google Cloud (Client ID).**
1. console.cloud.google.com → proyecto (nuevo "danteautomotores" o existente) → **Google Auth Platform**.
2. **Branding:** nombre "Dante Automotores", mail de soporte, mail del desarrollador.
3. **Audience:** tipo **External**; **Publish app** (pasar de *Testing* a *In production*). Con solo `openid`, `email`, `profile` no requiere verificación ni muestra advertencia; en *Testing* solo entran hasta 100 usuarios de prueba.
4. **Clients → Create client → Web application** (`danteautomotores-web`):
   - *Authorized JavaScript origins:* `http://localhost`, `http://localhost:5173`, `http://localhost:5174` y el dominio de producción del front (`https://<proyecto>.vercel.app` o el propio). Sin barra final ni path; sin comodines.
   - *Authorized redirect URIs:* **vacío** (no se usa redirect).
5. Copiar el **Client ID** (`…apps.googleusercontent.com`). **No** hace falta ni se debe guardar el *Client secret* en ningún lado.
6. Cargarlo como `GOOGLE_CLIENT_ID` (Railway y, para correr el back local, entorno del shell) y como `VITE_GOOGLE_CLIENT_ID` (Vercel *Production*, y `.env` del front local, que está en `.gitignore`). `VITE_*` se inlinea al **compilar**: hay que *redeployar* el front después de cambiarla.
7. Verificación: abrir `/login` en `localhost:5174` → aparece el botón de Google; elegir una cuenta → vuelve con sesión. Si el popup queda en blanco: revisar COOP y que el origen exacto esté autorizado.

**H2 — Brevo (cuenta, remitente, API key, IPs).**
1. Crear cuenta en brevo.com (plan **Free**, 300 mails/día), verificar el mail y completar el perfil. Si el envío transaccional no está activo, pedir la activación a soporte desde el panel (primer envío).
2. *Settings → Senders, Domains & Dedicated IPs → Senders → Add a sender*: nombre (p. ej. "Dante Automotores") y la casilla de la agencia; validar con el código de 6 dígitos que llega a esa casilla. (Una casilla de Gmail/Yahoo no se puede autenticar: Brevo enviará desde `@brevosend.com`. Es lo que permite D-13.)
3. *SMTP & API → API Keys → Generate a new API key* (`danteautomotores-railway`). Copiarla **una sola vez**. No es la "SMTP key".
4. *Security → Authorised IPs*: **desactivar "Block unknown IP addresses"** (Railway rota las IPs de salida). Verificar que el listado no bloquea.
5. Variables (H3): `BREVO_API_KEY`, `MAIL_REMITENTE_EMAIL` (la casilla validada), `MAIL_REMITENTE_NOMBRE`.
6. **Prueba de entregabilidad** (después de H3/H5): pedir "Olvidé mi contraseña" con casillas propias de **Gmail** y **Outlook** y anotar: ¿llegó?, ¿bandeja o spam?, ¿"vía brevosend.com"?, ¿el link funciona?, ¿vence/un solo uso?
7. **Camino a dominio propio (D-13, más adelante):** comprar un dominio → Brevo → *Domains → Add a domain* → crear en el DNS el registro **Brevo code (TXT)**, el **DKIM** y un **DMARC** (TXT `_dmarc`) que Brevo indica → *Authenticate* → cambiar `MAIL_REMITENTE_EMAIL` a `no-responder@<dominio>` (sin tocar código).

**H3 — Variables en Railway (servicio del back → Variables)**, nombres exactos (cargar **antes** de desplegar el código nuevo):
| Variable | Valor / nota |
|----------|--------------|
| `GOOGLE_CLIENT_ID` | del paso H1 |
| `BREVO_API_KEY` | del paso H2 |
| `MAIL_REMITENTE_EMAIL` / `MAIL_REMITENTE_NOMBRE` | la casilla validada en Brevo / "Dante Automotores" |
| `APP_FRONTEND_URL` | origen exacto del front (`https://<proyecto>.vercel.app`), sin `/` final; **debe coincidir** con uno de `APP_CORS_ALLOWED_ORIGINS` |
| `MAIL_RESPONDER_A` | opcional |
| (sin cambios) `SPRING_PROFILES_ACTIVE=prod`, `APP_JWT_SECRET`, `SPRING_DATASOURCE_*`, `CLOUDINARY_*`, `ADMIN_*`, `APP_CORS_ALLOWED_ORIGINS` | ya cargadas en Fase 2 |

**H4 — Vercel (front):** `VITE_GOOGLE_CLIENT_ID` en *Production*; agregar a `vercel.json` los headers `Referrer-Policy: strict-origin-when-cross-origin` (no agregar COOP, o que sea `same-origin-allow-popups`); *redeploy* del front.

**H5 — Antes de desplegar V4 (solo lectura sobre producción):** en Railway → Postgres → *Query*:
```sql
-- Debe devolver 0 filas; si devuelve filas, hay mails que difieren solo en mayusculas y V4 fallaria.
SELECT lower(email) AS email_normalizado, count(*) FROM usuarios GROUP BY lower(email) HAVING count(*) > 1;
-- Contexto: cuantos compradores quedaran para "Completa tus datos".
SELECT rol, count(*) FROM usuarios GROUP BY rol;
```
Más **backup** previo (mismo procedimiento H2 de Fase 2: snapshot de Railway + `pg_dump` fuera del repo; el dump trae hashes y datos personales). V4 es transaccional (Postgres) y aditiva: el rollback normal es *Railway → Deployments → deploy anterior*; la versión vieja del back ignora las columnas nuevas y `password_hash` nullable, y no hay `UPDATE` sobre mails. Si Fase 2 todavía no se desplegó, el primer arranque hace baseline V1 y aplica V2-V4 juntas.

**H6 — Verificación posterior (producción):** `curl https://<back>/actuator/health` → 200 (solo lectura). Con **casillas del propio usuario**: registrarse, recibir y usar el mail de confirmación; entrar con Google; "Olvidé mi contraseña" → mail → nueva contraseña → entrar. Probar que el mismo link no sirve dos veces y que `GET /api/usuarios/me` sin token da 401.

## Common Pitfalls adicionales de Ley 25.326 (mínimos prácticos)

- **No loguear** DNI, teléfono ni mail en claro (revisar `log.*`, `toString`, handler de integridad) `[VERIFIED: lectura del código]`.
- **No exponerlos** salvo al propio usuario (`/usuarios/me`) y al admin (Fase 4).
- **Aviso al recolectar** (art. 6): informar de forma expresa y clara la **finalidad** y los **destinatarios**, la existencia de la base y la **identidad y domicilio del responsable** `[CITED: texto de la ley 25.326 vía búsqueda: oas.org/juridico/pdfs/arg_ley25326.pdf]`; el **consentimiento** debe ser libre, expreso e informado `[CITED: idem]`.
- **Derechos** de acceso (gratis cada 6 meses), rectificación y supresión: dar un canal (mail de la agencia) en `/privacidad`.
- **Leyenda de la AAIP** (Disposición 10/2008): "El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto conforme lo establecido en el artículo 14, inciso 3 de la Ley Nº 25.326. La AGENCIA DE ACCESO A LA INFORMACIÓN PÚBLICA, en su carácter de Órgano de Control de la Ley Nº 25.326, tiene la atribución de atender las denuncias y reclamos que interpongan quienes resulten afectados en sus derechos por incumplimiento de las normas vigentes en materia de protección de datos personales." `[CITED: argentina.gob.ar/normativa/nacional/norma-144735 (Disposición 10/2008), vía búsqueda; el texto exacto de la leyenda debe validarlo un profesional]`.
- **Encargados/transferencias:** los datos pasan por proveedores en el exterior (Google para el login, Brevo para mails, Railway/Vercel/Cloudinary para hosting): nombrarlos en `/privacidad`. `[ASSUMED: A13]` (inscripción en el Registro Nacional de Bases de Datos de la AAIP y alcance de las transferencias: consultar a un profesional).
- Contenido de `/privacidad` (página estática, español): responsable (Dante Automotores, domicilio, mail), qué datos (nombre, apellido, mail, teléfono, DNI), para qué (identificar al usuario para comprar/cotizar y que la agencia lo contacte; el admin confirma identidad en persona), con quién se comparte (proveedores técnicos), cuánto se guardan, cómo ejercer derechos, la leyenda de la AAIP, y la baja de la cuenta (por mail a la agencia).

## Sources

### Primary (HIGH confidence)
- Código leído esta sesión (con rutas): `Usuario.java`, `AuthService.java`, `AuthController.java`, `JwtService.java`, `JwtAuthenticationFilter.java`, `CustomUserDetailsService.java`, `SecurityConfig.java` (líneas 49-61), `SecretosGuard.java`, `EntornoDeDesarrollo.java`, `DataSeeder.java`, `ConsultaService.java`, `ConsultaRequest.java`, `GlobalExceptionHandler.java` (línea 63), `RegistroRequest.java`, `application.yml`, migraciones V1-V3, `ClockConfig.java`, tests (`AuthServiceTest`, `SeguridadWebMvcTestBase`, `PostgresLocalTestBase`, `SecretosGuardTest`, `MigracionesPostgresTest`, `ConsultaServiceTest`); front: `AuthContext.jsx`, `api.js`, `ProtectedRoute.jsx`, `LoginPage.jsx`, `RegistroPage.jsx`, `AppRouter.jsx`, `Navbar.jsx`, `SeccionFinanciamiento.jsx`, `PublicacionDetallePage.jsx`, `errores.js`, `vercel.json`, `.env.example`, `package.json`.
- Ejecución local en esta sesión (probes en `scratchpad`, todo fuera del repo y la base temporal ya borrada): verificador Nimbus (JVM 17, Spring Security 6.3.4, JWKS real de Google), `RestClient` contra servidor HTTP local, Caffeine, libphonenumber 9.0.40 (41 entradas), `User.builder().password(null)` y BCrypt 72 bytes, `psql` contra Postgres 16.15 (UNIQUE con NULL, CHECK, mensaje `DETAIL`), `mvn -o -Ddante.pg.required=true test` (325 verdes) y `node --test` (17).
- `spring-boot-dependencies-3.3.5.pom` (versiones de Spring Security 6.3.4 y Caffeine 3.1.8) en `~/.m2`.
- Google: developers.google.com/identity/gsi/web/guides/verify-google-id-token; …/get-google-api-clientid; …/reference/js-reference.
- Spring Security: docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html.
- Brevo: developers.brevo.com/reference/sendtransacemail; developers.brevo.com/docs/ip-security.
- Railway: docs.railway.com/networking/outbound-networking.
- OWASP: cheatsheetseries.owasp.org (Forgot_Password_Cheat_Sheet, Authentication_Cheat_Sheet).
- libphonenumber: github.com/google/libphonenumber (README: coordenadas Maven, Apache-2.0).

### Secondary (MEDIUM confidence)
- Resultados de búsqueda que citan help.brevo.com (dominios gratuitos no autenticables → `@brevosend.com`; DMARC `reject` → soft bounce; plan gratis 300/día y activación transaccional; autorizar remitente con código; autenticación de dominio con Brevo code/DKIM/DMARC). Las páginas de help.brevo.com devolvieron 403 al abrirlas.
- station.railway.com (SMTP bloqueado en planes bajos; 401 "unrecognised IP" de Brevo; cabeceras `X-Forwarded-For`/`X-Real-IP`).
- support.google.com/cloud/answer/15549945 y developers.google.com/identity/protocols/oauth2/scopes (publicación y scopes básicos), vía búsqueda.
- Ley 25.326 (oas.org/juridico/pdfs/arg_ley25326.pdf), Disposición DNPDP 10/2008 (argentina.gob.ar/normativa/nacional/norma-144735), vía búsqueda.

### Tertiary (LOW confidence)
- Comportamiento de `X-Forwarded-For` en el borde de Railway (fuentes contradictorias).
- Interpretación del alcance de la inscripción en el Registro de bases de datos (no es asesoramiento legal).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versiones leídas del BOM y de Maven Central, y todo compilado/ejecutado en una JVM 17 con Spring 6.1.14 / Security 6.3.4.
- Architecture (verificación de Google, tokens, V4, gate): HIGH — probado; el diseño del front es estándar pero no se ejecutó.
- Brevo/Railway: MEDIUM — la API y el sender están en docs oficiales, pero las reglas de dominios gratuitos, activación y bloqueo de IP vienen de búsqueda; el humo con casillas reales (H2/H6) es el que lo confirma.
- Ley 25.326: MEDIUM-LOW — mínimos prácticos citados; el texto final y la inscripción necesitan revisión profesional.
- Pitfalls: HIGH para los reproducidos (password nulo, DETAIL de Postgres, 72 bytes, `node --test` con directorio), MEDIUM para el resto.

**Research date:** 2026-10-03
**Valid until:** 2026-10-17 para Brevo/Railway/Google Cloud (consolas y políticas cambian rápido); 2026-11-02 para el resto (Spring/Flyway estables). Revalidar H1/H2 antes del deploy.
