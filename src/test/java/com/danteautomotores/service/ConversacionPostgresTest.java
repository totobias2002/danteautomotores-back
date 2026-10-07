package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.dto.conversacion.NoLeidosResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
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
import org.springframework.boot.test.mock.mockito.MockBean;
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
@Import({ConversacionService.class, RegistroDeMensajes.class, VerificacionCuenta.class, LimitadorDeIntentos.class,
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

    // El aviso por mail no se prueba acá: el servicio de mail es un doble y no sale nada.
    @MockBean
    private NotificacionesService notificaciones;
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

    private MensajeRequest texto(String texto) {
        MensajeRequest request = new MensajeRequest();
        request.setTexto(texto);
        return request;
    }

    @Test
    void findByIdAndUsuarioIdNoDevuelveLaConversacionDeOtroUsuario() {
        Conversacion deAna = conversacion(ana, corolla, EstadoConversacion.ABIERTA);

        assertThat(conversacionRepository.findByIdAndUsuarioId(deAna.getId(), ana.getId())).isPresent();
        assertThat(conversacionRepository.findByIdAndUsuarioId(deAna.getId(), beto.getId())).isEmpty();
        assertThat(conversacionRepository.findByIdAndUsuarioId(deAna.getId() + 1000, ana.getId())).isEmpty();
    }

    @Test
    void findByConversacionIdOrderByIdAscRespetaElOrdenDeInsercionYNoMezclaConversaciones() {
        Conversacion deAna = conversacion(ana, corolla, EstadoConversacion.ABIERTA);
        Conversacion deBeto = conversacion(beto, corolla, EstadoConversacion.ABIERTA);
        // Todos con el mismo instante del reloj fijo: el orden lo da el id, no la fecha.
        registroDeMensajes.agregar(deAna, ana, AutorMensaje.USUARIO, "uno");
        registroDeMensajes.agregar(deBeto, beto, AutorMensaje.USUARIO, "ajeno");
        registroDeMensajes.agregar(deAna, admin, AutorMensaje.AGENCIA, "dos");
        registroDeMensajes.agregar(deAna, ana, AutorMensaje.USUARIO, "tres");
        em.flush();
        em.clear();

        assertThat(mensajeRepository.findByConversacionIdOrderByIdAsc(deAna.getId()))
                .extracting(Mensaje::getTexto).containsExactly("uno", "dos", "tres");
        assertThat(mensajeRepository.findByConversacionIdOrderByIdAsc(deBeto.getId()))
                .extracting(Mensaje::getTexto).containsExactly("ajeno");
    }

    @Test
    void unMensajeEnviadoConElServicioRealSeGuardaEnElHiloYMueveElUltimoMensajeEn() {
        ConversacionResumenResponse creada = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");
        // La conversación quedó vieja: el envío tiene que moverla al instante del reloj.
        jdbc.update("UPDATE conversaciones SET ultimo_mensaje_en = '2026-10-01 10:00:00' WHERE id = ?", creada.getId());
        em.flush();
        em.clear();

        MensajeResponse enviado = conversacionService.enviarMensaje(creada.getId(), texto("  ¿Aceptan permuta?  "), "ana@dante.test");
        em.flush();
        em.clear();

        assertThat(enviado.getAutor()).isEqualTo(AutorMensaje.USUARIO);
        assertThat(enviado.getTexto()).isEqualTo("¿Aceptan permuta?");
        String ultimo = jdbc.queryForObject(
                "SELECT to_char(ultimo_mensaje_en, 'YYYY-MM-DD\"T\"HH24:MI:SS') FROM conversaciones WHERE id = ?",
                String.class, creada.getId());
        assertThat(ultimo).isEqualTo("2026-10-07T15:30:00");

        ConversacionDetalleResponse hilo = conversacionService.obtenerMia(creada.getId(), "ana@dante.test");
        assertThat(hilo.getMensajes()).extracting(MensajeResponse::getTexto)
                .containsExactly("Hola, me interesa este auto: Toyota Corolla 2020.", "¿Aceptan permuta?");
        assertThat(hilo.getConversacion().getUltimoMensajeEn()).isEqualTo(INSTANTE);
    }

    @Test
    void unaConversacionAjenaDa404AlLeerYAlEscribirYUnaCerradaRechazaElMensaje() {
        ConversacionResumenResponse deAna = conversacionService.iniciarCompra(pedido(corolla, null), "ana@dante.test");
        em.flush();
        em.clear();

        assertThatThrownBy(() -> conversacionService.obtenerMia(deAna.getId(), "beto@dante.test"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> conversacionService.enviarMensaje(deAna.getId(), texto("intruso"), "beto@dante.test"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensajes", Integer.class)).isEqualTo(1);

        jdbc.update("UPDATE conversaciones SET estado = 'CERRADA' WHERE id = ?", deAna.getId());
        em.clear();
        assertThatThrownBy(() -> conversacionService.enviarMensaje(deAna.getId(), texto("tarde"), "ana@dante.test"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensajes", Integer.class)).isEqualTo(1);
    }

    // Ana tiene dos conversaciones; Beto una. Cada uno recibe mensajes de la agencia y escribe los suyos.
    private Conversacion[] hilosConMensajesSinLeer() {
        Conversacion deAnaCorolla = conversacion(ana, corolla, EstadoConversacion.ABIERTA);
        Conversacion deAnaYaris = conversacion(ana, yaris, EstadoConversacion.ABIERTA);
        Conversacion deBeto = conversacion(beto, corolla, EstadoConversacion.ABIERTA);
        registroDeMensajes.agregar(deAnaCorolla, ana, AutorMensaje.USUARIO, "propio de Ana");
        registroDeMensajes.agregar(deAnaCorolla, admin, AutorMensaje.AGENCIA, "agencia a Ana 1");
        registroDeMensajes.agregar(deAnaCorolla, admin, AutorMensaje.AGENCIA, "agencia a Ana 2");
        registroDeMensajes.agregar(deAnaYaris, admin, AutorMensaje.AGENCIA, "agencia a Ana 3");
        registroDeMensajes.agregar(deBeto, beto, AutorMensaje.USUARIO, "propio de Beto");
        registroDeMensajes.agregar(deBeto, admin, AutorMensaje.AGENCIA, "agencia a Beto");
        em.flush();
        em.clear();
        return new Conversacion[]{deAnaCorolla, deAnaYaris, deBeto};
    }

    @Test
    void tresMensajesDeLaAgenciaSinLeerEnDosConversacionesSumanTresYDosYNoMezclanUsuarios() {
        hilosConMensajesSinLeer();

        NoLeidosResponse deAna = conversacionService.contarNoLeidos("ana@dante.test");
        NoLeidosResponse deBeto = conversacionService.contarNoLeidos("beto@dante.test");

        // Los propios sin leer ("propio de Ana") no cuentan, ni los de Beto se suman a Ana.
        assertThat(deAna.getNoLeidos()).isEqualTo(3);
        assertThat(deAna.getConversaciones()).isEqualTo(2);
        assertThat(deBeto.getNoLeidos()).isEqualTo(1);
        assertThat(deBeto.getConversaciones()).isEqualTo(1);
    }

    @Test
    void elAdminCuentaLosMensajesDeUsuariosDeTodaLaBandejaYNoLosDeLaAgencia() {
        hilosConMensajesSinLeer();

        NoLeidosResponse delAdmin = conversacionService.contarNoLeidos("admin@dante.test");

        // "propio de Ana" y "propio de Beto": 2 mensajes en 2 conversaciones.
        assertThat(delAdmin.getNoLeidos()).isEqualTo(2);
        assertThat(delAdmin.getConversaciones()).isEqualTo(2);
    }

    @Test
    void marcarLeidaDejaEnCeroSoloLaConversacionIndicadaYNoTocaLosPropiosNiLosAjenos() {
        Conversacion[] hilos = hilosConMensajesSinLeer();
        Conversacion deAnaCorolla = hilos[0];

        NoLeidosResponse despues = conversacionService.marcarLeida(deAnaCorolla.getId(), "ana@dante.test");
        em.flush();
        em.clear();

        // Quedó el de la otra conversación de Ana.
        assertThat(despues.getNoLeidos()).isEqualTo(1);
        assertThat(despues.getConversaciones()).isEqualTo(1);
        List<Mensaje> hilo = mensajeRepository.findByConversacionIdOrderByIdAsc(deAnaCorolla.getId());
        assertThat(hilo).filteredOn(m -> m.getAutorTipo() == AutorMensaje.AGENCIA)
                .isNotEmpty().allMatch(m -> m.getLeidoEn() != null);
        assertThat(hilo.get(0).getLeidoEn()).isNull(); // el propio de Ana sigue sin leer
        assertThat(hilo.get(1).getLeidoEn()).isEqualTo(AHORA_UTC);
        // La de Beto no se movió, ni los mensajes que Ana escribió cuentan para el admin.
        assertThat(conversacionService.contarNoLeidos("beto@dante.test").getNoLeidos()).isEqualTo(1);
        assertThat(conversacionService.contarNoLeidos("admin@dante.test").getNoLeidos()).isEqualTo(2);
    }

    @Test
    void marcarLeidaDeUnaConversacionAjenaDa404YNoTocaNada() {
        Conversacion[] hilos = hilosConMensajesSinLeer();
        Conversacion deBeto = hilos[2];

        assertThatThrownBy(() -> conversacionService.marcarLeida(deBeto.getId(), "ana@dante.test"))
                .isInstanceOf(ResourceNotFoundException.class);
        em.clear();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM mensajes WHERE leido_en IS NOT NULL", Integer.class)).isZero();
        assertThat(conversacionService.contarNoLeidos("beto@dante.test").getNoLeidos()).isEqualTo(1);
    }

    @Test
    void listarMiasTraeLosNoLeidosPorConversacionConLaBaseReal() {
        Conversacion[] hilos = hilosConMensajesSinLeer();

        List<ConversacionResumenResponse> lista = conversacionService.listarMias("ana@dante.test");

        assertThat(lista).hasSize(2);
        assertThat(lista).filteredOn(c -> c.getId().equals(hilos[0].getId()))
                .extracting(ConversacionResumenResponse::getNoLeidos).containsExactly(2L);
        assertThat(lista).filteredOn(c -> c.getId().equals(hilos[1].getId()))
                .extracting(ConversacionResumenResponse::getNoLeidos).containsExactly(1L);
        // El detalle cuenta lo mismo desde el hilo cargado.
        assertThat(conversacionService.obtenerMia(hilos[0].getId(), "ana@dante.test").getConversacion().getNoLeidos())
                .isEqualTo(2);
    }
}
