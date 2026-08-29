# Integraciones entre plugins en TrastCMS 3.6.1

## Dependencias obligatorias y opcionales

`BundledPlugin` distingue dos relaciones:

```java
List<String> requiredPlugins();
List<String> optionalPlugins();
```

- Una dependencia requerida debe estar activa antes de habilitar el plugin.
- Un plugin requerido no puede desactivarse mientras otro plugin activo dependa
  de él.
- Una integración opcional mejora la experiencia, pero su ausencia no impide
  utilizar el módulo principal.

El centro de plugins muestra ambas listas y el estado actual de cada conexión.

## Mapa incorporado

| Plugin | Requiere | Integraciones opcionales |
|---|---|---|
| TrastTravel | TrastPay | TrastCRM, TrastAccount |
| TrastAccount | — | TrastPay, TrastStore, TrastSaaS, TrastTravel |
| TrastStore | TrastPay | TrastAccount |
| TrastSaaS | TrastPay | TrastAccount |
| TrastCRM | — | TrastTravel |
| TrastPay | — | TrastAccount, TrastStore, TrastSaaS, TrastTravel |
| TrastTranslate | — | Todos los temas y bloques públicos |

## Contratos comunes

### `SellableProvider`

Permite que Store, SaaS y Travel utilicen el mismo carrito y checkout. En 3.0,
`FulfillmentRequest` incluye subtotal de línea, descuento asignado, total real
pagado y cupón. Esto permite conciliar cupones y pagos parciales en el dominio
que originó la venta.

### `AccountSectionProvider`

Permite que cada plugin contribuya datos a Mi Cuenta sin que TrastAccount lea
sus tablas internas.

### `PluginEvent`

Permite integraciones no acopladas. TrastTravel publica eventos y TrastCRM los
consume para crear o actualizar contactos, negocios y actividades.

### `PlatformMailService`

Servicio SMTP común para todos los plugins. La contraseña se cifra con
`PluginSecretCipher`, no se devuelve por API y puede verificarse mediante un
correo de prueba desde **Configuración → Correo electrónico**.

## Reglas de diseño

1. Un plugin no debe importar entidades JPA de otro plugin para modificar datos.
2. Las operaciones comerciales se realizan mediante contratos o eventos.
3. Desactivar un plugin no elimina su información.
4. Las integraciones opcionales deben degradarse con seguridad.
5. Los datos calculados por el backend prevalecen sobre metadatos enviados por
   el cliente.
6. Los clientes `CUSTOMER` nunca reciben capacidades administrativas.


### `TrastTranslate`

TrastTranslate se integra en `PublicLayout` y en el constructor mediante el bloque `plugin:trasttranslate:selector`. No modifica las entidades de Travel, Store, SaaS o Account; traduce la interfaz pública después del renderizado.
