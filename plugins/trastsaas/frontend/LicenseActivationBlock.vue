<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '@/services/api'

const props = withDefaults(defineProps<{
  productKey?: string
  editing?: boolean
}>(), {
  productKey: 'trast-cloud',
  editing: false
})

const loading = ref(false)
const result = ref<any>(null)
const error = ref('')
const form = reactive({
  licenseKey: '',
  fingerprint: '',
  deviceName: '',
  platform: 'web',
  applicationVersion: '1.0.0'
})

async function activate() {
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await api('/api/public/saas/licenses/activate', {
      method: 'POST',
      body: JSON.stringify({
        ...form,
        productKey: props.productKey,
        fingerprint: form.fingerprint || crypto.randomUUID()
      })
    })
  } catch (exception: any) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="license-activation-block">
    <div v-if="editing" class="plugin-preview-card">
      <span class="eyebrow">TRASTSAAS</span>
      <h3>Activación de licencia</h3>
      <p>Los clientes podrán validar su clave y registrar un dispositivo.</p>
    </div>
    <form v-else class="form-stack" @submit.prevent="activate">
      <div>
        <span class="eyebrow">LICENCIAS</span>
        <h3 class="text-2xl font-bold">Activar este dispositivo</h3>
      </div>
      <label>Clave de licencia<input v-model="form.licenseKey" required placeholder="TRAST-..."></label>
      <label>Nombre del dispositivo<input v-model="form.deviceName" placeholder="Equipo administrativo"></label>
      <label>Identificador del dispositivo<input v-model="form.fingerprint" placeholder="Se generará uno temporal si se deja vacío"></label>
      <p v-if="error" class="form-error">{{ error }}</p>
      <div v-if="result" class="form-success">
        <strong>{{ result.message }}</strong>
        <span>Estado: {{ result.status }}</span>
      </div>
      <button class="button full" :disabled="loading">
        {{ loading ? 'Validando…' : 'Activar licencia' }}
      </button>
    </form>
  </section>
</template>
