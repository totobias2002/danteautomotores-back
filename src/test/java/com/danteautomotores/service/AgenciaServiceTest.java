package com.danteautomotores.service;

import com.danteautomotores.dto.agencia.AgenciaRequest;
import com.danteautomotores.dto.agencia.AgenciaResponse;
import com.danteautomotores.enums.ZonaAgencia;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.PublicacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenciaServiceTest {

    @Mock
    private AgenciaRepository agenciaRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @InjectMocks
    private AgenciaService agenciaService;

    @Test
    void eliminarUnaAgenciaConAutosPublicadosLanzaErrorYNoBorraNada() {
        when(agenciaRepository.existsById(1L)).thenReturn(true);
        when(publicacionRepository.existsByAgenciaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> agenciaService.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tiene autos publicados");

        verify(agenciaRepository, never()).deleteById(any());
    }

    @Test
    void eliminarUnaAgenciaSinAutosLaBorra() {
        when(agenciaRepository.existsById(2L)).thenReturn(true);
        when(publicacionRepository.existsByAgenciaId(2L)).thenReturn(false);

        agenciaService.eliminar(2L);

        verify(agenciaRepository).deleteById(2L);
    }

    @Test
    void eliminarUnaAgenciaInexistenteDa404SinConsultarLosAutos() {
        when(agenciaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> agenciaService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe una agencia con id: 99");

        verify(agenciaRepository, never()).deleteById(any());
    }

    private static AgenciaRequest requestAgencia(ZonaAgencia zona) {
        AgenciaRequest request = new AgenciaRequest();
        request.setNombre("Dante Norte");
        request.setEmailContacto("norte@dante.com");
        request.setZona(zona);
        return request;
    }

    @Test
    void crearConZonaLaGuardaYLaDevuelve() {
        AgenciaResponse respuesta = agenciaService.crear(requestAgencia(ZonaAgencia.ZONA_NORTE));

        ArgumentCaptor<Agencia> guardada = ArgumentCaptor.forClass(Agencia.class);
        verify(agenciaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getZona()).isEqualTo(ZonaAgencia.ZONA_NORTE);
        assertThat(respuesta.getZona()).isEqualTo(ZonaAgencia.ZONA_NORTE);
    }

    @Test
    void crearSinZonaLaDejaEnNull() {
        AgenciaResponse respuesta = agenciaService.crear(requestAgencia(null));

        ArgumentCaptor<Agencia> guardada = ArgumentCaptor.forClass(Agencia.class);
        verify(agenciaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getZona()).isNull();
        assertThat(respuesta.getZona()).isNull();
    }

    @Test
    void actualizarCambiaLaZonaYConZonaNullaLaBorra() {
        Agencia existente = Agencia.builder().id(3L).nombre("Dante").slug("dante").zona(ZonaAgencia.CABA).build();
        when(agenciaRepository.findById(3L)).thenReturn(Optional.of(existente));

        AgenciaResponse cambiada = agenciaService.actualizar(3L, requestAgencia(ZonaAgencia.INTERIOR));
        assertThat(existente.getZona()).isEqualTo(ZonaAgencia.INTERIOR);
        assertThat(cambiada.getZona()).isEqualTo(ZonaAgencia.INTERIOR);

        AgenciaResponse sinZona = agenciaService.actualizar(3L, requestAgencia(null));
        assertThat(existente.getZona()).isNull();
        assertThat(sinZona.getZona()).isNull();
    }

    @Test
    void listarPidePorIdParaTenerUnOrdenEstable() {
        when(agenciaRepository.findAll(any(Sort.class))).thenReturn(List.of(new Agencia()));

        assertThat(agenciaService.listar()).hasSize(1);

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(agenciaRepository).findAll(sort.capture());
        assertThat(sort.getValue()).isEqualTo(Sort.by("id"));
    }
}
