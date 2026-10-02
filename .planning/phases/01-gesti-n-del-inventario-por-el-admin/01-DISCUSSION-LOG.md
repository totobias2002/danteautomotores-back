# Phase 1: Gestión del inventario por el admin - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-02
**Phase:** 01-Gestión del inventario por el admin
**Areas discussed:** Cuenta admin y agencia

---

## Cuenta admin y agencia

### ¿Cómo se crea la cuenta admin?

| Option | Description | Selected |
|--------|-------------|----------|
| Variables de entorno | Al arrancar, si no existe ningún ADMIN, se crea con ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_NOMBRE. Si ya existe, no se toca. | ✓ |
| Migración/SQL con hash | Script SQL con un hash de BCrypt fijo; la credencial inicial queda en el repo | |
| Vos decidís | Lo define Claude | |

### Si las variables cambian y el admin ya existe

| Option | Description | Selected |
|--------|-------------|----------|
| No se toca | Solo se crea si no hay ningún admin | ✓ |
| Sincroniza la contraseña | Se actualiza la contraseña con la del env | |
| Vos decidís | Lo define Claude | |

### Agencias

| Option | Description | Selected |
|--------|-------------|----------|
| Una sola agencia fija | Se siembra Dante, los autos se asocian solos y el panel solo la edita; la tabla se mantiene | ✓ |
| Mantener el CRUD de varias | Queda como está | |
| Eliminar Agencia del modelo | Los datos de contacto pasan a config y se borra la entidad | |

### Bloqueo de otros admins

| Option | Description | Selected |
|--------|-------------|----------|
| Solo por seed, sin endpoint | Sin endpoint que cree o promueva admins; los ADMIN de dev se dejan | ✓ |
| Seed + chequeo de unicidad | El arranque falla o avisa si hay más de un ADMIN | |
| Vos decidís | Lo define Claude | |

---

## Claude's Discretion

- Fotos (validación, reorden, portada, borrado en Cloudinary)
- Listado del panel, estados y destacados
- Formato de errores y manejo de la sesión vencida en el front

## Deferred Ideas

Ninguna.
