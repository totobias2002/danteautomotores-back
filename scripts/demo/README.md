# Carga de datos de demo

Carga agencias y autos de ejemplo (los mismos del catálogo mock del front), cada auto con 5 fotos reales de su modelo, para que la web no se vea vacía en una demo.

## Qué carga

- 6 agencias ficticias: Autocity Belgrano, Norte Motors, Premium Hub, Rivadavia Cars, Punto Auto y Garage 21 (con emails `.example`, que no existen), cada una con su zona (CABA, Zona Norte u Oeste).
- 11 autos con precio, kilometraje, ficha, descripción y tipo de carrocería (sedán, SUV, hatchback, pickup, utilitario). 3 traen además un precio anterior (Onix Premier, 208 Feline 2023 de Garage 21 y Ranger XLT), para que el filtro de ofertas tenga resultados. 6 quedan destacados, el BMW 320i reservado y el Fiat Cronos vendido.
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

Variables:

| Variable | Qué hace |
|---|---|
| `API` | URL base del backend (por defecto `http://localhost:8080/api`). |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Obligatorias: el script sale con error si faltan o si el login falla. |
| `LIMPIAR=1` | Borra todo antes de cargar (ver más abajo). |
| `CONFIRMAR_BORRADO_EN_PRODUCCION=SI` | Única forma de permitir `LIMPIAR=1` contra un backend que no sea localhost/127.0.0.1. |
| `FORZAR=1` | Carga los autos aunque ya existan los de la demo (los duplica). |

**Correrlo dos veces ya no duplica:** antes de crear nada, el script busca en el backend autos de la demo (misma marca, modelo y año) y, si hay alguno, aborta y los lista. Las agencias que ya existen con el mismo nombre no se duplican: se les completa la zona y el resto de sus datos con un `PUT`.

**Si una corrida se corta a la mitad** quedan autos parciales (sin todas las fotos). Borralos desde el panel de admin y volvé a correr el script; sin eso, abortaría por considerar que la demo ya está cargada.

**Contra producción:** antes de cargar la demo, la página `/creditos` del front tiene que estar publicada (licencias CC BY y CC BY-SA, ver arriba). Y nunca se usa `LIMPIAR=1`: borraría autos, fotos, consultas y favoritos reales.

### Empezar de cero

Con `LIMPIAR=1`, antes de cargar **borra todos los autos y todas las agencias** del backend (salvo la agencia `dante-automotores`), junto con sus fotos en Cloudinary, favoritos y consultas. Usalo solo contra una base de prueba, nunca contra producción con datos reales. El script se niega a usar `LIMPIAR=1` contra un backend que no sea `localhost` o `127.0.0.1` (lo chequea antes de hacer cualquier request) salvo que se defina `CONFIRMAR_BORRADO_EN_PRODUCCION=SI`.

```bash
LIMPIAR=1 API=http://localhost:8080/api ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```
