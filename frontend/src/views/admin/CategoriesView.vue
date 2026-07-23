<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import type { Category } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const categories = ref<Category[]>([])
const error = ref('')
const notice = ref('')
const loading = ref(true)
const search = ref('')
const form = reactive({ id: '', name: '', slug: '', description: '' })

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  return categories.value.filter(category => !term || `${category.name} ${category.slug} ${category.description || ''}`.toLowerCase().includes(term))
})

async function load() {
  loading.value = true
  try { categories.value = await api<Category[]>('/api/admin/categories') }
  finally { loading.value = false }
}

function edit(category: Category) {
  Object.assign(form, { id: category.id, name: category.name, slug: category.slug, description: category.description || '' })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function clear() {
  Object.assign(form, { id: '', name: '', slug: '', description: '' })
}

async function save() {
  error.value = ''
  notice.value = ''
  try {
    await api(form.id ? `/api/admin/categories/${form.id}` : '/api/admin/categories', {
      method: form.id ? 'PUT' : 'POST',
      body: JSON.stringify(form)
    })
    notice.value = form.id ? 'Categoría actualizada.' : 'Categoría creada y disponible en el editor.'
    clear()
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar la categoría'
  }
}

async function remove(category: Category) {
  if (!confirm(`¿Eliminar la categoría ${category.name}? Las publicaciones quedarán sin categoría.`)) return
  try {
    await api(`/api/admin/categories/${category.id}`, { method: 'DELETE' })
    notice.value = 'Categoría eliminada.'
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo eliminar la categoría'
  }
}

onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="Organización editorial" title="Categorías" description="Cree una estructura clara para el blog y encuentre el contenido con mayor facilidad.">
    <template #actions><RouterLink :to="{ name: 'post-new' }" class="button"><UiIcon name="plus" :size="16"/>Nueva publicación</RouterLink></template>
  </AdminPageHeader>

  <div class="category-overview">
    <article><span class="category-overview__icon"><UiIcon name="category" :size="21"/></span><div><strong>{{ categories.length }}</strong><small>Categorías creadas</small></div></article>
    <article><span class="category-overview__icon blue"><UiIcon name="post" :size="21"/></span><div><strong>Blog</strong><small>Clasificación disponible al publicar</small></div></article>
    <article><span class="category-overview__icon amber"><UiIcon name="search" :size="21"/></span><div><strong>SEO</strong><small>Slugs y descripciones editables</small></div></article>
  </div>

  <p v-if="notice" class="form-success mt-5">{{ notice }}</p>
  <p v-if="error" class="form-error mt-5">{{ error }}</p>

  <div class="category-workspace">
    <form class="category-editor-card" @submit.prevent="save">
      <header><div><span class="eyebrow">{{ form.id ? 'Editar taxonomía' : 'Nueva taxonomía' }}</span><h2>{{ form.id ? form.name : 'Crear categoría' }}</h2><p>El slug puede generarse automáticamente al dejarlo vacío.</p></div><button v-if="form.id" type="button" class="icon-button" title="Cancelar edición" @click="clear"><UiIcon name="close" :size="17"/></button></header>
      <div class="form-stack">
        <label>Nombre<input v-model="form.name" required maxlength="160" placeholder="Ej. Desarrollo web"></label>
        <label>Slug<input v-model="form.slug" maxlength="180" placeholder="desarrollo-web"><small>Se utiliza en URLs y filtros del blog.</small></label>
        <label>Descripción<textarea v-model="form.description" rows="5" maxlength="500" placeholder="Explique qué contenido pertenece a esta categoría."></textarea><small>{{ form.description.length }}/500 caracteres</small></label>
      </div>
      <footer><button class="button" :disabled="!form.name.trim()"><UiIcon name="save" :size="16"/>{{ form.id ? 'Actualizar categoría' : 'Crear categoría' }}</button><button v-if="form.id" type="button" class="button secondary" @click="clear">Cancelar</button></footer>
    </form>

    <section class="category-list-card">
      <header><div><h2>Biblioteca de categorías</h2><p>Administre los grupos disponibles para autores y editores.</p></div><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Buscar categoría"></div></header>
      <div v-if="loading" class="empty">Cargando categorías…</div>
      <div v-else-if="!filtered.length" class="category-empty"><UiIcon name="category" :size="34"/><h3>No encontramos categorías</h3><p>Cree la primera categoría o cambie el término de búsqueda.</p></div>
      <div v-else class="category-grid">
        <article v-for="category in filtered" :key="category.id" class="category-card">
          <div class="category-card__top"><span class="category-card__icon"><UiIcon name="category" :size="18"/></span><div class="category-card__actions"><button title="Editar" @click="edit(category)"><UiIcon name="edit" :size="16"/></button><button class="danger" title="Eliminar" @click="remove(category)"><UiIcon name="trash" :size="16"/></button></div></div>
          <h3>{{ category.name }}</h3><code>/{{ category.slug }}</code><p>{{ category.description || 'Sin descripción. Agregue contexto para autores y buscadores.' }}</p>
          <footer><span>Actualizada {{ new Date(category.updatedAt).toLocaleDateString() }}</span><button @click="edit(category)">Administrar</button></footer>
        </article>
      </div>
    </section>
  </div>
</template>
