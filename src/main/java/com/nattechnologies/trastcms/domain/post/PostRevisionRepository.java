package com.nattechnologies.trastcms.domain.post;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRevisionRepository extends JpaRepository<PostRevision, String> {
    long countByPostId(String postId);
    List<PostRevision> findTop20ByPostIdOrderByRevisionNumberDesc(String postId);
}
