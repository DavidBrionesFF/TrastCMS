# API REST de TrastAcademy

**Versión de TrastCMS:** 3.6.1
**Plugin:** `trastacademy` 1.0.3
**Formato:** JSON UTF-8
**OpenAPI:** grupo `academy` en `/v3/api-docs/academy`

## 1. Autenticación

El panel web y `/api/account/**` continúan utilizando la sesión de Spring Security y CSRF. La aplicación móvil utiliza tokens opacos revocables. TrastAcademy aporta el autenticador mediante el contrato público `BearerTokenAuthenticator`; el núcleo no accede a sus tablas de tokens.

### Registrar una cuenta

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "displayName": "Estudiante",
  "password": "Una-Clave-Segura-2026!",
  "phone": "+504 9999-9999",
  "deviceName": "Aplicación Android"
}
```

Respuesta `201 Created`:

```json
{
  "tokenType": "Bearer",
  "accessToken": "token-opaco",
  "refreshToken": "refresh-opaco",
  "expiresIn": 3600,
  "expiresAt": "2026-07-27T02:00:00Z",
  "refreshExpiresAt": "2026-08-26T01:00:00Z",
  "userId": "uuid",
  "email": "student@example.com",
  "displayName": "Estudiante",
  "role": "CUSTOMER"
}
```

La contraseña debe tener al menos 12 caracteres.

### Iniciar sesión

```http
POST /api/auth/token
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "password": "Una-Clave-Segura-2026!",
  "deviceName": "Aplicación Android"
}
```

### Renovar

```http
POST /api/auth/token/refresh
Content-Type: application/json
```

```json
{ "refreshToken": "refresh-opaco" }
```

Al renovar, el refresh token anterior queda revocado.

### Validar

```http
POST /api/auth/token/validate
Authorization: Bearer token-opaco
```

### Revocar

```http
POST /api/auth/token/revoke
Authorization: Bearer token-opaco
```

También puede enviarse `{ "token": "..." }` en el cuerpo. La respuesta es `204 No Content`.

## 2. Catálogo público

### Configuración

```http
GET /api/public/academy/settings
```

### Cursos

Las respuestas de resumen y detalle incluyen `category`, además de nivel, acceso, duración, cantidad de módulos, lecciones y evaluaciones.

```http
GET /api/public/academy/courses?search=java&page=0&size=24
```

Respuesta paginada:

```json
{
  "content": [],
  "page": 0,
  "size": 24,
  "totalElements": 0,
  "totalPages": 0
}
```

### Curso por slug

```http
GET /api/public/academy/courses/{slug}
```

Para visitantes solo se entrega el contenido marcado como vista previa. Cuando existe una sesión autenticada y matrícula activa, la respuesta incluye `enrollment` y el currículo completo.

## 3. Mi Cuenta

Requiere sesión autenticada.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/account/academy/courses` | Matrículas del usuario con progreso combinado de lecciones y evaluaciones aprobadas. |
| GET | `/api/account/academy/courses/{courseId}` | Curso, currículo y progreso. |
| POST | `/api/account/academy/courses/{courseId}/enroll` | Inscripción a un curso gratuito. |
| GET | `/api/account/academy/lessons/{lessonId}` | Lección completa. |
| PUT | `/api/account/academy/lessons/{lessonId}/progress` | Guardar avance o completar. |
| GET | `/api/account/academy/quizzes/{quizId}` | Evaluación sin respuestas correctas. |
| POST | `/api/account/academy/quizzes/{quizId}/attempts` | Iniciar intento. |
| POST | `/api/account/academy/attempts/{attemptId}/finish` | Evaluar y finalizar intento. |

Ejemplo de progreso:

```json
{
  "watchedSeconds": 420,
  "completed": true
}
```

Ejemplo de finalización de evaluación:

```json
{
  "answers": [
    {
      "questionId": "uuid",
      "selectedOptionIds": ["option-uuid"],
      "textAnswer": null
    }
  ]
}
```

Tipos de pregunta:

- `SINGLE_CHOICE`
- `MULTIPLE_CHOICE`
- `TRUE_FALSE`
- `SHORT_TEXT`
- `LONG_TEXT`

Las respuestas abiertas se almacenan para revisión; no se consideran correctas automáticamente. Cuando una evaluación define duración, el backend cierra como no aprobado cualquier intento que exceda el tiempo. Si `randomizeQuestions` está activo, el orden entregado al estudiante se mezcla sin alterar los identificadores usados para calificar.

## 4. API móvil

Requiere `Authorization: Bearer <accessToken>`.

| Método | Ruta |
|---|---|
| GET | `/api/mobile/academy/me/courses` |
| GET | `/api/mobile/academy/me/courses/{courseId}` |
| POST | `/api/mobile/academy/courses/{courseId}/enroll` |
| GET | `/api/mobile/academy/lessons/{lessonId}` |
| POST | `/api/mobile/academy/lessons/{lessonId}/complete` |
| GET | `/api/mobile/academy/quizzes/{quizId}` |
| POST | `/api/mobile/academy/quizzes/{quizId}/attempts` |
| POST | `/api/mobile/academy/attempts/{attemptId}/finish` |

## 5. Administración

Requiere `ADMIN` o `EDITOR` y CSRF en la interfaz web.

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/admin/academy/dashboard` | Métricas principales. |
| GET | `/api/admin/academy/courses` | Cursos paginados. |
| GET | `/api/admin/academy/courses/{id}` | Curso editable completo. |
| POST | `/api/admin/academy/courses` | Crear curso. |
| PUT | `/api/admin/academy/courses/{id}` | Actualizar curso y currículo. |
| DELETE | `/api/admin/academy/courses/{id}` | Eliminar o archivar si tiene matrículas. |
| GET | `/api/admin/academy/enrollments` | Matrículas. |
| POST | `/api/admin/academy/courses/{id}/enrollments` | Asignar curso por correo. |
| GET | `/api/admin/academy/settings` | Configuración. |
| PUT | `/api/admin/academy/settings` | Actualizar configuración. |
| POST | `/api/admin/academy/imports/preview` | Analizar fuente sin guardar. |
| POST | `/api/admin/academy/imports` | Importar o sincronizar. |
| GET | `/api/admin/academy/imports` | Historial de importación. |
| GET | `/api/admin/academy/reviews` | Intentos con respuestas abiertas pendientes. |
| POST | `/api/admin/academy/attempts/{attemptId}/review` | Calificar respuestas abiertas y cerrar el intento. |
| POST | `/api/admin/academy/demo-data` | Generar o sincronizar cuatro cursos demostrativos. |
| DELETE | `/api/admin/academy/demo-data` | Eliminar únicamente los cursos y matrículas demo. |

El cuerpo de un curso contiene sus secciones, lecciones, evaluaciones, preguntas y opciones. La operación es agregada y transaccional.

### Datos demo y tema de demostración

`POST /api/admin/academy/demo-data` es idempotente: crea los registros que faltan y actualiza los cursos previamente marcados como demostrativos. No reemplaza un curso real que tenga el mismo `slug`. La respuesta indica cuántos registros se crearon o conservaron, la ruta del catálogo y el tema recomendado:

```json
{
  "created": 4,
  "existing": 0,
  "deleted": 0,
  "enrolled": 1,
  "themeId": "nexus",
  "catalogUrl": "/academy",
  "message": "Se preparó una academia demostrativa..."
}
```

Los datos incluyen cursos gratuitos y pagados, clases de YouTube, contenido escrito, cuestionarios automáticos, respuestas abiertas y progreso de ejemplo. `DELETE /api/admin/academy/demo-data` solo elimina cursos con `demo_data=true`.

El plugin funciona con cualquiera de los temas comunitarios. Por ejemplo, `nexus` se activa desde `/api/admin/themes/active` y su contenido inicial puede restaurarse desde `/api/admin/themes/nexus/starter-content/restore`.

### Subir video

Los cursos pagados se integran con TrastPay mediante `SellableProvider`. El contrato marca el curso como `singleQuantity`, por lo que agregarlo repetidamente conserva una sola unidad. Si el comprador aún no tiene cuenta, la matrícula queda asociada al correo de la compra y puede reclamarse al registrarse con ese mismo correo.

TrastAcademy reutiliza la biblioteca de medios central:

```http
POST /api/admin/media?folder=Academy
Content-Type: multipart/form-data
```

Campo multipart: `file`.

La respuesta incluye `id`, `publicUrl`, MIME y metadatos. El `id` se guarda como `mediaId` y `publicUrl` como `videoUrl` de la lección.

El límite predeterminado es 500 MB por archivo y puede ajustarse sin modificar código:

```properties
TRASTCMS_UPLOAD_MAX_FILE_SIZE=500MB
TRASTCMS_UPLOAD_MAX_REQUEST_SIZE=550MB
TRASTCMS_MEDIA_MAX_BYTES=524288000
```

Los tres valores deben mantenerse coherentes; `TRASTCMS_MEDIA_MAX_BYTES` se expresa en bytes.

### Revisión manual

Las preguntas `LONG_TEXT` dejan el intento en `PENDING_REVIEW`. El administrador obtiene esos intentos en `/api/admin/academy/reviews` y envía únicamente los puntajes manuales:

```json
{
  "grades": [
    {
      "questionId": "uuid-pregunta",
      "awardedPoints": 3.5
    }
  ]
}
```

Cada puntaje debe estar entre `0` y el máximo de la pregunta. Al guardar, el backend combina los puntos objetivos y manuales, calcula el porcentaje, determina aprobado/reprobado y reevalúa la finalización del curso.

## 6. Importación por URL

### Previsualización

```json
{
  "url": "https://academy.example/api/course/10",
  "bearerToken": "token-opcional",
  "publish": false,
  "overwriteExisting": false
}
```

La previsualización devuelve:

- tipo detectado: `JSON` o `HTML`;
- curso normalizado;
- videos de YouTube;
- cantidad de secciones, lecciones, evaluaciones y preguntas;
- advertencias de información no disponible.

### Ejecución

Use el mismo cuerpo en `POST /api/admin/academy/imports`.

Estados del trabajo:

- `RUNNING`
- `COMPLETED`
- `FAILED`

`overwriteExisting=true` actualiza el curso asociado a la misma URL. No crea un duplicado. Secciones, lecciones, evaluaciones, preguntas y opciones se reconcilian por identificador externo y, cuando este no existe, por posición y contenido normalizado.

Cuando una respuesta LearnPress contiene solo el resumen del curso, el adaptador continúa automáticamente por los endpoints de secciones, elementos, detalle de lecciones/evaluaciones y preguntas. El token opcional se reutiliza en esas consultas. Se aplican límites de seguridad para evitar importaciones infinitas o excesivas.

### Estructura JSON reconocida

El adaptador reconoce variantes comunes:

- `title`, `title.rendered`, `name`, `course_name`;
- `content`, `content.rendered`, `description`;
- `sections`, `course_data.sections`, `curriculum.sections`;
- `items`, `lessons`, `quizzes`;
- `questions`, `answers`, `options`;
- `video_url`, `youtube_url`, iframes o enlaces YouTube.

Cuando una página HTML o una API no expone las respuestas correctas, el importador conserva la evaluación en borrador, registra una advertencia y no inventa esos datos. El curso no puede publicarse hasta completar la clave de respuestas de las preguntas autoevaluables.

## 7. Compatibilidad LearnPress v1

Base:

```text
/api/compat/learnpress/v1
```

La compatibilidad es funcional, no una copia de WordPress. Los identificadores son UUID en lugar de enteros.

### Autenticación compatible

| Método | Ruta |
|---|---|
| POST | `/token` |
| POST | `/token/validate` |
| POST | `/token/refresh` |
| POST | `/token/register` |

`/token` acepta `username` o `email`. La respuesta incluye:

```json
{
  "token": "token-opaco",
  "refresh_token": "refresh-opaco",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user_id": "uuid",
  "user_login": "student@example.com",
  "user_email": "student@example.com",
  "user_display_name": "Estudiante",
  "role": "CUSTOMER"
}
```

### Cursos y currículo

| Método | Ruta |
|---|---|
| GET | `/courses` |
| GET | `/courses/{id-or-slug}` |
| POST | `/courses/enroll` |
| POST | `/courses/finish` |
| POST | `/courses/retake` |
| GET | `/lessons?course_id={id}` |
| GET | `/lessons/{id}` |
| POST | `/lessons/finish` |
| GET | `/quiz?course_id={id}` |
| GET | `/quiz/{id}` |
| POST | `/quiz/start` |
| POST | `/quiz/check_answer` |
| POST | `/quiz/finish` |
| GET | `/questions?quiz_id={id}` |
| GET | `/questions/{id}` |
| GET | `/sections/sections-by-course-id/{courseId}` |
| GET | `/section-items/items/{sectionId}` |

### Aliases `users/*`

| Método | Ruta | Función |
|---|---|---|
| POST | `/users/start-quiz` | Inicia o recupera el intento abierto usando `item_id` y `course_id`. |
| POST | `/users/submit-quiz` | Envía `answered` como mapa `questionId -> respuesta`. |
| POST | `/users/check-answer` | Confirma recepción sin revelar la clave. |

## 8. Eventos y webhooks

TrastAcademy publica:

- `academy.course.created`
- `academy.course.updated`
- `academy.course.published`
- `academy.enrollment.created`
- `academy.lesson.completed`
- `academy.quiz.started`
- `academy.quiz.submitted`
- `academy.quiz.reviewed`
- `academy.quiz.completed`
- `academy.course.completed`
- `academy.course.retake`

El mecanismo central de TrastCMS puede distribuirlos a plugins y webhooks firmados.

## 9. Diagnóstico y MCP

### Diagnóstico administrativo

```http
GET /api/admin/academy/diagnostics
```

La respuesta incluye estado `READY` o `DEGRADED`, fecha de generación, totales e incidencias de integridad. Se detectan cursos sin módulos, módulos vacíos, lecciones sin contenido y evaluaciones sin preguntas o clave válida.

### Herramientas MCP

Cuando el plugin está activo se registran:

- `academy_dashboard`
- `academy_diagnostics`
- `academy_course_list`
- `academy_course_get`
- `academy_enrollment_list`
- `academy_student_progress`
- `academy_pending_reviews`
- `academy_import_preview`
- `academy_import_history`
- `academy_settings_get`

Todas son de solo lectura. Las operaciones que modifican cursos, matrículas, evaluaciones o calificaciones permanecen en la API administrativa.

## 10. Robustez de sesión y errores

La SPA obtiene el token CSRF desde `/api/auth/csrf`, lo sincroniza con la cookie `XSRF-TOKEN` y, ante un `403` en una operación de escritura, solicita un token nuevo y repite la petición una sola vez. Esto cubre la rotación de sesión al iniciar sesión o mantener el aula abierta durante un periodo prolongado.

El inicio de una evaluación persiste el intento antes de responder, reutiliza cualquier intento `IN_PROGRESS` aún válido y calcula el siguiente número a partir del último intento almacenado. Un intento `PENDING_REVIEW` bloquea otro intento hasta que el instructor lo revise.

### Códigos de error

La API nativa utiliza el manejador global de TrastCMS y códigos HTTP estándar:

- `400`: validación o estado inválido.
- `401`: token o sesión ausente.
- `403`: permiso insuficiente.
- `404`: plugin desactivado o recurso inexistente.
- `409`: slug duplicado o importación ya existente.

Nunca se deben registrar access tokens, refresh tokens, contraseñas ni el `bearerToken` utilizado para una importación.
## Curso demo completo de Spring Boot

Al ejecutar `POST /api/admin/academy/demo-data`, el curso `java-spring-boot-apis` se crea o actualiza con:

- acceso gratuito;
- 15 lecciones de YouTube vinculadas a la playlist de Byte Code;
- contenido descriptivo, objetivos, práctica y conceptos clave por lección;
- cuatro evaluaciones parciales y un examen final;
- 38 preguntas;
- matrícula automática para el administrador que genera la demostración.

La operación es idempotente. Puede ejecutarse nuevamente para actualizar una demostración creada con TrastCMS 3.3.1. Al regenerar, se reemplaza el currículo demo y se reconstruyen únicamente sus matrículas y muestras de progreso para evitar contenido obsoleto o duplicado.
