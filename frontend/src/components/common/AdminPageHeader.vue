<script setup lang="ts">
import UiIcon from './UiIcon.vue'

defineProps<{
  eyebrow?: string
  title: string
  description?: string
}>()
</script>

<template>
  <header class="mb-7 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
    <div>
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h1 class="mt-1 text-3xl font-[780] tracking-[-0.035em] text-slate-950 lg:text-4xl">{{ title }}</h1>
      <p v-if="description" class="mt-2 max-w-3xl text-[15px] leading-6 text-slate-500">{{ description }}</p>
    </div>
    <div class="flex flex-wrap items-center gap-2">
      <slot name="actions" />
    </div>
  </header>
</template>
