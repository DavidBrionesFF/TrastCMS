# 📦 Directores de Entregables (`release/`)

Esta carpeta se utiliza como destino centralizado para colocar los binarios y ejecutables compilados de **TrastCMS** para **Linux**, **Windows** y **JAR Multiplataforma**.

---

## 📄 Archivos Generados en Compilación

Tras ejecutar los scripts de compilación, los artefactos finales se guardarán en esta carpeta:

| Sistema Operativo | Tipo de Compilado | Nombre del Archivo | Descripción |
|---|---|---|---|
| 🐧 **Linux x64** | Ejecutable Nativo (GraalVM) | `trastcms-linux-x64` | Binario independiente de alto rendimiento sin requerir Java instalado |
| 🪟 **Windows x64** | Ejecutable Nativo (GraalVM) | `trastcms-windows-x64.exe` | Ejecutable nativo Windows de alto rendimiento (`.exe`) |
| ☕ **Multiplataforma** | JAR ejecutable (Spring Boot) | `trastcms-3.6.1.jar` | Aplicación empaquetada compatible con Java 21 o superior |

---

## 🛠️ Cómo Compilar Localmente

### 🪟 Compilar para Windows (Nativo .exe)
```powershell
.\scripts\build-native.ps1
```
El ejecutable `trastcms-windows-x64.exe` y su checksum `.sha256` se colocarán en `release/`.

### 🐧 Compilar para Linux (Nativo Linux)
```bash
./scripts/build-native.sh
```
El archivo `trastcms-linux-x64` y su checksum `.sha256` se colocarán en `release/`.

### ☕ Compilar JAR Standalone
```powershell
# Windows
.\scripts\build.ps1

# Linux / macOS
./scripts/build.sh
```
El JAR final se copiará a `release/trastcms-3.6.1.jar`.
