<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'

interface ClaimedLicense {
  id: string
  status: string
  productKey: string
  planKey: string
  customerEmail: string
  licenseKey: string
}

interface ClaimedSubscription {
  id: string
  planName: string
  status: string
  currentPeriodEnd?: string | null
}

interface ClaimResponse {
  licenses: ClaimedLicense[]
  subscriptions: ClaimedSubscription[]
}

const route = useRoute()
const data = ref<ClaimResponse | null>(null)
const error = ref('')
const copiedLicenseId = ref<string | null>(null)

function queryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value.length > 0 && value[0] != null
      ? String(value[0])
      : ''
  }

  return value == null ? '' : String(value)
}

async function copyLicense(
  licenseId: string,
  licenseKey: string
): Promise<void> {
  error.value = ''

  try {
    if (!window.navigator.clipboard) {
      throw new Error('Clipboard API unavailable')
    }

    await window.navigator.clipboard.writeText(licenseKey)
    copiedLicenseId.value = licenseId

    window.setTimeout(() => {
      if (copiedLicenseId.value === licenseId) {
        copiedLicenseId.value = null
      }
    }, 1800)
  } catch {
    error.value =
      'No se pudo copiar automáticamente. Seleccione la clave y cópiela manualmente.'
  }
}

onMounted(async () => {
  const order = queryValue(route.query.order)
  const token = queryValue(route.query.token)

  if (!order || !token) {
    error.value =
      'El enlace para reclamar las licencias está incompleto o no es válido.'
    return
  }

  try {
    data.value = await api<ClaimResponse>(
      `/api/public/saas/orders/${encodeURIComponent(order)}/claim`
      + `?token=${encodeURIComponent(token)}`
    )
  } catch (caught: unknown) {
    error.value = caught instanceof Error
      ? caught.message
      : 'No se pudieron consultar las licencias de esta compra.'
  }
})
</script>

<template><main class="content-section"><div class="mx-auto max-w-4xl"><span class="eyebrow">PORTAL DEL CLIENTE</span><h1 class="text-5xl font-black">Licencias de su compra</h1><p v-if="error" class="form-error mt-6">{{error}}</p><div v-if="data" class="mt-8 space-y-5"><article v-for="l in data.licenses" :key="l.id" class="panel"><div class="flex flex-wrap justify-between gap-4"><div><span class="status-pill ok">{{l.status}}</span><h2 class="mt-3 text-2xl font-bold">{{l.productKey}} · {{l.planKey}}</h2><p>{{l.customerEmail}}</p></div><div class="license-key"><small>Clave de licencia</small><code>{{l.licenseKey}}</code><button class="button secondary small" @click="copyLicense(l.id, l.licenseKey)">{{ copiedLicenseId === l.id ? 'Copiada' : 'Copiar' }}</button></div></div></article><article v-for="s in data.subscriptions" :key="s.id" class="panel"><h2 class="text-xl font-bold">Suscripción {{s.planName}}</h2><p>Estado: {{s.status}} · Próximo periodo: {{s.currentPeriodEnd?new Date(s.currentPeriodEnd).toLocaleDateString():'perpetuo'}}</p></article></div></div></main></template>
