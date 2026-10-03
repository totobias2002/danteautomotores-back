package com.danteautomotores.config;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Set;

/**
 * Único criterio del backend para decidir si corre en "modo desarrollo". Lo comparten {@link SecretosGuard} y
 * {@link DataSeeder}: en desarrollo faltar un secreto o el admin solo avisa en el log; en cualquier otro caso el
 * arranque se aborta.
 * <p>
 * Con perfiles activos, es desarrollo solo si todos son {@code dev}, {@code local} o {@code test} (una mezcla como
 * {@code prod,dev} no lo es). Sin perfiles activos se miran los perfiles por defecto: es desarrollo solo si cada uno
 * es de desarrollo o el {@code default} implícito de Spring. Se miran porque un {@code SPRING_PROFILES_DEFAULT=prod}
 * sin perfil activo es producción: antes {@code DataSeeder} lo trataba así y {@code SecretosGuard} no.
 */
public final class EntornoDeDesarrollo {

    static final Set<String> PERFILES_DE_DESARROLLO = Set.of("dev", "local", "test");

    // Nombre del perfil por defecto de Spring; la constante de AbstractEnvironment es protected.
    private static final String PERFIL_POR_DEFECTO_DE_SPRING = "default";

    private EntornoDeDesarrollo() {
    }

    public static boolean esDesarrollo(Environment environment) {
        String[] activos = environment.getActiveProfiles();
        if (activos.length > 0) {
            return Arrays.stream(activos).allMatch(PERFILES_DE_DESARROLLO::contains);
        }
        return Arrays.stream(environment.getDefaultProfiles())
                .allMatch(perfil -> PERFIL_POR_DEFECTO_DE_SPRING.equals(perfil) || PERFILES_DE_DESARROLLO.contains(perfil));
    }
}
