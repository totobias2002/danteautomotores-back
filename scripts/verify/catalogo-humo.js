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

  // Plan 02-03: campos nuevos del contrato (tipo, precio anterior, oferta, zona de la agencia y fecha de venta).
  await revisar("destacados: cada item trae tipoCarroceria, precioAnterior, oferta, agenciaZona y fechaVendido", async () => {
    if (destacados.length === 0) saltear("la lista viene vacia");
    for (const p of destacados) {
      for (const clave of ["tipoCarroceria", "precioAnterior", "oferta", "agenciaZona", "fechaVendido"]) {
        exigir(clave in p, `al auto ${p.id} le falta la clave ${clave}`);
      }
      exigir(typeof p.oferta === "boolean", `oferta del auto ${p.id} no es booleana`);
      const esOferta = p.precioAnterior != null && Number(p.precioAnterior) > Number(p.precio);
      exigir(p.oferta === esOferta, `el auto ${p.id} dice oferta=${p.oferta} con precio ${p.precio} y anterior ${p.precioAnterior}`);
    }
  });

  await revisar("GET /agencias trae la clave zona en cada agencia", async () => {
    const { estado, cuerpo } = await get("/agencias");
    exigir(estado === 200, `estado ${estado}`);
    exigir(Array.isArray(cuerpo), "la respuesta no es un array");
    if (cuerpo.length === 0) saltear("no hay agencias");
    for (const a of cuerpo) exigir("zona" in a, `a la agencia ${a.id} le falta la clave zona`);
  });

  // Plan 02-04: listado paginado y facetas.
  let pagina = null;

  await revisar("GET /publicaciones responde una pagina con la forma {contenido, pagina, tamanio, totalElementos, totalPaginas}", async () => {
    const { estado, cuerpo } = await get("/publicaciones");
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo && !Array.isArray(cuerpo), "la respuesta no es un objeto de pagina");
    for (const clave of ["contenido", "pagina", "tamanio", "totalElementos", "totalPaginas"]) {
      exigir(clave in cuerpo, `falta la clave ${clave}`);
    }
    exigir(Array.isArray(cuerpo.contenido), "contenido no es un array");
    exigir(cuerpo.contenido.length <= 24, `trae ${cuerpo.contenido.length} autos (maximo 24)`);
    exigir(cuerpo.tamanio === 24, `tamanio ${cuerpo.tamanio} (debe ser 24)`);
    exigir(cuerpo.pagina === 1, `pagina ${cuerpo.pagina} (debe ser 1)`);
    pagina = cuerpo;
  });

  await revisar("listado: ningun vendido antes de un no vendido", async () => {
    if (!pagina || pagina.contenido.length === 0) saltear("el catalogo esta vacio");
    let vistoVendido = false;
    for (const p of pagina.contenido) {
      if (p.estado === "VENDIDO") vistoVendido = true;
      else exigir(!vistoVendido, `el auto ${p.id} (${p.estado}) aparece despues de un vendido`);
    }
  });

  await revisar("listado: cada item es el resumen publico (sin descripcion, fotos ni admin)", async () => {
    if (!pagina || pagina.contenido.length === 0) saltear("el catalogo esta vacio");
    for (const p of pagina.contenido) {
      for (const clave of ["id", "marca", "modelo", "precio", "estado", "fotoPortada", "oferta"]) {
        exigir(clave in p, `al auto ${p.id} le falta la clave ${clave}`);
      }
      for (const clave of ["descripcion", "fotos", "admin"]) {
        exigir(!(clave in p), `el auto ${p.id} expone la clave ${clave}`);
      }
    }
  });

  await revisar("GET /publicaciones?pagina=999 devuelve contenido vacio", async () => {
    const { estado, cuerpo } = await get("/publicaciones?pagina=999");
    exigir(estado === 200, `estado ${estado}`);
    exigir(Array.isArray(cuerpo.contenido) && cuerpo.contenido.length === 0, "contenido no esta vacio");
  });

  await revisar("GET /publicaciones?size=1000&sort=admin.email no cambia el tamano ni rompe", async () => {
    const { estado, cuerpo } = await get("/publicaciones?size=1000&sort=admin.email");
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo.tamanio === 24 && cuerpo.contenido.length <= 24, "el cliente pudo cambiar el tamano");
  });

  await revisar("GET /publicaciones?tipo=NAVE responde 400 con error", async () => {
    const { estado, cuerpo } = await get("/publicaciones?tipo=NAVE");
    exigir(estado === 400, `estado ${estado}`);
    exigir(cuerpo && typeof cuerpo.error === "string", "la respuesta no trae error");
  });

  await revisar("listado: marca y precioMax se aplican en el servidor", async () => {
    if (!pagina || pagina.contenido.length === 0) saltear("el catalogo esta vacio");
    const marca = pagina.contenido[0].marca;
    const filtrado = await get(`/publicaciones?marca=${encodeURIComponent(marca)}`);
    exigir(filtrado.estado === 200, `estado ${filtrado.estado}`);
    exigir(filtrado.cuerpo.contenido.length > 0, "el filtro por marca no trae el auto que existe");
    for (const p of filtrado.cuerpo.contenido) {
      exigir(p.marca.toLowerCase() === marca.toLowerCase(), `trajo ${p.marca} con marca=${marca}`);
    }
    const imposible = await get("/publicaciones?precioMax=1");
    exigir(imposible.cuerpo.totalElementos === 0, "precioMax=1 deberia dejar la pagina vacia");
  });

  await revisar("GET /publicaciones/facetas trae opciones, rangos y un histograma de 16 tramos (o 1 si min = max)", async () => {
    const { estado, cuerpo } = await get("/publicaciones/facetas");
    exigir(estado === 200, `estado ${estado}`);
    for (const clave of ["marcas", "modelos", "tipos", "zonas", "colores", "transmisiones", "estados"]) {
      exigir(Array.isArray(cuerpo[clave]), `${clave} no es un array`);
    }
    exigir(cuerpo.anio && "min" in cuerpo.anio && "max" in cuerpo.anio, "falta el rango de anio");
    exigir(cuerpo.kilometraje && "min" in cuerpo.kilometraje && "max" in cuerpo.kilometraje, "falta el rango de kilometraje");
    if (cuerpo.precio === null) {
      exigir(cuerpo.marcas.length === 0, "sin precio pero con marcas");
      saltear("el catalogo esta vacio: no hay precio ni histograma");
    }
    const { min, max, histograma } = cuerpo.precio;
    exigir(Array.isArray(histograma), "histograma no es un array");
    exigir(histograma.length === (Number(min) === Number(max) ? 1 : 16), `histograma con ${histograma.length} tramos`);
    const suma = histograma.reduce((acc, t) => acc + t.cantidad, 0);
    const total = cuerpo.marcas.reduce((acc, m) => acc + m.cantidad, 0);
    exigir(suma === total, `el histograma suma ${suma} y las marcas ${total}`);
  });

  await revisar("facetas: la suma de marcas coincide con el total del listado visible", async () => {
    const facetas = await get("/publicaciones/facetas");
    const listado = await get("/publicaciones");
    const total = facetas.cuerpo.marcas.reduce((acc, m) => acc + m.cantidad, 0);
    exigir(total === listado.cuerpo.totalElementos, `facetas ${total} vs listado ${listado.cuerpo.totalElementos}`);
  });

  await revisar("GET /publicaciones/facetas?agenciaId=999999 no trae autos", async () => {
    const { estado, cuerpo } = await get("/publicaciones/facetas?agenciaId=999999");
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo.marcas.length === 0 && cuerpo.precio === null, "una agencia inexistente trae datos");
  });

  console.log(`humo: ${ok} ok, ${fallas} fallas, ${skip} skip`);
  // exitCode en vez de process.exit: en Windows, salir con fetch pendiente puede abortar Node con un assert de libuv.
  process.exitCode = fallas > 0 ? 1 : 0;
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
