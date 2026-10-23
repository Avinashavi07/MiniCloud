# libvirt console

```text
GET /api/v1/consoles
  → ConsoleService
    → LibvirtConsoleProvider
      → virsh dumpxml
      → virsh vncdisplay   (only while running)
```

## Commands that match the UI

```bash
virsh --connect qemu:///system dumpxml ubuntu-test-02 | grep -A2 graphics
virsh --connect qemu:///system vncdisplay ubuntu-test-02
ss -ltnp | grep 590
```

`dumpxml` is the configured graphics device. `vncdisplay` is the live
listen address after QEMU starts.

## Open Console

```text
POST /api/v1/vms/{name}/console/sessions
  → ticket (5 minutes)
  → browser noVNC
  → ws://…/ws/console?ticket=…
  → 127.0.0.1:{port}
```

The ticket is the WebSocket credential. VNC itself has no password on
this lab. That is why the listen address must stay loopback.
