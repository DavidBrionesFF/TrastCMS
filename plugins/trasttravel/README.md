# TrastTravel

Motor turístico incorporado para TrastCMS.

## Incluye en 3.0.1

- Catálogo de tours, destinos y actividades.
- Información estructurada: incluidos, excluidos, itinerario, preguntas frecuentes y galería.
- Tarifas por adulto, niño e infante; precio por persona o grupo.
- Calendario de salidas con capacidad, cupos retenidos, reservados y bloqueados.
- Reservación pública con retención temporal de cupos.
- Servicios adicionales y viajeros.
- Integración con TrastPay mediante `SellableProvider`.
- Confirmación automática después del pago.
- Enlace de una pulsación para agregar la salida a Google Calendar.
- Eventos `travel.*` para CRM, webhooks y automatizaciones.
- Sección `Mis viajes` para el plugin TrastAccount.
- API REST pública, autenticada y administrativa.
- Datos demo para Roatán, Utila y La Ceiba.

## Flujo comercial

1. El cliente busca por destino, actividad, fecha y cantidad de viajeros.
2. Selecciona una salida con cupos.
3. TrastTravel crea una reserva `PENDING_PAYMENT` y retiene cupos.
4. El frontend agrega la reserva al carrito usando `providerKey=travel`.
5. TrastPay procesa cupón, checkout y pasarela.
6. Al confirmarse el pago, `fulfill()` convierte la retención en cupos reservados.
7. El viaje aparece en Mi Cuenta y se publica `travel.booking.confirmed`.

## Dependencias

- Requerida: `trastpay`.
- Integraciones opcionales: `trastcrm`, `trastaccount`, webhooks externos.

Los datos no se eliminan al desactivar el plugin.
