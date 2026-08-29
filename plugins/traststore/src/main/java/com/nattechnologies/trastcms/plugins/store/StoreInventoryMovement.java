package com.nattechnologies.trastcms.plugins.store;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="store_inventory_movement") class StoreInventoryMovement {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") StoreProduct product; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id") StoreVariant variant;
 @Column(nullable=false,length=30) String type; @Column(nullable=false) int quantity; @Column(nullable=false,length=240) String reason; @Column(name="reference_type",length=50) String referenceType; @Column(name="reference_id",length=80) String referenceId; @Column(name="created_at",nullable=false) Instant createdAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=Instant.now();}
}
