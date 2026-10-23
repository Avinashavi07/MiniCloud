# Virtual network objects

A libvirt network is not a Kubernetes Network, not an OVS bridge, and not
a row in MiniCloud's database.

It is:

1. An XML definition stored by libvirt
2. Optionally an active Linux bridge
3. Optionally a NAT/forwarding setup
4. Optionally a DHCP service (dnsmasq)

## Create path

```text
POST /api/v1/networks
  { name, cidr, gateway, dhcpStart, dhcpEnd, autostart }
        ↓
NetworkService validates RFC1918, DHCP, overlap
        ↓
NetworkXmlFactory builds <network> XML
        ↓
virsh net-define
virsh net-start
virsh net-autostart   (optional)
        ↓
GET the live object from libvirt and return it
```

If define succeeds and start fails, the network stays defined and inactive
so you can inspect it with `virsh net-info` / `net-dumpxml`.

## What MiniCloud will not do in Phase 4

- Overlap `192.168.122.0/24` (or any existing CIDR)
- Use public IPv4 space
- Delete `default`
- Delete or stop a network that still has VM interfaces
- Create OVS, VXLAN, VLAN trunks, or OVN logical switches

## Example

```json
{
  "name": "minicloud-net-01",
  "cidr": "192.168.100.0/24",
  "gateway": "192.168.100.1",
  "dhcpStart": "192.168.100.10",
  "dhcpEnd": "192.168.100.200"
}
```

After a successful create you should see a second bridge, usually `virbr1`,
and `virsh net-list --all` should show the same name as the UI.
