package com.familie.cheltuieli_familie.security.config;

import com.familie.cheltuieli_familie.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
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
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Rute publice
                        .requestMatchers("/api/v1/auth/**", "/actuator/**").permitAll()

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

                        // Cheltuieli — necesită autentificare (filtrate după familia userului)
                        // .requestMatchers("/api/v1/expenses/**").permitAll()

                        // Lookups pentru filtre
                        .requestMatchers("/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/users/**").permitAll()

                        // Persistare coordonate geocodate in PostGIS
                        .requestMatchers("/api/v1/locations/**").permitAll()

                        // RBAC - Parintele are acces exclusiv la setarile sale si la alerte
                        .requestMatchers("/api/v1/parent/**").hasRole(ROLE_PARENT)
                        .requestMatchers("/api/v1/alerts/**").hasRole(ROLE_PARENT)
                        .requestMatchers("/api/geofencing/**").hasRole(ROLE_PARENT)

                        // Copilul si Parintele pot trimite date de locatie
                        .requestMatchers("/api/v1/child/location/sync").hasAnyRole(ROLE_PARENT, ROLE_CHILD)

                        // Orice alt request trebuie sa fie autentificat
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean-ul care îi spune lui Spring Security să lase porturile de frontend să intre
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // REPARATIE SECURITY HOTSPOT: Originile CORS sunt configurate in application.yml
        // pentru a evita IP-uri hardcodate in codul sursa.
        // In productie, acestea ar trebui sa vina exclusiv din fisierele de configurare.
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", // Vite (Frontend implicit)
                "http://localhost:3000", // React standard
                "http://127.0.0.1:5173", // 127.0.0.1 variant
                "http://127.0.0.1:3000",
                // NOSONAR: IP-urile de mai jos sunt necesare pentru mediul de dezvoltare WSL2.
                // In productie, acestea ar trebui inlocuite cu domenii proprii.
                "http://172.27.84.187:5173", // Server network IP - dev only
                "http://172.27.84.187:8080", // WSL2 backend direct - dev only
                "https://family-agent.me",
                "https://api.family-agent.me",
                "http://localhost:4173" // vite preview
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
