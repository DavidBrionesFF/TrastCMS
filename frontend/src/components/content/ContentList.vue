<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import type { ContentType, PageResponse, PostStatus, PostSummary } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const props = defineProps<{
  contentType: ContentType
  title: string
  description: string
  createRoute: string
  editRoute: string
}>()

const data = ref<PageResponse<PostSummary> | null>(null)
const error = ref('')
const loading = ref(false)
const search = ref('')
const status = ref<PostStatus | ''>('')
const view = ref<'table' | 'grid'>('table')

const items = computed(() => (data.value?.content || []).filter(item => {
  const term = search.value.trim().toLowerCase()
  return (!term || `${item.title} ${item.slug} ${item.authorName}`.toLowerCase().includes(term))
    && (!status.value || item.status === status.value)
}))

async function load() {
  loading.value = true
  error.value = ''
  try { data.value = await api<PageResponse<PostSummary>>(`/api/admin/posts?type=${props.contentType}&size=100`) }
  catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo cargar el contenido' }
  finally { loading.value = false }
}

async function remove(item: PostSummary) {
  if (!confirm(`¿Eliminar “${item.title}”? Esta acción no se puede deshacer.`)) return
  try { await api(`/api/admin/posts/${item.id}`, { method: 'DELETE' }); await load() }
  catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo eliminar' }
}

function statusLabel(value: PostStatus) {
  return value === 'PUBLISHED' ? 'Publicada' : value === 'DRAFT' ? 'Borrador' : 'Archivada'
}
onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="CONTENIDO" :title="title" :description="description">
    <template #actions>
      <RouterLink :to="{ name: createRoute }" class="inline-flex items-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white shadow-lg shadow-violet-200 transition hover:-translate-y-0.5 hover:bg-violet-700"><UiIcon name="plus" :size="17" />{{ contentType === 'PAGE' ? 'Nueva página' : 'Nueva publicación' }}</RouterLink>
    </template>
  </AdminPageHeader>

  <p v-if="error" class="form-error mb-4">{{ error }}</p>

  <section class="mb-5 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm lg:flex-row lg:items-center">
    <label class="relative min-w-[260px] flex-1"><UiIcon name="search" :size="17" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input v-model="search" class="w-full rounded-xl border border-slate-200 py-2.5 pl-10 pr-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" :placeholder="`Buscar ${contentType === 'PAGE' ? 'páginas' : 'publicaciones'}…`"></label>
    <select v-model="status" class="rounded-xl border border-slate-200 bg-white px-3 py-2.5"><option value="">Todos los estados</option><option value="PUBLISHED">Publicadas</option><option value="DRAFT">Borradores</option><option value="ARCHIVED">Archivadas</option></select>
    <div class="flex rounded-xl border border-slate-200 p-1"><button type="button" :class="['rounded-lg p-2', view === 'table' ? 'bg-slate-100 text-violet-700' : 'text-slate-400']" @click="view='table'"><UiIcon name="list" :size="18" /></button><button type="button" :class="['rounded-lg p-2', view === 'grid' ? 'bg-slate-100 text-violet-700' : 'text-slate-400']" @click="view='grid'"><UiIcon name="grid" :size="18" /></button></div>
  </section>

  <div v-if="loading" class="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white text-slate-500">Cargando…</div>
  <div v-else-if="!items.length" class="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white p-8 text-center"><div><span class="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-violet-100 text-violet-700"><UiIcon :name="contentType === 'PAGE' ? 'page' : 'post'" :size="28" /></span><h3 class="text-lg font-bold text-slate-900">No hay {{ contentType === 'PAGE' ? 'páginas' : 'publicaciones' }}</h3><p class="mt-2 text-sm text-slate-500">Cree el primer contenido para comenzar a construir su sitio.</p></div></div>

  <section v-else-if="view === 'table'" class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div class="overflow-x-auto"><table class="w-full min-w-[880px] border-collapse"><thead><tr class="bg-slate-50 text-left text-[11px] uppercase tracking-wider text-slate-500"><th class="px-5 py-3.5">Contenido</th><th class="px-5 py-3.5">Editor</th><th class="px-5 py-3.5">Estado</th><th class="px-5 py-3.5">Autor</th><th class="px-5 py-3.5">Actualización</th><th class="px-5 py-3.5"></th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="item in items" :key="item.id" class="transition hover:bg-slate-50/70"><td class="px-5 py-4"><div class="flex items-center gap-3"><div class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-violet-50 text-violet-700"><UiIcon :name="contentType === 'PAGE' ? 'page' : 'post'" :size="18" /></div><div><RouterLink :to="{ name: editRoute, params: { id: item.id } }" class="font-bold text-slate-900 hover:text-violet-700">{{ item.title }}</RouterLink><small class="block text-xs text-slate-400">/{{ item.slug }}</small></div></div></td><td class="px-5 py-4 text-sm text-slate-600">{{ item.editorMode === 'VISUAL_BUILDER' ? 'Constructor visual' : 'Texto enriquecido' }}</td><td class="px-5 py-4"><span :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</span></td><td class="px-5 py-4 text-sm text-slate-600">{{ item.authorName }}</td><td class="px-5 py-4 text-sm text-slate-500">{{ new Date(item.updatedAt).toLocaleString() }}</td><td class="px-5 py-4"><div class="flex justify-end gap-2"><RouterLink :to="{ name: editRoute, params: { id: item.id } }" class="rounded-lg border border-slate-200 p-2 text-slate-500 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700"><UiIcon name="edit" :size="16" /></RouterLink><a v-if="item.status === 'PUBLISHED'" :href="contentType === 'PAGE' ? `/page/${item.slug}` : `/post/${item.slug}`" target="_blank" class="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-50"><UiIcon name="external" :size="16" /></a><button class="rounded-lg border border-red-100 p-2 text-red-500 hover:bg-red-50" @click="remove(item)"><UiIcon name="trash" :size="16" /></button></div></td></tr></tbody></table></div>
  </section>

  <section v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
    <article v-for="item in items" :key="item.id" class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-xl">
      <div class="relative aspect-[16/8] bg-gradient-to-br from-violet-100 via-white to-sky-100"><img v-if="item.featuredImageUrl" :src="item.featuredImageUrl" :alt="item.title" class="h-full w-full object-cover"><span class="absolute left-3 top-3" :class="['status', item.status.toLowerCase()]">{{ statusLabel(item.status) }}</span></div>
      <div class="p-5"><div class="mb-2 flex items-center gap-2 text-xs text-slate-400"><span>{{ item.editorMode === 'VISUAL_BUILDER' ? 'Constructor visual' : 'Rich text' }}</span><span>·</span><span>{{ item.authorName }}</span></div><RouterLink :to="{ name: editRoute, params: { id: item.id } }" class="text-lg font-bold text-slate-900 hover:text-violet-700">{{ item.title }}</RouterLink><p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{{ item.excerpt || 'Sin extracto.' }}</p><div class="mt-4 flex items-center justify-between"><small class="text-xs text-slate-400">{{ new Date(item.updatedAt).toLocaleDateString() }}</small><div class="flex gap-2"><RouterLink :to="{ name: editRoute, params: { id: item.id } }" class="text-sm font-semibold text-violet-700">Editar</RouterLink><button class="text-sm font-semibold text-red-500" @click="remove(item)">Eliminar</button></div></div></div>
    </article>
  </section>
</template>
