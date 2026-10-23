import type { ReactNode } from 'react'

interface MetricCardProps {
  label: string
  value: ReactNode
  hint?: string
  icon?: ReactNode
}

export function MetricCard({ label, value, hint, icon }: MetricCardProps) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
          <p className="mt-2 text-2xl font-semibold text-slate-50">{value}</p>
          {hint ? <p className="mt-1 text-xs text-slate-500">{hint}</p> : null}
        </div>
        {icon ? <div className="rounded-lg bg-slate-800/80 p-2 text-sky-300">{icon}</div> : null}
      </div>
    </div>
  )
}
