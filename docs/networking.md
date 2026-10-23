# Networking — Phase 4

MiniCloud now manages **libvirt virtual networks** (Phase 4) and can
attach VMs to **Open vSwitch** bridges (Phase 7). It does not implement
VXLAN or OVN yet.

```text
VM virtio NIC
      ↓
   vnetN   (TAP on the host, only while the guest is running)
      ↓
   virbrN  (Linux bridge created by libvirt)
      ↓
   NAT     (iptables / nft masquerade)
      ↓
   host NIC
      ↓
   Internet
```

On this lab the first network is still libvirt's `default`:

```text
Name: default
Bridge: virbr0
Network: 192.168.122.0/24
Gateway: 192.168.122.1
DHCP: 192.168.122.2 – 192.168.122.254
Forward: NAT
```

## What is a virtual NIC?

The guest sees a PCI network card. QEMU implements that card in software
(or with virtio). Packets leaving the guest are written into a TAP device
on the Ubuntu host. That TAP is the `vnet0` you see in `virsh domiflist`
while the VM is running.

## Why virtio-net?

`virtio` is a paravirtual NIC. The guest driver knows it is virtual, so it
skips emulating a real Intel e1000. Throughput and CPU cost are better.
MiniCloud still allows `e1000` / `rtl8139` for attach, but create uses virtio.

## What is `vnet0`?

A host TAP interface. libvirt creates it when the domain starts and names
it `vnet0`, `vnet1`, … It is **not** stored in the domain XML as a fixed
name. When the VM is shut off, `virsh domiflist` shows Interface `-`.

## What is `virbr0`?

A Linux bridge. libvirt's `default` network owns it. TAP devices (`vnet0`)
are enslaved to that bridge. `virbr0` has `192.168.122.1/24` and is the
DHCP/DNS/NAT gateway for guests.

## How DHCP works

dnsmasq, started by libvirt for that network, answers DHCP on the bridge.
The lease is real. MiniCloud reads it with `virsh net-dhcp-leases` and
`virsh domifaddr --source lease`. If the guest has not requested an address,
the UI shows `—`. It does not invent an IP.

## How a MAC is chosen

Every Ethernet NIC needs a MAC. libvirt assigns a `52:54:00:…` address
(QEMU's reserved OUI) when the interface is defined. Attach/detach use that
MAC as the stable identifier.

## Source of truth

libvirt. PostgreSQL stores audit events only. A network row is never created
to pretend a bridge exists.

Read next:

- [libvirt-networking.md](libvirt-networking.md)
- [virtual-network.md](virtual-network.md)
- [vm-network-interface.md](vm-network-interface.md)
