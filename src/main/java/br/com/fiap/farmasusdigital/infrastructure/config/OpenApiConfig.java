package br.com.fiap.farmasusdigital.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FarmaSUS Digital API")
                        .description("API de reserva de medicamentos do SUS")
                        .version("v1"));
    }
}
