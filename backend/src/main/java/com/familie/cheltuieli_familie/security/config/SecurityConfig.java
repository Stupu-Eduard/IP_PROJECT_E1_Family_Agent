package com.familie.cheltuieli_familie.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Constante pentru roluri - evita duplicarea string-urilor (fix SonarCloud S1192)
    private static final String ROLE_PARENT = "PARENT";
    private static final String ROLE_CHILD = "CHILD";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rute publice
                        .requestMatchers("/api/v1/auth/**").permitAll()

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
}