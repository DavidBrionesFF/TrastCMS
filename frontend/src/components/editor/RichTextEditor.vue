<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { Node, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Placeholder from '@tiptap/extension-placeholder'
import Highlight from '@tiptap/extension-highlight'
import Typography from '@tiptap/extension-typography'
import CharacterCount from '@tiptap/extension-character-count'
import { Table, TableRow, TableHeader, TableCell } from '@tiptap/extension-table'
import MediaPickerModal from '@/components/media/MediaPickerModal.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import type { MediaAsset } from '@/types'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  minHeight?: number
}>(), {
  placeholder: 'Escriba contenido profesional con texto, tablas, enlaces y medios…',
  minHeight: 420
})
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const pickerOpen = ref(false)
const pickerKinds = ref<Array<'IMAGE' | 'VIDEO' | 'AUDIO'>>(['IMAGE'])

const MediaNode = Node.create({
  name: 'trastMedia',
  group: 'block',
  atom: true,
  draggable: true,
  addAttributes() {
    return {
      src: { default: null },
      contentType: { default: null },
      kind: { default: 'VIDEO' },
      title: { default: null },
      poster: { default: null }
    }
  },
  parseHTML() {
    return [
      { tag: 'video[data-trast-media]' },
      { tag: 'audio[data-trast-media]' }
    ]
  },
  renderHTML({ HTMLAttributes }) {
    const tag = HTMLAttributes.kind === 'AUDIO' ? 'audio' : 'video'
    const attributes = mergeAttributes(HTMLAttributes, {
      'data-trast-media': 'true',
      controls: 'true',
      playsinline: tag === 'video' ? 'true' : undefined,
      preload: 'metadata',
      class: 'trast-media-embed'
    })
    return [tag, attributes]
  }
})

const editor = useEditor({
  content: props.modelValue || '',
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4] },
      codeBlock: { HTMLAttributes: { class: 'code-block' } },
      link: false,
      underline: false
    }),
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Link.configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      HTMLAttributes: { rel: 'noopener noreferrer' }
    }),
    Image.configure({
      inline: false,
      allowBase64: false,
      resize: {
        enabled: true,
        directions: ['top', 'bottom', 'left', 'right'],
        minWidth: 80,
        minHeight: 60,
        alwaysPreserveAspectRatio: true
      },
      HTMLAttributes: { loading: 'lazy', class: 'content-image' }
    }),
    MediaNode,
    Placeholder.configure({ placeholder: props.placeholder }),
    Highlight.configure({ multicolor: true }),
    Typography,
    CharacterCount.configure({ limit: 250_000 }),
    Table.configure({ resizable: true }),
    TableRow,
    TableHeader,
    TableCell
  ],
  onUpdate: ({ editor }) => emit('update:modelValue', editor.getHTML())
})

watch(() => props.modelValue, value => {
  if (!editor.value) return
  if (editor.value.getHTML() !== value) editor.value.commands.setContent(value || '', { emitUpdate: false })
})

function setLink() {
  if (!editor.value) return
  const previous = editor.value.getAttributes('link').href as string | undefined
  const href = window.prompt('URL del enlace', previous || 'https://')
  if (href === null) return
  if (!href.trim()) {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: href.trim(), target: '_blank' }).run()
}

function openMedia(kinds: Array<'IMAGE' | 'VIDEO' | 'AUDIO'>) {
  pickerKinds.value = kinds
  pickerOpen.value = true
}

function insertMedia(items: MediaAsset[]) {
  const item = items[0]
  if (!item || !editor.value) return
  if (item.kind === 'IMAGE') {
    editor.value.chain().focus().setImage({
      src: item.publicUrl,
      alt: item.altText || item.title || item.originalFilename,
      title: item.title || item.originalFilename
    }).run()
  } else {
    editor.value.chain().focus().insertContent({
      type: 'trastMedia',
      attrs: {
        src: item.publicUrl,
        contentType: item.contentType,
        kind: item.kind,
        title: item.title || item.originalFilename
      }
    }).run()
  }
  pickerOpen.value = false
}

function addTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

function addHorizontalRule() {
  editor.value?.chain().focus().setHorizontalRule().run()
}

function changeBlockType(event: Event) {
  if (!editor.value) return
  const value = (event.target as HTMLSelectElement).value
  if (value === 'p') editor.value.chain().focus().setParagraph().run()
  else editor.value.chain().focus().toggleHeading({ level: Number(value.slice(1)) as 1 | 2 | 3 }).run()
}

onBeforeUnmount(() => editor.value?.destroy())
</script>

<template>
  <div class="rich-editor">
    <div v-if="editor" class="rich-editor__toolbar" role="toolbar" aria-label="Formato del contenido">
      <button type="button" title="Deshacer" @click="editor.chain().focus().undo().run()">↶</button>
      <button type="button" title="Rehacer" @click="editor.chain().focus().redo().run()">↷</button>
      <span class="mx-1 h-8 w-px bg-slate-300" />
      <select class="h-9 w-auto rounded-lg border border-slate-200 bg-white px-2 text-sm" :value="editor.isActive('heading', { level: 1 }) ? 'h1' : editor.isActive('heading', { level: 2 }) ? 'h2' : editor.isActive('heading', { level: 3 }) ? 'h3' : 'p'" @change="changeBlockType">
        <option value="p">Párrafo</option><option value="h1">Título 1</option><option value="h2">Título 2</option><option value="h3">Título 3</option>
      </select>
      <button type="button" :class="{ 'is-active': editor.isActive('bold') }" title="Negrita" @click="editor.chain().focus().toggleBold().run()"><strong>B</strong></button>
      <button type="button" :class="{ 'is-active': editor.isActive('italic') }" title="Cursiva" @click="editor.chain().focus().toggleItalic().run()"><em>I</em></button>
      <button type="button" :class="{ 'is-active': editor.isActive('underline') }" title="Subrayado" @click="editor.chain().focus().toggleUnderline().run()"><u>U</u></button>
      <button type="button" :class="{ 'is-active': editor.isActive('strike') }" title="Tachado" @click="editor.chain().focus().toggleStrike().run()"><s>S</s></button>
      <button type="button" :class="{ 'is-active': editor.isActive('highlight') }" title="Resaltado" @click="editor.chain().focus().toggleHighlight({ color: '#fef08a' }).run()">H</button>
      <span class="mx-1 h-8 w-px bg-slate-300" />
      <button type="button" :class="{ 'is-active': editor.isActive({ textAlign: 'left' }) }" title="Alinear a la izquierda" @click="editor.chain().focus().setTextAlign('left').run()">≡</button>
      <button type="button" :class="{ 'is-active': editor.isActive({ textAlign: 'center' }) }" title="Centrar" @click="editor.chain().focus().setTextAlign('center').run()">≣</button>
      <button type="button" :class="{ 'is-active': editor.isActive({ textAlign: 'right' }) }" title="Alinear a la derecha" @click="editor.chain().focus().setTextAlign('right').run()">☷</button>
      <button type="button" :class="{ 'is-active': editor.isActive('bulletList') }" title="Lista" @click="editor.chain().focus().toggleBulletList().run()">•</button>
      <button type="button" :class="{ 'is-active': editor.isActive('orderedList') }" title="Lista numerada" @click="editor.chain().focus().toggleOrderedList().run()">1.</button>
      <button type="button" :class="{ 'is-active': editor.isActive('blockquote') }" title="Cita" @click="editor.chain().focus().toggleBlockquote().run()">❝</button>
      <button type="button" :class="{ 'is-active': editor.isActive('codeBlock') }" title="Código" @click="editor.chain().focus().toggleCodeBlock().run()"><UiIcon name="code" :size="16" /></button>
      <button type="button" :class="{ 'is-active': editor.isActive('link') }" title="Enlace" @click="setLink">🔗</button>
      <span class="mx-1 h-8 w-px bg-slate-300" />
      <button type="button" title="Insertar imagen" @click="openMedia(['IMAGE'])"><UiIcon name="image" :size="17" /></button>
      <button type="button" title="Insertar video" @click="openMedia(['VIDEO'])"><UiIcon name="video" :size="17" /></button>
      <button type="button" title="Insertar audio" @click="openMedia(['AUDIO'])"><UiIcon name="audio" :size="17" /></button>
      <button type="button" title="Insertar tabla" @click="addTable">▦</button>
      <button type="button" title="Línea horizontal" @click="addHorizontalRule">—</button>
      <template v-if="editor.isActive('table')">
        <button type="button" title="Agregar columna" @click="editor.chain().focus().addColumnAfter().run()">+Col</button>
        <button type="button" title="Eliminar columna" @click="editor.chain().focus().deleteColumn().run()">−Col</button>
        <button type="button" title="Agregar fila" @click="editor.chain().focus().addRowAfter().run()">+Fila</button>
        <button type="button" title="Eliminar fila" @click="editor.chain().focus().deleteRow().run()">−Fila</button>
        <button type="button" title="Combinar o separar celdas" @click="editor.chain().focus().mergeOrSplit().run()">⇄</button>
        <button type="button" title="Alternar encabezado" @click="editor.chain().focus().toggleHeaderRow().run()">Enc.</button>
        <button type="button" title="Eliminar tabla" @click="editor.chain().focus().deleteTable().run()">×Tabla</button>
      </template>
    </div>
    <EditorContent v-if="editor" :editor="editor" class="rich-editor__content" :style="{ '--editor-min-height': `${minHeight}px` }" />
    <footer v-if="editor" class="rich-editor__footer">
      <span>{{ editor.storage.characterCount.characters() }} caracteres</span>
      <span>{{ editor.storage.characterCount.words() }} palabras</span>
    </footer>
  </div>

  <MediaPickerModal :open="pickerOpen" :kinds="pickerKinds" title="Insertar medio en el contenido" @close="pickerOpen=false" @select="insertMedia" />
</template>

<style scoped>
.rich-editor__content :deep(.ProseMirror) { min-height: var(--editor-min-height); }
.rich-editor__content :deep(.trast-media-embed) { display:block;width:100%;max-height:560px;margin:1.5rem 0;border-radius:.9rem;background:#0f172a; }
</style>
