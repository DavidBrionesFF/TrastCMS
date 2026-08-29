package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="academy_question_response", uniqueConstraints=@UniqueConstraint(name="uq_academy_response", columnNames={"attempt_id","question_id"}))
class AcademyQuestionResponse {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="attempt_id") AcademyQuizAttempt attempt;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="question_id") AcademyQuestion question;
    @Lob @Column(name="selected_option_ids") String selectedOptionIds;
    @Lob @Column(name="text_answer") String textAnswer;
    Boolean correct;
    @Column(name="awarded_points", nullable=false, precision=10, scale=2) BigDecimal awardedPoints=BigDecimal.ZERO;
}
