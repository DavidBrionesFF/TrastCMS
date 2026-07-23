package com.nattechnologies.trastcms.plugins.pay;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Entity @Table(name="commerce_order") class CommerceOrder {
 @Id @Column(length=36) String id; @Column(name="order_number",nullable=false,unique=true,length=40) String orderNumber; @Column(name="public_token",nullable=false,unique=true,length=80) String publicToken;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) OrderStatus status=OrderStatus.PENDING_PAYMENT; @Column(nullable=false,length=3) String currency;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id") CommerceCustomer customer; @Column(name="customer_email",nullable=false,length=190) String customerEmail;
 @Column(name="customer_name",length=190) String customerName; @Column(name="subtotal",nullable=false,precision=19,scale=4) BigDecimal subtotal;
 @Column(name="discount_total",nullable=false,precision=19,scale=4) BigDecimal discountTotal=BigDecimal.ZERO; @Column(name="tax_total",nullable=false,precision=19,scale=4) BigDecimal taxTotal=BigDecimal.ZERO;
 @Column(name="grand_total",nullable=false,precision=19,scale=4) BigDecimal grandTotal; @Column(name="coupon_code",length=80) String couponCode; @Lob String billingJson; @Lob String shippingJson; @Lob String notes;
 @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) List<CommerceOrderItem> items=new ArrayList<>();
 @Column(name="paid_at") Instant paidAt; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
 @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
}
