# TrastCMS 3.6.1 · Multisite con Nginx y Certbot

TrastCMS puede servir varios sitios desde **una sola instancia Spring Boot y una sola base de datos**. El sitio se resuelve con el host y puerto de la petición, y cada tenant conserva su propio contenido, tema, configuración, plugins y membresías de usuario.

> **Actualización desde una instalación anterior a V21:** antes del primer arranque configure `TRASTCMS_BASE_URL` con la URL pública actual, por ejemplo `https://tours.example.com`. El bootstrap registrará ese host como endpoint del tenant principal para evitar que el dominio publicado quede sin resolver.

## 1. Topología recomendada

```text
Internet
   │
   ▼
Nginx :80/:443
   ├── tours.example.com   ─┐
   ├── academy.example.com ─┤
   └── software.example.com ─┘──► 127.0.0.1:8080 · TrastCMS
                                  │
                                  └── PostgreSQL/MySQL/H2
```

Nginx selecciona el bloque `server` por `server_name` y actúa como reverse proxy. TrastCMS necesita recibir el `Host`/`X-Forwarded-Host`, el puerto y el protocolo originales para resolver correctamente el tenant.

## 2. Tenants de referencia

Las migraciones V21 y V23 crean tenants de referencia para comprobar el aislamiento multisitio:

| Sitio | Clave | Host local inicial | Blueprint |
|---|---|---|---|
| Sitio principal | `primary-site` | `localhost:8080` | Empty |
| Academia | `academy` | `academy.localhost:8080` | Academy |
| Tienda de software | `saas-store` | `saas.localhost:8080` | Store + SaaS |

Los dominios se pueden cambiar posteriormente desde **Panel → Red → Sitios**.

## 3. Configuración Nginx por dominios

Un único `upstream` apunta al proceso Spring Boot:

```nginx
upstream trastcms_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}
```

Después, cree un bloque por sitio:

```nginx
server {
    listen 80;
    server_name tours.example.com www.tours.example.com;

    location / {
        proxy_pass http://trastcms_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

server {
    listen 80;
    server_name academy.example.com;

    location / {
        proxy_pass http://trastcms_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}

server {
    listen 80;
    server_name software.example.com;

    location / {
        proxy_pass http://trastcms_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Puede usar `deploy/nginx/trastcms-multisite.conf.example` como base.

## 4. Producción con HTTPS y Certbot

Antes de solicitar certificados:

1. Cree los registros DNS `A` y, si corresponde, `AAAA` de todos los dominios.
2. Confirme que cada dominio resuelve a la IP pública del servidor.
3. Permita tráfico TCP en los puertos `80` y `443`.
4. Instale primero la configuración HTTP de la sección anterior. Let's Encrypt necesita alcanzar el puerto 80 durante la validación inicial.

En Debian o Ubuntu instale Nginx y el complemento oficial de Certbot:

```bash
sudo apt update
sudo apt install nginx certbot python3-certbot-nginx
sudo nginx -t
sudo systemctl reload nginx
```

Solicite un certificado independiente por sitio. Separarlos permite renovar o retirar un dominio sin afectar a los demás:

```bash
sudo certbot --nginx -d tours.example.com -d www.tours.example.com
sudo certbot --nginx -d travel.example.com
sudo certbot --nginx -d academy.example.com
sudo certbot --nginx -d software.example.com
```

Seleccione la redirección automática de HTTP a HTTPS cuando Certbot la ofrezca. El complemento modifica los bloques `server`, instala las rutas del certificado y recarga Nginx. Compruebe el resultado y la renovación automática:

```bash
sudo nginx -t
sudo certbot certificates
systemctl status certbot.timer
sudo certbot renew --dry-run
```

Finalmente, registre cada endpoint en **Panel → Red → Sitios** con esquema `https`, dominio público y puerto `443`. El host configurado en TrastCMS debe coincidir exactamente con uno de los nombres incluidos en el certificado.

### Configuración manual del certificado

Si el complemento de Certbot no puede editar Nginx o utiliza certificados administrados por otro sistema, el patrón equivalente es:

```nginx
server {
    listen 443 ssl http2;
    server_name academy.example.com;

    ssl_certificate     /etc/letsencrypt/live/academy.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/academy.example.com/privkey.pem;

    location / {
        proxy_pass http://trastcms_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Configure en TrastCMS el endpoint del sitio como `https`, host público y puerto `443`.

## 5. Desarrollo local

La forma recomendada es utilizar nombres distintos:

```text
127.0.0.1 tours.localhost
127.0.0.1 academy.localhost
127.0.0.1 saas.localhost
```

En la mayoría de navegadores `*.localhost` resuelve automáticamente a loopback, pero si su entorno no lo hace, añada las entradas anteriores al archivo `hosts`.

Ejemplo:

```text
http://localhost:8080              → Sitio principal
http://academy.localhost:8080      → Academy
http://saas.localhost:8080         → Store + SaaS
```

## 6. Alternativa por puertos

También es posible exponer varios puertos de Nginx y enviarlos al mismo backend:

```nginx
server {
    listen 8101;
    server_name localhost;
    location / {
        proxy_pass http://trastcms_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 8101;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Repita para `8102`, `8103`, etc. y registre esos host/puerto en **Red → Sitios**.

**Preferencia:** use dominios/subdominios distintos. Las cookies HTTP se delimitan por dominio/path y no por puerto, por lo que separar sitios únicamente por puerto no ofrece el mismo aislamiento de sesión que utilizar hosts diferentes.

## 7. Crear un cuarto o quinto sitio

1. Ingrese como administrador de red.
2. Abra **Panel → Red → Sitios**.
3. Pulse **Nuevo sitio**.
4. Defina nombre, clave, blueprint y dominio.
5. TrastCMS provisionará tema, configuración y plugins del blueprint.
6. Cree el registro DNS del nuevo host hacia el servidor.
7. Añada otro bloque `server` de Nginx que apunte al mismo `trastcms_backend`.
8. Ejecute:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

No es necesario iniciar otro JAR ni crear otra base de datos.

## 8. Usuarios por sitio

Los correos representan identidades globales, pero el acceso se concede mediante una membresía por sitio. Después de seleccionar el sitio en el switcher del panel, **Usuarios del sitio** administra únicamente las membresías de ese tenant.

Un mismo correo puede pertenecer a varios sitios con roles diferentes. El administrador de red puede entrar en todos los tenants.

## 9. Plugins y configuración por sitio

En **Red → Sitios → Plugins** puede decidir qué plugins están activos en cada tenant. La configuración de temas, correo, traducción, Commerce/Pay, Travel, Academy y SaaS se guarda en el contexto del sitio seleccionado.

Ejemplo inicial:

```text
Sitio principal: Account + Translate
Academy      : Academy + Pay + Account + Translate
Store + SaaS : Store + SaaS + Pay + Account + Translate + Newsletter
```

## 10. Diagnóstico de resolución

TrastCMS devuelve en las respuestas:

```http
X-TrastCMS-Site: academy
```

Compruebe el tenant sin modificar el frontend:

```bash
curl -I -H 'Host: academy.example.com' http://127.0.0.1:8080/
```

También puede revisar:

```text
GET /api/public/site
```

Para el panel, el header `X-TrastCMS-Site` se usa internamente únicamente en rutas administrativas y `/api/auth/me`, para que un administrador autorizado cambie de tenant sin abandonar la SPA. Las rutas públicas, registro, Mi Cuenta, API móvil y MCP no permiten cambiar tenant con ese header: siempre quedan ligadas al host solicitado.

Un host desconocido devuelve `404 Sitio no encontrado`; TrastCMS no cae silenciosamente en el sitio principal. Si usa el mismo host para varios tenants por puertos, Nginx debe enviar `X-Forwarded-Port` correctamente.

## 11. Seguridad operativa

- Exponga al público Nginx, no el puerto interno `8080`, cuando esté en producción.
- Use HTTPS para cada dominio público.
- Mantenga una copia de seguridad de toda la base: la base es compartida por todos los sitios.
- No reutilice dominios/puertos entre tenants; TrastCMS impone unicidad para los endpoints.
- Revise las membresías antes de entregar roles ADMIN o EDITOR.
- Cambie `TRASTCMS_PLUGIN_MASTER_KEY` y `TRASTCMS_MCP_TOKEN` antes de producción.

## 12. Flujo de DNS

Para cada sitio público:

```text
academy.example.com  A/AAAA  → IP del servidor
software.example.com A/AAAA  → IP del servidor
tours.example.com    A/AAAA  → IP del servidor
```

Todos pueden apuntar a la misma IP. Nginx diferencia el sitio por el nombre solicitado y TrastCMS vuelve a validar ese host contra `cms_site_endpoint`.
## 13. Actualización desde versiones anteriores

Antes de iniciar TrastCMS 3.6.1 sobre datos de una versión anterior:

1. Haga una copia de seguridad de la base de datos y de `data/`.
2. Detenga completamente TrastCMS.
3. Configure la URL pública actual del sitio principal antes del primer arranque:

```bash
export TRASTCMS_BASE_URL=https://tours.example.com
```

En Windows/IntelliJ configure la misma variable en la Run Configuration. El bootstrap conserva también `localhost:8080` para administración local y registra la URL pública si todavía no existe.

4. Actualice el árbol de código al checkout completo de la versión publicada.
5. Elimine `target/` antes de recompilar. Esto es especialmente importante porque versiones históricas del árbol contenían dos recursos Flyway llamados `V17`; el `pom.xml` actual excluye la copia duplicada de Newsletter y V21 repara ese esquema. Un `target/classes` viejo podría conservar el recurso anterior.
6. Ejecute `scripts/build.ps1` en Windows o `./scripts/build.sh` en Linux/macOS.
7. Inicie TrastCMS y confirme que Flyway aplica V21.
8. Entre en **Red → Sitios**, revise los tres tenants y agregue sus dominios productivos.
9. Configure Nginx y pruebe cada dominio antes de cambiar DNS.

No elimine la base ni vuelva a crear el sitio principal. V21 migra los datos históricos y usa ese tenant como destino conservador por defecto cuando no existe una señal inequívoca de Academy o SaaS.

## 14. Webhooks, pagos y URLs externas

Aunque el backend sea único, las URLs públicas deben configurarse con el dominio del tenant correspondiente. Para integraciones de pago, OAuth, callbacks y webhooks use siempre la URL canónica del sitio que originó la operación.

Ejemplo conceptual:

```text
Sitio principal → https://www.example.com/...
Academy       → https://academy.example.com/...
Store + SaaS  → https://software.example.com/...
```

Los webhooks emitidos por TrastCMS incluyen el contexto del sitio en headers internos de integración. No use `X-TrastCMS-Site` desde clientes públicos para intentar cambiar de tenant; la autoridad pública continúa siendo host + puerto.

## 15. MCP por sitio

MCP también se resuelve por dominio. El catálogo de cada tenant anuncia su propia URL canónica:

```text
https://tours.example.com/mcp
https://academy.example.com/mcp
https://software.example.com/mcp
```

Las herramientas consultan los datos del tenant resuelto. En 3.6.1 `TRASTCMS_MCP_TOKEN` sigue siendo una credencial de servidor compartida por la instancia, por lo que debe tratarse como secreto de red; el aislamiento de datos se mantiene por host.

## 16. Alta y provisión segura de sitios

Al crear un sitio desde el panel, TrastCMS registra sitio, endpoints y membresía inicial de forma transaccional. Después ejecuta el blueprint dentro de una transacción nueva con el `SiteContext` del tenant objetivo. Si la provisión del tema o plugins falla, el registro incompleto se compensa para no dejar un tenant fantasma.

Después de crear un sitio:

1. Revise el endpoint y marque **Activo** únicamente cuando el dominio/puerto deba resolver tráfico.
2. Marque un solo endpoint como **Principal**; esa URL se usa como canónica, incluida la URL anunciada por MCP.
3. Configure Nginx/DNS.
4. Entre en el tenant con **Administrar** y complete usuarios, tema y configuración del plugin.
