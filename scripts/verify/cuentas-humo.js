// Humo de las cuentas verificadas (Fase 3), de punta a punta. Solo corre contra un back local (nunca contra produccion).
// Uso: bash scripts/verify/con-back-local.sh --vacia dante_humo_cuentas node scripts/verify/cuentas-humo.js
// El back local corre en modo desarrollo: sin BREVO_API_KEY los mails solo se escriben en su log. Por eso con-back-local.sh
// exporta LOG_BACK (la ruta de ese log) y el humo lee de ahi los links de confirmacion y de restablecimiento. Sin LOG_BACK
// los chequeos que necesitan el log se saltean y se informan como skip.
// Todo dato es de prueba y claramente falso (mails @dante.test). No imprime tokens ni contrasenas.
// El login con Google no tiene humo (necesita un ID token real): lo cubren GoogleIdTokenVerifierTest, GoogleAuthServiceTest
// y la verificacion manual.
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

let ok = 0, fallas = 0, skip = 0;

class Salteado extends Error {}
const saltear = (motivo) => { throw new Salteado(motivo); };
const exigir = (condicion, detalle) => { if (!condicion) throw new Error(detalle); };
const dormir = (ms) => new Promise((resolver) => setTimeout(resolver, ms));

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
  return { estado: r.status, cuerpo: json, reintentarEn: r.headers.get("retry-after") };
}

const post = (ruta, cuerpo, token) => pedir("POST", ruta, cuerpo, token);

// ---- Log del back (mails de desarrollo) ----

function exigirLog() {
  if (!LOG_BACK) saltear("LOG_BACK no está definida (correr con scripts/verify/con-back-local.sh)");
}

// latin1: el log de Java en Windows puede no ser UTF-8 y solo se buscan cadenas ASCII (links, destinatarios, DNI, claves).
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

// Mails de aviso de contraseña cambiada de ese destinatario (el asunto empieza con "Tu contrase").
async function esperarAvisos(destinatario, cantidad) {
  exigirLog();
  const limite = Date.now() + 10000;
  for (;;) {
    const lineas = leerLog().split(/\r?\n/)
      .filter((l) => l.includes(`para=${destinatario} `) && l.includes("asunto=Tu contrase"));
    if (lineas.length >= cantidad) return lineas;
    if (Date.now() > limite) throw new Error(`no llegó al log el aviso de contraseña cambiada (${lineas.length} de ${cantidad})`);
    await dormir(250);
  }
}

// ---- Contratos de respuesta ----

function exigirFaltantes(cuerpo, esperados) {
  exigir(Array.isArray(cuerpo.faltantes), "faltantes no es una lista");
  const lista = [...cuerpo.faltantes].sort();
  const esperada = [...esperados].sort();
  exigir(JSON.stringify(lista) === JSON.stringify(esperada),
    `faltantes ${JSON.stringify(cuerpo.faltantes)} (esperaba ${JSON.stringify(esperados)})`);
}

// Una cuenta recien registrada con identidad completa solo debe confirmar el mail.
const SOLO_EMAIL_SIN_CONFIRMAR = ["EMAIL_SIN_CONFIRMAR"];

function exigirCuentaConMailSinConfirmar(cuerpo, apellido) {
  exigir(cuerpo && typeof cuerpo.token === "string" && cuerpo.token.length > 0, "la respuesta no trae token");
  exigir(cuerpo.cuentaVerificada === false, `cuentaVerificada ${cuerpo.cuentaVerificada} (debe ser false)`);
  exigir(cuerpo.emailConfirmado === false, `emailConfirmado ${cuerpo.emailConfirmado} (debe ser false)`);
  exigir(cuerpo.apellido === apellido, `apellido ${JSON.stringify(cuerpo.apellido)} (debe ser ${apellido})`);
  exigirFaltantes(cuerpo, SOLO_EMAIL_SIN_CONFIRMAR);
  for (const clave of ["dni", "telefono"]) {
    exigir(!(clave in cuerpo), `la respuesta expone la clave ${clave}`);
  }
}

function exigirCodigoDeCuentaNoVerificada(cuerpo) {
  exigir(cuerpo && cuerpo.codigo === "CUENTA_NO_VERIFICADA", `codigo ${JSON.stringify(cuerpo && cuerpo.codigo)}`);
  exigir(Array.isArray(cuerpo.faltantes) && cuerpo.faltantes.includes("EMAIL_SIN_CONFIRMAR"),
    `faltantes ${JSON.stringify(cuerpo.faltantes)}`);
}

(async () => {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  const email = `humo.${sufijo}@dante.test`;
  const emailAjeno = `humo2.${sufijo}@dante.test`;
  const emailInexistente = `nadie.${sufijo}@dante.test`;
  const emailLimite = `humo3.${sufijo}@dante.test`;
  const passwordInicial = `Humo-${sufijo}-x`;
  const passwordRestablecida = `Nueva-${sufijo}-y`;
  const passwordDelPerfil = `Tercera-${sufijo}-z`;

  // Datos de prueba falsos. El telefono y el DNI se escriben "sucios" a proposito: el back los normaliza.
  const TELEFONO_SUCIO = "011 15 1234-5678";
  const TELEFONO_NORMALIZADO = "+5491112345678";
  const DNI_SUCIO = "30.123.456";
  const DNI_NORMALIZADO = "30123456";
  const datos = { nombre: "Humo", apellido: "Prueba", telefono: TELEFONO_SUCIO, dni: DNI_SUCIO };
  const registroValido = { ...datos, email, password: passwordInicial };

  // ---- Registro: lo que falta se rechaza, lo sucio se normaliza ----
  await revisar("POST /auth/registro sin teléfono da 400 con campos.telefono", async () => {
    const { telefono, ...sinTelefono } = registroValido;
    const { estado, cuerpo } = await post("/auth/registro", sinTelefono);
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && cuerpo.campos && typeof cuerpo.campos.telefono === "string", `campos ${JSON.stringify(cuerpo)}`);
  });

  await revisar("POST /auth/registro sin DNI da 400 con campos.dni", async () => {
    const { dni, ...sinDni } = registroValido;
    const { estado, cuerpo } = await post("/auth/registro", sinDni);
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && cuerpo.campos && typeof cuerpo.campos.dni === "string", `campos ${JSON.stringify(cuerpo)}`);
  });

  await revisar("POST /auth/registro con un teléfono inválido da 400 con el mensaje del normalizador", async () => {
    const { estado, cuerpo } = await post("/auth/registro", { ...registroValido, telefono: "1234" });
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && /celular argentino válido/.test(cuerpo.error), `mensaje ${JSON.stringify(cuerpo)}`);
  });

  let tokenRegistro = null;
  await revisar("POST /auth/registro válido (teléfono y DNI con formato libre) da 200 y solo falta confirmar el mail", async () => {
    const { estado, cuerpo } = await post("/auth/registro", registroValido);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCuentaConMailSinConfirmar(cuerpo, "Prueba");
    tokenRegistro = cuerpo.token;
  });

  await revisar("POST /auth/registro con el mismo mail en mayúsculas da 400 'Ya existe una cuenta con ese email'", async () => {
    const { estado, cuerpo } = await post("/auth/registro",
      { ...registroValido, email: email.toUpperCase(), dni: "28.111.222" });
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && cuerpo.error === "Ya existe una cuenta con ese email", `mensaje ${JSON.stringify(cuerpo)}`);
  });

  await revisar("POST /auth/registro con el DNI de otra cuenta da 400 'DNI ya está registrado' sin revelar de quién", async () => {
    const { estado, cuerpo } = await post("/auth/registro", { ...registroValido, email: emailAjeno });
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && /DNI ya está registrado/.test(cuerpo.error), `mensaje ${JSON.stringify(cuerpo)}`);
    const texto = JSON.stringify(cuerpo);
    exigir(!texto.includes(email), "el error revela el mail de la otra cuenta");
    exigir(!texto.includes(DNI_NORMALIZADO) && !texto.includes(DNI_SUCIO), "el error repite el DNI");
  });

  // ---- Login y perfil de la cuenta registrada ----
  await revisar("POST /auth/login de esa cuenta devuelve los mismos faltantes y sin PII", async () => {
    const { estado, cuerpo } = await post("/auth/login", { email, password: passwordInicial });
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCuentaConMailSinConfirmar(cuerpo, "Prueba");
  });

  await revisar("GET /usuarios/me sin token responde 401 con error", async () => {
    const { estado, cuerpo } = await pedir("GET", "/usuarios/me");
    exigir(estado === 401, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && typeof cuerpo.error === "string", "la respuesta no trae error");
  });

  await revisar("GET /usuarios/me de la cuenta nueva trae teléfono y DNI normalizados y solo falta confirmar el mail", async () => {
    exigir(tokenRegistro, "no hay token del registro");
    const { estado, cuerpo } = await pedir("GET", "/usuarios/me", undefined, tokenRegistro);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.email === email, "el perfil no es de la cuenta del token");
    exigir(cuerpo.telefono === TELEFONO_NORMALIZADO, `telefono ${JSON.stringify(cuerpo.telefono)}`);
    exigir(cuerpo.dni === DNI_NORMALIZADO, `dni ${JSON.stringify(cuerpo.dni)}`);
    exigir(cuerpo.tieneContrasena === true && cuerpo.tieneGoogle === false, "tieneContrasena/tieneGoogle incorrectos");
    exigirFaltantes(cuerpo, SOLO_EMAIL_SIN_CONFIRMAR);
  });

  await revisar("PUT /usuarios/me con los mismos datos los deja normalizados y la cuenta sigue sin verificar", async () => {
    exigir(tokenRegistro, "no hay token del registro");
    const { estado, cuerpo } = await pedir("PUT", "/usuarios/me", datos, tokenRegistro);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.telefono === TELEFONO_NORMALIZADO && cuerpo.dni === DNI_NORMALIZADO, "los datos no quedaron normalizados");
    exigir(cuerpo.cuentaVerificada === false, "la cuenta no debe estar verificada: falta confirmar el mail");
    exigirFaltantes(cuerpo, SOLO_EMAIL_SIN_CONFIRMAR);
  });

  await revisar("PUT /usuarios/me con otro DNI da 400 porque el DNI no se puede modificar", async () => {
    exigir(tokenRegistro, "no hay token del registro");
    const { estado, cuerpo } = await pedir("PUT", "/usuarios/me", { ...datos, dni: "40.999.888" }, tokenRegistro);
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && /no se puede modificar/.test(cuerpo.error), `mensaje ${JSON.stringify(cuerpo)}`);
    const releido = await pedir("GET", "/usuarios/me", undefined, tokenRegistro);
    exigir(releido.cuerpo.dni === DNI_NORMALIZADO, "el DNI cambió");
  });

  // ---- Gate: una cuenta con el mail sin confirmar no puede consultar ni vender ----
  const consultaInexistente = { publicacionId: 999999999, mensaje: "Consulta del humo" };
  const solicitudDeVenta = {
    marca: "Marca", modelo: "Humo", anio: 2020, kilometraje: 1000,
    nombreVendedor: "Humo Prueba", telefonoVendedor: "11 5555-0000",
  };

  await revisar("POST /consultas y /solicitudes-venta sin token responden 401", async () => {
    const consulta = await pedir("POST", "/consultas", consultaInexistente);
    exigir(consulta.estado === 401, `consultas: estado ${consulta.estado}`);
    const venta = await pedir("POST", "/solicitudes-venta", solicitudDeVenta);
    exigir(venta.estado === 401, `solicitudes-venta: estado ${venta.estado}`);
  });

  await revisar("POST /consultas de una cuenta incompleta da 403 CUENTA_NO_VERIFICADA (antes de mirar la publicación)", async () => {
    exigir(tokenRegistro, "no hay token del registro");
    const { estado, cuerpo } = await pedir("POST", "/consultas", consultaInexistente, tokenRegistro);
    exigir(estado === 403, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCodigoDeCuentaNoVerificada(cuerpo);
  });

  await revisar("POST /solicitudes-venta de una cuenta incompleta da 403 CUENTA_NO_VERIFICADA", async () => {
    exigir(tokenRegistro, "no hay token del registro");
    const { estado, cuerpo } = await pedir("POST", "/solicitudes-venta", solicitudDeVenta, tokenRegistro);
    exigir(estado === 403, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCodigoDeCuentaNoVerificada(cuerpo);
  });

  // ---- Mail de confirmacion: el link sale en el log, confirma una sola vez y habilita la cuenta ----
  let tokenDeConfirmacion = null;
  await revisar("el mail de confirmación del registro llega al log con el link /confirmar-email?token=", async () => {
    tokenDeConfirmacion = await esperarLink(email, "/confirmar-email");
    exigir(tokenDeConfirmacion.length > 0, "el token del link está vacío");
  });

  await revisar("POST /auth/confirmar-email confirma una sola vez: repetirlo da 400", async () => {
    exigirLog();
    exigir(tokenDeConfirmacion, "no hay token de confirmación");
    const primero = await post("/auth/confirmar-email", { token: tokenDeConfirmacion });
    exigir(primero.estado === 200, `primer uso: estado ${primero.estado}: ${JSON.stringify(primero.cuerpo)}`);
    const repetido = await post("/auth/confirmar-email", { token: tokenDeConfirmacion });
    exigir(repetido.estado === 400, `segundo uso: estado ${repetido.estado}: ${JSON.stringify(repetido.cuerpo)}`);
  });

  await revisar("el login posterior ya no informa EMAIL_SIN_CONFIRMAR y la cuenta queda verificada", async () => {
    exigirLog();
    exigir(tokenDeConfirmacion, "no hay token de confirmación");
    const { estado, cuerpo } = await post("/auth/login", { email, password: passwordInicial });
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.emailConfirmado === true && cuerpo.cuentaVerificada === true, "la cuenta no quedó verificada");
    exigirFaltantes(cuerpo, []);
  });

  await revisar("una cuenta verificada pasa el gate: /consultas da 404 por la publicación y /solicitudes-venta da 200", async () => {
    exigirLog();
    exigir(tokenDeConfirmacion, "no hay token de confirmación");
    const login = await post("/auth/login", { email, password: passwordInicial });
    exigir(login.estado === 200, `login: estado ${login.estado}`);
    const consulta = await pedir("POST", "/consultas", consultaInexistente, login.cuerpo.token);
    exigir(consulta.estado === 404, `consultas: estado ${consulta.estado}: ${JSON.stringify(consulta.cuerpo)}`);
    const venta = await pedir("POST", "/solicitudes-venta", solicitudDeVenta, login.cuerpo.token);
    exigir(venta.estado === 200, `solicitudes-venta: estado ${venta.estado}: ${JSON.stringify(venta.cuerpo)}`);
  });

  // ---- Recuperacion de contraseña ----
  await revisar("POST /auth/olvide-contrasena responde 200 con el mismo cuerpo exista o no la cuenta", async () => {
    const existente = await post("/auth/olvide-contrasena", { email });
    const inexistente = await post("/auth/olvide-contrasena", { email: emailInexistente });
    exigir(existente.estado === 200, `cuenta existente: estado ${existente.estado}`);
    exigir(inexistente.estado === 200, `cuenta inexistente: estado ${inexistente.estado}`);
    exigir(JSON.stringify(existente.cuerpo) === JSON.stringify(inexistente.cuerpo),
      "el cuerpo distingue entre una cuenta que existe y una que no");
  });

  let tokenDeRestablecimiento = null;
  let tokenAnteriorAlRestablecimiento = null;
  await revisar("el mail de recuperación llega al log con el link /restablecer-contrasena?token=", async () => {
    tokenDeRestablecimiento = await esperarLink(email, "/restablecer-contrasena");
    exigir(tokenDeRestablecimiento.length > 0, "el token del link está vacío");
    const sesion = await post("/auth/login", { email, password: passwordInicial });
    exigir(sesion.estado === 200, `login: estado ${sesion.estado}`);
    tokenAnteriorAlRestablecimiento = sesion.cuerpo.token;
  });

  await revisar("POST /auth/restablecer-contrasena da 200 sin devolver sesión y el link sirve una sola vez", async () => {
    exigirLog();
    exigir(tokenDeRestablecimiento, "no hay token de restablecimiento");
    const primero = await post("/auth/restablecer-contrasena",
      { token: tokenDeRestablecimiento, password: passwordRestablecida });
    exigir(primero.estado === 200, `primer uso: estado ${primero.estado}: ${JSON.stringify(primero.cuerpo)}`);
    exigir(primero.cuerpo && !("token" in primero.cuerpo), "restablecer devolvió un token de sesión");
    const repetido = await post("/auth/restablecer-contrasena",
      { token: tokenDeRestablecimiento, password: `Otra-${sufijo}-w` });
    exigir(repetido.estado === 400, `segundo uso: estado ${repetido.estado}: ${JSON.stringify(repetido.cuerpo)}`);
  });

  let tokenNuevo = null;
  await revisar("el login con la contraseña nueva da 200 y la anterior da 401", async () => {
    exigirLog();
    exigir(tokenDeRestablecimiento, "no hay token de restablecimiento");
    const conNueva = await post("/auth/login", { email, password: passwordRestablecida });
    exigir(conNueva.estado === 200, `contraseña nueva: estado ${conNueva.estado}: ${JSON.stringify(conNueva.cuerpo)}`);
    tokenNuevo = conNueva.cuerpo.token;
    const conVieja = await post("/auth/login", { email, password: passwordInicial });
    exigir(conVieja.estado === 401, `contraseña anterior: estado ${conVieja.estado}`);
  });

  await revisar("la sesión anterior al restablecimiento ya no sirve (GET /usuarios/me da 401) y la nueva sí", async () => {
    exigirLog();
    exigir(tokenAnteriorAlRestablecimiento && tokenNuevo, "faltan los tokens de sesión");
    const anterior = await pedir("GET", "/usuarios/me", undefined, tokenAnteriorAlRestablecimiento);
    exigir(anterior.estado === 401, `sesión anterior: estado ${anterior.estado}`);
    const nueva = await pedir("GET", "/usuarios/me", undefined, tokenNuevo);
    exigir(nueva.estado === 200, `sesión nueva: estado ${nueva.estado}`);
  });

  await revisar("el aviso de contraseña cambiada llega al log sin ningún link", async () => {
    exigirLog();
    exigir(tokenDeRestablecimiento, "no hay token de restablecimiento");
    const avisos = await esperarAvisos(email, 1);
    for (const linea of avisos) {
      exigir(!/https?:\/\//.test(linea) && !linea.includes("token="), "el aviso trae un link");
    }
  });

  // ---- Perfil: cambio de contraseña con la actual ----
  await revisar("POST /usuarios/me/contrasena con la actual da 200 con un token nuevo y el anterior da 401", async () => {
    exigir(tokenNuevo, "no hay sesión posterior al restablecimiento");
    const { estado, cuerpo } = await pedir("POST", "/usuarios/me/contrasena",
      { actual: passwordRestablecida, nueva: passwordDelPerfil }, tokenNuevo);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(typeof cuerpo.token === "string" && cuerpo.token !== tokenNuevo, "no devolvió un token nuevo");
    const anterior = await pedir("GET", "/usuarios/me", undefined, tokenNuevo);
    exigir(anterior.estado === 401, `token anterior: estado ${anterior.estado}`);
    const nuevo = await pedir("GET", "/usuarios/me", undefined, cuerpo.token);
    exigir(nuevo.estado === 200, `token nuevo: estado ${nuevo.estado}`);
  });

  // ---- Limites: el contador de login no bloquea la recuperacion ----
  await revisar("diez logins fallidos del mismo mail dan 429 con Retry-After y la recuperación de ese mail sigue dando 200", async () => {
    const registro = await post("/auth/registro",
      { ...datos, email: emailLimite, password: passwordInicial, dni: "27.333.444" });
    exigir(registro.estado === 200, `registro: estado ${registro.estado}: ${JSON.stringify(registro.cuerpo)}`);
    const claveIncorrecta = `Mala-${sufijo}-q`;
    for (let intento = 1; intento <= 10; intento++) {
      const fallido = await post("/auth/login", { email: emailLimite, password: claveIncorrecta });
      exigir(fallido.estado === 401, `intento ${intento}: estado ${fallido.estado} (esperaba 401)`);
    }
    const bloqueado = await post("/auth/login", { email: emailLimite, password: claveIncorrecta });
    exigir(bloqueado.estado === 429, `undécimo intento: estado ${bloqueado.estado} (esperaba 429)`);
    exigir(bloqueado.reintentarEn && Number(bloqueado.reintentarEn) > 0, "la respuesta 429 no trae Retry-After");
    // D-14: las claves del limitador de login y de recuperación son distintas, así que recuperar no se bloquea.
    const olvide = await post("/auth/olvide-contrasena", { email: emailLimite });
    exigir(olvide.estado === 200, `olvide-contrasena: estado ${olvide.estado}: ${JSON.stringify(olvide.cuerpo)}`);
  });

  await revisar("la recuperación de la cuenta con el login bloqueado sí manda el mail con el link", async () => {
    const token = await esperarLink(emailLimite, "/restablecer-contrasena");
    exigir(token.length > 0, "el token del link está vacío");
  });

  // ---- Datos personales: el log del back no puede tener DNI, teléfono ni contraseñas (Ley 25.326) ----
  await revisar("el log del back no contiene el DNI, el teléfono ni ninguna contraseña usada en la corrida", async () => {
    exigirLog();
    const log = leerLog();
    const prohibidos = {
      "el DNI normalizado": DNI_NORMALIZADO,
      "el DNI con puntos": DNI_SUCIO,
      "el DNI de la segunda cuenta": "27333444",
      "el teléfono normalizado": TELEFONO_NORMALIZADO.replace("+", ""),
      "el teléfono como lo escribió el usuario": TELEFONO_SUCIO,
      "la contraseña inicial": passwordInicial,
      "la contraseña restablecida": passwordRestablecida,
      "la contraseña del perfil": passwordDelPerfil,
      "la contraseña de la segunda cuenta": `Mala-${sufijo}-q`,
    };
    const encontrados = Object.entries(prohibidos).filter(([, valor]) => log.includes(valor)).map(([nombre]) => nombre);
    exigir(encontrados.length === 0, `el log contiene: ${encontrados.join(", ")}`);
  });

  console.log(`humo: ${ok} ok, ${fallas} fallas, ${skip} skip`);
  // exitCode en vez de process.exit: en Windows, salir con fetch pendiente puede abortar Node con un assert de libuv.
  process.exitCode = fallas > 0 ? 1 : 0;
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
