package br.com.belloinfo.saap_mvp.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuração geral do Springdoc OpenAPI (Swagger).
 * Configura metadados do projeto e o esquema global de autenticação Bearer JWT.
 * Habilitado apenas no profile "dev" para evitar exposição em produção.
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAAP - Sistema de Agendamento e Atendimento de Pacientes")
                        .version("1.2.0")
                        .description("Documentação interativa das APIs REST do projeto SAAP-MVP."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Insira o token JWT retornado pelo endpoint de login para acessar rotas protegidas.")));
    }
}
