package com.nattechnologies.trastcms.plugins.comments;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class CommentsDtos {

    public record CommentRequest(
            @NotBlank @Size(max=120) String authorName,
            @NotBlank @Email @Size(max=190) String authorEmail,
            @Size(max=500) String authorUrl,
            String parentId,
            @NotBlank @Size(max=5000) String body
    ) {}

    public record CommentResponse(
            String id,
            String postId,
            String parentId,
            String authorName,
            String authorEmail,
            String authorUrl,
            String body,
            String status,
            Instant createdAt,
            Instant updatedAt,
            List<CommentResponse> replies
    ) {}

    public record CommentStatusRequest(
            @NotBlank String status
    ) {}

    public record CommentsPage(
            List<CommentResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            long pendingCount
    ) {}

    public record CommentsSettingsRequest(
            boolean enabled,
            boolean requireModeration,
            boolean allowGuests,
            boolean allowNested,
            int maxNestingDepth,
            boolean notifyOnComment,
            String notifyEmail,
            int maxPerHourIp,
            boolean honeypotEnabled
    ) {}

    public record CommentsSettingsResponse(
            boolean enabled,
            boolean requireModeration,
            boolean allowGuests,
            boolean allowNested,
            int maxNestingDepth,
            boolean notifyOnComment,
            String notifyEmail,
            int maxPerHourIp,
            boolean honeypotEnabled
    ) {}

    public record CommentsStats(
            long total,
            long pending,
            long approved,
            long spam
    ) {}
}
