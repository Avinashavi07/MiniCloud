import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchConsoles } from '../api/client'
import type { ConsoleListResponse } from '../api/types'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'

export function ConsolesPage() {
  const [data, setData] = useState<ConsoleListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchConsoles())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read libvirt consoles'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const consoles = useMemo(
    () => (data?.consoles ?? []).filter((item) => item.vmName.toLowerCase().includes(query.toLowerCase())),
    [data, query],
  )

  return (
    <div>
      <PageHeader
        title="Consoles"
        crumbs={['VDI', 'Consoles']}
        description="QEMU VNC endpoints from libvirt. MiniCloud does not implement VNC. The browser uses noVNC; MiniCloud only bridges 127.0.0.1."
        actions={
          <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            Refresh
          </button>
        }
      />
      {data ? <p className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">{data.note}</p> : null}
      <input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Filter VMs"
        className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500"
      />
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && consoles.length === 0 ? (
        <EmptyState
          title={data?.listingAvailable ? 'libvirt has no domains' : 'Hypervisor is not connected'}
          description={data?.listingAvailable ? 'Create or start a VM to get a QEMU VNC display.' : 'Connect qemu:///system first.'}
        />
      ) : null}
      {!loading && !error && consoles.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">VM</th>
                <th className="px-4 py-3">Protocol</th>
                <th className="px-4 py-3">Listen</th>
                <th className="px-4 py-3">Port</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {consoles.map((console) => (
                <tr key={console.vmName} className="border-t border-slate-800">
                  <td className="px-4 py-3">
                    <Link to={`/vms/${encodeURIComponent(console.vmName)}`} className="text-sky-300 hover:underline">
                      {console.vmName}
                    </Link>
                  </td>
                  <td className="px-4 py-3">{console.protocol ?? '—'}</td>
                  <td className="px-4 py-3">{console.host ?? '—'}</td>
                  <td className="px-4 py-3">{console.port ?? '—'}</td>
                  <td className="px-4 py-3"><StatusBadge status={console.status} /></td>
                  <td className="px-4 py-3 text-right">
                    <Link to={`/consoles/${encodeURIComponent(console.vmName)}`} className="text-sky-300 hover:underline">
                      Open
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
