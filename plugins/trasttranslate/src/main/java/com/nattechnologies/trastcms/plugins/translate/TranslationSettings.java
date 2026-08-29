package com.nattechnologies.trastcms.plugins.translate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "translation_settings")
class TranslationSettings {
    static String currentId() { return com.nattechnologies.trastcms.multisite.SiteContext.siteId(); }

    @Id
    @Column(length = 36)
    String id = currentId();

    @Column(nullable = false)
    boolean enabled = true;

    @Column(name = "provider", nullable = false, length = 40)
    String provider = "GOOGLE_ELEMENT";

    @Column(name = "source_language", nullable = false, length = 10)
    String sourceLanguage = "en";

    @Column(name = "enabled_languages", nullable = false, length = 1000)
    String enabledLanguages = "en,es,fr,de,it,pt,nl";

    @Column(nullable = false, length = 40)
    String placement = "TOPBAR";

    @Column(name = "display_style", nullable = false, length = 40)
    String displayStyle = "COMPACT";

    @Column(name = "show_flags", nullable = false)
    boolean showFlags = true;

    @Column(name = "show_native_names", nullable = false)
    boolean showNativeNames = true;

    @Column(name = "remember_selection", nullable = false)
    boolean rememberSelection = true;

    @Column(name = "button_label", nullable = false, length = 80)
    String buttonLabel = "Language";

    @Column(name = "floating_offset_x", nullable = false)
    int floatingOffsetX = 24;

    @Column(name = "floating_offset_y", nullable = false)
    int floatingOffsetY = 88;

    @Column(name = "excluded_paths", nullable = false, length = 2000)
    String excludedPaths = "/admin,/api";

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
