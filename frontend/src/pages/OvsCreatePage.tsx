import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage, createOvsBridge } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'

export function OvsCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [name, setName] = useState('mc-br0')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const bridge = await createOvsBridge({ name })
      toast.push(`Created OVS bridge ${bridge.name}`, 'success')
      navigate(`/ovs/${encodeURIComponent(bridge.name)}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create OVS bridge'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create OVS bridge"
        crumbs={['Infrastructure', 'OVS', 'Create']}
        description="Runs ovs-vsctl add-br. Name must start with mc-. No physical uplink is attached."
      />
      <form onSubmit={onSubmit} className="max-w-lg space-y-4 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <label className="block text-sm">
          <span className="text-slate-400">Bridge name</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500"
            placeholder="mc-br0"
            required
          />
        </label>
        {error ? <p className="text-sm text-red-300">{error}</p> : null}
        <div className="flex items-center gap-3">
          <button type="submit" disabled={busy} className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500 disabled:opacity-40">
            Create bridge
          </button>
          <Link to="/ovs" className="text-sm text-slate-400">Cancel</Link>
        </div>
      </form>
    </div>
  )
}
