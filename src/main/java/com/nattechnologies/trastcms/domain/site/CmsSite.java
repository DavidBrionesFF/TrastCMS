package com.nattechnologies.trastcms.domain.site;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cms_site")
public class CmsSite {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "site_key", nullable = false, unique = true, length = 80)
    private String siteKey;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SiteStatus status = SiteStatus.ACTIVE;

    @Column(nullable = false, length = 20)
    private String locale = "es-HN";

    @Column(nullable = false, length = 80)
    private String timezone = "America/Tegucigalpa";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSiteKey() { return siteKey; }
    public void setSiteKey(String siteKey) { this.siteKey = siteKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SiteStatus getStatus() { return status; }
    public void setStatus(SiteStatus status) { this.status = status; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
