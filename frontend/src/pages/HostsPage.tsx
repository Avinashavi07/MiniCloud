import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiErrorMessage, fetchHosts } from '../api/client'
import type { Host } from '../api/types'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes, formatPercent } from '../lib/format'

export function HostsPage() {
  const [hosts, setHosts] = useState<Host[]>([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setHosts(await fetchHosts())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load hosts'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const filtered = useMemo(
    () => hosts.filter((host) => host.hostname.toLowerCase().includes(query.toLowerCase())),
    [hosts, query],
  )

  return (
    <div>
      <PageHeader
        title="Hosts"
        crumbs={['Infrastructure', 'Hosts']}
        description="Phase 1 discovers the local Ubuntu machine. CPU, memory, disk, KVM, and libvirt status are read from the operating system."
        actions={
          <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            Refresh
          </button>
        }
      />
      <div className="mb-4">
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Filter by hostname"
          className="w-full max-w-sm rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm outline-none focus:border-sky-500"
        />
      </div>
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error ? (
        <div className="overflow-hidden rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Hostname</th>
                <th className="px-4 py-3">OS</th>
                <th className="px-4 py-3">CPU</th>
                <th className="px-4 py-3">Memory</th>
                <th className="px-4 py-3">Disk</th>
                <th className="px-4 py-3">KVM</th>
                <th className="px-4 py-3">libvirt</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((host) => (
                <tr key={host.id} className="border-t border-slate-800 hover:bg-slate-900/60">
                  <td className="px-4 py-3">
                    <Link to={`/hosts/${host.id}`} className="font-medium text-sky-300 hover:underline">
                      {host.hostname}
                    </Link>
                    {host.local ? <span className="ml-2 text-xs text-slate-500">local</span> : null}
                  </td>
                  <td className="px-4 py-3 text-slate-300">{host.osVersion ?? host.osName}</td>
                  <td className="px-4 py-3">{host.metrics?.cpuCores ?? 'n/a'} · {formatPercent(host.metrics?.cpuUsagePercent)}</td>
                  <td className="px-4 py-3">{formatBytes(host.metrics?.memoryTotalBytes)}</td>
                  <td className="px-4 py-3">{formatBytes(host.metrics?.diskTotalBytes)}</td>
                  <td className="px-4 py-3"><StatusBadge status={host.kvmStatus} /></td>
                  <td className="px-4 py-3"><StatusBadge status={host.libvirtStatus} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
