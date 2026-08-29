<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'
type Activity = { id: string; slug: string; name: string; description?: string; icon?: string }
const props = withDefaults(defineProps<{ limit?: number; editing?: boolean }>(), { limit: 8, editing: false })
const activities = ref<Activity[]>([])
onMounted(async () => { activities.value = (await api<Activity[]>('/api/public/travel/activities').catch(() => [])).slice(0, props.limit) })
</script>
<template>
  <div class="activity-widget-grid">
    <RouterLink v-for="item in activities" :key="item.id" :to="editing ? '' : { path: '/travel', query: { activity: item.slug } }" class="activity-widget-card">
      <span class="activity-widget-icon">{{ item.icon || '✦' }}</span><div><h3>{{ item.name }}</h3><p>{{ item.description || 'Explorar experiencias disponibles' }}</p></div><b>Ver tours →</b>
    </RouterLink>
    <div v-if="!activities.length" class="activity-widget-empty">Agregue actividades para mostrar este widget.</div>
  </div>
</template>
<style scoped>
.activity-widget-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.activity-widget-card{display:grid;grid-template-columns:auto 1fr;gap:13px;padding:18px;border:1px solid var(--theme-border,var(--line,#d9e6e3));border-radius:var(--theme-radius,17px);background:var(--theme-surface,var(--surface,#fff));color:var(--theme-text,var(--text,#092c2f));text-decoration:none;box-shadow:0 10px 28px color-mix(in srgb,var(--theme-primary,var(--primary,#087f7b)) 8%,transparent)}.activity-widget-icon{display:grid;place-items:center;width:42px;height:42px;border-radius:12px;background:color-mix(in srgb,var(--theme-primary,var(--primary,#087f7b)) 12%,white);font-size:1.35rem}.activity-widget-card h3{margin:0 0 4px}.activity-widget-card p{margin:0;color:var(--theme-text-muted,var(--muted,#617572));font-size:.82rem}.activity-widget-card b{grid-column:2;color:var(--theme-primary,var(--primary,#087f7b));font-size:.8rem}.activity-widget-empty{grid-column:1/-1;padding:35px;text-align:center;border:1px dashed var(--theme-border,var(--line,#d9e6e3));border-radius:16px}@media(max-width:950px){.activity-widget-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:560px){.activity-widget-grid{grid-template-columns:1fr}}
</style>
