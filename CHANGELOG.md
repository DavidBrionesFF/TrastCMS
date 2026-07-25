# Changelog

## 2.7.0-alpha.9.1 — 2026-07-24

- Corrige imports de fecha y duración en TrastCRM.
- Actualiza las pruebas de contenido inicial para el tema Nexus.
- Corrige el `NullPointerException` al agregar planes SaaS al carrito asignando
  identificadores de línea antes del flush y sanitizando metadatos opcionales.
- Hace idempotente y segura la generación de datos demo de Store, SaaS, CRM y
  Commerce, incluso ante clics repetidos.
- Evita conflictos por `product_key` al regenerar la demo SaaS.
- Permite crear productos y planes SaaS con claves normalizadas.
- Mejora catálogo, ficha de producto, carrito, checkout y panel de tienda.
- Reemplaza cantidades select por controles numéricos accesibles.
- Agrega diagnóstico MCP, carga automática de `.env` y encabezado alternativo.

## 2.7.0-alpha.9 — 2026-07-23

### Experiencia y datos demostrativos

- Incorpora toasts globales y elimina alertas bloqueantes en Store, SaaS y Commerce.
- Agrega skeletons reutilizables en vistas públicas y paneles de plugins.
- Amplía los datos demo de TrastStore, TrastSaaS, TrastCRM y TrastPay.
- Cada plugin permite generar o eliminar únicamente sus registros demostrativos.
- Nexus se convierte en el tema predeterminado para instalaciones nuevas.

### TrastStore y TrastPay

- Catálogo público premium con búsqueda, categorías, ordenamiento y productos destacados.
- Ficha de producto, carrito y checkout con navegación y estados más claros.
- Productos con condición tributaria, clase fiscal y tasa de impuesto opcional.
- Configuración global de impuesto, precios con impuesto incluido y país fiscal.
- Cupones de demostración y métricas comerciales más completas.
- Integración PayPal Orders v2 con OAuth 2.0, redirección, captura y webhook verificado.

### TrastSaaS

- Configuración comercial, soporte, moneda, prueba, gracia y autoservicio.
- Catálogo público de planes mensuales, anuales y perpetuos.
- Suscripciones y licencias demostrativas con estados administrables.
- Portal de cliente para reclamar, copiar y revisar licencias.
- Corrige la construcción de metadatos SaaS durante el checkout.

### TrastCRM

- Empresas, contactos, oportunidades, actividades y formulario demo contextualizados.
- Acciones de generación y limpieza sin afectar información real.
- Skeletons y notificaciones integradas en el panel.

### Native Image

- Agrega hints de recursos para Spring AOT y GraalVM Native Image.
- Incorpora scripts PowerShell y shell para generar binarios.
- Agrega configuración de ejecución nativa y workflow multiplataforma.
- Documenta compilación para Linux, Windows, macOS Intel y macOS ARM.

## 2.6.0-alpha.8 — 2026-07-22

### Motor comercial

- Agrega `SellableProvider` y `PaymentGateway` como contratos reutilizables.
- Agrega dependencias declarativas entre plugins incorporados.
- Impide desactivar TrastPay mientras TrastStore o TrastSaaS estén activos.

### TrastPay

- Carritos, checkout, clientes, órdenes, pagos, cupones e idempotencia.
- Pasarelas de transferencia, contra entrega y sandbox.
- Moneda, impuestos, expiración del carrito e instrucciones bancarias configurables.
- Bloques de carrito y checkout, API REST, OpenAPI y herramientas MCP.

### TrastStore

- Productos físicos, digitales, servicios y productos externos.
- Variantes, SKU, inventario, backorders y movimientos.
- Fulfillment automático al confirmarse el pago.
- Tienda pública, ficha de producto, bloques, panel administrativo y MCP.

### TrastSaaS

- Productos de software, planes, periodos de prueba y entitlements.
- Suscripciones, licencias cifradas, activaciones, heartbeat y desactivación.
- Uso medido idempotente, releases y recuperación de licencias por orden.
- Tabla de precios, activador de licencias, API REST y herramientas MCP.

### Calidad

- Agrega pruebas integrales de compra de tienda y emisión/activación SaaS.
- Agrega documentación independiente para comercio, tienda y licensing.

## 2.5.0-alpha.7 — 2026-07-22

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

## 2.7.0-alpha.9.5

- Rediseño profesional de catálogo, ficha de producto y checkout en tres pasos.
- Portal SaaS con detalle de planes y explicación explícita de licencia o suscripción.
- Corrección de rutas de planes para evitar páginas 404.
- Administración de Store mediante cuadrícula de cuatro columnas y drawers laterales.
- Movimientos de inventario con entradas, salidas, daños, devoluciones y cantidades establecidas.
- Carga parcial tolerante a fallos en TrastSaaS y TrastCRM.
- Carga explícita de asociaciones JPA para planes, licencias, suscripciones y negocios.
- Contraste corregido para el tema Midnight.
- Nuevas pruebas de administración SaaS, movimientos de inventario y pipeline CRM.
