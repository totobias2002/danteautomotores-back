package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionDetalleResponse;
import com.danteautomotores.dto.conversacion.ConversacionRequest;
import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.conversacion.MensajeRequest;
import com.danteautomotores.dto.conversacion.MensajeResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversacionServiceTest {

    private static final String EMAIL = "ana@cuenta.com";
    private static final Instant INSTANTE = Instant.parse("2026-10-07T15:30:00Z");
    private static final LocalDateTime AHORA_UTC = LocalDateTime.of(2026, 10, 7, 15, 30);

    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    // La regla real: así el test falla si cambia lo que significa "verificada".
    @Spy
    private VerificacionCuenta verificacionCuenta = new VerificacionCuenta();

    private LimitadorDeIntentos limitador;
    private ConversacionService servicio;

    @BeforeEach
    void armarElServicio() {
        // El reloj del servidor está en otra zona a propósito: las fechas guardadas tienen que salir igual, en UTC.
        Clock reloj = Clock.fixed(INSTANTE, ZoneId.of("America/Argentina/Buenos_Aires"));
        // El limitador real: el test del mensaje 21 prueba el límite de verdad, no un mock.
        limitador = new LimitadorDeIntentos();
        servicio = new ConversacionService(conversacionRepository, mensajeRepository, publicacionRepository,
                usuarioRepository, verificacionCuenta, new RegistroDeMensajes(mensajeRepository, reloj), limitador, reloj);
    }

    private ConversacionRequest pedido(Long publicacionId, String mensaje) {
        ConversacionRequest request = new ConversacionRequest();
        request.setPublicacionId(publicacionId);
        request.setMensaje(mensaje);
        return request;
    }

    private Usuario cuentaVerificada() {
        return Usuario.builder()
                .id(3L).nombre("Ana").apellido("Pérez").email(EMAIL)
                .telefono("+5491112345678").dni("30123456")
                .rol(Rol.COMPRADOR).emailConfirmado(true)
                .build();
    }

    private Publicacion auto(EstadoPublicacion estado) {
        return Publicacion.builder()
                .id(7L).marca("Toyota").modelo("Corolla").anio(2020).estado(estado)
                .agencia(Agencia.builder().id(1L).nombre("Dante").slug("dante").build())
                .build();
    }

    private void existeLaCuenta(Usuario usuario) {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private void existeElAuto(EstadoPublicacion estado) {
        when(publicacionRepository.findById(7L)).thenReturn(Optional.of(auto(estado)));
    }

    private void noHayConversacionAbierta() {
        when(conversacionRepository.findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
                3L, 7L, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA)).thenReturn(Optional.empty());
    }

    private void guardarDevuelveLoQueRecibe() {
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(i -> {
            Conversacion c = i.getArgument(0);
            c.setId(50L);
            return c;
        });
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> {
            Mensaje m = i.getArgument(0);
            m.setId(900L);
            return m;
        });
    }

    private Conversacion abiertaExistente() {
        return Conversacion.builder()
                .id(50L).tipo(TipoConversacion.COMPRA).estado(EstadoConversacion.ABIERTA)
                .usuario(cuentaVerificada()).publicacion(auto(EstadoPublicacion.DISPONIBLE))
                .creadaEn(AHORA_UTC.minusDays(2)).ultimoMensajeEn(AHORA_UTC.minusDays(2))
                .build();
    }

    @Test
    void unaCuentaVerificadaCreaLaConversacionDeCompraAbiertaConSuPrimerMensajeAutomatico() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        noHayConversacionAbierta();
        guardarDevuelveLoQueRecibe();

        ConversacionResumenResponse respuesta = servicio.iniciarCompra(pedido(7L, null), EMAIL);

        ArgumentCaptor<Conversacion> conversacion = ArgumentCaptor.forClass(Conversacion.class);
        verify(conversacionRepository).save(conversacion.capture());
        Conversacion guardada = conversacion.getValue();
        assertThat(guardada.getTipo()).isEqualTo(TipoConversacion.COMPRA);
        assertThat(guardada.getEstado()).isEqualTo(EstadoConversacion.ABIERTA);
        assertThat(guardada.getUsuario().getId()).isEqualTo(3L);
        assertThat(guardada.getPublicacion().getId()).isEqualTo(7L);
        assertThat(guardada.getCreadaEn()).isEqualTo(AHORA_UTC);
        assertThat(guardada.getUltimoMensajeEn()).isEqualTo(AHORA_UTC);

        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(mensaje.capture());
        assertThat(mensaje.getValue().getAutorTipo()).isEqualTo(AutorMensaje.USUARIO);
        assertThat(mensaje.getValue().getAutor().getId()).isEqualTo(3L);
        assertThat(mensaje.getValue().getCreadoEn()).isEqualTo(AHORA_UTC);
        assertThat(mensaje.getValue().getTexto()).contains("Toyota").contains("Corolla").contains("2020");

        assertThat(respuesta.getTipo()).isEqualTo(TipoConversacion.COMPRA);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoConversacion.ABIERTA);
        assertThat(respuesta.getCreadaEn()).isEqualTo(INSTANTE);
        assertThat(respuesta.getUltimoMensajeEn()).isEqualTo(INSTANTE);
    }

    @Test
    void unMensajePropioSeGuardaRecortado() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        noHayConversacionAbierta();
        guardarDevuelveLoQueRecibe();

        ConversacionResumenResponse respuesta = servicio.iniciarCompra(pedido(7L, "   ¿Sigue disponible?  \n"), EMAIL);

        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(mensaje.capture());
        assertThat(mensaje.getValue().getTexto()).isEqualTo("¿Sigue disponible?");
        assertThat(respuesta.getUltimoMensaje()).isEqualTo("¿Sigue disponible?");
    }

    @Test
    void unMensajeEnBlancoUsaElTextoAutomatico() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        noHayConversacionAbierta();
        guardarDevuelveLoQueRecibe();

        servicio.iniciarCompra(pedido(7L, "    "), EMAIL);

        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(mensaje.capture());
        assertThat(mensaje.getValue().getTexto()).isEqualTo("Hola, me interesa este auto: Toyota Corolla 2020.");
    }

    @Test
    void unSegundoPedidoSinTextoDevuelveLaConversacionExistenteSinGuardarNada() {
        Conversacion existente = abiertaExistente();
        Mensaje ultimo = Mensaje.builder().id(900L).conversacion(existente).autorTipo(AutorMensaje.USUARIO)
                .texto("Hola, me interesa este auto: Toyota Corolla 2020.").creadoEn(AHORA_UTC.minusDays(2)).build();
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        when(conversacionRepository.findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
                3L, 7L, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA)).thenReturn(Optional.of(existente));
        when(mensajeRepository.findUltimosPorConversaciones(List.of(50L))).thenReturn(List.of(ultimo));

        ConversacionResumenResponse respuesta = servicio.iniciarCompra(pedido(7L, null), EMAIL);

        assertThat(respuesta.getId()).isEqualTo(50L);
        assertThat(respuesta.getUltimoMensaje()).isEqualTo("Hola, me interesa este auto: Toyota Corolla 2020.");
        verify(conversacionRepository, never()).save(any());
        verify(mensajeRepository, never()).save(any());
        // La conversación no se mueve en la lista si no hubo un mensaje nuevo.
        assertThat(existente.getUltimoMensajeEn()).isEqualTo(AHORA_UTC.minusDays(2));
    }

    @Test
    void unSegundoPedidoConTextoPropioLeSumaSoloEseMensajeALaConversacionExistente() {
        Conversacion existente = abiertaExistente();
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        when(conversacionRepository.findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
                3L, 7L, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA)).thenReturn(Optional.of(existente));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));

        ConversacionResumenResponse respuesta = servicio.iniciarCompra(pedido(7L, " Puedo verlo el sábado? "), EMAIL);

        assertThat(respuesta.getId()).isEqualTo(50L);
        assertThat(respuesta.getUltimoMensaje()).isEqualTo("Puedo verlo el sábado?");
        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(mensaje.capture());
        assertThat(mensaje.getValue().getConversacion()).isSameAs(existente);
        assertThat(mensaje.getValue().getTexto()).isEqualTo("Puedo verlo el sábado?");
        verify(conversacionRepository, never()).save(any());
        assertThat(existente.getUltimoMensajeEn()).isEqualTo(AHORA_UTC);
    }

    @Test
    void unaCuentaIncompletaLanzaConLosFaltantesNoGuardaNadaYNiSiquieraBuscaElAuto() {
        Usuario incompleta = cuentaVerificada();
        incompleta.setDni(null);
        incompleta.setEmailConfirmado(false);
        existeLaCuenta(incompleta);

        assertThatThrownBy(() -> servicio.iniciarCompra(pedido(7L, null), EMAIL))
                .isInstanceOfSatisfying(CuentaNoVerificadaException.class,
                        e -> assertThat(e.getFaltantes())
                                .containsExactly(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR));

        verify(conversacionRepository, never()).save(any());
        verify(mensajeRepository, never()).save(any());
        verifyNoInteractions(publicacionRepository);
    }

    @Test
    void unAutoVendidoSeRechazaYNoSeGuardaNada() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.VENDIDO);

        assertThatThrownBy(() -> servicio.iniciarCompra(pedido(7L, null), EMAIL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Este auto ya se vendió");

        verify(conversacionRepository, never()).save(any());
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void unAutoReservadoSeAcepta() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.RESERVADO);
        noHayConversacionAbierta();
        guardarDevuelveLoQueRecibe();

        assertThat(servicio.iniciarCompra(pedido(7L, null), EMAIL)).isNotNull();

        verify(conversacionRepository).save(any(Conversacion.class));
    }

    @Test
    void unaPublicacionInexistenteDa404() {
        existeLaCuenta(cuentaVerificada());
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarCompra(pedido(99L, null), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(conversacionRepository, never()).save(any());
    }

    @Test
    void unaCuentaInexistenteDa404() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarCompra(pedido(7L, null), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> servicio.listarMias(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(conversacionRepository, never()).save(any());
        verifyNoInteractions(publicacionRepository);
    }

    @Test
    void listarMiasDevuelveSoloLasDelUsuarioConElExtractoDelUltimoMensajeYFechasEnUtc() {
        Conversacion primera = abiertaExistente();
        Conversacion segunda = abiertaExistente();
        segunda.setId(51L);
        segunda.setUltimoMensajeEn(AHORA_UTC.minusHours(1));
        String largo = "x".repeat(300);
        Mensaje deLaPrimera = Mensaje.builder().id(901L).conversacion(primera).autorTipo(AutorMensaje.AGENCIA)
                .texto("Sí, lo tenemos").creadoEn(AHORA_UTC).build();
        Mensaje deLaSegunda = Mensaje.builder().id(902L).conversacion(segunda).autorTipo(AutorMensaje.USUARIO)
                .texto(largo).creadoEn(AHORA_UTC.minusHours(1)).build();
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(3L))
                .thenReturn(List.of(primera, segunda));
        when(mensajeRepository.findUltimosPorConversaciones(List.of(50L, 51L)))
                .thenReturn(List.of(deLaPrimera, deLaSegunda));

        List<ConversacionResumenResponse> lista = servicio.listarMias(EMAIL);

        assertThat(lista).extracting(ConversacionResumenResponse::getId).containsExactly(50L, 51L);
        assertThat(lista.get(0).getUltimoMensaje()).isEqualTo("Sí, lo tenemos");
        assertThat(lista.get(0).getUltimoMensajeAutor()).isEqualTo(AutorMensaje.AGENCIA);
        assertThat(lista.get(1).getUltimoMensaje()).hasSize(120).endsWith("...");
        assertThat(lista.get(0).getCreadaEn()).isEqualTo(AHORA_UTC.minusDays(2).toInstant(ZoneOffset.UTC));
        assertThat(lista.get(1).getUltimoMensajeEn()).isEqualTo(AHORA_UTC.minusHours(1).toInstant(ZoneOffset.UTC));
        verify(conversacionRepository).findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(3L);
    }

    @Test
    void listarMiasSinConversacionesDevuelveVaciaYNoConsultaMensajes() {
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(3L)).thenReturn(List.of());

        assertThat(servicio.listarMias(EMAIL)).isEmpty();

        verify(mensajeRepository, never()).findUltimosPorConversaciones(anyCollection());
    }

    private MensajeRequest texto(String texto) {
        MensajeRequest request = new MensajeRequest();
        request.setTexto(texto);
        return request;
    }

    @Test
    void obtenerMiaDevuelveElHiloEnOrdenAscendenteParaElDueno() {
        Conversacion conversacion = abiertaExistente();
        Mensaje primero = Mensaje.builder().id(900L).conversacion(conversacion).autorTipo(AutorMensaje.USUARIO)
                .texto("Hola").creadoEn(AHORA_UTC.minusHours(2)).build();
        Mensaje segundo = Mensaje.builder().id(901L).conversacion(conversacion).autorTipo(AutorMensaje.AGENCIA)
                .texto("Buenas, ¿en qué te ayudamos?").creadoEn(AHORA_UTC.minusHours(1)).leidoEn(AHORA_UTC).build();
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByIdAndUsuarioId(50L, 3L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.findByConversacionIdOrderByIdAsc(50L)).thenReturn(List.of(primero, segundo));

        ConversacionDetalleResponse detalle = servicio.obtenerMia(50L, EMAIL);

        assertThat(detalle.getConversacion().getId()).isEqualTo(50L);
        assertThat(detalle.getConversacion().getPublicacion().getMarca()).isEqualTo("Toyota");
        assertThat(detalle.getMensajes()).extracting(MensajeResponse::getId).containsExactly(900L, 901L);
        assertThat(detalle.getMensajes()).extracting(MensajeResponse::getAutor)
                .containsExactly(AutorMensaje.USUARIO, AutorMensaje.AGENCIA);
        assertThat(detalle.getMensajes()).extracting(MensajeResponse::isLeido).containsExactly(false, true);
        assertThat(detalle.getMensajes().get(0).getCreadoEn())
                .isEqualTo(AHORA_UTC.minusHours(2).toInstant(ZoneOffset.UTC));
        assertThat(detalle.getConversacion().getUltimoMensaje()).isEqualTo("Buenas, ¿en qué te ayudamos?");
    }

    @Test
    void obtenerMiaDa404ParaUnaConversacionAjenaYParaUnIdInexistente() {
        existeLaCuenta(cuentaVerificada());
        // El repositorio busca por id Y dueño: la ajena y la inexistente son lo mismo (D-12).
        when(conversacionRepository.findByIdAndUsuarioId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerMia(77L, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("No existe la conversación");
        assertThatThrownBy(() -> servicio.obtenerMia(9999L, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("No existe la conversación");

        verify(conversacionRepository).findByIdAndUsuarioId(77L, 3L);
        verify(mensajeRepository, never()).findByConversacionIdOrderByIdAsc(any());
    }

    @Test
    void enviarMensajeGuardaConAutorUsuarioRecortadoYMueveLaConversacion() {
        Conversacion conversacion = abiertaExistente();
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByIdAndUsuarioId(50L, 3L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> {
            Mensaje m = i.getArgument(0);
            m.setId(910L);
            return m;
        });

        MensajeResponse respuesta = servicio.enviarMensaje(50L, texto("   ¿Puedo verlo hoy?  \n"), EMAIL);

        ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository).save(mensaje.capture());
        assertThat(mensaje.getValue().getAutorTipo()).isEqualTo(AutorMensaje.USUARIO);
        assertThat(mensaje.getValue().getAutor().getId()).isEqualTo(3L);
        assertThat(mensaje.getValue().getConversacion()).isSameAs(conversacion);
        assertThat(mensaje.getValue().getTexto()).isEqualTo("¿Puedo verlo hoy?");
        assertThat(mensaje.getValue().getCreadoEn()).isEqualTo(AHORA_UTC);
        assertThat(conversacion.getUltimoMensajeEn()).isEqualTo(AHORA_UTC);
        assertThat(respuesta.getId()).isEqualTo(910L);
        assertThat(respuesta.getAutor()).isEqualTo(AutorMensaje.USUARIO);
        assertThat(respuesta.getTexto()).isEqualTo("¿Puedo verlo hoy?");
        assertThat(respuesta.getCreadoEn()).isEqualTo(INSTANTE);
        assertThat(respuesta.isLeido()).isFalse();
    }

    @Test
    void enviarMensajeEnUnaConversacionCerradaDa400SinGuardar() {
        Conversacion cerrada = abiertaExistente();
        cerrada.setEstado(EstadoConversacion.CERRADA);
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByIdAndUsuarioId(50L, 3L)).thenReturn(Optional.of(cerrada));

        assertThatThrownBy(() -> servicio.enviarMensaje(50L, texto("Hola"), EMAIL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Esta conversación está cerrada.");

        verify(mensajeRepository, never()).save(any());
        assertThat(cerrada.getUltimoMensajeEn()).isEqualTo(AHORA_UTC.minusDays(2));
    }

    @Test
    void enviarMensajeConUnaCuentaIncompletaLanzaConLosFaltantesSinBuscarLaConversacion() {
        Usuario incompleta = cuentaVerificada();
        incompleta.setTelefono(null);
        existeLaCuenta(incompleta);

        assertThatThrownBy(() -> servicio.enviarMensaje(50L, texto("Hola"), EMAIL))
                .isInstanceOfSatisfying(CuentaNoVerificadaException.class,
                        e -> assertThat(e.getFaltantes()).containsExactly(DatoFaltante.TELEFONO));

        verifyNoInteractions(conversacionRepository);
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void enviarMensajeSobreUnaConversacionAjenaDa404SinGuardar() {
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByIdAndUsuarioId(77L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.enviarMensaje(77L, texto("Hola"), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void elMensaje21DeLaMismaCuentaDentroDeLaVentanaLanzaElLimiteConDiezMinutosYNoSeGuarda() {
        Conversacion conversacion = abiertaExistente();
        existeLaCuenta(cuentaVerificada());
        when(conversacionRepository.findByIdAndUsuarioId(50L, 3L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));

        for (int i = 1; i <= 20; i++) {
            servicio.enviarMensaje(50L, texto("mensaje " + i), EMAIL);
        }
        verify(mensajeRepository, times(20)).save(any(Mensaje.class));

        assertThatThrownBy(() -> servicio.enviarMensaje(50L, texto("mensaje 21"), EMAIL))
                .isInstanceOfSatisfying(LimiteDeIntentosException.class, e -> {
                    assertThat(e.getReintentarEnSegundos()).isEqualTo(600);
                    assertThat(e.getMessage())
                            .isEqualTo("Enviaste muchos mensajes seguidos. Esperá unos minutos y volvé a intentar.");
                });

        verify(mensajeRepository, times(20)).save(any(Mensaje.class));
    }

    @Test
    void elLoQuieroCuentaParaElMismoLimiteQueLosMensajes() {
        existeLaCuenta(cuentaVerificada());
        existeElAuto(EstadoPublicacion.DISPONIBLE);
        Conversacion existente = abiertaExistente();
        when(conversacionRepository.findFirstByUsuarioIdAndPublicacionIdAndTipoAndEstado(
                3L, 7L, TipoConversacion.COMPRA, EstadoConversacion.ABIERTA)).thenReturn(Optional.of(existente));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));

        for (int i = 1; i <= 20; i++) {
            servicio.iniciarCompra(pedido(7L, "consulta " + i), EMAIL);
        }

        assertThatThrownBy(() -> servicio.iniciarCompra(pedido(7L, "otra"), EMAIL))
                .isInstanceOf(LimiteDeIntentosException.class);
        assertThatThrownBy(() -> servicio.enviarMensaje(50L, texto("y otra"), EMAIL))
                .isInstanceOf(LimiteDeIntentosException.class);
        verify(mensajeRepository, times(20)).save(any(Mensaje.class));
    }

    @Test
    void elLimiteEsPorCuentaYNoAfectaAOtroUsuario() {
        Usuario otra = cuentaVerificada();
        otra.setId(4L);
        otra.setEmail("otra@cuenta.com");
        Conversacion deOtra = abiertaExistente();
        deOtra.setUsuario(otra);
        when(usuarioRepository.findByEmailIgnoreCase("otra@cuenta.com")).thenReturn(Optional.of(otra));
        when(conversacionRepository.findByIdAndUsuarioId(50L, 4L)).thenReturn(Optional.of(deOtra));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));
        for (int i = 0; i < 20; i++) {
            limitador.intentar("msg:3", 20, Duration.ofMinutes(10));
        }

        assertThat(servicio.enviarMensaje(50L, texto("Hola"), "otra@cuenta.com")).isNotNull();
    }
}
