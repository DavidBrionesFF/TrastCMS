<script setup lang="ts">
withDefaults(defineProps<{
  lines?: number
  height?: string
  radius?: string
}>(), {
  lines: 1,
  height: '1rem',
  radius: '0.75rem'
})
</script>

<template>
  <div class="skeleton-stack" aria-hidden="true">
    <span
      v-for="line in lines"
      :key="line"
      class="skeleton-block"
      :style="{
        height,
        borderRadius: radius,
        width: line === lines && lines > 1 ? '72%' : '100%'
      }"
    />
  </div>
</template>
