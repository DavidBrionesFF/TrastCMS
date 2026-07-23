# Seguridad

No publique vulnerabilidades como issues públicos. Repórtelas de forma privada al responsable del repositorio mediante GitHub Security Advisories.

## Producción

- Cambie `TRASTCMS_ADMIN_PASSWORD`.
- Cambie `TRASTCMS_PLUGIN_MASTER_KEY` antes de registrar plugins.
- Use PostgreSQL o MySQL con credenciales dedicadas.
- Active HTTPS y `SESSION_COOKIE_SECURE=true`.
- Restrinja `/actuator` mediante red o autenticación.
- Mantenga copias de seguridad de base de datos y `TRASTCMS_DATA_DIR`.

La versión alfa no debe exponerse públicamente sin revisión adicional de seguridad.
