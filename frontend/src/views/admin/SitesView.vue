<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { setSelectedSite } from '@/services/site-context'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import type { BundledPlugin, CmsSite, SiteEndpointRequest, SiteRequest } from '@/types'

const sites = ref<CmsSite[]>([])
const bundled = ref<BundledPlugin[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const editing = ref<CmsSite | null>(null)
const showForm = ref(false)
const pluginSite = ref<CmsSite | null>(null)
const selectedPlugins = ref<string[]>([])

const blueprints = [
  { value: 'ACADEMY', label: 'Academia', detail: 'Cursos, pagos, cuentas y traducción.' },
  { value: 'SAAS', label: 'Tienda + SaaS', detail: 'Tienda, licencias, planes, pagos y newsletter.' },
  { value: 'TRAVEL', label: 'Turismo', detail: 'Sitio turístico nuevo con Aurora.' },
  { value: 'EMPTY', label: 'Sitio vacío', detail: 'Base limpia con cuenta y traducción.' }
]

const form = reactive<SiteRequest>({
  siteKey: '', name: '', locale: 'es-HN', timezone: 'America/Tegucigalpa', status: 'ACTIVE', blueprint: 'EMPTY',
  endpoints: [{ host: '', port: 8080, scheme: 'http', primaryEndpoint: true, active: true }]
})

const activeSites = computed(() => sites.value.filter(site => site.status === 'ACTIVE').length)
const totalUsers = computed(() => sites.value.reduce((sum, site) => sum + Number(site.users || 0), 0))
const uniquePlugins = computed(() => new Set(sites.value.flatMap(site => site.enabledPlugins || [])).size)

async function load() {
  loading.value = true; error.value = ''
  try {
    const [siteData, pluginData] = await Promise.all([
      api<CmsSite[]>('/api/admin/sites'),
      api<BundledPlugin[]>('/api/admin/plugins/bundled').catch(() => [])
    ])
    sites.value = siteData
    bundled.value = pluginData
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar la red de sitios'
  } finally { loading.value = false }
}

function resetForm() {
  editing.value = null
  Object.assign(form, {
    siteKey: '', name: '', locale: 'es-HN', timezone: 'America/Tegucigalpa', status: 'ACTIVE', blueprint: 'EMPTY',
    endpoints: [{ host: '', port: 8080, scheme: 'http', primaryEndpoint: true, active: true }]
  })
}

function createSite() { resetForm(); showForm.value = true }
function editSite(site: CmsSite) {
  editing.value = site
  Object.assign(form, {
    siteKey: site.siteKey, name: site.name, locale: site.locale, timezone: site.timezone, status: site.status, blueprint: null,
    endpoints: site.endpoints.map(endpoint => ({ host: endpoint.host, port: endpoint.port, scheme: endpoint.scheme, primaryEndpoint: endpoint.primaryEndpoint, active: endpoint.active }))
  })
  if (!form.endpoints.length) form.endpoints.push({ host: `${site.siteKey}.localhost`, port: 8080, scheme: 'http', primaryEndpoint: true, active: true })
  showForm.value = true
}
function addEndpoint() { form.endpoints.push({ host: '', port: 443, scheme: 'https', primaryEndpoint: false, active: true }) }
function removeEndpoint(index: number) { if (form.endpoints.length > 1) form.endpoints.splice(index, 1) }
function makePrimary(index: number) { form.endpoints.forEach((endpoint, i) => endpoint.primaryEndpoint = i === index) }

async function saveSite() {
  saving.value = true; error.value = ''; notice.value = ''
  try {
    const body = JSON.stringify(form)
    if (editing.value) await api(`/api/admin/sites/${editing.value.id}`, { method: 'PUT', body })
    else await api('/api/admin/sites', { method: 'POST', body })
    notice.value = editing.value ? 'Sitio actualizado.' : 'Sitio creado y provisionado.'
    showForm.value = false
    resetForm()
    await load()
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo guardar el sitio' }
  finally { saving.value = false }
}

function editPlugins(site: CmsSite) {
  pluginSite.value = site
  selectedPlugins.value = [...(site.enabledPlugins || [])]
}
function togglePlugin(id: string) {
  const index = selectedPlugins.value.indexOf(id)
  if (index >= 0) selectedPlugins.value.splice(index, 1)
  else selectedPlugins.value.push(id)
}
async function savePlugins() {
  if (!pluginSite.value) return
  saving.value = true; error.value = ''
  try {
    await api(`/api/admin/sites/${pluginSite.value.id}/plugins`, { method: 'PUT', body: JSON.stringify({ enabledPluginIds: selectedPlugins.value }) })
    notice.value = `Plugins actualizados para ${pluginSite.value.name}.`
    pluginSite.value = null
    await load()
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudieron actualizar los plugins' }
  finally { saving.value = false }
}
async function reprovision(site: CmsSite, blueprint: string) {
  if (!confirm(`¿Aplicar nuevamente el blueprint ${blueprint} a ${site.name}? Esto restaurará el tema y contenido inicial del blueprint.`)) return
  saving.value = true; error.value = ''
  try {
    await api(`/api/admin/sites/${site.id}/provision`, { method: 'POST', body: JSON.stringify({ blueprint }) })
    notice.value = `${site.name} fue reconfigurado con ${blueprint}.`
    await load()
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo reconfigurar el sitio' }
  finally { saving.value = false }
}
function enter(site: CmsSite) {
  setSelectedSite(site.siteKey)
  window.location.assign('/admin')
}
function pluginName(id: string) { return bundled.value.find(plugin => plugin.pluginId === id)?.name || id }
function applyBlueprintFromSelect(site: CmsSite, event: Event) {
  const select = event.target as HTMLSelectElement
  const value = select.value
  if (!value) return
  void reprovision(site, value).finally(() => { select.value = '' })
}
</script>

<template>
  <div class="space-y-7">
    <AdminPageHeader title="Red de sitios" eyebrow="Multisite" description="Administre varios sitios independientes sobre una sola instancia de TrastCMS.">
      <template #actions><button class="rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-bold text-white hover:bg-slate-800" @click="createSite"><span class="mr-2">＋</span>Nuevo sitio</button></template>
    </AdminPageHeader>

    <div v-if="error" class="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-700">{{ error }}</div>
    <div v-if="notice" class="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-700">{{ notice }}</div>

    <section class="grid gap-4 md:grid-cols-4">
      <article class="rounded-2xl border border-slate-200 bg-white p-5"><small class="font-bold uppercase tracking-wider text-slate-400">Sitios</small><strong class="mt-2 block text-3xl text-slate-950">{{ sites.length }}</strong></article>
      <article class="rounded-2xl border border-slate-200 bg-white p-5"><small class="font-bold uppercase tracking-wider text-slate-400">En línea</small><strong class="mt-2 block text-3xl text-emerald-600">{{ activeSites }}</strong></article>
      <article class="rounded-2xl border border-slate-200 bg-white p-5"><small class="font-bold uppercase tracking-wider text-slate-400">Membresías</small><strong class="mt-2 block text-3xl text-slate-950">{{ totalUsers }}</strong></article>
      <article class="rounded-2xl border border-slate-200 bg-white p-5"><small class="font-bold uppercase tracking-wider text-slate-400">Plugins usados</small><strong class="mt-2 block text-3xl text-slate-950">{{ uniquePlugins }}</strong></article>
    </section>

    <section v-if="loading" class="rounded-3xl border border-slate-200 bg-white p-12 text-center text-slate-500">Cargando red…</section>
    <section v-else class="grid gap-5 xl:grid-cols-2">
      <article v-for="site in sites" :key="site.id" class="overflow-hidden rounded-3xl border border-slate-200 bg-white">
        <div class="flex flex-wrap items-start justify-between gap-4 p-6">
          <div class="flex gap-4">
            <span class="grid h-12 w-12 place-items-center rounded-2xl bg-slate-950 text-white"><UiIcon name="globe" :size="23"/></span>
            <div><div class="flex flex-wrap items-center gap-2"><h3 class="text-xl font-extrabold text-slate-950">{{ site.name }}</h3><span v-if="site.current" class="rounded-full bg-violet-100 px-2 py-1 text-[10px] font-extrabold uppercase text-violet-700">Actual</span><span :class="site.status==='ACTIVE'?'bg-emerald-100 text-emerald-700':'bg-amber-100 text-amber-700'" class="rounded-full px-2 py-1 text-[10px] font-extrabold uppercase">{{ site.status }}</span></div><p class="mt-1 text-sm text-slate-500">{{ site.siteKey }} · {{ site.locale }} · {{ site.timezone }}</p></div>
          </div>
          <button class="rounded-xl border border-slate-200 px-3 py-2 text-sm font-bold text-slate-700 hover:bg-slate-50" @click="enter(site)">Administrar</button>
        </div>
        <div class="grid gap-4 border-y border-slate-100 bg-slate-50/70 p-5 sm:grid-cols-3">
          <div><small class="text-slate-400">Tema</small><strong class="block text-sm text-slate-800">{{ site.activeTheme }}</strong></div>
          <div><small class="text-slate-400">Usuarios</small><strong class="block text-sm text-slate-800">{{ site.users }}</strong></div>
          <div><small class="text-slate-400">Plugins</small><strong class="block text-sm text-slate-800">{{ site.enabledPlugins.length }}</strong></div>
        </div>
        <div class="p-5">
          <a v-if="site.primaryUrl" :href="site.primaryUrl" target="_blank" rel="noopener" class="mb-4 block truncate text-sm font-semibold text-violet-700">{{ site.primaryUrl }} ↗</a>
          <div class="mb-5 flex flex-wrap gap-2"><span v-for="plugin in site.enabledPlugins" :key="plugin" class="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">{{ pluginName(plugin) }}</span></div>
          <div class="flex flex-wrap gap-2"><button class="rounded-xl border border-slate-200 px-3 py-2 text-sm font-bold" @click="editSite(site)">Configuración</button><button class="rounded-xl border border-slate-200 px-3 py-2 text-sm font-bold" @click="editPlugins(site)">Plugins</button><select class="rounded-xl border border-slate-200 px-3 py-2 text-sm font-semibold" @change="applyBlueprintFromSelect(site, $event)"><option value="">Aplicar blueprint…</option><option v-for="blueprint in blueprints" :key="blueprint.value" :value="blueprint.value">{{ blueprint.label }}</option></select></div>
        </div>
      </article>
    </section>

    <div v-if="showForm" class="fixed inset-0 z-[100] grid place-items-center bg-slate-950/60 p-4" @click.self="showForm=false">
      <section class="max-h-[92vh] w-full max-w-4xl overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl">
        <div class="mb-6 flex items-start justify-between gap-4"><div><small class="font-bold uppercase tracking-wider text-violet-600">Multisite</small><h2 class="text-2xl font-extrabold">{{ editing ? 'Editar sitio' : 'Crear sitio' }}</h2><p class="text-sm text-slate-500">Cada sitio mantiene contenido, usuarios, configuración, tema y plugins independientes.</p></div><button class="rounded-xl p-2 hover:bg-slate-100" @click="showForm=false"><UiIcon name="close"/></button></div>
        <div class="grid gap-5 md:grid-cols-2">
          <label class="text-sm font-bold text-slate-700">Nombre<input v-model="form.name" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal" placeholder="Firma Legal NaT"></label>
          <label class="text-sm font-bold text-slate-700">Clave del sitio<input v-model="form.siteKey" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal" placeholder="firma-legal"></label>
          <label v-if="!editing" class="text-sm font-bold text-slate-700 md:col-span-2">Blueprint<select v-model="form.blueprint" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"><option v-for="blueprint in blueprints" :key="blueprint.value" :value="blueprint.value">{{ blueprint.label }} — {{ blueprint.detail }}</option></select></label>
          <label class="text-sm font-bold text-slate-700">Idioma<input v-model="form.locale" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"></label>
          <label class="text-sm font-bold text-slate-700">Zona horaria<input v-model="form.timezone" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"></label>
          <label class="text-sm font-bold text-slate-700">Estado<select v-model="form.status" class="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"><option value="ACTIVE">Activo</option><option value="SUSPENDED">Suspendido</option><option value="ARCHIVED">Archivado</option></select></label>
        </div>
        <div class="mt-7 flex items-center justify-between"><div><h3 class="font-extrabold">Dominios y puertos</h3><p class="text-sm text-slate-500">Todos pueden apuntar al mismo backend mediante Nginx.</p></div><button class="rounded-xl border border-slate-200 px-3 py-2 text-sm font-bold" @click="addEndpoint">+ Dominio</button></div>
        <div class="mt-3 space-y-3"><div v-for="(endpoint,index) in form.endpoints" :key="index" class="grid gap-3 rounded-2xl border border-slate-200 p-4 md:grid-cols-[1fr_110px_110px_100px_auto]"><input v-model="endpoint.host" class="rounded-xl border border-slate-200 px-3 py-2" placeholder="academy.midominio.com"><input v-model.number="endpoint.port" type="number" min="1" max="65535" class="rounded-xl border border-slate-200 px-3 py-2" placeholder="443"><select v-model="endpoint.scheme" class="rounded-xl border border-slate-200 px-3 py-2"><option value="https">https</option><option value="http">http</option></select><label class="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600"><input v-model="endpoint.active" type="checkbox">Activo</label><div class="flex items-center gap-2"><button :class="endpoint.primaryEndpoint?'bg-violet-100 text-violet-700':'bg-slate-100 text-slate-600'" class="rounded-lg px-2 py-2 text-xs font-bold" @click="makePrimary(index)">Principal</button><button class="rounded-lg bg-rose-50 p-2 text-rose-600" @click="removeEndpoint(index)"><UiIcon name="trash" :size="16"/></button></div></div></div>
        <div class="mt-7 flex justify-end gap-3"><button class="rounded-xl border border-slate-200 px-4 py-2.5 font-bold" @click="showForm=false">Cancelar</button><button :disabled="saving" class="rounded-xl bg-slate-950 px-5 py-2.5 font-bold text-white disabled:opacity-50" @click="saveSite">{{ saving ? 'Guardando…' : 'Guardar sitio' }}</button></div>
      </section>
    </div>

    <div v-if="pluginSite" class="fixed inset-0 z-[100] grid place-items-center bg-slate-950/60 p-4" @click.self="pluginSite=null">
      <section class="w-full max-w-3xl rounded-3xl bg-white p-6 shadow-2xl"><div class="mb-5 flex justify-between"><div><small class="font-bold uppercase tracking-wider text-violet-600">{{ pluginSite.name }}</small><h2 class="text-2xl font-extrabold">Plugins del sitio</h2></div><button @click="pluginSite=null"><UiIcon name="close"/></button></div><div class="grid gap-3 md:grid-cols-2"><label v-for="plugin in bundled" :key="plugin.pluginId" class="flex cursor-pointer gap-3 rounded-2xl border border-slate-200 p-4"><input type="checkbox" :checked="selectedPlugins.includes(plugin.pluginId)" class="mt-1" @change="togglePlugin(plugin.pluginId)"><div><strong class="text-slate-900">{{ plugin.name }}</strong><p class="mt-1 text-xs text-slate-500">{{ plugin.description }}</p><small v-if="plugin.requiredPlugins.length" class="mt-2 block text-[11px] text-amber-700">Requiere: {{ plugin.requiredPlugins.join(', ') }}</small></div></label></div><div class="mt-6 flex justify-end gap-3"><button class="rounded-xl border border-slate-200 px-4 py-2.5 font-bold" @click="pluginSite=null">Cancelar</button><button :disabled="saving" class="rounded-xl bg-violet-600 px-5 py-2.5 font-bold text-white disabled:opacity-50" @click="savePlugins">Guardar plugins</button></div></section>
    </div>
  </div>
</template>
