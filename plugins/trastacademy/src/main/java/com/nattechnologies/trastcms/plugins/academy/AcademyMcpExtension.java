package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.mcp.McpExtension;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP de administración y observabilidad de TrastAcademy.
 *
 * Las herramientas son deliberadamente de solo lectura. Las operaciones que
 * cambian cursos, matrículas o evaluaciones continúan pasando por la API
 * administrativa y sus controles de autorización.
 */
@Component
public class AcademyMcpExtension implements McpExtension {
    private final AcademyService service;
    private final AcademyImportService importer;
    private final AcademyPluginGuard guard;

    AcademyMcpExtension(AcademyService service, AcademyImportService importer, AcademyPluginGuard guard) {
        this.service = service;
        this.importer = importer;
        this.guard = guard;
    }

    @Override public int order() { return 40; }
    @Override public boolean enabled() { return guard.enabled(); }

    @Override
    public List<McpToolDefinition> tools() {
        return List.of(
                tool("academy_dashboard", "Resumen de la academia",
                        "Obtiene indicadores de cursos, matrículas, finalizaciones, demostraciones e importaciones.",
                        schema(Map.of(), List.of())),
                tool("academy_diagnostics", "Diagnosticar academia",
                        "Audita cursos vacíos, módulos sin contenido, lecciones sin material y evaluaciones sin claves válidas.",
                        schema(Map.of(), List.of())),
                tool("academy_course_list", "Listar cursos",
                        "Lista cursos administrables de TrastAcademy con búsqueda y paginación.",
                        schema(Map.of(
                                "search", str("Texto opcional para título, slug, instructor o categoría"),
                                "page", integer(0, 10000, "Página iniciando en cero"),
                                "size", integer(1, 100, "Cantidad de resultados")), List.of())),
                tool("academy_course_get", "Obtener curso",
                        "Obtiene un curso y su currículo completo por identificador.",
                        schema(Map.of("courseId", str("Identificador del curso")), List.of("courseId"))),
                tool("academy_enrollment_list", "Listar inscripciones",
                        "Lista matrículas y progreso; permite filtrar por correo, curso o estado.",
                        schema(Map.of(
                                "email", str("Correo total o parcial del estudiante"),
                                "courseId", str("Identificador del curso"),
                                "status", str("Estado: ACTIVE, COMPLETED o CANCELLED")), List.of())),
                tool("academy_student_progress", "Consultar progreso del estudiante",
                        "Devuelve todas las matrículas y el progreso asociado a un correo.",
                        schema(Map.of("email", str("Correo del estudiante")), List.of("email"))),
                tool("academy_pending_reviews", "Listar revisiones pendientes",
                        "Lista intentos con respuestas abiertas pendientes de calificación manual.",
                        schema(Map.of("courseId", str("Filtrar por curso"), "email", str("Filtrar por correo")), List.of())),
                tool("academy_import_preview", "Previsualizar importación",
                        "Analiza una URL externa sin guardar el curso y devuelve contenido detectado y advertencias.",
                        schema(Map.of(
                                "url", str("URL pública o endpoint REST"),
                                "bearerToken", secret("Bearer token opcional de la fuente")), List.of("url"))),
                tool("academy_import_history", "Historial de importaciones",
                        "Obtiene el historial de importaciones y sus advertencias o errores.",
                        schema(Map.of("limit", integer(1, 100, "Cantidad máxima de registros")), List.of())),
                tool("academy_settings_get", "Configuración de academia",
                        "Obtiene título del catálogo, moneda, URL de importación y política de matrículas gratuitas.",
                        schema(Map.of(), List.of()))
        );
    }

    @Override
    public Object call(String name, Map<String, Object> args) {
        Map<String, Object> safeArgs = args == null ? Map.of() : args;
        return switch (name) {
            case "academy_dashboard" -> service.dashboard();
            case "academy_diagnostics" -> service.diagnostics();
            case "academy_course_list" -> service.courses(
                    text(safeArgs.get("search")),
                    number(safeArgs.get("page"), 0, 0, 10000),
                    number(safeArgs.get("size"), 20, 1, 100),
                    false);
            case "academy_course_get" -> service.adminCourse(required(safeArgs, "courseId"));
            case "academy_enrollment_list" -> filterEnrollments(service.adminEnrollments(), safeArgs);
            case "academy_student_progress" -> filterEnrollments(
                    service.adminEnrollments(), Map.of("email", required(safeArgs, "email")));
            case "academy_pending_reviews" -> filterReviews(service.pendingReviews(), safeArgs);
            case "academy_import_preview" -> importer.preview(new AcademyDtos.ImportRequest(
                    required(safeArgs, "url"), text(safeArgs.get("bearerToken")), false, false));
            case "academy_import_history" -> importer.history().stream()
                    .limit(number(safeArgs.get("limit"), 25, 1, 100))
                    .toList();
            case "academy_settings_get" -> service.settingsResponse();
            default -> throw new IllegalArgumentException("Herramienta MCP desconocida: " + name);
        };
    }

    private List<AcademyDtos.EnrollmentResponse> filterEnrollments(
            List<AcademyDtos.EnrollmentResponse> values, Map<String, Object> args) {
        String email = lower(text(args.get("email")));
        String courseId = text(args.get("courseId"));
        String status = upper(text(args.get("status")));
        return values.stream()
                .filter(value -> email == null || lower(value.email()).contains(email))
                .filter(value -> courseId == null || courseId.equals(value.courseId()))
                .filter(value -> status == null || status.equals(value.status().name()))
                .toList();
    }

    private List<AcademyDtos.PendingReview> filterReviews(
            List<AcademyDtos.PendingReview> values, Map<String, Object> args) {
        String email = lower(text(args.get("email")));
        String courseId = text(args.get("courseId"));
        return values.stream()
                .filter(value -> email == null || lower(value.enrollmentEmail()).contains(email))
                .filter(value -> courseId == null || courseId.equals(value.courseId()))
                .toList();
    }

    private McpToolDefinition tool(String name, String title, String description, Map<String, Object> schema) {
        return new McpToolDefinition(name, title, description, schema, true, false, true);
    }

    private Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false);
    }

    private Map<String, Object> str(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> secret(String description) {
        return Map.of("type", "string", "description", description, "writeOnly", true);
    }

    private Map<String, Object> integer(int minimum, int maximum, String description) {
        return Map.of(
                "type", "integer",
                "minimum", minimum,
                "maximum", maximum,
                "description", description);
    }

    private String required(Map<String, Object> args, String key) {
        String value = text(args.get(key));
        if (value == null) throw new IllegalArgumentException(key + " es obligatorio");
        return value;
    }

    private String text(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String lower(String value) { return value == null ? null : value.toLowerCase(Locale.ROOT); }
    private String upper(String value) { return value == null ? null : value.toUpperCase(Locale.ROOT); }

    private int number(Object value, int fallback, int minimum, int maximum) {
        try {
            int parsed = value == null ? fallback : Integer.parseInt(String.valueOf(value));
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
