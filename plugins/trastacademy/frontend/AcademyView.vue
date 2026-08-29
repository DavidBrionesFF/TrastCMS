<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import MediaPickerModal from '@/components/media/MediaPickerModal.vue'
import type { MediaAsset } from '@/types'

type Tab = 'overview' | 'courses' | 'imports' | 'enrollments' | 'reviews' | 'mcp' | 'settings'
type EditorTab = 'details' | 'curriculum' | 'publish'
type ItemKind = 'lesson' | 'quiz'
type Row = Record<string, any>

const toast = useToastStore()
const tab = ref<Tab>('overview')
const editorTab = ref<EditorTab>('details')
const loading = ref(true)
const saving = ref(false)
const demoBusy = ref(false)
const themeBusy = ref(false)
const error = ref('')
const dashboard = ref<Row>({})
const courses = ref<Row[]>([])
const enrollments = ref<Row[]>([])
const imports = ref<Row[]>([])
const reviews = ref<Row[]>([])
const diagnostics = ref<Row>({ status: 'UNKNOWN', totals: {}, issues: [], recommendations: [] })
const settings = reactive<Row>({ catalogTitle: 'Academy', catalogDescription: '', defaultCurrency: 'USD', defaultImportUrl: '', allowFreeEnrollment: true })
const search = ref('')
const editor = ref(false)
const draft = reactive<Row>({})
const mediaOpen = ref(false)
const activeSectionIndex = ref(0)
const activeItemKind = ref<ItemKind>('lesson')
const activeItemIndex = ref(0)
const importForm = reactive<Row>({ url: '', bearerToken: '', publish: false, overwriteExisting: false })
const preview = ref<Row | null>(null)
const importBusy = ref(false)
const enrollmentForm = reactive<Row>({ courseId: '', email: '' })

const filteredCourses = computed(() => {
  const term = search.value.toLowerCase().trim()
  return !term ? courses.value : courses.value.filter(course => `${course.title} ${course.slug} ${course.instructorName || ''} ${course.category || ''}`.toLowerCase().includes(term))
})
const tabs: Array<{ id: Tab; label: string }> = [
  { id: 'overview', label: 'Resumen' }, { id: 'courses', label: 'Cursos' }, { id: 'imports', label: 'Importar' },
  { id: 'enrollments', label: 'Inscripciones' }, { id: 'reviews', label: 'Revisiones' }, { id: 'mcp', label: 'MCP y diagnóstico' }, { id: 'settings', label: 'Configuración' }
]
const editorTabs: Array<{ id: EditorTab; label: string }> = [
  { id: 'details', label: 'Información del curso' }, { id: 'curriculum', label: 'Contenido y evaluaciones' }, { id: 'publish', label: 'Publicación' }
]
const mcpTools = [
  { name: 'academy_dashboard', description: 'Indicadores generales de cursos, matrículas e importaciones.' },
  { name: 'academy_diagnostics', description: 'Auditoría de cursos vacíos, lecciones incompletas y evaluaciones inválidas.' },
  { name: 'academy_course_list', description: 'Búsqueda y paginación de cursos administrables.' },
  { name: 'academy_course_get', description: 'Curso y currículo completo por identificador.' },
  { name: 'academy_enrollment_list', description: 'Matrículas filtradas por estudiante, curso o estado.' },
  { name: 'academy_student_progress', description: 'Progreso completo de un estudiante por correo.' },
  { name: 'academy_pending_reviews', description: 'Respuestas abiertas pendientes de calificación.' },
  { name: 'academy_import_preview', description: 'Análisis seguro de una fuente externa antes de importarla.' },
  { name: 'academy_import_history', description: 'Historial de importaciones, advertencias y errores.' },
  { name: 'academy_settings_get', description: 'Configuración pública y comercial de la academia.' }
]
const activeSection = computed<Row | null>(() => draft.sections?.[activeSectionIndex.value] || null)
const activeContent = computed<Row | null>(() => {
  const section = activeSection.value
  if (!section) return null
  return activeItemKind.value === 'lesson'
    ? section.lessons?.[activeItemIndex.value] || null
    : section.quizzes?.[activeItemIndex.value] || null
})
const lessonCount = computed(() => (draft.sections || []).reduce((total: number, section: Row) => total + (section.lessons || []).length, 0))
const quizCount = computed(() => (draft.sections || []).reduce((total: number, section: Row) => total + (section.quizzes || []).length, 0))
const questionCount = computed(() => (draft.sections || []).reduce((total: number, section: Row) => total + (section.quizzes || []).reduce((subtotal: number, quiz: Row) => subtotal + (quiz.questions || []).length, 0), 0))
const activeQuizPoints = computed(() => activeItemKind.value === 'quiz' ? (activeContent.value?.questions || []).reduce((sum: number, question: Row) => sum + Number(question.points || 0), 0) : 0)
const editorialChecks = computed(() => [
  { label: 'Título definido', ok: Boolean(draft.title?.trim()) },
  { label: 'Extracto para el catálogo', ok: Boolean(draft.excerpt?.trim()) },
  { label: 'Descripción enriquecida', ok: Boolean(stripHtml(draft.description || '').length > 80) },
  { label: 'Imagen destacada', ok: Boolean(draft.featuredImageUrl) },
  { label: 'Categoría e instructor', ok: Boolean(draft.category?.trim() && draft.instructorName?.trim()) },
  { label: 'Al menos una lección', ok: lessonCount.value > 0 },
  { label: 'Evaluación de aprendizaje', ok: quizCount.value > 0 }
])
const editorialScore = computed(() => Math.round(editorialChecks.value.filter(item => item.ok).length * 100 / editorialChecks.value.length))

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  const requests = await Promise.allSettled([
    api<Row>('/api/admin/academy/dashboard'),
    api<Row>('/api/admin/academy/courses?size=100'),
    api<Row[]>('/api/admin/academy/enrollments'),
    api<Row[]>('/api/admin/academy/reviews'),
    api<Row[]>('/api/admin/academy/imports'),
    api<Row>('/api/admin/academy/settings'),
    api<Row>('/api/admin/academy/diagnostics')
  ])

  const failures: string[] = []
  const value = <T,>(index: number, fallback: T): T => {
    const result = requests[index]
    if (result.status === 'fulfilled') return unwrap<T>(result.value)
    failures.push(result.reason instanceof Error ? result.reason.message : `No se pudo cargar el recurso ${index + 1}`)
    return fallback
  }

  dashboard.value = value<Row>(0, {})
  courses.value = value<Row>(1, { content: [] }).content || []
  enrollments.value = value<Row[]>(2, []) || []
  reviews.value = (value<Row[]>(3, []) || []).map((row: Row) => ({
    ...row,
    answers: (row.answers || []).map((answer: Row) => ({ ...answer, awardedPoints: Number(answer.awardedPoints || 0) }))
  }))
  imports.value = value<Row[]>(4, []) || []
  Object.assign(settings, value<Row>(5, settings))
  diagnostics.value = value<Row>(6, diagnostics.value)
  if (!importForm.url) importForm.url = settings.defaultImportUrl || ''
  if (!enrollmentForm.courseId) enrollmentForm.courseId = courses.value[0]?.id || ''
  if (failures.length) error.value = `TrastAcademy cargó parcialmente. ${[...new Set(failures)].join(' · ')}`
  loading.value = false
}

function unwrap<T = Row>(payload: any): T {
  if (payload?.data && typeof payload.data === 'object') return payload.data as T
  if (payload?.results && typeof payload.results === 'object') return payload.results as T
  return payload as T
}

function emptyCourse(): Row {
  return {
    title: '', slug: '', excerpt: '', description: '', featuredImageUrl: '', instructorName: 'Academy', category: 'General',
    level: 'BEGINNER', accessType: 'FREE', status: 'DRAFT', price: 0, currency: settings.defaultCurrency || 'USD',
    estimatedMinutes: 0, featured: false, externalSourceId: '', externalSourceUrl: '', sections: [emptySection(0)]
  }
}
function emptySection(position = 0): Row { return { title: `Módulo ${position + 1}`, description: '', position, externalSourceId: '', lessons: [emptyLesson(0)], quizzes: [] } }
function emptyLesson(position = 0): Row { return { title: 'Nueva lección', content: '<p>Escriba aquí los objetivos, explicación y práctica de la lección.</p>', position, lessonType: 'TEXT', videoProvider: null, videoUrl: '', mediaId: '', durationSeconds: 0, preview: false, externalSourceId: '' } }
function emptyQuiz(position = 0): Row { return { title: 'Nueva evaluación', description: '<p>Explique qué conocimientos comprobará esta evaluación.</p>', position, passingScore: 70, maxAttempts: 3, durationMinutes: 0, randomizeQuestions: false, preview: false, externalSourceId: '', questions: [emptyQuestion(0)] } }
function emptyQuestion(position = 0): Row { return { text: 'Nueva pregunta', type: 'SINGLE_CHOICE', position, points: 1, explanation: '', required: true, options: [{ text: 'Opción correcta', position: 0, correct: true }, { text: 'Otra opción', position: 1, correct: false }] } }

async function openCourse(item?: Row) {
  Object.keys(draft).forEach(key => delete draft[key])
  editorTab.value = 'details'
  activeSectionIndex.value = 0
  activeItemKind.value = 'lesson'
  activeItemIndex.value = 0
  editor.value = true
  if (item) {
    saving.value = true
    try {
      const value = unwrap<Row>(await api<Row>(`/api/admin/academy/courses/${item.id}`))
      value.title ||= ''
      value.slug ||= ''
      value.excerpt ||= ''
      value.description ||= ''
      value.featuredImageUrl ||= ''
      value.instructorName ||= 'Academy'
      value.category ||= 'General'
      value.currency ||= settings.defaultCurrency || 'USD'
      value.externalSourceId ||= ''
      value.externalSourceUrl ||= ''
      value.sections ||= []
      value.sections.forEach((section: Row) => {
        section.title ||= ''
        section.description ||= ''
        section.externalSourceId ||= ''
        section.lessons ||= []
        section.quizzes ||= []
        section.lessons.forEach((lesson: Row) => {
          lesson.title ||= ''
          lesson.content ||= ''
          lesson.videoUrl ||= ''
          lesson.mediaId ||= ''
          lesson.externalSourceId ||= ''
        })
        section.quizzes.forEach((quiz: Row) => {
          quiz.title ||= ''
          quiz.description ||= ''
          quiz.externalSourceId ||= ''
          quiz.questions ||= []
          quiz.questions.forEach((question: Row) => {
            question.text ||= ''
            question.explanation ||= ''
            question.options ||= []
            question.options.forEach((option: Row) => { option.text ||= '' })
          })
        })
      })
      Object.assign(draft, value)
    } catch (exception) {
      error.value = exception instanceof Error ? exception.message : 'No se pudo abrir el curso'
      editor.value = false
    } finally {
      saving.value = false
    }
  } else {
    Object.assign(draft, emptyCourse())
  }
}

function selectSection(index: number) {
  activeSectionIndex.value = index
  const section = draft.sections[index]
  if (section?.lessons?.length) { activeItemKind.value = 'lesson'; activeItemIndex.value = 0 }
  else if (section?.quizzes?.length) { activeItemKind.value = 'quiz'; activeItemIndex.value = 0 }
}
function selectContent(kind: ItemKind, index: number) { activeItemKind.value = kind; activeItemIndex.value = index }
function addSection() { draft.sections.push(emptySection(draft.sections.length)); selectSection(draft.sections.length - 1) }
function removeSection(index: number) { if (window.confirm('¿Eliminar este módulo y todo su contenido?')) { draft.sections.splice(index, 1); selectSection(Math.max(0, index - 1)) } }
function addLesson(section: Row) { section.lessons.push(emptyLesson(section.lessons.length + section.quizzes.length)); activeItemKind.value = 'lesson'; activeItemIndex.value = section.lessons.length - 1 }
function removeLesson(section: Row, index: number) { if (window.confirm('¿Eliminar esta lección?')) { section.lessons.splice(index, 1); activeItemIndex.value = Math.max(0, index - 1) } }
function addQuiz(section: Row) { section.quizzes.push(emptyQuiz(section.lessons.length + section.quizzes.length)); activeItemKind.value = 'quiz'; activeItemIndex.value = section.quizzes.length - 1 }
function removeQuiz(section: Row, index: number) { if (window.confirm('¿Eliminar esta evaluación y sus preguntas?')) { section.quizzes.splice(index, 1); activeItemIndex.value = Math.max(0, index - 1) } }
function addQuestion(quiz: Row) { quiz.questions.push(emptyQuestion(quiz.questions.length)) }
function removeQuestion(quiz: Row, index: number) { quiz.questions.splice(index, 1) }
function addOption(question: Row) { question.options ||= []; question.options.push({ text: 'Nueva opción', position: question.options.length, correct: false }) }
function removeOption(question: Row, index: number) { question.options.splice(index, 1) }
function moveSection(index: number, direction: number) { move(draft.sections, index, direction); activeSectionIndex.value = Math.max(0, Math.min(draft.sections.length - 1, index + direction)) }
function moveContent(section: Row, kind: ItemKind, index: number, direction: number) { const list = kind === 'lesson' ? section.lessons : section.quizzes; move(list, index, direction); activeItemIndex.value = Math.max(0, Math.min(list.length - 1, index + direction)) }
function move(list: Row[], index: number, direction: number) { const target = index + direction; if (target < 0 || target >= list.length) return; [list[index], list[target]] = [list[target], list[index]] }

async function saveCourse(closeAfter = false) {
  saving.value = true
  error.value = ''
  try {
    normalizePositions()
    const id = draft.id
    const payload = JSON.parse(JSON.stringify(draft))
    ;['createdAt', 'updatedAt', 'enrollment', 'demoData'].forEach(key => delete payload[key])
    const saved = unwrap<Row>(await api(id ? `/api/admin/academy/courses/${id}` : '/api/admin/academy/courses', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) }))
    Object.assign(draft, saved)
    toast.success('Curso guardado correctamente.')
    if (closeAfter) editor.value = false
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar el curso'
  } finally {
    saving.value = false
  }
}

function normalizePositions() {
  (draft.sections || []).forEach((section: Row, sectionIndex: number) => {
    section.position = sectionIndex
    const ordered = [
      ...(section.lessons || []).map((item: Row) => ({ item, kind: 'lesson' })),
      ...(section.quizzes || []).map((item: Row) => ({ item, kind: 'quiz' }))
    ].sort((a, b) => Number(a.item.position || 0) - Number(b.item.position || 0))
    ordered.forEach((entry, position) => { entry.item.position = position })
    ;(section.quizzes || []).forEach((quiz: Row) => (quiz.questions || []).forEach((question: Row, questionIndex: number) => {
      question.position = questionIndex
      ;(question.options || []).forEach((option: Row, optionIndex: number) => { option.position = optionIndex })
    }))
  })
}

async function deleteCourse(course: Row) { if (!window.confirm(`¿Eliminar o archivar ${course.title}?`)) return; await api(`/api/admin/academy/courses/${course.id}`, { method: 'DELETE' }); await load() }
async function uploadVideo(lesson: Row, event: Event) { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return; const form = new FormData(); form.append('file', file); saving.value = true; try { const media = unwrap<Row>(await api<Row>('/api/admin/media?folder=Academy', { method: 'POST', body: form })); lesson.lessonType = 'VIDEO'; lesson.videoProvider = 'UPLOAD'; lesson.videoUrl = media.publicUrl; lesson.mediaId = media.id; lesson.durationSeconds = Math.round(media.durationSeconds || lesson.durationSeconds || 0); toast.success('Video cargado y vinculado.') } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo subir el video' } finally { saving.value = false; input.value = '' } }
function chooseFeatured(items: MediaAsset[]) { const item = items[0]; if (!item) return; draft.featuredImageUrl = item.publicUrl; mediaOpen.value = false }
async function previewImport() { importBusy.value = true; error.value = ''; preview.value = null; try { preview.value = unwrap<Row>(await api<Row>('/api/admin/academy/imports/preview', { method: 'POST', body: JSON.stringify(importForm) })) } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo analizar la fuente' } finally { importBusy.value = false } }
async function executeImport() { importBusy.value = true; error.value = ''; try { await api('/api/admin/academy/imports', { method: 'POST', body: JSON.stringify(importForm) }); toast.success('Curso importado correctamente.'); preview.value = null; await load(); tab.value = 'courses' } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo importar' } finally { importBusy.value = false } }
async function createEnrollment() { if (!enrollmentForm.courseId || !enrollmentForm.email) return; try { await api(`/api/admin/academy/courses/${enrollmentForm.courseId}/enrollments`, { method: 'POST', body: JSON.stringify({ email: enrollmentForm.email }) }); toast.success('Inscripción creada.'); enrollmentForm.email = ''; await load() } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo inscribir al usuario' } }
async function reviewAttempt(review: Row) { saving.value = true; error.value = ''; try { await api(`/api/admin/academy/attempts/${review.attemptId}/review`, { method: 'POST', body: JSON.stringify({ grades: (review.answers || []).map((answer: Row) => ({ questionId: answer.questionId, awardedPoints: Number(answer.awardedPoints || 0) })) }) }); toast.success('Evaluación calificada.'); await load() } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo guardar la calificación' } finally { saving.value = false } }
async function saveSettings() { saving.value = true; try { await api('/api/admin/academy/settings', { method: 'PUT', body: JSON.stringify(settings) }); toast.success('Configuración guardada.'); await load() } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo guardar' } finally { saving.value = false } }
async function demoData(action: 'generate' | 'delete') {
  if (action === 'delete' && !window.confirm('¿Eliminar únicamente los cursos y matrículas demo?')) return
  demoBusy.value = true
  error.value = ''
  try {
    const result = unwrap<Row>(await api<Row>('/api/admin/academy/demo-data', { method: action === 'generate' ? 'POST' : 'DELETE' }))
    if (action === 'generate') await prepareAcademyTheme(true)
    toast.success(action === 'generate' ? 'Academia demo preparada' : 'Datos demo eliminados', result.message)
    await load()
    if (action === 'generate') tab.value = 'courses'
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudieron procesar los datos demo'
  } finally {
    demoBusy.value = false
  }
}

async function prepareAcademyTheme(silent = false) {
  themeBusy.value = true
  error.value = ''
  try {
    await api('/api/admin/themes/active', { method: 'PUT', body: JSON.stringify({ themeId: 'nexus' }) })
    const restored = unwrap<Row>(await api<Row>('/api/admin/themes/nexus/starter-content/restore', { method: 'POST' }))
    window.localStorage.setItem('trastcms.activeTheme', 'nexus')
    if (!silent) toast.success('Tema Nexus activado', `${restored.created || 0} páginas creadas y ${restored.updated || 0} actualizadas.`)
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo preparar el tema comunitario'
    throw exception
  } finally {
    themeBusy.value = false
  }
}
function money(value: any, currency = 'USD') { return new Intl.NumberFormat('es-US', { style: 'currency', currency }).format(Number(value || 0)) }
function stripHtml(value: string) { return value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim() }
function levelLabel(value = '') { return value === 'BEGINNER' ? 'Inicial' : value === 'INTERMEDIATE' ? 'Intermedio' : value === 'ADVANCED' ? 'Avanzado' : 'Todos los niveles' }
</script>

<template>
  <div class="academy-admin">
    <AdminPageHeader title="TrastAcademy" eyebrow="Aprendizaje" description="Cursos, lecciones, evaluaciones, importaciones y progreso desde un solo módulo.">
      <template #actions><div class="header-actions"><RouterLink class="btn" to="/academy" target="_blank">Ver academia</RouterLink><button class="btn" :disabled="demoBusy" @click="demoData('generate')">{{ demoBusy ? 'Preparando…' : 'Generar demo' }}</button><button class="btn" :disabled="themeBusy" @click="prepareAcademyTheme()">{{ themeBusy ? 'Activando…' : 'Activar Nexus' }}</button><button class="btn primary" @click="openCourse()">Nuevo curso</button></div></template>
    </AdminPageHeader>

    <p v-if="error && !editor" class="alert error">{{ error }}</p>
    <nav class="tabs"><button v-for="item in tabs" :key="item.id" :class="{ active: tab === item.id }" @click="tab = item.id">{{ item.label }}<span v-if="item.id === 'reviews' && reviews.length">{{ reviews.length }}</span></button></nav>
    <div v-if="loading" class="state">Cargando TrastAcademy…</div>

    <template v-else>
      <section v-if="tab === 'overview'" class="space">
        <div class="metrics"><article><span>Cursos</span><strong>{{ dashboard.totalCourses || 0 }}</strong><small>{{ dashboard.publishedCourses || 0 }} publicados</small></article><article><span>Inscripciones activas</span><strong>{{ dashboard.activeEnrollments || 0 }}</strong><small>{{ dashboard.completedEnrollments || 0 }} completadas</small></article><article><span>Cursos demo</span><strong>{{ dashboard.demoCourses || 0 }}</strong><small>Contenido para probar el LMS</small></article><article><span>Revisiones</span><strong>{{ reviews.length }}</strong><small>Evaluaciones abiertas pendientes</small></article></div>
        <div class="two-cols"><article class="panel"><header><h2>Cursos destacados</h2><button class="link" @click="tab = 'courses'">Ver todos</button></header><div v-for="course in dashboard.featuredCourses || []" :key="course.id" class="list-row"><div><strong>{{ course.title }}</strong><small>{{ course.category || 'General' }} · {{ course.lessonCount }} lecciones</small></div><button class="link" @click="openCourse(course)">Editar</button></div><p v-if="!dashboard.featuredCourses?.length" class="empty">Todavía no hay cursos destacados.</p></article><article class="panel"><header><h2>Importaciones recientes</h2><button class="link" @click="tab = 'imports'">Importar curso</button></header><div v-for="job in dashboard.recentImports || []" :key="job.id" class="list-row"><div><strong>{{ job.detectedTitle || job.sourceUrl }}</strong><small>{{ job.status }} · {{ job.sourceType }}</small></div><span class="status" :class="job.status?.toLowerCase()">{{ job.status }}</span></div><p v-if="!dashboard.recentImports?.length" class="empty">Todavía no hay importaciones.</p></article></div>
      </section>

      <section v-if="tab === 'courses'" class="space"><div class="toolbar"><input v-model="search" type="search" placeholder="Buscar por título, slug, categoría o instructor"><button class="btn primary" @click="openCourse()">Crear curso</button></div><div class="course-admin-grid"><article v-for="course in filteredCourses" :key="course.id" class="course-admin-card"><div class="cover" :style="course.featuredImageUrl ? { backgroundImage: `url(${course.featuredImageUrl})` } : {}"><div class="cover-badges"><span>{{ course.status }}</span><span>{{ course.category || 'General' }}</span><span v-if="course.demoData" class="demo-badge">DEMO</span></div></div><div class="body"><small>{{ course.instructorName || 'Academy' }} · {{ levelLabel(course.level) }}</small><h2>{{ course.title }}</h2><p>{{ course.excerpt || 'Sin extracto para el catálogo.' }}</p><div class="meta"><span>{{ course.sectionCount }} módulos</span><span>{{ course.lessonCount }} lecciones</span><span>{{ course.quizCount }} evaluaciones</span><span>{{ course.enrollmentCount }} alumnos</span></div><footer><strong>{{ course.accessType === 'FREE' ? 'Gratis' : money(course.price, course.currency) }}</strong><div><button class="link" @click="openCourse(course)">Editar</button><button class="link danger" @click="deleteCourse(course)">Eliminar</button></div></footer></div></article></div><p v-if="!filteredCourses.length" class="empty panel">No hay cursos que coincidan con la búsqueda.</p></section>

      <section v-if="tab === 'imports'" class="two-cols align-start"><form class="panel form" @submit.prevent="previewImport"><header><div><h2>Importar o sincronizar un curso</h2><p>Use una API REST, JSON o página compatible con LearnPress.</p></div></header><label>URL de la fuente<input v-model="importForm.url" required type="url" placeholder="https://sitio.com/wp-json/learnpress/v1/courses/123"></label><label>Bearer Token opcional<input v-model="importForm.bearerToken" type="password" autocomplete="off"></label><div class="checks"><label><input v-model="importForm.publish" type="checkbox"> Publicar al importar</label><label><input v-model="importForm.overwriteExisting" type="checkbox"> Actualizar si ya existe</label></div><button class="btn primary" :disabled="importBusy">{{ importBusy ? 'Analizando…' : 'Previsualizar' }}</button></form><article class="panel"><header><h2>Resultado del análisis</h2></header><div v-if="preview" class="preview"><h3>{{ preview.title }}</h3><p>{{ preview.excerpt }}</p><div class="metrics compact"><article><strong>{{ preview.sections }}</strong><span>Secciones</span></article><article><strong>{{ preview.lessons }}</strong><span>Lecciones</span></article><article><strong>{{ preview.quizzes }}</strong><span>Evaluaciones</span></article><article><strong>{{ preview.questions }}</strong><span>Preguntas</span></article></div><ul v-if="preview.warnings?.length" class="warnings"><li v-for="warning in preview.warnings" :key="warning">{{ warning }}</li></ul><button class="btn primary" :disabled="importBusy" @click="executeImport">Importar curso</button></div><p v-else class="empty">Ingrese una URL y analícela antes de importar.</p></article><article class="panel span-two"><header><h2>Historial</h2></header><div class="table-wrap"><table><thead><tr><th>Fuente</th><th>Tipo</th><th>Estado</th><th>Resultado</th></tr></thead><tbody><tr v-for="job in imports" :key="job.id"><td>{{ job.detectedTitle || job.sourceUrl }}</td><td>{{ job.sourceType }}</td><td><span class="status">{{ job.status }}</span></td><td>{{ job.errorMessage || job.resultCourseId || '—' }}</td></tr></tbody></table></div></article></section>

      <section v-if="tab === 'enrollments'" class="space"><form class="panel inline-form" @submit.prevent="createEnrollment"><label>Curso<select v-model="enrollmentForm.courseId"><option v-for="course in courses" :key="course.id" :value="course.id">{{ course.title }}</option></select></label><label>Correo del estudiante<input v-model="enrollmentForm.email" required type="email" placeholder="estudiante@correo.com"></label><button class="btn primary">Inscribir</button></form><div class="panel table-wrap"><table><thead><tr><th>Estudiante</th><th>Curso</th><th>Origen</th><th>Progreso</th><th>Estado</th></tr></thead><tbody><tr v-for="row in enrollments" :key="row.id"><td>{{ row.email }}</td><td>{{ row.courseTitle }}</td><td>{{ row.source }}</td><td><div class="progress-cell"><i :style="{ width: `${row.progressPercentage}%` }" /></div><small>{{ row.progressPercentage }}%</small></td><td><span class="status" :class="row.status?.toLowerCase()">{{ row.status }}</span></td></tr></tbody></table></div></section>

      <section v-if="tab === 'reviews'" class="space"><article v-for="review in reviews" :key="review.attemptId" class="panel review-card"><header><div><h2>{{ review.courseTitle }}</h2><small>{{ review.quizTitle }} · {{ review.enrollmentEmail || review.email }} · intento {{ review.attemptNumber }}</small></div></header><div v-for="answer in review.answers" :key="answer.responseId" class="review-answer"><div><strong>{{ answer.questionText }}</strong><p>{{ answer.textAnswer || 'Sin respuesta' }}</p></div><label>Puntaje (máx. {{ answer.maxPoints }})<input v-model.number="answer.awardedPoints" type="number" min="0" :max="answer.maxPoints" step="0.5"></label></div><footer><button class="btn primary" :disabled="saving" @click="reviewAttempt(review)">Guardar calificación</button></footer></article><p v-if="!reviews.length" class="empty panel">No hay evaluaciones pendientes de revisión manual.</p></section>

      <section v-if="tab === 'mcp'" class="space mcp-area">
        <div class="diagnostic-hero panel" :class="String(diagnostics.status || 'unknown').toLowerCase()">
          <div><span>ESTADO OPERATIVO</span><h2>{{ diagnostics.status === 'READY' ? 'TrastAcademy está listo' : diagnostics.status === 'DEGRADED' ? 'La academia requiere atención' : 'Diagnóstico no disponible' }}</h2><p>Revise contenido, evaluaciones, matrículas e integraciones antes de publicar la academia.</p></div>
          <strong>{{ diagnostics.status || 'UNKNOWN' }}</strong>
        </div>
        <div class="metrics diagnostic-metrics">
          <article><span>Cursos publicados</span><strong>{{ diagnostics.totals?.publishedCourses || 0 }}</strong><small>de {{ diagnostics.totals?.courses || 0 }} cursos</small></article>
          <article><span>Lecciones</span><strong>{{ diagnostics.totals?.lessons || 0 }}</strong><small>{{ diagnostics.emptyLessons || 0 }} sin contenido</small></article>
          <article><span>Evaluaciones</span><strong>{{ diagnostics.totals?.quizzes || 0 }}</strong><small>{{ diagnostics.invalidQuizzes || 0 }} con problemas</small></article>
          <article><span>Intentos</span><strong>{{ diagnostics.totals?.attempts || 0 }}</strong><small>{{ diagnostics.totals?.enrollments || 0 }} matrículas</small></article>
        </div>
        <div class="two-cols align-start">
          <article class="panel">
            <header><div><h2>Herramientas MCP</h2><p>Disponibles mediante el servidor MCP central de TrastCMS.</p></div><code>/mcp</code></header>
            <div class="mcp-tool" v-for="tool in mcpTools" :key="tool.name"><code>{{ tool.name }}</code><p>{{ tool.description }}</p><span>Solo lectura</span></div>
          </article>
          <article class="panel">
            <header><div><h2>Hallazgos del diagnóstico</h2><p>Los primeros 100 problemas se exponen también mediante <code>academy_diagnostics</code>.</p></div><button class="link" type="button" @click="load">Actualizar</button></header>
            <div v-for="issue in diagnostics.issues || []" :key="`${issue.code}-${issue.entityId}`" class="diagnostic-issue" :class="issue.severity"><span>{{ issue.severity }}</span><div><strong>{{ issue.title || issue.code }}</strong><p>{{ issue.detail }}</p><small>{{ issue.code }} · {{ issue.entityId }}</small></div></div>
            <p v-if="!diagnostics.issues?.length" class="empty">No se detectaron problemas estructurales.</p>
            <ul v-if="diagnostics.recommendations?.length" class="recommendations"><li v-for="recommendation in diagnostics.recommendations" :key="recommendation">{{ recommendation }}</li></ul>
          </article>
        </div>
      </section>

      <section v-if="tab === 'settings'" class="two-cols align-start"><form class="panel form settings-form" @submit.prevent="saveSettings"><header><h2>Catálogo y matrículas</h2></header><label>Título del catálogo<input v-model="settings.catalogTitle" required></label><label>Descripción<textarea v-model="settings.catalogDescription" rows="5"></textarea></label><div class="form-grid"><label>Moneda predeterminada<input v-model="settings.defaultCurrency" maxlength="3"></label><label>URL predeterminada para importar<input v-model="settings.defaultImportUrl" type="url"></label></div><label class="check"><input v-model="settings.allowFreeEnrollment" type="checkbox"> Permitir inscripción directa a cursos gratuitos</label><button class="btn primary" :disabled="saving">Guardar configuración</button></form><article class="panel"><header><h2>Contenido demostrativo</h2></header><p>Prepare cursos completos con videos, evaluaciones y progreso para comprobar todas las funciones.</p><div class="actions"><button class="btn" :disabled="demoBusy" @click="demoData('generate')">Generar o actualizar demo</button><button class="btn danger-outline" :disabled="demoBusy" @click="demoData('delete')">Eliminar demo</button></div></article></section>
    </template>

    <Teleport to="body">
      <div v-if="editor" class="course-editor-shell">
        <header class="course-editor-topbar">
          <button class="back-button" type="button" @click="editor = false">← Volver a cursos</button>
          <div class="editor-title"><span>{{ draft.id ? 'EDITAR CURSO' : 'NUEVO CURSO' }}</span><strong>{{ draft.title || 'Curso sin título' }}</strong></div>
          <div class="editor-actions"><RouterLink v-if="draft.slug" :to="`/academy/${draft.slug}`" target="_blank" class="button secondary">Vista previa</RouterLink><button class="button secondary" type="button" :disabled="saving" @click="saveCourse(false)">{{ saving ? 'Guardando…' : 'Guardar borrador' }}</button><button class="button" type="button" :disabled="saving" @click="saveCourse(true)">Guardar y cerrar</button></div>
        </header>

        <nav class="course-editor-tabs"><button v-for="item in editorTabs" :key="item.id" :class="{ active: editorTab === item.id }" type="button" @click="editorTab = item.id">{{ item.label }}</button></nav>
        <p v-if="error" class="editor-error">{{ error }}</p>

        <main v-if="editorTab === 'details'" class="course-editor-main">
          <section class="editor-primary-column">
            <article class="title-card"><label>Título del curso<input v-model="draft.title" maxlength="240" placeholder="Ej. API REST completa con Spring Boot"></label><div><label>URL del curso<span>/academy/</span><input v-model="draft.slug" maxlength="180" placeholder="se-genera-automaticamente"></label><label>Estado<select v-model="draft.status"><option value="DRAFT">Borrador</option><option value="PUBLISHED">Publicado</option><option value="ARCHIVED">Archivado</option></select></label></div></article>
            <article class="editor-section"><header><div><h2>Descripción del curso</h2><p>Esta información se mostrará en la página pública y debe explicar qué aprenderá el estudiante.</p></div></header><RichTextEditor v-model="draft.description" :min-height="480" placeholder="Presente el curso, resultados, requisitos y metodología…" /></article>
            <article class="editor-section"><header><div><h2>Resumen para el catálogo</h2><p>Texto breve utilizado en tarjetas y resultados de búsqueda.</p></div><span>{{ (draft.excerpt || '').length }}/1000</span></header><textarea v-model="draft.excerpt" rows="5" maxlength="1000" placeholder="Explique en pocas líneas por qué este curso es útil."></textarea></article>
          </section>

          <aside class="editor-sidebar">
            <article class="sidebar-card"><h3>Imagen destacada</h3><button class="featured-picker" type="button" @click="mediaOpen = true"><img v-if="draft.featuredImageUrl" :src="draft.featuredImageUrl" alt="Portada del curso"><span v-else><b>＋</b>Seleccionar desde medios</span></button><button v-if="draft.featuredImageUrl" class="text-danger" type="button" @click="draft.featuredImageUrl = ''">Quitar imagen</button></article>
            <article class="sidebar-card form-stack"><h3>Clasificación</h3><label>Categoría<input v-model="draft.category" maxlength="120" placeholder="Ej. Desarrollo backend"></label><label>Instructor<input v-model="draft.instructorName" maxlength="180"></label><label>Nivel<select v-model="draft.level"><option value="BEGINNER">Inicial</option><option value="INTERMEDIATE">Intermedio</option><option value="ADVANCED">Avanzado</option><option value="ALL_LEVELS">Todos los niveles</option></select></label><label>Duración estimada (minutos)<input v-model.number="draft.estimatedMinutes" type="number" min="0"></label></article>
            <article class="sidebar-card"><div class="quality-header"><div><h3>Calidad del curso</h3><p>Lista mínima antes de publicar.</p></div><strong :class="editorialScore >= 80 ? 'good' : editorialScore >= 50 ? 'medium' : 'low'">{{ editorialScore }}%</strong></div><ul class="quality-list"><li v-for="item in editorialChecks" :key="item.label" :class="{ done: item.ok }"><span>{{ item.ok ? '✓' : '○' }}</span>{{ item.label }}</li></ul></article>
          </aside>
        </main>

        <main v-else-if="editorTab === 'curriculum'" class="curriculum-editor">
          <aside class="curriculum-tree">
            <header><div><strong>Plan del curso</strong><span>{{ lessonCount }} lecciones · {{ quizCount }} evaluaciones</span></div><button type="button" @click="addSection">＋ Módulo</button></header>
            <article v-for="(section, sectionIndex) in draft.sections" :key="section.id || sectionIndex" :class="{ active: activeSectionIndex === sectionIndex }">
              <button class="section-row" type="button" @click="selectSection(sectionIndex)"><span>{{ sectionIndex + 1 }}</span><div><strong>{{ section.title || `Módulo ${sectionIndex + 1}` }}</strong><small>{{ section.lessons.length }} clases · {{ section.quizzes.length }} exámenes</small></div></button>
              <div v-if="activeSectionIndex === sectionIndex" class="tree-items">
                <button v-for="(lesson, index) in section.lessons" :key="lesson.id || `l${index}`" :class="{ selected: activeItemKind === 'lesson' && activeItemIndex === index }" type="button" @click="selectContent('lesson', index)"><span>▶</span><b>{{ lesson.title }}</b></button>
                <button v-for="(quiz, index) in section.quizzes" :key="quiz.id || `q${index}`" :class="{ selected: activeItemKind === 'quiz' && activeItemIndex === index }" type="button" @click="selectContent('quiz', index)"><span>?</span><b>{{ quiz.title }}</b></button>
                <div class="tree-actions"><button type="button" @click="addLesson(section)">＋ Lección</button><button type="button" @click="addQuiz(section)">＋ Evaluación</button></div>
              </div>
            </article>
            <p v-if="!draft.sections?.length" class="tree-empty">Agregue el primer módulo para comenzar.</p>
          </aside>

          <section v-if="activeSection" class="curriculum-workspace">
            <article class="module-settings"><div class="module-heading"><span>MÓDULO {{ activeSectionIndex + 1 }}</span><div><button type="button" :disabled="activeSectionIndex === 0" @click="moveSection(activeSectionIndex, -1)">↑</button><button type="button" :disabled="activeSectionIndex === draft.sections.length - 1" @click="moveSection(activeSectionIndex, 1)">↓</button><button class="danger" type="button" @click="removeSection(activeSectionIndex)">Eliminar</button></div></div><input v-model="activeSection.title" class="module-title" placeholder="Nombre del módulo"><textarea v-model="activeSection.description" rows="2" placeholder="Descripción breve del módulo"></textarea></article>

            <article v-if="activeContent && activeItemKind === 'lesson'" class="content-workspace">
              <header><div><span>LECCIÓN {{ activeItemIndex + 1 }}</span><h2>{{ activeContent.title }}</h2></div><div><button type="button" :disabled="activeItemIndex === 0" @click="moveContent(activeSection, 'lesson', activeItemIndex, -1)">↑</button><button type="button" :disabled="activeItemIndex === activeSection.lessons.length - 1" @click="moveContent(activeSection, 'lesson', activeItemIndex, 1)">↓</button><button class="danger" type="button" @click="removeLesson(activeSection, activeItemIndex)">Eliminar</button></div></header>
              <div class="content-fields"><label class="span-two">Título<input v-model="activeContent.title" maxlength="240"></label><label>Tipo<select v-model="activeContent.lessonType"><option value="TEXT">Texto</option><option value="VIDEO">Video</option><option value="MIXED">Mixta</option></select></label><label>Proveedor de video<select v-model="activeContent.videoProvider"><option :value="null">Sin video</option><option value="YOUTUBE">YouTube</option><option value="UPLOAD">Video subido</option><option value="EXTERNAL">URL externa</option></select></label><label class="span-two">URL del video<input v-model="activeContent.videoUrl" placeholder="https://www.youtube.com/watch?v=..."></label><label>Duración (segundos)<input v-model.number="activeContent.durationSeconds" type="number" min="0"></label><label class="file-label">Subir video<input type="file" accept="video/*" @change="uploadVideo(activeContent, $event)"></label><label class="switch-label span-two"><input v-model="activeContent.preview" type="checkbox"><span><strong>Permitir vista previa</strong><small>La clase podrá abrirse antes de la inscripción.</small></span></label></div>
              <div class="rich-content"><div><h3>Contenido enriquecido de la lección</h3><p>Agregue objetivos, explicación, código, enlaces, imágenes, tablas y práctica.</p></div><RichTextEditor v-model="activeContent.content" :min-height="500" /></div>
            </article>

            <article v-else-if="activeContent && activeItemKind === 'quiz'" class="content-workspace quiz-workspace">
              <header><div><span>EVALUACIÓN {{ activeItemIndex + 1 }}</span><h2>{{ activeContent.title }}</h2></div><div><button type="button" :disabled="activeItemIndex === 0" @click="moveContent(activeSection, 'quiz', activeItemIndex, -1)">↑</button><button type="button" :disabled="activeItemIndex === activeSection.quizzes.length - 1" @click="moveContent(activeSection, 'quiz', activeItemIndex, 1)">↓</button><button class="danger" type="button" @click="removeQuiz(activeSection, activeItemIndex)">Eliminar</button></div></header>
              <div class="content-fields"><label class="span-two">Título<input v-model="activeContent.title"></label><label>Nota mínima (%)<input v-model.number="activeContent.passingScore" type="number" min="0" max="100"></label><label>Intentos (0 ilimitado)<input v-model.number="activeContent.maxAttempts" type="number" min="0"></label><label>Duración (minutos)<input v-model.number="activeContent.durationMinutes" type="number" min="0"></label><label class="switch-label"><input v-model="activeContent.randomizeQuestions" type="checkbox"><span><strong>Preguntas aleatorias</strong><small>Cambia el orden por intento.</small></span></label></div>
              <div class="rich-content compact"><div><h3>Introducción de la evaluación</h3></div><RichTextEditor v-model="activeContent.description" :min-height="260" /></div>
              <section class="questions-editor"><header><div><h3>Banco de preguntas</h3><p>{{ activeContent.questions.length }} preguntas · {{ activeQuizPoints }} puntos</p></div><button type="button" @click="addQuestion(activeContent)">＋ Agregar pregunta</button></header><article v-for="(question, questionIndex) in activeContent.questions" :key="question.id || questionIndex" class="question-card"><div class="question-number"><span>{{ questionIndex + 1 }}</span><button type="button" @click="removeQuestion(activeContent, questionIndex)">Eliminar</button></div><div class="question-body"><label>Pregunta<textarea v-model="question.text" rows="2"></textarea></label><div class="question-grid"><label>Tipo<select v-model="question.type"><option value="SINGLE_CHOICE">Selección única</option><option value="MULTIPLE_CHOICE">Selección múltiple</option><option value="TRUE_FALSE">Verdadero/Falso</option><option value="SHORT_TEXT">Respuesta corta</option><option value="LONG_TEXT">Respuesta abierta</option></select></label><label>Puntos<input v-model.number="question.points" type="number" min="0" step="0.5"></label></div><div v-if="question.type !== 'LONG_TEXT'" class="options-editor"><label v-for="(option, optionIndex) in question.options" :key="option.id || optionIndex"><input v-model="option.correct" type="checkbox" title="Respuesta correcta"><input v-model="option.text" placeholder="Texto de la respuesta"><button type="button" @click="removeOption(question, optionIndex)">×</button></label><button type="button" @click="addOption(question)">＋ Agregar respuesta</button></div><label>Retroalimentación<textarea v-model="question.explanation" rows="2" placeholder="Explique por qué la respuesta es correcta."></textarea></label></div></article></section>
            </article>

            <article v-else class="workspace-empty"><strong>Seleccione o agregue una lección.</strong><span>El editor aparecerá en este espacio.</span></article>
          </section>
        </main>

        <main v-else class="publish-editor">
          <section class="publish-main"><article><span>RESUMEN DEL CURSO</span><h2>{{ draft.title || 'Curso sin título' }}</h2><p>{{ draft.excerpt || 'Agregue un extracto para explicar el curso.' }}</p><div class="publish-metrics"><div><strong>{{ draft.sections?.length || 0 }}</strong><span>Módulos</span></div><div><strong>{{ lessonCount }}</strong><span>Lecciones</span></div><div><strong>{{ quizCount }}</strong><span>Evaluaciones</span></div><div><strong>{{ questionCount }}</strong><span>Preguntas</span></div></div></article><article><h3>Comprobación antes de publicar</h3><ul class="publish-checks"><li v-for="item in editorialChecks" :key="item.label" :class="{ done: item.ok }"><span>{{ item.ok ? '✓' : '!' }}</span><div><strong>{{ item.label }}</strong><small>{{ item.ok ? 'Listo' : 'Pendiente' }}</small></div></li></ul></article></section>
          <aside class="publish-sidebar"><article class="sidebar-card form-stack"><h3>Visibilidad</h3><label>Estado<select v-model="draft.status"><option value="DRAFT">Borrador</option><option value="PUBLISHED">Publicado</option><option value="ARCHIVED">Archivado</option></select></label><label class="switch-label"><input v-model="draft.featured" type="checkbox"><span><strong>Curso destacado</strong><small>Aparecerá primero en el catálogo.</small></span></label></article><article class="sidebar-card form-stack"><h3>Precio y acceso</h3><label>Tipo de acceso<select v-model="draft.accessType"><option value="FREE">Gratuito</option><option value="PAID">De pago</option><option value="PRIVATE">Privado</option></select></label><div class="price-grid"><label>Precio<input v-model.number="draft.price" type="number" min="0" step="0.01"></label><label>Moneda<input v-model="draft.currency" maxlength="3"></label></div></article><article class="sidebar-card form-stack"><h3>Origen y sincronización</h3><label>ID externo<input v-model="draft.externalSourceId"></label><label>URL de origen<input v-model="draft.externalSourceUrl" type="url"></label></article><button class="publish-button" type="button" :disabled="saving" @click="saveCourse(true)">{{ draft.status === 'PUBLISHED' ? 'Guardar curso publicado' : 'Guardar curso' }}</button></aside>
        </main>
      </div>
    </Teleport>

    <MediaPickerModal :open="mediaOpen" :kinds="['IMAGE']" title="Seleccionar portada del curso" @close="mediaOpen = false" @select="chooseFeatured" />
  </div>
</template>

<style scoped>
.academy-admin{display:grid;gap:1.25rem;min-width:0}.tabs{display:flex;gap:.35rem;overflow:auto;padding:.35rem;background:var(--surface,#fff);border:1px solid var(--border,#e5e7eb);border-radius:1rem}.tabs button{display:flex;align-items:center;gap:.45rem;border:0;background:transparent;padding:.7rem 1rem;border-radius:.7rem;font-weight:750;white-space:nowrap;cursor:pointer;color:var(--muted,#64748b)}.tabs button span{min-width:20px;height:20px;display:grid;place-items:center;border-radius:999px;background:#fef3f2;color:#b42318;font-size:.68rem}.tabs button.active{background:var(--primary,#5b4ee8);color:#fff}.space{display:grid;gap:1.25rem}.panel{background:var(--surface,#fff);border:1px solid var(--border,#e5e7eb);border-radius:1rem;padding:1.2rem;min-width:0}.panel header{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin-bottom:1rem}.panel h2{margin:0;font-size:1.1rem}.panel header p{margin:.3rem 0 0;color:var(--muted,#64748b);font-size:.82rem}.state,.empty{text-align:center;color:var(--muted,#64748b);padding:2rem}.alert{padding:.8rem 1rem;border-radius:.8rem}.alert.error{background:#fef3f2;color:#b42318}.metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1rem}.metrics article{display:grid;gap:.35rem;padding:1.25rem;border:1px solid var(--border,#e5e7eb);border-radius:1rem;background:var(--surface,#fff)}.metrics strong{font-size:2rem}.metrics span,.metrics small{color:var(--muted,#64748b)}.metrics.compact{grid-template-columns:repeat(4,1fr);margin:1rem 0}.metrics.compact article{padding:.8rem}.two-cols{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1.25rem}.align-start{align-items:start}.span-two{grid-column:1/-1}.list-row{display:flex;justify-content:space-between;gap:1rem;align-items:center;padding:.8rem 0;border-top:1px solid var(--border,#e5e7eb)}.list-row>div{display:grid;min-width:0}.list-row small{color:var(--muted,#64748b);overflow:hidden;text-overflow:ellipsis}.btn,.button{display:inline-flex;align-items:center;justify-content:center;text-decoration:none;border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);color:inherit;border-radius:.75rem;padding:.7rem 1rem;font-weight:800;cursor:pointer}.btn.primary,.button,.publish-button{border-color:var(--primary,#5b4ee8);background:var(--primary,#5b4ee8);color:#fff}.button.secondary{border-color:var(--border,#d9dee8);background:#fff;color:#334155}.btn.danger-outline{border-color:#f3b3ac;color:#b42318}.header-actions{display:flex;flex-wrap:wrap;gap:.6rem}.btn:disabled,.button:disabled{opacity:.55}.link{border:0;background:transparent;color:var(--primary,#5b4ee8);font-weight:800;cursor:pointer}.link.danger,.text-danger{color:#b42318}.toolbar{display:flex;justify-content:space-between;gap:1rem}.toolbar input{width:min(480px,100%);border:1px solid var(--border,#d9dee8);border-radius:.75rem;padding:.75rem;background:var(--surface,#fff);color:inherit}.course-admin-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1rem}.course-admin-card{border:1px solid var(--border,#e5e7eb);border-radius:1rem;overflow:hidden;background:var(--surface,#fff);min-width:0}.cover{aspect-ratio:16/8;background:linear-gradient(135deg,var(--primary,#5b4ee8),#111827);background-size:cover;background-position:center;padding:.8rem}.cover-badges{display:flex;flex-wrap:wrap;gap:.45rem}.cover span,.status{display:inline-flex;padding:.3rem .55rem;border-radius:999px;font-size:.7rem;font-weight:850;background:#eef2ff;color:#4338ca}.demo-badge{background:#fff7ed!important;color:#9a3412!important}.status.completed,.status.active,.status.completed{background:#ecfdf3;color:#027a48}.status.failed,.status.cancelled{background:#fef3f2;color:#b42318}.course-admin-card .body{padding:1rem}.course-admin-card .body>small{color:var(--primary,#5b4ee8);font-weight:800}.course-admin-card h2{font-size:1.15rem;margin:.35rem 0}.course-admin-card p{color:var(--muted,#64748b);height:3rem;overflow:hidden}.meta{display:flex;flex-wrap:wrap;gap:.7rem;font-size:.75rem;color:var(--muted,#64748b)}.course-admin-card footer{display:flex;justify-content:space-between;align-items:center;gap:.5rem;border-top:1px solid var(--border,#e5e7eb);margin-top:1rem;padding-top:1rem}.form{display:grid;gap:1rem}.form label,.form-stack label{display:grid;gap:.4rem;font-size:.82rem;font-weight:750}.form input,.form textarea,.form select,.form-stack input,.form-stack textarea,.form-stack select,.inline-form input,.inline-form select{width:100%;box-sizing:border-box;border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);color:inherit;border-radius:.7rem;padding:.72rem;outline:none}.checks,.actions{display:flex;flex-wrap:wrap;gap:1rem}.checks label,.check{display:flex!important;align-items:center;gap:.5rem!important}.checks input,.check input{width:auto}.warnings{padding:1rem;border-radius:.8rem;background:#fffaeb;color:#93370d}.inline-form{display:grid;grid-template-columns:1fr 1fr auto;align-items:end;gap:1rem}.inline-form label{display:grid;gap:.4rem;font-size:.82rem;font-weight:750}.table-wrap{overflow:auto}.table-wrap table{width:100%;border-collapse:collapse;min-width:820px}.table-wrap th,.table-wrap td{text-align:left;padding:.8rem;border-bottom:1px solid var(--border,#e5e7eb)}.progress-cell{width:100px;height:7px;background:var(--border,#e5e7eb);border-radius:999px;overflow:hidden}.progress-cell i{display:block;height:100%;background:var(--primary,#5b4ee8)}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}.review-card small{color:var(--muted,#64748b)}.review-answer{display:grid;grid-template-columns:minmax(0,1fr) 180px;gap:1rem;align-items:start;padding:1rem 0;border-top:1px solid var(--border,#e5e7eb)}.review-answer p{white-space:pre-wrap;color:var(--muted,#64748b)}.review-card footer{display:flex;justify-content:flex-end;padding-top:1rem}.review-answer label{display:grid;gap:.35rem;font-size:.8rem;font-weight:750}.preview h3{font-size:1.4rem}.preview p{color:var(--muted,#64748b)}
.course-editor-shell{position:fixed;inset:0;z-index:200;background:#f5f7fb;color:#172033;overflow:auto}.course-editor-topbar{position:sticky;top:0;z-index:8;display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:1.2rem;padding:.85rem 1.25rem;background:rgba(255,255,255,.96);backdrop-filter:blur(16px);border-bottom:1px solid #e2e8f0}.back-button{border:0;background:transparent;color:#475569;font-weight:850;cursor:pointer}.editor-title{display:grid;min-width:0}.editor-title span{font-size:.65rem;letter-spacing:.14em;color:#6366f1;font-weight:900}.editor-title strong{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.editor-actions{display:flex;gap:.55rem}.course-editor-tabs{position:sticky;top:66px;z-index:7;display:flex;justify-content:center;gap:.3rem;padding:.55rem;background:#fff;border-bottom:1px solid #e2e8f0}.course-editor-tabs button{border:0;background:transparent;padding:.7rem 1rem;border-radius:.65rem;color:#64748b;font-weight:850;cursor:pointer}.course-editor-tabs button.active{background:#eef2ff;color:#4f46e5}.editor-error{max-width:1380px;margin:1rem auto 0;padding:.8rem 1rem;border-radius:.8rem;background:#fef3f2;color:#b42318}.course-editor-main{max-width:1380px;margin:0 auto;padding:1.5rem;display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:1.5rem}.editor-primary-column,.editor-sidebar{display:grid;align-content:start;gap:1.2rem}.title-card,.editor-section,.sidebar-card,.module-settings,.content-workspace,.workspace-empty,.publish-main>article{background:#fff;border:1px solid #e2e8f0;border-radius:1rem;box-shadow:0 8px 28px rgba(15,23,42,.035)}.title-card{padding:1.3rem}.title-card>label{display:grid;gap:.5rem;font-weight:850}.title-card>label input{font-size:2rem;font-weight:850;border:0;border-bottom:1px solid #e2e8f0;padding:.7rem 0;outline:0}.title-card>div{display:grid;grid-template-columns:1fr 190px;gap:1rem;margin-top:1rem}.title-card>div label,.sidebar-card label,.content-fields label,.question-body>label,.question-grid label{display:grid;gap:.4rem;font-size:.78rem;font-weight:800;color:#475569}.title-card>div label:first-child{grid-template-columns:auto auto 1fr;align-items:center}.title-card input,.title-card select,.sidebar-card input,.sidebar-card select,.content-fields input,.content-fields select,.content-fields textarea,.module-settings input,.module-settings textarea,.question-body input,.question-body select,.question-body textarea,.publish-sidebar input,.publish-sidebar select{width:100%;box-sizing:border-box;border:1px solid #dbe2ea;border-radius:.75rem;padding:.75rem;background:#fff;color:#172033}.title-card>div label:first-child input{grid-column:1/-1}.editor-section{padding:1.2rem}.editor-section>header{display:flex;justify-content:space-between;gap:1rem;align-items:start;margin-bottom:1rem}.editor-section h2{margin:0;font-size:1.1rem}.editor-section header p{margin:.3rem 0 0;color:#64748b;font-size:.8rem}.editor-section>textarea{width:100%;box-sizing:border-box;border:1px solid #dbe2ea;border-radius:.8rem;padding:1rem}.sidebar-card{padding:1.1rem}.sidebar-card h3{margin:0 0 .8rem}.featured-picker{width:100%;overflow:hidden;border:2px dashed #dbe2ea;border-radius:.85rem;background:#f8fafc;color:#64748b;cursor:pointer}.featured-picker img{display:block;width:100%;aspect-ratio:16/9;object-fit:cover}.featured-picker>span{min-height:170px;display:grid;place-content:center;justify-items:center;gap:.4rem}.featured-picker b{font-size:2rem;color:#6366f1}.text-danger{border:0;background:transparent;margin-top:.6rem;font-weight:800;cursor:pointer}.quality-header{display:flex;justify-content:space-between;gap:1rem}.quality-header p{margin:.2rem 0;color:#64748b;font-size:.75rem}.quality-header>strong{width:48px;height:48px;display:grid;place-items:center;border-radius:999px;background:#f1f5f9}.quality-header .good{background:#ecfdf3;color:#027a48}.quality-header .medium{background:#fffaeb;color:#b54708}.quality-header .low{background:#fef3f2;color:#b42318}.quality-list{list-style:none;padding:0;margin:1rem 0 0;display:grid;gap:.55rem}.quality-list li{display:flex;gap:.55rem;color:#64748b;font-size:.78rem}.quality-list li.done{color:#027a48}.form-stack{display:grid;gap:.9rem}
.curriculum-editor{height:calc(100vh - 127px);display:grid;grid-template-columns:330px minmax(0,1fr);overflow:hidden}.curriculum-tree{overflow:auto;background:#fff;border-right:1px solid #e2e8f0;padding:1rem}.curriculum-tree>header{display:flex;justify-content:space-between;gap:.7rem;align-items:center;padding:.3rem .3rem 1rem}.curriculum-tree>header div{display:grid}.curriculum-tree>header span{font-size:.7rem;color:#64748b}.curriculum-tree button{cursor:pointer}.curriculum-tree>header button,.tree-actions button{border:1px solid #dbe2ea;background:#fff;border-radius:.65rem;padding:.55rem;color:#4f46e5;font-weight:850}.curriculum-tree article{border:1px solid #e2e8f0;border-radius:.8rem;margin-bottom:.7rem;overflow:hidden}.curriculum-tree article.active{border-color:#a5b4fc}.section-row{width:100%;display:grid;grid-template-columns:30px 1fr;gap:.65rem;text-align:left;align-items:center;border:0;background:#fff;padding:.8rem}.section-row>span{width:28px;height:28px;display:grid;place-items:center;border-radius:.6rem;background:#eef2ff;color:#4f46e5;font-weight:900}.section-row div{display:grid}.section-row small{font-size:.66rem;color:#64748b}.tree-items{display:grid;gap:.15rem;padding:.4rem;background:#f8fafc;border-top:1px solid #e2e8f0}.tree-items>button{display:grid;grid-template-columns:22px 1fr;gap:.5rem;text-align:left;border:0;background:transparent;border-radius:.55rem;padding:.55rem;color:#475569}.tree-items>button.selected{background:#e0e7ff;color:#4338ca}.tree-items b{font-size:.75rem}.tree-actions{display:flex;gap:.35rem;padding-top:.35rem}.tree-actions button{flex:1;font-size:.68rem}.tree-empty{padding:2rem;text-align:center;color:#64748b}.curriculum-workspace{overflow:auto;padding:1.4rem;display:grid;align-content:start;gap:1rem}.module-settings{padding:1rem}.module-heading,.content-workspace>header,.questions-editor>header{display:flex;justify-content:space-between;gap:1rem;align-items:center}.module-heading>span,.content-workspace>header span{font-size:.68rem;letter-spacing:.12em;color:#6366f1;font-weight:900}.module-heading button,.content-workspace>header button{border:1px solid #dbe2ea;background:#fff;border-radius:.55rem;padding:.45rem .6rem;font-weight:800}.module-heading .danger,.content-workspace>header .danger{color:#b42318}.module-title{font-size:1.5rem!important;font-weight:850;margin:.7rem 0}.content-workspace{padding:1.2rem}.content-workspace>header h2{margin:.3rem 0 0}.content-workspace>header>div:last-child{display:flex;gap:.35rem}.content-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.9rem;margin:1.2rem 0}.switch-label{display:flex!important;align-items:center;gap:.7rem!important;border:1px solid #e2e8f0;border-radius:.75rem;padding:.7rem}.switch-label input{width:auto!important}.switch-label span{display:grid}.switch-label small{font-size:.68rem;color:#64748b}.file-label input{padding:.45rem!important}.rich-content{display:grid;gap:.8rem;padding-top:1rem;border-top:1px solid #e2e8f0}.rich-content h3{margin:0}.rich-content p{margin:.25rem 0;color:#64748b;font-size:.8rem}.questions-editor{margin-top:1.2rem;border-top:1px solid #e2e8f0;padding-top:1.2rem}.questions-editor>header h3{margin:0}.questions-editor>header p{margin:.2rem 0;color:#64748b;font-size:.75rem}.questions-editor>header button{border:0;border-radius:.65rem;background:#4f46e5;color:#fff;padding:.65rem .8rem;font-weight:850}.question-card{display:grid;grid-template-columns:44px 1fr;gap:1rem;margin-top:1rem;padding:1rem;border:1px solid #e2e8f0;border-radius:.85rem;background:#f8fafc}.question-number{display:grid;align-content:start;justify-items:center;gap:.5rem}.question-number>span{width:34px;height:34px;display:grid;place-items:center;border-radius:.7rem;background:#4f46e5;color:#fff;font-weight:900}.question-number button{border:0;background:transparent;color:#b42318;font-size:.65rem}.question-body{display:grid;gap:.8rem}.question-grid{display:grid;grid-template-columns:1fr 150px;gap:.8rem}.options-editor{display:grid;gap:.45rem}.options-editor label{display:grid;grid-template-columns:auto 1fr auto;gap:.45rem;align-items:center}.options-editor label>input:first-child{width:auto}.options-editor button{border:1px dashed #cbd5e1;background:#fff;border-radius:.6rem;padding:.55rem;color:#4f46e5;font-weight:800}.workspace-empty{min-height:300px;display:grid;place-content:center;justify-items:center;color:#64748b}.workspace-empty strong{color:#172033}
.publish-editor{max-width:1180px;margin:0 auto;padding:2rem;display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:1.4rem}.publish-main,.publish-sidebar{display:grid;align-content:start;gap:1rem}.publish-main>article{padding:1.3rem}.publish-main>article>span{font-size:.68rem;letter-spacing:.12em;color:#6366f1;font-weight:900}.publish-main h2{font-size:2rem;margin:.5rem 0}.publish-main p{color:#64748b}.publish-metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:.7rem;margin-top:1.2rem}.publish-metrics div{display:grid;padding:.9rem;border-radius:.75rem;background:#f8fafc}.publish-metrics strong{font-size:1.5rem}.publish-metrics span{font-size:.7rem;color:#64748b}.publish-checks{list-style:none;padding:0;display:grid;gap:.65rem}.publish-checks li{display:flex;align-items:center;gap:.75rem;padding:.7rem;border:1px solid #fee2e2;border-radius:.7rem;color:#b42318}.publish-checks li.done{border-color:#bbf7d0;color:#027a48}.publish-checks li>span{width:28px;height:28px;display:grid;place-items:center;border-radius:.6rem;background:currentColor;color:#fff}.publish-checks li div{display:grid}.publish-checks small{font-size:.68rem}.price-grid{display:grid;grid-template-columns:1fr 90px;gap:.6rem}.publish-button{border:0;border-radius:.8rem;padding:.9rem;font-weight:900;cursor:pointer}
.mcp-area code{font-family:ui-monospace,SFMono-Regular,Menlo,monospace}.diagnostic-hero{display:flex;align-items:center;justify-content:space-between;gap:1.5rem;background:linear-gradient(135deg,#14234d,#3157d5);color:#fff}.diagnostic-hero.degraded{background:linear-gradient(135deg,#5d2805,#c05a10)}.diagnostic-hero.unknown{background:#334155}.diagnostic-hero span{font-size:.68rem;letter-spacing:.13em;font-weight:900;opacity:.72}.diagnostic-hero h2{font-size:1.65rem;margin:.35rem 0}.diagnostic-hero p{margin:0;opacity:.8}.diagnostic-hero>strong{padding:.75rem 1rem;border:1px solid rgba(255,255,255,.25);border-radius:999px;background:rgba(255,255,255,.12)}.mcp-tool{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:.25rem 1rem;padding:.9rem 0;border-top:1px solid var(--border,#e5e7eb)}.mcp-tool code{font-weight:850;color:var(--primary,#5b4ee8)}.mcp-tool p{grid-column:1;margin:0;color:var(--muted,#64748b);font-size:.78rem}.mcp-tool span{grid-column:2;grid-row:1/3;align-self:center;padding:.3rem .5rem;border-radius:999px;background:#ecfdf3;color:#027a48;font-size:.65rem;font-weight:850}.diagnostic-issue{display:grid;grid-template-columns:auto 1fr;gap:.7rem;padding:.85rem 0;border-top:1px solid var(--border,#e5e7eb)}.diagnostic-issue>span{height:max-content;padding:.25rem .45rem;border-radius:999px;background:#fffaeb;color:#b54708;font-size:.62rem;font-weight:900;text-transform:uppercase}.diagnostic-issue.error>span{background:#fef3f2;color:#b42318}.diagnostic-issue p{margin:.25rem 0;color:var(--muted,#64748b);font-size:.78rem}.diagnostic-issue small{color:var(--muted,#64748b);font-size:.65rem}.recommendations{margin:1rem 0 0;padding:1rem 1rem 1rem 2rem;border-radius:.8rem;background:#f8fafc;color:#475569;font-size:.8rem}
@media(max-width:1100px){.course-admin-grid{grid-template-columns:repeat(2,1fr)}.course-editor-main,.publish-editor{grid-template-columns:1fr}.editor-sidebar{grid-template-columns:repeat(2,1fr)}.editor-sidebar .sidebar-card:last-child{grid-column:1/-1}.curriculum-editor{grid-template-columns:280px minmax(0,1fr)}}
@media(max-width:800px){.metrics,.two-cols,.form-grid,.review-answer{grid-template-columns:1fr}.metrics.compact{grid-template-columns:repeat(2,1fr)}.span-two{grid-column:auto}.inline-form{grid-template-columns:1fr}.course-admin-grid{grid-template-columns:1fr}.course-editor-topbar{grid-template-columns:1fr auto}.editor-title{display:none}.editor-actions .secondary:first-child{display:none}.course-editor-tabs{justify-content:flex-start;overflow:auto}.curriculum-editor{height:auto;min-height:calc(100vh - 127px);grid-template-columns:1fr;overflow:visible}.curriculum-tree{max-height:360px;border-right:0;border-bottom:1px solid #e2e8f0}.curriculum-workspace{overflow:visible}.content-fields{grid-template-columns:1fr}.content-fields .span-two{grid-column:auto}.publish-metrics{grid-template-columns:repeat(2,1fr)}}
@media(max-width:560px){.header-actions{width:100%}.header-actions .btn{flex:1}.toolbar{align-items:stretch;flex-direction:column}.course-editor-topbar{padding:.65rem;gap:.5rem}.back-button{font-size:.75rem}.editor-actions{gap:.3rem}.editor-actions .button{padding:.6rem;font-size:.7rem}.course-editor-main,.publish-editor{padding:.8rem}.editor-sidebar{grid-template-columns:1fr}.editor-sidebar .sidebar-card:last-child{grid-column:auto}.title-card>div{grid-template-columns:1fr}.question-card{grid-template-columns:1fr}.question-number{display:flex;justify-content:space-between}.question-grid{grid-template-columns:1fr}.module-heading,.content-workspace>header{align-items:flex-start}.content-workspace>header{display:grid}.publish-metrics{grid-template-columns:1fr 1fr}}
</style>
