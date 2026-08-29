# Variables de entorno

Referencia completa de variables de configuración de TrastCMS.

## Backend (Spring Boot)

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `TRASTCMS_ADMIN_EMAIL` | Sí | `admin@trastcms.local` | Correo del administrador inicial |
| `TRASTCMS_ADMIN_PASSWORD` | Sí | `ChangeMeNow-2026!` | Contraseña del administrador inicial |
| `TRASTCMS_ADMIN_NAME` | No | `Administrador` | Nombre visible del administrador |
| `TRASTCMS_DATA_DIR` | No | `./data` | Directorio de datos (base, uploads, plugins) |
| `TRASTCMS_BASE_URL` | No | `http://localhost:8080` | URL base para enlaces y assets |
| `TRASTCMS_PLUGIN_MASTER_KEY` | Sí | — | Clave maestra para registrar plugins externos |
| `TRASTCMS_JAVA_PLUGINS_ENABLED` | No | `true` | Habilitar carga de plugins PF4J en distribución JVM |
| `TRASTCMS_PLUGIN_DIR` | No | `plugins` | Directorio de plugins Java dinámicos |
| `TRASTCMS_MEDIA_MAX_BYTES` | No | `524288000` | Tamaño máximo de un archivo de medios |
| `TRASTCMS_MEDIA_MAX_BATCH_FILES` | No | `20` | Cantidad máxima de archivos por carga múltiple |
| `TRASTCMS_UPLOAD_MAX_FILE_SIZE` | No | `500MB` | Límite multipart por archivo |
| `TRASTCMS_UPLOAD_MAX_REQUEST_SIZE` | No | `550MB` | Límite multipart total por petición |
| `SESSION_COOKIE_SECURE` | No | `false` | Enviar cookie de sesión solo por HTTPS |
| `SPRING_PROFILES_ACTIVE` | No | — | Perfil Spring (`h2`, `postgresql`, `mysql`) |
| `DATABASE_URL` | Condicional | — | JDBC URL (requerida si no es H2) |
| `DATABASE_USERNAME` | Condicional | — | Usuario de base de datos |
| `DATABASE_PASSWORD` | Condicional | — | Contraseña de base de datos |

## Model Context Protocol (MCP)

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `TRASTCMS_MCP_ENABLED` | No | `true` | Habilitar servidor MCP; requiere token para uso autenticado |
| `TRASTCMS_MCP_ENDPOINT` | No | `/mcp` | Ruta del endpoint MCP |
| `TRASTCMS_MCP_TOKEN` | Sí | — | Token Bearer para acceder a MCP |

## Correo (SMTP)

Configurado desde el panel **Administración → Configuración → Correo**. No requiere variables de entorno.

## Frontend (Vite)

| Variable | Descripción |
|---|---|
| `VITE_API_URL` | URL del backend para el proxy de desarrollo |
| `VITE_PORT` | Puerto del servidor Vite (por defecto `5173`) |
| `VITE_SITE_HOST` | Host que Vite envía al backend para probar un tenant concreto |

## Plugins externos

Configurados desde el panel **Administración → Plugins**. Los secretos se cifran con AES-GCM antes de persistirse.

## Desarrollo local

```env
TRASTCMS_ADMIN_EMAIL=admin@trastcms.local
TRASTCMS_ADMIN_PASSWORD=ChangeMeNow-2026!
TRASTCMS_ADMIN_NAME=Administrador
TRASTCMS_DATA_DIR=./data
TRASTCMS_BASE_URL=http://localhost:8080
TRASTCMS_PLUGIN_MASTER_KEY=change-this-plugin-master-key-before-production
SESSION_COOKIE_SECURE=false
VITE_API_URL=http://localhost:8080
VITE_PORT=5173
TRASTCMS_MCP_ENABLED=true
TRASTCMS_MCP_ENDPOINT=/mcp
TRASTCMS_MCP_TOKEN=<secreto-largo-y-aleatorio>
```

## Producción

- Cambiar `TRASTCMS_ADMIN_PASSWORD`.
- Cambiar `TRASTCMS_PLUGIN_MASTER_KEY`.
- Usar `SESSION_COOKIE_SECURE=true`.
- Configurar base de datos dedicada (PostgreSQL o MySQL).
- Restringir `/actuator` mediante red o autenticación.
- Habilitar HTTPS.
