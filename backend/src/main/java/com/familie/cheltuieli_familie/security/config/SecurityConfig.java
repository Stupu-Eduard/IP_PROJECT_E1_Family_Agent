package com.familie.cheltuieli_familie.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_CHILD = "CHILD";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. ACTIVARE CORS GLOBAL (Rezolvă eroarea roșie din consolă)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Dezactivăm CSRF deoarece folosim JWT (stateless), nu sesiuni bazate pe cookie-uri.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rute publice
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 2. THE PIPE - WebSockets & SSE (Rezolvă eroarea 403 Forbidden)
                        // Am adăugat rutele din pozele tale anterioare
                        .requestMatchers("/locatie/**").permitAll()
                        .requestMatchers("/api/v1/parent/stream/**").permitAll()
                        .requestMatchers("/api/ws/**").permitAll() // in caz ca ai un prefix general de ws
                        .requestMatchers("/api/v1/demo/**").permitAll() // <-- ADAUGAT PENTRU BUTOANELE DE TEST

                        // Swagger UI
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // Cheltuieli (folosite de frontend pentru harta/istoric)
                        .requestMatchers("/api/v1/expenses/**").permitAll()

                        // Lookups pentru filtre
                        .requestMatchers("/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/users/**").permitAll()

                        // Persistare coordonate geocodate in PostGIS
                        .requestMatchers("/api/v1/locations/**").permitAll()

                        // RBAC - Parintele are acces exclusiv la setarile sale si la alerte
                        .requestMatchers("/api/v1/parent/**").hasRole(ROLE_PARENT)
                        .requestMatchers("/api/v1/alerts/**").hasRole(ROLE_PARENT)

                        // Copilul si Parintele pot trimite date de locatie
                        .requestMatchers("/api/v1/child/location/sync").hasAnyRole(ROLE_PARENT, ROLE_CHILD)

                        // Orice alt request trebuie sa fie autentificat
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // Bean-ul care îi spune lui Spring Security să lase porturile de frontend să intre
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // REPARATIE SECURITY HOTSPOT: Inlocuim wildcard-ul "*" cu originile specifice frontend-ului
        // In productie, acestea ar trebui sa vina din fisierele de configurare (.yml)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", // Vite (Frontend implicit)
                "http://localhost:3000", // React standard
                "http://localhost:8080"  // Swagger / Altele
        ));

        // Permite metodele HTTP clasice si pe cele speciale pentru WebSockets
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Permite orice headere trimise de frontend
        configuration.setAllowedHeaders(List.of("*"));

        // Crucial pentru WebSockets si SSE ca sa isi mentina conexiunea deschisa
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica aceste reguli pe absolut toate rutele aplicației
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}