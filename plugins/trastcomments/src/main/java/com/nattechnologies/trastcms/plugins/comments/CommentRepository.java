package com.nattechnologies.trastcms.plugins.comments;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findByPostIdAndStatus(String postId, String status, Pageable pageable);

    Page<Comment> findByStatus(String status, Pageable pageable);

    Page<Comment> findAll(Pageable pageable);

    long countByStatus(String status);

    Page<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(String postId, String status, Pageable pageable);
}
