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
  { nombre: "Autocity Belgrano", direccion: "Av. Cabildo 2450, CABA", telefonoContacto: "011 4555-1234", emailContacto: "contacto@autocitybelgrano.example", descripcion: "Concesionaria multimarca con más de 15 años en el mercado. Financiación propia y garantía en todas nuestras unidades." },
  { nombre: "Norte Motors", direccion: "Av. del Libertador 15200, San Isidro", telefonoContacto: "011 4555-0202", emailContacto: "contacto@nortemotors.example", descripcion: "Especialistas en SUVs y pickups. Recibimos tu usado como parte de pago." },
  { nombre: "Premium Hub", direccion: "Av. Figueroa Alcorta 3500, CABA", telefonoContacto: "011 4555-0404", emailContacto: "ventas@premiumhub.example", descripcion: "Usados premium seleccionados, con historial de service verificado." },
  { nombre: "Rivadavia Cars", direccion: "Av. Rivadavia 7800, CABA", telefonoContacto: "011 4555-0505", emailContacto: "info@rivadaviacars.example", descripcion: "Autos seminuevos y 0 km con entrega inmediata." },
  { nombre: "Punto Auto", direccion: "Av. Maipú 1900, Vicente López", telefonoContacto: "011 4555-0606", emailContacto: "hola@puntoauto.example", descripcion: "Compra, venta y consignación de usados de alta gama." },
  { nombre: "Garage 21", direccion: "Av. Gaona 3100, Ramos Mejía", telefonoContacto: "011 4555-0707", emailContacto: "garage21@garage21.example", descripcion: "Hatchbacks y compactos para la ciudad, con financiación en cuotas." },
];
const desc = (m) => `${m} en excelente estado, único dueño y con todos los services al día en concesionario. Papeles al día, lista para transferir. Aceptamos tu usado como parte de pago.`;
const AUTOS = [
  ["Autocity Belgrano", "Toyota", "Corolla XEI", 2022, 42000, 24500000, "AUTOMATICA", "NAFTA", "Blanco", "MUY_BUENO", "corolla", true, "DISPONIBLE"],
  ["Norte Motors", "Jeep", "Compass Limited", 2023, 18500, 36800000, "AUTOMATICA", "NAFTA", "Gris", "EXCELENTE", "compass", true, "DISPONIBLE"],
  ["Premium Hub", "Volkswagen", "Golf GTI", 2021, 36200, 31200000, "AUTOMATICA", "NAFTA", "Gris", "MUY_BUENO", "golf", true, "DISPONIBLE"],
  ["Rivadavia Cars", "Ford", "Territory Titanium", 2024, 9800, 43900000, "AUTOMATICA", "NAFTA", "Negro", "EXCELENTE", "territory", true, "DISPONIBLE"],
  ["Punto Auto", "BMW", "320i Sport", 2020, 52000, 47500000, "AUTOMATICA", "NAFTA", "Negro", "MUY_BUENO", "bmw320", true, "RESERVADO"],
  ["Garage 21", "Peugeot", "208 Feline", 2023, 22100, 21900000, "MANUAL", "NAFTA", "Blanco", "EXCELENTE", "p208", true, "DISPONIBLE"],
  ["Autocity Belgrano", "Chevrolet", "Onix Premier", 2022, 28000, 19800000, "AUTOMATICA", "NAFTA", "Plata", "MUY_BUENO", "onix", false, "DISPONIBLE"],
  ["Autocity Belgrano", "Ford", "Ranger XLT", 2020, 65000, 42500000, "MANUAL", "DIESEL", "Rojo", "BUENO", "ranger", false, "DISPONIBLE"],
  ["Autocity Belgrano", "Fiat", "Cronos Drive", 2023, 12000, 17200000, "MANUAL", "NAFTA", "Rojo", "EXCELENTE", "cronos", false, "VENDIDO"],
  ["Autocity Belgrano", "Peugeot", "208 Feline", 2022, 31000, 21300000, "MANUAL", "NAFTA", "Gris", "MUY_BUENO", "p208", false, "DISPONIBLE"],
  ["Autocity Belgrano", "Renault", "Kangoo", 2019, 89000, 15600000, "MANUAL", "NAFTA", "Rojo", "BUENO", "kangoo", false, "DISPONIBLE"],
];
(async () => {
  T = (await (await fetch(B + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD }) })).json()).token;
  if (process.env.LIMPIAR === "1") {
    for (const p of await api("GET", "/admin/publicaciones")) await api("DELETE", "/publicaciones/" + p.id);
    for (const a of await api("GET", "/agencias")) if (a.slug !== "dante-automotores") await api("DELETE", "/agencias/" + a.id);
  }
  const ids = {};
  const existentes = await api("GET", "/agencias");
  for (const a of AGENCIAS) { const e = existentes.find(x => x.nombre === a.nombre); ids[a.nombre] = e ? e.id : (await api("POST", "/agencias", a)).id; }
  for (const [ag, marca, modelo, anio, km, precio, tr, comb, color, cond, fotos, dest, estado] of AUTOS) {
    const p = await api("POST", "/publicaciones", { agenciaId: ids[ag], marca, modelo, anio, kilometraje: km, precio, moneda: "ARS", transmision: tr, combustible: comb, color, condicion: cond, descripcion: desc(`${marca} ${modelo} ${anio}`) });
    for (let i = 1; i <= 5; i++) { const fd = new FormData(); fd.append("archivo", new Blob([fs.readFileSync(path.join(DIR, `${fotos}-${i}.jpg`))], { type: "image/jpeg" }), `${fotos}-${i}.jpg`); await api("POST", `/publicaciones/${p.id}/fotos`, fd, true); }
    if (dest) await api("PATCH", `/publicaciones/${p.id}/destacado`, { destacado: true });
    if (estado !== "DISPONIBLE") await api("PATCH", `/publicaciones/${p.id}/estado`, { estado });
    console.log("ok", p.id, marca, modelo, anio, "->", ag, dest ? "★" : "", estado);
  }
})().catch(e => { console.error(e.message); process.exit(1); });
