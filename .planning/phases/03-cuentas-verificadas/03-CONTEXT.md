# Phase 3: Cuentas verificadas - Context

**Gathered:** 2026-10-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Toda persona que quiera comprar o cotizar tiene una cuenta con identidad completa (nombre, apellido, mail confirmado, teléfono y DNI), entre con mail y contraseña o con Google. Incluye: registro con los datos obligatorios, login con Google, pantalla "Completá tus datos" para quien no los tiene, confirmación del mail por link, recuperación de contraseña por mail (servicio de mail configurado en producción), ver y editar el perfil, y el gate de login con vuelta a la página de origen para "Lo quiero", "Cotizá tu usado" y "Mis mensajes" (los botones existen en el front; sus flujos llegan en las Fases 4 y 5).

Requisitos: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, PROD-03.

</domain>

<decisions>
## Implementation Decisions

### Qué significa "cuenta verificada"
- **D-01:** Una cuenta está **verificada** (puede comprar, cotizar y consultar) cuando tiene nombre, apellido, teléfono y DNI cargados **y** el mail confirmado. Sin eso puede navegar y guardar favoritos. — **Reversibility:** costly — el estado "verificada" lo consultan el back (gate de endpoints) y el front (gate de botones) de esta fase y de las Fases 4 y 5.
- **D-02:** **El mail se confirma con un link** que llega al registrarse (y que se puede reenviar). El link de confirmación dura **24 horas**. Con Google el mail ya viene confirmado.
- **D-03:** **El teléfono no se confirma con código**: solo se valida que sea un celular argentino válido (código de área + número) y se guarda normalizado.
- **D-04:** **El DNI es único** (una cuenta por DNI): si alguien registra un DNI existente se rechaza con un mensaje que sugiere recuperar la contraseña, sin revelar de quién es la cuenta. — **Reversibility:** one-way — es una restricción UNIQUE en la base (migración Flyway) y una regla del contrato del registro.
- **D-05:** **El DNI se valida solo por formato** (7 u 8 dígitos, se guarda sin puntos). Sin foto de DNI ni validación contra RENAPER: el admin confirma la identidad al cerrar la operación en persona.

### Login con Google
- **D-06:** Si el mail de Google **ya tiene una cuenta con contraseña, se unen automáticamente**: entra a la misma cuenta (datos, favoritos) y desde ahí puede usar los dos métodos. Solo cuando Google confirma el mail (`email_verified`). — **Reversibility:** costly — define el modelo de identidad (una cuenta, varios métodos de ingreso).
- **D-07:** A quien entra con Google por primera vez (o a cualquier cuenta incompleta) se le muestra **"Completá tus datos" apenas entra** (apellido, teléfono, DNI). Puede saltearla y navegar, pero comprar, cotizar o consultar lo vuelve a llevar ahí.
- **D-08:** El nombre y apellido de Google **se precargan y se pueden corregir** (en "Completá tus datos" y en el perfil), para que coincidan con el DNI.

### Cuentas que ya existen
- **D-09:** Los compradores ya registrados (sin apellido ni DNI, teléfono opcional) se tratan **igual que Google**: en el próximo login ven "Completá tus datos"; para operar la tienen que completar. — **Reversibility:** costly — la migración deja `apellido`/`dni` nulos para las cuentas viejas, así que las columnas no pueden ser NOT NULL a nivel base; la obligatoriedad vive en la regla de "cuenta verificada".
- **D-10:** Los mails de las cuentas existentes **no se dan por confirmados**: también tienen que confirmarlo antes de operar.
- **D-11:** La **consulta anónima actual** de la ficha ("Enviar consulta" con nombre/mail/teléfono sueltos) **pasa a exigir cuenta verificada** y usa los datos de la cuenta (el formulario solo pide el mensaje). La Fase 4 la reemplaza por la conversación "Lo quiero".

### Mails y recuperación de contraseña
- **D-12:** El servicio de mail es **Brevo** (plan gratis), detrás de una interfaz propia para poder cambiarlo; credenciales por variables de entorno (la cuenta y la API key/SMTP las crea el usuario: checkpoint humano). En desarrollo y tests no se mandan mails reales (implementación que loguea o captura). — **Reversibility:** reversible — aislado detrás de la interfaz.
- **D-13:** **Sin dominio propio por ahora**: el remitente (dirección y nombre) es configurable por variable de entorno y se arranca con el que permita Brevo sin dominio verificado. Dejar en el runbook cómo pasar a un dominio propio (registros DNS) más adelante.
- **D-14:** El link para **cambiar la contraseña dura 1 hora y es de un solo uso**. Pedirlo no revela si el mail tiene cuenta (respuesta siempre igual).

### Decisiones tomadas tras el research (2026-10-05)
- **D-15:** La **unión con Google borra la contraseña** de una cuenta cuyo mail no está confirmado (anti pre-hijacking). Quien quiera volver a usar mail y contraseña la restablece con "olvidé mi contraseña"; no pierde datos ni favoritos. Una cuenta con mail confirmado conserva su contraseña. (Resuelve Open Question 1 / A3.) — **Reversibility:** costly.
- **D-16:** El botón de la ficha **"Reservar o agendar visita" se renombra a "Lo quiero"** y se agrega **"Mis mensajes"** al `Navbar` (usuarios con sesión) con la ruta `/mensajes` protegida y una página "Próximamente"; la Fase 4 la llena. (Open Question 2.)
- **D-17:** Se **mantiene el mensaje "Ya existe una cuenta con ese email"** al registrarse, con límite de intentos por IP; queda anotado como deuda de seguridad (OWASP recomienda mensaje genérico). (Open Question 3.)
- **D-18:** Remitente de Brevo con **casilla Gmail sin dominio propio**; se mide la entregabilidad y, si cae en spam, se pasa a dominio propio (ver D-13). (Open Question 4.)
- **D-19:** **Cambiar o restablecer la contraseña cierra las demás sesiones** (claim `pca` en el JWT verificado en el filtro). (Open Question 5.) — **Reversibility:** costly.
- **D-20:** El texto de **`/privacidad`** se publica como **borrador** con una leyenda simple; la **revisión legal y la inscripción ante la AAIP** (Ley 25.326) quedan como pendiente de la agencia, **fuera del código**. (Open Question 6.)
- **D-21:** **No se manda mail masivo** a las cuentas viejas: el mail de confirmación se envía cuando el usuario toca "Reenviar mail de confirmación" en `/completar-datos`. (Open Question 7.)
- **D-22:** El formulario interino **"Vender tu auto"** (`/vender` y `POST /api/solicitudes-venta`) **exige cuenta verificada**, en el back y en la ruta del front (coherente con D-01). Confirmado por el usuario el 2026-10-05.
- **D-23:** **Una cuenta ADMIN no se une por Google**: el admin entra solo con mail y contraseña. Confirmado el 2026-10-05.
- **D-24:** Tras el gate, **"Cotizá tu usado" conserva el link de WhatsApp** hasta que llegue el cotizador de la Fase 5. Confirmado el 2026-10-05.
- **Checkpoints humanos (los hace el usuario, sin pegar secretos en el chat):** Client ID de Google Cloud, cuenta y API key de Brevo con remitente, variables en Railway y Vercel.

### Claude's Discretion
- Diseño de "Completá tus datos", del perfil y de los mails (estilo del sitio; Kavak / Mercado Libre como referencia).
- Qué se puede editar en el perfil: por defecto nombre, apellido y teléfono editables; **DNI no editable** una vez cargado (si está mal, lo corrige el admin) y **mail no editable** en esta fase (cambiarlo exigiría reconfirmar; queda como mejora). El planner puede ajustar si encuentra una razón fuerte, dejándolo anotado.
- Cambio de contraseña desde el perfil (pidiendo la actual) y cómo convive con cuentas solo-Google (pueden definir una contraseña vía "olvidé mi contraseña").
- Mecanismo técnico de Google (Google Identity Services con ID token verificado en el back vs. flujo OAuth con redirect) y configuración en Google Cloud (credenciales y URIs de producción: checkpoint humano).
- Límites de intentos (login, reenvío de confirmación, pedidos de recuperación) y mensajes de error.
- Cómo se transporta el estado "verificada" al front (claims del JWT y/o endpoint `/me`) y cómo el back rechaza las acciones de cuentas no verificadas (403 con mensaje accionable).
- Tratamiento de datos personales (Ley 25.326): no loguear DNI ni teléfono, no exponerlos en respuestas públicas, aviso de privacidad mínimo en el registro.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Alcance y requisitos
- `.planning/ROADMAP.md` §Phase 3 — goal y success criteria
- `.planning/REQUIREMENTS.md` — AUTH-01..AUTH-06, PROD-03
- `.planning/PROJECT.md` — core value ("el admin sabe al 100 % con quién está hablando"), Ley 25.326, una sola cuenta admin

### Decisiones previas
- `.planning/phases/01-gesti-n-del-inventario-por-el-admin/01-CONTEXT.md` — sesión vencida → login con vuelta a la página de origen (patrón a reutilizar para AUTH-06), formato de error uniforme
- `.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-CONTEXT.md` — Flyway: todo cambio de esquema es una migración nueva (V4+), nunca editar V1–V3
- `.planning/phases/02-cat-logo-p-blico-real-en-producci-n/02-08-PLAN.md` — runbook de producción (Railway/Vercel); las variables nuevas de esta fase (Brevo, Google) se suman ahí como checkpoints humanos

No hay specs externas: los requisitos quedan en las decisiones de arriba.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Back: `AuthService`/`AuthController` (`/api/auth/registro`, `/api/auth/login`), `JwtService` (HS256), `DataSeeder` (el registro público solo crea COMPRADOR), `SecretosGuard` + `EntornoDeDesarrollo` (secretos obligatorios fuera de desarrollo: sumar los de Brevo y Google), `GlobalExceptionHandler` (errores `{error}` / `{error,campos}`), Flyway V1–V3.
- Front: `AuthContext` (login/registrar/logout, sesión en localStorage, manejo de sesión vencida con `state.from`), `ProtectedRoute` (redirige a /login), `LoginPage` y `RegistroPage` (ya tienen el botón "Continuar con Google" decorativo con un TODO), `IconoGoogle`, `mensajeDeError` (`src/utils/errores.js`).

### Established Patterns
- Errores uniformes en español; 401/403 en JSON.
- `open-in-view: false`: lecturas lazy dentro de servicios `@Transactional`.
- Front: el interceptor 401 ignora `/auth/*`; la vuelta a la página de origen usa `location.state.from`.

### Integration Points
- `Usuario` (entity): hoy tiene `nombre`, `email`, `passwordHash` (NOT NULL), `rol`, `telefono`, `fechaRegistro`. Faltan `apellido`, `dni`, `emailConfirmado`, proveedor/ID de Google; `passwordHash` debe admitir cuentas solo-Google.
- `ConsultaService.crear` / `ConsultaRequest` (D-11) y la ficha `PublicacionDetallePage` ("Enviar consulta").
- Botones "Lo quiero", "Cotizá tu usado" (navbar "Vender tu auto", `SeccionFinanciamiento`) y "Mis mensajes": hoy abren WhatsApp o no hacen nada; en esta fase solo se les pone el gate de login/verificación (AUTH-06).

</code_context>

<specifics>
## Specific Ideas

- Comportamiento tipo Mercado Libre: una cuenta, varios métodos de ingreso; completar datos sin cortar la navegación.
- La fricción se concentra en el momento de operar (comprar/cotizar/consultar), no al navegar.

</specifics>

<deferred>
## Deferred Ideas

- Cambiar el mail desde el perfil (con reconfirmación).
- Confirmar el teléfono con código por SMS/WhatsApp.
- Foto o validación del DNI contra RENAPER.
- Dominio propio para los mails (registros DNS) cuando la agencia lo tenga.

</deferred>

---

*Phase: 03-cuentas-verificadas*
*Context gathered: 2026-10-03*
