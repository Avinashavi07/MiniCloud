# VM disks

A domain disk is an `<disk>` in the domain XML that points at a volume:

```xml
<disk type='file' device='disk'>
  <driver name='qemu' type='qcow2'/>
  <source file='/var/lib/libvirt/images/ubuntu-test-02.qcow2'/>
  <target dev='vda' bus='virtio'/>
</disk>
```

`vda` is the virtio-blk target. Additional volumes attach as `vdb`, `vdc`.

```bash
virsh --connect qemu:///system domblklist ubuntu-test-02 --details
```

must match the VM detail page. MiniCloud will not invent a disk row.

Live attach uses `--live` and `--config` as separate virsh calls, then
re-reads `domblklist`. MiniCloud will not report success if the target
is missing. Live detach can linger until the guest releases the PCI
device; persistent XML is updated first.

q35 guests need a free `pcie-root-port`. If every port is already used
(boot disk, USB, balloon, NICs), `attach-disk --live` fails with
`No more available PCI slots`. MiniCloud reports that error. New MiniCloud
domains define spare PCIe ports so a later `vdb` can hotplug.
