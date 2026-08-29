package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "travel_departure")
class TravelDeparture {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    TravelTour tour;
    @Column(name = "starts_at", nullable = false) Instant startsAt;
    @Column(name = "ends_at") Instant endsAt;
    @Column(nullable = false) int capacity;
    @Column(name = "reserved_seats", nullable = false) int reservedSeats;
    @Column(name = "held_seats", nullable = false) int heldSeats;
    @Column(name = "blocked_seats", nullable = false) int blockedSeats;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    TravelDepartureStatus status = TravelDepartureStatus.SCHEDULED;
    @Column(name = "adult_price", precision = 19, scale = 4) BigDecimal adultPrice;
    @Column(name = "child_price", precision = 19, scale = 4) BigDecimal childPrice;
    @Column(name = "infant_price", precision = 19, scale = 4) BigDecimal infantPrice;
    @Column(name = "meeting_point", length = 500) String meetingPoint;
    @Column(name = "google_calendar_event_id", length = 300) String googleCalendarEventId;
    @Column(name = "google_calendar_url", length = 1000) String googleCalendarUrl;
    @Lob String notes;
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    int availableSeats() {
        return Math.max(0, capacity - reservedSeats - heldSeats - blockedSeats);
    }

    @PrePersist void create() {
        createdAt = updatedAt = Instant.now();
    }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
