package com.nattechnologies.trastcms.service;

import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class SlugService {
    public String slugify(String value) {
        if (value == null || value.isBlank()) return "contenido";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "contenido" : normalized;
    }
}
