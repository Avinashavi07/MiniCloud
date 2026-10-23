# Linux bridge vs Open vSwitch

| | Linux bridge (`virbr0`) | Open vSwitch (`mc-br0`) |
| --- | --- | --- |
| Kernel object | `bridge` module | `openvswitch` module + `ovs-vswitchd` |
| Config tool | `ip link` / libvirt | `ovs-vsctl` |
| Used by MiniCloud | Phase 4 NAT networks | Phase 7 lab bridges |
| DHCP / NAT | libvirt dnsmasq + nft | Not provided in Phase 7 |
| Programmable flows | No | OpenFlow |
| Typical cloud use | Simple host NAT | Tenant isolation, tunnels, OVN |

HCI products often run **both**: a Linux bridge or OVS for L2, and
sometimes OVN on top of OVS for logical networks across nodes.

Phase 7 teaches the OVS object model without claiming it replaces libvirt NAT.
