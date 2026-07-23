<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { BuilderBlock } from '@/types'
import MediaPlayer from '@/components/media/MediaPlayer.vue'
import CrmFormBlock from '@plugins/trastcrm/frontend/CrmFormBlock.vue'

const props = withDefaults(defineProps<{
  block: BuilderBlock
  editing?: boolean
  selected?: boolean
}>(), { editing: false, selected: false })

const emit = defineEmits<{
  select: [id: string]
  moveUp: [id: string]
  moveDown: [id: string]
  duplicate: [id: string]
  remove: [id: string]
  addChild: [id: string]
}>()

const style = computed(() => ({
  backgroundColor: props.block.style.backgroundColor || undefined,
  backgroundImage: props.block.style.backgroundImage ? `url(${props.block.style.backgroundImage})` : undefined,
  backgroundSize: props.block.style.backgroundImage ? 'cover' : undefined,
  backgroundPosition: props.block.style.backgroundImage ? 'center' : undefined,
  color: props.block.style.color || undefined,
  textAlign: props.block.style.textAlign || undefined,
  paddingTop: `${props.block.style.paddingTop || 0}px`,
  paddingRight: `${props.block.style.paddingRight || 0}px`,
  paddingBottom: `${props.block.style.paddingBottom || 0}px`,
  paddingLeft: `${props.block.style.paddingLeft || 0}px`,
  marginTop: `${props.block.style.marginTop || 0}px`,
  marginRight: `${props.block.style.marginRight || 0}px`,
  marginBottom: `${props.block.style.marginBottom || 0}px`,
  marginLeft: `${props.block.style.marginLeft || 0}px`,
  borderRadius: `${props.block.style.borderRadius || 0}px`,
  maxWidth: props.block.style.maxWidth ? `${props.block.style.maxWidth}px` : undefined,
  minHeight: props.block.style.minHeight ? `${props.block.style.minHeight}px` : undefined,
  marginInline: props.block.style.maxWidth ? 'auto' : undefined
}))

const columns = computed(() => Math.min(4, Math.max(1, Number(props.block.data.columns || 2))))
const stats = computed(() => Array.isArray(props.block.data.items) ? props.block.data.items as Array<{ value: string; label: string }> : [])
const gallery = computed(() => Array.isArray(props.block.data.images) ? props.block.data.images as Array<{ src: string; alt?: string; caption?: string }> : [])

const imageStyle = computed<CSSProperties>(() => ({
  height: `${Number(props.block.data.height || 420)}px`,
  objectFit: normalizeObjectFit(props.block.data.objectFit),
  borderRadius: `${props.block.style.borderRadius || 16}px`
}))

const dividerStyle = computed<CSSProperties>(() => ({
  width: `${Number(props.block.data.width || 100)}%`,
  borderTopWidth: `${Number(props.block.data.thickness || 1)}px`,
  borderTopStyle: normalizeBorderStyle(props.block.data.style),
  borderColor: props.block.style.color || '#cbd5e1',
  marginInline: 'auto'
}))

function normalizeObjectFit(value: unknown): CSSProperties['objectFit'] {
  const normalized = String(value || 'cover')
  return ['contain', 'cover', 'fill', 'none', 'scale-down'].includes(normalized)
    ? normalized as CSSProperties['objectFit']
    : 'cover'
}

function normalizeBorderStyle(value: unknown): CSSProperties['borderTopStyle'] {
  const normalized = String(value || 'solid')
  return ['none', 'hidden', 'dotted', 'dashed', 'solid', 'double', 'groove', 'ridge', 'inset', 'outset'].includes(normalized)
    ? normalized as CSSProperties['borderTopStyle']
    : 'solid'
}
const pluginHtml = computed(() => {
  if (!String(props.block.type).startsWith('plugin:')) return ''
  const template = String(props.block.data._template || '<div class="plugin-block">Plugin block</div>')
  return template.replace(/{{\s*([a-zA-Z0-9_.-]+)\s*}}/g, (_match, key: string) => escapeHtml(props.block.data[key]))
})

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}
</script>

<template>
  <div
    :id="block.style.anchorId || undefined"
    :class="[editing ? 'builder-block' : '', { selected }, block.style.cssClass]"
    :style="style"
    @click.stop="editing && emit('select', block.id)"
  >
    <div v-if="editing" class="builder-block__actions">
      <button type="button" title="Mover arriba" @click.stop="emit('moveUp', block.id)">↑</button>
      <button type="button" title="Mover abajo" @click.stop="emit('moveDown', block.id)">↓</button>
      <button type="button" title="Duplicar" @click.stop="emit('duplicate', block.id)">⧉</button>
      <button v-if="block.type === 'section' || block.type === 'columns'" type="button" title="Agregar elemento" @click.stop="emit('addChild', block.id)">＋</button>
      <button type="button" title="Eliminar" @click.stop="emit('remove', block.id)">×</button>
    </div>

    <section v-if="block.type === 'section'" :class="block.data.width === 'full' ? 'w-full' : 'mx-auto w-full max-w-7xl'">
      <BlockRenderer
        v-for="child in block.children || []"
        :key="child.id"
        :block="child"
        :editing="editing"
        :selected="false"
        @select="emit('select', $event)"
        @move-up="emit('moveUp', $event)"
        @move-down="emit('moveDown', $event)"
        @duplicate="emit('duplicate', $event)"
        @remove="emit('remove', $event)"
        @add-child="emit('addChild', $event)"
      />
      <div v-if="editing && !(block.children || []).length" class="rounded-xl border-2 border-dashed border-violet-200 bg-violet-50/60 p-8 text-center text-sm text-violet-600">Sección vacía. Selecciónela y agregue bloques desde la biblioteca.</div>
    </section>

    <section v-else-if="block.type === 'columns'" class="grid" :style="{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`, gap: `${Number(block.data.gap || 24)}px`, alignItems: String(block.data.verticalAlign || 'start') }">
      <div v-for="columnIndex in columns" :key="columnIndex" class="min-w-0 rounded-lg" :class="editing ? 'min-h-24 border border-dashed border-slate-200 p-1' : ''">
        <BlockRenderer
          v-for="child in (block.children || []).filter(item => Number(item.data.column || 1) === columnIndex)"
          :key="child.id"
          :block="child"
          :editing="editing"
          :selected="false"
          @select="emit('select', $event)"
          @move-up="emit('moveUp', $event)"
          @move-down="emit('moveDown', $event)"
          @duplicate="emit('duplicate', $event)"
          @remove="emit('remove', $event)"
          @add-child="emit('addChild', $event)"
        />
        <div v-if="editing && !(block.children || []).some(item => Number(item.data.column || 1) === columnIndex)" class="grid min-h-20 place-items-center text-xs text-slate-400">Columna {{ columnIndex }}</div>
      </div>
    </section>

    <component v-else-if="block.type === 'heading'" :is="`h${Number(block.data.level || 2)}`" class="font-[800] tracking-[-0.035em]" :class="Number(block.data.level || 2) === 1 ? 'text-5xl lg:text-7xl' : Number(block.data.level || 2) === 2 ? 'text-4xl lg:text-5xl' : 'text-2xl lg:text-3xl'">
      {{ block.data.text }}
      <small v-if="block.data.subtitle" class="mt-3 block text-lg font-normal tracking-normal opacity-65">{{ block.data.subtitle }}</small>
    </component>

    <div v-else-if="block.type === 'richText'" class="article-body" v-html="String(block.data.html || '')" />

    <figure v-else-if="block.type === 'image'" class="m-0">
      <a :href="String(block.data.link || '') || undefined">
        <img v-if="block.data.src" :src="String(block.data.src)" :alt="String(block.data.alt || '')" class="w-full" :style="imageStyle">
        <div v-else class="grid h-72 place-items-center rounded-xl bg-slate-100 text-slate-400">Seleccione una imagen</div>
      </a>
      <figcaption v-if="block.data.caption" class="mt-2 text-sm opacity-65">{{ block.data.caption }}</figcaption>
    </figure>

    <div v-else-if="block.type === 'gallery'" class="grid" :style="{ gridTemplateColumns: `repeat(${Math.max(1, Number(block.data.columns || 3))}, minmax(0,1fr))`, gap: `${Number(block.data.gap || 16)}px` }">
      <figure v-for="(item, index) in gallery" :key="`${item.src}-${index}`" class="m-0 overflow-hidden rounded-xl">
        <img :src="item.src" :alt="item.alt || ''" class="aspect-square h-full w-full object-cover transition hover:scale-105">
        <figcaption v-if="item.caption" class="p-2 text-xs opacity-65">{{ item.caption }}</figcaption>
      </figure>
      <div v-if="!gallery.length" class="col-span-full grid h-56 place-items-center rounded-xl bg-slate-100 text-slate-400">Seleccione imágenes para la galería</div>
    </div>

    <MediaPlayer v-else-if="block.type === 'video'" :src="String(block.data.src || '')" :content-type="String(block.data.contentType || 'video/mp4')" :poster="String(block.data.poster || '')" :title="String(block.data.title || '')" />
    <MediaPlayer v-else-if="block.type === 'audio'" :src="String(block.data.src || '')" :content-type="String(block.data.contentType || 'audio/mpeg')" :title="String(block.data.title || '')" />

    <a v-else-if="block.type === 'button'" :href="String(block.data.href || '#')" :target="String(block.data.target || '_self')" :class="['inline-flex items-center justify-center rounded-xl font-bold transition hover:-translate-y-0.5', block.data.variant === 'outline' ? 'border-2 border-current bg-transparent px-6 py-3' : block.data.variant === 'ghost' ? 'px-6 py-3 hover:bg-slate-100' : 'bg-violet-600 px-6 py-3 text-white shadow-lg shadow-violet-200 hover:bg-violet-700', block.data.size === 'large' ? 'text-lg px-8 py-4' : block.data.size === 'small' ? 'text-sm px-4 py-2' : '']">{{ block.data.text }}</a>

    <blockquote v-else-if="block.type === 'quote'" class="border-l-4 border-violet-500 pl-6">
      <p class="text-2xl font-semibold leading-relaxed">“{{ block.data.quote }}”</p>
      <footer class="mt-4 text-sm opacity-70"><strong>{{ block.data.author }}</strong><span v-if="block.data.role"> · {{ block.data.role }}</span></footer>
    </blockquote>

    <div v-else-if="block.type === 'iconBox'" class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="mb-4 grid h-12 w-12 place-items-center rounded-xl bg-violet-100 text-2xl text-violet-700">{{ block.data.icon }}</div>
      <h3 class="text-xl font-bold">{{ block.data.title }}</h3><p class="mt-2 leading-7 opacity-70">{{ block.data.text }}</p>
    </div>

    <div v-else-if="block.type === 'stats'" class="grid gap-5 sm:grid-cols-3">
      <div v-for="(item, index) in stats" :key="index" class="rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm">
        <strong class="block text-4xl font-[850] text-violet-700">{{ item.value }}</strong><span class="mt-2 block opacity-65">{{ item.label }}</span>
      </div>
    </div>

    <CrmFormBlock v-else-if="block.type === 'plugin:trastcrm:form'" :form-key="String(block.data.formKey || 'contacto')" :title="String(block.data.title || '')" :show-title="block.data.showTitle !== false" :style="String(block.data.style || 'card')" :editing="editing" />
    <div v-else-if="String(block.type).startsWith('plugin:')" class="plugin-builder-output" v-html="pluginHtml" />

    <hr v-else-if="block.type === 'divider'" :style="dividerStyle">
    <div v-else-if="block.type === 'spacer'" :style="{ height: `${Number(block.data.height || 64)}px` }" />
    <div v-else-if="block.type === 'html'" v-html="String(block.data.html || '')" />
  </div>
</template>
