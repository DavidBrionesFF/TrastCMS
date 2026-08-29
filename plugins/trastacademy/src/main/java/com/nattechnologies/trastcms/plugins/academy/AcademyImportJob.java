package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="academy_import_job")
class AcademyImportJob {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length=36) String id=UUID.randomUUID().toString();
    @Column(name="source_url", nullable=false, length=1200) String sourceUrl;
    @Column(name="source_type", nullable=false, length=40) String sourceType;
    @Column(nullable=false, length=30) String status;
    @Column(name="detected_title", length=240) String detectedTitle;
    @Column(name="result_course_id", length=36) String resultCourseId;
    @Lob @Column(name="warnings_json") String warningsJson;
    @Lob @Column(name="error_message") String errorMessage;
    @Column(name="requested_by", nullable=false, length=190) String requestedBy;
    @Column(name="started_at", nullable=false) Instant startedAt;
    @Column(name="finished_at") Instant finishedAt;
    @PrePersist void create(){startedAt=Instant.now();}
}
