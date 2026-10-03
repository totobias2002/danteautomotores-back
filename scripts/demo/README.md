# Carga de datos de demo

Carga agencias y autos de ejemplo (los mismos del catálogo mock del front), cada auto con 5 fotos reales de su modelo, para que la web no se vea vacía en una demo.

## Qué carga

- 6 agencias ficticias: Autocity Belgrano, Norte Motors, Premium Hub, Rivadavia Cars, Punto Auto y Garage 21 (con emails `.example`, que no existen).
- 11 autos con precio, kilometraje, ficha y descripción. 6 quedan destacados, el BMW 320i reservado y el Fiat Cronos vendido.
- 5 fotos por auto, subidas a Cloudinary a través de la API (pasan por la misma validación que una carga desde el panel).

## Fotos y licencias

Las fotos son de Wikimedia Commons, con licencias libres (CC BY, CC BY-SA, CC0 o dominio público). La lista de cada archivo, su autor, licencia y página de origen está en [`docs/demo/CREDITOS-FOTOS.md`](../../docs/demo/CREDITOS-FOTOS.md).

Las licencias CC BY y CC BY-SA exigen mencionar al autor y la licencia cuando las fotos se muestran en público. Si la demo se publica, hay que mostrar esos créditos en la web (por ejemplo, un enlace en el pie de página).

## Cómo usarlo

Requiere Node 18 o superior y el backend corriendo con credenciales de Cloudinary.

```bash
# 1. Bajar las fotos (quedan en scripts/demo/fotos/, que no se versiona)
node scripts/demo/bajar-fotos.js

# 2. Cargar agencias, autos y fotos contra un backend
API=http://localhost:8080/api ADMIN_EMAIL=<email del admin> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```

Las agencias que ya existen con el mismo nombre se reutilizan. Los autos se crean siempre de nuevo, así que correrlo dos veces los duplica.

### Empezar de cero

Con `LIMPIAR=1`, antes de cargar **borra todos los autos y todas las agencias** del backend (salvo la agencia `dante-automotores`), junto con sus fotos en Cloudinary, favoritos y consultas. Usalo solo contra una base de prueba, nunca contra producción con datos reales.

```bash
LIMPIAR=1 API=http://localhost:8080/api ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```
