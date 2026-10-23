import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  crumbs?: string[]
  actions?: ReactNode
}

export function PageHeader({ title, description, crumbs, actions }: PageHeaderProps) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {crumbs && crumbs.length > 0 ? (
          <p className="mb-1 text-xs uppercase tracking-wide text-slate-500">{crumbs.join(' / ')}</p>
        ) : null}
        <h1 className="text-2xl font-semibold text-slate-50">{title}</h1>
        {description ? <p className="mt-1 max-w-3xl text-sm text-slate-400">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  )
}
