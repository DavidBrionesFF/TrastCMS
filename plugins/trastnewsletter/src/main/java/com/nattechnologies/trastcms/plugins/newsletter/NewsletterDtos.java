package com.nattechnologies.trastcms.plugins.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class NewsletterDtos {

    public record SubscriberResponse(String id, String email, String name, String status, String source, Instant subscribedAt) {}

    public record SubscribeRequest(
            @NotBlank @Email String email,
            @Size(max=120) String name,
            boolean pushConsent) {}

    public record CampaignRequest(
            @NotBlank @Size(max=220) String title,
            @NotBlank @Size(max=220) String subject,
            @NotBlank String bodyHtml,
            boolean pushEnabled,
            @Size(max=100) String pushTitle,
            @Size(max=300) String pushBody,
            @Size(max=1000) String pushUrl) {}

    public record CampaignResponse(
            String id, String title, String subject, String bodyHtml,
            boolean pushEnabled, String pushTitle, String pushBody, String pushUrl,
            String status, Instant scheduledAt, Instant sentAt,
            Integer recipientCount, Integer openCount,
            String createdBy, Instant createdAt, Instant updatedAt) {}

    public record SendCampaignRequest(boolean includeEmail, boolean includePush) {}

    public record NewsletterStats(long totalSubscribers, long activeSubscribers, long totalCampaigns, long sentCampaigns) {}

    public record NewsletterSettingsRequest(
            String senderName, String senderEmail, String replyTo,
            boolean doubleOptIn, String logoUrl, String footerText) {}

    public record NewsletterSettingsResponse(
            String senderName, String senderEmail, String replyTo,
            boolean doubleOptIn, String logoUrl, String footerText) {}

    public record UnsubscribeRequest(String token) {}
}
