package com.nattechnologies.trastcms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:academy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "trastcms.data-dir=target/test-data-academy",
        "trastcms.admin.password=Testing-Password-2026!"
})
@AutoConfigureMockMvc
class AcademyIntegrationTests {

    @BeforeEach
    void academyHost() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/")
                        .header("X-Forwarded-Host", "academy.localhost")
                        .header("X-Forwarded-Port", "8080"))
                .build();
    }


    MockMvc mvc;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void academyPluginIsRegisteredAndEnabled() throws Exception {
        mvc.perform(get("/api/admin/plugins/bundled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].pluginId", hasItem("trastacademy")))
                .andExpect(jsonPath("$[?(@.pluginId == 'trastacademy')].enabled").value(hasItem(true)));
    }

    @Test
    void completeAcademyFlowWorksThroughBearerApi() throws Exception {
        JsonNode course = createCourse();
        String courseId = course.path("id").asText();
        String lessonId = course.path("sections").get(0).path("lessons").get(0).path("id").asText();
        String quizId = course.path("sections").get(0).path("quizzes").get(0).path("id").asText();
        String questionId = course.path("sections").get(0).path("quizzes").get(0)
                .path("questions").get(0).path("id").asText();
        String correctOptionId = course.path("sections").get(0).path("quizzes").get(0)
                .path("questions").get(0).path("options").get(0).path("id").asText();

        mvc.perform(get("/api/public/academy/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug", hasItem("curso-api")));

        JsonNode registration = json(mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"student@example.com",
                                  "displayName":"Estudiante",
                                  "password":"Academy-Test-2026!",
                                  "phone":"+504 9999-9999",
                                  "deviceName":"Prueba de integración"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn());
        String token = registration.path("accessToken").asText();

        mvc.perform(post("/api/mobile/academy/courses/{courseId}/enroll", courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.progressPercentage").value(0));

        mvc.perform(post("/api/mobile/academy/lessons/{lessonId}/complete", lessonId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchedSeconds\":120,\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLessons").value(1));

        JsonNode attempt = json(mvc.perform(post("/api/mobile/academy/quizzes/{quizId}/attempts", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn());

        mvc.perform(post("/api/mobile/academy/attempts/{attemptId}/finish", attempt.path("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[{
                                    "questionId":"%s",
                                    "selectedOptionIds":["%s"],
                                    "textAnswer":null
                                  }]
                                }
                                """.formatted(questionId, correctOptionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.score").value(100.00));

        mvc.perform(get("/api/mobile/academy/me/courses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].progressPercentage").value(100));
    }


    @Test
    void essayQuizWaitsForManualReviewAndDoesNotCreateParallelAttempts() throws Exception {
        JsonNode course = createEssayCourse();
        String courseId = course.path("id").asText();
        String quizId = course.path("sections").get(0).path("quizzes").get(0).path("id").asText();
        String questionId = course.path("sections").get(0).path("quizzes").get(0)
                .path("questions").get(0).path("id").asText();

        JsonNode registration = json(mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"essay.student@example.com",
                                  "displayName":"Estudiante de ensayo",
                                  "password":"Academy-Essay-2026!",
                                  "deviceName":"Prueba manual"
                                }
                                """))
                .andExpect(status().isCreated()).andReturn());
        String token = registration.path("accessToken").asText();

        mvc.perform(post("/api/mobile/academy/courses/{courseId}/enroll", courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode firstAttempt = json(mvc.perform(post("/api/mobile/academy/quizzes/{quizId}/attempts", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/mobile/academy/quizzes/{quizId}/attempts", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstAttempt.path("id").asText()));

        mvc.perform(post("/api/mobile/academy/attempts/{attemptId}/finish", firstAttempt.path("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[{
                                    "questionId":"%s",
                                    "selectedOptionIds":[],
                                    "textAnswer":"Una respuesta que requiere valoración humana."
                                  }]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.score").doesNotExist());

        mvc.perform(post("/api/mobile/academy/quizzes/{quizId}/attempts", quizId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/academy/attempts/{attemptId}/review", firstAttempt.path("id").asText())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin@trastcms.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grades":[{
                                    "questionId":"%s",
                                    "awardedPoints":5
                                  }]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.score").value(100.00));

        mvc.perform(get("/api/mobile/academy/me/courses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }


    @Test
    void importerRejectsPrivateNetworksBeforeConnecting() throws Exception {
        mvc.perform(post("/api/admin/academy/imports/preview")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin@trastcms.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url":"http://127.0.0.1/private-course",
                                  "publish":false,
                                  "overwriteExisting":false
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void learnPressCompatibilityAcceptsUsernameAndRegistrationShape() throws Exception {
        mvc.perform(post("/api/compat/learnpress/v1/token/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"legacy@example.com",
                                  "display_name":"Aplicación heredada",
                                  "password":"Legacy-Test-2026!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user_email").value("legacy@example.com"));

        mvc.perform(post("/api/compat/learnpress/v1/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"legacy@example.com",
                                  "password":"Legacy-Test-2026!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user_login").value("legacy@example.com"));
    }


    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void demoCoursesAndAcademyThemeAreAvailableAndIdempotent() throws Exception {
        mvc.perform(post("/api/admin/academy/demo-data").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeId").value("nexus"))
                .andExpect(jsonPath("$.catalogUrl").value("/academy"));

        mvc.perform(post("/api/admin/academy/demo-data").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.existing").value(4));

        mvc.perform(get("/api/public/academy/courses").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug", hasItem("trastcms-primer-sitio")))
                .andExpect(jsonPath("$.content[*].slug", hasItem("java-spring-boot-apis")))
                .andExpect(jsonPath("$.content[*].slug", hasItem("vue3-typescript-practico")))
                .andExpect(jsonPath("$.content[*].slug", hasItem("negocio-digital-sostenible")));

        JsonNode springCourse = json(mvc.perform(get("/api/public/academy/courses/java-spring-boot-apis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("FREE"))
                .andExpect(jsonPath("$.category").value("Desarrollo backend"))
                .andExpect(jsonPath("$.enrollment").exists())
                .andReturn());
        assertEquals(5, springCourse.path("sections").size());

        String springCourseId = springCourse.path("id").asText();
        JsonNode classroomCourse = json(mvc.perform(get("/api/account/academy/courses/{courseId}", springCourseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("API REST completa con Spring Boot e Hibernate"))
                .andExpect(jsonPath("$.sections.length()").value(5))
                .andExpect(jsonPath("$.sections[0].lessons.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn());
        assertEquals(15, classroomCourse.path("sections").findValues("lessons").stream()
                .mapToInt(JsonNode::size).sum());
        int lessonCount = 0;
        int quizCount = 0;
        int questionCount = 0;
        int playlistBackedLessons = 0;
        for (JsonNode section : springCourse.path("sections")) {
            lessonCount += section.path("lessons").size();
            quizCount += section.path("quizzes").size();
            for (JsonNode lesson : section.path("lessons")) {
                assertEquals("YOUTUBE", lesson.path("videoProvider").asText());
                assertTrue(lesson.path("preview").asBoolean());
                String videoUrl = lesson.path("videoUrl").asText();
                assertTrue(videoUrl.startsWith("https://www.youtube.com/watch?v="));
                if (videoUrl.contains("PLcIHm18h1i4m1xuhwrL-LjVjf5wuYFRCV")) {
                    playlistBackedLessons++;
                }
                assertTrue(lesson.path("content").asText().contains("Objetivos de aprendizaje"));
                assertTrue(lesson.path("content").asText().contains("Práctica de la lección"));
            }
            for (JsonNode quiz : section.path("quizzes")) {
                questionCount += quiz.path("questions").size();
            }
        }
        assertEquals(15, lessonCount);
        assertEquals(5, quizCount);
        assertEquals(38, questionCount);
        assertEquals(8, playlistBackedLessons);

        mvc.perform(get("/api/admin/academy/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoCourses").value(4));

        mvc.perform(get("/api/admin/themes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("nexus")));

        mvc.perform(get("/api/public/themes/nexus/tokens.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-theme=\"nexus\"")));

        mvc.perform(delete("/api/admin/academy/demo-data").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(4));

        mvc.perform(get("/api/admin/academy/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoCourses").value(0));
    }

    @Test
    @WithMockUser(username = "admin@trastcms.local", roles = "ADMIN")
    void classroomCanStartQuizWithSessionCsrfAndDiagnosticsAreAvailable() throws Exception {
        mvc.perform(post("/api/admin/academy/demo-data").with(csrf()))
                .andExpect(status().isOk());

        JsonNode course = json(mvc.perform(get("/api/public/academy/courses/java-spring-boot-apis"))
                .andExpect(status().isOk())
                .andReturn());
        String lessonId = course.path("sections").get(0).path("lessons").get(0).path("id").asText();
        String quizId = course.path("sections").get(0).path("quizzes").get(0).path("id").asText();
        assertTrue(!lessonId.isBlank());
        assertTrue(!quizId.isBlank());

        mvc.perform(get("/api/account/academy/lessons/{lessonId}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").isNotEmpty());

        mvc.perform(get("/api/account/academy/quizzes/{quizId}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quizId))
                .andExpect(jsonPath("$.questions.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        JsonNode first = json(mvc.perform(post("/api/account/academy/quizzes/{quizId}/attempts", quizId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn());

        mvc.perform(post("/api/account/academy/quizzes/{quizId}/attempts", quizId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(first.path("id").asText()));

        mvc.perform(get("/api/admin/academy/diagnostics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.totals.courses").isNumber())
                .andExpect(jsonPath("$.totals.lessons").isNumber())
                .andExpect(jsonPath("$.issues").isArray());
    }

    private JsonNode createEssayCourse() throws Exception {
        MvcResult result = mvc.perform(post("/api/admin/academy/courses")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin@trastcms.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Curso con ensayo",
                                  "slug":"curso-con-ensayo",
                                  "description":"<p>Evaluación manual.</p>",
                                  "accessType":"FREE",
                                  "status":"PUBLISHED",
                                  "currency":"USD",
                                  "sections":[{
                                    "title":"Evaluación",
                                    "position":0,
                                    "lessons":[],
                                    "quizzes":[{
                                      "title":"Ensayo final",
                                      "position":0,
                                      "passingScore":70,
                                      "maxAttempts":2,
                                      "questions":[{
                                        "text":"Explique el concepto principal.",
                                        "type":"LONG_TEXT",
                                        "position":0,
                                        "points":5,
                                        "required":true,
                                        "options":[]
                                      }]
                                    }]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result);
    }


    private JsonNode createCourse() throws Exception {
        MvcResult result = mvc.perform(post("/api/admin/academy/courses")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin@trastcms.local").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Curso API",
                                  "slug":"curso-api",
                                  "excerpt":"Curso para validar el flujo completo",
                                  "description":"<p>Contenido del curso.</p>",
                                  "instructorName":"Academy",
                                  "level":"BEGINNER",
                                  "accessType":"FREE",
                                  "status":"PUBLISHED",
                                  "price":0,
                                  "currency":"USD",
                                  "estimatedMinutes":30,
                                  "featured":true,
                                  "sections":[{
                                    "title":"Introducción",
                                    "position":0,
                                    "lessons":[{
                                      "title":"Bienvenida",
                                      "content":"<p>Primera clase.</p>",
                                      "position":0,
                                      "lessonType":"VIDEO",
                                      "videoProvider":"YOUTUBE",
                                      "videoUrl":"https://youtu.be/dQw4w9WgXcQ",
                                      "durationSeconds":120,
                                      "preview":true
                                    }],
                                    "quizzes":[{
                                      "title":"Evaluación inicial",
                                      "position":1,
                                      "passingScore":70,
                                      "maxAttempts":3,
                                      "questions":[{
                                        "text":"¿Cuál es la respuesta correcta?",
                                        "type":"SINGLE_CHOICE",
                                        "position":0,
                                        "points":1,
                                        "required":true,
                                        "options":[
                                          {"text":"Correcta","position":0,"correct":true},
                                          {"text":"Incorrecta","position":1,"correct":false}
                                        ]
                                      }]
                                    }]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("curso-api"))
                .andReturn();
        return json(result);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
