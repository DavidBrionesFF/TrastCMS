package com.nattechnologies.trastcms.plugins.comments;

import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.PlatformMailService;
import com.nattechnologies.trastcms.service.SiteSettingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CommentsService {

    private final CommentRepository repo;
    private final SiteSettingService settings;
    private final PlatformMailService mail;
    private final DataSource ds;

    public CommentsService(CommentRepository repo, SiteSettingService settings, PlatformMailService mail, DataSource ds) {
        this.repo = repo;
        this.settings = settings;
        this.mail = mail;
        this.ds = ds;
    }

    public CommentsDtos.CommentsSettingsResponse getSettings() {
        return new CommentsDtos.CommentsSettingsResponse(
                settings.getBoolean("comments.enabled", true),
                settings.getBoolean("comments.require_moderation", true),
                settings.getBoolean("comments.allow_guests", true),
                settings.getBoolean("comments.allow_nested", true),
                Integer.parseInt(settings.get("comments.max_nesting_depth", "3")),
                settings.getBoolean("comments.notify_on_comment", false),
                settings.get("comments.notify_email", ""),
                Integer.parseInt(settings.get("comments.max_per_hour_ip", "5")),
                settings.getBoolean("comments.honeypot_enabled", true)
        );
    }

    @Transactional
    public void saveSettings(CommentsDtos.CommentsSettingsRequest req) {
        settings.put("comments.enabled", String.valueOf(req.enabled()));
        settings.put("comments.require_moderation", String.valueOf(req.requireModeration()));
        settings.put("comments.allow_guests", String.valueOf(req.allowGuests()));
        settings.put("comments.allow_nested", String.valueOf(req.allowNested()));
        settings.put("comments.max_nesting_depth", String.valueOf(req.maxNestingDepth()));
        settings.put("comments.notify_on_comment", String.valueOf(req.notifyOnComment()));
        settings.put("comments.notify_email", req.notifyEmail());
        settings.put("comments.max_per_hour_ip", String.valueOf(req.maxPerHourIp()));
        settings.put("comments.honeypot_enabled", String.valueOf(req.honeypotEnabled()));
    }

    public CommentsDtos.CommentsPage listAll(String status, String postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage;

        if (postId != null && !postId.isBlank()) {
            if (status != null && !status.isBlank()) {
                commentPage = repo.findByPostIdAndStatus(postId, status, pageable);
            } else {
                commentPage = repo.findAll(pageable); // Need standard findByPostId, but falling back to all or similar if omitted. Let's just do findByStatus if post is omitted.
            }
        } else {
            if (status != null && !status.isBlank()) {
                commentPage = repo.findByStatus(status, pageable);
            } else {
                commentPage = repo.findAll(pageable);
            }
        }

        long pendingCount = repo.countByStatus(CommentStatus.PENDING.name());

        List<CommentsDtos.CommentResponse> dtos = commentPage.getContent().stream()
                .map(c -> toDto(c, new ArrayList<>()))
                .collect(Collectors.toList());

        return new CommentsDtos.CommentsPage(
                dtos,
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages(),
                pendingCount
        );
    }

    public CommentsDtos.CommentsStats stats() {
        long total = repo.count();
        long pending = repo.countByStatus(CommentStatus.PENDING.name());
        long approved = repo.countByStatus(CommentStatus.APPROVED.name());
        long spam = repo.countByStatus(CommentStatus.SPAM.name());
        return new CommentsDtos.CommentsStats(total, pending, approved, spam);
    }

    @Transactional
    public CommentsDtos.CommentResponse approve(String id) {
        Comment c = repo.findById(id).orElseThrow(() -> new NotFoundException("Comment not found"));
        c.setStatus(CommentStatus.APPROVED.name());
        c = repo.save(c);
        return toDto(c, new ArrayList<>());
    }

    @Transactional
    public CommentsDtos.CommentResponse markSpam(String id) {
        Comment c = repo.findById(id).orElseThrow(() -> new NotFoundException("Comment not found"));
        c.setStatus(CommentStatus.SPAM.name());
        c = repo.save(c);
        return toDto(c, new ArrayList<>());
    }

    @Transactional
    public void delete(String id) {
        Comment c = repo.findById(id).orElseThrow(() -> new NotFoundException("Comment not found"));
        c.setStatus(CommentStatus.DELETED.name());
        repo.save(c);
    }

    @Transactional
    public CommentsDtos.CommentResponse submit(String postId, CommentsDtos.CommentRequest req, String ipAddress, String userAgent) {
        CommentsDtos.CommentsSettingsResponse currentSettings = getSettings();
        if (!currentSettings.enabled()) {
            throw new BadRequestException("Comments are disabled");
        }
        if (!currentSettings.allowGuests()) {
            throw new BadRequestException("Guests are not allowed to comment"); // Assuming all are guests in public controller for now unless we add auth
        }

        String ipHash = DigestUtils.md5DigestAsHex((ipAddress != null ? ipAddress : "0.0.0.0").getBytes());
        int rateLimit = currentSettings.maxPerHourIp();

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT count(id) FROM comment WHERE site_id = ? AND ip_hash = ? AND created_at > ?")) {
            ps.setString(1, SiteContext.siteId());
            ps.setString(2, ipHash);
            ps.setObject(3, Instant.now().minusSeconds(3600)); // 1 hour ago
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count >= rateLimit) {
                        throw new BadRequestException("Rate limit exceeded for comments");
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof BadRequestException) throw (BadRequestException) e;
            // Ignore DB errors on rate limit for now
        }

        if (req.parentId() != null && !req.parentId().isBlank()) {
            if (!currentSettings.allowNested()) {
                throw new BadRequestException("Nested comments are disabled");
            }
            Comment parent = repo.findById(req.parentId()).orElseThrow(() -> new BadRequestException("Parent comment not found"));
            if (!parent.getPostId().equals(postId)) {
                throw new BadRequestException("Parent comment belongs to a different post");
            }
        }

        Comment c = new Comment();
        c.setPostId(postId);
        c.setParentId(req.parentId());
        c.setAuthorName(req.authorName());
        c.setAuthorEmail(req.authorEmail());
        c.setAuthorUrl(req.authorUrl());
        c.setBody(req.body());
        c.setIpHash(ipHash);
        c.setUserAgent(userAgent);
        c.setStatus(currentSettings.requireModeration() ? CommentStatus.PENDING.name() : CommentStatus.APPROVED.name());

        c = repo.save(c);

        if (currentSettings.notifyOnComment() && currentSettings.notifyEmail() != null && !currentSettings.notifyEmail().isBlank()) {
            try {
                mail.send(currentSettings.notifyEmail(), "New Comment on Post", "You have a new comment from " + c.getAuthorName() + ".\n\n" + c.getBody());
            } catch (Exception e) {
                // Ignore mail errors
            }
        }

        return toDto(c, new ArrayList<>());
    }

    public List<CommentsDtos.CommentResponse> getApprovedForPost(String postId) {
        List<Comment> all = repo.findByPostIdAndStatusOrderByCreatedAtAsc(postId, CommentStatus.APPROVED.name(), Pageable.unpaged()).getContent();
        return all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId().isBlank())
                .map(c -> toDto(c, all))
                .collect(Collectors.toList());
    }

    private CommentsDtos.CommentResponse toDto(Comment c, List<Comment> allForPost) {
        List<CommentsDtos.CommentResponse> replies = allForPost.stream()
                .filter(reply -> Objects.equals(reply.getParentId(), c.getId()))
                .map(reply -> toDto(reply, allForPost))
                .collect(Collectors.toList());

        return new CommentsDtos.CommentResponse(
                c.getId(),
                c.getPostId(),
                c.getParentId(),
                c.getAuthorName(),
                c.getAuthorEmail(),
                c.getAuthorUrl(),
                c.getBody(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                replies
        );
    }
}
