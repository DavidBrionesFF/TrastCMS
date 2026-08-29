package com.nattechnologies.trastcms.plugins.comments;

import com.nattechnologies.trastcms.domain.post.PostRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/posts/{postSlug}/comments")
public class CommentsPublicController {

    private final CommentsService service;
    private final CommentsPluginGuard guard;
    private final PostRepository postRepository;

    public CommentsPublicController(CommentsService service, CommentsPluginGuard guard, PostRepository postRepository) {
        this.service = service;
        this.guard = guard;
        this.postRepository = postRepository;
    }

    @ModelAttribute
    public void requireEnabled() {
        guard.requireEnabled();
    }

    @GetMapping
    public List<CommentsDtos.CommentResponse> getApprovedComments(@PathVariable String postSlug) {
        var post = postRepository.findBySlug(postSlug).orElseThrow(() -> new NotFoundException("Post not found"));
        return service.getApprovedForPost(post.getId());
    }

    @PostMapping
    public CommentsDtos.CommentResponse submitComment(
            @PathVariable String postSlug,
            @RequestBody @Valid CommentsDtos.CommentRequest req,
            HttpServletRequest request) {
        var post = postRepository.findBySlug(postSlug).orElseThrow(() -> new NotFoundException("Post not found"));
        return service.submit(post.getId(), req, request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
