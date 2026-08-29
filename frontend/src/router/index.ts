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
        { path: 'store/product/:slug', name: 'store-product', component: () => import('@/views/public/ProductView.vue') },
        { path: 'cart', name: 'cart-public', component: () => import('@/views/public/CartView.vue') },
        { path: 'pricing', name: 'pricing-public', component: () => import('@/views/public/PricingView.vue') },
        { path: 'pricing/:productKey/:planKey', name: 'pricing-plan', component: () => import('@/views/public/PlanDetailView.vue') },
        { path: 'saas/claim', name: 'saas-claim', redirect: to => ({ name: 'my-account', query: { ...to.query, section: 'licenses' } }) },
        { path: 'travel', name: 'travel-catalog', component: () => import('@plugins/trasttravel/frontend/TravelCatalogView.vue') },
        { path: 'academy', name: 'academy-catalog', component: () => import('@plugins/trastacademy/frontend/AcademyCatalogView.vue') },
        { path: 'academy/learn/:slug', name: 'academy-learn', component: () => import('@plugins/trastacademy/frontend/AcademyLearnView.vue') },
        { path: 'academy/:slug', name: 'academy-course', component: () => import('@plugins/trastacademy/frontend/AcademyCourseView.vue') },
        { path: 'travel/booking/:code', name: 'travel-booking', component: () => import('@plugins/trasttravel/frontend/TravelBookingView.vue') },
        { path: 'travel/:slug', name: 'travel-detail', component: () => import('@plugins/trasttravel/frontend/TravelDetailView.vue') },
        { path: 'my-account', name: 'my-account', component: () => import('@plugins/trastaccount/frontend/MyAccountView.vue') },
        { path: 'review/:token', name: 'crm-review', component: () => import('@plugins/trastcrm/frontend/CrmReviewView.vue') },
        { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('@/views/public/NotFoundView.vue') }
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
        { path: 'sites', name: 'sites', component: () => import('@/views/admin/SitesView.vue'), meta: { roles: ['ADMIN'], networkAdmin: true } },
        { path: 'crm', name: 'crm', component: () => import('@plugins/trastcrm/frontend/CrmView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'commerce', name: 'commerce', component: () => import('@plugins/trastpay/frontend/CommerceView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'commerce/gateways/cleanpay', name: 'cleanpay-settings', component: () => import('@plugins/trastpay/frontend/CleanPayGatewayAdmin.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'store', name: 'store', component: () => import('@plugins/traststore/frontend/StoreView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'saas', name: 'saas', component: () => import('@plugins/trastsaas/frontend/SaasView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'travel', name: 'travel', component: () => import('@plugins/trasttravel/frontend/TravelView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'account-portal', name: 'account-portal-admin', component: () => import('@plugins/trastaccount/frontend/AccountAdminView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'translation', name: 'translation', component: () => import('@plugins/trasttranslate/frontend/TranslationView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'maintenance', name: 'maintenance', component: () => import('@plugins/trastmaintenance/frontend/MaintenanceView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'comments', name: 'comments', component: () => import('@plugins/trastcomments/frontend/CommentsView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'newsletter', name: 'newsletter', component: () => import('@plugins/trastnewsletter/frontend/NewsletterView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'academy', name: 'academy', component: () => import('@plugins/trastacademy/frontend/AcademyView.vue'), meta: { roles: ['ADMIN', 'EDITOR'] } },
        { path: 'mail', name: 'mail-settings', component: () => import('@/views/admin/MailSettingsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'extensions/:pluginId/:pageId', name: 'plugin-page', component: () => import('@/views/admin/PluginAdminView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'users', name: 'users', component: () => import('@/views/admin/UsersView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'settings', name: 'settings', component: () => import('@/views/admin/SettingsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'developer', name: 'developer', component: () => import('@/views/admin/DeveloperDocsView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'account', name: 'account', component: () => import('@/views/admin/AccountView.vue') }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) await auth.load().catch(() => { auth.loaded = true })
  if (to.meta.requiresAuth && !auth.user.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.path.startsWith('/admin') && auth.user.authenticated && auth.user.role === 'CUSTOMER') {
    return { name: 'my-account' }
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && (!auth.user.role || !roles.includes(auth.user.role))) return { name: 'dashboard' }
  if (to.meta.networkAdmin && !auth.user.networkAdmin) return { name: 'dashboard' }
  if (to.meta.guest && auth.user.authenticated) {
    return auth.user.role === 'CUSTOMER' ? { name: 'my-account' } : { name: 'dashboard' }
  }
})

export default router
