package com.familie.cheltuieli_familie.security.config;

import com.familie.cheltuieli_familie.security.filter.CustomCsrfFilter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final com.familie.cheltuieli_familie.security.filter.SessionCookieFilter sessionCookieFilter;
    private final CustomCsrfFilter customCsrfFilter; // <-- Injectăm noul nostru filtru

    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_CHILD = "CHILD";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Rămâne disable() pentru că folosim filtrul nostru custom!
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Adăugăm filtrele în lanț
                .addFilterBefore(sessionCookieFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(customCsrfFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class) // <-- Filtrul nostru acționează aici

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/locatie/**").permitAll()
                        .requestMatchers("/api/v1/parent/stream/**").permitAll()
<<<<<<< Updated upstream
                        .requestMatchers("/api/ws/**").permitAll() // in caz ca ai un prefix general de ws
                        .requestMatchers("/api/v1/demo/**").permitAll() // <-- ADAUGAT PENTRU BUTOANELE DE TEST
                        .requestMatchers("/v1/chat/**").permitAll()      // <-- AI CHAT

                        // Swagger UI
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // Cheltuieli (folosite de frontend pentru harta/istoric)
=======
                        .requestMatchers("/api/ws/**").permitAll()
                        .requestMatchers("/api/v1/demo/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
>>>>>>> Stashed changes
                        .requestMatchers("/api/v1/expenses/**").permitAll()
                        .requestMatchers("/api/v1/categories/**", "/api/v1/users/**").permitAll()
                        .requestMatchers("/api/v1/locations/**").permitAll()
                        .requestMatchers("/api/v1/parent/**").hasRole(ROLE_PARENT)
                        .requestMatchers("/api/v1/alerts/**").hasRole(ROLE_PARENT)
                        .requestMatchers("/api/v1/child/location/sync").hasAnyRole(ROLE_PARENT, ROLE_CHILD)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", "http://localhost:3000",
                "https://family-agent.me", "https://api.family-agent.me", "http://localhost:4173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}