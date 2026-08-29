package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="academy_lesson_progress", uniqueConstraints=@UniqueConstraint(name="uq_academy_progress", columnNames={"enrollment_id","lesson_id"}))
class AcademyLessonProgress {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="enrollment_id") AcademyEnrollment enrollment;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="lesson_id") AcademyLesson lesson;
    @Column(nullable=false) boolean completed;
    @Column(name="watched_seconds", nullable=false) int watchedSeconds;
    @Column(name="completed_at") Instant completedAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @PrePersist void create(){updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}
