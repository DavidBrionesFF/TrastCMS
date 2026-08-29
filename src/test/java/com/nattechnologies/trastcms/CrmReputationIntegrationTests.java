package com.nattechnologies.trastcms;

import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.plugins.crm.CrmDtos;
import com.nattechnologies.trastcms.plugins.crm.CrmReputationDtos;
import com.nattechnologies.trastcms.plugins.crm.CrmReputationService;
import com.nattechnologies.trastcms.plugins.crm.CrmReputationSource;
import com.nattechnologies.trastcms.plugins.crm.CrmReputationStatus;
import com.nattechnologies.trastcms.plugins.crm.CrmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:crmreputation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "trastcms.data-dir=target/test-data-crm-reputation",
        "trastcms.admin.email=admin@trastcms.local",
        "trastcms.admin.password=Testing-Password-2026!"
})
class CrmReputationIntegrationTests {

    @Autowired CrmReputationService reputation;
    @Autowired CrmService crm;

    private static final SiteContext.ResolvedSite PRIMARY = new SiteContext.ResolvedSite(
            SiteContext.PRIMARY_SITE_ID, "primary-site", "Primary Site");
    private static final SiteContext.ResolvedSite ACADEMY = new SiteContext.ResolvedSite(
            SiteContext.ACADEMY_SITE_ID, "academy", "Academy");
    private static final SiteContext.ResolvedSite SAAS = new SiteContext.ResolvedSite(
            SiteContext.SAAS_SITE_ID, "saas-store", "Software Store");

    @Test
    void travelPaidCreatesCustomerReputationRequestAndDeduplicatesSourceEvent() {
        SiteContext.withSite(PRIMARY, () -> {
            PluginEvent event = PluginEvent.of("travel.booking.paid", Map.of(
                    "bookingId", "booking-review-001",
                    "bookingCode", "TR-001",
                    "tourName", "Roatán Full Island Tour",
                    "customerEmail", "travel-review@example.com",
                    "customerName", "Ana Turista"));

            reputation.handleEvent(event);
            reputation.handleEvent(event);

            var page = reputation.list(0, 100);
            assertEquals(1L, page.content().stream()
                    .filter(item -> "booking-review-001".equals(item.sourceReference()))
                    .count(), "El mismo evento de origen no debe crear solicitudes duplicadas");
            var request = page.content().stream()
                    .filter(item -> "booking-review-001".equals(item.sourceReference()))
                    .findFirst().orElseThrow();
            assertEquals(CrmReputationSource.TRAVEL, request.sourceType());
            assertEquals("travel-review@example.com", request.recipientEmail());
            assertNotNull(request.contactId());
            assertTrue(request.reviewUrl().startsWith("/review/"));

            CrmDtos.PageResponse<CrmDtos.ContactResponse> contacts = crm.contacts(
                    "travel-review@example.com", null, 0, 20);
            assertEquals(1, contacts.content().size());
            assertEquals("CUSTOMER", contacts.content().getFirst().status().name());
        });
    }

    @Test
    void academyAndSaasEventsStayInsideTheirOwnTenants() {
        SiteContext.withSite(ACADEMY, () -> reputation.handleEvent(PluginEvent.of(
                "academy.course.completed", Map.of(
                        "enrollmentId", "academy-review-001",
                        "courseId", "course-001",
                        "courseTitle", "Spring Boot profesional",
                        "email", "student-review@example.com",
                        "customerName", "Estudiante Demo"))));

        SiteContext.withSite(SAAS, () -> reputation.handleEvent(PluginEvent.of(
                "saas.purchase.completed", Map.of(
                        "orderId", "saas-order-review-001",
                        "planId", "business",
                        "planName", "Business",
                        "productName", "NaT Suite Cloud",
                        "customerEmail", "saas-review@example.com",
                        "customerName", "Empresa SaaS"))));

        SiteContext.withSite(ACADEMY, () -> {
            var rows = reputation.list(0, 100).content();
            assertTrue(rows.stream().anyMatch(item -> "academy-review-001".equals(item.sourceReference())));
            assertFalse(rows.stream().anyMatch(item -> item.sourceReference().startsWith("saas-order-review-001")));
        });

        SiteContext.withSite(SAAS, () -> {
            var rows = reputation.list(0, 100).content();
            assertTrue(rows.stream().anyMatch(item -> item.sourceReference().startsWith("saas-order-review-001")));
            assertFalse(rows.stream().anyMatch(item -> "academy-review-001".equals(item.sourceReference())));
        });
    }

    @Test
    void ratingCompletesFollowUpAndTracksExternalChannelWithoutReviewGating() {
        SiteContext.withSite(PRIMARY, () -> {
            CrmReputationDtos.Settings current = reputation.settings();
            reputation.saveSettings(new CrmReputationDtos.Settings(
                    true, true, true, true, 3,
                    current.emailSubject(), current.emailIntro(), current.thankYouMessage(),
                    List.of(
                            new CrmReputationDtos.Channel("tripadvisor", "TripAdvisor", "https://www.tripadvisor.com/", true),
                            new CrmReputationDtos.Channel("google", "Google", "https://www.google.com/", true))));

            reputation.handleEvent(PluginEvent.of("travel.booking.paid", Map.of(
                    "bookingId", "booking-review-002",
                    "tourName", "Manglares y playa",
                    "customerEmail", "rating-review@example.com",
                    "customerName", "Carlos Cliente")));

            var request = reputation.list(0, 100).content().stream()
                    .filter(item -> "booking-review-002".equals(item.sourceReference()))
                    .findFirst().orElseThrow();
            String token = request.reviewUrl().substring("/review/".length());

            CrmReputationDtos.PublicView rated = reputation.rate(token, 2);
            assertEquals(2, rated.rating());
            assertEquals(CrmReputationStatus.RATED, rated.status());
            assertEquals(2, rated.channels().size(), "Los canales públicos no deben ocultarse por una puntuación baja");

            CrmDtos.PageResponse<CrmDtos.ActivityResponse> activities = crm.activities(0, 100);
            CrmDtos.ActivityResponse followUp = activities.content().stream()
                    .filter(item -> item.subject().contains("Verificar valoración") && item.subject().contains("Manglares y playa"))
                    .findFirst().orElseThrow();
            assertNotNull(followUp.completedAt(), "La tarea debe cerrarse automáticamente al recibir la valoración");

            URI redirect = reputation.channelRedirect(token, "tripadvisor");
            assertEquals("www.tripadvisor.com", redirect.getHost());
            var updated = reputation.list(0, 100).content().stream()
                    .filter(item -> "booking-review-002".equals(item.sourceReference()))
                    .findFirst().orElseThrow();
            assertEquals(CrmReputationStatus.SHARED, updated.status());
            assertEquals("tripadvisor", updated.channelId());
        });
    }

    @Test
    void privateFeedbackIsStoredOnTheExperience() {
        SiteContext.withSite(ACADEMY, () -> {
            reputation.handleEvent(PluginEvent.of("academy.course.completed", Map.of(
                    "enrollmentId", "academy-review-002",
                    "courseTitle", "Vue 3 y TypeScript",
                    "email", "feedback-review@example.com",
                    "customerName", "Luisa Alumna")));

            var request = reputation.list(0, 100).content().stream()
                    .filter(item -> "academy-review-002".equals(item.sourceReference()))
                    .findFirst().orElseThrow();
            String token = request.reviewUrl().substring("/review/".length());
            reputation.rate(token, 5);
            reputation.feedback(token, "El curso fue práctico y la explicación fue clara.");

            var updated = reputation.list(0, 100).content().stream()
                    .filter(item -> "academy-review-002".equals(item.sourceReference()))
                    .findFirst().orElseThrow();
            assertEquals(5, updated.rating());
            assertEquals("El curso fue práctico y la explicación fue clara.", updated.privateFeedback());
        });
    }
}
