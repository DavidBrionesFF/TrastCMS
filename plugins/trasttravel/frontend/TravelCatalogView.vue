<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import UiIcon from '@/components/common/UiIcon.vue'

type Row = Record<string, any>
const router = useRouter()
const route = useRoute()
const loading = ref(true)
const error = ref('')
const results = ref<Row[]>([])
const destinations = ref<Row[]>([])
const activities = ref<Row[]>([])
const total = ref(0)
const sort = ref('recent')
const filters = reactive({
  search: String(route.query.search || ''),
  destination: String(route.query.destination || ''),
  activity: String(route.query.activity || ''),
  date: String(route.query.date || ''),
  travellers: Math.max(1, Number(route.query.travellers || 1))
})

const sortedResults = computed(() => {
  const items = [...results.value]
  if (sort.value === 'price-asc') return items.sort((a, b) => Number(a.adultPrice || 0) - Number(b.adultPrice || 0))
  if (sort.value === 'price-desc') return items.sort((a, b) => Number(b.adultPrice || 0) - Number(a.adultPrice || 0))
  if (sort.value === 'name') return items.sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'en'))
  return items
})

onMounted(search)

async function search() {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams()
  const query: Record<string, string> = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (String(value || '').trim()) {
      params.set(key, String(value))
      query[key] = String(value)
    }
  })
  await router.replace({ path: '/travel', query })
  try {
    const response = await api<Row>(`/api/public/travel/search?${params}`)
    results.value = response.tours || []
    destinations.value = response.destinations || []
    activities.value = response.activities || []
    total.value = response.total ?? results.value.length
  } catch (exception) {
    results.value = []
    total.value = 0
    error.value = exception instanceof Error
      ? exception.message
      : 'The travel catalogue could not be loaded.'
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filters.search = ''
  filters.destination = ''
  filters.activity = ''
  filters.date = ''
  filters.travellers = 1
  void search()
}

function money(value: any, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(Number(value || 0))
}

function durationLabel(tour: Row) {
  const unit = tour.durationUnit === 'DAY' ? 'day(s)' : tour.durationUnit === 'HOUR' ? 'hour(s)' : 'minute(s)'
  return `${tour.durationValue || 1} ${unit}`
}

function tourImage(tour: Row) {
  return tour.featuredImageUrl ? String(tour.featuredImageUrl) : ''
}

function recoverTourImage(event: Event, _tour?: Row) {
  const image = event.currentTarget as HTMLImageElement
  image.style.display = 'none'
}

function openTour(tour: Row) {
  void router.push(`/travel/${tour.slug}`)
}
</script>

<template>
  <div class="travel-catalog">
    <section class="travel-hero">
      <div class="travel-shell">
        <span class="travel-kicker">MEMORABLE EXPERIENCES</span>
        <h1>Find your next experience.</h1>
        <p>Explore the full catalogue or filter by destination, activity, date, and number of travellers.</p>
        <form class="travel-search" @submit.prevent="search">
          <label>
            <span><UiIcon name="map" :size="15" />Destination</span>
            <select v-model="filters.destination">
              <option value="">All destinations</option>
              <option v-for="item in destinations" :key="item.id || item.slug" :value="item.slug">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span><UiIcon name="grid" :size="15" />Activity</span>
            <select v-model="filters.activity">
              <option value="">Any activity</option>
              <option v-for="item in activities" :key="item.id || item.slug" :value="item.slug">{{ item.name }}</option>
            </select>
          </label>
          <label>
            <span><UiIcon name="calendar" :size="15" />Date</span>
            <input v-model="filters.date" type="date">
          </label>
          <label>
            <span><UiIcon name="users" :size="15" />Travellers</span>
            <input v-model.number="filters.travellers" type="number" min="1" max="99">
          </label>
          <button type="submit"><UiIcon name="search" :size="18" />Search tours</button>
        </form>
      </div>
    </section>

    <section class="travel-shell travel-content">
      <header class="travel-results-head">
        <div><span class="travel-kicker">TOURS & ACTIVITIES</span><h1>Explore Our Experiences</h1></div>
        <p>{{ total }} options available to book in Roatán.</p>
      </header>

      <div class="catalog-layout">
        <aside class="catalog-sidebar">
          <div class="catalog-sidebar__title"><strong>Filter By</strong><button type="button" @click="clearFilters">Clear</button></div>
          <label class="catalog-filter catalog-filter--search">
            <span>Search</span>
            <div><UiIcon name="search" :size="15" /><input v-model="filters.search" placeholder="Tour name" @keyup.enter="search"></div>
          </label>
          <label class="catalog-filter">
            <span>Destination</span>
            <select v-model="filters.destination" @change="search">
              <option value="">All destinations</option>
              <option v-for="item in destinations" :key="item.id || item.slug" :value="item.slug">{{ item.name }}</option>
            </select>
          </label>
          <label class="catalog-filter">
            <span>Activities</span>
            <select v-model="filters.activity" @change="search">
              <option value="">All activities</option>
              <option v-for="item in activities" :key="item.id || item.slug" :value="item.slug">{{ item.name }}</option>
            </select>
          </label>
          <label class="catalog-filter">
            <span>Travel date</span>
            <input v-model="filters.date" type="date" @change="search">
          </label>
          <label class="catalog-filter">
            <span>Travellers</span>
            <input v-model.number="filters.travellers" type="number" min="1" max="99">
          </label>
          <button class="catalog-filter-button" type="button" @click="search"><UiIcon name="search" :size="16" />Apply filters</button>
        </aside>

        <section class="catalog-results">
          <div class="catalog-toolbar">
            <div class="catalog-search-inline">
              <UiIcon name="search" :size="18" />
              <input v-model="filters.search" placeholder="Search tours" @keyup.enter="search">
            </div>
            <select v-model="sort" aria-label="Sort tours">
              <option value="recent">Sort: Recently Added</option>
              <option value="name">Name A–Z</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
            </select>
            <span class="catalog-count">{{ total }} result{{ total === 1 ? '' : 's' }}</span>
          </div>

          <div v-if="error" class="travel-alert" role="alert">
            <UiIcon name="info" :size="21" />
            <div><b>The catalogue could not be loaded.</b><p>{{ error }}</p></div>
            <button type="button" @click="search">Try again</button>
          </div>
          <div v-else-if="loading" class="travel-empty">Searching experiences…</div>
          <div v-else-if="sortedResults.length" class="travel-grid">
            <article v-for="tour in sortedResults" :key="tour.id || tour.slug" class="travel-card" @click="openTour(tour)">
              <div class="travel-card__media">
                <img v-if="tourImage(tour)" :src="tourImage(tour)" :alt="tour.name" @error="recoverTourImage($event, tour)">
                <div v-else class="travel-card__placeholder"><UiIcon name="image" :size="48" /></div>
                <span v-if="tour.featured" class="travel-card__badge">Featured</span>
              </div>
              <div class="travel-card__body">
                <span class="travel-card__destination"><UiIcon name="map" :size="14" />{{ tour.destination?.name || 'Roatán' }}</span>
                <h3>{{ tour.name }}</h3>
                <p>{{ tour.shortDescription || 'View the full details, dates, and rates for this experience.' }}</p>
                <div class="travel-card__facts">
                  <span><UiIcon name="calendar" :size="15" />{{ durationLabel(tour) }}</span>
                  <span><UiIcon name="users" :size="15" />Up to {{ tour.maximumGroupSize || 1 }}</span>
                  <span v-if="tour.difficulty"><UiIcon name="info" :size="15" />{{ tour.difficulty }}</span>
                </div>
                <div class="travel-card__months"><small>Availability:</small><span>Jan</span><span>Feb</span><span>Mar</span><span>Apr</span><span>May</span><span>Jun</span><span>Jul</span><span>Aug</span><span>Sep</span><span>Oct</span><span>Nov</span><span>Dec</span></div>
              </div>
              <aside class="travel-card__price">
                <small>From</small>
                <strong>{{ money(tour.adultPrice, tour.currency) }}</strong>
                <span>{{ tour.pricingMode === 'PER_GROUP' ? 'per group' : 'per person' }}</span>
                <button type="button" @click.stop="openTour(tour)">View Details</button>
                <div class="travel-card__next"><small>Next Departures</small><span>✓ Check live availability</span></div>
              </aside>
            </article>
          </div>
          <div v-else class="travel-empty">
            <UiIcon name="search" :size="42" />
            <b>No tours matched those filters.</b>
            <p>The catalogue is still available. Remove the date or change the destination to see more experiences.</p>
            <button type="button" @click="clearFilters">Clear filters</button>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.travel-catalog {
  --travel-primary: var(--theme-primary, #087f7b);
  --travel-primary-hover: var(--theme-primary-hover, #066d69);
  --travel-accent: var(--theme-accent, #f2ad44);
  --travel-ink: var(--theme-text, #092c2f);
  --travel-muted: var(--theme-text-muted, #617572);
  --travel-border: var(--theme-border, #d9e6e3);
  --travel-surface: var(--theme-surface, #fff);
  --travel-soft: var(--theme-surface-muted, #f4f8f6);
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 70vh;
  overflow-x: clip;
  color: var(--travel-ink);
  background: var(--travel-surface);
}
.travel-shell {
  width: min(var(--site-container-width, 1180px), calc(100% - 48px));
  max-width: var(--site-container-width, 1180px);
  min-width: 0;
  margin-inline: auto;
  box-sizing: border-box;
}
.travel-hero {
  width: 100%;
  padding: 88px 0 54px;
  background: linear-gradient(110deg, rgba(2, 38, 42, .92), rgba(5, 103, 99, .78)),
    url('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1800&q=80') center / cover;
}
.travel-kicker { font-size: .72rem; font-weight: 900; letter-spacing: .18em; color: color-mix(in srgb, var(--travel-primary) 72%, #31e5d6); }
.travel-hero h1 { max-width: 840px; margin: .45rem 0; font-size: clamp(2.6rem, 7vw, 5.6rem); line-height: .95; color: #fff; }
.travel-hero p { max-width: 690px; color: #d5f3ef; font-size: 1.08rem; }
.travel-search { display: grid; grid-template-columns: 1.4fr 1.2fr 1fr .7fr auto; gap: 10px; margin-top: 34px; padding: 12px; border-radius: 20px; background: #fff; box-shadow: 0 24px 60px #001f2040; }
.travel-search label { display: grid; gap: 4px; min-width: 0; padding: 4px 12px; border-right: 1px solid #d9e4e2; }
.travel-search label > span { display: flex; align-items: center; gap: 6px; font-size: .68rem; text-transform: uppercase; font-weight: 800; letter-spacing: .08em; color: #66817f; }
.travel-search :is(input, select) { width: 100%; min-width: 0; border: 0; padding: 7px 0; background: transparent; color: #123; outline: none; }
.travel-search button,
.travel-card button,
.travel-empty button,
.travel-alert button,
.catalog-filter-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; border: 0; border-radius: 13px; background: var(--travel-primary); color: #fff; font-weight: 800; padding: 13px 20px; cursor: pointer; }
.travel-search button:hover,
.travel-card button:hover,
.travel-empty button:hover,
.travel-alert button:hover,
.catalog-filter-button:hover { background: var(--travel-primary-hover); }
.travel-content { width: min(var(--site-container-width, 1180px), calc(100% - 48px)); padding: 64px 0 90px; }
.travel-results-head { display: flex; align-items: end; justify-content: space-between; gap: 30px; margin-bottom: 28px; }
.travel-results-head h1 { font-size: clamp(2rem, 4vw, 3.4rem); margin: .3rem 0; }
.travel-results-head p { max-width: 420px; color: var(--travel-muted); text-align: right; }
.catalog-layout { display: block; min-width: 0; }
.catalog-sidebar { display: none; }
.catalog-results { min-width: 0; }
.catalog-toolbar { display: flex; align-items: center; gap: 12px; min-width: 0; margin-bottom: 20px; }
.catalog-toolbar > select { margin-left: auto; border: 1px solid var(--travel-border); border-radius: 12px; background: var(--travel-surface); padding: 11px 13px; color: var(--travel-ink); }
.catalog-count { font-size: .8rem; color: var(--travel-muted); }
.catalog-search-inline { display: flex; align-items: center; gap: 9px; width: min(380px, 100%); min-width: 0; border: 1px solid var(--travel-border); border-radius: 14px; padding: 0 14px; background: var(--travel-surface); }
.catalog-search-inline input { width: 100%; min-width: 0; border: 0; padding: 12px 0; background: transparent; outline: none; }
.travel-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 24px; min-width: 0; }
.travel-card { overflow: hidden; min-width: 0; border: 1px solid var(--travel-border); border-radius: 24px; background: var(--travel-surface); box-shadow: 0 12px 36px #0c4d4d12; cursor: pointer; transition: .25s; }
.travel-card:hover { transform: translateY(-5px); box-shadow: 0 20px 50px #0c4d4d22; }
.travel-card__media { height: 235px; position: relative; min-width: 0; background: color-mix(in srgb, var(--travel-primary) 14%, #fff); }
.travel-card__media img { width: 100%; height: 100%; object-fit: cover; }
.travel-card__placeholder { display: grid; place-items: center; height: 100%; color: var(--travel-primary); }
.travel-card__badge { position: absolute; top: 16px; left: 16px; background: #fff; color: var(--travel-primary); border-radius: 999px; padding: 7px 11px; font-size: .75rem; font-weight: 900; }
.travel-card__body { min-width: 0; padding: 22px; }
.travel-card__destination { display: flex; align-items: center; gap: 5px; color: var(--travel-primary); text-transform: uppercase; letter-spacing: .08em; font-size: .72rem; font-weight: 900; }
.travel-card h3 { font-size: 1.35rem; margin: .35rem 0; }
.travel-card p { height: 45px; overflow: hidden; color: var(--travel-muted); font-size: .92rem; line-height: 1.55; }
.travel-card__facts { display: flex; flex-wrap: wrap; gap: 15px; padding: 16px 0; color: var(--travel-muted); font-size: .78rem; border-bottom: 1px solid var(--travel-border); }
.travel-card__facts span { display: flex; align-items: center; gap: 5px; }
.travel-card__months { display: flex; gap: 8px; align-items: center; overflow: hidden; padding-top: 12px; color: #98a2b3; font-size: .62rem; }
.travel-card__months small { color: var(--travel-muted); }
.travel-card__price { display: grid; gap: 4px; min-width: 0; padding: 0 22px 22px; }
.travel-card__price strong { font-size: 1.45rem; }
.travel-card__price > span,
.travel-card__price > small { color: var(--travel-muted); font-size: .7rem; }
.travel-card__next { display: none; }
.travel-empty { display: grid; justify-items: center; gap: 10px; padding: 70px; text-align: center; border: 1px dashed var(--travel-border); border-radius: 24px; background: var(--travel-soft); color: var(--travel-muted); }
.travel-empty b { color: var(--travel-ink); font-size: 1.15rem; }
.travel-empty p { margin: 0; }
.travel-alert { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; padding: 18px; border: 1px solid #f5c2c0; border-radius: 18px; background: #fff4f3; color: #8f2d27; }
.travel-alert div { flex: 1; }
.travel-alert p { margin: .25rem 0 0; }
.travel-alert button { background: #a33b34; padding: 9px 14px; }
@media (max-width: 900px) {
  .travel-search { grid-template-columns: 1fr 1fr; }
  .travel-search button { grid-column: 1 / -1; }
  .travel-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .travel-shell,
  .travel-content { width: min(calc(100% - 28px), var(--site-container-width, 1180px)); }
  .travel-results-head { align-items: flex-start; flex-direction: column; }
  .travel-results-head p { text-align: left; }
  .catalog-toolbar { align-items: stretch; flex-direction: column; }
  .catalog-toolbar > select { width: 100%; margin-left: 0; }
  .catalog-count { display: none; }
}
@media (max-width: 620px) {
  .travel-hero { padding: 60px 0 35px; }
  .travel-search { grid-template-columns: 1fr; }
  .travel-search label { border-right: 0; border-bottom: 1px solid #e5eceb; }
  .travel-grid { grid-template-columns: 1fr; }
  .travel-card__media { height: 210px; }
  .travel-empty { padding: 40px 20px; }
  .travel-alert { align-items: flex-start; flex-wrap: wrap; }
}
</style>
