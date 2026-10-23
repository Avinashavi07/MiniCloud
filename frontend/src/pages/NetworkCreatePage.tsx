import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage, createNetwork } from '../api/client'
import { ErrorState } from '../components/ErrorState'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'

export function NetworkCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [name, setName] = useState('minicloud-net-01')
  const [cidr, setCidr] = useState('192.168.100.0/24')
  const [gateway, setGateway] = useState('192.168.100.1')
  const [dhcpStart, setDhcpStart] = useState('192.168.100.10')
  const [dhcpEnd, setDhcpEnd] = useState('192.168.100.200')
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
      const network = await createNetwork({ name, cidr, gateway, dhcpStart, dhcpEnd, autostart })
      toast.push(`libvirt defined ${network.name} on ${network.bridge ?? 'a new bridge'}`, 'success')
      navigate(`/networks/${encodeURIComponent(network.name)}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'Network creation failed'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <PageHeader
        title="Create virtual network"
        crumbs={['Infrastructure', 'Networks', 'Create']}
        description="This creates a real libvirt NAT network: XML define, start, optional autostart. PostgreSQL only stores the audit event."
      />
      {error ? <div className="mb-4"><ErrorState message={error} /></div> : null}

      <form onSubmit={onSubmit} className="max-w-2xl space-y-5 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <p className="text-xs uppercase tracking-wide text-slate-500">Step {step} of 2</p>
        {step === 1 ? (
          <div className="space-y-4">
            <Field label="Network name">
              <input required value={name} onChange={(event) => setName(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="CIDR">
              <input required value={cidr} onChange={(event) => setCidr(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="Gateway">
              <input required value={gateway} onChange={(event) => setGateway(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="DHCP start">
              <input required value={dhcpStart} onChange={(event) => setDhcpStart(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="DHCP end">
              <input required value={dhcpEnd} onChange={(event) => setDhcpEnd(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={autostart} onChange={(event) => setAutostart(event.target.checked)} />
              Autostart with libvirtd
            </label>
          </div>
        ) : (
          <div className="space-y-2 text-sm">
            <p className="text-slate-300">Confirm this will create a real libvirt NAT network and a Linux bridge.</p>
            <Review label="Name" value={name} />
            <Review label="CIDR" value={cidr} />
            <Review label="Gateway" value={gateway} />
            <Review label="DHCP" value={`${dhcpStart} – ${dhcpEnd}`} />
            <Review label="Autostart" value={autostart ? 'yes' : 'no'} />
          </div>
        )}
        <div className="flex justify-between">
          {step === 1 ? (
            <Link to="/networks" className="text-sm text-slate-400">Cancel</Link>
          ) : (
            <button type="button" onClick={() => setStep(1)} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm">Back</button>
          )}
          <button type="submit" disabled={submitting} className="rounded-md bg-sky-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-500 disabled:opacity-60">
            {step < 2 ? 'Next' : submitting ? 'Creating in libvirt…' : 'Create network'}
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
