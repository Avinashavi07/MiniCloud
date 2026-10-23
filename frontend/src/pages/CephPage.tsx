import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchCeph } from '../api/client'
import type { CephListResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes } from '../lib/format'

export function CephPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [data, setData] = useState<CephListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchCeph())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read Ceph'))
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
        title="Ceph"
        crumbs={['Infrastructure', 'Ceph']}
        description="Live Ceph cluster, OSD pools, and RBD images from ceph/rbd. Directory pools stay under Storage."
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>
            {canManage && data?.capabilities.connected ? (
              <Link to="/ceph/create" className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">Create pool</Link>
            ) : null}
          </>
        }
      />
      {data ? (
        <div className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={data.capabilities.status} />
            {data.status ? <StatusBadge status={data.status.health} /> : null}
            <span>{data.note}</span>
          </div>
          {data.capabilities.cephVersion ? (
            <p className="mt-2 text-xs text-slate-500">
              ceph {data.capabilities.cephVersion}
              {data.capabilities.clusterFsId ? ` · fsid ${data.capabilities.clusterFsId}` : ''}
            </p>
          ) : null}
          {data.status ? (
            <dl className="mt-3 grid gap-3 text-xs sm:grid-cols-4">
              <div><dt className="text-slate-500">MONs</dt><dd>{data.status.monCount}</dd></div>
              <div><dt className="text-slate-500">OSDs</dt><dd>{data.status.osdUp}/{data.status.osdCount} up</dd></div>
              <div><dt className="text-slate-500">Used</dt><dd>{formatBytes(data.status.usedBytes)}</dd></div>
              <div><dt className="text-slate-500">Available</dt><dd>{formatBytes(data.status.availableBytes)}</dd></div>
            </dl>
          ) : null}
        </div>
      ) : null}
      <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Filter pools" className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500" />
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && pools.length === 0 ? (
        <EmptyState
          title={data?.capabilities.connected ? 'No Ceph pools yet' : 'Ceph is not connected'}
          description={
            data?.capabilities.connected
              ? 'Create an mc-* pool for RBD images. Lab installs often use MicroCeph with loop OSDs.'
              : data?.capabilities.detail ?? 'Install MicroCeph and grant the libvirt group read access to the client keyring.'
          }
        />
      ) : null}
      {!loading && !error && pools.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Pool</th>
                <th className="px-4 py-3">App</th>
                <th className="px-4 py-3">PGs</th>
                <th className="px-4 py-3">Stored</th>
                <th className="px-4 py-3">Images</th>
                <th className="px-4 py-3">Managed</th>
              </tr>
            </thead>
            <tbody>
              {pools.map((pool) => (
                <tr key={pool.name} className="border-t border-slate-800">
                  <td className="px-4 py-3">
                    <Link to={`/ceph/${encodeURIComponent(pool.name)}`} className="text-sky-300 hover:underline">{pool.name}</Link>
                  </td>
                  <td className="px-4 py-3">{pool.application ?? '—'}</td>
                  <td className="px-4 py-3">{pool.pgNum ?? '—'}</td>
                  <td className="px-4 py-3">{formatBytes(pool.storedBytes)}</td>
                  <td className="px-4 py-3">{pool.images.length}</td>
                  <td className="px-4 py-3">{pool.managedByMiniCloud ? 'yes' : 'host'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
