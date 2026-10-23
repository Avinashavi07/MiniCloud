# Open vSwitch — Phase 7

Phase 7 adds **Open vSwitch** next to the Phase 4 libvirt NAT networks.
They are different tools.

```text
VM virtio NIC
      ↓
   vnetN          (TAP on the host)
      ↓
   mc-br0         (OVS bridge — ovs-vsctl)
      ↓
   (lab only)     no physical uplink in Phase 7
```

Compare with Phase 4:

```text
VM → vnetN → virbrN (Linux bridge) → NAT → host NIC → Internet
```

## Why OVS exists

A Linux bridge is enough for simple L2 forwarding and libvirt NAT.
Open vSwitch adds a programmable datapath used by clouds:

- OpenFlow flows
- tunnels (VXLAN later)
- OVN (later)
- richer port types (internal, patch, geneve)

Phase 7 only creates **internal lab bridges** (`mc-*`). It does not steal
your Wi‑Fi interface or build an overlay.

## Source of truth

`ovs-vsctl`, not PostgreSQL. MiniCloud tags bridges it creates with
`external_ids:minicloud=true` and refuses to delete host bridges.

## Attach path

```text
POST /api/v1/vms/{name}/ovs-interfaces
  → virsh attach-device with
      <interface type='bridge'>
        <source bridge='mc-br0'/>
        <virtualport type='openvswitch'/>
      </interface>
```

libvirt still owns the domain XML. OVS owns the bridge and OpenFlow ports.

Read next:

- [linux-bridge-vs-ovs.md](linux-bridge-vs-ovs.md)
- [ovs-bridge.md](ovs-bridge.md)
- [networking.md](networking.md)
