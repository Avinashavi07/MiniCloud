# Guacamole (not in Phase 6)

Apache Guacamole is a **multi-protocol** HTML5 gateway:

```text
Browser
  ↓
guacamole-client (HTTP)
  ↓
guacd
  ↓
VNC / RDP / SSH
```

Phase 6 does not install Guacamole. The same MiniCloud idea applies:
the control plane must not implement those protocols. A later
`GuacamoleConsoleProvider` can replace `LibvirtConsoleProvider` and
return a Guacamole URL instead of a localhost VNC ticket.

Use Guacamole when you need RDP, SSH, recording, or many users sharing
one gateway. Use the Phase 6 bridge when you only need the QEMU monitor
on a single lab host.
