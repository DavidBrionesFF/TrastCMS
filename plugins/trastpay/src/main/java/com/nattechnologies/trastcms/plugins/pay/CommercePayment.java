package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="commerce_payment") class CommercePayment {
 @Id @Column(length=36) String id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id") CommerceOrder order;
 @Column(name="gateway_key",nullable=false,length=80) String gatewayKey; @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) PaymentStatus status=PaymentStatus.CREATED;
 @Column(nullable=false,precision=19,scale=4) BigDecimal amount; @Column(nullable=false,length=3) String currency; @Column(name="external_reference",length=190) String externalReference;
 @Column(name="redirect_url",length=1000) String redirectUrl; @Column(name="idempotency_key",unique=true,length=120) String idempotencyKey; @Lob String metadataJson; @Lob String failureMessage;
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
