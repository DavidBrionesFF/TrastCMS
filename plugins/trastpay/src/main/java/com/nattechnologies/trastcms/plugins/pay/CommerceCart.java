package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="commerce_cart") class CommerceCart {
 @Id @Column(length=36) String id; @Column(name="cart_token",nullable=false,unique=true,length=80) String cartToken;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) CartStatus status=CartStatus.ACTIVE; @Column(nullable=false,length=3) String currency="USD";
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id") CommerceCustomer customer; @Column(name="expires_at") Instant expiresAt;
 @OneToMany(mappedBy="cart",cascade=CascadeType.ALL,orphanRemoval=true) List<CommerceCartItem> items=new ArrayList<>();
 @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();if(cartToken==null)cartToken=UUID.randomUUID().toString().replace("-","");createdAt=updatedAt=Instant.now();if(expiresAt==null)expiresAt=Instant.now().plusSeconds(60*60*24*14);} @PreUpdate void update(){updatedAt=Instant.now();}
}
