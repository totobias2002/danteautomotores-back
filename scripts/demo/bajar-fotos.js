const fs = require("fs");
const path = require("path");
const DIR = path.join(__dirname, "fotos");
fs.mkdirSync(DIR, { recursive: true });
const UA = { "User-Agent": "DanteAutomotoresDemoSeed/1.0 (script de carga de demo de danteautomotores-back)" };
const sets = JSON.parse(fs.readFileSync(path.join(__dirname, "fotos.json"), "utf8"));
const strip = (h) => (h || "").replace(/<[^>]*>/g, "").replace(/\s+/g, " ").trim();
(async () => {
  const creditos = [];
  for (const [clave, titulos] of Object.entries(sets)) {
    const url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|extmetadata&iiextmetadatafilter=LicenseShortName|Artist&iiurlwidth=1280&titles=" + encodeURIComponent(titulos.map(t => "File:" + t).join("|"));
    const j = await (await fetch(url, { headers: UA })).json();
    const porTitulo = Object.fromEntries(Object.values(j.query.pages).map(p => [p.title, p]));
    let n = 0;
    for (const t of titulos) {
      const p = porTitulo["File:" + t] || Object.values(porTitulo).find(x => x.title.replace(/_/g, " ") === "File:" + t);
      if (!p || !p.imageinfo) { console.log("FALTA", clave, t); continue; }
      const i = p.imageinfo[0]; n++;
      const buf = Buffer.from(await (await fetch(i.thumburl, { headers: UA })).arrayBuffer());
      fs.writeFileSync(path.join(DIR, `${clave}-${n}.jpg`), buf);
      creditos.push({ archivo: `${clave}-${n}.jpg`, titulo: t, autor: strip(i.extmetadata.Artist?.value), licencia: i.extmetadata.LicenseShortName?.value, fuente: i.descriptionurl, kb: Math.round(buf.length / 1024) });
      await new Promise(r => setTimeout(r, 300));
    }
  }
  fs.writeFileSync(path.join(DIR, "creditos.json"), JSON.stringify(creditos, null, 1));
  console.log("descargadas", creditos.length); for (const c of creditos) if (c.kb < 20 || c.kb > 3000) console.log("tamaño raro", c.archivo, c.kb);
})();
