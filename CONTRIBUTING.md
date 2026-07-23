# Contribuir

1. Cree una rama desde la rama de desarrollo.
2. Mantenga los cambios dentro del dominio correspondiente.
3. Ejecute `mvn clean verify`.
4. No incluya credenciales, `data/`, `node_modules/` ni artefactos compilados.
5. Documente cambios de API, migraciones y variables de entorno.
6. Use commits claros, por ejemplo `feat(content): add scheduled publishing`.

Los cambios de esquema deben agregarse como una nueva migración Flyway. Nunca modifique una migración publicada.
