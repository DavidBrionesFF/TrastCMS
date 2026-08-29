package com.nattechnologies.trastcms.plugins.travel;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/travel")
class TravelPublicController {
    private final TravelService service;

    TravelPublicController(TravelService service) { this.service = service; }

    @GetMapping("/search")
    TravelDtos.SearchResponse search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer travellers,
            @RequestParam(required = false) Integer travelers) {
        int requestedTravellers = travellers != null ? travellers
                : travelers != null ? travelers : 1;
        return service.search(
                search, destination, activity, date, Math.max(1, requestedTravellers));
    }

    @GetMapping("/destinations")
    List<TravelDtos.DestinationResponse> destinations() {
        return service.destinations(true);
    }

    @GetMapping("/activities")
    List<TravelDtos.ActivityResponse> activities() {
        return service.activities(true);
    }

    @GetMapping("/extras")
    List<TravelDtos.ExtraResponse> extras() {
        return service.extras(true);
    }

    @GetMapping("/tours/{slug}")
    TravelDtos.TourResponse tour(@PathVariable String slug) {
        return service.publicTour(slug);
    }

    @GetMapping("/tours/{slug}/departures")
    List<TravelDtos.DepartureResponse> departures(
            @PathVariable String slug,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        TravelDtos.TourResponse tour = service.publicTour(slug);
        return service.departures(from, to, tour.id());
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.BookingResponse createBooking(
            @Valid @RequestBody TravelDtos.BookingQuoteRequest request,
            Authentication authentication) {
        String email = authentication == null || "anonymousUser".equals(authentication.getName())
                ? null : authentication.getName();
        return service.createBooking(request, email);
    }

    @GetMapping("/bookings/{code}")
    TravelDtos.BookingResponse booking(
            @PathVariable String code,
            @RequestParam String email) {
        return service.booking(code, email);
    }
}

@RestController
@RequestMapping("/api/account/travel")
class TravelAccountController {
    private final TravelService service;

    TravelAccountController(TravelService service) { this.service = service; }

    @GetMapping("/bookings")
    List<java.util.Map<String, Object>> bookings(Authentication authentication) {
        return service.accountBookings(authentication.getName());
    }

    @GetMapping("/bookings/{id}")
    TravelDtos.BookingResponse booking(
            @PathVariable String id,
            Authentication authentication) {
        return service.booking(id, authentication.getName());
    }

    @PostMapping("/bookings/{id}/cancel")
    TravelDtos.BookingResponse cancel(
            @PathVariable String id,
            Authentication authentication) {
        return service.cancel(id, authentication.getName());
    }
}

@RestController
@RequestMapping("/api/admin/travel")
class TravelAdminController {
    private final TravelService service;

    TravelAdminController(TravelService service) { this.service = service; }

    @GetMapping("/dashboard")
    TravelDtos.Dashboard dashboard() { return service.dashboard(); }

    @GetMapping("/destinations")
    List<TravelDtos.DestinationResponse> destinations() { return service.destinations(false); }

    @PostMapping("/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.DestinationResponse createDestination(
            @Valid @RequestBody TravelDtos.DestinationRequest request) {
        return service.saveDestination(null, request);
    }

    @PutMapping("/destinations/{id}")
    TravelDtos.DestinationResponse updateDestination(
            @PathVariable String id,
            @Valid @RequestBody TravelDtos.DestinationRequest request) {
        return service.saveDestination(id, request);
    }

    @DeleteMapping("/destinations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDestination(@PathVariable String id) { service.deleteDestination(id); }

    @GetMapping("/activities")
    List<TravelDtos.ActivityResponse> activities() { return service.activities(false); }

    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.ActivityResponse createActivity(
            @Valid @RequestBody TravelDtos.ActivityRequest request) {
        return service.saveActivity(null, request);
    }

    @PutMapping("/activities/{id}")
    TravelDtos.ActivityResponse updateActivity(
            @PathVariable String id,
            @Valid @RequestBody TravelDtos.ActivityRequest request) {
        return service.saveActivity(id, request);
    }

    @DeleteMapping("/activities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteActivity(@PathVariable String id) { service.deleteActivity(id); }

    @GetMapping("/extras")
    List<TravelDtos.ExtraResponse> extras() { return service.extras(false); }

    @PostMapping("/extras")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.ExtraResponse createExtra(
            @Valid @RequestBody TravelDtos.ExtraRequest request) {
        return service.saveExtra(null, request);
    }

    @PutMapping("/extras/{id}")
    TravelDtos.ExtraResponse updateExtra(
            @PathVariable String id,
            @Valid @RequestBody TravelDtos.ExtraRequest request) {
        return service.saveExtra(id, request);
    }

    @DeleteMapping("/extras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteExtra(@PathVariable String id) { service.deleteExtra(id); }

    @GetMapping("/tours")
    TravelDtos.PageResponse<TravelDtos.TourResponse> tours(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.adminTours(search, page, size);
    }

    @PostMapping("/tours")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.TourResponse createTour(
            @Valid @RequestBody TravelDtos.TourRequest request) {
        return service.saveTour(null, request);
    }

    @PutMapping("/tours/{id}")
    TravelDtos.TourResponse updateTour(
            @PathVariable String id,
            @Valid @RequestBody TravelDtos.TourRequest request) {
        return service.saveTour(id, request);
    }

    @DeleteMapping("/tours/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTour(@PathVariable String id) { service.deleteTour(id); }

    @GetMapping("/departures")
    List<TravelDtos.DepartureResponse> departures(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String tourId) {
        return service.departures(from, to, tourId);
    }

    @GetMapping("/calendar")
    TravelDtos.CalendarResponse calendar(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return service.calendar(from, to);
    }

    @PostMapping("/departures")
    @ResponseStatus(HttpStatus.CREATED)
    TravelDtos.DepartureResponse createDeparture(
            @Valid @RequestBody TravelDtos.DepartureRequest request) {
        return service.saveDeparture(null, request);
    }

    @PutMapping("/departures/{id}")
    TravelDtos.DepartureResponse updateDeparture(
            @PathVariable String id,
            @Valid @RequestBody TravelDtos.DepartureRequest request) {
        return service.saveDeparture(id, request);
    }

    @DeleteMapping("/departures/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDeparture(@PathVariable String id) { service.deleteDeparture(id); }

    @GetMapping("/bookings")
    TravelDtos.PageResponse<TravelDtos.BookingResponse> bookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.bookings(page, size);
    }

    @PostMapping("/bookings/{id}/cancel")
    TravelDtos.BookingResponse cancel(@PathVariable String id) {
        return service.cancel(id, null);
    }

    @GetMapping("/settings")
    TravelDtos.SettingsResponse settings() { return service.settingsResponse(); }

    @PutMapping("/settings")
    TravelDtos.SettingsResponse settings(
            @Valid @RequestBody TravelDtos.SettingsRequest request) {
        return service.saveSettings(request);
    }

    @PostMapping("/demo-data")
    TravelDtos.DemoDataResponse demoData() { return service.generateDemoData(); }

    @DeleteMapping("/demo-data")
    TravelDtos.DemoDataResponse deleteDemoData() { return service.deleteDemoData(); }
}
