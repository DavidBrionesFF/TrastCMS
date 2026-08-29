# Documentación de TrastCMS 3.6.1

Este índice separa las guías vigentes del historial de entregas. Salvo que un documento indique lo contrario, la documentación describe el checkout completo de TrastCMS 3.6.1.

## Primeros pasos y operación

- [Instalación y desarrollo](GETTING_STARTED.md)
- [Variables de entorno](ENVIRONMENT_VARIABLES.md)
- [Pruebas y estado conocido](TESTING.md)
- [Compilación nativa](NATIVE_IMAGE.md)
- [Multisitio con Nginx, Certbot y HTTPS](MULTISITE_NGINX.md)
- [Arquitectura multisitio](MULTISITE_ARCHITECTURE.md)

## Arquitectura y contratos

- [Arquitectura general](ARCHITECTURE.md)
- [Arquitectura del frontend](FRONTEND_ARCHITECTURE.md)
- [Esquema de base de datos](DATABASE_SCHEMA.md)
- [REST API y MCP](API.md)
- [REST API](REST_API.md)
- [MCP Server](MCP.md)
- [Plataforma para desarrolladores](DEVELOPER_PLATFORM.md)

## Extensiones

- [SDK de plugins](PLUGINS.md)
- [Plugins incorporados](BUNDLED_PLUGINS.md)
- [Integraciones entre plugins](PLUGIN_INTEGRATIONS.md)
- [SDK de temas](THEMES.md)
- [Datos demostrativos](DEMO_DATA.md)

## Plugins incorporados

- [TrastAccount](ACCOUNT_PORTAL.md)
- [TrastAcademy](TRASTACADEMY_API.md)
- [TrastCRM Reputation](CRM_REPUTATION_AUTOMATION.md)
- [TrastPay](COMMERCE.md)
- [TrastSaaS](SAAS.md)
- [TrastStore](STORE.md)
- [TrastTranslate](TRANSLATION.md)
- [TrastTravel](TRAVEL_ENGINE.md)

Los README específicos de implementación también están dentro de `plugins/<id>/`.

## Temas

La guía comunitaria para crear, empaquetar y mantener temas está en [SDK de temas](THEMES.md). Los temas de referencia incluidos en el checkout conservan su README junto a sus recursos en `src/main/resources/themes/<id>/`; la operación de sitios concretos se documenta fuera del repositorio público.

## Proyecto y comunidad

- [Contribución](../CONTRIBUTING.md)
- [Código de conducta](../CODE_OF_CONDUCT.md)
- [Seguridad](../SECURITY.md)
- [Estilo](CODE_STYLE.md)
- [Comunidad](COMMUNITY.md)
- [Roadmap](ROADMAP.md)
- [Changelog](../CHANGELOG.md)
