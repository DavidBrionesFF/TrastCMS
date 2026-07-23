# Arquitectura de TrastCMS 2.2

## Modelo de despliegue

TrastCMS es un **monolito modular** distribuido como un único JAR:

```text
Vue 3 + Tailwind + Tiptap + Plyr
             │
     /api/public y /api/admin
             │
Spring MVC + Spring Security + CSRF SPA
             │
Servicios, hooks y PF4J
             │
JPA + Flyway
             │
H2 / PostgreSQL / MySQL
```

Maven compila el frontend durante `generate-resources`, copia `frontend/dist` a `target/classes/static` y Spring Boot produce `target/trastcms-2.5.0-alpha.7.jar`.

## Dominios

- `user`: cuentas, roles y credenciales.
- `post`: publicaciones, páginas, revisiones, SEO y modo de edición.
- `category`: taxonomía.
- `media`: biblioteca y almacenamiento local.
- `setting`: configuración general y del tema.
- `plugin`: plugins externos y registro de capacidades.
- `audit`: trazabilidad administrativa.
- `theme`: paquetes visuales y personalización.

## Contenido

Una página puede usar:

- `RICH_TEXT`: HTML producido por Tiptap.
- `VISUAL_BUILDER`: documento JSON versionado con bloques y estilos.

El backend sanitiza ambos formatos antes de persistirlos. El constructor no guarda componentes Vue arbitrarios; guarda un contrato JSON que el renderer interpreta.

## Extensibilidad

### Hooks precompilados

Beans Spring que implementan `ContentHook`. Son compatibles con Native Image porque forman parte del análisis de compilación.

### Plugins Java PF4J

Disponibles en la JVM. Se cargan desde `data/plugins` y pueden aportar eventos, bloques, menús, páginas administrativas y acciones. Se consideran código confiable con acceso al proceso.

### Plugins externos

Servicios independientes que reciben eventos JSON firmados. Son el mecanismo recomendado para integraciones aisladas y para Native Image.

### Temas

ZIPs de datos, CSS y assets. No ejecutan JavaScript arbitrario en el servidor.

## Seguridad

- Sesiones HTTP con rotación al autenticar.
- CSRF mediante cookie y encabezado para SPA.
- Roles y restricciones de propiedad.
- Sanitización HTML, JSON del constructor y CSS.
- Límites de carga y rutas normalizadas.
- Temas protegidos contra ZIP Slip.
- Secretos cifrados con AES-GCM.
- Webhooks firmados con HMAC-SHA256.
- Contribuciones administrativas PF4J renderizadas de forma declarativa.

## Límites conscientes

- Los plugins Java dinámicos no funcionan dentro de Native Image.
- Un plugin PF4J es código confiable; no existe sandbox de JVM.
- El sistema todavía no incluye marketplace remoto firmado, actualizaciones automáticas, comercio electrónico ni colaboración simultánea.

## Contenido inicial y navegación

Los temas declaran páginas iniciales como datos. `ThemeStarterContentService`
las valida y crea como entidades `Post` de tipo `PAGE`. El menú público consulta
páginas publicadas con `show_in_menu=true` y las ordena por `menu_order`.

## Frontera de plugins incorporados

Los plugins incorporados se registran mediante `BundledPluginService`. El core
solo conoce su contrato, estado, contribuciones y eventos. La lógica de negocio,
entidades, migraciones, controladores y frontend de TrastCRM viven dentro de
`plugins/trastcrm`.


## REST y MCP

La plataforma de integración comparte los servicios de dominio del CMS:

```text
REST controllers ─┐
                  ├─> servicios de dominio ─> repositorios
MCP extensions ──┘
```

La API REST usa Spring MVC, OpenAPI y sesión/CSRF para administración. MCP usa Streamable HTTP, JSON-RPC 2.0 y un token Bearer independiente. Los plugins incorporados pueden implementar `McpExtension`; sus herramientas se publican únicamente mientras el plugin está activo.
