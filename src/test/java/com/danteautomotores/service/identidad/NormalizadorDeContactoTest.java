package com.danteautomotores.service.identidad;

import com.danteautomotores.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.danteautomotores.service.identidad.NormalizadorDeContacto.MENSAJE_CELULAR_INVALIDO;
import static com.danteautomotores.service.identidad.NormalizadorDeContacto.MENSAJE_DNI_INVALIDO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tabla de casos de teléfono (D-03) y DNI (D-05): puras, sin Spring y sin mocks. */
class NormalizadorDeContactoTest {

    @ParameterizedTest(name = "celular [{0}] se guarda como {1}")
    @CsvSource(delimiter = '|', value = {
            "011 15 1234-5678|+5491112345678",
            "+54 9 11 1234-5678|+5491112345678",
            "+5491112345678|+5491112345678",
            "11 1234-5678|+5491112345678",
            "11-15-1234-5678|+5491112345678",
            "0351 15 6123456|+5493516123456",
            "351 15 6123456|+5493516123456",
            "02954 15 412345|+5492954412345",
            "+54 9 223 512 3456|+5492235123456"
    })
    void normalizaCelularesArgentinos(String entrada, String esperado) {
        assertThat(NormalizadorDeContacto.normalizarCelular(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "celular [{0}] se rechaza")
    @NullAndEmptySource
    @ValueSource(strings = {"+34 612 345 678", "15 1234 5678", "+54 9 11 1234-567", "abc", "   "})
    void rechazaCelularesInvalidos(String entrada) {
        assertThatThrownBy(() -> NormalizadorDeContacto.normalizarCelular(entrada))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_CELULAR_INVALIDO);
    }

    @Test
    void elMensajeDeCelularEsElDeLaTabla() {
        assertThat(MENSAJE_CELULAR_INVALIDO)
                .isEqualTo("Ingresá un celular argentino válido, con código de área. Ej: 11 2345-6789");
    }

    @Test
    void esCelularValidoAceptaUnValorYaNormalizado() {
        assertThat(NormalizadorDeContacto.esCelularValido("+5491112345678")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "   ", "+34 612 345 678"})
    void esCelularValidoDevuelveFalseSinLanzar(String entrada) {
        assertThat(NormalizadorDeContacto.esCelularValido(entrada)).isFalse();
    }

    @ParameterizedTest(name = "DNI [{0}] se guarda como {1}")
    @CsvSource(delimiter = '|', value = {
            "30.123.456|30123456",
            "12 345 678|12345678",
            "12-345-678|12345678",
            "1234567|1234567"
    })
    void normalizaDni(String entrada, String esperado) {
        assertThat(NormalizadorDeContacto.normalizarDni(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "DNI [{0}] se rechaza")
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "123456789", "0123456", "abc", "   "})
    void rechazaDniInvalidos(String entrada) {
        assertThatThrownBy(() -> NormalizadorDeContacto.normalizarDni(entrada))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage(MENSAJE_DNI_INVALIDO);
    }

    @Test
    void elMensajeDeDniEsElDeLaTabla() {
        assertThat(MENSAJE_DNI_INVALIDO).isEqualTo("El DNI debe tener 7 u 8 dígitos, sin puntos.");
    }
}
