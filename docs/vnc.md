# VNC

Virtual Network Computing is a framebuffer protocol. QEMU implements a
VNC **server** for the virtual monitor.

```text
virsh vncdisplay ubuntu-test-02
# 127.0.0.1:0     → TCP 5900
# 127.0.0.1:1     → TCP 5901
```

Display number N is port `5900 + N`.

A stopped domain has no QEMU process, so it has no VNC port. Persistent
XML still says `port='-1'` `autoport='yes'`. MiniCloud reports that as
unavailable instead of inventing 5900.

Phase 6 does not implement RFB. The browser uses **noVNC**. MiniCloud
is a TCP bridge, the same job `websockify` does.
