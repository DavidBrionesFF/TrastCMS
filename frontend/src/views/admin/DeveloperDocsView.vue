<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, ApiError } from '@/services/api'
import { useToastStore } from '@/stores/toast'
import UiIcon from '@/components/common/UiIcon.vue'
import SkeletonBlock from '@/components/common/SkeletonBlock.vue'

type Tool = {
  name: string
  title: string
  description: string
  annotations?: Record<string, boolean>
}

type Resource = {
  uri?: string
  uriTemplate?: string
  name: string
  description: string
}

type Prompt = {
  name: string
  title: string
  description: string
}

type Platform = {
  rest: {
    openApi: string
    swaggerUi: string
    documentation: string
  }
  mcp: {
    enabled: boolean
    configured: boolean
    endpoint: string
    absoluteEndpoint: string
    protocolVersion: string
    serverVersion: string
    tokenLength: number
    tools: Tool[]
    resources: Resource[]
    resourceTemplates: Resource[]
    prompts: Prompt[]
  }
}

type McpTest = {
  ok: boolean
  enabled: boolean
  configured: boolean
  endpoint: string
  tools: number
  resources: number
  message: string
}

const toast = useToastStore()
const platform = ref<Platform | null>(null)
const loading = ref(true)
const testing = ref(false)
const testResult = ref<McpTest | null>(null)
const search = ref('')
const copied = ref('')

const filteredTools = computed(() => {
  const term = search.value.trim().toLowerCase()
  return (platform.value?.mcp.tools || []).filter(
    tool =>
      !term ||
      `${tool.name} ${tool.title} ${tool.description}`
        .toLowerCase()
        .includes(term)
  )
})

const curlExample = computed(() => {
  const endpoint =
    platform.value?.mcp.absoluteEndpoint ||
    'http://localhost:8080/mcp'
  return `curl -X POST "${endpoint}" \\
  -H "Authorization: Bearer SU_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"trastcms-test","version":"1.0.0"}}}'`
})

async function load(): Promise<void> {
  loading.value = true
  try {
    platform.value = await api<Platform>('/api/admin/developer')
  } catch (caught: unknown) {
    toast.error(
      'No se pudo cargar la plataforma',
      caught instanceof ApiError ? caught.message : undefined
    )
  } finally {
    loading.value = false
  }
}

async function testMcp(): Promise<void> {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await api<McpTest>(
      '/api/admin/developer/mcp/test',
      { method: 'POST' }
    )
    if (testResult.value.ok) {
      toast.success(
        'MCP funciona dentro del servidor',
        `${testResult.value.tools} herramientas registradas.`
      )
    }
  } catch (caught: unknown) {
    toast.error(
      'La prueba MCP falló',
      caught instanceof ApiError ? caught.message : undefined
    )
  } finally {
    testing.value = false
  }
}

async function copy(value: string, label: string): Promise<void> {
  try {
    await window.navigator.clipboard.writeText(value)
    copied.value = label
    window.setTimeout(() => {
      if (copied.value === label) copied.value = ''
    }, 1600)
  } catch {
    toast.warning(
      'No se pudo copiar automáticamente',
      'Seleccione el valor y cópielo manualmente.'
    )
  }
}

onMounted(load)
</script>

<template>
  <div class="developer-admin-page">
    <header class="developer-admin-hero">
      <div>
        <span class="eyebrow">INTEGRACIONES</span>
        <h1>API y MCP</h1>
        <p>
          Conecte TrastCMS con aplicaciones, automatizaciones y agentes de IA
          mediante contratos documentados y verificables.
        </p>
      </div>
      <div class="developer-admin-actions">
        <a class="button secondary" href="/developers" target="_blank">
          <UiIcon name="external" :size="16" /> Portal público
        </a>
        <a class="button" href="/swagger-ui/index.html" target="_blank">
          <UiIcon name="code" :size="16" /> Abrir Swagger
        </a>
      </div>
    </header>

    <div v-if="loading" class="developer-status-grid">
      <article v-for="item in 4" :key="item">
        <SkeletonBlock :lines="3" />
      </article>
    </div>

    <template v-else-if="platform">
      <section class="developer-status-grid">
        <article>
          <span>REST API</span>
          <strong>Disponible</strong>
          <small>OpenAPI generado</small>
        </article>
        <article>
          <span>MCP Server</span>
          <strong>{{ platform.mcp.enabled ? 'Habilitado' : 'Deshabilitado' }}</strong>
          <small>{{ platform.mcp.protocolVersion }}</small>
        </article>
        <article>
          <span>Token MCP</span>
          <strong>{{ platform.mcp.configured ? 'Configurado' : 'Pendiente' }}</strong>
          <small>
            {{ platform.mcp.configured
              ? `${platform.mcp.tokenLength} caracteres`
              : 'TRASTCMS_MCP_TOKEN' }}
          </small>
        </article>
        <article>
          <span>Herramientas</span>
          <strong>{{ platform.mcp.tools.length }}</strong>
          <small>Núcleo + plugins activos</small>
        </article>
      </section>

      <section class="developer-admin-grid">
        <article class="panel developer-endpoint-card">
          <header>
            <div>
              <span class="eyebrow">REST</span>
              <h2>OpenAPI y Swagger UI</h2>
            </div>
            <span class="status-pill success">ACTIVO</span>
          </header>
          <p>
            Explore controladores Spring, parámetros, respuestas y esquemas
            generados por la aplicación.
          </p>
          <label>
            Documento OpenAPI
            <div>
              <code>{{ platform.rest.openApi }}</code>
              <button @click="copy(platform.rest.openApi, 'openapi')">
                {{ copied === 'openapi' ? 'Copiado' : 'Copiar' }}
              </button>
            </div>
          </label>
          <label>
            Swagger UI
            <div>
              <code>{{ platform.rest.swaggerUi }}</code>
              <button @click="copy(platform.rest.swaggerUi, 'swagger')">
                {{ copied === 'swagger' ? 'Copiado' : 'Copiar' }}
              </button>
            </div>
          </label>
        </article>

        <article class="panel developer-endpoint-card">
          <header>
            <div>
              <span class="eyebrow">MCP</span>
              <h2>Streamable HTTP</h2>
            </div>
            <span
              :class="[
                'status-pill',
                platform.mcp.configured ? 'success' : 'warning'
              ]"
            >
              {{ platform.mcp.configured ? 'LISTO' : 'CONFIGURAR TOKEN' }}
            </span>
          </header>

          <p>
            Endpoint JSON-RPC 2.0 para clientes compatibles con Model Context
            Protocol. Admite Bearer token y el encabezado alternativo
            <code>X-MCP-Token</code>.
          </p>

          <label>
            Endpoint absoluto
            <div>
              <code>{{ platform.mcp.absoluteEndpoint }}</code>
              <button @click="copy(platform.mcp.absoluteEndpoint, 'mcp')">
                {{ copied === 'mcp' ? 'Copiado' : 'Copiar' }}
              </button>
            </div>
          </label>

          <div class="mcp-diagnostic-card">
            <dl>
              <div>
                <dt>Servidor</dt>
                <dd>{{ platform.mcp.serverVersion }}</dd>
              </div>
              <div>
                <dt>Variable</dt>
                <dd><code>TRASTCMS_MCP_TOKEN</code></dd>
              </div>
              <div>
                <dt>Archivo local</dt>
                <dd><code>.env</code> en la raíz</dd>
              </div>
            </dl>

            <button
              class="button"
              :disabled="testing || !platform.mcp.enabled"
              @click="testMcp"
            >
              {{ testing ? 'Probando…' : 'Probar servidor MCP' }}
            </button>

            <div v-if="testResult" class="mcp-test-result">
              <b>{{ testResult.message }}</b>
              <span>
                {{ testResult.tools }} herramientas ·
                {{ testResult.resources }} recursos
              </span>
            </div>
          </div>
        </article>
      </section>

      <section class="panel mt-6">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">PRUEBA EXTERNA</span>
            <h2>Solicitud de inicialización</h2>
            <p>
              Reinicie la aplicación después de cambiar <code>.env</code>.
              El archivo se carga automáticamente desde la raíz del proyecto.
            </p>
          </div>
          <button
            class="button secondary"
            @click="copy(curlExample, 'curl')"
          >
            {{ copied === 'curl' ? 'Copiado' : 'Copiar cURL' }}
          </button>
        </div>
        <pre class="code-block">{{ curlExample }}</pre>
      </section>

      <section class="panel mt-6">
        <div class="developer-catalog-header">
          <div>
            <span class="eyebrow">CATÁLOGO MCP</span>
            <h2>Herramientas registradas</h2>
            <p>
              El catálogo combina funciones del núcleo y contribuciones de
              plugins incorporados que estén activos.
            </p>
          </div>
          <div class="search-field">
            <UiIcon name="search" :size="17" />
            <input v-model="search" placeholder="Buscar herramienta">
          </div>
        </div>

        <div class="mcp-tool-table">
          <article v-for="tool in filteredTools" :key="tool.name">
            <div>
              <code>{{ tool.name }}</code>
              <strong>{{ tool.title }}</strong>
              <p>{{ tool.description }}</p>
            </div>
            <span
              :class="[
                'status-pill',
                tool.annotations?.readOnlyHint ? 'info' : 'warning'
              ]"
            >
              {{ tool.annotations?.readOnlyHint ? 'LECTURA' : 'ESCRITURA' }}
            </span>
          </article>
        </div>
      </section>

      <section class="developer-admin-grid mt-6">
        <article class="panel">
          <span class="eyebrow">RECURSOS</span>
          <h2 class="mt-2 text-xl font-bold">Contexto navegable</h2>
          <div class="developer-mini-list">
            <div
              v-for="resource in [
                ...platform.mcp.resources,
                ...platform.mcp.resourceTemplates
              ]"
              :key="resource.uri || resource.uriTemplate"
            >
              <code>{{ resource.uri || resource.uriTemplate }}</code>
              <span>{{ resource.name }}</span>
            </div>
          </div>
        </article>

        <article class="panel">
          <span class="eyebrow">PROMPTS</span>
          <h2 class="mt-2 text-xl font-bold">Plantillas reutilizables</h2>
          <div class="developer-mini-list">
            <div
              v-for="prompt in platform.mcp.prompts"
              :key="prompt.name"
            >
              <code>{{ prompt.name }}</code>
              <span>{{ prompt.title }}</span>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>
