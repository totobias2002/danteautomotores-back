const test = require("node:test");
const assert = require("node:assert/strict");
const path = require("path");
const { cuerpoParaCompletarZona, planDeDeshacer, rutaDelRegistro } = require("./sembrar-demo.js");

const existente = {
  id: 7,
  nombre: "Norte Motors",
  slug: "norte-motors",
  logo: "https://res.cloudinary.com/x/logo.png",
  descripcion: "Descripcion real",
  direccion: "Calle Real 123",
  telefonoContacto: "011 1111-1111",
  emailContacto: "real@norte.com",
  zona: null,
  fechaAlta: "2026-01-01T00:00:00",
};
const demo = {
  nombre: "Norte Motors",
  zona: "ZONA_NORTE",
  direccion: "Av. de ejemplo 1",
  telefonoContacto: "011 4555-0202",
  emailContacto: "contacto@nortemotors.example",
  descripcion: "Descripcion de ejemplo",
};

test("cuerpoParaCompletarZona reenvia los datos de la agencia existente y suma solo la zona de la demo", () => {
  for (const sinZona of [null, undefined, ""]) {
    assert.deepEqual(cuerpoParaCompletarZona({ ...existente, zona: sinZona }, demo), {
      nombre: "Norte Motors",
      logo: "https://res.cloudinary.com/x/logo.png",
      descripcion: "Descripcion real",
      direccion: "Calle Real 123",
      telefonoContacto: "011 1111-1111",
      emailContacto: "real@norte.com",
      zona: "ZONA_NORTE",
    });
  }
});

test("cuerpoParaCompletarZona no manda id, slug, fechaAlta ni datos de ejemplo", () => {
  const cuerpo = cuerpoParaCompletarZona(existente, demo);
  for (const campo of ["id", "slug", "fechaAlta"]) assert.equal(campo in cuerpo, false, campo);
  assert.notEqual(cuerpo.direccion, demo.direccion);
  assert.notEqual(cuerpo.telefonoContacto, demo.telefonoContacto);
  assert.notEqual(cuerpo.emailContacto, demo.emailContacto);
  assert.notEqual(cuerpo.descripcion, demo.descripcion);
});

test("cuerpoParaCompletarZona devuelve null si la agencia ya tiene zona o la demo no trae zona", () => {
  assert.equal(cuerpoParaCompletarZona({ ...existente, zona: "CABA" }, demo), null);
  assert.equal(cuerpoParaCompletarZona(existente, { ...demo, zona: undefined }), null);
  assert.equal(cuerpoParaCompletarZona(existente, { ...demo, zona: "" }), null);
});

test("planDeDeshacer borra un auto registrado que sigue en el backend con los mismos datos", () => {
  const registro = { publicaciones: [{ id: 10, marca: "Toyota", modelo: "Corolla XEI", anio: 2022 }], agencias: [] };
  const actuales = [{ id: 10, marca: "Toyota", modelo: "Corolla XEI", anio: 2022, precio: 1 }];
  const plan = planDeDeshacer(registro, actuales, []);
  assert.deepEqual(plan.publicaciones.borrar, registro.publicaciones);
  assert.deepEqual(plan.publicaciones.yaNoEstan, []);
  assert.deepEqual(plan.publicaciones.noCoinciden, []);
});

test("planDeDeshacer separa los autos que ya no estan y los que cambiaron de datos", () => {
  const registro = {
    publicaciones: [
      { id: 10, marca: "Toyota", modelo: "Corolla XEI", anio: 2022 },
      { id: 11, marca: "Jeep", modelo: "Compass Limited", anio: 2023 },
      { id: 12, marca: "Ford", modelo: "Ranger XLT", anio: 2020 },
    ],
    agencias: [],
  };
  const actuales = [
    { id: 10, marca: "Toyota", modelo: "Corolla XEI", anio: 2022 },
    { id: 12, marca: "Ford", modelo: "Ranger XLT", anio: 2021 },
  ];
  const plan = planDeDeshacer(registro, actuales, []);
  assert.deepEqual(plan.publicaciones.borrar.map((p) => p.id), [10]);
  assert.deepEqual(plan.publicaciones.yaNoEstan.map((p) => p.id), [11]);
  assert.deepEqual(plan.publicaciones.noCoinciden.map((p) => p.id), [12]);
});

test("planDeDeshacer compara agencias por id y nombre", () => {
  const registro = {
    publicaciones: [],
    agencias: [
      { id: 1, nombre: "Norte Motors" },
      { id: 2, nombre: "Premium Hub" },
      { id: 3, nombre: "Garage 21" },
    ],
  };
  const actuales = [
    { id: 1, nombre: "Norte Motors", slug: "norte-motors" },
    { id: 3, nombre: "Otro nombre" },
  ];
  const plan = planDeDeshacer(registro, [], actuales);
  assert.deepEqual(plan.agencias.borrar.map((a) => a.id), [1]);
  assert.deepEqual(plan.agencias.yaNoEstan.map((a) => a.id), [2]);
  assert.deepEqual(plan.agencias.noCoinciden.map((a) => a.id), [3]);
});

test("planDeDeshacer con un registro sin listas o vacio da todas las listas vacias", () => {
  for (const registro of [{}, { publicaciones: [], agencias: [] }]) {
    const plan = planDeDeshacer(registro, [{ id: 1, marca: "X", modelo: "Y", anio: 2000 }], [{ id: 1, nombre: "Z" }]);
    for (const grupo of [plan.publicaciones, plan.agencias]) {
      assert.deepEqual(grupo, { borrar: [], yaNoEstan: [], noCoinciden: [] });
    }
  }
});

test("rutaDelRegistro respeta REGISTRO si esta definido", () => {
  assert.equal(rutaDelRegistro("http://localhost:8080/api", { REGISTRO: "/tmp/mi-registro.json" }), "/tmp/mi-registro.json");
});

test("rutaDelRegistro arma scripts/demo/registros/<host>.json con el host saneado", () => {
  const local = rutaDelRegistro("http://localhost:8080/api", {});
  assert.equal(path.basename(local), "localhost_8080.json");
  assert.equal(path.basename(path.dirname(local)), "registros");
  assert.equal(path.dirname(path.dirname(local)), __dirname);

  const remoto = rutaDelRegistro("https://Dante-Back.up.railway.app/api", {});
  assert.equal(path.basename(remoto), "dante-back.up.railway.app.json");
});

test("rutaDelRegistro sanea cualquier caracter fuera de [a-z0-9.-]", () => {
  const ruta = rutaDelRegistro("http://usuario:clave@Host_Raro.example:9090/api", {});
  assert.match(path.basename(ruta), /^[a-z0-9._-]+\.json$/);
  assert.equal(path.basename(path.dirname(ruta)), "registros");
  // una URL ilegible tampoco puede salirse de la carpeta de registros
  const ilegible = rutaDelRegistro("../../etc/passwd", {});
  assert.equal(path.basename(path.dirname(ilegible)), "registros");
  assert.match(path.basename(ilegible), /^[a-z0-9._-]+\.json$/);
});
