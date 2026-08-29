# TrastCRM · Automatización de reputación

La automatización, introducida en 3.4.1 y vigente en TrastCMS 3.6.1, conecta TrastCRM con TrastTravel, TrastAcademy y TrastSaaS para convertir eventos reales del cliente en un flujo medible de seguimiento y reputación.

## Flujo

```text
Evento del plugin
    ↓
Contacto CUSTOMER en TrastCRM
    ↓
Solicitud de reputación (deduplicada por origen)
    ↓
Tarea CRM de seguimiento
    ↓
Correo HTML con 1–5 estrellas
    ↓
Landing /review/{token}
    ├─ valoración 1–5
    ├─ comentario privado opcional
    └─ canales públicos configurados
          ↓
TripAdvisor / Google / Facebook / otro
```

La puntuación se guarda primero en TrastCRM. Todos los canales externos activos se muestran independientemente de la puntuación; la aplicación no filtra ni redirige únicamente las valoraciones positivas.

## Disparadores incorporados

| Plugin | Evento | Referencia idempotente |
|---|---|---|
| TrastTravel | `travel.booking.paid` | `bookingId` |
| TrastAcademy | `academy.course.completed` | `enrollmentId` |
| TrastSaaS | `saas.purchase.completed` | `orderId + planId` |

TrastSaaS emite el evento después del fulfillment de la compra, cuando ya se han creado suscripción/licencia. TrastAcademy lo emite una sola vez al pasar la matrícula a `COMPLETED`. Travel lo emite cuando la reserva alcanza estado `PAID`.

## Multisitio

Toda la configuración y los datos se resuelven bajo `SiteContext`:

- solicitudes y valoraciones;
- contactos y actividades CRM;
- configuración de disparadores;
- SMTP;
- nombre del sitio;
- dominio canónico usado en los enlaces del correo;
- enlaces de TripAdvisor, Google, Facebook u otros canales.

Esto permite que cada tenant use mensajes, remitentes y destinos distintos desde una sola instancia.

## Configuración administrativa

Abra **CRM → Reputación** en el tenant correspondiente.

Puede configurar:

- activar o pausar la automatización;
- Travel / Academy / SaaS individualmente;
- días de seguimiento CRM;
- asunto del correo (`{siteName}` y `{sourceLabel}`);
- texto introductorio;
- mensaje de agradecimiento;
- canales externos y URLs;
- reenvío manual de solicitudes fallidas o pendientes.

El correo usa la configuración SMTP del sitio actual. La solicitud solo se marca `SENT` si el intento SMTP fue aceptado; si falla se marca `FAILED` y queda disponible para reenvío.

## Estados

- `PENDING`: creada antes de intentar el correo.
- `SENT`: correo aceptado por SMTP.
- `FAILED`: no se pudo enviar el mensaje o preparar su URL pública.
- `RATED`: el cliente dejó de 1 a 5 estrellas.
- `SHARED`: además abrió uno de los canales externos.
- `CANCELLED`: reservado para cancelación administrativa futura.

Al recibir una valoración, la tarea de seguimiento se completa automáticamente y se añade una nota al historial del contacto.

## API

Administración:

```text
GET  /api/admin/crm/reputation/summary
GET  /api/admin/crm/reputation/requests
GET  /api/admin/crm/reputation/settings
PUT  /api/admin/crm/reputation/settings
POST /api/admin/crm/reputation/requests/{id}/resend
```

Público:

```text
GET  /api/public/crm/reputation/{token}
POST /api/public/crm/reputation/{token}/rating
POST /api/public/crm/reputation/{token}/feedback
GET  /api/public/crm/reputation/{token}/channels/{channelId}
```

La URL pública normal para el usuario es:

```text
/review/{token}
```

## MCP

TrastCRM expone además:

```text
crm_reputation_summary
crm_reputation_requests
```

Estas herramientas respetan el tenant resuelto igual que el resto de MCP en Multisite.

## Eventos adicionales

El módulo publica:

```text
crm.reputation.requested
crm.reputation.rated
crm.reputation.shared
```

Pueden utilizarse por webhooks o extensiones futuras para analítica, alertas o automatizaciones adicionales.
