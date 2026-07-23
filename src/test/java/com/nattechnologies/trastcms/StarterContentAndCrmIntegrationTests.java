package com.nattechnologies.trastcms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:startercrm;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "trastcms.data-dir=target/test-data-starter-crm",
    "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class StarterContentAndCrmIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void activeThemeCreatesEditableLocalPagesAndNavigation() throws Exception {
        mvc.perform(get("/api/public/pages/inicio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("PAGE"))
                .andExpect(jsonPath("$.pageRole").value("HOME"))
                .andExpect(jsonPath("$.themeOrigin").value("aurora"))
                .andExpect(jsonPath("$.builderData",
                        containsString("starter-hero")))
                .andExpect(jsonPath("$.builderData",
                        containsString("Model Context Protocol")));

        mvc.perform(get("/api/public/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("inicio"))
                .andExpect(jsonPath("$[1].slug").value("quienes-somos"))
                .andExpect(jsonPath("$[4].slug").value("contactanos"));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void bundledCrmIsRegisteredAndEnabledByDefault() throws Exception {
        mvc.perform(get("/api/admin/plugins/bundled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pluginId").value("trastcrm"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void publicCrmFormCanCaptureALead() throws Exception {
        mvc.perform(get("/api/public/plugins/trastcrm/forms/contacto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formKey").value("contacto"));

        mvc.perform(post("/api/public/plugins/trastcrm/forms/contacto/submissions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceUrl":"/page/contactanos",
                      "values":{
                        "nombre":"Cliente de prueba",
                        "email":"cliente@example.com",
                        "telefono":"+504 9999-9999",
                        "empresa":"Empresa Demo",
                        "mensaje":"Deseo información del CMS"
                      }
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
