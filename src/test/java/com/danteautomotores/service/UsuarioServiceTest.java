package com.danteautomotores.service;

import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
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

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String EMAIL = "ana@x.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void prepararService() {
        usuarioService = new UsuarioService(usuarioRepository, new VerificacionCuenta());
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
}
