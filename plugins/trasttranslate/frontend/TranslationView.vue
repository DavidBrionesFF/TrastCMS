<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import { languageCatalog, type TranslationSettings } from './translation'

const toast = useToastStore()
const loading = ref(true)
const saving = ref(false)
const error = ref('')

const form = reactive<TranslationSettings>({
  enabled: true,
  provider: 'GOOGLE_ELEMENT',
  sourceLanguage: 'en',
  enabledLanguages: ['en', 'es', 'fr', 'de', 'it', 'pt', 'nl'],
  placement: 'TOPBAR',
  displayStyle: 'COMPACT',
  showFlags: true,
  showNativeNames: true,
  rememberSelection: true,
  buttonLabel: 'Language',
  floatingOffsetX: 24,
  floatingOffsetY: 88,
  excludedPaths: ['/admin', '/api']
})

const excludedPathsText = computed({
  get: () => form.excludedPaths.join('\n'),
  set: value => { form.excludedPaths = value.split(/\r?\n|,/).map(item => item.trim()).filter(Boolean) }
})

function languageEnabled(code: string) {
  return form.enabledLanguages.includes(code)
}

function toggleLanguage(code: string, checked: boolean) {
  if (code === form.sourceLanguage) return
  if (checked) form.enabledLanguages = Array.from(new Set([...form.enabledLanguages, code]))
  else form.enabledLanguages = form.enabledLanguages.filter(item => item !== code)
}

function changeSourceLanguage(code: string) {
  form.sourceLanguage = code
  if (!form.enabledLanguages.includes(code)) form.enabledLanguages.unshift(code)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    Object.assign(form, await api<TranslationSettings>('/api/admin/translation/settings'))
  } catch (exception) {
    error.value = exception instanceof ApiError ? exception.message : 'No se pudo cargar la configuración.'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const saved = await api<TranslationSettings>('/api/admin/translation/settings', {
      method: 'PUT',
      body: JSON.stringify(form)
    })
    Object.assign(form, saved)
    toast.success('Configuración guardada', 'El selector público utilizará esta configuración.')
  } catch (exception) {
    error.value = exception instanceof ApiError ? exception.message : 'No se pudo guardar la configuración.'
    toast.error('No se pudo guardar', error.value)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <AdminPageHeader eyebrow="PLUGIN DE TRADUCCIÓN" title="TrastTranslate" description="Configure los idiomas del sitio público y la posición del selector. El administrador permanece sin traducción." />

    <div v-if="error" class="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{{ error }}</div>
    <div v-if="loading" class="grid min-h-72 place-items-center rounded-3xl border border-slate-200 bg-white text-slate-500">Cargando configuración…</div>

    <form v-else class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,.65fr)]" @submit.prevent="save">
      <div class="space-y-6">
        <section class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div><h2 class="text-xl font-bold text-slate-950">Estado y proveedor</h2><p class="mt-1 text-sm text-slate-500">La traducción se carga únicamente en el sitio público.</p></div>
            <label class="flex items-center gap-3 rounded-2xl bg-slate-100 px-4 py-3 text-sm font-bold text-slate-700"><input v-model="form.enabled" type="checkbox" class="h-5 w-5">Plugin visible</label>
          </div>
          <div class="mt-6 grid gap-5 md:grid-cols-2">
            <label class="grid gap-2 text-sm font-bold text-slate-700">Proveedor
              <select v-model="form.provider" class="rounded-xl border border-slate-200 px-4 py-3 font-medium">
                <option value="GOOGLE_ELEMENT">Google Translate — elemento integrado</option>
                <option value="GOOGLE_REDIRECT">Google Translate — redirección externa</option>
              </select>
            </label>
            <label class="grid gap-2 text-sm font-bold text-slate-700">Idioma original
              <select :value="form.sourceLanguage" class="rounded-xl border border-slate-200 px-4 py-3 font-medium" @change="changeSourceLanguage(($event.target as HTMLSelectElement).value)">
                <option v-for="language in languageCatalog" :key="language.code" :value="language.code">{{ language.flag }} {{ language.nativeName }}</option>
              </select>
            </label>
          </div>
          <div class="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            <strong>Google Element es una integración heredada.</strong> Para sitios comerciales que requieran SLA, control de glosarios o traducciones persistentes, conviene implementar Google Cloud Translation API como proveedor adicional.
          </div>
        </section>

        <section class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 class="text-xl font-bold text-slate-950">Idiomas disponibles</h2>
          <p class="mt-1 text-sm text-slate-500">El idioma original siempre permanece habilitado.</p>
          <div class="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <label v-for="language in languageCatalog" :key="language.code" class="flex items-center gap-3 rounded-2xl border border-slate-200 p-4 text-sm font-semibold text-slate-700 hover:border-violet-300">
              <input :checked="languageEnabled(language.code)" :disabled="language.code === form.sourceLanguage" type="checkbox" class="h-4 w-4" @change="toggleLanguage(language.code, ($event.target as HTMLInputElement).checked)">
              <span class="text-xl">{{ language.flag }}</span><span>{{ language.nativeName }}<small class="block font-normal text-slate-400">{{ language.englishName }}</small></span>
            </label>
          </div>
        </section>

        <section class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 class="text-xl font-bold text-slate-950">Presentación</h2>
          <div class="mt-5 grid gap-5 md:grid-cols-2">
            <label class="grid gap-2 text-sm font-bold text-slate-700">Ubicación
              <select v-model="form.placement" class="rounded-xl border border-slate-200 px-4 py-3 font-medium">
                <option value="TOPBAR">Barra superior</option><option value="HEADER">Encabezado</option><option value="FOOTER">Pie de página</option><option value="FLOATING_LEFT">Flotante a la izquierda</option><option value="FLOATING_RIGHT">Flotante a la derecha</option><option value="BLOCK_ONLY">Solo mediante bloque</option>
              </select>
            </label>
            <label class="grid gap-2 text-sm font-bold text-slate-700">Estilo
              <select v-model="form.displayStyle" class="rounded-xl border border-slate-200 px-4 py-3 font-medium"><option value="COMPACT">Compacto</option><option value="DROPDOWN">Desplegable con etiqueta</option><option value="BUTTONS">Botones por idioma</option></select>
            </label>
            <label class="grid gap-2 text-sm font-bold text-slate-700">Etiqueta<input v-model="form.buttonLabel" maxlength="80" class="rounded-xl border border-slate-200 px-4 py-3 font-medium"></label>
            <div class="grid grid-cols-2 gap-3">
              <label class="grid gap-2 text-sm font-bold text-slate-700">Separación X<input v-model.number="form.floatingOffsetX" type="number" min="0" max="300" class="rounded-xl border border-slate-200 px-4 py-3 font-medium"></label>
              <label class="grid gap-2 text-sm font-bold text-slate-700">Separación Y<input v-model.number="form.floatingOffsetY" type="number" min="0" max="300" class="rounded-xl border border-slate-200 px-4 py-3 font-medium"></label>
            </div>
          </div>
          <div class="mt-5 grid gap-3 sm:grid-cols-3">
            <label class="flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold"><input v-model="form.showFlags" type="checkbox">Mostrar banderas</label>
            <label class="flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold"><input v-model="form.showNativeNames" type="checkbox">Nombres nativos</label>
            <label class="flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold"><input v-model="form.rememberSelection" type="checkbox">Recordar idioma</label>
          </div>
        </section>
      </div>

      <aside class="space-y-6">
        <section class="sticky top-24 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 class="text-lg font-bold text-slate-950">Rutas excluidas</h2>
          <p class="mt-1 text-sm text-slate-500">Una ruta por línea. El panel administrativo ya está excluido por defecto.</p>
          <textarea v-model="excludedPathsText" rows="7" class="mt-4 w-full rounded-xl border border-slate-200 px-4 py-3 font-mono text-sm" placeholder="/admin&#10;/api"></textarea>
          <button type="submit" :disabled="saving" class="mt-6 w-full rounded-xl bg-violet-600 px-5 py-3 font-bold text-white hover:bg-violet-700 disabled:opacity-50">{{ saving ? 'Guardando…' : 'Guardar configuración' }}</button>
        </section>
      </aside>
    </form>
  </div>
</template>
