package com.nattechnologies.trastcms.plugins.translate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

final class TranslationDtos {
    private TranslationDtos() {
    }

    record SettingsResponse(
            boolean enabled,
            String provider,
            String sourceLanguage,
            List<String> enabledLanguages,
            String placement,
            String displayStyle,
            boolean showFlags,
            boolean showNativeNames,
            boolean rememberSelection,
            String buttonLabel,
            int floatingOffsetX,
            int floatingOffsetY,
            List<String> excludedPaths,
            Instant updatedAt) {
    }

    record SettingsRequest(
            boolean enabled,
            @NotBlank @Size(max = 40) String provider,
            @NotBlank @Size(max = 10) String sourceLanguage,
            @NotEmpty @Size(max = 40) List<@NotBlank @Size(max = 10) String> enabledLanguages,
            @NotBlank @Size(max = 40) String placement,
            @NotBlank @Size(max = 40) String displayStyle,
            boolean showFlags,
            boolean showNativeNames,
            boolean rememberSelection,
            @NotBlank @Size(max = 80) String buttonLabel,
            @Min(0) @Max(300) int floatingOffsetX,
            @Min(0) @Max(300) int floatingOffsetY,
            @Size(max = 30) List<@NotBlank @Size(max = 300) String> excludedPaths) {
    }
}
