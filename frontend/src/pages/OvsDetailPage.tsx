import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, deleteOvsBridge, fetchOvsBridge } from '../api/client'
import type { OvsBridgeInfo } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'

export function OvsDetailPage() {
  const { name } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [bridge, setBridge] = useState<OvsBridgeInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [confirmDelete, setConfirmDelete] = useState(false)

  async function load() {
    if (!name) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      setBridge(await fetchOvsBridge(name))
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read OVS bridge'))
      setBridge(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [name])

  if (loading) {
    return <LoadingSkeleton />
  }
  if (error && !bridge) {
    return <EmptyState title="Bridge not reported by OVS" description={error} action={<Link to="/ovs" className="text-sky-300">Back to OVS</Link>} />
  }
  if (!bridge) {
    return null
  }

  return (
    <div>
      <PageHeader
        title={bridge.name}
        crumbs={['Infrastructure', 'OVS', bridge.name]}
        description={bridge.detail ?? 'Live Open vSwitch bridge.'}
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>
            {canManage && bridge.managedByMiniCloud ? (
              <button type="button" onClick={() => setConfirmDelete(true)} className="rounded-md border border-red-800 px-3 py-1.5 text-sm text-red-300 hover:bg-red-950">Delete</button>
            ) : null}
          </>
        }
      />
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      <section className="mb-4 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-sm sm:grid-cols-2">
        <div><dt className="text-slate-500">UUID</dt><dd className="font-mono text-xs">{bridge.uuid ?? '—'}</dd></div>
        <div><dt className="text-slate-500">Managed by MiniCloud</dt><dd>{bridge.managedByMiniCloud ? 'yes' : 'no'}</dd></div>
        <div><dt className="text-slate-500">Datapath ID</dt><dd className="font-mono text-xs">{bridge.datapathId ?? '—'}</dd></div>
        <div><dt className="text-slate-500">Ports</dt><dd>{bridge.ports.length}</dd></div>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <h2 className="text-sm font-medium text-slate-200">Ports</h2>
        {bridge.ports.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">No ports yet. Attach a VM NIC to this bridge from the VM detail page.</p>
        ) : (
          <table className="mt-3 w-full text-left text-sm">
            <thead className="text-xs uppercase text-slate-500">
              <tr>
                <th className="py-2">Name</th>
                <th>Type</th>
                <th>ofport</th>
              </tr>
            </thead>
            <tbody>
              {bridge.ports.map((port) => (
                <tr key={port.name} className="border-t border-slate-800">
                  <td className="py-2">{port.name}</td>
                  <td>{port.type ?? '—'}</td>
                  <td>{port.ofport ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      <ConfirmDialog
        open={confirmDelete}
        title={`Delete ${bridge.name}?`}
        description="This runs ovs-vsctl del-br. Only empty MiniCloud-managed bridges can be deleted."
        confirmLabel="Delete bridge"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false)
          deleteOvsBridge(bridge.name)
            .then(() => {
              toast.push('OVS bridge deleted', 'success')
              navigate('/ovs')
            })
            .catch((err) => toast.push(apiErrorMessage(err), 'error'))
        }}
      />
    </div>
  )
}
