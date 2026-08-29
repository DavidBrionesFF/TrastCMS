package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "travel_booking")
class TravelBooking {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(name = "booking_code", nullable = false, length = 40) String bookingCode;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    TravelTour tour;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "departure_id", nullable = false)
    TravelDeparture departure;
    @Column(name = "account_id", length = 36) String accountId;
    @Column(name = "customer_email", nullable = false, length = 190) String customerEmail;
    @Column(name = "customer_name", nullable = false, length = 190) String customerName;
    @Column(name = "customer_phone", length = 60) String customerPhone;
    @Column(name = "adult_count", nullable = false) int adultCount;
    @Column(name = "child_count", nullable = false) int childCount;
    @Column(name = "infant_count", nullable = false) int infantCount;
    @Column(nullable = false, length = 3) String currency;
    @Column(nullable = false, precision = 19, scale = 4) BigDecimal subtotal;
    @Column(name = "extras_total", nullable = false, precision = 19, scale = 4) BigDecimal extrasTotal;
    @Column(name = "discount_total", nullable = false, precision = 19, scale = 4) BigDecimal discountTotal;
    @Column(name = "tax_total", nullable = false, precision = 19, scale = 4) BigDecimal taxTotal;
    @Lob @Column(name = "tax_breakdown_json") String taxBreakdownJson;
    @Column(name = "grand_total", nullable = false, precision = 19, scale = 4) BigDecimal grandTotal;
    @Column(name = "deposit_required", nullable = false, precision = 19, scale = 4) BigDecimal depositRequired;
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4) BigDecimal amountPaid;
    @Column(name = "balance_due", nullable = false, precision = 19, scale = 4) BigDecimal balanceDue;
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 40)
    TravelBookingStatus bookingStatus = TravelBookingStatus.PENDING_PAYMENT;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 40)
    TravelPaymentStatus paymentStatus = TravelPaymentStatus.UNPAID;
    @Column(name = "order_id", length = 36) String orderId;
    @Column(name = "order_number", length = 40) String orderNumber;
    @Column(name = "coupon_code", length = 80) String couponCode;
    @Lob @Column(name = "extras_json") String extrasJson;
    @Lob String notes;
    @Column(name = "hold_expires_at") Instant holdExpiresAt;
    @Column(name = "confirmed_at") Instant confirmedAt;
    @Column(name = "cancelled_at") Instant cancelledAt;
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TravelTraveller> travellers = new ArrayList<>();
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    int seats() { return adultCount + childCount + infantCount; }

    @PrePersist void create() {
        createdAt = updatedAt = Instant.now();
    }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
