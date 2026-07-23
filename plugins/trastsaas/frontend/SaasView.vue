<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'

type SaasTab =
  | 'dashboard'
  | 'plans'
  | 'licenses'
  | 'subscriptions'
  | 'api'

type JsonRecord = Record<string, any>

const tab = ref<SaasTab>('dashboard')
const data = ref<JsonRecord>({})
const products = ref<JsonRecord[]>([])
const plans = ref<JsonRecord[]>([])
const licenses = ref<JsonRecord[]>([])
const subscriptions = ref<JsonRecord[]>([])
const planEditor = ref(false)
const licenseEditor = ref(false)
const plan = reactive<JsonRecord>({})
const issue = reactive<JsonRecord>({})

function emptyPlan(): JsonRecord {
  return {
    id: '',
    productId: products.value[0]?.id || '',
    planKey: '',
    name: '',
    description: '',
    price: 0,
    currency: 'USD',
    billingInterval: 'MONTH',
    intervalCount: 1,
    trialDays: 14,
    maxActivations: 3,
    entitlements: {
      'users.max': 5,
      'api.enabled': true
    },
    active: true,
    featured: false
  }
}

async function load(): Promise<void> {
  const [
    loadedDashboard,
    loadedProducts,
    loadedPlans,
    loadedLicenses,
    loadedSubscriptions
  ] = await Promise.all([
    api<JsonRecord>('/api/admin/saas/dashboard'),
    api<JsonRecord[]>('/api/admin/saas/products'),
    api<JsonRecord[]>('/api/admin/saas/plans'),
    api<JsonRecord[]>('/api/admin/saas/licenses'),
    api<JsonRecord[]>('/api/admin/saas/subscriptions')
  ])

  data.value = loadedDashboard
  products.value = loadedProducts
  plans.value = loadedPlans
  licenses.value = loadedLicenses
  subscriptions.value = loadedSubscriptions
}

function editPlan(value?: JsonRecord): void {
  Object.assign(plan, emptyPlan(), value || {})
  planEditor.value = true
}

async function savePlan(): Promise<void> {
  await api(
    plan.id
      ? `/api/admin/saas/plans/${plan.id}`
      : '/api/admin/saas/plans',
    {
      method: plan.id ? 'PUT' : 'POST',
      body: JSON.stringify(plan)
    }
  )

  planEditor.value = false
  await load()
}

function newLicense(): void {
  Object.assign(issue, {
    planId: plans.value[0]?.id || '',
    customerEmail: '',
    expiresAt: null,
    maxActivations: null,
    orderId: null
  })

  licenseEditor.value = true
}

async function saveLicense(): Promise<void> {
  const created = await api<JsonRecord>('/api/admin/saas/licenses', {
    method: 'POST',
    body: JSON.stringify(issue)
  })

  licenseEditor.value = false
  await load()
  window.alert(
    `Clave creada. Guárdela ahora:\n${created.licenseKey}`
  )
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', {
    style: 'currency',
    currency
  }).format(value || 0)
}

onMounted(load)
</script>

<template><div class="space-y-6"><header class="flex flex-wrap items-end justify-between gap-4"><div><span class="eyebrow">TRASTSAAS</span><h1 class="text-3xl font-black">SaaS y licencias</h1><p class="text-slate-500">Planes, suscripciones, activaciones, entitlements, uso medido y releases.</p></div><div class="flex gap-2"><button class="button secondary" @click="tab='licenses';newLicense()">Emitir licencia</button><button class="button" @click="tab='plans';editPlan()">Nuevo plan</button></div></header><nav class="plugin-tabs"><button v-for="i in [{id:'dashboard',label:'Resumen'},{id:'plans',label:'Planes'},{id:'licenses',label:'Licencias'},{id:'subscriptions',label:'Suscripciones'},{id:'api',label:'API de activación'}]" :key="i.id" :class="{active:tab===i.id}" @click="tab=i.id as any">{{i.label}}</button></nav><section v-if="tab==='dashboard'" class="space-y-6"><div class="grid gap-4 md:grid-cols-4"><article class="metric-card"><span>Suscripciones activas</span><strong>{{data.activeSubscriptions}}</strong><small>{{data.subscriptions}} totales</small></article><article class="metric-card"><span>Licencias activas</span><strong>{{data.activeLicenses}}</strong><small>{{data.licenses}} emitidas</small></article><article class="metric-card"><span>Activaciones</span><strong>{{data.activations}}</strong></article><article class="metric-card"><span>Eventos de uso</span><strong>{{data.usageEvents}}</strong></article></div><div class="grid gap-6 xl:grid-cols-2"><section class="panel"><h2 class="text-xl font-bold">Planes destacados</h2><article v-for="p in data.featuredPlans" :key="p.id" class="mt-4 rounded-2xl border border-violet-200 bg-violet-50 p-5"><div class="flex justify-between"><div><b class="text-lg">{{p.name}}</b><p class="text-sm text-slate-500">{{p.productKey}}</p></div><strong>{{money(p.price,p.currency)}}/{{p.billingInterval}}</strong></div></article></section><section class="panel"><h2 class="text-xl font-bold">Licencias recientes</h2><div v-for="l in data.recentLicenses" :key="l.id" class="mt-3 flex justify-between rounded-xl border p-4"><div><b>{{l.customerEmail}}</b><small class="block">{{l.productKey}} · {{l.planKey}}</small></div><span class="status-pill ok">{{l.status}}</span></div></section></div></section><section v-else-if="tab==='plans'" class="grid gap-4 lg:grid-cols-3"><article v-for="p in plans" :key="p.id" class="panel relative"><span v-if="p.featured" class="status-pill ok">Recomendado</span><h2 class="mt-3 text-2xl font-black">{{p.name}}</h2><p class="text-sm text-slate-500">{{p.description}}</p><strong class="mt-5 block text-3xl text-violet-700">{{money(p.price,p.currency)}}<small class="text-sm">/{{p.billingInterval}}</small></strong><ul class="mt-5 space-y-2 text-sm"><li v-for="(value,key) in p.entitlements" :key="key">✓ {{key}}: {{value}}</li></ul><button class="button secondary mt-5 w-full" @click="editPlan(p)">Editar plan</button></article></section><section v-else-if="tab==='licenses'" class="panel"><div class="overflow-x-auto"><table class="data-table"><thead><tr><th>Licencia</th><th>Cliente</th><th>Plan</th><th>Activaciones</th><th>Estado</th><th>Expira</th></tr></thead><tbody><tr v-for="l in licenses" :key="l.id"><td><code>{{l.maskedKey}}</code></td><td>{{l.customerEmail}}</td><td>{{l.productKey}} · {{l.planKey}}</td><td>{{l.activeActivations}} / {{l.maxActivations}}</td><td><span class="status-pill ok">{{l.status}}</span></td><td>{{l.expiresAt?new Date(l.expiresAt).toLocaleDateString():'Sin vencimiento'}}</td></tr></tbody></table></div></section><section v-else-if="tab==='subscriptions'" class="panel"><div class="overflow-x-auto"><table class="data-table"><thead><tr><th>Cliente</th><th>Producto</th><th>Plan</th><th>Estado</th><th>Periodo</th><th>Orden</th></tr></thead><tbody><tr v-for="s in subscriptions" :key="s.id"><td>{{s.customerEmail}}</td><td>{{s.productKey}}</td><td>{{s.planName}}</td><td><span class="status-pill">{{s.status}}</span></td><td>{{s.currentPeriodEnd?new Date(s.currentPeriodEnd).toLocaleDateString():'Perpetuo'}}</td><td>{{s.orderId||'Manual'}}</td></tr></tbody></table></div></section><section v-else class="grid gap-6 xl:grid-cols-2"><article class="panel"><span class="eyebrow">REST API</span><h2 class="mt-2 text-2xl font-bold">Activación de licencias</h2><pre class="code-block mt-4">POST /api/public/saas/licenses/activate
{
  "licenseKey": "TRAST-...",
  "productKey": "trast-cloud",
  "fingerprint": "sha256-device",
  "platform": "windows"
}</pre></article><article class="panel"><span class="eyebrow">SEGURIDAD</span><h2 class="mt-2 text-2xl font-bold">Buenas prácticas incorporadas</h2><ul class="mt-4 space-y-3"><li>✓ Claves cifradas y hash SHA-256</li><li>✓ Límite de activaciones</li><li>✓ Heartbeat y desactivación</li><li>✓ Idempotencia para uso medido</li><li>✓ Respuestas con firma verificable</li></ul></article></section><div v-if="planEditor" class="modal-backdrop"><form class="modal-card max-w-3xl" @submit.prevent="savePlan"><header><h2 class="text-2xl font-bold">{{plan.id?'Editar':'Nuevo'}} plan</h2><button type="button" class="icon-button" @click="planEditor=false">×</button></header><div class="grid gap-4 md:grid-cols-2"><label>Producto<select v-model="plan.productId"><option v-for="p in products" :key="p.id" :value="p.id">{{p.name}}</option></select></label><label>Clave<input v-model="plan.planKey" required></label><label>Nombre<input v-model="plan.name" required></label><label>Precio<input v-model.number="plan.price" type="number" step="0.01"></label><label>Intervalo<select v-model="plan.billingInterval"><option>MONTH</option><option>YEAR</option><option>ONE_TIME</option></select></label><label>Días de prueba<input v-model.number="plan.trialDays" type="number"></label><label>Máximo de activaciones<input v-model.number="plan.maxActivations" type="number"></label><label class="md:col-span-2">Descripción<textarea v-model="plan.description"></textarea></label><label class="md:col-span-2">Entitlements JSON<textarea :value="JSON.stringify(plan.entitlements,null,2)" @input="plan.entitlements=JSON.parse(($event.target as HTMLTextAreaElement).value||'{}')"></textarea></label><label class="check-row"><input v-model="plan.active" type="checkbox">Activo</label><label class="check-row"><input v-model="plan.featured" type="checkbox">Destacado</label></div><footer><button class="button" type="submit">Guardar</button></footer></form></div><div v-if="licenseEditor" class="modal-backdrop"><form class="modal-card max-w-lg" @submit.prevent="saveLicense"><header><h2 class="text-xl font-bold">Emitir licencia manual</h2><button type="button" class="icon-button" @click="licenseEditor=false">×</button></header><label>Plan<select v-model="issue.planId"><option v-for="p in plans" :key="p.id" :value="p.id">{{p.productKey}} · {{p.name}}</option></select></label><label>Correo del cliente<input v-model="issue.customerEmail" type="email" required></label><label>Máximo de activaciones<input v-model.number="issue.maxActivations" type="number" placeholder="Usar valor del plan"></label><footer><button class="button">Emitir licencia</button></footer></form></div></div></template>
