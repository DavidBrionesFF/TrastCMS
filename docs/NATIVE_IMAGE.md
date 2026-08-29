# Compilación nativa de TrastCMS

TrastCMS puede generar un ejecutable autónomo mediante Spring AOT, GraalVM
Native Image y el perfil Maven `native`. El frontend Vue y los plugins
incorporados se compilan dentro del mismo binario.

## Qué incluye el binario

- Spring Boot y API REST.
- Frontend Vue compilado.
- Temas incorporados.
- TrastCRM, TrastPay, TrastStore y TrastSaaS.
- Migraciones Flyway.
- Servidor MCP.
- H2, MySQL y PostgreSQL mediante sus controladores configurados.

Los plugins PF4J instalados dinámicamente como JAR se deshabilitan en una
imagen nativa. Los plugins incorporados y compilados junto al proyecto sí
funcionan, porque forman parte del análisis AOT.

## Requisitos

- GraalVM JDK 25 o superior.
- `native-image` disponible en `PATH`.
- Maven Wrapper incluido en el proyecto.
- En Windows: Visual Studio Build Tools con el entorno de compilación C++.
- Memoria libre recomendada: 8 GB o más.

Compruebe el entorno:

```bash
java -version
native-image --version
```

## Linux y macOS

```bash
./scripts/build-native.sh
```

## Windows PowerShell

Ejecute PowerShell desde **x64 Native Tools Command Prompt for VS**:

```powershell
.\scripts\build-native.ps1
```

## Comando Maven directo

```bash
./mvnw clean -Pnative -DskipTests native:compile
```

El resultado queda en `target/` y el script lo copia también a `dist/`.

## Ejecutar

Linux o macOS:

```bash
./dist/trastcms-3.6.1
```

Windows:

```powershell
.\dist\trastcms-3.6.1.exe
```

Variables mínimas recomendadas:

```text
TRASTCMS_DATA_DIR=./data
TRASTCMS_ADMIN_PASSWORD=CAMBIE_ESTA_CLAVE
TRASTCMS_PLUGIN_MASTER_KEY=CAMBIE_ESTA_CLAVE_LARGA
TRASTCMS_MCP_TOKEN=CAMBIE_ESTE_TOKEN
SESSION_COOKIE_SECURE=true
```

## Workflow de GitHub Actions

`.github/workflows/native.yml` genera artefactos independientes para Linux,
Windows, macOS Intel y macOS ARM. También puede iniciarse manualmente desde
**Actions → Native executables → Run workflow**.

## Solución de problemas

### Un recurso no aparece

Regístrelo en `NativeRuntimeHints` si se obtiene dinámicamente desde el
classpath. Los patrones actuales incluyen `static/**`, `themes/**`,
`db/migration/**` y archivos de configuración.

### Un plugin dinámico no inicia

Es el comportamiento previsto. Una imagen nativa opera con un conjunto
cerrado de clases. Convierta esa integración en un plugin incorporado dentro
de `plugins/<plugin>/src/main/java`.

### La compilación consume demasiada memoria

Cierre IDE y servicios pesados, y configure:

```text
MAVEN_OPTS=-Xmx4g
```
