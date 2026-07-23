<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Plyr from 'plyr'
import type { MediaAsset } from '@/types'

const props = defineProps<{
  asset?: MediaAsset | null
  src?: string
  contentType?: string
  poster?: string
  title?: string
}>()

const mediaElement = ref<HTMLMediaElement | null>(null)
const zoom = ref(1)
const rotation = ref(0)
const fit = ref<'contain' | 'cover'>('contain')
let player: Plyr | null = null

function source(): string {
  return props.asset?.publicUrl || props.src || ''
}
function mime(): string {
  return props.asset?.contentType || props.contentType || ''
}
function isImage(): boolean {
  return props.asset?.kind === 'IMAGE' || mime().startsWith('image/')
}
function isVideo(): boolean {
  return props.asset?.kind === 'VIDEO' || mime().startsWith('video/')
}
function isAudio(): boolean {
  return props.asset?.kind === 'AUDIO' || mime().startsWith('audio/')
}

async function mountPlayer() {
  player?.destroy()
  player = null
  await nextTick()
  if (!mediaElement.value) return
  player = new Plyr(mediaElement.value, {
    controls: isVideo()
      ? ['play-large', 'play', 'progress', 'current-time', 'mute', 'volume', 'captions', 'settings', 'pip', 'airplay', 'fullscreen']
      : ['play', 'progress', 'current-time', 'mute', 'volume', 'settings'],
    settings: ['captions', 'quality', 'speed'],
    speed: { selected: 1, options: [0.5, 0.75, 1, 1.25, 1.5, 2] },
    keyboard: { focused: true, global: false },
    tooltips: { controls: true, seek: true }
  })
}

onMounted(mountPlayer)
watch(() => [props.asset?.id, props.src, props.contentType], () => {
  zoom.value = 1
  rotation.value = 0
  fit.value = 'contain'
  mountPlayer()
})
onBeforeUnmount(() => player?.destroy())
</script>

<template>
  <div class="w-full">
    <figure v-if="isImage()" class="overflow-hidden rounded-2xl border border-slate-200 bg-slate-950">
      <div class="flex flex-wrap items-center justify-between gap-2 border-b border-white/10 bg-slate-900 px-3 py-2 text-white">
        <span class="truncate text-xs font-semibold">{{ title || asset?.title || asset?.originalFilename || 'Imagen' }}</span>
        <div class="flex items-center gap-1">
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-sm hover:bg-white/10" title="Alejar" @click="zoom=Math.max(.25,zoom-.25)">−</button>
          <span class="min-w-12 text-center text-[11px] text-slate-300">{{ Math.round(zoom * 100) }}%</span>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-sm hover:bg-white/10" title="Acercar" @click="zoom=Math.min(4,zoom+.25)">+</button>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10" title="Rotar" @click="rotation=(rotation+90)%360">↻</button>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10" title="Cambiar ajuste" @click="fit=fit==='contain'?'cover':'contain'">{{ fit === 'contain' ? 'Ajustar' : 'Cubrir' }}</button>
          <a :href="source()" target="_blank" rel="noopener noreferrer" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10">Original</a>
        </div>
      </div>
      <div class="grid min-h-72 max-h-[620px] place-items-center overflow-auto bg-[linear-gradient(45deg,#111827_25%,transparent_25%),linear-gradient(-45deg,#111827_25%,transparent_25%),linear-gradient(45deg,transparent_75%,#111827_75%),linear-gradient(-45deg,transparent_75%,#111827_75%)] bg-[length:20px_20px] bg-[position:0_0,0_10px,10px_-10px,-10px_0px] p-4">
        <img :src="source()" :alt="title || asset?.altText || asset?.title || asset?.originalFilename" :class="['max-h-[580px] w-full transition-transform duration-200', fit === 'contain' ? 'object-contain' : 'object-cover']" :style="{ transform: `scale(${zoom}) rotate(${rotation}deg)` }">
      </div>
      <figcaption v-if="asset?.caption" class="bg-slate-900 px-4 py-3 text-sm text-slate-300">{{ asset.caption }}</figcaption>
    </figure>

    <video
      v-else-if="isVideo()"
      ref="mediaElement"
      class="w-full"
      playsinline
      controls
      preload="metadata"
      :poster="poster"
      :aria-label="title || asset?.title || asset?.originalFilename"
    >
      <source :src="source()" :type="mime()">
      Su navegador no puede reproducir este video.
    </video>
    <audio
      v-else-if="isAudio()"
      ref="mediaElement"
      class="w-full"
      controls
      preload="metadata"
      :aria-label="title || asset?.title || asset?.originalFilename"
    >
      <source :src="source()" :type="mime()">
      Su navegador no puede reproducir este audio.
    </audio>
  </div>
</template>
