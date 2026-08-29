# Guía de estilo

TrastCMS sigue convenciones estrictas para mantener la legibilidad y coherencia entre Java y Vue.

## Java

### Formato

- Indentación: 4 espacios.
- Límite de línea: 120 caracteres.
- Llaves en la misma línea.
- Importaciones organizadas por IDE (agrupar estáticas, luego por paquete).

### Nombres

- Clases: `PascalCase`.
- Métodos y variables: `camelCase`.
- Constantes: `UPPER_SNAKE_CASE`.
- Paquetes: `lowercase` sin guiones.

### Clases

- Una clase pública por archivo.
- Orden de miembros: constantes, constructores, métodos públicos, métodos privados.
- Preferir `record` para DTOs inmutables.
- Usar `var` localmente cuando el tipo es obvio.

### Anotaciones

- `@Service`, `@RestController`, `@Configuration` en clases.
- `@Transactional` en métodos de servicio.
- `@PrePersist`, `@PreUpdate` en entidades.
- `@Bean` en métodos de configuración.

### Documentación

- JavaDoc en interfaces públicas y clases de servicio principales.
- Documentar parámetros no obvios y valores de retorno.

### Inyección de dependencias

- Usar constructor injection obligatoriamente.
- No usar `@Autowired` en campos.

## Vue / TypeScript

### Formato

- Indentación: 2 espacios.
- Usar `<script setup lang="ts">` en componentes.
- Tipar todas las props, emits y estado.
- No usar `any` sin justificación.

### Nombres

- Componentes: `PascalCase` en archivo y template.
- Composables: `useCamelCase.ts`.
- Stores Pinia: `camelCase.ts` con `defineStore('kebab-case', ...)`.
- Props: `camelCase` en script, `kebab-case` en template.

### Estructura de componente

```vue
<script setup lang="ts">
// 1. Imports
// 2. Props / Emits
// 3. Estado reactivo
// 4. Computadas
// 5. Métodos
// 6. Watch
// 7. Lifecycle
</script>

<template>
  <!-- markup -->
</template>

<style scoped>
/* estilos */
</style>
```

### Comunicación con backend

- Usar el cliente centralizado en `frontend/src/services/api.ts`.
- No crear instancias independientes de `axios`.
- Usar tipos definidos en `frontend/src/types.ts`.

### Estado global

- Usar Pinia para estado compartido entre componentes.
- No duplicar estado local que ya vive en un store.

## Migraciones Flyway

- Nombre: `V<version>__<descripcion>.sql`.
- Versiones: enteros incrementales (1, 2, 3...).
- Nunca modificar una migración ya ejecutada en producción.
- Incluir `rollback` solo si es necesario.

## Commits

Seguir [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>(<scope>): <descripción>
```

Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

## Seguridad

- No commitear secretos, credenciales o `.env`.
- No incluir `data/`, `node_modules/` ni artefactos compilados.
- Usar `TRASTCMS_PLUGIN_MASTER_KEY` para registrar plugins en producción.
- Validar y sanitizar todas las entradas del usuario en backend.
