package com.danteautomotores.service;

import com.danteautomotores.dto.solicitudventa.SolicitudVentaRequest;
import com.danteautomotores.dto.solicitudventa.SolicitudVentaResponse;
import com.danteautomotores.entity.SolicitudVenta;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.EstadoSolicitudVenta;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.CuentaNoVerificadaException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.SolicitudVentaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudVentaServiceTest {

    private static final String EMAIL = "ana@cuenta.com";

    @Mock
    private SolicitudVentaRepository solicitudVentaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    // La regla real: así el test falla si cambia lo que significa "verificada".
    @Spy
    private VerificacionCuenta verificacionCuenta = new VerificacionCuenta();
    @InjectMocks
    private SolicitudVentaService servicio;

    private SolicitudVentaRequest formulario() {
        SolicitudVentaRequest request = new SolicitudVentaRequest();
        request.setMarca("Ford");
        request.setModelo("Fiesta");
        request.setAnio(2018);
        request.setKilometraje(60000);
        request.setNombreVendedor("Luis Gómez");
        request.setTelefonoVendedor("11 5555-0000");
        request.setCiudad("Rosario");
        request.setDescripcion("Único dueño");
        return request;
    }

    private Usuario cuentaVerificada() {
        return Usuario.builder()
                .id(3L).nombre("Ana").apellido("Pérez").email(EMAIL)
                .telefono("+5491112345678").dni("30123456")
                .rol(Rol.COMPRADOR).emailConfirmado(true)
                .build();
    }

    @Test
    void unaCuentaVerificadaGuardaLaSolicitudConLosDatosDelFormularioYEstadoPendiente() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(cuentaVerificada()));

        SolicitudVentaResponse respuesta = servicio.crear(formulario(), EMAIL);

        ArgumentCaptor<SolicitudVenta> captor = ArgumentCaptor.forClass(SolicitudVenta.class);
        verify(solicitudVentaRepository).save(captor.capture());
        SolicitudVenta guardada = captor.getValue();
        assertThat(guardada.getMarca()).isEqualTo("Ford");
        assertThat(guardada.getModelo()).isEqualTo("Fiesta");
        assertThat(guardada.getAnio()).isEqualTo(2018);
        // El contacto de la venta sale del formulario, no de la cuenta.
        assertThat(guardada.getNombreVendedor()).isEqualTo("Luis Gómez");
        assertThat(guardada.getTelefonoVendedor()).isEqualTo("11 5555-0000");
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudVenta.PENDIENTE);
        assertThat(respuesta.getEstado()).isEqualTo(EstadoSolicitudVenta.PENDIENTE);
    }

    @Test
    void unaCuentaIncompletaLanzaConLosFaltantesYNoGuarda() {
        Usuario incompleta = cuentaVerificada();
        incompleta.setApellido(null);
        incompleta.setTelefono(null);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(incompleta));

        assertThatThrownBy(() -> servicio.crear(formulario(), EMAIL))
                .isInstanceOfSatisfying(CuentaNoVerificadaException.class,
                        e -> assertThat(e.getFaltantes())
                                .containsExactly(DatoFaltante.APELLIDO, DatoFaltante.TELEFONO));

        verify(solicitudVentaRepository, never()).save(any());
    }

    @Test
    void unaCuentaInexistenteDa404YNoGuarda() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(formulario(), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(solicitudVentaRepository, never()).save(any());
    }
}
