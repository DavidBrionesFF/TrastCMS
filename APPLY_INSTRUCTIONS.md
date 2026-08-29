# Preparación y actualización a TrastCMS 3.6.1

Este repositorio contiene el árbol completo de TrastCMS 3.6.1. Para instalar o actualizar, utilice siempre un checkout completo de la rama publicada.

## Antes de actualizar una instalación existente

1. Detenga backend y frontend.
2. Haga backup de la base de datos y del directorio `data/`.
3. Confirme que `.env` no esté versionado y conserve una copia privada.
4. Guarde sus temas personalizados y uploads.
5. Configure `TRASTCMS_BASE_URL` con el dominio público actual antes del primer arranque que aplique V21.

## Compilar

Windows:

```powershell
.\scripts\build.ps1
```

Linux o macOS:

```bash
./scripts/build.sh
```

El resultado principal es `target/trastcms-3.6.1.jar`; los scripts también lo copian a `release/` y `dist/`.

## Primer arranque

1. Inicie el JAR con Java 21 o superior.
2. Verifique que Flyway alcance V24.
3. Entre en **Red → Sitios** y revise endpoints, membresías, tema y plugins por tenant.
4. Configure DNS, Nginx y HTTPS siguiendo [`docs/MULTISITE_NGINX.md`](docs/MULTISITE_NGINX.md).
5. Compruebe `/actuator/health`, `/api/public/site` y el header `X-TrastCMS-Site` para cada dominio.

## Validación conocida

El empaquetado y el frontend compilan. Consulte [`docs/TESTING.md`](docs/TESTING.md) para el estado de la suite antes de publicar una release.
