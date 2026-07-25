<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'
import UiIcon from '@/components/common/UiIcon.vue'

type Tab = 'overview' | 'products' | 'inventory'
type JsonRecord = Record<string, any>
type ProductPage = { content: JsonRecord[]; totalElements: number; totalPages: number }

const toast = useToastStore()
const tab = ref<Tab>('overview')
const dashboard = ref<JsonRecord>({})
const products = ref<JsonRecord[]>([])
const movements = ref<JsonRecord[]>([])
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const search = ref('')
const statusFilter = ref('ALL')
const typeFilter = ref('ALL')
const stockFilter = ref('ALL')
const editor = ref(false)
const inventoryEditor = ref(false)
const demoBusy = ref(false)
const draft = reactive<JsonRecord>({})
const adjustment = reactive<JsonRecord>({
  productId: '',
  variantId: '',
  type: 'ADD',
  quantity: 1,
  reason: 'Ajuste manual',
  referenceType: 'MANUAL',
  referenceId: ''
})
const fileInput = ref<HTMLInputElement | null>(null)

const filteredProducts = computed(() =>
  products.value.filter(product => {
    const statusMatches = statusFilter.value === 'ALL' || product.status === statusFilter.value
    const typeMatches = typeFilter.value === 'ALL' || product.type === typeFilter.value
    const term = search.value.trim().toLowerCase()
    const searchMatches =
      !term ||
      `${product.name} ${product.sku} ${product.category || ''} ${product.brand || ''}`
        .toLowerCase()
        .includes(term)
    return statusMatches && typeMatches && searchMatches
  })
)

const inventoryTrackedProducts = computed(() =>
  products.value.filter(product => {
    if (!product.trackInventory) return false
    const term = search.value.trim().toLowerCase()
    const searchMatches =
      !term ||
      `${product.name} ${product.sku} ${product.category || ''}`
        .toLowerCase()
        .includes(term)

    if (stockFilter.value === 'LOW') {
      return (
        searchMatches &&
        product.stockQuantity > 0 &&
        product.stockQuantity <= product.lowStockThreshold
      )
    }
    if (stockFilter.value === 'OUT') {
      return searchMatches && product.stockQuantity <= 0
    }
    if (stockFilter.value === 'OK') {
      return searchMatches && product.stockQuantity > product.lowStockThreshold
    }
    return searchMatches
  })
)

const inventoryProduct = computed(
  () => products.value.find(item => item.id === adjustment.productId) || null
)
const inventoryVariants = computed(() => inventoryProduct.value?.variants || [])

const projectedStock = computed(() => {
  if (!inventoryProduct.value) return 0
  const current = Number(inventoryProduct.value.stockQuantity || 0)
  const qty = Number(adjustment.quantity || 0)
  if (adjustment.type === 'ADD' || adjustment.type === 'RETURN') return current + qty
  if (adjustment.type === 'REMOVE' || adjustment.type === 'DAMAGE') return Math.max(0, current - qty)
  if (adjustment.type === 'SET') return qty
  return current
})

function emptyProduct(): JsonRecord {
  return {
    id: '',
    name: '',
    slug: '',
    sku: '',
    description: '',
    shortDescription: '',
    type: 'PHYSICAL',
    status: 'DRAFT',
    price: 0,
    compareAtPrice: null,
    currency: 'USD',
    category: '',
    brand: '',
    featuredImageUrl: '',
    gallery: [],
    attributes: {},
    seo: {},
    featured: false,
    trackInventory: true,
    allowBackorder: false,
    stockQuantity: 0,
    lowStockThreshold: 5,
    weightGrams: null,
    digitalUrl: '',
    taxable: true,
    taxClass: 'STANDARD',
    taxRateOverride: null,
    variants: []
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [dashboardResult, productsResult, movementsResult] = await Promise.allSettled([
      api<JsonRecord>('/api/admin/store/dashboard'),
      api<ProductPage>('/api/admin/store/products?size=100'),
      api<ProductPage>('/api/admin/store/inventory/movements?size=100')
    ])
    if (dashboardResult.status === 'fulfilled') dashboard.value = dashboardResult.value
    if (productsResult.status === 'fulfilled') products.value = productsResult.value.content
    if (movementsResult.status === 'fulfilled') movements.value = movementsResult.value.content
    const failed = [dashboardResult, productsResult, movementsResult].filter(
      item => item.status === 'rejected'
    )
    if (failed.length) {
      toast.warning('TrastStore cargó parcialmente', 'Revise las secciones que muestran datos pendientes.')
    }
  } catch (caught: unknown) {
    toast.error('No se pudo cargar TrastStore', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    loading.value = false
  }
}

function editProduct(product?: JsonRecord): void {
  Object.keys(draft).forEach(key => delete draft[key])
  Object.assign(draft, emptyProduct(), product ? JSON.parse(JSON.stringify(product)) : {})
  draft.variants = (product?.variants || []).map((variant: JsonRecord) => ({ ...variant }))
  editor.value = true
}

function triggerFileUpload(): void {
  fileInput.value?.click()
}

async function handleFileUpload(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const result = await api<JsonRecord>('/api/admin/media/upload', {
      method: 'POST',
      body: formData
    })
    draft.digitalUrl = result.url || result.path || draft.digitalUrl
    toast.success('Archivo digital subido', `${file.name} vinculado al producto.`)
  } catch (caught: unknown) {
    toast.error('No se pudo subir el archivo', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    uploading.value = false
    if (target) target.value = ''
  }
}

function addVariant(): void {
  draft.variants.push({
    id: '',
    name: '',
    sku: '',
    price: null,
    stockQuantity: 0,
    optionValues: '',
    imageUrl: '',
    active: true
  })
}

function removeVariant(index: number): void {
  draft.variants.splice(index, 1)
}

async function saveProduct(): Promise<void> {
  saving.value = true
  try {
    // Autoconfigurar banderas según el tipo
    if (draft.type === 'DIGITAL') {
      draft.trackInventory = false
    }
    await api(draft.id ? `/api/admin/store/products/${draft.id}` : '/api/admin/store/products', {
      method: draft.id ? 'PUT' : 'POST',
      body: JSON.stringify(draft)
    })
    editor.value = false
    toast.success('Producto guardado', `${draft.name} quedó actualizado.`)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo guardar el producto', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function deleteProduct(product: JsonRecord): Promise<void> {
  if (!window.confirm(`¿Eliminar ${product.name}? Esta acción no se puede deshacer.`)) return
  try {
    await api(`/api/admin/store/products/${product.id}`, { method: 'DELETE' })
    toast.success('Producto eliminado')
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo eliminar', caught instanceof ApiError ? caught.message : undefined)
  }
}

function openInventory(product?: JsonRecord): void {
  adjustment.productId = product?.id || products.value[0]?.id || ''
  adjustment.variantId = ''
  adjustment.type = 'ADD'
  adjustment.quantity = 1
  adjustment.reason = 'Ajuste manual'
  adjustment.referenceType = 'MANUAL'
  adjustment.referenceId = ''
  inventoryEditor.value = true
}

async function saveAdjustment(): Promise<void> {
  if (!adjustment.productId) {
    toast.warning('Seleccione un producto')
    return
  }
  saving.value = true
  try {
    const { productId, ...payload } = adjustment
    await api(`/api/admin/store/products/${productId}/inventory`, {
      method: 'POST',
      body: JSON.stringify({ ...payload, variantId: payload.variantId || null })
    })
    inventoryEditor.value = false
    toast.success('Inventario actualizado', 'El movimiento quedó registrado en el historial.')
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudo ajustar el inventario', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    saving.value = false
  }
}

async function generateDemo(): Promise<void> {
  demoBusy.value = true
  try {
    const result = await api<JsonRecord>('/api/admin/store/demo-data', { method: 'POST' })
    toast.success('Datos demo cargados', result.message)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudieron cargar los datos demo', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    demoBusy.value = false
  }
}

async function deleteDemo(): Promise<void> {
  if (!window.confirm('Se eliminarán únicamente los productos marcados como demostrativos.')) return
  demoBusy.value = true
  try {
    const result = await api<JsonRecord>('/api/admin/store/demo-data', { method: 'DELETE' })
    toast.success('Datos demo eliminados', result.message)
    await load()
  } catch (caught: unknown) {
    toast.error('No se pudieron eliminar los datos demo', caught instanceof ApiError ? caught.message : undefined)
  } finally {
    demoBusy.value = false
  }
}

function money(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-HN', { style: 'currency', currency }).format(value || 0)
}

function stockLabel(product: JsonRecord): string {
  if (product.type === 'DIGITAL') {
    return product.digitalUrl ? '📥 Descargable listo' : '⚠️ Sin archivo'
  }
  if (product.type === 'SERVICE') return '✦ Servicio'
  if (!product.trackInventory) return 'Sin control'
  if (product.stockQuantity <= 0) return 'Agotado'
  if (product.stockQuantity <= product.lowStockThreshold) return 'Inventario bajo'
  return `${product.stockQuantity} unidades`
}

function movementSign(item: JsonRecord): string {
  return Number(item.quantity) > 0 ? `+${item.quantity}` : String(item.quantity)
}
function movementTypeLabel(type: string): string {
  if (type === 'ADD') return 'Entrada'
  if (type === 'REMOVE') return 'Salida'
  if (type === 'SET') return 'Ajuste manual'
  if (type === 'DAMAGE') return 'Pérdida / Daño'
  if (type === 'RETURN') return 'Devolución'
  return type || 'Movimiento'
}

onMounted(load)
</script>

<template>
  <div class="plugin-workspace store-admin-workspace store-admin-workspace--v2">
    <!-- Hidden File Input for Digital Downloads -->
    <input
      ref="fileInput"
      type="file"
      class="hidden"
      style="display: none"
      @change="handleFileUpload"
    >

    <!-- Header Banner -->
    <header class="plugin-hero plugin-hero--store">
      <div>
        <span class="eyebrow">TRASTSTORE · GESTIÓN DE TIENDA E INVENTARIO</span>
        <h1>Tienda online</h1>
        <p>Control de productos físicos y digitales, inventario en tiempo real, impuestos y descargas.</p>
      </div>
      <div class="plugin-hero__actions">
        <button class="button secondary" :disabled="demoBusy" @click="generateDemo">
          Generar demo
        </button>
        <button class="button secondary danger-outline" :disabled="demoBusy" @click="deleteDemo">
          Eliminar demo
        </button>
        <button class="button" @click="tab = 'products'; editProduct()">
          <UiIcon name="plus" :size="16" />
          <span>Nuevo producto</span>
        </button>
      </div>
    </header>

    <!-- Clean Navigation Tabs -->
    <nav class="plugin-tabs plugin-tabs--cards" aria-label="Navegación de Tienda">
      <button :class="{ active: tab === 'overview' }" @click="tab = 'overview'">
        📊 Resumen
      </button>
      <button :class="{ active: tab === 'products' }" @click="tab = 'products'">
        📦 Catálogo ({{ products.length }})
      </button>
      <button :class="{ active: tab === 'inventory' }" @click="tab = 'inventory'">
        🏭 Inventario {{ dashboard.lowStock > 0 ? `(⚠️ ${dashboard.lowStock})` : '' }}
      </button>
    </nav>

    <!-- Skeleton Loading State -->
    <section v-if="loading" class="store-admin-skeleton">
      <article v-for="index in 4" :key="index" class="panel">
        <SkeletonBlock :lines="5" />
      </article>
    </section>

    <template v-else>
      <!-- TAB 1: OVERVIEW -->
      <section v-if="tab === 'overview'" class="space-y-6">
        <div class="store-metric-grid">
          <article class="store-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon primary">📦</span>
              <span class="metric-label">Productos</span>
            </div>
            <strong class="metric-value">{{ dashboard.products || 0 }}</strong>
            <small class="metric-sub">{{ dashboard.active || 0 }} publicados activamente</small>
          </article>

          <article class="store-metric-card" :class="{ alert: (dashboard.lowStock || 0) > 0 }">
            <div class="metric-card__header">
              <span class="metric-icon warning">⚠️</span>
              <span class="metric-label">Inventario bajo</span>
            </div>
            <strong class="metric-value">{{ dashboard.lowStock || 0 }}</strong>
            <small class="metric-sub">{{ dashboard.outOfStock || 0 }} productos agotados</small>
          </article>

          <article class="store-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon success">📥</span>
              <span class="metric-label">Productos Digitales</span>
            </div>
            <strong class="metric-value">{{ products.filter(p => p.type === 'DIGITAL').length }}</strong>
            <small class="metric-sub">Descargables y entregas en línea</small>
          </article>

          <article class="store-metric-card">
            <div class="metric-card__header">
              <span class="metric-icon muted">🛠️</span>
              <span class="metric-label">Datos demo</span>
            </div>
            <strong class="metric-value">{{ dashboard.demoProducts || 0 }}</strong>
            <small class="metric-sub">Eliminables de forma segura</small>
          </article>
        </div>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <span class="eyebrow">DESTACADOS</span>
              <h2>Productos principales</h2>
            </div>
            <button class="text-link" @click="tab = 'products'">Administrar todo →</button>
          </div>

          <div class="store-featured-grid">
            <article
              v-for="product in (dashboard.featured || []).slice(0, 4)"
              :key="product.id"
              class="store-featured-tile"
            >
              <div
                class="featured-tile__image"
                :style="product.featuredImageUrl ? { backgroundImage: `url(${product.featuredImageUrl})` } : {}"
              >
                <span class="tile-category-tag">{{ product.type === 'DIGITAL' ? '📥 DIGITAL' : (product.category || product.type) }}</span>
              </div>
              <div class="featured-tile__body">
                <h3>{{ product.name }}</h3>
                <div class="featured-tile__footer">
                  <strong class="tile-price">{{ money(product.price, product.currency) }}</strong>
                  <button class="button secondary small" @click="editProduct(product)">
                    Editar
                  </button>
                </div>
              </div>
            </article>

            <div v-if="!(dashboard.featured || []).length" class="public-empty-state">
              <span class="empty-state-icon">★</span>
              <h3>No hay productos destacados</h3>
              <p>Marque productos como destacados desde el catálogo para destacarlos aquí.</p>
            </div>
          </div>
        </section>
      </section>

      <!-- TAB 2: PRODUCTS CATALOG -->
      <section v-else-if="tab === 'products'" class="space-y-5">
        <div class="panel store-admin-toolbar">
          <div>
            <span class="eyebrow">CATÁLOGOS Y PRODUCTOS</span>
            <h2>{{ filteredProducts.length }} productos</h2>
          </div>
          <div class="toolbar-actions">
            <input
              v-model="search"
              placeholder="Buscar por nombre, SKU, marca o categoría..."
              class="toolbar-search-input"
            >
            <select v-model="typeFilter" class="toolbar-select">
              <option value="ALL">Todos los tipos</option>
              <option value="PHYSICAL">📦 Físicos</option>
              <option value="DIGITAL">📥 Digitales</option>
              <option value="SERVICE">✦ Servicios</option>
            </select>
            <select v-model="statusFilter" class="toolbar-select">
              <option value="ALL">Todos los estados</option>
              <option value="ACTIVE">Activos</option>
              <option value="DRAFT">Borradores</option>
              <option value="ARCHIVED">Archivados</option>
            </select>
            <button class="button" @click="editProduct()">
              + Nuevo producto
            </button>
          </div>
        </div>

        <div class="store-admin-card-grid">
          <article
            v-for="product in filteredProducts"
            :key="product.id"
            class="store-admin-product-card"
          >
            <div
              class="store-admin-product-card__image"
              :style="product.featuredImageUrl ? { backgroundImage: `url(${product.featuredImageUrl})` } : {}"
            >
              <span :class="['product-type-badge', product.type === 'DIGITAL' ? 'digital' : 'physical']">
                {{ product.type === 'DIGITAL' ? '📥 Digital' : product.type === 'PHYSICAL' ? '📦 Físico' : product.type }}
              </span>
            </div>

            <div class="store-admin-product-card__body">
              <div class="card-meta-line">
                <small class="product-category-name">{{ product.category || 'General' }}</small>
                <span :class="['status-pill', product.status === 'ACTIVE' ? 'ok' : 'warning']">
                  {{ product.status }}
                </span>
              </div>

              <h3>{{ product.name }}</h3>
              <p class="product-short-desc">{{ product.shortDescription || 'Sin descripción corta.' }}</p>

              <dl class="product-specs-grid">
                <div>
                  <dt>SKU</dt>
                  <dd>{{ product.sku }}</dd>
                </div>
                <div>
                  <dt>Precio</dt>
                  <dd>{{ money(product.price, product.currency) }}</dd>
                </div>
                <div>
                  <dt>Tipo / Stock</dt>
                  <dd :class="{ danger: product.type === 'PHYSICAL' && product.stockQuantity <= 0, warning: product.type === 'DIGITAL' && !product.digitalUrl }">
                    {{ stockLabel(product) }}
                  </dd>
                </div>
                <div>
                  <dt>Impuesto</dt>
                  <dd>{{ product.taxable ? `${product.effectiveTaxRate || 0}%` : 'Exento' }}</dd>
                </div>
              </dl>

              <footer class="card-actions-bar">
                <button
                  v-if="product.type === 'PHYSICAL'"
                  class="button secondary small"
                  @click="openInventory(product)"
                >
                  📦 Inventario
                </button>
                <button
                  v-else-if="product.type === 'DIGITAL'"
                  class="button secondary small"
                  @click="editProduct(product)"
                >
                  📥 Descargable
                </button>
                <button class="button small" @click="editProduct(product)">
                  ✏️ Editar
                </button>
                <button class="icon-button danger" title="Eliminar" @click="deleteProduct(product)">
                  ×
                </button>
              </footer>
            </div>
          </article>

          <div v-if="!filteredProducts.length" class="public-empty-state">
            <h2>No hay productos disponibles</h2>
            <p>Ajuste la búsqueda o cree su primer producto en el catálogo.</p>
          </div>
        </div>
      </section>

      <!-- TAB 3: INVENTORY MANAGEMENT & MOVEMENTS -->
      <section v-else class="space-y-5">
        <div class="panel store-inventory-header">
          <div>
            <span class="eyebrow">GESTIÓN DE STOCK</span>
            <h2>Existencias y trazabilidad de inventario (Productos Físicos)</h2>
            <p>Controle el stock físico, alertas mínimas y registre movimientos con historial completo.</p>
          </div>
          <button class="button" @click="openInventory()">
            <UiIcon name="plus" :size="16" />
            <span>Registrar movimiento</span>
          </button>
        </div>

        <!-- Inventory Summary Bar -->
        <div class="inventory-summary-bar">
          <div class="inventory-stat-chip">
            <span>Productos físicos controlados</span>
            <strong>{{ products.filter(p => p.trackInventory && p.type === 'PHYSICAL').length }}</strong>
          </div>
          <div class="inventory-stat-chip warning">
            <span>En nivel de alerta</span>
            <strong>{{ products.filter(p => p.trackInventory && p.type === 'PHYSICAL' && p.stockQuantity > 0 && p.stockQuantity <= p.lowStockThreshold).length }}</strong>
          </div>
          <div class="inventory-stat-chip danger">
            <span>Agotados sin stock</span>
            <strong>{{ products.filter(p => p.trackInventory && p.type === 'PHYSICAL' && p.stockQuantity <= 0).length }}</strong>
          </div>
          <div class="inventory-stat-chip info">
            <span>Movimientos registrados</span>
            <strong>{{ movements.length }}</strong>
          </div>
        </div>

        <div class="store-inventory-grid">
          <!-- Stock Table Column -->
          <section class="panel inventory-stock-panel">
            <div class="panel-heading">
              <div>
                <h2>Existencias de Productos Físicos</h2>
                <p>Productos con seguimiento de stock activo.</p>
              </div>
              <div class="inventory-stock-filters">
                <input
                  v-model="search"
                  placeholder="Buscar producto o SKU..."
                  class="search-mini-input"
                >
                <select v-model="stockFilter" class="select-mini-input">
                  <option value="ALL">Todos los productos</option>
                  <option value="OK">Stock normal</option>
                  <option value="LOW">Inventario bajo</option>
                  <option value="OUT">Agotados</option>
                </select>
              </div>
            </div>

            <div class="inventory-stock-list">
              <article
                v-for="product in inventoryTrackedProducts"
                :key="product.id"
                class="stock-item-row"
              >
                <div class="stock-item-info">
                  <strong>{{ product.name }}</strong>
                  <small>SKU: {{ product.sku }} · Alerta min: {{ product.lowStockThreshold }}</small>
                </div>

                <div class="stock-item-badge-wrap">
                  <span
                    :class="[
                      'stock-badge',
                      product.stockQuantity <= 0
                        ? 'out'
                        : product.stockQuantity <= product.lowStockThreshold
                        ? 'low'
                        : 'ok'
                    ]"
                  >
                    {{ product.stockQuantity }} unidades
                  </span>
                </div>

                <button class="button secondary small" @click="openInventory(product)">
                  ⚡ Ajustar
                </button>
              </article>

              <p v-if="!inventoryTrackedProducts.length" class="empty-cell">
                No hay productos físicos con control de inventario que coincidan.
              </p>
            </div>
          </section>

          <!-- Movement History Column -->
          <section class="panel inventory-movements-panel">
            <div class="panel-heading">
              <div>
                <h2>Historial reciente</h2>
                <p>Trazabilidad de entradas, salidas y ajustes.</p>
              </div>
            </div>

            <div class="inventory-movement-list">
              <article v-for="item in movements" :key="item.id" class="movement-row">
                <span :class="['movement-pill', Number(item.quantity) >= 0 ? 'positive' : 'negative']">
                  {{ movementSign(item) }}
                </span>
                <div class="movement-details">
                  <strong>{{ item.productName }}</strong>
                  <small>
                    <em class="type-tag">{{ movementTypeLabel(item.type) }}</em> · {{ item.reason }}
                  </small>
                  <time>{{ new Date(item.createdAt).toLocaleString() }}</time>
                </div>
              </article>

              <p v-if="!movements.length" class="empty-cell">
                Todavía no hay movimientos registrados.
              </p>
            </div>
          </section>
        </div>
      </section>
    </template>

    <!-- DRAWER: EDIT PRODUCT -->
    <div v-if="editor" class="app-drawer-backdrop" @click.self="editor = false">
      <form class="app-drawer app-drawer--wide" @submit.prevent="saveProduct">
        <header>
          <div>
            <span class="eyebrow">CATÁLOGO DE PRODUCTOS</span>
            <h2>{{ draft.id ? 'Editar producto' : 'Nuevo producto' }}</h2>
            <p>Configure producto físico o digital descargable, precios, inventario y adjuntos.</p>
          </div>
          <button type="button" class="icon-button" @click="editor = false">×</button>
        </header>

        <div class="app-drawer__body">
          <fieldset>
            <legend>Tipo de producto e información principal</legend>
            <div class="form-grid two">
              <label class="col-span-2 highlight-label">
                Tipo de Producto
                <select v-model="draft.type" class="product-type-select">
                  <option value="PHYSICAL">📦 Producto Físico (Lleva inventario y envíos)</option>
                  <option value="DIGITAL">📥 Producto Digital (Archivo descargable en línea)</option>
                  <option value="SERVICE">✦ Servicio Profesional / Consultoría</option>
                  <option value="EXTERNAL">🔗 Producto Externo / Afiliado</option>
                </select>
              </label>

              <label>Nombre del producto<input v-model="draft.name" required></label>
              <label>SKU (Código único)<input v-model="draft.sku" required></label>
              <label>Slug URL<input v-model="draft.slug"></label>
              <label>Estado
                <select v-model="draft.status">
                  <option value="DRAFT">Borrador</option>
                  <option value="ACTIVE">Activo</option>
                  <option value="ARCHIVED">Archivado</option>
                </select>
              </label>
              <label>Categoría<input v-model="draft.category" placeholder="Ej. Software, Ropa, Libros..."></label>
              <label>Marca o Fabricante<input v-model="draft.brand"></label>
              <label class="col-span-2">Imagen principal (URL)<input v-model="draft.featuredImageUrl" type="url" placeholder="https://..."></label>
            </div>
            <label>Descripción corta<textarea v-model="draft.shortDescription" rows="2"></textarea></label>
            <label>Descripción completa<textarea v-model="draft.description" rows="4"></textarea></label>
          </fieldset>

          <!-- SECCIÓN ESPECÍFICA: PRODUCTO DIGITAL (DESCARGABLE) -->
          <fieldset v-if="draft.type === 'DIGITAL'" class="digital-product-fieldset">
            <legend>📥 Configuración de Producto Digital & Descargable</legend>
            <p class="fieldset-desc">
              Los productos digitales no requieren inventario físico. Adjunte el archivo ejecutable, PDF, ZIP o recursos descargables que recibirá el comprador.
            </p>

            <div class="digital-upload-box">
              <label>URL del Archivo Descargable
                <div class="file-upload-input-wrap">
                  <input
                    v-model="draft.digitalUrl"
                    type="url"
                    placeholder="https://servidor.com/archivos/software-setup.zip"
                  >
                  <button
                    type="button"
                    class="button secondary"
                    :disabled="uploading"
                    @click="triggerFileUpload"
                  >
                    {{ uploading ? 'Subiendo…' : '📁 Subir archivo' }}
                  </button>
                </div>
              </label>

              <div v-if="draft.digitalUrl" class="digital-file-preview">
                <span class="file-preview-icon">📄</span>
                <div class="file-preview-info">
                  <strong>Archivo vinculado activo:</strong>
                  <code>{{ draft.digitalUrl }}</code>
                </div>
                <a :href="draft.digitalUrl" target="_blank" class="button secondary small">
                  Probar descarga ↗
                </a>
              </div>
            </div>
          </fieldset>

          <!-- SECCIÓN ESPECÍFICA: PRODUCTO FÍSICO (INVENTARIO) -->
          <fieldset v-if="draft.type === 'PHYSICAL'">
            <legend>📦 Inventario y Almacén Físico</legend>
            <div class="form-grid three">
              <label class="check-row">
                <input v-model="draft.trackInventory" type="checkbox">
                Controlar existencia de inventario
              </label>
              <label class="check-row">
                <input v-model="draft.allowBackorder" type="checkbox">
                Permitir venta sin stock (Sobreventa)
              </label>
              <label class="check-row">
                <input v-model="draft.featured" type="checkbox">
                Producto destacado
              </label>
              <label>Existencia actual (Unidades)
                <input v-model.number="draft.stockQuantity" type="number" min="0">
              </label>
              <label>Alerta de inventario bajo (Mínimo)
                <input v-model.number="draft.lowStockThreshold" type="number" min="0">
              </label>
              <label>Peso estimado (Gramos)
                <input v-model.number="draft.weightGrams" type="number" min="0">
              </label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Precio e Impuestos</legend>
            <div class="form-grid three">
              <label>Precio de venta<input v-model.number="draft.price" type="number" min="0" step="0.01" required></label>
              <label>Precio comparativo (Antes)<input v-model.number="draft.compareAtPrice" type="number" min="0" step="0.01"></label>
              <label>Moneda<input v-model="draft.currency" maxlength="3" required></label>
              <label class="check-row"><input v-model="draft.taxable" type="checkbox">Producto sujeto a impuesto</label>
              <label>Clase fiscal
                <select v-model="draft.taxClass">
                  <option value="STANDARD">Estándar</option>
                  <option value="REDUCED">Reducida</option>
                  <option value="ZERO">Tasa cero</option>
                  <option value="EXEMPT">Exento</option>
                </select>
              </label>
              <label>Tasa específica (%)<input v-model.number="draft.taxRateOverride" type="number" min="0" max="100" step="0.01" placeholder="Usar tasa general"></label>
            </div>
          </fieldset>

          <fieldset>
            <legend>Variantes de Producto</legend>
            <div class="variant-editor-list">
              <article v-for="(variant, index) in draft.variants" :key="index">
                <div class="form-grid three">
                  <label>Nombre<input v-model="variant.name" required></label>
                  <label>SKU<input v-model="variant.sku" required></label>
                  <label>Precio<input v-model.number="variant.price" type="number" min="0" step="0.01"></label>
                  <label>Existencia<input v-model.number="variant.stockQuantity" type="number" min="0"></label>
                  <label>Opciones<input v-model="variant.optionValues" placeholder="color=negro;talla=M"></label>
                  <label>Imagen<input v-model="variant.imageUrl" type="url"></label>
                </div>
                <button type="button" class="text-link danger" @click="removeVariant(index)">
                  Eliminar variante
                </button>
              </article>
              <button type="button" class="button secondary" @click="addVariant">
                + Agregar variante
              </button>
            </div>
          </fieldset>
        </div>

        <footer>
          <button type="button" class="button secondary" @click="editor = false">Cancelar</button>
          <button class="button" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar producto' }}</button>
        </footer>
      </form>
    </div>

    <!-- DRAWER: ADJUST INVENTORY -->
    <div v-if="inventoryEditor" class="app-drawer-backdrop" @click.self="inventoryEditor = false">
      <form class="app-drawer" @submit.prevent="saveAdjustment">
        <header>
          <div>
            <span class="eyebrow">MOVIMIENTO DE STOCK</span>
            <h2>Ajustar inventario físico</h2>
            <p>Registre una entrada, salida, corrección o pérdida con trazabilidad.</p>
          </div>
          <button type="button" class="icon-button" @click="inventoryEditor = false">×</button>
        </header>

        <div class="app-drawer__body">
          <label>Producto físico
            <select v-model="adjustment.productId" required @change="adjustment.variantId = ''">
              <option value="" disabled>Seleccione producto físico</option>
              <option v-for="product in products.filter(p => p.type === 'PHYSICAL')" :key="product.id" :value="product.id">
                {{ product.name }} (Actual: {{ product.stockQuantity }})
              </option>
            </select>
          </label>

          <label v-if="inventoryVariants.length">Variante
            <select v-model="adjustment.variantId">
              <option value="">Producto principal</option>
              <option v-for="variant in inventoryVariants" :key="variant.id" :value="variant.id">
                {{ variant.name }} (Actual: {{ variant.stockQuantity }})
              </option>
            </select>
          </label>

          <label>Tipo de movimiento
            <select v-model="adjustment.type">
              <option value="ADD">🟢 Entrada (+ stock)</option>
              <option value="REMOVE">🔴 Salida (- stock)</option>
              <option value="SET">🔵 Establecer cantidad exacta</option>
              <option value="DAMAGE">⚠️ Pérdida o daño (- stock)</option>
              <option value="RETURN">↩️ Devolución (+ stock)</option>
            </select>
          </label>

          <label>Cantidad
            <input v-model.number="adjustment.quantity" type="number" min="0" required>
          </label>

          <label>Motivo o justificación
            <textarea v-model="adjustment.reason" rows="3" required placeholder="Ej. Recibo de proveedor, inventario físico anual, merma, etc."></textarea>
          </label>

          <div v-if="inventoryProduct" class="inventory-preview-box">
            <div class="preview-line">
              <span>Stock actual:</span>
              <strong>{{ inventoryProduct.stockQuantity }} unidades</strong>
            </div>
            <div class="preview-line highlight">
              <span>Nuevo stock proyectado:</span>
              <strong :class="{ danger: projectedStock <= 0, warning: projectedStock > 0 && projectedStock <= inventoryProduct.lowStockThreshold }">
                {{ projectedStock }} unidades
              </strong>
            </div>
            <small class="preview-sku">SKU: {{ inventoryProduct.sku }}</small>
          </div>
        </div>

        <footer>
          <button type="button" class="button secondary" @click="inventoryEditor = false">
            Cancelar
          </button>
          <button class="button" :disabled="saving">
            {{ saving ? 'Registrando…' : 'Registrar movimiento' }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>
