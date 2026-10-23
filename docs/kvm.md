# KVM

KVM (Kernel-based Virtual Machine) is a Linux kernel module. It exposes
`/dev/kvm` so a userspace process can run guest code on the real CPU.

## Why it exists

Software-only emulation (old QEMU without KVM) translates every guest
instruction in software. That is too slow for a desktop or server VM.

Intel VT-x (`vmx`) and AMD-V (`svm`) let the CPU switch into a guest context.
KVM is the Linux interface to those features.

## How MiniCloud detects it

`LinuxHostMetricsProvider` checks:

- `/dev/kvm` exists
- `/sys/module/kvm` is loaded
- `/proc/cpuinfo` contains `vmx` or `svm`

This machine already reported KVM **AVAILABLE** with Intel VT-x.

## How to debug

```bash
ls -l /dev/kvm
ls /sys/module/kvm
lscpu | grep -E 'Virtualization|VMX|SVM'
grep -E 'vmx|svm' /proc/cpuinfo | head
```

If `/dev/kvm` is missing, enable virtualization in firmware or nested
virtualization if this Ubuntu install is itself a VM.

KVM alone is not enough to manage VMs. You still need QEMU (to build the
machine) and libvirt (to control it).
