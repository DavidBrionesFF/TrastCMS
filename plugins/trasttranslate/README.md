# TrastTranslate

Plugin incorporado para traducir únicamente el sitio público de TrastCMS.

## Alcance

- Idioma original configurable; por defecto `en`.
- Selector para varios idiomas.
- Posición en barra superior, encabezado, pie, flotante o únicamente como bloque.
- Estilos compacto, desplegable o botones.
- Persistencia de la selección del visitante.
- Bloque `plugin:trasttranslate:selector` para el constructor visual.
- El panel administrativo no carga el traductor.

## Proveedores

- `GOOGLE_ELEMENT`: integración de traducción de página completa mediante el elemento web de Google. Es una integración heredada y su disponibilidad depende de Google.
- `GOOGLE_REDIRECT`: abre la página utilizando la traducción web alojada por Google.

Para un sitio comercial con garantías de disponibilidad, se recomienda evolucionar el proveedor hacia Google Cloud Translation API y caché de traducciones.
