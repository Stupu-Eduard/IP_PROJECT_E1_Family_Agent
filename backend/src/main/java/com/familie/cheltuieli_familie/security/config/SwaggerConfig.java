package com.familie.cheltuieli_familie.security.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Family Agent API")
                        .version("1.0")
                        .description("""
                                API pentru aplicatia Family Agent.
                                
                                ## Module disponibile:
                                - **Alerts** - Gestionarea alertelor de securitate pentru parinti
                                - **Child Location** - Sincronizarea locatiei copilului
                                - **Parent Stream** - Stream SSE in timp real catre parinte
                                - **Map** - Integrare Google Maps pentru geocodare
                                """)
                        .contact(new Contact()
                                .name("Nechita Alexandru"))
                )
                .addTagsItem(new Tag()
                        .name("Alerts")
                        .description("Endpoint-uri pentru alertele de securitate - accesibile doar de PARENT"))
                .addTagsItem(new Tag()
                        .name("Child Location")
                        .description("Endpoint-uri pentru sincronizarea locatiei - accesibile de CHILD si PARENT"))
                .addTagsItem(new Tag()
                        .name("Parent Stream")
                        .description("Stream SSE in timp real pentru dashboard-ul parintelui"))
                .addTagsItem(new Tag()
                        .name("Map")
                        .description("Geocodare adrese prin Google Maps API"));
    }
}