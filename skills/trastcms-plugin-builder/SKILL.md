# TrastCMS Plugin Builder

## Objetivo

Crear plugins incorporados o externos compatibles con Spring Boot, Vue, Flyway, REST, eventos y el centro de plugins.

## Estructura recomendada

```text
plugins/<id>/
├── frontend/
├── src/main/java/.../<plugin>/
├── src/main/resources/db/migration/
├── src/main/resources/plugin.json
└── src/test/
```

## Procedimiento

1. Definir responsabilidad única y contratos con otros plugins.
2. Registrar dependencias obligatorias e integraciones opcionales.
3. Diseñar entidades y migraciones Flyway sin tocar tablas de otro plugin.
4. Exponer servicios mediante interfaces compartidas, eventos o proveedores; nunca consultar tablas ajenas directamente.
5. Implementar API pública, de cuenta y administrativa con autorización separada.
6. Registrar rutas Vue, bloques del constructor y navegación administrativa.
7. Emitir eventos con nombres `<plugin>.<aggregate>.<action>` y datos suficientes para webhooks.
8. Añadir pruebas unitarias, integración y seguridad.

## Integraciones comunes

- `SellableProvider` para carrito y TrastPay.
- `AccountSectionProvider` para Mi Cuenta.
- `PluginEventDispatcher` para CRM, correo y webhooks.
- Biblioteca de medios para imágenes y documentos.
- Servicio de correo central para notificaciones.

## Reglas transaccionales

- Validar disponibilidad en backend.
- Usar bloqueos cuando exista inventario o cupos.
- Aplicar idempotencia en callbacks y webhooks.
- No confirmar pagos por una redirección del navegador.
- Desactivar el plugin no elimina sus datos.
