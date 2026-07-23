package com.nattechnologies.trastcms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ThemeService {
    private static final Pattern THEME_ID =
            Pattern.compile("[a-z0-9][a-z0-9-]{2,63}");
    private static final Pattern REMOTE_CSS = Pattern.compile(
            "url\\s*\\(\\s*['\"]?\\s*(?:javascript:)",
            Pattern.CASE_INSENSITIVE);
    private static final long MAX_UNCOMPRESSED_BYTES =
            25L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 500;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".ico",
            ".woff", ".woff2", ".ttf", ".otf",
            ".html", ".css", ".json", ".txt", ".md");

    private static final Map<String, Object> DEFAULT_SETTINGS = Map.of(
            "primaryColor", Map.of(
                    "type", "color",
                    "label", "Color principal",
                    "default", "#6d4aff"),
            "fontFamily", Map.of(
                    "type", "select",
                    "label", "Tipografía",
                    "default", "outfit",
                    "options", List.of("outfit", "system", "serif")),
            "containerWidth", Map.of(
                    "type", "number",
                    "label", "Ancho máximo",
                    "default", "1180"));

    private static final List<ThemeManifest> BUILT_INS = List.of(
            new ThemeManifest(
                    "aurora",
                    "Aurora",
                    "Tema editorial claro para empresas, portafolios y blogs.",
                    "2.1.0",
                    "NaT Technologies",
                    "https://nattechnologiesagency.com",
                    "GPL-3.0",
                    "screenshot.svg",
                    List.of("default", "full-width", "landing"),
                    List.of("responsive", "blog", "pages", "menus", "dark-mode"),
                    DEFAULT_SETTINGS),
            new ThemeManifest(
                    "midnight",
                    "Midnight",
                    "Tema oscuro de alto contraste para productos tecnológicos.",
                    "2.1.0",
                    "NaT Technologies",
                    "https://nattechnologiesagency.com",
                    "GPL-3.0",
                    "screenshot.svg",
                    List.of("default", "full-width", "landing"),
                    List.of("responsive", "dark", "portfolio", "pages"),
                    DEFAULT_SETTINGS),
            new ThemeManifest(
                    "coffee",
                    "Coffee",
                    "Tema cálido para cafeterías, fincas y marcas artesanales.",
                    "2.1.0",
                    "NaT Technologies",
                    "https://nattechnologiesagency.com",
                    "GPL-3.0",
                    "screenshot.svg",
                    List.of("default", "full-width", "menu", "landing"),
                    List.of("responsive", "commerce-ready", "gallery", "pages"),
                    DEFAULT_SETTINGS));

    private final SiteSettingService settings;
    private final AuditService audit;
    private final ObjectMapper objectMapper;
    private final PluginEventDispatcher events;
    private final ThemeStarterContentService starterContent;
    private final Path themesDirectory;

    public ThemeService(
            SiteSettingService settings,
            AuditService audit,
            ObjectMapper objectMapper,
            TrastCmsProperties properties,
            PluginEventDispatcher events,
            ThemeStarterContentService starterContent) {
        this.settings = settings;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.events = events;
        this.starterContent = starterContent;
        this.themesDirectory = Paths.get(
                        properties.getDataDir(), "themes")
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(themesDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de temas", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ThemeResponse> list() {
        String active = settings.get("theme.active", "aurora");
        List<ApiDtos.ThemeResponse> result = new ArrayList<>();
        BUILT_INS.forEach(theme ->
                result.add(toResponse(theme, active, false)));
        installedThemes().stream()
                .sorted(Comparator.comparing(
                        ThemeManifest::name,
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(theme ->
                        result.add(toResponse(theme, active, true)));
        return result;
    }

    @Transactional
    public ApiDtos.ThemeStarterContentResponse restoreStarterContent(
            String themeId,
            String actor) {
        ThemeManifest theme = requireManifest(themeId);
        ThemeStarterContentService.StarterResult result =
                starterContent.restore(
                        theme.id(),
                        isBuiltIn(theme.id())
                                ? null
                                : customThemePath(theme.id()),
                        actor);
        events.publish(PluginEvent.of(
                "theme.starter_content.restored",
                Map.of(
                        "id", theme.id(),
                        "created", result.created(),
                        "updated", result.updated(),
                        "slugs", result.slugs())));
        return new ApiDtos.ThemeStarterContentResponse(
                theme.id(),
                result.created(),
                result.updated(),
                result.slugs());
    }

    @Transactional
    public ApiDtos.ThemeResponse activate(
            String themeId,
            String actor) {
        ThemeManifest theme = requireManifest(themeId);
        settings.put("theme.active", theme.id());
        starterContent.apply(
                theme.id(),
                isBuiltIn(theme.id()) ? null : customThemePath(theme.id()),
                actor);
        audit.record(
                actor,
                "theme.activated",
                "theme",
                theme.id(),
                theme.name());
        events.publish(PluginEvent.of("theme.activated", Map.of(
                "id", theme.id(),
                "name", theme.name(),
                "version", theme.version())));
        return toResponse(
                theme,
                theme.id(),
                !isBuiltIn(theme.id()));
    }

    @Transactional
    public ApiDtos.ThemeResponse install(
            MultipartFile archive,
            String actor) {
        if (archive == null || archive.isEmpty()) {
            throw new BadRequestException(
                    "Debe seleccionar un tema ZIP");
        }

        String original = Optional.ofNullable(
                        archive.getOriginalFilename())
                .orElse("");
        if (!original.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BadRequestException(
                    "El tema debe enviarse como archivo ZIP");
        }

        Path staging = null;
        try {
            staging = Files.createTempDirectory(
                    themesDirectory, ".install-");
            extractSafely(archive, staging);
            Path packageRoot = locatePackageRoot(staging);
            ThemeManifest manifest = readManifest(
                    packageRoot.resolve("theme.json"));

            validateManifest(manifest);
            validatePackageContents(packageRoot);

            if (isBuiltIn(manifest.id())) {
                throw new ConflictException(
                        "No se puede reemplazar un tema incorporado");
            }

            Path css = packageRoot.resolve("tokens.css");
            if (!Files.isRegularFile(css)) {
                throw new BadRequestException(
                        "El tema debe incluir tokens.css");
            }
            validateCss(css);

            Path target = themesDirectory
                    .resolve(manifest.id())
                    .normalize();
            if (!target.startsWith(themesDirectory)) {
                throw new BadRequestException(
                        "Identificador de tema inválido");
            }

            deleteTree(target);
            Files.move(
                    packageRoot,
                    target,
                    StandardCopyOption.REPLACE_EXISTING);
            if (!staging.equals(packageRoot)) {
                deleteTree(staging);
            }

            audit.record(
                    actor,
                    "theme.installed",
                    "theme",
                    manifest.id(),
                    manifest.name() + " " + manifest.version());

            String active = settings.get("theme.active", "aurora");
            return toResponse(manifest, active, true);
        } catch (BadRequestException | ConflictException exception) {
            deleteTreeQuietly(staging);
            throw exception;
        } catch (IOException exception) {
            deleteTreeQuietly(staging);
            throw new IllegalStateException(
                    "No se pudo instalar el tema", exception);
        }
    }

    @Transactional
    public void delete(String themeId, String actor) {
        if (isBuiltIn(themeId)) {
            throw new BadRequestException(
                    "Los temas incorporados no pueden eliminarse");
        }
        if (themeId.equals(settings.get("theme.active", "aurora"))) {
            throw new ConflictException(
                    "Active otro tema antes de eliminar este paquete");
        }

        Path target = customThemePath(themeId);
        if (!Files.isDirectory(target)) {
            throw new NotFoundException("Tema no encontrado");
        }

        deleteTree(target);
        audit.record(
                actor,
                "theme.deleted",
                "theme",
                themeId,
                themeId);
    }

    @Transactional(readOnly = true)
    public ApiDtos.ThemeSettingsResponse settings(String themeId) {
        requireManifest(themeId);
        String raw = settings.get(
                "theme.settings." + themeId,
                "{}");
        try {
            Map<String, String> values = objectMapper.readValue(
                    raw,
                    new TypeReference<>() {});
            return new ApiDtos.ThemeSettingsResponse(
                    themeId,
                    values);
        } catch (IOException exception) {
            return new ApiDtos.ThemeSettingsResponse(
                    themeId,
                    Map.of());
        }
    }

    @Transactional
    public ApiDtos.ThemeSettingsResponse updateSettings(
            String themeId,
            Map<String, String> values,
            String actor) {
        ThemeManifest manifest = requireManifest(themeId);
        if (values == null) {
            throw new BadRequestException(
                    "No se recibieron opciones del tema");
        }

        Set<String> allowed = manifest.settingsSchema() == null
                ? Set.of()
                : manifest.settingsSchema().keySet();
        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                throw new BadRequestException(
                        "Opción de tema no permitida: " + entry.getKey());
            }
            normalized.put(
                    entry.getKey(),
                    validateSettingValue(
                            entry.getKey(),
                            entry.getValue(),
                            manifest.settingsSchema().get(entry.getKey())));
        }

        try {
            settings.put(
                    "theme.settings." + themeId,
                    objectMapper.writeValueAsString(normalized));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudieron guardar las opciones del tema",
                    exception);
        }

        audit.record(
                actor,
                "theme.settings.updated",
                "theme",
                themeId,
                String.join(",", normalized.keySet()));

        return new ApiDtos.ThemeSettingsResponse(
                themeId,
                normalized);
    }

    public ThemeFile stylesheet(String themeId) {
        ThemeManifest manifest = requireManifest(themeId);
        Resource base;
        if (isBuiltIn(themeId)) {
            base = new ClassPathResource(
                    "themes/" + themeId + "/tokens.css");
            if (!base.exists()) {
                throw new NotFoundException(
                        "Hoja de estilo no encontrada");
            }
        } else {
            Path root = customThemePath(themeId);
            Path css = root.resolve("tokens.css").normalize();
            if (!css.startsWith(root) || !Files.isRegularFile(css)) {
                throw new NotFoundException(
                        "Hoja de estilo no encontrada");
            }
            base = new FileSystemResource(css);
        }

        try (InputStream input = base.getInputStream()) {
            String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String customized = css + renderSettingOverrides(
                    themeId,
                    manifest,
                    settings(themeId).values());
            return new ThemeFile(
                    new ByteArrayResource(customized.getBytes(StandardCharsets.UTF_8)),
                    "text/css");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar la hoja de estilo del tema",
                    exception);
        }
    }

    public ThemeFile screenshot(String themeId) {
        ThemeManifest manifest = requireManifest(themeId);
        String screenshot = manifest.screenshot() == null
                || manifest.screenshot().isBlank()
                ? "screenshot.svg"
                : manifest.screenshot();

        if (screenshot.contains("..")) {
            throw new BadRequestException(
                    "Ruta de captura inválida");
        }

        if (isBuiltIn(themeId)) {
            Resource resource = new ClassPathResource(
                    "themes/" + themeId + "/assets/" + screenshot);
            if (!resource.exists()) {
                throw new NotFoundException(
                        "Captura del tema no encontrada");
            }
            return new ThemeFile(
                    resource,
                    contentType(screenshot));
        }

        Path root = customThemePath(themeId);
        Path file = root.resolve(screenshot).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            file = root.resolve("assets")
                    .resolve(screenshot)
                    .normalize();
        }
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new NotFoundException(
                    "Captura del tema no encontrada");
        }

        return new ThemeFile(
                new FileSystemResource(file),
                contentType(screenshot));
    }

    public ThemeFile asset(String themeId, String relativePath) {
        String clean = relativePath == null
                ? ""
                : relativePath.replaceFirst("^/+", "");

        if (clean.isBlank() || clean.contains("..")) {
            throw new BadRequestException(
                    "Ruta de recurso inválida");
        }

        if (isBuiltIn(themeId)) {
            Resource resource = new ClassPathResource(
                    "themes/" + themeId + "/assets/" + clean);
            if (!resource.exists()) {
                throw new NotFoundException(
                        "Recurso de tema no encontrado");
            }
            return new ThemeFile(
                    resource,
                    contentType(clean));
        }

        Path base = customThemePath(themeId)
                .resolve("assets")
                .normalize();
        Path file = base.resolve(clean).normalize();

        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw new NotFoundException(
                    "Recurso de tema no encontrado");
        }

        return new ThemeFile(
                new FileSystemResource(file),
                contentType(clean));
    }

    private void extractSafely(
            MultipartFile archive,
            Path staging) throws IOException {
        int entries = 0;
        long total = 0;
        byte[] buffer = new byte[8192];

        try (ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(
                        archive.getInputStream()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new BadRequestException(
                            "El tema contiene demasiados archivos");
                }

                String entryName = entry.getName()
                        .replace('\\', '/');
                Path relative = Paths.get(entryName).normalize();

                if (relative.isAbsolute()
                        || relative.startsWith("..")) {
                    throw new BadRequestException(
                            "El ZIP contiene rutas inválidas");
                }

                Path destination = staging
                        .resolve(relative)
                        .normalize();

                if (!destination.startsWith(staging)) {
                    throw new BadRequestException(
                            "El ZIP contiene rutas inválidas");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.createDirectories(destination.getParent());
                try (OutputStream output = Files.newOutputStream(
                        destination,
                        StandardOpenOption.CREATE_NEW)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_UNCOMPRESSED_BYTES) {
                            throw new BadRequestException(
                                    "El tema supera 25 MB descomprimido");
                        }
                        output.write(buffer, 0, read);
                    }
                }
            }
        }

        if (entries == 0) {
            throw new BadRequestException(
                    "El ZIP del tema está vacío");
        }
    }

    private Path locatePackageRoot(Path staging)
            throws IOException {
        if (Files.isRegularFile(staging.resolve("theme.json"))) {
            return staging;
        }

        try (Stream<Path> paths = Files.list(staging)) {
            List<Path> candidates = paths
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(
                            path.resolve("theme.json")))
                    .toList();
            if (candidates.size() == 1) {
                return candidates.getFirst();
            }
        }

        throw new BadRequestException(
                "No se encontró theme.json en la raíz del paquete");
    }

    private List<ThemeManifest> installedThemes() {
        if (!Files.isDirectory(themesDirectory)) {
            return List.of();
        }

        List<ThemeManifest> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(themesDirectory)) {
            paths.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName()
                            .toString()
                            .startsWith(".install-"))
                    .forEach(path -> {
                        try {
                            ThemeManifest manifest = readManifest(
                                    path.resolve("theme.json"));
                            validateManifest(manifest);
                            result.add(manifest);
                        } catch (RuntimeException exception) {
                            // El paquete inválido no se expone.
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudieron leer los temas instalados",
                    exception);
        }
        return result;
    }

    private ThemeManifest requireManifest(String themeId) {
        validateThemeId(themeId);
        return BUILT_INS.stream()
                .filter(theme -> theme.id().equals(themeId))
                .findFirst()
                .orElseGet(() -> {
                    Path manifestPath = customThemePath(themeId)
                            .resolve("theme.json");
                    if (!Files.isRegularFile(manifestPath)) {
                        throw new NotFoundException(
                                "Tema no encontrado");
                    }
                    return readManifest(manifestPath);
                });
    }

    private ThemeManifest readManifest(Path path) {
        try {
            return objectMapper.readValue(
                    path.toFile(),
                    ThemeManifest.class);
        } catch (IOException exception) {
            throw new BadRequestException(
                    "theme.json no es válido");
        }
    }

    private void validatePackageContents(Path packageRoot)
            throws IOException {
        try (Stream<Path> paths = Files.walk(packageRoot)) {
            for (Path file : paths
                    .filter(Files::isRegularFile)
                    .toList()) {
                Path relative = packageRoot.relativize(file);
                String normalized = relative
                        .toString()
                        .replace('\\', '/');

                if (normalized.equals("theme.json")
                        || normalized.equals("tokens.css")) {
                    continue;
                }

                String lower = normalized.toLowerCase(Locale.ROOT);
                boolean allowed = ALLOWED_EXTENSIONS.stream()
                        .anyMatch(lower::endsWith);

                if (!allowed) {
                    throw new BadRequestException(
                            "Tipo de recurso no permitido en el tema: "
                                    + normalized);
                }

                if (!(normalized.startsWith("assets/")
                        || normalized.startsWith("templates/")
                        || normalized.startsWith("components/")
                        || normalized.startsWith("patterns/")
                        || normalized.startsWith("languages/")
                        || normalized.startsWith("screenshot."))) {
                    throw new BadRequestException(
                            "Ruta no permitida en el tema: "
                                    + normalized);
                }
            }
        }
    }

    private void validateManifest(ThemeManifest manifest) {
        if (manifest == null) {
            throw new BadRequestException(
                    "theme.json no es válido");
        }

        validateThemeId(manifest.id());

        if (manifest.name() == null
                || manifest.name().isBlank()
                || manifest.name().length() > 120) {
            throw new BadRequestException(
                    "El nombre del tema es obligatorio y admite "
                            + "hasta 120 caracteres");
        }

        if (manifest.version() == null
                || manifest.version().isBlank()
                || manifest.version().length() > 50) {
            throw new BadRequestException(
                    "La versión del tema es obligatoria");
        }

        if (manifest.description() != null
                && manifest.description().length() > 1000) {
            throw new BadRequestException(
                    "La descripción del tema admite "
                            + "hasta 1000 caracteres");
        }
    }

    private void validateThemeId(String themeId) {
        if (themeId == null
                || !THEME_ID.matcher(themeId).matches()) {
            throw new BadRequestException(
                    "El identificador del tema debe usar "
                            + "minúsculas, números y guiones");
        }
    }

    private void validateCss(Path css) throws IOException {
        long size = Files.size(css);
        if (size == 0 || size > 512L * 1024L) {
            throw new BadRequestException(
                    "tokens.css debe medir entre 1 byte y 512 KB");
        }

        String value = Files.readString(
                css,
                StandardCharsets.UTF_8);

        if (value.toLowerCase(Locale.ROOT).contains("@import")
                || REMOTE_CSS.matcher(value).find()) {
            throw new BadRequestException(
                    "El tema contiene CSS no permitido");
        }
    }

    private String validateSettingValue(
            String key,
            String raw,
            Object schemaValue) {
        String value = raw == null ? "" : raw.trim();
        if (!(schemaValue instanceof Map<?, ?> schema)) {
            return safeCssText(key, value, 500);
        }

        String type = String.valueOf(schema.containsKey("type") ? schema.get("type") : "text");
        return switch (type) {
            case "color" -> {
                if (!value.matches("#[0-9a-fA-F]{3,8}|(?:rgb|hsl)a?\\([^;{}]{1,80}\\)")) {
                    throw new BadRequestException(
                            "Color inválido para " + key);
                }
                yield value;
            }
            case "number" -> {
                try {
                    double number = Double.parseDouble(value);
                    double min = numericSchema(schema.get("min"), -1_000_000d);
                    double max = numericSchema(schema.get("max"), 1_000_000d);
                    if (!Double.isFinite(number) || number < min || number > max) {
                        throw new BadRequestException(
                                "Valor numérico fuera de rango para " + key);
                    }
                    yield number == Math.rint(number)
                            ? Long.toString((long) number)
                            : Double.toString(number);
                } catch (NumberFormatException exception) {
                    throw new BadRequestException(
                            "Valor numérico inválido para " + key);
                }
            }
            case "boolean" -> {
                if (!value.equals("true") && !value.equals("false")) {
                    throw new BadRequestException(
                            "Valor booleano inválido para " + key);
                }
                yield value;
            }
            case "select" -> {
                Object optionsValue = schema.get("options");
                List<String> options = optionsValue instanceof Collection<?> collection
                        ? collection.stream()
                            .map(option -> option instanceof Map<?, ?> map
                                    ? String.valueOf(map.get("value"))
                                    : String.valueOf(option))
                            .toList()
                        : List.of();
                if (!options.contains(value)) {
                    throw new BadRequestException(
                            "Opción inválida para " + key);
                }
                yield value;
            }
            case "textarea" -> safeCssText(key, value, 2_000);
            default -> safeCssText(key, value, 500);
        };
    }

    private String safeCssText(String key, String value, int maxLength) {
        if (value.length() > maxLength
                || value.contains("{")
                || value.contains("}")
                || value.contains(";")
                || value.toLowerCase(Locale.ROOT).contains("javascript:")) {
            throw new BadRequestException(
                    "Valor no permitido para " + key);
        }
        return value;
    }

    private double numericSchema(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return value == null
                    ? fallback
                    : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String renderSettingOverrides(
            String themeId,
            ThemeManifest manifest,
            Map<String, String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder css = new StringBuilder("\n:root[data-theme=\"")
                .append(themeId)
                .append("\"] {");

        appendVariable(css, "--primary", values.get("primaryColor"));
        appendVariable(css, "--primary-strong", values.get("primaryStrongColor"));
        String width = values.get("containerWidth");
        if (width != null && !width.isBlank()) {
            appendVariable(css, "--site-container-width", width + "px");
        }
        String font = values.get("fontFamily");
        if (font != null) {
            appendVariable(css, "--public-font-family", switch (font) {
                case "outfit" -> "\"Outfit Variable\", Outfit, ui-sans-serif, system-ui, sans-serif";
                case "serif" -> "Georgia, Cambria, \"Times New Roman\", serif";
                default -> "ui-sans-serif, system-ui, sans-serif";
            });
        }
        css.append("}\n");
        return css.toString();
    }

    private void appendVariable(
            StringBuilder css,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            css.append(name).append(':').append(value).append(';');
        }
    }

    private Path customThemePath(String themeId) {
        validateThemeId(themeId);
        Path result = themesDirectory
                .resolve(themeId)
                .normalize();

        if (!result.startsWith(themesDirectory)) {
            throw new BadRequestException(
                    "Ruta de tema inválida");
        }
        return result;
    }

    private boolean isBuiltIn(String id) {
        return BUILT_INS.stream()
                .anyMatch(theme -> theme.id().equals(id));
    }

    private ApiDtos.ThemeResponse toResponse(
            ThemeManifest theme,
            String active,
            boolean custom) {
        return new ApiDtos.ThemeResponse(
                theme.id(),
                theme.name(),
                Optional.ofNullable(theme.description()).orElse(""),
                theme.version(),
                Optional.ofNullable(theme.author()).orElse(""),
                Optional.ofNullable(theme.homepage()).orElse(""),
                Optional.ofNullable(theme.license()).orElse(""),
                theme.id().equals(active),
                custom,
                "/api/public/themes/" + theme.id() + "/tokens.css",
                "/api/public/themes/" + theme.id() + "/screenshot",
                theme.templates() == null
                        ? List.of("default")
                        : theme.templates(),
                theme.features() == null
                        ? List.of()
                        : theme.features(),
                theme.settingsSchema() == null
                        ? Map.of()
                        : theme.settingsSchema());
    }

    private String contentType(String filename) {
        try {
            String detected = Files.probeContentType(
                    Path.of(filename));
            return detected == null
                    ? "application/octet-stream"
                    : detected;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    private void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;

        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalStateException(
                    "No se pudo eliminar el paquete del tema",
                    exception);
        }
    }

    private void deleteTreeQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (RuntimeException ignored) {
        }
    }

    public record ThemeFile(
            Resource resource,
            String contentType) {}

    public record ThemeManifest(
            String id,
            String name,
            String description,
            String version,
            String author,
            String homepage,
            String license,
            String screenshot,
            List<String> templates,
            List<String> features,
            Map<String, Object> settingsSchema) {}
}
