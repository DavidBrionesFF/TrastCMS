# Esquema de base de datos

TrastCMS usa JPA/Hibernate con Flyway para migraciones. Esta guía describe las entidades principales y sus relaciones en la versión 3.6.1.

Desde Multisite Foundation, los datos de negocio pertenecen a un tenant mediante `site_id`. Las identidades de usuario siguen siendo globales, pero sus permisos efectivos se conceden con membresías por sitio.

## Red multisitio

| Tabla | Responsabilidad |
|---|---|
| `cms_site` | Tenant, clave, nombre, estado, locale y zona horaria |
| `cms_site_endpoint` | Host, puerto, esquema, endpoint principal y estado |
| `cms_site_membership` | Relación entre usuario global, sitio y rol efectivo |

Los endpoints son únicos por host y puerto. Las tablas tenantizadas incluyen `site_id`, una clave foránea a `cms_site` y, cuando corresponde, restricciones únicas compuestas como `(site_id, slug)`.

## Entidades principales

### user_account

Cuentas de usuario con roles y credenciales.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `email` | VARCHAR(250) | Correo único |
| `password_hash` | VARCHAR(250) | Hash BCrypt |
| `display_name` | VARCHAR(250) | Nombre visible |
| `role` | VARCHAR(30) | Rol: ADMIN, EDITOR, AUTHOR, CUSTOMER |
| `enabled` | BOOLEAN | Cuenta activa |
| `last_login_at` | TIMESTAMP | Último acceso |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### post

Publicaciones y páginas.

En 3.6.1 el slug es único dentro del sitio, no globalmente, y la fila incluye `site_id`.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `title` | VARCHAR(250) | Título |
| `slug` | VARCHAR(280) | Slug único |
| `excerpt` | VARCHAR(600) | Resumen |
| `body` | TEXT | Contenido HTML sanitizado |
| `editor_mode` | VARCHAR(30) | RICH_TEXT o VISUAL_BUILDER |
| `builder_data` | TEXT | JSON del constructor visual |
| `custom_css` | TEXT | CSS personalizado por página |
| `seo_title` | VARCHAR(250) | Título SEO |
| `seo_description` | VARCHAR(500) | Descripción SEO |
| `page_template` | VARCHAR(120) | Plantilla |
| `show_in_menu` | BOOLEAN | Visible en menú |
| `menu_order` | INT | Orden en menú |
| `page_role` | VARCHAR(40) | Rol requerido para ver |
| `theme_origin` | VARCHAR(64) | Tema que creó la página |
| `content_type` | VARCHAR(30) | POST o PAGE |
| `status` | VARCHAR(30) | DRAFT, PUBLISHED, ARCHIVED |
| `featured_image_url` | VARCHAR(500) | Imagen destacada |
| `category_id` | VARCHAR(36) | FK a category |
| `author_id` | VARCHAR(36) | FK a user_account |
| `published_at` | TIMESTAMP | Fecha de publicación |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |
| `version` | BIGINT | Versión optimista |

### category

Taxonomía de contenido.

El nombre y el slug se aíslan por `site_id`.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `name` | VARCHAR(150) | Nombre único |
| `slug` | VARCHAR(180) | Slug único |
| `description` | VARCHAR(500) | Descripción |
| `created_at` | TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | Última actualización |

### media_asset

Archivos multimedia.

La biblioteca y sus carpetas se aíslan por `site_id`; el binario se conserva bajo `TRASTCMS_DATA_DIR`.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `filename` | VARCHAR(250) | Nombre de archivo almacenado |
| `original_filename` | VARCHAR(250) | Nombre original |
| `title` | VARCHAR(250) | Título |
| `alt_text` | VARCHAR(250) | Texto alternativo |
| `caption` | VARCHAR(500) | Leyenda |
| `description` | TEXT | Descripción |
| `content_type` | VARCHAR(100) | MIME type |
| `kind` | VARCHAR(30) | IMAGE, AUDIO, VIDEO, DOCUMENT |
| `folder` | VARCHAR(100) | Carpeta |
| `size_bytes` | BIGINT | Tamaño en bytes |
| `width` | INT | Ancho (imágenes) |
| `height` | INT | Alto (imágenes) |
| `duration_seconds` | INT | Duración (audio/video) |
| `storage_path` | VARCHAR(500) | Ruta en disco |
| `public_url` | VARCHAR(500) | URL pública |
| `uploaded_by` | VARCHAR(250) | Correo del usuario |
| `created_at` | TIMESTAMP | Fecha de carga |
| `updated_at` | TIMESTAMP | Última actualización |

### site_setting

Configuración clave-valor.

Para conservar compatibilidad, la clave física usa `{siteId}::{logicalKey}`. La API y los servicios trabajan con la clave lógica, por ejemplo `theme.active`.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `key` | VARCHAR(150) | Clave única |
| `value` | TEXT | Valor |
| `updated_at` | TIMESTAMP | Última actualización |

### audit_log

Registro de auditoría.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `actor` | VARCHAR(250) | Usuario o sistema |
| `action` | VARCHAR(100) | Acción realizada |
| `resource_type` | VARCHAR(50) | Tipo de recurso |
| `resource_id` | VARCHAR(36) | ID del recurso |
| `details` | VARCHAR(500) | Detalles |
| `created_at` | TIMESTAMP | Fecha |

### post_revision

Revisiones de contenido.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | VARCHAR(36) | UUID primario |
| `post_id` | VARCHAR(36) | FK a post |
| `revision_number` | INT | Número secuencial |
| `title` | VARCHAR(250) | Título en la revisión |
| `excerpt` | VARCHAR(600) | Resumen |
| `body` | TEXT | Contenido |
| `content_type` | VARCHAR(30) | Tipo de contenido |
| `editor_mode` | VARCHAR(30) | Modo de edición |
| `builder_data` | TEXT | JSON del constructor |
| `custom_css` | TEXT | CSS personalizado |
| `status` | VARCHAR(30) | Estado |
| `created_by` | VARCHAR(250) | Autor |
| `created_at` | TIMESTAMP | Fecha |

## Relaciones

- `post.author_id` → `user_account.id`
- `post.category_id` → `category.id`
- `post_revision.post_id` → `post.id`
- `cms_site_endpoint.site_id` → `cms_site.id`
- `cms_site_membership.site_id` → `cms_site.id`
- tablas tenantizadas `.site_id` → `cms_site.id`

## Migraciones

Las migraciones Flyway se encuentran en:

- `src/main/resources/db/migration/`
- `plugins/*/src/main/resources/db/migration/`

Convención: `V<version>__<descripcion>.sql` o una migración Java `V<version>__<descripcion>.java`. TrastCMS 3.6.1 incluye V21 multisitio, V22 reputación y V24 Travel/CleanPay.
