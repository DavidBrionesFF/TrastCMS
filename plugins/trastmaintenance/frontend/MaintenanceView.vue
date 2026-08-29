<template>
  <div class="space-y-6">
    <!-- Header principal estilizado con el sistema de diseño del CMS -->
    <AdminPageHeader
      eyebrow="Salud & Rendimiento"
      title="Mantenimiento del Sistema"
      description="Supervisa el estado del servidor, gestiona copias de seguridad locales y en la nube (Google Drive) y optimiza las cachés."
    >
      <template #actions>
        <button type="button" class="button secondary" :disabled="loading" @click="loadData">
          <UiIcon name="refresh" :size="16" />
          <span>{{ loading ? 'Actualizando...' : 'Actualizar métricas' }}</span>
        </button>
        <button type="button" class="button primary" @click="optimizeDb">
          <UiIcon name="sparkles" :size="16" />
          <span>Optimizar BD</span>
        </button>
      </template>
    </AdminPageHeader>

    <!-- Pestañas de navegación superiores con íconos -->
    <nav class="flex space-x-1 rounded-2xl bg-slate-100 p-1.5 border border-slate-200/80 shadow-inner">
      <button
        v-for="item in tabs"
        :key="item.id"
        type="button"
        :class="[
          'flex items-center gap-2.5 rounded-xl px-5 py-2.5 text-sm font-semibold transition-all duration-200',
          tab === item.id
            ? 'bg-white text-violet-700 shadow-md shadow-violet-500/5 ring-1 ring-black/5'
            : 'text-slate-600 hover:bg-white/60 hover:text-slate-900'
        ]"
        @click="tab = item.id"
      >
        <UiIcon :name="item.icon" :size="18" />
        <span>{{ item.label }}</span>
        <span
          v-if="item.id === 'backups' && backups.length > 0"
          class="ml-1 rounded-full bg-violet-100 px-2 py-0.5 text-xs font-bold text-violet-700"
        >
          {{ backups.length }}
        </span>
      </button>
    </nav>

    <!-- Pestaña 1: DASHBOARD DE SALUD -->
    <div v-if="tab === 'dashboard'" class="space-y-6">
      <!-- Tarjetas de métricas principales -->
      <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Estado BD -->
        <div class="panel p-5 space-y-3 relative overflow-hidden">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Base de Datos</span>
            <div class="rounded-xl bg-emerald-50 p-2 text-emerald-600">
              <UiIcon name="database" :size="20" />
            </div>
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="h-3 w-3 rounded-full bg-emerald-500 animate-pulse"></span>
              <span class="text-lg font-bold text-slate-800">{{ health?.dbConnected ? 'Conectada & Estable' : 'Desconectada' }}</span>
            </div>
            <p class="text-xs text-slate-500 mt-1">Motor: H2 / PostgreSQL JPA</p>
          </div>
          <button type="button" class="w-full button secondary text-xs justify-center py-2" @click="optimizeDb">
            <UiIcon name="sparkles" :size="14" />
            <span>Ejecutar CHECKPOINT / ANALYZE</span>
          </button>
        </div>

        <!-- Memoria Heap JVM -->
        <div class="panel p-5 space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Memoria Heap (JVM)</span>
            <div class="rounded-xl bg-violet-50 p-2 text-violet-600">
              <UiIcon name="cpu" :size="20" />
            </div>
          </div>
          <div>
            <div class="flex items-baseline justify-between">
              <span class="text-2xl font-black text-slate-900">{{ health?.heapUsedMb || 0 }} <span class="text-xs font-normal text-slate-500">MB</span></span>
              <span class="text-xs font-semibold text-slate-500">de {{ health?.heapMaxMb || 0 }} MB</span>
            </div>
            <div class="w-full bg-slate-100 rounded-full h-2.5 mt-2 overflow-hidden">
              <div
                class="h-full rounded-full transition-all duration-500"
                :class="heapPercentage > 80 ? 'bg-rose-500' : heapPercentage > 60 ? 'bg-amber-500' : 'bg-violet-600'"
                :style="{ width: `${heapPercentage}%` }"
              ></div>
            </div>
            <p class="text-xs text-slate-400 mt-1 text-right">{{ heapPercentage }}% consumido</p>
          </div>
        </div>

        <!-- Carga de CPU -->
        <div class="panel p-5 space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Carga Procesador</span>
            <div class="rounded-xl bg-sky-50 p-2 text-sky-600">
              <UiIcon name="activity" :size="20" />
            </div>
          </div>
          <div>
            <div class="text-2xl font-black text-slate-900">{{ cpuDisplay }}</div>
            <p class="text-xs text-slate-500 mt-1">Hilos paralelos asignados</p>
          </div>
        </div>

        <!-- Tiempo de Actividad / Uptime -->
        <div class="panel p-5 space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Tiempo Activo (Uptime)</span>
            <div class="rounded-xl bg-amber-50 p-2 text-amber-600">
              <UiIcon name="clock" :size="20" />
            </div>
          </div>
          <div>
            <div class="text-xl font-bold text-slate-800">{{ formatUptime(health?.uptimeSeconds || 0) }}</div>
            <p class="text-xs text-slate-400 mt-1">{{ health?.osName }} &bull; Java {{ health?.javaVersion }}</p>
          </div>
        </div>
      </div>

      <!-- Sección de Acciones Rápidas -->
      <div class="panel p-6 space-y-4">
        <h3 class="text-base font-bold text-slate-900 flex items-center gap-2">
          <UiIcon name="shield-check" :size="20" class="text-violet-600" />
          <span>Acciones de Mantenimiento Inmediato</span>
        </h3>
        <div class="grid gap-4 sm:grid-cols-3">
          <button type="button" class="panel p-4 hover:border-violet-300 hover:shadow-md transition text-left space-y-2 group" @click="triggerBackup('DATABASE')">
            <div class="flex items-center justify-between">
              <div class="rounded-xl bg-violet-50 p-2 text-violet-600 group-hover:bg-violet-600 group-hover:text-white transition">
                <UiIcon name="database" :size="20" />
              </div>
              <span class="text-xs font-bold text-violet-600">Lanzar</span>
            </div>
            <h4 class="font-bold text-slate-800 text-sm">Backup Base de Datos</h4>
            <p class="text-xs text-slate-500">Crea una copia de seguridad en caliente de las tablas SQL.</p>
          </button>

          <button type="button" class="panel p-4 hover:border-violet-300 hover:shadow-md transition text-left space-y-2 group" @click="triggerBackup('FULL')">
            <div class="flex items-center justify-between">
              <div class="rounded-xl bg-indigo-50 p-2 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white transition">
                <UiIcon name="archive" :size="20" />
              </div>
              <span class="text-xs font-bold text-indigo-600">Lanzar</span>
            </div>
            <h4 class="font-bold text-slate-800 text-sm">Backup Completo (BD + Uploads)</h4>
            <p class="text-xs text-slate-500">Genera un paquete ZIP comprimido con todo el estado del CMS.</p>
          </button>

          <button type="button" class="panel p-4 hover:border-rose-300 hover:shadow-md transition text-left space-y-2 group" @click="evictCaches">
            <div class="flex items-center justify-between">
              <div class="rounded-xl bg-rose-50 p-2 text-rose-600 group-hover:bg-rose-600 group-hover:text-white transition">
                <UiIcon name="trash" :size="20" />
              </div>
              <span class="text-xs font-bold text-rose-600">Vaciar</span>
            </div>
            <h4 class="font-bold text-slate-800 text-sm">Purgar Memoria Caché</h4>
            <p class="text-xs text-slate-500">Limpia las cachés de traducción, páginas y datos dinámicos.</p>
          </button>
        </div>
      </div>
    </div>

    <!-- Pestaña 2: BACKUPS -->
    <div v-else-if="tab === 'backups'" class="space-y-5">
      <div class="panel p-6 space-y-4">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h3 class="text-base font-bold text-slate-900">Copias de Seguridad (Backups)</h3>
            <p class="text-xs text-slate-500">Descarga tus respaldos locales o envíalos a la nube de Google Drive.</p>
          </div>
          <div class="flex items-center gap-2">
            <button type="button" class="button secondary" @click="triggerBackup('DATABASE')">
              <UiIcon name="database" :size="16" />
              <span>Backup BD</span>
            </button>
            <button type="button" class="button primary" @click="triggerBackup('FULL')">
              <UiIcon name="plus" :size="16" />
              <span>Nuevo Backup Completo</span>
            </button>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm text-slate-600">
            <thead class="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 font-bold border-b border-slate-200">
              <tr>
                <th class="p-3.5">Tipo</th>
                <th class="p-3.5">Estado</th>
                <th class="p-3.5">Tamaño</th>
                <th class="p-3.5">Fecha de Creación</th>
                <th class="p-3.5 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="b in backups" :key="b.id" class="hover:bg-slate-50/60 transition">
                <td class="p-3.5 font-bold text-slate-900 flex items-center gap-2">
                  <UiIcon :name="b.backupType === 'FULL' ? 'archive' : 'database'" :size="18" class="text-violet-600" />
                  <span>{{ b.backupType === 'FULL' ? 'Completo (ZIP)' : 'Base de Datos (SQL)' }}</span>
                </td>
                <td class="p-3.5">
                  <span :class="['inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold ring-1 ring-inset', statusBadgeClass(b.status)]">
                    {{ b.status }}
                  </span>
                </td>
                <td class="p-3.5 font-mono text-xs text-slate-600">
                  {{ b.fileSizeBytes ? (b.fileSizeBytes / (1024 * 1024)).toFixed(2) + ' MB' : '-' }}
                </td>
                <td class="p-3.5 text-xs text-slate-500">
                  {{ formatDate(b.createdAt) }}
                </td>
                <td class="p-3.5 text-right space-x-2">
                  <a v-if="b.status === 'DONE'" :href="`/api/admin/maintenance/backups/${b.id}/download`" target="_blank" class="button secondary py-1 px-3 text-xs inline-flex items-center gap-1">
                    <UiIcon name="download" :size="14" />
                    <span>Descargar</span>
                  </a>
                  <button v-if="b.status === 'DONE' && settings?.googleDriveEnabled" type="button" class="button ghost py-1 px-3 text-xs" @click="uploadToDrive(b.id)">
                    <UiIcon name="cloud" :size="14" />
                    <span>Drive</span>
                  </button>
                  <button type="button" class="button border border-rose-200 text-rose-600 hover:bg-rose-50 py-1 px-3 text-xs" @click="deleteBackup(b.id)">
                    <UiIcon name="trash" :size="14" />
                  </button>
                </td>
              </tr>
              <tr v-if="backups.length === 0">
                <td colspan="5" class="p-8 text-center text-slate-400">
                  No hay copias de seguridad registradas. Genera una con los botones superiores.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Pestaña 3: CACHÉ -->
    <div v-else-if="tab === 'cache'" class="space-y-5">
      <div class="panel p-6 space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-base font-bold text-slate-900">Gestión de Caché en Memoria</h3>
            <p class="text-xs text-slate-500">Optimiza el tiempo de respuesta del sitio vaciando la memoria RAM retenida.</p>
          </div>
          <button type="button" class="button border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100" @click="evictCaches">
            <UiIcon name="trash" :size="16" />
            <span>Vaciar todas las Cachés</span>
          </button>
        </div>

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div v-for="c in caches" :key="c.name" class="panel p-5 space-y-2 border border-slate-200/80 hover:border-violet-300 transition">
            <div class="flex items-center justify-between">
              <h4 class="font-bold text-slate-800 text-sm uppercase tracking-wide">{{ c.name }}</h4>
              <span class="rounded-full bg-violet-100 px-2.5 py-0.5 text-xs font-bold text-violet-700">Activa</span>
            </div>
            <p class="text-xs text-slate-500">{{ c.description || 'Caché de contenido dinámico' }}</p>
            <div class="pt-2 text-xs font-mono text-slate-400">Entradas estimadas: {{ c.size || 0 }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pestaña 4: CONFIGURACIÓN -->
    <div v-else-if="tab === 'config'" class="panel p-6 space-y-6">
      <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">Configuración de Mantenimiento & Cloud</h3>
      <form class="space-y-5" @submit.prevent="saveSettings">
        <div class="space-y-4">
          <h4 class="text-xs font-bold uppercase tracking-wider text-violet-600">1. Respaldos Automáticos</h4>
          <div class="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50">
            <div>
              <label class="font-bold text-slate-800 text-sm block">Auto-Backup Programado</label>
              <p class="text-xs text-slate-500">Ejecuta copias de seguridad periódicas mediante expresiones cron.</p>
            </div>
            <input v-model="settings.autoBackupEnabled" type="checkbox" class="h-5 w-5 rounded text-violet-600 focus:ring-violet-500" />
          </div>

          <div v-if="settings.autoBackupEnabled" class="grid gap-4 sm:grid-cols-2 pt-2">
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Expresión Cron</label>
              <input v-model="settings.backupCron" type="text" placeholder="0 0 2 * * ?" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
              <p class="text-[11px] text-slate-400 mt-1">Por defecto: Todos los días a las 2:00 AM (0 0 2 * * ?)</p>
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Retención de Backups (Días)</label>
              <input v-model="settings.backupRetentionDays" type="number" min="1" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
            </div>
          </div>
        </div>

        <div class="space-y-4 pt-4 border-t border-slate-100">
          <h4 class="text-xs font-bold uppercase tracking-wider text-violet-600">2. Integración con Google Drive API</h4>
          <div class="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50">
            <div>
              <label class="font-bold text-slate-800 text-sm block">Subida Automática a Google Drive</label>
              <p class="text-xs text-slate-500">Almacena las copias de seguridad de forma segura en tu unidad corporativa de Google.</p>
            </div>
            <input v-model="settings.googleDriveEnabled" type="checkbox" class="h-5 w-5 rounded text-violet-600 focus:ring-violet-500" />
          </div>

          <div v-if="settings.googleDriveEnabled" class="grid gap-4 sm:grid-cols-2 pt-2">
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Google Client ID</label>
              <input v-model="settings.googleDriveClientId" type="text" placeholder="apps.googleusercontent.com" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Google Client Secret</label>
              <input v-model="settings.googleDriveClientSecret" type="password" placeholder="••••••••••••••••" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
            </div>
            <div class="sm:col-span-2">
              <label class="block text-xs font-bold text-slate-700 mb-1">Google Refresh Token</label>
              <input v-model="settings.googleDriveRefreshToken" type="text" placeholder="1//0G..." class="w-full rounded-xl border border-slate-200 p-2.5 text-sm font-mono" />
            </div>
          </div>
        </div>

        <div class="pt-4 flex justify-end">
          <button type="submit" class="button primary py-2.5 px-6" :disabled="saving">
            <UiIcon name="check" :size="16" />
            <span>{{ saving ? 'Guardando...' : 'Guardar Configuración' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api } from '@/services/api'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const tab = ref('dashboard')
const loading = ref(false)
const saving = ref(false)
const health = ref<any>(null)
const backups = ref<any[]>([])
const caches = ref<any[]>([])
const settings = ref<any>({
  autoBackupEnabled: false,
  backupCron: '0 0 2 * * ?',
  backupRetentionDays: 30,
  googleDriveEnabled: false,
  googleDriveClientId: '',
  googleDriveClientSecret: '',
  googleDriveRefreshToken: ''
})

const tabs = [
  { id: 'dashboard', label: 'Salud & Métricas', icon: 'dashboard' },
  { id: 'backups', label: 'Copias de Seguridad', icon: 'database' },
  { id: 'cache', label: 'Memoria Caché', icon: 'cpu' },
  { id: 'config', label: 'Configuración Cloud', icon: 'settings' }
]

const heapPercentage = computed(() => {
  if (!health.value || !health.value.heapMaxMb) return 0
  return Math.min(100, Math.round((health.value.heapUsedMb / health.value.heapMaxMb) * 100))
})

const cpuDisplay = computed(() => {
  if (!health.value || health.value.cpuUsage === undefined) return 'N/A'
  if (health.value.cpuUsage < 0) return 'Normal (JVM)'
  return `${(health.value.cpuUsage * 100).toFixed(1)}%`
})

function formatDate(iso: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString('es-HN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

function formatUptime(sec: number) {
  const d = Math.floor(sec / 86400)
  const h = Math.floor((sec % 86400) / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return `${d}d ${h}h ${m}m`
}

function statusBadgeClass(status: string) {
  switch (status) {
    case 'DONE': return 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
    case 'RUNNING': return 'bg-amber-50 text-amber-700 ring-amber-600/20'
    case 'FAILED': return 'bg-rose-50 text-rose-700 ring-rose-600/20'
    default: return 'bg-slate-100 text-slate-600 ring-slate-500/20'
  }
}

async function loadData() {
  loading.value = true
  try {
    const [h, b, c, s]: [any, any, any, any] = await Promise.all([
      api('/api/admin/maintenance/health').catch(() => null),
      api('/api/admin/maintenance/backups').catch(() => null),
      api('/api/admin/maintenance/cache/stats').catch(() => null),
      api('/api/admin/maintenance/settings').catch(() => null)
    ])
    if (h) health.value = h
    if (b) backups.value = b.content || []
    if (c) caches.value = c.caches || []
    if (s) settings.value = { ...settings.value, ...s }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function triggerBackup(type: string) {
  try {
    await api('/api/admin/maintenance/backups', {
      method: 'POST',
      body: JSON.stringify({ backupType: type })
    })
    alert('Copia de seguridad iniciada en segundo plano.')
    await loadData()
  } catch (e) {
    alert('Error al iniciar backup')
  }
}

async function uploadToDrive(id: string) {
  try {
    await api(`/api/admin/maintenance/backups/${id}/upload-drive`, { method: 'POST' })
    alert('Subida a Google Drive iniciada.')
  } catch (e) {
    alert('Error al subir a Google Drive')
  }
}

async function deleteBackup(id: string) {
  if (!confirm('¿Eliminar archivo de copia de seguridad?')) return
  try {
    await api(`/api/admin/maintenance/backups/${id}`, { method: 'DELETE' })
    await loadData()
  } catch (e) {
    alert('Error al eliminar copia de seguridad')
  }
}

async function evictCaches() {
  if (!confirm('¿Purgar todas las cachés del sistema?')) return
  try {
    await api('/api/admin/maintenance/cache/evict', { method: 'POST' })
    alert('Cachés purgadas exitosamente.')
    await loadData()
  } catch (e) {
    alert('Error al vaciar caché')
  }
}

async function optimizeDb() {
  try {
    await api('/api/admin/maintenance/optimize', { method: 'POST' })
    alert('Optimización de base de datos ejecutada exitosamente.')
  } catch (e) {
    alert('Error al optimizar BD')
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await api('/api/admin/maintenance/settings', {
      method: 'PUT',
      body: JSON.stringify(settings.value)
    })
    alert('Configuración guardada correctamente.')
  } catch (e) {
    alert('Error al guardar la configuración')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
