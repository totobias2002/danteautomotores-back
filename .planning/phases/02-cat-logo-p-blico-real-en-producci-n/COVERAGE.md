# API Coverage — Cloudinary (entrega de imágenes) y plataformas de despliegue

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> La Fase 1 integró la subida y el borrado con el SDK `cloudinary-http44` (ver `../01-gesti-n-del-inventario-por-el-admin/COVERAGE.md`) y dejó las transformaciones de entrega para esta fase. La Fase 2 las integra en el catálogo público por URL (`src/utils/cloudinary.js` del front: `urlMiniatura`, `TRANSFORMACION_MINIATURA`, `TRANSFORMACION_CARD`, `TRANSFORMACION_DETALLE`; planes 02-01, 02-05 y 02-07). Railway y Vercel no se integran por API: el despliegue lo hace el usuario desde sus paneles (plan 02-08, checkpoints humanos).

| capability | decision | reason |
|---|---|---|
| transformación de entrega por URL para cards (c_fill,w_640,h_420) | INTEGRATE | |
| entrega por URL de la foto principal del detalle (c_limit,w_1280) | INTEGRATE | |
| transformación de entrega por URL para miniaturas (c_fill,w_160,h_120) | INTEGRATE | |
| calidad automática en la entrega (q_auto) | INTEGRATE | |
| formato automático en la entrega (f_auto) | INTEGRATE | |
| upload y destroy por public_id (Fase 1; los usa la demo vía la API) | INTEGRATE | |
| imágenes responsivas con srcset, dpr_auto, w_auto o client hints | OPT-OUT | not needed yet — tamaños fijos pensados para pantallas 2x alcanzan; la galería inmersiva es UX-03 (Fase 6) |
| zoom y pantalla completa servidos por Cloudinary | OPT-OUT | explicitly out of scope — galería inmersiva de la Fase 6 (UX-03) |
| transformaciones eager o named transformations | OPT-OUT | not needed — las variantes por URL se generan una vez y quedan en el CDN; son 3 variantes por foto (RESEARCH A13) |
| overlays o marca de agua | OPT-OUT | not needed — ningún requisito lo pide |
| URLs de entrega firmadas o recursos privados | OPT-OUT | not needed — las fotos del catálogo son públicas |
| Admin API para monitorear cuota de transformaciones | OPT-OUT | not needed yet — el plan gratuito absorbe las variantes de ~55 fotos de demo (RESEARCH A13); se revisa si crece el inventario |
| entrega de video | OPT-OUT | explicitly out of scope — el inventario solo usa imágenes |
| API o CLI de Railway (deploy, variables, backups) | OPT-OUT | explicitly out of scope — restricción del usuario: todo lo que toca producción lo hace el usuario desde el panel (plan 02-08) |
| API o CLI de Vercel (deploy, variables de entorno) | OPT-OUT | explicitly out of scope — misma restricción; el front se despliega con push y la variable VITE_API_URL se carga en el panel |
