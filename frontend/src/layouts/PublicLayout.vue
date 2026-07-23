<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { SiteInfo, ThemeMenuItem, ThemeMenus } from '@/types'

const auth = useAuthStore()
const router = useRouter()
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

const headerMenu = computed(() => menus.value.header.filter(item => item.visible))
const footerMenu = computed(() => menus.value.footer.filter(item => item.visible))
const currentYear = new Date().getFullYear()

onMounted(async () => {
  const [siteInfo, menuInfo] = await Promise.all([
    api<SiteInfo>('/api/public/site'),
    api<ThemeMenus>('/api/public/menus')
  ])
  site.value = siteInfo
  menus.value = menuInfo
  if (!auth.loaded) await auth.load().catch(() => undefined)
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
</script>

<template>
  <div class="public-shell" :class="{ 'has-admin-bar': auth.user.authenticated && site.showAdminBar }">
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

    <header class="public-header">
      <RouterLink to="/" class="brand">
        <img v-if="site.logoUrl" :src="site.logoUrl" :alt="site.name" class="site-logo">
        <span v-else class="brand-mark">T</span>
        <span>{{ site.name }}</span>
      </RouterLink>
      <nav class="public-navigation" aria-label="Navegación principal">
        <template v-for="item in headerMenu" :key="item.id">
          <RouterLink v-if="internal(item)" :to="menuUrl(item)" :target="item.newTab ? '_blank' : undefined">
            {{ item.label }}
          </RouterLink>
          <a v-else :href="menuUrl(item)" :target="item.newTab ? '_blank' : undefined" :rel="item.newTab ? 'noopener noreferrer' : undefined">
            {{ item.label }}
          </a>
        </template>
        <RouterLink v-if="!auth.user.authenticated" to="/admin" class="public-login-link">Administrar</RouterLink>
      </nav>
    </header>

    <main><RouterView :site="site" /></main>

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
