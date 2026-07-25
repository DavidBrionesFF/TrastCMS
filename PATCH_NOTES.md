# TrastCMS 2.7.0-alpha.9.5

Parche incremental funcional y visual para Store, TrastPay, TrastSaaS,
TrastCRM y el tema Midnight.

## Base requerida

Debe aplicarse después de los hotfixes 2.7.0-alpha.9.2, 9.3 y 9.4.

## Inventario

- Agregados: **1**
- Modificados: **29**
- Eliminados: **0**

## Cambios principales

### Store público

- Catálogo de cuatro columnas en escritorio.
- Ficha profesional con galería, variantes, impuestos y disponibilidad.
- Cantidad mediante campo numérico y controles `+` y `−`.
- Descripción, especificaciones y entrega organizadas en pestañas.

### Checkout

Flujo real de tres etapas:

```text
1. Carrito
2. Datos y pago
3. Revisión
```

Incluye cupón, datos del cliente, método de pago, impuestos, revisión final y
explicaciones específicas para suscripciones y licencias.

### Store administrativo

- Productos en cuadrícula.
- Editor mediante drawer lateral.
- Drawer de movimientos de inventario.
- Entradas, salidas, daños, devoluciones y ajustes.
- Historial de movimientos.
- Las salidas y daños ahora disminuyen correctamente la existencia.

### TrastSaaS

- Ruta `/pricing/{productKey}/{planKey}`.
- Detalle de plan sin error 404.
- Diferenciación explícita entre suscripción y licencia perpetua.
- Portal de licencias más claro.
- Carga parcial con `Promise.allSettled`.
- Relaciones JPA de producto, plan y suscripción precargadas.

### TrastCRM

- Carga parcial tolerante a fallos.
- Pipeline seguro cuando una etapa no viene cargada.
- Negocios con etapa, contacto y empresa precargados mediante `@EntityGraph`.

### Midnight

- Contraste corregido en hero, títulos, párrafos, tarjetas, métricas,
  navegación, footer y formularios.

## Instalación

Este ZIP es un overlay plano. Extraiga directamente donde está `pom.xml`.

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\VERIFY_PATCH_2_7_0_ALPHA_9_5.ps1
.\mvnw.cmd clean verify
```

JAR esperado:

```text
target\trastcms-2.7.0-alpha.9.5.jar
```
