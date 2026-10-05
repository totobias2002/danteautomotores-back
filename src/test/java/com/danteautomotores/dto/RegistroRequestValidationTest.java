package com.danteautomotores.dto;

import com.danteautomotores.dto.auth.RegistroRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static RegistroRequest valido() {
        RegistroRequest request = new RegistroRequest();
        request.setNombre("Ana");
        request.setApellido("Pérez");
        request.setEmail("ana@x.com");
        request.setPassword("12345678");
        request.setTelefono("11 2345-6789");
        request.setDni("30.111.222");
        return request;
    }

    private Map<String, String> errores(RegistroRequest request) {
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        return violaciones.stream().collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
    }

    @Test
    void unPedidoCompletoEsValido() {
        assertThat(errores(valido())).isEmpty();
    }

    @Test
    void sinApellidoTelefonoODniHayUnaViolacionEnEseCampo() {
        RegistroRequest sinApellido = valido();
        sinApellido.setApellido(null);
        RegistroRequest sinTelefono = valido();
        sinTelefono.setTelefono(null);
        RegistroRequest sinDni = valido();
        sinDni.setDni(null);

        assertThat(errores(sinApellido)).containsOnlyKeys("apellido");
        assertThat(errores(sinTelefono)).containsOnlyKeys("telefono");
        assertThat(errores(sinDni)).containsOnlyKeys("dni");
    }

    @Test
    void apellidoTelefonoYDniSoloConEspaciosSeRechazan() {
        RegistroRequest request = valido();
        request.setApellido("   ");
        request.setTelefono("  ");
        request.setDni(" ");

        assertThat(errores(request)).containsOnlyKeys("apellido", "telefono", "dni");
    }

    @Test
    void unMailSinFormatoOConMasDe254CaracteresEsInvalido() {
        RegistroRequest sinFormato = valido();
        sinFormato.setEmail("no-es-un-mail");
        RegistroRequest largo = valido();
        largo.setEmail("a".repeat(250) + "@x.com");

        assertThat(errores(sinFormato)).containsOnlyKeys("email");
        assertThat(errores(largo)).containsOnlyKeys("email");
    }

    @Test
    void laContrasenaAceptaDe8A72Caracteres() {
        RegistroRequest corta = valido();
        corta.setPassword("1234567");
        RegistroRequest minima = valido();
        minima.setPassword("12345678");
        RegistroRequest maxima = valido();
        maxima.setPassword("a".repeat(72));
        RegistroRequest larga = valido();
        larga.setPassword("a".repeat(73));

        assertThat(errores(corta)).containsOnlyKeys("password");
        assertThat(errores(minima)).isEmpty();
        assertThat(errores(maxima)).isEmpty();
        assertThat(errores(larga)).containsOnlyKeys("password");
        assertThat(errores(larga).get("password")).contains("entre 8 y 72 caracteres");
    }

    @Test
    void nombreApellidoTelefonoYDniTienenTopeDeLargo() {
        RegistroRequest nombre = valido();
        nombre.setNombre("n".repeat(101));
        RegistroRequest apellido = valido();
        apellido.setApellido("a".repeat(101));
        RegistroRequest telefono = valido();
        telefono.setTelefono("1".repeat(31));
        RegistroRequest dni = valido();
        dni.setDni("1".repeat(21));

        assertThat(errores(nombre)).containsOnlyKeys("nombre");
        assertThat(errores(apellido)).containsOnlyKeys("apellido");
        assertThat(errores(telefono)).containsOnlyKeys("telefono");
        assertThat(errores(dni)).containsOnlyKeys("dni");
    }

    @Test
    void elToStringNoImprimeLaContrasenaElTelefonoNiElDni() {
        String texto = valido().toString();

        assertThat(texto).doesNotContain("12345678").doesNotContain("11 2345-6789").doesNotContain("30.111.222");
    }

    @Test
    void noDeclaraUnCampoDeRolNiDePrivacidad() {
        Field[] campos = RegistroRequest.class.getDeclaredFields();

        assertThat(Arrays.stream(campos).map(Field::getName))
                .doesNotContain("rol")
                .noneMatch(nombre -> nombre.toLowerCase().contains("privacidad")
                        || nombre.toLowerCase().contains("acept"));
    }
}
