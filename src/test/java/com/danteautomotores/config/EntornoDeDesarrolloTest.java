package com.danteautomotores.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EntornoDeDesarrolloTest {

    private static MockEnvironment conActivos(String... perfiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(perfiles);
        return environment;
    }

    @Test
    void sinPerfilesActivosNiPorDefecto_esDesarrollo() {
        // MockEnvironment sin configurar: getDefaultProfiles() es ["default"], el implícito de Spring.
        assertThat(EntornoDeDesarrollo.esDesarrollo(new MockEnvironment())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "local", "test"})
    void perfilesDeDesarrollo_sonDesarrollo(String perfil) {
        assertThat(EntornoDeDesarrollo.esDesarrollo(conActivos(perfil))).isTrue();
    }

    @Test
    void variosPerfilesDeDesarrollo_sonDesarrollo() {
        assertThat(EntornoDeDesarrollo.esDesarrollo(conActivos("dev", "test"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "production", "railway", "staging", "qa"})
    void perfilesQueNoSonDeDesarrollo_noSonDesarrollo(String perfil) {
        assertThat(EntornoDeDesarrollo.esDesarrollo(conActivos(perfil))).isFalse();
    }

    @Test
    void mezclaConProd_noEsDesarrollo() {
        assertThat(EntornoDeDesarrollo.esDesarrollo(conActivos("prod", "dev"))).isFalse();
    }

    @Test
    void sinActivosYPerfilPorDefectoProd_noEsDesarrollo() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("prod");
        assertThat(EntornoDeDesarrollo.esDesarrollo(environment)).isFalse();
    }

    @Test
    void sinActivosYPerfilPorDefectoDev_esDesarrollo() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("dev");
        assertThat(EntornoDeDesarrollo.esDesarrollo(environment)).isTrue();
    }

    @Test
    void perfilActivoDevConPorDefectoProd_esDesarrollo() {
        // Los perfiles activos mandan sobre los por defecto, como en Spring.
        MockEnvironment environment = conActivos("dev");
        environment.setDefaultProfiles("prod");
        assertThat(EntornoDeDesarrollo.esDesarrollo(environment)).isTrue();
    }
}
