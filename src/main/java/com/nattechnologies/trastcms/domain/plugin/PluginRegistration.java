package com.nattechnologies.trastcms.domain.plugin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plugin_registration")
public class PluginRegistration {
    @Id @Column(length = 36)
    private String id;
    @Column(name = "plugin_key", nullable = false, unique = true, length = 120)
    private String pluginKey;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 50)
    private String version;
    @Column(length = 1000)
    private String description;
    @Column(length = 160)
    private String author;
    @Column(length = 600)
    private String homepage;
    @Column(name = "base_url", nullable = false, length = 600)
    private String baseUrl;
    @Column(name = "encrypted_secret", nullable = false, length = 1000)
    private String encryptedSecret;
    @Column(name = "subscriptions", columnDefinition = "TEXT")
    private String subscriptions;
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;
    @Column(name = "health_check_path", length = 240)
    private String healthCheckPath = "/health";
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "last_test_status", length = 30)
    private String lastTestStatus = "NEVER";
    @Column(name = "last_test_message", length = 1000)
    private String lastTestMessage;
    @Column(name = "last_test_at")
    private Instant lastTestAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (lastTestStatus == null) lastTestStatus = "NEVER";
        if (healthCheckPath == null || healthCheckPath.isBlank()) healthCheckPath = "/health";
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public String getPluginKey() { return pluginKey; }
    public void setPluginKey(String pluginKey) { this.pluginKey = pluginKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getEncryptedSecret() { return encryptedSecret; }
    public void setEncryptedSecret(String encryptedSecret) { this.encryptedSecret = encryptedSecret; }
    public String getSubscriptions() { return subscriptions; }
    public void setSubscriptions(String subscriptions) { this.subscriptions = subscriptions; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public String getHealthCheckPath() { return healthCheckPath; }
    public void setHealthCheckPath(String healthCheckPath) { this.healthCheckPath = healthCheckPath; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLastTestStatus() { return lastTestStatus; }
    public void setLastTestStatus(String lastTestStatus) { this.lastTestStatus = lastTestStatus; }
    public String getLastTestMessage() { return lastTestMessage; }
    public void setLastTestMessage(String lastTestMessage) { this.lastTestMessage = lastTestMessage; }
    public Instant getLastTestAt() { return lastTestAt; }
    public void setLastTestAt(Instant lastTestAt) { this.lastTestAt = lastTestAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
