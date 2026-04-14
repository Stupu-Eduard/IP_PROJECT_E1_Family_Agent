package com.proiect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // aplică pe TOATE endpoint-urile
            .allowedOrigins(
                "https://family-agent-frontend.vercel.app",
                "https://api.family-agent.me",
                "http://localhost:4173" //pepntru vite preview
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            // allowCredentials(true) e necesar dacă folosiți cookies pentru auth
            .allowCredentials(true)
            // Browserul cachează răspunsul preflight 1 oră — reduce requesturile OPTIONS
            .maxAge(3600);
    }
}
