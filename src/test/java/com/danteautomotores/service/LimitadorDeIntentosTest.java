package com.danteautomotores.service;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LimitadorDeIntentosTest {

    private static final Duration QUINCE_MINUTOS = Duration.ofMinutes(15);
    private static final Duration UNA_HORA = Duration.ofHours(1);

    /** Reloj falso y mutable: los tests avanzan el tiempo sin dormir. */
    private static class RelojFalso implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void avanzar(Duration duracion) {
            nanos.addAndGet(duracion.toNanos());
        }
    }

    private RelojFalso reloj;
    private LimitadorDeIntentos limitador;

    @BeforeEach
    void preparar() {
        reloj = new RelojFalso();
        limitador = new LimitadorDeIntentos(reloj);
    }

    @Test
    void permiteHastaElMaximoYBloqueaDesdeElSexto() {
        for (int i = 1; i <= 5; i++) {
            assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).as("intento %d", i).isTrue();
        }

        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();
        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();
    }

    @Test
    void dosClavesDistintasNoComparteContador() {
        for (int i = 0; i < 5; i++) {
            limitador.intentar("login:ana", 5, QUINCE_MINUTOS);
        }

        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();
        assertThat(limitador.intentar("login:beto", 5, QUINCE_MINUTOS)).isTrue();
    }

    @Test
    void pasadaLaVentanaLaClaveEmpiezaDeCero() {
        for (int i = 0; i < 6; i++) {
            limitador.intentar("login:ana", 5, QUINCE_MINUTOS);
        }
        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();

        reloj.avanzar(QUINCE_MINUTOS.plusSeconds(1));

        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isTrue();
    }

    @Test
    void antesDeQueVenzaLaVentanaSigueBloqueada() {
        for (int i = 0; i < 6; i++) {
            limitador.intentar("login:ana", 5, QUINCE_MINUTOS);
        }

        reloj.avanzar(QUINCE_MINUTOS.minusSeconds(1));

        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();
    }

    @Test
    void bloqueadoEsFalsoHastaAlcanzarElMaximoYVerdaderoDespues() {
        for (int i = 0; i < 4; i++) {
            limitador.registrarFallo("login:ana", QUINCE_MINUTOS);
        }
        assertThat(limitador.bloqueado("login:ana", 5, QUINCE_MINUTOS)).isFalse();

        limitador.registrarFallo("login:ana", QUINCE_MINUTOS);

        assertThat(limitador.bloqueado("login:ana", 5, QUINCE_MINUTOS)).isTrue();
    }

    @Test
    void consultarBloqueadoNoSumaIntentos() {
        for (int i = 0; i < 4; i++) {
            limitador.registrarFallo("login:ana", QUINCE_MINUTOS);
        }

        for (int i = 0; i < 20; i++) {
            assertThat(limitador.bloqueado("login:ana", 5, QUINCE_MINUTOS)).isFalse();
        }

        // Tras 20 consultas sigue habiendo lugar para un quinto intento y recién el sexto se rechaza.
        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isTrue();
        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isFalse();
    }

    @Test
    void bloqueadoDeUnaClaveSinHistorialEsFalso() {
        assertThat(limitador.bloqueado("login:nueva", 5, QUINCE_MINUTOS)).isFalse();
    }

    @Test
    void olvidarDejaLaClaveEnCeroAntesDeLaVentana() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("login:ana", QUINCE_MINUTOS);
        }
        assertThat(limitador.bloqueado("login:ana", 5, QUINCE_MINUTOS)).isTrue();

        limitador.olvidar("login:ana", QUINCE_MINUTOS);

        assertThat(limitador.bloqueado("login:ana", 5, QUINCE_MINUTOS)).isFalse();
        assertThat(limitador.intentar("login:ana", 5, QUINCE_MINUTOS)).isTrue();
    }

    @Test
    void dosVentanasDistintasConLaMismaClaveSonContadoresIndependientes() {
        for (int i = 0; i < 3; i++) {
            limitador.registrarFallo("mail:ana", UNA_HORA);
        }

        assertThat(limitador.bloqueado("mail:ana", 3, UNA_HORA)).isTrue();
        assertThat(limitador.bloqueado("mail:ana", 3, QUINCE_MINUTOS)).isFalse();

        limitador.olvidar("mail:ana", QUINCE_MINUTOS);

        assertThat(limitador.bloqueado("mail:ana", 3, UNA_HORA)).isTrue();
    }

    @Test
    void cadaVentanaVenceConSuPropioPlazo() {
        for (int i = 0; i < 3; i++) {
            limitador.registrarFallo("mail:ana", QUINCE_MINUTOS);
            limitador.registrarFallo("mail:ana", UNA_HORA);
        }

        reloj.avanzar(Duration.ofMinutes(20));

        assertThat(limitador.bloqueado("mail:ana", 3, QUINCE_MINUTOS)).isFalse();
        assertThat(limitador.bloqueado("mail:ana", 3, UNA_HORA)).isTrue();
    }

    @Test
    void noLanzaExcepcionesNiConClavesNuevasNiAlOlvidarUnaClaveInexistente() {
        assertThatCode(() -> {
            limitador.olvidar("no-existe", QUINCE_MINUTOS);
            limitador.bloqueado("no-existe", 1, QUINCE_MINUTOS);
            limitador.registrarFallo("no-existe", QUINCE_MINUTOS);
            limitador.intentar("no-existe", 1, QUINCE_MINUTOS);
        }).doesNotThrowAnyException();
    }

    @Test
    void masDeDiezMilClavesDistintasNoHacenCrecerLaMemoriaSinLimite() {
        for (int i = 0; i < 12_000; i++) {
            limitador.intentar("ip:" + i, 5, QUINCE_MINUTOS);
        }

        assertThat(limitador.tamanoTotal()).isLessThanOrEqualTo(10_000);
    }

    @Test
    void conElRelojRealUnaClaveNuevaTieneLugar() {
        LimitadorDeIntentos conRelojReal = new LimitadorDeIntentos();

        assertThat(conRelojReal.intentar("login:ana", 1, QUINCE_MINUTOS)).isTrue();
        assertThat(conRelojReal.intentar("login:ana", 1, QUINCE_MINUTOS)).isFalse();
    }
}
