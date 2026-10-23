interface StatusBadgeProps {
  status?: string | null
}

const TONES: Record<string, string> = {
  RUNNING: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  AVAILABLE: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  CONNECTED: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  SUCCESS: 'bg-emerald-500/15 text-emerald-300 ring-emerald-500/30',
  STOPPED: 'bg-slate-500/20 text-slate-300 ring-slate-500/30',
  UNAVAILABLE: 'bg-slate-500/20 text-slate-300 ring-slate-500/30',
  NOT_INSTALLED: 'bg-slate-500/20 text-slate-300 ring-slate-500/30',
  PAUSED: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
  CREATING: 'bg-sky-500/15 text-sky-300 ring-sky-500/30',
  DELETING: 'bg-orange-500/15 text-orange-300 ring-orange-500/30',
  ERROR: 'bg-red-500/15 text-red-300 ring-red-500/30',
  PERMISSION_DENIED: 'bg-red-500/15 text-red-300 ring-red-500/30',
  UNKNOWN: 'bg-zinc-500/20 text-zinc-300 ring-zinc-500/30',
  SOCKET_PRESENT: 'bg-amber-500/15 text-amber-300 ring-amber-500/30',
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const value = status ?? 'UNKNOWN'
  const tone = TONES[value] ?? 'bg-zinc-500/20 text-zinc-300 ring-zinc-500/30'
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ring-1 ${tone}`}>
      {value}
    </span>
  )
}
