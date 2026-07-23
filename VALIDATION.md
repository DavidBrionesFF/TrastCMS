# Validación — TrastCMS 2.5.0-alpha.7

## Validaciones completadas

- Parseo de 112 archivos Java con `JavacTask.parse()` sobre Java 21.
- Compilación semántica aislada de las nuevas clases MCP, controlador,
  extensión de TrastCRM y configuración OpenAPI mediante stubs tipados.
- Validación JSON de configuraciones, manifiestos y contenido inicial.
- Validación XML de los POM.
- Validación YAML de configuración y workflows.
- Validación sintáctica de todas las hojas CSS con `tinycss2`.
- Validación sintáctica de 38 unidades TypeScript/Vue con TypeScript 5.8.3.
- Validación semántica aislada de los scripts de DeveloperPortalView y
  DeveloperDocsView bajo `strict: true`.
- Comprobación de que las 30 clases CSS personalizadas utilizadas por las
  páginas demostrativas existen en `styles.css`.
- Validación de la estructura e integridad del ZIP.
- Pruebas de integración agregadas para descubrimiento MCP, autenticación,
  inicialización, catálogo de herramientas, plugins desactivados, errores
  JSON-RPC y solicitudes por lotes.

## Compilación completa intentada

Se intentó ejecutar npm y Maven en el entorno de generación. La red del
contenedor no pudo resolver o conectar con `registry.npmjs.org` ni
`repo.maven.apache.org`, por lo que no fue posible descargar el árbol de
dependencias y completar aquí `npm run build` y `mvn clean verify`.

La compilación definitiva debe ejecutarse en el equipo del usuario:

```powershell
.\mvnw.cmd clean verify
```

No se declara falsamente una compilación completa que el entorno no permitió.
