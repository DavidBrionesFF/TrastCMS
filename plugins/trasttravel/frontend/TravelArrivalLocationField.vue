<template>
  <div v-if="config.enabled" class="travel-arrival-field">
    <label class="travel-arrival-field__label" for="travel-arrival-location">{{ config.label }}</label>
    <select id="travel-arrival-location" :value="modelValue ?? ''" :required="config.required" class="travel-arrival-field__select" @change="onChange">
      <option value="">{{ config.required ? 'Select a location' : 'No location selected' }}</option>
      <option v-for="location in config.locations" :key="location.id" :value="location.id">{{ location.name }}</option>
    </select>
    <p v-if="selected?.instructions || config.help" class="travel-arrival-field__help">{{ selected?.instructions || config.help }}</p>
    <p v-if="error" class="travel-arrival-field__error" role="alert">{{ error }}</p>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
type Location = { id:number; name:string; type:string; instructions?:string }
type Config = { enabled:boolean; required:boolean; label:string; help:string; locations:Location[] }
const props = defineProps<{ modelValue:number|null; tourId?:string|number|null }>()
const emit = defineEmits<{ 'update:modelValue':[number|null]; 'update:label':[string] }>()
const config=ref<Config>({enabled:false,required:false,label:'Arrival or pickup location',help:'',locations:[]})
const error=ref('')
const selected=computed(()=>config.value.locations.find(x=>x.id===props.modelValue))
async function load(){ try{const q=props.tourId?`?tourId=${encodeURIComponent(String(props.tourId))}`:'';const r=await fetch(`/api/public/travel/customer-experience${q}`);if(!r.ok)throw new Error();config.value=await r.json()}catch{error.value='Arrival locations could not be loaded.'} }
function onChange(e:Event){const value=Number((e.target as HTMLSelectElement).value);const id=Number.isFinite(value)&&value>0?value:null;emit('update:modelValue',id);emit('update:label',config.value.locations.find(x=>x.id===id)?.name??'')}
onMounted(load);watch(()=>props.tourId,load)
</script>
<style scoped>
.travel-arrival-field{display:grid;gap:.45rem;margin:1rem 0}.travel-arrival-field__label{font-weight:650;color:var(--theme-heading,var(--theme-text,#17211c))}.travel-arrival-field__select{width:100%;min-height:44px;border:1px solid var(--theme-border,#d7ddd9);border-radius:var(--theme-radius,.5rem);padding:.65rem .75rem;background:var(--theme-surface,#fff);color:var(--theme-text,#17211c)}.travel-arrival-field__help{margin:0;color:var(--theme-text-muted,#66736c);font-size:.875rem}.travel-arrival-field__error{margin:0;color:#b42318;font-size:.875rem}
</style>
