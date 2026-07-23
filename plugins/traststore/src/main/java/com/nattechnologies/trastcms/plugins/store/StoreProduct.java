package com.nattechnologies.trastcms.plugins.store;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Entity @Table(name="store_product") class StoreProduct {
 @Id @Column(length=36) String id; @Column(nullable=false,length=240) String name; @Column(nullable=false,unique=true,length=180) String slug; @Column(nullable=false,unique=true,length=100) String sku;
 @Lob String description; @Lob String shortDescription; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) ProductType type=ProductType.PHYSICAL; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) ProductStatus status=ProductStatus.DRAFT;
 @Column(nullable=false,precision=19,scale=4) BigDecimal price=BigDecimal.ZERO; @Column(name="compare_at_price",precision=19,scale=4) BigDecimal compareAtPrice; @Column(nullable=false,length=3) String currency="USD";
 @Column(length=120) String category; @Column(length=120) String brand; @Column(name="featured_image_url",length=1000) String featuredImageUrl; @Lob String galleryJson; @Lob String attributesJson; @Lob String seoJson;
 @Column(nullable=false) boolean featured=false; @Column(name="track_inventory",nullable=false) boolean trackInventory=true; @Column(name="allow_backorder",nullable=false) boolean allowBackorder=false;
 @Column(name="stock_quantity",nullable=false) int stockQuantity=0; @Column(name="low_stock_threshold",nullable=false) int lowStockThreshold=5; @Column(name="weight_grams") Integer weightGrams; @Column(name="digital_url",length=1000) String digitalUrl;
 @OneToMany(mappedBy="product",cascade=CascadeType.ALL,orphanRemoval=true) List<StoreVariant> variants=new ArrayList<>();
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
