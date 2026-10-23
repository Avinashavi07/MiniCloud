# VDI — Phase 6

**VDI** (Virtual Desktop Infrastructure) means the desktop a person uses is a
virtual machine, not a physical PC.

```text
Operator browser (noVNC)
        ↓
MiniCloud JWT + short-lived ticket
        ↓
WebSocket /ws/console
        ↓
TCP 127.0.0.1:590x
        ↓
QEMU VNC   (from the domain <graphics type='vnc'>)
        ↓
guest framebuffer
```

MiniCloud is still a control plane. It does not implement VNC or RDP.
QEMU already serves VNC. noVNC (in the browser) already speaks RFB.
MiniCloud only:

1. Reads the listen address and port from libvirt
2. Issues a ticket after login
3. Copies bytes between the WebSocket and `127.0.0.1`

## What is not VDI yet

Apache Guacamole, RDP, SSH, and per-user desktop assignment are later.
The `ConsoleProvider` interface is the plug-in point.

## Why VNC stays on 127.0.0.1

Domain XML is:

```xml
<graphics type='vnc' port='-1' listen='127.0.0.1'/>
```

That keeps QEMU off the LAN. The browser cannot reach that port directly.
The control plane must proxy. That is the educational reason MiniCloud
owns `/ws/console`.
