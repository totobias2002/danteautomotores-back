package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El catálogo público contra un PostgreSQL real, con el esquema creado por Flyway y validado por Hibernate
 * (ddl-auto=validate): si la entidad no calzara con las migraciones, el contexto ni siquiera arranca.
 */
@Import(CatalogoService.class)
class CatalogoPostgresTest extends PostgresLocalTestBase {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 10, 10, 0);

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
}
