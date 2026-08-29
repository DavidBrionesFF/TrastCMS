package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface AcademyEnrollmentRepository extends JpaRepository<AcademyEnrollment,String> {
    Optional<AcademyEnrollment> findByCourseIdAndEmailIgnoreCase(String courseId,String email);
    List<AcademyEnrollment> findByEmailIgnoreCaseOrderByEnrolledAtDesc(String email);
    List<AcademyEnrollment> findAllByOrderByEnrolledAtDesc();
    List<AcademyEnrollment> findByCourseId(String courseId);
    long countByCourseId(String courseId);
    long countByStatus(AcademyEnrollmentStatus status);
}
