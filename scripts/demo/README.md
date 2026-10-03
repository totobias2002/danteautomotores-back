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
| `DESHACER=1` | Deshace una corrida anterior: borra solo los autos y agencias registrados en su archivo de registro (ver más abajo). No se combina con `LIMPIAR=1`. |
| `REGISTRO` | Ruta alternativa del archivo de registro (por defecto `scripts/demo/registros/<host>.json`). |

**Correrlo dos veces ya no duplica:** antes de crear nada, el script busca en el backend autos de la demo (misma marca, modelo y año) y, si hay alguno, aborta y los lista. Las agencias que ya existen con el mismo nombre no se duplican ni se tocan: sus datos (dirección, teléfono, email, descripción, logo) quedan como están. Solo si la agencia existente no tiene zona se la completa con un `PUT` que reenvía sus propios datos más la zona de la demo; si ese `PUT` falla se avisa por consola y se sigue con la agencia tal como está. Las agencias existentes no se registran (no las creó el script).

**Contra producción:** antes de cargar la demo, la página `/creditos` del front tiene que estar publicada (licencias CC BY y CC BY-SA, ver arriba). Y nunca se usa `LIMPIAR=1`: borraría autos, fotos, consultas y favoritos reales. El registro de la carga (ver abajo) queda en la máquina de quien la corrió: conviene no perderlo hasta decidir si la demo se queda.

### Registro de la corrida y cómo deshacerla

A medida que crea agencias y autos, el script anota sus ids en `scripts/demo/registros/<host>_<puerto>.json` (por ejemplo `localhost_8080.json`; la carpeta `registros/` no se versiona). El archivo se reescribe después de cada creación exitosa (cada auto apenas se crea, antes de subir sus fotos), así que una corrida cortada a la mitad igual queda registrada. Contiene la URL de la API, la fecha de inicio, si la corrida terminó (`completa`) y, por cada agencia creada, su id y nombre, y por cada auto, su id, marca, modelo y año. **No guarda el email, la contraseña ni el token del admin.**

Mientras exista ese registro, una corrida nueva contra el mismo backend se niega a empezar (para no mezclar dos cargas): primero hay que deshacer la anterior o, si se quiere conservarla, borrar el archivo.

Para deshacer una corrida (completa o cortada a la mitad):

```bash
DESHACER=1 API=http://localhost:8080/api ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```

Antes de borrar verifica, sin hacer ningún request, que el registro exista y sea del mismo backend (`API`). Después borra primero los autos y luego las agencias, y **solo los ids registrados**, y solo si siguen siendo lo mismo que se creó (mismo id con la misma marca, modelo y año, o el mismo nombre de agencia). Lo que ya no está se informa y se saltea; lo que tiene el mismo id pero otros datos no se borra. Si algún borrado falla (por ejemplo, una agencia que ahora tiene otros autos) se sigue con el resto. Si no queda nada pendiente, borra el archivo de registro; si queda algo, lo reescribe solo con lo pendiente y termina con código de error. Contra un backend que no sea `localhost` o `127.0.0.1` exige `CONFIRMAR_BORRADO_EN_PRODUCCION=SI` (aunque solo se borre lo registrado).

### Empezar de cero

Con `LIMPIAR=1`, antes de cargar **borra todos los autos y todas las agencias** del backend (salvo la agencia `dante-automotores`), junto con sus fotos en Cloudinary, favoritos y consultas. Usalo solo contra una base de prueba, nunca contra producción con datos reales. El script se niega a usar `LIMPIAR=1` contra un backend que no sea `localhost` o `127.0.0.1` (lo chequea antes de hacer cualquier request) salvo que se defina `CONFIRMAR_BORRADO_EN_PRODUCCION=SI`.

```bash
LIMPIAR=1 API=http://localhost:8080/api ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js
```
