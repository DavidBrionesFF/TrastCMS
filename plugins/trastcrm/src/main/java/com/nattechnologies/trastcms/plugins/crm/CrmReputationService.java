package com.nattechnologies.trastcms.plugins.crm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PlatformMailService;
import com.nattechnologies.trastcms.service.PluginEventDispatcher;
import com.nattechnologies.trastcms.service.SiteSettingService;
import com.nattechnologies.trastcms.service.SiteUrlService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrmReputationService {
    private static final String KEY_PREFIX = "plugins.trastcrm.reputation.";
    private static final TypeReference<List<CrmReputationDtos.Channel>> CHANNEL_LIST = new TypeReference<>() { };

    private final CrmReputationRepository requests;
    private final CrmContactRepository contacts;
    private final CrmActivityRepository activities;
    private final UserAccountRepository users;
    private final SiteSettingService siteSettings;
    private final SiteUrlService siteUrls;
    private final PlatformMailService mail;
    private final PluginEventDispatcher events;
    private final ObjectMapper mapper;

    public CrmReputationService(
            CrmReputationRepository requests,
            CrmContactRepository contacts,
            CrmActivityRepository activities,
            UserAccountRepository users,
            SiteSettingService siteSettings,
            SiteUrlService siteUrls,
            PlatformMailService mail,
            PluginEventDispatcher events,
            ObjectMapper mapper) {
        this.requests = requests;
        this.contacts = contacts;
        this.activities = activities;
        this.users = users;
        this.siteSettings = siteSettings;
        this.siteUrls = siteUrls;
        this.mail = mail;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional
    public void handleEvent(PluginEvent event) {
        if (event == null || event.type() == null) return;
        CrmReputationDtos.Settings settings = settings();
        if (!settings.enabled()) return;
        Map<String, Object> payload = event.payload() == null ? Map.of() : event.payload();

        switch (event.type()) {
            case "travel.booking.paid" -> {
                if (!settings.travelEnabled()) return;
                createAndSend(
                        CrmReputationSource.TRAVEL,
                        required(payload, "bookingId"),
                        first(payload, "tourName", "bookingCode", "Tour reservado"),
                        first(payload, "customerEmail"),
                        first(payload, "customerName"),
                        event.type(), settings);
            }
            case "academy.course.completed" -> {
                if (!settings.academyEnabled()) return;
                createAndSend(
                        CrmReputationSource.ACADEMY,
                        required(payload, "enrollmentId"),
                        first(payload, "courseTitle", "courseName", "Curso completado"),
                        first(payload, "email", "customerEmail"),
                        first(payload, "customerName", "displayName"),
                        event.type(), settings);
            }
            case "saas.purchase.completed" -> {
                if (!settings.saasEnabled()) return;
                String order = required(payload, "orderId");
                String plan = first(payload, "planId", "planKey", "plan");
                createAndSend(
                        CrmReputationSource.SAAS,
                        order + ":" + (plan == null ? "saas" : plan),
                        saasLabel(payload),
                        first(payload, "customerEmail", "email"),
                        first(payload, "customerName", "displayName"),
                        event.type(), settings);
            }
            default -> { }
        }
    }

    @Transactional(readOnly = true)
    public CrmReputationDtos.Settings settings() {
        return new CrmReputationDtos.Settings(
                bool("enabled", true),
                bool("travel.enabled", true),
                bool("academy.enabled", true),
                bool("saas.enabled", true),
                integer("followUpDays", 3, 1, 30),
                siteSettings.get(KEY_PREFIX + "emailSubject", "¿Cómo fue su experiencia con {siteName}?"),
                siteSettings.get(KEY_PREFIX + "emailIntro", "Su opinión nos ayuda a mejorar. Seleccione de 1 a 5 estrellas para contarnos cómo fue su experiencia."),
                siteSettings.get(KEY_PREFIX + "thankYouMessage", "Gracias por compartir su experiencia. Su valoración quedó registrada y puede publicarla también en cualquiera de nuestros canales."),
                channels());
    }

    @Transactional
    public CrmReputationDtos.Settings saveSettings(CrmReputationDtos.Settings value) {
        if (value == null) throw new BadRequestException("La configuración de reputación es obligatoria");
        List<CrmReputationDtos.Channel> normalized = normalizeChannels(value.channels());
        siteSettings.put(KEY_PREFIX + "enabled", Boolean.toString(value.enabled()));
        siteSettings.put(KEY_PREFIX + "travel.enabled", Boolean.toString(value.travelEnabled()));
        siteSettings.put(KEY_PREFIX + "academy.enabled", Boolean.toString(value.academyEnabled()));
        siteSettings.put(KEY_PREFIX + "saas.enabled", Boolean.toString(value.saasEnabled()));
        siteSettings.put(KEY_PREFIX + "followUpDays", Integer.toString(Math.max(1, Math.min(30, value.followUpDays()))));
        siteSettings.put(KEY_PREFIX + "emailSubject", trim(value.emailSubject(), 220, "El asunto del correo es obligatorio"));
        siteSettings.put(KEY_PREFIX + "emailIntro", trim(value.emailIntro(), 2000, "El texto del correo es obligatorio"));
        siteSettings.put(KEY_PREFIX + "thankYouMessage", trim(value.thankYouMessage(), 1000, "El mensaje de agradecimiento es obligatorio"));
        siteSettings.put(KEY_PREFIX + "channels", write(normalized));
        return settings();
    }

    @Transactional(readOnly = true)
    public CrmDtos.PageResponse<CrmReputationDtos.RequestResponse> list(int page, int size) {
        var result = requests.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)))
                .map(this::response);
        return new CrmDtos.PageResponse<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CrmReputationDtos.Summary summary() {
        List<CrmReputationRequest> all = requests.findAll();
        long sent = all.stream().filter(item -> item.sentAt != null).count();
        List<CrmReputationRequest> rated = all.stream().filter(item -> item.rating != null).toList();
        double average = rated.stream().mapToInt(item -> item.rating).average().orElse(0d);
        double fiveStarRate = rated.isEmpty() ? 0d : rated.stream().filter(item -> item.rating == 5).count() * 100d / rated.size();
        Map<String, Long> bySource = all.stream().collect(Collectors.groupingBy(item -> item.sourceType.name(), LinkedHashMap::new, Collectors.counting()));
        List<CrmReputationDtos.RequestResponse> recent = all.stream()
                .sorted(Comparator.comparing((CrmReputationRequest item) -> item.createdAt).reversed())
                .limit(10).map(this::response).toList();
        return new CrmReputationDtos.Summary(all.size(), sent, rated.size(), round(average), round(fiveStarRate), bySource, recent);
    }

    @Transactional
    public CrmReputationDtos.RequestResponse resend(String id) {
        CrmReputationRequest request = require(id);
        sendEmail(request, settings(), true);
        return response(requests.save(request));
    }

    @Transactional(readOnly = true)
    public CrmReputationDtos.PublicView publicView(String token) {
        CrmReputationRequest request = requireToken(token);
        CrmReputationDtos.Settings settings = settings();
        List<CrmReputationDtos.PublicChannel> channels = settings.channels().stream()
                .filter(CrmReputationDtos.Channel::active)
                .filter(channel -> channel.url() != null && !channel.url().isBlank())
                .map(channel -> new CrmReputationDtos.PublicChannel(
                        channel.id(), channel.label(),
                        "/api/public/crm/reputation/" + encode(request.publicToken) + "/channels/" + encode(channel.id())))
                .toList();
        return new CrmReputationDtos.PublicView(
                request.sourceLabel,
                request.customerName,
                siteSettings.get("site.name", "TrastCMS"),
                request.status,
                request.rating,
                settings.thankYouMessage(),
                channels);
    }

    @Transactional
    public CrmReputationDtos.PublicView rate(String token, int rating) {
        if (rating < 1 || rating > 5) throw new BadRequestException("La valoración debe estar entre 1 y 5 estrellas");
        CrmReputationRequest request = requireToken(token);
        boolean firstRating = request.rating == null;
        request.rating = rating;
        request.ratedAt = Instant.now();
        request.status = request.channelClickedAt == null ? CrmReputationStatus.RATED : CrmReputationStatus.SHARED;
        requests.save(request);
        completeFollowUp(request);
        if (firstRating) addNote(request, "Valoración recibida: " + rating + "/5 estrellas.");
        events.publish(PluginEvent.of("crm.reputation.rated", Map.of(
                "requestId", request.id,
                "sourceType", request.sourceType.name(),
                "sourceReference", request.sourceReference,
                "rating", rating)));
        if (request.contact != null) {
            request.contact.tags = mergeTags(request.contact.tags, "reputacion", "valoracion-" + rating);
            request.contact.notes = mergeNotes(request.contact.notes,
                    "Valoración de " + request.sourceType.name() + ": " + rating + "/5\nOrigen: " + request.sourceLabel);
            contacts.save(request.contact);
        }
        return publicView(token);
    }

    @Transactional
    public CrmReputationDtos.PublicView feedback(String token, String feedback) {
        CrmReputationRequest request = requireToken(token);
        String normalized = feedback == null ? null : feedback.trim();
        if (normalized != null && normalized.length() > 4000) throw new BadRequestException("El comentario es demasiado extenso");
        request.privateFeedback = normalized == null || normalized.isBlank() ? null : normalized;
        requests.save(request);
        if (request.privateFeedback != null) {
            addNote(request, "Comentario privado recibido:\n" + request.privateFeedback);
            if (request.contact != null) {
                request.contact.notes = mergeNotes(request.contact.notes, "Comentario de experiencia:\n" + request.privateFeedback);
                contacts.save(request.contact);
            }
        }
        return publicView(token);
    }

    @Transactional
    public URI channelRedirect(String token, String channelId) {
        CrmReputationRequest request = requireToken(token);
        CrmReputationDtos.Channel channel = settings().channels().stream()
                .filter(CrmReputationDtos.Channel::active)
                .filter(item -> item.id().equalsIgnoreCase(channelId))
                .filter(item -> item.url() != null && !item.url().isBlank())
                .findFirst().orElseThrow(() -> new NotFoundException("Canal de reseñas no encontrado"));
        URI target;
        try {
            target = URI.create(channel.url().trim());
            if (!("https".equalsIgnoreCase(target.getScheme()) || "http".equalsIgnoreCase(target.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new BadRequestException("La URL del canal de reseñas no es válida");
        }
        request.channelId = channel.id();
        request.channelClickedAt = Instant.now();
        request.status = request.rating == null ? CrmReputationStatus.SENT : CrmReputationStatus.SHARED;
        requests.save(request);
        addNote(request, "El cliente abrió el canal externo de reseñas: " + channel.label() + ".");
        events.publish(PluginEvent.of("crm.reputation.shared", Map.of(
                "requestId", request.id,
                "sourceType", request.sourceType.name(),
                "sourceReference", request.sourceReference,
                "channelId", channel.id())));
        return target;
    }

    private void createAndSend(
            CrmReputationSource source,
            String sourceReference,
            String sourceLabel,
            String email,
            String customerName,
            String triggerEvent,
            CrmReputationDtos.Settings settings) {
        String normalizedEmail = lower(email);
        if (normalizedEmail == null) return;
        if (sourceReference == null || sourceReference.isBlank()) return;
        if (requests.findBySourceTypeAndSourceReference(source, sourceReference).isPresent()) return;

        CrmContact contact = upsertContact(normalizedEmail, customerName, source, sourceLabel);
        CrmReputationRequest request = new CrmReputationRequest();
        request.contact = contact;
        request.sourceType = source;
        request.sourceReference = truncate(sourceReference, 190);
        request.sourceLabel = truncate(sourceLabel == null || sourceLabel.isBlank() ? source.name() : sourceLabel, 240);
        request.recipientEmail = normalizedEmail;
        request.customerName = truncate(resolveName(normalizedEmail, customerName), 180);
        request.triggerEvent = truncate(triggerEvent, 120);
        request.status = CrmReputationStatus.PENDING;
        request = requests.save(request);

        CrmActivity followUp = new CrmActivity();
        followUp.type = CrmActivityType.TASK;
        followUp.subject = truncate("Verificar valoración · " + request.sourceLabel, 220);
        followUp.description = "Solicitud automática de reputación generada por " + source.name()
                + ". Si el cliente no responde, contacte de forma cordial para conocer su experiencia.";
        followUp.dueAt = Instant.now().plus(settings.followUpDays(), ChronoUnit.DAYS);
        followUp.contact = contact;
        followUp.company = contact.company;
        followUp = activities.save(followUp);
        request.followUpActivityId = followUp.id;
        requests.save(request);
        events.publish(PluginEvent.of("crm.reputation.requested", Map.of(
                "requestId", request.id,
                "sourceType", request.sourceType.name(),
                "sourceReference", request.sourceReference,
                "recipientEmail", request.recipientEmail)));
        sendEmail(request, settings, false);
    }

    private CrmContact upsertContact(String email, String customerName, CrmReputationSource source, String sourceLabel) {
        CrmContact contact = contacts.findByEmailIgnoreCase(email).orElseGet(CrmContact::new);
        String resolved = resolveName(email, customerName);
        String[] parts = resolved.split("\\s+", 2);
        if (contact.firstName == null || contact.firstName.isBlank()) contact.firstName = truncate(parts[0], 100);
        if ((contact.lastName == null || contact.lastName.isBlank()) && parts.length > 1) contact.lastName = truncate(parts[1], 100);
        contact.email = truncate(email, 190);
        contact.status = CrmContactStatus.CUSTOMER;
        if (contact.source == null || contact.source.isBlank()) contact.source = sourceName(source);
        contact.tags = mergeTags(contact.tags, "cliente", "reputacion", source.name().toLowerCase(Locale.ROOT));
        contact.notes = mergeNotes(contact.notes, "Experiencia registrada desde " + sourceName(source) + ": " + sourceLabel);
        return contacts.save(contact);
    }

    private void sendEmail(CrmReputationRequest request, CrmReputationDtos.Settings settings, boolean resend) {
        String baseUrl = siteUrls.currentBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            if (request.rating == null) request.status = CrmReputationStatus.FAILED;
            requests.save(request);
            addNote(request, "No se pudo preparar el correo de reputación porque el sitio no tiene un endpoint público activo.");
            return;
        }
        String siteName = siteSettings.get("site.name", "TrastCMS");
        String subject = settings.emailSubject().replace("{siteName}", siteName).replace("{sourceLabel}", request.sourceLabel);
        String html = emailHtml(request, settings, siteName, baseUrl);
        PlatformMailService.MailResult result = mail.sendHtmlNow(request.recipientEmail, subject, html);
        if (!result.delivered()) {
            if (request.rating == null) request.status = CrmReputationStatus.FAILED;
            requests.save(request);
            addNote(request, "No se pudo enviar la solicitud de valoración a " + request.recipientEmail
                    + ". Detalle: " + (result.message() == null ? "Error SMTP" : result.message()));
            return;
        }
        request.status = request.rating == null ? CrmReputationStatus.SENT : request.status;
        request.sentAt = Instant.now();
        requests.save(request);

        CrmActivity activity = new CrmActivity();
        activity.type = CrmActivityType.EMAIL;
        activity.subject = truncate((resend ? "Reenvío de valoración · " : "Solicitud de valoración · ") + request.sourceLabel, 220);
        activity.description = "Correo de reputación enviado a " + request.recipientEmail + ".";
        activity.completedAt = Instant.now();
        activity.contact = request.contact;
        activity.company = request.contact == null ? null : request.contact.company;
        activities.save(activity);
    }

    private String emailHtml(CrmReputationRequest request, CrmReputationDtos.Settings settings, String siteName, String baseUrl) {
        StringBuilder stars = new StringBuilder();
        for (int rating = 1; rating <= 5; rating++) {
            String url = baseUrl + "/review/" + encode(request.publicToken) + "?rating=" + rating;
            stars.append("<a href=\"").append(html(url)).append("\" style=\"display:inline-block;margin:4px 3px;padding:10px 12px;border-radius:10px;background:#fff7d6;color:#f59e0b;text-decoration:none;font-size:26px;font-weight:700;border:1px solid #fde68a\" title=\"")
                    .append(rating).append(" estrellas\">★</a>");
        }
        return "<!doctype html><html><body style=\"margin:0;background:#f6f7fb;font-family:Arial,sans-serif;color:#172033\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:32px 16px\"><div style=\"background:#fff;border:1px solid #e5e7eb;border-radius:18px;padding:30px\">"
                + "<div style=\"font-size:13px;font-weight:700;color:#635bff;text-transform:uppercase;letter-spacing:.08em\">" + html(siteName) + "</div>"
                + "<h1 style=\"font-size:26px;margin:10px 0 8px\">¿Cómo fue su experiencia?</h1>"
                + "<p style=\"font-size:16px;line-height:1.65;color:#5b6475\">Hola " + html(request.customerName == null ? "" : request.customerName) + ",</p>"
                + "<p style=\"font-size:16px;line-height:1.65;color:#5b6475\">" + html(settings.emailIntro()) + "</p>"
                + "<div style=\"margin:22px 0;padding:16px;border-radius:12px;background:#f8f9ff\"><strong>" + html(request.sourceLabel) + "</strong></div>"
                + "<p style=\"font-size:14px;color:#5b6475;margin-bottom:8px\">Seleccione una puntuación:</p><div>" + stars + "</div>"
                + "<p style=\"margin-top:26px;font-size:12px;line-height:1.5;color:#98a2b3\">La valoración se registra primero en " + html(siteName) + ". Después podrá elegir si desea compartirla también en nuestros canales públicos.</p>"
                + "</div></div></body></html>";
    }

    private void completeFollowUp(CrmReputationRequest request) {
        if (request.followUpActivityId == null) return;
        activities.findById(request.followUpActivityId).ifPresent(activity -> {
            if (activity.completedAt == null) {
                activity.completedAt = Instant.now();
                activity.description = (activity.description == null ? "" : activity.description + "\n\n") + "Completada automáticamente al recibir la valoración.";
                activities.save(activity);
            }
        });
    }

    private void addNote(CrmReputationRequest request, String text) {
        CrmActivity note = new CrmActivity();
        note.type = CrmActivityType.NOTE;
        note.subject = truncate("Reputación · " + request.sourceLabel, 220);
        note.description = text;
        note.completedAt = Instant.now();
        note.contact = request.contact;
        note.company = request.contact == null ? null : request.contact.company;
        activities.save(note);
    }

    private CrmReputationRequest require(String id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Solicitud de reputación no encontrada"));
    }

    private CrmReputationRequest requireToken(String token) {
        if (token == null || token.isBlank()) throw new NotFoundException("Solicitud de reputación no encontrada");
        return requests.findByPublicToken(token.trim()).orElseThrow(() -> new NotFoundException("Solicitud de reputación no encontrada"));
    }

    private CrmReputationDtos.RequestResponse response(CrmReputationRequest item) {
        return new CrmReputationDtos.RequestResponse(
                item.id, item.sourceType, item.sourceReference, item.sourceLabel,
                item.recipientEmail, item.customerName, item.contact == null ? null : item.contact.id,
                item.status, item.rating, item.privateFeedback, item.channelId,
                "/review/" + encode(item.publicToken), item.sentAt, item.ratedAt, item.channelClickedAt, item.createdAt, item.updatedAt);
    }

    private List<CrmReputationDtos.Channel> channels() {
        String raw = siteSettings.get(KEY_PREFIX + "channels", "");
        if (raw == null || raw.isBlank()) return defaultChannels();
        try {
            List<CrmReputationDtos.Channel> parsed = mapper.readValue(raw, CHANNEL_LIST);
            return parsed == null || parsed.isEmpty() ? defaultChannels() : normalizeChannels(parsed);
        } catch (Exception ignored) {
            return defaultChannels();
        }
    }

    private List<CrmReputationDtos.Channel> normalizeChannels(List<CrmReputationDtos.Channel> values) {
        if (values == null) return defaultChannels();
        LinkedHashMap<String, CrmReputationDtos.Channel> normalized = new LinkedHashMap<>();
        for (CrmReputationDtos.Channel item : values) {
            if (item == null) continue;
            String id = item.id() == null ? "" : item.id().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
            String label = item.label() == null ? "" : item.label().trim();
            if (id.isBlank() || label.isBlank()) continue;
            String url = item.url() == null ? "" : item.url().trim();
            if (!url.isBlank()) validateChannelUrl(url);
            normalized.put(id, new CrmReputationDtos.Channel(truncate(id, 80), truncate(label, 120), truncate(url, 1000), item.active()));
        }
        return normalized.isEmpty() ? defaultChannels() : List.copyOf(normalized.values());
    }

    private List<CrmReputationDtos.Channel> defaultChannels() {
        return List.of(
                new CrmReputationDtos.Channel("tripadvisor", "TripAdvisor", "", false),
                new CrmReputationDtos.Channel("google", "Google", "", false),
                new CrmReputationDtos.Channel("facebook", "Facebook", "", false));
    }

    private void validateChannelUrl(String url) {
        try {
            URI parsed = URI.create(url);
            if (!("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme())) || parsed.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new BadRequestException("Las URLs de reputación deben usar http o https");
        }
    }

    private boolean bool(String suffix, boolean fallback) {
        return Boolean.parseBoolean(siteSettings.get(KEY_PREFIX + suffix, Boolean.toString(fallback)));
    }

    private int integer(String suffix, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(siteSettings.get(KEY_PREFIX + suffix, Integer.toString(fallback)));
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String resolveName(String email, String requested) {
        if (requested != null && !requested.isBlank()) return requested.trim();
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName().trim();
            String combined = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
            if (!combined.isBlank()) return combined;
        }
        String local = email == null ? "Cliente" : email.split("@", 2)[0].replace('.', ' ').replace('_', ' ').trim();
        if (local.isBlank()) return "Cliente";
        return Arrays.stream(local.split("\\s+")).map(part -> part.isBlank() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1)).collect(Collectors.joining(" "));
    }

    private String sourceName(CrmReputationSource source) {
        return switch (source) {
            case TRAVEL -> "TrastTravel";
            case ACADEMY -> "TrastAcademy";
            case SAAS -> "TrastSaaS";
            case MANUAL -> "CRM";
        };
    }

    private String saasLabel(Map<String, Object> payload) {
        String product = first(payload, "productName", "productKey", "Software");
        String plan = first(payload, "planName", "planKey");
        return plan == null || plan.isBlank() ? product : product + " · " + plan;
    }

    private String required(Map<String, Object> payload, String key) {
        String value = first(payload, key);
        return value == null || value.isBlank() ? "event-" + UUID.randomUUID() : value;
    }

    private String first(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value).trim();
        }
        return null;
    }

    private String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("No se pudo guardar la configuración de reputación", exception); }
    }

    private String trim(String value, int max, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return truncate(value.trim(), max);
    }

    private String lower(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String mergeTags(String current, String... values) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (current != null) Arrays.stream(current.split(",")).map(String::trim).filter(v -> !v.isBlank()).forEach(tags::add);
        Arrays.stream(values).filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).forEach(tags::add);
        return truncate(String.join(", ", tags), 500);
    }

    private String mergeNotes(String current, String addition) {
        String value = current == null || current.isBlank() ? addition : current.trim() + "\n\n---\n" + addition;
        return truncate(value, 10000);
    }

    private String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private double round(double value) { return Math.round(value * 100d) / 100d; }
}
