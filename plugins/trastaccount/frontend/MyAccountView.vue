<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '@/services/api'
import UiIcon from '@/components/common/UiIcon.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const cart = useCartStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()
const mode = ref<'login' | 'register'>(route.query.mode === 'register' ? 'register' : 'login')
const loading = ref(false)
const error = ref('')
const overview = ref<Record<string, any> | null>(null)
const active = ref('profile')
const selected = ref<Record<string, any> | null>(null)

const credentials = reactive({ email: '', password: '' })
const registration = reactive({ displayName: '', email: '', phone: '', password: '' })
const profile = reactive({
  displayName: '', firstName: '', lastName: '', phone: '', bio: '', avatarUrl: '',
  locale: 'es-HN', timezone: 'America/Tegucigalpa'
})
const password = reactive({ currentPassword: '', newPassword: '' })
const sections = computed(() => overview.value?.sections || [])
const activeSection = computed(() => sections.value.find((item: any) => item.key === active.value))
const redirectTarget = computed(() => String(route.query.redirect || ''))

onMounted(async () => {
  if (!auth.loaded) await auth.load().catch(() => undefined)
  if (auth.user.authenticated) await load()
})

async function completeAuthentication(): Promise<void> {
  if (redirectTarget.value.startsWith('/')) {
    await router.push(redirectTarget.value)
    return
  }
  await load()
}

async function login(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    await auth.login(credentials.email, credentials.password)
    await completeAuthentication()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo iniciar sesión'
  } finally { loading.value = false }
}

async function register(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    await api('/api/public/account/register', { method: 'POST', body: JSON.stringify(registration) })
    await auth.login(registration.email, registration.password)
    await completeAuthentication()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo crear la cuenta'
  } finally { loading.value = false }
}

async function load(): Promise<void> {
  overview.value = await api('/api/account')
  Object.assign(profile, overview.value?.profile || {})
  const requested = String(route.query.section || '')
  active.value = sections.value.some((item: any) => item.key === requested) ? requested : 'profile'
  selected.value = null
  const requestedBooking = String(route.query.booking || '')
  const requestedOrder = String(route.query.order || '')
  if (activeSection.value) {
    selected.value = activeSection.value.items?.find((item: any) =>
      (requestedBooking && item.bookingCode === requestedBooking) ||
      (requestedOrder && [item.orderNumber, item.reference, item.orderId].includes(requestedOrder))
    ) || null
  }
}

function setActive(value: string): void {
  active.value = value; selected.value = null
  void router.replace({ query: value === 'profile' ? {} : { section: value } })
}

async function saveProfile(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    if (overview.value) overview.value.profile = await api('/api/account/profile', { method: 'PUT', body: JSON.stringify(profile) })
    toast.success('Perfil actualizado')
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo guardar el perfil' }
  finally { loading.value = false }
}

async function changePassword(): Promise<void> {
  loading.value = true; error.value = ''
  try {
    await api('/api/account/password', { method: 'POST', body: JSON.stringify(password) })
    password.currentPassword = ''; password.newPassword = ''; toast.success('Contraseña actualizada')
  } catch (exception) { error.value = exception instanceof Error ? exception.message : 'No se pudo cambiar la contraseña' }
  finally { loading.value = false }
}

async function logout(): Promise<void> {
  await auth.logout(); overview.value = null; active.value = 'profile'; selected.value = null
}

async function payTravel(item: Record<string, any>): Promise<void> {
  try {
    await cart.add('travel', item.id, 1, { paymentKind: 'BALANCE' })
    await router.push('/cart')
  } catch (caught: unknown) {
    toast.error('No se pudo agregar el saldo', caught instanceof ApiError ? caught.message : undefined)
  }
}

async function copyLicense(value: string): Promise<void> {
  await navigator.clipboard.writeText(value)
  toast.success('Licencia copiada')
}

function icon(section: any): string { return section?.icon || 'plugin' }
function statusClass(value = ''): string {
  return ['PAID', 'ACTIVE', 'CONFIRMED', 'COMPLETED', 'FULFILLED', 'TRIALING'].includes(value)
    ? 'ok' : ['PENDING', 'PENDING_PAYMENT', 'PARTIALLY_PAID', 'PAYMENT_PROCESSING'].includes(value) ? 'pending' : 'muted'
}
function money(value: any, currency = 'USD'): string { return new Intl.NumberFormat('es-US', { style: 'currency', currency }).format(Number(value || 0)) }
function date(value: any): string { return value ? new Intl.DateTimeFormat('es-HN', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(value)) : 'No definido' }
function labelFor(key: string): string {
  const labels: Record<string, string> = { total: 'Total', amount: 'Monto', balanceDue: 'Saldo pendiente', amountPaid: 'Pagado' }
  return labels[key] || key
}
</script>

<template>
  <main class="account-page">
    <section v-if="!auth.user.authenticated" class="auth-wrap">
      <div class="auth-copy">
        <span class="kicker">PORTAL DEL CLIENTE</span><h1>Todo lo suyo, en un solo lugar.</h1>
        <p>Consulte viajes, pagos, pedidos, facturas, suscripciones y licencias sin entrar al panel administrativo.</p>
        <ul><li>Reservaciones y viajeros</li><li>Pagos y saldos pendientes</li><li>Pedidos y documentos</li><li>Licencias y activaciones</li></ul>
      </div>
      <div class="auth-card">
        <div v-if="redirectTarget" class="auth-notice">Su compra necesita una cuenta. Al terminar regresará automáticamente al checkout.</div>
        <div class="switch"><button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">Iniciar sesión</button><button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">Crear cuenta</button></div>
        <form v-if="mode === 'login'" @submit.prevent="login"><label>Correo<input v-model="credentials.email" type="email" required autocomplete="email"></label><label>Contraseña<input v-model="credentials.password" type="password" required autocomplete="current-password"></label><button :disabled="loading">{{ loading ? 'Ingresando…' : 'Entrar a Mi Cuenta' }}</button></form>
        <form v-else @submit.prevent="register"><label>Nombre completo<input v-model="registration.displayName" required></label><label>Correo<input v-model="registration.email" type="email" required></label><label>Teléfono<input v-model="registration.phone"></label><label>Contraseña<input v-model="registration.password" type="password" minlength="12" required><small>Mínimo 12 caracteres.</small></label><button :disabled="loading">{{ loading ? 'Creando…' : 'Crear Mi Cuenta' }}</button></form>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </section>

    <div v-else-if="overview" class="portal-shell">
      <aside class="account-nav">
        <div class="user"><span>{{ overview.profile.displayName?.charAt(0) || 'U' }}</span><div><strong>{{ overview.profile.displayName }}</strong><small>{{ overview.profile.email }}</small></div></div>
        <nav>
          <button :class="{ active: active === 'profile' }" @click="setActive('profile')"><UiIcon name="account" :size="19"/><span>Mi información</span></button>
          <button v-for="section in sections" :key="section.key" :class="{ active: active === section.key }" @click="setActive(section.key)"><UiIcon :name="icon(section)" :size="19"/><span>{{ section.label }}</span><i :class="statusClass(section.status)">{{ section.items?.length || 0 }}</i></button>
        </nav>
        <button class="logout" @click="logout"><UiIcon name="logout" :size="18"/>Cerrar sesión</button>
      </aside>

      <section class="account-content">
        <header><div><span class="kicker">MI CUENTA</span><h1>{{ active === 'profile' ? 'Mi información' : activeSection?.label }}</h1><p>{{ active === 'profile' ? 'Mantenga actualizados sus datos de contacto.' : 'Toda la información permanece dentro de su cuenta.' }}</p></div><RouterLink to="/" class="home-link"><UiIcon name="external" :size="17"/>Volver al sitio</RouterLink></header>
        <p v-if="error" class="error account-error">{{ error }}</p>

        <template v-if="active === 'profile'">
          <div class="profile-grid">
            <form class="panel-card" @submit.prevent="saveProfile"><h2>Perfil</h2><label>Nombre<input v-model="profile.displayName" required></label><label>Teléfono<input v-model="profile.phone"></label><div class="grid-two"><label>Nombres<input v-model="profile.firstName"></label><label>Apellidos<input v-model="profile.lastName"></label></div><label>Zona horaria<input v-model="profile.timezone"></label><label>Foto de perfil<input v-model="profile.avatarUrl" type="url"></label><label>Biografía<textarea v-model="profile.bio" rows="4"></textarea></label><button :disabled="loading"><UiIcon name="save" :size="18"/>Guardar perfil</button></form>
            <form class="panel-card" @submit.prevent="changePassword"><h2>Seguridad</h2><label>Contraseña actual<input v-model="password.currentPassword" type="password" required></label><label>Nueva contraseña<input v-model="password.newPassword" type="password" minlength="12" required></label><button :disabled="loading"><UiIcon name="lock" :size="18"/>Cambiar contraseña</button><div class="integration-note"><b>Panel seguro</b><p>Esta cuenta no concede acceso al administrador. Cada plugin aporta únicamente la información que le corresponde.</p></div></form>
          </div>
        </template>

        <template v-else>
          <article v-if="selected" class="detail-panel">
            <button class="detail-close" @click="selected = null">×</button>
            <div class="detail-heading"><img v-if="selected.imageUrl" :src="selected.imageUrl" alt=""><div><span :class="['badge', statusClass(selected.status || selected.paymentStatus)]">{{ selected.status || selected.paymentStatus }}</span><h2>{{ selected.title }}</h2><p>{{ selected.description }}</p></div></div>
            <div v-if="active === 'travel'" class="detail-grid">
              <div><span>Fecha y hora</span><b>{{ date(selected.startsAt) }}</b></div><div><span>Destino</span><b>{{ selected.destination || 'Por confirmar' }}</b></div><div><span>Viajeros</span><b>{{ selected.travellerCount }} ({{ selected.adults }} adultos, {{ selected.children }} niños)</b></div><div><span>Punto de encuentro</span><b>{{ selected.meetingPoint || 'Por confirmar' }}</b></div><div><span>Total</span><b>{{ money(selected.total, selected.currency) }}</b></div><div><span>Saldo pendiente</span><b>{{ money(selected.balanceDue, selected.currency) }}</b></div>
            </div>
            <div v-else-if="active === 'academy'" class="detail-grid">
              <div><span>Progreso</span><b>{{ selected.progress || 0 }}%</b></div><div><span>Lecciones completadas</span><b>{{ selected.completedLessons || 0 }} de {{ selected.totalLessons || 0 }}</b></div><div><span>Evaluaciones aprobadas</span><b>{{ selected.completedQuizzes || 0 }} de {{ selected.totalQuizzes || 0 }}</b></div><div><span>Estado</span><b>{{ selected.status }}</b></div>
            </div>
            <div v-else-if="active === 'licenses'" class="detail-grid">
              <div><span>Producto</span><b>{{ selected.product }}</b></div><div><span>Plan</span><b>{{ selected.plan }}</b></div><div><span>Emisión</span><b>{{ date(selected.issuedAt || selected.startedAt) }}</b></div><div><span>Vencimiento</span><b>{{ date(selected.expiresAt || selected.periodEnd) }}</b></div><div v-if="selected.maxActivations != null"><span>Activaciones</span><b>{{ selected.activeActivations || 0 }} de {{ selected.maxActivations }}</b></div>
              <div v-if="selected.licenseKey" class="license-key-row"><span>Clave de licencia</span><code>{{ selected.licenseKey }}</code><button @click="copyLicense(selected.licenseKey)"><UiIcon name="copy" :size="16"/>Copiar</button></div>
            </div>
            <template v-else-if="active === 'orders'">
              <div class="detail-grid">
                <div><span>Número de orden</span><b>{{ selected.orderNumber }}</b></div><div><span>Fecha</span><b>{{ date(selected.createdAt) }}</b></div><div><span>Estado</span><b>{{ selected.status }}</b></div><div><span>Subtotal</span><b>{{ money(selected.subtotal, selected.currency) }}</b></div><div><span>Impuestos</span><b>{{ money(selected.taxTotal, selected.currency) }}</b></div><div><span>Total</span><b>{{ money(selected.total, selected.currency) }}</b></div>
              </div>
              <div v-if="selected.lines?.length" class="order-detail-list"><h3>Artículos</h3><div v-for="line in selected.lines" :key="line.id"><span>{{ line.quantity }} × {{ line.name }}</span><b>{{ money(line.total, selected.currency) }}</b></div></div>
              <div v-if="selected.payments?.length" class="order-detail-list"><h3>Pagos</h3><div v-for="paymentItem in selected.payments" :key="paymentItem.id"><span>{{ paymentItem.gateway }} · {{ paymentItem.status }}</span><b>{{ money(paymentItem.amount, paymentItem.currency) }}</b></div></div>
            </template>
            <div class="detail-actions"><RouterLink v-if="selected.href" :to="selected.href" class="account-button"><UiIcon name="external" :size="17"/>Abrir</RouterLink><button v-if="active === 'travel' && Number(selected.balanceDue) > 0" class="account-button" @click="payTravel(selected)">Pagar saldo {{ money(selected.balanceDue, selected.currency) }}</button><a v-if="selected.googleCalendarUrl" :href="selected.googleCalendarUrl" target="_blank" rel="noopener" class="account-button secondary"><UiIcon name="calendar" :size="17"/>Agregar a Google Calendar</a><RouterLink v-if="selected.tourUrl" :to="selected.tourUrl" class="account-button secondary">Ver experiencia</RouterLink></div>
          </article>

          <section v-if="activeSection?.items?.length" class="item-grid">
            <article v-for="item in activeSection.items" :key="item.id || item.reference" class="item-card">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="item-card__image">
              <div class="item-card__top"><span :class="['badge', statusClass(item.status || item.paymentStatus)]">{{ item.status || item.paymentStatus || 'ACTIVO' }}</span><small>{{ item.reference || item.code || item.orderNumber || item.bookingCode }}</small></div>
              <h2>{{ item.title || item.name || item.tourName || item.productName }}</h2><p>{{ item.description || (item.startsAt ? date(item.startsAt) : item.createdAt) }}</p>
              <div v-if="item.progress != null" class="course-progress"><div><i :style="{ width: `${Number(item.progress) || 0}%` }"/></div><span>{{ Number(item.progress) || 0 }}%</span></div>
              <dl><template v-for="(value, key) in item" :key="String(key)"><div v-if="['total', 'amount', 'balanceDue', 'amountPaid'].includes(String(key))"><dt>{{ labelFor(String(key)) }}</dt><dd>{{ money(value, item.currency || 'USD') }}</dd></div></template></dl>
              <button class="item-detail-button" @click="selected = item"><UiIcon name="external" :size="16"/>Ver detalle</button>
            </article>
          </section>
          <div v-else class="empty-state"><UiIcon :name="icon(activeSection)" :size="40"/><h2>No hay registros todavía</h2><p>Cuando este plugin genere información para su cuenta, aparecerá aquí automáticamente.</p></div>
        </template>
      </section>
    </div>
  </main>
</template>

<style scoped>
.account-page{--brand:var(--theme-primary,var(--primary,#087f7b));--brand-strong:var(--theme-primary-hover,var(--primary-strong,#066864));--ink:var(--theme-text,var(--text,#132f34));--muted:var(--theme-text-muted,var(--muted,#667085));--surface:var(--theme-surface,var(--surface,#fff));--soft:var(--theme-surface-muted,var(--surface-soft,#f2f8f7));--border:var(--theme-border,var(--line,#d8e6e4));min-height:78vh;background:var(--soft);color:var(--ink);overflow-x:hidden}.kicker{font-size:.7rem;font-weight:900;letter-spacing:.18em;color:var(--brand)}
.auth-wrap{width:min(1120px,calc(100% - 32px));min-height:75vh;margin:auto;display:grid;grid-template-columns:minmax(0,1fr) minmax(320px,470px);gap:clamp(32px,6vw,80px);align-items:center;padding:65px 0}.auth-copy h1{font-size:clamp(2.8rem,7vw,5.4rem);line-height:.95;margin:.5rem 0 1.4rem}.auth-copy p{max-width:590px;font-size:1.1rem;color:var(--muted);line-height:1.7}.auth-copy ul{display:grid;grid-template-columns:1fr 1fr;gap:12px;list-style:none;padding:0;margin-top:30px}.auth-copy li{padding:14px;border:1px solid var(--border);border-radius:13px;background:var(--surface)}.auth-copy li:before{content:'✓';color:var(--brand);margin-right:8px}.auth-card,.panel-card,.item-card,.detail-panel{background:var(--surface);border:1px solid var(--border);box-shadow:0 24px 70px #17383a14}.auth-card{padding:32px;border-radius:26px}.auth-notice{padding:12px;margin-bottom:14px;border-radius:12px;background:color-mix(in srgb,var(--brand) 10%,var(--surface));font-size:.84rem}.switch{display:grid;grid-template-columns:1fr 1fr;padding:4px;background:var(--soft);border-radius:13px;margin-bottom:25px}.switch button{border:0;border-radius:10px;background:transparent;padding:11px;font-weight:800;color:var(--muted)}.switch button.active{background:var(--surface);color:var(--brand);box-shadow:0 4px 15px #24304a12}.auth-card form,.panel-card{display:grid;gap:15px}.auth-card label,.panel-card label{display:grid;gap:7px;font-size:.78rem;font-weight:800}.auth-card input,.panel-card :is(input,textarea){width:100%;border:1px solid var(--border);border-radius:12px;padding:12px;background:var(--surface);color:var(--ink)}.auth-card form>button,.panel-card>button,.account-button{display:flex;align-items:center;justify-content:center;gap:8px;border:0;border-radius:12px;background:var(--brand);color:white;padding:14px;font-weight:900;text-decoration:none}.account-button.secondary{background:var(--soft);color:var(--brand);border:1px solid var(--border)}.error{color:var(--danger,#b42318)}
.portal-shell{width:min(1280px,calc(100% - 32px));margin:auto;display:grid;grid-template-columns:minmax(230px,285px) minmax(0,1fr);gap:28px;padding:38px 0 70px}.account-nav{position:sticky;top:20px;height:fit-content;padding:18px;border-radius:23px;background:color-mix(in srgb,var(--ink) 92%,#061b1e);color:white}.user{display:flex;gap:12px;align-items:center;padding:10px 8px 22px;border-bottom:1px solid #ffffff18}.user>span{display:grid;place-items:center;width:46px;height:46px;border-radius:50%;background:var(--brand);font-weight:900}.user div{display:grid;min-width:0}.user strong,.user small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.user small{color:#bcc9c9}.account-nav nav{display:grid;gap:6px;padding:18px 0}.account-nav nav button,.logout{display:grid;grid-template-columns:22px minmax(0,1fr) auto;align-items:center;gap:10px;width:100%;border:0;border-radius:12px;background:transparent;color:#d6e0e0;padding:12px;text-align:left;font-weight:800}.account-nav nav button.active{background:color-mix(in srgb,var(--brand) 28%,transparent);color:white}.account-nav nav i{display:grid;place-items:center;min-width:23px;height:23px;border-radius:999px;background:#ffffff15;font-size:.7rem;font-style:normal}.logout{display:flex;color:#ffaaa1}.account-content{min-width:0}.account-content>header{display:flex;justify-content:space-between;gap:24px;margin:4px 0 28px}.account-content h1{font-size:clamp(2.2rem,4vw,3.3rem);line-height:1.05;margin:.45rem 0 .5rem}.account-content header p{color:var(--muted)}.home-link{display:inline-flex;gap:7px;color:var(--brand);font-weight:900;text-decoration:none}.profile-grid{display:grid;grid-template-columns:minmax(0,1fr) minmax(300px,.78fr);gap:20px;align-items:start}.panel-card{padding:24px;border-radius:21px}.grid-two{display:grid;grid-template-columns:1fr 1fr;gap:12px}.integration-note{padding:15px;border-radius:14px;background:var(--soft)}
.item-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.item-card{border-radius:20px;padding:21px;overflow:hidden}.item-card__image{width:calc(100% + 42px);height:150px;object-fit:cover;margin:-21px -21px 18px}.item-card__top{display:flex;justify-content:space-between;gap:12px}.item-card h2{font-size:1.1rem;margin:18px 0 7px}.item-card>p{color:var(--muted);line-height:1.55}.badge{display:inline-flex;border-radius:999px;padding:5px 9px;background:#eef2f6;font-size:.67rem;font-weight:900}.badge.ok{background:color-mix(in srgb,var(--success,#067647) 16%,white);color:var(--success,#067647)}.badge.pending{background:color-mix(in srgb,var(--warning,#b54708) 14%,white);color:var(--warning,#b54708)}.course-progress{display:flex;align-items:center;gap:10px;margin:16px 0;font-size:.75rem;font-weight:900}.course-progress>div{height:8px;flex:1;border-radius:999px;overflow:hidden;background:var(--border)}.course-progress i{display:block;height:100%;background:var(--brand)}.item-card dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin:18px 0}.item-card dl div{padding:10px;border-radius:11px;background:var(--soft)}.item-card dt,.detail-grid span{font-size:.68rem;color:var(--muted)}.item-card dd{margin:4px 0 0;font-weight:900}.item-detail-button{display:inline-flex;align-items:center;gap:7px;border:0;background:transparent;color:var(--brand);font-weight:900;padding:0;cursor:pointer}.detail-panel{position:relative;border-radius:22px;padding:25px;margin-bottom:22px}.detail-close{position:absolute;right:16px;top:16px;border:0;background:var(--soft);width:36px;height:36px;border-radius:50%;font-size:1.4rem}.detail-heading{display:grid;grid-template-columns:150px 1fr;gap:20px;align-items:center}.detail-heading img{width:150px;height:115px;object-fit:cover;border-radius:16px}.detail-heading h2{margin:10px 0 5px}.detail-heading p{color:var(--muted)}.detail-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:22px}.detail-grid>div{display:grid;gap:6px;padding:14px;border-radius:13px;background:var(--soft)}.license-key-row{grid-column:1/-1!important}.license-key-row code{overflow-wrap:anywhere;font-size:.85rem}.license-key-row button{justify-self:start;border:0;background:var(--brand);color:white;padding:8px 12px;border-radius:9px}.order-detail-list{display:grid;gap:9px;margin-top:20px;padding:17px;border:1px solid var(--border);border-radius:15px}.order-detail-list h3{margin:0 0 4px}.order-detail-list>div{display:flex;justify-content:space-between;gap:18px;padding:10px 0;border-top:1px solid var(--border)}.detail-actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:20px}.empty-state{display:grid;place-items:center;text-align:center;padding:80px 24px;border:1px dashed var(--border);border-radius:22px;background:var(--surface);color:var(--muted)}
@media(max-width:1000px){.portal-shell{grid-template-columns:1fr}.account-nav{position:static}.account-nav nav{grid-template-columns:repeat(2,minmax(0,1fr))}.profile-grid{grid-template-columns:1fr}}@media(max-width:820px){.auth-wrap{grid-template-columns:1fr}.auth-copy ul,.item-grid{grid-template-columns:1fr}.account-content>header{flex-direction:column}.account-nav nav{grid-template-columns:1fr}.detail-heading{grid-template-columns:1fr}.detail-grid{grid-template-columns:1fr 1fr}}@media(max-width:560px){.portal-shell,.auth-wrap{width:min(100% - 20px,1280px)}.grid-two,.detail-grid{grid-template-columns:1fr}.item-card dl{grid-template-columns:1fr}}
</style>
