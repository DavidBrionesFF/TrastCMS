<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import UiIcon from '@/components/common/UiIcon.vue'

type Tool = { name:string; title:string; description:string; annotations?:Record<string,boolean> }
type Resource = { uri?:string; uriTemplate?:string; name:string; description:string }
type Prompt = { name:string; title:string; description:string }
type Platform = {
  rest:{ openApi:string; swaggerUi:string; documentation:string }
  mcp:{ enabled:boolean; configured:boolean; endpoint:string; protocolVersion:string; tools:Tool[]; resources:Resource[]; resourceTemplates:Resource[]; prompts:Prompt[] }
}

const platform = ref<Platform | null>(null)
const loading = ref(true)
const search = ref('')
const copied = ref('')

const filteredTools = computed(() => {
  const term = search.value.trim().toLowerCase()
  return (platform.value?.mcp.tools || []).filter(tool => !term || `${tool.name} ${tool.title} ${tool.description}`.toLowerCase().includes(term))
})

onMounted(async () => {
  platform.value = await api<Platform>('/api/admin/developer')
  loading.value = false
})

async function copy(value:string,label:string){await navigator.clipboard.writeText(value);copied.value=label;setTimeout(()=>copied.value='',1600)}
</script>

<template>
  <div class="developer-admin-page">
    <header class="developer-admin-hero">
      <div><span class="eyebrow">INTEGRACIONES</span><h1>API y MCP</h1><p>Conecte TrastCMS con aplicaciones, automatizaciones y agentes de IA mediante contratos documentados y seguros.</p></div>
      <div class="developer-admin-actions"><a class="button secondary" href="/developers" target="_blank"><UiIcon name="external" :size="16"/>Portal público</a><a class="button" href="/swagger-ui/index.html" target="_blank"><UiIcon name="code" :size="16"/>Abrir Swagger</a></div>
    </header>

    <div v-if="loading" class="panel empty">Cargando plataforma para desarrolladores…</div>
    <template v-else-if="platform">
      <section class="developer-status-grid">
        <article><span>REST API</span><strong>Disponible</strong><small>OpenAPI generado</small></article>
        <article><span>MCP Server</span><strong>{{ platform.mcp.enabled ? 'Habilitado' : 'Deshabilitado' }}</strong><small>{{ platform.mcp.protocolVersion }}</small></article>
        <article><span>Autenticación MCP</span><strong>{{ platform.mcp.configured ? 'Configurada' : 'Pendiente' }}</strong><small>Bearer token</small></article>
        <article><span>Herramientas</span><strong>{{ platform.mcp.tools.length }}</strong><small>Núcleo + plugins</small></article>
      </section>

      <section class="developer-admin-grid">
        <article class="panel developer-endpoint-card">
          <header><div><span class="eyebrow">REST</span><h2>OpenAPI y Swagger UI</h2></div><span class="status-pill success">ACTIVO</span></header>
          <p>Explore todos los controladores Spring, parámetros, respuestas y esquemas generados por la aplicación.</p>
          <label>Documento OpenAPI<div><code>{{ platform.rest.openApi }}</code><button @click="copy(platform.rest.openApi,'openapi')">{{copied==='openapi'?'Copiado':'Copiar'}}</button></div></label>
          <label>Swagger UI<div><code>{{ platform.rest.swaggerUi }}</code><button @click="copy(platform.rest.swaggerUi,'swagger')">{{copied==='swagger'?'Copiado':'Copiar'}}</button></div></label>
        </article>

        <article class="panel developer-endpoint-card">
          <header><div><span class="eyebrow">MCP</span><h2>Streamable HTTP</h2></div><span :class="['status-pill',platform.mcp.configured?'success':'warning']">{{platform.mcp.configured?'LISTO':'CONFIGURAR TOKEN'}}</span></header>
          <p>Endpoint stateless JSON-RPC 2.0 para clientes compatibles con Model Context Protocol.</p>
          <label>Endpoint<div><code>{{ platform.mcp.endpoint }}</code><button @click="copy(platform.mcp.endpoint,'mcp')">{{copied==='mcp'?'Copiado':'Copiar'}}</button></div></label>
          <label>Variable de entorno<div><code>TRASTCMS_MCP_TOKEN</code><button @click="copy('TRASTCMS_MCP_TOKEN','token')">{{copied==='token'?'Copiado':'Copiar'}}</button></div></label>
        </article>
      </section>

      <section class="panel mt-6">
        <div class="developer-catalog-header"><div><span class="eyebrow">CATÁLOGO MCP</span><h2>Herramientas registradas</h2><p>El catálogo combina funciones del núcleo y contribuciones de plugins incorporados.</p></div><div class="search-field"><UiIcon name="search" :size="17"/><input v-model="search" placeholder="Buscar herramienta"></div></div>
        <div class="mcp-tool-table">
          <article v-for="tool in filteredTools" :key="tool.name"><div><code>{{tool.name}}</code><strong>{{tool.title}}</strong><p>{{tool.description}}</p></div><span :class="['status-pill',tool.annotations?.readOnlyHint?'info':'warning']">{{tool.annotations?.readOnlyHint?'LECTURA':'ESCRITURA'}}</span></article>
        </div>
      </section>

      <section class="developer-admin-grid mt-6">
        <article class="panel"><span class="eyebrow">RECURSOS</span><h2 class="mt-2 text-xl font-bold">Contexto navegable</h2><div class="developer-mini-list"><div v-for="resource in [...platform.mcp.resources,...platform.mcp.resourceTemplates]" :key="resource.uri||resource.uriTemplate"><code>{{resource.uri||resource.uriTemplate}}</code><span>{{resource.name}}</span></div></div></article>
        <article class="panel"><span class="eyebrow">PROMPTS</span><h2 class="mt-2 text-xl font-bold">Plantillas reutilizables</h2><div class="developer-mini-list"><div v-for="prompt in platform.mcp.prompts" :key="prompt.name"><code>{{prompt.name}}</code><span>{{prompt.title}}</span></div></div></article>
      </section>
    </template>
  </div>
</template>
