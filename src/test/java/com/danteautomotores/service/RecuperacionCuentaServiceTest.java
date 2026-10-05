package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperacionCuentaServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneId.of("UTC"));
    private static final String TOKEN = "t0ken-ficticio-t0ken-ficticio-t0ken-ficti";
    private static final String EMAIL = "ana@x.com";
    private static final String IP = "203.0.113.9";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenCuentaService tokenCuentaService;
    @Mock
    private NotificacionesService notificacionesService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private RecuperacionCuentaService service;

    @BeforeEach
    void preparar() {
        // El limitador es el real: los límites se prueban de verdad.
        service = new RecuperacionCuentaService(usuarioRepository, tokenCuentaService, notificacionesService,
                new LimitadorDeIntentos(), passwordEncoder, RELOJ);
    }

    private Usuario cuenta() {
        return Usuario.builder().id(7L).nombre("Ana").email(EMAIL).passwordHash("hash-viejo")
                .rol(Rol.COMPRADOR).emailConfirmado(false).build();
    }

    // ---- confirmarEmail ----

    @Test
    void unTokenValidoDeConfirmacionMarcaLaCuentaConfirmada() {
        Usuario usuario = cuenta();
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.CONFIRMAR_EMAIL)).thenReturn(Optional.of(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.confirmarEmail(TOKEN);

        assertThat(usuario.isEmailConfirmado()).isTrue();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void confirmarUnaCuentaYaConfirmadaNoFallaNiVuelveAGuardar() {
        Usuario usuario = cuenta();
        usuario.setEmailConfirmado(true);
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.CONFIRMAR_EMAIL)).thenReturn(Optional.of(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThatCode(() -> service.confirmarEmail(TOKEN)).doesNotThrowAnyException();

        assertThat(usuario.isEmailConfirmado()).isTrue();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void unTokenInvalidoDeConfirmacionDaElMensajeGenericoSinCambios() {
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.CONFIRMAR_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarEmail(TOKEN))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El link no es válido o ya venció. Pedí uno nuevo.");

        verifyNoInteractions(usuarioRepository);
    }

    // ---- solicitarRestablecimiento ----

    @Test
    void paraUnaCuentaExistenteEmiteElTokenYMandaElMail() {
        Usuario usuario = cuenta();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenCuentaService.emitir(7L, TipoTokenCuenta.RESTABLECER_CONTRASENA)).thenReturn(TOKEN);

        service.solicitarRestablecimiento("  Ana@X.com ", IP);

        verify(notificacionesService).enviarRestablecerContrasena(usuario, TOKEN);
    }

    @Test
    void paraUnaCuentaInexistenteNoEmiteNiMandaNiLanza() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@x.com")).thenReturn(Optional.empty());

        assertThatCode(() -> service.solicitarRestablecimiento("nadie@x.com", IP)).doesNotThrowAnyException();

        verifyNoInteractions(tokenCuentaService, notificacionesService);
    }

    @Test
    void laCuartaSolicitudDelMismoMailEnLaHoraNoMandaNadaNiLanza() {
        Usuario usuario = cuenta();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(tokenCuentaService.emitir(anyLong(), any())).thenReturn(TOKEN);

        for (int i = 0; i < 3; i++) {
            service.solicitarRestablecimiento(EMAIL, IP);
        }
        assertThatCode(() -> service.solicitarRestablecimiento(EMAIL, IP)).doesNotThrowAnyException();

        verify(notificacionesService, org.mockito.Mockito.times(3)).enviarRestablecerContrasena(any(), anyString());
    }

    @Test
    void laOnceavaSolicitudDesdeLaMismaIpNoMandaNadaAunqueSeanMailsDistintos() {
        when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(cuenta()));
        when(tokenCuentaService.emitir(anyLong(), any())).thenReturn(TOKEN);

        for (int i = 0; i < 10; i++) {
            service.solicitarRestablecimiento("m" + i + "@x.com", IP);
        }
        assertThatCode(() -> service.solicitarRestablecimiento("otro@x.com", IP)).doesNotThrowAnyException();

        verify(notificacionesService, org.mockito.Mockito.times(10)).enviarRestablecerContrasena(any(), anyString());
    }

    // ---- restablecerContrasena ----

    @Test
    void conTokenValidoCambiaElHashFijaElInstanteConfirmaElMailDescartaYNotifica() {
        Usuario usuario = cuenta();
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.RESTABLECER_CONTRASENA)).thenReturn(Optional.of(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.restablecerContrasena(TOKEN, "contraseña-nueva-1");

        assertThat(passwordEncoder.matches("contraseña-nueva-1", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.getPasswordCambiadaEn()).isEqualTo(LocalDateTime.of(2026, 10, 5, 12, 0, 0));
        assertThat(usuario.isEmailConfirmado()).isTrue();
        var orden = inOrder(usuarioRepository, tokenCuentaService, notificacionesService);
        orden.verify(usuarioRepository).save(usuario);
        orden.verify(tokenCuentaService).descartarPendientes(7L);
        orden.verify(notificacionesService).enviarContrasenaCambiada(usuario);
    }

    @Test
    void conTokenInvalidoDaElMensajeGenericoYNoCambiaNada() {
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.RESTABLECER_CONTRASENA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecerContrasena(TOKEN, "contraseña-nueva-1"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("El link no es válido o ya venció. Pedí uno nuevo.");

        verifyNoInteractions(usuarioRepository, notificacionesService);
        verify(tokenCuentaService, never()).descartarPendientes(anyLong());
    }

    @Test
    void unaContrasenaDeMasDe72BytesSeRechazaSinConsumirElToken() {
        // 40 caracteres de 2 bytes = 80 bytes: cabe en el @Size(max = 72) del DTO pero BCrypt la truncaría.
        String larga = "ñ".repeat(40);

        assertThatThrownBy(() -> service.restablecerContrasena(TOKEN, larga))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("72 bytes");

        verifyNoInteractions(tokenCuentaService, usuarioRepository, notificacionesService);
    }

    @Test
    void unaCuentaSinContrasenaDeGooglePuedeDefinirUna() {
        Usuario soloGoogle = cuenta();
        soloGoogle.setPasswordHash(null);
        soloGoogle.setGoogleSub("sub-ficticio");
        when(tokenCuentaService.consumir(eq(TOKEN), eq(TipoTokenCuenta.RESTABLECER_CONTRASENA))).thenReturn(Optional.of(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(soloGoogle));

        service.restablecerContrasena(TOKEN, "contraseña-nueva-1");

        assertThat(passwordEncoder.matches("contraseña-nueva-1", soloGoogle.getPasswordHash())).isTrue();
        assertThat(soloGoogle.getGoogleSub()).isEqualTo("sub-ficticio");
    }

    @Test
    void elInstanteDelCambioNuncaRetrocedeNiSeRepiteEnElMismoSegundo() {
        Usuario usuario = cuenta();
        // Un cambio anterior en el futuro (reloj que retrocedió) o en el mismo segundo no puede dejar vivas las sesiones.
        usuario.setPasswordCambiadaEn(LocalDateTime.of(2026, 10, 5, 12, 0, 0, 500_000_000));
        when(tokenCuentaService.consumir(TOKEN, TipoTokenCuenta.RESTABLECER_CONTRASENA)).thenReturn(Optional.of(7L));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.restablecerContrasena(TOKEN, "contraseña-nueva-1");

        assertThat(usuario.getPasswordCambiadaEn()).isEqualTo(LocalDateTime.of(2026, 10, 5, 12, 0, 1));
    }
}
