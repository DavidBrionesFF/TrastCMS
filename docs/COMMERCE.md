# TrastPay: comercio y pagos

## Propósito

TrastPay es el núcleo comercial reutilizable de TrastCMS. No administra un
catálogo propio: otros plugins registran artículos vendibles mediante
`SellableProvider`. TrastStore aporta productos y TrastSaaS aporta planes.

El plugin administra:

- carritos persistentes por token;
- clientes comerciales;
- checkout;
- órdenes y líneas históricas;
- pagos e idempotencia;
- cupones;
- impuestos configurables;
- fulfillment hacia el plugin que originó cada artículo;
- pasarelas reemplazables mediante `PaymentGateway`;
- herramientas MCP de consulta.

## Dependencias

TrastPay no depende de TrastStore ni de TrastSaaS. Los plugins que venden
productos deben declarar:

```java
@Override
public List<String> requiredPlugins() {
    return List.of("trastpay");
}
```

El motor impide activar un plugin cuando falta una dependencia y también
impide desactivar TrastPay mientras exista un plugin dependiente activo.

## Contrato de artículos vendibles

```java
public interface SellableProvider {
    String providerKey();
    String pluginId();
    Optional<SellableItem> resolve(String reference);
    List<SellableItem> featured(int limit);
    void fulfill(FulfillmentRequest request);
}
```

La referencia debe ser estable dentro del plugin proveedor. TrastPay guarda
una copia del nombre, precio, moneda y metadatos al crear la orden, evitando
que cambios posteriores alteren el historial comercial.

## Contrato de pasarelas

```java
public interface PaymentGateway {
    String key();
    String name();
    String description();
    boolean enabled();
    boolean supportsRecurring();
    boolean supportsRefunds();
    PaymentResult create(PaymentRequest request);
    PaymentResult refund(String externalReference,
                         BigDecimal amount,
                         String reason);
}
```

La versión incluye:

- `sandbox`: confirma inmediatamente y sirve para pruebas automatizadas;
- `bank_transfer`: deja la orden pendiente y muestra instrucciones;
- `cash`: pago contra entrega;
- `paypal`: crea una orden PayPal Orders v2, redirige al comprador y captura
  el pago en el servidor después de la aprobación.

TrastCMS no almacena números de tarjeta ni CVC. PayPal utiliza OAuth 2.0,
idempotencia, captura del lado servidor y verificación de webhooks.

## Configuración

Ruta administrativa:

```text
Administración → Comercio y pagos → Configuración
```

Opciones:

- moneda predeterminada;
- tasa de impuesto;
- vigencia del carrito;
- compra como invitado;
- solicitud de dirección de envío;
- nombre comercial;
- correo de soporte;
- URL de términos;
- instrucciones bancarias;
- entorno PayPal Sandbox o Live;
- Client ID;
- Client Secret cifrado con la clave maestra del motor de plugins;
- Webhook ID para verificar eventos.

## Configurar PayPal

1. Cree una aplicación REST en PayPal Developer.
2. Copie el Client ID y el Client Secret.
3. En TrastCMS abra **Comercio y pagos → Configuración → PayPal**.
4. Comience en `SANDBOX` y active PayPal.
5. Registre este webhook en la aplicación de PayPal:

```text
https://su-dominio.com/api/public/commerce/paypal/webhook
```

Eventos recomendados:

```text
CHECKOUT.ORDER.APPROVED
PAYMENT.CAPTURE.COMPLETED
PAYMENT.CAPTURE.DENIED
PAYMENT.CAPTURE.REFUNDED
```

El Client Secret nunca se devuelve a la interfaz. Si el campo se deja vacío
al actualizar la configuración, se conserva el secreto cifrado existente.

## API pública

```text
POST   /api/public/commerce/carts
GET    /api/public/commerce/carts/{token}
POST   /api/public/commerce/carts/{token}/items
PUT    /api/public/commerce/carts/{token}/items/{itemId}
DELETE /api/public/commerce/carts/{token}/items
POST   /api/public/commerce/carts/{token}/checkout
GET    /api/public/commerce/gateways
GET    /api/public/commerce/paypal/return
POST   /api/public/commerce/paypal/webhook
```

### Crear un carrito

```bash
curl -X POST \
  'http://localhost:8080/api/public/commerce/carts?currency=USD' \
  -H 'X-XSRF-TOKEN: <token>' \
  -b 'XSRF-TOKEN=<token>'
```

### Agregar un artículo

```json
{
  "providerKey": "store",
  "reference": "<product-id>",
  "quantity": 1,
  "metadata": {}
}
```

### Checkout

```json
{
  "email": "cliente@example.com",
  "name": "Cliente",
  "phone": "+504 9999-9999",
  "gatewayKey": "sandbox",
  "couponCode": null,
  "billing": {},
  "shipping": {},
  "returnUrl": "https://example.com/cart",
  "cancelUrl": "https://example.com/cart",
  "idempotencyKey": "uuid-unico"
}
```

## API administrativa

```text
GET    /api/admin/commerce/dashboard
GET    /api/admin/commerce/orders
GET    /api/admin/commerce/orders/{id}
PUT    /api/admin/commerce/orders/{id}/status
PUT    /api/admin/commerce/payments/{id}/status
GET    /api/admin/commerce/coupons
POST   /api/admin/commerce/coupons
PUT    /api/admin/commerce/coupons/{id}
DELETE /api/admin/commerce/coupons/{id}
GET    /api/admin/commerce/settings
PUT    /api/admin/commerce/settings
```

## Estados

Órdenes:

```text
DRAFT
PENDING_PAYMENT
PAYMENT_PROCESSING
PAID
ON_HOLD
PARTIALLY_REFUNDED
REFUNDED
CANCELLED
FAILED
COMPLETED
```

Pagos:

```text
CREATED
PENDING
SUCCEEDED
FAILED
CANCELLED
REFUNDED
PARTIALLY_REFUNDED
```

## Eventos

```text
commerce.cart.updated
commerce.order.created
commerce.payment.succeeded
```

Los plugins pueden escuchar estos eventos sin depender directamente de las
entidades de TrastPay.

## Bloques del constructor

```text
plugin:trastpay:cart
plugin:trastpay:checkout
```

Ambos bloques utilizan el carrito de la sesión pública y son adaptables a
móvil, tableta y escritorio.

## MCP

```text
commerce_order_summary
commerce_list_orders
```

Son herramientas de lectura. No exponen datos completos de pago ni ejecutan
reembolsos.
