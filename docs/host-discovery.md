# Host discovery

A hypervisor manager is useless if it does not know the machine it is sitting on.
Phase 1 treats the local Ubuntu 24.04 host as the first HCI node.

## What we are learning

A **host** is a physical (or nested) machine that can run VMs.

In a real HCI product this node also contributes:

- CPU and RAM for compute
- local or Ceph disks for storage
- NICs / OVS bridges for networking

MiniCloud starts with one node: this laptop or lab server.

## How MiniCloud discovers it

On startup, `DataInitializer` calls `HostService.discoverLocalHost()`.

`LinuxHostMetricsProvider` then reads the OS:

| Fact | Source |
| --- | --- |
| Hostname | `/etc/hostname` |
| OS | `/etc/os-release` |
| Kernel | `/proc/sys/kernel/osrelease` or `uname -r` |
| CPU cores | `Runtime.availableProcessors()` |
| CPU model and VT-x/AMD-V | `/proc/cpuinfo` flags `vmx` / `svm` |
| CPU usage | `OperatingSystemMXBean.getCpuLoad()` |
| Load average | `/proc/loadavg` |
| Memory | `/proc/meminfo` `MemTotal` / `MemAvailable` |
| Disk | `Files.getFileStore("/")` |
| KVM | `/dev/kvm`, `/sys/module/kvm` |
| libvirt | socket under `/run/libvirt`, `systemctl is-active libvirtd`, `virsh --connect qemu:///system version` |

If a value cannot be read, the API returns `null` and a status string.
It does not invent 64 cores or 90% CPU.

## KVM vs libvirt vs QEMU

These are different layers. Phase 1 only detects them.

- **KVM** is a Linux kernel module. It lets the host CPU run guest instructions.
  Presence of `/dev/kvm` means the kernel can virtualize.
- **QEMU** is userspace. It emulates devices (disk, NIC, firmware) and uses KVM
  for CPU when available.
- **libvirt** is the management API / daemon (`libvirtd`). Tools and MiniCloud
  talk to libvirt instead of spawning `qemu-system-x86_64` by hand.

```text
MiniCloud  →  libvirt  →  QEMU  →  KVM  →  hardware
```

## Commands you can run yourself

```bash
hostnamectl
cat /etc/os-release
uname -r
nproc
lscpu | grep -E 'Model name|Virtualization'
cat /proc/meminfo | head
df -h /
free -h
ls -l /dev/kvm
ls -l /run/libvirt/libvirt-sock
systemctl status libvirtd
virsh --connect qemu:///system version
virsh --connect qemu:///system list --all
```

Compare those outputs with the Hosts page. They should match.

## Why this matters for HCI

When we later add a second node, the same discovery path becomes "inventory".
The control plane will keep one `Host` row per machine and refresh live metrics
from each node. Phase 1 proves that path on localhost first.
