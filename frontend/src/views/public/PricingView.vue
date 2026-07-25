<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type JsonRecord = Record<string, any>
type CatalogResponse = { settings: JsonRecord; products: JsonRecord[]; plans: JsonRecord[] }

const cart = useCartStore()
const toast = useToastStore()
const catalog = ref<CatalogResponse | null>(null)
const loading = ref(true)
const interval = ref<'MONTH' | 'YEAR' | 'ALL'>('MONTH')
const selectedProduct = ref('ALL')
const addingPlanId = ref('')

const products = computed(() => catalog.value?.products || [])
const plans = computed(() => (catalog.value?.plans || []).filter(plan => {
  const productMatches = selectedProduct.value === 'ALL' || plan.productKey === selectedProduct.value
  const intervalMatches = interval.value === 'ALL' || plan.billingInterval === interval.value || plan.billingInterval === 'ONE_TIME'
  return productMatches && intervalMatches
}))

// Generar dinámicamente las características y entitlements desde los datos del Backend
const comparisonFeatures = computed(() => {
  const keysMap = new Map<string, string>()

  const friendlyNames: Record<string, string> = {
    'users.max': 'Usuarios incluidos',
    'projects.max': 'Proyectos activos',
    'storage.gb': 'Almacenamiento en la nube (GB)',
    'devices.max': 'Dispositivos / Activaciones',
    'api.enabled': 'Acceso a la API REST',
    'reports.advanced': 'Reportes avanzados y analítica',
    'white_label.enabled': 'Marca blanca y personalización',
    'offline.enabled': 'Modo offline habilitado',
    'support.priority': 'Soporte técnico prioritario'
  }

  // Lista base de características
  const systemKeys = [
    'users.max',
    'projects.max',
    'storage.gb',
    'devices.max',
    'api.enabled',
    'reports.advanced',
    'white_label.enabled',
    'offline.enabled',
    'support.priority'
  ]

  systemKeys.forEach(k => keysMap.set(k, friendlyNames[k] || k))

  // Descubrir cualquier entitlement personalizado presente en los planes del backend
  plans.value.forEach(p => {
    if (p.entitlements && typeof p.entitlements === 'object') {
      Object.keys(p.entitlements).forEach(k => {
        if (!keysMap.has(k)) {
          const pretty = friendlyNames[k] || k.replace(/\./g, ' ').replace(/_/g, ' ')
          keysMap.set(k, pretty.charAt(0).toUpperCase() + pretty.slice(1))
        }
      })
    }
  })

  return Array.from(keysMap.entries()).map(([key, name]) => ({ key, name }))
})

function formatEntitlementValue(plan: JsonRecord, key: string): { label: string; isCheck: boolean; isCross: boolean } {
  let val: any = undefined
  if (key === 'devices.max') {
    val = plan.maxActivations
  } else if (plan.entitlements && key in plan.entitlements) {
    val = plan.entitlements[key]
  }

  if (val === true) return { label: '✓ Incluido', isCheck: true, isCross: false }
  if (val === false || val === 'No') return { label: '✗ No disponible', isCheck: false, isCross: true }
  if (val === undefined || val === null || val === '') return { label: '—', isCheck: false, isCross: false }
  if (typeof val === 'number') {
    if (key.includes('gb') || key.includes('storage')) return { label: `${val} GB`, isCheck: true, isCross: false }
    if (key.includes('users') || key.includes('user')) return { label: `${val} usuario(s)`, isCheck: true, isCross: false }
    if (key.includes('devices') || key.includes('maxActivations')) return { label: `${val} equipo(s)`, isCheck: true, isCross: false }
    return { label: String(val), isCheck: true, isCross: false }
  }
  return { label: String(val), isCheck: true, isCross: false }
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}
function intervalLabel(value: string): string {
  return ({ MONTH: 'mes', YEAR: 'año', WEEK: 'semana', DAY: 'día', ONE_TIME: 'pago único' } as Record<string, string>)[value] || value
}
function entitlementLabel(key: string, value: unknown): string {
  const labels: Record<string, string> = { 'users.max': 'Usuarios incluidos', 'projects.max': 'Proyectos', 'api.enabled': 'Acceso a la API', 'reports.advanced': 'Reportes avanzados', 'storage.gb': 'Almacenamiento (GB)', 'white_label.enabled': 'Marca blanca', 'devices.max': 'Dispositivos', 'offline.enabled': 'Modo offline', 'support.priority': 'Soporte prioritario' }
  return `${labels[key] || key}: ${value === true ? 'Sí' : value === false ? 'No' : value}`
}
function planMode(plan: JsonRecord): string {
  return plan.billingInterval === 'ONE_TIME' ? 'Licencia perpetua' : `Suscripción ${intervalLabel(plan.billingInterval)}`
}
async function select(plan: JsonRecord): Promise<void> {
  addingPlanId.value = plan.id
  try {
    await cart.add('saas', plan.id)
    toast.success(plan.billingInterval === 'ONE_TIME' ? 'Licencia agregada' : 'Suscripción agregada', `${plan.productName} · ${plan.name} está en el carrito.`)
    window.location.href = '/cart'
  } catch (caught: unknown) {
    toast.error('No se pudo agregar el plan', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    addingPlanId.value = ''
  }
}

onMounted(async () => {
  try {
    catalog.value = await api<CatalogResponse>('/api/public/saas/catalog')
  } catch (caught: unknown) {
    toast.error('No se pudieron cargar los planes', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="saas-pricing-page">
    <!-- Hero Banner -->
    <section class="pricing-hero pricing-hero--premium">
      <div class="pricing-hero__content">
        <span class="eyebrow">TRASTSAAS · PLANES Y SUSCRIPCIONES</span>
        <h1>{{ catalog?.settings.pricingHeadline || 'Venda software con un modelo recurrente profesional' }}</h1>
        <p>{{ catalog?.settings.pricingDescription || 'Planes, licencias, activaciones y uso medido listos para integrarse.' }}</p>
        <div class="pricing-trust-row">
          <span>✓ Checkout integrado</span>
          <span>✓ Licencias automáticas</span>
          <span>✓ Suscripción o pago único</span>
          <span>✓ Activaciones por equipo</span>
        </div>
      </div>
      <aside class="pricing-hero__console">
        <div class="console-dots"><i /><i /><i /></div>
        <code>POST /api/public/saas/licenses/activate</code>
        <strong>200 · LICENSE_ACTIVE</strong>
        <span>users.max: 20</span>
        <span>api.enabled: true</span>
        <span>expiresAt: 2027-07-22</span>
      </aside>
    </section>

    <!-- Controls Bar -->
    <section class="pricing-controls content-section">
      <div>
        <button :class="{ active: interval === 'MONTH' }" @click="interval = 'MONTH'">Mensual</button>
        <button :class="{ active: interval === 'YEAR' }" @click="interval = 'YEAR'">Anual</button>
        <button :class="{ active: interval === 'ALL' }" @click="interval = 'ALL'">Todos</button>
      </div>
      <select v-model="selectedProduct">
        <option value="ALL">Todos los productos</option>
        <option v-for="product in products" :key="product.id" :value="product.productKey">{{ product.name }}</option>
      </select>
    </section>

    <!-- Skeleton Loading -->
    <section v-if="loading" class="pricing-grid content-section">
      <article v-for="item in 3" :key="item" class="pricing-card">
        <SkeletonBlock :lines="7" height="1.2rem" />
      </article>
    </section>

    <!-- Pricing Cards Grid -->
    <section v-else class="pricing-grid content-section">
      <article
        v-for="plan in plans"
        :key="plan.id"
        :class="['pricing-card', { featured: plan.featured }]"
      >
        <span v-if="plan.featured" class="pricing-badge">Más popular</span>

        <div class="pricing-card__product">{{ plan.productName }}</div>
        <span class="pricing-card__mode">{{ planMode(plan) }}</span>

        <h2>{{ plan.name }}</h2>
        <p class="plan-card-desc">{{ plan.description }}</p>

        <div class="pricing-card__price-box">
          <strong>{{ money(plan.price, plan.currency) }}</strong>
          <small v-if="plan.billingInterval !== 'ONE_TIME'">/{{ intervalLabel(plan.billingInterval) }}</small>
        </div>

        <div class="trial-meta-chips">
          <span class="trial-chip">⏳ {{ plan.trialDays ? `${plan.trialDays} días de prueba` : 'Sin periodo de prueba' }}</span>
          <span class="trial-chip">💻 {{ plan.maxActivations }} activaciones</span>
        </div>

        <ul class="plan-entitlements-list">
          <li
            v-for="(value, key) in plan.entitlements"
            :key="key"
            :class="{ disabled: value === false || value === 'No' }"
          >
            <span class="check-icon">{{ (value === false || value === 'No') ? '✗' : '✓' }}</span>
            <span>{{ entitlementLabel(String(key), value) }}</span>
          </li>
        </ul>

        <div class="pricing-card__actions">
          <RouterLink :to="`/pricing/${plan.productKey}/${plan.planKey}`" class="button secondary full">
            Ver detalles
          </RouterLink>
          <button class="button full" :disabled="addingPlanId === plan.id" @click="select(plan)">
            {{ addingPlanId === plan.id ? 'Agregando…' : plan.billingInterval === 'ONE_TIME' ? 'Comprar licencia' : 'Suscribirme' }}
          </button>
        </div>
      </article>

      <div v-if="!plans.length" class="public-empty-state">
        <h2>No hay planes para este filtro</h2>
        <p>Cambie el periodo o seleccione otro producto.</p>
      </div>
    </section>

    <!-- SECTION: DETAILED DYNAMIC FEATURE COMPARISON TABLE (BACKEND CONNECTED) -->
    <section v-if="plans.length" class="content-section pricing-comparison-section">
      <div class="section-title text-center">
        <span class="eyebrow">MATRIZ COMPARATIVA DINÁMICA</span>
        <h2>Comparativa de características conectada al Backend</h2>
        <p>Esta tabla se genera automáticamente a partir de los permisos (entitlements) configurados en el sistema.</p>
      </div>

      <div class="comparison-table-wrapper overflow-x-auto">
        <table class="comparison-matrix-table">
          <thead>
            <tr>
              <th class="feature-col-header">Características & Entitlements</th>
              <th v-for="plan in plans" :key="plan.id" :class="{ highlight: plan.featured }">
                <span v-if="plan.featured" class="table-badge">Más popular</span>
                <strong>{{ plan.name }}</strong>
                <small>{{ money(plan.price, plan.currency) }}/{{ intervalLabel(plan.billingInterval) }}</small>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="feature in comparisonFeatures" :key="feature.key">
              <td class="feature-name">{{ feature.name }}</td>
              <td v-for="plan in plans" :key="plan.id">
                <span
                  :class="[
                    'matrix-icon',
                    formatEntitlementValue(plan, feature.key).isCheck ? 'yes' : '',
                    formatEntitlementValue(plan, feature.key).isCross ? 'no' : ''
                  ]"
                >
                  {{ formatEntitlementValue(plan, feature.key).label }}
                </span>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td class="feature-name"></td>
              <td v-for="plan in plans" :key="plan.id">
                <button class="button small full" :disabled="addingPlanId === plan.id" @click="select(plan)">
                  {{ addingPlanId === plan.id ? 'Agregando…' : 'Seleccionar plan' }}
                </button>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>

    <!-- Capabilities Footer -->
    <section class="saas-capabilities content-section">
      <div>
        <span class="eyebrow">MÁS QUE UNA TABLA DE PRECIOS</span>
        <h2>Todo el ciclo comercial del software</h2>
        <p>Desde la selección del plan hasta la activación dentro de la aplicación del cliente.</p>
      </div>
      <div class="saas-capability-grid">
        <article>
          <b>01</b>
          <h3>Compra</h3>
          <p>TrastPay procesa el carrito, impuestos, cupón y método de pago.</p>
        </article>
        <article>
          <b>02</b>
          <h3>Provisionamiento</h3>
          <p>TrastSaaS crea la suscripción y emite la licencia al confirmarse la orden.</p>
        </article>
        <article>
          <b>03</b>
          <h3>Activación</h3>
          <p>La aplicación cliente valida la clave, el dispositivo y los entitlements.</p>
        </article>
        <article>
          <b>04</b>
          <h3>Seguimiento</h3>
          <p>Controle uso, renovaciones, cambios de plan y licencias desde el panel.</p>
        </article>
      </div>
    </section>
  </main>
</template>
