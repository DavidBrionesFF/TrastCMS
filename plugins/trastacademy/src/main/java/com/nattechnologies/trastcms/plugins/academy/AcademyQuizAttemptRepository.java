package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface AcademyQuizAttemptRepository extends JpaRepository<AcademyQuizAttempt,String> {
    long countByEnrollmentIdAndQuizId(String enrollmentId,String quizId);
    Optional<AcademyQuizAttempt> findByIdAndEnrollmentEmailIgnoreCase(String id,String email);
    Optional<AcademyQuizAttempt> findFirstByEnrollmentIdAndQuizIdAndStatusOrderByStartedAtDesc(
            String enrollmentId,String quizId,AcademyAttemptStatus status);
    Optional<AcademyQuizAttempt> findFirstByEnrollmentIdAndQuizIdOrderByAttemptNumberDesc(
            String enrollmentId,String quizId);
    Optional<AcademyQuizAttempt> findFirstByEnrollmentEmailIgnoreCaseAndQuizIdAndStatusOrderByStartedAtDesc(
            String email,String quizId,AcademyAttemptStatus status);
    List<AcademyQuizAttempt> findByEnrollmentIdOrderByStartedAtDesc(String enrollmentId);
    List<AcademyQuizAttempt> findByStatusOrderByStartedAtAsc(AcademyAttemptStatus status);
    void deleteByEnrollmentId(String enrollmentId);
}
