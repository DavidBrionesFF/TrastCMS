export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type ContentType = 'POST' | 'PAGE'
export type EditorMode = 'RICH_TEXT' | 'VISUAL_BUILDER'
export type MediaKind = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'OTHER'

export interface AuthUser {
  authenticated: boolean
  email: string | null
  displayName: string | null
  role: string | null
  avatarUrl?: string | null
  lastLoginAt?: string | null
}

export interface AccountProfile {
  id: string
  email: string
  displayName: string
  firstName?: string | null
  lastName?: string | null
  phone?: string | null
  avatarUrl?: string | null
  bio?: string | null
  locale: string
  timezone: string
  role: UserRole
  enabled: boolean
  lastLoginAt?: string | null
  createdAt: string
  updatedAt: string
}

export type UserRole = 'ADMIN' | 'EDITOR' | 'AUTHOR'
export interface CmsUser {
  id: string
  email: string
  displayName: string
  role: UserRole
  enabled: boolean
  avatarUrl?: string | null
  lastLoginAt?: string | null
  contentCount: number
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: string
  name: string
  slug: string
  description?: string
  createdAt: string
  updatedAt: string
}

export interface PostSummary {
  id: string
  title: string
  slug: string
  excerpt?: string
  contentType: ContentType
  editorMode: EditorMode
  status: PostStatus
  showInMenu: boolean
  menuOrder: number
  pageRole?: string | null
  themeOrigin?: string | null
  featuredImageUrl?: string
  category?: Category
  authorName: string
  publishedAt?: string
  updatedAt: string
}

export interface Post extends PostSummary {
  body: string
  builderData?: string | null
  customCss?: string | null
  seoTitle?: string | null
  seoDescription?: string | null
  template?: string | null
  showInMenu: boolean
  menuOrder: number
  pageRole?: string | null
  themeOrigin?: string | null
  createdAt: string
  version: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface SiteInfo {
  name: string
  tagline: string
  description: string
  locale: string
  activeTheme: string
  logoUrl: string
  homePageSlug: string
  showAdminBar: boolean
  contactEmail: string
  contactPhone: string
}

export interface NavigationItem {
  title: string
  slug: string
  order: number
  role?: string | null
}

export type ThemeMenuItemType = 'PAGE' | 'CUSTOM'

export interface ThemeMenuItem {
  id: string
  label: string
  type: ThemeMenuItemType
  target: string
  url: string
  visible: boolean
  newTab: boolean
  order: number
}

export interface ThemeMenus {
  header: ThemeMenuItem[]
  footer: ThemeMenuItem[]
}

export interface Dashboard {
  posts: number
  pages: number
  published: number
  drafts: number
  categories: number
  media: number
  users: number
}

export interface MediaAsset {
  id: string
  filename: string
  originalFilename: string
  title?: string
  altText?: string
  caption?: string
  description?: string
  kind: MediaKind
  folder: string
  contentType: string
  sizeBytes: number
  width?: number
  height?: number
  durationSeconds?: number
  publicUrl: string
  uploadedBy: string
  createdAt: string
  updatedAt: string
}

export interface ThemeSettingDefinition {
  type: 'text' | 'textarea' | 'color' | 'number' | 'select' | 'boolean'
  label: string
  description?: string
  default?: string | number | boolean
  options?: string[] | Array<{ label: string; value: string }>
  min?: number
  max?: number
  step?: number
}

export interface Theme {
  id: string
  name: string
  description: string
  version: string
  author?: string
  homepage?: string
  license?: string
  active: boolean
  custom: boolean
  stylesheetUrl: string
  screenshotUrl?: string
  templates: string[]
  features: string[]
  settingsSchema: Record<string, ThemeSettingDefinition>
}

export interface ThemeSettings {
  themeId: string
  values: Record<string, string>
}

export interface Plugin {
  id: string
  pluginKey: string
  name: string
  version: string
  description?: string
  author?: string
  homepage?: string
  baseUrl: string
  subscriptions: string[]
  permissions: string[]
  healthCheckPath?: string
  enabled: boolean
  lastTestStatus: 'NEVER' | 'HEALTHY' | 'FAILED' | string
  lastTestMessage?: string
  lastTestAt?: string
  createdAt: string
  updatedAt: string
}

export interface BundledPlugin {
  pluginId: string
  name: string
  version: string
  description: string
  author: string
  enabled: boolean
  enabledByDefault: boolean
  capabilities: string[]
  updatedAt?: string | null
}

export interface JavaPlugin {
  pluginId: string
  name: string
  version: string
  provider?: string
  description?: string
  state: 'CREATED' | 'DISABLED' | 'RESOLVED' | 'STARTED' | 'STOPPED' | string
  path: string
  dependencies: string[]
  capabilities: string[]
  deletable: boolean
  javaPluginsEnabled: boolean
}

export interface PluginCatalog {
  events: string[]
  permissions: string[]
  javaPluginsEnabled: boolean
  nativeImage: boolean
}

export interface PluginFieldDefinition {
  type: 'text' | 'textarea' | 'number' | 'color' | 'boolean' | 'select'
  label?: string
  default?: string | number | boolean
  options?: string[] | Array<{ label: string; value: string }>
  optionsEndpoint?: string
  optionsLabel?: string
  optionsValue?: string
}

export interface PluginBuilderBlockDefinition {
  type: string
  label: string
  description?: string
  category?: string
  icon?: string
  template?: string
  schema?: Record<string, PluginFieldDefinition>
  pluginId: string
  pluginVersion?: string
  extensionName?: string
}

export interface PluginAdminFieldDefinition {
  name: string
  label: string
  type: 'text' | 'textarea' | 'number' | 'color' | 'boolean' | 'select'
  description?: string
  placeholder?: string
  default?: string | number | boolean
  options?: string[] | Array<{ label: string; value: string }>
  required?: boolean
}

export interface PluginAdminSection {
  id?: string
  type: 'notice' | 'stats' | 'links' | 'form'
  title?: string
  description?: string
  tone?: 'info' | 'success' | 'warning' | 'danger'
  items?: Array<Record<string, unknown>>
  fields?: PluginAdminFieldDefinition[]
  action?: string
  submitLabel?: string
}

export interface PluginAdminMenuItem {
  id: string
  label: string
  icon?: string
  description?: string
  pluginId: string
  pluginVersion?: string
  extensionName?: string
  routeName?: string
  bundled?: boolean
  sections?: PluginAdminSection[]
}

export interface PluginContributions {
  blocks: PluginBuilderBlockDefinition[]
  adminMenuItems: PluginAdminMenuItem[]
}

export type BuilderBlockType =
  | 'section'
  | 'columns'
  | 'heading'
  | 'richText'
  | 'image'
  | 'gallery'
  | 'video'
  | 'audio'
  | 'button'
  | 'quote'
  | 'divider'
  | 'spacer'
  | 'html'
  | 'iconBox'
  | 'stats'
  | `plugin:${string}`

export interface BuilderStyle {
  backgroundColor?: string
  backgroundImage?: string
  color?: string
  textAlign?: 'left' | 'center' | 'right'
  paddingTop?: number
  paddingRight?: number
  paddingBottom?: number
  paddingLeft?: number
  marginTop?: number
  marginRight?: number
  marginBottom?: number
  marginLeft?: number
  borderRadius?: number
  maxWidth?: number
  minHeight?: number
  cssClass?: string
  anchorId?: string
}

export type BuilderData = Record<string, any>

export interface BuilderBlock {
  id: string
  type: BuilderBlockType
  label: string
  data: BuilderData
  style: BuilderStyle
  children?: BuilderBlock[]
}

export interface BuilderDocument {
  version: 1
  blocks: BuilderBlock[]
  global: {
    containerWidth: number
    gap: number
  }
}

export type CrmContactStatus = 'LEAD' | 'PROSPECT' | 'CUSTOMER' | 'INACTIVE'
export type CrmCompanyStatus = 'PROSPECT' | 'ACTIVE' | 'CUSTOMER' | 'INACTIVE'
export type CrmActivityType = 'TASK' | 'CALL' | 'EMAIL' | 'MEETING' | 'NOTE'
export type CrmSubmissionStatus = 'NEW' | 'READ' | 'ARCHIVED' | 'SPAM'

export interface CrmCompany { id:string; name:string; domain?:string; email?:string; phone?:string; address?:string; city?:string; country?:string; status:CrmCompanyStatus; notes?:string; createdAt:string; updatedAt:string }
export interface CrmContact { id:string; firstName:string; lastName?:string; email?:string; phone?:string; jobTitle?:string; status:CrmContactStatus; source?:string; ownerEmail?:string; tags?:string; notes?:string; company?:CrmCompany; createdAt:string; updatedAt:string }
export interface CrmStage { id:string; name:string; position:number; probability:number; color:string; closed:boolean; won:boolean }
export interface CrmDeal { id:string; title:string; value:number; currency:string; expectedCloseDate?:string; ownerEmail?:string; description?:string; contact?:CrmContact; company?:CrmCompany; stage:CrmStage; createdAt:string; updatedAt:string }
export interface CrmActivity { id:string; type:CrmActivityType; subject:string; description?:string; dueAt?:string; completedAt?:string; assignedTo?:string; contactId?:string; companyId?:string; dealId?:string; createdAt:string; updatedAt:string }
export interface CrmFormField { name:string; label:string; type:'text'|'email'|'phone'|'textarea'|'number'|'select'|'checkbox'|'date'; required:boolean; placeholder?:string; options:string[] }
export interface CrmForm { id:string; formKey:string; name:string; description?:string; successMessage:string; notifyEmails?:string; fields:CrmFormField[]; settings:Record<string,unknown>; active:boolean; createdAt:string; updatedAt:string }
export interface CrmPublicForm { formKey:string; name:string; description?:string; successMessage:string; fields:CrmFormField[]; settings:Record<string,unknown> }
export interface CrmSubmission { id:string; formId:string; formName:string; values:Record<string,unknown>; status:CrmSubmissionStatus; contactId?:string; sourceUrl?:string; createdAt:string }
export interface CrmStageMetric { id:string; name:string; color:string; probability:number; deals:number; value:number; closed:boolean; won:boolean }
export interface CrmDashboard { contacts:number; companies:number; openDeals:number; pendingActivities:number; forms:number; newSubmissions:number; pipelineValue:number; pipeline:CrmStageMetric[]; recentSubmissions:CrmSubmission[] }
