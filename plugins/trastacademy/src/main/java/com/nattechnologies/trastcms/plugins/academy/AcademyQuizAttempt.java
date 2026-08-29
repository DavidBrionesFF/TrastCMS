package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="academy_quiz_attempt", uniqueConstraints=@UniqueConstraint(name="uq_academy_attempt", columnNames={"enrollment_id","quiz_id","attempt_number"}))
class AcademyQuizAttempt {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="enrollment_id") AcademyEnrollment enrollment;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="quiz_id") AcademyQuiz quiz;
    @Column(name="attempt_number", nullable=false) int attemptNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) AcademyAttemptStatus status=AcademyAttemptStatus.IN_PROGRESS;
    @Column(precision=7, scale=2) BigDecimal score;
    Boolean passed;
    @Column(name="started_at", nullable=false) Instant startedAt=Instant.now();
    @Column(name="finished_at") Instant finishedAt;
    @PrePersist void create(){if(startedAt==null)startedAt=Instant.now();}
}
