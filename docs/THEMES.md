# SDK de temas

Los temas son paquetes ZIP instalables sin recompilar TrastCMS.

## Estructura

```text
my-theme.zip
├── theme.json
├── tokens.css
├── screenshot.webp
├── assets/
│   ├── images/
│   └── fonts/
├── templates/
├── components/
├── patterns/
└── languages/
```

## Manifiesto

```json
{
  "id": "nat-corporate",
  "name": "NaT Corporate",
  "description": "Tema corporativo.",
  "version": "1.0.0",
  "author": "NaT Technologies",
  "homepage": "https://nattechnologiesagency.com",
  "license": "GPL-3.0",
  "screenshot": "screenshot.webp",
  "templates": ["default", "full-width", "landing", "blank"],
  "features": ["responsive", "pages", "builder", "dark-mode"],
  "settingsSchema": {
    "primaryColor": {
      "type": "color",
      "label": "Color principal",
      "default": "#6d4aff"
    },
    "fontFamily": {
      "type": "select",
      "label": "Tipografía",
      "default": "outfit",
      "options": ["outfit", "system", "serif"]
    },
    "containerWidth": {
      "type": "number",
      "label": "Ancho máximo",
      "default": 1180,
      "min": 720,
      "max": 1920
    }
  }
}
```

El personalizador se genera automáticamente desde `settingsSchema`. Sus valores se guardan fuera del ZIP.

## Tipografía

El panel y los temas incorporados usan **Outfit Variable**, empaquetada localmente mediante Fontsource. Un tema puede incluir sus propias fuentes dentro de `assets/fonts`.

## Tokens CSS

```css
:root[data-theme="nat-corporate"] {
  --primary: #6d4aff;
  --primary-strong: #5536dc;
  --surface: #ffffff;
  --surface-soft: #f5f6fa;
  --text: #182033;
  --muted: #667085;
  --line: #e5e7ef;
  --public-font-family: "Outfit Variable", Outfit, sans-serif;
}
```

## Seguridad

- Protección contra ZIP Slip.
- Máximo de 500 entradas y 25 MB descomprimidos.
- Lista blanca de extensiones.
- CSS limitado y validado.
- Temas incorporados protegidos.
- No se ejecuta JavaScript remoto desde el manifiesto.

## Contenido inicial del tema

Un tema puede incluir `starter-content.json` junto a `theme.json` y
`tokens.css`:

```json
{
  "version": "1.0.0",
  "homePageSlug": "inicio",
  "pages": [
    {
      "title": "Inicio",
      "slug": "inicio",
      "role": "HOME",
      "showInMenu": true,
      "menuOrder": 10,
      "editorMode": "VISUAL_BUILDER",
      "builder": { "version": 1, "blocks": [], "global": { "containerWidth": 1180, "gap": 24 } }
    }
  ]
}
```

TrastCMS crea únicamente las páginas faltantes y registra la versión aplicada.
No sobrescribe páginas editadas y no vuelve a crear en cada arranque una página
que el administrador decidió eliminar.

Los roles recomendados son `HOME`, `ABOUT`, `SERVICES`, `BLOG` y `CONTACT`.

## Páginas demostrativas profesionales

Los temas incorporados incluyen `starter-content.json` con Inicio, Quiénes somos, Servicios, Blog y Contáctanos. Las páginas se guardan como contenido local `PAGE`; no son plantillas rígidas y pueden modificarse con el constructor visual.

Cuando una versión actualiza el diseño demostrativo, una instalación existente conserva sus cambios. Para cargar deliberadamente la nueva demostración use:

```text
Administración → Temas → Restaurar páginas demo
```

La restauración solo reemplaza páginas cuyo `themeOrigin` coincide con el tema activo. Las páginas creadas por el usuario o por otro tema no se sobrescriben.
