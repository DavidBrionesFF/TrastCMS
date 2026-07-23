package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="crm_activity") class CrmActivity {
 @Id @Column(length=36) String id; @Enumerated(EnumType.STRING) @Column(name="activity_type",nullable=false,length=30) CrmActivityType type; @Column(nullable=false,length=220) String subject; @Lob String description; @Column(name="due_at") Instant dueAt; @Column(name="completed_at") Instant completedAt; @Column(name="assigned_to",length=190) String assignedTo;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id") CrmContact contact; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") CrmCompany company; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="deal_id") CrmDeal deal; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt; @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
