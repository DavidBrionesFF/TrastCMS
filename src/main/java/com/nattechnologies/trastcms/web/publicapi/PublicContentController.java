package com.nattechnologies.trastcms.web.publicapi;

import com.nattechnologies.trastcms.domain.media.MediaKind;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.service.*;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicContentController {
    private final PostService posts;
    private final CategoryService categories;
    private final SiteSettingService settings;
    private final MediaService media;
    private final ThemeService themes;
    private final ThemeMenuService menus;
    private final BundledPluginService bundledPlugins;

    public PublicContentController(
            PostService posts,
            CategoryService categories,
            SiteSettingService settings,
            MediaService media,
            ThemeService themes,
            ThemeMenuService menus,
            BundledPluginService bundledPlugins) {
        this.posts = posts;
        this.categories = categories;
        this.settings = settings;
        this.media = media;
        this.themes = themes;
        this.menus = menus;
        this.bundledPlugins = bundledPlugins;
    }

    @GetMapping("/navigation")
    public List<ApiDtos.NavigationItem> navigation() { return posts.navigation(); }

    @GetMapping("/menus")
    public ApiDtos.ThemeMenusResponse menus() {
        return menus.get();
    }

    @GetMapping("/site")
    public ApiDtos.SiteInfo site() {
        return settings.siteInfo();
    }

    @GetMapping("/plugins")
    public Map<String, Boolean> plugins() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        bundledPlugins.list().forEach(plugin ->
                result.put(plugin.pluginId(), plugin.enabled()));
        return Map.copyOf(result);
    }

    @GetMapping("/posts")
    public ApiDtos.PageResponse<ApiDtos.PostSummary> posts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return posts.publicList(search, page, size);
    }

    @GetMapping("/posts/{slug}")
    public ApiDtos.PostResponse post(@PathVariable String slug) {
        return posts.findPublicBySlug(slug, ContentType.POST);
    }

    @GetMapping("/pages/{slug}")
    public ApiDtos.PostResponse page(@PathVariable String slug) {
        return posts.findPublicBySlug(slug, ContentType.PAGE);
    }

    @GetMapping("/categories")
    public List<ApiDtos.CategoryResponse> categories() {
        return categories.list();
    }

    @GetMapping(value = "/themes/{id}/tokens.css", produces = "text/css")
    public ResponseEntity<Resource> themeStyles(@PathVariable String id) {
        ThemeService.ThemeFile file = themes.stylesheet(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(file.resource());
    }

    @GetMapping("/themes/{id}/screenshot")
    public ResponseEntity<Resource> themeScreenshot(
            @PathVariable String id) {
        ThemeService.ThemeFile file = themes.screenshot(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(file.resource());
    }

    @GetMapping("/themes/{id}/assets/{*path}")
    public ResponseEntity<Resource> themeAsset(
            @PathVariable String id,
            @PathVariable String path) {
        ThemeService.ThemeFile file = themes.asset(id, path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(file.resource());
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<?> media(
            @PathVariable String id,
            @RequestHeader HttpHeaders headers) throws IOException {
        MediaService.MediaFile file = media.load(id);
        MediaType contentType;

        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (InvalidMediaTypeException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        boolean streamable = file.kind() == MediaKind.VIDEO
                || file.kind() == MediaKind.AUDIO;
        List<HttpRange> ranges = headers.getRange();

        if (streamable && !ranges.isEmpty()) {
            Resource resource = file.resource();
            ResourceRegion region = ranges.getFirst()
                    .toResourceRegion(resource);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(contentType)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(region);
        }

        ContentDisposition disposition = contentType.equals(MediaType.APPLICATION_PDF)
                ? ContentDisposition.attachment()
                    .filename(file.originalFilename())
                    .build()
                : ContentDisposition.inline()
                    .filename(file.originalFilename())
                    .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(file.resource().contentLength())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(file.resource());
    }
}
