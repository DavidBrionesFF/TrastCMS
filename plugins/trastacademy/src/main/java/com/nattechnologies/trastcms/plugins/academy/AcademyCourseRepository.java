package com.nattechnologies.trastcms.plugins.academy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface AcademyCourseRepository extends JpaRepository<AcademyCourse,String> {
    Optional<AcademyCourse> findBySlug(String slug);
    Optional<AcademyCourse> findByExternalSourceUrl(String externalSourceUrl);
    boolean existsBySlugAndIdNot(String slug, String id);
    Page<AcademyCourse> findByTitleContainingIgnoreCase(String search, Pageable pageable);
    Page<AcademyCourse> findByStatusAndTitleContainingIgnoreCase(AcademyCourseStatus status, String search, Pageable pageable);
    List<AcademyCourse> findByStatusAndFeaturedTrueOrderByUpdatedAtDesc(AcademyCourseStatus status);
    long countByStatus(AcademyCourseStatus status);
    long countByDemoDataTrue();
    List<AcademyCourse> findByDemoDataTrueOrderByCreatedAtAsc();
}
