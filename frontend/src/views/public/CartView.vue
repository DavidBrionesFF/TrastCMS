<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, ApiError } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import type { CartLine } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type Gateway = {
  key: string
  name: string
  description?: string
  recurring?: boolean
  refunds?: boolean
}

type CheckoutResult = {
  orderNumber: string
  orderToken?: string
  orderStatus: string
  paymentStatus: string
  redirectUrl?: string | null
  message?: string
}

type CheckoutStep = 1 | 2 | 3

const cart = useCartStore()
const toast = useToastStore()
const route = useRoute()
const coupon = ref('')
const couponApplied = ref('')
const gateways = ref<Gateway[]>([])
const loading = ref(true)
const buying = ref(false)
const result = ref<CheckoutResult | null>(null)
const step = ref<CheckoutStep>(1)

const checkout = reactive({
  email: '',
  name: '',
  phone: '',
  gatewayKey: 'sandbox',
  notes: '',
  acceptTerms: false
})

const errors = reactive({
  email: '',
  name: '',
  phone: '',
  gatewayKey: '',
  acceptTerms: ''
})

const hasPhysicalItems = computed(() =>
  cart.cart?.items.some((item: CartLine) => item.metadata?.productType === 'PHYSICAL') || false
)

const hasSaasItems = computed(() =>
  cart.cart?.items.some((item: CartLine) => item.providerKey === 'saas') || false
)

const selectedGateway = computed(() =>
  gateways.value.find(item => item.key === checkout.gatewayKey)
)

function queryValue(value: unknown): string {
  if (Array.isArray(value)) return value[0] == null ? '' : String(value[0])
  return value == null ? '' : String(value)
}

function handlePayPalReturn(): void {
  const status = queryValue(route.query.paypal)
  if (!status) return
  const orderNumber = queryValue(route.query.order)
  const message = queryValue(route.query.message)
  cart.reset()

  if (status === 'success') {
    result.value = {
      orderNumber: orderNumber || 'PayPal',
      orderStatus: 'PAID',
      paymentStatus: 'SUCCEEDED',
      message: message || 'Pago PayPal confirmado correctamente'
    }
    toast.success('Pago PayPal confirmado', orderNumber ? `Orden ${orderNumber}` : message)
  } else if (status === 'cancelled') {
    toast.warning('Pago PayPal cancelado', 'La orden quedó pendiente y puede revisarse desde el panel.')
  } else {
    toast.error('No se pudo confirmar PayPal', message || 'Revise la orden o intente nuevamente.')
  }
}

async function applyCoupon(): Promise<void> {
  try {
    await cart.refresh(coupon.value)
    couponApplied.value = coupon.value.trim().toUpperCase()
    toast.success(
      couponApplied.value ? 'Cupón aplicado' : 'Cupón eliminado',
      couponApplied.value ? 'El descuento ya aparece en el resumen.' : undefined
    )
  } catch (caught: unknown) {
    toast.error('No se pudo aplicar el cupón', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function changeQuantity(itemId: string, value: number): Promise<void> {
  const safeValue = Math.max(0, Math.min(Number(value) || 0, 999))
  try {
    await cart.update(itemId, safeValue)
    if (safeValue === 0) toast.info('Producto eliminado del carrito')
  } catch (caught: unknown) {
    toast.error('No se pudo actualizar el carrito', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function stepQuantity(itemId: string, current: number, delta: number): Promise<void> {
  await changeQuantity(itemId, current + delta)
}

async function clearCart(): Promise<void> {
  try {
    await cart.clear()
    step.value = 1
    toast.info('Carrito vaciado')
  } catch (caught: unknown) {
    toast.error('No se pudo vaciar el carrito', caught instanceof ApiError ? caught.message : undefined)
  }
}

function goToCustomer(): void {
  if (cart.empty) {
    toast.warning('Agregue al menos un producto')
    return
  }
  step.value = 2
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function validateStep2(): boolean {
  let valid = true;
  errors.name = '';
  errors.email = '';
  errors.phone = '';
  errors.gatewayKey = '';

  if (!checkout.name.trim()) {
    errors.name = 'El nombre completo es requerido';
    valid = false;
  } else if (checkout.name.trim().length < 2) {
    errors.name = 'El nombre debe tener al menos 2 caracteres';
    valid = false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!checkout.email.trim()) {
    errors.email = 'El correo electrónico es requerido';
    valid = false;
  } else if (!emailRegex.test(checkout.email.trim())) {
    errors.email = 'Ingrese un correo electrónico válido (ej. cliente@empresa.com)';
    valid = false;
  }

  if (!checkout.gatewayKey) {
    errors.gatewayKey = 'Debe seleccionar un método de pago';
    valid = false;
  }

  return valid;
}

function goToReview(): void {
  if (!validateStep2()) {
    toast.warning('Complete los datos requeridos', 'Por favor verifique los campos señalados en rojo.')
    return
  }
  step.value = 3
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function buy(): Promise<void> {
  if (!checkout.acceptTerms) {
    errors.acceptTerms = 'Debe aceptar los términos de compra'
    toast.warning('Debe aceptar los términos de compra')
    return
  }
  errors.acceptTerms = ''

  if (!cart.token) {
    toast.error('Error de sesión del carrito', 'Intente recargar la página.')
    return
  }

  buying.value = true
  try {
    result.value = await api<CheckoutResult>(
      `/api/public/commerce/carts/${cart.token}/checkout`,
      {
        method: 'POST',
        body: JSON.stringify({
          email: checkout.email,
          name: checkout.name,
          phone: checkout.phone,
          gatewayKey: checkout.gatewayKey,
          notes: checkout.notes,
          couponCode: couponApplied.value || coupon.value,
          billing: {},
          shipping: { required: hasPhysicalItems.value },
          returnUrl: `${window.location.origin}/cart`,
          cancelUrl: `${window.location.origin}/cart`,
          idempotencyKey: window.crypto.randomUUID()
        })
      }
    )

    if (result.value.redirectUrl) {
      toast.info('Redirigiendo al proveedor de pago')
      window.location.assign(result.value.redirectUrl)
      return
    }

    cart.reset()
    toast.success('Orden creada', `La orden ${result.value.orderNumber} fue procesada.`)
  } catch (caught: unknown) {
    toast.error('No se pudo finalizar la compra', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    buying.value = false
  }
}

function itemKind(item: CartLine): string {
  if (item.providerKey !== 'saas') return item.type.replaceAll('_', ' ')
  const licenseType = String(item.metadata?.licenseType || '')
  return licenseType === 'PERPETUAL' ? 'LICENCIA PERPETUA' : 'SUSCRIPCIÓN DE SOFTWARE'
}

function itemRenewal(item: CartLine): string {
  const interval = String(item.metadata?.billingInterval || '')
  const count = Number(item.metadata?.intervalCount || 1)
  if (interval === 'ONE_TIME') return 'Pago único · sin renovación automática'
  const label = ({ MONTH: 'mes', YEAR: 'año', WEEK: 'semana', DAY: 'día' } as Record<string, string>)[interval] || interval.toLowerCase()
  return `Renovación cada ${count > 1 ? `${count} ` : ''}${label}${count > 1 ? 'es' : ''}`
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

onMounted(async () => {
  handlePayPalReturn()
  loading.value = true
  try {
    await cart.ensure()
    gateways.value = await api<Gateway[]>('/api/public/commerce/gateways')
    if (gateways.value.length && !gateways.value.some(item => item.key === checkout.gatewayKey)) {
      checkout.gatewayKey = gateways.value[0].key
    }
  } catch (caught: unknown) {
    toast.error('No se pudo preparar el checkout', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="cart-page cart-page--stepped">
    <section class="content-section">
      <header class="cart-page__header">
        <div>
          <span class="eyebrow">TRASTPAY CHECKOUT</span>
          <h1>Complete su compra en tres pasos</h1>
          <p>El carrito, sus datos y la confirmación están separados para que siempre sepa qué está comprando.</p>
        </div>
        <RouterLink to="/store" class="button secondary continue-shopping-btn">← Continuar comprando</RouterLink>
      </header>

      <!-- Clean Connected Stepper Bar -->
      <nav class="cart-stepper-container" aria-label="Proceso de compra">
        <ol class="cart-stepper">
          <li
            class="cart-stepper__step"
            :class="{ 'is-active': step === 1, 'is-complete': step > 1 }"
            @click="step = 1"
          >
            <div class="cart-stepper__badge">
              <svg v-if="step > 1" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span v-else>1</span>
            </div>
            <div class="cart-stepper__label">
              <b>Carrito</b>
              <small>Productos y cantidades</small>
            </div>
          </li>

          <div class="cart-stepper__line" :class="{ 'is-complete': step > 1 }"></div>

          <li
            class="cart-stepper__step"
            :class="{ 'is-active': step === 2, 'is-complete': step > 2 }"
            @click="!cart.empty && (step = 2)"
          >
            <div class="cart-stepper__badge">
              <svg v-if="step > 2" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              <span v-else>2</span>
            </div>
            <div class="cart-stepper__label">
              <b>Datos</b>
              <small>Cliente y pago</small>
            </div>
          </li>

          <div class="cart-stepper__line" :class="{ 'is-complete': step > 2 }"></div>

          <li
            class="cart-stepper__step"
            :class="{ 'is-active': step === 3 }"
            @click="step === 3"
          >
            <div class="cart-stepper__badge">
              <span>3</span>
            </div>
            <div class="cart-stepper__label">
              <b>Confirmación</b>
              <small>Revise y finalice</small>
            </div>
          </li>
        </ol>
      </nav>

      <div v-if="result" class="cart-success cart-success--complete">
        <span class="checkout-success__icon">✓</span>
        <span class="status-pill ok">{{ result.paymentStatus }}</span>
        <h2>Orden {{ result.orderNumber }}</h2>
        <p>{{ result.message || 'La orden fue creada correctamente.' }}</p>
        <div v-if="hasSaasItems || result.orderToken" class="order-license-notice">
          <b>Esta compra incluye software</b>
          <span>La licencia o suscripción se emite después de confirmar el pago.</span>
        </div>
        <div class="checkout-success__actions">
          <RouterLink
            v-if="result.orderToken"
            :to="`/saas/claim?order=${result.orderNumber}&token=${result.orderToken}`"
            class="button"
          >
            Abrir portal de licencias
          </RouterLink>
          <RouterLink to="/store" class="button secondary">Volver a la tienda</RouterLink>
        </div>
      </div>

      <div v-else-if="loading" class="cart-layout">
        <section class="panel"><SkeletonBlock :lines="10" height="1.1rem" /></section>
        <aside class="panel"><SkeletonBlock :lines="10" height="1rem" /></aside>
      </div>

      <div v-else class="checkout-step-layout">
        <section class="checkout-step-panel panel">
          <!-- STEP 1: CART REVISION -->
          <template v-if="step === 1">
            <header class="checkout-step-panel__header">
              <div>
                <span class="eyebrow">PASO 1</span>
                <h2>Revise sus artículos</h2>
                <p>Modifique cantidades o retire productos antes de continuar.</p>
              </div>
              <button v-if="!cart.empty" class="text-link danger" type="button" @click="clearCart">Vaciar carrito</button>
            </header>

            <div v-if="cart.empty" class="public-empty-state">
              <span class="empty-state-icon">🛒</span>
              <h2>Su carrito está vacío</h2>
              <p>Agregue productos de la tienda o seleccione un plan de software.</p>
              <div class="cart-empty-actions">
                <RouterLink to="/store" class="button">Explorar tienda</RouterLink>
                <RouterLink to="/pricing" class="button secondary">Ver planes SaaS</RouterLink>
              </div>
            </div>

            <div v-else class="checkout-line-list">
              <article v-for="item in cart.cart?.items" :key="item.id" class="checkout-line-card">
                <div class="checkout-line-card__image" :style="item.imageUrl ? { backgroundImage: `url(${item.imageUrl})` } : {}">
                  <span>{{ item.providerKey === 'saas' ? 'SaaS' : 'Store' }}</span>
                </div>
                <div class="checkout-line-card__content">
                  <small class="kind-tag">{{ itemKind(item) }}</small>
                  <h3>{{ item.name }}</h3>
                  <p class="renewal-note">{{ item.providerKey === 'saas' ? itemRenewal(item) : `${money(item.unitPrice, item.currency)} por unidad` }}</p>
                  <span class="tax-badge">{{ item.taxable ? `Impuesto ${item.taxRate || 0}%` : 'Exento de impuesto' }}</span>
                </div>
                <div class="checkout-line-card__quantity">
                  <div class="quantity-control quantity-control--cart">
                    <button type="button" aria-label="Restar una unidad" @click="stepQuantity(item.id, item.quantity, -1)">−</button>
                    <input :value="item.quantity" type="number" min="0" max="999" aria-label="Cantidad" @change="changeQuantity(item.id, Number(($event.target as HTMLInputElement).value))">
                    <button type="button" aria-label="Sumar una unidad" @click="stepQuantity(item.id, item.quantity, 1)">+</button>
                  </div>
                  <strong class="line-total-price">{{ money(item.lineTotal, item.currency) }}</strong>
                  <button type="button" class="text-link danger delete-item-btn" @click="changeQuantity(item.id, 0)">Eliminar</button>
                </div>
              </article>
            </div>

            <footer class="checkout-step-actions">
              <div class="checkout-summary-pill">
                <strong>{{ cart.count }} {{ cart.count === 1 ? 'unidad' : 'unidades' }}</strong>
                <span>Total: {{ money(cart.cart?.total || 0, cart.cart?.currency) }}</span>
              </div>
              <button class="button checkout-action-btn" :disabled="cart.empty" @click="goToCustomer">
                Continuar con mis datos →
              </button>
            </footer>
          </template>

          <!-- STEP 2: CUSTOMER & PAYMENT DATA -->
          <template v-else-if="step === 2">
            <header class="checkout-step-panel__header">
              <div>
                <span class="eyebrow">PASO 2</span>
                <h2>Datos del comprador y pago</h2>
                <p>Usaremos esta información para la orden, entrega y licencias.</p>
              </div>
            </header>

            <div class="checkout-customer-grid">
              <div class="checkout-customer-fields">
                <label :class="{ 'has-error': errors.name }">
                  <span>Nombre completo <em class="required-star">*</em></span>
                  <input v-model="checkout.name" required autocomplete="name" placeholder="Ej. Juan Pérez" @input="errors.name = ''">
                  <span v-if="errors.name" class="field-error-msg">{{ errors.name }}</span>
                </label>

                <label :class="{ 'has-error': errors.email }">
                  <span>Correo electrónico <em class="required-star">*</em></span>
                  <input v-model="checkout.email" type="email" required autocomplete="email" placeholder="correo@empresa.com" @input="errors.email = ''">
                  <span v-if="errors.email" class="field-error-msg">{{ errors.email }}</span>
                </label>

                <label :class="{ 'has-error': errors.phone }">
                  <span>Teléfono (opcional)</span>
                  <input v-model="checkout.phone" autocomplete="tel" placeholder="+504 9999-9999" @input="errors.phone = ''">
                  <span v-if="errors.phone" class="field-error-msg">{{ errors.phone }}</span>
                </label>

                <label>
                  <span>Notas de la orden</span>
                  <textarea v-model="checkout.notes" rows="3" placeholder="Información adicional para procesar su compra" />
                </label>
              </div>

              <div class="checkout-gateways checkout-gateways--cards">
                <span class="gateways-title">Método de pago <em class="required-star">*</em></span>
                <label v-for="gateway in gateways" :key="gateway.key" :class="['checkout-gateway', { active: checkout.gatewayKey === gateway.key }]">
                  <input v-model="checkout.gatewayKey" type="radio" :value="gateway.key" @change="errors.gatewayKey = ''">
                  <div class="gateway-info">
                    <b>{{ gateway.name }}</b>
                    <small v-if="gateway.description">{{ gateway.description }}</small>
                    <em v-if="gateway.recurring" class="recurring-tag">Compatible con renovaciones</em>
                  </div>
                </label>
                <span v-if="errors.gatewayKey" class="field-error-msg">{{ errors.gatewayKey }}</span>
              </div>
            </div>

            <footer class="checkout-step-actions">
              <button class="button secondary nav-back-btn" type="button" @click="step = 1">
                ← Volver al carrito
              </button>
              <button class="button checkout-action-btn" type="button" @click="goToReview">
                Revisar mi orden →
              </button>
            </footer>
          </template>

          <!-- STEP 3: ORDER CONFIRMATION & PAYMENT -->
          <template v-else>
            <header class="checkout-step-panel__header">
              <div>
                <span class="eyebrow">PASO 3</span>
                <h2>Confirme su orden</h2>
                <p>Revise los datos finales antes de procesar el pago.</p>
              </div>
            </header>

            <div class="checkout-review-grid">
              <article class="review-card">
                <div class="review-card__header">
                  <span class="review-card__label">Comprador</span>
                  <button type="button" class="text-link" @click="step = 2">Editar</button>
                </div>
                <b class="review-card__title">{{ checkout.name }}</b>
                <span class="review-card__sub">{{ checkout.email }}</span>
                <small class="review-card__detail">{{ checkout.phone || 'Sin teléfono' }}</small>
              </article>

              <article class="review-card">
                <div class="review-card__header">
                  <span class="review-card__label">Método de pago</span>
                  <button type="button" class="text-link" @click="step = 2">Cambiar</button>
                </div>
                <b class="review-card__title">{{ selectedGateway?.name || checkout.gatewayKey }}</b>
                <small class="review-card__detail">{{ selectedGateway?.description || 'Método de pago seleccionado' }}</small>
              </article>

              <article class="review-card full-width">
                <div class="review-card__header">
                  <span class="review-card__label">Contenido de la orden</span>
                  <button type="button" class="text-link" @click="step = 1">Revisar</button>
                </div>
                <b class="review-card__title">{{ cart.count }} {{ cart.count === 1 ? 'unidad' : 'unidades' }} seleccionadas</b>
                <small class="review-card__detail">
                  {{ hasSaasItems ? 'Incluye licencia o suscripción de software.' : 'Productos de tienda.' }}
                </small>
              </article>
            </div>

            <label :class="['check-row', 'checkout-terms', { 'has-error': errors.acceptTerms }]">
              <input v-model="checkout.acceptTerms" type="checkbox" @change="errors.acceptTerms = ''">
              <span>Acepto los términos de compra y el tratamiento de los datos necesarios para procesar la orden.</span>
            </label>
            <span v-if="errors.acceptTerms" class="field-error-msg inline-terms-error">{{ errors.acceptTerms }}</span>

            <div class="checkout-final-callout">
              <span class="lock-icon">🔒</span>
              <div class="callout-content">
                <b>Checkout seguro</b>
                <p>TrastCMS no almacena datos sensibles de tarjetas. El proveedor procesa el pago de forma encriptada.</p>
              </div>
            </div>

            <footer class="checkout-step-actions">
              <button class="button secondary nav-back-btn" type="button" @click="step = 2">
                ← Volver
              </button>
              <button
                class="button checkout-button"
                :disabled="cart.empty || buying || !checkout.acceptTerms"
                @click="buy"
              >
                {{ buying ? 'Procesando orden…' : `Confirmar y pagar ${money(cart.cart?.total || 0, cart.cart?.currency)}` }}
              </button>
            </footer>
          </template>
        </section>

        <!-- SUMMARY SIDEBAR -->
        <aside class="cart-summary cart-summary--step panel">
          <span class="eyebrow">RESUMEN</span>
          <h2>Su orden</h2>
          <div class="cart-summary__mini-lines">
            <div v-for="item in cart.cart?.items" :key="item.id" class="summary-line-item">
              <span class="item-name">{{ item.quantity }} × {{ item.name }}</span>
              <b class="item-price">{{ money(item.lineTotal, item.currency) }}</b>
            </div>
          </div>

          <dl class="summary-totals">
            <div>
              <dt>Subtotal</dt>
              <dd>{{ money(cart.cart?.subtotal || 0, cart.cart?.currency) }}</dd>
            </div>
            <div v-if="cart.cart?.discountTotal" class="discount">
              <dt>Descuento</dt>
              <dd>-{{ money(cart.cart?.discountTotal || 0, cart.cart?.currency) }}</dd>
            </div>
            <div>
              <dt>Impuestos</dt>
              <dd>{{ money(cart.cart?.taxTotal || 0, cart.cart?.currency) }}</dd>
            </div>
            <div class="total">
              <dt>Total</dt>
              <dd>{{ money(cart.cart?.total || 0, cart.cart?.currency) }}</dd>
            </div>
          </dl>

          <div v-if="step === 1" class="coupon-box coupon-box--stacked">
            <label>
              <span>Cupón promocional</span>
              <input v-model="coupon" placeholder="Ej. DEMO10">
            </label>
            <button class="button secondary" type="button" @click="applyCoupon">Aplicar cupón</button>
          </div>

          <div v-if="hasSaasItems" class="cart-software-note">
            <b>Compra de software</b>
            <p>Después del pago recibirá acceso al portal de licencias para la activación.</p>
          </div>
        </aside>
      </div>
    </section>
  </main>
</template>
