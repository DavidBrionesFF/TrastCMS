# TrastStore: tienda online

## Propósito

TrastStore aporta el catálogo vendible a TrastPay. Mantiene productos,
variantes, precios, inventario y movimientos, pero delega carrito, checkout,
órdenes y pagos al núcleo comercial.

## Funciones

- productos físicos;
- productos digitales;
- servicios;
- productos externos;
- borradores, publicados y archivados;
- SKU único;
- precio y precio comparativo;
- moneda;
- categoría y marca;
- imagen destacada y galería;
- atributos y SEO en JSON;
- variantes con precio e inventario propios;
- inventario y backorders;
- alertas de existencia baja;
- movimientos de inventario;
- fulfillment automático después de un pago confirmado.

## Dependencia

```text
TrastStore → TrastPay
```

## API pública

```text
GET /api/public/store/products
GET /api/public/store/products/{slug}
```

Parámetros del catálogo:

```text
search
page
size
```

## API administrativa

```text
GET    /api/admin/store/dashboard
GET    /api/admin/store/products
POST   /api/admin/store/products
PUT    /api/admin/store/products/{id}
DELETE /api/admin/store/products/{id}
POST   /api/admin/store/products/{id}/inventory
```

### Crear un producto

```json
{
  "name": "Aplicación empresarial",
  "slug": "aplicacion-empresarial",
  "sku": "APP-001",
  "description": "Implementación completa",
  "shortDescription": "Software e implementación",
  "type": "SERVICE",
  "status": "ACTIVE",
  "price": 1200,
  "compareAtPrice": 1500,
  "currency": "USD",
  "category": "Software",
  "brand": "NaT Technologies",
  "featured": true,
  "trackInventory": false,
  "allowBackorder": true,
  "stockQuantity": 999,
  "lowStockThreshold": 5,
  "gallery": [],
  "attributes": {},
  "seo": {},
  "variants": []
}
```

## Inventario

El endpoint de ajustes admite:

```json
{
  "type": "ADJUSTMENT",
  "quantity": 10,
  "reason": "Compra a proveedor",
  "variantId": null,
  "referenceType": "PURCHASE",
  "referenceId": "COMPRA-100"
}
```

Con `type=SET`, la cantidad representa el saldo final. En otros tipos,
representa el cambio positivo o negativo.

Al confirmarse una orden, TrastPay invoca el fulfillment. TrastStore reduce
la existencia cuando el producto controla inventario y registra un movimiento
`SALE` vinculado con la orden.

## Páginas públicas

```text
/store
/cart
```

La tienda pública incluye búsqueda, tarjetas, precios, disponibilidad y
botones conectados con el carrito.

## Bloques del constructor

```text
plugin:traststore:grid
plugin:traststore:product
```

La ficha individual permite elegir un producto real desde el inspector del
constructor visual.

## MCP

```text
store_inventory_alerts
store_product_lookup
```
