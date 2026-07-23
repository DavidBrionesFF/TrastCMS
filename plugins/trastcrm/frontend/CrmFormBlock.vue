<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { api } from '@/services/api'
import type { CrmPublicForm } from '@/types'
const props=withDefaults(defineProps<{formKey:string;title?:string;showTitle?:boolean;style?:string;editing?:boolean}>(),{title:'',showTitle:true,style:'card',editing:false})
const form=ref<CrmPublicForm|null>(null);const values=reactive<Record<string,string|boolean>>({});const loading=ref(true);const error=ref('');const success=ref('');const sending=ref(false)
async function load(){loading.value=true;error.value='';try{form.value=await api<CrmPublicForm>(`/api/public/plugins/trastcrm/forms/${encodeURIComponent(props.formKey)}`);for(const field of form.value.fields)if(values[field.name]===undefined)values[field.name]=field.type==='checkbox'?false:''}catch(e){form.value=null;error.value=e instanceof Error?e.message:'Formulario no disponible'}finally{loading.value=false}}
async function submit(){if(props.editing)return;sending.value=true;error.value='';success.value='';try{const result=await api<{message:string}>(`/api/public/plugins/trastcrm/forms/${encodeURIComponent(props.formKey)}/submissions`,{method:'POST',body:JSON.stringify({values,sourceUrl:window.location.href})});success.value=result.message;for(const key of Object.keys(values))values[key]=typeof values[key]==='boolean'?false:''}catch(e){error.value=e instanceof Error?e.message:'No se pudo enviar el formulario'}finally{sending.value=false}}
function textValue(name:string){const value=values[name];return typeof value==='boolean'?'':String(value??'')}
function boolValue(name:string){return values[name]===true}
function setText(name:string,event:Event){values[name]=(event.target as HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement).value}
function setBool(name:string,event:Event){values[name]=(event.target as HTMLInputElement).checked}
onMounted(load);watch(()=>props.formKey,load)
</script>
<template>
  <div :class="style==='dark'?'crm-form-dark':style==='minimal'?'':'crm-form-card'">
    <div v-if="loading" class="text-sm opacity-60">Cargando formulario…</div>
    <div v-else-if="error && !form" class="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"><strong>Formulario no disponible</strong><p class="mt-1">{{error}}</p></div>
    <form v-else-if="form" class="crm-public-form" @submit.prevent="submit">
      <header v-if="showTitle"><h2 class="text-2xl font-bold">{{title||form.name}}</h2><p v-if="form.description" class="mt-1 opacity-65">{{form.description}}</p></header>
      <label v-for="field in form.fields" :key="field.name">{{field.label}}<span v-if="field.required" class="text-red-500">*</span>
        <textarea v-if="field.type==='textarea'" :value="textValue(field.name)" :required="field.required" :placeholder="field.placeholder" rows="5" @input="setText(field.name,$event)"/>
        <select v-else-if="field.type==='select'" :value="textValue(field.name)" :required="field.required" @change="setText(field.name,$event)"><option value="">Seleccione una opción</option><option v-for="option in field.options" :key="option" :value="option">{{option}}</option></select>
        <span v-else-if="field.type==='checkbox'" class="flex items-center gap-2"><input :checked="boolValue(field.name)" type="checkbox" @change="setBool(field.name,$event)"><span class="font-normal">{{field.placeholder||'Sí'}}</span></span>
        <input v-else :value="textValue(field.name)" :type="field.type==='phone'?'tel':field.type" :required="field.required" :placeholder="field.placeholder" @input="setText(field.name,$event)">
      </label>
      <p v-if="success" class="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">{{success}}</p><p v-if="error" class="rounded-xl bg-red-50 p-3 text-sm text-red-700">{{error}}</p>
      <button class="button" :disabled="sending||editing">{{editing?'Vista previa del formulario':sending?'Enviando…':String(form.settings.submitLabel||'Enviar')}}</button>
    </form>
  </div>
</template>
