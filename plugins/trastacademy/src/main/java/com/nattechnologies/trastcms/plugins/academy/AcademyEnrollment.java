package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="academy_enrollment", uniqueConstraints=@UniqueConstraint(name="uq_academy_enrollment", columnNames={"course_id","email"}))
class AcademyEnrollment {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="course_id") AcademyCourse course;
    @Column(name="user_id", length=36) String userId;
    @Column(nullable=false, length=190) String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) AcademyEnrollmentStatus status=AcademyEnrollmentStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) AcademyEnrollmentSource source=AcademyEnrollmentSource.FREE;
    @Column(name="order_id", length=36) String orderId;
    @Column(name="enrolled_at", nullable=false) Instant enrolledAt;
    @Column(name="completed_at") Instant completedAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @PrePersist void create(){enrolledAt=updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}
