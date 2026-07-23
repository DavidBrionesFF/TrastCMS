<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import type { MediaAsset, MediaKind } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import MediaPlayer from '@/components/media/MediaPlayer.vue'

const media = ref<MediaAsset[]>([])
const loading = ref(false)
const uploading = ref(false)
const dragActive = ref(false)
const error = ref('')
const success = ref('')
const search = ref('')
const kind = ref<MediaKind | ''>('')
const folder = ref('')
const view = ref<'grid' | 'list'>('grid')
const selectedIds = ref<string[]>([])
const active = ref<MediaAsset | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const details = reactive({ title: '', altText: '', caption: '', description: '', folder: 'General', width: undefined as number | undefined, height: undefined as number | undefined, durationSeconds: undefined as number | undefined })

const folders = computed(() => Array.from(new Set(media.value.map(item => item.folder).filter(Boolean))).sort())
const filtered = computed(() => media.value.filter(item => {
  const term = search.value.trim().toLowerCase()
  return (!kind.value || item.kind === kind.value)
    && (!folder.value || item.folder === folder.value)
    && (!term || [item.title, item.originalFilename, item.altText, item.caption, item.folder]
      .filter(Boolean).some(value => String(value).toLowerCase().includes(term)))
}))
const storageBytes = computed(() => media.value.reduce((total, item) => total + item.sizeBytes, 0))

async function load() {
  loading.value = true
  error.value = ''
  try { media.value = await api<MediaAsset[]>('/api/admin/media') }
  catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo cargar la biblioteca' }
  finally { loading.value = false }
}

function choose(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  upload(files)
  ;(event.target as HTMLInputElement).value = ''
}

function dropped(event: DragEvent) {
  dragActive.value = false
  upload(Array.from(event.dataTransfer?.files || []))
}

async function upload(files: File[]) {
  if (!files.length) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const data = new FormData()
    files.forEach(file => data.append('files', file))
    await api('/api/admin/media/batch', { method: 'POST', body: data })
    success.value = `${files.length} archivo${files.length === 1 ? '' : 's'} subido${files.length === 1 ? '' : 's'} correctamente.`
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'No se pudieron subir los archivos'
  } finally { uploading.value = false }
}

function openDetails(item: MediaAsset) {
  active.value = item
  Object.assign(details, {
    title: item.title || '',
    altText: item.altText || '',
    caption: item.caption || '',
    description: item.description || '',
    folder: item.folder || 'General',
    width: item.width,
    height: item.height,
    durationSeconds: item.durationSeconds
  })
}

async function saveDetails() {
  if (!active.value) return
  error.value = ''
  try {
    const updated = await api<MediaAsset>(`/api/admin/media/${active.value.id}`, { method: 'PUT', body: JSON.stringify(details) })
    const index = media.value.findIndex(item => item.id === updated.id)
    if (index >= 0) media.value[index] = updated
    active.value = updated
    success.value = 'Metadatos actualizados.'
  } catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo actualizar' }
}

function toggle(item: MediaAsset) {
  selectedIds.value = selectedIds.value.includes(item.id)
    ? selectedIds.value.filter(id => id !== item.id)
    : [...selectedIds.value, item.id]
}

async function remove(item: MediaAsset) {
  if (!confirm(`¿Eliminar permanentemente ${item.originalFilename}?`)) return
  await api(`/api/admin/media/${item.id}`, { method: 'DELETE' })
  if (active.value?.id === item.id) active.value = null
  selectedIds.value = selectedIds.value.filter(id => id !== item.id)
  await load()
}

async function removeSelected() {
  if (!selectedIds.value.length || !confirm(`¿Eliminar ${selectedIds.value.length} archivos permanentemente?`)) return
  error.value = ''
  try {
    for (const id of selectedIds.value) await api(`/api/admin/media/${id}`, { method: 'DELETE' })
    selectedIds.value = []
    active.value = null
    await load()
  } catch (e) { error.value = e instanceof Error ? e.message : 'No se completó la eliminación' }
}

async function copy(value: string) {
  await navigator.clipboard.writeText(value)
  success.value = 'URL copiada al portapapeles.'
}

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="BIBLIOTECA DIGITAL" title="Medios" description="Administre imágenes, video, audio y documentos desde una biblioteca central con metadatos reutilizables.">
    <template #actions>
      <input ref="fileInput" type="file" multiple class="hidden" accept="image/*,video/*,audio/*,.pdf" @change="choose">
      <button class="inline-flex items-center gap-2 rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white shadow-lg shadow-violet-200 hover:bg-violet-700" :disabled="uploading" @click="fileInput?.click()"><UiIcon name="upload" :size="17" />{{ uploading ? 'Subiendo…' : 'Subir archivos' }}</button>
    </template>
  </AdminPageHeader>

  <p v-if="error" class="form-error mb-4">{{ error }}</p>
  <p v-if="success" class="form-success mb-4">{{ success }}</p>

  <section class="mb-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
    <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Elementos</span><strong class="mt-2 block text-3xl text-slate-950">{{ media.length }}</strong></div>
    <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Almacenamiento</span><strong class="mt-2 block text-3xl text-slate-950">{{ formatSize(storageBytes) }}</strong></div>
    <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Imágenes</span><strong class="mt-2 block text-3xl text-slate-950">{{ media.filter(item => item.kind === 'IMAGE').length }}</strong></div>
    <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span class="text-xs font-semibold uppercase tracking-wider text-slate-400">Audio y video</span><strong class="mt-2 block text-3xl text-slate-950">{{ media.filter(item => item.kind === 'VIDEO' || item.kind === 'AUDIO').length }}</strong></div>
  </section>

  <section
    :class="['mb-5 rounded-2xl border-2 border-dashed p-7 text-center transition', dragActive ? 'border-violet-500 bg-violet-50' : 'border-slate-200 bg-white']"
    @dragenter.prevent="dragActive=true"
    @dragover.prevent="dragActive=true"
    @dragleave.prevent="dragActive=false"
    @drop.prevent="dropped"
  >
    <span class="mx-auto grid h-12 w-12 place-items-center rounded-xl bg-violet-100 text-violet-700"><UiIcon name="upload" :size="24" /></span>
    <h3 class="mt-3 font-bold text-slate-900">Arrastre archivos aquí</h3>
    <p class="mt-1 text-sm text-slate-500">JPG, PNG, WebP, GIF, MP4, WebM, MP3, WAV, OGG y PDF. Se permiten cargas múltiples.</p>
    <button type="button" class="mt-3 text-sm font-bold text-violet-700" @click="fileInput?.click()">Seleccionar desde el equipo</button>
  </section>

  <section class="mb-5 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm xl:flex-row xl:items-center">
    <label class="relative min-w-[280px] flex-1"><UiIcon name="search" :size="17" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input v-model="search" class="w-full rounded-xl border border-slate-200 py-2.5 pl-10 pr-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" placeholder="Buscar medios…"></label>
    <select v-model="kind" class="rounded-xl border border-slate-200 bg-white px-3 py-2.5"><option value="">Todos los tipos</option><option value="IMAGE">Imágenes</option><option value="VIDEO">Videos</option><option value="AUDIO">Audio</option><option value="DOCUMENT">Documentos</option></select>
    <select v-model="folder" class="rounded-xl border border-slate-200 bg-white px-3 py-2.5"><option value="">Todas las carpetas</option><option v-for="item in folders" :key="item" :value="item">{{ item }}</option></select>
    <button v-if="selectedIds.length" class="inline-flex items-center gap-2 rounded-xl border border-red-200 px-3 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50" @click="removeSelected"><UiIcon name="trash" :size="16" />Eliminar {{ selectedIds.length }}</button>
    <div class="flex rounded-xl border border-slate-200 p-1"><button type="button" :class="['rounded-lg p-2', view === 'grid' ? 'bg-slate-100 text-violet-700' : 'text-slate-400']" @click="view='grid'"><UiIcon name="grid" :size="18" /></button><button type="button" :class="['rounded-lg p-2', view === 'list' ? 'bg-slate-100 text-violet-700' : 'text-slate-400']" @click="view='list'"><UiIcon name="list" :size="18" /></button></div>
  </section>

  <div v-if="loading" class="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white text-slate-500">Cargando biblioteca…</div>
  <div v-else-if="!filtered.length" class="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white text-center"><div><UiIcon name="media" :size="46" class="mx-auto mb-3 text-slate-300"/><h3 class="font-bold text-slate-800">No hay archivos que coincidan</h3><p class="mt-1 text-sm text-slate-500">Cambie los filtros o cargue nuevos medios.</p></div></div>

  <section v-else-if="view === 'grid'" class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-6">
    <article v-for="item in filtered" :key="item.id" :class="['group relative overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-xl', selectedIds.includes(item.id) ? 'border-violet-500 ring-4 ring-violet-100' : 'border-slate-200']">
      <button type="button" class="absolute left-2 top-2 z-10 grid h-7 w-7 place-items-center rounded-lg bg-white/90 text-slate-500 shadow" @click="toggle(item)"><UiIcon :name="selectedIds.includes(item.id) ? 'check' : 'plus'" :size="14" /></button>
      <button type="button" class="block aspect-square w-full overflow-hidden bg-slate-100" @click="openDetails(item)">
        <img v-if="item.kind === 'IMAGE'" :src="item.publicUrl" :alt="item.altText || item.title" class="h-full w-full object-cover transition duration-300 group-hover:scale-105">
        <div v-else class="grid h-full place-items-center bg-gradient-to-br from-slate-100 to-slate-200 text-slate-400"><span class="text-center"><UiIcon :name="item.kind === 'VIDEO' ? 'video' : item.kind === 'AUDIO' ? 'audio' : 'document'" :size="42" class="mx-auto"/><small class="mt-2 block font-semibold">{{ item.kind }}</small></span></div>
      </button>
      <div class="p-3"><button class="block w-full truncate text-left text-sm font-bold text-slate-800 hover:text-violet-700" @click="openDetails(item)">{{ item.title || item.originalFilename }}</button><div class="mt-1 flex items-center justify-between text-[11px] text-slate-400"><span>{{ item.folder }}</span><span>{{ formatSize(item.sizeBytes) }}</span></div></div>
    </article>
  </section>

  <section v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div class="overflow-x-auto"><table class="w-full min-w-[900px]"><thead><tr class="bg-slate-50 text-left text-[11px] uppercase tracking-wider text-slate-500"><th class="px-4 py-3"></th><th class="px-4 py-3">Archivo</th><th class="px-4 py-3">Tipo</th><th class="px-4 py-3">Carpeta</th><th class="px-4 py-3">Dimensiones/duración</th><th class="px-4 py-3">Tamaño</th><th class="px-4 py-3"></th></tr></thead><tbody class="divide-y divide-slate-100"><tr v-for="item in filtered" :key="item.id" class="hover:bg-slate-50"><td class="px-4 py-3"><input type="checkbox" :checked="selectedIds.includes(item.id)" class="h-4 w-4" @change="toggle(item)"></td><td class="px-4 py-3"><button class="flex items-center gap-3 text-left" @click="openDetails(item)"><img v-if="item.kind === 'IMAGE'" :src="item.publicUrl" class="h-12 w-12 rounded-lg object-cover"><span v-else class="grid h-12 w-12 place-items-center rounded-lg bg-slate-100 text-slate-400"><UiIcon :name="item.kind === 'VIDEO' ? 'video' : item.kind === 'AUDIO' ? 'audio' : 'document'"/></span><span><strong class="block text-sm text-slate-800">{{ item.title || item.originalFilename }}</strong><small class="text-xs text-slate-400">{{ item.originalFilename }}</small></span></button></td><td class="px-4 py-3 text-sm text-slate-600">{{ item.contentType }}</td><td class="px-4 py-3 text-sm text-slate-600">{{ item.folder }}</td><td class="px-4 py-3 text-sm text-slate-500">{{ item.width && item.height ? `${item.width} × ${item.height}` : item.durationSeconds ? `${item.durationSeconds}s` : '—' }}</td><td class="px-4 py-3 text-sm text-slate-500">{{ formatSize(item.sizeBytes) }}</td><td class="px-4 py-3"><div class="flex justify-end gap-2"><button class="rounded-lg border border-slate-200 p-2 text-slate-500" @click="copy(item.publicUrl)"><UiIcon name="copy" :size="15" /></button><button class="rounded-lg border border-red-100 p-2 text-red-500" @click="remove(item)"><UiIcon name="trash" :size="15" /></button></div></td></tr></tbody></table></div></section>

  <Teleport to="body">
    <div v-if="active" class="fixed inset-0 z-[100] flex justify-end bg-slate-950/55 backdrop-blur-sm" @click.self="active=null">
      <aside class="h-full w-full max-w-xl overflow-y-auto bg-white shadow-2xl">
        <header class="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4"><div><h2 class="text-xl font-bold text-slate-900">Detalles del medio</h2><p class="text-xs text-slate-500">{{ active.originalFilename }}</p></div><button class="rounded-lg p-2 text-slate-500 hover:bg-slate-100" @click="active=null"><UiIcon name="close" /></button></header>
        <div class="space-y-5 p-5">
          <MediaPlayer v-if="active.kind === 'IMAGE' || active.kind === 'VIDEO' || active.kind === 'AUDIO'" :asset="active" />
          <div v-else class="grid h-60 place-items-center rounded-2xl bg-slate-100 text-slate-400"><UiIcon name="document" :size="58" /></div>

          <div class="grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4 text-xs"><div><span class="text-slate-400">Tipo</span><strong class="mt-1 block text-slate-700">{{ active.contentType }}</strong></div><div><span class="text-slate-400">Tamaño</span><strong class="mt-1 block text-slate-700">{{ formatSize(active.sizeBytes) }}</strong></div><div><span class="text-slate-400">Dimensiones</span><strong class="mt-1 block text-slate-700">{{ active.width && active.height ? `${active.width} × ${active.height}` : 'No disponibles' }}</strong></div><div><span class="text-slate-400">Fecha</span><strong class="mt-1 block text-slate-700">{{ new Date(active.createdAt).toLocaleString() }}</strong></div></div>

          <form class="space-y-4" @submit.prevent="saveDetails">
            <label class="block text-sm font-semibold text-slate-700">Título<input v-model="details.title" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3"></label>
            <label v-if="active.kind === 'IMAGE'" class="block text-sm font-semibold text-slate-700">Texto alternativo<input v-model="details.altText" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3" placeholder="Describa la imagen para accesibilidad y SEO"></label>
            <label class="block text-sm font-semibold text-slate-700">Leyenda<textarea v-model="details.caption" rows="2" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3"></textarea></label>
            <label class="block text-sm font-semibold text-slate-700">Descripción<textarea v-model="details.description" rows="4" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3"></textarea></label>
            <label class="block text-sm font-semibold text-slate-700">Carpeta<input v-model="details.folder" list="media-folders" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3"><datalist id="media-folders"><option v-for="item in folders" :key="item" :value="item" /></datalist></label>
            <div v-if="active.kind === 'VIDEO' || active.kind === 'AUDIO'" class="grid grid-cols-2 gap-3"><label class="block text-sm font-semibold text-slate-700">Duración (segundos)<input v-model.number="details.durationSeconds" type="number" min="0" step="0.01" class="mt-1.5 w-full rounded-xl border border-slate-200 p-3"></label></div>
            <div class="flex flex-wrap gap-2"><button class="rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white hover:bg-violet-700">Guardar cambios</button><button type="button" class="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2.5 font-semibold text-slate-700" @click="copy(active.publicUrl)"><UiIcon name="copy" :size="16"/>Copiar URL</button><button type="button" class="ml-auto inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2.5 font-semibold text-red-600 hover:bg-red-50" @click="remove(active)"><UiIcon name="trash" :size="16"/>Eliminar</button></div>
          </form>
        </div>
      </aside>
    </div>
  </Teleport>
</template>
