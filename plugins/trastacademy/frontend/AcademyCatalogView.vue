<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'

type Course = Record<string, any>

const route = useRoute()
const loading = ref(true)
const error = ref('')
const warning = ref('')
const search = ref('')
const category = ref('ALL')
const level = ref('ALL')
const access = ref('ALL')
const duration = ref('ALL')
const sort = ref('FEATURED')
const filtersOpen = ref(false)
const settings = ref<any>({ catalogTitle: 'Academia', catalogDescription: '' })
const courses = ref<Course[]>([])

const categories = computed(() => Array.from(new Set(courses.value.map(course => course.category || 'General'))).sort())
const instructors = computed(() => new Set(courses.value.map(course => course.instructorName).filter(Boolean)).size)
const totalLessons = computed(() => courses.value.reduce((total, course) => total + Number(course.lessonCount || 0), 0))
const totalMinutes = computed(() => courses.value.reduce((total, course) => total + Number(course.estimatedMinutes || 0), 0))
const featuredCourse = computed(() => courses.value.find(course => course.featured) || courses.value[0] || null)

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  const rows = courses.value.filter(course => {
    const haystack = `${course.title} ${course.excerpt || ''} ${course.instructorName || ''} ${course.category || ''}`.toLowerCase()
    if (term && !haystack.includes(term)) return false
    if (category.value !== 'ALL' && (course.category || 'General') !== category.value) return false
    if (level.value !== 'ALL' && course.level !== level.value) return false
    if (access.value !== 'ALL' && course.accessType !== access.value) return false
    const minutes = Number(course.estimatedMinutes || 0)
    if (duration.value === 'SHORT' && minutes > 120) return false
    if (duration.value === 'MEDIUM' && (minutes <= 120 || minutes > 300)) return false
    if (duration.value === 'LONG' && minutes <= 300) return false
    return true
  })

  return [...rows].sort((a, b) => {
    if (sort.value === 'NEWEST') return new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime()
    if (sort.value === 'TITLE') return String(a.title).localeCompare(String(b.title), 'es')
    if (sort.value === 'DURATION_ASC') return Number(a.estimatedMinutes || 0) - Number(b.estimatedMinutes || 0)
    if (sort.value === 'PRICE_ASC') return Number(a.price || 0) - Number(b.price || 0)
    return Number(Boolean(b.featured)) - Number(Boolean(a.featured)) || Number(b.enrollmentCount || 0) - Number(a.enrollmentCount || 0)
  })
})

const activeFilters = computed(() => [
  category.value !== 'ALL' ? category.value : '',
  level.value !== 'ALL' ? levelLabel(level.value) : '',
  access.value !== 'ALL' ? accessLabel(access.value) : '',
  duration.value !== 'ALL' ? durationLabel(duration.value) : ''
].filter(Boolean))

onMounted(() => {
  search.value = typeof route.query.q === 'string' ? route.query.q : ''
  load()
})
watch(() => route.query.q, value => { search.value = typeof value === 'string' ? value : '' })

function unwrap(payload: any) {
  if (payload?.data && typeof payload.data === 'object') return payload.data
  if (payload?.results && typeof payload.results === 'object') return payload.results
  return payload
}

async function load() {
  loading.value = true
  error.value = ''
  warning.value = ''
  const [settingsResult, coursesResult] = await Promise.allSettled([
    api<any>('/api/public/academy/settings'),
    api<any>('/api/public/academy/courses?size=100')
  ])

  if (settingsResult.status === 'fulfilled') {
    settings.value = { ...settings.value, ...(unwrap(settingsResult.value) || {}) }
  } else {
    warning.value = 'La configuración personalizada no respondió; se está utilizando la presentación predeterminada.'
  }

  if (coursesResult.status === 'fulfilled') {
    const response = unwrap(coursesResult.value)
    const rows = Array.isArray(response) ? response : Array.isArray(response?.content) ? response.content : []
    courses.value = rows.filter((course: Course) => course && course.id && course.slug)
    if (!courses.value.length) warning.value = warning.value || 'La academia respondió correctamente, pero todavía no tiene cursos publicados.'
  } else {
    const reason = coursesResult.reason
    error.value = reason instanceof Error ? reason.message : 'No se pudo cargar el catálogo de cursos'
  }

  loading.value = false
}

function clearFilters() {
  search.value = ''
  category.value = 'ALL'
  level.value = 'ALL'
  access.value = 'ALL'
  duration.value = 'ALL'
  sort.value = 'FEATURED'
}

function money(value: number, currency = 'USD') {
  return new Intl.NumberFormat('es-US', { style: 'currency', currency }).format(value || 0)
}

function levelLabel(value = '') {
  return value === 'BEGINNER' ? 'Inicial' : value === 'INTERMEDIATE' ? 'Intermedio' : value === 'ADVANCED' ? 'Avanzado' : 'Todos los niveles'
}

function accessLabel(value = '') {
  return value === 'FREE' ? 'Gratis' : value === 'PAID' ? 'De pago' : 'Acceso privado'
}

function durationLabel(value = '') {
  return value === 'SHORT' ? 'Hasta 2 horas' : value === 'MEDIUM' ? 'De 2 a 5 horas' : 'Más de 5 horas'
}

function timeLabel(minutes = 0) {
  if (!minutes) return 'A su ritmo'
  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  if (!hours) return `${minutes} min`
  return remaining ? `${hours} h ${remaining} min` : `${hours} h`
}
</script>

<template>
  <main class="academy-page">
    <section class="academy-hero">
      <div class="hero-copy">
        <span class="eyebrow">TRAST ACADEMY · APRENDIZAJE PRÁCTICO</span>
        <h1>{{ settings.catalogTitle || 'Aprenda, practique y construya' }}</h1>
        <p>{{ settings.catalogDescription || 'Cursos organizados por módulos, clases en video, prácticas y evaluaciones para avanzar con resultados visibles.' }}</p>
        <div class="hero-actions">
          <a href="#catalogo" class="primary-action">Explorar cursos</a>
          <RouterLink v-if="featuredCourse" :to="`/academy/${featuredCourse.slug}`" class="secondary-action">Ver curso destacado</RouterLink>
        </div>
        <div class="hero-stats">
          <article><strong>{{ courses.length }}</strong><span>Cursos disponibles</span></article>
          <article><strong>{{ totalLessons }}</strong><span>Lecciones</span></article>
          <article><strong>{{ Math.round(totalMinutes / 60) }} h</strong><span>Contenido formativo</span></article>
          <article><strong>{{ instructors }}</strong><span>Instructores</span></article>
        </div>
      </div>

      <RouterLink v-if="featuredCourse" :to="`/academy/${featuredCourse.slug}`" class="featured-panel">
        <div class="featured-cover" :style="featuredCourse.featuredImageUrl ? { backgroundImage: `url(${featuredCourse.featuredImageUrl})` } : {}">
          <span>Curso destacado</span>
        </div>
        <div class="featured-body">
          <small>{{ featuredCourse.category || 'General' }} · {{ levelLabel(featuredCourse.level) }}</small>
          <h2>{{ featuredCourse.title }}</h2>
          <p>{{ featuredCourse.excerpt }}</p>
          <div><span>{{ featuredCourse.lessonCount }} clases</span><span>{{ featuredCourse.quizCount }} evaluaciones</span><span>{{ timeLabel(featuredCourse.estimatedMinutes) }}</span></div>
          <footer><strong>{{ featuredCourse.accessType === 'FREE' ? 'Acceso gratuito' : money(featuredCourse.price, featuredCourse.currency) }}</strong><b>Comenzar →</b></footer>
        </div>
      </RouterLink>
    </section>

    <section class="value-strip">
      <article><span>01</span><div><strong>Aprenda a su ritmo</strong><p>Continúe exactamente desde la última clase.</p></div></article>
      <article><span>02</span><div><strong>Practique por módulos</strong><p>Objetivos, ejercicios y contenido estructurado.</p></div></article>
      <article><span>03</span><div><strong>Compruebe su avance</strong><p>Parciales, examen final y progreso medible.</p></div></article>
    </section>
    <div v-if="warning" class="catalog-notice"><span>i</span><p>{{ warning }}</p><button type="button" @click="load">Actualizar</button></div>

    <section id="catalogo" class="catalog-section">
      <header class="catalog-heading">
        <div><span class="eyebrow dark">CATÁLOGO FORMATIVO</span><h2>Encuentre el curso adecuado</h2><p>Filtre por área, nivel, acceso y duración.</p></div>
        <div class="catalog-search"><span>⌕</span><input v-model="search" type="search" placeholder="Buscar por curso, instructor o tema…"></div>
      </header>

      <div class="category-chips" aria-label="Categorías">
        <button :class="{ active: category === 'ALL' }" type="button" @click="category = 'ALL'">Todos</button>
        <button v-for="item in categories" :key="item" :class="{ active: category === item }" type="button" @click="category = item">{{ item }}</button>
      </div>

      <div class="catalog-toolbar">
        <button class="filter-toggle" type="button" @click="filtersOpen = !filtersOpen">☰ Filtros <span v-if="activeFilters.length">{{ activeFilters.length }}</span></button>
        <p><strong>{{ filtered.length }}</strong> curso{{ filtered.length === 1 ? '' : 's' }} encontrado{{ filtered.length === 1 ? '' : 's' }}</p>
        <label>Ordenar por<select v-model="sort"><option value="FEATURED">Destacados</option><option value="NEWEST">Más recientes</option><option value="TITLE">Título A–Z</option><option value="DURATION_ASC">Menor duración</option><option value="PRICE_ASC">Menor precio</option></select></label>
      </div>

      <div v-if="activeFilters.length" class="active-filters"><span v-for="item in activeFilters" :key="item">{{ item }}</span><button type="button" @click="clearFilters">Limpiar filtros</button></div>

      <div class="catalog-layout">
        <aside :class="['filter-panel', { open: filtersOpen }]">
          <div class="filter-title"><strong>Filtrar cursos</strong><button type="button" @click="clearFilters">Restablecer</button></div>
          <label>Categoría<select v-model="category"><option value="ALL">Todas las categorías</option><option v-for="item in categories" :key="item" :value="item">{{ item }}</option></select></label>
          <label>Nivel<select v-model="level"><option value="ALL">Todos los niveles</option><option value="BEGINNER">Inicial</option><option value="INTERMEDIATE">Intermedio</option><option value="ADVANCED">Avanzado</option><option value="ALL_LEVELS">Para todos</option></select></label>
          <label>Tipo de acceso<select v-model="access"><option value="ALL">Todos</option><option value="FREE">Gratuitos</option><option value="PAID">De pago</option><option value="PRIVATE">Privados</option></select></label>
          <label>Duración<select v-model="duration"><option value="ALL">Cualquier duración</option><option value="SHORT">Hasta 2 horas</option><option value="MEDIUM">De 2 a 5 horas</option><option value="LONG">Más de 5 horas</option></select></label>
          <div class="filter-help"><strong>¿No sabe por dónde empezar?</strong><p>Comience con un curso de nivel inicial y avance por módulos.</p></div>
        </aside>

        <div class="results-area">
          <div v-if="loading" class="state-card"><span class="loader" />Cargando cursos…</div>
          <div v-else-if="error && !courses.length" class="state-card error"><strong>No se pudo abrir el catálogo</strong><span>{{ error }}</span><button type="button" @click="load">Reintentar</button></div>
          <div v-else-if="!filtered.length" class="state-card"><strong>No encontramos cursos con esos filtros.</strong><span>Pruebe con una categoría o nivel diferente.</span><button type="button" @click="clearFilters">Ver todos los cursos</button></div>

          <div v-else class="course-grid">
            <RouterLink v-for="course in filtered" :key="course.id" :to="`/academy/${course.slug}`" class="course-card">
              <div class="course-cover" :style="course.featuredImageUrl ? { backgroundImage: `url(${course.featuredImageUrl})` } : {}">
                <div class="cover-badges"><span>{{ course.category || 'General' }}</span><span v-if="course.featured" class="featured-badge">Destacado</span></div>
                <strong>{{ course.accessType === 'FREE' ? 'Gratis' : money(course.price, course.currency) }}</strong>
              </div>
              <div class="course-body">
                <div class="course-topline"><span>{{ levelLabel(course.level) }}</span><span>{{ timeLabel(course.estimatedMinutes) }}</span></div>
                <h3>{{ course.title }}</h3>
                <p>{{ course.excerpt || 'Curso práctico con contenido organizado, lecciones y seguimiento de progreso.' }}</p>
                <div class="instructor"><span>{{ (course.instructorName || 'NA').slice(0, 2).toUpperCase() }}</span><div><small>Instructor</small><strong>{{ course.instructorName || 'Academy' }}</strong></div></div>
                <div class="course-meta"><span>▤ {{ course.lessonCount }} lecciones</span><span>✓ {{ course.quizCount }} evaluaciones</span><span>▦ {{ course.sectionCount }} módulos</span></div>
                <footer><strong>{{ course.accessType === 'FREE' ? 'Inscribirse gratis' : 'Ver información' }}</strong><span>Ver curso →</span></footer>
              </div>
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section class="learning-path">
      <div><span class="eyebrow dark">UNA EXPERIENCIA COMPLETA</span><h2>Más que una lista de videos</h2><p>Cada curso reúne teoría, práctica, evaluación y seguimiento para convertir el contenido en aprendizaje aplicable.</p></div>
      <div class="path-steps"><article><b>1</b><strong>Seleccione</strong><span>Filtre por tema y nivel.</span></article><article><b>2</b><strong>Aprenda</strong><span>Avance por módulos y videos.</span></article><article><b>3</b><strong>Practique</strong><span>Realice ejercicios y parciales.</span></article><article><b>4</b><strong>Complete</strong><span>Mida su progreso y resultado.</span></article></div>
    </section>
  </main>
</template>

<style scoped>
.academy-page{min-height:70vh;background:#f7f8fc;color:var(--text,#172033)}
.academy-hero{padding:clamp(3.5rem,7vw,7rem) max(1.25rem,calc((100vw - 1240px)/2));display:grid;grid-template-columns:minmax(0,1.15fr) minmax(360px,.85fr);gap:clamp(2rem,5vw,5rem);align-items:center;background:radial-gradient(circle at 88% 10%,rgba(255,255,255,.18),transparent 26%),linear-gradient(135deg,color-mix(in srgb,var(--primary,#5b4ee8) 92%,#111827),#111827);color:#fff;overflow:hidden}.eyebrow{font-size:.72rem;letter-spacing:.18em;font-weight:900}.eyebrow.dark{color:var(--primary,#5b4ee8)}.hero-copy h1{font-size:clamp(2.7rem,5vw,5.2rem);line-height:.96;margin:.9rem 0 1.2rem;max-width:800px}.hero-copy>p{max-width:720px;font-size:1.08rem;opacity:.83;line-height:1.75}.hero-actions{display:flex;flex-wrap:wrap;gap:.8rem;margin:1.8rem 0}.hero-actions a{display:inline-flex;text-decoration:none;border-radius:.85rem;padding:.9rem 1.15rem;font-weight:900}.primary-action{background:#fff;color:#172033}.secondary-action{border:1px solid rgba(255,255,255,.35);color:#fff}.hero-stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.8rem;margin-top:2rem}.hero-stats article{display:grid;gap:.15rem;padding:.8rem 0;border-top:1px solid rgba(255,255,255,.25)}.hero-stats strong{font-size:1.45rem}.hero-stats span{font-size:.72rem;opacity:.7}
.featured-panel{display:block;text-decoration:none;color:#172033;border:1px solid rgba(255,255,255,.4);background:#fff;border-radius:1.4rem;overflow:hidden;box-shadow:0 24px 70px rgba(0,0,0,.28);transition:.25s}.featured-panel:hover{transform:translateY(-5px);box-shadow:0 30px 80px rgba(0,0,0,.34)}.featured-cover{aspect-ratio:16/8;background:linear-gradient(135deg,#ced7ff,#3049bf);background-size:cover;background-position:center;padding:1rem}.featured-cover span{display:inline-flex;background:#fff;color:#172033;border-radius:999px;padding:.4rem .7rem;font-size:.72rem;font-weight:900;box-shadow:0 8px 20px rgba(15,23,42,.12)}.featured-body{padding:1.35rem;background:#fff}.featured-body small{font-weight:900;color:var(--primary,#3157d5)}.featured-body h2{font-size:1.55rem;line-height:1.15;margin:.55rem 0;color:#172033}.featured-body p{color:#536078;line-height:1.55}.featured-body>div{display:flex;flex-wrap:wrap;gap:.5rem;margin:1rem 0}.featured-body>div span{font-size:.72rem;background:#eef2ff;color:#3f4c68;border-radius:999px;padding:.4rem .6rem;font-weight:750}.featured-body footer{display:flex;justify-content:space-between;gap:1rem;padding-top:1rem;border-top:1px solid #e5eaf4}.featured-body footer strong,.featured-body footer b{color:var(--primary,#3157d5)}
.value-strip{max-width:1240px;margin:-1.5rem auto 0;position:relative;z-index:2;display:grid;grid-template-columns:repeat(3,1fr);background:var(--surface,#fff);border:1px solid var(--border,#e5e7eb);border-radius:1.1rem;box-shadow:0 18px 45px rgba(15,23,42,.1)}.value-strip article{display:flex;gap:1rem;padding:1.35rem}.value-strip article+article{border-left:1px solid var(--border,#e5e7eb)}.value-strip article>span{width:38px;height:38px;display:grid;place-items:center;flex:none;border-radius:.75rem;background:color-mix(in srgb,var(--primary,#5b4ee8) 10%,#fff);color:var(--primary,#5b4ee8);font-weight:900}.value-strip div{display:grid;gap:.2rem}.value-strip p{margin:0;font-size:.78rem;color:var(--muted,#64748b);line-height:1.45}
.catalog-notice{max-width:1240px;margin:1rem auto 0;display:grid;grid-template-columns:32px 1fr auto;gap:.75rem;align-items:center;padding:.8rem 1rem;border:1px solid #f3d38b;border-radius:.85rem;background:#fffaeb;color:#8a4b08}.catalog-notice>span{width:28px;height:28px;display:grid;place-items:center;border-radius:.6rem;background:#f59e0b;color:#fff;font-weight:900}.catalog-notice p{margin:0;font-size:.8rem}.catalog-notice button{border:0;background:transparent;color:inherit;font-weight:900;cursor:pointer}
.catalog-section{max-width:1240px;margin:auto;padding:clamp(3rem,6vw,6rem) 1.25rem}.catalog-heading{display:flex;justify-content:space-between;align-items:end;gap:2rem}.catalog-heading h2,.learning-path h2{font-size:clamp(2rem,4vw,3.2rem);margin:.55rem 0}.catalog-heading p{margin:0;color:var(--muted,#64748b)}.catalog-search{width:min(430px,100%);display:flex;align-items:center;gap:.6rem;border:1px solid var(--border,#d9dee8);border-radius:1rem;background:var(--surface,#fff);padding:.85rem 1rem;box-shadow:0 8px 25px rgba(15,23,42,.04)}.catalog-search input{width:100%;border:0;outline:0;background:transparent;color:inherit}.category-chips{display:flex;gap:.55rem;overflow:auto;padding:1.5rem 0 .8rem}.category-chips button{white-space:nowrap;border:1px solid var(--border,#d9dee8);border-radius:999px;background:var(--surface,#fff);padding:.58rem .85rem;font-weight:800;color:var(--muted,#64748b);cursor:pointer}.category-chips button.active{border-color:var(--primary,#5b4ee8);background:var(--primary,#5b4ee8);color:#fff}.catalog-toolbar{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:1rem;padding:1rem 0;border-top:1px solid var(--border,#e5e7eb);border-bottom:1px solid var(--border,#e5e7eb)}.catalog-toolbar p{margin:0;color:var(--muted,#64748b)}.catalog-toolbar label{display:flex;align-items:center;gap:.6rem;font-size:.78rem;font-weight:800}.catalog-toolbar select,.filter-panel select{border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);color:inherit;border-radius:.7rem;padding:.65rem}.filter-toggle{display:none;border:1px solid var(--border,#d9dee8);background:var(--surface,#fff);border-radius:.75rem;padding:.65rem .8rem;font-weight:850}.filter-toggle span{display:inline-grid;place-items:center;min-width:21px;height:21px;border-radius:999px;background:var(--primary,#5b4ee8);color:#fff}.active-filters{display:flex;flex-wrap:wrap;gap:.5rem;padding-top:1rem}.active-filters span{border-radius:999px;background:color-mix(in srgb,var(--primary,#5b4ee8) 9%,#fff);color:var(--primary,#5b4ee8);padding:.42rem .65rem;font-size:.75rem;font-weight:850}.active-filters button{border:0;background:transparent;color:var(--muted,#64748b);font-weight:800;cursor:pointer}
.catalog-layout{display:grid;grid-template-columns:245px minmax(0,1fr);gap:1.5rem;padding-top:1.5rem}.filter-panel{align-self:start;position:sticky;top:100px;display:grid;gap:1rem;border:1px solid var(--border,#e5e7eb);border-radius:1rem;background:var(--surface,#fff);padding:1.15rem}.filter-title{display:flex;justify-content:space-between;gap:1rem}.filter-title button{border:0;background:transparent;color:var(--primary,#5b4ee8);font-size:.72rem;font-weight:850;cursor:pointer}.filter-panel label{display:grid;gap:.4rem;font-size:.76rem;font-weight:850;color:var(--muted,#64748b)}.filter-help{padding:1rem;border-radius:.85rem;background:color-mix(in srgb,var(--primary,#5b4ee8) 8%,#fff)}.filter-help p{margin:.35rem 0 0;font-size:.75rem;line-height:1.5;color:var(--muted,#64748b)}.course-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1.25rem}.course-card{display:flex;flex-direction:column;min-width:0;border:1px solid var(--border,#e5e7eb);border-radius:1.15rem;overflow:hidden;text-decoration:none;color:inherit;background:var(--surface,#fff);box-shadow:0 10px 30px rgba(15,23,42,.055);transition:.22s}.course-card:hover{transform:translateY(-4px);border-color:color-mix(in srgb,var(--primary,#5b4ee8) 35%,var(--border,#e5e7eb));box-shadow:0 18px 45px rgba(15,23,42,.1)}.course-cover{aspect-ratio:16/8;background:linear-gradient(135deg,color-mix(in srgb,var(--primary,#5b4ee8) 18%,#fff),color-mix(in srgb,var(--primary,#5b4ee8) 65%,#111827));background-size:cover;background-position:center;padding:1rem;display:flex;flex-direction:column;justify-content:space-between;align-items:flex-start}.cover-badges{display:flex;flex-wrap:wrap;gap:.4rem}.course-cover span,.course-cover>strong{display:inline-flex;background:rgba(255,255,255,.93);color:#172033;padding:.38rem .65rem;border-radius:999px;font-size:.7rem;font-weight:900}.course-cover .featured-badge{background:#172033;color:#fff}.course-body{padding:1.2rem;display:flex;flex:1;flex-direction:column}.course-topline{display:flex;justify-content:space-between;gap:1rem;font-size:.72rem;font-weight:800;color:var(--primary,#5b4ee8)}.course-body h3{font-size:1.25rem;line-height:1.2;margin:.55rem 0}.course-body>p{color:var(--muted,#64748b);line-height:1.6;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}.instructor{display:flex;align-items:center;gap:.65rem;margin:.9rem 0}.instructor>span{width:34px;height:34px;display:grid;place-items:center;border-radius:999px;background:#edf1fb;color:var(--primary,#5b4ee8);font-size:.7rem;font-weight:900}.instructor div{display:grid}.instructor small{font-size:.65rem;color:var(--muted,#64748b)}.instructor strong{font-size:.78rem}.course-meta{display:flex;flex-wrap:wrap;gap:.7rem;font-size:.7rem;color:var(--muted,#64748b);margin-top:auto}.course-body footer{display:flex;justify-content:space-between;gap:1rem;margin-top:1rem;padding-top:1rem;border-top:1px solid var(--border,#e5e7eb);font-size:.82rem}.course-body footer span{color:var(--primary,#5b4ee8);font-weight:900}.state-card{min-height:300px;display:grid;place-content:center;justify-items:center;gap:.7rem;text-align:center;border:1px dashed var(--border,#d9dee8);border-radius:1.1rem;color:var(--muted,#64748b);padding:2rem}.state-card.error{color:#b42318}.state-card button{border:0;border-radius:.75rem;background:var(--primary,#5b4ee8);color:#fff;padding:.7rem .9rem;font-weight:850}.loader{width:30px;height:30px;border:3px solid #dce4f4;border-top-color:var(--primary,#5b4ee8);border-radius:999px;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
.learning-path{padding:clamp(3rem,7vw,7rem) max(1.25rem,calc((100vw - 1240px)/2));display:grid;grid-template-columns:minmax(0,.8fr) minmax(0,1.2fr);gap:4rem;align-items:center;background:var(--surface,#fff);border-top:1px solid var(--border,#e5e7eb)}.learning-path p{color:var(--muted,#64748b);line-height:1.7}.path-steps{display:grid;grid-template-columns:repeat(2,1fr);gap:1rem}.path-steps article{display:grid;gap:.25rem;padding:1.2rem;border:1px solid var(--border,#e5e7eb);border-radius:1rem}.path-steps b{width:32px;height:32px;display:grid;place-items:center;border-radius:.7rem;background:var(--primary,#5b4ee8);color:#fff}.path-steps span{font-size:.78rem;color:var(--muted,#64748b)}
@media(max-width:1000px){.academy-hero{grid-template-columns:1fr}.featured-panel{max-width:720px}.value-strip{margin:0;border-radius:0;border-left:0;border-right:0}.catalog-layout{grid-template-columns:1fr}.filter-toggle{display:inline-flex;align-items:center;gap:.5rem}.filter-panel{display:none;position:static}.filter-panel.open{display:grid}.course-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.learning-path{grid-template-columns:1fr}}
@media(max-width:760px){.value-strip{grid-template-columns:1fr}.value-strip article+article{border-left:0;border-top:1px solid var(--border,#e5e7eb)}.catalog-heading{display:grid}.catalog-search{width:100%}.catalog-toolbar{grid-template-columns:auto 1fr}.catalog-toolbar label{grid-column:1/-1;justify-content:space-between}.course-grid{grid-template-columns:1fr}.hero-stats{grid-template-columns:repeat(2,1fr)}.path-steps{grid-template-columns:1fr}}
@media(max-width:480px){.academy-hero{padding-top:3rem;padding-bottom:3rem}.hero-copy h1{font-size:2.6rem}.hero-actions a{width:100%;justify-content:center}.featured-body footer{display:grid}.catalog-toolbar{grid-template-columns:1fr}.catalog-toolbar p{order:-1}.filter-toggle{justify-content:center}.hero-stats{gap:.5rem}.hero-stats article{padding:.6rem 0}}
</style>
