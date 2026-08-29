const STORAGE_KEY = 'trastcms.admin.site'
let selectedSite: string | null = null

function availableStorage(): Storage | null {
  if (typeof window === 'undefined') return null
  try { return window.localStorage } catch { return null }
}

export function getSelectedSite(): string | null {
  if (selectedSite) return selectedSite
  const value = availableStorage()?.getItem(STORAGE_KEY)?.trim() || ''
  selectedSite = value || null
  return selectedSite
}

export function setSelectedSite(siteKey: string | null): void {
  const normalized = siteKey?.trim() || null
  selectedSite = normalized
  const storage = availableStorage()
  if (!storage) return
  if (normalized) storage.setItem(STORAGE_KEY, normalized)
  else storage.removeItem(STORAGE_KEY)
}

export function shouldSendSiteHeader(url: string): boolean {
  return url.startsWith('/api/admin') || url === '/api/auth/me'
}
