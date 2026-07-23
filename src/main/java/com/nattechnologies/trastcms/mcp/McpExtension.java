package com.nattechnologies.trastcms.mcp;

import java.util.List;
import java.util.Map;

/**
 * Extension point used by the core and bundled plugins to expose MCP tools.
 */
public interface McpExtension {
    default int order() { return 100; }
    default boolean enabled() { return true; }
    List<McpToolDefinition> tools();
    Object call(String toolName, Map<String, Object> arguments);

    record McpToolDefinition(
            String name,
            String title,
            String description,
            Map<String, Object> inputSchema,
            boolean readOnly,
            boolean destructive,
            boolean idempotent) {

        public Map<String, Object> asProtocolMap() {
            return Map.of(
                    "name", name,
                    "title", title,
                    "description", description,
                    "inputSchema", inputSchema,
                    "annotations", Map.of(
                            "readOnlyHint", readOnly,
                            "destructiveHint", destructive,
                            "idempotentHint", idempotent,
                            "openWorldHint", false));
        }
    }
}
