// Humo de la Fase 4 (conversaciones con la agencia), de punta a punta. Solo corre contra un back local.
// Uso (ADMIN_EMAIL y ADMIN_PASSWORD son las del admin que siembra el back local):
//   ADMIN_EMAIL=admin.humo@dante.test ADMIN_PASSWORD=... ADMIN_NOMBRE=AdminHumo \
//     bash scripts/verify/con-back-local.sh --vacia dante_humo_mensajes node scripts/verify/mensajes-humo.js
// Cubre "Lo quiero", la lista de Mis mensajes, el hilo del comprador (leer, escribir, aislamiento y límite de envío), los
// mensajes sin leer, la bandeja del admin con sus filtros, el hilo de la agencia (responder, marcar leída, cerrar y
// reabrir, con el contador del comprador y el del admin), los avisos por mail que salen en el log del back (uno por
// ventana de 10 minutos, sin el texto del mensaje) y los casos negativos; los planes siguientes agregan el resto.
const {
  exigir,
  revisar,
  pedir,
  post,
  dormir,
  exigirLog,
  leerLog,
  sufijoUnico,
  dniAleatorio,
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

  const autosCreados = [];
  const crearAuto = async (modeloAuto, descripcion) => {
    const creado = await post("/publicaciones", {
      agenciaId,
      marca,
      modelo: modeloAuto,
      anio: 2020,
      precio: 12345678,
      moneda: "ARS",
      kilometraje: 40000,
      descripcion,
    }, admin.token);
    exigir(creado.estado === 201 || creado.estado === 200, `crear el auto: estado ${creado.estado}: ${creado.texto}`);
    autosCreados.push(creado.cuerpo.id);
    return creado.cuerpo.id;
  };
  const publicacionId = await crearAuto(modelo, "Auto de prueba del humo de mensajes");

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

  let consulta = null;

  // ---- Casos negativos y de borde (MSG-01, D-04, D-12, T-04-01, T-04-08) ----
  await revisar("sin token POST /conversaciones da 401 y GET también", async () => {
    const creacion = await post("/conversaciones", { publicacionId });
    exigir(creacion.estado === 401, `POST sin token: estado ${creacion.estado}`);
    const lista = await pedir("GET", "/conversaciones");
    exigir(lista.estado === 401, `GET sin token: estado ${lista.estado}`);
  });

  await revisar("con la cuenta admin POST y GET /conversaciones dan 403", async () => {
    const creacion = await post("/conversaciones", { publicacionId }, admin.token);
    exigir(creacion.estado === 403, `POST del admin: estado ${creacion.estado}`);
    const lista = await pedir("GET", "/conversaciones", undefined, admin.token);
    exigir(lista.estado === 403, `GET del admin: estado ${lista.estado}`);
  });

  await revisar("una cuenta con el mail sin confirmar recibe 403 CUENTA_NO_VERIFICADA y no se crea nada", async () => {
    const sinConfirmar = `sinconfirmar.${sufijo}@dante.test`;
    const clave = `Humo-${sufijo}-y`;
    const registro = await post("/auth/registro", {
      nombre: "Humo",
      apellido: "SinConfirmar",
      telefono: "011 15 1234-5678",
      dni: dniAleatorio(),
      email: sinConfirmar,
      password: clave,
    });
    exigir(registro.estado === 200, `registro: estado ${registro.estado}: ${registro.texto}`);
    const login = await post("/auth/login", { email: sinConfirmar, password: clave });
    exigir(login.estado === 200 && login.cuerpo && login.cuerpo.token, `login: estado ${login.estado}`);
    const { estado, cuerpo } = await post("/conversaciones", { publicacionId }, login.cuerpo.token);
    exigir(estado === 403, `estado ${estado}`);
    exigir(cuerpo && cuerpo.codigo === "CUENTA_NO_VERIFICADA", `codigo ${cuerpo && cuerpo.codigo}`);
    exigir(Array.isArray(cuerpo.faltantes) && cuerpo.faltantes.includes("EMAIL_SIN_CONFIRMAR"),
      `faltantes ${JSON.stringify(cuerpo.faltantes)}`);
    const lista = await pedir("GET", "/conversaciones", undefined, login.cuerpo.token);
    exigir(lista.estado === 200 && Array.isArray(lista.cuerpo) && lista.cuerpo.length === 0,
      `la cuenta sin confirmar no debería tener conversaciones: ${lista.texto}`);
  });

  await revisar("un auto VENDIDO da 400 'Este auto ya se vendió' y un RESERVADO se acepta", async () => {
    const vendidoId = await crearAuto("Vendido", "Auto vendido del humo");
    const reservadoId = await crearAuto("Reservado", "Auto reservado del humo");
    const vendido = await pedir("PATCH", `/publicaciones/${vendidoId}/estado`, { estado: "VENDIDO" }, admin.token);
    exigir(vendido.estado === 200, `pasar a VENDIDO: estado ${vendido.estado}: ${vendido.texto}`);
    const reservado = await pedir("PATCH", `/publicaciones/${reservadoId}/estado`, { estado: "RESERVADO" }, admin.token);
    exigir(reservado.estado === 200, `pasar a RESERVADO: estado ${reservado.estado}: ${reservado.texto}`);

    const rechazado = await post("/conversaciones", { publicacionId: vendidoId }, comprador.token);
    exigir(rechazado.estado === 400, `auto vendido: estado ${rechazado.estado}`);
    exigir(rechazado.cuerpo && rechazado.cuerpo.error === "Este auto ya se vendió", `mensaje: ${rechazado.texto}`);

    const aceptado = await post("/conversaciones", { publicacionId: reservadoId }, comprador.token);
    exigir(aceptado.estado === 200, `auto reservado: estado ${aceptado.estado}: ${aceptado.texto}`);
    exigir(aceptado.cuerpo.publicacion.estado === "RESERVADO", `estado del auto ${aceptado.cuerpo.publicacion.estado}`);

    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    exigir(!lista.cuerpo.some((c) => c.publicacion && c.publicacion.id === vendidoId), "se creó una conversación del auto vendido");
  });

  await revisar("un auto inexistente da 404", async () => {
    const { estado } = await post("/conversaciones", { publicacionId: 2000000000 }, comprador.token);
    exigir(estado === 404, `estado ${estado}`);
  });

  await revisar("un cuerpo con tipo, estado y usuarioId de más igual crea una conversación COMPRA ABIERTA propia", async () => {
    const autoDeMasId = await crearAuto("CamposDeMas", "Auto del humo para campos de más");
    const { estado, cuerpo } = await post("/conversaciones", {
      publicacionId: autoDeMasId,
      tipo: "COTIZACION",
      estado: "CERRADA",
      usuarioId: 1,
    }, comprador.token);
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo.tipo === "COMPRA" && cuerpo.estado === "ABIERTA", `salió ${cuerpo.tipo} ${cuerpo.estado}`);
    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    exigir(lista.cuerpo.some((c) => c.id === cuerpo.id), "la conversación no es de la cuenta del token");
  });

  await revisar("un mensaje de 2001 caracteres da 400 con campos.mensaje", async () => {
    const { estado, cuerpo } = await post("/conversaciones", { publicacionId, mensaje: "a".repeat(2001) }, comprador.token);
    exigir(estado === 400, `estado ${estado}`);
    exigir(cuerpo && cuerpo.campos && cuerpo.campos.mensaje, `sin campos.mensaje: ${JSON.stringify(cuerpo)}`);
  });

  // ---- "Consultar por este auto": el mismo POST con un texto propio (D-01, D-04) ----
  await revisar("un mensaje propio sobre un auto nuevo crea la conversación y es el último mensaje, recortado", async () => {
    const consultaId = await crearAuto("Consulta", "Auto del humo para consultar con texto propio");
    const { estado, cuerpo } = await post("/conversaciones", {
      publicacionId: consultaId,
      mensaje: "   ¿Aceptan permuta?   ",
    }, comprador.token);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.tipo === "COMPRA" && cuerpo.estado === "ABIERTA", `salió ${cuerpo.tipo} ${cuerpo.estado}`);
    exigir(cuerpo.ultimoMensaje === "¿Aceptan permuta?", `ultimoMensaje: ${JSON.stringify(cuerpo.ultimoMensaje)}`);
    exigir(cuerpo.ultimoMensajeAutor === "USUARIO", `autor ${cuerpo.ultimoMensajeAutor}`);
    consulta = { autoId: consultaId, conversacionId: cuerpo.id };
  });

  await revisar("un segundo mensaje propio sobre el mismo auto reutiliza la conversación y pasa a ser el último", async () => {
    exigir(consulta, "falta la conversación del caso anterior");
    const { estado, cuerpo } = await post("/conversaciones", {
      publicacionId: consulta.autoId,
      mensaje: "¿Y financian?",
    }, comprador.token);
    exigir(estado === 200, `estado ${estado}`);
    exigir(cuerpo.id === consulta.conversacionId, `otra conversación: ${cuerpo.id} (esperaba ${consulta.conversacionId})`);
    exigir(cuerpo.ultimoMensaje === "¿Y financian?", `ultimoMensaje: ${JSON.stringify(cuerpo.ultimoMensaje)}`);
    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    const delAuto = lista.cuerpo.filter((c) => c.publicacion && c.publicacion.id === consulta.autoId);
    exigir(delAuto.length === 1, `la lista tiene ${delAuto.length} conversaciones del auto (esperaba 1)`);
    exigir(delAuto[0].ultimoMensaje === "¿Y financian?", `la lista muestra: ${JSON.stringify(delAuto[0].ultimoMensaje)}`);
  });

  await revisar("un mensaje de solo espacios usa el texto automático que nombra el auto", async () => {
    const enBlancoId = await crearAuto("EnBlanco", "Auto del humo para mensaje en blanco");
    const { estado, cuerpo } = await post("/conversaciones", { publicacionId: enBlancoId, mensaje: "     " }, comprador.token);
    exigir(estado === 200, `estado ${estado}`);
    exigir(typeof cuerpo.ultimoMensaje === "string" && cuerpo.ultimoMensaje.includes(marca) && cuerpo.ultimoMensaje.includes("EnBlanco"),
      `el texto automático no nombra el auto: ${JSON.stringify(cuerpo.ultimoMensaje)}`);
  });

  await revisar("la lista de una segunda cuenta verificada no incluye la conversación de la primera (T-04-01)", async () => {
    const otra = await registrarCuentaVerificada("otra");
    const { estado, cuerpo } = await pedir("GET", "/conversaciones", undefined, otra.token);
    exigir(estado === 200 && Array.isArray(cuerpo), `estado ${estado}`);
    exigir(cuerpo.length === 0, `la segunda cuenta ve ${cuerpo.length} conversaciones ajenas`);
    exigir(!cuerpo.some((c) => c.id === conversacionId), "la conversación de la primera cuenta aparece en la lista de la segunda");
  });

  // ---- El hilo del comprador (MSG-03, MSG-04, D-05, D-12, D-14) ----
  // Presupuesto del límite de envío (D-10, 20 por cuenta cada 10 minutos, contando los "Lo quiero"): la cuenta principal
  // hace 10 POST /conversaciones antes de este bloque y 1 envío acá (11 de 20). Los planes siguientes suman a la misma
  // cuenta y tienen que quedar por debajo de 15 por corrida; solo la cuenta descartable de más abajo llega al límite.
  await revisar("GET /conversaciones/{id} devuelve la conversación y el primer mensaje, con fechas en UTC y sin datos personales", async () => {
    const { estado, cuerpo } = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.conversacion && cuerpo.conversacion.id === conversacionId, "no trae la conversación");
    exigir(cuerpo.conversacion.publicacion && cuerpo.conversacion.publicacion.marca === marca, "no trae el auto");
    exigir(Array.isArray(cuerpo.mensajes) && cuerpo.mensajes.length === 1, `mensajes: ${JSON.stringify(cuerpo.mensajes)}`);
    const primero = cuerpo.mensajes[0];
    exigir(primero.autor === "USUARIO" && primero.texto.includes(marca), `primer mensaje: ${JSON.stringify(primero)}`);
    exigir(typeof primero.creadoEn === "string" && primero.creadoEn.endsWith("Z"), `creadoEn ${primero.creadoEn}`);
    exigir(primero.leido === false, `leido ${primero.leido}`);
    const prohibidas = clavesPresentes(cuerpo, ["dni", "telefono", "passwordHash", "password", "email", "autorId", "usuarioId"]);
    exigir(prohibidas.length === 0, `la respuesta expone: ${prohibidas.join(", ")}`);
  });

  await revisar("POST /conversaciones/{id}/mensajes guarda el mensaje (recortado, HTML literal) y el hilo queda con dos en orden", async () => {
    const texto = "<b>Hola</b> <script>alert(1)</script> ¿sigue disponible?";
    const enviado = await post(`/conversaciones/${conversacionId}/mensajes`, { texto: `   ${texto}  ` }, comprador.token);
    exigir(enviado.estado === 200, `estado ${enviado.estado}: ${enviado.texto}`);
    exigir(enviado.cuerpo.autor === "USUARIO", `autor ${enviado.cuerpo.autor}`);
    exigir(enviado.cuerpo.texto === texto, `texto guardado: ${JSON.stringify(enviado.cuerpo.texto)}`);
    exigir(typeof enviado.cuerpo.creadoEn === "string" && enviado.cuerpo.creadoEn.endsWith("Z"), `creadoEn ${enviado.cuerpo.creadoEn}`);
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(hilo.cuerpo.mensajes.length === 2, `el hilo tiene ${hilo.cuerpo.mensajes.length} mensajes (esperaba 2)`);
    exigir(hilo.cuerpo.mensajes[0].id < hilo.cuerpo.mensajes[1].id, "los mensajes no vienen en orden ascendente");
    exigir(hilo.cuerpo.mensajes[1].id === enviado.cuerpo.id && hilo.cuerpo.mensajes[1].texto === texto, "el segundo mensaje no es el enviado");
    exigir(hilo.cuerpo.conversacion.ultimoMensaje === texto, `ultimoMensaje: ${JSON.stringify(hilo.cuerpo.conversacion.ultimoMensaje)}`);
  });

  await revisar("un texto vacío, en blanco o de 2001 caracteres da 400 con campos.texto y no se guarda", async () => {
    for (const texto of ["", "     ", "a".repeat(2001)]) {
      const { estado, cuerpo } = await post(`/conversaciones/${conversacionId}/mensajes`, { texto }, comprador.token);
      exigir(estado === 400, `texto de ${texto.length} caracteres: estado ${estado}`);
      exigir(cuerpo && cuerpo.campos && cuerpo.campos.texto, `sin campos.texto: ${JSON.stringify(cuerpo)}`);
    }
    const sinTexto = await post(`/conversaciones/${conversacionId}/mensajes`, {}, comprador.token);
    exigir(sinTexto.estado === 400 && sinTexto.cuerpo && sinTexto.cuerpo.campos && sinTexto.cuerpo.campos.texto,
      `cuerpo sin texto: estado ${sinTexto.estado}`);
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(hilo.cuerpo.mensajes.length === 2, `el hilo tiene ${hilo.cuerpo.mensajes.length} mensajes (esperaba 2)`);
  });

  await revisar("sin token el hilo y el envío dan 401, y con la cuenta admin dan 403", async () => {
    for (const [metodo, ruta, cuerpo] of [
      ["GET", `/conversaciones/${conversacionId}`, undefined],
      ["POST", `/conversaciones/${conversacionId}/mensajes`, { texto: "hola" }],
    ]) {
      const sin = await pedir(metodo, ruta, cuerpo);
      exigir(sin.estado === 401, `${metodo} sin token: estado ${sin.estado}`);
      const delAdmin = await pedir(metodo, ruta, cuerpo, admin.token);
      exigir(delAdmin.estado === 403, `${metodo} del admin: estado ${delAdmin.estado}`);
    }
  });

  await revisar("una segunda cuenta verificada recibe 404 al pedir y al escribir en la conversación de la primera (T-04-10)", async () => {
    const ajena = await registrarCuentaVerificada("ajena");
    const leer = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, ajena.token);
    exigir(leer.estado === 404, `GET de otra cuenta: estado ${leer.estado}`);
    const escribir = await post(`/conversaciones/${conversacionId}/mensajes`, { texto: "intruso" }, ajena.token);
    exigir(escribir.estado === 404, `POST de otra cuenta: estado ${escribir.estado}`);
    // Una inexistente da exactamente la misma respuesta que una ajena (D-12).
    const inexistente = await pedir("GET", "/conversaciones/2000000000", undefined, ajena.token);
    exigir(inexistente.estado === 404, `GET inexistente: estado ${inexistente.estado}`);
    exigir(inexistente.texto === leer.texto, `la ajena y la inexistente responden distinto: ${leer.texto} / ${inexistente.texto}`);
    const dueno = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(dueno.cuerpo.mensajes.length === 2, `el hilo del dueño tiene ${dueno.cuerpo.mensajes.length} mensajes (esperaba 2)`);
    exigir(!dueno.cuerpo.mensajes.some((m) => m.texto === "intruso"), "el mensaje de la cuenta ajena quedó en el hilo");
  });

  // ---- Mensajes sin leer (MSG-05, D-06, D-12, T-04-15, T-04-16) ----
  // Los casos con mensajes de la agencia sin leer se recorren de punta a punta más abajo, en el bloque del hilo de la agencia.
  await revisar("GET /conversaciones/no-leidas da 401 sin token, ceros al comprador sin mensajes de la agencia y cuenta lo que nadie leyó al admin", async () => {
    const sin = await pedir("GET", "/conversaciones/no-leidas");
    exigir(sin.estado === 401, `sin token: estado ${sin.estado}`);

    const propio = await pedir("GET", "/conversaciones/no-leidas", undefined, comprador.token);
    exigir(propio.estado === 200, `del comprador: estado ${propio.estado}: ${propio.texto}`);
    exigir(propio.cuerpo.noLeidos === 0 && propio.cuerpo.conversaciones === 0, `del comprador: ${propio.texto}`);
    // Solo dos números: nada de ids ni texto (T-04-15).
    exigir(Object.keys(propio.cuerpo).sort().join(",") === "conversaciones,noLeidos", `claves: ${Object.keys(propio.cuerpo)}`);

    const delAdmin = await pedir("GET", "/conversaciones/no-leidas", undefined, admin.token);
    exigir(delAdmin.estado === 200, `del admin: estado ${delAdmin.estado}: ${delAdmin.texto}`);
    exigir(delAdmin.cuerpo.noLeidos >= 1 && delAdmin.cuerpo.conversaciones >= 1,
      `el admin no cuenta los mensajes del comprador que nadie leyó: ${delAdmin.texto}`);

    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    exigir(lista.cuerpo.length > 0 && lista.cuerpo.every((c) => c.noLeidos === 0), `noLeidos en la lista: ${JSON.stringify(lista.cuerpo.map((c) => c.noLeidos))}`);
  });

  await revisar("POST /conversaciones/{id}/leida: el dueño recibe 200 con los conteos y sus propios mensajes siguen sin leer", async () => {
    const dueno = await post(`/conversaciones/${conversacionId}/leida`, undefined, comprador.token);
    exigir(dueno.estado === 200, `estado ${dueno.estado}: ${dueno.texto}`);
    exigir(dueno.cuerpo.noLeidos === 0 && dueno.cuerpo.conversaciones === 0, `conteos: ${dueno.texto}`);
    // Solo se marcan los de la agencia: lo que escribió el comprador sigue sin leer para la agencia.
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(hilo.cuerpo.mensajes.length === 2 && hilo.cuerpo.mensajes.every((m) => m.autor === "USUARIO" && m.leido === false),
      `mensajes propios: ${JSON.stringify(hilo.cuerpo.mensajes.map((m) => [m.autor, m.leido]))}`);
    exigir(hilo.cuerpo.conversacion.noLeidos === 0, `noLeidos del hilo: ${hilo.cuerpo.conversacion.noLeidos}`);
  });

  await revisar("marcar como leída sin token da 401, con el admin 403 y una conversación ajena o inexistente 404 igual", async () => {
    const sin = await post(`/conversaciones/${conversacionId}/leida`, undefined);
    exigir(sin.estado === 401, `sin token: estado ${sin.estado}`);
    const delAdmin = await post(`/conversaciones/${conversacionId}/leida`, undefined, admin.token);
    exigir(delAdmin.estado === 403, `del admin: estado ${delAdmin.estado}`);

    const otra = await registrarCuentaVerificada("leida");
    const ajena = await post(`/conversaciones/${conversacionId}/leida`, undefined, otra.token);
    exigir(ajena.estado === 404, `de otra cuenta: estado ${ajena.estado}`);
    const inexistente = await post("/conversaciones/2000000000/leida", undefined, otra.token);
    exigir(inexistente.estado === 404, `inexistente: estado ${inexistente.estado}`);
    exigir(ajena.texto === inexistente.texto, `la ajena y la inexistente responden distinto: ${ajena.texto} / ${inexistente.texto}`);
    // La del admin sigue viendo el mensaje del comprador: la cuenta ajena no pudo tocarlo.
    const delAdminDespues = await pedir("GET", "/conversaciones/no-leidas", undefined, admin.token);
    exigir(delAdminDespues.cuerpo.noLeidos >= 1, `el admin dejó de ver mensajes sin leer: ${delAdminDespues.texto}`);
  });

  // ---- La bandeja del admin (MSG-05, MSG-07, D-11, D-15, T-04-18, T-04-19, T-04-20) ----
  await revisar("GET /admin/conversaciones da 401 sin token y 403 al comprador", async () => {
    const sin = await pedir("GET", "/admin/conversaciones");
    exigir(sin.estado === 401, `sin token: estado ${sin.estado}`);
    const delComprador = await pedir("GET", "/admin/conversaciones", undefined, comprador.token);
    exigir(delComprador.estado === 403, `del comprador: estado ${delComprador.estado}`);
  });

  await revisar("el admin lista la conversación con el nombre del usuario, noLeidos mayor a cero y sin dni ni telefono", async () => {
    const { estado, cuerpo } = await pedir("GET", "/admin/conversaciones", undefined, admin.token);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(Array.isArray(cuerpo.contenido), "la respuesta no trae contenido");
    exigir(cuerpo.pagina === 1 && cuerpo.tamanio === 20 && cuerpo.totalElementos >= 1 && cuerpo.totalPaginas >= 1,
      `estructura de página: ${JSON.stringify({ ...cuerpo, contenido: undefined })}`);
    const fila = cuerpo.contenido.find((c) => c.id === conversacionId);
    exigir(fila, "la conversación no aparece en la bandeja del admin");
    exigir(fila.usuario && fila.usuario.nombre === "Humo" && fila.usuario.apellido === "Prueba" && fila.usuario.email,
      `usuario de la fila: ${JSON.stringify(fila.usuario)}`);
    exigir(fila.noLeidos > 0, `noLeidos ${fila.noLeidos}`);
    exigir(fila.tipo === "COMPRA" && fila.estado === "ABIERTA", `salió ${fila.tipo} ${fila.estado}`);
    exigir(fila.publicacion && fila.publicacion.marca === marca, "la fila no trae el auto");
    const prohibidas = clavesPresentes(cuerpo, ["dni", "telefono", "passwordHash", "password", "password_hash"]);
    exigir(prohibidas.length === 0, `la bandeja expone: ${prohibidas.join(", ")}`);
    // La bandeja ordena por el último mensaje, el más reciente primero.
    const fechas = cuerpo.contenido.map((c) => c.ultimoMensajeEn);
    exigir(fechas.every((f, i) => i === 0 || Date.parse(f) <= Date.parse(fechas[i - 1])), `orden de las filas: ${JSON.stringify(fechas)}`);
  });

  await revisar("los filtros de la bandeja: estado, solo no leídas y tipo, y un valor inválido da 400", async () => {
    const cerradas = await pedir("GET", "/admin/conversaciones?estado=CERRADA", undefined, admin.token);
    exigir(cerradas.estado === 200, `CERRADA: estado ${cerradas.estado}`);
    exigir(!cerradas.cuerpo.contenido.some((c) => c.id === conversacionId), "la abierta aparece entre las cerradas");
    exigir(cerradas.cuerpo.contenido.every((c) => c.estado === "CERRADA"), "hay filas que no están cerradas");

    const abiertas = await pedir("GET", "/admin/conversaciones?estado=ABIERTA", undefined, admin.token);
    exigir(abiertas.cuerpo.contenido.some((c) => c.id === conversacionId), "la abierta no aparece entre las abiertas");

    const sinLeer = await pedir("GET", "/admin/conversaciones?soloNoLeidas=true", undefined, admin.token);
    exigir(sinLeer.estado === 200, `soloNoLeidas: estado ${sinLeer.estado}`);
    exigir(sinLeer.cuerpo.contenido.some((c) => c.id === conversacionId), "la conversación sin leer no aparece con soloNoLeidas");
    exigir(sinLeer.cuerpo.contenido.every((c) => c.noLeidos > 0), "hay filas leídas con soloNoLeidas");

    const compras = await pedir("GET", "/admin/conversaciones?tipo=COMPRA", undefined, admin.token);
    exigir(compras.cuerpo.contenido.some((c) => c.id === conversacionId), "la compra no aparece con tipo=COMPRA");

    const cotizaciones = await pedir("GET", "/admin/conversaciones?tipo=COTIZACION", undefined, admin.token);
    exigir(cotizaciones.estado === 200, `COTIZACION: estado ${cotizaciones.estado}`);
    exigir(cotizaciones.cuerpo.contenido.length === 0 && cotizaciones.cuerpo.totalElementos === 0,
      `se esperaba una página vacía: ${JSON.stringify(cotizaciones.cuerpo)}`);

    const invalido = await pedir("GET", "/admin/conversaciones?tipo=NAVE", undefined, admin.token);
    exigir(invalido.estado === 400, `tipo=NAVE: estado ${invalido.estado}`);
    exigir(invalido.cuerpo && typeof invalido.cuerpo.error === "string", `sin error uniforme: ${invalido.texto}`);

    const combinado = await pedir("GET", "/admin/conversaciones?tipo=COMPRA&estado=ABIERTA&soloNoLeidas=true&pagina=1", undefined, admin.token);
    exigir(combinado.estado === 200 && combinado.cuerpo.contenido.some((c) => c.id === conversacionId),
      `filtros combinados: estado ${combinado.estado}`);
    const lejos = await pedir("GET", "/admin/conversaciones?pagina=999", undefined, admin.token);
    exigir(lejos.estado === 200 && lejos.cuerpo.contenido.length === 0, `página 999: estado ${lejos.estado}`);
  });

  await revisar("una cuenta comprador no puede usar los filtros de la bandeja (403 con cualquier parámetro)", async () => {
    const r = await pedir("GET", "/admin/conversaciones?soloNoLeidas=true&tipo=COMPRA", undefined, comprador.token);
    exigir(r.estado === 403, `estado ${r.estado}`);
  });

  // ---- El hilo de la agencia (MSG-04, MSG-05, MSG-08, D-05, D-06, D-08, D-12, T-04-22, T-04-23, T-04-24) ----
  // Presupuesto del límite de envío de la cuenta principal (20 cada 10 minutos, D-10): hasta acá lleva 11; este bloque suma
  // 4 más (el mensaje de después de la respuesta, el intento sobre la cerrada, el de después de reabrir y el segundo
  // "Lo quiero"), o sea 15 de 20.
  const hiloAdmin = (sufijoDeRuta = "") => `/admin/conversaciones/${conversacionId}${sufijoDeRuta}`;
  const contadorDelAdmin = async () => {
    const r = await pedir("GET", "/conversaciones/no-leidas", undefined, admin.token);
    exigir(r.estado === 200, `contador del admin: estado ${r.estado}`);
    return r.cuerpo.noLeidos;
  };

  await revisar("el hilo de la agencia da 401 sin token y 403 al comprador en cada endpoint", async () => {
    for (const [metodo, ruta, cuerpo] of [
      ["GET", hiloAdmin(), undefined],
      ["POST", hiloAdmin("/mensajes"), { texto: "intruso" }],
      ["POST", hiloAdmin("/leida"), undefined],
      ["POST", hiloAdmin("/cerrar"), undefined],
      ["POST", hiloAdmin("/reabrir"), undefined],
    ]) {
      const sin = await pedir(metodo, ruta, cuerpo);
      exigir(sin.estado === 401, `${metodo} ${ruta} sin token: estado ${sin.estado}`);
      const delComprador = await pedir(metodo, ruta, cuerpo, comprador.token);
      exigir(delComprador.estado === 403, `${metodo} ${ruta} del comprador: estado ${delComprador.estado}`);
    }
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(!hilo.cuerpo.mensajes.some((m) => m.texto === "intruso"), "el comprador escribió como la agencia");
  });

  await revisar("el admin abre el hilo con el usuario y sus mensajes sin leer, sin dni ni telefono, y una inexistente da 404", async () => {
    const { estado, cuerpo } = await pedir("GET", hiloAdmin(), undefined, admin.token);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(cuerpo.conversacion.id === conversacionId && cuerpo.conversacion.estado === "ABIERTA", "no trae la conversación abierta");
    const usuario = cuerpo.conversacion.usuario;
    exigir(usuario && usuario.nombre === "Humo" && usuario.apellido === "Prueba" && usuario.email, `usuario: ${JSON.stringify(usuario)}`);
    exigir(cuerpo.conversacion.noLeidos === 2, `noLeidos ${cuerpo.conversacion.noLeidos} (esperaba los 2 mensajes del comprador)`);
    exigir(cuerpo.mensajes.length === 2 && cuerpo.mensajes.every((m) => m.autor === "USUARIO" && m.leido === false),
      `mensajes: ${JSON.stringify(cuerpo.mensajes.map((m) => [m.autor, m.leido]))}`);
    const prohibidas = clavesPresentes(cuerpo, ["dni", "telefono", "passwordHash", "password", "password_hash"]);
    exigir(prohibidas.length === 0, `el hilo expone: ${prohibidas.join(", ")}`);
    const inexistente = await pedir("GET", "/admin/conversaciones/2000000000", undefined, admin.token);
    exigir(inexistente.estado === 404, `inexistente: estado ${inexistente.estado}`);
  });

  let adminAntesDeLeer = 0;
  await revisar("el admin responde (autor AGENCIA, texto recortado) y un texto vacío o de 2001 caracteres da 400 con campos.texto", async () => {
    adminAntesDeLeer = await contadorDelAdmin();
    for (const texto of ["", "     ", "a".repeat(2001)]) {
      const rechazo = await post(hiloAdmin("/mensajes"), { texto }, admin.token);
      exigir(rechazo.estado === 400 && rechazo.cuerpo && rechazo.cuerpo.campos && rechazo.cuerpo.campos.texto,
        `texto de ${texto.length} caracteres: estado ${rechazo.estado}: ${rechazo.texto}`);
    }
    const respuesta = await post(hiloAdmin("/mensajes"), { texto: "   Hola, pasá cuando quieras.   " }, admin.token);
    exigir(respuesta.estado === 200, `estado ${respuesta.estado}: ${respuesta.texto}`);
    exigir(respuesta.cuerpo.autor === "AGENCIA", `autor ${respuesta.cuerpo.autor}`);
    exigir(respuesta.cuerpo.texto === "Hola, pasá cuando quieras.", `texto: ${JSON.stringify(respuesta.cuerpo.texto)}`);
    exigir(typeof respuesta.cuerpo.creadoEn === "string" && respuesta.cuerpo.creadoEn.endsWith("Z"), `creadoEn ${respuesta.cuerpo.creadoEn}`);
    // Escribir no es leer: el contador del admin sigue igual hasta que abra el hilo.
    exigir((await contadorDelAdmin()) === adminAntesDeLeer, "el contador del admin cambió al responder");
  });

  await revisar("el admin marca leída: su contador baja, los mensajes del usuario quedan leídos y el de la agencia no", async () => {
    const { estado, cuerpo } = await post(hiloAdmin("/leida"), undefined, admin.token);
    exigir(estado === 200, `estado ${estado}: ${JSON.stringify(cuerpo)}`);
    exigir(Object.keys(cuerpo).sort().join(",") === "conversaciones,noLeidos", `claves: ${Object.keys(cuerpo)}`);
    exigir(cuerpo.noLeidos === adminAntesDeLeer - 2, `noLeidos ${cuerpo.noLeidos} (esperaba ${adminAntesDeLeer - 2})`);
    exigir((await contadorDelAdmin()) === cuerpo.noLeidos, "el contador no coincide con la respuesta");
    const hilo = await pedir("GET", hiloAdmin(), undefined, admin.token);
    exigir(hilo.cuerpo.conversacion.noLeidos === 0, `noLeidos del hilo ${hilo.cuerpo.conversacion.noLeidos}`);
    exigir(hilo.cuerpo.mensajes.filter((m) => m.autor === "USUARIO").every((m) => m.leido === true), "quedaron mensajes del usuario sin leer");
    exigir(hilo.cuerpo.mensajes.filter((m) => m.autor === "AGENCIA").every((m) => m.leido === false), "se marcó un mensaje de la agencia");
  });

  await revisar("el comprador ve la respuesta sin leer en su contador, en la lista y en el hilo, y al abrirlo vuelve a cero", async () => {
    const contador = await pedir("GET", "/conversaciones/no-leidas", undefined, comprador.token);
    exigir(contador.estado === 200 && contador.cuerpo.noLeidos >= 1 && contador.cuerpo.conversaciones >= 1,
      `contador del comprador: ${contador.texto}`);
    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    const fila = lista.cuerpo.find((c) => c.id === conversacionId);
    exigir(fila && fila.noLeidos === 1 && fila.ultimoMensajeAutor === "AGENCIA", `fila: ${JSON.stringify(fila)}`);
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    const ultimo = hilo.cuerpo.mensajes[hilo.cuerpo.mensajes.length - 1];
    exigir(ultimo.autor === "AGENCIA" && ultimo.leido === false && ultimo.texto === "Hola, pasá cuando quieras.",
      `último mensaje: ${JSON.stringify(ultimo)}`);
    exigir(hilo.cuerpo.conversacion.noLeidos === 1, `noLeidos del hilo ${hilo.cuerpo.conversacion.noLeidos}`);
    // El comprador no ve quién de la agencia escribió: el mensaje no trae autor de cuenta.
    const prohibidas = clavesPresentes(hilo.cuerpo, ["autorId", "usuarioId", "email", "dni", "telefono"]);
    exigir(prohibidas.length === 0, `el hilo del comprador expone: ${prohibidas.join(", ")}`);

    const leida = await post(`/conversaciones/${conversacionId}/leida`, undefined, comprador.token);
    exigir(leida.estado === 200 && leida.cuerpo.noLeidos === 0 && leida.cuerpo.conversaciones === 0, `leída: ${leida.texto}`);
    const despues = await pedir("GET", "/conversaciones/no-leidas", undefined, comprador.token);
    exigir(despues.cuerpo.noLeidos === 0, `contador después de leer: ${despues.texto}`);
    const hiloDespues = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(hiloDespues.cuerpo.mensajes[hiloDespues.cuerpo.mensajes.length - 1].leido === true, "el mensaje de la agencia sigue sin leer");
  });

  await revisar("cuando el comprador vuelve a escribir, el contador del admin sube", async () => {
    const antes = await contadorDelAdmin();
    const enviado = await post(`/conversaciones/${conversacionId}/mensajes`, { texto: "Perfecto, voy mañana." }, comprador.token);
    exigir(enviado.estado === 200, `estado ${enviado.estado}: ${enviado.texto}`);
    exigir((await contadorDelAdmin()) === antes + 1, `el contador del admin no subió (antes ${antes})`);
  });

  await revisar("el admin cierra: nadie escribe en la cerrada, cerrar de nuevo no falla y la bandeja la ve como CERRADA", async () => {
    const cerrada = await post(hiloAdmin("/cerrar"), undefined, admin.token);
    exigir(cerrada.estado === 200 && cerrada.cuerpo.estado === "CERRADA" && cerrada.cuerpo.id === conversacionId,
      `cerrar: estado ${cerrada.estado}: ${cerrada.texto}`);
    exigir(cerrada.cuerpo.usuario && cerrada.cuerpo.usuario.nombre === "Humo", "el resumen no trae el usuario");

    const delComprador = await post(`/conversaciones/${conversacionId}/mensajes`, { texto: "¿Siguen ahí?" }, comprador.token);
    exigir(delComprador.estado === 400 && delComprador.cuerpo.error === "Esta conversación está cerrada.",
      `el comprador escribió en la cerrada: estado ${delComprador.estado}: ${delComprador.texto}`);
    const delAdmin = await post(hiloAdmin("/mensajes"), { texto: "Hola de nuevo" }, admin.token);
    exigir(delAdmin.estado === 400 && delAdmin.cuerpo.error === "Esta conversación está cerrada. Reabrila para responder.",
      `el admin respondió en la cerrada: estado ${delAdmin.estado}: ${delAdmin.texto}`);

    const otraVez = await post(hiloAdmin("/cerrar"), undefined, admin.token);
    exigir(otraVez.estado === 200 && otraVez.cuerpo.estado === "CERRADA", `cerrar de nuevo: estado ${otraVez.estado}`);
    const bandeja = await pedir("GET", "/admin/conversaciones?estado=CERRADA", undefined, admin.token);
    exigir(bandeja.cuerpo.contenido.some((c) => c.id === conversacionId), "la cerrada no aparece entre las cerradas");
    const hilo = await pedir("GET", `/conversaciones/${conversacionId}`, undefined, comprador.token);
    exigir(hilo.cuerpo.conversacion.estado === "CERRADA", "el comprador no ve la conversación cerrada");
    exigir(!hilo.cuerpo.mensajes.some((m) => m.texto === "¿Siguen ahí?" || m.texto === "Hola de nuevo"), "quedó un mensaje en la cerrada");
  });

  await revisar("el admin reabre: la conversación vuelve a abierta y el comprador vuelve a escribir", async () => {
    const reabierta = await post(hiloAdmin("/reabrir"), undefined, admin.token);
    exigir(reabierta.estado === 200 && reabierta.cuerpo.estado === "ABIERTA", `reabrir: estado ${reabierta.estado}: ${reabierta.texto}`);
    const otraVez = await post(hiloAdmin("/reabrir"), undefined, admin.token);
    exigir(otraVez.estado === 200 && otraVez.cuerpo.estado === "ABIERTA", `reabrir una abierta: estado ${otraVez.estado}`);
    const enviado = await post(`/conversaciones/${conversacionId}/mensajes`, { texto: "Ya estoy de vuelta." }, comprador.token);
    exigir(enviado.estado === 200, `el comprador no pudo escribir tras reabrir: estado ${enviado.estado}: ${enviado.texto}`);
    const respuesta = await post(hiloAdmin("/mensajes"), { texto: "Te esperamos." }, admin.token);
    exigir(respuesta.estado === 200, `el admin no pudo responder tras reabrir: estado ${respuesta.estado}`);
    const sinLeer = await post(hiloAdmin("/leida"), undefined, admin.token);
    exigir(sinLeer.estado === 200, `leída: estado ${sinLeer.estado}`);
  });

  await revisar("Lo quiero sobre una cerrada crea una nueva abierta y reabrir la anterior da 400 (una sola abierta por usuario y auto)", async () => {
    const cerrada = await post(hiloAdmin("/cerrar"), undefined, admin.token);
    exigir(cerrada.estado === 200 && cerrada.cuerpo.estado === "CERRADA", `cerrar: estado ${cerrada.estado}`);

    const nueva = await post("/conversaciones", { publicacionId }, comprador.token);
    exigir(nueva.estado === 200, `Lo quiero sobre la cerrada: estado ${nueva.estado}: ${nueva.texto}`);
    exigir(nueva.cuerpo.id !== conversacionId && nueva.cuerpo.estado === "ABIERTA", `nueva: ${JSON.stringify(nueva.cuerpo)}`);

    const rechazo = await post(hiloAdmin("/reabrir"), undefined, admin.token);
    exigir(rechazo.estado === 400 && rechazo.cuerpo.error === "El usuario ya tiene otra conversación abierta por este auto.",
      `reabrir con otra abierta: estado ${rechazo.estado}: ${rechazo.texto}`);

    const anterior = await pedir("GET", hiloAdmin(), undefined, admin.token);
    exigir(anterior.cuerpo.conversacion.estado === "CERRADA", "la anterior quedó abierta");
    const lista = await pedir("GET", "/conversaciones", undefined, comprador.token);
    const abiertasDelAuto = lista.cuerpo.filter((c) => c.publicacion && c.publicacion.id === publicacionId && c.estado === "ABIERTA");
    exigir(abiertasDelAuto.length === 1 && abiertasDelAuto[0].id === nueva.cuerpo.id,
      `abiertas del auto: ${JSON.stringify(abiertasDelAuto.map((c) => c.id))}`);
  });

  await revisar("una cuenta descartable recibe 429 con Retry-After en el envío 21 y ese mensaje no queda en el hilo (D-10)", async () => {
    const descartable = await registrarCuentaVerificada("limite");
    const autoLimiteId = await crearAuto("Limite", "Auto del humo para el límite de envío");
    const abierta = await post("/conversaciones", { publicacionId: autoLimiteId }, descartable.token);
    exigir(abierta.estado === 200, `Lo quiero de la cuenta descartable: estado ${abierta.estado}`);
    const idLimite = abierta.cuerpo.id;

    // El "Lo quiero" fue el envío 1: del 2 al 20 se aceptan y el 21 tiene que dar 429.
    let aceptados = 1;
    let rechazo = null;
    let textoRechazado = null;
    for (let n = 2; n <= 25; n++) {
      const texto = `mensaje-limite-${n}`;
      const r = await post(`/conversaciones/${idLimite}/mensajes`, { texto }, descartable.token);
      if (r.estado === 429) {
        rechazo = r;
        textoRechazado = texto;
        break;
      }
      exigir(r.estado === 200, `mensaje ${n}: estado ${r.estado}: ${r.texto}`);
      aceptados++;
    }
    exigir(rechazo, "no apareció ningún 429 en 25 envíos");
    exigir(aceptados === 20, `el 429 llegó tras ${aceptados} envíos aceptados (esperaba 20, antes del 22)`);
    exigir(Number(rechazo.reintentarEn) > 0, `Retry-After: ${rechazo.reintentarEn}`);
    exigir(rechazo.cuerpo && typeof rechazo.cuerpo.error === "string" && rechazo.cuerpo.error.length > 0, "el 429 no trae mensaje");

    const hilo = await pedir("GET", `/conversaciones/${idLimite}`, undefined, descartable.token);
    exigir(hilo.estado === 200, `leer el hilo después del límite: estado ${hilo.estado}`);
    exigir(hilo.cuerpo.mensajes.length === aceptados, `el hilo tiene ${hilo.cuerpo.mensajes.length} mensajes (esperaba ${aceptados})`);
    exigir(!hilo.cuerpo.mensajes.some((m) => m.texto === textoRechazado), "el mensaje rechazado por el límite quedó guardado");
    // También frena otro "Lo quiero": el límite es de la cuenta, no de la conversación.
    const otroLoQuiero = await post("/conversaciones", { publicacionId: autoLimiteId }, descartable.token);
    exigir(otroLoQuiero.estado === 429, `Lo quiero con el límite agotado: estado ${otroLoQuiero.estado}`);
  });

  // ---- Avisos por mail (MSG-06, D-09, D-11, T-04-25, T-04-26, T-04-27) ----
  // En desarrollo los mails no salen: LogEmailSender los escribe en el log del back, una línea por mail.
  const mailsA = (destinatario, ruta) => {
    const patron = new RegExp(`${ruta}(?![0-9])`);
    return leerLog().split(/\r?\n/).filter((l) => l.includes("[MAIL SOLO LOG")
      && l.toLowerCase().includes(`para=${destinatario.toLowerCase()} `) && patron.test(l));
  };
  // El envío es asíncrono: se espera hasta 10 segundos a que aparezca la línea.
  const esperarMails = async (destinatario, ruta, cantidad) => {
    const limite = Date.now() + 10000;
    for (;;) {
      const lineas = mailsA(destinatario, ruta);
      if (lineas.length >= cantidad) return lineas;
      if (Date.now() > limite) throw new Error(`no apareció en el log el mail para ${ruta} (hay ${lineas.length}, esperaba ${cantidad})`);
      await dormir(250);
    }
  };
  const marcador = `MARCADOR${sufijo}`;
  const rutaAdmin = (id) => `/admin/mensajes/${id}`;
  const rutaUsuario = (id) => `(?<!admin)/mensajes/${id}`;

  await revisar("el Lo quiero del comprador dejó un mail para la cuenta admin con el nombre del comprador y el link de la bandeja", async () => {
    exigirLog();
    const [linea] = await esperarMails(admin.email, rutaAdmin(conversacionId), 1);
    exigir(linea.includes("asunto=Mensaje nuevo en la bandeja de Dante Automotores"), "el asunto no es el fijo de la agencia");
    exigir(linea.includes("Humo Prueba"), "el mail no nombra al comprador");
    exigir(linea.includes(marca), "el mail no nombra el auto");
  });

  await revisar("la respuesta de la agencia dejó un mail para el comprador con el link a su conversación y el asunto fijo", async () => {
    exigirLog();
    const [linea] = await esperarMails(comprador.email, rutaUsuario(conversacionId), 1);
    // El log del back puede no ser UTF-8: del asunto fijo solo se compara la parte ASCII.
    exigir(/asunto=Ten.{1,2}s un mensaje nuevo en Dante Automotores texto=/.test(linea), "el asunto no es el fijo del usuario");
  });

  await revisar("varios mensajes dentro de los 10 minutos mandan un solo mail por conversación y destinatario, y cerrar y reabrir no mandan ninguno", async () => {
    exigirLog();
    const avisos = await registrarCuentaVerificada("avisos");
    const autoAvisosId = await crearAuto("Avisos", "Auto del humo para los avisos por mail");
    const abierta = await post("/conversaciones", { publicacionId: autoAvisosId }, avisos.token);
    exigir(abierta.estado === 200, `Lo quiero de la cuenta de avisos: estado ${abierta.estado}`);
    const id = abierta.cuerpo.id;
    await esperarMails(admin.email, rutaAdmin(id), 1);

    for (const n of [1, 2]) {
      const enviado = await post(`/conversaciones/${id}/mensajes`, { texto: `${marcador} del comprador ${n}` }, avisos.token);
      exigir(enviado.estado === 200, `mensaje ${n} del comprador: estado ${enviado.estado}`);
    }
    for (const n of [1, 2]) {
      const respuesta = await post(`/admin/conversaciones/${id}/mensajes`, { texto: `${marcador} de la agencia ${n}` }, admin.token);
      exigir(respuesta.estado === 200, `respuesta ${n} de la agencia: estado ${respuesta.estado}`);
    }
    await esperarMails(avisos.email, rutaUsuario(id), 1);
    await dormir(3000);
    exigir(mailsA(admin.email, rutaAdmin(id)).length === 1, `mails a la agencia: ${mailsA(admin.email, rutaAdmin(id)).length} (esperaba 1)`);
    exigir(mailsA(avisos.email, rutaUsuario(id)).length === 1, `mails al usuario: ${mailsA(avisos.email, rutaUsuario(id)).length} (esperaba 1)`);

    const cerrada = await post(`/admin/conversaciones/${id}/cerrar`, undefined, admin.token);
    exigir(cerrada.estado === 200, `cerrar: estado ${cerrada.estado}`);
    const reabierta = await post(`/admin/conversaciones/${id}/reabrir`, undefined, admin.token);
    exigir(reabierta.estado === 200, `reabrir: estado ${reabierta.estado}`);
    await dormir(3000);
    exigir(mailsA(admin.email, rutaAdmin(id)).length === 1, "cerrar y reabrir mandaron un mail a la agencia");
    exigir(mailsA(avisos.email, rutaUsuario(id)).length === 1, "cerrar y reabrir mandaron un mail al usuario");
  });

  await revisar("ningún mail de aviso ni el log completo del back contienen el texto de los mensajes", async () => {
    exigirLog();
    const log = leerLog();
    exigir(!log.includes(marcador), "el log del back contiene el texto de un mensaje");
    const avisosDeMensaje = log.split(/\r?\n/).filter((l) => l.includes("[MAIL SOLO LOG") && /\/(admin\/)?mensajes\/[0-9]/.test(l));
    exigir(avisosDeMensaje.length >= 3, `se esperaban al menos 3 mails de aviso en el log y hay ${avisosDeMensaje.length}`);
    for (const linea of avisosDeMensaje) {
      for (const texto of ["Hola, pasá cuando quieras", "Perfecto, voy mañana", "Ya estoy de vuelta", "Te esperamos"]) {
        exigir(!linea.includes(texto), `un mail de aviso lleva el texto de un mensaje: ${texto}`);
      }
    }
  });

  // ---- Limpieza de los autos de prueba (la base es descartable, pero el humo no deja basura si se reusa) ----
  await revisar("el admin borra los autos de prueba y se lleva sus conversaciones", async () => {
    for (const id of autosCreados) {
      const borrado = await pedir("DELETE", `/publicaciones/${id}`, undefined, admin.token);
      exigir(borrado.estado === 204 || borrado.estado === 200, `borrar ${id}: estado ${borrado.estado}: ${borrado.texto}`);
    }
    const despues = await pedir("GET", "/conversaciones", undefined, comprador.token);
    exigir(despues.cuerpo.length === 0, `quedaron ${despues.cuerpo.length} conversaciones`);
  });

  cerrar();
})().catch((e) => {
  console.error("humo: error inesperado:", e.message);
  process.exitCode = 1;
});
