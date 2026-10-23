import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchOvs } from '../api/client'
import type { OvsListResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'

export function OvsPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [data, setData] = useState<OvsListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchOvs())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read Open vSwitch'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const bridges = useMemo(
    () => (data?.bridges ?? []).filter((bridge) => bridge.name.toLowerCase().includes(query.toLowerCase())),
    [data, query],
  )

  return (
    <div>
      <PageHeader
        title="Open vSwitch"
        crumbs={['Infrastructure', 'OVS']}
        description="Live OVS bridges from ovs-vsctl. This is not libvirt NAT. MiniCloud creates only mc-* lab bridges."
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>
            {canManage && data?.capabilities.connected ? (
              <Link to="/ovs/create" className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">Create bridge</Link>
            ) : null}
          </>
        }
      />
      {data ? (
        <div className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={data.capabilities.status} />
            <span>{data.note}</span>
          </div>
          {data.capabilities.ovsVersion ? (
            <p className="mt-2 text-xs text-slate-500">
              ovs-vsctl {data.capabilities.ovsVersion}
              {data.capabilities.dbVersion ? ` · DB ${data.capabilities.dbVersion}` : ''}
            </p>
          ) : null}
        </div>
      ) : null}
      <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Filter bridges" className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500" />
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && bridges.length === 0 ? (
        <EmptyState
          title={data?.capabilities.connected ? 'No OVS bridges yet' : 'Open vSwitch is not connected'}
          description={
            data?.capabilities.connected
              ? 'Create an mc-* bridge for lab VMs. Do not attach the host Wi‑Fi uplink here.'
              : data?.capabilities.detail ?? 'Install openvswitch-switch and start ovs-vswitchd.'
          }
        />
      ) : null}
      {!loading && !error && bridges.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Bridge</th>
                <th className="px-4 py-3">Ports</th>
                <th className="px-4 py-3">Managed</th>
                <th className="px-4 py-3">Datapath</th>
              </tr>
            </thead>
            <tbody>
              {bridges.map((bridge) => (
                <tr key={bridge.name} className="border-t border-slate-800">
                  <td className="px-4 py-3">
                    <Link to={`/ovs/${encodeURIComponent(bridge.name)}`} className="text-sky-300 hover:underline">{bridge.name}</Link>
                  </td>
                  <td className="px-4 py-3">{bridge.ports.length}</td>
                  <td className="px-4 py-3">{bridge.managedByMiniCloud ? 'yes' : 'host'}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-400">{bridge.datapathId ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
