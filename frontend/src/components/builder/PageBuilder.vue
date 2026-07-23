<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { BuilderBlock, BuilderBlockType, BuilderDocument, MediaAsset, MediaKind, PluginBuilderBlockDefinition, PluginContributions, PluginFieldDefinition } from '@/types'
import { BLOCK_LIBRARY, cloneBlock, createBlock, createPluginBlock, parseDocument } from './builder'
import BlockRenderer from './BlockRenderer.vue'
import MediaPickerModal from '@/components/media/MediaPickerModal.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import { api } from '@/services/api'

const props = defineProps<{ modelValue?: string | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const document = reactive<BuilderDocument>(parseDocument(props.modelValue))
const selectedId = ref<string | null>(document.blocks[0]?.id || null)
const previewMode = ref<'desktop' | 'tablet' | 'mobile'>('desktop')
const paletteSearch = ref('')
const paletteCategory = ref<string>('Todos')
const mediaPickerOpen = ref(false)
const mediaKinds = ref<MediaKind[]>(['IMAGE'])
const mediaMultiple = ref(false)
const mediaTarget = ref<'image' | 'gallery' | 'video' | 'audio' | 'background'>('image')
const draggingType = ref<BuilderBlockType | null>(null)
const pluginBlocks = ref<PluginBuilderBlockDefinition[]>([])
const dynamicPluginOptions = ref<Record<string, Array<{ label: string; value: string }>>>({})

const pluginLibrary = computed(() => pluginBlocks.value.map(definition => ({
  type: `plugin:${definition.pluginId}:${definition.type}` as BuilderBlockType,
  label: definition.label,
  description: definition.description || `Bloque registrado por ${definition.extensionName || definition.pluginId}`,
  category: definition.category || 'Plugins',
  icon: definition.icon || 'plugin',
  pluginDefinition: definition
})))
const allLibrary = computed(() => [...BLOCK_LIBRARY, ...pluginLibrary.value])
const categories = computed(() => ['Todos', ...Array.from(new Set(allLibrary.value.map(item => item.category)))])
const palette = computed(() => allLibrary.value.filter(item => {
  const term = paletteSearch.value.trim().toLowerCase()
  const matchesCategory = paletteCategory.value === 'Todos' || item.category === paletteCategory.value
  const matchesTerm = !term || `${item.label} ${item.description}`.toLowerCase().includes(term)
  return matchesCategory && matchesTerm
}))

const selected = computed(() => selectedId.value ? findBlock(document.blocks, selectedId.value) : null)
const selectedParent = computed(() => selected.value ? findParent(document.blocks, selected.value.id) : null)
const canvasWidth = computed(() => previewMode.value === 'desktop' ? '100%' : previewMode.value === 'tablet' ? '768px' : '390px')
const previewModes: Array<'desktop' | 'tablet' | 'mobile'> = ['desktop', 'tablet', 'mobile']
const selectedRichText = computed({
  get: () => String(selected.value?.data.html || ''),
  set: (value: string) => { if (selected.value) selected.value.data.html = value }
})
const selectedStats = computed<Array<{ value: string; label: string }>>(() => {
  const items = selected.value?.data.items
  return Array.isArray(items) ? items as Array<{ value: string; label: string }> : []
})
const selectedPluginSchema = computed<Record<string, PluginFieldDefinition>>(() => {
  const schema = selected.value?.data._schema
  return schema && typeof schema === 'object' && !Array.isArray(schema)
    ? schema as Record<string, PluginFieldDefinition>
    : {}
})

const history = ref<string[]>([JSON.stringify(document)])
const historyIndex = ref(0)
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)
let applyingHistory = false
let historyTimer: ReturnType<typeof setTimeout> | null = null

function remember(snapshot: string) {
  if (history.value[historyIndex.value] === snapshot) return
  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push(snapshot)
  if (history.value.length > 60) history.value.shift()
  historyIndex.value = history.value.length - 1
}

watch(document, () => {
  const snapshot = JSON.stringify(document)
  emit('update:modelValue', snapshot)
  if (applyingHistory) return
  if (historyTimer) clearTimeout(historyTimer)
  historyTimer = setTimeout(() => remember(snapshot), 350)
}, { deep: true, flush: 'sync' })

watch(() => props.modelValue, value => {
  const serialized = JSON.stringify(document)
  if (value && value !== serialized) {
    applyingHistory = true
    Object.assign(document, parseDocument(value))
    applyingHistory = false
    remember(value)
  }
})

function restoreHistory(index: number) {
  const snapshot = history.value[index]
  if (!snapshot) return
  applyingHistory = true
  Object.assign(document, parseDocument(snapshot))
  historyIndex.value = index
  selectedId.value = null
  applyingHistory = false
  emit('update:modelValue', snapshot)
}

function undo() { if (canUndo.value) restoreHistory(historyIndex.value - 1) }
function redo() { if (canRedo.value) restoreHistory(historyIndex.value + 1) }

function findBlock(blocks: BuilderBlock[], id: string): BuilderBlock | null {
  for (const block of blocks) {
    if (block.id === id) return block
    const nested = block.children ? findBlock(block.children, id) : null
    if (nested) return nested
  }
  return null
}

function findParent(blocks: BuilderBlock[], id: string, parent: BuilderBlock | null = null): BuilderBlock | null {
  for (const block of blocks) {
    if (block.id === id) return parent
    if (block.children) {
      const found = findParent(block.children, id, block)
      if (found !== null || block.children.some(child => child.id === id)) return found
    }
  }
  return null
}

function parentArray(id: string, blocks: BuilderBlock[] = document.blocks): BuilderBlock[] | null {
  if (blocks.some(block => block.id === id)) return blocks
  for (const block of blocks) {
    if (block.children) {
      const result = parentArray(id, block.children)
      if (result) return result
    }
  }
  return null
}

function addBlock(type: BuilderBlockType, targetId?: string | null) {
  const pluginDefinition = pluginLibrary.value.find(item => item.type === type)?.pluginDefinition
  const block = pluginDefinition ? createPluginBlock(pluginDefinition) : createBlock(type)
  const target = targetId ? findBlock(document.blocks, targetId) : selected.value
  if (target && (target.type === 'section' || target.type === 'columns')) {
    target.children ||= []
    if (target.type === 'columns') block.data.column = 1
    target.children.push(block)
  } else {
    document.blocks.push(block)
  }
  selectedId.value = block.id
}

function addChild(id: string) {
  selectedId.value = id
  const type = window.prompt('Tipo de bloque: heading, richText, image, video, audio, button, iconBox', 'heading') as BuilderBlockType | null
  if (type && allLibrary.value.some(item => item.type === type)) addBlock(type, id)
}

function move(id: string, direction: -1 | 1) {
  const array = parentArray(id)
  if (!array) return
  const index = array.findIndex(item => item.id === id)
  const next = index + direction
  if (index < 0 || next < 0 || next >= array.length) return
  const [item] = array.splice(index, 1)
  array.splice(next, 0, item)
}

function duplicate(id: string) {
  const array = parentArray(id)
  const block = findBlock(document.blocks, id)
  if (!array || !block) return
  const index = array.findIndex(item => item.id === id)
  const copy = cloneBlock(block)
  array.splice(index + 1, 0, copy)
  selectedId.value = copy.id
}

function remove(id: string) {
  const array = parentArray(id)
  if (!array) return
  const index = array.findIndex(item => item.id === id)
  if (index >= 0) array.splice(index, 1)
  if (selectedId.value === id) selectedId.value = null
}

function onPaletteDrag(event: DragEvent, type: BuilderBlockType) {
  draggingType.value = type
  event.dataTransfer?.setData('application/x-trast-block', type)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy'
}

function onDrop(event: DragEvent, targetId?: string) {
  event.preventDefault()
  const type = (event.dataTransfer?.getData('application/x-trast-block') || draggingType.value) as BuilderBlockType
  if (type && allLibrary.value.some(item => item.type === type)) addBlock(type, targetId)
  draggingType.value = null
}

function openMedia(target: typeof mediaTarget.value, kinds: MediaKind[], multiple = false) {
  mediaTarget.value = target
  mediaKinds.value = kinds
  mediaMultiple.value = multiple
  mediaPickerOpen.value = true
}

function chooseMedia(items: MediaAsset[]) {
  const block = selected.value
  if (!block || !items.length) return
  if (mediaTarget.value === 'gallery') {
    block.data.images = items.map(item => ({ src: item.publicUrl, alt: item.altText || item.title || item.originalFilename, caption: item.caption || '' }))
  } else if (mediaTarget.value === 'background') {
    block.style.backgroundImage = items[0].publicUrl
  } else {
    const item = items[0]
    block.data.src = item.publicUrl
    block.data.contentType = item.contentType
    block.data.title = item.title || item.originalFilename
    if (mediaTarget.value === 'image') block.data.alt = item.altText || item.title || item.originalFilename
  }
  mediaPickerOpen.value = false
}

function layerList(): Array<{ id: string; label: string; depth: number; type: string }> {
  const result: Array<{ id: string; label: string; depth: number; type: string }> = []
  const visit = (blocks: BuilderBlock[], depth = 0) => {
    blocks.forEach(block => {
      result.push({ id: block.id, label: block.label, depth, type: block.type })
      if (block.children) visit(block.children, depth + 1)
    })
  }
  visit(document.blocks)
  return result
}

function numberValue(value: unknown, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function addStat() {
  if (!selected.value) return
  const items = Array.isArray(selected.value.data.items) ? selected.value.data.items : []
  items.push({ value: '0', label: 'Métrica' })
  selected.value.data.items = items
}

function pluginOptions(field: PluginFieldDefinition): Array<{ label: string; value: string }> {
  const staticOptions = (field.options || []).map(option => typeof option === 'string' ? { label: option, value: option } : option)
  if (staticOptions.length || !field.optionsEndpoint) return staticOptions
  return dynamicPluginOptions.value[field.optionsEndpoint] || []
}

function nestedValue(item: Record<string, unknown>, path?: string): unknown {
  if (!path) return undefined
  return path.split('.').reduce<unknown>((value, segment) => {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return undefined
    return (value as Record<string, unknown>)[segment]
  }, item)
}

function optionText(item: Record<string, unknown>, field: PluginFieldDefinition, kind: 'label' | 'value'): string {
  const explicit = nestedValue(item, kind === 'label' ? field.optionsLabel : field.optionsValue)
  if (explicit !== undefined && explicit !== null) return String(explicit)
  const candidates = kind === 'label'
    ? ['name', 'label', 'title', 'formKey', 'id']
    : ['formKey', 'value', 'id', 'name']
  for (const key of candidates) {
    const value = item[key]
    if (value !== undefined && value !== null) return String(value)
  }
  return ''
}

async function loadDynamicPluginOptions(definitions: PluginBuilderBlockDefinition[]) {
  const fields = definitions.flatMap(definition => Object.values(definition.schema || {}))
  const endpoints = Array.from(new Set(fields.map(field => field.optionsEndpoint).filter((value): value is string => Boolean(value))))
  await Promise.all(endpoints.map(async endpoint => {
    try {
      const response = await api<unknown>(endpoint)
      const rows = Array.isArray(response)
        ? response
        : response && typeof response === 'object' && Array.isArray((response as Record<string, unknown>).content)
          ? (response as Record<string, unknown>).content as unknown[]
          : []
      const matchingField = fields.find(field => field.optionsEndpoint === endpoint)
      dynamicPluginOptions.value[endpoint] = matchingField
        ? rows.filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object' && !Array.isArray(item))
            .map(item => ({ label: optionText(item, matchingField, 'label'), value: optionText(item, matchingField, 'value') }))
            .filter(option => option.label && option.value)
        : []
    } catch {
      dynamicPluginOptions.value[endpoint] = []
    }
  }))
}

function applyPattern(name: 'hero' | 'landing' | 'services') {
  const section = createBlock('section')
  if (name === 'hero' || name === 'landing') {
    section.style.backgroundColor = '#f5f3ff'
    section.style.borderRadius = 24
    const heading = createBlock('heading')
    heading.data.level = 1
    heading.data.text = 'Construya una presencia digital que genere resultados'
    heading.data.subtitle = 'Edite cada bloque, color, espacio y medio desde el constructor visual de TrastCMS.'
    heading.style.textAlign = 'center'
    const text = createBlock('richText')
    text.data.html = '<p style="text-align:center">Una página rápida, adaptable y mantenible, construida con Spring Boot y Vue.</p>'
    const button = createBlock('button')
    button.data.text = 'Comenzar ahora'
    button.data.href = '#contacto'
    button.style.textAlign = 'center'
    section.children = [heading, text, button]
  }
  if (name === 'services' || name === 'landing') {
    const services = createBlock('columns')
    services.data.columns = 3
    services.style.marginTop = 36
    services.children = ['Diseño visual', 'Medios profesionales', 'Extensiones'].map((title, index) => {
      const card = createBlock('iconBox')
      card.data.column = index + 1
      card.data.title = title
      card.data.text = index === 0
        ? 'Construya secciones y páginas con bloques reutilizables.'
        : index === 1
          ? 'Inserte imágenes, galerías, video y audio con reproductores modernos.'
          : 'Amplíe el CMS mediante plugins Java, hooks y webhooks.'
      return card
    })
    if (name === 'services') document.blocks.push(services)
    else document.blocks.push(section, services)
  } else {
    document.blocks.push(section)
  }
  selectedId.value = document.blocks.at(-1)?.id || null
}

function choosePattern(event: Event) {
  const select = event.target as HTMLSelectElement
  const value = select.value
  select.value = ''
  if (value === 'hero' || value === 'landing' || value === 'services') applyPattern(value)
}

function exportDocument() {
  const blob = new Blob([JSON.stringify(document, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const anchor = window.document.createElement('a')
  anchor.href = url
  anchor.download = `trastcms-page-${new Date().toISOString().slice(0, 10)}.json`
  anchor.click()
  URL.revokeObjectURL(url)
}

function importDocument(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const parsed = parseDocument(String(reader.result || ''))
    applyingHistory = true
    Object.assign(document, parsed)
    applyingHistory = false
    remember(JSON.stringify(parsed))
    selectedId.value = parsed.blocks[0]?.id || null
  }
  reader.readAsText(file)
}

onMounted(async () => {
  try {
    const contributions = await api<PluginContributions>('/api/admin/plugins/contributions')
    pluginBlocks.value = contributions.blocks || []
    await loadDynamicPluginOptions(pluginBlocks.value)
  } catch {
    pluginBlocks.value = []
  }
})
</script>

<template>
  <div class="mb-3 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm">
    <div class="flex flex-wrap items-center gap-1">
      <button type="button" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30" title="Deshacer" :disabled="!canUndo" @click="undo">↶</button>
      <button type="button" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30" title="Rehacer" :disabled="!canRedo" @click="redo">↷</button>
      <span class="mx-1 h-7 w-px bg-slate-200"></span>
      <button v-for="mode in previewModes" :key="mode" type="button" :class="['rounded-lg p-2 transition', previewMode === mode ? 'bg-violet-100 text-violet-700' : 'text-slate-500 hover:bg-slate-100']" @click="previewMode=mode"><UiIcon :name="mode" :size="18" /></button>
      <span class="mx-1 h-7 w-px bg-slate-200"></span>
      <select class="rounded-lg border border-slate-200 px-2 py-1.5 text-xs" value="" @change="choosePattern"><option value="">Insertar patrón…</option><option value="hero">Hero</option><option value="services">Servicios</option><option value="landing">Landing completa</option></select>
      <label class="rounded-lg p-2 text-slate-500 hover:bg-slate-100" title="Importar JSON"><input type="file" accept="application/json,.json" class="hidden" @change="importDocument"><UiIcon name="upload" :size="17"/></label>
      <button type="button" class="rounded-lg p-2 text-slate-500 hover:bg-slate-100" title="Exportar JSON" @click="exportDocument"><UiIcon name="document" :size="17"/></button>
    </div>
    <div class="flex items-center gap-3 text-xs text-slate-500">
      <span>{{ layerList().length }} bloques</span>
      <label class="flex items-center gap-2">Ancho global <input v-model.number="document.global.containerWidth" type="number" min="720" max="1920" class="w-24 rounded-lg border border-slate-200 px-2 py-1.5"></label>
    </div>
  </div>

  <div class="builder-shell">
    <aside class="builder-panel">
      <div class="sticky top-0 z-10 border-b border-slate-200 bg-white p-4">
        <h3 class="font-bold text-slate-950">Elementos</h3>
        <p class="mt-1 text-xs text-slate-500">Arrastre o pulse para agregar.</p>
        <div class="relative mt-3"><UiIcon name="search" :size="15" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input v-model="paletteSearch" class="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-2 text-sm" placeholder="Buscar elemento"></div>
        <div class="mt-3 flex flex-wrap gap-1"><button v-for="category in categories" :key="category" type="button" :class="['rounded-full px-2.5 py-1 text-[11px] font-semibold', paletteCategory === category ? 'bg-violet-600 text-white' : 'bg-slate-100 text-slate-600']" @click="paletteCategory=category">{{ category }}</button></div>
      </div>
      <div class="grid grid-cols-2 gap-2 p-3">
        <button
          v-for="item in palette"
          :key="item.type"
          type="button"
          draggable="true"
          class="group rounded-xl border border-slate-200 bg-white p-3 text-left transition hover:border-violet-300 hover:bg-violet-50 hover:shadow-sm"
          @dragstart="onPaletteDrag($event, item.type)"
          @click="addBlock(item.type)"
        >
          <span class="mb-2 grid h-9 w-9 place-items-center rounded-lg bg-slate-100 text-slate-600 group-hover:bg-violet-100 group-hover:text-violet-700"><UiIcon :name="item.icon" :size="18" /></span>
          <strong class="block text-xs text-slate-800">{{ item.label }}</strong>
          <small class="mt-1 block text-[10px] leading-4 text-slate-400">{{ item.description }}</small>
        </button>
      </div>
      <div class="border-t border-slate-200 p-4">
        <h4 class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">Capas</h4>
        <button v-for="layer in layerList()" :key="layer.id" type="button" :class="['flex w-full items-center gap-2 rounded-lg py-2 pr-2 text-left text-xs', selectedId === layer.id ? 'bg-violet-100 text-violet-700' : 'text-slate-600 hover:bg-slate-100']" :style="{ paddingLeft: `${8 + layer.depth * 14}px` }" @click="selectedId=layer.id"><UiIcon :name="layer.type === 'section' ? 'layers' : layer.type === 'image' ? 'image' : layer.type === 'video' ? 'video' : 'document'" :size="14"/><span class="truncate">{{ layer.label }}</span></button>
      </div>
    </aside>

    <main class="builder-canvas-wrap" @dragover.prevent @drop="onDrop($event)">
      <div class="builder-canvas" :style="{ width: canvasWidth, maxWidth: `${document.global.containerWidth}px` }">
        <BlockRenderer
          v-for="block in document.blocks"
          :key="block.id"
          :block="block"
          editing
          :selected="selectedId === block.id"
          @select="selectedId=$event"
          @move-up="move($event,-1)"
          @move-down="move($event,1)"
          @duplicate="duplicate"
          @remove="remove"
          @add-child="addChild"
        />
        <div v-if="!document.blocks.length" class="builder-empty" @dragover.prevent @drop="onDrop($event)">
          <div class="text-center"><span class="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-violet-100 text-violet-700"><UiIcon name="layers" :size="28" /></span><h3 class="font-bold text-slate-700">Construya su página visualmente</h3><p class="mt-2 max-w-sm text-sm">Arrastre elementos desde la izquierda o comience con una sección.</p><button type="button" class="mt-4 rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white" @click="addBlock('section')">Agregar primera sección</button></div>
        </div>
      </div>
    </main>

    <aside class="builder-panel right">
      <div v-if="selected" class="p-4">
        <div class="mb-4 flex items-center justify-between"><div><span class="text-[10px] font-bold uppercase tracking-wider text-violet-600">{{ selected.type }}</span><h3 class="font-bold text-slate-950">{{ selected.label }}</h3></div><button type="button" class="rounded-lg p-2 text-slate-400 hover:bg-slate-100" @click="selectedId=null"><UiIcon name="close" :size="17" /></button></div>

        <div class="space-y-5">
          <section class="space-y-3">
            <h4 class="text-xs font-bold uppercase tracking-wider text-slate-500">Contenido</h4>
            <template v-if="selected.type === 'section'">
              <label class="block text-xs font-semibold text-slate-600">Ancho<select v-model="selected.data.width" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option value="boxed">Contenido limitado</option><option value="full">Ancho completo</option></select></label>
              <button type="button" class="w-full rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-semibold text-violet-700" @click="addBlock('heading', selected.id)">Agregar elemento dentro</button>
            </template>
            <template v-else-if="selected.type === 'columns'">
              <label class="block text-xs font-semibold text-slate-600">Columnas<input v-model.number="selected.data.columns" type="range" min="1" max="4" class="mt-2 w-full"><span class="float-right">{{ selected.data.columns }}</span></label>
              <label class="block text-xs font-semibold text-slate-600">Separación<input v-model.number="selected.data.gap" type="number" min="0" max="100" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <button type="button" class="w-full rounded-lg border border-violet-200 bg-violet-50 px-3 py-2 text-sm font-semibold text-violet-700" @click="addBlock('heading', selected.id)">Agregar elemento a columna 1</button>
            </template>
            <template v-else-if="selected.type === 'heading'">
              <label class="block text-xs font-semibold text-slate-600">Texto<textarea v-model="selected.data.text" rows="3" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></textarea></label>
              <label class="block text-xs font-semibold text-slate-600">Subtítulo<input v-model="selected.data.subtitle" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Nivel<select v-model.number="selected.data.level" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option :value="1">H1</option><option :value="2">H2</option><option :value="3">H3</option><option :value="4">H4</option></select></label>
            </template>
            <template v-else-if="selected.type === 'richText'">
              <RichTextEditor v-model="selectedRichText" :min-height="220" />
            </template>
            <template v-else-if="selected.type === 'image'">
              <button type="button" class="w-full rounded-lg bg-violet-600 px-3 py-2 text-sm font-semibold text-white" @click="openMedia('image',['IMAGE'])">Seleccionar imagen</button>
              <label class="block text-xs font-semibold text-slate-600">URL<input v-model="selected.data.src" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Texto alternativo<input v-model="selected.data.alt" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Altura<input v-model.number="selected.data.height" type="number" min="100" max="1200" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
            </template>
            <template v-else-if="selected.type === 'gallery'">
              <button type="button" class="w-full rounded-lg bg-violet-600 px-3 py-2 text-sm font-semibold text-white" @click="openMedia('gallery',['IMAGE'],true)">Seleccionar imágenes</button>
              <label class="block text-xs font-semibold text-slate-600">Columnas<input v-model.number="selected.data.columns" type="range" min="1" max="6" class="mt-2 w-full"></label>
            </template>
            <template v-else-if="selected.type === 'video'">
              <button type="button" class="w-full rounded-lg bg-violet-600 px-3 py-2 text-sm font-semibold text-white" @click="openMedia('video',['VIDEO'])">Seleccionar video</button>
              <label class="block text-xs font-semibold text-slate-600">URL<input v-model="selected.data.src" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Poster<input v-model="selected.data.poster" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
            </template>
            <template v-else-if="selected.type === 'audio'">
              <button type="button" class="w-full rounded-lg bg-violet-600 px-3 py-2 text-sm font-semibold text-white" @click="openMedia('audio',['AUDIO'])">Seleccionar audio</button>
              <label class="block text-xs font-semibold text-slate-600">URL<input v-model="selected.data.src" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
            </template>
            <template v-else-if="selected.type === 'button'">
              <label class="block text-xs font-semibold text-slate-600">Texto<input v-model="selected.data.text" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Enlace<input v-model="selected.data.href" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
              <label class="block text-xs font-semibold text-slate-600">Estilo<select v-model="selected.data.variant" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option value="primary">Principal</option><option value="outline">Contorno</option><option value="ghost">Limpio</option></select></label>
            </template>
            <template v-else-if="selected.type === 'quote'">
              <label class="block text-xs font-semibold text-slate-600">Cita<textarea v-model="selected.data.quote" rows="4" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></textarea></label><label class="block text-xs font-semibold text-slate-600">Autor<input v-model="selected.data.author" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label><label class="block text-xs font-semibold text-slate-600">Cargo/empresa<input v-model="selected.data.role" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label>
            </template>
            <template v-else-if="selected.type === 'iconBox'">
              <label class="block text-xs font-semibold text-slate-600">Icono<input v-model="selected.data.icon" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label><label class="block text-xs font-semibold text-slate-600">Título<input v-model="selected.data.title" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></label><label class="block text-xs font-semibold text-slate-600">Texto<textarea v-model="selected.data.text" rows="4" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></textarea></label>
            </template>
            <template v-else-if="selected.type === 'stats'">
              <div v-for="(item,index) in selectedStats" :key="index" class="grid grid-cols-2 gap-2"><input v-model="item.value" class="rounded-lg border border-slate-200 p-2" placeholder="Valor"><input v-model="item.label" class="rounded-lg border border-slate-200 p-2" placeholder="Etiqueta"></div>
              <button type="button" class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" @click="addStat">Agregar métrica</button>
            </template>
            <template v-else-if="String(selected.type).startsWith('plugin:')">
              <div class="rounded-xl border border-violet-200 bg-violet-50 p-3 text-xs text-violet-700"><strong>Bloque de plugin</strong><span class="mt-1 block">{{ selected.data._pluginId }} · {{ selected.data._pluginType }}</span></div>
              <div v-for="(field,key) in selectedPluginSchema" :key="key" class="block text-xs font-semibold text-slate-600">
                <span class="block">{{ field.label || key }}</span>
                <input v-if="field.type === 'text' || field.type === 'color' || field.type === 'number'" v-model="selected.data[key]" :type="field.type" class="mt-1 w-full rounded-lg border border-slate-200 p-2">
                <textarea v-else-if="field.type === 'textarea'" v-model="selected.data[key]" rows="4" class="mt-1 w-full rounded-lg border border-slate-200 p-2"></textarea>
                <select v-else-if="field.type === 'select'" v-model="selected.data[key]" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option v-for="option in pluginOptions(field)" :key="option.value" :value="option.value">{{ option.label }}</option></select>
                <label v-else-if="field.type === 'boolean'" class="mt-2 flex items-center gap-2"><input v-model="selected.data[key]" type="checkbox"><span>Habilitado</span></label>
              </div>
            </template>
            <template v-else-if="selected.type === 'spacer'"><label class="block text-xs font-semibold text-slate-600">Altura<input v-model.number="selected.data.height" type="range" min="8" max="300" class="mt-2 w-full"><span class="float-right">{{ selected.data.height }} px</span></label></template>
            <template v-else-if="selected.type === 'html'"><label class="block text-xs font-semibold text-slate-600">HTML<textarea v-model="selected.data.html" rows="12" class="mt-1 w-full rounded-lg border border-slate-200 p-2 font-mono text-xs"></textarea></label></template>
            <label v-if="selectedParent?.type === 'columns'" class="block text-xs font-semibold text-slate-600">Columna<select v-model.number="selected.data.column" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option v-for="column in Number(selectedParent.data.columns || 2)" :key="column" :value="column">Columna {{ column }}</option></select></label>
          </section>

          <section class="space-y-3 border-t border-slate-200 pt-4">
            <h4 class="text-xs font-bold uppercase tracking-wider text-slate-500">Estilo</h4>
            <div class="grid grid-cols-2 gap-2"><label class="text-[11px] text-slate-500">Fondo<input v-model="selected.style.backgroundColor" type="color" class="mt-1 h-10 w-full rounded border border-slate-200 p-1"></label><label class="text-[11px] text-slate-500">Texto<input v-model="selected.style.color" type="color" class="mt-1 h-10 w-full rounded border border-slate-200 p-1"></label></div>
            <button type="button" class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" @click="openMedia('background',['IMAGE'])">Imagen de fondo</button>
            <label class="block text-xs font-semibold text-slate-600">Alineación<select v-model="selected.style.textAlign" class="mt-1 w-full rounded-lg border border-slate-200 p-2"><option value="left">Izquierda</option><option value="center">Centro</option><option value="right">Derecha</option></select></label>
            <div class="grid grid-cols-4 gap-1">
              <label class="text-[10px] text-slate-500">P. S<input v-model.number="selected.style.paddingTop" type="number" min="0" max="300" class="mt-1 w-full rounded border border-slate-200 p-1.5 text-xs"></label>
              <label class="text-[10px] text-slate-500">P. D<input v-model.number="selected.style.paddingRight" type="number" min="0" max="300" class="mt-1 w-full rounded border border-slate-200 p-1.5 text-xs"></label>
              <label class="text-[10px] text-slate-500">P. I<input v-model.number="selected.style.paddingBottom" type="number" min="0" max="300" class="mt-1 w-full rounded border border-slate-200 p-1.5 text-xs"></label>
              <label class="text-[10px] text-slate-500">P. Iz<input v-model.number="selected.style.paddingLeft" type="number" min="0" max="300" class="mt-1 w-full rounded border border-slate-200 p-1.5 text-xs"></label>
            </div>
            <label class="block text-xs font-semibold text-slate-600">Radio de borde<input v-model.number="selected.style.borderRadius" type="range" min="0" max="80" class="mt-2 w-full"></label>
            <label class="block text-xs font-semibold text-slate-600">Clase CSS<input v-model="selected.style.cssClass" class="mt-1 w-full rounded-lg border border-slate-200 p-2" placeholder="mi-clase"></label>
            <label class="block text-xs font-semibold text-slate-600">ID de ancla<input v-model="selected.style.anchorId" class="mt-1 w-full rounded-lg border border-slate-200 p-2" placeholder="contacto"></label>
          </section>

          <section class="grid grid-cols-2 gap-2 border-t border-slate-200 pt-4"><button type="button" class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold" @click="duplicate(selected.id)">Duplicar</button><button type="button" class="rounded-lg border border-red-200 px-3 py-2 text-xs font-semibold text-red-600" @click="remove(selected.id)">Eliminar</button></section>
        </div>
      </div>
      <div v-else class="grid h-full place-items-center p-8 text-center text-sm text-slate-500"><div><UiIcon name="settings" :size="36" class="mx-auto mb-3 text-slate-300"/><p>Seleccione un bloque para modificar contenido, diseño, espaciado y propiedades avanzadas.</p></div></div>
    </aside>
  </div>

  <MediaPickerModal :open="mediaPickerOpen" :kinds="mediaKinds" :multiple="mediaMultiple" title="Seleccionar medios para el constructor" @close="mediaPickerOpen=false" @select="chooseMedia" />
</template>
