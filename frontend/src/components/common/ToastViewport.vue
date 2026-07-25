<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-viewport" aria-live="polite" aria-relevant="additions">
    <TransitionGroup name="toast-list">
      <article
        v-for="item in toast.visible"
        :key="item.id"
        class="toast-card"
        :class="`is-${item.tone}`"
        :data-tone="item.tone"
        role="status"
      >
        <span class="toast-card__icon" aria-hidden="true">
          {{ item.tone === 'success' ? '✓' : item.tone === 'error' ? '!' : item.tone === 'warning' ? '△' : 'i' }}
        </span>
        <div class="toast-card__content">
          <strong>{{ item.title }}</strong>
          <p v-if="item.message">{{ item.message }}</p>
        </div>
        <button type="button" aria-label="Cerrar notificación" @click="toast.remove(item.id)">×</button>
      </article>
    </TransitionGroup>
  </div>
</template>
