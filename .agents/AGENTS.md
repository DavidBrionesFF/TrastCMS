# TrastCMS — Reglas y Contexto para Agentes de IA

TrastCMS es un CMS headless de código abierto construido con **Spring Boot 3** (Java 21) y **Vue 3** (TypeScript). Este archivo establece las reglas que los agentes deben seguir al trabajar con este repositorio.

---

## Arquitectura General

- **Backend:** Spring Boot 3 + Spring Security (sesión + CSRF) + JPA/Hibernate + Flyway
- **Frontend:** Vue 3 + TypeScript + Vite (admin SPA y vistas públicas)
- **Base de datos:** H2 (desarrollo) / PostgreSQL (producción)
- **Plugins:** módulos Maven incorporados bajo `plugins/`, cada uno con frontend, backend y migraciones propias
- **Temas:** recursos bajo `src/main/resources/themes/<id>/` con `tokens.css`, `starter-content.json` y `assets/`

## Estructura de Directorios Clave

```
src/main/java/com/nattechnologies/trastcms/   # Core del CMS
plugins/<id>/                                  # Plugins incorporados (trasttravel, trastpay, etc.)
frontend/src/                                  # SPA Vue (admin + vistas públicas)
src/main/resources/themes/<id>/                # Temas (CSS, starter-content, assets)
docs/                                          # Documentación técnica detallada
skills/                                        # Skills de agentes IA (también en .agents/skills/)
tools/                                         # Herramientas de desarrollo (wp-media-import.mjs, etc.)
data/                                          # Runtime — NO incluido en git
```

---

## Reglas de Desarrollo

### Backend (Java/Spring)
- **No exponer entidades JPA directamente.** Usar DTOs en `ApiDtos.java`.
- **Rutas de API:** `/api/public/**` (público), `/api/account/**` (autenticado), `/api/admin/**` (admin/editor).
- **Transacciones:** usar `@Transactional` en servicios, no en controladores.
- **Migraciones:** toda DDL va en `src/main/resources/db/migration/` o en `plugins/<id>/src/main/resources/db/migration/` con prefijo `V<versión>__`.
- **No acceder directamente a tablas de otro plugin.** Usar eventos (`PluginEventDispatcher`) o interfaces compartidas.
- **Seguridad:** validar permisos en backend, no solo en frontend.

### Frontend (Vue/TypeScript)
- **Usar el cliente central `api()`** definido en `frontend/src/services/api.ts`. No usar `fetch` directo salvo en scripts de arranque de tema.
- **Autenticación:** basada en sesión (cookie `JSESSIONID` + header `X-XSRF-TOKEN`). El token CSRF se obtiene de `GET /api/auth/csrf`.
- **No crear estilos globales sin el selector de ámbito del tema** (`:root[data-theme="<id>"]`).
- **TypeScript estricto:** no usar `any` salvo en casos extremos justificados.

### Temas
- En multisitio no existe un único tema global: cada tenant define `theme.active`.
- Los assets de cada tema van en `src/main/resources/themes/<id>/assets/`.
- **Todas las imágenes deben ser locales** — no referenciar URLs externas en `tokens.css` ni en `starter-content.json`.
- El contenido inicial se versiona con el campo `"version"` en `starter-content.json`. Incrementar la versión activa el re-apply automático.
- Restaurar contenido: `POST /api/admin/themes/<id>/starter-content/restore`

### Base de Datos / Datos de Demo
- Los datos de demostración están en `plugins/<id>/src/main/resources/demo/`.
- Las imágenes de demostración deben usar recursos locales o rutas `/api/public/media/<uuid>`.
- No subir el directorio `data/` (base de datos runtime, uploads, logs).

---

## Skills Disponibles

Los agentes pueden usar las skills en `.agents/skills/` para tareas especializadas:

| Skill | Cuándo usarla |
|---|---|
| `trastcms-api` | Consumir o ampliar la API REST del CMS |
| `trastcms-mcp` | Interactuar con TrastCMS via Model Context Protocol |
| `trastcms-plugin-builder` | Crear o modificar plugins incorporados |
| `trastcms-theme-builder` | Construir o modificar temas del CMS |

---

## Herramientas de Desarrollo

### Script de importación de medios WordPress
```bash
node tools/wp-media-import.mjs \
  --wp-url https://wordpress.example.com \
  --user admin@trastcms.local \
  --password <contraseña> \
  --replace
```
- Descarga imágenes originales (sin `-scaled`) desde la API REST de WordPress
- Las sube a la biblioteca de medios de TrastCMS (carpeta "WordPress Import")
- Reemplaza URLs externas en los archivos de demostración y `starter-content.json`
- Guarda mapeo en `tools/.wp-import-cache/url-mapping.json`

### Servidor de desarrollo
```bash
# En una terminal: backend
./mvnw spring-boot:run

# En otra terminal: frontend con HMR
cd frontend && npm run dev
# O usar el script unificado:
./dev.sh       # Linux/macOS
./dev.ps1      # Windows PowerShell
```

### Compilación y verificación
```bash
./mvnw clean verify           # Tests + empaquetado
cd frontend && npm run build  # Build de producción del frontend
```

---

## Configuración del Entorno

El archivo `.env` (no subido a git) define variables como:
- `TRASTCMS_ADMIN_EMAIL` y `TRASTCMS_ADMIN_PASSWORD` — credenciales del admin inicial
- `TRASTCMS_DATA_DIR` — directorio de datos runtime (uploads, DB)
- `DATABASE_URL` — conexión a base de datos (H2 en desarrollo)

Ver `.env.example` para la lista completa de variables.

---

## Documentación Detallada

Toda la documentación técnica está en el directorio `docs/`:

- `ARCHITECTURE.md` — arquitectura general
- `API.md` / `REST_API.md` — contratos de la API
- `PLUGINS.md` / `PLUGIN_INTEGRATIONS.md` — desarrollo de plugins
- `THEMES.md` — desarrollo de temas
- `TRAVEL_ENGINE.md` — motor de viajes (TrastTravel)
- `DATABASE_SCHEMA.md` — esquema de base de datos
- `GETTING_STARTED.md` — guía de inicio rápido
- `ENVIRONMENT_VARIABLES.md` — variables de entorno
- `MCP.md` — integración con Model Context Protocol
- `docs/README.md` — índice de la documentación comunitaria

---

## Convenciones de Commit

```
feat: descripción corta del cambio
fix: descripción del bug corregido
docs: actualización de documentación
style: cambios de formato sin lógica
refactor: refactorización sin cambio funcional
chore: tareas de mantenimiento (deps, config)
```

---

## Lo que NO hacer

- ❌ No subir `.env` ni credenciales al repositorio
- ❌ No referenciar imágenes externas en CSS ni JSON de un tema
- ❌ No modificar migraciones Flyway ya ejecutadas (crear nuevas en su lugar)
- ❌ No acceder a tablas de otro plugin directamente desde SQL/JPA
- ❌ No subir el directorio `data/` (base de datos y uploads locales)
- ❌ No subir `tools/.wp-import-cache/` (caché local de importación)
- ❌ No usar `target/` ni `frontend/dist/` en git
