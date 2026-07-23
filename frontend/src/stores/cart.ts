import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/services/api'
export interface CartLine { id:string;providerKey:string;reference:string;type:string;name:string;quantity:number;unitPrice:number;lineTotal:number;currency:string;imageUrl?:string;metadata:Record<string,unknown> }
export interface Cart { token:string;status:string;currency:string;items:CartLine[];subtotal:number;discountTotal:number;taxTotal:number;total:number;expiresAt:string }
export const useCartStore=defineStore('commerce-cart',()=>{
 const cart=ref<Cart|null>(null);const loading=ref(false);const token=ref(localStorage.getItem('trastcms.cart')||'');const count=computed(()=>cart.value?.items.reduce((sum,item)=>sum+item.quantity,0)||0)
 async function ensure(currency='USD'){if(!token.value){const created=await api<Cart>(`/api/public/commerce/carts?currency=${currency}`,{method:'POST'});token.value=created.token;localStorage.setItem('trastcms.cart',created.token);cart.value=created}else await refresh();return cart.value!}
 async function refresh(coupon=''){if(!token.value)return null;loading.value=true;try{cart.value=await api<Cart>(`/api/public/commerce/carts/${token.value}${coupon?`?coupon=${encodeURIComponent(coupon)}`:''}`);return cart.value}catch{token.value='';localStorage.removeItem('trastcms.cart');cart.value=null;return null}finally{loading.value=false}}
 async function add(providerKey:string,reference:string,quantity=1,metadata:Record<string,unknown>={}){await ensure();cart.value=await api<Cart>(`/api/public/commerce/carts/${token.value}/items`,{method:'POST',body:JSON.stringify({providerKey,reference,quantity,metadata})});return cart.value}
 async function update(itemId:string,quantity:number){if(!token.value)return;cart.value=await api<Cart>(`/api/public/commerce/carts/${token.value}/items/${itemId}`,{method:'PUT',body:JSON.stringify({quantity})})}
 function reset(){token.value='';cart.value=null;localStorage.removeItem('trastcms.cart')}
 return{cart,token,count,loading,ensure,refresh,add,update,reset}
})
