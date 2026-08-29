# Arquitectura del frontend

TrastCMS usa Vue 3 con TypeScript, Vite, Tailwind CSS, Pinia y Vue Router en un único bundle compilado por Maven.

## Stack

- **Vue 3.5** con Composition API y `<script setup>`.
- **TypeScript 5.8** estricto.
- **Vite 7** como bundler.
- **Tailwind CSS 4** para estilos.
- **Pinia 3** para estado global.
- **Vue Router 4** para enrutamiento.
- **Tiptap 3** como editor rich text.
- **Plyr 3** para reproductores multimedia.

## Estructura

```
frontend/src/
├── App.vue                  Componente raíz
├── main.ts                  Punto de entrada
├── env.d.ts                 Tipos de Vite
├── types.ts                 Tipos TypeScript compartidos
├── components/
│   ├── builder/             Bloques y renderizador del constructor visual
│   │   ├── BlockRenderer.vue
│   │   ├── PageBuilder.vue
│   │   └── builder.ts
│   ├── common/              Componentes reutilizables
│   │   ├── AdminPageHeader.vue
│   │   ├── SkeletonBlock.vue
│   │   ├── ToastViewport.vue
│   │   └── UiIcon.vue
│   ├── content/             Lista de contenido
│   │   └── ContentList.vue
│   ├── editor/              Editor rich text
│   │   └── RichTextEditor.vue
│   └── media/               Selector y reproductor multimedia
│       ├── MediaPickerModal.vue
│       └── MediaPlayer.vue
├── layouts/
│   ├── AdminLayout.vue      Layout del panel administrativo
│   └── PublicLayout.vue     Layout del sitio público
├── router/
│   └── index.ts             Rutas y guards
├── services/
│   └── api.ts               Cliente HTTP centralizado (axios)
├── stores/
│   ├── auth.ts              Estado de autenticación
│   ├── cart.ts              Estado del carrito
│   └── toast.ts             Notificaciones toast
└── views/
    ├── admin/               Vistas del panel administrativo
    │   ├── AccountView.vue
    │   ├── DashboardView.vue
    │   ├── LoginView.vue
    │   ├── PagesView.vue
    │   ├── PostsView.vue
    │   └── ...
    └── public/              Vistas del sitio público
        ├── HomeView.vue
        ├── PageView.vue
        ├── StorefrontView.vue
        └── ...
```

## Convenciones

### Componentes

- Un componente por archivo.
- Usar `<script setup lang="ts">`.
- Definir props con `defineProps<T>()`.
- Definir emits con `defineEmits<T>()`.

### Estado

- Estado local: `ref` y `reactive` dentro del componente.
- Estado compartido: stores Pinia en `stores/`.
- No duplicar estado que ya existe en un store.

### HTTP

- Todas las llamadas pasan por `services/api.ts`.
- Usar tipos de `types.ts` para respuestas.
- No crear instancias de `axios` fuera del servicio centralizado.

### Rutas

- Administración: prefijo `/admin`.
- Públicas: raíz y prefijos de plugin.
- API: prefijo `/api`.
- Rutas protegidas usan guards en `router/index.ts`.

## Desarrollo con HMR

```bash
# Terminal 1: Backend
./mvnw spring-boot:run -Pbackend-only

# Terminal 2: Frontend
cd frontend
npm install
npm run dev
```

Vite sirve en `http://localhost:5173` y reenvía `/api` y `/actuator` al backend.

## Build

```bash
./mvnw clean verify
```

Maven:

1. Ejecuta `npm install`.
2. Ejecuta `vue-tsc --noEmit`.
3. Compila Vue y Tailwind con Vite.
4. Copia `frontend/dist` a `target/classes/static`.

El resultado es un único JAR con el frontend embebido.

## Temas

Cada tema en `src/main/resources/themes/<nombre>/` incluye:

- `assets/` — CSS, imágenes, fuentes.
- `templates/` — Plantillas Vue.
- `starter-content.json` — Contenido inicial opcional.

El personalizador genera opciones tipadas desde `settingsSchema` del manifiesto del tema.
