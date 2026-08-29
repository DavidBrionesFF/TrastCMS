package com.nattechnologies.trastcms.plugins.travel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugin.api.SellableProvider;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.ContentSanitizer;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import com.nattechnologies.trastcms.service.PlatformMailService;
import com.nattechnologies.trastcms.service.SlugService;
import com.nattechnologies.trastcms.service.SiteSettingService;
import com.nattechnologies.trastcms.service.UserAccountService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TravelService {
    private final TravelDestinationRepository destinations;
    private final TravelActivityRepository activities;
    private final TravelExtraRepository extras;
    private final TravelTourRepository tours;
    private final TravelDepartureRepository departures;
    private final TravelBookingRepository bookings;
    private final TravelSettingsRepository settingsRepository;
    private final SlugService slugs;
    private final ContentSanitizer sanitizer;
    private final ObjectMapper mapper;
    private final PluginEventDispatcher events;
    private final PlatformMailService mail;
    private final UserAccountService users;
    private final SiteSettingService siteSettings;
    private final TravelPluginGuard guard;

    TravelService(
            TravelDestinationRepository destinations,
            TravelActivityRepository activities,
            TravelExtraRepository extras,
            TravelTourRepository tours,
            TravelDepartureRepository departures,
            TravelBookingRepository bookings,
            TravelSettingsRepository settingsRepository,
            SlugService slugs,
            ContentSanitizer sanitizer,
            ObjectMapper mapper,
            PluginEventDispatcher events,
            PlatformMailService mail,
            UserAccountService users,
            SiteSettingService siteSettings,
            TravelPluginGuard guard) {
        this.destinations = destinations;
        this.activities = activities;
        this.extras = extras;
        this.tours = tours;
        this.departures = departures;
        this.bookings = bookings;
        this.settingsRepository = settingsRepository;
        this.slugs = slugs;
        this.sanitizer = sanitizer;
        this.mapper = mapper;
        this.events = events;
        this.mail = mail;
        this.users = users;
        this.siteSettings = siteSettings;
        this.guard = guard;
    }

    @Transactional
    public void initializeDefaults() {
        if (!settingsRepository.existsById(TravelSettings.currentId())) {
            settingsRepository.save(new TravelSettings());
        }
        if (tours.count() == 0) {
            generateDemoDataInternal();
        }
    }

    @Transactional(readOnly = true)
    public TravelDtos.Dashboard dashboard() {
        guard.requireEnabled();
        TravelSettings settings = settings();
        List<TravelBooking> recent = bookings.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8)).getContent();
        BigDecimal revenue = bookings.findAll().stream()
                .filter(item -> item.amountPaid != null && item.amountPaid.signum() > 0)
                .map(item -> item.amountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TravelDeparture> upcoming = departures.findByStartsAtBetweenOrderByStartsAtAsc(
                Instant.now(), Instant.now().plus(Duration.ofDays(45)));
        return new TravelDtos.Dashboard(
                tours.count(),
                tours.countByStatus(TravelTourStatus.PUBLISHED),
                destinations.count(),
                activities.count(),
                departures.countByStartsAtAfter(Instant.now()),
                bookings.countByBookingStatus(TravelBookingStatus.PENDING_PAYMENT),
                bookings.countByBookingStatus(TravelBookingStatus.CONFIRMED),
                money(revenue),
                settings.defaultCurrency,
                recent.stream().filter(this::hasValidBooking).map(this::booking).toList(),
                upcoming.stream().filter(this::hasValidDeparture).limit(10).map(this::departure).toList());
    }

    @Transactional(readOnly = true)
    public List<TravelDtos.DestinationResponse> destinations(boolean publicOnly) {
        guard.requireEnabled();
        List<TravelDestination> values = publicOnly
                ? destinations.findByActiveTrueOrderByFeaturedDescNameAsc()
                : destinations.findAll();
        return values.stream()
                .filter(Objects::nonNull)
                .filter(item -> !publicOnly || validDestination(item))
                .sorted(Comparator.comparing((TravelDestination item) -> !item.featured)
                        .thenComparing(
                                item -> Optional.ofNullable(item.name).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .map(this::destination)
                .toList();
    }

    @Transactional
    public TravelDtos.DestinationResponse saveDestination(
            String id, TravelDtos.DestinationRequest request) {
        guard.requireEnabled();
        TravelDestination entity = id == null ? new TravelDestination() : requireDestination(id);
        String slug = uniqueDestinationSlug(request.slug(), request.name(), id);
        entity.name = request.name().trim();
        entity.slug = slug;
        entity.summary = blank(request.summary());
        entity.description = sanitizedHtml(request.description());
        entity.imageUrl = blank(request.imageUrl());
        entity.country = blank(request.country());
        entity.region = blank(request.region());
        entity.city = blank(request.city());
        entity.latitude = request.latitude();
        entity.longitude = request.longitude();
        entity.featured = request.featured();
        entity.active = request.active();
        return destination(destinations.save(entity));
    }

    @Transactional
    public void deleteDestination(String id) {
        guard.requireEnabled();
        TravelDestination entity = requireDestination(id);
        if (tours.findAll().stream().anyMatch(item -> item.destination != null && Objects.equals(item.destination.id, id))) {
            throw new BadRequestException("El destino tiene tours asociados");
        }
        destinations.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<TravelDtos.ActivityResponse> activities(boolean publicOnly) {
        guard.requireEnabled();
        List<TravelActivity> values = publicOnly
                ? activities.findByActiveTrueOrderByNameAsc()
                : activities.findAll();
        return values.stream()
                .filter(Objects::nonNull)
                .filter(item -> !publicOnly || validActivity(item))
                .sorted(Comparator.comparing(
                        item -> Optional.ofNullable(item.name).orElse(""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::activity)
                .toList();
    }

    @Transactional
    public TravelDtos.ActivityResponse saveActivity(
            String id, TravelDtos.ActivityRequest request) {
        guard.requireEnabled();
        TravelActivity entity = id == null ? new TravelActivity() : requireActivity(id);
        entity.name = request.name().trim();
        entity.slug = uniqueActivitySlug(request.slug(), request.name(), id);
        entity.description = sanitizedHtml(request.description());
        entity.icon = blank(request.icon());
        entity.imageUrl = blank(request.imageUrl());
        entity.active = request.active();
        return activity(activities.save(entity));
    }

    @Transactional
    public void deleteActivity(String id) {
        guard.requireEnabled();
        TravelActivity entity = requireActivity(id);
        for (TravelTour tour : tours.findAll()) {
            if (tour.activities.removeIf(item -> Objects.equals(item.id, id))) tours.save(tour);
        }
        activities.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<TravelDtos.ExtraResponse> extras(boolean publicOnly) {
        guard.requireEnabled();
        List<TravelExtra> values = publicOnly
                ? extras.findByActiveTrueOrderByNameAsc()
                : extras.findAll();
        return values.stream()
                .filter(Objects::nonNull)
                .filter(item -> !publicOnly || validExtra(item))
                .map(this::extra)
                .toList();
    }

    @Transactional
    public TravelDtos.ExtraResponse saveExtra(String id, TravelDtos.ExtraRequest request) {
        guard.requireEnabled();
        TravelExtra entity = id == null ? new TravelExtra() : requireExtra(id);
        entity.name = request.name().trim();
        entity.slug = uniqueExtraSlug(request.slug(), request.name(), id);
        entity.description = blank(request.description());
        entity.price = money(request.price());
        entity.currency = normalizeCurrency(request.currency());
        entity.pricingUnit = request.pricingUnit() == null ? "PER_BOOKING" : request.pricingUnit();
        entity.maxQuantity = Math.max(1, request.maxQuantity());
        entity.imageUrl = blank(request.imageUrl());
        entity.active = request.active();
        return extra(extras.save(entity));
    }

    @Transactional
    public void deleteExtra(String id) {
        guard.requireEnabled();
        extras.delete(requireExtra(id));
    }

    @Transactional(readOnly = true)
    public TravelDtos.PageResponse<TravelDtos.TourResponse> adminTours(
            String search, int page, int size) {
        guard.requireEnabled();
        List<TravelTour> filtered = filterTours(tours.findAll(), search, null, null, false);
        return page(filtered.stream().map(this::tour).toList(), page, size);
    }

    @Transactional
    public TravelDtos.SearchResponse search(
            String search, String destinationSlug, String activitySlug,
            LocalDate date, int travellers) {
        guard.requireEnabled();
        expireHolds();
        List<TravelTour> filtered = filterTours(
                tours.findByStatusOrderByFeaturedDescUpdatedAtDesc(TravelTourStatus.PUBLISHED),
                search, destinationSlug, activitySlug, true);
        if (date != null) {
            ZoneId zone = zone();
            Instant from = date.atStartOfDay(zone).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
            int requiredSeats = Math.max(1, travellers);
            filtered = filtered.stream()
                    .filter(tour -> tour.id != null)
                    .filter(tour -> departures
                            .findByTourIdAndStartsAtBetweenOrderByStartsAtAsc(tour.id, from, to)
                            .stream()
                            .anyMatch(item -> item.status == TravelDepartureStatus.SCHEDULED
                                    && item.availableSeats() >= requiredSeats))
                    .toList();
        }
        return new TravelDtos.SearchResponse(
                filtered.stream().map(this::tour).toList(),
                destinations(true),
                activities(true),
                filtered.size());
    }

    @Transactional(readOnly = true)
    public TravelDtos.TourResponse publicTour(String slug) {
        guard.requireEnabled();
        TravelTour tour = tours.findBySlugIgnoreCase(slug)
                .filter(item -> item.status == TravelTourStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Tour no encontrado"));
        return tour(tour);
    }

    @Transactional
    public TravelDtos.TourResponse saveTour(String id, TravelDtos.TourRequest request) {
        guard.requireEnabled();
        TravelTour entity = id == null ? new TravelTour() : requireTour(id);
        entity.name = request.name().trim();
        entity.slug = uniqueTourSlug(request.slug(), request.name(), id);
        entity.code = uniqueTourCode(request.code(), request.name(), id);
        entity.shortDescription = blank(request.shortDescription());
        entity.description = sanitizedHtml(request.description());
        entity.includedJson = write(request.included());
        entity.excludedJson = write(request.excluded());
        entity.itineraryJson = write(request.itinerary());
        entity.faqJson = write(request.faqs());
        entity.galleryJson = write(request.gallery());
        entity.destination = request.destinationId() == null || request.destinationId().isBlank()
                ? null : requireDestination(request.destinationId());
        entity.activities.clear();
        if (request.activityIds() != null) {
            request.activityIds().stream().filter(Objects::nonNull)
                    .map(this::requireActivity).forEach(entity.activities::add);
        }
        entity.durationValue = Math.max(1, request.durationValue());
        entity.durationUnit = request.durationUnit() == null ? "DAY" : request.durationUnit();
        entity.difficulty = request.difficulty() == null ? "EASY" : request.difficulty().trim().toUpperCase(Locale.ROOT);
        entity.minimumAge = request.minimumAge();
        entity.maximumAge = request.maximumAge();
        entity.minimumGroupSize = Math.max(1, request.minimumGroupSize());
        entity.maximumGroupSize = Math.max(entity.minimumGroupSize, request.maximumGroupSize());
        entity.adultPrice = money(request.adultPrice());
        entity.childPrice = money(request.childPrice());
        entity.infantPrice = money(request.infantPrice());
        entity.currency = normalizeCurrency(request.currency());
        entity.pricingMode = request.pricingMode() == null ? TravelPricingMode.PER_PERSON : request.pricingMode();
        entity.depositPercentage = percent(request.depositPercentage() == null
                ? BigDecimal.valueOf(100) : request.depositPercentage());
        entity.taxExempt = request.taxExempt();
        entity.taxRulesJson = write(normalizeTaxes(request.taxes()));
        entity.featuredImageUrl = blank(request.featuredImageUrl());
        entity.status = request.status() == null ? TravelTourStatus.DRAFT : request.status();
        entity.featured = request.featured();
        entity.confirmationMode = request.confirmationMode() == null ? "INSTANT" : request.confirmationMode();
        TravelTour saved = tours.save(entity);
        events.publish(PluginEvent.of(
                id == null ? "travel.tour.created" : "travel.tour.updated",
                Map.of("tourId", saved.id, "slug", saved.slug, "status", saved.status.name())));
        return tour(saved);
    }

    @Transactional
    public void deleteTour(String id) {
        guard.requireEnabled();
        TravelTour entity = requireTour(id);
        if (departures.findAll().stream().anyMatch(item -> item.tour != null && Objects.equals(item.tour.id, id))) {
            throw new BadRequestException("El tour tiene salidas asociadas");
        }
        tours.delete(entity);
    }

    @Transactional
    public List<TravelDtos.DepartureResponse> departures(
            Instant from, Instant to, String tourId) {
        guard.requireEnabled();
        expireHolds();
        Instant start = from == null ? Instant.now().minus(Duration.ofDays(1)) : from;
        Instant end = to == null ? start.plus(Duration.ofDays(120)) : to;
        List<TravelDeparture> values = tourId == null || tourId.isBlank()
                ? departures.findByStartsAtBetweenOrderByStartsAtAsc(start, end)
                : departures.findByTourIdAndStartsAtBetweenOrderByStartsAtAsc(tourId, start, end);
        return values.stream()
                .filter(this::hasValidDeparture)
                .map(this::departure)
                .toList();
    }

    @Transactional
    public TravelDtos.CalendarResponse calendar(Instant from, Instant to) {
        guard.requireEnabled();
        expireHolds();
        Instant start = from == null ? Instant.now().minus(Duration.ofDays(7)) : from;
        Instant end = to == null ? start.plus(Duration.ofDays(90)) : to;
        if (!end.isAfter(start)) {
            throw new BadRequestException("El final del calendario debe ser posterior al inicio");
        }
        if (Duration.between(start, end).toDays() > 370) {
            throw new BadRequestException("El calendario admite rangos de hasta 370 días");
        }
        return new TravelDtos.CalendarResponse(
                start, end,
                departures.findByStartsAtBetweenOrderByStartsAtAsc(start, end)
                        .stream().filter(this::hasValidDeparture).map(this::departure).toList(),
                bookings.findCalendarBookings(start, end)
                        .stream().filter(this::hasValidBooking).map(this::booking).toList());
    }

    @Transactional
    public TravelDtos.DepartureResponse saveDeparture(
            String id, TravelDtos.DepartureRequest request) {
        guard.requireEnabled();
        TravelDeparture entity = id == null ? new TravelDeparture()
                : departures.findById(id).orElseThrow(() -> new NotFoundException("Salida no encontrada"));
        TravelTour tour = requireTour(request.tourId());
        if (request.startsAt().isBefore(Instant.now().minus(Duration.ofHours(1)))) {
            throw new BadRequestException("La salida no puede iniciar en el pasado");
        }
        if (request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("La fecha final debe ser posterior al inicio de la salida");
        }
        entity.tour = tour;
        entity.startsAt = request.startsAt();
        entity.endsAt = request.endsAt();
        entity.capacity = Math.max(1, request.capacity());
        entity.blockedSeats = Math.max(0, request.blockedSeats());
        if (entity.reservedSeats + entity.heldSeats + entity.blockedSeats > entity.capacity) {
            throw new BadRequestException("La capacidad es menor que los cupos ocupados o bloqueados");
        }
        entity.status = request.status() == null ? TravelDepartureStatus.SCHEDULED : request.status();
        entity.adultPrice = request.adultPrice() == null ? null : money(request.adultPrice());
        entity.childPrice = request.childPrice() == null ? null : money(request.childPrice());
        entity.infantPrice = request.infantPrice() == null ? null : money(request.infantPrice());
        entity.meetingPoint = blank(request.meetingPoint());
        entity.notes = blank(request.notes());
        entity.googleCalendarUrl = googleCalendarUrl(entity, null);
        TravelDeparture saved = departures.save(entity);
        events.publish(PluginEvent.of(
                id == null ? "travel.departure.created" : "travel.departure.updated",
                Map.of("departureId", saved.id, "tourId", tour.id, "startsAt", saved.startsAt.toString())));
        return departure(saved);
    }

    @Transactional
    public void deleteDeparture(String id) {
        guard.requireEnabled();
        TravelDeparture entity = departures.findById(id)
                .orElseThrow(() -> new NotFoundException("Salida no encontrada"));
        if (entity.reservedSeats > 0 || entity.heldSeats > 0) {
            throw new BadRequestException("La salida tiene reservaciones o retenciones activas");
        }
        departures.delete(entity);
    }

    @Transactional
    public TravelDtos.BookingResponse createBooking(
            TravelDtos.BookingQuoteRequest request, String authenticatedEmail) {
        guard.requireEnabled();
        expireHolds();
        TravelSettings settings = settings();
        if (!settings.guestBooking && (authenticatedEmail == null || authenticatedEmail.isBlank())) {
            throw new BadRequestException("Debe iniciar sesión en Mi Cuenta para reservar");
        }
        UserAccount authenticatedAccount = null;
        if (authenticatedEmail != null && !authenticatedEmail.isBlank()) {
            try {
                authenticatedAccount = users.requireByEmail(authenticatedEmail);
            } catch (RuntimeException ignored) {
                authenticatedAccount = null;
            }
        }
        if (authenticatedAccount != null
                && authenticatedAccount.getRole() == UserRole.CUSTOMER
                && !authenticatedAccount.getEmail().equalsIgnoreCase(request.customerEmail())) {
            throw new BadRequestException("La reservación debe utilizar el correo de la cuenta autenticada");
        }
        TravelDeparture departure = departures.findLockedById(request.departureId())
                .orElseThrow(() -> new NotFoundException("Salida no encontrada"));
        if (departure.status != TravelDepartureStatus.SCHEDULED) {
            throw new BadRequestException("La salida no está disponible");
        }
        if (departure.startsAt.isBefore(Instant.now().plus(Duration.ofHours(settings.minimumLeadHours)))) {
            throw new BadRequestException("La salida ya no admite reservaciones por el tiempo mínimo de anticipación");
        }
        int seats = request.adults() + request.children() + request.infants();
        if (seats <= 0) throw new BadRequestException("Seleccione al menos un viajero");
        if (seats < departure.tour.minimumGroupSize || seats > departure.tour.maximumGroupSize) {
            throw new BadRequestException("La cantidad de viajeros no cumple el rango permitido para este tour");
        }
        if (departure.availableSeats() < seats) {
            throw new BadRequestException("No hay cupos suficientes para esta salida");
        }
        if (request.travellers() != null && !request.travellers().isEmpty()
                && request.travellers().size() != seats) {
            throw new BadRequestException("La cantidad de viajeros detallados no coincide con la reserva");
        }

        BigDecimal adultPrice = departure.adultPrice == null ? departure.tour.adultPrice : departure.adultPrice;
        BigDecimal childPrice = departure.childPrice == null ? departure.tour.childPrice : departure.childPrice;
        BigDecimal infantPrice = departure.infantPrice == null ? departure.tour.infantPrice : departure.infantPrice;
        BigDecimal subtotal = departure.tour.pricingMode == TravelPricingMode.PER_GROUP
                ? adultPrice
                : adultPrice.multiply(BigDecimal.valueOf(request.adults()))
                    .add(childPrice.multiply(BigDecimal.valueOf(request.children())))
                    .add(infantPrice.multiply(BigDecimal.valueOf(request.infants())));
        Map<String, Object> selectedExtras = new LinkedHashMap<>();
        BigDecimal extrasTotal = BigDecimal.ZERO;
        if (request.extras() != null) {
            for (Map.Entry<String, Integer> entry : request.extras().entrySet()) {
                int quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (quantity <= 0) continue;
                TravelExtra extra = extras.findBySlugIgnoreCase(entry.getKey())
                        .filter(item -> item.active)
                        .orElseThrow(() -> new BadRequestException("Servicio adicional no disponible: " + entry.getKey()));
                if (!extra.currency.equalsIgnoreCase(departure.tour.currency)) {
                    throw new BadRequestException("El servicio adicional utiliza otra moneda");
                }
                quantity = Math.min(quantity, extra.maxQuantity);
                BigDecimal multiplier = switch (extra.pricingUnit) {
                    case "PER_PERSON" -> BigDecimal.valueOf(seats * quantity);
                    default -> BigDecimal.valueOf(quantity);
                };
                BigDecimal lineTotal = extra.price.multiply(multiplier);
                extrasTotal = extrasTotal.add(lineTotal);
                selectedExtras.put(extra.slug, Map.of(
                        "name", extra.name,
                        "quantity", quantity,
                        "pricingUnit", extra.pricingUnit,
                        "unitPrice", extra.price,
                        "total", money(lineTotal)));
            }
        }

        subtotal = money(subtotal);
        extrasTotal = money(extrasTotal);
        BigDecimal taxableBase = money(subtotal.add(extrasTotal));
        List<TravelDtos.TaxLine> taxLines = calculateTaxes(departure.tour, settings, taxableBase);
        BigDecimal tax = money(taxLines.stream()
                .map(TravelDtos.TaxLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = money(taxableBase.add(tax));
        BigDecimal deposit = money(total.multiply(departure.tour.depositPercentage)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));

        TravelBooking booking = new TravelBooking();
        booking.bookingCode = bookingCode();
        booking.tour = departure.tour;
        booking.departure = departure;
        booking.customerEmail = request.customerEmail().trim().toLowerCase(Locale.ROOT);
        booking.customerName = request.customerName().trim();
        booking.customerPhone = blank(request.customerPhone());
        booking.adultCount = request.adults();
        booking.childCount = request.children();
        booking.infantCount = request.infants();
        booking.currency = departure.tour.currency;
        booking.subtotal = subtotal;
        booking.extrasTotal = extrasTotal;
        booking.discountTotal = BigDecimal.ZERO.setScale(2);
        booking.taxTotal = tax;
        booking.taxBreakdownJson = write(taxLines);
        booking.grandTotal = total;
        booking.depositRequired = deposit;
        booking.amountPaid = BigDecimal.ZERO.setScale(2);
        booking.balanceDue = total;
        booking.extrasJson = write(selectedExtras);
        booking.notes = blank(request.notes());
        booking.holdExpiresAt = Instant.now().plus(Duration.ofMinutes(settings.bookingHoldMinutes));
        if (authenticatedAccount != null
                && authenticatedAccount.getEmail().equalsIgnoreCase(booking.customerEmail)) {
            booking.accountId = authenticatedAccount.getId();
        }
        if (request.travellers() != null) {
            request.travellers().forEach(item -> booking.travellers.add(traveller(booking, item)));
        }
        departure.heldSeats += seats;
        departures.save(departure);
        TravelBooking saved = bookings.save(booking);
        events.publish(PluginEvent.of("travel.booking.created", Map.of(
                "bookingId", saved.id,
                "bookingCode", saved.bookingCode,
                "tourId", saved.tour.id,
                "tourName", saved.tour.name,
                "customerEmail", saved.customerEmail,
                "customerName", saved.customerName,
                "total", saved.grandTotal,
                "currency", saved.currency,
                "startsAt", saved.departure.startsAt.toString())));
        mail.send(
                saved.customerEmail,
                "Reservación recibida · " + saved.bookingCode,
                "Hola " + saved.customerName + ",\n\n"
                        + "Recibimos su reservación para " + saved.tour.name + ".\n"
                        + "Fecha: " + saved.departure.startsAt + "\n"
                        + "Total: " + saved.currency + " " + saved.grandTotal + "\n"
                        + "Pago requerido ahora: " + saved.currency + " " + saved.depositRequired + "\n\n"
                        + "Su código es " + saved.bookingCode + ".");
        notifyOperations(
                settings.notificationEmail,
                "Nueva reservación · " + saved.bookingCode,
                "Cliente: " + saved.customerName + " <" + saved.customerEmail + ">\n"
                        + "Tour: " + saved.tour.name + "\n"
                        + "Fecha: " + saved.departure.startsAt + "\n"
                        + "Viajeros: " + saved.seats() + "\n"
                        + "Total: " + saved.currency + " " + saved.grandTotal + "\n"
                        + "Estado: pendiente de pago");
        return booking(saved);
    }

    @Transactional(readOnly = true)
    public TravelDtos.PageResponse<TravelDtos.BookingResponse> bookings(int page, int size) {
        guard.requireEnabled();
        var result = bookings.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 500)));
        return new TravelDtos.PageResponse<>(
                result.getContent().stream().map(this::booking).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TravelDtos.BookingResponse booking(String idOrCode, String email) {
        guard.requireEnabled();
        TravelBooking entity = bookings.findById(idOrCode)
                .or(() -> bookings.findByBookingCodeIgnoreCase(idOrCode))
                .orElseThrow(() -> new NotFoundException("Reservación no encontrada"));
        if (email != null && !entity.customerEmail.equalsIgnoreCase(email)) {
            throw new NotFoundException("Reservación no encontrada");
        }
        return booking(entity);
    }

    @Transactional
    public TravelDtos.BookingResponse cancel(String id, String actorEmail) {
        guard.requireEnabled();
        TravelBooking entity = bookings.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Reservación no encontrada"));
        if (actorEmail != null && !entity.customerEmail.equalsIgnoreCase(actorEmail)) {
            throw new NotFoundException("Reservación no encontrada");
        }
        if (entity.bookingStatus == TravelBookingStatus.CANCELLED) return booking(entity);
        if (entity.bookingStatus == TravelBookingStatus.COMPLETED) {
            throw new BadRequestException("Un viaje completado no puede cancelarse");
        }
        if (entity.amountPaid != null && entity.amountPaid.signum() > 0) {
            throw new BadRequestException(
                    "La reservación tiene pagos aplicados. Procese primero el reembolso desde administración");
        }
        releaseSeats(entity);
        entity.bookingStatus = TravelBookingStatus.CANCELLED;
        entity.cancelledAt = Instant.now();
        TravelBooking saved = bookings.save(entity);
        events.publish(PluginEvent.of("travel.booking.cancelled", Map.of(
                "bookingId", saved.id,
                "bookingCode", saved.bookingCode,
                "customerEmail", saved.customerEmail)));
        return booking(saved);
    }

    @Transactional
    public void fulfill(String bookingId, SellableProvider.FulfillmentRequest request) {
        guard.requireEnabled();
        TravelBooking booking = bookings.findLockedById(bookingId)
                .orElseThrow(() -> new NotFoundException("Reservación no encontrada"));
        if (booking.paymentStatus == TravelPaymentStatus.PAID) return;
        if (booking.bookingStatus == TravelBookingStatus.CANCELLED
                || booking.bookingStatus == TravelBookingStatus.EXPIRED) {
            throw new BadRequestException("La reservación ya no puede confirmarse");
        }

        boolean initialPayment = booking.bookingStatus == TravelBookingStatus.PENDING_PAYMENT;
        BigDecimal paymentAmount = money(request.lineTotal());
        BigDecimal appliedDiscount = money(request.discountAmount());
        if (paymentAmount.signum() <= 0) {
            throw new BadRequestException("El pago de la reservación no tiene un importe válido");
        }
        BigDecimal expectedAmount = money(initialPayment ? booking.depositRequired : booking.balanceDue);
        if (paymentAmount.add(appliedDiscount).compareTo(expectedAmount) > 0) {
            throw new BadRequestException("El pago excede el importe pendiente de la reservación");
        }

        if (initialPayment) {
            TravelDeparture departure = departures.findLockedById(booking.departure.id)
                    .orElseThrow(() -> new NotFoundException("Salida no encontrada"));
            int seats = booking.seats();
            departure.heldSeats = Math.max(0, departure.heldSeats - seats);
            departure.reservedSeats += seats;
            if (departure.availableSeats() == 0) departure.status = TravelDepartureStatus.SOLD_OUT;
            departures.save(departure);
            booking.departure = departure;
            booking.bookingStatus = TravelBookingStatus.CONFIRMED;
            booking.confirmedAt = Instant.now();
            booking.holdExpiresAt = null;
        }

        if (appliedDiscount.signum() > 0) {
            booking.discountTotal = money(booking.discountTotal.add(appliedDiscount));
            booking.grandTotal = money(booking.grandTotal.subtract(appliedDiscount).max(BigDecimal.ZERO));
            booking.couponCode = request.couponCode();
        }
        booking.amountPaid = money(booking.amountPaid.add(paymentAmount).min(booking.grandTotal));
        booking.balanceDue = money(booking.grandTotal.subtract(booking.amountPaid).max(BigDecimal.ZERO));
        booking.paymentStatus = booking.balanceDue.signum() == 0
                ? TravelPaymentStatus.PAID
                : TravelPaymentStatus.PARTIALLY_PAID;
        booking.orderId = request.orderId();
        booking.orderNumber = request.orderNumber();
        bookings.save(booking);

        Map<String, Object> paymentEvent = new LinkedHashMap<>();
        paymentEvent.put("bookingId", booking.id);
        paymentEvent.put("bookingCode", booking.bookingCode);
        paymentEvent.put("orderId", request.orderId());
        paymentEvent.put("orderNumber", request.orderNumber());
        paymentEvent.put("customerEmail", booking.customerEmail);
        paymentEvent.put("customerName", booking.customerName);
        paymentEvent.put("tourName", booking.tour.name);
        paymentEvent.put("startsAt", booking.departure.startsAt.toString());
        paymentEvent.put("paymentAmount", paymentAmount);
        paymentEvent.put("discountAmount", appliedDiscount);
        if (request.couponCode() != null) paymentEvent.put("couponCode", request.couponCode());
        paymentEvent.put("amountPaid", booking.amountPaid);
        paymentEvent.put("balanceDue", booking.balanceDue);
        paymentEvent.put("currency", booking.currency);
        paymentEvent.put("paymentStatus", booking.paymentStatus.name());
        paymentEvent.put("googleCalendarUrl", googleCalendarUrl(booking.departure, booking));
        events.publish(PluginEvent.of(
                initialPayment ? "travel.booking.confirmed" : "travel.payment.completed",
                paymentEvent));
        if (booking.paymentStatus == TravelPaymentStatus.PAID) {
            events.publish(PluginEvent.of("travel.booking.paid", paymentEvent));
        }

        String paymentMessage = initialPayment
                ? "Su primer pago fue confirmado y la reservación ya está activa."
                : "Registramos el pago de su saldo pendiente.";
        mail.send(
                booking.customerEmail,
                (initialPayment ? "Viaje confirmado · " : "Pago recibido · ") + booking.bookingCode,
                "Hola " + booking.customerName + ",\n\n"
                        + paymentMessage + "\n"
                        + "Tour: " + booking.tour.name + "\n"
                        + "Fecha: " + booking.departure.startsAt + "\n"
                        + "Orden: " + request.orderNumber() + "\n"
                        + "Pagado acumulado: " + booking.currency + " " + booking.amountPaid + "\n"
                        + "Saldo pendiente: " + booking.currency + " " + booking.balanceDue + "\n\n"
                        + "Agregar a Google Calendar: "
                        + googleCalendarUrl(booking.departure, booking));
        notifyOperations(
                settings().notificationEmail,
                "Pago de viaje recibido · " + booking.bookingCode,
                "Cliente: " + booking.customerName + " <" + booking.customerEmail + ">\n"
                        + "Tour: " + booking.tour.name + "\n"
                        + "Orden: " + request.orderNumber() + "\n"
                        + "Pago: " + booking.currency + " " + paymentAmount + "\n"
                        + (appliedDiscount.signum() > 0 ? "Descuento: " + booking.currency + " " + appliedDiscount + "\n" : "")
                        + "Saldo: " + booking.currency + " " + booking.balanceDue);
    }

    @Transactional(readOnly = true)
    public Optional<com.nattechnologies.trastcms.plugin.api.SellableProvider.SellableItem> sellable(String bookingId) {
        guard.requireEnabled();
        return bookings.findById(bookingId)
                .filter(item -> {
                    boolean activeHold = item.bookingStatus == TravelBookingStatus.PENDING_PAYMENT
                            && item.holdExpiresAt != null
                            && item.holdExpiresAt.isAfter(Instant.now());
                    boolean pendingBalance = item.bookingStatus == TravelBookingStatus.CONFIRMED
                            && item.paymentStatus == TravelPaymentStatus.PARTIALLY_PAID
                            && item.balanceDue.signum() > 0;
                    return activeHold || pendingBalance;
                })
                .map(item -> {
                    boolean balancePayment = item.bookingStatus == TravelBookingStatus.CONFIRMED;
                    BigDecimal requiredNow = item.depositRequired == null || item.depositRequired.signum() <= 0
                            ? item.grandTotal
                            : item.depositRequired;
                    BigDecimal paymentAmount = balancePayment ? item.balanceDue : requiredNow;
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("bookingCode", item.bookingCode);
                    metadata.put("tourSlug", item.tour.slug);
                    metadata.put("departureId", item.departure.id);
                    metadata.put("startsAt", item.departure.startsAt.toString());
                    metadata.put("travellers", item.seats());
                    metadata.put("grandTotal", item.grandTotal);
                    metadata.put("depositRequired", item.depositRequired);
                    metadata.put("balanceAfterPayment", money(item.balanceDue.subtract(paymentAmount).max(BigDecimal.ZERO)));
                    metadata.put("travelPaymentKind", balancePayment ? "BALANCE" : "DEPOSIT");
                    metadata.put("travelPaymentAmount", paymentAmount);
                    metadata.put("taxable", false);
                    return new SellableProvider.SellableItem(
                            "travel",
                            item.id,
                            "TRAVEL_BOOKING",
                            (balancePayment ? "Saldo · " : "") + item.tour.name + " · " + item.bookingCode,
                            balancePayment ? "Pago del saldo pendiente" : "Salida " + item.departure.startsAt,
                            paymentAmount,
                            item.currency,
                            false,
                            null,
                            false,
                            item.tour.featuredImageUrl,
                            true,
                            metadata);
                });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> accountBookings(String email) {
        if (email == null || email.isBlank()) return List.of();
        return bookings.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(item -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("id", item.id);
                    value.put("type", "TRAVEL_BOOKING");
                    value.put("bookingCode", item.bookingCode);
                    value.put("title", item.tour.name);
                    value.put("tourName", item.tour.name);
                    value.put("tourSlug", item.tour.slug);
                    value.put("description", item.tour.shortDescription);
                    value.put("imageUrl", item.tour.featuredImageUrl);
                    value.put("destination", item.tour.destination == null ? null : item.tour.destination.name);
                    value.put("status", item.bookingStatus.name());
                    value.put("paymentStatus", item.paymentStatus.name());
                    value.put("startsAt", item.departure.startsAt);
                    value.put("endsAt", item.departure.endsAt);
                    value.put("meetingPoint", item.departure.meetingPoint);
                    value.put("adults", item.adultCount);
                    value.put("children", item.childCount);
                    value.put("infants", item.infantCount);
                    value.put("travellerCount", item.seats());
                    value.put("customerName", item.customerName);
                    value.put("customerPhone", item.customerPhone);
                    value.put("subtotal", item.subtotal);
                    value.put("taxTotal", item.taxTotal);
                    value.put("total", item.grandTotal);
                    value.put("amountPaid", item.amountPaid);
                    value.put("balanceDue", item.balanceDue);
                    value.put("depositRequired", item.depositRequired);
                    value.put("currency", item.currency);
                    value.put("createdAt", item.createdAt);
                    value.put("publicUrl", "/travel/booking/" + item.bookingCode);
                    value.put("tourUrl", "/travel/" + item.tour.slug);
                    value.put("googleCalendarUrl", googleCalendarUrl(item.departure, item));
                    return Collections.unmodifiableMap(value);
                }).toList();
    }

    @Transactional(readOnly = true)
    public TravelDtos.SettingsResponse settingsResponse() {
        guard.requireEnabled();
        return settings(settings());
    }

    @Transactional
    public TravelDtos.SettingsResponse saveSettings(TravelDtos.SettingsRequest request) {
        guard.requireEnabled();
        TravelSettings settings = settings();
        settings.defaultCurrency = normalizeCurrency(request.defaultCurrency());
        settings.timezone = request.timezone() == null || request.timezone().isBlank()
                ? "America/Tegucigalpa" : request.timezone().trim();
        try { ZoneId.of(settings.timezone); }
        catch (Exception exception) { throw new BadRequestException("Zona horaria inválida"); }
        settings.taxRate = percent(request.taxRate());
        settings.bookingHoldMinutes = request.bookingHoldMinutes();
        settings.minimumLeadHours = request.minimumLeadHours();
        settings.guestBooking = request.guestBooking();
        settings.googleCalendarEnabled = request.googleCalendarEnabled();
        settings.googleCalendarId = blank(request.googleCalendarId());
        settings.notificationEmail = blank(request.notificationEmail());
        settings.termsUrl = blank(request.termsUrl());
        return settings(settingsRepository.save(settings));
    }

    @Transactional
    public TravelDtos.DemoDataResponse generateDemoData() {
        guard.requireEnabled();
        if (!settingsRepository.existsById(TravelSettings.currentId())) {
            settingsRepository.save(new TravelSettings());
        }
        return generateDemoDataInternal();
    }

    @Transactional
    public TravelDtos.DemoDataResponse restoreSourceSnapshot() {
        guard.requireEnabled();
        if (!settingsRepository.existsById(TravelSettings.currentId())) {
            settingsRepository.save(new TravelSettings());
        }
        return restoreSourceSnapshotInternal();
    }

    private record TravelDemoCatalog(
            String id,
            String name,
            String source,
            String restApi,
            String capturedAt,
            List<TravelDemoDestination> destinations,
            List<TravelDemoActivity> activities,
            List<TravelDemoExtra> extras,
            List<TravelDemoTour> tours) { }

    private record TravelDemoDestination(
            String name, String slug, String summary, String country,
            String region, String city, String image, boolean featured) { }

    private record TravelDemoActivity(
            String name, String slug, String icon,
            String description, String image) { }

    private record TravelDemoExtra(
            String name, String slug, String unit, BigDecimal price) { }

    private record TravelDemoFaq(String question, String answer) { }

    private record TravelDemoItinerary(String title, String time, String description) { }

    private record TravelDemoTour(
            String name, String slug, String code, String destination,
            List<String> activities, BigDecimal adultPrice,
            BigDecimal childPrice, BigDecimal infantPrice,
            String pricingMode, int durationValue, String durationUnit,
            String difficulty, int maxGroup, boolean featured,
            String summary, String description, String image,
            List<String> gallery, List<String> included, List<String> excluded,
            List<TravelDemoFaq> faqs, List<TravelDemoItinerary> itinerary,
            BigDecimal depositPercentage, String confirmationMode,
            String meetingPoint, String sourceUrl) { }

    private TravelDtos.DemoDataResponse generateDemoDataInternal() {
        return restoreCatalogInternal(false);
    }

    private TravelDtos.DemoDataResponse restoreSourceSnapshotInternal() {
        return restoreCatalogInternal(true);
    }

    private TravelDtos.DemoDataResponse restoreCatalogInternal(boolean persistentSource) {
        TravelDemoCatalog catalog = readDemoCatalog();
        if (!persistentSource) clearDemoCatalogForRefresh();

        Map<String, TravelDestination> destinationMap = new LinkedHashMap<>();
        for (TravelDemoDestination item : safe(catalog.destinations())) {
            createDestinationCatalog(
                    item.name(), item.slug(), item.summary(), item.country(),
                    item.region(), item.city(), item.image(), item.featured(), persistentSource)
                    .ifPresent(destination -> destinationMap.put(destination.slug, destination));
        }

        for (TravelDemoActivity item : safe(catalog.activities())) {
            createActivityCatalog(item, persistentSource);
        }
        for (TravelDemoExtra item : safe(catalog.extras())) {
            createExtraCatalog(item.name(), item.slug(), item.unit(), item.price(), persistentSource);
        }
        for (TravelDemoTour item : safe(catalog.tours())) {
            TravelDestination destination = destinationMap.get(item.destination());
            if (destination == null) {
                destination = destinations.findBySlugIgnoreCase(item.destination()).orElse(null);
            }
            createTourCatalog(item, destination, persistentSource);
        }

        int departuresCreated = 0;
        ZoneId zone = zone();
        for (TravelDemoTour demo : safe(catalog.tours())) {
            Optional<TravelTour> persisted = tours.findBySlugIgnoreCase(demo.slug());
            if (persisted.isEmpty()) continue;
            TravelTour tour = persisted.get();
            if (!persistentSource && !tour.demoData) continue;
            if (persistentSource) {
                for (TravelDeparture existingDeparture : departures.findByTourId(tour.id)) {
                    if (!existingDeparture.demoData) continue;
                    existingDeparture.demoData = false;
                    departures.save(existingDeparture);
                }
            }
            if (!departures.findByTourIdAndStartsAtAfterOrderByStartsAtAsc(
                    tour.id, Instant.now()).isEmpty()) continue;
            for (int day : List.of(3, 7, 14, 21, 28)) {
                TravelDeparture departure = new TravelDeparture();
                departure.tour = tour;
                departure.startsAt = ZonedDateTime.now(zone)
                        .plusDays(day).withHour(8 + day % 3).withMinute(0)
                        .withSecond(0).withNano(0).toInstant();
                departure.endsAt = departure.startsAt.plus(tourDuration(tour));
                departure.capacity = Math.max(1, tour.maximumGroupSize);
                departure.meetingPoint = blank(demo.meetingPoint()) == null
                        ? "Punto de encuentro"
                        : demo.meetingPoint().trim();
                departure.demoData = !persistentSource;
                departure.googleCalendarUrl = googleCalendarUrl(departure, null);
                departures.save(departure);
                departuresCreated++;
            }
        }
        String catalogName = blank(catalog.name()) == null ? "catálogo turístico" : catalog.name().trim();
        return new TravelDtos.DemoDataResponse(
                safe(catalog.destinations()).size(),
                safe(catalog.activities()).size(),
                safe(catalog.tours()).size(),
                departuresCreated,
                safe(catalog.extras()).size(),
                persistentSource
                        ? "Catálogo persistente de " + catalogName + " restaurado"
                        : "Catálogo demo de " + catalogName + " cargado");
    }

    @Transactional
    public TravelDtos.DemoDataResponse deleteDemoData() {
        guard.requireEnabled();
        int destinationCount = (int) destinations.countByDemoDataTrue();
        int activityCount = (int) activities.countByDemoDataTrue();
        int tourCount = (int) tours.countByDemoDataTrue();
        int departureCount = (int) departures.countByDemoDataTrue();
        int extraCount = (int) extras.countByDemoDataTrue();
        if (hasReferencedDemoData()) {
            throw new BadRequestException(
                    "No se pueden eliminar los datos demo porque existen reservaciones asociadas");
        }
        deleteDemoCatalog();
        return new TravelDtos.DemoDataResponse(
                destinationCount, activityCount, tourCount, departureCount, extraCount,
                "Datos turísticos de demostración eliminados");
    }

    private TravelDemoCatalog readDemoCatalog() {
        String resource = "/demo/travel-demo.json";
        try (java.io.InputStream input = TravelService.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new BadRequestException(
                        "No se encontró el catálogo turístico de migración para el tema activo");
            }
            return mapper.readValue(input, TravelDemoCatalog.class);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException(
                    "No se pudo leer el catálogo turístico de migración para el tema activo");
        }
    }

    private boolean hasReferencedDemoData() {
        return bookings.findAll().stream().anyMatch(booking ->
                booking.demoData
                        || (booking.tour != null && booking.tour.demoData)
                        || (booking.departure != null && booking.departure.demoData));
    }

    private void clearDemoCatalogForRefresh() {
        if (!hasReferencedDemoData()) deleteDemoCatalog();
    }

    private void deleteDemoCatalog() {
        departures.deleteByDemoDataTrue();
        tours.deleteByDemoDataTrue();
        activities.deleteByDemoDataTrue();
        destinations.deleteByDemoDataTrue();
        extras.deleteByDemoDataTrue();
    }

    private Duration tourDuration(TravelTour tour) {
        int value = Math.max(1, tour.durationValue);
        return switch (Optional.ofNullable(tour.durationUnit)
                .orElse("HOUR").toUpperCase(Locale.ROOT)) {
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(value);
            case "DAY", "DAYS" -> Duration.ofHours(Math.max(8, value * 8L));
            default -> Duration.ofHours(value);
        };
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Optional<TravelDestination> createDestinationCatalog(
            String name, String slug, String summary, String country,
            String region, String city, String image, boolean featured, boolean persistentSource) {
        Optional<TravelDestination> existing = destinations.findBySlugIgnoreCase(slug);
        if (existing.isPresent() && !existing.get().demoData && !persistentSource) {
            return existing;
        }
        TravelDestination entity = existing.orElseGet(TravelDestination::new);
        entity.name = name;
        entity.slug = slug;
        entity.summary = summary;
        entity.description = summary;
        entity.country = country;
        entity.region = region;
        entity.city = city;
        entity.imageUrl = image;
        entity.featured = featured;
        entity.active = true;
        entity.demoData = !persistentSource;
        return Optional.of(destinations.save(entity));
    }

    private Optional<TravelActivity> createActivityCatalog(TravelDemoActivity item, boolean persistentSource) {
        Optional<TravelActivity> existing = activities.findBySlugIgnoreCase(item.slug());
        if (existing.isPresent() && !existing.get().demoData && !persistentSource) {
            return existing;
        }
        TravelActivity entity = existing.orElseGet(TravelActivity::new);
        entity.name = item.name();
        entity.slug = item.slug();
        entity.icon = item.icon();
        entity.description = item.description();
        entity.imageUrl = item.image();
        entity.active = true;
        entity.demoData = !persistentSource;
        return Optional.of(activities.save(entity));
    }

    private void createExtraCatalog(
            String name, String slug, String unit, BigDecimal price, boolean persistentSource) {
        Optional<TravelExtra> existing = extras.findBySlugIgnoreCase(slug);
        if (existing.isPresent() && !existing.get().demoData && !persistentSource) return;
        TravelExtra entity = existing.orElseGet(TravelExtra::new);
        entity.name = name;
        entity.slug = slug;
        entity.pricingUnit = unit;
        entity.price = money(price);
        entity.currency = "USD";
        entity.active = true;
        entity.demoData = !persistentSource;
        entity.maxQuantity = 20;
        extras.save(entity);
    }

    private void createTourCatalog(
            TravelDemoTour item, TravelDestination destination, boolean persistentSource) {
        Optional<TravelTour> existing = tours.findBySlugIgnoreCase(item.slug());
        if (existing.isPresent() && !existing.get().demoData && !persistentSource) return;
        TravelTour tour = existing.orElseGet(TravelTour::new);
        tour.name = item.name();
        tour.slug = item.slug();
        tour.code = item.code();
        tour.shortDescription = item.summary();
        tour.description = item.description();
        tour.destination = destination;
        tour.durationValue = Math.max(1, item.durationValue());
        tour.durationUnit = Optional.ofNullable(item.durationUnit()).orElse("DAY");
        tour.difficulty = Optional.ofNullable(item.difficulty()).orElse("EASY");
        tour.minimumGroupSize = 1;
        tour.maximumGroupSize = Math.max(1, item.maxGroup());
        tour.adultPrice = money(item.adultPrice());
        tour.childPrice = money(item.childPrice());
        tour.infantPrice = money(item.infantPrice());
        tour.currency = "USD";
        try {
            tour.pricingMode = TravelPricingMode.valueOf(
                    Optional.ofNullable(item.pricingMode())
                            .orElse("PER_PERSON").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            tour.pricingMode = TravelPricingMode.PER_PERSON;
        }
        tour.depositPercentage = percent(item.depositPercentage() == null
                ? BigDecimal.valueOf(100) : item.depositPercentage());
        tour.taxExempt = true;
        tour.taxRulesJson = write(List.of());
        tour.featuredImageUrl = item.image();
        tour.status = TravelTourStatus.PUBLISHED;
        tour.featured = item.featured();
        tour.demoData = !persistentSource;
        String confirmationMode = Optional.ofNullable(item.confirmationMode()).orElse("INSTANT").toUpperCase(Locale.ROOT);
        tour.confirmationMode = Set.of("INSTANT", "MANUAL").contains(confirmationMode) ? confirmationMode : "INSTANT";
        tour.includedJson = write(safe(item.included()));
        tour.excludedJson = write(safe(item.excluded()));
        List<Map<String, String>> itinerary = safe(item.itinerary()).stream()
                .filter(entry -> blank(entry.title()) != null)
                .map(entry -> Map.of(
                        "title", entry.title().trim(),
                        "time", Optional.ofNullable(blank(entry.time())).orElse(""),
                        "description", Optional.ofNullable(blank(entry.description())).orElse("")))
                .toList();
        if (itinerary.isEmpty()) {
            itinerary = List.of(
                    Map.of(
                            "title", "Meeting and check-in",
                            "time", "",
                            "description", "Traveler verification and operational briefing at "
                                    + Optional.ofNullable(item.meetingPoint()).orElse("the meeting point") + "."),
                    Map.of(
                            "title", item.name(),
                            "time", "",
                            "description", item.summary()),
                    Map.of(
                            "title", "Return or completion",
                            "time", "",
                            "description", "End of the experience according to the confirmed schedule."));
        }
        tour.itineraryJson = write(itinerary);

        List<Map<String, String>> faqs = safe(item.faqs()).stream()
                .filter(entry -> blank(entry.question()) != null && blank(entry.answer()) != null)
                .map(entry -> Map.of("question", entry.question().trim(), "answer", entry.answer().trim()))
                .toList();
        if (faqs.isEmpty()) {
            faqs = List.of(
                    Map.of(
                            "question", "Is transportation included?",
                            "answer", "Transportation depends on the selected package. Confirm the pickup details before booking."),
                    Map.of(
                            "question", "Can the tour be customized?",
                            "answer", "Contact the operator to confirm adjustments for your time, interests and group size."));
        }
        tour.faqJson = write(faqs);
        List<String> gallery = safe(item.gallery()).stream()
                .filter(value -> blank(value) != null)
                .map(String::trim)
                .distinct()
                .toList();
        tour.galleryJson = write(gallery.isEmpty() ? List.of(item.image()) : gallery);
        tour.activities.clear();
        safe(item.activities()).stream()
                .map(activities::findBySlugIgnoreCase)
                .flatMap(Optional::stream)
                .forEach(tour.activities::add);
        tours.save(tour);
    }

    private List<TravelTour> filterTours(
            List<TravelTour> source,
            String search,
            String destinationSlug,
            String activitySlug,
            boolean publishedOnly) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(Objects::nonNull)
                .filter(item -> !publishedOnly || validTour(item))
                .filter(item -> term.isBlank()
                        || Optional.ofNullable(item.name).orElse("").toLowerCase(Locale.ROOT).contains(term)
                        || Optional.ofNullable(item.shortDescription).orElse("").toLowerCase(Locale.ROOT).contains(term)
                        || Optional.ofNullable(item.code).orElse("").toLowerCase(Locale.ROOT).contains(term))
                .filter(item -> destinationSlug == null || destinationSlug.isBlank()
                        || item.destination != null
                        && Optional.ofNullable(item.destination.slug).orElse("").equalsIgnoreCase(destinationSlug))
                .filter(item -> activitySlug == null || activitySlug.isBlank()
                        || item.activities.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(activity -> Optional.ofNullable(activity.slug).orElse("")
                                .equalsIgnoreCase(activitySlug)))
                .sorted(Comparator.comparing((TravelTour item) -> !item.featured)
                        .thenComparing(
                                item -> Optional.ofNullable(item.name).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void expireHolds() {
        List<TravelBooking> expired = bookings.findByBookingStatusAndHoldExpiresAtBefore(
                TravelBookingStatus.PENDING_PAYMENT, Instant.now());
        for (TravelBooking candidate : expired) {
            TravelBooking booking = bookings.findLockedById(candidate.id).orElse(null);
            if (booking == null
                    || booking.bookingStatus != TravelBookingStatus.PENDING_PAYMENT
                    || booking.holdExpiresAt == null
                    || !booking.holdExpiresAt.isBefore(Instant.now())) {
                continue;
            }
            booking.departure = departures.findLockedById(booking.departure.id)
                    .orElseThrow(() -> new NotFoundException("Salida no encontrada"));
            releaseSeats(booking);
            booking.bookingStatus = TravelBookingStatus.EXPIRED;
            booking.holdExpiresAt = null;
            bookings.save(booking);
        }
    }

    private void releaseSeats(TravelBooking booking) {
        if (booking.bookingStatus == TravelBookingStatus.PENDING_PAYMENT) {
            booking.departure.heldSeats = Math.max(0, booking.departure.heldSeats - booking.seats());
        } else if (booking.bookingStatus == TravelBookingStatus.CONFIRMED) {
            booking.departure.reservedSeats = Math.max(0, booking.departure.reservedSeats - booking.seats());
        }
        if (booking.departure.status == TravelDepartureStatus.SOLD_OUT
                && booking.departure.availableSeats() > 0) {
            booking.departure.status = TravelDepartureStatus.SCHEDULED;
        }
        departures.save(booking.departure);
    }

    private TravelTraveller traveller(TravelBooking booking, TravelDtos.TravellerRequest request) {
        TravelTraveller entity = new TravelTraveller();
        entity.booking = booking;
        entity.firstName = request.firstName().trim();
        entity.lastName = blank(request.lastName());
        entity.travellerType = request.travellerType() == null ? "ADULT" : request.travellerType();
        entity.birthDate = request.birthDate();
        entity.nationality = blank(request.nationality());
        entity.dietaryNotes = blank(request.dietaryNotes());
        entity.medicalNotes = blank(request.medicalNotes());
        entity.emergencyName = blank(request.emergencyName());
        entity.emergencyPhone = blank(request.emergencyPhone());
        return entity;
    }

    private TravelDestination requireDestination(String id) {
        return destinations.findById(id).orElseThrow(() -> new NotFoundException("Destino no encontrado"));
    }
    private TravelActivity requireActivity(String id) {
        return activities.findById(id).orElseThrow(() -> new NotFoundException("Actividad no encontrada"));
    }
    private TravelExtra requireExtra(String id) {
        return extras.findById(id).orElseThrow(() -> new NotFoundException("Servicio adicional no encontrado"));
    }
    private TravelTour requireTour(String id) {
        return tours.findById(id).orElseThrow(() -> new NotFoundException("Tour no encontrado"));
    }
    private TravelSettings settings() {
        return settingsRepository.findById(TravelSettings.currentId())
                .orElseGet(() -> settingsRepository.save(new TravelSettings()));
    }
    private ZoneId zone() {
        try { return ZoneId.of(settings().timezone); }
        catch (Exception ignored) { return ZoneId.of("America/Tegucigalpa"); }
    }

    private String uniqueDestinationSlug(String requested, String name, String currentId) {
        String base = slugs.slugify(requested == null || requested.isBlank() ? name : requested);
        String candidate = base; int index = 2;
        while (destinations.findBySlugIgnoreCase(candidate)
                .filter(item -> !Objects.equals(item.id, currentId)).isPresent()) candidate = base + "-" + index++;
        return candidate;
    }
    private String uniqueActivitySlug(String requested, String name, String currentId) {
        String base = slugs.slugify(requested == null || requested.isBlank() ? name : requested);
        String candidate = base; int index = 2;
        while (activities.findBySlugIgnoreCase(candidate)
                .filter(item -> !Objects.equals(item.id, currentId)).isPresent()) candidate = base + "-" + index++;
        return candidate;
    }
    private String uniqueExtraSlug(String requested, String name, String currentId) {
        String base = slugs.slugify(requested == null || requested.isBlank() ? name : requested);
        String candidate = base; int index = 2;
        while (extras.findBySlugIgnoreCase(candidate)
                .filter(item -> !Objects.equals(item.id, currentId)).isPresent()) candidate = base + "-" + index++;
        return candidate;
    }
    private String uniqueTourSlug(String requested, String name, String currentId) {
        String base = slugs.slugify(requested == null || requested.isBlank() ? name : requested);
        String candidate = base; int index = 2;
        while (tours.findBySlugIgnoreCase(candidate)
                .filter(item -> !Objects.equals(item.id, currentId)).isPresent()) candidate = base + "-" + index++;
        return candidate;
    }
    private String uniqueTourCode(String requested, String name, String currentId) {
        String base = requested == null || requested.isBlank()
                ? slugs.slugify(name).replace("-", "").toUpperCase(Locale.ROOT).substring(0, Math.min(8, slugs.slugify(name).replace("-", "").length()))
                : requested.trim().toUpperCase(Locale.ROOT);
        if (base.isBlank()) base = "TOUR";
        String candidate = base; int index = 2;
        while (tours.findByCodeIgnoreCase(candidate)
                .filter(item -> !Objects.equals(item.id, currentId)).isPresent()) candidate = base + "-" + index++;
        return candidate;
    }

    private boolean validDestination(TravelDestination item) {
        return item.id != null
                && item.name != null && !item.name.isBlank()
                && item.slug != null && !item.slug.isBlank()
                && item.active;
    }

    private boolean validActivity(TravelActivity item) {
        return item.id != null
                && item.name != null && !item.name.isBlank()
                && item.slug != null && !item.slug.isBlank()
                && item.active;
    }

    private boolean validExtra(TravelExtra item) {
        return item.id != null
                && item.name != null && !item.name.isBlank()
                && item.slug != null && !item.slug.isBlank()
                && item.price != null
                && item.active;
    }

    private boolean validTour(TravelTour item) {
        return item.id != null
                && item.name != null && !item.name.isBlank()
                && item.slug != null && !item.slug.isBlank()
                && item.status == TravelTourStatus.PUBLISHED
                && item.adultPrice != null
                && item.childPrice != null
                && item.infantPrice != null;
    }

    private boolean hasValidDeparture(TravelDeparture item) {
        return item != null
                && item.tour != null
                && item.tour.id != null
                && item.startsAt != null;
    }

    private boolean hasValidBooking(TravelBooking item) {
        return item != null
                && item.tour != null
                && item.tour.id != null
                && item.departure != null
                && item.departure.id != null;
    }

    private TravelDtos.DestinationResponse destination(TravelDestination item) {
        return new TravelDtos.DestinationResponse(
                item.id, item.name, item.slug, item.summary, item.description,
                item.imageUrl, item.country, item.region, item.city,
                item.latitude, item.longitude, item.featured, item.active,
                item.demoData, item.createdAt, item.updatedAt);
    }
    private TravelDtos.ActivityResponse activity(TravelActivity item) {
        return new TravelDtos.ActivityResponse(
                item.id, item.name, item.slug, item.description, item.icon,
                item.imageUrl, item.active, item.demoData, item.createdAt, item.updatedAt);
    }
    private TravelDtos.ExtraResponse extra(TravelExtra item) {
        return new TravelDtos.ExtraResponse(
                item.id, item.name, item.slug, item.description, item.price,
                item.currency, item.pricingUnit, item.maxQuantity, item.imageUrl,
                item.active, item.demoData, item.createdAt, item.updatedAt);
    }
    private TravelDtos.TourResponse tour(TravelTour item) {
        return new TravelDtos.TourResponse(
                item.id, item.name, item.slug, item.code, item.shortDescription,
                item.description, readList(item.includedJson), readList(item.excludedJson),
                readMapList(item.itineraryJson), readMapList(item.faqJson),
                readList(item.galleryJson),
                item.destination == null ? null : destination(item.destination),
                item.activities.stream().filter(Objects::nonNull).map(this::activity).toList(),
                item.durationValue, item.durationUnit, item.difficulty,
                item.minimumAge, item.maximumAge, item.minimumGroupSize,
                item.maximumGroupSize, item.adultPrice, item.childPrice,
                item.infantPrice, item.currency, item.pricingMode.name(),
                item.depositPercentage, item.taxExempt, readTaxes(item.taxRulesJson),
                settings().taxRate, item.featuredImageUrl, item.status.name(),
                item.featured, item.confirmationMode, item.demoData,
                item.createdAt, item.updatedAt);
    }
    private TravelDtos.DepartureResponse departure(TravelDeparture item) {
        return new TravelDtos.DepartureResponse(
                item.id, item.tour.id, item.tour.name, item.tour.slug,
                item.startsAt, item.endsAt, item.capacity, item.reservedSeats,
                item.heldSeats, item.blockedSeats, item.availableSeats(),
                item.status.name(), item.adultPrice, item.childPrice, item.infantPrice,
                item.tour.currency, item.meetingPoint,
                item.googleCalendarUrl == null ? googleCalendarUrl(item, null) : item.googleCalendarUrl,
                item.notes, item.demoData, item.createdAt, item.updatedAt);
    }
    private TravelDtos.BookingResponse booking(TravelBooking item) {
        return new TravelDtos.BookingResponse(
                item.id, item.bookingCode, item.tour.id, item.tour.name,
                item.tour.slug, item.departure.id, item.departure.startsAt,
                item.customerEmail, item.customerName, item.customerPhone,
                item.adultCount, item.childCount, item.infantCount, item.currency,
                item.subtotal, item.extrasTotal, item.discountTotal, item.taxTotal,
                readTaxLines(item.taxBreakdownJson), item.grandTotal, item.depositRequired,
                item.amountPaid, item.balanceDue,
                item.bookingStatus.name(), item.paymentStatus.name(), item.orderId,
                item.orderNumber, googleCalendarUrl(item.departure, item),
                item.holdExpiresAt,
                item.travellers.stream().map(this::traveller).toList(),
                readMap(item.extrasJson), item.createdAt, item.updatedAt);
    }
    private TravelDtos.TravellerResponse traveller(TravelTraveller item) {
        return new TravelDtos.TravellerResponse(
                item.id, item.firstName, item.lastName, item.travellerType,
                item.birthDate, item.nationality, item.dietaryNotes,
                item.medicalNotes, item.emergencyName, item.emergencyPhone);
    }
    private TravelDtos.SettingsResponse settings(TravelSettings item) {
        return new TravelDtos.SettingsResponse(
                item.defaultCurrency, item.timezone, item.taxRate,
                item.bookingHoldMinutes, item.minimumLeadHours, item.guestBooking,
                item.googleCalendarEnabled, item.googleCalendarId,
                item.notificationEmail, item.termsUrl, item.updatedAt);
    }

    private List<TravelDtos.TaxRule> normalizeTaxes(List<TravelDtos.TaxRule> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> new TravelDtos.TaxRule(
                        item.name() == null || item.name().isBlank() ? "Impuesto" : item.name().trim(),
                        percent(item.rate())))
                .filter(item -> item.rate().signum() > 0)
                .limit(10)
                .toList();
    }

    private List<TravelDtos.TaxRule> effectiveTaxes(TravelTour tour, TravelSettings settings) {
        if (tour.taxExempt) return List.of();
        List<TravelDtos.TaxRule> configured = readTaxes(tour.taxRulesJson);
        if (!configured.isEmpty()) return configured;
        if (settings.taxRate == null || settings.taxRate.signum() <= 0) return List.of();
        return List.of(new TravelDtos.TaxRule("Impuesto general", settings.taxRate));
    }

    private List<TravelDtos.TaxLine> calculateTaxes(
            TravelTour tour, TravelSettings settings, BigDecimal taxableBase) {
        return effectiveTaxes(tour, settings).stream()
                .map(rule -> new TravelDtos.TaxLine(
                        rule.name(),
                        rule.rate(),
                        money(taxableBase.multiply(rule.rate())
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))))
                .toList();
    }

    private List<TravelDtos.TaxRule> readTaxes(String value) {
        try {
            return value == null || value.isBlank()
                    ? List.of()
                    : mapper.readValue(value, new TypeReference<List<TravelDtos.TaxRule>>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<TravelDtos.TaxLine> readTaxLines(String value) {
        try {
            return value == null || value.isBlank()
                    ? List.of()
                    : mapper.readValue(value, new TypeReference<List<TravelDtos.TaxLine>>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String googleCalendarUrl(TravelDeparture departure, TravelBooking booking) {
        String title = booking == null
                ? departure.tour.name
                : departure.tour.name + " · " + booking.bookingCode;
        Instant end = departure.endsAt == null ? departure.startsAt.plus(Duration.ofHours(4)) : departure.endsAt;
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC);
        String details = booking == null
                ? departure.tour.shortDescription
                : "Reservación " + booking.bookingCode + " para " + booking.customerName;
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + encode(title)
                + "&dates=" + format.format(departure.startsAt) + "/" + format.format(end)
                + "&details=" + encode(Optional.ofNullable(details).orElse(""))
                + "&location=" + encode(Optional.ofNullable(departure.meetingPoint).orElse(""));
    }
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
    private String bookingCode() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneOffset.UTC).format(Instant.now());
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "TV-" + date + "-"
                    + ThreadLocalRandom.current().nextInt(100000, 1_000_000);
            if (bookings.findByBookingCodeIgnoreCase(candidate).isEmpty()) return candidate;
        }
        return "TV-" + date + "-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }
    private String normalizeCurrency(String value) {
        String normalized = value == null || value.isBlank() ? "USD" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) throw new BadRequestException("Moneda inválida");
        return normalized;
    }
    private BigDecimal percent(BigDecimal value) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
        if (normalized.signum() < 0 || normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El porcentaje debe estar entre 0 y 100");
        }
        return normalized;
    }
    private void notifyOperations(String recipient, String subject, String body) {
        if (recipient != null && !recipient.isBlank()) {
            mail.send(recipient, subject, body);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private String sanitizedHtml(String value) {
        String normalized = blank(value);
        return normalized == null ? null : sanitizer.html(normalized);
    }
    private String write(Object value) {
        try { return mapper.writeValueAsString(value == null ? List.of() : value); }
        catch (Exception exception) { throw new BadRequestException("Datos estructurados inválidos"); }
    }
    private List<String> readList(String value) {
        try { return value == null || value.isBlank() ? List.of() : mapper.readValue(value, new TypeReference<>() { }); }
        catch (Exception exception) { return List.of(); }
    }
    private List<Map<String, Object>> readMapList(String value) {
        try { return value == null || value.isBlank() ? List.of() : mapper.readValue(value, new TypeReference<>() { }); }
        catch (Exception exception) { return List.of(); }
    }
    private Map<String, Object> readMap(String value) {
        try { return value == null || value.isBlank() ? Map.of() : mapper.readValue(value, new TypeReference<>() { }); }
        catch (Exception exception) { return Map.of(); }
    }
    private <T> TravelDtos.PageResponse<T> page(List<T> values, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 500);
        int from = Math.min(values.size(), safePage * safeSize);
        int to = Math.min(values.size(), from + safeSize);
        int totalPages = values.isEmpty() ? 0 : (int) Math.ceil(values.size() / (double) safeSize);
        return new TravelDtos.PageResponse<>(
                values.subList(from, to), safePage, safeSize, values.size(), totalPages);
    }
}
