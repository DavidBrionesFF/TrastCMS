<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { AccountProfile } from '@/types'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'

const auth=useAuthStore();const tab=ref<'profile'|'preferences'|'security'>('profile');const loading=ref(true);const saving=ref(false);const saved=ref('');const error=ref('')
const profile=reactive({displayName:'',firstName:'',lastName:'',phone:'',avatarUrl:'',bio:'',locale:'es-HN',timezone:'America/Tegucigalpa'})
const details=ref<AccountProfile|null>(null);const password=reactive({currentPassword:'',newPassword:'',confirmation:''})
const initials=computed(()=>profile.displayName.split(/\s+/).map(v=>v[0]).join('').slice(0,2).toUpperCase()||'U')
async function load(){loading.value=true;try{details.value=await api<AccountProfile>('/api/admin/account');Object.assign(profile,{displayName:details.value.displayName,firstName:details.value.firstName||'',lastName:details.value.lastName||'',phone:details.value.phone||'',avatarUrl:details.value.avatarUrl||'',bio:details.value.bio||'',locale:details.value.locale||'es-HN',timezone:details.value.timezone||'America/Tegucigalpa'})}catch(e){error.value=e instanceof Error?e.message:'No se pudo cargar la cuenta'}finally{loading.value=false}}
async function saveProfile(){saving.value=true;saved.value='';error.value='';try{details.value=await api<AccountProfile>('/api/admin/account',{method:'PUT',body:JSON.stringify(profile)});await auth.load();saved.value='Perfil actualizado correctamente.'}catch(e){error.value=e instanceof Error?e.message:'No se pudo guardar'}finally{saving.value=false}}
async function changePassword(){saved.value='';error.value='';if(password.newPassword!==password.confirmation){error.value='Las contraseñas no coinciden';return}saving.value=true;try{await api('/api/admin/account/password',{method:'PUT',body:JSON.stringify({currentPassword:password.currentPassword,newPassword:password.newPassword})});Object.assign(password,{currentPassword:'',newPassword:'',confirmation:''});saved.value='Contraseña actualizada.'}catch(e){error.value=e instanceof Error?e.message:'No se pudo cambiar la contraseña'}finally{saving.value=false}}
onMounted(load)

function setTab(value: string) {
  if (value === 'profile' || value === 'preferences' || value === 'security') tab.value = value
}
</script>
<template>
  <AdminPageHeader eyebrow="Cuenta" title="Mi cuenta" description="Administre su perfil, preferencias regionales y seguridad.">
    <template #actions><span v-if="details" class="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700">{{details.enabled?'Cuenta activa':'Deshabilitada'}}</span></template>
  </AdminPageHeader>
  <div v-if="loading" class="panel">Cargando perfil…</div>
  <template v-else>
    <section class="grid gap-5 xl:grid-cols-[320px_minmax(0,1fr)]">
      <aside class="panel h-fit">
        <div class="text-center"><img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="Avatar" class="mx-auto h-24 w-24 rounded-3xl object-cover shadow-lg"><span v-else class="mx-auto grid h-24 w-24 place-items-center rounded-3xl bg-gradient-to-br from-violet-600 to-sky-400 text-3xl font-black text-white shadow-lg">{{initials}}</span><h2 class="mt-4 text-xl font-bold">{{profile.displayName}}</h2><p class="text-sm text-slate-500">{{details?.email}}</p><span class="mt-3 inline-flex rounded-full bg-violet-50 px-3 py-1 text-xs font-bold text-violet-700">{{details?.role}}</span></div>
        <dl class="mt-6 divide-y divide-slate-100 text-sm"><div class="flex justify-between gap-4 py-3"><dt class="text-slate-500">Creada</dt><dd class="font-semibold">{{details?.createdAt?new Date(details.createdAt).toLocaleDateString():''}}</dd></div><div class="flex justify-between gap-4 py-3"><dt class="text-slate-500">Último acceso</dt><dd class="text-right font-semibold">{{details?.lastLoginAt?new Date(details.lastLoginAt).toLocaleString():'Primera sesión'}}</dd></div><div class="flex justify-between gap-4 py-3"><dt class="text-slate-500">Zona horaria</dt><dd class="text-right font-semibold">{{profile.timezone}}</dd></div></dl>
      </aside>
      <div>
        <div class="mb-5 flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm"><button v-for="item in [{id:'profile',label:'Perfil',icon:'account'},{id:'preferences',label:'Preferencias',icon:'settings'},{id:'security',label:'Seguridad',icon:'lock'}]" :key="item.id" type="button" :class="['tab-button',{active:tab===item.id}]" @click="setTab(item.id)"><UiIcon :name="item.icon" :size="17"/>{{item.label}}</button></div>
        <p v-if="saved" class="mb-4 form-success">{{saved}}</p><p v-if="error" class="mb-4 form-error">{{error}}</p>
        <form v-if="tab==='profile'" class="panel form-stack" @submit.prevent="saveProfile"><div><h2 class="text-xl font-bold">Información personal</h2><p class="text-sm text-slate-500">Estos datos identifican su autoría dentro del CMS.</p></div><div class="grid gap-4 md:grid-cols-2"><label>Nombre visible<input v-model="profile.displayName" required maxlength="120"></label><label>URL del avatar<input v-model="profile.avatarUrl" type="url" maxlength="600" placeholder="https://..."></label><label>Nombres<input v-model="profile.firstName" maxlength="80"></label><label>Apellidos<input v-model="profile.lastName" maxlength="80"></label><label>Teléfono<input v-model="profile.phone" maxlength="60"></label><label>Correo<input :value="details?.email" disabled></label><label class="md:col-span-2">Biografía<textarea v-model="profile.bio" maxlength="1000" rows="5" placeholder="Cuente brevemente quién es y qué hace."></textarea></label></div><button class="button w-fit" :disabled="saving">{{saving?'Guardando…':'Guardar perfil'}}</button></form>
        <form v-else-if="tab==='preferences'" class="panel form-stack" @submit.prevent="saveProfile"><div><h2 class="text-xl font-bold">Preferencias regionales</h2><p class="text-sm text-slate-500">Controlan la presentación de fechas y la zona horaria de su usuario.</p></div><div class="grid gap-4 md:grid-cols-2"><label>Idioma<select v-model="profile.locale"><option value="es-HN">Español · Honduras</option><option value="es-419">Español · Latinoamérica</option><option value="en-US">English · United States</option></select></label><label>Zona horaria<select v-model="profile.timezone"><option value="America/Tegucigalpa">Honduras · Tegucigalpa</option><option value="America/Guatemala">Guatemala</option><option value="America/Costa_Rica">Costa Rica</option><option value="America/Panama">Panamá</option><option value="America/New_York">Nueva York</option><option value="Europe/Madrid">Madrid</option><option value="UTC">UTC</option></select></label></div><button class="button w-fit" :disabled="saving">Guardar preferencias</button></form>
        <form v-else class="panel form-stack" @submit.prevent="changePassword"><div><h2 class="text-xl font-bold">Cambiar contraseña</h2><p class="text-sm text-slate-500">Use al menos 12 caracteres y una combinación difícil de adivinar.</p></div><label>Contraseña actual<input v-model="password.currentPassword" type="password" required autocomplete="current-password"></label><label>Nueva contraseña<input v-model="password.newPassword" type="password" required minlength="12" autocomplete="new-password"></label><label>Confirmar contraseña<input v-model="password.confirmation" type="password" required minlength="12" autocomplete="new-password"></label><button class="button w-fit" :disabled="saving">Actualizar contraseña</button></form>
      </div>
    </section>
  </template>
</template>
