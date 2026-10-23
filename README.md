<div align="center">

# MiniCloud

**A single-host infrastructure control plane for real KVM virtual machines.**

Spring Boot drives libvirt, Open vSwitch, and Ceph. React is the operator console, including a browser VNC session. Virtual machines, networks, disks, and RBD images are created on the Ubuntu host — they are not rows invented in PostgreSQL.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![KVM](https://img.shields.io/badge/KVM-libvirt-orange)](https://libvirt.org/)
[![Ceph](https://img.shields.io/badge/Ceph-RBD-EF4444)](https://ceph.io/)

</div>

## What this is

MiniCloud is an operator console for one Ubuntu hypervisor. The UI and API sit in front of the same tools a platform engineer uses on the host: `virsh`, `ovs-vsctl`, `ceph`, and `rbd`.

PostgreSQL stores users, roles, and audit events. Libvirt is the source of truth for domains. Open vSwitch is the source of truth for OVS bridges. Ceph is the source of truth for pools and RBD images.

That split is the point. A control plane that only updates its own database will drift from the host. MiniCloud reads the host back after every change.

## What you can operate

| Area | What the console does |
| --- | --- |
| Virtual machines | Create, start, stop, restart, shut down, and delete KVM domains |
| Networking | libvirt NAT networks, plus Open vSwitch bridges and VM NIC attach |
| Storage | Directory pools and qcow2 volumes, attach and detach VM disks |
| Ceph / RBD | Cluster status, `mc-*` pools, RBD images, attach an image as a VM disk |
| Console | Discover QEMU VNC and open it in the browser through a ticketed WebSocket bridge |
| Access control | JWT login, BCrypt passwords, `ADMIN` / `OPERATOR` / `VIEWER` |
| Operations | Host metrics, dashboard, user list, and an audit log of control-plane actions |

`VIEWER` can read infrastructure and open a desktop console. `ADMIN` and `OPERATOR` can change VMs, networks, storage, and bridges. Only `ADMIN` can list users.

## Architecture

```text
Browser (React, noVNC)
        │  REST + ticketed WebSocket
        ▼
Spring Boot control plane
        │
        ├── PostgreSQL          users, roles, audit
        └── Host adapters
               ├── LibvirtHypervisorProvider    qemu:///system
               ├── LibvirtNetworkProvider       NAT / virbr
               ├── OpenVSwitchProvider          ovs-vsctl
               ├── LibvirtStorageProvider       dir pools / qcow2
               ├── MicroCephProvider            ceph / rbd
               └── LibvirtConsoleProvider       VNC → localhost bridge
```

Each adapter is behind an interface (`HypervisorProvider`, `OvsProvider`, `CephProvider`, and the rest). The services do not shell out themselves. CLI output is parsed in dedicated classes and covered by unit tests, so a libvirt or Ceph format change fails in a test instead of in the UI.

The backend stays on the host JVM. A generic container does not get `/dev/kvm` or the libvirt socket, so PostgreSQL is the only Docker service.

## Stack

| Layer | Choice |
| --- | --- |
| API | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, WebSocket |
| Contract | OpenAPI / Swagger UI, Bean Validation |
| Data | PostgreSQL 16, Testcontainers for the API integration test |
| UI | React 19, TypeScript, Vite, Tailwind CSS, React Router |
| Compute | KVM, QEMU, libvirt (`virsh`) |
| Network | libvirt virtual networks, Open vSwitch |
| Storage | libvirt directory pools, MicroCeph / RBD |
| Console | noVNC over a short-lived console ticket |

## Run it

Ubuntu 24.04 with KVM already set up:

```bash
sudo apt install -y openjdk-21-jdk maven nodejs npm docker.io docker-compose-v2
sudo apt install -y qemu-system-x86 qemu-kvm libvirt-daemon-system libvirt-clients virtinst
sudo usermod -aG libvirt,kvm "$USER"
sudo systemctl enable --now libvirtd
```

Log out after the group change, or start the backend with `sg libvirt`.

```bash
docker compose up -d postgres
cd backend && sg libvirt -c 'mvn spring-boot:run'
```

```bash
cd frontend && npm install && npm run dev
```

Open [http://localhost:5173](http://localhost:5173). API docs are at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

| User | Password | Role |
| --- | --- | --- |
| `admin` | `ChangeMe!Admin` | ADMIN |
| `operator` | `ChangeMe!Operator` | OPERATOR |
| `viewer` | `ChangeMe!Viewer` | VIEWER |

These accounts and the JWT signing key are local defaults. Change them before the API is reachable beyond localhost. MicroCeph and Open vSwitch are optional; the console reports them as unavailable instead of pretending the cluster exists. Setup notes are in [docs/ceph.md](docs/ceph.md) and [docs/ovs.md](docs/ovs.md).

## Tests

```bash
cd backend
mvn test -Dtest=AuthenticationServiceTest,HostServiceTest,DashboardServiceTest,LibvirtOutputParserTest,VmServiceTest,DomainXmlFactoryTest,Ipv4NetworksTest,NetworkXmlFactoryTest,NetworkServiceTest,StoragePathsTest,StorageServiceTest,OvsNamesTest,OvsOutputParserTest,OvsServiceTest,CephNamesTest,CephOutputParserTest,CephServiceTest
```

`AuthApiIT` starts PostgreSQL with Testcontainers and needs Docker.

## Design notes

The `docs/` folder is the design record for the host integration: why a directory pool is not Ceph, how an RBD disk is attached through libvirt, and how the VNC bridge avoids putting a VNC password in the browser.

- [Architecture](docs/architecture.md)
- [VM lifecycle](docs/vm-lifecycle.md)
- [libvirt](docs/libvirt.md) · [KVM](docs/kvm.md) · [QEMU](docs/qemu.md)
- [Networking](docs/networking.md) · [Open vSwitch](docs/ovs.md)
- [Storage](docs/storage.md) · [qcow2](docs/qcow2.md) · [Ceph / RBD](docs/ceph.md)
- [VNC console](docs/vnc.md) · [Authentication](docs/authentication.md)

## Scope

Shipped on a single node: auth, host discovery, VM lifecycle, libvirt networks, directory storage, Open vSwitch, Ceph RBD, and the browser console.

Not in this repository: VXLAN, OVN, VLAN trunking, SR-IOV, LVM, iSCSI, CephFS, RGW, Guacamole, RDP, SPICE, and multi-node Ceph.
