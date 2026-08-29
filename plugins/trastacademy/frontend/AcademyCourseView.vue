<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const course = ref<any>(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const message = ref('')
const previewLesson = ref<any>(null)
const totalLessons = computed(() => (course.value?.sections || []).reduce((sum: number, section: any) => sum + (section.lessons?.length || 0), 0))
const totalQuizzes = computed(() => (course.value?.sections || []).reduce((sum: number, section: any) => sum + (section.quizzes?.length || 0), 0))

onMounted(load)
watch(() => route.params.slug, load)
async function load() {
  loading.value = true; error.value = ''
  try { const payload = await api<any>(`/api/public/academy/courses/${encodeURIComponent(String(route.params.slug))}`); course.value = payload?.data || payload }
  catch (exception) { error.value = exception instanceof Error ? exception.message : 'Curso no encontrado' }
  finally { loading.value = false }
}
async function action() {
  if (!course.value) return
  if (course.value.enrollment) return router.push(`/academy/learn/${course.value.slug}`)
  busy.value = true; error.value = ''; message.value = ''
  try {
    if (course.value.accessType === 'FREE') {
      await api(`/api/account/academy/courses/${course.value.id}/enroll`, { method: 'POST' })
      await load(); router.push(`/academy/learn/${course.value.slug}`)
    } else if (course.value.accessType === 'PAID') {
      await cart.ensure(course.value.currency)
      await cart.add('academy', course.value.id, 1)
      router.push('/cart')
    } else message.value = 'Este curso requiere una invitación del administrador.'
  } catch (exception: any) {
    if (exception?.status === 401) router.push({ name: 'my-account', query: { redirect: route.fullPath } })
    else error.value = exception instanceof Error ? exception.message : 'No se pudo procesar la inscripción'
  } finally { busy.value = false }
}
function money(value: number, currency='USD'){return new Intl.NumberFormat('es-US',{style:'currency',currency}).format(value||0)}
function youtubeEmbed(url = '') {
  try {
    const parsed = new URL(url)
    const playlist = parsed.searchParams.get('list')
    let id = parsed.hostname.includes('youtu.be') ? parsed.pathname.slice(1) : parsed.searchParams.get('v')
    if (!id && parsed.pathname.includes('/embed/')) id = parsed.pathname.split('/embed/')[1]?.split('/')[0]
    if (!id && parsed.pathname.includes('/shorts/')) id = parsed.pathname.split('/shorts/')[1]?.split('/')[0]
    if (id) return `https://www.youtube-nocookie.com/embed/${id}?rel=0${playlist ? `&list=${encodeURIComponent(playlist)}` : ''}`
    if (playlist) {
      const requested = Number.parseInt(parsed.searchParams.get('index') || '1', 10)
      const zeroBasedIndex = Number.isFinite(requested) ? Math.max(0, requested - 1) : 0
      return `https://www.youtube-nocookie.com/embed/videoseries?list=${encodeURIComponent(playlist)}&index=${zeroBasedIndex}&rel=0`
    }
    return ''
  } catch { return '' }
}
function openPreview(lesson: any) {
  if (!lesson?.preview) return
  previewLesson.value = lesson
  requestAnimationFrame(() => document.querySelector('.preview-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
}
</script>

<template>
  <main class="course-page">
    <div v-if="loading" class="course-state">Cargando curso…</div>
    <div v-else-if="error && !course" class="course-state error">{{ error }}</div>
    <template v-else-if="course">
      <section class="course-hero">
        <div class="course-copy">
          <RouterLink to="/academy" class="back">← Volver a la academia</RouterLink>
          <div class="badges"><span class="badge">{{ course.category || 'General' }}</span><span class="badge secondary-badge">{{ course.level.replaceAll('_',' ') }}</span></div>
          <h1>{{ course.title }}</h1>
          <p>{{ course.excerpt }}</p>
          <div class="facts"><span>{{ course.sections?.length || 0 }} módulos</span><span>{{ totalLessons }} lecciones</span><span>{{ totalQuizzes }} evaluaciones</span><span>{{ course.estimatedMinutes || 0 }} minutos</span><span>{{ course.instructorName || 'Academy' }}</span></div>
        </div>
        <aside class="enroll-card">
          <img v-if="course.featuredImageUrl" :src="course.featuredImageUrl" :alt="course.title">
          <div class="enroll-body">
            <strong class="price">{{ course.accessType === 'FREE' ? 'Gratis' : course.accessType === 'PAID' ? money(course.price, course.currency) : 'Privado' }}</strong>
            <button :disabled="busy" @click="action">{{ course.enrollment ? 'Continuar curso' : course.accessType === 'PAID' ? 'Comprar curso' : 'Inscribirme ahora' }}</button>
            <p v-if="message" class="notice">{{ message }}</p><p v-if="error" class="error-text">{{ error }}</p>
          </div>
        </aside>
      </section>
      <section class="course-content">
        <article class="description" v-html="course.description" />
        <div class="curriculum">
          <section v-if="previewLesson" class="preview-panel">
            <div class="preview-heading"><div><span>VISTA PREVIA</span><h2>{{ previewLesson.title }}</h2></div><button type="button" @click="previewLesson=null">Cerrar</button></div>
            <div v-if="previewLesson.videoProvider === 'YOUTUBE' && youtubeEmbed(previewLesson.videoUrl)" class="preview-video"><iframe :src="youtubeEmbed(previewLesson.videoUrl)" :title="previewLesson.title" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen /></div>
            <video v-else-if="previewLesson.videoUrl" class="preview-video-file" :src="previewLesson.videoUrl" controls preload="metadata" />
            <article class="preview-content" v-html="previewLesson.content" />
          </section>
          <header><div><span>CONTENIDO</span><h2>Programa del curso</h2></div><b>{{ course.sections?.length || 0 }} secciones</b></header>
          <section v-for="section in course.sections" :key="section.id" class="section-card">
            <div class="section-title"><h3>{{ section.title }}</h3><span>{{ section.lessons.length + section.quizzes.length }} elementos</span></div>
            <div v-for="lesson in section.lessons" :key="lesson.id" class="item-row" :class="{ previewable: lesson.preview }" @click="openPreview(lesson)"><span>▶</span><div><strong>{{ lesson.title }}</strong><small>{{ lesson.durationSeconds ? `${Math.ceil(lesson.durationSeconds/60)} min` : 'Lección' }}</small></div><button v-if="lesson.preview" type="button" @click.stop="openPreview(lesson)">Ver lección</button></div>
            <div v-for="quiz in section.quizzes" :key="quiz.id" class="item-row"><span>✓</span><div><strong>{{ quiz.title }}</strong><small>{{ quiz.questions.length }} preguntas</small></div><em v-if="quiz.preview">Vista previa</em></div>
          </section>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.course-page{width:100%;max-width:none;margin:0;padding:clamp(2rem,4vw,4rem) clamp(1rem,3vw,2.5rem) clamp(3rem,5vw,5rem);color:var(--text,#172033)}.course-state{padding:5rem;text-align:center}.course-state.error,.error-text{color:#b42318}.course-hero{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:3rem;align-items:start}.course-copy{padding-top:2rem}.back{color:var(--primary,#5b4ee8);font-weight:800;text-decoration:none}.badges{display:flex;flex-wrap:wrap;gap:.5rem;margin-top:2rem}.badge{display:inline-flex;padding:.45rem .75rem;border-radius:999px;background:color-mix(in srgb,var(--primary,#5b4ee8) 12%,transparent);color:var(--primary,#5b4ee8);font-weight:800;font-size:.75rem}.secondary-badge{background:#eef2f7;color:#475569}.course-copy h1{font-size:clamp(2.5rem,5vw,4.6rem);line-height:1.02;margin:1rem 0}.course-copy>p{font-size:1.18rem;color:var(--muted,#64748b);line-height:1.7;max-width:none}.facts{display:flex;flex-wrap:wrap;gap:1.2rem;margin-top:1.5rem;font-weight:700}.enroll-card{border:1px solid var(--border,#e5e7eb);border-radius:1.4rem;overflow:hidden;box-shadow:0 22px 60px rgba(15,23,42,.12);position:sticky;top:2rem;background:var(--surface,#fff)}.enroll-card img{width:100%;aspect-ratio:16/9;object-fit:cover}.enroll-body{padding:1.4rem}.price{display:block;font-size:2rem;margin-bottom:1rem}.enroll-body button{width:100%;border:0;border-radius:.9rem;padding:1rem;background:var(--primary,#5b4ee8);color:#fff;font-weight:850;cursor:pointer}.enroll-body button:disabled{opacity:.6}.notice{color:var(--muted,#64748b)}.course-content{display:grid;grid-template-columns:minmax(0,.72fr) minmax(0,1.28fr);gap:3rem;margin-top:5rem;align-items:start}.description{line-height:1.8;min-width:0}.curriculum header{display:flex;justify-content:space-between;align-items:end;margin-bottom:1rem}.curriculum header span{font-size:.72rem;letter-spacing:.15em;font-weight:850;color:var(--primary,#5b4ee8)}.curriculum h2{margin:.3rem 0;font-size:2rem}.section-card{border:1px solid var(--border,#e5e7eb);border-radius:1rem;overflow:hidden;margin-bottom:1rem}.section-title{display:flex;justify-content:space-between;gap:1rem;padding:1rem 1.2rem;background:color-mix(in srgb,var(--primary,#5b4ee8) 5%,var(--surface,#fff))}.section-title h3{margin:0}.item-row{display:grid;grid-template-columns:24px 1fr auto;gap:.75rem;align-items:center;padding:.9rem 1.2rem;border-top:1px solid var(--border,#e5e7eb)}.item-row div{display:grid}.item-row small{color:var(--muted,#64748b)}.item-row.previewable{cursor:pointer}.item-row.previewable:hover{background:color-mix(in srgb,var(--primary,#5b4ee8) 5%,var(--surface,#fff))}.item-row button,.preview-heading button{border:0;background:transparent;color:var(--primary,#5b4ee8);font-weight:800;cursor:pointer}.preview-panel{margin-bottom:1.5rem;border:1px solid var(--border,#e5e7eb);border-radius:1rem;overflow:hidden;background:var(--surface,#fff)}.preview-heading{display:flex;justify-content:space-between;gap:1rem;align-items:start;padding:1.2rem}.preview-heading span{font-size:.72rem;letter-spacing:.15em;font-weight:850;color:var(--primary,#5b4ee8)}.preview-heading h2{margin:.3rem 0 0}.preview-video{aspect-ratio:16/9;background:#000}.preview-video iframe,.preview-video-file{width:100%;height:100%;border:0}.preview-video-file{max-height:560px;background:#000}.preview-content{padding:1.2rem;line-height:1.75}
@media(max-width:900px){.course-hero,.course-content{grid-template-columns:1fr}.enroll-card{position:static;max-width:520px}.course-content{margin-top:3rem}}@media(max-width:600px){.facts{display:grid}.item-row{grid-template-columns:20px 1fr}.item-row button{grid-column:2;justify-self:start}}
</style>
