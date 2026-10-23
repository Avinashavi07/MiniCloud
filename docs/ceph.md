# Ceph / RBD — Phase 8

Phase 8 adds **Ceph** next to the Phase 5 libvirt directory pools.
They are different tools.

```text
VM virtio-blk
      ↓
   RBD image      mc-rbd/data-01
      ↓
   Ceph OSDs      (MicroCeph loop devices in the lab)
      ↓
   MON / MGR      cluster quorum
```

Compare with Phase 5:

```text
VM → qcow2 file → libvirt dir pool → host filesystem
```

## Why Ceph exists

Directory pools are enough for a single-host lab disk.
Ceph is the distributed block store used by HCI:

- replicated (or erasure-coded) objects across OSDs
- RBD images as VM disks
- monitors for cluster membership
- independent of any one host’s `/var/lib/libvirt/images`

Phase 8 uses a **single-node MicroCeph** lab. That is still a real Ceph
cluster (MON/MGR/OSDs), not a fake JSON store. Multi-node stretch and
production networking stay for later.

## Source of truth

`ceph` and `rbd`, not PostgreSQL. MiniCloud only creates pools named
`mc-*`. System pools (`.mgr`, etc.) are visible but not deleted.

## Lab install (Ubuntu)

```bash
sudo snap install microceph
sudo snap refresh --hold microceph
sudo microceph cluster bootstrap
sudo microceph disk add loop,4G,3
sudo ceph osd pool create mc-rbd 8 8
sudo ceph osd pool set mc-rbd size 2
sudo ceph osd pool set mc-rbd min_size 1
sudo ceph osd pool application enable mc-rbd rbd
sudo rbd pool init mc-rbd
```

Grant the `libvirt` group read access to the client keyring so MiniCloud
(and `virsh`) can authenticate without root:

```bash
sudo mkdir -p /etc/ceph
sudo cp /var/snap/microceph/current/conf/ceph.conf /etc/ceph/
sudo cp /var/snap/microceph/current/conf/ceph.client.admin.keyring /etc/ceph/
sudo chgrp libvirt /etc/ceph /etc/ceph/ceph.conf /etc/ceph/ceph.client.admin.keyring
sudo chmod 750 /etc/ceph
sudo chmod 640 /etc/ceph/ceph.conf /etc/ceph/ceph.client.admin.keyring
# snap client prefers its own conf dir — make that readable too
sudo chgrp libvirt /var/snap/microceph/current/conf/ceph.conf \
  /var/snap/microceph/current/conf/ceph.client.admin.keyring
sudo chmod 640 /var/snap/microceph/current/conf/ceph.conf \
  /var/snap/microceph/current/conf/ceph.client.admin.keyring
```

If Ceph is missing, MiniCloud reports `NOT_INSTALLED` honestly.

## RBD attach

Attaching an RBD image to a VM:

1. Defines a libvirt secret with the Ceph admin key (stable UUID).
2. Builds `<disk type='network'>` XML with `protocol='rbd'`.
3. Runs `virsh attach-device` with separate `--live` / `--config` calls.

Detach uses the same `virsh detach-disk` path as file disks.

## What Phase 8 does not do

- Multi-node Ceph / stretch clusters
- CephFS, RGW, iSCSI gateways
- LVM pools
- Stealing the host root filesystem as an OSD
