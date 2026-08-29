# TrastAccount — Portal Mi Cuenta

TrastAccount proporciona un portal público de autoservicio en `/my-account`.
Utiliza la misma entidad `cms_user`, pero asigna el rol `CUSTOMER`, que no tiene
acceso al panel administrativo ni a `/api/admin/**`.

## Funciones

- Registro público de clientes.
- Inicio y cierre de sesión con la autenticación de TrastCMS.
- Edición de nombre, teléfono, biografía, avatar, idioma y zona horaria.
- Cambio de contraseña con verificación de la contraseña actual.
- Panel responsive separado del administrador.
- Secciones aportadas dinámicamente por plugins.

## Contrato de integración

Los plugins implementan `AccountSectionProvider`:

```java
@Component
final class ExampleAccountSection implements AccountSectionProvider {
    @Override public String key() { return "example"; }
    @Override public String pluginId() { return "example-plugin"; }
    @Override public String label() { return "Mis registros"; }
    @Override public AccountSection load(AccountContext context) {
        return new AccountSection(
            key(), pluginId(), label(), "document", 100, "READY",
            "No hay registros.", List.of(), Map.of("total", 0));
    }
}
```

TrastAccount solo carga proveedores cuyo plugin esté habilitado. Si una
integración falla, el resto del portal sigue disponible y la sección muestra
estado `ERROR`.

## Integraciones incorporadas

- **TrastPay:** órdenes y pagos asociados al correo del cliente.
- **TrastTravel:** reservaciones, fechas, estado de pago, saldo y Google Calendar.
- **TrastSaaS:** licencias, suscripciones, planes y vigencia.
- **TrastStore:** sus compras aparecen a través de las órdenes de TrastPay.

## Seguridad

- El rol `CUSTOMER` es redirigido de cualquier ruta `/admin` hacia Mi Cuenta.
- `/api/account/**` exige sesión autenticada.
- `/api/admin/**` exige uno de los roles administrativos.
- Cada consulta de viaje verifica que el correo de la reservación coincida con
  el usuario autenticado.
- Las contraseñas continúan almacenándose con el encoder configurado por
  TrastCMS; nunca se devuelven al navegador.
