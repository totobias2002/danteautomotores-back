package com.danteautomotores.dto;

import com.danteautomotores.dto.publicacion.PublicacionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PublicacionRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static PublicacionRequest valido() {
        PublicacionRequest request = new PublicacionRequest();
        request.setAgenciaId(1L);
        request.setMarca("Toyota");
        request.setModelo("Corolla");
        request.setAnio(2020);
        request.setPrecio(new BigDecimal("15000.50"));
        request.setMoneda("USD");
        request.setKilometraje(0);
        request.setDescripcion("Excelente estado, único dueño.");
        return request;
    }

    private Map<String, String> errores(PublicacionRequest request) {
        Set<ConstraintViolation<PublicacionRequest>> violaciones = validator.validate(request);
        return violaciones.stream().collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
    }

    @Test
    void unRequestCompletoEsValido() {
        assertThat(errores(valido())).isEmpty();
    }

    @Test
    void sinMonedaNiDescripcionNiKilometrajeEsValido() {
        PublicacionRequest request = valido();
        request.setMoneda(null);
        request.setDescripcion(null);
        request.setKilometraje(null);

        assertThat(errores(request)).isEmpty();
    }

    @Test
    void anioFueraDeRangoSeRechazaConMensajeEnEspanol() {
        for (int anio : new int[]{0, -5, 1899, Year.now().getValue() + 2, 99999}) {
            PublicacionRequest request = valido();
            request.setAnio(anio);
            assertThat(errores(request)).containsKey("anio");
            assertThat(errores(request).get("anio")).contains("año");
        }
    }

    @Test
    void anioDelProximoAnioSeAcepta() {
        PublicacionRequest request = valido();
        request.setAnio(Year.now().getValue() + 1);

        assertThat(errores(request)).isEmpty();
    }

    @Test
    void kilometrajeNegativoSeRechaza() {
        PublicacionRequest request = valido();
        request.setKilometraje(-1);

        assertThat(errores(request)).containsEntry("kilometraje", "El kilometraje no puede ser negativo");
    }

    @Test
    void monedaDistintaDeArsOUsdSeRechaza() {
        PublicacionRequest request = valido();
        request.setMoneda("EUR");

        assertThat(errores(request)).containsEntry("moneda", "La moneda tiene que ser ARS o USD");
    }

    @Test
    void textosMasLargosQueLaColumnaSeRechazan() {
        PublicacionRequest request = valido();
        String largo = "a".repeat(256);
        request.setMarca(largo);
        request.setModelo(largo);
        request.setColor(largo);

        assertThat(errores(request)).containsKeys("marca", "modelo", "color");
    }

    @Test
    void precioConMasDigitosQueLaColumnaSeRechaza() {
        PublicacionRequest request = valido();
        request.setPrecio(new BigDecimal("12345678901"));
        assertThat(errores(request)).containsKey("precio");

        request.setPrecio(new BigDecimal("100.123"));
        assertThat(errores(request)).containsKey("precio");

        request.setPrecio(new BigDecimal("9999999999.99"));
        assertThat(errores(request)).isEmpty();
    }

    @Test
    void sinPrecioAnteriorNiTipoEsValido() {
        PublicacionRequest request = valido();
        request.setPrecioAnterior(null);
        request.setTipoCarroceria(null);

        assertThat(errores(request)).isEmpty();
    }

    @Test
    void precioAnteriorCeroONegativoSeRechazaSenalandoElCampo() {
        for (String valor : new String[]{"0", "-1", "-0.01"}) {
            PublicacionRequest request = valido();
            request.setPrecioAnterior(new BigDecimal(valor));
            assertThat(errores(request)).containsKey("precioAnterior");
        }
    }

    @Test
    void precioAnteriorConMasDigitosQueLaColumnaSeRechaza() {
        PublicacionRequest request = valido();
        request.setPrecioAnterior(new BigDecimal("12345678901"));
        assertThat(errores(request)).containsKey("precioAnterior");

        request.setPrecioAnterior(new BigDecimal("100.123"));
        assertThat(errores(request)).containsKey("precioAnterior");

        request.setPrecioAnterior(new BigDecimal("9999999999.99"));
        assertThat(errores(request)).isEmpty();
    }

    @Test
    void precioAnteriorMenorOIgualAlPrecioSeAceptaPeroNoEsOferta() {
        PublicacionRequest request = valido();
        request.setPrecioAnterior(new BigDecimal("100"));
        assertThat(errores(request)).isEmpty();

        request.setPrecioAnterior(request.getPrecio());
        assertThat(errores(request)).isEmpty();
    }

    @Test
    void descripcionCortaOExcesivaSeRechaza() {
        PublicacionRequest request = valido();
        request.setDescripcion("corta");
        assertThat(errores(request)).containsKey("descripcion");

        request.setDescripcion("x".repeat(5001));
        assertThat(errores(request)).containsKey("descripcion");
    }
}
