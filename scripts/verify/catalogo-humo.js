// Humo de la API publica del catalogo. Solo corre contra un back local (nunca contra produccion).
// Uso: API=http://localhost:8080/api node scripts/verify/catalogo-humo.js
// Los planes 02-03, 02-04 y 02-07 suman chequeos a este mismo script.
const API = process.env.API || "http://localhost:8080/api";

const host = (() => {
  try { return new URL(API).hostname; } catch { return ""; }
})();
if (host !== "localhost" && host !== "127.0.0.1") {
  console.error("el humo solo corre contra un back local; nunca contra producción");
  process.exit(2);
}

let ok = 0, fallas = 0, skip = 0;

class Salteado extends Error {}
const saltear = (motivo) => { throw new Salteado(motivo); };
const exigir = (condicion, detalle) => { if (!condicion) throw new Error(detalle); };

async function revisar(nombre, fn) {
  try {
    await fn();
    ok++;
    console.log(`OK ${nombre}`);
  } catch (e) {
    if (e instanceof Salteado) {
      skip++;
      console.log(`SKIP ${nombre}: ${e.message}`);
    } else {
      fallas++;
      console.log(`FALLA ${nombre}: ${e.message}`);
    }
  }
}

async function get(ruta) {
  const r = await fetch(API + ruta); // sin token: el catalogo es publico
  const texto = await r.text();
  let cuerpo = null;
  try { cuerpo = texto ? JSON.parse(texto) : null; } catch { /* queda null */ }
  return { estado: r.status, cuerpo };
}

(async () => {
  let destacados = [];

  await revisar("GET /publicaciones/destacados responde 200 con un array de hasta 6", async () => {
    const { estado, cuerpo } = await get("/publicaciones/destacados");
    exigir(estado === 200, `estado ${estado}`);
    exigir(Array.isArray(cuerpo), "la respuesta no es un array");
    exigir(cuerpo.length <= 6, `trae ${cuerpo.length} autos (maximo 6)`);
    destacados = cuerpo;
  });

  await revisar("destacados: ninguno vendido", async () => {
    if (destacados.length === 0) saltear("la lista viene vacia");
    const vendido = destacados.find((p) => p.estado === "VENDIDO");
    exigir(!vendido, `el auto ${vendido && vendido.id} esta VENDIDO`);
  });

  await revisar("destacados: cada item trae id, marca, modelo, precio, estado y fotoPortada", async () => {
    if (destacados.length === 0) saltear("la lista viene vacia");
    for (const p of destacados) {
      for (const clave of ["id", "marca", "modelo", "precio", "estado", "fotoPortada"]) {
        exigir(clave in p, `al auto ${p.id} le falta la clave ${clave}`);
      }
    }
  });

  await revisar("destacados: ningun item trae descripcion, fotos ni admin", async () => {
    if (destacados.length === 0) saltear("la lista viene vacia");
    for (const p of destacados) {
      for (const clave of ["descripcion", "fotos", "admin"]) {
        exigir(!(clave in p), `el auto ${p.id} expone la clave ${clave}`);
      }
    }
  });

  await revisar("GET /publicaciones/destacados?limite=50 trae como mucho 12", async () => {
    const { estado, cuerpo } = await get("/publicaciones/destacados?limite=50");
    exigir(estado === 200, `estado ${estado}`);
    exigir(Array.isArray(cuerpo), "la respuesta no es un array");
    exigir(cuerpo.length <= 12, `trae ${cuerpo.length} autos (maximo 12)`);
  });

  console.log(`humo: ${ok} ok, ${fallas} fallas, ${skip} skip`);
  // exitCode en vez de process.exit: en Windows, salir con fetch pendiente puede abortar Node con un assert de libuv.
  process.exitCode = fallas > 0 ? 1 : 0;
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
