package com.nattechnologies.trastcms.plugins.newsletter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@Entity
public class NewsletterSubscriber {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id
    private String id;

    private String email;
    private String name;
    private String status = "ACTIVE";
    private String pushSubscriptionId;
    private String source;
    private String token;
    private Instant subscribedAt;
    private Instant unsubscribedAt;
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (token == null) {
            token = UUID.randomUUID().toString().replace("-", "");
        }
        if (subscribedAt == null) {
            subscribedAt = Instant.now();
        }
        createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPushSubscriptionId() { return pushSubscriptionId; }
    public void setPushSubscriptionId(String pushSubscriptionId) { this.pushSubscriptionId = pushSubscriptionId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(Instant subscribedAt) { this.subscribedAt = subscribedAt; }
    public Instant getUnsubscribedAt() { return unsubscribedAt; }
    public void setUnsubscribedAt(Instant unsubscribedAt) { this.unsubscribedAt = unsubscribedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
