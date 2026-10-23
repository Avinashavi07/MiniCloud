import { useEffect, useState, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import { apiErrorMessage, fetchHost, fetchHypervisorStatus } from '../api/client'
import type { Host, HypervisorCapabilities } from '../api/types'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { formatBytes, formatDate, formatPercent } from '../lib/format'

export function HostDetailPage() {
  const { id } = useParams()
  const [host, setHost] = useState<Host | null>(null)
  const [hypervisor, setHypervisor] = useState<HypervisorCapabilities | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    if (!id) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      const [nextHost, nextHypervisor] = await Promise.all([fetchHost(id), fetchHypervisorStatus()])
      setHost(nextHost)
      setHypervisor(nextHypervisor)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not load host'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [id])

  if (loading) {
    return <LoadingSkeleton rows={6} />
  }
  if (error) {
    return <ErrorState message={error} onRetry={() => void load()} />
  }
  if (!host) {
    return null
  }

  const metrics = host.metrics

  return (
    <div>
      <PageHeader
        title={host.hostname}
        crumbs={['Infrastructure', 'Hosts', host.hostname]}
        description="Inventory is stored in PostgreSQL. Usage numbers are collected live from Linux on each request."
        actions={
          <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            Refresh metrics
          </button>
        }
      />

      <div className="grid gap-4 xl:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Identity</h2>
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <Item label="Hostname" value={host.hostname} />
            <Item label="OS" value={host.osVersion ?? host.osName} />
            <Item label="Kernel" value={host.kernel} />
            <Item label="Architecture" value={host.architecture} />
            <Item label="Last seen" value={formatDate(host.lastSeenAt)} />
            <Item label="Discovered" value={formatDate(host.createdAt)} />
          </dl>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Compute and memory</h2>
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <Item label="CPU model" value={metrics?.cpuModel} />
            <Item label="CPU cores" value={metrics?.cpuCores} />
            <Item label="CPU usage" value={formatPercent(metrics?.cpuUsagePercent)} />
            <Item label="Load average" value={metrics?.loadAverage1?.toFixed(2)} />
            <Item label="Memory" value={`${formatBytes(metrics?.memoryUsedBytes)} / ${formatBytes(metrics?.memoryTotalBytes)}`} />
            <Item label="Memory usage" value={formatPercent(metrics?.memoryUsagePercent)} />
          </dl>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Disk</h2>
          <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
            <Item label="Mount" value={metrics?.diskMount} />
            <Item label="Capacity" value={formatBytes(metrics?.diskTotalBytes)} />
            <Item label="Used" value={formatBytes(metrics?.diskUsedBytes)} />
            <Item label="Available" value={formatBytes(metrics?.diskAvailableBytes)} />
            <Item label="Usage" value={formatPercent(metrics?.diskUsagePercent)} />
          </dl>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Virtualization probe</h2>
          <div className="mt-4 space-y-4 text-sm">
            <div>
              <div className="mb-1 flex items-center justify-between">
                <span className="text-slate-400">KVM</span>
                <StatusBadge status={metrics?.kvm?.status ?? host.kvmStatus} />
              </div>
              <p className="text-slate-400">{metrics?.kvm?.detail}</p>
              <p className="mt-1 text-xs text-slate-500">
                /dev/kvm: {metrics?.kvm?.devicePresent ? 'present' : 'missing'} · module: {metrics?.kvm?.moduleLoaded ? 'loaded' : 'not loaded'} · hardware: {metrics?.kvm?.vendorHint}
              </p>
            </div>
            <div>
              <div className="mb-1 flex items-center justify-between">
                <span className="text-slate-400">libvirt</span>
                <StatusBadge status={metrics?.libvirt?.status ?? host.libvirtStatus} />
              </div>
              <p className="text-slate-400">{metrics?.libvirt?.detail}</p>
              <p className="mt-1 text-xs text-slate-500">
                URI {metrics?.libvirt?.uri ?? host.libvirtUri} · service {metrics?.libvirt?.serviceState} · virsh {metrics?.libvirt?.virshAvailable ? 'available' : 'missing'}
              </p>
              {hypervisor?.connected ? (
                <p className="mt-2 text-xs text-slate-500">
                  {hypervisor.libvirtVersion ?? 'libvirt'} · {hypervisor.hypervisorVersion ?? 'QEMU'}
                  {hypervisor.node?.cpus ? ` · nodeinfo ${hypervisor.node.cpus} CPUs / ${formatBytes(hypervisor.node.memoryBytes)}` : ''}
                </p>
              ) : null}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}

function Item({ label, value }: { label: string; value?: ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-1 text-slate-200">{value ?? 'n/a'}</dd>
    </div>
  )
}
