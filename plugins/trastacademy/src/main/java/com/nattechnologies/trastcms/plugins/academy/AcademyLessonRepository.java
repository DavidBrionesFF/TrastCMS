package com.nattechnologies.trastcms.plugins.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AcademyLessonRepository extends JpaRepository<AcademyLesson, String> {
    @Query(value = """
            SELECT s.course_id
            FROM academy_lesson l
            JOIN academy_section s ON s.id = l.section_id
            WHERE l.id = :lessonId AND l.site_id = :siteId AND s.site_id = :siteId
            """, nativeQuery = true)
    Optional<String> findCourseIdByLessonId(@Param("lessonId") String lessonId, @Param("siteId") String siteId);
}
