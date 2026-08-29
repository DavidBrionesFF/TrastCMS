import { ref } from 'vue'
import { api } from '@/services/api'

export type TranslationPlacement = 'TOPBAR' | 'HEADER' | 'FOOTER' | 'FLOATING_LEFT' | 'FLOATING_RIGHT' | 'BLOCK_ONLY'
export type TranslationDisplayStyle = 'COMPACT' | 'DROPDOWN' | 'BUTTONS'
export type TranslationProvider = 'GOOGLE_ELEMENT' | 'GOOGLE_REDIRECT'

export type TranslationSettings = {
  enabled: boolean
  provider: TranslationProvider
  sourceLanguage: string
  enabledLanguages: string[]
  placement: TranslationPlacement
  displayStyle: TranslationDisplayStyle
  showFlags: boolean
  showNativeNames: boolean
  rememberSelection: boolean
  buttonLabel: string
  floatingOffsetX: number
  floatingOffsetY: number
  excludedPaths: string[]
  updatedAt?: string
}

export const defaultTranslationSettings: TranslationSettings = {
  enabled: true,
  provider: 'GOOGLE_ELEMENT',
  sourceLanguage: 'en',
  enabledLanguages: ['en', 'es'],
  placement: 'TOPBAR',
  displayStyle: 'COMPACT',
  showFlags: true,
  showNativeNames: true,
  rememberSelection: true,
  buttonLabel: 'Language',
  floatingOffsetX: 24,
  floatingOffsetY: 20,
  excludedPaths: ['/admin', '/api']
}

export type LanguageOption = { code: string; nativeName: string; englishName: string; flag: string }

export const languageCatalog: readonly LanguageOption[] = [
  { code: 'en', nativeName: 'English', englishName: 'English', flag: '🇺🇸' },
  { code: 'es', nativeName: 'Español', englishName: 'Spanish', flag: '🇭🇳' },
  { code: 'fr', nativeName: 'Français', englishName: 'French', flag: '🇫🇷' },
  { code: 'de', nativeName: 'Deutsch', englishName: 'German', flag: '🇩🇪' },
  { code: 'it', nativeName: 'Italiano', englishName: 'Italian', flag: '🇮🇹' },
  { code: 'pt', nativeName: 'Português', englishName: 'Portuguese', flag: '🇧🇷' },
  { code: 'nl', nativeName: 'Nederlands', englishName: 'Dutch', flag: '🇳🇱' },
  { code: 'ja', nativeName: '日本語', englishName: 'Japanese', flag: '🇯🇵' },
  { code: 'ko', nativeName: '한국어', englishName: 'Korean', flag: '🇰🇷' },
  { code: 'zh-CN', nativeName: '简体中文', englishName: 'Chinese', flag: '🇨🇳' },
  { code: 'ar', nativeName: 'العربية', englishName: 'Arabic', flag: '🇸🇦' },
  { code: 'ru', nativeName: 'Русский', englishName: 'Russian', flag: '🇷🇺' },
  { code: 'pl', nativeName: 'Polski', englishName: 'Polish', flag: '🇵🇱' },
  { code: 'sv', nativeName: 'Svenska', englishName: 'Swedish', flag: '🇸🇪' },
  { code: 'da', nativeName: 'Dansk', englishName: 'Danish', flag: '🇩🇰' },
  { code: 'no', nativeName: 'Norsk', englishName: 'Norwegian', flag: '🇳🇴' },
  { code: 'tr', nativeName: 'Türkçe', englishName: 'Turkish', flag: '🇹🇷' },
  { code: 'he', nativeName: 'עברית', englishName: 'Hebrew', flag: '🇮🇱' }
]

const currentSettings = ref<TranslationSettings>({ ...defaultTranslationSettings })
let settingsPromise: Promise<TranslationSettings> | null = null
let googleInitialization: Promise<void> | null = null

export function useTranslationSettings() {
  return currentSettings
}

export async function loadTranslationSettings(force = false): Promise<TranslationSettings> {
  if (!force && settingsPromise) return settingsPromise
  settingsPromise = api<TranslationSettings>('/api/public/translation/settings')
    .then(settings => {
      currentSettings.value = normalizeSettings(settings)
      document.documentElement.lang = currentSettings.value.sourceLanguage
      return currentSettings.value
    })
    .catch(() => {
      currentSettings.value = { ...defaultTranslationSettings }
      return currentSettings.value
    })
  return settingsPromise
}

export function normalizeSettings(settings: TranslationSettings): TranslationSettings {
  const languages = Array.from(new Set([
    settings.sourceLanguage || 'en',
    ...(settings.enabledLanguages || [])
  ].filter(Boolean)))
  return {
    ...defaultTranslationSettings,
    ...settings,
    sourceLanguage: settings.sourceLanguage || 'en',
    enabledLanguages: languages,
    excludedPaths: settings.excludedPaths || []
  }
}

export function translationAllowed(settings: TranslationSettings, path = window.location.pathname): boolean {
  if (!settings.enabled) return false
  return !(settings.excludedPaths || []).some(prefix => prefix && path.startsWith(prefix))
}

export function currentLanguage(settings: TranslationSettings): string {
  const stored = settings.rememberSelection ? window.localStorage.getItem('trastcms.translation.language') : null
  if (stored && settings.enabledLanguages.includes(stored)) return stored
  const cookie = document.cookie.split(';').map(value => value.trim()).find(value => value.startsWith('googtrans='))
  const cookieValue = cookie ? decodeURIComponent(cookie.substring('googtrans='.length)) : ''
  const translated = cookieValue.split('/').filter(Boolean).at(-1)
  return translated && settings.enabledLanguages.includes(translated) ? translated : settings.sourceLanguage
}

export async function initializeTranslation(settings: TranslationSettings): Promise<void> {
  if (!translationAllowed(settings)) return
  if (settings.provider !== 'GOOGLE_ELEMENT') return
  if (googleInitialization) return googleInitialization

  googleInitialization = new Promise<void>(resolve => {
    const elementId = 'trast-google-translate-element'
    let host = document.getElementById(elementId)
    if (!host) {
      host = document.createElement('div')
      host.id = elementId
      host.className = 'trast-google-element-host'
      host.setAttribute('aria-hidden', 'true')
      document.body.appendChild(host)
    }

    const finish = () => {
      try {
        const googleObject = (window as any).google
        if (googleObject?.translate?.TranslateElement && !host?.dataset.initialized) {
          new googleObject.translate.TranslateElement({
            pageLanguage: settings.sourceLanguage,
            includedLanguages: settings.enabledLanguages.filter(code => code !== settings.sourceLanguage).join(','),
            autoDisplay: false
          }, elementId)
          if (host) host.dataset.initialized = 'true'
        }
      } catch {
        // Google controla la disponibilidad del elemento; el selector mantiene el modo redirect como alternativa.
      }
      resolve()
    }

    if ((window as any).google?.translate?.TranslateElement) {
      finish()
      return
    }

    ;(window as any).trastTranslateGoogleInit = finish
    const existing = document.querySelector<HTMLScriptElement>('script[data-trast-google-translate]')
    if (existing) {
      existing.addEventListener('load', finish, { once: true })
      window.setTimeout(finish, 1800)
      return
    }

    const script = document.createElement('script')
    script.src = 'https://translate.google.com/translate_a/element.js?cb=trastTranslateGoogleInit'
    script.async = true
    script.defer = true
    script.dataset.trastGoogleTranslate = 'true'
    script.addEventListener('error', () => resolve(), { once: true })
    document.head.appendChild(script)
    window.setTimeout(finish, 3500)
  })

  return googleInitialization
}

export async function changeLanguage(settings: TranslationSettings, targetLanguage: string): Promise<void> {
  const target = settings.enabledLanguages.includes(targetLanguage)
    ? targetLanguage
    : settings.sourceLanguage

  if (settings.rememberSelection) {
    window.localStorage.setItem('trastcms.translation.language', target)
  }

  if (settings.provider === 'GOOGLE_REDIRECT' && target !== settings.sourceLanguage) {
    const url = new URL('https://translate.google.com/translate')
    url.searchParams.set('sl', settings.sourceLanguage)
    url.searchParams.set('tl', target)
    url.searchParams.set('u', window.location.href)
    window.location.assign(url.toString())
    return
  }

  await initializeTranslation(settings)
  setGoogleCookie(settings.sourceLanguage, target, settings.rememberSelection)
  await new Promise(resolve => window.setTimeout(resolve, 160))

  const combo = document.querySelector<HTMLSelectElement>('.goog-te-combo')
  if (combo && target !== settings.sourceLanguage) {
    combo.value = target
    combo.dispatchEvent(new Event('change', { bubbles: true }))
    return
  }

  if (target !== settings.sourceLanguage && !(window as any).google?.translate?.TranslateElement) {
    const url = new URL('https://translate.google.com/translate')
    url.searchParams.set('sl', settings.sourceLanguage)
    url.searchParams.set('tl', target)
    url.searchParams.set('u', window.location.href)
    window.location.assign(url.toString())
    return
  }

  window.location.reload()
}

export async function refreshTranslation(settings: TranslationSettings): Promise<void> {
  if (!translationAllowed(settings) || settings.provider !== 'GOOGLE_ELEMENT') return
  const target = currentLanguage(settings)
  if (target === settings.sourceLanguage) return
  await initializeTranslation(settings)
  window.setTimeout(() => {
    const combo = document.querySelector<HTMLSelectElement>('.goog-te-combo')
    if (!combo) return
    combo.value = target
    combo.dispatchEvent(new Event('change', { bubbles: true }))
  }, 120)
}

function setGoogleCookie(source: string, target: string, remember: boolean) {
  const value = target === source ? '' : `/${source}/${target}`
  const expires = target === source ? '; expires=Thu, 01 Jan 1970 00:00:00 GMT' : (remember ? '; max-age=31536000' : '')
  document.cookie = `googtrans=${value}; path=/${expires}; SameSite=Lax`
  if (window.location.hostname && !['localhost', '127.0.0.1'].includes(window.location.hostname)) {
    document.cookie = `googtrans=${value}; path=/; domain=.${window.location.hostname}${expires}; SameSite=Lax`
  }
}
