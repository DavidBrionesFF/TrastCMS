<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

type Tab = 'overview' | 'tours' | 'calendar' | 'bookings' | 'catalog' | 'extras' | 'settings'
type Row = Record<string, any>
type Page<T> = { content: T[]; totalElements: number }

const toast = useToastStore()
const tab = ref<Tab>('overview')
const loading = ref(true)
const saving = ref(false)
const demoBusy = ref(false)
const error = ref('')
const search = ref('')
const calendarCursor = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const dashboard = ref<Row>({})
const tours = ref<Row[]>([])
const destinations = ref<Row[]>([])
const activities = ref<Row[]>([])
const extras = ref<Row[]>([])
const departures = ref<Row[]>([])
const bookings = ref<Row[]>([])
const settings = reactive<Row>({})
const editor = ref<'tour'|'departure'|'destination'|'activity'|'extra'|''>('')
const draft = reactive<Row>({})

const tabs: Array<{ id: Tab; label: string; icon: string }> = [
  { id: 'overview', label: 'Resumen', icon: 'dashboard' },
  { id: 'tours', label: 'Tours', icon: 'map' },
  { id: 'calendar', label: 'Calendario', icon: 'calendar' },
  { id: 'bookings', label: 'Reservaciones', icon: 'document' },
  { id: 'catalog', label: 'Destinos y actividades', icon: 'grid' },
  { id: 'extras', label: 'Servicios adicionales', icon: 'plus' },
  { id: 'settings', label: 'Configuración', icon: 'settings' }
]

const filteredTours = computed(() => {
  const term = search.value.trim().toLowerCase()
  return !term ? tours.value : tours.value.filter(item => `${item.name} ${item.code || ''} ${item.destination?.name || ''}`.toLowerCase().includes(term))
})

const calendarTitle = computed(() => calendarCursor.value.toLocaleDateString('es-HN', { month: 'long', year: 'numeric' }))
const calendarWeekdays = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']
const calendarDays = computed(() => {
  const first = new Date(calendarCursor.value.getFullYear(), calendarCursor.value.getMonth(), 1)
  const start = new Date(first)
  start.setDate(first.getDate() - first.getDay())
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    const key = localDateKey(date)
    const dayDepartures = departures.value.filter(item => localDateKey(new Date(item.startsAt)) === key)
    const dayBookings = bookings.value.filter(item =>
      localDateKey(new Date(item.startsAt)) === key
      && !['CANCELLED', 'EXPIRED'].includes(item.bookingStatus)
    )
    return {
      key,
      date,
      currentMonth: date.getMonth() === calendarCursor.value.getMonth(),
      departures: dayDepartures,
      bookings: dayBookings
    }
  })
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [dash, tourPage, destinationRows, activityRows, extraRows, departureRows, bookingPage, currentSettings] = await Promise.all([
      api<Row>('/api/admin/travel/dashboard'),
      api<Page<Row>>('/api/admin/travel/tours?size=200'),
      api<Row[]>('/api/admin/travel/destinations'),
      api<Row[]>('/api/admin/travel/activities'),
      api<Row[]>('/api/admin/travel/extras'),
      api<Row[]>('/api/admin/travel/departures'),
      api<Page<Row>>('/api/admin/travel/bookings?size=200'),
      api<Row>('/api/admin/travel/settings')
    ])
    dashboard.value = dash
    tours.value = tourPage.content
    destinations.value = destinationRows
    activities.value = activityRows
    extras.value = extraRows
    departures.value = departureRows
    bookings.value = bookingPage.content
    Object.assign(settings, currentSettings)
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar Trast Travel'
  } finally {
    loading.value = false
  }
}

function emptyTour(): Row {
  return {
    name: '', slug: '', code: '', shortDescription: '', description: '', destinationId: '', activityIds: [],
    includedText: 'Guía local\nTransporte durante el tour', excludedText: 'Propinas\nGastos personales',
    itineraryText: 'Día 1|Encuentro y bienvenida|Recibimiento del grupo y explicación del recorrido.',
    faqText: '¿Qué debo llevar?|Ropa cómoda, protector solar y agua.', galleryText: '',
    durationValue: 4, durationUnit: 'HOUR', difficulty: 'Fácil', minimumAge: 0, maximumAge: 90,
    minimumGroupSize: 1, maximumGroupSize: 20, adultPrice: 75, childPrice: 45, infantPrice: 0,
    currency: settings.defaultCurrency || 'USD', pricingMode: 'PER_PERSON', depositPercentage: 30,
    featuredImageUrl: '', status: 'DRAFT', featured: false, confirmationMode: 'INSTANT',
    taxExempt: false, taxes: []
  }
}

function openTour(item?: Row) {
  Object.keys(draft).forEach(key => delete draft[key])
  const value = item ? {
    ...item,
    destinationId: item.destination?.id || '',
    activityIds: (item.activities || []).map((activity: Row) => activity.id),
    includedText: (item.included || []).join('\n'),
    excludedText: (item.excluded || []).join('\n'),
    itineraryText: (item.itinerary || []).map((row: Row) => `${row.title || row.day || ''}|${row.subtitle || row.time || ''}|${row.description || ''}`).join('\n'),
    faqText: (item.faqs || []).map((row: Row) => `${row.question || ''}|${row.answer || ''}`).join('\n'),
    galleryText: (item.gallery || []).join('\n'),
    taxExempt: Boolean(item.taxExempt),
    taxes: Array.isArray(item.taxes) ? item.taxes.map((tax: Row) => ({ name: tax.name || '', rate: Number(tax.rate || 0) })) : []
  } : emptyTour()
  Object.assign(draft, value)
  editor.value = 'tour'
}

function openSimple(type: 'destination'|'activity'|'extra', item?: Row) {
  Object.keys(draft).forEach(key => delete draft[key])
  if (type === 'destination') Object.assign(draft, item || { name: '', slug: '', summary: '', description: '', imageUrl: '', country: 'Honduras', region: '', city: '', latitude: null, longitude: null, featured: false, active: true })
  if (type === 'activity') Object.assign(draft, item || { name: '', slug: '', description: '', icon: '🌴', imageUrl: '', active: true })
  if (type === 'extra') Object.assign(draft, item || { name: '', slug: '', description: '', price: 0, currency: settings.defaultCurrency || 'USD', pricingUnit: 'PER_BOOKING', maxQuantity: 10, imageUrl: '', active: true })
  editor.value = type
}

function openDeparture(item?: Row) {
  Object.keys(draft).forEach(key => delete draft[key])
  Object.assign(draft, item ? {
    ...item,
    startsAtLocal: toLocalInput(item.startsAt),
    endsAtLocal: item.endsAt ? toLocalInput(item.endsAt) : ''
  } : {
    tourId: tours.value[0]?.id || '', startsAtLocal: toLocalInput(new Date(Date.now() + 86400000).toISOString()), endsAtLocal: '',
    capacity: 20, blockedSeats: 0, status: 'SCHEDULED', adultPrice: null, childPrice: null, infantPrice: null,
    meetingPoint: '', notes: ''
  })
  editor.value = 'departure'
}

function lines(value: string) { return String(value || '').split('\n').map(item => item.trim()).filter(Boolean) }
function pipeRows(value: string, faq = false) {
  return lines(value).map(line => {
    const [one = '', two = '', three = ''] = line.split('|').map(item => item.trim())
    return faq ? { question: one, answer: two } : { title: one, subtitle: two, description: three }
  })
}
function toLocalInput(value: string) {
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}
function formatDate(value: string) { return new Date(value).toLocaleString('es-HN', { dateStyle: 'medium', timeStyle: 'short' }) }
function localDateKey(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
async function moveCalendar(months: number) {
  calendarCursor.value = new Date(calendarCursor.value.getFullYear(), calendarCursor.value.getMonth() + months, 1)
  const from = new Date(calendarCursor.value.getFullYear(), calendarCursor.value.getMonth() - 1, 20)
  const to = new Date(calendarCursor.value.getFullYear(), calendarCursor.value.getMonth() + 2, 10)
  const calendar = await api<Row>(`/api/admin/travel/calendar?from=${encodeURIComponent(from.toISOString())}&to=${encodeURIComponent(to.toISOString())}`)
  departures.value = calendar.departures || []
  bookings.value = calendar.bookings || []
}
function bookingsForDeparture(departureId: string) {
  return bookings.value.filter(item => item.departureId === departureId && !['CANCELLED', 'EXPIRED'].includes(item.bookingStatus))
}
function money(value: any, currency = 'USD') { return new Intl.NumberFormat('es-US', { style: 'currency', currency }).format(Number(value || 0)) }
function addTaxRule() {
  if (!Array.isArray(draft.taxes)) draft.taxes = []
  draft.taxes.push({ name: '', rate: 0 })
}
function removeTaxRule(index: number) {
  if (Array.isArray(draft.taxes)) draft.taxes.splice(index, 1)
}
const editorTitle = computed(() => editor.value === 'tour' ? 'Tour o actividad turística' : editor.value === 'departure' ? 'Salida y disponibilidad' : editor.value === 'destination' ? 'Destino' : editor.value === 'activity' ? 'Actividad' : 'Servicio adicional')
function bookingClass(value = '') { return ['CONFIRMED','PAID','COMPLETED'].includes(value) ? 'ok' : ['PENDING_PAYMENT','PENDING','PARTIALLY_PAID'].includes(value) ? 'pending' : value === 'CANCELLED' ? 'danger' : 'muted' }

async function saveEditor() {
  saving.value = true
  error.value = ''
  try {
    let endpoint = ''
    let payload: Row = { ...draft }
    if (editor.value === 'tour') {
      endpoint = '/api/admin/travel/tours'
      payload = {
        ...draft,
        taxes: Array.isArray(draft.taxes) ? draft.taxes.filter((tax: Row) => String(tax.name || '').trim() && Number(tax.rate || 0) >= 0) : [],
        included: lines(draft.includedText), excluded: lines(draft.excludedText),
        itinerary: pipeRows(draft.itineraryText), faqs: pipeRows(draft.faqText, true), gallery: lines(draft.galleryText)
      }
      delete payload.includedText; delete payload.excludedText; delete payload.itineraryText; delete payload.faqText; delete payload.galleryText
      delete payload.destination; delete payload.activities; delete payload.createdAt; delete payload.updatedAt; delete payload.demoData
    } else if (editor.value === 'departure') {
      endpoint = '/api/admin/travel/departures'
      payload.startsAt = new Date(draft.startsAtLocal).toISOString()
      payload.endsAt = draft.endsAtLocal ? new Date(draft.endsAtLocal).toISOString() : null
      delete payload.startsAtLocal; delete payload.endsAtLocal; delete payload.availableSeats; delete payload.reservedSeats; delete payload.heldSeats
      delete payload.tourName; delete payload.tourSlug; delete payload.currency; delete payload.googleCalendarUrl; delete payload.createdAt; delete payload.updatedAt; delete payload.demoData
    } else endpoint = `/api/admin/travel/${editor.value === 'extra' ? 'extras' : `${editor.value}s`}`
    const id = draft.id
    await api(id ? `${endpoint}/${id}` : endpoint, { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) })
    toast.success('Cambios guardados correctamente.')
    editor.value = ''
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar'
  } finally { saving.value = false }
}

async function remove(type: string, item: Row) {
  if (!confirm(`¿Eliminar ${item.name || item.bookingCode || 'este registro'}?`)) return
  await api(`/api/admin/travel/${type}/${item.id}`, { method: 'DELETE' })
  await load()
}

async function cancelBooking(item: Row) {
  if (!confirm(`¿Cancelar la reservación ${item.bookingCode}?`)) return
  await api(`/api/admin/travel/bookings/${item.id}/cancel`, { method: 'POST' })
  await load()
}

async function saveSettings() {
  saving.value = true
  try {
    const saved = await api<Row>('/api/admin/travel/settings', { method: 'PUT', body: JSON.stringify(settings) })
    Object.assign(settings, saved)
    toast.success('Configuración de viajes actualizada.')
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo guardar' }
  finally { saving.value = false }
}

async function demo(action: 'create'|'delete') {
  demoBusy.value = true
  try {
    const response = await api<Row>('/api/admin/travel/demo-data', { method: action === 'create' ? 'POST' : 'DELETE' })
    toast.success(response.message || 'Datos demo actualizados.')
    await load()
  } finally { demoBusy.value = false }
}
</script>


<template>
  <section class="space-y-6 travel-admin">
    <AdminPageHeader eyebrow="OPERACIÓN TURÍSTICA" title="Trast Travel Engine" description="Catálogo, disponibilidad, reservaciones, viajeros y conexión con TrastPay, Mi Cuenta y TrastCRM.">
      <template #actions>
        <a class="button secondary" href="/travel" target="_blank"><UiIcon name="external" :size="16"/>Ver catálogo</a>
        <button class="button" type="button" @click="openTour()"><UiIcon name="plus" :size="16"/>Nuevo tour</button>
      </template>
    </AdminPageHeader>

    <p v-if="error" class="form-error">{{ error }}</p>
    <div class="travel-tabs">
      <button v-for="item in tabs" :key="item.id" :class="['tab-button', { active: tab === item.id }]" type="button" @click="tab = item.id">
        <UiIcon :name="item.icon" :size="17"/>{{ item.label }}
      </button>
    </div>

    <div v-if="loading" class="panel empty">Cargando el motor turístico…</div>

    <template v-else-if="tab === 'overview'">
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <article class="metric-card"><span>Tours publicados</span><strong>{{ dashboard.publishedTours || 0 }}</strong><small>{{ dashboard.tours || 0 }} en total</small></article>
        <article class="metric-card"><span>Próximas salidas</span><strong>{{ dashboard.upcomingDepartures || 0 }}</strong><small>con disponibilidad administrable</small></article>
        <article class="metric-card"><span>Reservaciones confirmadas</span><strong>{{ dashboard.confirmedBookings || 0 }}</strong><small>{{ dashboard.pendingBookings || 0 }} pendientes de pago</small></article>
        <article class="metric-card"><span>Ingresos confirmados</span><strong class="text-2xl">{{ money(dashboard.revenue, dashboard.currency) }}</strong><small>pagos conciliados por TrastPay</small></article>
      </div>
      <div class="grid gap-6 xl:grid-cols-[1.2fr_.8fr]">
        <section class="panel">
          <div class="section-heading"><div><h2>Próximas salidas</h2><p>Abra Google Calendar o gestione cupos desde aquí.</p></div><button class="button secondary small" @click="openDeparture()"><UiIcon name="plus" :size="15"/>Agregar salida</button></div>
          <div v-if="dashboard.nextDepartures?.length" class="space-y-3">
            <article v-for="item in dashboard.nextDepartures" :key="item.id" class="departure-row">
              <div><strong>{{ item.tourName }}</strong><p>{{ formatDate(item.startsAt) }} · {{ item.availableSeats }} cupos disponibles</p></div>
              <a :href="item.googleCalendarUrl" target="_blank" class="button secondary small"><UiIcon name="calendar" :size="15"/>Google Calendar</a>
            </article>
          </div><div v-else class="empty">Todavía no hay salidas programadas.</div>
        </section>
        <section class="panel">
          <h2 class="text-xl font-bold">Catálogo de migración del sitio</h2>
          <p class="mt-2 text-sm leading-6 text-slate-500">Carga o restaura el catálogo de migración correspondiente al tema activo: destinos, actividades, tours, tarifas, extras y salidas. Los registros quedan identificados como datos demo y pueden retirarse sin afectar información creada manualmente.</p>
          <div class="mt-5 grid gap-3"><button class="button" :disabled="demoBusy" @click="demo('create')"><UiIcon name="refresh" :size="16"/>Cargar o restaurar catálogo del sitio</button><button class="button secondary" :disabled="demoBusy" @click="demo('delete')"><UiIcon name="trash" :size="16"/>Retirar solo datos demo</button></div>
        </section>
      </div>
    </template>

    <template v-else-if="tab === 'tours'">
      <section class="panel">
        <div class="section-heading">
          <div><h2>Catálogo de tours</h2><p>Contenido, itinerarios, tarifas, impuestos y publicación.</p></div>
          <div class="toolbar"><label class="search-control"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Buscar por nombre, código o destino"></label><button class="button" @click="openTour()"><UiIcon name="plus" :size="16"/>Nuevo tour</button></div>
        </div>
        <div v-if="filteredTours.length" class="tour-admin-grid">
          <article v-for="item in filteredTours" :key="item.id" class="tour-admin-card">
            <div class="tour-admin-card__image"><img v-if="item.featuredImageUrl" :src="item.featuredImageUrl" :alt="item.name"><div v-else><UiIcon name="image" :size="30"/></div><span :class="['status-pill', item.status === 'PUBLISHED' ? 'ok' : 'muted']">{{ item.status }}</span></div>
            <div class="tour-admin-card__body">
              <span class="eyebrow">{{ item.destination?.name || 'Sin destino' }}</span><h3>{{ item.name }}</h3><p>{{ item.shortDescription }}</p>
              <div class="tour-admin-meta"><strong>{{ money(item.adultPrice, item.currency) }}</strong><span>{{ item.durationValue }} {{ item.durationUnit }}</span><span v-if="item.taxExempt" class="status-pill ok">Exento</span><span v-else-if="item.taxes?.length" class="status-pill muted">{{ item.taxes.length }} impuesto(s)</span></div>
              <div class="card-actions"><button class="button secondary small" @click="openTour(item)"><UiIcon name="edit" :size="15"/>Editar</button><RouterLink class="button secondary small" :to="`/travel/${item.slug}`" target="_blank"><UiIcon name="external" :size="15"/>Ver</RouterLink><button class="icon-button danger" title="Eliminar" @click="remove('tours', item)"><UiIcon name="trash" :size="16"/></button></div>
            </div>
          </article>
        </div><div v-else class="empty">No hay tours que coincidan con la búsqueda.</div>
      </section>
    </template>

    <template v-else-if="tab === 'calendar'">
      <section class="panel overflow-hidden">
        <div class="section-heading">
          <div><h2>Calendario de llegadas</h2><p>Vea por mes quién llega, cuántas personas vienen y agregue cada salida a Google Calendar.</p></div>
          <div class="calendar-toolbar"><button class="icon-button" title="Mes anterior" @click="moveCalendar(-1)">←</button><strong>{{ calendarTitle }}</strong><button class="icon-button" title="Mes siguiente" @click="moveCalendar(1)">→</button><button class="button" @click="openDeparture()"><UiIcon name="plus" :size="16"/>Programar salida</button></div>
        </div>
        <div class="calendar-scroll"><div class="calendar-grid">
          <div v-for="weekday in calendarWeekdays" :key="weekday" class="weekday">{{ weekday }}</div>
          <section v-for="day in calendarDays" :key="day.key" :class="['calendar-day', { outside: !day.currentMonth }]">
            <div class="calendar-day__head"><b :class="{ today: day.key === localDateKey(new Date()) }">{{ day.date.getDate() }}</b><small v-if="day.bookings.length">{{ day.bookings.length }} reserva{{ day.bookings.length === 1 ? '' : 's' }}</small></div>
            <article v-for="departure in day.departures" :key="departure.id" class="calendar-event">
              <button type="button" @click="openDeparture(departure)"><strong>{{ departure.tourName }}</strong><span>{{ new Date(departure.startsAt).toLocaleTimeString('es-HN', { hour: '2-digit', minute: '2-digit' }) }} · {{ departure.availableSeats }} libres</span></button>
              <div v-if="bookingsForDeparture(departure.id).length" class="arrival-list"><div v-for="reservation in bookingsForDeparture(departure.id)" :key="reservation.id"><b>{{ reservation.customerName }}</b><span>{{ reservation.adults + reservation.children + reservation.infants }} viajero(s) · {{ reservation.bookingCode }}</span></div></div>
              <a v-if="departure.googleCalendarUrl" :href="departure.googleCalendarUrl" target="_blank" rel="noopener"><UiIcon name="calendar" :size="13"/>Google Calendar</a>
            </article>
          </section>
        </div></div>
      </section>
    </template>

    <template v-else-if="tab === 'bookings'">
      <section class="panel">
        <div class="section-heading"><div><h2>Reservaciones y viajeros</h2><p>Los estados de reservación y pago se mantienen separados.</p></div><span class="status-pill muted">{{ bookings.length }} registros</span></div>
        <div v-if="bookings.length" class="booking-grid">
          <article v-for="item in bookings" :key="item.id" class="booking-card">
            <div class="booking-card__head"><div><span class="eyebrow">{{ item.bookingCode }}</span><h3>{{ item.tourName }}</h3></div><span :class="['status-pill', bookingClass(item.bookingStatus)]">{{ item.bookingStatus }}</span></div>
            <div class="booking-card__customer"><UiIcon name="account" :size="19"/><div><b>{{ item.customerName }}</b><small>{{ item.customerEmail }}</small></div></div>
            <dl><div><dt>Salida</dt><dd>{{ formatDate(item.startsAt) }}</dd></div><div><dt>Viajeros</dt><dd>{{ item.adults + item.children + item.infants }}</dd></div><div><dt>Total</dt><dd>{{ money(item.grandTotal, item.currency) }}</dd></div><div><dt>Saldo</dt><dd>{{ money(item.balanceDue, item.currency) }}</dd></div></dl>
            <div class="booking-card__footer"><span :class="['status-pill', bookingClass(item.paymentStatus)]">Pago: {{ item.paymentStatus }}</span><div class="card-actions"><a v-if="item.googleCalendarUrl" :href="item.googleCalendarUrl" target="_blank" class="icon-button" title="Google Calendar"><UiIcon name="calendar" :size="16"/></a><button v-if="!['CANCELLED','COMPLETED'].includes(item.bookingStatus)" class="icon-button danger" title="Cancelar" @click="cancelBooking(item)"><UiIcon name="close" :size="16"/></button></div></div>
          </article>
        </div><div v-else class="empty">Todavía no hay reservaciones.</div>
      </section>
    </template>

    <template v-else-if="tab === 'catalog'">
      <div class="grid gap-6 xl:grid-cols-2">
        <section class="panel"><div class="section-heading"><div><h2>Destinos</h2><p>Se usan en filtros y páginas de exploración.</p></div><button class="button small" @click="openSimple('destination')"><UiIcon name="plus" :size="15"/>Nuevo</button></div><div class="catalog-list"><article v-for="item in destinations" :key="item.id"><img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name"><div v-else class="catalog-placeholder"><UiIcon name="map" :size="22"/></div><div class="catalog-copy"><b>{{ item.name }}</b><p>{{ item.summary }}</p></div><button class="icon-button" title="Editar" @click="openSimple('destination', item)"><UiIcon name="edit" :size="16"/></button><button class="icon-button danger" title="Eliminar" @click="remove('destinations', item)"><UiIcon name="trash" :size="16"/></button></article></div></section>
        <section class="panel"><div class="section-heading"><div><h2>Actividades</h2><p>Clasifican y facilitan descubrir experiencias.</p></div><button class="button small" @click="openSimple('activity')"><UiIcon name="plus" :size="15"/>Nueva</button></div><div class="activity-grid"><article v-for="item in activities" :key="item.id"><div class="activity-icon">{{ item.icon || '🌴' }}</div><b>{{ item.name }}</b><p>{{ item.description }}</p><div class="card-actions"><button class="button secondary small" @click="openSimple('activity', item)"><UiIcon name="edit" :size="15"/>Editar</button><button class="icon-button danger" @click="remove('activities', item)"><UiIcon name="trash" :size="16"/></button></div></article></div></section>
      </div>
    </template>

    <template v-else-if="tab === 'extras'">
      <section class="panel"><div class="section-heading"><div><h2>Servicios adicionales</h2><p>Recogida, fotografía, comida, equipo y otros complementos.</p></div><button class="button" @click="openSimple('extra')"><UiIcon name="plus" :size="16"/>Nuevo servicio</button></div><div class="extra-grid"><article v-for="item in extras" :key="item.id"><span class="eyebrow">{{ item.pricingUnit }}</span><h3>{{ item.name }}</h3><p>{{ item.description }}</p><strong>{{ money(item.price, item.currency) }}</strong><div class="card-actions"><button class="button secondary small" @click="openSimple('extra', item)"><UiIcon name="edit" :size="15"/>Editar</button><button class="icon-button danger" @click="remove('extras', item)"><UiIcon name="trash" :size="16"/></button></div></article></div></section>
    </template>

    <template v-else-if="tab === 'settings'">
      <form class="panel travel-settings" @submit.prevent="saveSettings">
        <div><h2>Configuración del motor turístico</h2><p>Reglas comunes para disponibilidad, impuesto predeterminado, calendario y notificaciones.</p></div>
        <div class="form-grid three"><label>Moneda<select v-model="settings.defaultCurrency"><option>USD</option><option>HNL</option><option>EUR</option></select></label><label>Zona horaria<input v-model="settings.timezone" placeholder="America/Tegucigalpa"></label><label>Impuesto predeterminado (%)<input v-model.number="settings.taxRate" type="number" min="0" max="100" step="0.01"><small>Se usa solo cuando el tour no tiene impuestos propios.</small></label><label>Retención de cupos (minutos)<input v-model.number="settings.bookingHoldMinutes" type="number" min="5"></label><label>Anticipación mínima (horas)<input v-model.number="settings.minimumLeadHours" type="number" min="0"></label><label>Correo de operaciones<input v-model="settings.notificationEmail" type="email"></label><label>Calendario de Google<input v-model="settings.googleCalendarId" placeholder="ID opcional"></label><label>URL de términos<input v-model="settings.termsUrl" type="url"></label></div>
        <div class="check-grid"><label class="option-check"><input v-model="settings.guestBooking" type="checkbox"><span><b>Reservación como invitado</b><small>Permite reservar sin crear una cuenta primero.</small></span></label><label class="option-check"><input v-model="settings.googleCalendarEnabled" type="checkbox"><span><b>Enlaces de Google Calendar</b><small>Genera enlaces por salida y reservación.</small></span></label></div>
        <div class="drawer-actions inline"><button class="button" :disabled="saving"><UiIcon name="save" :size="16"/>{{ saving ? 'Guardando…' : 'Guardar configuración' }}</button></div>
      </form>
    </template>

    <Teleport to="body">
      <div v-if="editor" class="travel-editor-backdrop" @click.self="editor = ''">
        <form :class="['travel-editor-drawer', { wide: editor === 'tour' }]" @submit.prevent="saveEditor">
          <header class="drawer-header"><div><span class="eyebrow">EDITOR</span><h2>{{ editorTitle }}</h2><p>Complete la información y guarde los cambios.</p></div><button type="button" class="icon-button" title="Cerrar" @click="editor = ''"><UiIcon name="close" :size="18"/></button></header>
          <div class="drawer-body">
            <template v-if="editor === 'tour'">
              <section class="form-section"><div class="form-section__title"><span>1</span><div><h3>Información principal</h3><p>Datos que verá el viajero en el catálogo.</p></div></div><div class="form-grid two"><label>Nombre<input v-model="draft.name" required></label><label>Código<input v-model="draft.code" placeholder="ROA-001"></label><label>Destino<select v-model="draft.destinationId"><option value="">Sin destino</option><option v-for="item in destinations" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>Estado<select v-model="draft.status"><option>DRAFT</option><option>PUBLISHED</option><option>PAUSED</option><option>ARCHIVED</option></select></label><label class="full">Descripción corta<textarea v-model="draft.shortDescription" rows="2"></textarea></label><label class="full">Descripción completa<textarea v-model="draft.description" rows="5"></textarea></label><label>Imagen destacada<input v-model="draft.featuredImageUrl" type="url"></label><label>Galería (una URL por línea)<textarea v-model="draft.galleryText" rows="3"></textarea></label></div></section>
              <section class="form-section"><div class="form-section__title"><span>2</span><div><h3>Clasificación y operación</h3><p>Duración, capacidad y perfil de la experiencia.</p></div></div><div class="form-grid four"><label>Duración<input v-model.number="draft.durationValue" type="number" min="1"></label><label>Unidad<select v-model="draft.durationUnit"><option value="MINUTE">Minutos</option><option value="HOUR">Horas</option><option value="DAY">Días</option></select></label><label>Dificultad<input v-model="draft.difficulty"></label><label>Máximo de personas<input v-model.number="draft.maximumGroupSize" type="number" min="1"></label><label>Edad mínima<input v-model.number="draft.minimumAge" type="number" min="0"></label><label>Edad máxima<input v-model.number="draft.maximumAge" type="number" min="0"></label><label>Grupo mínimo<input v-model.number="draft.minimumGroupSize" type="number" min="1"></label><label>Confirmación<select v-model="draft.confirmationMode"><option value="INSTANT">Inmediata</option><option value="MANUAL">Manual</option></select></label></div><div class="field-group"><b>Actividades</b><div class="chips"><label v-for="item in activities" :key="item.id" class="option-check compact"><input v-model="draft.activityIds" type="checkbox" :value="item.id"><span>{{ item.icon }} {{ item.name }}</span></label></div></div></section>
              <section class="form-section"><div class="form-section__title"><span>3</span><div><h3>Tarifas e impuestos</h3><p>Precios por viajero y reglas fiscales específicas del tour.</p></div></div><div class="form-grid five"><label>Adulto<input v-model.number="draft.adultPrice" type="number" min="0" step="0.01"></label><label>Niño<input v-model.number="draft.childPrice" type="number" min="0" step="0.01"></label><label>Infante<input v-model.number="draft.infantPrice" type="number" min="0" step="0.01"></label><label>Moneda<select v-model="draft.currency"><option>USD</option><option>HNL</option><option>EUR</option></select></label><label>Depósito %<input v-model.number="draft.depositPercentage" type="number" min="1" max="100"></label></div><div class="form-grid two"><label>Modalidad<select v-model="draft.pricingMode"><option value="PER_PERSON">Por persona</option><option value="PER_GROUP">Por grupo</option></select></label><label class="option-check standalone"><input v-model="draft.taxExempt" type="checkbox"><span><b>Tour exento de impuestos</b><small>No aplicará el impuesto global ni reglas propias.</small></span></label></div><div v-if="!draft.taxExempt" class="tax-editor"><div class="tax-editor__head"><div><b>Impuestos propios del tour</b><small>Puede agregar uno, dos o más. Si queda vacío se aplicará el impuesto predeterminado de {{ settings.taxRate || 0 }}%.</small></div><button type="button" class="button secondary small" @click="addTaxRule"><UiIcon name="plus" :size="14"/>Agregar impuesto</button></div><div v-for="(tax, index) in draft.taxes" :key="index" class="tax-row"><label>Nombre<input v-model="tax.name" placeholder="Impuesto sobre ventas"></label><label>Porcentaje<input v-model.number="tax.rate" type="number" min="0" max="100" step="0.01"></label><button type="button" class="icon-button danger" title="Quitar impuesto" @click="removeTaxRule(index)"><UiIcon name="trash" :size="16"/></button></div></div></section>
              <section class="form-section"><div class="form-section__title"><span>4</span><div><h3>Contenido de la experiencia</h3><p>Incluidos, itinerario y preguntas frecuentes.</p></div></div><div class="form-grid two"><label>Incluye (un elemento por línea)<textarea v-model="draft.includedText" rows="6"></textarea></label><label>No incluye (un elemento por línea)<textarea v-model="draft.excludedText" rows="6"></textarea></label><label>Itinerario (título | subtítulo/hora | descripción)<textarea v-model="draft.itineraryText" rows="7"></textarea></label><label>Preguntas frecuentes (pregunta | respuesta)<textarea v-model="draft.faqText" rows="7"></textarea></label></div><label class="option-check standalone"><input v-model="draft.featured" type="checkbox"><span><b>Tour destacado</b><small>Aparecerá primero en los catálogos y bloques de inicio.</small></span></label></section>
            </template>

            <template v-else-if="editor === 'departure'"><section class="form-section"><div class="form-section__title"><span><UiIcon name="calendar" :size="17"/></span><div><h3>Fecha y capacidad</h3><p>Configure la salida que aparecerá en el calendario público.</p></div></div><div class="form-grid two"><label>Tour<select v-model="draft.tourId" required><option value="" disabled>Seleccione un tour</option><option v-for="item in tours" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>Estado<select v-model="draft.status"><option>SCHEDULED</option><option>CLOSED</option><option>SOLD_OUT</option><option>CANCELLED</option><option>COMPLETED</option></select></label><label>Inicio<input v-model="draft.startsAtLocal" type="datetime-local" required></label><label>Final<input v-model="draft.endsAtLocal" type="datetime-local"></label><label>Capacidad<input v-model.number="draft.capacity" type="number" min="1"></label><label>Cupos bloqueados<input v-model.number="draft.blockedSeats" type="number" min="0"></label><label>Tarifa adulto especial<input v-model.number="draft.adultPrice" type="number" min="0" step=".01" placeholder="Usar tarifa del tour"></label><label>Tarifa niño especial<input v-model.number="draft.childPrice" type="number" min="0" step=".01"></label><label>Punto de encuentro<input v-model="draft.meetingPoint"></label><label class="full">Notas<textarea v-model="draft.notes" rows="4"></textarea></label></div></section></template>

            <template v-else><section class="form-section"><div class="form-section__title"><span><UiIcon :name="editor === 'destination' ? 'map' : editor === 'activity' ? 'grid' : 'plus'" :size="17"/></span><div><h3>{{ editorTitle }}</h3><p>Información reutilizable en el catálogo y proceso de reservación.</p></div></div><div class="form-grid two"><label>Nombre<input v-model="draft.name" required></label><label>Slug<input v-model="draft.slug"></label><template v-if="editor === 'destination'"><label>País<input v-model="draft.country"></label><label>Región o departamento<input v-model="draft.region"></label><label>Ciudad o zona<input v-model="draft.city"></label><label>Imagen<input v-model="draft.imageUrl" type="url"></label><label class="full">Resumen<textarea v-model="draft.summary" rows="2"></textarea></label><label class="full">Descripción<textarea v-model="draft.description" rows="4"></textarea></label><label>Latitud<input v-model.number="draft.latitude" type="number" step="any"></label><label>Longitud<input v-model.number="draft.longitude" type="number" step="any"></label><label class="option-check standalone"><input v-model="draft.featured" type="checkbox"><span>Destino destacado</span></label></template><template v-else-if="editor === 'activity'"><label>Icono o emoji<input v-model="draft.icon" placeholder="🌊"></label><label>Imagen<input v-model="draft.imageUrl" type="url"></label><label class="full">Descripción<textarea v-model="draft.description" rows="4"></textarea></label></template><template v-else><label>Precio<input v-model.number="draft.price" type="number" min="0" step=".01"></label><label>Moneda<select v-model="draft.currency"><option>USD</option><option>HNL</option><option>EUR</option></select></label><label>Cobro<select v-model="draft.pricingUnit"><option value="PER_BOOKING">Por reservación</option><option value="PER_PERSON">Por persona</option><option value="PER_UNIT">Por unidad</option></select></label><label>Cantidad máxima<input v-model.number="draft.maxQuantity" type="number" min="1"></label><label class="full">Descripción<textarea v-model="draft.description" rows="4"></textarea></label></template><label class="option-check standalone"><input v-model="draft.active" type="checkbox"><span>Activo y visible</span></label></div></section></template>
          </div>
          <footer class="drawer-actions"><button type="button" class="button secondary" @click="editor = ''">Cancelar</button><button class="button" :disabled="saving"><UiIcon name="save" :size="16"/>{{ saving ? 'Guardando…' : 'Guardar cambios' }}</button></footer>
        </form>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.travel-admin{--travel-primary:var(--theme-primary,#6d35df);--travel-border:#dbe3ec;--travel-muted:#64748b;--travel-ink:#172033}.travel-tabs{display:flex;flex-wrap:wrap;gap:8px;padding:8px;border:1px solid var(--travel-border);border-radius:16px;background:#fff;box-shadow:0 4px 18px #1720330b}.tab-button{display:inline-flex;align-items:center;gap:7px;border:0;border-radius:11px;background:transparent;padding:10px 13px;color:#526074;font-weight:800}.tab-button.active{background:#f0eaff;color:#6632d1}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;margin-bottom:20px}.section-heading h2,.travel-settings h2{font-size:1.2rem;font-weight:900;margin:0}.section-heading p,.travel-settings>div>p{margin:.25rem 0 0;color:var(--travel-muted);font-size:.875rem}.toolbar,.calendar-toolbar,.card-actions{display:flex;align-items:center;gap:9px;flex-wrap:wrap}.search-control{display:flex!important;align-items:center!important;gap:8px!important;min-width:min(360px,55vw)!important;border:1px solid var(--travel-border)!important;border-radius:12px!important;padding:0 12px!important;background:#fff!important}.search-control input{border:0!important;box-shadow:none!important;padding:10px 0!important}.departure-row{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:15px;border:1px solid var(--travel-border);border-radius:14px}.departure-row p{margin:.3rem 0 0;color:var(--travel-muted);font-size:.84rem}.tour-admin-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.tour-admin-card{overflow:hidden;border:1px solid var(--travel-border);border-radius:18px;background:#fff}.tour-admin-card__image{position:relative;height:170px;background:#eef3f7}.tour-admin-card__image img{width:100%;height:100%;object-fit:cover}.tour-admin-card__image>div{display:grid;height:100%;place-items:center;color:#8091a4}.tour-admin-card__image>.status-pill{position:absolute;right:12px;top:12px}.tour-admin-card__body{display:grid;gap:9px;padding:17px}.tour-admin-card__body h3{font-size:1.05rem;font-weight:900;margin:0}.tour-admin-card__body>p{min-height:42px;color:var(--travel-muted);font-size:.85rem;line-height:1.5}.tour-admin-meta{display:flex;align-items:center;gap:9px;flex-wrap:wrap}.tour-admin-meta>span:not(.status-pill){font-size:.78rem;color:var(--travel-muted)}.calendar-scroll{overflow-x:auto;border:1px solid var(--travel-border);border-radius:16px}.calendar-grid{display:grid;grid-template-columns:repeat(7,minmax(138px,1fr));min-width:980px}.weekday{padding:9px;text-align:center;background:#172033;color:white;font-size:.7rem;font-weight:900;text-transform:uppercase;letter-spacing:.08em;border-right:1px solid #ffffff1a}.calendar-day{min-height:175px;padding:8px;border-right:1px solid var(--travel-border);border-bottom:1px solid var(--travel-border);background:#fff}.calendar-day.outside{background:#f7f9fb;color:#94a3b8}.calendar-day__head{display:flex;align-items:center;justify-content:space-between;margin-bottom:7px}.calendar-day__head b{display:grid;width:27px;height:27px;place-items:center;border-radius:50%;font-size:.8rem}.calendar-day__head b.today{background:#6934da;color:white}.calendar-day__head small{border-radius:999px;background:#dcfce7;padding:4px 7px;color:#166534;font-size:.62rem;font-weight:900}.calendar-event{margin-bottom:7px;border:1px solid #bde9e6;border-radius:11px;background:#ebfbfa;padding:8px;color:#173638}.calendar-event>button{display:block;width:100%;border:0;background:transparent;text-align:left}.calendar-event strong,.calendar-event span{display:block}.calendar-event strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:.72rem}.calendar-event span{font-size:.65rem;color:#60777a}.calendar-event>a{display:inline-flex;align-items:center;gap:4px;margin-top:7px;color:#087f7b;font-size:.62rem;font-weight:900}.arrival-list{display:grid;gap:4px;margin-top:7px;padding-top:7px;border-top:1px solid #bde9e6}.arrival-list>div{padding:5px;border-radius:7px;background:white}.arrival-list b{font-size:.65rem}.booking-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.booking-card{border:1px solid var(--travel-border);border-radius:18px;background:#fff;padding:18px}.booking-card__head,.booking-card__footer{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.booking-card h3{margin:.25rem 0 0;font-size:1rem;font-weight:900}.booking-card__customer{display:flex;align-items:center;gap:10px;margin:17px 0;padding:12px;border-radius:12px;background:#f7f9fb}.booking-card__customer div{display:grid}.booking-card__customer small{color:var(--travel-muted)}.booking-card dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin-bottom:16px}.booking-card dl div{padding:10px;border:1px solid #edf1f5;border-radius:10px}.booking-card dt{font-size:.65rem;color:var(--travel-muted)}.booking-card dd{margin:.25rem 0 0;font-size:.78rem;font-weight:800}.booking-card__footer{align-items:center}.status-pill.pending{background:#fff7d6;color:#8a5b00}.status-pill.danger{background:#fee4e2;color:#b42318}.catalog-list{display:grid;gap:10px}.catalog-list article{display:grid;grid-template-columns:56px minmax(0,1fr) 38px 38px;align-items:center;gap:12px;padding:12px;border:1px solid var(--travel-border);border-radius:13px}.catalog-list img,.catalog-placeholder{width:56px;height:56px;border-radius:11px;object-fit:cover}.catalog-placeholder{display:grid;place-items:center;background:#eaf7f3;color:#087f7b}.catalog-copy{min-width:0}.catalog-copy p{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--travel-muted);font-size:.8rem}.activity-grid,.extra-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.activity-grid article,.extra-grid article{display:grid;gap:8px;padding:16px;border:1px solid var(--travel-border);border-radius:14px}.activity-grid p,.extra-grid p{color:var(--travel-muted);font-size:.82rem;line-height:1.45}.activity-icon{font-size:1.65rem}.extra-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.extra-grid h3{font-size:1rem;font-weight:900}.extra-grid>article>strong{font-size:1.2rem}.travel-settings{display:grid;gap:24px}.form-grid{display:grid;gap:16px}.form-grid.two{grid-template-columns:repeat(2,minmax(0,1fr))}.form-grid.three{grid-template-columns:repeat(3,minmax(0,1fr))}.form-grid.four{grid-template-columns:repeat(4,minmax(0,1fr))}.form-grid.five{grid-template-columns:repeat(5,minmax(0,1fr))}.form-grid .full{grid-column:1/-1}.travel-settings label,.travel-editor-drawer label{display:grid;align-content:start;gap:7px;min-width:0;color:#263548;font-size:.76rem;font-weight:800}.travel-settings :is(input,select,textarea),.travel-editor-drawer :is(input,select,textarea){width:100%;min-width:0;border:1px solid #cfd8e3;border-radius:11px;background:#fff;padding:11px 12px;color:#172033;outline:none}.travel-settings :is(input,select,textarea):focus,.travel-editor-drawer :is(input,select,textarea):focus{border-color:#7040dd;box-shadow:0 0 0 3px #7040dd1a}.travel-settings label small,.travel-editor-drawer label small{color:var(--travel-muted);font-size:.68rem;font-weight:500;line-height:1.45}.check-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.option-check{display:flex!important;align-items:flex-start!important;gap:10px!important;padding:13px!important;border:1px solid var(--travel-border)!important;border-radius:12px!important;background:#fff!important}.option-check input{flex:0 0 auto!important;width:16px!important;height:16px!important;margin-top:2px!important;padding:0!important}.option-check span{display:grid;gap:2px}.option-check.compact{padding:9px 11px!important}.option-check.standalone{margin-top:15px}.drawer-actions.inline{justify-content:flex-end;border:0;padding:0}.travel-editor-backdrop{position:fixed;inset:0;z-index:1200;display:flex;justify-content:flex-end;background:#101828a8;backdrop-filter:blur(2px)}.travel-editor-drawer{display:grid;grid-template-rows:auto minmax(0,1fr) auto;width:min(720px,100%);height:100%;overflow:hidden;background:#f7f9fc;box-shadow:-25px 0 70px #1018284d;animation:drawer-in .22s ease-out}.travel-editor-drawer.wide{width:min(980px,100%)}.drawer-header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:24px 28px;border-bottom:1px solid var(--travel-border);background:#fff}.drawer-header h2{margin:.35rem 0 .2rem;font-size:1.55rem;font-weight:900}.drawer-header p{margin:0;color:var(--travel-muted);font-size:.82rem}.drawer-body{overflow-y:auto;padding:24px 28px}.form-section{margin-bottom:20px;padding:22px;border:1px solid var(--travel-border);border-radius:18px;background:#fff}.form-section__title{display:flex;align-items:flex-start;gap:12px;margin-bottom:18px}.form-section__title>span{display:grid;flex:0 0 32px;width:32px;height:32px;place-items:center;border-radius:10px;background:#efe9ff;color:#6a34d5;font-weight:900}.form-section__title h3{margin:0;font-size:1rem;font-weight:900}.form-section__title p{margin:.2rem 0 0;color:var(--travel-muted);font-size:.75rem}.field-group{display:grid;gap:9px;margin-top:16px}.chips{display:flex;flex-wrap:wrap;gap:8px}.tax-editor{display:grid;gap:10px;margin-top:16px;padding:15px;border:1px dashed #cfc3ed;border-radius:14px;background:#faf8ff}.tax-editor__head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.tax-editor__head>div{display:grid;gap:3px}.tax-editor__head small{color:var(--travel-muted);font-size:.7rem}.tax-row{display:grid;grid-template-columns:minmax(0,1fr) 150px 40px;align-items:end;gap:10px}.drawer-actions{display:flex;justify-content:flex-end;gap:10px;padding:17px 28px;border-top:1px solid var(--travel-border);background:#fff}.icon-button{display:inline-grid;place-items:center;width:38px;height:38px;border:1px solid var(--travel-border);border-radius:10px;background:#fff;color:#526074}.icon-button.danger{color:#b42318}.button{display:inline-flex;align-items:center;justify-content:center;gap:7px}.eyebrow{font-size:.65rem;font-weight:900;letter-spacing:.13em;text-transform:uppercase;color:#6a34d5}@keyframes drawer-in{from{transform:translateX(28px);opacity:.5}to{transform:translateX(0);opacity:1}}@media(max-width:1180px){.tour-admin-grid,.extra-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.form-grid.five{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:850px){.section-heading{flex-direction:column}.toolbar,.calendar-toolbar{width:100%}.search-control{min-width:0!important;flex:1}.tour-admin-grid,.booking-grid,.activity-grid,.extra-grid,.form-grid.three,.form-grid.four,.form-grid.five{grid-template-columns:repeat(2,minmax(0,1fr))}.travel-editor-drawer,.travel-editor-drawer.wide{width:100%}}@media(max-width:600px){.tour-admin-grid,.booking-grid,.activity-grid,.extra-grid,.form-grid.two,.form-grid.three,.form-grid.four,.form-grid.five,.check-grid{grid-template-columns:1fr}.drawer-header,.drawer-body,.drawer-actions{padding-left:16px;padding-right:16px}.form-section{padding:16px}.tax-editor__head{flex-direction:column}.tax-row{grid-template-columns:1fr 1fr 40px}.calendar-toolbar strong{order:-1;width:100%}.catalog-list article{grid-template-columns:48px minmax(0,1fr) 36px 36px}.catalog-list img,.catalog-placeholder{width:48px;height:48px}}
</style>
