<script setup lang="ts">
import TravelArrivalLocationField from './TravelArrivalLocationField.vue'

import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const email = ref(String(route.query.email || ''))
const booking = ref<Record<string, any> | null>(null)
const error = ref('')
const loading = ref(false)
const addingPayment = ref(false)

const canPay = computed(() => {
  if (!booking.value || Number(booking.value.balanceDue || 0) <= 0) return false
  return booking.value.bookingStatus === 'PENDING_PAYMENT'
    || booking.value.paymentStatus === 'PARTIALLY_PAID'
})

onMounted(async () => {
  if (!auth.loaded) await auth.load().catch(() => undefined)
  if (auth.user.authenticated) await loadFromAccount()
  else if (email.value) await lookup()
})

async function loadFromAccount() {
  loading.value = true
  error.value = ''
  try {
    booking.value = await api(`/api/account/travel/bookings/${route.params.code}`)
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'The booking could not be found'
  } finally {
    loading.value = false
  }
}

async function lookup() {
  loading.value = true
  error.value = ''
  try {
    booking.value = await api(
      `/api/public/travel/bookings/${route.params.code}?email=${encodeURIComponent(email.value)}`
    )
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'The booking could not be found'
  } finally {
    loading.value = false
  }
}

async function payBalance() {
  if (!booking.value) return
  addingPayment.value = true
  error.value = ''
  try {
    await cart.ensure(booking.value.currency)
    await cart.add('travel', booking.value.id, 1, { bookingCode: booking.value.bookingCode })
    await router.push('/cart')
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'The payment could not be prepared'
  } finally {
    addingPayment.value = false
  }
}

function money(value: unknown, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency })
    .format(Number(value || 0))
}

const arrivalLocationId = ref<number | null>(null)
const arrivalLocationLabel = ref('')
</script>

<template>
  <main class="lookup">
    <section class="card">
      <span class="kicker">TRIP LOOKUP</span>
      <h1>Booking {{ route.params.code }}</h1>

      <div v-if="loading" class="loading">Loading booking…</div>

      <form v-else-if="!booking && !auth.user.authenticated" @submit.prevent="lookup">
        <p>Enter the same email address used during checkout.</p>
        <input v-model="email" type="email" required placeholder="email@example.com">
        <button>{{ loading ? 'Loading…' : 'View booking' }}</button>
      </form>

      <div v-else-if="booking" class="details">
        <div class="status">
          <span>{{ booking.bookingStatus }}</span>
          <b>{{ booking.paymentStatus }}</b>
        </div>
        <h2>{{ booking.tourName }}</h2>
        <p>{{ new Date(booking.startsAt).toLocaleString('en-US', { dateStyle: 'full', timeStyle: 'short' }) }}</p>
        <dl>
          <div><dt>Customer</dt><dd>{{ booking.customerName }}</dd></div>
          <div><dt>Travellers</dt><dd>{{ booking.adults + booking.children + booking.infants }}</dd></div>
          <div><dt>Total</dt><dd>{{ money(booking.grandTotal, booking.currency) }}</dd></div>
          <div><dt>Paid</dt><dd>{{ money(booking.amountPaid, booking.currency) }}</dd></div>
          <div><dt>Balance</dt><dd>{{ money(booking.balanceDue, booking.currency) }}</dd></div>
          <div><dt>Code</dt><dd>{{ booking.bookingCode }}</dd></div>
        </dl>
        <button v-if="canPay" class="pay" :disabled="addingPayment" @click="payBalance">
          {{ addingPayment ? 'Preparing payment…' : `Pay ${money(booking.balanceDue, booking.currency)}` }}
        </button>
        <a :href="booking.googleCalendarUrl" target="_blank" rel="noopener">Add to Google Calendar</a>
        <RouterLink to="/my-account">Open My Account</RouterLink>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </main>
<TravelArrivalLocationField v-model="arrivalLocationId" @update:label="arrivalLocationLabel = $event" />
      </template>

<style scoped>
.lookup{display:grid;place-items:center;min-height:70vh;padding:40px;background:var(--theme-surface-muted,var(--surface-soft,#eff8f6))}.card{width:min(680px,100%);padding:35px;border-radius:28px;background:var(--theme-surface,var(--surface,#fff));box-shadow:0 24px 70px #075c5a1b}.kicker{font-size:.72rem;letter-spacing:.16em;color:var(--theme-primary,var(--primary,#087f7b));font-weight:900}.card h1{font-size:2.2rem;margin:.5rem 0 1.4rem}.card form{display:grid;gap:14px}.card input{border:1px solid var(--theme-border,var(--line,#ccdcd9));border-radius:13px;padding:14px}.card button,.details a{border:0;border-radius:13px;background:var(--theme-primary,var(--primary,#087f7b));color:white;padding:13px;text-align:center;font-weight:800}.details .pay{background:var(--theme-accent,var(--accent,#f2ad44))}.error{color:#b42318}.loading{padding:25px;border-radius:14px;background:var(--theme-surface-muted,var(--surface-soft,#f4f8f7))}.status{display:flex;gap:8px}.status>*{padding:6px 10px;border-radius:999px;background:#e8f7f3;color:#12634f;font-size:.78rem}.details dl{display:grid;grid-template-columns:1fr 1fr;gap:12px}.details dl div{padding:14px;background:var(--theme-surface-muted,var(--surface-soft,#f4f8f7));border-radius:12px}.details dt{font-size:.7rem;color:#668}.details dd{margin:4px 0 0;font-weight:800}.details{display:grid;gap:14px}.details a{display:block}.details .router-link-active{color:var(--theme-primary,var(--primary,#087f7b))}@media(max-width:560px){.lookup{padding:18px}.card{padding:23px}.details dl{grid-template-columns:1fr}}
</style>
