# VM network interfaces

A domain can have one or more `<interface type='network'>` elements.

```xml
<interface type='network'>
  <source network='default'/>
  <model type='virtio'/>
  <mac address='52:54:00:f6:1c:06'/>
</interface>
```

That is the **persistent** NIC. The **runtime** TAP name (`vnet0`) appears
only while QEMU is running.

## What the UI shows

| Field | Source |
| --- | --- |
| Interface / host if | `virsh domiflist` (TAP name, or `-` if shut off) |
| Type | `network` |
| Source network | `domiflist` Source column |
| Model | `virtio` / `e1000` / `rtl8139` |
| MAC | `domiflist` |
| Bridge | `virsh net-info <source>` → Bridge |
| IP | `domifaddr --source lease` or `net-dhcp-leases` |

No field is invented. A guest that has not requested DHCP has no IP.

## ubuntu-test-02

Expected while running and attached to `default`:

```text
Interface: vnet0
Network: default
Bridge: virbr0
Model: virtio
MAC: 52:54:00:f6:1c:06
```

```bash
virsh --connect qemu:///system domiflist ubuntu-test-02
virsh --connect qemu:///system domifaddr ubuntu-test-02 --source lease
virsh --connect qemu:///system net-dhcp-leases default
```

## Attach another network

```bash
# after POST /api/v1/vms/ubuntu-test-02/interfaces
virsh --connect qemu:///system domiflist ubuntu-test-02
```

You should see a second row with Source `minicloud-net-01` (or whatever
network you attached) and a new `52:54:00:…` MAC. The MiniCloud VM detail
page must show the same rows.

Live detach asks QEMU to unplug the PCI NIC. The guest may keep `vnetN`
until it acknowledges that, or until the domain is restarted. Persistent
XML (`virsh dumpxml --inactive`) is updated immediately. MiniCloud
re-reads `domiflist` and will not claim the TAP is gone if it is still there.
