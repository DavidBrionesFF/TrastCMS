# Plugins incorporados

Los plugins incorporados resuelven el conflicto entre extensibilidad y
GraalVM Native Image: viven en módulos fuente separados, pero se conocen en
tiempo de compilación y se empaquetan en el ejecutable final.

## Contrato

Una extensión implementa:

```java
public interface BundledPlugin {
    String id();
    String name();
    String version();
    boolean enabledByDefault();
    List<String> capabilities();
    List<Map<String, Object>> blocks();
    List<Map<String, Object>> adminMenuItems();
    void onEnabled();
    void onDisabled();
    void onEvent(PluginEvent event);
}
```

El estado se persiste en `bundled_plugin_state`. Desactivar un plugin no elimina
sus tablas ni información.

## Aportes al panel

Los plugins pueden declarar:

- Menús administrativos.
- Rutas Vue conocidas durante el build.
- Bloques del constructor visual.
- Acciones administrativas.
- Eventos y listeners.

El frontend consume el contrato combinado en:

```text
GET /api/admin/plugins/contributions
```

## Distribución

El `build-helper-maven-plugin` incorpora los directorios Java y resources de
cada plugin incluido. El frontend utiliza el alias `@plugins` de Vite y
TypeScript.

TrastCRM sirve como implementación de referencia.

## Opciones dinámicas para bloques

Los campos `select` de un bloque pueden obtener sus opciones desde una API
administrativa del propio plugin:

```json
{
  "type": "select",
  "label": "Formulario",
  "optionsEndpoint": "/api/admin/crm/forms",
  "optionsLabel": "name",
  "optionsValue": "formKey"
}
```

El constructor carga esas opciones únicamente para plugins habilitados. Esto
permite seleccionar recursos reales del plugin sin acoplar el núcleo a sus
entidades ni escribir identificadores manualmente.


## Herramientas MCP

Un plugin incorporado puede registrar herramientas para agentes implementando `McpExtension`. La extensión debe consultar `BundledPluginService.isEnabled(...)` o sobrescribir `enabled()` para que sus herramientas desaparezcan cuando el plugin se desactive. TrastCRM sirve como ejemplo con herramientas de pipeline, leads y envíos.

## Dependencias entre plugins

Un plugin puede declarar dependencias obligatorias:

```java
@Override
public List<String> requiredPlugins() {
    return List.of("trastpay");
}
```

El motor no permite activarlo sin sus dependencias y evita desactivar una
dependencia mientras haya extensiones activas que la necesiten.

## Plugins comerciales incorporados

```text
plugins/trastpay
plugins/traststore
plugins/trastsaas
```

TrastPay proporciona contratos reutilizables:

```text
SellableProvider
PaymentGateway
```

TrastStore y TrastSaaS registran productos sin duplicar carrito, checkout,
órdenes ni pagos.

Documentación:

- `docs/COMMERCE.md`
- `docs/STORE.md`
- `docs/SAAS.md`
