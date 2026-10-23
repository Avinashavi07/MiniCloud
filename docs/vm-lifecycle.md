# VM lifecycle

A libvirt **domain** is a VM definition plus a runtime state.

```text
undefined → defined (XML stored by libvirt)
                 ↓
              running      QEMU process exists, uses KVM
                 ↓
              shut off     definition remains, disk remains
                 ↓
              undefined    definition and disk deleted
```

## What MiniCloud does in Phase 3

```text
Browser wizard
   ↓
POST /api/v1/vms
   ↓
VmService  (validate name, CPU, RAM, disk, host capacity)
   ↓
LibvirtHypervisorProvider
   ↓
virsh vol-create-as default <name>.qcow2 <N>G --format qcow2
virsh define <generated XML>
virsh start <name>          (optional)
   ↓
QEMU + KVM
   ↓
REAL VM
```

PostgreSQL stores metadata **only after** libvirt succeeds. A failed create
does not leave a fake VM row.

| Action | libvirt command | Meaning |
| --- | --- | --- |
| Create | `vol-create-as` + `define` + optional `start` | New disk + domain XML |
| Start | `start` | Power on |
| Shutdown | `shutdown` | ACPI, guest must cooperate |
| Stop | `destroy` | Immediate power off |
| Restart | `reboot` or destroy+start | Reset |
| Delete | `undefine --remove-all-storage` | Remove domain and qcow2 |

## How a guest gets CPU and memory

The domain XML asks libvirt for `<vcpu>` and `<memory>`. QEMU creates the
process. KVM schedules those vCPUs on host cores. Memory is allocated from
the host when the domain is running.

## How a guest gets a disk

`vol-create-as` makes a qcow2 file in the `default` pool
(`/var/lib/libvirt/images`). The XML points `<disk>` at that file.

## How a guest gets a NIC

The XML adds `<interface type='network'><source network='default'/>`.
libvirt attaches a tap to `virbr0` (the default NAT network). Phase 4 will
go deeper on networking.

## Debug

```bash
virsh --connect qemu:///system list --all
virsh --connect qemu:///system dominfo <name>
virsh --connect qemu:///system dumpxml <name>
virsh --connect qemu:///system vol-list default
ps aux | grep qemu-system
journalctl -u libvirtd -e
```
