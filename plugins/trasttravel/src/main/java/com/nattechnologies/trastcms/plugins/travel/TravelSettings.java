package com.nattechnologies.trastcms.plugins.travel;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "travel_settings")
class TravelSettings {
    static String currentId() { return com.nattechnologies.trastcms.multisite.SiteContext.siteId(); }
    @Id @Column(length = 36) String id = currentId();
    @Column(name = "default_currency", nullable = false, length = 3) String defaultCurrency = "USD";
    @Column(nullable = false, length = 80) String timezone = "America/Tegucigalpa";
    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4) BigDecimal taxRate = BigDecimal.ZERO;
    @Column(name = "booking_hold_minutes", nullable = false) int bookingHoldMinutes = 30;
    @Column(name = "minimum_lead_hours", nullable = false) int minimumLeadHours = 4;
    @Column(name = "guest_booking", nullable = false) boolean guestBooking = true;
    @Column(name = "google_calendar_enabled", nullable = false) boolean googleCalendarEnabled;
    @Column(name = "google_calendar_id", length = 300) String googleCalendarId;
    @Column(name = "notification_email", length = 190) String notificationEmail;
    @Column(name = "terms_url", length = 1000) String termsUrl;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;

    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }
}
