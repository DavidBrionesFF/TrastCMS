# TrastCRM

TrastCRM es el primer **plugin incorporado** de TrastCMS. Su código permanece
separado del núcleo dentro de `plugins/trastcrm`, pero Maven lo incorpora en el
mismo JAR y en las compilaciones Native Image.

## Capacidades

- Contactos y estados comerciales.
- Empresas y relaciones con contactos.
- Pipeline configurable.
- Negocios y valor proyectado.
- Actividades, llamadas, reuniones, correos, tareas y notas.
- Formularios dinámicos.
- Captura pública de formularios.
- Creación automática de leads.
- Envíos con estados `NEW`, `READ`, `ARCHIVED` y `SPAM`.
- Bloque `plugin:trastcrm:form` para el constructor visual.
- Eventos internos `crm.*` para otras extensiones.
- Automatización de reputación conectada con Travel, Academy y SaaS.
- Correo HTML de 1–5 estrellas, comentario privado y canales públicos configurables.
- Tareas automáticas y trazabilidad de valoración/canal por contacto.
- Herramientas MCP `crm_reputation_summary` y `crm_reputation_requests`.

## Activación

El plugin está activo por defecto. Puede administrarse desde:

```text
Administración → Plugins → Incluidos
```

Al desactivarlo:

- Sus datos permanecen en la base de datos.
- Las rutas administrativas y públicas dejan de responder.
- Su menú y bloques dejan de publicarse como contribuciones.

## Estructura

```text
plugins/trastcrm/
├── frontend/
│   ├── CrmView.vue
│   └── CrmFormBlock.vue
└── src/main/
    ├── java/com/nattechnologies/trastcms/plugins/crm/
    └── resources/db/migration/
```

## Formularios en páginas

1. Abra **CRM → Formularios**.
2. Cree o edite un formulario.
3. Abra una página en el constructor visual.
4. Agregue el bloque **Formulario CRM**.
5. Seleccione el formulario disponible en la lista cargada dinámicamente por el plugin.

El formulario se renderiza desde el plugin y sus envíos se almacenan en las
tablas `crm_form` y `crm_submission`.


## Reputación y experiencia del cliente

Desde **CRM → Reputación** cada sitio puede activar sus propios disparadores, textos de correo, plazo de seguimiento y enlaces a TripAdvisor, Google, Facebook u otros canales.

Los eventos incorporados son `travel.booking.paid`, `academy.course.completed` y `saas.purchase.completed`. Cada evento se deduplica por su referencia de negocio, convierte al comprador/estudiante/viajero en cliente CRM, crea una tarea y envía una solicitud de valoración.

La puntuación se registra en TrastCMS antes de ofrecer los canales públicos. Todos los canales activos se muestran para cualquier puntuación. La guía completa está en `docs/CRM_REPUTATION_AUTOMATION.md`.
