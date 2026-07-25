# Aplicación del hotfix 2.7.0-alpha.9.2

Este ZIP no contiene una carpeta contenedora.

Debe extraerse directamente dentro de la raíz del proyecto, en el mismo
nivel donde se encuentra `pom.xml`.

Ejemplo correcto:

```text
TrastCMS-2.0.0-alpha.1/
├── pom.xml
├── src/
├── frontend/
└── plugins/
```

Después de extraer, ejecute:

```powershell
.\VERIFY_PATCH.ps1
.\mvnw.cmd clean verify
```

Si existe una carpeta como esta dentro del proyecto:

```text
TrastCMS-2.7.0-alpha.9.1-functional-ui-mcp-hotfix/
```

puede eliminarla después de aplicar correctamente este overlay. Maven no
compila esa carpeta.
