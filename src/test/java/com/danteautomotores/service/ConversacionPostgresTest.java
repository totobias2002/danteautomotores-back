package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.support.PostgresLocalTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las conversaciones contra un PostgreSQL real, con el esquema de Flyway (V6) validado por Hibernate: si las entidades
 * no calzaran con la migración, el contexto ni siquiera arranca (ddl-auto=validate).
 */
@Import({ConversacionService.class, RegistroDeMensajes.class, VerificacionCuenta.class,
        ConversacionPostgresTest.RelojFijo.class})
class ConversacionPostgresTest extends PostgresLocalTestBase {

    private static final Instant INSTANTE = Instant.parse("2026-10-07T15:30:00Z");
    private static final LocalDateTime AHORA_UTC = LocalDateTime.of(2026, 10, 7, 15, 30);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        Clock clock() {
            // El reloj del servidor en otra zona: lo guardado tiene que salir en UTC igual.
            return Clock.fixed(INSTANTE, ZoneId.of("America/Argentina/Buenos_Aires"));
        }
    }

    @Autowired
    private ConversacionService conversacionService;
    @Autowired
    private RegistroDeMensajes registroDeMensajes;
    @Autowired
    private ConversacionRepository conversacionRepository;
    @Autowired
    private MensajeRepository mensajeRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private AgenciaRepository agenciaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager em;

    private Usuario admin;
    private Agencia agencia;
    private Usuario ana;
    private Usuario beto;
    private Publicacion corolla;
    private Publicacion yaris;

    @BeforeEach
    void fixtureBase() {
        admin = usuarioRepository.save(Usuario.builder()
                .nombre("Admin").email("admin@dante.test").passwordHash("hash").rol(Rol.ADMIN).build());
        agencia = agenciaRepository.save(Agencia.builder().nombre("Agencia Test").slug("agencia-test").build());
        ana = comprador("ana@dante.test", "30111111");
        beto = comprador("beto@dante.test", "30222222");
        corolla = auto("Corolla");
        yaris = auto("Yaris");
    }

    private Usuario comprador(String email, String dni) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Comprador").apellido("Prueba").email(email).passwordHash("hash")
                .telefono("+5491112345678").dni(dni).emailConfirmado(true).rol(Rol.COMPRADOR).build());
    }

    private Publicacion auto(String modelo) {
        return publicacionRepository.save(Publicacion.builder()
                .agencia(agencia).admin(admin).marca("Toyota").modelo(modelo).anio(2020)
                .precio(new BigDecimal("1000000.00")).descripcion("descripcion").build());
    }

    private Conversacion conversacion(Usuario usuario, Publicacion publicacion, EstadoConversacion estado) {
        return conversacionRepository.saveAndFlush(Conversacion.builder()
                .tipo(TipoConversacion.COMPRA).estado(estado).usuario(usuario).publicacion(publicacion)
                .creadaEn(AHORA_UTC).ultimoMensajeEn(AHORA_UTC).build());
    }

    private ConversacionRequest pedido(Publicacion publicacion, String mensaje) {
        ConversacionRequest request = new ConversacionRequest();
        request.setPublicacionId(publicacion.getId());
        request.setMensaje(mensaje);
        return request;
    }

    @Test
    void unaAbiertaYUnaCerradaDelMismoUsuarioYAutoConviven() {
        conversacion(ana, corolla, EstadoConversacion.ABIERTA);
        conversacion(ana, corolla, EstadoConversacion.CERRADA);
        conversacion(ana, corolla, EstadoConversacion.CERRADA);

        Integer filas = jdbc.queryForObject("SELECT count(*) FROM conversaciones", Integer.class);
        assertThat(filas).isEqualTo(3);
    }

    @Test
    void unaAbiertaDeOtroUsuarioOdeOtroAutoNoChocan() {
        conversacion(ana, corolla, EstadoConversacion.ABIERTA);
        conversacion(beto, corolla, EstadoConversacion.ABIERTA);
        conversacion(ana, yaris, EstadoConversacion.ABIERTA);

        Integer filas = jdbc.queryForObject("SELECT count(*) FROM conversaciones", Integer.class);
        assertThat(filas).isEqualTo(3);
    }

    @Test
    void dosAbiertasDeCompraDelMismoUsuarioYAutoLaBaseLasRechaza() {
        conversacion(ana, corolla, EstadoConversacion.ABIERTA);

        assertThatThrownBy(() -> conversacion(ana, corolla, EstadoConversacion.ABIERTA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void iniciarCompraDosVecesDejaUnaSolaFilaYDevuelveElMismoId() {
        ConversacionResumenResponse primera = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");
        ConversacionResumenResponse segunda = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");

        assertThat(segunda.getId()).isEqualTo(primera.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversaciones", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensajes", Integer.class)).isEqualTo(1);

        // Con texto propio suma solo ese mensaje a la misma conversación.
        ConversacionResumenResponse tercera = conversacionService.iniciarCompra(pedido(corolla, "¿Acepta permuta?"), "ana@dante.test");
        assertThat(tercera.getId()).isEqualTo(primera.getId());
        assertThat(tercera.getUltimoMensaje()).isEqualTo("¿Acepta permuta?");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversaciones", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensajes", Integer.class)).isEqualTo(2);
    }

    @Test
    void lasFechasSeGuardanEnUtcConElRelojFijo() {
        ConversacionResumenResponse respuesta = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");
        em.flush();

        // Se lee como texto: así no depende de la zona de la JVM del test.
        String creada = jdbc.queryForObject("SELECT to_char(creada_en, 'YYYY-MM-DD\"T\"HH24:MI:SS') FROM conversaciones", String.class);
        String ultimo = jdbc.queryForObject("SELECT to_char(ultimo_mensaje_en, 'YYYY-MM-DD\"T\"HH24:MI:SS') FROM conversaciones", String.class);
        String mensaje = jdbc.queryForObject("SELECT to_char(creado_en, 'YYYY-MM-DD\"T\"HH24:MI:SS') FROM mensajes", String.class);
        assertThat(creada).isEqualTo("2026-10-07T15:30:00");
        assertThat(ultimo).isEqualTo("2026-10-07T15:30:00");
        assertThat(mensaje).isEqualTo("2026-10-07T15:30:00");
        assertThat(respuesta.getCreadaEn()).isEqualTo(INSTANTE);
    }

    @Test
    void elUltimoMensajeDeCadaConversacionSeResuelveEnUnaConsultaYRespetaLasPedidas() {
        Conversacion deAna = conversacion(ana, corolla, EstadoConversacion.ABIERTA);
        Conversacion deBeto = conversacion(beto, corolla, EstadoConversacion.ABIERTA);
        Conversacion deAnaYaris = conversacion(ana, yaris, EstadoConversacion.ABIERTA);
        registroDeMensajes.agregar(deAna, ana, AutorMensaje.USUARIO, "uno de Ana");
        registroDeMensajes.agregar(deBeto, beto, AutorMensaje.USUARIO, "uno de Beto");
        registroDeMensajes.agregar(deAna, admin, AutorMensaje.AGENCIA, "respuesta a Ana");
        Mensaje ultimoDeAna = registroDeMensajes.agregar(deAna, ana, AutorMensaje.USUARIO, "último de Ana");
        Mensaje ultimoDeBeto = registroDeMensajes.agregar(deBeto, admin, AutorMensaje.AGENCIA, "último de Beto");
        em.flush();
        em.clear();

        List<Mensaje> ultimos = mensajeRepository.findUltimosPorConversaciones(
                List.of(deAna.getId(), deBeto.getId(), deAnaYaris.getId()));

        // La tercera no tiene mensajes: no aparece. De las otras, solo el de mayor id.
        assertThat(ultimos).extracting(Mensaje::getId).containsExactlyInAnyOrder(ultimoDeAna.getId(), ultimoDeBeto.getId());
        assertThat(ultimos).extracting(Mensaje::getTexto).containsExactlyInAnyOrder("último de Ana", "último de Beto");

        // Solo las conversaciones que se piden.
        List<Mensaje> soloAna = mensajeRepository.findUltimosPorConversaciones(List.of(deAna.getId()));
        assertThat(soloAna).extracting(Mensaje::getId).containsExactly(ultimoDeAna.getId());
    }

    @Test
    void listarMiasNoMuestraLasConversacionesDeOtraCuenta() {
        ConversacionResumenResponse deAna = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");
        ConversacionResumenResponse deBeto = conversacionService.iniciarCompra(pedido(yaris, null), "beto@dante.test");
        em.flush();
        em.clear();

        assertThat(conversacionService.listarMias("ana@dante.test"))
                .extracting(ConversacionResumenResponse::getId).containsExactly(deAna.getId());
        assertThat(conversacionService.listarMias("beto@dante.test"))
                .extracting(ConversacionResumenResponse::getId).containsExactly(deBeto.getId());
    }
}
