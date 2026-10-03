package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.FotoPublicacionRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private AgenciaRepository agenciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FotoPublicacionRepository fotoPublicacionRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private PublicacionService publicacionService;

    @BeforeEach
    void autenticarComoAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("admin@dante.com", null));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static PublicacionRequest requestValido(Long agenciaId) {
        PublicacionRequest request = new PublicacionRequest();
        request.setAgenciaId(agenciaId);
        request.setMarca("Toyota");
        request.setModelo("Corolla");
        request.setAnio(2020);
        request.setPrecio(new BigDecimal("15000"));
        return request;
    }

    private static Agencia agencia(Long id, String nombre) {
        return Agencia.builder().id(id).nombre(nombre).slug(nombre.toLowerCase()).build();
    }

    private void stubAdminAutenticado() {
        Usuario admin = Usuario.builder().id(1L).email("admin@dante.com").rol(Rol.ADMIN).build();
        when(usuarioRepository.findByEmail("admin@dante.com")).thenReturn(Optional.of(admin));
    }

    @Test
    void crearConUnaAgenciaExistenteGuardaLaPublicacionEnEsaAgencia() {
        stubAdminAutenticado();
        Agencia sucursal = agencia(2L, "Sucursal");
        when(agenciaRepository.findById(2L)).thenReturn(Optional.of(sucursal));

        publicacionService.crear(requestValido(2L));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getAgencia()).isSameAs(sucursal);
        assertThat(guardada.getValue().getMarca()).isEqualTo("Toyota");
    }

    @Test
    void actualizarConOtraAgenciaMueveLaPublicacionAEsaAgencia() {
        Agencia original = agencia(1L, "Dante");
        Agencia destino = agencia(2L, "Sucursal");
        Publicacion existente = Publicacion.builder().id(10L).agencia(original)
                .marca("Toyota").modelo("Corolla").build();
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(agenciaRepository.findById(2L)).thenReturn(Optional.of(destino));

        publicacionService.actualizar(10L, requestValido(2L));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getAgencia()).isSameAs(destino);
    }

    @Test
    void crearConUnaAgenciaInexistenteLanzaNotFoundYNoGuardaNada() {
        when(agenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicacionService.crear(requestValido(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe una agencia con id: 99");

        verify(publicacionRepository, never()).save(any());
    }
}
