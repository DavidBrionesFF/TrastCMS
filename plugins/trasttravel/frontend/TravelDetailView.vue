<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import UiIcon from '@/components/common/UiIcon.vue'

type Row = Record<string, any>

type CalendarCell = {
  key: string
  day: number | null
  departure: Row | null
}

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()
const loading = ref(true)
const booking = ref(false)
const error = ref('')
const availabilityError = ref('')
const tour = ref<Row | null>(null)
const departures = ref<Row[]>([])
const extras = ref<Row[]>([])
const relatedTours = ref<Row[]>([])
const showWizard = ref(false)
const wizardStep = ref(1)
const wizardMain = ref<HTMLElement | null>(null)

const form = reactive<Row>({
  departureId: '',
  customerName: '',
  customerEmail: '',
  customerPhone: '',
  adults: 1,
  children: 0,
  infants: 0,
  extras: {},
  notes: '',
  travellers: []
})

const fieldErrors = reactive<Record<string, string>>({
  customerName: '',
  customerEmail: '',
  customerPhone: ''
})

function validateStep3(): boolean {
  fieldErrors.customerName = ''
  fieldErrors.customerEmail = ''
  fieldErrors.customerPhone = ''
  let valid = true

  const name = String(form.customerName || '').trim()
  if (!name) {
    fieldErrors.customerName = 'Please enter your full name.'
    valid = false
  }

  const email = String(form.customerEmail || '').trim()
  if (!email) {
    fieldErrors.customerEmail = 'Please enter your email address.'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    fieldErrors.customerEmail = 'Please enter a valid email address.'
    valid = false
  }

  return valid
}

const isFeaturedTravelLayout = computed(() => false)
const selectedDeparture = computed(() => departures.value.find(item => item.id === form.departureId) || null)
const travellerCount = computed(() => Number(form.adults) + Number(form.children) + Number(form.infants))
const isGroupPricing = computed(() => tour.value?.pricingMode === 'PER_GROUP')
const baseEstimated = computed(() => {
  if (!tour.value) return 0
  const departure = selectedDeparture.value
  const perGroup = tour.value.pricingMode === 'PER_GROUP'
  const adult = Number(departure?.adultPrice ?? tour.value.adultPrice ?? 0) * (perGroup ? 1 : Number(form.adults))
  const child = perGroup ? 0 : Number(departure?.childPrice ?? tour.value.childPrice ?? 0) * Number(form.children)
  const infant = perGroup ? 0 : Number(departure?.infantPrice ?? tour.value.infantPrice ?? 0) * Number(form.infants)
  const extraTotal = extras.value.reduce((sum, item) => {
    const quantity = Number(form.extras[item.slug] || 0)
    const multiplier = item.pricingUnit === 'PER_PERSON' ? travellerCount.value : 1
    return sum + quantity * Number(item.price || 0) * multiplier
  }, 0)
  return adult + child + infant + extraTotal
})
const effectiveTaxes = computed(() => {
  if (!tour.value || tour.value.taxExempt) return []
  if (Array.isArray(tour.value.taxes) && tour.value.taxes.length) return tour.value.taxes
  const fallback = Number(tour.value.defaultTaxRate || 0)
  return fallback > 0 ? [{ name: 'General tax', rate: fallback }] : []
})
const estimatedTax = computed(() => effectiveTaxes.value.reduce(
  (sum: number, item: Row) => sum + baseEstimated.value * Number(item.rate || 0) / 100,
  0
))
const estimated = computed(() => baseEstimated.value + estimatedTax.value)
const calendarMonth = computed(() => {
  const base = selectedDeparture.value?.startsAt || departures.value[0]?.startsAt || new Date().toISOString()
  const value = new Date(base)
  return new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(value)
})
const calendarCells = computed<CalendarCell[]>(() => {
  const base = new Date(selectedDeparture.value?.startsAt || departures.value[0]?.startsAt || new Date())
  const year = base.getFullYear()
  const month = base.getMonth()
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  const mondayOffset = (first.getDay() + 6) % 7
  const result: CalendarCell[] = []
  for (let index = 0; index < mondayOffset; index += 1) {
    result.push({ key: `blank-${index}`, day: null, departure: null })
  }
  for (let day = 1; day <= last.getDate(); day += 1) {
    const departure = departures.value.find(item => {
      const current = new Date(item.startsAt)
      return current.getFullYear() === year && current.getMonth() === month && current.getDate() === day && Number(item.availableSeats || 0) > 0
    }) || null
    result.push({ key: `${year}-${month}-${day}`, day, departure })
  }
  return result
})

onMounted(async () => {
  if (!auth.loaded) await auth.load().catch(() => undefined)
  await load()
})

async function load() {
  loading.value = true
  error.value = ''
  availabilityError.value = ''
  const slug = String(route.params.slug || '')
  try {
    tour.value = await api<Row>(`/api/public/travel/tours/${encodeURIComponent(slug)}`)
    const [departureResult, extrasResult, relatedResult] = await Promise.allSettled([
      api<Row[]>(`/api/public/travel/tours/${encodeURIComponent(slug)}/departures`),
      api<Row[]>('/api/public/travel/extras'),
      api<Row>(`/api/public/travel/search?destination=${encodeURIComponent(tour.value?.destination?.slug || '')}`)
    ])
    if (departureResult.status === 'fulfilled') {
      departures.value = departureResult.value.filter(item => item && Number(item.availableSeats || 0) > 0)
      if (departures.value[0]) form.departureId = departures.value[0].id
    } else {
      departures.value = []
      availabilityError.value = departureResult.reason instanceof Error
        ? departureResult.reason.message
        : 'Availability could not be loaded.'
    }
    extras.value = extrasResult.status === 'fulfilled' ? extrasResult.value : []
    relatedTours.value = relatedResult.status === 'fulfilled'
      ? (relatedResult.value.tours || []).filter((item: Row) => item.slug !== slug).slice(0, 3)
      : []
    if (auth.user.authenticated) {
      form.customerName = auth.user.displayName || ''
      form.customerEmail = auth.user.email || ''
    }
  } catch (exception) {
    tour.value = null
    error.value = exception instanceof Error ? exception.message : 'The experience could not be loaded.'
  } finally {
    loading.value = false
  }
}

function money(value: any, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(Number(value || 0))
}

function date(value: string) {
  return new Date(value).toLocaleString('en-US', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

function shortDate(value?: string) {
  if (!value) return 'Select a date'
  return new Date(value).toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' })
}

function tourImage(item: Row | null) {
  if (!item) return ''
  return item.featuredImageUrl ? String(item.featuredImageUrl) : ''
}

function recoverTourImage(event: Event, _item?: Row) {
  const image = event.currentTarget as HTMLImageElement
  image.style.display = 'none'
}

function durationLabel() {
  if (!tour.value) return ''
  const unit = tour.value.durationUnit === 'DAY' ? 'Days' : tour.value.durationUnit === 'HOUR' ? 'Hours' : 'Minutes'
  return `${tour.value.durationValue || 1} ${unit}`
}

async function setWizardStep(step: number) {
  wizardStep.value = Math.max(1, Math.min(3, step))
  await nextTick()
  wizardMain.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

async function openWizard() {
  error.value = ''
  showWizard.value = true
  await setWizardStep(form.departureId ? 2 : 1)
}

async function chooseDeparture(item: Row) {
  form.departureId = item.id
  await setWizardStep(2)
}

function changeCount(field: 'adults' | 'children' | 'infants', amount: number) {
  const minimum = field === 'adults' ? 1 : 0
  const capacity = Number(selectedDeparture.value?.availableSeats || tour.value?.maximumGroupSize || 99)
  const current = Number(form[field] || 0)
  const occupiedByOthers = travellerCount.value - current
  const maximumForField = Math.max(minimum, capacity - occupiedByOthers)
  form[field] = Math.min(maximumForField, Math.max(minimum, current + amount))
}

function nextWizardStep() {
  error.value = ''
  if (wizardStep.value === 1 && !form.departureId) {
    error.value = 'Select an available date.'
    return
  }
  if (wizardStep.value === 2 && travellerCount.value < 1) {
    error.value = 'Select at least one traveller.'
    return
  }
  void setWizardStep(Math.min(3, wizardStep.value + 1))
}

async function reserve() {
  error.value = ''
  if (!form.departureId) {
    error.value = 'Select an available date.'
    return
  }
  if (travellerCount.value > Number(selectedDeparture.value?.availableSeats || 0)) {
    error.value = 'The number of travellers exceeds the available seats.'
    await setWizardStep(2)
    showWizard.value = true
    return
  }
  if (!validateStep3()) {
    error.value = 'Please correct the highlighted fields.'
    showWizard.value = true
    await setWizardStep(3)
    return
  }
  booking.value = true
  try {
    const created = await api<Row>('/api/public/travel/bookings', {
      method: 'POST',
      body: JSON.stringify({
        ...form,
        extras: Object.fromEntries(Object.entries(form.extras).filter(([, qty]) => Number(qty) > 0))
      })
    })
    await cart.ensure(created.currency)
    await cart.add('travel', created.id, 1, { bookingCode: created.bookingCode })
    showWizard.value = false
    await router.push('/cart')
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'The booking could not be created.'
  } finally {
    booking.value = false
  }
}
</script>

<template>
  <div v-if="loading" class="loading-state"><UiIcon name="refresh" :size="28" />Loading experience…</div>

  <main v-else-if="tour" :class="['detail', { 'travel-detail': isFeaturedTravelLayout }]">
    <template v-if="isFeaturedTravelLayout">
      <section class="travel-tour-page shell">
        <div class="travel-tour-title">
          <div><RouterLink to="/travel">← Tours</RouterLink><h1>{{ tour.name }}</h1></div>
          <span><b>{{ tour.durationValue || 1 }}</b>{{ tour.durationUnit === 'HOUR' ? 'Hours' : tour.durationUnit === 'DAY' ? 'Days' : 'Minutes' }}</span>
        </div>

        <div class="travel-tour-main">
          <article>
            <div class="travel-tour-image">
              <img v-if="tourImage(tour)" :src="tourImage(tour)" :alt="tour.name" @error="recoverTourImage($event, tour)">
              <div v-else class="travel-tour-image__empty"><UiIcon name="image" :size="56" /></div>
              <span v-if="tour.gallery?.length">▧ Gallery</span>
            </div>
            <section class="travel-tour-overview">
              <nav><b>Overview</b><span>Cost</span><span>Map</span></nav>
              <div v-if="tour.description" class="prose" v-html="tour.description"></div>
              <p v-else>{{ tour.shortDescription }}</p>
            </section>
          </article>

          <aside class="travel-tour-rate-card">
            <span>From</span><strong>{{ money(tour.adultPrice, tour.currency) }}</strong><small>/ Adults/Kids</small>
            <template v-if="Number(tour.childPrice || 0) > 0"><span>From</span><strong>{{ money(tour.childPrice, tour.currency) }}</strong><small>/ Children</small></template>
            <template v-if="Number(tour.infantPrice || 0) === 0"><strong class="free">Free</strong><small>/ Children under 5 years old</small></template>
            <button type="button" @click="openWizard">CHECK AVAILABILITY</button>
            <p>Need help with booking? <RouterLink to="/page/contact-us">Send Us A Message</RouterLink></p>
          </aside>
        </div>

        <div class="travel-tour-information">
          <section><h2>What Is Included</h2><ul><li v-for="item in tour.included || []" :key="item">✓ {{ item }}</li><li v-if="!tour.included?.length">Details will be confirmed by the operator.</li></ul></section>
          <section><h2>What Is Not Included</h2><ul><li v-for="item in tour.excluded || []" :key="item">– {{ item }}</li><li v-if="!tour.excluded?.length">No exclusions have been registered.</li></ul></section>
          <section class="travel-tour-itinerary"><h2>Itinerary</h2><div v-if="tour.itinerary?.length"><article v-for="(item, index) in tour.itinerary" :key="index"><b>{{ item.title || `Stop ${index + 1}` }}</b><p>{{ item.description }}</p></article></div><p v-else>The itinerary will be confirmed before departure.</p></section>
        </div>

        <section v-if="relatedTours.length" class="travel-related-tours">
          <h2>Related Trips You Might Be Interested In</h2>
          <div><RouterLink v-for="item in relatedTours" :key="item.id" :to="`/travel/${item.slug}`"><img v-if="tourImage(item)" :src="tourImage(item)" :alt="item.name" @error="recoverTourImage($event, item)"><span>{{ item.destination?.name || 'Roatán' }}</span><h3>{{ item.name }}</h3><strong>{{ money(item.adultPrice, item.currency) }}</strong></RouterLink></div>
        </section>
      </section>
    </template>

    <template v-else>
      <section class="detail-hero" :style="tour.featuredImageUrl ? { backgroundImage: `linear-gradient(90deg,rgba(3,32,35,.92),rgba(3,32,35,.18)),url(${tour.featuredImageUrl})` } : {}">
        <div class="shell">
          <RouterLink to="/travel" class="back"><UiIcon name="map" :size="16" />All tours</RouterLink>
          <span class="kicker">{{ tour.destination?.name || 'EXPERIENCE' }}</span>
          <h1>{{ tour.name }}</h1><p>{{ tour.shortDescription }}</p>
          <div class="facts"><span><UiIcon name="calendar" :size="17" />{{ durationLabel() }}</span><span><UiIcon name="users" :size="17" />{{ tour.minimumGroupSize }}–{{ tour.maximumGroupSize }} travellers</span><span><UiIcon name="map" :size="17" />{{ tour.difficulty || 'Suitable for everyone' }}</span><span v-for="item in tour.activities || []" :key="item.id || item.slug">{{ item.icon || '•' }} {{ item.name }}</span></div>
        </div>
      </section>
      <div class="shell detail-layout">
        <article class="detail-content">
          <section><span class="section-kicker">THE EXPERIENCE</span><h2>About this experience</h2><div v-if="tour.description" class="prose" v-html="tour.description"></div><p v-else class="muted-copy">{{ tour.shortDescription }}</p></section>
          <section class="split"><div><h2>What is included</h2><ul class="included"><li v-for="item in tour.included || []" :key="item"><UiIcon name="check" :size="17" />{{ item }}</li></ul><p v-if="!tour.included?.length" class="muted-copy">The operator has not listed the inclusions yet.</p></div><div><h2>What is not included</h2><ul class="excluded"><li v-for="item in tour.excluded || []" :key="item"><UiIcon name="close" :size="17" />{{ item }}</li></ul><p v-if="!tour.excluded?.length" class="muted-copy">No exclusions have been listed.</p></div></section>
          <section><h2>Itinerary</h2><div v-if="tour.itinerary?.length" class="timeline"><article v-for="(item,index) in tour.itinerary" :key="index"><span>{{ index + 1 }}</span><div><h3>{{ item.title || `Stage ${index + 1}` }}</h3><p>{{ item.description }}</p></div></article></div><p v-else class="muted-copy">The itinerary will be confirmed by the operator.</p></section>
          <section v-if="tour.gallery?.length"><h2>Gallery</h2><div class="gallery"><img v-for="url in tour.gallery" :key="url" :src="url" :alt="tour.name"></div></section>
          <section v-if="tour.faqs?.length"><h2>Frequently asked questions</h2><details v-for="(item, index) in tour.faqs" :key="index"><summary>{{ item.question }}</summary><p>{{ item.answer }}</p></details></section>
        </article>
        <aside><form class="booking-card" @submit.prevent="reserve"><span class="kicker">BOOK NOW</span><div class="price"><small>From</small><strong>{{ money(tour.adultPrice, tour.currency) }}</strong><span>{{ tour.pricingMode === 'PER_GROUP' ? 'per group' : 'per adult' }}</span></div><label>Date and time<select v-model="form.departureId" required :disabled="!departures.length"><option value="">{{ departures.length ? 'Select availability' : 'No departures available' }}</option><option v-for="item in departures" :key="item.id" :value="item.id">{{ date(item.startsAt) }} · {{ item.availableSeats }} seats</option></select></label><p v-if="selectedDeparture" class="availability"><UiIcon name="check" :size="16" />{{ selectedDeparture.availableSeats }} seats available · {{ selectedDeparture.meetingPoint || 'Meeting point to be confirmed' }}</p><p v-else-if="availabilityError" class="availability warning"><UiIcon name="info" :size="16" />{{ availabilityError }}</p><p v-else-if="!departures.length" class="availability warning"><UiIcon name="calendar" :size="16" />There are no open dates yet. Contact the operator to request a private departure.</p><div class="people"><label>Adults<input v-model.number="form.adults" type="number" min="0" :max="selectedDeparture?.availableSeats || 99"></label><label>Children<input v-model.number="form.children" type="number" min="0"></label><label>Infants<input v-model.number="form.infants" type="number" min="0"></label></div><label>Full name<input v-model="form.customerName" required></label><label>Email<input v-model="form.customerEmail" type="email" required></label><label>Phone<input v-model="form.customerPhone"></label><div v-if="extras.length" class="extras"><b>Additional services</b><label v-for="item in extras" :key="item.id" class="extra"><span><strong>{{ item.name }}</strong><small>{{ money(item.price, item.currency) }} · {{ item.pricingUnit }}</small></span><input v-model.number="form.extras[item.slug]" type="number" min="0" :max="item.maxQuantity" placeholder="0"></label></div><div class="price-breakdown"><div><span>Estimated subtotal</span><b>{{ money(baseEstimated,tour.currency) }}</b></div><div v-for="tax in effectiveTaxes" :key="tax.name"><span>{{ tax.name }} ({{ tax.rate }}%)</span><b>{{ money(baseEstimated * Number(tax.rate || 0) / 100, tour.currency) }}</b></div><div class="total"><span>Estimated total</span><strong>{{ money(estimated,tour.currency) }}</strong></div></div><p v-if="tour.taxExempt" class="deposit">This tour is configured as tax exempt.</p><p v-if="tour.depositPercentage > 0" class="deposit">A {{ tour.depositPercentage }}% deposit will be requested at checkout.</p><p v-if="error" class="error">{{ error }}</p><button :disabled="booking || travellerCount < 1 || !form.departureId"><UiIcon name="payment" :size="18" />{{ booking ? 'Creating booking…' : 'Continue to checkout' }}</button><small class="secure"><UiIcon name="lock" :size="15" />Seats are held temporarily while you complete payment.</small></form></aside>
      </div>
    </template>
  </main>

  <main v-else class="detail-error"><UiIcon name="info" :size="44" /><h1>This experience could not be opened</h1><p>{{ error }}</p><RouterLink to="/travel">Back to catalogue</RouterLink></main>

  <Teleport to="body">
    <div v-if="showWizard && tour" class="travel-booking-overlay" @click.self="showWizard = false">
      <section class="travel-booking-modal">
        <button class="travel-booking-close" type="button" aria-label="Close modal" @click="showWizard = false">×</button>
        <div class="travel-booking-main">
          <nav class="travel-booking-steps">
            <button :class="{ active: wizardStep === 1, complete: wizardStep > 1 }" type="button" @click="setWizardStep(1)">▣ Date &amp; Time</button>
            <button :class="{ active: wizardStep === 2, complete: wizardStep > 2 }" type="button" :disabled="!form.departureId" @click="setWizardStep(2)">◇ Package Type</button>
            <button :class="{ active: wizardStep === 3 }" type="button" :disabled="wizardStep < 3" @click="setWizardStep(3)">◉ Your Details</button>
          </nav>

          <div class="travel-wizard-content-scroll" ref="wizardMain">
            <div v-if="wizardStep === 1" class="travel-calendar-step">
              <header><h2>{{ calendarMonth }}</h2></header>
              <div class="travel-weekdays"><span>MON</span><span>TUE</span><span>WED</span><span>THU</span><span>FRI</span><span>SAT</span><span>SUN</span></div>
              <div class="travel-calendar-grid">
                <button v-for="cell in calendarCells" :key="cell.key" type="button" :disabled="!cell.departure" :class="{ selected: cell.departure?.id === form.departureId }" @click="cell.departure && chooseDeparture(cell.departure)">
                  {{ cell.day || '' }}
                  <small v-if="cell.departure">{{ cell.departure.availableSeats }} spots</small>
                </button>
              </div>
            </div>

            <div v-else-if="wizardStep === 2" class="travel-package-step">
              <div class="travel-package-head"><span>TRAVELLERS</span><span>QUANTITY</span></div>

              <article class="travel-package-card">
                <div>
                  <h3>{{ isGroupPricing ? 'Private group' : 'Adults/Kids' }}</h3>
                  <strong>{{ money(selectedDeparture?.adultPrice ?? tour.adultPrice, tour.currency) }} <small>{{ isGroupPricing ? '/ Group' : '/ Person' }}</small></strong>
                  <p v-if="isGroupPricing" class="travel-group-note">One group price covers up to {{ tour.maximumGroupSize }} travellers.</p>
                </div>
                <div class="travel-quantity">
                  <button type="button" aria-label="Decrease adults" @click="changeCount('adults',-1)">−</button>
                  <b>{{ form.adults }}</b>
                  <button type="button" aria-label="Increase adults" @click="changeCount('adults',1)">＋</button>
                </div>
              </article>

              <article v-if="!isGroupPricing && Number(selectedDeparture?.childPrice ?? tour.childPrice) > 0" class="travel-package-card">
                <div>
                  <h3>Children</h3>
                  <strong>{{ money(selectedDeparture?.childPrice ?? tour.childPrice, tour.currency) }} <small>/ Person</small></strong>
                </div>
                <div class="travel-quantity">
                  <button type="button" aria-label="Decrease children" @click="changeCount('children',-1)">−</button>
                  <b>{{ form.children }}</b>
                  <button type="button" aria-label="Increase children" @click="changeCount('children',1)">＋</button>
                </div>
              </article>

              <article class="travel-package-card">
                <div>
                  <h3>Children under 5 years old</h3>
                  <strong>{{ Number(tour.infantPrice || 0) ? money(tour.infantPrice,tour.currency) : 'Free' }}</strong>
                </div>
                <div class="travel-quantity">
                  <button type="button" aria-label="Decrease infants" @click="changeCount('infants',-1)">−</button>
                  <b>{{ form.infants }}</b>
                  <button type="button" aria-label="Increase infants" @click="changeCount('infants',1)">＋</button>
                </div>
              </article>

              <div v-if="extras.length" class="travel-wizard-extras">
                <h3>Additional services</h3>
                <div class="travel-extras-grid">
                  <div v-for="item in extras" :key="item.id" class="travel-extra-card">
                    <div class="extra-info">
                      <strong>{{ item.name }}</strong>
                      <small>{{ money(item.price,item.currency) }} · {{ item.pricingUnit }}</small>
                    </div>
                    <div class="extra-input">
                      <input v-model.number="form.extras[item.slug]" type="number" min="0" :max="item.maxQuantity" placeholder="0">
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <form v-else id="travel-details-form" class="travel-details-step" novalidate @submit.prevent="reserve">
              <h2>Traveller &amp; Billing Details</h2>
              <p>TrastTravel requires these details before creating the booking and sending it to the secure checkout.</p>
              <div class="details-fields">
                <label :class="{ 'has-error': fieldErrors.customerName }">
                  <span>Full Name <small class="req-star">*</small></span>
                  <input
                    v-model="form.customerName"
                    type="text"
                    placeholder="John Doe"
                    :class="{ 'input-error': fieldErrors.customerName }"
                    @input="fieldErrors.customerName = ''"
                  >
                  <small v-if="fieldErrors.customerName" class="field-error-msg">⚠️ {{ fieldErrors.customerName }}</small>
                </label>

                <label :class="{ 'has-error': fieldErrors.customerEmail }">
                  <span>Email <small class="req-star">*</small></span>
                  <input
                    v-model="form.customerEmail"
                    type="email"
                    placeholder="john@example.com"
                    :class="{ 'input-error': fieldErrors.customerEmail }"
                    @input="fieldErrors.customerEmail = ''"
                  >
                  <small v-if="fieldErrors.customerEmail" class="field-error-msg">⚠️ {{ fieldErrors.customerEmail }}</small>
                </label>

                <label :class="{ 'has-error': fieldErrors.customerPhone }">
                  <span>Phone Number</span>
                  <input
                    v-model="form.customerPhone"
                    type="tel"
                    placeholder="+1 (555) 000-0000"
                    :class="{ 'input-error': fieldErrors.customerPhone }"
                    @input="fieldErrors.customerPhone = ''"
                  >
                  <small v-if="fieldErrors.customerPhone" class="field-error-msg">⚠️ {{ fieldErrors.customerPhone }}</small>
                </label>

                <label class="full-width">
                  <span>Additional Notes</span>
                  <textarea v-model="form.notes" rows="3" placeholder="Special requests, flight details, or cruise ship info..."></textarea>
                </label>
              </div>
              <p v-if="error" class="travel-wizard-error">⚠️ {{ error }}</p>
            </form>
          </div>

          <!-- Pinned Wizard Footer -->
          <footer class="travel-wizard-footer">
            <button v-if="wizardStep > 1" type="button" class="travel-btn-back" @click="setWizardStep(wizardStep - 1)">‹ Back</button>
            <span v-else></span>
            <button v-if="wizardStep < 3" class="travel-booking-next" type="button" @click="nextWizardStep">Continue</button>
            <button v-else class="travel-booking-next" form="travel-details-form" type="submit" :disabled="booking">{{ booking ? 'Preparing checkout…' : 'Proceed To Checkout' }}</button>
          </footer>
        </div>

        <aside class="travel-booking-summary">
          <span class="summary-kicker">BOOKING SUMMARY</span>
          <h2>{{ tour.name }}</h2>
          <p class="starting-date">Starting Date: <b>{{ shortDate(selectedDeparture?.startsAt) }}</b></p>
          <strong class="travel-package-label">Package: Standard</strong>

          <div v-if="travellerCount" class="summary-travellers">
            <h3>Travellers</h3>
            <p v-if="isGroupPricing">Private group: {{ travellerCount }} traveller{{ travellerCount === 1 ? '' : 's' }} · {{ money(selectedDeparture?.adultPrice ?? tour.adultPrice,tour.currency) }}</p>
            <template v-else>
              <p v-if="form.adults">Adults/Kids: {{ form.adults }} × {{ money(selectedDeparture?.adultPrice ?? tour.adultPrice,tour.currency) }}</p>
              <p v-if="form.children">Children: {{ form.children }} × {{ money(selectedDeparture?.childPrice ?? tour.childPrice,tour.currency) }}</p>
              <p v-if="form.infants">Infants: {{ form.infants }} × {{ money(selectedDeparture?.infantPrice ?? tour.infantPrice,tour.currency) }}</p>
            </template>
          </div>

          <footer class="summary-total">
            <span>Total:</span>
            <strong>{{ money(estimated,tour.currency) }}</strong>
          </footer>
        </aside>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.detail{--travel-primary:var(--theme-primary,#087f7b);--travel-primary-hover:var(--theme-primary-hover,#066d69);--travel-accent:var(--theme-accent,#f2ad44);--travel-ink:var(--theme-text,#092c2f);--travel-muted:var(--theme-text-muted,#617572);--travel-border:var(--theme-border,#d9e6e3);--travel-surface:var(--theme-surface,#fff);--travel-soft:var(--theme-surface-muted,#f4f8f6);background:var(--travel-surface);color:var(--travel-ink)}.shell{width:min(1180px,calc(100% - 32px));margin:auto}.detail-hero{position:relative;min-height:520px;display:flex;align-items:center;background:linear-gradient(120deg,#075753,#0a8c84);background-size:cover;background-position:center;color:white}.detail-hero .shell{padding:65px 0}.back{display:inline-flex;align-items:center;gap:7px;color:#dff7f4;font-weight:800;margin-bottom:65px}.kicker,.section-kicker{font-size:.7rem;font-weight:900;letter-spacing:.17em;color:#43dfd2}.detail-hero h1{max-width:850px;font-size:clamp(2.8rem,7vw,5.5rem);line-height:.95;margin:.5rem 0}.detail-hero p{max-width:680px;color:#d7edeb;font-size:1.08rem}.facts{display:flex;flex-wrap:wrap;gap:12px;margin-top:25px}.facts span{display:flex;align-items:center;gap:7px;padding:9px 12px;border:1px solid #ffffff3b;border-radius:999px}.detail-layout{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:55px;padding:65px 0 90px}.detail-content{display:grid;gap:50px}.detail-content h2{font-size:2rem}.prose,.muted-copy{color:var(--travel-muted);line-height:1.8}.split{display:grid;grid-template-columns:1fr 1fr;gap:45px}.included,.excluded{list-style:none;padding:0;display:grid;gap:10px}.included li,.excluded li{display:flex;align-items:center;gap:8px}.included li{color:#19745c}.excluded li{color:#8c5552}.timeline{display:grid;gap:18px}.timeline article{display:grid;grid-template-columns:38px 1fr;gap:14px}.timeline article>span{display:grid;place-items:center;width:35px;height:35px;border-radius:50%;background:var(--travel-primary);color:white;font-weight:900}.timeline h3{margin:0}.timeline p{margin:.4rem 0;color:var(--travel-muted)}.gallery{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.gallery img{width:100%;height:180px;object-fit:cover;border-radius:13px}.detail-content details{border-bottom:1px solid var(--travel-border);padding:18px 0}.detail-content summary{font-weight:800;cursor:pointer}.booking-card{position:sticky;top:110px;padding:26px;border:1px solid var(--travel-border);border-radius:25px;background:var(--travel-surface);box-shadow:0 22px 60px #0a51511b}.price{display:flex;align-items:baseline;gap:7px;margin:10px 0 22px}.price strong{font-size:2rem}.price small,.price span{font-size:.74rem;color:var(--travel-muted)}.booking-card label{display:grid;gap:6px;margin:12px 0;font-size:.78rem;font-weight:800}.booking-card :is(input,select){width:100%;border:1px solid var(--travel-border);border-radius:11px;padding:11px;background:var(--travel-surface);color:var(--travel-ink)}.people{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}.availability,.deposit,.secure{display:flex;align-items:flex-start;gap:7px;padding:10px;border-radius:10px;background:color-mix(in srgb,var(--travel-primary) 10%,white);color:var(--travel-primary);font-size:.72rem}.availability.warning{background:#fff7e8;color:#8a5a00}.extras{margin-top:18px;padding-top:17px;border-top:1px solid var(--travel-border)}.extra{grid-template-columns:1fr 65px!important;align-items:center}.extra span{display:grid}.extra small{font-weight:500;color:var(--travel-muted)}.price-breakdown{display:grid;gap:8px;margin-top:18px;padding-top:18px;border-top:1px solid var(--travel-border)}.price-breakdown>div{display:flex;justify-content:space-between;color:var(--travel-muted)}.price-breakdown .total{color:var(--travel-ink);font-size:1.2rem}.booking-card button{width:100%;border:0;border-radius:13px;background:var(--travel-primary);color:white;padding:15px;font-weight:900}.error{color:#b42318}.loading-state,.detail-error{display:grid;place-items:center;align-content:center;gap:14px;min-height:65vh;padding:40px;text-align:center}
.travel-tour-page{padding:70px 0 100px}.travel-tour-title{display:flex;align-items:end;justify-content:space-between;gap:25px;margin-bottom:28px}.travel-tour-title a{color:var(--travel-primary);font-weight:750}.travel-tour-title h1{margin:14px 0 0;font-size:clamp(2.4rem,5vw,4.6rem);line-height:1;letter-spacing:-.04em}.travel-tour-title>span{display:grid;place-items:center;width:64px;height:64px;border:1px solid var(--travel-primary);color:var(--travel-primary);font-size:.67rem}.travel-tour-title>span b{font-size:1.7rem}.travel-tour-main{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:28px;align-items:start}.travel-tour-image{position:relative;min-height:500px;background:#eef0f3}.travel-tour-image img{width:100%;height:500px;object-fit:cover}.travel-tour-image__empty{display:grid;place-items:center;height:500px}.travel-tour-image>span{position:absolute;right:18px;bottom:18px;padding:8px 13px;border-radius:999px;background:#fff;font-size:.75rem}.travel-tour-overview{margin-top:26px;border:1px solid #dfe1e8}.travel-tour-overview nav{display:flex;gap:35px;padding:24px;border-bottom:1px solid #eceef2;color:#777}.travel-tour-overview nav b{color:var(--travel-primary);border-bottom:2px solid var(--travel-primary)}.travel-tour-overview>div,.travel-tour-overview>p{padding:28px;line-height:1.8;color:var(--travel-muted)}.travel-tour-rate-card{padding:28px;border:1px solid #e1e3e8;background:white;box-shadow:0 14px 35px #241c5e12}.travel-tour-rate-card>span{display:block;color:#777;font-size:.72rem;margin-top:8px}.travel-tour-rate-card>strong{font-size:1.55rem}.travel-tour-rate-card>small{color:#777}.travel-tour-rate-card .free{color:#19a966}.travel-tour-rate-card button{width:100%;margin:28px 0 14px;border:0;border-radius:999px;background:var(--travel-primary);color:white;padding:15px;font-weight:900}.travel-tour-rate-card p{font-size:.72rem;color:#777}.travel-tour-rate-card a{color:var(--travel-primary)}.travel-tour-information{display:grid;grid-template-columns:1fr 1fr;gap:30px;margin-top:48px}.travel-tour-information>section{padding:28px;border:1px solid #e1e3e8}.travel-tour-information h2{margin-top:0}.travel-tour-information ul{padding-left:0;list-style:none;display:grid;gap:10px;color:var(--travel-muted)}.travel-tour-itinerary{grid-column:1/-1}.travel-tour-itinerary article{padding:14px 0;border-bottom:1px solid #eceef2}.travel-related-tours{margin-top:70px}.travel-related-tours>div{display:grid;grid-template-columns:repeat(3,1fr);gap:24px}.travel-related-tours a{overflow:hidden;border:1px solid #e4e5eb;background:white;box-shadow:0 12px 30px #271d6810}.travel-related-tours img{width:100%;height:220px;object-fit:cover}.travel-related-tours a :is(span,h3,strong){display:block;margin:12px 18px}.travel-related-tours a span{color:var(--travel-muted);font-size:.72rem}.travel-related-tours a strong{color:var(--travel-primary)}

/* Booking Overlay & Modal */
.travel-booking-overlay { position: fixed; inset: 0; z-index: 1200; display: grid; place-items: center; padding: 24px; background: rgba(15, 23, 42, 0.75); backdrop-filter: blur(8px); }
.travel-booking-modal { position: relative; width: min(1350px, 95vw); height: min(780px, 90vh); display: grid; grid-template-columns: minmax(0, 1fr) 360px; overflow: hidden; border-radius: 20px; background: #ffffff; box-shadow: 0 30px 90px rgba(0, 0, 0, 0.35); }
.travel-booking-close { position: absolute; z-index: 10; right: 20px; top: 16px; width: 36px; height: 36px; display: grid; place-items: center; border: 0; border-radius: 50%; background: #f1f5f9; color: #475467; font-size: 1.5rem; cursor: pointer; transition: all 0.18s; }
.travel-booking-close:hover { background: #e2e8f0; color: #0f172a; }
.travel-booking-main { display: flex; flex-direction: column; height: 100%; min-width: 0; overflow: hidden; }
.travel-booking-steps { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; padding: 28px 36px 0; background: #ffffff; }
.travel-booking-steps button { border: 0; border-bottom: 3px solid #e2e8f0; background: transparent; padding: 14px 10px; color: #64748b; font-size: 0.95rem; font-weight: 700; transition: all 0.2s; }
.travel-booking-steps button.active, .travel-booking-steps button.complete { border-color: var(--travel-primary, #5142d8); color: var(--travel-primary, #5142d8); }
.travel-wizard-content-scroll { flex: 1; overflow-y: auto; padding: 24px 36px 32px; }

/* Calendar Step */
.travel-calendar-step header { text-align: center; margin-bottom: 18px; }
.travel-weekdays, .travel-calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
.travel-weekdays span { padding: 12px; text-align: center; color: #64748b; font-weight: 700; font-size: 0.82rem; }
.travel-calendar-grid button { min-height: 72px; border: 1px solid #e2e8f0; background: #ffffff; color: #0f172a; font-weight: 700; border-radius: 8px; margin: 2px; }
.travel-calendar-grid button:disabled { color: #cbd5e1; background: #f8fafc; cursor: not-allowed; }
.travel-calendar-grid button:not(:disabled):hover, .travel-calendar-grid button.selected { background: var(--travel-primary, #5142d8); color: #ffffff; }
.travel-calendar-grid small { display: block; font-size: 0.7rem; margin-top: 4px; opacity: 0.9; }

/* Package Step & Quantity Controls */
.travel-package-head { display: flex; justify-content: space-between; color: #64748b; font-size: 0.78rem; font-weight: 800; letter-spacing: 0.05em; margin-bottom: 8px; }
.travel-package-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 14px; padding: 20px 24px; border: 1px solid #e2e8f0; border-radius: 14px; background: #ffffff; box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04); }
.travel-package-card h3 { margin: 0 0 4px; font-size: 1.1rem; font-weight: 800; color: #0f172a; }
.travel-package-card strong { color: var(--travel-primary, #5142d8); font-size: 1.15rem; }
.travel-package-card strong small { color: #64748b; font-size: 0.8rem; font-weight: 500; }
.travel-quantity { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px; border: 1px solid #cbd5e1; border-radius: 999px; background: #f8fafc; }
.travel-quantity button { display: grid; place-items: center; width: 38px; height: 38px; border: 0; border-radius: 50%; background: #ffffff; color: #0f172a; font-size: 1.2rem; font-weight: 800; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); cursor: pointer; transition: all 0.18s ease; }
.travel-quantity button:hover { background: var(--travel-primary, #5142d8); color: #ffffff; }
.travel-quantity b { min-width: 36px; text-align: center; font-size: 1.15rem; font-weight: 850; color: #0f172a; }

/* Extras Cards */
.travel-wizard-extras { margin-top: 28px; }
.travel-wizard-extras h3 { margin: 0 0 14px; font-size: 1.1rem; font-weight: 800; color: #0f172a; }
.travel-extras-grid { display: grid; gap: 10px; }
.travel-extra-card { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; border: 1px solid #e2e8f0; border-radius: 12px; background: #f8fafc; }
.extra-info strong { display: block; color: #0f172a; font-size: 0.95rem; font-weight: 750; }
.extra-info small { color: #64748b; font-size: 0.8rem; }
.extra-input input { width: 80px; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 8px; background: #ffffff; color: #0f172a; font-weight: 750; text-align: center; }

/* Details Form */
.travel-details-step h2 { margin: 0 0 6px; font-size: 1.35rem; color: #0f172a; }
.travel-details-step p { margin: 0 0 20px; color: #64748b; font-size: 0.9rem; }
.details-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.details-fields label { display: grid; gap: 6px; font-size: 0.8rem; font-weight: 750; color: #334155; }
.details-fields label span { display: flex; align-items: center; gap: 4px; color: #334155; font-weight: 750; }
.req-star { color: #dc2626; font-weight: 800; font-size: 0.9rem; }
.details-fields label.full-width { grid-column: 1 / -1; }
.details-fields :is(input, textarea) { width: 100%; border: 1px solid #cbd5e1; border-radius: 10px; padding: 12px; background: #ffffff; color: #0f172a; font-size: 0.9rem; outline: none; transition: border-color 0.18s, box-shadow 0.18s, background-color 0.18s; }
.details-fields :is(input, textarea):focus { border-color: var(--travel-primary, #5142d8); box-shadow: 0 0 0 3px rgba(81, 66, 216, 0.12); }
.details-fields :is(input, textarea).input-error { border-color: #ef4444 !important; background-color: #fef2f2 !important; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important; }
.field-error-msg { display: block; margin-top: 4px; color: #dc2626 !important; font-size: 0.78rem !important; font-weight: 700 !important; }
.travel-wizard-error { color: #dc2626; font-weight: 700; margin-top: 14px; padding: 10px 14px; border-radius: 8px; background: #fef2f2; border: 1px solid #fecaca; }

/* Pinned Footer Bar */
.travel-wizard-footer { display: flex; align-items: center; justify-content: space-between; padding: 18px 36px; border-top: 1px solid #e2e8f0; background: #ffffff; box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.05); }
.travel-btn-back { border: 0; background: transparent; color: var(--travel-primary, #5142d8); font-size: 1rem; font-weight: 800; cursor: pointer; }
.travel-wizard-footer .travel-booking-next { min-width: 200px; margin: 0; padding: 14px 28px; border: 0; border-radius: 999px; background: var(--travel-primary, #5142d8); color: #ffffff; font-size: 1rem; font-weight: 850; cursor: pointer; transition: all 0.2s ease; }
.travel-wizard-footer .travel-booking-next:hover { background: var(--travel-primary-hover, #4030c2); transform: translateY(-1px); }

/* Booking Summary Side Panel */
.travel-booking-summary { overflow-y: auto; padding: 36px 28px; background: #f0f7ff; border-left: 1px solid #e2e8f0; }
.summary-kicker { display: block; margin-bottom: 8px; color: #64748b; font-size: 0.72rem; font-weight: 850; letter-spacing: 0.08em; }
.travel-booking-summary h2 { margin: 0 0 12px; color: #0f172a; font-size: 1.45rem; font-weight: 850; }
.starting-date { color: #475467; font-size: 0.88rem; }
.starting-date b { color: #0f172a; }
.travel-package-label { display: inline-block; margin: 16px 0 20px; padding: 8px 14px; border-radius: 8px; background: var(--travel-primary, #5142d8); color: #ffffff !important; font-size: 0.85rem; font-weight: 800; box-shadow: 0 4px 12px rgba(81, 66, 216, 0.2); }
.summary-travellers { margin-top: 24px; padding-top: 20px; border-top: 1px solid #cbd5e1; }
.summary-travellers h3 { margin: 0 0 10px; font-size: 0.95rem; font-weight: 800; color: #0f172a; }
.summary-travellers p { margin: 4px 0; color: #475467; font-size: 0.88rem; }
.summary-total { display: flex; align-items: center; justify-content: space-between; margin-top: 28px; padding-top: 20px; border-top: 1px solid #cbd5e1; font-size: 1.25rem; color: #0f172a; }
.summary-total strong { color: var(--travel-primary, #5142d8); font-size: 1.4rem; font-weight: 900; }

@media(max-width:950px){ .detail-layout,.travel-tour-main,.travel-booking-modal{grid-template-columns:1fr}.booking-card{position:static}.travel-booking-summary{order:-1}.travel-related-tours>div{grid-template-columns:1fr 1fr}.travel-tour-information{grid-template-columns:1fr}.travel-tour-itinerary{grid-column:auto}}
@media(max-width:650px){ .split,.gallery,.travel-related-tours>div{grid-template-columns:1fr}.people{grid-template-columns:repeat(3,1fr)}.travel-tour-page{padding-top:35px}.travel-tour-title{align-items:start}.travel-tour-main{gap:18px}.travel-tour-image,.travel-tour-image img{height:320px;min-height:320px}.travel-booking-overlay{padding:0}.travel-booking-modal{width:100vw;height:100vh;max-height:100vh;min-height:100vh}.travel-booking-steps{grid-template-columns:1fr;padding:18px 18px 0}.travel-wizard-content-scroll{padding:18px}.travel-wizard-footer{padding:14px 18px}.travel-extra-card{flex-direction:column;align-items:flex-start}.extra-input input{width:100%}.details-fields{grid-template-columns:1fr} }
</style>
