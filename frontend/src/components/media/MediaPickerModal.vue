<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '@/services/api'
import type { MediaAsset, MediaKind } from '@/types'
import UiIcon from '@/components/common/UiIcon.vue'
import MediaPlayer from './MediaPlayer.vue'

const props = withDefaults(defineProps<{
  open: boolean
  kinds?: MediaKind[]
  multiple?: boolean
  title?: string
}>(), {
  kinds: () => ['IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT'],
  multiple: false,
  title: 'Seleccionar medio'
})

const emit = defineEmits<{
  close: []
  select: [assets: MediaAsset[]]
}>()

const media = ref<MediaAsset[]>([])
const loading = ref(false)
const search = ref('')
const kind = ref<MediaKind | ''>('')
const selected = ref<string[]>([])
const preview = ref<MediaAsset | null>(null)

const filtered = computed(() => media.value.filter(item => {
  if (!props.kinds.includes(item.kind)) return false
  if (kind.value && item.kind !== kind.value) return false
  const term = search.value.trim().toLowerCase()
  return !term || [item.title, item.originalFilename, item.altText, item.folder]
    .filter(Boolean)
    .some(value => String(value).toLowerCase().includes(term))
}))

async function load() {
  loading.value = true
  try { media.value = await api<MediaAsset[]>('/api/admin/media') }
  finally { loading.value = false }
}

function toggle(item: MediaAsset) {
  if (!props.multiple) {
    selected.value = [item.id]
    preview.value = item
    return
  }
  selected.value = selected.value.includes(item.id)
    ? selected.value.filter(id => id !== item.id)
    : [...selected.value, item.id]
  preview.value = item
}

function confirmSelection() {
  const assets = media.value.filter(item => selected.value.includes(item.id))
  if (assets.length) emit('select', assets)
}

watch(() => props.open, value => {
  if (value) {
    selected.value = []
    preview.value = null
    load()
  }
})
onMounted(() => { if (props.open) load() })
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/65 p-3 backdrop-blur-sm" @click.self="emit('close')">
      <section class="flex h-[min(860px,94vh)] w-[min(1380px,98vw)] flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <header class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2 class="text-xl font-bold text-slate-950">{{ title }}</h2>
            <p class="text-sm text-slate-500">Biblioteca central de imágenes, video, audio y documentos.</p>
          </div>
          <button class="rounded-lg p-2 text-slate-500 hover:bg-slate-100" @click="emit('close')"><UiIcon name="close" /></button>
        </header>

        <div class="grid min-h-0 flex-1 grid-cols-1 xl:grid-cols-[1fr_360px]">
          <main class="flex min-h-0 flex-col border-r border-slate-200">
            <div class="flex flex-wrap gap-2 border-b border-slate-200 p-4">
              <label class="relative min-w-[240px] flex-1">
                <UiIcon name="search" :size="17" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input v-model="search" class="w-full rounded-xl border border-slate-200 py-2.5 pl-10 pr-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" placeholder="Buscar por nombre, título o carpeta">
              </label>
              <select v-model="kind" class="rounded-xl border border-slate-200 bg-white px-3 py-2.5">
                <option value="">Todos los tipos</option>
                <option v-for="itemKind in kinds" :key="itemKind" :value="itemKind">{{ itemKind }}</option>
              </select>
            </div>

            <div class="min-h-0 flex-1 overflow-auto p-4">
              <div v-if="loading" class="grid h-full place-items-center text-slate-500">Cargando biblioteca…</div>
              <div v-else-if="!filtered.length" class="grid h-full place-items-center text-center text-slate-500">
                <div><UiIcon name="media" :size="42" class="mx-auto mb-3 text-slate-300" /><p class="font-semibold">No se encontraron medios</p></div>
              </div>
              <div v-else class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-5">
                <button
                  v-for="item in filtered"
                  :key="item.id"
                  type="button"
                  :class="['group overflow-hidden rounded-xl border bg-white text-left transition hover:-translate-y-0.5 hover:shadow-lg', selected.includes(item.id) ? 'border-violet-500 ring-4 ring-violet-100' : 'border-slate-200']"
                  @click="toggle(item)"
                >
                  <div class="relative aspect-square overflow-hidden bg-slate-100">
                    <img v-if="item.kind === 'IMAGE'" :src="item.publicUrl" :alt="item.altText || item.title || item.originalFilename" class="h-full w-full object-cover transition group-hover:scale-105">
                    <div v-else class="grid h-full place-items-center text-slate-400"><UiIcon :name="item.kind === 'VIDEO' ? 'video' : item.kind === 'AUDIO' ? 'audio' : 'document'" :size="38" /></div>
                    <span v-if="selected.includes(item.id)" class="absolute right-2 top-2 grid h-7 w-7 place-items-center rounded-full bg-violet-600 text-white"><UiIcon name="check" :size="16" /></span>
                  </div>
                  <div class="p-2.5">
                    <strong class="block truncate text-sm text-slate-800">{{ item.title || item.originalFilename }}</strong>
                    <small class="text-xs text-slate-500">{{ item.kind }} · {{ Math.max(1, Math.round(item.sizeBytes / 1024)) }} KB</small>
                  </div>
                </button>
              </div>
            </div>
          </main>

          <aside class="min-h-0 overflow-auto bg-slate-50 p-5">
            <div v-if="preview" class="space-y-4">
              <img v-if="preview.kind === 'IMAGE'" :src="preview.publicUrl" :alt="preview.altText || preview.title" class="max-h-64 w-full rounded-xl bg-white object-contain shadow-sm">
              <MediaPlayer v-else-if="preview.kind === 'VIDEO' || preview.kind === 'AUDIO'" :asset="preview" />
              <div v-else class="grid h-48 place-items-center rounded-xl border border-slate-200 bg-white text-slate-400"><UiIcon name="document" :size="52" /></div>
              <div>
                <h3 class="break-words font-bold text-slate-900">{{ preview.title || preview.originalFilename }}</h3>
                <p class="mt-1 text-xs text-slate-500">{{ preview.contentType }} · {{ Math.max(1, Math.round(preview.sizeBytes / 1024)) }} KB</p>
              </div>
              <dl class="grid grid-cols-[100px_1fr] gap-2 text-sm">
                <dt class="text-slate-500">Carpeta</dt><dd>{{ preview.folder }}</dd>
                <dt class="text-slate-500">Dimensiones</dt><dd>{{ preview.width && preview.height ? `${preview.width} × ${preview.height}` : 'No disponibles' }}</dd>
                <dt class="text-slate-500">Subido por</dt><dd>{{ preview.uploadedBy }}</dd>
              </dl>
            </div>
            <div v-else class="grid h-full place-items-center text-center text-sm text-slate-500">Seleccione un elemento para revisar sus detalles.</div>
          </aside>
        </div>

        <footer class="flex items-center justify-between border-t border-slate-200 px-5 py-4">
          <span class="text-sm text-slate-500">{{ selected.length }} seleccionado{{ selected.length === 1 ? '' : 's' }}</span>
          <div class="flex gap-2">
            <button class="rounded-xl border border-slate-300 px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-50" @click="emit('close')">Cancelar</button>
            <button class="rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white hover:bg-violet-700 disabled:opacity-50" :disabled="!selected.length" @click="confirmSelection">Insertar</button>
          </div>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
