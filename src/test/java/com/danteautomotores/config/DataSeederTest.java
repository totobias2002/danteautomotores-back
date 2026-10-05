package com.danteautomotores.config;

import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DataSeederTest {

    private static final String PASSWORD = "ClaveSecreta123";
    private static final String HASH = "hash-bcrypt-simulado";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AgenciaRepository agenciaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder seeder(boolean prod, String email, String password, String nombre) {
        return seederConPerfiles(prod ? new String[]{"prod"} : new String[0], null, email, password, nombre);
    }

    // porDefecto null deja el perfil por defecto implícito de Spring ("default").
    private DataSeeder seederConPerfiles(String[] activos, String[] porDefecto, String email, String password,
                                         String nombre) {
        MockEnvironment environment = new MockEnvironment();
        if (activos.length > 0) {
            environment.setActiveProfiles(activos);
        }
        if (porDefecto != null) {
            environment.setDefaultProfiles(porDefecto);
        }
        DataSeeder seeder = new DataSeeder(usuarioRepository, agenciaRepository, passwordEncoder, environment);
        ReflectionTestUtils.setField(seeder, "adminEmail", email);
        ReflectionTestUtils.setField(seeder, "adminPassword", password);
        ReflectionTestUtils.setField(seeder, "adminNombre", nombre);
        return seeder;
    }

    private DataSeeder seederCompleto(boolean prod) {
        return seeder(prod, "dante@agencia.com", PASSWORD, "Dante");
    }

    @Test
    void sinAgencias_siembraDanteAutomotores() throws Exception {
        when(agenciaRepository.count()).thenReturn(0L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        seederCompleto(false).run(null);

        ArgumentCaptor<Agencia> captor = ArgumentCaptor.forClass(Agencia.class);
        verify(agenciaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Dante Automotores");
        assertThat(captor.getValue().getSlug()).isEqualTo("dante-automotores");
    }

    @Test
    void conAgencias_noSiembraNingunaNiBorra() throws Exception {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        seederCompleto(false).run(null);

        verify(agenciaRepository, never()).save(any());
        verify(agenciaRepository, never()).delete(any());
        verify(agenciaRepository, never()).deleteAll();
    }

    @Test
    void sinAdminYVariablesCompletas_creaAdminConPasswordHasheada(CapturedOutput output) throws Exception {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("dante@agencia.com")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        seederCompleto(true).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario admin = captor.getValue();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("dante@agencia.com");
        assertThat(admin.getNombre()).isEqualTo("Dante");
        assertThat(admin.getPasswordHash()).isEqualTo(HASH);
        assertThat(admin.isEmailConfirmado()).isTrue();
        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain(HASH);
    }

    @Test
    void conAdminExistente_noGuardaNingunUsuarioNiUsaElEncoder() throws Exception {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        seeder(true, "otro@agencia.com", "OtraClave12345", "Otro").run(null);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void faltanVariablesConPerfilProd_lanzaIllegalStateYNoGuarda() {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seeder(true, "", "", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageContaining("ADMIN_NOMBRE");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void faltanVariablesSinPerfilProd_soloAvisaYNoGuarda(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatCode(() -> seeder(false, "", "", "").run(null)).doesNotThrowAnyException();

        verify(usuarioRepository, never()).save(any());
        assertThat(output.getAll()).contains("ADMIN_EMAIL");
    }

    @Test
    void passwordCortaConPerfilProd_lanzaIllegalState(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seeder(true, "dante@agencia.com", "corta12", "Dante").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD")
                .hasMessageNotContaining("corta12");

        verify(usuarioRepository, never()).save(any());
        assertThat(output.getAll()).doesNotContain("corta12");
    }

    @Test
    void passwordCortaSinPerfilProd_noGuarda(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatCode(() -> seeder(false, "dante@agencia.com", "corta12", "Dante").run(null))
                .doesNotThrowAnyException();

        verify(usuarioRepository, never()).save(any());
        assertThat(output.getAll()).doesNotContain("corta12");
    }

    @Test
    void emailSinArrobaConPerfilProd_lanzaIllegalState() {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seeder(true, "no-es-un-email", PASSWORD, "Dante").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageNotContaining(PASSWORD);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emailYaRegistradoConPerfilProd_lanzaYNuncaPromueve(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("dante@agencia.com")).thenReturn(true);

        assertThatThrownBy(() -> seederCompleto(true).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageNotContaining(PASSWORD);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).findByEmail(any());
        assertThat(output.getAll()).doesNotContain(PASSWORD);
    }

    @Test
    void emailYaRegistradoSinPerfilProd_noGuardaNiPromueve(CapturedOutput output) throws Exception {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("dante@agencia.com")).thenReturn(true);

        seederCompleto(false).run(null);

        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).findByEmail(any());
        assertThat(output.getAll()).doesNotContain(PASSWORD);
    }

    @Test
    void emailConEspaciosYMayusculas_seNormalizaAntesDeValidarBuscarYGuardar(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("admin@dante.com")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThatCode(() -> seeder(true, "  Admin@Dante.com  ", PASSWORD, "Dante").run(null))
                .doesNotThrowAnyException();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@dante.com");
        verify(usuarioRepository).existsByEmailIgnoreCase("admin@dante.com");
        assertThat(output.getAll()).contains("admin@dante.com").doesNotContain(PASSWORD).doesNotContain(HASH);
    }

    @Test
    void emailDeSoloEspaciosConPerfilProd_cuentaComoFaltante() {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seeder(true, "    ", PASSWORD, "Dante").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");

        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"production", "railway", "staging", "qa", "prod,dev"})
    void sinAdminYVariablesFaltantesConPerfilQueNoEsDeDesarrollo_lanzaYNoGuarda(String perfiles) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seederConPerfiles(perfiles.split(","), null, "", "", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");

        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "local", "test"})
    void sinAdminYVariablesFaltantesConPerfilDeDesarrollo_soloAvisaYNoGuarda(String perfil, CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatCode(() -> seederConPerfiles(new String[]{perfil}, null, "", "", "").run(null))
                .doesNotThrowAnyException();

        verify(usuarioRepository, never()).save(any());
        assertThat(output.getAll()).contains("ADMIN_EMAIL");
    }

    @Test
    void sinPerfilActivoPeroConPerfilPorDefectoProd_lanza() {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> seederConPerfiles(new String[0], new String[]{"prod"}, "", "", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void conAdminExistenteYPerfilStaging_noLanzaAunqueFaltenLasVariablesNiGuarda() {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(true);

        assertThatCode(() -> seederConPerfiles(new String[]{"staging"}, null, "", "", "").run(null))
                .doesNotThrowAnyException();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emailDeSoloEspaciosSinPerfilProd_soloAvisaYNoGuarda(CapturedOutput output) {
        when(agenciaRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByRol(Rol.ADMIN)).thenReturn(false);

        assertThatCode(() -> seeder(false, "    ", PASSWORD, "Dante").run(null)).doesNotThrowAnyException();

        verify(usuarioRepository, never()).save(any());
        assertThat(output.getAll()).contains("ADMIN_EMAIL");
    }
}
