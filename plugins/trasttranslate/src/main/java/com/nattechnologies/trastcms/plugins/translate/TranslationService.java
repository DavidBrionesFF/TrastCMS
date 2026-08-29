package com.nattechnologies.trastcms.plugins.translate;

import com.nattechnologies.trastcms.service.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
class TranslationService {
    private static final Set<String> PROVIDERS = Set.of("GOOGLE_ELEMENT", "GOOGLE_REDIRECT");
    private static final Set<String> PLACEMENTS = Set.of(
            "TOPBAR", "HEADER", "FOOTER", "FLOATING_LEFT", "FLOATING_RIGHT", "BLOCK_ONLY");
    private static final Set<String> DISPLAY_STYLES = Set.of("COMPACT", "DROPDOWN", "BUTTONS");

    private final TranslationSettingsRepository repository;

    TranslationService(TranslationSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    void initializeDefaults() {
        settings();
    }

    @Transactional(readOnly = true)
    TranslationDtos.SettingsResponse publicSettings() {
        return response(repository.findById(TranslationSettings.currentId())
                .orElseGet(this::defaults));
    }

    @Transactional
    TranslationDtos.SettingsResponse settingsResponse() {
        return response(settings());
    }

    @Transactional
    TranslationDtos.SettingsResponse save(TranslationDtos.SettingsRequest request) {
        String provider = enumValue(request.provider(), PROVIDERS, "proveedor");
        String placement = enumValue(request.placement(), PLACEMENTS, "ubicación");
        String displayStyle = enumValue(request.displayStyle(), DISPLAY_STYLES, "estilo");
        String sourceLanguage = language(request.sourceLanguage());

        LinkedHashSet<String> languages = new LinkedHashSet<>();
        languages.add(sourceLanguage);
        request.enabledLanguages().forEach(value -> languages.add(language(value)));
        if (languages.size() < 2) {
            throw new BadRequestException("Seleccione al menos un idioma adicional al idioma original");
        }

        TranslationSettings entity = settings();
        entity.enabled = request.enabled();
        entity.provider = provider;
        entity.sourceLanguage = sourceLanguage;
        entity.enabledLanguages = String.join(",", languages);
        entity.placement = placement;
        entity.displayStyle = displayStyle;
        entity.showFlags = request.showFlags();
        entity.showNativeNames = request.showNativeNames();
        entity.rememberSelection = request.rememberSelection();
        entity.buttonLabel = request.buttonLabel().trim();
        entity.floatingOffsetX = request.floatingOffsetX();
        entity.floatingOffsetY = request.floatingOffsetY();
        entity.excludedPaths = request.excludedPaths() == null
                ? ""
                : request.excludedPaths().stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(","));
        return response(repository.save(entity));
    }

    private TranslationSettings settings() {
        return repository.findById(TranslationSettings.currentId())
                .orElseGet(() -> repository.save(defaults()));
    }

    private TranslationSettings defaults() {
        return new TranslationSettings();
    }

    private TranslationDtos.SettingsResponse response(TranslationSettings entity) {
        return new TranslationDtos.SettingsResponse(
                entity.enabled,
                entity.provider,
                entity.sourceLanguage,
                list(entity.enabledLanguages),
                entity.placement,
                entity.displayStyle,
                entity.showFlags,
                entity.showNativeNames,
                entity.rememberSelection,
                entity.buttonLabel,
                entity.floatingOffsetX,
                entity.floatingOffsetY,
                list(entity.excludedPaths),
                entity.updatedAt);
    }

    private List<String> list(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String language(String value) {
        String normalized = value == null ? "" : value.trim().replace('_', '-');
        if (!normalized.matches("(?i)[a-z]{2,3}(-[a-z]{2,4})?")) {
            throw new BadRequestException("Código de idioma no válido: " + value);
        }
        String[] parts = normalized.split("-", 2);
        if (parts.length == 1) return parts[0].toLowerCase(Locale.ROOT);
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    private String enumValue(String value, Set<String> allowed, String label) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BadRequestException("Valor de " + label + " no válido");
        }
        return normalized;
    }
}
