# Troubleshooting

## PostgreSQL will not start / backend cannot connect

```bash
docker compose ps
docker compose logs postgres
ss -ltnp | grep 5432
```

If port 5432 is already used, stop the other PostgreSQL or change the compose
port mapping.

The backend expects:

```text
jdbc:postgresql://localhost:5432/minicloud
user: minicloud
password: minicloud
```

## Backend starts but login fails

- Confirm you are using a seeded user (`admin` / `ChangeMe!Admin`)
- Check backend logs for `Seeded ADMIN user`
- `GET /actuator/health` should be UP

## Frontend shows network errors

The Vite dev server proxies `/api` to `http://localhost:8080`.
If you open the UI from another origin, set CORS in `application-dev.yml`.

## Host metrics look empty

Read the same files MiniCloud reads:

```bash
cat /etc/hostname
cat /proc/meminfo
df -h /
```

If those fail, the API correctly returns `n/a` rather than fake numbers.

## KVM shows UNAVAILABLE

```bash
ls -l /dev/kvm
lscpu | grep Virtualization
```

Common causes:

- CPU virtualization disabled in firmware
- Running in a VM without nested virtualization
- `qemu-kvm` / `kvm` module not installed

This does **not** stop Phase 1. Host discovery still works.

## libvirt shows NOT_INSTALLED or PERMISSION_DENIED

```bash
systemctl status libvirtd
virsh --connect qemu:///system list --all
groups
```

```bash
sudo apt install -y qemu-system-x86 qemu-kvm libvirt-daemon-system libvirt-clients
sudo usermod -aG libvirt,kvm "$USER"
sudo systemctl enable --now libvirtd
sg libvirt -c 'cd backend && mvn spring-boot:run'
```

`PERMISSION_DENIED` means the socket exists but this user cannot use
`qemu:///system`. MiniCloud reports that honestly.

## VM creation fails

```bash
virsh --connect qemu:///system pool-info default
virsh --connect qemu:///system net-info default
dmesg | tail
journalctl -u libvirtd -e
```

Common causes:

- name already exists
- not enough RAM (`MemAvailable`) or pool free space
- ISO in `$HOME` blocked by AppArmor — copy it into `/var/lib/libvirt/images`
- user not in the `libvirt` group

## VM list is empty after libvirt connects

That can be correct if no domains exist yet. Phase 3 creates real domains.
Compare with:

```bash
virsh --connect qemu:///system list --all
```

## Network create or attach fails

```bash
virsh --connect qemu:///system net-list --all
virsh --connect qemu:///system net-info default
virsh --connect qemu:///system net-dumpxml default
virsh --connect qemu:///system domiflist <vm>
```

Common causes:

- CIDR overlaps `192.168.122.0/24`
- trying to stop/delete a network that still has VM NICs
- trying to undefine `default`
- attaching to an inactive network while the VM is running
- guest has no DHCP lease yet — IP stays empty on purpose

## Storage create or attach fails

```bash
virsh --connect qemu:///system pool-list --all
virsh --connect qemu:///system vol-list --details default
virsh --connect qemu:///system domblklist <vm> --details
```

Common causes:

- path outside `/var/lib/libvirt/images`
- deleting `default` or a pool that still has volumes
- deleting a volume that a VM is using
- detaching `vda`
- `No more available PCI slots` — the q35 guest has no free `pcie-root-port`
- live detach reports success from virsh for `--config`, but `domblklist` still shows the target until the guest releases the PCI device

## Console will not open

```bash
virsh --connect qemu:///system vncdisplay <vm>
virsh --connect qemu:///system dumpxml <vm> | grep -A2 graphics
ss -ltnp | grep 590
```

Common causes:

- domain is shut off — QEMU is not listening
- `vncdisplay` fails with "Domain is not running"
- ticket expired (5 minutes)
- VNC listen is not 127.0.0.1 — MiniCloud will not proxy an arbitrary host

## OVS missing or bridge create fails

```bash
ovs-vsctl --version
systemctl status openvswitch-switch
ovs-vsctl list-br
```

Common causes:

- `openvswitch-switch` not installed → MiniCloud reports `NOT_INSTALLED`
- bridge name not `mc-*`
- deleting a bridge that still has `vnet*` ports
- deleting a host bridge MiniCloud did not create

## Tests

Unit tests do not need KVM or OVS.

```bash
cd backend
mvn test -Dtest=AuthenticationServiceTest,HostServiceTest,DashboardServiceTest,LibvirtOutputParserTest,VmServiceTest,DomainXmlFactoryTest,Ipv4NetworksTest,NetworkXmlFactoryTest,NetworkServiceTest,StoragePathsTest,StorageServiceTest,ConsoleServiceTest,GraphicsXmlParserTest,OvsNamesTest,OvsOutputParserTest,OvsServiceTest
```

The API integration test needs Docker for Testcontainers PostgreSQL.
