<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'

type Discovery = {
  name: string
  title: string
  endpoint: string
  transport: string
  protocolVersion: string
  authentication: { type: string; environmentVariable: string }
  documentation: string
}

const discovery = ref<Discovery | null>(null)
const copied = ref('')

onMounted(async () => {
  discovery.value = await api<Discovery>('/.well-known/mcp.json')
})

async function copy(text: string, label: string) {
  await navigator.clipboard.writeText(text)
  copied.value = label
  window.setTimeout(() => { copied.value = '' }, 1800)
}

const initializeExample = `curl -X POST http://localhost:8080/mcp \\
  -H "Authorization: Bearer $TRASTCMS_MCP_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "jsonrpc":"2.0",
    "id":1,
    "method":"initialize",
    "params":{
      "protocolVersion":"2025-11-25",
      "capabilities":{},
      "clientInfo":{"name":"mi-cliente","version":"1.0.0"}
    }
  }'`

const restExample = `curl http://localhost:8080/api/public/posts?size=12`
</script>

<template>
  <div class="developer-portal">
    <section class="developer-hero">
      <div class="developer-hero__copy">
        <span class="developer-kicker">PLATAFORMA PARA DESARROLLADORES</span>
        <h1>Construya sobre TrastCMS con REST y MCP</h1>
        <p>Integre aplicaciones, agentes de IA, automatizaciones y plugins sin salir del mismo JAR. La API REST sirve a la web y el servidor MCP expone herramientas, recursos y prompts mediante JSON-RPC 2.0.</p>
        <div class="developer-hero__actions">
          <a href="#rest" class="button">Explorar REST API</a>
          <a href="#mcp" class="button secondary">Configurar MCP</a>
        </div>
        <div class="developer-proof-row">
          <span>OpenAPI 3</span><span>Streamable HTTP</span><span>Bearer token</span><span>Plugins extensibles</span>
        </div>
      </div>
      <div class="developer-terminal" aria-label="Ejemplo de integración">
        <div class="developer-terminal__bar"><i></i><i></i><i></i><span>trastcms / developer</span></div>
        <pre><code><span class="term-muted"># Descubra el servidor</span>
GET /.well-known/mcp.json

<span class="term-success">200 OK</span>
{
  "transport": "streamable-http",
  "endpoint": "/mcp",
  "protocolVersion": "2025-11-25"
}</code></pre>
      </div>
    </section>

    <section class="developer-metrics">
      <article><strong>1 JAR</strong><span>Backend, frontend, REST y MCP</span></article>
      <article><strong>JSON-RPC 2.0</strong><span>Protocolo MCP estándar</span></article>
      <article><strong>OpenAPI</strong><span>Contrato REST navegable</span></article>
      <article><strong>Plugin-aware</strong><span>TrastCRM aporta herramientas MCP</span></article>
    </section>

    <section id="rest" class="developer-section">
      <div class="developer-section__heading"><span class="developer-kicker">REST API</span><h2>Una API clara para contenido, medios y administración</h2><p>Los endpoints públicos no requieren sesión. La administración utiliza sesión segura y CSRF. OpenAPI se genera automáticamente desde Spring MVC.</p></div>
      <div class="developer-feature-grid">
        <article><b>Contenido público</b><code>/api/public/posts</code><p>Publicaciones, páginas, navegación, categorías, menús y temas.</p></article>
        <article><b>Administración</b><code>/api/admin/*</code><p>Contenido, usuarios, configuración, medios, temas, plugins y CRM.</p></article>
        <article><b>OpenAPI</b><code>/v3/api-docs</code><p>Contrato JSON generado directamente desde el backend.</p></article>
        <article><b>Swagger UI</b><code>/swagger-ui/index.html</code><p>Consola interactiva disponible para administradores.</p></article>
      </div>
      <div class="developer-code-card">
        <header><span>REST · publicaciones públicas</span><button @click="copy(restExample, 'rest')">{{ copied === 'rest' ? 'Copiado' : 'Copiar' }}</button></header>
        <pre><code>{{ restExample }}</code></pre>
      </div>
    </section>

    <section id="mcp" class="developer-section developer-section--mcp">
      <div class="developer-section__heading"><span class="developer-kicker">MODEL CONTEXT PROTOCOL</span><h2>Conecte asistentes y agentes directamente al CMS</h2><p>TrastCMS implementa MCP mediante Streamable HTTP. El servidor es stateless, usa un token Bearer independiente y permite que el núcleo y los plugins registren herramientas.</p></div>
      <div class="mcp-architecture">
        <article><span>01</span><b>Cliente MCP</b><p>Claude, ChatGPT, IDE o un agente propio.</p></article>
        <i>→</i>
        <article><span>02</span><b>POST {{ discovery?.endpoint || '/mcp' }}</b><p>JSON-RPC 2.0 y autenticación Bearer.</p></article>
        <i>→</i>
        <article><span>03</span><b>TrastCMS</b><p>Contenido, sitio, categorías y plugins.</p></article>
      </div>
      <div class="developer-code-card developer-code-card--dark">
        <header><span>MCP · initialize</span><button @click="copy(initializeExample, 'mcp')">{{ copied === 'mcp' ? 'Copiado' : 'Copiar' }}</button></header>
        <pre><code>{{ initializeExample }}</code></pre>
      </div>
      <div class="developer-callout">
        <div><strong>Configuración requerida</strong><p>Defina <code>TRASTCMS_MCP_TOKEN</code> antes de iniciar. El token no se expone en la interfaz ni en el documento de descubrimiento.</p></div>
        <code>{{ discovery?.authentication.environmentVariable || 'TRASTCMS_MCP_TOKEN' }}</code>
      </div>
    </section>

    <section class="developer-section">
      <div class="developer-section__heading"><span class="developer-kicker">CAPACIDADES</span><h2>Herramientas listas para agentes y automatizaciones</h2></div>
      <div class="tool-catalog-public">
        <article><span>CORE</span><b>site_overview</b><p>Identidad, tema y métricas del sitio.</p></article>
        <article><span>CORE</span><b>content_list</b><p>Lista páginas y publicaciones administrables.</p></article>
        <article><span>CORE</span><b>content_create_draft</b><p>Crea borradores sin publicar automáticamente.</p></article>
        <article><span>TRASTCRM</span><b>crm_pipeline_summary</b><p>Pipeline, valor abierto y actividad comercial.</p></article>
        <article><span>TRASTCRM</span><b>crm_list_leads</b><p>Consulta leads por estado y búsqueda.</p></article>
        <article><span>RESOURCE</span><b>trastcms://page/{slug}</b><p>Expone páginas publicadas como contexto.</p></article>
      </div>
    </section>
  </div>
</template>
