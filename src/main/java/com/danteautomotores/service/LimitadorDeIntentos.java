package com.danteautomotores.service;

import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Esqueleto de la fase RED: compila pero todavía no cuenta nada. */
@Component
public class LimitadorDeIntentos {

    @Autowired
    public LimitadorDeIntentos() {
    }

    public LimitadorDeIntentos(Ticker ticker) {
    }

    public boolean intentar(String clave, int maximo, Duration ventana) {
        return true;
    }

    public boolean bloqueado(String clave, int maximo, Duration ventana) {
        return false;
    }

    public void registrarFallo(String clave, Duration ventana) {
    }

    public void olvidar(String clave, Duration ventana) {
    }

    int tamanoTotal() {
        return 0;
    }
}
