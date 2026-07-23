<script setup lang="ts">
import{onMounted,ref}from'vue';import{api}from'@/services/api';import{useCartStore}from'@/stores/cart';const props=defineProps<{limit?:number;columns?:string;editing?:boolean}>();const products=ref<any[]>([]);const cart=useCartStore();onMounted(async()=>products.value=(await api<any>(`/api/public/store/products?size=${props.limit||8}`)).content);const money=(v:number,c='USD')=>new Intl.NumberFormat('es-HN',{style:'currency',currency:c}).format(v);
</script>

<template>
<div class="store-grid" :style="{gridTemplateColumns:`repeat(${Number(columns||4)},minmax(0,1fr))`}"><article v-for="p in products" :key="p.id" class="product-card"><div class="product-media"></div><div class="p-5"><h3>{{p.name}}</h3><p>{{p.shortDescription}}</p><div class="mt-4 flex justify-between"><strong>{{money(p.price,p.currency)}}</strong><button class="button small" :disabled="editing" @click="cart.add('store',p.id)">Agregar</button></div></div></article></div></template>
