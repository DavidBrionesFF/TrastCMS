<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/services/api'

type Option = { id: string; slug: string; name: string }
const props = withDefaults(defineProps<{
  title?: string
  layout?: 'bar' | 'columns' | 'compact'
  showDate?: boolean
  showTravellers?: boolean
  showActivity?: boolean
  editing?: boolean
}>(), {
  title: 'Where would you like to go?', layout: 'bar', showDate: true,
  showTravellers: true, showActivity: true, editing: false
})
const router = useRouter()
const destinations = ref<Option[]>([])
const activities = ref<Option[]>([])
const destination = ref('')
const activity = ref('')
const date = ref('')
const travellers = ref(2)

onMounted(async () => {
  const [loadedDestinations, loadedActivities] = await Promise.all([
    api<Option[]>('/api/public/travel/destinations').catch(() => []),
    api<Option[]>('/api/public/travel/activities').catch(() => [])
  ])
  destinations.value = loadedDestinations
  activities.value = loadedActivities
})

function search(): void {
  if (props.editing) return
  void router.push({ path: '/travel', query: {
    destination: destination.value || undefined,
    activity: activity.value || undefined,
    date: date.value || undefined,
    travellers: travellers.value
  } })
}
</script>

<template>
  <section :class="['travel-search-widget', `layout-${layout}`]">
    <div class="travel-search-widget__heading"><span>DISCOVER EXPERIENCES</span><h2>{{ title }}</h2></div>
    <form @submit.prevent="search">
      <label><span>Destination</span><select v-model="destination"><option value="">All destinations</option><option v-for="item in destinations" :key="item.id" :value="item.slug">{{ item.name }}</option></select></label>
      <label v-if="showActivity"><span>Activity</span><select v-model="activity"><option value="">All activities</option><option v-for="item in activities" :key="item.id" :value="item.slug">{{ item.name }}</option></select></label>
      <label v-if="showDate"><span>Date</span><input v-model="date" type="date"></label>
      <label v-if="showTravellers"><span>Travellers</span><input v-model.number="travellers" type="number" min="1" max="99"></label>
      <button>Search tours</button>
    </form>
  </section>
</template>

<style scoped>
.travel-search-widget{--brand:var(--theme-primary,var(--primary,#087f7b));--brand-strong:var(--theme-primary-hover,var(--primary-strong,#066d69));--accent:var(--theme-accent,var(--accent,#f2ad44));padding:clamp(24px,4vw,40px);border-radius:var(--theme-radius,24px);background:linear-gradient(125deg,var(--brand-strong),var(--brand));color:white}.travel-search-widget__heading span{font-size:.68rem;font-weight:900;letter-spacing:.16em;color:color-mix(in srgb,var(--accent) 75%,white)}.travel-search-widget h2{margin:.4rem 0 1.3rem;font-size:clamp(1.8rem,5vw,3.3rem)}form{display:grid;grid-template-columns:1.25fr 1fr 1fr .65fr auto;gap:10px;padding:10px;border-radius:16px;background:var(--theme-surface,var(--surface,#fff));box-shadow:var(--theme-shadow,0 16px 45px #063f4325)}label{display:grid;gap:5px;color:var(--theme-text,var(--text,#092c2f));font-size:.67rem;font-weight:900}form :is(select,input,button){min-width:0;border:1px solid var(--theme-border,var(--line,#d9e6e3));border-radius:10px;padding:11px;background:var(--theme-surface,var(--surface,#fff));color:var(--theme-text,var(--text,#092c2f))}button{align-self:end;border-color:var(--accent)!important;background:var(--accent)!important;color:#1e2b2d!important;font-weight:900}.layout-columns form{grid-template-columns:repeat(2,minmax(0,1fr));padding:18px}.layout-columns button{grid-column:1/-1}.layout-compact{padding:20px}.layout-compact .travel-search-widget__heading h2{font-size:1.45rem}.layout-compact form{grid-template-columns:1fr 1fr auto}.layout-compact label:nth-of-type(n+3){display:none}@media(max-width:900px){form{grid-template-columns:repeat(2,minmax(0,1fr))}button{grid-column:1/-1}}@media(max-width:600px){form,.layout-columns form,.layout-compact form{grid-template-columns:1fr}.layout-compact label:nth-of-type(n+3){display:grid}}
</style>
