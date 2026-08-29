package com.nattechnologies.trastcms.plugins.academy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AcademyDtos {
    private AcademyDtos() { }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }

    public record Dashboard(
            long totalCourses, long publishedCourses, long activeEnrollments,
            long completedEnrollments, long importJobs, long demoCourses,
            List<CourseSummary> featuredCourses, List<ImportJobResponse> recentImports) { }

    public record CourseSummary(
            String id, String title, String slug, String excerpt, String featuredImageUrl,
            String instructorName, String category, AcademyLevel level, AcademyAccessType accessType,
            AcademyCourseStatus status, BigDecimal price, String currency,
            int estimatedMinutes, boolean featured, boolean demoData, int sectionCount,
            int lessonCount, int quizCount, long enrollmentCount,
            Instant createdAt, Instant updatedAt) { }

    public record CourseDetail(
            String id, String title, String slug, String excerpt, String description,
            String featuredImageUrl, String instructorName, String category, AcademyLevel level,
            AcademyAccessType accessType, AcademyCourseStatus status, BigDecimal price,
            String currency, int estimatedMinutes, boolean featured, boolean demoData,
            String externalSourceId, String externalSourceUrl,
            List<SectionResponse> sections, EnrollmentResponse enrollment,
            Instant createdAt, Instant updatedAt) { }

    public record SectionResponse(
            String id, String title, String description, int position,
            String externalSourceId, List<LessonResponse> lessons,
            List<QuizResponse> quizzes) { }

    public record LessonResponse(
            String id, String title, String content, int position,
            AcademyLessonType lessonType, AcademyVideoProvider videoProvider,
            String videoUrl, String mediaId, int durationSeconds,
            boolean preview, String externalSourceId,
            boolean completed, int watchedSeconds) { }

    public record QuizResponse(
            String id, String title, String description, int position,
            BigDecimal passingScore, int maxAttempts, int durationMinutes,
            boolean randomizeQuestions, boolean preview, String externalSourceId,
            List<QuestionResponse> questions, long attemptsUsed) { }

    public record QuestionResponse(
            String id, String text, AcademyQuestionType type, int position,
            BigDecimal points, String explanation, boolean required,
            List<OptionResponse> options) { }

    public record OptionResponse(String id, String text, int position, Boolean correct) { }

    public record CourseRequest(
            @NotBlank @Size(max=240) String title,
            @Size(max=180) String slug,
            @Size(max=1000) String excerpt,
            @Size(max=200000) String description,
            @Size(max=1000) String featuredImageUrl,
            @Size(max=180) String instructorName,
            @Size(max=120) String category,
            AcademyLevel level,
            AcademyAccessType accessType,
            AcademyCourseStatus status,
            @DecimalMin("0") BigDecimal price,
            @Pattern(regexp="[A-Za-z]{3}") String currency,
            @PositiveOrZero Integer estimatedMinutes,
            boolean featured,
            @Size(max=190) String externalSourceId,
            @Size(max=1000) String externalSourceUrl,
            @Valid List<SectionRequest> sections) { }

    public record SectionRequest(
            String id,
            @NotBlank @Size(max=240) String title,
            @Size(max=1000) String description,
            @PositiveOrZero Integer position,
            @Size(max=190) String externalSourceId,
            @Valid List<LessonRequest> lessons,
            @Valid List<QuizRequest> quizzes) { }

    public record LessonRequest(
            String id,
            @NotBlank @Size(max=240) String title,
            @Size(max=200000) String content,
            @PositiveOrZero Integer position,
            AcademyLessonType lessonType,
            AcademyVideoProvider videoProvider,
            @Size(max=1200) String videoUrl,
            @Size(max=36) String mediaId,
            @PositiveOrZero Integer durationSeconds,
            boolean preview,
            @Size(max=190) String externalSourceId) { }

    public record QuizRequest(
            String id,
            @NotBlank @Size(max=240) String title,
            @Size(max=200000) String description,
            @PositiveOrZero Integer position,
            @DecimalMin("0") @DecimalMax("100") BigDecimal passingScore,
            @PositiveOrZero Integer maxAttempts,
            @PositiveOrZero Integer durationMinutes,
            boolean randomizeQuestions,
            boolean preview,
            @Size(max=190) String externalSourceId,
            @Valid List<QuestionRequest> questions) { }

    public record QuestionRequest(
            String id,
            @NotBlank @Size(max=20000) String text,
            AcademyQuestionType type,
            @PositiveOrZero Integer position,
            @DecimalMin("0") BigDecimal points,
            @Size(max=20000) String explanation,
            boolean required,
            @Valid List<OptionRequest> options) { }

    public record OptionRequest(
            String id,
            @NotBlank @Size(max=10000) String text,
            @PositiveOrZero Integer position,
            boolean correct) { }

    public record EnrollmentResponse(
            String id, String courseId, String courseTitle, String courseSlug,
            String courseImageUrl, String userId, String email,
            AcademyEnrollmentStatus status, AcademyEnrollmentSource source,
            String orderId, int completedLessons, int totalLessons,
            int completedQuizzes, int totalQuizzes, int progressPercentage,
            Instant enrolledAt, Instant completedAt,
            Instant updatedAt) { }

    public record LessonProgressRequest(@PositiveOrZero Integer watchedSeconds, boolean completed) { }

    public record AnswerRequest(
            @NotBlank String questionId,
            List<String> selectedOptionIds,
            @Size(max=20000) String textAnswer) { }

    public record FinishQuizRequest(@NotNull @Valid List<AnswerRequest> answers) { }

    public record QuizAttemptResponse(
            String id, String quizId, String quizTitle, int attemptNumber,
            AcademyAttemptStatus status, BigDecimal score, Boolean passed,
            Instant startedAt, Instant finishedAt,
            List<Map<String,Object>> results) { }

    public record PendingReviewAnswer(
            String responseId, String questionId, String questionText,
            BigDecimal maximumPoints, String textAnswer, BigDecimal awardedPoints) { }

    public record PendingReview(
            String attemptId, String courseId, String courseTitle, String quizId,
            String quizTitle, String enrollmentEmail, int attemptNumber,
            Instant submittedAt, List<PendingReviewAnswer> answers) { }

    public record ManualGradeRequest(
            @NotBlank String questionId,
            @NotNull @DecimalMin("0") BigDecimal awardedPoints) { }

    public record ManualReviewRequest(@NotNull @Valid List<ManualGradeRequest> grades) { }


    public record DemoDataResponse(
            int created, int existing, int deleted, int enrolled,
            String themeId, String catalogUrl, String message) { }

    public record SettingsRequest(
            @NotBlank @Size(max=220) String catalogTitle,
            @Size(max=1000) String catalogDescription,
            @Pattern(regexp="[A-Za-z]{3}") String defaultCurrency,
            @Size(max=1000) String defaultImportUrl,
            boolean allowFreeEnrollment) { }

    public record SettingsResponse(
            String catalogTitle, String catalogDescription, String defaultCurrency,
            String defaultImportUrl, boolean allowFreeEnrollment,
            Instant createdAt, Instant updatedAt) { }

    public record ImportRequest(
            @NotBlank @Size(max=1000) String url,
            @Size(max=4000) String bearerToken,
            boolean publish,
            boolean overwriteExisting) { }

    public record ImportPreview(
            String sourceType, String sourceUrl, String title, String excerpt,
            String description, String featuredImageUrl,
            List<String> youtubeVideos, int sections, int lessons,
            int quizzes, int questions, List<String> warnings,
            CourseRequest normalizedCourse) { }

    public record ImportJobResponse(
            String id, String sourceUrl, String sourceType, String status,
            String detectedTitle, String resultCourseId, List<String> warnings,
            String errorMessage, String requestedBy,
            Instant startedAt, Instant finishedAt) { }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(max=180) String displayName,
            @NotBlank @Size(min=12, max=200) String password,
            @Size(max=40) String phone,
            @Size(max=180) String deviceName) { }

    public record TokenRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @Size(max=180) String deviceName) { }

    public record RefreshTokenRequest(@NotBlank String refreshToken) { }
    public record RevokeTokenRequest(@NotBlank String token) { }

    public record TokenResponse(
            String tokenType, String accessToken, String refreshToken,
            long expiresIn, Instant expiresAt, Instant refreshExpiresAt,
            String userId, String email, String displayName, String role) { }
}
