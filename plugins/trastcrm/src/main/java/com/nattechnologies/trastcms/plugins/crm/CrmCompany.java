package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="crm_company")
class CrmCompany {
 @Id @Column(length=36) String id; @Column(nullable=false,length=180) String name; @Column(length=190) String domain;
 @Column(length=190) String email; @Column(length=60) String phone; @Column(length=500) String address;
 @Column(length=120) String city; @Column(length=120) String country; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) CrmCompanyStatus status=CrmCompanyStatus.PROSPECT;
 @Lob String notes; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
