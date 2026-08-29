package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "travel_tour")
class TravelTour {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(nullable = false, length = 240) String name;
    @Column(nullable = false, length = 180) String slug;
    @Column(nullable = false, length = 80) String code;
    @Column(name = "short_description", length = 600) String shortDescription;
    @Lob String description;
    @Lob @Column(name = "included_json") String includedJson;
    @Lob @Column(name = "excluded_json") String excludedJson;
    @Lob @Column(name = "itinerary_json") String itineraryJson;
    @Lob @Column(name = "faq_json") String faqJson;
    @Lob @Column(name = "gallery_json") String galleryJson;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_id")
    TravelDestination destination;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "travel_tour_activity",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "activity_id"))
    Set<TravelActivity> activities = new LinkedHashSet<>();
    @Column(name = "duration_value", nullable = false) int durationValue = 1;
    @Column(name = "duration_unit", nullable = false, length = 20) String durationUnit = "DAY";
    @Column(length = 40) String difficulty = "EASY";
    @Column(name = "minimum_age") Integer minimumAge;
    @Column(name = "maximum_age") Integer maximumAge;
    @Column(name = "minimum_group_size", nullable = false) int minimumGroupSize = 1;
    @Column(name = "maximum_group_size", nullable = false) int maximumGroupSize = 20;
    @Column(name = "adult_price", nullable = false, precision = 19, scale = 4) BigDecimal adultPrice;
    @Column(name = "child_price", nullable = false, precision = 19, scale = 4) BigDecimal childPrice;
    @Column(name = "infant_price", nullable = false, precision = 19, scale = 4) BigDecimal infantPrice;
    @Column(nullable = false, length = 3) String currency = "USD";
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 30)
    TravelPricingMode pricingMode = TravelPricingMode.PER_PERSON;
    @Column(name = "deposit_percentage", nullable = false, precision = 7, scale = 4)
    BigDecimal depositPercentage = BigDecimal.valueOf(100);
    @Column(name = "tax_exempt", nullable = false) boolean taxExempt;
    @Lob @Column(name = "tax_rules_json") String taxRulesJson;
    @Column(name = "featured_image_url", length = 1000) String featuredImageUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    TravelTourStatus status = TravelTourStatus.DRAFT;
    @Column(nullable = false) boolean featured;
    @Column(name = "confirmation_mode", nullable = false, length = 30) String confirmationMode = "INSTANT";
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @PrePersist void create() {
        createdAt = updatedAt = Instant.now();
    }
    @PreUpdate void update() { updatedAt = Instant.now(); }
}
