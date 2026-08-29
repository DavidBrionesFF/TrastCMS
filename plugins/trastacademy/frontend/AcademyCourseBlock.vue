<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { api } from '@/services/api'
const props=withDefaults(defineProps<{courseSlug?:string;editing?:boolean}>(),{courseSlug:'',editing:false})
const course=ref<any>(null);const error=ref('')
async function load(){if(props.editing||!props.courseSlug)return;try{course.value=await api<any>(`/api/public/academy/courses/${encodeURIComponent(props.courseSlug)}`)}catch(e){error.value=e instanceof Error?e.message:'Curso no disponible'}}
onMounted(load);watch(()=>props.courseSlug,load)
</script>
<template><section class="course-block"><div v-if="editing" class="placeholder"><b>Detalle de curso</b><span>{{ courseSlug||'Configure el slug del curso' }}</span></div><p v-else-if="error">{{ error }}</p><div v-else-if="course" class="content"><div><small>TRAST ACADEMY</small><h2>{{ course.title }}</h2><p>{{ course.excerpt }}</p><RouterLink :to="`/academy/${course.slug}`">Ver programa</RouterLink></div><img v-if="course.featuredImageUrl" :src="course.featuredImageUrl" :alt="course.title"></div></section></template>
<style scoped>.placeholder{padding:2rem;border:1px dashed var(--border,#cbd5e1);border-radius:1rem;display:grid;text-align:center}.content{display:grid;grid-template-columns:1fr minmax(260px,.65fr);gap:2rem;align-items:center;padding:clamp(1.5rem,4vw,4rem);border-radius:1.4rem;background:color-mix(in srgb,var(--primary,#5b4ee8) 8%,var(--surface,#fff));overflow:hidden}.content small{font-weight:900;color:var(--primary,#5b4ee8)}.content h2{font-size:clamp(2rem,4vw,3.5rem);margin:.5rem 0}.content p{line-height:1.7;color:var(--muted,#64748b)}.content a{display:inline-flex;margin-top:1rem;padding:.8rem 1rem;border-radius:.75rem;background:var(--primary,#5b4ee8);color:#fff;text-decoration:none;font-weight:850}.content img{width:100%;border-radius:1rem;aspect-ratio:16/10;object-fit:cover}@media(max-width:700px){.content{grid-template-columns:1fr}}</style>
