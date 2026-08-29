import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@fontsource-variable/outfit'
import 'plyr/dist/plyr.css'
import './styles.css'
import App from './App.vue'
import router from './router'

type ThemeSiteInfo = {
  activeTheme?: string
}

type PublicTranslationInfo = {
  sourceLanguage?: string
}

const ACTIVE_THEME_KEY = 'trastcms.activeTheme'
const DEFAULT_THEME = 'nexus'

function normalizeTheme(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : DEFAULT_THEME
}

async function resolveActiveTheme() {
  const cached = normalizeTheme(window.localStorage.getItem(ACTIVE_THEME_KEY))
  try {
    const response = await fetch('/api/public/site', {
      headers: { Accept: 'application/json' },
      credentials: 'same-origin'
    })
    if (!response.ok) return cached
    const site = await response.json() as ThemeSiteInfo
    return normalizeTheme(site.activeTheme || cached)
  } catch {
    return cached
  }
}

async function resolveSourceLanguage() {
  if (window.location.pathname.startsWith('/admin')) return document.documentElement.lang || 'es'
  try {
    const response = await fetch('/api/public/translation/settings', { headers: { Accept: 'application/json' }, credentials: 'same-origin' })
    if (!response.ok) return 'en'
    const settings = await response.json() as PublicTranslationInfo
    return settings.sourceLanguage?.trim() || 'en'
  } catch {
    return 'en'
  }
}

async function preloadTheme(theme: string) {
  const selected = normalizeTheme(theme)
  document.documentElement.dataset.theme = selected
  window.localStorage.setItem(ACTIVE_THEME_KEY, selected)

  let link = document.querySelector<HTMLLinkElement>('#trastcms-theme')
  if (!link) {
    link = document.createElement('link')
    link.id = 'trastcms-theme'
    link.rel = 'stylesheet'
    document.head.appendChild(link)
  }

  const href = `/api/public/themes/${encodeURIComponent(selected)}/tokens.css`
  if (link.getAttribute('href') === href && link.sheet) {
    document.documentElement.classList.add('trast-theme-ready')
    return
  }

  await new Promise<void>(resolve => {
    let settled = false
    const done = () => {
      if (settled) return
      settled = true
      link!.onload = null
      link!.onerror = null
      document.documentElement.classList.add('trast-theme-ready')
      resolve()
    }
    link!.onload = done
    link!.onerror = done
    link!.href = href
    window.setTimeout(done, 1800)
  })
}

async function bootstrap() {
  const [theme, sourceLanguage] = await Promise.all([resolveActiveTheme(), resolveSourceLanguage()])
  document.documentElement.lang = sourceLanguage
  await preloadTheme(theme)
  createApp(App)
    .use(createPinia())
    .use(router)
    .mount('#app')
}

void bootstrap()
