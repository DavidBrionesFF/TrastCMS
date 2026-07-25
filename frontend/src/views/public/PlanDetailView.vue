<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type JsonRecord = Record<string, any>
type CatalogResponse = { settings: JsonRecord; products: JsonRecord[]; plans: JsonRecord[] }

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()
const catalog = ref<CatalogResponse | null>(null)
const loading = ref(true)
const adding = ref(false)

const plan = computed(() => catalog.value?.plans.find(item =>
  item.productKey === String(route.params.productKey)
  && item.planKey === String(route.params.planKey)
) || null)

const product = computed(() => catalog.value?.products.find(item =>
  item.productKey === String(route.params.productKey)
) || null)

const recurring = computed(() => plan.value?.billingInterval !== 'ONE_TIME')

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

function intervalLabel(value: string): string {
  return ({ MONTH: 'mes', YEAR: 'año', WEEK: 'semana', DAY: 'día', ONE_TIME: 'pago único' } as Record<string, string>)[value] || value
}

function entitlementLabel(key: string, value: unknown): string {
  const labels: Record<string, string> = {
    'users.max': 'Usuarios incluidos',
    'projects.max': 'Proyectos',
    'api.enabled': 'Acceso a la API',
    'reports.advanced': 'Reportes avanzados',
    'storage.gb': 'Almacenamiento (GB)',
    'white_label.enabled': 'Marca blanca',
    'devices.max': 'Dispositivos',
    'offline.enabled': 'Modo offline',
    'support.priority': 'Soporte prioritario'
  }
  return `${labels[key] || key}: ${value === true ? 'Sí' : value === false ? 'No' : value}`
}

async function add(): Promise<void> {
  if (!plan.value) return
  adding.value = true
  try {
    await cart.add('saas', plan.value.id)
    toast.success(
      recurring.value ? 'Suscripción agregada' : 'Licencia agregada',
      `${plan.value.productName} · ${plan.value.name} está en el carrito.`
    )
    await router.push('/cart')
  } catch (caught: unknown) {
    toast.error('No se pudo agregar el plan', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    adding.value = false
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    catalog.value = await api<CatalogResponse>('/api/public/saas/catalog')
  } catch (caught: unknown) {
    toast.error('No se pudo cargar el plan', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    loading.value = false
  }
}

watch(() => route.fullPath, load)
onMounted(load)
</script>

<template>
  <main class="plan-detail-page">
    <section v-if="loading" class="content-section plan-detail-layout"><article class="panel"><SkeletonBlock :lines="12" /></article><aside class="panel"><SkeletonBlock :lines="10" /></aside></section>
    <section v-else-if="plan" class="content-section plan-detail-layout">
      <article class="plan-detail-main">
        <RouterLink to="/pricing" class="text-link">← Volver a precios</RouterLink>
        <div class="plan-detail-tags"><span>{{ product?.name || plan.productName }}</span><span>{{ recurring ? 'Suscripción' : 'Licencia perpetua' }}</span><span>{{ plan.maxActivations }} activaciones</span></div>
        <h1>{{ plan.name }}</h1>
        <p>{{ plan.description }}</p>
        <div class="plan-detail-price"><strong>{{ money(plan.price, plan.currency) }}</strong><span>{{ recurring ? `cada ${intervalLabel(plan.billingInterval)}` : 'pago único' }}</span></div>
        <div class="plan-detail-explanation">
          <article><b>{{ recurring ? 'Qué está comprando' : 'Licencia de uso' }}</b><p>{{ recurring ? 'Acceso recurrente al software, actualizaciones y funciones del plan durante el periodo contratado.' : 'Una licencia sin renovación automática, limitada por las condiciones y activaciones del plan.' }}</p></article>
          <article><b>Después del pago</b><p>TrastSaaS crea la suscripción cuando aplica, emite la licencia y abre el portal de activación.</p></article>
          <article><b>Prueba y renovación</b><p>{{ plan.trialDays ? `${plan.trialDays} días de prueba antes del primer periodo.` : 'Este plan no incluye periodo de prueba.' }} {{ recurring ? 'La renovación depende del método de pago configurado.' : 'No existe renovación automática.' }}</p></article>
        </div>
        <section class="plan-detail-features"><span class="eyebrow">FUNCIONES INCLUIDAS</span><div><article v-for="(value, key) in plan.entitlements" :key="key">✓ {{ entitlementLabel(String(key), value) }}</article></div></section>
      </article>
      <aside class="plan-checkout-card">
        <span class="eyebrow">RESUMEN DEL PLAN</span><h2>{{ plan.productName }} · {{ plan.name }}</h2>
        <dl><div><dt>Modalidad</dt><dd>{{ recurring ? 'Suscripción' : 'Licencia perpetua' }}</dd></div><div><dt>Cobro</dt><dd>{{ recurring ? intervalLabel(plan.billingInterval) : 'Una sola vez' }}</dd></div><div><dt>Activaciones</dt><dd>{{ plan.maxActivations }}</dd></div><div><dt>Prueba</dt><dd>{{ plan.trialDays ? `${plan.trialDays} días` : 'No incluida' }}</dd></div></dl>
        <strong>{{ money(plan.price, plan.currency) }}</strong>
        <button class="button full" :disabled="adding" @click="add">{{ adding ? 'Agregando…' : recurring ? 'Continuar con esta suscripción' : 'Comprar esta licencia' }}</button>
        <small>El precio final, impuestos y método de pago se confirman en TrastPay.</small>
      </aside>
    </section>
    <section v-else class="content-section public-error-state"><h2>Plan no disponible</h2><p>El plan fue desactivado o el enlace no corresponde a un producto activo.</p><RouterLink to="/pricing" class="button">Ver todos los planes</RouterLink></section>
  </main>
</template>
