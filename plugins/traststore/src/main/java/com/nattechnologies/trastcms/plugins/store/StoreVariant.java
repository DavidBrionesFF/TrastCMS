package com.nattechnologies.trastcms.plugins.store;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="store_variant") class StoreVariant {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") StoreProduct product; @Column(nullable=false,length=180) String name; @Column(nullable=false,length=100) String sku;
 @Column(precision=19,scale=4) BigDecimal price; @Column(name="stock_quantity",nullable=false) int stockQuantity; @Column(name="option_values",length=500) String optionValues; @Column(name="image_url",length=1000) String imageUrl; @Column(nullable=false) boolean active=true; @Column(name="created_at",nullable=false) Instant createdAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=Instant.now();}
}
