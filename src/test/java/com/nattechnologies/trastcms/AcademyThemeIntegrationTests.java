package com.nattechnologies.trastcms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:academytheme;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "trastcms.data-dir=target/test-data-academy-theme",
        "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class AcademyThemeIntegrationTests {

    @BeforeEach
    void academyHost() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/")
                        .header("X-Forwarded-Host", "academy.localhost")
                        .header("X-Forwarded-Port", "8080"))
                .build();
    }


    MockMvc mvc;
    @Autowired WebApplicationContext context;

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void explicitRestoreKeepsAcademyContentConsistent() throws Exception {
        mvc.perform(get("/api/public/pages/quienes-somos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeOrigin").value("nexus"))
                .andExpect(jsonPath("$.builderData", containsString("academy-manifesto")));

        mvc.perform(post("/api/admin/themes/nexus/starter-content/restore").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").isNumber());

        mvc.perform(get("/api/public/pages/quienes-somos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeOrigin").value("nexus"))
                .andExpect(jsonPath("$.builderData", containsString("academy-manifesto")));

        mvc.perform(get("/api/public/pages/preguntas-frecuentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeOrigin").value("nexus"))
                .andExpect(jsonPath("$.builderData", containsString("academy-faq-grid")));
    }
}
