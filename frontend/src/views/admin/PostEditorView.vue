<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/services/api'
import type { Category, ContentType, EditorMode, MediaAsset, Post, PostStatus } from '@/types'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import PageBuilder from '@/components/builder/PageBuilder.vue'
import MediaPickerModal from '@/components/media/MediaPickerModal.vue'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id as string | undefined)
const contentType = computed<ContentType>(() => (route.meta.contentType as ContentType) || 'POST')
const isPage = computed(() => contentType.value === 'PAGE')
const listRoute = computed(() => isPage.value ? 'pages' : 'posts')
const categories = ref<Category[]>([])
const saving = ref(false)
const error = ref('')
const success = ref('')
const mediaOpen = ref(false)
const categoryOpen = ref(false)
const categorySaving = ref(false)
const categoryError = ref('')
const categoryForm = reactive({ name: '', slug: '', description: '' })
const advancedOpen = ref(false)
const revisions = ref<Array<{ id: string; revisionNumber: number; createdBy: string; createdAt: string }>>([])

const form = reactive({
  title: '',
  slug: '',
  excerpt: '',
  body: '<p></p>',
  builderData: '',
  customCss: '',
  editorMode: (isPage.value ? 'VISUAL_BUILDER' : 'RICH_TEXT') as EditorMode,
  contentType: contentType.value,
  status: 'DRAFT' as PostStatus,
  featuredImageUrl: '',
  categoryId: '',
  seoTitle: '',
  seoDescription: '',
  template: 'default',
  showInMenu: isPage.value,
  menuOrder: 100,
  pageRole: ''
})

const publicUrl = computed(() => {
  if (!form.slug || form.status !== 'PUBLISHED') return ''
  return isPage.value ? `/page/${form.slug}` : `/post/${form.slug}`
})

const editorialChecks = computed(() => [
  { label: 'Título claro', ok: form.title.trim().length >= 12 },
  { label: 'Extracto informativo', ok: form.excerpt.trim().length >= 70 },
  { label: 'Contenido desarrollado', ok: (form.editorMode === 'RICH_TEXT' ? form.body : form.builderData).length >= 250 },
  { label: 'Imagen destacada', ok: Boolean(form.featuredImageUrl) },
  { label: 'Categoría seleccionada', ok: isPage.value || Boolean(form.categoryId) },
  { label: 'SEO configurado', ok: Boolean(form.seoTitle && form.seoDescription) }
])

const editorialScore = computed(() => Math.round(
  editorialChecks.value.filter(item => item.ok).length / editorialChecks.value.length * 100
))

onMounted(async () => {
  categories.value = await api<Category[]>('/api/admin/categories')
  if (id.value) {
    const post = await api<Post>(`/api/admin/posts/${id.value}`)
    Object.assign(form, {
      title: post.title,
      slug: post.slug,
      excerpt: post.excerpt || '',
      body: post.body || '<p></p>',
      builderData: post.builderData || '',
      customCss: post.customCss || '',
      editorMode: post.editorMode || (post.contentType === 'PAGE' ? 'VISUAL_BUILDER' : 'RICH_TEXT'),
      contentType: post.contentType,
      status: post.status,
      featuredImageUrl: post.featuredImageUrl || '',
      categoryId: post.category?.id || '',
      seoTitle: post.seoTitle || '',
      seoDescription: post.seoDescription || '',
      template: post.template || 'default',
      showInMenu: post.showInMenu,
      menuOrder: post.menuOrder ?? 100,
      pageRole: post.pageRole || ''
    })
    revisions.value = await api<Array<{ id: string; revisionNumber: number; createdBy: string; createdAt: string }>>(`/api/admin/posts/${id.value}/revisions`)
  }
})

async function save(stay = false) {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const payload = {
      ...form,
      contentType: contentType.value,
      categoryId: !isPage.value && form.categoryId ? form.categoryId : null,
      body: form.editorMode === 'VISUAL_BUILDER' ? (form.body || '<p></p>') : form.body,
      builderData: form.editorMode === 'VISUAL_BUILDER' ? form.builderData : null,
      customCss: form.editorMode === 'VISUAL_BUILDER' ? form.customCss : ''
    }
    const saved = await api<Post>(id.value ? `/api/admin/posts/${id.value}` : '/api/admin/posts', {
      method: id.value ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    success.value = 'Contenido guardado correctamente.'
    if (!stay) await router.push({ name: listRoute.value })
    else if (!id.value) await router.replace({ name: isPage.value ? 'page-edit' : 'post-edit', params: { id: saved.id } })
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'No se pudo guardar'
  } finally {
    saving.value = false
  }
}

function chooseFeatured(items: MediaAsset[]) {
  const item = items[0]
  if (item) form.featuredImageUrl = item.publicUrl
  mediaOpen.value = false
}

async function createCategory() {
  categoryError.value = ''
  categorySaving.value = true
  try {
    const created = await api<Category>('/api/admin/categories', {
      method: 'POST',
      body: JSON.stringify(categoryForm)
    })
    categories.value = [...categories.value, created].sort((a, b) => a.name.localeCompare(b.name))
    form.categoryId = created.id
    Object.assign(categoryForm, { name: '', slug: '', description: '' })
    categoryOpen.value = false
    success.value = `Categoría ${created.name} creada y seleccionada.`
  } catch (exception) {
    categoryError.value = exception instanceof Error ? exception.message : 'No se pudo crear la categoría'
  } finally {
    categorySaving.value = false
  }
}
</script>

<template>
  <AdminPageHeader :eyebrow="isPage ? 'CONSTRUCTOR DE PÁGINAS' : 'EDITOR EDITORIAL'" :title="id ? (isPage ? 'Editar página' : 'Editar publicación') : (isPage ? 'Nueva página' : 'Nueva publicación')" :description="isPage ? 'Diseñe la página visualmente o utilice el editor enriquecido.' : 'Prepare contenido profesional con tablas, enlaces, imágenes, video y audio.'">
    <template #actions>
      <a v-if="publicUrl" :href="publicUrl" target="_blank" class="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 font-semibold text-slate-700 hover:bg-slate-50"><UiIcon name="external" :size="17" />Vista pública</a>
      <button type="button" class="rounded-xl border border-violet-200 bg-white px-4 py-2.5 font-semibold text-violet-700 hover:bg-violet-50" :disabled="saving" @click="save(true)">Guardar y continuar</button>
      <button type="button" class="rounded-xl bg-violet-600 px-4 py-2.5 font-semibold text-white shadow-lg shadow-violet-200 hover:bg-violet-700" :disabled="saving" @click="save(false)">{{ saving ? 'Guardando…' : 'Guardar' }}</button>
    </template>
  </AdminPageHeader>

  <p v-if="error" class="form-error mb-4">{{ error }}</p>
  <p v-if="success" class="form-success mb-4">{{ success }}</p>

  <form class="space-y-5" @submit.prevent="save(false)">
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <label class="block"><span class="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">Título</span><input v-model="form.title" required maxlength="250" class="w-full border-0 bg-transparent p-0 text-3xl font-[780] tracking-[-0.035em] text-slate-950 outline-none placeholder:text-slate-300 lg:text-4xl" :placeholder="isPage ? 'Título de la página' : 'Título de la publicación'"></label>
      <div class="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center">
        <label class="flex flex-1 items-center gap-2 text-sm text-slate-500"><span>/{{ isPage ? 'page' : 'post' }}/</span><input v-model="form.slug" maxlength="280" class="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-slate-700" placeholder="se-genera-automaticamente"></label>
        <label class="flex items-center gap-2 text-sm text-slate-500">Estado<select v-model="form.status" class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-700"><option value="DRAFT">Borrador</option><option value="PUBLISHED">Publicada</option><option value="ARCHIVED">Archivada</option></select></label>
      </div>
    </section>

    <section v-if="isPage" class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
      <div class="flex rounded-xl bg-slate-100 p-1"><button type="button" :class="['rounded-lg px-4 py-2 text-sm font-semibold transition', form.editorMode === 'VISUAL_BUILDER' ? 'bg-white text-violet-700 shadow-sm' : 'text-slate-500']" @click="form.editorMode='VISUAL_BUILDER'">Constructor visual</button><button type="button" :class="['rounded-lg px-4 py-2 text-sm font-semibold transition', form.editorMode === 'RICH_TEXT' ? 'bg-white text-violet-700 shadow-sm' : 'text-slate-500']" @click="form.editorMode='RICH_TEXT'">Texto enriquecido</button></div>
      <p class="text-xs text-slate-500">El constructor guarda un documento JSON y genera una página adaptable.</p>
    </section>

    <PageBuilder v-if="form.editorMode === 'VISUAL_BUILDER'" v-model="form.builderData" />
    <RichTextEditor v-else v-model="form.body" :min-height="520" />

    <div class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
      <section class="space-y-5">
        <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 class="text-lg font-bold text-slate-900">Resumen y metadatos</h3>
          <label class="mt-4 block text-sm font-semibold text-slate-700">Extracto<textarea v-model="form.excerpt" rows="4" maxlength="600" class="mt-2 w-full rounded-xl border border-slate-200 p-3 outline-none focus:border-violet-500 focus:ring-4 focus:ring-violet-100" placeholder="Descripción breve para listados, buscadores y redes sociales."></textarea></label>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <button type="button" class="flex w-full items-center justify-between p-5 text-left" @click="advancedOpen=!advancedOpen"><div><h3 class="text-lg font-bold text-slate-900">SEO y opciones avanzadas</h3><p class="mt-1 text-sm text-slate-500">Título SEO, descripción, plantilla y CSS de la página.</p></div><span class="text-xl text-slate-400">{{ advancedOpen ? '−' : '+' }}</span></button>
          <div v-if="advancedOpen" class="grid gap-4 border-t border-slate-200 p-5 md:grid-cols-2">
            <label class="text-sm font-semibold text-slate-700">Título SEO<input v-model="form.seoTitle" maxlength="250" class="mt-2 w-full rounded-xl border border-slate-200 p-3"></label>
            <label class="text-sm font-semibold text-slate-700">Plantilla<select v-model="form.template" class="mt-2 w-full rounded-xl border border-slate-200 bg-white p-3"><option value="default">Predeterminada</option><option value="full-width">Ancho completo</option><option value="landing">Landing page</option><option value="blank">Lienzo en blanco</option></select></label>
            <label class="text-sm font-semibold text-slate-700 md:col-span-2">Descripción SEO<textarea v-model="form.seoDescription" maxlength="500" rows="3" class="mt-2 w-full rounded-xl border border-slate-200 p-3"></textarea></label>
            <label v-if="form.editorMode === 'VISUAL_BUILDER'" class="text-sm font-semibold text-slate-700 md:col-span-2">CSS personalizado<textarea v-model="form.customCss" rows="10" class="mt-2 w-full rounded-xl border border-slate-200 bg-slate-950 p-3 font-mono text-xs text-slate-100" placeholder=".mi-clase { ... }"></textarea></label>
          </div>
        </div>
      </section>

      <aside class="space-y-5">
        <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 class="font-bold text-slate-900">Imagen destacada</h3>
          <button type="button" class="mt-4 w-full overflow-hidden rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 text-slate-400 transition hover:border-violet-300 hover:bg-violet-50" @click="mediaOpen=true">
            <img v-if="form.featuredImageUrl" :src="form.featuredImageUrl" alt="Imagen destacada" class="aspect-video w-full object-cover">
            <span v-else class="grid aspect-video place-items-center"><span><UiIcon name="image" :size="32" class="mx-auto mb-2"/><strong class="text-sm">Seleccionar desde medios</strong></span></span>
          </button>
          <button v-if="form.featuredImageUrl" type="button" class="mt-2 text-sm font-semibold text-red-500" @click="form.featuredImageUrl=''">Quitar imagen</button>
        </section>

        <section v-if="isPage" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 class="font-bold text-slate-900">Navegación</h3>
          <p class="mt-1 text-xs text-slate-500">Controle si la página aparece en el menú principal del tema.</p>
          <label class="switch-row mt-4"><input v-model="form.showInMenu" type="checkbox"><span><strong>Mostrar en el menú</strong><small>Los temas leen las páginas locales publicadas.</small></span></label>
          <label class="mt-4 block text-sm font-semibold text-slate-700">Orden<input v-model.number="form.menuOrder" type="number" min="0" max="10000" class="mt-2 w-full rounded-xl border border-slate-200 p-3"></label>
          <label class="mt-4 block text-sm font-semibold text-slate-700">Rol de página<select v-model="form.pageRole" class="mt-2 w-full rounded-xl border border-slate-200 bg-white p-3"><option value="">Página estándar</option><option value="HOME">Inicio</option><option value="ABOUT">Quiénes somos</option><option value="SERVICES">Servicios</option><option value="BLOG">Blog</option><option value="CONTACT">Contacto</option></select></label>
          <p v-if="form.pageRole" class="mt-3 rounded-xl bg-violet-50 px-3 py-2 text-xs text-violet-700">El tema puede usar este rol para ubicar la página automáticamente.</p>
        </section>

        <section v-if="!isPage" class="editor-sidebar-card">
          <div class="editor-sidebar-card__header"><div><h3>Organización</h3><p>Clasifique la publicación sin salir del editor.</p></div><button type="button" class="icon-button" title="Nueva categoría" @click="categoryOpen=true"><UiIcon name="plus" :size="16"/></button></div>
          <label class="mt-4 block text-sm font-semibold text-slate-700">Categoría<select v-model="form.categoryId" class="mt-2 w-full rounded-xl border border-slate-200 bg-white p-3"><option value="">Sin categoría</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
          <button type="button" class="mt-3 inline-flex items-center gap-2 text-sm font-bold text-violet-700" @click="categoryOpen=true"><UiIcon name="category" :size="16"/>Crear categoría aquí</button>
        </section>

        <section class="editor-sidebar-card">
          <div class="editor-sidebar-card__header"><div><h3>Calidad editorial</h3><p>Lista rápida antes de publicar.</p></div><strong :class="['editor-score', editorialScore >= 80 ? 'good' : editorialScore >= 50 ? 'medium' : 'low']">{{ editorialScore }}%</strong></div>
          <ul class="editor-checklist"><li v-for="item in editorialChecks" :key="item.label" :class="{ done: item.ok }"><span>{{ item.ok ? '✓' : '○' }}</span>{{ item.label }}</li></ul>
        </section>

        <section v-if="revisions.length" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h3 class="font-bold text-slate-900">Revisiones</h3><ul class="mt-3 divide-y divide-slate-100"><li v-for="revision in revisions.slice(0,5)" :key="revision.id" class="py-3 text-sm"><strong class="text-slate-700">Revisión {{ revision.revisionNumber }}</strong><span class="block text-xs text-slate-400">{{ revision.createdBy }} · {{ new Date(revision.createdAt).toLocaleString() }}</span></li></ul></section>
      </aside>
    </div>
  </form>

  <Teleport to="body">
    <div v-if="categoryOpen" class="modal-backdrop" @click.self="categoryOpen=false">
      <form class="modal-card compact-modal" @submit.prevent="createCategory">
        <header class="modal-header"><div><span class="eyebrow">Organización</span><h2>Nueva categoría</h2><p>Se agregará al blog y quedará seleccionada en esta publicación.</p></div><button type="button" class="icon-button" @click="categoryOpen=false"><UiIcon name="close" :size="18"/></button></header>
        <div class="modal-scroll form-stack"><p v-if="categoryError" class="form-error">{{ categoryError }}</p><label>Nombre<input v-model="categoryForm.name" required maxlength="160" placeholder="Ej. Casos de éxito"></label><label>Slug<input v-model="categoryForm.slug" maxlength="180" placeholder="Se genera automáticamente"></label><label>Descripción<textarea v-model="categoryForm.description" rows="4" maxlength="500"></textarea></label></div>
        <footer class="modal-footer"><button type="button" class="button secondary" @click="categoryOpen=false">Cancelar</button><button class="button" :disabled="categorySaving">{{ categorySaving ? 'Creando…' : 'Crear y seleccionar' }}</button></footer>
      </form>
    </div>
  </Teleport>

  <MediaPickerModal :open="mediaOpen" :kinds="['IMAGE']" title="Seleccionar imagen destacada" @close="mediaOpen=false" @select="chooseFeatured" />
</template>
