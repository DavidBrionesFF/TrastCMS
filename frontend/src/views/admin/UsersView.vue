<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import type { CmsUser, UserRole } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const users = ref<CmsUser[]>([])
const error = ref('')
const notice = ref('')
const loading = ref(true)
const showForm = ref(false)
const search = ref('')
const roleFilter = ref<'ALL' | UserRole>('ALL')
const statusFilter = ref<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
const form = reactive({ id: '', email: '', displayName: '', role: 'AUTHOR' as UserRole, enabled: true, password: '' })

const stats = computed(() => ({
  total: users.value.length,
  active: users.value.filter(user => user.enabled).length,
  admins: users.value.filter(user => user.role === 'ADMIN').length,
  authors: users.value.filter(user => user.role === 'AUTHOR').length
}))

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  return users.value.filter(user => {
    const termOk = !term || `${user.displayName} ${user.email} ${user.role}`.toLowerCase().includes(term)
    const roleOk = roleFilter.value === 'ALL' || user.role === roleFilter.value
    const statusOk = statusFilter.value === 'ALL' || (statusFilter.value === 'ACTIVE' ? user.enabled : !user.enabled)
    return termOk && roleOk && statusOk
  })
})

const roleDescription: Record<UserRole, string> = {
  ADMIN: 'Control completo de contenido, apariencia, usuarios y extensiones.',
  EDITOR: 'Publica y administra contenido, categorías, medios y CRM.',
  AUTHOR: 'Crea y administra únicamente su propio contenido.'
}

async function load() {
  loading.value = true
  try { users.value = await api<CmsUser[]>('/api/admin/users') }
  catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudieron cargar los usuarios' }
  finally { loading.value = false }
}

function openNew() {
  clear()
  showForm.value = true
}

function edit(user: CmsUser) {
  Object.assign(form, {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    role: user.role,
    enabled: user.enabled,
    password: ''
  })
  showForm.value = true
}

function clear() {
  Object.assign(form, { id: '', email: '', displayName: '', role: 'AUTHOR' as UserRole, enabled: true, password: '' })
  showForm.value = false
  error.value = ''
}

async function save() {
  error.value = ''
  notice.value = ''
  try {
    const payload = {
      email: form.email,
      displayName: form.displayName,
      role: form.role,
      enabled: form.enabled,
      password: form.password || null
    }
    await api(form.id ? `/api/admin/users/${form.id}` : '/api/admin/users', {
      method: form.id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    notice.value = form.id ? 'Cuenta actualizada correctamente.' : 'Usuario creado y listo para iniciar sesión.'
    clear()
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar el usuario'
  }
}

async function toggle(user: CmsUser) {
  try {
    await api(`/api/admin/users/${user.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        email: user.email,
        displayName: user.displayName,
        role: user.role,
        enabled: !user.enabled,
        password: null
      })
    })
    notice.value = user.enabled ? 'Cuenta deshabilitada.' : 'Cuenta habilitada.'
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cambiar el estado'
  }
}

async function remove(user: CmsUser) {
  if (!confirm(`¿Eliminar definitivamente la cuenta de ${user.displayName}?`)) return
  try {
    await api(`/api/admin/users/${user.id}`, { method: 'DELETE' })
    notice.value = 'Usuario eliminado.'
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo eliminar el usuario'
  }
}

function initials(user: CmsUser) {
  return user.displayName.split(/\s+/).slice(0, 2).map(part => part.charAt(0)).join('').toUpperCase() || 'U'
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString() : 'Nunca'
}

onMounted(load)
</script>

<template>
  <AdminPageHeader eyebrow="Seguridad y colaboración" title="Usuarios" description="Administre acceso, responsabilidades editoriales y actividad del equipo.">
    <template #actions><button class="button" @click="openNew"><UiIcon name="plus" :size="16"/>Nuevo usuario</button></template>
  </AdminPageHeader>

  <div class="user-stats">
    <article><span><UiIcon name="users" :size="22"/></span><div><strong>{{ stats.total }}</strong><small>Usuarios registrados</small></div></article>
    <article><span class="emerald"><UiIcon name="check" :size="22"/></span><div><strong>{{ stats.active }}</strong><small>Cuentas activas</small></div></article>
    <article><span class="violet"><UiIcon name="lock" :size="22"/></span><div><strong>{{ stats.admins }}</strong><small>Administradores</small></div></article>
    <article><span class="amber"><UiIcon name="post" :size="22"/></span><div><strong>{{ stats.authors }}</strong><small>Autores</small></div></article>
  </div>

  <p v-if="notice" class="form-success mt-5">{{ notice }}</p>
  <p v-if="error && !showForm" class="form-error mt-5">{{ error }}</p>

  <section class="user-directory mt-6">
    <header>
      <div><h2>Directorio del equipo</h2><p>{{ filtered.length }} de {{ users.length }} cuentas visibles</p></div>
      <div class="user-filters"><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Nombre o correo"></div><select v-model="roleFilter" class="control-select"><option value="ALL">Todos los roles</option><option value="ADMIN">Administradores</option><option value="EDITOR">Editores</option><option value="AUTHOR">Autores</option></select><select v-model="statusFilter" class="control-select"><option value="ALL">Todos los estados</option><option value="ACTIVE">Activos</option><option value="INACTIVE">Inactivos</option></select></div>
    </header>

    <div v-if="loading" class="empty">Cargando usuarios…</div>
    <div v-else-if="!filtered.length" class="user-empty"><UiIcon name="users" :size="38"/><h3>No hay resultados</h3><p>Cambie los filtros o cree una cuenta nueva.</p></div>
    <div v-else class="user-table-wrap">
      <table class="user-table">
        <thead><tr><th>Usuario</th><th>Rol y permisos</th><th>Contenido</th><th>Último acceso</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          <tr v-for="user in filtered" :key="user.id">
            <td><div class="user-identity"><img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.displayName"><span v-else>{{ initials(user) }}</span><div><strong>{{ user.displayName }}</strong><small>{{ user.email }}</small></div></div></td>
            <td><span :class="['role-pill', user.role.toLowerCase()]">{{ user.role }}</span><small class="role-copy">{{ roleDescription[user.role] }}</small></td>
            <td><strong>{{ user.contentCount }}</strong><small class="block text-slate-500">elementos creados</small></td>
            <td><span>{{ formatDate(user.lastLoginAt) }}</span><small class="block text-slate-500">Creada {{ new Date(user.createdAt).toLocaleDateString() }}</small></td>
            <td><button :class="['account-status', { active: user.enabled }]" @click="toggle(user)"><i></i>{{ user.enabled ? 'Activa' : 'Inactiva' }}</button></td>
            <td><div class="table-actions"><button title="Editar" @click="edit(user)"><UiIcon name="edit" :size="16"/></button><button class="danger" title="Eliminar" @click="remove(user)"><UiIcon name="trash" :size="16"/></button></div></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="showForm" class="modal-backdrop" @click.self="clear">
      <form class="modal-card user-modal" @submit.prevent="save">
        <header class="modal-header"><div><span class="eyebrow">Gestión de acceso</span><h2>{{ form.id ? 'Editar usuario' : 'Crear usuario' }}</h2><p>{{ form.id ? 'Actualice el rol, estado o credenciales de la cuenta.' : 'Invite a una persona para colaborar en el sitio.' }}</p></div><button type="button" class="icon-button" @click="clear"><UiIcon name="close" :size="18"/></button></header>
        <div class="modal-scroll">
          <p v-if="error" class="form-error">{{ error }}</p>
          <div class="form-stack">
            <label>Nombre visible<input v-model="form.displayName" required maxlength="120" placeholder="Nombre y apellido"></label>
            <label>Correo electrónico<input v-model="form.email" required type="email" maxlength="190" placeholder="persona@empresa.com"></label>
            <label>Rol<select v-model="form.role"><option value="AUTHOR">Autor</option><option value="EDITOR">Editor</option><option value="ADMIN">Administrador</option></select><small>{{ roleDescription[form.role] }}</small></label>
            <label>{{ form.id ? 'Nueva contraseña (opcional)' : 'Contraseña temporal' }}<input v-model="form.password" type="password" :required="!form.id" minlength="12" autocomplete="new-password" placeholder="Mínimo 12 caracteres"><small v-if="form.id">Déjela vacía para conservar la contraseña actual.</small></label>
            <label class="switch-row"><input v-model="form.enabled" type="checkbox"><span><strong>Cuenta habilitada</strong><small>Una cuenta deshabilitada no puede iniciar sesión.</small></span></label>
          </div>
        </div>
        <footer class="modal-footer"><button type="button" class="button secondary" @click="clear">Cancelar</button><button class="button"><UiIcon name="save" :size="16"/>{{ form.id ? 'Guardar cambios' : 'Crear usuario' }}</button></footer>
      </form>
    </div>
  </Teleport>
</template>
