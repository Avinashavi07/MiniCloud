import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage, createCephPool } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'

export function CephCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [name, setName] = useState('mc-rbd')
  const [pgNum, setPgNum] = useState(8)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const pool = await createCephPool({ name, pgNum })
      toast.push(`Created Ceph pool ${pool.name}`, 'success')
      navigate(`/ceph/${encodeURIComponent(pool.name)}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create Ceph pool'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create Ceph pool"
        crumbs={['Infrastructure', 'Ceph', 'Create']}
        description="Runs ceph osd pool create for an mc-* pool, enables the rbd application, and initializes RBD."
      />
      <form onSubmit={onSubmit} className="max-w-lg space-y-4 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <label className="block text-sm">
          <span className="text-slate-400">Pool name</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500"
            placeholder="mc-rbd"
            required
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-400">pg_num</span>
          <input
            type="number"
            min={1}
            max={128}
            value={pgNum}
            onChange={(event) => setPgNum(Number(event.target.value))}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500"
          />
        </label>
        {error ? <p className="text-sm text-red-300">{error}</p> : null}
        <div className="flex items-center gap-3">
          <button type="submit" disabled={busy} className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500 disabled:opacity-40">
            Create pool
          </button>
          <Link to="/ceph" className="text-sm text-slate-400">Cancel</Link>
        </div>
      </form>
    </div>
  )
}
