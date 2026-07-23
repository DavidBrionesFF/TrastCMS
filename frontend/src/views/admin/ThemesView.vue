<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import type {
  PageResponse,
  PostSummary,
  Theme,
  ThemeMenuItem,
  ThemeMenus,
  ThemeSettingDefinition,
  ThemeSettings
} from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const mode = ref<'themes' | 'menus'>('themes')
const themes = ref<Theme[]>([])
const pages = ref<PostSummary[]>([])
const menus = ref<ThemeMenus>({ header: [], footer: [] })
const menuLocation = ref<'header' | 'footer'>('header')
const selectedFile = ref<File | null>(null)
const installing = ref(false)
const loading = ref(true)
const menuSaving = ref(false)
const error = ref('')
const notice = ref('')
const selectedTheme = ref<Theme | null>(null)
const settings = ref<Record<string, string>>({})
const settingsLoading = ref(false)
const settingsSaving = ref(false)
const search = ref('')
const filter = ref<'all' | 'built-in' | 'custom'>('all')

const activeTheme = computed(() => themes.value.find(theme => theme.active))
const currentMenu = computed(() => menus.value[menuLocation.value])
const filteredThemes = computed(() => {
  const term = search.value.trim().toLowerCase()
  return themes.value.filter(theme => {
    const typeOk = filter.value === 'all' || (filter.value === 'custom' ? theme.custom : !theme.custom)
    const termOk = !term || `${theme.name} ${theme.description} ${theme.author || ''} ${theme.features.join(' ')}`.toLowerCase().includes(term)
    return typeOk && termOk
  })
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [themeList, menuResponse, pageResponse] = await Promise.all([
      api<Theme[]>('/api/admin/themes'),
      api<ThemeMenus>('/api/admin/themes/menus'),
      api<PageResponse<PostSummary>>('/api/admin/posts?type=PAGE&size=100')
    ])
    themes.value = themeList
    menus.value = {
      header: menuResponse.header.map(item => ({ ...item })),
      footer: menuResponse.footer.map(item => ({ ...item }))
    }
    pages.value = pageResponse.content
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar la apariencia'
  } finally {
    loading.value = false
  }
}

async function restoreStarterContent(theme: Theme) {
  const confirmed = confirm(
    `¿Restaurar las páginas iniciales de ${theme.name}? ` +
    'Se actualizarán Inicio, Quiénes somos, Servicios, Blog y Contáctanos ' +
    'cuando pertenezcan a este tema.'
  )
  if (!confirmed) return
  error.value = ''
  notice.value = ''
  try {
    const result = await api<{ created: number; updated: number }>(
      `/api/admin/themes/${theme.id}/starter-content/restore`,
      { method: 'POST' }
    )
    notice.value = `Contenido inicial restaurado: ${result.created} páginas creadas y ${result.updated} actualizadas.`
    await load()
  } catch (exception) {
    error.value = exception instanceof Error
      ? exception.message
      : 'No se pudo restaurar el contenido inicial'
  }
}

async function activate(theme: Theme) {
  error.value = ''
  notice.value = ''
  try {
    await api('/api/admin/themes/active', { method: 'PUT', body: JSON.stringify({ themeId: theme.id }) })
    notice.value = `${theme.name} está activo.`
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo activar el tema'
  }
}

function choose(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] || null
}

async function install() {
  if (!selectedFile.value) return
  const body = new FormData()
  body.append('file', selectedFile.value)
  installing.value = true
  error.value = ''
  notice.value = ''
  try {
    const installed = await api<Theme>('/api/admin/themes', { method: 'POST', body })
    selectedFile.value = null
    notice.value = `${installed.name} fue instalado correctamente.`
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo instalar el tema'
  } finally {
    installing.value = false
  }
}

async function remove(theme: Theme) {
  if (!confirm(`¿Eliminar el tema ${theme.name}?`)) return
  try {
    await api(`/api/admin/themes/${theme.id}`, { method: 'DELETE' })
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo eliminar el tema'
  }
}

async function customize(theme: Theme) {
  selectedTheme.value = theme
  settingsLoading.value = true
  error.value = ''
  try {
    const response = await api<ThemeSettings>(`/api/admin/themes/${theme.id}/settings`)
    const values: Record<string, string> = { ...response.values }
    Object.entries(theme.settingsSchema as Record<string, ThemeSettingDefinition>).forEach(([key, definition]) => {
      if (values[key] === undefined && definition.default !== undefined) values[key] = String(definition.default)
    })
    settings.value = values
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudieron cargar las opciones'
  } finally {
    settingsLoading.value = false
  }
}

async function saveSettings() {
  if (!selectedTheme.value) return
  settingsSaving.value = true
  error.value = ''
  try {
    await api(`/api/admin/themes/${selectedTheme.value.id}/settings`, {
      method: 'PUT',
      body: JSON.stringify({ values: settings.value })
    })
    notice.value = `Opciones de ${selectedTheme.value.name} guardadas.`
    selectedTheme.value = null
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudieron guardar las opciones'
  } finally {
    settingsSaving.value = false
  }
}

function optionValues(definition: ThemeSettingDefinition): Array<{ label: string; value: string }> {
  return (definition.options || []).map(option => typeof option === 'string' ? { label: option, value: option } : option)
}

function id() {
  return typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`
}

function addPageItem() {
  const page = pages.value.find(candidate => !currentMenu.value.some(item => item.type === 'PAGE' && item.target === candidate.slug)) || pages.value[0]
  if (!page) return
  currentMenu.value.push({
    id: id(),
    label: page.title,
    type: 'PAGE',
    target: page.slug,
    url: '',
    visible: true,
    newTab: false,
    order: currentMenu.value.length
  })
}

function addCustomItem() {
  currentMenu.value.push({
    id: id(),
    label: 'Nuevo enlace',
    type: 'CUSTOM',
    target: '',
    url: '#',
    visible: true,
    newTab: false,
    order: currentMenu.value.length
  })
}

function selectPage(item: ThemeMenuItem) {
  const page = pages.value.find(candidate => candidate.slug === item.target)
  if (page && (!item.label || item.label === 'Nueva página')) item.label = page.title
}

function move(index: number, direction: -1 | 1) {
  const target = index + direction
  const items = currentMenu.value
  if (target < 0 || target >= items.length) return
  const [item] = items.splice(index, 1)
  items.splice(target, 0, item)
  items.forEach((entry, order) => { entry.order = order })
}

function removeMenuItem(index: number) {
  currentMenu.value.splice(index, 1)
  currentMenu.value.forEach((entry, order) => { entry.order = order })
}

async function saveMenus() {
  menuSaving.value = true
  error.value = ''
  notice.value = ''
  try {
    const response = await api<ThemeMenus>('/api/admin/themes/menus', {
      method: 'PUT',
      body: JSON.stringify(menus.value)
    })
    menus.value = response
    notice.value = 'Menús del tema guardados correctamente.'
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudieron guardar los menús'
  } finally {
    menuSaving.value = false
  }
}

function restoreSuggestedMenu() {
  const visiblePages = pages.value.filter(page => page.status === 'PUBLISHED')
  const header = visiblePages.slice(0, 8).map((page, order): ThemeMenuItem => ({
    id: id(), label: page.title, type: 'PAGE', target: page.slug, url: '', visible: true, newTab: false, order
  }))
  const footer = visiblePages.filter(page => page.pageRole !== 'HOME').slice(0, 6).map((page, order): ThemeMenuItem => ({
    id: id(), label: page.title, type: 'PAGE', target: page.slug, url: '', visible: true, newTab: false, order
  }))
  menus.value = { header, footer }
}

onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="Apariencia" title="Temas y navegación" description="Controle el diseño, las páginas iniciales y los menús visibles en el encabezado y el pie de página.">
    <template #actions>
      <a class="button secondary" href="https://github.com/DavidBrionesFF/TrastCMS/blob/master/docs/THEMES.md" target="_blank"><UiIcon name="document" :size="16"/>Guía de temas</a>
    </template>
  </AdminPageHeader>

  <div class="appearance-tabs">
    <button :class="{ active: mode === 'themes' }" @click="mode='themes'"><UiIcon name="theme" :size="18"/>Temas</button>
    <button :class="{ active: mode === 'menus' }" @click="mode='menus'"><UiIcon name="menu" :size="18"/>Menús</button>
  </div>

  <p v-if="notice" class="mt-4 form-success">{{ notice }}</p>
  <p v-if="error" class="mt-4 form-error">{{ error }}</p>

  <template v-if="mode === 'themes'">
    <section v-if="activeTheme" class="active-theme-hero mt-6">
      <div class="active-theme-shot"><img :src="activeTheme.screenshotUrl" :alt="`Vista previa de ${activeTheme.name}`"></div>
      <div class="min-w-0 flex-1"><span class="eyebrow">Tema activo</span><h2>{{ activeTheme.name }}</h2><p>{{ activeTheme.description }}</p><div class="mt-4 flex flex-wrap gap-2"><span v-for="feature in activeTheme.features" :key="feature" class="chip">{{ feature }}</span></div><div class="mt-6 flex flex-wrap gap-3"><button class="button" @click="customize(activeTheme)"><UiIcon name="settings" :size="16"/>Personalizar</button><button class="button secondary" @click="mode='menus'"><UiIcon name="menu" :size="16"/>Editar menús</button><button class="button secondary" @click="restoreStarterContent(activeTheme)"><UiIcon name="refresh" :size="16"/>Restaurar páginas demo</button><a href="/" target="_blank" class="button secondary"><UiIcon name="external" :size="16"/>Ver sitio</a></div></div>
    </section>

    <section class="starter-pages-banner mt-6">
      <div class="starter-pages-icon"><UiIcon name="pages" :size="22"/></div>
      <div class="min-w-0 flex-1"><h2 class="text-base font-bold text-slate-950">Páginas locales y editables desde el primer arranque</h2><p class="mt-1 text-sm leading-6 text-slate-600">Inicio, Quiénes somos, Servicios, Blog y Contáctanos son contenido real del CMS. Puede rediseñarlas, duplicarlas o reemplazarlas desde el constructor visual.</p></div>
      <RouterLink class="button secondary small" to="/admin/pages"><UiIcon name="pages" :size="16"/>Administrar páginas</RouterLink>
    </section>

    <section class="panel mt-6">
      <div class="grid gap-5 lg:grid-cols-[1fr_auto]"><div><h2 class="text-lg font-bold">Instalar un tema</h2><p class="mt-1 text-sm text-slate-500">Suba un ZIP con manifiesto, tokens, capturas, plantillas y recursos. TrastCMS valida rutas, extensiones y límites antes de instalarlo.</p></div><label class="upload-drop compact"><input type="file" accept=".zip,application/zip" @change="choose"><UiIcon name="upload" :size="23"/><span>{{ selectedFile?.name || 'Seleccionar paquete ZIP' }}</span><button class="button small" type="button" :disabled="!selectedFile || installing" @click="install">{{ installing ? 'Instalando…' : 'Instalar' }}</button></label></div>
    </section>

    <section class="mt-6">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3"><div><h2 class="text-xl font-bold">Biblioteca de temas</h2><p class="text-sm text-slate-500">{{ themes.length }} temas disponibles · {{ themes.filter(item => item.custom).length }} instalados por el usuario</p></div><div class="flex flex-wrap gap-2"><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Buscar tema"></div><select v-model="filter" class="control-select"><option value="all">Todos</option><option value="built-in">Incluidos</option><option value="custom">Instalados</option></select></div></div>
      <div v-if="loading" class="panel empty">Cargando temas…</div>
      <div v-else class="theme-market-grid">
        <article v-for="theme in filteredThemes" :key="theme.id" :class="['theme-market-card', { active: theme.active }]">
          <div class="theme-market-preview"><img :src="theme.screenshotUrl" :alt="`Vista previa de ${theme.name}`" loading="lazy"><div class="theme-card-overlay"><button v-if="!theme.active" class="button" @click="activate(theme)">Activar</button><button class="button secondary" @click="customize(theme)">Personalizar</button></div><span v-if="theme.active" class="theme-active-label"><UiIcon name="check" :size="14"/>Activo</span></div>
          <div class="p-5"><div class="flex items-start justify-between gap-3"><div><h3 class="text-lg font-bold">{{ theme.name }}</h3><p class="mt-1 text-sm text-slate-500">{{ theme.description }}</p></div><span :class="['status-pill', theme.custom ? 'info' : 'muted']">{{ theme.custom ? 'Instalado' : 'Incluido' }}</span></div><div class="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-500"><span><b>Versión:</b> {{ theme.version }}</span><span><b>Autor:</b> {{ theme.author || 'No indicado' }}</span><span><b>Licencia:</b> {{ theme.license || 'No indicada' }}</span><span><b>Plantillas:</b> {{ theme.templates.length }}</span></div><div class="mt-4 flex flex-wrap gap-1.5"><span v-for="feature in theme.features.slice(0,5)" :key="feature" class="chip">{{ feature }}</span></div><div class="mt-5 flex items-center justify-between border-t border-slate-100 pt-4"><button class="link font-semibold" @click="customize(theme)">Detalles y opciones</button><button v-if="theme.custom && !theme.active" class="link danger" @click="remove(theme)">Eliminar</button></div></div>
        </article>
      </div>
    </section>
  </template>

  <template v-else>
    <section class="menu-manager mt-6">
      <header class="menu-manager__header">
        <div><span class="eyebrow">Navegación del sitio</span><h2>Constructor de menús</h2><p>Defina qué enlaces muestra el tema. El encabezado y el footer se administran por separado, como en un CMS profesional.</p></div>
        <div class="flex flex-wrap gap-2"><button class="button secondary" @click="restoreSuggestedMenu"><UiIcon name="refresh" :size="16"/>Restaurar sugeridos</button><button class="button" :disabled="menuSaving" @click="saveMenus"><UiIcon name="check" :size="16"/>{{ menuSaving ? 'Guardando…' : 'Guardar menús' }}</button></div>
      </header>

      <div class="menu-location-tabs">
        <button :class="{ active: menuLocation === 'header' }" @click="menuLocation='header'"><UiIcon name="menu" :size="18"/><span><strong>Menú principal</strong><small>Encabezado del sitio</small></span><b>{{ menus.header.length }}</b></button>
        <button :class="{ active: menuLocation === 'footer' }" @click="menuLocation='footer'"><UiIcon name="footer" :size="18"/><span><strong>Menú del footer</strong><small>Pie de página</small></span><b>{{ menus.footer.length }}</b></button>
      </div>

      <div class="menu-manager__body">
        <aside class="menu-add-panel">
          <h3>Agregar elementos</h3>
          <p>Use páginas internas o enlaces personalizados.</p>
          <button class="menu-add-card" :disabled="!pages.length" @click="addPageItem"><UiIcon name="page" :size="20"/><span><strong>Página</strong><small>Enlace a una página publicada</small></span><b>+</b></button>
          <button class="menu-add-card" @click="addCustomItem"><UiIcon name="external" :size="20"/><span><strong>Enlace personalizado</strong><small>URL, correo, teléfono o ancla</small></span><b>+</b></button>
          <div class="menu-help"><UiIcon name="info" :size="18"/><p>Los cambios se aplican al tema activo sin modificar sus archivos. Puede cambiar de tema y conservar los menús.</p></div>
        </aside>

        <section class="menu-editor-panel">
          <div class="menu-editor-panel__title"><div><h3>{{ menuLocation === 'header' ? 'Menú principal' : 'Menú del footer' }}</h3><p>Ordene los elementos y configure su destino.</p></div><span>{{ currentMenu.filter(item => item.visible).length }} visibles</span></div>
          <div v-if="!currentMenu.length" class="menu-empty"><UiIcon name="menu" :size="34"/><h3>Este menú está vacío</h3><p>Agregue una página o un enlace personalizado desde el panel izquierdo.</p></div>
          <div v-else class="menu-items">
            <article v-for="(item,index) in currentMenu" :key="item.id" class="menu-item-editor">
              <div class="menu-item-editor__handle"><UiIcon name="drag" :size="18"/></div>
              <div class="menu-item-editor__fields">
                <label>Etiqueta<input v-model="item.label" maxlength="120"></label>
                <label>Tipo<select v-model="item.type"><option value="PAGE">Página</option><option value="CUSTOM">Enlace personalizado</option></select></label>
                <label v-if="item.type === 'PAGE'">Página<select v-model="item.target" @change="selectPage(item)"><option v-for="page in pages" :key="page.id" :value="page.slug">{{ page.title }} · /{{ page.slug }}</option></select></label>
                <label v-else>URL<input v-model="item.url" placeholder="/ruta, https://, mailto: o tel:"></label>
                <div class="menu-item-editor__options"><label class="compact-check"><input v-model="item.visible" type="checkbox">Visible</label><label class="compact-check"><input v-model="item.newTab" type="checkbox">Nueva pestaña</label></div>
              </div>
              <div class="menu-item-editor__actions"><button :disabled="index === 0" title="Subir" @click="move(index,-1)">↑</button><button :disabled="index === currentMenu.length-1" title="Bajar" @click="move(index,1)">↓</button><button class="danger" title="Eliminar" @click="removeMenuItem(index)">×</button></div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </template>

  <Teleport to="body">
    <div v-if="selectedTheme" class="modal-backdrop" @click.self="selectedTheme=null">
      <aside class="theme-customizer">
        <header><div><span class="eyebrow">Personalizador</span><h2>{{ selectedTheme.name }}</h2><p>Edite tokens declarados por el tema. Los cambios quedan separados del paquete instalado.</p></div><button class="icon-button" @click="selectedTheme=null"><UiIcon name="close"/></button></header>
        <div class="theme-customizer-body">
          <div class="customizer-preview"><img :src="selectedTheme.screenshotUrl" :alt="selectedTheme.name"><div class="p-4"><h3 class="font-bold">Compatibilidad</h3><div class="mt-3 flex flex-wrap gap-1.5"><span v-for="template in selectedTheme.templates" :key="template" class="chip">plantilla: {{ template }}</span><span v-for="feature in selectedTheme.features" :key="feature" class="chip">{{ feature }}</span></div></div></div>
          <form class="customizer-controls" @submit.prevent="saveSettings"><div v-if="settingsLoading" class="empty">Cargando opciones…</div><template v-else><div v-for="(definition,key) in selectedTheme.settingsSchema" :key="key" class="theme-setting-field"><span>{{ definition.label }}</span><small v-if="definition.description">{{ definition.description }}</small><input v-if="definition.type === 'text'" v-model="settings[key]" type="text"><textarea v-else-if="definition.type === 'textarea'" v-model="settings[key]" rows="4"></textarea><input v-else-if="definition.type === 'color'" v-model="settings[key]" type="color" class="h-12"><input v-else-if="definition.type === 'number'" v-model="settings[key]" type="number" :min="definition.min" :max="definition.max" :step="definition.step || 1"><select v-else-if="definition.type === 'select'" v-model="settings[key]"><option v-for="option in optionValues(definition)" :key="option.value" :value="option.value">{{ option.label }}</option></select><label v-else-if="definition.type === 'boolean'" class="option-check"><input v-model="settings[key]" type="checkbox" true-value="true" false-value="false"><span>Habilitado</span></label></div><div v-if="!Object.keys(selectedTheme.settingsSchema || {}).length" class="empty"><UiIcon name="settings" :size="36"/><p>Este tema no declara opciones configurables.</p></div></template></form>
        </div>
        <footer><button class="button secondary" @click="selectedTheme=null">Cancelar</button><button class="button" :disabled="settingsSaving" @click="saveSettings"><UiIcon name="check" :size="16"/>{{ settingsSaving ? 'Guardando…' : 'Guardar personalización' }}</button></footer>
      </aside>
    </div>
  </Teleport>
</template>
