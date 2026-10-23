import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage, createVm, fetchVmCatalog } from '../api/client'
import type { VmCatalog } from '../api/types'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../components/Toast'
import { formatBytes } from '../lib/format'

export function VmCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()
  const [step, setStep] = useState(1)
  const [catalog, setCatalog] = useState<VmCatalog | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [name, setName] = useState('ubuntu-test-01')
  const [operatingSystem, setOperatingSystem] = useState('ubuntu-24.04')
  const [network, setNetwork] = useState('default')
  const [cpuCores, setCpuCores] = useState(2)
  const [memoryMb, setMemoryMb] = useState(2048)
  const [diskGb, setDiskGb] = useState(20)
  const [startAfterCreate, setStartAfterCreate] = useState(true)

  useEffect(() => {
    fetchVmCatalog()
      .then((data) => {
        setCatalog(data)
        if (data.networks[0] && !data.networks.some((item) => item.name === network)) {
          setNetwork(data.networks[0].name)
        }
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load create options')))
  }, [])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (step < 3) {
      setStep(step + 1)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const vm = await createVm({
        name,
        cpuCores,
        memoryMb,
        diskGb,
        operatingSystem,
        network,
        startAfterCreate,
      })
      toast.push(`libvirt defined ${vm.name}`, 'success')
      navigate(`/vms/${encodeURIComponent(vm.name)}`)
    } catch (err) {
      setError(apiErrorMessage(err, 'VM creation failed'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!catalog && !error) {
    return <LoadingSkeleton />
  }

  return (
    <div>
      <PageHeader
        title="Create virtual machine"
        crumbs={['Infrastructure', 'Virtual Machines', 'Create']}
        description="This wizard creates a real libvirt domain: qcow2 disk, XML define, optional start. PostgreSQL only stores metadata after libvirt succeeds."
      />
      {error ? <div className="mb-4"><ErrorState message={error} /></div> : null}
      {catalog && !catalog.connected ? (
        <p className="mb-4 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
          libvirt is not connected ({catalog.hypervisorStatus}). Creation will fail until qemu:///system works.
        </p>
      ) : null}

      <form onSubmit={onSubmit} className="max-w-2xl space-y-5 rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <p className="text-xs uppercase tracking-wide text-slate-500">Step {step} of 3</p>

        {step === 1 ? (
          <div className="space-y-4">
            <Field label="VM name">
              <input required value={name} onChange={(event) => setName(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="Operating system">
              <select value={operatingSystem} onChange={(event) => setOperatingSystem(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500">
                {(catalog?.operatingSystems ?? []).map((os) => (
                  <option key={os.id} value={os.id}>
                    {os.label}{os.isoAvailable ? '' : ' — ISO not found'}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Network">
              <select value={network} onChange={(event) => setNetwork(event.target.value)} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500">
                {(catalog?.networks ?? [{ name: 'default', state: 'unknown', autostart: false }]).map((item) => (
                  <option key={item.name} value={item.name}>
                    {item.name} ({item.state})
                  </option>
                ))}
              </select>
            </Field>
            {catalog?.isoImages.length ? (
              <p className="text-xs text-slate-500">
                Found ISOs: {catalog.isoImages.map((iso) => iso.fileName).join(', ')}
              </p>
            ) : (
              <p className="text-xs text-slate-500">No installer ISO found. The VM will still be created with an empty disk.</p>
            )}
          </div>
        ) : null}

        {step === 2 ? (
          <div className="space-y-4">
            <p className="text-sm text-slate-400">
              Host: {catalog?.hostCpuCores ?? 'n/a'} cores, {formatBytes(catalog?.hostMemoryAvailableBytes)} available.
              Pool free: {formatBytes(catalog?.storagePool?.availableBytes)}.
            </p>
            <Field label="CPU cores">
              <input type="number" min={1} max={catalog?.hostCpuCores ?? 64} value={cpuCores} onChange={(event) => setCpuCores(Number(event.target.value))} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="Memory (MB)">
              <input type="number" min={128} value={memoryMb} onChange={(event) => setMemoryMb(Number(event.target.value))} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <Field label="Disk (GB)">
              <input type="number" min={1} value={diskGb} onChange={(event) => setDiskGb(Number(event.target.value))} className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500" />
            </Field>
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input type="checkbox" checked={startAfterCreate} onChange={(event) => setStartAfterCreate(event.target.checked)} />
              Start after create
            </label>
          </div>
        ) : null}

        {step === 3 ? (
          <div className="space-y-2 text-sm">
            <p className="text-slate-300">Confirm this will create a real KVM domain via libvirt.</p>
            <Review label="Name" value={name} />
            <Review label="CPU" value={`${cpuCores} cores`} />
            <Review label="Memory" value={`${memoryMb} MB`} />
            <Review label="Disk" value={`${diskGb} GB`} />
            <Review label="OS" value={operatingSystem} />
            <Review label="Network" value={network} />
            <Review label="Start" value={startAfterCreate ? 'yes' : 'define only'} />
          </div>
        ) : null}

        <div className="flex justify-between">
          {step === 1 ? (
            <Link to="/vms" className="text-sm text-slate-400">Cancel</Link>
          ) : (
            <button type="button" onClick={() => setStep(step - 1)} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm">
              Back
            </button>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="rounded-md bg-sky-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-500 disabled:opacity-60"
          >
            {step < 3 ? 'Next' : submitting ? 'Creating in libvirt…' : 'Create VM'}
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
