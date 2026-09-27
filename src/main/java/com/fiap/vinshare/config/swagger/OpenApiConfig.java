package com.fiap.vinshare.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ford VIN Share API")
                        .version("0.0.1")
                        .description("""
                                API REST do Ford VIN Share (Challenge FIAP 2026).

                                **Autenticação:** faça `POST /auth/login`, copie o `accessToken` e clique em **Authorize**.
                                O access token dura 15 minutos; renove com `POST /auth/refresh` (o refresh token é
                                rotacionado a cada uso e o reuso de um token antigo revoga a sessão inteira).

                                **Perfis:** `CLIENT` (app do cliente), `ANALYST` (concessionária; vê apenas clientes da
                                própria concessionária de relacionamento) e `ADMIN` (operador Ford; visão da rede e
                                catálogo de prêmios).

                                **Erros:** sempre `application/problem+json` (RFC 7807).
                                """)
                        .contact(new Contact().name("Equipe VIN Share").email("equipe@fiap.com.br"))
                        .license(new License().name("Uso acadêmico")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
