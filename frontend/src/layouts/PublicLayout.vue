<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { SiteInfo, ThemeMenuItem, ThemeMenus } from '@/types'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const route = useRoute()

const isMobileNavOpen = ref(false)

const site = ref<SiteInfo>({
  name: 'TrastCMS',
  tagline: '',
  description: '',
  locale: 'es-HN',
  activeTheme: 'aurora',
  logoUrl: '',
  homePageSlug: 'inicio',
  showAdminBar: true,
  contactEmail: '',
  contactPhone: ''
})
const menus = ref<ThemeMenus>({ header: [], footer: [] })
const pluginStatus = ref<Record<string, boolean>>({})

const headerMenu = computed(() => menus.value.header.filter(item => item.visible))
const footerMenu = computed(() => menus.value.footer.filter(item => item.visible))
const currentYear = new Date().getFullYear()

onMounted(async () => {
  const [siteInfo, menuInfo, plugins] = await Promise.all([
    api<SiteInfo>('/api/public/site').catch(() => site.value),
    api<ThemeMenus>('/api/public/menus').catch(() => menus.value),
    api<Record<string, boolean>>('/api/public/plugins').catch(() => ({}))
  ])
  site.value = siteInfo
  menus.value = menuInfo
  pluginStatus.value = plugins
  if (!auth.loaded) await auth.load().catch(() => undefined)
  if (cart.token) await cart.refresh().catch(() => undefined)
})

watch(() => site.value.activeTheme, (theme) => {
  document.documentElement.dataset.theme = theme
  let link = document.querySelector<HTMLLinkElement>('#trastcms-theme')
  if (!link) {
    link = document.createElement('link')
    link.id = 'trastcms-theme'
    link.rel = 'stylesheet'
    document.head.appendChild(link)
  }
  link.href = `/api/public/themes/${encodeURIComponent(theme)}/tokens.css`
}, { immediate: true })

watch(() => route.path, () => {
  isMobileNavOpen.value = false
})

onUnmounted(() => document.querySelector('#trastcms-theme')?.remove())

async function signOut() {
  await auth.logout()
  await router.push('/')
}

function menuUrl(item: ThemeMenuItem) {
  if (item.type === 'CUSTOM') return item.url || '#'
  return item.target === site.value.homePageSlug ? '/' : `/page/${item.target}`
}

function internal(item: ThemeMenuItem) {
  const url = menuUrl(item)
  return item.type === 'PAGE' || url.startsWith('/') || url.startsWith('#')
}

function toggleMobileNav() {
  isMobileNavOpen.value = !isMobileNavOpen.value
}
</script>

<template>
  <div class="public-shell" :class="{ 'has-admin-bar': auth.user.authenticated && site.showAdminBar, 'mobile-nav-active': isMobileNavOpen }">
    <!-- Admin Bar -->
    <div v-if="auth.user.authenticated && site.showAdminBar" class="public-admin-bar">
      <div class="public-admin-bar__inner">
        <RouterLink to="/admin" class="public-admin-bar__brand">
          <span class="brand-mark small">T</span>
          <strong>{{ site.name }}</strong>
        </RouterLink>
        <nav>
          <RouterLink :to="{ name: 'dashboard' }">Panel</RouterLink>
          <RouterLink :to="{ name: 'page-new' }">Nueva página</RouterLink>
          <RouterLink :to="{ name: 'post-new' }">Nueva publicación</RouterLink>
          <RouterLink :to="{ name: 'themes' }">Apariencia</RouterLink>
        </nav>
        <div class="public-admin-bar__user">
          <RouterLink :to="{ name: 'account' }">{{ auth.user.displayName }}</RouterLink>
          <span>{{ auth.user.role }}</span>
          <button type="button" @click="signOut">Salir</button>
        </div>
      </div>
    </div>

    <!-- Desktop & Mobile Main Header -->
    <header class="public-header">
      <RouterLink to="/" class="brand">
        <img v-if="site.logoUrl" :src="site.logoUrl" :alt="site.name" class="site-logo">
        <span v-else class="brand-mark">T</span>
        <span>{{ site.name }}</span>
      </RouterLink>

      <!-- Desktop Navigation -->
      <nav class="public-navigation desktop-only" aria-label="Navegación principal">
        <template v-for="item in headerMenu" :key="item.id">
          <RouterLink v-if="internal(item)" :to="menuUrl(item)" :target="item.newTab ? '_blank' : undefined">
            {{ item.label }}
          </RouterLink>
          <a v-else :href="menuUrl(item)" :target="item.newTab ? '_blank' : undefined" :rel="item.newTab ? 'noopener noreferrer' : undefined">
            {{ item.label }}
          </a>
        </template>
        <RouterLink v-if="pluginStatus.traststore !== false" to="/store">Tienda</RouterLink>
        <RouterLink v-if="pluginStatus.trastsaas !== false" to="/pricing">Precios</RouterLink>
        <RouterLink to="/cart" class="cart-link">
          Carrito
          <span v-if="cart.count" class="cart-badge-pill">{{ cart.count }}</span>
        </RouterLink>
        <RouterLink v-if="!auth.user.authenticated" to="/admin" class="public-login-link">Administrar</RouterLink>
      </nav>

      <!-- Mobile Menu Toggle Button -->
      <button
        type="button"
        class="mobile-hamburger-btn mobile-only"
        :aria-expanded="isMobileNavOpen"
        aria-label="Abrir menú de navegación"
        @click="toggleMobileNav"
      >
        <svg v-if="!isMobileNavOpen" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
        <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </header>

    <!-- Mobile Navigation Side Drawer (Sidenav) -->
    <div
      class="mobile-nav-backdrop"
      :class="{ open: isMobileNavOpen }"
      @click="isMobileNavOpen = false"
    />
    <aside class="mobile-sidenav" :class="{ open: isMobileNavOpen }">
      <div class="mobile-sidenav__header">
        <RouterLink to="/" class="brand" @click="isMobileNavOpen = false">
          <img v-if="site.logoUrl" :src="site.logoUrl" :alt="site.name" class="site-logo">
          <span v-else class="brand-mark">T</span>
          <span>{{ site.name }}</span>
        </RouterLink>
        <button type="button" class="mobile-sidenav__close" aria-label="Cerrar menú" @click="isMobileNavOpen = false">
          ✕
        </button>
      </div>

      <nav class="mobile-sidenav__menu">
        <span class="mobile-sidenav__section-title">Navegación</span>
        <template v-for="item in headerMenu" :key="item.id">
          <RouterLink v-if="internal(item)" :to="menuUrl(item)" class="mobile-sidenav__item">
            {{ item.label }}
          </RouterLink>
          <a v-else :href="menuUrl(item)" class="mobile-sidenav__item">
            {{ item.label }}
          </a>
        </template>

        <div class="mobile-sidenav__divider" />
        <span class="mobile-sidenav__section-title">Secciones</span>

        <RouterLink to="/store" class="mobile-sidenav__item">
          <span>🛍️ Tienda</span>
        </RouterLink>
        <RouterLink to="/pricing" class="mobile-sidenav__item">
          <span>🏷️ Precios y SaaS</span>
        </RouterLink>
        <RouterLink to="/cart" class="mobile-sidenav__item">
          <span>🛒 Carrito</span>
          <span v-if="cart.count" class="cart-badge-pill">{{ cart.count }}</span>
        </RouterLink>
        <RouterLink v-if="!auth.user.authenticated" to="/admin" class="mobile-sidenav__item highlight">
          <span>⚡ Administrar CMS</span>
        </RouterLink>
      </nav>

      <div class="mobile-sidenav__footer">
        <p>{{ site.tagline || 'Plataforma CMS con comercio y SaaS' }}</p>
        <small>© {{ currentYear }} {{ site.name }}</small>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main><RouterView :site="site" /></main>

    <!-- Mobile Bottom Navigation Bar -->
    <nav class="mobile-bottom-bar" aria-label="Navegación rápida inferior">
      <RouterLink to="/" class="mobile-bottom-bar__item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
        <span>Inicio</span>
      </RouterLink>

      <RouterLink to="/store" class="mobile-bottom-bar__item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <path d="M16 10a4 4 0 0 1-8 0"></path>
        </svg>
        <span>Tienda</span>
      </RouterLink>

      <RouterLink to="/pricing" class="mobile-bottom-bar__item">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="1" x2="12" y2="23"></line>
          <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
        </svg>
        <span>Precios</span>
      </RouterLink>

      <RouterLink to="/cart" class="mobile-bottom-bar__item cart-bottom-item">
        <div class="cart-icon-wrapper">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
          <span v-if="cart.count" class="bottom-bar-badge">{{ cart.count }}</span>
        </div>
        <span>Carrito</span>
      </RouterLink>

      <button type="button" class="mobile-bottom-bar__item" @click="toggleMobileNav">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
        <span>Menú</span>
      </button>
    </nav>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="site-footer__grid">
        <section class="site-footer__brand">
          <RouterLink to="/" class="brand">
            <img v-if="site.logoUrl" :src="site.logoUrl" :alt="site.name" class="site-logo">
            <span v-else class="brand-mark">T</span>
            <span>{{ site.name }}</span>
          </RouterLink>
          <p>{{ site.description || site.tagline || 'Un CMS open source para construir sitios rápidos, mantenibles y extensibles con Spring Boot y Vue.' }}</p>
          <span class="site-footer__technology">Spring Boot · Vue · Temas · Plugins</span>
        </section>

        <section>
          <h2>Navegación</h2>
          <nav aria-label="Navegación del pie de página">
            <template v-for="item in footerMenu" :key="item.id">
              <RouterLink v-if="internal(item)" :to="menuUrl(item)" :target="item.newTab ? '_blank' : undefined">{{ item.label }}</RouterLink>
              <a v-else :href="menuUrl(item)" :target="item.newTab ? '_blank' : undefined" :rel="item.newTab ? 'noopener noreferrer' : undefined">{{ item.label }}</a>
            </template>
          </nav>
        </section>

        <section>
          <h2>Contacto</h2>
          <a v-if="site.contactEmail" :href="`mailto:${site.contactEmail}`">{{ site.contactEmail }}</a>
          <a v-if="site.contactPhone" :href="`tel:${site.contactPhone}`">{{ site.contactPhone }}</a>
          <p v-if="!site.contactEmail && !site.contactPhone">Configure los datos de contacto desde el panel administrativo.</p>
          <RouterLink to="/page/contactanos" class="site-footer__cta">Iniciar una conversación</RouterLink>
          <RouterLink to="/developers" class="site-footer__developer-link">Documentación para desarrolladores →</RouterLink>
        </section>
      </div>
      <div class="site-footer__bottom">
        <span>© {{ currentYear }} {{ site.name }}. Contenido bajo su control.</span>
        <span>Impulsado por <strong>TrastCMS</strong></span>
      </div>
    </footer>
  </div>
</template>
