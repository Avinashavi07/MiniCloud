# Storage pool objects

A pool is not a Ceph cluster and not the host’s root filesystem.

Create path:

```text
POST /api/v1/storage
  { name, path, autostart }
        ↓
validate path under /var/lib/libvirt/images
        ↓
virsh pool-define-as <name> dir --target <path>
virsh pool-build
virsh pool-start
virsh pool-autostart
```

Phase 5 will not create pools on `/home`, `/etc`, or an arbitrary disk.
That keeps the educational surface small and avoids pointing libvirt at
system directories.

`pool-undefine` removes the libvirt object. It does not always `rmdir`
the target directory. An empty leftover folder under
`/var/lib/libvirt/images` is not a pool.
