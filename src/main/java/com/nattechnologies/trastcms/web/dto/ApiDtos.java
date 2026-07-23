package com.nattechnologies.trastcms.web.dto;

import com.nattechnologies.trastcms.domain.media.MediaKind;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.domain.post.EditorMode;
import com.nattechnologies.trastcms.domain.post.PostStatus;
import com.nattechnologies.trastcms.domain.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {}

    public record AuthUser(boolean authenticated, String email, String displayName, String role,
                           String avatarUrl, Instant lastLoginAt) {}
    public record Csrf(String token, String headerName, String parameterName) {}

    public record UserRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(max = 120) String displayName,
            @NotNull UserRole role,
            boolean enabled,
            @Size(min = 12, max = 200) String password) {}

    public record UserResponse(
            String id, String email, String displayName, UserRole role,
            boolean enabled, String avatarUrl, Instant lastLoginAt,
            long contentCount, Instant createdAt, Instant updatedAt) {}

    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 200) String newPassword) {}

    public record AccountProfileRequest(
            @NotBlank @Size(max = 120) String displayName,
            @Size(max = 80) String firstName,
            @Size(max = 80) String lastName,
            @Size(max = 60) String phone,
            @Size(max = 600) String avatarUrl,
            @Size(max = 1000) String bio,
            @Pattern(regexp = "[a-z]{2}(?:-(?:[A-Z]{2}|[0-9]{3}))?") String locale,
            @Size(max = 80) String timezone) {}

    public record AccountProfileResponse(
            String id, String email, String displayName, String firstName,
            String lastName, String phone, String avatarUrl, String bio,
            String locale, String timezone, UserRole role, boolean enabled,
            Instant lastLoginAt, Instant createdAt, Instant updatedAt) {}

    public record CategoryRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 180) String slug,
            @Size(max = 500) String description) {}

    public record CategoryResponse(
            String id, String name, String slug, String description,
            Instant createdAt, Instant updatedAt) {}

    public record PostRequest(
            @NotBlank @Size(max = 250) String title,
            @Size(max = 280) String slug,
            @Size(max = 600) String excerpt,
            @NotNull String body,
            EditorMode editorMode,
            String builderData,
            String customCss,
            @Size(max = 250) String seoTitle,
            @Size(max = 500) String seoDescription,
            @Size(max = 120) String template,
            boolean showInMenu,
            @Min(0) @Max(10000) int menuOrder,
            @Size(max = 40) String pageRole,
            ContentType contentType,
            @NotNull PostStatus status,
            @Size(max = 500) String featuredImageUrl,
            String categoryId) {}

    public record PostResponse(
            String id, String title, String slug, String excerpt, String body,
            EditorMode editorMode, String builderData, String customCss,
            String seoTitle, String seoDescription, String template,
            boolean showInMenu, int menuOrder, String pageRole, String themeOrigin,
            ContentType contentType, PostStatus status, String featuredImageUrl,
            CategoryResponse category, String authorName,
            Instant publishedAt, Instant createdAt, Instant updatedAt, long version) {}

    public record PostSummary(
            String id, String title, String slug, String excerpt,
            ContentType contentType, EditorMode editorMode, PostStatus status,
            boolean showInMenu, int menuOrder, String pageRole, String themeOrigin,
            String featuredImageUrl, CategoryResponse category, String authorName,
            Instant publishedAt, Instant updatedAt) {}

    public record RevisionResponse(
            String id, long revisionNumber, String title, String excerpt, String body,
            ContentType contentType, EditorMode editorMode, String builderData, String customCss,
            PostStatus status, String createdBy, Instant createdAt) {}

    public record PageResponse<T>(
            List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record Dashboard(
            long posts, long pages, long published, long drafts,
            long categories, long media, long users) {}

    public record SiteInfo(
            String name, String tagline, String description, String locale,
            String activeTheme, String logoUrl, String homePageSlug,
            boolean showAdminBar, String contactEmail, String contactPhone) {}

    public record NavigationItem(String title, String slug, int order, String role) {}

    public record ThemeMenuItem(
            @Size(max = 80) String id,
            @NotBlank @Size(max = 120) String label,
            @NotBlank @Pattern(regexp = "PAGE|CUSTOM") String type,
            @Size(max = 280) String target,
            @Size(max = 1000) String url,
            boolean visible,
            boolean newTab,
            @Min(0) @Max(10000) int order) {}

    public record ThemeMenusResponse(
            List<ThemeMenuItem> header,
            List<ThemeMenuItem> footer) {}

    public record ThemeMenusRequest(
            List<@Valid ThemeMenuItem> header,
            List<@Valid ThemeMenuItem> footer) {}

    public record SettingsRequest(Map<String, String> values) {}
    public record SettingsResponse(Map<String, String> values) {}

    public record ThemeResponse(
            String id, String name, String description, String version,
            String author, String homepage, String license,
            boolean active, boolean custom, String stylesheetUrl, String screenshotUrl,
            List<String> templates, List<String> features, Map<String, Object> settingsSchema) {}

    public record ActivateThemeRequest(@NotBlank String themeId) {}
    public record ThemeSettingsRequest(Map<String, String> values) {}
    public record ThemeSettingsResponse(String themeId, Map<String, String> values) {}
    public record ThemeStarterContentResponse(
            String themeId,
            int created,
            int updated,
            List<String> slugs) {}

    public record MediaResponse(
            String id, String filename, String originalFilename, String title,
            String altText, String caption, String description, MediaKind kind, String folder,
            String contentType, long sizeBytes, Integer width, Integer height, Double durationSeconds,
            String publicUrl, String uploadedBy, Instant createdAt, Instant updatedAt) {}

    public record MediaUpdateRequest(
            @Size(max = 255) String title,
            @Size(max = 500) String altText,
            @Size(max = 1000) String caption,
            @Size(max = 10000) String description,
            @Size(max = 160) String folder,
            @PositiveOrZero Integer width,
            @PositiveOrZero Integer height,
            @PositiveOrZero Double durationSeconds) {}

    public record PluginRequest(
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9._-]{2,119}") String pluginKey,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 50) String version,
            @Size(max = 1000) String description,
            @Size(max = 160) String author,
            @Size(max = 600) String homepage,
            @NotBlank @Size(max = 600) String baseUrl,
            @Size(min = 16, max = 500) String secret,
            List<String> subscriptions,
            List<String> permissions,
            @Size(max = 240) String healthCheckPath,
            boolean enabled) {}

    public record PluginResponse(
            String id, String pluginKey, String name, String version,
            String description, String author, String homepage, String baseUrl,
            List<String> subscriptions, List<String> permissions, String healthCheckPath,
            boolean enabled, String lastTestStatus, String lastTestMessage, Instant lastTestAt,
            Instant createdAt, Instant updatedAt) {}

    public record JavaPluginResponse(
            String pluginId, String name, String version, String provider,
            String description, String state, String path,
            List<String> dependencies, List<String> capabilities,
            boolean deletable, boolean javaPluginsEnabled) {}

    public record PluginCatalogResponse(
            List<String> events, List<String> permissions,
            boolean javaPluginsEnabled, boolean nativeImage) {}

    public record PluginContributionsResponse(
            List<Map<String, Object>> blocks,
            List<Map<String, Object>> adminMenuItems) {}

    public record BundledPluginResponse(
            String pluginId, String name, String version, String description,
            String author, boolean enabled, boolean enabledByDefault,
            List<String> capabilities, Instant updatedAt) {}

    public record ApiMessage(String message) {}
}
