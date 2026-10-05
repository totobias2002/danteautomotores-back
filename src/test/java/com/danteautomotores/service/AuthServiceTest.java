package com.danteautomotores.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.CuentaUserDetails;
import com.danteautomotores.security.JwtService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fija el contrato de D-04 (el registro web nunca crea ni promueve admins) y el del registro con identidad completa
 * (AUTH-01): datos normalizados, DNI único y mail de confirmación sin que su falla rompa el alta.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String MENSAJE_MAIL_REPETIDO = "Ya existe una cuenta con ese email";
    private static final String MENSAJE_DNI_REPETIDO = "Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña.";

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
    @Mock
    private TokenCuentaService tokenCuentaService;
    @Mock
    private NotificacionesService notificacionesService;

    @InjectMocks
    private AuthService authService;

    private static RegistroRequest registro() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("  Ana ");
        request.setApellido(" Pérez  ");
        request.setEmail("  Ana@X.com ");
        request.setPassword("12345678");
        request.setTelefono("11 2345-6789");
        request.setDni("30.111.222");
        return request;
    }

    /** Deja el camino feliz del registro armado: mail y DNI libres, hash, guardado con id y token. */
    private void prepararRegistroValido() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("30111222")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario guardado = invocacion.getArgument(0);
            guardado.setId(7L);
            return guardado;
        });
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
    }

    private Usuario usuarioGuardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void registroConRolAdminEnElBody_siempreCreaComprador() throws Exception {
        // Spring Boot ignora las propiedades desconocidas al deserializar
        RegistroRequest request = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue("{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"ana@x.com\","
                        + "\"password\":\"12345678\",\"telefono\":\"1123456789\",\"dni\":\"30111222\","
                        + "\"rol\":\"ADMIN\"}", RegistroRequest.class);
        prepararRegistroValido();

        AuthResponse respuesta = authService.registrar(request);

        assertThat(usuarioGuardado().getRol()).isEqualTo(Rol.COMPRADOR);
        assertThat(respuesta.getRol()).isEqualTo("COMPRADOR");
    }

    @Test
    void registroRequest_noDeclaraCampoDeRol() {
        Field[] campos = RegistroRequest.class.getDeclaredFields();

        assertThat(Arrays.stream(campos).map(Field::getName)).doesNotContain("rol");
        assertThat(Arrays.stream(campos).map(Field::getType)).doesNotContain(Rol.class);
    }

    @Test
    void registroValidoGuardaLosDatosNormalizadosYSinConfirmar() {
        prepararRegistroValido();

        authService.registrar(registro());

        Usuario guardado = usuarioGuardado();
        assertThat(guardado.getEmail()).isEqualTo("ana@x.com");
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getApellido()).isEqualTo("Pérez");
        assertThat(guardado.getTelefono()).isEqualTo("+5491123456789");
        assertThat(guardado.getDni()).isEqualTo("30111222");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash");
        assertThat(guardado.getRol()).isEqualTo(Rol.COMPRADOR);
        assertThat(guardado.isEmailConfirmado()).isFalse();
    }

    @Test
    void registroValidoEmiteElTokenDeConfirmacionYEncolaElMailConEseToken() {
        prepararRegistroValido();
        when(tokenCuentaService.emitir(7L, TipoTokenCuenta.CONFIRMAR_EMAIL)).thenReturn("tok-confirmacion");

        authService.registrar(registro());

        Usuario guardado = usuarioGuardado();
        InOrder orden = inOrder(usuarioRepository, tokenCuentaService, notificacionesService);
        orden.verify(usuarioRepository).saveAndFlush(any(Usuario.class));
        orden.verify(tokenCuentaService).emitir(7L, TipoTokenCuenta.CONFIRMAR_EMAIL);
        orden.verify(notificacionesService).enviarConfirmacionEmail(guardado, "tok-confirmacion");
    }

    @Test
    void registroValidoDevuelveLaSesionConElMailSinConfirmarComoFaltante() {
        prepararRegistroValido();
        when(verificacionCuenta.faltantes(any(Usuario.class))).thenReturn(List.of(DatoFaltante.EMAIL_SIN_CONFIRMAR));

        AuthResponse respuesta = authService.registrar(registro());

        assertThat(respuesta.getToken()).isEqualTo("token");
        assertThat(respuesta.getFaltantes()).containsExactly(DatoFaltante.EMAIL_SIN_CONFIRMAR);
        assertThat(respuesta.isCuentaVerificada()).isFalse();
        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtService).generateToken(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CuentaUserDetails.class);
    }

    @Test
    void siFallaEmitirElTokenElRegistroIgualDevuelveLaSesion() {
        prepararRegistroValido();
        when(tokenCuentaService.emitir(anyLong(), any())).thenThrow(new IllegalStateException("base caída"));

        AuthResponse respuesta = authService.registrar(registro());

        assertThat(respuesta.getToken()).isEqualTo("token");
        verifyNoInteractions(notificacionesService);
    }

    @Test
    void siFallaEncolarElMailElRegistroIgualDevuelveLaSesion() {
        prepararRegistroValido();
        when(tokenCuentaService.emitir(anyLong(), any())).thenReturn("tok");
        doThrow(new IllegalStateException("cola llena"))
                .when(notificacionesService).enviarConfirmacionEmail(any(Usuario.class), anyString());

        AuthResponse respuesta = authService.registrar(registro());

        assertThat(respuesta.getToken()).isEqualTo("token");
    }

    @Test
    void laAdvertenciaPorFallaDelMailNoLlevaMailDniNiTelefono() {
        Logger logger = (Logger) LoggerFactory.getLogger(AuthService.class);
        ListAppender<ILoggingEvent> capturado = new ListAppender<>();
        capturado.start();
        logger.addAppender(capturado);
        try {
            prepararRegistroValido();
            when(tokenCuentaService.emitir(anyLong(), any()))
                    .thenThrow(new IllegalStateException("fallo con ana@x.com 30111222 +5491123456789"));

            authService.registrar(registro());

            assertThat(capturado.list).isNotEmpty();
            for (ILoggingEvent evento : capturado.list) {
                assertThat(evento.getFormattedMessage())
                        .doesNotContain("ana@x.com").doesNotContain("30111222").doesNotContain("5491123456789");
                assertThat(evento.getThrowableProxy()).isNull();
            }
        } finally {
            logger.detachAppender(capturado);
        }
    }

    @Test
    void registroConEmailExistente_lanzaReglaDeNegocio() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registro()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_MAIL_REPETIDO);

        verify(usuarioRepository, never()).existsByDni(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(tokenCuentaService, notificacionesService);
    }

    @Test
    void registroConDniExistenteNoGuardaYNoDiceDeQuienEs() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("30111222")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registro()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_DNI_REPETIDO);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(tokenCuentaService, notificacionesService);
    }

    @Test
    void siSaveAndFlushChocaConElUniqueDelDniSeTraduceAlMensajeDelDni() {
        prepararRegistroValido();
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(violacion("uk_usuarios_dni"));

        assertThatThrownBy(() -> authService.registrar(registro()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_DNI_REPETIDO);

        verifyNoInteractions(tokenCuentaService, notificacionesService);
    }

    @Test
    void siSaveAndFlushChocaConElUniqueDelMailSeTraduceAlMensajeDelMail() {
        prepararRegistroValido();
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(violacion("uk_usuarios_email_lower"));

        assertThatThrownBy(() -> authService.registrar(registro()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_MAIL_REPETIDO);
    }

    @Test
    void otraViolacionDeIntegridadSeRelanza() {
        prepararRegistroValido();
        DataIntegrityViolationException otra = violacion("fk_otra_cosa");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(otra);

        assertThatThrownBy(() -> authService.registrar(registro())).isSameAs(otra);
    }

    @Test
    void unTelefonoInvalidoLanzaElMensajeDelNormalizadorYNoGuarda() {
        RegistroRequest request = registro();
        request.setTelefono("123");
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("celular argentino válido");

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unDniInvalidoLanzaElMensajeDelNormalizadorYNoGuarda() {
        RegistroRequest request = registro();
        request.setDni("12");
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("7 u 8 dígitos");

        verify(usuarioRepository, never()).existsByDni(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaContrasenaDeMasDe72BytesSeRechazaAunqueTengaMenosDe72Caracteres() {
        RegistroRequest request = registro();
        request.setPassword("é".repeat(40)); // 40 caracteres, 80 bytes en UTF-8
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("72");

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaContrasenaDeExactamente72BytesSeAcepta() {
        RegistroRequest request = registro();
        request.setPassword("a".repeat(72));
        prepararRegistroValidoConPassword("a".repeat(72));

        AuthResponse respuesta = authService.registrar(request);

        assertThat(respuesta.getToken()).isEqualTo("token");
    }

    private void prepararRegistroValidoConPassword(String password) {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("30111222")).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hash");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");
    }

    private static DataIntegrityViolationException violacion(String restriccion) {
        return new DataIntegrityViolationException("violación",
                new ConstraintViolationException("violación", new SQLException("duplicado"), restriccion));
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
        prepararRegistroValido();

        authService.registrar(registro());

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtService).generateToken(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CuentaUserDetails.class);
        verify(usuarioRepository, never()).findByEmailIgnoreCase(eq("ana@x.com"));
    }
}
