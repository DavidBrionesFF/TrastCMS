<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, ApiError } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type Product = Record<string, any>
type DetailTab = 'description' | 'specifications' | 'delivery'

const route = useRoute()
const cart = useCartStore()
const toast = useToastStore()
const product = ref<Product | null>(null)
const loading = ref(true)
const quantity = ref(1)
const adding = ref(false)
const selectedVariant = ref('')
const activeImage = ref('')
const detailTab = ref<DetailTab>('description')

const selected = computed(() =>
  product.value?.variants?.find(
    (item: any) => item.id === selectedVariant.value
  )
)

const displayPrice = computed(() =>
  selected.value?.price ?? product.value?.price ?? 0
)

const images = computed<string[]>(() => {
  if (!product.value) return []
  const values = [
    product.value.featuredImageUrl,
    ...(product.value.gallery || []),
    selected.value?.imageUrl
  ].filter(Boolean)
  return Array.from(new Set(values.map(String)))
})

const availableStock = computed(() => {
  if (selected.value) return selected.value.stockQuantity ?? 0
  return product.value?.trackInventory
    ? product.value.stockQuantity ?? 0
    : 999
})

const availabilityLabel = computed(() => {
  if (!product.value?.trackInventory) return 'Disponibilidad inmediata'
  if (availableStock.value <= 0 && product.value.allowBackorder) {
    return 'Disponible bajo pedido'
  }
  if (availableStock.value <= 0) return 'Producto agotado'
  if (availableStock.value <= product.value.lowStockThreshold) {
    return `Últimas ${availableStock.value} unidades`
  }
  return `${availableStock.value} unidades disponibles`
})

async function load(): Promise<void> {
  loading.value = true
  try {
    product.value = await api<Product>(
      `/api/public/store/products/${encodeURIComponent(
        String(route.params.slug)
      )}`
    )
    selectedVariant.value =
      product.value.variants?.find((item: any) => item.active)?.id || ''
    activeImage.value = String(product.value.featuredImageUrl || images.value[0] || '')
    quantity.value = 1
    detailTab.value = 'description'
  } catch (caught: unknown) {
    product.value = null
    toast.error(
      'No se pudo abrir el producto',
      caught instanceof ApiError ? caught.message : undefined
    )
  } finally {
    loading.value = false
  }
}

async function add(): Promise<void> {
  if (!product.value) return
  adding.value = true
  try {
    const limit = product.value.trackInventory && !product.value.allowBackorder
      ? Math.max(1, availableStock.value)
      : 99
    const safeQuantity = Math.max(
      1,
      Math.min(Number(quantity.value) || 1, limit)
    )
    quantity.value = safeQuantity

    await cart.add(
      'store',
      product.value.id,
      safeQuantity,
      selected.value ? { variantId: selected.value.id } : {}
    )
    toast.success(
      'Producto agregado',
      `${safeQuantity} × ${product.value.name} se agregó al carrito.`
    )
  } catch (caught: unknown) {
    toast.error(
      'No se pudo agregar',
      caught instanceof ApiError ? caught.message : undefined
    )
  } finally {
    adding.value = false
  }
}

function changeQuantity(delta: number): void {
  const maximum = product.value?.trackInventory && !product.value?.allowBackorder
    ? Math.max(1, availableStock.value)
    : 99
  quantity.value = Math.max(1, Math.min(quantity.value + delta, maximum))
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', {
    style: 'currency',
    currency
  }).format(value || 0)
}

function productTypeLabel(type: string): string {
  if (type === 'DIGITAL') return 'Producto Digital'
  if (type === 'SERVICE') return 'Servicio profesional'
  if (type === 'PHYSICAL') return 'Producto físico'
  return type || 'Producto'
}

watch(selectedVariant, () => {
  if (selected.value?.imageUrl) activeImage.value = selected.value.imageUrl
  quantity.value = 1
})
watch(() => route.params.slug, load)
onMounted(load)
</script>

<template>
  <main class="product-page product-page--commerce">
    <section v-if="loading" class="content-section product-commerce-layout">
      <div class="product-gallery-card panel"><SkeletonBlock :lines="10" height="1.1rem" /></div>
      <div class="product-buy-card panel"><SkeletonBlock :lines="12" height="1.1rem" /></div>
    </section>

    <template v-else-if="product">
      <section class="content-section product-commerce-layout">
        <!-- Media Gallery Column -->
        <div class="product-gallery-card">
          <div
            class="product-gallery-card__main"
            :class="{ 'has-image': activeImage }"
            :style="activeImage ? { backgroundImage: `url(${activeImage})` } : {}"
          >
            <span v-if="product.featured" class="product-ribbon">★ Destacado</span>
            <div v-if="!activeImage" class="product-placeholder-art">
              <span class="art-icon">{{ product.type === 'DIGITAL' ? '↓' : product.type === 'SERVICE' ? '✦' : '📦' }}</span>
              <strong class="art-brand">{{ product.brand || 'TrastStore' }}</strong>
            </div>
          </div>

          <div v-if="images.length > 1" class="product-gallery-card__thumbs">
            <button
              v-for="image in images"
              :key="image"
              type="button"
              :class="{ active: activeImage === image }"
              :style="{ backgroundImage: `url(${image})` }"
              @click="activeImage = image"
            />
          </div>

          <div class="product-gallery-card__service">
            <span class="trust-item">✓ Compra 100% protegida</span>
            <span class="trust-item">✓ Impuestos transparentes</span>
            <span class="trust-item">✓ Seguimiento de orden</span>
          </div>
        </div>

        <!-- Product Purchase Information Column -->
        <article class="product-buy-card">
          <nav class="product-breadcrumbs" aria-label="Ruta del producto">
            <RouterLink to="/store">Tienda</RouterLink>
            <span class="breadcrumb-separator">/</span>
            <span>{{ product.category || 'Catálogo' }}</span>
          </nav>

          <div class="product-tags">
            <span class="tag-pill type">{{ productTypeLabel(product.type) }}</span>
            <span v-if="product.brand" class="tag-pill brand">{{ product.brand }}</span>
            <span class="tag-pill sku">SKU: {{ product.sku }}</span>
          </div>

          <h1 class="product-title">{{ product.name }}</h1>
          <p class="product-buy-card__lead">
            {{ product.shortDescription || product.description }}
          </p>

          <div class="product-buy-card__price">
            <del v-if="product.compareAtPrice && product.compareAtPrice > displayPrice">
              {{ money(product.compareAtPrice, product.currency) }}
            </del>
            <strong class="price-main">{{ money(displayPrice, product.currency) }}</strong>
            <span v-if="product.taxable" class="tax-badge">
              + {{ product.effectiveTaxRate }}% de impuesto aplicable
            </span>
            <span v-else class="tax-badge exento">Producto exento de impuesto</span>
          </div>

          <label v-if="product.variants?.length" class="product-field">
            <span>Seleccionar variante</span>
            <select v-model="selectedVariant">
              <option
                v-for="variant in product.variants.filter((item: any) => item.active)"
                :key="variant.id"
                :value="variant.id"
              >
                {{ variant.name }} · {{ money(variant.price, product.currency) }}
                · {{ variant.stockQuantity }} en existencia
              </option>
            </select>
          </label>

          <div class="product-availability" :class="{ warning: availableStock <= product.lowStockThreshold && availableStock > 0 && product.type === 'PHYSICAL', out: availableStock <= 0 && product.type === 'PHYSICAL' }">
            <span :class="['availability-dot', { available: product.type === 'DIGITAL' || product.inStock }]" />
            <div class="availability-text">
              <strong>{{ product.type === 'DIGITAL' ? '⚡ Entrega Inmediata Digital' : availabilityLabel }}</strong>
              <small>{{ product.type === 'DIGITAL' ? 'Archivo descargable disponible inmediatamente tras el pago.' : (product.allowBackorder ? 'Se permiten pedidos pendientes.' : 'Inventario actualizado en tiempo real.') }}</small>
            </div>
          </div>

          <!-- Redesigned Purchase Controls Box -->
          <div class="product-purchase-card">
            <div class="quantity-picker-box">
              <span class="quantity-label">Cantidad</span>
              <div class="quantity-control-stepper">
                <button type="button" aria-label="Restar una unidad" @click="changeQuantity(-1)">−</button>
                <input
                  v-model.number="quantity"
                  type="number"
                  min="1"
                  :max="product.trackInventory && !product.allowBackorder ? Math.max(1, availableStock) : 99"
                  aria-label="Cantidad"
                >
                <button type="button" aria-label="Sumar una unidad" @click="changeQuantity(1)">+</button>
              </div>
            </div>

            <button
              class="button product-add-button"
              :disabled="adding || (!product.inStock && !product.allowBackorder)"
              @click="add"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <path d="M16 10a4 4 0 0 1-8 0"></path>
              </svg>
              <span>{{ adding ? 'Agregando…' : product.inStock ? 'Agregar al carrito' : 'Agotado' }}</span>
            </button>
          </div>

          <!-- Enhanced Benefit Cards -->
          <div class="product-checkout-benefits">
            <article class="benefit-card">
              <span class="benefit-icon">💳</span>
              <div class="benefit-info">
                <b>Pago flexible</b>
                <small>Transferencia, PayPal, sandbox o tarjetas.</small>
              </div>
            </article>
            <article class="benefit-card">
              <span class="benefit-icon">📦</span>
              <div class="benefit-info">
                <b>Entrega clara</b>
                <small>Digital instantánea o envío coordinado.</small>
              </div>
            </article>
            <article class="benefit-card">
              <span class="benefit-icon">🎧</span>
              <div class="benefit-info">
                <b>Soporte postventa</b>
                <small>Orden vinculada a su cuenta de cliente.</small>
              </div>
            </article>
          </div>
        </article>
      </section>

      <!-- Details Tabs Section -->
      <section class="content-section product-information">
        <nav class="product-information__tabs" aria-label="Pestañas de información del producto">
          <button :class="{ active: detailTab === 'description' }" @click="detailTab = 'description'">
            📄 Descripción
          </button>
          <button :class="{ active: detailTab === 'specifications' }" @click="detailTab = 'specifications'">
            ⚙️ Especificaciones
          </button>
          <button :class="{ active: detailTab === 'delivery' }" @click="detailTab = 'delivery'">
            🚚 Entrega y soporte
          </button>
        </nav>

        <div v-if="detailTab === 'description'" class="product-information__panel">
          <div class="panel-header">
            <span class="eyebrow">INFORMACIÓN DETALLADA</span>
            <h2>Todo lo que incluye este producto</h2>
          </div>
          <div class="panel-body text-content">
            <p>{{ product.description || product.shortDescription || 'Sin información detallada adicional.' }}</p>
          </div>
        </div>

        <div v-else-if="detailTab === 'specifications'" class="product-information__panel">
          <div class="panel-header">
            <span class="eyebrow">FICHA TÉCNICA</span>
            <h2>Características principales</h2>
          </div>
          <dl class="product-specification-grid">
            <div class="spec-row"><dt>Tipo de Producto</dt><dd>{{ productTypeLabel(product.type) }}</dd></div>
            <div class="spec-row"><dt>Marca / Proveedor</dt><dd>{{ product.brand || 'TrastCMS' }}</dd></div>
            <div class="spec-row"><dt>Categoría</dt><dd>{{ product.category || 'General' }}</dd></div>
            <div class="spec-row"><dt>Código SKU</dt><dd>{{ product.sku }}</dd></div>
            <div class="spec-row"><dt>Impuestos</dt><dd>{{ product.taxable ? `Aplica (${product.effectiveTaxRate}%)` : 'Exento' }}</dd></div>
            <div v-for="(value, key) in product.attributes" :key="key" class="spec-row">
              <dt>{{ key }}</dt><dd>{{ value }}</dd>
            </div>
          </dl>
        </div>

        <div v-else class="product-information__panel">
          <div class="panel-header">
            <span class="eyebrow">POSTCOMPRA Y ENTREGA</span>
            <h2>¿Qué sucede después de realizar su pago?</h2>
          </div>
          <div class="product-delivery-grid">
            <article class="delivery-card">
              <div class="delivery-card__header">
                <span class="delivery-card__badge">Paso 01</span>
                <span class="delivery-card__icon">📋</span>
              </div>
              <h3>Confirmación de Orden</h3>
              <p>TrastPay verifica el pago de forma segura y genera la confirmación inmediata de su orden.</p>
            </article>

            <article class="delivery-card">
              <div class="delivery-card__header">
                <span class="delivery-card__badge">Paso 02</span>
                <span class="delivery-card__icon">⚡</span>
              </div>
              <h3>Procesamiento Automático</h3>
              <p>Si es un producto digital o SaaS, las licencias o descargas se emiten al instante para su activación.</p>
            </article>

            <article class="delivery-card">
              <div class="delivery-card__header">
                <span class="delivery-card__badge">Paso 03</span>
                <span class="delivery-card__icon">🛡️</span>
              </div>
              <h3>Seguimiento y Atención</h3>
              <p>Podrá consultar su comprobante, estado de orden y gestionar su producto desde el portal o por correo.</p>
            </article>
          </div>
        </div>
      </section>
    </template>

    <section v-else class="content-section public-error-state">
      <span class="error-icon">⚠️</span>
      <h2>Producto no disponible</h2>
      <p>El producto fue eliminado, archivado o no está publicado en el catálogo actual.</p>
      <RouterLink to="/store" class="button">Volver a la tienda</RouterLink>
    </section>
  </main>
</template>
