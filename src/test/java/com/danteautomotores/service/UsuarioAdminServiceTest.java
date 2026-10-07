package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.usuario.UsuarioFichaResponse;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

    private static final LocalDateTime UNA_FECHA = LocalDateTime.of(2026, 3, 15, 10, 0);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private RegistroDeMensajes registroDeMensajes;

    private UsuarioAdminService servicio;

    @BeforeEach
    void armarElServicio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-07T15:30:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        // El armado de resúmenes es el real de la bandeja: la ficha y la bandeja comparten el mismo.
        ConversacionAdminService conversacionAdminService = new ConversacionAdminService(
                conversacionRepository, mensajeRepository, usuarioRepository, registroDeMensajes, reloj);
        servicio = new UsuarioAdminService(usuarioRepository, conversacionRepository, conversacionAdminService,
                new VerificacionCuenta());
    }

    private Usuario.UsuarioBuilder comprador() {
        return Usuario.builder().id(7L).nombre("Ana").apellido("Lopez").email("ana@x.com")
                .telefono("+5491155550000").dni("30123456").rol(Rol.COMPRADOR).emailConfirmado(true)
                .fechaRegistro(UNA_FECHA);
    }

    private Conversacion conversacion(Long id, Usuario usuario) {
        return Conversacion.builder().id(id).tipo(TipoConversacion.COTIZACION).estado(EstadoConversacion.ABIERTA)
                .usuario(usuario).creadaEn(UNA_FECHA).ultimoMensajeEn(UNA_FECHA).build();
    }

    @Test
    void unCompradorVerificadoDevuelveTodosSusDatosDeContacto() {
        Usuario ana = comprador().build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(7L)).thenReturn(List.of());

        UsuarioFichaResponse ficha = servicio.obtenerFicha(7L);

        assertThat(ficha.getId()).isEqualTo(7L);
        assertThat(ficha.getNombre()).isEqualTo("Ana");
        assertThat(ficha.getApellido()).isEqualTo("Lopez");
        assertThat(ficha.getEmail()).isEqualTo("ana@x.com");
        assertThat(ficha.getTelefono()).isEqualTo("+5491155550000");
        assertThat(ficha.getDni()).isEqualTo("30123456");
        assertThat(ficha.isEmailConfirmado()).isTrue();
        assertThat(ficha.isCuentaVerificada()).isTrue();
        assertThat(ficha.getFaltantes()).isEmpty();
        assertThat(ficha.getFechaRegistro()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void unaCuentaConDatosFaltantesLosListaYNoEstaVerificada() {
        Usuario vieja = comprador().apellido(null).dni(null).emailConfirmado(false).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(vieja));
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(7L)).thenReturn(List.of());

        UsuarioFichaResponse ficha = servicio.obtenerFicha(7L);

        assertThat(ficha.isCuentaVerificada()).isFalse();
        assertThat(ficha.isEmailConfirmado()).isFalse();
        assertThat(ficha.getFaltantes()).containsExactly(DatoFaltante.APELLIDO, DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR);
    }

    @Test
    void elHistorialTraeSusConversacionesEnElOrdenDelRepositorioConUltimoMensajeYNoLeidos() {
        Usuario ana = comprador().build();
        Conversacion reciente = conversacion(60L, ana);
        Conversacion vieja = conversacion(50L, ana);
        Mensaje ultimo = Mensaje.builder().id(600L).conversacion(reciente).autorTipo(AutorMensaje.USUARIO)
                .texto("Hola, me interesa").creadoEn(UNA_FECHA).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(7L))
                .thenReturn(List.of(reciente, vieja));
        when(mensajeRepository.findUltimosPorConversaciones(anyCollection())).thenReturn(List.of(ultimo));
        when(mensajeRepository.contarNoLeidosPorConversacion(anyCollection(), eq(AutorMensaje.USUARIO)))
                .thenReturn(List.of(new MensajeRepository.ConteoPorConversacion() {
                    @Override
                    public Long getConversacionId() {
                        return 60L;
                    }

                    @Override
                    public Long getCantidad() {
                        return 2L;
                    }
                }));

        List<ConversacionResumenResponse> historial = servicio.obtenerFicha(7L).getConversaciones();

        assertThat(historial).extracting(ConversacionResumenResponse::getId).containsExactly(60L, 50L);
        assertThat(historial.get(0).getUltimoMensaje()).isEqualTo("Hola, me interesa");
        assertThat(historial.get(0).getNoLeidos()).isEqualTo(2);
        assertThat(historial.get(1).getUltimoMensaje()).isNull();
        assertThat(historial.get(1).getNoLeidos()).isZero();
        assertThat(historial.get(0).getUsuario().getId()).isEqualTo(7L);
    }

    @Test
    void sinConversacionesElHistorialEsUnaListaVacia() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(comprador().build()));
        when(conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(7L)).thenReturn(List.of());

        assertThat(servicio.obtenerFicha(7L).getConversaciones()).isNotNull().isEmpty();
        verifyNoInteractions(mensajeRepository);
    }

    @Test
    void unaCuentaAdminYUnIdInexistenteDanElMismoErrorConElMismoMensaje() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(comprador().id(1L).rol(Rol.ADMIN).build()));
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable deAdmin = org.assertj.core.api.Assertions.catchThrowable(() -> servicio.obtenerFicha(1L));
        Throwable deInexistente = org.assertj.core.api.Assertions.catchThrowable(() -> servicio.obtenerFicha(999L));

        assertThat(deAdmin).isInstanceOf(ResourceNotFoundException.class).hasMessage("No existe el usuario");
        assertThat(deInexistente).isInstanceOf(ResourceNotFoundException.class).hasMessage(deAdmin.getMessage());
        assertThatThrownBy(() -> servicio.obtenerFicha(1L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(conversacionRepository);
    }
}
