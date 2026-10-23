import { useEffect, useState } from 'react'
import { apiErrorMessage, fetchAuditEvents } from '../api/client'
import type { AuditEvent, PageResponse } from '../api/types'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatDate } from '../lib/format'

export function AuditLogsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<AuditEvent> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load(nextPage = page) {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchAuditEvents(nextPage, 15))
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load audit events'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load(page)
  }, [page])

  return (
    <div>
      <PageHeader
        title="Audit logs"
        crumbs={['Operations', 'Audit Logs']}
        description="Phase 1 records authentication and host discovery. VM and network actions will appear here in later phases."
      />
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {data && !loading ? (
        <>
          <div className="overflow-hidden rounded-xl border border-slate-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Resource</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Message</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((event) => (
                  <tr key={event.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 text-slate-400">{formatDate(event.timestamp)}</td>
                    <td className="px-4 py-3">{event.action}</td>
                    <td className="px-4 py-3">{event.username ?? 'system'}</td>
                    <td className="px-4 py-3 text-slate-400">{event.resourceType} {event.resourceId}</td>
                    <td className="px-4 py-3"><StatusBadge status={event.status} /></td>
                    <td className="px-4 py-3 text-slate-400">{event.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-4 flex items-center justify-between text-sm text-slate-400">
            <span>
              Page {data.number + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} events
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
                className="rounded-md border border-slate-700 px-3 py-1 disabled:opacity-40"
              >
                Previous
              </button>
              <button
                type="button"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((value) => value + 1)}
                className="rounded-md border border-slate-700 px-3 py-1 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : null}
    </div>
  )
}
