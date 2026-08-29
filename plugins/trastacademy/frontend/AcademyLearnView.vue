<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'

type Row = Record<string, any>

const route = useRoute()
const router = useRouter()
const course = ref<Row | null>(null)
const activeLesson = ref<Row | null>(null)
const activeQuiz = ref<Row | null>(null)
const attempt = ref<Row | null>(null)
const answers = reactive<Record<string, any>>({})
const loading = ref(true)
const selecting = ref(false)
const busy = ref(false)
const curriculumOpen = ref(false)
const error = ref('')
const message = ref('')
const lastFailedAction = ref<'load' | 'select' | 'complete' | 'startQuiz' | 'finishQuiz' | null>(null)

function unwrap<T = Row>(payload: any): T {
  if (payload && typeof payload === 'object') {
    if (payload.data && typeof payload.data === 'object') return payload.data as T
    if (payload.results && typeof payload.results === 'object') return payload.results as T
  }
  return payload as T
}

function sortedSections(value: Row | null) {
  return [...(value?.sections || [])]
    .sort((a, b) => Number(a.position || 0) - Number(b.position || 0))
    .map(section => ({
      ...section,
      items: [
        ...(section.lessons || []).map((item: Row) => ({ ...item, itemKind: 'lesson', sectionTitle: section.title })),
        ...(section.quizzes || []).map((item: Row) => ({ ...item, itemKind: 'quiz', sectionTitle: section.title }))
      ].sort((a, b) => Number(a.position || 0) - Number(b.position || 0))
    }))
}

const sections = computed(() => sortedSections(course.value))
const items = computed(() => sections.value.flatMap(section => section.items))
const progress = computed(() => {
  const reported = Number(course.value?.enrollment?.progressPercentage || 0)
  const calculated = items.value.length ? Math.round(completedItems.value * 100 / items.value.length) : 0
  return Math.max(0, Math.min(100, Math.max(reported, calculated)))
})
const currentIndex = computed(() => items.value.findIndex(item => item.id === activeLesson.value?.id || item.id === activeQuiz.value?.id))
const previousItem = computed(() => currentIndex.value > 0 ? items.value[currentIndex.value - 1] : null)
const nextItem = computed(() => currentIndex.value >= 0 && currentIndex.value < items.value.length - 1 ? items.value[currentIndex.value + 1] : null)
const completedItems = computed(() => Number(course.value?.enrollment?.completedLessons || 0) + Number(course.value?.enrollment?.completedQuizzes || 0))

function setFailure(action: typeof lastFailedAction.value, exception: any, fallback: string) {
  lastFailedAction.value = action
  const status = Number(exception?.status || 0)
  if (status === 401) error.value = 'Su sesión finalizó. Ingrese nuevamente para continuar.'
  else if (status === 403) error.value = 'La sesión segura venció. Actualice el aula y vuelva a intentarlo.'
  else if (status === 409) error.value = 'La evaluación cambió mientras estaba abierta. Recargue el contenido.'
  else error.value = exception instanceof Error ? exception.message : fallback
}

async function retryLastAction() {
  const action = lastFailedAction.value
  error.value = ''
  lastFailedAction.value = null
  if (action === 'startQuiz') return startQuiz()
  if (action === 'finishQuiz') return finishQuiz()
  if (action === 'complete') return completeLesson()
  if (action === 'select') {
    const current = items.value.find(item => item.id === activeLesson.value?.id || item.id === activeQuiz.value?.id)
    if (current) return selectItem(current, false)
  }
  return load()
}

onMounted(load)
watch(() => route.params.slug, load)

async function load() {
  loading.value = true
  error.value = ''
  message.value = ''
  activeLesson.value = null
  activeQuiz.value = null
  attempt.value = null

  try {
    const publicPayload = await api<Row>(`/api/public/academy/courses/${encodeURIComponent(String(route.params.slug))}`)
    const publicCourse = unwrap<Row>(publicPayload)
    if (!publicCourse?.id) throw new Error('El curso no devolvió una estructura válida.')
    if (!publicCourse.enrollment) {
      router.replace(`/academy/${publicCourse.slug}`)
      return
    }

    // El endpoint público autenticado ya devuelve currículo completo. Se usa de inmediato
    // para impedir que una respuesta parcial del endpoint de cuenta deje el aula vacía.
    course.value = publicCourse

    try {
      const accountPayload = await api<Row>(`/api/account/academy/courses/${publicCourse.id}`)
      const accountCourse = unwrap<Row>(accountPayload)
      if (accountCourse?.id || accountCourse?.title || accountCourse?.sections) {
        course.value = {
          ...publicCourse,
          ...accountCourse,
          title: accountCourse.title || publicCourse.title,
          slug: accountCourse.slug || publicCourse.slug,
          sections: Array.isArray(accountCourse.sections) && accountCourse.sections.length
            ? accountCourse.sections
            : publicCourse.sections || [],
          enrollment: accountCourse.enrollment || publicCourse.enrollment
        }
      }
    } catch {
      // El aula continúa con la respuesta pública autenticada, que ya contiene el currículo.
    }

    const requested = String(route.query.item || '')
    const first = items.value.find(item => item.id === requested)
      || items.value.find(item => item.itemKind === 'lesson' && !item.completed)
      || items.value.find(item => item.itemKind === 'quiz' && !item.passed)
      || items.value[0]

    if (first) await selectItem(first, false)
    lastFailedAction.value = null
  } catch (exception: any) {
    if (exception?.status === 401) {
      router.replace({ name: 'my-account', query: { redirect: route.fullPath } })
    } else {
      setFailure('load', exception, 'No se pudo cargar el curso')
    }
  } finally {
    loading.value = false
  }
}

async function selectItem(item: Row, closeMobile = true) {
  if (!item?.id) return
  selecting.value = true
  message.value = ''
  error.value = ''
  attempt.value = null
  Object.keys(answers).forEach(key => delete answers[key])

  try {
    if (item.itemKind === 'lesson') {
      activeQuiz.value = null
      try {
        activeLesson.value = unwrap<Row>(await api<Row>(`/api/account/academy/lessons/${item.id}`))
      } catch {
        activeLesson.value = item
      }
    } else {
      activeLesson.value = null
      try {
        activeQuiz.value = unwrap<Row>(await api<Row>(`/api/account/academy/quizzes/${item.id}`))
      } catch {
        activeQuiz.value = item
      }
    }
    router.replace({ query: { ...route.query, item: item.id } })
    if (closeMobile) curriculumOpen.value = false
    lastFailedAction.value = null
  } catch (exception) {
    setFailure('select', exception, 'No se pudo abrir el contenido')
  } finally {
    selecting.value = false
  }
}

async function completeLesson() {
  if (!activeLesson.value) return
  busy.value = true
  try {
    await api(`/api/account/academy/lessons/${activeLesson.value.id}/progress`, {
      method: 'PUT',
      body: JSON.stringify({ watchedSeconds: activeLesson.value.durationSeconds || 0, completed: true })
    })
    activeLesson.value.completed = true
    message.value = 'Lección completada. Su progreso fue actualizado.'
    lastFailedAction.value = null
    await refreshCourse()
  } catch (exception) {
    setFailure('complete', exception, 'No se pudo guardar el progreso')
  } finally {
    busy.value = false
  }
}

async function startQuiz() {
  if (!activeQuiz.value) return
  busy.value = true
  error.value = ''
  message.value = ''
  lastFailedAction.value = null
  try {
    // Cuando el currículo llegó como resumen, recuperamos el cuestionario completo
    // antes de crear el intento para garantizar que la vista tenga las preguntas.
    if (!Array.isArray(activeQuiz.value.questions) || !activeQuiz.value.questions.length) {
      activeQuiz.value = unwrap<Row>(await api<Row>(`/api/account/academy/quizzes/${activeQuiz.value.id}`))
    }
    if (!Array.isArray(activeQuiz.value.questions) || !activeQuiz.value.questions.length) {
      throw new Error('Esta evaluación todavía no tiene preguntas publicadas. Revísela desde el editor del curso.')
    }
    const created = unwrap<Row>(await api<Row>(`/api/account/academy/quizzes/${activeQuiz.value.id}/attempts`, { method: 'POST' }))
    if (!created?.id || !created?.status) throw new Error('El servidor no devolvió un intento de evaluación válido.')
    attempt.value = created
    message.value = created.status === 'IN_PROGRESS'
      ? `Intento ${created.attemptNumber || 1} iniciado. Responda todas las preguntas y finalice antes de salir.`
      : 'La evaluación fue recuperada correctamente.'
  } catch (exception) {
    setFailure('startQuiz', exception, 'No se pudo iniciar la evaluación')
  } finally {
    busy.value = false
  }
}

function toggleOption(question: Row, optionId: string) {
  if (question.type === 'MULTIPLE_CHOICE') {
    const current: string[] = Array.isArray(answers[question.id]) ? answers[question.id] : []
    answers[question.id] = current.includes(optionId)
      ? current.filter(id => id !== optionId)
      : [...current, optionId]
  } else {
    answers[question.id] = optionId
  }
}

async function finishQuiz() {
  if (!attempt.value || !activeQuiz.value) return
  busy.value = true
  error.value = ''
  try {
    const payload = (activeQuiz.value.questions || []).map((question: Row) => ({
      questionId: question.id,
      selectedOptionIds: question.type === 'MULTIPLE_CHOICE'
        ? (answers[question.id] || [])
        : ['SINGLE_CHOICE', 'TRUE_FALSE'].includes(question.type) && answers[question.id]
          ? [answers[question.id]]
          : [],
      textAnswer: ['SHORT_TEXT', 'LONG_TEXT'].includes(question.type)
        ? String(answers[question.id] || '')
        : null
    }))
    attempt.value = unwrap<Row>(await api<Row>(`/api/account/academy/attempts/${attempt.value.id}/finish`, {
      method: 'POST',
      body: JSON.stringify({ answers: payload })
    }))
    message.value = attempt.value.status === 'PENDING_REVIEW'
      ? 'Su evaluación fue enviada y está pendiente de revisión manual.'
      : attempt.value.passed
        ? `Aprobó con ${attempt.value.score}%`
        : `Resultado: ${attempt.value.score}%. Puede volver a intentarlo si aún tiene intentos disponibles.`
    await refreshCourse()
    lastFailedAction.value = null
  } catch (exception) {
    setFailure('finishQuiz', exception, 'No se pudo finalizar la evaluación')
  } finally {
    busy.value = false
  }
}

async function refreshCourse() {
  if (!course.value) return
  try {
    const refreshed = unwrap<Row>(await api<Row>(`/api/public/academy/courses/${encodeURIComponent(course.value.slug)}`))
    course.value = {
      ...course.value,
      ...refreshed,
      sections: Array.isArray(refreshed.sections) && refreshed.sections.length ? refreshed.sections : course.value.sections
    }
  } catch {
    // El contenido activo no debe desaparecer si falla una actualización secundaria.
  }
}

function youtubeEmbed(url = '') {
  try {
    const parsed = new URL(url)
    const playlist = parsed.searchParams.get('list')
    const videoId = parsed.hostname.includes('youtu.be')
      ? parsed.pathname.slice(1)
      : parsed.searchParams.get('v')
        || (parsed.pathname.includes('/embed/') ? parsed.pathname.split('/embed/')[1]?.split('/')[0] : '')
        || (parsed.pathname.includes('/shorts/') ? parsed.pathname.split('/shorts/')[1]?.split('/')[0] : '')

    if (videoId) {
      const playlistQuery = playlist ? `&list=${encodeURIComponent(playlist)}` : ''
      return `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?rel=0${playlistQuery}`
    }
    if (playlist) {
      const requested = Number.parseInt(parsed.searchParams.get('index') || '1', 10)
      const zeroBasedIndex = Number.isFinite(requested) ? Math.max(0, requested - 1) : 0
      return `https://www.youtube-nocookie.com/embed/videoseries?list=${encodeURIComponent(playlist)}&index=${zeroBasedIndex}&rel=0`
    }
    return ''
  } catch {
    return ''
  }
}

function duration(seconds = 0) {
  if (!seconds) return 'Duración no indicada'
  const minutes = Math.max(1, Math.round(seconds / 60))
  return `${minutes} min`
}
</script>

<template>
  <main class="learn-page">
    <div v-if="loading" class="learn-state"><span class="spinner" />Cargando aula…</div>
    <div v-else-if="error && !course" class="learn-state error"><strong>No se pudo abrir el aula</strong><span>{{ error }}</span><button class="primary" type="button" @click="retryLastAction">Reintentar</button></div>

    <template v-else-if="course">
      <header class="learn-header">
        <div class="course-heading">
          <RouterLink :to="`/academy/${course.slug}`">← Volver al curso</RouterLink>
          <h1>{{ course.title || 'Curso' }}</h1>
          <span>{{ completedItems }} de {{ items.length }} contenidos completados</span>
        </div>
        <div class="header-actions">
          <button class="curriculum-toggle" type="button" @click="curriculumOpen = !curriculumOpen">{{ curriculumOpen ? 'Cerrar contenido' : 'Ver contenido' }}</button>
          <div class="progress" :aria-label="`Progreso ${progress}%`">
            <span>{{ progress }}%</span>
            <div><i :style="{ width: `${progress}%` }" /></div>
          </div>
        </div>
      </header>

      <div class="learn-layout">
        <aside :class="['curriculum-nav', { open: curriculumOpen }]">
          <div class="curriculum-summary">
            <strong>Contenido del curso</strong>
            <span>{{ sections.length }} módulos · {{ items.length }} contenidos</span>
          </div>
          <section v-for="section in sections" :key="section.id">
            <h2>{{ section.title }}</h2>
            <p v-if="section.description">{{ section.description }}</p>
            <button
              v-for="item in section.items"
              :key="item.id"
              :class="{ active: activeLesson?.id === item.id || activeQuiz?.id === item.id }"
              type="button"
              @click="selectItem(item)"
            >
              <span class="item-icon">{{ item.itemKind === 'quiz' ? '?' : item.completed ? '✓' : '▶' }}</span>
              <span class="item-copy">
                <b>{{ item.title || (item.itemKind === 'quiz' ? 'Evaluación' : 'Lección') }}</b>
                <small>{{ item.itemKind === 'quiz' ? 'Evaluación' : duration(item.durationSeconds) }}</small>
              </span>
            </button>
          </section>
        </aside>

        <section class="lesson-stage">
          <div v-if="selecting" class="stage-loading">Abriendo contenido…</div>
          <div v-else class="lesson-card">
            <div v-if="error" class="alert error"><div><strong>No se pudo completar la acción</strong><span>{{ error }}</span></div><button type="button" @click="retryLastAction">Reintentar</button></div>
            <p v-if="message" class="alert success">{{ message }}</p>

            <template v-if="activeLesson">
              <div class="stage-topline">
                <span class="stage-label">LECCIÓN</span>
                <span>{{ duration(activeLesson.durationSeconds) }}</span>
              </div>
              <h2>{{ activeLesson.title }}</h2>

              <div v-if="activeLesson.videoProvider === 'YOUTUBE' && youtubeEmbed(activeLesson.videoUrl)" class="video-frame">
                <iframe
                  :src="youtubeEmbed(activeLesson.videoUrl)"
                  :title="activeLesson.title"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  referrerpolicy="strict-origin-when-cross-origin"
                  allowfullscreen
                />
              </div>
              <video v-else-if="activeLesson.videoUrl" class="video-player" :src="activeLesson.videoUrl" controls preload="metadata" />
              <div v-else class="no-video"><strong>Lección de lectura</strong><span>Esta clase no requiere video.</span></div>

              <article v-if="activeLesson.content" class="lesson-content" v-html="activeLesson.content" />
              <div v-else class="empty-content"><strong>Esta lección todavía no tiene descripción.</strong><span>Puede agregar contenido desde Academia → Cursos → Editar.</span></div>

              <footer class="stage-footer">
                <button class="secondary" type="button" :disabled="!previousItem" @click="previousItem && selectItem(previousItem)">← Anterior</button>
                <button class="primary" type="button" :disabled="busy || activeLesson.completed" @click="completeLesson">{{ activeLesson.completed ? '✓ Lección completada' : 'Marcar como completada' }}</button>
                <button class="secondary" type="button" :disabled="!nextItem" @click="nextItem && selectItem(nextItem)">Siguiente →</button>
              </footer>
            </template>

            <template v-else-if="activeQuiz">
              <div class="stage-topline"><span class="stage-label">EVALUACIÓN</span><span>{{ activeQuiz.questions?.length || 0 }} preguntas</span></div>
              <h2>{{ activeQuiz.title || 'Evaluación del módulo' }}</h2>
              <article v-if="activeQuiz.description" class="lesson-content" v-html="activeQuiz.description" />
              <div class="quiz-meta"><span>Nota mínima: {{ activeQuiz.passingScore }}%</span><span>Intentos: {{ activeQuiz.attemptsUsed }}/{{ activeQuiz.maxAttempts || '∞' }}</span><span v-if="activeQuiz.durationMinutes">Tiempo: {{ activeQuiz.durationMinutes }} min</span></div>
              <button v-if="!attempt" class="primary" type="button" :disabled="busy" @click="startQuiz">Iniciar evaluación</button>

              <form v-else-if="attempt.status === 'IN_PROGRESS'" class="questions" @submit.prevent="finishQuiz">
                <fieldset v-for="(question, index) in activeQuiz.questions" :key="question.id">
                  <legend>{{ index + 1 }}. {{ question.text }}</legend>
                  <label v-for="option in question.options" :key="option.id" class="option">
                    <input
                      :type="question.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'"
                      :name="question.id"
                      :checked="question.type === 'MULTIPLE_CHOICE' ? (answers[question.id] || []).includes(option.id) : answers[question.id] === option.id"
                      @change="toggleOption(question, option.id)"
                    >
                    <span>{{ option.text }}</span>
                  </label>
                  <textarea v-if="['SHORT_TEXT', 'LONG_TEXT'].includes(question.type)" v-model="answers[question.id]" :rows="question.type === 'LONG_TEXT' ? 6 : 2" placeholder="Escriba su respuesta" />
                </fieldset>
                <button class="primary" :disabled="busy" type="submit">Finalizar y evaluar</button>
              </form>

              <div v-else-if="attempt.status === 'PENDING_REVIEW'" class="result pending"><strong>…</strong><span>Pendiente de revisión manual</span></div>
              <div v-else class="result" :class="attempt.passed ? 'passed' : 'failed'"><strong>{{ attempt.score }}%</strong><span>{{ attempt.passed ? 'Aprobado' : 'No aprobado' }}</span><button v-if="!attempt.passed" class="secondary" type="button" @click="startQuiz">Nuevo intento</button></div>

              <footer class="stage-footer quiz-footer">
                <button class="secondary" type="button" :disabled="!previousItem" @click="previousItem && selectItem(previousItem)">← Anterior</button>
                <span />
                <button class="secondary" type="button" :disabled="!nextItem" @click="nextItem && selectItem(nextItem)">Siguiente →</button>
              </footer>
            </template>

            <div v-else-if="!items.length" class="empty-classroom">
              <span>!</span>
              <h2>Este curso no tiene lecciones publicadas</h2>
              <p>Edite el curso y agregue al menos una sección con una lección o evaluación. Para los datos demo, vuelva a ejecutar “Generar demo”.</p>
              <RouterLink to="/admin/academy">Administrar academia</RouterLink>
            </div>
            <div v-else class="learn-state">Seleccione una lección.</div>
          </div>
        </section>
      </div>
    </template>
  </main>
</template>

<style scoped>
.learn-page{min-height:100vh;background:#f5f7fb;color:var(--text,#172033)}
.learn-state{min-height:360px;display:grid;place-content:center;justify-items:center;gap:.8rem;padding:4rem;text-align:center;color:var(--muted,#64748b)}
.learn-state.error strong{color:#b42318;font-size:1.25rem}.spinner{width:32px;height:32px;border:3px solid #dbe2f0;border-top-color:var(--primary,#5b4ee8);border-radius:999px;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
.learn-header{position:sticky;top:0;z-index:15;display:flex;justify-content:space-between;gap:2rem;align-items:center;padding:1rem clamp(1rem,3vw,2rem);background:color-mix(in srgb,var(--surface,#fff) 95%,transparent);backdrop-filter:blur(16px);border-bottom:1px solid var(--border,#e5e7eb)}
.course-heading{min-width:0}.course-heading a{font-size:.78rem;color:var(--primary,#5b4ee8);text-decoration:none;font-weight:850}.course-heading h1{font-size:1.05rem;line-height:1.3;margin:.22rem 0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.course-heading>span{font-size:.75rem;color:var(--muted,#64748b)}
.header-actions{display:flex;align-items:center;gap:1rem}.curriculum-toggle{display:none;border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);border-radius:.75rem;padding:.65rem .8rem;font-weight:800}.progress{display:flex;align-items:center;gap:.8rem;font-weight:900}.progress>div{width:180px;height:8px;background:#dce4f4;border-radius:999px;overflow:hidden}.progress i{display:block;height:100%;background:var(--primary,#5b4ee8);transition:width .3s}
.learn-layout{display:grid;grid-template-columns:minmax(300px,350px) minmax(0,1fr);min-height:calc(100vh - 83px)}
.curriculum-nav{background:var(--surface,#fff);border-right:1px solid var(--border,#e5e7eb);padding:1rem;overflow:auto}.curriculum-summary{display:grid;gap:.2rem;padding:.5rem .55rem 1rem;border-bottom:1px solid var(--border,#e5e7eb)}.curriculum-summary strong{font-size:1rem}.curriculum-summary span{font-size:.76rem;color:var(--muted,#64748b)}
.curriculum-nav section{padding:.35rem 0}.curriculum-nav h2{font-size:.76rem;text-transform:uppercase;letter-spacing:.08em;margin:1rem .55rem .35rem;color:var(--muted,#64748b)}.curriculum-nav section>p{font-size:.72rem;line-height:1.45;color:var(--muted,#64748b);margin:.15rem .55rem .55rem}.curriculum-nav button{width:100%;display:grid;grid-template-columns:32px 1fr;text-align:left;align-items:start;gap:.6rem;border:0;background:transparent;padding:.72rem;border-radius:.8rem;color:inherit;cursor:pointer}.curriculum-nav button:hover,.curriculum-nav button.active{background:color-mix(in srgb,var(--primary,#5b4ee8) 9%,transparent);color:var(--primary,#5b4ee8)}.item-icon{width:28px;height:28px;display:grid;place-items:center;border-radius:.6rem;background:#edf1f8;font-size:.72rem;font-weight:900}.curriculum-nav button.active .item-icon{background:var(--primary,#5b4ee8);color:#fff}.item-copy{display:grid;gap:.2rem;min-width:0}.item-copy b{font-size:.86rem;line-height:1.35}.item-copy small{font-size:.7rem;color:var(--muted,#64748b)}
.lesson-stage{min-width:0;padding:clamp(1rem,3vw,3rem);background:radial-gradient(circle at top right,color-mix(in srgb,var(--primary,#5b4ee8) 8%,transparent),transparent 34%),#f5f7fb}.lesson-card{width:100%;max-width:1320px;margin:0 auto;background:var(--surface,#fff);border:1px solid var(--border,#e5e7eb);border-radius:1.25rem;padding:clamp(1.25rem,3vw,3rem);box-shadow:0 16px 50px rgba(15,23,42,.06)}.stage-loading{min-height:420px;display:grid;place-items:center;color:var(--muted,#64748b)}.stage-topline{display:flex;justify-content:space-between;gap:1rem;color:var(--muted,#64748b);font-size:.78rem}.stage-label{letter-spacing:.15em;font-weight:900;color:var(--primary,#5b4ee8)}.lesson-stage h2{font-size:clamp(1.8rem,3.6vw,3rem);line-height:1.08;margin:.55rem 0 1.5rem}
.video-frame{aspect-ratio:16/9;border-radius:1rem;overflow:hidden;background:#000;margin-bottom:2rem;box-shadow:0 18px 45px rgba(15,23,42,.18)}.video-frame iframe,.video-player{width:100%;height:100%;border:0}.video-player{aspect-ratio:16/9;background:#000;border-radius:1rem;margin-bottom:2rem}.no-video,.empty-content{display:grid;gap:.25rem;padding:1.2rem;border:1px dashed var(--border,#d9dee8);border-radius:1rem;color:var(--muted,#64748b);margin-bottom:1.5rem}.no-video strong,.empty-content strong{color:var(--text,#172033)}
.lesson-content{line-height:1.8;margin-bottom:2rem;color:color-mix(in srgb,var(--text,#172033) 88%,#64748b)}.lesson-content :deep(h3){font-size:1.25rem;margin:1.8rem 0 .6rem}.lesson-content :deep(ul),.lesson-content :deep(ol){padding-left:1.4rem}.lesson-content :deep(li){margin:.35rem 0}.lesson-content :deep(pre){overflow:auto;background:#101827;color:#e5eefb;padding:1rem;border-radius:.85rem}
.stage-footer{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:.8rem;padding-top:1.4rem;border-top:1px solid var(--border,#e5e7eb)}.stage-footer .primary{justify-self:center}.primary,.secondary{border-radius:.8rem;padding:.85rem 1rem;font-weight:850;cursor:pointer}.primary{border:0;background:var(--primary,#5b4ee8);color:#fff}.secondary{border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);color:inherit}.primary:disabled,.secondary:disabled{opacity:.45;cursor:not-allowed}.alert{padding:.85rem 1rem;border-radius:.8rem;margin-bottom:1rem}.alert.error{display:flex;align-items:center;justify-content:space-between;gap:1rem;background:#fef3f2;color:#b42318}.alert.error>div{display:grid;gap:.2rem}.alert.error button{border:1px solid currentColor;border-radius:.65rem;background:transparent;color:inherit;padding:.55rem .7rem;font-weight:850;cursor:pointer;white-space:nowrap}.alert.success{background:#ecfdf3;color:#027a48}
.quiz-meta{display:flex;flex-wrap:wrap;gap:.65rem;margin:1rem 0 2rem}.quiz-meta span{background:#f1f4fa;border-radius:999px;padding:.45rem .7rem;color:var(--muted,#64748b);font-size:.78rem;font-weight:800}.questions{display:grid;gap:1.2rem;margin-top:1.5rem}.questions fieldset{border:1px solid var(--border,#e5e7eb);border-radius:1rem;padding:1.2rem;background:#fbfcff}.questions legend{font-weight:850;padding:0 .4rem}.option{display:flex;gap:.7rem;padding:.68rem;border-radius:.65rem;cursor:pointer}.option:hover{background:color-mix(in srgb,var(--primary,#5b4ee8) 6%,transparent)}.questions textarea{width:100%;box-sizing:border-box;margin-top:.8rem;border:1px solid var(--border,#d9dee8);border-radius:.7rem;padding:.8rem;background:var(--surface,#fff);color:inherit}.result{display:flex;align-items:center;gap:1rem;padding:2rem;border-radius:1rem;margin-top:1rem}.result strong{font-size:3rem}.result.passed{background:#ecfdf3;color:#027a48}.result.failed{background:#fff4ed;color:#b54708}.result.pending{background:color-mix(in srgb,var(--primary,#5b4ee8) 8%,var(--surface,#fff));color:var(--primary,#5b4ee8)}.quiz-footer{margin-top:2rem}
.empty-classroom{min-height:430px;display:grid;place-content:center;justify-items:center;text-align:center;gap:.7rem}.empty-classroom>span{width:48px;height:48px;display:grid;place-items:center;border-radius:1rem;background:#fff4ed;color:#b54708;font-size:1.5rem;font-weight:900}.empty-classroom h2{font-size:1.5rem;margin:.4rem 0}.empty-classroom p{max-width:620px;color:var(--muted,#64748b);line-height:1.7}.empty-classroom a{display:inline-flex;text-decoration:none;background:var(--primary,#5b4ee8);color:#fff;border-radius:.8rem;padding:.8rem 1rem;font-weight:850}
@media(max-width:900px){.learn-layout{grid-template-columns:1fr}.curriculum-toggle{display:inline-flex}.curriculum-nav{display:none;border-right:0;border-bottom:1px solid var(--border,#e5e7eb);max-height:65vh}.curriculum-nav.open{display:block}.progress>div{width:100px}.lesson-stage{padding:1rem}.lesson-card{border-radius:1rem}.learn-header{align-items:flex-start}.course-heading>span{display:none}}
@media(max-width:600px){.learn-header{display:grid;gap:.8rem}.header-actions{justify-content:space-between}.progress{flex:1;justify-content:flex-end}.stage-footer{grid-template-columns:1fr 1fr}.stage-footer .primary{grid-column:1/-1;grid-row:1;justify-self:stretch}.stage-footer .secondary{width:100%}.quiz-footer span{display:none}.lesson-card{padding:1rem}.result{align-items:flex-start;flex-direction:column}}
</style>
