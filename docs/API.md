# API e integraciones de TrastCMS 2.5

TrastCMS ofrece dos contratos complementarios dentro del mismo JAR:

- [REST API](REST_API.md), documentada con OpenAPI y Swagger UI.
- [MCP Server](MCP.md), orientado a agentes, asistentes e IDE compatibles con Model Context Protocol.

La arquitectura y las reglas para extender ambos contratos se encuentran en [DEVELOPER_PLATFORM.md](DEVELOPER_PLATFORM.md).

## Accesos rápidos

| Recurso | Ruta |
|---|---|
| Portal público para desarrolladores | `/developers` |
| Panel administrativo API y MCP | `/admin/developer` |
| OpenAPI JSON | `/v3/api-docs` |
| Swagger UI | `/swagger-ui/index.html` |
| Descubrimiento MCP | `/.well-known/mcp.json` |
| Endpoint MCP | `/mcp` |

## Seguridad

- REST administrativo: sesión de Spring Security y CSRF.
- REST público: solo contenido expresamente publicado.
- MCP: token Bearer independiente configurado mediante `TRASTCMS_MCP_TOKEN`.
- Swagger/OpenAPI: rol `ADMIN`.
