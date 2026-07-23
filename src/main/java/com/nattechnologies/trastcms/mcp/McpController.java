package com.nattechnologies.trastcms.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.nattechnologies.trastcms.config.TrastCmsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
public class McpController {
    private final McpService service;
    private final TrastCmsProperties properties;

    public McpController(McpService service, TrastCmsProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/.well-known/mcp.json")
    public Map<String, Object> discovery() {
        return Map.of(
                "name", "TrastCMS",
                "title", "TrastCMS MCP Server",
                "enabled", properties.getMcp().isEnabled(),
                "endpoint", properties.getMcp().normalizedEndpoint(),
                "transport", "streamable-http",
                "protocolVersion", McpService.PROTOCOL_VERSION,
                "authentication", Map.of(
                        "type", "bearer",
                        "environmentVariable", "TRASTCMS_MCP_TOKEN"),
                "documentation", "/developers#mcp");
    }

    @PostMapping(
            value = "${trastcms.mcp.endpoint:/mcp}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> invoke(
            @RequestHeader HttpHeaders headers,
            @RequestBody JsonNode request) {
        requireAccess(headers);
        validateProtocolHeader(headers, request);
        Object response = service.handle(request);
        if (response == null) {
            return ResponseEntity.accepted()
                    .header("MCP-Protocol-Version", McpService.PROTOCOL_VERSION)
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .build();
        }
        return ResponseEntity.ok()
                .header("MCP-Protocol-Version", McpService.PROTOCOL_VERSION)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping("${trastcms.mcp.endpoint:/mcp}")
    public ResponseEntity<Void> streamNotEnabled() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, "POST")
                .build();
    }

    @DeleteMapping("${trastcms.mcp.endpoint:/mcp}")
    public ResponseEntity<Void> deleteNotEnabled() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, "POST")
                .build();
    }


    private void validateProtocolHeader(
            HttpHeaders headers,
            JsonNode request) {
        String method = request != null && request.isObject()
                ? request.path("method").asText()
                : "";
        if ("initialize".equals(method) || request == null || request.isArray()) {
            return;
        }
        String protocol = headers.getFirst("MCP-Protocol-Version");
        if (protocol != null
                && !protocol.isBlank()
                && !McpService.PROTOCOL_VERSION.equals(protocol)
                && !"2025-06-18".equals(protocol)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Versión MCP no soportada: " + protocol);
        }
    }

    private void requireAccess(HttpHeaders headers) {
        if (!properties.getMcp().isEnabled()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El servidor MCP está deshabilitado");
        }
        String configured = properties.getMcp().getToken();
        if (configured == null || configured.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure TRASTCMS_MCP_TOKEN antes de utilizar MCP");
        }
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String received = authorization != null
                && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : "";
        boolean matches = MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token MCP inválido");
        }
    }
}
