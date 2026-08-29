package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.category.CategoryRepository;
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
    private final CategoryRepository categories;
    private final UserAccountRepository users;
    private final SiteSettingService settings;
    private final ObjectMapper objectMapper;
    private final AuditService audit;
    private final ContentSanitizer sanitizer;

    public ThemeStarterContentService(PostRepository posts, CategoryRepository categories,
                                      UserAccountRepository users, SiteSettingService settings,
                                      ObjectMapper objectMapper, AuditService audit,
                                      ContentSanitizer sanitizer) {
        this.posts = posts;
        this.categories = categories;
        this.users = users;
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.sanitizer = sanitizer;
    }

    @Transactional
    public StarterResult apply(String themeId, Path customThemeRoot, String actorEmail) {
        return applyInternal(themeId, customThemeRoot, actorEmail, false);
    }

    @Transactional
    public StarterResult restore(String themeId, Path customThemeRoot, String actorEmail) {
        return applyInternal(themeId, customThemeRoot, actorEmail, true);
    }

    private StarterResult applyInternal(String themeId, Path customThemeRoot,
                                        String actorEmail, boolean overwriteThemeContent) {
        StarterContent content = read(themeId, customThemeRoot);
        if (content == null) return new StarterResult(0, 0, List.of());
        List<StarterPage> starterPages = safe(content.pages());
        List<StarterCategory> starterCategories = safe(content.categories());
        List<StarterPost> starterPosts = safe(content.posts());
        if (starterPages.isEmpty() && starterPosts.isEmpty()) {
            return new StarterResult(0, 0, List.of());
        }

        String contentVersion = content.version() == null || content.version().isBlank()
                ? "1" : content.version().trim();
        String markerKey = "theme.starterContent." + themeId;
        if (!overwriteThemeContent && contentVersion.equals(settings.get(markerKey, ""))) {
            return new StarterResult(0, 0, List.of());
        }

        UserAccount author = users.findByEmailIgnoreCase(actorEmail)
                .orElseGet(() -> users.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe un usuario para crear el contenido inicial")));

        Map<String, Category> categoryMap = prepareCategories(starterCategories);
        int created = 0;
        int updated = 0;
        int hiddenStalePages = hideStaleThemeMenuPages(themeId, starterPages);
        List<String> slugs = new ArrayList<>();

        for (StarterPage page : starterPages) {
            if (blank(page.slug())) continue;
            Optional<Post> existing = posts.findBySlug(page.slug());
            Post entity;
            if (existing.isEmpty()) {
                entity = new Post();
                entity.setAuthor(author);
                applyPage(entity, page, themeId);
                created++;
            } else if (existing.get().getContentType() == ContentType.PAGE
                    && (themeId.equals(existing.get().getThemeOrigin()) || overwriteThemeContent)) {
                entity = existing.get();
                if (overwriteThemeContent) {
                    // Restaurar contenido inicial es una acción administrativa explícita.
                    // También puede reclamar una página con el mismo slug creada por
                    // otro tema, evitando conservar contenido genérico heredado.
                    applyPage(entity, page, themeId);
                } else {
                    applyNavigationMetadata(entity, page);
                    mergeBuilderEvolution(entity, page);
                }
                updated++;
            } else continue;
            posts.save(entity);
            slugs.add(page.slug());
        }

        for (StarterPost starterPost : starterPosts) {
            if (blank(starterPost.slug())) continue;
            Optional<Post> existing = posts.findBySlug(starterPost.slug());
            Post entity;
            if (existing.isEmpty()) {
                entity = new Post();
                entity.setAuthor(author);
                applyPost(entity, starterPost, themeId, categoryMap);
                created++;
            } else if (themeId.equals(existing.get().getThemeOrigin())
                    && existing.get().getContentType() == ContentType.POST) {
                entity = existing.get();
                if (overwriteThemeContent) {
                    applyPost(entity, starterPost, themeId, categoryMap);
                }
                updated++;
            } else continue;
            posts.save(entity);
            slugs.add(starterPost.slug());
        }

        String home = content.homePageSlug();
        if (!blank(home)) settings.put("reading.homePageSlug", home);
        settings.put(markerKey, contentVersion);

        if (created > 0 || updated > 0) {
            audit.record(actorEmail,
                    overwriteThemeContent ? "theme.starter_content.restored" : "theme.starter_content.created",
                    "theme", themeId,
                    "created=" + created + ",updated=" + updated
                            + ",hiddenStaleThemePages=" + hiddenStalePages
                            + ",slugs=" + String.join(",", slugs));
        }
        return new StarterResult(created, updated, List.copyOf(slugs));
    }

    private Map<String, Category> prepareCategories(List<StarterCategory> starterCategories) {
        Map<String, Category> result = new LinkedHashMap<>();
        for (StarterCategory starter : starterCategories) {
            if (blank(starter.slug()) || blank(starter.name())) continue;
            Category category = categories.findBySlug(starter.slug()).orElseGet(Category::new);
            category.setName(starter.name().trim());
            category.setSlug(starter.slug().trim());
            category.setDescription(starter.description());
            result.put(category.getSlug(), categories.save(category));
        }
        return result;
    }

    private int hideStaleThemeMenuPages(String themeId, List<StarterPage> starterPages) {
        Set<String> currentSlugs = safe(starterPages).stream()
                .map(StarterPage::slug).filter(Objects::nonNull).filter(slug -> !slug.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        int hidden = 0;
        for (Post page : posts.findAll()) {
            String origin = page.getThemeOrigin();
            if (page.getContentType() != ContentType.PAGE || !page.isShowInMenu()
                    || origin == null || origin.isBlank()) continue;
            boolean belongsToAnotherTheme = !themeId.equals(origin);
            boolean obsoleteForCurrentTheme = themeId.equals(origin) && !currentSlugs.contains(page.getSlug());
            if (belongsToAnotherTheme || obsoleteForCurrentTheme) {
                page.setShowInMenu(false);
                posts.save(page);
                hidden++;
            }
        }
        return hidden;
    }

    private void applyNavigationMetadata(Post entity, StarterPage page) {
        entity.setShowInMenu(page.showInMenu());
        entity.setMenuOrder(page.menuOrder());
        entity.setPageRole(page.role());
    }

    private void mergeBuilderEvolution(Post entity, StarterPage page) {
        if (entity.getEditorMode() != EditorMode.VISUAL_BUILDER || page.builder() == null) return;
        try {
            Map<String, Object> current = entity.getBuilderData() == null || entity.getBuilderData().isBlank()
                    ? new LinkedHashMap<>()
                    : objectMapper.readValue(entity.getBuilderData(), new TypeReference<LinkedHashMap<String, Object>>() { });
            Map<String, Object> starter = objectMapper.convertValue(page.builder(),
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            current.putIfAbsent("version", starter.getOrDefault("version", 1));
            List<Map<String, Object>> currentBlocks = blockList(current.computeIfAbsent("blocks", ignored -> new ArrayList<>()));
            Set<String> deprecated = new LinkedHashSet<>(safe(page.deprecatedBlockIds()));
            removeDeprecatedBlocks(currentBlocks, deprecated);
            mergeMissingBlocks(currentBlocks, blockList(starter.get("blocks")));
            current.put("blocks", currentBlocks);
            entity.setBuilderData(sanitizer.json(objectMapper.writeValueAsString(current)));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("No se pudo actualizar la estructura visual de " + page.slug(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> blockList(Object value) {
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private void removeDeprecatedBlocks(List<Map<String, Object>> blocks, Set<String> deprecated) {
        if (deprecated.isEmpty()) return;
        blocks.removeIf(block -> deprecated.contains(String.valueOf(block.get("id"))));
        for (Map<String, Object> block : blocks) {
            List<Map<String, Object>> children = blockList(block.get("children"));
            removeDeprecatedBlocks(children, deprecated);
            if (block.containsKey("children")) block.put("children", children);
        }
    }

    private void mergeMissingBlocks(List<Map<String, Object>> current, List<Map<String, Object>> starter) {
        for (int index = 0; index < starter.size(); index++) {
            Map<String, Object> starterBlock = starter.get(index);
            String id = String.valueOf(starterBlock.getOrDefault("id", ""));
            Map<String, Object> existing = current.stream()
                    .filter(block -> id.equals(String.valueOf(block.getOrDefault("id", ""))))
                    .findFirst().orElse(null);
            if (existing == null) {
                current.add(Math.min(index, current.size()), objectMapper.convertValue(starterBlock,
                        new TypeReference<LinkedHashMap<String, Object>>() { }));
                continue;
            }
            List<Map<String, Object>> starterChildren = blockList(starterBlock.get("children"));
            if (!starterChildren.isEmpty()) {
                List<Map<String, Object>> currentChildren = blockList(existing.get("children"));
                mergeMissingBlocks(currentChildren, starterChildren);
                existing.put("children", currentChildren);
            }
        }
    }

    private void applyPage(Post entity, StarterPage page, String themeId) {
        entity.setTitle(page.title());
        entity.setSlug(page.slug());
        entity.setExcerpt(page.excerpt());
        entity.setBody(sanitizer.html(page.body() == null ? "" : page.body()));
        entity.setEditorMode(page.editorMode() == null ? EditorMode.VISUAL_BUILDER : page.editorMode());
        entity.setBuilderData(sanitizer.json(write(page.builder())));
        entity.setCustomCss(sanitizer.css(page.customCss() == null ? "" : page.customCss()));
        entity.setSeoTitle(page.seoTitle());
        entity.setSeoDescription(page.seoDescription());
        entity.setTemplate(page.template() == null ? "default" : page.template());
        entity.setContentType(ContentType.PAGE);
        entity.setStatus(PostStatus.PUBLISHED);
        entity.setShowInMenu(page.showInMenu());
        entity.setMenuOrder(page.menuOrder());
        entity.setPageRole(page.role());
        entity.setThemeOrigin(themeId);
        entity.setPublishedAt(Instant.now());
    }

    private void applyPost(Post entity, StarterPost post, String themeId,
                           Map<String, Category> categoryMap) {
        entity.setTitle(post.title());
        entity.setSlug(post.slug());
        entity.setExcerpt(post.excerpt());
        entity.setBody(sanitizer.html(post.body() == null ? "" : post.body()));
        entity.setEditorMode(EditorMode.RICH_TEXT);
        entity.setBuilderData(sanitizer.json(write(null)));
        entity.setCustomCss("");
        entity.setSeoTitle(post.seoTitle());
        entity.setSeoDescription(post.seoDescription());
        entity.setTemplate(post.template() == null ? "default" : post.template());
        entity.setFeaturedImageUrl(post.featuredImageUrl());
        entity.setContentType(ContentType.POST);
        entity.setStatus(PostStatus.PUBLISHED);
        entity.setShowInMenu(false);
        entity.setMenuOrder(0);
        entity.setPageRole(null);
        entity.setThemeOrigin(themeId);
        entity.setCategory(categoryMap.get(post.categorySlug()));
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
        try {
            return objectMapper.writeValueAsString(value == null
                    ? Map.of("version", 1, "blocks", List.of()) : value);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo serializar el contenido inicial", exception);
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }

    public record StarterResult(int created, int updated, List<String> slugs) { }
    public record StarterContent(String version, String homePageSlug, List<StarterPage> pages,
                                 List<StarterCategory> categories, List<StarterPost> posts) { }
    public record StarterPage(String title, String slug, String excerpt, String body,
                              EditorMode editorMode, Map<String, Object> builder, String customCss,
                              String seoTitle, String seoDescription, String template,
                              boolean showInMenu, int menuOrder, String role,
                              List<String> deprecatedBlockIds) { }
    public record StarterCategory(String name, String slug, String description) { }
    public record StarterPost(String title, String slug, String excerpt, String body,
                              String featuredImageUrl, String categorySlug,
                              String seoTitle, String seoDescription, String template) { }
}
