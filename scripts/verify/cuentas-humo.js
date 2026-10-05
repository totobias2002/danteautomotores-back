// Humo de las cuentas verificadas (Fase 3). Solo corre contra un back local (nunca contra produccion).
// Uso: bash scripts/verify/con-back-local.sh --vacia dante_humo_cuentas node scripts/verify/cuentas-humo.js
// Los planes y tareas siguientes de la Fase 3 suman chequeos a este mismo script.
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

async function post(ruta, cuerpo) {
  const r = await fetch(API + ruta, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo),
  });
  const texto = await r.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* queda null */ }
  return { estado: r.status, cuerpo: json };
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
  return { estado: r.status, cuerpo: json };
}

function exigirFaltantes(cuerpo, esperados) {
  exigir(Array.isArray(cuerpo.faltantes), "faltantes no es una lista");
  const lista = [...cuerpo.faltantes].sort();
  const esperada = [...esperados].sort();
  exigir(JSON.stringify(lista) === JSON.stringify(esperada),
    `faltantes ${JSON.stringify(cuerpo.faltantes)} (esperaba ${JSON.stringify(esperados)})`);
}

// Lo que se espera de una cuenta nueva que se registra con el contrato actual (sin apellido, telefono ni DNI).
const FALTANTES_DE_UNA_CUENTA_NUEVA = ["APELLIDO", "TELEFONO", "DNI", "EMAIL_SIN_CONFIRMAR"];

function exigirCuentaNueva(cuerpo) {
  exigir(cuerpo && typeof cuerpo.token === "string" && cuerpo.token.length > 0, "la respuesta no trae token");
  exigir(cuerpo.cuentaVerificada === false, `cuentaVerificada ${cuerpo.cuentaVerificada} (debe ser false)`);
  exigir(cuerpo.emailConfirmado === false, `emailConfirmado ${cuerpo.emailConfirmado} (debe ser false)`);
  exigir("apellido" in cuerpo && cuerpo.apellido === null, `apellido ${JSON.stringify(cuerpo.apellido)} (debe ser null)`);
  exigir(Array.isArray(cuerpo.faltantes), "faltantes no es una lista");
  const lista = [...cuerpo.faltantes].sort();
  const esperada = [...FALTANTES_DE_UNA_CUENTA_NUEVA].sort();
  exigir(JSON.stringify(lista) === JSON.stringify(esperada),
    `faltantes ${JSON.stringify(cuerpo.faltantes)} (esperaba ${JSON.stringify(FALTANTES_DE_UNA_CUENTA_NUEVA)})`);
  for (const clave of ["dni", "telefono"]) {
    exigir(!(clave in cuerpo), `la respuesta expone la clave ${clave}`);
  }
}

(async () => {
  const sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  const email = `humo.${sufijo}@dante.test`;
  const password = `Humo-${sufijo}-x`;

  await revisar("POST /auth/registro sin apellido, telefono ni DNI: cuenta no verificada con 4 faltantes y sin PII", async () => {
    const { estado, cuerpo } = await post("/auth/registro", { nombre: "Humo", email, password });
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCuentaNueva(cuerpo);
  });

  await revisar("POST /auth/login de esa cuenta devuelve los mismos faltantes y sin PII", async () => {
    const { estado, cuerpo } = await post("/auth/login", { email, password });
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigirCuentaNueva(cuerpo);
  });

  // ---- Completar datos (03-05): el camino del tracer cerrado de punta a punta ----
  let tokenPrimera = null;
  const datos = { nombre: "Humo", apellido: "Prueba", telefono: "011 15 1234-5678", dni: "30.123.456" };

  await revisar("GET /usuarios/me sin token responde 401 con error", async () => {
    const { estado, cuerpo } = await pedir("GET", "/usuarios/me");
    exigir(estado === 401, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && typeof cuerpo.error === "string", "la respuesta no trae error");
  });

  await revisar("GET /usuarios/me de la cuenta nueva informa los 4 faltantes y el telefono nulo", async () => {
    const login = await post("/auth/login", { email, password });
    exigir(login.estado === 200, `login estado ${login.estado}`);
    tokenPrimera = login.cuerpo.token;
    const { estado, cuerpo } = await pedir("GET", "/usuarios/me", undefined, tokenPrimera);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.email === email, "el perfil no es de la cuenta del token");
    exigir(cuerpo.telefono === null, `telefono ${JSON.stringify(cuerpo.telefono)} (debe ser null)`);
    exigir(cuerpo.dni === null, `dni ${JSON.stringify(cuerpo.dni)} (debe ser null)`);
    exigir(cuerpo.tieneContrasena === true && cuerpo.tieneGoogle === false, "tieneContrasena/tieneGoogle incorrectos");
    exigirFaltantes(cuerpo, FALTANTES_DE_UNA_CUENTA_NUEVA);
  });

  await revisar("PUT /usuarios/me completa los datos normalizados y queda solo el mail sin confirmar", async () => {
    exigir(tokenPrimera, "no hay token de la primera cuenta");
    const { estado, cuerpo } = await pedir("PUT", "/usuarios/me", datos, tokenPrimera);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.telefono === "+5491112345678", `telefono ${JSON.stringify(cuerpo.telefono)}`);
    exigir(cuerpo.dni === "30123456", `dni ${JSON.stringify(cuerpo.dni)}`);
    exigir(cuerpo.apellido === "Prueba", `apellido ${JSON.stringify(cuerpo.apellido)}`);
    exigir(cuerpo.cuentaVerificada === false, "la cuenta no debe estar verificada: falta confirmar el mail");
    exigirFaltantes(cuerpo, ["EMAIL_SIN_CONFIRMAR"]);
  });

  await revisar("GET /usuarios/me posterior confirma lo guardado", async () => {
    const { estado, cuerpo } = await pedir("GET", "/usuarios/me", undefined, tokenPrimera);
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo.telefono === "+5491112345678" && cuerpo.dni === "30123456", "los datos no quedaron guardados");
    exigirFaltantes(cuerpo, ["EMAIL_SIN_CONFIRMAR"]);
  });

  await revisar("un DNI de otra cuenta da 400 con el mensaje de D-04 y sin el mail de la primera", async () => {
    const otroEmail = `humo2.${sufijo}@dante.test`;
    const registro = await post("/auth/registro", { nombre: "Humo", email: otroEmail, password });
    exigir(registro.estado === 200, `registro estado ${registro.estado}`);
    const { estado, cuerpo } = await pedir("PUT", "/usuarios/me", datos, registro.cuerpo.token);
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && /DNI ya está registrado/.test(cuerpo.error), `mensaje ${JSON.stringify(cuerpo)}`);
    exigir(!JSON.stringify(cuerpo).includes(email), "el error revela el mail de la otra cuenta");
    exigir(!JSON.stringify(cuerpo).includes("30123456"), "el error repite el DNI");
  });

  await revisar("la primera cuenta con otro DNI da 400 porque el DNI no se puede modificar", async () => {
    const { estado, cuerpo } = await pedir("PUT", "/usuarios/me", { ...datos, dni: "40.999.888" }, tokenPrimera);
    exigir(estado === 400, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo && /no se puede modificar/.test(cuerpo.error), `mensaje ${JSON.stringify(cuerpo)}`);
    const releido = await pedir("GET", "/usuarios/me", undefined, tokenPrimera);
    exigir(releido.cuerpo.dni === "30123456", "el DNI cambió");
  });

  console.log(`humo: ${ok} ok, ${fallas} fallas, ${skip} skip`);
  // exitCode en vez de process.exit: en Windows, salir con fetch pendiente puede abortar Node con un assert de libuv.
  process.exitCode = fallas > 0 ? 1 : 0;
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
