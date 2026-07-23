package com.nattechnologies.trastcms.domain.plugin;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bundled_plugin_state")
public class BundledPluginState {
    @Id @Column(name = "plugin_id", length = 120)
    private String pluginId;
    @Column(name = "installed_version", nullable = false, length = 50)
    private String installedVersion;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = Instant.now(); }
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }
    public String getInstalledVersion() { return installedVersion; }
    public void setInstalledVersion(String installedVersion) { this.installedVersion = installedVersion; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
