# OVS bridges

```text
GET /api/v1/ovs
  → OvsService
    → OpenVSwitchProvider
      → ovs-vsctl --version
      → ovs-vsctl list-br
      → ovs-vsctl list-ports / get Bridge …
```

## Commands that match the UI

```bash
ovs-vsctl --version
ovs-vsctl list-br
ovs-vsctl list-ports mc-br0
ovs-vsctl show
```

## Create / delete

```bash
ovs-vsctl -- may-exist add-br mc-br0 -- set Bridge mc-br0 external_ids:minicloud=true
ovs-vsctl --if-exists del-br mc-br0
```

MiniCloud only creates names matching `mc-*`. Delete is refused if the
bridge is not MiniCloud-managed or still has VM ports.

## Install (Ubuntu 24.04)

```bash
sudo apt update
sudo apt install -y openvswitch-switch
sudo systemctl enable --now openvswitch-switch
ovs-vsctl show
```

Without that package MiniCloud reports `NOT_INSTALLED` and does not invent bridges.

The OVS database socket is normally `root:root`. MiniCloud runs as a
`libvirt` group member, so the lab host should allow that group to talk to
ovsdb (for example `/etc/tmpfiles.d/minicloud-ovs.conf` with
`z /run/openvswitch/db.sock 0770 root libvirt -`). Without that, the UI shows
`PERMISSION_DENIED` instead of inventing bridges.
