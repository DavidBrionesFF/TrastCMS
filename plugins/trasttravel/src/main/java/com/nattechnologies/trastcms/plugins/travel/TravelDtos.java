package com.nattechnologies.trastcms.plugins.travel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class TravelDtos {
    private TravelDtos() { }

    public record DestinationRequest(Long arrivalLocationId, String arrivalLocationLabel,
            @NotBlank @Size(max = 180) String name,
            @Size(max = 180) String slug,
            @Size(max = 500) String summary,
            String description,
            @Size(max = 1000) String imageUrl,
            @Size(max = 120) String country,
            @Size(max = 120) String region,
            @Size(max = 120) String city,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean featured,
            boolean active) { }

    public record DestinationResponse(
            String id, String name, String slug, String summary, String description,
            String imageUrl, String country, String region, String city,
            BigDecimal latitude, BigDecimal longitude, boolean featured,
            boolean active, boolean demoData, Instant createdAt, Instant updatedAt) { }

    public record ActivityRequest(
            @NotBlank @Size(max = 180) String name,
            @Size(max = 180) String slug,
            @Size(max = 500) String description,
            @Size(max = 80) String icon,
            @Size(max = 1000) String imageUrl,
            boolean active) { }

    public record ActivityResponse(
            String id, String name, String slug, String description, String icon,
            String imageUrl, boolean active, boolean demoData,
            Instant createdAt, Instant updatedAt) { }

    public record ExtraRequest(
            @NotBlank @Size(max = 180) String name,
            @Size(max = 180) String slug,
            @Size(max = 600) String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            @Pattern(regexp = "PER_BOOKING|PER_PERSON|PER_UNIT") String pricingUnit,
            @Min(1) @Max(999) int maxQuantity,
            @Size(max = 1000) String imageUrl,
            boolean active) { }

    public record ExtraResponse(
            String id, String name, String slug, String description,
            BigDecimal price, String currency, String pricingUnit,
            int maxQuantity, String imageUrl, boolean active,
            boolean demoData, Instant createdAt, Instant updatedAt) { }

    public record TaxRule(
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal rate) { }

    public record TaxLine(
            String name, BigDecimal rate, BigDecimal amount) { }

    public record TourRequest(
            @NotBlank @Size(max = 240) String name,
            @Size(max = 180) String slug,
            @Size(max = 80) String code,
            @Size(max = 600) String shortDescription,
            String description,
            List<String> included,
            List<String> excluded,
            List<Map<String, Object>> itinerary,
            List<Map<String, Object>> faqs,
            List<String> gallery,
            String destinationId,
            List<String> activityIds,
            @Min(1) int durationValue,
            @Pattern(regexp = "MINUTE|HOUR|DAY") String durationUnit,
            @Size(max = 40) String difficulty,
            @Min(0) Integer minimumAge,
            @Min(0) Integer maximumAge,
            @Min(1) int minimumGroupSize,
            @Min(1) int maximumGroupSize,
            @NotNull @DecimalMin("0.00") BigDecimal adultPrice,
            @NotNull @DecimalMin("0.00") BigDecimal childPrice,
            @NotNull @DecimalMin("0.00") BigDecimal infantPrice,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            TravelPricingMode pricingMode,
            @DecimalMin("1.00") @DecimalMax("100.00") BigDecimal depositPercentage,
            boolean taxExempt,
            List<@Valid TaxRule> taxes,
            @Size(max = 1000) String featuredImageUrl,
            TravelTourStatus status,
            boolean featured,
            @Pattern(regexp = "INSTANT|MANUAL") String confirmationMode) { }

    public record TourResponse(
            String id, String name, String slug, String code,
            String shortDescription, String description,
            List<String> included, List<String> excluded,
            List<Map<String, Object>> itinerary, List<Map<String, Object>> faqs,
            List<String> gallery, DestinationResponse destination,
            List<ActivityResponse> activities, int durationValue, String durationUnit,
            String difficulty, Integer minimumAge, Integer maximumAge,
            int minimumGroupSize, int maximumGroupSize,
            BigDecimal adultPrice, BigDecimal childPrice, BigDecimal infantPrice,
            String currency, String pricingMode, BigDecimal depositPercentage,
            boolean taxExempt, List<TaxRule> taxes, BigDecimal defaultTaxRate,
            String featuredImageUrl, String status, boolean featured,
            String confirmationMode, boolean demoData, Instant createdAt,
            Instant updatedAt) { }

    public record DepartureRequest(
            @NotBlank String tourId,
            @NotNull Instant startsAt,
            Instant endsAt,
            @Min(1) int capacity,
            @Min(0) int blockedSeats,
            TravelDepartureStatus status,
            @DecimalMin("0.00") BigDecimal adultPrice,
            @DecimalMin("0.00") BigDecimal childPrice,
            @DecimalMin("0.00") BigDecimal infantPrice,
            @Size(max = 500) String meetingPoint,
            String notes) { }

    public record DepartureResponse(
            String id, String tourId, String tourName, String tourSlug,
            Instant startsAt, Instant endsAt, int capacity, int reservedSeats,
            int heldSeats, int blockedSeats, int availableSeats, String status,
            BigDecimal adultPrice, BigDecimal childPrice, BigDecimal infantPrice,
            String currency, String meetingPoint, String googleCalendarUrl,
            String notes, boolean demoData, Instant createdAt, Instant updatedAt) { }

    public record TravellerRequest(
            @NotBlank @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Pattern(regexp = "ADULT|CHILD|INFANT") String travellerType,
            LocalDate birthDate,
            @Size(max = 100) String nationality,
            @Size(max = 1000) String dietaryNotes,
            @Size(max = 1000) String medicalNotes,
            @Size(max = 180) String emergencyName,
            @Size(max = 60) String emergencyPhone) { }

    public record TravellerResponse(
            String id, String firstName, String lastName, String travellerType,
            LocalDate birthDate, String nationality, String dietaryNotes,
            String medicalNotes, String emergencyName, String emergencyPhone) { }

    public record BookingQuoteRequest(
            @NotBlank String departureId,
            @Email @NotBlank String customerEmail,
            @NotBlank @Size(max = 190) String customerName,
            @Size(max = 60) String customerPhone,
            @Min(0) @Max(99) int adults,
            @Min(0) @Max(99) int children,
            @Min(0) @Max(99) int infants,
            Map<String, Integer> extras,
            List<@Valid TravellerRequest> travellers,
            String notes) { }

    public record BookingResponse(
            String id, String bookingCode, String tourId, String tourName,
            String tourSlug, String departureId, Instant startsAt,
            String customerEmail, String customerName, String customerPhone,
            int adults, int children, int infants, String currency,
            BigDecimal subtotal, BigDecimal extrasTotal, BigDecimal discountTotal,
            BigDecimal taxTotal, List<TaxLine> taxes, BigDecimal grandTotal, BigDecimal depositRequired,
            BigDecimal amountPaid, BigDecimal balanceDue, String bookingStatus,
            String paymentStatus, String orderId, String orderNumber,
            String googleCalendarUrl, Instant holdExpiresAt,
            List<TravellerResponse> travellers, Map<String, Object> extras,
            Instant createdAt, Instant updatedAt) { }

    public record SettingsRequest(
            @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
            @Size(max = 80) String timezone,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
            @Min(5) @Max(1440) int bookingHoldMinutes,
            @Min(0) @Max(8760) int minimumLeadHours,
            boolean guestBooking,
            boolean googleCalendarEnabled,
            @Size(max = 300) String googleCalendarId,
            @Email String notificationEmail,
            @Size(max = 1000) String termsUrl) { }

    public record SettingsResponse(
            String defaultCurrency, String timezone, BigDecimal taxRate,
            int bookingHoldMinutes, int minimumLeadHours, boolean guestBooking,
            boolean googleCalendarEnabled, String googleCalendarId,
            String notificationEmail, String termsUrl, Instant updatedAt) { }

    public record Dashboard(
            long tours, long publishedTours, long destinations, long activities,
            long upcomingDepartures, long pendingBookings, long confirmedBookings,
            BigDecimal revenue, String currency, List<BookingResponse> recentBookings,
            List<DepartureResponse> nextDepartures) { }

    public record SearchResponse(
            List<TourResponse> tours, List<DestinationResponse> destinations,
            List<ActivityResponse> activities, int total) { }

    public record CalendarResponse(
            Instant from, Instant to,
            List<DepartureResponse> departures,
            List<BookingResponse> bookings) { }

    public record DemoDataResponse(
            int destinations, int activities, int tours, int departures,
            int extras, String message) { }

    public record PageResponse<T>(
            List<T> content, int page, int size, long totalElements, int totalPages) { }
}
