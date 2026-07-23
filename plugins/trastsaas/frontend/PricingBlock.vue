<script setup lang="ts">
import{onMounted,ref}from'vue';import{api}from'@/services/api';import{useCartStore}from'@/stores/cart';const props=defineProps<{productKey?:string;editing?:boolean}>();const plans=ref<any[]>([]);const cart=useCartStore();onMounted(async()=>plans.value=await api(`/api/public/saas/products/${props.productKey||'trast-cloud'}/plans`));const money=(v:number,c='USD')=>new Intl.NumberFormat('es-HN',{style:'currency',currency:c}).format(v);
</script>

<template>
<div class="pricing-grid"><article v-for="p in plans" :key="p.id" :class="['pricing-card',{featured:p.featured}]"><h3>{{p.name}}</h3><strong>{{money(p.price,p.currency)}}</strong><ul><li v-for="(v,k) in p.entitlements" :key="k">✓ {{k}}: {{v}}</li></ul><button class="button full" :disabled="editing" @click="cart.add('saas',p.id)">Elegir plan</button></article></div></template>
