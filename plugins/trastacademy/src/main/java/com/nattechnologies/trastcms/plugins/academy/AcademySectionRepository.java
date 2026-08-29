package com.nattechnologies.trastcms.plugins.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AcademySectionRepository extends JpaRepository<AcademySection, String> {
    @Query(value = """
            SELECT course_id
            FROM academy_section
            WHERE id = :sectionId AND site_id = :siteId
            """, nativeQuery = true)
    Optional<String> findCourseIdBySectionId(@Param("sectionId") String sectionId, @Param("siteId") String siteId);
}
