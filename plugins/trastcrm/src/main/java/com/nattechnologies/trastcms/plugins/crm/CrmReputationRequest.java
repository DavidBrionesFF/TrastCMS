package com.nattechnologies.trastcms.plugins.crm;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_reputation_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_crm_reputation_source", columnNames = {"site_id", "source_type", "source_reference"}),
                @UniqueConstraint(name = "uq_crm_reputation_token", columnNames = {"public_token"})
        },
        indexes = {
                @Index(name = "idx_crm_reputation_status", columnList = "site_id,status,created_at"),
                @Index(name = "idx_crm_reputation_contact", columnList = "site_id,contact_id,created_at")
        })
class CrmReputationRequest {
    @org.hibernate.annotations.TenantId
    @Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contact_id") CrmContact contact;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 30) CrmReputationSource sourceType;
    @Column(name = "source_reference", nullable = false, length = 190) String sourceReference;
    @Column(name = "source_label", nullable = false, length = 240) String sourceLabel;
    @Column(name = "recipient_email", nullable = false, length = 190) String recipientEmail;
    @Column(name = "customer_name", length = 180) String customerName;
    @Column(name = "trigger_event", nullable = false, length = 120) String triggerEvent;
    @Column(name = "public_token", nullable = false, length = 64) String publicToken;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) CrmReputationStatus status = CrmReputationStatus.PENDING;
    @Column Integer rating;
    @Lob @Column(name = "private_feedback") String privateFeedback;
    @Column(name = "channel_id", length = 80) String channelId;
    @Column(name = "follow_up_activity_id", length = 36) String followUpActivityId;
    @Column(name = "sent_at") Instant sentAt;
    @Column(name = "rated_at") Instant ratedAt;
    @Column(name = "channel_clicked_at") Instant channelClickedAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        if (publicToken == null) publicToken = UUID.randomUUID().toString();
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate void update() { updatedAt = Instant.now(); }
}
