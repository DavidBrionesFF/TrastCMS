# TrastSaaS: suscripciones, licencias y activaciones

## Propósito

TrastSaaS permite vender software y servicios digitales mediante planes. Los
planes se registran como artículos vendibles en TrastPay. Después de un pago
confirmado, el plugin crea la suscripción y emite la licencia.

## Funciones

- productos de software;
- planes mensuales, anuales o de pago único;
- periodos de prueba;
- cantidad máxima de activaciones;
- entitlements por plan;
- suscripciones;
- emisión manual o automática de licencias;
- clave cifrada y hash irreversible para búsqueda;
- validación;
- activación por fingerprint;
- heartbeat;
- desactivación;
- periodo de gracia y expiración;
- ingestión de uso con idempotencia;
- releases por producto, plataforma y canal;
- recuperación segura de licencias mediante número y token de orden;
- API REST para aplicaciones externas;
- herramientas MCP de consulta.

## Dependencia

```text
TrastSaaS → TrastPay
```

## Flujo comercial

```text
Plan SaaS
  → carrito TrastPay
  → checkout
  → pago confirmado
  → suscripción
  → licencia
  → portal de recuperación
  → activación desde la aplicación cliente
```

## Entitlements

Los planes almacenan capacidades como JSON:

```json
{
  "users.max": 20,
  "projects.max": 100,
  "api.enabled": true,
  "reports.advanced": true,
  "storage.gb": 100
}
```

La aplicación cliente debe usar los entitlements devueltos por la API en vez
de codificar el nombre del plan.

## API pública

```text
GET  /api/public/saas/products/{productKey}/plans
POST /api/public/saas/licenses/validate
POST /api/public/saas/licenses/activate
POST /api/public/saas/licenses/heartbeat
POST /api/public/saas/licenses/deactivate
POST /api/public/saas/usage
GET  /api/public/saas/orders/{orderNumber}/claim
POST /api/public/saas/releases/latest
```

### Activar una licencia

```http
POST /api/public/saas/licenses/activate
Content-Type: application/json
```

```json
{
  "licenseKey": "TRAST-TRASTCLOUD-...",
  "productKey": "trast-cloud",
  "fingerprint": "sha256-del-dispositivo",
  "deviceName": "Equipo administrativo",
  "platform": "windows",
  "applicationVersion": "1.0.0"
}
```

Respuesta:

```json
{
  "valid": true,
  "status": "ACTIVE",
  "licenseId": "...",
  "activationId": "...",
  "expiresAt": null,
  "graceEndsAt": null,
  "entitlements": {
    "api.enabled": true
  },
  "message": "Licencia válida",
  "signature": "sha256-del-resultado"
}
```

### Uso medido

```json
{
  "licenseKey": "TRAST-...",
  "productKey": "trast-cloud",
  "meter": "api.requests",
  "quantity": 125,
  "idempotencyKey": "usage-2026-07-22-001",
  "occurredAt": "2026-07-22T20:00:00Z",
  "metadata": {}
}
```

El mismo `idempotencyKey` no crea eventos duplicados.

### Recuperar licencias de una compra

```text
GET /api/public/saas/orders/{orderNumber}/claim?token={orderToken}
```

El token de orden se entrega una sola vez durante el checkout y evita exponer
licencias únicamente con un número de orden predecible.

## API administrativa

```text
GET  /api/admin/saas/dashboard
GET  /api/admin/saas/products
POST /api/admin/saas/products
PUT  /api/admin/saas/products/{id}
GET  /api/admin/saas/plans
POST /api/admin/saas/plans
PUT  /api/admin/saas/plans/{id}
GET  /api/admin/saas/licenses
POST /api/admin/saas/licenses
GET  /api/admin/saas/subscriptions
POST /api/admin/saas/releases
```

## Seguridad

- Las claves completas se almacenan cifradas.
- La búsqueda usa SHA-256, no texto plano.
- El portal administrativo nunca lista la clave completa.
- La clave completa solo se devuelve al emitirla o al reclamar una orden con
  su token público.
- Los fingerprints se almacenan como hash.
- Cada plan limita activaciones.
- Las llamadas de uso son idempotentes.
- Producción debe configurar una clave maestra distinta de la de desarrollo.

## Páginas públicas

```text
/pricing
/saas/claim
```

## Bloques del constructor

```text
plugin:trastsaas:pricing
plugin:trastsaas:license
```

## MCP

```text
saas_subscription_summary
saas_list_licenses
```

Las herramientas no revelan claves completas.
