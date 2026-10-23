import { Link } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage, fetchNetworks } from '../api/client'
import type { NetworkListResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'

export function NetworksPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [data, setData] = useState<NetworkListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchNetworks())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read libvirt networks'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const networks = useMemo(
    () => (data?.networks ?? []).filter((network) => network.name.toLowerCase().includes(query.toLowerCase())),
    [data, query],
  )

  return (
    <div>
      <PageHeader
        title="Networks"
        crumbs={['Infrastructure', 'Networks']}
        description="Live libvirt virtual networks. Create, start, and stop operate on virbr + NAT, not on database rows."
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
              Refresh
            </button>
            {canManage ? (
              <Link to="/networks/create" className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">
                Create network
              </Link>
            ) : null}
          </>
        }
      />

      {data ? (
        <div className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
          {data.note}
        </div>
      ) : null}

      <input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Filter networks"
        className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500"
      />

      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && networks.length === 0 ? (
        <EmptyState
          title={data?.listingAvailable ? 'libvirt has no virtual networks' : 'Hypervisor is not connected'}
          description={data?.listingAvailable ? 'Use Create network to define a real libvirt NAT network.' : 'Connect qemu:///system first.'}
        />
      ) : null}
      {!loading && !error && networks.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">State</th>
                <th className="px-4 py-3">Bridge</th>
                <th className="px-4 py-3">Forward</th>
                <th className="px-4 py-3">CIDR</th>
                <th className="px-4 py-3">Gateway</th>
                <th className="px-4 py-3">DHCP</th>
                <th className="px-4 py-3">VMs</th>
              </tr>
            </thead>
            <tbody>
              {networks.map((network) => (
                <tr key={network.name} className="border-t border-slate-800 hover:bg-slate-900/60">
                  <td className="px-4 py-3">
                    <Link to={`/networks/${encodeURIComponent(network.name)}`} className="text-sky-300 hover:underline">
                      {network.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={network.active ? 'RUNNING' : 'STOPPED'} /></td>
                  <td className="px-4 py-3">{network.bridge ?? '—'}</td>
                  <td className="px-4 py-3">{network.forwardMode ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{network.cidr ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{network.gateway ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">
                    {network.dhcpStart && network.dhcpEnd ? `${network.dhcpStart} – ${network.dhcpEnd}` : '—'}
                  </td>
                  <td className="px-4 py-3">{network.connectedVms.length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
