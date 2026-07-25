<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type Product = {
  id: string
  name: string
  slug: string
  sku: string
  shortDescription?: string
  description?: string
  type: string
  status: string
  price: number
  compareAtPrice?: number | null
  currency: string
  category?: string | null
  brand?: string | null
  featuredImageUrl?: string | null
  featured: boolean
  inStock: boolean
  taxable: boolean
  taxClass: string
  effectiveTaxRate: number
}

type ProductPage = {
  content: Product[]
  totalElements: number
  totalPages: number
}

const cart = useCartStore()
const toast = useToastStore()
const products = ref<Product[]>([])
const loading = ref(true)
const addingId = ref('')
const search = ref('')
const category = ref('all')
const sort = ref<'featured' | 'price-asc' | 'price-desc' | 'name'>('featured')
const total = ref(0)

const categories = computed(() => [
  'all',
  ...Array.from(new Set(products.value.map(product => product.category).filter(Boolean) as string[])).sort()
])

const visibleProducts = computed(() => {
  let result = category.value === 'all'
    ? [...products.value]
    : products.value.filter(product => product.category === category.value)

  if (sort.value === 'price-asc') result.sort((left, right) => left.price - right.price)
  if (sort.value === 'price-desc') result.sort((left, right) => right.price - left.price)
  if (sort.value === 'name') result.sort((left, right) => left.name.localeCompare(right.name))
  if (sort.value === 'featured') {
    result.sort((left, right) => Number(right.featured) - Number(left.featured))
  }
  return result
})

const featured = computed(() => visibleProducts.value.filter(product => product.featured).slice(0, 4))

async function load(): Promise<void> {
  loading.value = true
  try {
    const response = await api<ProductPage>(
      `/api/public/store/products?size=60&search=${encodeURIComponent(search.value)}`
    )
    products.value = response.content
    total.value = response.totalElements
    if (category.value !== 'all' && !categories.value.includes(category.value)) {
      category.value = 'all'
    }
  } catch (caught: unknown) {
    toast.error(
      'No se pudo cargar la tienda',
      caught instanceof ApiError ? caught.message : 'Intente nuevamente en unos segundos.'
    )
  } finally {
    loading.value = false
  }
}

async function add(product: Product): Promise<void> {
  addingId.value = product.id
  try {
    await cart.add('store', product.id)
    toast.success(
      'Producto agregado',
      `${product.name} ya está en su carrito.`
    )
  } catch (caught: unknown) {
    toast.error(
      'No se pudo agregar el producto',
      caught instanceof ApiError ? caught.message : 'Revise el inventario o la moneda del carrito.'
    )
  } finally {
    addingId.value = ''
  }
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

function productTypeLabel(type: string): string {
  if (type === 'DIGITAL') return 'Descargable'
  if (type === 'SERVICE') return 'Servicio'
  if (type === 'PHYSICAL') return 'Físico'
  return type || 'Producto'
}

onMounted(load)
</script>

<template>
  <main class="storefront storefront--modern">
    <!-- Store Hero Header -->
    <section class="store-hero store-hero--ecommerce">
      <div class="store-hero__content">
        <span class="eyebrow">TRASTSTORE · TIENDA OFICIAL</span>
        <h1>Catálogo de Productos y Licencias</h1>
        <p>
          Encuentre software, soluciones digitales y productos físicos con checkout transparente y gestión instantánea.
        </p>

        <form class="store-search" @submit.prevent="load">
          <div class="store-search__wrapper">
            <svg class="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              v-model="search"
              aria-label="Buscar en la tienda"
              placeholder="Buscar por nombre, SKU o categoría..."
            >
            <button v-if="search" type="button" class="search-clear-btn" aria-label="Limpiar búsqueda" @click="search = ''; load()">✕</button>
          </div>
          <button class="button" type="submit">Buscar</button>
        </form>

        <div class="store-hero__badges">
          <span class="hero-chip">✓ Envíos y entregas inmediatas</span>
          <span class="hero-chip">✓ Licencias oficiales</span>
          <span class="hero-chip">✓ Pago 100% seguro</span>
        </div>
      </div>

      <aside class="store-hero__quick-cart" aria-label="Resumen rápido">
        <div class="cart-summary-badge">
          <span class="cart-badge-icon">🛒</span>
          <div>
            <b>Su Carrito</b>
            <small>{{ cart.count }} {{ cart.count === 1 ? 'artículo' : 'artículos' }} seleccionados</small>
          </div>
        </div>
        <RouterLink to="/cart" class="button full">
          {{ cart.count ? 'Ir al Checkout →' : 'Ver Carrito' }}
        </RouterLink>
      </aside>
    </section>

    <!-- Category Pill Bar -->
    <section v-if="categories.length > 1 && !loading" class="content-section store-category-pills">
      <div class="category-pills-scroll">
        <button
          v-for="cat in categories"
          :key="cat"
          type="button"
          :class="['category-pill', { active: category === cat }]"
          @click="category = cat"
        >
          {{ cat === 'all' ? '🏷️ Todas las categorías' : cat }}
        </button>
      </div>
    </section>

    <!-- Catalog Controls Bar -->
    <section class="content-section store-catalog-section">
      <div class="store-controls">
        <div class="store-controls__info">
          <span class="eyebrow">CATÁLOGO DE TIENDA</span>
          <h2>{{ visibleProducts.length }} {{ visibleProducts.length === 1 ? 'producto disponible' : 'productos disponibles' }}</h2>
          <p v-if="category !== 'all'">Filtrando por categoría: <strong>{{ category }}</strong></p>
        </div>

        <div class="store-filters">
          <label class="filter-group">
            <span>Categoría</span>
            <select v-model="category">
              <option v-for="item in categories" :key="item" :value="item">
                {{ item === 'all' ? 'Todas las categorías' : item }}
              </option>
            </select>
          </label>
          <label class="filter-group">
            <span>Ordenar por</span>
            <select v-model="sort">
              <option value="featured">Destacados primero</option>
              <option value="price-asc">Precio: Menor a Mayor</option>
              <option value="price-desc">Precio: Mayor a Menor</option>
              <option value="name">Nombre A-Z</option>
            </select>
          </label>
        </div>
      </div>

      <!-- Loading Skeleton State -->
      <div v-if="loading" class="store-grid">
        <article
          v-for="index in 6"
          :key="index"
          class="product-card product-card--skeleton"
        >
          <div class="skeleton-block product-card__media-skeleton" />
          <div class="product-card__body">
            <SkeletonBlock :lines="2" height="1.1rem" />
            <div class="mt-5"><SkeletonBlock :lines="3" height="0.8rem" /></div>
          </div>
        </article>
      </div>

      <!-- Product Cards Grid -->
      <div v-else-if="visibleProducts.length" class="store-grid">
        <article
          v-for="product in visibleProducts"
          :key="product.id"
          class="product-card product-card--modern"
        >
          <RouterLink
            :to="`/store/product/${product.slug}`"
            class="product-media"
            :style="product.featuredImageUrl
              ? { backgroundImage: `url(${product.featuredImageUrl})` }
              : {}"
          >
            <div class="product-media__badges">
              <span v-if="product.featured" class="status-pill ok">Destacado</span>
              <span class="product-type-pill">{{ productTypeLabel(product.type) }}</span>
            </div>
            <div v-if="!product.featuredImageUrl" class="product-media__placeholder">
              <span class="placeholder-icon">🛍️</span>
              <span class="placeholder-brand">{{ product.brand || 'TrastStore' }}</span>
            </div>
          </RouterLink>

          <div class="product-card__body">
            <div class="product-card__header">
              <div class="product-card__topline">
                <span class="product-category-tag">{{ product.category || 'General' }}</span>
                <span v-if="product.brand" class="product-brand-tag">{{ product.brand }}</span>
              </div>
              <RouterLink :to="`/store/product/${product.slug}`" class="product-title-link">
                <h3>{{ product.name }}</h3>
              </RouterLink>
              <p class="product-desc">{{ product.shortDescription || product.description || 'Sin descripción disponible.' }}</p>
            </div>

            <div class="product-card__stock-bar">
              <span :class="['availability-dot', { available: product.inStock }]" />
              <span class="stock-text">{{ product.inStock ? 'En existencia' : 'Agotado' }}</span>
            </div>

            <div class="product-card__footer">
              <div class="product-card__price">
                <del v-if="product.compareAtPrice && product.compareAtPrice > product.price">
                  {{ money(product.compareAtPrice, product.currency) }}
                </del>
                <strong>{{ money(product.price, product.currency) }}</strong>
                <small class="product-tax-note">
                  {{ product.taxable ? `+ ${product.effectiveTaxRate}% imp.` : 'Exento' }}
                </small>
              </div>

              <button
                class="button small add-to-cart-btn"
                :disabled="!product.inStock || addingId === product.id"
                @click="add(product)"
              >
                <svg v-if="addingId !== product.id" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
                <span>{{ addingId === product.id ? 'Agregando…' : product.inStock ? 'Agregar' : 'Agotado' }}</span>
              </button>
            </div>
          </div>
        </article>
      </div>

      <!-- Empty Search / Filter State -->
      <div v-else class="public-empty-state">
        <span class="empty-state-icon">⌕</span>
        <h2>No encontramos productos que coincidan</h2>
        <p>Pruebe con otra palabra clave o restablezca los filtros de categoría.</p>
        <button
          class="button secondary"
          @click="search = ''; category = 'all'; load()"
        >
          Restablecer búsqueda y filtros
        </button>
      </div>
    </section>
  </main>
</template>
