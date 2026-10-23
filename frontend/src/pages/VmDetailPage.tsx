import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, attachVmDisk, attachVmInterface, attachVmOvsInterface, attachVmRbdDisk, deleteVm, detachVmDisk, detachVmInterface, fetchCeph, fetchNetworks, fetchOvs, fetchStorage, fetchVm, restartVm, shutdownVm, startVm, stopVm } from '../api/client'
import type { OvsBridgeInfo, RbdImageInfo, StorageVolume, VirtualMachineInfo, VirtualNetworkInfo } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'
import { useToast } from '../components/Toast'
import { formatBytes } from '../lib/format'

export function VmDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canManage = user?.role === 'ADMIN' || user?.role === 'OPERATOR'
  const [vm, setVm] = useState<VirtualMachineInfo | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [confirmStop, setConfirmStop] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [deleteName, setDeleteName] = useState('')
  const [networks, setNetworks] = useState<VirtualNetworkInfo[]>([])
  const [attachNetwork, setAttachNetwork] = useState('default')
  const [attachModel, setAttachModel] = useState('virtio')
  const [ovsBridges, setOvsBridges] = useState<OvsBridgeInfo[]>([])
  const [attachOvsBridge, setAttachOvsBridge] = useState('')
  const [detachMac, setDetachMac] = useState<string | null>(null)
  const [volumes, setVolumes] = useState<StorageVolume[]>([])
  const [attachVolume, setAttachVolume] = useState('')
  const [rbdImages, setRbdImages] = useState<RbdImageInfo[]>([])
  const [attachRbd, setAttachRbd] = useState('')
  const [detachTarget, setDetachTarget] = useState<string | null>(null)
  const toast = useToast()

  async function load() {
    if (!id) {
      return
    }
    setLoading(true)
    setError(null)
    try {
      const [next, catalog, storage, ovs, ceph] = await Promise.all([
        fetchVm(id),
        fetchNetworks().catch(() => null),
        fetchStorage().catch(() => null),
        fetchOvs().catch(() => null),
        fetchCeph().catch(() => null),
      ])
      setVm(next)
      if (catalog) {
        setNetworks(catalog.networks)
        if (catalog.networks[0] && !catalog.networks.some((item) => item.name === attachNetwork)) {
          setAttachNetwork(catalog.networks[0].name)
        }
      }
      if (storage) {
        const unused = storage.pools.flatMap((pool) => pool.volumes.filter((volume) => volume.usedBy.length === 0))
        setVolumes(unused)
        if (unused[0] && !unused.some((item) => `${item.pool}/${item.name}` === attachVolume)) {
          setAttachVolume(`${unused[0].pool}/${unused[0].name}`)
        }
      }
      if (ovs?.capabilities.connected) {
        setOvsBridges(ovs.bridges)
        if (ovs.bridges[0] && !ovs.bridges.some((item) => item.name === attachOvsBridge)) {
          setAttachOvsBridge(ovs.bridges[0].name)
        }
      } else {
        setOvsBridges([])
        setAttachOvsBridge('')
      }
      if (ceph?.capabilities.connected) {
        const unusedRbd = ceph.pools.flatMap((pool) => pool.images.filter((image) => image.usedByVms.length === 0))
        setRbdImages(unusedRbd)
        if (unusedRbd[0] && !unusedRbd.some((item) => `${item.pool}/${item.name}` === attachRbd)) {
          setAttachRbd(`${unusedRbd[0].pool}/${unusedRbd[0].name}`)
        }
      } else {
        setRbdImages([])
        setAttachRbd('')
      }
    } catch (err) {
      setVm(null)
      setError(apiErrorMessage(err, 'libvirt has no such domain'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [id])

  async function run(action: () => Promise<VirtualMachineInfo>, label: string) {
    setBusy(true)
    try {
      setVm(await action())
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
  if (error && !vm) {
    return (
      <EmptyState
        title="Domain not reported by libvirt"
        description={error}
        action={<Link to="/vms" className="text-sky-300">Back to VMs</Link>}
      />
    )
  }
  if (!vm) {
    return <ErrorState message="Domain not available" onRetry={() => void load()} />
  }

  return (
    <div>
      <PageHeader
        title={vm.name}
        crumbs={['Infrastructure', 'Virtual Machines', vm.name]}
        description="Live libvirt domain. Operations call virsh through the Spring Boot adapter."
        actions={
          <button type="button" onClick={() => void load()} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            Refresh
          </button>
        }
      />
      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Overview</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="Status" value={<StatusBadge status={vm.normalizedState} />} />
            <Row label="libvirt state" value={vm.state} />
            <Row label="UUID" value={vm.uuid ?? '—'} />
            <Row label="Id" value={vm.hypervisorId ?? '—'} />
            <Row label="OS type" value={vm.osType ?? '—'} />
          </dl>
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Compute</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <Row label="vCPU" value={vm.cpuCores ?? 'n/a'} />
            <Row label="Memory" value={formatBytes(vm.memoryBytes)} />
            <Row label="Used memory" value={formatBytes(vm.memoryUsedBytes)} />
          </dl>
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Storage</h2>
          {vm.disks.length === 0 ? (
            <p className="mt-3 text-sm text-slate-500">No disks reported.</p>
          ) : (
            <table className="mt-3 w-full text-left text-sm">
              <thead className="text-xs uppercase text-slate-500">
                <tr><th className="py-1">Target</th><th>Device</th><th>Source</th>{canManage ? <th></th> : null}</tr>
              </thead>
              <tbody>
                {vm.disks.map((disk) => (
                  <tr key={`${disk.target}-${disk.source}`} className="border-t border-slate-800">
                    <td className="py-2">{disk.target}</td>
                    <td>{disk.device}</td>
                    <td className="text-slate-400">{disk.source ?? '—'}</td>
                    {canManage ? (
                      <td>
                        <button type="button" disabled={busy || disk.target === 'vda' || disk.device === 'cdrom'} onClick={() => setDetachTarget(disk.target ?? null)} className="text-xs text-red-300 hover:underline disabled:opacity-40">
                          Detach
                        </button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {canManage ? (
            <div className="mt-4 space-y-3">
              <div className="flex flex-wrap items-end gap-2">
                <label className="text-xs text-slate-400">
                  Unused qcow2 volume
                  <select value={attachVolume} onChange={(event) => setAttachVolume(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                    {volumes.length === 0 ? <option value="">No unused volumes</option> : null}
                    {volumes.map((item) => (
                      <option key={`${item.pool}/${item.name}`} value={`${item.pool}/${item.name}`}>{item.pool}/{item.name}</option>
                    ))}
                  </select>
                </label>
                <button
                  type="button"
                  disabled={busy || !attachVolume}
                  onClick={() => {
                    const [pool, volume] = attachVolume.split('/')
                    void run(() => attachVmDisk(vm.name, { pool, volume }), 'Attach disk')
                  }}
                  className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40"
                >
                  Attach file disk
                </button>
              </div>
              {rbdImages.length > 0 ? (
                <div className="flex flex-wrap items-end gap-2">
                  <label className="text-xs text-slate-400">
                    Unused RBD image
                    <select value={attachRbd} onChange={(event) => setAttachRbd(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                      {rbdImages.map((item) => (
                        <option key={`${item.pool}/${item.name}`} value={`${item.pool}/${item.name}`}>{item.pool}/{item.name}</option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    disabled={busy || !attachRbd}
                    onClick={() => {
                      const [pool, image] = attachRbd.split('/')
                      void run(() => attachVmRbdDisk(vm.name, { pool, image }), 'Attach RBD')
                    }}
                    className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40"
                  >
                    Attach RBD disk
                  </button>
                </div>
              ) : (
                <p className="text-xs text-slate-500">No unused RBD images. Create one under Ceph after MicroCeph is connected.</p>
              )}
            </div>
          ) : null}
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
          <h2 className="text-sm font-medium text-slate-200">Networking</h2>
          {vm.nics.length === 0 ? (
            <p className="mt-3 text-sm text-slate-500">No interfaces yet.</p>
          ) : (
            <table className="mt-3 w-full text-left text-sm">
              <thead className="text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-1">Interface</th>
                  <th>MAC</th>
                  <th>Model</th>
                  <th>Network</th>
                  <th>Bridge</th>
                  <th>Host if</th>
                  <th>IP</th>
                  {canManage ? <th></th> : null}
                </tr>
              </thead>
              <tbody>
                {vm.nics.map((nic) => (
                  <tr key={nic.mac ?? nic.interfaceName} className="border-t border-slate-800">
                    <td className="py-2">{nic.interfaceName}</td>
                    <td className="text-slate-400">{nic.mac ?? '—'}</td>
                    <td>{nic.model ?? '—'}</td>
                    <td>
                      {nic.type === 'bridge' ? (
                        nic.bridge || nic.network ? (
                          <Link to={`/ovs/${encodeURIComponent(nic.bridge ?? nic.network!)}`} className="text-sky-300 hover:underline">
                            {nic.bridge ?? nic.network} <span className="text-slate-500">(ovs)</span>
                          </Link>
                        ) : '—'
                      ) : nic.network ? (
                        <Link to={`/networks/${encodeURIComponent(nic.network)}`} className="text-sky-300 hover:underline">{nic.network}</Link>
                      ) : '—'}
                    </td>
                    <td>{nic.bridge ?? '—'}</td>
                    <td>{nic.hostInterface ?? '—'}</td>
                    <td>{nic.ip ?? '—'}</td>
                    {canManage ? (
                      <td>
                        <button type="button" disabled={busy || !nic.mac} onClick={() => setDetachMac(nic.mac ?? null)} className="text-xs text-red-300 hover:underline disabled:opacity-40">
                          Detach
                        </button>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {canManage ? (
            <div className="mt-4 space-y-3">
              <div className="flex flex-wrap items-end gap-2">
                <label className="text-xs text-slate-400">
                  libvirt network
                  <select value={attachNetwork} onChange={(event) => setAttachNetwork(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                    {(networks.length ? networks : [{ name: 'default' }]).map((item) => (
                      <option key={item.name} value={item.name}>{item.name}</option>
                    ))}
                  </select>
                </label>
                <label className="text-xs text-slate-400">
                  Model
                  <select value={attachModel} onChange={(event) => setAttachModel(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                    <option value="virtio">virtio</option>
                    <option value="e1000">e1000</option>
                    <option value="rtl8139">rtl8139</option>
                  </select>
                </label>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => void run(() => attachVmInterface(vm.name, { network: attachNetwork, model: attachModel }), 'Attach NIC')}
                  className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40"
                >
                  Attach NAT NIC
                </button>
              </div>
              {ovsBridges.length > 0 ? (
                <div className="flex flex-wrap items-end gap-2">
                  <label className="text-xs text-slate-400">
                    OVS bridge
                    <select value={attachOvsBridge} onChange={(event) => setAttachOvsBridge(event.target.value)} className="mt-1 block rounded-md border border-slate-700 bg-slate-950 px-2 py-1 text-sm text-slate-100">
                      {ovsBridges.map((item) => (
                        <option key={item.name} value={item.name}>{item.name}</option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    disabled={busy || !attachOvsBridge}
                    onClick={() => void run(() => attachVmOvsInterface(vm.name, { bridge: attachOvsBridge, model: attachModel }), 'Attach OVS NIC')}
                    className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800 disabled:opacity-40"
                  >
                    Attach OVS NIC
                  </button>
                </div>
              ) : (
                <p className="text-xs text-slate-500">No OVS bridges available. Create one under Open vSwitch after installing openvswitch-switch.</p>
              )}
            </div>
          ) : null}
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 lg:col-span-2">
          <h2 className="text-sm font-medium text-slate-200">Operations</h2>
          <p className="mt-2 text-sm text-slate-400">
            Start/reboot use libvirt. Stop is a hard power-off (`virsh destroy`). Shutdown is ACPI.
            Console opens QEMU VNC through MiniCloud.
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            {canManage ? (
              <>
                <Action disabled={busy} onClick={() => void run(() => startVm(vm.name), 'Start')}>Start</Action>
                <Action disabled={busy} onClick={() => setConfirmStop(true)}>Stop</Action>
                <Action disabled={busy} onClick={() => void run(() => restartVm(vm.name), 'Restart')}>Restart</Action>
                <Action disabled={busy} onClick={() => void run(() => shutdownVm(vm.name), 'Shutdown')}>Shutdown</Action>
                <Action disabled={busy} onClick={() => { setDeleteName(''); setConfirmDelete(true) }}>Delete</Action>
              </>
            ) : (
              <p className="w-full text-sm text-slate-500">VIEWER cannot change power state. Console is allowed because VDI is desktop access.</p>
            )}
            <Link to={`/consoles/${encodeURIComponent(vm.name)}`} className="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-500">
              Open Console
            </Link>
          </div>
        </section>
      </div>

      <ConfirmDialog
        open={detachTarget !== null}
        title={`Detach ${detachTarget}?`}
        description="This is virsh detach-disk. The boot disk (vda) cannot be removed."
        confirmLabel="Detach disk"
        onCancel={() => setDetachTarget(null)}
        onConfirm={() => {
          const target = detachTarget
          setDetachTarget(null)
          if (target) {
            void run(() => detachVmDisk(vm.name, target), 'Detach disk')
          }
        }}
      />
      <ConfirmDialog
        open={detachMac !== null}
        title={`Detach ${detachMac}?`}
        description="This is virsh detach-interface. The guest loses that NIC immediately if it is running."
        confirmLabel="Detach NIC"
        onCancel={() => setDetachMac(null)}
        onConfirm={() => {
          const mac = detachMac
          setDetachMac(null)
          if (mac) {
            void run(() => detachVmInterface(vm.name, mac), 'Detach NIC')
          }
        }}
      />
      <ConfirmDialog
        open={confirmStop}
        title={`Force-stop ${vm.name}?`}
        description="This is virsh destroy: immediate power-off, not a graceful ACPI shutdown."
        confirmLabel="Stop VM"
        onCancel={() => setConfirmStop(false)}
        onConfirm={() => {
          setConfirmStop(false)
          void run(() => stopVm(vm.name), 'Stop')
        }}
      />

      {confirmDelete ? (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md rounded-xl border border-slate-700 bg-slate-900 p-5">
            <h2 className="text-lg font-semibold">Delete {vm.name}?</h2>
            <p className="mt-2 text-sm text-slate-400">
              This undefines the libvirt domain and removes its qcow2 disk. Type the VM name to confirm.
            </p>
            <input
              value={deleteName}
              onChange={(event) => setDeleteName(event.target.value)}
              className="mt-3 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              placeholder={vm.name}
            />
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" onClick={() => setConfirmDelete(false)} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm">
                Cancel
              </button>
              <button
                type="button"
                disabled={deleteName !== vm.name || busy}
                onClick={() => {
                  setConfirmDelete(false)
                  setBusy(true)
                  deleteVm(vm.name)
                    .then(() => {
                      toast.push('VM deleted from libvirt', 'success')
                      navigate('/vms')
                    })
                    .catch((err) => toast.push(apiErrorMessage(err, 'Delete failed'), 'error'))
                    .finally(() => setBusy(false))
                }}
                className="rounded-md bg-red-600 px-3 py-1.5 text-sm text-white disabled:opacity-40"
              >
                Delete permanently
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function Action({ children, onClick, disabled }: { children: ReactNode; onClick: () => void; disabled?: boolean }) {
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
