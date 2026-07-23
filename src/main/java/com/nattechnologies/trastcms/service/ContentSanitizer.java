package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ContentSanitizer {
    private static final String SANITIZER_BASE_URI = "https://trastcms.invalid/";
    private static final Pattern UNSAFE_CSS = Pattern.compile(
            "(?i)(?:javascript\\s*:|expression\\s*\\(|@import|behavior\\s*:|binding\\s*:|-moz-binding)");
    private static final Pattern SCRIPT_BLOCK = Pattern.compile(
            "(?is)<script\\b[^>]*>.*?</script\\s*>");
    private static final Set<String> HTML_FIELDS = Set.of("html", "body", "content", "template", "_template");
    private static final Set<String> URL_FIELDS = Set.of("src", "href", "poster", "backgroundImage", "link");
    private static final Set<String> INLINE_STYLE_PROPERTIES = Set.of(
            "text-align", "background-color", "color", "width", "height",
            "max-width", "min-width", "max-height", "min-height");
    private static final Pattern SAFE_INLINE_STYLE_VALUE = Pattern.compile(
            "[a-zA-Z0-9#.,%()\\s/+_-]{1,120}");

    private final Safelist richContent;
    private final ObjectMapper objectMapper;

    public ContentSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        richContent = Safelist.relaxed()
                .addTags("section", "article", "header", "footer", "main", "nav", "aside",
                        "figure", "figcaption", "picture", "source", "video", "audio",
                        "iframe", "details", "summary", "mark", "s", "u", "small",
                        "table", "thead", "tbody", "tfoot", "colgroup", "col", "tr", "th", "td",
                        "pre", "code", "span", "div", "hr")
                .addAttributes(":all", "class", "id", "title", "role", "aria-label", "aria-hidden", "style")
                .addAttributes("a", "target", "rel", "download")
                .addAttributes("img", "src", "alt", "width", "height", "loading", "decoding")
                .addAttributes("video", "src", "controls", "poster", "preload", "autoplay", "muted", "loop", "playsinline", "data-trast-media", "kind", "contenttype")
                .addAttributes("audio", "src", "controls", "preload", "autoplay", "muted", "loop", "data-trast-media", "kind", "contenttype")
                .addAttributes("mark", "data-color")
                .addAttributes("source", "src", "type")
                .addAttributes("iframe", "src", "width", "height", "allow", "allowfullscreen", "loading", "referrerpolicy")
                .addAttributes("table", "colspan", "rowspan")
                .addAttributes("colgroup", "span")
                .addAttributes("col", "span", "width")
                .addAttributes("th", "colspan", "rowspan", "scope")
                .addAttributes("td", "colspan", "rowspan")
                .addProtocols("a", "href", "http", "https", "mailto", "tel")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("video", "src", "http", "https")
                .addProtocols("video", "poster", "http", "https")
                .addProtocols("audio", "src", "http", "https")
                .addProtocols("source", "src", "http", "https")
                .addProtocols("iframe", "src", "https")
                .preserveRelativeLinks(true);
    }

    public String html(String value) {
        if (value == null || value.isBlank()) return "";
        String withoutScripts = SCRIPT_BLOCK.matcher(value).replaceAll("");
        String cleaned = Jsoup.clean(
                withoutScripts,
                SANITIZER_BASE_URI,
                richContent,
                new Document.OutputSettings().prettyPrint(false));

        Document fragment = Jsoup.parseBodyFragment(cleaned);
        fragment.outputSettings().prettyPrint(false);
        for (Element element : fragment.body().getAllElements()) {
            if (!element.hasAttr("style")) continue;
            String safeStyle = sanitizeInlineStyle(element.attr("style"));
            if (safeStyle.isBlank()) element.removeAttr("style");
            else element.attr("style", safeStyle);
        }
        return fragment.body().html();
    }

    private String sanitizeInlineStyle(String style) {
        if (style == null || style.isBlank() || UNSAFE_CSS.matcher(style).find()) {
            return "";
        }
        StringBuilder safe = new StringBuilder();
        for (String declaration : style.split(";")) {
            int separator = declaration.indexOf(':');
            if (separator < 1) continue;
            String property = declaration.substring(0, separator)
                    .trim()
                    .toLowerCase();
            String cssValue = declaration.substring(separator + 1).trim();
            if (!INLINE_STYLE_PROPERTIES.contains(property)) continue;
            if (!SAFE_INLINE_STYLE_VALUE.matcher(cssValue).matches()) continue;
            if (safe.length() > 0) safe.append(';');
            safe.append(property).append(':').append(cssValue);
        }
        return safe.toString();
    }

    public String css(String value) {
        if (value == null || value.isBlank()) return "";
        if (UNSAFE_CSS.matcher(value).find()) {
            throw new BadRequestException("El CSS contiene construcciones no permitidas");
        }
        if (value.length() > 500_000) {
            throw new BadRequestException("El CSS de la página supera el límite de 500 KB");
        }
        return value;
    }

    public String json(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 5_000_000) {
            throw new BadRequestException("El proyecto visual supera el límite de 5 MB");
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (!root.isObject() || root.path("version").asInt(-1) != 1 || !root.path("blocks").isArray()) {
                throw new BadRequestException("El proyecto visual no tiene un formato válido");
            }
            sanitizeNode(root, null);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("El proyecto visual contiene JSON inválido");
        }
    }

    private void sanitizeNode(JsonNode node, String fieldName) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = entry.getValue();
                if (child.isTextual() && HTML_FIELDS.contains(entry.getKey())) {
                    object.put(entry.getKey(), html(child.asText()));
                } else if (child.isTextual() && URL_FIELDS.contains(entry.getKey())) {
                    object.put(entry.getKey(), safeUrl(child.asText()));
                } else {
                    sanitizeNode(child, entry.getKey());
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (JsonNode child : array) sanitizeNode(child, fieldName);
        }
    }

    private String safeUrl(String value) {
        if (value == null || value.isBlank()) return "";
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:text/html") || lower.startsWith("vbscript:")) {
            throw new BadRequestException("El constructor contiene una URL no permitida");
        }
        return trimmed;
    }
}
