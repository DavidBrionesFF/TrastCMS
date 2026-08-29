package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface AcademyQuestionResponseRepository extends JpaRepository<AcademyQuestionResponse,String> {
    void deleteByAttemptId(String attemptId);
    List<AcademyQuestionResponse> findByAttemptIdOrderByQuestionPositionAsc(String attemptId);
}
