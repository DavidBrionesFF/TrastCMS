package com.nattechnologies.trastcms.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {
    Optional<Post> findBySlug(String slug);
    Optional<Post> findBySlugAndContentType(String slug, ContentType contentType);
    Optional<Post> findBySlugAndStatusAndContentType(String slug, PostStatus status, ContentType contentType);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, String id);

    Page<Post> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    Page<Post> findByContentTypeOrderByUpdatedAtDesc(ContentType contentType, Pageable pageable);
    Page<Post> findByAuthorEmailIgnoreCaseOrderByUpdatedAtDesc(String email, Pageable pageable);
    Page<Post> findByAuthorEmailIgnoreCaseAndContentTypeOrderByUpdatedAtDesc(
            String email, ContentType contentType, Pageable pageable);

    Page<Post> findByStatusAndContentTypeOrderByPublishedAtDesc(
            PostStatus status, ContentType contentType, Pageable pageable);
    Page<Post> findByStatusAndContentTypeAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            PostStatus status, ContentType contentType, String title, Pageable pageable);
    List<Post> findByStatusAndContentTypeAndShowInMenuTrueOrderByMenuOrderAscTitleAsc(
            PostStatus status, ContentType contentType);

    long countByStatus(PostStatus status);
    long countByContentType(ContentType contentType);
    long countByAuthorEmailIgnoreCase(String email);
    long countByAuthorEmailIgnoreCaseAndStatus(String email, PostStatus status);
}
