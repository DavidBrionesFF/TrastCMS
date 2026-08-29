package com.nattechnologies.trastcms.plugins.academy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.ConflictException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AcademyImportService {
    private static final int MAX_RESPONSE_CHARS = 5_000_000;
    private static final Pattern YOUTUBE = Pattern.compile(
            "(?i)(?:https?:)?//(?:www\\.)?(?:youtube(?:-nocookie)?\\.com/(?:watch\\?[^\\s\"']*v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{6,})");

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final AcademyService academy;
    private final AcademyImportJobRepository jobs;

    AcademyImportService(RestClient restClient, ObjectMapper mapper, AcademyService academy, AcademyImportJobRepository jobs) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.academy = academy;
        this.jobs = jobs;
    }

    public AcademyDtos.ImportPreview preview(AcademyDtos.ImportRequest request) {
        URI uri = validateExternalUrl(request.url());
        String body = fetch(uri, request.bearerToken());
        boolean json = body.stripLeading().startsWith("{") || body.stripLeading().startsWith("[");
        Normalized normalized = json ? fromJson(uri, body, request.bearerToken()) : fromHtml(uri, body, request.bearerToken());
        AcademyDtos.CourseRequest course = normalized.course();
        int sections = list(course.sections()).size();
        int lessons = list(course.sections()).stream().mapToInt(section -> list(section.lessons()).size()).sum();
        int quizzes = list(course.sections()).stream().mapToInt(section -> list(section.quizzes()).size()).sum();
        int questions = list(course.sections()).stream().flatMap(section -> list(section.quizzes()).stream())
                .mapToInt(quiz -> list(quiz.questions()).size()).sum();
        return new AcademyDtos.ImportPreview(normalized.sourceType(), uri.toString(), course.title(), course.excerpt(),
                course.description(), course.featuredImageUrl(), normalized.youtubeVideos(), sections,
                lessons, quizzes, questions, normalized.warnings(), course);
    }

    public AcademyDtos.ImportJobResponse execute(AcademyDtos.ImportRequest request, String actor) {
        AcademyImportJob job = new AcademyImportJob();
        job.sourceUrl = validateExternalUrl(request.url()).toString();
        job.sourceType = "AUTO";
        job.status = "RUNNING";
        job.requestedBy = actor;
        jobs.save(job);
        try {
            AcademyDtos.ImportPreview preview = preview(request);
            job.sourceType = preview.sourceType();
            job.detectedTitle = preview.title();
            AcademyCourse existing = academy.findByExternalUrl(job.sourceUrl).orElse(null);
            if (existing != null && !request.overwriteExisting()) {
                throw new ConflictException("Ese curso ya fue importado. Active la opción de actualizar para sincronizarlo.");
            }
            AcademyDtos.CourseRequest raw = preview.normalizedCourse();
            AcademyDtos.CourseRequest course = new AcademyDtos.CourseRequest(
                    raw.title(), raw.slug(), raw.excerpt(), raw.description(), raw.featuredImageUrl(),
                    raw.instructorName(), raw.category(), raw.level(), raw.accessType(),
                    request.publish() ? AcademyCourseStatus.PUBLISHED : AcademyCourseStatus.DRAFT,
                    raw.price(), raw.currency(), raw.estimatedMinutes(), raw.featured(),
                    raw.externalSourceId(), job.sourceUrl, raw.sections());
            AcademyDtos.CourseDetail result = academy.importCourse(existing, course);
            job.resultCourseId = result.id();
            job.status = "COMPLETED";
            job.warningsJson = mapper.writeValueAsString(preview.warnings());
            job.finishedAt = Instant.now();
            return academy.importJobResponse(jobs.save(job));
        } catch (RuntimeException exception) {
            job.status = "FAILED";
            job.errorMessage = Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName());
            job.finishedAt = Instant.now();
            jobs.save(job);
            throw exception;
        } catch (Exception exception) {
            job.status = "FAILED";
            job.errorMessage = Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName());
            job.finishedAt = Instant.now();
            jobs.save(job);
            throw new BadRequestException("No se pudo completar la importación: " + job.errorMessage);
        }
    }

    @Transactional(readOnly = true)
    public List<AcademyDtos.ImportJobResponse> history() {
        return jobs.findTop20ByOrderByStartedAtDesc().stream().map(academy::importJobResponse).toList();
    }

    private String fetch(URI uri, String bearerToken) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri).header(HttpHeaders.ACCEPT,
                    "application/json,text/html;q=0.9,application/xhtml+xml;q=0.8");
            if (bearerToken != null && !bearerToken.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken.trim());
            }
            String body = request.retrieve().body(String.class);
            if (body == null || body.isBlank()) throw new BadRequestException("La fuente no devolvió contenido");
            if (body.length() > MAX_RESPONSE_CHARS) throw new BadRequestException("La respuesta supera el límite de 5 MB");
            return body;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo consultar la fuente: " +
                    Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName()));
        }
    }

    private Normalized fromHtml(URI uri, String html, String bearerToken) {
        Document document = Jsoup.parse(html, uri.toString());
        List<String> pageVideos = youtubeVideos(html);
        List<String> detectionWarnings = new ArrayList<>();
        LearnPressSource source = detectLearnPressSource(document, html, uri);
        if (source != null) {
            try {
                URI endpoint = source.apiRoot().resolve("courses/" + source.courseId());
                Normalized normalized = fromJson(endpoint, fetchLearnPressCourse(endpoint, bearerToken), bearerToken);
                LinkedHashSet<String> videos = new LinkedHashSet<>(normalized.youtubeVideos());
                videos.addAll(pageVideos);
                List<String> warnings = new ArrayList<>(normalized.warnings());
                warnings.addAll(detectionWarnings);
                return new Normalized("LEARNPRESS_REST", normalized.course(), List.copyOf(videos), warnings);
            } catch (RuntimeException exception) {
                detectionWarnings.add("Se detectó LearnPress, pero no fue posible recorrer su API REST: "
                        + Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName())
                        + ". Se importará únicamente el contenido público disponible en la página.");
            }
        }

        String title = text(document.selectFirst("h1"));
        if (title == null) title = document.title();
        if (title == null || title.isBlank()) title = "Curso importado";
        String excerpt = Optional.ofNullable(document.selectFirst("meta[name=description]"))
                .map(element -> element.attr("content")).filter(value -> !value.isBlank()).orElse(null);
        Element content = first(document, "article", ".course-content", ".learn-press-course", "main", "body");
        String description = content == null ? "" : content.html();
        String image = Optional.ofNullable(document.selectFirst("meta[property=og:image]"))
                .map(element -> element.attr("content")).filter(value -> !value.isBlank()).orElse(null);
        List<AcademyDtos.LessonRequest> lessons = new ArrayList<>();
        int position = 0;
        for (String video : pageVideos) {
            String videoId = video.substring(video.lastIndexOf('=') + 1);
            Element frame = document.selectFirst("iframe[src*=" + videoId + "],a[href*=" + videoId + "]");
            String lessonTitle = nearestHeading(frame);
            lessons.add(new AcademyDtos.LessonRequest(null,
                    lessonTitle == null ? "Lección en video " + (position + 1) : lessonTitle,
                    frame == null ? "" : frame.parent() == null ? "" : frame.parent().html(), position++,
                    AcademyLessonType.VIDEO, AcademyVideoProvider.YOUTUBE, video, null, 0,
                    position == 1, videoId));
        }
        List<String> warnings = new ArrayList<>(detectionWarnings);
        if (lessons.isEmpty()) {
            lessons.add(new AcademyDtos.LessonRequest(null, title, description, 0,
                    AcademyLessonType.TEXT, null, null, null, 0, true, null));
            warnings.add("No se detectaron videos de YouTube; el contenido se importó como una lección de texto.");
        }
        if (!document.select(".quiz,.lp-quiz,.question,.learn-press-question").isEmpty()) {
            warnings.add("La página contiene señales de cuestionarios, pero el HTML público no expone de forma segura las respuestas correctas. Use un endpoint REST autenticado para importarlos completos.");
        }
        AcademyDtos.SectionRequest section = new AcademyDtos.SectionRequest(null, "Contenido del curso",
                "Sección creada desde la página importada.", 0, null, lessons, List.of());
        AcademyDtos.CourseRequest course = new AcademyDtos.CourseRequest(title, null, excerpt, description, image,
                null, "Importados", AcademyLevel.ALL_LEVELS, AcademyAccessType.FREE, AcademyCourseStatus.DRAFT,
                BigDecimal.ZERO, "USD", lessons.stream().mapToInt(value -> value.durationSeconds() == null ? 0 : value.durationSeconds()).sum() / 60,
                false, null, uri.toString(), List.of(section));
        return new Normalized("HTML", course, pageVideos, warnings);
    }

    private Normalized fromJson(URI uri, String json, String bearerToken) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root.isArray()) {
                if (root.isEmpty()) throw new BadRequestException("La API devolvió una colección vacía");
                root = root.get(0);
            }
            if (root.has("data") && root.get("data").isObject()) root = root.get("data");
            String title = firstText(root, "title.rendered", "title", "name", "course_name");
            if (title == null) title = "Curso importado";
            String excerpt = firstText(root, "excerpt.rendered", "excerpt", "short_description", "summary");
            String description = firstText(root, "content.rendered", "content", "description");
            String image = firstText(root, "featured_image_url", "image", "thumbnail", "featured_image");
            String externalId = firstText(root, "id", "course_id");
            List<String> videos = new ArrayList<>(youtubeVideos(json));
            List<String> warnings = new ArrayList<>();
            URI learnPressRoot = learnPressApiRoot(uri);
            boolean learnPress = learnPressRoot != null && looksLikeLearnPress(root, uri);
            List<AcademyDtos.SectionRequest> sections = learnPress
                    ? parseLearnPressSections(root, learnPressRoot, externalId, bearerToken, warnings)
                    : parseSections(root, warnings);
            sections.stream().flatMap(section -> list(section.lessons()).stream())
                    .map(AcademyDtos.LessonRequest::videoUrl).filter(Objects::nonNull)
                    .filter(value -> value.contains("youtu")).forEach(videos::add);
            videos = List.copyOf(new LinkedHashSet<>(videos));
            if (sections.isEmpty()) {
                List<AcademyDtos.LessonRequest> lessonRows = new ArrayList<>();
                int index = 0;
                for (String video : videos) lessonRows.add(new AcademyDtos.LessonRequest(null,
                        "Lección en video " + (++index), "", index - 1, AcademyLessonType.VIDEO,
                        AcademyVideoProvider.YOUTUBE, video, null, 0, index == 1, null));
                if (lessonRows.isEmpty()) lessonRows.add(new AcademyDtos.LessonRequest(null, title,
                        description, 0, AcademyLessonType.TEXT, null, null, null, 0, true, externalId));
                sections = List.of(new AcademyDtos.SectionRequest(null, "Contenido del curso", null, 0, null, lessonRows, List.of()));
                warnings.add("La respuesta no incluyó una estructura explícita de secciones; se normalizó el contenido disponible.");
            }
            String category = firstText(root, "category.name", "category", "course_category");
            AcademyDtos.CourseRequest course = new AcademyDtos.CourseRequest(title, null, excerpt, description, image,
                    firstText(root, "instructor.name", "instructor", "author_name"),
                    category == null ? "Importados" : category,
                    AcademyLevel.ALL_LEVELS,
                    price(root).signum() > 0 ? AcademyAccessType.PAID : AcademyAccessType.FREE,
                    AcademyCourseStatus.DRAFT, price(root), currency(root), durationMinutes(root, "duration", "duration_minutes"),
                    booleanValue(root, "featured"), externalId, uri.toString(), sections);
            return new Normalized(learnPress ? "LEARNPRESS_REST" : "JSON", course, videos, warnings);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("La respuesta JSON no tiene un formato de curso reconocible");
        }
    }

    private List<AcademyDtos.SectionRequest> parseLearnPressSections(
            JsonNode root, URI apiRoot, String courseId, String bearerToken, List<String> warnings) {
        JsonNode sectionsNode = firstArray(root, "sections", "course_data.sections", "curriculum.sections", "course.sections");
        if ((sectionsNode == null || sectionsNode.isEmpty()) && !blank(courseId)) {
            try {
                sectionsNode = fetchLearnPressCollection(
                        apiRoot.resolve("sections/sections-by-course-id/" + courseId), bearerToken, 100, 20);
            } catch (RuntimeException exception) {
                warnings.add("La respuesta del curso no incluyó el currículo y tampoco se pudieron consultar sus secciones: "
                        + Optional.ofNullable(exception.getMessage()).orElse("respuesta no disponible") + ".");
            }
        }
        if (sectionsNode == null || sectionsNode.isEmpty()) return List.of();

        List<AcademyDtos.SectionRequest> result = new ArrayList<>();
        int sectionPosition = 0;
        int traversedItems = 0;
        boolean limitReached = false;
        for (JsonNode section : sectionsNode) {
            String sectionTitle = Optional.ofNullable(firstText(section, "title", "name", "section_name"))
                    .orElse("Sección " + (sectionPosition + 1));
            String sectionId = firstText(section, "id", "section_id");
            List<AcademyDtos.LessonRequest> lessonRows = new ArrayList<>();
            List<AcademyDtos.QuizRequest> quizRows = new ArrayList<>();
            JsonNode items = firstArray(section, "items", "section_items", "lessons");
            if ((items == null || items.isEmpty()) && !blank(sectionId)) {
                try {
                    items = fetchLearnPressCollection(
                            apiRoot.resolve("section-items/items/" + sectionId), bearerToken, 100, 20);
                } catch (RuntimeException exception) {
                    warnings.add("No se pudieron consultar los elementos de la sección '" + sectionTitle + "': "
                            + Optional.ofNullable(exception.getMessage()).orElse("respuesta no disponible") + ".");
                }
            }

            int itemPosition = 0;
            if (items != null) for (JsonNode item : items) {
                if (++traversedItems > 200) {
                    if (!limitReached) {
                        warnings.add("La importación se limitó a 200 elementos del currículo para proteger el servidor.");
                        limitReached = true;
                    }
                    break;
                }
                String type = Optional.ofNullable(firstText(item, "type", "item_type", "post_type"))
                        .orElse("lesson").toLowerCase(Locale.ROOT);
                String itemId = firstText(item, "id", "lesson_id", "quiz_id");
                JsonNode detail = item;
                if (itemId != null && needsLearnPressDetail(item, type)) {
                    String resource = type.contains("quiz") ? "quiz/" : "lessons/";
                    try {
                        detail = fetchLearnPressDetail(apiRoot.resolve(resource + itemId), bearerToken);
                    } catch (RuntimeException exception) {
                        warnings.add("No se pudo descargar el detalle de '"
                                + Optional.ofNullable(firstText(item, "title", "name")).orElse(itemId)
                                + "': " + Optional.ofNullable(exception.getMessage()).orElse("respuesta no disponible") + ".");
                    }
                }
                if (type.contains("quiz")) {
                    detail = enrichLearnPressQuiz(detail, apiRoot, bearerToken, warnings);
                    quizRows.add(parseQuiz(detail, itemPosition++, warnings));
                } else {
                    lessonRows.add(parseLesson(detail, itemPosition++));
                }
            }
            result.add(new AcademyDtos.SectionRequest(null, sectionTitle,
                    firstText(section, "description", "content"), sectionPosition++,
                    sectionId, lessonRows, quizRows));
            if (limitReached) break;
        }
        return result;
    }

    private JsonNode fetchLearnPressCollection(
            URI endpoint, String bearerToken, int pageSize, int maxPages) {
        com.fasterxml.jackson.databind.node.ArrayNode combined = mapper.createArrayNode();
        for (int page = 1; page <= maxPages; page++) {
            String separator = endpoint.toString().contains("?") ? "&" : "?";
            URI pageUri = URI.create(endpoint + separator + "page=" + page + "&per_page=" + pageSize + "&order=asc");
            JsonNode response = readJson(fetch(validateExternalUrl(pageUri.toString()), bearerToken));
            JsonNode rows = collectionNode(response);
            if (rows == null || !rows.isArray() || rows.isEmpty()) break;
            rows.forEach(combined::add);
            if (rows.size() < pageSize) break;
        }
        return combined;
    }

    private JsonNode collectionNode(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) return node;
        for (String key : List.of("data", "items", "results", "sections", "lessons", "quizzes", "questions")) {
            JsonNode candidate = node.get(key);
            if (candidate != null && candidate.isArray()) return candidate;
        }
        return null;
    }

    private JsonNode enrichLearnPressQuiz(
            JsonNode quiz, URI apiRoot, String bearerToken, List<String> warnings) {
        if (quiz == null || quiz.isNull()) return quiz;
        JsonNode questions = firstArray(quiz, "questions", "question_items", "items");
        if (questions != null && !questions.isEmpty()) return quiz;
        JsonNode ids = firstArray(quiz, "question_ids", "results.question_ids");
        if (ids == null || ids.isEmpty()) return quiz;

        com.fasterxml.jackson.databind.node.ArrayNode resolved = mapper.createArrayNode();
        int limit = 0;
        for (JsonNode idNode : ids) {
            if (++limit > 200) {
                warnings.add("Una evaluación superó el límite de 200 preguntas y fue truncada.");
                break;
            }
            String questionId = idNode.asText();
            if (questionId.isBlank()) continue;
            try {
                resolved.add(fetchLearnPressDetail(apiRoot.resolve("questions/" + questionId), bearerToken));
            } catch (RuntimeException exception) {
                warnings.add("No se pudo descargar la pregunta " + questionId + ": "
                        + Optional.ofNullable(exception.getMessage()).orElse("respuesta no disponible") + ".");
            }
        }
        if (resolved.isEmpty() || !quiz.isObject()) return quiz;
        com.fasterxml.jackson.databind.node.ObjectNode copy = ((com.fasterxml.jackson.databind.node.ObjectNode) quiz).deepCopy();
        copy.set("questions", resolved);
        return copy;
    }

    private String fetchLearnPressCourse(URI endpoint, String bearerToken) {
        if (blank(bearerToken)) return fetch(validateExternalUrl(endpoint.toString()), null);
        URI editEndpoint = URI.create(endpoint + "?context=edit");
        try {
            return fetch(validateExternalUrl(editEndpoint.toString()), bearerToken);
        } catch (RuntimeException editFailure) {
            return fetch(validateExternalUrl(endpoint.toString()), bearerToken);
        }
    }

    private JsonNode fetchLearnPressDetail(URI endpoint, String bearerToken) {
        URI selected = endpoint;
        if (!blank(bearerToken)) selected = URI.create(endpoint + "?context=edit");
        try {
            return unwrap(mapper.readTree(fetch(validateExternalUrl(selected.toString()), bearerToken)));
        } catch (RuntimeException exception) {
            if (blank(bearerToken)) throw exception;
            return unwrap(readJson(fetch(validateExternalUrl(endpoint.toString()), bearerToken)));
        } catch (Exception exception) {
            throw new BadRequestException("La API de LearnPress devolvió JSON inválido");
        }
    }

    private JsonNode readJson(String value) {
        try { return mapper.readTree(value); }
        catch (Exception exception) { throw new BadRequestException("La API de LearnPress devolvió JSON inválido"); }
    }

    private JsonNode unwrap(JsonNode node) {
        if (node != null && node.has("data") && node.get("data").isObject()) return node.get("data");
        return node;
    }

    private boolean needsLearnPressDetail(JsonNode item, String type) {
        if (type.contains("quiz")) return firstArray(item, "questions", "question_items") == null;
        return firstText(item, "content.rendered", "content") == null;
    }

    private boolean looksLikeLearnPress(JsonNode root, URI uri) {
        if (Optional.ofNullable(uri.getPath()).orElse("").contains("/learnpress/v1/")) return true;
        JsonNode sectionsNode = firstArray(root, "sections", "course_data.sections", "curriculum.sections", "course.sections");
        if (sectionsNode == null) return false;
        for (JsonNode section : sectionsNode) {
            JsonNode items = firstArray(section, "items", "section_items");
            if (items == null) continue;
            for (JsonNode item : items) {
                String type = Optional.ofNullable(firstText(item, "type", "item_type", "post_type")).orElse("");
                if (type.startsWith("lp_") || type.contains("lp_lesson") || type.contains("lp_quiz")) return true;
            }
        }
        return false;
    }

    private LearnPressSource detectLearnPressSource(Document document, String html, URI pageUri) {
        String lower = html.toLowerCase(Locale.ROOT);
        boolean signal = lower.contains("learnpress") || lower.contains("learn-press")
                || lower.contains("lp_course") || lower.contains("lp-course");
        String courseId = firstMatch(html,
                "(?i)\\bpostid-(\\d+)\\b",
                "(?i)id=[\\\"']post-(\\d+)[\\\"']",
                "(?i)\\blp-course-(\\d+)\\b",
                "(?i)/(?:wp-json/(?:wp/v2/lp_course|learnpress/v1/courses)|courses)/(\\d+)",
                "(?i)[\\\"']course_id[\\\"']\\s*[:=]\\s*[\\\"']?(\\d+)");
        Element apiLink = document.selectFirst("link[rel=https://api.w.org/][href]");
        URI apiRoot = null;
        if (apiLink != null && !apiLink.attr("href").isBlank()) {
            URI wpRoot = validateExternalUrl(apiLink.attr("href"));
            if (sameSite(pageUri, wpRoot)) apiRoot = ensureTrailingSlash(wpRoot).resolve("learnpress/v1/");
        }
        if (apiRoot == null && signal) {
            try {
                apiRoot = new URI(pageUri.getScheme(), null, pageUri.getHost(), pageUri.getPort(),
                        "/wp-json/learnpress/v1/", null, null);
            } catch (Exception ignored) { }
        }
        if (courseId == null || apiRoot == null || !signal) return null;
        return new LearnPressSource(validateExternalUrl(apiRoot.toString()), courseId);
    }

    private URI learnPressApiRoot(URI uri) {
        String path = Optional.ofNullable(uri.getPath()).orElse("");
        int marker = path.indexOf("/learnpress/v1/");
        if (marker < 0) return null;
        try {
            URI root = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    path.substring(0, marker) + "/learnpress/v1/", null, null);
            return validateExternalUrl(root.toString());
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean sameSite(URI left, URI right) {
        return normalizedHost(left).equals(normalizedHost(right));
    }

    private String normalizedHost(URI uri) {
        String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private URI ensureTrailingSlash(URI uri) {
        return uri.toString().endsWith("/") ? uri : URI.create(uri + "/");
    }

    private String firstMatch(String value, String... expressions) {
        for (String expression : expressions) {
            Matcher matcher = Pattern.compile(expression).matcher(value);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    private List<AcademyDtos.SectionRequest> parseSections(JsonNode root, List<String> warnings) {
        JsonNode sectionsNode = firstArray(root, "sections", "course_data.sections", "curriculum.sections", "course.sections");
        if (sectionsNode == null) return List.of();
        List<AcademyDtos.SectionRequest> sections = new ArrayList<>(); int sectionPosition=0;
        for (JsonNode section : sectionsNode) {
            String sectionTitle = Optional.ofNullable(firstText(section, "title", "name", "section_name"))
                    .orElse("Sección " + (sectionPosition + 1));
            List<AcademyDtos.LessonRequest> lessonRows = new ArrayList<>();
            List<AcademyDtos.QuizRequest> quizRows = new ArrayList<>();
            JsonNode items = firstArray(section, "items", "section_items", "lessons");
            int itemPosition=0;
            if (items != null) for (JsonNode item : items) {
                String type = Optional.ofNullable(firstText(item, "type", "item_type", "post_type"))
                        .orElse("lesson").toLowerCase(Locale.ROOT);
                if (type.contains("quiz")) quizRows.add(parseQuiz(item, itemPosition++, warnings));
                else lessonRows.add(parseLesson(item, itemPosition++));
            }
            JsonNode quizArray = firstArray(section, "quizzes");
            if (quizArray != null) for (JsonNode quiz : quizArray) quizRows.add(parseQuiz(quiz, itemPosition++, warnings));
            sections.add(new AcademyDtos.SectionRequest(null, sectionTitle,
                    firstText(section, "description", "content"), sectionPosition++, firstText(section, "id", "section_id"), lessonRows, quizRows));
        }
        return sections;
    }

    private AcademyDtos.LessonRequest parseLesson(JsonNode item, int position) {
        String title = Optional.ofNullable(firstText(item, "title.rendered", "title", "name"))
                .orElse("Lección " + (position + 1));
        String content = firstText(item, "content.rendered", "content", "description");
        List<String> videos = youtubeVideos(item.toString());
        String video = firstText(item, "video_url", "video", "youtube_url", "video_intro");
        if (video == null && !videos.isEmpty()) video = videos.getFirst();
        AcademyVideoProvider provider = video == null ? null : video.contains("youtu") ? AcademyVideoProvider.YOUTUBE : AcademyVideoProvider.EXTERNAL;
        return new AcademyDtos.LessonRequest(null, title, content, position,
                video == null ? AcademyLessonType.TEXT : AcademyLessonType.VIDEO,
                provider, video, firstText(item, "media_id"), durationSeconds(item, "duration_seconds", "duration"),
                booleanValue(item, "preview", "can_view"), firstText(item, "id", "lesson_id"));
    }

    private AcademyDtos.QuizRequest parseQuiz(JsonNode node, int position, List<String> warnings) {
        String title = Optional.ofNullable(firstText(node, "title.rendered", "title", "name"))
                .orElse("Evaluación " + (position + 1));
        JsonNode questionsNode = firstArray(node, "questions", "question_items", "items");
        List<AcademyDtos.QuestionRequest> questionRows = new ArrayList<>(); int questionPosition=0;
        if (questionsNode != null) for (JsonNode question : questionsNode) questionRows.add(parseQuestion(question, questionPosition++));
        if (questionRows.isEmpty()) {
            warnings.add("La evaluación '" + title + "' no expuso preguntas en la respuesta consultada.");
        } else {
            questionRows.stream()
                    .filter(question -> List.of(AcademyQuestionType.SINGLE_CHOICE,
                            AcademyQuestionType.MULTIPLE_CHOICE, AcademyQuestionType.TRUE_FALSE,
                            AcademyQuestionType.SHORT_TEXT).contains(question.type()))
                    .filter(question -> list(question.options()).stream().noneMatch(AcademyDtos.OptionRequest::correct))
                    .forEach(question -> warnings.add("La pregunta '" + question.text()
                            + "' fue importada sin clave de respuesta; revísela antes de publicar."));
        }
        return new AcademyDtos.QuizRequest(null, title, firstText(node, "description", "content"), position,
                decimal(node, BigDecimal.valueOf(70), "passing_score", "passing_grade", "results.passing_grade", "meta_data._lp_passing_grade"),
                integer(node, 3, "max_attempts", "retake_count", "results.retake_count", "meta_data._lp_retake_count"),
                durationMinutes(node, "duration_minutes", "duration", "results.duration", "meta_data._lp_duration"),
                booleanValue(node, "randomize", "random_questions", "meta_data._lp_random_questions"),
                booleanValue(node, "preview"), firstText(node, "id", "quiz_id"), questionRows);
    }

    private AcademyDtos.QuestionRequest parseQuestion(JsonNode node, int position) {
        String text = Optional.ofNullable(firstText(node, "title", "question", "text", "content"))
                .orElse("Pregunta " + (position + 1));
        String rawType = Optional.ofNullable(firstText(node, "type", "question_type", "meta_data._lp_type"))
                .orElse("single_choice").toUpperCase(Locale.ROOT);
        AcademyQuestionType type = rawType.contains("MULTI") ? AcademyQuestionType.MULTIPLE_CHOICE
                : rawType.contains("TRUE") ? AcademyQuestionType.TRUE_FALSE
                : rawType.contains("LONG") || rawType.contains("ESSAY") ? AcademyQuestionType.LONG_TEXT
                : rawType.contains("TEXT") || rawType.contains("FILL") ? AcademyQuestionType.SHORT_TEXT
                : AcademyQuestionType.SINGLE_CHOICE;
        JsonNode optionsNode = firstArray(node, "options", "answers", "answer_options", "meta_data._lp_answers");
        List<AcademyDtos.OptionRequest> optionRows = new ArrayList<>(); int optionPosition=0;
        if (optionsNode != null) for (JsonNode option : optionsNode) {
            optionRows.add(new AcademyDtos.OptionRequest(null,
                    Optional.ofNullable(firstText(option, "title", "text", "value", "answer")).orElse("Opción " + (optionPosition + 1)),
                    optionPosition++, booleanValue(option, "is_true", "correct", "is_correct")));
        }
        return new AcademyDtos.QuestionRequest(null, text, type, position,
                decimal(node, BigDecimal.ONE, "points", "point", "mark", "meta_data._lp_mark"),
                firstText(node, "explanation", "hint", "meta_data._lp_explanation", "meta_data._lp_hint"),
                !node.has("required") || node.path("required").asBoolean(true), optionRows);
    }

    URI validateExternalUrl(String raw) {
        try {
            URI uri = URI.create(raw.trim());
            if (!List.of("http", "https").contains(Optional.ofNullable(uri.getScheme()).orElse("").toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("La fuente debe usar HTTP o HTTPS");
            }
            if (uri.getUserInfo() != null) throw new BadRequestException("La URL no puede contener credenciales");
            String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            if (host.isBlank() || host.equals("localhost") || host.endsWith(".local") || host.equals("metadata.google.internal")) {
                throw new BadRequestException("El host de importación no está permitido");
            }
            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) throw new BadRequestException("Solo se permiten los puertos 80 y 443");
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new BadRequestException("La fuente apunta a una red privada o reservada");
                }
            }
            return uri.normalize();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("La URL de importación no es válida");
        }
    }

    private List<String> youtubeVideos(String input) {
        if (input == null || input.isBlank()) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher matcher = YOUTUBE.matcher(input.replace("\\/", "/"));
        while (matcher.find()) result.add("https://www.youtube.com/watch?v=" + matcher.group(1));
        return List.copyOf(result);
    }

    private Element first(Document document, String... selectors) {
        for (String selector : selectors) { Element value = document.selectFirst(selector); if (value != null) return value; }
        return null;
    }
    private String nearestHeading(Element element) {
        if (element == null) return null;
        Element current = element;
        for (int depth=0; depth<5 && current!=null; depth++,current=current.parent()) {
            Element previous = current.previousElementSibling();
            while(previous!=null){ if(previous.is("h1,h2,h3,h4,h5,h6")) return text(previous); previous=previous.previousElementSibling(); }
        }
        return null;
    }
    private String text(Element element){return element==null||element.text().isBlank()?null:element.text().trim();}
    private JsonNode firstArray(JsonNode node,String...paths){for(String path:paths){JsonNode value=at(node,path);if(value!=null&&value.isArray())return value;}return null;}
    private String firstText(JsonNode node,String...paths){for(String path:paths){JsonNode value=at(node,path);if(value!=null&&!value.isNull()){if(value.isValueNode()){String text=value.asText();if(!text.isBlank())return text;}else if(value.has("rendered")){String text=value.path("rendered").asText();if(!text.isBlank())return text;}}}return null;}
    private JsonNode at(JsonNode node,String path){JsonNode current=node;for(String part:path.split("\\.")){if(current==null||!current.has(part))return null;current=current.get(part);}return current;}
    private boolean booleanValue(JsonNode node,String...keys){for(String key:keys){JsonNode value=at(node,key);if(value!=null&&!value.isNull())return value.isBoolean()?value.asBoolean():List.of("1","true","yes","on").contains(value.asText().toLowerCase(Locale.ROOT));}return false;}
    private int integer(JsonNode node,int fallback,String...keys){String value=firstText(node,keys);if(value==null)return fallback;try{return Math.max(0,Integer.parseInt(value.replaceAll("[^0-9-]","")));}catch(Exception ignored){return fallback;}}
    private int durationSeconds(JsonNode node,String...keys){
        for(String key:keys){String value=firstText(node,key);if(value==null)continue;int parsed=parseDurationSeconds(value,key);if(parsed>0)return parsed;}return 0;
    }
    private int durationMinutes(JsonNode node,String...keys){int seconds=durationSeconds(node,keys);return seconds==0?0:(int)Math.ceil(seconds/60.0);}
    private int parseDurationSeconds(String raw,String key){
        String value=raw.trim().toLowerCase(Locale.ROOT);if(value.isBlank()||value.contains("lifetime"))return 0;
        try{if(value.toUpperCase(Locale.ROOT).startsWith("P"))return Math.toIntExact(java.time.Duration.parse(value.toUpperCase(Locale.ROOT)).toSeconds());}catch(Exception ignored){}
        if(value.matches("-?\\d+(?:\\.\\d+)?")){try{double number=Double.parseDouble(value);return (int)Math.max(0,Math.round(number*(key.contains("minute")?60:1)));}catch(Exception ignored){return 0;}}
        double seconds=0;Matcher matcher=Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(days?|d|hours?|hrs?|h|minutes?|mins?|min|m|seconds?|secs?|sec|s)").matcher(value);
        while(matcher.find()){double amount=Double.parseDouble(matcher.group(1));String unit=matcher.group(2);if(unit.startsWith("d"))seconds+=amount*86400;else if(unit.startsWith("h"))seconds+=amount*3600;else if(unit.startsWith("m"))seconds+=amount*60;else seconds+=amount;}
        if(seconds>0)return (int)Math.min(Integer.MAX_VALUE,Math.round(seconds));
        try{return Math.max(0,Integer.parseInt(value.replaceAll("[^0-9]",""))*(key.contains("minute")?60:1));}catch(Exception ignored){return 0;}
    }
    private BigDecimal decimal(JsonNode node,BigDecimal fallback,String...keys){String value=firstText(node,keys);if(value==null)return fallback;try{return new BigDecimal(value.replaceAll("[^0-9.-]",""));}catch(Exception ignored){return fallback;}}
    private BigDecimal price(JsonNode node){return decimal(node,BigDecimal.ZERO,"price","regular_price","course_price").max(BigDecimal.ZERO);}
    private String currency(JsonNode node){String value=firstText(node,"currency","price_currency");return value!=null&&value.matches("(?i)[a-z]{3}")?value.toUpperCase(Locale.ROOT):"USD";}
    private boolean blank(String value){return value==null||value.isBlank();}
    private <T> List<T> list(List<T> value){return value==null?List.of():value;}
    private record LearnPressSource(URI apiRoot,String courseId){}
    private record Normalized(String sourceType, AcademyDtos.CourseRequest course,List<String> youtubeVideos,List<String> warnings){}
}
