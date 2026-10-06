---
phase: quick
plan: 261006-ohs
type: execute
wave: 1
depends_on: []
files_modified:
  - ../danteautomotores-front/src/pages/ConfirmarEmailPage.jsx
autonomous: true
---

<objective>
En la pantalla de mail confirmado (estado 'ok') de ConfirmarEmailPage, el botón "Seguir buscando autos" debe llevar al home ("/") en vez de a "/autos". El texto del botón no cambia. Pedido del usuario tras probar el flujo real en producción (2026-10-06).
</objective>

<tasks>
<task type="auto">
  <name>Tarea 1: apuntar el botón al home</name>
  <files>../danteautomotores-front/src/pages/ConfirmarEmailPage.jsx</files>
  <action>Cambiar el `to="/autos"` del `<Link>` del estado 'ok' por `to="/"`. No tocar el texto ni el resto del archivo. Ningún test del front depende de esa ruta (no hay test de esta página). Commit atómico en el repo del front; NO hacer push (lo hace el usuario).</action>
  <verify><automated>cd ../danteautomotores-front && npm run lint --silent -- src/pages/ConfirmarEmailPage.jsx && npm test --silent && npm run build --silent</automated></verify>
  <done>El botón del estado 'ok' lleva a "/"; lint, tests y build del front pasan; el cambio está commiteado y sin push.</done>
</task>
</tasks>
