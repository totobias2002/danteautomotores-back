# API Coverage — Cloudinary

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Fase 1 usa el SDK `cloudinary-http44` 1.39.0 (ya integrado) para las fotos de los autos. Planes: 01-06 (subida) y 01-07 (borrado).
> La Fase 5 (fotos de cotizaciones) parte de esta misma matriz.

| capability | decision | reason |
|---|---|---|
| upload de imagen (Uploader.upload) a la carpeta danteautomotores/publicaciones | INTEGRATE | |
| opción resource_type=image en la subida | INTEGRATE | |
| opción allowed_formats=jpg,png,webp en la subida | INTEGRATE | |
| guardar public_id y secure_url de la respuesta de upload | INTEGRATE | |
| destroy por public_id (Uploader.destroy) | INTEGRATE | |
| invalidación de CDN al borrar (invalidate=true) | INTEGRATE | |
| errores del SDK (credenciales, IOException) → 502 en español | INTEGRATE | |
| rename de public_id | OPT-OUT | not needed — el orden y la portada viven en la columna orden de la DB, nunca se renombran assets |
| transformaciones de entrega (resize, f_auto, q_auto en URLs) | OPT-OUT | not needed yet — se optimiza la entrega en el catálogo público (Fase 2) y la galería (Fase 6); la Fase 1 guarda secure_url tal cual |
| transformaciones eager o entrantes al subir | OPT-OUT | not needed yet — el límite de 10 MB alcanza para la Fase 1; el redimensionado se decide con la galería (Fase 6) |
| Admin API: listar recursos de una carpeta | OPT-OUT | not needed — la DB es la fuente de verdad de las fotos y no hay UI de listado de assets |
| Admin API: borrado masivo o por prefijo (delete_resources) | OPT-OUT | not needed — alcanza con el destroy por asset después del commit; los huérfanos viejos sin public_id se aceptan (RESEARCH Runtime State) |
| tags y context metadata en la subida | OPT-OUT | not needed — la relación foto → publicación vive en la DB |
| subida firmada directa desde el navegador (upload presets, signatures) | OPT-OUT | explicitly out of scope — las subidas pasan por el backend para validar tipo real, tamaño y tope de 10 fotos (ADM-03) |
| Upload Widget de Cloudinary en el front | OPT-OUT | explicitly out of scope — misma razón que la subida directa; además no se agregan dependencias al front |
| subida de video (resource_type=video) | OPT-OUT | explicitly out of scope — el inventario solo usa imágenes |
| add-ons de moderación, auto-tagging o background removal | OPT-OUT | not needed — son add-ons pagos y ningún requisito los pide |
| explicit (regenerar derivados de un asset existente) | OPT-OUT | not needed — no hay derivados (ver transformaciones) |
