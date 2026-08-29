package com.nattechnologies.trastcms.plugins.comments;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comments")
public class CommentsAdminController {

    private final CommentsService service;
    private final CommentsPluginGuard guard;

    public CommentsAdminController(CommentsService service, CommentsPluginGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @ModelAttribute
    public void requireEnabled() {
        guard.requireEnabled();
    }

    @GetMapping
    public CommentsDtos.CommentsPage list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.listAll(status, postId, page, size);
    }

    @GetMapping("/stats")
    public CommentsDtos.CommentsStats stats() {
        return service.stats();
    }

    @PutMapping("/{id}/status")
    public CommentsDtos.CommentResponse updateStatus(@PathVariable String id, @RequestBody @Valid CommentsDtos.CommentStatusRequest req) {
        if (CommentStatus.APPROVED.name().equals(req.status())) {
            return service.approve(id);
        } else if (CommentStatus.SPAM.name().equals(req.status())) {
            return service.markSpam(id);
        }
        throw new IllegalArgumentException("Unsupported status update");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/settings")
    public CommentsDtos.CommentsSettingsResponse getSettings() {
        return service.getSettings();
    }

    @PutMapping("/settings")
    public void saveSettings(@RequestBody @Valid CommentsDtos.CommentsSettingsRequest req) {
        service.saveSettings(req);
    }
}
