const fs = require("fs");
const path = require("path");
const DIR = path.join(__dirname, "fotos");
const B = process.env.API || "http://localhost:8080/api";
let T;
const api = async (m, p, body, isForm) => {
  const h = { Authorization: "Bearer " + T }; if (body && !isForm) h["Content-Type"] = "application/json; charset=utf-8";
  const r = await fetch(B + p, { method: m, headers: h, body: isForm ? body : body ? JSON.stringify(body) : undefined });
  const t = await r.text(); if (!r.ok) throw new Error(`${m} ${p} -> ${r.status} ${t}`); return t ? JSON.parse(t) : null;
};
const AGENCIAS = [
  { nombre: "Autocity Belgrano", zona: "CABA", direccion: "Av. Cabildo 2450, CABA", telefonoContacto: "011 4555-1234", emailContacto: "contacto@autocitybelgrano.example", descripcion: "Concesionaria multimarca con más de 15 años en el mercado. Financiación propia y garantía en todas nuestras unidades." },
  { nombre: "Norte Motors", zona: "ZONA_NORTE", direccion: "Av. del Libertador 15200, San Isidro", telefonoContacto: "011 4555-0202", emailContacto: "contacto@nortemotors.example", descripcion: "Especialistas en SUVs y pickups. Recibimos tu usado como parte de pago." },
  { nombre: "Premium Hub", zona: "CABA", direccion: "Av. Figueroa Alcorta 3500, CABA", telefonoContacto: "011 4555-0404", emailContacto: "ventas@premiumhub.example", descripcion: "Usados premium seleccionados, con historial de service verificado." },
  { nombre: "Rivadavia Cars", zona: "CABA", direccion: "Av. Rivadavia 7800, CABA", telefonoContacto: "011 4555-0505", emailContacto: "info@rivadaviacars.example", descripcion: "Autos seminuevos y 0 km con entrega inmediata." },
  { nombre: "Punto Auto", zona: "ZONA_NORTE", direccion: "Av. Maipú 1900, Vicente López", telefonoContacto: "011 4555-0606", emailContacto: "hola@puntoauto.example", descripcion: "Compra, venta y consignación de usados de alta gama." },
  { nombre: "Garage 21", zona: "ZONA_OESTE", direccion: "Av. Gaona 3100, Ramos Mejía", telefonoContacto: "011 4555-0707", emailContacto: "garage21@garage21.example", descripcion: "Hatchbacks y compactos para la ciudad, con financiación en cuotas." },
];
const desc = (m) => `${m} en excelente estado, único dueño y con todos los services al día en concesionario. Papeles al día, lista para transferir. Aceptamos tu usado como parte de pago.`;
// Filas: [agencia, marca, modelo, anio, km, precio, transmision, combustible, color, condicion, fotos, destacado, estado, tipoCarroceria, precioAnterior]
// precioAnterior (3 autos) es el precio de lista antes de la rebaja: alimenta el filtro de ofertas.
const AUTOS = [
  ["Autocity Belgrano", "Toyota", "Corolla XEI", 2022, 42000, 24500000, "AUTOMATICA", "NAFTA", "Blanco", "MUY_BUENO", "corolla", true, "DISPONIBLE", "SEDAN", null],
  ["Norte Motors", "Jeep", "Compass Limited", 2023, 18500, 36800000, "AUTOMATICA", "NAFTA", "Gris", "EXCELENTE", "compass", true, "DISPONIBLE", "SUV", null],
  ["Premium Hub", "Volkswagen", "Golf GTI", 2021, 36200, 31200000, "AUTOMATICA", "NAFTA", "Gris", "MUY_BUENO", "golf", true, "DISPONIBLE", "HATCHBACK", null],
  ["Rivadavia Cars", "Ford", "Territory Titanium", 2024, 9800, 43900000, "AUTOMATICA", "NAFTA", "Negro", "EXCELENTE", "territory", true, "DISPONIBLE", "SUV", null],
  ["Punto Auto", "BMW", "320i Sport", 2020, 52000, 47500000, "AUTOMATICA", "NAFTA", "Negro", "MUY_BUENO", "bmw320", true, "RESERVADO", "SEDAN", null],
  ["Garage 21", "Peugeot", "208 Feline", 2023, 22100, 21900000, "MANUAL", "NAFTA", "Blanco", "EXCELENTE", "p208", true, "DISPONIBLE", "HATCHBACK", 23900000],
  ["Autocity Belgrano", "Chevrolet", "Onix Premier", 2022, 28000, 19800000, "AUTOMATICA", "NAFTA", "Plata", "MUY_BUENO", "onix", false, "DISPONIBLE", "SEDAN", 21500000],
  ["Autocity Belgrano", "Ford", "Ranger XLT", 2020, 65000, 42500000, "MANUAL", "DIESEL", "Rojo", "BUENO", "ranger", false, "DISPONIBLE", "PICKUP", 44900000],
  ["Autocity Belgrano", "Fiat", "Cronos Drive", 2023, 12000, 17200000, "MANUAL", "NAFTA", "Rojo", "EXCELENTE", "cronos", false, "VENDIDO", "SEDAN", null],
  ["Autocity Belgrano", "Peugeot", "208 Feline", 2022, 31000, 21300000, "MANUAL", "NAFTA", "Gris", "MUY_BUENO", "p208", false, "DISPONIBLE", "HATCHBACK", null],
  ["Autocity Belgrano", "Renault", "Kangoo", 2019, 89000, 15600000, "MANUAL", "NAFTA", "Rojo", "BUENO", "kangoo", false, "DISPONIBLE", "UTILITARIO", null],
];
const esLocal = (() => { try { const h = new URL(B).hostname; return h === "localhost" || h === "127.0.0.1"; } catch { return false; } })();
const clave = (marca, modelo, anio) => `${marca} ${modelo} ${anio}`.toLowerCase();
const sinBarras = (u) => String(u).replace(/\/+$/, "");

// Una agencia existente con el mismo nombre NO se sobrescribe con los datos de ejemplo (puede ser una agencia real).
// Solo si no tiene zona se la completa con un PUT que reenvía sus propios datos (el PUT reemplaza todos los campos
// de AgenciaRequest, lo que no se manda queda en null). Devuelve null si no hay nada que completar.
function cuerpoParaCompletarZona(existente, demo) {
  if (!demo.zona || existente.zona) return null;
  const { nombre, logo, descripcion, direccion, telefonoContacto, emailContacto } = existente;
  return { nombre, logo: logo ?? null, descripcion: descripcion ?? null, direccion: direccion ?? null, telefonoContacto: telefonoContacto ?? null, emailContacto, zona: demo.zona };
}

// Clasifica lo registrado por una corrida contra lo que hay hoy en el backend: borrar (sigue ahí y los datos coinciden),
// yaNoEstan (alguien lo borró) y noCoinciden (el id existe pero ahora es otra cosa: nunca se toca).
function planDeDeshacer(registro, publicacionesActuales, agenciasActuales) {
  const clasificar = (registrados, actuales, iguales) => {
    const plan = { borrar: [], yaNoEstan: [], noCoinciden: [] };
    for (const r of registrados || []) {
      const actual = (actuales || []).find((x) => x.id === r.id);
      if (!actual) plan.yaNoEstan.push(r);
      else if (iguales(r, actual)) plan.borrar.push(r);
      else plan.noCoinciden.push(r);
    }
    return plan;
  };
  return {
    publicaciones: clasificar(registro.publicaciones, publicacionesActuales, (r, a) => r.marca === a.marca && r.modelo === a.modelo && r.anio === a.anio),
    agencias: clasificar(registro.agencias, agenciasActuales, (r, a) => r.nombre === a.nombre),
  };
}

// Un registro por backend: scripts/demo/registros/<host>.json (host con puerto, saneado a [a-z0-9.-]), o REGISTRO.
function rutaDelRegistro(api, env) {
  if (env.REGISTRO) return env.REGISTRO;
  let host;
  try { host = new URL(api).host; } catch { host = String(api); }
  return path.join(__dirname, "registros", host.toLowerCase().replace(/[^a-z0-9.-]/g, "_") + ".json");
}

async function main() {
  const RUTA = rutaDelRegistro(B, process.env);
  const deshacer = process.env.DESHACER === "1";
  // Guardas: corren antes de cualquier request.
  if (!process.env.ADMIN_EMAIL || !process.env.ADMIN_PASSWORD) { console.error("Faltan ADMIN_EMAIL o ADMIN_PASSWORD"); process.exit(1); }
  if (deshacer && process.env.LIMPIAR === "1") { console.error("DESHACER=1 no se combina con LIMPIAR=1: uno borra solo lo registrado y el otro borra todo."); process.exit(1); }
  if (process.env.LIMPIAR === "1" && !esLocal && process.env.CONFIRMAR_BORRADO_EN_PRODUCCION !== "SI") {
    console.error(`LIMPIAR=1 borra todos los autos y agencias del backend (${B}), que no es local. Para confirmarlo definí CONFIRMAR_BORRADO_EN_PRODUCCION=SI. Contra producción no se usa LIMPIAR.`);
    process.exit(1);
  }
  if (deshacer && !esLocal && process.env.CONFIRMAR_BORRADO_EN_PRODUCCION !== "SI") {
    console.error(`DESHACER=1 borra autos y agencias del backend (${B}), que no es local. Solo se borra lo registrado en la corrida, pero para confirmarlo definí CONFIRMAR_BORRADO_EN_PRODUCCION=SI.`);
    process.exit(1);
  }
  let registroPrevio;
  if (deshacer) {
    if (!fs.existsSync(RUTA)) { console.error(`No hay registro de corrida en ${RUTA}: no hay nada que deshacer.`); process.exit(1); }
    try { registroPrevio = JSON.parse(fs.readFileSync(RUTA, "utf8")); } catch { console.error(`No se pudo leer el registro ${RUTA} (JSON inválido).`); process.exit(1); }
    if (sinBarras(registroPrevio.api ?? "") !== sinBarras(B)) { console.error(`El registro ${RUTA} es de otro backend (${registroPrevio.api}): no se borra nada.`); process.exit(1); }
  } else if (fs.existsSync(RUTA)) {
    console.error(`Ya hay un registro de una corrida anterior contra este backend en ${RUTA}. Para deshacer esa carga corré con DESHACER=1; si querés conservarla, borrá ese archivo.`);
    process.exit(1);
  }
  const rl = await fetch(B + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD }) });
  const login = rl.ok ? await rl.json().catch(() => ({})) : {};
  if (!login.token) { console.error(`No se pudo iniciar sesión como admin (status ${rl.status})`); process.exitCode = 1; return; }
  T = login.token;
  if (deshacer) { await deshacerCorrida(RUTA, registroPrevio); return; }
  if (process.env.LIMPIAR === "1") {
    for (const p of await api("GET", "/admin/publicaciones")) await api("DELETE", "/publicaciones/" + p.id);
    for (const a of await api("GET", "/agencias")) if (a.slug !== "dante-automotores") await api("DELETE", "/agencias/" + a.id);
  }
  // Idempotencia: si ya hay autos de la demo (misma marca, modelo y año) no se crea ni se modifica nada, salvo FORZAR=1.
  // Cubre también una corrida interrumpida: se deshace con DESHACER=1 (o se borran del panel los autos que quedaron) y se vuelve a correr.
  const claves = new Set(AUTOS.map(([, marca, modelo, anio]) => clave(marca, modelo, anio)));
  const repetidos = (await api("GET", "/admin/publicaciones")).filter(p => claves.has(clave(p.marca, p.modelo, p.anio)));
  if (repetidos.length && process.env.FORZAR !== "1") {
    console.error(["Ya existen autos de la demo:", ...repetidos.map(p => `  #${p.id} ${p.marca} ${p.modelo} ${p.anio}`), "Borralos desde el panel o corré con FORZAR=1 si querés duplicarlos"].join("\n"));
    process.exitCode = 1; return; // exitCode y no exit(): en Windows exit() con fetch pendientes aborta Node
  }
  // Registro de lo que crea esta corrida (solo ids y datos de identificación; nunca credenciales ni token). Se reescribe
  // completo después de cada POST exitoso, así una corrida cortada a la mitad igual se puede deshacer.
  const registro = { api: B, inicio: new Date().toISOString(), completa: false, agencias: [], publicaciones: [] };
  const guardar = () => { fs.mkdirSync(path.dirname(RUTA), { recursive: true }); fs.writeFileSync(RUTA, JSON.stringify(registro, null, 2) + "\n"); };
  guardar();
  const ids = {};
  const existentes = await api("GET", "/agencias");
  for (const a of AGENCIAS) {
    const e = existentes.find(x => x.nombre === a.nombre);
    if (e) {
      // Una agencia existente no se sobrescribe ni se registra (no la creó el script): solo se le completa la zona vacía.
      ids[a.nombre] = e.id;
      const cuerpo = cuerpoParaCompletarZona(e, a);
      if (!cuerpo) { console.log("agencia existente, sin cambios:", a.nombre); continue; }
      try { await api("PUT", "/agencias/" + e.id, cuerpo); console.log("agencia existente, zona completada:", a.nombre); }
      catch (err) { console.warn(`agencia existente, la zona no se pudo completar (${a.nombre}): ${err.message}`); }
    } else {
      const nueva = await api("POST", "/agencias", a);
      ids[a.nombre] = nueva.id;
      registro.agencias.push({ id: nueva.id, nombre: nueva.nombre ?? a.nombre }); guardar();
    }
  }
  for (const [ag, marca, modelo, anio, km, precio, tr, comb, color, cond, fotos, dest, estado, tipoCarroceria, precioAnterior] of AUTOS) {
    const p = await api("POST", "/publicaciones", { agenciaId: ids[ag], marca, modelo, anio, kilometraje: km, precio, precioAnterior, tipoCarroceria, moneda: "ARS", transmision: tr, combustible: comb, color, condicion: cond, descripcion: desc(`${marca} ${modelo} ${anio}`) });
    // Se registra antes de subir las fotos: un corte durante las fotos igual deja el auto registrado.
    registro.publicaciones.push({ id: p.id, marca: p.marca ?? marca, modelo: p.modelo ?? modelo, anio: p.anio ?? anio }); guardar();
    for (let i = 1; i <= 5; i++) { const fd = new FormData(); fd.append("archivo", new Blob([fs.readFileSync(path.join(DIR, `${fotos}-${i}.jpg`))], { type: "image/jpeg" }), `${fotos}-${i}.jpg`); await api("POST", `/publicaciones/${p.id}/fotos`, fd, true); }
    if (dest) await api("PATCH", `/publicaciones/${p.id}/destacado`, { destacado: true });
    if (estado !== "DISPONIBLE") await api("PATCH", `/publicaciones/${p.id}/estado`, { estado });
    console.log("ok", p.id, marca, modelo, anio, tipoCarroceria, precioAnterior ? "oferta" : "", "->", ag, dest ? "★" : "", estado);
  }
  registro.completa = true; guardar();
  const reg = process.env.REGISTRO ? ` REGISTRO=${RUTA}` : "";
  console.log(`Registro de la corrida: ${RUTA}\nPara deshacerla: DESHACER=1 API=${B}${reg} ADMIN_EMAIL=<email> ADMIN_PASSWORD=<clave> node scripts/demo/sembrar-demo.js`);
}

// Borra solo lo registrado (primero los autos, después las agencias) y solo si el id sigue siendo lo mismo que se creó.
async function deshacerCorrida(ruta, registro) {
  const plan = planDeDeshacer(registro, await api("GET", "/admin/publicaciones"), await api("GET", "/agencias"));
  const fallidos = { publicaciones: [], agencias: [] };
  const borrar = async (grupo, ruta_, etiqueta, descripcion) => {
    for (const r of plan[grupo].borrar) {
      try { await api("DELETE", `${ruta_}/${r.id}`); console.log(`borrado ${etiqueta} #${r.id} ${descripcion(r)}`); }
      catch (e) { fallidos[grupo].push(r); console.error(`FALLÓ borrar ${etiqueta} #${r.id} ${descripcion(r)}: ${e.message}`); }
    }
    for (const r of plan[grupo].yaNoEstan) console.log(`ya no estaba ${etiqueta} #${r.id} ${descripcion(r)}`);
    for (const r of plan[grupo].noCoinciden) console.warn(`NO coincide ${etiqueta} #${r.id} ${descripcion(r)}: ahora es otro dato, no se borra`);
  };
  await borrar("publicaciones", "/publicaciones", "auto", (r) => `${r.marca} ${r.modelo} ${r.anio}`);
  await borrar("agencias", "/agencias", "agencia", (r) => r.nombre);
  const pendientes = {
    publicaciones: [...plan.publicaciones.noCoinciden, ...fallidos.publicaciones],
    agencias: [...plan.agencias.noCoinciden, ...fallidos.agencias],
  };
  if (!pendientes.publicaciones.length && !pendientes.agencias.length) {
    fs.unlinkSync(ruta);
    console.log(`Corrida deshecha; registro borrado (${ruta}).`);
    return;
  }
  fs.writeFileSync(ruta, JSON.stringify({ ...registro, ...pendientes }, null, 2) + "\n");
  console.error(`Quedaron ${pendientes.publicaciones.length} autos y ${pendientes.agencias.length} agencias pendientes en ${ruta}.`);
  process.exitCode = 1;
}

module.exports = { cuerpoParaCompletarZona, planDeDeshacer, rutaDelRegistro };

if (require.main === module) main().catch(e => { console.error(e.message); process.exit(1); });
