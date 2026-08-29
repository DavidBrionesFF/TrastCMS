# TrastCMS 3.6.1 · Arquitectura Multisite

## Objetivo

Ejecutar varios sitios completamente configurables sobre una sola instancia de TrastCMS, conservando aislamiento lógico de datos, usuarios, configuración, temas y plugins.

## Modelo

### Red

`cms_site` representa un tenant. `cms_site_endpoint` vincula hosts/puertos y `cms_site_membership` concede a una identidad acceso a un sitio con rol independiente.

### Resolución de tenant

`SiteResolutionFilter` resuelve el tenant antes de autenticación mediante:

1. `X-TrastCMS-Site` únicamente en el panel administrativo y `/api/auth/me`, para que la SPA cambie de contexto sin abandonar el panel.
2. `X-Forwarded-Host` / `X-Forwarded-Port` cuando existe reverse proxy.
3. host y puerto de la petición directa.
4. si el host/puerto público no está registrado, la petición falla con 404; no se entrega el contenido de otro tenant.

Las rutas públicas, registro, Mi Cuenta, API móvil y MCP **no aceptan** el header de cambio de tenant: su sitio lo determina el host. Si un mismo host está registrado en varios sitios con puertos diferentes, el puerto debe coincidir; un puerto desconocido falla en lugar de elegir un tenant arbitrariamente.

El contexto queda disponible en `SiteContext` y se limpia al terminar cada request.

### Persistencia

Los agregados que pertenecen a un tenant utilizan `@TenantId`. Hibernate aplica el identificador retornado por `SiteTenantIdentifierResolver` a las operaciones ORM.

Las consultas SQL nativas deben incluir `site_id` explícitamente. Los resolvers nativos de TrastAcademy ya incluyen este predicado.

### Configuración

`SiteSettingService` conserva la tabla histórica pero serializa físicamente la clave como:

```text
{siteId}::{logicalKey}
```

La API sigue trabajando con claves lógicas (`theme.active`, `site.name`, etc.).

Las configuraciones singleton de Travel, Academy, Pay, Translate y SaaS utilizan el UUID del sitio como clave primaria.

### Plugins

`bundled_plugin_state.plugin_id` usa:

```text
{siteId}::{pluginId}
```

Por lo tanto un plugin puede estar habilitado en un sitio y deshabilitado en otro sin reiniciar la aplicación.

### Usuarios

`cms_user` es la identidad global. El rol efectivo proviene de `cms_site_membership`.

Esto permite:

```text
persona@example.com
  Sitio principal → AUTHOR
  Academy      → EDITOR
  SaaS         → CUSTOMER
```

`network_admin=true` identifica administradores de toda la red.

### Temas

Como `theme.active` y el starter content son site-scoped, cada tenant puede activar un tema diferente simultáneamente.

### Provisión y transacciones

La provisión de un blueprint cambia primero `SiteContext` y después abre una transacción `REQUIRES_NEW`. Esto es intencional: Hibernate fija el identificador del tenant al crear el contexto de persistencia. De esta manera, activar un tema, crear starter content o ejecutar `onEnabled()` de un plugin nunca reutiliza una sesión que pertenecía al sitio anterior.

Los trabajos `@Async` usan un `TaskDecorator` que captura y restaura el `SiteContext`, evitando que correo, eventos o webhooks asíncronos caigan al tenant predeterminado.

## Tenants de referencia

El checkout incorpora tenants de referencia para viajes, academia y venta de software. Sus claves y temas sirven para pruebas de migración y demostraciones; cada instalación comunitaria debe registrar sus propios nombres y dominios desde **Red → Sitios**.

- Sitio principal (`primary-site`) → `nexus`.
- Academia (`academy`) → `nexus`.
- Tienda de software (`saas-store`) → `nexus`.

## Blueprints

Al crear un sitio desde **Red → Sitios** se puede provisionar:

- ACADEMY
- SAAS
- TRAVEL
- EMPTY

El blueprint aplica tema, configuración base y selección inicial de plugins.

## Compatibilidad de migración

V21 conserva los datos existentes:

- contenido general/turismo → sitio principal;
- datos de Academy → Academy;
- Store/SaaS → Store + SaaS;
- órdenes digitales históricas se clasifican por provider cuando existe esa información.

Los slugs y claves de negocio que antes eran únicos globalmente pasan a ser únicos por `(site_id, valor)`.

La fuente histórica contenía dos recursos llamados `V17`. Para evitar una colisión de Flyway al empaquetar con Maven, el `pom.xml` conserva como V17 efectiva la migración histórica del núcleo y excluye la V17 duplicada de Newsletter. V21 crea o repara las tablas de Newsletter antes de tenantizarlas, por lo que el plugin no depende de esa migración duplicada.

## Restricciones actuales

- La identidad y contraseña de un correo son globales; la autorización sí es por sitio.
- El backup físico de la base continúa siendo una operación de red porque todos los tenants comparten base.
- El token MCP configurado por entorno continúa siendo una credencial de servidor; la información expuesta por MCP queda resuelta exclusivamente dentro del tenant del host público. El header administrativo de switching no cambia el tenant de MCP.
- Los plugins PF4J externos continúan instalándose a nivel de instancia; su habilitación funcional en sitios debe usar contratos site-aware. Los plugins bundled ya lo hacen.

## Añadir un sitio

1. Panel → Red → Sitios → Nuevo sitio.
2. Seleccionar blueprint.
3. Definir endpoint.
4. Configurar DNS/Nginx.
5. Entrar al tenant mediante el switcher.
6. Ajustar plugins, tema, configuración y usuarios.

No requiere otro backend ni otra base de datos.
## Preservación del sitio productivo al actualizar

El primer arranque de una actualización que incorpora Multisite Foundation lee `TRASTCMS_BASE_URL`. Si apunta al dominio público actual, `MultisiteBootstrapService` lo registra como endpoint del tenant `primary-site` y lo promueve a URL principal cuando no es localhost. Esto permite activar el comportamiento fail-closed de hosts desconocidos sin dejar inaccesible el primer sitio publicado.

## URLs externas y MCP

`SiteUrlService` resuelve la URL canónica del tenant desde `cms_site_endpoint`. MCP deja de depender de una URL global y publica el endpoint del sitio actual. Los callbacks, webhooks y configuraciones de proveedores externos deben usar igualmente el dominio del tenant que origina la operación.

## Fallos durante el alta

El registro de `cms_site`, endpoints y membresía inicial se confirma antes de provisionar porque las tablas tenantizadas tienen claves foráneas hacia `cms_site`. El blueprint se aplica después en una transacción nueva ya fijada al tenant. Si esa fase falla, `SiteRegistryOperations` elimina el registro no provisionado para evitar tenants parciales.
