<script setup lang="ts">
import { onMounted } from 'vue'
import { useCartStore } from '@/stores/cart'

const props = withDefaults(defineProps<{
  style?: string
  editing?: boolean
}>(), {
  style: 'card',
  editing: false
})

const cart = useCartStore()
const money = (value: number, currency = 'USD') => new Intl.NumberFormat(
  'es-HN',
  { style: 'currency', currency }
).format(value || 0)

onMounted(async () => {
  if (!props.editing && cart.token) await cart.refresh()
})
</script>

<template>
  <section :class="['commerce-cart-block', `is-${style}`]">
    <div v-if="editing" class="plugin-preview-card">
      <span class="eyebrow">TRASTPAY</span>
      <h3>Carrito interactivo</h3>
      <p>Durante la navegación mostrará las líneas, cantidades y total del carrito activo.</p>
    </div>
    <template v-else>
      <header>
        <div>
          <span class="eyebrow">SU COMPRA</span>
          <h3>Carrito</h3>
        </div>
        <span class="status-pill">{{ cart.count }} artículos</span>
      </header>
      <div v-if="cart.cart?.items.length" class="commerce-cart-block__items">
        <article v-for="item in cart.cart.items" :key="item.id">
          <div>
            <strong>{{ item.name }}</strong>
            <small>{{ item.quantity }} × {{ money(item.unitPrice, item.currency) }}</small>
          </div>
          <b>{{ money(item.lineTotal, item.currency) }}</b>
        </article>
      </div>
      <div v-else class="empty compact">
        <p>No hay productos en el carrito.</p>
      </div>
      <footer>
        <span>Total</span>
        <strong>{{ money(cart.cart?.total || 0, cart.cart?.currency) }}</strong>
      </footer>
      <RouterLink to="/cart" class="button full">Revisar y pagar</RouterLink>
    </template>
  </section>
</template>
