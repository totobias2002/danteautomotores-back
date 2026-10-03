# Dante Automotores

## What This Is

Marketplace web de autos usados (Dante Automotores, con soporte para varias agencias). Un único usuario administrador gestiona las agencias y publica sus autos. Los usuarios registrados pueden comprar un auto (abrir una conversación de compra con el admin desde la propia web) y cotizar su propio auto, que reciben con un precio estimado al instante, para vendérselo a la agencia o entregarlo como parte de pago. Son dos repos: backend Spring Boot (`danteautomotores-back`) y frontend React (`danteautomotores-front`).

## Core Value

Un usuario registrado y verificado puede encontrar un auto y hablar con la agencia para comprarlo, o cotizar el suyo, todo dentro de la web, y el admin sabe al 100 % con quién está hablando.

## Business Context

- **Customer**: Compradores y vendedores particulares de autos usados (Argentina). Del otro lado está la agencia, representada por una única cuenta admin.
- **Revenue model**: Venta de autos de la agencia y compra o toma de usados (venta directa o parte de pago). La web genera y gestiona esas operaciones.
- **Success metric**: Conversaciones de compra y cotizaciones iniciadas por usuarios reales y gestionadas por el admin desde el panel.
- **Strategy notes**: Pedido del jefe. El front debe resultar familiar (estilo Kavak / Mercado Libre) y verse más "wow" sin exagerar con las animaciones.

## Requirements

### Validated

<!-- Inferido del código existente (ver .planning/codebase/) -->

- ✓ Registro y login con mail y contraseña, usando JWT (roles ADMIN / COMPRADOR) — existing
- ✓ Backend: CRUD de publicaciones con búsqueda filtrada (Specifications), cambio de estado y fotos en Cloudinary — existing
- ✓ Backend: entidades y endpoints de Agencia, Consulta (contacto sobre publicación) y Favorito — existing
- ✓ Front: catálogo estilo Kavak, detalle de publicación, home con video, sección de financiamiento — existing (parcialmente con datos mock)
- ✓ Front: panel admin estilo Mercado Libre con CRUD de publicaciones — existing
- ✓ Config por variables de entorno, Dockerfile y actuator, para deploy en Railway/Render + Vercel — existing
- ✓ Una única cuenta admin (sembrada desde `ADMIN_*`) que gestiona todas las publicaciones de varias agencias: crear, editar, eliminar con conteo de consultas/favoritos, fotos en Cloudinary (validación por contenido, tope de 10, orden y portada), estados disponible/reservado/vendido y destacados — Phase 1
- ✓ Errores de la API con formato uniforme (`{"error"}` / `{"error","campos"}`), 401/403 en JSON y sesión vencida que lleva al login y vuelve a la página de origen — Phase 1

### Active

- [ ] Front conectado al backend real en todas las páginas (eliminar `catalogoMock.js` y `USE_MOCK_DATA`)
- [ ] Registro obligatorio para cotizar o comprar, con teléfono y DNI obligatorios
- [ ] Login con Google funcional; el usuario que entra con Google debe completar teléfono y DNI antes de operar
- [ ] Botón "Lo quiero" en la publicación, que abre una conversación de compra con el admin atada a ese auto (opcionalmente con el auto del usuario como parte de pago)
- [ ] Mensajería in-site estilo bandeja (tipo Mercado Libre): conversaciones atadas a un auto o a una cotización, con indicador de no leídos para usuario y admin
- [ ] Bandeja del admin en el panel para ver y responder todas las conversaciones y saber quién es cada usuario (nombre, teléfono, DNI)
- [ ] Cotizador "Vendé / cotizá tu usado": formulario paso a paso (marca, modelo, año, versión, km, estado, fotos) que devuelve un precio estimado desde una API externa
- [ ] La cotización sirve para venta directa a la agencia o como parte de pago; el usuario puede aceptar o "consultar otro precio", lo que abre una conversación con el admin
- [ ] Integración de precios detrás de una interfaz intercambiable (proveedor a definir con la investigación)
- [ ] Pulido visual medido: microinteracciones (hover, feedback, skeletons), aparición al scrollear, galería inmersiva en el detalle y un cotizador con progreso animado y revelado de precio

### Out of Scope

- Pago o checkout online (seña o compra completa) — la operación se cierra hablando con el admin; puede evaluarse más adelante
- Múltiples vendedores o admins publicando (agencias con admin propio) — por ahora una sola cuenta admin gestiona todas las agencias y sus autos
- Chat en tiempo real (WebSockets) — se eligió bandeja de mensajes, que es más simple; tiempo real puede llegar después
- Usuarios particulares publicando sus autos en el marketplace — el usuario cotiza y le vende a la agencia; no publica
- App mobile nativa — web responsive alcanza

## Context

- Brownfield: el backend ya tiene auth JWT, publicaciones, agencias, consultas y favoritos. El front tiene diseño avanzado pero varias páginas usan mocks (Home, Autos, Agencia, Detalle).
- Hoy "Cotizá tu usado" y la compra solo abren WhatsApp (`utils/whatsapp.js`, con un número placeholder). El navbar ya tiene "Vender tu auto" sin flujo detrás.
- La entidad `Consulta` existente (contacto anónimo sobre una publicación) probablemente se reemplace o evolucione hacia el modelo de conversaciones con usuario registrado.
- Deuda conocida (ver `.planning/codebase/CONCERNS.md`): búsqueda sin paginación, `ddl-auto: update`, `show-sql` y secret JWT por defecto en el yml base, y el split de CORS no recorta espacios (la Fase 2 lo cierra). La Fase 1 resolvió los tests (172 en el back), el `@ControllerAdvice`, el 500 ante tokens malformados y la validación de uploads.
- El entorno local tiene solo JDK 17: el back se compila y se prueba con `-Djava.version=17`.
- Las APIs de precios de autos en Argentina (InfoAuto, ACARA) suelen ser pagas o requerir convenio; la investigación debe evaluar alternativas (por ejemplo, la API de Mercado Libre con publicaciones comparables).
- Los datos personales (DNI, teléfono) se guardan y se muestran al admin; hay que considerar protección de datos personales (Ley 25.326).

## Constraints

- **Tech stack**: Spring Boot 3.3 / Java 21 / PostgreSQL / JPA + React 18 / Vite / Tailwind v4 — se mantiene el stack existente
- **Repos**: back y front en repos separados (`danteautomotores-back`, `danteautomotores-front`); la planificación vive en el back
- **Imágenes**: Cloudinary (ya integrado) también para las fotos de los autos cotizados
- **Admin**: una única cuenta admin por ahora; el diseño no debe impedir sumar más en el futuro
- **Precios**: depende de un proveedor externo aún no elegido; el costo y el acceso pueden condicionar la elección
- **Timeline**: sin fecha; se prioriza hacerlo bien
- **UX**: el front debe ser familiar (patrones Kavak / Mercado Libre) y animado con moderación

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Compra = conversación con el admin ("Lo quiero"), sin pago online | El negocio cierra la venta en persona; la web genera leads calificados | — Pending |
| Mensajería tipo bandeja, no tiempo real | Más simple de construir y operar; alcanza para el volumen esperado | — Pending |
| Registro obligatorio con teléfono + DNI | El admin necesita saber con certeza con quién habla | — Pending |
| Login con Google + completar perfil | Baja la fricción del registro obligatorio | — Pending |
| Precio estimado desde una API externa detrás de una interfaz | Precisión de mercado; la interfaz permite cambiar de proveedor o usar un fallback | — Pending |
| Una sola cuenta admin | Pedido actual del negocio | ✓ Good — sembrada al arrancar desde `ADMIN_*`, el registro público solo crea compradores (Phase 1) |
| Varias agencias gestionadas por el único admin | El negocio tiene sucursales; el auto pertenece a una agencia | ✓ Good — ABM de agencias con baja segura (Phase 1) |
| Destacado independiente del estado | El admin decide qué se muestra en la Home sin tocar el estado | ✓ Good (Phase 1) |
| Subida a Cloudinary fuera de la transacción, con lock corto y compensación | Una subida lenta no debe retener locks ni conexiones de la base | ✓ Good — WR-13 (Phase 1) |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-10-03 after Phase 1*
