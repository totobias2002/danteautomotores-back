package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.security.GoogleIdTokenVerifier;
import com.danteautomotores.security.IdentidadGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CREDENTIAL = "credential-de-prueba";
    private static final String SUB = "sub-123";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private GoogleIdTokenVerifier verificador;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuthService authService;

    private GoogleAuthService servicio;
    private final AuthResponse sesion = AuthResponse.builder().token("jwt").build();

    @BeforeEach
    void armar() {
        servicio = new GoogleAuthService(verificador, usuarioRepository, authService, RELOJ);
    }

    private static IdentidadGoogle identidad(String email, boolean verificado, String nombre, String apellido) {
        return new IdentidadGoogle(SUB, email, verificado, nombre, apellido,
                nombre == null ? null : (apellido == null ? nombre : nombre + " " + apellido));
    }

    private void tokenValido(IdentidadGoogle identidad) {
        when(verificador.verificar(CREDENTIAL)).thenReturn(identidad);
    }

    private static Usuario cuentaDeContrasena(boolean emailConfirmado) {
        return Usuario.builder()
                .id(7L)
                .nombre("Ana")
                .email("ana@example.com")
                .passwordHash("hash-bcrypt")
                .rol(Rol.COMPRADOR)
                .emailConfirmado(emailConfirmado)
                .build();
    }

    private Usuario guardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void unSubYaVinculadoEntraAEsaCuentaSinCrearNada() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(true);
        cuenta.setGoogleSub(SUB);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.of(cuenta));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        assertThat(servicio.entrar(CREDENTIAL)).isSameAs(sesion);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void unSubNuevoConUnMailSinCuentaCreaUnCompradorSoloDeGoogle() {
        tokenValido(identidad("Nuevo@Example.COM", true, "Nora", "Gómez"));
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(any(Usuario.class))).thenReturn(sesion);

        assertThat(servicio.entrar(CREDENTIAL)).isSameAs(sesion);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario nuevo = captor.getValue();
        assertThat(nuevo.getRol()).isEqualTo(Rol.COMPRADOR);
        assertThat(nuevo.getNombre()).isEqualTo("Nora");
        assertThat(nuevo.getApellido()).isEqualTo("Gómez");
        assertThat(nuevo.getEmail()).isEqualTo("nuevo@example.com");
        assertThat(nuevo.isEmailConfirmado()).isTrue();
        assertThat(nuevo.getGoogleSub()).isEqualTo(SUB);
        assertThat(nuevo.getTelefono()).isNull();
        assertThat(nuevo.getDni()).isNull();
        assertThat(nuevo.getPasswordHash()).isNull();
        verify(authService).iniciarSesion(nuevo);
    }

    @Test
    void sinNombreEnElTokenLaCuentaNuevaUsaLaParteLocalDelMailYSinApellido() {
        tokenValido(identidad("sin.nombre@example.com", true, null, null));
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("sin.nombre@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(any(Usuario.class))).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("sin.nombre");
        assertThat(captor.getValue().getApellido()).isNull();
    }

    @Test
    void unaCuentaDeContrasenaConMailConfirmadoSeUneYConservaSuContrasena() {
        tokenValido(identidad("Ana@Example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(true);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        assertThat(servicio.entrar(CREDENTIAL)).isSameAs(sesion);

        Usuario unida = guardado();
        assertThat(unida.getId()).isEqualTo(7L);
        assertThat(unida.getGoogleSub()).isEqualTo(SUB);
        assertThat(unida.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(unida.getPasswordCambiadaEn()).isNull();
        assertThat(unida.isEmailConfirmado()).isTrue();
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void unaCuentaConMailSinConfirmarSeUneDescartaLaContrasenaYMarcaElInstante() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(false);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        Usuario unida = guardado();
        assertThat(unida.getGoogleSub()).isEqualTo(SUB);
        assertThat(unida.isEmailConfirmado()).isTrue();
        assertThat(unida.getPasswordHash()).isNull();
        assertThat(unida.getPasswordCambiadaEn()).isEqualTo(LocalDateTime.now(RELOJ));
        // Los datos de la cuenta no se pierden.
        assertThat(unida.getId()).isEqualTo(7L);
        assertThat(unida.getNombre()).isEqualTo("Ana");
    }

    @Test
    void unaCuentaSoloGoogleSinContrasenaNoMarcaCambioDeContrasenaAlUnirse() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(false);
        cuenta.setPasswordHash(null);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        assertThat(guardado().getPasswordCambiadaEn()).isNull();
    }

    @Test
    void siLaCuentaUnidaNoTeniaApellidoYGoogleTraeUnoSeCompleta() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(true);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        assertThat(guardado().getApellido()).isEqualTo("Pérez");
    }

    @Test
    void siLaCuentaUnidaYaTeniaApellidoNoSePisa() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(true);
        cuenta.setApellido("Gutiérrez");
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        assertThat(guardado().getApellido()).isEqualTo("Gutiérrez");
    }

    @Test
    void siGoogleNoTraeApellidoElDeLaCuentaUnidaQuedaComoEstaba() {
        tokenValido(identidad("ana@example.com", true, "Ana", null));
        Usuario cuenta = cuentaDeContrasena(true);
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(authService.iniciarSesion(cuenta)).thenReturn(sesion);

        servicio.entrar(CREDENTIAL);

        assertThat(guardado().getApellido()).isNull();
    }

    @Test
    void emailVerifiedEnFalseSeRechazaYNoGuardaNada() {
        tokenValido(identidad("ana@example.com", false, "Ana", "Pérez"));

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Google no confirmó tu mail: usá tu mail y contraseña.");

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(usuarioRepository, authService);
    }

    @Test
    void unTokenSinMailSeTrataComoNoConfirmado() {
        tokenValido(identidad(null, true, "Ana", "Pérez"));

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Google no confirmó tu mail: usá tu mail y contraseña.");

        verifyNoInteractions(usuarioRepository, authService);
    }

    @Test
    void unaCuentaAdminConEseMailSeRechazaYNoSeModifica() {
        tokenValido(identidad("admin@example.com", true, "Admin", null));
        Usuario admin = Usuario.builder().id(1L).nombre("Admin").email("admin@example.com")
                .passwordHash("hash-admin").rol(Rol.ADMIN).emailConfirmado(false).build();
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Esta cuenta no puede ingresar con Google.");

        assertThat(admin.getGoogleSub()).isNull();
        assertThat(admin.getPasswordHash()).isEqualTo("hash-admin");
        assertThat(admin.getPasswordCambiadaEn()).isNull();
        assertThat(admin.isEmailConfirmado()).isFalse();
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(authService);
    }

    @Test
    void unaCuentaDelMailYaUnidaAOtroSubSeRechazaYNoSeModifica() {
        tokenValido(identidad("ana@example.com", true, "Ana", "Pérez"));
        Usuario cuenta = cuentaDeContrasena(false);
        cuenta.setGoogleSub("otro-sub");
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL))
                .isInstanceOf(ReglaDeNegocioException.class);

        assertThat(cuenta.getGoogleSub()).isEqualTo("otro-sub");
        assertThat(cuenta.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(cuenta.getPasswordCambiadaEn()).isNull();
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(authService);
    }

    @Test
    void unTokenInvalidoPropagaBadCredentialsYNoConsultaLaBase() {
        when(verificador.verificar(CREDENTIAL)).thenThrow(new BadCredentialsException("x"));

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(usuarioRepository, authService);
    }

    @Test
    void siElAltaFallaPorUnaCarreraSeReintentaLaBusquedaPorSubYSeUsaLaCuenta() {
        tokenValido(identidad("nuevo@example.com", true, "Nora", "Gómez"));
        Usuario ganadora = Usuario.builder().id(9L).nombre("Nora").email("nuevo@example.com")
                .googleSub(SUB).rol(Rol.COMPRADOR).emailConfirmado(true).build();
        when(usuarioRepository.findByGoogleSub(SUB))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ganadora));
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));
        when(authService.iniciarSesion(ganadora)).thenReturn(sesion);

        assertThat(servicio.entrar(CREDENTIAL)).isSameAs(sesion);

        verify(usuarioRepository, times(1)).saveAndFlush(any());
        verify(usuarioRepository, times(2)).findByGoogleSub(SUB);
    }

    @Test
    void siElAltaFallaYLaCuentaNoApareceSeRelanzaLaExcepcion() {
        tokenValido(identidad("nuevo@example.com", true, "Nora", "Gómez"));
        DataIntegrityViolationException conflicto = new DataIntegrityViolationException("otro conflicto");
        when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(conflicto);

        assertThatThrownBy(() -> servicio.entrar(CREDENTIAL)).isSameAs(conflicto);

        verify(usuarioRepository, times(2)).findByGoogleSub(SUB);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void elServicioNoEsTransaccionalParaPoderReintentarTrasLaCarrera() {
        assertThat(GoogleAuthService.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(GoogleAuthService.class.getMethods())
                .noneMatch(m -> m.isAnnotationPresent(Transactional.class));
    }
}
