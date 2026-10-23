import { Cpu, HardDrive, MemoryStick, Server } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { Cell, Pie, PieChart, ResponsiveContainer } from 'recharts'
import { apiErrorMessage, fetchDashboard } from '../api/client'
import type { DashboardSummary } from '../api/types'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes, formatDate, formatPercent } from '../lib/format'

const COLORS = ['#38bdf8', '#34d399', '#f59e0b', '#f87171', '#94a3b8']

export function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setData(await fetchDashboard())
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load dashboard'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  if (loading) {
    return <LoadingSkeleton rows={6} />
  }
  if (error) {
    return <ErrorState message={error} onRetry={() => void load()} />
  }
  if (!data) {
    return null
  }

  const distribution = Object.entries(data.vmStatusDistribution).map(([name, value]) => ({ name, value }))
  const usage = [
    { name: 'CPU', used: data.cpuUsagePercent ?? 0 },
    { name: 'Memory', used: data.memoryUsagePercent ?? 0 },
    { name: 'Storage', used: data.storageUsagePercent ?? 0 },
  ]

  return (
    <div>
      <PageHeader
        title="Infrastructure dashboard"
        crumbs={['Overview', 'Dashboard']}
        description="These cards are built from the live Ubuntu host and a libvirt probe. MiniCloud does not invent VMs or usage numbers."
        actions={
          <button
            type="button"
            onClick={() => void load()}
            className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800"
          >
            Refresh
          </button>
        }
      />

      <div className="mb-6 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
        <div className="flex flex-wrap items-center gap-3">
          <span>Hypervisor {data.hypervisor.providerType}</span>
          <StatusBadge status={data.hypervisor.status} />
          <span className="text-slate-500">{data.hypervisor.detail}</span>
        </div>
        {data.hypervisor.connected ? (
          <p className="mt-2 text-xs text-slate-500">
            {data.hypervisor.libvirtVersion ?? 'libvirt'} · {data.hypervisor.hypervisorVersion ?? 'QEMU'}
            {data.hypervisor.node?.cpus ? ` · libvirt node ${data.hypervisor.node.cpus} CPUs` : ''}
          </p>
        ) : null}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="Hosts" value={data.totalHosts} hint="Discovered local node" icon={<Server size={18} />} />
        <MetricCard label="VMs" value={data.totalVms} hint={data.hypervisor.connected ? 'From libvirt' : 'None until libvirt connects'} icon={<Cpu size={18} />} />
        <MetricCard label="Running / Stopped" value={`${data.runningVms} / ${data.stoppedVms}`} hint="Hypervisor-reported states" />
        <MetricCard
          label="CPU"
          value={`${data.totalCpuCores ?? 'n/a'} cores`}
          hint={`Allocated to VMs: ${data.allocatedCpuCores ?? 0}`}
        />
        <MetricCard
          label="Memory"
          value={formatBytes(data.totalMemoryBytes)}
          hint={`Allocated to VMs: ${formatBytes(data.allocatedMemoryBytes)}`}
          icon={<MemoryStick size={18} />}
        />
        <MetricCard
          label="Storage"
          value={formatBytes(data.storageCapacityBytes)}
          hint={`Used on host root: ${formatBytes(data.storageUsedBytes)}`}
          icon={<HardDrive size={18} />}
        />
        <MetricCard label="CPU usage" value={formatPercent(data.cpuUsagePercent)} hint="Live host CPU" />
        <MetricCard label="Memory usage" value={formatPercent(data.memoryUsagePercent)} hint="From /proc/meminfo" />
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-3">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
          <h2 className="text-sm font-medium text-slate-200">Host resource usage</h2>
          <p className="mt-1 text-xs text-slate-500">Current snapshot only. Phase 1 does not invent historical series.</p>
          <div className="mt-4 space-y-4">
            {usage.map((item) => (
              <div key={item.name}>
                <div className="mb-1 flex justify-between text-xs text-slate-400">
                  <span>{item.name}</span>
                  <span>{formatPercent(item.used)}</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                  <div className="h-full bg-sky-500" style={{ width: `${Math.min(item.used, 100)}%` }} />
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
          <h2 className="text-sm font-medium text-slate-200">VM status</h2>
          {distribution.length === 0 ? (
            <p className="mt-8 text-center text-sm text-slate-500">No domains reported by libvirt.</p>
          ) : (
            <div className="h-52">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={distribution} dataKey="value" nameKey="name" innerRadius={45} outerRadius={70}>
                    {distribution.map((entry, index) => (
                      <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-4">
          <h2 className="text-sm font-medium text-slate-200">Local host</h2>
          {data.localHost ? (
            <dl className="mt-3 space-y-2 text-sm">
              <Row label="Hostname" value={data.localHost.hostname} />
              <Row label="OS" value={data.localHost.osVersion ?? data.localHost.osName} />
              <Row label="Kernel" value={data.localHost.kernel} />
              <Row label="KVM" value={<StatusBadge status={data.localHost.kvmStatus} />} />
              <Row label="libvirt" value={<StatusBadge status={data.localHost.libvirtStatus} />} />
            </dl>
          ) : (
            <p className="mt-6 text-sm text-slate-500">Local host has not been discovered yet.</p>
          )}
        </section>
      </div>

      <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900/60">
        <div className="border-b border-slate-800 px-4 py-3">
          <h2 className="text-sm font-medium text-slate-200">Recent infrastructure events</h2>
        </div>
        {data.recentEvents.length === 0 ? (
          <p className="px-4 py-8 text-sm text-slate-500">No audit events yet.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-2">Time</th>
                <th className="px-4 py-2">Action</th>
                <th className="px-4 py-2">User</th>
                <th className="px-4 py-2">Status</th>
                <th className="px-4 py-2">Message</th>
              </tr>
            </thead>
            <tbody>
              {data.recentEvents.map((event) => (
                <tr key={event.id} className="border-t border-slate-800">
                  <td className="px-4 py-2 text-slate-400">{formatDate(event.timestamp)}</td>
                  <td className="px-4 py-2">{event.action}</td>
                  <td className="px-4 py-2">{event.username ?? 'system'}</td>
                  <td className="px-4 py-2"><StatusBadge status={event.status} /></td>
                  <td className="px-4 py-2 text-slate-400">{event.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  )
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right text-slate-200">{value}</dd>
    </div>
  )
}
