# TrastAccount

Portal de autoservicio para clientes de TrastCMS.

## Principio de integración

TrastAccount no consulta tablas ajenas directamente. Cada plugin aporta una
sección mediante `AccountSectionProvider`.

Integraciones incluidas:

- TrastPay: órdenes, facturas y saldos.
- TrastSaaS: licencias y suscripciones.
- TrastTravel: viajes, pagos, saldos y Google Calendar.

Los usuarios `CUSTOMER` utilizan la misma tabla `cms_user`, pero no tienen
acceso al panel `/admin`. Su superficie es `/my-account`.
