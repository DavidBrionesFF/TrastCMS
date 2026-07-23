package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="commerce_order_item") class CommerceOrderItem {
 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id") CommerceOrder order;
 @Column(name="provider_key",nullable=false,length=80) String providerKey; @Column(name="item_reference",nullable=false,length=120) String itemReference; @Column(name="item_type",nullable=false,length=50) String itemType;
 @Column(nullable=false,length=240) String name; @Column(nullable=false) int quantity; @Column(name="unit_price",nullable=false,precision=19,scale=4) BigDecimal unitPrice;
 @Column(name="line_total",nullable=false,precision=19,scale=4) BigDecimal lineTotal; @Lob String metadataJson; @Column(name="fulfilled_at") Instant fulfilledAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();}
}
