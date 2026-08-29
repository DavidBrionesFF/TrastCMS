<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UiIcon from '@/components/common/UiIcon.vue'
import { api } from '@/services/api'
import { getSelectedSite, setSelectedSite } from '@/services/site-context'
import type { PluginContributions, PluginAdminMenuItem, SiteMembership } from '@/types'

type NavItem = {
  name?: string
  to?: RouteLocationRaw
  label: string
  icon: string
  roles: string[]
  plugin?: boolean
  networkOnly?: boolean
}
type NavGroup = { label: string; items: NavItem[] }

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const open = ref(false)
const collapsed = ref(false)
const switchingSite = ref(false)
const pluginMenus = ref<PluginAdminMenuItem[]>([])
const siteMemberships = ref<SiteMembership[]>([])
const selectedSite = ref(getSelectedSite() || auth.user.siteKey || '')

const baseGroups: NavGroup[] = [
  { label: 'Principal', items: [
    { name: 'dashboard', label: 'Resumen', icon: 'dashboard', roles: ['ADMIN', 'EDITOR', 'AUTHOR'] }
  ]},
  { label: 'Contenido', items: [
    { name: 'posts', label: 'Publicaciones', icon: 'post', roles: ['ADMIN', 'EDITOR', 'AUTHOR'] },
    { name: 'pages', label: 'Páginas', icon: 'page', roles: ['ADMIN', 'EDITOR', 'AUTHOR'] },
    { name: 'categories', label: 'Categorías', icon: 'category', roles: ['ADMIN', 'EDITOR'] },
    { name: 'media', label: 'Biblioteca de medios', icon: 'media', roles: ['ADMIN', 'EDITOR'] }
  ]},
  { label: 'Extensibilidad', items: [
    { name: 'themes', label: 'Temas', icon: 'theme', roles: ['ADMIN'] },
    { name: 'plugins', label: 'Plugins', icon: 'plugin', roles: ['ADMIN'] }
  ]},
  { label: 'Red', items: [
    { name: 'sites', label: 'Sitios de la red', icon: 'globe', roles: ['ADMIN'], networkOnly: true }
  ]},
  { label: 'Sistema', items: [
    { name: 'users', label: 'Usuarios del sitio', icon: 'users', roles: ['ADMIN'] },
    { name: 'settings', label: 'Configuración', icon: 'settings', roles: ['ADMIN'] },
    { name: 'mail-settings', label: 'Correo electrónico', icon: 'send', roles: ['ADMIN'] },
    { name: 'developer', label: 'API y MCP', icon: 'code', roles: ['ADMIN'] },
    { name: 'account', label: 'Mi cuenta', icon: 'account', roles: ['ADMIN', 'EDITOR', 'AUTHOR'] }
  ]}
]

const currentSite = computed(() => siteMemberships.value.find(site => site.siteKey === selectedSite.value)
  || siteMemberships.value.find(site => site.siteKey === auth.user.siteKey)
  || siteMemberships.value[0])

const pluginGroup = computed<NavGroup | null>(() => {
  if (!['ADMIN','EDITOR'].includes(auth.user.role || '') || !pluginMenus.value.length) return null
  return {
    label: 'Plugins del sitio',
    items: pluginMenus.value.map(item => ({
      label: item.label,
      icon: item.icon || 'plugin',
      roles: item.pluginId === 'trastaccount' ? ['ADMIN'] : ['ADMIN', 'EDITOR'],
      plugin: true,
      to: item.routeName ? { name: item.routeName } : { name: 'plugin-page', params: { pluginId: item.pluginId, pageId: item.id } }
    }))
  }
})

const visibleGroups = computed(() => {
  const groups = [...baseGroups]
  if (pluginGroup.value) groups.splice(3, 0, pluginGroup.value)
  return groups.map(group => ({
    ...group,
    items: group.items.filter(item => item.roles.includes(auth.user.role || '') && (!item.networkOnly || auth.user.networkAdmin))
  })).filter(group => group.items.length)
})

const currentLabel = computed(() => {
  if (route.name === 'plugin-page') return pluginMenus.value.find(item => item.id === String(route.params.pageId))?.label || 'Plugin'
  const pluginItem = pluginMenus.value.find(entry => entry.routeName === route.name)
  if (pluginItem) return pluginItem.label
  const item = baseGroups.flatMap(group => group.items).find(entry =>
    route.name === entry.name || (entry.name && String(route.name).startsWith(entry.name.replace(/s$/, '')))
  )
  return item?.label || 'Administración'
})

async function loadSites() {
  try {
    const values = await api<SiteMembership[]>('/api/admin/sites/available')
    siteMemberships.value = values
    const requested = getSelectedSite()
    const valid = values.find(site => site.siteKey === requested)
    const backend = values.find(site => site.siteKey === auth.user.siteKey)
    const resolved = valid || backend || values[0]
    if (resolved) {
      selectedSite.value = resolved.siteKey
      setSelectedSite(resolved.siteKey)
    }
  } catch {
    siteMemberships.value = []
  }
}

async function loadPluginMenus() {
  if (!['ADMIN','EDITOR'].includes(auth.user.role || '')) return
  try {
    const contributions = await api<PluginContributions>('/api/admin/plugins/contributions')
    pluginMenus.value = contributions.adminMenuItems || []
  } catch { pluginMenus.value = [] }
}

async function switchSite() {
  if (!selectedSite.value || selectedSite.value === auth.user.siteKey) return
  switchingSite.value = true
  setSelectedSite(selectedSite.value)
  try {
    await auth.load()
    await loadPluginMenus()
    await router.push({ name: 'dashboard' })
    window.location.reload()
  } finally { switchingSite.value = false }
}

async function signOut() {
  await auth.logout()
  await router.push({ name: 'login' })
}

onMounted(async () => {
  await loadSites()
  await loadPluginMenus()
})
</script>

<template>
  <div :class="['admin-shell', { 'sidebar-collapsed': collapsed }]">
    <aside :class="['admin-sidebar', { open }]">
      <div class="flex h-20 items-center justify-between px-5">
        <RouterLink to="/" class="flex items-center gap-3 overflow-hidden text-white"><span class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-violet-500 to-sky-400 text-lg font-black shadow-lg shadow-violet-950/30">T</span><span v-if="!collapsed" class="text-xl font-[820] tracking-tight">TrastCMS</span></RouterLink>
        <button class="hidden rounded-lg p-2 text-slate-400 hover:bg-white/10 hover:text-white lg:block" @click="collapsed=!collapsed"><span>{{ collapsed ? '›' : '‹' }}</span></button>
      </div>

      <div v-if="!collapsed && siteMemberships.length" class="mx-3 mb-5 rounded-2xl border border-white/10 bg-white/5 p-3">
        <label for="site-switcher" class="mb-2 block text-[10px] font-bold uppercase tracking-[.16em] text-slate-500">Sitio actual</label>
        <div class="flex items-center gap-2">
          <select id="site-switcher" v-model="selectedSite" :disabled="switchingSite" class="min-w-0 flex-1 rounded-xl border border-white/10 bg-slate-900 px-3 py-2 text-sm font-semibold text-white" @change="switchSite">
            <option v-for="site in siteMemberships" :key="site.siteId" :value="site.siteKey">{{ site.siteName }}</option>
          </select>
          <span class="h-2.5 w-2.5 shrink-0 rounded-full bg-emerald-400" :title="currentSite?.role || ''" />
        </div>
        <small class="mt-2 block truncate text-[11px] text-slate-500">{{ currentSite?.role }} · {{ currentSite?.siteKey }}</small>
      </div>

      <nav class="min-h-0 flex-1 overflow-y-auto px-3 pb-4">
        <section v-for="group in visibleGroups" :key="group.label" class="mb-5">
          <h3 v-if="!collapsed" class="mb-2 px-3 text-[10px] font-bold uppercase tracking-[.16em] text-slate-500">{{ group.label }}</h3>
          <div class="space-y-1">
            <RouterLink v-for="item in group.items" :key="item.label" :to="item.to || { name: item.name }" :title="collapsed ? item.label : undefined" class="group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-400 transition hover:bg-white/7 hover:text-white" @click="open=false"><UiIcon :name="item.icon" :size="19" class="shrink-0"/><span v-if="!collapsed" class="truncate">{{ item.label }}</span></RouterLink>
          </div>
        </section>
      </nav>

      <div class="border-t border-white/8 p-3">
        <div v-if="!collapsed" class="mb-2 flex items-center gap-3 rounded-xl bg-white/5 p-3"><span class="grid h-9 w-9 place-items-center rounded-full bg-violet-500/20 text-sm font-bold text-violet-200">{{ auth.user.displayName?.charAt(0) || 'A' }}</span><div class="min-w-0"><strong class="block truncate text-sm text-white">{{ auth.user.displayName }}</strong><small class="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{{ auth.user.networkAdmin ? 'NETWORK ADMIN' : auth.user.role }}</small></div></div>
        <button class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-rose-400 transition hover:bg-rose-500/10 hover:text-rose-300" title="Cerrar sesión" @click="signOut"><UiIcon name="logout" :size="18"/><span v-if="!collapsed">Cerrar sesión</span></button>
      </div>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <button class="mobile-menu mr-1 text-slate-600" @click="open=!open">☰</button>
        <div class="min-w-0 flex-1"><span class="text-xs font-semibold uppercase tracking-wider text-slate-400">{{ auth.user.siteName || 'TrastCMS' }}</span><h2 class="truncate text-lg font-bold text-slate-900">{{ currentLabel }}</h2></div>
        <RouterLink :to="{ name: 'post-new' }" class="hidden items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-700 md:inline-flex"><UiIcon name="plus" :size="16"/>Publicar</RouterLink>
        <a :href="currentSite?.primaryUrl || '/'" target="_blank" rel="noopener" class="inline-flex items-center gap-2 rounded-xl bg-slate-950 px-3 py-2 text-sm font-semibold text-white hover:bg-slate-800"><UiIcon name="external" :size="16"/><span class="hidden sm:inline">Ver sitio</span></a>
      </header>
      <main class="admin-content"><RouterView /></main>
    </section>
    <button v-if="open" class="fixed inset-0 z-40 bg-slate-950/50 lg:hidden" aria-label="Cerrar menú" @click="open=false" />
  </div>
</template>

<style scoped>
nav a.router-link-active { background:linear-gradient(135deg,rgba(124,58,237,.28),rgba(59,130,246,.16));color:white;box-shadow:inset 0 0 0 1px rgba(167,139,250,.22); }
</style>
