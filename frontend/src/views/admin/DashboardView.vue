<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import type { Dashboard } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const data = ref<Dashboard | null>(null)
const loading = ref(true)
const publishRate = computed(() => {
  if (!data.value || !data.value.posts) return 0
  return Math.round((data.value.published / Math.max(1, data.value.posts + data.value.pages)) * 100)
})
onMounted(async () => { try { data.value = await api<Dashboard>('/api/admin/dashboard') } finally { loading.value = false } })
</script>

<template>
  <AdminPageHeader eyebrow="Panel general" title="Resumen del sitio" description="Contenido, medios, usuarios y accesos rápidos para administrar TrastCMS.">
    <template #actions><RouterLink :to="{ name: 'page-new' }" class="button secondary"><UiIcon name="page" :size="16"/>Nueva página</RouterLink><RouterLink :to="{ name: 'post-new' }" class="button"><UiIcon name="plus" :size="16"/>Nueva publicación</RouterLink></template>
  </AdminPageHeader>

  <div v-if="loading" class="panel empty">Cargando indicadores…</div>
  <template v-else-if="data">
    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article class="dashboard-stat"><span class="stat-icon violet"><UiIcon name="post"/></span><div><small>Publicaciones</small><strong>{{ data.posts }}</strong><p>{{ data.published }} publicadas</p></div></article>
      <article class="dashboard-stat"><span class="stat-icon blue"><UiIcon name="page"/></span><div><small>Páginas</small><strong>{{ data.pages }}</strong><p>Constructor visual disponible</p></div></article>
      <article class="dashboard-stat"><span class="stat-icon emerald"><UiIcon name="media"/></span><div><small>Medios</small><strong>{{ data.media }}</strong><p>Imágenes, audio y video</p></div></article>
      <article class="dashboard-stat"><span class="stat-icon amber"><UiIcon name="users"/></span><div><small>Usuarios</small><strong>{{ data.users }}</strong><p>{{ data.categories }} categorías</p></div></article>
    </section>

    <section class="mt-6 grid gap-6 xl:grid-cols-[1.25fr_.75fr]">
      <article class="panel dashboard-welcome"><div><span class="eyebrow">TrastCMS 2.2</span><h2>Construya sitios completos con Spring Boot y Vue</h2><p>Diseñe páginas visualmente, publique contenido enriquecido, administre medios profesionales y amplíe el CMS con temas o plugins.</p><div class="mt-6 flex flex-wrap gap-3"><RouterLink :to="{ name: 'page-new' }" class="button"><UiIcon name="layers" :size="17"/>Abrir constructor visual</RouterLink><RouterLink :to="{ name: 'media' }" class="button secondary"><UiIcon name="media" :size="17"/>Biblioteca de medios</RouterLink></div></div><div class="dashboard-orbit"><span class="orbit-core">T</span><span class="orbit-item one"><UiIcon name="page"/></span><span class="orbit-item two"><UiIcon name="plugin"/></span><span class="orbit-item three"><UiIcon name="theme"/></span></div></article>
      <article class="panel"><div class="flex items-center justify-between"><div><span class="eyebrow">Estado editorial</span><h2 class="mt-1 text-xl font-bold">Contenido listo</h2></div><strong class="text-3xl text-violet-700">{{ publishRate }}%</strong></div><div class="mt-5 h-3 overflow-hidden rounded-full bg-slate-100"><span class="block h-full rounded-full bg-gradient-to-r from-violet-600 to-sky-400" :style="{ width: `${publishRate}%` }"></span></div><dl class="mt-6 space-y-3"><div class="flex justify-between"><dt class="text-slate-500">Publicados</dt><dd class="font-bold">{{ data.published }}</dd></div><div class="flex justify-between"><dt class="text-slate-500">Borradores</dt><dd class="font-bold">{{ data.drafts }}</dd></div><div class="flex justify-between"><dt class="text-slate-500">Total de contenido</dt><dd class="font-bold">{{ data.posts + data.pages }}</dd></div></dl></article>
    </section>

    <section class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <RouterLink :to="{ name: 'pages' }" class="quick-card"><UiIcon name="layers"/><div><strong>Diseñar páginas</strong><span>Constructor tipo Elementor</span></div><b>→</b></RouterLink>
      <RouterLink :to="{ name: 'media' }" class="quick-card"><UiIcon name="video"/><div><strong>Gestionar medios</strong><span>Video, audio e imágenes</span></div><b>→</b></RouterLink>
      <RouterLink :to="{ name: 'themes' }" class="quick-card"><UiIcon name="theme"/><div><strong>Personalizar tema</strong><span>Tokens, plantillas y estilos</span></div><b>→</b></RouterLink>
      <RouterLink :to="{ name: 'plugins' }" class="quick-card"><UiIcon name="plugin"/><div><strong>Extender TrastCMS</strong><span>PF4J, hooks y webhooks</span></div><b>→</b></RouterLink>
    </section>
  </template>
</template>
