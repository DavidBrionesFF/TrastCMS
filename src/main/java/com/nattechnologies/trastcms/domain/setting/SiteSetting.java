package com.nattechnologies.trastcms.domain.setting;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "site_setting")
public class SiteSetting {
    @Id
    @Column(name = "setting_key", length = 190)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public SiteSetting() {}
    public SiteSetting(String key, String value) { this.key = key; this.value = value; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Instant getUpdatedAt() { return updatedAt; }
}
