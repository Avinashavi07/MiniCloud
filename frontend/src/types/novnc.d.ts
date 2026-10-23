declare module '@novnc/novnc' {
  export default class RFB {
    constructor(target: HTMLElement, url: string, options?: Record<string, unknown>)
    addEventListener(type: string, listener: (event: Event) => void): void
    removeEventListener(type: string, listener: (event: Event) => void): void
    disconnect(): void
    focus(): void
    scaleViewport: boolean
    resizeSession: boolean
    viewOnly: boolean
  }
}
