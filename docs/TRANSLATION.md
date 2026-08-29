# TrastTranslate

TrastTranslate es un plugin incorporado para traducir el sitio público sin modificar el panel administrativo.

## Principio de arquitectura

La traducción pertenece al plugin, no al tema. Cualquier tema activo reutiliza el mismo selector y la misma configuración. Los plugins funcionales, como TrastTravel, conservan sus procesos propios; TrastTranslate solo traduce el DOM público que renderizan.

## Configuración

Ruta administrativa:

```text
Administración → Traducción web
```

Opciones:

- Activar o desactivar el selector.
- Idioma original; `en` por defecto.
- Idiomas disponibles.
- Proveedor Google.
- Posición: barra superior, encabezado, pie, flotante o solo bloque.
- Estilo: compacto, desplegable o botones.
- Banderas y nombres nativos.
- Persistencia de la selección.
- Rutas excluidas.

## Bloque del constructor

```text
plugin:trasttranslate:selector
```

Este bloque permite insertar el selector en cualquier página sin depender del encabezado del tema.

## API

```http
GET /api/public/translation/settings
GET /api/admin/translation/settings
PUT /api/admin/translation/settings
```

## Google Translate

`GOOGLE_ELEMENT` carga el elemento web de Google únicamente en las rutas públicas permitidas. La selección se conserva mediante `localStorage` y la cookie `googtrans`.

`GOOGLE_REDIRECT` abre la URL actual mediante el traductor web alojado por Google.

El elemento web es una integración heredada. Para un sitio comercial que necesite SLA, glosarios, revisión humana o almacenamiento de traducciones, se debe añadir un proveedor basado en Google Cloud Translation API y una tabla de caché.

## Exclusión del administrador

El plugin no se monta en `/admin`. Las rutas `/admin` y `/api` también figuran como exclusiones predeterminadas.
