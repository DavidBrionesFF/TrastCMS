package com.nattechnologies.trastcms.plugins.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AcademyQuestionRepository extends JpaRepository<AcademyQuestion, String> {
    @Query(value = """
            SELECT s.course_id
            FROM academy_question q
            JOIN academy_quiz z ON z.id = q.quiz_id
            JOIN academy_section s ON s.id = z.section_id
            WHERE q.id = :questionId AND q.site_id = :siteId AND z.site_id = :siteId AND s.site_id = :siteId
            """, nativeQuery = true)
    Optional<String> findCourseIdByQuestionId(@Param("questionId") String questionId, @Param("siteId") String siteId);
}
