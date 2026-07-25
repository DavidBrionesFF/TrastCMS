import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export type ToastTone = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: number
  title: string
  message?: string
  tone: ToastTone
  timeout: number
}

let sequence = 0

export const useToastStore = defineStore('ui-toast', () => {
  const items = ref<ToastMessage[]>([])
  const visible = computed(() => items.value.slice(-5))

  function push(
    title: string,
    options: Partial<Omit<ToastMessage, 'id' | 'title'>> = {}
  ): number {
    const id = ++sequence
    const toast: ToastMessage = {
      id,
      title,
      message: options.message,
      tone: options.tone || 'info',
      timeout: options.timeout ?? 4200
    }
    items.value.push(toast)
    if (toast.timeout > 0) {
      window.setTimeout(() => remove(id), toast.timeout)
    }
    return id
  }

  function success(title: string, message?: string): number {
    return push(title, { message, tone: 'success' })
  }

  function error(title: string, message?: string): number {
    return push(title, { message, tone: 'error', timeout: 6500 })
  }

  function warning(title: string, message?: string): number {
    return push(title, { message, tone: 'warning', timeout: 5500 })
  }

  function info(title: string, message?: string): number {
    return push(title, { message, tone: 'info' })
  }

  function remove(id: number): void {
    items.value = items.value.filter(item => item.id !== id)
  }

  function clear(): void {
    items.value = []
  }

  return { items, visible, push, success, error, warning, info, remove, clear }
})
