package com.danteautomotores.config;

import com.danteautomotores.security.JwtAuthenticationFilter;
import com.danteautomotores.security.RestAccessDeniedHandler;
import com.danteautomotores.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Consultar y cotizar exigen sesión de comprador; que la cuenta esté verificada lo decide
                        // cada service con el estado actual de la base (VerificacionCuenta.exigir).
                        .requestMatchers(HttpMethod.POST, "/api/consultas/**").hasRole("COMPRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/solicitudes-venta/**").hasRole("COMPRADOR")
                        // Las conversaciones del lado del comprador: sin token 401 y el admin 403. La bandeja del admin
                        // vive en /api/admin/** (D-17).
                        .requestMatchers("/api/conversaciones/**").hasRole("COMPRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**", "/api/agencias/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/agencias/**", "/api/publicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/agencias/**", "/api/publicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/agencias/**", "/api/publicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/agencias/**", "/api/publicaciones/**").hasRole("ADMIN")
                        .requestMatchers("/api/consultas/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Railway activa un deploy recién cuando el healthcheck responde 2xx. Es lo único de actuator
                        // que se abre; el resto sigue cerrado y la exposición está limitada a health (application.yml).
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/solicitudes-venta/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Se recortan los espacios: "https://a.com, https://b.com" es una forma natural de escribir la variable.
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
