package com.danteautomotores.service;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.entity.Consulta;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.ConsultaRepository;
import com.danteautomotores.repository.PublicacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @InjectMocks
    private ConsultaService servicio;

    private ConsultaRequest pedido(Long publicacionId) {
        ConsultaRequest request = new ConsultaRequest();
        request.setPublicacionId(publicacionId);
        request.setNombreComprador("Ana");
        request.setEmailComprador("ana@test.com");
        request.setMensaje("¿Sigue disponible?");
        return request;
    }

    private void existeConEstado(EstadoPublicacion estado) {
        when(publicacionRepository.findById(7L))
                .thenReturn(Optional.of(Publicacion.builder().id(7L).estado(estado).build()));
    }

    @Test
    void unaConsultaSobreUnAutoVendidoSeRechazaYNoSeGuarda() {
        existeConEstado(EstadoPublicacion.VENDIDO);

        assertThatThrownBy(() -> servicio.crear(pedido(7L)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Este auto ya se vendió");

        verify(consultaRepository, never()).save(any());
    }

    @Test
    void unaConsultaSobreUnAutoReservadoSeGuarda() {
        existeConEstado(EstadoPublicacion.RESERVADO);

        assertThat(servicio.crear(pedido(7L))).isNotNull();

        verify(consultaRepository).save(any(Consulta.class));
    }

    @Test
    void unaConsultaSobreUnAutoDisponibleSeGuarda() {
        existeConEstado(EstadoPublicacion.DISPONIBLE);

        assertThat(servicio.crear(pedido(7L))).isNotNull();

        verify(consultaRepository).save(any(Consulta.class));
    }

    @Test
    void unaConsultaSobreUnAutoInexistenteDa404() {
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido(99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(consultaRepository, never()).save(any());
    }
}
