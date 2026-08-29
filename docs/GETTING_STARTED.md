# Comienza aquí

Guía para nuevos contribuidores y maintainers de TrastCMS.

## Requisitos previos

- JDK 21 (Temurin/OpenJDK recomendado)
- Git
- Maven 3.9+ (o Maven Wrapper incluido)
- Node.js 20+ (opcional; Maven lo descarga automáticamente)
- IDE recomendado: IntelliJ IDEA, VS Code o Eclipse

## Clonar y compilar

```bash
git clone https://github.com/DavidBrionesFF/TrastCMS.git
cd TrastCMS

# Compilar backend y frontend y generar el JAR
./scripts/build.sh

# Ejecutar
java -jar target/trastcms-3.6.1.jar
```

Sitio: `http://localhost:8080`
Administración: `http://localhost:8080/admin`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Credenciales de desarrollo:

```text
Correo: admin@trastcms.local
Contraseña: ChangeMeNow-2026!
```

## Flujo de trabajo

### Rama principal

- `master` contiene la versión estable publicada en este repositorio.
- Las ramas de desarrollo se crean desde `master`.
- Los Pull Requests se abren hacia `master`.

### Convención de commits

```
<tipo>(<scope>): <descripción>
```

Tipos:

- `feat` — nueva funcionalidad
- `fix` — corrección de bug
- `docs` — documentación
- `style` — formato, sin cambio de lógica
- `refactor` — refactorización
- `test` — pruebas
- `chore` — mantenimiento

Ejemplos:

```text
feat(content): add scheduled publishing
fix(media): prevent duplicate uploads on drag-drop
docs(plugins): update PF4J installation steps
```

### Verificación antes de enviar PR

```bash
# Backend + frontend + pruebas (consulte docs/TESTING.md)
./mvnw clean verify

# Solo backend (más rápido)
./mvnw spring-boot:run -Pbackend-only
```

## Estructura del código

```
src/main/java/com/nattechnologies/trastcms/
├── config/          Configuración Spring, seguridad, OpenAPI, Native Image
├── domain/          Entidades JPA y repositorios
│   ├── audit/       Registro de acciones administrativas
│   ├── category/    Taxonomía
│   ├── media/       Biblioteca multimedia
│   ├── plugin/      Plugins externos y bundled
│   ├── post/        Publicaciones, páginas, revisiones
│   ├── setting/     Configuración general y del tema
│   └── user/        Cuentas, roles y credenciales
├── mcp/             Extensiones MCP y servidor Streamable HTTP
├── plugin/          API de plugins, eventos y servicios PF4J
├── service/         Lógica de aplicación
└── web/             Controladores REST, DTOs y API pública
    ├── admin/       Panel administrativo
    ├── auth/        Autenticación
    ├── dto/         Objetos de transferencia
    └── publicapi/   Endpoints públicos

plugins/
├── trastaccount/    Portal Mi Cuenta
├── trastcrm/        CRM incorporado
├── trastpay/        Pasarela de pagos
├── trastsaas/       Gestión de suscripciones
├── traststore/      Catálogo y tienda
└── trasttravel/     Motor turístico

frontend/src/
├── components/      Componentes Vue reutilizables
├── layouts/         AdminLayout y PublicLayout
├── router/          Rutas Vue
├── services/        Cliente HTTP (api.ts)
├── stores/          Estado global Pinia
└── views/           Vistas de administración y públicas
```

## ¿Por dónde empezar?

1. Busca issues etiquetados `good first issue`.
2. Revisa `docs/ROADMAP.md` para ver prioridades.
3. Lee `docs/ARCHITECTURE.md` para entender el modelo modular.
4. Prueba el ejemplo `examples/plugins/hello-java-plugin`.

## Obtener ayuda

- Abre un issue con la etiqueta `question`.
- Consulta `docs/DEVELOPER_PLATFORM.md` para REST y MCP.
- Revisa los issues existentes antes de crear uno nuevo.
