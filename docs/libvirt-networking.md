# libvirt networking

libvirt stores each virtual network as XML and a runtime object. The CLI
MiniCloud uses is still `virsh`, with fixed argument lists.

```text
GET /api/v1/networks
  → NetworkService
    → LibvirtNetworkProvider
      → virsh net-list --all
      → virsh net-info <name>
      → virsh net-dumpxml <name>
```

## Commands that match the UI

```bash
virsh --connect qemu:///system net-list --all
virsh --connect qemu:///system net-info default
virsh --connect qemu:///system net-dumpxml default
virsh --connect qemu:///system net-dhcp-leases default
```

`net-info` is identity and runtime (active, bridge, autostart).
`net-dumpxml` is addressing (forward mode, IP, DHCP range).

## Mapping XML to behavior

```bash
virsh --connect qemu:///system net-dumpxml default
```

| XML | Host behavior |
| --- | --- |
| `<forward mode='nat'>` | Guests can reach the internet; inbound connections are not forwarded unless you add them later |
| `<bridge name='virbr0'>` | Linux bridge `virbr0` is created and managed by libvirt |
| `<ip address='192.168.122.1' netmask='255.255.255.0'>` | Address on `virbr0` — the guest gateway |
| `<dhcp><range …/></dhcp>` | dnsmasq listens on the bridge and hands out leases |

MiniCloud generates the same shape for a new network, then calls
`net-define`, `net-start`, and optionally `net-autostart`. It does not pick
a bridge name; libvirt assigns the next `virbrN`.

## Lifecycle

| Action | virsh | Restriction MiniCloud keeps |
| --- | --- | --- |
| Start | `net-start` | Already-active is success |
| Stop | `net-destroy` | Refused if a VM NIC is still attached |
| Autostart | `net-autostart` / `--disable` | Persists across libvirtd restart |
| Delete | `net-undefine` | Refused while active; `default` is never undefined |

libvirt itself also refuses `net-undefine` on an active network. MiniCloud
returns that as a 409 instead of pretending the delete worked.

## Attach / detach

```text
POST /api/v1/vms/{name}/interfaces
  → virsh attach-interface --type network --source <net> --model virtio --config [--live]

DELETE /api/v1/vms/{name}/interfaces/{mac}
  → virsh detach-interface --type network --mac <mac> --config [--live]
```

`--config` writes the domain XML. `--live` asks QEMU to unplug now.
If the guest virtio driver does not release the PCI device, `domiflist`
can still show the TAP. MiniCloud reports that instead of inventing
success. Restarting the domain finishes the unplug.
