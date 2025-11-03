package com.chat.e2e.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("chat-e2e API")
                        .version("1.0.0")
                        .description("""
                                Backend API for the chat-e2e project — an end-to-end encrypted chat 
                                built with Spring Boot, PostgreSQL and React.
                                """)
                        .contact(new Contact()
                                .name("chat-e2e Team")
                                .email("dev@chat-e2e.local")
                                .url("https://github.com/yourname/chat-e2e"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                );
    }
}
