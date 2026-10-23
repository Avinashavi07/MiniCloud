import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchStorage } from '../api/client'
import type { StorageListResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes } from '../lib/format'

export function StoragePage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [data, setData] = useState<StorageListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchStorage())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read libvirt storage'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const pools = useMemo(
    () => (data?.pools ?? []).filter((pool) => pool.name.toLowerCase().includes(query.toLowerCase())),
    [data, query],
  )

  return (
    <div>
      <PageHeader
        title="Storage"
        crumbs={['Infrastructure', 'Storage']}
        description="Live libvirt directory pools and qcow2 volumes. The host root filesystem is shown separately so it is not mistaken for a VM disk."
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>
            {canManage ? (
              <Link to="/storage/create" className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">Create pool</Link>
            ) : null}
          </>
        }
      />

      {data ? <p className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">{data.note}</p> : null}
      {data?.hostFilesystems.map((fs) => (
        <section key={fs.name} className="mb-4 rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-sm">
          <h2 className="font-medium text-slate-100">{fs.name}</h2>
          <p className="mt-1 text-slate-500">{fs.note}</p>
          <dl className="mt-3 grid gap-3 sm:grid-cols-3">
            <div><dt className="text-slate-500">Mount</dt><dd>{fs.mount}</dd></div>
            <div><dt className="text-slate-500">Capacity</dt><dd>{formatBytes(fs.capacityBytes)}</dd></div>
            <div><dt className="text-slate-500">Available</dt><dd>{formatBytes(fs.availableBytes)}</dd></div>
          </dl>
        </section>
      ))}

      <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Filter pools" className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500" />
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && pools.length === 0 ? (
        <EmptyState title={data?.listingAvailable ? 'libvirt has no storage pools' : 'Hypervisor is not connected'} description={data?.listingAvailable ? 'Use Create pool to define a directory pool under /var/lib/libvirt/images.' : 'Connect qemu:///system first.'} />
      ) : null}
      {!loading && !error && pools.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">State</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Path</th>
                <th className="px-4 py-3">Capacity</th>
                <th className="px-4 py-3">Available</th>
                <th className="px-4 py-3">Volumes</th>
              </tr>
            </thead>
            <tbody>
              {pools.map((pool) => (
                <tr key={pool.name} className="border-t border-slate-800 hover:bg-slate-900/60">
                  <td className="px-4 py-3"><Link to={`/storage/${encodeURIComponent(pool.name)}`} className="text-sky-300 hover:underline">{pool.name}</Link></td>
                  <td className="px-4 py-3"><StatusBadge status={pool.active ? 'RUNNING' : 'STOPPED'} /></td>
                  <td className="px-4 py-3">{pool.type ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{pool.path ?? '—'}</td>
                  <td className="px-4 py-3">{formatBytes(pool.capacityBytes)}</td>
                  <td className="px-4 py-3">{formatBytes(pool.availableBytes)}</td>
                  <td className="px-4 py-3">{pool.volumes.length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
