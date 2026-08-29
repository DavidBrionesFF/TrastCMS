<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const youtubePlaying = ref(false)
let player: Plyr | null = null

const source = computed(() => props.asset?.publicUrl || props.src || '')
const mime = computed(() => props.asset?.contentType || props.contentType || '')
const youtubeId = computed(() => extractYoutubeId(source.value))
const isYoutube = computed(() => Boolean(youtubeId.value) || mime.value === 'video/youtube')
const youtubeEmbed = computed(() => youtubeId.value
  ? `https://www.youtube-nocookie.com/embed/${youtubeId.value}?autoplay=1&rel=0&modestbranding=1&playsinline=1`
  : '')
const youtubePoster = computed(() => props.poster || (youtubeId.value
  ? `https://i.ytimg.com/vi/${youtubeId.value}/maxresdefault.jpg`
  : ''))

function extractYoutubeId(value: string) {
  if (!value) return ''
  try {
    const url = new URL(value, window.location.origin)
    if (url.hostname.includes('youtu.be')) return url.pathname.split('/').filter(Boolean)[0] || ''
    if (url.hostname.includes('youtube.com') || url.hostname.includes('youtube-nocookie.com')) {
      if (url.pathname.startsWith('/embed/')) return url.pathname.split('/')[2] || ''
      if (url.pathname.startsWith('/shorts/')) return url.pathname.split('/')[2] || ''
      return url.searchParams.get('v') || ''
    }
  } catch {
    const match = value.match(/(?:youtu\.be\/|youtube(?:-nocookie)?\.com\/(?:watch\?v=|embed\/|shorts\/))([A-Za-z0-9_-]{6,})/)
    return match?.[1] || ''
  }
  return ''
}

function isImage() {
  return props.asset?.kind === 'IMAGE' || mime.value.startsWith('image/')
}
function isVideo() {
  return !isYoutube.value && (props.asset?.kind === 'VIDEO' || mime.value.startsWith('video/'))
}
function isAudio() {
  return props.asset?.kind === 'AUDIO' || mime.value.startsWith('audio/')
}

async function mountPlayer() {
  player?.destroy()
  player = null
  await nextTick()
  if (isYoutube.value || !mediaElement.value) return
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

function startYoutube() {
  youtubePlaying.value = true
}

onMounted(mountPlayer)
watch(() => [props.asset?.id, props.src, props.contentType], () => {
  zoom.value = 1
  rotation.value = 0
  fit.value = 'contain'
  youtubePlaying.value = false
  void mountPlayer()
})
onBeforeUnmount(() => player?.destroy())
</script>

<template>
  <div class="trast-media-player w-full">
    <figure v-if="isImage()" class="overflow-hidden rounded-2xl border border-slate-200 bg-slate-950">
      <div class="flex flex-wrap items-center justify-between gap-2 border-b border-white/10 bg-slate-900 px-3 py-2 text-white">
        <span class="truncate text-xs font-semibold">{{ title || asset?.title || asset?.originalFilename || 'Imagen' }}</span>
        <div class="flex items-center gap-1">
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-sm hover:bg-white/10" title="Alejar" @click="zoom=Math.max(.25,zoom-.25)">−</button>
          <span class="min-w-12 text-center text-[11px] text-slate-300">{{ Math.round(zoom * 100) }}%</span>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-sm hover:bg-white/10" title="Acercar" @click="zoom=Math.min(4,zoom+.25)">+</button>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10" title="Rotar" @click="rotation=(rotation+90)%360">↻</button>
          <button type="button" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10" title="Cambiar ajuste" @click="fit=fit==='contain'?'cover':'contain'">{{ fit === 'contain' ? 'Ajustar' : 'Cubrir' }}</button>
          <a :href="source" target="_blank" rel="noopener noreferrer" class="rounded-lg px-2.5 py-1.5 text-xs hover:bg-white/10">Original</a>
        </div>
      </div>
      <div class="grid min-h-72 max-h-[620px] place-items-center overflow-auto bg-[linear-gradient(45deg,#111827_25%,transparent_25%),linear-gradient(-45deg,#111827_25%,transparent_25%),linear-gradient(45deg,transparent_75%,#111827_75%),linear-gradient(-45deg,transparent_75%,#111827_75%)] bg-[length:20px_20px] bg-[position:0_0,0_10px,10px_-10px,-10px_0px] p-4">
        <img :src="source" :alt="title || asset?.altText || asset?.title || asset?.originalFilename" :class="['max-h-[580px] w-full transition-transform duration-200', fit === 'contain' ? 'object-contain' : 'object-cover']" :style="{ transform: `scale(${zoom}) rotate(${rotation}deg)` }">
      </div>
      <figcaption v-if="asset?.caption" class="bg-slate-900 px-4 py-3 text-sm text-slate-300">{{ asset.caption }}</figcaption>
    </figure>

    <section v-else-if="isYoutube" class="trast-youtube-player" :aria-label="title || 'Video de TrastCMS'">
      <iframe
        v-if="youtubePlaying && youtubeEmbed"
        :src="youtubeEmbed"
        :title="title || 'Video de TrastCMS'"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
        allowfullscreen
        referrerpolicy="strict-origin-when-cross-origin"
      />
      <button v-else type="button" class="trast-youtube-player__cover" @click="startYoutube">
        <img v-if="youtubePoster" :src="youtubePoster" :alt="title || 'Vista previa del video'">
        <span class="trast-youtube-player__shade" />
        <span class="trast-youtube-player__play" aria-hidden="true">▶</span>
        <span class="trast-youtube-player__brand">TrastCMS Player</span>
        <strong>{{ title || 'Reproducir video' }}</strong>
      </button>
    </section>

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
      <source :src="source" :type="mime">
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
      <source :src="source" :type="mime">
      Su navegador no puede reproducir este audio.
    </audio>
  </div>
</template>

<style scoped>
.trast-youtube-player{position:relative;width:100%;aspect-ratio:16/9;overflow:hidden;background:#08111c;color:#fff}.trast-youtube-player iframe{width:100%;height:100%;border:0}.trast-youtube-player__cover{position:absolute;inset:0;width:100%;height:100%;display:grid;place-items:center;border:0;padding:0;background:#08111c;color:#fff;cursor:pointer;overflow:hidden}.trast-youtube-player__cover img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}.trast-youtube-player__shade{position:absolute;inset:0;background:linear-gradient(180deg,#06101c2b,#06101cbf)}.trast-youtube-player__play{position:relative;z-index:2;display:grid;place-items:center;width:82px;height:82px;border-radius:999px;background:#fff;color:#553fe0;font-size:1.7rem;padding-left:5px;box-shadow:0 18px 45px #0007;transition:transform .2s ease}.trast-youtube-player__cover:hover .trast-youtube-player__play{transform:scale(1.08)}.trast-youtube-player__brand{position:absolute;z-index:2;left:20px;bottom:18px;padding:7px 10px;border:1px solid #ffffff55;border-radius:999px;background:#08111caa;font-size:.72rem;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.trast-youtube-player__cover strong{position:absolute;z-index:2;right:20px;bottom:21px;max-width:55%;text-align:right;font-size:.9rem}@media(max-width:640px){.trast-youtube-player__play{width:62px;height:62px;font-size:1.25rem}.trast-youtube-player__brand{left:12px;bottom:10px}.trast-youtube-player__cover strong{display:none}}
</style>
