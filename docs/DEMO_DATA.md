# Datos demostrativos por plugin

TrastCMS separa los registros de demostración de la información creada por el
usuario mediante la columna `demo_data`. Esto permite preparar una instalación
para evaluación y, posteriormente, limpiarla sin regenerar toda la base.

## TrastCRM

Genera empresas, contactos, negocios distribuidos en el pipeline, actividades
de seguimiento y un formulario de diagnóstico digital.

```http
POST   /api/admin/crm/demo-data
DELETE /api/admin/crm/demo-data
```

## TrastPay

Genera cupones para probar descuentos y checkout. La configuración fiscal se
conserva porque puede haber sido personalizada por el administrador.

```http
POST   /api/admin/commerce/demo-data
DELETE /api/admin/commerce/demo-data
```

## TrastStore

Genera diez productos variados, inventario, precios comparativos, impuestos,
imágenes y variantes.

```http
POST   /api/admin/store/demo-data
DELETE /api/admin/store/demo-data
```

## TrastSaaS

Genera dos productos de software, planes mensuales, anuales y perpetuos,
suscripciones con distintos estados, licencias y releases.

```http
POST   /api/admin/saas/demo-data
DELETE /api/admin/saas/demo-data
```

## Consideraciones

TrastTravel carga catálogos turísticos site-aware y TrastAcademy prepara cursos, lecciones, cuestionarios y progreso. Algunos tenants de referencia usan snapshots incluidos con sus plugins. Los archivos multimedia de una instalación permanecen en `data/uploads/` y no se versionan en Git.

- La acción de eliminación borra únicamente registros marcados como demo.
- Las páginas, usuarios, medios y configuraciones reales no se eliminan.
- En instalaciones nuevas, los plugins habilitados cargan sus ejemplos de
  forma automática.
- En una instalación productiva puede eliminarse el demo desde cada panel y
  continuar configurando el plugin sin reiniciar Flyway ni la base de datos.
- Cuando el administrador elimina explícitamente el demo, TrastCMS conserva
  esa preferencia y no vuelve a generarlo durante los siguientes reinicios.
- La acción **Generar datos demo** reactiva voluntariamente la carga de
  ejemplos para ese plugin.
- En multisitio, generar o eliminar ejemplos solo afecta al tenant resuelto.
