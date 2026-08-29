# TrastCMS REST API

## Objetivo

Consumir o ampliar la API REST de TrastCMS de forma consistente con OpenAPI y la arquitectura de plugins.

## Convenciones

- Público: `/api/public/...`
- Cuenta autenticada: `/api/account/...`
- Administración: `/api/admin/...`
- Respuestas y errores en JSON.
- Fechas en ISO 8601.
- Monedas en ISO 4217.
- IDs como cadenas opacas.

## Procedimiento de integración

1. Revisar `/swagger-ui.html` y `/v3/api-docs` antes de implementar.
2. Crear DTOs explícitos; no exponer entidades JPA.
3. Validar entrada con Bean Validation y reglas de dominio.
4. Aplicar autorización por superficie y no solo por interfaz gráfica.
5. Documentar ejemplos, estados y errores.
6. Publicar eventos después de confirmar la transacción.
7. Añadir pruebas de contratos y casos no autorizados.

## Pagos y webhooks

- Guardar referencia externa y clave de idempotencia.
- Verificar firma sobre el cuerpo original.
- Comparar firmas en tiempo constante.
- Aceptar únicamente timestamps dentro de la tolerancia configurada.
- Responder 2xx rápidamente y procesar trabajo pesado fuera del endpoint.

## Consumo desde Vue

Usar el cliente central `api()` para conservar manejo uniforme de credenciales, errores y JSON. No usar `fetch` directo salvo en el prearranque del tema o integraciones que lo requieran explícitamente.
