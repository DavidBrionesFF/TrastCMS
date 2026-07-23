<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import { api, ApiError } from '@/services/api'
import type { PluginAdminFieldDefinition, PluginAdminMenuItem, PluginAdminSection } from '@/types'

const route = useRoute()
const loading = ref(true)
const saving = ref<string | null>(null)
const error = ref('')
const result = ref<Record<string, unknown> | null>(null)
const page = ref<PluginAdminMenuItem | null>(null)
type PluginFormValue = string | number | boolean

function scalarValue(value: PluginFormValue | undefined): string | number {
  return typeof value === 'number' ? value : String(value ?? '')
}

function booleanValue(value: PluginFormValue | undefined): boolean {
  return value === true
}

function setTextValue(section: PluginAdminSection, index: number, fieldName: string, event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
  values[sectionKey(section, index)][fieldName] = target.value
}

function setBooleanValue(section: PluginAdminSection, index: number, fieldName: string, event: Event) {
  const target = event.target as HTMLInputElement
  values[sectionKey(section, index)][fieldName] = target.checked
}

const values = reactive<Record<string, Record<string, PluginFormValue>>>({})

const pluginId = computed(() => String(route.params.pluginId || ''))
const pageId = computed(() => String(route.params.pageId || ''))

function sectionKey(section: PluginAdminSection, index: number) {
  return section.id || `section-${index}`
}

function options(field: PluginAdminFieldDefinition) {
  return (field.options || []).map(option => typeof option === 'string'
    ? { label: option, value: option }
    : option)
}

function initializeForms() {
  Object.keys(values).forEach(key => delete values[key])
  ;(page.value?.sections || []).forEach((section, index) => {
    if (section.type !== 'form') return
    const key = sectionKey(section, index)
    values[key] = {}
    ;(section.fields || []).forEach(field => {
      values[key][field.name] = field.default ?? (field.type === 'boolean' ? false : '')
    })
  })
}

async function load() {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    page.value = await api<PluginAdminMenuItem>(
      `/api/admin/plugins/${encodeURIComponent(pluginId.value)}/pages/${encodeURIComponent(pageId.value)}`
    )
    initializeForms()
  } catch (exception) {
    error.value = exception instanceof ApiError ? exception.message : 'No se pudo cargar la página del plugin.'
    page.value = null
  } finally {
    loading.value = false
  }
}

async function execute(section: PluginAdminSection, index: number) {
  if (!section.action) return
  const key = sectionKey(section, index)
  saving.value = key
  error.value = ''
  result.value = null
  try {
    result.value = await api<Record<string, unknown>>(
      `/api/admin/plugins/${encodeURIComponent(pluginId.value)}/actions/${encodeURIComponent(section.action)}`,
      { method: 'POST', body: JSON.stringify(values[key] || {}) }
    )
  } catch (exception) {
    error.value = exception instanceof ApiError ? exception.message : 'El plugin no pudo ejecutar la acción.'
  } finally {
    saving.value = null
  }
}

function safeLink(item: Record<string, unknown>) {
  const href = String(item.href || '#')
  return href.startsWith('/') || /^https:\/\//i.test(href) ? href : '#'
}

watch(() => route.fullPath, load)
onMounted(load)
</script>

<template>
  <div>
    <AdminPageHeader
      :eyebrow="page?.extensionName || 'Plugin Java'"
      :title="page?.label || 'Extensión instalada'"
      :description="page?.description || 'Página administrativa declarada por un plugin de TrastCMS.'"
    >
      <template #actions>
        <RouterLink :to="{ name: 'plugins' }" class="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:border-violet-200 hover:text-violet-700">Gestionar plugins</RouterLink>
      </template>
    </AdminPageHeader>

    <div v-if="loading" class="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white text-slate-500">Cargando extensión…</div>
    <div v-else-if="!page" class="grid min-h-72 place-items-center rounded-2xl border border-red-200 bg-white text-center"><div><UiIcon name="plugin" :size="44" class="mx-auto mb-3 text-red-300"/><h3 class="font-bold text-slate-900">No se pudo abrir el plugin</h3><p class="mt-2 text-sm text-red-600">{{ error }}</p></div></div>

    <div v-else class="space-y-5">
      <div class="flex flex-wrap items-center gap-2 rounded-2xl border border-violet-200 bg-violet-50 px-4 py-3 text-xs text-violet-800">
        <UiIcon name="plugin" :size="17" />
        <strong>{{ page.pluginId }}</strong>
        <span>v{{ page.pluginVersion }}</span>
        <span class="text-violet-500">{{ page.extensionName }}</span>
      </div>

      <div v-if="error" class="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ error }}</div>
      <div v-if="result" class="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
        <strong class="block">Acción completada</strong>
        <pre class="mt-2 overflow-auto whitespace-pre-wrap text-xs">{{ JSON.stringify(result, null, 2) }}</pre>
      </div>

      <template v-for="(section,index) in page.sections || []" :key="sectionKey(section,index)">
        <section v-if="section.type === 'notice'" :class="['rounded-2xl border p-5', section.tone === 'success' ? 'border-emerald-200 bg-emerald-50' : section.tone === 'warning' ? 'border-amber-200 bg-amber-50' : section.tone === 'danger' ? 'border-red-200 bg-red-50' : 'border-sky-200 bg-sky-50']">
          <h3 class="font-bold text-slate-900">{{ section.title }}</h3>
          <p class="mt-1 text-sm text-slate-600">{{ section.description }}</p>
        </section>

        <section v-else-if="section.type === 'stats'" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article v-for="(item,itemIndex) in section.items || []" :key="itemIndex" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <span class="text-xs font-semibold uppercase tracking-wider text-slate-400">{{ item.label }}</span>
            <strong class="mt-2 block text-3xl text-slate-950">{{ item.value }}</strong>
            <small v-if="item.description" class="mt-1 block text-slate-500">{{ item.description }}</small>
          </article>
        </section>

        <section v-else-if="section.type === 'links'" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 class="text-lg font-bold text-slate-950">{{ section.title }}</h3>
          <p v-if="section.description" class="mt-1 text-sm text-slate-500">{{ section.description }}</p>
          <div class="mt-4 grid gap-3 md:grid-cols-2">
            <a v-for="(item,itemIndex) in section.items || []" :key="itemIndex" :href="safeLink(item)" :target="String(item.href || '').startsWith('https://') ? '_blank' : undefined" rel="noopener noreferrer" class="flex items-center justify-between rounded-xl border border-slate-200 p-4 hover:border-violet-300 hover:bg-violet-50">
              <span><strong class="block text-sm text-slate-800">{{ item.label }}</strong><small class="mt-1 block text-slate-500">{{ item.description }}</small></span><UiIcon name="external" :size="17" class="text-violet-600"/>
            </a>
          </div>
        </section>

        <section v-else-if="section.type === 'form'" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 class="text-lg font-bold text-slate-950">{{ section.title }}</h3>
          <p v-if="section.description" class="mt-1 text-sm text-slate-500">{{ section.description }}</p>
          <form class="mt-5 grid gap-4 md:grid-cols-2" @submit.prevent="execute(section,index)">
            <div v-for="field in section.fields || []" :key="field.name" :class="field.type === 'textarea' ? 'md:col-span-2' : ''">
              <label class="block text-sm font-semibold text-slate-700">{{ field.label }}<span v-if="field.required" class="text-red-500"> *</span></label>
              <small v-if="field.description" class="mt-1 block text-xs text-slate-400">{{ field.description }}</small>
              <input v-if="field.type === 'text' || field.type === 'number' || field.type === 'color'" :value="scalarValue(values[sectionKey(section,index)][field.name])" :type="field.type" :required="field.required" :placeholder="field.placeholder" class="mt-2 w-full rounded-xl border border-slate-200 p-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" @input="setTextValue(section,index,field.name,$event)">
              <textarea v-else-if="field.type === 'textarea'" :value="scalarValue(values[sectionKey(section,index)][field.name])" rows="5" :required="field.required" :placeholder="field.placeholder" class="mt-2 w-full rounded-xl border border-slate-200 p-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" @input="setTextValue(section,index,field.name,$event)"></textarea>
              <select v-else-if="field.type === 'select'" :value="scalarValue(values[sectionKey(section,index)][field.name])" :required="field.required" class="mt-2 w-full rounded-xl border border-slate-200 bg-white p-3" @change="setTextValue(section,index,field.name,$event)"><option v-for="option in options(field)" :key="option.value" :value="option.value">{{ option.label }}</option></select>
              <label v-else-if="field.type === 'boolean'" class="mt-3 flex items-center gap-2 text-sm font-medium text-slate-600"><input :checked="booleanValue(values[sectionKey(section,index)][field.name])" type="checkbox" class="h-4 w-4" @change="setBooleanValue(section,index,field.name,$event)"><span>Habilitado</span></label>
            </div>
            <div class="md:col-span-2"><button type="submit" :disabled="saving === sectionKey(section,index)" class="rounded-xl bg-violet-600 px-5 py-3 font-semibold text-white hover:bg-violet-700 disabled:opacity-50">{{ saving === sectionKey(section,index) ? 'Procesando…' : section.submitLabel || 'Guardar' }}</button></div>
          </form>
        </section>
      </template>

      <div v-if="!(page.sections || []).length" class="grid min-h-64 place-items-center rounded-2xl border border-slate-200 bg-white text-center"><div><UiIcon name="plugin" :size="44" class="mx-auto mb-3 text-slate-300"/><h3 class="font-bold text-slate-800">El plugin no declaró componentes</h3><p class="mt-1 text-sm text-slate-500">Agregue secciones declarativas en <code>adminMenuItems()</code>.</p></div></div>
    </div>
  </div>
</template>
