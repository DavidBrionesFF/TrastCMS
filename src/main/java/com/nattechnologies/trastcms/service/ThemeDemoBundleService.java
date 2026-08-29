package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Restores and optionally exports a theme-owned source snapshot without coupling the core to
 * plugin persistence. Plugin data is restored through each bundled plugin's
 * public admin-action contract; the exported ZIP is assembled only from
 * classpath resources declared by the theme demo manifest.
 */
@Service
public class ThemeDemoBundleService {
    private static final String MANIFEST_TEMPLATE = "themes/%s/data/demo-bundle.json";

    private final ObjectMapper mapper;
    private final ThemeService themes;
    private final BundledPluginService plugins;
    private final SiteSettingService settings;
    private final AuditService audit;
    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    public ThemeDemoBundleService(ObjectMapper mapper,
                                  ThemeService themes,
                                  BundledPluginService plugins,
                                  SiteSettingService settings,
                                  AuditService audit) {
        this.mapper = mapper;
        this.themes = themes;
        this.plugins = plugins;
        this.settings = settings;
        this.audit = audit;
    }

    public DemoBundleStatus status(String themeId) {
        DemoManifest manifest = readManifest(themeId);
        return new DemoBundleStatus(
                manifest.id(), manifest.version(), manifest.source(), manifest.capturedAt(),
                manifest.tours(), manifest.destinations(), manifest.activities(), manifest.products(),
                manifest.pages(), manifest.posts(), manifest.media(),
                manifest.historyAvailable(), manifest.historyEmbedded(),
                activeTheme().equals(themeId));
    }

    public DemoRestoreResult restore(String themeId, String actor) {
        DemoManifest manifest = readManifest(themeId);
        if (!activeTheme().equals(themeId)) {
            throw new BadRequestException("Active primero el tema " + themeId + " en el sitio actual");
        }

        ApiDtos.ThemeStarterContentResponse starter = themes.restoreStarterContent(themeId, actor);
        Map<String, Map<String, Object>> pluginResults = new LinkedHashMap<>();
        for (PluginAction action : safe(manifest.restoreActions())) {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("themeId", themeId);
            input.put("bundleId", manifest.id());
            input.put("bundleVersion", manifest.version());
            pluginResults.put(action.pluginId(), plugins.execute(action.pluginId(), action.action(), input, actor));
        }

        audit.record(actor, "theme.demo_bundle.restored", "theme", themeId,
                "bundle=" + manifest.id() + ",version=" + manifest.version());
        return new DemoRestoreResult(
                themeId, manifest.id(), manifest.version(), starter.created(), starter.updated(),
                starter.slugs(), Map.copyOf(pluginResults));
    }

    public DemoExport export(String themeId, String actor) {
        DemoManifest manifest = readManifest(themeId);
        List<ExportEntry> entries = new ArrayList<>();

        for (DemoResource item : safe(manifest.resources())) {
            String source = normalizeResourcePath(item.source());
            String target = safeZipPath(item.target());
            entries.add(new ExportEntry(target, readBytes(new ClassPathResource(source))));
        }

        for (DemoPattern pattern : safe(manifest.resourcePatterns())) {
            try {
                Resource[] matches = resources.getResources(pattern.pattern());
                for (Resource resource : matches) {
                    if (!resource.exists() || !resource.isReadable() || resource.getFilename() == null) continue;
                    entries.add(new ExportEntry(patternTarget(resource, pattern.sourceRoot(), pattern.targetRoot()), readBytes(resource)));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudieron enumerar los recursos adicionales del paquete demo", exception);
            }
        }

        if (manifest.assetPattern() != null && !manifest.assetPattern().isBlank()) {
            try {
                Resource[] matches = resources.getResources(manifest.assetPattern());
                for (Resource resource : matches) {
                    if (!resource.exists() || !resource.isReadable() || resource.getFilename() == null) continue;
                    entries.add(new ExportEntry(assetTarget(themeId, resource), readBytes(resource)));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudieron enumerar los recursos del paquete demo", exception);
            }
        }

        entries = entries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ExportEntry::path,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values().stream().toList();

        Map<String, Object> generatedManifest = new LinkedHashMap<>();
        generatedManifest.put("format", "trastcms-theme-demo-bundle");
        generatedManifest.put("version", 1);
        generatedManifest.put("themeId", themeId);
        generatedManifest.put("bundleId", manifest.id());
        generatedManifest.put("bundleVersion", manifest.version());
        generatedManifest.put("source", manifest.source());
        generatedManifest.put("capturedAt", manifest.capturedAt());
        generatedManifest.put("exportedAt", Instant.now().toString());
        generatedManifest.put("containsPrivateBookingHistory", false);
        generatedManifest.put("files", entries.stream().map(entry -> Map.of(
                "path", entry.path(),
                "bytes", entry.bytes().length,
                "sha256", sha256(entry.bytes()))).toList());

        byte[] manifestBytes;
        try {
            manifestBytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(generatedManifest);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo serializar el manifiesto del paquete demo", exception);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            writeZipEntry(zip, "bundle-manifest.json", manifestBytes);
            for (ExportEntry entry : entries) writeZipEntry(zip, entry.path(), entry.bytes());
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo construir el paquete demo", exception);
        }

        audit.record(actor, "theme.demo_bundle.exported", "theme", themeId,
                "bundle=" + manifest.id() + ",files=" + (entries.size() + 1));
        return new DemoExport(
                "trastcms-demo-" + themeId + "-" + manifest.version() + ".zip",
                bytes.toByteArray(), entries.size() + 1);
    }

    private DemoManifest readManifest(String themeId) {
        if (themeId == null || !themeId.matches("[a-z0-9][a-z0-9-]{2,63}")) {
            throw new BadRequestException("Identificador de tema inválido");
        }
        ClassPathResource resource = new ClassPathResource(MANIFEST_TEMPLATE.formatted(themeId));
        if (!resource.exists()) throw new NotFoundException("El tema no incluye un paquete de datos demo");
        try (InputStream input = resource.getInputStream()) {
            DemoManifest manifest = mapper.readValue(input, DemoManifest.class);
            if (!Objects.equals(themeId, manifest.themeId())) {
                throw new IllegalStateException("El manifiesto demo no pertenece al tema solicitado");
            }
            return manifest;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el paquete demo del tema " + themeId, exception);
        }
    }

    private String activeTheme() {
        return settings.get("theme.active", "nexus");
    }

    private static byte[] readBytes(Resource resource) {
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Falta un recurso declarado por el paquete demo: " + resource.getDescription());
        }
        try (InputStream input = resource.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + resource.getDescription(), exception);
        }
    }

    private static void writeZipEntry(ZipOutputStream zip, String path, byte[] value) throws IOException {
        ZipEntry entry = new ZipEntry(safeZipPath(path));
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(value);
        zip.closeEntry();
    }

    private static String normalizeResourcePath(String value) {
        String path = value == null ? "" : value.replace('\\', '/').replaceFirst("^classpath:/?", "");
        if (path.isBlank() || path.startsWith("/") || path.contains("..")) {
            throw new IllegalStateException("Ruta de recurso demo inválida: " + value);
        }
        return path;
    }

    private static String safeZipPath(String value) {
        String path = value == null ? "" : value.replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        if (path.isBlank() || path.contains("\u0000")) {
            throw new IllegalStateException("Ruta ZIP demo inválida: " + value);
        }
        for (String segment : path.split("/")) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalStateException("Ruta ZIP demo inválida: " + value);
            }
        }
        return path;
    }

    private static String patternTarget(Resource resource, String sourceRoot, String targetRoot) {
        String root = normalizeResourcePath(sourceRoot);
        if (!root.endsWith("/")) root += "/";
        String target = targetRoot == null ? "" : targetRoot.replace('\\', '/');
        while (target.startsWith("/")) target = target.substring(1);
        while (target.endsWith("/")) target = target.substring(0, target.length() - 1);
        if (!target.isBlank()) target = safeZipPath(target);
        try {
            String location = resource.getURL().toExternalForm().replace('\\', '/');
            int markerIndex = location.indexOf(root);
            if (markerIndex >= 0) {
                String relative = safeZipPath(location.substring(markerIndex + root.length()));
                return target.isBlank() ? relative : target + "/" + relative;
            }
        } catch (IOException ignored) {
            // Fall back to the resource filename when a classpath implementation has no URL.
        }
        String filename = resource.getFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalStateException("No se pudo determinar el nombre de un recurso demo");
        }
        String relative = safeZipPath(filename);
        return target.isBlank() ? relative : target + "/" + relative;
    }

    private static String assetTarget(String themeId, Resource resource) {
        String marker = "themes/" + themeId + "/assets/";
        try {
            String location = resource.getURL().toExternalForm().replace('\\', '/');
            int markerIndex = location.indexOf(marker);
            if (markerIndex >= 0) {
                String relative = location.substring(markerIndex + marker.length());
                return "assets/" + safeZipPath(relative);
            }
        } catch (IOException ignored) {
            // Fall back to the resource filename when a classpath implementation has no URL.
        }
        String filename = resource.getFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalStateException("No se pudo determinar el nombre de un asset demo");
        }
        return "assets/" + safeZipPath(filename);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }

    private record ExportEntry(String path, byte[] bytes) { }

    public record DemoBundleStatus(String id, String version, String source, String capturedAt,
                                   int tours, int destinations, int activities, int products,
                                   int pages, int posts, int media,
                                   boolean historyAvailable, boolean historyEmbedded,
                                   boolean activeTheme) { }

    public record DemoRestoreResult(String themeId, String bundleId, String bundleVersion,
                                    int pagesCreated, int pagesUpdated, List<String> slugs,
                                    Map<String, Map<String, Object>> plugins) { }

    public record DemoExport(String filename, byte[] bytes, int files) { }

    public record DemoManifest(String id, String themeId, String version, String source, String capturedAt,
                               int tours, int destinations, int activities, int products,
                               int pages, int posts, int media,
                               boolean historyAvailable, boolean historyEmbedded,
                               String assetPattern, List<DemoResource> resources,
                               List<DemoPattern> resourcePatterns, List<PluginAction> restoreActions) { }

    public record DemoResource(String source, String target) { }
    public record DemoPattern(String pattern, String sourceRoot, String targetRoot) { }
    public record PluginAction(String pluginId, String action) { }
}
