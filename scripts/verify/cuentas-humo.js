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

  console.log(`humo: ${ok} ok, ${fallas} fallas, ${skip} skip`);
  // exitCode en vez de process.exit: en Windows, salir con fetch pendiente puede abortar Node con un assert de libuv.
  process.exitCode = fallas > 0 ? 1 : 0;
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
