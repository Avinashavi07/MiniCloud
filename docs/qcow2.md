# qcow2

QEMU Copy-On-Write version 2 is the default MiniCloud disk format.

```text
Capacity    = virtual size the guest sees (20 GiB)
Allocation  = bytes actually used on the host (~200 KiB when empty)
```

The guest writes to `vda`. QEMU writes clusters into the qcow2 file.
That is why creating a VM does not instantly consume 20 GiB on disk.

`raw` skips that metadata. It is simpler and often faster, but the file
is the full size.

MiniCloud never reports a fake allocation. Those two numbers come from
`virsh vol-list --details`.
