<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { api } from '@/services/api'
import { useCartStore } from '@/stores/cart'

const props = withDefaults(defineProps<{
  productSlug?: string
  editing?: boolean
}>(), {
  productSlug: '',
  editing: false
})

const product = ref<any>(null)
const cart = useCartStore()
const money = (value: number, currency = 'USD') => new Intl.NumberFormat(
  'es-HN',
  { style: 'currency', currency }
).format(value || 0)

async function load() {
  if (!props.productSlug) return
  try {
    product.value = await api(`/api/public/store/products/${encodeURIComponent(props.productSlug)}`)
  } catch {
    product.value = null
  }
}

watch(() => props.productSlug, load)
onMounted(load)
</script>

<template>
  <article v-if="product" class="product-feature-block">
    <div class="product-feature-block__media" :style="product.featuredImageUrl ? { backgroundImage: `url(${product.featuredImageUrl})` } : {}">
      <span v-if="product.featured" class="status-pill ok">Destacado</span>
    </div>
    <div class="product-feature-block__content">
      <span class="eyebrow">{{ product.category || product.type }}</span>
      <h3>{{ product.name }}</h3>
      <p>{{ product.shortDescription || product.description }}</p>
      <div class="product-price-row">
        <strong>{{ money(product.price, product.currency) }}</strong>
        <del v-if="product.compareAtPrice">{{ money(product.compareAtPrice, product.currency) }}</del>
      </div>
      <button class="button" :disabled="editing || !product.inStock" @click="cart.add('store', product.id)">
        {{ product.inStock ? 'Agregar al carrito' : 'Agotado' }}
      </button>
    </div>
  </article>
  <div v-else class="plugin-preview-card">
    <span class="eyebrow">TRASTSTORE</span>
    <h3>Seleccione un producto</h3>
    <p>El bloque mostrará su ficha, precio y botón para agregarlo al carrito.</p>
  </div>
</template>
