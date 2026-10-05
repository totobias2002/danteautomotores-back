package com.danteautomotores.service;

import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.JwtService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fija el contrato de D-04: el registro web nunca crea ni promueve admins.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private VerificacionCuenta verificacionCuenta;

    @InjectMocks
    private AuthService authService;

    @Test
    void registroConRolAdminEnElBody_siempreCreaComprador() throws Exception {
        // Spring Boot ignora las propiedades desconocidas al deserializar
        RegistroRequest request = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue("{\"nombre\":\"Ana\",\"email\":\"ana@x.com\",\"password\":\"12345678\",\"rol\":\"ADMIN\"}",
                        RegistroRequest.class);
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        AuthResponse respuesta = authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.COMPRADOR);
        assertThat(respuesta.getRol()).isEqualTo("COMPRADOR");
    }

    @Test
    void registroRequest_noDeclaraCampoDeRol() {
        Field[] campos = RegistroRequest.class.getDeclaredFields();

        assertThat(Arrays.stream(campos).map(Field::getName)).doesNotContain("rol");
        assertThat(Arrays.stream(campos).map(Field::getType)).doesNotContain(Rol.class);
    }

    @Test
    void registroConEmailExistente_lanzaIllegalArgument() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Ana");
        request.setEmail("ana@x.com");
        request.setPassword("12345678");
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ya existe una cuenta con ese email");

        verify(usuarioRepository, never()).save(any());
    }
}
