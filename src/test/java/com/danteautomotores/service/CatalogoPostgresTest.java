package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.FacetasResponse;
import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.enums.Transmision;
import com.danteautomotores.enums.ZonaAgencia;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.FotoPublicacionRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.support.PostgresLocalTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * El catálogo público contra un PostgreSQL real, con el esquema creado por Flyway y validado por Hibernate
 * (ddl-auto=validate): si la entidad no calzara con las migraciones, el contexto ni siquiera arranca.
 */
@Import({CatalogoService.class, CatalogoPostgresTest.RelojFijo.class})
class CatalogoPostgresTest extends PostgresLocalTestBase {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 10, 10, 0);
    // "Ahora" del servicio en estos tests: las fechas de publicación y de venta se fijan relativas a este reloj.
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 12, 0);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private CatalogoService catalogoService;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private AgenciaRepository agenciaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private FotoPublicacionRepository fotoPublicacionRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager em;

    private Usuario admin;
    private Agencia agencia;

    @BeforeEach
    void fixtureBase() {
        admin = usuarioRepository.save(Usuario.builder()
                .nombre("Admin").email("admin@dante.test").passwordHash("hash-secreto").rol(Rol.ADMIN).build());
        agencia = agenciaRepository.save(Agencia.builder().nombre("Agencia Test").slug("agencia-test").build());
    }

    /** Guarda un auto y fija por SQL la fecha de publicación y el estado (el @PrePersist y el builder los pisan). */
    private Long auto(String modelo, boolean destacado, String estado, LocalDateTime fecha) {
        Publicacion p = publicacionRepository.saveAndFlush(Publicacion.builder()
                .agencia(agencia).admin(admin).marca("Marca").modelo(modelo).anio(2022)
                .precio(new BigDecimal("1000000.00")).destacado(destacado).descripcion("descripcion larga").build());
        jdbc.update("UPDATE publicaciones SET fecha_publicacion = ?, estado = ? WHERE id = ?",
                Timestamp.valueOf(fecha), estado, p.getId());
        em.clear();
        return p.getId();
    }

    @Test
    void destacadosDevuelveSoloLosVisiblesOrdenadosPorFechaYLuegoPorIdDescendente() {
        Long a = auto("A", true, "DISPONIBLE", BASE.minusDays(1));
        Long b = auto("B", true, "DISPONIBLE", BASE.minusDays(1)); // misma fecha que A: desempata el id
        Long c = auto("C", true, "DISPONIBLE", BASE.minusDays(5));
        Long d = auto("D", true, "RESERVADO", BASE);
        auto("E", true, "VENDIDO", BASE.plusDays(1));
        Long f = auto("F", true, null, BASE.minusDays(3)); // estado NULL cuenta como visible
        auto("G", false, "DISPONIBLE", BASE.plusDays(2));
        auto("H", false, "DISPONIBLE", BASE.plusDays(3));

        List<PublicacionResumenResponse> resultado = catalogoService.destacados(null);

        assertThat(resultado).extracting(PublicacionResumenResponse::getId).containsExactly(d, b, a, f, c);
        assertThat(resultado).extracting(PublicacionResumenResponse::getEstado)
                .doesNotContain(EstadoPublicacion.VENDIDO);
    }

    @Test
    void destacadosAcotaElLimiteEntre1y12() {
        for (int i = 0; i < 14; i++) {
            auto("M" + i, true, "DISPONIBLE", BASE.minusHours(i));
        }

        assertThat(catalogoService.destacados(null)).hasSize(6);
        assertThat(catalogoService.destacados(2)).hasSize(2);
        assertThat(catalogoService.destacados(0)).hasSize(1);
        assertThat(catalogoService.destacados(-5)).hasSize(1);
        assertThat(catalogoService.destacados(50)).hasSize(12);
    }

    @Test
    void fotoPortadaEsLaDeMenorOrdenYNullSiNoHayFotos() {
        Long conFotos = auto("ConFotos", true, "DISPONIBLE", BASE);
        Long sinFotos = auto("SinFotos", true, "DISPONIBLE", BASE.minusDays(1));
        Publicacion p = publicacionRepository.findById(conFotos).orElseThrow();
        fotoPublicacionRepository.save(FotoPublicacion.builder().publicacion(p).url("https://fotos.test/orden-2.jpg").orden(2).build());
        fotoPublicacionRepository.save(FotoPublicacion.builder().publicacion(p).url("https://fotos.test/orden-0.jpg").orden(0).build());
        fotoPublicacionRepository.save(FotoPublicacion.builder().publicacion(p).url("https://fotos.test/orden-1.jpg").orden(1).build());
        em.flush();
        em.clear();

        List<PublicacionResumenResponse> resultado = catalogoService.destacados(6);

        assertThat(resultado).extracting(PublicacionResumenResponse::getId).containsExactly(conFotos, sinFotos);
        assertThat(resultado.get(0).getFotoPortada()).isEqualTo("https://fotos.test/orden-0.jpg");
        assertThat(resultado.get(1).getFotoPortada()).isNull();
    }

    @Test
    void elResumenTraeLosDatosDelAutoYDeSuAgencia() {
        auto("Corolla", true, "DISPONIBLE", BASE);

        PublicacionResumenResponse r = catalogoService.destacados(1).get(0);

        assertThat(r.getModelo()).isEqualTo("Corolla");
        assertThat(r.getPrecio()).isEqualByComparingTo("1000000.00");
        assertThat(r.getAgenciaId()).isEqualTo(agencia.getId());
        assertThat(r.getAgenciaNombre()).isEqualTo("Agencia Test");
        assertThat(r.getAgenciaSlug()).isEqualTo("agencia-test");
        assertThat(r.isDestacado()).isTrue();
    }

    @Test
    void elResumenSerializadoNoExponeAdminNiDescripcionNiFotos() throws Exception {
        Long id = auto("Serializado", true, "DISPONIBLE", BASE);
        Publicacion p = publicacionRepository.findById(id).orElseThrow();
        fotoPublicacionRepository.save(FotoPublicacion.builder().publicacion(p).url("https://fotos.test/x.jpg").orden(0).build());
        em.flush();
        em.clear();

        PublicacionResumenResponse resumen = PublicacionMapper.toResumen(publicacionRepository.findById(id).orElseThrow());
        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(resumen);

        assertThat(json.has("fotoPortada")).isTrue();
        assertThat(json.has("admin")).isFalse();
        assertThat(json.has("email")).isFalse();
        assertThat(json.has("passwordHash")).isFalse();
        assertThat(json.has("descripcion")).isFalse();
        assertThat(json.has("fotos")).isFalse();
        assertThat(json.toString()).doesNotContain("admin@dante.test").doesNotContain("hash-secreto")
                .doesNotContain("descripcion larga");
    }

    // ---- Listado paginado (plan 02-04) ----

    private Publicacion.PublicacionBuilder base(String marca, String modelo) {
        return Publicacion.builder().agencia(agencia).admin(admin).marca(marca).modelo(modelo).anio(2022)
                .precio(new BigDecimal("1000000.00")).descripcion("descripcion larga");
    }

    /** Guarda el auto y fija por SQL estado, fecha de publicación y fecha de venta (el builder y el @PrePersist las pisan). */
    private Long guardar(Publicacion.PublicacionBuilder builder, String estado, LocalDateTime publicado, LocalDateTime vendido) {
        Publicacion p = publicacionRepository.saveAndFlush(builder.build());
        jdbc.update("UPDATE publicaciones SET fecha_publicacion = ?, estado = ?, fecha_vendido = ? WHERE id = ?",
                Timestamp.valueOf(publicado), estado, vendido == null ? null : Timestamp.valueOf(vendido), p.getId());
        em.clear();
        return p.getId();
    }

    private Long disponible(Publicacion.PublicacionBuilder builder) {
        return guardar(builder, "DISPONIBLE", AHORA.minusDays(2), null);
    }

    private static List<Long> ids(PaginaResponse<PublicacionResumenResponse> pagina) {
        return pagina.contenido().stream().map(PublicacionResumenResponse::getId).toList();
    }

    @Test
    void paginaDe24ConLaSegundaPaginaSinRepetirNiSaltear() {
        for (int i = 0; i < 30; i++) {
            guardar(base("Marca", "M" + i), "DISPONIBLE", AHORA.minusDays(2), null); // misma fecha: el id desempata
        }
        FiltrosCatalogo f = new FiltrosCatalogo();

        PaginaResponse<PublicacionResumenResponse> p1 = catalogoService.buscar(f);
        f.setPagina(2);
        PaginaResponse<PublicacionResumenResponse> p2 = catalogoService.buscar(f);

        assertThat(p1.contenido()).hasSize(24);
        assertThat(p2.contenido()).hasSize(6);
        assertThat(p1.pagina()).isEqualTo(1);
        assertThat(p2.pagina()).isEqualTo(2);
        assertThat(p1.tamanio()).isEqualTo(24);
        assertThat(p1.totalElementos()).isEqualTo(30);
        assertThat(p1.totalPaginas()).isEqualTo(2);
        List<Long> todos = new java.util.ArrayList<>(ids(p1));
        todos.addAll(ids(p2));
        assertThat(todos).doesNotHaveDuplicates().hasSize(30);
        assertThat(todos).isSortedAccordingTo(java.util.Comparator.reverseOrder()); // misma fecha y sin destacados: id desc

        f.setPagina(5);
        PaginaResponse<PublicacionResumenResponse> lejana = catalogoService.buscar(f);
        assertThat(lejana.contenido()).isEmpty();
        assertThat(lejana.totalPaginas()).isEqualTo(2);
    }

    @Test
    void relevanciaPoneLosVendidosAlFinalLuegoDestacadosLuegoMasRecientesYPorUltimoElId() {
        Long vendido = guardar(base("M", "vendido").destacado(true), "VENDIDO", AHORA, AHORA.minusDays(1));
        Long viejo = guardar(base("M", "viejo"), "DISPONIBLE", AHORA.minusDays(10), null);
        Long nuevoA = guardar(base("M", "nuevoA"), "DISPONIBLE", AHORA.minusDays(1), null);
        Long nuevoB = guardar(base("M", "nuevoB"), "DISPONIBLE", AHORA.minusDays(1), null); // misma fecha que nuevoA
        Long destacadoViejo = guardar(base("M", "destacado").destacado(true), "DISPONIBLE", AHORA.minusDays(20), null);

        PaginaResponse<PublicacionResumenResponse> pagina = catalogoService.buscar(new FiltrosCatalogo());

        assertThat(ids(pagina)).containsExactly(destacadoViejo, nuevoB, nuevoA, viejo, vendido);
    }

    @Test
    void ordenPorPrecioAnioYKmConLosVendidosSiempreAlFinal() {
        Long barato = disponible(base("M", "barato").precio(new BigDecimal("500000")).anio(2018).kilometraje(90000));
        Long caro = disponible(base("M", "caro").precio(new BigDecimal("900000")).anio(2024).kilometraje(10000));
        Long sinKm = disponible(base("M", "sinkm").precio(new BigDecimal("700000")).anio(2020));
        Long vendidoBarato = guardar(base("M", "vendidoBarato").precio(new BigDecimal("100000")).anio(2025).kilometraje(1),
                "VENDIDO", AHORA.minusDays(3), AHORA.minusDays(1));

        assertThat(ids(ordenado("precio_asc"))).containsExactly(barato, sinKm, caro, vendidoBarato);
        assertThat(ids(ordenado("PRECIO_DESC"))).containsExactly(caro, sinKm, barato, vendidoBarato);
        assertThat(ids(ordenado("anio_desc"))).containsExactly(caro, sinKm, barato, vendidoBarato);
        // km_asc: el que no tiene km queda al final de los no vendidos, y el vendido después de todos.
        assertThat(ids(ordenado("km_asc"))).containsExactly(caro, barato, sinKm, vendidoBarato);
    }

    private PaginaResponse<PublicacionResumenResponse> ordenado(String orden) {
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setOrden(orden);
        return catalogoService.buscar(f);
    }

    @Test
    void unVendidoSoloApareceSiSeVendioHace30DiasOMenos() {
        Long hace29 = guardar(base("M", "hace29"), "VENDIDO", AHORA.minusDays(60), AHORA.minusDays(29));
        guardar(base("M", "hace31"), "VENDIDO", AHORA.minusDays(60), AHORA.minusDays(31));
        guardar(base("M", "sinFecha"), "VENDIDO", AHORA.minusDays(60), null);
        Long reservado = guardar(base("M", "reservado"), "RESERVADO", AHORA.minusDays(60), null);
        Long sinEstado = guardar(base("M", "sinEstado"), null, AHORA.minusDays(60), null);

        List<PublicacionResumenResponse> contenido = catalogoService.buscar(new FiltrosCatalogo()).contenido();

        assertThat(contenido).extracting(PublicacionResumenResponse::getId)
                .containsExactlyInAnyOrder(hace29, reservado, sinEstado);
        // El visitante ve el estado que cargó el admin: RESERVADO como RESERVADO y VENDIDO como VENDIDO.
        assertThat(contenido).filteredOn(r -> r.getId().equals(reservado)).extracting(PublicacionResumenResponse::getEstado)
                .containsExactly(EstadoPublicacion.RESERVADO);
        assertThat(contenido.get(contenido.size() - 1).getId()).isEqualTo(hace29); // el vendido va al final
    }

    @Test
    void marcaYModeloSonExactosSinDistinguirMayusculasYAdmitenVariosValores() {
        Long toyota = disponible(base("Toyota", "Corolla"));
        Long ford = disponible(base("Ford", "Focus"));
        disponible(base("Toyotomi", "Zeta"));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setMarca(List.of("toyota"));
        assertThat(ids(catalogoService.buscar(f))).containsExactly(toyota);

        f.setMarca(List.of("Toyota", "FORD"));
        assertThat(ids(catalogoService.buscar(f))).containsExactlyInAnyOrder(toyota, ford);

        f.setMarca(null);
        f.setModelo(List.of("focus"));
        assertThat(ids(catalogoService.buscar(f))).containsExactly(ford);
    }

    @Test
    void zonaFiltraPorLaZonaDeLaAgenciaDelAuto() {
        Agencia norte = agenciaRepository.save(Agencia.builder().nombre("Norte").slug("norte").zona(ZonaAgencia.ZONA_NORTE).build());
        Long enNorte = disponible(base("M", "enNorte").agencia(norte));
        disponible(base("M", "enSinZona")); // la agencia de test no tiene zona

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setZona(List.of(ZonaAgencia.ZONA_NORTE));
        assertThat(ids(catalogoService.buscar(f))).containsExactly(enNorte);
        assertThat(catalogoService.buscar(f).contenido().get(0).getAgenciaZona()).isEqualTo(ZonaAgencia.ZONA_NORTE);

        f.setZona(List.of(ZonaAgencia.CABA));
        assertThat(catalogoService.buscar(f).contenido()).isEmpty();
    }

    @Test
    void ofertasDejaSoloLosQueTienenPrecioAnteriorMayorAlPrecio() {
        Long oferta = disponible(base("M", "oferta").precioAnterior(new BigDecimal("1200000.00")));
        disponible(base("M", "igual").precioAnterior(new BigDecimal("1000000.00")));
        disponible(base("M", "menor").precioAnterior(new BigDecimal("900000.00")));
        disponible(base("M", "sinAnterior"));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setOfertas(true);
        PaginaResponse<PublicacionResumenResponse> pagina = catalogoService.buscar(f);

        assertThat(ids(pagina)).containsExactly(oferta);
        assertThat(pagina.contenido().get(0).isOferta()).isTrue(); // la misma regla que el mapper
        f.setOfertas(false);
        assertThat(catalogoService.buscar(f).contenido()).hasSize(4);
    }

    @Test
    void kmMaxExcluyeLosAutosSinKilometraje() {
        Long poco = disponible(base("M", "poco").kilometraje(20000));
        disponible(base("M", "mucho").kilometraje(150000));
        disponible(base("M", "sinKm"));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setKmMax(50000);

        assertThat(ids(catalogoService.buscar(f))).containsExactly(poco);
    }

    @Test
    void laBusquedaExigeCadaPalabraEnMarcaYModelo() {
        Long xei = disponible(base("Toyota", "Corolla XEI"));
        disponible(base("Toyota", "Corolla Cross"));
        disponible(base("Honda", "Civic"));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setBusqueda("corolla xei");
        assertThat(ids(catalogoService.buscar(f))).containsExactly(xei);

        f.setBusqueda("toyota XEI"); // palabras de marca y de modelo, sin importar mayúsculas
        assertThat(ids(catalogoService.buscar(f))).containsExactly(xei);

        f.setBusqueda("corolla");
        assertThat(catalogoService.buscar(f).contenido()).hasSize(2);
    }

    @Test
    void laBusquedaTrataPorcentajeYGuionBajoComoTextoYNoComoComodin() {
        Long conPorcentaje = disponible(base("M", "Edicion 100%"));
        disponible(base("M", "Edicion 1000"));
        Long conGuion = disponible(base("M", "Serie_A"));
        disponible(base("M", "SerieXA"));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setBusqueda("100%");
        assertThat(ids(catalogoService.buscar(f))).containsExactly(conPorcentaje);

        f.setBusqueda("serie_a");
        assertThat(ids(catalogoService.buscar(f))).containsExactly(conGuion);

        f.setBusqueda("%");
        assertThat(ids(catalogoService.buscar(f))).containsExactly(conPorcentaje);
    }

    @Test
    void agenciaIdFiltraPorAgencia() {
        Agencia otra = agenciaRepository.save(Agencia.builder().nombre("Otra").slug("otra").build());
        disponible(base("M", "deLaPrimera"));
        Long deOtra = disponible(base("M", "deOtra").agencia(otra));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setAgenciaId(otra.getId());

        assertThat(ids(catalogoService.buscar(f))).containsExactly(deOtra);
    }

    @Test
    void tipoYPrecioMaximoSeCombinanConAnd() {
        Long sedanBarato = disponible(base("M", "sedanBarato").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("500000")));
        disponible(base("M", "sedanCaro").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("900000")));
        disponible(base("M", "suvBarata").tipoCarroceria(TipoCarroceria.SUV).precio(new BigDecimal("400000")));
        disponible(base("M", "sinTipo").precio(new BigDecimal("300000")));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setTipo(List.of(TipoCarroceria.SEDAN));
        f.setPrecioMax(new BigDecimal("600000"));

        assertThat(ids(catalogoService.buscar(f))).containsExactly(sedanBarato);
    }

    @Test
    void anioPrecioMinimoYTransmisionFiltranPorRango() {
        Long ok = disponible(base("M", "ok").anio(2021).precio(new BigDecimal("800000")).transmision(Transmision.AUTOMATICA));
        disponible(base("M", "viejo").anio(2015).precio(new BigDecimal("800000")).transmision(Transmision.AUTOMATICA));
        disponible(base("M", "manual").anio(2021).precio(new BigDecimal("800000")).transmision(Transmision.MANUAL));
        disponible(base("M", "barato").anio(2021).precio(new BigDecimal("100000")).transmision(Transmision.AUTOMATICA));

        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setAnioMin(2020);
        f.setAnioMax(2023);
        f.setPrecioMin(new BigDecimal("500000"));
        f.setTransmision(List.of(Transmision.AUTOMATICA));

        assertThat(ids(catalogoService.buscar(f))).containsExactly(ok);
    }

    // ---- Facetas (plan 02-04) ----

    @Test
    void lasFacetasCuentanSoloLosVisiblesYOrdenanLasMarcas() {
        disponible(base("Toyota", "Corolla").precio(new BigDecimal("1000")));
        disponible(base("Toyota", "Yaris").precio(new BigDecimal("2000")));
        disponible(base("Ford", "Focus").precio(new BigDecimal("3000")));
        guardar(base("Honda", "Civic").precio(new BigDecimal("999999")), "VENDIDO", AHORA.minusDays(60), AHORA.minusDays(31));

        FacetasResponse f = catalogoService.facetas(null);

        assertThat(f.getMarcas()).extracting(FacetasResponse.Conteo::getValor, FacetasResponse.Conteo::getCantidad)
                .containsExactly(tuple("Ford", 1L), tuple("Toyota", 2L)); // Honda vendido hace 31 días no suma
        assertThat(f.getModelos()).extracting(FacetasResponse.ConteoModelo::getMarca, FacetasResponse.ConteoModelo::getValor,
                        FacetasResponse.ConteoModelo::getCantidad)
                .containsExactly(tuple("Ford", "Focus", 1L), tuple("Toyota", "Corolla", 1L), tuple("Toyota", "Yaris", 1L));
        assertThat(f.getPrecio().getMin()).isEqualByComparingTo("1000");
        assertThat(f.getPrecio().getMax()).isEqualByComparingTo("3000"); // el vendido oculto no estira el rango
        assertThat(f.getPrecio().getHistograma()).hasSize(16);
        assertThat(f.getPrecio().getHistograma().stream().mapToLong(FacetasResponse.TramoPrecio::getCantidad).sum()).isEqualTo(3);
    }

    @Test
    void lasFacetasDeEnumsIgnoranElNullYUsanElNombreDelEnum() {
        Agencia sur = agenciaRepository.save(Agencia.builder().nombre("Sur").slug("sur").zona(ZonaAgencia.ZONA_SUR).build());
        disponible(base("M", "a").tipoCarroceria(TipoCarroceria.SUV).transmision(Transmision.AUTOMATICA).agencia(sur));
        disponible(base("M", "b").tipoCarroceria(TipoCarroceria.SUV).transmision(Transmision.MANUAL).agencia(sur));
        disponible(base("M", "c").tipoCarroceria(TipoCarroceria.SEDAN)); // sin transmisión ni zona
        guardar(base("M", "d"), "RESERVADO", AHORA.minusDays(2), null);
        guardar(base("M", "e"), null, AHORA.minusDays(2), null); // estado null: visible pero sin conteo de estado

        FacetasResponse f = catalogoService.facetas(null);

        assertThat(f.getTipos()).extracting(FacetasResponse.Conteo::getValor, FacetasResponse.Conteo::getCantidad)
                .containsExactly(tuple("SEDAN", 1L), tuple("SUV", 2L));
        assertThat(f.getZonas()).extracting(FacetasResponse.Conteo::getValor, FacetasResponse.Conteo::getCantidad)
                .containsExactly(tuple("ZONA_SUR", 2L));
        assertThat(f.getTransmisiones()).extracting(FacetasResponse.Conteo::getValor).containsExactly("MANUAL", "AUTOMATICA");
        assertThat(f.getEstados()).extracting(FacetasResponse.Conteo::getValor, FacetasResponse.Conteo::getCantidad)
                .containsExactly(tuple("DISPONIBLE", 3L), tuple("RESERVADO", 1L));
    }

    @Test
    void losColoresSeAgrupanSinDistinguirMayusculasYLosRangosIgnoranLosNull() {
        disponible(base("M", "a").color("Blanco").anio(2019).kilometraje(50000));
        disponible(base("M", "b").color("blanco").anio(2024).kilometraje(10000));
        disponible(base("M", "c").color("Negro").anio(2021)); // sin km
        disponible(base("M", "d")); // sin color

        FacetasResponse f = catalogoService.facetas(null);

        assertThat(f.getColores()).extracting(FacetasResponse.Conteo::getCantidad).containsExactly(2L, 1L);
        assertThat(f.getColores().get(0).getValor()).isEqualToIgnoringCase("blanco");
        assertThat(f.getColores().get(1).getValor()).isEqualTo("Negro");
        assertThat(f.getAnio().getMin()).isEqualTo(2019);
        assertThat(f.getAnio().getMax()).isEqualTo(2024);
        assertThat(f.getKilometraje().getMin()).isEqualTo(10000);
        assertThat(f.getKilometraje().getMax()).isEqualTo(50000);
    }

    @Test
    void lasFacetasPorAgenciaCuentanSoloLosAutosDeEsaAgencia() {
        Agencia otra = agenciaRepository.save(Agencia.builder().nombre("Otra").slug("otra").build());
        disponible(base("Toyota", "a"));
        disponible(base("Toyota", "b"));
        disponible(base("Ford", "c").agencia(otra));

        FacetasResponse deOtra = catalogoService.facetas(otra.getId());
        FacetasResponse todas = catalogoService.facetas(null);

        assertThat(deOtra.getMarcas()).extracting(FacetasResponse.Conteo::getValor).containsExactly("Ford");
        assertThat(todas.getMarcas()).extracting(FacetasResponse.Conteo::getCantidad).containsExactly(1L, 2L);
    }

    @Test
    void sinAutosVisiblesLasFacetasNoTienenPrecio() {
        FacetasResponse f = catalogoService.facetas(null);

        assertThat(f.getPrecio()).isNull();
        assertThat(f.getMarcas()).isEmpty();
        assertThat(f.getAnio().getMin()).isNull();
    }

    // ---- Autos parecidos (plan 02-07, D-06) ----

    @Test
    void similaresFiltraPorEstadoMonedaPrecioYTipoOMarcaYOrdenaPorCercaniaDePrecio() {
        Long elAuto = disponible(base("Toyota", "Corolla").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("20000000")));
        Long mismaMarcaOtroTipo = disponible(base("Toyota", "Yaris").tipoCarroceria(TipoCarroceria.HATCHBACK).precio(new BigDecimal("19000000")));
        Long mismoTipoOtraMarca = disponible(base("Ford", "Focus").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("22000000")));
        disponible(base("Ford", "EcoSport").tipoCarroceria(TipoCarroceria.SUV).precio(new BigDecimal("21000000"))); // otra marca y otro tipo
        disponible(base("Toyota", "Camry").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("27000000"))); // sobre 130 %
        disponible(base("Toyota", "Etios").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("13000000"))); // bajo 70 %
        guardar(base("Toyota", "Reservado").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("20000000")), "RESERVADO", AHORA.minusDays(2), null);
        guardar(base("Toyota", "Vendido").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("20000000")), "VENDIDO", AHORA.minusDays(2), AHORA.minusDays(1));
        disponible(base("Toyota", "EnDolares").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("20000")).moneda("USD"));

        List<PublicacionResumenResponse> resultado = catalogoService.similares(elAuto, null);

        // Distancia 1.000.000 (Yaris) antes que 2.000.000 (Focus); el propio auto nunca aparece.
        assertThat(resultado).extracting(PublicacionResumenResponse::getId)
                .containsExactly(mismaMarcaOtroTipo, mismoTipoOtraMarca);
    }

    @Test
    void similaresDesempataPorIdDescendenteAIgualDistanciaDePrecio() {
        Long elAuto = disponible(base("Toyota", "Corolla").precio(new BigDecimal("1000000")));
        Long mas = disponible(base("Toyota", "Mas").precio(new BigDecimal("1100000")));
        Long menos = disponible(base("Toyota", "Menos").precio(new BigDecimal("900000")));

        assertThat(catalogoService.similares(elAuto, null)).extracting(PublicacionResumenResponse::getId)
                .containsExactly(menos, mas);
    }

    @Test
    void similaresSinTipoDeCarroceriaSoloConsideraLaMismaMarcaSinDistinguirMayusculas() {
        Long elAuto = disponible(base("Toyota", "Corolla").precio(new BigDecimal("1000000")));
        Long mismaMarca = disponible(base("TOYOTA", "Yaris").tipoCarroceria(TipoCarroceria.HATCHBACK).precio(new BigDecimal("1000000")));
        disponible(base("Ford", "Focus").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("1000000")));
        disponible(base("Ford", "Ka").precio(new BigDecimal("1000000")));

        assertThat(catalogoService.similares(elAuto, null)).extracting(PublicacionResumenResponse::getId)
                .containsExactly(mismaMarca);
    }

    @Test
    void similaresDeUnVendidoDeHace90DiasFuncionaIgual() {
        Long vendido = guardar(base("Toyota", "Corolla").tipoCarroceria(TipoCarroceria.SEDAN).precio(new BigDecimal("1000000")),
                "VENDIDO", AHORA.minusDays(120), AHORA.minusDays(90));
        Long parecido = disponible(base("Toyota", "Yaris").precio(new BigDecimal("1050000")));

        assertThat(catalogoService.similares(vendido, null)).extracting(PublicacionResumenResponse::getId)
                .containsExactly(parecido);
    }

    @Test
    void similaresRespetaElLimiteYPorDefectoDevuelveCuatro() {
        Long elAuto = disponible(base("Toyota", "Corolla").precio(new BigDecimal("1000000")));
        for (int i = 0; i < 10; i++) {
            disponible(base("Toyota", "M" + i).precio(new BigDecimal("1000000")));
        }

        assertThat(catalogoService.similares(elAuto, null)).hasSize(4);
        assertThat(catalogoService.similares(elAuto, 2)).hasSize(2);
        assertThat(catalogoService.similares(elAuto, 50)).hasSize(8);
        assertThat(catalogoService.similares(elAuto, 0)).hasSize(1);
    }

    @Test
    void similaresSinCandidatosDevuelveListaVacia() {
        Long solo = disponible(base("Toyota", "Corolla").precio(new BigDecimal("1000000")));

        assertThat(catalogoService.similares(solo, null)).isEmpty();
    }
}
