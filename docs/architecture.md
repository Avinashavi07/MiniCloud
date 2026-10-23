# MiniCloud architecture — Phase 8

MiniCloud is a small educational control plane for a single Ubuntu host.
It talks to libvirt, Open vSwitch (when installed), and Ceph (when installed).

```text
Browser
   │
   ▼
Spring Boot
   │
   ├── PostgreSQL
   └── Adapters
          ├── LibvirtHypervisorProvider
          ├── LibvirtNetworkProvider   (NAT / virbr)
          ├── OpenVSwitchProvider      (ovs-vsctl)
          ├── LibvirtStorageProvider   (dir pools / qcow2)
          ├── MicroCephProvider        (ceph / rbd)
          └── LibvirtConsoleProvider
```

## Provider status

| Interface | Phase 8 status |
| --- | --- |
| `HostMetricsProvider` | Live Linux metrics |
| `HypervisorProvider` | Domains + NAT/OVS NIC + file/RBD disk attach |
| `NetworkProvider` | libvirt NAT networks |
| `OvsProvider` | OVS bridge list/create/delete |
| `StorageProvider` | Directory pools and volumes |
| `CephProvider` | Cluster status, pools, RBD images |
| `ConsoleProvider` | QEMU VNC + localhost WebSocket bridge |

libvirt remains the source of truth for domains. OVS is the source of truth
for OVS bridges. Ceph is the source of truth for pools and RBD images.
PostgreSQL stores audit and metadata only.
