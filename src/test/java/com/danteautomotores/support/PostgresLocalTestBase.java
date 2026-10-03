package com.danteautomotores.support;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Base de los tests que necesitan un PostgreSQL real (el del docker-compose, localhost:5433). Cada corrida trabaja
 * sobre bases descartables {@code test_xxxxxxxx} que se crean y se borran solas: NUNCA toca la base de desarrollo.
 *
 * <p>Sin Postgres, los tests se saltean (Assumptions) salvo que se pase {@code -Ddante.pg.required=true}, que los
 * hace fallar. Conexión sobreescribible con {@code dante.pg.host|port|user|password}.
 *
 * <p>Los helpers estáticos los usa también MigracionesPostgresTest, que no levanta Spring.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PostgresLocalTestBase {

    private static final String PREFIJO_BASE = "test_";

    // Una sola base por JVM para todas las clases que extienden esta base: Spring cachea el contexto entre clases
    // (mismo conjunto de propiedades), así que una base por clase dejaría a la segunda apuntando a una base ya borrada.
    private static String baseCompartida;

    @BeforeAll
    static void postgresDeLaClase() {
        exigirPostgres();
    }

    @DynamicPropertySource
    static void propiedadesDeLaBase(DynamicPropertyRegistry registry) {
        String base = baseCompartida();
        registry.add("spring.datasource.url", () -> urlDe(base));
        registry.add("spring.datasource.username", PostgresLocalTestBase::usuario);
        registry.add("spring.datasource.password", PostgresLocalTestBase::clave);
    }

    private static synchronized String baseCompartida() {
        if (baseCompartida == null) {
            baseCompartida = crearBaseDescartable();
            String paraBorrar = baseCompartida;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> borrarBase(paraBorrar)));
        }
        return baseCompartida;
    }

    // ---- Helpers estáticos (sin Spring) ----

    public static String host() {
        return System.getProperty("dante.pg.host", "localhost");
    }

    public static String puerto() {
        return System.getProperty("dante.pg.port", "5433");
    }

    public static String usuario() {
        return System.getProperty("dante.pg.user", "dante");
    }

    public static String clave() {
        return System.getProperty("dante.pg.password", "dante_dev_password");
    }

    public static String urlDe(String base) {
        return "jdbc:postgresql://" + host() + ":" + puerto() + "/" + base;
    }

    public static boolean postgresDisponible() {
        try (Connection ignorada = abrirConexion("postgres")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Saltea la clase si no hay Postgres; con -Ddante.pg.required=true falla con instrucciones. */
    public static void exigirPostgres() {
        if (postgresDisponible()) {
            return;
        }
        String mensaje = "No hay PostgreSQL en " + host() + ":" + puerto() + ". Levantalo con: "
                + "docker compose -f docker-compose.yml up -d";
        if (Boolean.getBoolean("dante.pg.required")) {
            throw new AssertionError(mensaje + " (dante.pg.required=true)");
        }
        Assumptions.assumeTrue(false, mensaje);
    }

    /** Crea una base vacía {@code test_} + 8 hex en minúscula y devuelve su nombre. */
    public static String crearBaseDescartable() {
        String nombre = PREFIJO_BASE + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ejecutarEnPostgres("CREATE DATABASE " + nombre);
        return nombre;
    }

    /** Borra una base descartable; se niega a borrar cualquier nombre que no empiece con {@code test_}. */
    public static void borrarBase(String nombre) {
        if (nombre == null || !nombre.matches(PREFIJO_BASE + "[a-z0-9_]+")) {
            throw new IllegalArgumentException("Solo se pueden borrar bases test_*: " + nombre);
        }
        try {
            ejecutarEnPostgres("DROP DATABASE IF EXISTS " + nombre + " WITH (FORCE)");
        } catch (RuntimeException e) {
            // Limpieza de mejor esfuerzo: una base test_ que quede huérfana no afecta a las demás corridas.
        }
    }

    public static Connection abrirConexion(String base) throws SQLException {
        return DriverManager.getConnection(urlDe(base), usuario(), clave());
    }

    private static void ejecutarEnPostgres(String sql) {
        try (Connection conexion = abrirConexion("postgres"); Statement st = conexion.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo ejecutar en Postgres: " + sql, e);
        }
    }
}
