<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const requestedPath = computed(() => route.fullPath)
</script>

<template>
  <main class="not-found-page">
    <section class="not-found-hero">
      <div class="not-found-code" aria-hidden="true">404</div>
      <div class="not-found-copy">
        <span>PÁGINA NO ENCONTRADA</span>
        <h1>No encontramos esta página</h1>
        <p>La dirección pudo cambiar o el contenido ya no está disponible.</p>
        <small>Ruta solicitada: <code>{{ requestedPath }}</code></small>
        <div class="not-found-actions">
          <RouterLink to="/">Volver al inicio</RouterLink>
          <RouterLink to="/page/contacto" class="secondary">Contactar</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>
