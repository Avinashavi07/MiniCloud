import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, createRbdImage, deleteCephPool, deleteRbdImage, fetchCephPool } from '../api/client'
import type { CephPoolInfo } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'
import { formatBytes } from '../lib/format'

export function CephDetailPage() {
  const { name } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [pool, setPool] = useState<CephPoolInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [imageName, setImageName] = useState('data-01')
  const [sizeGb, setSizeGb] = useState(1)
  const [deleteImage, setDeleteImage] = useState<string | null>(null)

  async function load() {
    if (!name) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      setPool(await fetchCephPool(name))
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not read Ceph pool'))
      setPool(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [name])

  async function onCreateImage(event: FormEvent) {
    event.preventDefault()
    if (!pool) {
      return
    }
    setBusy(true)
    try {
      await createRbdImage(pool.name, { name: imageName, sizeGb })
      toast.push(`Created RBD image ${imageName}`, 'success')
      setPool(await fetchCephPool(pool.name))
    } catch (err) {
      toast.push(apiErrorMessage(err, 'RBD create failed'), 'error')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <LoadingSkeleton />
  }
  if (error && !pool) {
    return <EmptyState title="Pool not reported by Ceph" description={error} action={<Link to="/ceph" className="text-sky-300">Back to Ceph</Link>} />
  }
  if (!pool) {
    return null
  }

  return (
    <div>
      <PageHeader
        title={pool.name}
        crumbs={['Infrastructure', 'Ceph', pool.name]}
        description={pool.detail ?? 'Live Ceph OSD pool.'}
        actions={
          <>
            <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">Refresh</button>
            {canManage && pool.managedByMiniCloud ? (
              <button type="button" onClick={() => setConfirmDelete(true)} className="rounded-md border border-red-800 px-3 py-1.5 text-sm text-red-300 hover:bg-red-950">Delete pool</button>
            ) : null}
          </>
        }
      />
      {error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      <section className="mb-4 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-sm sm:grid-cols-3">
        <div><dt className="text-slate-500">Application</dt><dd>{pool.application ?? '—'}</dd></div>
        <div><dt className="text-slate-500">pg_num</dt><dd>{pool.pgNum ?? '—'}</dd></div>
        <div><dt className="text-slate-500">Managed</dt><dd>{pool.managedByMiniCloud ? 'yes' : 'host'}</dd></div>
        <div><dt className="text-slate-500">Stored</dt><dd>{formatBytes(pool.storedBytes)}</dd></div>
        <div><dt className="text-slate-500">Max available</dt><dd>{formatBytes(pool.maxAvailableBytes)}</dd></div>
        <div><dt className="text-slate-500">Objects</dt><dd>{pool.objectCount}</dd></div>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <h2 className="text-sm font-medium text-slate-200">RBD images</h2>
        {pool.images.length === 0 ? (
          <p className="mt-3 text-sm text-slate-500">No images yet. Create one below, then attach it from a VM detail page.</p>
        ) : (
          <table className="mt-3 w-full text-left text-sm">
            <thead className="text-xs uppercase text-slate-500">
              <tr>
                <th className="py-2">Name</th>
                <th>Size</th>
                <th>Used</th>
                <th>Attached to</th>
                {canManage ? <th></th> : null}
              </tr>
            </thead>
            <tbody>
              {pool.images.map((image) => (
                <tr key={image.name} className="border-t border-slate-800">
                  <td className="py-2 font-mono text-xs">{image.name}</td>
                  <td>{formatBytes(image.sizeBytes)}</td>
                  <td>{formatBytes(image.usedBytes)}</td>
                  <td>{image.usedByVms.length ? image.usedByVms.join(', ') : '—'}</td>
                  {canManage ? (
                    <td>
                      <button
                        type="button"
                        disabled={busy || image.usedByVms.length > 0}
                        onClick={() => setDeleteImage(image.name)}
                        className="text-xs text-red-300 hover:underline disabled:opacity-40"
                      >
                        Delete
                      </button>
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {canManage ? (
          <form onSubmit={onCreateImage} className="mt-4 flex flex-wrap items-end gap-2">
            <label className="text-xs text-slate-400">
              Image name
              <input value={imageName} onChange={(event) => setImageName(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100" required />
            </label>
            <label className="text-xs text-slate-400">
              Size (GiB)
              <input type="number" min={1} max={2048} value={sizeGb} onChange={(event) => setSizeGb(Number(event.target.value))} className="mt-1 block w-24 rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100" />
            </label>
            <button type="submit" disabled={busy} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40">
              Create image
            </button>
          </form>
        ) : null}
      </section>
      <ConfirmDialog
        open={confirmDelete}
        title={`Delete ${pool.name}?`}
        description="This runs ceph osd pool delete. Only empty MiniCloud-managed mc-* pools can be deleted."
        confirmLabel="Delete pool"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false)
          deleteCephPool(pool.name)
            .then(() => {
              toast.push('Ceph pool deleted', 'success')
              navigate('/ceph')
            })
            .catch((err) => toast.push(apiErrorMessage(err), 'error'))
        }}
      />
      <ConfirmDialog
        open={deleteImage !== null}
        title={`Delete RBD image ${deleteImage}?`}
        description="This runs rbd rm. Detach from VMs first."
        confirmLabel="Delete image"
        onCancel={() => setDeleteImage(null)}
        onConfirm={() => {
          const image = deleteImage
          setDeleteImage(null)
          if (!image) {
            return
          }
          deleteRbdImage(pool.name, image)
            .then(async () => {
              toast.push('RBD image deleted', 'success')
              setPool(await fetchCephPool(pool.name))
            })
            .catch((err) => toast.push(apiErrorMessage(err), 'error'))
        }}
      />
    </div>
  )
}
