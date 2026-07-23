# TrastCMS REST API

## Objetivo

La API REST es el contrato HTTP principal de TrastCMS. Sirve al frontend Vue y permite integrar aplicaciones, automatizaciones, portales, aplicaciones móviles y servicios externos sin acceder directamente a la base de datos.

## Documentación ejecutable

Con la aplicación iniciada:

| Recurso | Ruta | Acceso |
|---|---|---|
| OpenAPI JSON | `/v3/api-docs` | Administrador |
| Swagger UI | `/swagger-ui/index.html` | Administrador |
| Portal de desarrollo | `/developers#rest` | Público |
| Catálogo administrativo | `/admin/developer` | Administrador |

La URL base se configura con `TRASTCMS_BASE_URL`.

## Autenticación

### Endpoints públicos

Las rutas bajo `/api/public/**` no requieren una sesión administrativa. Esto incluye contenido publicado, navegación, menús, temas y formularios públicos aportados por plugins.

### Endpoints administrativos

Las rutas bajo `/api/admin/**` utilizan la sesión de Spring Security. El login se realiza en:

```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded

username=admin@trastcms.local&password=...
```

Las operaciones que cambian datos requieren el token CSRF de la cookie `XSRF-TOKEN`, enviado en el encabezado `X-XSRF-TOKEN`. El frontend oficial realiza este flujo automáticamente.

## Convenciones

- Formato predeterminado: `application/json`.
- Fechas: ISO 8601 en UTC.
- Identificadores: cadenas opacas; no deben interpretarse como secuencias.
- Paginación: `page` comienza en cero y `size` define el tamaño de página.
- Errores: respuestas JSON con código HTTP y descripción del problema.
- Contenido publicado: `POST` para publicaciones y `PAGE` para páginas.
- Estados editoriales: `DRAFT`, `PUBLISHED` y `ARCHIVED`.

## Endpoints públicos principales

```text
GET  /api/public/site
GET  /api/public/navigation
GET  /api/public/menus
GET  /api/public/posts?page=0&size=12
GET  /api/public/posts/{slug}
GET  /api/public/pages/{slug}
GET  /api/public/categories
GET  /api/public/themes/{themeId}/tokens.css
GET  /api/public/media/{id}
```

### Ejemplo: listar publicaciones

```bash
curl "http://localhost:8080/api/public/posts?page=0&size=12"
```

### Ejemplo: leer una página

```bash
curl "http://localhost:8080/api/public/pages/servicios"
```

## Administración

El catálogo completo se genera en OpenAPI. Las áreas principales son:

```text
/api/admin/posts
/api/admin/categories
/api/admin/media
/api/admin/themes
/api/admin/plugins
/api/admin/users
/api/admin/settings
/api/admin/crm
/api/admin/developer
```

## Integración con plugins

Los plugins incorporados pueden aportar:

- rutas administrativas;
- rutas públicas;
- bloques del constructor visual;
- menús del panel;
- acciones y eventos;
- herramientas MCP.

TrastCRM, por ejemplo, aporta formularios públicos en:

```text
GET  /api/public/plugins/trastcrm/forms/{formKey}
POST /api/public/plugins/trastcrm/forms/{formKey}/submissions
```

## Compatibilidad y versionado

La versión alfa mantiene las rutas bajo `/api/public` y `/api/admin`. Antes de una versión estable, los cambios incompatibles deben documentarse en `CHANGELOG.md`. Para integraciones externas se recomienda generar un cliente desde `/v3/api-docs` y fijar la versión de TrastCMS desplegada.

## Seguridad para producción

- Cambie la contraseña administrativa inicial.
- Use HTTPS.
- Active `SESSION_COOKIE_SECURE=true`.
- Limite Swagger UI a administradores o a una red interna.
- No comparta cookies administrativas con aplicaciones de terceros.
- Use MCP con un token independiente para agentes y automatizaciones.
