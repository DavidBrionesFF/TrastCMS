<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'
import type { PageResponse, Post, PostSummary } from '@/types'
import { parseDocument } from '@/components/builder/builder'
import BlockRenderer from '@/components/builder/BlockRenderer.vue'

const route = useRoute()
const page = ref<Post | null>(null)
const blogPosts = ref<PostSummary[]>([])
const error = ref('')
let customStyle: HTMLStyleElement | null = null

const builderDocument = computed(() => parseDocument(page.value?.builderData))
const isVisual = computed(() => page.value?.editorMode === 'VISUAL_BUILDER')

function applyCustomCss(css?: string | null) {
  customStyle?.remove()
  customStyle = null
  if (!css) return
  customStyle = document.createElement('style')
  customStyle.dataset.trastPageStyle = String(route.params.slug || '')
  customStyle.textContent = css
  document.head.appendChild(customStyle)
}

async function load() {
  error.value = ''
  try {
    page.value = await api<Post>(`/api/public/pages/${route.params.slug}`)
    blogPosts.value = page.value.pageRole === 'BLOG'
      ? (await api<PageResponse<PostSummary>>('/api/public/posts?size=24')).content
      : []
    applyCustomCss(page.value.customCss)
    document.title = page.value.seoTitle || page.value.title
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar la página'
  }
}

onMounted(load)
onUnmounted(() => customStyle?.remove())
watch(() => route.params.slug, load)
</script>

<template>
  <article v-if="page" :class="['page-content', { 'visual-page': isVisual }]">
    <template v-if="isVisual">
      <BlockRenderer v-for="block in builderDocument.blocks" :key="block.id" :block="block" />
      <div v-if="!builderDocument.blocks.length" class="empty"><h2>Página sin contenido</h2><p>Edite esta página desde el constructor visual.</p></div>
    </template>
    <template v-else>
      <div class="article-page">
        <RouterLink to="/" class="back-link">← Volver</RouterLink>
        <h1>{{ page.title }}</h1>
        <p v-if="page.excerpt" class="article-lead">{{ page.excerpt }}</p>
        <img v-if="page.featuredImageUrl" :src="page.featuredImageUrl" :alt="page.title" class="featured-image">
        <div class="article-body" v-html="page.body" />
      </div>
    </template>
    <section v-if="page.pageRole === 'BLOG'" class="content-section pt-0">
      <div class="section-heading"><div><span class="eyebrow">BLOG</span><h2>Publicaciones recientes</h2></div></div>
      <div v-if="blogPosts.length" class="post-grid"><article v-for="post in blogPosts" :key="post.id" class="post-card"><div v-if="post.featuredImageUrl" class="card-image" :style="{backgroundImage:`url(${post.featuredImageUrl})`}"></div><div class="card-body"><span v-if="post.category" class="badge">{{post.category.name}}</span><h3><RouterLink :to="`/post/${post.slug}`">{{post.title}}</RouterLink></h3><p>{{post.excerpt||'Abra la publicación para leer el contenido completo.'}}</p><div class="post-meta"><span>{{post.authorName}}</span><span>{{post.publishedAt?new Date(post.publishedAt).toLocaleDateString():''}}</span></div></div></article></div>
      <div v-else class="empty"><h3>Todavía no hay publicaciones</h3><p>El contenido publicado aparecerá en esta página automáticamente.</p></div>
    </section>
  </article>
  <div v-else-if="error" class="empty"><h2>Página no disponible</h2><p>{{ error }}</p><RouterLink to="/" class="button">Regresar</RouterLink></div>
  <div v-else class="empty">Cargando…</div>
</template>
