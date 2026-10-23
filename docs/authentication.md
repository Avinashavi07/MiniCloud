# Authentication and RBAC

Infrastructure APIs are dangerous. Even listing hosts should require a login
once the control plane is reachable on a network.

## What Phase 1 implements

- Username / password login
- BCrypt password hashing
- JWT bearer tokens
- Roles: `ADMIN`, `OPERATOR`, `VIEWER`
- Audit events `USER_LOGIN` and `USER_LOGOUT`

## Seeded accounts

| Username | Default password | Role |
| --- | --- | --- |
| `admin` | `ChangeMe!Admin` | ADMIN |
| `operator` | `ChangeMe!Operator` | OPERATOR |
| `viewer` | `ChangeMe!Viewer` | VIEWER |

Change these before exposing the app beyond localhost.

## How JWT is used

1. `POST /api/v1/auth/login` verifies the password
2. The server signs a JWT containing `sub` (username) and `role`
3. The browser stores the token and sends `Authorization: Bearer …`
4. `JwtAuthenticationFilter` restores the security context

Phase 1 logout is client-side plus an audit event. The token stays valid until
expiry. A denylist can be added later if we need immediate revoke.

## Permissions in this phase

Every authenticated role can read hosts, dashboard, VMs, libvirt networks,
storage (host disk), and audit logs.

Only `ADMIN` can list users.

`ADMIN` and `OPERATOR` can manage VMs, networks, storage pools, volumes, and
OVS bridges (`mc-*`).
`VIEWER` stays read-only for those writes. Opening a console is allowed for
every authenticated role because VDI is desktop access, not infrastructure
mutation. The WebSocket uses a short-lived ticket, not a VNC password.
