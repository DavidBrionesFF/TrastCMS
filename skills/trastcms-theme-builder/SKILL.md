# TrastCMS Theme Builder

## Objetivo

Construir o modificar temas de TrastCMS sin acoplar catálogo, pagos, usuarios ni contenido operativo al tema.

## Procedimiento

1. Inspeccionar `theme.json`, `tokens.css` y `starter-content.json` del tema más cercano.
2. Crear un ID estable en kebab-case bajo `src/main/resources/themes/<id>/`.
3. Definir variables visuales en `tokens.css`; evitar estilos globales sin el selector `:root[data-theme="<id>"]`.
4. Usar bloques del constructor para páginas y contratos `plugin:<plugin>:<block>` para funciones dinámicas.
5. Mantener tours, productos, licencias y reservas en sus plugins; el tema solo representa esos datos.
6. Incluir contenido inicial versionado y establecer `themeOrigin` mediante `ThemeStarterContentService`.
7. Probar navegación entre temas para detectar CSS residual o parpadeos.
8. Validar escritorio, tableta y móvil, incluidos encabezado, menú, formularios, carrito y Mi Cuenta.

## Reglas

- La tipografía se define con `--public-font-family`.
- El ancho se define con `--site-container-width`.
- No sobrescribir páginas de otro tema.
- No usar selectores genéricos como `main`, `form` o `button` sin el ámbito del tema.
- Los assets deben tener respaldo local y trazabilidad de su fuente.
- El ZIP de entrega contiene solo archivos modificados o agregados.

## Comprobación

```bash
./mvnw clean verify
```

Verificar además JSON, enlaces de assets, rutas públicas y cambio de tema sin flash visual.
