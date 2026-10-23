import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, deleteNetwork, fetchNetwork, setNetworkAutostart, startNetwork, stopNetwork } from '../api/client'
import type { VirtualNetworkInfo } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'

export function NetworkDetailPage() {
  const { name } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [network, setNetwork] = useState<VirtualNetworkInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [confirmStop, setConfirmStop] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const toast = useToast()

  async function load() {
    if (!name) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      setNetwork(await fetchNetwork(name))
    } catch (err) {
      setNetwork(null)
      setError(apiErrorMessage(err, 'libvirt has no such network'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [name])

  async function run(action: () => Promise<VirtualNetworkInfo>, label: string) {
    setBusy(true)
    try {
      setNetwork(await action())
      toast.push(`${label} completed`, 'success')
    } catch (err) {
      toast.push(apiErrorMessage(err, `${label} failed`), 'error')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <LoadingSkeleton />
  }
  if (error && !network) {
    return (
      <EmptyState
        title="Network not reported by libvirt"
        description={error}
        action={<Link to="/networks" className="text-sky-300">Back to networks</Link>}
      />
    )
  }
  if (!network) {
    return <ErrorState message="Network not available" onRetry={() => void load()} />
  }

  return (
    <div>
      <PageHeader
        title={network.name}
        crumbs={['Infrastructure', 'Networks', network.name]}
        description="Live libvirt network. The bridge, NAT, and DHCP range come from virsh, not PostgreSQL."
        actions={
          <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            Refresh
          </button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Network information</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="Status" value={<StatusBadge status={network.active ? 'RUNNING' : 'STOPPED'} />} />
            <Row label="libvirt state" value={network.state} />
            <Row label="UUID" value={network.uuid ?? '—'} />
            <Row label="Bridge" value={network.bridge ?? '—'} />
            <Row label="Forward mode" value={network.forwardMode ?? '—'} />
            <Row label="CIDR" value={network.cidr ?? '—'} />
            <Row label="Gateway" value={network.gateway ?? '—'} />
            <Row label="DHCP range" value={network.dhcpStart && network.dhcpEnd ? `${network.dhcpStart} – ${network.dhcpEnd}` : '—'} />
            <Row label="Persistent" value={network.persistent ? 'yes' : 'no'} />
            <Row label="Autostart" value={network.autostart ? 'yes' : 'no'} />
          </dl>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Operations</h2>
          <p className="mt-2 text-sm text-slate-400">
            Start/stop call virsh net-start / net-destroy. Delete refuses an active network and will not undefine <code>default</code>.
          </p>
          {canManage ? (
            <div className="mt-4 flex flex-wrap gap-2">
              <Action disabled={busy} onClick={() => void run(() => startNetwork(network.name), 'Start')}>Start</Action>
              <Action disabled={busy} onClick={() => setConfirmStop(true)}>Stop</Action>
              <Action disabled={busy} onClick={() => void run(() => setNetworkAutostart(network.name, !network.autostart), network.autostart ? 'Disable autostart' : 'Enable autostart')}>
                {network.autostart ? 'Disable autostart' : 'Enable autostart'}
              </Action>
              <Action disabled={busy || network.name === 'default'} onClick={() => setConfirmDelete(true)}>Delete</Action>
            </div>
          ) : (
            <p className="mt-3 text-sm text-slate-500">VIEWER role is read-only.</p>
          )}
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 lg:col-span-2">
          <h2 className="text-sm font-medium text-slate-200">Connected VMs</h2>
          {network.connectedVms.length === 0 ? (
            <p className="mt-3 text-sm text-slate-500">No domains currently have a NIC on this network.</p>
          ) : (
            <table className="mt-3 w-full text-left text-sm">
              <thead className="text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-1">VM</th>
                  <th>Interface</th>
                  <th>MAC</th>
                  <th>IP</th>
                </tr>
              </thead>
              <tbody>
                {network.connectedVms.map((vm) => (
                  <tr key={`${vm.vmName}-${vm.mac}`} className="border-t border-slate-800">
                    <td className="py-2">
                      <Link to={`/vms/${encodeURIComponent(vm.vmName)}`} className="text-sky-300 hover:underline">{vm.vmName}</Link>
                    </td>
                    <td>{vm.interfaceName ?? '—'}</td>
                    <td className="text-slate-400">{vm.mac ?? '—'}</td>
                    <td>{vm.ip ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>

      <ConfirmDialog
        open={confirmStop}
        title={`Stop ${network.name}?`}
        description="This is virsh net-destroy. Connected guests lose this NAT until the network is started again."
        confirmLabel="Stop network"
        onCancel={() => setConfirmStop(false)}
        onConfirm={() => {
          setConfirmStop(false)
          void run(() => stopNetwork(network.name), 'Stop')
        }}
      />
      <ConfirmDialog
        open={confirmDelete}
        title={`Delete ${network.name}?`}
        description="This undefines the libvirt network. The network must already be stopped."
        confirmLabel="Delete network"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false)
          setBusy(true)
          deleteNetwork(network.name)
            .then(() => {
              toast.push('Network deleted from libvirt', 'success')
              navigate('/networks')
            })
            .catch((err) => toast.push(apiErrorMessage(err, 'Delete failed'), 'error'))
            .finally(() => setBusy(false))
        }}
      />
    </div>
  )
}

function Action({ children, onClick, disabled }: { children: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800 disabled:opacity-40"
    >
      {children}
    </button>
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
