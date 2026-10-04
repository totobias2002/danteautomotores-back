# Phase 3: Cuentas verificadas - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-03
**Phase:** 03-cuentas-verificadas
**Areas discussed:** Qué significa "verificado", Login con Google, Cuentas que ya existen, Mails y recuperación

---

## Qué significa "verificado"

| Pregunta | Opciones | Elegida |
|----------|----------|---------|
| ¿Confirmar el mail antes de comprar/cotizar? | Sí, link al mail / No, alcanza con los datos | Sí, link al mail |
| ¿Confirmar el teléfono con código? | No, solo validar formato / Sí, código por SMS o WhatsApp | No, solo validar formato |
| ¿DNI único por cuenta? | Sí, único / No, puede repetirse | Sí, único |
| ¿Validar el DNI más allá del formato? | Solo formato / Foto del DNI | Solo formato |

---

## Login con Google

| Pregunta | Opciones | Elegida |
|----------|----------|---------|
| Mail de Google que ya tiene cuenta con contraseña | Se unen automáticamente / Se rechaza | Se unen automáticamente |
| ¿Cuándo pedir teléfono y DNI? | Apenas entra / Solo al comprar o cotizar | Apenas entra |
| ¿Nombre y apellido de Google editables? | Sí, se precargan y se pueden corregir / No | Sí, se precargan y se pueden corregir |

---

## Cuentas que ya existen

| Pregunta | Opciones | Elegida |
|----------|----------|---------|
| Compradores existentes sin apellido/DNI | Igual que Google: completar al entrar / Solo cuando quieran operar | Igual que Google: completar al entrar |
| ¿Mails existentes dados por confirmados? | No, también confirman / Sí | No, también confirman |
| Consulta anónima actual de la ficha | Pasa a exigir cuenta verificada / Queda anónima hasta la Fase 4 | Pasa a exigir cuenta verificada |

---

## Mails y recuperación

| Pregunta | Opciones | Elegida |
|----------|----------|---------|
| Servicio de mail | Brevo / Resend / SMTP de Gmail / Que lo investigue el research | Brevo |
| ¿Dominio propio? | Todavía no / no sé / Sí, tenemos dominio | Todavía no / no sé |
| Duración del link de contraseña | 1 hora, un solo uso / 24 horas | 1 hora, un solo uso |

---

## Claude's Discretion

- Diseño de "Completá tus datos", perfil y mails; qué campos del perfil se editan (por defecto DNI y mail no editables); cambio de contraseña desde el perfil; mecanismo técnico de Google; límites de intentos; transporte del estado "verificada" al front; tratamiento de datos personales (Ley 25.326).

## Deferred Ideas

- Cambiar el mail desde el perfil (con reconfirmación).
- Confirmar el teléfono con código.
- Foto o validación del DNI contra RENAPER.
- Dominio propio para los mails.
