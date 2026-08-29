# Informe histórico de mejoras — TrastCMS 2.0.0-alpha.1

> Documento histórico. Describe el estado de la versión 2.0.0-alpha.1 y no sustituye la documentación vigente de TrastCMS 3.6.1. Consulte [`docs/README.md`](docs/README.md) para las guías actuales.

Este documento detalla todas las correcciones, optimizaciones visuales, rediseño de paneles administrativos, portal de cliente, tabla comparativa de precios y explorador de API en TrastCMS.

---

## 🏷️ 1. Correcciones en la Página de Precios (`/pricing`)

- **Separación de Botones (`.pricing-card__actions`)**:
  - Se corrigió el margen entre los botones `Ver detalles` y `Comprar licencia` / `Suscribirme`, agregando un espaciado vertical limpio (`gap: 0.75rem`) para evitar que aparezcan pegados.
- **Tabla de Comparación Detallada de Características (`.comparison-matrix-table`)**:
  - Se creó una sección de matriz comparativa que contrasta las funciones entre planes (*Starter*, *Pro*, *Business*) dividida por categorías:
    1. **Capacidad y Límite de Uso**: Usuarios incluidos, Proyectos activos, Almacenamiento (GB), Dispositivos / Activaciones.
    2. **Funcionalidades e Integraciones**: Acceso a la API REST, Reportes avanzados, Marca blanca, Modo offline.
    3. **Prueba, Soporte y Garantía**: Días de prueba gratis y nivel de soporte técnico.
- **Separación de Chips en Tarjetas (`.trial-meta-chips`)**:
  - Se solucionó el error donde los días de prueba y el número de activaciones aparecían pegados (`14 dias de prueba2 activaciones`).
  - Ahora se presentan en una cápsula con iconos independientes y espaciado claro: `⏳ 14 días de prueba` y `💻 2 activaciones`.
- **Iconografía en Características (Entitlements)**:
  - Características deshabilitadas en `false` o `No` se muestran con cruz gris `✗` en lugar de check mark verde.

---

## ⚡ 2. Explorador Interactivo de API & SDK SaaS (`SaasView.vue`)

- **Navegador Interactivo de Endpoints REST**:
  - `POST /api/public/saas/licenses/activate` (Activación en dispositivo)
  - `POST /api/public/saas/licenses/validate` (Validación periódica / Heartbeat)
  - `GET /api/public/saas/orders/{order}/claim` (Consulta de licencias cliente)
  - `POST /api/public/saas/licenses/revoke` (Desvinculación y liberación de cupo)
- **Generador de Código Multilenguaje**:
  - Generación automática de fragmentos de código listos para copiar en `cURL`, `JavaScript (Fetch)`, `Python (requests)` y `Java (HttpClient)`.
- **Tabla de Especificaciones de Parámetros**:
  - Detalles de encabezados, campos requeridos, tipos de datos y respuestas HTTP (`200 OK`, `403 Forbidden`, `404 Not Found`).

---

## 🔑 3. Portal del Cliente y Licencias (`/saas/claim`)

- **Corrección de colapso de texto vertical**:
  - Se solucionó el problema donde la barra lateral del portal reducía el ancho provocando que el texto se envolviera palabra por palabra.
  - Se configuró la grilla principal (`.license-portal__grid`) con `grid-template-columns: minmax(0, 1fr) minmax(320px, 380px)`.
- **Grilla de Pasos Explicativos de 3 Tarjetas (`.license-order-explainer-grid`)**:
  - Tarjetas con insignias `01`, `02` y `03`.
- **Caja de Clave de Licencia (`.key-code-wrap`)**:
  - Formato monoespaciado oscuro con resaltado en cian y botón directo `📋 Copiar clave`.

---

## 📥 4. Productos Digitales vs Físicos y Carga de Archivos Descargables

- **Selector Inteligente de Tipo de Producto (`StoreView.vue`)**:
  - **Físico (📦)**: Habilita control de existencias, alerta de inventario bajo y peso en gramos.
  - **Digital (📥)**: Oculta el inventario físico e integra la sección **"📥 Configuración de Producto Digital & Descargable"**.
- **Gestión y Carga de Archivos Descargables**:
  - Permite ingresar la URL o presionar `📁 Subir archivo` para subir archivos comprimidos (ZIP), ejecutables, manuales o PDFs mediante TrastCMS Media Manager.

---

## 📐 5. Entorno de Desarrollo Frontend Independiente (HMR)
- Scripts de desarrollo: `./dev.sh` y `.\dev.ps1`.
- Compilación verificada con cero errores en Vite v7.3.6.

---

*TrastCMS 2.0.0-alpha.1 - Precios, Matriz Comparativa, Explorador API, Licencias y Módulo Digital*
