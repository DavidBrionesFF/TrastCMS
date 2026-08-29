package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface AcademyLessonProgressRepository extends JpaRepository<AcademyLessonProgress,String> {
    Optional<AcademyLessonProgress> findByEnrollmentIdAndLessonId(String enrollmentId,String lessonId);
    List<AcademyLessonProgress> findByEnrollmentId(String enrollmentId);
    long countByEnrollmentIdAndCompletedTrue(String enrollmentId);
    void deleteByEnrollmentId(String enrollmentId);
}
