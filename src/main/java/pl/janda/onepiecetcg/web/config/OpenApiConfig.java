package pl.janda.onepiecetcg.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI onePieceTcgOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("One Piece TCG API")
                        .description("REST API for One Piece Trading Card Game application. " +
                                "Provides endpoints for card search, deck management, and shop directory.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("One Piece TCG Team")
                                .email("contact@onepiecetcg.pl")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:3000")
                                .description("Local Development Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")));
    }
}
