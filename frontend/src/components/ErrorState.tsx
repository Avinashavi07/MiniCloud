interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
      <div className="flex items-center justify-between gap-3">
        <p>{message}</p>
        {onRetry ? (
          <button
            type="button"
            onClick={onRetry}
            className="rounded-md border border-red-400/30 px-3 py-1 text-xs text-red-100 hover:bg-red-500/20"
          >
            Retry
          </button>
        ) : null}
      </div>
    </div>
  )
}
