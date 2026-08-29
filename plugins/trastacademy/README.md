# TrastAcademy

Plugin incorporado de TrastCMS para administrar cursos, secciones, lecciones en video, evaluaciones, matrículas, progreso e importaciones desde páginas o APIs externas.

## Responsabilidades

- Mantiene su propio dominio y tablas `academy_*`.
- Publica catálogo y detalle de cursos.
- Agrega el aula del estudiante y la sección **Mis cursos** en TrastAccount.
- Registra cursos pagados como vendibles mediante `SellableProvider` cuando TrastPay está activo.
- Reutiliza `MediaService` para videos subidos y `PlatformMailService` para notificaciones.
- Publica eventos `academy.*`, herramientas MCP y una API móvil con tokens revocables.
- Expone un adaptador de compatibilidad bajo `/api/compat/learnpress/v1` sin copiar código o tablas de WordPress/LearnPress, incluidos aliases `users/start-quiz`, `users/submit-quiz` y `users/check-answer`.

## Estructura

```text
plugins/trastacademy/
├── frontend/
├── src/main/java/com/nattechnologies/trastcms/plugins/academy/
├── src/main/resources/db/migration/V18__trastacademy_schema.sql
├── src/main/resources/db/migration/V19__trastacademy_demo_data.sql
├── src/main/resources/db/migration/V20__trastacademy_course_category.sql
└── README.md
```


## Experiencia editorial y catálogo

- El editor administrativo reutiliza `RichTextEditor`, la biblioteca de medios y una interfaz a pantalla completa similar a **Nueva publicación**.
- El currículo se administra por módulos, lecciones y evaluaciones, con selección enfocada para evitar cargar múltiples editores enriquecidos al mismo tiempo.
- El catálogo público incluye categorías persistentes, búsqueda, nivel, acceso, duración, ordenamiento, curso destacado y métricas derivadas de contenido real.
- El aula usa la respuesta pública autenticada como respaldo, abre la primera lección pendiente automáticamente y nunca reemplaza un currículo válido por una respuesta parcial.

## Datos demostrativos

Desde **Academia → Generar demo** un administrador puede crear una demostración idempotente que incluye:

- cuatro cursos publicados; el curso completo de Spring Boot y el curso de negocio son gratuitos;
- un curso completo de Spring Boot con 15 videos de YouTube, cinco módulos, descripciones prácticas y 38 preguntas;
- clases de texto, video YouTube y contenido mixto en los demás cursos;
- los cinco tipos de pregunta soportados por TrastAcademy;
- cuestionarios automáticos y una respuesta abierta para revisión manual;
- matrículas de ejemplo para el administrador en TrastCMS y Spring Boot, con dos lecciones completadas en cada curso;
- portadas servidas como assets del tema `nexus`.

`DELETE /api/admin/academy/demo-data` elimina exclusivamente cursos y matrículas marcados como demo. Los cursos reales, importados o creados por el usuario no se modifican.

## Tema Nexus

El tema incorporado `nexus` presenta las superficies completas del plugin:

- portada creada con el constructor visual;
- bloque de cursos y curso recomendado;
- catálogo `/academy`;
- ficha `/academy/courses/{slug}`;
- aula `/academy/learn/{courseId}`;
- progreso en Mi Cuenta;
- estilos de video, evaluaciones, currículo, inscripción y carrito.

Para preparar o actualizar la demostración: active **Nexus** desde Apariencia, restaure su contenido inicial y luego use **Generar demo** en TrastAcademy. Si los datos demo ya existían, la misma acción actualiza el currículo sin duplicarlo. La regeneración sustituye el currículo y reinicia únicamente las matrículas y el progreso marcados como demostrativos; los cursos reales no se alteran.

## Tipos de video

- `YOUTUBE`: almacena una URL canónica y se reproduce mediante `youtube-nocookie.com`.
- `UPLOAD`: reutiliza un medio subido a `/api/admin/media`; el límite predeterminado es 500 MB y se configura mediante las variables `TRASTCMS_UPLOAD_*` y `TRASTCMS_MEDIA_MAX_BYTES`.
- `EXTERNAL`: reproduce una URL externa compatible con el navegador.
- Las lecciones también pueden ser `TEXT` o `MIXED`.

## Importación

El importador acepta una URL HTTP/HTTPS y prioriza JSON estructurado. Cuando recibe HTML:

- extrae título, descripción, imagen Open Graph y contenido;
- detecta enlaces e iframes de YouTube;
- crea lecciones de video;
- avisa cuando existen indicios de cuestionarios que no exponen respuestas correctas.

Las importaciones se identifican por `externalSourceUrl`, por lo que una sincronización con `overwriteExisting=true` actualiza el curso sin duplicarlo. Para APIs LearnPress resumidas, el importador recorre secciones, elementos, detalle de lecciones/evaluaciones y preguntas antes de persistir.

## Diagnóstico y tolerancia a fallos

El panel **MCP y diagnóstico** consulta `GET /api/admin/academy/diagnostics` y presenta:

- estado operativo `READY` o `DEGRADED`;
- totales de cursos, módulos, lecciones, evaluaciones, preguntas, matrículas, intentos e importaciones;
- cursos sin módulos;
- módulos vacíos;
- lecciones sin texto, video o medio;
- evaluaciones sin preguntas o sin clave de respuesta;
- recomendaciones de reparación.

El panel administrativo y el catálogo público cargan sus recursos con tolerancia parcial. Una falla de configuración, historial o diagnóstico no elimina cursos válidos de la pantalla.

## Herramientas MCP

Cuando TrastAcademy está activo registra diez herramientas de solo lectura:

| Herramienta | Uso |
|---|---|
| `academy_dashboard` | Indicadores generales de la academia. |
| `academy_diagnostics` | Auditoría de integridad del contenido. |
| `academy_course_list` | Búsqueda y paginación de cursos. |
| `academy_course_get` | Curso y currículo completo. |
| `academy_enrollment_list` | Matrículas filtradas. |
| `academy_student_progress` | Progreso de un estudiante por correo. |
| `academy_pending_reviews` | Evaluaciones pendientes de revisión manual. |
| `academy_import_preview` | Previsualización segura de una importación. |
| `academy_import_history` | Historial de importaciones. |
| `academy_settings_get` | Configuración pública y comercial. |

Las herramientas no publican, eliminan, matriculan ni califican. Las operaciones de escritura continúan requiriendo la API administrativa y autorización humana.

## Seguridad

- Los endpoints administrativos requieren `ADMIN` o `EDITOR`.
- Mi Cuenta utiliza la sesión normal de Spring Security.
- La API móvil utiliza access tokens de una hora y refresh tokens de treinta días, almacenados como hash SHA-256 y revocables por dispositivo.
- El importador bloquea localhost, redes privadas, link-local, multicast, credenciales embebidas y puertos distintos de 80/443.
- Las evaluaciones sin clave de respuesta se conservan como borrador y deben revisarse antes de publicar; nunca se inventan respuestas correctas.
- Las respuestas correctas nunca se envían al estudiante antes de terminar el intento.
- Las preguntas abiertas dejan el intento en `PENDING_REVIEW`; un administrador asigna los puntos antes de emitir el resultado definitivo.
- Los límites de tiempo se aplican en backend y los cuestionarios pueden entregar preguntas en orden aleatorio sin exponer respuestas correctas.

## Integraciones opcionales

| Plugin | Integración |
|---|---|
| TrastPay | Venta y fulfillment de cursos pagados. |
| TrastAccount | Sección pública **Mis cursos**. |
| TrastCRM | Puede consumir eventos `academy.*`; no se accede a sus tablas. |

La documentación completa de endpoints está en [`docs/TRASTACADEMY_API.md`](../../docs/TRASTACADEMY_API.md).
