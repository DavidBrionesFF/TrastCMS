package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="crm_submission") class CrmSubmission {
 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="form_id",nullable=false) CrmForm form; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id") CrmContact contact; @Lob @Column(name="payload_json",nullable=false) String payloadJson; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) CrmSubmissionStatus status=CrmSubmissionStatus.NEW; @Column(name="source_url",length=1000) String sourceUrl; @Column(name="ip_hash",length=128) String ipHash; @Column(name="user_agent",length=1000) String userAgent; @Column(name="created_at",nullable=false) Instant createdAt; @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=Instant.now();}
}
