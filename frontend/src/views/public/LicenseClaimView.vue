<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type JsonRecord = Record<string, any>
type ClaimResponse = { orderNumber: string; email: string; licenses: JsonRecord[]; subscriptions: JsonRecord[] }

const route = useRoute()
const toast = useToastStore()
const data = ref<ClaimResponse | null>(null)
const loading = ref(true)
const error = ref('')
const copiedLicenseId = ref<string | null>(null)
const selectedLicenseId = ref('')

const selectedLicense = computed(() => data.value?.licenses.find(item => item.id === selectedLicenseId.value) || data.value?.licenses[0] || null)
const relatedSubscription = computed(() => {
  const license = selectedLicense.value
  if (!license) return null
  return data.value?.subscriptions.find(item => item.productKey === license.productKey && item.planKey === license.planKey) || null
})

function queryValue(value: unknown): string {
  if (Array.isArray(value)) return value[0] == null ? '' : String(value[0])
  return value == null ? '' : String(value)
}

async function copyLicense(id: string, key: string): Promise<void> {
  try {
    await window.navigator.clipboard.writeText(key)
    copiedLicenseId.value = id
    toast.success('Licencia copiada', 'Guárdela en un lugar seguro.')
    window.setTimeout(() => { if (copiedLicenseId.value === id) copiedLicenseId.value = null }, 1800)
  } catch {
    toast.warning('Copie la clave manualmente', 'El navegador no permitió acceder al portapapeles.')
  }
}

function statusLabel(value: string): string {
  return ({ ACTIVE: 'Activa', SUSPENDED: 'Suspendida', REVOKED: 'Revocada', EXPIRED: 'Vencida', TRIALING: 'En prueba', PAST_DUE: 'Pago vencido', CANCELLED: 'Cancelada' } as Record<string, string>)[value] || value
}

onMounted(async () => {
  const order = queryValue(route.query.order)
  const token = queryValue(route.query.token)
  if (!order || !token) {
    error.value = 'El enlace para reclamar las licencias está incompleto o no es válido.'
    loading.value = false
    return
  }
  try {
    data.value = await api<ClaimResponse>(`/api/public/saas/orders/${encodeURIComponent(order)}/claim?token=${encodeURIComponent(token)}`)
    selectedLicenseId.value = data.value.licenses[0]?.id || ''
  } catch (caught: unknown) {
    error.value = caught instanceof ApiError ? caught.message : 'No se pudieron consultar las licencias de esta compra.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="license-portal content-section license-portal--clear">
    <!-- Hero Banner -->
    <section class="license-portal__hero">
      <div class="hero-text-content">
        <span class="eyebrow">PORTAL DEL CLIENTE · LICENCIAS Y ACTIVACIÓN</span>
        <h1>Su software está listo para activar</h1>
        <p v-if="data">Orden {{ data.orderNumber }} · {{ data.email }}</p>
        <p v-else>Consulte licencias, suscripciones y claves de activación de su compra.</p>
      </div>
      <div class="license-portal__shield">
        <div class="shield-badge">✓</div>
        <span>Compra confirmada</span>
      </div>
    </section>

    <!-- Skeleton Loading -->
    <div v-if="loading" class="license-portal__grid">
      <article class="panel"><SkeletonBlock :lines="10" /></article>
      <aside class="panel"><SkeletonBlock :lines="9" /></aside>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="public-error-state">
      <h2>No pudimos abrir esta compra</h2>
      <p>{{ error }}</p>
      <RouterLink to="/pricing" class="button">Ver planes de software</RouterLink>
    </div>

    <!-- Claim Content -->
    <template v-else-if="data">
      <!-- Clean 3-Step Explainer Grid -->
      <section class="license-order-explainer-grid">
        <article class="explainer-card">
          <div class="card-step-badge">01</div>
          <div class="card-text-block">
            <h3>Compra confirmada</h3>
            <p>TrastPay registró la orden y autorizó el provisionamiento automático.</p>
          </div>
        </article>

        <article class="explainer-card">
          <div class="card-step-badge">02</div>
          <div class="card-text-block">
            <h3>Licencia emitida</h3>
            <p>Cada producto o plan de software adquiere una clave única e independiente.</p>
          </div>
        </article>

        <article class="explainer-card">
          <div class="card-step-badge">03</div>
          <div class="card-text-block">
            <h3>Activación del producto</h3>
            <p>Ingrese la clave dentro de su aplicación para habilitar las funciones.</p>
          </div>
        </article>
      </section>

      <!-- Main Portal Grid -->
      <div class="license-portal__grid">
        <!-- License Selector Column -->
        <section class="license-selector-panel panel">
          <header class="panel-heading">
            <div>
              <span class="eyebrow">LICENCIAS DE ESTA ORDEN</span>
              <h2>{{ data.licenses.length }} licencia{{ data.licenses.length === 1 ? '' : 's' }} disponible{{ data.licenses.length === 1 ? '' : 's' }}</h2>
            </div>
          </header>

          <div class="license-selector-list">
            <button
              v-for="license in data.licenses"
              :key="license.id"
              :class="{ active: selectedLicense?.id === license.id }"
              @click="selectedLicenseId = license.id"
            >
              <div class="selector-item-title">
                <strong>{{ license.productName || license.productKey }}</strong>
                <span>{{ license.planName || license.planKey }}</span>
              </div>
              <div class="selector-item-meta">
                <span :class="['status-pill', license.status === 'ACTIVE' ? 'ok' : 'warning']">
                  {{ statusLabel(license.status) }}
                </span>
                <small>{{ license.activeActivations }}/{{ license.maxActivations }} equipos</small>
              </div>
            </button>

            <div v-if="!data.licenses.length" class="empty-cell">
              No se encontraron licencias activas para esta orden.
            </div>
          </div>

          <!-- Featured License Detail Card -->
          <article v-if="selectedLicense" class="license-card license-card--featured">
            <header class="license-card-header">
              <div>
                <span :class="['status-pill', selectedLicense.status === 'ACTIVE' ? 'ok' : 'warning']">
                  {{ statusLabel(selectedLicense.status) }}
                </span>
                <h2>{{ selectedLicense.productName || selectedLicense.productKey }}</h2>
                <p>{{ selectedLicense.planName || selectedLicense.planKey }}</p>
              </div>
              <span class="license-type-chip">
                {{ relatedSubscription ? 'Suscripción SaaS' : 'Licencia de software' }}
              </span>
            </header>

            <div class="license-key-box">
              <span class="key-box-label">Clave de licencia</span>
              <div class="key-code-wrap">
                <code>{{ selectedLicense.licenseKey }}</code>
                <button class="button secondary small" @click="copyLicense(selectedLicense.id, selectedLicense.licenseKey)">
                  {{ copiedLicenseId === selectedLicense.id ? '✓ Copiada' : '📋 Copiar clave' }}
                </button>
              </div>
            </div>

            <dl class="license-facts-grid">
              <div>
                <dt>Activaciones</dt>
                <dd>{{ selectedLicense.activeActivations }} de {{ selectedLicense.maxActivations }}</dd>
              </div>
              <div>
                <dt>Emitida</dt>
                <dd>{{ new Date(selectedLicense.issuedAt).toLocaleDateString() }}</dd>
              </div>
              <div>
                <dt>Vencimiento</dt>
                <dd>{{ selectedLicense.expiresAt ? new Date(selectedLicense.expiresAt).toLocaleDateString() : 'Sin vencimiento' }}</dd>
              </div>
            </dl>
          </article>
        </section>

        <!-- Sidebar Column -->
        <aside class="license-portal__sidebar">
          <article v-if="relatedSubscription" class="panel subscription-summary">
            <span class="eyebrow">DETALLES DE SUSCRIPCIÓN</span>
            <h2>{{ relatedSubscription.productName }} · {{ relatedSubscription.planName }}</h2>
            <dl class="subscription-facts-list">
              <div>
                <dt>Estado actual</dt>
                <dd><span class="status-pill ok">{{ statusLabel(relatedSubscription.status) }}</span></dd>
              </div>
              <div>
                <dt>Próxima renovación</dt>
                <dd>{{ relatedSubscription.currentPeriodEnd ? new Date(relatedSubscription.currentPeriodEnd).toLocaleDateString() : 'No aplica' }}</dd>
              </div>
              <div>
                <dt>Modalidad</dt>
                <dd>{{ relatedSubscription.cancelAtPeriodEnd ? 'Finaliza al terminar el periodo' : 'Renovación automática activa' }}</dd>
              </div>
            </dl>
          </article>

          <article class="panel activation-guide-card">
            <span class="eyebrow">INSTRUCCIONES</span>
            <h2>Cómo activar su software</h2>
            <ol class="activation-steps-list">
              <li>
                <span class="step-num">1</span>
                <span>Copie la clave de licencia generada arriba.</span>
              </li>
              <li>
                <span class="step-num">2</span>
                <span>Abra la aplicación o software adquirido.</span>
              </li>
              <li>
                <span class="step-num">3</span>
                <span>Ingrese la clave en la sección de activación y asigne un nombre a su equipo.</span>
              </li>
              <li>
                <span class="step-num">4</span>
                <span>La API validará la clave y habilitará todas las funciones de su plan.</span>
              </li>
            </ol>
            <RouterLink to="/developers#mcp" class="button secondary full">
              Ver guía de integración API →
            </RouterLink>
          </article>

          <RouterLink to="/pricing" class="button full">
            Administrar otras compras / Ver planes
          </RouterLink>
        </aside>
      </div>
    </template>
  </main>
</template>
