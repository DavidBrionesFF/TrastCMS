package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "travel_traveller")
class TravelTraveller {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    TravelBooking booking;
    @Column(name = "first_name", nullable = false, length = 100) String firstName;
    @Column(name = "last_name", length = 100) String lastName;
    @Column(name = "traveller_type", nullable = false, length = 30) String travellerType;
    @Column(name = "birth_date") LocalDate birthDate;
    @Column(length = 100) String nationality;
    @Column(name = "dietary_notes", length = 1000) String dietaryNotes;
    @Column(name = "medical_notes", length = 1000) String medicalNotes;
    @Column(name = "emergency_name", length = 180) String emergencyName;
    @Column(name = "emergency_phone", length = 60) String emergencyPhone;
    @Column(name = "created_at", nullable = false) Instant createdAt;

    @PrePersist void create() {
        createdAt = Instant.now();
    }
}
