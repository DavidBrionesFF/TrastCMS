<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number; strokeWidth?: number }>(), {
  size: 20,
  strokeWidth: 1.8
})
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <template v-if="name === 'dashboard'"><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></template>
    <template v-else-if="name === 'post'"><path d="M4 4h16v16H4z"/><path d="M8 8h8M8 12h8M8 16h5"/></template>
    <template v-else-if="name === 'page'"><path d="M6 2h9l5 5v15H6z"/><path d="M14 2v6h6M9 13h6M9 17h6"/></template>
    <template v-else-if="name === 'category'"><path d="M3 7h7l2 2h9v10H3z"/><path d="M3 7V5h7l2 2"/></template>
    <template v-else-if="name === 'media'"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m3 17 5-5 4 4 3-3 6 6"/></template>
    <template v-else-if="name === 'theme'"><circle cx="12" cy="12" r="9"/><path d="M12 3v18M3 12h18"/></template>
    <template v-else-if="name === 'plugin'"><path d="M8 3h3v4h2V3h3v4h2v4h-2v3a4 4 0 0 1-8 0v-3H6V7h2z"/><path d="M12 18v3"/></template>
    <template v-else-if="name === 'users'"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></template>
    <template v-else-if="name === 'settings'"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1.1V21h-4v-.09A1.7 1.7 0 0 0 8.6 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1.1-.4H3v-4h.09A1.7 1.7 0 0 0 4.6 8.6a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1.1V3h4v.09A1.7 1.7 0 0 0 15.4 4.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9c.1.37.3.7.6 1 .3.27.68.4 1.1.4H21v4h-.09a1.7 1.7 0 0 0-1.51.6z"/></template>
    <template v-else-if="name === 'account'"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></template>
    <template v-else-if="name === 'logout'"><path d="M10 17l5-5-5-5M15 12H3M21 3v18"/></template>
    <template v-else-if="name === 'external'"><path d="M14 3h7v7M10 14 21 3M21 14v6H4V3h6"/></template>
    <template v-else-if="name === 'search'"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></template>
    <template v-else-if="name === 'upload'"><path d="M12 16V4M7 9l5-5 5 5"/><path d="M4 20h16"/></template>
    <template v-else-if="name === 'grid'"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></template>
    <template v-else-if="name === 'list'"><path d="M8 6h13M8 12h13M8 18h13"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/></template>
    <template v-else-if="name === 'trash'"><path d="M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6"/></template>
    <template v-else-if="name === 'copy'"><rect x="9" y="9" width="12" height="12" rx="2"/><rect x="3" y="3" width="12" height="12" rx="2"/></template>
    <template v-else-if="name === 'edit'"><path d="m14 4 6 6L8 22H2v-6zM13 5l6 6"/></template>
    <template v-else-if="name === 'more'"><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></template>
    <template v-else-if="name === 'check'"><path d="m5 12 4 4L19 6"/></template>
    <template v-else-if="name === 'close'"><path d="m6 6 12 12M18 6 6 18"/></template>
    <template v-else-if="name === 'plus'"><path d="M12 5v14M5 12h14"/></template>
    <template v-else-if="name === 'play'"><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4z"/></template>
    <template v-else-if="name === 'image'"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="2"/><path d="m3 17 5-5 4 4 3-3 6 6"/></template>
    <template v-else-if="name === 'video'"><rect x="3" y="5" width="14" height="14" rx="2"/><path d="m17 10 4-3v10l-4-3z"/></template>
    <template v-else-if="name === 'audio'"><path d="M9 18V5l10-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="16" cy="16" r="3"/></template>
    <template v-else-if="name === 'document'"><path d="M6 2h9l5 5v15H6z"/><path d="M14 2v6h6"/></template>
    <template v-else-if="name === 'desktop'"><rect x="3" y="4" width="18" height="13" rx="2"/><path d="M8 21h8M12 17v4"/></template>
    <template v-else-if="name === 'tablet'"><rect x="6" y="2" width="12" height="20" rx="2"/><path d="M11 18h2"/></template>
    <template v-else-if="name === 'mobile'"><rect x="8" y="2" width="8" height="20" rx="2"/><path d="M11 18h2"/></template>
    <template v-else-if="name === 'layers'"><path d="m12 2 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 17l9 5 9-5"/></template>
    <template v-else-if="name === 'code'"><path d="m8 9-4 3 4 3M16 9l4 3-4 3M14 4l-4 16"/></template>
    <template v-else-if="name === 'package'"><path d="m21 8-9 5-9-5 9-5z"/><path d="m3 8 9 5 9-5v8l-9 5-9-5z"/><path d="M12 13v8"/></template>
    <template v-else-if="name === 'cart'"><circle cx="9" cy="20" r="1"/><circle cx="19" cy="20" r="1"/><path d="M3 4h2l2 11h12l2-8H6M9 9h8"/></template>
    <template v-else-if="name === 'store'"><path d="M4 10v11h16V10M3 4h18l-2 6H5z"/><path d="M8 21v-7h8v7"/></template>
    <template v-else-if="name === 'key'"><circle cx="8" cy="15" r="5"/><path d="m12 12 8-8M17 4l3 3M14 7l3 3"/></template>
    <template v-else-if="name === 'payment'"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></template>
    <template v-else-if="name === 'pricing'"><path d="M20 13 11 22l-9-9V4h9z"/><circle cx="7" cy="9" r="1"/></template>
    <template v-else-if="name === 'crm'"><circle cx="7" cy="8" r="3"/><path d="M2 20a5 5 0 0 1 10 0"/><path d="M15 4h6v5h-6zM15 13h6v7h-6z"/><path d="M12 8h3M12 17h3"/></template>
    <template v-else-if="name === 'company'"><path d="M3 21h18M5 21V5h9v16M14 9h5v12"/><path d="M8 8h3M8 12h3M8 16h3M16 12h1M16 16h1"/></template>
    <template v-else-if="name === 'pipeline'"><path d="M4 5h16M7 5v5h10V5M10 10v5h4v-5M12 15v5"/><circle cx="4" cy="5" r="1"/><circle cx="20" cy="5" r="1"/><circle cx="12" cy="20" r="1"/></template>
    <template v-else-if="name === 'calendar'"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></template>
    <template v-else-if="name === 'form'"><path d="M5 3h14v18H5z"/><path d="M8 7h8M8 11h8M8 15h5"/><circle cx="8" cy="18" r=".5"/></template>
    <template v-else-if="name === 'inbox'"><path d="M4 4h16l2 11v5H2v-5z"/><path d="M2 15h6l2 3h4l2-3h6"/></template>
    <template v-else-if="name === 'lock'"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></template>
    <template v-else-if="name === 'save'"><path d="M4 3h14l2 2v16H4z"/><path d="M8 3v6h8V3M8 21v-7h8v7"/></template>
    <template v-else-if="name === 'menu'"><path d="M4 6h16M4 12h16M4 18h16"/></template>
    <template v-else-if="name === 'footer'"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 15h18M8 18h8"/></template>
    <template v-else-if="name === 'refresh'"><path d="M20 6v5h-5M4 18v-5h5"/><path d="M18 9a7 7 0 0 0-12-2L4 9M6 15a7 7 0 0 0 12 2l2-2"/></template>
    <template v-else-if="name === 'info'"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></template>
    <template v-else-if="name === 'drag'"><circle cx="9" cy="6" r="1"/><circle cx="15" cy="6" r="1"/><circle cx="9" cy="12" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="9" cy="18" r="1"/><circle cx="15" cy="18" r="1"/></template>
    <template v-else><circle cx="12" cy="12" r="9"/></template>
  </svg>
</template>
