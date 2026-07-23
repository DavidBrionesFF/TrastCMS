import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/', component: PublicLayout, children: [
        { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
        { path: 'post/:slug', name: 'post-public', component: () => import('@/views/public/PostView.vue') },
        { path: 'page/:slug', name: 'page-public', component: () => import('@/views/public/PageView.vue') },
        { path: 'developers', name: 'developers', component: () => import('@/views/public/DeveloperPortalView.vue') },
        { path: 'store', name: 'store-public', component: () => import('@/views/public/StorefrontView.vue') },
        { path: 'cart', name: 'cart-public', component: () => import('@/views/public/CartView.vue') },
        { path: 'pricing', name: 'pricing-public', component: () => import('@/views/public/PricingView.vue') },
        { path: 'saas/claim', name: 'saas-claim', component: () => import('@/views/public/LicenseClaimView.vue') }
      ]
    },
    { path: '/admin/login', name: 'login', component: () => import('@/views/admin/LoginView.vue'), meta: { guest: true } },
    {
      path: '/admin', component: AdminLayout, meta: { requiresAuth: true }, children: [
        { path: '', name: 'dashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'posts', name: 'posts', component: () => import('@/views/admin/PostsView.vue') },
        { path: 'posts/new', name: 'post-new', component: () => import('@/views/admin/PostEditorView.vue'), meta: { contentType: 'POST' } },
        { path: 'posts/:id', name: 'post-edit', component: () => import('@/views/admin/PostEditorView.vue'), meta: { contentType: 'POST' } },
        { path: 'pages', name: 'pages', component: () => import('@/views/admin/PagesView.vue') },
        { path: 'pages/new', name: 'page-new', component: () => import('@/views/admin/PostEditorView.vue'), meta: { contentType: 'PAGE' } },
        { path: 'pages/:id', name: 'page-edit', component: () => import('@/views/admin/PostEditorView.vue'), meta: { contentType: 'PAGE' } },
        { path: 'categories', name: 'categories', component: () => import('@/views/admin/CategoriesView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'media', name: 'media', component: () => import('@/views/admin/MediaView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'themes', name: 'themes', component: () => import('@/views/admin/ThemesView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'plugins', name: 'plugins', component: () => import('@/views/admin/PluginsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'crm', name: 'crm', component: () => import('@plugins/trastcrm/frontend/CrmView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'commerce', name: 'commerce', component: () => import('@plugins/trastpay/frontend/CommerceView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'store', name: 'store', component: () => import('@plugins/traststore/frontend/StoreView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'saas', name: 'saas', component: () => import('@plugins/trastsaas/frontend/SaasView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'extensions/:pluginId/:pageId', name: 'plugin-page', component: () => import('@/views/admin/PluginAdminView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'users', name: 'users', component: () => import('@/views/admin/UsersView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'settings', name: 'settings', component: () => import('@/views/admin/SettingsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'developer', name: 'developer', component: () => import('@/views/admin/DeveloperDocsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'account', name: 'account', component: () => import('@/views/admin/AccountView.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) await auth.load().catch(() => { auth.loaded = true })
  if (to.meta.requiresAuth && !auth.user.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  const roles = to.meta.roles as string[] | undefined
  if (roles && (!auth.user.role || !roles.includes(auth.user.role))) return { name: 'dashboard' }
  if (to.meta.guest && auth.user.authenticated) return { name: 'dashboard' }
})

export default router
