package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="commerce_customer") class CommerceCustomer {
 @Id @Column(length=36) String id; @Column(nullable=false,length=190) String email; @Column(name="display_name",length=190) String displayName;
 @Column(length=60) String phone; @Lob String metadataJson; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
