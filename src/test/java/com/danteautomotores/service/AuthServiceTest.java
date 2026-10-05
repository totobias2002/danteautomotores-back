package com.danteautomotores.service;

import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.CuentaUserDetails;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private static LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static Usuario cuenta(String email, String hash) {
        return Usuario.builder()
                .nombre("Ana")
                .apellido("Perez")
                .email(email)
                .passwordHash(hash)
                .rol(Rol.COMPRADOR)
                .dni("30111222")
                .telefono("+5491123456789")
                .build();
    }

    @Test
    void loginConMailEnMayusculasYConEspaciosBuscaPorElMailNormalizado() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(cuenta("Ana@x.com", "hash")));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        AuthResponse respuesta = authService.login(login("  ANA@X.com ", "12345678"));

        verify(usuarioRepository).findByEmailIgnoreCase("ana@x.com");
        ArgumentCaptor<UsernamePasswordAuthenticationToken> credenciales =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(credenciales.capture());
        assertThat(credenciales.getValue().getPrincipal()).isEqualTo("ana@x.com");
        assertThat(respuesta.getToken()).isEqualTo("token");
        assertThat(respuesta.getEmail()).isEqualTo("Ana@x.com");
    }

    @Test
    void loginDeUnaCuentaSinContrasenaLanzaCredencialesInvalidasSinLlamarAlAuthenticationManager() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(cuenta("ana@x.com", null)));

        assertThatThrownBy(() -> authService.login(login("ana@x.com", "cualquiera")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginDeUnMailInexistenteDaLaMismaExcepcionQueUnaContrasenaIncorrecta() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@x.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(login("nadie@x.com", "12345678")))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void iniciarSesionPideElTokenConUnaCuentaUserDetailsConSuPca() {
        Usuario usuario = cuenta("ana@x.com", "hash");
        usuario.setPasswordCambiadaEn(LocalDateTime.ofEpochSecond(1_700_000_000L, 0, ZoneOffset.UTC));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        authService.iniciarSesion(usuario);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtService).generateToken(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CuentaUserDetails.class);
        assertThat(((CuentaUserDetails) captor.getValue()).getPcaSegundos()).isEqualTo(1_700_000_000L);
        assertThat(captor.getValue().getUsername()).isEqualTo("ana@x.com");
    }

    @Test
    void iniciarSesionDeUnaCuentaSinContrasenaNoLanzaYDevuelveLosFaltantes() {
        Usuario usuario = cuenta("ana@x.com", null);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
        when(verificacionCuenta.faltantes(usuario))
                .thenReturn(List.of(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR));

        AuthResponse respuesta = authService.iniciarSesion(usuario);

        assertThat(respuesta.getToken()).isEqualTo("token");
        assertThat(respuesta.getFaltantes()).containsExactly(DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR);
        assertThat(respuesta.isCuentaVerificada()).isFalse();
    }

    @Test
    void laRespuestaDeSesionNuncaTraeDniNiTelefono() {
        Usuario usuario = cuenta("ana@x.com", "hash");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        AuthResponse respuesta = authService.iniciarSesion(usuario);

        assertThat(Arrays.stream(AuthResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("dni", "telefono");
        assertThat(respuesta.toString()).doesNotContain("30111222").doesNotContain("+5491123456789");
    }

    @Test
    void registrarEmiteLaSesionPorIniciarSesion() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Ana");
        request.setEmail("ana@x.com");
        request.setPassword("12345678");
        when(usuarioRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        authService.registrar(request);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtService).generateToken(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CuentaUserDetails.class);
        verify(usuarioRepository, never()).findByEmailIgnoreCase(eq("ana@x.com"));
    }
}
