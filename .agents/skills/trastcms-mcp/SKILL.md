# TrastCMS MCP

## Objetivo

Interactuar con TrastCMS mediante Model Context Protocol sin eludir permisos, validaciones ni contratos de la API.

## Flujo

1. Consultar las herramientas MCP disponibles y su esquema.
2. Resolver primero el recurso relacionado: usuario, página, tour, salida, producto u orden.
3. Enviar únicamente campos documentados y conservar IDs internos devueltos por el servidor.
4. Para escrituras, explicar la operación y validar moneda, estado, disponibilidad y permisos.
5. Consultar el resultado después de crear o actualizar.
6. Registrar errores de contrato con endpoint, payload sanitizado y respuesta, sin exponer secretos.

## Seguridad

- No enviar tokens, contraseñas, tarjetas o secretos de webhooks en mensajes o logs.
- Respetar los roles ADMIN, EDITOR, AUTHOR y CUSTOMER.
- No ejecutar operaciones financieras irreversibles sin confirmación explícita.
- Usar idempotencia para pagos, reservas y migraciones.

## Casos turísticos

Antes de crear una reserva, consultar tour, salida y cupos. Después, agregar el `TRAVEL_BOOKING` al carrito mediante el proveedor comercial; no crear pagos directamente en las tablas de Travel.
