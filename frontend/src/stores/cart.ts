import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api } from '@/services/api'

export interface CartLine {
  id: string
  providerKey: string
  reference: string
  type: string
  name: string
  quantity: number
  unitPrice: number
  lineTotal: number
  currency: string
  imageUrl?: string
  taxable?: boolean
  taxRate?: number
  taxAmount?: number
  metadata: Record<string, unknown>
}

export interface Cart {
  token: string
  status: string
  currency: string
  items: CartLine[]
  subtotal: number
  discountTotal: number
  taxTotal: number
  total: number
  expiresAt: string
}

export const useCartStore = defineStore('commerce-cart', () => {
  const cart = ref<Cart | null>(null)
  const loading = ref(false)
  const mutating = ref(false)
  const token = ref(localStorage.getItem('trastcms.cart') || '')

  const count = computed(() =>
    cart.value?.items.reduce((sum, item) => sum + item.quantity, 0) || 0
  )
  const empty = computed(() => !cart.value?.items.length)

  async function ensure(currency = 'USD'): Promise<Cart> {
    if (!token.value) {
      const created = await api<Cart>(
        `/api/public/commerce/carts?currency=${currency}`,
        { method: 'POST' }
      )
      token.value = created.token
      localStorage.setItem('trastcms.cart', created.token)
      cart.value = created
    } else if (!cart.value) {
      await refresh()
    }
    return cart.value as Cart
  }

  async function refresh(coupon = ''): Promise<Cart | null> {
    if (!token.value) return null
    loading.value = true
    try {
      cart.value = await api<Cart>(
        `/api/public/commerce/carts/${token.value}`
        + (coupon ? `?coupon=${encodeURIComponent(coupon)}` : '')
      )
      return cart.value
    } catch {
      reset()
      return null
    } finally {
      loading.value = false
    }
  }

  async function add(
    providerKey: string,
    reference: string,
    quantity = 1,
    metadata: Record<string, unknown> = {}
  ): Promise<Cart> {
    await ensure()
    mutating.value = true
    try {
      cart.value = await api<Cart>(
        `/api/public/commerce/carts/${token.value}/items`,
        {
          method: 'POST',
          body: JSON.stringify({ providerKey, reference, quantity, metadata })
        }
      )
      return cart.value
    } finally {
      mutating.value = false
    }
  }

  async function update(itemId: string, quantity: number): Promise<Cart | null> {
    if (!token.value) return null
    mutating.value = true
    try {
      cart.value = await api<Cart>(
        `/api/public/commerce/carts/${token.value}/items/${itemId}`,
        {
          method: 'PUT',
          body: JSON.stringify({ quantity: Math.max(0, quantity) })
        }
      )
      return cart.value
    } finally {
      mutating.value = false
    }
  }

  async function remove(itemId: string): Promise<Cart | null> {
    return update(itemId, 0)
  }

  async function clear(): Promise<void> {
    if (!token.value) return
    mutating.value = true
    try {
      cart.value = await api<Cart>(
        `/api/public/commerce/carts/${token.value}/items`,
        { method: 'DELETE' }
      )
    } finally {
      mutating.value = false
    }
  }

  function reset(): void {
    token.value = ''
    cart.value = null
    localStorage.removeItem('trastcms.cart')
  }

  return {
    cart,
    token,
    count,
    empty,
    loading,
    mutating,
    ensure,
    refresh,
    add,
    update,
    remove,
    clear,
    reset
  }
})
