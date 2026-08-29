<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'
import type { CrmReputationPublicView } from '@/types'

const route = useRoute()
const data = ref<CrmReputationPublicView | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const feedback = ref('')
const feedbackSaved = ref(false)
const token = computed(() => String(route.params.token || ''))

onMounted(async () => {
  await load()
  const requested = Number(route.query.rating)
  if (Number.isInteger(requested) && requested >= 1 && requested <= 5 && data.value?.rating !== requested) {
    await rate(requested)
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await api<CrmReputationPublicView>(`/api/public/crm/reputation/${encodeURIComponent(token.value)}`)
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar la solicitud de valoración.'
  } finally {
    loading.value = false
  }
}

async function rate(value: number) {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    data.value = await api<CrmReputationPublicView>(`/api/public/crm/reputation/${encodeURIComponent(token.value)}/rating`, {
      method: 'POST',
      body: JSON.stringify({ rating: value })
    })
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo registrar la valoración.'
  } finally {
    saving.value = false
  }
}

async function saveFeedback() {
  if (!feedback.value.trim() || saving.value) return
  saving.value = true
  error.value = ''
  try {
    data.value = await api<CrmReputationPublicView>(`/api/public/crm/reputation/${encodeURIComponent(token.value)}/feedback`, {
      method: 'POST',
      body: JSON.stringify({ feedback: feedback.value.trim() })
    })
    feedbackSaved.value = true
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar el comentario.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="review-page">
    <section v-if="loading" class="review-card review-state">Cargando valoración…</section>
    <section v-else-if="error && !data" class="review-card review-state error">{{ error }}</section>
    <section v-else-if="data" class="review-card">
      <div class="review-brand">{{ data.siteName }}</div>
      <span class="review-kicker">Su experiencia importa</span>
      <h1>¿Cómo calificaría su experiencia?</h1>
      <p class="review-lead">{{ data.sourceLabel }}</p>

      <div class="review-stars" role="group" aria-label="Seleccione de 1 a 5 estrellas">
        <button v-for="star in 5" :key="star" type="button" :class="{ active: (data.rating || 0) >= star }" :disabled="saving" @click="rate(star)">
          <span>★</span><small>{{ star }}</small>
        </button>
      </div>

      <div v-if="data.rating" class="review-thanks">
        <strong>Gracias por su valoración de {{ data.rating }}/5.</strong>
        <p>{{ data.thankYouMessage }}</p>
      </div>

      <div v-if="data.rating" class="review-feedback">
        <label for="review-feedback">¿Desea contarnos algo más? <small>Este comentario queda dentro del CRM del negocio.</small></label>
        <textarea id="review-feedback" v-model="feedback" rows="4" maxlength="4000" placeholder="Cuéntenos qué le gustó o qué podríamos mejorar"></textarea>
        <button type="button" :disabled="saving || !feedback.trim()" @click="saveFeedback">{{ feedbackSaved ? 'Comentario guardado' : 'Enviar comentario' }}</button>
      </div>

      <div v-if="data.rating && data.channels.length" class="review-channels">
        <span>También puede compartir su opinión públicamente</span>
        <div>
          <a v-for="channel in data.channels" :key="channel.id" :href="channel.redirectUrl" rel="noopener noreferrer">{{ channel.label }} ↗</a>
        </div>
      </div>

      <p v-if="error" class="review-error">{{ error }}</p>
    </section>
  </main>
</template>

<style scoped>
.review-page{width:100%;min-height:70vh;display:grid;place-items:center;padding:clamp(2rem,6vw,5rem) 1rem;background:linear-gradient(180deg,color-mix(in srgb,var(--primary,#635bff) 7%,#fff),#fff)}
.review-card{width:min(760px,100%);border:1px solid var(--line,#e5e7eb);border-radius:1.5rem;background:var(--surface,#fff);padding:clamp(1.5rem,5vw,3.25rem);box-shadow:0 26px 70px rgba(15,23,42,.10);text-align:center;color:var(--text,#172033)}
.review-state{min-height:220px;display:grid;place-items:center}.review-state.error,.review-error{color:#b42318}.review-brand{font-size:.78rem;font-weight:850;color:var(--primary,#635bff);text-transform:uppercase;letter-spacing:.12em}.review-kicker{display:block;margin-top:1.4rem;color:var(--muted,#667085);font-size:.78rem;font-weight:760}.review-card h1{margin:.5rem auto 0;max-width:620px;font-size:clamp(2rem,5vw,3.5rem);line-height:1.05;letter-spacing:-.04em}.review-lead{margin:1rem auto 0;max-width:620px;color:var(--muted,#667085);font-size:1.05rem}.review-stars{display:flex;justify-content:center;gap:.55rem;flex-wrap:wrap;margin:2rem 0}.review-stars button{display:grid;place-items:center;gap:.2rem;width:76px;height:76px;border:1px solid #f0d58a;border-radius:1rem;background:#fffdf5;color:#d8a51a;cursor:pointer;transition:.18s}.review-stars button:hover,.review-stars button.active{transform:translateY(-2px);background:#fff5c8;border-color:#e9b72f;box-shadow:0 10px 24px rgba(217,164,30,.15)}.review-stars button span{font-size:2rem;line-height:1}.review-stars button small{font-size:.7rem;color:#8b6a12}.review-stars button:disabled{opacity:.6;cursor:wait}.review-thanks{margin-top:1.5rem;border-radius:1rem;background:color-mix(in srgb,var(--primary,#635bff) 7%,#fff);padding:1.2rem}.review-thanks strong{font-size:1rem}.review-thanks p{margin:.45rem 0 0;color:var(--muted,#667085);line-height:1.6}.review-feedback{display:grid;gap:.7rem;margin-top:1.5rem;text-align:left}.review-feedback label{font-weight:800}.review-feedback label small{display:block;margin-top:.2rem;color:var(--muted,#667085);font-weight:500}.review-feedback textarea{width:100%;resize:vertical;border:1px solid var(--line,#d0d5dd);border-radius:.9rem;background:#fff;padding:.9rem;font:inherit}.review-feedback button{justify-self:start;border:0;border-radius:.8rem;background:var(--primary,#635bff);color:#fff;padding:.8rem 1rem;font-weight:800;cursor:pointer}.review-feedback button:disabled{opacity:.55}.review-channels{margin-top:1.7rem;border-top:1px solid var(--line,#e5e7eb);padding-top:1.4rem}.review-channels>span{display:block;color:var(--muted,#667085);font-size:.8rem;font-weight:700}.review-channels>div{display:flex;justify-content:center;gap:.65rem;flex-wrap:wrap;margin-top:.9rem}.review-channels a{display:inline-flex;border:1px solid var(--line,#d0d5dd);border-radius:.8rem;padding:.75rem 1rem;color:var(--text,#172033);font-weight:800;text-decoration:none}.review-channels a:hover{border-color:var(--primary,#635bff);color:var(--primary,#635bff)}
@media(max-width:560px){.review-stars button{width:58px;height:64px}.review-card{border-radius:1.1rem}.review-channels>div{display:grid}.review-channels a{justify-content:center}}
</style>
