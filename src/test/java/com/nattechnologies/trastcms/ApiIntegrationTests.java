package com.nattechnologies.trastcms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "trastcms.data-dir=target/test-data-api",
    "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class ApiIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void publicSiteIsAvailable() throws Exception {
        mvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TrastCMS"));
    }

    @Test
    void adminApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void authenticatedAdminCanCreateCategory() throws Exception {
        mvc.perform(post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Tecnología","slug":"tecnologia","description":"Contenido tecnológico"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("tecnologia"));
    }

    @Test
    void builtInThemeStylesheetIsPublic() throws Exception {
        mvc.perform(get("/api/public/themes/aurora/tokens.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void authenticatedAdminCanCreateAndPublishPage() throws Exception {
        mvc.perform(post("/api/admin/posts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Nosotros","slug":"nosotros","excerpt":"Información corporativa",
                     "body":"Contenido de la página","contentType":"PAGE","status":"PUBLISHED",
                     "featuredImageUrl":null,"categoryId":null}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("PAGE"));

        mvc.perform(get("/api/public/pages/nosotros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nosotros"));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void administratorCanListUsers() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@trastcms.local"));
    }

}
