<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const email = ref('admin@trastcms.local')
const password = ref('')
const error = ref('')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function submit() {
  loading.value = true; error.value = ''
  try {
    await auth.login(email.value, password.value)
    await router.push(String(route.query.redirect || '/admin'))
  } catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo iniciar sesión' }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <RouterLink to="/" class="admin-brand center"><span class="brand-mark">T</span><span>TrastCMS</span></RouterLink>
      <div><span class="eyebrow">ADMINISTRACIÓN</span><h1>Bienvenido</h1><p>Administre su sitio desde una interfaz rápida y sencilla.</p></div>
      <form @submit.prevent="submit" class="form-stack">
        <label>Correo<input v-model="email" type="email" required autocomplete="username"></label>
        <label>Contraseña<input v-model="password" type="password" required autocomplete="current-password"></label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button full" :disabled="loading">{{ loading ? 'Ingresando…' : 'Iniciar sesión' }}</button>
      </form>
      <small>La contraseña inicial se define con <code>TRASTCMS_ADMIN_PASSWORD</code>.</small>
    </section>
  </main>
</template>
