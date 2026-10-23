import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchVms } from '../api/client'
import type { VmListResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes } from '../lib/format'

export function VmsPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [data, setData] = useState<VmListResponse | null>(null)
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchVms())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read libvirt domains'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const vms = useMemo(
    () => (data?.vms ?? []).filter((vm) => vm.name.toLowerCase().includes(query.toLowerCase())),
    [data, query],
  )

  return (
    <div>
      <PageHeader
        title="Virtual machines"
        crumbs={['Infrastructure', 'Virtual Machines']}
        description="Live libvirt domains. Create, start, stop, and delete operate on QEMU/KVM, not on database rows."
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
              Refresh
            </button>
            {canManage ? (
              <Link to="/vms/create" className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">
                Create VM
              </Link>
            ) : null}
          </>
        }
      />

      {data ? (
        <div className="mb-4 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={data.hypervisor.status} />
            <span>{data.note}</span>
          </div>
        </div>
      ) : null}

      <input
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Filter domains"
        className="mb-4 w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500"
      />

      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && vms.length === 0 ? (
        <EmptyState
          title={data?.listingAvailable ? 'libvirt has no domains yet' : 'Hypervisor is not connected'}
          description={
            data?.listingAvailable
              ? 'Use Create VM to define a real KVM domain. MiniCloud will not invent one in PostgreSQL.'
              : 'Connect qemu:///system first.'
          }
        />
      ) : null}
      {!loading && !error && vms.length > 0 ? (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">CPU</th>
                <th className="px-4 py-3">Memory</th>
                <th className="px-4 py-3">Disk</th>
                <th className="px-4 py-3">IP</th>
                <th className="px-4 py-3">MAC</th>
              </tr>
            </thead>
            <tbody>
              {vms.map((vm) => (
                <tr key={vm.name} className="border-t border-slate-800 hover:bg-slate-900/60">
                  <td className="px-4 py-3">
                    <Link to={`/vms/${encodeURIComponent(vm.name)}`} className="text-sky-300 hover:underline">
                      {vm.name}
                    </Link>
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={vm.normalizedState} /></td>
                  <td className="px-4 py-3">{vm.cpuCores ?? 'n/a'}</td>
                  <td className="px-4 py-3">{formatBytes(vm.memoryBytes)}</td>
                  <td className="px-4 py-3 text-slate-400">{vm.disks[0]?.target ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{vm.nics[0]?.ip ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{vm.nics[0]?.mac ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
