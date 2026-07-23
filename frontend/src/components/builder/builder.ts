import type { BuilderBlock, BuilderBlockType, BuilderData, BuilderDocument } from '@/types'

export interface BlockDefinition {
  type: BuilderBlockType
  label: string
  description: string
  category: string
  icon: string
  pluginDefinition?: import('@/types').PluginBuilderBlockDefinition
}

export const BLOCK_LIBRARY: BlockDefinition[] = [
  { type: 'section', label: 'Sección', description: 'Contenedor de ancho completo', category: 'Estructura', icon: 'layers' },
  { type: 'columns', label: 'Columnas', description: 'Diseño de 2 a 4 columnas', category: 'Estructura', icon: 'grid' },
  { type: 'heading', label: 'Encabezado', description: 'Títulos H1–H4', category: 'Contenido', icon: 'post' },
  { type: 'richText', label: 'Texto enriquecido', description: 'Contenido HTML profesional', category: 'Contenido', icon: 'document' },
  { type: 'quote', label: 'Cita', description: 'Testimonio o frase destacada', category: 'Contenido', icon: 'post' },
  { type: 'image', label: 'Imagen', description: 'Imagen adaptable con enlace', category: 'Medios', icon: 'image' },
  { type: 'gallery', label: 'Galería', description: 'Cuadrícula de imágenes', category: 'Medios', icon: 'grid' },
  { type: 'video', label: 'Video', description: 'Video HTML5 con Plyr', category: 'Medios', icon: 'video' },
  { type: 'audio', label: 'Audio', description: 'Audio con reproductor Plyr', category: 'Medios', icon: 'audio' },
  { type: 'button', label: 'Botón', description: 'Llamada a la acción', category: 'Marketing', icon: 'external' },
  { type: 'iconBox', label: 'Caja de beneficio', description: 'Icono, título y texto', category: 'Marketing', icon: 'check' },
  { type: 'stats', label: 'Estadísticas', description: 'Indicadores y métricas', category: 'Marketing', icon: 'dashboard' },
  { type: 'divider', label: 'Separador', description: 'Línea horizontal', category: 'Avanzado', icon: 'more' },
  { type: 'spacer', label: 'Espaciador', description: 'Separación vertical', category: 'Avanzado', icon: 'more' },
  { type: 'html', label: 'HTML', description: 'Código HTML sanitizado', category: 'Avanzado', icon: 'code' }
]

export function emptyDocument(): BuilderDocument {
  return { version: 1, blocks: [], global: { containerWidth: 1200, gap: 24 } }
}

function id(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `block-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function createBlock(type: BuilderBlockType): BuilderBlock {
  const definition = BLOCK_LIBRARY.find(item => item.type === type)
  const base: BuilderBlock = {
    id: id(),
    type,
    label: definition?.label || type,
    data: {},
    style: {
      paddingTop: type === 'section' ? 64 : 12,
      paddingRight: type === 'section' ? 24 : 12,
      paddingBottom: type === 'section' ? 64 : 12,
      paddingLeft: type === 'section' ? 24 : 12,
      textAlign: 'left'
    }
  }

  switch (type) {
    case 'section':
      base.data = { width: 'boxed', verticalAlign: 'center' }
      base.children = []
      break
    case 'columns':
      base.data = { columns: 2, gap: 24, verticalAlign: 'start' }
      base.children = []
      break
    case 'heading':
      base.data = { text: 'Un título que comunica valor', level: 2, subtitle: '' }
      break
    case 'richText':
      base.data = { html: '<p>Escriba aquí su contenido. Puede agregar enlaces, listas, imágenes y formatos.</p>' }
      break
    case 'quote':
      base.data = { quote: 'Una experiencia excelente para nuestros clientes.', author: 'Nombre del cliente', role: 'Empresa' }
      break
    case 'image':
      base.data = { src: '', alt: '', caption: '', objectFit: 'cover', height: 420, link: '' }
      break
    case 'gallery':
      base.data = { images: [], columns: 3, gap: 16, lightbox: true }
      break
    case 'video':
      base.data = { src: '', contentType: 'video/mp4', poster: '', title: '' }
      break
    case 'audio':
      base.data = { src: '', contentType: 'audio/mpeg', title: '' }
      break
    case 'button':
      base.data = { text: 'Comenzar ahora', href: '#', target: '_self', variant: 'primary', size: 'medium' }
      break
    case 'iconBox':
      base.data = { icon: '✓', title: 'Beneficio principal', text: 'Explique de forma clara cómo ayuda su producto o servicio.' }
      break
    case 'stats':
      base.data = { items: [{ value: '10+', label: 'Años de experiencia' }, { value: '250', label: 'Clientes' }, { value: '99%', label: 'Satisfacción' }] }
      break
    case 'divider':
      base.data = { width: 100, thickness: 1, style: 'solid' }
      break
    case 'spacer':
      base.data = { height: 64 }
      break
    case 'html':
      base.data = { html: '<div class="custom-block">HTML personalizado</div>' }
      break
  }
  return base
}

export function createPluginBlock(definition: import('@/types').PluginBuilderBlockDefinition): BuilderBlock {
  const data: BuilderData = {
    _pluginId: definition.pluginId,
    _pluginType: definition.type,
    _template: definition.template || '<div class="plugin-block"><strong>{{title}}</strong></div>',
    _schema: definition.schema || {}
  }
  const schema: Record<string, import('@/types').PluginFieldDefinition> = definition.schema ?? {}
  Object.entries(schema).forEach(([key, field]) => {
    data[key] = field.default ?? ''
  })
  return {
    id: id(),
    type: `plugin:${definition.pluginId}:${definition.type}`,
    label: definition.label,
    data,
    style: { paddingTop: 12, paddingRight: 12, paddingBottom: 12, paddingLeft: 12, textAlign: 'left' }
  }
}

export function parseDocument(value?: string | null): BuilderDocument {
  if (!value) return emptyDocument()
  try {
    const parsed = JSON.parse(value) as BuilderDocument
    if (!parsed || parsed.version !== 1 || !Array.isArray(parsed.blocks)) return emptyDocument()
    return {
      version: 1,
      blocks: parsed.blocks,
      global: {
        containerWidth: Number(parsed.global?.containerWidth) || 1200,
        gap: Number(parsed.global?.gap) || 24
      }
    }
  } catch {
    return emptyDocument()
  }
}

export function cloneBlock(block: BuilderBlock): BuilderBlock {
  const copy = structuredClone(block)
  const refresh = (item: BuilderBlock) => {
    item.id = id()
    item.children?.forEach(refresh)
  }
  refresh(copy)
  return copy
}

export function walkBlocks(blocks: BuilderBlock[], visitor: (block: BuilderBlock, parent: BuilderBlock | null) => void, parent: BuilderBlock | null = null) {
  blocks.forEach(block => {
    visitor(block, parent)
    if (block.children) walkBlocks(block.children, visitor, block)
  })
}
