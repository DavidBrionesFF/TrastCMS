package com.nattechnologies.trastcms.domain.site;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cms_site_endpoint", indexes = {
        @Index(name = "ix_site_endpoint_host", columnList = "host,port,active")
})
public class CmsSiteEndpoint {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private CmsSite site;

    @Column(nullable = false, length = 253)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 10)
    private String scheme = "https";

    @Column(name = "primary_endpoint", nullable = false)
    private boolean primaryEndpoint;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public CmsSite getSite() { return site; }
    public void setSite(CmsSite site) { this.site = site; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }
    public boolean isPrimaryEndpoint() { return primaryEndpoint; }
    public void setPrimaryEndpoint(boolean primaryEndpoint) { this.primaryEndpoint = primaryEndpoint; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
