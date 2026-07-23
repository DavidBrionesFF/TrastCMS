# Changelog

## 2.5.0-alpha.7

- Rediseño profesional de las páginas predeterminadas de Aurora, Coffee y Midnight.
- Portal público de desarrolladores y sección administrativa API y MCP.
- Servidor MCP integrado al mismo JAR mediante Streamable HTTP y JSON-RPC 2.0.
- Herramientas, recursos y prompts del núcleo.
- Herramientas MCP aportadas por TrastCRM cuando el plugin está activo.
- Documentación REST, MCP y arquitectura de integración dentro del repositorio.
- Configuración segura mediante `TRASTCMS_MCP_TOKEN`.


## 2.4.0-alpha.6 — 2026-07-22

- Rediseñó TrastCRM con dashboard, pipeline, contactos, empresas, actividades, formularios y bandeja de envíos.
- Incorporó un formulario predeterminado para solicitudes de proyectos web, además del formulario general.
- Agregó administración de menús de encabezado y footer desde Apariencia.
- Amplió las páginas iniciales de los tres temas con una demostración más completa del CMS.
- Agregó restauración explícita de páginas demostrativas sin sobrescribir contenido ajeno al tema.
- Mejoró categorías, usuarios y el editor de publicaciones, incluida la creación rápida de categorías.
- Incorporó un footer público profesional y una barra administrativa integrada con la sesión.

## 2.3.0-alpha.5 — 2026-07-22

### Temas y páginas iniciales

- Los temas pueden declarar `starter-content.json`.
- Aurora, Midnight y Coffee crean Inicio, Quiénes somos, Servicios, Blog y Contáctanos como páginas locales editables.
- Las páginas declaran rol, orden de menú, visibilidad y tema de origen.
- La portada pública puede renderizar una página local seleccionada desde Configuración.
- La importación es idempotente y nunca sobrescribe contenido existente.

### Experiencia pública y cuenta

- Barra administrativa estilo WordPress para usuarios con sesión iniciada.
- Navegación pública generada desde páginas publicadas.
- Mi cuenta incluye perfil, avatar, biografía, preferencias regionales, zona horaria, último acceso y seguridad.
- Configuración se amplía con identidad, lectura, portada, SEO, contacto, redes, rendimiento y mantenimiento.
- El centro de temas explica y enlaza la administración de las páginas iniciales creadas localmente.

### Motor de plugins

- Nuevo contrato `BundledPlugin` para extensiones separadas que se compilan dentro del JAR y Native Image.
- Estado persistente de activación y desactivación.
- Contribuciones combinadas de bloques y menús para plugins incorporados y PF4J.
- Maven y Vite incorporan código desde la carpeta `plugins/`.

### TrastCRM

- Plugin incorporado independiente dentro de `plugins/trastcrm`.
- Contactos, empresas, pipeline, negocios, actividades y formularios.
- Formularios públicos, envíos, creación automática de leads y estados de atención.
- Bloque de formulario insertable en el constructor visual.
- El constructor carga dinámicamente los formularios disponibles del plugin y permite seleccionarlos sin escribir claves manuales.
- Panel administrativo Vue propio y migración Flyway independiente.

### Pruebas

- Cobertura de páginas iniciales, navegación, registro del plugin incorporado y captura pública de formularios CRM.

## 2.2.0-alpha.4 — 2026-07-22

### Administración

- Rediseño del panel con Tailwind CSS y tipografía Outfit Variable.
- Barra lateral agrupada, colapsable y adaptable.
- Nuevo tablero con indicadores y accesos rápidos.
- Nueva sección independiente de páginas.

### Editor y constructor visual

- Integración de Tiptap 3 para edición rich text.
- Tablas, imágenes redimensionables, audio, video, código, enlaces, listas y alineación.
- Constructor visual con bloques, secciones, columnas y inspector.
- Capas, drag and drop, vistas responsivas, historial, patrones e importación/exportación JSON.
- Renderizado público de páginas visuales y CSS por página.
- Bloques declarativos aportados por plugins Java.

### Medios

- Biblioteca con carga múltiple, carpetas, filtros y vistas grid/lista.
- Eliminación masiva y edición de metadatos.
- Plyr para video y audio.
- Visor de imágenes con zoom, rotación y ajuste.

### Temas

- Centro de temas con instalación ZIP, previsualización y personalizador tipado.
- Validación reforzada de manifiestos, opciones y recursos.
- Outfit como tipografía predeterminada.

### Plugins

- Gestión ampliada de plugins externos y PF4J.
- Catálogo de eventos y permisos.
- Contribuciones de bloques y menús administrativos.
- Páginas administrativas declarativas y acciones Java autenticadas.
- Plugin Java de ejemplo actualizado.

### Seguridad y persistencia

- Sanitización recursiva del JSON del constructor.
- Conservación segura de estilos rich text y atributos de medios.
- Rechazo de URLs y CSS peligrosos.
- Pruebas del sanitizador y autenticación SPA.

## 2.1.0-alpha.3

- Base para editor visual, metadatos de medios y plugins PF4J.
- Migraciones V2 y V3.

## 2.0.0-alpha.2

- Correcciones de alias TypeScript, CSRF, MySQL y `PostService`.

## 2.0.0-alpha.1

- Reescritura inicial Spring Boot + Vue en un único JAR.
