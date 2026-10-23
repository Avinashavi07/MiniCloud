# Virtualization

A physical CPU can run one operating system at a time. Virtualization lets that
CPU run **guest** operating systems beside the host OS.

MiniCloud does not implement virtualization. It manages it.

```text
Hardware
   ↑
KVM          kernel: runs guest CPU instructions
   ↑
QEMU         userspace: emulates disk, NIC, firmware
   ↑
libvirt      management API and daemon
   ↑
Spring Boot  MiniCloud control plane
   ↑
Browser
```

## Why this exists

HCI and VDI both need VMs:

- **HCI** packs compute, storage, and networking onto the same nodes.
- **VDI** gives a user a desktop that is actually a VM.

Without a hypervisor, MiniCloud would only be a website with empty tables.

## Type 1 vs what we use

KVM is a **Type 1** hypervisor in the Linux kernel. QEMU is the userspace
process that builds a virtual machine around KVM. libvirt is the stable API
that MiniCloud talks to so we do not spawn `qemu-system-x86_64` by hand.

Phase 2 connects to that stack and **reads** domains. Phase 3 **creates** them.
Phase 6 opens the QEMU VNC display those domains already have.
