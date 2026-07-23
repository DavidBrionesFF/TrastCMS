package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.mcp.McpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/developer")
public class AdminDeveloperController {
    private final McpService mcp;

    public AdminDeveloperController(McpService mcp) {
        this.mcp = mcp;
    }

    @GetMapping
    public Map<String, Object> platform() {
        return Map.of(
                "rest", Map.of(
                        "openApi", "/v3/api-docs",
                        "swaggerUi", "/swagger-ui/index.html",
                        "documentation", "/developers#rest"),
                "mcp", mcp.catalog());
    }
}
