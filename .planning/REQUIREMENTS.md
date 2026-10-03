# Requirements: Dante Automotores

**Defined:** 2026-10-02
**Core Value:** Un usuario registrado y verificado puede encontrar un auto y hablar con la agencia para comprarlo, o cotizar el suyo, todo dentro de la web, y el admin sabe al 100 % con quién está hablando.

## v1 Requirements

### Catálogo

- [x] **CAT-01**: El visitante ve el catálogo con datos reales del backend (sin mocks) en Home, Autos, Agencia y Detalle
- [x] **CAT-02**: El visitante puede filtrar y ordenar el catálogo, que se carga paginado
- [ ] **CAT-03**: El visitante ve en la Home los autos que el admin marcó como destacados
- [x] **CAT-04**: El visitante ve el estado de cada auto (disponible / reservado / vendido) en card y detalle

### Admin – Publicaciones

- [x] **ADM-01**: Existe una única cuenta admin, creada por config o seed, y no se puede registrar otra desde la web
- [x] **ADM-02**: El admin puede crear, editar y eliminar publicaciones desde el panel, contra la API real
- [x] **ADM-03**: El admin puede subir, reordenar y eliminar fotos (solo imágenes válidas, con límite de tamaño)
- [x] **ADM-04**: El admin puede cambiar el estado de un auto (disponible / reservado / vendido)
- [x] **ADM-05**: El admin puede marcar o desmarcar autos como destacados
- [ ] **ADM-06**: El admin ve métricas simples del mes (cotizaciones, conversaciones abiertas, autos vendidos)

### Cuentas

- [ ] **AUTH-01**: El usuario puede registrarse con nombre, apellido, mail, contraseña, teléfono y DNI (los dos últimos obligatorios)
- [ ] **AUTH-02**: El usuario puede iniciar sesión con Google
- [ ] **AUTH-03**: El usuario que entra con Google y no tiene teléfono o DNI debe completarlos antes de comprar o cotizar
- [ ] **AUTH-04**: El usuario puede recuperar su contraseña con un link por mail
- [ ] **AUTH-05**: El usuario puede ver y editar su perfil
- [ ] **AUTH-06**: "Lo quiero", el cotizador y la mensajería exigen estar logueado; si no lo está, se lo lleva al login y después vuelve a donde estaba

### Compra y mensajería

- [ ] **MSG-01**: El usuario puede tocar "Lo quiero" en una publicación y se abre una conversación de compra atada a ese auto
- [ ] **MSG-02**: Al iniciar la compra, el usuario puede indicar opcionalmente que entrega su auto (una cotización suya) como parte de pago
- [ ] **MSG-03**: El usuario ve "Mis mensajes", con sus conversaciones y el auto o cotización asociado
- [ ] **MSG-04**: El usuario y el admin pueden intercambiar mensajes de texto dentro de una conversación
- [ ] **MSG-05**: Ambos ven un contador de mensajes no leídos en la web
- [ ] **MSG-06**: Ambos reciben un mail cuando les llega un mensaje nuevo
- [ ] **MSG-07**: El admin tiene una bandeja con todas las conversaciones, filtrable por tipo (compra/cotización), estado (abierta/cerrada) y no leídas
- [ ] **MSG-08**: El admin puede cerrar o reabrir una conversación
- [ ] **MSG-09**: El admin ve la ficha del usuario (nombre, teléfono, DNI, mail) con su historial de conversaciones y cotizaciones

### Cotizador

- [ ] **COT-01**: El usuario puede cotizar su auto en un formulario paso a paso (marca → modelo → año → versión → km → estado → fotos)
- [ ] **COT-02**: Al final ve un precio estimado (rango) obtenido de un proveedor externo de precios
- [ ] **COT-03**: El usuario elige si es venta directa o parte de pago
- [ ] **COT-04**: El usuario puede aceptar la cotización o "consultar otro precio", y ambas opciones abren una conversación con el admin
- [ ] **COT-05**: El usuario ve el historial de sus cotizaciones
- [ ] **COT-06**: El proveedor de precios está detrás de una interfaz intercambiable; si falla, el usuario igual puede enviar la cotización para que el admin la valúe manualmente

### Experiencia visual

- [ ] **UX-01**: Microinteracciones en cards, botones y formularios (hover, feedback al click, skeletons de carga)
- [ ] **UX-02**: Secciones que aparecen al scrollear y transiciones suaves entre páginas
- [ ] **UX-03**: Galería inmersiva en el detalle (swipe, zoom, pantalla completa)
- [ ] **UX-04**: Cotizador con barra de progreso animada y revelado animado del precio
- [ ] **UX-05**: Animaciones moderadas que respetan `prefers-reduced-motion`

### Producción

- [x] **PROD-01**: Un token inválido o vencido devuelve 401, y los errores de la API tienen un formato uniforme (manejo global)
- [ ] **PROD-02**: La app no arranca sin un secret JWT y credenciales de Cloudinary válidas; CORS acepta orígenes con espacios
- [ ] **PROD-03**: Hay un servicio de envío de mails configurado (para recuperar contraseña y avisos)
- [ ] **PROD-04**: Back y front quedan desplegados (Railway/Render + Vercel) con perfil de producción (sin `ddl-auto: update` ni `show-sql`)

## v2 Requirements

### Cuentas

- **AUTH-V2-01**: El usuario recibe un mail de verificación al registrarse

### Calidad

- **QA-V2-01**: Tests automatizados de auth, permisos de admin y flujos principales (y quitar `-DskipTests` del Dockerfile)

### Mensajería

- **MSG-V2-01**: Chat en tiempo real (WebSockets)
- **MSG-V2-02**: Aviso con sonido o título de pestaña al llegar mensajes

### Compra

- **BUY-V2-01**: Reserva con seña online (por ejemplo, Mercado Pago)

### Admin

- **ADM-V2-01**: Múltiples cuentas admin / vendedores

## Out of Scope

| Feature | Reason |
|---------|--------|
| Checkout / pago total online | La venta se cierra hablando con el admin y en persona |
| Particulares publicando autos | El usuario cotiza y le vende a la agencia; no publica |
| App mobile nativa | Web responsive alcanza |
| Animaciones excesivas | Pedido explícito: "ni exceso ni quedarnos cortos" |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CAT-01 | Phase 2 | Complete |
| CAT-02 | Phase 2 | Complete |
| CAT-03 | Phase 2 | Pending |
| CAT-04 | Phase 2 | Complete |
| ADM-01 | Phase 1 | Complete |
| ADM-02 | Phase 1 | Complete |
| ADM-03 | Phase 1 | Complete |
| ADM-04 | Phase 1 | Complete |
| ADM-05 | Phase 1 | Complete |
| ADM-06 | Phase 5 | Pending |
| AUTH-01 | Phase 3 | Pending |
| AUTH-02 | Phase 3 | Pending |
| AUTH-03 | Phase 3 | Pending |
| AUTH-04 | Phase 3 | Pending |
| AUTH-05 | Phase 3 | Pending |
| AUTH-06 | Phase 3 | Pending |
| MSG-01 | Phase 4 | Pending |
| MSG-02 | Phase 5 | Pending |
| MSG-03 | Phase 4 | Pending |
| MSG-04 | Phase 4 | Pending |
| MSG-05 | Phase 4 | Pending |
| MSG-06 | Phase 4 | Pending |
| MSG-07 | Phase 4 | Pending |
| MSG-08 | Phase 4 | Pending |
| MSG-09 | Phase 4 | Pending |
| COT-01 | Phase 5 | Pending |
| COT-02 | Phase 5 | Pending |
| COT-03 | Phase 5 | Pending |
| COT-04 | Phase 5 | Pending |
| COT-05 | Phase 5 | Pending |
| COT-06 | Phase 5 | Pending |
| UX-01 | Phase 6 | Pending |
| UX-02 | Phase 6 | Pending |
| UX-03 | Phase 6 | Pending |
| UX-04 | Phase 6 | Pending |
| UX-05 | Phase 6 | Pending |
| PROD-01 | Phase 1 | Complete |
| PROD-02 | Phase 2 | Pending |
| PROD-03 | Phase 3 | Pending |
| PROD-04 | Phase 2 | Pending |

**Coverage:**
- v1 requirements: 40 total
- Mapped to phases: 40
- Unmapped: 0 ✓

---
*Requirements defined: 2026-10-02*
*Last updated: 2026-10-02 after roadmap creation (traceability mapped)*
