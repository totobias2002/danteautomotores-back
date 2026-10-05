package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.CambiarContrasenaRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.service.identidad.NormalizadorDeContacto;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String EMAIL = "ana@x.com";

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuthService authService;
    @Mock
    private TokenCuentaService tokenCuentaService;
    @Mock
    private NotificacionesService notificacionesService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UsuarioService usuarioService;

    @BeforeEach
    void prepararService() {
        // El limitador y el codificador son los reales: los límites y la verificación de la contraseña se prueban de verdad.
        usuarioService = new UsuarioService(usuarioRepository, new VerificacionCuenta(), passwordEncoder, authService,
                tokenCuentaService, notificacionesService, new LimitadorDeIntentos(), RELOJ);
    }

    private Usuario comprador() {
        return Usuario.builder()
                .id(7L)
                .nombre("Ana")
                .email(EMAIL)
                .passwordHash("hash")
                .rol(Rol.COMPRADOR)
                .build();
    }

    private ActualizarPerfilRequest pedido(String dni) {
        ActualizarPerfilRequest request = new ActualizarPerfilRequest();
        request.setNombre("  Ana  ");
        request.setApellido("  Pérez ");
        request.setTelefono("011 15 1234-5678");
        request.setDni(dni);
        return request;
    }

    private void cuentaExistente(Usuario usuario) {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private void guardadoDevuelveLoRecibido() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---- obtenerPerfil ----

    @Test
    void obtenerPerfilDevuelveLaCuentaConFaltantesYBanderas() {
        Usuario usuario = comprador();
        usuario.setGoogleSub("sub-123");
        usuario.setPasswordHash(null);
        cuentaExistente(usuario);

        UsuarioResponse perfil = usuarioService.obtenerPerfil(EMAIL);

        assertThat(perfil.getId()).isEqualTo(7L);
        assertThat(perfil.getEmail()).isEqualTo(EMAIL);
        assertThat(perfil.getRol()).isEqualTo("COMPRADOR");
        assertThat(perfil.isTieneContrasena()).isFalse();
        assertThat(perfil.isTieneGoogle()).isTrue();
        assertThat(perfil.isCuentaVerificada()).isFalse();
        assertThat(perfil.getFaltantes()).containsExactly(
                DatoFaltante.APELLIDO, DatoFaltante.TELEFONO, DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR);
    }

    @Test
    void obtenerPerfilDeUnaCuentaCompletaEstaVerificadaYSinFaltantes() {
        Usuario usuario = comprador();
        usuario.setApellido("Pérez");
        usuario.setTelefono("+5491112345678");
        usuario.setDni("30123456");
        usuario.setEmailConfirmado(true);
        cuentaExistente(usuario);

        UsuarioResponse perfil = usuarioService.obtenerPerfil(EMAIL);

        assertThat(perfil.isCuentaVerificada()).isTrue();
        assertThat(perfil.getFaltantes()).isEmpty();
        assertThat(perfil.isTieneContrasena()).isTrue();
        assertThat(perfil.isTieneGoogle()).isFalse();
        assertThat(perfil.getDni()).isEqualTo("30123456");
    }

    @Test
    void cuentaInexistenteLanzaResourceNotFound() {
        when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPerfil("nadie@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> usuarioService.actualizarPerfil("nadie@x.com", pedido("30123456")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    // ---- actualizarPerfil ----

    @Test
    void completaNombreApellidoTelefonoNormalizadoYDniNormalizado() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        guardadoDevuelveLoRecibido();
        when(usuarioRepository.existsByDniAndIdNot("30123456", 7L)).thenReturn(false);

        UsuarioResponse perfil = usuarioService.actualizarPerfil(EMAIL, pedido("30.123.456"));

        assertThat(usuario.getNombre()).isEqualTo("Ana");
        assertThat(usuario.getApellido()).isEqualTo("Pérez");
        assertThat(usuario.getTelefono()).isEqualTo("+5491112345678");
        assertThat(usuario.getDni()).isEqualTo("30123456");
        // Sigue sin ser verificada porque falta confirmar el mail.
        assertThat(perfil.getFaltantes()).containsExactly(DatoFaltante.EMAIL_SIN_CONFIRMAR);
        assertThat(perfil.isCuentaVerificada()).isFalse();
    }

    @Test
    void conElDniYaCargadoElMismoDniConPuntosSeAceptaYNoCambiaNada() {
        Usuario usuario = comprador();
        usuario.setDni("30123456");
        cuentaExistente(usuario);
        guardadoDevuelveLoRecibido();

        usuarioService.actualizarPerfil(EMAIL, pedido("30.123.456"));

        assertThat(usuario.getDni()).isEqualTo("30123456");
        verify(usuarioRepository, never()).existsByDniAndIdNot(anyString(), anyLong());
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void conElDniYaCargadoUnaEdicionSinDniNoLoToca() {
        Usuario usuario = comprador();
        usuario.setDni("30123456");
        cuentaExistente(usuario);
        guardadoDevuelveLoRecibido();

        usuarioService.actualizarPerfil(EMAIL, pedido(null));

        assertThat(usuario.getDni()).isEqualTo("30123456");
    }

    @Test
    void conElDniYaCargadoUnDniDistintoSeRechazaYNoGuarda() {
        Usuario usuario = comprador();
        usuario.setDni("30123456");
        cuentaExistente(usuario);

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("40999888")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El DNI no se puede modificar desde la web. Escribinos si hay un error.");

        assertThat(usuario.getDni()).isEqualTo("30123456");
        assertThat(usuario.getApellido()).isNull();
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unDniDeOtraCuentaSeRechazaConElMensajeDeD04YNoGuarda() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        when(usuarioRepository.existsByDniAndIdNot("30123456", 7L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("30.123.456")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña.")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(EMAIL).doesNotContain("30123456"));

        assertThat(usuario.getDni()).isNull();
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void siElUniqueDeLaBaseGanaLaCarreraSeTraduceAlMismoMensaje() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        when(usuarioRepository.existsByDniAndIdNot("30123456", 7L)).thenReturn(false);
        ConstraintViolationException causa = new ConstraintViolationException(
                "duplicate", new SQLException("x"), "uk_usuarios_dni");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("no se guardó", causa));

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("30123456")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña.");
    }

    @Test
    void otraViolacionDeIntegridadSeRelanzaTalCual() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        when(usuarioRepository.existsByDniAndIdNot("30123456", 7L)).thenReturn(false);
        ConstraintViolationException causa = new ConstraintViolationException(
                "otra", new SQLException("x"), "uk_usuarios_google_sub");
        DataIntegrityViolationException original = new DataIntegrityViolationException("otra", causa);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(original);

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("30123456")))
                .isSameAs(original);
    }

    @Test
    void unCompradorSinDniNiEnElPedidoNiEnLaCuentaRecibeIngresaTuDni() {
        cuentaExistente(comprador());

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido(null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ingresá tu DNI.");
        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("   ")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Ingresá tu DNI.");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unAdminSinDniGuardaSinProblema() {
        Usuario admin = comprador();
        admin.setRol(Rol.ADMIN);
        cuentaExistente(admin);
        guardadoDevuelveLoRecibido();

        UsuarioResponse perfil = usuarioService.actualizarPerfil(EMAIL, pedido(null));

        assertThat(admin.getDni()).isNull();
        assertThat(admin.getTelefono()).isEqualTo("+5491112345678");
        assertThat(perfil.getFaltantes()).isEmpty();
    }

    @Test
    void unTelefonoInvalidoLanzaElMensajeDelNormalizadorYNoGuarda() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        ActualizarPerfilRequest request = pedido("30123456");
        request.setTelefono("12345");

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(NormalizadorDeContacto.MENSAJE_CELULAR_INVALIDO);

        assertThat(usuario.getTelefono()).isNull();
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unDniConFormatoInvalidoLanzaElMensajeDelNormalizador() {
        cuentaExistente(comprador());

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(EMAIL, pedido("123")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(NormalizadorDeContacto.MENSAJE_DNI_INVALIDO);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    // ---- el request y la respuesta no exponen datos de más ----

    @Test
    void actualizarPerfilRequestNoDeclaraCampoDeEmailNiDeRol() {
        List<String> nombres = Arrays.stream(ActualizarPerfilRequest.class.getDeclaredFields())
                .map(Field::getName).toList();

        assertThat(nombres).containsExactlyInAnyOrder("nombre", "apellido", "telefono", "dni");
        assertThat(nombres).doesNotContain("email", "rol", "googleSub", "emailConfirmado");
        assertThat(Arrays.stream(ActualizarPerfilRequest.class.getDeclaredFields()).map(Field::getType))
                .doesNotContain(Rol.class);
    }

    @Test
    void elToStringDelRequestNoContieneElTelefonoNiElDni() {
        ActualizarPerfilRequest request = pedido("30123456");
        request.setTelefono("+5491112345678");

        assertThat(request.toString()).doesNotContain("30123456").doesNotContain("5491112345678");
    }

    @Test
    void elToStringDeLaRespuestaNoContieneElTelefonoNiElDni() {
        Usuario usuario = comprador();
        usuario.setDni("30123456");
        usuario.setTelefono("+5491112345678");
        cuentaExistente(usuario);

        UsuarioResponse perfil = usuarioService.obtenerPerfil(EMAIL);

        assertThat(perfil.toString()).doesNotContain("30123456").doesNotContain("5491112345678");
    }

    // ---- cambiarContrasena (AUTH-05, D-19) ----

    private Usuario conContrasena(String actual) {
        Usuario usuario = comprador();
        usuario.setPasswordHash(passwordEncoder.encode(actual));
        return usuario;
    }

    private CambiarContrasenaRequest cambio(String actual, String nueva) {
        CambiarContrasenaRequest request = new CambiarContrasenaRequest();
        request.setActual(actual);
        request.setNueva(nueva);
        return request;
    }

    @Test
    void conLaContrasenaActualCorrectaCambiaElHashFijaElInstanteDescartaNotificaYDevuelveLaSesion() {
        Usuario usuario = conContrasena("actual-1234");
        cuentaExistente(usuario);
        AuthResponse sesionNueva = AuthResponse.builder().token("jwt-nuevo").build();
        when(authService.iniciarSesion(usuario)).thenReturn(sesionNueva);

        AuthResponse respuesta = usuarioService.cambiarContrasena(EMAIL, cambio("actual-1234", "nueva-5678"));

        assertThat(respuesta).isSameAs(sesionNueva);
        assertThat(passwordEncoder.matches("nueva-5678", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.getPasswordCambiadaEn()).isEqualTo(LocalDateTime.of(2026, 10, 5, 12, 0, 0));
        var orden = inOrder(usuarioRepository, tokenCuentaService, notificacionesService, authService);
        orden.verify(usuarioRepository).save(usuario);
        orden.verify(tokenCuentaService).descartarPendientes(7L);
        orden.verify(notificacionesService).enviarContrasenaCambiada(usuario);
        // La sesión se emite con la cuenta ya actualizada: su token lleva el pca nuevo.
        orden.verify(authService).iniciarSesion(usuario);
    }

    @Test
    void laContrasenaActualIncorrectaDaElMensajeNoCambiaNadaYRegistraElFallo() {
        Usuario usuario = conContrasena("actual-1234");
        String hashAntes = usuario.getPasswordHash();
        cuentaExistente(usuario);

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(EMAIL, cambio("otra-cosa", "nueva-5678")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("La contraseña actual no es correcta.");

        assertThat(usuario.getPasswordHash()).isEqualTo(hashAntes);
        assertThat(usuario.getPasswordCambiadaEn()).isNull();
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(tokenCuentaService, notificacionesService, authService);
    }

    @Test
    void unaCuentaSinContrasenaDeGoogleRecibeElMensajeQueLaMandaAOlvideMiContrasena() {
        Usuario soloGoogle = comprador();
        soloGoogle.setPasswordHash(null);
        cuentaExistente(soloGoogle);

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(EMAIL, cambio("lo-que-sea", "nueva-5678")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Tu cuenta ingresa con Google: definí una contraseña desde \"Olvidé mi contraseña\".");

        assertThat(soloGoogle.getPasswordHash()).isNull();
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(authService, notificacionesService);
    }

    @Test
    void elSextoIntentoConContrasenaActualIncorrectaEnLosQuinceMinutosDaLimiteDeIntentos() {
        Usuario usuario = conContrasena("actual-1234");
        cuentaExistente(usuario);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> usuarioService.cambiarContrasena(EMAIL, cambio("mal", "nueva-5678")))
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
        // Aun con la contraseña correcta, la cuenta ya está bloqueada.
        assertThatThrownBy(() -> usuarioService.cambiarContrasena(EMAIL, cambio("actual-1234", "nueva-5678")))
                .isInstanceOfSatisfying(LimiteDeIntentosException.class,
                        e -> assertThat(e.getReintentarEnSegundos()).isEqualTo(900));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void unaContrasenaNuevaDeMasDe72BytesSeRechazaSinCambiarNada() {
        Usuario usuario = conContrasena("actual-1234");
        String hashAntes = usuario.getPasswordHash();
        cuentaExistente(usuario);

        assertThatThrownBy(() -> usuarioService.cambiarContrasena(EMAIL, cambio("actual-1234", "ñ".repeat(40))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("72 bytes");

        assertThat(usuario.getPasswordHash()).isEqualTo(hashAntes);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(authService);
    }

    @Test
    void unCambioDeContrasenaNoRetrocedeNiRepiteElInstanteAnterior() {
        Usuario usuario = conContrasena("actual-1234");
        usuario.setPasswordCambiadaEn(LocalDateTime.of(2026, 10, 5, 12, 0, 0));
        cuentaExistente(usuario);

        usuarioService.cambiarContrasena(EMAIL, cambio("actual-1234", "nueva-5678"));

        assertThat(usuario.getPasswordCambiadaEn()).isEqualTo(LocalDateTime.of(2026, 10, 5, 12, 0, 1));
    }

    @Test
    void elToStringDelCambioDeContrasenaNoContieneNingunaContrasena() {
        assertThat(cambio("actual-1234", "nueva-5678").toString())
                .doesNotContain("actual-1234").doesNotContain("nueva-5678");
    }

    // ---- reenviarConfirmacion (D-21) ----

    @Test
    void reenviarConElMailSinConfirmarEmiteElTokenYEncolaElMail() {
        Usuario usuario = comprador();
        cuentaExistente(usuario);
        when(tokenCuentaService.emitir(7L, TipoTokenCuenta.CONFIRMAR_EMAIL)).thenReturn("token-ficticio");

        String mensaje = usuarioService.reenviarConfirmacion(EMAIL);

        assertThat(mensaje).isEqualTo("Te mandamos un mail para confirmar tu cuenta. Revisá también la carpeta de spam.");
        verify(notificacionesService).enviarConfirmacionEmail(usuario, "token-ficticio");
    }

    @Test
    void reenviarConElMailYaConfirmadoNoEmiteNiMandaNada() {
        Usuario usuario = comprador();
        usuario.setEmailConfirmado(true);
        cuentaExistente(usuario);

        String mensaje = usuarioService.reenviarConfirmacion(EMAIL);

        assertThat(mensaje).isEqualTo("Tu mail ya está confirmado.");
        verifyNoInteractions(tokenCuentaService, notificacionesService);
    }

    @Test
    void elCuartoReenvioEnLaHoraLanzaLimiteDeIntentos() {
        cuentaExistente(comprador());
        when(tokenCuentaService.emitir(anyLong(), any())).thenReturn("token-ficticio");

        for (int i = 0; i < 3; i++) {
            usuarioService.reenviarConfirmacion(EMAIL);
        }
        assertThatThrownBy(() -> usuarioService.reenviarConfirmacion(EMAIL))
                .isInstanceOfSatisfying(LimiteDeIntentosException.class,
                        e -> assertThat(e.getReintentarEnSegundos()).isEqualTo(3600));

        verify(notificacionesService, times(3)).enviarConfirmacionEmail(any(), anyString());
    }

    @Test
    void reenviarConElMailYaConfirmadoNoConsumeElCupoDeReenvios() {
        Usuario usuario = comprador();
        usuario.setEmailConfirmado(true);
        cuentaExistente(usuario);

        for (int i = 0; i < 10; i++) {
            usuarioService.reenviarConfirmacion(EMAIL);
        }

        verifyNoInteractions(tokenCuentaService);
    }
}
