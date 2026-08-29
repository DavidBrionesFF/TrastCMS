# Contribuir

¡Gracias por tu interés en contribuir a TrastCMS! Esta guía te ayudará a enviar cambios de forma efectiva.

## Antes de empezar

1. Lee [`docs/GETTING_STARTED.md`](docs/GETTING_STARTED.md) para configurar tu entorno.
2. Revisa [`docs/COMMUNITY.md`](docs/COMMUNITY.md) para entender el proceso de contribución.
3. Busca issues etiquetados `good first issue` o `help wanted`.
4. Para cambios grandes, abre un issue primero para discutir la propuesta.

## Flujo de trabajo

### 1. Ramas

Crea tu rama desde `master`, la rama predeterminada actual de GitHub:

```bash
git checkout -b feat/mi-nueva-funcionalidad
```

Convenciones de nombre:

- `feat/` — nueva funcionalidad
- `fix/` — corrección de bug
- `docs/` — documentación
- `refactor/` — refactorización sin cambio de comportamiento
- `test/` — pruebas
- `chore/` — mantenimiento

### 2. Desarrollo

```bash
# Backend + frontend + pruebas
./mvnw clean verify

# Solo backend (más rápido)
./mvnw spring-boot:run -Pbackend-only
```

Revise el [estado conocido de la suite](docs/TESTING.md) y documente por separado cualquier fallo nuevo frente a la deuda heredada de 3.6.1.

Frontend independiente:

```bash
cd frontend
npm install
npm run dev
```

### 3. Commits

Usa [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>(<scope>): <descripción>
```

Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

Ejemplos:

```text
feat(content): add scheduled publishing
fix(media): prevent duplicate uploads on drag-drop
docs(plugins): update PF4J installation steps
```

### 4. Pull Request

- Describe el problema resuelto y la decisión técnica.
- Marca el tipo de cambio: funcionalidad, corrección, seguridad, documentación o migración.
- Verifica que `./mvnw clean verify` pasa.
- No incluyas secretos ni datos de ejecución.
- Actualiza la documentación si cambias API, variables de entorno o migraciones.

## Estándares

- Sigue [`docs/CODE_STYLE.md`](docs/CODE_STYLE.md).
- Escribe pruebas para servicios y controladores públicos.
- Usa migraciones Flyway para cambios de esquema; nunca modifiques una migración publicada.
- No commitear `data/`, `node_modules/`, `target/` ni `.idea/`.

## Revisión de código

- Un maintainer revisará tu PR.
- Responde a los comentarios de forma constructiva.
- Una vez aprobado, el maintainer fusionará el PR.

## Licencia

Al contribuir, aceptas que tu código se publique bajo GNU General Public License v3.0. Consulta [`LICENSE`](LICENSE).
