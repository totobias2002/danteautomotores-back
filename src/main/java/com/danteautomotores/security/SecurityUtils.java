package com.danteautomotores.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static String obtenerEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
