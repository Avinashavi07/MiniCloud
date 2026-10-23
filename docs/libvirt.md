# libvirt

libvirt is the management layer for KVM/QEMU on Linux. The daemon is
`libvirtd`. The CLI is `virsh`. The connection MiniCloud uses is:

```text
qemu:///system
```

That URI means: talk to the **system** QEMU driver, which can start
privileged VMs and use the default NAT network later.

## Why we use it

Talking to QEMU process flags directly is brittle. libvirt gives us:

- a domain (VM) inventory
- start/stop/destroy (Phase 3)
- virtual networks, bridges, NAT, DHCP (Phase 4)
- XML for CPU, memory, disks, NICs
- a place to plug in other drivers later (not in Phase 4)

Spring Boot never embeds libvirt C calls in controllers. It uses
`HypervisorProvider` → `LibvirtHypervisorProvider` → `LibvirtCli`.

`LibvirtCli` runs **fixed** `virsh` argument lists. Domain names are
validated (`[A-Za-z0-9][A-Za-z0-9_.-]{0,63}`) before they become an argument.
User text is never concatenated into a shell string.

## What happens on a list

```text
GET /api/v1/vms
  → VmService
    → LibvirtHypervisorProvider.listVms()
      → virsh --connect qemu:///system list --all
      → virsh dominfo <name>
      → virsh domblklist <name> --details
      → virsh domiflist <name>
      → virsh domifaddr <name> --source lease
```

If libvirt is down, the list is empty. MiniCloud does not write fake VMs.

## Permissions

`qemu:///system` uses `/run/libvirt/libvirt-sock`. Ubuntu allows the
`libvirt` group.

```bash
sudo usermod -aG libvirt,kvm "$USER"
```

Then log out, or start the backend with:

```bash
sg libvirt -c 'cd backend && mvn spring-boot:run'
```

## Debug

```bash
systemctl status libvirtd
virsh --connect qemu:///system version
virsh --connect qemu:///system nodeinfo
virsh --connect qemu:///system list --all
virsh --connect qemu:///system dominfo <name>
journalctl -u libvirtd -e
```

Common failures:

| Symptom | Meaning |
| --- | --- |
| `virsh: command not found` | install `libvirt-clients` |
| `Failed to connect socket` | `libvirtd` is not running |
| `Permission denied` | user is not in `libvirt` |
| empty VM list | no domains exist yet — that is valid |
