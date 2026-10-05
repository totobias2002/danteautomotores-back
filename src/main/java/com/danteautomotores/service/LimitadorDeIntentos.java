package com.danteautomotores.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contadores de intentos de ventana fija, por clave, en memoria. Los usan el login, el registro, la recuperación
 * de contraseña, el reenvío de mails y el acceso con Google; cada llamador pasa su propio máximo y su ventana.
 * El limitador no lanza excepciones: quien lo llama decide cuándo lanzar {@code LimiteDeIntentosException}.
 *
 * <p>La ventana empieza en el primer intento de la clave y la clave se borra sola al vencer. Cada ventana tiene su
 * propio caché (hay pocas: 15 minutos, 1 hora y 24 horas), de modo que la misma clave en dos ventanas distintas
 * cuenta por separado. Cada caché tiene un tope de {@value #TOPE_DE_CLAVES} claves para que un atacante que
 * mande muchas IP o mails distintos no haga crecer la memoria sin límite.
 *
 * <p>Limitaciones aceptadas:
 * <ul>
 *   <li>El estado vive en la memoria de una sola instancia: un reinicio o un deploy lo resetea, y si el back se
 *       escala a varias instancias hay que mover los contadores a la base.</li>
 *   <li>La IP detrás del proxy de Railway es de mejor esfuerzo (se puede falsear o ser compartida), así que el
 *       límite por cuenta (mail) es la defensa real y el límite por IP solo suma una capa.</li>
 * </ul>
 */
@Component
public class LimitadorDeIntentos {

    static final int TOPE_DE_CLAVES = 10_000;

    private final Ticker ticker;
    private final Map<Duration, Cache<String, AtomicInteger>> cachesPorVentana = new ConcurrentHashMap<>();

    /** Con el reloj del sistema. */
    @Autowired
    public LimitadorDeIntentos() {
        this(Ticker.systemTicker());
    }

    /** Con un reloj propio, para que los tests avancen el tiempo sin dormir. */
    public LimitadorDeIntentos(Ticker ticker) {
        this.ticker = ticker;
    }

    /** Suma un intento a la clave; devuelve false si con este ya superó el máximo. */
    public boolean intentar(String clave, int maximo, Duration ventana) {
        return contador(clave, ventana).incrementAndGet() <= maximo;
    }

    /** Dice si la clave ya llegó al máximo, sin sumar un intento. */
    public boolean bloqueado(String clave, int maximo, Duration ventana) {
        AtomicInteger contador = cache(ventana).getIfPresent(clave);
        return contador != null && contador.get() >= maximo;
    }

    /** Suma un fallo a la clave (por ejemplo, una contraseña incorrecta). */
    public void registrarFallo(String clave, Duration ventana) {
        contador(clave, ventana).incrementAndGet();
    }

    /** Deja la clave en cero (por ejemplo, tras un login correcto). */
    public void olvidar(String clave, Duration ventana) {
        cache(ventana).invalidate(clave);
    }

    // Para los tests: claves vivas en todas las ventanas, después de aplicar los vencimientos y el tope.
    int tamanoTotal() {
        long total = 0;
        for (Cache<String, AtomicInteger> cache : cachesPorVentana.values()) {
            cache.cleanUp();
            total += cache.estimatedSize();
        }
        return (int) total;
    }

    private AtomicInteger contador(String clave, Duration ventana) {
        return cache(ventana).get(clave, k -> new AtomicInteger());
    }

    private Cache<String, AtomicInteger> cache(Duration ventana) {
        return cachesPorVentana.computeIfAbsent(ventana, v -> Caffeine.newBuilder()
                .expireAfterWrite(v)
                .maximumSize(TOPE_DE_CLAVES)
                .ticker(ticker)
                // Mantenimiento en el hilo que llama: sin hilos de limpieza y con tamaño determinista.
                .executor(Runnable::run)
                .build());
    }
}
