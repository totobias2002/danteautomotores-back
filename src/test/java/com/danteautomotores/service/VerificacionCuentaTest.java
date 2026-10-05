package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** La regla de cuenta verificada (D-01): nombre, apellido, teléfono, DNI y mail confirmado. */
class VerificacionCuentaTest {

    private final VerificacionCuenta verificacion = new VerificacionCuenta();

    private Usuario.UsuarioBuilder comprador() {
        return Usuario.builder()
                .nombre("Ana")
                .email("ana@dante.test")
                .rol(Rol.COMPRADOR)
                .apellido("Pérez")
                .telefono("+5491155550000")
                .dni("30123456")
                .emailConfirmado(true);
    }

    @Test
    void cuentaCompletaConMailConfirmadoEsVerificadaYNoTieneFaltantes() {
        Usuario usuario = comprador().build();

        assertThat(verificacion.faltantes(usuario)).isEmpty();
        assertThat(verificacion.estaVerificada(usuario)).isTrue();
    }

    @Test
    void faltaElApellido() {
        Usuario usuario = comprador().apellido(null).build();

        assertThat(verificacion.faltantes(usuario)).containsExactly(DatoFaltante.APELLIDO);
        assertThat(verificacion.estaVerificada(usuario)).isFalse();
    }

    @Test
    void faltaElTelefono() {
        Usuario usuario = comprador().telefono(null).build();

        assertThat(verificacion.faltantes(usuario)).containsExactly(DatoFaltante.TELEFONO);
        assertThat(verificacion.estaVerificada(usuario)).isFalse();
    }

    @Test
    void faltaElDni() {
        Usuario usuario = comprador().dni(null).build();

        assertThat(verificacion.faltantes(usuario)).containsExactly(DatoFaltante.DNI);
        assertThat(verificacion.estaVerificada(usuario)).isFalse();
    }

    @Test
    void faltanApellidoYDniJuntos() {
        Usuario usuario = comprador().apellido(null).dni(null).build();

        assertThat(verificacion.faltantes(usuario)).containsExactly(DatoFaltante.APELLIDO, DatoFaltante.DNI);
    }

    @Test
    void elMailSinConfirmarAgregaEmailSinConfirmar() {
        Usuario usuario = comprador().emailConfirmado(false).build();

        assertThat(verificacion.faltantes(usuario)).containsExactly(DatoFaltante.EMAIL_SIN_CONFIRMAR);
        assertThat(verificacion.estaVerificada(usuario)).isFalse();
    }

    @Test
    void sinNingunDatoElOrdenEsApellidoTelefonoDniYMailSinConfirmar() {
        Usuario usuario = comprador().apellido(null).telefono(null).dni(null).emailConfirmado(false).build();

        assertThat(verificacion.faltantes(usuario))
                .containsExactly(DatoFaltante.APELLIDO, DatoFaltante.TELEFONO, DatoFaltante.DNI, DatoFaltante.EMAIL_SIN_CONFIRMAR);
    }

    @Test
    void unTextoSoloConEspaciosCuentaComoFaltante() {
        Usuario usuario = comprador().apellido("   ").telefono("\t").dni(" ").build();

        assertThat(verificacion.faltantes(usuario))
                .containsExactly(DatoFaltante.APELLIDO, DatoFaltante.TELEFONO, DatoFaltante.DNI);
    }

    @Test
    void unAdminSinNingunDatoNoTieneFaltantesYEstaVerificado() {
        Usuario admin = Usuario.builder()
                .nombre("Dante")
                .email("admin@dante.test")
                .rol(Rol.ADMIN)
                .build();

        assertThat(verificacion.faltantes(admin)).isEmpty();
        assertThat(verificacion.estaVerificada(admin)).isTrue();
    }

    @Test
    void unaCuentaSoloGoogleSinContrasenaSeEvaluaIgualQueElResto() {
        Usuario completa = comprador().passwordHash(null).googleSub("sub-123").build();
        Usuario incompleta = comprador().passwordHash(null).googleSub("sub-456").apellido(null).dni(null).build();

        assertThat(verificacion.estaVerificada(completa)).isTrue();
        assertThat(verificacion.faltantes(incompleta)).containsExactly(DatoFaltante.APELLIDO, DatoFaltante.DNI);
    }
}
