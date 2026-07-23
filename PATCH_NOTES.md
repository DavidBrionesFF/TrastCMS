# TrastCMS 2.5.0-alpha.7 — Parche incremental

## Base requerida

Aplique este paquete sobre **TrastCMS 2.4.0-alpha.6**.

## Alcance

- Archivos agregados: 13
- Archivos modificados: 28
- Archivos eliminados: 0

## Páginas públicas profesionales

Los tres temas incorporados incluyen una nueva demostración editable:

- Inicio con hero de producto, panel simulado, capacidades, flujo, arquitectura,
  métricas, prueba social y CTA.
- Quiénes somos con manifiesto, principios, evolución y CTA.
- Servicios con soluciones, proceso e integración con TrastCRM.
- Blog con portada editorial y categorías temáticas.
- Contáctanos con formulario de proyecto web, automatización CRM y métricas.
- Footer profesional y acceso a la documentación para desarrolladores.

Las páginas ya existentes no se sobrescriben automáticamente. Después de
actualizar, use:

```text
Administración → Temas → Restaurar páginas demo
```

La restauración solo reemplaza páginas que todavía pertenecen al tema activo.

## REST API y MCP

- OpenAPI enriquecido y Swagger UI.
- Portal público `/developers`.
- Sección administrativa **API y MCP**.
- Servidor MCP integrado al mismo JAR en `/mcp`.
- Streamable HTTP stateless y JSON-RPC 2.0.
- Autenticación Bearer independiente mediante `TRASTCMS_MCP_TOKEN`.
- Herramientas, recursos y prompts del núcleo.
- Herramientas MCP de TrastCRM disponibles solo cuando el plugin está activo.
- Soporte para lotes JSON-RPC y notificaciones.
- Documentación en `docs/REST_API.md`, `docs/MCP.md` y
  `docs/DEVELOPER_PLATFORM.md`.

## Configuración MCP

Agregue al entorno de ejecución:

```text
TRASTCMS_MCP_ENABLED=true
TRASTCMS_MCP_ENDPOINT=/mcp
TRASTCMS_MCP_TOKEN=un-secreto-largo-y-aleatorio
```

## Instalación

1. Detenga TrastCMS.
2. Haga una copia de seguridad de la base y de `data/`.
3. Copie el contenido del ZIP sobre la raíz del proyecto.
4. Permita reemplazar archivos.
5. Ejecute:

```powershell
.\mvnw.cmd clean verify
```

6. Inicie:

```powershell
java -jar target\trastcms-2.5.0-alpha.7.jar
```

No se agregan migraciones de base de datos en esta revisión.
