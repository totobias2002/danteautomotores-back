package com.danteautomotores.service;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.entity.Consulta;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.ConsultaRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    private static final String EMAIL = "ana@cuenta.com";

    @Mock
    private ConsultaRepository consultaRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    // La regla real: así el test falla si cambia lo que significa "verificada".
    @Spy
    private VerificacionCuenta verificacionCuenta = new VerificacionCuenta();
    @InjectMocks
    private ConsultaService servicio;

    private ConsultaRequest pedido(Long publicacionId) {
        ConsultaRequest request = new ConsultaRequest();
        request.setPublicacionId(publicacionId);
        request.setMensaje("¿Sigue disponible?");
        return request;
    }

    private Usuario cuentaVerificada() {
        return Usuario.builder()
                .id(3L).nombre("Ana").apellido("Pérez").email(EMAIL)
                .telefono("+5491112345678").dni("30123456")
                .rol(Rol.COMPRADOR).emailConfirmado(true)
                .build();
    }

    private void existeLaCuenta(Usuario usuario) {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private void existeConEstado(EstadoPublicacion estado) {
        when(publicacionRepository.findById(7L))
                .thenReturn(Optional.of(Publicacion.builder().id(7L).estado(estado).build()));
    }

    @Test
    void unaCuentaVerificadaGuardaLaConsultaConLosDatosDeLaCuenta() {
        existeLaCuenta(cuentaVerificada());
        existeConEstado(EstadoPublicacion.DISPONIBLE);

        servicio.crear(pedido(7L), EMAIL);

        ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
        verify(consultaRepository).save(captor.capture());
        Consulta guardada = captor.getValue();
        assertThat(guardada.getNombreComprador()).isEqualTo("Ana Pérez");
        assertThat(guardada.getEmailComprador()).isEqualTo(EMAIL);
        assertThat(guardada.getTelefonoComprador()).isEqualTo("+5491112345678");
        assertThat(guardada.getMensaje()).isEqualTo("¿Sigue disponible?");
    }

    @Test
    void unaCuentaIncompletaLanzaConLosFaltantesYNoGuarda() {
        Usuario incompleta = cuentaVerificada();
        incompleta.setDni(null);
        incompleta.setEmailConfirmado(false);
        existeLaCuenta(incompleta);

        assertThatThrownBy(() -> servicio.crear(pedido(7L), EMAIL))
                .isInstanceOfSatisfying(CuentaNoVerificadaException.class,
                        e -> assertThat(e.getFaltantes())
                                .containsExactly(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR));

        verify(consultaRepository, never()).save(any());
        // La autorización se decide antes de revelar si la publicación existe.
        verifyNoInteractions(publicacionRepository);
    }

    @Test
    void unaCuentaInexistenteDa404() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido(7L), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(consultaRepository, never()).save(any());
    }

    @Test
    void unaConsultaSobreUnAutoVendidoSeRechazaYNoSeGuarda() {
        existeLaCuenta(cuentaVerificada());
        existeConEstado(EstadoPublicacion.VENDIDO);

        assertThatThrownBy(() -> servicio.crear(pedido(7L), EMAIL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Este auto ya se vendió");

        verify(consultaRepository, never()).save(any());
    }

    @Test
    void unaConsultaSobreUnAutoReservadoSeGuarda() {
        existeLaCuenta(cuentaVerificada());
        existeConEstado(EstadoPublicacion.RESERVADO);

        assertThat(servicio.crear(pedido(7L), EMAIL)).isNotNull();

        verify(consultaRepository).save(any(Consulta.class));
    }

    @Test
    void unaConsultaSobreUnAutoDisponibleSeGuarda() {
        existeLaCuenta(cuentaVerificada());
        existeConEstado(EstadoPublicacion.DISPONIBLE);

        assertThat(servicio.crear(pedido(7L), EMAIL)).isNotNull();

        verify(consultaRepository).save(any(Consulta.class));
    }

    @Test
    void unaConsultaSobreUnAutoInexistenteDa404() {
        existeLaCuenta(cuentaVerificada());
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido(99L), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(consultaRepository, never()).save(any());
    }
}
