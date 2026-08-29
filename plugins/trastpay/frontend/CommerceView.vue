<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'
import UiIcon from '@/components/common/UiIcon.vue'

type Tab = 'dashboard' | 'orders' | 'coupons' | 'gateways' | 'settings'
type JsonRecord = Record<string, any>

type Order = {
  id: string
  orderNumber: string
  status: string
  currency: string
  customerEmail: string
  customerName: string
  subtotal: number
  discountTotal: number
  taxTotal: number
  total: number
  items: Array<{ name: string; quantity: number; total: number }>
  createdAt: string
}

type Coupon = {
  id: string
  code: string
  discountType: string
  discountValue: number
  minimumAmount?: number | null
  maximumUses?: number | null
  usedCount: number
  active: boolean
  demoData: boolean
}

const toast = useToastStore()
const tab = ref<Tab>('dashboard')
const data = ref<JsonRecord>({})
const orders = ref<Order[]>([])
const coupons = ref<Coupon[]>([])
const loading = ref(true)
const saving = ref(false)
const editor = ref(false)
const demoBusy = ref(false)
const orderSearch = ref('')
const orderStatus = ref('ALL')

const coupon = reactive<JsonRecord>({
  id: '', code: '', discountType: 'PERCENT', discountValue: 10,
  minimumAmount: 0, maximumUses: 100, active: true
})

const settings = reactive<JsonRecord>({
  defaultCurrency: 'USD', taxRate: 15, pricesIncludeTax: false,
  taxLabel: 'ISV', taxCountry: 'HN', cartExpirationDays: 14,
  guestCheckout: true, collectShipping: false, companyName: '',
  supportEmail: '', termsUrl: '', bankInstructions: '',
  bankTransferEnabled: true, cashEnabled: true, sandboxEnabled: true,
  paypalEnabled: false, paypalMode: 'SANDBOX', paypalClientId: '',
  paypalClientSecret: '', paypalConfigured: false, paypalWebhookId: '',
  paypalReceiverEmail: '',
  clinpaysEnabled: false, clinpaysEnvironment: 'STAGE', clinpaysAccessToken: '',
  clinpaysAccountId: '', clinpaysWebhookSecret: '', clinpaysPreviousWebhookSecret: '',
  clinpaysConfigured: false
})

const paypalWebhookUrl = computed(() =>
  typeof window === 'undefined'
    ? '/api/public/commerce/paypal/webhook'
    : `${window.location.origin}/api/public/commerce/paypal/webhook`
)

const clinpaysWebhookUrl = computed(() =>
  typeof window === 'undefined'
    ? '/api/public/commerce/clinpays/webhook'
    : `${window.location.origin}/api/public/commerce/clinpays/webhook`
)

const filteredOrders = computed(() => orders.value.filter(order => {
  const statusMatches = orderStatus.value === 'ALL' || order.status === orderStatus.value
  const term = orderSearch.value.trim().toLowerCase()
  const searchMatches = !term || `${order.orderNumber} ${order.customerName} ${order.customerEmail}`
    .toLowerCase().includes(term)
  return statusMatches && searchMatches
}))

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [dashboard, orderPage, loadedCoupons, configuration] = await Promise.all([
      api<JsonRecord>('/api/admin/commerce/dashboard'),
      api<{ content: Order[] }>('/api/admin/commerce/orders?size=100'),
      api<Coupon[]>('/api/admin/commerce/coupons'),
      api<JsonRecord>('/api/admin/commerce/settings')
    ])
    data.value = dashboard
    orders.value = orderPage.content
    coupons.value = loadedCoupons
    Object.assign(settings, configuration, {
      paypalClientSecret: '', clinpaysAccessToken: '',
      clinpaysWebhookSecret: '', clinpaysPreviousWebhookSecret: ''
    })
  } catch (caught: unknown) {
    toast.error('No se pudo cargar TrastPay', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    loading.value = false
  }
}

function editCoupon(value?: Coupon): void {
  Object.assign(coupon, value || {
    id: '', code: '', discountType: 'PERCENT', discountValue: 10,
    minimumAmount: 0, maximumUses: 100, active: true
  })
  editor.value = true
}

async function saveCoupon(): Promise<void> {
  saving.value = true
  try {
    const body = {
      ...coupon,
      minimumAmount: Number(coupon.minimumAmount) || null,
      maximumUses: Number(coupon.maximumUses) || null
    }
    await api(coupon.id
      ? `/api/admin/commerce/coupons/${coupon.id}`
      : '/api/admin/commerce/coupons', {
      method: coupon.id ? 'PUT' : 'POST',
      body: JSON.stringify(body)
    })
    editor.value = false
    toast.success('Cupón guardado', `${coupon.code} ya está disponible en el checkout.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo guardar el cupón', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function deleteCoupon(value: Coupon): Promise<void> {
  if (!window.confirm(`¿Eliminar el cupón ${value.code}?`)) return
  try {
    await api(`/api/admin/commerce/coupons/${value.id}`, { method: 'DELETE' })
    toast.success('Cupón eliminado')
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo eliminar', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function setStatus(order: Order, status: string): Promise<void> {
  try {
    await api(`/api/admin/commerce/orders/${order.id}/status`, {
      method: 'PUT', body: JSON.stringify({ status })
    })
    toast.success('Estado actualizado', `${order.orderNumber} ahora está ${status}.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo actualizar la orden', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function saveSettings(): Promise<void> {
  saving.value = true
  try {
    Object.assign(settings, await api<JsonRecord>('/api/admin/commerce/settings', {
      method: 'PUT', body: JSON.stringify(settings)
    }), {
      paypalClientSecret: '', clinpaysAccessToken: '',
      clinpaysWebhookSecret: '', clinpaysPreviousWebhookSecret: ''
    })
    toast.success('Configuración guardada', 'Los nuevos cálculos se aplicarán a los próximos carritos.')
  } catch (caught: unknown) {
    toast.error('No se pudo guardar', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function demo(action: 'generate' | 'delete'): Promise<void> {
  if (action === 'delete' && !window.confirm('¿Eliminar los cupones demo de TrastPay?')) return
  demoBusy.value = true
  try {
    const result = await api<JsonRecord>('/api/admin/commerce/demo-data', {
      method: action === 'generate' ? 'POST' : 'DELETE'
    })
    toast.success(action === 'generate' ? 'Datos demo generados' : 'Datos demo eliminados', result.message)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudieron procesar los datos demo', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    demoBusy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="commerce-admin space-y-6">
    <header class="plugin-hero plugin-hero--commerce">
      <div>
        <span class="eyebrow">TRASTPAY</span>
        <h1>Comercio, pagos e impuestos</h1>
        <p>Un núcleo reutilizable para Store, SaaS y cualquier plugin que necesite vender.</p>
      </div>
      <div class="plugin-hero__actions">
        <button class="button secondary" :disabled="demoBusy" @click="demo('generate')">
          <UiIcon name="sparkles" /> Generar demo
        </button>
        <button class="button ghost" :disabled="demoBusy || !data.demoCoupons" @click="demo('delete')">
          Eliminar demo
        </button>
        <button class="button" @click="tab = 'coupons'; editCoupon()">
          <UiIcon name="plus" /> Nuevo cupón
        </button>
      </div>
    </header>

    <nav class="plugin-tabs" aria-label="Secciones de TrastPay">
      <button v-for="item in [
        { id: 'dashboard', label: 'Resumen' }, { id: 'orders', label: 'Órdenes' },
        { id: 'coupons', label: 'Cupones' }, { id: 'gateways', label: 'Pasarelas' },
        { id: 'settings', label: 'Configuración' }
      ]" :key="item.id" :class="{ active: tab === item.id }" @click="tab = item.id as Tab">
        {{ item.label }}
      </button>
    </nav>

    <div v-if="loading" class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="item in 4" :key="item" class="metric-card"><SkeletonBlock :lines="3" height="1.2rem" /></article>
    </div>

    <template v-else>
      <section v-if="tab === 'dashboard'" class="space-y-6">
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <article class="metric-card"><span>Ingresos confirmados</span><strong>{{ money(data.revenue) }}</strong><small>{{ data.paidOrders }} órdenes pagadas</small></article>
          <article class="metric-card"><span>Órdenes</span><strong>{{ data.orders }}</strong><small>{{ data.pendingOrders }} pendientes</small></article>
          <article class="metric-card"><span>Pagos</span><strong>{{ data.payments }}</strong><small>Intentos registrados</small></article>
          <article class="metric-card"><span>Cupones</span><strong>{{ data.coupons }}</strong><small>{{ data.demoCoupons }} pertenecen al demo</small></article>
        </div>
        <div class="grid gap-6 xl:grid-cols-[1.4fr_.6fr]">
          <section class="panel">
            <div class="panel-heading"><div><span class="eyebrow">ACTIVIDAD</span><h2>Órdenes recientes</h2></div><button class="button ghost small" @click="tab = 'orders'">Ver todas</button></div>
            <div class="overflow-x-auto"><table class="data-table"><thead><tr><th>Orden</th><th>Cliente</th><th>Estado</th><th>Total</th></tr></thead><tbody><tr v-for="order in data.recentOrders" :key="order.id"><td><b>{{ order.orderNumber }}</b><small>{{ new Date(order.createdAt).toLocaleString() }}</small></td><td>{{ order.customerName }}<small>{{ order.customerEmail }}</small></td><td><span class="status-pill">{{ order.status }}</span></td><td>{{ money(order.total, order.currency) }}</td></tr><tr v-if="!data.recentOrders?.length"><td colspan="4" class="empty-cell">Todavía no hay órdenes. Pruebe el checkout público para crear la primera.</td></tr></tbody></table></div>
          </section>
          <aside class="panel commerce-health">
            <span class="eyebrow">CONFIGURACIÓN ACTIVA</span>
            <h2>{{ settings.taxLabel || 'Impuesto' }} {{ settings.taxRate }}%</h2>
            <p>{{ settings.pricesIncludeTax ? 'Los precios ya incluyen el impuesto.' : 'El impuesto se suma durante el checkout.' }}</p>
            <dl><div><dt>Moneda</dt><dd>{{ settings.defaultCurrency }}</dd></div><div><dt>País fiscal</dt><dd>{{ settings.taxCountry }}</dd></div><div><dt>Carrito</dt><dd>{{ settings.cartExpirationDays }} días</dd></div><div><dt>Pasarelas</dt><dd>{{ data.gateways?.length || 0 }}</dd></div></dl>
          </aside>
        </div>
      </section>

      <section v-else-if="tab === 'orders'" class="panel">
        <div class="panel-heading panel-heading--filters"><div><span class="eyebrow">OPERACIÓN</span><h2>Órdenes</h2></div><div class="filter-row"><input v-model="orderSearch" placeholder="Buscar por orden o cliente"><select v-model="orderStatus"><option value="ALL">Todos los estados</option><option v-for="status in ['PENDING_PAYMENT','PAID','ON_HOLD','COMPLETED','CANCELLED','REFUNDED']" :key="status">{{ status }}</option></select></div></div>
        <div class="overflow-x-auto"><table class="data-table"><thead><tr><th>Orden</th><th>Cliente</th><th>Productos</th><th>Impuesto</th><th>Total</th><th>Estado</th></tr></thead><tbody><tr v-for="order in filteredOrders" :key="order.id"><td><b>{{ order.orderNumber }}</b><small>{{ new Date(order.createdAt).toLocaleString() }}</small></td><td>{{ order.customerName }}<small>{{ order.customerEmail }}</small></td><td><span v-for="item in order.items" :key="item.name" class="block text-sm">{{ item.quantity }}× {{ item.name }}</span></td><td>{{ money(order.taxTotal, order.currency) }}</td><td><b>{{ money(order.total, order.currency) }}</b></td><td><select :value="order.status" @change="setStatus(order, ($event.target as HTMLSelectElement).value)"><option v-for="status in ['PENDING_PAYMENT','PAID','ON_HOLD','COMPLETED','CANCELLED','REFUNDED']" :key="status">{{ status }}</option></select></td></tr><tr v-if="!filteredOrders.length"><td colspan="6" class="empty-cell">No se encontraron órdenes con estos filtros.</td></tr></tbody></table></div>
      </section>

      <section v-else-if="tab === 'coupons'" class="space-y-5">
        <div class="section-heading-inline"><div><span class="eyebrow">PROMOCIONES</span><h2>Cupones</h2><p>Cree incentivos reutilizables por Store o SaaS.</p></div><button class="button" @click="editCoupon()"><UiIcon name="plus" /> Nuevo cupón</button></div>
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <article v-for="value in coupons" :key="value.id" class="commerce-coupon-card">
            <div class="flex justify-between gap-3"><div><span v-if="value.demoData" class="status-pill info">Demo</span><h3>{{ value.code }}</h3></div><span :class="['status-pill', value.active ? 'ok' : 'muted']">{{ value.active ? 'Activo' : 'Inactivo' }}</span></div>
            <strong>{{ value.discountType === 'PERCENT' ? `${value.discountValue}%` : money(value.discountValue) }}</strong>
            <p>Compra mínima: {{ value.minimumAmount ? money(value.minimumAmount) : 'Sin mínimo' }}</p>
            <small>{{ value.usedCount }} usos<span v-if="value.maximumUses"> de {{ value.maximumUses }}</span></small>
            <footer><button class="button secondary small" @click="editCoupon(value)">Editar</button><button class="button ghost small danger" @click="deleteCoupon(value)">Eliminar</button></footer>
          </article>
        </div>
      </section>

      <section v-else-if="tab === 'gateways'" class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <article v-for="gateway in data.gateways" :key="gateway.key" class="gateway-card">
          <div class="gateway-card__icon"><UiIcon name="credit-card" /></div>
          <span :class="['status-pill', gateway.enabled ? 'ok' : 'muted']">{{ gateway.enabled ? 'Disponible' : 'Desactivada' }}</span>
          <h2>{{ gateway.name }}</h2><p>{{ gateway.description }}</p>
          <div class="gateway-card__features"><span>{{ gateway.recurring ? 'Cobros recurrentes' : 'Pago único' }}</span><span>{{ gateway.refunds ? 'Reembolsos API' : 'Gestión manual de reembolso' }}</span></div>
        </article>
      </section>

      <section v-else class="panel max-w-6xl">
        <div class="panel-heading"><div><span class="eyebrow">CHECKOUT</span><h2>Configuración comercial</h2><p>Controle cuentas, pasarelas, impuestos y comportamiento del carrito.</p></div></div>
        <form class="settings-grid" @submit.prevent="saveSettings">
          <fieldset><legend>Fiscalidad</legend><div class="grid gap-4 md:grid-cols-2"><label>Moneda<input v-model="settings.defaultCurrency" maxlength="3" required></label><label>Tasa (%)<input v-model.number="settings.taxRate" type="number" min="0" max="100" step="0.01"></label><label>Nombre del impuesto<input v-model="settings.taxLabel" placeholder="ISV"></label><label>País fiscal<input v-model="settings.taxCountry" maxlength="2" placeholder="HN"></label><label class="check-row md:col-span-2"><input v-model="settings.pricesIncludeTax" type="checkbox">Los precios del catálogo ya incluyen impuestos</label></div></fieldset>
          <fieldset><legend>Cuenta y checkout</legend><div class="grid gap-4 md:grid-cols-2"><label>Vigencia del carrito<input v-model.number="settings.cartExpirationDays" type="number" min="1" max="90"></label><label class="check-row"><input v-model="settings.guestCheckout" type="checkbox">Permitir compra como invitado</label><label class="check-row"><input v-model="settings.collectShipping" type="checkbox">Solicitar dirección de envío</label><label>URL de términos<input v-model="settings.termsUrl" type="url"></label><p class="md:col-span-2 text-sm text-slate-500">Las licencias y suscripciones SaaS siempre requieren una cuenta, aunque la compra como invitado esté habilitada.</p></div></fieldset>
          <fieldset><legend>Métodos manuales</legend><div class="grid gap-4 md:grid-cols-2"><label class="check-row"><input v-model="settings.bankTransferEnabled" type="checkbox">Transferencia bancaria</label><label class="check-row"><input v-model="settings.cashEnabled" type="checkbox">Pago contra entrega</label><label class="check-row"><input v-model="settings.sandboxEnabled" type="checkbox">Pago de prueba</label><label class="md:col-span-2">Instrucciones de transferencia<textarea v-model="settings.bankInstructions" rows="5" placeholder="Banco, cuenta, titular y pasos para confirmar"></textarea></label></div></fieldset>
          <fieldset><legend>Negocio y soporte</legend><div class="grid gap-4 md:grid-cols-2"><label>Nombre comercial<input v-model="settings.companyName"></label><label>Correo de soporte<input v-model="settings.supportEmail" type="email"></label></div></fieldset>
          <fieldset><legend>PayPal</legend><div class="grid gap-4 md:grid-cols-2"><label class="check-row md:col-span-2"><input v-model="settings.paypalEnabled" type="checkbox">Activar PayPal en el checkout</label><label>Entorno<select v-model="settings.paypalMode"><option value="SANDBOX">Sandbox</option><option value="LIVE">Producción</option></select></label><label>Correo receptor<input v-model="settings.paypalReceiverEmail" type="email" placeholder="pagos@empresa.com"></label><label>Client ID<input v-model="settings.paypalClientId" autocomplete="off" placeholder="Client ID de la aplicación REST"></label><label>Client Secret<input v-model="settings.paypalClientSecret" type="password" autocomplete="new-password" :placeholder="settings.paypalConfigured ? 'Dejar vacío para conservar el secreto' : 'Client Secret requerido'"></label><label>Webhook ID<input v-model="settings.paypalWebhookId" autocomplete="off" placeholder="ID del webhook configurado en PayPal"></label><div class="paypal-config-state md:col-span-2"><span :class="['status-pill', settings.paypalConfigured ? 'ok' : 'muted']">{{ settings.paypalConfigured ? 'Credenciales guardadas' : 'Pendiente de configurar' }}</span><p>URL del webhook: <code>{{ paypalWebhookUrl }}</code></p></div></div></fieldset>
          <fieldset><legend>Clinpays / Promerica 3DS</legend><div class="grid gap-4 md:grid-cols-2"><label class="check-row md:col-span-2"><input v-model="settings.clinpaysEnabled" type="checkbox">Activar cobro con tarjeta y autenticación 3DS</label><label>Entorno<select v-model="settings.clinpaysEnvironment"><option value="STAGE">Pruebas</option><option value="PRODUCTION">Producción</option></select></label><label>ID de cuenta antifraude<input v-model="settings.clinpaysAccountId" autocomplete="off" placeholder="Account ID proporcionado por Clinpays"></label><label class="md:col-span-2">Bearer access token<input v-model="settings.clinpaysAccessToken" type="password" autocomplete="new-password" :placeholder="settings.clinpaysConfigured ? 'Dejar vacío para conservar el token' : 'Token requerido'"></label><label>Secreto actual del webhook<input v-model="settings.clinpaysWebhookSecret" type="password" autocomplete="new-password" :placeholder="settings.clinpaysConfigured ? 'Dejar vacío para conservarlo' : 'Secreto requerido'"></label><label>Secreto anterior (rotación)<input v-model="settings.clinpaysPreviousWebhookSecret" type="password" autocomplete="new-password" placeholder="Opcional durante 72 horas"></label><div class="paypal-config-state md:col-span-2"><span :class="['status-pill', settings.clinpaysConfigured ? 'ok' : 'muted']">{{ settings.clinpaysConfigured ? 'Token y firma configurados' : 'Pendiente de configurar' }}</span><p>Webhook: <code>{{ clinpaysWebhookUrl }}</code></p><p>El resultado final se confirma únicamente mediante el webhook firmado.</p></div></div></fieldset>
          <button class="button" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar configuración' }}</button>
        </form>
      </section>
    </template>

    <div v-if="editor" class="modal-backdrop">
      <form class="modal-card max-w-xl" @submit.prevent="saveCoupon">
        <header><div><span class="eyebrow">PROMOCIÓN</span><h2>{{ coupon.id ? 'Editar' : 'Nuevo' }} cupón</h2></div><button type="button" class="icon-button" @click="editor = false">×</button></header>
        <div class="grid gap-4 md:grid-cols-2"><label>Código<input v-model="coupon.code" required></label><label>Tipo<select v-model="coupon.discountType"><option value="PERCENT">Porcentaje</option><option value="FIXED">Monto fijo</option></select></label><label>Valor<input v-model.number="coupon.discountValue" type="number" min="0.01" step="0.01" required></label><label>Compra mínima<input v-model.number="coupon.minimumAmount" type="number" min="0" step="0.01"></label><label>Máximo de usos<input v-model.number="coupon.maximumUses" type="number" min="1"></label><label class="check-row"><input v-model="coupon.active" type="checkbox">Activo</label></div>
        <footer><button type="button" class="button secondary" @click="editor = false">Cancelar</button><button class="button" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar cupón' }}</button></footer>
      </form>
    </div>
  </div>
</template>
