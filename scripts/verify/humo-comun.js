// Helpers reutilizables de los humos (Fase 4 en adelante). Salen de cuentas-humo.js, que no se modifica.
// Solo corren contra un back local (nunca contra produccion): se levantan con scripts/verify/con-back-local.sh, que exporta
// API y LOG_BACK (el log del back, donde en desarrollo se escriben los mails). Todo dato es de prueba y claramente falso
// (mails @dante.test). Nada de lo que se imprime incluye tokens ni contrasenas.
const fs = require("node:fs");

const API = process.env.API || "http://localhost:8080/api";
const LOG_BACK = process.env.LOG_BACK || "";

const host = (() => {
  try { return new URL(API).hostname; } catch { return ""; }
})();
if (host !== "localhost" && host !== "127.0.0.1") {
  console.error("el humo solo corre contra un back local; nunca contra producción");
  process.exit(2);
}

const contadores = { ok: 0, fallas: 0, skip: 0 };

class Salteado extends Error {}
const saltear = (motivo) => { throw new Salteado(motivo); };
const exigir = (condicion, detalle) => { if (!condicion) throw new Error(detalle); };
const dormir = (ms) => new Promise((resolver) => setTimeout(resolver, ms));

async function revisar(nombre, fn) {
  try {
    await fn();
    contadores.ok++;
    console.log(`OK ${nombre}`);
  } catch (e) {
    if (e instanceof Salteado) {
      contadores.skip++;
      console.log(`SKIP ${nombre}: ${e.message}`);
    } else {
      contadores.fallas++;
      console.log(`FALLA ${nombre}: ${e.message}`);
    }
  }
}

async function pedir(metodo, ruta, cuerpo, token) {
  const headers = {};
  if (cuerpo !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(API + ruta, {
    method: metodo,
    headers,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  const texto = await r.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* queda null */ }
  return { estado: r.status, cuerpo: json, texto, reintentarEn: r.headers.get("retry-after") };
}

const post = (ruta, cuerpo, token) => pedir("POST", ruta, cuerpo, token);

// ---- Log del back (mails de desarrollo) ----

function exigirLog() {
  if (!LOG_BACK) saltear("LOG_BACK no está definida (correr con scripts/verify/con-back-local.sh)");
}

// latin1: el log de Java en Windows puede no ser UTF-8 y solo se buscan cadenas ASCII (links, destinatarios).
function leerLog() {
  return fs.readFileSync(LOG_BACK, "latin1");
}

// El envio es asincrono: se reintenta hasta unos 10 segundos. Se acepta una linea completa (el link va seguido de " | ").
async function esperarLink(destinatario, ruta) {
  exigirLog();
  const patron = new RegExp(`${ruta}\\?token=([^\\s|&]+)\\s\\|`);
  const limite = Date.now() + 10000;
  for (;;) {
    const lineas = leerLog().split(/\r?\n/).filter((l) => l.includes(`para=${destinatario} `) && patron.test(l));
    if (lineas.length > 0) return decodeURIComponent(patron.exec(lineas[lineas.length - 1])[1]);
    if (Date.now() > limite) throw new Error(`no apareció en el log el mail con el link ${ruta} para el destinatario`);
    await dormir(250);
  }
}

// ---- Cuentas ----

function sufijoUnico() {
  return `${Date.now()}${Math.floor(Math.random() * 1000)}`;
}

// DNI de 8 digitos sin cero inicial (formato de V5), aleatorio para que dos corridas no choquen.
function dniAleatorio() {
  return String(Math.floor(10000000 + Math.random() * 89999999));
}

// Registra una cuenta con datos falsos, confirma el mail con el link que sale en el log, inicia sesion y la devuelve
// lista para operar (cuenta verificada). Devuelve email, contrasena, dni y token.
async function registrarCuentaVerificada(prefijo) {
  const sufijo = sufijoUnico();
  const email = `${prefijo}.${sufijo}@dante.test`;
  const password = `Humo-${sufijo}-x`;
  const dni = dniAleatorio();
  const registro = await post("/auth/registro", {
    nombre: "Humo",
    apellido: "Prueba",
    telefono: "011 15 1234-5678",
    dni,
    email,
    password,
  });
  exigir(registro.estado === 200, `registro de ${prefijo}: estado ${registro.estado}: ${registro.texto}`);
  const tokenDeConfirmacion = await esperarLink(email, "/confirmar-email");
  const confirmacion = await post("/auth/confirmar-email", { token: tokenDeConfirmacion });
  exigir(confirmacion.estado === 200, `confirmar mail de ${prefijo}: estado ${confirmacion.estado}`);
  const login = await post("/auth/login", { email, password });
  exigir(login.estado === 200 && login.cuerpo && login.cuerpo.token, `login de ${prefijo}: estado ${login.estado}`);
  return { email, password, dni, token: login.cuerpo.token };
}

// Inicia sesion con la cuenta admin que el back siembra a partir de ADMIN_EMAIL y ADMIN_PASSWORD. Nunca imprime la clave.
async function iniciarSesionAdmin() {
  const email = process.env.ADMIN_EMAIL;
  const password = process.env.ADMIN_PASSWORD;
  if (!email || !password) {
    console.error("humo: faltan ADMIN_EMAIL y ADMIN_PASSWORD en el entorno (son las mismas con las que se levanta el back local)");
    process.exit(2);
  }
  const login = await post("/auth/login", { email: email.trim().toLowerCase(), password });
  exigir(login.estado === 200 && login.cuerpo && login.cuerpo.token, `login del admin: estado ${login.estado}`);
  return { email, token: login.cuerpo.token };
}

// Imprime la linea final y fija el codigo de salida. exitCode en vez de process.exit: en Windows, salir con fetch
// pendiente puede abortar Node con un assert de libuv.
function cerrar() {
  console.log(`humo: ${contadores.ok} ok, ${contadores.fallas} fallas, ${contadores.skip} skip`);
  process.exitCode = contadores.fallas > 0 ? 1 : 0;
}

module.exports = {
  API,
  LOG_BACK,
  contadores,
  saltear,
  exigir,
  dormir,
  revisar,
  pedir,
  post,
  exigirLog,
  leerLog,
  esperarLink,
  sufijoUnico,
  dniAleatorio,
  registrarCuentaVerificada,
  iniciarSesionAdmin,
  cerrar,
};
