package com.nattechnologies.trastcms.mcp;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.domain.post.EditorMode;
import com.nattechnologies.trastcms.domain.post.PostStatus;
import com.nattechnologies.trastcms.service.CategoryService;
import com.nattechnologies.trastcms.service.DashboardService;
import com.nattechnologies.trastcms.service.PostService;
import com.nattechnologies.trastcms.service.SiteSettingService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CoreMcpExtension implements McpExtension {
    private final PostService posts;
    private final CategoryService categories;
    private final SiteSettingService settings;
    private final DashboardService dashboard;
    private final TrastCmsProperties properties;

    public CoreMcpExtension(
            PostService posts,
            CategoryService categories,
            SiteSettingService settings,
            DashboardService dashboard,
            TrastCmsProperties properties) {
        this.posts = posts;
        this.categories = categories;
        this.settings = settings;
        this.dashboard = dashboard;
        this.properties = properties;
    }

    @Override
    public int order() { return 0; }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool("site_overview", "Resumen del sitio",
                        "Devuelve identidad, configuración pública y métricas principales de TrastCMS.",
                        objectSchema(Map.of(), List.of()), true, false, true),
                tool("content_list", "Listar contenido",
                        "Lista publicaciones o páginas administrables con paginación.",
                        objectSchema(Map.of(
                                "type", enumProperty("POST", "PAGE"),
                                "page", integerProperty(0, 100000),
                                "size", integerProperty(1, 100)), List.of()),
                        true, false, true),
                tool("content_get", "Obtener contenido",
                        "Obtiene una publicación o página por slug, incluyendo borradores.",
                        objectSchema(Map.of(
                                "slug", stringProperty("Slug del contenido"),
                                "type", enumProperty("POST", "PAGE")),
                                List.of("slug", "type")), true, false, true),
                tool("content_create_draft", "Crear borrador",
                        "Crea un borrador de publicación o página utilizando el usuario administrador configurado.",
                        objectSchema(Map.of(
                                "title", stringProperty("Título del contenido"),
                                "type", enumProperty("POST", "PAGE"),
                                "excerpt", stringProperty("Resumen opcional"),
                                "body", stringProperty("Contenido HTML seguro"),
                                "categoryId", stringProperty("ID opcional de categoría")),
                                List.of("title", "type")), false, false, false),
                tool("category_list", "Listar categorías",
                        "Devuelve el catálogo de categorías editoriales.",
                        objectSchema(Map.of(), List.of()), true, false, true));
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "site_overview" -> Map.of(
                    "site", settings.siteInfo(),
                    "dashboard", dashboard.dashboard(properties.getAdmin().getEmail()),
                    "baseUrl", properties.getBaseUrl());
            case "content_list" -> posts.adminList(
                    integer(arguments, "page", 0),
                    integer(arguments, "size", 20),
                    contentType(arguments.get("type")),
                    properties.getAdmin().getEmail());
            case "content_get" -> posts.findBySlugForMcp(
                    required(arguments, "slug"),
                    contentType(required(arguments, "type")));
            case "content_create_draft" -> createDraft(arguments);
            case "category_list" -> categories.list();
            default -> throw new IllegalArgumentException("Herramienta MCP desconocida: " + toolName);
        };
    }

    private ApiDtos.PostResponse createDraft(Map<String, Object> arguments) {
        ContentType type = contentType(arguments.get("type"));
        String title = required(arguments, "title");
        String body = text(arguments.get("body"));
        ApiDtos.PostRequest request = new ApiDtos.PostRequest(
                title,
                null,
                nullable(arguments.get("excerpt")),
                body == null ? "" : body,
                type == ContentType.PAGE ? EditorMode.VISUAL_BUILDER : EditorMode.RICH_TEXT,
                type == ContentType.PAGE
                        ? "{\"version\":1,\"blocks\":[],\"global\":{\"containerWidth\":1200,\"gap\":24}}"
                        : null,
                "",
                null,
                null,
                "default",
                false,
                0,
                null,
                type,
                PostStatus.DRAFT,
                null,
                nullable(arguments.get("categoryId")));
        return posts.create(request, properties.getAdmin().getEmail());
    }

    private ContentType contentType(Object value) {
        String normalized = value == null ? "POST" : String.valueOf(value)
                .trim().toUpperCase(Locale.ROOT);
        try {
            return ContentType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("type debe ser POST o PAGE");
        }
    }

    private int integer(Map<String, Object> arguments, String key, int fallback) {
        Object raw = arguments.get(key);
        if (raw == null) return fallback;
        if (raw instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(raw)); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " debe ser numérico");
        }
    }

    private String required(Map<String, Object> arguments, String key) {
        String value = text(arguments.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El argumento " + key + " es obligatorio");
        }
        return value;
    }

    private String nullable(Object value) {
        String normalized = text(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private McpToolDefinition tool(
            String name, String title, String description,
            Map<String, Object> schema,
            boolean readOnly, boolean destructive, boolean idempotent) {
        return new McpToolDefinition(
                name, title, description, schema,
                readOnly, destructive, idempotent);
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> integerProperty(int minimum, int maximum) {
        return Map.of(
                "type", "integer",
                "minimum", minimum,
                "maximum", maximum);
    }

    private Map<String, Object> enumProperty(String... values) {
        return Map.of("type", "string", "enum", List.of(values));
    }
}
