package com.nattechnologies.trastcms.plugins.academy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AcademyQuizRepository extends JpaRepository<AcademyQuiz, String> {
    @Query(value = """
            SELECT s.course_id
            FROM academy_quiz q
            JOIN academy_section s ON s.id = q.section_id
            WHERE q.id = :quizId AND q.site_id = :siteId AND s.site_id = :siteId
            """, nativeQuery = true)
    Optional<String> findCourseIdByQuizId(@Param("quizId") String quizId, @Param("siteId") String siteId);
}
