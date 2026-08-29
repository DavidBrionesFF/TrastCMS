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

TrastCMS carga automáticamente un archivo `.env` ubicado en la raíz del
proyecto. También puede configurar las mismas variables desde IntelliJ, el
sistema operativo o el servicio que ejecuta el JAR.

```bash
TRASTCMS_MCP_ENABLED=true
TRASTCMS_MCP_ENDPOINT=/mcp
TRASTCMS_MCP_TOKEN=un-secreto-largo-y-aleatorio
```

Después de modificar `.env` debe reiniciar la aplicación. El token admite
valores con o sin comillas exteriores; internamente se eliminan espacios y
comillas accidentales.

El token MCP debe ser distinto de la contraseña administrativa y se envía así:

```http
Authorization: Bearer <TRASTCMS_MCP_TOKEN>
```

También se admite el encabezado alternativo:

```http
X-MCP-Token: <TRASTCMS_MCP_TOKEN>
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

## Herramientas comerciales

Cuando los plugins correspondientes están activos se registran:

### TrastPay

```text
commerce_order_summary
commerce_list_orders
```

### TrastStore

```text
store_inventory_alerts
store_product_lookup
```

### TrastSaaS

```text
saas_subscription_summary
saas_list_licenses
```

### TrastAcademy

```text
academy_dashboard
academy_diagnostics
academy_course_list
academy_course_get
academy_enrollment_list
academy_student_progress
academy_pending_reviews
academy_import_preview
academy_import_history
academy_settings_get
```

Ejemplo de diagnóstico:

```json
{
  "jsonrpc":"2.0",
  "id":20,
  "method":"tools/call",
  "params":{
    "name":"academy_diagnostics",
    "arguments":{}
  }
}
```

Las herramientas de academia son de solo lectura. Permiten consultar integridad, progreso y operaciones pendientes sin publicar cursos, crear matrículas ni calificar respuestas.

Todas son herramientas de lectura. La versión inicial no permite confirmar
pagos, reembolsar órdenes, revocar licencias ni modificar inventario mediante
MCP. Esas operaciones requieren la API administrativa y confirmación humana.


## Diagnóstico desde el panel

Abra:

```text
Administración → API y MCP
```

El panel muestra si MCP está habilitado, si el token fue reconocido, la URL
absoluta y la cantidad de herramientas. El botón **Probar servidor MCP**
ejecuta una inicialización interna sin exponer el secreto.

Prueba desde PowerShell en Windows:

```powershell
$token = "su-token-mcp"
$body = @{
  jsonrpc = "2.0"
  id = 1
  method = "initialize"
  params = @{
    protocolVersion = "2025-11-25"
    capabilities = @{}
    clientInfo = @{ name = "powershell"; version = "1.0.0" }
  }
} | ConvertTo-Json -Depth 8

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/mcp" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $body
```

Respuestas frecuentes:

| Estado | Causa |
|---:|---|
| `401` | Token ausente o distinto del configurado. |
| `503` | MCP deshabilitado o token no cargado. Reinicie después de editar `.env`. |
| `400` | JSON-RPC o versión de protocolo inválidos. |
| `200` | Solicitud MCP procesada. |
