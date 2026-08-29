package com.nattechnologies.trastcms.plugins.crm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CrmReputationDtos {
    private CrmReputationDtos() { }

    public record Channel(
            @NotBlank @Size(max = 80) String id,
            @NotBlank @Size(max = 120) String label,
            @Size(max = 1000) String url,
            boolean active) { }

    public record Settings(
            boolean enabled,
            boolean travelEnabled,
            boolean academyEnabled,
            boolean saasEnabled,
            @Min(1) @Max(30) int followUpDays,
            @NotBlank @Size(max = 220) String emailSubject,
            @NotBlank @Size(max = 2000) String emailIntro,
            @NotBlank @Size(max = 1000) String thankYouMessage,
            @NotNull List<Channel> channels) { }

    public record RequestResponse(
            String id,
            CrmReputationSource sourceType,
            String sourceReference,
            String sourceLabel,
            String recipientEmail,
            String customerName,
            String contactId,
            CrmReputationStatus status,
            Integer rating,
            String privateFeedback,
            String channelId,
            String reviewUrl,
            Instant sentAt,
            Instant ratedAt,
            Instant channelClickedAt,
            Instant createdAt,
            Instant updatedAt) { }

    public record Summary(
            long total,
            long sent,
            long rated,
            double averageRating,
            double fiveStarRate,
            Map<String, Long> bySource,
            List<RequestResponse> recent) { }

    public record PublicView(
            String sourceLabel,
            String customerName,
            String siteName,
            CrmReputationStatus status,
            Integer rating,
            String thankYouMessage,
            List<PublicChannel> channels) { }

    public record PublicChannel(String id, String label, String redirectUrl) { }

    public record RatingRequest(@Min(1) @Max(5) int rating) { }
    public record FeedbackRequest(@Size(max = 4000) String feedback) { }
}
