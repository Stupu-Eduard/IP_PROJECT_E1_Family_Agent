package com.familie.cheltuieli_familie.security.config;

import com.familie.cheltuieli_familie.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("#{'${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,https://family-agent.me,https://www.family-agent.me}'.split(',')}")
    private List<String> allowedOrigins;

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

        // Originile sunt configurate in application.yml pentru a evita IP-uri hardcodate.
        configuration.setAllowedOrigins(allowedOrigins);

        // Permite metodele HTTP clasice si pe cele speciale pentru WebSockets
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));


        // Permite headerele necesare
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        
        // Expunem headerul Authorization pentru ca frontendul sa poata citi tokenul daca e cazul
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Link", "X-Total-Count"));

        // Crucial pentru WebSockets si SSE ca sa isi mentina conexiunea deschisa
        configuration.setAllowCredentials(true);
        
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica aceste reguli pe absolut toate rutele aplicației
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}