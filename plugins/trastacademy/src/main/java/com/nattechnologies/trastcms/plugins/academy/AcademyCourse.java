package com.nattechnologies.trastcms.plugins.academy;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "academy_course")
class AcademyCourse {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @Column(nullable = false, length = 240) String title;
    @Column(nullable = false, length = 180) String slug;
    @Column(length = 1000) String excerpt;
    @Lob String description;
    @Column(name = "featured_image_url", length = 1000) String featuredImageUrl;
    @Column(name = "instructor_name", length = 180) String instructorName;
    @Column(nullable = false, length = 120) String category = "General";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) AcademyLevel level = AcademyLevel.BEGINNER;
    @Enumerated(EnumType.STRING) @Column(name = "access_type", nullable = false, length = 30) AcademyAccessType accessType = AcademyAccessType.FREE;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) AcademyCourseStatus status = AcademyCourseStatus.DRAFT;
    @Column(nullable = false, precision = 19, scale = 4) BigDecimal price = BigDecimal.ZERO;
    @Column(nullable = false, length = 3) String currency = "USD";
    @Column(name = "estimated_minutes", nullable = false) int estimatedMinutes;
    @Column(nullable = false) boolean featured;
    @Column(name = "demo_data", nullable = false) boolean demoData;
    @Column(name = "external_source_id", length = 190) String externalSourceId;
    @Column(name = "external_source_url", length = 1000) String externalSourceUrl;
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    List<AcademySection> sections = new ArrayList<>();
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @PrePersist void create(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}

enum AcademyCourseStatus { DRAFT, PUBLISHED, ARCHIVED }
enum AcademyAccessType { FREE, PAID, PRIVATE }
enum AcademyLevel { BEGINNER, INTERMEDIATE, ADVANCED, ALL_LEVELS }
enum AcademyLessonType { TEXT, VIDEO, MIXED }
enum AcademyVideoProvider { YOUTUBE, UPLOAD, EXTERNAL }
enum AcademyQuestionType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_TEXT, LONG_TEXT }
enum AcademyEnrollmentStatus { ACTIVE, COMPLETED, CANCELLED }
enum AcademyEnrollmentSource { FREE, PURCHASE, ADMIN, IMPORT }
enum AcademyAttemptStatus { IN_PROGRESS, PENDING_REVIEW, FINISHED }

@Entity
@Table(name = "academy_section")
class AcademySection {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id") AcademyCourse course;
    @Column(nullable = false, length = 240) String title;
    @Column(length = 1000) String description;
    @Column(nullable = false) int position;
    @Column(name = "external_source_id", length = 190) String externalSourceId;
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") List<AcademyLesson> lessons = new ArrayList<>();
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") List<AcademyQuiz> quizzes = new ArrayList<>();
}

@Entity
@Table(name = "academy_lesson")
class AcademyLesson {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") AcademySection section;
    @Column(nullable = false, length = 240) String title;
    @Lob String content;
    @Column(nullable = false) int position;
    @Enumerated(EnumType.STRING) @Column(name = "lesson_type", nullable = false, length = 30) AcademyLessonType lessonType = AcademyLessonType.TEXT;
    @Enumerated(EnumType.STRING) @Column(name = "video_provider", length = 30) AcademyVideoProvider videoProvider;
    @Column(name = "video_url", length = 1200) String videoUrl;
    @Column(name = "media_id", length = 36) String mediaId;
    @Column(name = "duration_seconds", nullable = false) int durationSeconds;
    @Column(nullable = false) boolean preview;
    @Column(name = "external_source_id", length = 190) String externalSourceId;
}

@Entity
@Table(name = "academy_quiz")
class AcademyQuiz {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") AcademySection section;
    @Column(nullable = false, length = 240) String title;
    @Lob String description;
    @Column(nullable = false) int position;
    @Column(name = "passing_score", nullable = false, precision = 7, scale = 2) BigDecimal passingScore = BigDecimal.valueOf(70);
    @Column(name = "max_attempts", nullable = false) int maxAttempts = 3;
    @Column(name = "duration_minutes", nullable = false) int durationMinutes;
    @Column(name = "randomize_questions", nullable = false) boolean randomizeQuestions;
    @Column(nullable = false) boolean preview;
    @Column(name = "external_source_id", length = 190) String externalSourceId;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") List<AcademyQuestion> questions = new ArrayList<>();
}

@Entity
@Table(name = "academy_question")
class AcademyQuestion {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quiz_id") AcademyQuiz quiz;
    @Lob @Column(name = "question_text", nullable = false) String text;
    @Enumerated(EnumType.STRING) @Column(name = "question_type", nullable = false, length = 40) AcademyQuestionType type = AcademyQuestionType.SINGLE_CHOICE;
    @Column(nullable = false) int position;
    @Column(nullable = false, precision = 10, scale = 2) BigDecimal points = BigDecimal.ONE;
    @Lob String explanation;
    @Column(nullable = false) boolean required = true;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") List<AcademyAnswerOption> options = new ArrayList<>();
}

@Entity
@Table(name = "academy_answer_option")
class AcademyAnswerOption {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "site_id", nullable = false, length = 36, updatable = false)
    private String siteId;

    @Id @Column(length = 36) String id = UUID.randomUUID().toString();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") AcademyQuestion question;
    @Lob @Column(name = "option_text", nullable = false) String text;
    @Column(nullable = false) int position;
    @Column(nullable = false) boolean correct;
}
