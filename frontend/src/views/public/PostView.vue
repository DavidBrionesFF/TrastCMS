<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'
import type { Post, SiteInfo } from '@/types'

defineProps<{ site?: SiteInfo }>()

const route = useRoute()
const post = ref<Post | null>(null)
const error = ref('')
async function load() {
  error.value = ''
  try { post.value = await api<Post>(`/api/public/posts/${route.params.slug}`) }
  catch (e) { error.value = e instanceof Error ? e.message : 'No se pudo cargar la publicación' }
}
onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <article v-if="post" class="article-page">
    <RouterLink to="/" class="back-link">← Volver</RouterLink>
    <span v-if="post.category" class="badge">{{ post.category.name }}</span>
    <h1>{{ post.title }}</h1>
    <p class="article-lead">{{ post.excerpt }}</p>
    <div class="post-meta"><span>Por {{ post.authorName }}</span><span>{{ post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : '' }}</span></div>
    <img v-if="post.featuredImageUrl" :src="post.featuredImageUrl" :alt="post.title" class="featured-image">
    <div class="article-body" v-html="post.body" />
  </article>
  <div v-else-if="error" class="empty"><h2>Publicación no disponible</h2><p>{{ error }}</p><RouterLink to="/" class="button">Regresar</RouterLink></div>
  <div v-else class="empty">Cargando…</div>
</template>
