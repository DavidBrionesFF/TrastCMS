<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'

interface TravelDestination {
  id: string
  name: string
  slug: string
  summary?: string | null
  imageUrl?: string | null
  country?: string | null
  region?: string | null
  city?: string | null
}

const props = withDefaults(
  defineProps<{
    limit?: number
    editing?: boolean
  }>(),
  {
    limit: 6,
    editing: false
  }
)

const rows = ref<TravelDestination[]>([])

onMounted(async () => {
  const destinations = await api<TravelDestination[]>('/api/public/travel/destinations')
    .catch(() => [])

  rows.value = destinations.slice(0, Math.max(0, props.limit))
})
</script>

<template>
  <div class="destinations">
    <RouterLink
      v-for="item in rows"
      :key="item.id"
      :to="editing ? '' : { path: '/travel', query: { destination: item.slug } }"
      class="destination"
      :style="item.imageUrl
        ? { backgroundImage: `linear-gradient(0deg,#032e31cc,#032e3110),url(${item.imageUrl})` }
        : {}"
    >
      <div>
        <span>{{ item.city || item.region || item.country }}</span>
        <h3>{{ item.name }}</h3>
        <p>{{ item.summary }}</p>
      </div>
    </RouterLink>
  </div>
</template>

<style scoped>
.destinations {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.destination {
  min-height: 300px;
  display: flex;
  align-items: end;
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(145deg, #0b7772, #063f43);
  background-size: cover;
  background-position: center;
  color: white;
  text-decoration: none;
}

.destination span {
  font-size: 0.7rem;
  letter-spacing: 0.12em;
  font-weight: 900;
  color: #70eee3;
}

.destination h3 {
  font-size: 1.8rem;
  margin: 0.3rem 0;
}

.destination p {
  margin: 0;
  color: #d7edeb;
}

@media (max-width: 750px) {
  .destinations {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .destinations {
    grid-template-columns: 1fr;
  }
}
</style>
