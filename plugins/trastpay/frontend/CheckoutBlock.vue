<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useCartStore } from '@/stores/cart'

const props = withDefaults(defineProps<{
  showCoupon?: boolean
  editing?: boolean
}>(), {
  showCoupon: true,
  editing: false
})

const cart = useCartStore()
const coupon = ref('')
const gateways = ref<Array<{ key: string; name: string }>>([])
const processing = ref(false)
const message = ref('')
const form = reactive({
  name: '',
  email: '',
  phone: '',
  gatewayKey: 'sandbox'
})

onMounted(async () => {
  if (props.editing) return
  await cart.ensure()
  gateways.value = await api<Array<{ key: string; name: string }>>('/api/public/commerce/gateways')
  if (gateways.value.length && !gateways.value.some(item => item.key === form.gatewayKey)) {
    form.gatewayKey = gateways.value[0].key
  }
})

async function checkout() {
  if (!cart.token || !cart.cart?.items.length) return
  processing.value = true
  message.value = ''
  try {
    const result = await api<{ orderNumber: string; message: string }>(
      `/api/public/commerce/carts/${cart.token}/checkout`,
      {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          couponCode: coupon.value || null,
          billing: {},
          shipping: {},
          returnUrl: `${location.origin}/cart`,
          cancelUrl: `${location.origin}/cart`,
          idempotencyKey: crypto.randomUUID()
        })
      }
    )
    message.value = `Orden ${result.orderNumber}: ${result.message}`
    cart.reset()
  } finally {
    processing.value = false
  }
}
</script>

<template>
  <section class="checkout-block">
    <div v-if="editing" class="plugin-preview-card">
      <span class="eyebrow">TRASTPAY</span>
      <h3>Checkout seguro</h3>
      <p>El formulario utilizará el carrito y las pasarelas activas al publicarse.</p>
    </div>
    <form v-else class="form-stack" @submit.prevent="checkout">
      <div>
        <span class="eyebrow">FINALIZAR COMPRA</span>
        <h3 class="text-2xl font-bold">Datos del cliente</h3>
      </div>
      <label>Nombre<input v-model="form.name" required></label>
      <label>Correo electrónico<input v-model="form.email" type="email" required></label>
      <label>Teléfono<input v-model="form.phone"></label>
      <label v-if="showCoupon">Cupón<input v-model="coupon"></label>
      <label>Método de pago
        <select v-model="form.gatewayKey">
          <option v-for="gateway in gateways" :key="gateway.key" :value="gateway.key">
            {{ gateway.name }}
          </option>
        </select>
      </label>
      <p v-if="message" class="form-success">{{ message }}</p>
      <button class="button full" :disabled="processing || !cart.cart?.items.length">
        {{ processing ? 'Procesando…' : 'Confirmar compra' }}
      </button>
    </form>
  </section>
</template>
