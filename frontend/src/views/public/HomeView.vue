<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { api } from '@/services/api'
import type { PageResponse, Post, PostSummary, SiteInfo } from '@/types'
import { parseDocument } from '@/components/builder/builder'
import BlockRenderer from '@/components/builder/BlockRenderer.vue'

const props=defineProps<{site:SiteInfo}>()
const home=ref<Post|null>(null)
const page=ref<PageResponse<PostSummary>>({content:[],page:0,size:12,totalElements:0,totalPages:0})
const search=ref('');const loading=ref(true);let customStyle:HTMLStyleElement|null=null
const documentData=computed(()=>parseDocument(home.value?.builderData))
function applyCss(css?:string|null){customStyle?.remove();customStyle=null;if(!css)return;customStyle=document.createElement('style');customStyle.textContent=css;document.head.appendChild(customStyle)}
async function load(){loading.value=true;const postsPromise=api<PageResponse<PostSummary>>(`/api/public/posts?search=${encodeURIComponent(search.value)}`);try{home.value=await api<Post>(`/api/public/pages/${props.site.homePageSlug||'inicio'}`);applyCss(home.value.customCss)}catch{home.value=null}page.value=await postsPromise;loading.value=false}
onMounted(load);onUnmounted(()=>customStyle?.remove());watch(()=>props.site.homePageSlug,load)
</script>
<template>
  <template v-if="home?.editorMode==='VISUAL_BUILDER'">
    <BlockRenderer v-for="block in documentData.blocks" :key="block.id" :block="block" />
  </template>
  <section v-else class="hero"><div class="eyebrow">CMS OPEN SOURCE · SPRING BOOT + VUE</div><h1>{{site.tagline||'Contenido rápido, moderno y bajo su control'}}</h1><p>{{site.description}}</p><div class="hero-actions"><a href="#contenido" class="button">Explorar contenido</a><RouterLink to="/admin" class="button secondary">Administrar</RouterLink></div></section>
  <section id="contenido" class="content-section">
    <div class="section-heading"><div><span class="eyebrow">PUBLICACIONES</span><h2>Contenido reciente</h2></div><form class="search" @submit.prevent="load"><input v-model="search" placeholder="Buscar publicaciones"><button>Buscar</button></form></div>
    <div v-if="loading" class="empty">Cargando contenido…</div>
    <div v-else-if="!page.content.length" class="empty"><h3>Todavía no hay publicaciones</h3><p>Publique el primer contenido desde el panel administrativo.</p></div>
    <div v-else class="post-grid"><article v-for="post in page.content" :key="post.id" class="post-card"><div v-if="post.featuredImageUrl" class="card-image" :style="{backgroundImage:`url(${post.featuredImageUrl})`}"></div><div class="card-body"><span v-if="post.category" class="badge">{{post.category.name}}</span><h3><RouterLink :to="`/post/${post.slug}`">{{post.title}}</RouterLink></h3><p>{{post.excerpt||'Abra la publicación para leer el contenido completo.'}}</p><div class="post-meta"><span>{{post.authorName}}</span><span>{{post.publishedAt?new Date(post.publishedAt).toLocaleDateString():''}}</span></div></div></article></div>
  </section>
</template>
