package com.nattechnologies.trastcms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.service.SiteSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:multisite;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "trastcms.data-dir=target/test-data-multisite",
        "trastcms.admin.email=admin@trastcms.local",
        "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class MultisiteIntegrationTests {

    private static final String PRIMARY = "primary-site";
    private static final String ACADEMY = "academy";
    private static final String SAAS = "saas-store";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired SiteSettingService settings;

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void networkStartsWithThreeIsolatedSites() throws Exception {
        mvc.perform(get("/api/admin/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].siteKey", hasItem(PRIMARY)))
                .andExpect(jsonPath("$[*].siteKey", hasItem(ACADEMY)))
                .andExpect(jsonPath("$[*].siteKey", hasItem(SAAS)));

        mvc.perform(get("/api/admin/plugins/bundled").header("X-TrastCMS-Site", PRIMARY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pluginId == 'trasttravel')].enabled").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'trastacademy')].enabled").value(hasItem(false)));

        mvc.perform(get("/api/admin/plugins/bundled").header("X-TrastCMS-Site", ACADEMY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pluginId == 'trastacademy')].enabled").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'trasttravel')].enabled").value(hasItem(false)));

        mvc.perform(get("/api/admin/plugins/bundled").header("X-TrastCMS-Site", SAAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pluginId == 'traststore')].enabled").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'trastsaas')].enabled").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'trasttravel')].enabled").value(hasItem(false)));

    }

    @Test
    void siteSettingsUseIndependentNamespaces() {
        SiteContext.withSite(new SiteContext.ResolvedSite(SiteContext.PRIMARY_SITE_ID, PRIMARY, "Primary Site"),
                () -> settings.put("test.multisite.marker", "primary"));
        SiteContext.withSite(new SiteContext.ResolvedSite(SiteContext.ACADEMY_SITE_ID, ACADEMY, "Academy"),
                () -> settings.put("test.multisite.marker", "academy"));
        SiteContext.withSite(new SiteContext.ResolvedSite(SiteContext.SAAS_SITE_ID, SAAS, "Software Store"),
                () -> settings.put("test.multisite.marker", "saas"));

        assertEquals("primary", SiteContext.withSite(
                new SiteContext.ResolvedSite(SiteContext.PRIMARY_SITE_ID, PRIMARY, "Primary Site"),
                () -> settings.get("test.multisite.marker", "")));
        assertEquals("academy", SiteContext.withSite(
                new SiteContext.ResolvedSite(SiteContext.ACADEMY_SITE_ID, ACADEMY, "Academy"),
                () -> settings.get("test.multisite.marker", "")));
        assertEquals("saas", SiteContext.withSite(
                new SiteContext.ResolvedSite(SiteContext.SAAS_SITE_ID, SAAS, "Software Store"),
                () -> settings.get("test.multisite.marker", "")));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void samePageSlugCanExistInDifferentSitesAndPublicHostSelectsCorrectOne() throws Exception {
        createPage(PRIMARY, "Página Primary", "prueba-tenant");
        createPage(ACADEMY, "Página Academy", "prueba-tenant");
        createPage(SAAS, "Página SaaS", "prueba-tenant");

        publicPage("localhost", "8080", "Página Primary");
        publicPage("academy.localhost", "8080", "Página Academy");
        publicPage("saas.localhost", "8080", "Página SaaS");
    }



    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void mcpCatalogUsesTheCurrentSiteCanonicalUrl() throws Exception {
        mvc.perform(get("/api/admin/developer").header("X-TrastCMS-Site", ACADEMY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mcp.site.key").value(ACADEMY))
                .andExpect(jsonPath("$.mcp.absoluteEndpoint").value("http://academy.localhost:8080/mcp"));

        mvc.perform(get("/api/admin/developer").header("X-TrastCMS-Site", SAAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mcp.site.key").value(SAAS))
                .andExpect(jsonPath("$.mcp.absoluteEndpoint").value("http://saas.localhost:8080/mcp"));
    }

    @Test
    void unknownPublicHostFailsClosedInsteadOfLeakingDefaultSite() throws Exception {
        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "unregistered.example.test")
                        .header("X-Forwarded-Port", "443"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void publicTrafficCannotOverrideTenantWithAdministrativeHeader() throws Exception {
        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8080")
                        .header("X-TrastCMS-Site", ACADEMY))
                .andExpect(status().isOk())
                .andExpect(header().string("X-TrastCMS-Site", PRIMARY));

        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "academy.localhost")
                        .header("X-Forwarded-Port", "8080")
                        .header("X-TrastCMS-Site", SAAS))
                .andExpect(status().isOk())
                .andExpect(header().string("X-TrastCMS-Site", ACADEMY));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void availableSitesCanRecoverWhenSelectedTenantHeaderIsStale() throws Exception {
        mvc.perform(get("/api/admin/sites/available")
                        .header("X-TrastCMS-Site", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].siteKey", hasItem(PRIMARY)))
                .andExpect(jsonPath("$[*].siteKey", hasItem(ACADEMY)))
                .andExpect(jsonPath("$[*].siteKey", hasItem(SAAS)));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void usersBelongToTheSelectedSite() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("X-TrastCMS-Site", ACADEMY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"academy.editor@example.com",
                                  "displayName":"Editor Academy",
                                  "role":"EDITOR",
                                  "enabled":true,
                                  "password":"Multisite-Editor-2026!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("academy.editor@example.com"));

        mvc.perform(get("/api/admin/users").header("X-TrastCMS-Site", ACADEMY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("academy.editor@example.com")));

        mvc.perform(get("/api/admin/users").header("X-TrastCMS-Site", PRIMARY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", not(hasItem("academy.editor@example.com"))));
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void sameIdentityUsesTheRoleGrantedByEachSite() throws Exception {
        String payloadEditor = """
                {
                  "email":"shared.member@example.com",
                  "displayName":"Usuario compartido",
                  "role":"EDITOR",
                  "enabled":true,
                  "password":"Shared-Multisite-2026!"
                }
                """;
        String payloadCustomer = """
                {
                  "email":"shared.member@example.com",
                  "displayName":"Usuario compartido",
                  "role":"CUSTOMER",
                  "enabled":true,
                  "password":"Shared-Multisite-2026!"
                }
                """;

        mvc.perform(post("/api/admin/users")
                        .header("X-TrastCMS-Site", ACADEMY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadEditor))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/users")
                        .header("X-TrastCMS-Site", PRIMARY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadCustomer))
                .andExpect(status().isCreated());

        // The request starts with an intentionally over-privileged global role.
        // The multisite filter must replace it with EDITOR for Academy.
        mvc.perform(get("/api/admin/plugins/contributions")
                        .header("X-TrastCMS-Site", ACADEMY)
                        .with(user("shared.member@example.com").roles("ADMIN")))
                .andExpect(status().isOk());

        // The exact same global identity is only CUSTOMER in Primary Site, so
        // Spring Security must deny an administrative route there.
        mvc.perform(get("/api/admin/plugins/contributions")
                        .header("X-TrastCMS-Site", PRIMARY)
                        .with(user("shared.member@example.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void academyTokenCannotBeIssuedToIdentityWithoutAcademyMembership() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("X-TrastCMS-Site", PRIMARY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"primary.only@example.com",
                                  "displayName":"Solo Primary",
                                  "role":"CUSTOMER",
                                  "enabled":true,
                                  "password":"Primary-Only-2026!"
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/token")
                        .header("X-Forwarded-Host", "academy.localhost")
                        .header("X-Forwarded-Port", "8080")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"primary.only@example.com",
                                  "password":"Primary-Only-2026!",
                                  "deviceName":"Multisite test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void siteAdministratorCannotCreateNetworkSites() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("X-TrastCMS-Site", ACADEMY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"academy.admin.local@example.com",
                                  "displayName":"Administrador local",
                                  "role":"ADMIN",
                                  "enabled":true,
                                  "password":"Academy-Admin-Local-2026!"
                                }
                                """))
                .andExpect(status().isCreated());

        // Membership discovery must not depend on the identity's legacy
        // global role; otherwise a site admin whose global role is CUSTOMER
        // cannot recover the tenant switcher.
        mvc.perform(get("/api/admin/sites/available")
                        .header("X-Forwarded-Host", "academy.localhost")
                        .header("X-Forwarded-Port", "8080")
                        .with(user("academy.admin.local@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].siteKey", hasItem(ACADEMY)));

        mvc.perform(post("/api/admin/sites")
                        .header("X-TrastCMS-Site", ACADEMY)
                        .with(user("academy.admin.local@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteKey":"forbidden-network-site",
                                  "name":"No debe crearse",
                                  "locale":"es-HN",
                                  "timezone":"America/Tegucigalpa",
                                  "status":"ACTIVE",
                                  "blueprint":"EMPTY",
                                  "endpoints":[{
                                    "host":"forbidden.localhost",
                                    "port":8080,
                                    "scheme":"http",
                                    "primaryEndpoint":true,
                                    "active":true
                                  }]
                                }
                                """))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void newSaasSiteProvisioningWritesStarterContentIntoTargetTenant() throws Exception {
        mvc.perform(post("/api/admin/sites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteKey":"saas-provision-isolation",
                                  "name":"SaaS Provision Isolation",
                                  "locale":"es-HN",
                                  "timezone":"America/Tegucigalpa",
                                  "status":"ACTIVE",
                                  "blueprint":"SAAS",
                                  "endpoints":[{
                                    "host":"saas-provision.localhost",
                                    "port":8080,
                                    "scheme":"http",
                                    "primaryEndpoint":true,
                                    "active":true
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activeTheme").value("nexus"))
                .andExpect(jsonPath("$.enabledPlugins", hasItem("trastsaas")))
                .andExpect(jsonPath("$.enabledPlugins", hasItem("traststore")));

        mvc.perform(get("/api/public/pages/inicio")
                        .header("X-Forwarded-Host", "saas-provision.localhost")
                        .header("X-Forwarded-Port", "8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excerpt").value("Software, licencias y suscripciones con una experiencia digital moderna."));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void networkAdminCanCreateAndProvisionAnotherSite() throws Exception {
        MvcResult result = mvc.perform(post("/api/admin/sites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteKey":"legal-demo",
                                  "name":"Firma Legal Demo",
                                  "locale":"es-HN",
                                  "timezone":"America/Tegucigalpa",
                                  "status":"ACTIVE",
                                  "blueprint":"EMPTY",
                                  "endpoints":[{
                                    "host":"legal.localhost",
                                    "port":8080,
                                    "scheme":"http",
                                    "primaryEndpoint":true,
                                    "active":true
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteKey").value("legal-demo"))
                .andExpect(jsonPath("$.activeTheme").value("nexus"))
                .andExpect(jsonPath("$.enabledPlugins", hasItem("trastaccount")))
                .andReturn();

        JsonNode site = mapper.readTree(result.getResponse().getContentAsByteArray());
        mvc.perform(get("/api/admin/sites/available").header("X-TrastCMS-Site", "legal-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].siteKey", hasItem("legal-demo")));

        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "legal.localhost")
                        .header("X-Forwarded-Port", "8080"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-TrastCMS-Site", "legal-demo"));

        // The response must keep a stable UUID so it can be referenced by Nginx/admin automation.
        org.junit.jupiter.api.Assertions.assertFalse(site.path("id").asText().isBlank());
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void sameHostCanRouteDifferentSitesByPortWithoutAmbiguousFallback() throws Exception {
        createNetworkSite("port-site-a", "Puerto A", "ports.localhost", 8111);
        createNetworkSite("port-site-b", "Puerto B", "ports.localhost", 8112);

        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "ports.localhost")
                        .header("X-Forwarded-Port", "8111"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-TrastCMS-Site", "port-site-a"));

        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "ports.localhost")
                        .header("X-Forwarded-Port", "8112"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-TrastCMS-Site", "port-site-b"));

        // With the same host assigned to two tenants, an unknown port must not
        // pick one tenant arbitrarily.
        mvc.perform(get("/api/public/site")
                        .header("X-Forwarded-Host", "ports.localhost")
                        .header("X-Forwarded-Port", "8999"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void activeSiteRejectsAnInactivePrimaryEndpoint() throws Exception {
        mvc.perform(post("/api/admin/sites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteKey":"invalid-inactive-endpoint",
                                  "name":"Invalid endpoint tenant",
                                  "locale":"es-HN",
                                  "timezone":"America/Tegucigalpa",
                                  "status":"ACTIVE",
                                  "blueprint":"EMPTY",
                                  "endpoints":[{
                                    "host":"invalid-endpoint.localhost",
                                    "port":8080,
                                    "scheme":"http",
                                    "primaryEndpoint":true,
                                    "active":false
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].siteKey", not(hasItem("invalid-inactive-endpoint"))));
    }


    private void createNetworkSite(String siteKey, String name, String host, int port) throws Exception {
        mvc.perform(post("/api/admin/sites")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteKey":"%s",
                                  "name":"%s",
                                  "locale":"es-HN",
                                  "timezone":"America/Tegucigalpa",
                                  "status":"ACTIVE",
                                  "blueprint":"EMPTY",
                                  "endpoints":[{
                                    "host":"%s",
                                    "port":%d,
                                    "scheme":"http",
                                    "primaryEndpoint":true,
                                    "active":true
                                  }]
                                }
                                """.formatted(siteKey, name, host, port)))
                .andExpect(status().isCreated());
    }

    private void createPage(String siteKey, String title, String slug) throws Exception {
        mvc.perform(post("/api/admin/posts")
                        .header("X-TrastCMS-Site", siteKey)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "slug":"%s",
                                  "excerpt":"Aislamiento multisitio",
                                  "body":"<p>%s</p>",
                                  "editorMode":"RICH_TEXT",
                                  "builderData":null,
                                  "customCss":null,
                                  "seoTitle":null,
                                  "seoDescription":null,
                                  "template":"default",
                                  "showInMenu":false,
                                  "menuOrder":0,
                                  "pageRole":null,
                                  "contentType":"PAGE",
                                  "status":"PUBLISHED",
                                  "featuredImageUrl":null,
                                  "categoryId":null
                                }
                                """.formatted(title, slug, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug));
    }

    private void publicPage(String host, String port, String expectedTitle) throws Exception {
        mvc.perform(get("/api/public/pages/prueba-tenant")
                        .header("X-Forwarded-Host", host)
                        .header("X-Forwarded-Port", port))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expectedTitle));
    }
}
