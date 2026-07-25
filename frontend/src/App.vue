<script setup lang="ts">
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'
import ToastViewport from '@/components/common/ToastViewport.vue'
</script>

<template>
  <RouterView v-slot="{ Component }">
    <Suspense timeout="0">
      <component :is="Component" />
      <template #fallback>
        <main class="route-skeleton" aria-busy="true" aria-label="Cargando página">
          <SkeletonBlock height="34px" width="180px" />
          <SkeletonBlock height="76px" />
          <div class="route-skeleton__grid">
            <SkeletonBlock v-for="index in 6" :key="index" height="210px" />
          </div>
        </main>
      </template>
    </Suspense>
  </RouterView>
  <ToastViewport />
</template>
