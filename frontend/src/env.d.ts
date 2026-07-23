/// <reference types="vite/client" />

declare module 'plyr' {
  export interface Options {
    controls?: string[]
    settings?: string[]
    captions?: { active?: boolean; language?: string; update?: boolean }
    ratio?: string
    keyboard?: { focused?: boolean; global?: boolean }
    tooltips?: { controls?: boolean; seek?: boolean }
    speed?: { selected?: number; options?: number[] }
  }

  export default class Plyr {
    constructor(target: HTMLElement | string, options?: Options)
    destroy(): void
  }
}
