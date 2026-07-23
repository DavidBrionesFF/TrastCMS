package com.nattechnologies.trastcms.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.service.PostService;
import com.nattechnologies.trastcms.service.SiteSettingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpService {
    public static final String PROTOCOL_VERSION = "2025-11-25";

    private final ObjectMapper mapper;
    private final TrastCmsProperties properties;
    private final SiteSettingService settings;
    private final PostService posts;
    private final List<McpExtension> extensions;

    public McpService(
            ObjectMapper mapper,
            TrastCmsProperties properties,
            SiteSettingService settings,
            PostService posts,
            List<McpExtension> extensions) {
        this.mapper = mapper;
        this.properties = properties;
        this.settings = settings;
        this.posts = posts;
        this.extensions = extensions.stream()
                .sorted(Comparator.comparingInt(McpExtension::order))
                .toList();
    }

    public Object handle(JsonNode request) {
        if (request == null || request.isNull()) {
            return error(null, -32600, "Solicitud MCP vacía", null);
        }
        if (request.isArray()) {
            if (request.isEmpty()) {
                return error(null, -32600, "El lote JSON-RPC no puede estar vacío", null);
            }
            if (request.size() > 50) {
                return error(null, -32600,
                        "El lote JSON-RPC no puede superar 50 solicitudes", null);
            }
            List<Object> responses = new ArrayList<>();
            request.forEach(item -> {
                Object response = handleSingle(item);
                if (response != null) responses.add(response);
            });
            return responses.isEmpty() ? null : List.copyOf(responses);
        }
        return handleSingle(request);
    }

    private Object handleSingle(JsonNode request) {
        JsonNode id = request.get("id");
        if (!request.isObject()
                || !"2.0".equals(request.path("jsonrpc").asText())) {
            return error(id, -32600,
                    "La solicitud debe ser un objeto JSON-RPC 2.0", null);
        }
        String method = request.path("method").asText();
        if (method.isBlank()) {
            return error(id, -32600, "La solicitud no contiene method", null);
        }
        boolean notification = id == null || id.isNull();
        try {
            Object result = dispatch(method, request.path("params"));
            if (notification) return null;
            return success(id, result);
        } catch (McpMethodNotFoundException exception) {
            return notification ? null
                    : error(id, -32601, exception.getMessage(), null);
        } catch (IllegalArgumentException exception) {
            return notification ? null
                    : error(id, -32602, exception.getMessage(), null);
        } catch (Exception exception) {
            return notification ? null
                    : error(id, -32603,
                            "No se pudo completar la operación MCP",
                            Map.of("exception",
                                    exception.getClass().getSimpleName()));
        }
    }

    public Map<String, Object> catalog() {
        return Map.of(
                "enabled", properties.getMcp().isEnabled(),
                "configured", properties.getMcp().getToken() != null
                        && !properties.getMcp().getToken().isBlank(),
                "endpoint", properties.getMcp().normalizedEndpoint(),
                "protocolVersion", PROTOCOL_VERSION,
                "tools", tools(),
                "resources", resources(),
                "resourceTemplates", resourceTemplates(),
                "prompts", prompts());
    }

    private Object dispatch(String method, JsonNode params) {
        return switch (method) {
            case "initialize" -> initialize(params);
            case "notifications/initialized" -> Map.of();
            case "ping" -> Map.of();
            case "tools/list" -> Map.of("tools", tools());
            case "tools/call" -> callTool(params);
            case "resources/list" -> Map.of("resources", resources());
            case "resources/templates/list" -> Map.of(
                    "resourceTemplates", resourceTemplates());
            case "resources/read" -> readResource(params);
            case "prompts/list" -> Map.of("prompts", prompts());
            case "prompts/get" -> getPrompt(params);
            default -> throw new McpMethodNotFoundException(method);
        };
    }

    private Object initialize(JsonNode params) {
        String requested = params.path("protocolVersion").asText(PROTOCOL_VERSION);
        String selected = List.of(PROTOCOL_VERSION, "2025-06-18")
                .contains(requested) ? requested : PROTOCOL_VERSION;
        return Map.of(
                "protocolVersion", selected,
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", false),
                        "resources", Map.of("subscribe", false, "listChanged", false),
                        "prompts", Map.of("listChanged", false)),
                "serverInfo", Map.of(
                        "name", "TrastCMS",
                        "title", "TrastCMS MCP Server",
                        "version", properties.getMcp().getServerVersion()),
                "instructions", "Use las herramientas para consultar el sitio, administrar borradores y trabajar con las extensiones activas. Confirme con el usuario antes de ejecutar operaciones de escritura.");
    }

    private Object callTool(JsonNode params) {
        String name = params.path("name").asText();
        if (name.isBlank()) throw new IllegalArgumentException("name es obligatorio");
        Map<String, Object> arguments = params.has("arguments")
                ? mapper.convertValue(params.get("arguments"), new TypeReference<>() { })
                : Map.of();
        for (McpExtension extension : activeExtensions()) {
            boolean supported = extension.tools().stream()
                    .anyMatch(tool -> tool.name().equals(name));
            if (!supported) continue;
            Object value = extension.call(name, arguments);
            return toolResult(value, false);
        }
        throw new IllegalArgumentException("Herramienta MCP desconocida: " + name);
    }

    private List<Map<String, Object>> tools() {
        return activeExtensions().stream()
                .flatMap(extension -> extension.tools().stream())
                .sorted(Comparator.comparing(McpExtension.McpToolDefinition::name))
                .map(McpExtension.McpToolDefinition::asProtocolMap)
                .toList();
    }


    private List<McpExtension> activeExtensions() {
        return extensions.stream()
                .filter(McpExtension::enabled)
                .toList();
    }

    private List<Map<String, Object>> resources() {
        return List.of(
                resource("trastcms://site", "Información del sitio",
                        "Identidad, tema y configuración pública.", "application/json"),
                resource("trastcms://navigation", "Navegación pública",
                        "Páginas publicadas visibles en el menú.", "application/json"),
                resource("trastcms://content/schema", "Esquema de contenido",
                        "Tipos, estados y modos editoriales disponibles.", "application/json"));
    }

    private List<Map<String, Object>> resourceTemplates() {
        return List.of(
                template("trastcms://page/{slug}", "Página por slug",
                        "Lee una página publicada como contexto estructurado."),
                template("trastcms://post/{slug}", "Publicación por slug",
                        "Lee una publicación publicada como contexto estructurado."));
    }

    private Object readResource(JsonNode params) {
        String uri = params.path("uri").asText();
        if (uri.isBlank()) throw new IllegalArgumentException("uri es obligatorio");
        Object value;
        if (uri.equals("trastcms://site")) {
            value = settings.siteInfo();
        } else if (uri.equals("trastcms://navigation")) {
            value = posts.navigation();
        } else if (uri.equals("trastcms://content/schema")) {
            value = Map.of(
                    "contentTypes", List.of("POST", "PAGE"),
                    "statuses", List.of("DRAFT", "PUBLISHED", "ARCHIVED"),
                    "editorModes", List.of("RICH_TEXT", "VISUAL_BUILDER"));
        } else if (uri.startsWith("trastcms://page/")) {
            value = posts.findPublicBySlug(
                    uri.substring("trastcms://page/".length()), ContentType.PAGE);
        } else if (uri.startsWith("trastcms://post/")) {
            value = posts.findPublicBySlug(
                    uri.substring("trastcms://post/".length()), ContentType.POST);
        } else {
            throw new IllegalArgumentException("Recurso MCP desconocido: " + uri);
        }
        return Map.of("contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", json(value))));
    }

    private List<Map<String, Object>> prompts() {
        return List.of(
                Map.of(
                        "name", "cms_page_brief",
                        "title", "Brief para una página",
                        "description", "Genera un brief para construir una página visual en TrastCMS.",
                        "arguments", List.of(
                                promptArgument("goal", "Objetivo de la página", true),
                                promptArgument("audience", "Público objetivo", true),
                                promptArgument("tone", "Tono de comunicación", false))),
                Map.of(
                        "name", "seo_article_outline",
                        "title", "Esquema SEO de artículo",
                        "description", "Prepara una estructura editorial antes de crear un borrador.",
                        "arguments", List.of(
                                promptArgument("topic", "Tema principal", true),
                                promptArgument("keyword", "Palabra clave principal", true))));
    }

    private Object getPrompt(JsonNode params) {
        String name = params.path("name").asText();
        Map<String, Object> arguments = params.has("arguments")
                ? mapper.convertValue(params.get("arguments"), new TypeReference<>() { })
                : Map.of();
        String text = switch (name) {
            case "cms_page_brief" -> "Diseñe un brief de página para TrastCMS. Objetivo: "
                    + argument(arguments, "goal") + ". Público: "
                    + argument(arguments, "audience") + ". Tono: "
                    + arguments.getOrDefault("tone", "profesional y claro")
                    + ". Proponga hero, prueba de valor, secciones, CTA, SEO y bloques del constructor visual.";
            case "seo_article_outline" -> "Cree un esquema SEO para un artículo sobre "
                    + argument(arguments, "topic") + " orientado a la palabra clave "
                    + argument(arguments, "keyword")
                    + ". Incluya intención de búsqueda, título, meta descripción, H2/H3, preguntas frecuentes y CTA.";
            default -> throw new IllegalArgumentException("Prompt MCP desconocido: " + name);
        };
        return Map.of(
                "description", "Plantilla generada por TrastCMS",
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", Map.of("type", "text", "text", text))));
    }

    private Map<String, Object> toolResult(Object value, boolean error) {
        Object normalized = value == null ? Map.of() : value;
        Object structured = normalized instanceof Map<?, ?>
                ? normalized
                : Map.of("result", normalized);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of(
                "type", "text",
                "text", json(normalized))));
        result.put("structuredContent", structured);
        result.put("isError", error);
        return result;
    }

    private Map<String, Object> success(JsonNode id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(
            JsonNode id, int code, String message, Object data) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("code", code);
        detail.put("message", message == null ? "Error MCP" : message);
        if (data != null) detail.put("data", data);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id == null ? null : id);
        response.put("error", detail);
        return response;
    }

    private Map<String, Object> resource(
            String uri, String name, String description, String mimeType) {
        return Map.of(
                "uri", uri,
                "name", name,
                "description", description,
                "mimeType", mimeType);
    }

    private Map<String, Object> template(
            String uriTemplate, String name, String description) {
        return Map.of(
                "uriTemplate", uriTemplate,
                "name", name,
                "description", description,
                "mimeType", "application/json");
    }

    private Map<String, Object> promptArgument(
            String name, String description, boolean required) {
        return Map.of(
                "name", name,
                "description", description,
                "required", required);
    }

    private String argument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("El argumento " + name + " es obligatorio");
        }
        return String.valueOf(value);
    }

    private String json(Object value) {
        try { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value); }
        catch (Exception exception) { return String.valueOf(value); }
    }

    private static final class McpMethodNotFoundException
            extends IllegalArgumentException {
        private McpMethodNotFoundException(String method) {
            super("Método MCP no soportado: " + method);
        }
    }
}
