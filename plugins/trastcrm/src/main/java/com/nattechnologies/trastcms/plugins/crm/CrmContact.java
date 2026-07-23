package com.nattechnologies.trastcms.plugins.crm;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="crm_contact")
class CrmContact {
 @Id @Column(length=36) String id; @Column(name="first_name",nullable=false,length=100) String firstName; @Column(name="last_name",length=100) String lastName;
 @Column(length=190) String email; @Column(length=60) String phone; @Column(name="job_title",length=140) String jobTitle;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) CrmContactStatus status=CrmContactStatus.LEAD; @Column(length=120) String source; @Column(name="owner_email",length=190) String ownerEmail;
 @Lob String tags; @Lob String notes; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") CrmCompany company;
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
