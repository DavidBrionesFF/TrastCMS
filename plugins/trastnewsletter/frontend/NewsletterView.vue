<template>
  <div class="space-y-6">
    <!-- Header principal estilizado -->
    <AdminPageHeader
      eyebrow="Marketing Digital & Enganche"
      title="Newsletter & Notificaciones Push"
      description="Gestiona listas de suscriptores, envía campañas de correo electrónico y emite alertas push directo al navegador."
    >
      <template #actions>
        <button type="button" class="button secondary" :disabled="loading" @click="loadData">
          <UiIcon name="refresh" :size="16" />
          <span>Refrescar</span>
        </button>
        <button type="button" class="button primary" @click="openNewCampaign">
          <UiIcon name="plus" :size="16" />
          <span>Nueva Campaña</span>
        </button>
      </template>
    </AdminPageHeader>

    <!-- Tarjetas de métricas superiores -->
    <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Total Suscriptores</span>
          <div class="text-2xl font-black text-slate-900 mt-1">{{ stats.totalSubscribers }}</div>
        </div>
        <div class="rounded-xl bg-violet-50 p-3 text-violet-600">
          <UiIcon name="users" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Activos en Boletín</span>
          <div class="text-2xl font-black text-emerald-600 mt-1">{{ stats.activeSubscribers }}</div>
        </div>
        <div class="rounded-xl bg-emerald-50 p-3 text-emerald-600">
          <UiIcon name="mail" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Campañas Creadas</span>
          <div class="text-2xl font-black text-slate-800 mt-1">{{ stats.totalCampaigns }}</div>
        </div>
        <div class="rounded-xl bg-indigo-50 p-3 text-indigo-600">
          <UiIcon name="file" :size="22" />
        </div>
      </div>

      <div class="panel p-5 flex items-center justify-between border border-slate-200/80">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Campañas Enviadas</span>
          <div class="text-2xl font-black text-violet-600 mt-1">{{ stats.sentCampaigns }}</div>
        </div>
        <div class="rounded-xl bg-purple-50 p-3 text-purple-600">
          <UiIcon name="bell" :size="22" />
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
      </button>
    </nav>

    <!-- Tab 1: DASHBOARD & PUSH RÁPIDO -->
    <div v-if="tab === 'dashboard'" class="grid gap-6 lg:grid-cols-3">
      <!-- Emisor Push en Vivo + Preview Mockup -->
      <div class="lg:col-span-2 space-y-4">
        <div class="panel p-6 space-y-4 border border-violet-100 bg-gradient-to-br from-white to-violet-50/20">
          <div class="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 class="font-bold text-slate-900 text-base flex items-center gap-2">
              <UiIcon name="bell" :size="20" class="text-violet-600" />
              <span>Emisión de Notificación Web Push Rápida</span>
            </h3>
            <span class="rounded-full bg-violet-100 px-3 py-1 text-xs font-bold text-violet-700">En Vivo</span>
          </div>

          <p class="text-xs text-slate-500">
            Envía una alerta emergente instantánea a todos los navegadores web suscritos a tus publicaciones.
          </p>

          <div class="space-y-3">
            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Título de la Notificación</label>
              <input v-model="quickPush.title" type="text" placeholder="ej: 🌴 ¡Nuevo Tour a Roatán disponible!" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Cuerpo / Mensaje</label>
              <textarea v-model="quickPush.body" placeholder="Reservas abiertas con 20% de descuento por tiempo limitado." class="w-full rounded-xl border border-slate-200 p-2.5 text-sm h-20"></textarea>
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-700 mb-1">Enlace de Destino al hacer clic</label>
              <input v-model="quickPush.url" type="text" placeholder="/travel/tour-roatan" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
            </div>

            <div class="pt-2 flex justify-end">
              <button type="button" class="button primary py-2.5 px-6" :disabled="sendingPush" @click="sendQuickPush">
                <UiIcon name="bell" :size="16" />
                <span>{{ sendingPush ? 'Transmitiendo...' : '🚀 Emitir Push a Suscriptores' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Preview Mockup de la Notificación Push -->
      <div class="space-y-4">
        <div class="panel p-5 space-y-3">
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400 block">Previsualización Push (OS Mockup)</span>

          <div class="rounded-2xl bg-slate-900 p-4 text-white space-y-3 shadow-xl border border-slate-800">
            <div class="flex items-center justify-between text-xs text-slate-400 border-b border-slate-800 pb-2">
              <span class="flex items-center gap-1.5 font-bold">
                <span class="h-2 w-2 rounded-full bg-violet-400"></span>
                TrastCMS Push
              </span>
              <span>ahora</span>
            </div>

            <div class="space-y-1">
              <h4 class="font-bold text-sm text-slate-100 truncate">{{ quickPush.title || 'Título de ejemplo de notificación' }}</h4>
              <p class="text-xs text-slate-300 leading-snug line-clamp-3">{{ quickPush.body || 'Este es el mensaje de la notificación emergente que el usuario verá en su pantalla o dispositivo móvil.' }}</p>
            </div>

            <div class="text-[11px] font-mono text-violet-400 truncate pt-1 border-t border-slate-800">
              🔗 {{ quickPush.url || '/' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 2: CAMPAÑAS DE EMAIL & PUSH -->
    <div v-else-if="tab === 'campaigns'" class="space-y-4">
      <div class="panel p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-base font-bold text-slate-900">Historial de Campañas</h3>
            <p class="text-xs text-slate-500">Organiza y transmite boletines masivos por correo y alertas push.</p>
          </div>
          <button type="button" class="button primary" @click="openNewCampaign">
            <UiIcon name="plus" :size="16" />
            <span>Crear Campaña</span>
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm text-slate-600">
            <thead class="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 font-bold border-b border-slate-200">
              <tr>
                <th class="p-3.5">Título</th>
                <th class="p-3.5">Asunto</th>
                <th class="p-3.5">Estado</th>
                <th class="p-3.5">Receptores</th>
                <th class="p-3.5 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="c in campaigns" :key="c.id" class="hover:bg-slate-50/60 transition">
                <td class="p-3.5 font-bold text-slate-900">{{ c.title }}</td>
                <td class="p-3.5 text-slate-600">{{ c.subject }}</td>
                <td class="p-3.5">
                  <span :class="['inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold ring-1 ring-inset', c.status === 'SENT' ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 'bg-slate-100 text-slate-600 ring-slate-500/20']">
                    {{ c.status }}
                  </span>
                </td>
                <td class="p-3.5 font-mono text-xs text-slate-600">{{ c.recipientCount || 0 }} usuarios</td>
                <td class="p-3.5 text-right">
                  <button v-if="c.status === 'DRAFT'" type="button" class="button primary text-xs py-1 px-3" @click="sendCampaign(c.id)">
                    Enviar Ahora
                  </button>
                </td>
              </tr>
              <tr v-if="campaigns.length === 0">
                <td colspan="5" class="p-8 text-center text-slate-400">
                  No hay campañas registradas. Haz clic en "Crear Campaña" para redactar una.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Tab 3: LISTA DE SUSCRIPTORES -->
    <div v-else-if="tab === 'subscribers'" class="panel p-6 space-y-4">
      <h3 class="text-base font-bold text-slate-900">Directorio de Suscriptores</h3>

      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm text-slate-600">
          <thead class="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 font-bold border-b border-slate-200">
            <tr>
              <th class="p-3.5">Correo Electrónico</th>
              <th class="p-3.5">Nombre</th>
              <th class="p-3.5">Estado</th>
              <th class="p-3.5">Fecha Suscripción</th>
              <th class="p-3.5 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="s in subscribers" :key="s.id" class="hover:bg-slate-50/60 transition">
              <td class="p-3.5 font-bold text-slate-900">{{ s.email }}</td>
              <td class="p-3.5 text-slate-600">{{ s.name || '-' }}</td>
              <td class="p-3.5">
                <span :class="['inline-flex items-center rounded-md px-2.5 py-1 text-xs font-bold ring-1 ring-inset', s.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 'bg-rose-50 text-rose-700 ring-rose-600/20']">
                  {{ s.status }}
                </span>
              </td>
              <td class="p-3.5 text-xs text-slate-500">{{ formatDate(s.subscribedAt) }}</td>
              <td class="p-3.5 text-right">
                <button type="button" class="button border border-rose-200 text-rose-600 hover:bg-rose-50 text-xs py-1 px-3" @click="deleteSubscriber(s.id)">
                  Eliminar
                </button>
              </td>
            </tr>
            <tr v-if="subscribers.length === 0">
              <td colspan="5" class="p-8 text-center text-slate-400">
                No hay suscriptores en la lista.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tab 4: CONFIGURACIÓN BOLETÍN -->
    <div v-else-if="tab === 'settings'" class="panel p-6 space-y-6">
      <h3 class="text-base font-bold text-slate-900 border-b border-slate-100 pb-3">Configuración de Envío de Email & Remitente</h3>

      <form class="space-y-5" @submit.prevent="saveSettings">
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Nombre del Remitente</label>
            <input v-model="settings.senderName" type="text" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-700 mb-1">Email del Remitente</label>
            <input v-model="settings.senderEmail" type="email" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm" />
          </div>
        </div>

        <div>
          <label class="block text-xs font-bold text-slate-700 mb-1">Texto al Pie del Correo (Unsubscribe Notice)</label>
          <textarea v-model="settings.footerText" class="w-full rounded-xl border border-slate-200 p-2.5 text-sm h-20"></textarea>
        </div>

        <div class="pt-4 flex justify-end">
          <button type="submit" class="button primary py-2.5 px-6">
            <UiIcon name="check" :size="16" />
            <span>Guardar Configuración</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/services/api'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const tab = ref('dashboard')
const loading = ref(false)
const sendingPush = ref(false)
const stats = ref({ totalSubscribers: 0, activeSubscribers: 0, totalCampaigns: 0, sentCampaigns: 0 })
const campaigns = ref<any[]>([])
const subscribers = ref<any[]>([])

const quickPush = ref({ title: '', body: '', url: '/' })

const settings = ref({
  senderName: 'TrastCMS',
  senderEmail: 'noreply@trastcms.com',
  replyTo: '',
  doubleOptIn: false,
  logoUrl: '',
  footerText: 'TrastCMS Newsletter'
})

const tabs = [
  { id: 'dashboard', label: 'Emisión Push & Resumen', icon: 'bell' },
  { id: 'campaigns', label: 'Campañas', icon: 'mail' },
  { id: 'subscribers', label: 'Suscriptores', icon: 'users' },
  { id: 'settings', label: 'Remitente & Ajustes', icon: 'settings' }
]

function formatDate(iso: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleDateString('es-HN', {
    day: '2-digit', month: 'short', year: 'numeric'
  })
}

async function loadData() {
  loading.value = true
  try {
    const [st, c, sub, set]: [any, any, any, any] = await Promise.all([
      api('/api/admin/newsletter/stats').catch(() => null),
      api('/api/admin/newsletter/campaigns').catch(() => null),
      api('/api/admin/newsletter/subscribers').catch(() => null),
      api('/api/admin/newsletter/settings').catch(() => null)
    ])
    if (st) stats.value = st
    if (c) campaigns.value = c.content || []
    if (sub) subscribers.value = sub.content || []
    if (set) settings.value = { ...settings.value, ...set }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function sendQuickPush() {
  if (!quickPush.value.title || !quickPush.value.body) return alert('Ingresa título y mensaje para la alerta Push.')
  sendingPush.value = true
  try {
    await api('/api/admin/push/test', {
      method: 'POST',
      body: JSON.stringify(quickPush.value)
    })
    alert('Notificación Push enviada correctamente.')
    quickPush.value = { title: '', body: '', url: '/' }
  } catch (e) {
    alert('Error al emitir notificación Push')
  } finally {
    sendingPush.value = false
  }
}

async function openNewCampaign() {
  const title = prompt('Título interno de la campaña:')
  if (!title) return
  const subject = prompt('Asunto del correo electrónico:')
  if (!subject) return
  try {
    await api('/api/admin/newsletter/campaigns', {
      method: 'POST',
      body: JSON.stringify({
        title,
        subject,
        bodyHtml: '<p>Hola {{name}}, ¡tenemos novedades en nuestro sitio!</p>',
        pushEnabled: true,
        pushTitle: title,
        pushBody: subject,
        pushUrl: '/'
      })
    })
    await loadData()
  } catch (e) {
    alert('Error al crear campaña')
  }
}

async function sendCampaign(id: string) {
  if (!confirm('¿Transmitir esta campaña a todos los suscriptores?')) return
  try {
    await api(`/api/admin/newsletter/campaigns/${id}/send`, {
      method: 'POST',
      body: JSON.stringify({ includeEmail: true, includePush: true })
    })
    alert('Campaña transmitida exitosamente.')
    await loadData()
  } catch (e) {
    alert('Error al enviar la campaña')
  }
}

async function deleteSubscriber(id: string) {
  if (!confirm('¿Eliminar suscriptor?')) return
  try {
    await api(`/api/admin/newsletter/subscribers/${id}`, { method: 'DELETE' })
    await loadData()
  } catch (e) {}
}

async function saveSettings() {
  try {
    await api('/api/admin/newsletter/settings', {
      method: 'PUT',
      body: JSON.stringify(settings.value)
    })
    alert('Configuración guardada correctamente.')
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>
