# TrastTravel Engine

TrastTravel es el motor turístico incorporado de TrastCMS. Se compila dentro del
JAR principal, conserva sus datos al desactivarse y utiliza los contratos comunes
de TrastCMS para pagos, Mi Cuenta, CRM, temas, constructor visual, eventos y API.

## Módulos funcionales

### Catálogo

- Destinos con país, región, ciudad, coordenadas, imagen y contenido SEO-ready.
- Actividades reutilizables para clasificar tours.
- Tours con descripción, incluidos, excluidos, galería, itinerario y preguntas frecuentes.
- Duración, dificultad, edades y tamaños mínimo/máximo del grupo.
- Tarifas por adulto, niño e infante o precio por grupo.
- Depósito configurable entre 1 % y 100 %.
- Servicios adicionales por reservación, persona o unidad.

### Disponibilidad

Cada salida mantiene por separado:

- capacidad total;
- cupos confirmados;
- cupos retenidos durante checkout;
- cupos bloqueados por operación;
- cupos disponibles;
- precio especial por tipo de viajero;
- estado operativo y punto de encuentro.

Las operaciones de retención, expiración y confirmación bloquean la salida con
`PESSIMISTIC_WRITE` para impedir sobreventa cuando dos clientes intentan comprar
los últimos cupos simultáneamente.

### Reservación y checkout

1. El visitante filtra por destino, actividad, fecha y viajeros.
2. Selecciona una salida y revisa disponibilidad.
3. TrastTravel calcula tarifas, extras, impuesto y depósito.
4. Se crea una reservación `PENDING_PAYMENT` y los cupos quedan retenidos.
5. La reservación se agrega a TrastPay como `providerKey=travel`.
6. TrastPay aplica cupón, crea la orden y procesa la pasarela.
7. `TravelSellableProvider.fulfill()` confirma la reservación y convierte la
   retención en cupos reservados.
8. Los pagos parciales dejan un saldo disponible para pagar desde la ficha de la
   reservación o desde Mi Cuenta.

Los metadatos calculados por el proveedor son autoritativos; el navegador no
puede sobrescribir importes, impuestos ni referencias de cumplimiento.

### Calendario de administración

La vista mensual muestra:

- salidas por día;
- hora y tour;
- cupos libres;
- clientes esperados;
- cantidad de viajeros;
- código de reservación;
- enlace de una pulsación para Google Calendar.

La integración incluida genera enlaces compatibles con Google Calendar sin
requerir credenciales OAuth. El campo `googleCalendarId` queda disponible para
una integración API bidireccional posterior.

### Notificaciones

TrastTravel reutiliza `PlatformMailService` y la configuración SMTP central de
TrastCMS. Envía:

- recepción de reservación al viajero;
- confirmación del primer pago;
- recepción de pagos de saldo;
- copia operativa al correo configurado en TrastTravel.

Los fallos SMTP se registran sin revertir una reservación o un pago válidos.

## Integraciones

| Plugin o servicio | Integración |
|---|---|
| TrastPay | carrito, cupones, checkout, pasarelas, órdenes y fulfillment |
| TrastCRM | contacto, negocio y actividad creados mediante eventos `travel.*` |
| TrastAccount | sección Mis viajes, estados, pagos, saldos y calendario |
| Constructor visual | buscador, catálogo, destinos y panel de reservación |
| Temas | Componentes públicos compatibles con los temas comunitarios |
| SMTP | servicio compartido `PlatformMailService` |
| Webhooks | eventos publicados por el dispatcher de plugins |

## API REST

### Pública

```text
GET  /api/public/travel/search
GET  /api/public/travel/destinations
GET  /api/public/travel/activities
GET  /api/public/travel/extras
GET  /api/public/travel/tours/{slug}
GET  /api/public/travel/tours/{slug}/departures
POST /api/public/travel/bookings
GET  /api/public/travel/bookings/{code}?email=...
```

### Mi Cuenta

```text
GET  /api/account/travel/bookings
GET  /api/account/travel/bookings/{id-or-code}
POST /api/account/travel/bookings/{id}/cancel
```

### Administración

El prefijo `/api/admin/travel` incluye dashboard, destinos, actividades, extras,
tours, salidas, reservaciones, configuración y datos demo.

## Eventos

Eventos principales:

```text
travel.tour.created
travel.tour.updated
travel.departure.created
travel.departure.updated
travel.booking.created
travel.booking.confirmed
travel.payment.completed
travel.booking.paid
travel.booking.cancelled
```

## Datos demo

Desde **Tours y reservaciones → Resumen** se pueden cargar o retirar únicamente
los registros demostrativos. Se incluyen destinos, actividades, extras, cuatro
tours y salidas futuras de Roatán, Utila y La Ceiba.

Los datos demo están marcados con `demo_data`; la limpieza no afecta registros
reales.
