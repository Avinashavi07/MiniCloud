import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, createVolume, deleteStoragePool, deleteVolume, fetchStoragePool, setStorageAutostart, startStoragePool, stopStoragePool } from '../api/client'
import type { VirtualStoragePool } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import { formatBytes } from '../lib/format'

export function StorageDetailPage() {
  const { name } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [pool, setPool] = useState<VirtualStoragePool | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [confirmStop, setConfirmStop] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [volumeName, setVolumeName] = useState('data-01.qcow2')
  const [volumeGb, setVolumeGb] = useState(10)
  const [volumeFormat, setVolumeFormat] = useState('qcow2')
  const toast = useToast()

  async function load() {
    if (!name) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      setPool(await fetchStoragePool(name))
    } catch (err) {
      setPool(null)
      setError(apiErrorMessage(err, 'libvirt has no such pool'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [name])

  async function run(action: () => Promise<VirtualStoragePool>, label: string) {
    setBusy(true)
    try {
      setPool(await action())
      toast.push(`${label} completed`, 'success')
    } catch (err) {
      toast.push(apiErrorMessage(err, `${label} failed`), 'error')
    } finally {
      setBusy(false)
    }
  }

  async function onCreateVolume(event: FormEvent) {
    event.preventDefault()
    if (!pool) {
      return
    }
    setBusy(true)
    try {
      await createVolume(pool.name, { name: volumeName, sizeGb: volumeGb, format: volumeFormat })
      toast.push(`Created ${volumeName}`, 'success')
      setPool(await fetchStoragePool(pool.name))
    } catch (err) {
      toast.push(apiErrorMessage(err, 'Volume create failed'), 'error')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <LoadingSkeleton />
  }
  if (error && !pool) {
    return <EmptyState title="Pool not reported by libvirt" description={error} action={<Link to="/storage" className="text-sky-300">Back to storage</Link>} />
  }
  if (!pool) {
    return <ErrorState message="Pool not available" onRetry={() => void load()} />
  }

  return (
    <div>
      <PageHeader
        title={pool.name}
        crumbs={['Infrastructure', 'Storage', pool.name]}
        description="Live libvirt storage pool. Volumes are qcow2/raw files, not PostgreSQL rows."
        actions={<button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>}
      />
      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Pool information</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="Status" value={<StatusBadge status={pool.active ? 'RUNNING' : 'STOPPED'} />} />
            <Row label="libvirt state" value={pool.state} />
            <Row label="UUID" value={pool.uuid ?? '—'} />
            <Row label="Type" value={pool.type ?? '—'} />
            <Row label="Path" value={pool.path ?? '—'} />
            <Row label="Capacity" value={formatBytes(pool.capacityBytes)} />
            <Row label="Allocation" value={formatBytes(pool.allocationBytes)} />
            <Row label="Available" value={formatBytes(pool.availableBytes)} />
            <Row label="Persistent" value={pool.persistent ? 'yes' : 'no'} />
            <Row label="Autostart" value={pool.autostart ? 'yes' : 'no'} />
          </dl>
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Operations</h2>
          <p className="mt-2 text-sm text-slate-400">Stop/delete refuse an in-use pool. The default pool cannot be undefined.</p>
          {canManage ? (
            <div className="mt-4 flex flex-wrap gap-2">
              <Action disabled={busy} onClick={() => void run(() => startStoragePool(pool.name), 'Start')}>Start</Action>
              <Action disabled={busy} onClick={() => setConfirmStop(true)}>Stop</Action>
              <Action disabled={busy} onClick={() => void run(() => setStorageAutostart(pool.name, !pool.autostart), pool.autostart ? 'Disable autostart' : 'Enable autostart')}>
                {pool.autostart ? 'Disable autostart' : 'Enable autostart'}
              </Action>
              <Action disabled={busy || pool.name === 'default'} onClick={() => setConfirmDelete(true)}>Delete</Action>
            </div>
          ) : <p className="mt-3 text-sm text-slate-500">VIEWER role is read-only.</p>}
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 lg:col-span-2">
          <h2 className="text-sm font-medium text-slate-200">Volumes</h2>
          {pool.volumes.length === 0 ? <p className="mt-3 text-sm text-slate-500">No volumes in this pool.</p> : (
            <table className="mt-3 w-full text-left text-sm">
              <thead className="text-xs uppercase text-slate-500">
                <tr><th className="py-1">Name</th><th>Capacity</th><th>Allocation</th><th>Used by</th>{canManage ? <th></th> : null}</tr>
              </thead>
              <tbody>
                {pool.volumes.map((volume) => (
                  <tr key={volume.name} className="border-t border-slate-800">
                    <td className="py-2">{volume.name}<div className="text-xs text-slate-500">{volume.path}</div></td>
                    <td>{formatBytes(volume.capacityBytes)}</td>
                    <td>{formatBytes(volume.allocationBytes)}</td>
                    <td>
                      {volume.usedBy.length === 0 ? '—' : volume.usedBy.map((item) => (
                        <Link key={`${item.vmName}-${item.target}`} to={`/vms/${encodeURIComponent(item.vmName)}`} className="mr-2 text-sky-300 hover:underline">{item.vmName}:{item.target}</Link>
                      ))}
                    </td>
                    {canManage ? (
                      <td>
                        <button type="button" disabled={busy || volume.usedBy.length > 0} onClick={() => {
                          setBusy(true)
                          deleteVolume(pool.name, volume.name)
                            .then(async () => { toast.push(`Deleted ${volume.name}`, 'success'); setPool(await fetchStoragePool(pool.name)) })
                            .catch((err) => toast.push(apiErrorMessage(err, 'Delete volume failed'), 'error'))
                            .finally(() => setBusy(false))
                        }} className="text-xs text-red-300 hover:underline disabled:opacity-40">Delete</button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {canManage ? (
            <form onSubmit={onCreateVolume} className="mt-4 flex flex-wrap items-end gap-2">
              <label className="text-xs text-slate-400">Name<input value={volumeName} onChange={(event) => setVolumeName(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400">Size (GB)<input type="number" min={1} value={volumeGb} onChange={(event) => setVolumeGb(Number(event.target.value))} className="mt-1 block w-24 rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100" /></label>
              <label className="text-xs text-slate-400">Format
                <select value={volumeFormat} onChange={(event) => setVolumeFormat(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                  <option value="qcow2">qcow2</option>
                  <option value="raw">raw</option>
                </select>
              </label>
              <button type="submit" disabled={busy} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40">Create volume</button>
            </form>
          ) : null}
        </section>
      </div>
      <ConfirmDialog open={confirmStop} title={`Stop ${pool.name}?`} description="This is virsh pool-destroy. Volumes stay on disk; VMs using them can fail." confirmLabel="Stop pool" onCancel={() => setConfirmStop(false)} onConfirm={() => { setConfirmStop(false); void run(() => stopStoragePool(pool.name), 'Stop') }} />
      <ConfirmDialog open={confirmDelete} title={`Delete ${pool.name}?`} description="This undefines the libvirt pool. It must be empty and stopped." confirmLabel="Delete pool" onCancel={() => setConfirmDelete(false)} onConfirm={() => {
        setConfirmDelete(false)
        setBusy(true)
        deleteStoragePool(pool.name)
          .then(() => { toast.push('Pool deleted from libvirt', 'success'); navigate('/storage') })
          .catch((err) => toast.push(apiErrorMessage(err, 'Delete failed'), 'error'))
          .finally(() => setBusy(false))
      }} />
    </div>
  )
}

function Action({ children, onClick, disabled }: { children: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button type="button" disabled={disabled} onClick={onClick} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800 disabled:opacity-40">
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
