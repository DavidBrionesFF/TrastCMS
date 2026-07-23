package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="commerce_cart_item") class CommerceCartItem {
 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cart_id") CommerceCart cart;
 @Column(name="provider_key",nullable=false,length=80) String providerKey; @Column(name="item_reference",nullable=false,length=120) String itemReference;
 @Column(name="item_type",nullable=false,length=50) String itemType; @Column(nullable=false,length=240) String name; @Column(nullable=false) int quantity=1;
 @Column(name="unit_price",nullable=false,precision=19,scale=4) BigDecimal unitPrice; @Column(nullable=false,length=3) String currency; @Lob String metadataJson;
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
