<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import type { BundledPlugin, JavaPlugin, Plugin, PluginCatalog } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

type PluginTab = 'bundled' | 'webhooks' | 'java' | 'catalog'
const tab = ref<PluginTab>('bundled')
const pluginTabs: Array<{ id: PluginTab; label: string; icon: string }> = [
  { id: 'bundled', label: 'Incluidos', icon: 'package' },
  { id: 'webhooks', label: 'Plugins externos', icon: 'external' },
  { id: 'java', label: 'Plugins Java', icon: 'plugin' },
  { id: 'catalog', label: 'Eventos y permisos', icon: 'code' }
]
const bundledPlugins = ref<BundledPlugin[]>([])
const plugins = ref<Plugin[]>([])
const javaPlugins = ref<JavaPlugin[]>([])
const catalog = ref<PluginCatalog>({ events: [], permissions: [], javaPluginsEnabled: false, nativeImage: false })
const loading = ref(true)
const saving = ref(false)
const installing = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const javaFile = ref<File | null>(null)
const search = ref('')
const form = reactive({
  pluginKey: '',
  name: '',
  version: '1.0.0',
  description: '',
  author: '',
  homepage: '',
  baseUrl: '',
  secret: '',
  subscriptions: [] as string[],
  permissions: [] as string[],
  healthCheckPath: '/health',
  enabled: true
})

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return plugins.value
  return plugins.value.filter(plugin => `${plugin.name} ${plugin.pluginKey} ${plugin.author || ''} ${plugin.description || ''}`.toLowerCase().includes(term))
})

const bundledActiveCount = computed(() => bundledPlugins.value.filter(plugin => plugin.enabled).length)
const activeCount = computed(() => plugins.value.filter(plugin => plugin.enabled).length)
const healthyCount = computed(() => plugins.value.filter(plugin => plugin.lastTestStatus === 'HEALTHY').length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [bundled, registered, javaList, pluginCatalog] = await Promise.all([
      api<BundledPlugin[]>('/api/admin/plugins/bundled'),
      api<Plugin[]>('/api/admin/plugins'),
      api<JavaPlugin[]>('/api/admin/plugins/java'),
      api<PluginCatalog>('/api/admin/plugins/catalog')
    ])
    bundledPlugins.value = bundled
    plugins.value = registered
    javaPlugins.value = javaList
    catalog.value = pluginCatalog
    if (!form.subscriptions.length && pluginCatalog.events.length) {
      form.subscriptions = pluginCatalog.events.slice(0, 2)
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar el centro de plugins'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    pluginKey: '', name: '', version: '1.0.0', description: '', author: '', homepage: '',
    baseUrl: '', secret: '', subscriptions: catalog.value.events.slice(0, 2), permissions: [],
    healthCheckPath: '/health', enabled: true
  })
}

function toggleArray(target: string[], value: string) {
  const index = target.indexOf(value)
  if (index >= 0) target.splice(index, 1)
  else target.push(value)
}

async function save() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await api('/api/admin/plugins', { method: 'POST', body: JSON.stringify(form) })
    notice.value = 'Plugin externo registrado correctamente.'
    showForm.value = false
    resetForm()
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo registrar el plugin'
  } finally {
    saving.value = false
  }
}

async function toggle(plugin: Plugin) {
  error.value = ''
  try {
    await api(`/api/admin/plugins/${plugin.id}/enabled?value=${!plugin.enabled}`, { method: 'PUT' })
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cambiar el estado'
  }
}

async function test(plugin: Plugin) {
  error.value = ''
  notice.value = `Verificando ${plugin.name}…`
  try {
    const result = await api<Plugin>(`/api/admin/plugins/${plugin.id}/test`, { method: 'POST' })
    notice.value = result.lastTestStatus === 'HEALTHY'
      ? `${plugin.name} respondió correctamente.`
      : `${plugin.name}: ${result.lastTestMessage || 'no respondió correctamente'}`
    await load()
  } catch (exception) {
    notice.value = ''
    error.value = exception instanceof Error ? exception.message : 'Falló la prueba de salud'
  }
}

async function remove(plugin: Plugin) {
  if (!confirm(`¿Eliminar el plugin ${plugin.name}? Esta acción no puede deshacerse.`)) return
  await api(`/api/admin/plugins/${plugin.id}`, { method: 'DELETE' })
  await load()
}

function chooseJava(event: Event) {
  javaFile.value = (event.target as HTMLInputElement).files?.[0] || null
}

async function installJava() {
  if (!javaFile.value) return
  const body = new FormData()
  body.append('file', javaFile.value)
  installing.value = true
  error.value = ''
  try {
    await api('/api/admin/plugins/java', { method: 'POST', body })
    javaFile.value = null
    notice.value = 'Plugin Java instalado. Puede iniciarlo desde esta pantalla.'
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo instalar el plugin Java'
  } finally {
    installing.value = false
  }
}

async function javaAction(plugin: JavaPlugin, action: 'start' | 'stop' | 'delete') {
  error.value = ''
  try {
    if (action === 'delete') {
      if (!confirm(`¿Eliminar definitivamente ${plugin.pluginId}?`)) return
      await api(`/api/admin/plugins/java/${plugin.pluginId}`, { method: 'DELETE' })
    } else {
      await api(`/api/admin/plugins/java/${plugin.pluginId}/${action}`, { method: 'PUT' })
    }
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo completar la operación'
  }
}

function bundledRoute(pluginId: string) { return ({ trastcrm: 'crm', trastpay: 'commerce', traststore: 'store', trastsaas: 'saas' } as Record<string,string>)[pluginId] || '' }

async function toggleBundled(plugin: BundledPlugin) {
  error.value = ''
  notice.value = ''
  try {
    await api(`/api/admin/plugins/bundled/${encodeURIComponent(plugin.pluginId)}/enabled?value=${!plugin.enabled}`, { method: 'PUT' })
    notice.value = `${plugin.name} fue ${plugin.enabled ? 'desactivado' : 'activado'} correctamente.`
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cambiar el estado del plugin incluido'
  }
}

function statusLabel(plugin: Plugin) {
  if (!plugin.enabled) return 'Deshabilitado'
  if (plugin.lastTestStatus === 'HEALTHY') return 'Saludable'
  if (plugin.lastTestStatus === 'FAILED') return 'Con errores'
  return 'Sin verificar'
}

onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="Extensibilidad" title="Centro de plugins" description="Instale extensiones Java en la edición JVM o conecte servicios externos firmados, compatibles con JAR y Native Image.">
    <template #actions>
      <button class="button secondary" type="button" @click="tab='catalog'"><UiIcon name="document" :size="16"/>API y eventos</button>
      <button class="button" type="button" @click="tab='webhooks';showForm=!showForm"><UiIcon name="plus" :size="16"/>Registrar plugin</button>
    </template>
  </AdminPageHeader>

  <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
    <article class="metric-card"><span>Plugins incluidos</span><strong>{{ bundledPlugins.length }}</strong><small>{{ bundledActiveCount }} activos</small></article>
    <article class="metric-card"><span>Plugins externos</span><strong>{{ plugins.length }}</strong><small>{{ activeCount }} habilitados</small></article>
    <article class="metric-card"><span>Conexiones saludables</span><strong>{{ healthyCount }}</strong><small>según última prueba</small></article>
    <article class="metric-card"><span>Plugins Java</span><strong>{{ javaPlugins.length }}</strong><small>{{ catalog.javaPluginsEnabled ? 'Motor PF4J activo' : 'No disponible' }}</small></article>
    <article class="metric-card"><span>Modo de ejecución</span><strong class="text-2xl">{{ catalog.nativeImage ? 'Native' : 'JVM' }}</strong><small>{{ catalog.nativeImage ? 'Use plugins externos' : 'Java + externos' }}</small></article>
  </div>

  <div class="mt-6 flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
    <button v-for="item in pluginTabs" :key="item.id" type="button" :class="['tab-button', { active: tab === item.id }]" @click="tab=item.id"><UiIcon :name="item.icon" :size="17"/>{{ item.label }}</button>
  </div>

  <p v-if="notice" class="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">{{ notice }}</p>
  <p v-if="error" class="mt-4 form-error">{{ error }}</p>

  <template v-if="tab === 'bundled'">
    <section class="panel mt-6">
      <div class="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div><span class="eyebrow">DISTRIBUCIÓN OFICIAL</span><h2 class="mt-1 text-xl font-bold">Plugins incluidos con TrastCMS</h2><p class="mt-1 text-sm text-slate-500">Su código vive en la carpeta <code>plugins/</code>, se compila dentro del mismo JAR y conserva un ciclo de activación independiente.</p></div>
        <span class="status-pill ok">Compatible con Native Image</span>
      </div>
      <div v-if="loading" class="empty">Cargando plugins incluidos…</div>
      <div v-else-if="bundledPlugins.length" class="grid gap-4 xl:grid-cols-2">
        <article v-for="plugin in bundledPlugins" :key="plugin.pluginId" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-start gap-4">
            <div class="plugin-logo bundled"><UiIcon :name="plugin.pluginId === 'trastcrm' ? 'crm' : 'package'" :size="28"/></div>
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2"><h3 class="text-lg font-bold">{{ plugin.name }}</h3><span :class="['status-pill', plugin.enabled ? 'ok' : 'muted']">{{ plugin.enabled ? 'Activo' : 'Inactivo' }}</span><span class="status-pill muted">v{{ plugin.version }}</span></div>
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ plugin.description }}</p>
              <p class="mt-3 text-xs text-slate-400"><b>Autor:</b> {{ plugin.author }} · <b>ID:</b> {{ plugin.pluginId }}</p>
              <div class="mt-4 flex flex-wrap gap-1.5"><span v-for="capability in plugin.capabilities" :key="capability" class="chip">{{ capability }}</span></div>
            </div>
          </div>
          <div class="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
            <RouterLink v-if="plugin.enabled && bundledRoute(plugin.pluginId)" :to="{ name: bundledRoute(plugin.pluginId) }" class="button secondary small"><UiIcon name="external" :size="15"/>Abrir {{ plugin.name }}</RouterLink><span v-else class="text-xs text-slate-400">{{ plugin.enabled ? 'Plugin listo para utilizar.' : 'Actívelo para habilitar sus rutas, bloques y eventos.' }}</span>
            <button type="button" :class="['button small', plugin.enabled ? 'danger' : '']" @click="toggleBundled(plugin)">{{ plugin.enabled ? 'Desactivar' : 'Activar' }}</button>
          </div>
        </article>
      </div>
      <div v-else class="empty"><UiIcon name="package" :size="42"/><h3>No hay plugins incluidos</h3><p>Agregue implementaciones de <code>BundledPlugin</code> dentro de la carpeta <code>plugins/</code>.</p></div>
    </section>
  </template>

  <template v-else-if="tab === 'webhooks'">
    <form v-if="showForm" class="panel mt-6 space-y-6" @submit.prevent="save">
      <div class="flex items-start justify-between gap-4"><div><h2 class="text-xl font-bold">Registrar plugin externo</h2><p class="mt-1 text-sm text-slate-500">TrastCMS firmará cada evento con HMAC-SHA256 y lo enviará al endpoint <code>/trastcms/events</code>.</p></div><button type="button" class="icon-button" @click="showForm=false"><UiIcon name="close"/></button></div>
      <div class="grid gap-4 lg:grid-cols-3">
        <label>Identificador<input v-model="form.pluginKey" required pattern="[a-z0-9][a-z0-9._-]{2,119}" placeholder="comercio-pagos"><small>Minúsculas, números, punto, guion o guion bajo.</small></label>
        <label>Nombre<input v-model="form.name" required placeholder="Pagos para TrastCMS"></label>
        <label>Versión<input v-model="form.version" required placeholder="1.0.0"></label>
        <label>Autor<input v-model="form.author" placeholder="NaT Technologies"></label>
        <label>Sitio del plugin<input v-model="form.homepage" type="url" placeholder="https://.../"></label>
        <label>Estado<select v-model="form.enabled"><option :value="true">Habilitado</option><option :value="false">Deshabilitado</option></select></label>
        <label class="lg:col-span-2">URL base<input v-model="form.baseUrl" required type="url" placeholder="https://plugin.example.com"></label>
        <label>Ruta de salud<input v-model="form.healthCheckPath" placeholder="/health"></label>
        <label class="lg:col-span-3">Descripción<textarea v-model="form.description" rows="3" placeholder="Qué hace, qué datos procesa y cómo se integra."></textarea></label>
        <label class="lg:col-span-3">Secreto HMAC<input v-model="form.secret" required minlength="16" type="password" autocomplete="new-password" placeholder="Mínimo 16 caracteres"><small>Se cifra en el servidor y nunca vuelve a mostrarse.</small></label>
      </div>
      <div class="grid gap-6 xl:grid-cols-2">
        <section><h3 class="mb-3 font-bold">Eventos suscritos</h3><div class="permission-grid"><label v-for="event in catalog.events" :key="event" class="option-check"><input type="checkbox" :checked="form.subscriptions.includes(event)" @change="toggleArray(form.subscriptions,event)"><span><strong>{{ event }}</strong><small>Recibir este evento cuando ocurra.</small></span></label></div></section>
        <section><h3 class="mb-3 font-bold">Permisos declarados</h3><div class="permission-grid"><label v-for="permission in catalog.permissions" :key="permission" class="option-check"><input type="checkbox" :checked="form.permissions.includes(permission)" @change="toggleArray(form.permissions,permission)"><span><strong>{{ permission }}</strong><small>Capacidad solicitada por la integración.</small></span></label></div></section>
      </div>
      <div class="flex justify-end gap-3"><button type="button" class="button secondary" @click="showForm=false">Cancelar</button><button class="button" :disabled="saving"><UiIcon name="check" :size="16"/>{{ saving ? 'Registrando…' : 'Registrar plugin' }}</button></div>
    </form>

    <section class="panel mt-6">
      <div class="mb-5 flex flex-wrap items-center justify-between gap-3"><div><h2 class="text-lg font-bold">Plugins externos</h2><p class="text-sm text-slate-500">Servicios aislados, webhooks, automatizaciones y conectores.</p></div><div class="search-field min-w-64"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Buscar plugin"></div></div>
      <div v-if="loading" class="empty">Cargando extensiones…</div>
      <div v-else-if="filtered.length" class="space-y-3">
        <article v-for="plugin in filtered" :key="plugin.id" class="plugin-card">
          <div class="plugin-logo"><UiIcon name="plugin" :size="26"/></div>
          <div class="min-w-0 flex-1"><div class="flex flex-wrap items-center gap-2"><h3 class="text-lg font-bold">{{ plugin.name }}</h3><span :class="['status-pill', plugin.enabled && plugin.lastTestStatus !== 'FAILED' ? 'ok' : plugin.lastTestStatus === 'FAILED' ? 'error' : 'muted']">{{ statusLabel(plugin) }}</span></div><p class="mt-1 text-sm text-slate-500">{{ plugin.description || 'Plugin externo sin descripción.' }}</p><div class="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs text-slate-500"><span><b>ID:</b> {{ plugin.pluginKey }}</span><span><b>Versión:</b> {{ plugin.version }}</span><span v-if="plugin.author"><b>Autor:</b> {{ plugin.author }}</span><span><b>URL:</b> {{ plugin.baseUrl }}</span></div><div class="mt-3 flex flex-wrap gap-1.5"><span v-for="event in plugin.subscriptions" :key="event" class="chip">{{ event }}</span></div><p v-if="plugin.lastTestMessage" class="mt-3 text-xs" :class="plugin.lastTestStatus === 'FAILED' ? 'text-rose-600' : 'text-emerald-700'">{{ plugin.lastTestMessage }}<span v-if="plugin.lastTestAt"> · {{ new Date(plugin.lastTestAt).toLocaleString() }}</span></p></div>
          <div class="flex shrink-0 flex-wrap items-center justify-end gap-2"><button class="button secondary small" type="button" @click="test(plugin)">Probar conexión</button><button class="button secondary small" type="button" @click="toggle(plugin)">{{ plugin.enabled ? 'Deshabilitar' : 'Habilitar' }}</button><button class="icon-button danger" type="button" title="Eliminar" @click="remove(plugin)"><UiIcon name="trash" :size="17"/></button></div>
        </article>
      </div>
      <div v-else class="empty"><UiIcon name="plugin" :size="42"/><h3>No hay plugins externos</h3><p>Registre una integración para recibir eventos firmados de contenido, medios, usuarios o temas.</p><button class="button" type="button" @click="showForm=true"><UiIcon name="plus" :size="16"/>Registrar el primero</button></div>
    </section>
  </template>

  <template v-else-if="tab === 'java'">
    <section class="panel mt-6">
      <div class="grid gap-5 lg:grid-cols-[1fr_auto]"><div><h2 class="text-xl font-bold">Plugins Java dinámicos (PF4J)</h2><p class="mt-1 text-sm text-slate-500">Instale JAR o ZIP desarrollados contra <code>TrastCmsExtension</code>. Disponibles únicamente al ejecutar la distribución JVM.</p><div class="mt-4 flex flex-wrap gap-2"><span :class="['status-pill', catalog.javaPluginsEnabled ? 'ok' : 'error']">{{ catalog.javaPluginsEnabled ? 'Motor habilitado' : 'Motor no disponible' }}</span><span v-if="catalog.nativeImage" class="status-pill muted">Native Image detectado</span></div></div><label class="upload-drop compact"><input type="file" accept=".jar,.zip,application/java-archive,application/zip" @change="chooseJava"><UiIcon name="upload" :size="24"/><span>{{ javaFile?.name || 'Seleccionar JAR o ZIP' }}</span><button class="button small" type="button" :disabled="!javaFile || installing || !catalog.javaPluginsEnabled" @click="installJava">{{ installing ? 'Instalando…' : 'Instalar' }}</button></label></div>
    </section>
    <section class="mt-6 grid gap-4 xl:grid-cols-2">
      <article v-for="plugin in javaPlugins" :key="plugin.pluginId" class="panel plugin-java-card"><div class="flex items-start gap-4"><div class="plugin-logo java"><UiIcon name="code" :size="27"/></div><div class="min-w-0 flex-1"><div class="flex flex-wrap items-center gap-2"><h3 class="text-lg font-bold">{{ plugin.name }}</h3><span :class="['status-pill', plugin.state === 'STARTED' ? 'ok' : 'muted']">{{ plugin.state }}</span></div><p class="mt-1 text-sm text-slate-500">{{ plugin.description || 'Extensión Java para TrastCMS.' }}</p><dl class="mt-4 grid grid-cols-2 gap-3 text-xs"><div><dt>Identificador</dt><dd>{{ plugin.pluginId }}</dd></div><div><dt>Versión</dt><dd>{{ plugin.version }}</dd></div><div><dt>Proveedor</dt><dd>{{ plugin.provider || 'No indicado' }}</dd></div><div><dt>Ubicación</dt><dd class="truncate" :title="plugin.path">{{ plugin.path }}</dd></div></dl><div class="mt-4 flex flex-wrap gap-1.5"><span v-for="capability in plugin.capabilities" :key="capability" class="chip">{{ capability }}</span><span v-if="!plugin.capabilities.length" class="text-xs text-slate-400">Sin capacidades declaradas.</span></div></div></div><div class="mt-5 flex justify-end gap-2"><button v-if="plugin.state !== 'STARTED'" class="button small" @click="javaAction(plugin,'start')">Iniciar</button><button v-else class="button secondary small" @click="javaAction(plugin,'stop')">Detener</button><button v-if="plugin.deletable" class="button danger small" @click="javaAction(plugin,'delete')">Eliminar</button></div></article>
      <div v-if="!javaPlugins.length" class="panel empty xl:col-span-2"><UiIcon name="code" :size="42"/><h3>No hay plugins Java instalados</h3><p>Use el SDK del proyecto para registrar bloques, menús administrativos, capacidades y listeners de eventos.</p></div>
    </section>
  </template>

  <template v-else>
    <section class="mt-6 grid gap-6 xl:grid-cols-2">
      <article class="panel"><div class="mb-5 flex items-center gap-3"><span class="plugin-logo"><UiIcon name="external" :size="24"/></span><div><h2 class="text-xl font-bold">Catálogo de eventos</h2><p class="text-sm text-slate-500">Eventos disponibles para plugins externos y extensiones Java.</p></div></div><div class="catalog-list"><div v-for="event in catalog.events" :key="event"><code>{{ event }}</code><span>Se publica después de confirmar la transacción.</span></div></div></article>
      <article class="panel"><div class="mb-5 flex items-center gap-3"><span class="plugin-logo"><UiIcon name="settings" :size="24"/></span><div><h2 class="text-xl font-bold">Permisos y capacidades</h2><p class="text-sm text-slate-500">Contrato declarativo para auditar lo que solicita una extensión.</p></div></div><div class="catalog-list"><div v-for="permission in catalog.permissions" :key="permission"><code>{{ permission }}</code><span>Debe declararse en el manifiesto del plugin.</span></div></div></article>
      <article class="panel xl:col-span-2"><h2 class="text-xl font-bold">Arquitectura de extensiones</h2><div class="mt-5 grid gap-4 md:grid-cols-3"><div class="architecture-step"><b>1</b><h3>Declarar</h3><p>Defina identificador, versión, proveedor, permisos, eventos y compatibilidad.</p></div><div class="architecture-step"><b>2</b><h3>Instalar o conectar</h3><p>Use PF4J en JVM o un servicio externo con webhooks HMAC en cualquier distribución.</p></div><div class="architecture-step"><b>3</b><h3>Operar con aislamiento</h3><p>Pruebas de salud, auditoría, activación, detención y eliminación controlada.</p></div></div></article>
    </section>
  </template>
</template>
