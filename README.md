# TrastCMS 3.6.1

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)

TrastCMS es un CMS open source y autohospedable construido con **Spring Boot**, **Java 21** y **Vue 3**. El backend, el sitio público y todo el panel administrativo se compilan dentro de un **único JAR ejecutable**.

> Versión estable actual: `3.6.1`. Una sola instancia Spring Boot y una sola base de datos pueden servir múltiples sitios con dominios, usuarios, roles, contenido, temas, configuración y plugins aislados por tenant.

## Contribuir

¡Bienvenido! TrastCMS es un proyecto abierto y buscamos contribuidores de todos los niveles.

- **Primeros pasos**: revisa [`docs/GETTING_STARTED.md`](docs/GETTING_STARTED.md).
- **Código de conducta**: consulta [`docs/COMMUNITY.md`](docs/COMMUNITY.md).
- **Guía de estilo**: lee [`docs/CODE_STYLE.md`](docs/CODE_STYLE.md).
- **Issues para newcomers**: busca la etiqueta `good first issue` en [GitHub Issues](https://github.com/DavidBrionesFF/TrastCMS/issues).

¿Tienes dudas? Abre un issue con la etiqueta `question`.

## Multisite Foundation

- Panel **Red → Sitios** para crear, editar, suspender y reconfigurar tenants desde la misma administración.
- Resolución segura por host y puerto, compatible con Nginx y `X-Forwarded-*`; un host desconocido responde 404 en vez de caer en otro sitio.
- Administración de múltiples tenants desde una misma instalación.
- Membresías por sitio: una misma identidad puede tener roles diferentes en cada tenant.
- Tema, configuración, páginas, categorías, medios, plugins bundled y datos de negocio aislados mediante `site_id`.
- Blueprints comunitarios para academia, comercio, viajes y sitios vacíos.
- MCP resuelto por dominio del tenant y URL canónica por sitio.
- Documentación operativa en [`docs/MULTISITE_ARCHITECTURE.md`](docs/MULTISITE_ARCHITECTURE.md) y [`docs/MULTISITE_NGINX.md`](docs/MULTISITE_NGINX.md).

## Qué incluye esta versión

### Administración y diseño

- Panel administrativo rediseñado con Tailwind CSS.
- Tipografía **Outfit Variable** incorporada localmente en el bundle; no depende de Google Fonts durante la ejecución.
- Navegación agrupada, barra lateral colapsable, tablero con métricas y accesos rápidos.
- Secciones independientes para **Publicaciones** y **Páginas**.
- Roles `ADMIN`, `EDITOR`, `AUTHOR` y `CUSTOMER` con superficies separadas y autorización efectiva.

### Editor de contenido

- Editor rich text basado en Tiptap/ProseMirror.
- Títulos, párrafos, negrita, cursiva, subrayado, tachado y resaltado.
- Alineación, listas, citas, bloques de código y enlaces.
- Tablas editables, filas, columnas, encabezados y combinación de celdas.
- Inserción de imágenes redimensionables, audio y video desde la biblioteca.
- Contador de palabras y caracteres.
- Sanitización HTML y de estilos en el backend.

### Constructor visual de páginas

- Constructor visual inspirado en la experiencia de Elementor, integrado en las páginas.
- Arrastrar y soltar bloques.
- Secciones, columnas, títulos, texto enriquecido, imágenes, galerías, video, audio, botones, citas, icon boxes, estadísticas, divisores, espaciadores y HTML controlado.
- Panel de capas, inspector, duplicación, eliminación y reordenamiento.
- Vistas de escritorio, tableta y móvil.
- Historial con deshacer y rehacer.
- Patrones iniciales para hero, servicios y landing page.
- Importación y exportación del documento visual en JSON.
- CSS personalizado por página.
- Bloques declarados por plugins Java.

### Biblioteca de medios

- Carga múltiple mediante selector o arrastrar y soltar.
- Imágenes, video, audio y documentos.
- Vistas de cuadrícula y tabla.
- Carpetas, búsqueda, filtros, selección múltiple y eliminación masiva.
- Metadatos, título, texto alternativo, leyenda y descripción.
- Visor de imágenes con zoom, rotación, ajuste y acceso al original.
- Reproductores profesionales de video y audio mediante Plyr.

### Temas

- Centro de temas con previsualización, búsqueda, activación e instalación ZIP.
- Temas incorporados: **Aurora**, **Midnight**, **Coffee** y **Nexus**.
- Cada tema puede declarar contenido inicial en `starter-content.json`.
- Los temas incorporados crean contenido inicial editable y pueden ampliarse mediante paquetes ZIP.
- La activación es idempotente: no sobrescribe contenido ni recrea páginas eliminadas en cada reinicio.
- Personalizador generado desde `settingsSchema`.
- Colores, tipografía, ancho del sitio y otras opciones tipadas.
- Paquetes con manifiesto, assets, plantillas, patrones y fuentes locales.
- Validación contra ZIP Slip, límites de tamaño y CSS inseguro.

### Demostración de TrastAcademy

- Cuatro cursos demo idempotentes, gratuitos y pagados, con portadas, clases de video, texto y contenido mixto.
- Evaluaciones con los cinco tipos de pregunta, resultados automáticos y revisión manual.
- Progreso de ejemplo para el administrador que genera la demostración.
- Catálogo, ficha de curso, aula y Mi Cuenta compatibles con los temas comunitarios.
- Los datos demo pueden eliminarse sin tocar cursos reales.

### Plugins

TrastCMS admite cuatro mecanismos:

1. **Plugins incorporados**, separados dentro de `plugins/` y compilados en el JAR/Native Image.
2. **Hooks Spring compilados**, adecuados para lógica esencial y Native Image.
3. **Plugins Java dinámicos con PF4J** en la distribución JVM.
4. **Plugins externos por webhooks HMAC**, compatibles con JVM y Native Image.

Los plugins Java pueden declarar:

- Capacidades.
- Listeners de eventos.
- Bloques para el constructor visual.
- Entradas de menú administrativo.
- Páginas administrativas declarativas.
- Formularios y acciones Java autenticadas.

Se incluye un plugin PF4J completo en `examples/plugins/hello-java-plugin`.

### Datos demostrativos por plugin

Cada plugin incorporado puede preparar o retirar su propio entorno de demostración sin borrar información real:

- TrastCRM: empresas, contactos, negocios, actividades y formulario de diagnóstico.
- TrastPay: cupones y configuración fiscal de ejemplo.
- TrastStore: catálogo amplio, inventario, categorías, variantes e impuestos.
- TrastSaaS: productos de software, planes, pruebas, suscripciones, licencias y releases.
- TrastTravel: destinos, actividades, tours, salidas, extras y calendarios turísticos de Honduras.

Las acciones están disponibles en el encabezado de cada panel mediante **Generar demo** y **Eliminar demo**. Los registros se identifican con `demo_data`, por lo que la limpieza no depende de eliminar la base completa. Cuando el administrador elimina el demo, la decisión queda persistida y los ejemplos no reaparecen al reiniciar; pueden recuperarse en cualquier momento con **Generar demo**.

### Commerce Suite incorporada

- **TrastPay** aporta carrito, checkout, órdenes, impuestos, cupones y gateways, incluida una integración PayPal configurable.
- **TrastAcademy** aporta cursos, lecciones en video, cuestionarios, progreso, importación por URL y API móvil.
- **TrastStore** aporta productos, variantes, inventario y catálogo público.
- **TrastSaaS** aporta planes, suscripciones, licencias, activaciones y uso medido.
- Los plugins comerciales se integran mediante `SellableProvider`, `PaymentGateway` y eventos de fulfillment.
- La experiencia pública utiliza toasts, skeletons, vistas responsivas y estados vacíos.

### TrastCRM incorporado

El código de TrastCRM vive en `plugins/trastcrm` y se puede activar o desactivar sin eliminar sus datos. Incluye:

- Contactos, empresas, pipeline, negocios y actividades.
- Formularios dinámicos con envíos y estados de seguimiento.
- Creación automática de leads desde formularios públicos.
- Bloque `plugin:trastcrm:form` para insertar formularios en páginas.
- Panel Vue propio y migración Flyway independiente.
- Eventos `crm.*` para integraciones posteriores.

### Plataforma

- Contenido con borradores, publicación, archivo, slugs y revisiones.
- Categorías, SEO básico y auditoría administrativa.
- H2, PostgreSQL y MySQL mediante perfiles.
- Flyway, OpenAPI, Actuator, Docker y Docker Compose.
- GitHub Actions para CI, CodeQL, dependencias, Docker, Native Image y releases.

## Requisitos

- JDK 21.
- Internet durante la primera compilación para Maven y npm.
- Maven Wrapper incluido.

Maven descarga Node.js dentro de `target/node`; no es obligatorio instalar Node manualmente.

## Compilar un ejecutable nativo

Con GraalVM 25+ y `native-image` disponibles:

Windows PowerShell:

```powershell
.\scripts\build-native.ps1
```

Linux/macOS:

```bash
./scripts/build-native.sh
```

La guía completa está en [`docs/NATIVE_IMAGE.md`](docs/NATIVE_IMAGE.md). El ejecutable incluye Vue, los temas, MCP y los plugins incorporados. Los plugins PF4J instalados dinámicamente se reservan para la distribución JVM.

## Compilar el JAR único

Windows:

```powershell
.\scripts\build.ps1
```

Linux/macOS:

```bash
./scripts/build.sh
```

El proceso:

1. Descarga Node.js.
2. Ejecuta `npm install`.
3. Ejecuta `vue-tsc --noEmit`.
4. Compila Vue y Tailwind con Vite.
5. Copia `frontend/dist` dentro de los recursos Spring.
6. Compila las fuentes de prueba sin ejecutarlas.
7. Produce el JAR ejecutable y lo copia a `release/` y `dist/`.

La validación completa se ejecuta por separado con `mvnw clean verify`. Consulte [Pruebas](docs/TESTING.md) antes de publicar una release.

Resultado:

```text
target/trastcms-3.6.1.jar
```

## Ejecutar

```bash
java -jar target/trastcms-3.6.1.jar
```

- Sitio: `http://localhost:8080`
- Administración: `http://localhost:8080/admin`
- Salud: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Administrador inicial de desarrollo

```text
Correo: admin@trastcms.local
Contraseña: ChangeMeNow-2026!
```

Cambie estas variables antes del primer arranque productivo:

```text
TRASTCMS_ADMIN_EMAIL
TRASTCMS_ADMIN_PASSWORD
TRASTCMS_PLUGIN_MASTER_KEY
```

## Desarrollo Frontend Independiente (Recarga en vivo / HMR)

Para evitar recompilar todo el proyecto con `mvn verify` en cada cambio visual de Vue:

1. Inicie el backend de Spring Boot normalmente desde su IDE o mediante Maven (`./mvnw spring-boot:run -Pbackend-only`).
2. En una terminal independiente, ejecute el script de desarrollo:

Linux / macOS / Git Bash:

```bash
./dev.sh
```

PowerShell (Windows):

```powershell
.\dev.ps1
```

Opcionalmente, puede especificar la URL del API y el puerto de desarrollo:

```bash
./dev.sh http://localhost:8080 5173
```

O configurar las variables en su archivo `.env`:

```env
VITE_API_URL=http://localhost:8080
VITE_PORT=5173
```

El servidor Vite (`http://localhost:5173`) actualizará los componentes al instante mediante **Hot Module Replacement (HMR)** y redirigirá las peticiones `/api` y `/actuator` hacia el backend configurado.

## MySQL

En IntelliJ IDEA, agregue estas variables a la configuración de ejecución:

```text
SPRING_PROFILES_ACTIVE=mysql
DATABASE_URL=jdbc:mysql://localhost:3306/trastcms?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
DATABASE_USERNAME=trastcms
DATABASE_PASSWORD=su-contraseña
SESSION_COOKIE_SECURE=false
```

O en una sola línea:

```text
SPRING_PROFILES_ACTIVE=mysql;DATABASE_URL=jdbc:mysql://localhost:3306/trastcms?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false;DATABASE_USERNAME=trastcms;DATABASE_PASSWORD=su-contraseña;SESSION_COOKIE_SECURE=false
```

## Desarrollo con recarga rápida

Backend:

```bash
./mvnw spring-boot:run -Pbackend-only
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Vite utiliza `http://localhost:5173` y reenvía `/api` a Spring Boot.

## Distribución nativa

```bash
./mvnw -Pnative -DskipTests native:compile
```

Los plugins Java instalables después de compilar solo están disponibles en la JVM. En Native Image se utilizan hooks precompilados o plugins externos.

## Estructura

```text
src/main/java/        Backend, seguridad, dominios, plugins y API
src/main/resources/   Configuración, migraciones y temas incorporados
plugins/              Plugins incorporados compilados en el JAR
frontend/             Panel Vue, sitio público, editor y constructor visual
examples/plugins/     Plugins Java y externos de ejemplo
examples/themes/      Tema ZIP de ejemplo
.github/workflows/    CI, seguridad, Docker, native y release
docs/                 Arquitectura y SDKs
```

## Documentación

- [Comienza aquí](docs/GETTING_STARTED.md)
- [Índice de documentación](docs/README.md)
- [Arquitectura](docs/ARCHITECTURE.md)
- [API](docs/API.md)
- [Plugins](docs/PLUGINS.md)
- [Plugins incorporados](docs/BUNDLED_PLUGINS.md)
- [TrastCRM](plugins/trastcrm/README.md)
- [TrastAcademy](plugins/trastacademy/README.md)
- [API de TrastAcademy](docs/TRASTACADEMY_API.md)
- [Temas](docs/THEMES.md)
- [Roadmap](docs/ROADMAP.md)
- [Comunidad](docs/COMMUNITY.md)
- [Guía de estilo](docs/CODE_STYLE.md)
- [Pruebas](docs/TESTING.md)
- [Variables de entorno](docs/ENVIRONMENT_VARIABLES.md)
- [Frontend](docs/FRONTEND_ARCHITECTURE.md)
- [Changelog](CHANGELOG.md)

## Licencia

GNU General Public License v3.0. Consulte [`LICENSE`](LICENSE).


## TrastTravel y Mi Cuenta

- Catálogo de tours, destinos y actividades con búsqueda por fecha y viajeros.
- Itinerarios, incluidos, excluidos, preguntas frecuentes, galería y tarifas por tipo de viajero.
- Salidas con capacidad, cupos retenidos, reservados y bloqueados.
- Calendario administrativo mensual con clientes esperados y enlaces a Google Calendar.
- Reservaciones, viajeros, extras, depósitos, pagos de saldo y cupones procesados por TrastPay.
- Captación automática de contactos, negocios y actividades en TrastCRM.
- Portal `/my-account` para perfil, compras, pagos, viajes, licencias y suscripciones.
- SMTP central cifrado para notificaciones de todos los plugins.
- Datos demostrativos genéricos para probar el flujo turístico.

Documentación: [`docs/TRAVEL_ENGINE.md`](docs/TRAVEL_ENGINE.md), [`docs/ACCOUNT_PORTAL.md`](docs/ACCOUNT_PORTAL.md) y [`docs/PLUGIN_INTEGRATIONS.md`](docs/PLUGIN_INTEGRATIONS.md).

## Plataforma para desarrolladores

TrastCMS incluye dos superficies de integración dentro del mismo JAR:

- **REST API** con OpenAPI en `/v3/api-docs` y Swagger UI en `/swagger-ui/index.html`.
- **MCP Server** por Streamable HTTP en `/mcp`, protegido con un token Bearer independiente.

Configure MCP antes de utilizarlo:

```bash
export TRASTCMS_MCP_ENABLED=true
export TRASTCMS_MCP_TOKEN='genere-un-secreto-largo-y-aleatorio'
```

Documentación del repositorio:

- [`docs/REST_API.md`](docs/REST_API.md)
- [`docs/MCP.md`](docs/MCP.md)
- [`docs/DEVELOPER_PLATFORM.md`](docs/DEVELOPER_PLATFORM.md)

La documentación navegable también está disponible en `/developers` y, para administradores, en **API y MCP** dentro del panel.

## Ecosistema comercial incorporado

TrastCMS incluye tres plugins independientes y activables:

| Plugin | Responsabilidad |
|---|---|
| TrastPay | Carrito, checkout, órdenes, impuestos, cupones, PayPal y pasarelas. |
| TrastStore | Catálogo, variantes, productos digitales e inventario. |
| TrastSaaS | Planes, suscripciones, licencias, activaciones y uso medido. |

TrastStore y TrastSaaS dependen de TrastPay. Los tres se compilan dentro del
mismo JAR y aportan paneles Vue, API REST, bloques del constructor y
herramientas MCP.

Documentación técnica:

- [TrastPay](docs/COMMERCE.md)
- [TrastStore](docs/STORE.md)
- [TrastSaaS](docs/SAAS.md)

### Comercio, SaaS y CRM

TrastCMS incorpora una experiencia comercial y turística integrada con catálogo responsive, ficha de producto, checkout por pasos, portal de licencias, detalle de planes, inventario administrable y paneles tolerantes a fallos parciales. Los temas incluidos aplican tokens y reglas de contraste compatibles con las páginas demo y los bloques del constructor.
