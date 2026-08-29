package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.service.NotFoundException;
import com.nattechnologies.trastcms.service.UserAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
class AcademyDemoDataService {
    static final String THEME_ID = "nexus";
    private static final String ASSET_ROOT = "/api/public/themes/" + THEME_ID + "/assets/";
    private static final String DEMO_SOURCE = "trastcms://demo/academy/";
    private static final String YOUTUBE_DEMO = "https://www.youtube.com/watch?v=M7lc1UVf-VE";
    private static final String SPRING_PLAYLIST_ID = "PLcIHm18h1i4m1xuhwrL-LjVjf5wuYFRCV";
    private static final String SPRING_PLAYLIST_FIRST_VIDEO = "UpdkMjunXSk";

    private final AcademyService academy;
    private final AcademyCourseRepository courses;
    private final AcademyEnrollmentRepository enrollments;
    private final AcademyLessonProgressRepository lessonProgress;
    private final UserAccountService users;
    private final AcademyPluginGuard guard;

    AcademyDemoDataService(
            AcademyService academy,
            AcademyCourseRepository courses,
            AcademyEnrollmentRepository enrollments,
            AcademyLessonProgressRepository lessonProgress,
            UserAccountService users,
            AcademyPluginGuard guard) {
        this.academy = academy;
        this.courses = courses;
        this.enrollments = enrollments;
        this.lessonProgress = lessonProgress;
        this.users = users;
        this.guard = guard;
    }

    @Transactional
    public synchronized AcademyDtos.DemoDataResponse generate(String actorEmail) {
        guard.requireEnabled();
        int created = 0;
        int existing = 0;
        List<AcademyCourse> demoCourses = new ArrayList<>();

        for (AcademyDtos.CourseRequest request : definitions()) {
            AcademyCourse current = courses.findBySlug(request.slug()).orElse(null);
            if (current != null && !current.demoData) {
                existing++;
                continue;
            }

            if (current != null) {
                resetDemoEnrollmentState(current);
            }
            AcademyDtos.CourseDetail saved = academy.saveCourse(
                    current == null ? null : current.id,
                    request);
            AcademyCourse entity = courses.findById(saved.id())
                    .orElseThrow(() -> new NotFoundException("No se pudo recuperar el curso demo"));
            entity.demoData = true;
            demoCourses.add(courses.save(entity));
            if (current == null) created++; else existing++;
        }

        int enrolled = createProgressSample(actorEmail, demoCourses);
        String message;
        if (created == 0) {
            message = "Se actualizaron los cursos demostrativos. El curso de Spring Boot incluye ahora 15 clases de YouTube, descripciones, evaluaciones parciales y examen final.";
        } else {
            message = "Se preparó una academia demostrativa con cursos, videos, evaluaciones, precios y progreso de ejemplo.";
        }
        return new AcademyDtos.DemoDataResponse(
                created, existing, 0, enrolled,
                THEME_ID, "/academy", message);
    }

    private void resetDemoEnrollmentState(AcademyCourse course) {
        List<AcademyEnrollment> demoEnrollments = enrollments.findByCourseId(course.id);
        if (!demoEnrollments.isEmpty()) {
            enrollments.deleteAll(demoEnrollments);
            enrollments.flush();
        }
    }

    @Transactional
    public synchronized AcademyDtos.DemoDataResponse delete() {
        guard.requireEnabled();
        List<AcademyCourse> demoCourses = courses.findByDemoDataTrueOrderByCreatedAtAsc();
        int deleted = 0;
        for (AcademyCourse course : demoCourses) {
            List<AcademyEnrollment> courseEnrollments = enrollments.findByCourseId(course.id);
            if (!courseEnrollments.isEmpty()) {
                enrollments.deleteAll(courseEnrollments);
                enrollments.flush();
            }
            courses.delete(course);
            courses.flush();
            deleted++;
        }
        return new AcademyDtos.DemoDataResponse(
                0, 0, deleted, 0,
                THEME_ID, "/academy",
                deleted == 0
                        ? "No había datos demostrativos de TrastAcademy."
                        : "Se eliminaron únicamente los cursos y matrículas demostrativas de TrastAcademy.");
    }

    private int createProgressSample(String actorEmail, List<AcademyCourse> generated) {
        if (actorEmail == null || actorEmail.isBlank() || generated.isEmpty()) return 0;
        UserAccount actor;
        try {
            actor = users.requireByEmail(actorEmail.trim().toLowerCase(Locale.ROOT));
        } catch (NotFoundException exception) {
            return 0;
        }

        int createdEnrollments = 0;
        for (String slug : List.of("java-spring-boot-apis", "trastcms-primer-sitio")) {
            AcademyCourse course = generated.stream()
                    .filter(value -> slug.equals(value.slug))
                    .findFirst().orElse(null);
            if (course == null) continue;

            AcademyEnrollment existing = enrollments
                    .findByCourseIdAndEmailIgnoreCase(course.id, actor.getEmail())
                    .orElse(null);
            AcademyEnrollment enrollment = existing == null ? new AcademyEnrollment() : existing;
            enrollment.course = course;
            enrollment.userId = actor.getId();
            enrollment.email = actor.getEmail().toLowerCase(Locale.ROOT);
            enrollment.status = AcademyEnrollmentStatus.ACTIVE;
            enrollment.source = AcademyEnrollmentSource.ADMIN;
            enrollment.orderId = null;
            enrollment = enrollments.save(enrollment);
            if (existing == null) createdEnrollments++;

            List<AcademyLesson> lessons = course.sections.stream()
                    .flatMap(section -> section.lessons.stream())
                    .toList();
            int sampleCount = Math.min(2, lessons.size());
            for (int index = 0; index < sampleCount; index++) {
                AcademyLesson lesson = lessons.get(index);
                AcademyLessonProgress state = lessonProgress
                        .findByEnrollmentIdAndLessonId(enrollment.id, lesson.id)
                        .orElseGet(AcademyLessonProgress::new);
                state.enrollment = enrollment;
                state.lesson = lesson;
                state.completed = true;
                state.watchedSeconds = lesson.durationSeconds > 0
                        ? lesson.durationSeconds
                        : 300;
                state.completedAt = Instant.now();
                lessonProgress.save(state);
            }
        }
        return createdEnrollments;
    }

    private List<AcademyDtos.CourseRequest> definitions() {
        return List.of(
                trastCmsCourse(),
                javaCourse(),
                vueCourse(),
                businessCourse());
    }

    private AcademyDtos.CourseRequest trastCmsCourse() {
        return course(
                "Construya su primer sitio con TrastCMS",
                "trastcms-primer-sitio",
                "Aprenda a configurar el CMS, crear páginas por bloques y publicar un sitio profesional.",
                "<p>Un recorrido práctico por la instalación, el panel, los temas y el constructor visual de TrastCMS.</p>"
                        + "<p>El curso combina videos, contenido escrito, actividades y una evaluación final.</p>",
                "course-trastcms.svg",
                "Equipo NaT Lab",
                AcademyLevel.BEGINNER,
                AcademyAccessType.FREE,
                BigDecimal.ZERO,
                150,
                true,
                List.of(
                        section("fundamentos", "Fundamentos del CMS", "Conozca la plataforma y prepare su primer proyecto.",
                                List.of(
                                        video("bienvenida", "Bienvenida y recorrido general", 0,
                                                "<p>Video demostrativo incrustado desde YouTube. En un curso real puede reemplazarlo por su propia clase.</p>", true, 420),
                                        text("arquitectura", "Cómo está organizado TrastCMS", 1,
                                                "<h3>Núcleo, plugins y temas</h3><p>El núcleo administra contenido, usuarios y seguridad. Los plugins agregan capacidades, mientras los temas controlan la presentación pública.</p>"),
                                        mixed("panel", "Configure el panel y los datos del sitio", 2,
                                                "<p>Revise nombre, descripción, logotipo, correo de contacto y navegación antes de publicar.</p>", 360)),
                                List.of(quiz("evaluacion-fundamentos", "Evaluación de fundamentos", 3, 70, 3, 10, true,
                                        List.of(
                                                single("q1", "¿Qué componente debe controlar la lógica comercial?", 0,
                                                        "Un plugin", "El tema", "Una hoja CSS"),
                                                multiple("q2", "Seleccione capacidades del núcleo de TrastCMS.", 1,
                                                        List.of("Seguridad", "Gestión de contenido", "Usuarios"),
                                                        List.of("Diseño específico de un cliente")),
                                                trueFalse("q3", "Los temas deben acceder directamente a tablas comerciales.", 2, false),
                                                shortText("q4", "Escriba el nombre del constructor usado por TrastCMS.", 3,
                                                        List.of("constructor visual", "visual builder")))))),
                        section("publicacion", "Diseño y publicación", "Construya la portada y valide la experiencia pública.",
                                List.of(
                                        text("bloques", "Cree una página con bloques", 0,
                                                "<p>Combine encabezados, texto enriquecido, columnas y bloques aportados por plugins.</p>"),
                                        text("tema", "Active y personalice un tema", 1,
                                                "<p>Active Nexus, restaure su contenido inicial y ajuste colores, tipografía y ancho desde Apariencia.</p>"),
                                        text("lanzamiento", "Lista de verificación para publicar", 2,
                                                "<ul><li>Revise navegación y enlaces.</li><li>Pruebe dispositivos móviles.</li><li>Verifique formularios, compra e inicio de sesión.</li></ul>")),
                                List.of())));
    }

    private AcademyDtos.CourseRequest javaCourse() {
        return course(
                "API REST completa con Spring Boot e Hibernate",
                "java-spring-boot-apis",
                "Construya paso a paso una API REST con arquitectura por capas, CRUD, paginación, seguridad con tokens y clientes REST.",
                "<h2>Proyecto práctico del curso</h2>"
                        + "<p>En este curso desarrollará una API REST completa con Spring Boot e Hibernate tomando como referencia la serie pública de Byte Code. Cada clase contiene un video de YouTube, objetivos, conceptos clave y una práctica concreta.</p>"
                        + "<h3>Al finalizar podrá</h3>"
                        + "<ul><li>Configurar un proyecto Spring Boot y organizarlo por capas.</li>"
                        + "<li>Modelar entidades, repositorios, convertidores y servicios.</li>"
                        + "<li>Construir controladores CRUD con respuestas HTTP coherentes.</li>"
                        + "<li>Agregar registros, paginación y seguridad basada en tokens.</li>"
                        + "<li>Consumir la API desde otro servicio mediante un cliente REST.</li></ul>"
                        + "<h3>Contenido</h3>"
                        + "<p><strong>15 clases en video</strong>, aproximadamente <strong>2 horas y 29 minutos</strong> de reproducción, cinco módulos, cuatro evaluaciones parciales y un examen final.</p>"
                        + "<h3>Requisitos</h3>"
                        + "<p>Conocimientos básicos de Java, Maven, HTTP y bases de datos relacionales. Se recomienda tener Java, un IDE y MySQL o PostgreSQL disponibles para reproducir las prácticas.</p>",
                "course-java.svg",
                "Byte Code",
                AcademyLevel.INTERMEDIATE,
                AcademyAccessType.FREE,
                BigDecimal.ZERO,
                240,
                true,
                List.of(
                        section("spring-fundamentos", "Módulo 1 · Preparación y arquitectura",
                                "Configure el proyecto y construya las capas fundamentales del dominio.",
                                List.of(
                                        playlistLesson("spring-01", "1. Configuración e introducción del proyecto", 0, 1, 730,
                                                "Prepare el proyecto base y comprenda la meta técnica de la API que se construirá durante el curso.",
                                                List.of("Crear el proyecto con las dependencias necesarias", "Reconocer la estructura inicial de Spring Boot", "Definir el recurso principal de la API"),
                                                "Cree un proyecto nuevo, ejecútelo y confirme que la aplicación inicia sin errores.",
                                                List.of("Spring Initializr", "Maven", "Spring Web", "JPA", "configuración del proyecto")),
                                        playlistLesson("spring-02", "2. Entidades y modelo", 1, 2, 481,
                                                "Modele la información del sistema mediante entidades persistentes y relaciones comprensibles.",
                                                List.of("Crear una entidad JPA", "Definir identificadores y columnas", "Separar el modelo persistente del contrato REST"),
                                                "Implemente una entidad con identificador, campos obligatorios y restricciones coherentes.",
                                                List.of("@Entity", "@Id", "mapeo objeto-relacional", "validación", "modelo de dominio")),
                                        playlistLesson("spring-03", "3. Repositorio y convertidor", 2, 3, 633,
                                                "Implemente el acceso a datos y transforme entidades en objetos aptos para la API.",
                                                List.of("Crear un repositorio Spring Data", "Usar operaciones CRUD heredadas", "Convertir entidad y DTO sin acoplar las capas"),
                                                "Cree un repositorio y un convertidor bidireccional para el recurso del curso.",
                                                List.of("JpaRepository", "persistencia", "DTO", "converter", "separación de responsabilidades")),
                                        playlistLesson("spring-04", "4. Capa de servicio", 3, 4, 582,
                                                "Centralice las reglas de negocio en un servicio reutilizable y testeable.",
                                                List.of("Inyectar el repositorio", "Definir operaciones del caso de uso", "Manejar registros inexistentes"),
                                                "Implemente un servicio con operaciones para crear, actualizar, consultar y eliminar.",
                                                List.of("@Service", "inyección de dependencias", "transacciones", "reglas de negocio", "excepciones"))),
                                List.of(quiz("spring-parcial-1", "Evaluación 1 · Arquitectura y persistencia",
                                        "<p>Compruebe que domina la configuración inicial, las entidades, los repositorios, los convertidores y la capa de servicio.</p>",
                                        4, 70, 3, 12, true,
                                        List.of(
                                                single("spring-p1-q1", "¿Qué herramienta permite generar la estructura inicial de un proyecto Spring Boot?", 0,
                                                        "Spring Initializr", "Hibernate Console", "Flyway Desktop"),
                                                trueFalse("spring-p1-q2", "Una entidad JPA debe representar directamente cada respuesta pública de la API.", 1, false),
                                                single("spring-p1-q3", "¿Qué interfaz ofrece operaciones CRUD sobre una entidad JPA?", 2,
                                                        "JpaRepository", "HttpServlet", "RestTemplate"),
                                                multiple("spring-p1-q4", "¿Qué responsabilidades pertenecen normalmente a la capa de servicio?", 3,
                                                        List.of("Aplicar reglas de negocio", "Coordinar repositorios", "Gestionar errores del caso de uso"),
                                                        List.of("Definir colores del tema")),
                                                shortText("spring-p1-q5", "Escriba la sigla del objeto usado para transportar datos entre capas o hacia la API.", 4,
                                                        List.of("DTO", "dto")))))),
                        section("spring-crud", "Módulo 2 · Controladores y operaciones CRUD",
                                "Exponga las operaciones principales mediante endpoints REST bien definidos.",
                                List.of(
                                        playlistLesson("spring-05", "5. Controlador: crear", 0, 5, 435,
                                                "Construya el endpoint de creación y devuelva una respuesta HTTP consistente.",
                                                List.of("Recibir JSON en el controlador", "Validar el cuerpo de la petición", "Responder con el recurso creado"),
                                                "Implemente un POST que delegue en el servicio y responda con estado 201.",
                                                List.of("@RestController", "@PostMapping", "@RequestBody", "HTTP 201", "validación")),
                                        playlistLesson("spring-06", "6. Controlador: actualizar", 1, 6, 294,
                                                "Actualice un recurso existente conservando las reglas de negocio en el servicio.",
                                                List.of("Recibir el identificador por ruta", "Validar cambios", "Diferenciar actualización total y parcial"),
                                                "Implemente la actualización y pruebe tanto un identificador válido como uno inexistente.",
                                                List.of("@PutMapping", "@PathVariable", "HTTP 200", "HTTP 404", "actualización")),
                                        playlistLesson("spring-07", "7. Controlador: borrar", 2, 7, 198,
                                                "Elimine recursos mediante un endpoint predecible y seguro.",
                                                List.of("Diseñar una operación DELETE", "Comprobar la existencia del recurso", "Elegir la respuesta HTTP adecuada"),
                                                "Implemente DELETE y confirme que una segunda eliminación devuelve un error controlado.",
                                                List.of("@DeleteMapping", "HTTP 204", "idempotencia", "Not Found", "eliminación")),
                                        playlistLesson("spring-08", "8. Controlador: obtener", 3, 8, 297,
                                                "Exponga consultas individuales y colecciones sin filtrar detalles internos de persistencia.",
                                                List.of("Consultar por identificador", "Listar recursos", "Convertir entidades a respuestas REST"),
                                                "Agregue GET por identificador y GET de colección; pruebe ambos con datos reales.",
                                                List.of("@GetMapping", "ResponseEntity", "colecciones", "serialización JSON", "DTO"))),
                                List.of(quiz("spring-parcial-2", "Evaluación 2 · Controladores CRUD",
                                        "<p>Valide el uso correcto de métodos HTTP, códigos de estado, parámetros y responsabilidades del controlador.</p>",
                                        4, 75, 3, 12, true,
                                        List.of(
                                                single("spring-p2-q1", "¿Qué método HTTP se utiliza normalmente para crear un recurso?", 0,
                                                        "POST", "GET", "DELETE"),
                                                single("spring-p2-q2", "¿Qué estado HTTP representa correctamente una creación exitosa?", 1,
                                                        "201 Created", "204 No Content", "401 Unauthorized"),
                                                trueFalse("spring-p2-q3", "El controlador debe contener directamente todas las reglas de negocio.", 2, false),
                                                multiple("spring-p2-q4", "Seleccione componentes comunes de un endpoint de actualización.", 3,
                                                        List.of("Identificador de ruta", "DTO de entrada", "Llamada al servicio"),
                                                        List.of("Acceso directo al CSS del tema")),
                                                single("spring-p2-q5", "¿Qué método HTTP representa la eliminación de un recurso?", 4,
                                                        "DELETE", "PATCH", "HEAD"),
                                                single("spring-p2-q6", "¿Qué respuesta corresponde a un recurso que no existe?", 5,
                                                        "404 Not Found", "200 OK", "302 Found"))))),
                        section("spring-operacion", "Módulo 3 · Observabilidad y paginación",
                                "Mejore la operación de la API mediante registros útiles y respuestas paginadas.",
                                List.of(
                                        playlistLesson("spring-09", "9. Logs de información", 0, 9, 382,
                                                "Agregue registros que ayuden a comprender el comportamiento de la aplicación sin exponer información sensible.",
                                                List.of("Configurar un logger", "Elegir niveles apropiados", "Registrar contexto útil para diagnóstico"),
                                                "Agregue logs a una operación de creación y a un caso de recurso inexistente.",
                                                List.of("SLF4J", "INFO", "WARN", "ERROR", "trazabilidad")),
                                        playlistLesson("spring-10", "10. Paginación", 1, 10, 592,
                                                "Evite respuestas ilimitadas utilizando los contratos de paginación de Spring Data.",
                                                List.of("Recibir parámetros de página y tamaño", "Usar Pageable y Page", "Devolver metadatos de navegación"),
                                                "Convierta el listado general en un endpoint paginado y pruebe dos páginas diferentes.",
                                                List.of("Pageable", "Page", "size", "page", "ordenamiento"))),
                                List.of(quiz("spring-parcial-3", "Evaluación 3 · Logs y paginación",
                                        "<p>Compruebe que puede operar una API con registros útiles y colecciones limitadas.</p>",
                                        2, 75, 3, 10, true,
                                        List.of(
                                                single("spring-p3-q1", "¿Qué nivel de log es apropiado para un evento operativo esperado?", 0,
                                                        "INFO", "FATAL para todo", "TRACE obligatorio"),
                                                trueFalse("spring-p3-q2", "Es seguro registrar contraseñas y tokens completos para facilitar el soporte.", 1, false),
                                                single("spring-p3-q3", "¿Qué abstracción de Spring Data representa una solicitud paginada?", 2,
                                                        "Pageable", "EntityManagerFactory", "HttpSession"),
                                                multiple("spring-p3-q4", "¿Qué datos suelen acompañar una respuesta paginada?", 3,
                                                        List.of("Número de página", "Tamaño", "Total de elementos", "Total de páginas"),
                                                        List.of("Contraseña del usuario")),
                                                trueFalse("spring-p3-q5", "La paginación ayuda a controlar memoria, tiempo de respuesta y volumen transferido.", 4, true))))),
                        section("spring-seguridad", "Módulo 4 · Seguridad mediante tokens",
                                "Proteja los endpoints y procese credenciales mediante un flujo de autenticación desacoplado.",
                                List.of(
                                        playlistLesson("spring-11", "11. Autenticación por tokens", 0, 11, 465,
                                                "Comprenda el flujo de autenticación y la entrega de un token al cliente.",
                                                List.of("Diferenciar autenticación y autorización", "Emitir un token tras validar credenciales", "Enviar el token en solicitudes posteriores"),
                                                "Dibuje el flujo completo desde el inicio de sesión hasta el acceso a un endpoint protegido.",
                                                List.of("token", "Bearer", "credenciales", "autenticación", "autorización")),
                                        playlistLesson("spring-12", "12. Acceso a la entidad de seguridad", 1, 12, 944,
                                                "Integre la información del usuario con el mecanismo de seguridad de Spring.",
                                                List.of("Cargar el usuario", "Asociar roles o autoridades", "Evitar exponer credenciales en respuestas"),
                                                "Implemente el servicio que recupera el usuario y traduzca sus permisos a autoridades.",
                                                List.of("UserDetails", "authorities", "roles", "PasswordEncoder", "usuario autenticado")),
                                        playlistLesson("spring-13", "13. Filtro JWT y validación del token", 2, 13, 1170,
                                                "Intercepte cada solicitud, valide el token y establezca el contexto de seguridad.",
                                                List.of("Leer el encabezado Authorization", "Validar firma y expiración", "Continuar la cadena de filtros de forma segura"),
                                                "Proteja un endpoint y pruebe acceso válido, token vencido y solicitud sin token.",
                                                List.of("OncePerRequestFilter", "JWT", "SecurityContext", "Authorization", "expiración"))),
                                List.of(quiz("spring-parcial-4", "Evaluación 4 · Seguridad con tokens",
                                        "<p>Evalúe los conceptos de autenticación, autorización, usuarios, roles y filtros de seguridad.</p>",
                                        3, 80, 3, 15, true,
                                        List.of(
                                                single("spring-p4-q1", "¿Qué prefijo suele acompañar el token en el encabezado Authorization?", 0,
                                                        "Bearer", "BasicToken", "SessionOnly"),
                                                trueFalse("spring-p4-q2", "Autenticación y autorización representan exactamente el mismo proceso.", 1, false),
                                                single("spring-p4-q3", "¿Qué componente puede procesar una vez cada solicitud HTTP?", 2,
                                                        "OncePerRequestFilter", "JpaRepository", "CommandLineRunner"),
                                                multiple("spring-p4-q4", "¿Qué debe verificarse antes de aceptar un token?", 3,
                                                        List.of("Firma", "Expiración", "Usuario o sujeto", "Revocación cuando aplique"),
                                                        List.of("Color del botón")),
                                                single("spring-p4-q5", "¿Qué estado corresponde normalmente a credenciales ausentes o inválidas?", 4,
                                                        "401 Unauthorized", "404 Not Found", "201 Created"),
                                                single("spring-p4-q6", "¿Qué estado corresponde a un usuario autenticado sin permiso suficiente?", 5,
                                                        "403 Forbidden", "200 OK", "301 Moved Permanently"),
                                                trueFalse("spring-p4-q7", "Las contraseñas deben almacenarse mediante un algoritmo de hash apropiado.", 6, true))))),
                        section("spring-cliente", "Módulo 5 · Cliente REST y proyecto final",
                                "Consuma la API desde otro componente y consolide todo el conocimiento del curso.",
                                List.of(
                                        playlistLesson("spring-14", "14. Plantilla del cliente REST", 0, 14, 761,
                                                "Prepare una aplicación cliente capaz de comunicarse con los endpoints construidos.",
                                                List.of("Definir la URL base", "Modelar las respuestas del servidor", "Separar configuración y consumo"),
                                                "Cree una clase cliente y configure la dirección del servicio sin escribirla directamente en cada método.",
                                                List.of("cliente REST", "URL base", "configuración", "DTO de respuesta", "integración")),
                                        playlistLesson("spring-15", "15. Consumo con RestTemplate", 1, 15, 973,
                                                "Ejecute operaciones HTTP desde Java y gestione respuestas y errores del servicio remoto.",
                                                List.of("Realizar solicitudes GET y POST", "Enviar encabezados y cuerpo", "Procesar respuestas y errores"),
                                                "Consuma al menos dos endpoints de la API y muestre los resultados en la aplicación cliente.",
                                                List.of("RestTemplate", "HttpEntity", "HttpHeaders", "ResponseEntity", "manejo de errores"))),
                                List.of(quiz("spring-final", "Examen final · API REST completa",
                                        "<p>Evaluación acumulativa del proyecto. Debe obtener al menos 80% para completar el curso.</p>",
                                        2, 80, 2, 30, true,
                                        List.of(
                                                single("spring-final-q1", "¿Qué dependencia se utiliza para crear controladores HTTP en Spring Boot?", 0,
                                                        "Spring Web", "Spring Batch", "Spring Shell"),
                                                single("spring-final-q2", "¿Qué anotación identifica una clase persistente para JPA?", 1,
                                                        "@Entity", "@ControllerAdvice", "@ConfigurationProperties"),
                                                single("spring-final-q3", "¿Qué capa debe concentrar las reglas del caso de uso?", 2,
                                                        "Servicio", "Tema", "Controlador CSS"),
                                                trueFalse("spring-final-q4", "Es recomendable devolver siempre entidades JPA directamente al cliente.", 3, false),
                                                single("spring-final-q5", "¿Qué método HTTP corresponde a una consulta sin modificar el recurso?", 4,
                                                        "GET", "POST", "DELETE"),
                                                single("spring-final-q6", "¿Qué código indica una eliminación exitosa sin cuerpo de respuesta?", 5,
                                                        "204 No Content", "201 Created", "409 Conflict"),
                                                multiple("spring-final-q7", "Seleccione ventajas de utilizar DTO.", 6,
                                                        List.of("Controlar el contrato público", "Evitar exponer campos internos", "Validar entradas", "Facilitar evolución"),
                                                        List.of("Eliminar la necesidad de servicios")),
                                                single("spring-final-q8", "¿Qué objeto se usa para solicitar una página de datos?", 7,
                                                        "Pageable", "LoggerFactory", "PasswordEncoder"),
                                                trueFalse("spring-final-q9", "Los logs deben evitar secretos, contraseñas y tokens completos.", 8, true),
                                                single("spring-final-q10", "¿Dónde se envía normalmente un token Bearer?", 9,
                                                        "Encabezado Authorization", "Nombre de la tabla", "Archivo CSS"),
                                                single("spring-final-q11", "¿Qué componente valida un token antes de llegar al controlador?", 10,
                                                        "Filtro de seguridad", "Repositorio JPA", "Convertidor de moneda"),
                                                trueFalse("spring-final-q12", "Un token expirado debe aceptarse si el usuario existe.", 11, false),
                                                single("spring-final-q13", "¿Qué clase tradicional de Spring permite consumir servicios HTTP desde Java?", 12,
                                                        "RestTemplate", "ModelMapper", "FlywayMigrationStrategy"),
                                                multiple("spring-final-q14", "¿Qué elementos forman una API mantenible?", 13,
                                                        List.of("Contratos definidos", "Validación", "Errores consistentes", "Pruebas", "Seguridad"),
                                                        List.of("Dependencia del tema activo")),
                                                shortText("spring-final-q15", "Escriba la sigla del estilo arquitectónico usado por la API del curso.", 14,
                                                        List.of("REST", "rest"))))))));
    }

    private AcademyDtos.CourseRequest vueCourse() {
        return course(
                "Vue 3 y TypeScript práctico",
                "vue3-typescript-practico",
                "Construya interfaces conectadas al backend con estados de carga, error y contenido vacío.",
                "<p>Una ruta práctica para crear componentes Vue 3 sólidos con Composition API y TypeScript.</p>",
                "course-vue.svg",
                "Academy",
                AcademyLevel.INTERMEDIATE,
                AcademyAccessType.PAID,
                new BigDecimal("39.00"),
                210,
                true,
                List.of(
                        section("componentes", "Componentes y estado", "Composition API, propiedades y datos remotos.",
                                List.of(
                                        video("vue-intro", "Estructura de una vista Vue", 0,
                                                "<p>Video demostrativo para comprobar el reproductor dentro del aula.</p>", true, 460),
                                        text("typescript", "Tipos que previenen errores", 1,
                                                "<p>Modele respuestas REST, eventos y estados locales con tipos explícitos y valores predeterminados.</p>"),
                                        text("states", "Carga, error y estado vacío", 2,
                                                "<p>Cada vista conectada debe explicar qué sucede mientras carga, cuando falla y cuando aún no hay información.</p>")),
                                List.of(quiz("quiz-vue", "Evaluación Vue 3", 3, 70, 3, 8, false,
                                        List.of(
                                                single("vue-q1", "¿Qué API se usa en los ejemplos del plugin?", 0,
                                                        "Composition API", "Options API exclusivamente", "jQuery"),
                                                multiple("vue-q2", "Seleccione estados necesarios en una pantalla remota.", 1,
                                                        List.of("Cargando", "Error", "Vacío"),
                                                        List.of("Botón sin acción")),
                                                trueFalse("vue-q3", "Los estilos específicos deben aislarse para no contaminar otras páginas.", 2, true))))),
                        section("experiencia", "Experiencia responsive", "Jerarquía, accesibilidad y dispositivos móviles.",
                                List.of(
                                        text("responsive", "Diseño sin desbordamientos", 0,
                                                "<p>Use grids fluidos, límites de ancho, saltos de línea y componentes que se adapten desde 320 píxeles.</p>"),
                                        text("forms", "Formularios conectados", 1,
                                                "<p>Deshabilite acciones durante el guardado y muestre retroalimentación clara al usuario.</p>")),
                                List.of())));
    }

    private AcademyDtos.CourseRequest businessCourse() {
        return course(
                "Negocio digital sostenible",
                "negocio-digital-sostenible",
                "Convierta conocimiento técnico en una oferta que venda, genere valor y pueda mantenerse.",
                "<p>Curso introductorio para definir problema, cliente, propuesta de valor, ingresos y operación sostenible.</p>",
                "course-business.svg",
                "NaT Lab",
                AcademyLevel.ALL_LEVELS,
                AcademyAccessType.FREE,
                BigDecimal.ZERO,
                120,
                false,
                List.of(
                        section("propuesta", "Propuesta de valor", "Del problema real a una solución comprensible.",
                                List.of(
                                        video("modelo", "Diseñe una oferta útil", 0,
                                                "<p>Video demostrativo para mostrar cómo una lección puede mezclar explicación y material escrito.</p>", true, 380),
                                        text("cliente", "Defina el cliente y su problema", 1,
                                                "<p>Describa una situación concreta, el costo de no resolverla y el resultado que el cliente espera.</p>"),
                                        text("ingresos", "Modelo de ingresos y continuidad", 2,
                                                "<p>Combine implementación, suscripción, soporte y formación de acuerdo con el valor entregado.</p>")),
                                List.of(quiz("quiz-negocio", "Diagnóstico de la idea", 3, 70, 0, 0, false,
                                        List.of(
                                                single("business-q1", "¿Qué debe validar primero un producto digital?", 0,
                                                        "Un problema real del cliente", "El color del logotipo", "La cantidad de pantallas"),
                                                longText("business-q2", "Describa en pocas líneas el beneficio social de su proyecto.", 1))))),
                        section("ejecucion", "Ejecución sostenible", "Métricas, operación y aprendizaje.",
                                List.of(
                                        text("metricas", "Mida valor y sostenibilidad", 0,
                                                "<p>Observe adopción, retención, margen, tiempo de soporte e impacto generado.</p>"),
                                        text("roadmap", "Construya un plan por fases", 1,
                                                "<p>Priorice una capacidad vendible, valide con clientes y amplíe sin comprometer la calidad técnica.</p>")),
                                List.of())));
    }

    private AcademyDtos.CourseRequest course(
            String title,
            String slug,
            String excerpt,
            String description,
            String image,
            String instructor,
            AcademyLevel level,
            AcademyAccessType accessType,
            BigDecimal price,
            int minutes,
            boolean featured,
            List<AcademyDtos.SectionRequest> sections) {
        return new AcademyDtos.CourseRequest(
                title, slug, excerpt, description,
                ASSET_ROOT + image, instructor, demoCategory(slug), level, accessType,
                AcademyCourseStatus.PUBLISHED, price, "USD", minutes,
                featured, "demo:" + slug, DEMO_SOURCE + slug, sections);
    }


    private String demoCategory(String slug) {
        return switch (slug) {
            case "java-spring-boot-apis" -> "Desarrollo backend";
            case "vue3-typescript-practico" -> "Desarrollo frontend";
            case "negocio-digital-sostenible" -> "Negocios digitales";
            default -> "TrastCMS";
        };
    }

    private AcademyDtos.SectionRequest section(
            String key,
            String title,
            String description,
            List<AcademyDtos.LessonRequest> lessons,
            List<AcademyDtos.QuizRequest> quizzes) {
        return new AcademyDtos.SectionRequest(
                null, title, description, null,
                "demo:section:" + key, lessons, quizzes);
    }

    private AcademyDtos.LessonRequest video(
            String key,
            String title,
            int position,
            String content,
            boolean preview,
            int durationSeconds) {
        return new AcademyDtos.LessonRequest(
                null, title, content, position,
                AcademyLessonType.VIDEO, AcademyVideoProvider.YOUTUBE,
                YOUTUBE_DEMO, null, durationSeconds, preview,
                "demo:lesson:" + key);
    }

    private AcademyDtos.LessonRequest video(
            String key,
            String title,
            int position,
            String content,
            boolean preview,
            int durationSeconds,
            String videoUrl) {
        return new AcademyDtos.LessonRequest(
                null, title, content, position,
                AcademyLessonType.VIDEO, AcademyVideoProvider.YOUTUBE,
                videoUrl, null, durationSeconds, preview,
                "demo:lesson:" + key);
    }

    private AcademyDtos.LessonRequest playlistLesson(
            String key,
            String title,
            int position,
            int playlistPosition,
            int durationSeconds,
            String summary,
            List<String> objectives,
            String practice,
            List<String> concepts) {
        return video(key, title, position,
                lessonContent(summary, objectives, practice, concepts),
                true, durationSeconds, springPlaylistVideo(playlistPosition));
    }

    private String springPlaylistVideo(int playlistPosition) {
        String verifiedVideoId = switch (playlistPosition) {
            case 1 -> "UpdkMjunXSk";
            case 2 -> "eojRj2ws3Ak";
            case 4 -> "pYhGeYUN34c";
            case 5 -> "rGx8mdBVAkI";
            case 8 -> "z8PBjJVpidY";
            case 11 -> "JcfnXNlnb_w";
            case 12 -> "mqjHXobMvwQ";
            default -> null;
        };
        if (verifiedVideoId != null) {
            return "https://www.youtube.com/watch?v=" + verifiedVideoId;
        }
        return "https://www.youtube.com/watch?v=" + SPRING_PLAYLIST_FIRST_VIDEO
                + "&list=" + SPRING_PLAYLIST_ID
                + "&index=" + Math.max(1, playlistPosition);
    }

    private String lessonContent(
            String summary,
            List<String> objectives,
            String practice,
            List<String> concepts) {
        StringBuilder html = new StringBuilder();
        html.append("<p>").append(summary).append("</p>");
        html.append("<h3>Objetivos de aprendizaje</h3><ul>");
        for (String objective : objectives) html.append("<li>").append(objective).append("</li>");
        html.append("</ul><h3>Práctica de la lección</h3><p>").append(practice).append("</p>");
        html.append("<h3>Conceptos clave</h3><ul>");
        for (String concept : concepts) html.append("<li>").append(concept).append("</li>");
        html.append("</ul><p><em>Después de completar la práctica, marque la lección como terminada para actualizar el progreso del curso.</em></p>");
        return html.toString();
    }

    private AcademyDtos.LessonRequest mixed(
            String key,
            String title,
            int position,
            String content,
            int durationSeconds) {
        return new AcademyDtos.LessonRequest(
                null, title, content, position,
                AcademyLessonType.MIXED, AcademyVideoProvider.YOUTUBE,
                YOUTUBE_DEMO, null, durationSeconds, false,
                "demo:lesson:" + key);
    }

    private AcademyDtos.LessonRequest text(
            String key,
            String title,
            int position,
            String content) {
        return new AcademyDtos.LessonRequest(
                null, title, content, position,
                AcademyLessonType.TEXT, null,
                null, null, 0, false,
                "demo:lesson:" + key);
    }

    private AcademyDtos.QuizRequest quiz(
            String key,
            String title,
            int position,
            int passingScore,
            int attempts,
            int duration,
            boolean randomize,
            List<AcademyDtos.QuestionRequest> questions) {
        return quiz(key, title,
                "<p>Evaluación demostrativa creada por TrastAcademy.</p>",
                position, passingScore, attempts, duration, randomize, questions);
    }

    private AcademyDtos.QuizRequest quiz(
            String key,
            String title,
            String description,
            int position,
            int passingScore,
            int attempts,
            int duration,
            boolean randomize,
            List<AcademyDtos.QuestionRequest> questions) {
        return new AcademyDtos.QuizRequest(
                null, title, description,
                position, BigDecimal.valueOf(passingScore), attempts,
                duration, randomize, false,
                "demo:quiz:" + key, questions);
    }

    private AcademyDtos.QuestionRequest single(
            String key,
            String text,
            int position,
            String correct,
            String... incorrect) {
        List<AcademyDtos.OptionRequest> options = new ArrayList<>();
        options.add(option(correct, 0, true));
        for (int index = 0; index < incorrect.length; index++) {
            options.add(option(incorrect[index], index + 1, false));
        }
        return question(key, text, AcademyQuestionType.SINGLE_CHOICE,
                position, BigDecimal.ONE,
                "Revise la separación de responsabilidades de la plataforma.", options);
    }

    private AcademyDtos.QuestionRequest multiple(
            String key,
            String text,
            int position,
            List<String> correct,
            List<String> incorrect) {
        List<AcademyDtos.OptionRequest> options = new ArrayList<>();
        int positionIndex = 0;
        for (String value : correct) options.add(option(value, positionIndex++, true));
        for (String value : incorrect) options.add(option(value, positionIndex++, false));
        return question(key, text, AcademyQuestionType.MULTIPLE_CHOICE,
                position, BigDecimal.valueOf(2),
                "Puede haber más de una respuesta correcta.", options);
    }

    private AcademyDtos.QuestionRequest trueFalse(
            String key,
            String text,
            int position,
            boolean answer) {
        return question(key, text, AcademyQuestionType.TRUE_FALSE,
                position, BigDecimal.ONE,
                answer ? "La afirmación es verdadera." : "La afirmación es falsa.",
                List.of(option("Verdadero", 0, answer), option("Falso", 1, !answer)));
    }

    private AcademyDtos.QuestionRequest shortText(
            String key,
            String text,
            int position,
            List<String> accepted) {
        List<AcademyDtos.OptionRequest> options = new ArrayList<>();
        for (int index = 0; index < accepted.size(); index++) {
            options.add(option(accepted.get(index), index, true));
        }
        return question(key, text, AcademyQuestionType.SHORT_TEXT,
                position, BigDecimal.ONE,
                "Se aceptan respuestas equivalentes configuradas por el instructor.", options);
    }

    private AcademyDtos.QuestionRequest longText(
            String key,
            String text,
            int position) {
        return question(key, text, AcademyQuestionType.LONG_TEXT,
                position, BigDecimal.valueOf(5),
                "La respuesta debe ser revisada por un instructor.", List.of());
    }

    private AcademyDtos.QuestionRequest question(
            String key,
            String text,
            AcademyQuestionType type,
            int position,
            BigDecimal points,
            String explanation,
            List<AcademyDtos.OptionRequest> options) {
        return new AcademyDtos.QuestionRequest(
                null, text, type, position, points,
                explanation, true, options);
    }

    private AcademyDtos.OptionRequest option(
            String text,
            int position,
            boolean correct) {
        return new AcademyDtos.OptionRequest(null, text, position, correct);
    }
}
