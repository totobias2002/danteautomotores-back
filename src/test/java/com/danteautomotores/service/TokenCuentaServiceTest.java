package com.danteautomotores.service;

import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.repository.TokenCuentaRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.support.PostgresLocalTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los tokens de cuenta contra un PostgreSQL real: se guardan hasheados, vencen según su tipo, se consumen una sola
 * vez (también con dos pedidos simultáneos) y emitir uno nuevo invalida el anterior del mismo tipo.
 */
@Import({TokenCuentaService.class, TokenCuentaServiceTest.RelojModificable.class})
class TokenCuentaServiceTest extends PostgresLocalTestBase {

    private static final Instant INICIO = Instant.parse("2026-10-01T12:00:00Z");

    /** Reloj que los tests pueden adelantar; un solo Instant compartido entre hilos. */
    static class Reloj extends Clock {
        private final AtomicReference<Instant> ahora = new AtomicReference<>(INICIO);

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora.get();
        }

        void fijar(Instant instante) {
            ahora.set(instante);
        }

        void avanzar(Duration duracion) {
            ahora.updateAndGet(i -> i.plus(duracion));
        }
    }

    @TestConfiguration
    static class RelojModificable {
        @Bean
        Reloj clock() {
            return new Reloj();
        }
    }

    @Autowired
    private TokenCuentaService service;
    @Autowired
    private TokenCuentaRepository tokenCuentaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Reloj reloj;

    @BeforeEach
    void relojAlInicio() {
        reloj.fijar(INICIO);
    }

    private Usuario nuevaCuenta() {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Ana")
                .email("ana-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .rol(Rol.COMPRADOR)
                .build());
    }

    @Test
    void emitirGuardaElHashYNoElToken() {
        Usuario cuenta = nuevaCuenta();

        String token = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        List<String> hashes = jdbc.queryForList("select token_hash from tokens_cuenta where usuario_id = ?",
                String.class, cuenta.getId());
        assertThat(hashes).hasSize(1);
        assertThat(hashes.get(0)).isNotEqualTo(token).hasSize(64).matches("[0-9a-f]{64}");
        // Ninguna columna de texto de la fila guarda el token.
        List<String> todosLosValores = jdbc.queryForList(
                "select concat_ws('|', id, usuario_id, tipo, token_hash, creado_en, expira_en, usado_en) "
                        + "from tokens_cuenta where usuario_id = ?", String.class, cuenta.getId());
        assertThat(todosLosValores.get(0)).doesNotContain(token);
    }

    @Test
    void elTokenTiene43CaracteresYDosEmisionesDanTokensDistintos() {
        Usuario cuenta = nuevaCuenta();
        Usuario otra = nuevaCuenta();

        String uno = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        String dos = service.emitir(otra.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        assertThat(uno).hasSize(43).matches("[A-Za-z0-9_-]{43}");
        assertThat(dos).hasSize(43);
        assertThat(uno).isNotEqualTo(dos);
    }

    @Test
    void consumirConElTokenCorrectoDevuelveLaCuentaYUnSegundoConsumoDevuelveVacio() {
        Usuario cuenta = nuevaCuenta();
        String token = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        assertThat(service.consumir(token, TipoTokenCuenta.CONFIRMAR_EMAIL)).contains(cuenta.getId());
        assertThat(service.consumir(token, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
    }

    @Test
    void laConfirmacionDeMailVenceALas24Horas() {
        Usuario cuenta = nuevaCuenta();
        String valido = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        reloj.avanzar(Duration.ofHours(23).plusMinutes(59));
        assertThat(service.consumir(valido, TipoTokenCuenta.CONFIRMAR_EMAIL)).contains(cuenta.getId());

        reloj.fijar(INICIO);
        String vencido = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        reloj.avanzar(Duration.ofHours(24).plusMinutes(1));
        assertThat(service.consumir(vencido, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
    }

    @Test
    void elRestablecimientoDeContrasenaVenceALaHora() {
        Usuario cuenta = nuevaCuenta();
        String valido = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);
        reloj.avanzar(Duration.ofMinutes(59));
        assertThat(service.consumir(valido, TipoTokenCuenta.RESTABLECER_CONTRASENA)).contains(cuenta.getId());

        reloj.fijar(INICIO);
        String vencido = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);
        reloj.avanzar(Duration.ofMinutes(61));
        assertThat(service.consumir(vencido, TipoTokenCuenta.RESTABLECER_CONTRASENA)).isEmpty();
    }

    @Test
    void unTokenDeUnTipoNoSirveParaElOtro() {
        Usuario cuenta = nuevaCuenta();
        String confirmacion = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        String restablecer = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);

        assertThat(service.consumir(confirmacion, TipoTokenCuenta.RESTABLECER_CONTRASENA)).isEmpty();
        assertThat(service.consumir(restablecer, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        // Un intento con el tipo equivocado no gasta el token: sigue sirviendo para su tipo.
        assertThat(service.consumir(confirmacion, TipoTokenCuenta.CONFIRMAR_EMAIL)).contains(cuenta.getId());
        assertThat(service.consumir(restablecer, TipoTokenCuenta.RESTABLECER_CONTRASENA)).contains(cuenta.getId());
    }

    @Test
    void emitirDeNuevoInvalidaElAnteriorDelMismoTipoPeroNoElDelOtro() {
        Usuario cuenta = nuevaCuenta();
        String primero = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        String delOtroTipo = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);
        String segundo = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        assertThat(service.consumir(primero, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir(segundo, TipoTokenCuenta.CONFIRMAR_EMAIL)).contains(cuenta.getId());
        assertThat(service.consumir(delOtroTipo, TipoTokenCuenta.RESTABLECER_CONTRASENA)).contains(cuenta.getId());
    }

    @Test
    void descartarPendientesBorraLosDosTipos() {
        Usuario cuenta = nuevaCuenta();
        Usuario otra = nuevaCuenta();
        String confirmacion = service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        String restablecer = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);
        String deLaOtra = service.emitir(otra.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        service.descartarPendientes(cuenta.getId());

        assertThat(service.consumir(confirmacion, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir(restablecer, TipoTokenCuenta.RESTABLECER_CONTRASENA)).isEmpty();
        assertThat(tokenCuentaRepository.findAll()).extracting("usuarioId").doesNotContain(cuenta.getId());
        assertThat(service.consumir(deLaOtra, TipoTokenCuenta.CONFIRMAR_EMAIL)).contains(otra.getId());
    }

    @Test
    void unTokenInventadoVacioONuloDevuelveVacio() {
        Usuario cuenta = nuevaCuenta();
        service.emitir(cuenta.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);

        assertThat(service.consumir("A".repeat(43), TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir("", TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir("   ", TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir(null, TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir("x".repeat(100_000), TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
        assertThat(service.consumir("con espacios y ' or 1=1 --", TipoTokenCuenta.CONFIRMAR_EMAIL)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosHilosQueConsumenElMismoTokenProducenExactamenteUnExito() throws Exception {
        // Sin transacción del test: cada hilo abre la suya, así que la base es la que decide quién gana.
        Usuario cuenta = nuevaCuenta();
        try {
            String token = service.emitir(cuenta.getId(), TipoTokenCuenta.RESTABLECER_CONTRASENA);

            int hilos = 8;
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            CountDownLatch largada = new CountDownLatch(1);
            try {
                List<Future<Optional<Long>>> resultados = new java.util.ArrayList<>();
                for (int i = 0; i < hilos; i++) {
                    Callable<Optional<Long>> tarea = () -> {
                        largada.await();
                        return service.consumir(token, TipoTokenCuenta.RESTABLECER_CONTRASENA);
                    };
                    resultados.add(pool.submit(tarea));
                }
                largada.countDown();

                int exitos = 0;
                for (Future<Optional<Long>> resultado : resultados) {
                    Optional<Long> valor = resultado.get(30, TimeUnit.SECONDS);
                    if (valor.isPresent()) {
                        assertThat(valor.get()).isEqualTo(cuenta.getId());
                        exitos++;
                    }
                }
                assertThat(exitos).isEqualTo(1);
            } finally {
                pool.shutdownNow();
            }
        } finally {
            // La cuenta de apoyo se borra al final; sus tokens caen por ON DELETE CASCADE.
            usuarioRepository.deleteById(cuenta.getId());
        }
    }
}
