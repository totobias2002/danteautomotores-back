package com.danteautomotores.dto;

import com.danteautomotores.dto.agencia.AgenciaRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AgenciaRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static AgenciaRequest valido() {
        AgenciaRequest request = new AgenciaRequest();
        request.setNombre("Dante Automotores");
        request.setEmailContacto("ventas@dante.com");
        return request;
    }

    private Map<String, String> errores(AgenciaRequest request) {
        Set<ConstraintViolation<AgenciaRequest>> violaciones = validator.validate(request);
        return violaciones.stream().collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
    }

    @Test
    void unRequestMinimoEsValido() {
        assertThat(errores(valido())).isEmpty();
    }

    @Test
    void unRequestCompletoEsValido() {
        AgenciaRequest request = valido();
        request.setLogo("https://res.cloudinary.com/x/logo.png");
        request.setDescripcion("d".repeat(5000)); // TEXT: sin límite de columna
        request.setDireccion("Av. Cabildo 2450, CABA");
        request.setTelefonoContacto("011 4555-1234");

        assertThat(errores(request)).isEmpty();
    }

    @Test
    void nombreYEmailEnBlancoSeRechazanConMensajeEnEspanol() {
        AgenciaRequest request = valido();
        request.setNombre(" ");
        request.setEmailContacto("");

        assertThat(errores(request))
                .containsEntry("nombre", "El nombre es obligatorio")
                .containsEntry("emailContacto", "El email de contacto es obligatorio");
    }

    @Test
    void emailSinFormatoDeEmailSeRechaza() {
        for (String malo : new String[]{"xx", "sin-arroba.com", "dos@@dante.com", "@dante.com"}) {
            AgenciaRequest request = valido();
            request.setEmailContacto(malo);
            assertThat(errores(request)).as(malo)
                    .containsEntry("emailContacto", "El email de contacto no tiene un formato válido");
        }
    }

    @Test
    void textosMasLargosQueLaColumnaSeRechazanConMensajeEnEspanol() {
        AgenciaRequest request = valido();
        String largo = "a".repeat(256);
        request.setNombre(largo);
        request.setLogo(largo);
        request.setDireccion(largo);
        request.setTelefonoContacto(largo);

        assertThat(errores(request)).containsOnlyKeys("nombre", "logo", "direccion", "telefonoContacto");
        assertThat(errores(request).get("nombre")).isEqualTo("El nombre no puede superar los 255 caracteres");
    }

    @Test
    void unEmailBienFormadoPeroMasLargoQueLaColumnaSeRechaza() {
        AgenciaRequest request = valido();
        // Local de 60 y cuatro etiquetas de dominio de 60: formato válido (local <= 64, etiquetas <= 63) y 308 caracteres.
        request.setEmailContacto("a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(4) + "com");

        assertThat(errores(request))
                .containsEntry("emailContacto", "El email de contacto no puede superar los 255 caracteres");
    }

    @Test
    void justoEnElLimiteDe255SeAcepta() {
        AgenciaRequest request = valido();
        request.setNombre("a".repeat(255));
        request.setLogo("a".repeat(255));
        request.setDireccion("a".repeat(255));
        request.setTelefonoContacto("1".repeat(255));

        assertThat(errores(request)).isEmpty();
    }
}
