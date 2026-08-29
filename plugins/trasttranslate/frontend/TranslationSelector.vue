<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  changeLanguage,
  currentLanguage,
  defaultTranslationSettings,
  initializeTranslation,
  languageCatalog,
  loadTranslationSettings,
  translationAllowed,
  type LanguageOption,
  type TranslationDisplayStyle,
  type TranslationSettings
} from './translation'

const props = withDefaults(defineProps<{
  settings?: TranslationSettings | null
  styleOverride?: string
  label?: string
}>(), {
  settings: null,
  styleOverride: 'inherit',
  label: ''
})

const resolved = ref<TranslationSettings>(props.settings || { ...defaultTranslationSettings })
const selected = ref('en')
const busy = ref(false)

const style = computed<TranslationDisplayStyle>(() => {
  const override = props.styleOverride.trim().toUpperCase()
  if (['COMPACT', 'DROPDOWN', 'BUTTONS'].includes(override)) return override as TranslationDisplayStyle
  return resolved.value.displayStyle
})
const label = computed(() => props.label || resolved.value.buttonLabel || 'Language')
const languages = computed(() => resolved.value.enabledLanguages.map(code => {
  const item = languageCatalog.find(language => language.code.toLowerCase() === code.toLowerCase())
  return item || { code, nativeName: code.toUpperCase(), englishName: code.toUpperCase(), flag: '🌐' }
}))
const visible = computed(() => translationAllowed(resolved.value) && languages.value.length > 1)

function languageName(language: LanguageOption) {
  return resolved.value.showNativeNames ? language.nativeName : language.englishName
}

async function selectLanguage(code: string) {
  if (busy.value || code === selected.value) return
  busy.value = true
  selected.value = code
  try {
    await changeLanguage(resolved.value, code)
  } finally {
    busy.value = false
  }
}

async function boot() {
  resolved.value = props.settings || await loadTranslationSettings()
  selected.value = currentLanguage(resolved.value)
  await initializeTranslation(resolved.value)
}

watch(() => props.settings, value => {
  if (!value) return
  resolved.value = value
  selected.value = currentLanguage(value)
  void initializeTranslation(value)
}, { deep: true })

onMounted(boot)
</script>

<template>
  <div v-if="visible" :class="['trast-translate notranslate', `is-${style.toLowerCase()}`]" translate="no">
    <span v-if="style !== 'COMPACT'" class="trast-translate__label">{{ label }}</span>

    <div v-if="style === 'BUTTONS'" class="trast-translate__buttons" role="group" :aria-label="label">
      <button
        v-for="language in languages"
        :key="language.code"
        type="button"
        :class="{ active: selected === language.code }"
        :disabled="busy"
        @click="selectLanguage(language.code)"
      >
        <span v-if="resolved.showFlags" aria-hidden="true">{{ language.flag }}</span>
        <span>{{ languageName(language) }}</span>
      </button>
    </div>

    <label v-else class="trast-translate__select-wrap">
      <span v-if="style === 'COMPACT'" class="sr-only">{{ label }}</span>
      <span v-if="resolved.showFlags" class="trast-translate__current-flag" aria-hidden="true">{{ languages.find(item => item.code === selected)?.flag || '🌐' }}</span>
      <select :value="selected" :disabled="busy" :aria-label="label" @change="selectLanguage(($event.target as HTMLSelectElement).value)">
        <option v-for="language in languages" :key="language.code" :value="language.code">
          {{ resolved.showFlags ? `${language.flag} ` : '' }}{{ languageName(language) }}
        </option>
      </select>
    </label>

    <small v-if="style === 'DROPDOWN'" class="trast-translate__powered">Powered by Google Translate</small>
  </div>
</template>

<style>
/* Keep Google's legacy banner from shifting the TrastCMS layout. */
html{top:0!important}
body{top:0!important}
.goog-te-banner-frame.skiptranslate,iframe.goog-te-banner-frame,iframe.VIpgJd-ZVi9od-ORHb-OEVmcd{display:none!important;visibility:hidden!important}
#goog-gt-tt,.goog-te-balloon-frame{display:none!important}
.goog-text-highlight{background:transparent!important;box-shadow:none!important}
.trast-google-element-host{position:fixed!important;left:-9999px!important;top:-9999px!important;width:1px!important;height:1px!important;overflow:hidden!important}
.trast-translate{--translate-accent:var(--primary,#6d4aff);display:inline-flex;align-items:center;gap:.65rem;font:inherit;color:inherit;max-width:100%}
.trast-translate__label{font-size:.75rem;font-weight:800;text-transform:uppercase;letter-spacing:.08em;opacity:.7}
.trast-translate__select-wrap{position:relative;display:inline-flex;align-items:center;min-width:9rem}
.trast-translate__current-flag{position:absolute;left:.7rem;z-index:1;pointer-events:none}
.trast-translate select{width:100%;min-height:2.5rem;border:1px solid color-mix(in srgb,currentColor 18%,transparent);border-radius:.75rem;background:var(--surface,#fff);color:var(--text,#111827);padding:.55rem 2rem .55rem .8rem;font:inherit;font-size:.85rem;font-weight:700;cursor:pointer}
.trast-translate__current-flag+select{padding-left:2.25rem}
.trast-translate.is-compact select{min-width:7.5rem;min-height:2.25rem;border-radius:999px;background:color-mix(in srgb,var(--surface,#fff) 92%,transparent)}
.trast-translate.is-dropdown{display:grid;gap:.35rem;align-items:start}
.trast-translate__powered{font-size:.62rem;opacity:.58}
.trast-translate__buttons{display:flex;flex-wrap:wrap;gap:.4rem}
.trast-translate__buttons button{display:inline-flex;align-items:center;gap:.35rem;border:1px solid color-mix(in srgb,currentColor 16%,transparent);border-radius:999px;background:var(--surface,#fff);color:inherit;padding:.45rem .7rem;font:inherit;font-size:.78rem;font-weight:750;cursor:pointer}
.trast-translate__buttons button.active{background:var(--translate-accent);border-color:var(--translate-accent);color:#fff}
.trast-translate__buttons button:disabled,.trast-translate select:disabled{cursor:wait;opacity:.65}
.trast-translate-placement.is-floating-left,.trast-translate-placement.is-floating-right{position:fixed;z-index:1300;max-width:calc(100vw - 2rem)}
.trast-translate-placement.is-floating-left{left:var(--translate-offset-x,24px);bottom:var(--translate-offset-y,20px)}
.trast-translate-placement.is-floating-right{right:var(--translate-offset-x,24px);bottom:var(--translate-offset-y,20px)}
.trast-translate-placement.is-floating-left .trast-translate,.trast-translate-placement.is-floating-right .trast-translate{padding:.55rem;border-radius:1rem;background:var(--surface,#fff);box-shadow:0 16px 44px rgba(15,23,42,.18)}
@media(max-width:720px){.trast-translate__buttons{max-width:min(92vw,24rem)}.trast-translate-placement.is-floating-left,.trast-translate-placement.is-floating-right{bottom:20px}.trast-translate.is-dropdown .trast-translate__powered{display:none}}
</style>
