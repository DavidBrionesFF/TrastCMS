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
                        .description("API REST del CMS para contenido, medios, temas, usuarios, plugins, TrastCRM, TrastPay, TrastStore y TrastSaaS. Los endpoints administrativos utilizan sesión y CSRF.")
                        .contact(new Contact()
                                .name("NaT Technologies")
                                .url("https://github.com/DavidBrionesFF/TrastCMS"))
                        .license(new License()
                                .name("GPL-3.0")
                                .url("https://www.gnu.org/licenses/gpl-3.0.html")))
                .servers(List.of(new Server()
                        .url(properties.getBaseUrl())
                        .description("Servidor configurado de TrastCMS")))
                .externalDocs(new ExternalDocumentation()
                        .description("Portal para desarrolladores")
                        .url(properties.getBaseUrl() + "/developers"));
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

}
