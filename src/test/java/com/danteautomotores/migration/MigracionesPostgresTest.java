package com.danteautomotores.migration;

import com.danteautomotores.support.PostgresLocalTestBase;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las migraciones contra un PostgreSQL real, sin Spring. Cada test trabaja sobre sus propias bases test_* y las borra.
 * Lo que se prueba es lo que no se puede arreglar después en producción: una base creada por Hibernate sin historial
 * de Flyway queda marcada como V1, recibe de V2 a V6 y no pierde ninguna fila.
 */
class MigracionesPostgresTest {

    private static final List<String> TABLAS = List.of(
            "usuarios", "agencias", "publicaciones", "fotos_publicacion", "consultas", "favoritos");

    private final List<String> basesCreadas = new ArrayList<>();

    @BeforeAll
    static void postgresDisponible() {
        PostgresLocalTestBase.exigirPostgres();
    }

    @AfterEach
    void borrarBases() {
        basesCreadas.forEach(PostgresLocalTestBase::borrarBase);
        basesCreadas.clear();
    }

    // ---- Helpers ----

    private String baseNueva() {
        String nombre = PostgresLocalTestBase.crearBaseDescartable();
        basesCreadas.add(nombre);
        return nombre;
    }

    private Flyway flyway(String base, boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(PostgresLocalTestBase.urlDe(base), PostgresLocalTestBase.usuario(), PostgresLocalTestBase.clave())
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion("1")
                .load();
    }

    private Connection conexion(String base) throws SQLException {
        return PostgresLocalTestBase.abrirConexion(base);
    }

    /** Una base como la de producción: V1 aplicado a mano (como lo hizo Hibernate), sin flyway_schema_history, con datos. */
    private String produccionSimulada() throws SQLException {
        String base = baseNueva();
        try (Connection c = conexion(base)) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/V1__esquema_original.sql"));
            insertarDatos(c);
        }
        return base;
    }

    /** Una base como la de desarrollo: igual que la de producción pero con las columnas que ddl-auto agregó en la Fase 1. */
    private String desarrolloSimulada() throws SQLException {
        String base = produccionSimulada();
        try (Connection c = conexion(base); Statement st = c.createStatement()) {
            st.execute("ALTER TABLE publicaciones ADD COLUMN destacado boolean NOT NULL DEFAULT false");
            st.execute("ALTER TABLE fotos_publicacion ADD COLUMN public_id varchar(255)");
        }
        return base;
    }

    private void insertarDatos(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES "
                    + "('Admin', 'admin@dante.test', 'hash', 'ADMIN'), ('Comprador', 'comprador@dante.test', 'hash', 'COMPRADOR')");
            st.execute("INSERT INTO agencias (nombre, slug) VALUES ('Agencia', 'agencia')");
            st.execute("INSERT INTO publicaciones (anio, marca, modelo, precio, estado, admin_id, agencia_id) VALUES "
                    + "(2020, 'Toyota', 'Corolla', 1000000, 'VENDIDO', 1, 1), (2022, 'Ford', 'Ranger', 2000000, 'DISPONIBLE', 1, 1)");
            st.execute("INSERT INTO fotos_publicacion (url, orden, publicacion_id) VALUES ('https://fotos.test/a.jpg', 0, 1)");
            st.execute("INSERT INTO consultas (email_comprador, nombre_comprador, publicacion_id) VALUES ('c@dante.test', 'Comprador', 2)");
            st.execute("INSERT INTO favoritos (publicacion_id, usuario_id) VALUES (2, 2)");
        }
    }

    private Map<String, Long> conteos(String base) throws SQLException {
        Map<String, Long> resultado = new LinkedHashMap<>();
        try (Connection c = conexion(base); Statement st = c.createStatement()) {
            for (String tabla : TABLAS) {
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM " + tabla)) {
                    rs.next();
                    resultado.put(tabla, rs.getLong(1));
                }
            }
        }
        return resultado;
    }

    /** Filas del historial como "version:tipo:exito", en el orden en que se registraron. */
    private List<String> historial(String base) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection c = conexion(base); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank")) {
            while (rs.next()) {
                filas.add(rs.getString("version") + ":" + rs.getString("type") + ":" + rs.getBoolean("success"));
            }
        }
        return filas;
    }

    private List<String> columnas(String base) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Connection c = conexion(base); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT table_name, column_name, data_type, character_maximum_length, "
                     + "numeric_precision, numeric_scale, is_nullable FROM information_schema.columns "
                     + "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history' "
                     + "ORDER BY table_name, column_name")) {
            while (rs.next()) {
                filas.add(String.join("|", rs.getString(1), rs.getString(2), rs.getString(3), String.valueOf(rs.getObject(4)),
                        String.valueOf(rs.getObject(5)), String.valueOf(rs.getObject(6)), rs.getString(7)));
            }
        }
        return filas;
    }

    private String valor(String base, String sql) throws SQLException {
        try (Connection c = conexion(base); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    // ---- Tests ----

    @Test
    void baseVaciaAplicaLasSeisMigraciones() throws SQLException {
        String base = baseNueva();

        MigrateResult resultado = flyway(base, true).migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(6);
        assertThat(historial(base)).containsExactly("1:SQL:true", "2:SQL:true", "3:SQL:true", "4:SQL:true", "5:SQL:true", "6:SQL:true");
    }

    @Test
    void produccionSimuladaQuedaEnBaselineV1RecibeDeV2AV6SinPerderFilas() throws SQLException {
        String base = produccionSimulada();
        Map<String, Long> antes = conteos(base);

        MigrateResult resultado = flyway(base, true).migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(5);
        assertThat(historial(base)).containsExactly("1:BASELINE:true", "2:SQL:true", "3:SQL:true", "4:SQL:true", "5:SQL:true", "6:SQL:true");
        assertThat(conteos(base)).isEqualTo(antes);
        assertThat(valor(base, "SELECT count(*) FROM publicaciones WHERE destacado = false")).isEqualTo("2");
        assertThat(valor(base, "SELECT fecha_vendido IS NOT NULL FROM publicaciones WHERE estado = 'VENDIDO'")).isEqualTo("t");
        assertThat(valor(base, "SELECT fecha_vendido IS NULL FROM publicaciones WHERE estado = 'DISPONIBLE'")).isEqualTo("t");
    }

    @Test
    void desarrolloConLasColumnasDeLaFase1HaceBaselineV2SinErrorYLasSiguientes() throws SQLException {
        String base = desarrolloSimulada();
        Map<String, Long> antes = conteos(base);

        MigrateResult resultado = flyway(base, true).migrate();

        assertThat(resultado.migrationsExecuted).isEqualTo(5);
        assertThat(historial(base)).containsExactly("1:BASELINE:true", "2:SQL:true", "3:SQL:true", "4:SQL:true", "5:SQL:true", "6:SQL:true");
        assertThat(conteos(base)).isEqualTo(antes);
    }

    @Test
    void v5SobreProduccionSimuladaNoTocaFilasYDejaLasRestriccionesDeIdentidad() throws SQLException {
        String base = produccionSimulada();
        Map<String, Long> antes = conteos(base);

        flyway(base, true).migrate();

        assertThat(conteos(base)).isEqualTo(antes);
        // D-09: las cuentas viejas quedan sin apellido ni DNI; la obligatoriedad vive en la regla de cuenta verificada.
        assertThat(valor(base, "SELECT count(*) FROM usuarios WHERE dni IS NULL AND apellido IS NULL")).isEqualTo("2");
        // D-10: los mails existentes no se dan por confirmados; el admin si.
        assertThat(valor(base, "SELECT email_confirmado FROM usuarios WHERE rol = 'ADMIN'")).isEqualTo("t");
        assertThat(valor(base, "SELECT email_confirmado FROM usuarios WHERE rol = 'COMPRADOR'")).isEqualTo("f");

        try (Connection c = conexion(base); Statement st = c.createStatement()) {
            // D-04: una cuenta por DNI.
            st.execute("UPDATE usuarios SET dni = '30123456' WHERE rol = 'ADMIN'");
            assertThatThrownBy(() -> st.execute("UPDATE usuarios SET dni = '30123456' WHERE rol = 'COMPRADOR'"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("uk_usuarios_dni");
            // D-05: 7 u 8 digitos sin cero inicial.
            assertThatThrownBy(() -> st.execute("UPDATE usuarios SET dni = '0123456' WHERE rol = 'COMPRADOR'"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("usuarios_dni_formato");
            // Mails que difieren solo en mayusculas no pueden coexistir.
            assertThatThrownBy(() -> st.execute("INSERT INTO usuarios (nombre, email, password_hash, rol) "
                    + "VALUES ('Otro', 'COMPRADOR@dante.test', 'hash', 'COMPRADOR')"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("uk_usuarios_email_lower");
            // Una cuenta solo-Google no tiene contrasena.
            st.execute("INSERT INTO usuarios (nombre, email, rol, google_sub) "
                    + "VALUES ('Sin clave', 'google@dante.test', 'COMPRADOR', 'sub-123')");
        }

        assertThat(valor(base, "SELECT count(*) FROM usuarios WHERE password_hash IS NULL")).isEqualTo("1");
        assertThat(valor(base, "SELECT to_regclass('public.tokens_cuenta') IS NOT NULL")).isEqualTo("t");
        assertThat(valor(base, "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_tokens_cuenta_usuario_tipo'")).isEqualTo("1");
    }

    @Test
    void v6CreaConversacionesYMensajesConSusRestricciones() throws SQLException {
        String base = produccionSimulada();
        Map<String, Long> antes = conteos(base);

        flyway(base, true).migrate();

        // Aditiva: ninguna fila existente cambia y las tablas nuevas nacen vacias.
        assertThat(conteos(base)).isEqualTo(antes);
        assertThat(valor(base, "SELECT count(*) FROM conversaciones")).isEqualTo("0");
        assertThat(valor(base, "SELECT count(*) FROM mensajes")).isEqualTo("0");

        String ahora = "'2026-10-07 12:00:00'";
        String compra = "INSERT INTO conversaciones (tipo, estado, usuario_id, publicacion_id, creada_en, ultimo_mensaje_en) "
                + "VALUES ('COMPRA', '%s', 2, %s, " + ahora + ", " + ahora + ")";

        try (Connection c = conexion(base); Statement st = c.createStatement()) {
            // D-02: una conversacion de COMPRA siempre tiene auto.
            assertThatThrownBy(() -> st.execute(compra.formatted("ABIERTA", "NULL")))
                    .isInstanceOf(SQLException.class).hasMessageContaining("conversaciones_compra_publicacion_check");
            // Una de COTIZACION puede no tener auto.
            st.execute("INSERT INTO conversaciones (tipo, estado, usuario_id, creada_en, ultimo_mensaje_en) "
                    + "VALUES ('COTIZACION', 'ABIERTA', 2, " + ahora + ", " + ahora + ")");
            // Tipo y estado validos.
            assertThatThrownBy(() -> st.execute("INSERT INTO conversaciones (tipo, estado, usuario_id, publicacion_id, creada_en, ultimo_mensaje_en) "
                    + "VALUES ('OTRO', 'ABIERTA', 2, 2, " + ahora + ", " + ahora + ")"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("conversaciones_tipo_check");
            assertThatThrownBy(() -> st.execute(compra.formatted("PAUSADA", "2")))
                    .isInstanceOf(SQLException.class).hasMessageContaining("conversaciones_estado_check");

            // D-03: una sola ABIERTA de compra por usuario y auto; una ABIERTA y una CERRADA conviven.
            st.execute(compra.formatted("ABIERTA", "2"));
            assertThatThrownBy(() -> st.execute(compra.formatted("ABIERTA", "2")))
                    .isInstanceOf(SQLException.class).hasMessageContaining("uk_conversaciones_compra_abierta");
            st.execute(compra.formatted("CERRADA", "2"));
            st.execute(compra.formatted("CERRADA", "2"));
            assertThat(valor(base, "SELECT count(*) FROM conversaciones WHERE tipo = 'COMPRA' AND publicacion_id = 2")).isEqualTo("3");

            // D-14: el texto de un mensaje tiene de 1 a 2000 caracteres.
            String idConversacion = valor(base, "SELECT min(id) FROM conversaciones WHERE tipo = 'COMPRA' AND estado = 'ABIERTA'");
            String mensaje = "INSERT INTO mensajes (conversacion_id, autor_id, autor_tipo, texto, creado_en) VALUES ("
                    + idConversacion + ", 2, 'USUARIO', %s, " + ahora + ")";
            assertThatThrownBy(() -> st.execute(mensaje.formatted("''")))
                    .isInstanceOf(SQLException.class).hasMessageContaining("mensajes_texto_largo_check");
            assertThatThrownBy(() -> st.execute(mensaje.formatted("repeat('a', 2001)")))
                    .isInstanceOf(SQLException.class).hasMessageContaining("mensajes_texto_largo_check");
            st.execute(mensaje.formatted("repeat('a', 2000)"));
            assertThatThrownBy(() -> st.execute("INSERT INTO mensajes (conversacion_id, autor_id, autor_tipo, texto, creado_en) VALUES ("
                    + idConversacion + ", 2, 'ROBOT', 'hola', " + ahora + ")"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("mensajes_autor_tipo_check");
        }
        assertThat(valor(base, "SELECT count(*) FROM mensajes")).isEqualTo("1");

        // D-16: borrar el auto borra en cascada sus conversaciones y mensajes (la cotizacion sin auto queda).
        // Las consultas y los favoritos del auto no tienen cascada en V1: se sacan primero.
        try (Connection c = conexion(base); Statement st = c.createStatement()) {
            st.execute("DELETE FROM consultas WHERE publicacion_id = 2");
            st.execute("DELETE FROM favoritos WHERE publicacion_id = 2");
            st.execute("DELETE FROM publicaciones WHERE id = 2");
        }
        assertThat(valor(base, "SELECT count(*) FROM conversaciones WHERE publicacion_id = 2")).isEqualTo("0");
        assertThat(valor(base, "SELECT count(*) FROM mensajes")).isEqualTo("0");
        assertThat(valor(base, "SELECT count(*) FROM conversaciones")).isEqualTo("1");
    }

    @Test
    void volverAMigrarEjecutaCeroMigracionesYNoCambiaFilas() throws SQLException {
        String vacia = baseNueva();
        String produccion = produccionSimulada();
        String desarrollo = desarrolloSimulada();
        for (String base : List.of(vacia, produccion, desarrollo)) {
            flyway(base, true).migrate();
            Map<String, Long> antes = conteos(base);
            List<String> historialAntes = historial(base);

            MigrateResult otraVez = flyway(base, true).migrate();

            assertThat(otraVez.migrationsExecuted).isZero();
            assertThat(conteos(base)).isEqualTo(antes);
            assertThat(historial(base)).isEqualTo(historialAntes);
        }
    }

    @Test
    void dosMigrateSimultaneosSobreLaMismaBaseVaciaAplicanCadaVersionUnaSolaVez() throws Exception {
        String base = baseNueva();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Flyway propio = flyway(base, true);
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return propio.migrate().migrationsExecuted;
                }));
            }
            largada.countDown();

            int total = 0;
            for (Future<Integer> futuro : futuros) {
                total += futuro.get(60, TimeUnit.SECONDS); // si un migrate lanzara, get() propaga la excepcion
            }

            assertThat(total).isEqualTo(6);
            assertThat(historial(base)).containsExactly("1:SQL:true", "2:SQL:true", "3:SQL:true", "4:SQL:true", "5:SQL:true", "6:SQL:true");
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void elEsquemaDeUnaBaseVaciaMigradaEsIgualAlDeUnaProduccionMigrada() throws SQLException {
        String vacia = baseNueva();
        String produccion = produccionSimulada();
        flyway(vacia, true).migrate();
        flyway(produccion, true).migrate();

        assertThat(columnas(produccion)).isNotEmpty().isEqualTo(columnas(vacia));
    }

    @Test
    void sinBaselineUnaBaseExistenteSinHistorialNoSeMigra() throws SQLException {
        String base = produccionSimulada();

        assertThatThrownBy(() -> flyway(base, false).migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
    }
}
