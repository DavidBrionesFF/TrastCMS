package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.post.*;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

@Service
public class ThemeStarterContentService {
    private final PostRepository posts;
    private final UserAccountRepository users;
    private final SiteSettingService settings;
    private final ObjectMapper objectMapper;
    private final AuditService audit;
    private final ContentSanitizer sanitizer;

    public ThemeStarterContentService(PostRepository posts, UserAccountRepository users,
                                      SiteSettingService settings, ObjectMapper objectMapper,
                                      AuditService audit, ContentSanitizer sanitizer) {
        this.posts = posts;
        this.users = users;
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.sanitizer = sanitizer;
    }

    @Transactional
    public StarterResult apply(
            String themeId,
            Path customThemeRoot,
            String actorEmail) {
        return applyInternal(
                themeId,
                customThemeRoot,
                actorEmail,
                false);
    }

    @Transactional
    public StarterResult restore(
            String themeId,
            Path customThemeRoot,
            String actorEmail) {
        return applyInternal(
                themeId,
                customThemeRoot,
                actorEmail,
                true);
    }

    private StarterResult applyInternal(
            String themeId,
            Path customThemeRoot,
            String actorEmail,
            boolean overwriteThemePages) {
        StarterContent content = read(themeId, customThemeRoot);
        if (content == null || content.pages() == null) {
            return new StarterResult(0, 0, List.of());
        }

        String contentVersion = content.version() == null
                || content.version().isBlank()
                ? "1"
                : content.version().trim();
        String markerKey = "theme.starterContent." + themeId;
        if (!overwriteThemePages
                && contentVersion.equals(settings.get(markerKey, ""))) {
            return new StarterResult(0, 0, List.of());
        }

        UserAccount author = users.findByEmailIgnoreCase(actorEmail)
                .orElseGet(() -> users.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe un usuario para crear las páginas iniciales")));

        int created = 0;
        int updated = 0;
        List<String> slugs = new ArrayList<>();

        for (StarterPage page : content.pages()) {
            if (page.slug() == null || page.slug().isBlank()) {
                continue;
            }

            Optional<Post> existing = posts.findBySlug(page.slug());
            Post entity;
            if (existing.isEmpty()) {
                entity = new Post();
                entity.setAuthor(author);
                created++;
            } else if (overwriteThemePages
                    && themeId.equals(existing.get().getThemeOrigin())
                    && existing.get().getContentType() == ContentType.PAGE) {
                entity = existing.get();
                updated++;
            } else {
                continue;
            }

            applyPage(entity, page, themeId);
            posts.save(entity);
            slugs.add(page.slug());
        }

        String home = content.homePageSlug();
        if (home != null && !home.isBlank()) {
            settings.put("reading.homePageSlug", home);
        }
        settings.put(markerKey, contentVersion);

        if (created > 0 || updated > 0) {
            audit.record(
                    actorEmail,
                    overwriteThemePages
                            ? "theme.starter_content.restored"
                            : "theme.starter_content.created",
                    "theme",
                    themeId,
                    "created=" + created
                            + ",updated=" + updated
                            + ",slugs=" + String.join(",", slugs));
        }

        return new StarterResult(
                created,
                updated,
                List.copyOf(slugs));
    }

    private void applyPage(
            Post entity,
            StarterPage page,
            String themeId) {
        entity.setTitle(page.title());
        entity.setSlug(page.slug());
        entity.setExcerpt(page.excerpt());
        entity.setBody(sanitizer.html(
                page.body() == null ? "" : page.body()));
        entity.setEditorMode(page.editorMode() == null
                ? EditorMode.VISUAL_BUILDER
                : page.editorMode());
        entity.setBuilderData(sanitizer.json(write(page.builder())));
        entity.setCustomCss(sanitizer.css(
                page.customCss() == null ? "" : page.customCss()));
        entity.setSeoTitle(page.seoTitle());
        entity.setSeoDescription(page.seoDescription());
        entity.setTemplate(page.template() == null
                ? "default"
                : page.template());
        entity.setContentType(ContentType.PAGE);
        entity.setStatus(PostStatus.PUBLISHED);
        entity.setShowInMenu(page.showInMenu());
        entity.setMenuOrder(page.menuOrder());
        entity.setPageRole(page.role());
        entity.setThemeOrigin(themeId);
        entity.setPublishedAt(Instant.now());
    }

    private StarterContent read(String themeId, Path customRoot) {
        try {
            if (customRoot != null) {
                Path file = customRoot.resolve("starter-content.json").normalize();
                if (!file.startsWith(customRoot) || !Files.isRegularFile(file)) return null;
                return objectMapper.readValue(file.toFile(), StarterContent.class);
            }
            ClassPathResource resource = new ClassPathResource("themes/" + themeId + "/starter-content.json");
            if (!resource.exists()) return null;
            try (InputStream input = resource.getInputStream()) {
                return objectMapper.readValue(input, StarterContent.class);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el contenido inicial del tema " + themeId, exception);
        }
    }

    private String write(Map<String, Object> value) {
        try { return objectMapper.writeValueAsString(value == null ? Map.of("version", 1, "blocks", List.of()) : value); }
        catch (IOException exception) { throw new IllegalStateException("No se pudo serializar una página inicial", exception); }
    }

    public record StarterResult(
            int created,
            int updated,
            List<String> slugs) { }
    public record StarterContent(String version, String homePageSlug, List<StarterPage> pages) { }
    public record StarterPage(String title, String slug, String excerpt, String body,
                              EditorMode editorMode, Map<String, Object> builder, String customCss,
                              String seoTitle, String seoDescription, String template,
                              boolean showInMenu, int menuOrder, String role) { }
}
