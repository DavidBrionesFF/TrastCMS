# TrastCMS MCP Server

## Descripción

TrastCMS incorpora un servidor Model Context Protocol dentro del mismo JAR. Expone herramientas, recursos y prompts para asistentes, IDE, automatizaciones y agentes que necesiten trabajar con el CMS sin depender de consultas SQL ni de contratos internos.

## Transporte

El endpoint predeterminado es:

```text
POST /mcp
```

El transporte es Streamable HTTP y los mensajes usan JSON-RPC 2.0. La implementación es stateless: no requiere mantener una sesión MCP del lado del servidor. Las notificaciones sin `id` responden con HTTP `202 Accepted`. Esta primera implementación no mantiene un canal SSE de eventos del servidor; por ello `GET /mcp` y `DELETE /mcp` responden `405 Method Not Allowed`, mientras que toda la comunicación operativa se realiza con `POST`.

Descubrimiento:

```text
GET /.well-known/mcp.json
```

## Configuración

```bash
TRASTCMS_MCP_ENABLED=true
TRASTCMS_MCP_ENDPOINT=/mcp
TRASTCMS_MCP_TOKEN=un-secreto-largo-y-aleatorio
```

El token MCP debe ser distinto de la contraseña administrativa y se envía así:

```http
Authorization: Bearer <TRASTCMS_MCP_TOKEN>
```

Si no se configura el token, el servidor devuelve `503 Service Unavailable` y no ejecuta solicitudes MCP.

## Inicialización

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer $TRASTCMS_MCP_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc":"2.0",
    "id":1,
    "method":"initialize",
    "params":{
      "protocolVersion":"2025-11-25",
      "capabilities":{},
      "clientInfo":{"name":"trastcms-demo","version":"1.0.0"}
    }
  }'
```

## Herramientas del núcleo

| Herramienta | Tipo | Descripción |
|---|---|---|
| `site_overview` | Lectura | Identidad, configuración y métricas del sitio. |
| `content_list` | Lectura | Lista páginas o publicaciones con paginación. |
| `content_get` | Lectura | Obtiene contenido por slug. |
| `content_create_draft` | Escritura | Crea un borrador sin publicarlo. |
| `category_list` | Lectura | Lista categorías editoriales. |

### Listar herramientas

```json
{
  "jsonrpc":"2.0",
  "id":2,
  "method":"tools/list",
  "params":{}
}
```

### Crear un borrador

```json
{
  "jsonrpc":"2.0",
  "id":3,
  "method":"tools/call",
  "params":{
    "name":"content_create_draft",
    "arguments":{
      "type":"PAGE",
      "title":"Nueva landing page",
      "excerpt":"Borrador creado desde MCP"
    }
  }
}
```

## Recursos

```text
trastcms://site
trastcms://navigation
trastcms://content/schema
trastcms://page/{slug}
trastcms://post/{slug}
```

Ejemplo:

```json
{
  "jsonrpc":"2.0",
  "id":4,
  "method":"resources/read",
  "params":{"uri":"trastcms://page/servicios"}
}
```

## Prompts

```text
cms_page_brief
seo_article_outline
```

Los prompts preparan instrucciones reutilizables para diseñar páginas o planificar artículos antes de invocar herramientas de escritura.

## Extensiones de plugins

Los plugins incorporados implementan `McpExtension`. Sus herramientas solo se publican cuando el plugin está activo. TrastCRM aporta:

```text
crm_pipeline_summary
crm_list_leads
crm_list_submissions
```

Al desactivar TrastCRM, estas herramientas desaparecen de `tools/list` y dejan de ser invocables.

## Errores JSON-RPC

| Código | Significado |
|---:|---|
| `-32600` | Solicitud inválida. |
| `-32601` | Método no soportado. |
| `-32602` | Parámetros inválidos. |
| `-32603` | Error interno. |

## Recomendaciones de seguridad

- Use HTTPS en producción.
- Genere un token de al menos 32 caracteres aleatorios.
- Rote el token periódicamente.
- No exponga el token en JavaScript público, logs, temas o repositorios.
- Revise las anotaciones de cada herramienta antes de permitir operaciones de escritura.
- Mantenga confirmación humana antes de publicar, borrar o modificar contenido sensible.
