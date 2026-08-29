# tools/

Esta carpeta contiene herramientas de compilación locales al proyecto.
Su contenido **no se sube al repositorio** (está en `.gitignore`).

---

## GraalVM — Compilación nativa

Para compilar TrastCMS como ejecutable nativo sin depender del entorno
del sistema, coloca GraalVM JDK 25+ dentro de `tools/graalvm/`.

### Descarga

Descarga GraalVM Community Edition (gratuita) desde:
👉 https://graalvm.org/downloads/

Elige **GraalVM JDK 25** para tu plataforma:

| Sistema | Archivo a descargar |
|---|---|
| Windows x64 | `graalvm-community-jdk-25_windows-x64_bin.zip` |
| Linux x64 | `graalvm-community-jdk-25_linux-x64_bin.tar.gz` |
| macOS x64 (Intel) | `graalvm-community-jdk-25_macos-x64_bin.tar.gz` |
| macOS ARM64 (Apple Silicon) | `graalvm-community-jdk-25_macos-aarch64_bin.tar.gz` |

### Instalación local

**Windows (PowerShell):**
```powershell
# Crea la carpeta
New-Item -ItemType Directory -Force tools\graalvm

# Descomprime el ZIP de GraalVM dentro de tools\graalvm\
# El resultado debe ser: tools\graalvm\bin\native-image.exe
Expand-Archive graalvm-community-jdk-25_windows-x64_bin.zip tools\graalvm_tmp
Move-Item tools\graalvm_tmp\graalvm-community-openjdk-25\* tools\graalvm\
Remove-Item tools\graalvm_tmp -Recurse
```

**Linux / macOS (bash):**
```bash
mkdir -p tools/graalvm

# Descomprime el tar.gz de GraalVM dentro de tools/graalvm/
tar -xzf graalvm-community-jdk-25_linux-x64_bin.tar.gz --strip-components=1 -C tools/graalvm

# Verifica la instalación
tools/graalvm/bin/native-image --version
```

### Estructura esperada

```
tools/
└── graalvm/
    ├── bin/
    │   ├── java
    │   ├── native-image        ← o native-image.exe en Windows
    │   └── ...
    ├── lib/
    └── ...
```

### Verificar que los scripts lo detectan

```bash
# Linux / macOS
./scripts/build-native.sh --help

# Windows PowerShell
.\scripts\build-native.ps1 -WhatIf

# Windows CMD
scripts\build-native.bat --help
```

Los scripts buscan GraalVM en este orden:
1. `tools/graalvm/` ← esta carpeta
2. Variable `GRAALVM_HOME`
3. Variable `JAVA_HOME` (si apunta a GraalVM)
4. `native-image` en el PATH del sistema
