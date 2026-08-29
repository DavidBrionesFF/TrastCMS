# Pruebas

Esta guía describe TrastCMS 3.6.1. El proyecto requiere un JDK capaz de compilar con `--release 21`. En este repositorio, `scripts/build.ps1` y `scripts/build.sh` seleccionan automáticamente `tools/graalvm` cuando está disponible.

## Ejecutar pruebas

### Backend y frontend

```bash
./mvnw clean verify
```

En Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Este comando:

1. Descarga Node.js si es necesario.
2. Ejecuta `npm install`.
3. Ejecuta `vue-tsc --noEmit`.
4. Compila Vue y Tailwind con Vite.
5. Copia `frontend/dist` dentro de los recursos Spring.
6. Ejecuta las pruebas Java.
7. Produce el JAR ejecutable.

## Estado conocido de 3.6.1

La compilación de producción y el frontend pasan, pero la suite completa todavía contiene expectativas heredadas del modelo anterior a multisitio. Actualmente `mvnw clean verify` no es una señal verde de release: falla en pruebas que esperan nombre de sitio, contenido demo o activación global de plugins.

Las pruebas de multisitio, propagación de `SiteContext` y experiencia de cliente deben ejecutarse junto con la suite completa antes de publicar una release.

Para generar un artefacto mientras se corrige esa deuda de pruebas:

```powershell
.\scripts\build.ps1
```

```bash
./scripts/build.sh
```

Estos scripts ejecutan `clean package -DskipTests`; no sustituyen la validación completa antes de publicar una release estable.

### Solo backend

```bash
./mvnw spring-boot:run -Pbackend-only
```

### Pruebas Java individuales

```bash
./mvnw test -Dtest=PostServiceTest
```

### Frontend independiente

```bash
cd frontend
npm install
npm run dev
```

## Estructura de pruebas

```
src/test/java/com/nattechnologies/trastcms/
├── service/         Pruebas de servicios
├── web/             Pruebas de controladores REST
└── ...

plugins/*/src/test/  Pruebas específicas de cada plugin
```

## Escribir pruebas

### Backend (Java)

- Usar `@SpringBootTest` para pruebas de integración.
- Usar `@WebMvcTest` para pruebas de controladores.
- Usar `MockMvc` para verificar endpoints REST.
- Nombre de clase: `<ClaseBajoPrueba>Test`.

Ejemplo:

```java
@SpringBootTest
class PostServiceTest {
    @Autowired PostService service;

    @Test
    void createPost_shouldSetDefaults() {
        // ...
    }
}
```

### Cobertura

- Cubrir servicios y controladores públicos.
- Priorizar casos de éxito y error.
- Verificar restricciones de rol y propiedad.

## Depuración

- Usar `logging.level.root=DEBUG` en `application.properties`.
- Ver `target/surefire-reports/` para resultados de Maven.
- Usar `npm run dev` con HMR para cambios visuales rápidos.
