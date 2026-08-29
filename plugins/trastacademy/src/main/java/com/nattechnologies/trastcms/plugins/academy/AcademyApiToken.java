package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="academy_api_token")
class AcademyApiToken {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @Column(name="user_id", nullable=false, length=36) String userId;
    @Column(nullable=false, length=190) String email;
    @Column(name="access_token_hash", nullable=false, unique=true, length=64) String accessTokenHash;
    @Column(name="refresh_token_hash", nullable=false, unique=true, length=64) String refreshTokenHash;
    @Column(name="device_name", length=180) String deviceName;
    @Column(name="expires_at", nullable=false) Instant expiresAt;
    @Column(name="refresh_expires_at", nullable=false) Instant refreshExpiresAt;
    @Column(name="revoked_at") Instant revokedAt;
    @Column(name="last_used_at") Instant lastUsedAt;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @PrePersist void create(){createdAt=Instant.now();}
}
