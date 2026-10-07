package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La bandeja del admin contra un PostgreSQL real: cada combinación de filtros, el EXISTS de "solo no leídas", el orden,
 * la paginación de a 20 y el conteo de la página.
 */
@Import(ConversacionAdminService.class)
class ConversacionAdminPostgresTest extends PostgresLocalTestBase {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 7, 12, 0);

    @Autowired
    private ConversacionAdminService servicio;
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

    private Usuario admin;
    private Usuario ana;
    private Usuario beto;
    private Usuario cata;
    private Publicacion corolla;
    private Publicacion yaris;

    // Las cinco conversaciones del escenario base.
    private Conversacion compraAbiertaSinLeerDeAna;   // 2 mensajes del usuario sin leer, t+1
    private Conversacion compraCerradaLeidaDeBeto;    // t+2
    private Conversacion cotizacionAbiertaSinLeerDeAna; // t+3
    private Conversacion cotizacionCerradaDeBeto;     // t+4, el usuario ya fue leído y hay una respuesta de la agencia sin leer
    private Conversacion compraAbiertaRespondidaDeCata; // t+5

    @BeforeEach
    void fixtureBase() {
        admin = usuarioRepository.save(Usuario.builder()
                .nombre("Admin").email("admin@dante.test").passwordHash("hash").rol(Rol.ADMIN).build());
        Agencia agencia = agenciaRepository.save(Agencia.builder().nombre("Agencia Test").slug("agencia-test").build());
        ana = comprador("Ana", "Lopez", "ana@dante.test", "30111111");
        beto = comprador("Beto", "Paz", "beto@dante.test", "30222222");
        cata = comprador("Cata", "Ruiz", "cata@dante.test", "30333333");
        corolla = auto(agencia, "Corolla");
        yaris = auto(agencia, "Yaris");
    }

    private Usuario comprador(String nombre, String apellido, String email, String dni) {
        return usuarioRepository.save(Usuario.builder()
                .nombre(nombre).apellido(apellido).email(email).passwordHash("hash")
                .telefono("+5491112345678").dni(dni).emailConfirmado(true).rol(Rol.COMPRADOR).build());
    }

    private Publicacion auto(Agencia agencia, String modelo) {
        return publicacionRepository.save(Publicacion.builder()
                .agencia(agencia).admin(admin).marca("Toyota").modelo(modelo).anio(2020)
                .precio(new BigDecimal("1000000.00")).descripcion("descripcion").build());
    }

    private Conversacion conversacion(Usuario usuario, Publicacion publicacion, TipoConversacion tipo,
                                      EstadoConversacion estado, LocalDateTime ultimoMensajeEn) {
        return conversacionRepository.saveAndFlush(Conversacion.builder()
                .tipo(tipo).estado(estado).usuario(usuario).publicacion(publicacion)
                .creadaEn(BASE).ultimoMensajeEn(ultimoMensajeEn).build());
    }

    private Mensaje mensaje(Conversacion conversacion, AutorMensaje autor, String texto, boolean leido) {
        Usuario quien = autor == AutorMensaje.USUARIO ? conversacion.getUsuario() : admin;
        return mensajeRepository.saveAndFlush(Mensaje.builder()
                .conversacion(conversacion).autor(quien).autorTipo(autor).texto(texto)
                .creadoEn(BASE).leidoEn(leido ? BASE.plusMinutes(1) : null).build());
    }

    private void escenarioBase() {
        compraAbiertaSinLeerDeAna = conversacion(ana, corolla, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, BASE.plusMinutes(1));
        mensaje(compraAbiertaSinLeerDeAna, AutorMensaje.USUARIO, "Primero", false);
        mensaje(compraAbiertaSinLeerDeAna, AutorMensaje.USUARIO, "Segundo", false);

        compraCerradaLeidaDeBeto = conversacion(beto, yaris, TipoConversacion.COMPRA, EstadoConversacion.CERRADA, BASE.plusMinutes(2));
        mensaje(compraCerradaLeidaDeBeto, AutorMensaje.USUARIO, "Lo quiero", true);

        cotizacionAbiertaSinLeerDeAna = conversacion(ana, null, TipoConversacion.COTIZACION, EstadoConversacion.ABIERTA, BASE.plusMinutes(3));
        mensaje(cotizacionAbiertaSinLeerDeAna, AutorMensaje.USUARIO, "Quiero cotizar", false);

        cotizacionCerradaDeBeto = conversacion(beto, null, TipoConversacion.COTIZACION, EstadoConversacion.CERRADA, BASE.plusMinutes(4));
        mensaje(cotizacionCerradaDeBeto, AutorMensaje.USUARIO, "Cotizo mi auto", true);
        mensaje(cotizacionCerradaDeBeto, AutorMensaje.AGENCIA, "Te pasamos el precio", false);

        compraAbiertaRespondidaDeCata = conversacion(cata, corolla, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, BASE.plusMinutes(5));
        mensaje(compraAbiertaRespondidaDeCata, AutorMensaje.USUARIO, "Hola", true);
        mensaje(compraAbiertaRespondidaDeCata, AutorMensaje.AGENCIA, "Hola Cata", false);
    }

    private List<Long> ids(TipoConversacion tipo, EstadoConversacion estado, boolean soloNoLeidas) {
        return servicio.listar(tipo, estado, soloNoLeidas, 1).contenido().stream()
                .map(ConversacionResumenResponse::getId).toList();
    }

    @Test
    void sinFiltrosDevuelveTodasLasConversacionesDeTodosLosUsuarios() {
        escenarioBase();

        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, false, 1);

        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
        assertThat(pagina.contenido()).extracting(c -> c.getUsuario().getNombre())
                .containsExactlyInAnyOrder("Ana", "Beto", "Ana", "Beto", "Cata");
    }

    @Test
    void cadaCombinacionDeFiltrosDevuelveExactamenteLasEsperadas() {
        escenarioBase();
        Long c1 = compraAbiertaSinLeerDeAna.getId();
        Long c2 = compraCerradaLeidaDeBeto.getId();
        Long c3 = cotizacionAbiertaSinLeerDeAna.getId();
        Long c4 = cotizacionCerradaDeBeto.getId();
        Long c5 = compraAbiertaRespondidaDeCata.getId();

        // Tipo solo.
        assertThat(ids(TipoConversacion.COMPRA, null, false)).containsExactlyInAnyOrder(c1, c2, c5);
        assertThat(ids(TipoConversacion.COTIZACION, null, false)).containsExactlyInAnyOrder(c3, c4);
        // Estado solo.
        assertThat(ids(null, EstadoConversacion.ABIERTA, false)).containsExactlyInAnyOrder(c1, c3, c5);
        assertThat(ids(null, EstadoConversacion.CERRADA, false)).containsExactlyInAnyOrder(c2, c4);
        // Tipo y estado.
        assertThat(ids(TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, false)).containsExactlyInAnyOrder(c1, c5);
        assertThat(ids(TipoConversacion.COMPRA, EstadoConversacion.CERRADA, false)).containsExactlyInAnyOrder(c2);
        assertThat(ids(TipoConversacion.COTIZACION, EstadoConversacion.ABIERTA, false)).containsExactlyInAnyOrder(c3);
        assertThat(ids(TipoConversacion.COTIZACION, EstadoConversacion.CERRADA, false)).containsExactlyInAnyOrder(c4);
        // Solo no leidas, solo y combinado.
        assertThat(ids(null, null, true)).containsExactlyInAnyOrder(c1, c3);
        assertThat(ids(TipoConversacion.COMPRA, null, true)).containsExactlyInAnyOrder(c1);
        assertThat(ids(TipoConversacion.COTIZACION, null, true)).containsExactlyInAnyOrder(c3);
        assertThat(ids(null, EstadoConversacion.ABIERTA, true)).containsExactlyInAnyOrder(c1, c3);
        assertThat(ids(null, EstadoConversacion.CERRADA, true)).isEmpty();
        assertThat(ids(TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, true)).containsExactlyInAnyOrder(c1);
        assertThat(ids(TipoConversacion.COTIZACION, EstadoConversacion.CERRADA, true)).isEmpty();
    }

    @Test
    void soloNoLeidasExcluyeLasQueLaAgenciaYaLeyoYLasQueSoloTienenMensajesDeLaAgenciaSinLeer() {
        escenarioBase();

        List<Long> sinLeer = ids(null, null, true);

        // La cerrada de Beto tiene un mensaje de la agencia sin leer, pero eso no es "no leído por la agencia".
        assertThat(sinLeer).doesNotContain(compraCerradaLeidaDeBeto.getId(), cotizacionCerradaDeBeto.getId(),
                compraAbiertaRespondidaDeCata.getId());

        // La agencia lee la primera: sale del filtro.
        mensajeRepository.marcarLeidos(compraAbiertaSinLeerDeAna.getId(), AutorMensaje.USUARIO, BASE.plusHours(1));

        assertThat(ids(null, null, true)).containsExactly(cotizacionAbiertaSinLeerDeAna.getId());
    }

    @Test
    void elRecuentoNoCuentaDosVecesUnaConversacionConVariosMensajesSinLeer() {
        escenarioBase();
        // La primera ya tiene 2 sin leer; se le suman 3 más.
        for (int i = 0; i < 3; i++) {
            mensaje(compraAbiertaSinLeerDeAna, AutorMensaje.USUARIO, "Otro " + i, false);
        }

        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, true, 1);

        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.contenido()).hasSize(2);
        assertThat(pagina.contenido()).filteredOn(c -> c.getId().equals(compraAbiertaSinLeerDeAna.getId()))
                .singleElement().satisfies(c -> assertThat(c.getNoLeidos()).isEqualTo(5));
    }

    @Test
    void cadaFilaTraeElUsuarioElUltimoMensajeYLosNoLeidosDelLadoDeLaAgencia() {
        escenarioBase();

        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, false, 1);

        ConversacionResumenResponse primera = fila(pagina, compraAbiertaSinLeerDeAna);
        assertThat(primera.getUsuario().getNombre()).isEqualTo("Ana");
        assertThat(primera.getUsuario().getApellido()).isEqualTo("Lopez");
        assertThat(primera.getUsuario().getEmail()).isEqualTo("ana@dante.test");
        assertThat(primera.getNoLeidos()).isEqualTo(2);
        assertThat(primera.getUltimoMensaje()).isEqualTo("Segundo");
        assertThat(primera.getUltimoMensajeAutor()).isEqualTo(AutorMensaje.USUARIO);
        assertThat(primera.getPublicacion().getModelo()).isEqualTo("Corolla");

        ConversacionResumenResponse cotizacion = fila(pagina, cotizacionCerradaDeBeto);
        assertThat(cotizacion.getPublicacion()).isNull();
        // Un mensaje de la agencia sin leer no cuenta como no leído de la agencia.
        assertThat(cotizacion.getNoLeidos()).isZero();
        assertThat(cotizacion.getUltimoMensajeAutor()).isEqualTo(AutorMensaje.AGENCIA);

        assertThat(fila(pagina, compraCerradaLeidaDeBeto).getNoLeidos()).isZero();
    }

    private ConversacionResumenResponse fila(PaginaResponse<ConversacionResumenResponse> pagina, Conversacion conversacion) {
        return pagina.contenido().stream().filter(c -> c.getId().equals(conversacion.getId())).findFirst().orElseThrow();
    }

    @Test
    void ordenaPorUltimoMensajeDescendenteYDesempataPorIdDescendente() {
        escenarioBase();
        // Dos conversaciones con el mismo instante: manda el id mayor.
        Conversacion empateA = conversacion(cata, yaris, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA, BASE.plusMinutes(10));
        Conversacion empateB = conversacion(cata, null, TipoConversacion.COTIZACION, EstadoConversacion.ABIERTA, BASE.plusMinutes(10));

        List<Long> orden = ids(null, null, false);

        assertThat(orden).containsExactly(
                empateB.getId(), empateA.getId(),
                compraAbiertaRespondidaDeCata.getId(), cotizacionCerradaDeBeto.getId(),
                cotizacionAbiertaSinLeerDeAna.getId(), compraCerradaLeidaDeBeto.getId(),
                compraAbiertaSinLeerDeAna.getId());
        assertThat(empateB.getId()).isGreaterThan(empateA.getId());
    }

    @Test
    void paginaDeAVeinteYRespetaLosTotales() {
        // 45 cotizaciones: la última es la más reciente.
        for (int i = 0; i < 45; i++) {
            Conversacion c = conversacion(ana, null, TipoConversacion.COTIZACION, EstadoConversacion.ABIERTA, BASE.plusMinutes(i));
            mensaje(c, AutorMensaje.USUARIO, "Mensaje " + i, false);
        }

        PaginaResponse<ConversacionResumenResponse> primera = servicio.listar(null, null, false, 1);
        PaginaResponse<ConversacionResumenResponse> segunda = servicio.listar(null, null, false, 2);
        PaginaResponse<ConversacionResumenResponse> tercera = servicio.listar(null, null, false, 3);
        PaginaResponse<ConversacionResumenResponse> cuarta = servicio.listar(null, null, false, 4);

        assertThat(primera.contenido()).hasSize(20);
        assertThat(segunda.contenido()).hasSize(20);
        assertThat(tercera.contenido()).hasSize(5);
        assertThat(cuarta.contenido()).isEmpty();
        assertThat(primera.totalElementos()).isEqualTo(45);
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(primera.tamanio()).isEqualTo(20);
        assertThat(tercera.pagina()).isEqualTo(3);
        // La más reciente primero y ninguna se repite entre páginas.
        assertThat(primera.contenido().get(0).getUltimoMensaje()).isEqualTo("Mensaje 44");
        assertThat(tercera.contenido().get(4).getUltimoMensaje()).isEqualTo("Mensaje 0");
        List<Long> todas = new java.util.ArrayList<>();
        todas.addAll(primera.contenido().stream().map(ConversacionResumenResponse::getId).toList());
        todas.addAll(segunda.contenido().stream().map(ConversacionResumenResponse::getId).toList());
        todas.addAll(tercera.contenido().stream().map(ConversacionResumenResponse::getId).toList());
        assertThat(todas).doesNotHaveDuplicates().hasSize(45);
    }

    @Test
    void laPaginacionConSoloNoLeidasTambienCuentaConversaciones() {
        for (int i = 0; i < 25; i++) {
            Conversacion c = conversacion(ana, null, TipoConversacion.COTIZACION, EstadoConversacion.ABIERTA, BASE.plusMinutes(i));
            mensaje(c, AutorMensaje.USUARIO, "Uno " + i, false);
            mensaje(c, AutorMensaje.USUARIO, "Dos " + i, false);
        }

        PaginaResponse<ConversacionResumenResponse> primera = servicio.listar(null, null, true, 1);

        assertThat(primera.totalElementos()).isEqualTo(25);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(primera.contenido()).hasSize(20);
        assertThat(primera.contenido()).allSatisfy(c -> assertThat(c.getNoLeidos()).isEqualTo(2));
    }

    @Test
    void sinConversacionesDevuelveUnaPaginaVacia() {
        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, false, 1);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.pagina()).isEqualTo(1);
    }
}
