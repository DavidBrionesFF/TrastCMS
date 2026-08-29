package com.nattechnologies.trastcms.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI trastCmsOpenApi(TrastCmsProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("TrastCMS REST API")
                        .version(properties.getMcp().getServerVersion())
                        .description("API REST del CMS para contenido, medios, temas, usuarios, plugins, TrastCRM, TrastPay, TrastStore, TrastSaaS, TrastTravel, TrastTranslate, TrastAcademy y Mi Cuenta. Los endpoints administrativos utilizan sesión y CSRF.")
                        .contact(new Contact()
                                .name("NaT Technologies")
                                .url("https://github.com/DavidBrionesFF/TrastCMS"))
                        .license(new License()
                                .name("GPL-3.0")
                                .url("https://www.gnu.org/licenses/gpl-3.0.html")))
                // Relative URLs keep Swagger/OpenAPI bound to the host that
                // resolved the current tenant instead of advertising Tour
                // The primary site from Academy or Store + SaaS.
                .servers(List.of(new Server()
                        .url("/")
                        .description("Sitio TrastCMS actual")))
                .externalDocs(new ExternalDocumentation()
                        .description("Portal para desarrolladores")
                        .url("/developers"));
    }
    @Bean
    GroupedOpenApi commerceApi() {
        return GroupedOpenApi.builder().group("commerce").pathsToMatch("/api/public/commerce/**", "/api/admin/commerce/**").build();
    }

    @Bean
    GroupedOpenApi storeApi() {
        return GroupedOpenApi.builder().group("store").pathsToMatch("/api/public/store/**", "/api/admin/store/**").build();
    }

    @Bean
    GroupedOpenApi saasApi() {
        return GroupedOpenApi.builder().group("saas").pathsToMatch("/api/public/saas/**", "/api/admin/saas/**").build();
    }

    @Bean
    GroupedOpenApi travelApi() {
        return GroupedOpenApi.builder()
                .group("travel")
                .pathsToMatch("/api/public/travel/**", "/api/admin/travel/**", "/api/account/travel/**")
                .build();
    }

    @Bean
    GroupedOpenApi accountApi() {
        return GroupedOpenApi.builder()
                .group("account")
                .pathsToMatch("/api/account/**", "/api/public/account/**", "/api/admin/account-portal/**")
                .build();
    }

    @Bean
    GroupedOpenApi translationApi() {
        return GroupedOpenApi.builder()
                .group("translation")
                .pathsToMatch("/api/public/translation/**", "/api/admin/translation/**")
                .build();
    }

    @Bean
    GroupedOpenApi academyApi() {
        return GroupedOpenApi.builder()
                .group("academy")
                .pathsToMatch("/api/public/academy/**", "/api/account/academy/**",
                        "/api/admin/academy/**", "/api/mobile/academy/**",
                        "/api/auth/register", "/api/auth/token", "/api/auth/token/**",
                        "/api/compat/learnpress/v1/**")
                .build();
    }

    @Bean
    GroupedOpenApi mailApi() {
        return GroupedOpenApi.builder()
                .group("mail")
                .pathsToMatch("/api/admin/mail/**")
                .build();
    }

}
