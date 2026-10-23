# Storage — Phase 5

MiniCloud manages **libvirt directory pools** and **volumes** here.
**Ceph / RBD** is Phase 8 — see [ceph.md](ceph.md). LVM and iSCSI stay later.

```text
VM virtio-blk
      ↓
   qcow2 file     /var/lib/libvirt/images/<name>.qcow2
      ↓
   dir pool       libvirt "default"
      ↓
   ext4 / XFS     the Ubuntu filesystem under that directory
```

A **host mount** (`/`) is not a VM disk. The Storage page shows both so
they are not confused.

## What is a storage pool?

A libvirt object that says “VM disks live in this place.” Phase 5 only
creates `type='dir'` pools. The default pool is:

```text
Name: default
Type: dir
Path: /var/lib/libvirt/images
```

## What is a volume?

A file (or later, an LVM/RBD object) inside a pool. MiniCloud creates
`qcow2` or `raw` with `virsh vol-create-as`.

## qcow2 vs raw

- **raw** — the file is as large as the virtual disk.
- **qcow2** — sparse, copy-on-write. A 20 GiB disk can start as a few
  hundred KiB. That is why `vol-list` shows Capacity 20 GiB and
  Allocation ~200 KiB for a new VM.

## Source of truth

libvirt. PostgreSQL stores audit events only.
