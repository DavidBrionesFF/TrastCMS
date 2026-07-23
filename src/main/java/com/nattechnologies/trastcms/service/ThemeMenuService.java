package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ThemeMenuService {
    private static final String HEADER_KEY = "theme.menu.header";
    private static final String FOOTER_KEY = "theme.menu.footer";
    private static final int MAX_ITEMS = 40;
    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9-]{0,279}");
    private static final Pattern SAFE_URL = Pattern.compile(
            "(?:/[^\\s]*|#[^\\s]*|https?://[^\\s]+|mailto:[^\\s]+|tel:[+0-9() .-]+)",
            Pattern.CASE_INSENSITIVE);

    private final SiteSettingService settings;
    private final PostService posts;
    private final ObjectMapper objectMapper;
    private final AuditService audit;

    public ThemeMenuService(
            SiteSettingService settings,
            PostService posts,
            ObjectMapper objectMapper,
            AuditService audit) {
        this.settings = settings;
        this.posts = posts;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    @Transactional
    public ApiDtos.ThemeMenusResponse get() {
        List<ApiDtos.ThemeMenuItem> header = read(HEADER_KEY);
        List<ApiDtos.ThemeMenuItem> footer = read(FOOTER_KEY);
        if (header.isEmpty() && footer.isEmpty()) {
            ApiDtos.ThemeMenusResponse defaults = defaults();
            persist(defaults);
            return defaults;
        }
        return new ApiDtos.ThemeMenusResponse(sorted(header), sorted(footer));
    }

    @Transactional
    public ApiDtos.ThemeMenusResponse update(
            ApiDtos.ThemeMenusRequest request,
            String actor) {
        List<ApiDtos.ThemeMenuItem> header = normalize(request.header(), "encabezado");
        List<ApiDtos.ThemeMenuItem> footer = normalize(request.footer(), "pie de página");
        ApiDtos.ThemeMenusResponse response = new ApiDtos.ThemeMenusResponse(header, footer);
        persist(response);
        audit.record(
                actor,
                "theme.menus.updated",
                "theme-menu",
                null,
                "header=" + header.size() + ",footer=" + footer.size());
        return response;
    }

    private ApiDtos.ThemeMenusResponse defaults() {
        List<ApiDtos.NavigationItem> pages = posts.navigation();
        List<ApiDtos.ThemeMenuItem> header = new ArrayList<>();
        int order = 0;
        for (ApiDtos.NavigationItem page : pages.stream().limit(8).toList()) {
            header.add(pageItem(page, order++));
        }
        List<ApiDtos.ThemeMenuItem> footer = new ArrayList<>();
        order = 0;
        for (ApiDtos.NavigationItem page : pages.stream()
                .filter(item -> !"HOME".equalsIgnoreCase(item.role()))
                .limit(6)
                .toList()) {
            footer.add(pageItem(page, order++));
        }
        return new ApiDtos.ThemeMenusResponse(List.copyOf(header), List.copyOf(footer));
    }

    private ApiDtos.ThemeMenuItem pageItem(ApiDtos.NavigationItem page, int order) {
        return new ApiDtos.ThemeMenuItem(
                UUID.randomUUID().toString(),
                page.title(),
                "PAGE",
                page.slug(),
                "",
                true,
                false,
                order);
    }

    private List<ApiDtos.ThemeMenuItem> normalize(
            List<ApiDtos.ThemeMenuItem> source,
            String location) {
        List<ApiDtos.ThemeMenuItem> input = source == null ? List.of() : source;
        if (input.size() > MAX_ITEMS) {
            throw new BadRequestException(
                    "El menú del " + location + " admite hasta " + MAX_ITEMS + " elementos");
        }
        List<ApiDtos.ThemeMenuItem> result = new ArrayList<>();
        int order = 0;
        for (ApiDtos.ThemeMenuItem item : input) {
            if (item == null) continue;
            String label = clean(item.label(), 120, "La etiqueta del menú es obligatoria");
            String type = item.type() == null
                    ? "PAGE"
                    : item.type().trim().toUpperCase(Locale.ROOT);
            if (!type.equals("PAGE") && !type.equals("CUSTOM")) {
                throw new BadRequestException("Tipo de elemento de menú inválido");
            }
            String target = item.target() == null ? "" : item.target().trim();
            String url = item.url() == null ? "" : item.url().trim();
            if (type.equals("PAGE")) {
                if (!SLUG.matcher(target).matches()) {
                    throw new BadRequestException("La página seleccionada para " + label + " no es válida");
                }
                url = "";
            } else {
                if (!SAFE_URL.matcher(url).matches()) {
                    throw new BadRequestException("La URL de " + label + " no es segura o no es válida");
                }
                target = "";
            }
            result.add(new ApiDtos.ThemeMenuItem(
                    item.id() == null || item.id().isBlank()
                            ? UUID.randomUUID().toString()
                            : item.id().trim(),
                    label,
                    type,
                    target,
                    url,
                    item.visible(),
                    item.newTab(),
                    order++));
        }
        return List.copyOf(result);
    }

    private List<ApiDtos.ThemeMenuItem> sorted(List<ApiDtos.ThemeMenuItem> items) {
        return items.stream()
                .sorted(Comparator.comparingInt(ApiDtos.ThemeMenuItem::order))
                .toList();
    }

    private List<ApiDtos.ThemeMenuItem> read(String key) {
        String value = settings.get(key, "");
        if (value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private void persist(ApiDtos.ThemeMenusResponse menus) {
        try {
            settings.put(HEADER_KEY, objectMapper.writeValueAsString(menus.header()));
            settings.put(FOOTER_KEY, objectMapper.writeValueAsString(menus.footer()));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudieron guardar los menús del tema", exception);
        }
    }

    private String clean(String value, int max, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new BadRequestException(message);
        if (normalized.length() > max) {
            throw new BadRequestException("El texto del menú supera " + max + " caracteres");
        }
        return normalized;
    }
}
