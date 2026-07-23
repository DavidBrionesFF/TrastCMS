# Hello Java Plugin

1. Instale primero TrastCMS en el repositorio Maven local:

```bash
mvn -Pbackend-only -DskipTests install
```

2. Compile el plugin:

```bash
cd examples/plugins/hello-java-plugin
mvn clean package
```

3. Desde **Administración → Plugins → Plugins Java**, instale:

```text
target/hello-java-plugin.jar
```

Los plugins Java dinámicos requieren la distribución JVM. Para Native Image utilice el protocolo de plugins externos con webhooks HMAC.
