package com.nattechnologies.trastcms.plugins.academy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AcademyService {
    private final AcademyCourseRepository courses;
    private final AcademySectionRepository sections;
    private final AcademyLessonRepository lessons;
    private final AcademyQuizRepository quizzes;
    private final AcademyQuestionRepository questions;
    private final AcademyEnrollmentRepository enrollments;
    private final AcademyLessonProgressRepository progress;
    private final AcademyQuizAttemptRepository attempts;
    private final AcademyQuestionResponseRepository responses;
    private final AcademySettingsRepository settingsRepository;
    private final AcademyImportJobRepository importJobs;
    private final UserAccountService users;
    private final SlugService slugs;
    private final ContentSanitizer sanitizer;
    private final PluginEventDispatcher events;
    private final PlatformMailService mail;
    private final ObjectMapper mapper;
    private final AcademyPluginGuard guard;

    AcademyService(
            AcademyCourseRepository courses,
            AcademySectionRepository sections,
            AcademyLessonRepository lessons,
            AcademyQuizRepository quizzes,
            AcademyQuestionRepository questions,
            AcademyEnrollmentRepository enrollments,
            AcademyLessonProgressRepository progress,
            AcademyQuizAttemptRepository attempts,
            AcademyQuestionResponseRepository responses,
            AcademySettingsRepository settingsRepository,
            AcademyImportJobRepository importJobs,
            UserAccountService users,
            SlugService slugs,
            ContentSanitizer sanitizer,
            PluginEventDispatcher events,
            PlatformMailService mail,
            ObjectMapper mapper,
            AcademyPluginGuard guard) {
        this.courses = courses;
        this.sections = sections;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.questions = questions;
        this.enrollments = enrollments;
        this.progress = progress;
        this.attempts = attempts;
        this.responses = responses;
        this.settingsRepository = settingsRepository;
        this.importJobs = importJobs;
        this.users = users;
        this.slugs = slugs;
        this.sanitizer = sanitizer;
        this.events = events;
        this.mail = mail;
        this.mapper = mapper;
        this.guard = guard;
    }

    @Transactional
    public void initializeDefaults() {
        if (!settingsRepository.existsById(AcademySettings.currentId())) {
            settingsRepository.save(new AcademySettings());
        }
    }

    @Transactional(readOnly = true)
    public AcademyDtos.Dashboard dashboard() {
        guard.requireEnabled();
        List<AcademyDtos.CourseSummary> featured = courses
                .findByStatusAndFeaturedTrueOrderByUpdatedAtDesc(AcademyCourseStatus.PUBLISHED)
                .stream().limit(6).map(this::summary).toList();
        return new AcademyDtos.Dashboard(
                courses.count(), courses.countByStatus(AcademyCourseStatus.PUBLISHED),
                enrollments.countByStatus(AcademyEnrollmentStatus.ACTIVE),
                enrollments.countByStatus(AcademyEnrollmentStatus.COMPLETED),
                importJobs.count(), courses.countByDemoDataTrue(), featured,
                importJobs.findTop20ByOrderByStartedAtDesc().stream().limit(8).map(this::importJob).toList());
    }

    @Transactional(readOnly = true)
    public AcademyDtos.PageResponse<AcademyDtos.CourseSummary> courses(
            String search, int page, int size, boolean publicOnly) {
        guard.requireEnabled();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = search == null ? "" : search.trim();
        PageRequest request = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("updatedAt")));
        Page<AcademyCourse> result = publicOnly
                ? courses.findByStatusAndTitleContainingIgnoreCase(AcademyCourseStatus.PUBLISHED, term, request)
                : term.isBlank() ? courses.findAll(request) : courses.findByTitleContainingIgnoreCase(term, request);
        return new AcademyDtos.PageResponse<>(result.getContent().stream().map(this::summary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AcademyDtos.CourseDetail publicCourse(String slug, String email) {
        guard.requireEnabled();
        AcademyCourse course = courses.findBySlug(slug)
                .filter(value -> value.status == AcademyCourseStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Curso no encontrado"));
        AcademyEnrollment enrollment = email == null ? null
                : enrollments.findByCourseIdAndEmailIgnoreCase(course.id, email).orElse(null);
        return detail(course, false, enrollment, enrollment != null);
    }

    @Transactional(readOnly = true)
    public AcademyDtos.CourseDetail adminCourse(String id) {
        guard.requireEnabled();
        return detail(requireCourse(id), true, null, true);
    }

    @Transactional(readOnly = true)
    public AcademyDtos.CourseDetail publicCourseById(String id, String email) {
        guard.requireEnabled();
        AcademyCourse course = requireCourse(id);
        if (course.status != AcademyCourseStatus.PUBLISHED) throw new NotFoundException("Curso no encontrado");
        AcademyEnrollment enrollment = email == null ? null : enrollments.findByCourseIdAndEmailIgnoreCase(id, email).orElse(null);
        return detail(course, false, enrollment, enrollment != null);
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.EnrollmentResponse> adminEnrollments() {
        guard.requireEnabled();
        return enrollments.findAllByOrderByEnrolledAtDesc().stream().map(this::enrollment).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.PendingReview> pendingReviews() {
        guard.requireEnabled();
        return attempts.findByStatusOrderByStartedAtAsc(AcademyAttemptStatus.PENDING_REVIEW).stream()
                .map(attempt -> {
                    List<AcademyDtos.PendingReviewAnswer> answers = responses
                            .findByAttemptIdOrderByQuestionPositionAsc(attempt.id).stream()
                            .filter(response -> response.question.type == AcademyQuestionType.LONG_TEXT)
                            .map(response -> new AcademyDtos.PendingReviewAnswer(
                                    response.id, response.question.id, response.question.text,
                                    response.question.points, response.textAnswer, response.awardedPoints))
                            .toList();
                    return new AcademyDtos.PendingReview(
                            attempt.id, attempt.enrollment.course.id, attempt.enrollment.course.title,
                            attempt.quiz.id, attempt.quiz.title, attempt.enrollment.email,
                            attempt.attemptNumber, attempt.finishedAt, answers);
                }).toList();
    }

    @Transactional
    public AcademyDtos.QuizAttemptResponse reviewAttempt(
            String attemptId, AcademyDtos.ManualReviewRequest request) {
        guard.requireEnabled();
        AcademyQuizAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Intento no encontrado"));
        if (attempt.status != AcademyAttemptStatus.PENDING_REVIEW) {
            throw new BadRequestException("El intento no está pendiente de revisión");
        }
        Map<String, BigDecimal> grades = request.grades().stream().collect(Collectors.toMap(
                AcademyDtos.ManualGradeRequest::questionId,
                AcademyDtos.ManualGradeRequest::awardedPoints,
                (left, right) -> right));
        List<AcademyQuestionResponse> answerRows = responses.findByAttemptIdOrderByQuestionPositionAsc(attempt.id);
        for (AcademyQuestionResponse response : answerRows) {
            if (response.question.type != AcademyQuestionType.LONG_TEXT) continue;
            BigDecimal awarded = grades.get(response.question.id);
            if (awarded == null) {
                throw new BadRequestException("Debe calificar la pregunta '" + response.question.text + "'");
            }
            if (awarded.compareTo(response.question.points) > 0) {
                throw new BadRequestException("La calificación de '" + response.question.text
                        + "' no puede superar " + response.question.points + " puntos");
            }
            response.awardedPoints = awarded.setScale(2, RoundingMode.HALF_UP);
            response.correct = awarded.compareTo(response.question.points) >= 0;
        }
        responses.saveAll(answerRows);
        BigDecimal total = attempt.quiz.questions.stream().map(question -> question.points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal awarded = answerRows.stream().map(response -> response.awardedPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal score = total.signum() == 0 ? BigDecimal.ZERO
                : awarded.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        attempt.score = score;
        attempt.passed = score.compareTo(attempt.quiz.passingScore) >= 0;
        attempt.status = AcademyAttemptStatus.FINISHED;
        attempts.save(attempt);
        evaluateCompletion(attempt.enrollment);
        events.publish(PluginEvent.of("academy.quiz.reviewed", Map.of(
                "courseId", attempt.enrollment.course.id, "quizId", attempt.quiz.id,
                "attemptId", attempt.id, "email", attempt.enrollment.email,
                "score", score, "passed", attempt.passed)));
        List<Map<String,Object>> results = answerRows.stream().map(response -> {
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("questionId", response.question.id);
            result.put("correct", response.correct);
            result.put("awardedPoints", response.awardedPoints);
            result.put("explanation", Optional.ofNullable(response.question.explanation).orElse(""));
            return result;
        }).toList();
        return attempt(attempt, results);
    }

    @Transactional
    public AcademyDtos.CourseDetail saveCourse(String id, AcademyDtos.CourseRequest request) {
        guard.requireEnabled();
        AcademyCourse course = id == null ? new AcademyCourse() : requireCourse(id);
        String slug = slugs.slugify(blank(request.slug()) ? request.title() : request.slug());
        if (courses.existsBySlugAndIdNot(slug, course.id)) {
            throw new ConflictException("Ya existe un curso con el slug " + slug);
        }
        course.title = request.title().trim();
        course.slug = slug;
        course.excerpt = nullable(request.excerpt());
        course.description = sanitizer.html(request.description());
        course.featuredImageUrl = nullable(request.featuredImageUrl());
        course.instructorName = nullable(request.instructorName());
        course.category = blank(request.category()) ? "General" : request.category().trim();
        course.level = value(request.level(), AcademyLevel.BEGINNER);
        course.accessType = value(request.accessType(), AcademyAccessType.FREE);
        course.status = value(request.status(), AcademyCourseStatus.DRAFT);
        course.price = money(request.price());
        course.currency = currency(request.currency(), settings().defaultCurrency);
        course.estimatedMinutes = Math.max(0, optional(request.estimatedMinutes(), 0));
        course.featured = request.featured();
        course.externalSourceId = nullable(request.externalSourceId());
        course.externalSourceUrl = nullable(request.externalSourceUrl());
        boolean deletionsAllowed = id == null || enrollments.countByCourseId(course.id) == 0;
        syncSections(course, list(request.sections()), deletionsAllowed);
        AcademyCourse saved = courses.save(course);
        events.publish(PluginEvent.of(id == null ? "academy.course.created" : "academy.course.updated",
                Map.of("courseId", saved.id, "slug", saved.slug, "status", saved.status.name())));
        if (saved.status == AcademyCourseStatus.PUBLISHED) {
            events.publish(PluginEvent.of("academy.course.published", Map.of("courseId", saved.id, "slug", saved.slug)));
        }
        return detail(saved, true, null, true);
    }

    @Transactional
    public void deleteCourse(String id) {
        guard.requireEnabled();
        AcademyCourse course = requireCourse(id);
        if (enrollments.countByCourseId(id) > 0) {
            course.status = AcademyCourseStatus.ARCHIVED;
            courses.save(course);
            return;
        }
        courses.delete(course);
    }

    @Transactional
    public AcademyDtos.EnrollmentResponse enrollFree(String courseId, String email) {
        guard.requireEnabled();
        AcademyCourse course = requireCourse(courseId);
        AcademySettings settings = settings();
        if (course.status != AcademyCourseStatus.PUBLISHED) throw new NotFoundException("Curso no disponible");
        if (!settings.allowFreeEnrollment || course.accessType != AcademyAccessType.FREE) {
            throw new BadRequestException("Este curso requiere compra o asignación administrativa");
        }
        UserAccount user = users.requireByEmail(email);
        return enrollment(createEnrollment(course, user, AcademyEnrollmentSource.FREE, null));
    }

    @Transactional
    public AcademyDtos.EnrollmentResponse enrollAdmin(String courseId, String email) {
        guard.requireEnabled();
        UserAccount user = users.requireByEmail(email);
        return enrollment(createEnrollment(requireCourse(courseId), user, AcademyEnrollmentSource.ADMIN, null));
    }

    @Transactional
    public void fulfill(String courseId, String customerId, String customerEmail, String orderId) {
        guard.requireEnabled();
        if (blank(customerEmail)) throw new BadRequestException("La compra del curso requiere el correo del cliente");
        AcademyCourse course = requireCourse(courseId);
        String normalizedEmail = customerEmail.trim().toLowerCase(Locale.ROOT);
        String userId = null;
        try {
            userId = users.requireByEmail(normalizedEmail).getId();
        } catch (NotFoundException ignored) {
            // La matrícula queda reclamable por correo hasta que el comprador cree su cuenta.
        }
        createEnrollment(course, userId, normalizedEmail, AcademyEnrollmentSource.PURCHASE, orderId);
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.EnrollmentResponse> accountEnrollments(String email) {
        guard.requireEnabled();
        return enrollments.findByEmailIgnoreCaseOrderByEnrolledAtDesc(email).stream()
                .map(this::enrollment).toList();
    }

    @Transactional(readOnly = true)
    public AcademyDtos.CourseDetail accountCourse(String courseId, String email) {
        guard.requireEnabled();
        AcademyEnrollment enrollment = requireEnrollment(courseId, email);
        // Vuelva a cargar el agregado del curso. Una matrícula puede conservar una referencia
        // anterior cuando el currículo demo fue regenerado dentro de otra transacción.
        AcademyCourse currentCourse = requireCourse(courseId);
        return detail(currentCourse, false, enrollment, true);
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.LessonResponse> accountLessons(String courseId, String email) {
        return accountCourse(courseId, email).sections().stream()
                .flatMap(section -> section.lessons().stream()).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.QuizResponse> accountQuizzes(String courseId, String email) {
        return accountCourse(courseId, email).sections().stream()
                .flatMap(section -> section.quizzes().stream()).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.QuestionResponse> accountQuestions(String quizId, String email) {
        return accountQuiz(quizId, email).questions();
    }

    @Transactional
    public AcademyDtos.EnrollmentResponse finishCourse(String courseId, String email) {
        guard.requireEnabled();
        AcademyEnrollment enrollment = requireEnrollment(courseId, email);
        evaluateCompletion(enrollment);
        if (enrollment.status != AcademyEnrollmentStatus.COMPLETED) {
            throw new BadRequestException("Aún existen lecciones o evaluaciones pendientes");
        }
        return enrollment(enrollment);
    }

    @Transactional
    public AcademyDtos.EnrollmentResponse retakeCourse(String courseId, String email) {
        guard.requireEnabled();
        AcademyEnrollment enrollment = requireEnrollment(courseId, email);
        progress.deleteByEnrollmentId(enrollment.id);
        attempts.deleteByEnrollmentId(enrollment.id);
        enrollment.status = AcademyEnrollmentStatus.ACTIVE;
        enrollment.completedAt = null;
        enrollments.save(enrollment);
        events.publish(PluginEvent.of("academy.course.retake",
                Map.of("courseId", courseId, "enrollmentId", enrollment.id, "email", email)));
        return enrollment(enrollment);
    }

    @Transactional
    public AcademyDtos.EnrollmentResponse updateLesson(
            String lessonId, String email, AcademyDtos.LessonProgressRequest request) {
        guard.requireEnabled();
        AcademyLesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Lección no encontrada"));
        AcademyEnrollment enrollment = requireEnrollment(courseIdForLesson(lesson.id), email);
        AcademyLessonProgress state = progress.findByEnrollmentIdAndLessonId(enrollment.id, lesson.id)
                .orElseGet(AcademyLessonProgress::new);
        state.enrollment = enrollment;
        state.lesson = lesson;
        state.watchedSeconds = Math.max(state.watchedSeconds, optional(request.watchedSeconds(), 0));
        boolean newlyCompleted = request.completed() && !state.completed;
        if (request.completed()) {
            state.completed = true;
            if (state.completedAt == null) state.completedAt = Instant.now();
        }
        progress.save(state);
        evaluateCompletion(enrollment);
        if (newlyCompleted) {
            events.publish(PluginEvent.of("academy.lesson.completed",
                    Map.of("courseId", enrollment.course.id, "lessonId", lesson.id, "email", email)));
        }
        return enrollment(enrollment);
    }

    @Transactional(readOnly = true)
    public AcademyDtos.LessonResponse accountLesson(String lessonId, String email) {
        guard.requireEnabled();
        AcademyLesson lesson = lessons.findById(lessonId).orElseThrow(() -> new NotFoundException("Lección no encontrada"));
        AcademyEnrollment enrollment = requireEnrollment(courseIdForLesson(lesson.id), email);
        AcademyLessonProgress state = progress.findByEnrollmentIdAndLessonId(enrollment.id, lesson.id).orElse(null);
        return new AcademyDtos.LessonResponse(lesson.id, lesson.title, lesson.content, lesson.position, lesson.lessonType,
                lesson.videoProvider, lesson.videoUrl, lesson.mediaId, lesson.durationSeconds, lesson.preview, lesson.externalSourceId,
                state != null && state.completed, state == null ? 0 : state.watchedSeconds);
    }

    @Transactional(readOnly = true)
    public AcademyDtos.SectionResponse accountSection(String sectionId, String email) {
        guard.requireEnabled();
        AcademySection section = sections.findById(sectionId).orElseThrow(() -> new NotFoundException("Sección no encontrada"));
        AcademyDtos.CourseDetail course = accountCourse(courseIdForSection(section.id), email);
        return course.sections().stream().filter(value -> value.id().equals(sectionId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Sección no encontrada"));
    }

    @Transactional(readOnly = true)
    public AcademyDtos.QuestionResponse accountQuestion(String questionId, String email) {
        guard.requireEnabled();
        AcademyQuestion question = questions.findById(questionId).orElseThrow(() -> new NotFoundException("Pregunta no encontrada"));
        requireEnrollment(courseIdForQuestion(question.id), email);
        return new AcademyDtos.QuestionResponse(question.id, question.text, question.type, question.position, question.points, null, question.required,
                question.options.stream().map(option -> new AcademyDtos.OptionResponse(option.id, option.text, option.position, null)).toList());
    }

    @Transactional(readOnly = true)
    public AcademyDtos.QuizResponse accountQuiz(String quizId, String email) {
        guard.requireEnabled();
        AcademyQuiz quiz = quizzes.findById(quizId).orElseThrow(() -> new NotFoundException("Evaluación no encontrada"));
        AcademyEnrollment enrollment = requireEnrollment(courseIdForQuiz(quiz.id), email);
        long used = attempts.countByEnrollmentIdAndQuizId(enrollment.id, quiz.id);
        List<AcademyDtos.QuestionResponse> questions = new ArrayList<>(quiz.questions.stream().map(question -> new AcademyDtos.QuestionResponse(
                question.id, question.text, question.type, question.position, question.points, null, question.required,
                question.options.stream().map(option -> new AcademyDtos.OptionResponse(option.id, option.text, option.position, null)).toList())).toList());
        if (quiz.randomizeQuestions) Collections.shuffle(questions);
        return new AcademyDtos.QuizResponse(quiz.id, quiz.title, quiz.description, quiz.position, quiz.passingScore, quiz.maxAttempts,
                quiz.durationMinutes, quiz.randomizeQuestions, quiz.preview, quiz.externalSourceId, questions, used);
    }

    @Transactional
    public AcademyDtos.QuizAttemptResponse startQuiz(String quizId, String email) {
        guard.requireEnabled();
        AcademyQuiz quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Evaluación no encontrada"));
        if (quiz.questions.isEmpty()) {
            throw new BadRequestException("La evaluación todavía no tiene preguntas publicadas");
        }
        AcademyEnrollment enrollment = requireEnrollment(courseIdForQuiz(quiz.id), email);
        if (attempts.findFirstByEnrollmentIdAndQuizIdAndStatusOrderByStartedAtDesc(
                enrollment.id, quiz.id, AcademyAttemptStatus.PENDING_REVIEW).isPresent()) {
            throw new BadRequestException("Tiene un intento pendiente de revisión manual");
        }
        Optional<AcademyQuizAttempt> openAttempt = attempts
                .findFirstByEnrollmentIdAndQuizIdAndStatusOrderByStartedAtDesc(
                        enrollment.id, quiz.id, AcademyAttemptStatus.IN_PROGRESS);
        if (openAttempt.isPresent()) {
            AcademyQuizAttempt current = openAttempt.get();
            if (!isExpired(current)) return attempt(current, List.of());
            expireAttempt(current);
        }
        long used = attempts.countByEnrollmentIdAndQuizId(enrollment.id, quiz.id);
        if (quiz.maxAttempts > 0 && used >= quiz.maxAttempts) {
            throw new BadRequestException("Ya utilizó el máximo de intentos permitido");
        }
        int nextAttemptNumber = attempts.findFirstByEnrollmentIdAndQuizIdOrderByAttemptNumberDesc(enrollment.id, quiz.id)
                .map(value -> value.attemptNumber + 1)
                .orElse(1);
        AcademyQuizAttempt attempt = new AcademyQuizAttempt();
        attempt.enrollment = enrollment;
        attempt.quiz = quiz;
        attempt.attemptNumber = nextAttemptNumber;
        attempt.startedAt = Instant.now();
        // saveAndFlush detecta aquí mismo problemas de esquema o unicidad y evita
        // devolver un intento que finalmente no quedó persistido.
        attempts.saveAndFlush(attempt);
        events.publish(PluginEvent.of("academy.quiz.started",
                Map.of("courseId", enrollment.course.id, "quizId", quiz.id, "attemptId", attempt.id, "email", email)));
        return attempt(attempt, List.of());
    }

    @Transactional
    public AcademyDtos.QuizAttemptResponse finishQuiz(
            String attemptId, String email, AcademyDtos.FinishQuizRequest request) {
        guard.requireEnabled();
        AcademyQuizAttempt attempt = attempts.findByIdAndEnrollmentEmailIgnoreCase(attemptId, email)
                .orElseThrow(() -> new NotFoundException("Intento no encontrado"));
        if (attempt.status != AcademyAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("El intento ya no está abierto para recibir respuestas");
        }
        if (isExpired(attempt)) {
            expireAttempt(attempt);
            return attempt(attempt, List.of(Map.of(
                    "expired", true,
                    "message", "El tiempo de la evaluación finalizó")));
        }
        responses.deleteByAttemptId(attempt.id);
        Map<String, AcademyDtos.AnswerRequest> submitted = request.answers().stream()
                .collect(Collectors.toMap(AcademyDtos.AnswerRequest::questionId, Function.identity(), (a,b)->b));
        BigDecimal total = attempt.quiz.questions.stream().map(q -> q.points).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal awarded = BigDecimal.ZERO;
        List<Map<String,Object>> results = new ArrayList<>();
        for (AcademyQuestion question : attempt.quiz.questions) {
            AcademyDtos.AnswerRequest answer = submitted.get(question.id);
            Grade grade = grade(question, answer);
            awarded = awarded.add(grade.points());
            AcademyQuestionResponse entity = new AcademyQuestionResponse();
            entity.attempt = attempt;
            entity.question = question;
            entity.selectedOptionIds = writeJson(answer == null ? List.of() : list(answer.selectedOptionIds()));
            entity.textAnswer = answer == null ? null : nullable(answer.textAnswer());
            entity.correct = grade.correct();
            entity.awardedPoints = grade.points();
            responses.save(entity);
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("questionId", question.id);
            result.put("correct", grade.correct());
            result.put("awardedPoints", grade.points());
            result.put("explanation", question.explanation == null ? "" : question.explanation);
            results.add(result);
        }
        boolean requiresManualReview = attempt.quiz.questions.stream()
                .anyMatch(question -> question.type == AcademyQuestionType.LONG_TEXT);
        BigDecimal score = total.signum() == 0 ? BigDecimal.ZERO
                : awarded.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        attempt.score = requiresManualReview ? null : score;
        attempt.passed = requiresManualReview ? null : score.compareTo(attempt.quiz.passingScore) >= 0;
        attempt.status = requiresManualReview ? AcademyAttemptStatus.PENDING_REVIEW : AcademyAttemptStatus.FINISHED;
        attempt.finishedAt = Instant.now();
        attempts.save(attempt);
        if (requiresManualReview) {
            events.publish(PluginEvent.of("academy.quiz.submitted", Map.of(
                    "courseId", attempt.enrollment.course.id, "quizId", attempt.quiz.id,
                    "attemptId", attempt.id, "email", email)));
        } else {
            evaluateCompletion(attempt.enrollment);
            events.publish(PluginEvent.of("academy.quiz.completed",
                    Map.of("courseId", attempt.enrollment.course.id, "quizId", attempt.quiz.id,
                            "attemptId", attempt.id, "email", email, "score", score, "passed", attempt.passed)));
        }
        return attempt(attempt, results);
    }


    @Transactional
    public AcademyDtos.QuizAttemptResponse finishLatestQuiz(
            String quizId, String email, AcademyDtos.FinishQuizRequest request) {
        guard.requireEnabled();
        AcademyQuizAttempt attempt = attempts
                .findFirstByEnrollmentEmailIgnoreCaseAndQuizIdAndStatusOrderByStartedAtDesc(
                        email, quizId, AcademyAttemptStatus.IN_PROGRESS)
                .orElseThrow(() -> new NotFoundException("No existe un intento activo para esta evaluación"));
        return finishQuiz(attempt.id, email, request);
    }

    @Transactional(readOnly = true)
    public Map<String,Object> diagnostics() {
        guard.requireEnabled();
        List<Map<String,Object>> issues = new ArrayList<>();
        long published = 0;
        long emptyCourses = 0;
        long emptySections = 0;
        long emptyLessons = 0;
        long invalidQuizzes = 0;

        for (AcademyCourse course : courses.findAll()) {
            if (course.status == AcademyCourseStatus.PUBLISHED) published++;
            if (course.sections.isEmpty()) {
                emptyCourses++;
                issues.add(issue("COURSE_WITHOUT_SECTIONS", "warning", course.id, course.title,
                        "El curso no contiene módulos."));
            }
            for (AcademySection section : course.sections) {
                if (section.lessons.isEmpty() && section.quizzes.isEmpty()) {
                    emptySections++;
                    issues.add(issue("SECTION_WITHOUT_CONTENT", "warning", section.id, section.title,
                            "El módulo no contiene lecciones ni evaluaciones."));
                }
                for (AcademyLesson lesson : section.lessons) {
                    boolean noText = blank(lesson.content);
                    boolean noVideo = blank(lesson.videoUrl) && blank(lesson.mediaId);
                    if (noText && noVideo) {
                        emptyLessons++;
                        issues.add(issue("LESSON_WITHOUT_CONTENT", "error", lesson.id, lesson.title,
                                "La lección no tiene texto, video ni archivo multimedia."));
                    }
                }
                for (AcademyQuiz quiz : section.quizzes) {
                    boolean invalid = quiz.questions.isEmpty();
                    if (!invalid) {
                        invalid = quiz.questions.stream().anyMatch(question ->
                                question.type != AcademyQuestionType.LONG_TEXT
                                        && question.options.stream().noneMatch(option -> option.correct));
                    }
                    if (invalid) {
                        invalidQuizzes++;
                        issues.add(issue("QUIZ_WITHOUT_ANSWER_KEY", "error", quiz.id, quiz.title,
                                "La evaluación no tiene preguntas o alguna pregunta no posee respuesta correcta."));
                    }
                }
            }
        }

        LinkedHashMap<String,Object> totals = new LinkedHashMap<>();
        totals.put("courses", courses.count());
        totals.put("publishedCourses", published);
        totals.put("sections", sections.count());
        totals.put("lessons", lessons.count());
        totals.put("quizzes", quizzes.count());
        totals.put("questions", questions.count());
        totals.put("enrollments", enrollments.count());
        totals.put("attempts", attempts.count());
        totals.put("imports", importJobs.count());

        LinkedHashMap<String,Object> result = new LinkedHashMap<>();
        result.put("status", issues.stream().anyMatch(value -> "error".equals(value.get("severity"))) ? "DEGRADED" : "READY");
        result.put("generatedAt", Instant.now());
        result.put("totals", totals);
        result.put("emptyCourses", emptyCourses);
        result.put("emptySections", emptySections);
        result.put("emptyLessons", emptyLessons);
        result.put("invalidQuizzes", invalidQuizzes);
        result.put("issues", issues.stream().limit(100).toList());
        result.put("recommendations", List.of(
                "Mantenga al menos una lección publicada por módulo.",
                "Defina una respuesta correcta para cada pregunta calificable.",
                "Ejecute nuevamente los datos demo si el currículo de muestra quedó incompleto."));
        return result;
    }

    private Map<String,Object> issue(String code,String severity,String entityId,String title,String detail) {
        LinkedHashMap<String,Object> value = new LinkedHashMap<>();
        value.put("code", code);
        value.put("severity", severity);
        value.put("entityId", entityId);
        value.put("title", title);
        value.put("detail", detail);
        return value;
    }

    @Transactional(readOnly = true)
    public AcademyDtos.SettingsResponse settingsResponse() {
        guard.requireEnabled();
        return settings(settings());
    }

    @Transactional
    public AcademyDtos.SettingsResponse saveSettings(AcademyDtos.SettingsRequest request) {
        guard.requireEnabled();
        AcademySettings settings = settings();
        settings.catalogTitle = request.catalogTitle().trim();
        settings.catalogDescription = nullable(request.catalogDescription());
        settings.defaultCurrency = currency(request.defaultCurrency(), "USD");
        settings.defaultImportUrl = nullable(request.defaultImportUrl());
        settings.allowFreeEnrollment = request.allowFreeEnrollment();
        return settings(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    AcademyCourse requireCourseEntity(String id) { return requireCourse(id); }

    @Transactional(readOnly = true)
    Optional<AcademyCourse> findByExternalUrl(String url) { return courses.findByExternalSourceUrl(url); }

    @Transactional
    AcademyDtos.CourseDetail importCourse(AcademyCourse existing, AcademyDtos.CourseRequest request) {
        return saveCourse(existing == null ? null : existing.id, request);
    }

    private AcademyEnrollment createEnrollment(
            AcademyCourse course, UserAccount user, AcademyEnrollmentSource source, String orderId) {
        return createEnrollment(course, user.getId(), user.getEmail(), source, orderId);
    }

    private AcademyEnrollment createEnrollment(
            AcademyCourse course, String userId, String email,
            AcademyEnrollmentSource source, String orderId) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        AcademyEnrollment enrollment = enrollments.findByCourseIdAndEmailIgnoreCase(course.id, normalizedEmail)
                .orElseGet(AcademyEnrollment::new);
        boolean created = enrollment.course == null;
        enrollment.course = course;
        if (userId != null && !userId.isBlank()) enrollment.userId = userId;
        enrollment.email = normalizedEmail;
        if (created || enrollment.status == AcademyEnrollmentStatus.CANCELLED) {
            enrollment.status = AcademyEnrollmentStatus.ACTIVE;
            enrollment.completedAt = null;
        }
        enrollment.source = source;
        if (orderId != null) enrollment.orderId = orderId;
        enrollment = enrollments.save(enrollment);
        if (created) {
            events.publish(PluginEvent.of("academy.enrollment.created",
                    Map.of("courseId", course.id, "enrollmentId", enrollment.id,
                            "email", enrollment.email, "source", source.name())));
            mail.send(enrollment.email, "Acceso a " + course.title,
                    "Su inscripción está activa. Cree o abra su cuenta con este correo para comenzar el curso " + course.title + ".");
        }
        return enrollment;
    }

    private void evaluateCompletion(AcademyEnrollment enrollment) {
        int totalLessons = lessonCount(enrollment.course);
        long completedLessons = progress.countByEnrollmentIdAndCompletedTrue(enrollment.id);
        boolean lessonsDone = totalLessons == 0 || completedLessons >= totalLessons;
        List<AcademyQuiz> courseQuizzes = enrollment.course.sections.stream().flatMap(section -> section.quizzes.stream()).toList();
        Set<String> passedQuizIds = attempts.findByEnrollmentIdOrderByStartedAtDesc(enrollment.id).stream()
                .filter(item -> item.status == AcademyAttemptStatus.FINISHED && Boolean.TRUE.equals(item.passed))
                .map(item -> item.quiz.id).collect(Collectors.toSet());
        boolean quizzesDone = courseQuizzes.stream().allMatch(quiz -> passedQuizIds.contains(quiz.id));
        if (lessonsDone && quizzesDone && enrollment.status != AcademyEnrollmentStatus.COMPLETED) {
            enrollment.status = AcademyEnrollmentStatus.COMPLETED;
            enrollment.completedAt = Instant.now();
            enrollments.save(enrollment);
            events.publish(PluginEvent.of("academy.course.completed",
                    Map.of("courseId", enrollment.course.id,
                            "courseTitle", enrollment.course.title,
                            "enrollmentId", enrollment.id,
                            "email", enrollment.email)));
            mail.send(enrollment.email, "Curso completado: " + enrollment.course.title,
                    "¡Felicidades! Ha completado el curso " + enrollment.course.title + ".");
        }
    }

    private String courseIdForLesson(String lessonId) {
        return lessons.findCourseIdByLessonId(lessonId, com.nattechnologies.trastcms.multisite.SiteContext.siteId())
                .orElseThrow(() -> new NotFoundException("No se pudo determinar el curso de la lección"));
    }

    private String courseIdForQuiz(String quizId) {
        return quizzes.findCourseIdByQuizId(quizId, com.nattechnologies.trastcms.multisite.SiteContext.siteId())
                .orElseThrow(() -> new NotFoundException("No se pudo determinar el curso de la evaluación"));
    }

    private String courseIdForSection(String sectionId) {
        return sections.findCourseIdBySectionId(sectionId, com.nattechnologies.trastcms.multisite.SiteContext.siteId())
                .orElseThrow(() -> new NotFoundException("No se pudo determinar el curso de la sección"));
    }

    private String courseIdForQuestion(String questionId) {
        return questions.findCourseIdByQuestionId(questionId, com.nattechnologies.trastcms.multisite.SiteContext.siteId())
                .orElseThrow(() -> new NotFoundException("No se pudo determinar el curso de la pregunta"));
    }

    private AcademyEnrollment requireEnrollment(String courseId, String email) {
        return enrollments.findByCourseIdAndEmailIgnoreCase(courseId, email)
                .filter(value -> value.status != AcademyEnrollmentStatus.CANCELLED)
                .orElseThrow(() -> new NotFoundException("No tiene una inscripción activa para este curso"));
    }

    private AcademyCourse requireCourse(String id) {
        return courses.findById(id).orElseThrow(() -> new NotFoundException("Curso no encontrado"));
    }

    private void syncSections(AcademyCourse course, List<AcademyDtos.SectionRequest> requests, boolean deletionsAllowed) {
        Map<String,AcademySection> existing = course.sections.stream()
                .collect(Collectors.toMap(value -> value.id, Function.identity()));
        Set<String> seen = new HashSet<>();
        int index = 0;
        for (AcademyDtos.SectionRequest request : requests) {
            int position = optional(request.position(), index++);
            AcademySection section = request.id() == null ? null : existing.get(request.id());
            if (section == null && !blank(request.externalSourceId())) {
                section = course.sections.stream().filter(value -> Objects.equals(
                        value.externalSourceId, request.externalSourceId().trim())).findFirst().orElse(null);
            }
            if (section == null) {
                section = course.sections.stream().filter(value -> value.position == position
                        && normalize(value.title).equals(normalize(request.title()))).findFirst().orElse(null);
            }
            if (section == null) section = new AcademySection();
            section.course = course;
            section.title = request.title().trim();
            section.description = nullable(request.description());
            section.position = position;
            section.externalSourceId = nullable(request.externalSourceId());
            if (!course.sections.contains(section)) course.sections.add(section);
            seen.add(section.id);
            syncLessons(section, list(request.lessons()), deletionsAllowed);
            syncQuizzes(section, list(request.quizzes()), deletionsAllowed);
        }
        if (deletionsAllowed) course.sections.removeIf(value -> !seen.contains(value.id));
    }

    private void syncLessons(AcademySection section, List<AcademyDtos.LessonRequest> requests, boolean deletionsAllowed) {
        Map<String,AcademyLesson> existing = section.lessons.stream()
                .collect(Collectors.toMap(value -> value.id, Function.identity()));
        Set<String> seen = new HashSet<>(); int index=0;
        for (AcademyDtos.LessonRequest request : requests) {
            int position = optional(request.position(), index++);
            AcademyLesson lesson = request.id()==null ? null : existing.get(request.id());
            if (lesson == null && !blank(request.externalSourceId())) {
                lesson = section.lessons.stream().filter(value -> Objects.equals(
                        value.externalSourceId, request.externalSourceId().trim())).findFirst().orElse(null);
            }
            if (lesson == null) {
                lesson = section.lessons.stream().filter(value -> value.position == position
                        && normalize(value.title).equals(normalize(request.title()))).findFirst().orElse(null);
            }
            if (lesson == null) lesson = new AcademyLesson();
            lesson.section=section; lesson.title=request.title().trim(); lesson.content=sanitizer.html(request.content());
            lesson.position=position; lesson.lessonType=value(request.lessonType(), AcademyLessonType.TEXT);
            lesson.videoProvider=request.videoProvider(); lesson.videoUrl=nullable(request.videoUrl()); lesson.mediaId=nullable(request.mediaId());
            lesson.durationSeconds=Math.max(0,optional(request.durationSeconds(),0)); lesson.preview=request.preview(); lesson.externalSourceId=nullable(request.externalSourceId());
            if(lesson.videoProvider==AcademyVideoProvider.YOUTUBE && lesson.videoUrl!=null) lesson.videoUrl=canonicalYoutube(lesson.videoUrl);
            if(!section.lessons.contains(lesson)) section.lessons.add(lesson); seen.add(lesson.id);
        }
        if(deletionsAllowed) section.lessons.removeIf(value->!seen.contains(value.id));
    }

    private void syncQuizzes(AcademySection section, List<AcademyDtos.QuizRequest> requests, boolean deletionsAllowed) {
        Map<String,AcademyQuiz> existing = section.quizzes.stream()
                .collect(Collectors.toMap(value -> value.id, Function.identity()));
        Set<String> seen=new HashSet<>(); int index=0;
        for(AcademyDtos.QuizRequest request:requests){
            int position=optional(request.position(),index++);
            AcademyQuiz quiz=request.id()==null?null:existing.get(request.id());
            if(quiz==null&&!blank(request.externalSourceId())){
                quiz=section.quizzes.stream().filter(value->Objects.equals(
                        value.externalSourceId,request.externalSourceId().trim())).findFirst().orElse(null);
            }
            if(quiz==null){
                quiz=section.quizzes.stream().filter(value->value.position==position
                        &&normalize(value.title).equals(normalize(request.title()))).findFirst().orElse(null);
            }
            if(quiz==null)quiz=new AcademyQuiz();
            quiz.section=section; quiz.title=request.title().trim(); quiz.description=sanitizer.html(request.description()); quiz.position=position;
            quiz.passingScore=request.passingScore()==null?BigDecimal.valueOf(70):request.passingScore(); quiz.maxAttempts=Math.max(0,optional(request.maxAttempts(),3));
            quiz.durationMinutes=Math.max(0,optional(request.durationMinutes(),0)); quiz.randomizeQuestions=request.randomizeQuestions(); quiz.preview=request.preview(); quiz.externalSourceId=nullable(request.externalSourceId());
            if(!section.quizzes.contains(quiz)) section.quizzes.add(quiz);
            seen.add(quiz.id);
            syncQuestions(quiz,list(request.questions()),deletionsAllowed);
            if (section.course.status == AcademyCourseStatus.PUBLISHED && quiz.questions.isEmpty()) {
                throw new BadRequestException("La evaluación '" + quiz.title + "' necesita al menos una pregunta antes de publicar");
            }
        }
        if(deletionsAllowed) section.quizzes.removeIf(value->!seen.contains(value.id));
    }

    private void syncQuestions(AcademyQuiz quiz,List<AcademyDtos.QuestionRequest> requests,boolean deletionsAllowed){
        Map<String,AcademyQuestion> existing=quiz.questions.stream()
                .collect(Collectors.toMap(value->value.id,Function.identity()));
        Set<String> seen=new HashSet<>(); int index=0;
        for(AcademyDtos.QuestionRequest request:requests){
            int position=optional(request.position(),index++);
            AcademyQuestion question=request.id()==null?null:existing.get(request.id());
            if(question==null){
                question=quiz.questions.stream().filter(value->value.position==position
                        &&value.type==value(request.type(),AcademyQuestionType.SINGLE_CHOICE)
                        &&normalize(value.text).equals(normalize(request.text()))).findFirst().orElse(null);
            }
            if(question==null)question=new AcademyQuestion();
            question.quiz=quiz; question.text=request.text().trim(); question.type=value(request.type(),AcademyQuestionType.SINGLE_CHOICE); question.position=position;
            question.points=request.points()==null?BigDecimal.ONE:request.points(); question.explanation=sanitizer.html(request.explanation()); question.required=request.required();
            if(!quiz.questions.contains(question)) quiz.questions.add(question); seen.add(question.id); syncOptions(question,list(request.options()),deletionsAllowed);
            boolean answerKeyRequired = List.of(
                    AcademyQuestionType.SINGLE_CHOICE,
                    AcademyQuestionType.MULTIPLE_CHOICE,
                    AcademyQuestionType.TRUE_FALSE,
                    AcademyQuestionType.SHORT_TEXT).contains(question.type);
            if (quiz.section.course.status == AcademyCourseStatus.PUBLISHED
                    && answerKeyRequired
                    && question.options.stream().noneMatch(option -> option.correct)) {
                throw new BadRequestException("La pregunta '" + question.text + "' necesita una respuesta correcta antes de publicar");
            }
        }
        if(deletionsAllowed) quiz.questions.removeIf(value->!seen.contains(value.id));
    }

    private void syncOptions(AcademyQuestion question,List<AcademyDtos.OptionRequest> requests,boolean deletionsAllowed){
        Map<String,AcademyAnswerOption> existing=question.options.stream()
                .collect(Collectors.toMap(value->value.id,Function.identity()));
        Set<String> seen=new HashSet<>(); int index=0;
        for(AcademyDtos.OptionRequest request:requests){
            int position=optional(request.position(),index++);
            AcademyAnswerOption option=request.id()==null?null:existing.get(request.id());
            if(option==null){
                option=question.options.stream().filter(value->value.position==position
                        &&normalize(value.text).equals(normalize(request.text()))).findFirst().orElse(null);
            }
            if(option==null)option=new AcademyAnswerOption();
            option.question=question; option.text=request.text().trim(); option.position=position; option.correct=request.correct();
            if(!question.options.contains(option)) question.options.add(option); seen.add(option.id);
        }
        if(deletionsAllowed) question.options.removeIf(value->!seen.contains(value.id));
    }

    private boolean isExpired(AcademyQuizAttempt attempt) {
        return attempt.quiz.durationMinutes > 0
                && !Instant.now().isBefore(attempt.startedAt.plusSeconds(attempt.quiz.durationMinutes * 60L));
    }

    private void expireAttempt(AcademyQuizAttempt attempt) {
        attempt.score = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        attempt.passed = false;
        attempt.status = AcademyAttemptStatus.FINISHED;
        attempt.finishedAt = Instant.now();
        attempts.save(attempt);
        evaluateCompletion(attempt.enrollment);
        events.publish(PluginEvent.of("academy.quiz.completed", Map.of(
                "courseId", attempt.enrollment.course.id,
                "quizId", attempt.quiz.id,
                "attemptId", attempt.id,
                "email", attempt.enrollment.email,
                "score", attempt.score,
                "passed", false,
                "expired", true)));
    }

    private Grade grade(AcademyQuestion question, AcademyDtos.AnswerRequest answer) {
        if (answer == null) return new Grade(false, BigDecimal.ZERO);
        if (question.type == AcademyQuestionType.SHORT_TEXT) {
            String actual = normalize(answer.textAnswer());
            boolean correct = question.options.stream().filter(option -> option.correct)
                    .map(option -> normalize(option.text)).anyMatch(actual::equals);
            return new Grade(correct, correct ? question.points : BigDecimal.ZERO);
        }
        if (question.type == AcademyQuestionType.LONG_TEXT) return new Grade(null, BigDecimal.ZERO);
        Set<String> selected = new HashSet<>(list(answer.selectedOptionIds()));
        Set<String> correctIds = question.options.stream().filter(option -> option.correct).map(option -> option.id).collect(Collectors.toSet());
        boolean correct = !correctIds.isEmpty() && selected.equals(correctIds);
        return new Grade(correct, correct ? question.points : BigDecimal.ZERO);
    }

    private AcademyDtos.CourseSummary summary(AcademyCourse course) {
        return new AcademyDtos.CourseSummary(course.id,course.title,course.slug,course.excerpt,course.featuredImageUrl,
                course.instructorName,course.category,course.level,course.accessType,course.status,course.price,course.currency,
                course.estimatedMinutes,course.featured,course.demoData,course.sections.size(),lessonCount(course),quizCount(course),
                enrollments.countByCourseId(course.id),course.createdAt,course.updatedAt);
    }

    private AcademyDtos.CourseDetail detail(AcademyCourse course, boolean admin, AcademyEnrollment enrollment, boolean fullAccess) {
        Map<String,AcademyLessonProgress> states = enrollment==null?Map.of():progress.findByEnrollmentId(enrollment.id).stream()
                .collect(Collectors.toMap(item->item.lesson.id,Function.identity()));
        List<AcademyDtos.SectionResponse> sections=course.sections.stream().map(section->new AcademyDtos.SectionResponse(
                section.id,section.title,section.description,section.position,section.externalSourceId,
                section.lessons.stream().map(lesson->{
                    AcademyLessonProgress state=states.get(lesson.id); boolean visible=admin||fullAccess||lesson.preview;
                    return new AcademyDtos.LessonResponse(lesson.id,lesson.title,visible?lesson.content:null,lesson.position,lesson.lessonType,
                            lesson.videoProvider,visible?lesson.videoUrl:null,visible?lesson.mediaId:null,lesson.durationSeconds,lesson.preview,lesson.externalSourceId,
                            state!=null&&state.completed,state==null?0:state.watchedSeconds);
                }).toList(),
                section.quizzes.stream().map(quiz->{
                    boolean visible=admin||fullAccess||quiz.preview;
                    long used=enrollment==null?0:attempts.countByEnrollmentIdAndQuizId(enrollment.id,quiz.id);
                    List<AcademyDtos.QuestionResponse> questionRows=visible?new ArrayList<>(quiz.questions.stream().map(question->new AcademyDtos.QuestionResponse(
                            question.id,question.text,question.type,question.position,question.points,
                            admin?question.explanation:null,question.required,
                            question.options.stream().map(option->new AcademyDtos.OptionResponse(option.id,option.text,option.position,admin?option.correct:null)).toList())).toList()):new ArrayList<>();
                    if (!admin && quiz.randomizeQuestions) Collections.shuffle(questionRows);
                    return new AcademyDtos.QuizResponse(quiz.id,quiz.title,visible?quiz.description:null,quiz.position,quiz.passingScore,quiz.maxAttempts,
                            quiz.durationMinutes,quiz.randomizeQuestions,quiz.preview,quiz.externalSourceId,questionRows,used);
                }).toList())).toList();
        return new AcademyDtos.CourseDetail(course.id,course.title,course.slug,course.excerpt,course.description,course.featuredImageUrl,
                course.instructorName,course.category,course.level,course.accessType,course.status,course.price,course.currency,course.estimatedMinutes,
                course.featured,course.demoData,course.externalSourceId,course.externalSourceUrl,sections,enrollment==null?null:enrollment(enrollment),course.createdAt,course.updatedAt);
    }

    private AcademyDtos.EnrollmentResponse enrollment(AcademyEnrollment value) {
        int totalLessons=lessonCount(value.course);
        int completedLessons=(int)progress.countByEnrollmentIdAndCompletedTrue(value.id);
        int totalQuizzes=quizCount(value.course);
        int completedQuizzes=(int)attempts.findByEnrollmentIdOrderByStartedAtDesc(value.id).stream()
                .filter(item->item.status==AcademyAttemptStatus.FINISHED&&Boolean.TRUE.equals(item.passed))
                .map(item->item.quiz.id).distinct().count();
        int totalItems=totalLessons+totalQuizzes;
        int completedItems=completedLessons+completedQuizzes;
        int percentage=totalItems==0?(value.status==AcademyEnrollmentStatus.COMPLETED?100:0)
                :(int)Math.min(100,Math.round(completedItems*100.0/totalItems));
        return new AcademyDtos.EnrollmentResponse(value.id,value.course.id,value.course.title,value.course.slug,value.course.featuredImageUrl,
                value.userId,value.email,value.status,value.source,value.orderId,completedLessons,totalLessons,
                completedQuizzes,totalQuizzes,percentage,value.enrolledAt,value.completedAt,value.updatedAt);
    }

    private AcademyDtos.QuizAttemptResponse attempt(AcademyQuizAttempt value,List<Map<String,Object>> results){
        return new AcademyDtos.QuizAttemptResponse(value.id,value.quiz.id,value.quiz.title,value.attemptNumber,value.status,value.score,value.passed,value.startedAt,value.finishedAt,results);
    }

    private AcademyDtos.SettingsResponse settings(AcademySettings value){return new AcademyDtos.SettingsResponse(value.catalogTitle,value.catalogDescription,value.defaultCurrency,value.defaultImportUrl,value.allowFreeEnrollment,value.createdAt,value.updatedAt);}
    private AcademyDtos.ImportJobResponse importJob(AcademyImportJob job){return new AcademyDtos.ImportJobResponse(job.id,job.sourceUrl,job.sourceType,job.status,job.detectedTitle,job.resultCourseId,readWarnings(job.warningsJson),job.errorMessage,job.requestedBy,job.startedAt,job.finishedAt);}
    AcademyDtos.ImportJobResponse importJobResponse(AcademyImportJob job){return importJob(job);}

    private AcademySettings settings(){return settingsRepository.findById(AcademySettings.currentId()).orElseGet(()->settingsRepository.save(new AcademySettings()));}
    private int lessonCount(AcademyCourse course){return course.sections.stream().mapToInt(section->section.lessons.size()).sum();}
    private int quizCount(AcademyCourse course){return course.sections.stream().mapToInt(section->section.quizzes.size()).sum();}
    private String canonicalYoutube(String value){
        String raw=value.trim(); String id=null; String playlist=null; String index=null;
        try{
            java.net.URI uri=java.net.URI.create(raw);
            String host=Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            String path=Optional.ofNullable(uri.getPath()).orElse("");
            boolean youtubeHost=host.equals("youtu.be")||host.endsWith(".youtu.be")
                    ||host.equals("youtube.com")||host.endsWith(".youtube.com")
                    ||host.equals("youtube-nocookie.com")||host.endsWith(".youtube-nocookie.com");
            if(!youtubeHost) return raw;
            for(String part:Optional.ofNullable(uri.getQuery()).orElse("").split("&")){
                if(part.startsWith("v=")) id=part.substring(2);
                else if(part.startsWith("list=")) playlist=part.substring(5);
                else if(part.startsWith("index=")) index=part.substring(6);
            }
            if(host.equals("youtu.be")||host.endsWith(".youtu.be")) id=path.replaceFirst("^/","").split("/")[0];
            else if(host.equals("youtube.com")||host.endsWith(".youtube.com")
                    ||host.equals("youtube-nocookie.com")||host.endsWith(".youtube-nocookie.com")){
                if(path.startsWith("/embed/videoseries")) id=null;
                else if(path.startsWith("/embed/")) id=path.substring(7).split("/")[0];
                else if(path.startsWith("/shorts/")) id=path.substring(8).split("/")[0];
            }
        }catch(Exception ignored){ }
        if(id!=null&&!id.matches("[A-Za-z0-9_-]{6,}")) id=null;
        if(playlist!=null&&!playlist.matches("[A-Za-z0-9_-]{6,}")) playlist=null;
        if(index!=null&&!index.matches("\\d+")) index=null;
        if(id==null&&playlist==null) return raw;
        StringBuilder canonical=new StringBuilder("https://www.youtube.com/");
        if(id!=null) canonical.append("watch?v=").append(id);
        else canonical.append("playlist?list=").append(playlist);
        if(id!=null&&playlist!=null) canonical.append("&list=").append(playlist);
        if(index!=null) canonical.append("&index=").append(index);
        return canonical.toString();
    }
    private String currency(String value,String fallback){String result=blank(value)?fallback:value.trim().toUpperCase(Locale.ROOT);return result.matches("[A-Z]{3}")?result:fallback;}
    private BigDecimal money(BigDecimal value){return value==null?BigDecimal.ZERO:value.max(BigDecimal.ZERO).setScale(4,RoundingMode.HALF_UP);}
    private int optional(Integer value,int fallback){return value==null?fallback:value;}
    private <T> T value(T current,T fallback){return current==null?fallback:current;}
    private boolean blank(String value){return value==null||value.isBlank();}
    private String nullable(String value){return blank(value)?null:value.trim();}
    private String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    private <T> List<T> list(List<T> value){return value==null?List.of():value;}
    private String writeJson(Object value){try{return mapper.writeValueAsString(value);}catch(JsonProcessingException exception){throw new BadRequestException("No se pudo guardar la respuesta");}}
    private List<String> readWarnings(String value){if(blank(value))return List.of();try{return mapper.readValue(value,new TypeReference<>(){});}catch(Exception ignored){return List.of(value);}}
    private record Grade(Boolean correct,BigDecimal points){}
}
