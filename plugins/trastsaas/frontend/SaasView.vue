<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'
import UiIcon from '@/components/common/UiIcon.vue'

type SaasTab = 'dashboard' | 'plans' | 'licenses' | 'subscriptions' | 'settings' | 'api'
type JsonRecord = Record<string, any>

const toast = useToastStore()
const tab = ref<SaasTab>('dashboard')
const data = ref<JsonRecord>({})
const products = ref<JsonRecord[]>([])
const plans = ref<JsonRecord[]>([])
const licenses = ref<JsonRecord[]>([])
const subscriptions = ref<JsonRecord[]>([])
const settings = reactive<JsonRecord>({})
const loading = ref(true)
const saving = ref(false)
const demoBusy = ref(false)
const productEditor = ref(false)
const planEditor = ref(false)
const licenseEditor = ref(false)
const search = ref('')
const statusFilter = ref('ALL')
const productDraft = reactive<JsonRecord>({})
const plan = reactive<JsonRecord>({})
const issue = reactive<JsonRecord>({})

// Interactive API State
const selectedEndpoint = ref<'activate' | 'validate' | 'claim' | 'revoke'>('activate')
const codeLanguage = ref<'curl' | 'js' | 'python' | 'java'>('curl')

const filteredLicenses = computed(() =>
  licenses.value.filter(item => {
    const term = search.value.trim().toLowerCase()
    const statusMatches = statusFilter.value === 'ALL' || item.status === statusFilter.value
    return (
      statusMatches &&
      (!term ||
        `${item.customerEmail} ${item.productName} ${item.planName} ${item.maskedKey}`
          .toLowerCase()
          .includes(term))
    )
  })
)

const filteredSubscriptions = computed(() =>
  subscriptions.value.filter(item => {
    const term = search.value.trim().toLowerCase()
    const statusMatches = statusFilter.value === 'ALL' || item.status === statusFilter.value
    return (
      statusMatches &&
      (!term ||
        `${item.customerEmail} ${item.productName} ${item.planName}`
          .toLowerCase()
          .includes(term))
    )
  })
)

function emptyProduct(): JsonRecord {
  return {
    id: '',
    productKey: '',
    name: '',
    description: '',
    logoUrl: '',
    active: true
  }
}

function editProduct(value?: JsonRecord): void {
  Object.assign(productDraft, emptyProduct(), value || {})
  productEditor.value = true
}

async function saveProduct(): Promise<void> {
  saving.value = true
  try {
    await api(
      productDraft.id
        ? `/api/admin/saas/products/${productDraft.id}`
        : '/api/admin/saas/products',
      {
        method: productDraft.id ? 'PUT' : 'POST',
        body: JSON.stringify(productDraft)
      }
    )
    productEditor.value = false
    toast.success('Producto SaaS guardado', `${productDraft.name} ya puede recibir planes.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo guardar el producto SaaS', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

function emptyPlan(): JsonRecord {
  return {
    id: '',
    productId: products.value[0]?.id || '',
    planKey: '',
    name: '',
    description: '',
    price: 0,
    currency: settings.defaultCurrency || 'USD',
    billingInterval: 'MONTH',
    intervalCount: 1,
    trialDays: settings.defaultTrialDays ?? 14,
    maxActivations: 3,
    entitlements: { 'users.max': 5, 'api.enabled': true },
    active: true,
    featured: false
  }
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

function readableInterval(value: string): string {
  return (
    ({
      MONTH: 'mes',
      YEAR: 'año',
      WEEK: 'semana',
      DAY: 'día',
      ONE_TIME: 'pago único'
    } as Record<string, string>)[value] || value
  )
}

function statusBadgeClass(status: string): string {
  if (['ACTIVE', 'PAID'].includes(status)) return 'ok'
  if (['TRIALING'].includes(status)) return 'info'
  if (['PAST_DUE', 'PAUSED'].includes(status)) return 'warning'
  if (['CANCELLED', 'EXPIRED', 'REVOKED', 'UNPAID'].includes(status)) return 'danger'
  return 'muted'
}

async function load(): Promise<void> {
  loading.value = true
  const requests = await Promise.allSettled([
    api<JsonRecord>('/api/admin/saas/dashboard'),
    api<JsonRecord[]>('/api/admin/saas/products'),
    api<JsonRecord[]>('/api/admin/saas/plans'),
    api<JsonRecord[]>('/api/admin/saas/licenses'),
    api<JsonRecord[]>('/api/admin/saas/subscriptions'),
    api<JsonRecord>('/api/admin/saas/settings')
  ])

  const [
    dashboardResult,
    productsResult,
    plansResult,
    licensesResult,
    subscriptionsResult,
    settingsResult
  ] = requests

  if (dashboardResult.status === 'fulfilled') data.value = dashboardResult.value
  if (productsResult.status === 'fulfilled') products.value = productsResult.value
  if (plansResult.status === 'fulfilled') plans.value = plansResult.value
  if (licensesResult.status === 'fulfilled') licenses.value = licensesResult.value
  if (subscriptionsResult.status === 'fulfilled') subscriptions.value = subscriptionsResult.value
  if (settingsResult.status === 'fulfilled') Object.assign(settings, settingsResult.value)

  loading.value = false
}

function editPlan(value?: JsonRecord): void {
  if (!products.value.length) {
    toast.warning('Primero cree un producto SaaS', 'Los planes deben pertenecer a un producto de software.')
    editProduct()
    return
  }
  Object.assign(plan, emptyPlan(), value || {})
  plan.entitlementsText = JSON.stringify(plan.entitlements || {}, null, 2)
  planEditor.value = true
}

async function savePlan(): Promise<void> {
  saving.value = true
  try {
    let entitlements: JsonRecord = {}
    try {
      entitlements = JSON.parse(plan.entitlementsText || '{}')
    } catch {
      toast.warning('JSON inválido', 'Revise los entitlements antes de guardar.')
      return
    }
    if (!plan.productId) {
      toast.warning('Seleccione un producto SaaS')
      return
    }
    const { entitlementsText, ...payload } = plan
    await api(plan.id ? `/api/admin/saas/plans/${plan.id}` : '/api/admin/saas/plans', {
      method: plan.id ? 'PUT' : 'POST',
      body: JSON.stringify({
        ...payload,
        currency: String(plan.currency || 'USD').toUpperCase(),
        entitlements
      })
    })
    planEditor.value = false
    toast.success('Plan guardado', `${plan.name} ya está disponible.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo guardar el plan', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
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
  saving.value = true
  try {
    const created = await api<JsonRecord>('/api/admin/saas/licenses', {
      method: 'POST',
      body: JSON.stringify(issue)
    })
    licenseEditor.value = false
    toast.push('Licencia emitida', { message: `Clave: ${created.licenseKey}`, tone: 'success', timeout: 0 })
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo emitir la licencia', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function updateSubscription(item: JsonRecord, status: string): Promise<void> {
  try {
    await api(`/api/admin/saas/subscriptions/${item.id}`, {
      method: 'PUT',
      body: JSON.stringify({ status, cancelAtPeriodEnd: item.cancelAtPeriodEnd })
    })
    toast.success('Suscripción actualizada', `${item.customerEmail} ahora está en ${status}.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo actualizar', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function updateLicense(item: JsonRecord, status: string): Promise<void> {
  try {
    await api(`/api/admin/saas/licenses/${item.id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    })
    toast.success('Licencia actualizada')
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo actualizar la licencia', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function saveSettings(): Promise<void> {
  saving.value = true
  try {
    Object.assign(
      settings,
      await api<JsonRecord>('/api/admin/saas/settings', {
        method: 'PUT',
        body: JSON.stringify(settings)
      })
    )
    toast.success('Configuración SaaS guardada')
  } catch (caught: unknown) {
    toast.error('No se pudo guardar la configuración', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function demo(action: 'generate' | 'delete'): Promise<void> {
  if (action === 'delete' && !window.confirm('¿Eliminar productos, planes, licencias y suscripciones demo?'))
    return
  demoBusy.value = true
  try {
    const result = await api<JsonRecord>('/api/admin/saas/demo-data', {
      method: action === 'generate' ? 'POST' : 'DELETE'
    })
    toast.success(action === 'generate' ? 'Demo SaaS generado' : 'Demo SaaS eliminado', result.message)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudieron procesar los datos demo', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    demoBusy.value = false
  }
}

// Snippets de Código Dinámicos para API
const sampleSnippets = computed(() => {
  if (selectedEndpoint.value === 'activate') {
    return {
      curl: `curl -X POST "https://mi-dominio.com/api/public/saas/licenses/activate" \\
  -H "Content-Type: application/json" \\
  -d '{
    "licenseKey": "TRAST-2026-PRO-9842",
    "productKey": "trast-cloud",
    "fingerprint": "a3f89e71c9b2",
    "deviceName": "Laptop Trabajo",
    "platform": "windows"
  }'`,
      js: `const response = await fetch('/api/public/saas/licenses/activate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    licenseKey: 'TRAST-2026-PRO-9842',
    productKey: 'trast-cloud',
    fingerprint: 'a3f89e71c9b2',
    deviceName: 'Laptop Trabajo',
    platform: 'windows'
  })
});
const data = await response.json();
console.log('Licencia activada:', data.status, data.entitlements);`,
      python: `import requests

url = "https://mi-dominio.com/api/public/saas/licenses/activate"
payload = {
    "licenseKey": "TRAST-2026-PRO-9842",
    "productKey": "trast-cloud",
    "fingerprint": "a3f89e71c9b2",
    "deviceName": "Laptop Trabajo",
    "platform": "windows"
}
response = requests.post(url, json=payload)
print(response.json())`,
      java: `HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://mi-dominio.com/api/public/saas/licenses/activate"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("{\\"licenseKey\\":\\"TRAST-2026-PRO-9842\\",\\"productKey\\":\\"trast-cloud\\"}"))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());`
    }
  } else if (selectedEndpoint.value === 'validate') {
    return {
      curl: `curl -X POST "https://mi-dominio.com/api/public/saas/licenses/validate" \\
  -H "Content-Type: application/json" \\
  -d '{
    "licenseKey": "TRAST-2026-PRO-9842",
    "fingerprint": "a3f89e71c9b2"
  }'`,
      js: `const response = await fetch('/api/public/saas/licenses/validate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    licenseKey: 'TRAST-2026-PRO-9842',
    fingerprint: 'a3f89e71c9b2'
  })
});
const valid = await response.json();`,
      python: `import requests
res = requests.post("https://mi-dominio.com/api/public/saas/licenses/validate", json={
    "licenseKey": "TRAST-2026-PRO-9842",
    "fingerprint": "a3f89e71c9b2"
})
print(res.json())`,
      java: `// Heartbeat de validación periódico`
    }
  } else if (selectedEndpoint.value === 'claim') {
    return {
      curl: `curl -X GET "https://mi-dominio.com/api/public/saas/orders/TR-20260725-842247/claim?token=4810ec5ee11e"`,
      js: `const response = await fetch('/api/public/saas/orders/TR-20260725-842247/claim?token=4810ec5ee11e');
const claim = await response.json();`,
      python: `import requests
res = requests.get("https://mi-dominio.com/api/public/saas/orders/TR-20260725-842247/claim?token=4810ec5ee11e")`,
      java: `// Consulta del portal de licencias del cliente`
    }
  } else {
    return {
      curl: `curl -X POST "https://mi-dominio.com/api/public/saas/licenses/revoke" \\
  -H "Content-Type: application/json" \\
  -d '{ "licenseKey": "TRAST-2026-PRO-9842", "fingerprint": "a3f89e71c9b2" }'`,
      js: `const response = await fetch('/api/public/saas/licenses/revoke', { method: 'POST', body: JSON.stringify({ licenseKey, fingerprint }) });`,
      python: `requests.post("https://mi-dominio.com/api/public/saas/licenses/revoke", json={"licenseKey": key, "fingerprint": fp})`,
      java: `// Liberación de cupo de activación`
    }
  }
})

onMounted(load)
</script>

<template>
  <div class="plugin-workspace saas-admin-workspace">
    <!-- Hero Header -->
    <header class="plugin-hero plugin-hero--saas">
      <div>
        <span class="eyebrow">TRASTSAAS · GESTIÓN DE PRODUCTOS Y SUSCRIPCIONES SAAS</span>
        <h1>Suscripciones y licencias</h1>
        <p>Monetización de software, planes de pago, emisión de licencias y control de dispositivos.</p>
      </div>
      <div class="plugin-hero__actions">
        <button class="button secondary" :disabled="demoBusy" @click="demo('generate')">
          <UiIcon name="sparkles" /> Generar demo
        </button>
        <button class="button secondary danger-outline" :disabled="demoBusy || !data.demoProducts" @click="demo('delete')">
          Eliminar demo
        </button>
        <button class="button secondary" @click="tab = 'licenses'; newLicense()">
          🔑 Emitir licencia
        </button>
        <button class="button secondary" @click="tab = 'plans'; editProduct()">
          + Nuevo producto
        </button>
        <button class="button" @click="tab = 'plans'; editPlan()">
          + Nuevo plan
        </button>
      </div>
    </header>

    <!-- Navigation Tabs -->
    <nav class="plugin-tabs plugin-tabs--cards" aria-label="Navegación SaaS">
      <button :class="{ active: tab === 'dashboard' }" @click="tab = 'dashboard'">
        📊 Resumen
      </button>
      <button :class="{ active: tab === 'plans' }" @click="tab = 'plans'">
        📦 Productos y planes ({{ plans.length }})
      </button>
      <button :class="{ active: tab === 'licenses' }" @click="tab = 'licenses'">
        🔑 Licencias ({{ licenses.length }})
      </button>
      <button :class="{ active: tab === 'subscriptions' }" @click="tab = 'subscriptions'">
        🔄 Suscripciones ({{ subscriptions.length }})
      </button>
      <button :class="{ active: tab === 'settings' }" @click="tab = 'settings'">
        ⚙️ Configuración
      </button>
      <button :class="{ active: tab === 'api' }" @click="tab = 'api'">
        ⚡ API & Integración
      </button>
    </nav>

    <!-- Skeleton Loading -->
    <section v-if="loading" class="saas-admin-skeleton">
      <article v-for="index in 5" :key="index" class="panel">
        <SkeletonBlock :lines="4" />
      </article>
    </section>

    <template v-else>
      <!-- TAB 1: DASHBOARD OVERVIEW -->
      <section v-if="tab === 'dashboard'" class="space-y-6">
        <div class="saas-metric-grid">
          <article class="saas-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon success">📊</span>
              <span class="metric-label">Suscripciones Activas</span>
            </div>
            <strong class="metric-value">{{ data.activeSubscriptions || 0 }}</strong>
            <small class="metric-sub">de {{ data.subscriptions || 0 }} registradas</small>
          </article>

          <article class="saas-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon primary">⏳</span>
              <span class="metric-label">En Periodo de Prueba</span>
            </div>
            <strong class="metric-value">{{ data.trialingSubscriptions || 0 }}</strong>
            <small class="metric-sub">Conversiones pendientes</small>
          </article>

          <article class="saas-metric-card" :class="{ alert: (data.pastDueSubscriptions || 0) > 0 }">
            <div class="metric-card__header">
              <span class="metric-icon warning">⚠️</span>
              <span class="metric-label">Pago Vencido</span>
            </div>
            <strong class="metric-value">{{ data.pastDueSubscriptions || 0 }}</strong>
            <small class="metric-sub">Requieren renovación</small>
          </article>

          <article class="saas-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon muted">🔑</span>
              <span class="metric-label">Licencias Emitidas</span>
            </div>
            <strong class="metric-value">{{ data.activeLicenses || 0 }}</strong>
            <small class="metric-sub">de {{ data.licences || 0 }} totales</small>
          </article>

          <article class="saas-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon info">⚡</span>
              <span class="metric-label">Activaciones de Equipos</span>
            </div>
            <strong class="metric-value">{{ data.activations || 0 }}</strong>
            <small class="metric-sub">{{ data.usageEvents || 0 }} eventos de uso</small>
          </article>
        </div>

        <div class="saas-dashboard-columns">
          <!-- Featured Plans Panel -->
          <section class="panel">
            <div class="panel-heading">
              <div>
                <span class="eyebrow">OFERTA ACTIVA</span>
                <h2>Planes destacados en producción</h2>
              </div>
              <button class="text-link" @click="tab = 'plans'">Administrar planes →</button>
            </div>

            <div class="saas-featured-plans-list">
              <article v-for="item in data.featuredPlans" :key="item.id" class="featured-plan-row">
                <div>
                  <strong>{{ item.productName }} · {{ item.name }}</strong>
                  <p>{{ item.description }}</p>
                </div>
                <div class="plan-price-tag">
                  <strong>{{ money(item.price, item.currency) }}</strong>
                  <small>/{{ readableInterval(item.billingInterval) }}</small>
                </div>
              </article>

              <p v-if="!(data.featuredPlans || []).length" class="empty-cell">
                No hay planes marcados como destacados.
              </p>
            </div>
          </section>

          <!-- Recent Licenses Panel -->
          <section class="panel">
            <div class="panel-heading">
              <div>
                <span class="eyebrow">SEGURIDAD Y CONTROL</span>
                <h2>Licencias recientes</h2>
              </div>
            </div>

            <div class="saas-recent-licenses-list">
              <article v-for="item in data.recentLicenses" :key="item.id" class="recent-license-row">
                <div class="license-info">
                  <strong>{{ item.customerEmail }}</strong>
                  <small>{{ item.productName }} · {{ item.planName }}</small>
                </div>
                <span :class="['status-pill', statusBadgeClass(item.status)]">
                  {{ item.status }}
                </span>
              </article>

              <p v-if="!(data.recentLicenses || []).length" class="empty-cell">
                Sin licencias emitidas recientemente.
              </p>
            </div>
          </section>
        </div>
      </section>

      <!-- TAB 2: PRODUCTS AND PLANS -->
      <section v-else-if="tab === 'plans'" class="space-y-5">
        <div class="panel saas-catalog-header">
          <div>
            <span class="eyebrow">CATÁLOGO DE SOFTWARE</span>
            <h2>Productos y planes SaaS</h2>
            <p>{{ products.length }} productos de software · {{ plans.length }} planes de cobro</p>
          </div>
          <div class="toolbar-actions">
            <button class="button secondary" @click="editProduct()">
              + Nuevo producto SaaS
            </button>
            <button class="button" @click="editPlan()">
              + Nuevo plan
            </button>
          </div>
        </div>

        <div v-if="!products.length" class="panel public-empty-state">
          <h3>No hay productos SaaS registrados</h3>
          <p>Cree su primer producto de software antes de definir precios y licencias.</p>
          <button class="button" @click="editProduct()">Crear primer producto</button>
        </div>

        <div v-for="product in products" :key="product.id" class="saas-product-group panel">
          <div class="saas-product-header">
            <div class="product-identity">
              <img v-if="product.logoUrl" :src="product.logoUrl" alt="" class="product-logo">
              <div>
                <h3>{{ product.name }}</h3>
                <p>{{ product.description }}</p>
                <code class="product-key-tag">Clave: {{ product.productKey }}</code>
              </div>
            </div>
            <div class="product-header-actions">
              <span v-if="product.demoData" class="status-pill info">Demo</span>
              <button class="button secondary small" @click="editProduct(product)">
                ✏️ Editar producto
              </button>
            </div>
          </div>

          <div class="saas-plans-grid">
            <article
              v-for="item in plans.filter(planItem => planItem.productId === product.id)"
              :key="item.id"
              :class="['saas-plan-card', { featured: item.featured }]"
            >
              <span v-if="item.featured" class="plan-featured-badge">⭐ Recomendado</span>
              <span v-if="item.demoData" class="status-pill info">Demo</span>

              <div class="plan-card-body">
                <h3>{{ item.name }}</h3>
                <p>{{ item.description }}</p>

                <div class="plan-price-box">
                  <strong>{{ money(item.price, item.currency) }}</strong>
                  <small>/{{ readableInterval(item.billingInterval) }}</small>
                </div>

                <div class="plan-meta-chips">
                  <span>⏳ {{ item.trialDays }} días de prueba</span>
                  <span>💻 Max {{ item.maxActivations }} equipos</span>
                </div>

                <ul class="entitlements-list">
                  <li v-for="(value, key) in item.entitlements" :key="key">
                    ✓ {{ key }}: {{ value }}
                  </li>
                </ul>
              </div>

              <footer>
                <button class="button secondary full" @click="editPlan(item)">
                  ✏️ Editar plan
                </button>
              </footer>
            </article>

            <article
              v-if="!plans.some(planItem => planItem.productId === product.id)"
              class="saas-plan-card empty-plan-card"
            >
              <h3>Sin planes configurados</h3>
              <p>Defina el primer esquema de cobro para {{ product.name }}.</p>
              <button class="button small" @click="editPlan({ productId: product.id })">
                + Crear plan
              </button>
            </article>
          </div>
        </div>
      </section>

      <!-- TAB 3: LICENSES TABLE -->
      <section v-else-if="tab === 'licenses'" class="panel">
        <div class="panel-heading panel-heading--filters">
          <div>
            <span class="eyebrow">ACCESO Y SEGURIDAD</span>
            <h2>Gestión de licencias emitidas</h2>
          </div>
          <div class="filter-row">
            <input v-model="search" placeholder="Buscar cliente, plan o clave..." class="search-mini-input">
            <select v-model="statusFilter" class="select-mini-input">
              <option value="ALL">Todos los estados</option>
              <option v-for="value in ['ACTIVE','SUSPENDED','REVOKED','EXPIRED']" :key="value" :value="value">
                {{ value }}
              </option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>Clave de Licencia</th>
                <th>Cliente / Correo</th>
                <th>Producto y Plan</th>
                <th>Equipos Activados</th>
                <th>Vencimiento</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredLicenses" :key="item.id">
                <td>
                  <code class="license-code">{{ item.maskedKey }}</code>
                  <small v-if="item.demoData" class="demo-tag">Demo</small>
                </td>
                <td><strong>{{ item.customerEmail }}</strong></td>
                <td>
                  <strong>{{ item.productName }}</strong>
                  <small class="block-muted">{{ item.planName }}</small>
                </td>
                <td>
                  <strong>{{ item.activeActivations }} / {{ item.maxActivations }}</strong>
                </td>
                <td>
                  {{ item.expiresAt ? new Date(item.expiresAt).toLocaleDateString() : 'Perpetua / Sin vencimiento' }}
                </td>
                <td>
                  <select
                    :value="item.status"
                    class="status-select-mini"
                    @change="updateLicense(item, ($event.target as HTMLSelectElement).value)"
                  >
                    <option v-for="value in ['ACTIVE','SUSPENDED','REVOKED','EXPIRED']" :key="value" :value="value">
                      {{ value }}
                    </option>
                  </select>
                </td>
              </tr>
              <tr v-if="!filteredLicenses.length">
                <td colspan="6" class="empty-cell">No hay licencias que coincidan con los filtros.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- TAB 4: SUBSCRIPTIONS TABLE -->
      <section v-else-if="tab === 'subscriptions'" class="panel">
        <div class="panel-heading panel-heading--filters">
          <div>
            <span class="eyebrow">INGRESO RECURRENTE</span>
            <h2>Suscripciones registradas</h2>
          </div>
          <div class="filter-row">
            <input v-model="search" placeholder="Buscar cliente o plan..." class="search-mini-input">
            <select v-model="statusFilter" class="select-mini-input">
              <option value="ALL">Todos los estados</option>
              <option v-for="value in ['TRIALING','ACTIVE','PAST_DUE','PAUSED','CANCELLED','EXPIRED','UNPAID']" :key="value" :value="value">
                {{ value }}
              </option>
            </select>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="data-table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Producto y Plan</th>
                <th>Periodo de Cobro</th>
                <th>Modalidad Renovación</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredSubscriptions" :key="item.id">
                <td>
                  <strong>{{ item.customerEmail }}</strong>
                  <small v-if="item.demoData" class="demo-tag">Demo</small>
                </td>
                <td>
                  <strong>{{ item.productName }}</strong>
                  <small class="block-muted">{{ item.planName }}</small>
                </td>
                <td>
                  {{ item.startedAt ? new Date(item.startedAt).toLocaleDateString() : '—' }} →
                  {{ item.currentPeriodEnd ? new Date(item.currentPeriodEnd).toLocaleDateString() : 'Perpetuo' }}
                </td>
                <td>
                  <span :class="item.cancelAtPeriodEnd ? 'text-warning' : 'text-success'">
                    {{ item.cancelAtPeriodEnd ? 'Cancela al final' : 'Renovación automática' }}
                  </span>
                </td>
                <td>
                  <select
                    :value="item.status"
                    class="status-select-mini"
                    @change="updateSubscription(item, ($event.target as HTMLSelectElement).value)"
                  >
                    <option v-for="value in ['TRIALING','ACTIVE','PAST_DUE','PAUSED','CANCELLED','EXPIRED','UNPAID']" :key="value" :value="value">
                      {{ value }}
                    </option>
                  </select>
                </td>
              </tr>
              <tr v-if="!filteredSubscriptions.length">
                <td colspan="5" class="empty-cell">No hay suscripciones que coincidan con los filtros.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- TAB 5: SETTINGS -->
      <section v-else-if="tab === 'settings'" class="panel max-w-6xl">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">CONFIGURACIÓN GLOBAL SAAS</span>
            <h2>Parámetros de licencias y cobro</h2>
            <p>Personalice periodos de prueba, días de gracia, textos públicos y portal.</p>
          </div>
        </div>

        <form class="settings-grid" @submit.prevent="saveSettings">
          <fieldset>
            <legend>Marca y Moneda Principal</legend>
            <div class="grid gap-4 md:grid-cols-2">
              <label>Nombre Comercial<input v-model="settings.companyName" required></label>
              <label>Moneda predeterminada<input v-model="settings.defaultCurrency" maxlength="3" required></label>
              <label>Correo de Soporte<input v-model="settings.supportEmail" type="email"></label>
              <label>Correo de Facturación<input v-model="settings.billingEmail" type="email"></label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Pruebas y Reglas de Licencia</legend>
            <div class="grid gap-4 md:grid-cols-2">
              <label>Días de prueba predeterminados<input v-model.number="settings.defaultTrialDays" type="number" min="0" max="365"></label>
              <label>Días de gracia por mora<input v-model.number="settings.defaultGraceDays" type="number" min="0" max="90"></label>
              <label class="check-row"><input v-model="settings.allowSelfServiceCancel" type="checkbox">Permitir cancelación desde el portal del cliente</label>
              <label class="check-row"><input v-model="settings.allowPlanChanges" type="checkbox">Permitir cambio de plan (Upgrade/Downgrade)</label>
              <label class="check-row"><input v-model="settings.requireDeviceName" type="checkbox">Exigir nombre del dispositivo al activar</label>
              <label class="check-row"><input v-model="settings.showAnnualSavings" type="checkbox">Mostrar cálculo de ahorro anual en precios</label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Textos Públicos del Portal</legend>
            <div class="grid gap-4">
              <label>Título de la página de precios<input v-model="settings.pricingHeadline"></label>
              <label>Descripción pública<textarea v-model="settings.pricingDescription" rows="2"></textarea></label>
              <label>Mensaje del portal de licencias<textarea v-model="settings.portalMessage" rows="2"></textarea></label>
            </div>
          </fieldset>

          <button class="button" :disabled="saving">
            {{ saving ? 'Guardando…' : 'Guardar configuración' }}
          </button>
        </form>
      </section>

      <!-- TAB 6: ROBUST API & INTEGRATION EXPLORER -->
      <section v-else class="space-y-6">
        <div class="panel api-explorer-header">
          <div>
            <span class="eyebrow">DESARROLLADORES & CLIENTES</span>
            <h2>Documentación de API & SDK TrastSaaS</h2>
            <p>EndPoints REST para activar licencias, validar dispositivos y sincronizar suscripciones.</p>
          </div>
          <div class="api-lang-picker">
            <button :class="{ active: codeLanguage === 'curl' }" @click="codeLanguage = 'curl'">cURL</button>
            <button :class="{ active: codeLanguage === 'js' }" @click="codeLanguage = 'js'">JavaScript</button>
            <button :class="{ active: codeLanguage === 'python' }" @click="codeLanguage = 'python'">Python</button>
            <button :class="{ active: codeLanguage === 'java' }" @click="codeLanguage = 'java'">Java</button>
          </div>
        </div>

        <!-- Endpoints Tab Navigation -->
        <div class="api-endpoints-nav">
          <button
            :class="{ active: selectedEndpoint === 'activate' }"
            @click="selectedEndpoint = 'activate'"
          >
            <span class="http-method post">POST</span>
            <code>/api/public/saas/licenses/activate</code>
          </button>
          <button
            :class="{ active: selectedEndpoint === 'validate' }"
            @click="selectedEndpoint = 'validate'"
          >
            <span class="http-method post">POST</span>
            <code>/api/public/saas/licenses/validate</code>
          </button>
          <button
            :class="{ active: selectedEndpoint === 'claim' }"
            @click="selectedEndpoint = 'claim'"
          >
            <span class="http-method get">GET</span>
            <code>/api/public/saas/orders/{order}/claim</code>
          </button>
          <button
            :class="{ active: selectedEndpoint === 'revoke' }"
            @click="selectedEndpoint = 'revoke'"
          >
            <span class="http-method post">POST</span>
            <code>/api/public/saas/licenses/revoke</code>
          </button>
        </div>

        <div class="grid gap-6 xl:grid-cols-2">
          <!-- Code Console Column -->
          <article class="panel api-code-panel">
            <div class="panel-heading">
              <div>
                <h2>Ejemplo de Código ({{ codeLanguage.toUpperCase() }})</h2>
                <p>Copia este fragmento dentro de tu aplicación de escritorio o web.</p>
              </div>
            </div>
            <pre class="code-block-fancy"><code>{{ sampleSnippets[codeLanguage] }}</code></pre>
          </article>

          <!-- Specifications Column -->
          <article class="panel api-spec-panel">
            <div class="panel-heading">
              <div>
                <h2>Parámetros de Solicitud (Body JSON)</h2>
                <p>Formato de datos requeridos por la API REST.</p>
              </div>
            </div>

            <div class="overflow-x-auto">
              <table class="data-table api-spec-table">
                <thead>
                  <tr>
                    <th>Campo</th>
                    <th>Tipo</th>
                    <th>Requerido</th>
                    <th>Descripción</th>
                  </tr>
                </thead>
                <tbody v-if="selectedEndpoint === 'activate'">
                  <tr>
                    <td><code>licenseKey</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Clave emitida (ej. TRAST-2026-PRO-9842)</td>
                  </tr>
                  <tr>
                    <td><code>productKey</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Clave técnica del producto (ej. trast-cloud)</td>
                  </tr>
                  <tr>
                    <td><code>fingerprint</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Hash único identificador del equipo o máquina</td>
                  </tr>
                  <tr>
                    <td><code>deviceName</code></td>
                    <td>string</td>
                    <td><span class="status-pill warning">Opcional</span></td>
                    <td>Nombre amigable del equipo</td>
                  </tr>
                </tbody>
                <tbody v-else-if="selectedEndpoint === 'validate'">
                  <tr>
                    <td><code>licenseKey</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Clave de licencia activa</td>
                  </tr>
                  <tr>
                    <td><code>fingerprint</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Hash de la máquina cliente</td>
                  </tr>
                </tbody>
                <tbody v-else-if="selectedEndpoint === 'claim'">
                  <tr>
                    <td><code>order</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Número de orden (TR-20260725-...)</td>
                  </tr>
                  <tr>
                    <td><code>token</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Token de seguridad de la orden</td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr>
                    <td><code>licenseKey</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Clave de licencia a liberar</td>
                  </tr>
                  <tr>
                    <td><code>fingerprint</code></td>
                    <td>string</td>
                    <td><span class="status-pill ok">Sí</span></td>
                    <td>Dispositivo a desvincular</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>
        </div>
      </section>
    </template>

    <!-- DRAWER: PRODUCT EDITOR -->
    <div v-if="productEditor" class="modal-backdrop">
      <form class="modal-card modal-card--scroll max-w-lg" @submit.prevent="saveProduct">
        <header>
          <div>
            <span class="eyebrow">PRODUCTO SAAS</span>
            <h2>{{ productDraft.id ? 'Editar' : 'Nuevo' }} producto</h2>
          </div>
          <button type="button" class="icon-button" @click="productEditor = false">×</button>
        </header>

        <div class="modal-card__body grid gap-4">
          <label>
            Clave técnica única
            <input
              v-model="productDraft.productKey"
              required
              pattern="[a-z0-9-]+"
              placeholder="trast-cloud"
            >
          </label>
          <label>
            Nombre comercial
            <input v-model="productDraft.name" required placeholder="TrastCMS Cloud Suite">
          </label>
          <label>
            Logotipo o imagen (URL)
            <input v-model="productDraft.logoUrl" type="url" placeholder="https://...">
          </label>
          <label>
            Descripción pública
            <textarea v-model="productDraft.description" rows="4"></textarea>
          </label>
          <label class="check-row">
            <input v-model="productDraft.active" type="checkbox">
            Producto activo y disponible para recibir planes
          </label>
        </div>

        <footer>
          <button type="button" class="button secondary" @click="productEditor = false">
            Cancelar
          </button>
          <button class="button" :disabled="saving">
            {{ saving ? 'Guardando…' : 'Guardar producto' }}
          </button>
        </footer>
      </form>
    </div>

    <!-- DRAWER: PLAN EDITOR -->
    <div v-if="planEditor" class="modal-backdrop">
      <form class="modal-card modal-card--scroll max-w-3xl" @submit.prevent="savePlan">
        <header>
          <div>
            <span class="eyebrow">ESQUEMA DE COBRO</span>
            <h2>{{ plan.id ? 'Editar' : 'Nuevo' }} plan SaaS</h2>
          </div>
          <button type="button" class="icon-button" @click="planEditor = false">×</button>
        </header>

        <div class="modal-card__body grid gap-4 md:grid-cols-2">
          <label>Producto SaaS
            <select v-model="plan.productId" required>
              <option v-for="item in products" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
          <label>Clave técnica del plan
            <input v-model="plan.planKey" required placeholder="pro-monthly">
          </label>
          <label>Nombre del plan
            <input v-model="plan.name" required placeholder="Plan Pro">
          </label>
          <label>Precio
            <input v-model.number="plan.price" type="number" min="0" step="0.01" required>
          </label>
          <label>Moneda
            <input v-model="plan.currency" maxlength="3" required>
          </label>
          <label>Frecuencia de cobro
            <select v-model="plan.billingInterval">
              <option value="MONTH">Mensual</option>
              <option value="YEAR">Anual</option>
              <option value="WEEK">Semanal</option>
              <option value="DAY">Diario</option>
              <option value="ONE_TIME">Pago único (Perpetuo)</option>
            </select>
          </label>
          <label>Intervalos de frecuencia
            <input v-model.number="plan.intervalCount" type="number" min="1">
          </label>
          <label>Días de prueba gratuitos
            <input v-model.number="plan.trialDays" type="number" min="0">
          </label>
          <label>Límite de activaciones (Equipos)
            <input v-model.number="plan.maxActivations" type="number" min="1">
          </label>

          <label class="md:col-span-2">Descripción pública
            <textarea v-model="plan.description" rows="2"></textarea>
          </label>
          <label class="md:col-span-2">Entitlements (Permisos en JSON)
            <textarea v-model="plan.entitlementsText" rows="6" spellcheck="false"></textarea>
          </label>

          <label class="check-row">
            <input v-model="plan.active" type="checkbox">Activo
          </label>
          <label class="check-row">
            <input v-model="plan.featured" type="checkbox">Destacado (Recomendado)
          </label>
        </div>

        <footer>
          <button type="button" class="button secondary" @click="planEditor = false">
            Cancelar
          </button>
          <button class="button" :disabled="saving">
            {{ saving ? 'Guardando…' : 'Guardar plan' }}
          </button>
        </footer>
      </form>
    </div>

    <!-- DRAWER: MANUAL LICENSE EMISSION -->
    <div v-if="licenseEditor" class="modal-backdrop">
      <form class="modal-card max-w-lg" @submit.prevent="saveLicense">
        <header>
          <div>
            <span class="eyebrow">EMISIÓN DE LICENCIA</span>
            <h2>Emitir licencia manual</h2>
          </div>
          <button type="button" class="icon-button" @click="licenseEditor = false">×</button>
        </header>

        <div class="grid gap-4">
          <label>Plan SaaS
            <select v-model="issue.planId" required>
              <option v-for="item in plans" :key="item.id" :value="item.id">
                {{ item.productName }} · {{ item.name }}
              </option>
            </select>
          </label>
          <label>Correo del cliente
            <input v-model="issue.customerEmail" type="email" required placeholder="cliente@empresa.com">
          </label>
          <label>Máximo de activaciones de equipos
            <input v-model.number="issue.maxActivations" type="number" min="1" placeholder="Usar valor predeterminado del plan">
          </label>
        </div>

        <footer>
          <button type="button" class="button secondary" @click="licenseEditor = false">
            Cancelar
          </button>
          <button class="button" :disabled="saving">
            {{ saving ? 'Emitiendo…' : 'Emitir licencia' }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>
