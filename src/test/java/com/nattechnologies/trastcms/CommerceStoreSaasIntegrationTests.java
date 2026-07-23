package com.nattechnologies.trastcms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:commerce;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "trastcms.data-dir=target/test-data-commerce",
    "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class CommerceStoreSaasIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void bundledCommercePluginsAreRegisteredAndEnabled() throws Exception {
        mvc.perform(get("/api/admin/plugins/bundled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].pluginId", hasItems(
                        "trastpay",
                        "traststore",
                        "trastsaas")))
                .andExpect(jsonPath("$[?(@.pluginId == 'trastpay')].enabled")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'traststore')].enabled")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.pluginId == 'trastsaas')].enabled")
                        .value(hasItem(true)));
    }

    @Test
    void storeProductCanBePurchasedThroughSandboxCheckout() throws Exception {
        JsonNode catalog = json(mvc.perform(get("/api/public/store/products")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn());
        String productId = catalog.path("content").get(0).path("id").asText();

        String cartToken = createCart();

        mvc.perform(post("/api/public/commerce/carts/{token}/items", cartToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "providerKey":"store",
                              "reference":"%s",
                              "quantity":1,
                              "metadata":{}
                            }
                            """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].providerKey").value("store"));

        mvc.perform(post("/api/public/commerce/carts/{token}/checkout", cartToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody("store@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.orderNumber").isNotEmpty());
    }

    @Test
    void saasCheckoutIssuesAClaimableAndActivatableLicense() throws Exception {
        JsonNode planList = json(mvc.perform(
                        get("/api/public/saas/products/trast-cloud/plans"))
                .andExpect(status().isOk())
                .andReturn());
        String planId = planList.get(0).path("id").asText();

        String cartToken = createCart();

        mvc.perform(post("/api/public/commerce/carts/{token}/items", cartToken)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "providerKey":"saas",
                              "reference":"%s",
                              "quantity":1,
                              "metadata":{}
                            }
                            """.formatted(planId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].providerKey").value("saas"));

        JsonNode checkout = json(mvc.perform(
                        post("/api/public/commerce/carts/{token}/checkout", cartToken)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(checkoutBody("license@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andReturn());

        String orderNumber = checkout.path("orderNumber").asText();
        String orderToken = checkout.path("orderToken").asText();

        JsonNode claim = json(mvc.perform(get(
                        "/api/public/saas/orders/{orderNumber}/claim",
                        orderNumber)
                        .param("token", orderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenses[0].licenseKey").isNotEmpty())
                .andExpect(jsonPath("$.subscriptions[0].status").exists())
                .andReturn());

        String licenseKey = claim.path("licenses").get(0)
                .path("licenseKey").asText();

        mvc.perform(post("/api/public/saas/licenses/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "licenseKey":"%s",
                              "productKey":"trast-cloud",
                              "fingerprint":"integration-test-device",
                              "deviceName":"Equipo de prueba",
                              "platform":"windows",
                              "applicationVersion":"1.0.0"
                            }
                            """.formatted(licenseKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activationId").isNotEmpty())
                .andExpect(jsonPath("$.entitlements").isMap());
    }

    private String createCart() throws Exception {
        JsonNode cart = json(mvc.perform(post("/api/public/commerce/carts")
                        .with(csrf())
                        .param("currency", "USD"))
                .andExpect(status().isCreated())
                .andReturn());
        return cart.path("token").asText();
    }

    private String checkoutBody(String email) {
        return """
            {
              "email":"%s",
              "name":"Cliente de integración",
              "phone":"+504 9999-0000",
              "gatewayKey":"sandbox",
              "billing":{},
              "shipping":{},
              "notes":"Compra automatizada de prueba",
              "returnUrl":"http://localhost/cart",
              "cancelUrl":"http://localhost/cart",
              "idempotencyKey":"%s"
            }
            """.formatted(email, UUID.randomUUID());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
