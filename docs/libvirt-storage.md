# libvirt storage

```text
GET /api/v1/storage
  → StorageService
    → LibvirtStorageProvider
      → virsh pool-list --all
      → virsh pool-info / pool-dumpxml
      → virsh vol-list --details
```

## Commands that match the UI

```bash
virsh --connect qemu:///system pool-list --all
virsh --connect qemu:///system pool-info default
virsh --connect qemu:///system pool-dumpxml default
virsh --connect qemu:///system vol-list --details default
virsh --connect qemu:///system domblklist ubuntu-test-02 --details
```

`pool-info` is identity and capacity. `pool-dumpxml` is type and path.
`vol-list --details` is each file’s virtual size vs allocated size.

## Mapping XML to behavior

```xml
<pool type='dir'>
  <name>default</name>
  <target><path>/var/lib/libvirt/images</path></target>
</pool>
```

libvirt does not store the guest filesystem. It stores files. QEMU opens
those files as virtio-blk devices.

## Lifecycle

| Action | virsh | MiniCloud restriction |
| --- | --- | --- |
| Create pool | `pool-define-as` + `pool-build` + `pool-start` | Path must be under `/var/lib/libvirt/images` |
| Start / stop | `pool-start` / `pool-destroy` | Stop refused if a volume is attached to a VM |
| Delete pool | `pool-undefine` | Refused while active, non-empty, or named `default` |
| Create volume | `vol-create-as … --format qcow2` | Size must fit pool free space |
| Delete volume | `vol-delete` | Refused if a domain uses the file |
| Attach disk | `--live` then `--config` (separate calls) | Next target is `vdb`, `vdc`, …; success requires `domblklist` |
| Detach disk | `--config` then `--live` (separate calls) | `vda` (boot disk) cannot be detached |
