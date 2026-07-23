package com.nattechnologies.trastcms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginState;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "trastcms.data-dir=target/test-data-mcp",
    "trastcms.admin.password=Testing-Password-2026!",
    "trastcms.mcp.enabled=true",
    "trastcms.mcp.token=testing-mcp-token-with-more-than-32-characters"
})
@AutoConfigureMockMvc
class McpIntegrationTests {

    private static final String TOKEN =
            "testing-mcp-token-with-more-than-32-characters";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    BundledPluginStateRepository pluginStates;

    @Test
    void discoveryAndDeveloperPortalArePublic() throws Exception {
        mvc.perform(get("/.well-known/mcp.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("/mcp"))
                .andExpect(jsonPath("$.transport").value("streamable-http"));

        mvc.perform(get("/developers"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void mcpRejectsRequestsWithoutBearerToken() throws Exception {
        mvc.perform(post("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(initializeRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpInitializesAndListsCoreAndActivePluginTools() throws Exception {
        mvc.perform(post("/mcp")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(initializeRequest()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "MCP-Protocol-Version", "2025-11-25"))
                .andExpect(jsonPath("$.result.serverInfo.name")
                        .value("TrastCMS"));

        mvc.perform(post("/mcp")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name",
                        hasItem("site_overview")))
                .andExpect(jsonPath("$.result.tools[*].name",
                        hasItem("content_create_draft")))
                .andExpect(jsonPath("$.result.tools[*].name",
                        hasItem("crm_pipeline_summary")));
    }

    @Test
    @Transactional
    void disabledPluginDoesNotContributeMcpTools() throws Exception {
        BundledPluginState state = pluginStates.findById("trastcrm")
                .orElseThrow();
        state.setEnabled(false);
        pluginStates.saveAndFlush(state);

        mvc.perform(post("/mcp")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{}}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name",
                        not(hasItem("crm_pipeline_summary"))))
                .andExpect(jsonPath("$.result.tools[*].name",
                        hasItem("site_overview")));
    }

    @Test
    void invalidMethodUsesJsonRpcMethodNotFoundCode() throws Exception {
        mvc.perform(post("/mcp")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"jsonrpc":"2.0","id":4,"method":"unknown/method","params":{}}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test
    void batchOmitsNotificationResponses() throws Exception {
        String batch = """
                [
                  {"jsonrpc":"2.0","method":"notifications/initialized","params":{}},
                  {"jsonrpc":"2.0","id":5,"method":"ping","params":{}}
                ]
                """;

        String body = mvc.perform(post("/mcp")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = mapper.readTree(body);
        org.assertj.core.api.Assertions.assertThat(response.isArray()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.size()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.get(0).path("id").asInt())
                .isEqualTo(5);
    }

    private String initializeRequest() {
        return """
                {
                  "jsonrpc":"2.0",
                  "id":1,
                  "method":"initialize",
                  "params":{
                    "protocolVersion":"2025-11-25",
                    "capabilities":{},
                    "clientInfo":{"name":"integration-test","version":"1.0.0"}
                  }
                }
                """;
    }
}
