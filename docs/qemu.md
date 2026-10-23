# QEMU

QEMU is a userspace emulator. With KVM it does **not** emulate the CPU for
every instruction. It uses KVM for CPU and emulates the **devices**:

- virtual disk (qcow2 / raw)
- virtual NIC
- firmware / UEFI
- display (VNC — Phase 6 uses this as the lab console)

## Why it exists

A guest OS expects a whole machine, not only a CPU. KVM runs the CPU.
QEMU provides the rest of the PC.

When you later create a VM, libvirt will start a process like:

```text
qemu-system-x86_64 -enable-kvm ...
```

MiniCloud will never construct that command from raw user strings.
libvirt generates it from a domain XML.

## How it fits MiniCloud

```text
libvirt domain
    ↓
QEMU process
    ↓
/dev/kvm
    ↓
guest OS
```

`virsh version` reports the running hypervisor as `QEMU x.y.z` once libvirt
is installed. Phase 2 shows that string in the dashboard when connected.

## Debug

```bash
qemu-system-x86_64 --version
ps aux | grep qemu-system
```

No QEMU process appears until a domain is running. Phase 2 does not create one.
