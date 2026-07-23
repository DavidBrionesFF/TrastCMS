package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.*; import java.util.UUID;
@Entity @Table(name="crm_deal") class CrmDeal {
 @Id @Column(length=36) String id; @Column(nullable=false,length=220) String title; @Column(name="value_amount",nullable=false,precision=19,scale=2) BigDecimal value=BigDecimal.ZERO; @Column(nullable=false,length=10) String currency="HNL"; @Column(name="expected_close_date") LocalDate expectedCloseDate; @Column(name="owner_email",length=190) String ownerEmail; @Lob String description;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_id") CrmContact contact; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") CrmCompany company; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="stage_id",nullable=false) CrmPipelineStage stage;
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt; @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
