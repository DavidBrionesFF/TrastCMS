# Plataforma para desarrolladores de TrastCMS

## Arquitectura

```text
Cliente web / aplicación / agente
                │
        ┌───────┴────────┐
        │                │
      REST              MCP
  Spring MVC      Streamable HTTP
        │                │
        └───────┬────────┘
                │
       Servicios de dominio
                │
  Contenido · Medios · Temas · Plugins
                │
          MySQL/PostgreSQL/H2
```

REST y MCP comparten los mismos servicios de dominio, validaciones, permisos y migraciones. No existen dos modelos de negocio separados.

## Elección del contrato

Use REST cuando:

- una aplicación conoce el endpoint que necesita;
- requiere carga de archivos;
- construye una interfaz web o móvil;
- necesita contratos OpenAPI y respuestas HTTP convencionales.

Use MCP cuando:

- un agente necesita descubrir capacidades dinámicamente;
- requiere herramientas descriptivas y esquemas de entrada;
- necesita leer recursos como contexto;
- trabaja con prompts reutilizables;
- debe incorporar capacidades aportadas por plugins activos.

## Desarrollo de herramientas MCP

Implemente:

```java
@Component
public final class MyMcpExtension implements McpExtension {
    @Override
    public List<McpToolDefinition> tools() {
        // Declare nombre, descripción, JSON Schema y anotaciones.
    }

    @Override
    public Object call(String toolName, Map<String, Object> arguments) {
        // Invoque servicios de dominio; no acceda directamente a controladores.
    }
}
```

Reglas:

- Use nombres estables en `snake_case`.
- Valide todos los argumentos.
- Marque correctamente lectura, destrucción e idempotencia.
- Devuelva objetos serializables por Jackson.
- Nunca incluya secretos en resultados.
- Las herramientas de plugins deben desaparecer cuando el plugin esté desactivado.

## Desarrollo REST

- Use DTO separados de las entidades JPA.
- Valide con Bean Validation.
- Devuelva códigos HTTP precisos.
- Mantenga rutas públicas y administrativas separadas.
- Documente cambios incompatibles.
- Agregue pruebas con `MockMvc`.

## Portal integrado

- Público: `/developers`
- Administración: `/admin/developer`
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI: `/v3/api-docs`
- MCP discovery: `/.well-known/mcp.json`
