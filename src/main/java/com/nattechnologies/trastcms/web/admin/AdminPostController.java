package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.service.PostService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {
    private final PostService service;

    public AdminPostController(PostService service) {
        this.service = service;
    }

    @GetMapping
    public ApiDtos.PageResponse<ApiDtos.PostSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ContentType type,
            Authentication auth) {
        return service.adminList(page, size, type, auth.getName());
    }

    @GetMapping("/{id}")
    public ApiDtos.PostResponse get(
            @PathVariable String id,
            Authentication auth) {
        return service.findAdmin(id, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.PostResponse create(
            @Valid @RequestBody ApiDtos.PostRequest request,
            Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id}")
    public ApiDtos.PostResponse update(
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.PostRequest request,
            Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String id,
            Authentication auth) {
        service.delete(id, auth.getName());
    }

    @GetMapping("/{id}/revisions")
    public List<ApiDtos.RevisionResponse> revisions(
            @PathVariable String id,
            Authentication auth) {
        return service.revisions(id, auth.getName());
    }
}
