<template>
  <div class="space-y-6">
    <!-- Header principal estilizado -->
    <AdminPageHeader
      eyebrow="Interacción & Comunidad"
      title="Gestión de Comentarios"
      description="Modera los comentarios recibidos, gestiona estados (Aprobados, Pendientes, SPAM) y ajusta reglas anti-spam."
    >
      <template #actions>
        <button type="button" class="button secondary" :disabled="loading" @click="loadData">
          <UiIcon name="refresh" :size="16" />
          <span>Refrescar</span>
        </button>
      </template>
    </AdminPageHeader>

    <!-- Tarjetas de Métricas de Comentarios -->
    <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Total Comentarios</span>
          <div class="text-2xl font-black text-slate-900 mt-1">{{ stats.total }}</div>
        </div>
        <div class="rounded-xl bg-violet-50 p-3 text-violet-600">
          <UiIcon name="comment" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Por Moderar</span>
          <div class="text-2xl font-black text-amber-600 mt-1 flex items-center gap-2">
            <span>{{ stats.pending }}</span>
            <span v-if="stats.pending > 0" class="h-2.5 w-2.5 rounded-full bg-amber-500 animate-ping"></span>
          </div>
        </div>
        <div class="rounded-xl bg-amber-50 p-3 text-amber-600">
          <UiIcon name="clock" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Aprobados</span>
          <div class="text-2xl font-black text-emerald-600 mt-1">{{ stats.approved }}</div>
        </div>
        <div class="rounded-xl bg-emerald-50 p-3 text-emerald-600">
          <UiIcon name="check-circle" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Marcados SPAM</span>
          <div class="text-2xl font-black text-rose-600 mt-1">{{ stats.spam }}</div>
        </div>
        <div class="rounded-xl bg-rose-50 p-3 text-rose-600">
          <UiIcon name="shield" :size="22" />
        </div>
      </div>
    </div>

    <!-- Pestañas de Navegación estilizadas -->
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
          v-if="item.badge > 0"
          class="ml-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-bold text-amber-800"
        >
          {{ item.badge }}
        </span>
      </button>
    </nav>

    <!-- Tab 1: MODERACIÓN PENDIENTE -->
    <div v-if="tab === 'moderation'" class="space-y-4">
      <div v-if="loading" class="panel p-8 text-center text-slate-500">
        <UiIcon name="refresh" :size="28" class="animate-spin mx-auto text-violet-600 mb-2" />
        <span>Cargando comentarios pendientes...</span>
      </div>

      <div v-else-if="pendingComments.length === 0" class="panel p-10 text-center text-slate-500 space-y-3">
        <div class="h-16 w-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto">
          <UiIcon name="check-circle" :size="32" />
        </div>
        <h3 class="text-lg font-bold text-slate-800">¡Bandeja de Moderación Limpia!</h3>
        <p class="text-xs text-slate-500 max-w-sm mx-auto">No hay comentarios pendientes. Los nuevos envíos aparecerán aquí para tu revisión.</p>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="c in pendingComments"
          :key="c.id"
          class="panel p-6 space-y-4 border border-slate-200/80 hover:border-violet-300 transition"
        >
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="flex items-center gap-3">
              <div class="h-10 w-10 rounded-full bg-violet-600 text-white font-bold flex items-center justify-center text-sm shadow-sm">
                {{ initials(c.authorName) }}
              </div>
              <div>
                <h4 class="font-bold text-slate-900 text-base">{{ c.authorName }}</h4>
                <p class="text-xs text-slate-500">{{ c.authorEmail }} &bull; {{ formatDate(c.createdAt) }}</p>
              </div>
            </div>
            <span class="inline-flex items-center rounded-md bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-700 ring-1 ring-inset ring-amber-600/20">
              Pendiente de Moderación
            </span>
          </div>

          <div class="bg-slate-50/80 p-4 rounded-xl border border-slate-100 text-sm text-slate-800 leading-relaxed whitespace-pre-wrap">
            {{ c.body }}
          </div>

          <div class="flex items-center justify-between pt-1">
            <div class="text-xs font-semibold text-slate-400 flex items-center gap-1">
              <UiIcon name="file" :size="14" />
              <span>Post ID: {{ c.postId }}</span>
            </div>

            <div class="flex items-center gap-2">
              <button type="button" class="button border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs py-1.5 px-3" @click="updateStatus(c.id, 'SPAM')">
                🚫 Marcar SPAM
              </button>
              <button type="button" class="button border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs py-1.5 px-3" @click="deleteComment(c.id)">
                🗑️ Eliminar
              </button>
              <button type="button" class="button primary text-xs py-1.5 px-4 bg-emerald-600 hover:bg-emerald-700" @click="updateStatus(c.id, 'APPROVED')">
                ✅ Aprobar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 2: TODOS LOS COMENTARIOS -->
    <div v-else-if="tab === 'all'" class="panel p-6 space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <select v-model="statusFilter" class="rounded-xl border border-slate-200 px-3.5 py-2 text-sm bg-white font-medium" @change="loadAll">
            <option value="">Todos los Estados</option>
            <option value="APPROVED">Aprobados</option>
            <option value="PENDING">Pendientes</option>
            <option value="SPAM">Spam</option>
          </select>
        </div>
        <button type="button" class="button secondary text-xs" @click="loadAll">
          <UiIcon name="refresh" :size="14" />
          <span>Actualizar Lista</span>
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm text-slate-600">
          <thead class="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 font-bold border-b border-slate-200">
            <tr>
              <th class="p-3.5">Autor</th>
              <th class="p-3.5">Comentario</th>
              <th class="p-3.5">Estado</th>
              <th class="p-3.5">Fecha</th>
              <th class="p-3.5 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="c in allComments" :key="c.id" class="hover:bg-slate-50/60 transition">
              <td class="p-3.5 font-bold text-slate-900">
                <div>{{ c.authorName }}</div>
                <div class="text-xs text-slate-400 font-normal">{{ c.authorEmail }}</div>
              </td>
              <td class="p-3.5 max-w-sm truncate text-slate-700 font-normal">{{ c.body }}</td>
              <td class="p-3.5">
                <span :class="['inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold ring-1 ring-inset', statusBadgeClass(c.status)]">
                  {{ c.status }}
                </span>
              </td>
              <td class="p-3.5 text-xs text-slate-500 whitespace-nowrap">{{ formatDate(c.createdAt) }}</td>
              <td class="p-3.5 text-right space-x-2">
                <button v-if="c.status !== 'APPROVED'" type="button" class="text-xs text-emerald-600 font-bold hover:underline" @click="updateStatus(c.id, 'APPROVED')">
                  Aprobar
                </button>
                <button v-if="c.status !== 'SPAM'" type="button" class="text-xs text-rose-600 font-bold hover:underline" @click="updateStatus(c.id, 'SPAM')">
                  Spam
                </button>
                <button type="button" class="text-xs text-slate-400 hover:text-slate-600" @click="deleteComment(c.id)">
                  Eliminar
                </button>
              </td>
            </tr>
            <tr v-if="allComments.length === 0">
              <td colspan="5" class="p-8 text-center text-slate-400">
                No hay comentarios registrados para el filtro seleccionado.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tab 3: CONFIGURACIÓN -->
    <div v-else-if="tab === 'settings'" class="panel p-6 space-y-6">
      <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">Reglas de Moderación & Seguridad</h3>

      <form class="space-y-5" @submit.prevent="saveSettings">
        <div class="space-y-3">
          <div class="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50">
            <div>
              <label class="font-bold text-slate-800 text-sm block">Habilitar Comentarios en Posts</label>
              <p class="text-xs text-slate-500">Permite publicar cajas de interacción en el blog.</p>
            </div>
            <input v-model="settings.enabled" type="checkbox" class="h-5 w-5 rounded text-violet-600" />
          </div>

          <div class="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50">
            <div>
              <label class="font-bold text-slate-800 text-sm block">Requiere Moderación Previa</label>
              <p class="text-xs text-slate-500">Los comentarios permanecen en estado pendiente hasta ser aprobados por un editor.</p>
            </div>
            <input v-model="settings.requireModeration" type="checkbox" class="h-5 w-5 rounded text-violet-600" />
          </div>

          <div class="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50/50">
            <div>
              <label class="font-bold text-slate-800 text-sm block">Permitir Comentarios de Invitados (No Autenticados)</label>
              <p class="text-xs text-slate-500">Permite que cualquier visitante comente ingresando su Nombre y Email.</p>
            </div>
            <input v-model="settings.allowGuests" type="checkbox" class="h-5 w-5 rounded text-violet-600" />
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2 pt-2 border-t border-slate-100">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Límite por Hora por IP (Anti-Flood)</label>
            <input v-model.number="settings.maxPerHourIp" type="number" min="1" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
          </div>

          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Email de Notificación al recibir Comentario</label>
            <input v-model="settings.notifyEmail" type="email" placeholder="admin@sitio.com" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
          </div>
        </div>

        <div class="pt-4 flex justify-end">
          <button type="submit" class="button primary py-2.5 px-6" :disabled="saving">
            <UiIcon name="check" :size="16" />
            <span>{{ saving ? 'Guardando...' : 'Guardar Ajustes' }}</span>
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

const tab = ref('moderation')
const loading = ref(false)
const saving = ref(false)
const pendingComments = ref<any[]>([])
const allComments = ref<any[]>([])
const statusFilter = ref('')
const stats = ref({ total: 0, pending: 0, approved: 0, spam: 0 })

const settings = ref({
  enabled: true,
  requireModeration: true,
  allowGuests: true,
  allowNested: true,
  maxNestingDepth: 3,
  notifyOnComment: false,
  notifyEmail: '',
  maxPerHourIp: 5,
  honeypotEnabled: true
})

const tabs = computed(() => [
  { id: 'moderation', label: 'Bandeja de Moderación', icon: 'clock', badge: pendingComments.value.length },
  { id: 'all', label: 'Historial de Comentarios', icon: 'comment', badge: 0 },
  { id: 'settings', label: 'Reglas & Ajustes', icon: 'settings', badge: 0 }
])

function initials(name: string) {
  if (!name) return 'U'
  return name.split(' ').map(n => n.charAt(0)).join('').substring(0, 2).toUpperCase()
}

function formatDate(iso: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString('es-HN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

function statusBadgeClass(status: string) {
  switch (status) {
    case 'APPROVED': return 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
    case 'PENDING': return 'bg-amber-50 text-amber-700 ring-amber-600/20'
    case 'SPAM': return 'bg-rose-50 text-rose-700 ring-rose-600/20'
    default: return 'bg-slate-100 text-slate-600 ring-slate-500/20'
  }
}

async function loadData() {
  loading.value = true
  try {
    const [p, a, s, st]: [any, any, any, any] = await Promise.all([
      api('/api/admin/comments?status=PENDING&size=50').catch(() => null),
      api('/api/admin/comments?size=50').catch(() => null),
      api('/api/admin/comments/settings').catch(() => null),
      api('/api/admin/comments/stats').catch(() => null)
    ])
    if (p) pendingComments.value = p.content || []
    if (a) allComments.value = a.content || []
    if (s) settings.value = { ...settings.value, ...s }
    if (st) {
      stats.value = st
    } else {
      stats.value = {
        total: allComments.value.length,
        pending: pendingComments.value.length,
        approved: allComments.value.filter(c => c.status === 'APPROVED').length,
        spam: allComments.value.filter(c => c.status === 'SPAM').length
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  try {
    let url = '/api/admin/comments?size=50'
    if (statusFilter.value) url += `&status=${statusFilter.value}`
    const res: any = await api(url)
    allComments.value = res?.content || []
  } catch (e) {
    console.error(e)
  }
}

async function updateStatus(id: string, status: string) {
  try {
    await api(`/api/admin/comments/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    })
    await loadData()
  } catch (e) {
    alert('Error al actualizar el estado del comentario')
  }
}

async function deleteComment(id: string) {
  if (!confirm('¿Eliminar definitivamente este comentario?')) return
  try {
    await api(`/api/admin/comments/${id}`, { method: 'DELETE' })
    await loadData()
  } catch (e) {
    alert('Error al eliminar comentario')
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await api('/api/admin/comments/settings', {
      method: 'PUT',
      body: JSON.stringify(settings.value)
    })
    alert('Reglas de comentarios guardadas correctamente.')
  } catch (e) {
    alert('Error al guardar ajustes')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
