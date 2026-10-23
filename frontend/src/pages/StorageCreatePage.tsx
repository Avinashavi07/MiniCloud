import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage, createStoragePool } from '../api/client'
import { ErrorState } from '../components/ErrorState'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'

export function StorageCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [name, setName] = useState('minicloud-pool-01')
  const [path, setPath] = useState('/var/lib/libvirt/images/minicloud-pool-01')
  const [autostart, setAutostart] = useState(true)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (step < 2) {
      setStep(2)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const pool = await createStoragePool({ name, path, autostart })
      toast.push(`libvirt defined pool ${pool.name}`, 'success')
      navigate(`/storage/${encodeURIComponent(pool.name)}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'Pool creation failed'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create storage pool"
        crumbs={['Infrastructure', 'Storage', 'Create']}
        description="Defines a real libvirt directory pool. Paths must stay under /var/lib/libvirt/images."
      />
      {error ? <div className="mb-4"><ErrorState message={error} /></div> : null}
      <form onSubmit={onSubmit} className="max-w-2xl space-y-5 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <p className="text-xs uppercase tracking-wide text-slate-500">Step {step} of 2</p>
        {step === 1 ? (
          <div className="space-y-4">
            <Field label="Pool name">
              <input required value={name} onChange={(event) => { setName(event.target.value); setPath(`/var/lib/libvirt/images/${event.target.value}`) }} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="Directory">
              <input required value={path} onChange={(event) => setPath(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={autostart} onChange={(event) => setAutostart(event.target.checked)} />
              Autostart with libvirtd
            </label>
          </div>
        ) : (
          <div className="space-y-2 text-sm">
            <p className="text-slate-300">This calls virsh pool-define-as / pool-build / pool-start.</p>
            <Review label="Name" value={name} />
            <Review label="Path" value={path} />
            <Review label="Autostart" value={autostart ? 'yes' : 'no'} />
          </div>
        )}
        <div className="flex justify-between">
          {step === 1 ? <Link to="/storage" className="text-sm text-slate-400">Cancel</Link> : (
            <button type="button" onClick={() => setStep(1)} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm">Back</button>
          )}
          <button type="submit" disabled={submitting} className="rounded-md bg-sky-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-500 disabled:opacity-60">
            {step < 2 ? 'Next' : submitting ? 'Creating in libvirt…' : 'Create pool'}
          </button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="text-slate-300">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  )
}

function Review({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-slate-800 py-2">
      <span className="text-slate-500">{label}</span>
      <span className="text-slate-100">{value}</span>
    </div>
  )
}
