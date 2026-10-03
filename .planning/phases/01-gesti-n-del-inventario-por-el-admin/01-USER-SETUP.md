# Phase 1: User Setup Required

**Generated:** 2026-10-02
**Phase:** 01-gesti-n-del-inventario-por-el-admin (plan 01-02)
**Status:** Incomplete

Para que exista la cuenta admin, el dueño de la agencia tiene que elegir y setear estas variables. Nunca se commitean.

## Environment Variables

| Status | Variable | Source | Add to |
|--------|----------|--------|--------|
| [ ] | `ADMIN_EMAIL` | La elige el dueño de la agencia | Run configuration del IDE o shell antes de `mvn spring-boot:run` (y el hosting en la Fase 2) |
| [ ] | `ADMIN_PASSWORD` | La elige el dueño; al menos 8 caracteres | Mismo lugar que `ADMIN_EMAIL` |
| [ ] | `ADMIN_NOMBRE` | Nombre visible del admin (por ejemplo, Dante) | Mismo lugar que `ADMIN_EMAIL` |

## Cloudinary (plan 01-06)

Los tests automatizados mockean Cloudinary; para la verificacion manual de subida y borrado real de fotos hacen falta credenciales reales.

| Status | Variable | Source | Add to |
|--------|----------|--------|--------|
| [ ] | `CLOUDINARY_CLOUD_NAME` | Cloudinary Console -> Dashboard -> Product Environment Credentials | Mismo lugar que `ADMIN_EMAIL` |
| [ ] | `CLOUDINARY_API_KEY` | Cloudinary Console -> Dashboard -> Product Environment Credentials | Mismo lugar que `ADMIN_EMAIL` |
| [ ] | `CLOUDINARY_API_SECRET` | Cloudinary Console -> Dashboard -> Product Environment Credentials | Mismo lugar que `ADMIN_EMAIL` |

Verificacion: en la edicion de un auto, subir una JPG valida (aparece en la grilla), un PDF (rechazado con "formato no permitido"), una imagen de 12-15 MB (rechazada con "pesa mas de 10 MB") y un .exe renombrado a .jpg (rechazado por el backend); reordenar con las flechas y "Hacer portada" y recargar para comprobar que el orden persiste; llegar a 10 fotos y comprobar que no se puede subir la undecima.

## Verification

Con Postgres arriba y una base sin usuarios ADMIN, arrancar el back con las tres variables y hacer `POST /api/auth/login` con esas credenciales.

Expected results:
- El login devuelve 200 con rol `ADMIN`.
- Reiniciar con otra `ADMIN_PASSWORD` no cambia la contraseña de la cuenta (el seed no sincroniza).
- Con `SPRING_PROFILES_ACTIVE=prod`, sin `ADMIN_*` y sin admin en la base, el arranque aborta con `IllegalStateException`.
- La tabla `agencias` tiene "Dante Automotores" / "dante-automotores".

---

**Once all items complete:** Mark status as "Complete" at top of file.
