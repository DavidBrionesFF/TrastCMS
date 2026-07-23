<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api } from '@/services/api'
import AdminPageHeader from '@/components/common/AdminPageHeader.vue'
import UiIcon from '@/components/common/UiIcon.vue'
import type {
  CrmActivity,
  CrmCompany,
  CrmContact,
  CrmContactStatus,
  CrmDashboard,
  CrmDeal,
  CrmForm,
  CrmFormField,
  CrmStage,
  CrmSubmission,
  CrmSubmissionStatus,
  PageResponse
} from '@/types'

type Tab = 'dashboard' | 'contacts' | 'companies' | 'deals' | 'activities' | 'forms' | 'submissions'
type Editor = 'contact' | 'company' | 'deal' | 'activity' | 'form' | 'stage' | null

const tab = ref<Tab>('dashboard')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const search = ref('')
const contactStatus = ref<'ALL' | CrmContactStatus>('ALL')
const dashboard = ref<CrmDashboard | null>(null)
const contacts = ref<CrmContact[]>([])
const companies = ref<CrmCompany[]>([])
const stages = ref<CrmStage[]>([])
const deals = ref<CrmDeal[]>([])
const activities = ref<CrmActivity[]>([])
const forms = ref<CrmForm[]>([])
const submissions = ref<CrmSubmission[]>([])
const editor = ref<Editor>(null)
const draft = reactive<Record<string, any>>({})
const saving = ref(false)
const selectedSubmission = ref<CrmSubmission | null>(null)

const tabs: Array<{ id: Tab; label: string; icon: string }> = [
  { id: 'dashboard', label: 'Resumen', icon: 'dashboard' },
  { id: 'contacts', label: 'Contactos', icon: 'users' },
  { id: 'companies', label: 'Empresas', icon: 'company' },
  { id: 'deals', label: 'Negocios', icon: 'pipeline' },
  { id: 'activities', label: 'Actividades', icon: 'calendar' },
  { id: 'forms', label: 'Formularios', icon: 'form' },
  { id: 'submissions', label: 'Envíos', icon: 'inbox' }
]

const money = (value: number | string, currency = 'HNL') => new Intl.NumberFormat('es-HN', {
  style: 'currency', currency
}).format(Number(value || 0))

const filteredContacts = computed(() => {
  const term = search.value.trim().toLowerCase()
  return contacts.value.filter(item => {
    const termOk = !term || `${item.firstName} ${item.lastName || ''} ${item.email || ''} ${item.phone || ''} ${item.company?.name || ''}`.toLowerCase().includes(term)
    const statusOk = contactStatus.value === 'ALL' || item.status === contactStatus.value
    return termOk && statusOk
  })
})

const filteredCompanies = computed(() => {
  const term = search.value.trim().toLowerCase()
  return companies.value.filter(item => !term || `${item.name} ${item.domain || ''} ${item.email || ''} ${item.city || ''}`.toLowerCase().includes(term))
})

const pendingActivities = computed(() => activities.value.filter(item => !item.completedAt))
const formSubmissionCount = computed(() => {
  const values = new Map<string, number>()
  submissions.value.forEach(item => values.set(item.formId, (values.get(item.formId) || 0) + 1))
  return values
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await api<CrmDashboard>('/api/admin/crm/dashboard')
    const [contactPage, companyPage, stageList] = await Promise.all([
      api<PageResponse<CrmContact>>('/api/admin/crm/contacts?size=100'),
      api<PageResponse<CrmCompany>>('/api/admin/crm/companies?size=100'),
      api<CrmStage[]>('/api/admin/crm/stages')
    ])
    contacts.value = contactPage.content
    companies.value = companyPage.content
    stages.value = stageList

    if (tab.value === 'dashboard' || tab.value === 'deals' || tab.value === 'activities') {
      deals.value = (await api<PageResponse<CrmDeal>>('/api/admin/crm/deals?size=100')).content
    }
    if (tab.value === 'dashboard' || tab.value === 'activities') {
      activities.value = (await api<PageResponse<CrmActivity>>('/api/admin/crm/activities?size=100')).content
    }
    if (tab.value === 'forms' || tab.value === 'dashboard') {
      forms.value = await api<CrmForm[]>('/api/admin/crm/forms')
    }
    if (tab.value === 'submissions' || tab.value === 'forms' || tab.value === 'dashboard') {
      submissions.value = (await api<PageResponse<CrmSubmission>>('/api/admin/crm/submissions?size=100')).content
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo cargar TrastCRM'
  } finally {
    loading.value = false
  }
}

watch(tab, () => {
  search.value = ''
  selectedSubmission.value = null
  load()
})
onMounted(load)

function reset(value: Record<string, any>) {
  Object.keys(draft).forEach(key => delete draft[key])
  Object.assign(draft, value)
}

function openNew(type: Exclude<Editor, null>) {
  editor.value = type
  if (type === 'contact') reset({ firstName: '', lastName: '', email: '', phone: '', jobTitle: '', status: 'LEAD', source: 'Manual', ownerEmail: '', tags: '', notes: '', companyId: '' })
  if (type === 'company') reset({ name: '', domain: '', email: '', phone: '', address: '', city: '', country: 'Honduras', status: 'PROSPECT', notes: '' })
  if (type === 'deal') reset({ title: '', value: 0, currency: 'HNL', expectedCloseDate: '', ownerEmail: '', description: '', contactId: '', companyId: '', stageId: stages.value[0]?.id || '' })
  if (type === 'activity') reset({ type: 'TASK', subject: '', description: '', dueAt: '', completed: false, assignedTo: '', contactId: '', companyId: '', dealId: '' })
  if (type === 'stage') reset({ name: '', position: (stages.value.at(-1)?.position || 0) + 10, probability: 20, color: '#6d4aff', closed: false, won: false })
  if (type === 'form') reset({ formKey: '', name: '', description: '', successMessage: 'Gracias. Hemos recibido su información.', notifyEmails: '', active: true, settings: { submitLabel: 'Enviar', createLead: true, createDeal: false, createFollowUp: true }, fields: [fieldTemplate()] })
}

function edit(type: Exclude<Editor, null>, value: any) {
  editor.value = type
  if (type === 'contact') reset({ ...value, companyId: value.company?.id || '' })
  else if (type === 'deal') reset({ ...value, contactId: value.contact?.id || '', companyId: value.company?.id || '', stageId: value.stage?.id || '' })
  else if (type === 'activity') reset({ ...value, completed: Boolean(value.completedAt), dueAt: value.dueAt ? value.dueAt.slice(0, 16) : '' })
  else if (type === 'form') reset({ ...JSON.parse(JSON.stringify(value)), settings: { submitLabel: 'Enviar', createLead: true, createDeal: false, createFollowUp: true, ...(value.settings || {}) }, fields: value.fields || [] })
  else reset(JSON.parse(JSON.stringify(value)))
}

function fieldTemplate(): CrmFormField {
  return { name: `campo_${Date.now().toString().slice(-5)}`, label: 'Nuevo campo', type: 'text', required: false, placeholder: '', options: [] }
}

function addField() {
  ;(draft.fields as CrmFormField[]).push(fieldTemplate())
}

function duplicateField(index: number) {
  const field = (draft.fields as CrmFormField[])[index]
  ;(draft.fields as CrmFormField[]).splice(index + 1, 0, { ...JSON.parse(JSON.stringify(field)), name: `${field.name}_copia` })
}

function removeField(index: number) {
  ;(draft.fields as CrmFormField[]).splice(index, 1)
}

function moveField(index: number, direction: -1 | 1) {
  const target = index + direction
  const fields = draft.fields as CrmFormField[]
  if (target < 0 || target >= fields.length) return
  const [field] = fields.splice(index, 1)
  fields.splice(target, 0, field)
}

async function save() {
  if (!editor.value) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const type = editor.value
    const id = draft.id
    const endpoint = type === 'contact' ? 'contacts' : type === 'company' ? 'companies' : type === 'deal' ? 'deals' : type === 'activity' ? 'activities' : type === 'form' ? 'forms' : 'stages'
    const payload = { ...draft }
    if (type === 'activity' && payload.dueAt) payload.dueAt = new Date(payload.dueAt).toISOString()
    await api(`/api/admin/crm/${endpoint}${id ? `/${id}` : ''}`, {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    })
    notice.value = 'Información guardada correctamente.'
    editor.value = null
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo guardar'
  } finally {
    saving.value = false
  }
}

async function remove(type: string, id: string, label: string) {
  if (!confirm(`¿Eliminar ${label}?`)) return
  try {
    await api(`/api/admin/crm/${type}/${id}`, { method: 'DELETE' })
    notice.value = 'Registro eliminado.'
    await load()
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : 'No se pudo eliminar'
  }
}

async function submissionStatus(item: CrmSubmission, status: CrmSubmissionStatus) {
  await api(`/api/admin/crm/submissions/${item.id}/status`, {
    method: 'PUT', body: JSON.stringify({ status })
  })
  item.status = status
  await load()
}

function changeSubmissionStatus(item: CrmSubmission, event: Event) {
  submissionStatus(item, (event.target as HTMLSelectElement).value as CrmSubmissionStatus)
}

function changeFieldOptions(field: CrmFormField, event: Event) {
  field.options = (event.target as HTMLInputElement).value.split(',').map(value => value.trim()).filter(Boolean)
}

function contactName(contact?: CrmContact) {
  return contact ? `${contact.firstName} ${contact.lastName || ''}`.trim() : 'Sin contacto'
}

function initials(contact: CrmContact) {
  return `${contact.firstName.charAt(0)}${contact.lastName?.charAt(0) || ''}`.toUpperCase()
}

function actionLabel() {
  if (tab.value === 'contacts') return 'Nuevo contacto'
  if (tab.value === 'companies') return 'Nueva empresa'
  if (tab.value === 'deals') return 'Nuevo negocio'
  if (tab.value === 'activities') return 'Nueva actividad'
  if (tab.value === 'forms') return 'Nuevo formulario'
  return ''
}

function createsLead(item: CrmForm): boolean {
  return item.settings.createLead !== false
}

function createsDeal(item: CrmForm): boolean {
  return item.settings.createDeal === true
}

function createsFollowUp(item: CrmForm): boolean {
  return item.settings.createFollowUp !== false
}

function openContextAction() {
  const type = actionType()
  if (type) openNew(type)
}

function actionType(): Exclude<Editor, null> | null {
  if (tab.value === 'contacts') return 'contact'
  if (tab.value === 'companies') return 'company'
  if (tab.value === 'deals') return 'deal'
  if (tab.value === 'activities') return 'activity'
  if (tab.value === 'forms') return 'form'
  return null
}
</script>

<template>
  <AdminPageHeader eyebrow="Plugin incorporado" title="TrastCRM" description="Convierta formularios del sitio en contactos, oportunidades y actividades de seguimiento.">
    <template #actions>
      <button v-if="actionType()" class="button" @click="openContextAction"><UiIcon name="plus" :size="16"/>{{ actionLabel() }}</button>
    </template>
  </AdminPageHeader>

  <section class="crm-intro">
    <div><span class="eyebrow">Centro comercial</span><h2>Desde la primera consulta hasta el cierre</h2><p>TrastCRM está conectado con sus páginas y formularios. Cada envío puede convertirse en un lead, relacionarse con una empresa y avanzar por el pipeline.</p></div>
    <div class="crm-intro__steps"><span><b>1</b>Captar</span><i></i><span><b>2</b>Calificar</span><i></i><span><b>3</b>Dar seguimiento</span><i></i><span><b>4</b>Cerrar</span></div>
  </section>

  <nav class="crm-tabs" aria-label="Secciones del CRM">
    <button v-for="item in tabs" :key="item.id" :class="{ active: tab === item.id }" @click="tab=item.id"><UiIcon :name="item.icon" :size="17"/><span>{{ item.label }}</span><b v-if="item.id==='submissions' && dashboard?.newSubmissions">{{ dashboard.newSubmissions }}</b></button>
  </nav>

  <p v-if="notice" class="form-success mt-4">{{ notice }}</p>
  <p v-if="error" class="form-error mt-4">{{ error }}</p>
  <div v-if="loading" class="panel mt-5 empty">Actualizando TrastCRM…</div>

  <template v-else>
    <section v-if="tab==='dashboard' && dashboard" class="crm-dashboard">
      <div class="crm-metrics">
        <article><span class="violet"><UiIcon name="users" :size="20"/></span><div><small>Contactos</small><strong>{{ dashboard.contacts }}</strong><p>personas registradas</p></div></article>
        <article><span class="blue"><UiIcon name="company" :size="20"/></span><div><small>Empresas</small><strong>{{ dashboard.companies }}</strong><p>organizaciones</p></div></article>
        <article><span class="emerald"><UiIcon name="pipeline" :size="20"/></span><div><small>Pipeline abierto</small><strong>{{ money(dashboard.pipelineValue) }}</strong><p>{{ dashboard.openDeals }} oportunidades</p></div></article>
        <article><span class="amber"><UiIcon name="calendar" :size="20"/></span><div><small>Seguimientos</small><strong>{{ dashboard.pendingActivities }}</strong><p>actividades pendientes</p></div></article>
        <article><span class="rose"><UiIcon name="inbox" :size="20"/></span><div><small>Nuevos leads</small><strong>{{ dashboard.newSubmissions }}</strong><p>envíos por atender</p></div></article>
      </div>

      <div class="crm-dashboard-grid">
        <section class="crm-surface crm-pipeline-summary">
          <header><div><h2>Embudo comercial</h2><p>Valor y oportunidades distribuidas por etapa.</p></div><button @click="tab='deals'">Abrir pipeline →</button></header>
          <div class="crm-stage-summary" v-for="stage in dashboard.pipeline" :key="stage.id"><div class="crm-stage-summary__label"><span><i :style="{ background: stage.color }"></i>{{ stage.name }}</span><strong>{{ stage.deals }} · {{ money(stage.value) }}</strong></div><div><i :style="{ width: `${Math.max(4, stage.probability)}%`, background: stage.color }"></i></div></div>
        </section>

        <section class="crm-surface">
          <header><div><h2>Próximas acciones</h2><p>No deje oportunidades sin seguimiento.</p></div><button @click="tab='activities'">Ver todas →</button></header>
          <div v-if="pendingActivities.length" class="crm-activity-list"><article v-for="item in pendingActivities.slice(0,5)" :key="item.id"><span><UiIcon name="calendar" :size="16"/></span><div><strong>{{ item.subject }}</strong><small>{{ item.type }} · {{ item.dueAt ? new Date(item.dueAt).toLocaleString() : 'Sin fecha' }}</small></div></article></div>
          <div v-else class="crm-mini-empty"><UiIcon name="check" :size="24"/><p>No hay actividades pendientes.</p></div>
        </section>
      </div>

      <section class="crm-surface">
        <header><div><h2>Captación desde la web</h2><p>Formularios instalados y solicitudes recientes.</p></div><button @click="tab='forms'">Administrar formularios →</button></header>
        <div class="crm-capture-grid"><article><span><UiIcon name="form" :size="20"/></span><strong>{{ dashboard.forms }}</strong><p>formularios activos para incrustar en páginas</p></article><article><span><UiIcon name="inbox" :size="20"/></span><strong>{{ submissions.length }}</strong><p>envíos disponibles en el centro de leads</p></article><article><span><UiIcon name="page" :size="20"/></span><strong>Conectado</strong><p>bloque visual listo para páginas y landing pages</p></article></div>
      </section>
    </section>

    <section v-else-if="tab==='contacts'" class="crm-surface">
      <header class="crm-list-header"><div><h2>Contactos y leads</h2><p>Personas captadas manualmente o desde formularios del sitio.</p></div><div class="crm-filters"><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Nombre, correo, teléfono o empresa"></div><select v-model="contactStatus" class="control-select"><option value="ALL">Todos los estados</option><option value="LEAD">Lead</option><option value="PROSPECT">Prospecto</option><option value="CUSTOMER">Cliente</option><option value="INACTIVE">Inactivo</option></select></div></header>
      <div v-if="!filteredContacts.length" class="crm-empty"><UiIcon name="users" :size="38"/><h3>No hay contactos para mostrar</h3><p>Cree un contacto manualmente o incruste un formulario CRM en una página.</p><button class="button" @click="openNew('contact')">Crear contacto</button></div>
      <div v-else class="crm-table-wrap"><table class="crm-table"><thead><tr><th>Contacto</th><th>Empresa</th><th>Estado</th><th>Origen</th><th>Última actualización</th><th></th></tr></thead><tbody><tr v-for="item in filteredContacts" :key="item.id"><td><div class="crm-person"><span>{{ initials(item) }}</span><div><strong>{{ contactName(item) }}</strong><a v-if="item.email" :href="`mailto:${item.email}`">{{ item.email }}</a><small v-if="item.phone">{{ item.phone }}</small></div></div></td><td>{{ item.company?.name || 'Sin empresa' }}</td><td><span :class="['crm-status', item.status.toLowerCase()]">{{ item.status }}</span></td><td><span class="crm-source">{{ item.source || 'Manual' }}</span></td><td>{{ new Date(item.updatedAt).toLocaleDateString() }}</td><td><div class="table-actions"><button title="Editar" @click="edit('contact',item)"><UiIcon name="edit" :size="16"/></button><button class="danger" title="Eliminar" @click="remove('contacts',item.id,contactName(item))"><UiIcon name="trash" :size="16"/></button></div></td></tr></tbody></table></div>
    </section>

    <section v-else-if="tab==='companies'" class="crm-surface">
      <header class="crm-list-header"><div><h2>Empresas y organizaciones</h2><p>Agrupe contactos, oportunidades y contexto comercial.</p></div><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Nombre, dominio, correo o ciudad"></div></header>
      <div v-if="!filteredCompanies.length" class="crm-empty"><UiIcon name="company" :size="38"/><h3>No hay empresas</h3><p>Cree organizaciones para relacionar contactos y negocios.</p><button class="button" @click="openNew('company')">Crear empresa</button></div>
      <div v-else class="crm-company-grid"><article v-for="item in filteredCompanies" :key="item.id"><header><span>{{ item.name.charAt(0).toUpperCase() }}</span><div><h3>{{ item.name }}</h3><a v-if="item.domain" :href="`https://${item.domain}`" target="_blank">{{ item.domain }}</a></div><b :class="['crm-status',item.status.toLowerCase()]">{{ item.status }}</b></header><dl><div><dt>Correo</dt><dd>{{ item.email || '—' }}</dd></div><div><dt>Teléfono</dt><dd>{{ item.phone || '—' }}</dd></div><div><dt>Ubicación</dt><dd>{{ [item.city,item.country].filter(Boolean).join(', ') || '—' }}</dd></div></dl><footer><button @click="edit('company',item)">Editar</button><button class="danger" @click="remove('companies',item.id,item.name)">Eliminar</button></footer></article></div>
    </section>

    <section v-else-if="tab==='deals'" class="crm-pipeline-board">
      <header><div><span class="eyebrow">Pipeline</span><h2>Oportunidades comerciales</h2><p>Mueva cada negociación por etapas y mantenga visible el valor esperado.</p></div><button class="button secondary" @click="openNew('stage')"><UiIcon name="plus" :size="16"/>Nueva etapa</button></header>
      <div class="crm-pipeline-scroll"><section v-for="stage in stages" :key="stage.id" class="crm-pipeline-column"><header><div><span :style="{ background: stage.color }"></span><strong>{{ stage.name }}</strong></div><button title="Editar etapa" @click="edit('stage',stage)"><UiIcon name="settings" :size="15"/></button></header><div class="crm-pipeline-column__summary"><b>{{ deals.filter(deal => deal.stage.id === stage.id).length }} negocios</b><span>{{ money(deals.filter(deal => deal.stage.id === stage.id).reduce((sum,deal)=>sum+Number(deal.value),0)) }}</span></div><article v-for="item in deals.filter(deal=>deal.stage.id===stage.id)" :key="item.id" class="crm-deal-card"><div class="crm-deal-card__top"><span>{{ item.company?.name || contactName(item.contact) }}</span><button @click="edit('deal',item)"><UiIcon name="more" :size="17"/></button></div><h3>{{ item.title }}</h3><strong>{{ money(item.value,item.currency) }}</strong><p v-if="item.expectedCloseDate">Cierre esperado: {{ new Date(`${item.expectedCloseDate}T00:00:00`).toLocaleDateString() }}</p><footer><button @click="edit('deal',item)">Editar</button><button class="danger" @click="remove('deals',item.id,item.title)">Eliminar</button></footer></article><div v-if="!deals.some(deal=>deal.stage.id===stage.id)" class="crm-column-empty">Sin negocios en esta etapa</div></section></div>
    </section>

    <section v-else-if="tab==='activities'" class="crm-surface">
      <header class="crm-list-header"><div><h2>Actividades y seguimiento</h2><p>Tareas, llamadas, reuniones, correos y notas relacionadas con el proceso comercial.</p></div><span class="crm-summary-chip">{{ pendingActivities.length }} pendientes</span></header>
      <div v-if="!activities.length" class="crm-empty"><UiIcon name="calendar" :size="38"/><h3>Sin actividades</h3><p>Programe una llamada o tarea para mantener el seguimiento.</p><button class="button" @click="openNew('activity')">Crear actividad</button></div>
      <div v-else class="crm-activity-timeline"><article v-for="item in activities" :key="item.id" :class="{ completed: item.completedAt }"><span><UiIcon :name="item.type === 'CALL' ? 'account' : item.type === 'EMAIL' ? 'inbox' : 'calendar'" :size="17"/></span><div><header><div><b>{{ item.subject }}</b><small>{{ item.type }}</small></div><em>{{ item.dueAt ? new Date(item.dueAt).toLocaleString() : 'Sin vencimiento' }}</em></header><p>{{ item.description || 'Sin descripción.' }}</p><footer><span :class="['crm-status',item.completedAt?'customer':'lead']">{{ item.completedAt ? 'Completada' : 'Pendiente' }}</span><div><button @click="edit('activity',item)">Editar</button><button class="danger" @click="remove('activities',item.id,item.subject)">Eliminar</button></div></footer></div></article></div>
    </section>

    <section v-else-if="tab==='forms'" class="crm-surface">
      <header class="crm-list-header"><div><h2>Formularios de captación</h2><p>Diseñe formularios, incrústelos en páginas y convierta envíos en contactos.</p></div><div class="crm-summary-chip">{{ forms.length }} formularios</div></header>
      <div class="crm-integration-banner"><span><UiIcon name="page" :size="22"/></span><div><strong>Integración con el constructor visual</strong><p>En una página agregue el bloque <b>Formulario CRM</b> y seleccione cualquiera de los formularios creados aquí.</p></div><RouterLink :to="{ name: 'page-new' }" class="button secondary small">Crear landing</RouterLink></div>
      <div v-if="!forms.length" class="crm-empty"><UiIcon name="form" :size="38"/><h3>No hay formularios</h3><p>Cree uno para comenzar a captar oportunidades desde la web.</p><button class="button" @click="openNew('form')">Crear formulario</button></div>
      <div v-else class="crm-form-grid"><article v-for="item in forms" :key="item.id"><header><span class="crm-form-icon"><UiIcon name="form" :size="22"/></span><div><span class="eyebrow">{{ item.formKey }}</span><h3>{{ item.name }}</h3></div><b :class="['crm-status',item.active?'customer':'inactive']">{{ item.active?'Activo':'Inactivo' }}</b></header><p>{{ item.description || 'Formulario sin descripción.' }}</p><div class="crm-form-stats"><span><strong>{{ item.fields.length }}</strong>campos</span><span><strong>{{ formSubmissionCount.get(item.id) || 0 }}</strong>envíos</span><span><strong>{{ createsLead(item) ? 'Sí' : 'No' }}</strong>crea lead</span><span><strong>{{ createsDeal(item) ? 'Sí' : 'No' }}</strong>crea negocio</span><span><strong>{{ createsFollowUp(item) ? 'Sí' : 'No' }}</strong>crea seguimiento</span></div><code>plugin:trastcrm:form · {{ item.formKey }}</code><footer><button class="button secondary small" @click="edit('form',item)"><UiIcon name="edit" :size="15"/>Diseñar</button><button class="button danger small" @click="remove('forms',item.id,item.name)"><UiIcon name="trash" :size="15"/>Eliminar</button></footer></article></div>
    </section>

    <section v-else class="crm-surface">
      <header class="crm-list-header"><div><h2>Centro de envíos</h2><p>Revise solicitudes recibidas, clasifíquelas y conviértalas en seguimiento comercial.</p></div><span class="crm-summary-chip">{{ submissions.filter(item=>item.status==='NEW').length }} nuevos</span></header>
      <div v-if="!submissions.length" class="crm-empty"><UiIcon name="inbox" :size="38"/><h3>Aún no hay envíos</h3><p>Cuando una persona complete un formulario, aparecerá aquí.</p></div>
      <div v-else class="crm-submission-list"><article v-for="item in submissions" :key="item.id" :class="{ unread:item.status==='NEW' }"><button class="crm-submission-main" @click="selectedSubmission=item"><span class="crm-form-icon"><UiIcon name="inbox" :size="20"/></span><div><strong>{{ item.formName }}</strong><small>{{ item.sourceUrl || 'Origen no registrado' }}</small></div><time>{{ new Date(item.createdAt).toLocaleString() }}</time></button><div class="crm-submission-actions"><select :value="item.status" @change="changeSubmissionStatus(item,$event)"><option value="NEW">Nuevo</option><option value="READ">Leído</option><option value="ARCHIVED">Archivado</option><option value="SPAM">Spam</option></select><button class="danger" @click="remove('submissions',item.id,'este envío')"><UiIcon name="trash" :size="16"/></button></div></article></div>
    </section>
  </template>

  <Teleport to="body">
    <div v-if="editor" class="modal-backdrop" @click.self="editor=null">
      <form class="modal-card crm-modal" @submit.prevent="save">
        <header class="modal-header"><div><span class="eyebrow">TrastCRM</span><h2>{{ draft.id ? 'Editar' : 'Crear' }} {{ editor }}</h2><p>Complete la información y guárdela sin abandonar el flujo de trabajo.</p></div><button type="button" class="icon-button" @click="editor=null"><UiIcon name="close" :size="18"/></button></header>
        <div class="modal-scroll">
          <template v-if="editor==='contact'">
            <section class="modal-section"><h3>Información del contacto</h3><div class="form-grid two"><label>Nombres<input v-model="draft.firstName" required></label><label>Apellidos<input v-model="draft.lastName"></label><label>Correo<input v-model="draft.email" type="email"></label><label>Teléfono<input v-model="draft.phone"></label><label>Cargo<input v-model="draft.jobTitle"></label><label>Empresa<select v-model="draft.companyId"><option value="">Sin empresa</option><option v-for="item in companies" :key="item.id" :value="item.id">{{ item.name }}</option></select></label></div></section>
            <section class="modal-section"><h3>Calificación comercial</h3><div class="form-grid two"><label>Estado<select v-model="draft.status"><option value="LEAD">Lead</option><option value="PROSPECT">Prospecto</option><option value="CUSTOMER">Cliente</option><option value="INACTIVE">Inactivo</option></select></label><label>Origen<input v-model="draft.source" placeholder="Web, referido, campaña…"></label><label>Responsable<input v-model="draft.ownerEmail" type="email" placeholder="asesor@empresa.com"></label><label>Etiquetas<input v-model="draft.tags" placeholder="web, prioritario, honduras"></label></div><label>Notas<textarea v-model="draft.notes" rows="5" placeholder="Necesidades, contexto, próximos pasos…"></textarea></label></section>
          </template>

          <template v-else-if="editor==='company'">
            <section class="modal-section"><h3>Información de la empresa</h3><label>Nombre<input v-model="draft.name" required></label><div class="form-grid two"><label>Dominio<input v-model="draft.domain" placeholder="empresa.com"></label><label>Estado<select v-model="draft.status"><option value="PROSPECT">Prospecto</option><option value="ACTIVE">Activa</option><option value="CUSTOMER">Cliente</option><option value="INACTIVE">Inactiva</option></select></label><label>Correo<input v-model="draft.email" type="email"></label><label>Teléfono<input v-model="draft.phone"></label><label>Ciudad<input v-model="draft.city"></label><label>País<input v-model="draft.country"></label></div><label>Dirección<input v-model="draft.address"></label><label>Notas<textarea v-model="draft.notes" rows="5"></textarea></label></section>
          </template>

          <template v-else-if="editor==='deal'">
            <section class="modal-section"><h3>Oportunidad</h3><label>Título<input v-model="draft.title" required placeholder="Ej. Sitio corporativo para Empresa X"></label><div class="form-grid two"><label>Valor<input v-model.number="draft.value" type="number" min="0" step="0.01"></label><label>Moneda<select v-model="draft.currency"><option>HNL</option><option>USD</option><option>EUR</option></select></label><label>Etapa<select v-model="draft.stageId" required><option v-for="item in stages" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>Cierre esperado<input v-model="draft.expectedCloseDate" type="date"></label><label>Contacto<select v-model="draft.contactId"><option value="">Sin contacto</option><option v-for="item in contacts" :key="item.id" :value="item.id">{{ contactName(item) }}</option></select></label><label>Empresa<select v-model="draft.companyId"><option value="">Sin empresa</option><option v-for="item in companies" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>Responsable<input v-model="draft.ownerEmail" type="email"></label></div><label>Descripción<textarea v-model="draft.description" rows="5" placeholder="Alcance, necesidades y próximos pasos."></textarea></label></section>
          </template>

          <template v-else-if="editor==='activity'">
            <section class="modal-section"><h3>Seguimiento</h3><div class="form-grid two"><label>Tipo<select v-model="draft.type"><option value="TASK">Tarea</option><option value="CALL">Llamada</option><option value="EMAIL">Correo</option><option value="MEETING">Reunión</option><option value="NOTE">Nota</option></select></label><label>Vencimiento<input v-model="draft.dueAt" type="datetime-local"></label></div><label>Asunto<input v-model="draft.subject" required></label><label>Descripción<textarea v-model="draft.description" rows="5"></textarea></label><div class="form-grid two"><label>Asignada a<input v-model="draft.assignedTo" type="email"></label><label>Contacto<select v-model="draft.contactId"><option value="">Sin contacto</option><option v-for="item in contacts" :key="item.id" :value="item.id">{{ contactName(item) }}</option></select></label><label>Empresa<select v-model="draft.companyId"><option value="">Sin empresa</option><option v-for="item in companies" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>Negocio<select v-model="draft.dealId"><option value="">Sin negocio</option><option v-for="item in deals" :key="item.id" :value="item.id">{{ item.title }}</option></select></label></div><label class="switch-row"><input v-model="draft.completed" type="checkbox"><span><strong>Actividad completada</strong><small>Marque la actividad como realizada.</small></span></label></section>
          </template>

          <template v-else-if="editor==='stage'">
            <section class="modal-section"><h3>Etapa del pipeline</h3><label>Nombre<input v-model="draft.name" required></label><div class="form-grid three"><label>Orden<input v-model.number="draft.position" type="number"></label><label>Probabilidad<input v-model.number="draft.probability" type="number" min="0" max="100"></label><label>Color<input v-model="draft.color" type="color"></label></div><div class="form-grid two"><label class="switch-row"><input v-model="draft.closed" type="checkbox"><span><strong>Etapa cerrada</strong><small>No forma parte del pipeline abierto.</small></span></label><label class="switch-row"><input v-model="draft.won" type="checkbox"><span><strong>Etapa ganada</strong><small>Representa un cierre exitoso.</small></span></label></div></section>
          </template>

          <template v-else>
            <section class="modal-section"><div class="form-grid two"><label>Identificador<input v-model="draft.formKey" required pattern="[a-z0-9][a-z0-9-]{2,119}" placeholder="solicitud-web"></label><label>Nombre<input v-model="draft.name" required></label></div><label>Descripción<textarea v-model="draft.description" rows="3"></textarea></label><label>Mensaje de éxito<input v-model="draft.successMessage" required></label><div class="form-grid two"><label>Correos para notificaciones<input v-model="draft.notifyEmails" placeholder="ventas@empresa.com"></label><label>Texto del botón<input v-model="draft.settings.submitLabel" placeholder="Enviar"></label></div><div class="form-grid two"><label class="switch-row"><input v-model="draft.active" type="checkbox"><span><strong>Formulario activo</strong><small>Puede recibir envíos desde la web.</small></span></label><label class="switch-row"><input v-model="draft.settings.createLead" type="checkbox"><span><strong>Crear contacto automáticamente</strong><small>Convierte cada envío en un lead.</small></span></label><label class="switch-row"><input v-model="draft.settings.createDeal" type="checkbox"><span><strong>Crear oportunidad</strong><small>Abre un negocio en la primera etapa del pipeline.</small></span></label><label class="switch-row"><input v-model="draft.settings.createFollowUp" type="checkbox"><span><strong>Programar seguimiento</strong><small>Crea una tarea para atender el lead en 24 horas.</small></span></label></div></section>
            <section class="modal-section"><div class="modal-section__heading"><div><h3>Campos del formulario</h3><p>Defina el orden, tipo y obligatoriedad.</p></div><button type="button" class="button secondary small" @click="addField"><UiIcon name="plus" :size="15"/>Agregar campo</button></div><div class="crm-field-builder"><article v-for="(field,index) in draft.fields" :key="`${field.name}-${index}`"><header><span><UiIcon name="drag" :size="17"/>Campo {{ index + 1 }}</span><div><button type="button" :disabled="index===0" @click="moveField(index,-1)">↑</button><button type="button" :disabled="index===draft.fields.length-1" @click="moveField(index,1)">↓</button><button type="button" @click="duplicateField(index)"><UiIcon name="copy" :size="15"/></button><button type="button" class="danger" @click="removeField(index)"><UiIcon name="trash" :size="15"/></button></div></header><div class="form-grid two"><label>Nombre técnico<input v-model="field.name" required pattern="[a-z][a-z0-9_]{1,79}"></label><label>Etiqueta<input v-model="field.label" required></label><label>Tipo<select v-model="field.type"><option value="text">Texto</option><option value="email">Correo</option><option value="phone">Teléfono</option><option value="textarea">Texto largo</option><option value="number">Número</option><option value="select">Lista</option><option value="checkbox">Casilla</option><option value="date">Fecha</option></select></label><label>Placeholder<input v-model="field.placeholder"></label><label v-if="field.type==='select'" class="full">Opciones separadas por coma<input :value="field.options.join(', ')" @input="changeFieldOptions(field,$event)"></label></div><label class="compact-check"><input v-model="field.required" type="checkbox">Campo obligatorio</label></article></div></section>
          </template>
        </div>
        <footer class="modal-footer"><button type="button" class="button secondary" @click="editor=null">Cancelar</button><button class="button" :disabled="saving"><UiIcon name="save" :size="16"/>{{ saving ? 'Guardando…' : 'Guardar' }}</button></footer>
      </form>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="selectedSubmission" class="modal-backdrop" @click.self="selectedSubmission=null">
      <aside class="modal-card compact-modal submission-modal">
        <header class="modal-header"><div><span class="eyebrow">Lead recibido</span><h2>{{ selectedSubmission.formName }}</h2><p>{{ new Date(selectedSubmission.createdAt).toLocaleString() }}</p></div><button class="icon-button" @click="selectedSubmission=null"><UiIcon name="close" :size="18"/></button></header>
        <div class="modal-scroll"><div class="submission-source"><strong>Origen</strong><a v-if="selectedSubmission.sourceUrl" :href="selectedSubmission.sourceUrl" target="_blank">{{ selectedSubmission.sourceUrl }}</a><span v-else>No registrado</span></div><dl class="submission-values"><div v-for="(value,key) in selectedSubmission.values" :key="key"><dt>{{ String(key).replaceAll('_',' ') }}</dt><dd>{{ value }}</dd></div></dl></div>
        <footer class="modal-footer"><select :value="selectedSubmission.status" class="control-select" @change="changeSubmissionStatus(selectedSubmission,$event)"><option value="NEW">Nuevo</option><option value="READ">Leído</option><option value="ARCHIVED">Archivado</option><option value="SPAM">Spam</option></select><button class="button" @click="selectedSubmission=null">Cerrar</button></footer>
      </aside>
    </div>
  </Teleport>
</template>
