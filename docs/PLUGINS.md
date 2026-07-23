# SDK de plugins

TrastCMS combina hooks, PF4J y webhooks para cubrir distintos niveles de extensión.

## 1. Hooks Spring

```java
@Component
public final class SeoHook implements ContentHook {
    @Override public int order() { return 20; }
    @Override public void beforeSave(Post post, HookContext context) {
        // Validar o enriquecer contenido.
    }
}
```

Se incluyen en el build y funcionan en JVM y Native Image.

## 2. Plugins Java dinámicos

La distribución JVM usa PF4J. Un JAR puede declarar:

- `capabilities()`
- `blocks()`
- `adminMenuItems()`
- `adminActions()`
- `handleAdminAction()`
- `onEvent()`

Ejemplo mínimo:

```java
@Extension
public final class MyExtension implements TrastCmsExtension {
    @Override
    public List<String> capabilities() {
        return List.of("builder.blocks.register", "admin.menu.register");
    }

    @Override
    public List<Map<String,Object>> blocks() {
        return List.of(Map.of(
            "type", "pricing-card",
            "label", "Tarjeta de precio",
            "category", "Comercio",
            "template", "<article><h3>{{title}}</h3><strong>{{price}}</strong></article>",
            "schema", Map.of(
                "title", Map.of("type", "text", "default", "Plan Pro"),
                "price", Map.of("type", "text", "default", "L. 499"))));
    }
}
```

Los valores insertados en una plantilla de bloque se escapan. El HTML resultante vuelve a sanitizarse cuando se guarda la página.

### Página administrativa declarativa

```java
@Override
public List<Map<String,Object>> adminMenuItems() {
    return List.of(Map.of(
        "id", "my-settings",
        "label", "Mi plugin",
        "icon", "plugin",
        "description", "Configuración del plugin",
        "sections", List.of(
            Map.of(
                "id", "settings",
                "type", "form",
                "title", "Opciones",
                "action", "my-plugin.save",
                "fields", List.of(
                    Map.of("name", "apiKey", "label", "API key", "type", "text"))))));
}

@Override
public List<String> adminActions() {
    return List.of("my-plugin.save");
}

@Override
public Map<String,Object> handleAdminAction(String action, Map<String,Object> input) {
    return Map.of("message", "Guardado", "values", input);
}
```

Tipos de sección soportados:

- `notice`
- `stats`
- `links`
- `form`

Tipos de campo:

- `text`
- `textarea`
- `number`
- `color`
- `boolean`
- `select`

Las acciones se ejecutan mediante una ruta administrativa autenticada y quedan en auditoría.

### Manifiesto PF4J

El JAR debe incluir:

```text
Plugin-Id
Plugin-Class
Plugin-Version
Plugin-Provider
Plugin-Description
Plugin-Requires
```

Proyecto completo: `examples/plugins/hello-java-plugin`.

## 3. Plugins externos HMAC

Funcionan con JVM y Native Image.

```text
POST {baseUrl}/trastcms/events
Content-Type: application/json
X-TrastCMS-Event: content.published
X-TrastCMS-Signature: sha256=<firma>
```

La firma es `HMAC-SHA256(cuerpo JSON exacto, secreto compartido)`.

Desde el panel se administran URL base, salud, eventos, permisos, descripción, autor y estado. Los secretos se cifran antes de persistirlos.

## Seguridad

- Un plugin Java es código confiable y puede afectar el proceso completo.
- Revise origen, licencia y checksum antes de instalarlo.
- Solicite el mínimo de capacidades.
- Los plugins externos deben validar cada firma HMAC y aplicar idempotencia.

## Native Image

GraalVM utiliza un mundo cerrado. Los JAR instalados posteriormente no forman parte del ejecutable nativo. Use hooks precompilados o plugins externos.

## Plugins incorporados

Además de PF4J y webhooks, TrastCMS admite plugins incorporados. Se mantienen
separados en `plugins/<id>` pero se compilan dentro de la distribución. Esto
permite activación/desactivación y aislamiento funcional sin romper el modelo
de mundo cerrado de GraalVM.

Consulte [BUNDLED_PLUGINS.md](BUNDLED_PLUGINS.md) y el plugin de referencia
[TrastCRM](../plugins/trastcrm/README.md).
