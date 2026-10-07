// Humo de la Fase 4 (conversaciones con la agencia), de punta a punta. Solo corre contra un back local.
// Uso (ADMIN_EMAIL y ADMIN_PASSWORD son las del admin que siembra el back local):
//   ADMIN_EMAIL=admin.humo@dante.test ADMIN_PASSWORD=... ADMIN_NOMBRE=AdminHumo \
//     bash scripts/verify/con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js
// Este plan cubre "Lo quiero" y la lista de Mis mensajes; los planes siguientes agregan el resto de la fase.
const {
  exigir,
  revisar,
  pedir,
  post,
  sufijoUnico,
  registrarCuentaVerificada,
  iniciarSesionAdmin,
  cerrar,
} = require("./humo-comun.js");

// Recorre todo el JSON buscando claves prohibidas (D-11, T-04-06): ninguna respuesta lleva DNI, telefono ni hash.
function clavesPresentes(valor, prohibidas, encontradas = new Set()) {
  if (Array.isArray(valor)) {
    valor.forEach((v) => clavesPresentes(v, prohibidas, encontradas));
  } else if (valor && typeof valor === "object") {
    for (const [clave, hijo] of Object.entries(valor)) {
      if (prohibidas.includes(clave)) encontradas.add(clave);
      clavesPresentes(hijo, prohibidas, encontradas);
    }
  }
  return [...encontradas];
}

(async () => {
  const sufijo = sufijoUnico();
  const marca = `MarcaHumo${sufijo}`;
  const modelo = "Modelo";

  // ---- Preparacion: admin, agencia (la sembrada), un auto y un comprador verificado ----
  const admin = await iniciarSesionAdmin();
  const agencias = await pedir("GET", "/agencias");
  exigir(agencias.estado === 200 && Array.isArray(agencias.cuerpo) && agencias.cuerpo.length > 0, "no hay agencias");
  const agenciaId = agencias.cuerpo[0].id;

  const auto = await post("/publicaciones", {
    agenciaId,
    marca,
    modelo,
    anio: 2020,
    precio: 12345678,
    moneda: "ARS",
    kilometraje: 40000,
    descripcion: "Auto de prueba del humo de mensajes",
  }, admin.token);
  exigir(auto.estado === 201 || auto.estado === 200, `crear el auto: estado ${auto.estado}: ${auto.texto}`);
  const publicacionId = auto.cuerpo.id;

  const comprador = await registrarCuentaVerificada("comprador");

  // ---- Lo quiero ----
  let conversacionId = null;
  await revisar("POST /conversaciones (Lo quiero) devuelve una conversación COMPRA ABIERTA atada al auto", async () => {
    const { estado, cuerpo } = await post("/conversaciones", { publicacionId }, comprador.token);
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo && cuerpo.tipo === "COMPRA", `tipo ${cuerpo && cuerpo.tipo}`);
    exigir(cuerpo.estado === "ABIERTA", `estado de la conversación ${cuerpo.estado}`);
    exigir(cuerpo.publicacion && cuerpo.publicacion.id === publicacionId, "la conversación no trae el auto");
    exigir(typeof cuerpo.creadaEn === "string" && cuerpo.creadaEn.endsWith("Z"), `creadaEn ${cuerpo.creadaEn} (debe ser un instante con Z)`);
    conversacionId = cuerpo.id;
    exigir(conversacionId, "la conversación no trae id");
  });

  await revisar("el extracto del primer mensaje nombra la marca del auto y lo escribió el usuario", async () => {
    const { cuerpo } = await post("/conversaciones", { publicacionId }, comprador.token);
    exigir(cuerpo && typeof cuerpo.ultimoMensaje === "string", "no hay extracto del último mensaje");
    exigir(cuerpo.ultimoMensaje.includes(marca), `el extracto no nombra la marca: ${cuerpo.ultimoMensaje}`);
    exigir(cuerpo.ultimoMensajeAutor === "USUARIO", `autor ${cuerpo.ultimoMensajeAutor}`);
  });

  // ---- Mis mensajes ----
  await revisar("GET /conversaciones la devuelve con los datos del auto y sin dni, telefono ni passwordHash", async () => {
    const { estado, cuerpo } = await pedir("GET", "/conversaciones", undefined, comprador.token);
    exigir(estado === 200, `estado ${estado}`);
    exigir(Array.isArray(cuerpo), "la respuesta no es una lista");
    const propia = cuerpo.find((c) => c.id === conversacionId);
    exigir(propia, "la conversación no aparece en la lista");
    exigir(propia.publicacion && propia.publicacion.marca === marca && propia.publicacion.modelo === modelo
      && propia.publicacion.anio === 2020 && propia.publicacion.estado === "DISPONIBLE",
      `datos del auto: ${JSON.stringify(propia.publicacion)}`);
    const prohibidas = clavesPresentes(cuerpo, ["dni", "telefono", "passwordHash", "password", "password_hash"]);
    exigir(prohibidas.length === 0, `la respuesta expone: ${prohibidas.join(", ")}`);
  });

  await revisar("repetir Lo quiero devuelve la misma conversación y la lista sigue teniendo una sola", async () => {
    const repetido = await post("/conversaciones", { publicacionId }, comprador.token);
    exigir(repetido.estado === 200, `estado ${repetido.estado}`);
    exigir(repetido.cuerpo.id === conversacionId, `otra conversación: ${repetido.cuerpo.id} (esperaba ${conversacionId})`);
    const { cuerpo } = await pedir("GET", "/conversaciones", undefined, comprador.token);
    const delAuto = cuerpo.filter((c) => c.publicacion && c.publicacion.id === publicacionId);
    exigir(delAuto.length === 1, `la lista tiene ${delAuto.length} conversaciones del auto (esperaba 1)`);
  });

  // ---- Limpieza del auto de prueba (la base es descartable, pero el humo no deja basura si se reusa) ----
  await revisar("el admin borra el auto de prueba y se lleva sus conversaciones", async () => {
    const borrado = await pedir("DELETE", `/publicaciones/${publicacionId}`, undefined, admin.token);
    exigir(borrado.estado === 204 || borrado.estado === 200, `estado ${borrado.estado}: ${borrado.texto}`);
  });

  cerrar();
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
