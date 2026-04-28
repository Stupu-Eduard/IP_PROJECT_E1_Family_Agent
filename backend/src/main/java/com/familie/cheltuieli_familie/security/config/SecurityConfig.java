package com.familie.cheltuieli_familie.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rute publice
                        .requestMatchers("/api/v1/auth/**").permitAll()

                    // Cheltuieli (folosite de frontend pentru harta/istoric)
                    .requestMatchers("/api/v1/expenses/**").permitAll()

                        // Lookups pentru filtre (categoriile/persoanele existente in DB)
                        .requestMatchers("/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/users/**").permitAll()

                        // Persistare coordonate geocodate in PostGIS
                        .requestMatchers("/api/v1/locations/**").permitAll()

                        // RBAC - Parintele are acces exclusiv la setarile sale si la alerte
                        .requestMatchers("/api/v1/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/v1/alerts/**").hasRole("PARENT")

                        // Copilul si Parintele pot trimite date de locatie
                        .requestMatchers("/api/v1/child/location/sync").hasAnyRole("PARENT", "CHILD")
                        .requestMatchers("/telemetry/**").permitAll()

                        // Orice alt request trebuie sa fie autentificat
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
