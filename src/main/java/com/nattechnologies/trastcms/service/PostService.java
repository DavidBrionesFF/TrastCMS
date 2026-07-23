package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.category.CategoryRepository;
import com.nattechnologies.trastcms.domain.post.*;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.plugin.api.ContentHook;
import com.nattechnologies.trastcms.plugin.api.HookContext;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PostService {
    private final PostRepository posts;
    private final PostRevisionRepository revisions;
    private final CategoryRepository categories;
    private final UserAccountService users;
    private final SlugService slugService;
    private final CategoryService categoryService;
    private final AuditService audit;
    private final List<ContentHook> hooks;
    private final PluginEventDispatcher events;
    private final ContentSanitizer sanitizer;

    public PostService(
            PostRepository posts,
            PostRevisionRepository revisions,
            CategoryRepository categories,
            UserAccountService users,
            SlugService slugService,
            CategoryService categoryService,
            AuditService audit,
            List<ContentHook> hooks,
            PluginEventDispatcher events,
            ContentSanitizer sanitizer
    ) {
        this.posts = posts;
        this.revisions = revisions;
        this.categories = categories;
        this.users = users;
        this.slugService = slugService;
        this.categoryService = categoryService;
        this.audit = audit;
        this.hooks = hooks.stream()
                .sorted(Comparator.comparingInt(ContentHook::order))
                .toList();
        this.events = events;
        this.sanitizer = sanitizer;
    }

    @Transactional(readOnly = true)
    public ApiDtos.PageResponse<ApiDtos.PostSummary> adminList(
            int page, int size, ContentType type, String actorEmail) {
        UserAccount actor = users.requireByEmail(actorEmail);
        Pageable pageable = pageable(page, size);
        Page<Post> result;

        if (actor.getRole() == UserRole.AUTHOR) {
            result = type == null
                    ? posts.findByAuthorEmailIgnoreCaseOrderByUpdatedAtDesc(actorEmail, pageable)
                    : posts.findByAuthorEmailIgnoreCaseAndContentTypeOrderByUpdatedAtDesc(
                            actorEmail, type, pageable);
        } else {
            result = type == null
                    ? posts.findAllByOrderByUpdatedAtDesc(pageable)
                    : posts.findByContentTypeOrderByUpdatedAtDesc(type, pageable);
        }
        return page(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public ApiDtos.PageResponse<ApiDtos.PostSummary> publicList(
            String search, int page, int size) {
        Pageable pageable = pageable(page, size);
        Page<Post> result = search == null || search.isBlank()
                ? posts.findByStatusAndContentTypeOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, ContentType.POST, pageable)
                : posts.findByStatusAndContentTypeAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                        PostStatus.PUBLISHED, ContentType.POST, search.trim(), pageable);
        return page(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.NavigationItem> navigation() {
        return posts.findByStatusAndContentTypeAndShowInMenuTrueOrderByMenuOrderAscTitleAsc(
                        PostStatus.PUBLISHED, ContentType.PAGE)
                .stream()
                .map(post -> new ApiDtos.NavigationItem(
                        post.getTitle(), post.getSlug(), post.getMenuOrder(), post.getPageRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostResponse findAdmin(String id, String actorEmail) {
        return toResponse(requireManageable(id, actorEmail));
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostResponse findBySlugForMcp(
            String slug, ContentType contentType) {
        return posts.findBySlugAndContentType(slug, contentType)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(
                        contentType == ContentType.PAGE
                                ? "Página no encontrada"
                                : "Publicación no encontrada"));
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostResponse findPublicBySlug(String slug, ContentType contentType) {
        return posts.findBySlugAndStatusAndContentType(
                        slug, PostStatus.PUBLISHED, contentType)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(
                        contentType == ContentType.PAGE
                                ? "Página no encontrada"
                                : "Publicación no encontrada"));
    }

    @Transactional
    public ApiDtos.PostResponse create(ApiDtos.PostRequest request, String actorEmail) {
        UserAccount author = users.requireByEmail(actorEmail);
        Post post = new Post();
        post.setAuthor(author);
        apply(post, request, null);

        HookContext context = new HookContext(actorEmail, "create");
        hooks.forEach(hook -> hook.beforeSave(post, context));

        Post savedPost = posts.save(post);
        audit.record(
                actorEmail,
                "content.created",
                savedPost.getContentType().name().toLowerCase(Locale.ROOT),
                savedPost.getId(),
                savedPost.getTitle());
        publishIfNeeded(savedPost, context, false);
        return toResponse(savedPost);
    }

    @Transactional
    public ApiDtos.PostResponse update(
            String id, ApiDtos.PostRequest request, String actorEmail) {
        Post post = requireManageable(id, actorEmail);
        boolean wasPublished = post.getStatus() == PostStatus.PUBLISHED;

        createRevision(post, actorEmail);
        apply(post, request, id);

        HookContext context = new HookContext(actorEmail, "update");
        hooks.forEach(hook -> hook.beforeSave(post, context));

        Post savedPost = posts.save(post);
        audit.record(
                actorEmail,
                "content.updated",
                savedPost.getContentType().name().toLowerCase(Locale.ROOT),
                savedPost.getId(),
                savedPost.getTitle());
        publishIfNeeded(savedPost, context, wasPublished);
        return toResponse(savedPost);
    }

    @Transactional
    public void delete(String id, String actorEmail) {
        Post post = requireManageable(id, actorEmail);
        posts.delete(post);
        audit.record(
                actorEmail,
                "content.deleted",
                post.getContentType().name().toLowerCase(Locale.ROOT),
                id,
                post.getTitle());
        events.publish(PluginEvent.of("content.deleted", Map.of(
                "id", id,
                "title", post.getTitle(),
                "contentType", post.getContentType().name())));
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.RevisionResponse> revisions(String id, String actorEmail) {
        requireManageable(id, actorEmail);
        return revisions.findTop20ByPostIdOrderByRevisionNumberDesc(id)
                .stream()
                .map(revision -> new ApiDtos.RevisionResponse(
                        revision.getId(),
                        revision.getRevisionNumber(),
                        revision.getTitle(),
                        revision.getExcerpt(),
                        revision.getBody(),
                        revision.getContentType(),
                        revision.getEditorMode(),
                        revision.getBuilderData(),
                        revision.getCustomCss(),
                        revision.getStatus(),
                        revision.getCreatedBy(),
                        revision.getCreatedAt()))
                .toList();
    }

    private void apply(Post post, ApiDtos.PostRequest request, String currentId) {
        ContentType contentType = request.contentType() == null
                ? ContentType.POST : request.contentType();
        EditorMode editorMode = request.editorMode() == null
                ? (contentType == ContentType.PAGE
                    ? EditorMode.VISUAL_BUILDER
                    : EditorMode.RICH_TEXT)
                : request.editorMode();

        String desired = request.slug() == null || request.slug().isBlank()
                ? request.title() : request.slug();
        String base = slugService.slugify(desired);
        String unique = uniqueSlug(base, currentId);

        post.setTitle(request.title().trim());
        post.setSlug(unique);
        post.setExcerpt(blankToNull(request.excerpt()));
        post.setBody(sanitizer.html(request.body()));
        post.setEditorMode(editorMode);
        post.setBuilderData(
                editorMode == EditorMode.VISUAL_BUILDER
                        ? sanitizer.json(request.builderData())
                        : null);
        post.setCustomCss(
                editorMode == EditorMode.VISUAL_BUILDER
                        ? sanitizer.css(request.customCss())
                        : "");
        post.setSeoTitle(blankToNull(request.seoTitle()));
        post.setSeoDescription(blankToNull(request.seoDescription()));
        post.setTemplate(
                request.template() == null || request.template().isBlank()
                        ? "default"
                        : request.template().trim());
        post.setShowInMenu(contentType == ContentType.PAGE && request.showInMenu());
        post.setMenuOrder(contentType == ContentType.PAGE ? request.menuOrder() : 0);
        post.setPageRole(contentType == ContentType.PAGE ? blankToNull(request.pageRole()) : null);
        post.setContentType(contentType);
        post.setStatus(request.status());
        post.setFeaturedImageUrl(blankToNull(request.featuredImageUrl()));

        Category category = request.categoryId() == null
                || request.categoryId().isBlank()
                ? null
                : categories.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        post.setCategory(category);

        if (request.status() == PostStatus.PUBLISHED
                && post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }
        if (request.status() != PostStatus.PUBLISHED) {
            post.setPublishedAt(null);
        }
    }

    private String uniqueSlug(String base, String currentId) {
        String candidate = base;
        int suffix = 2;
        while (currentId == null
                ? posts.existsBySlug(candidate)
                : posts.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private void createRevision(Post post, String actor) {
        PostRevision revision = new PostRevision();
        revision.setPostId(post.getId());
        revision.setRevisionNumber(revisions.countByPostId(post.getId()) + 1);
        revision.setTitle(post.getTitle());
        revision.setExcerpt(post.getExcerpt());
        revision.setContentType(post.getContentType());
        revision.setBody(post.getBody());
        revision.setEditorMode(post.getEditorMode());
        revision.setBuilderData(post.getBuilderData());
        revision.setCustomCss(post.getCustomCss());
        revision.setStatus(post.getStatus());
        revision.setCreatedBy(actor);
        revisions.save(revision);
    }

    private void publishIfNeeded(
            Post post, HookContext context, boolean wasPublished) {
        Map<String, Object> payload = Map.of(
                "id", post.getId(),
                "title", post.getTitle(),
                "slug", post.getSlug(),
                "contentType", post.getContentType().name(),
                "editorMode", post.getEditorMode().name(),
                "status", post.getStatus().name());

        if (post.getStatus() == PostStatus.PUBLISHED && !wasPublished) {
            hooks.forEach(hook -> hook.afterPublish(post, context));
            events.publish(PluginEvent.of("content.published", payload));
        } else {
            events.publish(PluginEvent.of("content.saved", payload));
        }
    }

    private Post require(String id) {
        return posts.findById(id)
                .orElseThrow(() -> new NotFoundException("Contenido no encontrado"));
    }

    private Post requireManageable(String id, String actorEmail) {
        Post post = require(id);
        UserAccount actor = users.requireByEmail(actorEmail);
        if (actor.getRole() == UserRole.AUTHOR
                && !post.getAuthor().getEmail().equalsIgnoreCase(actorEmail)) {
            throw new AccessDeniedException(
                    "No tiene permiso para administrar este contenido");
        }
        return post;
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(size, 1), 100));
    }

    private <T> ApiDtos.PageResponse<T> page(Page<T> page) {
        return new ApiDtos.PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private ApiDtos.PostSummary toSummary(Post post) {
        return new ApiDtos.PostSummary(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getContentType(),
                post.getEditorMode(),
                post.getStatus(),
                post.isShowInMenu(),
                post.getMenuOrder(),
                post.getPageRole(),
                post.getThemeOrigin(),
                post.getFeaturedImageUrl(),
                post.getCategory() == null
                        ? null
                        : categoryService.toResponse(post.getCategory()),
                post.getAuthor().getDisplayName(),
                post.getPublishedAt(),
                post.getUpdatedAt());
    }

    private ApiDtos.PostResponse toResponse(Post post) {
        return new ApiDtos.PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getBody(),
                post.getEditorMode(),
                post.getBuilderData(),
                post.getCustomCss(),
                post.getSeoTitle(),
                post.getSeoDescription(),
                post.getTemplate(),
                post.isShowInMenu(),
                post.getMenuOrder(),
                post.getPageRole(),
                post.getThemeOrigin(),
                post.getContentType(),
                post.getStatus(),
                post.getFeaturedImageUrl(),
                post.getCategory() == null
                        ? null
                        : categoryService.toResponse(post.getCategory()),
                post.getAuthor().getDisplayName(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getVersion());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
